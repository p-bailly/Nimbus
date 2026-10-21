package fr.perso.nimbus.ui.paper

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Hauteur des emplacements d'indice, en em : plus basse que l'ascendante, la ligne ne grandit pas. */
private const val SLOT_HEIGHT_EM = 0.5f

/**
 * [Text] dont les indices et exposants (les étendues à [androidx.compose.ui.text.style.BaselineShift] de
 * [tex]) sont posés comme dans un navigateur avec `vertical-align: sub | super` et `line-height: 0` :
 * la ligne de base de l'indice descend de taille/5 + 1 et celle de l'exposant monte de taille/3 + 1,
 * sans changer la hauteur ni la ligne de base de la ligne. Un `Text` ordinaire, lui, agrandit la ligne
 * qui contient un indice et la recentre : ses lignes de base ne tombent plus sur celles de la maquette.
 */
@Composable
fun PaperText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val prepared = remember(text, style, density) { prepareScripts(text, style, measurer, density) }
    Text(
        prepared.first,
        modifier = modifier,
        style = style,
        softWrap = softWrap,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = if (inlineContent.isEmpty()) prepared.second else inlineContent + prepared.second,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun PaperText(text: String, style: TextStyle, modifier: Modifier = Modifier) =
    PaperText(AnnotatedString(text), style, modifier)

/** Remplace chaque indice ou exposant par un emplacement en ligne de sa largeur exacte. */
private fun prepareScripts(
    text: AnnotatedString,
    style: TextStyle,
    measurer: TextMeasurer,
    density: Density,
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
    val scripts = text.spanStyles
        .filter { (it.item.baselineShift?.multiplier ?: 0f) != 0f && it.end > it.start }
        .sortedBy { it.start }
    if (scripts.isEmpty()) return text to emptyMap()

    val inline = HashMap<String, InlineTextContent>()
    val out = AnnotatedString.Builder()
    var pos = 0
    for ((n, script) in scripts.withIndex()) {
        if (script.start < pos) continue // étendue imbriquée dans la précédente
        if (script.start > pos) out.append(text.subSequence(pos, script.start))

        val up = script.item.baselineShift!!.multiplier > 0f
        val ratio = script.item.fontSize.takeIf { it.type == TextUnitType.Em }?.value ?: 0.69f
        // Styles du texte autour (italique, gras, couleur…) et de l'indice, sans décalage ni taille.
        val outer = text.spanStyles
            .filter { it !== script && it.start <= script.start && it.end >= script.end && it.item.baselineShift == null }
            .fold(SpanStyle()) { acc, s -> acc.merge(s.item.copy(fontSize = TextUnit.Unspecified)) }
        val own = script.item.copy(baselineShift = null, fontSize = TextUnit.Unspecified)
        val inner = AnnotatedString.Builder().apply {
            pushStyle(outer.merge(own))
            append(text.subSequence(script.start, script.end).let { sub ->
                // Les étendues propres à l'indice (romain dans une variable…) sont gardées, sans décalage.
                AnnotatedString(sub.text, sub.spanStyles.map { s ->
                    AnnotatedString.Range(s.item.copy(baselineShift = null, fontSize = TextUnit.Unspecified), s.start, s.end)
                })
            })
            pop()
        }.toAnnotatedString()

        // Largeur en em du texte autour : mesurée pour un texte de 100 sp (la chasse est linéaire).
        val reference = with(density) { 100.sp.toPx() }
        val width = measurer.measure(
            inner,
            style = style.copy(fontSize = (100 * ratio).sp, lineHeight = TextUnit.Unspecified),
            softWrap = false,
            density = density,
        ).size.width / reference

        val id = "paper.script.$n"
        out.appendInlineContent(id, inner.text)
        inline[id] = InlineTextContent(Placeholder(width.em, SLOT_HEIGHT_EM.em, PlaceholderVerticalAlign.AboveBaseline)) {
            ScriptSlot(inner, style, ratio, up)
        }
        pos = script.end
    }
    if (pos < text.length) out.append(text.subSequence(pos, text.length))
    return out.toAnnotatedString() to inline
}

/** Contenu d'un emplacement : le bas de l'emplacement est la ligne de base du texte autour. */
@Composable
private fun ScriptSlot(inner: AnnotatedString, style: TextStyle, ratio: Float, up: Boolean) {
    BoxWithConstraints {
        val density = LocalDensity.current
        val slot = constraints.maxHeight.toFloat()
        val parentPx = slot / SLOT_HEIGHT_EM
        val unit = with(density) { 1.dp.toPx() }
        // Décalage de Chrome : un tiers (exposant) ou un cinquième (indice) de la taille, plus un pixel CSS.
        val baseline = if (up) slot - (parentPx / 3f + unit) else slot + (parentPx / 5f + unit)
        val size = with(density) { (parentPx * ratio).toSp() }
        Text(
            inner,
            style = style.copy(fontSize = size, lineHeight = TextUnit.Unspecified),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                val first = placeable[FirstBaseline].takeIf { it != androidx.compose.ui.layout.AlignmentLine.Unspecified } ?: placeable.height
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(0, (baseline - first).roundToInt())
                }
            },
        )
    }
}
