package fr.perso.nimbus.service

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import fr.perso.nimbus.data.AppEntry

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
        if (app.isWorkProfile) intent.putExtra(Intent.EXTRA_USER, app.user)
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
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ------------------------------------------------------------ Divers

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

    fun openAccessibilitySettings(context: Context) {
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
