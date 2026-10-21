package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// Les premières figures : rosace, spirales, Sierpiński, cardioïde, Koch, dragon, Apollonius, Hilbert…

/** Paramètres des variantes, communs au dessin et à l'explication. */
internal object Classic {
    /** Cardioïde : on relie n à m·n, m de 2 à 7. */
    fun cardioidTable(variant: Int): Int = 2 + variant % 6

    /** Spirographe : dents de l'anneau et de la roue. */
    fun spirograph(variant: Int): Pair<Int, Int> = listOf(5 to 3, 7 to 4, 8 to 3, 9 to 5, 7 to 3, 11 to 4)[variant % 6]

    /** Fleur polaire : nombre de pétales. */
    fun petals(variant: Int): Int = intArrayOf(5, 6, 8, 7)[variant % 4]

    /** Triangle de Sierpiński : nombre d'étapes. */
    fun sierpinskiDepth(variant: Int): Int = if (variant % 2 == 0) 4 else 5

    /** Arbre de Pythagore : angle des branches, en degrés. */
    fun treeAngle(variant: Int): Float = floatArrayOf(45f, 40f, 50f, 36.87f)[variant % 4]

    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

internal val classicFigures: List<FigureSpec> = listOf(
    FigureSpec("rosace", "Rosace au compas", "Classiques", { _ ->
        "Avec un compas d’ouverture r, on trace un cercle, puis six cercles de même rayon centrés sur le premier et passant tous par son centre. " +
            "Chaque pétale est l’intersection de deux disques de rayon r dont les centres sont à distance r ; deux pétales voisins font un angle de 60°."
    }) { rosace(it) },
    FigureSpec("golden_spiral", "Spirale d’or", "Classiques", { _ ->
        "Les côtés des carrés suivent la suite de Fibonacci, 1, 1, 2, 3, 5, 8, 13, 21 : chaque terme est la somme des deux précédents. " +
            "Le rapport de deux termes consécutifs tend vers le nombre d’or φ = (1 + √5) / 2 ≈ 1,618. Dans chaque carré, un quart de cercle relie deux coins opposés : " +
            "la spirale obtenue approche une spirale logarithmique."
    }) { goldenSpiral(it) },
    FigureSpec("theodorus", "Spirale de Théodore", "Classiques", { _ ->
        "Le premier triangle rectangle a deux côtés de 1, donc une hypoténuse de √2, car 1² + 1² = 2 (Pythagore). Chaque triangle suivant s’appuie sur l’hypoténuse " +
            "du précédent et ajoute un côté de 1 : l’hypoténuse passe de √n à √(n + 1), jusqu’à √17. Théodore de Cyrène s’en serait servi pour montrer que √3, √5… √17 sont irrationnels."
    }) { theodorus(it) },
    FigureSpec("sierpinski", "Triangle de Sierpiński", "Classiques", { v ->
        val n = Classic.sierpinskiDepth(v)
        "On relie les milieux des côtés d’un triangle équilatéral et on ôte le triangle central, puis on recommence dans chacun des trois triangles restants. " +
            "Après n étapes il reste 3ⁿ triangles (ici 3^$n = ${if (n == 4) 81 else 243}) et l’aire restante vaut (3/4)ⁿ de l’aire de départ, qui tend vers 0. " +
            "Sa dimension fractale est log 3 / log 2 ≈ 1,585."
    }) { sierpinski(it) },
    FigureSpec("polar_rose", "Fleur polaire", "Classiques", { v ->
        "En coordonnées polaires, un point est repéré par sa distance r au centre et son angle θ. La courbe r = cos(kθ) dessine une rose : k pétales si k est impair, " +
            "2k s’il est pair. Ici, ${Classic.petals(v)} grands pétales, et ${Classic.petals(v)} petits tournés d’un demi-pétale."
    }) { polarRose(it) },
    FigureSpec("cardioid", "Cardioïde", "Classiques", { v ->
        val m = Classic.cardioidTable(v)
        val shape = when (m) {
            2 -> "une cardioïde, en forme de cœur"
            3 -> "une néphroïde, à deux pointes"
            else -> "une épicycloïde à ${m - 1} pointes"
        }
        "On place 144 points régulièrement sur un cercle et on relie chaque point n au point $m × n (en comptant modulo 144). L’enveloppe des cordes est $shape : " +
            "la courbe décrite par un point d’un cercle qui roule autour d’un autre."
    }) { cardioid(it) },
    FigureSpec("whirl", "Carrés tournants", "Classiques", { _ ->
        "Sur chaque côté d’un carré, on marque le point situé au dixième de sa longueur : ces quatre points forment un nouveau carré, tourné et plus petit d’un facteur " +
            "√(0,9² + 0,1²) ≈ 0,906. En recommençant, les côtés dessinent quatre spirales logarithmiques : les chemins de quatre fourmis parties des coins, chacune marchant vers sa voisine."
    }) { whirl(it) },
    FigureSpec("spirograph", "Spirographe", "Classiques", { v ->
        val (ring, wheel) = Classic.spirograph(v)
        "Une roue de $wheel dents roule à l’intérieur d’un anneau de $ring dents ; un trou de la roue décrit une hypotrochoïde, " +
            "x = (R − r) cos t + d cos((R − r) t / r), y = (R − r) sin t − d sin((R − r) t / r), avec R = $ring, r = $wheel et d la distance du trou au centre de la roue. " +
            "La courbe se referme après ${wheel / Classic.gcd(ring, wheel)} tours autour de l’anneau."
    }) { spirograph(it) },
    FigureSpec("koch", "Flocon de Koch", "Classiques", { _ ->
        "À chaque étape, chaque segment est coupé en trois et le tiers du milieu remplacé par deux côtés d’un triangle équilatéral : il y a 4 fois plus de segments, " +
            "3 fois plus courts. Le périmètre est donc multiplié par 4/3 à chaque étape et devient infini, alors que l’aire tend vers 8/5 de celle du triangle de départ."
    }) { koch(it) },
    FigureSpec("dragon", "Courbe du dragon", "Classiques", { _ ->
        "Plier une bande de papier en deux dix fois de suite, puis la déplier en ouvrant chaque pli à angle droit, donne 2¹⁰ = 1 024 segments. La suite des plis se construit " +
            "en recopiant la précédente, en ajoutant un pli, puis en recopiant la précédente à l’envers et retournée. La courbe ne se croise jamais."
    }) { dragon(it) },
    FigureSpec("apollonius", "Baderne d’Apollonius", "Classiques", { _ ->
        "Dans chaque creux laissé par trois cercles deux à deux tangents, on loge le cercle tangent aux trois. Les courbures k (inverses des rayons) de quatre cercles tangents " +
            "vérifient le théorème de Descartes : (k₁ + k₂ + k₃ + k₄)² = 2(k₁² + k₂² + k₃² + k₄²), ce qui donne chaque nouveau cercle à partir des trois qui l’entourent."
    }) { apollonius(it) },
    FigureSpec("carpet", "Tapis de Sierpiński", "Classiques", { _ ->
        "On coupe le carré en 9 carrés égaux et on ôte celui du centre, puis on recommence dans chacun des 8 restants. Après 3 étapes, il reste 8³ = 512 carrés de côté 1/27, " +
            "et l’aire restante vaut (8/9)³ ≈ 0,70 de l’aire de départ. Sa dimension fractale est log 8 / log 3 ≈ 1,893."
    }) { carpet(it) },
    FigureSpec("hilbert", "Courbe de Hilbert", "Classiques", { _ ->
        "La courbe d’ordre 1 parcourt les 4 cases d’un carré 2 × 2 en forme de U. Pour l’ordre suivant, on place une copie dans chaque quart, celles du bas tournées d’un quart de tour, " +
            "et on les relie. À l’ordre 5, elle passe par les 32 × 32 = 1 024 cases sans se croiser ; à la limite, elle remplit tout le carré."
    }) { hilbert(it) },
    FigureSpec("pythagoras", "Arbre de Pythagore", "Classiques", { v ->
        val a = Classic.treeAngle(v)
        val text = if (a == 36.87f) "36,87° et 53,13° (le triangle 3, 4, 5)" else "${a.toInt()}° et ${90 - a.toInt()}°"
        "Sur chaque carré pousse un triangle rectangle dont l’hypoténuse est le côté du carré, avec des angles de $text, puis un carré sur chacun de ses deux autres côtés. " +
            "Par le théorème de Pythagore, a² + b² = c² : les deux nouveaux carrés ont ensemble l’aire de celui qui les porte, et chaque génération a la même aire totale."
    }) { pythagoras(it) },
)
/** Pétale entre O et O + r·u(a) : deux arcs de rayon r centrés à 60° de part et d'autre (le compas). */
internal fun Pencil.lens(o: Offset, r: Float, a: Float, n: Int = 16): List<Offset> {
    val q1 = o + dir(a + rad(60f)) * r
    val q2 = o + dir(a - rad(60f)) * r
    return (0..n).map { q1 + dir(a + rad(240f + 60f * it / n)) * r } + (1 until n).map { q2 + dir(a + rad(60f + 60f * it / n)) * r }
}

internal fun Pencil.rosace(v: Int) {
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

internal fun Pencil.goldenSpiral(v: Int) {
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

internal fun Pencil.theodorus(v: Int) {
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


internal fun Pencil.sierpinski(v: Int) {
    val depth = Classic.sierpinskiDepth(v)
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

internal fun Pencil.polarRose(v: Int) {
    val n = Classic.petals(v)
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

internal fun Pencil.cardioid(v: Int) {
    val m = Classic.cardioidTable(v)
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

internal fun Pencil.whirl(v: Int) {
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

internal fun Pencil.spirograph(v: Int) {
    val (ring, wheel) = Classic.spirograph(v)
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


internal fun Pencil.koch(v: Int) {
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

internal fun Pencil.dragon(v: Int) {
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

internal fun Pencil.apollonius(v: Int) {
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


internal fun Pencil.carpet(v: Int) {
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

internal fun Pencil.hilbert(v: Int) {
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

internal fun Pencil.pythagoras(v: Int) {
    val alpha = rad(Classic.treeAngle(v))
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