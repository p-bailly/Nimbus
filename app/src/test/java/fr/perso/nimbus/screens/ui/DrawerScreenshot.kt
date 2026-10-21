package fr.perso.nimbus.screens.ui

import android.os.Process
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.ProfileState
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.UiState
import fr.perso.nimbus.ui.components.PaperIconContent
import fr.perso.nimbus.ui.drawer.AppDrawer
import fr.perso.nimbus.ui.drawer.TocPage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class DrawerScreenshot {

    /** Le vrai tiroir en sommaire : recherche, « Fréquentes », une section par catégorie (27 apps fictives). */
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

    /**
     * Fin du sommaire : notifications en appels de note (WhatsApp, Gmail) et espace privé déverrouillé
     * en annexe A, avec ses actions.
     */
    @Test
    fun drawerTocEnd() {
        val vm = UiFixture.viewModel()
        val state = UiFixture.appsState()
        val banque = UiFixture.app("Banque Populaire")
        val apps = state.copy(
            visible = state.visible.filter { it.category == AppCategory.COMMUNICATION },
            categories = listOf(AppCategory.COMMUNICATION, AppCategory.TRAVEL),
            frequent = emptyList(),
            privateSpace = ProfileState(Process.myUserHandle(), ProfileType.PRIVATE, quiet = false),
            privateApps = listOf(banque.copy(key = "private:" + banque.key, profile = ProfileType.PRIVATE)),
        )
        val badges = mapOf(UiFixture.app("WhatsApp").packageName to 3, UiFixture.app("Gmail").packageName to 12)
        UiFixture.shoot("drawer_toc_end") {
            TocPage(apps = apps, settings = LauncherSettings(themeMode = ThemeMode.PAPER), badges = badges, vm = vm)
        }
    }

    /** Le tiroir en grille : onglets, « Fréquentes », grille et défilement A–Z. */
    @Test
    fun drawerGrid() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("drawer_grid") {
            AppDrawer(
                vm = vm,
                ui = UiState(drawerOpen = true),
                settings = LauncherSettings(themeMode = ThemeMode.PAPER, drawerToc = false),
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
