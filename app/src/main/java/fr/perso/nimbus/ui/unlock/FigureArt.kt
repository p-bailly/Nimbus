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
internal val Graphite = Color(0xFF2E3644)

/** La boîte de crayons de couleur. */
internal object Crayons {
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
internal fun FigureArt(figure: DailyFigure, modifier: Modifier = Modifier) = FigureArt(figure.spec, figure.variant, modifier)

@Composable
internal fun FigureArt(spec: FigureSpec, variant: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = spec.title }) {
        val k = min(size.width / FW, size.height / FH)
        val pencil = Pencil(this, Random(spec.key.hashCode() + variant * 7 + 17), k, (size.width - FW * k) / 2f, (size.height - FH * k) / 2f)
        spec.draw(pencil, variant)
    }
}

internal fun pt(x: Float, y: Float) = Offset(x, y)
internal fun rad(d: Float) = d * PI.toFloat() / 180f
internal fun dir(a: Float) = Offset(cos(a), sin(a))
internal fun <T> List<T>.rot(k: Int): List<T> = List(size) { this[(it + k) % size] }

internal fun circle(cx: Float, cy: Float, r: Float, n: Int = max(18, (r * 1.2f).roundToInt())): List<Offset> =
    List(n) { i -> val a = 2 * PI.toFloat() * i / n; pt(cx + r * cos(a), cy + r * sin(a)) }

/** Cadre des polygones dans la case, avec [margin] unités autour. */
internal class Fit(polys: List<List<Offset>>, margin: Float) {
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
internal class Pencil(val scope: DrawScope, val random: Random, val k: Float, val ox: Float, val oy: Float) {
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

    /** Le centre de la case. */
    val center = pt(FW / 2, FH / 2)
}

