package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Figures du thème « Pavages » : chaque pavage remplit un cadre (rectangle ou disque), découpé au bord.

internal val tilingFigures: List<FigureSpec> = listOf(
    FigureSpec("penrose", "Pavage de Penrose", "Pavages", { _ -> "" }) { penrose(it) },
    FigureSpec("truchet", "Pavage de Truchet", "Pavages", { _ -> "" }) { truchet(it) },
    FigureSpec("honeycomb", "Nid d’abeilles", "Pavages", { _ -> "" }) { honeycomb(it) },
    FigureSpec("cairo", "Pavage du Caire", "Pavages", { _ -> "" }) { cairo(it) },
    FigureSpec("trihexagonal", "Pavage trihexagonal", "Pavages", { _ -> "" }) { trihexagonal(it) },
    FigureSpec("snub_square", "Pavage carré adouci", "Pavages", { _ -> "" }) { snubSquare(it) },
    FigureSpec("scales", "Pavage en écailles", "Pavages", { _ -> "" }) { scales(it) },
    FigureSpec("eight_stars", "Étoiles à huit branches", "Pavages", { _ -> "" }) { eightStars(it) },
    FigureSpec("rhombitrihexagonal", "Pavage 3.4.6.4", "Pavages", { _ -> "" }) { rhombitrihexagonal(it) },
    FigureSpec("octagons", "Octogones et carrés", "Pavages", { _ -> "" }) { octagons(it) },
    FigureSpec("pythagorean_tiling", "Pavage pythagoricien", "Pavages", { _ -> "" }) { pythagoreanTiling(it) },
    FigureSpec("voronoi", "Vitrail de Voronoï", "Pavages", { _ -> "" }) { voronoi(it) },
)

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

/** Arc de cercle de centre [c], de l'angle [a0] à [a1] (radians), en [n] pas. */
private fun arc(c: Offset, r: Float, a0: Float, a1: Float, n: Int = 12): List<Offset> =
    (0..n).map { c + dir(a0 + (a1 - a0) * it / n) * r }

private val PHI = ((1 + sqrt(5.0)) / 2).toFloat()

/** Garde du polygone convexe [poly] le côté de [s] par rapport à la médiatrice de [s] et [q]. */
private fun voronoiCut(poly: List<Offset>, s: Offset, q: Offset): List<Offset> {
    val m = (s + q) / 2f
    val n = q - s
    fun side(p: Offset) = (p.x - m.x) * n.x + (p.y - m.y) * n.y
    val out = ArrayList<Offset>(poly.size + 1)
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val sa = side(a)
        val sb = side(b)
        if (sa <= 0) out += a
        if ((sa < 0 && sb > 0) || (sa > 0 && sb < 0)) out += a + (b - a) * (sa / (sa - sb))
    }
    return out
}

/**
 * Vitrail de Voronoï : des germes tirés au hasard (assez écartés), et pour chacun la cellule des points
 * plus proches de lui que de tout autre germe, obtenue en coupant le cadre par les médiatrices.
 */
private fun Pencil.voronoi(v: Int) {
    val random = Random(31 + v * 17)
    val seeds = mutableListOf<Offset>()
    var tries = 0
    while (seeds.size < 22 && tries < 4000) {
        tries++
        val p = pt(14f + random.nextFloat() * 340f, 13f + random.nextFloat() * 224f)
        if (seeds.all { (it - p).getDistance() > 46f }) seeds += p
    }
    val pal = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose)
        .rot(v % 8)
    seeds.forEachIndexed { i, s ->
        var cell = FRAME
        for (q in seeds) if (q != s) cell = voronoiCut(cell, s, q)
        if (cell.size < 3) return@forEachIndexed
        fill(cell, pal[(i * 3 + random.nextInt(2)) % pal.size], wash = 0.42f, gap = 3.6f, angle = -58f + 37f * i, lw = 2.1f)
    }
    for (s in seeds) dot(s, 2f, Graphite, 0.85f)
    outline(FRAME, lw = 1.8f)
}

private fun Pencil.penrose(v: Int) {
    class Tri(val thin: Boolean, val a: Offset, val b: Offset, val c: Offset)
    val r = 132f
    val turn = rad(9f * (v % 4))
    var tris = (0 until 10).map { i ->
        var b = center + dir((2 * i - 1) * PI.toFloat() / 10 + turn) * r
        var c = center + dir((2 * i + 1) * PI.toFloat() / 10 + turn) * r
        if (i % 2 == 0) { val t = b; b = c; c = t }
        Tri(true, center, b, c)
    }
    repeat(4) {
        tris = tris.flatMap { t ->
            if (t.thin) {
                val p = t.a + (t.b - t.a) / PHI
                listOf(Tri(true, t.c, p, t.b), Tri(false, p, t.c, t.a))
            } else {
                val q = t.b + (t.a - t.b) / PHI
                val s = t.b + (t.c - t.b) / PHI
                listOf(Tri(false, s, t.c, t.a), Tri(false, q, s, t.b), Tri(true, s, q, t.a))
            }
        }
    }
    val pal = listOf(Crayons.rouge to Crayons.bleu, Crayons.orange to Crayons.violet, Crayons.rose to Crayons.turquoise, Crayons.jaune to Crayons.marine)[v % 4]
    val disc = circle(center.x, center.y, 118f, 140)
    framed(disc) {
        for (t in tris) {
            val p = listOf(t.a, t.b, t.c)
            if (p.all { (it - center).getDistance() > 124f }) continue
            fill(p, if (t.thin) pal.first else pal.second, outline = false, angle = if (t.thin) 20f else -60f, gap = if (t.thin) 3.2f else 4.2f, wash = if (t.thin) 0.42f else 0.3f)
        }
        for (t in tris) {
            if (listOf(t.a, t.b, t.c).all { (it - center).getDistance() > 124f }) continue
            line(listOf(t.c, t.a, t.b), Graphite, 0.9f)
        }
    }
}

private fun Pencil.truchet(v: Int) {
    val t = 29f
    val layout = Random(v * 31 + 5)
    val (c1, c2) = listOf(Crayons.bleu to Crayons.jaune, Crayons.rouge to Crayons.turquoise, Crayons.violet to Crayons.orange, Crayons.sapin to Crayons.rose)[v % 4]
    val frame = listOf(pt(10f, 9f), pt(358f, 9f), pt(358f, 241f), pt(10f, 241f))
    framed(frame) {
        for (i in 0 until 12) for (j in 0 until 8) {
            val o = layout.nextInt(2)
            val x = 10f + i * t; val y = 9f + j * t
            val corners = listOf(pt(x, y), pt(x + t, y), pt(x + t, y + t), pt(x, y + t))
            val discColor = if ((i + j + o) % 2 == 0) c1 else c2
            val bandColor = if (discColor == c1) c2 else c1
            // Coins des quarts de disque : haut-gauche et bas-droite (o = 0), ou haut-droite et bas-gauche (o = 1).
            val picks = if (o == 0) listOf(0, 2) else listOf(1, 3)
            fun quarterArc(k: Int) = arc(corners[k], t / 2, rad(90f * k), rad(90f * k + 90f), 10)
            // La bande : le carreau moins ses deux quarts de disque, parcourue dans le sens des coins.
            val band = (0 until 4).flatMap { k -> if (k in picks) quarterArc(k).reversed() else listOf(corners[k]) }
            fill(band, bandColor, outline = false, gap = 3.8f, wash = 0.34f, angle = 30f)
            for (k in picks) {
                fill(listOf(corners[k]) + quarterArc(k), discColor, outline = false, gap = 3.8f, wash = 0.34f, angle = 30f)
                line(quarterArc(k), Graphite, 1.1f)
            }
        }
    }
}

private fun Pencil.honeycomb(v: Int) {
    val r = 19f
    val pal = Crayons.rainbow.rot(v % 8).let { listOf(it[2], it[0], it[5]) }
    val w = sqrt(3f) * r
    framed {
        for (q in -8..14) for (s in -2..10) {
            val c = pt(10f + w * (q + s / 2f), 9f + 1.5f * r * s)
            val hex = regular(c, r, 6, 30f)
            if (!visible(hex)) continue
            val k = Math.floorMod(q - s, 3)
            fill(hex, pal[k], angle = -58f + 40f * k, gap = 3.6f + k * 0.5f, lw = 1.1f)
        }
    }
}

private fun Pencil.cairo(v: Int) {
    val s = 40f
    val half = 0.4226f * s / 2
    val pal = Crayons.rainbow.rot((v * 2) % 8).let { listOf(it[0], it[2], it[4], it[6]) }
    val o = pt(-6f, -12f)
    fun horizontal(i: Int, j: Int) = Math.floorMod(i + j, 2) == 0
    fun ends(i: Int, j: Int): List<Offset> {
        val m = o + Offset((i + 0.5f) * s, (j + 0.5f) * s)
        return if (horizontal(i, j)) listOf(m - Offset(half, 0f), m + Offset(half, 0f)) else listOf(m - Offset(0f, half), m + Offset(0f, half))
    }
    // Points de la région d'une case qui touche l'arête PQ : l'extrémité de barre la plus proche de P, puis de Q.
    fun inner(i: Int, j: Int, p: Offset, q: Offset): List<Offset> {
        val e = ends(i, j)
        val np = e.minBy { (it - p).getDistance() }
        val nq = e.minBy { (it - q).getDistance() }
        return if (np == nq) listOf(np) else listOf(np, nq)
    }
    framed {
        for (i in 0..10) for (j in 0..8) {
            val p = o + Offset(i * s, j * s)
            // Arête verticale de (i, j) à (i, j + 1), entre les cases (i − 1, j) et (i, j).
            run {
                val q = o + Offset(i * s, (j + 1) * s)
                val pent = listOf(p) + inner(i, j, p, q) + listOf(q) + inner(i - 1, j, p, q).reversed()
                if (visible(pent)) {
                    val k = if (horizontal(i, j)) 0 else 1
                    fill(pent, pal[k], angle = -58f + 70f * k, gap = 3.8f, lw = 1.1f)
                }
            }
            // Arête horizontale de (i, j) à (i + 1, j), entre les cases (i, j − 1) et (i, j).
            run {
                val q = o + Offset((i + 1) * s, j * s)
                val pent = listOf(p) + inner(i, j - 1, p, q) + listOf(q) + inner(i, j, p, q).reversed()
                if (visible(pent)) {
                    val k = if (horizontal(i, j)) 2 else 3
                    fill(pent, pal[k], angle = 30f + 50f * k, gap = 3.8f, lw = 1.1f)
                }
            }
        }
    }
}

private fun Pencil.trihexagonal(v: Int) {
    val a = 21f
    val h = a * sqrt(3f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    framed {
        for (i in -6..10) for (j in -1..8) {
            val c = pt(10f + 2 * a * i + a * j, 9f + h * j)
            val hex = regular(c, a, 6, 0f)
            val up = listOf(c + Offset(a, 0f), c + Offset(a / 2, h / 2), c + Offset(3 * a / 2, h / 2))
            val down = listOf(c + Offset(a, 0f), c + Offset(a / 2, -h / 2), c + Offset(3 * a / 2, -h / 2))
            if (visible(hex)) fill(hex, pal[Math.floorMod(i - j, 3)], gap = 4f, lw = 1.1f)
            if (visible(up)) fill(up, pal[4], angle = 30f, gap = 2.8f, wash = 0.45f, lw = 1.1f)
            if (visible(down)) fill(down, pal[5], angle = -30f, gap = 2.8f, wash = 0.45f, lw = 1.1f)
        }
    }
}

/** Pavage carré adouci : par maille (côté √(2 + √3)), deux carrés tournés de ±15° et quatre triangles. */
private val SNUB_TRIANGLES = listOf(
    listOf(pt(-0.3536f, 1.3195f), pt(0.3536f, 0.6124f), pt(0.6124f, 1.5783f)),
    listOf(pt(0.6124f, -0.3536f), pt(0.3536f, 0.6124f), pt(1.3195f, 0.3536f)),
    listOf(pt(1.5783f, 1.3195f), pt(0.6124f, 1.5783f), pt(1.3195f, 2.2854f)),
    listOf(pt(1.5783f, 1.3195f), pt(1.3195f, 0.3536f), pt(2.2854f, 0.6124f)),
)

private fun Pencil.snubSquare(v: Int) {
    val e = 30f
    val cell = sqrt(2f + sqrt(3f)) * e
    val pal = Crayons.rainbow.rot((v * 5) % 8)
    val o = pt(10f, 9f)
    framed {
        for (i in -1..7) for (j in -1..5) {
            val base = o + Offset(i * cell, j * cell)
            val a = regular(base, e / sqrt(2f), 4, 60f)
            val b = regular(base + Offset(cell / 2, cell / 2), e / sqrt(2f), 4, 30f)
            if (visible(a)) fill(a, pal[0], gap = 4f, lw = 1.1f)
            if (visible(b)) fill(b, pal[2], angle = 30f, gap = 4f, lw = 1.1f)
            SNUB_TRIANGLES.forEachIndexed { k, t ->
                val tri = t.map { base + it * e }
                if (visible(tri)) fill(tri, if (k % 2 == 0) pal[4] else pal[5], angle = -20f + 40f * k, gap = 2.9f, wash = 0.42f, lw = 1.1f)
            }
        }
    }
}

private fun Pencil.scales(v: Int) {
    val r = 22f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    framed {
        for (row in -1..12) for (i in -1..9) {
            val c = pt(10f + 2 * r * i + if (Math.floorMod(row, 2) == 1) r else 0f, 9f + r * row - r / 2)
            // Une écaille : le demi-cercle du haut, puis deux quarts de cercle qui descendent en pointe.
            val scale = arc(c, r, 0f, -PI.toFloat(), 16) +
                arc(c + Offset(-r, r), r, -PI.toFloat() / 2, 0f, 8).drop(1) +
                arc(c + Offset(r, r), r, PI.toFloat(), 3 * PI.toFloat() / 2, 8).drop(1).dropLast(1)
            if (!visible(scale)) continue
            fill(scale, pal[Math.floorMod(row, 8)], angle = -58f + row * 7f, gap = 3.6f, lw = 1f)
        }
    }
}

private fun Pencil.eightStars(v: Int) {
    val p = 66f
    val rr = p / 2
    val inner = rr * cos(rad(45f)) / cos(rad(22.5f))
    val pal = listOf(Crayons.bleu to Crayons.jaune, Crayons.rouge to Crayons.turquoise, Crayons.sapin to Crayons.orange, Crayons.violet to Crayons.jaune)[v % 4]
    val o = pt(184f - 3 * p, 125f - 2 * p)
    fun star(c: Offset) = List(16) { k -> c + dir(rad(22.5f * k)) * (if (k % 2 == 0) rr else inner) }
    framed {
        for (i in -1..7) for (j in -1..5) {
            val c = o + Offset(i * p, j * p)
            // La croix : la place laissée entre quatre étoiles, autour du centre de la maille.
            val m = c + Offset(p / 2, p / 2)
            val pts = mutableListOf<Offset>()
            for ((corner, toward) in listOf(c to 45f, c + Offset(p, 0f) to 135f, c + Offset(p, p) to 225f, c + Offset(0f, p) to 315f)) {
                for (k in -2..2) pts += corner + dir(rad(toward + 22.5f * k)) * (if (k % 2 == 0) rr else inner)
            }
            val cross = pts.distinctBy { (it.x * 10).toInt() to (it.y * 10).toInt() }.sortedBy { atan2(it.y - m.y, it.x - m.x) }
            if (visible(cross)) fill(cross, pal.second, angle = 30f, gap = 4.4f, wash = 0.25f, lw = 1.1f)
            val s = star(c)
            if (visible(s)) fill(s, if ((i + j) % 2 == 0) pal.first else Crayons.rainbow[(v + 5) % 8], gap = 3.4f, wash = 0.38f, lw = 1.2f)
        }
    }
}

private fun Pencil.rhombitrihexagonal(v: Int) {
    val a = 17f
    val d = a * (1 + sqrt(3f))
    val u1 = dir(rad(30f)) * d
    val u2 = dir(rad(90f)) * d
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    framed {
        for (i in -4..10) for (j in -7..8) {
            val c = pt(10f, 9f) + u1 * i.toFloat() + u2 * j.toFloat()
            if (c.x < -60 || c.x > 430 || c.y < -60 || c.y > 310) continue
            val hex = regular(c, a, 6, 0f)
            for (k in 0..2) {
                val n = dir(rad(30f + 60f * k)) * a
                val v1 = c + dir(rad(60f * k)) * a
                val v2 = c + dir(rad(60f * k + 60f)) * a
                val sq = listOf(v1, v2, v2 + n, v1 + n)
                if (visible(sq)) fill(sq, pal[2], angle = 30f + 60f * k, gap = 3.6f, lw = 1f)
            }
            for (k in 0..1) {
                val w = c + dir(rad(60f * k)) * a
                val tri = listOf(w, w + dir(rad(60f * k - 30f)) * a, w + dir(rad(60f * k + 30f)) * a)
                if (visible(tri)) fill(tri, pal[4 + k], gap = 2.6f, wash = 0.45f, lw = 1f)
            }
            if (visible(hex)) fill(hex, pal[0], gap = 4.2f, lw = 1.1f)
        }
    }
}

private fun Pencil.octagons(v: Int) {
    val a = 24f
    val d = a * (1 + sqrt(2f))
    val big = a / (2 * sin(rad(22.5f)))
    val pal = listOf(Crayons.turquoise to Crayons.rouge, Crayons.jaune to Crayons.marine, Crayons.rose to Crayons.vert, Crayons.orange to Crayons.violet)[v % 4]
    val o = pt(184f - 3 * d, 125f - 2 * d)
    framed {
        for (i in -1..7) for (j in -1..5) {
            val c = o + Offset(i * d, j * d)
            val oct = regular(c, big, 8, 22.5f)
            if (visible(oct)) fill(oct, pal.first, angle = if ((i + j) % 2 == 0) -58f else 32f, gap = 4f, lw = 1.1f)
            val sq = regular(c + Offset(d / 2, d / 2), a / sqrt(2f), 4, 0f)
            if (visible(sq)) fill(sq, pal.second, gap = 2.8f, wash = 0.45f, lw = 1.1f)
        }
    }
}

private fun Pencil.pythagoreanTiling(v: Int) {
    val a = 62f
    val b = floatArrayOf(34f, 26f, 42f, 20f)[v % 4]
    val pal = Crayons.rainbow.rot((v * 3 + 2) % 8)
    val o = pt(70f, 40f)
    val u = Offset(a, b)
    val w = Offset(-b, a)
    framed {
        for (i in -6..6) for (j in -6..6) {
            val p = o + u * i.toFloat() + w * j.toFloat()
            val big = listOf(p, p + Offset(a, 0f), p + Offset(a, a), p + Offset(0f, a))
            val small = listOf(p + Offset(a, 0f), p + Offset(a + b, 0f), p + Offset(a + b, b), p + Offset(a, b))
            if (visible(big)) fill(big, if (Math.floorMod(i + j, 2) == 0) pal[0] else pal[1], gap = 4.4f, lw = 1.2f)
            if (visible(small)) fill(small, pal[4], angle = 30f, gap = 2.8f, wash = 0.45f, lw = 1.1f)
        }
        // Le quadrillage incliné de côté c = √(a² + b²) : chaque carré a l'aire d'un grand plus un petit.
        val q = o + Offset(a * 0.3f, a * 0.62f)
        for (k in -8..8) {
            val s1 = q + w * k.toFloat()
            stroke(s1 - u * 8f, s1 + u * 8f, Crayons.rouge, 1.5f, 0.85f)
            val s2 = q + u * k.toFloat()
            stroke(s2 - w * 8f, s2 + w * 8f, Crayons.rouge, 1.5f, 0.85f)
        }
    }
}
