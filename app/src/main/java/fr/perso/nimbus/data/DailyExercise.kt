package fr.perso.nimbus.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Exercice du jour de la page de déverrouillage : huit familles de calcul de tête (celles de la
// maquette H8), une par jour à tour de rôle ; les nombres changent d'un tour à l'autre.

/** Morceau de texte : [text], puis l'exposant [sup] (vide : aucun) ; [italic] pour une variable. */
data class Seg(val text: String, val sup: String = "", val italic: Boolean = false)

/** Un exercice, avec sa solution (montrée le lendemain). */
data class Exercise(
    val date: LocalDate,
    /** Numéro affiché : le jour de l'année (1 à 366). */
    val number: Int,
    /** Famille (0 à 7). */
    val family: Int,
    /** « collège » ou « lycée ». */
    val level: String,
    val statement: String,
    /** Formule centrée sous l'énoncé (vide pour certaines familles). */
    val formula: List<Seg>,
    /** Explication, en phrase (majuscule initiale, point final). */
    val explanation: List<Seg>,
    val answer: String,
)

object DailyExercise {
    /** Jour 1 : le 1er janvier 2026 ; le compte continue d'une année à l'autre. */
    private val ORIGIN: LocalDate = LocalDate.of(2026, 1, 1)

    /** Espace insécable, avant « % » (de la chasse d'une espace ordinaire). */
    private const val NBSP = '\u00A0'

    /** Espace fine insécable : avant « ? », entre les milliers (2 415). */
    const val THIN = '\u202F'

    /** Rang du jour [date] : 1 le 1er janvier 2026, 293 le 20 octobre 2026, 366 le 1er janvier 2027. */
    fun dayIndex(date: LocalDate): Long = ChronoUnit.DAYS.between(ORIGIN, date) + 1

    /** Exercice du jour [date]. Celui de la veille (sa solution) : `of(date.minusDays(1))`. */
    fun of(date: LocalDate): Exercise {
        val d = dayIndex(date)
        val family = Math.floorMod(d, 8L).toInt()
        // Tour : il fait varier les nombres d'une semaine (de huit jours) à l'autre.
        val c = Math.floorDiv(d, 8L)
        val p = when (family) {
            0 -> sumTo(n = 30 + mod(c * 37, 70))
            1 -> handshakes(n = 12 + mod(c * 17, 30))
            2 -> squares(q = 2 + mod(c * 7, 18))
            3 -> odds(n = 15 + mod(c * 13, 40))
            4 -> diagonals(n = 8 + mod(c * 9, 20))
            5 -> unitsDigit(base = intArrayOf(7, 3, 8, 2)[mod(c, 4)], exponent = date.year)
            6 -> endsWithFive(m = 3 + mod(c * 3, 7))
            else -> percent(p = 10 * (1 + mod(c * 2, 5)))
        }
        return Exercise(
            date = date,
            number = date.dayOfYear,
            family = family,
            level = p.level,
            statement = p.statement,
            formula = p.formula,
            explanation = sentence(p.explanation),
            answer = p.answer,
        )
    }

    private class Parts(
        val level: String,
        val statement: String,
        val formula: List<Seg>,
        val explanation: List<Seg>,
        val answer: String,
    )

    private fun sumTo(n: Int) = Parts(
        "collège",
        "Calculer la somme de tous les entiers de 1 à $n.",
        listOf(Seg("1 + 2 + 3 + ⋯ + $n")),
        listOf(Seg("en écrivant la somme dans les deux sens, chaque colonne vaut ${n + 1}, d’où $n × ${n + 1} / 2.")),
        spaced(n.toLong() * (n + 1) / 2),
    )

    private fun handshakes(n: Int) = Parts(
        "collège",
        "À une fête, $n personnes se serrent toutes la main, une seule fois par paire. Combien y a-t-il de poignées de main$THIN?",
        emptyList(),
        listOf(Seg("chacun serre ${n - 1} mains et chaque poignée relie deux personnes : $n × ${n - 1} / 2.")),
        spaced(n.toLong() * (n - 1) / 2),
    )

    private fun squares(q: Int): Parts {
        val a = 50 + q
        val b = 50 - q
        return Parts(
            "collège",
            "Calculer, sans poser de multiplication :",
            listOf(Seg("$a", "2"), Seg(" − $b", "2")),
            listOf(
                Seg("a", "2", italic = true), Seg(" − "), Seg("b", "2", italic = true), Seg(" = ("),
                Seg("a", italic = true), Seg(" − "), Seg("b", italic = true), Seg(")("),
                Seg("a", italic = true), Seg(" + "), Seg("b", italic = true), Seg(") = ${a - b} × 100."),
            ),
            spaced(200L * q),
        )
    }

    private fun odds(n: Int) = Parts(
        "collège",
        "Calculer la somme des $n premiers nombres impairs :",
        listOf(Seg("1 + 3 + 5 + ⋯ + ${2 * n - 1}")),
        listOf(
            Seg("les sommes 1, 1 + 3, 1 + 3 + 5… valent 1, 4, 9… : la somme des "), Seg("n", italic = true),
            Seg(" premiers impairs est "), Seg("n", "2", italic = true), Seg("."),
        ),
        spaced(n.toLong() * n),
    )

    private fun diagonals(n: Int) = Parts(
        "collège",
        "Combien de diagonales a un polygone convexe à $n côtés$THIN?",
        emptyList(),
        listOf(
            Seg(
                "chaque sommet est relié à ${n - 3} autres par une diagonale, et chaque diagonale a deux extrémités : " +
                    "$n × ${n - 3} / 2.",
            ),
        ),
        spaced(n.toLong() * (n - 3) / 2),
    )

    private fun unitsDigit(base: Int, exponent: Int): Parts {
        // Chiffres des unités de base¹ à base⁴ : le cycle se répète ensuite.
        val cycle = (1..4).map { k -> pow(base, k) % 10 }
        val rest = Math.floorMod(exponent, 4)
        return Parts(
            "lycée",
            "Quel est le chiffre des unités de ce nombre$THIN?",
            listOf(Seg("$base", "$exponent")),
            listOf(
                Seg(
                    "les puissances de $base finissent par ${cycle.joinToString(", ")}, puis recommencent ; " +
                        "$exponent = 4 × ${Math.floorDiv(exponent, 4)} + $rest, donc $base",
                    "$exponent",
                ),
                Seg(" finit comme $base", "${if (rest == 0) 4 else rest}"),
                Seg("."),
            ),
            "${cycle[(rest + 3) % 4]}",
        )
    }

    private fun endsWithFive(m: Int): Parts {
        val x = 10 * m + 5
        return Parts(
            "collège",
            "Calculer de tête :",
            listOf(Seg("$x", "2")),
            listOf(
                Seg(
                    "pour un nombre qui finit par 5, on multiplie ses dizaines $m par ${m + 1} et l’on écrit 25 derrière : " +
                        "$m × ${m + 1} = ${m * (m + 1)}.",
                ),
            ),
            spaced(m.toLong() * (m + 1) * 100 + 25),
        )
    }

    private fun percent(p: Int): Parts {
        // En centièmes : p % = p / 100 ; le prix final vaut (10 000 − p²) / 10 000 du prix de départ.
        val rate = decimal(p, 100)
        return Parts(
            "lycée",
            "Un prix augmente de $p$NBSP%, puis baisse de $p$NBSP%. De combien de pour cent a-t-il baissé en tout$THIN?",
            emptyList(),
            listOf(
                Seg("il est multiplié par (1 + $rate)(1 − $rate) = 1 − $rate", "2"),
                Seg(" = ${decimal(10_000 - p * p, 10_000)}."),
            ),
            // La baisse totale est de p² / 100 %, un entier : p est un multiple de 10.
            "${p * p / 100}$NBSP%",
        )
    }

    /** Majuscule initiale, sauf si la phrase commence par une variable (« a² − b² = … »). */
    private fun sentence(segs: List<Seg>): List<Seg> {
        val first = segs.firstOrNull() ?: return segs
        if (first.italic) return segs
        return listOf(first.copy(text = first.text.replaceFirstChar { it.titlecase() })) + segs.drop(1)
    }

    /** [n] avec ses milliers séparés par une espace fine insécable : 2 415. */
    private fun spaced(n: Long): String = n.toString().reversed().chunked(3).joinToString("$THIN").reversed()

    /** [num] / [den] en écriture décimale à la française, sans zéro inutile : 0,3 ; 0,91. */
    private fun decimal(num: Int, den: Int): String =
        java.math.BigDecimal(num).divide(java.math.BigDecimal(den)).stripTrailingZeros().toPlainString().replace('.', ',')

    private fun pow(base: Int, k: Int): Int {
        var r = 1
        repeat(k) { r *= base }
        return r
    }

    private fun mod(a: Long, m: Int): Int = Math.floorMod(a, m.toLong()).toInt()
}
