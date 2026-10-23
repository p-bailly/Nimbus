package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.random.Random

// Figures du thème « Courbes », deuxième série : courbes de la physique (pendules, ondes, champs,
// vagues), courbes de dessinateurs (guilloché, kolam, Bézier) et belles familles de courbes.

private const val TURN = 2 * PI.toFloat()
private const val HALF_TURN = PI.toFloat()

// ------------------------------------------------------------ Outils

/** Points d'une courbe paramétrée, de [t0] à [t1] en [n] pas. */
private fun sweep(t0: Float, t1: Float, n: Int, f: (Float) -> Offset): List<Offset> =
    (0..n).map { f(t0 + (t1 - t0) * it / n) }

/** Une couleur foncée au crayon de papier, pour les traits. */
private fun deep(c: Color): Color = lerp(c, Graphite, 0.3f)

/** Un dégradé de [n] couleurs qui parcourt [colors] une fois, en revenant à la première. */
private fun gradient(colors: List<Color>, n: Int): List<Color> = List(n) { i ->
    val x = i * colors.size / n.toFloat()
    val a = x.toInt() % colors.size
    lerp(colors[a], colors[(a + 1) % colors.size], x - x.toInt())
}

/** Un nombre à la française, avec [d] décimales au plus (1,5 ; 0,75). */
private fun fr(x: Float, d: Int = 2): String {
    var s = "%.${d}f".format(Locale.ROOT, x)
    if (s.contains('.')) s = s.trimEnd('0').trimEnd('.')
    return s.replace('.', ',')
}

/** Trait fin et peu tremblé, d'un seul passage, pour les courbes très denses. */
private fun Pencil.fine(pts: List<Offset>, color: Color, lw: Float, alpha: Float = 1f) = with(scope) {
    drawPath(path(pts, false, 0.15f), color, alpha = alpha, style = Stroke(lw * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Une polyligne coupée en morceaux de couleurs successives ; [dense] : trait fin d'un seul passage. */
private fun Pencil.colorTrace(pts: List<Offset>, colors: List<Color>, lw: Float, alpha: Float = 1f, dense: Boolean = false) {
    val chunk = max(1, pts.size / colors.size)
    for (i in colors.indices) {
        val from = i * chunk
        if (from >= pts.size - 1) break
        val to = if (i == colors.lastIndex) pts.size else min(pts.size, (i + 1) * chunk + 1)
        val part = pts.subList(from, to)
        if (dense) fine(part, colors[i], lw, alpha) else line(part, colors[i], lw, alpha)
    }
}

/**
 * Comme [Pencil.fill], pour une région à plusieurs contours : lavis et hachures suivent la règle
 * pair-impair, si bien que les contours intérieurs font des trous.
 */
private fun Pencil.fillRegion(
    loops: List<List<Offset>>,
    color: Color,
    hatch: Boolean = true,
    angle: Float = -58f,
    gap: Float = 4.2f,
    wash: Float = 0.32f,
    outline: Boolean = true,
    lw: Float = 1.1f,
    strokeAlpha: Float = 0.8f,
) {
    val parts = loops.filter { it.size > 2 }
    if (parts.isEmpty()) return
    val shape = Path()
    for (l in parts) shape.addPath(path(l))
    shape.fillType = PathFillType.EvenOdd
    with(scope) {
        drawPath(shape, color, alpha = wash)
        if (hatch) {
            var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
            for (l in parts) for (o in l) { x0 = min(x0, o.x); y0 = min(y0, o.y); x1 = max(x1, o.x); y1 = max(y1, o.y) }
            val cx = (x0 + x1) / 2; val cy = (y0 + y1) / 2
            val half = hypot(x1 - x0, y1 - y0) / 2 + 4
            val d = dir(rad(angle + j(6f)))
            val n = Offset(-d.y, d.x)
            clipPath(shape) {
                var t = -half
                while (t <= half) {
                    val c = Offset(cx + n.x * t, cy + n.y * t)
                    drawLine(
                        color,
                        px(Offset(c.x - d.x * half + j(2f), c.y - d.y * half + j(2f))),
                        px(Offset(c.x + d.x * half + j(2f), c.y + d.y * half + j(2f))),
                        strokeWidth = (1.05f + random.nextFloat() * 0.6f) * k,
                        cap = StrokeCap.Round,
                        alpha = strokeAlpha * (0.75f + random.nextFloat() * 0.25f),
                    )
                    t += gap + j(1.4f)
                }
            }
        }
    }
    if (outline) for (l in parts) outline(l, true, lw)
}

/** Le polygone [poly] coupé au rectangle [x0, x1] × [y0, y1] (Sutherland–Hodgman). */
private fun clipPoly(poly: List<Offset>, x0: Float = 8f, y0: Float = 8f, x1: Float = FW - 8f, y1: Float = FH - 8f): List<Offset> {
    var out = poly
    for (edge in 0 until 4) {
        if (out.isEmpty()) break
        fun inside(p: Offset) = when (edge) { 0 -> p.x >= x0; 1 -> p.x <= x1; 2 -> p.y >= y0; else -> p.y <= y1 }
        fun cut(a: Offset, b: Offset): Offset {
            val t = when (edge) {
                0 -> (x0 - a.x) / (b.x - a.x)
                1 -> (x1 - a.x) / (b.x - a.x)
                2 -> (y0 - a.y) / (b.y - a.y)
                else -> (y1 - a.y) / (b.y - a.y)
            }
            return a + (b - a) * t
        }
        val res = ArrayList<Offset>()
        var prev = out.last()
        for (cur in out) {
            if (inside(cur)) {
                if (!inside(prev)) res += cut(prev, cur)
                res += cur
            } else if (inside(prev)) {
                res += cut(prev, cur)
            }
            prev = cur
        }
        out = res
    }
    return out
}

/** Les morceaux de la polyligne [pts] qui tiennent dans le rectangle, coupés exactement au bord (Liang–Barsky). */
private fun clipLine(pts: List<Offset>, x0: Float = 8f, y0: Float = 8f, x1: Float = FW - 8f, y1: Float = FH - 8f): List<List<Offset>> {
    val out = ArrayList<List<Offset>>()
    var cur = ArrayList<Offset>()
    for (i in 0 until pts.size - 1) {
        val a = pts[i]
        val b = pts[i + 1]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val p = floatArrayOf(-dx, dx, -dy, dy)
        val q = floatArrayOf(a.x - x0, x1 - a.x, a.y - y0, y1 - a.y)
        var t0 = 0f
        var t1 = 1f
        var ok = true
        for (e in 0..3) {
            if (p[e] == 0f) {
                if (q[e] < 0) ok = false
            } else {
                val r = q[e] / p[e]
                if (p[e] < 0) t0 = max(t0, r) else t1 = min(t1, r)
            }
        }
        if (!ok || t0 > t1) {
            if (cur.size > 1) out += cur
            cur = ArrayList()
            continue
        }
        if (cur.isEmpty()) cur.add(a + (b - a) * t0)
        cur.add(a + (b - a) * t1)
        if (t1 < 1f) {
            if (cur.size > 1) out += cur
            cur = ArrayList()
        }
    }
    if (cur.size > 1) out += cur
    return out
}

/** Une grille [nx] × [ny] de valeurs ; avec [pad], le bord vaut −10⁹, pour que les régions f ≥ c s'y referment. */
private fun grid(nx: Int, ny: Int, pad: Boolean, f: (Int, Int) -> Float): FloatArray = FloatArray(nx * ny) { idx ->
    val i = idx % nx
    val j = idx / nx
    if (pad && (i == 0 || j == 0 || i == nx - 1 || j == ny - 1)) -1e9f else f(i, j)
}

/**
 * Lignes de niveau f = [level] de la grille [g] (nx × ny), par la méthode des carrés qui marchent ; les
 * morceaux sont raccordés en polylignes, fermées si elles se referment. [at] place un point de la grille.
 */
private fun isolines(g: FloatArray, nx: Int, ny: Int, level: Float, at: (Float, Float) -> Offset): List<List<Offset>> {
    fun v(i: Int, j: Int) = g[j * nx + i]
    // Une clé par arête : 2(j·nx + i) pour (i, j)–(i + 1, j), + 1 pour (i, j)–(i, j + 1).
    fun point(key: Int): Offset {
        val c = key / 2
        val i = c % nx
        val j = c / nx
        val i2 = if (key % 2 == 0) i + 1 else i
        val j2 = if (key % 2 == 0) j else j + 1
        val a = v(i, j)
        val b = v(i2, j2)
        val s = ((level - a) / (b - a)).coerceIn(0f, 1f)
        return at(i + (i2 - i) * s, j + (j2 - j) * s)
    }
    val segs = ArrayList<IntArray>()
    for (j in 0 until ny - 1) for (i in 0 until nx - 1) {
        val a = v(i, j) >= level
        val b = v(i + 1, j) >= level
        val c = v(i + 1, j + 1) >= level
        val d = v(i, j + 1) >= level
        val top = 2 * (j * nx + i)
        val right = 2 * (j * nx + i + 1) + 1
        val bottom = 2 * ((j + 1) * nx + i)
        val left = 2 * (j * nx + i) + 1
        val cut = ArrayList<Int>(4)
        if (a != b) cut += top
        if (b != c) cut += right
        if (c != d) cut += bottom
        if (d != a) cut += left
        if (cut.size == 2) {
            segs += intArrayOf(cut[0], cut[1])
        } else if (cut.size == 4) {
            // Col : on isole les coins dont l'état diffère de celui du centre.
            val mid = (v(i, j) + v(i + 1, j) + v(i + 1, j + 1) + v(i, j + 1)) / 4 >= level
            if (b != mid) {
                segs += intArrayOf(top, right); segs += intArrayOf(bottom, left)
            } else {
                segs += intArrayOf(left, top); segs += intArrayOf(right, bottom)
            }
        }
    }
    val at2 = HashMap<Int, MutableList<Int>>()
    segs.forEachIndexed { s, e -> for (key in e) at2.getOrPut(key) { ArrayList(2) } += s }
    val used = BooleanArray(segs.size)
    fun other(s: Int, key: Int) = if (segs[s][0] == key) segs[s][1] else segs[s][0]
    val out = ArrayList<List<Offset>>()
    for (s0 in segs.indices) {
        if (used[s0]) continue
        used[s0] = true
        val keys = arrayListOf(segs[s0][0], segs[s0][1])
        var key = segs[s0][1]
        while (true) {
            val next = at2[key]?.firstOrNull { !used[it] } ?: break
            used[next] = true
            key = other(next, key)
            keys += key
        }
        if (keys.first() != keys.last()) {
            val back = ArrayList<Int>()
            key = segs[s0][0]
            while (true) {
                val next = at2[key]?.firstOrNull { !used[it] } ?: break
                used[next] = true
                key = other(next, key)
                back += key
            }
            keys.addAll(0, back.reversed())
        }
        out += keys.map { point(it) }
    }
    return out
}

/** Une ligne fermée de segments, arrondie : un arc de parabole d'un milieu de segment au suivant. */
private fun rounded(loop: List<Offset>, n: Int = 6): List<Offset> {
    val m = loop.size
    val out = ArrayList<Offset>(m * n + 1)
    for (i in 0 until m) {
        val c = loop[(i + 1) % m]
        val p0 = (loop[i] + c) / 2f
        val p1 = (c + loop[(i + 2) % m]) / 2f
        for (s in 0 until n) {
            val t = s / n.toFloat()
            out += p0 * ((1 - t) * (1 - t)) + c * (2 * t * (1 - t)) + p1 * (t * t)
        }
    }
    out += out.first()
    return out
}

// ------------------------------------------------------------ Paramètres des variantes

private object MoreCurves {
    /** Table de Lissajous : le déphasage δ, en fractions de π. */
    fun lissajousPhase(v: Int): Float = floatArrayOf(0.15f, 0.35f, 0.1f, 0.4f)[v % 4]

    /** Harmonographe : f₁…f₄, φ₁…φ₄, A₁…A₄, amortissement ; et le rapport des fréquences. */
    val harmonographs = listOf(
        floatArrayOf(3.001f, 2f, 3f, 2f, 0f, 0f, HALF_TURN / 2, 3 * HALF_TURN / 2, 1f, 1f, 1f, 1f, 0.012f) to "3:2",
        floatArrayOf(2f, 1.003f, 2.002f, 1f, HALF_TURN / 2, 0f, 0f, HALF_TURN / 2, 1f, 0.8f, 1f, 0.8f, 0.01f) to "2:1",
        floatArrayOf(1.002f, 3f, 1f, 2.997f, 0f, HALF_TURN / 4, HALF_TURN / 2, 0f, 1f, 0.6f, 1f, 0.6f, 0.009f) to "1:3",
        floatArrayOf(3f, 4.004f, 4f, 3.002f, 0f, HALF_TURN / 2, HALF_TURN / 2, 0f, 1f, 0.7f, 1f, 0.7f, 0.011f) to "3:4",
    )

    /** Roue de Farris : fréquences, coefficients (réel, imaginaire), ordre de symétrie, formule. */
    val farris = listOf(
        Triple(intArrayOf(1, 6, -14), floatArrayOf(1f, 0f, 0.5f, 0f, 0f, 1f / 3), 5) to "e^(it) + ½ e^(6it) + (i/3) e^(−14it)",
        Triple(intArrayOf(1, 8, -13), floatArrayOf(1f, 0f, 0.55f, 0f, 0f, 0.4f), 7) to "e^(it) + 0,55 e^(8it) + 0,4i e^(−13it)",
        Triple(intArrayOf(1, -3, 9), floatArrayOf(1f, 0f, 0.6f, 0f, 0f, 0.3f), 4) to "e^(it) + 0,6 e^(−3it) + 0,3i e^(9it)",
        Triple(intArrayOf(1, -5, 13), floatArrayOf(1f, 0f, 0.5f, 0f, 0f, 0.32f), 6) to "e^(it) + ½ e^(−5it) + 0,32i e^(13it)",
    )

    /** Chladni : (n, m, signe) pour la plaque de gauche et celle de droite. */
    fun chladni(v: Int): List<IntArray> = listOf(
        listOf(intArrayOf(3, 7, 1), intArrayOf(5, 8, -1)),
        listOf(intArrayOf(2, 9, -1), intArrayOf(4, 7, 1)),
        listOf(intArrayOf(1, 8, 1), intArrayOf(6, 9, -1)),
    )[v % 3]

    /** Kolam : la grille de points. */
    fun kolamGrid(v: Int): Pair<Int, Int> = listOf(7 to 5, 6 to 4, 8 to 5)[v % 3]

    /** Pendule double : les angles de départ, en degrés. */
    fun pendulum(v: Int): Pair<Int, Int> = listOf(120 to -10, 90 to 170, 150 to 60, 100 to 100)[v % 4]

    /** Lignes de champ : les charges (q, x, y). */
    fun charges(v: Int): List<FloatArray> = when (v % 3) {
        0 -> listOf(floatArrayOf(1f, 124f, 125f), floatArrayOf(-1f, 244f, 125f))
        1 -> listOf(floatArrayOf(1f, 132f, 74f), floatArrayOf(-1f, 236f, 74f), floatArrayOf(-1f, 132f, 176f), floatArrayOf(1f, 236f, 176f))
        else -> listOf(floatArrayOf(2f, 184f, 82f), floatArrayOf(-1f, 116f, 178f), floatArrayOf(-1f, 252f, 178f))
    }

    /** Courbes de niveau : les bosses (x, y, hauteur, σx, σy, angle). */
    fun hills(v: Int): List<FloatArray> = when (v % 3) {
        0 -> listOf(floatArrayOf(126f, 118f, 1f, 52f, 44f, 0.3f), floatArrayOf(254f, 138f, 0.78f, 46f, 40f, -0.4f))
        1 -> listOf(floatArrayOf(200f, 136f, 1f, 70f, 32f, -0.25f), floatArrayOf(106f, 88f, 0.55f, 30f, 28f, 0f))
        else -> listOf(
            floatArrayOf(116f, 150f, 0.85f, 44f, 40f, 0f),
            floatArrayOf(204f, 96f, 1f, 42f, 36f, 0.5f),
            floatArrayOf(278f, 156f, 0.62f, 34f, 40f, 0f),
        )
    }

    /** Poursuite : la vitesse du chien, en fraction de celle du lièvre. */
    fun pursuit(v: Int): Float = floatArrayOf(0.7f, 0.55f, 0.8f)[v % 3]

    /** Interférences : l'écart des sources, en longueurs d'onde. */
    fun sources(v: Int): Float = floatArrayOf(3.2f, 4.4f, 2.3f)[v % 3]

    /** Développée : le rapport b/a de l'ellipse. */
    fun evolute(v: Int): Float = floatArrayOf(0.72f, 0.78f)[v % 2]


    /** Vagues de Gerstner : la cambrure b/a de la surface. */
    fun steepness(v: Int): Float = floatArrayOf(0.8f, 0.95f, 0.6f)[v % 3]

    /** Bézier : la fraction t de la construction mise en valeur. */
    fun casteljau(v: Int): Float = floatArrayOf(0.4f, 0.6f, 0.3f, 0.5f)[v % 4]
}

internal val moreCurveFigures: List<FigureSpec> = listOf(
    FigureSpec("lissajous_table", "Table de Lissajous", "Courbes", { _ ->
        "Dans la case de la colonne a et de la ligne b, la courbe x = sin(at + δ), y = sin(bt), avec a = 1 à 5 et b = 1 à 3 : " +
            "si a et b sont premiers entre eux, elle touche a fois chaque bord vertical et b fois chaque bord horizontal. " +
            "Sur la diagonale, des ellipses ; sinon, c’est la courbe du rapport simplifié. Jules Lissajous les projetait en 1857 " +
            "avec deux diapasons munis de petits miroirs."
    }) { lissajousTable(it) },
    FigureSpec("harmonograph", "Harmonographe", "Courbes", { v ->
        val ratio = MoreCurves.harmonographs[v % MoreCurves.harmonographs.size].second
        "Des pendules oscillent dans deux directions et guident un stylo : x = Σ Aᵢ e^(−dt) sin(fᵢt + φᵢ), et de même pour y. " +
            "Les fréquences sont presque dans le rapport $ratio : ce petit écart fait tourner la figure d’un tour à l’autre, " +
            "pendant que l’amortissement la resserre vers le centre. Le pendule de Blackburn (1844) a lancé la mode des " +
            "harmonographes dans les salons victoriens."
    }) { harmonograph(it) },
    FigureSpec("farris_wheel", "Roue de Farris", "Courbes", { v ->
        val (spec, formula) = MoreCurves.farris[v % MoreCurves.farris.size]
        val f = spec.first
        val freqs = "${f[0]}, ${f[1]} et ${f[2]}".replace("-", "−")
        "Trois roues tournent l’une au bout de l’autre, à des vitesses différentes : z(t) = $formula. Les fréquences $freqs " +
            "ont toutes le même reste modulo ${spec.third} : la courbe a donc une symétrie d’ordre ${spec.third}, invisible dans la formule. " +
            "Frank Farris a décrit ces « courbes mystères » en 1996. Les zones hachurées sont entourées un nombre impair de fois."
    }) { farrisWheel(it) },
    FigureSpec("chladni", "Figures de Chladni", "Courbes", { v ->
        val (l, r) = MoreCurves.chladni(v)
        "Ernst Chladni (1787) saupoudrait de sable une plaque de métal et la faisait chanter avec un archet : le sable fuit les " +
            "parties qui vibrent et s’amasse sur les lignes nodales, immobiles. Sur une plaque carrée, elles suivent à peu près " +
            "cos(nπx) cos(mπy) ± cos(mπx) cos(nπy) = 0 ; ici (n, m) = (${l[0]}, ${l[1]}) à gauche et (${r[0]}, ${r[1]}) à droite. " +
            "Les deux couleurs vibrent en opposition."
    }) { chladni(it) },
    FigureSpec("kolam", "Kolam", "Courbes", { v ->
        val n = kolamLoops(v).size
        val loops = if (n == 1) "Ici une seule ligne fermée." else "Ici $n lignes fermées, chacune sa couleur."
        "Dans le sud de l’Inde, on trace chaque matin devant la maison un kolam à la farine de riz : des lignes fermées qui " +
            "s’enroulent autour d’une grille de points sans jamais les toucher. C’est une courbe miroir : elle file en diagonale " +
            "et rebondit sur le bord de la grille et sur de petits miroirs placés entre les points, ici posés en symétrie. $loops"
    }) { kolam(it) },
    FigureSpec("guilloche", "Rosace de guilloché", "Courbes", { v ->
        val m = intArrayOf(6, 5, 7)[v % 3]
        "Le guilloché, gravé au tour sur les cadrans de montre et les billets de banque : des courbes r = R + a sin(nθ + φ) + b sin(${m}θ), " +
            "répétées en décalant la phase φ d’une fraction de période. Leurs croisements tissent un filet régulier, ici à $m lobes, " +
            "très difficile à imiter : c’est pourquoi il protège les billets depuis le XIXᵉ siècle."
    }) { guilloche(it) },
    FigureSpec("lorenz", "Attracteur de Lorenz", "Courbes", { v ->
        val view = if (v % 2 == 0) "projetée sur le plan (x, z)" else "projetée sur le plan (y, z)"
        "En 1963, Edward Lorenz réduit la convection de l’air à trois équations : x′ = σ(y − x), y′ = x(ρ − z) − y, z′ = xy − βz, " +
            "avec σ = 10, ρ = 28, β = 8/3. La trajectoire, $view, tourne autour de deux points fixes (les points) et saute d’une aile " +
            "à l’autre sans jamais se répéter. Deux départs presque égaux finissent par diverger : c’est l’« effet papillon »."
    }) { lorenz(it) },
    FigureSpec("double_pendulum", "Pendule double", "Courbes", { v ->
        val (a, b) = MoreCurves.pendulum(v)
        "Un pendule accroché au bout d’un autre, de même longueur et même masse, lâché sans vitesse à θ₁ = $a° et θ₂ = ${b.toString().replace("-", "−")}° " +
            "de la verticale. La trace est celle de la masse du bas pendant 30 secondes, calculée pas à pas par la méthode de " +
            "Runge-Kutta. Le mouvement est chaotique : un millième de degré de différence au départ change toute la trajectoire " +
            "au bout de quelques secondes."
    }) { doublePendulum(it) },
    FigureSpec("field_lines", "Lignes de champ", "Courbes", { v ->
        val case = when (v % 3) {
            0 -> "Ici un dipôle, +q et −q."
            1 -> "Ici quatre charges en carré, deux + et deux −."
            else -> "Ici +2q en haut et deux charges −q : chaque ligne issue de +2q finit sur l’une d’elles."
        }
        "Les lignes de champ électrique partent des charges positives et finissent sur les négatives ; le champ " +
            "E = Σ kq·r/r³ leur est tangent en chaque point. Les bandes colorées sont les zones entre deux équipotentielles " +
            "(chaudes pour un potentiel positif, froides pour un négatif), partout perpendiculaires aux lignes de champ. $case"
    }) { fieldLines(it) },
    FigureSpec("contour_map", "Courbes de niveau", "Courbes", { v ->
        val n = MoreCurves.hills(v).size
        val hills = if (n == 2) "deux bosses" else "trois bosses"
        "Une fonction de deux variables, ici une somme de $hills en cloche, vue de dessus comme une carte : chaque courbe de " +
            "niveau relie les points de même altitude f(x, y) = c, de dixième en dixième du maximum, et les bandes entre deux " +
            "niveaux sont coloriées. Là où les courbes se serrent, la pente est forte ; entre deux sommets (les triangles), elles " +
            "dessinent un col, un point selle."
    }) { contourMap(it) },
    FigureSpec("wankel", "Moteur Wankel", "Courbes", { _ ->
        "Dans le moteur de Felix Wankel (1957), un rotor triangulaire tourne dans un carter dont la paroi est une épitrochoïde " +
            "à deux lobes : x = R cos t + e cos 3t, y = R sin t + e sin 3t. Ses trois sommets glissent toujours sur la paroi, " +
            "pendant que son centre décrit un cercle de rayon e, trois fois plus vite que lui. Les trois chambres colorées " +
            "grandissent et rapetissent tour à tour : admission, compression, détente, échappement."
    }) { wankel(it) },
    FigureSpec("pursuit", "Courbe de poursuite", "Courbes", { v ->
        val k = MoreCurves.pursuit(v)
        "Un lièvre court sur un cercle de rayon R ; un chien part du centre et court toujours droit vers lui, à ${fr(k)} fois " +
            "sa vitesse. Il ne le rattrape jamais : sa trajectoire s’enroule vers un cercle de rayon ${fr(k)}R (en pointillé), " +
            "où il finit par tourner au même rythme que le lièvre. Les lignes de visée, en couleur, sont tangentes à sa course. " +
            "Arthur Hathaway a posé ce problème en 1920."
    }) { pursuit(it) },
    FigureSpec("catenary", "Chaînettes", "Courbes", { _ ->
        "Cinq chaînes de longueurs différentes pendent des deux mêmes clous : chacune prend la forme d’une chaînette, " +
            "y = a ch(x/a), et non d’une parabole comme le croyait Galilée. Pour la plus longue, la parabole de mêmes extrémités " +
            "et même point bas est en pointillé, plus pointue. Huygens, Leibniz et Jean Bernoulli trouvent l’équation en 1691 ; " +
            "retournée, la chaînette donne l’arche la plus solide."
    }) { catenary(it) },
    FigureSpec("casteljau", "Courbe de Bézier", "Courbes", { v ->
        val t = fr(MoreCurves.casteljau(v))
        "Quatre points de contrôle fixent une courbe de Bézier. L’algorithme de De Casteljau coupe chaque côté à la même " +
            "fraction t, puis recommence sur les trois points obtenus, puis sur les deux derniers : le segment final touche la " +
            "courbe en son point B(t). Tracés pour t de 0 à 1, ces segments l’enveloppent ; la construction pour t = $t est " +
            "mise en valeur. Inventées chez Citroën et Renault vers 1960, elles dessinent nos polices."
    }) { casteljau(it) },
    FigureSpec("involute", "Développante du cercle", "Courbes", { _ ->
        "Une ficelle enroulée sur une bobine de rayon r se déroule en restant tendue : son bout trace la développante du cercle, " +
            "x = r(cos t + t sin t), y = r(sin t − t cos t). La ficelle est toujours tangente à la bobine et perpendiculaire à la " +
            "courbe, et deux spires voisines sont à 2πr l’une de l’autre. Depuis Euler, les dents d’engrenage ont ce profil : " +
            "elles roulent l’une sur l’autre sans à-coups."
    }) { involute(it) },
    FigureSpec("interference", "Interférences", "Courbes", { v ->
        val d = MoreCurves.sources(v)
        val n = 2 * (d + 0.5f).toInt()
        "Deux sources battent l’eau au même rythme et émettent des ondes circulaires de longueur λ (les anneaux). Là où une " +
            "crête rencontre un creux, l’eau reste immobile : ces lignes nodales, où |MS₁ − MS₂| = (m + ½)λ, sont des hyperboles " +
            "de foyers S₁ et S₂. Ici les sources sont à ${fr(d)}λ l’une de l’autre, d’où $n branches. Thomas Young l’a montré " +
            "pour la lumière en 1801."
    }) { interference(it) },
    FigureSpec("confocal", "Coniques homofocales", "Courbes", { _ ->
        "Les ellipses et les hyperboles qui ont les mêmes foyers (les deux points) se coupent toujours à angle droit : elles " +
            "quadrillent le plan en coordonnées elliptiques, x = c ch μ cos ν, y = c sh μ sin ν. Sur chaque ellipse, μ est constant " +
            "et la somme des distances aux foyers aussi ; sur chaque hyperbole, ν est constant et c’est la différence des " +
            "distances. Les cases sont coloriées en damier."
    }) { confocal(it) },
    FigureSpec("string_modes", "Corde vibrante", "Courbes", { _ ->
        "Une corde fixée à ses deux bouts vibre en ondes stationnaires, y = A sin(nπx/L) cos(2πfₙt), de fréquences fₙ = n f₁ : " +
            "le fondamental puis les harmoniques, n = 1 à 6 de haut en bas. Chaque mode a n − 1 nœuds immobiles (les points) " +
            "et n fuseaux ; les traits fins sont la corde à quelques instants. Les pythagoriciens entendaient déjà que la moitié " +
            "de la corde sonne l’octave."
    }) { stringModes(it) },
    FigureSpec("evolute", "Développée de l’ellipse", "Courbes", { v ->
        val r = MoreCurves.evolute(v)
        "Les normales à une ellipse (les segments colorés, menés de chaque point jusqu’à son centre de courbure) enveloppent " +
            "sa développée, une astroïde étirée : (ax)^(2/3) + (by)^(2/3) = (a² − b²)^(2/3). Chacun de ses points est le centre " +
            "d’un cercle osculateur, celui qui épouse le mieux l’ellipse (en pointillé). Ici b/a = ${fr(r)}. " +
            "Apollonius de Perge étudiait déjà ces normales."
    }) { evolute(it) },
    FigureSpec("gerstner", "Vagues de Gerstner", "Courbes", { v ->
        val s = MoreCurves.steepness(v)
        "Dans le modèle de Gerstner (1802), chaque particule d’eau tourne sur un cercle (en pointillé), plus petit en profondeur. " +
            "La surface est alors une trochoïde, x = aθ − b sin θ, y = b cos θ : creux arrondis, crêtes pointues. Ici b = ${fr(s)}a " +
            "en surface ; quand b atteint a, la crête devient la pointe d’une cycloïde et la vague déferle."
    }) { gerstner(it) },
)

// ------------------------------------------------------------ Les dessins

/** Table de Lissajous : 5 colonnes de fréquences a, 3 lignes de fréquences b, une courbe par case. */
private fun Pencil.lissajousTable(v: Int) {
    val delta = MoreCurves.lissajousPhase(v) * HALF_TURN
    val cols = 5
    val rows = 3
    val cw = 70f
    val ch = 77f
    val x0 = (FW - cols * cw) / 2
    val y0 = (FH - rows * ch) / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (b in 1..rows) for (a in 1..cols) {
        val c = pt(x0 + (a - 0.5f) * cw, y0 + (b - 0.5f) * ch)
        val rx = cw * 0.41f
        val ry = ch * 0.41f
        val pts = sweep(0f, TURN, 360) { t -> c + pt(rx * sin(a * t + delta), -ry * sin(b * t)) }
        val col = pal[(a + 3 * b) % 8]
        fillRegion(listOf(pts), col, hatch = (a + b) % 2 == 0, wash = 0.26f, gap = 3.2f, angle = -58f + 24f * a, outline = false, strokeAlpha = 0.55f)
        line(pts, deep(col), 1.3f, 0.95f)
    }
}

private val harmonoCache = HashMap<Int, List<Offset>>()

/** Harmonographe : la trace amortie de quatre pendules, en dégradé de couleurs. */
private fun Pencil.harmonograph(v: Int) {
    val i = v % MoreCurves.harmonographs.size
    val pts = harmonoCache.getOrPut(i) {
        val h = MoreCurves.harmonographs[i].first
        val d = h[12]
        sweep(0f, 2.3f / d, 7000) { t ->
            val e = exp(-d * t)
            pt(e * (h[8] * sin(h[0] * t + h[4]) + h[9] * sin(h[1] * t + h[5])), -e * (h[10] * sin(h[2] * t + h[6]) + h[11] * sin(h[3] * t + h[7])))
        }
    }
    val fit = Fit(listOf(pts), 12f)
    val grad = gradient(Crayons.rainbow.rot((v * 3) % 8).filter { it != Crayons.jaune }, 42)
    colorTrace(fit(pts), grad, 0.62f, 0.9f, dense = true)
    dot(fit(pts.last()), 2f, Graphite)
}

/** Roue de Farris : la courbe des trois roues, coloriée pair-impair, et son trait en dégradé symétrique. */
private fun Pencil.farrisWheel(v: Int) {
    val (spec, _) = MoreCurves.farris[v % MoreCurves.farris.size]
    val (f, c, n) = spec
    val raw = sweep(0f, TURN, 1800) { t ->
        var x = 0f
        var y = 0f
        for (q in 0..2) {
            val ct = cos(f[q] * t)
            val st = sin(f[q] * t)
            x += c[2 * q] * ct - c[2 * q + 1] * st
            y += c[2 * q] * st + c[2 * q + 1] * ct
        }
        pt(x, -y)
    }
    val fit = Fit(listOf(raw), 10f)
    val pts = fit(raw)
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    fill(pts, pal[5], hatch = false, wash = 0.14f, outline = false)
    fillRegion(listOf(pts), pal[1], wash = 0.3f, gap = 3f, strokeAlpha = 0.6f, outline = false)
    // Le dégradé se répète n fois, comme la courbe.
    val grad = gradient(listOf(pal[0], pal[6], pal[5], pal[3]), 6)
    colorTrace(pts, List(6 * n) { grad[it % 6] }, 1.3f, 0.95f)
}

private val chladniCache = HashMap<Int, List<Pair<List<List<Offset>>, List<List<Offset>>>>>()

/** Figures de Chladni : deux plaques carrées, leurs zones en opposition et le sable sur les lignes nodales. */
private fun Pencil.chladni(v: Int) {
    val side = 170f
    val gapP = 14f
    val y0 = (FH - side) / 2
    val res = 76
    val plates = chladniCache.getOrPut(v % 3) {
        MoreCurves.chladni(v).mapIndexed { p, (n, m, s) ->
            val x0 = (FW - 2 * side - gapP) / 2 + p * (side + gapP)
            fun f(i: Int, j: Int): Float {
                val x = i / res.toFloat() * HALF_TURN
                val y = j / res.toFloat() * HALF_TURN
                return cos(n * x) * cos(m * y) + s * cos(m * x) * cos(n * y)
            }
            val at = { gx: Float, gy: Float -> pt(x0 + gx * side / res, y0 + gy * side / res) }
            val zones = isolines(grid(res + 1, res + 1, true) { i, j -> f(i, j) }, res + 1, res + 1, 0f, at)
            val nodal = isolines(grid(res + 1, res + 1, false) { i, j -> f(i, j) }, res + 1, res + 1, 0f, at)
            zones to nodal
        }
    }
    val pal = listOf(
        Crayons.bleu to Crayons.orange,
        Crayons.violet to Crayons.jaune,
        Crayons.turquoise to Crayons.rose,
        Crayons.marine to Crayons.vert,
    )[v % 4]
    val sand = Random(v * 31 + 7)
    plates.forEachIndexed { p, (zones, nodal) ->
        val x0 = (FW - 2 * side - gapP) / 2 + p * (side + gapP)
        val plate = listOf(pt(x0, y0), pt(x0 + side, y0), pt(x0 + side, y0 + side), pt(x0, y0 + side))
        fill(plate, pal.first, wash = 0.24f, gap = 4.6f, strokeAlpha = 0.45f, outline = false)
        fillRegion(zones, pal.second, wash = 0.36f, gap = 3.4f, angle = 32f, strokeAlpha = 0.6f, outline = false)
        // Le sable : des grains semés le long des lignes nodales.
        for (l in nodal) for (q in 0 until l.size - 1) {
            val a = l[q]
            val b = l[q + 1]
            val grains = max(1, ((b - a).getDistance() / 1.2f).toInt())
            repeat(grains) {
                val t = sand.nextFloat()
                val o = a + (b - a) * t + pt((sand.nextFloat() - 0.5f) * 2.6f, (sand.nextFloat() - 0.5f) * 2.6f)
                dot(o, 0.55f + sand.nextFloat() * 0.45f, if (sand.nextInt(3) == 0) Crayons.brun else Graphite, 0.8f)
            }
        }
        outline(plate, lw = 1.4f)
        dot(pt(x0 + side / 2, y0 + side / 2), 2.2f, Graphite)
    }
}

private val kolamCache = HashMap<Int, List<List<Offset>>>()

/**
 * Le kolam de la variante [v] : ses lignes fermées, en unités de la grille de points. La courbe miroir
 * va d'un milieu à l'autre entre les points, en diagonale ; elle rebondit sur le bord et sur les miroirs.
 */
private fun kolamLoops(v: Int): List<List<Offset>> = kolamCache.getOrPut(v % 32) {
    val (na, nb) = MoreCurves.kolamGrid(v)
    val w = 2 * na
    val h = 2 * nb
    val rnd = Random(v % 32 * 7919 + 11)
    // Les miroirs (0 aucun, 1 horizontal, 2 vertical), tirés sur un quart et reproduits par symétrie.
    val mirror = IntArray((w + 1) * (h + 1))
    for (y in 1 until h) for (x in 1 until w) {
        if ((x + y) % 2 == 0 || x > w / 2 || y > h / 2) continue
        val m = if (rnd.nextFloat() < 0.3f) 1 + rnd.nextInt(2) else 0
        for ((xx, yy) in listOf(x to y, w - x to y, x to h - y, w - x to h - y)) mirror[yy * (w + 1) + xx] = m
    }
    fun key(x: Int, y: Int, dx: Int, dy: Int): Int {
        val x1 = min(x, x + dx)
        val y1 = if (x1 == x) y else y + dy
        return (y1 * (w + 1) + x1) * 2 + if (dx * dy > 0) 1 else 0
    }
    val seen = HashSet<Int>()
    val loops = ArrayList<List<Offset>>()
    for (y in 0..h) for (x in 0..w) {
        if ((x + y) % 2 == 0) continue
        for (dx in intArrayOf(-1, 1)) for (dy in intArrayOf(-1, 1)) {
            if (x + dx !in 0..w || y + dy !in 0..h) continue
            if (!seen.add(key(x, y, dx, dy))) continue
            val pts = ArrayList<Offset>()
            var cx = x
            var cy = y
            var ex = dx
            var ey = dy
            var guard = 0
            while (guard++ < 10000) {
                cx += ex
                cy += ey
                pts += pt((cx - 1) / 2f, (cy - 1) / 2f)
                if (cx == 0 || cx == w) ex = -ex
                if (cy == 0 || cy == h) ey = -ey
                when (mirror[cy * (w + 1) + cx]) {
                    1 -> ey = -ey
                    2 -> ex = -ex
                }
                if (cx == x && cy == y && ex == dx && ey == dy) break
                seen.add(key(cx, cy, ex, ey))
            }
            loops += pts
        }
    }
    loops
}

/** Kolam : la grille de points (pulli) et les lignes miroirs arrondies qui s'enroulent autour. */
private fun Pencil.kolam(v: Int) {
    val (na, nb) = MoreCurves.kolamGrid(v)
    val s = min(330f / na, 228f / nb)
    fun at(o: Offset) = center + pt((o.x - (na - 1) / 2f) * s, (o.y - (nb - 1) / 2f) * s)
    val loops = kolamLoops(v).map { l -> rounded(l).map { at(it) } }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val inks = pal.filter { it != Crayons.jaune }
    fillRegion(loops, pal[2], wash = 0.22f, gap = 3.6f, strokeAlpha = 0.5f, outline = false)
    loops.forEachIndexed { i, l ->
        val c = if (loops.size == 1) inks[0] else inks[i % inks.size]
        line(l, c, 2.4f, 0.95f)
    }
    for (i in 0 until na) for (j in 0 until nb) dot(at(pt(i.toFloat(), j.toFloat())), 2.4f, Graphite)
}

/** Rosace de guilloché : trois anneaux de courbes ondulées, décalées en phase, et une fleur au centre. */
private fun Pencil.guilloche(v: Int) {
    val m = intArrayOf(6, 5, 7)[v % 3]
    val pal = Crayons.rainbow.rot((v * 3 + 5) % 8)
    val sx = 1.45f
    fun at(r: Float, th: Float) = center + pt(sx * r * cos(th), -r * sin(th))
    fun curveR(rr: (Float) -> Float) = sweep(0f, TURN, 720) { th -> at(rr(th), th) }
    // Les fonds : trois bandes ovales au lavis.
    fill(curveR { 114f + 3f * sin(m * it) }, pal[0], hatch = false, wash = 0.12f, lw = 1f)
    fill(curveR { 80f - 3f * sin(m * it) }, pal[2], hatch = false, wash = 0.14f, outline = false)
    fill(curveR { 46f + 3f * sin(m * it) }, pal[4], hatch = false, wash = 0.16f, outline = false)
    // Chaque anneau : rayon, amplitude, fréquence, ondulation lente, nombre de courbes, couleurs.
    val rings = listOf(
        floatArrayOf(98f, 9f, 5f * m, 6f, 12f) to (pal[0] to pal[7]),
        floatArrayOf(63f, 10f, 3f * m, -6f, 12f) to (pal[2] to pal[3]),
        floatArrayOf(31f, 8f, 2f * m, 3f, 9f) to (pal[5] to pal[6]),
    )
    for ((ring, colors) in rings) {
        val count = ring[4].toInt()
        for (q in 0 until count) {
            val phase = TURN * q / count
            val c = lerp(colors.first, colors.second, q / (count - 1f))
            fine(curveR { ring[0] + ring[1] * sin(ring[2] * it + phase) + ring[3] * sin(m * it) }, deep(c), 0.75f, 0.85f)
        }
    }
    val flower = curveR { 13f + 6f * cos(m * it) }
    fill(flower, pal[1], gap = 2.6f, wash = 0.45f, lw = 1f)
    dot(center, 2f, Graphite)
}

/** La trajectoire de l'attracteur de Lorenz (RK4, pas 0,01), calculée une fois. */
private val lorenzTrack: List<FloatArray> by lazy {
    val out = ArrayList<FloatArray>(6000)
    var x = 0.1
    var y = 0.0
    var z = 0.0
    val dt = 0.01
    fun f(x: Double, y: Double, z: Double) = doubleArrayOf(10 * (y - x), x * (28 - z) - y, x * y - 8.0 / 3 * z)
    for (i in 0 until 6400) {
        val k1 = f(x, y, z)
        val k2 = f(x + dt / 2 * k1[0], y + dt / 2 * k1[1], z + dt / 2 * k1[2])
        val k3 = f(x + dt / 2 * k2[0], y + dt / 2 * k2[1], z + dt / 2 * k2[2])
        val k4 = f(x + dt * k3[0], y + dt * k3[1], z + dt * k3[2])
        x += dt / 6 * (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0])
        y += dt / 6 * (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1])
        z += dt / 6 * (k1[2] + 2 * k2[2] + 2 * k3[2] + k4[2])
        if (i >= 400) out += floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())
    }
    out
}

/** Attracteur de Lorenz : la trajectoire projetée, en dégradé, et les deux points fixes. */
private fun Pencil.lorenz(v: Int) {
    fun proj(p: FloatArray): Offset = pt(if (v % 2 == 0) p[0] else p[1], -p[2])
    val pts = lorenzTrack.map { proj(it) }
    val fit = Fit(listOf(pts), 10f)
    val grad = gradient(Crayons.rainbow.rot((v * 3 + 2) % 8).filter { it != Crayons.jaune }, 35)
    colorTrace(fit(pts), grad, 0.55f, 0.9f, dense = true)
    val c = sqrt(8f / 3 * 27)
    for (s in floatArrayOf(-1f, 1f)) {
        val o = fit(proj(floatArrayOf(s * c, s * c, 27f)))
        dot(o, 3.2f, Color.White, 0.8f)
        dot(o, 2.2f, Graphite)
    }
}

private val pendulumCache = HashMap<Int, List<FloatArray>>()

/** Le pendule double de la variante [v] : (θ₁, θ₂) toutes les 6 ms pendant 30 s (RK4, pas 2 ms). */
private fun pendulumTrack(v: Int): List<FloatArray> = pendulumCache.getOrPut(v % 4) {
    val (d1, d2) = MoreCurves.pendulum(v)
    var s = doubleArrayOf(Math.toRadians(d1.toDouble()), Math.toRadians(d2.toDouble()), 0.0, 0.0)
    val g = 9.81
    fun f(s: DoubleArray): DoubleArray {
        val d = s[0] - s[1]
        val den = 3 - cos(2 * d)
        val a1 = (-3 * g * sin(s[0]) - g * sin(s[0] - 2 * s[1]) - 2 * sin(d) * (s[3] * s[3] + s[2] * s[2] * cos(d))) / den
        val a2 = 2 * sin(d) * (2 * s[2] * s[2] + 2 * g * cos(s[0]) + s[3] * s[3] * cos(d)) / den
        return doubleArrayOf(s[2], s[3], a1, a2)
    }
    fun add(a: DoubleArray, b: DoubleArray, h: Double) = DoubleArray(4) { a[it] + b[it] * h }
    val dt = 0.002
    val out = ArrayList<FloatArray>()
    for (i in 0..15000) {
        if (i % 3 == 0) out += floatArrayOf(s[0].toFloat(), s[1].toFloat())
        val k1 = f(s)
        val k2 = f(add(s, k1, dt / 2))
        val k3 = f(add(s, k2, dt / 2))
        val k4 = f(add(s, k3, dt))
        s = DoubleArray(4) { s[it] + dt / 6 * (k1[it] + 2 * k2[it] + 2 * k3[it] + k4[it]) }
    }
    out
}

/** Pendule double : la trace de la masse du bas en dégradé, la pose de départ en pâle, la pose finale. */
private fun Pencil.doublePendulum(v: Int) {
    val track = pendulumTrack(v)
    val l = 55f
    val o = pt(FW / 2, FH / 2)
    fun bobs(a: FloatArray): Pair<Offset, Offset> {
        val b1 = o + pt(l * sin(a[0]), l * cos(a[0]))
        return b1 to b1 + pt(l * sin(a[1]), l * cos(a[1]))
    }
    val pal = Crayons.rainbow.rot((v * 3 + 4) % 8)
    fill(circle(o.x, o.y, 2 * l + 3, 120), pal[2], hatch = false, wash = 0.1f, lw = 0.8f, strokeAlpha = 0.4f)
    val lower = track.map { bobs(it).second }
    colorTrace(lower, gradient(pal.filter { it != Crayons.jaune }, 42), 0.55f, 0.85f, dense = true)
    // La pose de départ, en pâle, puis la pose finale.
    val (s1, s2) = bobs(track.first())
    stroke(o, s1, Graphite, 1.2f, 0.3f)
    stroke(s1, s2, Graphite, 1.2f, 0.3f)
    dot(s1, 3.5f, Graphite, 0.25f)
    dot(s2, 3.5f, Graphite, 0.25f)
    val (e1, e2) = bobs(track.last())
    line(listOf(o, e1, e2), Graphite, 1.7f)
    fill(circle(e1.x, e1.y, 6.5f, 24), pal[0], gap = 2.4f, wash = 0.5f, lw = 1.1f)
    fill(circle(e2.x, e2.y, 6.5f, 24), pal[4], gap = 2.4f, wash = 0.5f, angle = 30f, lw = 1.1f)
    dot(o, 2.6f, Graphite)
}

private val fieldCache = HashMap<Int, Triple<List<List<Offset>>, List<List<List<Offset>>>, List<List<List<Offset>>>>>()

/** Lignes de champ : bandes de potentiel coloriées, lignes de champ fléchées et charges. */
private fun Pencil.fieldLines(v: Int) {
    val charges = MoreCurves.charges(v)
    val levels = floatArrayOf(1f / 110, 1f / 70, 1f / 46, 1f / 30, 1f / 19)
    val (lines, plus, minus) = fieldCache.getOrPut(v % 3) {
        fun potential(x: Float, y: Float): Float {
            var s = 0f
            for (c in charges) s += c[0] / max(1f, hypot(x - c[1], y - c[2]))
            return s
        }
        val step = 3f
        val nx = (FW / step).toInt() + 1
        val ny = (FH / step).toInt() + 1
        val at = { gx: Float, gy: Float -> pt(gx * step, gy * step) }
        val gp = grid(nx, ny, true) { i, j -> potential(i * step, j * step) }
        val gm = grid(nx, ny, true) { i, j -> -potential(i * step, j * step) }
        val up = levels.map { isolines(gp, nx, ny, it, at) }
        val down = levels.map { isolines(gm, nx, ny, it, at) }
        // Les lignes de champ, issues des charges positives (méthode du point milieu).
        fun field(p: Offset): Offset {
            var ex = 0f
            var ey = 0f
            for (c in charges) {
                val dx = p.x - c[1]
                val dy = p.y - c[2]
                val r2 = dx * dx + dy * dy
                val r3 = r2 * sqrt(r2)
                ex += c[0] * dx / r3
                ey += c[0] * dy / r3
            }
            return pt(ex, ey)
        }
        fun unit(e: Offset): Offset? {
            val n = e.getDistance()
            return if (n < 1e-12f) null else e / n
        }
        val found = ArrayList<List<Offset>>()
        for (c in charges) if (c[0] > 0) {
            val n = (10 * c[0]).toInt()
            for (i in 0 until n) {
                var p = pt(c[1], c[2]) + dir(TURN * (i + 0.5f) / n + 0.13f) * 9f
                val pts = arrayListOf(p)
                for (s in 0 until 1500) {
                    val u1 = unit(field(p)) ?: break
                    val u2 = unit(field(p + u1 * 0.75f)) ?: break
                    p += u2 * 1.5f
                    pts += p
                    if (charges.any { it[0] < 0 && hypot(p.x - it[1], p.y - it[2]) < 9f }) break
                    if (p.x < 0 || p.x > FW || p.y < 0 || p.y > FH) break
                }
                clipLine(pts).firstOrNull()?.let { found += it }
            }
        }
        Triple(found, up, down)
    }
    val (hot, cold) = if (v % 2 == 0) {
        listOf(Crayons.jaune, Crayons.orange, Crayons.orange, Crayons.rouge, Crayons.rose) to
            listOf(Crayons.turquoise, Crayons.bleu, Crayons.bleu, Crayons.marine, Crayons.violet)
    } else {
        listOf(Crayons.jaune, Crayons.vert, Crayons.vert, Crayons.sapin, Crayons.turquoise) to
            listOf(Crayons.rose, Crayons.violet, Crayons.violet, Crayons.marine, Crayons.bleu)
    }
    for (i in levels.indices) {
        fillRegion(plus[i], hot[i], hatch = i % 2 == 1, wash = 0.2f, gap = 4f, angle = -58f + 18f * i, strokeAlpha = 0.45f, lw = 0.7f)
        fillRegion(minus[i], cold[i], hatch = i % 2 == 1, wash = 0.2f, gap = 4f, angle = 58f - 18f * i, strokeAlpha = 0.45f, lw = 0.7f)
    }
    for (l in lines) {
        line(l, Graphite, 0.9f, 0.75f)
        if (l.size > 12) {
            val m = l.size * 2 / 5
            val d = (l[m + 1] - l[m - 1]).let { it / it.getDistance() }
            val nrm = pt(-d.y, d.x)
            stroke(l[m], l[m] - d * 4.5f + nrm * 2.6f, Graphite, 1f, 0.8f)
            stroke(l[m], l[m] - d * 4.5f - nrm * 2.6f, Graphite, 1f, 0.8f)
        }
    }
    for (c in charges) {
        val o = pt(c[1], c[2])
        val r = if (abs(c[0]) > 1) 11f else 9f
        fill(circle(o.x, o.y, r, 30), if (c[0] > 0) Crayons.rouge else Crayons.bleu, gap = 2.6f, wash = 0.6f, lw = 1.2f)
        stroke(o - pt(4f, 0f), o + pt(4f, 0f), Graphite, 1.5f, 0.95f)
        if (c[0] > 0) stroke(o - pt(0f, 4f), o + pt(0f, 4f), Graphite, 1.5f, 0.95f)
    }
}

private val contourCache = HashMap<Int, Pair<List<List<List<Offset>>>, List<Offset>>>()

/** Courbes de niveau : neuf niveaux d'une somme de bosses en cloche, coloriés en bandes comme une carte. */
private fun Pencil.contourMap(v: Int) {
    val hills = MoreCurves.hills(v)
    val (levels, tops) = contourCache.getOrPut(v % 3) {
        fun f(x: Float, y: Float): Float {
            var s = 0f
            for (h in hills) {
                val dx = x - h[0]
                val dy = y - h[1]
                val c = cos(h[5])
                val sn = sin(h[5])
                val u = (c * dx + sn * dy) / h[3]
                val w = (-sn * dx + c * dy) / h[4]
                s += h[2] * exp(-(u * u + w * w) / 2)
            }
            return s
        }
        val step = 3f
        val nx = (FW / step).toInt() + 1
        val ny = (FH / step).toInt() + 1
        val g = grid(nx, ny, true) { i, j -> f(i * step, j * step) }
        val top = g.maxOrNull() ?: 1f
        val at = { gx: Float, gy: Float -> pt(gx * step, gy * step) }
        val found = (1..9).map { isolines(g, nx, ny, top * it / 10f, at) }
        // Les sommets : le plus haut point de la grille près de chaque bosse.
        val peaks = hills.map { h ->
            var best = Offset.Zero
            var bv = -1f
            for (j in -6..6) for (i in -6..6) {
                val p = pt(h[0] + i * 2f, h[1] + j * 2f)
                val fv = f(p.x, p.y)
                if (fv > bv) { bv = fv; best = p }
            }
            best
        }
        found to peaks
    }
    val pal = if (v % 2 == 0) {
        listOf(Crayons.sapin, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.brun, Crayons.rose, Crayons.violet, Crayons.marine)
    } else {
        listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.rose, Crayons.violet)
    }
    levels.forEachIndexed { i, loops ->
        fillRegion(loops, pal[i], hatch = i % 2 == 0, wash = 0.2f, gap = 4.2f, angle = -58f + 23f * i, strokeAlpha = 0.5f, lw = if (i == 4) 1.3f else 0.85f)
    }
    for (t in tops) {
        val tri = listOf(t + pt(0f, -5f), t + pt(4.6f, 3f), t + pt(-4.6f, 3f))
        fill(tri, Graphite, hatch = false, wash = 0.85f, lw = 0.8f)
    }
}

/** Le rotor du moteur Wankel, dans son repère : l'enveloppe intérieure de la paroi, par secteurs de 2°. */
private val wankelRotor: List<List<Offset>> by lazy {
    val e = 0.14f
    val bins = 180
    val best = FloatArray(bins) { Float.MAX_VALUE }
    val nt = 720
    val hx = FloatArray(nt) { val t = TURN * it / nt; cos(t) + e * cos(3 * t) }
    val hy = FloatArray(nt) { val t = TURN * it / nt; sin(t) + e * sin(3 * t) }
    for (i in 0 until 240) {
        val phi = TURN * i / 240
        val cx = e * cos(3 * phi)
        val cy = e * sin(3 * phi)
        val cs = cos(-phi)
        val sn = sin(-phi)
        for (j in 0 until nt) {
            val dx = hx[j] - cx
            val dy = hy[j] - cy
            val qx = dx * cs - dy * sn
            val qy = dx * sn + dy * cs
            var a = atan2(qy, qx)
            if (a < 0) a += TURN
            val b = (a / TURN * bins).toInt() % bins
            best[b] = min(best[b], hypot(qx, qy))
        }
    }
    // Trois flancs, d'un sommet au suivant, un peu en retrait de la paroi.
    (0 until 3).map { f ->
        val from = f * bins / 3
        listOf(dir(TURN * f / 3)) +
            (from until from + bins / 3).map { b -> dir(TURN * (b + 0.5f) / bins) * (best[b] * 0.985f) } +
            listOf(dir(TURN * (f + 1) / 3))
    }
}

/** Moteur Wankel : le carter épitrochoïde, les trois chambres coloriées, le rotor et ses engrenages. */
private fun Pencil.wankel(v: Int) {
    val e = 0.14f
    fun wall(t: Float) = dir(t) + dir(3 * t) * e
    fun normal(t: Float): Offset {
        val d = pt(-sin(t) - 3 * e * sin(3 * t), cos(t) + 3 * e * cos(3 * t))
        return pt(d.y, -d.x) / d.getDistance()
    }
    val housing = sweep(0f, TURN, 360) { wall(it) }
    val casing = sweep(0f, TURN, 360) { wall(it) + normal(it) * 0.16f }
    val flip = { o: Offset -> pt(o.x, -o.y) }
    val fit = Fit(listOf(casing.map(flip)), 12f)
    fun at(o: Offset) = fit(flip(o))
    val phi = rad(14f + 47f * v)
    val c = dir(3 * phi) * e
    fun world(q: Offset) = c + pt(q.x * cos(phi) - q.y * sin(phi), q.x * sin(phi) + q.y * cos(phi))
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fillRegion(listOf(casing.map { at(it) }, housing.map { at(it) }), Graphite, wash = 0.12f, gap = 3.4f, strokeAlpha = 0.35f, lw = 1.2f)
    val flanks = wankelRotor.map { f -> f.map { at(world(it)) } }
    val chamberColors = listOf(pal[0], pal[3], pal[5])
    for (f in 0 until 3) {
        val t0 = phi + TURN * f / 3
        val arc = sweep(t0, t0 + TURN / 3, 80) { at(wall(it)) }
        fill(arc + flanks[f].reversed(), chamberColors[f], wash = 0.34f, gap = 3.6f, angle = -58f + 50f * f, outline = false)
    }
    val rotor = flanks[0] + flanks[1].drop(1) + flanks[2].drop(1)
    fill(rotor, pal[6], wash = 0.3f, gap = 4.2f, angle = 30f, lw = 1.3f)
    // L'engrenage du rotor (rayon 3e) roule sur le pignon fixe (rayon 2e) : ils se touchent toujours.
    val gc = at(c)
    val unit = fit.s
    outline(circle(gc.x, gc.y, 3 * e * unit, 40), lw = 1f, color = Graphite.copy(alpha = 0.8f))
    val o = at(Offset.Zero)
    fill(circle(o.x, o.y, 2 * e * unit, 30), pal[1], gap = 2.6f, wash = 0.5f, lw = 1f)
    dot(o, 2.4f, Graphite)
    dot(gc, 2f, Graphite)
    for (f in 0 until 3) dot(at(world(dir(TURN * f / 3))), 2.6f, Graphite)
    line(housing.map { at(it) }, Graphite, 1.5f)
}

/** Courbe de poursuite : le chien parti du centre, le lièvre sur son cercle, et les lignes de visée. */
private fun Pencil.pursuit(v: Int) {
    val k = MoreCurves.pursuit(v)
    val r = 112f
    val o = center
    val dt = 0.002f
    var dog = o
    val dogs = ArrayList<Offset>()
    val hares = ArrayList<Offset>()
    var t = 0f
    val tMax = 3.2f * TURN
    var i = 0
    while (t <= tMax) {
        val hare = o + pt(r * cos(t), -r * sin(t))
        if (i % 10 == 0) { dogs += dog; hares += hare }
        val d = hare - dog
        val len = d.getDistance()
        if (len > 1e-3f) dog += d * (k * r * dt / len)
        t += dt
        i++
    }
    val pal = Crayons.rainbow.rot((v * 3 + 6) % 8)
    fill(circle(o.x, o.y, r, 120), pal[2], hatch = false, wash = 0.14f, lw = 1.2f)
    fill(circle(o.x, o.y, k * r, 100), pal[2], hatch = false, wash = 0.16f, outline = false)
    val limit = circle(o.x, o.y, k * r, 90)
    for (q in limit.indices step 2) stroke(limit[q], limit[(q + 1) % limit.size], Graphite, 1f, 0.6f)
    // Les lignes de visée, du chien au lièvre, en dégradé.
    val sights = 130
    val grad = gradient(pal.filter { it != Crayons.jaune }, sights)
    for (s in 0 until sights) {
        val q = s * (dogs.size - 1) / sights
        stroke(dogs[q], hares[q], grad[s], 0.8f, 0.85f)
    }
    line(dogs, deep(pal[0]), 2.2f)
    dot(dogs.last(), 3.2f, deep(pal[0]))
    dot(hares.last(), 3.2f, Graphite)
    dot(o, 2f, Graphite)
}

/** Chaînettes : cinq chaînes de perles pendues aux mêmes clous, et la parabole de la plus longue. */
private fun Pencil.catenary(v: Int) {
    val half = 150f
    val top = 30f
    val sags = floatArrayOf(0.18f, 0.42f, 0.7f, 1f, 1.32f)
    val pal = Crayons.rainbow.rot((v * 3 + 3) % 8)
    val inks = pal.filter { it != Crayons.jaune }
    // La poutre et les deux clous.
    fill(listOf(pt(18f, top - 14f), pt(FW - 18f, top - 14f), pt(FW - 18f, top - 4f), pt(18f, top - 4f)), Crayons.brun, gap = 3f, wash = 0.4f, lw = 1.1f)
    // Pour chaque flèche σ, le paramètre a (demi-portée 1) tel que a(ch(1/a) − 1) = σ, par dichotomie sur c = 1/a.
    val params = sags.map { sigma ->
        var lo = 1e-3f
        var hi = 10f
        repeat(40) { val m = (lo + hi) / 2; if ((cosh(m) - 1) / m < sigma) lo = m else hi = m }
        2f / (lo + hi)
    }
    fun at(i: Int, x: Float): Offset {
        val a = params[i]
        return pt(FW / 2 + x * half, top + (sags[i] - a * (cosh(x / a) - 1)) * half)
    }
    for (i in sags.indices) {
        val pts = sweep(-1f, 1f, 120) { at(i, it) }
        val band = if (i == 0) pts else pts + sweep(1f, -1f, 120) { at(i - 1, it) }
        fill(band, pal[(i * 2) % 8], hatch = i % 2 == 1, wash = 0.2f, gap = 4f, angle = -58f + 30f * i, strokeAlpha = 0.45f, outline = false)
    }
    // La parabole de la plus longue chaîne, de mêmes bouts et même point bas.
    val sigma = sags.last()
    val para = sweep(-1f, 1f, 60) { x -> pt(FW / 2 + x * half, top + (sigma - sigma * x * x) * half) }
    for (q in 0 until para.size - 1 step 2) stroke(para[q], para[q + 1], Crayons.rouge, 1.5f, 0.9f)
    for (i in sags.indices) {
        val a = params[i]
        val c = inks[i % inks.size]
        val total = 2 * a * sinh(1 / a)
        val n = (total * half / 6.4f).toInt()
        val pts = (0..n).map { b -> at(i, a * kotlin.math.asinh((-total / 2 + total * b / n) / a)) }
        fine(pts, Graphite, 0.7f, 0.6f)
        for (p in pts) {
            dot(p, 2.4f, deep(c), 0.95f)
            dot(p - pt(0.6f, 0.6f), 0.8f, Color.White, 0.7f)
        }
    }
    for (x in floatArrayOf(-half, half)) dot(pt(FW / 2 + x, top), 3f, Graphite)
}

/** Courbe de Bézier : les segments de De Casteljau pour t de 0 à 1, qui l'enveloppent, et une construction. */
private fun Pencil.casteljau(v: Int) {
    val sets = listOf(
        listOf(pt(34f, 200f), pt(110f, 26f), pt(244f, 230f), pt(336f, 48f)),
        listOf(pt(38f, 214f), pt(78f, 30f), pt(292f, 30f), pt(332f, 214f)),
        listOf(pt(30f, 60f), pt(130f, 236f), pt(330f, 220f), pt(300f, 28f)),
    )
    val p = sets[v % 3]
    fun lerpO(a: Offset, b: Offset, t: Float) = a + (b - a) * t
    fun levels(t: Float): List<List<Offset>> {
        val q = (0..2).map { lerpO(p[it], p[it + 1], t) }
        val r = (0..1).map { lerpO(q[it], q[it + 1], t) }
        return listOf(q, r, listOf(lerpO(r[0], r[1], t)))
    }
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    // L'enveloppe convexe des points de contrôle, au lavis : la courbe n'en sort pas.
    val hull = run {
        val pts = p.sortedWith(compareBy({ it.x }, { it.y }))
        fun cross(o: Offset, a: Offset, b: Offset) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
        val lower = ArrayList<Offset>()
        for (q in pts) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), q) <= 0) lower.removeAt(lower.size - 1)
            lower += q
        }
        val upper = ArrayList<Offset>()
        for (q in pts.reversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), q) <= 0) upper.removeAt(upper.size - 1)
            upper += q
        }
        lower.dropLast(1) + upper.dropLast(1)
    }
    fill(hull, pal[2], wash = 0.14f, gap = 5f, strokeAlpha = 0.3f, outline = false)
    val n = 30
    val grad = gradient(pal.filter { it != Crayons.jaune }, n + 1)
    for (s in 0..n) {
        val t = s / n.toFloat()
        val (q, r) = levels(t)
        stroke(q[0], q[1], pal[0], 0.7f, 0.35f)
        stroke(q[1], q[2], pal[4], 0.7f, 0.35f)
        stroke(r[0], r[1], grad[s], 1.1f, 0.85f)
    }
    outline(p, close = false, lw = 1.1f)
    val curve = sweep(0f, 1f, 120) { levels(it)[2][0] }
    line(curve, Graphite, 2f)
    // La construction mise en valeur.
    val t0 = MoreCurves.casteljau(v)
    val (q, r, b) = levels(t0)
    line(q, deep(pal[5]), 1.4f)
    line(r, deep(pal[6]), 1.6f)
    for (o in q) dot(o, 2.3f, deep(pal[5]))
    for (o in r) dot(o, 2.3f, deep(pal[6]))
    for (o in p) dot(o, 3.2f, Graphite)
    dot(b[0], 4f, Color.White)
    dot(b[0], 3.2f, Crayons.rouge)
}

/** Développante du cercle : trois tours de ficelle déroulée, coloriés par sixièmes de tour, et la bobine. */
private fun Pencil.involute(v: Int) {
    val flip = if (v % 2 == 1) -1f else 1f
    fun foot(t: Float) = pt(flip * cos(t), sin(t))
    fun tip(t: Float) = pt(flip * (cos(t) + t * sin(t)), sin(t) - t * cos(t))
    val turns = 3
    val fit = Fit(listOf(sweep(0f, TURN * turns, 600) { tip(it) }), 10f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val sectors = 6
    for (band in 0 until turns) for (q in 0 until sectors) {
        val s0 = TURN * q / sectors
        val s1 = TURN * (q + 1) / sectors
        val out = sweep(s0, s1, 24) { tip(it + TURN * band) }
        val inn = if (band == 0) sweep(s0, s1, 24) { foot(it) } else sweep(s0, s1, 24) { tip(it + TURN * (band - 1)) }
        fill(fit(out + inn.reversed()), pal[(q * 3 + band * 2) % 8], wash = 0.3f, gap = 3.4f + 0.4f * band, angle = -58f + 30f * q + 40f * band, lw = 1f, strokeAlpha = 0.6f)
    }
    val o = fit(Offset.Zero)
    fill(circle(o.x, o.y, fit.s, 40), Crayons.brun, gap = 2.4f, wash = 0.5f, lw = 1.2f)
    // Quelques positions de la ficelle, tangentes à la bobine et normales à la courbe.
    for (s in 1..5) {
        val t = TURN * turns - 0.9f * s
        stroke(fit(foot(t)), fit(tip(t)), Graphite, 0.9f, 0.55f)
        dot(fit(tip(t)), 1.8f, Graphite)
    }
    val end = TURN * turns
    line(listOf(fit(foot(end)), fit(tip(end))), Graphite, 1.6f)
    dot(fit(tip(end)), 3.4f, Crayons.rouge)
}

/** Interférences : les anneaux de deux sources, superposés, et les hyperboles nodales. */
private fun Pencil.interference(v: Int) {
    val d = MoreCurves.sources(v)
    val lambda = 22f
    val c = d * lambda / 2
    val s1 = center - pt(c, 0f)
    val s2 = center + pt(c, 0f)
    val pal = listOf(Crayons.bleu to Crayons.rose, Crayons.turquoise to Crayons.orange, Crayons.violet to Crayons.vert)[v % 3]
    for ((idx, s) in listOf(s1, s2).withIndex()) {
        val col = if (idx == 0) pal.first else pal.second
        var n = 0
        while (n * lambda < 260f) {
            val outer = clipPoly(circle(s.x, s.y, n * lambda + lambda / 2, 90 + 6 * n))
            val inner = if (n == 0) emptyList() else clipPoly(circle(s.x, s.y, n * lambda, 90 + 6 * n))
            fillRegion(listOf(outer, inner), col, hatch = n % 2 == 0, wash = 0.2f, gap = 4.6f, angle = if (idx == 0) -58f else 58f, outline = false, strokeAlpha = 0.45f)
            n++
        }
    }
    // Les hyperboles nodales : |MS₁ − MS₂| = (m + ½)λ.
    var m = 0
    while ((m + 0.5f) * lambda < 2 * c) {
        val a = (m + 0.5f) * lambda / 2
        val b = sqrt(c * c - a * a)
        for (sign in floatArrayOf(-1f, 1f)) {
            val pts = sweep(-3.2f, 3.2f, 160) { u -> center + pt(sign * a * cosh(u), b * sinh(u)) }
            for (piece in clipLine(pts)) line(piece, Graphite, 1.2f, 0.85f)
        }
        m++
    }
    outline(listOf(pt(8f, 8f), pt(FW - 8f, 8f), pt(FW - 8f, FH - 8f), pt(8f, FH - 8f)), lw = 1.3f)
    for (s in listOf(s1, s2)) {
        dot(s, 4.2f, Color.White)
        dot(s, 3.2f, Graphite)
    }
}

/** Coniques homofocales : le quadrillage en ellipses et hyperboles, colorié en damier. */
private fun Pencil.confocal(v: Int) {
    val muMax = 0.84f
    val c = 168f / cosh(muMax)
    val nm = 5
    val nn = if (v % 2 == 0) 16 else 20
    fun p(mu: Float, nu: Float) = center + pt(c * cosh(mu) * cos(nu), -c * sinh(mu) * sin(nu))
    val pal = Crayons.rainbow.rot((v * 3 + 2) % 8)
    val grad = gradient(pal, nn)
    for (i in 0 until nm) for (j in 0 until nn) {
        val m0 = muMax * i / nm
        val m1 = muMax * (i + 1) / nm
        val n0 = TURN * j / nn
        val n1 = TURN * (j + 1) / nn
        val cell = sweep(m0, m1, 6) { p(it, n0) } + sweep(n0, n1, 10) { p(m1, it) } +
            sweep(m1, m0, 6) { p(it, n1) } + sweep(n1, n0, 10) { p(m0, it) }
        val dark = (i + j) % 2 == 0
        fill(cell, grad[j], hatch = dark, wash = if (dark) 0.42f else 0.16f, gap = 2.8f, angle = -58f + 360f * j / nn, lw = 0.8f, strokeAlpha = 0.6f)
    }
    outline(sweep(0f, TURN, 160) { p(muMax, it) }, lw = 1.4f)
    for (s in floatArrayOf(-1f, 1f)) {
        dot(center + pt(s * c, 0f), 4f, Color.White)
        dot(center + pt(s * c, 0f), 3f, Graphite)
    }
}

/** Corde vibrante : les six premiers modes, fuseaux coloriés, nœuds et quelques instants de la corde. */
private fun Pencil.stringModes(v: Int) {
    val x0 = 26f
    val len = FW - 2 * x0
    val rowH = 38f
    val amp = 14f
    val pal = Crayons.rainbow.rot((v * 3 + 5) % 8)
    for (n in 1..6) {
        val y = (FH - 6 * rowH) / 2 + (n - 0.5f) * rowH
        val c1 = pal[(n - 1) % 8]
        val c2 = pal[(n + 3) % 8]
        for (lobe in 0 until n) {
            val a = lobe / n.toFloat()
            val b = (lobe + 1) / n.toFloat()
            val up = sweep(a, b, 40) { x -> pt(x0 + x * len, y - amp * abs(sin(n * HALF_TURN * x))) }
            val down = sweep(b, a, 40) { x -> pt(x0 + x * len, y + amp * abs(sin(n * HALF_TURN * x))) }
            fill(up + down, if (lobe % 2 == 0) c1 else c2, wash = 0.28f, gap = 3.2f, angle = -58f + 30f * lobe, lw = 0.9f, strokeAlpha = 0.6f)
        }
        // La corde à quelques instants.
        for ((q, ph) in floatArrayOf(0.62f, -0.3f).withIndex()) {
            line(sweep(0f, 1f, 80) { x -> pt(x0 + x * len, y - amp * ph * sin(n * HALF_TURN * x)) }, deep(if (q == 0) c1 else c2), 0.8f, 0.7f)
        }
        for (k in 1 until n) dot(pt(x0 + len * k / n, y), 2.3f, Graphite)
        for (e in floatArrayOf(x0, x0 + len)) stroke(pt(e, y - 6f), pt(e, y + 6f), Crayons.brun, 2.6f, 0.9f)
    }
}

/** Développée de l'ellipse : les normales jusqu'aux centres de courbure, en dégradé, et un cercle osculateur. */
private fun Pencil.evolute(v: Int) {
    val b = 108f
    val a = b / MoreCurves.evolute(v)
    fun p(t: Float) = center + pt(a * cos(t), -b * sin(t))
    fun cc(t: Float) = center + pt((a * a - b * b) / a * cos(t).pow(3), -(b * b - a * a) / b * sin(t).pow(3))
    val pal = Crayons.rainbow.rot((v * 3 + 7) % 8)
    val ellipse = sweep(0f, TURN, 200) { p(it) }
    fill(ellipse, pal[2], wash = 0.14f, gap = 5f, strokeAlpha = 0.3f, lw = 1.4f)
    val evo = sweep(0f, TURN, 240) { cc(it) }
    fill(evo, pal[6], wash = 0.3f, gap = 2.8f, strokeAlpha = 0.6f, lw = 1f)
    val n = 120
    val grad = gradient(pal.filter { it != Crayons.jaune }, n)
    for (i in 0 until n) {
        val t = TURN * (i + 0.5f) / n
        stroke(p(t), cc(t), grad[i], 1f, 0.8f)
    }
    // Un cercle osculateur, en pointillé.
    val t0 = rad(intArrayOf(16, 196, 344, 164)[v % 4].toFloat())
    val o = cc(t0)
    val r = (p(t0) - o).getDistance()
    val ring = circle(o.x, o.y, r, 120)
    for (q in ring.indices step 2) stroke(ring[q], ring[(q + 1) % ring.size], deep(pal[0]), 1.2f, 0.85f)
    stroke(o, p(t0), deep(pal[0]), 1.4f, 0.9f)
    dot(p(t0), 3f, Crayons.rouge)
    dot(o, 2.6f, Graphite)
}

/** Vagues de Gerstner : le ciel, des couches de trochoïdes de plus en plus plates, et les orbites des particules. */
private fun Pencil.gerstner(v: Int) {
    val a = 26f
    val b0 = a * MoreCurves.steepness(v)
    val y0 = 96f
    val dz = 17f
    val layers = 7
    val shift = (v % 4) * 0.9f
    fun depth(i: Int) = b0 * exp(-i * dz / a)
    fun surface(i: Int): List<Offset> {
        val b = depth(i)
        val th0 = (-12f) / a - shift
        return sweep(th0, th0 + (FW + 24f) / a, 260) { th -> pt(a * (th + shift) - b * sin(th), y0 + i * dz - b * cos(th)) }
    }
    val (sky, sea) = if (v % 2 == 0) {
        listOf(Crayons.jaune, Crayons.orange) to
            listOf(Crayons.turquoise, Crayons.turquoise, Crayons.bleu, Crayons.bleu, Crayons.marine, Crayons.marine, Crayons.violet)
    } else {
        listOf(Crayons.rose, Crayons.orange) to
            listOf(Crayons.vert, Crayons.turquoise, Crayons.turquoise, Crayons.bleu, Crayons.bleu, Crayons.marine, Crayons.marine)
    }
    // Le ciel et le soleil.
    fill(clipPoly(surface(0) + listOf(pt(FW + 20f, -20f), pt(-20f, -20f))), sky[0], hatch = false, wash = 0.16f, outline = false)
    val sun = pt(if (v % 2 == 0) 296f else 72f, 44f)
    fill(circle(sun.x, sun.y, 22f, 40), sky[1], gap = 2.8f, wash = 0.4f, lw = 1.1f)
    val bottom = FH - 10f
    for (i in 0 until layers) {
        val below = if (i + 1 < layers) surface(i + 1).reversed() else listOf(pt(FW + 20f, bottom), pt(-20f, bottom))
        fill(clipPoly(surface(i) + below), sea[i], wash = 0.2f + 0.04f * i, gap = 3.4f + 0.25f * i, angle = -58f + 12f * i, outline = false, strokeAlpha = 0.5f)
    }
    for (i in 0 until layers) for (piece in clipLine(surface(i))) line(piece, if (i == 0) Graphite else deep(sea[i]), if (i == 0) 1.6f else 0.8f, if (i == 0) 1f else 0.75f)
    outline(listOf(pt(8f, 8f), pt(FW - 8f, 8f), pt(FW - 8f, FH - 8f), pt(8f, FH - 8f)), lw = 1.3f)
    // Les orbites : la particule de repos aθ tourne sur un cercle de rayon b·e^(−z/a).
    for (col in 0 until 4) {
        val th = 2f + col * 3.1f - shift
        for (i in 0 until 3) {
            val b = depth(i)
            val c = pt(a * (th + shift), y0 + i * dz)
            val ring = circle(c.x, c.y, b, 36)
            for (q in ring.indices step 2) stroke(ring[q], ring[(q + 1) % ring.size], Graphite, 0.8f, 0.6f)
            dot(pt(a * (th + shift) - b * sin(th), y0 + i * dz - b * cos(th)), 2.2f, Graphite)
        }
    }
}
