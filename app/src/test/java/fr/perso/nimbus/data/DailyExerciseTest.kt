package fr.perso.nimbus.data

import org.junit.Assert.assertEquals
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
    fun powersOfTwo() = family(0).forEach { e ->
        val n = e.formula.single().sup.toInt()
        var sum = 0L
        var term = 1L
        repeat(n + 1) {
            sum += term
            term *= 2
        }
        assertEquals(e.toString(), sum, answer(e))
    }

    @Test
    fun arithmeticSum() = family(1).forEach { e ->
        val terms = Regex("""\d+""").findAll(e.formula.single().text).map { it.value.toInt() }.toList()
        val (a, b, c, last) = terms
        val r = b - a
        assertEquals(r, c - b)
        assertEquals(0, (last - a) % r)
        var sum = 0L
        for (x in a..last step r) sum += x
        assertEquals(e.toString(), sum, answer(e))
    }

    @Test
    fun committees() = family(2).forEach { e ->
        val n = int("""parmi (\d+) élèves""", e.statement)
        var trios = 0L
        for (i in 0 until n) for (j in i + 1 until n) for (k in j + 1 until n) trios++
        assertEquals(e.toString(), trios, answer(e))
        assertTrue(e.formula.isEmpty())
    }

    @Test
    fun sumOfSquares() = family(3).forEach { e ->
        assertTrue(e.formula.all { it.sup == "2" })
        val n = int("""(\d+)$""", e.formula.last().text)
        var sum = 0L
        for (k in 1..n) sum += k.toLong() * k
        assertEquals(e.toString(), sum, answer(e))
    }

    @Test
    fun digitsOfPowerOfTwo() = family(4).forEach { e ->
        val seg = e.formula.single()
        assertEquals("2", seg.text)
        val digits = BigInteger.TWO.pow(seg.sup.toInt()).toString().length
        assertEquals(e.toString(), digits.toLong(), answer(e))
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
    fun geometricSeries() = family(6).forEach { e ->
        val q = int("""1/(\d+)""", e.formula.first().text)
        val (num, den) = e.answer.split("/").map { it.toLong() }
        var sum = 0.0
        var term = 1.0
        repeat(80) {
            sum += term
            term /= q
        }
        assertEquals(e.toString(), num.toDouble() / den, sum, 1e-12)
        assertEquals(1L, BigInteger.valueOf(num).gcd(BigInteger.valueOf(den)).toLong())
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
            assertEquals("lycée", e.level)
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
        // 20 octobre 2026, n° 293, 7²⁰²⁶ ; solution d'hier n° 292, le nombre de chiffres d'une puissance de 2.
        val today = DailyExercise.of(LocalDate.of(2026, 10, 20))
        assertEquals(293, today.number)
        assertEquals("lycée", today.level)
        assertEquals(listOf(Seg("7", "2026")), today.formula)
        assertEquals("9", today.answer)
        val yesterday = DailyExercise.of(LocalDate.of(2026, 10, 19))
        assertEquals(292, yesterday.number)
        assertEquals("Combien de chiffres a ce nombre\u202F?", yesterday.statement)
        assertEquals(listOf(Seg("2", "132")), yesterday.formula)
        assertEquals(Seg(" = 132 × log 2 ≈ 39,74 : le nombre est entre 10", "39"), yesterday.explanation[1])
        assertEquals("40", yesterday.answer)
        // Pourcentages (n° 295) : « 9 % », et 1 − 0,3² = 0,91.
        val percent = DailyExercise.of(LocalDate.of(2026, 10, 22))
        assertEquals("9\u00A0%", percent.answer)
        assertEquals(" = 0,91.", percent.explanation.last().text)
        // Milliers : 1 048 575 avec une espace fine insécable.
        assertEquals("1\u202F048\u202F575", DailyExercise.of(LocalDate.of(2026, 10, 23)).answer)
    }
}
