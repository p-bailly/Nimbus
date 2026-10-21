package fr.perso.nimbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Figure du jour : chacune revient à tour de rôle, et ses paramètres changent d'un tour à l'autre. */
class DailyFigureTest {

    private val start = LocalDate.of(2026, 1, 1)
    private val n = FigureKind.entries.size

    @Test
    fun everyFigureInTurn() {
        val days = (0L until 10L * n).map { DailyFigures.of(start.plusDays(it)) }
        // Une période de n jours : chaque figure une fois.
        days.chunked(n).forEach { cycle -> assertEquals(FigureKind.entries.toSet(), cycle.map { it.kind }.toSet()) }
        // Le lendemain, la figure suivante ; au tour suivant, une autre variante.
        assertEquals((days[0].kind.ordinal + 1) % n, days[1].kind.ordinal)
        assertEquals(days[0].variant + 1, days[n].variant)
        for (f in days) {
            assertTrue(f.caption, f.caption.endsWith("."))
            assertTrue(f.caption, f.caption.first().isLowerCase())
        }
    }

    @Test
    fun captionsFollowTheParameters() {
        val cardioid = (0 until 6).map { DailyFigures.cardioidTable(it) }
        assertEquals(listOf(2, 3, 4, 5, 6, 7), cardioid)
        fun caption(kind: FigureKind, variant: Int): String {
            var d = start
            while (true) {
                val f = DailyFigures.of(d)
                if (f.kind == kind && f.variant == variant) return f.caption
                d = d.plusDays(1)
            }
        }
        assertTrue(caption(FigureKind.CARDIOID, 0).contains("au point 2 × n : les cordes dessinent un cœur"))
        assertTrue(caption(FigureKind.CARDIOID, 1).contains("néphroïde"))
        assertTrue(caption(FigureKind.CARDIOID, 3).contains("épicycloïde à 4 pointes"))
        assertTrue(caption(FigureKind.SIERPINSKI, 0).contains("81 petits triangles"))
        assertTrue(caption(FigureKind.SIERPINSKI, 1).contains("243 petits triangles"))
        assertTrue(caption(FigureKind.SPIROGRAPH, 1).contains("roue de 4 dents roule dans un anneau de 7"))
        assertTrue(caption(FigureKind.LISSAJOUS, 0).contains("3 allers-retours"))
    }
}
