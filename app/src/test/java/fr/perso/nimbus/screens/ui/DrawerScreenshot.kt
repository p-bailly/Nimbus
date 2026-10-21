package fr.perso.nimbus.screens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.UiState
import fr.perso.nimbus.ui.components.PaperIconContent
import fr.perso.nimbus.ui.drawer.AppDrawer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class DrawerScreenshot {

    /** Le vrai tiroir : recherche, onglets, « Fréquentes », grille et défilement A–Z (27 apps fictives). */
    @Test
    fun drawer() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("drawer") {
            AppDrawer(
                vm = vm,
                ui = UiState(drawerOpen = true),
                settings = LauncherSettings(themeMode = ThemeMode.PAPER),
                apps = UiFixture.appsState(),
                host = FakeHost,
            )
        }
    }

    /** Recherche en cours (le modèle de vue n'a pas d'apps : aucun résultat, puis Play Store et web). */
    @Test
    fun search() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("drawer_search") {
            AppDrawer(
                vm = vm,
                ui = UiState(drawerOpen = true, query = "wha"),
                settings = LauncherSettings(themeMode = ThemeMode.PAPER),
                apps = UiFixture.appsState(),
                host = FakeHost,
            )
        }
    }

    /** Pastilles du tiroir aux trois tailles, avec et sans notification. */
    @Test
    fun icons() = UiFixture.shoot("drawer_icons") {
        Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            for (size in listOf(44, 52, 60)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for ((i, app) in UiFixture.apps.take(5).withIndex()) {
                        Box(Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                            PaperIconContent(app, size, showLabel = true, dot = i % 2 == 1)
                        }
                    }
                }
            }
        }
    }
}
