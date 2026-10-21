package fr.perso.nimbus.data

import android.content.ComponentName
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Motifs choisis (enregistrement, emprunt à une autre app, couleur propre) et catégories réglées. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class GlyphsAndCategoriesTest {

    private fun app(label: String, pkg: String = "test.${label.lowercase()}") = AppEntry(
        key = "$pkg/Main#0",
        packageName = pkg,
        componentName = ComponentName(pkg, "$pkg.Main"),
        user = Process.myUserHandle(),
        label = label,
        icon = ImageBitmap(1, 1),
        autoCategory = AppCategory.OTHER,
    )

    @Test
    fun choicesRoundTrip() {
        val choices = listOf(
            GlyphChoice.Logo(PaperLogo.YOUTUBE),
            GlyphChoice.Text("+x"),
            GlyphChoice.Text("πr"),
            GlyphChoice.OfApp("com.google.android.apps.docs/Main#0"),
            GlyphChoice.Own,
        )
        for (choice in choices) assertEquals(choice, GlyphChoice.parse(choice.spec))
        assertNull(GlyphChoice.parse("logo:INCONNU"))
        assertNull(GlyphChoice.parse("text:"))
    }

    @Test
    fun borrowedGlyphFollowsTheOtherApp() {
        val drive = app("Drive")
        val files = app("Fichiers")
        val bank = app("Banque Populaire")
        val choices = GlyphChoices(
            mapOf(
                files.key to GlyphChoice.OfApp(drive.key),
                bank.key to GlyphChoice.Text("+x"),
                drive.key to GlyphChoice.OfApp(bank.key),
            ),
            listOf(drive, files, bank).associateBy { it.key },
        )
        assertEquals(GlyphChoice.OfApp(drive.key) to drive, choices.resolve(files))
        // Drive emprunte à la banque, qui a des lettres : ce sont elles qui sont reprises.
        assertEquals(GlyphChoice.Text("+x") to null, choices.resolve(drive))
        assertEquals(null to null, choices.resolve(app("Maps")))
    }

    @Test
    fun youtubeSlotPrefersRevancedWithTheYoutubeLogo() {
        val official = app("YouTube", "com.google.android.youtube")
        val revanced = app("YouTube ReVanced", "app.revanced.android.youtube")
        val maths = app("Prof de maths", "fr.pbailly.profdemaths")
        val slots = PaperSlot.resolve(listOf(official, revanced, maths), emptyMap(), emptyMap())
        assertEquals(revanced, slots[PaperSlot.YOUTUBE])
        assertEquals(GlyphChoice.Logo(PaperLogo.YOUTUBE), PaperSlot.YOUTUBE.glyph)
        assertEquals(maths, slots[PaperSlot.PIR])
        assertEquals(GlyphChoice.Text("πr"), PaperSlot.PIR.glyph)
        assertEquals(GlyphChoice.Text("+x"), PaperSlot.BANQUE.glyph)
    }

    @Test
    fun dominantColorIsTheMainHueDarkenedForPaper() {
        // Rouge vif sur les trois quarts, blanc ailleurs : rouge, pas plus clair que 45 %.
        val pixels = IntArray(400) { if (it < 300) 0xFFFF2020.toInt() else 0xFFFFFFFF.toInt() }
        val color = GlyphFactory.dominantColor(pixels)
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(color, hsl)
        assertTrue(hsl[0] < 5f || hsl[0] > 355f)
        assertTrue(hsl[2] <= 0.451f)
        // Icône grise : l'encre.
        assertEquals(0xFF14141A.toInt(), GlyphFactory.dominantColor(IntArray(400) { 0xFF808080.toInt() }))
    }

    @Test
    fun categoriesCanBeRenamedCreatedAndDeleted() {
        val prefs = Prefs.get(RuntimeEnvironment.getApplication())
        prefs.setCategories(Category.defaults)
        val renamed = prefs.categories.value.map { if (it.id == "COMMUNICATION") it.copy(label = "Messages") else it }
        prefs.setCategories(listOf(Category("c1", "Lycée")) + renamed.filterNot { it.id == "GAMES" || it.id == Category.OTHER_ID })
        prefs.setCategoryOverride("a/b#0", "c1")
        prefs.setCategoryOverride("c/d#0", "GAMES")

        val list = prefs.categories.value
        assertEquals("c1", list.first().id)
        assertEquals("Messages", list.first { it.id == "COMMUNICATION" }.label)
        // « Autres » revient toujours, à la fin ; « Jeux » supprimé emporte ses apps rangées à la main.
        assertEquals(Category.OTHER_ID, list.last().id)
        assertTrue(list.none { it.id == "GAMES" })
        prefs.setCategories(list)
        assertEquals(mapOf("a/b#0" to "c1"), prefs.categoryOverrides.value)

        prefs.setCategories(Category.defaults)
        assertEquals(Category.defaults, prefs.categories.value)
        assertTrue(prefs.categoryOverrides.value.isEmpty())
    }
}
