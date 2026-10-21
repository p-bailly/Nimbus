package fr.perso.nimbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.time.LocalDate

/**
 * Exercice du jour : la réponse annoncée est recalculée par une autre voie (force brute), pour chaque
 * famille et 400 jours de chacune ; les nombres sont relus dans le texte affiché.
 */
class DailyExerciseTest {

    private val start = LocalDate.of(2026, 1, 1)

    /** 3 200 jours consécutifs : 400 par famille, sur près de neuf ans. */
    private val days = (0L until 3200L).map { DailyExercise.of(start.plusDays(it)) }

    private fun answer(e: Exercise): Long = e.answer.removeSuffix("\u00A0%").replace("\u202F", "").toLong()

    private fun int(regex: String, text: String): Int =
        Regex(regex).find(text)?.groupValues?.get(1)?.toInt() ?: error("« $regex » absent de « $text »")

    private fun family(f: Int): List<Exercise> = days.filter { it.family == f }.also { assertEquals(400, it.size) }

    @Test
    fun sumOfIntegers() = family(0).forEach { e ->
        val n = int("""de 1 à (\d+)\.""", e.statement)
        assertEquals("1 + 2 + 3 + ⋯ + $n", e.formula.single().text)
        var sum = 0L
        for (k in 1..n) sum += k
        assertEquals(e.toString(), sum, answer(e))
    }

    @Test
    fun handshakes() = family(1).forEach { e ->
        val n = int("""(\d+) personnes""", e.statement)
        var pairs = 0L
        for (i in 0 until n) for (j in i + 1 until n) pairs++
        assertEquals(e.toString(), pairs, answer(e))
        assertTrue(e.formula.isEmpty())
    }

    @Test
    fun differenceOfSquares() = family(2).forEach { e ->
        val (a, b) = e.formula.map { it.text.filter(Char::isDigit).toInt() }
        assertEquals(listOf("2", "2"), e.formula.map { it.sup })
        // a² − b² par additions répétées.
        var value = 0L
        repeat(a) { value += a }
        repeat(b) { value -= b }
        assertEquals(e.toString(), value, answer(e))
    }

    @Test
    fun sumOfOddNumbers() = family(3).forEach { e ->
        val n = int("""des (\d+) premiers""", e.statement)
        val last = int("""⋯ \+ (\d+)$""", e.formula.single().text)
        val odds = (1..last step 2).toList()
        assertEquals(n, odds.size)
        assertEquals(e.toString(), odds.sumOf { it.toLong() }, answer(e))
    }

    @Test
    fun diagonals() = family(4).forEach { e ->
        val n = int("""à (\d+) côtés""", e.statement)
        // Paires de sommets non voisins sur le polygone.
        var count = 0L
        for (i in 0 until n) for (j in i + 1 until n) if (j - i != 1 && j - i != n - 1) count++
        assertEquals(e.toString(), count, answer(e))
    }

    @Test
    fun unitsDigit() = family(5).forEach { e ->
        val seg = e.formula.single()
        val base = seg.text.toInt()
        val exponent = seg.sup.toInt()
        assertEquals(e.date.year, exponent)
        val digit = BigInteger.valueOf(base.toLong()).modPow(BigInteger.valueOf(exponent.toLong()), BigInteger.TEN).toLong()
        assertEquals(e.toString(), digit, answer(e))
    }

    @Test
    fun squareEndingInFive() = family(6).forEach { e ->
        val seg = e.formula.single()
        assertEquals("2", seg.sup)
        val x = seg.text.toInt()
        assertEquals(5, x % 10)
        var square = 0L
        repeat(x) { square += x }
        assertEquals(e.toString(), square, answer(e))
    }

    @Test
    fun percentUpThenDown() = family(7).forEach { e ->
        val p = int("""de (\d+)\u00A0%, puis""", e.statement)
        assertTrue(e.statement.contains("baisse de $p\u00A0%"))
        // Prix de 10 000 centimes : +p %, puis −p %.
        val up = 10_000L * (100 + p) / 100
        val down = up * (100 - p) / 100
        assertEquals(0L, (10_000L - down) % 100)
        assertEquals(e.toString(), (10_000L - down) / 100, answer(e))
        assertTrue("« ${e.answer} »", e.answer.endsWith("\u00A0%"))
    }

    @Test
    fun numberingAndFamilies() {
        assertEquals(293L, DailyExercise.dayIndex(LocalDate.of(2026, 10, 20)))
        assertEquals(366L, DailyExercise.dayIndex(LocalDate.of(2027, 1, 1)))
        for (e in days) {
            assertEquals(e.date.dayOfYear, e.number)
            assertEquals(Math.floorMod(DailyExercise.dayIndex(e.date), 8L).toInt(), e.family)
            assertTrue(e.level == "collège" || e.level == "lycée")
            // L'explication est une phrase : majuscule (sauf variable en tête) et point final.
            val first = e.explanation.first()
            assertTrue(first.italic || first.text.first().isUpperCase())
            assertTrue(e.explanation.last().text.endsWith("."))
        }
        // Le 1er janvier : la solution est celle du 31 décembre, n° 365, avec son année.
        val jan1 = DailyExercise.of(LocalDate.of(2027, 1, 1).minusDays(1))
        assertEquals(365, jan1.number)
        assertEquals(LocalDate.of(2026, 12, 31), jan1.date)
        assertEquals(1, DailyExercise.of(LocalDate.of(2027, 1, 1)).number)
    }

    @Test
    fun mockupDay() {
        // Maquette : 20 octobre 2026, n° 293, 7²⁰²⁶ ; solution d'hier n° 292, 12 côtés, 54.
        val today = DailyExercise.of(LocalDate.of(2026, 10, 20))
        assertEquals(293, today.number)
        assertEquals("lycée", today.level)
        assertEquals(listOf(Seg("7", "2026")), today.formula)
        assertEquals("9", today.answer)
        val yesterday = DailyExercise.of(LocalDate.of(2026, 10, 19))
        assertEquals(292, yesterday.number)
        assertEquals("Combien de diagonales a un polygone convexe à 12 côtés\u202F?", yesterday.statement)
        assertEquals(
            "Chaque sommet est relié à 9 autres par une diagonale, et chaque diagonale a deux extrémités : 12 × 9 / 2.",
            yesterday.explanation.single().text,
        )
        assertEquals("54", yesterday.answer)
        // Pourcentages (n° 295) : « 9 % », et 1 − 0,3² = 0,91.
        val percent = DailyExercise.of(LocalDate.of(2026, 10, 22))
        assertEquals("9\u00A0%", percent.answer)
        assertEquals(" = 0,91.", percent.explanation.last().text)
        // Carrés : la phrase commence par la variable a, sans majuscule.
        val squares = days.first { it.family == 2 }
        assertFalse(squares.explanation.first().text.first().isUpperCase())
        // Milliers : 2 415 avec une espace fine insécable.
        assertEquals("2\u202F415", DailyExercise.of(LocalDate.of(2026, 10, 23)).answer)
    }
}
