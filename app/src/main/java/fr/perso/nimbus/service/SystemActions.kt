package fr.perso.nimbus.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.AppOpsManager
import android.app.StatusBarManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import fr.perso.nimbus.R
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.data.SearchEngine
import fr.perso.nimbus.ui.unlock.UnlockWallpaper

/** Actions système utilisées par le launcher. */
object SystemActions {

    private fun launcherApps(context: Context): LauncherApps =
        context.getSystemService(LauncherApps::class.java)

    fun toast(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    // ------------------------------------------------------------ Apps

    fun launch(context: Context, app: AppEntry) {
        try {
            launcherApps(context).startMainActivity(app.componentName, app.user, null, null)
        } catch (e: Exception) {
            toast(context, "Impossible d'ouvrir ${app.label}")
        }
    }

    fun appInfo(context: Context, app: AppEntry) {
        try {
            launcherApps(context).startAppDetailsActivity(app.componentName, app.user, null, null)
        } catch (e: Exception) {
            start(
                context,
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")),
            )
        }
    }

    fun uninstall(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
        if (app.profile != ProfileType.MAIN) intent.putExtra(Intent.EXTRA_USER, app.user)
        if (!start(context, intent)) toast(context, "Désinstallation impossible pour cette app")
    }

    // ------------------------------------------------------------ Raccourcis d'app (appui long)

    fun shortcuts(context: Context, app: AppEntry): List<ShortcutInfo> {
        val la = launcherApps(context)
        return try {
            if (!la.hasShortcutHostPermission()) return emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setActivity(app.componentName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            val list: List<ShortcutInfo> = la.getShortcuts(query, app.user) ?: emptyList()
            list.filter { it.isEnabled }.sortedBy { it.rank }.take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun startShortcut(context: Context, shortcut: ShortcutInfo) {
        try {
            launcherApps(context).startShortcut(shortcut, null, null)
        } catch (e: Exception) {
            toast(context, "Raccourci indisponible")
        }
    }

    fun shortcutIcon(context: Context, shortcut: ShortcutInfo) = try {
        launcherApps(context).getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi)
    } catch (e: Exception) {
        null
    }

    // ------------------------------------------------------------ Gestes

    fun lockScreen(): Boolean = GestureService.lockScreen()

    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context): Boolean {
        if (GestureService.openNotifications()) return true
        return try {
            val service = context.getSystemService("statusbar") ?: return false
            Class.forName("StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Réglages rapides : service d'accessibilité, sinon StatusBarManager (API cachée). */
    @SuppressLint("WrongConstant")
    fun expandQuickSettings(context: Context): Boolean {
        if (GestureService.performGlobal(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)) return true
        return try {
            val service = context.getSystemService("statusbar") ?: return false
            Class.forName("StatusBarManager")
                .getMethod("expandSettingsPanel")
                .invoke(service)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Applis récentes, capture d'écran, menu Marche/Arrêt… (nécessite le service « Gestes Nimbus »). */
    fun globalAction(action: Int): Boolean = GestureService.performGlobal(action)

    fun openAssistant(context: Context) {
        if (start(context, Intent(Intent.ACTION_ASSIST))) return
        if (!start(context, Intent(Intent.ACTION_VOICE_COMMAND))) toast(context, "Aucun assistant installé")
    }

    // ------------------------------------------------------------ Profil pro et espace privé

    /**
     * Met en pause / réactive le profil pro, verrouille / déverrouille l'espace privé.
     * Le système demande lui-même l'authentification si besoin. Réservé au launcher par défaut.
     */
    fun setQuietMode(context: Context, user: UserHandle, quiet: Boolean): Boolean = try {
        context.getSystemService(UserManager::class.java).requestQuietModeEnabled(quiet, user)
    } catch (e: SecurityException) {
        toast(context, "Nimbus doit être le launcher par défaut")
        false
    } catch (e: Exception) {
        false
    }

    /** Play Store ouvert dans le profil (pour installer des apps dans l'espace privé). Android 15+. */
    fun openAppMarket(context: Context, user: UserHandle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val sender = try {
            launcherApps(context).getAppMarketActivityIntent(null, user)
        } catch (e: Exception) {
            null
        }
        if (sender == null || !startSender(context, sender)) toast(context, "Store indisponible dans ce profil")
    }

    /** Réglages de l'espace privé (Android 16+, par réflexion pour garder compileSdk 35). */
    fun openPrivateSpaceSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 36) {
            val sender = try {
                LauncherApps::class.java.getMethod("getPrivateSpaceSettingsIntent")
                    .invoke(launcherApps(context)) as? IntentSender
            } catch (e: Exception) {
                null
            }
            if (sender != null && startSender(context, sender)) return
        }
        start(context, Intent(Settings.ACTION_SECURITY_SETTINGS))
    }

    private fun startSender(context: Context, sender: IntentSender): Boolean = try {
        // Android 14+ : l'IntentSender est créé par le système, on autorise son lancement d'activité.
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }
        context.startIntentSender(
            sender, null,
            Intent.FLAG_ACTIVITY_NEW_TASK, Intent.FLAG_ACTIVITY_NEW_TASK, 0,
            options,
        )
        true
    } catch (e: Exception) {
        false
    }

    // ------------------------------------------------------------ Divers

    /** Recherche web avec le moteur choisi dans les réglages. */
    fun webSearch(context: Context, query: String, engine: SearchEngine) {
        val template = engine.urlTemplate ?: return webSearch(context, query)
        openUrl(context, template.replace("%s", Uri.encode(query)))
    }

    /** Recherche d'apps sur le Play Store (appli, sinon site web). */
    fun playStoreSearch(context: Context, query: String) {
        val q = Uri.encode(query)
        if (start(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q&c=apps")))) return
        openUrl(context, "https://play.google.com/store/search?q=$q&c=apps")
    }

    fun openUrl(context: Context, url: String) {
        if (!start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) toast(context, "Aucun navigateur installé")
    }

    fun webSearch(context: Context, query: String) {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query)
        if (start(context, search)) return
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
    }

    fun openAlarms(context: Context) {
        if (!start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))) toast(context, "Aucune app Horloge trouvée")
    }

    fun openCalendar(context: Context) {
        val uri = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
        if (!start(context, Intent(Intent.ACTION_VIEW, uri))) toast(context, "Aucune app Agenda trouvée")
    }

    fun openWallpaperPicker(context: Context) {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choisir un fond d'écran")
        if (!start(context, chooser)) toast(context, "Sélecteur de fond d'écran indisponible")
    }

    /** Propose d'ajouter la tuile « Dictée Claude » aux réglages rapides (Android 13+). */
    fun requestDictationTile(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
        statusBar.requestAddTileService(
            ComponentName(context, DictationTileService::class.java),
            context.getString(R.string.dictation_tile_label),
            Icon.createWithResource(context, R.drawable.ms_mic),
            context.mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                toast(context, "La tuile est déjà dans les réglages rapides")
            }
        }
    }

    fun openAccessibilitySettings(context: Context) {
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    // ------------------------------------------------------------ Xiaomi

    /** Téléphone Xiaomi (MIUI, HyperOS ; Redmi et POCO compris). */
    val isXiaomi: Boolean get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    /** Opérations propres à Xiaomi : « Afficher sur l'écran de verrouillage », « Ouvrir de nouvelles fenêtres en arrière-plan ». */
    private val XIAOMI_LOCK_SCREEN_OPS = intArrayOf(10020, 10021)

    /**
     * Les autorisations de Xiaomi sans lesquelles la page de déverrouillage ne s'affiche jamais (l'écran
     * verrouillé du système reste) : refusées par défaut, elles ne se demandent pas, elles se cochent
     * dans « Autres autorisations ». Vrai ailleurs que chez Xiaomi, ou si on ne peut pas les lire.
     */
    fun xiaomiLockScreenAllowed(context: Context): Boolean {
        if (!isXiaomi) return true
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return true
        return try {
            val check = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java,
            )
            XIAOMI_LOCK_SCREEN_OPS.all { op ->
                check.invoke(ops, op, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
            }
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Aperçu de l'écran verrouillé Nimbus (fond d'écran animé), où le système propose de le poser sur
     * l'écran de verrouillage ; à défaut, la liste des fonds d'écran animés.
     */
    fun openUnlockWallpaper(context: Context) {
        val preview = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, UnlockWallpaper.component(context))
        if (start(context, preview)) return
        start(context, Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
    }

    /** « Autres autorisations » de Nimbus dans la Sécurité de Xiaomi ; à défaut, la fiche de l'appli. */
    fun openXiaomiPermissions(context: Context) {
        val editor = Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", context.packageName)
        val security = Intent(editor).setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        if (start(context, security) || start(context, editor)) return
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    fun openNotificationAccessSettings(context: Context) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(context, BadgeService::class.java).flattenToString(),
        )
        if (android.os.Build.VERSION.SDK_INT >= 30 && start(context, detail)) return
        start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }
}
