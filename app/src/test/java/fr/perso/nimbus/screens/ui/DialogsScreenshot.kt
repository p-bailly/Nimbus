package fr.perso.nimbus.screens.ui

import fr.perso.nimbus.ui.theme.PaperDialogWindow
import fr.perso.nimbus.data.ClaudeSession
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.components.AppPickerDialog
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.components.ClaudeSessionDialog
import fr.perso.nimbus.ui.components.PaperPinPanel
import fr.perso.nimbus.ui.components.PinDialog
import fr.perso.nimbus.ui.components.PinMode
import fr.perso.nimbus.ui.components.TextInputDialog
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class DialogsScreenshot {

    @Test
    fun choice() = UiFixture.shootScreen("dialog_choice") {
        ChoiceDialog(
            title = "Thème du lanceur",
            options = ThemeMode.entries,
            selected = ThemeMode.PAPER,
            label = { it.label },
            enabled = { it != ThemeMode.MATERIAL_YOU },
            onSelect = {},
            onDismiss = {},
        )
    }

    @Test
    fun choiceWithFooter() = UiFixture.shootScreen("dialog_choice_footer") {
        ChoiceDialog(
            title = "Style de l'accueil",
            options = HomeStyle.entries,
            selected = HomeStyle.GROUPES,
            label = { it.label },
            onSelect = {},
            onDismiss = {},
            footer = "Les styles « papier » placent 29 applis sur une page d'article ; appui long sur une appli pour en changer.",
        )
    }

    @Test
    fun appPicker() = UiFixture.shootScreen("dialog_app_picker") {
        AppPickerDialog(
            title = "Place « WhatsApp »",
            apps = UiFixture.apps,
            noneLabel = "Choix automatique",
            onPick = {},
            onDismiss = {},
        )
    }

    @Test
    fun rename() = UiFixture.shootScreen("dialog_rename") {
        TextInputDialog(title = "Renommer", initial = "WhatsApp", placeholder = "WhatsApp", onConfirm = {}, onDismiss = {}, onReset = {})
    }

    @Test
    fun claudeSession() = UiFixture.shootScreen("dialog_claude_session") {
        ClaudeSessionDialog(initial = ClaudeSession("Nimbus", "https://claude.ai/code/session_01AbCdEf"), onSave = {}, onDismiss = {}, onDelete = {})
    }

    /** Le vrai dialogue, à l'ouverture (aucun chiffre saisi). */
    @Test
    fun pin() = UiFixture.shootScreen("dialog_pin") {
        PinDialog(title = "Apps masquées", mode = PinMode.VERIFY, onDismiss = {}, onSuccess = {})
    }

    /** Pavé avec deux chiffres saisis. */
    @Test
    fun pinTyping() = UiFixture.shootScreen("dialog_pin_typing") {
        PaperDialogWindow(onDismissRequest = {}) {
            PaperPinPanel("Apps masquées", "Saisis ton code PIN", count = 2, error = null, onDigit = {}, onDelete = {}, onSubmit = {}, onDismiss = {})
        }
    }

    /** Code refusé : la note de bas de page. */
    @Test
    fun pinError() = UiFixture.shootScreen("dialog_pin_error") {
        PaperDialogWindow(onDismissRequest = {}) {
            PaperPinPanel(
                "Apps masquées", "Saisis ton code PIN", count = 0,
                error = "Code incorrect. Vérifie-le et recommence.",
                onDigit = {}, onDelete = {}, onSubmit = {}, onDismiss = {},
            )
        }
    }
}
