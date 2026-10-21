package fr.perso.nimbus.ui.drawer

import android.content.ComponentName
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DrawerTocTest {
    private fun app(label: String, category: AppCategory) = AppEntry(
        key = "test.$label/Main#0",
        packageName = "test.$label",
        componentName = ComponentName("test.$label", "test.$label.Main"),
        user = Process.myUserHandle(),
        label = label,
        icon = ImageBitmap(1, 1),
        autoCategory = category,
    )

    private fun TocLine.text() = when (this) {
        is TocLine.Section -> listOfNotNull(number, title, page).joinToString(" ")
        is TocLine.Entry -> listOfNotNull(number, app.label, page).joinToString(" ")
    }

    @Test
    fun sectionsSubsectionsAndPages() {
        val gmail = app("Gmail", AppCategory.COMMUNICATION)
        val sms = app("Messages", AppCategory.COMMUNICATION)
        val tel = app("Téléphone", AppCategory.COMMUNICATION)
        val wa = app("WhatsApp", AppCategory.COMMUNICATION)
        val signal = app("Signal", AppCategory.COMMUNICATION)
        val maps = app("Maps", AppCategory.TRAVEL)
        val lines = tableOfContents(
            apps = listOf(gmail, sms, signal, tel, wa, maps),
            categories = listOf(AppCategory.COMMUNICATION, AppCategory.TRAVEL),
            frequent = listOf(wa, maps),
            privateApps = listOf(app("Banque", AppCategory.SHOPPING)),
        )
        assertEquals(
            listOf(
                "Fréquentes i", "WhatsApp i", "Maps i",
                // Quatre apps par page ; chaque section commence sur une nouvelle page.
                "1 Communication 1", "1.1 Gmail 1", "1.2 Messages 1", "1.3 Signal 1", "1.4 Téléphone 1", "1.5 WhatsApp 2",
                "2 Voyage & Cartes 3", "2.1 Maps 3",
                "A Espace privé 4", "A.1 Banque 4",
            ),
            lines.map { it.text() },
        )
        assertEquals(lines.size, lines.map { it.key }.toSet().size)
    }

    @Test
    fun singleCategoryNumbersTheApps() {
        val lines = tableOfContents(listOf(app("Agenda", AppCategory.OTHER), app("Waze", AppCategory.OTHER)), listOf(AppCategory.OTHER))
        assertEquals(listOf("1 Agenda 1", "2 Waze 1"), lines.map { it.text() })
    }

    @Test
    fun lockedPrivateSpaceKeepsItsAppendix() {
        val lines = tableOfContents(listOf(app("Agenda", AppCategory.OTHER)), emptyList(), privateApps = emptyList())
        assertEquals(listOf("1 Agenda 1", "A Espace privé 2"), lines.map { it.text() })
    }

    @Test
    fun romanNumerals() {
        assertEquals(listOf("i", "iv", "ix", "xiv", "xl"), listOf(1, 4, 9, 14, 40).map(::roman))
    }
}
