package fr.perso.nimbus.ui.unlock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.perso.nimbus.data.BasketShare
import fr.perso.nimbus.data.DailyExercise
import fr.perso.nimbus.data.Exercise
import fr.perso.nimbus.data.LiveLine
import fr.perso.nimbus.data.MeteoNow
import fr.perso.nimbus.data.MeteoShare
import fr.perso.nimbus.data.Seg
import fr.perso.nimbus.ui.paper.HRule
import fr.perso.nimbus.ui.paper.PAPER_HEIGHT
import fr.perso.nimbus.ui.paper.PAPER_WIDTH
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.PaperText
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.rememberPaperTime
import fr.perso.nimbus.ui.theme.PaperColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Page de déverrouillage (maquette H8a) : sous l'heure, la météo comme l'écran Aujourd'hui de l'appli
// Météo (et son coloriage), les matchs en direct des équipes favorites de Basket Live, puis l'exercice
// du jour et la solution de celui d'hier. Une section sans données disparaît, la suite remonte.
//
// Chaque texte est posé par sa ligne de base, relevée dans la maquette (Chrome arrondit ascendante et
// descendante au pixel, et pose la ligne de base en conséquence) : haut = base − interligne / 2 − k × taille,
// k = (ascendante − descendante) / 2 de la face (voir [Face]).

/** Ce qu'affiche la page ; null ou vide : la section disparaît. */
@Immutable
data class UnlockState(
    val meteo: MeteoNow? = null,
    /** Coloriage d'Aujourd'hui (411 × 287 dp de composition) ; sans lui, la météo tient en quelques lignes. */
    val sketch: ImageBitmap? = null,
    val lines: List<LiveLine> = emptyList(),
)

/** Couleurs de Météo (extrêmes du jour) et de Basket Live (direct, étoile des favoris). */
private val Maxi = Color(0xFFE8711C)
private val Mini = Color(0xFF1F5FA8)
private val Live = Color(0xFFD92D2D)
private val Star = Color(0xFFE0560C)

/** Face de la police, et son k = (ascendante − descendante) / 2, en em. */
private enum class Face(val k: Float) {
    ROMAN(0.3425f),
    ITALIC(0.34f),
    BOLD(0.3145f),
}

/** Haut d'un texte de [size] sur [line] dont la première ligne de base tombe en [baseline]. */
private fun top(baseline: Float, size: Float, line: Float = size, face: Face = Face.ROMAN): Float =
    baseline - line / 2f - face.k * size

private fun style(size: Float, line: Float = size, face: Face = Face.ROMAN, color: Color = PaperColors.Ink): TextStyle =
    Paper.text(size, line, color).copy(
        fontWeight = if (face == Face.BOLD) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (face == Face.ITALIC) FontStyle.Italic else FontStyle.Normal,
    )

private fun TextStyle.smallCaps(spacing: Float): TextStyle = copy(fontFeatureSettings = "smcp", letterSpacing = spacing.em)

/** Interligne « normal » de la maquette : 1,385 em. */
private const val NORMAL = 1.385f

/** Haut de la première section quand la météo manque (celui de l'exercice dans la maquette sans météo). */
private const val FIRST = 176f

/** Haut du bloc météo. */
private const val METEO = 146f

/** Coloriage : 368 × 199 unités, 8 sous le haut du bloc ; on en montre le bas (le ciel le plus haut est coupé). */
private const val SKETCH_W = 368f
private const val SKETCH_H = 199f

/** Entre deux sections : filet 14 sous la fin de l'une, l'autre 16 sous le filet. */
private const val RULE_GAP = 14f
private const val AFTER_RULE = 16f

/** Lignes des matchs : 23 unités l'une sous l'autre (20 d'interligne, l'étoile en ajoute 1, puis 2 d'écart). */
private const val ROW_PITCH = 23f

@Composable
fun UnlockPage(
    state: UnlockState,
    onMeteo: () -> Unit,
    onBasket: () -> Unit,
    onUnlock: () -> Unit,
) {
    val now by rememberPaperTime()
    val today = now.toLocalDate()
    val exercise = remember(today) { DailyExercise.of(today) }
    val yesterday = remember(today) { DailyExercise.of(today.minusDays(1)) }
    val unlock by rememberUpdatedState(onUnlock)

    PaperPage(
        // Glisser vers le haut, n'importe où : déverrouiller.
        modifier = Modifier.pointerInput(Unit) {
            val threshold = 64.dp.toPx()
            var total = 0f
            var fired = false
            detectVerticalDragGestures(
                onDragStart = {
                    total = 0f
                    fired = false
                },
                onVerticalDrag = { change, amount ->
                    change.consume()
                    total += amount
                    if (!fired && total < -threshold) {
                        fired = true
                        unlock()
                    }
                },
            )
        },
    ) {
        var y = FIRST
        state.meteo?.let { meteo ->
            val end = MeteoSection(meteo, state.sketch, now.hour, onMeteo)
            HRule(22, end + RULE_GAP, 368, 0.6f)
            y = end + RULE_GAP + AFTER_RULE
        }
        if (state.lines.isNotEmpty()) {
            val end = BasketSection(y, state.lines, onBasket)
            HRule(22, end + RULE_GAP, 368, 0.6f)
            y = end + RULE_GAP + AFTER_RULE
        }
        ExerciseSection(y, exercise, yesterday, onUnlock)
    }
}

// ---------------------------------------------------------------- Météo

/** Le bloc météo ; rend le bas de son contenu. */
@Composable
private fun MeteoSection(meteo: MeteoNow, sketch: ImageBitmap?, hour: Int, onOpen: () -> Unit): Float {
    val t = METEO

    if (sketch != null) Sketch(sketch, Modifier.at(22, t + 8).size(SKETCH_W.dp, SKETCH_H.dp))

    // À gauche : le lieu en petites capitales, la température (« °C » plus petit, en haut), le ressenti.
    val placeStyle = style(12f, 14f, color = PaperColors.Soft).smallCaps(0.06f)
    Text(
        meteo.place.lowercase(Locale.FRENCH),
        style = placeStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(22, top(t + 15, 12f, 14f)).widthIn(max = 180.dp),
    )
    val tempTop = top(t + 65, 46f)
    Row(Modifier.at(21, tempTop)) {
        Text(degrees(meteo.temperature), style = style(46f), softWrap = false)
        // « °C » : 17 unités, sa ligne de base 19 au-dessus de celle de la température.
        Text(
            "°C",
            style = style(17f),
            softWrap = false,
            modifier = Modifier.padding(start = 2.dp, top = (top(t + 46, 17f) - tempTop).dp),
        )
    }
    Text(
        "ressenti ${degrees(meteo.feelsLike)}\u00A0°C",
        style = style(12.5f, 16f, Face.ITALIC),
        softWrap = false,
        modifier = Modifier.at(22, top(t + 78, 12.5f, 16f, Face.ITALIC)),
    )

    // À droite : le ciel en gras (sur deux lignes s'il est long, pour laisser le milieu au dessin),
    // puis les extrêmes du jour en orange et en bleu.
    val skyStyle = style(15f, 20f, Face.BOLD).copy(textAlign = TextAlign.End, lineBreak = LineBreak.Heading)
    val skyLines = lineCount(AnnotatedString(meteo.sky), skyStyle, SKY_WIDTH)
    RightText(meteo.sky, skyStyle, top(t + 18, 15f, 20f, Face.BOLD), Modifier.widthIn(max = SKY_WIDTH.dp))
    val extremes = t + 41 + 20f * (skyLines - 1)
    RightText("max ${degrees(meteo.todayMax)}\u00A0°C", style(13.5f, 19f, color = Maxi), top(extremes, 13.5f, 19f))
    RightText("min ${degrees(meteo.todayMin)}\u00A0°C", style(13.5f, 19f, color = Mini), top(extremes + 19, 13.5f, 19f))

    val end: Float
    if (sketch != null) {
        // Légende de la figure, comme sur les autres pages.
        val caption = buildAnnotatedString {
            withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
            append(" – ${meteo.place} ${moment(hour)}, dessiné par l'appli Météo.")
        }
        val captionStyle = Paper.caption.copy(lineBreak = LineBreak.Heading)
        Text(caption, style = captionStyle, modifier = Modifier.at(22, top(t + 223, 12.5f, 16f)).width(368.dp))
        end = t + 211 + 16f * lineCount(caption, captionStyle, 368f)
    } else {
        end = maxOf(t + 82, extremes + 19 + 5)
    }

    TapZone(
        "Ouvrir Météo, ${meteo.place} : ${spoken(meteo.temperature)} degrés, ${meteo.sky.lowercase(Locale.FRENCH)} ; " +
            "aujourd'hui ${spoken(meteo.todayMax)} au plus, ${spoken(meteo.todayMin)} au moins",
        Modifier.at(22, t).size(368.dp, (if (sketch != null) 8f + SKETCH_H else end - t).dp),
        onOpen,
    )
    return end
}

/** Largeur au plus du ciel, à droite : le milieu reste au dessin. */
private const val SKY_WIDTH = 128f

/**
 * Le coloriage : composé par Météo pour 411 × 287 dp, posé sur la largeur du texte et coupé en haut
 * (la maquette n'en montre que le bas, 199 unités sur 257) ; un fondu de quelques unités adoucit la coupe.
 */
@Composable
private fun Sketch(image: ImageBitmap, modifier: Modifier) {
    Canvas(
        modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = FADE.dp.toPx()),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val shown = SKETCH_H / (SKETCH_W * MeteoShare.SKETCH_RATIO)
        val srcTop = (image.height * (1f - shown)).roundToInt().coerceIn(0, image.height - 1)
        drawImage(
            image,
            srcOffset = IntOffset(0, srcTop),
            srcSize = IntSize(image.width, image.height - srcTop),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

private const val FADE = 10f

/** Moment de la journée de la légende. */
private fun moment(hour: Int): String = when (hour) {
    in 5..11 -> "ce matin"
    in 12..17 -> "cet après-midi"
    in 18..22 -> "ce soir"
    else -> "cette nuit"
}

/** Température arrondie ; au-dessous de zéro, un tiret demi-cadratin, comme le veut la typographie française. */
private fun degrees(t: Double): String {
    val n = t.roundToInt()
    return if (n < 0) "–${abs(n)}" else "$n"
}

private fun spoken(t: Double): String = "${t.roundToInt()}"

// ---------------------------------------------------------------- Basket

/** Les matchs en direct ; rend le bas de la section. */
@Composable
private fun BasketSection(y0: Float, lines: List<LiveLine>, onOpen: () -> Unit): Float {
    val b = y0 + 15
    Text("Basket", style = style(15f, face = Face.BOLD), modifier = Modifier.at(22, top(b, 15f, face = Face.BOLD)))
    RightText("en direct", style(12f).smallCaps(0.06f), top(b, 12f))

    val rows = rememberRows(lines)
    rows.forEachIndexed { i, row -> GameRow(y0 + 38 + ROW_PITCH * i, row) }
    val end = y0 + 21 + ROW_PITCH * lines.size
    TapZone(
        "Ouvrir Basket Live : " + lines.joinToString(" ; ") { l ->
            "${l.game.away.name} ${l.game.away.score ?: 0}, ${l.game.home.name} ${l.game.home.score ?: 0}, ${BasketShare.spokenStatus(l.game)}"
        },
        Modifier.at(22, y0).size(368.dp, (end - y0 + 4).dp),
        onOpen,
    )
    return end
}

/** Une ligne prête à poser : noms affichés (le sigle si le nom ne tient pas), décalage des colonnes. */
private class GameLine(val line: LiveLine, val away: String, val home: String, val status: AnnotatedString, val dx: Float)

/** Colonnes de la maquette : l'équipe à l'extérieur finit à 114, l'équipe à domicile commence à 184. */
private const val AWAY_END = 114f
private const val AWAY_CELL = 92f
private const val HOME_START = 184f

/** Étoile : 2 unités après le nom, puis sa chasse (1,045 em de 11,2). */
private const val STAR_W = 2f + 11.2f * 2140f / 2048f

/** Point du direct : 6, et 5 avant le texte. */
private const val DOT_W = 11f

/**
 * Noms et décalage : un nom trop long pour sa colonne (« Timberwolves ») pousse toutes les colonnes vers
 * la droite, de 30 au plus ; s'il ne tient toujours pas, ou si le nom à domicile touche l'état du
 * match, le sigle d'ESPN le remplace.
 */
@Composable
private fun rememberRows(lines: List<LiveLine>): List<GameLine> {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(lines, density) {
        fun width(text: AnnotatedString, style: TextStyle) =
            with(density) { measurer.measure(text, style, softWrap = false).size.width.toDp().value }
        fun team(name: String, favorite: Boolean) =
            width(AnnotatedString(name), style(14f, face = if (favorite) Face.BOLD else Face.ROMAN)) + if (favorite) STAR_W else 0f
        val dx = lines.maxOfOrNull { team(it.game.away.name, it.awayFavorite) - AWAY_CELL }?.coerceIn(0f, 30f) ?: 0f
        lines.map { line ->
            val game = line.game
            val status = segs(BasketShare.status(game), supRatio = 0.7f, nowrap = true)
            val statusStart = 390f - DOT_W - width(status, style(12.5f))
            val away = if (team(game.away.name, line.awayFavorite) <= AWAY_CELL + dx) game.away.name else game.away.abbr
            val fits = HOME_START + dx + team(game.home.name, line.homeFavorite) <= statusStart - 6f
            GameLine(line, away, if (fits) game.home.name else game.home.abbr, status, dx)
        }
    }
}

/**
 * Une ligne, en colonnes comme un tableau : l'équipe à l'extérieur calée à droite (jusqu'à 114), son
 * score (jusqu'à 140), le tiret (centré sur 149), le score à domicile (dès 158), l'équipe à domicile (dès 184),
 * et l'état du match calé à droite, en rouge avec un point.
 */
@Composable
private fun GameRow(baseline: Float, row: GameLine) {
    val game = row.line.game
    val dx = row.dx
    TeamCell(row.away, row.line.awayFavorite, baseline, right = AWAY_END + dx)
    Score(game.away.score?.toString(), Modifier.at(114 + dx, top(baseline, 14f)).width(26.dp), Alignment.TopEnd)
    Score("–", Modifier.at(140 + dx, top(baseline, 14f)).width(18.dp), Alignment.TopCenter)
    Score(game.home.score?.toString(), Modifier.at(158 + dx, top(baseline, 14f)).width(40.dp), Alignment.TopStart)
    TeamCell(row.home, row.line.homeFavorite, baseline, left = HOME_START + dx)

    // État : point de 6 (5 avant le texte, son bas 1 au-dessus de la ligne de base), puis le texte.
    val statusTop = top(baseline, 12.5f)
    Box(Modifier.at(22, statusTop).width(368.dp)) {
        Row(Modifier.align(Alignment.TopEnd)) {
            Box(
                Modifier
                    .padding(top = (baseline - 7 - statusTop).dp, end = 5.dp)
                    .size(6.dp)
                    .background(Live, CircleShape),
            )
            PaperText(row.status, style = style(12.5f, color = Live), softWrap = false)
        }
    }
}

/** Nom d'équipe (gras et étoilé pour une favorite), calé à droite sur [right] ou à gauche sur [left]. */
@Composable
private fun TeamCell(name: String, favorite: Boolean, baseline: Float, right: Float? = null, left: Float? = null) {
    val face = if (favorite) Face.BOLD else Face.ROMAN
    val y = top(baseline, 14f, face = face)
    val content: @Composable () -> Unit = {
        Row {
            Text(name, style = style(14f, face = face), softWrap = false, maxLines = 1)
            if (favorite) FavoriteStar(baseline - y)
        }
    }
    if (right != null) {
        Box(Modifier.at(0, y).width(right.dp)) { Box(Modifier.align(Alignment.TopEnd)) { content() } }
    } else {
        Box(Modifier.at(left ?: 0f, y)) { content() }
    }
}

/**
 * Étoile des favoris : celle de la police de repli de la maquette (Inter, 0,8 em = 11,2 unités, chasse
 * 1,045 em), 2 unités après le nom, posée sur la ligne de base ([baseline] depuis le haut de la case).
 * Le nom est en gras et Inter n'y a servi qu'en romain : le navigateur l'a grossie de 1/24 em (gras
 * synthétique de FreeType), d'où le trait autour du contour.
 */
@Composable
private fun FavoriteStar(baseline: Float) {
    val em = 11.2f
    Canvas(Modifier.padding(start = 2.dp).size((em * 2140f / 2048f).dp, (baseline + 4).dp)) {
        val s = em.dp.toPx() / 2048f
        val base = baseline.dp.toPx()
        val path = Path()
        STAR.forEachIndexed { i, (x, y) ->
            val p = Offset(x * s, base - y * s)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, Star)
        drawPath(path, Star, style = Stroke(width = em.dp.toPx() / 24f, miter = 4f, join = StrokeJoin.Miter))
    }
}

/** Contour de ★ dans Inter (unités de 1/2048 em, origine sur la ligne de base). */
private val STAR = listOf(
    1070f to 382f, 1620f to -14f, 1404f to 632f, 1954f to 1030f, 1280f to 1030f,
    1070f to 1676f, 862f to 1030f, 186f to 1030f, 738f to 632f, 520f to -14f,
)

/** Score (chiffres à chasse fixe) ou tiret, calé dans sa colonne ; rien avant le match. */
@Composable
private fun Score(text: String?, modifier: Modifier, alignment: Alignment) {
    if (text == null) return
    Box(modifier) {
        Text(
            text,
            style = style(14f).copy(fontFeatureSettings = "tnum"),
            softWrap = false,
            modifier = Modifier.align(alignment),
        )
    }
}

// ---------------------------------------------------------------- Exercice

@Composable
private fun ExerciseSection(e0: Float, exercise: Exercise, yesterday: Exercise, onUnlock: () -> Unit) {
    // Titre et numéro, sur la même ligne de base.
    Text("Exercice du jour", style = style(15f, face = Face.BOLD), modifier = Modifier.at(22, top(e0 + 15, 15f, face = Face.BOLD)))
    RightText("n° ${exercise.number} · niveau : ${exercise.level}", style(12f).smallCaps(0.06f), top(e0 + 15, 12f))

    Text(
        fine(exercise.statement),
        style = style(14.5f, 20f).copy(textIndent = TextIndent(firstLine = 1.2.em), lineBreak = LineBreak.Simple),
        modifier = Modifier.at(22, top(e0 + 45, 14.5f, 20f)).width(368.dp),
    )
    if (exercise.formula.isNotEmpty()) {
        // Formule centrée dans une bande de 48 sous l'énoncé (interligne « normal »).
        PaperText(
            segs(exercise.formula, supRatio = 0.62f, nowrap = false),
            style = style(24f, 24f * NORMAL).copy(textAlign = TextAlign.Center),
            softWrap = false,
            inlineContent = Inline,
            modifier = Modifier.at(22, top(e0 + 117.5f, 24f, 24f * NORMAL)).width(368.dp),
        )
    }
    Text(
        "La solution sera donnée demain.",
        style = style(12.5f, 16f, Face.ITALIC, PaperColors.Soft).copy(textAlign = TextAlign.Center),
        modifier = Modifier.at(22, top(e0 + 162, 12.5f, 16f, Face.ITALIC)).width(368.dp),
    )

    HRule(22, e0 + 194, 368, 0.6f)
    Text("Solution d'hier", style = style(13.5f, face = Face.BOLD), modifier = Modifier.at(22, top(e0 + 224, 13.5f, face = Face.BOLD)))
    RightText("n° ${yesterday.number}", style(12f).smallCaps(0.06f), top(e0 + 224, 12f))

    // L'énoncé d'hier en italique, sa formule à la suite (insécable) ; puis l'explication et la réponse.
    // Sous le texte, le bas de la page : un appui déverrouille.
    val body = style(13.5f, 19f).copy(lineBreak = LineBreak.Simple)
    Column(Modifier.at(0, e0 + 234).size(PAPER_WIDTH.dp, (PAPER_HEIGHT - e0 - 234).dp)) {
        PaperText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendFine(yesterday.statement) }
                if (yesterday.formula.isNotEmpty()) {
                    append(" ")
                    append(segs(yesterday.formula, supRatio = 0.68f, nowrap = true))
                }
            },
            style = body,
            inlineContent = Inline,
            modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(),
        )
        Spacer(Modifier.height(5.dp))
        PaperText(
            buildAnnotatedString {
                appendInlineContent(INDENT, " ")
                append(WJ)
                append(segs(yesterday.explanation, supRatio = 0.68f, nowrap = false))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendFine("Réponse : ${yesterday.answer}.") }
            },
            style = body,
            inlineContent = Inline,
            modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(),
        )
        TapZone("Déverrouiller", Modifier.fillMaxWidth().weight(1f), onUnlock)
    }
}

/**
 * Morceaux de texte : exposants (à [supRatio] em, posés par [PaperText]), variables en italique, « ⋯ »
 * dessinés ; [nowrap] : espaces insécables (la formule ne se coupe pas). Android permet une coupure
 * devant tout emplacement en ligne (exposant, points) : un gluon (U+2060, sans chasse) l'interdit.
 */
private fun segs(list: List<Seg>, supRatio: Float, nowrap: Boolean): AnnotatedString = buildAnnotatedString {
    for (seg in list) {
        val text = if (nowrap) seg.text.replace(' ', '\u00A0') else seg.text
        withStyle(SpanStyle(fontStyle = if (seg.italic) FontStyle.Italic else FontStyle.Normal)) {
            text.split('⋯').forEachIndexed { i, part ->
                if (i > 0) {
                    if (nowrap) append(WJ)
                    appendInlineContent(DOTS, "⋯")
                    if (nowrap) append(WJ)
                }
                appendFine(part)
            }
        }
        if (seg.sup.isNotEmpty()) {
            append(WJ)
            withStyle(SpanStyle(fontSize = supRatio.em, baselineShift = BaselineShift.Superscript)) { append(seg.sup) }
            append(WJ)
        }
    }
}

private const val DOTS = "unlock.dots"
private const val INDENT = "unlock.indent"

/** Gluon (WORD JOINER) : invisible, sans chasse, interdit la coupure de part et d'autre. */
private const val WJ = '\u2060'

/**
 * Ajoute [text] ; l'espace fine insécable, absente de la police, devient une espace insécable de
 * demi-corps : 1/6 em, la chasse que lui donne le navigateur de la maquette.
 */
private fun AnnotatedString.Builder.appendFine(text: String) {
    text.split(DailyExercise.THIN).forEachIndexed { i, part ->
        if (i > 0) withStyle(SpanStyle(fontSize = 0.5.em)) { append('\u00A0') }
        append(part)
    }
}

private fun fine(text: String): AnnotatedString = buildAnnotatedString { appendFine(text) }

/** Contenus en ligne des textes de l'exercice. */
private val Inline: Map<String, InlineTextContent> = mapOf(
    // Alinéa de 1,2 em, en emplacement : avec TextIndent, Android décale les emplacements des lignes
    // suivantes de la valeur de l'alinéa (les exposants s'écartent de leur base).
    INDENT to InlineTextContent(Placeholder(1.2.em, 0.1.em, PlaceholderVerticalAlign.AboveBaseline)) {},
    // « ⋯ », absent de la police : les trois points carrés de la police de repli de la maquette (DejaVu
    // Sans, chasse 1 em), dessinés sur la ligne de base (le bas de l'emplacement).
    DOTS to InlineTextContent(Placeholder(1.em, 0.5.em, PlaceholderVerticalAlign.AboveBaseline)) {
        Canvas(Modifier.fillMaxSize()) {
            val em = size.width
            val u = em / 2048f
            for (x in floatArrayOf(236f, 918f, 1600f)) {
                drawRect(
                    PaperColors.Ink,
                    topLeft = Offset(x * u, size.height - 764f * u),
                    size = Size(211f * u, 254f * u),
                )
            }
        }
    },
)

// ---------------------------------------------------------------- Outils

/** Texte calé à droite sur la marge (x = 390), son haut en [top]. */
@Composable
private fun RightText(text: String, style: TextStyle, top: Float, modifier: Modifier = Modifier) {
    Box(Modifier.at(22, top).width(368.dp)) {
        Text(text, style = style, modifier = modifier.align(Alignment.TopEnd))
    }
}

/** Nombre de lignes de [text] composé sur [width] unités. */
@Composable
private fun lineCount(text: AnnotatedString, style: TextStyle, width: Float): Int {
    val measurer = rememberTextMeasurer()
    val px = with(LocalDensity.current) { width.dp.roundToPx() }
    return remember(text, style, px) { measurer.measure(text, style, constraints = Constraints(maxWidth = px)).lineCount }
}

/** Zone d'appui sans encre (la maquette n'en montre pas) : un bouton pour l'accessibilité. */
@Composable
private fun TapZone(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    )
}
