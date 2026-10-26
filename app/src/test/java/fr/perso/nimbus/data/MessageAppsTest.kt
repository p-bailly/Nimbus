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
}
