package fr.perso.nimbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Applis dont la page de déverrouillage montre les notifications. */
class MessageAppsTest {
    private fun note(pkg: String, title: String, time: Long) =
        MessageApps.Candidate("0|$pkg|$time", pkg, title, "texte", time, clearable = true)

    @Test
    fun scrabbleNotificationsReachTheUnlockPage() {
        assertEquals("Scrabble", MessageApps.of("fr.perso.scrabble")?.name)
        val notes = MessageApps.notes(
            listOf(
                note("fr.perso.scrabble", "Alice vous invite à une partie", 2_000),
                // Pas une appli de messages : jamais sur la page.
                note("com.android.vending", "Mises à jour", 3_000),
            ),
        )
        assertEquals(listOf("Alice vous invite à une partie"), notes.map { it.title })
        assertNull(MessageApps.of("com.android.vending"))
    }

    private fun message(visibility: Int, publicTitle: String = "", publicText: String = "") = MessageApps.Candidate(
        "0|com.whatsapp|$visibility", "com.whatsapp", "Maman", "Ton code : 482913", 1_000, clearable = true,
        lockVisibility = visibility, publicTitle = publicTitle, publicText = publicText,
    )

    @Test
    fun privateMessagesFollowTheLockScreenRules() {
        // Contenu sensible masqué (réglage du système) : un message privé sans version publique ne montre que l'appli.
        val hidden = MessageApps.notes(listOf(message(MessageApps.VISIBILITY_PRIVATE)), showPrivate = false).single()
        assertEquals("" to MessageApps.HIDDEN_TEXT, hidden.title to hidden.text)
        assertEquals("WhatsApp", hidden.app.name)
        // Sa version publique, quand l'appli en donne une.
        val public = MessageApps.notes(listOf(message(MessageApps.VISIBILITY_PRIVATE, "WhatsApp", "1 nouveau message")), showPrivate = false).single()
        assertEquals("WhatsApp" to "1 nouveau message", public.title to public.text)
        // Contenu sensible affiché : le message en entier.
        val shown = MessageApps.notes(listOf(message(MessageApps.VISIBILITY_PRIVATE)), showPrivate = true).single()
        assertEquals("Maman" to "Ton code : 482913", shown.title to shown.text)
        // Un message public est toujours montré en entier.
        val open = MessageApps.notes(listOf(message(MessageApps.VISIBILITY_PUBLIC)), showPrivate = false).single()
        assertEquals("Ton code : 482913", open.text)
        // Par défaut (réglage illisible), le plus discret.
        assertEquals(MessageApps.HIDDEN_TEXT, MessageApps.notes(listOf(message(MessageApps.VISIBILITY_PRIVATE))).single().text)
    }

    @Test
    fun secretMessagesAndHiddenNotificationsNeverReachThePage() {
        assertEquals(emptyList<UnlockNote>(), MessageApps.notes(listOf(message(MessageApps.VISIBILITY_SECRET)), showPrivate = true))
        assertEquals(
            emptyList<UnlockNote>(),
            MessageApps.notes(listOf(message(MessageApps.VISIBILITY_PUBLIC)), showPrivate = true, showNotifications = false),
        )
    }

    @Test
    fun visibilityConstantsMatchAndroid() {
        assertEquals(android.app.Notification.VISIBILITY_SECRET, MessageApps.VISIBILITY_SECRET)
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, MessageApps.VISIBILITY_PRIVATE)
        assertEquals(android.app.Notification.VISIBILITY_PUBLIC, MessageApps.VISIBILITY_PUBLIC)
    }
}
