package fr.perso.nimbus.ui.settings

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.R
import fr.perso.nimbus.data.BadgeStyle
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.IconPack
import fr.perso.nimbus.data.IconPackInfo
import fr.perso.nimbus.data.IconShape
import fr.perso.nimbus.data.IconSize
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.SearchEngine
import fr.perso.nimbus.data.SortMode
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.SystemStatus
import fr.perso.nimbus.ui.components.AppPickerDialog
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.components.PinDialog
import fr.perso.nimbus.ui.components.PinMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

/** Dialogues ouverts depuis les réglages. */
private sealed interface SettingsDialog {
    data object Theme : SettingsDialog
    data object Shape : SettingsDialog
    data object IconPackChoice : SettingsDialog
    data object Size : SettingsDialog
    data object DrawerColumns : SettingsDialog
    data object Sort : SettingsDialog
    data object HomeColumns : SettingsDialog
    data object HomeRows : SettingsDialog
    data object DockColumns : SettingsDialog
    data object Badge : SettingsDialog
    data object Engine : SettingsDialog
    data class GestureChoice(val gesture: Gesture) : SettingsDialog
    data class GestureApp(val gesture: Gesture) : SettingsDialog
    data object ConfirmImport : SettingsDialog
    data object PinUnlock : SettingsDialog
    data object PinCreate : SettingsDialog
    data object PinChangeOld : SettingsDialog
    data object PinChangeNew : SettingsDialog
}

/** Actions de geste qui passent par le service d'accessibilité. */
private val SERVICE_ACTIONS = setOf(GestureAction.LOCK, GestureAction.RECENTS, GestureAction.SCREENSHOT, GestureAction.POWER_MENU)

private fun gestureIcon(gesture: Gesture): Int = when (gesture) {
    Gesture.DOUBLE_TAP -> R.drawable.ms_touch_app
    Gesture.SWIPE_UP -> R.drawable.ms_swipe_up
    Gesture.SWIPE_DOWN -> R.drawable.ms_swipe_down
    Gesture.TWO_FINGER_UP, Gesture.TWO_FINGER_DOWN -> R.drawable.ms_swipe_vertical
    Gesture.PINCH_IN -> R.drawable.ms_pinch
    Gesture.PINCH_OUT -> R.drawable.ms_open_in_full
}

/** Paramètres façon Smart Launcher : page principale (bandeaux, entrées colorées, sections) et sous-pages. */
@Composable
fun SettingsScreen(vm: LauncherViewModel, host: LauncherHost, section: SettingsSection) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val gestureApps by vm.gestureApps.collectAsStateWithLifecycle()
    val backupFolder by vm.autoBackupFolder.collectAsStateWithLifecycle()
    val wallpaperColor by vm.wallpaperColor.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val iconPacks by produceState<List<IconPackInfo>?>(null) {
        value = withContext(Dispatchers.IO) { IconPack.installed(context) }
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings) = vm.updateSettings(transform)
    val open: (SettingsSection) -> Unit = vm::openSettingsSection
    val back: () -> Unit = vm::closeScreen

    BackHandler { vm.closeScreen() }

    AnimatedContent(
        targetState = section,
        transitionSpec = {
            if (targetState != SettingsSection.MAIN) {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 8 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 8 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
            }
        },
        label = "settings",
    ) { page ->
        when (page) {
            SettingsSection.MAIN -> SettingsPage("Paramètres", onBack = null) {
                mainPage(settings, status, context, host, open)
            }
            SettingsSection.APPEARANCE -> SettingsPage("Apparence globale", onBack = back) {
                item(key = "preview") {
                    AppearancePreview(apps, settings, wallpaperColor) { dialog = SettingsDialog.Theme }
                }
                item {
                    SettingsEntry(R.drawable.ms_wallpaper, SettingsColors.Green, "Fond d'écran", "Image ou fond animé") {
                        SystemActions.openWallpaperPicker(context)
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_interests, SettingsColors.Orange, "Forme des icônes", settings.iconShape.label, chevron = true) {
                        dialog = SettingsDialog.Shape
                    }
                }
                item {
                    val packLabel = settings.iconPack?.let { pkg ->
                        iconPacks?.firstOrNull { it.packageName == pkg }?.label ?: pkg
                    } ?: "Aucun (icônes d'origine)"
                    SettingsEntry(R.drawable.ms_style, SettingsColors.Orange, "Pack d'icônes", packLabel, chevron = true) {
                        dialog = SettingsDialog.IconPackChoice
                    }
                }
                item {
                    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    SettingsEntry(
                        R.drawable.ms_format_paint, SettingsColors.Orange, "Icônes à thème",
                        if (supported) "Monochromes, aux couleurs du thème (comme sur Pixel)" else "Nécessite Android 13",
                        enabled = supported,
                        checked = settings.themedIcons && supported,
                    ) { update { it.copy(themedIcons = !it.themedIcons) } }
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_dashboard_customize, SettingsColors.Orange, "Uniformiser les anciennes icônes",
                        "Fond de la forme choisie pour les icônes non adaptatives",
                        checked = settings.wrapLegacyIcons,
                    ) { update { it.copy(wrapLegacyIcons = !it.wrapLegacyIcons) } }
                }
                item {
                    SettingsEntry(R.drawable.ms_format_size, SettingsColors.Orange, "Taille des icônes", settings.iconSize.label, chevron = true) {
                        dialog = SettingsDialog.Size
                    }
                }
            }
            SettingsSection.GESTURES -> SettingsPage("Gestes et raccourcis", onBack = back) {
                for (gesture in Gesture.entries) {
                    item(key = "gesture:${gesture.name}") {
                        val action = settings.actionFor(gesture)
                        val subtitle = if (action == GestureAction.APP) {
                            "Ouvrir " + (gestureApps[gesture]?.let { apps.byKey[it]?.label } ?: "… (aucune app choisie)")
                        } else {
                            action.label
                        }
                        SettingsEntry(gestureIcon(gesture), SettingsColors.Red, gesture.label, subtitle, chevron = true) {
                            dialog = SettingsDialog.GestureChoice(gesture)
                        }
                    }
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_accessibility_new, SettingsColors.Gray, "Service « Gestes Nimbus »",
                        if (status.gestureService) "Activé" else "Désactivé : verrouillage, capture d'écran, applis récentes…",
                        chevron = true,
                    ) { SystemActions.openAccessibilitySettings(context) }
                }
            }
            SettingsSection.BACKUP -> SettingsPage("Sauvegarde", onBack = back) {
                item {
                    SettingsEntry(
                        R.drawable.ms_upload_file, SettingsColors.Cyan, "Exporter la configuration",
                        "Réglages, accueil, dock, catégories, noms, icônes, gestes (.json)",
                    ) { host.exportBackup() }
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_file_open, SettingsColors.Cyan, "Importer une configuration",
                        "Sauvegarde Nimbus (.json) ou Smart Launcher (.slbk)",
                    ) { dialog = SettingsDialog.ConfirmImport }
                }
                item {
                    val folder = backupFolder
                    val subtitle = if (folder == null) {
                        "Désactivée · choisir un dossier"
                    } else {
                        val last = vm.lastAutoBackup
                        "Chaque jour dans « ${folderName(folder)} » (7 dernières gardées)" +
                            if (last > 0) " · dernière : ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(last))}" else ""
                    }
                    SettingsEntry(R.drawable.ms_autorenew, SettingsColors.Cyan, "Sauvegarde automatique", subtitle, chevron = true) {
                        host.chooseAutoBackupFolder()
                    }
                }
                if (backupFolder != null) {
                    item {
                        SettingsEntry(R.drawable.ms_close, SettingsColors.Gray, "Désactiver la sauvegarde automatique") {
                            vm.setAutoBackupFolder(null)
                        }
                    }
                }
            }
            SettingsSection.HOME -> SettingsPage("Page d'accueil", onBack = back) {
                item {
                    SettingsEntry(R.drawable.ms_view_column, SettingsColors.Blue, "Colonnes de la grille", "${settings.homeColumns}", chevron = true) {
                        dialog = SettingsDialog.HomeColumns
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_grid_view, SettingsColors.Blue, "Lignes de la grille", "${settings.homeRows}", chevron = true) {
                        dialog = SettingsDialog.HomeRows
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_title, SettingsColors.Blue, "Noms sous les icônes", checked = settings.homeShowLabels) {
                        update { it.copy(homeShowLabels = !it.homeShowLabels) }
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_dock_to_bottom, SettingsColors.Blue, "Icônes par rangée du dock", "${settings.dockColumns}", chevron = true) {
                        dialog = SettingsDialog.DockColumns
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_schedule, SettingsColors.Blue, "Horloge et date", checked = settings.showClock) {
                        update { it.copy(showClock = !it.showClock) }
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_battery_full, SettingsColors.Blue, "Niveau de batterie", checked = settings.showBattery) {
                        update { it.copy(showBattery = !it.showBattery) }
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_search, SettingsColors.Blue, "Barre de recherche", "En bas de l'accueil", checked = settings.showSearchBar) {
                        update { it.copy(showSearchBar = !it.showSearchBar) }
                    }
                }
            }
            SettingsSection.DRAWER -> SettingsPage("Page des applications", onBack = back) {
                item {
                    SettingsEntry(R.drawable.ms_view_column, SettingsColors.Blue, "Colonnes", "${settings.drawerColumns}", chevron = true) {
                        dialog = SettingsDialog.DrawerColumns
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_title, SettingsColors.Blue, "Nom des apps", checked = settings.showLabels) {
                        update { it.copy(showLabels = !it.showLabels) }
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_sort, SettingsColors.Blue, "Tri", settings.sortMode.label, chevron = true) {
                        dialog = SettingsDialog.Sort
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_history, SettingsColors.Blue, "Apps fréquentes en haut", "En tri alphabétique", checked = settings.showFrequentApps) {
                        update { it.copy(showFrequentApps = !it.showFrequentApps) }
                    }
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_category, SettingsColors.Blue, "Catégories automatiques",
                        "Onglets ; appui long sur une app › Plus pour la déplacer",
                        checked = settings.autoCategories,
                    ) { update { it.copy(autoCategories = !it.autoCategories) } }
                }
            }
            SettingsSection.SEARCH -> SettingsPage("Page de recherche", onBack = back) {
                item {
                    SettingsEntry(R.drawable.ms_public, SettingsColors.Blue, "Moteur de recherche web", settings.webSearchEngine.label, chevron = true) {
                        dialog = SettingsDialog.Engine
                    }
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_contacts, SettingsColors.Blue, "Rechercher dans les contacts",
                        if (settings.searchContacts && !status.contactsPermission) "Autorisation requise" else "Contacts dans les résultats",
                        checked = settings.searchContacts,
                    ) {
                        val enable = !settings.searchContacts
                        update { it.copy(searchContacts = enable) }
                        if (enable && !status.contactsPermission) host.requestContactsPermission()
                    }
                }
                if (settings.searchContacts && !status.contactsPermission) {
                    item { SettingsBanner(R.drawable.ms_contacts, "Autoriser l'accès aux contacts", host::requestContactsPermission) }
                }
            }
            SettingsSection.NOTIFICATIONS -> SettingsPage("Notifications", onBack = back) {
                if (settings.badgesEnabled && !status.notificationAccess) {
                    item {
                        SettingsBanner(R.drawable.ms_app_badging, "Autoriser « Pastilles de notification Nimbus »") {
                            SystemActions.openNotificationAccessSettings(context)
                        }
                    }
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_app_badging, SettingsColors.Purple, "Pastilles de notification",
                        "Sur les icônes des apps qui ont des notifications",
                        checked = settings.badgesEnabled,
                    ) {
                        val enable = !settings.badgesEnabled
                        update { it.copy(badgesEnabled = enable) }
                        if (enable && !status.notificationAccess) SystemActions.openNotificationAccessSettings(context)
                    }
                }
                item {
                    SettingsEntry(R.drawable.ms_notifications, SettingsColors.Purple, "Style des pastilles", settings.badgeStyle.label, chevron = true) {
                        dialog = SettingsDialog.Badge
                    }
                }
            }
            SettingsSection.PRIVACY -> SettingsPage("Sécurité et confidentialité", onBack = back) {
                item {
                    val count = apps.hidden.size
                    SettingsEntry(
                        R.drawable.ms_visibility_off, SettingsColors.Teal, "Apps masquées",
                        (if (count == 0) "Aucune" else "$count masquée${if (count > 1) "s" else ""}") + " · protégé par code PIN",
                        chevron = true,
                    ) { dialog = if (status.hasPin) SettingsDialog.PinUnlock else SettingsDialog.PinCreate }
                }
                if (status.hasPin) {
                    item {
                        SettingsEntry(R.drawable.ms_password, SettingsColors.Teal, "Modifier le code PIN", chevron = true) {
                            dialog = SettingsDialog.PinChangeOld
                        }
                    }
                }
                apps.workProfile?.let { work ->
                    item {
                        SettingsEntry(
                            R.drawable.ms_work, SettingsColors.Teal, "Apps professionnelles",
                            if (work.quiet) "En pause" else "Actives",
                            checked = !work.quiet,
                        ) { vm.setProfileQuiet(work, !work.quiet) }
                    }
                }
                apps.privateSpace?.let { private ->
                    item {
                        SettingsEntry(
                            if (private.quiet) R.drawable.ms_lock else R.drawable.ms_lock_open, SettingsColors.Teal, "Espace privé",
                            if (private.quiet) "Verrouillé · toucher pour déverrouiller" else "Déverrouillé · toucher pour verrouiller",
                        ) { vm.setProfileQuiet(private, !private.quiet) }
                    }
                    item {
                        SettingsEntry(
                            R.drawable.ms_visibility_off, SettingsColors.Teal, "Masquer l'espace privé verrouillé",
                            "Accessible en tapant « espace privé » dans la recherche" +
                                if (private.hiddenWhenLocked) " (déjà masqué par le système)" else "",
                            enabled = !private.hiddenWhenLocked,
                            checked = settings.hidePrivateSpaceWhenLocked || private.hiddenWhenLocked,
                        ) { update { it.copy(hidePrivateSpaceWhenLocked = !it.hidePrivateSpaceWhenLocked) } }
                    }
                    item {
                        SettingsEntry(R.drawable.ms_settings, SettingsColors.Teal, "Réglages de l'espace privé", chevron = true, onClick = vm::openPrivateSpaceSettings)
                    }
                }
            }
            SettingsSection.ABOUT -> SettingsPage("À propos", onBack = back) {
                item {
                    val version = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        } catch (e: Exception) {
                            null
                        } ?: "?"
                    }
                    SettingsEntry(R.drawable.ms_info, SettingsColors.Gray, "Nimbus $version", "Launcher personnel") {}
                }
                item {
                    SettingsEntry(
                        R.drawable.ms_home, SettingsColors.Gray, "Application d'accueil",
                        if (status.isDefaultLauncher) "Nimbus est l'application d'accueil" else "Toucher pour définir Nimbus",
                        chevron = !status.isDefaultLauncher,
                    ) { host.requestDefaultLauncher() }
                }
                item {
                    SettingsEntry(R.drawable.ms_star, SettingsColors.Gray, "Icônes", "Material Symbols, Google (licence Apache 2.0)") {}
                }
            }
        }
    }

    SettingsDialogs(dialog, { dialog = it }, settings, status, apps, iconPacks.orEmpty(), vm, host, context)
}

/** Contenu de la page principale. */
private fun LazyListScope.mainPage(
    settings: LauncherSettings,
    status: SystemStatus,
    context: android.content.Context,
    host: LauncherHost,
    open: (SettingsSection) -> Unit,
) {
    if (!status.isDefaultLauncher) {
        item { SettingsBanner(R.drawable.ms_home, "Définir Nimbus comme application d'accueil", host::requestDefaultLauncher) }
    }
    if (settings.badgesEnabled && !status.notificationAccess) {
        item {
            SettingsBanner(R.drawable.ms_app_badging, "Voir le nombre de notifications sur les icônes") {
                SystemActions.openNotificationAccessSettings(context)
            }
        }
    }
    val needsService = Gesture.entries.any { settings.actionFor(it) in SERVICE_ACTIONS }
    if (needsService && !status.gestureService) {
        item {
            SettingsBanner(R.drawable.ms_accessibility_new, "Activer les gestes système (verrouillage…)") {
                SystemActions.openAccessibilitySettings(context)
            }
        }
    }
    item { Spacer(Modifier.height(8.dp)) }
    item { SettingsEntry(R.drawable.ms_wallpaper, SettingsColors.Green, "Fond d'écran") { SystemActions.openWallpaperPicker(context) } }
    item { SettingsEntry(R.drawable.ms_palette, SettingsColors.Orange, "Apparence globale") { open(SettingsSection.APPEARANCE) } }
    item { SettingsEntry(R.drawable.ms_swipe, SettingsColors.Red, "Gestes et raccourcis") { open(SettingsSection.GESTURES) } }
    item { SettingsEntry(R.drawable.ms_save, SettingsColors.Cyan, "Sauvegarde") { open(SettingsSection.BACKUP) } }

    settingsSection("Pages")
    item { SettingsEntry(R.drawable.ms_home, SettingsColors.Blue, "Page d'accueil", chevron = true) { open(SettingsSection.HOME) } }
    item { SettingsEntry(R.drawable.ms_apps, SettingsColors.Blue, "Page des applications", chevron = true) { open(SettingsSection.DRAWER) } }
    item { SettingsEntry(R.drawable.ms_search, SettingsColors.Blue, "Page de recherche", chevron = true) { open(SettingsSection.SEARCH) } }

    settingsSection("Autres")
    item { SettingsEntry(R.drawable.ms_notifications, SettingsColors.Purple, "Notifications") { open(SettingsSection.NOTIFICATIONS) } }
    item { SettingsEntry(R.drawable.ms_shield_lock, SettingsColors.Teal, "Sécurité et confidentialité") { open(SettingsSection.PRIVACY) } }
    item { SettingsEntry(R.drawable.ms_info, SettingsColors.Gray, "À propos") { open(SettingsSection.ABOUT) } }
}

/**
 * Aperçu de l'accueil (comme « Apparence globale » de Smart Launcher) : couleur du fond
 * d'écran, heure, vraies icônes avec la forme et le pack choisis, puis le thème.
 */
@Composable
private fun AppearancePreview(apps: AppsState, settings: LauncherSettings, wallpaperColor: Int?, onThemeClick: () -> Unit) {
    val seed = wallpaperColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val samples = remember(apps.favorites, apps.visible) { (apps.favorites + apps.visible).distinctBy { it.key }.take(4) }
    val time = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    SettingsCard {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(seed, lerp(seed, Color(0xFF1B2333), 0.75f))))
                    .padding(24.dp),
            ) {
                Text(time, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    for (app in samples) {
                        Image(app.icon, contentDescription = app.label, modifier = Modifier.size(settings.iconSize.dp.coerceAtMost(56).dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(painterResource(R.drawable.ms_search), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Rechercher", color = Color.White.copy(alpha = 0.85f))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .clickable(onClick = onThemeClick)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Thème du lanceur", fontSize = 13.sp, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f))
                    Text(settings.themeMode.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.inverseOnSurface)
                }
                Icon(painterResource(R.drawable.ms_expand_more), contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
    }
}

/** Tous les dialogues des réglages. */
@Composable
private fun SettingsDialogs(
    dialog: SettingsDialog?,
    setDialog: (SettingsDialog?) -> Unit,
    settings: LauncherSettings,
    status: SystemStatus,
    apps: AppsState,
    iconPacks: List<IconPackInfo>,
    vm: LauncherViewModel,
    host: LauncherHost,
    context: android.content.Context,
) {
    fun update(transform: (LauncherSettings) -> LauncherSettings) = vm.updateSettings(transform)
    val close = { setDialog(null) }
    when (dialog) {
        SettingsDialog.Theme -> ChoiceDialog(
            title = "Thème du lanceur",
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label },
            enabled = { it != ThemeMode.MATERIAL_YOU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
            onSelect = { mode -> update { it.copy(themeMode = mode) }; close() },
            onDismiss = close,
        )
        SettingsDialog.Shape -> ChoiceDialog(
            title = "Forme des icônes",
            options = IconShape.entries,
            selected = settings.iconShape,
            label = { it.label },
            onSelect = { shape -> update { it.copy(iconShape = shape) }; close() },
            onDismiss = close,
        )
        SettingsDialog.IconPackChoice -> ChoiceDialog(
            title = "Pack d'icônes",
            options = listOf<String?>(null) + iconPacks.map { it.packageName },
            selected = settings.iconPack,
            label = { pkg -> if (pkg == null) "Aucun (icônes d'origine)" else iconPacks.firstOrNull { it.packageName == pkg }?.label ?: pkg },
            onSelect = { pkg -> update { it.copy(iconPack = pkg) }; close() },
            onDismiss = close,
            footer = if (iconPacks.isEmpty()) "Aucun pack installé. Nimbus accepte les packs au format ADW / Nova du Play Store." else null,
        )
        SettingsDialog.Size -> ChoiceDialog(
            title = "Taille des icônes",
            options = IconSize.entries,
            selected = settings.iconSize,
            label = { it.label },
            onSelect = { size -> update { it.copy(iconSize = size) }; close() },
            onDismiss = close,
        )
        SettingsDialog.DrawerColumns -> ChoiceDialog(
            title = "Colonnes du tiroir",
            options = (3..6).toList(),
            selected = settings.drawerColumns,
            label = { "$it colonnes" },
            onSelect = { n -> update { it.copy(drawerColumns = n) }; close() },
            onDismiss = close,
        )
        SettingsDialog.Sort -> ChoiceDialog(
            title = "Tri des apps",
            options = SortMode.entries,
            selected = settings.sortMode,
            label = { it.label },
            onSelect = { mode -> update { it.copy(sortMode = mode) }; close() },
            onDismiss = close,
        )
        SettingsDialog.HomeColumns -> ChoiceDialog(
            title = "Colonnes de la grille d'accueil",
            options = (3..8).toList(),
            selected = settings.homeColumns,
            label = { "$it colonnes" },
            onSelect = { n -> update { it.copy(homeColumns = n) }; close() },
            onDismiss = close,
        )
        SettingsDialog.HomeRows -> ChoiceDialog(
            title = "Lignes de la grille d'accueil",
            options = (3..14).toList(),
            selected = settings.homeRows,
            label = { "$it lignes" },
            onSelect = { n -> update { it.copy(homeRows = n) }; close() },
            onDismiss = close,
        )
        SettingsDialog.DockColumns -> ChoiceDialog(
            title = "Icônes par rangée du dock",
            options = (4..6).toList(),
            selected = settings.dockColumns,
            label = { "$it icônes" },
            onSelect = { n -> update { it.copy(dockColumns = n) }; close() },
            onDismiss = close,
        )
        SettingsDialog.Badge -> ChoiceDialog(
            title = "Style des pastilles",
            options = BadgeStyle.entries,
            selected = settings.badgeStyle,
            label = { it.label },
            onSelect = { style -> update { it.copy(badgeStyle = style) }; close() },
            onDismiss = close,
        )
        SettingsDialog.Engine -> ChoiceDialog(
            title = "Moteur de recherche web",
            options = SearchEngine.entries,
            selected = settings.webSearchEngine,
            label = { it.label },
            onSelect = { engine -> update { it.copy(webSearchEngine = engine) }; close() },
            onDismiss = close,
        )
        is SettingsDialog.GestureChoice -> ChoiceDialog(
            title = dialog.gesture.label,
            options = GestureAction.entries,
            selected = settings.actionFor(dialog.gesture),
            label = { it.label },
            footer = if (!status.gestureService) {
                "Verrouillage, capture d'écran, applis récentes et menu Marche/Arrêt nécessitent le service d'accessibilité « Gestes Nimbus »."
            } else {
                null
            },
            onSelect = { action ->
                update { it.withAction(dialog.gesture, action) }
                setDialog(if (action == GestureAction.APP) SettingsDialog.GestureApp(dialog.gesture) else null)
                if (action in SERVICE_ACTIONS && !status.gestureService) {
                    SystemActions.toast(context, "Active « Gestes Nimbus » pour cette action")
                    SystemActions.openAccessibilitySettings(context)
                }
            },
            onDismiss = close,
        )
        is SettingsDialog.GestureApp -> AppPickerDialog(
            title = "${dialog.gesture.label} : ouvrir…",
            apps = apps.visible,
            onPick = { app ->
                vm.setGestureApp(dialog.gesture, app)
                close()
            },
            onDismiss = close,
        )
        SettingsDialog.ConfirmImport -> AlertDialog(
            onDismissRequest = close,
            title = { Text("Importer une configuration") },
            text = {
                Text(
                    "L'import remplace l'accueil, le dock, les catégories, les apps masquées, les noms et les réglages " +
                        "par ceux du fichier. Exporte d'abord ta configuration actuelle si tu veux pouvoir y revenir.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    close()
                    host.importBackup()
                }) { Text("Choisir le fichier") }
            },
            dismissButton = { TextButton(onClick = close) { Text("Annuler") } },
        )
        SettingsDialog.PinUnlock -> PinDialog(
            title = "Apps masquées",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = {
                close()
                vm.openHiddenApps()
            },
            onDismiss = close,
        )
        SettingsDialog.PinCreate -> PinDialog(
            title = "Protéger les apps masquées",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                close()
                vm.openHiddenApps()
            },
            onDismiss = close,
        )
        SettingsDialog.PinChangeOld -> PinDialog(
            title = "Code actuel",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = { setDialog(SettingsDialog.PinChangeNew) },
            onDismiss = close,
        )
        SettingsDialog.PinChangeNew -> PinDialog(
            title = "Nouveau code",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                close()
                SystemActions.toast(context, "Code PIN modifié")
            },
            onDismiss = close,
        )
        null -> Unit
    }
}

/** Nom lisible d'un dossier SAF (« primary:Download/Nimbus » -> « Download/Nimbus »). */
private fun folderName(treeUri: String): String = try {
    DocumentsContract.getTreeDocumentId(Uri.parse(treeUri)).substringAfter(':').ifEmpty { "stockage" }
} catch (e: Exception) {
    "dossier choisi"
}
