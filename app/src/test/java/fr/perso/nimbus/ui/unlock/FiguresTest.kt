package fr.perso.nimbus.ui.unlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Figures du jour : chacune revient à tour de rôle, avec un nom, et une explication pour chaque variante si elle est rédigée. */
class FiguresTest {

    @Test
    fun everyFigureInTurn() {
        val n = ALL_FIGURES.size
        assertEquals(ALL_FIGURES.map { it.key }.toSet().size, n)
        val start = LocalDate.of(2026, 1, 1)
        val days = (0L until 3L * n).map { DailyFigures.of(start.plusDays(it)) }
        days.chunked(n).forEach { cycle -> assertEquals(ALL_FIGURES.map { it.key }.toSet(), cycle.map { it.spec.key }.toSet()) }
        assertEquals(days[0].variant + 1, days[n].variant)
    }

    @Test
    fun namesAndExplanations() {
        for (spec in ALL_FIGURES) {
            assertTrue(spec.key, spec.title.isNotBlank() && spec.title.length <= 34)
            assertTrue(spec.key, spec.key.matches(Regex("[a-z0-9_]+")))
            for (v in 0..5) {
                val info = spec.info(v)
                // Explication pas encore rédigée : la légende n'a pas de (i).
                if (info.isEmpty()) continue
                assertTrue("${spec.key} : $info", info.first().isUpperCase() && info.endsWith("."))
                assertTrue("${spec.key} : ${info.length} caractères", info.length in 80..480)
            }
        }
    }
}
