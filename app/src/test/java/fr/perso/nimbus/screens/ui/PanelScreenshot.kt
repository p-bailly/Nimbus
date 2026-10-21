package fr.perso.nimbus.screens.ui

import android.os.Process
import fr.perso.nimbus.service.BadgeStore
import fr.perso.nimbus.service.NotificationItem
import fr.perso.nimbus.ui.panel.PaperPanel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class PanelScreenshot {

    /** Bandeau papier : heure, réglages rapides, trois notifications d'applis fictives. */
    @Test
    fun panel() {
        val vm = UiFixture.viewModel()
        val now = System.currentTimeMillis()
        fun note(app: String, title: String, text: String, minutes: Long) = UiFixture.app(app).let {
            NotificationItem("k:$app", it.packageName, Process.myUserHandle(), title, text, now - minutes * 60_000, true, null, true)
        }
        BadgeStore.update(
            mapOf(),
            listOf(
                note("WhatsApp", "Marie", "On se retrouve à 19 h devant le cinéma ?", 4),
                note("Gmail", "Pronote", "Nouvelle note en mathématiques : 17/20", 52),
                note("Claude", "Session terminée", "Les modifications sont poussées sur la branche principale.", 130),
            ),
        )
        UiFixture.shootScreen("panel") { PaperPanel(vm, UiFixture.appsState(), hasNotificationAccess = true) }
        BadgeStore.update(emptyMap())
    }
}
