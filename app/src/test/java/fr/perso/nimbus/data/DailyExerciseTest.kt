package fr.perso.nimbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Exercice du jour : la réponse annoncée est retrouvée par une autre voie (simulation, force brute),
 * pour chaque famille et 300 jours de chacune ; les nombres sont relus dans le texte affiché.
 */
class DailyExerciseTest {

    private val start = LocalDate.of(2026, 1, 1)

    /** 3 600 jours consécutifs : 300 par famille, sur près de dix ans. */
    private val days = (0L until 3600L).map { DailyExercise.of(start.plusDays(it)) }

    /** Premier nombre de la réponse : « 12 jours » → 12. */
    private fun answer(e: Exercise): Int = int("""^(\d+)""", e.answer)

    private fun int(regex: String, text: String): Int =
        Regex(regex).find(text)?.groupValues?.get(1)?.toInt() ?: error("« $regex » absent de « $text »")

    private fun family(f: Int): List<Exercise> =
        days.filter { it.family == f }.also { assertEquals(3600 / DailyExercise.FAMILIES, it.size) }

    @Test
    fun snail() = family(0).forEach { e ->
        val height = int("""puits de (\d+) m""", e.statement)
        var at = 0
        var day = 0
        while (true) {
            day++
            at += 3
            if (at >= height) break
            at -= 2
        }
        assertEquals(e.toString(), day, answer(e))
    }

    @Test
    fun farm() = family(1).forEach { e ->
        val heads = int("""(\d+) têtes""", e.statement)
        val legs = int("""(\d+) pattes""", e.statement)
        val solutions = (0..heads).filter { rabbits -> 4 * rabbits + 2 * (heads - rabbits) == legs }
        assertEquals(e.toString(), listOf(answer(e)), solutions)
        assertTrue(answer(e) in 1 until heads)
    }

    @Test
    fun waterLily() = family(2).forEach { e ->
        val days = int("""en (\d+) jours""", e.statement)
        // Surface en 2⁻⁶⁰ de l'étang : couvert le jour [days].
        val half = (1..days).first { d -> 1L shl (60 - (days - d)) >= 1L shl 59 }
        assertEquals(e.toString(), half, answer(e))
    }

    @Test
    fun handshakes() = family(3).forEach { e ->
        val n = int("""(\d+) amis""", e.statement)
        var pairs = 0
        for (i in 0 until n) for (j in i + 1 until n) pairs++
        assertEquals(e.toString(), pairs, answer(e))
    }

    @Test
    fun socks() = family(4).forEach { e ->
        val red = int("""(\d+) chaussettes rouges""", e.statement)
        val blue = int("""et (\d+) bleues""", e.statement)
        // Le plus petit tirage dont le pire cas (le plus de bleues possible) donne deux rouges.
        val k = (1..red + blue).first { k -> k - minOf(k, blue) >= 2 }
        assertEquals(e.toString(), k, answer(e))
    }

    @Test
    fun racket() = family(5).forEach { e ->
        fun cents(text: String) = Regex("""(\d+)(?:,(\d\d))? €""").find(text)!!.groupValues.let {
            it[1].toInt() * 100 + (it[2].ifEmpty { "0" }).toInt()
        }
        val total = cents(e.statement.substringAfter("coûtent "))
        val gap = cents(e.statement.substringAfter("coûte "))
        val ball = (0..total).single { b -> b + (b + gap) == total }
        assertEquals(e.toString(), ball, cents(e.answer))
    }

    @Test
    fun ages() = family(6).forEach { e ->
        val times = if ("trois fois" in e.statement) 3 else 4
        val later = if ("le double" in e.statement) 2 else 3
        assertEquals(times - 1, later)
        val years = int("""Dans (\d+) ans""", e.statement)
        val brother = (1..100).single { x -> times * x + years == later * (x + years) }
        assertEquals(e.toString(), brother, answer(e))
    }

    @Test
    fun sequences() {
        val kinds = mutableSetOf<String>()
        family(7).forEach { e ->
            val terms = e.formula.single().text.removeSuffix(", ?").split(", ").map { it.toLong() }
            assertEquals(5, terms.size)
            val next = e.answer.toLong()
            // L'une des règles connaît les cinq termes et donne la réponse.
            val rules = mapOf<String, (List<Long>) -> Long?>(
                "somme" to { t -> if ((2 until t.size).all { t[it] == t[it - 1] + t[it - 2] }) t[t.size - 1] + t[t.size - 2] else null },
                "double" to { t -> if ((1 until t.size).all { t[it] == 2 * t[it - 1] }) 2 * t.last() else null },
                "écarts" to { t ->
                    val d = (1 until t.size).map { t[it] - t[it - 1] }
                    if ((1 until d.size).all { d[it] - d[it - 1] == d[1] - d[0] }) t.last() + d.last() + (d[1] - d[0]) else null
                },
                "alterné" to { t ->
                    val d = (1 until t.size).map { t[it] - t[it - 1] }
                    if ((2 until d.size).all { d[it] == d[it - 2] } && d[0] != d[1]) t.last() + d[d.size - 2] else null
                },
                "lecture" to { t -> if ((1 until t.size).all { t[it] == lookAndSay(t[it - 1]) }) lookAndSay(t.last()) else null },
            )
            val fits = rules.filter { (_, rule) -> rule(terms) == next }.keys
            assertTrue("$terms → $next", fits.isNotEmpty())
            kinds += fits
            // De tête : de petits nombres (sauf la suite qu'on lit à voix haute).
            if ("lecture" !in fits) assertTrue("$terms → $next", next <= 100)
        }
        assertEquals(setOf("somme", "double", "écarts", "alterné", "lecture"), kinds)
    }

    private fun lookAndSay(n: Long): Long {
        val s = n.toString()
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            var j = i
            while (j < s.length && s[j] == s[i]) j++
            out.append(j - i).append(s[i])
            i = j
        }
        return out.toString().toLong()
    }

    @Test
    fun clock() = family(8).forEach { e ->
        val hour = int("""à (\d+) h""", e.statement)
        val minutes = if ("h 30" in e.statement) 30 else 0
        // En demi-degrés : la petite aiguille avance d'un demi-degré par minute, la grande de 6°.
        val small = (hour * 60 + minutes) % 720
        val big = minutes * 12
        val gap = Math.abs(small - big)
        val angle = minOf(gap, 720 - gap) / 2
        assertEquals(e.toString(), angle, answer(e))
        assertTrue(e.answer.endsWith("°"))
    }

    @Test
    fun machines() = family(9).forEach { e ->
        val n = int("""Si (\d+) machines""", e.statement)
        val m = int("""faut-il à (\d+) machines""", e.statement)
        // Une machine fait n objets / (n machines × n minutes) par minute.
        val minutes = m.toDouble() / (m * (n.toDouble() / (n * n)))
        assertEquals(e.toString(), minutes, answer(e).toDouble(), 1e-9)
    }

    @Test
    fun race() {
        val ordinals = listOf("premier", "deuxième", "troisième", "quatrième", "cinquième", "sixième")
        family(10).forEach { e ->
            val passed = ordinals.indexOfFirst { "tu doubles le $it." in e.statement } + 1
            assertTrue(e.statement, passed >= 2)
            // Les coureurs dans l'ordre ; « moi » juste derrière lui, puis devant lui.
            val runners = (1..8).map { "c$it" }.toMutableList()
            runners.add(passed, "moi")
            runners.remove("moi")
            runners.add(passed - 1, "moi")
            assertEquals(e.toString(), ordinals[runners.indexOf("moi")], e.answer)
        }
    }

    @Test
    fun logs() = family(11).forEach { e ->
        val minutes = int("""Il faut (\d+) minutes""", e.statement)
        val pieces = int("""la scier en (\d+) morceaux""", e.statement)
        var count = 1
        var time = 0
        while (count < pieces) {
            count++
            time += minutes
        }
        assertEquals(e.toString(), time, answer(e))
    }

    @Test
    fun numberingAndFamilies() {
        assertEquals(293L, DailyExercise.dayIndex(LocalDate.of(2026, 10, 20)))
        assertEquals(366L, DailyExercise.dayIndex(LocalDate.of(2027, 1, 1)))
        for (e in days) {
            assertEquals(e.date.dayOfYear, e.number)
            assertEquals(Math.floorMod(DailyExercise.dayIndex(e.date), DailyExercise.FAMILIES.toLong()).toInt(), e.family)
            assertTrue(e.level, e.level == "facile" || e.level == "malin")
            // L'explication est une phrase : majuscule (sauf variable en tête) et point final.
            val first = e.explanation.first()
            assertTrue(first.italic || first.text.first().isUpperCase())
            assertTrue(e.explanation.last().text.endsWith("."))
            // Plus aucune puissance ni grand calcul : pas d'exposant, et pas de nombre de plus de 4 chiffres
            // hors de la suite qu'on lit à voix haute.
            assertTrue(e.toString(), (e.formula + e.explanation).all { it.sup.isEmpty() })
            if (e.answer != "312211") {
                val numbers = Regex("""\d+""").findAll(e.statement + e.formula.joinToString { it.text } + e.answer)
                assertTrue(e.toString(), numbers.all { it.value.length <= 4 })
            }
            // Un énoncé court : il tient en trois lignes de la page, et il est repris le lendemain.
            assertTrue(e.statement, e.statement.length <= 150)
        }
        // Le 1er janvier : la solution est celle du 31 décembre, n° 365.
        val jan1 = DailyExercise.of(LocalDate.of(2027, 1, 1).minusDays(1))
        assertEquals(365, jan1.number)
        assertEquals(LocalDate.of(2026, 12, 31), jan1.date)
        assertEquals(1, DailyExercise.of(LocalDate.of(2027, 1, 1)).number)
    }

    @Test
    fun mockupDay() {
        // 20 octobre 2026, n° 293 : le jour 293 est de la famille 5 (raquette et balle).
        val today = DailyExercise.of(LocalDate.of(2026, 10, 20))
        assertEquals(293, today.number)
        assertEquals(5, today.family)
        assertFalse(today.statement.isEmpty())
        // Euros à la française.
        assertEquals("0,05 €", DailyExercise.euros(5))
        assertEquals("1,10 €", DailyExercise.euros(110))
        assertEquals("10 €", DailyExercise.euros(1000))
    }
}
