package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import fr.perso.nimbus.ui.theme.PaperColors
import java.util.Locale
import java.util.PriorityQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.random.Random

// Figures des thèmes « Informatique » et « Fonctions » : fourmi de Langton, jeu de la vie, automates
// cellulaires, réseau de tri, appels récursifs, code de Gray, quadtree, Bresenham, plus court chemin,
// additionneur, arbre rouge-noir, tramage ; coloration de domaine, Weierstrass, sin(1/x), zêta, Gamma,
// Taylor, sinus cardinal et champ de pentes.

internal val codeFigures: List<FigureSpec> = listOf(
    // ------------------------------------------------------------ Informatique
    FigureSpec("langton_ant", "Fourmi de Langton", "Informatique", { _ ->
        "Une fourmi sur un quadrillage : sur une case blanche, elle tourne à droite ; sur une noire, à gauche ; puis elle change la couleur de la case et avance. " +
            "Après environ 10 000 pas de désordre, elle se met soudain à bâtir une « autoroute » : un motif de 104 pas qui se répète en filant en diagonale. " +
            "Ici 12 000 pas ; chaque case est coloriée selon son dernier passage. Chris Langton, 1986."
    }) { langtonAnt(it) },
    FigureSpec("gosper_gun", "Canon à planeurs de Gosper", "Informatique", { v ->
        "Le jeu de la vie de John Conway (1970) : une case vivante survit si elle a 2 ou 3 voisines vivantes, une case vide naît si elle en a exactement 3. " +
            "Le canon de Bill Gosper, trouvé la même année, tire un planeur toutes les 30 générations ; ici ${LifeParams.gens(v)} générations superposées, " +
            "chaque case coloriée selon la première où elle a vécu. Les planeurs filent en diagonale, d’une case toutes les 4 générations."
    }) { gosperGun(it) },
    FigureSpec("elementary_ca", "Automate cellulaire élémentaire", "Informatique", { v ->
        val rule = CaParams.rule(v)
        val bits = rule.toString(2).padStart(8, '0')
        "Chaque ligne naît de la précédente : une case dépend des trois cases au-dessus d’elle, selon une table de 8 cas (en haut à gauche). " +
            "La règle $rule, c’est $bits en binaire, la liste des 8 réponses. Les couleurs disent quel cas a fait naître la case. " +
            when (rule) {
                30 -> "Partie d’une seule case, la règle 30 a l’air de tirer au hasard : Stephen Wolfram s’en est servi comme générateur aléatoire."
                110 -> "Partie d’une ligne tirée au hasard, la règle 110 fait naître des motifs qui voyagent et se percutent ; Matthew Cook a démontré en 2004 qu’elle peut tout calculer."
                90 -> "La règle 90 fait le ou exclusif des deux voisines : on obtient le triangle de Pascal modulo 2, celui de Sierpiński."
                else -> "La règle 150 fait le ou exclusif des trois cases : la ligne n donne les coefficients de (1 + x + x²)ⁿ modulo 2."
            }
    }) { elementaryCa(it) },
    FigureSpec("sorting_network", "Réseau de tri de Batcher", "Informatique", { v ->
        val (name, count) = if (v % 2 == 0) "tri pair-impair par fusion" to 63 else "tri bitonique" to 80
        "Seize nombres, un par fil, entrent à gauche dans le désordre. Chaque trait vertical est un comparateur : il échange ses deux nombres s’ils sont mal rangés. " +
            "Ce réseau de Kenneth Batcher (1968), le $name, compte $count comparateurs en 10 étapes, chacune faite d’un coup en parallèle. " +
            "Quelle que soit l’entrée, les couleurs ressortent dans l’ordre. Il suffit de le vérifier sur les 2¹⁶ entrées de 0 et de 1."
    }) { sortingNetwork(it) },
    FigureSpec("fib_calls", "Appels récursifs de Fibonacci", "Informatique", { v ->
        val n = FibParams.n(v)
        "L’arbre des appels de fib($n), avec la fonction Python def fib(n): return n if n < 2 else fib(n-1) + fib(n-2). " +
            "Chaque disque est un appel, colorié selon son argument : ${2 * fibo(n + 1) - 1} appels, dont ${fibo(n - 1)} pour fib(2). " +
            "Leur nombre croît comme φⁿ, avec φ ≈ 1,618. Avec un cache (@cache), il n’en reste que ${2 * n - 1} : la branche de gauche (cerclée) et ses voisins directs."
    }) { fibCalls(it) },
    FigureSpec("gray_code", "Disques du code de Gray", "Informatique", { v ->
        val b = GrayParams.bits(v)
        "Deux disques d’encodeur rotatif à $b pistes, lus du centre vers le bord : à gauche, le secteur s’écrit en binaire ; à droite, en code de Gray (Frank Gray, 1953). " +
            "En binaire, passer de 0${"1".repeat(b - 1)} à 1${"0".repeat(b - 1)} change toutes les pistes à la fois (trait rouge) : un capteur un peu décalé lirait n’importe quoi. " +
            "En Gray, un seul bit change à chaque pas. En Python : g = n ^ (n >> 1)."
    }) { grayCode(it) },
    FigureSpec("quadtree", "Arbre quaternaire (quadtree)", "Informatique", { v ->
        val leaves = quadtrees[v].leaves.size
        "Un quadtree découpe un carré en quatre tant qu’il est à cheval sur le bord de la forme ; les cases tout entières dedans ou dehors restent entières. " +
            "Avec 6 carrés de départ et 5 découpages au plus, $leaves cases suffisent là où il faudrait 6 144 pixels. " +
            "Leur nombre suit la longueur du bord, pas l’aire : on s’en sert pour compresser des images, ou pour trouver vite les voisins d’un point."
    }) { quadtree(it) },
    FigureSpec("bresenham", "Tracé de Bresenham", "Informatique", { _ ->
        "Pour tracer un segment en pixels, l’algorithme de Jack Bresenham (1962, pour une table traçante d’IBM) avance d’un pixel à la fois et choisit le plus proche de la droite " +
            "grâce à une erreur entière : que des additions, aucune division. Les arcs viennent de sa variante pour les cercles, l’algorithme du point milieu, " +
            "qui calcule un huitième du cercle et en déduit le reste par symétrie. Le trait fin montre la ligne idéale."
    }) { bresenham(it) },
    FigureSpec("shortest_path", "Plus court chemin", "Informatique", { v ->
        val run = pathRuns[v]
        val text = if (run.astar) {
            "A* (Hart, Nilsson et Raphael, 1968) explore d’abord les cases dont la distance parcourue plus la distance restante estimée, |dx| + |dy|, est la plus petite : " +
                "il fonce vers l’arrivée et n’ouvre ici que ${run.explored} cases."
        } else {
            "L’algorithme de Dijkstra (1956) explore les cases par distance croissante au départ, en tache d’huile, et n’atteint l’arrivée qu’après avoir ouvert ${run.explored} cases."
        }
        "Trouver le plus court chemin entre deux points d’un labyrinthe, en pas d’une case. $text " +
            "Les cases explorées sont coloriées selon leur distance au départ ; le chemin trouvé fait ${run.length} pas."
    }) { shortestPath(it) },
    FigureSpec("full_adder", "Additionneur complet", "Informatique", { v ->
        val (a, b, c) = AdderParams.bits(v)
        val sum = a + b + c
        "Un additionneur complet ajoute deux bits a et b à la retenue r du rang précédent. La somme s = a ⊕ b ⊕ r sort de deux portes OU exclusif (violet), " +
            "la retenue (a ∧ b) ∨ ((a ⊕ b) ∧ r) de deux portes ET (vert) et d’une porte OU (orange). Ici $a + $b + $c = $sum, soit ${sum / 2}${sum % 2} en binaire : " +
            "s = ${sum % 2}, retenue ${sum / 2}. Les fils à 1 sont rouges. Soixante-quatre de ces circuits en chaîne additionnent deux nombres de 64 bits."
    }) { fullAdder(it) },
    FigureSpec("red_black", "Arbre rouge-noir", "Informatique", { v ->
        val t = redBlackTrees[v]
        "Un arbre rouge-noir de ${t.size} clés insérées dans le désordre, rangées de gauche à droite. La racine est noire, un nœud rouge n’a pas d’enfant rouge, " +
            "et tout chemin de la racine à une feuille vide (petit carré) croise autant de nœuds noirs, ici ${t.blackHeight}. " +
            "La hauteur, ici ${t.levels} niveaux, reste donc sous 2 log₂(n + 1) ≈ ${"%.1f".format(Locale.ROOT, 2 * log2(t.size + 1.0)).replace('.', ',')}. " +
            "Un nœud rouge et son parent (gélule) forment un nœud à deux clés d’un arbre 2-3. Rudolf Bayer les a inventés en 1972."
    }) { redBlack(it) },
    FigureSpec("dithering", "Tramage de Floyd-Steinberg", "Informatique", { _ ->
        "Un dégradé peint avec seulement 5 crayons, en 88 × 58 pixels. À gauche, chaque pixel prend la couleur permise la plus proche : des bandes nettes apparaissent. " +
            "À droite, le tramage de Robert Floyd et Louis Steinberg (1976) reporte l’erreur commise sur les voisins pas encore traités, 7/16 à droite et 3/16, 5/16, 1/16 en dessous : " +
            "de loin, le mélange des points rend les nuances."
    }) { dithering(it) },

    // ------------------------------------------------------------ Fonctions
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
    FigureSpec("sin_inverse", "Sinus de 1/x", "Fonctions", { v ->
        when (v % 3) {
            0 -> "Le graphe de sin(1/x) oscille de plus en plus vite près de 0 : une arche entre 1/((n + 1)π) et 1/(nπ), une infinité en tout. La fonction n’a pas de limite en 0, " +
                "et son graphe s’accumule sur tout le segment vertical de −1 à 1 (la bande hachurée) : c’est la « courbe sinus du topologue », connexe mais pas connexe par arcs."
            1 -> "Le graphe de x sin(1/x), pris entre les droites y = x et y = −x (pointillés), oscille une infinité de fois près de 0. Elle tend vers 0 : en posant f(0) = 0, elle est continue, " +
                "mais pas dérivable en 0, car (f(x) − f(0)) / x = sin(1/x) n’a pas de limite."
            else -> "Le graphe de x² sin(1/x), pris entre les paraboles y = ±x² (pointillés). Cette fois, elle est dérivable partout, avec f′(0) = 0, " +
                "mais sa dérivée 2x sin(1/x) − cos(1/x) oscille sans fin près de 0 : une fonction dérivable dont la dérivée n’est pas continue."
        }
    }) { sinInverse(it) },
    FigureSpec("zeta_spiral", "Fonction zêta de Riemann", "Fonctions", { v ->
        val t = ZetaParams.tMax(v)
        val zeros = ZETA_ZEROS.count { it < t }
        "La courbe suit ζ(1/2 + it) dans le plan complexe quand t va de 0 à ${t.roundToInt()} : elle part de ζ(1/2) ≈ −1,46 et repasse par 0 à chaque zéro de la droite critique, " +
            "ici $zeros fois (t ≈ 14,13 ; 21,02 ; 25,01…), chaque boucle d’une couleur. L’hypothèse de Riemann (1859) affirme que tous les zéros non triviaux sont sur cette droite ; " +
            "des milliers de milliards ont été vérifiés."
    }) { zetaSpiral(it) },
    FigureSpec("gamma_function", "Fonction Gamma", "Fonctions", { v ->
        "La fonction Gamma d’Euler (1729), intégrale de t^(x−1) e^(−t), prolonge la factorielle : Γ(n) = (n − 1)! (points jaunes : 1, 1, 2, 6) et Γ(x + 1) = x Γ(x). " +
            "Elle ne s’annule jamais, mais a un pôle en 0 et en chaque entier négatif (pointillés), où ses branches changent de signe. Γ(1/2) = √π ≈ 1,772. " +
            if (v % 2 == 1) "En bleu, 1/Γ : définie partout, elle s’annule aux pôles de Γ." else "Son minimum positif vaut environ 0,886, en x ≈ 1,462."
    }) { gammaFunction(it) },
    FigureSpec("taylor_series", "Polynômes de Taylor", "Fonctions", { v ->
        if (v % 2 == 0) {
            "Les polynômes de Taylor de sin en 0 : x, puis x − x³/3!, puis x − x³/3! + x⁵/5!… jusqu’au degré 17, chacun d’une couleur. " +
                "Chacun suit la sinusoïde (en gris) un peu plus loin que le précédent, puis s’en échappe. La série converge vers sin x pour tout x, " +
                "mais d’autant plus lentement que x est loin de 0 : l’erreur est au plus |x|ⁿ⁺¹/(n + 1)!. Brook Taylor, 1715."
        } else {
            "Les polynômes de Taylor de cos en 0 : 1, puis 1 − x²/2!, puis 1 − x²/2! + x⁴/4!… jusqu’au degré 16, chacun d’une couleur. " +
                "Chacun suit la sinusoïde (en gris) un peu plus loin que le précédent, puis s’en échappe. La série converge vers cos x pour tout x, " +
                "mais d’autant plus lentement que x est loin de 0 : l’erreur est au plus |x|ⁿ⁺¹/(n + 1)!. Brook Taylor, 1715."
        }
    }) { taylorSeries(it) },
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

/** Le contour fin d'une case, d'un seul trait. */
private fun Pencil.cellFrame(x: Float, y: Float, w: Float, h: Float, alpha: Float, lw: Float = 0.45f) = scope.drawRect(
    Graphite,
    topLeft = px(pt(x, y)),
    size = Size(w * k, h * k),
    alpha = alpha,
    style = Stroke(lw * k),
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

/** Une polyligne en pointillés : un tronçon de [on] points sur deux. */
private fun Pencil.dashedPath(pts: List<Offset>, color: Color, lw: Float, alpha: Float, on: Int = 4) {
    var i = 0
    while (i < pts.size - 1) {
        line(pts.subList(i, min(pts.size, i + on + 1)), color, lw, alpha)
        i += 2 * on
    }
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

// ------------------------------------------------------------ Fourmi de Langton

private const val ANT_STEPS = 12000

/** Les cases visitées par la fourmi : (x, y, noire ?, dernier passage), le cadre et la fourmi à la fin. */
private class AntRun(val cells: List<IntArray>, val x0: Int, val y0: Int, val x1: Int, val y1: Int, val ant: IntArray)

private val antRuns = PerVariant { _ ->
    val n = 260
    val black = BooleanArray(n * n)
    val last = IntArray(n * n) { -1 }
    val dx = intArrayOf(0, 1, 0, -1)
    val dy = intArrayOf(-1, 0, 1, 0)
    var x = n / 2; var y = n / 2; var d = 0
    for (step in 0 until ANT_STEPS) {
        val i = y * n + x
        d = if (black[i]) (d + 3) % 4 else (d + 1) % 4
        black[i] = !black[i]
        last[i] = step
        x += dx[d]; y += dy[d]
    }
    val cells = mutableListOf<IntArray>()
    var x0 = x; var y0 = y; var x1 = x; var y1 = y
    for (i in 0 until n * n) if (last[i] >= 0) {
        val cx = i % n; val cy = i / n
        cells += intArrayOf(cx, cy, if (black[i]) 1 else 0, last[i])
        x0 = min(x0, cx); y0 = min(y0, cy); x1 = max(x1, cx); y1 = max(y1, cy)
    }
    AntRun(cells, x0, y0, x1, y1, intArrayOf(x, y))
}

/** Fourmi de Langton : les cases noires coloriées selon le moment de leur dernier passage, l'autoroute au bout. */
private fun Pencil.langtonAnt(v: Int) {
    val run = antRuns[v]
    val w = run.x1 - run.x0 + 1
    val h = run.y1 - run.y0 + 1
    val c = min((FW - 24) / w, (FH - 20) / h)
    val ox = (FW - w * c) / 2
    val oy = (FH - h * c) / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8).take(6)
    // Selon la variante, la figure est retournée de gauche à droite ou de haut en bas.
    fun cx(x: Int) = ox + (if (v % 2 == 1) run.x1 - x else x - run.x0) * c
    fun cy(y: Int) = oy + (if (v % 4 >= 2) run.y1 - y else y - run.y0) * c
    for ((x, y, b, t) in run.cells) {
        val color = ramp(pal, t / ANT_STEPS.toFloat())
        pixel(cx(x), cy(y), c, color, if (b == 1) 0.82f else 0.14f)
    }
    val a = pt(cx(run.ant[0]) + c / 2, cy(run.ant[1]) + c / 2)
    fill(circle(a.x, a.y, 3.4f, 14), Graphite, hatch = false, wash = 0.85f, lw = 0.8f)
    dot(a, 1.1f, Crayons.jaune)
}

// ------------------------------------------------------------ Canon à planeurs de Gosper

private object LifeParams {
    fun gens(v: Int) = if (v % 4 < 2) 150 else 140
}

private val GOSPER_GUN = listOf(
    "........................O...........",
    "......................O.O...........",
    "............OO......OO............OO",
    "...........O...O....OO............OO",
    "OO........O.....O...OO..............",
    "OO........O...O.OO....O.O...........",
    "..........O.....O.......O...........",
    "...........O...O....................",
    "............OO......................",
)

private const val LIFE_COLS = 68
private const val LIFE_ROWS = 46

/** Pour chaque case de la fenêtre, la première génération où elle a vécu (−1 : jamais), et les cases vivantes à la fin. */
private val lifeRuns = PerVariant { v ->
    val gens = LifeParams.gens(v)
    // Un monde plus grand que la fenêtre : les planeurs sortis ne reviennent pas la troubler.
    val w = 140; val h = 120
    var cur = BooleanArray(w * h)
    var next = BooleanArray(w * h)
    GOSPER_GUN.forEachIndexed { r, row -> row.forEachIndexed { c, ch -> if (ch == 'O') cur[(r + 2) * w + c + 2] = true } }
    val first = IntArray(LIFE_COLS * LIFE_ROWS) { -1 }
    fun mark(g: Int) {
        for (y in 0 until LIFE_ROWS) for (x in 0 until LIFE_COLS) if (cur[y * w + x] && first[y * LIFE_COLS + x] < 0) first[y * LIFE_COLS + x] = g
    }
    mark(0)
    for (g in 1..gens) {
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            var n = 0
            if (cur[i - w - 1]) n++; if (cur[i - w]) n++; if (cur[i - w + 1]) n++
            if (cur[i - 1]) n++; if (cur[i + 1]) n++
            if (cur[i + w - 1]) n++; if (cur[i + w]) n++; if (cur[i + w + 1]) n++
            next[i] = n == 3 || (n == 2 && cur[i])
        }
        val t = cur; cur = next; next = t
        mark(g)
    }
    first to BooleanArray(LIFE_COLS * LIFE_ROWS) { cur[it / LIFE_COLS * w + it % LIFE_COLS] }
}

/** Le canon de Gosper et sa traînée de planeurs, les générations superposées en dégradé. */
private fun Pencil.gosperGun(v: Int) {
    val (first, alive) = lifeRuns[v]
    val gens = LifeParams.gens(v)
    val c = min((FW - 16) / LIFE_COLS, (FH - 16) / LIFE_ROWS)
    val ox = (FW - LIFE_COLS * c) / 2
    val oy = (FH - LIFE_ROWS * c) / 2
    val pal = Crayons.rainbow.rot((v * 3 + 5) % 8).take(5)
    val mirror = v % 2 == 1
    fun cx(x: Int) = ox + (if (mirror) LIFE_COLS - 1 - x else x) * c
    // Le quadrillage, très pâle.
    for (x in 0..LIFE_COLS) stroke(pt(ox + x * c, oy), pt(ox + x * c, oy + LIFE_ROWS * c), Graphite, 0.3f, 0.07f)
    for (y in 0..LIFE_ROWS) stroke(pt(ox, oy + y * c), pt(ox + LIFE_COLS * c, oy + y * c), Graphite, 0.3f, 0.07f)
    for (y in 0 until LIFE_ROWS) for (x in 0 until LIFE_COLS) {
        val g = first[y * LIFE_COLS + x]
        if (g < 0) continue
        pixel(cx(x), oy + y * c, c, ramp(pal, g / gens.toFloat()), 0.5f)
    }
    // Les cases vivantes à la dernière génération.
    for (y in 0 until LIFE_ROWS) for (x in 0 until LIFE_COLS) {
        if (!alive[y * LIFE_COLS + x]) continue
        fill(rect(cx(x) + 0.3f, oy + y * c + 0.3f, c - 0.6f), Graphite, hatch = false, wash = 0.8f, lw = 0.5f)
    }
}

// ------------------------------------------------------------ Automates élémentaires

private object CaParams {
    fun rule(v: Int) = intArrayOf(30, 110, 90, 150)[v % 4]
}

/** Automate cellulaire élémentaire parti d'une seule case, chaque case coloriée selon le cas de la règle qui l'a fait naître. */
private fun Pencil.elementaryCa(v: Int) {
    val rule = CaParams.rule(v)
    val cell = 3.2f
    val cols = 110
    val rows = 74
    val pad = rows + 2
    val width = cols + 2 * pad
    val x0 = (FW - cols * cell) / 2
    val y0 = (FH - rows * cell) / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    var row = IntArray(width)
    // Une seule case au milieu ; pour la règle 110, une ligne au hasard, refermée en anneau.
    val random = rule == 110
    if (random) {
        val rnd = Random(v * 13 + 1)
        for (c in 0 until cols) row[pad + c] = if (rnd.nextFloat() < 0.5f) 1 else 0
    } else {
        row[pad + cols / 2] = 1
    }
    var cases = IntArray(width) { 2 }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            // La table de la règle, en haut à gauche, garde sa place.
            if (row[pad + c] == 0 || (x0 + c * cell < 98f && y0 + r * cell < 48f)) continue
            pixel(x0 + c * cell, y0 + r * cell, cell, pal[cases[pad + c]], 0.8f)
        }
        val next = IntArray(width)
        val nextCases = IntArray(width)
        for (i in 1 until width - 1) {
            fun at(j: Int) = if (random) row[pad + (j - pad).mod(cols)] else row[j]
            val idx = 4 * at(i - 1) + 2 * at(i) + at(i + 1)
            next[i] = (rule shr idx) and 1
            nextCases[i] = idx
        }
        row = next
        cases = nextCases
    }
    // La règle : les 8 cas, de 111 à 000, et la case qu'ils donnent.
    val s = 4.6f
    for (i in 0 until 8) {
        val p = 7 - i
        val bx = 12f + (i % 4) * 21f
        val by = 10f + (i / 4) * 17f
        for (b in 0 until 3) {
            val on = (p shr (2 - b)) and 1 == 1
            val box = rect(bx + b * s, by, s)
            if (on) fill(box, Graphite, hatch = false, wash = 0.55f, lw = 0.5f) else outline(box, lw = 0.5f)
        }
        val out = rect(bx + s, by + s + 1.4f, s)
        if ((rule shr p) and 1 == 1) fill(out, pal[p], hatch = false, wash = 0.9f, lw = 0.5f) else outline(out, lw = 0.5f)
    }
}

// ------------------------------------------------------------ Réseau de tri

/** Réseau de tri pair-impair par fusion de Batcher pour [n] fils (une puissance de 2). */
private fun oddEvenMergeNetwork(n: Int): List<IntArray> {
    val out = mutableListOf<IntArray>()
    var p = 1
    while (p < n) {
        var k = p
        while (k >= 1) {
            var j = k % p
            while (j <= n - 1 - k) {
                for (i in 0..min(k - 1, n - j - k - 1)) {
                    if ((i + j) / (p * 2) == (i + j + k) / (p * 2)) out += intArrayOf(i + j, i + j + k)
                }
                j += 2 * k
            }
            k /= 2
        }
        p *= 2
    }
    return out
}

/** Réseau de tri bitonique, dans sa forme où tous les comparateurs rangent dans le même sens. */
private fun bitonicNetwork(n: Int): List<IntArray> {
    val out = mutableListOf<IntArray>()
    var k = 2
    while (k <= n) {
        for (b in 0 until n step k) for (i in 0 until k / 2) out += intArrayOf(b + i, b + k - 1 - i)
        var j = k / 4
        while (j >= 1) {
            for (i in 0 until n) { val l = i xor j; if (l > i) out += intArrayOf(i, l) }
            j /= 2
        }
        k *= 2
    }
    return out
}

/** Réseau de tri : 16 fils, les comparateurs, et chaque nombre qui suit son chemin en couleur. */
private fun Pencil.sortingNetwork(v: Int) {
    val n = 16
    val comps = if (v % 2 == 0) oddEvenMergeNetwork(n) else bitonicNetwork(n)
    // Colonnes : un comparateur se place juste après tout ce qui occupe déjà les fils qu'il enjambe.
    val occupied = IntArray(n) { -1 }
    val column = comps.map { (a, b) ->
        val c = (a..b).maxOf { occupied[it] } + 1
        for (w in a..b) occupied[w] = c
        c
    }
    val columns = column.max() + 1
    val left = 30f
    val right = FW - 30f
    val dx = (right - left) / columns
    val top = 17f
    val gap = (FH - 2 * top) / (n - 1)
    fun y(w: Int) = top + w * gap
    fun x(c: Int) = left + (c + 0.5f) * dx
    val d = min(dx * 0.42f, 3.6f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun color(value: Int) = ramp(pal, value / (n - 1f))

    val values = (0 until n).shuffled(Random(v * 31 + 5)).toIntArray()
    val paths = Array(n) { mutableListOf<Offset>() }
    for (w in 0 until n) paths[values[w]] += pt(left - 10f, y(w))
    val swapped = BooleanArray(comps.size)
    comps.forEachIndexed { i, (a, b) ->
        if (values[a] > values[b]) {
            val cx = x(column[i])
            paths[values[a]] += pt(cx - d, y(a)); paths[values[a]] += pt(cx + d, y(b))
            paths[values[b]] += pt(cx - d, y(b)); paths[values[b]] += pt(cx + d, y(a))
            val t = values[a]; values[a] = values[b]; values[b] = t
            swapped[i] = true
        }
    }
    for (w in 0 until n) paths[values[w]] += pt(right + 10f, y(w))

    for (w in 0 until n) stroke(pt(left - 10f, y(w)), pt(right + 10f, y(w)), Graphite, 0.5f, 0.25f)
    comps.forEachIndexed { i, (a, b) -> stroke(pt(x(column[i]), y(a)), pt(x(column[i]), y(b)), Graphite, 0.9f, if (swapped[i]) 0.75f else 0.4f) }
    for (value in 0 until n) line(paths[value], color(value), 1.9f, 0.85f)
    comps.forEachIndexed { i, (a, b) ->
        dot(pt(x(column[i]), y(a)), 1.7f, Graphite, 0.85f)
        dot(pt(x(column[i]), y(b)), 1.7f, Graphite, 0.85f)
    }
    for (value in 0 until n) {
        val p0 = paths[value].first()
        val p1 = paths[value].last()
        fill(circle(p0.x - 4f, p0.y, 4f, 14), color(value), hatch = false, wash = 0.85f, lw = 0.7f)
        fill(circle(p1.x + 4f, p1.y, 4f, 14), color(value), hatch = false, wash = 0.85f, lw = 0.7f)
    }
}

// ------------------------------------------------------------ Appels récursifs

private object FibParams {
    fun n(v: Int) = intArrayOf(7, 6, 8)[v % 3]
}

private fun fibo(n: Int): Int = if (n < 2) n else fibo(n - 1) + fibo(n - 2)

/** Un appel fib([n]) : sa profondeur, sa place et ses deux appels. */
private class FibCall(val n: Int, val depth: Int, val x: Float, val kids: List<FibCall>, val first: Boolean)

/** L'arbre des appels de fib(n) : un disque par appel, colorié selon son argument. */
private fun Pencil.fibCalls(v: Int) {
    val n = FibParams.n(v)
    var leaf = 0
    fun build(m: Int, depth: Int, first: Boolean): FibCall {
        if (m < 2) return FibCall(m, depth, (leaf++).toFloat(), emptyList(), first)
        val a = build(m - 1, depth + 1, first)
        val b = build(m - 2, depth + 1, false)
        return FibCall(m, depth, (a.x + b.x) / 2, listOf(a, b), first)
    }
    val root = build(n, 0, true)
    val leaves = leaf
    val sx = (FW - 36f) / (leaves - 1)
    val sy = (FH - 36f) / (n - 1)
    val r = min(7.5f, sx * 0.4f)
    fun at(c: FibCall) = pt(18f + c.x * sx, 18f + c.depth * sy)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun color(m: Int) = pal[m % 8]
    fun edges(c: FibCall) {
        for (kid in c.kids) {
            // Le trait s'arrête au bord des disques.
            val u = (at(kid) - at(c)) / (at(kid) - at(c)).getDistance()
            line(listOf(at(c) + u * r, at(kid) - u * r), Graphite, 0.9f, 0.6f)
            edges(kid)
        }
    }
    edges(root)
    fun nodes(c: FibCall) {
        val p = at(c)
        if (c.first) outline(circle(p.x, p.y, r + 2.4f, 20), lw = 0.9f, color = Crayons.rouge)
        fill(circle(p.x, p.y, r, 18), color(c.n), hatch = r > 6f, gap = 2.2f, wash = 0.55f, lw = 0.8f, angle = -58f + 25f * c.n)
        for (kid in c.kids) nodes(kid)
    }
    nodes(root)
}

// ------------------------------------------------------------ Code de Gray

private object GrayParams {
    fun bits(v: Int) = if (v % 2 == 0) 5 else 6
}

/** Un disque d'encodeur : [bits] pistes (le bit de poids fort au centre), les secteurs où le bit vaut 1 coloriés. */
private fun Pencil.encoderDisc(c: Offset, radius: Float, bits: Int, code: (Int) -> Int, pal: List<Color>) {
    val n = 1 shl bits
    val r0 = radius * 0.27f
    val w = (radius - r0) / bits
    fun ang(s: Float) = rad(-90f + 360f * s / n)
    fill(circle(c.x, c.y, radius + 3f, 120), Crayons.jaune, hatch = false, wash = 0.1f, lw = 1.1f)
    for (b in 0 until bits) {
        val shift = bits - 1 - b
        val ri = r0 + b * w + 0.5f
        val ro = r0 + (b + 1) * w - 0.5f
        fun bit(i: Int) = (code(i % n) shr shift) and 1
        val start = (0 until n).firstOrNull { bit(it) == 0 } ?: 0
        var i = start
        while (i < start + n) {
            if (bit(i) == 0) { i++; continue }
            var e = i
            while (e < start + n && bit(e) == 1) e++
            val steps = max(2, (e - i) * 96 / n)
            val outer = List(steps + 1) { c + dir(ang(i + (e - i) * it / steps.toFloat())) * ro }
            val inner = List(steps + 1) { c + dir(ang(e - (e - i) * it / steps.toFloat())) * ri }
            val big = (e - i) * ro * 2 * PI.toFloat() / n > 10f
            fill(outer + inner, pal[b], hatch = big, gap = 2.8f, angle = -58f + 30f * b, wash = if (big) 0.45f else 0.7f, lw = 0.6f)
            i = e
        }
    }
    for (s in 0 until n) stroke(c + dir(ang(s.toFloat())) * r0, c + dir(ang(s.toFloat())) * radius, Graphite, 0.4f, 0.18f)
    fill(circle(c.x, c.y, r0 - 4f, 30), Graphite, hatch = false, wash = 0.12f, lw = 0.9f)
    dot(c, 2.2f, Graphite, 0.9f)
    // Le capteur, sur la frontière du milieu (de 01…1 à 10…0).
    line(listOf(c + pt(0f, r0 - 2f), c + pt(0f, radius + 7f)), Crayons.rouge, 1.5f, 0.9f)
}

/** Deux disques d'encodeur rotatif : le binaire à gauche, le code de Gray à droite. */
private fun Pencil.grayCode(v: Int) {
    val bits = GrayParams.bits(v)
    val pal = Crayons.rainbow.rot((v * 3) % 8).take(bits)
    val r = 84f
    encoderDisc(pt(FW / 2 - 92f, FH / 2 - 3f), r, bits, { it }, pal)
    encoderDisc(pt(FW / 2 + 92f, FH / 2 - 3f), r, bits, { it xor (it shr 1) }, pal)
}

// ------------------------------------------------------------ Quadtree

/** Une case du quadtree : coin, côté, profondeur, dedans ou non. */
private class QuadLeaf(val x: Float, val y: Float, val s: Float, val depth: Int, val inside: Boolean)

/** Le quadtree d'une réunion de disques tirés au hasard (selon la variante) et ces disques. */
private class QuadRun(val leaves: List<QuadLeaf>, val discs: List<FloatArray>)

private const val QUAD_SIDE = 112f
private const val QUAD_X = (FW - 3 * QUAD_SIDE) / 2
private const val QUAD_Y = (FH - 2 * QUAD_SIDE) / 2

private val quadtrees = PerVariant { v ->
    val rnd = Random(v * 97 + 3)
    // Des disques qui restent dans le cadre, à 8 unités au moins du bord.
    val discs = List(5) {
        val r = 22f + rnd.nextFloat() * 30f
        floatArrayOf(QUAD_X + r + 8f + rnd.nextFloat() * (3 * QUAD_SIDE - 2 * r - 16f), QUAD_Y + r + 8f + rnd.nextFloat() * (2 * QUAD_SIDE - 2 * r - 16f), r)
    }
    // Distance (signée) à la réunion des disques : négative dedans.
    fun sd(x: Float, y: Float) = discs.minOf { hypot(x - it[0], y - it[1]) - it[2] }
    val leaves = mutableListOf<QuadLeaf>()
    fun split(x: Float, y: Float, s: Float, depth: Int) {
        val d = sd(x + s / 2, y + s / 2)
        val half = s * 0.7072f
        when {
            d < -half -> leaves += QuadLeaf(x, y, s, depth, true)
            d > half -> leaves += QuadLeaf(x, y, s, depth, false)
            depth == 5 -> leaves += QuadLeaf(x, y, s, depth, d < 0)
            else -> for (i in 0..1) for (j in 0..1) split(x + i * s / 2, y + j * s / 2, s / 2, depth + 1)
        }
    }
    for (i in 0 until 3) for (j in 0 until 2) split(QUAD_X + i * QUAD_SIDE, QUAD_Y + j * QUAD_SIDE, QUAD_SIDE, 0)
    QuadRun(leaves, discs)
}

/** Quadtree : les cases d'autant plus petites qu'elles approchent le bord de la forme. */
private fun Pencil.quadtree(v: Int) {
    val run = quadtrees[v]
    val pal = Crayons.rainbow.rot((v * 3 + 4) % 8).take(6)
    for (q in run.leaves) if (q.inside) {
        fill(rect(q.x, q.y, q.s), pal[q.depth], hatch = q.s > 20f, gap = 3.4f, angle = -58f + 20f * q.depth, wash = 0.35f + 0.08f * q.depth, outline = false)
    }
    for (q in run.leaves) cellFrame(q.x, q.y, q.s, q.s, if (q.inside) 0.55f else 0.3f, if (q.depth < 2) 0.7f else 0.45f)
    // Le vrai bord de la forme, en pointillés.
    for (d in run.discs) {
        val m = (d[2] * 1.6f).toInt()
        for (i in 0 until m) {
            val p = pt(d[0], d[1]) + dir(2 * PI.toFloat() * i / m) * d[2]
            if (run.discs.all { it === d || hypot(p.x - it[0], p.y - it[1]) > it[2] }) dot(p, 0.75f, Graphite, 0.75f)
        }
    }
    outline(rect(QUAD_X, QUAD_Y, 3 * QUAD_SIDE, 2 * QUAD_SIDE), lw = 1.4f)
}

// ------------------------------------------------------------ Bresenham

/** Les pixels d'un segment, par l'algorithme de Bresenham. */
private fun bresenhamLine(x0: Int, y0: Int, x1: Int, y1: Int, put: (Int, Int) -> Unit) {
    var x = x0; var y = y0
    val dx = abs(x1 - x0); val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
    var err = dx + dy
    while (true) {
        put(x, y)
        if (x == x1 && y == y1) break
        val e2 = 2 * err
        if (e2 >= dy) { err += dy; x += sx }
        if (e2 <= dx) { err += dx; y += sy }
    }
}

/** Les pixels d'un cercle de rayon [r] centré en 0, par l'algorithme du point milieu (un huitième, puis les symétries). */
private fun midpointCircle(r: Int, put: (Int, Int) -> Unit) {
    var x = r; var y = 0; var err = 1 - r
    while (x >= y) {
        put(x, y); put(y, x); put(-y, x); put(-x, y); put(-x, -y); put(-y, -x); put(y, -x); put(x, -y)
        y++
        if (err < 0) err += 2 * y + 1 else { x--; err += 2 * (y - x) + 1 }
    }
}

/** Un arc-en-ciel en pixels (cercles du point milieu), un soleil et ses rayons (segments de Bresenham), l'horizon. */
private fun Pencil.bresenham(v: Int) {
    val rOut = 52
    val cols = 2 * rOut + 3
    val cx = cols / 2
    val cy = rOut + 1
    val rows = cy + 5
    val c = min((FW - 16) / cols, (FH - 20) / rows)
    val ox = (FW - cols * c) / 2
    val oy = (FH - rows * c) / 2
    val grid = arrayOfNulls<Color>(cols * rows)
    fun horizon(x: Int) = cy + 3 - (2 * x + cols / 2) / cols
    fun put(x: Int, y: Int, color: Color) {
        if (x in 0 until cols && y in 0 until rows && y <= horizon(x)) grid[y * cols + x] = color
    }
    val arcs = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet)
        .let { if (v % 2 == 0) it else it.reversed() }
    arcs.forEachIndexed { i, color -> midpointCircle(rOut - 2 * i) { x, y -> if (y <= 0) put(cx + x, cy + y, color) } }
    // Le soleil : un disque plein, bordé par son cercle du point milieu.
    val sun = 7
    for (y in -sun..sun) for (x in -sun..sun) if (x * x + y * y <= sun * sun + sun) put(cx + x, cy + y, Crayons.jaune)
    midpointCircle(sun) { x, y -> put(cx + x, cy + y, Crayons.orange) }
    val rays = if (v % 2 == 0) 13 else 9
    val ends = List(rays) { i ->
        val a = PI.toFloat() * (1f + i / (rays - 1f))
        Triple(dir(a) * (sun + 3.5f), dir(a) * 34f, i)
    }
    for ((a, b, i) in ends) {
        bresenhamLine(cx + a.x.roundToInt(), cy + a.y.roundToInt(), cx + b.x.roundToInt(), cy + b.y.roundToInt()) { x, y ->
            put(x, y, if (i % 2 == 0) Crayons.orange else Crayons.rose)
        }
    }
    bresenhamLine(0, horizon(0), cols - 1, horizon(cols - 1)) { x, y -> if (y < rows) grid[y * cols + x] = Crayons.sapin }

    for (x in 0..cols) stroke(pt(ox + x * c, oy), pt(ox + x * c, oy + rows * c), Graphite, 0.3f, 0.07f)
    for (y in 0..rows) stroke(pt(ox, oy + y * c), pt(ox + cols * c, oy + y * c), Graphite, 0.3f, 0.07f)
    for (y in 0 until rows) for (x in 0 until cols) {
        val color = grid[y * cols + x] ?: continue
        pixel(ox + x * c + 0.2f, oy + y * c + 0.2f, c - 0.4f, color, 0.78f)
    }
    // Les tracés idéaux, au crayon fin.
    fun at(x: Float, y: Float) = pt(ox + (cx + x + 0.5f) * c, oy + (cy + y + 0.5f) * c)
    val n = 120
    for (r in listOf(rOut, rOut - 12)) line(List(n + 1) { at(r * cos(PI.toFloat() * (1 + it / n.toFloat())), r * sin(PI.toFloat() * (1 + it / n.toFloat()))) }, Graphite, 0.45f, 0.55f)
    for ((a, b, _) in ends) stroke(at(a.x, a.y), at(b.x, b.y), Graphite, 0.4f, 0.5f)
    stroke(pt(ox + 0.5f * c, oy + (horizon(0) + 0.5f) * c), pt(ox + (cols - 0.5f) * c, oy + (horizon(cols - 1) + 0.5f) * c), Graphite, 0.4f, 0.5f)
}

// ------------------------------------------------------------ Plus court chemin

private const val PATH_COLS = 47
private const val PATH_ROWS = 31

/** Une recherche : les murs, la distance des cases explorées (−1 sinon), le chemin trouvé. */
private class PathRun(val astar: Boolean, val wall: BooleanArray, val dist: IntArray, val open: BooleanArray, val path: List<Int>) {
    val explored get() = dist.count { it >= 0 }
    val length get() = path.size - 1
}

private val pathRuns = PerVariant { v ->
    val astar = v % 2 == 1
    val start = (PATH_ROWS / 2) * PATH_COLS + 1
    val goal = (PATH_ROWS / 2) * PATH_COLS + PATH_COLS - 2
    // Le même terrain pour Dijkstra et A* : il ne change qu'une variante sur deux.
    var seed = v / 2 * 41 + 7
    var wall: BooleanArray
    while (true) {
        wall = pathWalls(Random(seed))
        if (pathSearch(wall, start, goal, false) != null) break
        seed++
    }
    pathSearch(wall, start, goal, astar)!!
}

/** Trois murs percés de portes, et des cloisons au hasard. */
private fun pathWalls(rnd: Random): BooleanArray {
    val wall = BooleanArray(PATH_COLS * PATH_ROWS)
    val mid = PATH_ROWS / 2
    for (k in 0 until 3) {
        val x = 11 + 12 * k + rnd.nextInt(-1, 2)
        val doors = List(2) { rnd.nextInt(2, PATH_ROWS - 2) }
        for (y in 0 until PATH_ROWS) if (doors.none { abs(it - y) <= 1 }) wall[y * PATH_COLS + x] = true
    }
    repeat(16) {
        val horizontal = rnd.nextBoolean()
        val len = rnd.nextInt(4, 11)
        val x0 = rnd.nextInt(3, PATH_COLS - 4)
        val y0 = rnd.nextInt(1, PATH_ROWS - 1)
        for (t in 0 until len) {
            val x = if (horizontal) x0 + t else x0
            val y = if (horizontal) y0 else y0 + t
            if (x !in 3 until PATH_COLS - 3 || y !in 0 until PATH_ROWS) continue
            if (abs(y - mid) <= 1 && (x < 5 || x > PATH_COLS - 6)) continue
            wall[y * PATH_COLS + x] = true
        }
    }
    return wall
}

/** Dijkstra (ici un parcours en largeur, tous les pas valant 1) ou A* avec la distance de Manhattan. */
private fun pathSearch(wall: BooleanArray, start: Int, goal: Int, astar: Boolean): PathRun? {
    val n = PATH_COLS * PATH_ROWS
    val g = IntArray(n) { Int.MAX_VALUE }
    val from = IntArray(n) { -1 }
    val closed = IntArray(n) { -1 }
    val open = BooleanArray(n)
    fun h(i: Int) = if (astar) abs(i % PATH_COLS - goal % PATH_COLS) + abs(i / PATH_COLS - goal / PATH_COLS) else 0
    var order = 0L
    // File de priorité : f = g + h, puis le plus grand g, puis l'ordre d'arrivée.
    val queue = PriorityQueue<LongArray>(compareBy<LongArray>({ it[0] }, { -it[1] }, { it[2] }))
    g[start] = 0
    queue += longArrayOf(h(start).toLong(), 0, order++, start.toLong())
    open[start] = true
    while (queue.isNotEmpty()) {
        val top = queue.poll()!!
        val i = top[3].toInt()
        if (closed[i] >= 0) continue
        closed[i] = g[i]
        open[i] = false
        if (i == goal) {
            val path = mutableListOf(goal)
            while (path.last() != start) path += from[path.last()]
            return PathRun(astar, wall, closed, open, path.reversed())
        }
        val x = i % PATH_COLS; val y = i / PATH_COLS
        for ((nx, ny) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
            if (nx !in 0 until PATH_COLS || ny !in 0 until PATH_ROWS) continue
            val j = ny * PATH_COLS + nx
            if (wall[j] || closed[j] >= 0 || g[i] + 1 >= g[j]) continue
            g[j] = g[i] + 1
            from[j] = i
            open[j] = true
            queue += longArrayOf((g[j] + h(j)).toLong(), g[j].toLong(), order++, j.toLong())
        }
    }
    return null
}

/** Plus court chemin : les cases explorées en dégradé, les murs, la frontière et le chemin. */
private fun Pencil.shortestPath(v: Int) {
    val run = pathRuns[v]
    val c = min((FW - 20) / PATH_COLS, (FH - 20) / PATH_ROWS)
    val ox = (FW - PATH_COLS * c) / 2
    val oy = (FH - PATH_ROWS * c) / 2
    val pal = Crayons.rainbow.rot((v / 2 * 3 + 3) % 8).take(5)
    val maxD = run.dist.max().coerceAtLeast(1)
    fun center(i: Int) = pt(ox + (i % PATH_COLS + 0.5f) * c, oy + (i / PATH_COLS + 0.5f) * c)
    for (i in run.dist.indices) {
        val x = ox + (i % PATH_COLS) * c
        val y = oy + (i / PATH_COLS) * c
        when {
            run.wall[i] -> pixel(x, y, c, Graphite, 0.72f)
            // Des bandes de 3 pas : on voit avancer le front de la recherche.
            run.dist[i] >= 0 -> pixel(x + 0.3f, y + 0.3f, c - 0.6f, ramp(pal, run.dist[i] / maxD.toFloat()), if (run.dist[i] / 3 % 2 == 0) 0.66f else 0.36f)
            run.open[i] -> dot(center(i), 1.3f, ramp(pal, 1f), 0.8f)
        }
    }
    outline(rect(ox, oy, PATH_COLS * c, PATH_ROWS * c), lw = 1.3f)
    line(run.path.map { center(it) }, Crayons.rouge, 2.2f, 0.9f)
    val a = center(run.path.first())
    val b = center(run.path.last())
    fill(circle(a.x, a.y, 4.6f, 16), Crayons.jaune, hatch = false, wash = 0.95f, lw = 1f)
    fill(circle(b.x, b.y, 4.6f, 16), Crayons.rouge, hatch = false, wash = 0.95f, lw = 1f)
}

// ------------------------------------------------------------ Additionneur complet

private object AdderParams {
    /** Les trois entrées a, b, r. */
    fun bits(v: Int): Triple<Int, Int, Int> {
        val i = (v * 5 + 7) % 8
        return Triple(i shr 2 and 1, i shr 1 and 1, i and 1)
    }
}

/** Porte ET : dos plat, nez en demi-cercle. */
private fun andGate(x: Float, yc: Float, w: Float, h: Float): List<Offset> {
    val r = h / 2
    return listOf(pt(x, yc + r), pt(x, yc - r)) + List(17) { pt(x + w - r, yc) + dir(rad(-90f + 180f * it / 16)) * r }
}

/** Point d'une courbe de Bézier quadratique. */
private fun bezier(a: Offset, c: Offset, b: Offset, t: Float) = a * ((1 - t) * (1 - t)) + c * (2 * t * (1 - t)) + b * (t * t)

/** Dos creusé d'une porte OU, du bas vers le haut, décalé de [shift]. */
private fun orBack(x: Float, yc: Float, h: Float, shift: Float = 0f) =
    List(13) { bezier(pt(x + shift, yc + h / 2), pt(x + shift + h * 0.38f, yc), pt(x + shift, yc - h / 2), it / 12f) }

/** Porte OU : dos creusé, flancs en ogive. */
private fun orGate(x: Float, yc: Float, w: Float, h: Float): List<Offset> {
    val tip = pt(x + w, yc)
    val upper = List(16) { bezier(pt(x, yc - h / 2), pt(x + w * 0.62f, yc - h / 2), tip, (it + 1) / 16f) }
    val lower = List(15) { bezier(tip, pt(x + w * 0.62f, yc + h / 2), pt(x, yc + h / 2), (it + 1) / 16f) }
    return orBack(x, yc, h) + upper + lower
}

/** Un additionneur complet en portes logiques, les fils à 1 en rouge. */
private fun Pencil.fullAdder(v: Int) {
    val (a, b, r) = AdderParams.bits(v)
    val p = a xor b
    val s = p xor r
    val g = a and b
    val hh = p and r
    val carry = g or hh
    val dy = -13f
    fun q(x: Float, y: Float) = pt(x, y + dy)
    fun wire(value: Int, p0: Offset, p1: Offset, p2: Offset? = null, p3: Offset? = null) {
        val pts = listOfNotNull(p0, p1, p2, p3)
        if (value == 1) line(pts, Crayons.rouge, 2.1f, 0.9f) else line(pts, Crayons.bleu, 1.5f, 0.6f)
    }
    fun junction(value: Int, o: Offset) = dot(o, 2.4f, if (value == 1) Crayons.rouge else Crayons.bleu, 0.95f)
    val w = 44f
    val h = 34f
    // Les dos des portes OU : l'entrée s'arrête sur la courbe.
    fun backX(x: Float, yc: Float, y: Float): Float { val t = (yc + h / 2 - y) / h; return x + 2 * t * (1 - t) * h * 0.38f }

    // Les fils.
    wire(a, q(20f, 60f), q(44f, 60f), q(44f, 79f), q(backX(104f, 88f, 79f) + 2f, 79f)); wire(a, q(58f, 79f), q(58f, 151f), q(104f, 151f))
    wire(b, q(20f, 106f), q(44f, 106f), q(44f, 97f), q(backX(104f, 88f, 97f) + 2f, 97f)); wire(b, q(74f, 97f), q(74f, 169f), q(104f, 169f))
    wire(r, q(20f, 216f), q(190f, 216f), q(190f, 121f), q(backX(212f, 112f, 121f) + 2f, 121f)); wire(r, q(190f, 193f), q(212f, 193f))
    wire(p, q(148f, 88f), q(176f, 88f), q(176f, 103f), q(backX(212f, 112f, 103f) + 2f, 103f)); wire(p, q(176f, 103f), q(176f, 175f), q(212f, 175f))
    wire(g, q(148f, 160f), q(backX(290f, 172f, 160f) + 2f, 160f))
    wire(hh, q(256f, 184f), q(backX(290f, 172f, 184f) + 2f, 184f))
    wire(s, q(256f, 112f), q(340f, 112f))
    wire(carry, q(334f, 172f), q(340f, 172f))
    junction(a, q(58f, 79f)); junction(b, q(74f, 97f)); junction(r, q(190f, 193f)); junction(p, q(176f, 103f))

    // Les portes : OU exclusif en violet, ET en vert, OU en orange.
    fun xorGate(x: Float, yc: Float) {
        fill(orGate(x, yc, w, h).map { pt(it.x, it.y + dy) }, Crayons.violet, gap = 3.2f, lw = 1.1f)
        outline(orBack(x, yc, h, -6f).map { pt(it.x, it.y + dy) }, close = false, lw = 1.1f)
    }
    xorGate(104f, 88f)
    xorGate(212f, 112f)
    fill(andGate(104f, 160f + dy, w, h), Crayons.vert, gap = 3.2f, angle = -40f, lw = 1.1f)
    fill(andGate(212f, 184f + dy, w, h), Crayons.vert, gap = 3.2f, angle = -40f, lw = 1.1f)
    fill(orGate(290f, 172f, w, h).map { pt(it.x, it.y + dy) }, Crayons.orange, gap = 3.2f, angle = -70f, lw = 1.1f)

    // Les voyants des entrées et des sorties.
    fun lamp(value: Int, o: Offset) {
        if (value == 1) {
            fill(circle(o.x, o.y, 8.5f, 22), Crayons.jaune, gap = 2.2f, wash = 0.85f, lw = 1.1f)
            outline(circle(o.x, o.y, 11.5f, 26), lw = 0.6f, color = Crayons.orange)
        } else {
            fill(circle(o.x, o.y, 8.5f, 22), Graphite, hatch = false, wash = 0.1f, lw = 1.1f)
        }
    }
    lamp(a, q(16f, 60f)); lamp(b, q(16f, 106f)); lamp(r, q(16f, 216f))
    lamp(s, q(350f, 112f)); lamp(carry, q(350f, 172f))
}

// ------------------------------------------------------------ Arbre rouge-noir

/** Nœud d'un arbre rouge-noir penché à gauche (Sedgewick). */
private class RbNode(val key: Int) {
    var left: RbNode? = null
    var right: RbNode? = null
    var red = true
}

private fun isRed(h: RbNode?) = h?.red == true

private fun rbInsert(h: RbNode?, key: Int): RbNode {
    if (h == null) return RbNode(key)
    if (key < h.key) h.left = rbInsert(h.left, key) else h.right = rbInsert(h.right, key)
    var n: RbNode = h
    if (isRed(n.right) && !isRed(n.left)) {
        val x = n.right!!; n.right = x.left; x.left = n; x.red = n.red; n.red = true; n = x
    }
    if (isRed(n.left) && isRed(n.left!!.left)) {
        val x = n.left!!; n.left = x.right; x.right = n; x.red = n.red; n.red = true; n = x
    }
    if (isRed(n.left) && isRed(n.right)) {
        n.red = !n.red; n.left!!.red = !n.left!!.red; n.right!!.red = !n.right!!.red
    }
    return n
}

/** Un arbre rouge-noir tout fait : racine, nombre de clés, hauteur noire, nombre de niveaux. */
private class RbTree(val root: RbNode, val size: Int) {
    val blackHeight: Int get() { var n: RbNode? = root; var b = 0; while (n != null) { if (!n.red) b++; n = n.left }; return b }
    val levels: Int get() = depth(root)
    private fun depth(n: RbNode?): Int = if (n == null) 0 else 1 + max(depth(n.left), depth(n.right))
}

private val redBlackTrees = PerVariant { v ->
    val size = intArrayOf(31, 24, 27, 20)[v % 4]
    var root: RbNode? = null
    for (key in (1..size).shuffled(Random(v * 7 + 11))) {
        root = rbInsert(root, key)
        root.red = false
    }
    RbTree(root!!, size)
}

/** Arbre rouge-noir : les clés de gauche à droite, les feuilles vides en petits carrés, les nœuds 2-3 en gélules. */
private fun Pencil.redBlack(v: Int) {
    val tree = redBlackTrees[v]
    val n = tree.size
    val levels = tree.levels
    val sx = (FW - 28f) / (2 * n)
    val sy = (FH - 30f) / levels
    val r = min(6.5f, sx * 0.85f)
    fun at(slot: Int, depth: Int) = pt(14f + slot * sx, 14f + depth * sy)
    val tint = listOf(Crayons.jaune, Crayons.turquoise, Crayons.rose, Crayons.vert)[v % 4]
    // Les gélules d'abord : un nœud rouge et son parent.
    fun capsules(node: RbNode, depth: Int) {
        val p = at(2 * node.key - 1, depth)
        for (kid in listOfNotNull(node.left, node.right)) {
            if (kid.red) {
                val q = at(2 * kid.key - 1, depth + 1)
                val d = (q - p) / (q - p).getDistance()
                val nrm = pt(-d.y, d.x)
                val rr = r + 3.2f
                val shape = List(10) { p + (nrm * cos(PI.toFloat() * it / 9) - d * sin(PI.toFloat() * it / 9)) * rr } +
                    List(10) { q + (d * sin(PI.toFloat() * it / 9) - nrm * cos(PI.toFloat() * it / 9)) * rr }
                fill(shape, tint, hatch = false, wash = 0.35f, lw = 0.6f)
            }
            capsules(kid, depth + 1)
        }
    }
    capsules(tree.root, 0)
    fun edges(node: RbNode, depth: Int) {
        val p = at(2 * node.key - 1, depth)
        val kids = listOf(node.left, node.right)
        kids.forEachIndexed { side, kid ->
            if (kid == null) {
                val q = at(2 * node.key - 1 + if (side == 0) -1 else 1, depth + 1)
                stroke(p, q, Graphite, 0.6f, 0.35f)
                fill(rect(q.x - 1.6f, q.y - 1.6f, 3.2f), Graphite, hatch = false, wash = 0.75f, outline = false)
            } else {
                val q = at(2 * kid.key - 1, depth + 1)
                if (kid.red) line(listOf(p, q), Crayons.rouge, 1.8f, 0.9f) else line(listOf(p, q), Graphite, 1f, 0.75f)
                edges(kid, depth + 1)
            }
        }
    }
    edges(tree.root, 0)
    fun nodes(node: RbNode, depth: Int) {
        val p = at(2 * node.key - 1, depth)
        if (node.red) fill(circle(p.x, p.y, r, 18), Crayons.rouge, hatch = false, wash = 0.85f, lw = 0.8f)
        else fill(circle(p.x, p.y, r, 18), Graphite, hatch = false, wash = 0.8f, lw = 0.8f)
        node.left?.let { nodes(it, depth + 1) }
        node.right?.let { nodes(it, depth + 1) }
    }
    nodes(tree.root, 0)
}

// ------------------------------------------------------------ Tramage

private const val DITHER_COLS = 88
private const val DITHER_ROWS = 58

/** Les deux palettes de cinq crayons du tramage. */
private fun ditherPalette(v: Int) = if (v % 2 == 0) {
    listOf(Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.rose, Crayons.violet)
} else {
    listOf(Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.marine)
}

/** Pour chaque pixel, l'indice du crayon choisi : par seuil à gauche, par diffusion d'erreur à droite. */
private val ditherRuns = PerVariant { v ->
    val pal = ditherPalette(v)
    val rgb = pal.map { floatArrayOf(it.red, it.green, it.blue) }
    val img = Array(DITHER_ROWS * DITHER_COLS) { i ->
        val x = (i % DITHER_COLS + 0.5f) / DITHER_COLS
        val y = (i / DITHER_COLS + 0.5f) / DITHER_ROWS
        // Un soleil couchant : des anneaux autour d'un point au milieu du bas.
        val t = (hypot((x - 0.5f) * 1.5f, (y - 1.02f)) / 1.08f).coerceIn(0f, 1f)
        val c = ramp(pal, t)
        floatArrayOf(c.red, c.green, c.blue)
    }
    fun nearest(c: FloatArray) = rgb.indices.minBy { k -> (0..2).sumOf { ((c[it] - rgb[k][it]) * (c[it] - rgb[k][it])).toDouble() } }
    val out = IntArray(DITHER_ROWS * DITHER_COLS)
    val half = DITHER_COLS / 2
    for (y in 0 until DITHER_ROWS) for (x in 0 until DITHER_COLS) {
        val i = y * DITHER_COLS + x
        val k = nearest(img[i])
        out[i] = k
        if (x < half) continue
        val err = FloatArray(3) { img[i][it] - rgb[k][it] }
        fun spread(dx: Int, dy: Int, wgt: Float) {
            val nx = x + dx; val ny = y + dy
            if (nx < half || nx >= DITHER_COLS || ny >= DITHER_ROWS) return
            val cell = img[ny * DITHER_COLS + nx]
            for (ch in 0..2) cell[ch] += err[ch] * wgt
        }
        spread(1, 0, 7f / 16); spread(-1, 1, 3f / 16); spread(0, 1, 5f / 16); spread(1, 1, 1f / 16)
    }
    out
}

/** Le même dégradé en cinq crayons : seuil à gauche, Floyd-Steinberg à droite. */
private fun Pencil.dithering(v: Int) {
    val out = ditherRuns[v]
    val pal = ditherPalette(v)
    val c = min((FW - 16) / DITHER_COLS, (FH - 18) / DITHER_ROWS)
    val ox = (FW - DITHER_COLS * c) / 2
    val oy = (FH - DITHER_ROWS * c) / 2
    for (i in out.indices) pixel(ox + (i % DITHER_COLS) * c, oy + (i / DITHER_COLS) * c, c, pal[out[i]], 0.8f)
    outline(rect(ox, oy, DITHER_COLS * c, DITHER_ROWS * c), lw = 1.3f)
    line(listOf(pt(ox + DITHER_COLS / 2 * c, oy - 4f), pt(ox + DITHER_COLS / 2 * c, oy + DITHER_ROWS * c + 4f)), Graphite, 1.6f)
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

// ------------------------------------------------------------ sin(1/x)

/** Le graphe de xᵏ sin(1/x), arche par arche, avec la bande où les oscillations deviennent trop serrées. */
private fun Pencil.sinInverse(v: Int) {
    val kpow = v % 3
    val xMax = 0.62f
    val yMax = if (kpow == 0) 1f else xMax.pow(kpow)
    val plot = Plot(-xMax, xMax, -yMax * 1.12f, yMax * 1.12f, left = 16f, right = FW - 16f, top = 16f, bottom = FH - 16f)
    fun f(x: Float) = x.pow(kpow) * sin(1 / x)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val uMax = 60f
    // Les axes.
    stroke(plot(-xMax, 0f), plot(xMax, 0f), Graphite, 0.6f, 0.4f)
    stroke(plot(0f, -yMax * 1.12f), plot(0f, yMax * 1.12f), Graphite, 0.6f, 0.4f)
    // Les enveloppes.
    if (kpow == 0) {
        dashed(plot(-xMax, 1f), plot(xMax, 1f), alpha = 0.45f); dashed(plot(-xMax, -1f), plot(xMax, -1f), alpha = 0.45f)
    } else {
        for (sgn in listOf(1f, -1f)) {
            val env = List(81) { -xMax + 2 * xMax * it / 80 }.map { plot(it, sgn * abs(it).pow(kpow)) }
            dashedPath(env, Graphite, 0.8f, 0.5f, 2)
        }
    }
    // Chaque arche, de u = nπ à (n + 1)π, avec u = 1/x.
    for (sgn in listOf(1f, -1f)) {
        var u = 1 / xMax
        var arch = floor(u / PI.toFloat()).toInt()
        while (u < uMax) {
            val end = min(uMax, (arch + 1) * PI.toFloat())
            val m = max(max(4, ((end - u) / 0.12f).toInt()), ((1 / u - 1 / end) * plot.sx / 1.5f).toInt())
            val pts = List(m + 1) { val x = sgn / (u + (end - u) * it / m); plot(x, f(x)) }
            val color = pal[(arch + if (sgn < 0) 4 else 0) % 8]
            val wide = abs(pts.first().x - pts.last().x) > 10f
            fill(pts + listOf(pt(pts.last().x, plot.y(0f)), pt(pts.first().x, plot.y(0f))), color, hatch = wide, gap = 3f, wash = 0.25f, outline = false, strokeAlpha = 0.5f)
            line(sparse(pts, 1.5f), color, 1.4f, 0.95f)
            u = end
            arch++
        }
    }
    // Près de 0, les oscillations se fondent en une bande.
    val e = 1 / uMax
    val band = if (kpow == 0) {
        listOf(plot(-e, 1f), plot(e, 1f), plot(e, -1f), plot(-e, -1f))
    } else {
        val ys = e.pow(kpow)
        listOf(plot(-e, ys), plot(0f, 0f), plot(e, ys), plot(e, -ys), plot(0f, 0f), plot(-e, -ys))
    }
    fill(band, Graphite, gap = 1.8f, wash = 0.14f, strokeAlpha = 0.4f, lw = 0.7f, angle = 90f)
}

// ------------------------------------------------------------ Zêta de Riemann

private object ZetaParams {
    fun tMax(v: Int) = if (v % 2 == 0) 50.0 else 35.0
}

/** Les premiers zéros non triviaux de ζ, parties imaginaires. */
private val ZETA_ZEROS = listOf(14.134725, 21.022040, 25.010858, 30.424876, 32.935062, 37.586178, 40.918719, 43.327073, 48.005151, 49.773832)

/** B₂ₖ / (2k)! pour k de 1 à 7 (Euler-Maclaurin). */
private val BERNOULLI_TERMS = doubleArrayOf(1.0 / 12, -1.0 / 720, 1.0 / 30240, -1.0 / 1209600, 1.0 / 47900160, -691.0 / 1307674368000.0, 1.0 / 74724249600.0)

/** ζ(1/2 + it) par la formule d'Euler-Maclaurin : 23 termes de la série, puis la correction. */
private fun zetaCritical(t: Double): Pair<Double, Double> {
    val big = 24
    val sr = 0.5
    var re = 0.0; var im = 0.0
    for (n in 1 until big) {
        val m = n.toDouble().pow(-sr); val a = t * ln(n.toDouble())
        re += m * cos(a); im -= m * sin(a)
    }
    // N^(−s), puis N^(1−s)/(s − 1) et N^(−s)/2.
    val lnN = ln(big.toDouble())
    val nm = exp(-sr * lnN)
    val nsr = nm * cos(t * lnN); val nsi = -nm * sin(t * lnN)
    re += nsr / 2; im += nsi / 2
    val ar = big * nsr; val ai = big * nsi
    val dr = sr - 1; val di = t
    val den = dr * dr + di * di
    re += (ar * dr + ai * di) / den; im += (ai * dr - ar * di) / den
    // Termes de Bernoulli : s (s + 1) … (s + 2k − 2) N^(−s−2k+1).
    var pr = (sr * nsr - t * nsi) / big; var pi = (sr * nsi + t * nsr) / big
    for (k in 1..BERNOULLI_TERMS.size) {
        re += BERNOULLI_TERMS[k - 1] * pr; im += BERNOULLI_TERMS[k - 1] * pi
        val ar1 = sr + 2 * k - 1; val ar2 = sr + 2 * k
        // p ← p (s + 2k − 1)(s + 2k) / N²
        val qr = ar1 * ar2 - t * t; val qi = t * (ar1 + ar2)
        val nr = pr * qr - pi * qi; val ni = pr * qi + pi * qr
        pr = nr / (big * big); pi = ni / (big * big)
    }
    return re to im
}

/** Les points de ζ(1/2 + it), coupés aux zéros. */
private val zetaRuns = PerVariant { v ->
    val tMax = ZetaParams.tMax(v)
    val cuts = listOf(0.0) + ZETA_ZEROS.filter { it < tMax } + tMax
    cuts.zipWithNext { a, b ->
        val m = max(20, ((b - a) / 0.02).toInt())
        List(m + 1) { val (x, y) = zetaCritical(a + (b - a) * it / m); pt(x.toFloat(), -y.toFloat()) }
    }
}

/** La spirale de ζ sur la droite critique : une boucle par zéro, chacune d'une couleur. */
private fun Pencil.zetaSpiral(v: Int) {
    val pieces = zetaRuns[v]
    val fit = Fit(pieces, 14f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    // Les axes réel et imaginaire.
    val all = pieces.flatten()
    val xs = all.minOf { it.x } to all.maxOf { it.x }
    val ys = all.minOf { it.y } to all.maxOf { it.y }
    stroke(fit(pt(xs.first - 0.2f, 0f)), fit(pt(xs.second + 0.2f, 0f)), Graphite, 0.6f, 0.35f)
    stroke(fit(pt(0f, ys.first - 0.1f)), fit(pt(0f, ys.second + 0.1f)), Graphite, 0.6f, 0.35f)
    dashed(fit(pt(1f, ys.first)), fit(pt(1f, ys.second)), alpha = 0.25f)
    val drawn = pieces.map { sparse(fit(it)) }
    drawn.forEachIndexed { i, p -> line(p, pal[i % 8], 1.6f, 0.9f) }
    val o = fit(Offset.Zero)
    outline(circle(o.x, o.y, 4.5f, 16), lw = 1f)
    val s = fit(pieces.first().first())
    fill(circle(s.x, s.y, 4f, 14), Crayons.jaune, hatch = false, wash = 0.95f, lw = 1f)
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

// ------------------------------------------------------------ Taylor

/** Les polynômes de Taylor de sin (ou de cos) en 0, de plus en plus longs, autour de la sinusoïde. */
private fun Pencil.taylorSeries(v: Int) {
    val sine = v % 2 == 0
    val plot = Plot(-9.6f, 9.6f, -2.5f, 2.5f)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    stroke(plot(plot.x0, 0f), plot(plot.x1, 0f), Graphite, 0.7f, 0.45f)
    stroke(plot(0f, plot.y0), plot(0f, plot.y1), Graphite, 0.7f, 0.45f)
    for (i in -3..3) if (i != 0) dot(plot(i * PI.toFloat(), 0f), 1.4f, Graphite, 0.6f)
    val m = 220
    val xs = List(m + 1) { plot.x0 + (plot.x1 - plot.x0) * it / m }
    inside(plot) {
        line(xs.map { plot(it, if (sine) sin(it) else cos(it)) }, Graphite, 3.4f, 0.28f)
        for (i in 0 until 9) {
            val degree = if (sine) 2 * i + 1 else 2 * i
            val pts = xs.map { x ->
                var term = if (sine) x else 1f
                var sum = term
                var d = if (sine) 1 else 0
                while (d < degree) { term *= -x * x / ((d + 1) * (d + 2)); sum += term; d += 2 }
                plot(x, sum.coerceIn(-4f, 4f))
            }
            line(pts, ramp(pal, i / 8f), 1.4f, 0.9f)
        }
    }
    outline(rect(plot.left, plot.top, plot.right - plot.left, plot.bottom - plot.top), lw = 1f, color = Graphite.copy(alpha = 0.35f))
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
