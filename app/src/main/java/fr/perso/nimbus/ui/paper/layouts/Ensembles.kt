package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.DiscStyle
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.MathBuilder
import fr.perso.nimbus.ui.paper.PAPER_HEIGHT
import fr.perso.nimbus.ui.paper.PAPER_WIDTH
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.SlotDisc
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.tex
import fr.perso.nimbus.ui.theme.PaperColors

// Page 3 : les applis réparties dans un diagramme de Venn (sociales S, médias M, utiles U),
// puis la formule du crible qui les dénombre.

// La figure est tracée dans son propre repère puis réduite, comme dans la maquette :
// translate(10,3 ; 20) scale(0,95) depuis le coin haut gauche.
private const val FIG_X = 10.3f
private const val FIG_Y = 20f
private const val FIG_SCALE = 0.95f

private fun fx(x: Float) = FIG_X + FIG_SCALE * x
private fun fy(y: Float) = FIG_Y + FIG_SCALE * y

// Palette « vif » ; les disques sont teintés à 10 %.
private val InkS = Color(0xFFD9583B)
private val InkM = Color(0xFF0F8F86)
private val InkU = Color(0xFF6B46C1)
private const val FILL_ALPHA = 0.1f

/** Interligne « normal » de la maquette pour Computer Modern (935 + 250 + 200 millièmes d'em). */
private const val NORMAL = 1.385f

private class VennSet(val cx: Float, val cy: Float, val r: Float, val color: Color)

private val Sets = listOf(
    VennSet(146f, 322f, 130f, InkS),
    VennSet(266f, 322f, 130f, InkM),
    VennSet(206f, 478f, 158f, InkU),
)

/** Pastilles : 42 unités cerclées de 1,2 (anneau extérieur), motif de 20, avant réduction. */
private const val DISC = 42f
private const val DISC_RING = 1.2f
private const val DISC_GLYPH = 20f

/**
 * Centres des pastilles dans le repère de la figure (recherche sous contraintes de la maquette :
 * chaque pastille reste à 5 unités au moins de tout cercle, centres à 50 unités au moins).
 */
private val Discs: List<Triple<PaperSlot, Float, Float>> = listOf(
    // S ∩ M ∩ U
    Triple(PaperSlot.WHATSAPP, 206f, 352f),
    // S ∩ M, S ∩ U, M ∩ U
    Triple(PaperSlot.YOUTUBE, 212f, 252f),
    Triple(PaperSlot.REDDIT, 165f, 298f),
    Triple(PaperSlot.GMAIL, 116f, 404f),
    Triple(PaperSlot.PHOTOS, 314f, 405f),
    Triple(PaperSlot.DRIVE, 266f, 424f),
    // S seulement
    Triple(PaperSlot.MESSAGES, 47f, 312f),
    Triple(PaperSlot.TELEPHONE, 96f, 293f),
    Triple(PaperSlot.DISCORD, 139f, 223f),
    // M seulement
    Triple(PaperSlot.STREMIO, 347f, 256f),
    Triple(PaperSlot.SPOTIFY, 302f, 297f),
    Triple(PaperSlot.CAMERA, 351f, 348f),
    Triple(PaperSlot.TWITCH, 274f, 225f),
    // U seulement
    Triple(PaperSlot.CLAUDE, 207f, 526f),
    Triple(PaperSlot.GITHUB, 281f, 495f),
    Triple(PaperSlot.FICHIERS, 202f, 581f),
    Triple(PaperSlot.AUTHENTICATOR, 269f, 566f),
    Triple(PaperSlot.BANQUE, 122f, 489f),
    Triple(PaperSlot.PLAY, 143f, 557f),
    Triple(PaperSlot.AMAZON, 205f, 470f),
    Triple(PaperSlot.PRONOTE, 334f, 494f),
    Triple(PaperSlot.MAPS, 89f, 530f),
    Triple(PaperSlot.WAZE, 318f, 545f),
    Triple(PaperSlot.METEO, 75f, 461f),
    // Hors de S ∪ M ∪ U
    Triple(PaperSlot.LICHESS, 356f, 622f),
    Triple(PaperSlot.BASKET, 259f, 666f),
    Triple(PaperSlot.PIR, 109f, 707f),
)

@Composable
fun EnsemblesPage() {
    PaperPage {
        Text(
            tex {
                bf("Notations.")
                +" "; it("S"); +", "; it("M"); +", "; it("U"); +" ⊂ "; it("A"); +" = {applis}, |"; it("A"); +"| = 27."
            },
            style = Paper.body,
            modifier = Modifier.at(22, 142).width(368.dp),
        )

        VennFigure()

        Text(
            tex {
                b.withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                +" – Les applis sociales "; it("S"); +", médias "; it("M"); +" et utiles "; it("U"); +"."
            },
            style = Paper.caption,
            modifier = Modifier.at(22, 728).width(368.dp),
        )

        Text(
            tex { bf("Proposition"); +" (crible)"; bf(".") },
            style = Paper.body,
            modifier = Modifier.at(22, 754),
        )
        // Colonne de la maquette, écarts de 6 : dans le navigateur, les italiques portent chaque
        // rangée de formule à 21 unités (754 + 20 + 6, puis + 21 + 6, puis + 21 + 6 + 6).
        Equation(1, 780f) {
            +"|"; it("S"); +" ∪ "; it("M"); +" ∪ "; it("U"); +"| = 7 + 9 + 15 − 3 − 2 − 3 + 1 = 24"
        }
        Equation(2, 807f) {
            +"|"; it("A"); +" ∖ ("; it("S"); +" ∪ "; it("M"); +" ∪ "; it("U"); +")| = 27 − 24 = 3"
        }
        Text(
            tex {
                bf("Exercice.")
                +" Déterminer le nombre d'applis qui sont dans un seul des ensembles "
                it("S"); +", "; it("M"); +", "; it("U"); +"."
            },
            style = Paper.body.copy(lineBreak = LineBreak.Simple),
            modifier = Modifier.at(22, 840).width(368.dp),
        )
    }
}

/**
 * Équation numérotée : centrée dans la colonne de gauche (la largeur du texte moins les 28 unités
 * du numéro), numéro calé à droite, comme la grille de la maquette. La rangée mesure 21 unités :
 * le numéro, d'interligne 20, y est centré, une demi-unité plus bas que la formule.
 */
@Composable
private fun Equation(number: Int, top: Float, content: MathBuilder.() -> Unit) {
    Text(
        tex(content),
        style = Paper.body.copy(textAlign = TextAlign.Center),
        softWrap = false,
        modifier = Modifier.at(22, top).width(340.dp),
    )
    Text(
        "($number)",
        style = Paper.body.copy(textAlign = TextAlign.End),
        modifier = Modifier.at(362, top + 0.5f).width(28.dp),
    )
}

/** Cadre, trois cercles teintés, leurs noms et les 27 pastilles, réduits à 0,95. */
@Composable
private fun VennFigure() {
    Canvas(Modifier.size(PAPER_WIDTH.dp, PAPER_HEIGHT.dp)) {
        val u = 1.dp.toPx()
        translate(FIG_X * u, FIG_Y * u) {
            scale(FIG_SCALE, pivot = Offset.Zero) {
                drawRoundRect(
                    color = PaperColors.Ink,
                    topLeft = Offset(14f * u, 160f * u),
                    size = Size(384f * u, 576f * u),
                    cornerRadius = CornerRadius(16f * u),
                    style = Stroke(1f * u),
                )
                for (set in Sets) {
                    val center = Offset(set.cx * u, set.cy * u)
                    drawCircle(set.color.copy(alpha = FILL_ALPHA), set.r * u, center)
                    drawCircle(set.color, set.r * u, center, style = Stroke(2f * u))
                }
            }
        }
    }

    // Noms des ensembles (15 unités, 13 pour le complémentaire), avant réduction, en interligne
    // « normal » du navigateur (1,385 em).
    val label = Paper.text(15f * FIG_SCALE, 15f * NORMAL * FIG_SCALE)
    SetLabel("S", "sociales", InkS, label, Modifier.at(fx(26f), fy(170f)))
    SetLabel(
        "M", "médias", InkM, label.copy(textAlign = TextAlign.End),
        Modifier.at(0, fy(170f)).width(fx(386f).dp),
    )
    SetLabel("U", "utiles", InkU, label, Modifier.at(fx(302f), fy(650f)))
    Text(
        "A ∖ (S ∪ M ∪ U)",
        style = Paper.text(13f * FIG_SCALE, 13f * NORMAL * FIG_SCALE).copy(fontStyle = FontStyle.Italic),
        modifier = Modifier.at(fx(150f), fy(708f)),
    )

    // Pastilles à leur place, ou là où l'utilisateur les a posées (appui long puis glisser).
    val env = LocalPaper.current
    var placed by remember(env.ensembles) { mutableStateOf(env.ensembles) }
    var dragged by remember { mutableStateOf<Pair<PaperSlot, Offset>?>(null) }
    val discs = remember(placed, dragged) {
        val moved = dragged?.let { placed + it } ?: placed
        Discs.map { (slot, x, y) -> moved[slot]?.let { Triple(slot, it.x, it.y) } ?: Triple(slot, x, y) }
    }

    val size = (DISC + 2 * DISC_RING) * FIG_SCALE
    for ((slot, x, y) in discs) {
        SlotDisc(
            slot, fx(x), fy(y),
            size = size,
            style = DiscStyle.RING,
            color = PaperColors.Ink,
            ring = DISC_RING * FIG_SCALE,
            glyphScale = DISC_GLYPH / (DISC + 2 * DISC_RING),
            // Les motifs du sommaire : chaque appli à sa couleur.
            ownColors = true,
        )
    }
    DiscTouch(
        discs,
        onMove = { slot, at -> dragged = slot to at },
        onDrop = {
            dragged?.let {
                placed = placed + it
                env.onEnsembles(placed)
            }
            dragged = null
        },
        onCancel = { dragged = null },
    )
}

/** Cadre de la figure (repère de la figure) : une pastille déplacée y reste entière. */
private const val FRAME_X0 = 14f
private const val FRAME_Y0 = 160f
private const val FRAME_X1 = 398f
private const val FRAME_Y1 = 736f

/** Écart minimal entre deux centres de pastilles (repère de la figure). */
private const val DISC_GAP = 46f

/**
 * Position permise pour la pastille [i] tirée en [p] : dans le cadre, à [DISC_GAP] au moins des autres
 * pastilles ; null si elle touche une autre pastille (elle reste alors où elle était).
 */
internal fun allowedDisc(p: Offset, i: Int, discs: List<Offset>): Offset? {
    val r = DISC / 2 + 2f
    val at = Offset(p.x.coerceIn(FRAME_X0 + r, FRAME_X1 - r), p.y.coerceIn(FRAME_Y0 + r, FRAME_Y1 - r))
    return at.takeIf { discs.withIndex().none { (j, o) -> j != i && (o - at).getDistance() < DISC_GAP } }
}

/**
 * Touchers de la figure, par-dessus les pastilles : appui sur une pastille, l'ouvrir ; appui long, ses
 * actions ; appui long puis glisser, la déplacer ([onMove] à chaque pas, [onDrop] au lâcher).
 */
@Composable
private fun DiscTouch(
    discs: List<Triple<PaperSlot, Float, Float>>,
    onMove: (PaperSlot, Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
    val env by rememberUpdatedState(LocalPaper.current)
    val current by rememberUpdatedState(discs)
    val haptics = LocalHapticFeedback.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Pastille sous le doigt (repère de la figure), ou -1.
    fun hit(list: List<Triple<PaperSlot, Float, Float>>, x: Float, y: Float): Int {
        val i = list.indices.minByOrNull { (list[it].second - x) * (list[it].second - x) + (list[it].third - y) * (list[it].third - y) } ?: return -1
        val d = Offset(list[i].second - x, list[i].third - y).getDistance()
        return if (d <= DISC / 2 + 6f) i else -1
    }
    Box(
        Modifier
            .at(fx(FRAME_X0), fy(FRAME_Y0))
            .size(((FRAME_X1 - FRAME_X0) * FIG_SCALE).dp, ((FRAME_Y1 - FRAME_Y0) * FIG_SCALE).dp)
            .onGloballyPositioned { coords = it }
            .pointerInput(Unit) {
                val unit = 1.dp.toPx() * FIG_SCALE
                detectTapGestures(onTap = { at ->
                    val i = hit(current, FRAME_X0 + at.x / unit, FRAME_Y0 + at.y / unit)
                    if (i >= 0) env.onOpen(current[i].first)
                })
            }
            .pointerInput(Unit) {
                val unit = 1.dp.toPx() * FIG_SCALE
                val slop = viewConfiguration.touchSlop
                var index = -1
                var start = Offset.Zero
                var total = Offset.Zero
                var moving = false
                fun rect(d: Triple<PaperSlot, Float, Float>): Rect {
                    val c = coords ?: return Rect.Zero
                    val r = DISC / 2 * unit
                    val x = (d.second - FRAME_X0) * unit
                    val y = (d.third - FRAME_Y0) * unit
                    return Rect(c.localToWindow(Offset(x - r, y - r)), c.localToWindow(Offset(x + r, y + r)))
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        index = hit(current, FRAME_X0 + at.x / unit, FRAME_Y0 + at.y / unit)
                        if (index >= 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            start = Offset(current[index].second, current[index].third)
                        }
                        total = Offset.Zero
                        moving = false
                    },
                    onDrag = { change, amount ->
                        if (index < 0) return@detectDragGesturesAfterLongPress
                        change.consume()
                        total += amount
                        if (!moving && total.getDistance() > slop) moving = true
                        if (moving) {
                            val others = current.map { Offset(it.second, it.third) }
                            allowedDisc(start + total / unit, index, others)?.let { onMove(current[index].first, it) }
                        }
                    },
                    onDragEnd = {
                        if (index >= 0) {
                            if (moving) onDrop() else current[index].let { env.onActions(it.first, rect(it)) }
                        }
                        moving = false
                    },
                    onDragCancel = {
                        if (moving) onCancel()
                        moving = false
                    },
                )
            },
    )
}

/** Nom d'un ensemble : la lettre en gras, le mot en italique, à l'encre de l'ensemble. */
@Composable
private fun SetLabel(letter: String, word: String, color: Color, style: TextStyle, modifier: Modifier) {
    Text(tex { bf(letter); +" "; it(word) }, style = style.copy(color = color), modifier = modifier)
}
