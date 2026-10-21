package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Figures du thème « Cercles » : chaînes de Pappus et de Steiner, cercles de Ford, fleur de vie,
// empilement de disques, arbelos, spirale de Doyle.

internal val circleFigures: List<FigureSpec> = listOf(
    FigureSpec("pappus", "Chaîne de Pappus", "Cercles", { _ -> "" }) { pappus(it) },
    FigureSpec("steiner", "Chaîne de Steiner", "Cercles", { _ -> "" }) { steiner(it) },
    FigureSpec("ford", "Cercles de Ford", "Cercles", { _ -> "" }) { ford(it) },
    FigureSpec("flower_of_life", "Fleur de vie", "Cercles", { _ -> "" }) { flowerOfLife(it) },
    FigureSpec("circle_packing", "Empilement de disques", "Cercles", { _ -> "" }) { circlePacking(it) },
    FigureSpec("arbelos", "Arbelos", "Cercles", { _ -> "" }) { arbelos(it) },
    FigureSpec("doyle", "Spirale de Doyle", "Cercles", { _ -> "" }) { doyle(it) },
)

private val TAU = 2 * PI.toFloat()

/** Rapport AC / AB des arbelos (Pappus, arbelos), selon la variante. */
private fun ratio(v: Int) = floatArrayOf(0.62f, 0.55f, 0.68f, 0.5f)[v % 4]

/**
 * Chaîne de Pappus dans l'arbelos de rapport r (AB = 1, A à l'origine) : le n-ième cercle a pour centre
 * (r(1 + r) / 2D, n r(1 − r) / D) et pour rayon r(1 − r) / 2D, avec D = n²(1 − r)² + r.
 */
private fun Pencil.pappus(v: Int) {
    val r = ratio(v)
    val s = 236f
    val o = pt(center.x - s / 2, center.y)
    fun at(x: Float, y: Float) = pt(o.x + x * s, o.y + y * s)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fill(circle(o.x + s / 2, o.y, s / 2, 140), Crayons.jaune, wash = 0.16f, strokeAlpha = 0.3f, gap = 5.5f, lw = 1.4f)
    fill(circle(o.x + r * s / 2, o.y, r * s / 2, 110), Crayons.bleu, wash = 0.22f, gap = 4.6f, angle = 30f, lw = 1.2f)
    for (n in -9..9) {
        val d = n * n * (1 - r) * (1 - r) + r
        val c = at(r * (1 + r) / (2 * d), n * r * (1 - r) / d)
        val rad = r * (1 - r) / (2 * d) * s
        if (rad < 1.2f) continue
        fill(circle(c.x, c.y, rad), pal[abs(n) % 8], hatch = rad > 5f, gap = (rad / 4).coerceIn(2.4f, 4.2f), lw = if (rad > 8) 1.1f else 0.7f, wash = if (rad > 5) 0.32f else 0.6f)
    }
}

/**
 * Chaîne de Steiner : dans un anneau concentrique, n cercles tangents se suivent ; une homographie du
 * disque (z ↦ (z − a) / (1 − az)) décentre le cercle intérieur sans défaire les contacts.
 */
private fun Pencil.steiner(v: Int) {
    val n = intArrayOf(7, 9, 6, 8)[v % 4]
    val a = floatArrayOf(0.34f, 0.28f, 0.4f, 0.3f)[v % 4]
    val sn = sin(PI.toFloat() / n)
    val rho = (1 - sn) / (1 + sn)
    val big = 118f
    fun map(z: Offset): Offset {
        // (z − a) / (1 − a z), a réel.
        val nx = z.x - a; val ny = z.y
        val dx = 1 - a * z.x; val dy = -a * z.y
        val den = dx * dx + dy * dy
        val w = Offset((nx * dx + ny * dy) / den, (ny * dx - nx * dy) / den)
        return pt(center.x + w.x * big, center.y + w.y * big)
    }
    fun ring(cx: Float, cy: Float, r: Float, m: Int = 120) = List(m) { val t = TAU * it / m; map(pt(cx + r * cos(t), cy + r * sin(t))) }
    val pal = Crayons.rainbow.rot((v * 5) % 8)
    fill(ring(0f, 0f, 1f, 160), Crayons.jaune, wash = 0.15f, strokeAlpha = 0.3f, gap = 5.5f, lw = 1.4f)
    val phase = rad(17f + 23f * v)
    for (k in 0 until n) {
        val t = phase + TAU * k / n
        val c = (1 + rho) / 2
        fill(ring(c * cos(t), c * sin(t), (1 - rho) / 2, 90), pal[k % 8], angle = -58f + 360f * k / n, gap = 3.8f, lw = 1.1f)
    }
    fill(ring(0f, 0f, rho, 120), Crayons.bleu, wash = 0.2f, gap = 4.8f, angle = 25f, lw = 1.2f)
}

/**
 * Cercles de Ford : au-dessus de chaque fraction irréductible p/q de [0, 1], un cercle de rayon 1/(2q²)
 * tangent à l'axe ; dessinés de part et d'autre de l'axe, comme dans un miroir.
 */
private fun Pencil.ford(v: Int) {
    val maxQ = if (v % 2 == 0) 11 else 14
    val s = 344f
    val x0 = center.x - s / 2
    val base = center.y
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val left = (x0 - 4) * k + ox
    val right = (x0 + s + 4) * k + ox
    with(scope) {
        clipRect(left, (base - 116f) * k + oy, right, (base + 116f) * k + oy) {
            for (side in listOf(-1f, 1f)) for (q in 1..maxQ) for (p in 0..q) {
                if (gcd(p, q) != 1) continue
                val r = s / (2f * q * q)
                if (r < 0.8f) continue
                val c = pt(x0 + s * p / q, base + side * r)
                if (q == 1) {
                    // Les deux grands cercles, de rayon 1/2 : à peine coloriés, pour laisser voir les autres.
                    fill(circle(c.x, c.y, r, 220), pal[7], wash = 0.07f, gap = 7f, strokeAlpha = 0.22f, lw = 1.1f)
                    continue
                }
                fill(
                    circle(c.x, c.y, r, (r * 1.2f).toInt().coerceIn(18, 200)),
                    pal[(q - 2 + if (side > 0) 0 else 3) % 8],
                    hatch = r > 4f,
                    gap = (r / 5).coerceIn(2.2f, 4.6f),
                    angle = side * (58f - q * 17f),
                    lw = if (r > 8) 1.1f else 0.6f,
                    wash = if (r > 4) 0.32f else 0.6f,
                )
            }
        }
    }
    line(listOf(pt(x0 - 4, base), pt(x0 + s + 4, base)), Graphite, 1.2f)
}

/** Fleur de vie : dix-neuf cercles de rayon r centrés sur un réseau de triangles de côté r, dans un cercle de rayon 3r. */
private fun Pencil.flowerOfLife(v: Int) {
    val r = 38f
    val pts = mutableListOf<Offset>()
    for (i in -2..2) for (j in -2..2) {
        val p = pt(center.x + r * (i + j / 2f), center.y + r * j * sqrt(3f) / 2)
        if ((p - center).getDistance() <= 2 * r + 0.5f) pts += p
    }
    val pal = listOf(Crayons.rose, Crayons.violet, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.orange).rot(v % 6)
    fill(circle(center.x, center.y, 3 * r, 160), Crayons.jaune, wash = 0.14f, strokeAlpha = 0.3f, gap = 5.5f, lw = 1.4f)
    // Un pétale entre chaque paire de centres voisins ; sa couleur dit sa direction (trois directions).
    for (a in pts.indices) for (b in pts.indices) {
        if (a == b) continue
        val d = pts[b] - pts[a]
        if (abs(d.getDistance() - r) > 0.5f) continue
        val ang = kotlin.math.atan2(d.y, d.x)
        val deg = ((ang * 180 / PI.toFloat()) + 360) % 360
        if (deg >= 180f - 0.5f) continue
        val dirIndex = ((deg + 0.5f) / 60f).toInt() % 3
        val ring = if ((pts[a] - center).getDistance() < 1f || (pts[b] - center).getDistance() < 1f) 0 else 3
        fill(lens(pts[a], r, ang), pal[dirIndex + ring], gap = 3.4f, angle = deg + 90f, lw = 0.9f, wash = 0.3f)
    }
    for (p in pts) outline(circle(p.x, p.y, r, 70), lw = 0.6f)
}

/** Empilement hexagonal de disques, en anneaux autour du centre. */
private fun Pencil.circlePacking(v: Int) {
    val r = 11.6f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val rings = 5
    for (q in -rings..rings) for (s in -rings..rings) {
        val t = -q - s
        val ring = maxOf(abs(q), abs(s), abs(t))
        if (ring > rings) continue
        val x = center.x + 2 * r * (q + s / 2f)
        val y = center.y + 2 * r * s * sqrt(3f) / 2
        val color = if (v % 2 == 0) pal[ring % 8] else pal[((q - s) % 3 + 3) % 3 * 2]
        fill(circle(x, y, r - 0.4f, 34), color, gap = 3.2f, angle = -58f + ring * 20f, lw = 0.9f)
    }
}

/**
 * Arbelos : entre le demi-cercle de diamètre AB et ceux de diamètres AC et CB. Les cercles jumeaux
 * d'Archimède, de rayon r₁r₂ / (r₁ + r₂), touchent la verticale en C, le grand arc et un petit arc.
 */
private fun Pencil.arbelos(v: Int) {
    val ratio = ratio(v + 1)
    val big = 165f
    val r1 = big * ratio
    val r2 = big - r1
    val ax = center.x - big
    val base = center.y + 76f
    val cx = ax + 2 * r1
    val m = 70
    fun arc(ox: Float, rr: Float, from: Float, to: Float) = (0..m).map { val t = from + (to - from) * it / m; pt(ox + rr * cos(t), base - rr * sin(t)) }
    val pal = listOf(Crayons.orange, Crayons.bleu, Crayons.vert, Crayons.rose, Crayons.violet, Crayons.turquoise).rot(v % 6)
    // L'arbelos : le grand arc de A à B, puis les deux petits arcs de B à A.
    val region = arc(ax + big, big, PI.toFloat(), 0f) + arc(cx + r2, r2, 0f, PI.toFloat()) + arc(ax + r1, r1, 0f, PI.toFloat())
    fill(region, pal[0], gap = 4.4f, lw = 1.3f)
    fill(arc(ax + r1, r1, 0f, PI.toFloat()), pal[1], wash = 0.2f, gap = 5f, angle = 35f, lw = 1.1f)
    fill(arc(cx + r2, r2, 0f, PI.toFloat()), pal[2], wash = 0.2f, gap = 5f, angle = -35f, lw = 1.1f)
    // La verticale en C, jusqu'au grand arc.
    val top = base - sqrt(big * big - (cx - ax - big) * (cx - ax - big))
    line(listOf(pt(cx, base), pt(cx, top)), Graphite, 1.1f)
    // Les cercles jumeaux.
    val rho = r1 * r2 / (r1 + r2)
    fill(circle(cx - rho, base - 2 * sqrt(r1 * rho), rho, 60), pal[3], gap = 3.2f, lw = 1.1f)
    fill(circle(cx + rho, base - 2 * sqrt(r2 * rho), rho, 60), pal[4], gap = 3.2f, angle = 30f, lw = 1.1f)
    line(listOf(pt(ax - 6, base), pt(ax + 2 * big + 6, base)), Graphite, 1.3f)
}

/** Spirale de Doyle : (p, q) selon la variante. */
private fun doyleParams(v: Int): Pair<Int, Int> = listOf(8 to 13, 5 to 8, 7 to 10, 9 to 14)[v % 4]

/** Nombre complexe minimal pour la spirale de Doyle. */
private class DoyleZ(val re: Double, val im: Double) {
    operator fun times(o: DoyleZ) = DoyleZ(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun minus(o: DoyleZ) = DoyleZ(re - o.re, im - o.im)
    val abs: Double get() = Math.hypot(re, im)
    fun inverse(): DoyleZ {
        val m2 = re * re + im * im
        return DoyleZ(re / m2, -im / m2)
    }
}

/** Les générateurs a et b de la spirale, et ρ, rapport du rayon d'un cercle à la distance de son centre au centre. */
private class DoyleSpiral(val a: DoyleZ, val b: DoyleZ, val rho: Double)

private val doyleCache = HashMap<Pair<Int, Int>, DoyleSpiral?>()

/**
 * Résout la spirale de Doyle (p, q) : a = z·e^{it} et b = w·e^{is}, avec w = z^{p/q} et s = (pt + 2π)/q,
 * de sorte que a^p = b^q. Le cercle centré en 1 doit toucher ceux centrés en a et en b, et ces deux-là
 * se toucher : |a − 1| = ρ(1 + |a|), |b − 1| = ρ(1 + |b|), |a − b| = ρ(|a| + |b|). Méthode de Newton
 * depuis plusieurs départs ; on garde la solution de plus grand ρ.
 */
private fun doyleSolve(p: Int, q: Int): DoyleSpiral? = doyleCache.getOrPut(p to q) {
    val one = DoyleZ(1.0, 0.0)
    fun gens(z: Double, t: Double): Pair<DoyleZ, DoyleZ> {
        val w = Math.pow(z, p.toDouble() / q)
        val s = (p * t + 2 * Math.PI) / q
        return DoyleZ(z * Math.cos(t), z * Math.sin(t)) to DoyleZ(w * Math.cos(s), w * Math.sin(s))
    }
    fun g(z: Double, t: Double): DoubleArray {
        val (a, b) = gens(z, t)
        val ra = (a - one).abs / (1 + a.abs)
        val rb = (b - one).abs / (1 + b.abs)
        val rab = (a - b).abs / (a.abs + b.abs)
        return doubleArrayOf(ra - rb, rab - ra)
    }
    var best: DoyleSpiral? = null
    for (z0 in doubleArrayOf(1.1, 1.3, 1.6, 2.0, 3.0)) for (t0 in doubleArrayOf(0.0, 0.2, -0.2, 0.5, -0.5, 1.0, -1.0)) {
        var z = z0
        var t = t0
        var ok = true
        for (i in 0 until 100) {
            val f = g(z, t)
            val h = 1e-7
            val fz = g(z + h, t)
            val ft = g(z, t + h)
            val a11 = (fz[0] - f[0]) / h
            val a12 = (ft[0] - f[0]) / h
            val a21 = (fz[1] - f[1]) / h
            val a22 = (ft[1] - f[1]) / h
            val det = a11 * a22 - a12 * a21
            if (Math.abs(det) < 1e-14) { ok = false; break }
            val dz = (f[0] * a22 - f[1] * a12) / det
            val dt = (a11 * f[1] - a21 * f[0]) / det
            z -= dz
            t -= dt
            if (z <= 1.0001 || z > 50) { ok = false; break }
            if (Math.abs(dz) + Math.abs(dt) < 1e-12) break
        }
        if (!ok) continue
        val f = g(z, t)
        if (Math.abs(f[0]) + Math.abs(f[1]) > 1e-9) continue
        val (a, b) = gens(z, t)
        val rho = (a - one).abs / (1 + a.abs)
        if (rho > 0 && rho < 1 && (best == null || rho > best.rho)) best = DoyleSpiral(a, b, rho)
    }
    best
}

/**
 * Spirale de Doyle : les cercles centrés en a^m·b^n, de rayon ρ·|a^m·b^n|, touchent chacun six voisins ;
 * leurs rayons croissent en progression géométrique le long de chaque bras. Comme a^p = b^q, il y a q
 * bras (n de 0 à q − 1), coloriés en dégradé ; le tout découpé dans un disque.
 */
private fun Pencil.doyle(v: Int) {
    val (p, q) = doyleParams(v)
    val sp = doyleSolve(p, q) ?: return
    val big = 118f
    val scale = 22.0
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    // Le centre de la figure (dans le découpage, `center` serait celui du canevas, en pixels).
    val o = center
    val disk = circle(o.x, o.y, big, 160)
    val inward = sp.a.inverse()
    fill(disk, Crayons.jaune, wash = 0.12f, hatch = false, outline = false)
    scope.run {
        clipPath(path(disk)) {
            var bn = DoyleZ(1.0, 0.0)
            for (n in 0 until q) {
                // Le bras n : b^n, puis multiplié par a vers l'extérieur et par 1/a vers le centre.
                for (step in listOf(sp.a, inward)) {
                    var c = if (step === sp.a) bn else bn * inward
                    for (i in 0 until 160) {
                        val d = c.abs * scale
                        val r = (sp.rho * d).toFloat()
                        if (step === sp.a && d - r > big + 4) break
                        if (step !== sp.a && r < 0.6f) break
                        if (d - r <= big + 4 && r >= 0.6f) {
                            val x = o.x + (c.re * scale).toFloat()
                            val y = o.y + (c.im * scale).toFloat()
                            fill(
                                circle(x, y, r, (r * 1.2f).toInt().coerceIn(14, 120)),
                                pal[(n * 8 / q) % 8],
                                hatch = r > 5f,
                                gap = (r / 5).coerceIn(2.2f, 4.4f),
                                angle = -58f + n * 360f / q,
                                lw = if (r > 8) 1.1f else 0.6f,
                                wash = if (r > 5) 0.32f else 0.6f,
                                outline = r > 1.5f,
                            )
                        }
                        c *= step
                    }
                }
                bn *= sp.b
            }
        }
    }
    outline(disk, true, 1.5f)
}
