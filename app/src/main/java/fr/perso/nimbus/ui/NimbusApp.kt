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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.ui.components.AppActionSheet
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
    val overrides by vm.categoryOverrides.collectAsStateWithLifecycle()

    val dark = isDarkTheme(settings.themeMode, isSystemInDarkTheme())
    val onWallpaper = !ui.drawerOpen && ui.screen == Screen.HOME
    // Sur le fond d'écran les icônes système restent claires ; ailleurs elles suivent le thème.
    LaunchedEffect(dark, onWallpaper) { host.setLightSystemBars(!onWallpaper && !dark) }

    // Un launcher ne se ferme pas avec Retour : ce gestionnaire, déclaré en premier,
    // n'est utilisé que si aucun écran au-dessus n'intercepte le geste.
    BackHandler {}

    NimbusTheme(mode = settings.themeMode, wallpaperColor = wallpaperColor) {
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
                SettingsScreen(vm = vm, host = host)
            }

            AnimatedVisibility(
                visible = ui.screen == Screen.HIDDEN_APPS,
                enter = slideInHorizontally { it / 4 } + fadeIn(),
                exit = slideOutHorizontally { it / 4 } + fadeOut(),
            ) {
                HiddenAppsScreen(vm = vm, apps = apps, settings = settings)
            }

            ui.sheetApp?.let { app ->
                AppActionSheet(
                    app = app,
                    isFavorite = app.key in apps.favoriteKeys,
                    isHidden = app.key in apps.hiddenKeys,
                    categoryOverride = overrides[app.key],
                    onDismiss = vm::dismissAppSheet,
                    onShortcut = vm::startShortcut,
                    onToggleFavorite = { vm.toggleFavorite(app) },
                    onSetCategory = { vm.setCategory(app, it) },
                    onToggleHidden = { vm.setHidden(app, app.key !in apps.hiddenKeys) },
                    onAppInfo = { vm.appInfo(app) },
                    onUninstall = { vm.uninstall(app) },
                )
            }

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
