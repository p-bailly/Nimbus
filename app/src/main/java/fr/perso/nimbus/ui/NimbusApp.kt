package fr.perso.nimbus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.ui.components.AppActionSheet
import fr.perso.nimbus.ui.components.AppPopup
import fr.perso.nimbus.ui.components.WidgetPicker
import fr.perso.nimbus.ui.drawer.AppDrawer
import fr.perso.nimbus.ui.home.HomeScreen
import fr.perso.nimbus.ui.settings.HiddenAppsScreen
import fr.perso.nimbus.ui.settings.SettingsScreen
import fr.perso.nimbus.ui.theme.NimbusTheme
import fr.perso.nimbus.ui.theme.isDarkTheme

/** Racine de l'interface : accueil, et par-dessus tiroir, réglages, feuilles et dialogues. */
@Composable
fun NimbusApp(vm: LauncherViewModel, host: LauncherHost) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val wallpaperColor by vm.wallpaperColor.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val importResult by vm.importResult.collectAsStateWithLifecycle()

    val dark = isDarkTheme(settings.themeMode, isSystemInDarkTheme())
    val onWallpaper = !ui.drawerOpen && ui.screen == Screen.HOME
    // Sur le fond d'écran les icônes système restent claires ; ailleurs elles suivent le thème.
    LaunchedEffect(dark, onWallpaper) { host.setLightSystemBars(!onWallpaper && !dark) }

    // Un launcher ne se ferme pas avec Retour : ce gestionnaire, déclaré en premier,
    // n'est utilisé que si aucun écran au-dessus n'intercepte le geste.
    BackHandler {}

    // Glisser-déposer sur l'accueil (icônes, widgets, apps du tiroir) : le lâcher est géré par l'accueil.
    val homeDrag = remember { HomeDragState() }

    NimbusTheme(mode = settings.themeMode, wallpaperColor = wallpaperColor) {
        CompositionLocalProvider(LocalHomeDrag provides homeDrag) {
            // Couleurs des icônes à thème, à la manière du Pixel : fond pastel / motif accentué.
            val scheme = MaterialTheme.colorScheme
            val themedBackground = if (dark) scheme.surfaceContainerHighest else scheme.primaryContainer
            val themedForeground = scheme.primary
            LaunchedEffect(themedBackground, themedForeground) {
                vm.setThemedIconColors(themedBackground.toArgb(), themedForeground.toArgb())
            }

            Box(Modifier.fillMaxSize()) {
                HomeScreen(vm = vm, ui = ui, settings = settings, apps = apps, active = onWallpaper)

                AnimatedVisibility(
                    visible = ui.drawerOpen,
                    enter = slideInVertically { it / 3 } + fadeIn(),
                    exit = slideOutVertically { it / 3 } + fadeOut(),
                ) {
                    AppDrawer(vm = vm, ui = ui, settings = settings, apps = apps, host = host)
                }

                AnimatedVisibility(
                    visible = ui.screen == Screen.SETTINGS,
                    enter = slideInHorizontally { it / 4 } + fadeIn(),
                    exit = slideOutHorizontally { it / 4 } + fadeOut(),
                ) {
                    SettingsScreen(vm = vm, host = host, section = ui.settingsSection)
                }

                AnimatedVisibility(
                    visible = ui.screen == Screen.HIDDEN_APPS,
                    enter = slideInHorizontally { it / 4 } + fadeIn(),
                    exit = slideOutHorizontally { it / 4 } + fadeOut(),
                ) {
                    HiddenAppsScreen(vm = vm, apps = apps, settings = settings)
                }

                // L'app de la feuille est rafraîchie (nom, icône) ; si elle a disparu (espace privé
                // verrouillé, désinstallation), la feuille n'est plus affichée.
                ui.appPopup?.let { request ->
                    apps.byKey[request.app.key]?.let { app -> AppPopup(app, request.anchor, apps, vm) }
                }

                ui.sheetApp?.let { apps.byKey[it.key] }?.let { app ->
                    AppActionSheet(app = app, apps = apps, vm = vm, host = host)
                }

                importResult?.let { result ->
                    AlertDialog(
                        onDismissRequest = vm::dismissImportResult,
                        title = { Text(result.title) },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                for (line in result.details) {
                                    Text("• $line", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
                                }
                            }
                        },
                        confirmButton = {
                            if (result.widgets.isNotEmpty()) {
                                TextButton(onClick = {
                                    vm.dismissImportResult()
                                    host.addWidgets(result.widgets)
                                }) { Text("Rajouter les widgets") }
                            } else {
                                TextButton(onClick = vm::dismissImportResult) { Text("OK") }
                            }
                        },
                        dismissButton = if (result.widgets.isNotEmpty()) {
                            { TextButton(onClick = vm::dismissImportResult) { Text("Plus tard") } }
                        } else {
                            null
                        },
                    )
                }

                HomeDragLayer(homeDrag, settings.iconSize.dp)

                if (ui.widgetPickerOpen) {
                    WidgetPicker(
                        onDismiss = vm::closeWidgetPicker,
                        onPick = { info ->
                            vm.closeWidgetPicker()
                            host.addWidget(info)
                        },
                    )
                }
            }
        }
    }
}
