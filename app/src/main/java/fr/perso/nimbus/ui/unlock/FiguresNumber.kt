package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Figures des thèmes « Nombres », « Graphes » et « Hasard » : spirale d'Ulam, crible, preuves sans mots,
// corail de Syracuse, Venn, tables modulo 7, graphes, planche de Galton, marches, Buffon, Monte-Carlo,
// billard elliptique.

internal val numberFigures: List<FigureSpec> = listOf(
    FigureSpec("ulam", "Spirale d’Ulam", "Nombres", { _ -> "" }) { ulam(it) },
    FigureSpec("sieve", "Crible d’Ératosthène", "Nombres", { _ -> "" }) { sieve(it) },
    FigureSpec("odd_squares", "Somme des impairs", "Nombres", { _ -> "" }) { oddSquares(it) },
    FigureSpec("triangular", "Nombres triangulaires", "Nombres", { _ -> "" }) { triangular(it) },
    FigureSpec("geometric_square", "Une demi, un quart…", "Nombres", { _ -> "" }) { geometricSquare(it) },
    FigureSpec("nicomachus", "Somme des cubes", "Nombres", { _ -> "" }) { nicomachus(it) },
    FigureSpec("collatz", "Corail de Syracuse", "Nombres", { _ -> "" }) { collatz(it) },
    FigureSpec("venn", "Diagramme de Venn", "Nombres", { _ -> "" }) { venn(it) },
    FigureSpec("modular_times", "Table modulo 7", "Nombres", { _ -> "" }) { modularTable(it) },
    FigureSpec("complete_graph", "Graphe complet", "Graphes", { _ -> "" }) { completeGraph(it) },
    FigureSpec("petersen", "Graphe de Petersen", "Graphes", { _ -> "" }) { petersen(it) },
    FigureSpec("galton", "Planche de Galton", "Hasard", { _ -> "" }) { galton(it) },
    FigureSpec("random_walk", "Marches aléatoires", "Hasard", { _ -> "" }) { randomWalk(it) },
    FigureSpec("buffon", "Aiguilles de Buffon", "Hasard", { _ -> "" }) { buffon(it) },
    FigureSpec("monte_carlo", "Pluie de points", "Hasard", { _ -> "" }) { monteCarlo(it) },
    FigureSpec("ellipse_billiard", "Billard elliptique", "Hasard", { _ -> "" }) { ellipseBilliard(it) },
)

private val TWO_PI = 2 * PI.toFloat()

private fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    var d = 2
    while (d * d <= n) { if (n % d == 0) return false; d++ }
    return true
}

private fun smallestFactor(n: Int): Int {
    var d = 2
    while (d * d <= n) { if (n % d == 0) return d; d++ }
    return n
}

private fun square(x: Float, y: Float, w: Float, h: Float = w) = listOf(pt(x, y), pt(x + w, y), pt(x + w, y + h), pt(x, y + h))

// ------------------------------------------------------------ Nombres

/** Spirale d'Ulam : les entiers en spirale carrée depuis 1 au centre, les premiers coloriés. */
private fun Pencil.ulam(v: Int) {
    val cell = 4.4f
    val cols = 81
    val rows = 55
    val x0 = center.x - cols * cell / 2
    val y0 = center.y - rows * cell / 2
    // Parcours de la spirale : 1 au centre, puis droite, haut, gauche, gauche, bas, bas…
    var x = 0; var y = 0; var n = 1
    var step = 1
    val dirs = listOf(1 to 0, 0 to -1, -1 to 0, 0 to 1)
    var d = 0
    val limit = (cols + 2) * (cols + 2)
    val digits = mapOf(1 to Crayons.rouge, 3 to Crayons.bleu, 7 to Crayons.vert, 9 to Crayons.violet)
    fun put(px: Int, py: Int, value: Int) {
        val cx = px + cols / 2; val cy = py + rows / 2
        if (cx !in 0 until cols || cy !in 0 until rows) return
        val sx = x0 + cx * cell; val sy = y0 + cy * cell
        when {
            value == 1 -> fill(square(sx - 1f, sy - 1f, cell + 2f), Crayons.jaune, hatch = false, wash = 0.95f, lw = 0.8f)
            isPrime(value) -> {
                val color = if (v % 2 == 0) {
                    digits[value % 10] ?: Crayons.orange
                } else {
                    if (isPrime(value - 2) || isPrime(value + 2)) Crayons.rouge else Crayons.bleu
                }
                fill(square(sx + 0.3f, sy + 0.3f, cell - 0.6f), color, hatch = false, wash = 0.85f, outline = false)
            }
            else -> dot(pt(sx + cell / 2, sy + cell / 2), 0.35f, Graphite, 0.25f)
        }
    }
    put(0, 0, 1)
    n = 2
    while (n <= limit) {
        repeat(2) {
            repeat(step) {
                x += dirs[d].first; y += dirs[d].second
                put(x, y, n)
                n++
            }
            d = (d + 1) % 4
        }
        step++
    }
}

/** Crible d'Ératosthène jusqu'à 100 (ou 120) : chaque composé barré de la couleur de son plus petit facteur premier. */
private fun Pencil.sieve(v: Int) {
    val cols = if (v % 2 == 0) 10 else 12
    val rows = 10
    val cell = 22.5f
    val x0 = center.x - cols * cell / 2
    val y0 = center.y - rows * cell / 2
    val factor = mapOf(2 to Crayons.bleu, 3 to Crayons.vert, 5 to Crayons.violet, 7 to Crayons.orange)
    for (n in 1..cols * rows) {
        val i = (n - 1) % cols; val j = (n - 1) / cols
        val x = x0 + i * cell; val y = y0 + j * cell
        val box = square(x + 1.2f, y + 1.2f, cell - 2.4f)
        when {
            n == 1 -> outline(box, lw = 0.6f)
            isPrime(n) -> fill(box, Crayons.rouge, gap = 2.8f, wash = 0.45f, lw = 0.9f)
            else -> {
                val c = factor[smallestFactor(n)] ?: Crayons.brun
                fill(box, c, hatch = false, wash = 0.16f, lw = 0.5f)
                // La croix du crible.
                stroke(pt(x + 5, y + 5), pt(x + cell - 5, y + cell - 5), c, 1.6f, 0.85f)
                stroke(pt(x + cell - 5, y + 5), pt(x + 5, y + cell - 5), c, 1.6f, 0.85f)
            }
        }
    }
}

/** 1 + 3 + 5 + … + (2n − 1) = n² : les équerres emboîtées. */
private fun Pencil.oddSquares(v: Int) {
    val n = intArrayOf(8, 7, 9, 6)[v % 4]
    val side = 228f
    val c = side / n
    val x0 = center.x - side / 2
    val y0 = center.y - side / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (k in 1..n) {
        // Équerre k : de k − 1 à k, en bas à gauche du carré k × k (origine en haut à gauche).
        val a = (k - 1) * c; val b = k * c
        val l = listOf(pt(x0 + a, y0), pt(x0 + b, y0), pt(x0 + b, y0 + b), pt(x0, y0 + b), pt(x0, y0 + a), pt(x0 + a, y0 + a))
        fill(l, pal[(k - 1) % 8], gap = 3.6f, angle = -58f + k * 22f, lw = 1.2f)
    }
    for (i in 1 until n) {
        stroke(pt(x0 + i * c, y0), pt(x0 + i * c, y0 + side), Graphite, 0.5f, 0.3f)
        stroke(pt(x0, y0 + i * c), pt(x0 + side, y0 + i * c), Graphite, 0.5f, 0.3f)
    }
}

/** 1 + 2 + … + n = n(n + 1)/2 : deux escaliers remplissent un rectangle n × (n + 1). */
private fun Pencil.triangular(v: Int) {
    val n = intArrayOf(8, 7, 9, 6)[v % 4]
    val c = min(320f / (n + 1), 226f / n)
    val w = (n + 1) * c; val h = n * c
    val x0 = center.x - w / 2; val y0 = center.y - h / 2
    val (ca, cb) = listOf(Crayons.orange to Crayons.bleu, Crayons.rose to Crayons.vert, Crayons.jaune to Crayons.violet, Crayons.rouge to Crayons.turquoise)[v % 4]
    for (row in 0 until n) {
        val y = y0 + row * c
        val left = row + 1 // l'escalier du haut à gauche : 1, 2, … n cases
        fill(square(x0, y, left * c, c), ca, gap = 3.4f, angle = -58f + row * 6f, lw = 1.1f, wash = 0.28f + 0.03f * row)
        fill(square(x0 + left * c, y, (n + 1 - left) * c, c), cb, gap = 3.4f, angle = 32f + row * 6f, lw = 1.1f, wash = 0.28f + 0.03f * (n - row))
    }
    for (i in 1..n) stroke(pt(x0 + i * c, y0), pt(x0 + i * c, y0 + h), Graphite, 0.5f, 0.3f)
}

/** 1/2 + 1/4 + 1/8 + … = 1 : le carré coupé en deux, puis la moitié restante en deux… en spirale. */
private fun Pencil.geometricSquare(v: Int) {
    val side = 232f
    var x = center.x - side / 2; var y = center.y - side / 2; var w = side; var h = side
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val clockwise = v % 2 == 0
    for (i in 0 until 11) {
        val piece: List<Offset>
        when (if (clockwise) i % 4 else (4 - i % 4) % 4) {
            0 -> { piece = square(x, y, w / 2, h); x += w / 2; w /= 2 }
            1 -> { piece = square(x, y, w, h / 2); y += h / 2; h /= 2 }
            2 -> { piece = square(x + w / 2, y, w / 2, h); w /= 2 }
            else -> { piece = square(x, y + h / 2, w, h / 2); h /= 2 }
        }
        val small = min(w, h) < 6f
        fill(piece, pal[i % 8], hatch = !small, gap = 3.6f + (8 - min(i, 8)) * 0.2f, angle = -58f + i * 37f, lw = if (small) 0.6f else 1.2f)
    }
    fill(square(x, y, w, h), pal[3], hatch = false, wash = 0.6f, lw = 0.5f)
}

/** 1³ + 2³ + … + n³ = (1 + 2 + … + n)² : chaque équerre de largeur k contient k carrés de côté k. */
private fun Pencil.nicomachus(v: Int) {
    val n = if (v % 2 == 0) 5 else 6
    val total = n * (n + 1) / 2
    val side = 232f
    val u = side / total
    val x0 = center.x - side / 2; val y0 = center.y - side / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (k in 1..n) {
        val a = (k - 1) * k / 2 * u; val b = k * (k + 1) / 2 * u
        val l = listOf(pt(x0 + a, y0), pt(x0 + b, y0), pt(x0 + b, y0 + b), pt(x0, y0 + b), pt(x0, y0 + a), pt(x0 + a, y0 + a))
        fill(l, pal[(k - 1) % 8], gap = 3.6f, angle = -58f + k * 30f, lw = 1.3f)
        // Les carrés k × k dans les deux bras de l'équerre.
        var t = k * u
        while (t < b - 0.5f) {
            stroke(pt(x0 + t, y0 + a), pt(x0 + t, y0 + b), Graphite, 0.8f, 0.6f)
            stroke(pt(x0 + a, y0 + t), pt(x0 + b, y0 + t), Graphite, 0.8f, 0.6f)
            t += k * u
        }
    }
}

/**
 * Corail de Syracuse : l'arbre des suites de Syracuse des nombres de 2 à N, chacune lue à l'envers depuis
 * 1. Chaque nombre est un pas de plus que le suivant de sa suite, tourné un peu à droite s'il est pair,
 * à gauche s'il est impair ; les suites qui se rejoignent partagent leur tronc.
 */
private fun Pencil.collatz(v: Int) {
    val count = if (v % 2 == 0) 3000 else 2200
    val even = rad(if (v % 2 == 0) 8.6f else 10f)
    val odd = rad(if (v % 2 == 0) -16f else -18f)
    // Le suivant de chaque nombre rencontré, et sa profondeur (le nombre de pas jusqu'à 1).
    val next = HashMap<Long, Long>()
    for (start in 2..count) {
        var m = start.toLong()
        while (m != 1L && m !in next) {
            val n = if (m % 2 == 0L) m / 2 else 3 * m + 1
            next[m] = n
            m = n
        }
    }
    val pos = HashMap<Long, Offset>().apply { put(1L, Offset.Zero) }
    val heading = HashMap<Long, Float>().apply { put(1L, -PI.toFloat() / 2) }
    val depth = HashMap<Long, Int>().apply { put(1L, 0) }
    fun place(m: Long) {
        if (m in pos) return
        val chain = mutableListOf<Long>()
        var c = m
        while (c !in pos) { chain += c; c = next.getValue(c) }
        for (x in chain.asReversed()) {
            val parent = next.getValue(x)
            val a = heading.getValue(parent) + if (x % 2 == 0L) even else odd
            heading[x] = a
            pos[x] = pos.getValue(parent) + dir(a)
            depth[x] = depth.getValue(parent) + 1
        }
    }
    next.keys.forEach { place(it) }
    // Le corail redressé : la direction de la racine vers son centre de gravité devient la verticale.
    val g = pos.values.fold(Offset.Zero) { acc, o -> acc + o } / pos.size.toFloat()
    val turn = -PI.toFloat() / 2 - kotlin.math.atan2(g.y, g.x)
    val ct = cos(turn); val st = sin(turn)
    for ((m, o) in pos) pos[m] = Offset(o.x * ct - o.y * st, o.x * st + o.y * ct)
    val fit = Fit(listOf(pos.values.toList()), 8f)
    val maxDepth = depth.values.max()
    val pal = listOf(Crayons.brun, Crayons.sapin, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose)
    // Du tronc vers les pointes : une arête par nombre, de son suivant jusqu'à lui.
    for ((m, n) in next.entries.sortedBy { depth.getValue(it.key) }) {
        val dd = depth.getValue(m)
        val f = dd.toFloat() / maxDepth
        stroke(fit(pos.getValue(n)), fit(pos.getValue(m)), pal[minOf(pal.size - 1, (f * pal.size).toInt())], maxOf(0.5f, 1.9f - f * 1.7f), 0.75f)
    }
}

/** Diagramme de Venn : trois cercles (ou quatre ellipses), hachurés chacun dans un sens ; les couleurs se mêlent. */
private fun Pencil.venn(v: Int) {
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.jaune, Crayons.vert)
    if (v % 2 == 0) {
        val r = 78f
        val d = 46f
        val angles = listOf(-90f, 30f, 150f)
        angles.forEachIndexed { i, a ->
            val c = center + dir(rad(a)) * d + pt(0f, 8f)
            fill(circle(c.x, c.y, r, 120), pal[i], wash = 0.22f, gap = 4.6f, angle = -60f + 60f * i, strokeAlpha = 0.6f, lw = 1.3f)
        }
    } else {
        // Quatre ellipses, deux à deux symétriques : les 2⁴ = 16 régions.
        fun ellipse(cx: Float, cy: Float, a: Float, b: Float, th: Float) = List(140) {
            val t = TWO_PI * it / 140
            pt(cx + a * cos(t) * cos(th) - b * sin(t) * sin(th), cy + a * cos(t) * sin(th) + b * sin(t) * cos(th))
        }
        val s = 112f
        val t = rad(45f)
        val shapes = listOf(
            ellipse(center.x - 0.4f * s, center.y + 0.12f * s, s, 0.55f * s, t),
            ellipse(center.x + 0.4f * s, center.y + 0.12f * s, s, 0.55f * s, -t),
            ellipse(center.x - 0.12f * s, center.y - 0.08f * s, s, 0.55f * s, t),
            ellipse(center.x + 0.12f * s, center.y - 0.08f * s, s, 0.55f * s, -t),
        )
        val fit = Fit(shapes, 8f)
        shapes.forEachIndexed { i, e -> fill(fit(e), pal[i], wash = 0.2f, gap = 4.8f, angle = -60f + 45f * i, strokeAlpha = 0.55f, lw = 1.2f) }
    }
}

/** Table modulo 7 : la case (a, b) coloriée selon le reste de a × b (ou a + b) dans la division par 7. */
private fun Pencil.modularTable(v: Int) {
    val n = 7
    val cell = 31f
    val x0 = center.x - n * cell / 2; val y0 = center.y - n * cell / 2
    val pal = listOf(Color.Transparent, Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.bleu, Crayons.violet)
    val sum = v % 2 == 1
    for (a in 0 until n) for (b in 0 until n) {
        val r = if (sum) (a + b) % n else (a * b) % n
        val box = square(x0 + b * cell + 1, y0 + a * cell + 1, cell - 2)
        if (r == 0) outline(box, lw = 0.6f) else fill(box, pal[r], gap = 3.4f, angle = -58f + r * 25f, lw = 0.9f)
    }
}

// ------------------------------------------------------------ Graphes

/** Graphe complet : n sommets sur un cercle, chaque arête coloriée selon l'écart entre ses extrémités. */
private fun Pencil.completeGraph(v: Int) {
    val n = if (v % 2 == 0) 9 else 11
    val r = 112f
    val p = List(n) { center + dir(-PI.toFloat() / 2 + TWO_PI * it / n) * r }
    val pal = listOf(Crayons.rouge, Crayons.orange, Crayons.vert, Crayons.bleu, Crayons.violet)
    for (gap in (n - 1) / 2 downTo 1) for (i in 0 until n) {
        val j = (i + gap) % n
        line(listOf(p[i], p[j]), pal[(gap - 1) % pal.size], 1.4f, 0.8f)
    }
    for (q in p) fill(circle(q.x, q.y, 6f, 20), Crayons.jaune, gap = 2.4f, lw = 1.1f)
}

/** Graphe de Petersen : sommets en trois couleurs (variante paire), ou arêtes en quatre couleurs (impaire). */
private fun Pencil.petersen(v: Int) {
    val outer = List(5) { center + dir(-PI.toFloat() / 2 + TWO_PI * it / 5) * 112f }
    val inner = List(5) { center + dir(-PI.toFloat() / 2 + TWO_PI * it / 5) * 52f }
    val pos = outer + inner
    val edges = mutableListOf<Pair<Int, Int>>()
    for (i in 0 until 5) {
        edges += i to (i + 1) % 5
        edges += i to i + 5
        edges += i + 5 to (i + 2) % 5 + 5
    }
    val vertexColors = listOf(Crayons.rouge, Crayons.bleu, Crayons.jaune)
    val edgeColors = listOf(Crayons.rouge, Crayons.bleu, Crayons.vert, Crayons.violet)
    if (v % 2 == 0) {
        val colors = solveColoring(10, edges, 3)
        for ((a, b) in edges) line(listOf(pos[a], pos[b]), Graphite, 1.6f, 0.85f)
        pos.forEachIndexed { i, q -> fill(circle(q.x, q.y, 9f, 26), vertexColors[colors[i]], gap = 2.6f, wash = 0.45f, lw = 1.2f) }
    } else {
        // Arêtes : une arête par sommet du « graphe des arêtes », voisines si elles se touchent.
        val adj = mutableListOf<Pair<Int, Int>>()
        for (i in edges.indices) for (j in i + 1 until edges.size) {
            val (a, b) = edges[i]; val (c, d) = edges[j]
            if (a == c || a == d || b == c || b == d) adj += i to j
        }
        val colors = solveColoring(edges.size, adj, 4)
        edges.forEachIndexed { i, (a, b) -> line(listOf(pos[a], pos[b]), edgeColors[colors[i]], 2.6f, 0.9f) }
        pos.forEach { q -> fill(circle(q.x, q.y, 7f, 22), Color.White, hatch = false, wash = 1f, lw = 1.2f) }
    }
}

/** Coloriage par retour arrière : une couleur parmi [k] pour chacun des [n] sommets, voisins différents. */
private fun solveColoring(n: Int, edges: List<Pair<Int, Int>>, k: Int): IntArray {
    val neighbors = List(n) { i -> edges.filter { it.first == i || it.second == i }.map { if (it.first == i) it.second else it.first } }
    val color = IntArray(n) { -1 }
    fun go(i: Int): Boolean {
        if (i == n) return true
        for (c in 0 until k) {
            if (neighbors[i].any { color[it] == c }) continue
            color[i] = c
            if (go(i + 1)) return true
        }
        color[i] = -1
        return false
    }
    go(0)
    return IntArray(n) { maxOf(0, color[it]) }
}

// ------------------------------------------------------------ Hasard

/** Planche de Galton : 12 rangées de clous, des billes qui rebondissent, et leurs tas dans 13 cases. */
private fun Pencil.galton(v: Int) {
    val rows = 12
    val bin = 22f
    val top = 16f
    val pitch = 8.4f
    val rnd = Random(400 + v)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    // Les clous.
    for (r in 0 until rows) for (i in 0..r) {
        dot(pt(center.x + (i - r / 2f) * bin, top + r * pitch), 1.6f, Graphite, 0.8f)
    }
    // Quelques billes en chemin.
    val bottom = top + rows * pitch
    repeat(5) { b ->
        var x = center.x
        val path = mutableListOf(pt(x, top - 8))
        for (r in 0 until rows) {
            path += pt(x, top + r * pitch - 3)
            x += if (rnd.nextBoolean()) bin / 2 else -bin / 2
        }
        path += pt(x, bottom + 4)
        line(path, pal[b], 1.2f, 0.8f)
    }
    // Les cases et les tas de billes.
    val balls = 210
    val counts = IntArray(rows + 1)
    repeat(balls) { var k = 0; repeat(rows) { if (rnd.nextBoolean()) k++ }; counts[k]++ }
    val floor = 240f
    val radius = 3.3f
    for (k in 0..rows) {
        val cx = center.x + (k - rows / 2f) * bin
        val color = pal[abs(k - rows / 2) % 8]
        for (j in 0 until counts[k]) {
            val col = j % 3; val row = j / 3
            val c = pt(cx + (col - 1) * radius * 2.05f, floor - radius - row * radius * 2f)
            if (c.y < bottom + 6) continue
            dot(c, radius, color, 0.55f)
            outline(circle(c.x, c.y, radius, 12), lw = 0.45f)
        }
    }
    for (k in 0..rows + 1) {
        val x = center.x + (k - rows / 2f - 0.5f) * bin
        stroke(pt(x, bottom + 8), pt(x, floor), Graphite, 0.8f, 0.6f)
    }
    line(listOf(pt(center.x - (rows / 2f + 0.5f) * bin, floor), pt(center.x + (rows / 2f + 0.5f) * bin, floor)), Graphite, 1.2f)
}

/** Cinq marches aléatoires de 600 pas sur le quadrillage, depuis le même point. */
private fun Pencil.randomWalk(v: Int) {
    val rnd = Random(1000 + v)
    val steps = listOf(pt(1f, 0f), pt(-1f, 0f), pt(0f, 1f), pt(0f, -1f))
    val walks = List(5) {
        var p = Offset.Zero
        val pts = mutableListOf(p)
        repeat(600) { p += steps[rnd.nextInt(4)]; pts += p }
        pts
    }
    val fit = Fit(walks, 12f)
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange).rot(v % 5)
    walks.forEachIndexed { i, w ->
        line(fit(w), pal[i], 1.3f, 0.85f)
        val end = fit(w.last())
        fill(circle(end.x, end.y, 4.5f, 16), pal[i], hatch = false, wash = 0.8f, lw = 0.9f)
    }
    val start = fit(Offset.Zero)
    fill(circle(start.x, start.y, 5.5f, 18), Crayons.jaune, hatch = false, wash = 0.9f, lw = 1.2f)
}

/** Aiguilles de Buffon : longueur égale à l'écart des rainures ; rouges si elles en coupent une. */
private fun Pencil.buffon(v: Int) {
    val rnd = Random(2000 + v)
    val d = 40f
    val x0 = 14f; val x1 = FW - 14f
    val lines = (0..5).map { 25f + it * d }
    // Le parquet : des lattes alternées.
    for (i in 0 until lines.size - 1) {
        fill(square(x0, lines[i], x1 - x0, d), if (i % 2 == 0) Crayons.brun else Crayons.orange, wash = 0.1f, gap = 7f, angle = 0f, strokeAlpha = 0.25f, outline = false)
    }
    for (y in lines) line(listOf(pt(x0, y), pt(x1, y)), Graphite, 1f, 0.8f)
    repeat(70) {
        val a = rnd.nextFloat() * PI.toFloat()
        val half = dir(a) * (d / 2)
        val c = pt(x0 + d / 2 + rnd.nextFloat() * (x1 - x0 - d), lines.first() + d / 2 + rnd.nextFloat() * (lines.last() - lines.first() - d))
        val p = c - half; val q = c + half
        val crosses = lines.any { y -> (p.y - y) * (q.y - y) <= 0 }
        line(listOf(p, q), if (crosses) Crayons.rouge else Crayons.bleu, 1.9f, 0.9f)
        dot(p, 1.2f, Graphite, 0.8f)
    }
}

/** Pluie de points : 800 points au hasard dans un carré ; ceux du disque inscrit en orange. */
private fun Pencil.monteCarlo(v: Int) {
    val rnd = Random(3000 + v)
    val side = 232f
    val x0 = center.x - side / 2; val y0 = center.y - side / 2
    fill(square(x0, y0, side), Crayons.bleu, wash = 0.08f, gap = 6f, strokeAlpha = 0.25f, lw = 1.2f)
    fill(circle(center.x, center.y, side / 2, 150), Crayons.jaune, wash = 0.18f, gap = 6f, angle = 30f, strokeAlpha = 0.3f, lw = 1.2f)
    repeat(800) {
        val x = rnd.nextFloat() * 2 - 1; val y = rnd.nextFloat() * 2 - 1
        val inside = x * x + y * y <= 1
        dot(pt(center.x + x * side / 2, center.y + y * side / 2), 1.7f, if (inside) Crayons.rouge else Crayons.marine, 0.8f)
    }
}

/** Billard elliptique : une bille rebondit soixante fois ; ses trajets enveloppent une ellipse ou une hyperbole de mêmes foyers. */
private fun Pencil.ellipseBilliard(v: Int) {
    val a = 170f; val b = 112f
    val f = sqrt(a * a - b * b)
    fill(List(160) { val t = TWO_PI * it / 160; pt(center.x + a * cos(t), center.y + b * sin(t)) }, Crayons.vert, wash = 0.16f, gap = 5.5f, strokeAlpha = 0.35f, lw = 1.6f)
    // Départ : un point du bord et une direction qui passe entre les foyers (variante impaire) ou non.
    var p = Offset(a * cos(2.2f), b * sin(2.2f))
    // Variante paire : le trajet coupe l'axe hors des foyers (|x| > f), la caustique est une ellipse ;
    // impaire : entre les foyers, c'est une hyperbole.
    var d = if (v % 2 == 0) (Offset((f + a) / 2 + 6f, 0f) - p) else (Offset(f * 0.3f, 0f) - p)
    d /= d.getDistance()
    val pts = mutableListOf(p)
    repeat(if (v % 2 == 0) 90 else 70) {
        val t = -2 * (p.x * d.x / (a * a) + p.y * d.y / (b * b)) / (d.x * d.x / (a * a) + d.y * d.y / (b * b))
        p += d * t
        pts += p
        val n = Offset(p.x / (a * a), p.y / (b * b)).let { it / it.getDistance() }
        d -= n * (2 * (d.x * n.x + d.y * n.y))
    }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (i in 0 until pts.size - 1) {
        stroke(center + pts[i], center + pts[i + 1], pal[i * 8 / pts.size], 1.1f, 0.75f)
    }
    for (s in listOf(-f, f)) fill(circle(center.x + s, center.y, 4f, 16), Crayons.rouge, hatch = false, wash = 0.9f, lw = 1f)
}
