package fr.perso.nimbus.data

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import fr.perso.nimbus.ui.unlock.SKETCH_SHOWN
import fr.perso.nimbus.ui.unlock.SKETCH_WIDTH
import fr.perso.nimbus.ui.unlock.TEXT_MARGIN
import fr.perso.nimbus.ui.unlock.TOP_FADE
import fr.perso.nimbus.ui.unlock.imageZones
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Le texte de la page de déverrouillage, tel qu'on le dit à Météo pour qu'elle dessine autour. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeteoShareTest {

    @Test
    fun `text zones as Meteo reads them`() {
        val zones = listOf(Rect(0f, 0f, 1f, 0.3f), Rect(0.0958f, 0.33981f, 0.25f, 0.5322f))
        assertEquals("0.0000,0.0000,1.0000,0.3000;0.0958,0.3398,0.2500,0.5322", MeteoShare.textParameter(zones))
    }

    @Test
    fun `page text in fractions of the sketch`() {
        // 2 pixels par unité : la page fait 824 pixels de large, le dessin deux fois SKETCH_WIDTH, débordant de chaque côté.
        val density = Density(2f)
        val overflow = (SKETCH_WIDTH * 2f - 824f) / 2f
        val shown = SKETCH_WIDTH * MeteoShare.SKETCH_RATIO * SKETCH_SHOWN * 2f
        val zones = with(density) { imageZones(listOf(Rect(-overflow, 0f, 412f, shown / 2f))) }
        // D'abord le haut coupé, jusqu'au milieu du fondu moins la marge de la gomme.
        assertEquals(0f, zones[0].top, 1e-4f)
        assertEquals(1f, zones[0].right, 1e-4f)
        assertEquals(1f - SKETCH_SHOWN + (TOP_FADE / 2 - TEXT_MARGIN) / (SKETCH_WIDTH * MeteoShare.SKETCH_RATIO), zones[0].bottom, 1e-4f)
        // Puis le texte : du bord gauche de l'image à son milieu, du haut montré au milieu de ce qui est montré.
        assertEquals(0f, zones[1].left, 1e-4f)
        assertEquals((412f + overflow) / (SKETCH_WIDTH * 2f), zones[1].right, 1e-4f)
        assertEquals(0.32f, zones[1].top, 1e-4f)
        assertEquals(0.66f, zones[1].bottom, 1e-4f)
        // Hors de l'image : rien.
        assertEquals(1, with(density) { imageZones(listOf(Rect(-100f, 10f, -60f, 20f))) }.size)
    }
}
