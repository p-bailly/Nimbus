package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Figures des thèmes « Nombres », « Graphes » et « Hasard » : spirale d'Ulam, somme des impairs, Venn,
// table modulo 7, graphe complet, planche de Galton, marches aléatoires, pluie de points.

internal val numberFigures: List<FigureSpec> = listOf(
    FigureSpec("ulam", "Spirale d’Ulam", "Nombres",
    { v ->
        val colors = if (v % 2 == 0) "colorié selon son dernier chiffre (1, 3, 7 ou 9)" else "en rouge s’il a un jumeau, un autre premier à 2 près (comme 11 et 13), en bleu sinon"
        "On écrit 1 au centre, puis 2, 3, 4… en spirale carrée, et on marque les nombres premiers, chacun $colors. " +
            "Des diagonales apparaissent : beaucoup de premiers sont des valeurs de polynômes comme 4n² − 2n + 41. Stanislaw Ulam l’a remarqué en 1963, en griffonnant pendant une conférence."
    }) { ulam(it) },
    FigureSpec("odd_squares", "Somme des impairs", "Nombres",
    { v ->
        val n = intArrayOf(8, 7, 9, 6)[v % 4]
        "Chaque équerre ajoute un nombre impair de cases : 1, puis 3, puis 5… Après n équerres, on a un carré de n × n cases, d’où 1 + 3 + 5 + … + (2n − 1) = n². " +
            "Ici, $n équerres : la somme des $n premiers impairs vaut $n × $n = ${n * n}. Une preuve sans mots, connue depuis les pythagoriciens."
    }) { oddSquares(it) },
    FigureSpec("venn", "Diagramme de Venn", "Nombres",
    { v ->
        if (v % 2 == 0) {
            "Trois ensembles, trois cercles : chaque région correspond à un choix « dedans » ou « dehors » pour chaque ensemble, soit 2³ = 8 régions, en comptant l’extérieur. " +
                "Au centre, ce qui appartient aux trois à la fois (A ∩ B ∩ C). Les hachures se croisent et les couleurs se mélangent dans les intersections."
        } else {
            "Avec quatre ensembles, des cercles ne suffisent plus à montrer les 2⁴ = 16 cas « dedans » ou « dehors » ; quatre ellipses, si. " +
                "John Venn a introduit ces diagrammes en 1880. Chaque région, extérieur compris, mêle les couleurs des ensembles qui la contiennent."
        }
    }) { venn(it) },
    FigureSpec("modular_times", "Table modulo 7", "Nombres",
    { v ->
        val op = if (v % 2 == 0) "a × b" else "a + b"
        "La case de la ligne a et de la colonne b (de 0 à 6) est coloriée selon le reste de $op dans la division par 7 ; le reste 0 est laissé blanc. " +
            if (v % 2 == 0) {
                "Comme 7 est premier, chaque ligne sauf la première contient chaque couleur une fois : tout nombre de 1 à 6 a un inverse modulo 7, par exemple 3 × 5 = 15 = 2 × 7 + 1."
            } else {
                "Chaque ligne est la précédente décalée d’un cran : les couleurs forment des diagonales, comme les heures d’une horloge qui n’en aurait que 7."
            }
    }) { modularTable(it) },
    FigureSpec("complete_graph", "Graphe complet", "Graphes",
    { v ->
        val n = if (v % 2 == 0) 9 else 11
        "Le graphe complet K$n : $n sommets, chacun relié à tous les autres. Chaque sommet a ${n - 1} voisins, et il y a $n × ${n - 1} / 2 = ${n * (n - 1) / 2} arêtes. " +
            "Les couleurs suivent l’écart entre les deux extrémités sur le cercle, de 1 à ${(n - 1) / 2} : chaque couleur dessine des polygones réguliers ou étoilés."
    }) { completeGraph(it) },
    FigureSpec("galton", "Planche de Galton", "Hasard",
    { _ ->
        "Chaque bille rebondit sur 12 rangées de clous, à gauche ou à droite avec une chance sur deux. Elle tombe dans la case k si elle est allée k fois à droite : C(12, k) chemins sur 2¹² = 4 096. " +
            "Les tas dessinent la loi binomiale, en cloche : la case du milieu reçoit C(12, 6) / 4 096 ≈ 23 % des billes, celles du bord presque jamais."
    }) { galton(it) },
    FigureSpec("random_walk", "Marches aléatoires", "Hasard",
    { _ ->
        "Cinq promeneurs partent du point jaune et font 600 pas chacun, au hasard : nord, sud, est ou ouest, une chance sur quatre. " +
            "Après n pas, ils sont en moyenne à environ √n pas du départ, ici √600 ≈ 24. Sur un quadrillage du plan, un promeneur finit toujours par repasser par son départ (théorème de Pólya)."
    }) { randomWalk(it) },
    FigureSpec("monte_carlo", "Pluie de points", "Hasard",
    { _ ->
        "On tire 800 points au hasard dans le carré. Chacun tombe dans le disque inscrit avec une probabilité égale au rapport des aires, πr² / (2r)² = π / 4 ≈ 0,785. " +
            "On estime donc π par 4 × (points dans le disque) / (points en tout) : c’est la méthode de Monte-Carlo. Plus il y a de points, plus on s’approche de π = 3,14159."
    }) { monteCarlo(it) },
    FigureSpec("maze", "Labyrinthe", "Graphes",
    { _ ->
        "Un labyrinthe parfait de 25 × 17 = 425 cases : entre deux cases, il existe un chemin et un seul. C’est un arbre couvrant de la grille, avec exactement 424 passages ouverts, un de moins que de cases. " +
            "Il est creusé en avançant au hasard vers une case jamais visitée, et en reculant quand on est coincé. Le chemin colorié va de l’entrée à la sortie."
    }) { maze(it) },
)

private val TWO_PI = 2 * PI.toFloat()

private fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    var d = 2
    while (d * d <= n) { if (n % d == 0) return false; d++ }
    return true
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

/**
 * Labyrinthe parfait : un arbre couvrant de la grille, tiré par un parcours en profondeur (on avance
 * vers une case voisine pas encore visitée en abattant le mur, on recule quand on est bloqué). Entre
 * deux cases, un seul chemin : celui de l'entrée à la sortie est colorié.
 */
private fun Pencil.maze(v: Int) {
    val cols = 25
    val lines = 17
    val cell = 13.6f
    val x0 = (FW - cols * cell) / 2
    val y0 = (FH - lines * cell) / 2
    val random = Random(7 + v * 13)
    // open[i][d] : passage depuis la case i dans la direction d (0 droite, 1 bas, 2 gauche, 3 haut).
    val open = Array(cols * lines) { BooleanArray(4) }
    val dx = intArrayOf(1, 0, -1, 0)
    val dy = intArrayOf(0, 1, 0, -1)
    val seen = BooleanArray(cols * lines)
    val stack = ArrayDeque<Int>()
    stack.addLast(0)
    seen[0] = true
    while (stack.isNotEmpty()) {
        val i = stack.last()
        val x = i % cols
        val y = i / cols
        val next = (0 until 4).filter { d ->
            val nx = x + dx[d]
            val ny = y + dy[d]
            nx in 0 until cols && ny in 0 until lines && !seen[ny * cols + nx]
        }
        if (next.isEmpty()) {
            stack.removeLast()
            continue
        }
        val d = next[random.nextInt(next.size)]
        val n = (y + dy[d]) * cols + x + dx[d]
        open[i][d] = true
        open[n][(d + 2) % 4] = true
        seen[n] = true
        stack.addLast(n)
    }

    // Le chemin de l'entrée (en haut à gauche) à la sortie (en bas à droite), par un parcours en largeur.
    val from = IntArray(cols * lines) { -1 }
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    from[0] = 0
    while (queue.isNotEmpty()) {
        val i = queue.removeFirst()
        for (d in 0 until 4) if (open[i][d]) {
            val n = (i / cols + dy[d]) * cols + i % cols + dx[d]
            if (from[n] < 0) { from[n] = i; queue.addLast(n) }
        }
    }
    val path = mutableListOf(cols * lines - 1)
    while (path.last() != 0) path += from[path.last()]
    path.reverse()

    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun corner(x: Int, y: Int) = pt(x0 + x * cell, y0 + y * cell)
    path.forEachIndexed { idx, i ->
        val x = i % cols
        val y = i / cols
        val color = pal[(idx * pal.size / path.size).coerceAtMost(pal.size - 1)]
        fill(listOf(corner(x, y), corner(x + 1, y), corner(x + 1, y + 1), corner(x, y + 1)), color, hatch = false, wash = 0.5f, outline = false)
    }
    line(path.map { pt(x0 + (it % cols + 0.5f) * cell, y0 + (it / cols + 0.5f) * cell) }, Crayons.rouge, 1.4f, 0.7f)

    // Les murs : à droite et en bas de chaque case fermée, puis le bord, ouvert à l'entrée et à la sortie.
    for (i in 0 until cols * lines) {
        val x = i % cols
        val y = i / cols
        if (x < cols - 1 && !open[i][0]) stroke(corner(x + 1, y), corner(x + 1, y + 1), Graphite, 1.5f, 0.9f)
        if (y < lines - 1 && !open[i][1]) stroke(corner(x, y + 1), corner(x + 1, y + 1), Graphite, 1.5f, 0.9f)
    }
    outline(listOf(corner(0, 1), corner(0, lines), corner(cols, lines)), close = false, lw = 1.8f)
    outline(listOf(corner(cols, lines - 1), corner(cols, 0), corner(0, 0)), close = false, lw = 1.8f)
}

