package fr.perso.nimbus.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Exercice du jour de la page de déverrouillage : huit familles de calcul de tête, toutes du niveau
// lycée (suites, dénombrement, logarithme…), une par jour à tour de rôle ; les nombres changent d'un tour à l'autre.

/** Morceau de texte : [text], puis l'exposant [sup] (vide : aucun) ; [italic] pour une variable. */
data class Seg(val text: String, val sup: String = "", val italic: Boolean = false)

/** Un exercice, avec sa solution (montrée le lendemain). */
data class Exercise(
    val date: LocalDate,
    /** Numéro affiché : le jour de l'année (1 à 366). */
    val number: Int,
    /** Famille (0 à 7). */
    val family: Int,
    /** Toujours « lycée ». */
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
            0 -> powersOfTwo(n = 10 + mod(c * 5, 11))
            1 -> arithmetic(a = 2 + mod(c * 3, 8), r = 3 + mod(c * 5, 9), n = 20 + mod(c * 7, 21))
            2 -> committees(n = 10 + mod(c * 7, 21))
            3 -> squaresSum(n = 10 + mod(c * 11, 31))
            4 -> digits(k = 60 + mod(c * 37, 140))
            5 -> unitsDigit(base = intArrayOf(7, 3, 8, 2)[mod(c, 4)], exponent = date.year)
            6 -> geometricSeries(q = 2 + mod(c * 3, 8))
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

    private fun powersOfTwo(n: Int) = Parts(
        "lycée",
        "Calculer la somme des puissances de 2 :",
        listOf(Seg("1 + 2 + 4 + ⋯ + 2", "$n")),
        listOf(
            Seg("c’est une suite géométrique de raison 2 : en ajoutant 1, chaque terme double le total (1 + 1 = 2, 2 + 2 = 4…), d’où 2", "${n + 1}"),
            Seg(" − 1."),
        ),
        spaced((1L shl (n + 1)) - 1),
    )

    private fun arithmetic(a: Int, r: Int, n: Int): Parts {
        val last = a + (n - 1) * r
        return Parts(
            "lycée",
            "Calculer la somme de ces termes d’une suite arithmétique :",
            listOf(Seg("$a + ${a + r} + ${a + 2 * r} + ⋯ + $last")),
            listOf(
                Seg(
                    "il y a ($last − $a) / $r + 1 = $n termes, et la somme vaut le nombre de termes fois la moyenne " +
                        "du premier et du dernier : $n × ${a + last} / 2.",
                ),
            ),
            spaced(n.toLong() * (a + last) / 2),
        )
    }

    private fun committees(n: Int) = Parts(
        "lycée",
        "De combien de façons peut-on choisir 3 délégués parmi $n élèves$THIN?",
        emptyList(),
        listOf(
            Seg(
                "on compte les listes ordonnées, $n × ${n - 1} × ${n - 2}, puis on divise par les 3 × 2 × 1 = 6 ordres " +
                    "d’un même trio.",
            ),
        ),
        spaced(n.toLong() * (n - 1) * (n - 2) / 6),
    )

    private fun squaresSum(n: Int) = Parts(
        "lycée",
        "Calculer la somme des carrés :",
        listOf(Seg("1", "2"), Seg(" + 2", "2"), Seg(" + 3", "2"), Seg(" + ⋯ + $n", "2")),
        listOf(
            Seg("par récurrence, 1", "2"), Seg(" + ⋯ + "), Seg("n", "2", italic = true), Seg(" = "),
            Seg("n", italic = true), Seg("("), Seg("n", italic = true), Seg(" + 1)(2"), Seg("n", italic = true),
            Seg(" + 1) / 6, ici $n × ${n + 1} × ${2 * n + 1} / 6."),
        ),
        spaced(n.toLong() * (n + 1) * (2 * n + 1) / 6),
    )

    private fun digits(k: Int): Parts {
        val log = k * Math.log10(2.0)
        val exponent = Math.floor(log).toInt()
        return Parts(
            "lycée",
            "Combien de chiffres a ce nombre$THIN?",
            listOf(Seg("2", "$k")),
            listOf(
                Seg("avec le logarithme décimal, log 2", "$k"),
                Seg(" = $k × log 2 ≈ ${decimal2(log)} : le nombre est entre 10", "$exponent"),
                Seg(" et 10", "${exponent + 1}"),
                Seg(", il a ${exponent + 1} chiffres."),
            ),
            "${exponent + 1}",
        )
    }

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

    private fun geometricSeries(q: Int) = Parts(
        "lycée",
        "Calculer la somme infinie :",
        listOf(Seg("1 + 1/$q + 1/$q", "2"), Seg(" + ⋯")),
        listOf(
            Seg("pour une raison "), Seg("r", italic = true), Seg(" entre −1 et 1, 1 + "), Seg("r", italic = true),
            Seg(" + "), Seg("r", "2", italic = true), Seg(" + ⋯ = 1 / (1 − "), Seg("r", italic = true),
            Seg(") ; ici 1 / (1 − 1/$q) = $q/${q - 1}."),
        ),
        "$q/${q - 1}",
    )

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

    /** [x] à deux décimales, à la française : 41,24. */
    private fun decimal2(x: Double): String = String.format(java.util.Locale.FRENCH, "%.2f", x)

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
