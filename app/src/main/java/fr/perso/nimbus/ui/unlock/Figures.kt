package fr.perso.nimbus.ui.unlock

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

// Figures du jour de la page de déverrouillage : chacune a un nom (la légende « Figure 2 – Nom »), une
// explication mathématique (montrée par le petit (i)) et son dessin au crayon. Une nouvelle à chaque
// affichage de la page, dans un ordre mélangé une fois pour toutes ; d'un tour à l'autre, ses couleurs ou
// ses paramètres changent.

/**
 * Une figure : [key] (stable, pour les captures), [title], [group] (son thème, pour la planche), son
 * explication selon la variante, et son dessin sur [FW] × [FH] unités.
 */
internal class FigureSpec(
    val key: String,
    val title: String,
    val group: String,
    val info: (variant: Int) -> String,
    val draw: Pencil.(variant: Int) -> Unit,
)

/** Toutes les figures, par thème. */
internal val ALL_FIGURES: List<FigureSpec> =
    classicFigures + fractalFigures + circleFigures + curveFigures + tilingFigures + geometryFigures + spaceFigures + numberFigures

/** La figure d'un jour et sa variante. */
internal class DailyFigure(val spec: FigureSpec, val variant: Int) {
    val title: String get() = spec.title
    val info: String get() = spec.info(variant)
}

internal object DailyFigures {
    /** Jour 1 : le 1er janvier 2026. */
    private val ORIGIN: LocalDate = LocalDate.of(2026, 1, 1)

    /** L'ordre de passage : les thèmes mélangés, pour ne pas voir cinq polyèdres de suite. */
    val order: List<FigureSpec> by lazy { ALL_FIGURES.shuffled(Random(2026)) }

    /** Rang du jour [date] : 1 le 1er janvier 2026. */
    fun dayIndex(date: LocalDate): Long = ChronoUnit.DAYS.between(ORIGIN, date) + 1

    /** La figure de rang [position] : toutes une fois, puis toutes de nouveau avec la variante suivante. */
    fun at(position: Long): DailyFigure {
        val n = order.size.toLong()
        val variant = Math.floorDiv(position, n).toInt().let { if (it < 0) -it else it }
        return DailyFigure(order[Math.floorMod(position, n).toInt()], variant)
    }

    /** La figure du jour [date] (captures). */
    fun of(date: LocalDate): DailyFigure = at(dayIndex(date))
}

/**
 * Le paquet de figures de la page de déverrouillage : une nouvelle à chaque affichage. Le rang de la
 * dernière montrée est gardé dans les préférences ; au premier usage, on part du jour.
 */
internal object FigureDeck {
    private const val PREFS = "nimbus_unlock"
    private const val KEY = "figure_position"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun last(context: Context): Long =
        prefs(context).getLong(KEY, DailyFigures.dayIndex(LocalDate.now()) - 1)

    /** La dernière figure montrée (le fond d'écran, sous la page qui s'efface). */
    fun current(context: Context): DailyFigure = DailyFigures.at(last(context))

    /** La prochaine figure, sans la compter comme vue (page posée écran éteint). */
    fun upcoming(context: Context): DailyFigure = DailyFigures.at(last(context) + 1)

    /** La prochaine figure, désormais montrée : le prochain affichage aura la suivante. */
    fun advance(context: Context): DailyFigure {
        val position = last(context) + 1
        prefs(context).edit().putLong(KEY, position).apply()
        return DailyFigures.at(position)
    }
}
