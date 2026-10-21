package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.DiscStyle
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.MathBuilder
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.PaperText
import fr.perso.nimbus.ui.paper.SlotDisc
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.color
import fr.perso.nimbus.ui.paper.tex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Page 5 : la courbe z(t) = 20 e^{it} − 6 e^{−it} − 3 e^{3it} − 2 e^{−3it}, tracée par une chaîne
// d'épicycles (un cercle par terme). Les 27 applis la parcourent, l'appli j avec 2jπ/27 d'avance
// sur l'appli 0 ; une période (t de 0 à 2π) dure 8 minutes.

/** Un terme c·e^{ikt} de z. */
private class Term(val c: Float, val k: Int)

private val Terms = listOf(Term(20f, 1), Term(-6f, -1), Term(-3f, 3), Term(-2f, -3))

// Repère de la figure : z = 0 au point (206, 478) de la page, 9 unités par unité de z, l'axe des y
// vers le haut (la maquette trace à 10 unités par unité de z, puis réduit le tout à 0,9).
private const val CENTER_X = 206f
private const val CENTER_Y = 478f
private const val UNIT = 9f

private const val TAU = (2 * PI).toFloat()
private const val PERIOD_MS = 480_000L

// Pastilles (maquette réduite à 0,9) : disque blanc de 34,2, anneau de 1,35 à l'extérieur, motif
// de 16,2. L'appli 0, celle des épicycles, a un anneau de 2,25 et un halo jusqu'à 5,4 du disque.
private const val DISC = 34.2f
private const val RING = 1.35f
private const val RING_FIRST = 2.25f
private const val HALO_FIRST = 5.4f
private const val GLYPH = 16.2f

// Épicycles : cercles de 0,9 à 22 % d'encre, centres de 4,5 de diamètre. Les rayons font 1,2 × 0,9
// dans la maquette, mais le navigateur les rend nettement plus gras : on suit son rendu.
private const val ARM = 1.8f

/** Espace du texte courant (1/3 de cadratin à 14,5). */
private const val SPACE = 14.5f / 3

/** Point de la page où se trouve z(t). */
private fun point(t: Float): Offset {
    var x = 0f
    var y = 0f
    for (term in Terms) {
        x += term.c * cos(term.k * t)
        y += term.c * sin(term.k * t)
    }
    return Offset(CENTER_X + UNIT * x, CENTER_Y - UNIT * y)
}

/** Paramètre t de l'appli 0, dans [0, 2π[, réglé sur l'horloge. */
private fun phaseNow(): Float = (System.currentTimeMillis() % PERIOD_MS) * TAU / PERIOD_MS

/**
 * Temps de la figure : il avance (une trentaine de fois par seconde, bien assez pour un mouvement
 * aussi lent) si la page est animée et que les animations du système ne sont pas coupées ; t = 0 sinon.
 */
@Composable
private fun rememberFigureTime(): State<Float> {
    val animate = LocalPaper.current.animate
    return produceState(if (animate) phaseNow() else 0f, animate) {
        if (!animate) {
            value = 0f
            return@produceState
        }
        while (isActive) {
            withFrameMillis { }
            if (coroutineContext[MotionDurationScale]?.scaleFactor != 0f) value = phaseNow()
            delay(33)
        }
    }
}

@Composable
fun FourierPage() {
    val time = rememberFigureTime()
    PaperPage("Fourier", "5") {
        Text(
            tex {
                bold { +"Définition." }
                +" L'appli "
                it("j")
                +" "
                element()
                +" {0, …, 26} est en "
                it("z")
                +"("
                it("t")
                +" + 2"
                it("jπ")
                +"/27),\noù"
            },
            style = Paper.body,
            modifier = Modifier.at(22, 142).width(368.dp),
        )
        // Équation (1), centrée sur la largeur du texte moins la colonne de 28 du numéro ; la boîte de
        // ligne de la maquette grandit au-dessus pour les exposants, le numéro est centré sur elle.
        PaperText(
            tex {
                it("z")
                +"("
                it("t")
                +") = 20 e"
                sup { it("it") }
                +" − 6 e"
                sup { +"−"; it("it") }
                +" − 3 e"
                sup { +"3"; it("it") }
                +" − 2 e"
                sup { +"−3"; it("it") }
            },
            style = Centered,
            modifier = Modifier.baselineAt(22f, 208f).width(340.dp),
        )
        Text("(1)", style = Numbered, modifier = Modifier.baselineAt(22f, 206.1f).width(368.dp))

        Epicycles(time)
        PaperSlot.entries.forEachIndexed { j, slot -> MovingApp(slot, j, time) }

        // Légende sur deux lignes (coupées comme dans la maquette) ; la première, à indices, a une
        // boîte de ligne plus haute.
        PaperText(
            tex {
                b.withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                +" – Chaque "
                it("c")
                captionSub("k")
                +" ≠ 0 donne un cercle de rayon |"
                it("c")
                captionSub("k")
                +"| qui fait "
                it("k")
            },
            style = Paper.caption,
            modifier = Modifier.baselineAt(22f, 760f).width(368.dp),
        )
        Text(
            "tours ; les 27 applis suivent la courbe.",
            style = Paper.caption,
            modifier = Modifier.baselineAt(22f, 781.4f).width(368.dp),
        )

        // Équation (2) : boîte de ligne de 36 (l'intégrale et ses bornes), ligne de base à 21,1.
        Box(Modifier.at(22, 790).width(340.dp), contentAlignment = Alignment.TopCenter) { Coefficient() }
        Text("(2)", style = Numbered, modifier = Modifier.baselineAt(22f, 813.3f).width(368.dp))

        PaperText(
            tex {
                bold { +"Exercice." }
                +" À l'aide de (1) et (2), calculer "
                it("c")
                sub("1")
                +", "
                it("c")
                sub("−1")
                +", "
                it("c")
                sub("3")
                +" et"
            },
            style = Paper.body,
            modifier = Modifier.baselineAt(22f, 851.3f).width(368.dp),
        )
        PaperText(
            tex {
                it("c")
                sub("−3")
                +"."
            },
            style = Paper.body,
            modifier = Modifier.baselineAt(22f, 877.5f).width(368.dp),
        )
    }
}

private val Centered = Paper.body.copy(textAlign = TextAlign.Center)
private val Numbered = Paper.body.copy(textAlign = TextAlign.End)

/**
 * Texte posé par sa première ligne de base, à ([x], [y]) dans la page : celle de la maquette, où les
 * boîtes de ligne grandissent avec les indices et exposants (ce que ne fait pas Compose).
 */
private fun Modifier.baselineAt(x: Float, y: Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    layout(p.width, p.height) { p.place(x.dp.roundToPx(), y.dp.roundToPx() - p[FirstBaseline]) }
}

/**
 * Le signe ∈ de la police (Latin Modern) est plus étroit de 3 unités que celui que le navigateur
 * prend pour la maquette : on lui rend cette chasse, pour que la suite de la ligne tombe au même endroit.
 */
private fun MathBuilder.element() =
    b.withStyle(SpanStyle(letterSpacing = (3f / 14.5f).em)) { append("∈") }

/** Indice de la légende : 10 sur 12,5 (0,8 em et non 0,69), en italique ; [PaperText] le pose. */
private fun MathBuilder.captionSub(s: String) =
    b.withStyle(SpanStyle(fontSize = 0.8.em, baselineShift = BaselineShift(-0.33f), fontStyle = FontStyle.Italic)) { append(s) }

/** La courbe, et la chaîne d'épicycles de l'appli 0 : cercles de rayon |c|, rayons et centres. */
@Composable
private fun Epicycles(time: State<Float>) {
    val curve = remember {
        Path().apply {
            val n = 720
            for (i in 0 until n) {
                val p = point(TAU * i / n)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
    }
    val ink = Paper.Ink
    Spacer(
        Modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawBehind {
                // Dessin en unités de page.
                scale(1.dp.toPx(), pivot = Offset.Zero) {
                    drawPath(curve, ink.copy(alpha = 0.5f), style = Stroke(0.9f))
                    val t = time.value
                    var p = Offset(CENTER_X, CENTER_Y)
                    for (term in Terms) {
                        val q = p + Offset(cos(term.k * t), -sin(term.k * t)) * (term.c * UNIT)
                        drawCircle(ink.copy(alpha = 0.22f), radius = abs(term.c) * UNIT - 0.45f, center = p, style = Stroke(0.9f))
                        drawLine(ink, p, q, strokeWidth = ARM)
                        drawCircle(ink, radius = 2.25f, center = p)
                        p = q
                    }
                }
            },
    )
}

/** L'appli j, en z(t + 2jπ/27) : sa position est calculée au dessin, sans recomposer la page. */
@Composable
private fun MovingApp(slot: PaperSlot, j: Int, time: State<Float>) {
    val first = j == 0
    val ring = if (first) RING_FIRST else RING
    val size = DISC + 2 * ring
    val lead = TAU * j / PaperSlot.entries.size
    val halo = slot.family.color.copy(alpha = 0.18f)
    Box(
        Modifier
            .graphicsLayer {
                val p = point(time.value + lead)
                translationX = (p.x - size / 2).dp.toPx()
                translationY = (p.y - size / 2).dp.toPx()
            }
            .then(
                if (first) Modifier.drawBehind { drawCircle(halo, radius = (DISC / 2 + HALO_FIRST).dp.toPx()) } else Modifier,
            ),
    ) {
        SlotDisc(slot, size / 2, size / 2, size, DiscStyle.RING, ring = ring, glyphScale = GLYPH / size)
    }
}

/**
 * c_k = 1/(2π) ∫₀^{2π} z(t) e^{−ikt} dt. La fraction et l'intégrale sont des piles centrées sur
 * l'axe de la ligne (vertical-align: middle de la maquette : milieu de la pile une demi-hauteur d'x
 * au-dessus de la ligne de base), leurs lignes posées comme dans des boîtes CSS ; les écarts entre
 * les pièces (espace + marges de 2) et la hauteur de la fraction sont recalés sur la maquette.
 */
@Composable
private fun Coefficient() {
    val axis = 0.4306f * 14.5f / 2
    Row {
        PaperText(tex { it("c"); sub("k", italic = true); +" =" }, style = Paper.body, modifier = Modifier.alignByBaseline())
        Spacer(Modifier.width((SPACE + 1.25f).dp))
        // 1 sur 2π : lignes de 15,225 (line-height 1,05), filet de 0,8 au-dessus du dénominateur ;
        // la maquette la rend 0,8 plus haut que le calcul.
        MathStack(
            height = 31.25f,
            baseline = 31.25f / 2 + axis + 0.8f,
            rows = listOf(12.58f, 28.6f),
            rule = 15.225f,
            modifier = Modifier.alignByBaseline(),
        ) {
            Text("1", style = Paper.body, modifier = Modifier.padding(horizontal = 3.dp))
            Text(tex { +"2"; it("π") }, style = Paper.body, modifier = Modifier.padding(horizontal = 3.dp))
        }
        Spacer(Modifier.width((SPACE + 2.5f).dp))
        // Intégrale de 0 à 2π : bornes en 9 sur 8,1, signe sur 19,8. Le ∫ de la police, plus grand et
        // penché, est pris en 19,2 pour couvrir la même hauteur que celui de la maquette ; il déborde
        // de la colonne des bornes.
        MathStack(
            height = 36f,
            baseline = 18f + axis,
            rows = listOf(7.13f, 24.87f, 35.03f),
            overflow = 1,
            modifier = Modifier.alignByBaseline(),
        ) {
            Text(tex { +"2"; it("π") }, style = Paper.text(9f, 9f))
            Text("∫", style = Paper.text(19.2f, 19.2f))
            Text("0", style = Paper.text(9f, 9f))
        }
        Spacer(Modifier.width((SPACE + 2).dp))
        PaperText(
            tex {
                it("z")
                +"("
                it("t")
                +") e"
                sup { +"−"; it("ikt") }
                +" d"
                it("t")
                +", "
                it("k")
                +" "
                element()
                +" ℤ"
            },
            style = Paper.body,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/**
 * Pile centrée de [height] unités, de ligne de base [baseline] (pour s'aligner sur la formule) :
 * la ligne i a sa ligne de base à rows[i] unités du haut ; [rule] : filet de 0,8 sur toute la
 * largeur, à cette hauteur ; [overflow] : ligne qui ne compte pas dans la largeur.
 */
@Composable
private fun MathStack(
    height: Float,
    baseline: Float,
    rows: List<Float>,
    modifier: Modifier = Modifier,
    rule: Float? = null,
    overflow: Int = -1,
    content: @Composable () -> Unit,
) {
    val ink = Paper.Ink
    Layout(
        content,
        modifier.then(
            if (rule == null) Modifier
            else Modifier.drawBehind { drawRect(ink, Offset(0f, rule.dp.toPx()), Size(size.width, 0.8.dp.toPx())) },
        ),
    ) { measurables, _ ->
        val items = measurables.map { it.measure(Constraints()) }
        val width = items.filterIndexed { i, _ -> i != overflow }.maxOf { it.width }
        layout(width, height.dp.roundToPx(), mapOf<AlignmentLine, Int>(FirstBaseline to baseline.dp.roundToPx())) {
            items.forEachIndexed { i, p ->
                p.place((width - p.width) / 2, rows[i].dp.roundToPx() - p[FirstBaseline])
            }
        }
    }
}
