package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.random.Random

// Figures des thèmes « Cercles », « Nombres » et « Graphes », deuxième série : faisceaux d'Apollonius,
// engrenages, bulles ; tas de sable, PGCD, spirale de Sacks, Catalan ; quatre couleurs, cavalier.

internal val moreNumberFigures: List<FigureSpec> = listOf(
    // ------------------------------------------------------------ Cercles
    FigureSpec("apollonius_pencils", "Faisceaux d’Apollonius", "Cercles", { _ ->
        "Deux familles de cercles autour de deux points A et B. Les uns passent par A et B. Les autres, coloriés, sont les cercles d’Apollonius : l’ensemble des points P " +
            "tels que PA / PB = k, pour k fixé, est un cercle centré sur la droite (AB). Chaque cercle d’une famille coupe ceux de l’autre à angle droit, comme les lignes " +
            "de champ et les équipotentielles de deux charges opposées."
    }) { apolloniusPencils(it) },
    FigureSpec("planetary_gears", "Train planétaire", "Cercles", { v ->
        val (zs, zp) = gearTeeth(v)
        val zr = zs + 2 * zp
        val g = gcd(zs, zs + zr)
        "Un soleil de $zs dents, trois satellites de $zp dents et une couronne de $zr dents à denture intérieure, soit $zs + 2 × $zp. Deux roues engrènent si leurs dents ont " +
            "le même pas ; trois satellites également espacés exigent que $zs + $zr soit divisible par 3. Couronne fixe, le porte-satellites tourne ${zs / g} fois quand " +
            "le soleil en fait ${(zs + zr) / g} (formule de Willis)."
    }) { planetaryGears(it) },
    FigureSpec("bubbles", "Bulles", "Cercles", { v ->
        val c = String.format(java.util.Locale.FRANCE, "%.2f", bubbleExponent(v))
        "Les bulles sont semées une à une, à des places tirées au hasard, en refusant celles qui en chevaucheraient une autre. La n-ième a une aire proportionnelle à " +
            "1/(n + 2)^$c : une série qui converge vers la surface à remplir. John Shier a observé en 2011 qu’avec de tels exposants, il reste toujours de la place " +
            "pour la suivante : les bulles finissent par tout couvrir. C’est la « géométrie statistique »."
    }) { seededBubbles(it) },
    // ------------------------------------------------------------ Nombres
    FigureSpec("sandpile", "Tas de sable abélien", "Nombres", { v ->
        val start = if (v % 2 == 0) "On verse 16 384 grains sur une seule case" else "On verse 8 192 grains sur chacune de deux cases"
        "$start d’une grille. Dès qu’une case en porte 4 ou plus, elle s’éboule et en donne un à chacune de ses quatre voisines. Quand tout s’est arrêté, chaque case " +
            "garde 0, 1, 2 ou 3 grains : quatre couleurs. L’ordre des éboulements ne change rien au résultat, d’où « abélien » (Bak, Tang et Wiesenfeld, 1987 ; " +
            "Dhar, 1990), et le motif, fait de plages régulières, est fractal."
    }) { sandpile(it) },
    FigureSpec("gcd_quilt", "Courtepointe des PGCD", "Nombres", { v ->
        val range = if (v % 2 == 0) "a de −18 à 18 et b de −12 à 12, l’origine au centre" else "a de 1 à 37 et b de 1 à 25, l’origine en bas à gauche"
        "La case (a, b), avec $range, est coloriée selon le plus grand diviseur commun de a et b, plus foncée quand il est grand. Les cases blanches pointées sont " +
            "les couples premiers entre eux : vus depuis l’origine, aucun autre point du réseau ne les cache. Sur un carré de plus en plus grand, leur proportion " +
            "tend vers 6/π² ≈ 60,8 %."
    }) { gcdQuilt(it) },
    FigureSpec("sacks_spiral", "Spirale de Sacks", "Nombres", { v ->
        val colors = if (v % 2 == 0) {
            "en rouge ceux de la forme 4k + 1 (sommes de deux carrés, selon Fermat), en bleu ceux de la forme 4k + 3"
        } else {
            "en rouge ceux de la forme k² + k + 41 : Euler a remarqué que ce polynôme donne 40 premiers de suite"
        }
        "Les entiers s’enroulent sur une spirale d’Archimède : n est à la distance √n du centre, et chaque tour s’achève sur un carré parfait, tous alignés à droite. " +
            "Les 1 229 nombres premiers jusqu’à 10 000 sont pointés, $colors. Ils se rangent le long de courbes. Robert Sacks, 1994."
    }) { sacksSpiral(it) },
    FigureSpec("catalan", "Nombres de Catalan", "Nombres", { _ ->
        "Les 14 façons de découper un hexagone en triangles par des diagonales qui ne se croisent pas. Un polygone à n + 2 côtés en a Cₙ = (2n)! / (n! (n + 1)!) : " +
            "1, 2, 5, 14, 42, 132… Ces nombres de Catalan comptent aussi les parenthésages et les chemins de Dyck. Les triangles sont coloriés selon leur nombre de côtés " +
            "pris sur l’hexagone : deux, un ou aucun."
    }) { hexTriangulations(it) },
    // ------------------------------------------------------------ Graphes
    FigureSpec("four_colors", "Carte à quatre couleurs", "Graphes", { v ->
        val (a, b, c) = mapRings(v)
        "Une carte de ${1 + a + b + c} pays : un au centre, puis des anneaux de $a, $b et $c. Quatre couleurs suffisent pour que deux pays qui partagent une frontière " +
            "diffèrent ; ici il en faut bien quatre, car le pays central touche un anneau de 5. Conjecturé en 1852 par Francis Guthrie, démontré en 1976 par Appel et " +
            "Haken avec un ordinateur, puis vérifié en Coq par Gonthier en 2005."
    }) { fourColorMap(it) },
    FigureSpec("knight_tour", "Tour du cavalier", "Graphes", { v ->
        val start = KNIGHT_STARTS[v % KNIGHT_STARTS.size]
        val square = "${'a' + start % 8}${8 - start / 8}"
        "Le cavalier passe une fois et une seule sur chacune des 64 cases, puis revient d’un bond à son départ, en $square : un tour fermé, brodé ici d’un fil " +
            "qui change de couleur. Il suit la règle de Warnsdorff (1823) : sauter toujours vers la case d’où il aura le moins de sorties libres. " +
            "L’échiquier compte 13 267 364 410 532 tours fermés (McKay, 1997)."
    }) { knightTour(it) },
)

private val FULL_TURN = 2 * PI.toFloat()

private fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)

private fun rect(x: Float, y: Float, w: Float, h: Float = w) = listOf(pt(x, y), pt(x + w, y), pt(x + w, y + h), pt(x, y + h))

/** Dessine [block] découpé dans le rectangle [x0, x1] × [y0, y1] de la figure. */
private inline fun Pencil.clipToRect(x0: Float, y0: Float, x1: Float, y1: Float, crossinline block: () -> Unit) =
    scope.clipRect(ox + x0 * k, oy + y0 * k, ox + x1 * k, oy + y1 * k) { block() }

/** Dégradé le long de [colors], pour t de 0 à 1. */
private fun gradient(colors: List<Color>, t: Float): Color {
    val x = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = floor(x).toInt().coerceAtMost(colors.size - 2)
    return lerp(colors[i], colors[i + 1], x - i)
}

// ------------------------------------------------------------ Cercles

/**
 * Faisceaux d'Apollonius : les cercles PA / PB = k (coloriés, emboîtés autour de A et de B) et les
 * cercles passant par A et B (au crayon), orthogonaux aux premiers.
 */
private fun Pencil.apolloniusPencils(v: Int) {
    val a = if (v % 2 == 0) 60f else 50f
    val o = center
    val warm = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune)
    val cool = listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise)
    val (left, right) = if (v % 2 == 0) warm to cool else listOf(Crayons.violet, Crayons.rose, Crayons.jaune) to listOf(Crayons.sapin, Crayons.vert, Crayons.turquoise)
    clipToRect(6f, 6f, FW - 6f, FH - 6f) {
        for (side in listOf(-1, 1)) for (m in 1..8) {
            val s = 0.3f * m
            val r = a / sinh(s)
            val cx = o.x + side * a * cosh(s) / sinh(s)
            val pal = if (side < 0) left else right
            fill(
                circle(cx, o.y, r, (r * 1.1f).toInt().coerceIn(30, 260)),
                gradient(pal, (m - 1) / 7f),
                hatch = r > 9f,
                gap = (r / 6).coerceIn(3f, 5.5f),
                angle = side * (40f + 12f * m),
                wash = 0.2f,
                strokeAlpha = 0.55f,
                lw = 1f,
            )
        }
        for (j in 1..11) {
            val phi = PI.toFloat() * j / 12
            val r = a / sin(phi)
            line(circle(o.x, o.y + a * cos(phi) / sin(phi), r, (r * 1.1f).toInt().coerceIn(40, 300)), Graphite, 0.9f, 0.6f)
        }
        line(listOf(pt(0f, o.y), pt(FW, o.y)), Graphite, 0.9f, 0.6f)
        line(listOf(pt(o.x, 0f), pt(o.x, FH)), Graphite, 0.9f, 0.6f)
    }
    outline(rect(6f, 6f, FW - 12f, FH - 12f), lw = 1.5f)
    for (p in listOf(pt(o.x - a, o.y), pt(o.x + a, o.y))) fill(circle(p.x, p.y, 3.6f, 14), Graphite, hatch = false, wash = 0.9f, lw = 0.8f)
}

/** Nombres de dents (soleil, satellite) du train planétaire, selon la variante. */
private fun gearTeeth(v: Int): Pair<Int, Int> = listOf(18 to 12, 24 to 9, 15 to 15, 21 to 12)[v % 4]

/**
 * Le contour d'une roue dentée de [z] dents au module [m] (rayon primitif zm/2), la dent n° 0 centrée
 * dans la direction [phase] ; dents tournées vers le centre si [internal] (couronne).
 */
private fun gearRim(c: Offset, z: Int, m: Float, phase: Float, internal: Boolean = false): List<Offset> {
    val r = z * m / 2
    val tip = if (internal) r - m else r + m
    val root = if (internal) r + 1.25f * m else r - 1.25f * m
    val mid = (tip + root) / 2
    val pitch = FULL_TURN / z
    val profile = listOf(-0.5f to root, -0.3f to root, -0.23f to mid, -0.15f to tip, 0f to tip, 0.15f to tip, 0.23f to mid, 0.3f to root)
    return (0 until z).flatMap { t -> profile.map { (f, rr) -> c + dir(phase + (t + f) * pitch) * rr } }
}

/**
 * Train planétaire : un soleil, trois satellites et une couronne à denture intérieure. Les phases des
 * roues sont calculées pour que chaque dent tombe dans un creux de la roue voisine.
 */
private fun Pencil.planetaryGears(v: Int) {
    val (zs, zp) = gearTeeth(v)
    val zr = zs + 2 * zp
    val m = 112f / (zr / 2f + 3.2f)
    val o = center
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun frac(x: Float) = x - floor(x)
    val sunPhase = rad(7f * v)
    val dist = (zs + zp) * m / 2
    val alphas = List(3) { rad(-90f + 13f * v) + FULL_TURN * it / 3 }
    // Satellite : la somme des décalages (en pas) au point de contact vaut 1/2.
    val planetPhases = alphas.map { al -> al + PI.toFloat() - (0.5f - frac((al - sunPhase) * zs / FULL_TURN)) * FULL_TURN / zp }
    // Couronne : la différence des décalages au contact vaut 1/2.
    val up = (alphas[0] - planetPhases[0]) * zp / FULL_TURN
    val ringPhase = alphas[0] - (up + 0.5f) * FULL_TURN / zr
    // La couronne : un anneau, le bord extérieur puis les dents parcourues à l'envers (un trou).
    val outer = circle(o.x, o.y, zr * m / 2 + 3.2f * m, 200)
    val teeth = gearRim(o, zr, m, ringPhase, internal = true)
    fill(outer + listOf(outer[0]) + teeth.reversed() + listOf(teeth.last(), outer[0]), pal[0], gap = 4.4f, wash = 0.28f, outline = false)
    outline(outer, lw = 1.5f)
    outline(teeth, lw = 1.1f)
    // Le soleil et les satellites.
    fill(gearRim(o, zs, m, sunPhase), pal[2], gap = 3.8f, angle = 30f, lw = 1.1f)
    alphas.forEachIndexed { i, al ->
        val c = o + dir(al) * dist
        fill(gearRim(c, zp, m, planetPhases[i]), pal[(4 + i) % 8], gap = 3.6f, angle = -58f + 40f * i, lw = 1.1f)
        fill(circle(c.x, c.y, m * 1.5f, 18), Crayons.brun, hatch = false, wash = 0.6f, lw = 0.9f)
    }
    // Le porte-satellites, par-dessus.
    val arms = alphas.map { o + dir(it) * dist }
    fill(arms, Graphite, hatch = false, wash = 0.08f, lw = 0.8f, outline = false)
    for (p in arms) line(listOf(o, p), Crayons.brun, 2.2f, 0.75f)
    fill(circle(o.x, o.y, m * 2.2f, 22), Crayons.brun, hatch = false, wash = 0.6f, lw = 1f)
}

/** Exposant de décroissance des aires des bulles, selon la variante. */
private fun bubbleExponent(v: Int) = floatArrayOf(1.3f, 1.24f, 1.36f)[v % 3]

private val bubbleCache = HashMap<Int, List<FloatArray>>()

/**
 * Les bulles (x, y, r) de la variante [v] : la n-ième a une aire A·(n + 3)^−c / ζ(c, 3), où A est 80 %
 * du cadre et ζ la fonction zêta de Hurwitz (la somme de toutes les aires vaut donc A) ; chacune est
 * posée au hasard jusqu'à ne chevaucher aucune autre.
 */
private fun bubbles(v: Int): List<FloatArray> = bubbleCache.getOrPut(v) {
    val c = bubbleExponent(v).toDouble()
    val x0 = 10.0; val y0 = 10.0
    val w = FW - 20.0; val h = FH - 20.0
    val n0 = 3.0
    var zeta = 0.0
    for (i in 0 until 20000) zeta += (n0 + i).pow(-c)
    zeta += (n0 + 20000).pow(1 - c) / (c - 1)
    val rnd = Random(77 + v * 31)
    val out = ArrayList<FloatArray>()
    var i = 0
    while (true) {
        val r = sqrt(w * h * 0.8 * (n0 + i).pow(-c) / zeta / PI) // la bulle n = i + 1 : aire en (n + 2)^−c
        if (r < 1.9) break
        for (attempt in 0 until 3000) {
            val x = x0 + r + rnd.nextDouble() * (w - 2 * r)
            val y = y0 + r + rnd.nextDouble() * (h - 2 * r)
            if (out.all { (it[0] - x) * (it[0] - x) + (it[1] - y) * (it[1] - y) >= (it[2] + r + 1.0) * (it[2] + r + 1.0) }) {
                out += floatArrayOf(x.toFloat(), y.toFloat(), r.toFloat())
                break
            }
        }
        i++
    }
    out
}

/** Bulles : des disques semés au hasard sans chevauchement, de plus en plus petits (géométrie statistique de Shier). */
private fun Pencil.seededBubbles(v: Int) {
    val all = bubbles(v)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val rMax = all.first()[2]
    for (b in all) {
        val (x, y, r) = Triple(b[0], b[1], b[2])
        val level = (ln(rMax / r) / ln(1.6f)).toInt()
        fill(
            circle(x, y, r, (r * 1.4f).toInt().coerceIn(16, 120)),
            pal[level % 8],
            hatch = r > 7f,
            gap = (r / 6).coerceIn(2.6f, 4.6f),
            angle = -58f + level * 23f,
            wash = if (r > 7f) 0.3f else 0.55f,
            lw = if (r > 10f) 1.2f else 0.7f,
        )
    }
}

// ------------------------------------------------------------ Nombres

private val sandCache = HashMap<Int, Array<IntArray>>()

/**
 * Tas de sable stabilisé : 16 384 grains sur une case (ou 8 192 sur chacune de deux), puis des
 * éboulements (une case de 4 grains ou plus en donne un à chaque voisine) jusqu'à ce que tout soit stable.
 */
private fun sandpileGrid(two: Boolean): Array<IntArray> = sandCache.getOrPut(if (two) 1 else 0) {
    val w = if (two) 171 else 113
    val h = if (two) 115 else 113
    val g = Array(h) { IntArray(w) }
    if (two) { g[57][57] = 8192; g[57][113] = 8192 } else g[56][56] = 16384
    var busy = true
    while (busy) {
        busy = false
        for (y in 0 until h) for (x in 0 until w) {
            val n = g[y][x]
            if (n < 4) continue
            val q = n / 4
            g[y][x] = n - 4 * q
            if (x > 0) g[y][x - 1] += q
            if (x < w - 1) g[y][x + 1] += q
            if (y > 0) g[y - 1][x] += q
            if (y < h - 1) g[y + 1][x] += q
            busy = true
        }
    }
    g
}

/**
 * Tas de sable abélien : les cases à 0, 1, 2 ou 3 grains en quatre couleurs (les cases vides du dehors
 * restent blanches), par bandes horizontales de même couleur.
 */
private fun Pencil.sandpile(v: Int) {
    val g = sandpileGrid(v % 2 == 1)
    val h = g.size
    val w = g[0].size
    var x0 = Int.MAX_VALUE; var x1 = -1; var y0 = Int.MAX_VALUE; var y1 = -1
    for (y in 0 until h) for (x in 0 until w) if (g[y][x] > 0) { x0 = min(x0, x); x1 = max(x1, x); y0 = min(y0, y); y1 = max(y1, y) }
    // Une case vide est dedans si le tas l'entoure dans les quatre directions.
    val inside = Array(h) { BooleanArray(w) }
    for (y in 0 until h) {
        val l = (0 until w).firstOrNull { g[y][it] > 0 } ?: continue
        val r = (0 until w).last { g[y][it] > 0 }
        for (x in l..r) inside[y][x] = true
    }
    for (x in 0 until w) {
        val t = (0 until h).firstOrNull { g[it][x] > 0 }
        val b = (0 until h).lastOrNull { g[it][x] > 0 }
        for (y in 0 until h) if (t == null || y < t || y > b!!) inside[y][x] = false
    }
    val cols = x1 - x0 + 1
    val rows = y1 - y0 + 1
    val cell = min((FW - 20) / cols, (FH - 16) / rows)
    val left = center.x - cols * cell / 2
    val top = center.y - rows * cell / 2
    val palettes = listOf(
        listOf(Crayons.marine, Crayons.rouge, Crayons.jaune, Crayons.bleu),
        listOf(Crayons.violet, Crayons.orange, Crayons.jaune, Crayons.rose),
        listOf(Crayons.sapin, Crayons.rose, Crayons.jaune, Crayons.vert),
        listOf(Crayons.brun, Crayons.bleu, Crayons.jaune, Crayons.orange),
    )
    val pal = palettes[(v / 2) % palettes.size]
    val wash = floatArrayOf(0.85f, 0.7f, 0.7f, 0.42f)
    for (y in y0..y1) {
        var x = x0
        while (x <= x1) {
            val n = g[y][x]
            val inn = inside[y][x]
            var end = x
            while (end + 1 <= x1 && g[y][end + 1] == n && inside[y][end + 1] == inn) end++
            if (n > 0 || inn) {
                fill(rect(left + (x - x0) * cell, top + (y - y0) * cell, (end - x + 1) * cell, cell), pal[n], hatch = false, wash = wash[n], outline = false)
            }
            x = end + 1
        }
    }
}

/** Courtepointe des PGCD : la case (a, b) coloriée selon pgcd(a, b) ; les couples premiers entre eux, pointés. */
private fun Pencil.gcdQuilt(v: Int) {
    val centered = v % 2 == 0
    val cols = 37
    val rows = 25
    val cell = min((FW - 14) / cols, (FH - 14) / rows)
    val x0 = center.x - cols * cell / 2
    val y0 = center.y - rows * cell / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (i in 0 until cols) for (j in 0 until rows) {
        val a = if (centered) i - cols / 2 else i + 1
        val b = if (centered) rows / 2 - j else rows - j
        val g = gcd(abs(a), abs(b))
        val x = x0 + i * cell
        val y = y0 + j * cell
        when (g) {
            0 -> fill(rect(x + 0.5f, y + 0.5f, cell - 1f), Graphite, hatch = false, wash = 0.85f, outline = false)
            1 -> dot(pt(x + cell / 2, y + cell / 2), 0.9f, Graphite, 0.5f)
            else -> fill(
                rect(x + 0.4f, y + 0.4f, cell - 0.8f),
                pal[(g - 2) % 8],
                hatch = false,
                wash = (0.4f + 0.16f * ln(g.toFloat())).coerceAtMost(0.9f),
                outline = false,
            )
        }
    }
    outline(rect(x0, y0, cols * cell, rows * cell), lw = 1.2f)
}

/** Spirale de Sacks : n à la distance √n du centre et à l'angle 2π√n ; les nombres premiers pointés. */
private fun Pencil.sacksSpiral(v: Int) {
    val n = 10000
    val big = 118f
    val s = big / sqrt(n.toFloat())
    val sieve = BooleanArray(n + 1) { it >= 2 }
    for (i in 2..100) if (sieve[i]) { var j = i * i; while (j <= n) { sieve[j] = false; j += i } }
    val euler = HashSet<Int>()
    var e = 0
    while (e * e + e + 41 <= n) { euler += e * e + e + 41; e++ }
    val disk = circle(center.x, center.y, big + 4, 200)
    fill(disk, if (v % 2 == 0) Crayons.jaune else Crayons.turquoise, hatch = false, wash = 0.12f, outline = false)
    line(listOf(center, pt(center.x + big + 4, center.y)), Graphite, 0.7f, 0.4f)
    val warm = Crayons.rouge
    val cool = if (v % 2 == 0) Crayons.marine else Crayons.violet
    for (p in 2..n) {
        if (!sieve[p]) continue
        val r = s * sqrt(p.toFloat())
        val th = FULL_TURN * sqrt(p.toFloat())
        val c = pt(center.x + r * cos(th), center.y - r * sin(th))
        if (v % 2 == 0) {
            dot(c, 1.25f, if (p % 4 == 1) warm else cool, 0.85f)
        } else if (p in euler) {
            dot(c, 1.8f, warm, 0.95f)
        } else {
            dot(c, 1.1f, cool, 0.6f)
        }
    }
    fill(circle(center.x, center.y, 2.6f, 12), Crayons.jaune, hatch = false, wash = 0.95f, lw = 0.8f)
    outline(disk, lw = 1.4f)
}

/** Nombre de côtés du triangle [t] (sommets de l'hexagone 0 à 5) pris sur l'hexagone. */
private fun hexSides(t: IntArray) = listOf(t[0] to t[1], t[1] to t[2], t[0] to t[2]).count { (a, b) -> abs(a - b) == 1 || abs(a - b) == 5 }

/** Les 14 triangulations de l'hexagone, chacune une liste de 4 triangles, rangées pour la planche. */
private val hexagonTriangulations: List<List<IntArray>> by lazy {
    fun tri(i: Int, j: Int): List<List<IntArray>> {
        if (j - i < 2) return listOf(emptyList())
        val out = ArrayList<List<IntArray>>()
        for (m in i + 1 until j) for (a in tri(i, m)) for (b in tri(m, j)) out += a + b + listOf(intArrayOf(i, m, j))
        return out
    }
    val all = tri(0, 5)
    val central = all.filter { ts -> ts.any { hexSides(it) == 0 } }
    val fans = all.filter { ts -> ts !in central && (0..5).any { s -> ts.all { s in it } } }
    val zigzags = all.filter { it !in central && it !in fans }
    // Rangées de 5, 4 et 5 : zigzags et éventails alternés, les deux à triangle central au milieu.
    listOf(zigzags[0], fans[0], zigzags[1], fans[1], zigzags[2], fans[2], central[0], central[1], fans[3], zigzags[3], fans[4], zigzags[4], fans[5], zigzags[5])
}

/** Nombres de Catalan : les 14 triangulations de l'hexagone, en rangées de 5, 4 et 5. */
private fun Pencil.hexTriangulations(v: Int) {
    val r = 31f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val colors = listOf(pal[5], pal[2], pal[0])
    val turn = if (v % 2 == 0) -90f else 0f
    var index = 0
    listOf(5, 4, 5).forEachIndexed { row, count ->
        for (c in 0 until count) {
            val cx = center.x + (c - (count - 1) / 2f) * 70f
            val cy = center.y + (row - 1) * 76f
            val corners = List(6) { pt(cx, cy) + dir(rad(turn + 60f * it)) * r }
            for (t in hexagonTriangulations[index]) {
                val sides = hexSides(t)
                fill(t.map { corners[it] }, colors[2 - sides], gap = 3.4f, angle = -58f + 40f * sides, wash = 0.32f, lw = 1f)
            }
            outline(corners, lw = 1.4f)
            for (p in corners) dot(p, 1.8f, Graphite)
            index++
        }
    }
}

// ------------------------------------------------------------ Graphes

/**
 * Colorie les sommets (ou les arêtes, vues comme sommets de leur graphe adjacent) avec [colors]
 * couleurs, par retour arrière : [conflicts] donne, pour chaque élément, ceux qui doivent différer.
 */
private fun backtrackColoring(count: Int, colors: Int, conflicts: List<List<Int>>): IntArray? {
    val c = IntArray(count) { -1 }
    fun go(i: Int): Boolean {
        if (i == count) return true
        for (k in 0 until colors) {
            if (conflicts[i].any { c[it] == k }) continue
            c[i] = k
            if (go(i + 1)) return true
        }
        c[i] = -1
        return false
    }
    return if (go(0)) c else null
}

/** Nombres de pays des trois anneaux de la carte, selon la variante. */
private fun mapRings(v: Int): Triple<Int, Int, Int> = listOf(Triple(5, 8, 11), Triple(5, 7, 10), Triple(5, 9, 12))[v % 3]

private val mapCache = HashMap<Int, Pair<List<List<Offset>>, IntArray>>()

/**
 * La carte de la variante [v] : un pays central et trois anneaux de pays, images de rectangles (u, w)
 * par une application lisse du plan (frontières ondulées, rayons tordus) ; deux pays d'anneaux voisins
 * se touchent si leurs secteurs se chevauchent. Puis quatre couleurs, par retour arrière.
 */
private fun fourColorRegions(v: Int): Pair<List<List<Offset>>, IntArray> = mapCache.getOrPut(v % 12) {
    val counts = mapRings(v).toList()
    val rnd = Random(500 + v % 12)
    val ph = List(4) { rnd.nextFloat() * FULL_TURN }
    val offsets = List(3) { rnd.nextFloat() * FULL_TURN }
    fun map(u: Float, w: Float): Offset {
        val base = if (u <= 1f) 30f * u else 30f + (u - 1f) * 26.7f
        val bump = min(u, 1f) * (0.07f * sin(3 * w + ph[0] + 1.1f * u) + 0.04f * sin(5 * w + ph[1] - 0.7f * u))
        val th = w + 0.24f * sin(1.3f * u + ph[2]) + 0.06f * sin(2.9f * u + ph[3])
        val r = base * (1 + bump)
        return pt(FW / 2 + 1.45f * r * cos(th), FH / 2 + r * sin(th))
    }
    val polys = ArrayList<List<Offset>>()
    val sectors = ArrayList<Triple<Int, Float, Float>>()
    polys += List(90) { map(1f, FULL_TURN * it / 90) }
    sectors += Triple(0, 0f, FULL_TURN)
    for (ring in 1..3) {
        val n = counts[ring - 1]
        for (s in 0 until n) {
            val a = offsets[ring - 1] + FULL_TURN * s / n
            val b = a + FULL_TURN / n
            val m = max(6, ((b - a) * 30).toInt())
            val inner = List(m + 1) { map(ring.toFloat(), a + (b - a) * it / m) }
            val right = List(7) { map(ring + it / 6f, b) }
            val outer = List(m + 1) { map(ring + 1f, b - (b - a) * it / m) }
            val left = List(7) { map(ring + 1 - it / 6f, a) }
            polys += inner + right + outer + left
            sectors += Triple(ring, a, b)
        }
    }
    fun overlap(a0: Float, a1: Float, b0: Float, b1: Float) =
        listOf(-FULL_TURN, 0f, FULL_TURN).sumOf { s -> max(0f, min(a1, b1 + s) - max(a0, b0 + s)).toDouble() } > 1e-3
    val conflicts = sectors.indices.map { i ->
        (0 until i).filter { j ->
            val (ri, ai, bi) = sectors[i]
            val (rj, aj, bj) = sectors[j]
            when {
                rj == 0 -> ri == 1
                ri == rj -> abs(bi - aj) < 1e-3 || abs(bj - ai) < 1e-3 || abs(bi - FULL_TURN - aj) < 1e-3 || abs(bj - FULL_TURN - ai) < 1e-3
                abs(ri - rj) == 1 -> overlap(ai, bi, aj, bj)
                else -> false
            }
        }
    }
    polys to backtrackColoring(polys.size, 4, conflicts)!!
}

/** Carte à quatre couleurs : un pays central, trois anneaux de pays aux frontières sinueuses, quatre crayons. */
private fun Pencil.fourColorMap(v: Int) {
    val (polys, colors) = fourColorRegions(v)
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert).rot(v % 4)
    polys.forEachIndexed { i, p ->
        fill(p, pal[colors[i]], gap = 4f, angle = -58f + 31f * i, wash = 0.34f, lw = 1.1f)
    }
    // La côte : le bord extérieur des pays du dernier anneau, repassé plus fort.
    for (p in polys.takeLast(mapRings(v).third)) {
        // Le polygone : bord intérieur (m + 1 points), côté (7), bord extérieur (m + 1), côté (7).
        val m = (p.size - 16) / 2
        outline(p.subList(m + 8, 2 * m + 9), close = false, lw = 2f)
    }
}

/** Cases de départ du tour du cavalier (0 = a8, 63 = h1), selon la variante. */
private val KNIGHT_STARTS = intArrayOf(0, 18, 9, 27, 1, 10, 2, 19)

private val knightCache = HashMap<Int, IntArray>()

/**
 * Un tour fermé du cavalier depuis la case de la variante : règle de Warnsdorff (aller vers la case
 * qui a le moins de sorties libres), les ex æquo tirés au sort ; on recommence jusqu'à ce que le tour
 * passe par les 64 cases et que la dernière soit à un saut de la première, ce qu'on vérifie.
 */
private fun knightPath(v: Int): IntArray = knightCache.getOrPut(v % KNIGHT_STARTS.size) {
    val start = KNIGHT_STARTS[v % KNIGHT_STARTS.size]
    val jumps = listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
    fun next(s: Int) = jumps.mapNotNull { (dx, dy) ->
        val x = s % 8 + dx
        val y = s / 8 + dy
        if (x in 0..7 && y in 0..7) y * 8 + x else null
    }
    fun jump(a: Int, b: Int) = next(a).contains(b)
    var best: IntArray? = null
    for (attempt in 0 until 4000) {
        val rnd = Random(v * 1009 + attempt)
        val seen = BooleanArray(64)
        val path = IntArray(64)
        path[0] = start
        seen[start] = true
        var len = 1
        while (len < 64) {
            val free = next(path[len - 1]).filter { !seen[it] }
            if (free.isEmpty()) break
            val degree = free.map { s -> next(s).count { !seen[it] } }
            val low = degree.min()
            val ties = free.filterIndexed { i, _ -> degree[i] == low }
            val s = ties[rnd.nextInt(ties.size)]
            path[len++] = s
            seen[s] = true
        }
        if (len < 64) continue
        val valid = (1 until 64).all { jump(path[it - 1], path[it]) } && path.toSet().size == 64
        if (!valid) continue
        if (best == null) best = path
        if (jump(path[63], path[0])) return@getOrPut path
    }
    best!!
}

/** Tour du cavalier, brodé : l'échiquier pâle, le fil qui change de couleur le long des 64 sauts, et ses points. */
private fun Pencil.knightTour(v: Int) {
    val path = knightPath(v)
    val cell = 28f
    val x0 = center.x - 4 * cell
    val y0 = center.y - 4 * cell
    fun at(s: Int) = pt(x0 + (s % 8 + 0.5f) * cell, y0 + (s / 8 + 0.5f) * cell)
    for (s in 0 until 64) {
        val dark = (s % 8 + s / 8) % 2 == 1
        fill(rect(x0 + s % 8 * cell, y0 + s / 8 * cell, cell), Crayons.brun, hatch = false, wash = if (dark) 0.16f else 0.04f, outline = false)
    }
    outline(rect(x0, y0, 8 * cell), lw = 1.6f)
    val pal = listOf(
        listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose, Crayons.rouge),
        listOf(Crayons.marine, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.violet, Crayons.marine),
    )[v % 2]
    val dx = abs(path[63] % 8 - path[0] % 8)
    val dy = abs(path[63] / 8 - path[0] / 8)
    val steps = if (dx * dy == 2) 64 else 63
    for (i in 0 until steps) {
        val a = at(path[i])
        val b = at(path[(i + 1) % 64])
        line(listOf(a, b), gradient(pal, i / steps.toFloat()), 2.1f, 0.88f)
    }
    for (s in path) dot(at(s), 2.2f, Graphite, 0.85f)
    val start = at(path[0])
    outline(circle(start.x, start.y, 6f, 18), lw = 1.4f)
}
