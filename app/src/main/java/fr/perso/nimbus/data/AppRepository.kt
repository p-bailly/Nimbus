package fr.perso.nimbus.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
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
 * Liste des apps lançables (tous profils, y compris profil pro),
 * rechargée automatiquement quand une app est installée / mise à jour / supprimée.
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

    init {
        launcherApps.registerCallback(callback)
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

    fun reload() {
        val config = iconConfig ?: return
        loadJob?.cancel()
        loadJob = scope.launch {
            val list = withContext(Dispatchers.Default) { load(config) }
            _apps.value = list
        }
    }

    private fun load(config: IconConfig): List<AppEntry> {
        val pack = config.iconPack?.let { IconPack.load(appContext, it) }
        val me = Process.myUserHandle()
        val result = ArrayList<AppEntry>()
        for (user in launcherApps.profiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val isWork = user != me
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (e: Exception) {
                emptyList()
            }
            for (info in activities) {
                val cn = info.componentName
                if (cn.packageName == appContext.packageName) continue
                val key = "${cn.packageName}/${cn.className}#$serial"
                val icon = iconFactory.create(key, info, pack, config, user, isWork)
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
                        isWorkProfile = isWork,
                    )
                )
            }
        }
        val collator = Collator.getInstance(Locale.getDefault())
        return result.sortedWith(compareBy(collator) { it.label })
    }

    fun close() {
        try {
            launcherApps.unregisterCallback(callback)
        } catch (e: Exception) {
            // déjà désinscrit
        }
    }
}
