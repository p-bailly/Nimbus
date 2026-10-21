package fr.perso.nimbus.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Figure du jour de la page de déverrouillage : une figure mathématique dessinée au crayon (contour au
// crayon de papier, coloriage aux crayons de couleur), une par jour à tour de rôle ; d'un tour à
// l'autre, ses couleurs ou ses paramètres changent (table de la cardioïde, roues du spirographe…).

/** Les figures, dans l'ordre où elles reviennent. */
enum class FigureKind(val title: String) {
    ROSACE("Rosace au compas"),
    GOLDEN_SPIRAL("Spirale d’or"),
    THEODORUS("Spirale de Théodore"),
    SUNFLOWER("Tournesol"),
    SIERPINSKI("Triangle de Sierpiński"),
    POLAR_ROSE("Fleur polaire"),
    CARDIOID("Cardioïde"),
    WHIRL("Carrés tournants"),
    SPIROGRAPH("Spirographe"),
    COMPASS_ROSE("Rose des vents"),
    KOCH("Flocon de Koch"),
    DRAGON("Courbe du dragon"),
    APOLLONIUS("Baderne d’Apollonius"),
    LISSAJOUS("Courbe de Lissajous"),
    CARPET("Tapis de Sierpiński"),
    HILBERT("Courbe de Hilbert"),
    PYTHAGORAS("Arbre de Pythagore"),
}

/** La figure d'un jour : laquelle, sa variante (couleurs, paramètres) et sa légende. */
data class DailyFigure(val kind: FigureKind, val variant: Int, val caption: String)

object DailyFigures {
    /** Jour 1 : le 1er janvier 2026. */
    private val ORIGIN: LocalDate = LocalDate.of(2026, 1, 1)

    private const val NBSP = ' '

    fun of(date: LocalDate): DailyFigure {
        val d = ChronoUnit.DAYS.between(ORIGIN, date) + 1
        val n = FigureKind.entries.size.toLong()
        val kind = FigureKind.entries[Math.floorMod(d, n).toInt()]
        val variant = Math.floorDiv(d, n).toInt().let { if (it < 0) -it else it }
        return DailyFigure(kind, variant, caption(kind, variant))
    }

    /** Cardioïde : on relie n à m·n, m de 2 à 7. */
    fun cardioidTable(variant: Int): Int = 2 + variant % 6

    /** Spirographe : dents de l'anneau et de la roue. */
    fun spirograph(variant: Int): Pair<Int, Int> =
        listOf(5 to 3, 7 to 4, 8 to 3, 9 to 5, 7 to 3, 11 to 4)[variant % 6]

    /** Fleur polaire : nombre de pétales. */
    fun petals(variant: Int): Int = intArrayOf(5, 6, 8, 7)[variant % 4]

    /** Lissajous : allers-retours horizontaux et verticaux. */
    fun lissajous(variant: Int): Pair<Int, Int> = listOf(3 to 2, 5 to 4, 3 to 4, 5 to 6)[variant % 4]

    /** Triangle de Sierpiński : nombre d'étapes. */
    fun sierpinskiDepth(variant: Int): Int = if (variant % 2 == 0) 4 else 5

    /** Arbre de Pythagore : angle des branches, en degrés. */
    fun treeAngle(variant: Int): Float = floatArrayOf(45f, 40f, 50f, 36.87f)[variant % 4]

    private fun caption(kind: FigureKind, v: Int): String = when (kind) {
        FigureKind.ROSACE ->
            "six pétales tracés avec une seule ouverture de compas : chaque arc passe par le centre et par la pointe de son voisin."
        FigureKind.GOLDEN_SPIRAL ->
            "chaque carré a pour côté la somme des deux précédents, 1, 1, 2, 3, 5, 8, 13, 21 ; les quarts de cercle s’enchaînent en spirale."
        FigureKind.THEODORUS ->
            "chaque triangle rectangle a un petit côté de 1 ; ceux qui partent du centre mesurent √2, √3, √4… jusqu’à √17."
        FigureKind.SUNFLOWER ->
            "chaque graine tourne de l’angle d’or, 137,5°, par rapport à la précédente : 21 spirales dans un sens, 34 dans l’autre."
        FigureKind.SIERPINSKI -> {
            val count = if (sierpinskiDepth(v) == 4) 81 else 243
            "on ôte le triangle du milieu, puis celui du milieu de chacun des trois restants, et ainsi de suite : $count petits triangles."
        }
        FigureKind.POLAR_ROSE ->
            "chaque pétale suit la courbe r${NBSP}=${NBSP}cos${NBSP}kθ ; ${petals(v)} grands pétales, et ${petals(v)} petits entre eux."
        FigureKind.CARDIOID -> {
            val m = cardioidTable(v)
            val shape = when (m) {
                2 -> "un cœur, la cardioïde"
                3 -> "une néphroïde, à deux pointes"
                else -> "une épicycloïde à ${m - 1} pointes"
            }
            "sur un cercle de 144 points, on relie chaque point n au point $m${NBSP}×${NBSP}n : les cordes dessinent $shape."
        }
        FigureKind.WHIRL ->
            "chaque carré a ses sommets au dixième des côtés du précédent : quatre spirales apparaissent, celles de quatre fourmis qui se poursuivent."
        FigureKind.SPIROGRAPH -> {
            val (ring, wheel) = spirograph(v)
            "une roue de $wheel dents roule dans un anneau de $ring ; un crayon fixé sur la roue trace la courbe. Trois trous, trois couleurs."
        }
        FigureKind.COMPASS_ROSE ->
            "les huit directions des marins, nord, nord-est, est…, chaque pointe ombrée d’un côté comme sur les cartes anciennes."
        FigureKind.KOCH ->
            "chaque côté est coupé en trois et celui du milieu remplacé par une pointe ; à force, le contour devient infini, l’aire reste finie."
        FigureKind.DRAGON ->
            "on plie une bande de papier en deux dix fois de suite, puis on la déplie à angles droits : voici son profil."
        FigureKind.APOLLONIUS ->
            "dans chaque creux entre trois cercles qui se touchent, on loge le plus grand cercle possible, et on recommence."
        FigureKind.LISSAJOUS -> {
            val (a, b) = lissajous(v)
            "un point fait $a allers-retours à l’horizontale pendant qu’il en fait $b à la verticale."
        }
        FigureKind.CARPET ->
            "on coupe le carré en neuf et on ôte celui du milieu, puis de même dans les huit qui restent, trois fois de suite."
        FigureKind.HILBERT ->
            "une ligne qui passe par chacune des 1${NBSP}024 cases d’un damier de 32 sur 32, sans jamais se croiser."
        FigureKind.PYTHAGORAS ->
            "sur chaque carré pousse un triangle rectangle, et sur ses deux côtés deux carrés plus petits : neuf générations, du tronc aux fleurs."
    }
}
