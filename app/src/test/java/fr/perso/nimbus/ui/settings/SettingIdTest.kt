package fr.perso.nimbus.ui.settings

import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.ui.SettingsSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le registre des réglages (`SettingId`) et ce que la recherche en tire. */
class SettingIdTest {

    @Test
    fun everySettingHasATitleAndAKeyOfItsOwn() {
        for (id in SettingId.entries) assertTrue("${id.name} sans intitulé", id.title.isNotBlank())
        // Deux lignes d'une même page ne portent pas le même intitulé : la recherche les confondrait.
        val titles = SettingId.entries.groupBy { it.section to it.title }
        assertTrue("Intitulés en double : " + titles.filterValues { it.size > 1 }.keys, titles.values.all { it.size == 1 })
    }

    @Test
    fun everyGestureHasItsRow() {
        for (gesture in Gesture.entries) {
            val id = SettingId.of(gesture)
            assertEquals(SettingsSection.GESTURES, id.section)
            assertEquals(gesture.label, id.title)
        }
    }

    @Test
    fun everyPageHasATitle() {
        for (section in SettingsSection.entries) assertTrue(SettingsIndex.pageTitle(section).isNotBlank())
    }

    @Test
    fun backGoesToTheParentOrTheMainPage() {
        assertNull(SettingsSection.MAIN.back)
        assertEquals(SettingsSection.MAIN, SettingsSection.APPEARANCE.back)
        assertEquals(SettingsSection.HOME, SettingsSection.HOME_APPS.back)
        assertEquals(SettingsSection.DRAWER, SettingsSection.CATEGORIES.back)
    }

    @Test
    fun pagesAreFoundToo() {
        val hit = SettingsIndex.search("categories").first { it.id == null }
        assertEquals(SettingsSection.CATEGORIES, hit.section)
        assertEquals("Paramètres › Tiroir et recherche", hit.path)
    }

    @Test
    fun aSubPageSettingShowsItsWholePath() {
        val hit = SettingsIndex.search("ajouter une cat").first()
        assertEquals(SettingId.ADD_CATEGORY, hit.id)
        assertEquals("Paramètres › Tiroir et recherche › Catégories › Actions", hit.path)
    }

    @Test
    fun aWordOfTheDescriptionFindsTheSetting() {
        val hits = SettingsIndex.search("colonnes").mapNotNull { it.id }
        assertTrue(SettingId.GRID_COLUMNS in hits)
        assertTrue(SettingId.DRAWER_COLUMNS in hits)
        assertNotNull(SettingsIndex.search("pin").firstOrNull { it.id == SettingId.PIN })
    }
}
