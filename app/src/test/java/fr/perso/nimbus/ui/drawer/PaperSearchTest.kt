package fr.perso.nimbus.ui.drawer

import android.content.ComponentName
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.ContactResult
import fr.perso.nimbus.data.FileResult
import fr.perso.nimbus.ui.SearchResults
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.settings.SettingsHit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Recherche papier : l'ensemble vide ∅ ne s'écrit que s'il n'y a de résultat dans aucune famille. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PaperSearchTest {

    private val app = AppEntry(
        key = "test.app/Main#0",
        packageName = "test.app",
        componentName = ComponentName("test.app", "test.app.Main"),
        user = Process.myUserHandle(),
        label = "Test",
        icon = ImageBitmap(1, 1),
        autoCategory = AppCategory.OTHER,
    )

    @Test
    fun emptySetWhenNothingMatchesAnywhere() {
        assertTrue(noResultAnywhere(SearchResults(query = "zzz")))
    }

    @Test
    fun noEmptySetAsSoonAsOneFamilyHasAResult() {
        assertFalse(noResultAnywhere(SearchResults(apps = listOf(app))))
        assertFalse(noResultAnywhere(SearchResults(contacts = listOf(ContactResult(1, "key", "Ada")))))
        assertFalse(noResultAnywhere(SearchResults(files = listOf(FileResult(1, "ada.pdf", "application/pdf", "Documents")))))
        // Aucune app, mais un réglage : Apps n'est pas écrit ∅.
        assertFalse(noResultAnywhere(SearchResults(settings = listOf(SettingsHit("Page de recherche", SettingsSection.MAIN)))))
    }

    @Test
    fun noEmptySetWhileContactsAndFilesAreStillBeingSearched() {
        // Contacts et fichiers répondent après un court délai : leur liste vide n'est pas encore définitive.
        assertFalse(noResultAnywhere(SearchResults(query = "ada", pending = true)))
        assertTrue(noResultAnywhere(SearchResults(query = "ada", pending = false)))
    }

    @Test
    fun theProposedPrivateSpaceIsAResult() {
        assertFalse(noResultAnywhere(SearchResults(query = "espace privé"), privateEntry = true))
    }
}
