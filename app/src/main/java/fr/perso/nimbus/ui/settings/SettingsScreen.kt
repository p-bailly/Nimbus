package fr.perso.nimbus.ui.settings

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.AppPickerDialog
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.components.PinDialog
import fr.perso.nimbus.ui.components.PinMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: LauncherViewModel, host: LauncherHost) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val gestureApps by vm.gestureApps.collectAsStateWithLifecycle()
    val backupFolder by vm.autoBackupFolder.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val iconPacks by produceState<List<IconPackInfo>?>(null) {
        value = withContext(Dispatchers.IO) { IconPack.installed(context) }
    }
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        } ?: "?"
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings) = vm.updateSettings(transform)

    BackHandler { vm.closeScreen() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres Nimbus") },
                navigationIcon = {
                    IconButton(onClick = vm::closeScreen) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!status.isDefaultLauncher) {
                item { DefaultLauncherCard(onClick = host::requestDefaultLauncher) }
            }

            section("Apparence")
            item { ClickRow("Thème", settings.themeMode.label) { dialog = SettingsDialog.Theme } }
            item {
                ClickRow("Fond d'écran", "Choisir une image ou un fond animé") {
                    SystemActions.openWallpaperPicker(context)
                }
            }

            section("Écran d'accueil")
            item {
                ClickRow("Colonnes de la grille", "${settings.homeColumns} · appui long sur une icône puis glisser pour la déplacer") {
                    dialog = SettingsDialog.HomeColumns
                }
            }
            item { ClickRow("Lignes de la grille", "${settings.homeRows}") { dialog = SettingsDialog.HomeRows } }
            item {
                SwitchRow(
                    title = "Noms sous les icônes de l'accueil",
                    checked = settings.homeShowLabels,
                    onCheckedChange = { checked -> update { it.copy(homeShowLabels = checked) } },
                )
            }
            item { ClickRow("Icônes par rangée du dock", "${settings.dockColumns}") { dialog = SettingsDialog.DockColumns } }
            item {
                SwitchRow(
                    title = "Horloge et date",
                    checked = settings.showClock,
                    onCheckedChange = { checked -> update { it.copy(showClock = checked) } },
                )
            }
            item {
                SwitchRow(
                    title = "Niveau de batterie",
                    checked = settings.showBattery,
                    onCheckedChange = { checked -> update { it.copy(showBattery = checked) } },
                )
            }
            item {
                SwitchRow(
                    title = "Barre de recherche",
                    subtitle = "En bas de l'accueil",
                    checked = settings.showSearchBar,
                    onCheckedChange = { checked -> update { it.copy(showSearchBar = checked) } },
                )
            }

            section("Icônes")
            item { ClickRow("Forme", settings.iconShape.label) { dialog = SettingsDialog.Shape } }
            item {
                val packLabel = settings.iconPack?.let { pkg ->
                    iconPacks?.firstOrNull { it.packageName == pkg }?.label ?: pkg
                } ?: "Aucun (icônes d'origine)"
                ClickRow("Pack d'icônes", packLabel) { dialog = SettingsDialog.IconPackChoice }
            }
            item {
                val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                SwitchRow(
                    title = "Icônes à thème",
                    subtitle = if (supported) "Icônes monochromes aux couleurs du thème, comme sur Pixel"
                    else "Nécessite Android 13",
                    checked = settings.themedIcons && supported,
                    enabled = supported,
                    onCheckedChange = { checked -> update { it.copy(themedIcons = checked) } },
                )
            }
            item {
                SwitchRow(
                    title = "Uniformiser les anciennes icônes",
                    subtitle = "Pose les icônes non adaptatives sur un fond de la forme choisie",
                    checked = settings.wrapLegacyIcons,
                    onCheckedChange = { checked -> update { it.copy(wrapLegacyIcons = checked) } },
                )
            }
            item { ClickRow("Taille", settings.iconSize.label) { dialog = SettingsDialog.Size } }

            section("Tiroir d'applications")
            item { ClickRow("Colonnes", "${settings.drawerColumns}") { dialog = SettingsDialog.DrawerColumns } }
            item {
                SwitchRow(
                    title = "Afficher le nom des apps",
                    checked = settings.showLabels,
                    onCheckedChange = { checked -> update { it.copy(showLabels = checked) } },
                )
            }
            item { ClickRow("Tri", settings.sortMode.label) { dialog = SettingsDialog.Sort } }
            item {
                SwitchRow(
                    title = "Apps fréquentes en haut du tiroir",
                    subtitle = "En tri alphabétique",
                    checked = settings.showFrequentApps,
                    onCheckedChange = { checked -> update { it.copy(showFrequentApps = checked) } },
                )
            }
            item {
                SwitchRow(
                    title = "Catégories automatiques",
                    subtitle = "Range les apps dans des onglets ; appui long sur une app pour la déplacer",
                    checked = settings.autoCategories,
                    onCheckedChange = { checked -> update { it.copy(autoCategories = checked) } },
                )
            }

            section("Notifications")
            item {
                SwitchRow(
                    title = "Pastilles de notification",
                    subtitle = if (status.notificationAccess) "Sur les icônes des apps qui ont des notifications"
                    else "Nécessite l'accès aux notifications",
                    checked = settings.badgesEnabled,
                    onCheckedChange = { checked ->
                        update { it.copy(badgesEnabled = checked) }
                        if (checked && !status.notificationAccess) SystemActions.openNotificationAccessSettings(context)
                    },
                )
            }
            item { ClickRow("Style des pastilles", settings.badgeStyle.label) { dialog = SettingsDialog.Badge } }
            if (settings.badgesEnabled && !status.notificationAccess) {
                item {
                    ClickRow("Autoriser l'accès aux notifications", "Activer « Pastilles de notification Nimbus »") {
                        SystemActions.openNotificationAccessSettings(context)
                    }
                }
            }

            section("Recherche")
            item { ClickRow("Moteur de recherche web", settings.webSearchEngine.label) { dialog = SettingsDialog.Engine } }
            item {
                SwitchRow(
                    title = "Rechercher dans les contacts",
                    subtitle = if (settings.searchContacts && !status.contactsPermission) "Autorisation d'accès aux contacts requise"
                    else "Affiche les contacts dans les résultats",
                    checked = settings.searchContacts,
                    onCheckedChange = { checked ->
                        update { it.copy(searchContacts = checked) }
                        if (checked && !status.contactsPermission) host.requestContactsPermission()
                    },
                )
            }
            if (settings.searchContacts && !status.contactsPermission) {
                item { ClickRow("Autoriser l'accès aux contacts", null, onClick = host::requestContactsPermission) }
            }

            section("Gestes")
            for (gesture in Gesture.entries) {
                item(key = "gesture:${gesture.name}") {
                    val action = settings.actionFor(gesture)
                    val subtitle = if (action == GestureAction.APP) {
                        "Ouvrir " + (gestureApps[gesture]?.let { apps.byKey[it]?.label } ?: "… (aucune app choisie)")
                    } else {
                        action.label
                    }
                    ClickRow(gesture.label, subtitle) { dialog = SettingsDialog.GestureChoice(gesture) }
                }
            }
            item {
                ClickRow(
                    "Service « Gestes Nimbus »",
                    if (status.gestureService) "Activé"
                    else "Désactivé : nécessaire pour verrouiller, capture d'écran, applis récentes… (accessibilité)",
                ) { SystemActions.openAccessibilitySettings(context) }
            }

            val work = apps.workProfile
            val private = apps.privateSpace
            if (work != null || private != null) {
                section("Profils")
                if (work != null) {
                    item {
                        SwitchRow(
                            title = "Apps professionnelles",
                            subtitle = if (work.quiet) "En pause : notifications et apps pro désactivées" else "Actives",
                            checked = !work.quiet,
                            onCheckedChange = { active -> vm.setProfileQuiet(work, !active) },
                        )
                    }
                }
                if (private != null) {
                    item {
                        ClickRow(
                            "Espace privé",
                            if (private.quiet) "Verrouillé · toucher pour déverrouiller" else "Déverrouillé · toucher pour verrouiller",
                        ) { vm.setProfileQuiet(private, !private.quiet) }
                    }
                    item {
                        SwitchRow(
                            title = "Masquer l'espace privé verrouillé",
                            subtitle = "Il reste accessible en tapant « espace privé » dans la recherche" +
                                if (private.hiddenWhenLocked) " (déjà masqué par le réglage système)" else "",
                            checked = settings.hidePrivateSpaceWhenLocked || private.hiddenWhenLocked,
                            enabled = !private.hiddenWhenLocked,
                            onCheckedChange = { checked -> update { it.copy(hidePrivateSpaceWhenLocked = checked) } },
                        )
                    }
                    item { ClickRow("Réglages de l'espace privé", null, onClick = vm::openPrivateSpaceSettings) }
                }
            }

            section("Confidentialité")
            item {
                val count = apps.hidden.size
                ClickRow(
                    "Apps masquées",
                    (if (count == 0) "Aucune app masquée" else "$count app${if (count > 1) "s" else ""} masquée${if (count > 1) "s" else ""}") +
                        " · protégé par code PIN",
                ) { dialog = if (status.hasPin) SettingsDialog.PinUnlock else SettingsDialog.PinCreate }
            }
            if (status.hasPin) {
                item { ClickRow("Modifier le code PIN", null) { dialog = SettingsDialog.PinChangeOld } }
            }

            section("Sauvegarde")
            item {
                ClickRow("Exporter la configuration", "Réglages, accueil, dock, catégories, noms, icônes, gestes (fichier .json)") {
                    host.exportBackup()
                }
            }
            item {
                ClickRow("Importer une configuration", "Sauvegarde Nimbus (.json) ou Smart Launcher (.slbk)") {
                    dialog = SettingsDialog.ConfirmImport
                }
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
                ClickRow("Sauvegarde automatique", subtitle, onClick = host::chooseAutoBackupFolder)
            }
            if (backupFolder != null) {
                item { ClickRow("Désactiver la sauvegarde automatique", null) { vm.setAutoBackupFolder(null) } }
            }

            section("À propos")
            item {
                ClickRow("Nimbus $version", if (status.isDefaultLauncher) "Launcher par défaut" else "Pas le launcher par défaut") {
                    host.requestDefaultLauncher()
                }
            }
        }
    }

    when (val d = dialog) {
        SettingsDialog.Theme -> ChoiceDialog(
            title = "Thème",
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label },
            enabled = { it != ThemeMode.MATERIAL_YOU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
            onSelect = { mode -> update { it.copy(themeMode = mode) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Shape -> ChoiceDialog(
            title = "Forme des icônes",
            options = IconShape.entries,
            selected = settings.iconShape,
            label = { it.label },
            onSelect = { shape -> update { it.copy(iconShape = shape) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.IconPackChoice -> {
            val packs = iconPacks.orEmpty()
            ChoiceDialog(
                title = "Pack d'icônes",
                options = listOf<String?>(null) + packs.map { it.packageName },
                selected = settings.iconPack,
                label = { pkg -> if (pkg == null) "Aucun (icônes d'origine)" else packs.firstOrNull { it.packageName == pkg }?.label ?: pkg },
                onSelect = { pkg -> update { it.copy(iconPack = pkg) }; dialog = null },
                onDismiss = { dialog = null },
                footer = if (packs.isEmpty()) "Aucun pack installé. Nimbus accepte les packs au format ADW / Nova du Play Store." else null,
            )
        }
        SettingsDialog.Size -> ChoiceDialog(
            title = "Taille des icônes",
            options = IconSize.entries,
            selected = settings.iconSize,
            label = { it.label },
            onSelect = { size -> update { it.copy(iconSize = size) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.DrawerColumns -> ChoiceDialog(
            title = "Colonnes du tiroir",
            options = (3..6).toList(),
            selected = settings.drawerColumns,
            label = { "$it colonnes" },
            onSelect = { n -> update { it.copy(drawerColumns = n) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Sort -> ChoiceDialog(
            title = "Tri des apps",
            options = SortMode.entries,
            selected = settings.sortMode,
            label = { it.label },
            onSelect = { mode -> update { it.copy(sortMode = mode) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.HomeColumns -> ChoiceDialog(
            title = "Colonnes de la grille d'accueil",
            options = (3..8).toList(),
            selected = settings.homeColumns,
            label = { "$it colonnes" },
            onSelect = { n -> update { it.copy(homeColumns = n) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.HomeRows -> ChoiceDialog(
            title = "Lignes de la grille d'accueil",
            options = (3..14).toList(),
            selected = settings.homeRows,
            label = { "$it lignes" },
            onSelect = { n -> update { it.copy(homeRows = n) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.DockColumns -> ChoiceDialog(
            title = "Icônes par rangée du dock",
            options = (4..6).toList(),
            selected = settings.dockColumns,
            label = { "$it icônes" },
            onSelect = { n -> update { it.copy(dockColumns = n) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Badge -> ChoiceDialog(
            title = "Style des pastilles",
            options = BadgeStyle.entries,
            selected = settings.badgeStyle,
            label = { it.label },
            onSelect = { style -> update { it.copy(badgeStyle = style) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Engine -> ChoiceDialog(
            title = "Moteur de recherche web",
            options = SearchEngine.entries,
            selected = settings.webSearchEngine,
            label = { it.label },
            onSelect = { engine -> update { it.copy(webSearchEngine = engine) }; dialog = null },
            onDismiss = { dialog = null },
        )
        is SettingsDialog.GestureChoice -> ChoiceDialog(
            title = d.gesture.label,
            options = GestureAction.entries,
            selected = settings.actionFor(d.gesture),
            label = { it.label },
            footer = if (!status.gestureService) {
                "Verrouillage, capture d'écran, applis récentes et menu Marche/Arrêt nécessitent le service d'accessibilité « Gestes Nimbus »."
            } else {
                null
            },
            onSelect = { action ->
                update { it.withAction(d.gesture, action) }
                dialog = if (action == GestureAction.APP) SettingsDialog.GestureApp(d.gesture) else null
                val needsService = action in setOf(
                    GestureAction.LOCK, GestureAction.RECENTS, GestureAction.SCREENSHOT, GestureAction.POWER_MENU,
                )
                if (needsService && !status.gestureService) {
                    SystemActions.toast(context, "Active « Gestes Nimbus » pour cette action")
                    SystemActions.openAccessibilitySettings(context)
                }
            },
            onDismiss = { dialog = null },
        )
        is SettingsDialog.GestureApp -> AppPickerDialog(
            title = "${d.gesture.label} : ouvrir…",
            apps = apps.visible,
            onPick = { app ->
                vm.setGestureApp(d.gesture, app)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ConfirmImport -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Importer une configuration") },
            text = {
                Text(
                    "L'import remplace l'accueil, le dock, les catégories, les apps masquées, les noms et les réglages " +
                        "par ceux du fichier. Exporte d'abord ta configuration actuelle si tu veux pouvoir y revenir.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    host.importBackup()
                }) { Text("Choisir le fichier") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Annuler") } },
        )
        SettingsDialog.PinUnlock -> PinDialog(
            title = "Apps masquées",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = {
                dialog = null
                vm.openHiddenApps()
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PinCreate -> PinDialog(
            title = "Protéger les apps masquées",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                dialog = null
                vm.openHiddenApps()
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PinChangeOld -> PinDialog(
            title = "Code actuel",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = { dialog = SettingsDialog.PinChangeNew },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PinChangeNew -> PinDialog(
            title = "Nouveau code",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                dialog = null
                SystemActions.toast(context, "Code PIN modifié")
            },
            onDismiss = { dialog = null },
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

@Composable
private fun DefaultLauncherCard(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Nimbus n'est pas le launcher par défaut",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Nécessaire pour le bouton Accueil, les raccourcis d'apps et l'espace privé",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onClick) { Text("Définir") }
        }
    }
}

private fun LazyListScope.section(title: String) {
    item(key = "section:$title") {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun ClickRow(title: String, subtitle: String?, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
