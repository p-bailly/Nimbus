package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.random.Random

// Figures des thèmes « Cercles », « Nombres » et « Graphes », deuxième série : moirés, inversion,
// faisceaux d'Apollonius, engrenages, sangaku, cercles osculateurs, bulles ; tas de sable, PGCD,
// diviseurs, Collatz, carrés magiques, spirale de Sacks, Catalan, Nicomaque ; Petersen, Heawood,
// Königsberg, quatre couleurs, cavalier.

internal val moreNumberFigures: List<FigureSpec> = listOf(
    // ------------------------------------------------------------ Cercles
    FigureSpec("moire_rings", "Moiré de cercles", "Cercles", { v ->
        if (v % 2 == 0) {
            "Deux familles de cercles concentriques, également espacés, autour de deux centres voisins. Là où les traits se superposent ou s’intercalent, l’œil voit " +
                "des franges : ce sont les points où le n-ième cercle de l’une croise le m-ième de l’autre avec n − m constant, donc où la différence des distances " +
                "aux deux centres est constante. Ce sont des hyperboles, de foyers les deux centres."
        } else {
            "Des cercles concentriques et des droites parallèles, avec le même écart. Les franges du moiré relient les croisements du n-ième cercle et de la m-ième droite " +
                "avec n − m constant : la distance au centre y est égale à la distance à une droite fixe, plus une constante. Ce sont des paraboles, toutes de même foyer, " +
                "le centre des cercles."
        }
    }) { moireRings(it) },
    FigureSpec("grid_inversion", "Inversion d’une grille", "Cercles", { _ ->
        "L’inversion de centre O et de rayon R envoie chaque point P sur le point P′ de la demi-droite [OP) tel que OP × OP′ = R². Elle fixe le cercle, échange " +
            "son intérieur et son extérieur, et change les droites en cercles passant par O. Le damier du dehors se retrouve plié dans le disque, ses cases rapetissant " +
            "vers O, où file tout ce qui est très loin."
    }) { gridInversion(it) },
    FigureSpec("apollonius_pencils", "Faisceaux d’Apollonius", "Cercles", { _ ->
        "Deux familles de cercles autour de deux points A et B. Les uns passent par A et B. Les autres, coloriés, sont les cercles d’Apollonius : l’ensemble des points P " +
            "tels que PA / PB = k, pour k fixé, est un cercle centré sur la droite (AB). Chaque cercle d’une famille coupe ceux de l’autre à angle droit, comme les lignes " +
            "de champ et les équipotentielles de deux charges opposées."
    }) { apolloniusPencils(it) },
    FigureSpec("planetary_gears", "Train planétaire", "Cercles", { v ->
        val (zs, zp) = gearTeeth(v)
        val zr = zs + 2 * zp
        val g = gcd(zs, zs + zr)
        "Un soleil de $zs dents, trois satellites de $zp dents et une couronne de $zr dents à denture intérieure, soit $zs + 2 × $zp. Deux roues engrènent si leurs dents ont " +
            "le même pas ; trois satellites également espacés exigent que $zs + $zr soit divisible par 3. Couronne fixe, le porte-satellites tourne ${zs / g} fois quand " +
            "le soleil en fait ${(zs + zr) / g} (formule de Willis)."
    }) { planetaryGears(it) },
    FigureSpec("sangaku", "Sangaku", "Cercles", { _ ->
        "Dans un carré de côté a, deux quarts de cercle de rayon a, centrés en deux coins voisins. Le cercle posé entre eux sur le côté a pour rayon 3a/8, celui coincé " +
            "au-dessus de leur croisement a/16, et ceux des bords a/6. Ces problèmes de contacts étaient peints sur des tablettes de bois offertes aux temples du Japon, " +
            "les sangaku, à l’époque d’Edo."
    }) { sangakuTablet(it) },
    FigureSpec("osculating", "Cercles osculateurs", "Cercles", { v ->
        val ratio = OSC_A / oscB(v)
        val cube = String.format(java.util.Locale.FRANCE, "%.1f", ratio * ratio * ratio)
        "En chaque point d’une ellipse, le cercle osculateur épouse la courbe au plus près : même tangente, même courbure. Son centre décrit la développée, l’étoile " +
            "rouge. Le long d’un arc entre deux sommets, la courbure varie dans un seul sens, et ces cercles s’emboîtent sans jamais se couper (théorème de Tait " +
            "et Kneser). Ici a/b ≈ ${String.format(java.util.Locale.FRANCE, "%.2f", ratio)} : le plus grand rayon, a²/b, vaut $cube fois le plus petit, b²/a."
    }) { osculatingCircles(it) },
    FigureSpec("bubbles", "Bulles", "Cercles", { v ->
        val c = String.format(java.util.Locale.FRANCE, "%.2f", bubbleExponent(v))
        "Les bulles sont semées une à une, à des places tirées au hasard, en refusant celles qui en chevaucheraient une autre. La n-ième a une aire proportionnelle à " +
            "1/(n + 2)^$c : une série qui converge vers la surface à remplir. John Shier a observé en 2011 qu’avec de tels exposants, il reste toujours de la place " +
            "pour la suivante : les bulles finissent par tout couvrir. C’est la « géométrie statistique »."
    }) { seededBubbles(it) },
    // ------------------------------------------------------------ Nombres
    FigureSpec("sandpile", "Tas de sable abélien", "Nombres", { v ->
        val start = if (v % 2 == 0) "On verse 16 384 grains sur une seule case" else "On verse 8 192 grains sur chacune de deux cases"
        "$start d’une grille. Dès qu’une case en porte 4 ou plus, elle s’éboule et en donne un à chacune de ses quatre voisines. Quand tout s’est arrêté, chaque case " +
            "garde 0, 1, 2 ou 3 grains : quatre couleurs. L’ordre des éboulements ne change rien au résultat, d’où « abélien » (Bak, Tang et Wiesenfeld, 1987 ; " +
            "Dhar, 1990), et le motif, fait de plages régulières, est fractal."
    }) { sandpile(it) },
    FigureSpec("gcd_quilt", "Courtepointe des PGCD", "Nombres", { v ->
        val range = if (v % 2 == 0) "a de −18 à 18 et b de −12 à 12, l’origine au centre" else "a de 1 à 37 et b de 1 à 25, l’origine en bas à gauche"
        "La case (a, b), avec $range, est coloriée selon le plus grand diviseur commun de a et b, plus foncée quand il est grand. Les cases blanches pointées sont " +
            "les couples premiers entre eux : vus depuis l’origine, aucun autre point du réseau ne les cache. Sur un carré de plus en plus grand, leur proportion " +
            "tend vers 6/π² ≈ 60,8 %."
    }) { gcdQuilt(it) },
    FigureSpec("divisor_arcs", "Toile des diviseurs", "Nombres", { v ->
        val n = divisorCount(v)
        "Les entiers de 1 à $n, sur une échelle logarithmique ; un arc relie m à 2m (au-dessus), à 3m (au-dessous), à 5m, à 7m…, une couleur par nombre premier. " +
            "Multiplier par p, c’est avancer d’un pas fixe : les arcs d’un même premier sont identiques. Chaque nombre reçoit autant d’arcs qu’il a de facteurs " +
            "premiers distincts ; les premiers, pointés en jaune, n’en reçoivent qu’un, venu de 1."
    }) { divisorArcs(it) },
    FigureSpec("collatz", "Corail de Collatz", "Nombres", { v ->
        val n = collatzParams(v).first
        "Suite de Syracuse : si n est pair, on le divise par 2, sinon on le remplace par 3n + 1. Les trajectoires des nombres de 1 à $n, remontées depuis 1, " +
            "tournent un peu à droite à chaque nombre pair et à gauche à chaque impair ; elles forment ce corail, épais là où passent beaucoup de trajectoires. " +
            "Toutes finissent-elles par 1 ? C’est la conjecture de Collatz (1937), vérifiée jusqu’à 2⁶⁸."
    }) { collatzCoral(it) },
    FigureSpec("magic_lines", "Lignes magiques", "Nombres", { v ->
        val (n, which) = when (v % 3) {
            0 -> 4 to "celui de Dürer, gravé dans Melencolia I en 1514"
            1 -> 5 to "construit par la méthode siamoise de La Loubère (1693)"
            else -> 8 to "obtenu en écrivant 1 à 64 dans l’ordre puis en remplaçant k par 65 − k sur les diagonales des blocs 4 × 4"
        }
        "Un carré magique d’ordre $n, $which. Chaque ligne, colonne et diagonale fait la même somme, n(n² + 1)/2 = ${n * (n * n + 1) / 2}. Les cases sont teintées " +
            "selon leur nombre, et le trait relie 1, 2, 3… dans l’ordre, du petit rond au point final : les « lignes magiques » dont l’architecte Claude Bragdon tirait des ornements."
    }) { magicLines(it) },
    FigureSpec("sacks_spiral", "Spirale de Sacks", "Nombres", { v ->
        val colors = if (v % 2 == 0) {
            "en rouge ceux de la forme 4k + 1 (sommes de deux carrés, selon Fermat), en bleu ceux de la forme 4k + 3"
        } else {
            "en rouge ceux de la forme k² + k + 41 : Euler a remarqué que ce polynôme donne 40 premiers de suite"
        }
        "Les entiers s’enroulent sur une spirale d’Archimède : n est à la distance √n du centre, et chaque tour s’achève sur un carré parfait, tous alignés à droite. " +
            "Les 1 229 nombres premiers jusqu’à 10 000 sont pointés, $colors. Ils se rangent le long de courbes. Robert Sacks, 1994."
    }) { sacksSpiral(it) },
    FigureSpec("catalan", "Nombres de Catalan", "Nombres", { _ ->
        "Les 14 façons de découper un hexagone en triangles par des diagonales qui ne se croisent pas. Un polygone à n + 2 côtés en a Cₙ = (2n)! / (n! (n + 1)!) : " +
            "1, 2, 5, 14, 42, 132… Ces nombres de Catalan comptent aussi les parenthésages et les chemins de Dyck. Les triangles sont coloriés selon leur nombre de côtés " +
            "pris sur l’hexagone : deux, un ou aucun."
    }) { hexTriangulations(it) },
    FigureSpec("nicomachus", "Somme des cubes", "Nombres", { v ->
        val n = 5 + v % 3
        val t = n * (n + 1) / 2
        "Le carré de côté 1 + 2 + … + $n = $t se découpe en équerres ; la k-ième, de largeur k, est faite de k carrés de côté k (pour k pair, deux moitiés en font un) : " +
            "elle vaut k³. D’où 1³ + 2³ + … + ${n}³ = (1 + 2 + … + $n)² = ${t}² = ${t * t}. Cette identité est attribuée à Nicomaque de Gérase, vers l’an 100."
    }) { cubeSum(it) },
    // ------------------------------------------------------------ Graphes
    FigureSpec("petersen", "Graphe de Petersen", "Graphes", { v ->
        if (v % 2 == 0) {
            "Dix sommets, quinze arêtes, chaque sommet a trois voisins : un pentagone, une étoile à cinq branches et cinq rayons qui les relient. Trois couleurs suffisent " +
                "pour les sommets, deux voisins n’ayant jamais la même. Deux sommets non voisins ont toujours exactement un voisin commun, et aucun circuit ne passe " +
                "une seule fois par chaque sommet. Julius Petersen, 1898."
        } else {
            "Le graphe de Petersen, avec ses arêtes coloriées de sorte que deux arêtes d’un même sommet diffèrent. Trois couleurs ne suffisent pas : il en faut une " +
                "quatrième, bien que chaque sommet n’ait que trois arêtes. C’est le plus petit « snark », graphe cubique sans pont qui résiste à trois couleurs ; " +
                "le nom, pris à Lewis Carroll, est dû à Martin Gardner (1976)."
        }
    }) { petersenGraph(it) },
    FigureSpec("heawood", "Graphe de Heawood", "Graphes", { v ->
        val shape = if (v % 2 == 0) {
            "Ses 14 sommets sont posés sur un cercle, reliés en anneau et par des cordes qui sautent cinq places"
        } else {
            "Dehors, les 7 points du plan de Fano ; dedans, ses 7 droites ; une arête relie chaque droite à ses 3 points"
        }
        "$shape. Le graphe de Heawood a 21 arêtes, chaque sommet trois voisins, et son plus petit circuit compte 6 arêtes : c’est le plus petit graphe ainsi. " +
            "Deux arêtes d’une même couleur ne se touchent jamais. Percy Heawood, 1890 : ce sont les frontières de sa carte à 7 pays sur un tore, tous voisins entre eux."
    }) { heawoodGraph(it) },
    FigureSpec("konigsberg", "Ponts de Königsberg", "Graphes", { v ->
        val walk = if (v % 2 == 0) {
            "Le graphe par-dessus : un sommet par rive ou île, une arête par pont."
        } else {
            "Le promeneur en pointillé passe six ponts et reste coincé : le septième, en ${listOf("rouge", "violet", "orange", "rose")[(v / 2) % 4]}, lui échappe."
        }
        "Peut-on traverser les sept ponts de Königsberg une fois chacun ? Euler a répondu non en 1736 : il faudrait qu’au plus deux des quatre terres touchent un nombre " +
            "impair de ponts, or l’île en touche 5 et les trois autres 3. C’est l’acte de naissance de la théorie des graphes. $walk"
    }) { konigsbergBridges(it) },
    FigureSpec("four_colors", "Carte à quatre couleurs", "Graphes", { v ->
        val (a, b, c) = mapRings(v)
        "Une carte de ${1 + a + b + c} pays : un au centre, puis des anneaux de $a, $b et $c. Quatre couleurs suffisent pour que deux pays qui partagent une frontière " +
            "diffèrent ; ici il en faut bien quatre, car le pays central touche un anneau de 5. Conjecturé en 1852 par Francis Guthrie, démontré en 1976 par Appel et " +
            "Haken avec un ordinateur, puis vérifié en Coq par Gonthier en 2005."
    }) { fourColorMap(it) },
    FigureSpec("knight_tour", "Tour du cavalier", "Graphes", { v ->
        val start = KNIGHT_STARTS[v % KNIGHT_STARTS.size]
        val square = "${'a' + start % 8}${8 - start / 8}"
        "Le cavalier passe une fois et une seule sur chacune des 64 cases, puis revient d’un bond à son départ, en $square : un tour fermé, brodé ici d’un fil " +
            "qui change de couleur. Il suit la règle de Warnsdorff (1823) : sauter toujours vers la case d’où il aura le moins de sorties libres. " +
            "L’échiquier compte 13 267 364 410 532 tours fermés (McKay, 1997)."
    }) { knightTour(it) },
)

private val FULL_TURN = 2 * PI.toFloat()

private fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)

private fun rect(x: Float, y: Float, w: Float, h: Float = w) = listOf(pt(x, y), pt(x + w, y), pt(x + w, y + h), pt(x, y + h))

/** Dessine [block] découpé dans le polygone [shape] (ou hors de lui, si [outside]). */
private inline fun Pencil.clipTo(shape: List<Offset>, outside: Boolean = false, crossinline block: () -> Unit) =
    scope.clipPath(path(shape), if (outside) ClipOp.Difference else ClipOp.Intersect) { block() }

/** Dessine [block] découpé dans le rectangle [x0, x1] × [y0, y1] de la figure. */
private inline fun Pencil.clipToRect(x0: Float, y0: Float, x1: Float, y1: Float, crossinline block: () -> Unit) =
    scope.clipRect(ox + x0 * k, oy + y0 * k, ox + x1 * k, oy + y1 * k) { block() }

/** Dégradé le long de [colors], pour t de 0 à 1. */
private fun gradient(colors: List<Color>, t: Float): Color {
    val x = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = floor(x).toInt().coerceAtMost(colors.size - 2)
    return lerp(colors[i], colors[i + 1], x - i)
}

// ------------------------------------------------------------ Cercles

/**
 * Moiré : deux familles de cercles concentriques (ou des cercles et des droites) superposées dans un
 * disque. Les traits sont tirés au compas et à la règle, sans tremblé : le moiré naît de leur régularité.
 */
private fun Pencil.moireRings(v: Int) {
    val d = floatArrayOf(15f, 21f, 12f)[v % 3]
    val pal = listOf(Crayons.rouge, Crayons.marine, Crayons.violet, Crayons.sapin, Crayons.orange, Crayons.bleu)
    // Les deux familles du même crayon : le moiré est une affaire de clair et de sombre.
    val ink = pal[(v / 2) % 6]
    val big = 116f
    val step = 4.6f
    val win = circle(center.x, center.y, big, 220)
    fill(win, Crayons.jaune, hatch = false, wash = 0.16f, outline = false)
    val pen = Stroke(2.1f * k)
    fun rings(c: Offset) {
        var r = step
        while (r < big + abs(c.x - center.x) + abs(c.y - center.y) + step) {
            scope.drawCircle(ink, r * k, px(c), alpha = 0.85f, style = pen)
            r += step
        }
    }
    clipTo(win) {
        if (v % 2 == 0) {
            rings(pt(center.x - d, center.y))
            rings(pt(center.x + d, center.y))
        } else {
            rings(pt(center.x, center.y + d))
            var y = center.y - big
            while (y <= center.y + big) {
                scope.drawLine(ink, px(pt(center.x - big, y)), px(pt(center.x + big, y)), 2.1f * k, alpha = 0.85f)
                y += step
            }
        }
    }
    outline(win, lw = 1.7f)
}

/**
 * Inversion d'un damier par rapport à un cercle : dehors, le damier tel quel ; dedans, l'image des cases
 * du dehors (des quadrilatères à côtés en arcs de cercle, qui rapetissent vers le centre).
 */
private fun Pencil.gridInversion(v: Int) {
    val o = center
    val big = 104f
    val h = 40f
    val shift = if (v % 2 == 0) 0f else h / 2
    val m = 14
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun inv(p: Offset): Offset {
        val d = p - o
        return o + d * (big * big / (d.x * d.x + d.y * d.y))
    }
    val disk = circle(o.x, o.y, big, 220)
    fun corner(i: Int, j: Int) = pt(o.x + shift + i * h, o.y + shift + j * h)
    fun colorOf(i: Int, j: Int): Color? {
        if ((i + j) and 1 != 0) return null
        val c = corner(i, j) + pt(h / 2, h / 2) - o
        val ring = (max(abs(c.x), abs(c.y)) / h).roundToInt()
        return pal[ring % 8]
    }
    // Dehors : les cases elles-mêmes.
    val frame = rect(6f, 6f, FW - 12f, FH - 12f)
    clipTo(frame) {
        clipTo(disk, outside = true) {
            for (i in -m until m) for (j in -m until m) {
                val c = colorOf(i, j) ?: continue
                val a = corner(i, j)
                if (a.x > FW || a.y > FH || a.x + h < 0 || a.y + h < 0) continue
                fill(rect(a.x, a.y, h), c, gap = 4.4f, angle = -58f + 25f * (i - j), wash = 0.3f, lw = 0.9f)
            }
        }
    }
    outline(frame, lw = 1.5f)
    // Dedans : les images des cases du dehors, et celles des droites de la grille.
    clipTo(disk) {
        fill(disk, pal[(v + 2) % 8], hatch = false, wash = 0.08f, outline = false)
        for (i in -m until m) for (j in -m until m) {
            val c = colorOf(i, j) ?: continue
            val a = corner(i, j)
            val far = listOf(a, a + pt(h, 0f), a + pt(h, h), a + pt(0f, h)).maxOf { (it - o).getDistance() }
            if (far <= big) continue
            val near = max(abs(a.x + h / 2 - o.x) - h / 2, abs(a.y + h / 2 - o.y) - h / 2).coerceAtLeast(1f)
            if (big * big * h / (near * near + h * h) < 0.6f) continue
            val sides = listOf(a, a + pt(h, 0f), a + pt(h, h), a + pt(0f, h))
            val poly = (0 until 4).flatMap { s -> List(10) { t -> inv(sides[s] + (sides[(s + 1) % 4] - sides[s]) * (t / 10f)) } }
            fill(poly, c, hatch = false, wash = 0.55f, outline = false)
        }
        val reach = m * h
        for (i in -m..m) {
            val a = shift + i * h
            if (abs(a) < 0.5f) continue
            val n = 160
            val vert = List(n + 1) { t -> inv(pt(o.x + a, o.y - reach + 2 * reach * t / n)) }
            val horiz = List(n + 1) { t -> inv(pt(o.x - reach + 2 * reach * t / n, o.y + a)) }
            line(vert, Graphite, 0.55f, 0.5f)
            line(horiz, Graphite, 0.55f, 0.5f)
        }
    }
    outline(disk, lw = 1.7f)
    dot(o, 2.4f, Graphite)
}

/**
 * Faisceaux d'Apollonius : les cercles PA / PB = k (coloriés, emboîtés autour de A et de B) et les
 * cercles passant par A et B (au crayon), orthogonaux aux premiers.
 */
private fun Pencil.apolloniusPencils(v: Int) {
    val a = if (v % 2 == 0) 60f else 50f
    val o = center
    val warm = listOf(Crayons.rouge, Crayons.orange, Crayons.jaune)
    val cool = listOf(Crayons.marine, Crayons.bleu, Crayons.turquoise)
    val (left, right) = if (v % 2 == 0) warm to cool else listOf(Crayons.violet, Crayons.rose, Crayons.jaune) to listOf(Crayons.sapin, Crayons.vert, Crayons.turquoise)
    clipToRect(6f, 6f, FW - 6f, FH - 6f) {
        for (side in listOf(-1, 1)) for (m in 1..8) {
            val s = 0.3f * m
            val r = a / sinh(s)
            val cx = o.x + side * a * cosh(s) / sinh(s)
            val pal = if (side < 0) left else right
            fill(
                circle(cx, o.y, r, (r * 1.1f).toInt().coerceIn(30, 260)),
                gradient(pal, (m - 1) / 7f),
                hatch = r > 9f,
                gap = (r / 6).coerceIn(3f, 5.5f),
                angle = side * (40f + 12f * m),
                wash = 0.2f,
                strokeAlpha = 0.55f,
                lw = 1f,
            )
        }
        for (j in 1..11) {
            val phi = PI.toFloat() * j / 12
            val r = a / sin(phi)
            line(circle(o.x, o.y + a * cos(phi) / sin(phi), r, (r * 1.1f).toInt().coerceIn(40, 300)), Graphite, 0.9f, 0.6f)
        }
        line(listOf(pt(0f, o.y), pt(FW, o.y)), Graphite, 0.9f, 0.6f)
        line(listOf(pt(o.x, 0f), pt(o.x, FH)), Graphite, 0.9f, 0.6f)
    }
    outline(rect(6f, 6f, FW - 12f, FH - 12f), lw = 1.5f)
    for (p in listOf(pt(o.x - a, o.y), pt(o.x + a, o.y))) fill(circle(p.x, p.y, 3.6f, 14), Graphite, hatch = false, wash = 0.9f, lw = 0.8f)
}

/** Nombres de dents (soleil, satellite) du train planétaire, selon la variante. */
private fun gearTeeth(v: Int): Pair<Int, Int> = listOf(18 to 12, 24 to 9, 15 to 15, 21 to 12)[v % 4]

/**
 * Le contour d'une roue dentée de [z] dents au module [m] (rayon primitif zm/2), la dent n° 0 centrée
 * dans la direction [phase] ; dents tournées vers le centre si [internal] (couronne).
 */
private fun gearRim(c: Offset, z: Int, m: Float, phase: Float, internal: Boolean = false): List<Offset> {
    val r = z * m / 2
    val tip = if (internal) r - m else r + m
    val root = if (internal) r + 1.25f * m else r - 1.25f * m
    val mid = (tip + root) / 2
    val pitch = FULL_TURN / z
    val profile = listOf(-0.5f to root, -0.3f to root, -0.23f to mid, -0.15f to tip, 0f to tip, 0.15f to tip, 0.23f to mid, 0.3f to root)
    return (0 until z).flatMap { t -> profile.map { (f, rr) -> c + dir(phase + (t + f) * pitch) * rr } }
}

/**
 * Train planétaire : un soleil, trois satellites et une couronne à denture intérieure. Les phases des
 * roues sont calculées pour que chaque dent tombe dans un creux de la roue voisine.
 */
private fun Pencil.planetaryGears(v: Int) {
    val (zs, zp) = gearTeeth(v)
    val zr = zs + 2 * zp
    val m = 112f / (zr / 2f + 3.2f)
    val o = center
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fun frac(x: Float) = x - floor(x)
    val sunPhase = rad(7f * v)
    val dist = (zs + zp) * m / 2
    val alphas = List(3) { rad(-90f + 13f * v) + FULL_TURN * it / 3 }
    // Satellite : la somme des décalages (en pas) au point de contact vaut 1/2.
    val planetPhases = alphas.map { al -> al + PI.toFloat() - (0.5f - frac((al - sunPhase) * zs / FULL_TURN)) * FULL_TURN / zp }
    // Couronne : la différence des décalages au contact vaut 1/2.
    val up = (alphas[0] - planetPhases[0]) * zp / FULL_TURN
    val ringPhase = alphas[0] - (up + 0.5f) * FULL_TURN / zr
    // La couronne : un anneau, le bord extérieur puis les dents parcourues à l'envers (un trou).
    val outer = circle(o.x, o.y, zr * m / 2 + 3.2f * m, 200)
    val teeth = gearRim(o, zr, m, ringPhase, internal = true)
    fill(outer + listOf(outer[0]) + teeth.reversed() + listOf(teeth.last(), outer[0]), pal[0], gap = 4.4f, wash = 0.28f, outline = false)
    outline(outer, lw = 1.5f)
    outline(teeth, lw = 1.1f)
    // Le soleil et les satellites.
    fill(gearRim(o, zs, m, sunPhase), pal[2], gap = 3.8f, angle = 30f, lw = 1.1f)
    alphas.forEachIndexed { i, al ->
        val c = o + dir(al) * dist
        fill(gearRim(c, zp, m, planetPhases[i]), pal[(4 + i) % 8], gap = 3.6f, angle = -58f + 40f * i, lw = 1.1f)
        fill(circle(c.x, c.y, m * 1.5f, 18), Crayons.brun, hatch = false, wash = 0.6f, lw = 0.9f)
    }
    // Le porte-satellites, par-dessus.
    val arms = alphas.map { o + dir(it) * dist }
    fill(arms, Graphite, hatch = false, wash = 0.08f, lw = 0.8f, outline = false)
    for (p in arms) line(listOf(o, p), Crayons.brun, 2.2f, 0.75f)
    fill(circle(o.x, o.y, m * 2.2f, 22), Crayons.brun, hatch = false, wash = 0.6f, lw = 1f)
}

/**
 * Sangaku : dans un carré de côté a, deux quarts de cercle de rayon a centrés en deux coins voisins ; le
 * cercle posé sur le côté (rayon 3a/8), celui coincé au-dessus du croisement (a/16), ceux des bords (a/6).
 */
private fun Pencil.sangakuTablet(v: Int) {
    val a = 226f
    val q = v % 4
    // (x, y) en fractions de a, y vers le haut ; le tout tourné d'un quart de tour par variante.
    fun at(x: Float, y: Float): Offset {
        var u = x - 0.5f
        var w = 0.5f - y
        repeat(q) { val t = u; u = -w; w = t }
        return pt(center.x + u * a, center.y + w * a)
    }
    fun disc(cx: Float, cy: Float, r: Float) = List(150) { val t = FULL_TURN * it / 150; at(cx + r * cos(t), cy + r * sin(t)) }
    fun quarter(cx: Float, from: Float, to: Float) = listOf(at(cx, 0f)) + (0..70).map { val t = from + (to - from) * it / 70; at(cx + cos(t), sin(t)) }
    // Deux quarts de cercle qui se mêlent bien, puis le grand cercle, le petit, et ceux des bords.
    val pal = listOf(
        listOf(Crayons.bleu, Crayons.jaune, Crayons.rouge, Crayons.vert, Crayons.violet),
        listOf(Crayons.rose, Crayons.jaune, Crayons.turquoise, Crayons.orange, Crayons.bleu),
        listOf(Crayons.turquoise, Crayons.jaune, Crayons.violet, Crayons.rouge, Crayons.orange),
        listOf(Crayons.bleu, Crayons.rose, Crayons.jaune, Crayons.vert, Crayons.orange),
    )[v % 4]
    val square = listOf(at(0f, 0f), at(1f, 0f), at(1f, 1f), at(0f, 1f))
    fill(square, Crayons.brun, hatch = false, wash = 0.08f, outline = false)
    fill(quarter(0f, 0f, PI.toFloat() / 2), pal[0], hatch = false, wash = 0.22f, lw = 1.2f)
    fill(quarter(1f, PI.toFloat() / 2, PI.toFloat()), pal[1], hatch = false, wash = 0.26f, lw = 1.2f)
    fill(disc(0.5f, 3f / 8, 3f / 8), pal[2], gap = 3.8f, angle = -20f, wash = 0.45f, lw = 1.3f)
    fill(disc(0.5f, 15f / 16, 1f / 16), pal[3], gap = 2.8f, lw = 1.1f)
    val side = sqrt(2f / 3)
    fill(disc(1f / 6, side, 1f / 6), pal[4], gap = 3.2f, angle = 60f, lw = 1.2f)
    fill(disc(5f / 6, side, 1f / 6), pal[4], gap = 3.2f, angle = 120f, lw = 1.2f)
    outline(square, lw = 1.8f)
}

/** Demi-grand axe des cercles osculateurs, et demi-petit axe selon la variante. */
private const val OSC_A = 116f
private fun oscB(v: Int) = floatArrayOf(80f, 88f, 76f)[v % 3]

/**
 * Cercles osculateurs d'une ellipse : le long de deux arcs opposés, de sommet à sommet, des cercles
 * emboîtés (Tait–Kneser) ; la développée (lieu des centres), en étoile, et les normales.
 */
private fun Pencil.osculatingCircles(v: Int) {
    val a = OSC_A
    val b = oscB(v)
    val flip = if (v % 2 == 0) 1f else -1f
    val o = center
    val e = a * a - b * b
    fun at(t: Float) = pt(o.x + a * cos(t), o.y - flip * b * sin(t))
    fun centerOf(t: Float): Offset {
        val c = cos(t); val s = sin(t)
        return pt(o.x + e / a * c * c * c, o.y + flip * e / b * s * s * s)
    }
    fun radius(t: Float): Float {
        val s = sin(t); val c = cos(t)
        return (a * a * s * s + b * b * c * c).pow(1.5f) / (a * b)
    }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val ellipse = List(200) { at(FULL_TURN * it / 200) }
    // Les paramètres : rayons en progression géométrique, de b²/a au plus grand cercle qui tient dans le cadre.
    fun fits(t: Float) = abs(centerOf(t).y - o.y) + radius(t) <= FH / 2 - 6 && abs(centerOf(t).x - o.x) + radius(t) <= FW / 2 - 6
    var tMax = 0f
    while (tMax < PI.toFloat() / 2 && fits(tMax + 0.01f)) tMax += 0.01f
    val r0 = radius(0f)
    val r1 = radius(tMax)
    val count = 7
    val ts = List(count) { i ->
        val target = r0 * (r1 / r0).pow(i / (count - 1f))
        var lo = 0f; var hi = PI.toFloat() / 2
        repeat(40) { val mid = (lo + hi) / 2; if (radius(mid) < target) lo = mid else hi = mid }
        lo
    }
    clipToRect(4f, 4f, FW - 4f, FH - 4f) {
        fill(ellipse, pal[2], hatch = false, wash = 0.1f, outline = false)
        for (base in listOf(0f, PI.toFloat())) for (i in count - 1 downTo 0) {
            val t = base + ts[i]
            val c = centerOf(t)
            val r = radius(t)
            val color = pal[(i + if (base > 0) 4 else 0) % 8]
            val disc = circle(c.x, c.y, r, (r * 1.1f).toInt().coerceIn(30, 220))
            fill(disc, color, hatch = i == 0, gap = 3.6f, wash = 0.12f, strokeAlpha = 0.6f, outline = false)
            outline(disc, lw = 1f, color = lerp(color, Graphite, 0.45f))
        }
        for (base in listOf(0f, PI.toFloat())) for (i in 0 until count) {
            val t = base + ts[i]
            stroke(at(t), centerOf(t), Graphite, 0.6f, 0.45f)
            dot(at(t), 2f, Graphite)
            dot(centerOf(t), 1.6f, Graphite, 0.8f)
        }
        line(List(241) { centerOf(FULL_TURN * it / 240) }, Crayons.rouge, 1.3f, 0.9f)
    }
    outline(ellipse, lw = 1.8f)
}

/** Exposant de décroissance des aires des bulles, selon la variante. */
private fun bubbleExponent(v: Int) = floatArrayOf(1.3f, 1.24f, 1.36f)[v % 3]

private val bubbleCache = HashMap<Int, List<FloatArray>>()

/**
 * Les bulles (x, y, r) de la variante [v] : la n-ième a une aire A·(n + 3)^−c / ζ(c, 3), où A est 80 %
 * du cadre et ζ la fonction zêta de Hurwitz (la somme de toutes les aires vaut donc A) ; chacune est
 * posée au hasard jusqu'à ne chevaucher aucune autre.
 */
private fun bubbles(v: Int): List<FloatArray> = bubbleCache.getOrPut(v) {
    val c = bubbleExponent(v).toDouble()
    val x0 = 10.0; val y0 = 10.0
    val w = FW - 20.0; val h = FH - 20.0
    val n0 = 3.0
    var zeta = 0.0
    for (i in 0 until 20000) zeta += (n0 + i).pow(-c)
    zeta += (n0 + 20000).pow(1 - c) / (c - 1)
    val rnd = Random(77 + v * 31)
    val out = ArrayList<FloatArray>()
    var i = 0
    while (true) {
        val r = sqrt(w * h * 0.8 * (n0 + i).pow(-c) / zeta / PI) // la bulle n = i + 1 : aire en (n + 2)^−c
        if (r < 1.9) break
        for (attempt in 0 until 3000) {
            val x = x0 + r + rnd.nextDouble() * (w - 2 * r)
            val y = y0 + r + rnd.nextDouble() * (h - 2 * r)
            if (out.all { (it[0] - x) * (it[0] - x) + (it[1] - y) * (it[1] - y) >= (it[2] + r + 1.0) * (it[2] + r + 1.0) }) {
                out += floatArrayOf(x.toFloat(), y.toFloat(), r.toFloat())
                break
            }
        }
        i++
    }
    out
}

/** Bulles : des disques semés au hasard sans chevauchement, de plus en plus petits (géométrie statistique de Shier). */
private fun Pencil.seededBubbles(v: Int) {
    val all = bubbles(v)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val rMax = all.first()[2]
    for (b in all) {
        val (x, y, r) = Triple(b[0], b[1], b[2])
        val level = (ln(rMax / r) / ln(1.6f)).toInt()
        fill(
            circle(x, y, r, (r * 1.4f).toInt().coerceIn(16, 120)),
            pal[level % 8],
            hatch = r > 7f,
            gap = (r / 6).coerceIn(2.6f, 4.6f),
            angle = -58f + level * 23f,
            wash = if (r > 7f) 0.3f else 0.55f,
            lw = if (r > 10f) 1.2f else 0.7f,
        )
    }
}

// ------------------------------------------------------------ Nombres

private fun isPrimeNumber(n: Int): Boolean {
    if (n < 2) return false
    var d = 2
    while (d * d <= n) { if (n % d == 0) return false; d++ }
    return true
}

private val sandCache = HashMap<Int, Array<IntArray>>()

/**
 * Tas de sable stabilisé : 16 384 grains sur une case (ou 8 192 sur chacune de deux), puis des
 * éboulements (une case de 4 grains ou plus en donne un à chaque voisine) jusqu'à ce que tout soit stable.
 */
private fun sandpileGrid(two: Boolean): Array<IntArray> = sandCache.getOrPut(if (two) 1 else 0) {
    val w = if (two) 171 else 113
    val h = if (two) 115 else 113
    val g = Array(h) { IntArray(w) }
    if (two) { g[57][57] = 8192; g[57][113] = 8192 } else g[56][56] = 16384
    var busy = true
    while (busy) {
        busy = false
        for (y in 0 until h) for (x in 0 until w) {
            val n = g[y][x]
            if (n < 4) continue
            val q = n / 4
            g[y][x] = n - 4 * q
            if (x > 0) g[y][x - 1] += q
            if (x < w - 1) g[y][x + 1] += q
            if (y > 0) g[y - 1][x] += q
            if (y < h - 1) g[y + 1][x] += q
            busy = true
        }
    }
    g
}

/**
 * Tas de sable abélien : les cases à 0, 1, 2 ou 3 grains en quatre couleurs (les cases vides du dehors
 * restent blanches), par bandes horizontales de même couleur.
 */
private fun Pencil.sandpile(v: Int) {
    val g = sandpileGrid(v % 2 == 1)
    val h = g.size
    val w = g[0].size
    var x0 = Int.MAX_VALUE; var x1 = -1; var y0 = Int.MAX_VALUE; var y1 = -1
    for (y in 0 until h) for (x in 0 until w) if (g[y][x] > 0) { x0 = min(x0, x); x1 = max(x1, x); y0 = min(y0, y); y1 = max(y1, y) }
    // Une case vide est dedans si le tas l'entoure dans les quatre directions.
    val inside = Array(h) { BooleanArray(w) }
    for (y in 0 until h) {
        val l = (0 until w).firstOrNull { g[y][it] > 0 } ?: continue
        val r = (0 until w).last { g[y][it] > 0 }
        for (x in l..r) inside[y][x] = true
    }
    for (x in 0 until w) {
        val t = (0 until h).firstOrNull { g[it][x] > 0 }
        val b = (0 until h).lastOrNull { g[it][x] > 0 }
        for (y in 0 until h) if (t == null || y < t || y > b!!) inside[y][x] = false
    }
    val cols = x1 - x0 + 1
    val rows = y1 - y0 + 1
    val cell = min((FW - 20) / cols, (FH - 16) / rows)
    val left = center.x - cols * cell / 2
    val top = center.y - rows * cell / 2
    val palettes = listOf(
        listOf(Crayons.marine, Crayons.rouge, Crayons.jaune, Crayons.bleu),
        listOf(Crayons.violet, Crayons.orange, Crayons.jaune, Crayons.rose),
        listOf(Crayons.sapin, Crayons.rose, Crayons.jaune, Crayons.vert),
        listOf(Crayons.brun, Crayons.bleu, Crayons.jaune, Crayons.orange),
    )
    val pal = palettes[(v / 2) % palettes.size]
    val wash = floatArrayOf(0.85f, 0.7f, 0.7f, 0.42f)
    for (y in y0..y1) {
        var x = x0
        while (x <= x1) {
            val n = g[y][x]
            val inn = inside[y][x]
            var end = x
            while (end + 1 <= x1 && g[y][end + 1] == n && inside[y][end + 1] == inn) end++
            if (n > 0 || inn) {
                fill(rect(left + (x - x0) * cell, top + (y - y0) * cell, (end - x + 1) * cell, cell), pal[n], hatch = false, wash = wash[n], outline = false)
            }
            x = end + 1
        }
    }
}

/** Courtepointe des PGCD : la case (a, b) coloriée selon pgcd(a, b) ; les couples premiers entre eux, pointés. */
private fun Pencil.gcdQuilt(v: Int) {
    val centered = v % 2 == 0
    val cols = 37
    val rows = 25
    val cell = min((FW - 14) / cols, (FH - 14) / rows)
    val x0 = center.x - cols * cell / 2
    val y0 = center.y - rows * cell / 2
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (i in 0 until cols) for (j in 0 until rows) {
        val a = if (centered) i - cols / 2 else i + 1
        val b = if (centered) rows / 2 - j else rows - j
        val g = gcd(abs(a), abs(b))
        val x = x0 + i * cell
        val y = y0 + j * cell
        when (g) {
            0 -> fill(rect(x + 0.5f, y + 0.5f, cell - 1f), Graphite, hatch = false, wash = 0.85f, outline = false)
            1 -> dot(pt(x + cell / 2, y + cell / 2), 0.9f, Graphite, 0.5f)
            else -> fill(
                rect(x + 0.4f, y + 0.4f, cell - 0.8f),
                pal[(g - 2) % 8],
                hatch = false,
                wash = (0.4f + 0.16f * ln(g.toFloat())).coerceAtMost(0.9f),
                outline = false,
            )
        }
    }
    outline(rect(x0, y0, cols * cell, rows * cell), lw = 1.2f)
}

/** Nombre d'entiers de la toile des diviseurs, selon la variante. */
private fun divisorCount(v: Int) = if (v % 2 == 0) 60 else 48

/**
 * Toile des diviseurs : les entiers sur une échelle logarithmique, un arc de m à pm pour chaque nombre
 * premier p (tous de même taille pour un même p), au-dessus ou au-dessous selon p ; les premiers en jaune.
 */
private fun Pencil.divisorArcs(v: Int) {
    val n = divisorCount(v)
    val x0 = 18f
    val w = FW - 2 * x0
    fun x(i: Int) = x0 + w * ln(i.toFloat()) / ln(n.toFloat())
    val y = center.y
    val primes = (2..n).filter { isPrimeNumber(it) }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (index in primes.indices.reversed()) {
        val p = primes[index]
        val side = if (index % 2 == 0) -1f else 1f
        val half = (x(p) - x(1)) / 2
        val height = min(half, 108f - 2f * index)
        for (m in 1..n / p) {
            val a = x(m)
            val arc = List(41) { t -> val th = PI.toFloat() * t / 40; pt(a + half - half * cos(th), y + side * height * sin(th)) }
            line(arc, pal[index % 8], if (index < 4) 1.3f else 1f, 0.8f)
        }
    }
    line(listOf(pt(x0 - 8, y), pt(FW - x0 + 8, y)), Graphite, 0.8f, 0.6f)
    for (i in 1..n) {
        val c = pt(x(i), y)
        // Les points se serrent vers la droite : leur taille suit l'écart avec le voisin.
        val room = (x(i + 1) - x(i)) * 0.45f
        when {
            i == 1 -> fill(circle(c.x, c.y, 4.4f, 16), Graphite, hatch = false, wash = 0.8f, lw = 0.9f)
            isPrimeNumber(i) -> fill(circle(c.x, c.y, min(3.2f, max(1.3f, room)), 14), Crayons.jaune, hatch = false, wash = 0.95f, lw = 0.7f)
            else -> dot(c, min(1.7f, max(0.8f, room)), Graphite, 0.85f)
        }
    }
}

/** Paramètres du corail : nombres de départ, et (virage des pairs, virage des impairs, raccourcissement du pas). */
private fun collatzParams(v: Int): Pair<Int, FloatArray> =
    if (v % 2 == 0) 2000 to floatArrayOf(0.15f, 0.30f, 1f) else 1500 to floatArrayOf(0.12f, 0.26f, 0.996f)

private val collatzCache = HashMap<Int, List<Pair<List<Offset>, Int>>>()

/**
 * Le corail de Collatz de la variante [v] : chaque nombre n est placé au bout de son père f(n) (n/2 ou
 * 3n + 1), après un virage à gauche s'il est pair, à droite s'il est impair. L'arbre est découpé en
 * brins (en suivant toujours l'enfant le plus fréquenté), chacun avec le nombre de trajectoires qui y
 * passent ; le tout tourné pour remplir au mieux le cadre.
 */
private fun collatzBranches(v: Int): List<Pair<List<Offset>, Int>> = collatzCache.getOrPut(v % 2) {
    val (starts, p) = collatzParams(v)
    val turnEven = p[0].toDouble()
    val turnOdd = p[1].toDouble()
    val decay = p[2].toDouble()
    // Position, cap et longueur du pas de chaque nombre visité.
    val px = HashMap<Long, Double>()
    val py = HashMap<Long, Double>()
    val head = HashMap<Long, Double>()
    val step = HashMap<Long, Double>()
    val count = HashMap<Long, Int>()
    val children = HashMap<Long, MutableList<Long>>()
    px[1L] = 0.0; py[1L] = 0.0; head[1L] = -PI / 2; step[1L] = 1.0
    for (s in 1..starts) {
        val traj = mutableListOf(s.toLong())
        while (traj.last() != 1L && traj.last() !in px) {
            val n = traj.last()
            traj += if (n % 2 == 0L) n / 2 else 3 * n + 1
        }
        // Les comptes : toute la trajectoire, jusqu'à 1.
        var n = s.toLong()
        while (true) {
            count[n] = (count[n] ?: 0) + 1
            if (n == 1L) break
            n = if (n % 2 == 0L) n / 2 else 3 * n + 1
        }
        for (i in traj.size - 2 downTo 0) {
            val m = traj[i]
            val f = traj[i + 1]
            val h = head[f]!! + if (m % 2 == 0L) turnEven else -turnOdd
            val l = step[f]!!
            px[m] = px[f]!! + l * kotlin.math.cos(h)
            py[m] = py[f]!! + l * kotlin.math.sin(h)
            head[m] = h
            step[m] = l * decay
            children.getOrPut(f) { mutableListOf() } += m
        }
    }
    // La rotation qui laisse la plus grande échelle dans le cadre.
    var bestAngle = 0.0
    var bestScale = 0.0
    for (r in 0 until 72) {
        val a = PI * r / 72
        val ca = kotlin.math.cos(a)
        val sa = kotlin.math.sin(a)
        var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE; var minY = Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
        for (key in px.keys) {
            val x = px[key]!! * ca - py[key]!! * sa
            val y = px[key]!! * sa + py[key]!! * ca
            minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
        }
        val sc = min((FW - 24) / (maxX - minX), (FH - 24) / (maxY - minY))
        if (sc > bestScale) { bestScale = sc; bestAngle = a }
    }
    val ca = kotlin.math.cos(bestAngle)
    val sa = kotlin.math.sin(bestAngle)
    fun at(n: Long) = pt((px[n]!! * ca - py[n]!! * sa).toFloat(), (px[n]!! * sa + py[n]!! * ca).toFloat())
    fun level(n: Long) = 31 - Integer.numberOfLeadingZeros(count[n] ?: 1)
    // Les brins : on suit l'enfant le plus fréquenté ; on coupe quand l'épaisseur (log₂ du compte) change.
    val pieces = ArrayList<Pair<List<Long>, Int>>()
    val todo = ArrayDeque<Pair<Long, Long>>()
    todo.addLast(1L to 1L)
    while (todo.isNotEmpty()) {
        val (from, first) = todo.removeFirst()
        var node = first
        if (first == from) {
            val kids = children[from].orEmpty()
            if (kids.isEmpty()) continue
            node = kids.maxBy { count[it] ?: 0 }
            for (kid in kids) if (kid != node) todo.addLast(from to kid)
        }
        var cur = mutableListOf(from)
        var lv = level(node)
        while (true) {
            if (level(node) != lv) {
                pieces += cur to (1 shl lv)
                cur = mutableListOf(cur.last())
                lv = level(node)
            }
            cur += node
            val kids = children[node].orEmpty()
            if (kids.isEmpty()) break
            val heavy = kids.maxBy { count[it] ?: 0 }
            for (kid in kids) if (kid != heavy) todo.addLast(node to kid)
            node = heavy
        }
        pieces += cur to (1 shl lv)
    }
    val polys = pieces.map { (nodes, c) -> nodes.map { at(it) } to c }
    val fit = Fit(polys.map { it.first }, 12f)
    polys.map { (pts, c) -> fit(pts) to c }.sortedBy { it.second }
}

/** Corail de Collatz : l'arbre des trajectoires de Syracuse, épais et sombre au tronc, clair aux pointes. */
private fun Pencil.collatzCoral(v: Int) {
    val pieces = collatzBranches(v)
    val maxLog = ln(pieces.maxOf { it.second }.toFloat())
    val pal = listOf(
        listOf(Crayons.marine, Crayons.violet, Crayons.rose, Crayons.orange, Crayons.jaune),
        listOf(Crayons.brun, Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert),
        listOf(Crayons.sapin, Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose),
    )[v % 3]
    for ((pts, c) in pieces) {
        val t = ln(c.toFloat()) / maxLog
        line(pts, gradient(pal, 1f - t), 0.5f + 2.8f * t, 0.5f + 0.45f * t)
    }
}

/** Le carré magique de la variante : Dürer (4), méthode siamoise (5), ou ordre 8 par diagonales. */
private fun magicSquare(v: Int): Array<IntArray> = when (v % 3) {
    0 -> arrayOf(intArrayOf(16, 3, 2, 13), intArrayOf(5, 10, 11, 8), intArrayOf(9, 6, 7, 12), intArrayOf(4, 15, 14, 1))
    1 -> {
        val n = 5
        val a = Array(n) { IntArray(n) }
        var r = 0
        var c = n / 2
        for (k in 1..n * n) {
            a[r][c] = k
            val nr = (r - 1 + n) % n
            val nc = (c + 1) % n
            if (a[nr][nc] != 0) r = (r + 1) % n else { r = nr; c = nc }
        }
        a
    }
    else -> Array(8) { i -> IntArray(8) { j -> val k = i * 8 + j + 1; if (i % 4 == j % 4 || i % 4 + j % 4 == 3) 65 - k else k } }
}

/** Lignes magiques : les cases teintées selon leur nombre, et le trait qui relie 1, 2, 3… dans l'ordre. */
private fun Pencil.magicLines(v: Int) {
    val sq = magicSquare(v)
    val n = sq.size
    val side = 224f
    val cell = side / n
    val x0 = center.x - side / 2
    val y0 = center.y - side / 2
    val pal = listOf(
        listOf(Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.violet, Crayons.marine),
        listOf(Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet),
        listOf(Crayons.rose, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.sapin),
    )[(v / 3) % 3]
    val last = n * n - 1f
    val where = arrayOfNulls<Offset>(n * n + 1)
    for (i in 0 until n) for (j in 0 until n) {
        val k = sq[i][j]
        where[k] = pt(x0 + (j + 0.5f) * cell, y0 + (i + 0.5f) * cell)
        fill(rect(x0 + j * cell, y0 + i * cell, cell), gradient(pal, (k - 1) / last), hatch = false, wash = 0.2f + 0.25f * (k - 1) / last, lw = 0.7f, strokeAlpha = 0.5f)
    }
    for (k in 1 until n * n) {
        line(listOf(where[k]!!, where[k + 1]!!), lerp(gradient(pal, (k - 0.5f) / last), Graphite, 0.45f), 2.2f, 0.9f)
    }
    val s = where[1]!!
    outline(circle(s.x, s.y, cell * 0.16f, 18), lw = 1.4f)
    val e = where[n * n]!!
    fill(circle(e.x, e.y, cell * 0.12f, 16), Graphite, hatch = false, wash = 0.9f, lw = 1f)
    outline(rect(x0, y0, side), lw = 1.6f)
}

/** Spirale de Sacks : n à la distance √n du centre et à l'angle 2π√n ; les nombres premiers pointés. */
private fun Pencil.sacksSpiral(v: Int) {
    val n = 10000
    val big = 118f
    val s = big / sqrt(n.toFloat())
    val sieve = BooleanArray(n + 1) { it >= 2 }
    for (i in 2..100) if (sieve[i]) { var j = i * i; while (j <= n) { sieve[j] = false; j += i } }
    val euler = HashSet<Int>()
    var e = 0
    while (e * e + e + 41 <= n) { euler += e * e + e + 41; e++ }
    val disk = circle(center.x, center.y, big + 4, 200)
    fill(disk, if (v % 2 == 0) Crayons.jaune else Crayons.turquoise, hatch = false, wash = 0.12f, outline = false)
    line(listOf(center, pt(center.x + big + 4, center.y)), Graphite, 0.7f, 0.4f)
    val warm = Crayons.rouge
    val cool = if (v % 2 == 0) Crayons.marine else Crayons.violet
    for (p in 2..n) {
        if (!sieve[p]) continue
        val r = s * sqrt(p.toFloat())
        val th = FULL_TURN * sqrt(p.toFloat())
        val c = pt(center.x + r * cos(th), center.y - r * sin(th))
        if (v % 2 == 0) {
            dot(c, 1.25f, if (p % 4 == 1) warm else cool, 0.85f)
        } else if (p in euler) {
            dot(c, 1.8f, warm, 0.95f)
        } else {
            dot(c, 1.1f, cool, 0.6f)
        }
    }
    fill(circle(center.x, center.y, 2.6f, 12), Crayons.jaune, hatch = false, wash = 0.95f, lw = 0.8f)
    outline(disk, lw = 1.4f)
}

/** Nombre de côtés du triangle [t] (sommets de l'hexagone 0 à 5) pris sur l'hexagone. */
private fun hexSides(t: IntArray) = listOf(t[0] to t[1], t[1] to t[2], t[0] to t[2]).count { (a, b) -> abs(a - b) == 1 || abs(a - b) == 5 }

/** Les 14 triangulations de l'hexagone, chacune une liste de 4 triangles, rangées pour la planche. */
private val hexagonTriangulations: List<List<IntArray>> by lazy {
    fun tri(i: Int, j: Int): List<List<IntArray>> {
        if (j - i < 2) return listOf(emptyList())
        val out = ArrayList<List<IntArray>>()
        for (m in i + 1 until j) for (a in tri(i, m)) for (b in tri(m, j)) out += a + b + listOf(intArrayOf(i, m, j))
        return out
    }
    val all = tri(0, 5)
    val central = all.filter { ts -> ts.any { hexSides(it) == 0 } }
    val fans = all.filter { ts -> ts !in central && (0..5).any { s -> ts.all { s in it } } }
    val zigzags = all.filter { it !in central && it !in fans }
    // Rangées de 5, 4 et 5 : zigzags et éventails alternés, les deux à triangle central au milieu.
    listOf(zigzags[0], fans[0], zigzags[1], fans[1], zigzags[2], fans[2], central[0], central[1], fans[3], zigzags[3], fans[4], zigzags[4], fans[5], zigzags[5])
}

/** Nombres de Catalan : les 14 triangulations de l'hexagone, en rangées de 5, 4 et 5. */
private fun Pencil.hexTriangulations(v: Int) {
    val r = 31f
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val colors = listOf(pal[5], pal[2], pal[0])
    val turn = if (v % 2 == 0) -90f else 0f
    var index = 0
    listOf(5, 4, 5).forEachIndexed { row, count ->
        for (c in 0 until count) {
            val cx = center.x + (c - (count - 1) / 2f) * 70f
            val cy = center.y + (row - 1) * 76f
            val corners = List(6) { pt(cx, cy) + dir(rad(turn + 60f * it)) * r }
            for (t in hexagonTriangulations[index]) {
                val sides = hexSides(t)
                fill(t.map { corners[it] }, colors[2 - sides], gap = 3.4f, angle = -58f + 40f * sides, wash = 0.32f, lw = 1f)
            }
            outline(corners, lw = 1.4f)
            for (p in corners) dot(p, 1.8f, Graphite)
            index++
        }
    }
}

/**
 * Somme des cubes : le carré de côté T = 1 + 2 + … + n découpé en équerres ; la k-ième, de largeur k,
 * faite de k carrés k × k (deux moitiés pour k pair), vaut k³.
 */
private fun Pencil.cubeSum(v: Int) {
    val n = 5 + v % 3
    val total = n * (n + 1) / 2f
    val side = 226f
    val u = side / total
    val x0 = center.x - side / 2
    val yb = center.y + side / 2
    fun at(x: Float, y: Float) = pt(x0 + x * u, yb - y * u)
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    for (k in 1..n) {
        val a = (k - 1) * k / 2f
        val b = k * (k + 1) / 2f
        val gnomon = listOf(at(0f, a), at(0f, b), at(b, b), at(b, 0f), at(a, 0f), at(a, a))
        fill(gnomon, pal[(k - 1) % 8], gap = 3.6f, angle = -58f + 37f * k, wash = 0.34f, lw = 1.3f)
        // Les carrés k × k de l'équerre : celui du coin, puis le long des deux bras, depuis les axes.
        stroke(at(a, a), at(a, b), Graphite, 0.8f, 0.7f)
        stroke(at(a, a), at(b, a), Graphite, 0.8f, 0.7f)
        var t = k.toFloat()
        while (t < a - 0.01f) {
            stroke(at(a, t), at(b, t), Graphite, 0.8f, 0.7f)
            stroke(at(t, a), at(t, b), Graphite, 0.8f, 0.7f)
            t += k
        }
    }
    // Le quadrillage des unités : T² petits carrés en tout.
    for (i in 1 until total.toInt()) {
        stroke(at(i.toFloat(), 0f), at(i.toFloat(), total), Graphite, 0.4f, 0.16f)
        stroke(at(0f, i.toFloat()), at(total, i.toFloat()), Graphite, 0.4f, 0.16f)
    }
    outline(listOf(at(0f, 0f), at(total, 0f), at(total, total), at(0f, total)), lw = 1.7f)
}

// ------------------------------------------------------------ Graphes

/**
 * Colorie les sommets (ou les arêtes, vues comme sommets de leur graphe adjacent) avec [colors]
 * couleurs, par retour arrière : [conflicts] donne, pour chaque élément, ceux qui doivent différer.
 */
private fun backtrackColoring(count: Int, colors: Int, conflicts: List<List<Int>>): IntArray? {
    val c = IntArray(count) { -1 }
    fun go(i: Int): Boolean {
        if (i == count) return true
        for (k in 0 until colors) {
            if (conflicts[i].any { c[it] == k }) continue
            c[i] = k
            if (go(i + 1)) return true
        }
        c[i] = -1
        return false
    }
    return if (go(0)) c else null
}

/** Les arêtes du graphe de Petersen : pentagone extérieur (0 à 4), rayons, pentagramme intérieur (5 à 9). */
private val petersenEdges: List<Pair<Int, Int>> = (0 until 5).flatMap { i -> listOf(i to (i + 1) % 5, i to 5 + i, 5 + i to 5 + (i + 2) % 5) }

/**
 * Graphe de Petersen en étoile : sommets coloriés en trois couleurs (variante paire), ou arêtes
 * coloriées en quatre, deux arêtes d'un même sommet différant toujours (variante impaire).
 */
private fun Pencil.petersenGraph(v: Int) {
    val o = center + pt(0f, 6f)
    val turn = rad(-90f)
    val pos = List(10) { i -> o + dir(turn + FULL_TURN * (i % 5) / 5) * (if (i < 5) 114f else 52f) }
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    fill(pos.subList(0, 5), pal[2], hatch = false, wash = 0.12f, outline = false)
    fill(listOf(pos[5], pos[7], pos[9], pos[6], pos[8]), pal[5], wash = 0.16f, gap = 5f, strokeAlpha = 0.4f, outline = false)
    if (v % 2 == 0) {
        val kinds = listOf(Crayons.marine, Crayons.brun, Crayons.violet)
        petersenEdges.forEachIndexed { e, (a, b) -> line(listOf(pos[a], pos[b]), kinds[e % 3], 2.4f, 0.85f) }
        val adj = List(10) { i -> petersenEdges.filter { it.first == i || it.second == i }.map { if (it.first == i) it.second else it.first }.filter { it < i } }
        val col = backtrackColoring(10, 3, adj)!!
        val vcol = listOf(pal[0], pal[3], pal[6])
        for (i in 0 until 10) fill(circle(pos[i].x, pos[i].y, 8f, 22), vcol[col[i]], gap = 2.6f, wash = 0.7f, lw = 1.2f)
    } else {
        val conflicts = petersenEdges.indices.map { e ->
            val (a, b) = petersenEdges[e]
            (0 until e).filter { f -> val (c, d) = petersenEdges[f]; a == c || a == d || b == c || b == d }
        }
        val col = backtrackColoring(petersenEdges.size, 4, conflicts)!!
        val ecol = listOf(pal[0], pal[3], pal[6], Graphite)
        petersenEdges.forEachIndexed { e, (a, b) -> line(listOf(pos[a], pos[b]), ecol[col[e]], if (col[e] == 3) 3.2f else 2.6f, 0.9f) }
        for (i in 0 until 10) fill(circle(pos[i].x, pos[i].y, 6.5f, 20), Crayons.jaune, hatch = false, wash = 0.9f, lw = 1.2f)
    }
}

/**
 * Graphe de Heawood : en anneau à 14 sommets avec les cordes i → i + 5 (variante paire), ou en graphe
 * d'incidence du plan de Fano, les points dehors et les droites {j, j + 1, j + 3} dedans (variante impaire).
 * Les arêtes sont en trois couleurs, deux arêtes d'un même sommet différant toujours.
 */
private fun Pencil.heawoodGraph(v: Int) {
    val o = center
    val pal = Crayons.rainbow.rot((v * 3) % 8)
    val edgeColors = listOf(pal[0], pal[3], pal[5])
    val pointColor = Crayons.jaune
    val lineColor = Crayons.violet
    if (v % 2 == 0) {
        val pos = List(14) { o + dir(rad(-90f) + FULL_TURN * it / 14) * 114f }
        fill(pos, pal[2], hatch = false, wash = 0.1f, outline = false)
        for (i in 0 until 14 step 2) line(listOf(pos[i], pos[(i + 5) % 14]), edgeColors[2], 2.4f, 0.85f)
        for (i in 0 until 14) line(listOf(pos[i], pos[(i + 1) % 14]), edgeColors[i % 2], 2.6f, 0.9f)
        for (i in 0 until 14) fill(circle(pos[i].x, pos[i].y, 7f, 20), if (i % 2 == 0) pointColor else lineColor, gap = 2.6f, wash = 0.75f, lw = 1.2f)
    } else {
        val points = List(7) { o + dir(rad(-90f) + FULL_TURN * it / 7) * 114f }
        val lines = List(7) { o + dir(rad(-90f) + FULL_TURN * (it + 4f / 3) / 7) * 50f }
        fill(points, pal[2], hatch = false, wash = 0.1f, outline = false)
        fill(lines, pal[6], wash = 0.14f, gap = 5f, strokeAlpha = 0.4f, outline = false)
        for (j in 0 until 7) listOf(0, 1, 3).forEachIndexed { d, off ->
            line(listOf(lines[j], points[(j + off) % 7]), edgeColors[d], 2.4f, 0.88f)
        }
        for (p in points) fill(circle(p.x, p.y, 7.5f, 20), pointColor, gap = 2.6f, wash = 0.75f, lw = 1.2f)
        for (p in lines) fill(circle(p.x, p.y, 6.5f, 20), lineColor, gap = 2.6f, wash = 0.75f, lw = 1.2f)
    }
}

/** Arrondit un polygone fermé par la méthode de Chaikin ([n] passes). */
private fun smooth(pts: List<Offset>, n: Int = 2): List<Offset> {
    var p = pts
    repeat(n) {
        p = p.indices.flatMap { i ->
            val a = p[i]
            val b = p[(i + 1) % p.size]
            listOf(a * 0.75f + b * 0.25f, a * 0.25f + b * 0.75f)
        }
    }
    return p
}

/** Une planche de pont de [a] à [b], de demi-largeur [w]. */
private fun plank(a: Offset, b: Offset, w: Float): List<Offset> {
    val d = b - a
    val n = pt(-d.y, d.x) * (w / d.getDistance())
    return listOf(a + n, b + n, b - n, a - n)
}

/** Courbe de [a] à [b] passant par [m] (une parabole, de sommet près de [m]). */
private fun through(a: Offset, m: Offset, b: Offset): List<Offset> {
    val c = m * 2f - (a + b) * 0.5f
    return List(25) { i -> val t = i / 24f; a * ((1 - t) * (1 - t)) + c * (2 * t * (1 - t)) + b * (t * t) }
}

/**
 * Ponts de Königsberg : la Pregel, l'île du Kneiphof, la terre de l'est entre les deux bras, les sept
 * ponts ; par-dessus, le graphe d'Euler (variante paire) ou un promeneur coincé après six ponts.
 */
private fun Pencil.konigsbergBridges(v: Int) {
    val pal = listOf(Crayons.rouge, Crayons.violet, Crayons.orange, Crayons.rose)
    val accent = pal[(v / 2) % pal.size]
    // L'eau : un bras autour de l'île, qui se partage à l'est autour de la terre D.
    val water = smooth(
        listOf(
            pt(-30f, 76f), pt(60f, 70f), pt(130f, 77f), pt(200f, 70f), pt(232f, 70f), pt(268f, 56f), pt(310f, 44f), pt(400f, 26f),
            pt(400f, 74f), pt(322f, 90f), pt(280f, 108f), pt(252f, 125f), pt(280f, 142f), pt(322f, 160f), pt(400f, 176f),
            pt(400f, 224f), pt(310f, 206f), pt(268f, 194f), pt(232f, 180f), pt(200f, 180f), pt(130f, 173f), pt(60f, 180f), pt(-30f, 174f),
        ),
    )
    val island = List(90) { val t = FULL_TURN * it / 90; pt(138f + 64f * cos(t), 125f + 27f * sin(t)) }
    val frame = rect(6f, 6f, FW - 12f, FH - 12f)
    clipTo(frame) {
        fill(frame, Crayons.vert, wash = 0.12f, gap = 7f, strokeAlpha = 0.25f, outline = false)
        fill(water, Crayons.bleu, wash = 0.3f, gap = 3.6f, angle = 12f, strokeAlpha = 0.55f, lw = 1.1f)
        fill(island, Crayons.jaune, wash = 0.35f, gap = 4.6f, angle = -40f, lw = 1.2f)
    }
    outline(frame, lw = 1.5f)
    // Les ponts : a, b vers le nord, c, d vers le sud, e vers l'est, f et g sur les bras.
    fun islandEdge(x: Float, side: Float) = pt(x, 125f + side * 27f * sqrt(1 - ((x - 138f) / 64f) * ((x - 138f) / 64f)))
    val bridges = listOf(
        islandEdge(108f, -1f) to pt(108f, 68f), islandEdge(168f, -1f) to pt(168f, 68f),
        islandEdge(108f, 1f) to pt(108f, 182f), islandEdge(168f, 1f) to pt(168f, 182f),
        pt(200f, 125f) to pt(258f, 125f),
        pt(296f, 47f) to pt(306f, 99f), pt(296f, 203f) to pt(306f, 151f),
    )
    val unused = if (v % 2 == 1) 4 else -1
    bridges.forEachIndexed { i, (a, b) ->
        val ext = (b - a) * (3f / (b - a).getDistance())
        fill(plank(a - ext, b + ext, 4.2f), if (i == unused) accent else Crayons.brun, hatch = false, wash = 0.75f, lw = 1f)
    }
    // Le graphe : N, S, l'île K, la terre de l'est D.
    val north = pt(138f, 30f)
    val south = pt(138f, 220f)
    val isle = pt(138f, 125f)
    val east = pt(334f, 125f)
    fun mid(i: Int) = (bridges[i].first + bridges[i].second) * 0.5f
    val ends = listOf(isle to north, isle to north, isle to south, isle to south, isle to east, north to east, south to east)
    if (v % 2 == 0) {
        ends.forEachIndexed { i, (a, b) -> line(through(a, mid(i), b), accent, 2.2f, 0.9f) }
    } else {
        // Le promeneur : N → K → S → K → N → D → S, puis plus de pont pour lui.
        val walk = listOf(0 to (north to isle), 2 to (isle to south), 3 to (south to isle), 1 to (isle to north), 5 to (north to east), 6 to (east to south))
        for ((i, ab) in walk) {
            val pts = through(ab.first, mid(i), ab.second)
            for (j in 0 until pts.size - 1 step 2) stroke(pts[j], pts[j + 1], Graphite, 1.6f, 0.85f)
        }
    }
    for (p in listOf(north, south, isle, east)) fill(circle(p.x, p.y, 7f, 20), if (p == isle) Crayons.orange else accent, gap = 2.6f, wash = 0.8f, lw = 1.2f)
}

/** Nombres de pays des trois anneaux de la carte, selon la variante. */
private fun mapRings(v: Int): Triple<Int, Int, Int> = listOf(Triple(5, 8, 11), Triple(5, 7, 10), Triple(5, 9, 12))[v % 3]

private val mapCache = HashMap<Int, Pair<List<List<Offset>>, IntArray>>()

/**
 * La carte de la variante [v] : un pays central et trois anneaux de pays, images de rectangles (u, w)
 * par une application lisse du plan (frontières ondulées, rayons tordus) ; deux pays d'anneaux voisins
 * se touchent si leurs secteurs se chevauchent. Puis quatre couleurs, par retour arrière.
 */
private fun fourColorRegions(v: Int): Pair<List<List<Offset>>, IntArray> = mapCache.getOrPut(v % 12) {
    val counts = mapRings(v).toList()
    val rnd = Random(500 + v % 12)
    val ph = List(4) { rnd.nextFloat() * FULL_TURN }
    val offsets = List(3) { rnd.nextFloat() * FULL_TURN }
    fun map(u: Float, w: Float): Offset {
        val base = if (u <= 1f) 30f * u else 30f + (u - 1f) * 26.7f
        val bump = min(u, 1f) * (0.07f * sin(3 * w + ph[0] + 1.1f * u) + 0.04f * sin(5 * w + ph[1] - 0.7f * u))
        val th = w + 0.24f * sin(1.3f * u + ph[2]) + 0.06f * sin(2.9f * u + ph[3])
        val r = base * (1 + bump)
        return pt(FW / 2 + 1.45f * r * cos(th), FH / 2 + r * sin(th))
    }
    val polys = ArrayList<List<Offset>>()
    val sectors = ArrayList<Triple<Int, Float, Float>>()
    polys += List(90) { map(1f, FULL_TURN * it / 90) }
    sectors += Triple(0, 0f, FULL_TURN)
    for (ring in 1..3) {
        val n = counts[ring - 1]
        for (s in 0 until n) {
            val a = offsets[ring - 1] + FULL_TURN * s / n
            val b = a + FULL_TURN / n
            val m = max(6, ((b - a) * 30).toInt())
            val inner = List(m + 1) { map(ring.toFloat(), a + (b - a) * it / m) }
            val right = List(7) { map(ring + it / 6f, b) }
            val outer = List(m + 1) { map(ring + 1f, b - (b - a) * it / m) }
            val left = List(7) { map(ring + 1 - it / 6f, a) }
            polys += inner + right + outer + left
            sectors += Triple(ring, a, b)
        }
    }
    fun overlap(a0: Float, a1: Float, b0: Float, b1: Float) =
        listOf(-FULL_TURN, 0f, FULL_TURN).sumOf { s -> max(0f, min(a1, b1 + s) - max(a0, b0 + s)).toDouble() } > 1e-3
    val conflicts = sectors.indices.map { i ->
        (0 until i).filter { j ->
            val (ri, ai, bi) = sectors[i]
            val (rj, aj, bj) = sectors[j]
            when {
                rj == 0 -> ri == 1
                ri == rj -> abs(bi - aj) < 1e-3 || abs(bj - ai) < 1e-3 || abs(bi - FULL_TURN - aj) < 1e-3 || abs(bj - FULL_TURN - ai) < 1e-3
                abs(ri - rj) == 1 -> overlap(ai, bi, aj, bj)
                else -> false
            }
        }
    }
    polys to backtrackColoring(polys.size, 4, conflicts)!!
}

/** Carte à quatre couleurs : un pays central, trois anneaux de pays aux frontières sinueuses, quatre crayons. */
private fun Pencil.fourColorMap(v: Int) {
    val (polys, colors) = fourColorRegions(v)
    val pal = listOf(Crayons.rouge, Crayons.jaune, Crayons.bleu, Crayons.vert).rot(v % 4)
    polys.forEachIndexed { i, p ->
        fill(p, pal[colors[i]], gap = 4f, angle = -58f + 31f * i, wash = 0.34f, lw = 1.1f)
    }
    // La côte : le bord extérieur des pays du dernier anneau, repassé plus fort.
    for (p in polys.takeLast(mapRings(v).third)) {
        // Le polygone : bord intérieur (m + 1 points), côté (7), bord extérieur (m + 1), côté (7).
        val m = (p.size - 16) / 2
        outline(p.subList(m + 8, 2 * m + 9), close = false, lw = 2f)
    }
}

/** Cases de départ du tour du cavalier (0 = a8, 63 = h1), selon la variante. */
private val KNIGHT_STARTS = intArrayOf(0, 18, 9, 27, 1, 10, 2, 19)

private val knightCache = HashMap<Int, IntArray>()

/**
 * Un tour fermé du cavalier depuis la case de la variante : règle de Warnsdorff (aller vers la case
 * qui a le moins de sorties libres), les ex æquo tirés au sort ; on recommence jusqu'à ce que le tour
 * passe par les 64 cases et que la dernière soit à un saut de la première, ce qu'on vérifie.
 */
private fun knightPath(v: Int): IntArray = knightCache.getOrPut(v % KNIGHT_STARTS.size) {
    val start = KNIGHT_STARTS[v % KNIGHT_STARTS.size]
    val jumps = listOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
    fun next(s: Int) = jumps.mapNotNull { (dx, dy) ->
        val x = s % 8 + dx
        val y = s / 8 + dy
        if (x in 0..7 && y in 0..7) y * 8 + x else null
    }
    fun jump(a: Int, b: Int) = next(a).contains(b)
    var best: IntArray? = null
    for (attempt in 0 until 4000) {
        val rnd = Random(v * 1009 + attempt)
        val seen = BooleanArray(64)
        val path = IntArray(64)
        path[0] = start
        seen[start] = true
        var len = 1
        while (len < 64) {
            val free = next(path[len - 1]).filter { !seen[it] }
            if (free.isEmpty()) break
            val degree = free.map { s -> next(s).count { !seen[it] } }
            val low = degree.min()
            val ties = free.filterIndexed { i, _ -> degree[i] == low }
            val s = ties[rnd.nextInt(ties.size)]
            path[len++] = s
            seen[s] = true
        }
        if (len < 64) continue
        val valid = (1 until 64).all { jump(path[it - 1], path[it]) } && path.toSet().size == 64
        if (!valid) continue
        if (best == null) best = path
        if (jump(path[63], path[0])) return@getOrPut path
    }
    best!!
}

/** Tour du cavalier, brodé : l'échiquier pâle, le fil qui change de couleur le long des 64 sauts, et ses points. */
private fun Pencil.knightTour(v: Int) {
    val path = knightPath(v)
    val cell = 28f
    val x0 = center.x - 4 * cell
    val y0 = center.y - 4 * cell
    fun at(s: Int) = pt(x0 + (s % 8 + 0.5f) * cell, y0 + (s / 8 + 0.5f) * cell)
    for (s in 0 until 64) {
        val dark = (s % 8 + s / 8) % 2 == 1
        fill(rect(x0 + s % 8 * cell, y0 + s / 8 * cell, cell), Crayons.brun, hatch = false, wash = if (dark) 0.16f else 0.04f, outline = false)
    }
    outline(rect(x0, y0, 8 * cell), lw = 1.6f)
    val pal = listOf(
        listOf(Crayons.rouge, Crayons.orange, Crayons.jaune, Crayons.vert, Crayons.turquoise, Crayons.bleu, Crayons.violet, Crayons.rose, Crayons.rouge),
        listOf(Crayons.marine, Crayons.turquoise, Crayons.vert, Crayons.jaune, Crayons.orange, Crayons.rouge, Crayons.violet, Crayons.marine),
    )[v % 2]
    val dx = abs(path[63] % 8 - path[0] % 8)
    val dy = abs(path[63] / 8 - path[0] / 8)
    val steps = if (dx * dy == 2) 64 else 63
    for (i in 0 until steps) {
        val a = at(path[i])
        val b = at(path[(i + 1) % 64])
        line(listOf(a, b), gradient(pal, i / steps.toFloat()), 2.1f, 0.88f)
    }
    for (s in path) dot(at(s), 2.2f, Graphite, 0.85f)
    val start = at(path[0])
    outline(circle(start.x, start.y, 6f, 18), lw = 1.4f)
}
