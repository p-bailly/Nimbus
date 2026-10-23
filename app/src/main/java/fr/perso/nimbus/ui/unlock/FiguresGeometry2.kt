package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

// Figures du thème « Géométrie », deuxième série : coniques, porisme de Poncelet, cercles exinscrits,
// kaléidoscope, inversion, spidron, Pythagore et Van Aubel, rectangle parfait, patrons du cube et
// pentaminos.

internal val moreGeometryFigures: List<FigureSpec> = listOf(
    FigureSpec("vertex_conics", "Coniques de même sommet", "Géométrie", { v ->
        val es = conicEccentricities(v)
        "Du cercle aux hyperboles, toutes ces coniques ont le même sommet et la même courbure en ce sommet : y² = 2px − (1 − e²)x², " +
            "où l’excentricité e va ici de 0 à ${frNum(es.last().toDouble(), 1)}. Ellipses pour e < 1, parabole pour e = 1, hyperboles au-delà ; " +
            "le foyer, à p/(1 + e) du sommet, s’en approche quand e grandit. Apollonius a nommé ces courbes d’après cette équation : " +
            "y² est en défaut (ellipse), égal (parabole) ou en excès (hyperbole) par rapport à 2px."
    }) { vertexConics(it) },
    FigureSpec("confocal_conics", "Ellipse du jardinier", "Géométrie", { v ->
        "Le jardinier trace une ellipse avec une ficelle nouée à deux piquets et tendue par un bâton : la somme des distances aux deux " +
            "foyers reste égale à la longueur de la ficelle. Autour, des ellipses et des hyperboles de mêmes foyers (sur une hyperbole, " +
            "c’est la différence des distances qui est constante) : elles se coupent partout à angle droit et découpent ce damier courbe " +
            "en ${confocalSectors(v)} secteurs."
    }) { confocalConics(it) },
    FigureSpec("excircles", "Cercles exinscrits", "Géométrie", { _ ->
        "Un triangle, son cercle inscrit et ses trois cercles exinscrits : chacun touche les trois droites des côtés. Leurs centres " +
            "forment un grand triangle dont les hauteurs sont les bissectrices du petit. Les rayons vérifient 1/r = 1/r₁ + 1/r₂ + 1/r₃. " +
            "Et, théorème de Feuerbach (1822), le cercle d’Euler du triangle, en rouge, touche les quatre cercles à la fois."
    }) { excircles(it) },
    FigureSpec("poncelet", "Porisme de Poncelet", "Géométrie", { v ->
        val (n, k, _) = ponceletParams(v)
        val turns = if (k == 1) "en faisant une fois le tour" else "en faisant deux fois le tour, en étoile"
        "Entre deux cercles, on part d’un point du grand, on suit une tangente au petit jusqu’au grand, et on recommence. Ici le " +
            "chemin se referme après $n côtés, $turns. Poncelet a montré que ce n’est pas un hasard : il se referme alors quel que soit " +
            "le point de départ, et les polygones tournent entre les deux cercles. Il l’a trouvé prisonnier en Russie, en 1813."
    }) { poncelet(it) },
    FigureSpec("perfect_rectangle", "Rectangle parfait", "Géométrie", { v ->
        val (w, h, squares) = PERFECT_RECTANGLES[v % 2]
        val sizes = squares.map { it[2] }.sortedDescending()
        "Un rectangle de $w × $h pavé par neuf carrés tous différents, de côtés ${sizes.dropLast(1).joinToString(", ")} et ${sizes.last()}. " +
            "Aucun rectangle ne se pave avec moins de neuf carrés distincts, et il n’y en a que deux avec neuf : 33 × 32 et 69 × 61. " +
            "Zbigniew Moroń a publié le premier en 1925. Le carré parfait est plus rétif : le plus petit en demande 21 (Duijvestijn, 1978)."
    }) { perfectRectangle(it) },
    FigureSpec("cube_nets", "Patrons du cube", "Géométrie", { _ ->
        "Les onze patrons du cube : les onze façons de découper sa surface le long de sept arêtes pour la déplier à plat, d’un seul " +
            "tenant. Les faces de même couleur deviennent opposées une fois le cube replié. Il existe 35 hexaminos, des formes de six " +
            "carrés collés par un côté, mais seuls ces onze se replient en cube."
    }) { cubeNets(it) },
    FigureSpec("pentominoes", "Pentaminos", "Géométrie", { _ ->
        "Les douze pentaminos, toutes les formes de cinq carrés collés par un côté, nommées d’après les lettres F, I, L, N, P, T, U, " +
            "V, W, X, Y et Z qu’elles évoquent, remplissent exactement ce rectangle 6 × 10 de 60 cases. Il y a 2339 façons de le faire, " +
            "sans compter les symétries du rectangle : les Haselgrove les ont toutes trouvées par ordinateur en 1960."
    }) { pentominoes(it) },
    FigureSpec("spidron", "Spidron", "Géométrie", { _ ->
        "Un hexagone découpé en six bras qui s’enroulent vers le centre : chaque bras enchaîne un triangle isocèle d’angles 30°, 30° " +
            "et 120° et un triangle équilatéral, puis recommence √3 fois plus petit. C’est un spidron, imaginé en 1979 par le graphiste " +
            "hongrois Dániel Erdély ; plié le long de ses arêtes, il se froisse en un relief en spirale."
    }) { spidron(it) },
    FigureSpec("pythagoras_shuffle", "Pythagore par réarrangement", "Géométrie", { v ->
        val (a, b) = shuffleRatio(v)
        "Deux carrés de côté a + b contiennent chacun quatre fois le même triangle rectangle (ici, côtés de l’angle droit dans le " +
            "rapport $a : $b). À gauche, il reste le carré de l’hypoténuse, c² ; à droite, les carrés des deux autres côtés, a² et b². " +
            "Les restes ont la même aire : a² + b² = c². Une preuve sans calcul, dont la figure de gauche orne déjà le Zhoubi suanjing chinois."
    }) { pythagorasShuffle(it) },
    FigureSpec("van_aubel", "Théorème de Van Aubel", "Géométrie", { _ ->
        "Sur les quatre côtés d’un quadrilatère quelconque, on pose quatre carrés vers l’extérieur. Les segments qui joignent les " +
            "centres de deux carrés opposés, en rouge et en bleu, ont toujours la même longueur et se coupent à angle droit. Henricus " +
            "van Aubel l’a publié en 1878 ; c’est un cousin du théorème de Napoléon, qui pose des triangles équilatéraux sur un triangle."
    }) { vanAubel(it) },
    FigureSpec("kaleidoscope", "Kaléidoscope", "Géométrie", { v ->
        val n = kaleidoscopeOrder(v)
        val angle = if (180 % n == 0) "${180 / n}°" else "180°/$n"
        "Deux miroirs à $angle l’un de l’autre, comme dans un kaléidoscope : le motif d’un seul secteur, reflété encore et encore, " +
            "remplit le disque de ${2 * n} copies, dont la moitié retournées. Ces images forment le groupe diédral d’ordre ${2 * n} : " +
            "$n rotations et $n réflexions. Pour qu’elles se raccordent, l’angle des miroirs doit diviser 180°. David Brewster a " +
            "inventé l’instrument en 1816."
    }) { kaleidoscope(it) },
    FigureSpec("inversion", "Inversion", "Géométrie", { _ ->
        "L’inversion de centre O et de rayon R envoie chaque point M sur le point M′ de la demi-droite OM tel que OM × OM′ = R² : " +
            "le cercle rouge reste en place, son intérieur et son extérieur s’échangent. L’image d’un damier : ses droites deviennent " +
            "des cercles qui passent par O, et comme l’inversion conserve les angles, les cases se croisent toujours à angle droit."
    }) { inversion(it) },
)

private fun Offset.unit() = this / getDistance()
private fun Offset.perp() = Offset(-y, x)
private fun cross(a: Offset, b: Offset) = a.x * b.y - a.y * b.x
private fun dotp(a: Offset, b: Offset) = a.x * b.x + a.y * b.y
private fun rotate(u: Offset, a: Float) = Offset(u.x * cos(a) - u.y * sin(a), u.x * sin(a) + u.y * cos(a))

/** Nombre écrit à la française, avec [d] décimales. */
private fun frNum(x: Double, d: Int) = String.format(Locale.ROOT, "%.${d}f", x).replace('.', ',')

/** Intersection des droites (p, p + d) et (q, q + e). */
private fun meet(p: Offset, d: Offset, q: Offset, e: Offset): Offset {
    val t = cross(q - p, e) / cross(d, e)
    return p + d * t
}

/** Polygone régulier : [n] sommets sur un cercle de rayon [r], le premier à l'angle [start] (degrés). */
private fun ngon(c: Offset, r: Float, n: Int, start: Float): List<Offset> = List(n) { c + dir(rad(start + 360f * it / n)) * r }

/** Le polygone [poly] découpé par le rectangle [x0, x1] × [y0, y1] (Sutherland–Hodgman). */
private fun clipBox(poly: List<Offset>, x0: Float, y0: Float, x1: Float, y1: Float): List<Offset> {
    var out = poly
    val sides = listOf<(Offset) -> Float>({ it.x - x0 }, { x1 - it.x }, { it.y - y0 }, { y1 - it.y })
    for (side in sides) {
        if (out.isEmpty()) break
        val src = out
        val res = ArrayList<Offset>(src.size + 4)
        for (i in src.indices) {
            val a = src[i]; val b = src[(i + 1) % src.size]
            val da = side(a); val db = side(b)
            if (da >= 0) res += a
            if ((da >= 0) != (db >= 0)) res += a + (b - a) * (da / (da - db))
        }
        out = res
    }
    return out
}

/** Contour d'un polyomino (sans point de contact intérieur) : les côtés de cases sans voisine, enchaînés. */
private fun cellsOutline(cells: List<Pair<Int, Int>>): List<Offset> {
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

/** Petit rond plein, pour marquer un point remarquable. */
private fun Pencil.mark(p: Offset, color: Color, r: Float = 3.6f) = fill(circle(p.x, p.y, r, 14), color, hatch = false, wash = 0.9f, lw = 0.9f)

// ——— Coniques ———

/** Excentricités dessinées, selon la variante. */
private fun conicEccentricities(v: Int) =
    if (v % 2 == 0) floatArrayOf(0f, 0.5f, 0.75f, 1f, 1.4f, 2.2f) else floatArrayOf(0f, 0.4f, 0.65f, 0.82f, 1f, 1.3f, 1.9f)

/**
 * Coniques de même sommet et de même paramètre p : y² = 2px − (1 − e²)x². Elles sont emboîtées ; chaque
 * bande entre deux voisines reçoit sa couleur, et chaque foyer, à p/(1 + e) du sommet, un point.
 */
private fun Pencil.vertexConics(v: Int) {
    val es = conicEccentricities(v)
    val p = 50f
    val x0 = 18f
    val cy = FH / 2
    val xMax = FW - 8f - x0
    val yMax = FH / 2 - 8f
    fun at(x: Float, y: Float) = pt(x0 + x, cy - y)
    // Chaque conique : la région qu'elle borde (du sommet vers le bas, puis le haut) et sa courbe seule.
    val regions = mutableListOf<List<Offset>>()
    val curves = mutableListOf<List<Offset>>()
    for (e in es) {
        val q = 1 - e * e
        if (q > 1e-4f) {
            val a = p / q
            val b = p / sqrt(q)
            val pts = (0..140).map { i -> val t = PI.toFloat() * (1 + 2f * i / 140); at(a + a * cos(t), b * sin(t)) }
            regions += pts
            curves += pts
        } else {
            // x(y) : la plus petite racine de (1 − e²)x² − 2px + y² = 0.
            fun xOf(y: Float) = if (abs(q) < 1e-4f) y * y / (2 * p) else (-p + sqrt(p * p - q * y * y)) / (-q)
            val lim = if (xOf(yMax) <= xMax) yMax else sqrt(2 * p * xMax - q * xMax * xMax)
            val ys = (0..70).map { lim * it / 70 }
            val down = ys.map { at(xOf(it), -it) }
            val up = ys.reversed().map { at(xOf(it), it) }
            curves += down.reversed() + up.drop(1)
            regions += down + listOf(at(xMax, -lim), at(xMax, lim)) + up
        }
    }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (i in es.indices.reversed()) {
        val band = if (i == 0) regions[0] else regions[i] + regions[i - 1].reversed()
        fill(band, pal[i % 8], angle = -58f + 27f * i, gap = 3.4f + 0.25f * i, wash = 0.3f, outline = false)
    }
    stroke(at(-10f, 0f), at(xMax, 0f), Graphite, 0.8f, 0.45f)
    curves.forEachIndexed { i, c -> outline(c, close = es[i] < 1f, lw = 1.2f) }
    es.forEachIndexed { i, e ->
        mark(at(p / (1 + e), 0f), pal[i % 8], 3.2f)
        if (e > 0f && e < 1f) mark(at(2 * p / (1 - e * e) - p / (1 + e), 0f), pal[i % 8], 3.2f)
    }
}

/** Nombre de secteurs (d'hyperboles) du damier homofocal. */
private fun confocalSectors(v: Int) = if (v % 2 == 0) 16 else 12

/**
 * Coniques homofocales : en coordonnées elliptiques x = c ch μ cos ν, y = c sh μ sin ν, les ellipses
 * μ = cte et les hyperboles ν = cte découpent un damier ; la ficelle du jardinier relie les foyers à un
 * point de la grande ellipse.
 */
private fun Pencil.confocalConics(v: Int) {
    val m = confocalSectors(v)
    val rings = 5
    val muMax = 0.8f
    val c = 131f
    val o = center
    val tau = 2 * PI.toFloat()
    fun at(mu: Float, nu: Float) = pt(o.x + c * cosh(mu) * cos(nu), o.y - c * sinh(mu) * sin(nu))
    val pal = Crayons.rainbow.rot((v * 3 + 4) % 8)
    for (i in 0 until rings) for (j in 0 until m) {
        val mu0 = muMax * i / rings; val mu1 = muMax * (i + 1) / rings
        val nu0 = tau * j / m; val nu1 = tau * (j + 1) / m
        val s = 8
        val cell = (0..s).map { at(mu0, nu0 + (nu1 - nu0) * it / s) } +
            (0..s).map { at(mu0 + (mu1 - mu0) * it / s, nu1) } +
            (0..s).map { at(mu1, nu1 - (nu1 - nu0) * it / s) } +
            (0..s).map { at(mu1 - (mu1 - mu0) * it / s, nu0) }
        if ((i + j) % 2 == 0) {
            fill(cell, pal[i], angle = -58f + 360f * j / m, gap = 3.4f, wash = 0.36f, lw = 0.8f)
        } else {
            fill(cell, pal[i], hatch = false, wash = 0.13f, lw = 0.8f)
        }
    }
    outline((0 until 160).map { at(muMax, tau * it / 160) }, true, 1.6f)
    // La ficelle du jardinier, nouée aux deux piquets.
    val f1 = pt(o.x - c, o.y); val f2 = pt(o.x + c, o.y)
    val pin = at(muMax, rad(58f + 41f * v))
    line(listOf(f1, pin, f2), Crayons.brun, 1.6f)
    mark(f1, Graphite, 3.4f)
    mark(f2, Graphite, 3.4f)
    mark(pin, Crayons.brun, 3.4f)
}

// ——— Triangles et cercles ———

/** Triangles des cercles exinscrits, selon la variante. */
private val EXCIRCLE_TRIANGLES = listOf(
    listOf(pt(-0.36f, 0.83f), pt(-0.36f, -0.77f), pt(0.46f, 0.02f)),
    listOf(pt(0.4f, 0.8f), pt(0.3f, -0.8f), pt(-0.45f, 0.1f)),
    listOf(pt(-0.42f, 0.7f), pt(-0.3f, -0.85f), pt(0.5f, 0.2f)),
)

/**
 * Cercle inscrit, cercles exinscrits (centres (−aA + bB + cC)/(−a + b + c)… , rayons Aire/(s − a)…)
 * et cercle d'Euler, qui les touche tous (Feuerbach).
 */
private fun Pencil.excircles(v: Int) {
    val (a, b, c) = EXCIRCLE_TRIANGLES[v % 3]
    val la = (b - c).getDistance(); val lb = (c - a).getDistance(); val lc = (a - b).getDistance()
    val s = (la + lb + lc) / 2
    val area = abs(cross(b - a, c - a)) / 2
    val inC = (a * la + b * lb + c * lc) / (2 * s)
    val r = area / s
    val ex = listOf(
        (a * -la + b * lb + c * lc) / (lb + lc - la) to area / (s - la),
        (a * la - b * lb + c * lc) / (la - lb + lc) to area / (s - lb),
        (a * la + b * lb - c * lc) / (la + lb - lc) to area / (s - lc),
    )
    // Centre du cercle circonscrit O, orthocentre H = A + B + C − 2O ; cercle d'Euler de centre (O + H)/2.
    val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
    val sa = a.x * a.x + a.y * a.y; val sb = b.x * b.x + b.y * b.y; val sc = c.x * c.x + c.y * c.y
    val o = pt((sa * (b.y - c.y) + sb * (c.y - a.y) + sc * (a.y - b.y)) / d, (sa * (c.x - b.x) + sb * (a.x - c.x) + sc * (b.x - a.x)) / d)
    val h = a + b + c - o * 2f
    val nine = (o + h) / 2f
    val nineR = (a - o).getDistance() / 2
    val discs = ex.map { (e, re) -> circle(e.x, e.y, re, 140) }
    val fit = Fit(discs, 6f)
    val pal = Crayons.rainbow.rot((v * 3 + 3) % 8)
    fill(fit(ex.map { it.first }), pal[7], hatch = false, wash = 0.1f, lw = 0.8f)
    discs.forEachIndexed { i, disc -> fill(fit(disc), pal[i * 2], wash = 0.26f, gap = 4.2f, angle = -58f + 50f * i, lw = 1.2f) }
    // Les droites des côtés, prolongées jusqu'aux points de contact.
    val tri = listOf(a, b, c)
    for (i in 0 until 3) {
        val p = tri[(i + 1) % 3]; val q = tri[(i + 2) % 3]
        val u = (q - p).unit()
        val ts = ex.map { dotp(it.first - p, u) } + listOf(0f, (q - p).getDistance())
        val pad = 0.08f
        stroke(fit(p + u * (ts.min() - pad)), fit(p + u * (ts.max() + pad)), Graphite, 1f, 0.75f)
    }
    // Les bissectrices intérieures : de chaque centre exinscrit au sommet opposé, par le centre inscrit.
    for (i in 0 until 3) stroke(fit(ex[i].first), fit(tri[i]), Graphite, 0.8f, 0.5f)
    fill(fit(tri), pal[6], gap = 3.2f, wash = 0.42f, angle = 30f, lw = 1.3f)
    fill(fit(circle(inC.x, inC.y, r, 70)), pal[1], gap = 3f, wash = 0.45f, angle = 70f, lw = 1.1f)
    val nc = fit(circle(nine.x, nine.y, nineR, 100))
    line(nc + nc[0], Crayons.rouge, 1.5f)
    for (p in tri) dot(fit(p), 2.2f, Graphite)
    for ((e, _) in ex) dot(fit(e), 2.2f, Graphite)
}

/** Variantes du porisme : nombre de côtés, nombre de tours, décalage du petit cercle. */
private fun ponceletParams(v: Int) = listOf(Triple(5, 1, 0.3), Triple(7, 2, 0.22), Triple(6, 1, 0.36), Triple(5, 2, 0.18))[v % 4]

/**
 * Un pas de Poncelet sur le cercle unité : du point d'angle [t], la tangente au petit cercle (centre
 * (d, 0), rayon r) qui le laisse à gauche, jusqu'au grand cercle ; renvoie l'angle du point d'arrivée.
 */
private fun ponceletStep(t: Double, d: Double, r: Double): Double {
    val px = cos(t); val py = sin(t)
    val wx = d - px; val wy = -py
    val l = hypot(wx, wy)
    val al = asin((r / l).coerceAtMost(1.0))
    val ux = wx / l; val uy = wy / l
    val tx = ux * cos(al) + uy * sin(al); val ty = -ux * sin(al) + uy * cos(al)
    val k = -2 * (px * tx + py * ty)
    var dt = atan2(py + ty * k, px + tx * k) - t
    while (dt <= 0) dt += 2 * PI
    while (dt > 2 * PI) dt -= 2 * PI
    return t + dt
}

private val ponceletCache = HashMap<Int, Double>()

/** Rayon du petit cercle pour lequel le chemin se referme après n côtés et k tours (dichotomie). */
private fun ponceletRadius(v: Int): Double = ponceletCache.getOrPut(v % 4) {
    val (n, k, d) = ponceletParams(v)
    var lo = 1e-4
    var hi = 1 - d - 1e-4
    repeat(60) {
        val mid = (lo + hi) / 2
        var t = 0.0
        repeat(n) { t = ponceletStep(t, d, mid) }
        if (t > 2 * PI * k) lo = mid else hi = mid
    }
    (lo + hi) / 2
}

/** Porisme de Poncelet : huit polygones de même départ décalé, inscrits dans le grand cercle et circonscrits au petit. */
private fun Pencil.poncelet(v: Int) {
    val (n, _, d) = ponceletParams(v)
    val r = ponceletRadius(v)
    val big = 114f
    val o = center
    fun at(t: Double) = pt(o.x + (cos(t) * big).toFloat(), o.y - (sin(t) * big).toFloat())
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val c = pt(o.x + (d * big).toFloat(), o.y)
    val inner = circle(c.x, c.y, (r * big).toFloat(), 120)
    // L'anneau entre les deux cercles, puis le petit cercle, chacun de sa couleur.
    scope.run { clipPath(path(inner), ClipOp.Difference) { fill(circle(o.x, o.y, big, 180), Crayons.jaune, wash = 0.14f, gap = 5.5f, strokeAlpha = 0.3f, lw = 1.4f) } }
    fill(inner, pal[5], wash = 0.2f, gap = 4.6f, angle = 30f, strokeAlpha = 0.5f, outline = false)
    val m = 8
    val first = ponceletStep(0.0, d, r)
    for (j in 0 until m) {
        var t = first * j / m + 0.3
        val pts = (0 until n).map { val q = at(t); t = ponceletStep(t, d, r); q }
        scope.run { clipPath(path(inner), ClipOp.Difference) { fill(pts, pal[j], hatch = false, wash = 0.08f, outline = false) } }
        line(pts + pts[0], pal[j], 1.3f)
        for (q in pts) dot(q, 1.8f, pal[j])
    }
    outline(inner, true, 1.4f)
}

// ——— Pavés et dissections ———

/** Les deux rectangles parfaits d'ordre 9 : largeur, hauteur, carrés (x, y, côté), vérifiés par placement en ligne d'horizon. */
private val PERFECT_RECTANGLES = listOf(
    Triple(
        33, 32,
        listOf(
            intArrayOf(0, 0, 18), intArrayOf(18, 0, 15), intArrayOf(18, 15, 7), intArrayOf(25, 15, 8), intArrayOf(0, 18, 14),
            intArrayOf(14, 18, 4), intArrayOf(14, 22, 10), intArrayOf(24, 22, 1), intArrayOf(24, 23, 9),
        ),
    ),
    Triple(
        69, 61,
        listOf(
            intArrayOf(0, 0, 36), intArrayOf(36, 0, 33), intArrayOf(36, 33, 5), intArrayOf(41, 33, 28), intArrayOf(0, 36, 25),
            intArrayOf(25, 36, 9), intArrayOf(34, 36, 2), intArrayOf(34, 38, 7), intArrayOf(25, 45, 16),
        ),
    ),
)

/** Rectangle parfait : neuf carrés tous différents, chacun de sa couleur. */
private fun Pencil.perfectRectangle(v: Int) {
    val (w, h, squares) = PERFECT_RECTANGLES[v % 2]
    val s = min((FW - 24) / w, (FH - 24) / h)
    val x0 = (FW - w * s) / 2; val y0 = (FH - h * s) / 2
    val flip = (v / 2) % 2 == 1
    val pal = (Crayons.rainbow + Crayons.marine).rot((v * 3) % 9)
    squares.forEachIndexed { i, (x, y, z) ->
        val xx = if (flip) w - x - z else x
        val q = listOf(pt(xx.toFloat(), y.toFloat()), pt((xx + z).toFloat(), y.toFloat()), pt((xx + z).toFloat(), (y + z).toFloat()), pt(xx.toFloat(), (y + z).toFloat()))
            .map { pt(x0 + it.x * s, y0 + it.y * s) }
        fill(q, pal[i], angle = -58f + 37f * i, gap = (2.8f + z * s / 70f).coerceAtMost(4.6f), wash = 0.34f, lw = 1.3f)
    }
    outline(listOf(pt(x0, y0), pt(x0 + w * s, y0), pt(x0 + w * s, y0 + h * s), pt(x0, y0 + h * s)), true, 1.8f)
}

/** Les onze patrons du cube ; chaque chiffre dit la paire de faces opposées (calculée en roulant le cube sur le patron). */
private val CUBE_NETS = listOf(
    listOf("2...", "1010", "2..."),
    listOf(".2..", "0101", ".2.."),
    listOf("2...", "1010", ".2.."),
    listOf("2...", "1010", "..2."),
    listOf("2...", "1010", "...2"),
    listOf(".2..", "0101", "..2."),
    listOf("20..", ".121", ".0.."),
    listOf("20..", ".121", "..0."),
    listOf("20..", ".121", "...0"),
    listOf("20..", ".12.", "..01"),
    listOf("202..", "..101"),
)

/** Patrons du cube : onze patrons en trois rangées ; les faces opposées ont la même couleur. */
private fun Pencil.cubeNets(v: Int) {
    val sz = 19f
    val slotH = 80f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val colors = listOf(pal[0], pal[3], pal[5])
    val mirror = v % 2 == 1
    CUBE_NETS.forEachIndexed { idx, rows ->
        val row = idx / 4; val col = idx % 4
        val inRow = if (row < 2) 4 else 3
        val slotW = if (row < 2) 90f else 112f
        val cx = FW / 2 + (col - (inRow - 1) / 2f) * slotW
        val cy = FH / 2 + (row - 1) * slotH
        val w = rows[0].length; val h = rows.size
        val ox = cx - w * sz / 2; val oy = cy - h * sz / 2
        for (y in 0 until h) for (x in 0 until w) {
            val ch = rows[y][x]
            if (ch == '.') continue
            val pair = ch - '0'
            val xx = if (mirror) w - 1 - x else x
            val q = listOf(pt(ox + xx * sz, oy + y * sz), pt(ox + (xx + 1) * sz, oy + y * sz), pt(ox + (xx + 1) * sz, oy + (y + 1) * sz), pt(ox + xx * sz, oy + (y + 1) * sz))
            fill(q, colors[pair], angle = -58f + 60f * pair, gap = 3.2f, wash = 0.38f, lw = 1.1f)
        }
    }
}

/** Deux pavages du rectangle 6 × 10 par les douze pentaminos (trouvés par un petit solveur, codés en dur). */
private val PENTO_SOLUTIONS = listOf(
    listOf("FFYYYYWWPP", "IFFXYWWPPP", "IFXXXWTVVV", "INNXTTTZZV", "ILNNNUTUZV", "ILLLLUUUZZ"),
    listOf("IIIIIWWVVV", "LLNNNXWWYV", "LNNPXXXWYV", "LTPPFXZZYY", "LTPPFFZUYU", "TTTFFZZUUU"),
)

/** Pentaminos : les douze pièces dans le rectangle 6 × 10. */
private fun Pencil.pentominoes(v: Int) {
    val sol = PENTO_SOLUTIONS[v % 2]
    val cell = 34f
    val o = pt(FW / 2 - 5 * cell, FH / 2 - 3 * cell)
    val colors = listOf(
        Crayons.rouge, Crayons.turquoise, Crayons.jaune, Crayons.violet, Crayons.orange, Crayons.bleu,
        Crayons.vert, Crayons.rose, Crayons.marine, Color(0xFFC9A93A), Crayons.sapin, Crayons.brun,
    ).rot((v * 5) % 12)
    "FILNPTUVWXYZ".forEachIndexed { i, letter ->
        val cells = mutableListOf<Pair<Int, Int>>()
        for (y in 0 until 6) for (x in 0 until 10) if (sol[y][x] == letter) cells += x to y
        val poly = cellsOutline(cells).map { o + it * cell }
        fill(poly, colors[i], angle = -58f + 31f * i, gap = 3.7f, wash = 0.36f, lw = 1.4f)
    }
    for (x in 0..10) stroke(o + Offset(x * cell, 0f), o + Offset(x * cell, 6 * cell), Graphite, 0.5f, 0.22f)
    for (y in 0..6) stroke(o + Offset(0f, y * cell), o + Offset(10 * cell, y * cell), Graphite, 0.5f, 0.22f)
}

/**
 * Spidron : dans l'hexagone, les centres des six triangles équilatéraux (centre, côté) forment un
 * hexagone √3 fois plus petit, tourné de 30° ; entre les deux, six triangles 30-30-120 et six équilatéraux.
 * On recommence : le k-ième de chaque anneau prolonge le bras k, qui s'enroule vers le centre.
 */
private fun Pencil.spidron(v: Int) {
    val side = 133f
    val o = center
    val mirror = v % 2 == 1
    fun m(p: Offset) = if (mirror) pt(2 * o.x - p.x, p.y) else p
    var verts = ngon(o, side, 6, 0f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    var level = 0
    while ((verts[0] - o).getDistance() > 2.5f) {
        val size = (verts[0] - o).getDistance()
        val f = sqrt(size / side)
        val cs = (0 until 6).map { k -> (o + verts[k] + verts[(k + 1) % 6]) / 3f }
        for (k in 0 until 6) {
            val col = pal[(k * 4 / 3) % 8]
            val lw = (1.4f * f).coerceAtLeast(0.45f)
            fill(listOf(verts[k], verts[(k + 1) % 6], cs[k]).map(::m), col, hatch = size > 14f, wash = if (size > 14f) 0.4f else 0.55f, gap = (3.8f * f).coerceAtLeast(2.2f), angle = -58f + 60f * k - 30f * level, lw = lw)
            fill(listOf(verts[(k + 1) % 6], cs[(k + 1) % 6], cs[k]).map(::m), col, hatch = false, wash = 0.14f, lw = lw)
        }
        verts = cs
        level++
    }
    fill(verts.map(::m), Crayons.jaune, hatch = false, wash = 0.5f, lw = 0.5f)
}

/** Rapport a : b des côtés de l'angle droit, selon la variante. */
private fun shuffleRatio(v: Int) = listOf(1 to 2, 3 to 4, 2 to 3)[v % 3]

/** Pythagore par réarrangement : quatre triangles autour de c², puis les mêmes autour de a² et b². */
private fun Pencil.pythagorasShuffle(v: Int) {
    val (ra, rb) = shuffleRatio(v)
    val l = 158f
    val a = l * ra / (ra + rb); val b = l - a
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    val tris = listOf(pal[0], pal[1], pal[2], pal[7])
    fun place(x0: Float, pts: List<Offset>) = pts.map { pt(x0 + it.x, (FH - l) / 2 + it.y) }
    val left = 16f; val right = FW - 16f - l
    // À gauche : le carré de l'hypoténuse au milieu.
    fill(place(left, listOf(pt(a, 0f), pt(l, a), pt(b, l), pt(0f, b))), pal[5], gap = 3.6f, wash = 0.36f, angle = 15f, lw = 1.4f)
    val t1 = listOf(
        listOf(pt(0f, 0f), pt(a, 0f), pt(0f, b)),
        listOf(pt(a, 0f), pt(l, 0f), pt(l, a)),
        listOf(pt(l, a), pt(l, l), pt(b, l)),
        listOf(pt(b, l), pt(0f, l), pt(0f, b)),
    )
    t1.forEachIndexed { i, t -> fill(place(left, t), tris[i], gap = 3.4f, wash = 0.34f, angle = -58f + 45f * i, lw = 1.3f) }
    // À droite : les carrés des deux côtés de l'angle droit, et deux rectangles coupés en diagonale.
    fill(place(right, listOf(pt(0f, 0f), pt(a, 0f), pt(a, a), pt(0f, a))), pal[4], gap = 3.4f, wash = 0.36f, angle = 15f, lw = 1.4f)
    fill(place(right, listOf(pt(a, a), pt(l, a), pt(l, l), pt(a, l))), pal[6], gap = 3.6f, wash = 0.36f, angle = 15f, lw = 1.4f)
    val t2 = listOf(
        listOf(pt(a, 0f), pt(l, 0f), pt(l, a)),
        listOf(pt(a, 0f), pt(l, a), pt(a, a)),
        listOf(pt(0f, a), pt(a, a), pt(a, l)),
        listOf(pt(0f, a), pt(a, l), pt(0f, l)),
    )
    t2.forEachIndexed { i, t -> fill(place(right, t), tris[i], gap = 3.4f, wash = 0.34f, angle = -58f + 45f * i, lw = 1.3f) }
    // Le signe égal, entre les deux.
    val mx = FW / 2
    line(listOf(pt(mx - 6f, FH / 2 - 4f), pt(mx + 6f, FH / 2 - 4f)), Graphite, 1.6f)
    line(listOf(pt(mx - 6f, FH / 2 + 4f), pt(mx + 6f, FH / 2 + 4f)), Graphite, 1.6f)
}

/** Quadrilatères de Van Aubel, selon la variante. */
private val VAN_AUBEL_QUADS = listOf(
    listOf(pt(-1f, 0.3f), pt(0.15f, 0.55f), pt(0.8f, -0.1f), pt(-0.35f, -0.55f)),
    listOf(pt(-0.9f, 0.45f), pt(0.7f, 0.5f), pt(0.55f, -0.45f), pt(-0.3f, -0.4f)),
    listOf(pt(-0.85f, 0.15f), pt(0.3f, 0.6f), pt(0.85f, 0.05f), pt(-0.05f, -0.5f)),
)

/** Théorème de Van Aubel : quatre carrés sur un quadrilatère, et les deux segments entre centres opposés. */
private fun Pencil.vanAubel(v: Int) {
    val q = VAN_AUBEL_QUADS[v % 3]
    val g = q.reduce { s, p -> s + p } / 4f
    val squares = (0 until 4).map { i ->
        val p = q[i]; val r = q[(i + 1) % 4]
        var n = (r - p).perp()
        if (dotp(n, (p + r) / 2f - g) < 0) n = -n
        listOf(p, r, r + n, p + n)
    }
    val centers = squares.map { (it[0] + it[1] + it[2] + it[3]) / 4f }
    val fit = Fit(squares, 8f)
    val pal = Crayons.rainbow.rot((v * 3 + 2) % 8)
    squares.forEachIndexed { i, sq -> fill(fit(sq), pal[i * 2 + 1], gap = 3.6f, wash = 0.34f, angle = -58f + 45f * i, lw = 1.3f) }
    fill(fit(q), Crayons.jaune, gap = 4f, wash = 0.36f, angle = 30f, lw = 1.4f)
    val (c0, c1, c2, c3) = centers.map { fit(it) }
    line(listOf(c0, c2), Crayons.rouge, 2f)
    line(listOf(c1, c3), Crayons.marine, 2f)
    // L'angle droit, au croisement.
    val x = meet(c0, c2 - c0, c1, c3 - c1)
    val u = (c2 - c0).unit() * 7f; val w = (c3 - c1).unit() * 7f
    line(listOf(x + u, x + u + w, x + w), Graphite, 1f)
    for (c in listOf(c0, c1, c2, c3)) mark(c, Graphite, 3f)
}

// ——— Quadrillages ———

// ——— Cercles tangents ———

/** Ordre du kaléidoscope (nombre de rotations), selon la variante. */
private fun kaleidoscopeOrder(v: Int) = intArrayOf(6, 5, 8, 7)[v % 4]

/**
 * Kaléidoscope : un motif posé dans le secteur 0 ≤ θ ≤ π/n, puis ses images par les n rotations et les n
 * réflexions du groupe diédral.
 */
private fun Pencil.kaleidoscope(v: Int) {
    val n = kaleidoscopeOrder(v)
    val big = 116f
    val o = center
    val w = PI.toFloat() / n
    fun polar(r: Float, t: Float) = pt(r * cos(t), r * sin(t))
    // Fuseau le long du rayon d'angle [t], de [r0] à [r1], de demi-ouverture [open] (en fraction de w).
    fun spindle(t: Float, r0: Float, r1: Float, open: Float): List<Offset> {
        val k = 16
        val one = (0..k).map { i -> val s = i.toFloat() / k; polar(r0 + (r1 - r0) * s, t + open * w * sin(PI.toFloat() * s)) }
        val two = (1 until k).map { i -> val s = 1f - i.toFloat() / k; polar(r0 + (r1 - r0) * s, t - open * w * sin(PI.toFloat() * s)) }
        return one + two
    }
    fun roundel(r: Float, t: Float, rr: Float): List<Offset> { val c = polar(r, t); return circle(c.x, c.y, rr, 28) }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val motif = listOf(
        spindle(0f, 0.16f, 0.8f, 0.55f) to pal[0],
        spindle(w, 0.34f, 0.97f, 0.5f) to pal[5],
        roundel(0.52f, 0.52f * w, 0.24f * sin(w) + 0.02f) to pal[2],
        listOf(polar(0.8f, 0.2f * w), polar(0.96f, 0.5f * w), polar(0.8f, 0.8f * w), polar(0.7f, 0.5f * w)) to pal[3],
        roundel(0.92f, 0f, 0.05f) to pal[6],
    )
    fill(circle(o.x, o.y, big, 180), Crayons.jaune, wash = 0.12f, gap = 6f, strokeAlpha = 0.25f, lw = 1f)
    fun place(p: Offset, k: Int, refl: Boolean): Offset {
        val q = rotate(if (refl) pt(p.x, -p.y) else p, 2 * w * k)
        return pt(o.x + q.x * big, o.y - q.y * big)
    }
    for ((idx, item) in motif.withIndex()) {
        val (shape, color) = item
        for (k in 0 until n) for (refl in listOf(false, true)) {
            fill(shape.map { place(it, k, refl) }, color, gap = 3f, wash = 0.3f, angle = -58f + 360f * k / n + if (refl) 40f else 0f, lw = if (idx < 2) 1.1f else 0.9f)
        }
    }
    fill(ngon(o, 0.12f * big, 2 * n, 0f), pal[7], gap = 2.6f, wash = 0.5f, lw = 1f)
    // Les miroirs.
    for (k in 0 until n) {
        val d = dir(w * k)
        stroke(pt(o.x - d.x * big, o.y + d.y * big), pt(o.x + d.x * big, o.y - d.y * big), Graphite, 0.6f, 0.3f)
    }
    outline(circle(o.x, o.y, big, 180), true, 1.5f)
}

/** Triangles de Malfatti, selon la variante (y vers le bas, base horizontale). */
private val MALFATTI_TRIANGLES = listOf(
    listOf(pt(0f, 0f), pt(1f, 0f), pt(0.38f, -0.72f)),
    listOf(pt(0f, 0f), pt(1f, 0f), pt(0.66f, -0.8f)),
    listOf(pt(0f, 0f), pt(1f, 0f), pt(0.5f, -0.866f)),
)

private val malfattiCache = HashMap<Int, Pair<List<Pair<Offset, Float>>, List<Pair<Offset, Float>>>>()

/**
 * Les cercles de Malfatti (méthode de Newton sur les trois rayons : le centre i est sur la bissectrice, à
 * rᵢ / sin(Âᵢ/2) du sommet, et deux centres sont à rᵢ + rⱼ l'un de l'autre) et les trois cercles gloutons
 * (le cercle inscrit, puis deux fois le plus grand cercle dans un coin).
 */
private fun malfattiCircles(v: Int) = malfattiCache.getOrPut(v % 3) {
    val tri = MALFATTI_TRIANGLES[v % 3]
    val bis = (0 until 3).map { i ->
        val p = tri[i]
        val u1 = (tri[(i + 1) % 3] - p).unit(); val u2 = (tri[(i + 2) % 3] - p).unit()
        (u1 + u2).unit() to sin(acos(dotp(u1, u2).coerceIn(-1f, 1f)) / 2).toDouble()
    }
    fun centre(i: Int, r: Double): Pair<Double, Double> {
        val (u, s) = bis[i]
        return (tri[i].x + u.x * r / s) to (tri[i].y + u.y * r / s)
    }
    fun f(r: DoubleArray) = DoubleArray(3) { i ->
        val j = (i + 1) % 3
        val (xi, yi) = centre(i, r[i]); val (xj, yj) = centre(j, r[j])
        hypot(xi - xj, yi - yj) - r[i] - r[j]
    }
    val la = (tri[1] - tri[2]).getDistance(); val lb = (tri[2] - tri[0]).getDistance(); val lc = (tri[0] - tri[1]).getDistance()
    val sp = (la + lb + lc) / 2
    val area = abs(cross(tri[1] - tri[0], tri[2] - tri[0])) / 2
    val inR = (area / sp).toDouble()
    val r = DoubleArray(3) { inR / 2 }
    repeat(60) {
        val f0 = f(r)
        val jac = Array(3) { DoubleArray(3) }
        for (c in 0 until 3) {
            val rr = r.copyOf(); rr[c] += 1e-7
            val f1 = f(rr)
            for (l in 0 until 3) jac[l][c] = (f1[l] - f0[l]) / 1e-7
        }
        // Résolution du système 3 × 3 par Cramer.
        fun det(m: Array<DoubleArray>) = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])
        val dt = det(jac)
        if (abs(dt) < 1e-14) return@repeat
        for (c in 0 until 3) {
            val mc = Array(3) { l -> DoubleArray(3) { cc -> if (cc == c) f0[l] else jac[l][cc] } }
            r[c] -= det(mc) / dt
        }
    }
    val malfatti = (0 until 3).map { i -> val (x, y) = centre(i, r[i]); pt(x.toFloat(), y.toFloat()) to r[i].toFloat() }
    // Méthode gloutonne : dans le coin i, le k-ième cercle a pour rayon r·qᵢᵏ, qᵢ = (1 − sin)/(1 + sin).
    val inC = (tri[0] * la + tri[1] * lb + tri[2] * lc) / (2 * sp)
    val greedy = mutableListOf(inC to inR.toFloat())
    val depth = IntArray(3)
    repeat(2) {
        val i = (0 until 3).maxBy { val s = bis[it].second; Math.pow((1 - s) / (1 + s), depth[it] + 1.0) }
        depth[i]++
        val s = bis[i].second
        val rr = inR * Math.pow((1 - s) / (1 + s), depth[i].toDouble())
        greedy += (tri[i] + bis[i].first * (rr / s).toFloat()) to rr.toFloat()
    }
    malfatti to greedy
}

/**
 * Cercles de Malfatti (à gauche) et cercles gloutons (à droite, dans le même triangle retourné d'un
 * demi-tour, le long de son côté droit).
 */
private fun Pencil.malfatti(v: Int) {
    val tri = MALFATTI_TRIANGLES[v % 3]
    val (mal, greedy) = malfattiCircles(v)
    // Symétrie centrale de centre P, juste au-delà du côté BC : le triangle retourné longe le premier.
    val bc = tri[2] - tri[1]
    var nrm = bc.perp().unit()
    if (dotp(nrm, tri[0] - tri[1]) > 0) nrm = -nrm
    val p = (tri[1] + tri[2]) / 2f + nrm * 0.03f
    fun turn(q: Offset) = p * 2f - q
    val tri2 = tri.map(::turn)
    val fit = Fit(listOf(tri, tri2), 10f)
    val pal = Crayons.rainbow.rot((v * 3 + 5) % 8)
    fill(fit(tri), pal[2], wash = 0.2f, gap = 5f, strokeAlpha = 0.45f, lw = 1.4f)
    fill(fit(tri2), pal[2], wash = 0.2f, gap = 5f, angle = 30f, strokeAlpha = 0.45f, lw = 1.4f)
    val colors = listOf(pal[0], pal[4], pal[6])
    mal.forEachIndexed { i, (c, r) ->
        val q = fit(c)
        fill(circle(q.x, q.y, r * fit.s, 70), colors[i], gap = 3.2f, wash = 0.4f, angle = -58f + 50f * i, lw = 1.2f)
    }
    greedy.forEachIndexed { i, (c, r) ->
        val q = fit(turn(c))
        fill(circle(q.x, q.y, r * fit.s, 70), colors[i], gap = 3.2f, wash = 0.4f, angle = -58f + 50f * i, lw = 1.2f)
    }
}

/**
 * Inversion d'un damier dans un cercle : chaque case (centre du cercle au milieu d'une case) devient un
 * quadrilatère d'arcs de cercle ; les images sont découpées au cadre.
 */
private fun Pencil.inversion(v: Int) {
    val o = center
    val rho = 72f
    val g = 30f
    val turn = if (v % 2 == 0) 0f else rad(45f)
    val pal = Crayons.rainbow.rot((v * 3 + 5) % 8)
    val n = 12
    fun inv(p: Offset): Offset {
        val d = p - o
        return o + d * (rho * rho / d.getDistanceSquared())
    }
    fun pre(x: Float, y: Float) = o + rotate(pt(x * g, y * g), turn)
    val cells = mutableListOf<Triple<List<Offset>, Int, Boolean>>()
    for (i in -n..n) for (j in -n..n) {
        val ring = max(abs(i), abs(j))
        if (ring == 0) continue
        val x0 = i - 0.5f; val y0 = j - 0.5f
        val k = if (ring < 4) 14 else 6
        val edge = (0 until k).map { pre(x0 + it.toFloat() / k, y0) } +
            (0 until k).map { pre(x0 + 1, y0 + it.toFloat() / k) } +
            (0 until k).map { pre(x0 + 1 - it.toFloat() / k, y0 + 1) } +
            (0 until k).map { pre(x0, y0 + 1 - it.toFloat() / k) }
        val img = clipBox(edge.map(::inv), 4f, 4f, FW - 4f, FH - 4f)
        if (img.size < 3) continue
        val ext = max(img.maxOf { it.x } - img.minOf { it.x }, img.maxOf { it.y } - img.minOf { it.y })
        if (ext < 1.2f) continue
        cells += Triple(img, ring, (i + j) % 2 == 0)
    }
    fill(circle(o.x, o.y, 16f, 40), pal[(n - 1) % 8], hatch = false, wash = 0.45f, outline = false)
    for ((img, ring, even) in cells) {
        val ext = max(img.maxOf { it.x } - img.minOf { it.x }, img.maxOf { it.y } - img.minOf { it.y })
        val color = pal[(ring - 1) % 8]
        val lw = if (ext > 14f) 0.9f else 0.5f
        if (even) {
            fill(img, color, hatch = ext > 8f, gap = 3.4f, wash = if (ext > 8f) 0.36f else 0.5f, angle = -58f + 30f * ring, lw = lw)
        } else {
            fill(img, color, hatch = false, wash = 0.12f, lw = lw)
        }
    }
    val rc = circle(o.x, o.y, rho, 120)
    line(rc + rc[0], Crayons.rouge, 1.8f)
    dot(o, 2.4f, Graphite)
}

