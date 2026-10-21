package fr.perso.nimbus.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Exercice du jour de la page de déverrouillage : douze énigmes à résoudre de tête (escargot dans un
// puits, poules et lapins, nénuphar, suites logiques…), une par jour à tour de rôle ; les nombres
// changent d'un tour à l'autre et restent petits : l'astuce compte plus que le calcul.

/** Morceau de texte : [text], puis l'exposant [sup] (vide : aucun) ; [italic] pour une variable. */
data class Seg(val text: String, val sup: String = "", val italic: Boolean = false)

/** Un exercice, avec sa solution (montrée le lendemain). */
data class Exercise(
    val date: LocalDate,
    /** Numéro affiché : le jour de l'année (1 à 366). */
    val number: Int,
    /** Famille (0 à [DailyExercise.FAMILIES] − 1). */
    val family: Int,
    /** « facile » ou « malin » (une énigme à piège). */
    val level: String,
    val statement: String,
    /** Formule centrée sous l'énoncé (vide pour la plupart des énigmes). */
    val formula: List<Seg>,
    /** Explication, en phrase (majuscule initiale, point final). */
    val explanation: List<Seg>,
    val answer: String,
)

object DailyExercise {
    /** Jour 1 : le 1er janvier 2026 ; le compte continue d'une année à l'autre. */
    private val ORIGIN: LocalDate = LocalDate.of(2026, 1, 1)

    /** Nombre de familles d'énigmes, une par jour à tour de rôle. */
    const val FAMILIES = 12

    /** Espace insécable, avant une unité (« 12 m », « 5 € »). */
    private const val NBSP = ' '

    /** Espace fine insécable : avant « ? ». */
    const val THIN = ' '

    private const val EASY = "facile"
    private const val TRICK = "malin"

    /** Rang du jour [date] : 1 le 1er janvier 2026, 293 le 20 octobre 2026, 366 le 1er janvier 2027. */
    fun dayIndex(date: LocalDate): Long = ChronoUnit.DAYS.between(ORIGIN, date) + 1

    /** Exercice du jour [date]. Celui de la veille (sa solution) : `of(date.minusDays(1))`. */
    fun of(date: LocalDate): Exercise {
        val d = dayIndex(date)
        val family = Math.floorMod(d, FAMILIES.toLong()).toInt()
        // Tour : il fait varier les nombres d'un cycle de douze jours à l'autre.
        val c = Math.floorDiv(d, FAMILIES.toLong())
        val p = when (family) {
            0 -> snail(height = 8 + mod(c * 5, 13))
            1 -> farm(heads = 10 + mod(c * 3, 11), rabbits = 2 + mod(c * 5, 7))
            2 -> waterLily(days = 10 + mod(c * 7, 41))
            3 -> handshakes(n = 4 + mod(c * 3, 7))
            4 -> socks(red = 4 + mod(c * 5, 9), blue = 3 + mod(c * 3, 8))
            5 -> racket(gap = intArrayOf(1, 2, 5, 10)[mod(c, 4)], ball = intArrayOf(5, 10, 20, 50)[mod(c * 3, 4)])
            6 -> ages(times = 3 + mod(c, 2), years = 2 + mod(c * 5, 8))
            7 -> sequence(kind = mod(c, 6), start = 1 + mod(c * 5, 3))
            8 -> clock(hour = 1 + mod(c * 5, 11), half = mod(c, 2) == 1)
            9 -> machines(n = 3 + mod(c * 3, 7), m = intArrayOf(10, 20, 50, 100)[mod(c, 4)])
            10 -> race(place = 2 + mod(c * 3, 5))
            else -> logs(minutes = 2 + mod(c * 3, 5), pieces = 3 + mod(c * 5, 6))
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

    /** Le jour, l'escargot monte de 3 m ; la nuit, il glisse de 2 m : le dernier jour, il ne glisse plus. */
    private fun snail(height: Int) = Parts(
        TRICK,
        "Un escargot est au fond d’un puits de $height${NBSP}m. Le jour, il monte de 3${NBSP}m ; la nuit, il " +
            "glisse de 2${NBSP}m. En combien de jours sort-il$THIN?",
        emptyList(),
        listOf(
            Seg(
                "il gagne 1${NBSP}m par jour et nuit : au matin du jour ${height - 2}, il est à ${height - 3}${NBSP}m, " +
                    "et ses 3${NBSP}m de la journée le font sortir avant de glisser.",
            ),
        ),
        "${height - 2} jours",
    )

    private fun farm(heads: Int, rabbits: Int): Parts {
        val legs = 2 * heads + 2 * rabbits
        return Parts(
            EASY,
            "Dans une ferme, on compte $heads têtes et $legs pattes, de poules et de lapins. Combien y a-t-il " +
                "de lapins$THIN?",
            emptyList(),
            listOf(
                Seg(
                    "si tous étaient des poules, il y aurait ${2 * heads} pattes ; les ${legs - 2 * heads} de plus " +
                        "viennent des lapins, qui en ont 2 de plus chacun.",
                ),
            ),
            "$rabbits lapins",
        )
    }

    private fun waterLily(days: Int) = Parts(
        TRICK,
        "Un nénuphar double de surface chaque jour. Il couvre tout l’étang en $days jours. En combien de " +
            "jours en couvre-t-il la moitié$THIN?",
        emptyList(),
        listOf(Seg("il double chaque jour : la veille du jour où il couvre tout l’étang, il en couvrait la moitié.")),
        "${days - 1} jours",
    )

    private fun handshakes(n: Int) = Parts(
        EASY,
        "À une fête, $n amis se serrent tous la main, une fois par paire. Combien de poignées de main$THIN?",
        emptyList(),
        listOf(
            Seg(
                "chacun serre ${n - 1} mains, soit $n × ${n - 1} = ${n * (n - 1)} ; mais chaque poignée de main " +
                    "est comptée deux fois, une par ami.",
            ),
        ),
        "${n * (n - 1) / 2}",
    )

    private fun socks(red: Int, blue: Int) = Parts(
        TRICK,
        "Dans un tiroir sombre, il y a $red chaussettes rouges et $blue bleues. Combien faut-il en prendre, " +
            "au minimum, pour être sûr d’avoir deux rouges$THIN?",
        emptyList(),
        listOf(Seg("au pire, on tire d’abord les $blue bleues ; les 2 suivantes sont forcément rouges.")),
        "${blue + 2}",
    )

    /** La raquette coûte [gap] € de plus que la balle, qui coûte [ball] centimes. */
    private fun racket(gap: Int, ball: Int): Parts {
        val total = 100 * gap + 2 * ball
        return Parts(
            TRICK,
            "Une raquette et une balle coûtent ${euros(total)} en tout. La raquette coûte $gap$NBSP€ de plus " +
                "que la balle. Combien coûte la balle$THIN?",
            emptyList(),
            listOf(
                Seg(
                    "pas ${euros(total - 100 * gap)} : la raquette coûterait alors ${euros(total)}, trop cher. " +
                        "Sans l’écart de $gap$NBSP€, il reste ${euros(2 * ball)} pour deux balles.",
                ),
            ),
            euros(ball),
        )
    }

    /** Léa a [times] fois l'âge de son frère ; dans [years] ans, une fois de moins. */
    private fun ages(times: Int, years: Int): Parts {
        val now = if (times == 3) "trois" else "quatre"
        val later = if (times == 3) "le double" else "le triple"
        val brother = (times - 2) * years
        return Parts(
            TRICK,
            "Léa a $now fois l’âge de son frère. Dans $years ans, elle n’aura plus que $later de son âge. Quel " +
                "âge a son frère$THIN?",
            emptyList(),
            if (times == 3) {
                listOf(
                    Seg(
                        "l’écart d’âge ne change pas : aujourd’hui deux fois l’âge du frère, dans $years ans une " +
                            "fois son âge d’alors ; il double donc en $years ans.",
                    ),
                )
            } else {
                listOf(
                    Seg("l’écart d’âge ne change pas : 3 fois l’âge du frère aujourd’hui, 2 fois son âge dans $years ans ; "),
                    Seg("3"), Seg("x", italic = true), Seg(" = 2"), Seg("x", italic = true), Seg(" + ${2 * years}."),
                )
            },
            "$brother ans",
        )
    }

    /** Six suites, une par tour ; les cinq premiers termes, puis « ? ». */
    private fun sequence(kind: Int, start: Int): Parts {
        val terms: List<Long>
        val next: Long
        val rule: String
        when (kind) {
            0 -> {
                // Fibonacci : 1, 2, 3, 5, 8…
                val t = mutableListOf(start.toLong(), start.toLong() + 1)
                while (t.size < 6) t += t[t.size - 1] + t[t.size - 2]
                terms = t.take(5)
                next = t[5]
                rule = "chaque nombre est la somme des deux précédents"
            }
            1 -> {
                terms = (0 until 5).map { (start + it).toLong() * (start + it) }
                next = (start + 5).toLong() * (start + 5)
                rule = "ce sont les carrés de ${start}, ${start + 1}, ${start + 2}… : le suivant est ${start + 5} × ${start + 5}"
            }
            2 -> {
                terms = (0 until 5).map { start.toLong() shl it }
                next = start.toLong() shl 5
                rule = "chaque nombre est le double du précédent"
            }
            3 -> {
                // + 1, + 2, + 3…
                val t = mutableListOf(start.toLong())
                for (k in 1..5) t += t.last() + k
                terms = t.take(5)
                next = t[5]
                rule = "on ajoute 1, puis 2, puis 3, puis 4, puis 5"
            }
            4 -> {
                // + 3, − 1, + 3, − 1…
                val t = mutableListOf(start.toLong())
                for (k in 1..5) t += t.last() + if (k % 2 == 1) 3 else -1
                terms = t.take(5)
                next = t[5]
                rule = "on ajoute 3, puis on enlève 1, et ainsi de suite"
            }
            else -> {
                terms = listOf(1, 11, 21, 1211, 111221)
                next = 312211
                rule = "chaque nombre lit le précédent à voix haute : « un 1 » donne 11, « deux 1 » donne 21, " +
                    "« un 2, un 1 » donne 1211…"
            }
        }
        return Parts(
            if (kind == 5) TRICK else EASY,
            "Quel nombre vient ensuite$THIN?",
            listOf(Seg(terms.joinToString(", ") + ", ?")),
            listOf(Seg("$rule.")),
            "$next",
        )
    }

    /** Angle des aiguilles à [hour] h, ou [hour] h 30 si [half]. */
    private fun clock(hour: Int, half: Boolean): Parts {
        val hourHand = 30 * hour + if (half) 15 else 0
        val minuteHand = if (half) 180 else 0
        val gap = Math.abs(hourHand - minuteHand)
        val angle = minOf(gap, 360 - gap)
        val time = if (half) "$hour${NBSP}h${NBSP}30" else "$hour${NBSP}h"
        val explanation = if (half) {
            "la petite aiguille avance de 30° par heure, et de 15° de plus en une demi-heure : elle est à " +
                "$hourHand° de midi, la grande à 180°."
        } else {
            "les douze heures se partagent le tour : 30° par heure, et $hour × 30° = ${30 * hour}°" +
                (if (angle != 30 * hour) ", soit $angle° de l’autre côté." else ".")
        }
        return Parts(
            EASY,
            "Quel angle font les aiguilles d’une horloge à $time$THIN?",
            emptyList(),
            listOf(Seg(explanation)),
            "$angle°",
        )
    }

    private fun machines(n: Int, m: Int) = Parts(
        TRICK,
        "Si $n machines fabriquent $n objets en $n minutes, combien de minutes faut-il à $m machines pour " +
            "fabriquer $m objets$THIN?",
        emptyList(),
        listOf(Seg("chaque machine fait un objet en $n minutes ; $m machines en font $m en même temps, dans le même délai.")),
        "$n minutes",
    )

    private fun race(place: Int): Parts {
        val name = ORDINALS[place]
        return Parts(
            TRICK,
            "Pendant une course, tu doubles le $name. À quelle place es-tu maintenant$THIN?",
            emptyList(),
            listOf(Seg("tu prends sa place : il passe juste derrière toi, et ceux qui étaient devant lui restent devant toi.")),
            name,
        )
    }

    private fun logs(minutes: Int, pieces: Int) = Parts(
        TRICK,
        "Il faut $minutes minutes pour scier une bûche en 2 morceaux. Combien de minutes pour la scier en " +
            "$pieces morceaux$THIN?",
        emptyList(),
        listOf(Seg("il faut ${pieces - 1} coups de scie, pas $pieces : ${pieces - 1} × $minutes.")),
        "${(pieces - 1) * minutes} minutes",
    )

    private val ORDINALS = listOf("", "premier", "deuxième", "troisième", "quatrième", "cinquième", "sixième")

    /** Majuscule initiale, sauf si la phrase commence par une variable (« x = … »). */
    private fun sentence(segs: List<Seg>): List<Seg> {
        val first = segs.firstOrNull() ?: return segs
        if (first.italic) return segs
        return listOf(first.copy(text = first.text.replaceFirstChar { it.titlecase() })) + segs.drop(1)
    }

    /** [cents] en euros, à la française : 5 €, 1,10 €, 0,05 €. */
    fun euros(cents: Int): String =
        if (cents % 100 == 0) "${cents / 100}$NBSP€" else "${cents / 100},${"%02d".format(cents % 100)}$NBSP€"

    private fun mod(a: Long, m: Int): Int = Math.floorMod(a, m.toLong()).toInt()
}
