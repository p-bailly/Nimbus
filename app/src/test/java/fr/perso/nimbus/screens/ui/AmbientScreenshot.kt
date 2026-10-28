package fr.perso.nimbus.screens.ui

import androidx.compose.ui.geometry.Rect
import fr.perso.nimbus.R
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.UiState
import fr.perso.nimbus.ui.components.AppActionSheet
import fr.perso.nimbus.ui.components.AppPickerDialog
import fr.perso.nimbus.ui.components.AppPopup
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.components.PinDialog
import fr.perso.nimbus.ui.components.PinMode
import fr.perso.nimbus.ui.components.TextInputDialog
import fr.perso.nimbus.ui.drawer.AppDrawer
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

/**
 * Thème ambiant (Material) : captures des écrans, des dialogues et de la page de réglages sous ce thème.
 * Les réglages ont changé de look à la refonte (couleur d'accent par page, pastilles pleines) ; les
 * dialogues, le tiroir et les menus, eux, n'ont pas bougé.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class AmbientScreenshot {
    private val mode = ThemeMode.AMBIENT

    @Test
    fun components() = UiFixture.shoot("ambient_settings_components", mode) {
        SettingsPage("Page d'accueil", onBack = {}) {
            item { SettingsEntry(R.drawable.ms_style, SettingsColors.Blue, "Style de l'accueil", "1 · Groupes", chevron = true) {} }
            settingsSection("Grille d'icônes")
            item { SettingsEntry(R.drawable.ms_title, SettingsColors.Blue, "Noms sous les icônes", checked = true) {} }
            item { SettingsEntry(R.drawable.ms_schedule, SettingsColors.Blue, "Horloge et date", checked = false) {} }
            item {
                SettingsEntry(R.drawable.ms_format_paint, SettingsColors.Orange, "Icônes à thème", "Nécessite Android 13", enabled = false, checked = false) {}
            }
            item { SettingsBanner(R.drawable.ms_app_badging, "Autoriser « Pastilles de notification Nimbus »") {} }
        }
    }

    @Test
    fun main() {
        val vm = UiFixture.viewModel()
        UiFixture.shoot("ambient_settings_main", mode) { SettingsScreen(vm, FakeHost, SettingsSection.MAIN) }
    }

    @Test
    fun choice() = UiFixture.shootScreen("ambient_dialog_choice", mode) {
        ChoiceDialog("Thème du lanceur", ThemeMode.entries, ThemeMode.AMBIENT, { it.label }, onSelect = {}, onDismiss = {}, footer = "Note.")
    }

    @Test
    fun appPicker() = UiFixture.shootScreen("ambient_dialog_app_picker", mode) {
        AppPickerDialog("Place « WhatsApp »", UiFixture.apps, onPick = {}, onDismiss = {}, noneLabel = "Choix automatique")
    }

    @Test
    fun pin() = UiFixture.shootScreen("ambient_dialog_pin", mode) {
        PinDialog(title = "Apps masquées", mode = PinMode.VERIFY, onDismiss = {}, onSuccess = {})
    }

    @Test
    fun rename() = UiFixture.shootScreen("ambient_dialog_rename", mode) {
        TextInputDialog(title = "Renommer", initial = "WhatsApp", placeholder = "WhatsApp", onConfirm = {}, onDismiss = {})
    }

    @Test
    fun drawer() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("ambient_drawer", mode) {
            AppDrawer(vm = vm, ui = UiState(drawerOpen = true), settings = LauncherSettings(), apps = UiFixture.appsState(), host = FakeHost)
        }
    }

    @Test
    fun actionSheet() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("ambient_action_sheet", mode) {
            AppActionSheet(UiFixture.app("WhatsApp"), UiFixture.appsState(), vm, FakeHost)
        }
    }

    @Test
    fun popup() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("ambient_popup", mode) {
            AppPopup(UiFixture.app("WhatsApp"), Rect(500f, 1500f, 660f, 1660f), UiFixture.appsState(), vm)
        }
    }
}
