package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Figures du thème « Pavages », deuxième série : des cubes empilés au hasard, le pavage de la chaise
// (par substitution), le pavage octogonal d'Ammann-Beenker et un parquet. Comme la première série,
// chacun remplit un cadre et y est découpé.

// ------------------------------------------------------------ Outils (repris de la première série)

/** Le cadre rectangulaire des pavages. */
private val FRAME = listOf(pt(10f, 9f), pt(358f, 9f), pt(358f, 241f), pt(10f, 241f))

/** Dessine [block] découpé par [frame], puis le contour du cadre. */
private fun Pencil.framed(frame: List<Offset> = FRAME, block: () -> Unit) {
    scope.clipPath(path(frame)) { block() }
    outline(frame, lw = 1.5f)
}

/** Vrai si le polygone touche la boîte du cadre (sinon inutile de le dessiner). */
private fun visible(p: List<Offset>, frame: List<Offset> = FRAME): Boolean {
    val fx0 = frame.minOf { it.x }; val fx1 = frame.maxOf { it.x }
    val fy0 = frame.minOf { it.y }; val fy1 = frame.maxOf { it.y }
    return p.maxOf { it.x } >= fx0 && p.minOf { it.x } <= fx1 && p.maxOf { it.y } >= fy0 && p.minOf { it.y } <= fy1
}

/** [o] tourné de [a] radians autour de [c]. */
private fun turnAround(o: Offset, c: Offset, a: Float): Offset {
    val d = o - c
    return c + Offset(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a))
}

// ------------------------------------------------------------ Cubes empilés

private val cubesCache = HashMap<Int, Array<IntArray>>()

/** Dimensions de la boîte (a, b, c) selon la variante. */
private fun cubesBox(v: Int) = listOf(Triple(6, 6, 6), Triple(7, 5, 6), Triple(5, 6, 7))[v % 3]

/**
 * Un empilement de cubes au hasard dans le coin d'une boîte a × b × c : hauteurs h(i, j) décroissantes
 * dans les deux directions (partition plane). Tirage uniforme par une longue marche aléatoire (on
 * ajoute ou retire un cube quand c'est permis), semée et mise en cache par variante.
 */
private fun cubeHeights(v: Int): Array<IntArray> = synchronized(cubesCache) {
    cubesCache.getOrPut(v) {
        val (a, b, c) = cubesBox(v)
        val random = Random(4242 + 97 * v)
        val h = Array(a) { i -> IntArray(b) { j -> if (i + j < (a + b) / 2) c / 2 else 0 } }
        repeat(60000) {
            val i = random.nextInt(a); val j = random.nextInt(b)
            val z = h[i][j]
            if (random.nextBoolean()) {
                if (z < c && (i == 0 || h[i - 1][j] > z) && (j == 0 || h[i][j - 1] > z)) h[i][j] = z + 1
            } else {
                if (z > 0 && (i == a - 1 || h[i + 1][j] < z) && (j == b - 1 || h[i][j + 1] < z)) h[i][j] = z - 1
            }
        }
        h
    }
}

/**
 * Cubes empilés : vue isométrique d'une partition plane. Chaque face visible est un losange : dessus
 * (une par colonne), et faces latérales, une par rangée et par étage ; ensemble ils pavent l'hexagone.
 */
private fun Pencil.stackedCubes(v: Int) {
    val (a, b, c) = cubesBox(v)
    val h = cubeHeights(v)
    val ex = dir(rad(30f)); val ey = dir(rad(150f)); val ez = Offset(0f, -1f)
    fun p(x: Int, y: Int, z: Int) = ex * x.toFloat() + ey * y.toFloat() + ez * z.toFloat()
    val faces = mutableListOf<Pair<List<Offset>, Int>>()
    for (i in 0 until a) for (j in 0 until b) {
        val z = h[i][j]
        faces += listOf(p(i, j, z), p(i + 1, j, z), p(i + 1, j + 1, z), p(i, j + 1, z)) to 0
    }
    for (j in 0 until b) for (k in 0 until c) {
        val x = (0 until a).count { h[it][j] > k }
        faces += listOf(p(x, j, k), p(x, j + 1, k), p(x, j + 1, k + 1), p(x, j, k + 1)) to 1
    }
    for (i in 0 until a) for (k in 0 until c) {
        val y = (0 until b).count { h[i][it] > k }
        faces += listOf(p(i, y, k), p(i + 1, y, k), p(i + 1, y, k + 1), p(i, y, k + 1)) to 2
    }
    val fit = Fit(faces.map { it.first }, 12f)
    val pals = listOf(
        listOf(Crayons.jaune, Crayons.rouge, Crayons.bleu),
        listOf(Crayons.rose, Crayons.violet, Crayons.turquoise),
        listOf(Crayons.orange, Crayons.sapin, Crayons.marine),
    )
    val pal = pals[v % pals.size]
    for ((poly, kind) in faces) {
        fill(fit(poly), pal[kind], angle = listOf(0f, 90f, 30f)[kind], gap = listOf(5f, 3.6f, 2.8f)[kind],
            wash = listOf(0.25f, 0.4f, 0.55f)[kind], lw = 1.1f)
    }
}

// ------------------------------------------------------------ Chaise

/** Une chaise (triomino en L) : coin [o], orientation [r] (quarts de tour), côté de case [s]. */
private class ChairTile(val o: Offset, val r: Int, val s: Float)

private fun quarter(p: Offset, r: Int): Offset = when (Math.floorMod(r, 4)) {
    0 -> p
    1 -> Offset(-p.y, p.x)
    2 -> Offset(-p.x, -p.y)
    else -> Offset(p.y, -p.x)
}

/** Les quatre chaises deux fois plus petites qui pavent [c]. */
private fun splitChairTile(c: ChairTile): List<ChairTile> {
    val h = c.s / 2
    return listOf(pt(0f, 0f) to 0, pt(1f, 1f) to 0, pt(4f, 0f) to 1, pt(0f, 4f) to 3).map { (p, r) ->
        ChairTile(c.o + quarter(p * h, c.r), c.r + r, h)
    }
}

private fun chairOutline(c: ChairTile): List<Offset> =
    listOf(pt(0f, 0f), pt(2f, 0f), pt(2f, 1f), pt(1f, 1f), pt(1f, 2f), pt(0f, 2f)).map { c.o + quarter(it * c.s, c.r) }

/**
 * Pavage de la chaise : un L de trois carrés se découpe en quatre L deux fois plus petits, et ainsi de
 * suite. Chaque groupe de quatre petites chaises prend la couleur de l'orientation de la chaise qu'il
 * forme, repassée en trait épais ; les hachures suivent l'orientation de chaque petite chaise.
 */
private fun Pencil.chairTiling(v: Int) {
    val unit = 12f
    // La grande chaise (5 découpes), en coordonnées y vers le haut ; on regarde une fenêtre dans son bras.
    val x0 = (v % 3) * 150f; val y0 = (v % 2) * 100f
    val toScreen = { p: Offset -> pt(10f + p.x - x0, 241f - (p.y - y0)) }
    val levels = mutableListOf(listOf(ChairTile(pt(0f, 0f), 0, unit * 32)))
    repeat(5) { levels += levels.last().flatMap { splitChairTile(it) } }
    val pals = listOf(
        listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert),
        listOf(Crayons.violet, Crayons.orange, Crayons.turquoise, Crayons.rose),
        listOf(Crayons.marine, Crayons.jaune, Crayons.rose, Crayons.turquoise),
    )
    val pal = pals[v % pals.size]
    framed {
        // Couleur de la chaise mère (quatre petites chaises par couleur), hachures selon sa propre orientation.
        for ((i, c) in levels[5].withIndex()) {
            val poly = chairOutline(c).map(toScreen)
            if (!visible(poly)) continue
            val r = Math.floorMod(c.r, 4)
            val color = lerp(pal[Math.floorMod(levels[4][i / 4].r, 4)], Graphite, 0.07f * r)
            fill(poly, color, angle = -58f + 45f * r, gap = 3.4f, wash = 0.3f + 0.05f * r, lw = 0.8f, strokeAlpha = 0.75f)
        }
        for (c in levels[4]) {
            val poly = chairOutline(c).map(toScreen)
            if (visible(poly)) outline(poly, lw = 2.1f)
        }
    }
}

// ------------------------------------------------------------ Ammann-Beenker

/**
 * Pavage octogonal d'Ammann-Beenker par la méthode des grilles de de Bruijn : quatre familles de
 * droites parallèles, à 45° les unes des autres ; chaque croisement de deux droites donne une tuile,
 * un carré (droites perpendiculaires) ou un losange d'angle 45°.
 */
private fun Pencil.ammannBeenker(v: Int) {
    val e = List(4) { dir(it * PI.toFloat() / 4) }
    val gamma = List(4) { 0.31f + 0.17f * it + 0.07f * (v % 5) }
    val s = 17f
    val range = 9
    val pals = listOf(
        listOf(Crayons.marine, Crayons.jaune, Crayons.rouge),
        listOf(Crayons.sapin, Crayons.rose, Crayons.orange),
        listOf(Crayons.violet, Crayons.jaune, Crayons.turquoise),
    )
    val pal = pals[v % pals.size]
    val turn = rad(11.25f * (v % 4))
    framed {
        for (j in 0 until 4) for (k in j + 1 until 4) for (nj in -range..range) for (nk in -range..range) {
            // Le point z où se croisent la droite nj de la famille j et la droite nk de la famille k.
            val bj = nj + gamma[j]; val bk = nk + gamma[k]
            val det = e[j].x * e[k].y - e[j].y * e[k].x
            val z = Offset((bj * e[k].y - bk * e[j].y) / det, (e[j].x * bk - e[k].x * bj) / det)
            val kk = IntArray(4) { i ->
                when (i) {
                    j -> nj
                    k -> nk
                    else -> ceil(z.x * e[i].x + z.y * e[i].y - gamma[i]).toInt()
                }
            }
            fun vertex(dj: Int, dk: Int): Offset {
                var p = Offset.Zero
                for (i in 0 until 4) p += e[i] * (kk[i] + (if (i == j) dj else 0) + (if (i == k) dk else 0)).toFloat()
                return turnAround(center + p * s, center, turn)
            }
            val tile = listOf(vertex(0, 0), vertex(1, 0), vertex(1, 1), vertex(0, 1))
            if (!visible(tile)) continue
            val square = k - j == 2
            // Quatre orientations de losanges, en deux couleurs alternées.
            val rhomb = if (k - j == 1) j else 3
            val color = if (square) lerp(pal[0], Graphite, 0.12f * j) else if (rhomb % 2 == 0) pal[1] else pal[2]
            fill(tile, color, angle = 45f * j + 20f, gap = if (square) 4f else 3f, wash = if (square) 0.3f else 0.42f, lw = 1.1f)
        }
    }
}

// ------------------------------------------------------------ Bâtons rompus

/** Rapport longueur / largeur des lames selon la variante. */
private fun plankRatio(v: Int) = listOf(4, 3, 5)[v % 3]

/**
 * Parquet à bâtons rompus : des lames k × 1, couchées puis debout, en escaliers. Les lames couchées
 * partent des cases (n + 2km, n), les lames debout des cases (n + k + 2km, n − k + 1) ; le tout est
 * tourné de 45°. Le fil du bois suit chaque lame.
 */
private fun Pencil.herringbone(v: Int) {
    val k = plankRatio(v)
    val u = 46f / k + 2f
    val random = Random(77 + v)
    val woods = if (v % 2 == 0) listOf(Crayons.brun, Crayons.orange, Crayons.jaune) else listOf(Crayons.turquoise, Crayons.bleu, Crayons.vert)
    fun place(x: Float, y: Float) = center + Offset((x - y) * u * 0.7071f, (x + y) * u * 0.7071f)
    framed {
        for (n in -30..30) for (m in -12..12) {
            for (vertical in listOf(false, true)) {
                val x = (if (vertical) n + k + 2 * k * m else n + 2 * k * m).toFloat()
                val y = (if (vertical) n - k + 1 else n).toFloat()
                val w = if (vertical) 1f else k.toFloat(); val h = if (vertical) k.toFloat() else 1f
                val poly = listOf(place(x, y), place(x + w, y), place(x + w, y + h), place(x, y + h))
                if (!visible(poly)) continue
                val tone = woods[random.nextInt(woods.size)]
                fill(poly, lerp(tone, if (vertical) Graphite else PaperColors.Paper, 0.12f), angle = if (vertical) 45f else -45f,
                    gap = 2.6f + random.nextFloat() * 1.4f, wash = 0.3f + random.nextFloat() * 0.15f, lw = 1.1f)
            }
        }
    }
}

// ------------------------------------------------------------ Les figures

internal val moreTilingFigures: List<FigureSpec> = listOf(
    FigureSpec("stacked_cubes", "Cubes empilés", "Pavages", { v ->
        val (a, b, c) = cubesBox(v)
        "Des cubes empilés au hasard dans le coin d’une boîte $a × $b × $c, vus en perspective isométrique : l’hexagone est pavé de " +
            "losanges de trois orientations. Quel que soit l’empilement, il y a toujours ${a * b} losanges couchés, ${b * c} et ${a * c} " +
            "debout. Le nombre d’empilements possibles est donné par la formule de MacMahon (1915)."
    }) { stackedCubes(it) },
    FigureSpec("chair_tiling", "Pavage de la chaise", "Pavages", { _ ->
        "Un L de trois carrés, la « chaise », se découpe en quatre chaises deux fois plus petites, qui se découpent à leur tour… " +
            "Cinq découpes donnent 4⁵ = 1024 chaises, dont on voit un morceau. Les traits épais entourent les chaises de l’étape " +
            "précédente, coloriées selon leur orientation ; les hachures suivent celle des petites. Ce pavage n’est pas périodique."
    }) { chairTiling(it) },
    FigureSpec("ammann_beenker", "Pavage d’Ammann-Beenker", "Pavages", { _ ->
        "Des carrés et des losanges d’angle 45°, de même côté. Le pavage n’est jamais périodique, mais il a des symétries d’ordre 8 " +
            "en moyenne, impossibles pour un pavage périodique. On l’obtient avec quatre familles de droites parallèles, à 45° les " +
            "unes des autres : chaque croisement de deux droites devient une tuile (méthode de de Bruijn)."
    }) { ammannBeenker(it) },
    FigureSpec("herringbone", "Parquet à bâtons rompus", "Pavages", { v ->
        val k = plankRatio(v)
        "Des lames $k fois plus longues que larges, posées alternativement en long et en large, en escaliers : c’est le parquet à " +
            "bâtons rompus. Le motif se répète par deux translations, d’une case en diagonale et de ${2 * k} cases en ligne : chaque " +
            "motif élémentaire contient une lame couchée et une lame debout, soit une aire de ${2 * k} cases."
    }) { herringbone(it) },
)
