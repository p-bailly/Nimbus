package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Figures du thème « Pavages », deuxième série : un zellige d'étoiles, des poissons à la Escher, des
// pavages aléatoires (diamant aztèque, cubes empilés), des pavages par substitution (chaise, pinwheel),
// le pavage octogonal d'Ammann-Beenker, le pavage hexagonal adouci et un parquet. Comme la première
// série, chacun remplit un cadre et y est découpé.

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

/** Polygone régulier : [n] sommets sur un cercle de rayon [r], le premier à l'angle [start] (degrés). */
private fun regular(c: Offset, r: Float, n: Int, start: Float): List<Offset> =
    List(n) { c + dir(rad(start + 360f * it / n)) * r }

/** [o] tourné de [a] radians autour de [c]. */
private fun turnAround(o: Offset, c: Offset, a: Float): Offset {
    val d = o - c
    return c + Offset(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a))
}

/** L'entier [n] en exposant (2¹⁰…). */
private fun superscript(n: Int): String = n.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")

// ------------------------------------------------------------ Zellige

/**
 * Étoile de Hankin dans le polygone [poly] : de chaque milieu de côté partent deux rayons inclinés de
 * [theta] sur le côté ; les rayons de deux côtés voisins se croisent près du sommet commun. Rend les
 * points milieu, croisement, milieu, croisement…
 */
private fun hankin(poly: List<Offset>, theta: Float): List<Offset> {
    val c = poly.reduce { a, b -> a + b } / poly.size.toFloat()
    fun turned(d: Offset, a: Float) = Offset(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a))
    fun inward(from: Offset, d: Offset): Offset {
        val u = d / d.getDistance()
        val p = turned(u, theta); val q = turned(u, -theta)
        val toC = c - from
        return if (p.x * toC.x + p.y * toC.y > q.x * toC.x + q.y * toC.y) p else q
    }
    val out = mutableListOf<Offset>()
    val n = poly.size
    for (i in 0 until n) {
        val a = poly[i]; val b = poly[(i + 1) % n]; val e = poly[(i + 2) % n]
        val m1 = (a + b) / 2f; val m2 = (b + e) / 2f
        val d1 = inward(m1, b - a); val d2 = inward(m2, b - e)
        // m1 + t d1 = m2 + u d2.
        val det = d1.x * (-d2.y) - d1.y * (-d2.x)
        val rx = m2.x - m1.x; val ry = m2.y - m1.y
        val t = (rx * (-d2.y) - ry * (-d2.x)) / det
        out += m1
        out += m1 + d1 * t
    }
    return out
}

/**
 * Zellige : le pavage 4.6.12 (dodécagones, hexagones et carrés réguliers) sert de canevas ; dans chaque
 * tuile, la méthode de Hankin trace une étoile (à 12, 6 ou 4 branches). Les coins restants forment des
 * rosaces autour des sommets ; les entrelacs sont dessinés en double trait.
 */
private fun Pencil.zellige(v: Int) {
    val a = 13f
    val r12 = a * (2 + sqrt(3f)) / 2
    val big = a / (2 * sin(rad(15f)))
    val l = a * (3 + sqrt(3f))
    val u1 = dir(0f) * l; val u2 = dir(rad(60f)) * l
    val tiles = mutableListOf<Pair<List<Offset>, Int>>()
    val o = center + Offset(0f, 0f)
    for (i in -5..5) for (j in -4..4) {
        val c = o + u1 * (i - j / 2).toFloat() + u2 * j.toFloat()
        tiles += regular(c, big, 12, 15f) to 0
        for (k in 0..2) {
            val d = rad(60f * k)
            tiles += regular(c + dir(d) * (r12 + a / 2), a / sqrt(2f), 4, 60f * k + 45f) to 2
        }
        tiles += regular(c + dir(rad(30f)) * (a * (1 + sqrt(3f))), a, 6, 0f) to 1
        tiles += regular(c + dir(rad(-30f)) * (a * (1 + sqrt(3f))), a, 6, 0f) to 1
    }
    val shown = tiles.filter { visible(it.first) }
    val pals = listOf(
        listOf(Crayons.sapin, Crayons.marine, Crayons.jaune, Crayons.turquoise),
        listOf(Crayons.marine, Crayons.rouge, Crayons.jaune, Crayons.bleu),
        listOf(Crayons.brun, Crayons.sapin, Crayons.orange, Crayons.jaune),
        listOf(Crayons.violet, Crayons.turquoise, Crayons.rose, Crayons.bleu),
    )
    val pal = pals[v % pals.size]
    val theta = rad(ZELLIGE_ANGLE)
    framed {
        for ((poly, kind) in shown) {
            val star = hankin(poly, theta)
            // Les coins de la tuile, hors de l'étoile : un morceau de rosace autour de chaque sommet.
            for (i in poly.indices) {
                val corner = listOf(star[2 * i], poly[(i + 1) % poly.size], star[(2 * i + 2) % star.size], star[2 * i + 1])
                fill(corner, pal[3], angle = 30f, gap = 3.6f, wash = 0.22f, outline = false, strokeAlpha = 0.55f)
            }
            fill(star, pal[kind], angle = -58f + 40f * kind, gap = if (kind == 0) 3.2f else 2.8f, wash = 0.42f, outline = false)
        }
        // Les entrelacs : chaque étoile en double trait, graphite puis papier.
        for ((poly, _) in shown) {
            val star = hankin(poly, theta)
            line(star + star[0], Graphite, 3.4f)
            line(star + star[0], PaperColors.Paper, 1.5f)
        }
    }
}

/** Angle des rayons de Hankin sur les côtés, en degrés. */
private const val ZELLIGE_ANGLE = 72f

// ------------------------------------------------------------ Poissons à la Escher

/** Courbe lisse (Catmull-Rom) par les points [p], [steps] pas par intervalle. */
private fun smooth(p: List<Offset>, steps: Int = 6): List<Offset> {
    val out = mutableListOf<Offset>()
    for (i in 0 until p.size - 1) {
        val p0 = p[max(0, i - 1)]; val p1 = p[i]; val p2 = p[i + 1]; val p3 = p[min(p.size - 1, i + 2)]
        for (s in 0 until steps) {
            val t = s.toFloat() / steps; val t2 = t * t; val t3 = t2 * t
            out += (p1 * 2f + (p2 - p0) * t + (p0 * 2f - p1 * 5f + p2 * 4f - p3) * t2 + (p1 * 3f - p0 - p2 * 3f + p3) * t3) * 0.5f
        }
    }
    out += p.last()
    return out
}

/** Translations du pavage de poissons. */
private val FISH_A = Offset(70f, 0f)
private val FISH_B = Offset(14f, 40f)

/** Le dos (de P0 à P0 + A) et la queue (de P0 à P0 + B) du poisson ; le ventre et la tête s'en déduisent par translation. */
private val FISH_BACK = smooth(listOf(pt(0f, 0f), pt(12f, -2f), pt(24f, -7f), pt(32f, -14f), pt(37f, -6f), pt(48f, -4f), pt(58f, -3f), pt(70f, 0f)))
private val FISH_TAIL = smooth(listOf(pt(0f, 0f), pt(-10f, 3f), pt(2f, 14f), pt(8f, 20f), pt(4f, 26f), pt(-8f, 34f), pt(14f, 40f)))

/** Le contour du poisson : dos, tête (queue + A), ventre (dos + B, à l'envers), queue (à l'envers). */
private val FISH = FISH_BACK.dropLast(1) + FISH_TAIL.map { it + FISH_A }.dropLast(1) +
    FISH_BACK.map { it + FISH_B }.reversed().dropLast(1) + FISH_TAIL.reversed().dropLast(1)

/**
 * Poissons à la Escher : un parallélogramme dont on déforme les côtés, ce qu'on retire d'un côté
 * revenant au côté opposé par translation. Les queues s'emboîtent dans les têtes, les nageoires dorsales
 * dans les ventres ; un œil, une ouïe et quelques écailles font le reste.
 */
private fun Pencil.escherFish(v: Int) {
    val pals = listOf(
        listOf(Crayons.orange, Crayons.bleu, Crayons.jaune),
        listOf(Crayons.turquoise, Crayons.rose, Crayons.jaune),
        listOf(Crayons.rouge, Crayons.turquoise, Crayons.jaune),
        listOf(Crayons.violet, Crayons.vert, Crayons.orange),
    )
    val pal = pals[v % pals.size]
    val tilt = rad(if (v % 2 == 0) -8f else 8f)
    val o = pt(-40f, -30f)
    framed {
        for (j in -2..8) for (i in -3..7) {
            val base = o + FISH_A * i.toFloat() + FISH_B * j.toFloat()
            fun at(p: Offset) = turnAround(base + p, center, tilt)
            val body = FISH.map { at(it) }
            if (!visible(body)) continue
            val color = pal[Math.floorMod(i + 2 * j, 3)]
            fill(body, color, angle = -40f + 25f * Math.floorMod(j, 3), gap = 3.6f, wash = 0.36f, lw = 1.3f)
            // Ouïe, écailles, nageoire, œil.
            line(smooth(listOf(pt(56f, 6f), pt(52f, 16f), pt(53f, 26f))).map { at(it) }, Graphite, 1f, 0.8f)
            for ((sx, sy) in listOf(24f to 12f, 34f to 20f, 24f to 26f, 42f to 10f, 44f to 28f, 14f to 19f)) {
                line(smooth(listOf(pt(sx, sy - 4f), pt(sx + 3f, sy), pt(sx, sy + 4f)), 3).map { at(it) }, lerp(color, Graphite, 0.4f), 0.9f, 0.7f)
            }
            line(smooth(listOf(pt(40f, 30f), pt(46f, 34f), pt(52f, 32f))).map { at(it) }, Graphite, 0.9f, 0.6f)
            val eye = at(pt(65f, 13f))
            fill(circle(eye.x, eye.y, 3.2f, 16), PaperColors.Paper, hatch = false, wash = 1f, lw = 0.9f)
            dot(eye + Offset(0.6f, 0.2f), 1.5f, Graphite)
        }
    }
}

// ------------------------------------------------------------ Diamant aztèque

/** Directions des dominos : nord, sud (horizontaux), est, ouest (verticaux). */
private const val DOM_N = 0
private const val DOM_S = 1
private const val DOM_E = 2
private const val DOM_W = 3

private val aztecCache = HashMap<Long, List<IntArray>>()

/**
 * Pavage aléatoire du diamant aztèque d'ordre [n], par brassage des dominos (Elkies, Kuperberg, Larsen
 * et Propp) : à chaque étape, on retire les paires de dominos qui vont se heurter, chaque domino avance
 * d'une case dans sa direction (le diamant grandit d'un ordre), puis on comble les carrés 2 × 2 vides
 * par deux dominos parallèles, à pile ou face. Un domino est [x, y, direction], (x, y) sa case en bas à
 * gauche ; sa direction se lit sur la couleur de cette case dans le damier.
 */
private fun aztecDiamond(n: Int, seed: Long): List<IntArray> = synchronized(aztecCache) {
    aztecCache.getOrPut(seed * 100 + n) {
        val random = Random(seed)
        val size = 2 * n + 8; val off = n + 4
        fun idx(x: Int, y: Int) = (x + off) * size + (y + off)
        val anchor = IntArray(size * size)
        val occupied = BooleanArray(size * size)
        var dominoes = ArrayList<IntArray>()
        for (m in 0 until n) {
            anchor.fill(-1)
            for (d in dominoes) anchor[idx(d[0], d[1])] = d[2]
            val moved = ArrayList<IntArray>()
            for (d in dominoes) {
                val x = d[0]; val y = d[1]
                val collides = when (d[2]) {
                    DOM_N -> anchor[idx(x, y + 1)] == DOM_S
                    DOM_S -> anchor[idx(x, y - 1)] == DOM_N
                    DOM_E -> anchor[idx(x + 1, y)] == DOM_W
                    else -> anchor[idx(x - 1, y)] == DOM_E
                }
                if (collides) continue
                when (d[2]) {
                    DOM_N -> moved += intArrayOf(x, y + 1, DOM_N)
                    DOM_S -> moved += intArrayOf(x, y - 1, DOM_S)
                    DOM_E -> moved += intArrayOf(x + 1, y, DOM_E)
                    else -> moved += intArrayOf(x - 1, y, DOM_W)
                }
            }
            val o = m + 1
            occupied.fill(false)
            for (d in moved) {
                occupied[idx(d[0], d[1])] = true
                if (d[2] <= DOM_S) occupied[idx(d[0] + 1, d[1])] = true else occupied[idx(d[0], d[1] + 1)] = true
            }
            fun even(k: Int) = Math.floorMod(k, 2) == 0
            for (y in -o until o) {
                val k = min(o + 1 + y, o - y)
                for (x in -k until k) {
                    if (occupied[idx(x, y)]) continue
                    if (random.nextBoolean()) {
                        moved += intArrayOf(x, y, if (even(x + y + o)) DOM_N else DOM_S)
                        moved += intArrayOf(x, y + 1, if (even(x + y + 1 + o)) DOM_N else DOM_S)
                    } else {
                        moved += intArrayOf(x, y, if (even(x + y + o)) DOM_E else DOM_W)
                        moved += intArrayOf(x + 1, y, if (even(x + 1 + y + o)) DOM_E else DOM_W)
                    }
                    for ((dx, dy) in listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)) occupied[idx(x + dx, y + dy)] = true
                }
            }
            dominoes = moved
        }
        dominoes
    }
}

/** Ordre du diamant aztèque selon la variante. */
private fun aztecOrder(v: Int) = listOf(20, 24, 22)[v % 3]

/**
 * Diamant aztèque : un pavage aléatoire par dominos, coloré selon la direction de chaque domino. Les
 * quatre coins sont gelés (une seule direction), le centre est désordonné ; la frontière tend vers le
 * cercle inscrit, le « cercle arctique », tracé en pointillé.
 */
private fun Pencil.aztec(v: Int) {
    val n = aztecOrder(v)
    val dominoes = aztecDiamond(n, 1789L + 31L * v)
    val cell = 228f / (2 * n)
    val pals = listOf(
        listOf(Crayons.rouge, Crayons.bleu, Crayons.jaune, Crayons.vert),
        listOf(Crayons.violet, Crayons.orange, Crayons.turquoise, Crayons.rose),
        listOf(Crayons.marine, Crayons.jaune, Crayons.rouge, Crayons.turquoise),
    )
    val pal = pals[v % pals.size]
    fun at(x: Float, y: Float) = pt(center.x + x * cell, center.y - y * cell)
    for (d in dominoes) {
        val w = if (d[2] <= DOM_S) 2f else 1f
        val h = if (d[2] <= DOM_S) 1f else 2f
        val x = d[0].toFloat(); val y = d[1].toFloat()
        val rect = listOf(at(x, y), at(x + w, y), at(x + w, y + h), at(x, y + h))
        fill(rect, pal[d[2]], angle = if (d[2] <= DOM_S) 20f else 70f, gap = 2.6f, wash = 0.42f, lw = 0.7f, strokeAlpha = 0.7f)
    }
    // Le cercle arctique.
    val r = n * cell / sqrt(2f)
    val ring = circle(center.x, center.y, r, 120)
    for (i in ring.indices step 2) stroke(ring[i], ring[(i + 1) % ring.size], Graphite, 1.2f, 0.75f)
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

// ------------------------------------------------------------ Pinwheel

/** Un triangle rectangle de côtés 1, 2, √5 : [a] l'angle droit, [b] au bout du grand côté, [c] du petit. */
private class PinwheelTriangle(val a: Offset, val b: Offset, val c: Offset)

/**
 * Pavage pinwheel de Conway et Radin : chaque triangle se découpe en cinq triangles semblables, √5 fois
 * plus petits : la hauteur issue de l'angle droit, puis le plus grand morceau coupé en quatre par ses
 * milieux, le rectangle du milieu étant recoupé selon son autre diagonale (c'est ce qui fait tourner les
 * tuiles). Après quatre découpes, on regarde une fenêtre près de l'angle droit du grand triangle.
 */
private fun Pencil.pinwheel(v: Int) {
    val l = 520f
    val dx = (v % 3) * 60f; val dy = (v % 2) * 30f
    val a0 = pt(10f - dx, 241f + dy)
    var tris = listOf(PinwheelTriangle(a0, a0 + Offset(2 * l, 0f), a0 + Offset(0f, -l)))
    repeat(4) {
        tris = tris.flatMap { t ->
            val bc = t.c - t.b
            val s = ((t.a - t.b).x * bc.x + (t.a - t.b).y * bc.y) / (bc.x * bc.x + bc.y * bc.y)
            val h = t.b + bc * s
            val m1 = (h + t.b) / 2f; val m2 = (t.b + t.a) / 2f; val m3 = (t.a + h) / 2f
            // Le rectangle h, m1, m2, m3 est coupé selon la diagonale h–m2 : deux triangles retournés.
            listOf(PinwheelTriangle(h, t.a, t.c), PinwheelTriangle(m1, t.b, m2), PinwheelTriangle(m3, m2, t.a), PinwheelTriangle(m1, h, m2), PinwheelTriangle(m3, m2, h))
        }
    }
    val mirror = v % 2 == 1
    val pals = listOf(
        listOf(Crayons.bleu, Crayons.jaune, Crayons.rouge),
        listOf(Crayons.turquoise, Crayons.rose, Crayons.violet),
        listOf(Crayons.vert, Crayons.orange, Crayons.marine),
    )
    val pal = pals[v % pals.size]
    framed {
        for ((i, t) in tris.withIndex()) {
            val poly = listOf(t.a, t.b, t.c).map { if (mirror) Offset(FW - it.x, it.y) else it }
            if (!visible(poly)) continue
            val ab = t.b - t.a; val ac = t.c - t.a
            val hand = if (ab.x * ac.y - ab.y * ac.x > 0) 0 else 1
            // Hachures le long du grand côté : on voit tourner les tuiles.
            val along = Math.toDegrees(kotlin.math.atan2(ab.y.toDouble(), ab.x.toDouble())).toFloat() * (if (mirror) -1f else 1f)
            val color = if (i % 5 == 0) pal[2] else pal[hand]
            fill(poly, color, angle = along, gap = 3.4f, wash = 0.36f, lw = 1f)
        }
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

// ------------------------------------------------------------ Pavage hexagonal adouci

/**
 * Pavage hexagonal adouci (3.3.3.3.6) : dans le pavage par triangles, on retire un sommet sur sept
 * (ceux où i + 3j est multiple de 7) ; les six triangles autour de chacun fusionnent en un hexagone.
 * Chaque autre sommet touche exactement un hexagone. Il est chiral : la variante impaire est son reflet.
 */
private fun Pencil.snubHexagonal(v: Int) {
    val a = 21f
    val e1 = Offset(a, 0f); val e2 = Offset(a / 2, a * sqrt(3f) / 2)
    val mirror = v % 2 == 1
    val o = center
    fun p(i: Int, j: Int): Offset {
        val q = o + e1 * i.toFloat() + e2 * j.toFloat()
        return if (mirror) Offset(FW - q.x, q.y) else q
    }
    fun isCenter(i: Int, j: Int) = Math.floorMod(i + 3 * j, 7) == 0
    val around = listOf(1 to 0, 0 to 1, -1 to 1, -1 to 0, 0 to -1, 1 to -1)
    fun centerOf(i: Int, j: Int): Pair<Int, Int> = around.map { (di, dj) -> i + di to j + dj }.first { isCenter(it.first, it.second) }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val hexColors = listOf(pal[0], pal[2], pal[5])
    framed {
        for (j in -9..9) for (i in -15..15) {
            for (up in listOf(true, false)) {
                val tri = if (up) listOf(i to j, i + 1 to j, i to j + 1) else listOf(i + 1 to j, i + 1 to j + 1, i to j + 1)
                if (tri.any { isCenter(it.first, it.second) }) continue
                val poly = tri.map { p(it.first, it.second) }
                if (!visible(poly)) continue
                val centers = tri.map { centerOf(it.first, it.second) }.toSet()
                val touching = centers.size < 3
                fill(poly, if (touching) pal[3] else pal[7], angle = if (up) 30f else -30f, gap = 3f, wash = if (touching) 0.3f else 0.5f, lw = 1f)
            }
            if (isCenter(i, j)) {
                val hex = around.map { (di, dj) -> p(i + di, j + dj) }
                if (!visible(hex)) continue
                // Trois couleurs pour les hexagones : coordonnées dans le réseau des centres, de base (3, −1) et (1, 2).
                val s = (2 * i - j) / 7; val t = (i + 3 * j) / 7
                fill(hex, hexColors[Math.floorMod(s - t, 3)], gap = 4f, wash = 0.36f, lw = 1.2f)
            }
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
    FigureSpec("zellige", "Zellige", "Pavages", { _ ->
        "Sous ce zellige se cache le pavage 4.6.12 : des dodécagones, des hexagones et des carrés réguliers. Dans chaque tuile, " +
            "la méthode de Hankin trace depuis le milieu de chaque côté deux rayons inclinés de 72° ; ils dessinent des étoiles à 12, " +
            "6 et 4 branches, et des entrelacs qui traversent tout le pavage sans jamais s’interrompre, comme dans l’art islamique."
    }) { zellige(it) },
    FigureSpec("escher_fish", "Poissons à la Escher", "Pavages", { _ ->
        "Chaque poisson est un parallélogramme déformé : ce qu’on retire d’un côté revient au côté opposé, décalé par translation. " +
            "Les queues entrent dans les têtes, les nageoires dans les ventres, et ces poissons tous identiques pavent le plan sans trou. " +
            "M. C. Escher a dessiné ainsi des dizaines de pavages animaliers ; celui-ci est du type le plus simple, à translations seules."
    }) { escherFish(it) },
    FigureSpec("aztec_diamond", "Diamant aztèque", "Pavages", { v ->
        val n = aztecOrder(v)
        "Un pavage par $n × ${n + 1} = ${n * (n + 1)} dominos tiré au hasard, parmi 2${superscript(n * (n + 1) / 2)} possibles, " +
            "par l’algorithme de brassage des dominos. Une couleur par direction de domino. Les quatre coins sont gelés, le centre " +
            "désordonné ; quand la taille grandit, leur frontière tend vers le cercle inscrit (en pointillé) : c’est le théorème " +
            "du cercle arctique (Jockusch, Propp et Shor, 1995)."
    }) { aztec(it) },
    FigureSpec("stacked_cubes", "Cubes empilés", "Pavages", { v ->
        val (a, b, c) = cubesBox(v)
        "Des cubes empilés au hasard dans le coin d’une boîte $a × $b × $c, vus en perspective isométrique : l’hexagone est pavé de " +
            "losanges de trois orientations. Quel que soit l’empilement, il y a toujours ${a * b} losanges couchés, ${b * c} et ${a * c} " +
            "debout. Le nombre d’empilements possibles est donné par la formule de MacMahon (1915)."
    }) { stackedCubes(it) },
    FigureSpec("pinwheel", "Pavage pinwheel", "Pavages", { _ ->
        "Un seul triangle rectangle, de côtés 1, 2 et √5. Chaque triangle se découpe en cinq triangles semblables, √5 fois plus " +
            "petits ; on recommence, encore et encore. Les tuiles du pavage obtenu (Conway et Radin, 1994) prennent une infinité " +
            "d’orientations différentes, car l’angle arctan(1/2) n’est pas une fraction de tour. Les hachures suivent le grand côté."
    }) { pinwheel(it) },
    FigureSpec("chair_tiling", "Pavage de la chaise", "Pavages", { _ ->
        "Un L de trois carrés, la « chaise », se découpe en quatre chaises deux fois plus petites, qui se découpent à leur tour… " +
            "Cinq découpes donnent 4⁵ = 1024 chaises, dont on voit un morceau. Les traits épais entourent les chaises de l’étape " +
            "précédente, coloriées selon leur orientation ; les hachures suivent celle des petites. Ce pavage n’est pas périodique."
    }) { chairTiling(it) },
    FigureSpec("snub_hexagonal", "Pavage hexagonal adouci", "Pavages", { _ ->
        "Autour de chaque sommet : quatre triangles équilatéraux et un hexagone régulier, soit 4 × 60° + 120° = 360°, d’où la " +
            "notation 3.3.3.3.6. Il y a huit triangles par hexagone. C’est le seul pavage semi-régulier chiral : il existe en deux " +
            "versions, images l’une de l’autre dans un miroir, qu’on ne peut pas superposer sans retourner le plan."
    }) { snubHexagonal(it) },
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
