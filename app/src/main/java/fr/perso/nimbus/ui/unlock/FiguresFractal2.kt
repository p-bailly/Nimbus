package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
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

// Figures des thèmes « Fractales » et « Hasard », deuxième série : éponge de Menger, tétraèdre de
// Sierpiński, flamme fractale, Lyapunov, vallée des hippocampes, chou romanesco, attracteur de Clifford,
// dragon jumeau, Rauzy, bifurcation, herbier en L-système, perles d'Indra, mot de Fibonacci, jeu du
// chaos, terdragon, escalier du diable ; agrégation par diffusion, aiguilles de Buffon, percolation et
// somme de dés. Les calculs lourds (itérations, marches au hasard) sont faits une fois par variante.

internal val moreFractalFigures: List<FigureSpec> = listOf(
    FigureSpec("menger", "Éponge de Menger", "Fractales", { _ ->
        "On coupe un cube en 27 cubes égaux et on ôte les sept du milieu : celui du centre et ceux du centre des six faces. Il en reste 20, et l’on recommence " +
            "dans chacun : après deux étapes (à droite), 20² = 400 cubes de côté 1/9. Le volume, (20/27)ⁿ, tend vers 0 et la surface devient infinie ; " +
            "chaque face est un tapis de Sierpiński. Dimension fractale : log 20 / log 3 ≈ 2,727 (Karl Menger, 1926)."
    }) { menger(it) },
    FigureSpec("sierpinski_tetra", "Tétraèdre de Sierpiński", "Fractales", { v ->
        val d = Fractal2.tetraDepth(v)
        "On remplace un tétraèdre régulier par les quatre tétraèdres deux fois plus petits posés à ses sommets, et l’on recommence : après $d étapes, " +
            "4${sup(d)} = ${1 shl (2 * d)} tétraèdres. Le volume est divisé par 2 à chaque étape, mais l’aire des faces ne change pas : quatre faces quatre fois plus petites " +
            "remplacent chaque face. Dimension fractale : log 4 / log 2 = 2, celle d’une surface, pour un objet de l’espace."
    }) { sierpinskiTetra(it) },
    FigureSpec("fractal_flame", "Flamme fractale", "Fractales", { v ->
        val p = FLAMES[v % FLAMES.size]
        "Comme pour la fougère de Barnsley, un point saute 150 000 fois au hasard d’une transformation à l’autre, mais elles ne sont plus affines : " +
            "${p.words}. Une rotation d’un ${if (p.sym == 4) "quart" else "tiers"} de tour ajoute une symétrie d’ordre ${p.sym}. " +
                        "Chaque case est foncée selon le logarithme de ses passages et teintée selon les derniers sauts (Scott Draves, 1992)."
    }) { fractalFlame(it) },
    FigureSpec("lyapunov", "Fractale de Lyapunov", "Fractales", { v ->
        val l = LYAPUNOV[v % LYAPUNOV.size]
        "On itère x ↦ r x (1 − x) en faisant alterner deux valeurs de r selon le motif ${l.seq} : a, de ${dec(l.a0, 1)} à ${dec(l.a1, 1)} en largeur, et b, de ${dec(l.b0, 1)} à ${dec(l.b1, 1)} en hauteur. " +
            "L’exposant de Lyapunov λ, moyenne de log |r (1 − 2x)|, dit si deux départs voisins s’écartent. λ < 0 : la suite se stabilise (tons chauds, plus sombres " +
            "si elle converge vite) ; λ > 0 : elle est chaotique (bleu). Mario Markus, 1990."
    }) { lyapunov(it) },
    FigureSpec("seahorse", "Vallée des hippocampes", "Fractales", { v ->
        val s = SEAHORSES[v % SEAHORSES.size]
        "Entre la grande cardioïde et le disque de gauche de l’ensemble de Mandelbrot s’ouvre la vallée des hippocampes. On la voit ici autour de c ≈ ${dec(s.cx, 4)} + ${dec(s.cy, 4)} i, " +
            "agrandie ${(3.0 / s.w).roundToInt()} fois : les queues s’enroulent en spirales, faites de spirales plus petites. La couleur suit le logarithme du nombre d’étapes " +
            "de z ↦ z² + c avant que |z| dépasse 2 (${s.iter} au plus) ; en gris, les points de l’ensemble."
    }) { seahorse(it) },
    FigureSpec("romanesco", "Chou romanesco", "Fractales", { _ ->
        "Une fractale du potager : chaque bourgeon du chou romanesco est un petit chou, fait lui-même de bourgeons plus petits (la loupe). Ils naissent l’un après " +
            "l’autre en tournant de l’angle d’or, 360° × (1 − 1/φ) ≈ 137,5°, et en s’écartant du centre d’un facteur constant : ils s’alignent alors sur des spirales " +
            "dont les nombres, dans un sens et dans l’autre, sont des termes voisins de la suite de Fibonacci, comme 8, 13 et 21."
    }) { romanesco(it) },
    FigureSpec("clifford", "Attracteur de Clifford", "Fractales", { v ->
        val (a, b, c, d) = CLIFFORD[v % CLIFFORD.size]
        "On itère x ↦ sin(a y) + c cos(a x), y ↦ sin(b x) + d cos(b y), avec a = ${dec(a, 1)}, b = ${dec(b, 1)}, c = ${dec(c, 1)} et d = ${dec(d, 1)}. " +
            "D’où qu’ils partent, les points finissent sur cette dentelle, un attracteur étrange : deux départs voisins s’écartent très vite (c’est le chaos), " +
            "mais tous dessinent la même figure. 100 000 points, teintés selon la direction de leur saut ; règle popularisée par Clifford Pickover."
    }) { clifford(it) },
    FigureSpec("twindragon", "Dragon jumeau", "Fractales", { _ ->
        "En base b = −1 + i, avec les seuls chiffres 0 et 1, chaque entier de Gauss m + n i s’écrit d’une seule façon (Walter Penney, 1965). Les nombres « après la virgule », " +
            "Σ dₖ b⁻ᵏ, forment le dragon jumeau ; ses copies décalées des entiers de Gauss pavent le plan sans trou ni chevauchement. Chaque tuile est faite de deux copies " +
            "d’elle-même, réduites de √2 et tournées de 135° (les deux nuances) ; son bord a pour dimension ≈ 1,524."
    }) { twindragon(it) },
    FigureSpec("rauzy", "Fractale de Rauzy", "Fractales", { v ->
        val pieces = if (v % 2 == 0) "les trois morceaux, coloriés, sont des copies du tout réduites de |α|, |α|² et |α|³" else
            "chaque morceau s’y redécoupe en copies plus petites : neuf couleurs pour deux étapes"
        "Les points Σ εₖ αᵏ, où chaque εₖ vaut 0 ou 1 sans trois 1 de suite et où α ≈ −0,4196 + 0,6063 i est une racine complexe de x³ = x² + x + 1. " +
            "Gérard Rauzy (1982) l’a tirée du mot de Tribonacci (1 → 12, 2 → 13, 3 → 1) ; $pieces. La figure pave le plan par translations, et son bord est fractal."
    }) { rauzy(it) },
    FigureSpec("bifurcation", "Diagramme de bifurcation", "Fractales", { v ->
        val (r0, _) = Fractal2.bifurcationRange(v)
        "Pour chaque r de ${dec(r0, 1)} à 4 (en largeur), on itère x ↦ r x (1 − x) et l’on marque les valeurs où la suite revient (en hauteur, de 0 à 1). " +
            "Un seul point jusqu’à r = 3, puis 2, 4, 8… : les doublements de période s’accumulent en r ≈ 3,5699, de plus en plus vite, à un rythme qui tend vers " +
            "la constante de Feigenbaum δ ≈ 4,669. Au-delà, le chaos, troué de fenêtres, comme le cycle de 3 en r = 1 + √8 ≈ 3,828."
    }) { bifurcation(it) },
    FigureSpec("lplant", "Herbier en L-système", "Fractales", { v ->
        val p = PLANTS[Fractal2.plantSet(v)[1]]
        "Aristid Lindenmayer (1968) : on réécrit ${p.n} fois une chaîne de lettres, ici ${p.rule}, puis une tortue la lit. F avance d’un pas, + et − tournent de " +
            "${dec(p.angle.toDouble(), 1)}°, [ retient la position et ] y revient : ce sont les branches. Chaque plante de l’herbier a sa règle, tirée de " +
            "The Algorithmic Beauty of Plants (Prusinkiewicz et Lindenmayer, 1990)."
    }) { plants(it) },
    FigureSpec("indra", "Perles d’Indra", "Fractales", { _ ->
        "L’inversion dans un cercle échange son intérieur et son extérieur, et change tout cercle en cercle. Chacun des cinq grands cercles est reflété dans les quatre autres, " +
            "chaque reflet dans les quatre cercles où il n’est pas, et ainsi de suite : 5 × 4ⁿ perles à l’étape n. Elles s’accumulent sur une poussière de Cantor, " +
            "l’ensemble limite du groupe engendré par les cinq inversions (Mumford, Series et Wright, Les Perles d’Indra, 2002)."
    }) { indra(it) },
    FigureSpec("fibonacci_word", "Fractale du mot de Fibonacci", "Fractales", { v ->
        val n = Fractal2.fibonacciLength(v)
        "Le mot de Fibonacci 0100101001001… se construit en recopiant le mot précédent puis l’avant-dernier. On en lit ici les ${grouped(n)} premières lettres : à chaque lettre " +
            "on avance d’un pas, et sur un 0 on tourne d’un quart de tour, à gauche si son rang est pair, à droite sinon (Monnerot-Dumaine, 2009). " +
            "Dimension fractale : 3 log φ / log(1 + √2) ≈ 1,638."
    }) { fibonacciWord(it) },
    FigureSpec("chaos_game", "Jeu du chaos", "Fractales", { v ->
        when (v % 3) {
            0 -> "On part d’un point quelconque et, 60 000 fois, on tire un sommet du pentagone au hasard et l’on s’en approche en ne gardant que 1/φ² ≈ 0,382 de la distance. " +
                "Les points dessinent cinq copies réduites du tout, qui se touchent : le pentagone de Sierpiński, de dimension log 5 / log φ² ≈ 1,672. Couleur : le dernier sommet tiré."
            1 -> "On part d’un point quelconque et, 60 000 fois, on va à mi-chemin d’un sommet du carré tiré au hasard, mais jamais deux fois de suite le même. " +
                "Sans cette règle, les points rempliraient le carré uniformément ; avec elle, une dentelle fractale apparaît. Chaque point a la couleur du dernier sommet tiré."
            else -> "On part d’un point quelconque et, 60 000 fois, on tire un sommet de l’hexagone au hasard et l’on s’en approche des deux tiers du chemin. " +
                "Les points dessinent six copies trois fois plus petites du tout, de dimension log 6 / log 3 ≈ 1,631 ; le trou du milieu est un flocon de Koch."
        }
    }) { chaosGame(it) },
    FigureSpec("terdragon", "Terdragon", "Fractales", { v ->
        val count = if (v % 2 == 0) "7 étapes, 3⁷ = 2 187" else "6 étapes, 3⁶ = 729"
        "Chaque segment est remplacé par trois segments √3 fois plus courts qui zigzaguent, tournant de 120° puis de −120°. Après $count segments " +
            "pour chacun des trois terdragons partis du même point, tournés d’un tiers de tour. Comme 3 × (1/√3)² = 1, la dimension vaut 2 : " +
            "à la limite la courbe couvre une surface, en touchant ses propres sommets sans jamais se croiser (Davis et Knuth, 1970)."
    }) { terdragon(it) },
    FigureSpec("devil_staircase", "Escalier du diable", "Fractales", { v ->
        val third = v % 2 == 0
        "La fonction de Cantor monte de 0 à 1 et elle est continue, mais elle ne grimpe que sur l’ensemble de Cantor (en bas) : sur chaque trou, elle reste à plat, " +
            "marche de hauteur k/2ⁿ. Sa dérivée est nulle presque partout, et pourtant elle monte ; sa courbe a pour longueur 2. Ici chaque étape garde deux morceaux " +
            "de ${if (third) "1/3" else "2/5"} : l’ensemble a pour dimension log 2 / log ${if (third) "3 ≈ 0,631" else "2,5 ≈ 0,756"}."
    }) { devilStaircase(it) },
    FigureSpec("dla", "Agrégation par diffusion", "Hasard", { v ->
        val c = dlaGrow(v)
        val where = if (v % 2 == 0) "d’un germe au centre" else "du bord du bas, comme du givre"
        "Des particules arrivent une à une, marchent au hasard sur le quadrillage et se collent dès qu’elles touchent l’amas, parti $where. Les pointes avancées " +
            "captent presque tout, les creux ne se remplissent jamais : des branches naissent. Ici ${grouped(c.n - c.seeds)} particules, coloriées selon leur ordre d’arrivée. " +
            "Dimension fractale ≈ 1,71 (Witten et Sander, 1981)."
    }) { dla(it) },
    FigureSpec("buffon", "Aiguilles de Buffon", "Hasard", { v ->
        val drop = buffonDrop(v)
        val l = if (drop.ratio == 1f) "ℓ = d" else "ℓ = 3d/4"
        val est = 2.0 * drop.ratio * drop.needles.size / drop.crossing
        "Buffon (1777) : une aiguille de longueur ℓ jetée au hasard sur un parquet de lames de largeur d croise une rainure avec la probabilité 2ℓ / (πd). " +
            "Ici $l et ${drop.needles.size} aiguilles, dont ${drop.crossing} (en rouge) croisent une rainure. D’où π ≈ ${if (drop.ratio == 1f) "2" else "1,5"} × ${drop.needles.size} / ${drop.crossing} ≈ ${dec(est, 2)} ; " +
            "il faudrait des millions d’aiguilles pour gagner quelques décimales."
    }) { buffon(it) },
    FigureSpec("percolation", "Percolation", "Hasard", { v ->
        val g = percolationGrid(v)
        val span = if (g.spans) "traverse la grille de haut en bas" else "ne traverse pas la grille de haut en bas"
        "Chaque case d’un quadrillage de ${PERC_COLS} × ${PERC_ROWS} est ouverte au hasard avec la probabilité p = ${dec(g.p, 2)} ; deux cases ouvertes voisines sont reliées. " +
            "Les plus gros amas sont coloriés. Sur un quadrillage infini, un amas infini apparaît dès que p dépasse un seuil critique, ≈ 0,5927, connu seulement par simulation. " +
            "Ici, le plus grand amas compte ${g.biggest} cases et $span."
    }) { percolation(it) },
    FigureSpec("dice_sum", "Somme de dés", "Hasard", { v ->
        val last = Fractal2.diceCounts(v).last()
        "Avec n dés, la somme va de n à 6n ; le nombre de façons d’obtenir chaque total se calcule de proche en proche, chaque dé ajoutant 1 à 6. Un dé : six barres égales ; " +
            "deux dés : un triangle, 7 sort 6 fois sur 36 ; trois dés : déjà une cloche. Avec $last dés, les barres collent à la courbe de Gauss de moyenne ${if (last % 2 == 0) "${last * 7 / 2}" else dec(3.5 * last, 1)} " +
            "et de variance 35 × $last / 12 (en gris) : c’est le théorème central limite."
    }) { diceSum(it) },
)

/** Paramètres des variantes, communs au dessin et à l'explication. */
private object Fractal2 {
    fun tetraDepth(v: Int): Int = if (v % 2 == 0) 3 else 4
    fun bifurcationRange(v: Int): Pair<Double, Double> = if (v % 2 == 0) 2.8 to 4.0 else 3.4 to 4.0
    fun plantSet(v: Int): IntArray = if (v % 2 == 0) intArrayOf(0, 5, 1) else intArrayOf(4, 2, 3)
    fun fibonacciLength(v: Int): Int = if (v % 2 == 0) 6765 else 2584
    fun diceCounts(v: Int): IntArray = if (v % 2 == 0) intArrayOf(1, 2, 3, 6) else intArrayOf(1, 2, 4, 8)
}

// ------------------------------------------------------------ Outils

/** Un nombre à virgule, à la française, avec le vrai signe moins. */
private fun dec(x: Double, digits: Int): String = String.format(Locale.ROOT, "%.${digits}f", x).replace('.', ',').replace('-', '−')

/** Un entier en chiffres en exposant. */
private fun sup(n: Int): String = n.toString().map { "⁰¹²³⁴⁵⁶⁷⁸⁹"[it - '0'] }.joinToString("")

/** Un entier, les milliers séparés par une espace. */
private fun grouped(n: Int): String = String.format(Locale.ROOT, "%,d", n).replace(',', ' ')

/** La couleur à la position [t] (de 0 à 1) d'un dégradé passant par les couleurs de [pal]. */
private fun ramp(pal: List<Color>, t: Float): Color {
    val x = t.coerceIn(0f, 1f) * (pal.size - 1)
    val i = min(floor(x).toInt(), pal.size - 2)
    return lerp(pal[i], pal[i + 1], x - i)
}

/** Des traits séparés, tracés en un seul chemin (bien plus rapide qu'un trait chacun). */
private fun Pencil.strokes(segs: List<Pair<Offset, Offset>>, color: Color, lw: Float, alpha: Float = 1f) {
    if (segs.isEmpty()) return
    val path = Path()
    for ((a, b) in segs) {
        val p = px(a)
        val q = px(b)
        path.moveTo(p.x, p.y)
        path.lineTo(q.x, q.y)
    }
    scope.drawPath(path, color, alpha = alpha, style = Stroke(lw * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
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

/** La case ([col], [row]) d'une grille de [cols] × [lines] cases de [cell] unités, centrée dans le cadre. */
private fun Pencil.cellAt(col: Int, row: Int, cols: Int, lines: Int, cell: Float, color: Color, alpha: Float) =
    scope.drawRect(
        color,
        topLeft = px(pt((FW - cols * cell) / 2 + col * cell + j(0.3f), (FH - lines * cell) / 2 + row * cell + j(0.3f))),
        size = Size(cell * k * 1.05f, cell * k * 1.05f),
        alpha = alpha,
    )

/** Le cadre au crayon d'une grille de [cols] × [lines] cases de [cell] unités, centrée. */
private fun Pencil.gridFrame(cols: Int, lines: Int, cell: Float) {
    val l = (FW - cols * cell) / 2
    val t = (FH - lines * cell) / 2
    outline(listOf(pt(l, t), pt(l + cols * cell, t), pt(l + cols * cell, t + lines * cell), pt(l, t + lines * cell)), lw = 1.5f)
}

/** Cadre de points ([xs], [ys]) dans la case, [trim] des points extrêmes écartés de chaque côté. */
private class CloudFrame(xs: FloatArray, ys: FloatArray, margin: Float, trim: Float) {
    val s: Float
    private val ox: Float
    private val oy: Float

    init {
        val sx = xs.copyOf().also { it.sort() }
        val sy = ys.copyOf().also { it.sort() }
        val cut = (xs.size * trim).toInt()
        val x0 = sx[cut]; val x1 = sx[xs.size - 1 - cut]
        val y0 = sy[cut]; val y1 = sy[ys.size - 1 - cut]
        s = min((FW - 2 * margin) / (x1 - x0), (FH - 2 * margin) / (y1 - y0))
        ox = (FW - (x1 - x0) * s) / 2 - x0 * s
        oy = (FH - (y1 - y0) * s) / 2 - y0 * s
    }

    fun x(v: Float) = v * s + ox
    fun y(v: Float) = v * s + oy
}

/** Un nuage de points mis en cases de [cell] unités : passages, teinte cumulée et dernière étiquette par case. */
private class Cloud(val cell: Float) {
    val cols = (FW / cell).toInt()
    val lines = (FH / cell).toInt()
    val count = IntArray(cols * lines)
    val tint = FloatArray(cols * lines)
    val label = IntArray(cols * lines) { -1 }
    var max = 1

    /** Un point (en unités de la case) de teinte [t] et d'étiquette [tag]. */
    fun add(x: Float, y: Float, t: Float, tag: Int = 0) {
        val c = floor((x - (FW - cols * cell) / 2) / cell).toInt()
        val r = floor((y - (FH - lines * cell) / 2) / cell).toInt()
        if (c < 0 || c >= cols || r < 0 || r >= lines) return
        val i = r * cols + c
        count[i]++
        tint[i] += t
        label[i] = tag
        if (count[i] > max) max = count[i]
    }

    /** Remplit le nuage avec les points ([xs], [ys]) cadrés, de teintes [ts] et d'étiquettes [tags]. */
    fun fill(xs: FloatArray, ys: FloatArray, ts: FloatArray?, tags: IntArray?, margin: Float, trim: Float): Cloud {
        val f = CloudFrame(xs, ys, margin, trim)
        for (i in xs.indices) add(f.x(xs[i]), f.y(ys[i]), ts?.get(i) ?: 0f, tags?.get(i) ?: 0)
        return this
    }
}

/** Dessine le nuage : chaque case foncée selon le logarithme de ses passages, teintée par [color] (teinte moyenne, étiquette). */
private fun Pencil.drawCloud(cloud: Cloud, alphaMax: Float, gamma: Float, floor: Float = 0f, color: (Float, Int) -> Color) {
    val lm = ln(1f + cloud.max)
    for (r in 0 until cloud.lines) for (c in 0 until cloud.cols) {
        val i = r * cloud.cols + c
        val n = cloud.count[i]
        if (n == 0) continue
        val a = floor + (alphaMax - floor) * (ln(1f + n) / lm).pow(gamma)
        cellAt(c, r, cloud.cols, cloud.lines, cloud.cell, color(cloud.tint[i] / n, cloud.label[i]), a)
    }
}

/** Un point de l'espace. */
private class P3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: P3) = P3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: P3) = P3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = P3(x * s, y * s, z * s)
    infix fun dot(o: P3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: P3) = P3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun unit(): P3 = this * (1f / max(sqrt(this dot this), 1e-6f))
}

// ------------------------------------------------------------ Fractales en relief

/**
 * Éponge de Menger en perspective isométrique, niveaux 1 et 2 : les petits cubes sont dessinés du plus
 * lointain au plus proche (algorithme du peintre), sans leurs faces collées à un voisin. Dessus pâle,
 * côtés hachurés ; les faces des tunnels sont plus sombres.
 */
private fun Pencil.menger(v: Int) {
    val sets = listOf(
        listOf(Crayons.jaune, Crayons.orange, Crayons.rouge),
        listOf(Crayons.turquoise, Crayons.bleu, Crayons.marine),
        listOf(Crayons.rose, Crayons.violet, Crayons.marine),
        listOf(Crayons.jaune, Crayons.vert, Crayons.sapin),
    )
    val pal = sets[v % sets.size]
    mengerCube(1, pt(80f, 136f), 64f, pal, 0.9f)
    mengerCube(2, pt(252f, 125f), 112f, pal, 0.6f)
}

/** Un cube de Menger de niveau [n], de côté [side] unités, centré en [o] (vue isométrique). */
private fun Pencil.mengerCube(n: Int, o: Offset, side: Float, pal: List<Color>, lw: Float) {
    var m = 1
    repeat(n) { m *= 3 }
    val u = side / m
    fun inside(x: Int, y: Int, z: Int): Boolean {
        if (x < 0 || y < 0 || z < 0 || x >= m || y >= m || z >= m) return false
        var a = x; var b = y; var c = z
        repeat(n) {
            if ((if (a % 3 == 1) 1 else 0) + (if (b % 3 == 1) 1 else 0) + (if (c % 3 == 1) 1 else 0) >= 2) return false
            a /= 3; b /= 3; c /= 3
        }
        return true
    }
    fun iso(x: Int, y: Int, z: Int) = o + Offset((x - y) * 0.866f * u, ((x + y) * 0.5f - z) * u)
    fun face(pts: List<Offset>, kind: Int, outer: Boolean) {
        scope.drawPath(path(pts), PaperColors.Paper)
        val c = if (outer) pal[kind] else lerp(pal[kind], Graphite, 0.35f)
        when (kind) {
            0 -> fill(pts, c, hatch = false, wash = 0.3f, lw = lw)
            1 -> fill(pts, c, angle = 30f, gap = 2.8f, wash = 0.42f, lw = lw, strokeAlpha = 0.7f)
            else -> fill(pts, c, angle = -62f, gap = 2.4f, wash = 0.58f, lw = lw, strokeAlpha = 0.8f)
        }
    }
    val cells = ArrayList<IntArray>()
    for (x in 0 until m) for (y in 0 until m) for (z in 0 until m) if (inside(x, y, z)) cells += intArrayOf(x, y, z)
    cells.sortBy { it[0] + it[1] + it[2] }
    for ((x, y, z) in cells) {
        if (!inside(x, y, z + 1)) face(listOf(iso(x, y, z + 1), iso(x + 1, y, z + 1), iso(x + 1, y + 1, z + 1), iso(x, y + 1, z + 1)), 0, z + 1 == m)
        if (!inside(x + 1, y, z)) face(listOf(iso(x + 1, y, z), iso(x + 1, y + 1, z), iso(x + 1, y + 1, z + 1), iso(x + 1, y, z + 1)), 1, x + 1 == m)
        if (!inside(x, y + 1, z)) face(listOf(iso(x, y + 1, z), iso(x + 1, y + 1, z), iso(x + 1, y + 1, z + 1), iso(x, y + 1, z + 1)), 2, y + 1 == m)
    }
}

/**
 * Tétraèdre de Sierpiński : 4ⁿ petits tétraèdres, tournés, projetés et dessinés du plus lointain au plus
 * proche ; chaque orientation de face a sa couleur, plus sombre à l'ombre.
 */
private fun Pencil.sierpinskiTetra(v: Int) {
    val depth = Fractal2.tetraDepth(v)
    val r = 2 * sqrt(2f) / 3
    val top = P3(0f, 1f, 0f)
    val base = (0 until 3).map { val a = rad(90f + 120f * it); P3(r * cos(a), -1f / 3, r * sin(a)) }
    val tets = ArrayList<List<P3>>()
    fun go(t: List<P3>, d: Int) {
        if (d == 0) { tets += t; return }
        for (i in 0..3) go(t.map { (it + t[i]) * 0.5f }, d - 1)
    }
    go(listOf(top) + base, depth)
    // Deux faces de côté, l'une plus de face que l'autre, vues d'un peu au-dessus.
    val yaw = rad(floatArrayOf(18f, -22f, 30f)[v % 3])
    val pitch = rad(16f)
    fun turn(p: P3): P3 {
        val x1 = p.x * cos(yaw) + p.z * sin(yaw)
        val z1 = -p.x * sin(yaw) + p.z * cos(yaw)
        return P3(x1, p.y * cos(pitch) - z1 * sin(pitch), p.y * sin(pitch) + z1 * cos(pitch))
    }
    val light = P3(-0.5f, 0.7f, 0.55f).unit()
    class Face(val pts: List<Offset>, val depth: Float, val kind: Int, val light: Float)
    val faces = ArrayList<Face>()
    for (t in tets) {
        val tt = t.map { turn(it) }
        val cen = (tt[0] + tt[1] + tt[2] + tt[3]) * 0.25f
        for (i in 0..3) {
            val f = tt.filterIndexed { idx, _ -> idx != i }
            var nrm = (f[1] - f[0]) cross (f[2] - f[0])
            if ((nrm dot (f[0] - cen)) < 0) nrm = nrm * -1f
            nrm = nrm.unit()
            if (nrm.z <= 0.02f) continue
            faces += Face(f.map { pt(it.x, -it.y) }, cen.z, i, nrm dot light)
        }
    }
    val fit = Fit(faces.map { it.pts }, 10f)
    // Couleurs des faces : le dessous (caché), puis les trois côtés.
    val pal = listOf(
        listOf(Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.jaune),
        listOf(Crayons.jaune, Crayons.turquoise, Crayons.bleu, Crayons.violet),
        listOf(Crayons.jaune, Crayons.vert, Crayons.sapin, Crayons.jaune),
    )[v % 3]
    for ((i, f) in faces.sortedBy { it.depth }.withIndex()) {
        val pts = fit(f.pts)
        val b = ((f.light + 1f) / 2f).coerceIn(0f, 1f)
        scope.drawPath(path(pts), PaperColors.Paper)
        fill(
            pts,
            lerp(pal[f.kind], Graphite, 0.3f * (1f - b)),
            hatch = depth == 3 || f.kind != 0,
            angle = -58f + 40f * f.kind + (i % 3) * 6f,
            gap = if (depth == 3) 2.6f + 2.2f * b else 2.2f + 1.4f * b,
            wash = 0.6f - 0.35f * b,
            lw = if (depth == 3) 1f else 0.7f,
        )
    }
}

// ------------------------------------------------------------ Nuages de points

/** Une transformation de flamme : affine (a, b, c, d, e, f), puis un mélange de variations, et sa teinte. */
private class FlameMap(val weight: Float, val m: FloatArray, val vars: List<Pair<Int, Float>>, val color: Float)

/** Une flamme : ses transformations, l'ordre de sa symétrie et ses mots pour l'explication. */
private class Flame(val maps: List<FlameMap>, val sym: Int, val words: String)

private val FLAMES = listOf(
    Flame(
        listOf(
            FlameMap(0.43f, floatArrayOf(-0.26f, 0.21f, 0.25f, -0.87f, -0.97f, 0.67f), listOf(3 to 1f), 0f),
            FlameMap(0.67f, floatArrayOf(0.43f, 0.08f, 0.10f, -0.21f, 0.72f, -0.54f), listOf(2 to 1f), 0.5f),
        ),
        4,
        "l’une passe en coordonnées polaires, l’autre tourne d’un angle qui croît avec la distance au centre",
    ),
    Flame(
        listOf(
            FlameMap(0.53f, floatArrayOf(-0.03f, -0.97f, -0.07f, -0.02f, -0.44f, 0.62f), listOf(1 to 1f), 0f),
            FlameMap(0.48f, floatArrayOf(-0.50f, 0.91f, 0.99f, -0.91f, 0.72f, 0.21f), listOf(4 to 1f, 0 to 0.53f), 0.5f),
            FlameMap(0.33f, floatArrayOf(-0.65f, -0.39f, -0.27f, -0.09f, 0.72f, -0.52f), listOf(5 to 1f), 1f),
        ),
        3,
        "l’une prend le sinus de chaque coordonnée, une autre enroule le plan en disque, la dernière en spirale",
    ),
)

private val flameCache = HashMap<Int, Cloud>()

/** Les passages de la flamme [p] dans les cases, calculés une fois. */
private fun flameCloud(p: Int): Cloud = flameCache.getOrPut(p) {
    val flame = FLAMES[p]
    val rnd = Random(11 + p)
    val total = flame.maps.sumOf { it.weight.toDouble() }.toFloat()
    val n = 150_000
    val xs = FloatArray(n); val ys = FloatArray(n); val ts = FloatArray(n)
    var x = 0.1f; var y = 0.1f; var c = 0.5f
    for (i in 0 until n + 30) {
        if (rnd.nextBoolean()) {
            val a = 2 * PI.toFloat() * (1 + rnd.nextInt(flame.sym - 1)) / flame.sym
            val nx = x * cos(a) - y * sin(a)
            y = x * sin(a) + y * cos(a)
            x = nx
        } else {
            var t = rnd.nextFloat() * total
            var f = flame.maps[0]
            for (mp in flame.maps) { f = mp; if (t < mp.weight) break; t -= mp.weight }
            val ax = f.m[0] * x + f.m[1] * y + f.m[4]
            val ay = f.m[2] * x + f.m[3] * y + f.m[5]
            val r2 = ax * ax + ay * ay + 1e-9f
            val r = sqrt(r2)
            val th = atan2(ay, ax)
            var nx = 0f; var ny = 0f
            for ((kind, w) in f.vars) {
                when (kind) {
                    0 -> { nx += w * ax; ny += w * ay }
                    1 -> { nx += w * sin(ax); ny += w * sin(ay) }
                    2 -> { nx += w * (ax * sin(r2) - ay * cos(r2)); ny += w * (ax * cos(r2) + ay * sin(r2)) }
                    3 -> { nx += w * th / PI.toFloat(); ny += w * (r - 1) }
                    4 -> { nx += w * th / PI.toFloat() * sin(PI.toFloat() * r); ny += w * th / PI.toFloat() * cos(PI.toFloat() * r) }
                    else -> { nx += w * (cos(th) + sin(r)) / r; ny += w * (sin(th) - cos(r)) / r }
                }
            }
            x = nx; y = ny
            c = (c + f.color) / 2
        }
        if (x.isNaN() || y.isNaN() || abs(x) > 1e5f || abs(y) > 1e5f) { x = rnd.nextFloat(); y = rnd.nextFloat() }
        if (i >= 30) { xs[i - 30] = x; ys[i - 30] = y; ts[i - 30] = c }
    }
    Cloud(1.5f).fill(xs, ys, ts, null, 8f, 0.003f)
}

/** Flamme fractale : un système de fonctions itérées non linéaires, avec une symétrie de rotation. */
private fun Pencil.fractalFlame(v: Int) {
    val pals = listOf(
        listOf(Crayons.marine, Crayons.violet, Crayons.rose, Crayons.rouge, Crayons.orange, Crayons.jaune),
        listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange),
    )
    val pal = pals[(v / FLAMES.size) % pals.size]
    drawCloud(flameCloud(v % FLAMES.size), 1f, 0.6f, 0.3f) { t, _ -> ramp(pal, t) }
}

/** Paramètres (a, b, c, d) des attracteurs de Clifford, un par variante. */
private val CLIFFORD = listOf(
    listOf(-1.4, 1.6, 1.0, 0.7),
    listOf(1.5, -1.8, 1.6, 0.9),
    listOf(1.6, -0.6, -1.2, 1.6),
)

private val cliffordCache = HashMap<Int, Cloud>()

/** Attracteur de Clifford : 100 000 points, teintés selon la direction de leur saut. */
private fun Pencil.clifford(v: Int) {
    val p = v % CLIFFORD.size
    val cloud = cliffordCache.getOrPut(p) {
        val (a, b, c, d) = CLIFFORD[p].map { it.toFloat() }
        val n = 100_000
        val xs = FloatArray(n); val ys = FloatArray(n); val ts = FloatArray(n)
        var x = 0.1f; var y = 0.1f
        for (i in 0 until n + 50) {
            val nx = sin(a * y) + c * cos(a * x)
            val ny = sin(b * x) + d * cos(b * y)
            if (i >= 50) {
                xs[i - 50] = nx; ys[i - 50] = ny
                ts[i - 50] = (atan2(ny - y, nx - x) + PI.toFloat()) / (2 * PI.toFloat())
            }
            x = nx; y = ny
        }
        Cloud(1.5f).fill(xs, ys, ts, null, 8f, 0.002f)
    }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val loop = pal + pal[0]
    drawCloud(cloud, 1f, 0.6f, 0.28f) { t, _ -> ramp(loop, t) }
}

private val rauzyCache = HashMap<Int, Cloud>()

/**
 * Fractale de Rauzy : les sommes Σ εₖ αᵏ (εₖ = 0 ou 1, jamais trois 1 de suite) pour 18 chiffres, mises en
 * cases ; chaque case prend la couleur de son morceau (premier bloc 0, 10 ou 110, ou les deux premiers
 * blocs), et un trait de crayon marque les frontières.
 */
private fun Pencil.rauzy(v: Int) {
    val fine = v % 2 == 1
    val cloud = rauzyCache.getOrPut(v % 2) {
        val ar = -0.41964337760708; val ai = 0.60629072920719 * (if (fine) -1 else 1)
        val len = 18
        val pr = DoubleArray(len); val pi = DoubleArray(len)
        pr[0] = 1.0
        for (i in 1 until len) { pr[i] = pr[i - 1] * ar - pi[i - 1] * ai; pi[i] = pr[i - 1] * ai + pi[i - 1] * ar }
        val xs = ArrayList<Float>(70000); val ys = ArrayList<Float>(70000); val tags = ArrayList<Int>(70000)
        val digits = IntArray(len)
        // Le bloc qui commence au rang i : 0, 10 ou 110 ; son numéro et sa longueur.
        fun block(i: Int): Pair<Int, Int> = when {
            i >= len || digits[i] == 0 -> 0 to 1
            i + 1 >= len || digits[i + 1] == 0 -> 1 to 2
            else -> 2 to 3
        }
        fun go(i: Int, x: Double, y: Double, run: Int) {
            if (i == len) {
                // Les trois premiers blocs : le morceau, son sous-morceau, et le suivant.
                val (b1, l1) = block(0)
                val (b2, l2) = block(l1)
                val tag = b1 * 9 + b2 * 3 + block(l1 + l2).first
                xs += x.toFloat(); ys += y.toFloat(); tags += tag
                return
            }
            digits[i] = 0
            go(i + 1, x, y, 0)
            if (run < 2) {
                digits[i] = 1
                go(i + 1, x + pr[i], y + pi[i], run + 1)
            }
        }
        go(0, 0.0, 0.0, 0)
        Cloud(2f).fill(xs.toFloatArray(), ys.toFloatArray(), null, tags.toIntArray(), 8f, 0f)
    }
    val pal = if (fine) {
        listOf(Crayons.rouge, Crayons.orange, Crayons.rose, Crayons.jaune, Crayons.vert, Crayons.sapin, Crayons.bleu, Crayons.turquoise, Crayons.violet)
    } else {
        listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange).rot((v / 2) % 6).take(3)
    }
    for (r in 0 until cloud.lines) for (c in 0 until cloud.cols) {
        val i = r * cloud.cols + c
        val label = cloud.label[i]
        if (label < 0) continue
        // La couleur dit le morceau (ou le sous-morceau), la nuance l'étape suivante.
        val tag = if (fine) label / 3 else label / 9
        val shade = if (fine) label % 3 else label / 3 % 3
        // Une case au bord de son morceau (un voisin vide ou d'un autre morceau) est repassée au crayon.
        var edge = false
        for ((dc, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nc = c + dc; val nr = r + dr
            val other = if (nc < 0 || nr < 0 || nc >= cloud.cols || nr >= cloud.lines) -1 else cloud.label[nr * cloud.cols + nc]
            if (other < 0 || (if (fine) other / 3 else other / 9) != tag) edge = true
        }
        if (edge) cellAt(c, r, cloud.cols, cloud.lines, cloud.cell, lerp(pal[tag], Graphite, 0.6f), 0.75f)
        else cellAt(c, r, cloud.cols, cloud.lines, cloud.cell, pal[tag], 0.32f + 0.17f * shade)
    }
}

private val chaosCache = HashMap<Int, Cloud>()

/** Jeu du chaos : 60 000 sauts vers un sommet tiré au hasard ; chaque point a la couleur du dernier sommet. */
private fun Pencil.chaosGame(v: Int) {
    val kind = v % 3
    val sides = intArrayOf(5, 4, 6)[kind]
    val keep = floatArrayOf((3 - sqrt(5f)) / 2, 0.5f, 1f / 3)[kind]
    val turn = floatArrayOf(-90f, -45f, 0f)[kind]
    val corners = List(sides) { dir(rad(turn + 360f * it / sides)) }
    val cloud = chaosCache.getOrPut(kind) {
        val rnd = Random(2024 + kind)
        val n = 60_000
        val xs = FloatArray(n + 2 * sides); val ys = FloatArray(n + 2 * sides); val tags = IntArray(n + 2 * sides) { -1 }
        var p = Offset(0.1f, 0.05f)
        var last = -1
        for (i in 0 until n + 20) {
            var c = rnd.nextInt(sides)
            if (kind == 1) while (c == last) c = rnd.nextInt(sides)
            last = c
            p = corners[c] + (p - corners[c]) * keep
            if (i >= 20) { xs[i - 20] = p.x; ys[i - 20] = p.y; tags[i - 20] = c }
        }
        // Les sommets eux-mêmes (étiquette −1, non dessinés) fixent le cadre.
        for (s in 0 until sides) { xs[n + s] = corners[s].x; ys[n + s] = corners[s].y }
        for (s in 0 until sides) { xs[n + sides + s] = corners[s].x; ys[n + sides + s] = corners[s].y }
        val cloud = Cloud(1.4f)
        val f = CloudFrame(xs, ys, 14f, 0f)
        for (i in 0 until n) cloud.add(f.x(xs[i]), f.y(ys[i]), 0f, tags[i])
        cloud
    }
    val pal = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose)
        .rot((v * 3) % 8)
    drawCloud(cloud, 0.85f, 0.55f) { _, tag -> pal[tag % pal.size] }
}

// ------------------------------------------------------------ Fractales en cases

/** Une fractale de Lyapunov : motif, et fenêtre (a en largeur, b en hauteur). */
private class Lyapunov(val seq: String, val a0: Double, val a1: Double, val b0: Double, val b1: Double)

private val LYAPUNOV = listOf(
    Lyapunov("AB", 2.5, 4.0, 3.0, 4.0),
    Lyapunov("BBBBBBAAAAAA", 2.5, 3.4, 3.4, 4.0),
    Lyapunov("AABAB", 2.0, 4.0, 2.6, 4.0),
)

private val lyapunovCache = HashMap<Int, FloatArray>()

/** Fractale de Lyapunov : l'exposant λ de chaque case, en tons chauds s'il est négatif, en bleu sinon. */
private fun Pencil.lyapunov(v: Int) {
    val cell = 2f
    val cols = ((FW - 20f) / cell).toInt()
    val lines = ((FH - 18f) / cell).toInt()
    val l = LYAPUNOV[v % LYAPUNOV.size]
    val lam = lyapunovCache.getOrPut(v % LYAPUNOV.size) {
        val out = FloatArray(cols * lines)
        for (row in 0 until lines) for (col in 0 until cols) {
            val a = l.a0 + (l.a1 - l.a0) * (col + 0.5) / cols
            val b = l.b1 - (l.b1 - l.b0) * (row + 0.5) / lines
            var x = 0.5
            var sum = 0.0
            for (n in 0 until 130) {
                val r = if (l.seq[n % l.seq.length] == 'A') a else b
                x = r * x * (1 - x)
                if (n >= 30) sum += ln(abs(r * (1 - 2 * x)) + 1e-12)
            }
            out[row * cols + col] = (sum / 100).toFloat()
        }
        out
    }
    val warm = if (v / LYAPUNOV.size % 2 == 0) listOf(Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.brun) else
        listOf(Crayons.jaune, Crayons.vert, Crayons.sapin, Crayons.brun)
    val cold = if (v / LYAPUNOV.size % 2 == 0) listOf(Crayons.bleu, Crayons.marine) else listOf(Crayons.violet, Crayons.marine)
    for (row in 0 until lines) for (col in 0 until cols) {
        val x = lam[row * cols + col]
        if (x < 0) {
            val t = (-x / 1.2f).coerceIn(0f, 1f)
            cellAt(col, row, cols, lines, cell, ramp(warm, t), 0.18f + 0.6f * t.pow(0.6f))
        } else {
            val t = (x / 0.6f).coerceIn(0f, 1f)
            cellAt(col, row, cols, lines, cell, ramp(cold, t), 0.1f + 0.5f * t)
        }
    }
    gridFrame(cols, lines, cell)
}

/** Une vue de la vallée des hippocampes : centre, largeur et nombre d'étapes au plus. */
private class Seahorse(val cx: Double, val cy: Double, val w: Double, val iter: Int)

private val SEAHORSES = listOf(Seahorse(-0.7436, 0.1318, 0.006, 600), Seahorse(-0.74364, 0.13188, 0.0035, 700))

private val seahorseCache = HashMap<Int, FloatArray>()

/** Vallée des hippocampes : un zoom dans l'ensemble de Mandelbrot, en couleurs selon log(étapes). */
private fun Pencil.seahorse(v: Int) {
    val cell = 2f
    val cols = ((FW - 20f) / cell).toInt()
    val lines = ((FH - 18f) / cell).toInt()
    val s = SEAHORSES[v % SEAHORSES.size]
    val vals = seahorseCache.getOrPut(v % SEAHORSES.size) {
        val out = FloatArray(cols * lines)
        val span = s.w / cols
        for (row in 0 until lines) for (col in 0 until cols) {
            val cr = s.cx + (col - cols / 2 + 0.5) * span
            val ci = s.cy - (row - lines / 2 + 0.5) * span
            var zr = 0.0; var zi = 0.0
            var n = 0
            while (n < s.iter && zr * zr + zi * zi <= 4.0) {
                val t = zr * zr - zi * zi + cr
                zi = 2 * zr * zi + ci
                zr = t
                n++
            }
            // Compte lissé : n + 1 − log₂ log |z|, et −1 pour les points de l'ensemble.
            out[row * cols + col] = if (n >= s.iter) -1f else
                (n + 1 - ln(ln(sqrt(zr * zr + zi * zi))) / ln(2.0)).toFloat().coerceAtLeast(1f)
        }
        out
    }
    var lo = Float.MAX_VALUE
    for (x in vals) if (x > 0 && x < lo) lo = x
    val lmin = ln(lo); val lmax = ln(s.iter.toFloat())
    val base = Crayons.rainbow.rot((v * 3 + 4) % 8)
    val loop = listOf(Crayons.marine) + base.take(6) + Crayons.marine
    for (row in 0 until lines) for (col in 0 until cols) {
        val x = vals[row * cols + col]
        if (x < 0) { cellAt(col, row, cols, lines, cell, Graphite, 0.75f); continue }
        val l = ln(x)
        val u = ((l - lmin) / (lmax - lmin)).coerceIn(0f, 1f)
        cellAt(col, row, cols, lines, cell, ramp(loop, (l * 1.6f) % 1f), 0.2f + 0.65f * u)
    }
    gridFrame(cols, lines, cell)
}

private val twindragonCache = HashMap<Int, Array<IntArray>>()

/**
 * Dragon jumeau : chaque case est écrite en base −1 + i (chiffres 0 et 1) ; sa partie entière (un entier
 * de Gauss) dit à quelle tuile elle appartient, son premier chiffre après la virgule à quelle moitié.
 */
private fun Pencil.twindragon(v: Int) {
    val cell = 2f
    val cols = ((FW - 20f) / cell).toInt()
    val lines = ((FH - 18f) / cell).toInt()
    val shift = v % 2
    val (tiles, halves) = twindragonCache.getOrPut(shift) {
        val tile = IntArray(cols * lines)
        val half = IntArray(cols * lines)
        val n = 12
        val span = 5.2 / cols
        // (−1 + i)¹² = −64 : on multiplie par b¹² et on arrondit à l'entier de Gauss le plus proche.
        for (row in 0 until lines) for (col in 0 until cols) {
            val zr = (col - cols / 2 + 0.5) * span + 0.25 * shift
            val zi = (row - lines / 2 + 0.5) * span + 0.4 * shift
            var a = (-64 * zr).roundToInt()
            var b = (-64 * zi).roundToInt()
            var first = 0
            repeat(n) {
                val d = (a + b) and 1
                a -= d
                // (a + b i) / (−1 + i) = (−a + b)/2 + (−a − b)/2 i
                val na = (-a + b) / 2
                val nb = (-a - b) / 2
                a = na; b = nb
                first = d
            }
            tile[row * cols + col] = (a + 512) * 1024 + (b + 512)
            half[row * cols + col] = first
        }
        arrayOf(tile, half)
    }
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.turquoise, Crayons.violet, Crayons.orange, Crayons.vert, Crayons.rose, Crayons.bleu)
        .rot((v / 2 * 3) % 8).take(5)
    for (row in 0 until lines) for (col in 0 until cols) {
        val i = row * cols + col
        val t = tiles[i]
        val edge = (col + 1 < cols && tiles[i + 1] != t) || (row + 1 < lines && tiles[i + cols] != t)
        if (edge) { cellAt(col, row, cols, lines, cell, Graphite, 0.62f); continue }
        val a = t / 1024 - 512
        val b = t % 1024 - 512
        val color = pal[Math.floorMod(a + 2 * b, 5)]
        val home = a == 0 && b == 0
        cellAt(col, row, cols, lines, cell, color, (if (halves[i] == 0) 0.5f else 0.28f) + (if (home) 0.25f else 0f))
    }
    gridFrame(cols, lines, cell)
}

private val bifurcationCache = HashMap<Int, IntArray>()

/** Diagramme de bifurcation de la suite logistique : où la suite revient, pour chaque r ; graduations aux points remarquables. */
private fun Pencil.bifurcation(v: Int) {
    val (r0, r1) = Fractal2.bifurcationRange(v)
    val cell = 1.5f
    val left = 12f; val top = 8f
    val cols = ((FW - 2 * left) / cell).toInt()
    val lines = ((FH - top - 22f) / cell).toInt()
    val counts = bifurcationCache.getOrPut(v % 2) {
        val out = IntArray(cols * lines)
        for (col in 0 until cols) for (sub in 0 until 2) {
            val r = r0 + (r1 - r0) * (col + 0.25 + 0.5 * sub) / cols
            var x = 0.5
            repeat(300) { x = r * x * (1 - x) }
            repeat(240) {
                x = r * x * (1 - x)
                val row = ((1 - x) * lines).toInt().coerceIn(0, lines - 1)
                out[row * cols + col]++
            }
        }
        out
    }
    val pal = Crayons.rainbow.rot((v / 2 * 5) % 8)
    val lm = ln(1f + 120f)
    for (row in 0 until lines) for (col in 0 until cols) {
        val n = counts[row * cols + col]
        if (n == 0) continue
        val color = ramp(pal, col.toFloat() / cols)
        val a = (0.22f + 0.7f * ln(1f + n) / lm).coerceAtMost(0.92f)
        scope.drawRect(color, topLeft = px(pt(left + col * cell, top + row * cell)), size = Size(cell * k * 1.05f, cell * k * 1.05f), alpha = a)
    }
    // L'axe des r et ses graduations : 3, 1 + √6, l'accumulation 3,5699 et la fenêtre 1 + √8.
    val base = top + lines * cell + 6f
    line(listOf(pt(left, base), pt(left + cols * cell, base)), Graphite, 1.1f, 0.8f)
    for (r in listOf(3.0, 1 + sqrt(6.0), 3.5441, 3.5699, 1 + sqrt(8.0), 4.0)) {
        if (r < r0) continue
        val x = left + ((r - r0) / (r1 - r0) * cols * cell).toFloat()
        stroke(pt(x, base - 3f), pt(x, base + 5f), Graphite, 1f, 0.8f)
    }
}

// ------------------------------------------------------------ Courbes et figures au crayon

/** Chou romanesco : un chou de bourgeons en phyllotaxie, chacun un petit chou ; à droite, un bourgeon à la loupe. */
private fun Pencil.romanesco(v: Int) {
    val sign = if (v % 2 == 0) 1f else -1f
    val pal = if (v % 2 == 0) listOf(Color(0xFFC9DB5A), Crayons.vert, Crayons.sapin) else listOf(Crayons.jaune, Crayons.turquoise, Crayons.sapin)
    val main = pt(126f, 125f)
    val big = romanescoHead(main, 114f, 280, 1.013f, 0.14f, sign, pal, 1f)
    // Le bourgeon qu'on montre à la loupe : le plus à droite du bord.
    val (c, r) = big.maxBy { it.first.x + it.second * 2 }
    val lens = pt(298f, 125f)
    val lr = 60f
    fill(circle(lens.x, lens.y, lr, 90), Color(0xFFEFF5D8), hatch = false, wash = 0.6f, outline = false)
    romanescoHead(lens, lr - 3f, 150, 1.025f, 0.2f, sign, pal, 0.85f)
    outline(circle(lens.x, lens.y, lr, 90), lw = 1.6f)
    outline(circle(c.x, c.y, r + 1.5f, 20), lw = 1f)
    // Les deux traits qui relient le bourgeon à sa loupe.
    val d = (lens - c) / (lens - c).getDistance()
    val n = Offset(-d.y, d.x)
    line(listOf(c + n * (r + 1.5f), lens + n * lr), Graphite, 0.8f, 0.55f)
    line(listOf(c - n * (r + 1.5f), lens - n * lr), Graphite, 0.8f, 0.55f)
}

/**
 * Une tête de chou : [n] bourgeons qui tournent de l'angle d'or et s'écartent du centre d'un facteur [q],
 * de rayon [frac] fois leur distance au centre ; chacun garni d'une petite spirale de grains. Rend les bourgeons (centre, rayon).
 */
private fun Pencil.romanescoHead(
    c: Offset,
    radius: Float,
    n: Int,
    q: Float,
    frac: Float,
    sign: Float,
    pal: List<Color>,
    grains: Float,
): List<Pair<Offset, Float>> {
    val golden = rad(137.508f) * sign
    val outer = radius / (1 + frac)
    val buds = (n - 1 downTo 0).map { i ->
        val rho = outer * q.pow(i - n + 1)
        c + dir(golden * i) * rho to rho * frac
    }
    for ((idx, b) in buds.withIndex()) {
        val (o, r) = b
        if (r < 0.6f) continue
        val t = idx.toFloat() / buds.size
        val color = ramp(pal.reversed(), t)
        // Un cône vu d'en haut : sombre au pied, clair à la pointe.
        fill(circle(o.x, o.y, r, max(10, (r * 1.4f).toInt())), lerp(color, Graphite, 0.15f), hatch = false, wash = 0.6f, lw = (r / 14f).coerceIn(0.3f, 0.75f))
        dot(o, r * 0.62f, lerp(color, Color.White, 0.25f), 0.55f)
        dot(o, r * 0.3f, lerp(color, Color.White, 0.55f), 0.6f)
        // Les grains du bourgeon : une spirale de Vogel, plus gros vers le bord.
        val m = (r * 2.6f * grains).toInt().coerceIn(0, 40)
        for (g in 1..m) {
            val gr = r * 0.84f * sqrt(g.toFloat() / m)
            val size = r * 0.14f * (0.45f + 0.55f * g / m)
            if (size < 0.35f) continue
            dot(o + dir(golden * g) * gr, size, lerp(color, Graphite, 0.3f), 0.75f)
        }
    }
    return buds
}

/** Une règle de plante : axiome, règles, angle, nombre d'étapes et la règle écrite pour l'explication. */
private class PlantRule(val axiom: String, val rules: Map<Char, String>, val angle: Float, val n: Int, val rule: String)

private val PLANTS = listOf(
    PlantRule("F", mapOf('F' to "F[+F]F[-F]F"), 25.7f, 4, "F → F[+F]F[−F]F"),
    PlantRule("F", mapOf('F' to "F[+F]F[-F][F]"), 20f, 4, "F → F[+F]F[−F][F]"),
    PlantRule("F", mapOf('F' to "FF-[-F+F+F]+[+F-F-F]"), 22.5f, 3, "F → FF−[−F+F+F]+[+F−F−F]"),
    PlantRule("X", mapOf('X' to "F[+X]F[-X]+X", 'F' to "FF"), 20f, 6, "X → F[+X]F[−X]+X et F → FF"),
    PlantRule("X", mapOf('X' to "F[+X][-X]FX", 'F' to "FF"), 25.7f, 6, "X → F[+X][−X]FX et F → FF"),
    PlantRule("X", mapOf('X' to "F-[[X]+X]+F[+FX]-X", 'F' to "FF"), 22.5f, 5, "X → F−[[X]+X]+F[+FX]−X et F → FF"),
)

/** Une plante dessinée : ses segments (avec leur profondeur de branche) et le bout de ses branches. */
private class Plant(val segs: List<Triple<Offset, Offset, Int>>, val tips: List<Offset>)

private val plantCache = HashMap<Int, Plant>()

private fun growPlant(i: Int): Plant = plantCache.getOrPut(i) {
    val rule = PLANTS[i]
    val s = lsystem(rule.axiom, rule.rules, rule.n)
    val segs = ArrayList<Triple<Offset, Offset, Int>>()
    val tips = ArrayList<Offset>()
    val stack = ArrayDeque<Triple<Offset, Float, Int>>()
    var p = Offset.Zero
    var a = rad(-90f)
    var depth = 0
    for (c in s) {
        when (c) {
            'F' -> { val q = p + dir(a); segs += Triple(p, q, depth); p = q }
            '+' -> a += rad(rule.angle)
            '-' -> a -= rad(rule.angle)
            '[' -> { stack.addLast(Triple(p, a, depth)); depth++ }
            ']' -> { tips += p; val t = stack.removeLast(); p = t.first; a = t.second; depth = t.third }
        }
    }
    tips += p
    Plant(segs, tips)
}

/** Herbier en L-système : trois plantes de règles différentes, posées côte à côte, avec leurs fleurs. */
private fun Pencil.plants(v: Int) {
    val set = Fractal2.plantSet(v)
    val boxes = listOf(floatArrayOf(8f, 122f, 196f), floatArrayOf(118f, 250f, 230f), floatArrayOf(246f, 360f, 196f))
    val ground = 240f
    val greens = listOf(
        listOf(Crayons.brun, Crayons.sapin, Crayons.vert, Color(0xFF9CCB5A)),
        listOf(Crayons.brun, Crayons.sapin, Crayons.turquoise, Crayons.vert),
        listOf(Crayons.brun, Crayons.vert, Color(0xFF9CCB5A), Crayons.jaune),
    )
    val flowers = listOf(Crayons.rose, Crayons.jaune, Crayons.violet, Crayons.orange, Crayons.rouge).rot(v % 5)
    line(listOf(pt(8f, ground + 2f), pt(360f, ground + 2f)), Crayons.brun, 1.2f, 0.5f)
    for ((slot, idx) in set.withIndex()) {
        val plant = growPlant(idx)
        var x0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
        for ((a, b, _) in plant.segs) for (o in listOf(a, b)) { x0 = min(x0, o.x); x1 = max(x1, o.x); y0 = min(y0, o.y); y1 = max(y1, o.y) }
        val (bx0, bx1, h) = boxes[slot].let { Triple(it[0], it[1], it[2]) }
        val s = min((bx1 - bx0 - 6f) / (x1 - x0), h / (y1 - y0))
        val ox = (bx0 + bx1) / 2 - (x0 + x1) / 2 * s
        val oy = ground - y1 * s
        fun at(o: Offset) = pt(o.x * s + ox, o.y * s + oy)
        val pal = greens[(slot + v) % greens.size]
        val byDepth = plant.segs.groupBy { min(it.third, 3) }
        for ((d, segs) in byDepth.toSortedMap()) {
            val lw = when (d) { 0 -> 2.6f; 1 -> 1.8f; 2 -> 1.3f; else -> 1f } * (if (slot == 1) 1f else 0.9f)
            strokes(segs.map { at(it.first) to at(it.second) }, pal[d], lw, 0.9f)
            strokes(segs.map { at(it.first) to at(it.second) }, pal[d], lw * 0.6f, 0.4f)
        }
        val flower = flowers[slot]
        for ((t, tip) in plant.tips.withIndex()) if (t % 2 == 0) dot(at(tip), 1.5f, flower, 0.75f)
    }
}

/** Perles d'Indra : cinq cercles, et leurs reflets successifs par inversion dans les autres. */
private fun Pencil.indra(v: Int) {
    val big = v % 2 == 0
    // Cinq cercles presque tangents : plus ils se serrent, plus les reflets sont gros.
    val side = if (big) 56f else 49f
    val mid = 121.3f - side - 1f
    val gens = listOf(
        Triple(184f, 125f, mid),
        Triple(78f, 66f, side),
        Triple(290f, 66f, side),
        Triple(290f, 184f, side),
        Triple(78f, 184f, side),
    )
    val pal = listOf(Crayons.jaune, Crayons.rouge, Crayons.bleu, Crayons.vert, Crayons.violet, Crayons.orange, Crayons.turquoise, Crayons.rose)
        .rot((v * 3) % 8).take(5)
    // Les perles d'un niveau, puis du suivant : (centre x, centre y, rayon, cercle où elle est, couleur d'origine).
    class Pearl(val x: Float, val y: Float, val r: Float, val inside: Int, val color: Int)
    var level = gens.mapIndexed { i, (x, y, r) -> Pearl(x, y, r, i, i) }
    var depth = 0
    while (level.isNotEmpty()) {
        for (p in level) {
            val color = pal[p.color]
            when {
                depth == 0 -> fill(circle(p.x, p.y, p.r, 90), color, gap = 5.5f, wash = 0.14f, strokeAlpha = 0.35f, lw = 1.4f, angle = -58f + 37f * p.color)
                p.r > 7f -> fill(circle(p.x, p.y, p.r, max(24, (p.r * 1.3f).toInt())), color, gap = (p.r / 6f).coerceIn(2.2f, 3.6f),
                    wash = 0.45f, lw = if (p.r > 20f) 1.1f else 0.8f, angle = -58f + 37f * p.color + 9f * depth)
                p.r > 2.2f -> fill(circle(p.x, p.y, p.r, 12), color, hatch = false, wash = 0.6f, lw = 0.45f)
                else -> dot(pt(p.x, p.y), p.r, color, 0.8f)
            }
        }
        val next = ArrayList<Pearl>()
        for (p in level) for ((g, gen) in gens.withIndex()) {
            if (g == p.inside) continue
            val (gx, gy, gr) = gen
            val dx = p.x - gx; val dy = p.y - gy
            val s = gr * gr / (dx * dx + dy * dy - p.r * p.r)
            val r = p.r * abs(s)
            if (r < 0.9f) continue
            next += Pearl(gx + dx * s, gy + dy * s, r, g, p.color)
        }
        level = next
        depth++
    }
}

/** Fractale du mot de Fibonacci : un pas par lettre, un quart de tour sur chaque 0 (selon la parité du rang). */
private fun Pencil.fibonacciWord(v: Int) {
    val len = Fractal2.fibonacciLength(v)
    var a = "0"; var b = "01"
    while (b.length < len) { val c = b + a; a = b; b = c }
    val word = b.substring(0, len)
    var d = 0
    var p = Offset.Zero
    val steps = listOf(pt(1f, 0f), pt(0f, -1f), pt(-1f, 0f), pt(0f, 1f))
    val pts = ArrayList<Offset>(len + 1)
    pts += p
    for (i in word.indices) {
        p += steps[d]
        pts += p
        if (word[i] == '0') d = if ((i + 1) % 2 == 0) (d + 1) % 4 else (d + 3) % 4
    }
    // Plus haute que large, on la couche.
    val tall = pts.maxOf { it.y } - pts.minOf { it.y } > pts.maxOf { it.x } - pts.minOf { it.x }
    val laid = if (tall) pts.map { pt(it.y, -it.x) } else pts
    val fit = Fit(listOf(laid), 10f)
    rainbowLine(fit(laid), Crayons.rainbow.rot((v * 3) % 8), if (len > 3000) 1.1f else 1.4f, 16)
}

/** Terdragon : trois courbes parties du même point, tournées d'un tiers de tour, chacune sa couleur. */
private fun Pencil.terdragon(v: Int) {
    val fine = v % 2 == 0
    val cmds = lsystem("F", mapOf('F' to "F+F-F"), if (fine) 7 else 6)
    val curves = (0 until 3).map { turtle(cmds, 120f, setOf('F'), 90f + 120f * it) }
    val fit = Fit(curves, 10f)
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.jaune, Crayons.vert, Crayons.violet, Crayons.orange).rot((v * 5) % 6)
    curves.forEachIndexed { i, c -> line(fit(c), pal[i], if (fine) 0.9f else 1.3f, 0.9f) }
    val o = fit(Offset.Zero)
    fill(circle(o.x, o.y, 4f, 14), Crayons.jaune, hatch = false, wash = 0.9f, lw = 1f)
}

/** Escalier du diable : les marches de la fonction de Cantor en colonnes coloriées, la courbe, et l'ensemble de Cantor dessous. */
private fun Pencil.devilStaircase(v: Int) {
    val r = if (v % 2 == 0) 1f / 3 else 0.4f
    val x0 = 16f; val x1 = 352f
    val y0 = 12f; val base = 222f
    val w = x1 - x0; val h = base - y0
    val pal = Crayons.rainbow.rot((v / 2 * 3) % 8)
    val curve = ArrayList<Offset>()
    val cantor = ArrayList<Pair<Float, Float>>()
    fun go(a: Float, b: Float, ya: Float, yb: Float, d: Int) {
        if (d == 7) {
            curve += pt(x0 + a * w, base - ya * h); curve += pt(x0 + b * w, base - yb * h)
            cantor += a to b
            return
        }
        val l = b - a
        val m = (ya + yb) / 2
        go(a, a + r * l, ya, m, d + 1)
        // La marche : une colonne du sol jusqu'à sa hauteur, coloriée selon l'étape.
        val p0 = x0 + (a + r * l) * w; val p1 = x0 + (b - r * l) * w
        if (p1 - p0 > 0.8f) {
            val top = base - m * h
            fill(listOf(pt(p0, top), pt(p1, top), pt(p1, base), pt(p0, base)), pal[d], hatch = p1 - p0 > 5f, gap = 3f, wash = if (p1 - p0 > 5f) 0.3f else 0.55f,
                lw = if (p1 - p0 > 5f) 0.9f else 0.4f, angle = -58f + 18f * d)
        }
        go(b - r * l, b, m, yb, d + 1)
    }
    go(0f, 1f, 0f, 1f, 0)
    line(curve, Graphite, 1.5f)
    for ((a, b) in cantor) {
        val p0 = x0 + a * w; val p1 = max(x0 + b * w, p0 + 0.6f)
        scope.drawRect(Graphite, topLeft = px(pt(p0, base + 9f)), size = Size((p1 - p0) * k, 6f * k), alpha = 0.75f)
    }
    line(listOf(pt(x0, base), pt(x1, base)), Graphite, 1f, 0.7f)
}

// ------------------------------------------------------------ Hasard

/** Un amas d'agrégation : positions des particules (en cases), leur parent, et le nombre de germes. */
private class DlaCluster(val cols: Int, val lines: Int, val x: IntArray, val y: IntArray, val parent: IntArray, val n: Int, val seeds: Int)

private val dlaCache = HashMap<Int, DlaCluster>()

/**
 * Agrégation limitée par diffusion, sur un quadrillage de 220 × 150 cases : un germe au centre (variante
 * paire) ou toute la ligne du bas. Chaque particule part d'un peu au-delà de l'amas, marche au hasard et se
 * colle au premier voisin occupé ; trop loin, elle repart.
 */
private fun dlaGrow(v: Int): DlaCluster = dlaCache.getOrPut(v) {
    val cols = 220; val lines = 150
    val rnd = Random(4242 + v * 31)
    val radial = v % 2 == 0
    val owner = IntArray(cols * lines) { -1 }
    val max = 5000
    val px = IntArray(max); val py = IntArray(max); val parent = IntArray(max)
    var n = 0
    fun put(x: Int, y: Int, p: Int) { px[n] = x; py[n] = y; parent[n] = p; owner[y * cols + x] = n; n++ }
    val cx = cols / 2; val cy = lines / 2
    if (radial) put(cx, cy, -1) else for (x in 0 until cols) put(x, lines - 1, -1)
    val seeds = n
    var reach = 1
    var topRow = lines - 1
    val dx = intArrayOf(1, -1, 0, 0)
    val dy = intArrayOf(0, 0, 1, -1)
    while (n < max) {
        if (radial && reach >= cy - 4) break
        if (!radial && topRow <= 8) break
        fun launch(): Pair<Int, Int> = if (radial) {
            val a = rnd.nextDouble() * 2 * PI
            (cx + ((reach + 4) * cos(a)).roundToInt()) to (cy + ((reach + 4) * sin(a)).roundToInt())
        } else rnd.nextInt(cols) to (topRow - 5).coerceAtLeast(1)
        var start = launch()
        var x = start.first
        var y = start.second
        while (true) {
            val d = rnd.nextInt(4)
            x += dx[d]; y += dy[d]
            if (!radial) x = Math.floorMod(x, cols)
            val far = if (radial) {
                val ex = x - cx; val ey = y - cy
                ex * ex + ey * ey > (reach + 20) * (reach + 20) || x < 1 || y < 1 || x >= cols - 1 || y >= lines - 1
            } else y < topRow - 25 || y < 1
            if (far) {
                start = launch()
                x = start.first; y = start.second
                continue
            }
            var stuck = -1
            for (e in 0 until 4) {
                val nx = if (radial) x + dx[e] else Math.floorMod(x + dx[e], cols)
                val ny = y + dy[e]
                if (nx < 0 || ny < 0 || nx >= cols || ny >= lines) continue
                val o = owner[ny * cols + nx]
                if (o >= 0) { stuck = o; break }
            }
            if (stuck >= 0 && owner[y * cols + x] < 0) {
                put(x, y, stuck)
                if (radial) reach = max(reach, hypot((x - cx).toFloat(), (y - cy).toFloat()).toInt() + 1)
                else topRow = min(topRow, y)
                break
            }
        }
    }
    DlaCluster(cols, lines, px, py, parent, n, seeds)
}

/** Agrégation limitée par diffusion : chaque particule reliée à celle où elle s'est collée, coloriée selon son arrivée. */
private fun Pencil.dla(v: Int) {
    val c = dlaGrow(v)
    val radial = v % 2 == 0
    // Le germe central : on cadre l'amas lui-même ; le givre : toute la grille.
    var x0 = 0; var x1 = c.cols - 1; var y0 = 0; var y1 = c.lines - 1
    if (radial) {
        x0 = c.x.take(c.n).min(); x1 = c.x.take(c.n).max(); y0 = c.y.take(c.n).min(); y1 = c.y.take(c.n).max()
    }
    val cell = min((FW - 20f) / (x1 - x0 + 1), (FH - 16f) / (y1 - y0 + 1))
    val ox = (FW - (x1 - x0 + 1) * cell) / 2 - x0 * cell; val oy = (FH - (y1 - y0 + 1) * cell) / 2 - y0 * cell
    fun at(i: Int) = pt(ox + (c.x[i] + 0.5f) * cell, oy + (c.y[i] + 0.5f) * cell)
    val pal = if (radial) Crayons.rainbow.rot((v / 2 * 3) % 8).take(7) else
        listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise, Crayons.violet, Crayons.rose)
    // Chaque trait est d'autant plus épais que la branche porte de particules, comme un corail.
    val load = IntArray(c.n) { 1 }
    for (i in c.n - 1 downTo c.seeds) { val p = c.parent[i]; if (p >= 0) load[p] += load[i] }
    val buckets = 12
    val widths = 5
    val groups = List(buckets * widths) { ArrayList<Pair<Offset, Offset>>() }
    for (i in c.seeds until c.n) {
        val p = c.parent[i]
        var a = at(p); val b = at(i)
        // Au bord du bas, la grille se referme sur elle-même : pas de trait d'un bord à l'autre.
        if (abs(a.x - b.x) > 2 * cell) a = b
        val g = min(buckets - 1, (i - c.seeds) * buckets / (c.n - c.seeds))
        val w = min(widths - 1, (ln(load[i].toFloat()) / 1.4f).toInt())
        groups[g * widths + w] += a to b
    }
    groups.forEachIndexed { k, segs ->
        val g = k / widths; val w = k % widths
        strokes(segs, ramp(pal, g / (buckets - 1f)), (cell * 0.75f + w * cell * 0.5f), 0.9f)
    }
    if (radial) {
        val o = at(0)
        fill(circle(o.x, o.y, 3.5f, 14), Crayons.jaune, hatch = false, wash = 0.9f, lw = 1f)
    } else {
        line(listOf(pt(ox, oy + (c.lines - 0.5f) * cell), pt(ox + c.cols * cell, oy + (c.lines - 0.5f) * cell)), Graphite, 1.6f, 0.8f)
    }
}

/** Un lancer d'aiguilles : leurs extrémités, croisement ou non, rapport ℓ/d, et la largeur des lames. */
private class BuffonDrop(val needles: List<Triple<Offset, Offset, Boolean>>, val crossing: Int, val ratio: Float, val d: Float, val top: Float, val planks: Int)

private fun buffonDrop(v: Int): BuffonDrop {
    val rnd = Random(77 + v * 13)
    val d = 38f
    val planks = 6
    val top = (FH - planks * d) / 2
    val ratio = if (v % 2 == 0) 1f else 0.75f
    val l = d * ratio
    val needles = List(100) {
        // Le centre tombe sur cinq lames entières (sa hauteur modulo d reste uniforme) : l'aiguille ne déborde pas du parquet.
        val c = pt(8f + l / 2 + rnd.nextFloat() * (FW - 16f - l), top + d / 2 + rnd.nextFloat() * (planks - 1) * d)
        val a = rnd.nextFloat() * PI.toFloat()
        val u = dir(a) * (l / 2)
        val p = c - u; val q = c + u
        Triple(p, q, floor((p.y - top) / d) != floor((q.y - top) / d))
    }
    return BuffonDrop(needles, needles.count { it.third }, ratio, d, top, planks)
}

/** Aiguilles de Buffon : un parquet de six lames, et cent aiguilles, en rouge celles qui croisent une rainure. */
private fun Pencil.buffon(v: Int) {
    val drop = buffonDrop(v)
    val rnd = Random(5 + v)
    val woods = listOf(Crayons.brun, Crayons.orange, Crayons.jaune)
    for (i in 0 until drop.planks) {
        val y = drop.top + i * drop.d
        // Deux planches par lame, le joint décalé d'une lame à l'autre.
        val cut = 60f + rnd.nextFloat() * 248f
        for ((a, b) in listOf(8f to cut, cut to FW - 8f)) {
            val wood = woods[(i + (if (a < 10f) 0 else 1) + v) % woods.size]
            fill(listOf(pt(a, y), pt(b, y), pt(b, y + drop.d), pt(a, y + drop.d)), wood, hatch = false, wash = 0.13f, lw = 0.8f, strokeAlpha = 0.5f)
            // Le fil du bois.
            repeat(3) {
                val gy = y + 6f + rnd.nextFloat() * (drop.d - 12f)
                val pts = (0..8).map { s -> pt(a + 3f + (b - a - 6f) * s / 8f, gy + sin(s * 0.9f + it) * 1.4f) }
                line(pts, Crayons.brun, 0.5f, 0.3f)
            }
        }
    }
    for (i in 0..drop.planks) line(listOf(pt(8f, drop.top + i * drop.d), pt(FW - 8f, drop.top + i * drop.d)), Graphite, 1.3f, 0.85f)
    for ((p, q, cross) in drop.needles) {
        val color = if (cross) Crayons.rouge else Crayons.marine
        line(listOf(p, q), color, 1.6f, 0.95f)
        dot(p, 1.6f, color, 0.9f)
    }
}

private const val PERC_COLS = 44
private const val PERC_ROWS = 30

/** Une percolation tirée : probabilité, amas de chaque case (−1 : fermée), rangs des amas par taille, et traversée. */
private class PercolationGrid(val p: Double, val cluster: IntArray, val rank: IntArray, val biggest: Int, val spans: Boolean)

private val percolationCache = HashMap<Int, PercolationGrid>()

private fun percolationGrid(v: Int): PercolationGrid = percolationCache.getOrPut(v) {
    val p = doubleArrayOf(0.6, 0.55, 0.65)[v % 3]
    val rnd = Random(500 + v * 7)
    val n = PERC_COLS * PERC_ROWS
    val open = BooleanArray(n) { rnd.nextDouble() < p }
    val cluster = IntArray(n) { -1 }
    val sizes = ArrayList<Int>()
    for (s in 0 until n) {
        if (!open[s] || cluster[s] >= 0) continue
        val id = sizes.size
        var size = 0
        val stack = ArrayDeque<Int>()
        stack.addLast(s); cluster[s] = id
        while (stack.isNotEmpty()) {
            val c = stack.removeLast()
            size++
            val x = c % PERC_COLS; val y = c / PERC_COLS
            for ((nx, ny) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
                if (nx < 0 || ny < 0 || nx >= PERC_COLS || ny >= PERC_ROWS) continue
                val o = ny * PERC_COLS + nx
                if (open[o] && cluster[o] < 0) { cluster[o] = id; stack.addLast(o) }
            }
        }
        sizes += size
    }
    val order = sizes.indices.sortedByDescending { sizes[it] }
    val rank = IntArray(sizes.size)
    order.forEachIndexed { r, id -> rank[id] = r }
    val topIds = (0 until PERC_COLS).map { cluster[it] }.filter { it >= 0 }.toSet()
    val spans = (0 until PERC_COLS).any { val id = cluster[(PERC_ROWS - 1) * PERC_COLS + it]; id >= 0 && id in topIds }
    PercolationGrid(p, cluster, rank, if (sizes.isEmpty()) 0 else sizes[order[0]], spans)
}

/** Percolation : les cases ouvertes en perles, reliées à leurs voisines ; les sept plus gros amas en couleurs. */
private fun Pencil.percolation(v: Int) {
    val g = percolationGrid(v)
    val step = min((FW - 20f) / PERC_COLS, (FH - 16f) / PERC_ROWS)
    val ox = (FW - (PERC_COLS - 1) * step) / 2; val oy = (FH - (PERC_ROWS - 1) * step) / 2
    fun at(i: Int) = pt(ox + (i % PERC_COLS) * step, oy + (i / PERC_COLS) * step)
    val pal = listOf(Crayons.rouge, Crayons.bleu, Crayons.vert, Crayons.orange, Crayons.violet, Crayons.turquoise, Crayons.rose, Crayons.jaune).rot(v % 8)
    fun colorOf(id: Int) = if (g.rank[id] < 7) pal[g.rank[id]] else Graphite
    val bonds = HashMap<Int, ArrayList<Pair<Offset, Offset>>>()
    for (i in g.cluster.indices) {
        val id = g.cluster[i]
        if (id < 0) continue
        val x = i % PERC_COLS
        if (x + 1 < PERC_COLS && g.cluster[i + 1] == id) bonds.getOrPut(id) { ArrayList() } += at(i) to at(i + 1)
        if (i + PERC_COLS < g.cluster.size && g.cluster[i + PERC_COLS] == id) bonds.getOrPut(id) { ArrayList() } += at(i) to at(i + PERC_COLS)
    }
    for ((id, segs) in bonds) strokes(segs, colorOf(id), if (g.rank[id] < 7) 2.2f else 1.2f, if (g.rank[id] < 7) 0.75f else 0.3f)
    for (i in g.cluster.indices) {
        val id = g.cluster[i]
        val o = at(i)
        when {
            id < 0 -> dot(o, 0.7f, Graphite, 0.25f)
            g.rank[id] < 7 -> fill(circle(o.x, o.y, 2.6f, 10), colorOf(id), hatch = false, wash = 0.75f, lw = 0.5f)
            else -> dot(o, 1.6f, Graphite, 0.35f)
        }
    }
}

/** Somme de dés : pour 4 nombres de dés, la loi exacte de la somme en barres, la courbe de Gauss et les dés. */
private fun Pencil.diceSum(v: Int) {
    val counts = Fractal2.diceCounts(v)
    val rnd = Random(90 + v)
    val pw = 172f; val ph = 113f
    val warm = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet)
    val pal = if (v / 2 % 2 == 0) warm else warm.reversed()
    for ((slot, n) in counts.withIndex()) {
        val x0 = 10f + (slot % 2) * (pw + 4f)
        val y0 = 9f + (slot / 2) * (ph + 6f)
        // La loi exacte : nombre de façons d'obtenir chaque total, dé par dé.
        var ways = longArrayOf(1)
        repeat(n) {
            val next = LongArray(ways.size + 6)
            for (i in ways.indices) for (f in 1..6) next[i + f] += ways[i]
            ways = next
        }
        val lo = n; val hi = 6 * n
        val maxWays = (lo..hi).maxOf { ways[it] }.toFloat()
        val total = 6.0.pow(n)
        val left = x0 + 4f; val right = x0 + pw - 4f
        val floor = y0 + ph - 4f
        val tall = ph - 34f
        val bw = (right - left) / (hi - lo + 1)
        val mean = 3.5f * n
        val sd = sqrt(35f * n / 12f)
        for (s in lo..hi) {
            val hgt = ways[s] / maxWays * tall
            val a = left + (s - lo) * bw
            val color = ramp(pal, (abs(s - mean) / (2.8f * sd)).coerceIn(0f, 1f))
            fill(listOf(pt(a + 0.6f, floor - hgt), pt(a + bw - 0.6f, floor - hgt), pt(a + bw - 0.6f, floor), pt(a + 0.6f, floor)), color,
                hatch = bw > 6f, gap = 2.6f, wash = if (bw > 6f) 0.35f else 0.6f, lw = if (bw > 6f) 0.9f else 0.5f)
        }
        line(listOf(pt(left - 2f, floor), pt(right + 2f, floor)), Graphite, 1f, 0.75f)
        if (n > 1) {
            // La courbe de Gauss de même moyenne et même variance, à la même échelle que les barres.
            val peak = maxWays / total
            val curve = (0..80).map {
                val s = lo - 0.5f + (hi - lo + 1) * it / 80f
                val dens = exp(-(s - mean) * (s - mean) / (2 * sd * sd)) / (sd * sqrt(2 * PI.toFloat()))
                pt(left + (s - lo + 0.5f) * bw, floor - (dens / peak).toFloat() * tall)
            }
            line(curve, Graphite, 1.2f, 0.8f)
        }
        // Les dés lancés, en haut à gauche du cadre.
        val die = 9f
        for (i in 0 until n) {
            val dx = x0 + 5f + (i % 4) * (die + 3f)
            val dy = y0 + 4f + (i / 4) * (die + 3f)
            fill(listOf(pt(dx, dy), pt(dx + die, dy), pt(dx + die, dy + die), pt(dx, dy + die)), PaperColors.Paper, hatch = false, wash = 1f, lw = 0.8f)
            val face = 1 + rnd.nextInt(6)
            val pips = when (face) {
                1 -> listOf(0 to 0); 2 -> listOf(-1 to -1, 1 to 1); 3 -> listOf(-1 to -1, 0 to 0, 1 to 1)
                4 -> listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1); 5 -> listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1)
                else -> listOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1)
            }
            for ((a, b) in pips) dot(pt(dx + die / 2 + a * 2.4f, dy + die / 2 + b * 2.4f), 0.8f, Graphite, 0.9f)
        }
    }
}
