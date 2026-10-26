package fr.perso.nimbus.ui.drawer

import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.settings.SettingsIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Surlignage des lettres trouvées, et recherche dans les réglages de Nimbus. */
class SearchHighlightTest {
    /** Les plages surlignées de [text] pour [query], en [début, fin[. */
    private fun marked(text: String, query: String): List<IntRange> =
        SearchMatcher.highlight(text, query).spanStyles.map { it.start until it.end }

    @Test
    fun highlightIgnoresCaseAndAccents() {
        assertEquals(listOf(0 until 3), marked("École", "eco"))
        assertEquals(listOf(0 until 2), marked("BAILLY Valérie", "ba"))
        assertEquals(listOf(7 until 10), marked("Banque Populaire", "pop"))
    }

    @Test
    fun highlightExpandsLigatures() {
        // « œ » compte pour « oe » : la saisie « oeuv » couvre « Œuv ».
        assertEquals(listOf(0 until 3), marked("Œuvres", "oeuv"))
    }

    @Test
    fun highlightFallsBackToInitials() {
        assertEquals(listOf(0 until 1, 7 until 8), marked("Google Maps", "gm"))
        assertEquals(listOf(0 until 1, 5 until 6), marked("Café Crème", "cc"))
    }

    @Test
    fun highlightNothingWhenNothingMatches() {
        assertTrue(marked("Valérie", "xyz").isEmpty())
        assertTrue(marked("Valérie", "").isEmpty())
    }

    @Test
    fun settingsAreFoundByTitleAndKeywords() {
        val byTitle = SettingsIndex.search("moteur")
        assertEquals("Moteur de recherche web", byTitle.first().title)
        assertEquals(SettingsSection.SEARCH, byTitle.first().section)
        assertEquals("Paramètres › Page de recherche", byTitle.first().path)

        // Sans accent ni majuscule, et par un mot de description.
        assertEquals("Entrée ouvre la première app", SettingsIndex.search("entree").first().title)
        assertEquals("Thème", SettingsIndex.search("mode sombre").first().title)
    }

    @Test
    fun settingsNeedTwoLettersAndFindNothingForNonsense() {
        assertTrue(SettingsIndex.search("e").isEmpty())
        assertTrue(SettingsIndex.search("xyzzy").isEmpty())
    }
}
