package fr.perso.nimbus.ui.unlock

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Le paquet de figures : une nouvelle à chaque affichage, sans en sauter quand la page est posée écran éteint. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FigureDeckTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()

    @Test
    fun aNewFigureAtEachDisplay() {
        // Page posée écran éteint : elle montre la prochaine, que le réveil confirme.
        val upcoming = FigureDeck.upcoming(app)
        val first = FigureDeck.advance(app)
        assertEquals(upcoming.spec.key, first.spec.key)
        assertEquals(upcoming.variant, first.variant)
        // Les affichages suivants passent toutes les figures en revue avant d'en revoir une.
        val seen = mutableListOf(first.spec.key)
        repeat(ALL_FIGURES.size - 1) { seen += FigureDeck.advance(app).spec.key }
        assertEquals(ALL_FIGURES.size, seen.toSet().size)
        // Au tour suivant, la même figure revient avec une autre variante.
        val again = FigureDeck.advance(app)
        assertEquals(first.spec.key, again.spec.key)
        assertNotEquals(first.variant, again.variant)
    }
}
