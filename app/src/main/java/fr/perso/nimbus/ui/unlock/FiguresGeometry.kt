package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// Figures du thème « Géométrie » : théorèmes du triangle, polygones, découpages.

internal val geometryFigures: List<FigureSpec> = listOf(
    FigureSpec("euclid", "Moulin d’Euclide", "Géométrie", { _ ->
        "Un triangle rectangle et un carré sur chacun de ses côtés. La hauteur partage le grand carré en deux rectangles, chacun de " +
            "même aire que le petit carré de sa couleur : Euclide le montre avec les paires de triangles égaux tracées en rouge et " +
            "en bleu. D’où le théorème de Pythagore, a² + b² = c²."
    }) { euclid(it) },
    FigureSpec("nested_polygons", "Polygones emboîtés", "Géométrie", { v ->
        val last = when (intArrayOf(10, 8, 12, 9)[v % 4]) {
            8 -> "l’octogone"
            9 -> "l’ennéagone"
            10 -> "le décagone"
            else -> "le dodécagone"
        }
        "Les polygones réguliers du triangle jusqu’à $last, tous posés sur le même côté. Un polygone à n côtés a des angles de " +
            "(n − 2) × 180° / n : 60° pour le triangle, 90° pour le carré, 108° pour le pentagone… Plus n grandit, plus les angles " +
            "approchent 180° et plus le polygone ressemble à un cercle."
    }) { nestedPolygons(it) },
    FigureSpec("star_polygons", "Polygones étoilés", "Géométrie", { _ ->
        "Le polygone étoilé {n/k} relie n points régulièrement répartis sur un cercle, en sautant de k en k. Si n et k n’ont pas de " +
            "diviseur commun, l’étoile se trace d’un seul trait, comme le pentagramme {5/2}. Sinon elle se sépare en plusieurs " +
            "polygones : {6/2} donne deux triangles, l’étoile de David."
    }) { starPolygons(it) },
    FigureSpec("nine_point", "Cercle d’Euler", "Géométrie", { _ ->
        "Dans tout triangle, neuf points sont sur un même cercle : les milieux des côtés (en bleu), les pieds des hauteurs (en rouge) " +
            "et les milieux des segments qui joignent les sommets à l’orthocentre H (en vert). Son centre est au milieu de O, centre " +
            "du cercle circonscrit, et de H, sur la droite d’Euler ; son rayon vaut la moitié de celui du cercle circonscrit."
    }) { ninePoint(it) },
    FigureSpec("reuleaux", "Triangle de Reuleaux", "Géométrie", { _ ->
        "Chaque côté d’un polygone régulier à 3, 5 ou 7 sommets est remplacé par un arc centré sur le sommet opposé. Ces formes ont la " +
            "même largeur dans toutes les directions : elles roulent entre deux droites parallèles comme une roue. Leur périmètre " +
            "vaut π fois cette largeur, comme celui d’un cercle (théorème de Barbier)."
    }) { reuleaux(it) },
    FigureSpec("tangram", "Tangram", "Géométrie", { _ ->
        "Un carré découpé en sept pièces : deux grands triangles, un moyen, deux petits, un carré et un parallélogramme. Le petit " +
            "triangle vaut 1/16 du grand carré ; le carré, le parallélogramme et le triangle moyen en valent chacun 1/8 ; les grands " +
            "triangles 1/4. Tous les angles valent 45°, 90° ou 135°."
    }) { tangram(it) },
    FigureSpec("tetrominoes", "Tétrominos", "Géométrie", { _ ->
        "Il y a cinq tétrominos, des formes de quatre carrés collés par un côté : I, O, T, S et L. Un exemplaire de chacun ne pave " +
            "aucun rectangle : sur un damier, le T couvre trois cases d’une couleur et une de l’autre, les autres deux et deux. " +
            "Avec deux T, les écarts se compensent : deux exemplaires de chacun remplissent ce rectangle 5 × 8."
    }) { tetrominoes(it) },
    FigureSpec("pentagram", "Pentagramme d’or", "Géométrie", { _ ->
        "Les diagonales d’un pentagone régulier dessinent une étoile, qui enferme un pentagone plus petit, et ainsi de suite. Le rapport " +
            "d’une diagonale au côté est le nombre d’or φ = (1 + √5) / 2 ≈ 1,618, et chaque pentagone est φ² ≈ 2,618 fois plus petit " +
            "que le précédent. Les pointes de l’étoile sont des triangles d’or, d’angles 36°, 72° et 72°."
    }) { pentagram(it) },
    FigureSpec("morley", "Triangle de Morley", "Géométrie", { _ ->
        "On partage chaque angle d’un triangle en trois angles égaux. Les deux trisectrices voisines de chaque côté se coupent en un " +
            "point, et ces trois points forment toujours un triangle équilatéral, quel que soit le triangle de départ. Frank Morley " +
            "l’a découvert en 1899, plus de deux mille ans après Euclide."
    }) { morley(it) },
    FigureSpec("napoleon", "Théorème de Napoléon", "Géométrie", { _ ->
        "Sur chaque côté d’un triangle quelconque, on construit un triangle équilatéral vers l’extérieur. Leurs trois centres forment " +
            "toujours un triangle équilatéral (en rouge). Les droites qui joignent chaque sommet au sommet du triangle équilatéral " +
            "opposé se coupent en un même point, le point de Fermat."
    }) { napoleon(it) },
    FigureSpec("lunes", "Lunules d’Hippocrate", "Géométrie", { v ->
        val shape = if (v % 2 == 0) "isocèle" else "de côtés 3, 4 et 5"
        "Un triangle rectangle $shape, inscrit dans un demi-cercle. Sur chacun de ses petits côtés, un demi-cercle déborde du grand " +
            "et forme une lunule. Par Pythagore, les deux petits demi-cercles ont ensemble l’aire du grand ; en retirant les parties " +
            "communes, les deux lunules ont ensemble exactement l’aire du triangle."
    }) { lunes(it) },
)

private fun Offset.unit() = this / getDistance()
private fun Offset.perp() = Offset(-y, x)
private fun cross(a: Offset, b: Offset) = a.x * b.y - a.y * b.x
private fun dot(a: Offset, b: Offset) = a.x * b.x + a.y * b.y
private fun rotate(u: Offset, a: Float) = Offset(u.x * cos(a) - u.y * sin(a), u.x * sin(a) + u.y * cos(a))

/** Intersection des droites (p, p + d) et (q, q + e). */
private fun meet(p: Offset, d: Offset, q: Offset, e: Offset): Offset {
    val t = cross(q - p, e) / cross(d, e)
    return p + d * t
}

/** Polygone régulier : [n] sommets sur un cercle de rayon [r], le premier à l'angle [start] (degrés). */
private fun ngon(c: Offset, r: Float, n: Int, start: Float): List<Offset> = List(n) { c + dir(rad(start + 360f * it / n)) * r }

/** Arc de centre [c] de [a] à [b] (deux points du cercle), dans le sens le plus court ou, si [far], par le côté loin de [away]. */
private fun arcBetween(c: Offset, a: Offset, b: Offset, n: Int = 24, away: Offset? = null): List<Offset> {
    val r = (a - c).getDistance()
    val a0 = atan2(a.y - c.y, a.x - c.x)
    var da = atan2(b.y - c.y, b.x - c.x) - a0
    while (da > PI) da -= 2 * PI.toFloat()
    while (da < -PI) da += 2 * PI.toFloat()
    if (away != null) {
        // Deux demi-tours possibles : celui dont le milieu est le plus loin de [away].
        val alt = if (da > 0) da - 2 * PI.toFloat() else da + 2 * PI.toFloat()
        val m1 = c + dir(a0 + da / 2) * r
        val m2 = c + dir(a0 + alt / 2) * r
        if ((m2 - away).getDistance() > (m1 - away).getDistance()) da = alt
    }
    return (0..n).map { c + dir(a0 + da * it / n) * r }
}

private fun Pencil.euclid(v: Int) {
    val (a, b) = listOf(3f to 4f, 1f to 1.6f, 5f to 6f, 2f to 3f)[v % 4]
    val c = sqrt(a * a + b * b)
    val pa = pt(0f, 0f); val pb = pt(c, 0f)
    val pc = pt(b * b / c, -a * b / c)
    val h = pt(b * b / c, 0f)
    fun square(p: Offset, q: Offset, other: Offset): List<Offset> {
        var n = (q - p).perp()
        if (dot(n, other - p) > 0) n = -n
        return listOf(p, q, q + n, p + n)
    }
    val sqHyp = listOf(pa, pb, pb + Offset(0f, c), pa + Offset(0f, c))
    val sqB = square(pa, pc, pb)
    val sqA = square(pc, pb, pa)
    val fit = Fit(listOf(sqHyp, sqA, sqB), 8f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val left = listOf(pa, h, h + Offset(0f, c), pa + Offset(0f, c))
    val right = listOf(h, pb, pb + Offset(0f, c), h + Offset(0f, c))
    fill(fit(left), pal[5], gap = 4.4f, wash = 0.22f, strokeAlpha = 0.55f)
    fill(fit(right), pal[3], gap = 4.4f, wash = 0.22f, strokeAlpha = 0.55f, angle = 30f)
    fill(fit(sqB), pal[5], gap = 3.4f, wash = 0.42f)
    fill(fit(sqA), pal[3], gap = 3.4f, wash = 0.42f, angle = 30f)
    fill(fit(listOf(pa, pb, pc)), pal[1], gap = 3f, wash = 0.45f, angle = 70f)
    // La hauteur, prolongée à travers le grand carré, et les deux paires de triangles égaux du « moulin ».
    line(listOf(fit(pc), fit(h + Offset(0f, c))), Graphite, 1.2f)
    stroke(fit(pb), fit(sqB[3]), Crayons.rouge, 1.2f, 0.8f)
    stroke(fit(pc), fit(sqHyp[3]), Crayons.rouge, 1.2f, 0.8f)
    stroke(fit(pa), fit(sqA[2]), Crayons.marine, 1.2f, 0.8f)
    stroke(fit(pc), fit(sqHyp[2]), Crayons.marine, 1.2f, 0.8f)
}

private fun Pencil.nestedPolygons(v: Int) {
    val maxN = intArrayOf(10, 8, 12, 9)[v % 4]
    val polys = (3..maxN).map { n ->
        val apothem = 1f / (2 * tan(PI.toFloat() / n))
        val r = 1f / (2 * sin(PI.toFloat() / n))
        ngon(pt(0f, -apothem), r, n, 90f - 180f / n)
    }
    val fit = Fit(polys, 8f)
    val pal = Crayons.rainbow.rot(v % 8)
    for (i in polys.indices.reversed()) {
        fill(fit(polys[i]), pal[i % 8], gap = 3.4f + i * 0.15f, angle = -58f + 23f * i, wash = 0.3f, lw = 1.1f)
    }
}

private fun Pencil.starPolygons(v: Int) {
    val sets = listOf(
        listOf(5 to 2, 6 to 2, 7 to 2, 7 to 3, 8 to 3, 9 to 4),
        listOf(5 to 2, 8 to 2, 7 to 3, 9 to 2, 10 to 3, 12 to 5),
    )[v % 2]
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    sets.forEachIndexed { idx, (n, k) ->
        val c = pt(66f + 118f * (idx % 3), 66f + 120f * (idx / 3))
        val r = 54f
        val pts = ngon(c, r, n, -90f)
        for (p in pts) dot(p, 1.6f, Graphite, 0.6f)
        val g = Classic.gcd(n, k)
        for (s in 0 until g) {
            val poly = (0 until n / g).map { pts[(s + it * k) % n] }
            fill(poly, pal[(idx + s) % 8], gap = 3.2f, wash = 0.35f, lw = 1.1f, angle = -58f + 40f * s)
        }
    }
}

private fun Pencil.ninePoint(v: Int) {
    val shapes = listOf(
        listOf(pt(-0.2f, -1f), pt(-1f, 0.55f), pt(1.15f, 0.55f)),
        listOf(pt(0.35f, -1f), pt(-1.05f, 0.5f), pt(1f, 0.6f)),
    )
    val (a, b, c) = shapes[v % 2]
    // Centre du cercle circonscrit O, orthocentre H = A + B + C − 2O.
    val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
    val sa = a.x * a.x + a.y * a.y; val sb = b.x * b.x + b.y * b.y; val sc = c.x * c.x + c.y * c.y
    val o = pt((sa * (b.y - c.y) + sb * (c.y - a.y) + sc * (a.y - b.y)) / d, (sa * (c.x - b.x) + sb * (a.x - c.x) + sc * (b.x - a.x)) / d)
    val h = a + b + c - o * 2f
    val big = (a - o).getDistance()
    val n = (o + h) / 2f
    val circum = circle(o.x, o.y, big, 120)
    val fit = Fit(listOf(circum), 8f)
    val pal = Crayons.rainbow.rot((v * 2) % 8)
    fill(fit(circum), pal[2], wash = 0.1f, hatch = false, lw = 1f)
    fill(fit(listOf(a, b, c)), pal[1], gap = 4.4f, wash = 0.25f, strokeAlpha = 0.55f, lw = 1.3f)
    fill(fit(circle(n.x, n.y, big / 2, 90)), pal[5], gap = 3.6f, wash = 0.3f, strokeAlpha = 0.6f, lw = 1.2f)
    fun foot(p: Offset, q: Offset, r: Offset): Offset {
        val u = (r - q).unit()
        return q + u * dot(p - q, u)
    }
    val feet = listOf(foot(a, b, c), foot(b, c, a), foot(c, a, b))
    listOf(a, b, c).zip(feet).forEach { (p, f) -> stroke(fit(p), fit(f), Graphite, 0.9f, 0.6f) }
    // La droite d'Euler, par O et H.
    val eu = (h - o).unit()
    stroke(fit(o - eu * 0.25f), fit(h + eu * 0.25f), Crayons.rouge, 1.4f, 0.85f)
    val mids = listOf((a + b) / 2f, (b + c) / 2f, (c + a) / 2f)
    val upper = listOf((a + h) / 2f, (b + h) / 2f, (c + h) / 2f)
    fun mark(p: Offset, color: androidx.compose.ui.graphics.Color) {
        val q = fit(p)
        fill(circle(q.x, q.y, 4f, 14), color, hatch = false, wash = 0.9f, lw = 0.9f)
    }
    mids.forEach { mark(it, Crayons.bleu) }
    feet.forEach { mark(it, Crayons.rouge) }
    upper.forEach { mark(it, Crayons.vert) }
    mark(o, Graphite)
    mark(h, Graphite)
    mark(n, Crayons.jaune)
}

private fun Pencil.reuleaux(v: Int) {
    val w = 98f
    val top = 125f - w / 2; val bottom = 125f + w / 2
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    stroke(pt(10f, top), pt(358f, top), Graphite, 1.3f, 0.9f)
    stroke(pt(10f, bottom), pt(358f, bottom), Graphite, 1.3f, 0.9f)
    listOf(3, 5, 7).forEachIndexed { idx, n ->
        // Polygone régulier dont la plus grande diagonale vaut w ; chaque côté devient un arc centré sur le sommet opposé.
        val diag = 2 * sin(PI.toFloat() * ((n - 1) / 2) / n)
        val r = w / diag
        val turn = 37f * (v + 1) * (idx + 1)
        val verts = ngon(Offset.Zero, r, n, turn)
        val shape = (0 until n).flatMap { j ->
            val c = verts[(j + (n + 1) / 2) % n]
            arcBetween(c, verts[j], verts[(j + 1) % n], 16).dropLast(1)
        }
        val cx = 66f + 118f * idx
        val y0 = shape.minOf { it.y }; val y1 = shape.maxOf { it.y }
        val x0 = shape.minOf { it.x }; val x1 = shape.maxOf { it.x }
        val shift = Offset(cx - (x0 + x1) / 2, 125f - (y0 + y1) / 2)
        val placed = shape.map { it + shift }
        fill(placed, pal[idx * 2], gap = 3.6f, angle = -58f + 30f * idx)
        for (p in verts) dot(p + shift, 2f, Graphite, 0.8f)
    }
}

private fun Pencil.tangram(v: Int) {
    val pieces = listOf(
        listOf(pt(0f, 0f), pt(4f, 0f), pt(2f, 2f)),
        listOf(pt(0f, 0f), pt(2f, 2f), pt(0f, 4f)),
        listOf(pt(4f, 2f), pt(4f, 4f), pt(2f, 4f)),
        listOf(pt(2f, 2f), pt(3f, 1f), pt(3f, 3f)),
        listOf(pt(2f, 2f), pt(3f, 3f), pt(2f, 4f), pt(1f, 3f)),
        listOf(pt(1f, 3f), pt(2f, 4f), pt(0f, 4f)),
        listOf(pt(4f, 0f), pt(4f, 2f), pt(3f, 3f), pt(3f, 1f)),
    )
    val spread = floatArrayOf(0.28f, 0.16f, 0.4f, 0.22f)[v % 4]
    val mid = pt(2f, 2f)
    val moved = pieces.map { p ->
        val g = p.reduce { s, q -> s + q } / p.size.toFloat()
        val push = (g - mid) * spread
        p.map { it + push }
    }
    val fit = Fit(moved, 10f)
    val pal = Crayons.rainbow.rot(v % 8)
    moved.forEachIndexed { i, p -> fill(fit(p), pal[i], angle = -58f + 26f * i, gap = 3.6f, lw = 1.3f) }
}

/** Trois rectangles 5 × 8 pavés par deux exemplaires de chacun des cinq tétrominos. */
private val TETRO_SOLUTIONS = listOf(
    listOf("I" to listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), "I" to listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3), "T" to listOf(2 to 0, 3 to 0, 3 to 1, 4 to 0), "O" to listOf(5 to 0, 5 to 1, 6 to 0, 6 to 1), "L" to listOf(6 to 2, 7 to 0, 7 to 1, 7 to 2), "S" to listOf(2 to 1, 2 to 2, 3 to 2, 3 to 3), "S" to listOf(4 to 1, 4 to 2, 5 to 2, 5 to 3), "L" to listOf(0 to 4, 1 to 4, 2 to 3, 2 to 4), "T" to listOf(3 to 4, 4 to 3, 4 to 4, 5 to 4), "O" to listOf(6 to 3, 6 to 4, 7 to 3, 7 to 4)),
    listOf("I" to listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), "I" to listOf(1 to 0, 2 to 0, 3 to 0, 4 to 0), "T" to listOf(5 to 0, 6 to 0, 6 to 1, 7 to 0), "O" to listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2), "O" to listOf(3 to 1, 3 to 2, 4 to 1, 4 to 2), "L" to listOf(4 to 3, 5 to 1, 5 to 2, 5 to 3), "S" to listOf(6 to 2, 6 to 3, 7 to 1, 7 to 2), "S" to listOf(0 to 4, 1 to 3, 1 to 4, 2 to 3), "T" to listOf(2 to 4, 3 to 3, 3 to 4, 4 to 4), "L" to listOf(5 to 4, 6 to 4, 7 to 3, 7 to 4)),
    listOf("I" to listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), "I" to listOf(1 to 0, 2 to 0, 3 to 0, 4 to 0), "S" to listOf(4 to 1, 4 to 2, 5 to 0, 5 to 1), "O" to listOf(6 to 0, 6 to 1, 7 to 0, 7 to 1), "S" to listOf(1 to 1, 1 to 2, 2 to 2, 2 to 3), "L" to listOf(2 to 1, 3 to 1, 3 to 2, 3 to 3), "O" to listOf(5 to 2, 5 to 3, 6 to 2, 6 to 3), "L" to listOf(6 to 4, 7 to 2, 7 to 3, 7 to 4), "T" to listOf(0 to 4, 1 to 3, 1 to 4, 2 to 4), "T" to listOf(3 to 4, 4 to 3, 4 to 4, 5 to 4)),
)

/** Contour d'un polyomino (sans point de contact intérieur) : les côtés de cases sans voisine, enchaînés. */
private fun outlineOf(cells: List<Pair<Int, Int>>): List<Offset> {
    val set = cells.toSet()
    val next = HashMap<Pair<Int, Int>, Pair<Int, Int>>()
    for ((x, y) in cells) {
        if ((x to y - 1) !in set) next[x to y] = x + 1 to y
        if ((x + 1 to y) !in set) next[x + 1 to y] = x + 1 to y + 1
        if ((x to y + 1) !in set) next[x + 1 to y + 1] = x to y + 1
        if ((x - 1 to y) !in set) next[x to y + 1] = x to y
    }
    val start = next.keys.first()
    val out = mutableListOf(start)
    var cur = next.getValue(start)
    while (cur != start) {
        out += cur
        cur = next.getValue(cur)
    }
    // Retire les points alignés.
    return out.filterIndexed { i, p ->
        val a = out[(i + out.size - 1) % out.size]; val b = out[(i + 1) % out.size]
        (p.first - a.first) * (b.second - p.second) - (p.second - a.second) * (b.first - p.first) != 0
    }.map { pt(it.first.toFloat(), it.second.toFloat()) }
}

private fun Pencil.tetrominoes(v: Int) {
    val sol = TETRO_SOLUTIONS[v % 3]
    val cell = 39f
    val o = pt(184f - 4 * cell, 125f - 2.5f * cell)
    val colors = mapOf("I" to Crayons.bleu, "O" to Crayons.jaune, "T" to Crayons.violet, "S" to Crayons.vert, "L" to Crayons.orange)
    sol.forEachIndexed { i, (name, cells) ->
        val poly = outlineOf(cells).map { o + it * cell }
        fill(poly, colors.getValue(name), angle = if (i % 2 == 0) -58f else 32f, gap = 3.8f, lw = 1.4f)
    }
    for (x in 0..8) stroke(o + Offset(x * cell, 0f), o + Offset(x * cell, 5 * cell), Graphite, 0.5f, 0.25f)
    for (y in 0..5) stroke(o + Offset(0f, y * cell), o + Offset(8 * cell, y * cell), Graphite, 0.5f, 0.25f)
}

private fun Pencil.pentagram(v: Int) {
    val phi2 = ((3 + sqrt(5.0)) / 2).toFloat()
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    var r = 120f
    var a0 = -90f
    val c = pt(184f, 136f)
    fill(ngon(c, r, 5, a0), pal[4], gap = 4.6f, wash = 0.2f, strokeAlpha = 0.5f, lw = 1.4f)
    for (level in 0 until 3) {
        val outer = ngon(c, r, 5, a0)
        val inner = ngon(c, r / phi2, 5, a0 + 36f)
        for (i in 0 until 5) {
            // Pointe de l'étoile, et triangle obtus entre deux pointes.
            fill(listOf(outer[i], inner[i], inner[(i + 4) % 5]), pal[level * 2], angle = a0 + 72f * i, gap = 3.2f, wash = 0.4f, lw = 1.1f)
            if (level > 0) fill(listOf(outer[i], outer[(i + 1) % 5], inner[i]), pal[level * 2 + 1], angle = 30f + 72f * i, gap = 3.6f, wash = 0.3f, lw = 1f)
        }
        r /= phi2
        a0 += 36f
    }
    fill(ngon(c, r, 5, a0), Crayons.jaune, gap = 2.6f, lw = 1f)
}

/** Trisectrice issue de [x] : la direction de [x] vers [y], tournée d'un tiers de l'angle vers [z]. */
private fun trisector(x: Offset, y: Offset, z: Offset): Offset {
    val u = (y - x).unit(); val w = (z - x).unit()
    val angle = acos(dot(u, w).coerceIn(-1f, 1f))
    val sign = if (cross(u, w) > 0) 1f else -1f
    return rotate(u, sign * angle / 3)
}

private fun Pencil.morley(v: Int) {
    val shapes = listOf(
        listOf(pt(-1.2f, 0.7f), pt(1.3f, 0.7f), pt(0.15f, -0.95f)),
        listOf(pt(-1.3f, 0.6f), pt(1.1f, 0.75f), pt(-0.45f, -0.9f)),
        listOf(pt(-1.1f, 0.65f), pt(1.35f, 0.5f), pt(0.6f, -0.85f)),
    )
    val (a, b, c) = shapes[v % 3]
    val pab = meet(a, trisector(a, b, c), b, trisector(b, a, c))
    val pbc = meet(b, trisector(b, c, a), c, trisector(c, b, a))
    val pca = meet(c, trisector(c, a, b), a, trisector(a, c, b))
    val fit = Fit(listOf(listOf(a, b, c)), 10f)
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    fill(fit(listOf(a, b, pab)), pal[5], gap = 3.8f, wash = 0.3f)
    fill(fit(listOf(b, c, pbc)), pal[6], gap = 3.8f, wash = 0.3f, angle = 30f)
    fill(fit(listOf(c, a, pca)), pal[4], gap = 3.8f, wash = 0.3f, angle = 70f)
    fill(fit(listOf(a, pab, pca)), pal[2], gap = 3.2f, wash = 0.35f, angle = -20f)
    fill(fit(listOf(b, pbc, pab)), pal[2], gap = 3.2f, wash = 0.35f, angle = 50f)
    fill(fit(listOf(c, pca, pbc)), pal[2], gap = 3.2f, wash = 0.35f, angle = 10f)
    fill(fit(listOf(pab, pbc, pca)), pal[0], gap = 2.6f, wash = 0.5f, lw = 1.5f)
}

private fun Pencil.napoleon(v: Int) {
    val shapes = listOf(
        listOf(pt(-1f, 0.45f), pt(1.2f, 0.45f), pt(-0.25f, -0.8f)),
        listOf(pt(-1.1f, 0.5f), pt(0.9f, 0.6f), pt(0.3f, -0.7f)),
    )
    val tri = shapes[v % 2]
    val apexes = (0 until 3).map { i ->
        val p = tri[i]; val q = tri[(i + 1) % 3]; val z = tri[(i + 2) % 3]
        val m = (p + q) / 2f
        var n = (q - p).perp().unit()
        if (dot(n, z - m) > 0) n = -n
        m + n * ((q - p).getDistance() * sqrt(3f) / 2)
    }
    val outer = (0 until 3).map { listOf(tri[it], tri[(it + 1) % 3], apexes[it]) }
    val centers = outer.map { t -> (t[0] + t[1] + t[2]) / 3f }
    val fit = Fit(outer, 8f)
    val pal = Crayons.rainbow.rot((v * 3 + 2) % 8)
    outer.forEachIndexed { i, t -> fill(fit(t), pal[i * 2 + 1], angle = -58f + 50f * i, gap = 3.8f) }
    fill(fit(tri), Crayons.jaune, gap = 4.2f, wash = 0.25f)
    // Les droites de chaque sommet au sommet du triangle équilatéral opposé : elles se coupent au point de Fermat.
    for (i in 0 until 3) stroke(fit(tri[(i + 2) % 3]), fit(apexes[i]), Graphite, 0.9f, 0.6f)
    fill(fit(centers), Crayons.rouge, gap = 3f, wash = 0.35f, lw = 1.6f)
    for (p in centers) dot(fit(p), 2.6f, Graphite)
}

private fun Pencil.lunes(v: Int) {
    val (la, lb) = if (v % 2 == 0) 1f to 1f else 3f to 4f
    val c = sqrt(la * la + lb * lb)
    val a = pt(-c / 2, 0f); val b = pt(c / 2, 0f)
    val p = a + pt(lb * lb / c, -la * lb / c)
    val o = Offset.Zero
    val halfDisc = arcBetween(o, b, a, 60, away = pt(0f, 1f))
    fun lune(x: Offset, y: Offset): List<Offset> {
        val m = (x + y) / 2f
        return arcBetween(m, x, y, 30, away = o) + arcBetween(o, y, x, 30).drop(1)
    }
    val l1 = lune(a, p); val l2 = lune(p, b)
    val fit = Fit(listOf(halfDisc, l1, l2), 8f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fill(fit(halfDisc), pal[2], gap = 4.6f, wash = 0.18f, strokeAlpha = 0.45f)
    fill(fit(listOf(a, b, p)), pal[5], gap = 3.4f, wash = 0.4f, angle = 30f)
    fill(fit(l1), pal[0], gap = 3f, wash = 0.45f)
    fill(fit(l2), pal[0], gap = 3f, wash = 0.45f, angle = -30f)
}
