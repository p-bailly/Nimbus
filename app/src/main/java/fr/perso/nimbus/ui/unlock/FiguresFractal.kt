package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Figures du thème « Fractales » : courbes de Lévy, de Gosper, de Minkowski, pointe de flèche,
// Cantor, arbre, fougère, Vicsek, H, pentaflocon, Pascal modulo p et Mandelbrot.

internal val fractalFigures: List<FigureSpec> = listOf(
    FigureSpec("levy", "Courbe de Lévy", "Fractales", { _ -> "" }) { levy(it) },
    FigureSpec("cantor", "Ensemble de Cantor", "Fractales", { _ -> "" }) { cantor(it) },
    FigureSpec("fractal_tree", "Arbre fractal", "Fractales", { _ -> "" }) { fractalTree(it) },
    FigureSpec("fern", "Fougère de Barnsley", "Fractales", { _ -> "" }) { fern(it) },
    FigureSpec("gosper", "Courbe de Gosper", "Fractales", { _ -> "" }) { gosper(it) },
    FigureSpec("arrowhead", "Pointe de flèche de Sierpiński", "Fractales", { _ -> "" }) { arrowhead(it) },
    FigureSpec("vicsek", "Flocon de Vicsek", "Fractales", { _ -> "" }) { vicsek(it) },
    FigureSpec("h_tree", "Fractale en H", "Fractales", { _ -> "" }) { hTree(it) },
    FigureSpec("pentaflake", "Pentaflocon", "Fractales", { _ -> "" }) { pentaflake(it) },
    FigureSpec("minkowski", "Saucisse de Minkowski", "Fractales", { _ -> "" }) { minkowski(it) },
    FigureSpec("pascal_mod", "Triangle de Pascal modulo", "Fractales", { _ -> "" }) { pascalMod(it) },
    FigureSpec("mandelbrot", "Ensemble de Mandelbrot", "Fractales", { _ -> "" }) { mandelbrot(it) },
)

/** Paramètres des variantes, communs au dessin et à l'explication. */
private object FractalParams {
    fun treeAngle(v: Int): Int = intArrayOf(25, 20, 30, 35)[v % 4]
    fun arrowheadOrder(v: Int): Int = if (v % 2 == 0) 6 else 7
    fun pascalPrime(v: Int): Int = intArrayOf(2, 3, 5, 7)[v % 4]
}


/** Une ligne coloriée en [chunks] tronçons successifs de la palette. */
private fun Pencil.rainbowLine(pts: List<Offset>, pal: List<Color>, lw: Float, chunks: Int = pal.size) {
    val size = pts.size / chunks
    for (i in 0 until chunks) {
        val end = if (i == chunks - 1) pts.size else min(pts.size, (i + 1) * size + 1)
        line(pts.subList(i * size, end), pal[i % pal.size], lw)
    }
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

private fun Pencil.levy(v: Int) {
    var pts = listOf(pt(0f, 0f), pt(1f, 0f))
    repeat(12) {
        val next = ArrayList<Offset>(pts.size * 2)
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val d = b - a
            // Sommet de l'angle droit : d tourné de 45° et réduit de √2.
            val c = a + Offset((d.x - d.y) / 2, (d.x + d.y) / 2)
            next += a
            next += c
        }
        next += pts.last()
        pts = next
    }
    pts = pts.map { pt(it.x, -it.y) }
    val fit = Fit(listOf(pts), 8f)
    rainbowLine(fit(pts), Crayons.rainbow.rot(v % 8), 1.3f)
}

private fun Pencil.cantor(v: Int) {
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val levels = 6
    val bar = 22f
    val gap = (FH - 12 - levels * bar) / (levels - 1)
    var segments = listOf(0f to 1f)
    val x0 = 16f
    val width = FW - 32f
    for (l in 0 until levels) {
        val y = 6f + l * (bar + gap)
        for ((a, b) in segments) {
            val w = (b - a) * width
            val rect = listOf(pt(x0 + a * width, y), pt(x0 + b * width, y), pt(x0 + b * width, y + bar), pt(x0 + a * width, y + bar))
            fill(rect, pal[l], hatch = w > 6, gap = 3.6f, angle = -58f + 14f * l, wash = if (w > 6) 0.32f else 0.7f, lw = if (w > 6) 1.2f else 0.6f)
        }
        segments = segments.flatMap { (a, b) -> val t = (b - a) / 3; listOf(a to a + t, b - t to b) }
    }
}

private fun Pencil.fractalTree(v: Int) {
    val angle = rad(FractalParams.treeAngle(v).toFloat())
    val branches = mutableListOf<Triple<Offset, Offset, Int>>()
    fun grow(p: Offset, a: Float, len: Float, d: Int) {
        val q = p + dir(a) * len
        branches += Triple(p, q, d)
        if (d == 10) return
        grow(q, a - angle, len * 0.7f, d + 1)
        grow(q, a + angle, len * 0.7f, d + 1)
    }
    grow(Offset.Zero, -PI.toFloat() / 2, 1f, 0)
    val fit = Fit(branches.map { listOf(it.first, it.second) }, 8f)
    val leaves = if (v % 2 == 0) listOf(Crayons.vert, Crayons.jaune, Crayons.rose) else listOf(Crayons.orange, Crayons.rouge, Crayons.jaune)
    for ((p, q, d) in branches) {
        val color = when {
            d < 4 -> Crayons.brun
            d < 7 -> if (v % 2 == 0) Crayons.sapin else Crayons.brun
            else -> leaves[0]
        }
        val lw = max(0.6f, 7f * Math.pow(0.68, d.toDouble()).toFloat())
        if (d < 4) line(listOf(fit(p), fit(q)), color, lw) else stroke(fit(p), fit(q), color, lw, 0.9f)
    }
    for ((_, q, d) in branches) if (d == 10) dot(fit(q), 1.6f, leaves[1 + (q.x * 1000).toInt().mod(2)], 0.85f)
}

private fun Pencil.fern(v: Int) {
    val rnd = kotlin.random.Random(7)
    var x = 0f
    var y = 0f
    val pts = ArrayList<Offset>(14000)
    repeat(14000) {
        val r = rnd.nextFloat()
        val nx: Float
        val ny: Float
        when {
            r < 0.01f -> { nx = 0f; ny = 0.16f * y }
            r < 0.86f -> { nx = 0.85f * x + 0.04f * y; ny = -0.04f * x + 0.85f * y + 1.6f }
            r < 0.93f -> { nx = 0.2f * x - 0.26f * y; ny = 0.23f * x + 0.22f * y + 1.6f }
            else -> { nx = -0.15f * x + 0.28f * y; ny = 0.26f * x + 0.24f * y + 0.44f }
        }
        x = nx
        y = ny
        if (it > 20) pts += pt(x, -y)
    }
    // La fougère penche : on la couche un peu pour qu'elle remplisse mieux la case.
    val tilt = rad(if (v % 2 == 0) 28f else -28f)
    val turned = pts.map { pt(it.x * cos(tilt) - it.y * sin(tilt), it.x * sin(tilt) + it.y * cos(tilt)) }
    val fit = Fit(listOf(turned), 6f)
    val autumn = v % 4 >= 2
    val low = if (autumn) Crayons.brun else Crayons.sapin
    val mid = if (autumn) Crayons.orange else Crayons.vert
    val high = if (autumn) Crayons.jaune else Color(0xFF9CCB5A)
    turned.forEachIndexed { i, p ->
        val h = (-pts[i].y / 10f).coerceIn(0f, 1f)
        val c = if (h < 0.35f) low else if (h < 0.7f) mid else high
        dot(fit(p), 0.55f, c, 0.75f)
    }
}

private fun Pencil.gosper(v: Int) {
    val cmds = lsystem("A", mapOf('A' to "A-B--B+A++AA+B-", 'B' to "+A-BB--B-A++A+B"), 4)
    val pts = turtle(cmds, 60f, setOf('A', 'B'))
    val fit = Fit(listOf(pts), 8f)
    rainbowLine(fit(pts), Crayons.rainbow.rot(v % 8), 1.4f, 7)
}

private fun Pencil.arrowhead(v: Int) {
    val n = FractalParams.arrowheadOrder(v)
    val cmds = lsystem("A", mapOf('A' to "B-A-B", 'B' to "A+B+A"), n)
    // Ordre pair : la courbe va de gauche à droite et pointe vers le haut.
    val pts = turtle(cmds, 60f, setOf('A', 'B')).map { pt(it.x, if (n % 2 == 0) -it.y else it.y) }
    val fit = Fit(listOf(pts), 8f)
    val drawn = fit(pts)
    // Le triangle qu'elle remplit, en lavis pâle dessous.
    var x0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
    for (p in drawn) { x0 = min(x0, p.x); x1 = max(x1, p.x); y0 = min(y0, p.y); y1 = max(y1, p.y) }
    fill(listOf(pt((x0 + x1) / 2, y0), pt(x1, y1), pt(x0, y1)), Crayons.jaune, wash = 0.14f, hatch = false, outline = false)
    rainbowLine(drawn, Crayons.rainbow.rot((v * 5) % 8), if (n == 6) 1.5f else 1.1f)
}

private fun Pencil.vicsek(v: Int) {
    val cross = v % 2 == 0
    val cells = mutableListOf<Pair<Offset, Float>>()
    fun go(o: Offset, s: Float, d: Int) {
        if (d == 0) { cells += o to s; return }
        val t = s / 3
        for (i in 0..2) for (j in 0..2) {
            val keep = if (cross) (i == 1 || j == 1) else ((i + j) % 2 == 0)
            if (keep) go(o + Offset(i * t, j * t), t, d - 1)
        }
    }
    val side = 236f
    val o = center - Offset(side / 2, side / 2)
    go(o, side, 3)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for ((c, s) in cells) {
        val m = c + Offset(s / 2, s / 2)
        val dist = hypot(m.x - center.x, m.y - center.y) / (side * 0.7f)
        fill(listOf(c, c + Offset(s, 0f), c + Offset(s, s), c + Offset(0f, s)), pal[min(7, (dist * 8).toInt())], gap = 2.8f, lw = 0.8f, wash = 0.42f)
    }
}

private fun Pencil.hTree(v: Int) {
    val segs = mutableListOf<Triple<Offset, Offset, Int>>()
    fun h(c: Offset, len: Float, horizontal: Boolean, d: Int) {
        val half = if (horizontal) Offset(len / 2, 0f) else Offset(0f, len / 2)
        val a = c - half
        val b = c + half
        segs += Triple(a, b, d)
        if (d == 8) return
        h(a, len / sqrt(2f), !horizontal, d + 1)
        h(b, len / sqrt(2f), !horizontal, d + 1)
    }
    h(Offset.Zero, 1f, true, 0)
    val fit = Fit(segs.map { listOf(it.first, it.second) }, 8f)
    val pal = Crayons.rainbow.rot(v % 8)
    for ((a, b, d) in segs) {
        val lw = max(1f, 4.2f * Math.pow(0.82, d.toDouble()).toFloat())
        line(listOf(fit(a), fit(b)), pal[d % 8], lw)
    }
    for ((a, b, d) in segs) if (d == 8) { dot(fit(a), 2.2f, pal[(d + 1) % 8], 0.9f); dot(fit(b), 2.2f, pal[(d + 1) % 8], 0.9f) }
}

private fun Pencil.pentaflake(v: Int) {
    val r = (3 - sqrt(5f)) / 2
    val shapes = mutableListOf<Pair<List<Offset>, Int>>()
    fun go(c: Offset, radius: Float, rot: Float, d: Int, tag: Int) {
        if (d == 0) {
            shapes += List(5) { c + dir(rot + 2 * PI.toFloat() * it / 5) * radius } to tag
            return
        }
        val small = radius * r
        go(c, small, rot + PI.toFloat(), d - 1, 0)
        for (i in 0 until 5) go(c + dir(rot + 2 * PI.toFloat() * i / 5) * (radius - small), small, rot, d - 1, if (d == 3) i + 1 else tag)
    }
    go(center + Offset(0f, 8f), 128f, -PI.toFloat() / 2, 3, 0)
    val pal = listOf(Crayons.jaune, Crayons.rouge, Crayons.orange, Crayons.vert, Crayons.bleu, Crayons.violet).rot(v % 6)
    for ((p, tag) in shapes) fill(p, pal[tag], gap = 2.6f, lw = 0.8f, wash = 0.45f, angle = -58f + tag * 25f)
}

private fun Pencil.minkowski(v: Int) {
    // Le motif d'un segment, en quarts : un créneau vers l'extérieur puis vers l'intérieur.
    val motif = listOf(pt(0f, 0f), pt(1f, 0f), pt(1f, -1f), pt(2f, -1f), pt(2f, 0f), pt(2f, 1f), pt(3f, 1f), pt(3f, 0f))
    fun island(depth: Int, size: Float): List<Offset> {
        var pts = listOf(pt(-1f, -1f), pt(1f, -1f), pt(1f, 1f), pt(-1f, 1f))
        repeat(depth) {
            val next = ArrayList<Offset>(pts.size * 8)
            for (i in pts.indices) {
                val a = pts[i]
                val b = pts[(i + 1) % pts.size]
                val u = (b - a) / 4f
                val n = Offset(u.y, -u.x)
                for (m in motif) next += a + u * m.x + n * m.y
            }
            pts = next
        }
        return pts.map { it * size }
    }
    val pal = listOf(Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune).rot(v % 4)
    // Les îles sont cadrées sur la plus grande : elle remplit la hauteur de la case.
    val big = island(3, 1f)
    val fit = Fit(listOf(big), 6f)
    fill(fit(big), pal[0], gap = 4.4f, lw = 1f)
    fill(fit(island(2, 0.6f)), pal[1], gap = 3.8f, angle = 30f, lw = 1f)
    fill(fit(island(1, 0.33f)), pal[2], gap = 3.2f, angle = -20f, lw = 1f)
    fill(fit(island(0, 0.13f)), pal[3], gap = 2.6f, lw = 1f)
}

private fun Pencil.pascalMod(v: Int) {
    val p = FractalParams.pascalPrime(v)
    val rows = when (p) { 2 -> 32; 3 -> 27; 5 -> 25; else -> 28 }
    val h = (FH - 12) / rows
    val w = h * 2 / sqrt(3f)
    val pal = when (p) {
        2 -> listOf(Crayons.violet)
        3 -> listOf(Crayons.bleu, Crayons.rouge)
        5 -> listOf(Crayons.rouge, Crayons.orange, Crayons.vert, Crayons.bleu)
        else -> listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.bleu, Crayons.violet)
    }.rot(v / 4 % 2)
    var row = IntArray(1) { 1 }
    for (n in 0 until rows) {
        val cy = 6 + h * (n + 0.5f)
        for (k in 0..n) {
            val cx = FW / 2 + (k - n / 2f) * w * 0.98f
            val value = row[k]
            if (value == 0) {
                dot(pt(cx, cy), 0.8f, Graphite, 0.25f)
                continue
            }
            val hex = List(6) { pt(cx, cy) + dir(rad(30f + 60f * it)) * (h * 0.6f) }
            fill(hex, pal[(value - 1) % pal.size], hatch = false, wash = 0.6f, lw = 0.45f)
        }
        val next = IntArray(n + 2)
        for (k in 0..n + 1) next[k] = ((if (k > 0) row[k - 1] else 0) + (if (k <= n) row[k] else 0)) % p
        row = next
    }
}

private fun Pencil.mandelbrot(v: Int) {
    val cell = 2.5f
    val cols = (FW / cell).toInt()
    val lines = (FH / cell).toInt()
    // Vue de −2,45 à 0,95 sur l'axe réel : l'ensemble (de −2 à 0,47, et ±1,12 en hauteur) y tient en entier.
    val x0 = -2.45f
    val x1 = 0.95f
    val span = (x1 - x0) / cols
    val y0 = -span * lines / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val max = 60
    for (row in 0 until lines) for (col in 0 until cols) {
        val cr = x0 + (col + 0.5f) * span
        val ci = y0 + (row + 0.5f) * span
        var zr = 0f
        var zi = 0f
        var n = 0
        while (n < max && zr * zr + zi * zi <= 4f) {
            val t = zr * zr - zi * zi + cr
            zi = 2 * zr * zi + ci
            zr = t
            n++
        }
        if (n < 4) continue
        val color = if (n == max) Graphite else pal[(n / 2) % pal.size]
        val alpha = if (n == max) 0.72f else (0.16f + min(0.55f, n / 28f))
        scope.drawRect(
            color,
            topLeft = px(pt((FW - cols * cell) / 2 + col * cell + j(0.3f), (FH - lines * cell) / 2 + row * cell + j(0.3f))),
            size = Size(cell * k * 1.05f, cell * k * 1.05f),
            alpha = alpha,
        )
    }
}
