package fr.perso.nimbus.ui.unlock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import fr.perso.nimbus.data.BasketShare
import fr.perso.nimbus.data.DailyExercise
import fr.perso.nimbus.data.Exercise
import fr.perso.nimbus.data.GameTeam
import fr.perso.nimbus.data.LiveLine
import fr.perso.nimbus.data.MeteoNow
import fr.perso.nimbus.data.MeteoShare
import fr.perso.nimbus.data.Seg
import fr.perso.nimbus.ui.paper.HRule
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PAPER_HEIGHT
import fr.perso.nimbus.ui.paper.PAPER_WIDTH
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperCanvas
import fr.perso.nimbus.ui.paper.PaperClock
import fr.perso.nimbus.ui.paper.PaperText
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.rememberPaperTime
import fr.perso.nimbus.ui.theme.PaperColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Page de déverrouillage (maquette H8a, sans en-tête et en plus gros) : sous l'heure, la météo comme l'écran Aujourd'hui de l'appli
// Météo (et son coloriage, agrandi et ravivé), les matchs en direct et les résultats de la nuit des équipes
// favorites de Basket Live, puis l'exercice du jour et la solution de celui d'hier, cachée sous un bandeau.
// Une section sans données disparaît, la suite remonte.
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
    /** Résultats de la nuit des équipes favorites (matchs terminés), sous le direct. */
    val results: List<LiveLine> = emptyList(),
)

/** Couleurs de Météo (extrêmes du jour) et de Basket Live (direct, victoire, défaite). */
private val Maxi = Color(0xFFE8711C)
private val Mini = Color(0xFF1F5FA8)
private val Live = Color(0xFFD92D2D)
private val Win = Color(0xFF2E8540)
private val Loss = Color(0xFFB0413E)

/** Titres des sections (Basket, Exercice du jour, Solution d'hier), en gras. */
private const val TITLE = 19f

/** Étiquettes à droite des titres, en petites capitales. */
private const val LABEL = 13.5f

/** Texte courant, le même pour les matchs et pour l'exercice. */
private const val BODY = 19f

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

/** Heure (sa case de [TIME] unités) puis date, en haut de la page : il n'y a pas d'en-tête. */
private const val CLOCK_TOP = 30f
private const val TIME = 80f
private const val DATE_TOP = 112f
private const val DATE = 15f

/** Haut de la première section quand la météo manque. */
private const val FIRST = 154f

/** Haut du bloc météo. */
private const val METEO = 142f

/**
 * Coloriage : dessiné sur [SKETCH_WIDTH] unités, centré et coupé aux bords de la page (ce n'est que de
 * l'herbe), [SKETCH_TOP] sous le haut du bloc ; on en montre le bas, [SKETCH_SHOWN] de sa hauteur (le
 * ciel le plus haut est coupé).
 */
internal const val SKETCH_WIDTH = 460f
private const val SKETCH_SHOWN = 0.68f
private val SKETCH_H = SKETCH_WIDTH * MeteoShare.SKETCH_RATIO * SKETCH_SHOWN
private const val SKETCH_TOP = 24f

/** Entre deux sections : filet 14 sous la fin de l'une, l'autre 16 sous le filet. */
private const val RULE_GAP = 14f
private const val AFTER_RULE = 16f

/** Lignes des matchs : 27 unités l'une sous l'autre. */
private const val ROW_PITCH = 27f

@Composable
fun UnlockPage(
    state: UnlockState,
    onMeteo: () -> Unit,
    onBasket: () -> Unit,
    onUnlock: () -> Unit,
    /** Solution d'hier montrée d'emblée (captures). */
    solutionShown: Boolean = false,
) {
    val now by rememberPaperTime()
    val today = now.toLocalDate()
    val exercise = remember(today) { DailyExercise.of(today) }
    val yesterday = remember(today) { DailyExercise.of(today.minusDays(1)) }
    val unlock by rememberUpdatedState(onUnlock)

    PaperCanvas(
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
        PaperClock(CLOCK_TOP, TIME, DATE_TOP, DATE)
        var y = FIRST
        state.meteo?.let { meteo ->
            val end = MeteoSection(meteo, state.sketch, now.hour, onMeteo)
            HRule(22, end + RULE_GAP, 368, 0.6f)
            y = end + RULE_GAP + AFTER_RULE
        }
        if (state.lines.isNotEmpty() || state.results.isNotEmpty()) {
            val end = BasketSection(y, state.lines + state.results, onBasket)
            HRule(22, end + RULE_GAP, 368, 0.6f)
            y = end + RULE_GAP + AFTER_RULE
        }
        ExerciseSection(y, exercise, yesterday, solutionShown, onUnlock)
    }
}

// ---------------------------------------------------------------- Météo

/** Le bloc météo ; rend le bas de son contenu. */
@Composable
private fun MeteoSection(meteo: MeteoNow, sketch: ImageBitmap?, hour: Int, onOpen: () -> Unit): Float {
    val t = METEO

    if (sketch != null) Sketch(sketch, Modifier.at(0, t + SKETCH_TOP).size(PAPER_WIDTH.dp, SKETCH_H.dp))

    // À gauche : le lieu en petites capitales, la température (« °C » plus petit, en haut), le ressenti.
    val placeStyle = style(17f, 20f, color = PaperColors.Soft).smallCaps(0.06f)
    Text(
        meteo.place.lowercase(Locale.FRENCH),
        style = placeStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(22, top(t + 18, 17f, 20f)).widthIn(max = 200.dp),
    )
    val tempTop = top(t + 88, 72f)
    Row(Modifier.at(20, tempTop)) {
        Text(degrees(meteo.temperature), style = style(72f), softWrap = false)
        // « °C » : 26 unités, sa ligne de base 29 au-dessus de celle de la température.
        Text(
            "°C",
            style = style(26f),
            softWrap = false,
            modifier = Modifier.padding(start = 3.dp, top = (top(t + 59, 26f) - tempTop).dp),
        )
    }
    Text(
        "ressenti ${degrees(meteo.feelsLike)}\u00A0°C",
        style = style(18f, 22f, Face.ITALIC),
        softWrap = false,
        modifier = Modifier.at(22, top(t + 113, 18f, 22f, Face.ITALIC)),
    )

    // À droite : le ciel en gras (sur deux lignes s'il est long, pour laisser le milieu au dessin),
    // puis les extrêmes du jour, en gras, en orange et en bleu.
    val skyStyle = style(21f, 26f, Face.BOLD).copy(textAlign = TextAlign.End, lineBreak = LineBreak.Heading)
    val skyLines = lineCount(AnnotatedString(meteo.sky), skyStyle, SKY_WIDTH)
    RightText(meteo.sky, skyStyle, top(t + 23, 21f, 26f, Face.BOLD), Modifier.widthIn(max = SKY_WIDTH.dp))
    val extremes = t + 55 + 26f * (skyLines - 1)
    RightText("max ${degrees(meteo.todayMax)}\u00A0°C", style(21f, 26f, Face.BOLD, Maxi), top(extremes, 21f, 26f, Face.BOLD))
    RightText("min ${degrees(meteo.todayMin)}\u00A0°C", style(21f, 26f, Face.BOLD, Mini), top(extremes + 26, 21f, 26f, Face.BOLD))

    val end: Float
    if (sketch != null) {
        // Légende de la figure, comme sur les autres pages.
        val caption = buildAnnotatedString {
            withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
            append(" – ${meteo.place}${moment(hour)?.let { " $it" }.orEmpty()}, dessiné par l'appli Météo.")
        }
        val captionStyle = Paper.caption.copy(lineBreak = LineBreak.Heading)
        val below = t + SKETCH_TOP + SKETCH_H
        Text(caption, style = captionStyle, modifier = Modifier.at(22, top(below + 16, 12.5f, 16f)).width(368.dp))
        end = below + 4 + 16f * lineCount(caption, captionStyle, 368f)
    } else {
        end = maxOf(t + 119, extremes + 26 + 6)
    }

    TapZone(
        "Ouvrir Météo, ${meteo.place} : ${spoken(meteo.temperature)} degrés, ${meteo.sky.lowercase(Locale.FRENCH)} ; " +
            "aujourd'hui ${spoken(meteo.todayMax)} au plus, ${spoken(meteo.todayMin)} au moins",
        Modifier.at(22, t).size(368.dp, (if (sketch != null) SKETCH_TOP + SKETCH_H else end - t).dp),
        onOpen,
    )
    return end
}

/** Largeur au plus du ciel, à droite : le milieu reste au dessin. */
private const val SKY_WIDTH = 165f

/**
 * Le coloriage : composé par Météo pour 411 × 287 dp, agrandi au-delà des marges et coupé en haut ; un
 * fondu de quelques unités adoucit la coupe. Ses couleurs sont ravivées (saturation, opacité des
 * hachures). À chaque réveil, il monte doucement et se colore, en [SKETCH_ANIMATION] ms, puis ne bouge
 * plus : seul le dessin est redessiné pendant ce temps, rien n'est recomposé.
 */
@Composable
private fun Sketch(image: ImageBitmap, modifier: Modifier) {
    val animate = LocalPaper.current.animate
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    if (animate) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        val state by lifecycle.currentStateAsState()
        val resumed = state.isAtLeast(Lifecycle.State.RESUMED)
        LaunchedEffect(resumed, image) {
            if (resumed) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(SKETCH_ANIMATION, easing = FastOutSlowInEasing))
            } else {
                // Écran éteint : prêt pour le prochain réveil.
                progress.snapTo(0f)
            }
        }
    }
    val vivid = remember { vivid(1f) }
    Canvas(
        modifier
            .clipToBounds()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val p = progress.value
                alpha = (p * 2.5f).coerceAtMost(1f)
                translationY = (1f - p) * RISE.dp.toPx()
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = FADE.dp.toPx()),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val p = progress.value
        val srcTop = (image.height * (1f - SKETCH_SHOWN)).roundToInt().coerceIn(0, image.height - 1)
        val w = SKETCH_WIDTH.dp.toPx()
        drawImage(
            image,
            srcOffset = IntOffset(0, srcTop),
            srcSize = IntSize(image.width, image.height - srcTop),
            dstOffset = IntOffset(((size.width - w) / 2f).roundToInt(), 0),
            dstSize = IntSize(w.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
            colorFilter = if (p >= 1f) vivid else vivid(p),
        )
    }
}

private const val FADE = 10f

/** Montée du dessin au réveil, en unités, et durée de l'animation. */
private const val RISE = 12f
private const val SKETCH_ANIMATION = 1600

/** Saturation des couleurs du coloriage, une fois coloré. */
private const val SATURATION = 1.9f

/**
 * Filtre du coloriage, à [p] de l'animation : en gris au début, puis de plus en plus coloré ; les
 * hachures, un peu transparentes, sont rendues plus franches.
 */
private fun vivid(p: Float): ColorFilter {
    val m = ColorMatrix().apply { setToSaturation(SATURATION * p) }
    m *= ColorMatrix().apply { setToScale(0.9f, 0.9f, 0.9f, 1.35f) }
    return ColorFilter.colorMatrix(m)
}

/** Moment de la journée de la légende ; rien la nuit. */
private fun moment(hour: Int): String? = when (hour) {
    in 5..11 -> "ce matin"
    in 12..17 -> "cet après-midi"
    in 18..22 -> "ce soir"
    else -> null
}

/** Température arrondie ; au-dessous de zéro, un tiret demi-cadratin, comme le veut la typographie française. */
private fun degrees(t: Double): String {
    val n = t.roundToInt()
    return if (n < 0) "–${abs(n)}" else "$n"
}

private fun spoken(t: Double): String = "${t.roundToInt()}"

// ---------------------------------------------------------------- Basket

/** Les matchs en direct, puis les résultats de la nuit (matchs terminés) ; rend le bas de la section. */
@Composable
private fun BasketSection(y0: Float, lines: List<LiveLine>, onOpen: () -> Unit): Float {
    val b = y0 + 17
    Text("Basket", style = style(TITLE, face = Face.BOLD), modifier = Modifier.at(22, top(b, TITLE, face = Face.BOLD)))
    if (lines.any { it.game.live }) RightText("en direct", style(LABEL, color = Live).smallCaps(0.06f), top(b, LABEL))

    val rows = rememberRows(lines)
    rows.forEachIndexed { i, row -> GameRow(y0 + 45 + ROW_PITCH * i, row) }
    val end = y0 + 45 + ROW_PITCH * (lines.size - 1) + 7
    TapZone(
        "Ouvrir Basket Live : " + lines.joinToString(" ; ") { l ->
            val winner = BasketShare.winner(l.game)?.let { ", victoire des ${it.name}" }.orEmpty()
            "${l.game.away.name} ${l.game.away.score ?: 0}, ${l.game.home.name} ${l.game.home.score ?: 0}, " +
                BasketShare.spokenStatus(l.game) + winner
        },
        Modifier.at(22, y0).size(368.dp, (end - y0 + 4).dp),
        onOpen,
    )
    return end
}

/** Une équipe d'une ligne : nom affiché (le sigle si le nom ne tient pas), face et couleur. */
private class TeamLook(val name: String, val face: Face, val color: Color)

/** Une ligne prête à poser : les deux équipes, l'état du match (et sa couleur), le décalage des colonnes. */
private class GameLine(
    val line: LiveLine,
    val away: TeamLook,
    val home: TeamLook,
    val status: AnnotatedString,
    val statusFace: Face,
    val statusColor: Color,
    val dx: Float,
)

/**
 * Colonnes : l'équipe à l'extérieur finit à [AWAY_END] (sa case commence à 22), puis, après [GAP], son
 * score dans [SCORE_W], le tiret dans [DASH_W], le score à domicile, et l'équipe à domicile dès [HOME_START].
 */
private const val AWAY_END = 116f
private const val AWAY_CELL = AWAY_END - 22f
private const val GAP = 8f
private const val SCORE_W = 31f
private const val DASH_W = 20f
private const val SCORES = AWAY_END + GAP
private const val HOME_START = SCORES + SCORE_W + DASH_W + SCORE_W + GAP

/** État du match : sa taille, et la place du point du direct (7, et 5 avant le texte). */
private const val STATUS = 14.5f
private const val DOT_W = 12f

/**
 * Faces des équipes : pendant le match, la favorite en gras ; une fois terminé, la gagnante en gras
 * et la perdante à l'encre pâle. L'état d'un match terminé dit, pour la favorite, « victoire » (en vert)
 * ou « défaite » (en rouge sombre).
 */
private fun look(line: LiveLine, team: GameTeam, favorite: Boolean): Pair<Face, Color> {
    if (!line.game.finished) return (if (favorite) Face.BOLD else Face.ROMAN) to PaperColors.Ink
    val winner = BasketShare.winner(line.game) ?: return Face.ROMAN to PaperColors.Ink
    return if (winner.id == team.id) Face.BOLD to PaperColors.Ink else Face.ROMAN to PaperColors.Soft
}

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
        fun team(name: String, face: Face) = width(AnnotatedString(name), style(BODY, face = face))
        val looks = lines.map { look(it, it.game.away, it.awayFavorite) to look(it, it.game.home, it.homeFavorite) }
        val dx = lines.indices.maxOfOrNull { team(lines[it].game.away.name, looks[it].first.first) - AWAY_CELL }
            ?.coerceIn(0f, 30f) ?: 0f
        lines.mapIndexed { i, line ->
            val game = line.game
            val (awayLook, homeLook) = looks[i]
            val won = BasketShare.favoriteWon(line)
            val status = buildAnnotatedString {
                if (won != null) append(if (won) "victoire" else "défaite")
                val rest = BasketShare.status(game)
                if (won != null && rest.isNotEmpty()) append(" · ")
                append(segs(rest, supRatio = 0.7f, nowrap = true))
            }
            val statusFace = if (won != null) Face.BOLD else Face.ROMAN
            val statusColor = when (won) {
                true -> Win
                false -> Loss
                null -> if (game.finished) PaperColors.Soft else Live
            }
            val dot = if (game.finished) 0f else DOT_W
            val statusStart = 390f - dot - width(status, style(STATUS, face = statusFace))
            val away = if (team(game.away.name, awayLook.first) <= AWAY_CELL + dx) game.away.name else game.away.abbr
            val fits = HOME_START + dx + team(game.home.name, homeLook.first) <= statusStart - 6f
            GameLine(
                line,
                TeamLook(away, awayLook.first, awayLook.second),
                TeamLook(if (fits) game.home.name else game.home.abbr, homeLook.first, homeLook.second),
                status,
                statusFace,
                statusColor,
                dx,
            )
        }
    }
}

/**
 * Une ligne, en colonnes comme un tableau : l'équipe à l'extérieur calée à droite, son score, le
 * tiret, le score à domicile, l'équipe à domicile, et l'état du match calé à droite (en rouge avec un
 * point pendant le direct ; victoire ou défaite de la favorite une fois terminé).
 */
@Composable
private fun GameRow(baseline: Float, row: GameLine) {
    val game = row.line.game
    val dx = row.dx
    TeamCell(row.away, baseline, right = AWAY_END + dx)
    Score(game.away.score?.toString(), row.away, baseline, SCORES + dx, SCORE_W, Alignment.TopEnd)
    Score("–", TeamLook("", Face.ROMAN, PaperColors.Ink), baseline, SCORES + SCORE_W + dx, DASH_W, Alignment.TopCenter)
    Score(game.home.score?.toString(), row.home, baseline, SCORES + SCORE_W + DASH_W + dx, 40f, Alignment.TopStart)
    TeamCell(row.home, baseline, left = HOME_START + dx)

    if (row.status.isEmpty()) return
    val statusTop = top(baseline, STATUS, face = row.statusFace)
    Box(Modifier.at(22, statusTop).width(368.dp)) {
        Row(Modifier.align(Alignment.TopEnd)) {
            if (!game.finished) {
                Box(
                    Modifier
                        .padding(top = (baseline - 8 - statusTop).dp, end = 5.dp)
                        .size(7.dp)
                        .background(Live, CircleShape),
                )
            }
            PaperText(row.status, style = style(STATUS, face = row.statusFace, color = row.statusColor), softWrap = false)
        }
    }
}

/** Nom d'équipe, calé à droite sur [right] ou à gauche sur [left]. */
@Composable
private fun TeamCell(team: TeamLook, baseline: Float, right: Float? = null, left: Float? = null) {
    val y = top(baseline, BODY, face = team.face)
    val content: @Composable () -> Unit = {
        Text(team.name, style = style(BODY, face = team.face, color = team.color), softWrap = false, maxLines = 1)
    }
    if (right != null) {
        Box(Modifier.at(0, y).width(right.dp)) { Box(Modifier.align(Alignment.TopEnd)) { content() } }
    } else {
        Box(Modifier.at(left ?: 0f, y)) { content() }
    }
}

/** Score (chiffres à chasse fixe, dans la face de son équipe) ou tiret, calé dans sa colonne ; rien avant le match. */
@Composable
private fun Score(text: String?, look: TeamLook, baseline: Float, x: Float, width: Float, alignment: Alignment) {
    if (text == null) return
    Box(Modifier.at(x, top(baseline, BODY, face = look.face)).width(width.dp)) {
        Text(
            text,
            style = style(BODY, face = look.face, color = look.color).copy(fontFeatureSettings = "tnum"),
            softWrap = false,
            modifier = Modifier.align(alignment),
        )
    }
}

// ---------------------------------------------------------------- Exercice

@Composable
private fun ExerciseSection(e0: Float, exercise: Exercise, yesterday: Exercise, solutionShown: Boolean, onUnlock: () -> Unit) {
    // Titre et numéro, sur la même ligne de base.
    Text("Exercice du jour", style = style(TITLE, face = Face.BOLD), modifier = Modifier.at(22, top(e0 + 18, TITLE, face = Face.BOLD)))
    RightText("n° ${exercise.number} · niveau : ${exercise.level}", style(LABEL).smallCaps(0.06f), top(e0 + 18, LABEL))

    // Énoncé, formule, puis l'avis : chacun sous le précédent, selon le nombre de lignes de l'énoncé.
    val statement = fine(exercise.statement)
    val statementStyle = style(BODY, 26f).copy(textIndent = TextIndent(firstLine = 1.2.em), lineBreak = LineBreak.Simple)
    Text(
        statement,
        style = statementStyle,
        modifier = Modifier.at(22, top(e0 + 52, BODY, 26f)).width(368.dp),
    )
    var last = e0 + 52 + 26f * (lineCount(statement, statementStyle, 368f) - 1)
    if (exercise.formula.isNotEmpty()) {
        last += 54
        PaperText(
            segs(exercise.formula, supRatio = 0.62f, nowrap = false),
            style = style(34f, 34f * NORMAL).copy(textAlign = TextAlign.Center),
            softWrap = false,
            inlineContent = Inline,
            modifier = Modifier.at(22, top(last, 34f, 34f * NORMAL)).width(368.dp),
        )
    }
    last += 40
    Text(
        "La solution sera donnée demain.",
        style = style(15.5f, 19f, Face.ITALIC, PaperColors.Soft).copy(textAlign = TextAlign.Center),
        modifier = Modifier.at(22, top(last, 15.5f, 19f, Face.ITALIC)).width(368.dp),
    )

    val rule = last + 26
    HRule(22, rule, 368, 0.6f)
    Text("Solution d'hier", style = style(TITLE, face = Face.BOLD), modifier = Modifier.at(22, top(rule + 30, TITLE, face = Face.BOLD)))
    RightText("n° ${yesterday.number}", style(LABEL).smallCaps(0.06f), top(rule + 30, LABEL))

    // L'énoncé d'hier en italique, sa formule à la suite (insécable) ; l'explication et la réponse restent
    // sous un bandeau hachuré tant qu'on ne l'a pas touché (de nouveau caché à chaque réveil).
    // Sous le texte, le bas de la page : un appui déverrouille.
    var revealed by remember(yesterday.date) { mutableStateOf(solutionShown) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val awake = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(awake) { if (!awake) revealed = false }

    val body = style(BODY, 25f).copy(lineBreak = LineBreak.Simple)
    val solution = rule + 42
    Column(Modifier.at(0, solution).size(PAPER_WIDTH.dp, (PAPER_HEIGHT - solution).dp)) {
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
        Spacer(Modifier.height(8.dp))
        if (revealed) {
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
        } else {
            SolutionBanner { revealed = true }
        }
        TapZone("Déverrouiller", Modifier.fillMaxWidth().weight(1f), onUnlock)
    }
}

/** Hauteur du bandeau qui cache la solution d'hier. */
private const val BANNER = 42f

/** Le bandeau qui cache la solution : des hachures, comme un coloriage, et l'invitation à toucher. */
@Composable
private fun SolutionBanner(onReveal: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 22.dp)
            .fillMaxWidth()
            .height(BANNER.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BannerFill)
            .drawBehind {
                val step = 9.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(BannerHatch, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.2.dp.toPx())
                    x += step
                }
            }
            .clickable(
                remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Afficher",
                role = Role.Button,
                onClick = onReveal,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Toucher pour voir la solution",
            style = style(16f, 20f, Face.ITALIC, PaperColors.Ink),
            softWrap = false,
            modifier = Modifier
                .background(BannerFill, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

/** Couleurs du bandeau : un jaune pâle, hachuré d'ocre. */
private val BannerFill = Color(0xFFFBEFC8)
private val BannerHatch = Color(0xFFE2BE62)

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
