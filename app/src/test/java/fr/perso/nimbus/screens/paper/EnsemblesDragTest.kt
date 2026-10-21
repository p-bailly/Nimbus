package fr.perso.nimbus.screens.paper

import androidx.compose.ui.geometry.Offset
import fr.perso.nimbus.ui.paper.layouts.allowedDisc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pastilles déplacées sur « Ensembles » : elles restent dans le cadre et ne se chevauchent pas. */
class EnsemblesDragTest {
    private val discs = listOf(Offset(206f, 352f), Offset(212f, 252f), Offset(116f, 404f))

    @Test
    fun staysInsideTheFrame() {
        assertEquals(Offset(37f, 183f), allowedDisc(Offset(-50f, 0f), 0, discs))
        assertEquals(Offset(375f, 713f), allowedDisc(Offset(900f, 900f), 0, discs))
    }

    @Test
    fun keepsAwayFromTheOtherDiscs() {
        assertNull(allowedDisc(Offset(212f, 270f), 0, discs))
        assertEquals(Offset(300f, 600f), allowedDisc(Offset(300f, 600f), 0, discs))
        // Sa propre place ne la gêne pas.
        assertEquals(Offset(206f, 360f), allowedDisc(Offset(206f, 360f), 0, discs))
    }
}
