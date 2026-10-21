package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.DiscStyle
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.SlotDisc
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.slot

// Page des remerciements d'une thèse : une dédicace, puis un paragraphe par famille où chaque appli
// est remerciée par son nom, précédé de sa pastille. Les noms sont ceux des apps (renommage compris) :
// les paragraphes sont recomposés autour, ligne à ligne et justifiés, comme dans un navigateur.
// Un Text justifié avec InlineTextContent ne convient pas : Android pose les contenus en ligne comme
// si le texte n'était pas étiré, et les pastilles mordent sur les mots.

/** Largeur du texte. */
private const val MEASURE = 368f

/** Interligne des paragraphes et alinéa (1,5 em). */
private const val LINE = 24f
private const val INDENT = 1.5f * 14.5f

/** Pastille : 17 unités, descendue de 3 unités sous la ligne de base, puis 4 unités avant le nom. */
private const val ICON = 17f
private const val ICON_DROP = 3f
private const val ICON_GAP = 4f

/** Un nom plus long que la ligne est abrégé. */
private const val MAX_NAME = MEASURE - ICON - ICON_GAP

/** Corps des paragraphes : 14,5 sur 24. */
private val Body: TextStyle
    get() = Paper.text(14.5f, LINE)

/** Les paragraphes ; {PLACE} : l'appli de cette place ; « \u202F » : espace fine insécable. */
private val Paragraphs = listOf(
    "Je tiens d'abord à remercier ceux qui me relient aux autres\u202F: {WHATSAPP} et {MESSAGES} pour les " +
        "conversations de chaque jour, {DISCORD} et {REDDIT} pour les discussions au long cours, {TELEPHONE} " +
        "pour les voix et {GMAIL} pour le courrier.",
    "Merci à {YOUTUBE}, {TWITCH} et {STREMIO} pour les images, à {SPOTIFY} pour la musique, à {PHOTOS} pour " +
        "les souvenirs et à {CAMERA} pour tout ce qui reste à voir.",
    "Pour m'avoir conduit à bon port, je remercie {WAZE} et {MAPS}\u202F; et {METEO}, consultée avant chaque sortie.",
    "Ma gratitude va aussi à {CLAUDE} pour ses relectures, à {GITHUB} pour le code, à {DRIVE} et {FICHIERS} " +
        "pour les documents, à {BANQUE} pour les comptes, à {AUTHENTICATOR} pour les codes, à {PLAY} pour les " +
        "mises à jour, à {AMAZON} pour les colis et à {PRONOTE} pour les notes.",
    "Enfin, merci à {LICHESS} pour les parties d'échecs, à {BASKET} pour le terrain et à {PIR}, mon appli personnelle.",
    "Les erreurs qui subsistent sont, bien entendu, les miennes.",
)

private val SlotRef = Regex("""\{(\w+)\}""")

@Composable
fun RemerciementsPage() {
    PaperPage("Avant-propos", "7") {
        Text(
            "À mes vingt-sept applis,\nsans qui cet écran serait vide.",
            style = Paper.text(14.5f, 20f).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.End),
            modifier = Modifier.at(22, 146).width(MEASURE.dp),
        )
        Column(Modifier.at(22, 222).width(MEASURE.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Remerciements",
                style = Paper.text(30f, 34f).copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 18.dp),
            )
            Paragraphs.forEach { ThanksParagraph(it) }
        }
    }
}

// ---------------------------------------------------------------- Composition des paragraphes

/** Morceau d'un mot : du texte, ou une appli (pastille et nom, insécables). */
private sealed interface Piece
private class Prose(val text: AnnotatedString) : Piece
private class Thanks(val slot: PaperSlot) : Piece

/** Mots d'un paragraphe : suites de morceaux entre deux espaces sécables. */
private fun words(template: String): List<List<Piece>> {
    val words = mutableListOf<List<Piece>>()
    var word = mutableListOf<Piece>()
    val prose = StringBuilder()
    fun flushProse() {
        if (prose.isNotEmpty()) word += Prose(fine(prose.toString()))
        prose.clear()
    }
    fun flushWord() {
        flushProse()
        if (word.isNotEmpty()) words += word
        word = mutableListOf()
    }
    var last = 0
    fun prose(s: String) = s.forEach { if (it == ' ') flushWord() else prose.append(it) }
    for (ref in SlotRef.findAll(template)) {
        prose(template.substring(last, ref.range.first))
        flushProse()
        word += Thanks(PaperSlot.valueOf(ref.groupValues[1]))
        last = ref.range.last + 1
    }
    prose(template.substring(last))
    flushWord()
    return words
}

/** L'espace fine insécable, absente de la police, devient une espace insécable réduite à 0,2 em. */
private fun fine(s: String): AnnotatedString = buildAnnotatedString {
    for (c in s) {
        if (c == '\u202F') withStyle(SpanStyle(fontSize = 0.6.em)) { append('\u00A0') } else append(c)
    }
}

/** Texte posé : une mise en page d'une ligne, son coin haut gauche (unités). */
private class PlacedProse(val layout: TextLayoutResult, val x: Float, val y: Float)

/** Appli posée : nom (abrégé s'il est trop long), largeur du nom, coin haut gauche de la ligne. */
private class PlacedThanks(val slot: PaperSlot, val name: String, val nameWidth: Float, val x: Float, val y: Float)

private class Composed(val prose: List<PlacedProse>, val thanks: List<PlacedThanks>, val lines: Int, val baseline: Float)

/**
 * Coupure ligne à ligne (le mot qui dépasse passe à la ligne suivante, sans césure), puis
 * justification : le blanc qui reste est réparti entre les espaces ; la dernière ligne reste au fer
 * à gauche. Une appli est indivisible et la justification n'étire pas l'intérieur de son nom.
 */
private fun compose(
    words: List<List<Piece>>,
    name: (PaperSlot) -> String,
    measurer: TextMeasurer,
    density: Density,
): Composed {
    fun layout(text: AnnotatedString) = measurer.measure(text, Body, softWrap = false, maxLines = 1, density = density)

    // Chasse sous-pixel (voir PaperTextBase) : l'avance du texte, en unités, est celle de la maquette
    // (la largeur intrinsèque, elle, est arrondie au pixel supérieur).
    fun width(layout: TextLayoutResult) =
        layout.getHorizontalPosition(layout.layoutInput.text.length, usePrimaryDirection = true) / density.density
    val blank = layout(AnnotatedString(" "))
    val space = width(blank)

    class Measured(val piece: Piece, val width: Float, val layout: TextLayoutResult? = null, val label: String = "")

    val measured = words.map { word ->
        word.map { piece ->
            when (piece) {
                is Prose -> layout(piece.text).let { Measured(piece, width(it), it) }
                is Thanks -> name(piece.slot).let { label ->
                    Measured(piece, ICON + ICON_GAP + minOf(width(layout(AnnotatedString(label))), MAX_NAME), label = label)
                }
            }
        }
    }

    // Coupure : chaque ligne prend autant de mots que la largeur le permet.
    val lines = mutableListOf<List<List<Measured>>>()
    var line = mutableListOf<List<Measured>>()
    var width = 0f
    for (word in measured) {
        val w = word.sumOf { it.width.toDouble() }.toFloat()
        val available = if (lines.isEmpty()) MEASURE - INDENT else MEASURE
        if (line.isNotEmpty() && width + space + w > available + 0.01f) {
            lines += line
            line = mutableListOf()
            width = 0f
        }
        width += if (line.isEmpty()) w else space + w
        line += word
    }
    if (line.isNotEmpty()) lines += line

    // Justification et pose.
    val prose = mutableListOf<PlacedProse>()
    val thanks = mutableListOf<PlacedThanks>()
    lines.forEachIndexed { i, words ->
        val start = if (i == 0) INDENT else 0f
        val natural = words.sumOf { w -> w.sumOf { it.width.toDouble() } }.toFloat() + space * (words.size - 1)
        val stretch = if (i == lines.lastIndex || words.size < 2) 0f else (MEASURE - start - natural) / (words.size - 1)
        var x = start
        val y = i * LINE
        for (word in words) {
            for (m in word) {
                when (val piece = m.piece) {
                    is Prose -> prose += PlacedProse(m.layout!!, x, y)
                    is Thanks -> thanks += PlacedThanks(piece.slot, m.label, m.width - ICON - ICON_GAP, x, y)
                }
                x += m.width
            }
            x += space + stretch
        }
    }
    return Composed(prose, thanks, lines.size, blank.firstBaseline / density.density)
}

/** Un paragraphe : le texte est dessiné, les applis sont des boutons posés dans les blancs. */
@Composable
private fun ThanksParagraph(template: String) {
    val env = LocalPaper.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val words = remember(template) { words(template) }
    val names = words.flatten().filterIsInstance<Thanks>().map { env.name(it.slot) }
    val composed = remember(words, names, density, measurer) { compose(words, env::name, measurer, density) }
    val plain = remember(template, names) {
        SlotRef.replace(template) { env.name(PaperSlot.valueOf(it.groupValues[1])) }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height((composed.lines * LINE).dp)
            .semantics { contentDescription = plain }
            .drawBehind {
                for (p in composed.prose) drawText(p.layout, topLeft = Offset(p.x.dp.toPx(), p.y.dp.toPx()))
            },
    ) {
        for (t in composed.thanks) ThanksButton(t, composed.baseline)
    }
}

/**
 * Une appli remerciée, sur toute la hauteur de sa ligne : pastille cerclée de la couleur de sa
 * famille, puis son nom sur la ligne de base du paragraphe. Appui : ouvrir ; appui long : actions.
 */
@Composable
private fun ThanksButton(t: PlacedThanks, baseline: Float) {
    Box(
        Modifier
            .at(t.x, t.y)
            .size((ICON + ICON_GAP + t.nameWidth).dp, LINE.dp)
            .slot(t.slot, RoundedCornerShape(3.dp)),
    ) {
        // La pastille répond aussi à l'appui ; pour l'accessibilité, le bouton suffit.
        Box(Modifier.clearAndSetSemantics {}) {
            SlotDisc(
                t.slot,
                cx = ICON / 2f,
                cy = baseline + ICON_DROP - ICON / 2f,
                size = ICON,
                style = DiscStyle.RING,
                ring = 1.4f,
                glyphScale = 0.6f,
            )
        }
        Text(
            t.name,
            style = Body,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.at(ICON + ICON_GAP, 0).widthIn(max = MAX_NAME.dp),
        )
    }
}
