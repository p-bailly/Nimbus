package fr.perso.nimbus.screens.ui

import fr.perso.nimbus.R
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.settings.PaperAppearancePreview
import fr.perso.nimbus.ui.settings.SettingsBanner
import fr.perso.nimbus.ui.settings.SettingsColors
import fr.perso.nimbus.ui.settings.SettingsEntry
import fr.perso.nimbus.ui.settings.SettingsPage
import fr.perso.nimbus.ui.settings.SettingsScreen
import fr.perso.nimbus.ui.settings.settingsSection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class SettingsScreenshot {

    /** Page de réglages remplie : entrées, sections, cases à cocher, entrée désactivée, note de bas de page. */
    @Test
    fun components() = UiFixture.shoot("settings_components") {
        SettingsPage("Page d'accueil", onBack = {}, folio = "v") {
            item { SettingsEntry(R.drawable.ms_style, SettingsColors.Blue, "Style de l'accueil", "1 · Groupes", chevron = true) {} }
            settingsSection("Grille d'icônes")
            item { SettingsEntry(R.drawable.ms_view_column, SettingsColors.Blue, "Colonnes de la grille", "5", chevron = true) {} }
            item { SettingsEntry(R.drawable.ms_title, SettingsColors.Blue, "Noms sous les icônes", checked = true) {} }
            item { SettingsEntry(R.drawable.ms_schedule, SettingsColors.Blue, "Horloge et date", checked = false) {} }
            item { SettingsEntry(R.drawable.ms_search, SettingsColors.Blue, "Barre de recherche", "En bas de l'accueil", checked = true) {} }
            item {
                SettingsEntry(R.drawable.ms_format_paint, SettingsColors.Orange, "Icônes à thème", "Nécessite Android 13", enabled = false, checked = false) {}
            }
            settingsSection("Notifications")
            item {
                SettingsEntry(
                    R.drawable.ms_app_badging, SettingsColors.Purple, "Pastilles de notification",
                    "Sur les icônes des apps qui ont des notifications", checked = true,
                ) {}
            }
            item { SettingsEntry(R.drawable.ms_notifications, SettingsColors.Purple, "Style des pastilles", "Nombre", chevron = true) {} }
            item { SettingsBanner(R.drawable.ms_app_badging, "Autoriser « Pastilles de notification Nimbus »") {} }
        }
    }

    /** Page principale réelle (modèle de vue sur Robolectric : aucune app, Nimbus n'est pas l'accueil). */
    @Test
    fun main() {
        val vm = UiFixture.viewModel()
        UiFixture.shoot("settings_main") { SettingsScreen(vm, FakeHost, SettingsSection.MAIN) }
    }

    /** Page de l'accueil avec un style papier : pastilles des places (vides ici), cases à cocher. */
    @Test
    fun home() {
        val vm = UiFixture.viewModel()
        vm.updateSettings { it.copy(homeStyle = HomeStyle.GROUPES, themeMode = ThemeMode.PAPER) }
        UiFixture.shoot("settings_home") { SettingsScreen(vm, FakeHost, SettingsSection.HOME) }
    }

    /** Apparence : aperçu papier de l'accueil, avec des apps. */
    @Test
    fun appearance() = UiFixture.shoot("settings_appearance") {
        SettingsPage("Apparence globale", onBack = {}, folio = "ii") {
            item {
                PaperAppearancePreview(
                    UiFixture.appsState(),
                    LauncherSettings(themeMode = ThemeMode.PAPER, homeStyle = HomeStyle.GROUPES),
                ) {}
            }
            item { SettingsEntry(R.drawable.ms_wallpaper, SettingsColors.Green, "Fond d'écran", "Image ou fond animé") {} }
            item { SettingsEntry(R.drawable.ms_interests, SettingsColors.Orange, "Forme des icônes", "Squircle", chevron = true) {} }
            item { SettingsEntry(R.drawable.ms_format_size, SettingsColors.Orange, "Taille des icônes", "Moyenne", chevron = true) {} }
        }
    }

    /** Sécurité : la page qui mène au code PIN. */
    @Test
    fun privacy() {
        val vm = UiFixture.viewModel()
        UiFixture.shoot("settings_privacy") { SettingsScreen(vm, FakeHost, SettingsSection.PRIVACY) }
    }
}
