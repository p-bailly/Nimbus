package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Figures du thème « Fractales », deuxième série : éponge de Menger, Lyapunov, vallée des hippocampes,
// dragon jumeau, Rauzy et terdragon. Les calculs lourds (itérations) sont faits une fois par variante.

internal val moreFractalFigures: List<FigureSpec> = listOf(
    FigureSpec("menger", "Éponge de Menger", "Fractales", { _ ->
        "On coupe un cube en 27 cubes égaux et on ôte les sept du milieu : celui du centre et ceux du centre des six faces. Il en reste 20, et l’on recommence " +
            "dans chacun : après deux étapes (à droite), 20² = 400 cubes de côté 1/9. Le volume, (20/27)ⁿ, tend vers 0 et la surface devient infinie ; " +
            "chaque face est un tapis de Sierpiński. Dimension fractale : log 20 / log 3 ≈ 2,727 (Karl Menger, 1926)."
    }) { menger(it) },
    FigureSpec("lyapunov", "Fractale de Lyapunov", "Fractales", { v ->
        val l = LYAPUNOV[v % LYAPUNOV.size]
        "On itère x ↦ r x (1 − x) en faisant alterner deux valeurs de r selon le motif ${l.seq} : a, de ${dec(l.a0, 1)} à ${dec(l.a1, 1)} en largeur, et b, de ${dec(l.b0, 1)} à ${dec(l.b1, 1)} en hauteur. " +
            "L’exposant de Lyapunov λ, moyenne de log |r (1 − 2x)|, dit si deux départs voisins s’écartent. λ < 0 : la suite se stabilise (tons chauds, plus sombres " +
            "si elle converge vite) ; λ > 0 : elle est chaotique (bleu). Mario Markus, 1990."
    }) { lyapunov(it) },
    FigureSpec("seahorse", "Vallée des hippocampes", "Fractales", { v ->
        val s = SEAHORSES[v % SEAHORSES.size]
        "Entre la grande cardioïde et le disque de gauche de l’ensemble de Mandelbrot s’ouvre la vallée des hippocampes. On la voit ici autour de c ≈ ${dec(s.cx, 4)} + ${dec(s.cy, 4)} i, " +
            "agrandie ${(3.0 / s.w).roundToInt()} fois : les queues s’enroulent en spirales, faites de spirales plus petites. La couleur suit le logarithme du nombre d’étapes " +
            "de z ↦ z² + c avant que |z| dépasse 2 (${s.iter} au plus) ; en gris, les points de l’ensemble."
    }) { seahorse(it) },
    FigureSpec("twindragon", "Dragon jumeau", "Fractales", { _ ->
        "En base b = −1 + i, avec les seuls chiffres 0 et 1, chaque entier de Gauss m + n i s’écrit d’une seule façon (Walter Penney, 1965). Les nombres « après la virgule », " +
            "Σ dₖ b⁻ᵏ, forment le dragon jumeau ; ses copies décalées des entiers de Gauss pavent le plan sans trou ni chevauchement. Chaque tuile est faite de deux copies " +
            "d’elle-même, réduites de √2 et tournées de 135° (les deux nuances) ; son bord a pour dimension ≈ 1,524."
    }) { twindragon(it) },
    FigureSpec("rauzy", "Fractale de Rauzy", "Fractales", { v ->
        val pieces = if (v % 2 == 0) "les trois morceaux, coloriés, sont des copies du tout réduites de |α|, |α|² et |α|³" else
            "chaque morceau s’y redécoupe en copies plus petites : neuf couleurs pour deux étapes"
        "Les points Σ εₖ αᵏ, où chaque εₖ vaut 0 ou 1 sans trois 1 de suite et où α ≈ −0,4196 + 0,6063 i est une racine complexe de x³ = x² + x + 1. " +
            "Gérard Rauzy (1982) l’a tirée du mot de Tribonacci (1 → 12, 2 → 13, 3 → 1) ; $pieces. La figure pave le plan par translations, et son bord est fractal."
    }) { rauzy(it) },
    FigureSpec("terdragon", "Terdragon", "Fractales", { v ->
        val count = if (v % 2 == 0) "7 étapes, 3⁷ = 2 187" else "6 étapes, 3⁶ = 729"
        "Chaque segment est remplacé par trois segments √3 fois plus courts qui zigzaguent, tournant de 120° puis de −120°. Après $count segments " +
            "pour chacun des trois terdragons partis du même point, tournés d’un tiers de tour. Comme 3 × (1/√3)² = 1, la dimension vaut 2 : " +
            "à la limite la courbe couvre une surface, en touchant ses propres sommets sans jamais se croiser (Davis et Knuth, 1970)."
    }) { terdragon(it) },
)

// ------------------------------------------------------------ Outils

/** Un nombre à virgule, à la française, avec le vrai signe moins. */
private fun dec(x: Double, digits: Int): String = String.format(Locale.ROOT, "%.${digits}f", x).replace('.', ',').replace('-', '−')

/** La couleur à la position [t] (de 0 à 1) d'un dégradé passant par les couleurs de [pal]. */
private fun ramp(pal: List<Color>, t: Float): Color {
    val x = t.coerceIn(0f, 1f) * (pal.size - 1)
    val i = min(floor(x).toInt(), pal.size - 2)
    return lerp(pal[i], pal[i + 1], x - i)
}

/** Développe un L-système : [n] fois, chaque lettre remplacée par sa règle. */
private fun lsystem(axiom: String, rules: Map<Char, String>, n: Int): String {
    var s = axiom
    repeat(n) {
        val b = StringBuilder()
        for (c in s) b.append(rules[c] ?: c)
        s = b.toString()
    }
    return s
}

/** La tortue : les lettres de [draw] avancent d'un pas, « + » et « − » tournent de [angle] degrés. */
private fun turtle(cmds: String, angle: Float, draw: Set<Char>, start: Float = 0f): List<Offset> {
    var a = rad(start)
    var p = Offset.Zero
    val out = mutableListOf(p)
    for (c in cmds) {
        when (c) {
            '+' -> a += rad(angle)
            '-' -> a -= rad(angle)
            in draw -> {
                p += dir(a)
                out += p
            }
        }
    }
    return out
}

/** La case ([col], [row]) d'une grille de [cols] × [lines] cases de [cell] unités, centrée dans le cadre. */
private fun Pencil.cellAt(col: Int, row: Int, cols: Int, lines: Int, cell: Float, color: Color, alpha: Float) =
    scope.drawRect(
        color,
        topLeft = px(pt((FW - cols * cell) / 2 + col * cell + j(0.3f), (FH - lines * cell) / 2 + row * cell + j(0.3f))),
        size = Size(cell * k * 1.05f, cell * k * 1.05f),
        alpha = alpha,
    )

/** Le cadre au crayon d'une grille de [cols] × [lines] cases de [cell] unités, centrée. */
private fun Pencil.gridFrame(cols: Int, lines: Int, cell: Float) {
    val l = (FW - cols * cell) / 2
    val t = (FH - lines * cell) / 2
    outline(listOf(pt(l, t), pt(l + cols * cell, t), pt(l + cols * cell, t + lines * cell), pt(l, t + lines * cell)), lw = 1.5f)
}

/** Cadre de points ([xs], [ys]) dans la case, [trim] des points extrêmes écartés de chaque côté. */
private class CloudFrame(xs: FloatArray, ys: FloatArray, margin: Float, trim: Float) {
    val s: Float
    private val ox: Float
    private val oy: Float

    init {
        val sx = xs.copyOf().also { it.sort() }
        val sy = ys.copyOf().also { it.sort() }
        val cut = (xs.size * trim).toInt()
        val x0 = sx[cut]; val x1 = sx[xs.size - 1 - cut]
        val y0 = sy[cut]; val y1 = sy[ys.size - 1 - cut]
        s = min((FW - 2 * margin) / (x1 - x0), (FH - 2 * margin) / (y1 - y0))
        ox = (FW - (x1 - x0) * s) / 2 - x0 * s
        oy = (FH - (y1 - y0) * s) / 2 - y0 * s
    }

    fun x(v: Float) = v * s + ox
    fun y(v: Float) = v * s + oy
}

/** Un nuage de points mis en cases de [cell] unités : passages, teinte cumulée et dernière étiquette par case. */
private class Cloud(val cell: Float) {
    val cols = (FW / cell).toInt()
    val lines = (FH / cell).toInt()
    val count = IntArray(cols * lines)
    val tint = FloatArray(cols * lines)
    val label = IntArray(cols * lines) { -1 }
    var max = 1

    /** Un point (en unités de la case) de teinte [t] et d'étiquette [tag]. */
    fun add(x: Float, y: Float, t: Float, tag: Int = 0) {
        val c = floor((x - (FW - cols * cell) / 2) / cell).toInt()
        val r = floor((y - (FH - lines * cell) / 2) / cell).toInt()
        if (c < 0 || c >= cols || r < 0 || r >= lines) return
        val i = r * cols + c
        count[i]++
        tint[i] += t
        label[i] = tag
        if (count[i] > max) max = count[i]
    }

    /** Remplit le nuage avec les points ([xs], [ys]) cadrés, de teintes [ts] et d'étiquettes [tags]. */
    fun fill(xs: FloatArray, ys: FloatArray, ts: FloatArray?, tags: IntArray?, margin: Float, trim: Float): Cloud {
        val f = CloudFrame(xs, ys, margin, trim)
        for (i in xs.indices) add(f.x(xs[i]), f.y(ys[i]), ts?.get(i) ?: 0f, tags?.get(i) ?: 0)
        return this
    }
}

// ------------------------------------------------------------ Fractales en relief

/**
 * Éponge de Menger en perspective isométrique, niveaux 1 et 2 : les petits cubes sont dessinés du plus
 * lointain au plus proche (algorithme du peintre), sans leurs faces collées à un voisin. Dessus pâle,
 * côtés hachurés ; les faces des tunnels sont plus sombres.
 */
private fun Pencil.menger(v: Int) {
    val sets = listOf(
        listOf(Crayons.jaune, Crayons.orange, Crayons.rouge),
        listOf(Crayons.turquoise, Crayons.bleu, Crayons.marine),
        listOf(Crayons.rose, Crayons.violet, Crayons.marine),
        listOf(Crayons.jaune, Crayons.vert, Crayons.sapin),
    )
    val pal = sets[v % sets.size]
    mengerCube(1, pt(80f, 136f), 64f, pal, 0.9f)
    mengerCube(2, pt(252f, 125f), 112f, pal, 0.6f)
}

/** Un cube de Menger de niveau [n], de côté [side] unités, centré en [o] (vue isométrique). */
private fun Pencil.mengerCube(n: Int, o: Offset, side: Float, pal: List<Color>, lw: Float) {
    var m = 1
    repeat(n) { m *= 3 }
    val u = side / m
    fun inside(x: Int, y: Int, z: Int): Boolean {
        if (x < 0 || y < 0 || z < 0 || x >= m || y >= m || z >= m) return false
        var a = x; var b = y; var c = z
        repeat(n) {
            if ((if (a % 3 == 1) 1 else 0) + (if (b % 3 == 1) 1 else 0) + (if (c % 3 == 1) 1 else 0) >= 2) return false
            a /= 3; b /= 3; c /= 3
        }
        return true
    }
    fun iso(x: Int, y: Int, z: Int) = o + Offset((x - y) * 0.866f * u, ((x + y) * 0.5f - z) * u)
    fun face(pts: List<Offset>, kind: Int, outer: Boolean) {
        scope.drawPath(path(pts), PaperColors.Paper)
        val c = if (outer) pal[kind] else lerp(pal[kind], Graphite, 0.35f)
        when (kind) {
            0 -> fill(pts, c, hatch = false, wash = 0.3f, lw = lw)
            1 -> fill(pts, c, angle = 30f, gap = 2.8f, wash = 0.42f, lw = lw, strokeAlpha = 0.7f)
            else -> fill(pts, c, angle = -62f, gap = 2.4f, wash = 0.58f, lw = lw, strokeAlpha = 0.8f)
        }
    }
    val cells = ArrayList<IntArray>()
    for (x in 0 until m) for (y in 0 until m) for (z in 0 until m) if (inside(x, y, z)) cells += intArrayOf(x, y, z)
    cells.sortBy { it[0] + it[1] + it[2] }
    for ((x, y, z) in cells) {
        if (!inside(x, y, z + 1)) face(listOf(iso(x, y, z + 1), iso(x + 1, y, z + 1), iso(x + 1, y + 1, z + 1), iso(x, y + 1, z + 1)), 0, z + 1 == m)
        if (!inside(x + 1, y, z)) face(listOf(iso(x + 1, y, z), iso(x + 1, y + 1, z), iso(x + 1, y + 1, z + 1), iso(x + 1, y, z + 1)), 1, x + 1 == m)
        if (!inside(x, y + 1, z)) face(listOf(iso(x, y + 1, z), iso(x + 1, y + 1, z), iso(x + 1, y + 1, z + 1), iso(x, y + 1, z + 1)), 2, y + 1 == m)
    }
}

// ------------------------------------------------------------ Nuages de points

private val rauzyCache = HashMap<Int, Cloud>()

/**
 * Fractale de Rauzy : les sommes Σ εₖ αᵏ (εₖ = 0 ou 1, jamais trois 1 de suite) pour 18 chiffres, mises en
 * cases ; chaque case prend la couleur de son morceau (premier bloc 0, 10 ou 110, ou les deux premiers
 * blocs), et un trait de crayon marque les frontières.
 */
private fun Pencil.rauzy(v: Int) {
    val fine = v % 2 == 1
    val cloud = rauzyCache.getOrPut(v % 2) {
        val ar = -0.41964337760708; val ai = 0.60629072920719 * (if (fine) -1 else 1)
        val len = 18
        val pr = DoubleArray(len); val pi = DoubleArray(len)
        pr[0] = 1.0
        for (i in 1 until len) { pr[i] = pr[i - 1] * ar - pi[i - 1] * ai; pi[i] = pr[i - 1] * ai + pi[i - 1] * ar }
        val xs = ArrayList<Float>(70000); val ys = ArrayList<Float>(70000); val tags = ArrayList<Int>(70000)
        val digits = IntArray(len)
        // Le bloc qui commence au rang i : 0, 10 ou 110 ; son numéro et sa longueur.
        fun block(i: Int): Pair<Int, Int> = when {
            i >= len || digits[i] == 0 -> 0 to 1
            i + 1 >= len || digits[i + 1] == 0 -> 1 to 2
            else -> 2 to 3
        }
        fun go(i: Int, x: Double, y: Double, run: Int) {
            if (i == len) {
                // Les trois premiers blocs : le morceau, son sous-morceau, et le suivant.
                val (b1, l1) = block(0)
                val (b2, l2) = block(l1)
                val tag = b1 * 9 + b2 * 3 + block(l1 + l2).first
                xs += x.toFloat(); ys += y.toFloat(); tags += tag
                return
            }
            digits[i] = 0
            go(i + 1, x, y, 0)
            if (run < 2) {
                digits[i] = 1
                go(i + 1, x + pr[i], y + pi[i], run + 1)
            }
        }
        go(0, 0.0, 0.0, 0)
        Cloud(2f).fill(xs.toFloatArray(), ys.toFloatArray(), null, tags.toIntArray(), 8f, 0f)
    }
    val pal = if (fine) {
        listOf(Crayons.rouge, Crayons.orange, Crayons.rose, Crayons.jaune, Crayons.vert, Crayons.sapin, Crayons.bleu, Crayons.turquoise, Crayons.violet)
    } else {
        listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange).rot((v / 2) % 6).take(3)
    }
    for (r in 0 until cloud.lines) for (c in 0 until cloud.cols) {
        val i = r * cloud.cols + c
        val label = cloud.label[i]
        if (label < 0) continue
        // La couleur dit le morceau (ou le sous-morceau), la nuance l'étape suivante.
        val tag = if (fine) label / 3 else label / 9
        val shade = if (fine) label % 3 else label / 3 % 3
        // Une case au bord de son morceau (un voisin vide ou d'un autre morceau) est repassée au crayon.
        var edge = false
        for ((dc, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nc = c + dc; val nr = r + dr
            val other = if (nc < 0 || nr < 0 || nc >= cloud.cols || nr >= cloud.lines) -1 else cloud.label[nr * cloud.cols + nc]
            if (other < 0 || (if (fine) other / 3 else other / 9) != tag) edge = true
        }
        if (edge) cellAt(c, r, cloud.cols, cloud.lines, cloud.cell, lerp(pal[tag], Graphite, 0.6f), 0.75f)
        else cellAt(c, r, cloud.cols, cloud.lines, cloud.cell, pal[tag], 0.32f + 0.17f * shade)
    }
}

// ------------------------------------------------------------ Fractales en cases

/** Une fractale de Lyapunov : motif, et fenêtre (a en largeur, b en hauteur). */
private class Lyapunov(val seq: String, val a0: Double, val a1: Double, val b0: Double, val b1: Double)

private val LYAPUNOV = listOf(
    Lyapunov("AB", 2.5, 4.0, 3.0, 4.0),
    Lyapunov("BBBBBBAAAAAA", 2.5, 3.4, 3.4, 4.0),
    Lyapunov("AABAB", 2.0, 4.0, 2.6, 4.0),
)

private val lyapunovCache = HashMap<Int, FloatArray>()

/** Fractale de Lyapunov : l'exposant λ de chaque case, en tons chauds s'il est négatif, en bleu sinon. */
private fun Pencil.lyapunov(v: Int) {
    val cell = 2f
    val cols = ((FW - 20f) / cell).toInt()
    val lines = ((FH - 18f) / cell).toInt()
    val l = LYAPUNOV[v % LYAPUNOV.size]
    val lam = lyapunovCache.getOrPut(v % LYAPUNOV.size) {
        val out = FloatArray(cols * lines)
        for (row in 0 until lines) for (col in 0 until cols) {
            val a = l.a0 + (l.a1 - l.a0) * (col + 0.5) / cols
            val b = l.b1 - (l.b1 - l.b0) * (row + 0.5) / lines
            var x = 0.5
            var sum = 0.0
            for (n in 0 until 130) {
                val r = if (l.seq[n % l.seq.length] == 'A') a else b
                x = r * x * (1 - x)
                if (n >= 30) sum += ln(abs(r * (1 - 2 * x)) + 1e-12)
            }
            out[row * cols + col] = (sum / 100).toFloat()
        }
        out
    }
    val warm = if (v / LYAPUNOV.size % 2 == 0) listOf(Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.brun) else
        listOf(Crayons.jaune, Crayons.vert, Crayons.sapin, Crayons.brun)
    val cold = if (v / LYAPUNOV.size % 2 == 0) listOf(Crayons.bleu, Crayons.marine) else listOf(Crayons.violet, Crayons.marine)
    for (row in 0 until lines) for (col in 0 until cols) {
        val x = lam[row * cols + col]
        if (x < 0) {
            val t = (-x / 1.2f).coerceIn(0f, 1f)
            cellAt(col, row, cols, lines, cell, ramp(warm, t), 0.18f + 0.6f * t.pow(0.6f))
        } else {
            val t = (x / 0.6f).coerceIn(0f, 1f)
            cellAt(col, row, cols, lines, cell, ramp(cold, t), 0.1f + 0.5f * t)
        }
    }
    gridFrame(cols, lines, cell)
}

/** Une vue de la vallée des hippocampes : centre, largeur et nombre d'étapes au plus. */
private class Seahorse(val cx: Double, val cy: Double, val w: Double, val iter: Int)

private val SEAHORSES = listOf(Seahorse(-0.7436, 0.1318, 0.006, 600), Seahorse(-0.74364, 0.13188, 0.0035, 700))

private val seahorseCache = HashMap<Int, FloatArray>()

/** Vallée des hippocampes : un zoom dans l'ensemble de Mandelbrot, en couleurs selon log(étapes). */
private fun Pencil.seahorse(v: Int) {
    val cell = 2f
    val cols = ((FW - 20f) / cell).toInt()
    val lines = ((FH - 18f) / cell).toInt()
    val s = SEAHORSES[v % SEAHORSES.size]
    val vals = seahorseCache.getOrPut(v % SEAHORSES.size) {
        val out = FloatArray(cols * lines)
        val span = s.w / cols
        for (row in 0 until lines) for (col in 0 until cols) {
            val cr = s.cx + (col - cols / 2 + 0.5) * span
            val ci = s.cy - (row - lines / 2 + 0.5) * span
            var zr = 0.0; var zi = 0.0
            var n = 0
            while (n < s.iter && zr * zr + zi * zi <= 4.0) {
                val t = zr * zr - zi * zi + cr
                zi = 2 * zr * zi + ci
                zr = t
                n++
            }
            // Compte lissé : n + 1 − log₂ log |z|, et −1 pour les points de l'ensemble.
            out[row * cols + col] = if (n >= s.iter) -1f else
                (n + 1 - ln(ln(sqrt(zr * zr + zi * zi))) / ln(2.0)).toFloat().coerceAtLeast(1f)
        }
        out
    }
    var lo = Float.MAX_VALUE
    for (x in vals) if (x > 0 && x < lo) lo = x
    val lmin = ln(lo); val lmax = ln(s.iter.toFloat())
    val base = Crayons.rainbow.rot((v * 3 + 4) % 8)
    val loop = listOf(Crayons.marine) + base.take(6) + Crayons.marine
    for (row in 0 until lines) for (col in 0 until cols) {
        val x = vals[row * cols + col]
        if (x < 0) { cellAt(col, row, cols, lines, cell, Graphite, 0.75f); continue }
        val l = ln(x)
        val u = ((l - lmin) / (lmax - lmin)).coerceIn(0f, 1f)
        cellAt(col, row, cols, lines, cell, ramp(loop, (l * 1.6f) % 1f), 0.2f + 0.65f * u)
    }
    gridFrame(cols, lines, cell)
}

private val twindragonCache = HashMap<Int, Array<IntArray>>()

/**
 * Dragon jumeau : chaque case est écrite en base −1 + i (chiffres 0 et 1) ; sa partie entière (un entier
 * de Gauss) dit à quelle tuile elle appartient, son premier chiffre après la virgule à quelle moitié.
 */
private fun Pencil.twindragon(v: Int) {
    val cell = 2f
    val cols = ((FW - 20f) / cell).toInt()
    val lines = ((FH - 18f) / cell).toInt()
    val shift = v % 2
    val (tiles, halves) = twindragonCache.getOrPut(shift) {
        val tile = IntArray(cols * lines)
        val half = IntArray(cols * lines)
        val n = 12
        val span = 5.2 / cols
        // (−1 + i)¹² = −64 : on multiplie par b¹² et on arrondit à l'entier de Gauss le plus proche.
        for (row in 0 until lines) for (col in 0 until cols) {
            val zr = (col - cols / 2 + 0.5) * span + 0.25 * shift
            val zi = (row - lines / 2 + 0.5) * span + 0.4 * shift
            var a = (-64 * zr).roundToInt()
            var b = (-64 * zi).roundToInt()
            var first = 0
            repeat(n) {
                val d = (a + b) and 1
                a -= d
                // (a + b i) / (−1 + i) = (−a + b)/2 + (−a − b)/2 i
                val na = (-a + b) / 2
                val nb = (-a - b) / 2
                a = na; b = nb
                first = d
            }
            tile[row * cols + col] = (a + 512) * 1024 + (b + 512)
            half[row * cols + col] = first
        }
        arrayOf(tile, half)
    }
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.turquoise, Crayons.violet, Crayons.orange, Crayons.vert, Crayons.rose, Crayons.bleu)
        .rot((v / 2 * 3) % 8).take(5)
    for (row in 0 until lines) for (col in 0 until cols) {
        val i = row * cols + col
        val t = tiles[i]
        val edge = (col + 1 < cols && tiles[i + 1] != t) || (row + 1 < lines && tiles[i + cols] != t)
        if (edge) { cellAt(col, row, cols, lines, cell, Graphite, 0.62f); continue }
        val a = t / 1024 - 512
        val b = t % 1024 - 512
        val color = pal[Math.floorMod(a + 2 * b, 5)]
        val home = a == 0 && b == 0
        cellAt(col, row, cols, lines, cell, color, (if (halves[i] == 0) 0.5f else 0.28f) + (if (home) 0.25f else 0f))
    }
    gridFrame(cols, lines, cell)
}

// ------------------------------------------------------------ Courbes et figures au crayon

/** Terdragon : trois courbes parties du même point, tournées d'un tiers de tour, chacune sa couleur. */
private fun Pencil.terdragon(v: Int) {
    val fine = v % 2 == 0
    val cmds = lsystem("F", mapOf('F' to "F+F-F"), if (fine) 7 else 6)
    val curves = (0 until 3).map { turtle(cmds, 120f, setOf('F'), 90f + 120f * it) }
    val fit = Fit(curves, 10f)
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.jaune, Crayons.vert, Crayons.violet, Crayons.orange).rot((v * 5) % 6)
    curves.forEachIndexed { i, c -> line(fit(c), pal[i], if (fine) 0.9f else 1.3f, 0.9f) }
    val o = fit(Offset.Zero)
    fill(circle(o.x, o.y, 4f, 14), Crayons.jaune, hatch = false, wash = 0.9f, lw = 1f)
}

