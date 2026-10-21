package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// Figures du thème « Courbes » : roses, spirales, courbes roulantes, enveloppes, séries de Fourier…

private const val TAU = 2 * PI.toFloat()

/** Points d'une courbe paramétrée, de [t0] à [t1] en [n] pas. */
private fun curve(t0: Float, t1: Float, n: Int, f: (Float) -> Offset): List<Offset> =
    (0..n).map { f(t0 + (t1 - t0) * it / n) }

/** Une polyligne coupée en morceaux de couleurs successives. */
private fun Pencil.rainbowLine(pts: List<Offset>, colors: List<Color>, lw: Float, alpha: Float = 1f) {
    val chunk = max(1, pts.size / colors.size)
    for (i in colors.indices) {
        val from = i * chunk
        if (from >= pts.size - 1) break
        val to = if (i == colors.lastIndex) pts.size else min(pts.size, (i + 1) * chunk + 1)
        line(pts.subList(from, to), colors[i], lw, alpha)
    }
}

// ------------------------------------------------------------ Paramètres des variantes

private object Curves {
    /** Rose de Maurer : n de la rose, pas d en degrés. */
    fun maurer(v: Int): Pair<Int, Int> = listOf(6 to 71, 2 to 39, 5 to 97, 4 to 31, 7 to 19, 3 to 47)[v % 6]

    /** Spirale d'Archimède : nombre de bras. */
    fun arms(v: Int): Int = if (v % 2 == 0) 1 else 2

    /** Fils tendus : nombre de coins du polygone. */
    fun corners(v: Int): Int = intArrayOf(4, 3, 6)[v % 3]
}

internal val curveFigures: List<FigureSpec> = listOf(
    FigureSpec("maurer", "Rose de Maurer", "Courbes", { _ -> "" }) { maurer(it) },
    FigureSpec("butterfly", "Courbe du papillon", "Courbes", { _ -> "" }) { butterfly(it) },
    FigureSpec("archimedes", "Spirale d’Archimède", "Courbes", { _ -> "" }) { archimedes(it) },
    FigureSpec("nautilus", "Nautile", "Courbes", { _ -> "" }) { nautilus(it) },
    FigureSpec("fermat", "Spirale de Fermat", "Courbes", { _ -> "" }) { fermat(it) },
    FigureSpec("cornu", "Clothoïde", "Courbes", { _ -> "" }) { cornu(it) },
    FigureSpec("astroid", "Astroïde", "Courbes", { _ -> "" }) { astroid(it) },
    FigureSpec("cup_caustic", "Caustique de la tasse", "Courbes", { _ -> "" }) { cupCaustic(it) },
    FigureSpec("lemniscate", "Lemniscate de Bernoulli", "Courbes", { _ -> "" }) { lemniscate(it) },
    FigureSpec("cassini", "Ovales de Cassini", "Courbes", { _ -> "" }) { cassini(it) },
    FigureSpec("limacon", "Limaçons de Pascal", "Courbes", { _ -> "" }) { limacon(it) },
    FigureSpec("cycloid", "Cycloïde", "Courbes", { _ -> "" }) { cycloid(it) },
    FigureSpec("hypocycloid", "Hypocycloïdes", "Courbes", { _ -> "" }) { hypocycloid(it) },
    FigureSpec("superellipse", "Courbes de Lamé", "Courbes", { _ -> "" }) { superellipse(it) },
    FigureSpec("fourier_square", "Série de Fourier", "Courbes", { _ -> "" }) { fourier(it) },
    FigureSpec("string_art", "Fils tendus", "Courbes", { _ -> "" }) { stringArt(it) },
    FigureSpec("heart", "Courbe du cœur", "Courbes", { _ -> "" }) { heart(it) },
    FigureSpec("baravelle", "Spirale de Baravelle", "Courbes", { _ -> "" }) { baravelle(it) },
    FigureSpec("triskele", "Triskèle", "Courbes", { _ -> "" }) { triskele(it) },
    FigureSpec("epicycloid", "Épicycloïdes", "Courbes", { _ -> "" }) { epicycloid(it) },
)

// ------------------------------------------------------------ Les dessins

private fun Pencil.maurer(v: Int) {
    val (n, d) = Curves.maurer(v)
    val r = 116f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    // Les pétales de la rose, coloriés : sur [jπ/n, (j + 1)π/n], r = |sin nθ| (n impair : seulement là où sin nθ ≥ 0).
    val petals = if (n % 2 == 1) (0 until 2 * n).filter { it % 2 == 0 } else (0 until 2 * n).toList()
    petals.forEachIndexed { i, j ->
        val a0 = PI.toFloat() * j / n
        val pts = curve(a0, a0 + PI.toFloat() / n, 40) { t -> center + dir(t - PI.toFloat() / 2) * (r * abs(sin(n * t))) }
        fill(pts, pal[i % 2 * 4], wash = 0.34f, gap = 4.6f, strokeAlpha = 0.6f, angle = -58f + 360f * j / (2 * n), lw = 1.2f)
    }
    // La toile : les points d'angles k·d degrés, reliés dans l'ordre.
    val web = (0..360).map { kk ->
        val t = rad((kk * d % 360).toFloat())
        center + dir(t - PI.toFloat() / 2) * (r * sin(n * t))
    }
    rainbowLine(web, listOf(pal[1], pal[2], pal[5], pal[6]), 0.55f, 0.7f)
}

private fun Pencil.butterfly(v: Int) {
    fun p(t: Float): Offset {
        val r = exp(sin(t)) - 2 * cos(4 * t) + sin((2 * t - PI.toFloat()) / 24).pow(5)
        return pt(sin(t) * r, -cos(t) * r)
    }
    val all = curve(0f, 12 * TAU, 4000) { p(it) }
    val fit = Fit(listOf(all), 8f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val wing = listOf(Crayons.rose, Crayons.bleu, Crayons.jaune, Crayons.violet)[v % 4]
    // Les ailes du premier tour, coloriées ; puis les douze tours au crayon.
    val wings = fit(curve(0f, TAU, 360) { p(it) })
    fill(wings, wing, wash = 0.28f, gap = 4.6f, strokeAlpha = 0.5f, outline = false)
    rainbowLine(fit(all), pal, 0.9f, 0.85f)
    outline(wings, lw = 1.1f)
}

private fun Pencil.archimedes(v: Int) {
    val arms = Curves.arms(v)
    val turns = 3.5f
    val a = 118f / (TAU * (turns + 1f / arms))
    val pal = if (arms == 1) Crayons.rainbow.rot(v % 8) else listOf(Crayons.bleu, Crayons.orange).rot(v % 2)
    val bands = (turns * arms).toInt()
    for (kk in 0 until bands) {
        val inner = curve(0f, TAU, 90) { t -> center + dir(t) * (a * (t + TAU * kk / arms)) }
        val outer = curve(0f, TAU, 90) { t -> center + dir(t) * (a * (t + TAU * (kk + 1) / arms)) }
        val color = if (arms == 1) pal[kk % pal.size] else pal[kk % 2]
        fill(outer + inner.reversed(), color, wash = if (arms == 2 && kk % 2 == 1) 0.4f else 0.3f, gap = 3.6f, angle = -58f + 25f * kk, lw = 1.1f)
    }
    dot(center, 2.2f, Graphite)
}

private fun Pencil.nautilus(v: Int) {
    val b = ln(3f) / TAU
    fun r(t: Float) = exp(b * t)
    fun p(t: Float) = dir(-t) * r(t)
    val tMax = 3 * TAU
    val living = TAU / 3
    // Cadre : le dernier tour de la coquille.
    val fit = Fit(listOf(curve(tMax - TAU, tMax, 120) { p(it) }), 10f)
    val pal = listOf(
        listOf(Crayons.orange, Crayons.jaune, Crayons.brun),
        listOf(Crayons.bleu, Crayons.turquoise, Crayons.marine),
        listOf(Crayons.rose, Crayons.violet, Crayons.rouge),
    )[v % 3]
    val step = TAU / 14
    // Cloison entre le tour extérieur et l'intérieur, un peu courbée vers l'arrière.
    fun septum(t: Float): List<Offset> {
        val outer = p(t)
        val inner = p(t - TAU)
        val back = (p(t - 0.25f) - outer)
        return (0..8).map { i ->
            val s = i / 8f
            outer + (inner - outer) * s + back * (0.5f * sin(PI.toFloat() * s))
        }
    }
    var t = tMax - living - 2.2f * TAU
    var i = 0
    while (t + step <= tMax - living + 1e-3f) {
        val a = t
        val c = t + step
        val shape = curve(a, c, 10) { p(it) } + septum(c) + curve(c - TAU, a - TAU, 10) { p(it) } + septum(a).reversed()
        val size = r(c) * fit.s
        fill(fit(shape), pal[i % 2], hatch = size > 14, wash = if (i % 2 == 0) 0.35f else 0.25f, gap = 3.4f, angle = -58f + 18f * i, lw = if (size > 14) 1.1f else 0.7f)
        t = c
        i++
    }
    // La loge habitée, la plus grande, avec des flammes brunes.
    val a = tMax - living
    val shell = curve(a, tMax, 30) { p(it) } + curve(tMax - TAU, a - TAU, 30) { p(it) } + septum(a).reversed()
    fill(fit(shell), pal[1], wash = 0.3f, gap = 4.4f, angle = 40f, lw = 1.3f)
    // Les flammes : de courtes bandes brunes qui partent du bord extérieur, un peu inclinées vers l'arrière.
    for (f in 1..5) {
        val tf = a + living * f / 6
        val outer = p(tf)
        val toward = p(tf - TAU) - outer
        val stripe = (0..6).map { i -> val s2 = i / 6f; outer + toward * (0.38f * s2) + (p(tf - 0.12f) - outer) * (0.8f * s2) }
        line(fit(stripe), pal[2], 3f, 0.55f)
    }
}

private fun Pencil.fermat(v: Int) {
    val turns = if (v % 2 == 0) 4 else 6
    val th = turns * TAU
    val a = 118f / sqrt(th)
    val pal = listOf(Crayons.violet to Crayons.jaune, Crayons.turquoise to Crayons.rose, Crayons.marine to Crayons.orange)[v % 3]
    fun arm(sign: Float, t: Float) = center + dir(t + if (sign < 0) PI.toFloat() else 0f) * (a * sqrt(t))
    val n = 600
    val plus = curve(0f, th, n) { arm(1f, it) }
    val minus = curve(0f, th, n) { arm(-1f, it) }
    val arc1 = curve(th, th + PI.toFloat(), 60) { center + dir(it) * (a * sqrt(th)) }
    val arc2 = curve(th + PI.toFloat(), th + TAU, 60) { center + dir(it) * (a * sqrt(th)) }
    fill(plus + arc1 + minus.reversed(), pal.first, wash = 0.35f, gap = 3.8f, angle = -58f, lw = 1.1f)
    fill(minus + arc2 + plus.reversed(), pal.second, wash = 0.3f, gap = 4.4f, angle = 30f, lw = 1.1f)
}

private fun Pencil.cornu(v: Int) {
    // Intégrales de Fresnel, par la méthode des trapèzes.
    val ds = 0.004f
    val len = 4.2f
    val pts = ArrayList<Offset>()
    var x = 0f
    var y = 0f
    var s = 0f
    val half = ArrayList<Offset>()
    half += Offset.Zero
    while (s < len) {
        val s2 = s + ds
        x += (cos(PI.toFloat() * s * s / 2) + cos(PI.toFloat() * s2 * s2 / 2)) / 2 * ds
        y += (sin(PI.toFloat() * s * s / 2) + sin(PI.toFloat() * s2 * s2 / 2)) / 2 * ds
        half += pt(x, y)
        s = s2
    }
    pts += half.reversed().map { pt(-it.x, -it.y) }
    pts += half.drop(1)
    val flipped = pts.map { pt(it.x, -it.y) }
    val fit = Fit(listOf(flipped), 10f)
    val pal = listOf(Crayons.rainbow, Crayons.rainbow.reversed(), listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.rose))[v % 3]
    // Les deux yeux de la spirale, coloriés : la courbe depuis s = 1,2 jusqu'au bout, refermée sur son point limite.
    val m = half.size
    val from = (0.9f / ds).toInt()
    val eye = half.subList(from, m).map { pt(it.x, -it.y) } + pt(0.5f, -0.5f)
    fill(fit(eye), pal[pal.lastIndex], wash = 0.25f, gap = 3.2f, strokeAlpha = 0.6f, outline = false)
    fill(fit(eye.map { pt(-it.x, -it.y) }), pal[0], wash = 0.25f, gap = 3.2f, angle = 30f, strokeAlpha = 0.6f, outline = false)
    val drawn = fit(flipped)
    rainbowLine(drawn, pal, 2.6f)
    dot(fit(pt(0.5f, -0.5f)), 2f, Graphite)
    dot(fit(pt(-0.5f, 0.5f)), 2f, Graphite)
}

private fun Pencil.astroid(v: Int) {
    val l = 116f
    val n = if (v % 2 == 0) 14 else 20
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.vert, Crayons.violet).rot(v % 4)
    val star = curve(0f, TAU, 240) { t -> center + pt(l * cos(t).pow(3), -l * sin(t).pow(3)) }
    fill(star, Crayons.jaune, wash = 0.25f, gap = 4.4f, strokeAlpha = 0.55f, lw = 1.2f)
    // Les axes : le mur et le sol.
    stroke(center + pt(-l - 6, 0f), center + pt(l + 6, 0f), Graphite, 1f, 0.8f)
    stroke(center + pt(0f, -l - 6), center + pt(0f, l + 6), Graphite, 1f, 0.8f)
    for (q in 0 until 4) {
        val sx = if (q == 0 || q == 3) 1f else -1f
        val sy = if (q < 2) -1f else 1f
        for (i in 1 until n) {
            val t = PI.toFloat() / 2 * i / n
            stroke(center + pt(sx * l * cos(t), 0f), center + pt(0f, sy * l * sin(t)), pal[q], 1.1f, 0.85f)
        }
    }
}

private fun Pencil.cupCaustic(v: Int) {
    val r = 112f
    val rays = if (v % 2 == 0) 26 else 38
    val light = if (v % 2 == 0) Crayons.jaune else Crayons.orange
    fill(circle(center.x, center.y, r + 8, 120), Crayons.brun, wash = 0.18f, hatch = false, lw = 1.4f)
    fill(circle(center.x, center.y, r, 120), Crayons.brun, wash = 0.32f, gap = 4.6f, strokeAlpha = 0.45f, lw = 1.1f)
    // L'enveloppe : C(α) = P + (R cos α / 2)·u, sur la moitié de droite.
    val caustic = curve(-PI.toFloat() / 2, PI.toFloat() / 2, 120) { a ->
        val p = dir(a) * r
        val u = pt(-cos(2 * a), -sin(2 * a))
        center + p + u * (r * cos(a) / 2)
    }
    fill(caustic, light, wash = 0.55f, gap = 2.6f, lw = 1f)
    for (i in 1 until rays) {
        val a = -PI.toFloat() / 2 + PI.toFloat() * i / rays
        val p = center + dir(a) * r
        val entry = center + pt(-sqrt(max(0f, r * r - (p.y - center.y) * (p.y - center.y))), p.y - center.y)
        stroke(entry, p, light, 0.9f, 0.55f)
        val u = pt(-cos(2 * a), -sin(2 * a))
        stroke(p, p + u * (2 * r * cos(a) * 0.92f), Crayons.orange, 0.9f, 0.8f)
    }
    line(caustic, Crayons.rouge, 1.8f)
}

private fun Pencil.lemniscate(v: Int) {
    val pal = listOf(Crayons.bleu to Crayons.rose, Crayons.vert to Crayons.violet, Crayons.orange to Crayons.turquoise)[v % 3]
    val scales = floatArrayOf(1f, 0.78f, 0.56f, 0.34f)
    for ((i, s) in scales.withIndex()) {
        val a = 172f * s
        fun p(t: Float) = center + pt(a * cos(t) / (1 + sin(t) * sin(t)), a * sin(t) * cos(t) / (1 + sin(t) * sin(t)))
        val right = curve(-PI.toFloat() / 2, PI.toFloat() / 2, 120) { p(it) }
        val left = curve(PI.toFloat() / 2, 3 * PI.toFloat() / 2, 120) { p(it) }
        val (c1, c2) = if (i % 2 == 0) pal else pal.second to pal.first
        fill(right, c1, wash = 0.3f, gap = 3.8f, angle = -58f + 20f * i, lw = 1.1f)
        fill(left, c2, wash = 0.3f, gap = 3.8f, angle = 58f - 20f * i, lw = 1.1f)
    }
    val c = 172f / sqrt(2f)
    dot(center + pt(c, 0f), 2.6f, Graphite)
    dot(center + pt(-c, 0f), 2.6f, Graphite)
}

private fun Pencil.cassini(v: Int) {
    val c = 1f
    val ratios = floatArrayOf(1.6f, 1.4f, 1.25f, 1.12f, 1f, 0.94f, 0.85f, 0.72f)
    val shapes = ArrayList<List<List<Offset>>>()
    for (b in ratios) {
        if (b >= c) {
            shapes += listOf(curve(0f, TAU, 360) { t ->
                val c2 = cos(2 * t)
                val r2 = c * c * c2 + sqrt(max(0f, c.pow(4) * c2 * c2 + b.pow(4) - c.pow(4)))
                dir(t) * sqrt(max(0f, r2))
            })
        } else {
            val tm = 0.5f * acos(sqrt(1 - (b / c).pow(4)))
            fun r(t: Float, sign: Float): Float {
                val c2 = cos(2 * t)
                return sqrt(max(0f, c * c * c2 + sign * sqrt(max(0f, c.pow(4) * c2 * c2 + b.pow(4) - c.pow(4)))))
            }
            val right = curve(-tm, tm, 120) { dir(it) * r(it, 1f) } + curve(tm, -tm, 120) { dir(it) * r(it, -1f) }
            shapes += listOf(right, right.map { pt(-it.x, it.y) })
        }
    }
    val fit = Fit(shapes.first(), 10f)
    val pal = Crayons.rainbow.rot(v % 8)
    shapes.forEachIndexed { i, parts ->
        for (part in parts) fill(fit(part), pal[i % 8], wash = 0.3f, gap = 3.8f, angle = -58f + 16f * i, lw = 1.1f)
    }
    dot(fit(pt(c, 0f)), 2.4f, Graphite)
    dot(fit(pt(-c, 0f)), 2.4f, Graphite)
}

private fun Pencil.limacon(v: Int) {
    val ratios = floatArrayOf(2.2f, 1.7f, 1.35f, 1f, 0.75f, 0.5f)
    val flip = if (v % 2 == 1) -1f else 1f
    val shapes = ratios.map { b -> curve(0f, TAU, 300) { t -> dir(t) * (b + cos(t)) }.map { pt(it.x * flip, it.y) } }
    val fit = Fit(listOf(shapes.first()), 10f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    shapes.forEachIndexed { i, s -> fill(fit(s), pal[i], wash = 0.3f, gap = 3.8f, angle = -58f + 20f * i, lw = 1.1f) }
    dot(fit(Offset.Zero), 2.4f, Graphite)
}

private fun Pencil.cycloid(v: Int) {
    val prolate = v % 2 == 1
    val r = 1f
    val d = if (prolate) 1.5f else 1f
    val turns = if (prolate) 2 else 1
    fun p(t: Float) = pt(r * t - d * sin(t), -(r - d * cos(t)))
    val path = curve(0f, TAU * turns, 240 * turns) { p(it) }
    val ground = listOf(pt(-0.3f, 0f), pt(TAU * turns + 0.3f, 0f))
    val wheels = (0..(if (prolate) 4 else 3)).map { TAU * turns * (it + 0.5f) / (if (prolate) 5 else 4) }
    val fit = Fit(listOf(path, ground, curve(0f, TAU * turns, 10) { pt(r * it, -2 * r) }), 10f)
    val pal = listOf(Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.turquoise).rot(v % 4)
    // L'aire sous les arches, coloriée.
    fill(fit(path + listOf(pt(TAU * turns, 0f), pt(0f, 0f))), Crayons.jaune, wash = 0.3f, gap = 4f, strokeAlpha = 0.6f, outline = false)
    line(fit(ground), Graphite, 1.4f)
    wheels.forEachIndexed { i, t ->
        val c = pt(r * t, -r)
        val wheel = circle(c.x, c.y, r, 60)
        fill(fit(wheel), pal[i % pal.size], wash = 0.12f, hatch = false, lw = 1f)
        val tip = p(t)
        stroke(fit(c), fit(tip), Graphite, 1f, 0.8f)
        dot(fit(c), 1.6f, Graphite)
        dot(fit(tip), 2.6f, Crayons.rouge)
    }
    line(fit(path), Crayons.rouge, 2.2f)
}

private fun Pencil.hypocycloid(v: Int) {
    val pal = Crayons.rainbow.rot(v % 8)
    val rr = 50f
    for (idx in 0 until 6) {
        val kk = idx + 3
        val c = pt(70f + 114f * (idx % 3), 68f + 114f * (idx / 3))
        val big = circle(c.x, c.y, rr, 80)
        fill(big, pal[idx], wash = 0.1f, hatch = false, lw = 0.9f)
        val star = curve(0f, TAU, 200) { t ->
            c + pt((rr / kk) * ((kk - 1) * cos(t) + cos((kk - 1) * t)), (rr / kk) * ((kk - 1) * sin(t) - sin((kk - 1) * t)))
        }
        fill(star, pal[idx], wash = 0.38f, gap = 3f, angle = -58f + 30f * idx, lw = 1.1f)
        // Le petit cercle qui roule, à un instant donné.
        val t = rad(35f + 20f * v)
        val sc = c + dir(t) * (rr - rr / kk)
        outline(circle(sc.x, sc.y, rr / kk, 30), lw = 0.8f, color = Graphite.copy(alpha = 0.7f))
        dot(sc + pt((rr / kk) * cos((kk - 1) * t), -(rr / kk) * sin((kk - 1) * t)), 2f, Crayons.rouge)
    }
}

/**
 * Épicycloïdes emboîtées comme une fleur : un cercle de rayon R / k roule à l'extérieur d'un cercle
 * fixe de rayon R et trace k arches ; dans le cercle fixe, une épicycloïde plus petite, tournée d'une
 * demi-arche, et ainsi de suite.
 */
private fun Pencil.epicycloid(v: Int) {
    val k = intArrayOf(5, 6, 4, 7)[v % 4]
    val pal = Crayons.rainbow.rot((v * 3 + 1) % 8)
    var big = 118f / (1 + 2f / k)
    var turn = -PI.toFloat() / 2
    for (layer in 0 until 3) {
        val r = big / k
        val c = center
        val arches = curve(0f, TAU, 360) { t ->
            val x = (big + r) * cos(t) - r * cos((big + r) / r * t)
            val y = (big + r) * sin(t) - r * sin((big + r) / r * t)
            c + pt(x * cos(turn) - y * sin(turn), x * sin(turn) + y * cos(turn))
        }
        fill(arches, pal[layer * 2], wash = 0.34f, gap = 3.6f + layer * 0.3f, angle = -58f + 40f * layer, lw = 1.2f - layer * 0.15f)
        fill(circle(c.x, c.y, big, 90), pal[layer * 2 + 1], wash = 0.22f, gap = 4.6f, strokeAlpha = 0.5f, angle = 32f + 40f * layer, lw = 0.9f)
        if (layer == 0) {
            // Le cercle qui roule, arrêté entre deux pointes, et le crayon fixé sur son bord.
            val a = turn + PI.toFloat() / k
            val rc = c + dir(a) * (big + r)
            outline(circle(rc.x, rc.y, r, 36), lw = 0.9f, color = Graphite.copy(alpha = 0.75f))
            dot(rc + dir(a) * r, 2.2f, Crayons.rouge)
        }
        // L'étage suivant tient dans le cercle fixe, tourné d'une demi-arche.
        big /= 1 + 2f / k
        turn += PI.toFloat() / k
    }
    fill(circle(center.x, center.y, big * 0.5f, 24), Crayons.jaune, gap = 2.8f, lw = 1f)
}

private fun Pencil.superellipse(v: Int) {
    val square = v % 2 == 1
    val a = if (square) 116f else 170f
    val b = 116f
    val ns = floatArrayOf(8f, 3f, 2f, 1.5f, 1f, 0.5f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    ns.forEachIndexed { i, n ->
        val pts = curve(0f, TAU, 360) { t ->
            val ct = cos(t)
            val st = sin(t)
            center + pt(a * sign(ct) * abs(ct).pow(2 / n), b * sign(st) * abs(st).pow(2 / n))
        }
        fill(pts, pal[i], wash = 0.3f, gap = 3.8f, angle = -58f + 22f * i, lw = 1.1f)
    }
}

private fun Pencil.fourier(v: Int) {
    val saw = v % 2 == 1
    val x0 = 14f
    val w = 340f
    val amp = 80f
    val periods = 2
    fun f(x: Float, n: Int): Float = if (!saw) {
        (1..n).sumOf { kk -> (sin((2 * kk - 1) * x) / (2 * kk - 1)).toDouble() }.toFloat() * 4 / PI.toFloat()
    } else {
        (1..n).sumOf { kk -> ((if (kk % 2 == 1) 1 else -1) * sin(kk * x) / kk).toDouble() }.toFloat() * 2 / PI.toFloat()
    }
    // Le dessin : x de 0 à 4π sur la largeur, l'amplitude du signal idéal vaut [amp].
    fun at(x: Float, y: Float) = pt(x0 + w * x / (periods * TAU), center.y - y * amp)
    val xs = (0..800).map { periods * TAU * it / 800 }
    // L'aire entre la meilleure approximation et l'axe, coloriée par demi-période.
    val best = 25
    for (h in 0 until 2 * periods) {
        val seg = xs.filter { it >= h * PI.toFloat() && it <= (h + 1) * PI.toFloat() }
        val top = seg.map { at(it, f(it, best)) }
        val poly = top + listOf(at(seg.last(), 0f), at(seg.first(), 0f))
        fill(poly, if (h % 2 == 0) Crayons.bleu else Crayons.orange, wash = 0.2f, gap = 4.4f, strokeAlpha = 0.45f, outline = false)
    }
    line(listOf(at(0f, 0f), at(periods * TAU, 0f)), Graphite, 1f)
    val terms = intArrayOf(1, 2, 3, 5, 25)
    val pal = listOf(Crayons.rouge, Crayons.orange, Crayons.vert, Crayons.violet, Crayons.marine)
    terms.forEachIndexed { i, n -> line(xs.map { at(it, f(it, n)) }, pal[i], if (n == 25) 1.8f else 1.4f, 0.9f) }
}

private fun Pencil.stringArt(v: Int) {
    val m = Curves.corners(v)
    val n = 18
    val rr = if (m == 3) 128f else 118f
    val o = if (m == 3) center + pt(0f, 18f) else center
    val turn = if (m == 4) -PI.toFloat() / 4 else -PI.toFloat() / 2
    val corners = (0 until m).map { o + dir(turn + TAU * it / m) * rr }
    val pal = Crayons.rainbow.rot(v % 8).let { if (m == 4) listOf(it[0], it[2], it[4], it[6]) else it }
    outline(corners, lw = 1.2f)
    for (i in 0 until m) {
        val a = corners[(i + m - 1) % m]
        val c = corners[i]
        val b = corners[(i + 1) % m]
        // La parabole enveloppe : la courbe de Bézier (a, c, b), et le coin qu'elle découpe, colorié.
        val bez = curve(0f, 1f, 40) { t -> a * ((1 - t) * (1 - t)) + c * (2 * t * (1 - t)) + b * (t * t) }
        fill(bez + c, pal[i % pal.size], wash = 0.22f, hatch = false, outline = false)
        for (j in 0..n) {
            val t = j.toFloat() / n
            stroke(a + (c - a) * t, c + (b - c) * t, pal[i % pal.size], 1f, 0.9f)
        }
    }
    for (c in corners) dot(c, 2f, Graphite)
}

private fun Pencil.heart(v: Int) {
    fun p(t: Float) = pt(16 * sin(t).pow(3), -(13 * cos(t) - 5 * cos(2 * t) - 2 * cos(3 * t) - cos(4 * t)))
    val base = curve(0f, TAU, 300) { p(it) }
    val fit = Fit(listOf(base), 8f)
    val pal = listOf(
        listOf(Crayons.rouge, Crayons.rose),
        listOf(Crayons.violet, Crayons.rose),
        listOf(Crayons.rouge, Crayons.orange),
    )[v % 3]
    // Centre de réduction : un peu au-dessus du milieu, pour que les cœurs restent emboîtés.
    val c = pt(0f, -1.5f)
    floatArrayOf(1f, 0.8f, 0.6f, 0.4f, 0.2f).forEachIndexed { i, s ->
        fill(fit(base.map { c + (it - c) * s }), pal[i % 2], wash = if (i % 2 == 0) 0.38f else 0.25f, gap = 3.6f, angle = -58f + 24f * i, lw = 1.2f)
    }
}

private fun Pencil.baravelle(v: Int) {
    val side = 234f
    var sq = listOf(pt(-1f, -1f), pt(1f, -1f), pt(1f, 1f), pt(-1f, 1f)).map { center + it * (side / 2) }
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange, Crayons.turquoise, Crayons.rose).rot((v * 2) % 8).take(4)
    val dirSign = if (v % 2 == 0) 1 else 3
    var level = 0
    while ((sq[0] - sq[1]).getDistance() > 5f) {
        val mids = (0 until 4).map { (sq[it] + sq[(it + 1) % 4]) / 2f }
        val big = (sq[0] - sq[1]).getDistance() > 26f
        for (i in 0 until 4) {
            // Le triangle du coin sq[i + 1], entre les milieux des deux côtés qui s'y rejoignent.
            val tri = listOf(mids[i], sq[(i + 1) % 4], mids[(i + 1) % 4])
            fill(tri, pal[(i + dirSign * level) % 4], hatch = big, wash = if (level % 2 == 0) 0.5f else 0.3f, gap = 3.2f, angle = -58f + 90f * i + 8f * level, lw = if (big) 1.1f else 0.7f)
        }
        sq = mids
        level++
    }
}

private fun Pencil.triskele(v: Int) {
    val mirror = if (v % 2 == 1) -1f else 1f
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange).rot(v % 6)
    val d = 54f
    val turns = 1.75f
    for (j in 0 until 3) {
        val alpha = rad(-90f + 120f * j)
        val c = center + dir(alpha) * d
        val phi0 = alpha + PI.toFloat()
        // Le bras : une spirale qui part du centre et s'enroule vers c, son rayon diminuant régulièrement.
        val path = curve(0f, 1f, 160) { s ->
            val phi = phi0 + mirror * TAU * turns * s
            c + dir(phi) * (d * (1 - s))
        }
        val width = { s: Float -> 15f * (1 - s) + 3.5f }
        val left = ArrayList<Offset>()
        val right = ArrayList<Offset>()
        for (i in path.indices) {
            val a = path[max(0, i - 1)]
            val b = path[min(path.lastIndex, i + 1)]
            val t = b - a
            val len = t.getDistance().coerceAtLeast(1e-3f)
            val nrm = pt(-t.y / len, t.x / len)
            val w = width(i / path.lastIndex.toFloat()) / 2
            left += path[i] + nrm * w
            right += path[i] - nrm * w
        }
        fill(left + right.reversed(), pal[j * 2 % pal.size], wash = 0.4f, gap = 3f, angle = -58f + 120f * j, lw = 1.2f)
        dot(c, 3.2f, Graphite)
    }
    fill(circle(center.x, center.y, 9f, 24), pal[1], gap = 2.6f, lw = 1.1f)
}
