package fr.perso.nimbus.ui.unlock

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fr.perso.nimbus.data.DailyFigure
import fr.perso.nimbus.data.DailyFigures
import fr.perso.nimbus.data.FigureKind
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Figure du jour de la page de déverrouillage, dessinée au crayon : chaque forme reçoit un lavis pâle de
// sa couleur, des hachures obliques un peu de travers et un contour au crayon de papier repassé deux
// fois. Les figures sont composées sur [FW] × [FH] unités, puis mises à l'échelle de leur case.

/** Taille de composition des figures, en unités. */
internal const val FW = 368f
internal const val FH = 250f

/** Le crayon de papier des contours. */
private val Graphite = Color(0xFF2E3644)

/** La boîte de crayons de couleur. */
private object Crayons {
    val rouge = Color(0xFFE4572E)
    val orange = Color(0xFFF08A3C)
    val jaune = Color(0xFFF6C445)
    val vert = Color(0xFF5DAE5B)
    val sapin = Color(0xFF2F8A57)
    val turquoise = Color(0xFF33B5A8)
    val bleu = Color(0xFF4A8BD6)
    val marine = Color(0xFF2F5DA8)
    val violet = Color(0xFF8C68C6)
    val rose = Color(0xFFE8779E)
    val brun = Color(0xFF9A5B34)
    val rainbow = listOf(rouge, orange, jaune, vert, turquoise, bleu, violet, rose)
}

/** La figure [figure], centrée dans sa case et à son échelle (le rapport [FW] × [FH] est gardé). */
@Composable
internal fun FigureArt(figure: DailyFigure, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = figure.kind.title }) {
        val k = min(size.width / FW, size.height / FH)
        val pencil = Pencil(this, Random(figure.kind.ordinal * 101 + figure.variant * 7 + 17), k, (size.width - FW * k) / 2f, (size.height - FH * k) / 2f)
        pencil.draw(figure.kind, figure.variant)
    }
}

private fun pt(x: Float, y: Float) = Offset(x, y)
private fun rad(d: Float) = d * PI.toFloat() / 180f
private fun dir(a: Float) = Offset(cos(a), sin(a))
private fun <T> List<T>.rot(k: Int): List<T> = List(size) { this[(it + k) % size] }

private fun circle(cx: Float, cy: Float, r: Float, n: Int = max(18, (r * 1.2f).roundToInt())): List<Offset> =
    List(n) { i -> val a = 2 * PI.toFloat() * i / n; pt(cx + r * cos(a), cy + r * sin(a)) }

/** Cadre des polygones dans la case, avec [margin] unités autour. */
private class Fit(polys: List<List<Offset>>, margin: Float) {
    val s: Float
    private val ox: Float
    private val oy: Float

    init {
        var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
        for (p in polys) for (o in p) { x0 = min(x0, o.x); y0 = min(y0, o.y); x1 = max(x1, o.x); y1 = max(y1, o.y) }
        s = min((FW - 2 * margin) / (x1 - x0), (FH - 2 * margin) / (y1 - y0))
        ox = (FW - (x1 - x0) * s) / 2 - x0 * s
        oy = (FH - (y1 - y0) * s) / 2 - y0 * s
    }

    operator fun invoke(o: Offset) = pt(o.x * s + ox, o.y * s + oy)
    operator fun invoke(p: List<Offset>) = p.map { this(it) }
}

/** Le crayon : il dessine en unités de figure ([k] pixels l'unité, décalé de [ox], [oy]). */
private class Pencil(val scope: DrawScope, val random: Random, val k: Float, val ox: Float, val oy: Float) {
    fun px(o: Offset) = Offset(ox + o.x * k, oy + o.y * k)
    fun j(v: Float) = (random.nextFloat() - 0.5f) * v

    fun path(pts: List<Offset>, close: Boolean = true, wobble: Float = 0f) = Path().apply {
        pts.forEachIndexed { i, o ->
            val p = px(Offset(o.x + j(wobble), o.y + j(wobble)))
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        if (close) close()
    }

    /** Lavis pâle, hachures obliques un peu de travers, contour au crayon de papier. */
    fun fill(
        pts: List<Offset>,
        color: Color,
        hatch: Boolean = true,
        angle: Float = -58f,
        gap: Float = 4.2f,
        wash: Float = 0.32f,
        outline: Boolean = true,
        lw: Float = 1.3f,
        strokeAlpha: Float = 0.8f,
    ) = with(scope) {
        val shape = path(pts)
        drawPath(shape, color, alpha = wash)
        if (hatch) {
            var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
            for (o in pts) { x0 = min(x0, o.x); y0 = min(y0, o.y); x1 = max(x1, o.x); y1 = max(y1, o.y) }
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
        if (outline) outline(pts, true, lw)
    }

    /** Contour repassé deux fois, un peu de travers. */
    fun outline(pts: List<Offset>, close: Boolean = true, lw: Float = 1.3f, color: Color = Graphite) = with(scope) {
        drawPath(path(pts, close, 0.7f), color, style = Stroke(lw * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path(pts, close, 0.7f), color, alpha = 0.5f, style = Stroke(lw * 0.7f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    /** Trait de crayon de couleur, repassé. */
    fun line(pts: List<Offset>, color: Color, lw: Float = 1.4f, alpha: Float = 1f) = with(scope) {
        drawPath(path(pts, false, 0.8f), color, alpha = alpha, style = Stroke(lw * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path(pts, false, 0.8f), color, alpha = alpha * 0.5f, style = Stroke(lw * 0.7f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    /** Trait court (une corde), d'un seul passage. */
    fun stroke(a: Offset, b: Offset, color: Color, lw: Float, alpha: Float) = scope.drawLine(
        color,
        px(Offset(a.x + j(0.8f), a.y + j(0.8f))),
        px(Offset(b.x + j(0.8f), b.y + j(0.8f))),
        strokeWidth = lw * k,
        cap = StrokeCap.Round,
        alpha = alpha,
    )

    fun dot(c: Offset, r: Float, color: Color, alpha: Float = 1f) = scope.drawCircle(color, r * k, px(c), alpha = alpha)

    fun draw(kind: FigureKind, v: Int) = when (kind) {
        FigureKind.ROSACE -> rosace(v)
        FigureKind.GOLDEN_SPIRAL -> goldenSpiral(v)
        FigureKind.THEODORUS -> theodorus(v)
        FigureKind.SUNFLOWER -> sunflower(v)
        FigureKind.SIERPINSKI -> sierpinski(v)
        FigureKind.POLAR_ROSE -> polarRose(v)
        FigureKind.CARDIOID -> cardioid(v)
        FigureKind.WHIRL -> whirl(v)
        FigureKind.SPIROGRAPH -> spirograph(v)
        FigureKind.COMPASS_ROSE -> compassRose(v)
        FigureKind.KOCH -> koch(v)
        FigureKind.DRAGON -> dragon(v)
        FigureKind.APOLLONIUS -> apollonius(v)
        FigureKind.LISSAJOUS -> lissajous(v)
        FigureKind.CARPET -> carpet(v)
        FigureKind.HILBERT -> hilbert(v)
        FigureKind.PYTHAGORAS -> pythagoras(v)
    }

    // ------------------------------------------------------------ Les figures

    private val center = pt(FW / 2, FH / 2)

    /** Pétale entre O et O + r·u(a) : deux arcs de rayon r centrés à 60° de part et d'autre (le compas). */
    private fun lens(o: Offset, r: Float, a: Float, n: Int = 16): List<Offset> {
        val q1 = o + dir(a + rad(60f)) * r
        val q2 = o + dir(a - rad(60f)) * r
        return (0..n).map { q1 + dir(a + rad(240f + 60f * it / n)) * r } + (1 until n).map { q2 + dir(a + rad(60f + 60f * it / n)) * r }
    }

    private fun rosace(v: Int) {
        val r = 92f
        val pal = listOf(Crayons.rose, Crayons.violet, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.orange).rot(v % 6)
        val a0 = rad(-90f + (v % 2) * 30f)
        fill(circle(center.x, center.y, r + 17, 90), Crayons.jaune, wash = 0.18f, strokeAlpha = 0.35f, gap = 5f)
        for (i in 0 until 12) {
            val c = center + dir(a0 + rad(15f + 30f * i)) * (r + 17)
            fill(circle(c.x, c.y, 7f, 22), pal[i % 6], gap = 3.2f, lw = 1.1f)
        }
        for (i in 0 until 6) fill(lens(center, r, a0 + rad(60f * i)), pal[i], angle = -58f + 60 * i)
        for (i in 0 until 6) fill(lens(center, r * 0.5f, a0 + rad(30f + 60f * i)), Crayons.jaune, gap = 3.4f, angle = 30f + 60 * i, lw = 1.1f)
        fill(circle(center.x, center.y, 9f, 24), Crayons.rouge, gap = 3f)
    }

    private fun goldenSpiral(v: Int) {
        val fib = intArrayOf(1, 1, 2, 3, 5, 8, 13, 21)
        val squares = mutableListOf(Triple(0f, 0f, 1f))
        var bx0 = 0f; var by0 = 0f; var bx1 = 1f; var by1 = 1f
        for (i in 1 until fib.size) {
            val f = fib[i].toFloat()
            val (x, y) = when ((i - 1) % 4) {
                0 -> bx1 to by0
                1 -> bx0 to by1
                2 -> bx0 - f to by0
                else -> bx0 to by0 - f
            }
            squares += Triple(x, y, f)
            bx0 = min(bx0, x); by0 = min(by0, y); bx1 = max(bx1, x + f); by1 = max(by1, y + f)
        }
        val flip = if (v % 2 == 1) -1f else 1f
        val polys = squares.map { (x, y, f) -> listOf(pt(x, y), pt(x + f, y), pt(x + f, y + f), pt(x, y + f)).map { pt(it.x * flip, it.y) } }
        val fit = Fit(polys, 8f)
        val pal = Crayons.rainbow.rot((v * 3) % 8).let { listOf(it[2], it[1], it[0], it[7], it[6], it[5], it[4], it[3]) }
        polys.forEachIndexed { i, p -> fill(fit(p), pal[i], angle = floatArrayOf(-58f, 32f, -32f, 58f)[i % 4], gap = 4f + i * 0.15f) }

        // Les arcs : chacun relie deux coins opposés de son carré, en partant de la fin du précédent.
        val corners = polys.map { fit(it) }
        fun same(a: Offset, b: Offset) = (a - b).getDistance() < 0.5f
        var arcs: List<Triple<Offset, Offset, List<Offset>>>? = null
        for (start in 0 until 4) {
            var e = corners[0][start]
            val list = mutableListOf<Triple<Offset, Offset, List<Offset>>>()
            var ok = true
            for (i in corners.indices) {
                val cs = corners[i]
                val at = cs.indexOfFirst { same(it, e) }
                if (at < 0) { ok = false; break }
                val opp = cs[(at + 2) % 4]
                if (i + 1 < corners.size && corners[i + 1].none { same(it, opp) }) { ok = false; break }
                list += Triple(e, opp, listOf(cs[(at + 1) % 4], cs[(at + 3) % 4]))
                e = opp
            }
            if (ok) { arcs = list; break }
        }
        val heart = corners[1].reduce { a, b -> a + b } / 4f
        val spiral = mutableListOf<Offset>()
        for ((a, b, candidates) in arcs ?: return) {
            val q = candidates.minBy { (it - heart).getDistance() }
            val r = (a - q).getDistance()
            val a1 = atan2(a.y - q.y, a.x - q.x)
            var da = atan2(b.y - q.y, b.x - q.x) - a1
            while (da > PI) da -= 2 * PI.toFloat()
            while (da < -PI) da += 2 * PI.toFloat()
            for (i in 0..14) spiral += q + dir(a1 + da * i / 14) * r
        }
        line(spiral, Graphite, 2.4f)
    }

    private fun theodorus(v: Int) {
        val pts = mutableListOf(pt(1f, 0f))
        repeat(16) {
            val p = pts.last()
            val len = p.getDistance()
            pts += p + Offset(-p.y, p.x) / len
        }
        val flip = if (v % 2 == 1) -1f else 1f
        val tris = (0 until 16).map { listOf(Offset.Zero, pts[it], pts[it + 1]).map { o -> pt(o.x, o.y * flip) } }
        val fit = Fit(tris, 6f)
        val pal = Crayons.rainbow.rot(v % 8)
        tris.forEachIndexed { i, t -> fill(fit(t), pal[i % 8], angle = -58f + 23f * i, gap = 3.8f, lw = 1.1f) }
    }

    private fun sunflower(v: Int) {
        val o = pt(FW / 2, FH / 2 + 2)
        val colors = listOf(Crayons.jaune to Crayons.orange, Crayons.rouge to Crayons.orange, Crayons.rose to Crayons.violet, Crayons.orange to Crayons.jaune)[v % 4]
        for (layer in 0..1) {
            val r0 = 70f; val r1 = if (layer == 1) 112f else 120f; val w = if (layer == 1) 9f else 11f
            val side = (0..10).map { val t = it / 10f; pt(r0 + (r1 - r0) * t, w * sin(PI.toFloat() * t.pow(0.8f))) }
            val outline = side + side.reversed().map { pt(it.x, -it.y) }
            for (i in 0 until 34) {
                val a = rad(360f * (i + layer * 0.5f) / 34)
                val petal = outline.map { pt(o.x + it.x * cos(a) - it.y * sin(a), o.y + it.x * sin(a) + it.y * cos(a)) }
                fill(petal, if (layer == 0) colors.first else colors.second, angle = 360f * i / 34 + 90, gap = 3.4f, lw = 1f)
            }
        }
        fill(circle(o.x, o.y, 74f, 80), Crayons.brun, wash = 0.45f, hatch = false, lw = 1.2f)
        val seeds = listOf(Color(0xFF5B3217), Color(0xFFB5652A), Color(0xFF7A4520))
        val n = 300
        val c = 70f / sqrt(n.toFloat())
        for (i in 1..n) {
            val r = c * sqrt(i.toFloat())
            val a = i * rad(137.508f)
            dot(o + dir(a) * r, 1.2f + 2.4f * sqrt(i.toFloat() / n), seeds[(i % 21) % 3], 0.9f)
        }
    }

    private fun sierpinski(v: Int) {
        val depth = DailyFigures.sierpinskiDepth(v)
        val base = 272f
        val h = base * sqrt(3f) / 2
        val a = pt(FW / 2, (FH - h) / 2); val b = pt(FW / 2 - base / 2, (FH + h) / 2); val c = pt(FW / 2 + base / 2, (FH + h) / 2)
        val pal = Crayons.rainbow.rot(v % 8)
        val tris = mutableListOf<List<Offset>>()
        fun go(p: Offset, q: Offset, r: Offset, d: Int) {
            if (d == 0) { tris += listOf(p, q, r); return }
            val pq = (p + q) / 2f; val qr = (q + r) / 2f; val rp = (r + p) / 2f
            go(p, pq, rp, d - 1); go(pq, q, qr, d - 1); go(rp, qr, r, d - 1)
        }
        go(a, b, c, depth)
        outline(listOf(a, b, c), lw = 1f)
        for (t in tris) {
            val m = (t[0] + t[1] + t[2]) / 3f
            val i = min(7, floor(((m.x - b.x) / base * 0.6f + (m.y - a.y) / h * 0.4f) * 8).toInt())
            fill(t, pal[i], gap = if (depth == 4) 3.6f else 2.8f, lw = if (depth == 4) 1.1f else 0.8f, wash = 0.42f)
        }
    }

    private fun polarRose(v: Int) {
        val n = DailyFigures.petals(v)
        val pal = Crayons.rainbow.rot((v * 3) % 8)
        fun petal(phi: Float, r: Float, w: Float) = (0..24).map {
            val t = phi - w + 2 * w * it / 24
            center + dir(t) * (r * cos((PI.toFloat() / 2) * (t - phi) / w))
        }
        val step = 2 * PI.toFloat() / n
        for (i in 0 until n) fill(petal(-PI.toFloat() / 2 + step * i, 116f, step / 2 * 0.95f), pal[i % 2 * 4], angle = -58f + 360f * i / n)
        for (i in 0 until n) fill(petal(-PI.toFloat() / 2 + step * (i + 0.5f), 70f, step / 2 * 0.8f), pal[2], gap = 3.4f, angle = 30f + 360f * i / n, lw = 1.1f)
        fill(circle(center.x, center.y, 13f, 28), Crayons.jaune, gap = 3f)
    }

    private fun cardioid(v: Int) {
        val m = DailyFigures.cardioidTable(v)
        val n = 144
        val r = 114f
        val pal = Crayons.rainbow + Crayons.rouge
        fill(circle(center.x, center.y, r, 120), Crayons.jaune, wash = 0.12f, hatch = false, lw = 1.2f)
        fun p(i: Int) = center + dir(PI.toFloat() + 2 * PI.toFloat() * i / n) * r
        for (i in 1 until n) {
            val c = pal[floor(i.toFloat() / n * (pal.size - 1)).toInt()]
            stroke(p(i), p((m * i) % n), c, 1.05f, 0.75f)
        }
        for (i in 0 until n step 2) dot(p(i), 0.9f, Graphite, 0.7f)
    }

    private fun whirl(v: Int) {
        val side = 232f
        var sq = listOf(pt(-1f, -1f), pt(1f, -1f), pt(1f, 1f), pt(-1f, 1f)).map { center + it * (side / 2) }
        val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange, Crayons.turquoise, Crayons.rose).rot((v * 2) % 8).take(4)
        var level = 0
        while ((sq[0] - sq[1]).getDistance() > 5f) {
            val next = (0 until 4).map { sq[it] + (sq[(it + 1) % 4] - sq[it]) * 0.1f }
            for (i in 0 until 4) {
                val tri = listOf(next[i], sq[(i + 1) % 4], next[(i + 1) % 4])
                val big = (sq[0] - sq[1]).getDistance() > 30f
                fill(tri, pal[i], hatch = big, wash = if (level % 2 == 0) 0.55f else 0.3f, gap = 3.2f, angle = -58f + 90f * i + level * 6f, lw = if (big) 1f else 0.7f)
            }
            sq = next
            level++
        }
    }

    private fun spirograph(v: Int) {
        val (ring, wheel) = DailyFigures.spirograph(v)
        val pal = listOf(Crayons.rose, Crayons.bleu, Crayons.orange, Crayons.violet, Crayons.turquoise).rot(v % 5)
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val turns = wheel / gcd(ring, wheel)
        val big = (ring - wheel).toFloat()
        val s = 116f / ring
        floatArrayOf(1f, 0.72f, 0.44f).forEachIndexed { i, dk ->
            val d = wheel * dk
            val steps = 360 * turns
            val pts = (0..steps).map {
                val t = 2 * PI.toFloat() * turns * it / steps
                center + Offset(big * cos(t) + d * cos(big / wheel * t), big * sin(t) - d * sin(big / wheel * t)) * s
            }
            line(pts, pal[i], 1.7f, 0.9f)
        }
        fill(circle(center.x, center.y, 6f, 18), Crayons.jaune, gap = 2.4f, lw = 1f)
    }

    private fun compassRose(v: Int) {
        val pal = listOf(Crayons.rouge, Crayons.marine, Crayons.sapin, Crayons.violet).rot(v % 4)
        fill(circle(center.x, center.y, 92f, 90), Crayons.jaune, wash = 0.16f, strokeAlpha = 0.35f, gap = 5f, lw = 1.1f)
        fill(circle(center.x, center.y, 76f, 80), Crayons.jaune, wash = 0.12f, hatch = false, lw = 0.9f)
        // Les pointes : les petites derrière, les cardinales devant ; chaque pointe en deux moitiés, l'une ombrée.
        fun point(a: Float, length: Float, inner: Float, spread: Float, color: Color, lw: Float) {
            val tip = center + dir(a) * length
            val left = center + dir(a - spread) * inner
            val right = center + dir(a + spread) * inner
            fill(listOf(center, left, tip), color, wash = 0.2f, gap = 6f, strokeAlpha = 0.45f, angle = a * 180 / PI.toFloat() + 30, lw = lw)
            fill(listOf(center, tip, right), color, wash = 0.5f, gap = 2.8f, angle = a * 180 / PI.toFloat() - 30, lw = lw)
        }
        for (i in 0 until 8) point(rad(-90f + 22.5f + 45f * i), 62f, 12f, rad(22.5f), pal[3], 0.9f)
        for (i in 0 until 4) point(rad(-45f + 90f * i), 86f, 20f, rad(45f), pal[1], 1.1f)
        for (i in 0 until 4) point(rad(-90f + 90f * i), 118f, 22f, rad(45f), if (i == 0) pal[0] else pal[2], 1.3f)
        fill(circle(center.x, center.y, 7f, 20), Crayons.jaune, gap = 2.6f, lw = 1.1f)
    }

    private fun koch(v: Int) {
        val o = pt(FW / 2, FH / 2 + 6)
        fun flake(rho: Float, depth: Int, turn: Float): List<Offset> {
            var pts = (0 until 3).map { o + dir(rad(-90f + 120f * it + turn)) * rho }
            repeat(depth) {
                val next = ArrayList<Offset>(pts.size * 4)
                for (i in pts.indices) {
                    val p = pts[i]; val q = pts[(i + 1) % pts.size]
                    val d = (q - p) / 3f
                    val a = p + d; val b = p + d * 2f
                    val c = a + Offset(d.x * cos(rad(-60f)) - d.y * sin(rad(-60f)), d.x * sin(rad(-60f)) + d.y * cos(rad(-60f)))
                    next += p; next += a; next += c; next += b
                }
                pts = next
            }
            return pts
        }
        val pal = listOf(Crayons.bleu, Crayons.violet, Crayons.turquoise, Crayons.rose).rot(v % 4)
        fill(flake(118f, 4, 0f), pal[0], gap = 4.4f, lw = 1.1f)
        fill(flake(80f, 3, 60f), pal[1], gap = 3.8f, angle = 30f, lw = 1.1f)
        fill(flake(48f, 2, 0f), pal[2], gap = 3.4f, angle = -20f, lw = 1f)
        fill(flake(20f, 1, 60f), Crayons.jaune, gap = 2.8f, lw = 1f)
    }

    private fun dragon(v: Int) {
        // Les plis : à chaque étape, la suite, un pli à droite, puis la suite à l'envers et retournée.
        var turns = listOf<Int>()
        repeat(10) { turns = turns + 1 + turns.reversed().map { -it } }
        var d = 0
        var p = Offset.Zero
        val pts = mutableListOf(p)
        val steps = listOf(pt(1f, 0f), pt(0f, 1f), pt(-1f, 0f), pt(0f, -1f))
        for (t in listOf(0) + turns) {
            d = Math.floorMod(d + t, 4)
            p += steps[d]
            pts += p
        }
        val fit = Fit(listOf(pts), 10f)
        val drawn = fit(pts)
        val pal = Crayons.rainbow.rot(v % 8)
        val chunk = drawn.size / 8
        for (i in 0 until 8) line(drawn.subList(i * chunk, min(drawn.size, (i + 1) * chunk + 1)), pal[i], 1.9f)
    }

    private fun apollonius(v: Int) {
        class Disc(val k: Float, val x: Float, val y: Float)
        val s = 118f
        val r1 = 2 * sqrt(3f) - 3
        val outer = Disc(-1f, 0f, 0f)
        val inner = (0 until 3).map { val a = rad(-90f + 120f * it); val k = 1 / r1; Disc(k, (1 - r1) * cos(a) * k, (1 - r1) * sin(a) * k) }
        val middle = Disc(3 / r1 + 2 * sqrt(3f) / r1, 0f, 0f)
        val discs = mutableListOf<Disc>().apply { addAll(inner); add(middle) }
        val maxK = s / 2.2f
        fun go(a: Disc, b: Disc, c: Disc, d: Disc) {
            val k = 2 * (a.k + b.k + c.k) - d.k
            if (k > maxK) return
            val e = Disc(k, 2 * (a.x + b.x + c.x) - d.x, 2 * (a.y + b.y + c.y) - d.y)
            discs += e
            go(a, b, e, c); go(a, c, e, b); go(b, c, e, a)
        }
        val (a, b, c) = inner
        go(a, b, middle, c); go(a, c, middle, b); go(b, c, middle, a)
        go(outer, a, b, c); go(outer, a, c, b); go(outer, b, c, a)
        val pal = listOf(Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.rose, Crayons.violet).rot(v % 8)
        fill(circle(center.x, center.y, s, 140), Crayons.jaune, wash = 0.14f, strokeAlpha = 0.3f, gap = 5.5f, lw = 1.5f)
        for (disc in discs.sortedBy { it.k }) {
            val r = s / disc.k
            val shade = pal[min(7, floor(ln(disc.k * r1) / ln(2f) * 1.6f).toInt().coerceAtLeast(0))]
            fill(
                circle(center.x + s * disc.x / disc.k, center.y + s * disc.y / disc.k, r),
                shade,
                hatch = r > 6,
                gap = (r / 5).coerceIn(2.4f, 4.4f),
                lw = if (r > 10) 1.1f else 0.7f,
                wash = if (r > 6) 0.3f else 0.55f,
            )
        }
    }

    private fun lissajous(v: Int) {
        val (a, b) = DailyFigures.lissajous(v)
        val pal = Crayons.rainbow.rot((v * 3) % 8)
        // Déphasage π / 2ab : la courbe ne repasse jamais sur elle-même (à π / 4, 3 : 2 se replie en une seule boucle).
        val phase = PI.toFloat() / (2 * a * b)
        val steps = 720
        val pts = (0..steps).map {
            val t = 2 * PI.toFloat() * it / steps
            center + Offset(160f * sin(a * t + phase), 112f * sin(b * t))
        }
        val chunk = steps / 6
        for (i in 0 until 6) line(pts.subList(i * chunk, min(pts.size, (i + 1) * chunk + 1)), pal[i], 2f)
    }

    private fun carpet(v: Int) {
        val side = 234f
        val pal = Crayons.rainbow.rot(v % 8)
        val cells = mutableListOf<Pair<Offset, Float>>()
        fun go(o: Offset, s: Float, d: Int) {
            if (d == 0) { cells += o to s; return }
            val t = s / 3
            for (i in 0..2) for (j in 0..2) if (i != 1 || j != 1) go(o + Offset(i * t, j * t), t, d - 1)
        }
        val o = center - Offset(side / 2, side / 2)
        go(o, side, 3)
        outline(listOf(o, o + Offset(side, 0f), o + Offset(side, side), o + Offset(0f, side)), lw = 1f)
        for ((c, s) in cells) {
            val m = (c.x - o.x + c.y - o.y + s) / (2 * side)
            fill(listOf(c, c + Offset(s, 0f), c + Offset(s, s), c + Offset(0f, s)), pal[min(7, (m * 8).toInt())], gap = 2.6f, lw = 0.6f, wash = 0.45f)
        }
    }

    private fun hilbert(v: Int) {
        val n = 32
        // Case de rang i le long de la courbe (algorithme classique d2xy).
        fun cell(i: Int): Offset {
            var x = 0; var y = 0; var t = i; var s = 1
            while (s < n) {
                val rx = 1 and (t / 2)
                val ry = 1 and (t xor rx)
                if (ry == 0) {
                    if (rx == 1) { x = s - 1 - x; y = s - 1 - y }
                    val tmp = x; x = y; y = tmp
                }
                x += s * rx; y += s * ry
                t /= 4; s *= 2
            }
            return pt(x.toFloat(), y.toFloat())
        }
        val step = 234f / n
        val o = center - Offset(117f - step / 2, 117f - step / 2)
        val pts = (0 until n * n).map { o + cell(it) * step }
        val pal = Crayons.rainbow.rot(v % 8)
        val chunk = pts.size / 8
        for (i in 0 until 8) line(pts.subList(i * chunk, min(pts.size, (i + 1) * chunk + 1)), pal[i], 1.7f)
    }

    private fun pythagoras(v: Int) {
        val alpha = rad(DailyFigures.treeAngle(v))
        val squares = mutableListOf<Pair<List<Offset>, Int>>()
        fun go(p0: Offset, p1: Offset, d: Int) {
            val e = p1 - p0
            val p3 = p0 + Offset(e.y, -e.x); val p2 = p1 + Offset(e.y, -e.x)
            squares += listOf(p0, p1, p2, p3) to d
            if (d == 9) return
            val len = e.getDistance() * cos(alpha)
            val apex = p3 + dir(atan2(e.y, e.x) - alpha) * len
            go(p3, apex, d + 1); go(apex, p2, d + 1)
        }
        go(Offset.Zero, pt(1f, 0f), 0)
        val fit = Fit(squares.map { it.first }, 6f)
        val shrink = max(cos(alpha), sin(alpha))
        for ((q, d) in squares) {
            val color = when {
                d < 3 -> Crayons.brun
                d < 6 -> Crayons.sapin
                d < 8 -> Crayons.vert
                v % 2 == 1 -> Crayons.rose
                else -> Crayons.jaune
            }
            val side = fit.s * shrink.pow(d)
            fill(fit(q), color, hatch = side > 7, outline = side > 6, gap = 3f, lw = max(0.6f, 1.3f - d * 0.08f), wash = if (side > 7) 0.35f else 0.75f, angle = -58f + d * 12)
        }
    }
}

/** Pour les captures : la figure seule. */
@Composable
fun FigureArtPreview(figure: DailyFigure, modifier: Modifier = Modifier) = FigureArt(figure, modifier)
