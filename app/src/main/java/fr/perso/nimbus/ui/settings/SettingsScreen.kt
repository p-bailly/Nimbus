package fr.perso.nimbus.ui.settings

import android.os.Build
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
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.IconPack
import fr.perso.nimbus.data.IconPackInfo
import fr.perso.nimbus.data.IconShape
import fr.perso.nimbus.data.IconSize
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.SortMode
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.components.PinDialog
import fr.perso.nimbus.ui.components.PinMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dialogues ouverts depuis les réglages. */
private enum class SettingsDialog {
    THEME, SHAPE, ICON_PACK, ICON_SIZE, COLUMNS, SORT,
    DOUBLE_TAP, SWIPE_UP, SWIPE_DOWN,
    PIN_UNLOCK, PIN_CREATE, PIN_CHANGE_OLD, PIN_CHANGE_NEW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: LauncherViewModel, host: LauncherHost) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
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
            item {
                ClickRow("Thème", settings.themeMode.label) { dialog = SettingsDialog.THEME }
            }
            item {
                ClickRow("Fond d'écran", "Choisir une image ou un fond animé") {
                    SystemActions.openWallpaperPicker(context)
                }
            }

            section("Icônes")
            item { ClickRow("Forme", settings.iconShape.label) { dialog = SettingsDialog.SHAPE } }
            item {
                val packLabel = settings.iconPack?.let { pkg ->
                    iconPacks?.firstOrNull { it.packageName == pkg }?.label ?: pkg
                } ?: "Aucun (icônes d'origine)"
                ClickRow("Pack d'icônes", packLabel) { dialog = SettingsDialog.ICON_PACK }
            }
            item {
                SwitchRow(
                    title = "Uniformiser les anciennes icônes",
                    subtitle = "Pose les icônes non adaptatives sur un fond de la forme choisie",
                    checked = settings.wrapLegacyIcons,
                    onCheckedChange = { checked -> update { it.copy(wrapLegacyIcons = checked) } },
                )
            }
            item { ClickRow("Taille", settings.iconSize.label) { dialog = SettingsDialog.ICON_SIZE } }

            section("Tiroir d'applications")
            item { ClickRow("Colonnes", "${settings.drawerColumns}") { dialog = SettingsDialog.COLUMNS } }
            item {
                SwitchRow(
                    title = "Afficher le nom des apps",
                    checked = settings.showLabels,
                    onCheckedChange = { checked -> update { it.copy(showLabels = checked) } },
                )
            }
            item { ClickRow("Tri", settings.sortMode.label) { dialog = SettingsDialog.SORT } }
            item {
                SwitchRow(
                    title = "Catégories automatiques",
                    subtitle = "Range les apps dans des onglets ; appui long sur une app pour la déplacer",
                    checked = settings.autoCategories,
                    onCheckedChange = { checked -> update { it.copy(autoCategories = checked) } },
                )
            }

            section("Écran d'accueil")
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

            section("Notifications")
            item {
                SwitchRow(
                    title = "Pastilles de notification",
                    subtitle = if (status.notificationAccess) "Nombre de notifications sur les icônes"
                    else "Nécessite l'accès aux notifications",
                    checked = settings.badgesEnabled,
                    onCheckedChange = { checked ->
                        update { it.copy(badgesEnabled = checked) }
                        if (checked && !status.notificationAccess) SystemActions.openNotificationAccessSettings(context)
                    },
                )
            }
            if (settings.badgesEnabled && !status.notificationAccess) {
                item {
                    ClickRow("Autoriser l'accès aux notifications", "Activer « Pastilles de notification Nimbus »") {
                        SystemActions.openNotificationAccessSettings(context)
                    }
                }
            }

            section("Recherche")
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
            item { ClickRow("Double tap", settings.doubleTapAction.label) { dialog = SettingsDialog.DOUBLE_TAP } }
            item { ClickRow("Glisser vers le haut", settings.swipeUpAction.label) { dialog = SettingsDialog.SWIPE_UP } }
            item { ClickRow("Glisser vers le bas", settings.swipeDownAction.label) { dialog = SettingsDialog.SWIPE_DOWN } }
            item {
                ClickRow(
                    "Service « Gestes Nimbus »",
                    if (status.gestureService) "Activé"
                    else "Désactivé : nécessaire pour verrouiller l'écran (réglages d'accessibilité)",
                ) { SystemActions.openAccessibilitySettings(context) }
            }

            section("Confidentialité")
            item {
                val count = apps.hidden.size
                ClickRow(
                    "Apps masquées",
                    (if (count == 0) "Aucune app masquée" else "$count app${if (count > 1) "s" else ""} masquée${if (count > 1) "s" else ""}") +
                        " · protégé par code PIN",
                ) { dialog = if (status.hasPin) SettingsDialog.PIN_UNLOCK else SettingsDialog.PIN_CREATE }
            }
            if (status.hasPin) {
                item { ClickRow("Modifier le code PIN", null) { dialog = SettingsDialog.PIN_CHANGE_OLD } }
            }

            section("À propos")
            item {
                ClickRow("Nimbus $version", if (status.isDefaultLauncher) "Launcher par défaut" else "Pas le launcher par défaut") {
                    host.requestDefaultLauncher()
                }
            }
        }
    }

    when (dialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = "Thème",
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label },
            enabled = { it != ThemeMode.MATERIAL_YOU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
            onSelect = { mode -> update { it.copy(themeMode = mode) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.SHAPE -> ChoiceDialog(
            title = "Forme des icônes",
            options = IconShape.entries,
            selected = settings.iconShape,
            label = { it.label },
            onSelect = { shape -> update { it.copy(iconShape = shape) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.ICON_PACK -> {
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
        SettingsDialog.ICON_SIZE -> ChoiceDialog(
            title = "Taille des icônes",
            options = IconSize.entries,
            selected = settings.iconSize,
            label = { it.label },
            onSelect = { size -> update { it.copy(iconSize = size) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.COLUMNS -> ChoiceDialog(
            title = "Colonnes du tiroir",
            options = (3..6).toList(),
            selected = settings.drawerColumns,
            label = { "$it colonnes" },
            onSelect = { n -> update { it.copy(drawerColumns = n) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.SORT -> ChoiceDialog(
            title = "Tri des apps",
            options = SortMode.entries,
            selected = settings.sortMode,
            label = { it.label },
            onSelect = { mode -> update { it.copy(sortMode = mode) }; dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.DOUBLE_TAP, SettingsDialog.SWIPE_UP, SettingsDialog.SWIPE_DOWN -> {
            val which = dialog
            val (title, current) = when (which) {
                SettingsDialog.DOUBLE_TAP -> "Double tap" to settings.doubleTapAction
                SettingsDialog.SWIPE_UP -> "Glisser vers le haut" to settings.swipeUpAction
                else -> "Glisser vers le bas" to settings.swipeDownAction
            }
            ChoiceDialog(
                title = title,
                options = GestureAction.entries,
                selected = current,
                label = { it.label },
                footer = if (!status.gestureService) "Le verrouillage nécessite le service d'accessibilité « Gestes Nimbus »." else null,
                onSelect = { action ->
                    update {
                        when (which) {
                            SettingsDialog.DOUBLE_TAP -> it.copy(doubleTapAction = action)
                            SettingsDialog.SWIPE_UP -> it.copy(swipeUpAction = action)
                            else -> it.copy(swipeDownAction = action)
                        }
                    }
                    dialog = null
                    if (action == GestureAction.LOCK && !status.gestureService) {
                        SystemActions.toast(context, "Active « Gestes Nimbus » pour verrouiller l'écran")
                        SystemActions.openAccessibilitySettings(context)
                    }
                },
                onDismiss = { dialog = null },
            )
        }
        SettingsDialog.PIN_UNLOCK -> PinDialog(
            title = "Apps masquées",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = {
                dialog = null
                vm.openHiddenApps()
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PIN_CREATE -> PinDialog(
            title = "Protéger les apps masquées",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                dialog = null
                vm.openHiddenApps()
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PIN_CHANGE_OLD -> PinDialog(
            title = "Code actuel",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = { dialog = SettingsDialog.PIN_CHANGE_NEW },
            onDismiss = { dialog = null },
        )
        SettingsDialog.PIN_CHANGE_NEW -> PinDialog(
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
                    "Nécessaire pour le bouton Accueil et les raccourcis d'apps",
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
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
