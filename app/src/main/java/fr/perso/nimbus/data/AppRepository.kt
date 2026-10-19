package fr.perso.nimbus.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.LauncherUserInfo
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * Liste des apps lançables (tous profils : principal, pro, espace privé),
 * rechargée automatiquement quand une app est installée / mise à jour / supprimée
 * ou quand un profil est mis en pause, verrouillé ou déverrouillé.
 *
 * Les apps d'un espace privé verrouillé ne sont jamais listées.
 */
class AppRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val iconFactory: IconFactory,
) {
    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _profiles = MutableStateFlow<List<ProfileState>>(emptyList())
    /** Profils secondaires (pro, espace privé) et leur état. */
    val profiles: StateFlow<List<ProfileState>> = _profiles.asStateFlow()

    private var iconConfig: IconConfig? = null
    private var loadJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            packageNames.forEach { iconFactory.invalidate(it) }
            reload()
        }
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            reload()
        }
    }

    /** Pause / reprise du profil pro, verrouillage / déverrouillage de l'espace privé, ajout de profil. */
    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    init {
        launcherApps.registerCallback(callback)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
        }
        ContextCompat.registerReceiver(appContext, profileReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun changed(packageName: String) {
        iconFactory.invalidate(packageName)
        reload()
    }

    fun setIconConfig(config: IconConfig) {
        if (config == iconConfig) return
        iconConfig = config
        iconFactory.clear()
        reload()
    }

    /** Après changement des icônes personnalisées de ces apps. */
    fun refreshIcons(keys: Collection<String>) {
        keys.forEach(iconFactory::invalidateKey)
        reload()
    }

    fun reload() {
        val config = iconConfig ?: return
        loadJob?.cancel()
        loadJob = scope.launch {
            val (list, profiles) = withContext(Dispatchers.Default) { load(config) }
            _apps.value = list
            _profiles.value = profiles
        }
    }

    private fun load(config: IconConfig): Pair<List<AppEntry>, List<ProfileState>> {
        val pack = config.iconPack?.let { IconPack.load(appContext, it) }
        val me = Process.myUserHandle()
        val result = ArrayList<AppEntry>()
        val profiles = ArrayList<ProfileState>()
        for (user in launcherApps.profiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val type = profileType(user, me)
            val quiet = type != ProfileType.MAIN && isQuiet(user)
            if (type != ProfileType.MAIN) profiles.add(ProfileState(user, type, quiet, entryHidden(user, type)))
            // Espace privé verrouillé : ses apps ne doivent apparaître nulle part.
            if (type == ProfileType.PRIVATE && quiet) continue
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                emptyList()
            }
            for (info in activities) {
                val cn = info.componentName
                if (cn.packageName == appContext.packageName) continue
                val key = "${cn.packageName}/${cn.className}#$serial"
                val icon = iconFactory.create(key, info, pack, config, user, type != ProfileType.MAIN)
                val category = Categorizer.categorize(info.applicationInfo)
                result.add(
                    AppEntry(
                        key = key,
                        packageName = cn.packageName,
                        componentName = cn,
                        user = user,
                        label = info.label?.toString()?.trim().orEmpty().ifEmpty { cn.packageName },
                        icon = icon,
                        autoCategory = category,
                        isWorkProfile = type == ProfileType.WORK,
                        profile = type,
                        isPaused = quiet,
                    )
                )
            }
        }
        val collator = Collator.getInstance(Locale.getDefault())
        return result.sortedWith(compareBy(collator) { it.label }) to profiles
    }

    private fun profileType(user: UserHandle, me: UserHandle): ProfileType {
        if (user == me) return ProfileType.MAIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val type = try {
                launcherApps.getLauncherUserInfo(user)?.userType
            } catch (e: Exception) {
                null
            }
            if (type == UserManager.USER_TYPE_PROFILE_PRIVATE) return ProfileType.PRIVATE
        }
        return ProfileType.WORK
    }

    private fun isQuiet(user: UserHandle): Boolean = try {
        userManager.isQuietModeEnabled(user)
    } catch (e: Exception) {
        false
    }

    /**
     * Réglage système « masquer l'espace privé quand il est verrouillé ».
     * LauncherUserInfo.getUserConfig() n'existe qu'à partir d'Android 16 (API 36) :
     * appel par réflexion pour garder compileSdk 35.
     */
    private fun entryHidden(user: UserHandle, type: ProfileType): Boolean {
        if (type != ProfileType.PRIVATE || Build.VERSION.SDK_INT < 36) return false
        return try {
            val info = launcherApps.getLauncherUserInfo(user) ?: return false
            val config = LauncherUserInfo::class.java.getMethod("getUserConfig").invoke(info) as? Bundle
            config?.getBoolean("private_space_entrypoint_hidden", false) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        try {
            launcherApps.unregisterCallback(callback)
            appContext.unregisterReceiver(profileReceiver)
        } catch (e: Exception) {
            // déjà désinscrit
        }
    }
}
