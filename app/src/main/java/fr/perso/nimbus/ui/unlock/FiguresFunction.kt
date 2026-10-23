package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

// Figures du thème « Fonctions » : coloration de domaine, fonction de Weierstrass, fonction Gamma,
// sinus cardinal et champ de pentes.

internal val functionFigures: List<FigureSpec> = listOf(
    FigureSpec("domain_coloring", "Coloration de domaine", "Fonctions", { v ->
        "Chaque point z du plan complexe est colorié selon f(z) : la teinte donne l’argument de f(z), et les bandes, foncées puis claires, marquent les modules qui doublent. " +
            when (v % 3) {
                0 -> "Ici f(z) = z³ − 1 : toutes les couleurs se rejoignent aux trois racines cubiques de l’unité (cerclées) ; au loin, f ressemble à z³ et les couleurs font trois fois le tour."
                1 -> "Ici f(z) = (z² − 1)(z − 2 − i)² / (z² + 2 + 2i) : zéros simples en ±1, double en 2 + i (les couleurs y tournent deux fois), pôles en noir, où elles tournent à l’envers."
                else -> "Ici f(z) = sin z, de −6,6 à 6,6 : zéros en 0, ±π, ±2π. Comme sin(x + iy) = sin x ch y + i cos x sh y, le module croît vite loin de l’axe réel."
            }
    }) { domainColoring(it) },
    FigureSpec("weierstrass", "Fonction de Weierstrass", "Fonctions", { v ->
        val a = WeierParams.a(v)
        "W(x) = Σ aⁿ cos(3ⁿ πx), ici avec a = ${WeierParams.label(v)} : chaque terme ajoute des ondulations 3 fois plus serrées et ${WeierParams.ratio(v)} fois moins hautes (les premières sommes, en couleurs). " +
            "Cette fonction est continue partout et dérivable nulle part. La loupe grossit 9 fois en largeur le petit cadre : c’est toujours aussi rugueux. " +
            "Karl Weierstrass l’a présentée en 1872, au grand trouble de ses contemporains."
    }) { weierstrass(it) },
    FigureSpec("gamma_function", "Fonction Gamma", "Fonctions", { v ->
        "La fonction Gamma d’Euler (1729), intégrale de t^(x−1) e^(−t), prolonge la factorielle : Γ(n) = (n − 1)! (points jaunes : 1, 1, 2, 6) et Γ(x + 1) = x Γ(x). " +
            "Elle ne s’annule jamais, mais a un pôle en 0 et en chaque entier négatif (pointillés), où ses branches changent de signe. Γ(1/2) = √π ≈ 1,772. " +
            if (v % 2 == 1) "En bleu, 1/Γ : définie partout, elle s’annule aux pôles de Γ." else "Son minimum positif vaut environ 0,886, en x ≈ 1,462."
    }) { gammaFunction(it) },
    FigureSpec("sinc_surface", "Sinus cardinal", "Fonctions", { _ ->
        "La surface z = sin(r)/r, où r est la distance au centre : un sombrero. Au centre, elle vaut 1, car sin r ≈ r près de 0 ; autour, des vagues circulaires " +
            "qui s’annulent pour r = π, 2π, 3π… et dont la hauteur décroît comme 1/r. En une dimension, sin(x)/x est le sinus cardinal du traitement du signal, " +
            "au cœur du théorème d’échantillonnage de Shannon."
    }) { sincSurface(it) },
    FigureSpec("slope_field", "Champ de pentes", "Fonctions", { v ->
        "En chaque point, un petit trait a la pente que doit avoir une solution qui y passe ; les courbes en couleur suivent ces traits pas à pas (méthode de Runge-Kutta). " +
            when (v % 3) {
                0 -> "Ici y′ = x − y : les solutions y = x − 1 + C e^(−x) se rapprochent toutes de la droite y = x − 1 (en pointillés)."
                1 -> "Ici y′ = y (1 − y), l’équation logistique de Verhulst (1838) : les solutions en S montent vers 1, qui attire tout ce qui part au-dessus de 0."
                else -> "Ici y′ = y cos x : les solutions y = C e^(sin x) ondulent sans jamais se croiser, car par chaque point passe une seule solution (Cauchy-Lipschitz)."
            }
    }) { slopeField(it) },
)

// ------------------------------------------------------------ Outils communs

/** Le dernier calcul fait pour une variante, gardé pour les affichages suivants. */
private class PerVariant<T : Any>(private val make: (Int) -> T) {
    private var variant = -1
    private var value: T? = null

    @Synchronized
    operator fun get(v: Int): T {
        val known = value
        if (known != null && variant == v) return known
        return make(v).also { value = it; variant = v }
    }
}

/** Couleur à la position [t] (de 0 à 1) d'un dégradé passant par [pal]. */
private fun ramp(pal: List<Color>, t: Float): Color {
    val x = t.coerceIn(0f, 1f) * (pal.size - 1)
    val i = min(x.toInt(), pal.size - 2)
    return lerp(pal[i], pal[i + 1], x - i)
}

/** Couleur à la position [t] d'un dégradé en boucle sur [pal] (la dernière couleur rejoint la première). */
private fun loopRamp(pal: List<Color>, t: Float): Color {
    val x = (t - floor(t)) * pal.size
    val i = x.toInt() % pal.size
    return lerp(pal[i], pal[(i + 1) % pal.size], x - x.toInt())
}

/** Du bleu nuit au rouge, en passant par le vert et le jaune. */
private val DEEP = listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge)

/** Un rectangle, coin en haut à gauche ([x], [y]). */
private fun rect(x: Float, y: Float, w: Float, h: Float = w) = listOf(pt(x, y), pt(x + w, y), pt(x + w, y + h), pt(x, y + h))

/** Une case pleine de [s] unités, coin en haut à gauche ([x], [y]), sans hachures ni contour. */
private fun Pencil.pixel(x: Float, y: Float, s: Float, color: Color, alpha: Float) = scope.drawRect(
    color,
    topLeft = px(pt(x + j(0.25f), y + j(0.25f))),
    size = Size(s * k * 1.04f, s * k * 1.04f),
    alpha = alpha,
)

/** Les points de [pts] espacés d'au moins [gap] unités : le trait de crayon, tremblé à chaque point, reste lisse. */
private fun sparse(pts: List<Offset>, gap: Float = 2.2f): List<Offset> {
    if (pts.size < 3) return pts
    val out = mutableListOf(pts.first())
    for (q in pts) if ((q - out.last()).getDistance() > gap) out += q
    if (out.last() != pts.last()) out += pts.last()
    return out
}

/** Un trait en pointillés de [a] à [b]. */
private fun Pencil.dashed(a: Offset, b: Offset, color: Color = Graphite, lw: Float = 0.8f, alpha: Float = 0.6f, dash: Float = 3.5f) {
    val n = max(2, ((b - a).getDistance() / dash).toInt())
    for (i in 0 until n step 2) stroke(a + (b - a) * (i / n.toFloat()), a + (b - a) * ((i + 1) / n.toFloat()), color, lw, alpha)
}

/** Repère d'un graphe : le monde [x0, x1] × [y0, y1] envoyé sur le rectangle [left, top, right, bottom] de la case. */
private class Plot(
    val x0: Float, val x1: Float, val y0: Float, val y1: Float,
    val left: Float = 14f, val top: Float = 14f, val right: Float = FW - 14f, val bottom: Float = FH - 14f,
) {
    val sx get() = (right - left) / (x1 - x0)
    val sy get() = (bottom - top) / (y1 - y0)
    fun x(u: Float) = left + (u - x0) * sx
    fun y(w: Float) = bottom - (w - y0) * sy
    operator fun invoke(u: Float, w: Float) = pt(x(u), y(w))
}

/** Dessine [block] sans déborder du cadre de [plot]. */
private fun Pencil.inside(plot: Plot, block: () -> Unit) {
    val a = px(pt(plot.left, plot.top))
    val b = px(pt(plot.right, plot.bottom))
    scope.clipRect(a.x, a.y, b.x, b.y) { block() }
}

// ------------------------------------------------------------ Coloration de domaine

private const val DOMAIN_CELL = 2.5f
private val DOMAIN_COLS = ((FW - 20f) / DOMAIN_CELL).toInt()
private val DOMAIN_ROWS = ((FH - 18f) / DOMAIN_CELL).toInt()

/** Demi-largeur de la vue, selon la fonction. */
private fun domainHalf(kind: Int) = when (kind) { 0 -> 2.0; 1 -> 3.2; else -> 6.6 }

/** f(z) pour la fonction [kind], en parties réelle et imaginaire. */
private fun domainF(kind: Int, x: Double, y: Double): Pair<Double, Double> {
    fun mul(a: Double, b: Double, c: Double, d: Double) = (a * c - b * d) to (a * d + b * c)
    return when (kind) {
        0 -> { val (x2, y2) = mul(x, y, x, y); val (x3, y3) = mul(x2, y2, x, y); (x3 - 1) to y3 }
        1 -> {
            val (z2r, z2i) = mul(x, y, x, y)
            val ur = z2r - 1; val ui = z2i
            val (wr, wi) = mul(x - 2, y - 1, x - 2, y - 1)
            val (nr, ni) = mul(ur, ui, wr, wi)
            val dr = z2r + 2; val di = z2i + 2
            val den = dr * dr + di * di
            ((nr * dr + ni * di) / den) to ((ni * dr - nr * di) / den)
        }
        else -> (sin(x) * cosh(y)) to (cos(x) * sinh(y))
    }
}

/** Pour chaque case : l'argument (en tours) et la position dans sa bande de module ; NaN si indéfini. */
private val domainRuns = PerVariant { v ->
    val kind = v % 3
    val half = domainHalf(kind)
    val span = 2 * half / DOMAIN_COLS
    val hue = FloatArray(DOMAIN_COLS * DOMAIN_ROWS)
    val band = FloatArray(DOMAIN_COLS * DOMAIN_ROWS)
    for (row in 0 until DOMAIN_ROWS) for (col in 0 until DOMAIN_COLS) {
        val x = (col + 0.5 - DOMAIN_COLS / 2.0) * span
        val y = -(row + 0.5 - DOMAIN_ROWS / 2.0) * span
        val (wr, wi) = domainF(kind, x, y)
        val m = hypot(wr, wi)
        val i = row * DOMAIN_COLS + col
        if (!m.isFinite() || m == 0.0) { hue[i] = Float.NaN; continue }
        hue[i] = (atan2(wi, wr) / (2 * PI)).toFloat()
        val l = log2(m)
        band[i] = (l - floor(l)).toFloat()
    }
    hue to band
}

/** Coloration de domaine : la teinte suit l'argument de f(z), les bandes son module ; zéros cerclés, pôles en noir. */
private fun Pencil.domainColoring(v: Int) {
    val kind = v % 3
    val (hue, band) = domainRuns[v]
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val left = (FW - DOMAIN_COLS * DOMAIN_CELL) / 2
    val top = (FH - DOMAIN_ROWS * DOMAIN_CELL) / 2
    for (i in hue.indices) {
        if (hue[i].isNaN()) continue
        pixel(left + (i % DOMAIN_COLS) * DOMAIN_CELL, top + (i / DOMAIN_COLS) * DOMAIN_CELL, DOMAIN_CELL, loopRamp(pal, hue[i]), 0.82f - 0.52f * band[i])
    }
    val half = domainHalf(kind)
    val scale = DOMAIN_COLS * DOMAIN_CELL / (2 * half)
    fun at(x: Double, y: Double) = pt((FW / 2 + x * scale).toFloat(), (FH / 2 - y * scale).toFloat())
    val (zeros, poles) = when (kind) {
        0 -> List(3) { cos(2 * PI * it / 3) to sin(2 * PI * it / 3) } to emptyList()
        1 -> {
            val m = sqrt(2 * sqrt(2.0)); val a = -3 * PI / 8
            listOf(1.0 to 0.0, -1.0 to 0.0, 2.0 to 1.0) to listOf(m * cos(a) to m * sin(a), -m * cos(a) to -m * sin(a))
        }
        else -> (-2..2).map { it * PI to 0.0 } to emptyList()
    }
    for ((x, y) in zeros) { val p = at(x, y); outline(circle(p.x, p.y, 4f, 16), lw = 1.1f); dot(p, 1f, Graphite) }
    for ((x, y) in poles) { val p = at(x, y); fill(circle(p.x, p.y, 3.4f, 14), Graphite, hatch = false, wash = 0.9f, lw = 0.9f) }
    outline(rect(left, top, DOMAIN_COLS * DOMAIN_CELL, DOMAIN_ROWS * DOMAIN_CELL), lw = 1.5f)
}

// ------------------------------------------------------------ Weierstrass

private object WeierParams {
    fun a(v: Int) = if (v % 2 == 0) 0.5 else 0.65
    fun label(v: Int) = if (v % 2 == 0) "0,5" else "0,65"
    fun ratio(v: Int) = if (v % 2 == 0) "2" else "1,54"
}

/** Somme des [terms] premiers termes de la série de Weierstrass (b = 3). */
private fun weierstrassSum(x: Double, a: Double, terms: Int): Double {
    var s = 0.0; var an = 1.0; var bn = 1.0
    repeat(terms) { s += an * cos(bn * PI * x); an *= a; bn *= 3.0 }
    return s
}

/** La fonction de Weierstrass, ses premières sommes partielles, et une loupe. */
private fun Pencil.weierstrass(v: Int) {
    val a = WeierParams.a(v)
    val amp = (1 / (1 - a)).toFloat()
    val plot = Plot(-1f, 1f, -amp * 1.04f, amp * 1.04f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val n = 2400
    val xs = List(n + 1) { -1.0 + 2.0 * it / n }
    val curve = xs.map { plot(it.toFloat(), weierstrassSum(it, a, 9).toFloat()) }
    fill(curve + listOf(plot(1f, -amp * 1.04f), plot(-1f, -amp * 1.04f)), pal[5], gap = 5f, wash = 0.13f, strokeAlpha = 0.35f, outline = false)
    for (t in 1..4) line(xs.filterIndexed { i, _ -> i % 4 == 0 }.map { plot(it.toFloat(), weierstrassSum(it, a, t).toFloat()) }, pal[t - 1], 1.1f, 0.6f)
    line(curve, Graphite, 0.9f, 0.95f)
    // La loupe, en haut à droite : 9 fois plus large que le cadre qu'elle grossit.
    val box = rect(plot.x(0.46f), plot.y(amp * 1.02f), plot.x(0.98f) - plot.x(0.46f), plot.y(amp * 0.6f) - plot.y(amp * 1.02f))
    val boxW = box[1].x - box[0].x
    val boxH = box[2].y - box[1].y
    val width = boxW / plot.sx / 9f
    val x0 = 0.215f
    val zoom = List(721) { x0 + width * it / 720.0 }.map { it to weierstrassSum(it, a, 12) }
    val lo = zoom.minOf { it.second }
    val hi = zoom.maxOf { it.second }
    val frame = rect(plot.x(x0), plot.y(hi.toFloat()) - 3f, plot.x(x0 + width) - plot.x(x0), plot.y(lo.toFloat()) - plot.y(hi.toFloat()) + 6f)
    fill(box, Crayons.jaune, hatch = false, wash = 0.22f, lw = 1.1f)
    outline(frame, lw = 1f)
    dashed(frame[1], box[0], alpha = 0.45f)
    dashed(frame[2], box[3], alpha = 0.45f)
    val zoomed = zoom.map { (x, y) ->
        pt(box[0].x + ((x - x0) / width).toFloat() * boxW, box[2].y - 4f - ((y - lo) / (hi - lo)).toFloat() * (boxH - 8f))
    }
    line(zoomed, Graphite, 0.9f, 0.95f)
}

// ------------------------------------------------------------ Gamma

/** Γ(x) par l'approximation de Lanczos, et la formule des compléments pour x < 1/2. */
private fun gammaOf(x: Double): Double {
    if (x < 0.5) return PI / (sin(PI * x) * gammaOf(1 - x))
    val p = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
        12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
    )
    val y = x - 1
    var a = p[0]
    for (i in 1 until 9) a += p[i] / (y + i)
    val t = y + 7.5
    return sqrt(2 * PI) * t.pow(y + 0.5) * exp(-t) * a
}

/** La fonction Gamma, branche par branche, ses pôles et la factorielle aux entiers. */
private fun Pencil.gammaFunction(v: Int) {
    val plot = Plot(-4.7f, 4.7f, -5.6f, 7.4f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun clampY(y: Double) = y.coerceIn(-9.0, 11.0).toFloat()
    // Le quadrillage léger, les axes et les pôles.
    for (i in -4..4) stroke(plot(i.toFloat(), plot.y0), plot(i.toFloat(), plot.y1), Graphite, 0.4f, 0.08f)
    for (i in -5..7) stroke(plot(plot.x0, i.toFloat()), plot(plot.x1, i.toFloat()), Graphite, 0.4f, 0.08f)
    stroke(plot(plot.x0, 0f), plot(plot.x1, 0f), Graphite, 0.8f, 0.55f)
    for (i in 0 downTo -4) dashed(plot(i.toFloat(), plot.y0), plot(i.toFloat(), plot.y1), alpha = 0.55f)
    inside(plot) {
        val branches = listOf(0.0 to 4.7) + (0 downTo -4).map { (it - 1).toDouble() to it.toDouble() }
        branches.forEachIndexed { i, (a, b) ->
            val lo = max(a, -4.7) + 0.004
            val hi = b - 0.004
            val m = 260
            val pts = List(m + 1) { val x = lo + (hi - lo) * it / m; plot(x.toFloat(), clampY(gammaOf(x))) }
            val color = pal[i % 8]
            // Chaque branche remplit sa coupe, entre la courbe et l'infini (en haut si Γ > 0, en bas sinon).
            val edge = if (gammaOf((lo + hi) / 2) > 0) plot.y1 + 1f else plot.y0 - 1f
            fill(pts + listOf(plot(hi.toFloat(), edge), plot(lo.toFloat(), edge)), color, gap = 4.2f, wash = 0.2f, angle = -58f + 30f * i, strokeAlpha = 0.5f, outline = false)
            line(sparse(pts), color, 1.7f, 0.95f)
        }
        if (v % 2 == 1) {
            val m = 600
            val inv = List(m + 1) { val x = -4.7 + 9.4 * it / m; plot(x.toFloat(), clampY(1 / gammaOf(x))) }
            line(sparse(inv), Crayons.marine, 1.4f, 0.8f)
        }
    }
    for (n in 1..4) {
        val p = plot(n.toFloat(), gammaOf(n.toDouble()).toFloat())
        fill(circle(p.x, p.y, 3.6f, 14), Crayons.jaune, hatch = false, wash = 0.95f, lw = 0.9f)
    }
    val half = plot(0.5f, sqrt(PI).toFloat())
    fill(circle(half.x, half.y, 2.8f, 12), Crayons.rose, hatch = false, wash = 0.95f, lw = 0.8f)
}

// ------------------------------------------------------------ Sinus cardinal

/** La surface z = sin(r)/r en facettes, coloriées selon la hauteur, des plus lointaines aux plus proches. */
private fun Pencil.sincSurface(v: Int) {
    val n = 32
    val yaw = rad(if (v % 2 == 0) 28f else 45f)
    val tilt = rad(if (v % 2 == 0) 32f else 40f)
    val ca = cos(yaw); val sa = sin(yaw); val st = sin(tilt); val ct = cos(tilt)
    fun h(x: Float, y: Float): Float { val r = 3.4f * PI.toFloat() * hypot(x, y); return if (r < 1e-4f) 1f else sin(r) / r }
    fun depth(x: Float, y: Float) = x * sa + y * ca
    fun proj(x: Float, y: Float) = pt(x * ca - y * sa, depth(x, y) * st - h(x, y) * 0.55f * ct)
    val faces = mutableListOf<Triple<List<Offset>, Float, Float>>()
    for (i in 0 until n) for (j in 0 until n) {
        val x0 = -1f + 2f * i / n; val x1 = -1f + 2f * (i + 1) / n
        val y0 = -1f + 2f * j / n; val y1 = -1f + 2f * (j + 1) / n
        val quad = listOf(proj(x0, y0), proj(x1, y0), proj(x1, y1), proj(x0, y1))
        val xm = (x0 + x1) / 2; val ym = (y0 + y1) / 2
        faces += Triple(quad, depth(xm, ym), h(xm, ym))
    }
    faces.sortBy { it.second }
    val fit = Fit(faces.map { it.first }, 10f)
    val pal = if (v % 2 == 0) DEEP else DEEP.reversed()
    for ((quad, _, z) in faces) {
        val t = ((z + 0.23f) / 1.23f).coerceIn(0f, 1f).pow(0.55f)
        // Facettes opaques (la couleur mêlée au papier) : celles de derrière ne transparaissent pas.
        fill(fit(quad), lerp(ramp(pal, t), PaperColors.Paper, 0.4f), hatch = false, wash = 1f, lw = 0.35f)
    }
}

// ------------------------------------------------------------ Champ de pentes

/** Le champ de pentes d'une équation y′ = f(x, y) et quelques solutions (Runge-Kutta d'ordre 4). */
private fun Pencil.slopeField(v: Int) {
    val kind = v % 3
    val plot = when (kind) {
        0 -> Plot(-3.5f, 3.5f, -2.3f, 2.3f)
        1 -> Plot(-6f, 6f, -0.6f, 1.6f)
        else -> Plot(-2 * PI.toFloat(), 2 * PI.toFloat(), -4.3f, 4.3f)
    }
    fun f(x: Float, y: Float) = when (kind) { 0 -> x - y; 1 -> y * (1 - y); else -> y * cos(x) }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    // Les petits traits.
    val gx = 27; val gy = 17
    for (i in 0 until gx) for (j in 0 until gy) {
        val x = plot.x0 + (plot.x1 - plot.x0) * (i + 0.5f) / gx
        val y = plot.y0 + (plot.y1 - plot.y0) * (j + 0.5f) / gy
        val d = pt(plot.sx, -f(x, y) * plot.sy)
        val u = d / d.getDistance() * 4.6f
        stroke(plot(x, y) - u, plot(x, y) + u, Graphite, 1f, 0.5f)
    }
    stroke(plot(plot.x0, 0f), plot(plot.x1, 0f), Graphite, 0.6f, 0.3f)
    stroke(plot(0f, plot.y0), plot(0f, plot.y1), Graphite, 0.6f, 0.3f)
    if (kind == 0) dashed(plot(-1.3f, -2.3f), plot(3.3f, 2.3f), Graphite, 1f, 0.6f)
    val starts = when (kind) {
        0 -> List(9) { -2.2f + 0.55f * it }
        1 -> listOf(-0.05f, 0.012f, 0.06f, 0.2f, 0.5f, 0.8f, 0.94f, 1.2f, 1.55f)
        else -> listOf(-1.5f, -0.9f, -0.45f, -0.15f, 0f, 0.15f, 0.45f, 0.9f, 1.5f)
    }
    inside(plot) {
        starts.forEachIndexed { idx, y0 ->
            val halves = listOf(1f, -1f).map { sgn ->
                val h = 0.01f * sgn * (plot.x1 - plot.x0) / 7f
                var x = 0f; var y = y0
                val pts = mutableListOf(plot(x, y))
                while (x in plot.x0..plot.x1 && y in plot.y0 - 1f..plot.y1 + 1f) {
                    val k1 = f(x, y); val k2 = f(x + h / 2, y + h / 2 * k1); val k3 = f(x + h / 2, y + h / 2 * k2); val k4 = f(x + h, y + h * k3)
                    y += h / 6 * (k1 + 2 * k2 + 2 * k3 + k4); x += h
                    pts += plot(x, y)
                }
                pts
            }
            line(sparse(halves[1].reversed() + halves[0].drop(1)), pal[idx % 8], 1.9f, 0.9f)
        }
    }
    outline(rect(plot.left, plot.top, plot.right - plot.left, plot.bottom - plot.top), lw = 1.1f)
}
