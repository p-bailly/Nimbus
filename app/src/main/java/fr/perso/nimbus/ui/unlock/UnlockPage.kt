package fr.perso.nimbus.ui.unlock

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import fr.perso.nimbus.R
import fr.perso.nimbus.data.BasketShare
import fr.perso.nimbus.data.CrayonLogos
import fr.perso.nimbus.data.GameTeam
import fr.perso.nimbus.data.LiveLine
import fr.perso.nimbus.data.MessageApps
import fr.perso.nimbus.data.MeteoNow
import fr.perso.nimbus.data.MeteoShare
import fr.perso.nimbus.data.Seg
import fr.perso.nimbus.data.UnlockNote
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
import fr.perso.nimbus.ui.theme.GoogleSans
import fr.perso.nimbus.ui.theme.GoogleSansDisplay
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperTextBase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Page de déverrouillage (maquette H8a, sans en-tête et en plus gros) : sous l'heure, la météo comme l'écran Aujourd'hui de l'appli
// Météo (et son coloriage, agrandi et ravivé), les messages reçus (WhatsApp, Discord, Messages, Claude…) dans
// des bandeaux coloriés à la couleur de leur appli, les matchs en direct et les résultats de la nuit des
// équipes favorites de Basket Live dans un bandeau colorié orange, les séries de LoL des équipes favorites de LoL
// Live dans un bandeau violet, puis une figure, dessinée au crayon,
// différente à chaque affichage. Une section sans données disparaît, la suite remonte.
//
// En fond de l'écran verrouillé ([UnlockWallpaper]), le haut de l'écran est au Pixel : son heure à gauche,
// sa date à droite. La page pose la météo dessous, sur une ligne, dans la police de l'horloge (Google Sans) ;
// sur le coloriage ne restent que le maximum et le minimum, coloriés au crayon, sans blanc autour.
//
// Une seule police, le Computer Modern droit, en romain ou en gras : ni italique, ni petites capitales.
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
    /** Séries de LoL en cours des équipes favorites de LoL Live, et leurs résultats récents. */
    val lol: List<LiveLine> = emptyList(),
    val lolResults: List<LiveLine> = emptyList(),
    /** Notifications des applis de messages, les plus récentes d'abord. */
    val notes: List<UnlockNote> = emptyList(),
    /** La musique en cours (ou en pause depuis peu). */
    val music: Music? = null,
)

/** Couleurs de Météo (extrêmes du jour) et de Basket Live (direct, victoire, défaite). */
private val Maxi = Color(0xFFE8711C)
private val Mini = Color(0xFF1F5FA8)
private val Live = Color(0xFFD92D2D)
private val Win = Color(0xFF2E8540)
private val Loss = Color(0xFFB0413E)

/** Titre de la section Basket, en gras. */
private const val TITLE = 19f

/** Étiquettes à droite des titres, en petites capitales. */
private const val LABEL = 13.5f

/** Texte des matchs. */
private const val BODY = 19f

/** Face de la police, et son k = (ascendante − descendante) / 2, en em. */
private enum class Face(val k: Float) {
    ROMAN(0.3425f),
    BOLD(0.3145f),
}

/** Haut d'un texte de [size] sur [line] dont la première ligne de base tombe en [baseline]. */
private fun top(baseline: Float, size: Float, line: Float = size, face: Face = Face.ROMAN): Float =
    baseline - line / 2f - face.k * size

private fun style(size: Float, line: Float = size, face: Face = Face.ROMAN, color: Color = PaperColors.Ink): TextStyle =
    Paper.text(size, line, color).copy(fontWeight = if (face == Face.BOLD) FontWeight.Bold else FontWeight.Normal)

/** Heure (sa case de [TIME] unités) puis date, en haut de la page : il n'y a pas d'en-tête. */
private const val CLOCK_TOP = 30f
private const val TIME = 80f
private const val DATE_TOP = 112f
private const val DATE = 15f

/**
 * En fond d'écran : la bande de l'heure du Pixel (heure et date) finit vers y = 134 (relevé sur un Pixel 11
 * Pro, horloge « petite ») ; la ligne de la météo a sa ligne de base en [WEATHER_BASE], le bloc météo
 * (coloriage) commence en [METEO_WALLPAPER].
 */
private const val WEATHER_BASE = 178f
private const val METEO_WALLPAPER = 192f

/**
 * En fond d'écran : le bas du rond de l'empreinte que le Pixel pose sur l'écran verrouillé (centré vers
 * y = 677, 40 de rayon, relevé sur un Pixel 11 Pro) ; la figure commence dessous.
 */
private const val FINGERPRINT_BOTTOM = 717f

/** k de Google Sans Flex : (ascendante − descendante) / 2, en em (voir [Face]). */
private const val SANS_K = 0.34f

/** Haut d'un texte en Google Sans de [size] (interligne [size]) dont la ligne de base tombe en [baseline]. */
private fun sansTop(baseline: Float, size: Float): Float = baseline - size / 2f - SANS_K * size

private fun sans(size: Float, weight: FontWeight = FontWeight.Normal, color: Color = PaperColors.Ink, display: Boolean = false): TextStyle =
    PaperTextBase.copy(
        fontFamily = if (display) GoogleSansDisplay else GoogleSans,
        fontSize = size.sp,
        lineHeight = size.sp,
        fontWeight = weight,
        color = color,
    )

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

/** Sous un bandeau colorié (messages, Basket) : pas de filet, 16 unités jusqu'à la section suivante. */
private const val AFTER_BANNER = 16f

/** Lignes des matchs : 27 unités l'une sous l'autre. */
private const val ROW_PITCH = 27f

@Composable
internal fun UnlockPage(
    state: UnlockState,
    onMeteo: () -> Unit,
    onBasket: () -> Unit,
    onUnlock: () -> Unit,
    /** Ouvrir LoL Live (après le déverrouillage). */
    onLol: () -> Unit = {},
    /** Ouvrir une notification (après le déverrouillage). */
    onNote: (UnlockNote) -> Unit = {},
    /** La figure à montrer (une nouvelle à chaque affichage) ; sans elle, celle du jour. */
    figure: DailyFigure? = null,
    /** Explication de la figure montrée d'emblée (captures). */
    infoShown: Boolean = false,
    /**
     * Le texte de la météo posé sur le coloriage, en fractions de l'image de Météo (gauche, haut, droite,
     * bas) : à lui demander de dessiner autour.
     */
    onSketchText: (List<Rect>) -> Unit = {},
    /**
     * Fond de l'écran verrouillé : l'heure et la date sont celles du système, en haut ; la page pose la
     * météo dessous.
     */
    wallpaper: Boolean = false,
    /** Les boutons du bandeau Musique, et l'appui sur le bandeau. */
    onMusic: (MusicAction) -> Unit = {},
) {
    val now by rememberPaperTime()
    val today = now.toLocalDate()
    val dayFigure = remember(today) { DailyFigures.of(today) }
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
        if (!wallpaper) PaperClock(CLOCK_TOP, TIME, DATE_TOP, DATE, smallCaps = false)
        var y = FIRST
        state.meteo?.let { meteo ->
            val end = MeteoSection(meteo, state.sketch, onMeteo, onSketchText, wallpaper)
            HRule(22, end + RULE_GAP, 368, 0.6f)
            y = end + RULE_GAP + AFTER_RULE
        }
        state.music?.let { music ->
            // Sur le fond d'écran, qui ne reçoit pas les appuis, pas de boutons.
            y = MusicSection(y, music, controls = !wallpaper, onMusic) + AFTER_BANNER
        }
        if (state.notes.isNotEmpty()) {
            y = NotesSection(y, state.notes, today, onNote) + AFTER_BANNER
        }
        if (state.lines.isNotEmpty() || state.results.isNotEmpty()) {
            y = GameSection(y, BASKET_LOOK, state.lines + state.results, onBasket) + AFTER_BANNER
        }
        if (state.lol.isNotEmpty() || state.lolResults.isNotEmpty()) {
            y = GameSection(y, LOL_LOOK, state.lol + state.lolResults, onLol) + AFTER_BANNER
        }
        // En fond d'écran, la figure passe sous l'empreinte du Pixel, qui ne la cache plus.
        if (wallpaper) y = maxOf(y, FINGERPRINT_BOTTOM + 6)
        // La figure du jour, numérotée après le coloriage de Météo s'il est là ; sous elle, un appui déverrouille.
        TapZone("Déverrouiller", Modifier.at(0, y).size(PAPER_WIDTH.dp, (PAPER_HEIGHT - y).coerceAtLeast(0f).dp), onUnlock)
        FigureSection(y, figure ?: dayFigure, if (state.meteo != null && state.sketch != null) 2 else 1, infoShown)
    }
}

// ---------------------------------------------------------------- Fond d'écran

/**
 * Sous la bande de l'heure du Pixel, la météo sur une ligne, en Google Sans comme l'heure : la température
 * en gros, puis le ciel et, dessous, le ressenti.
 */
@Composable
private fun WallpaperWeather(meteo: MeteoNow) {
    val temperature = "${degrees(meteo.temperature)}°C"
    val temperatureStyle = sans(40f, display = true)
    Text(temperature, style = temperatureStyle, softWrap = false, modifier = Modifier.at(24, sansTop(WEATHER_BASE, 40f)))
    val x = 24 + textWidth(AnnotatedString(temperature), temperatureStyle) + 12
    Text(
        meteo.sky,
        style = sans(17f, FontWeight.Medium),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(x, sansTop(WEATHER_BASE - 19, 17f)).width((390 - x).dp),
    )
    Text(
        "ressenti ${degrees(meteo.feelsLike)}°C",
        style = sans(15f, color = PaperColors.Soft),
        softWrap = false,
        modifier = Modifier.at(x, sansTop(WEATHER_BASE, 15f)),
    )
}

// ---------------------------------------------------------------- Météo

/** Le bloc météo ; rend le bas de son contenu. */
@Composable
private fun MeteoSection(
    meteo: MeteoNow,
    sketch: ImageBitmap?,
    onOpen: () -> Unit,
    onSketchText: (List<Rect>) -> Unit,
    /** En fond d'écran : le ciel, la température et le ressenti sont sur une ligne au-dessus ; le lieu n'est pas écrit. */
    wallpaper: Boolean = false,
): Float {
    val t = if (wallpaper) METEO_WALLPAPER else METEO

    // Le texte posé sur le coloriage, mesuré là où sera le dessin (même avant qu'il n'arrive) : Météo
    // pousse le soleil, la lune et les nuages à côté, et le ciel passe sous les lettres, sans blanc autour.
    val text = remember { SketchText() }
    val frame = Modifier.at(0, t + SKETCH_TOP).size(PAPER_WIDTH.dp, SKETCH_H.dp)
    Spacer(frame.then(text.frame))
    val density = LocalDensity.current
    val zones = text.zones
    LaunchedEffect(zones, density) { onSketchText(with(density) { imageZones(zones) }) }
    if (sketch != null) Sketch(sketch, frame)

    if (wallpaper) {
        WallpaperWeather(meteo)
        // Sur le dessin, le maximum et le minimum coloriés au crayon : le ciel passe dessous, le soleil et
        // les nuages s'écartent.
        val extremes = t + 50
        CrayonText(
            "max ${degrees(meteo.todayMax)}\u00A0°C",
            style(26f, 30f, Face.BOLD),
            Maxi,
            seed = 31,
            Modifier.at(22, top(extremes, 26f, 30f, Face.BOLD)).width(368.dp).wrapContentWidth(Alignment.End).then(text.of("max")),
        )
        CrayonText(
            "min ${degrees(meteo.todayMin)}\u00A0°C",
            style(26f, 30f, Face.BOLD),
            Mini,
            seed = 37,
            Modifier.at(22, top(extremes + 32, 26f, 30f, Face.BOLD)).width(368.dp).wrapContentWidth(Alignment.End).then(text.of("min")),
        )
        return sketchEnd(meteo, sketch, onOpen, t, extremes + 32 + 6)
    }

    // À gauche : le lieu, la température (« °C » plus petit, en haut), le ressenti.
    val placeStyle = style(17f, 20f, color = PaperColors.Soft)
    Text(
        meteo.place,
        style = placeStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = text.layout("place"),
        modifier = Modifier.at(22, top(t + 18, 17f, 20f)).widthIn(max = 200.dp).then(text.of("place")),
    )
    val tempTop = top(t + 88, 72f)
    Row(Modifier.at(20, tempTop)) {
        Text(
            degrees(meteo.temperature),
            style = style(72f),
            softWrap = false,
            onTextLayout = text.layout("temperature"),
            modifier = text.of("temperature"),
        )
        // « °C » : 26 unités, sa ligne de base 29 au-dessus de celle de la température.
        Text(
            "°C",
            style = style(26f),
            softWrap = false,
            onTextLayout = text.layout("unit"),
            modifier = Modifier.padding(start = 3.dp, top = (top(t + 59, 26f) - tempTop).dp).then(text.of("unit")),
        )
    }
    Text(
        "ressenti ${degrees(meteo.feelsLike)}\u00A0°C",
        style = style(18f, 22f, color = PaperColors.Soft),
        softWrap = false,
        onTextLayout = text.layout("feels"),
        modifier = Modifier.at(22, top(t + 113, 18f, 22f)).then(text.of("feels")),
    )

    // À droite : le ciel en gras (sur deux lignes s'il est long, pour laisser le milieu au dessin),
    // puis les extrêmes du jour, en gras, en orange et en bleu.
    val skyStyle = style(21f, 26f, Face.BOLD).copy(textAlign = TextAlign.End, lineBreak = LineBreak.Heading)
    val skyLines = lineCount(AnnotatedString(meteo.sky), skyStyle, SKY_WIDTH)
    RightText(meteo.sky, skyStyle, top(t + 23, 21f, 26f, Face.BOLD), Modifier.widthIn(max = SKY_WIDTH.dp), over = text, key = "sky")
    val extremes = t + 55 + 26f * (skyLines - 1)
    RightText("max ${degrees(meteo.todayMax)}\u00A0°C", style(21f, 26f, Face.BOLD, Maxi), top(extremes, 21f, 26f, Face.BOLD), over = text, key = "max")
    RightText("min ${degrees(meteo.todayMin)}\u00A0°C", style(21f, 26f, Face.BOLD, Mini), top(extremes + 26, 21f, 26f, Face.BOLD), over = text, key = "min")
    return sketchEnd(meteo, sketch, onOpen, t, maxOf(t + 119, extremes + 26 + 6))
}

/** Sous le bloc météo qui commence en [t] : la légende du coloriage, la zone d'appui ; rend le bas du bloc. */
@Composable
private fun sketchEnd(meteo: MeteoNow, sketch: ImageBitmap?, onOpen: () -> Unit, t: Float, textEnd: Float): Float {
    val end: Float
    if (sketch != null) {
        // Légende de la figure, courte comme celle de la figure du jour.
        val caption = AnnotatedString("Figure 1 – Météo à ${meteo.place}")
        val captionStyle = style(CAPTION, CAPTION_LINE).copy(textAlign = TextAlign.Center, lineBreak = LineBreak.Heading)
        val below = t + SKETCH_TOP + SKETCH_H
        Text(caption, style = captionStyle, modifier = Modifier.at(22, top(below + 17, CAPTION, CAPTION_LINE)).width(368.dp))
        end = below + 3 + CAPTION_LINE * lineCount(caption, captionStyle, 368f)
    } else {
        end = textEnd
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

/**
 * Les zones de texte [zones] (en pixels du cadre du coloriage) en fractions de l'image de Météo, telle
 * que [Sketch] la pose : [SKETCH_WIDTH] unités de large au milieu de la page, le bas seulement
 * ([SKETCH_SHOWN] de sa hauteur). Et, en plus, le haut coupé jusqu'au milieu du fondu (moins la marge
 * de la gomme) : le soleil, la lune et les nuages descendent là où on les voit bien.
 */
internal fun Density.imageZones(zones: List<Rect>): List<Rect> {
    val page = PAPER_WIDTH.dp.toPx()
    val w = SKETCH_WIDTH.dp.toPx()
    val h = SKETCH_H.dp.toPx()
    val left = (page - w) / 2f
    val hidden = 1f - SKETCH_SHOWN
    fun x(px: Float) = (px - left) / w
    fun y(px: Float) = hidden + px / h * SKETCH_SHOWN
    val full = SKETCH_WIDTH * MeteoShare.SKETCH_RATIO
    val cut = Rect(0f, 0f, 1f, (hidden + (FADE / 2 - TEXT_MARGIN) / full).coerceAtLeast(0.01f))
    return listOf(cut) + zones.map { Rect(x(it.left), y(it.top), x(it.right), y(it.bottom)) }
        .filter { it.right > 0f && it.left < 1f && it.bottom > 0f && it.top < 1f }
}

/** Montée du dessin au réveil, en unités, et durée de l'animation. */
private const val RISE = 12f
private const val SKETCH_ANIMATION = 1600

/** Saturation des couleurs du coloriage, une fois coloré. */
private const val SATURATION = 1.45f

/**
 * Filtre du coloriage, à [p] de l'animation : en gris au début, puis de plus en plus coloré ; les
 * hachures, un peu transparentes, sont rendues plus franches.
 */
private fun vivid(p: Float): ColorFilter {
    val m = ColorMatrix().apply { setToSaturation(SATURATION * p) }
    m *= ColorMatrix().apply { setToScale(0.92f, 0.92f, 0.92f, 1.2f) }
    return ColorFilter.colorMatrix(m)
}

/** Température arrondie ; au-dessous de zéro, un tiret demi-cadratin, comme le veut la typographie française. */
private fun degrees(t: Double): String {
    val n = t.roundToInt()
    return if (n < 0) "–${abs(n)}" else "$n"
}

private fun spoken(t: Double): String = "${t.roundToInt()}"

// ---------------------------------------------------------------- Basket

/** Ce qui distingue le bandeau d'un sport : titre, logo au crayon de l'appli, couleur, graine du coloriage, appli à ouvrir. */
private class GameLook(
    val title: String,
    @DrawableRes val logo: Int,
    val color: Color,
    val seed: Int,
    val app: String,
)

private val BASKET_LOOK = GameLook("Basket", R.drawable.crayon_basket, Color(0xFFE0702A), 7, "Basket Live")
private val LOL_LOOK = GameLook("LoL", R.drawable.crayon_lol, Color(0xFF7A5BC4), 11, "LoL Live")

/**
 * Les matchs en direct, puis les résultats de la nuit (matchs terminés), dans un bandeau colorié (orange
 * comme un ballon pour Basket Live, violet pour LoL Live), le logo au crayon de l'appli devant le titre ;
 * rend le bas du bandeau.
 */
@Composable
private fun GameSection(y0: Float, look: GameLook, lines: List<LiveLine>, onOpen: () -> Unit): Float {
    val y = y0 + BANNER_PAD
    val b = y + 17
    val end = y + 45 + ROW_PITCH * (lines.size - 1) + 7 + BANNER_PAD
    Box(Modifier.at(BANNER_X, y0).size(BANNER_W.dp, (end - y0).dp).crayonBanner(look.color, seed = look.seed))
    Image(
        painterResource(look.logo),
        contentDescription = null,
        modifier = Modifier.at(24, b - 21).size(28.dp),
    )
    Text(look.title, style = style(TITLE, face = Face.BOLD), modifier = Modifier.at(56, top(b, TITLE, face = Face.BOLD)))
    if (lines.any { it.game.live }) RightText("en direct", style(LABEL, color = Live), top(b, LABEL))

    val rows = rememberRows(lines)
    rows.forEachIndexed { i, row -> GameRow(y + 45 + ROW_PITCH * i, row) }
    TapZone(
        "Ouvrir ${look.app} : " + lines.joinToString(" ; ") { l ->
            val winner = BasketShare.winner(l.game)?.let { ", victoire des ${it.name}" }.orEmpty()
            "${l.game.away.name} ${l.game.away.score ?: 0}, ${l.game.home.name} ${l.game.home.score ?: 0}, " +
                BasketShare.spokenStatus(l.game) + winner
        },
        Modifier.at(BANNER_X, y0).size(BANNER_W.dp, (end - y0).dp),
        onOpen,
    )
    return end
}

/** Bandeaux coloriés : de 12 à 400, un peu plus larges que le texte (22 à 390) ; marge intérieure en haut et en bas. */
private const val BANNER_X = 12f
private const val BANNER_W = 388f
private const val BANNER_PAD = 9f

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
            // Victoire ou défaite : un coup de crayon de sa couleur, comme surligné.
            val mark = if (game.finished && row.statusFace == Face.BOLD) Modifier.crayonMark(row.statusColor, game.id.hashCode()) else Modifier
            PaperText(row.status, style = style(STATUS, face = row.statusFace, color = row.statusColor), softWrap = false, modifier = mark)
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

// ---------------------------------------------------------------- Messages

/** Bandeau d'une notification : [NOTE_H] de haut, [NOTE_GAP] entre deux. */
private const val NOTE_H = 54f
private const val NOTE_GAP = 8f

/** Pastille du logo, à gauche du bandeau. */
private const val NOTE_DISC = 36f

/**
 * Les messages reçus, un bandeau colorié par notification à la couleur de son appli (au plus
 * [MessageApps.MAX_NOTES], les plus récents), puis « et 2 autres » s'il y en a plus ; rend le bas.
 */
@Composable
private fun NotesSection(y0: Float, notes: List<UnlockNote>, today: LocalDate, onOpen: (UnlockNote) -> Unit): Float {
    val shown = notes.take(MessageApps.MAX_NOTES)
    shown.forEachIndexed { i, note -> NoteBanner(y0 + (NOTE_H + NOTE_GAP) * i, note, today, onOpen) }
    var end = y0 + (NOTE_H + NOTE_GAP) * shown.size - NOTE_GAP
    val more = notes.size - shown.size
    if (more > 0) {
        Text(
            "et $more autre${if (more > 1) "s" else ""} notification${if (more > 1) "s" else ""}",
            style = style(14.5f, 18f, color = PaperColors.Soft).copy(textAlign = TextAlign.End),
            modifier = Modifier.at(22, top(end + 19, 14.5f, 18f)).width(368.dp),
        )
        end += 24
    }
    return end
}

/**
 * Une notification : le logo au crayon de l'appli dans une pastille blanche cerclée de sa couleur (son
 * initiale s'il n'y a pas de logo), le titre en gras, l'appli à sa couleur et l'heure à droite,
 * puis le texte sur une ligne. Appui : déverrouiller et l'ouvrir.
 */
@Composable
private fun NoteBanner(y: Float, note: UnlockNote, today: LocalDate, onOpen: (UnlockNote) -> Unit) {
    val color = Color(note.app.color)
    val ink = inkOf(color)
    Box(
        Modifier
            .at(BANNER_X, y)
            .size(BANNER_W.dp, NOTE_H.dp)
            .crayonBanner(color, seed = note.key.hashCode())
            .semantics {
                contentDescription = "Ouvrir ${note.app.name} : ${note.title}, ${note.text}"
                role = Role.Button
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) { onOpen(note) },
    )
    val discTop = y + (NOTE_H - NOTE_DISC) / 2
    Box(
        Modifier
            .at(20, discTop)
            .size(NOTE_DISC.dp)
            .background(PaperColors.Paper, CircleShape)
            .drawBehind {
                drawCircle(color.copy(alpha = 0.8f), radius = size.minDimension / 2 - 0.8.dp.toPx(), style = Stroke(1.6.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        val logo = CrayonLogos.ofPackage(note.packageName)
        if (logo != null) {
            Image(painterResource(logo), contentDescription = null, modifier = Modifier.size((NOTE_DISC * 0.78f).dp))
        } else {
            Text(note.app.name.take(1), style = style(18f, face = Face.BOLD, color = ink), softWrap = false)
        }
    }

    // Première ligne : titre (ou l'appli s'il n'y en a pas) à gauche, appli et heure à droite.
    val left = 64f
    val first = y + 22
    val second = y + 43
    val meta = buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append(note.app.name) }
        withStyle(SpanStyle(color = PaperColors.Soft)) { append(" · ${noteTime(note.time, today)}") }
    }
    val metaStyle = style(13f)
    val metaWidth = textWidth(meta, metaStyle)
    Box(Modifier.at(22, top(first, 13f)).width(368.dp)) {
        Text(meta, style = metaStyle, softWrap = false, modifier = Modifier.align(Alignment.TopEnd))
    }
    Text(
        note.title.ifEmpty { note.app.name },
        style = style(16.5f, face = Face.BOLD),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(left, top(first, 16.5f, face = Face.BOLD)).width((390f - left - metaWidth - 8f).coerceAtLeast(40f).dp),
    )
    if (note.text.isNotEmpty()) {
        Text(
            note.text.replace('\n', ' '),
            style = style(15.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.at(left, top(second, 15.5f)).width((390f - left).dp),
        )
    }
}

/** Heure d'une notification : « 16:32 » aujourd'hui, « hier », sinon « 12 oct. ». */
private fun noteTime(ms: Long, today: LocalDate): String {
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
    return when (at.toLocalDate()) {
        today -> at.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE))
        today.minusDays(1) -> "hier"
        else -> at.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE))
    }
}

// ---------------------------------------------------------------- Musique

/** Bandeau de la musique : [MUSIC_H] de haut, la pochette de [MUSIC_ART] de côté. */
private const val MUSIC_H = 62f
private const val MUSIC_ART = 46f

/** Les trois boutons (précédent, lecture ou pause, suivant), de [MUSIC_BUTTON] de côté chacun. */
private const val MUSIC_BUTTON = 34f

/**
 * La musique en cours, dans un bandeau colorié à la couleur du lecteur : la pochette (sinon le logo au crayon
 * du lecteur, ou une note), le titre en gras, l'artiste et le lecteur à sa couleur ; à droite,
 * précédent, lecture ou pause, suivant — ou, sur le fond d'écran qui ne reçoit pas les appuis ([controls]
 * faux), « en lecture » ou « en pause ». Appui sur le reste du bandeau : déverrouiller et ouvrir le lecteur.
 * Rend le bas du bandeau.
 */
@Composable
private fun MusicSection(y: Float, music: Music, controls: Boolean, onAction: (MusicAction) -> Unit): Float {
    val color = Color(musicColor(music.packageName))
    val ink = inkOf(color)
    Box(
        Modifier
            .at(BANNER_X, y)
            .size(BANNER_W.dp, MUSIC_H.dp)
            .crayonBanner(color, seed = music.title.hashCode())
            .semantics {
                contentDescription = "Ouvrir ${music.app} : ${music.title}, ${music.artist}"
                role = Role.Button
            }
            .clickable(remember { MutableInteractionSource() }, indication = null) { onAction(MusicAction.OPEN) },
    )

    // La pochette, sinon le logo du lecteur (ou une note) dans une pastille blanche cerclée de sa couleur.
    val artTop = y + (MUSIC_H - MUSIC_ART) / 2
    val art = music.art
    if (art != null) {
        val shape = RoundedCornerShape(6.dp)
        Image(
            art,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.at(20, artTop).size(MUSIC_ART.dp).clip(shape).border(1.dp, ink.copy(alpha = 0.5f), shape),
        )
    } else {
        Box(
            Modifier
                .at(20 + (MUSIC_ART - NOTE_DISC) / 2, y + (MUSIC_H - NOTE_DISC) / 2)
                .size(NOTE_DISC.dp)
                .background(PaperColors.Paper, CircleShape)
                .drawBehind {
                    drawCircle(color.copy(alpha = 0.8f), radius = size.minDimension / 2 - 0.8.dp.toPx(), style = Stroke(1.6.dp.toPx()))
                },
            contentAlignment = Alignment.Center,
        ) {
            val logo = CrayonLogos.ofPackage(music.packageName)
            Image(
                painterResource(logo ?: R.drawable.ms_music_note),
                contentDescription = null,
                colorFilter = if (logo == null) ColorFilter.tint(ink) else null,
                modifier = Modifier.size((NOTE_DISC * if (logo != null) 0.78f else 0.6f).dp),
            )
        }
    }

    val left = 20 + MUSIC_ART + 10
    val first = y + 26
    val second = y + 47
    val right = if (controls) {
        MusicButtons(y + (MUSIC_H - MUSIC_BUTTON) / 2, music.playing, ink, onAction)
        390 - 3 * MUSIC_BUTTON - 6
    } else {
        val label = if (music.playing) "en lecture" else "en pause"
        val labelStyle = style(LABEL, color = ink)
        RightText(label, labelStyle, top(first, LABEL))
        390 - textWidth(AnnotatedString(label), labelStyle) - 8
    }
    Text(
        music.title,
        style = style(16.5f, face = Face.BOLD),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(left, top(first, 16.5f, face = Face.BOLD)).width((right - left).coerceAtLeast(40f).dp),
    )
    val by = buildAnnotatedString {
        if (music.artist.isNotEmpty()) append("${music.artist} · ")
        withStyle(SpanStyle(color = ink)) { append(music.app) }
    }
    Text(
        by,
        style = style(15f, color = PaperColors.Soft),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.at(left, top(second, 15f)).width(((if (controls) right else 390f) - left).dp),
    )
    return y + MUSIC_H
}

/** Précédent, lecture ou pause, suivant : dessinés à l'encre du bandeau, collés à la marge de droite. */
@Composable
private fun MusicButtons(top: Float, playing: Boolean, ink: Color, onAction: (MusicAction) -> Unit) {
    val buttons = listOf(
        MusicAction.PREVIOUS to "Morceau précédent",
        MusicAction.TOGGLE to if (playing) "Pause" else "Lecture",
        MusicAction.NEXT to "Morceau suivant",
    )
    buttons.forEachIndexed { i, (action, label) ->
        Canvas(
            Modifier
                .at(390 - MUSIC_BUTTON * (3 - i), top)
                .size(MUSIC_BUTTON.dp)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                }
                .clickable(remember { MutableInteractionSource() }, indication = null) { onAction(action) },
        ) {
            val u = size.minDimension / MUSIC_BUTTON
            val c = center
            fun triangle(tipX: Float, baseX: Float, half: Float) = Path().apply {
                moveTo(baseX, c.y - half)
                lineTo(tipX, c.y)
                lineTo(baseX, c.y + half)
                close()
            }
            val color = ink.copy(alpha = 0.9f)
            when (action) {
                MusicAction.PREVIOUS -> {
                    drawRect(color, Offset(c.x - 7 * u, c.y - 6 * u), Size(2.2f * u, 12 * u))
                    drawPath(triangle(c.x - 4.5f * u, c.x + 6 * u, 6 * u), color)
                }
                MusicAction.NEXT -> {
                    drawPath(triangle(c.x + 4.5f * u, c.x - 6 * u, 6 * u), color)
                    drawRect(color, Offset(c.x + 4.8f * u, c.y - 6 * u), Size(2.2f * u, 12 * u))
                }
                else -> if (playing) {
                    drawRect(color, Offset(c.x - 5.5f * u, c.y - 7.5f * u), Size(4 * u, 15 * u))
                    drawRect(color, Offset(c.x + 1.5f * u, c.y - 7.5f * u), Size(4 * u, 15 * u))
                } else {
                    drawPath(triangle(c.x + 7.5f * u, c.x - 5.5f * u, 8 * u), color)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Figure du jour

/** Hauteur de la figure dans sa case de 368 unités de large ; plus bas, la page s'arrête vers [PAGE_BOTTOM]. */
private const val FIGURE_MAX = FH
private const val PAGE_BOTTOM = 884f

/** En dessous de cette hauteur, la figure ne se lit plus : la page s'en passe. */
private const val FIGURE_MIN = 110f

/**
 * Quand la page a de la place (pas de messages, peu de sections), la figure grandit, jusqu'à toute la
 * largeur de la page, en gardant au moins [FIGURE_AIR] d'air au-dessus et au-dessous.
 */
private const val FIGURE_GROW = PAPER_WIDTH / FW
private const val FIGURE_AIR = 24f

/** Légendes des figures (« Figure 2 – Nom », puis le (i)) : 14 sur 18. */
private const val CAPTION = 14f
private const val CAPTION_LINE = 18f
private const val CAPTION_H = 20f
private const val INFO_MARK = 15f

/**
 * La figure du jour dessinée au crayon, et sa légende dessous, « Figure 2 – Nom » suivie d'un petit (i),
 * centrées dans la place qui reste sous les sections, la figure agrandie si cette place le permet. Toucher la légende montre l'explication
 * mathématique sur une fiche posée sur la figure ; la toucher de nouveau (ou la fiche) la range. Elle
 * est rangée à chaque réveil.
 */
@Composable
private fun FigureSection(y0: Float, figure: DailyFigure, number: Int, infoShown: Boolean) {
    var open by remember(figure.spec.key, figure.variant) { mutableStateOf(infoShown) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    val awake = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(awake) { if (!awake && !infoShown) open = false }

    val room = PAGE_BOTTOM - y0
    val grow = ((room - CAPTION_H - 10f - 2 * FIGURE_AIR) / FIGURE_MAX).coerceAtMost(FIGURE_GROW)
    val w = if (grow > 1f) FW * grow else FW
    val h = if (grow > 1f) FIGURE_MAX * grow else minOf(FIGURE_MAX, room - CAPTION_H - 10f)
    if (h < FIGURE_MIN) return
    val top = y0 + (room - (h + 10f + CAPTION_H)) / 2f
    FigureArt(figure, Modifier.at((PAPER_WIDTH - w) / 2f, top).size(w.dp, h.dp))

    val caption = "Figure $number – ${figure.title}"
    // Sans explication rédigée, pas de (i) : la légende n'est que le nom.
    val hasInfo = figure.info.isNotEmpty()
    val toggle = { open = hasInfo && !open }
    Box(Modifier.at(22, top + h + 10f).size(368.dp, CAPTION_H.dp), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .semantics { contentDescription = if (open) "Ranger l'explication" else "Explication : ${figure.title}" }
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = toggle),
        ) {
            Text(caption, style = style(CAPTION, CAPTION_LINE), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 340.dp))
            if (hasInfo) {
                Spacer(Modifier.width(7.dp))
                InfoMark(open)
            }
        }
    }
    if (open && hasInfo) InfoCard(figure.info, top, h, toggle)
}

/** Le petit (i) : un cercle au crayon fin et un i gras ; plein d'encre quand l'explication est ouverte. */
@Composable
private fun InfoMark(open: Boolean) {
    Box(
        Modifier
            .size(INFO_MARK.dp)
            .background(if (open) PaperColors.Ink else Color.Transparent, CircleShape)
            .drawBehind { drawCircle(PaperColors.Ink, radius = size.minDimension / 2 - 0.6.dp.toPx(), style = Stroke(1.1.dp.toPx())) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "i",
            style = style(11.5f, 12f, Face.BOLD, if (open) PaperColors.Paper else PaperColors.Ink),
            softWrap = false,
        )
    }
}

/** L'explication, sur une fiche de papier posée sur la figure ; sa taille se règle pour qu'elle tienne. */
@Composable
private fun InfoCard(text: String, top: Float, height: Float, onClose: () -> Unit) {
    val pad = 14f
    val size = fittingSize(text, 368f - 2 * pad, height - 2 * pad)
    Box(
        Modifier
            .at(22, top)
            .size(368.dp, height.dp)
            .background(PaperColors.Paper, RoundedCornerShape(10.dp))
            .crayonBanner(InfoBlue, seed = 3, radius = 10f)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Ranger", onClick = onClose)
            .padding(pad.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style(size, size * 1.32f).copy(textAlign = TextAlign.Start, lineBreak = LineBreak.Paragraph))
    }
}

/** Bleu pâle de la fiche d'explication. */
private val InfoBlue = Color(0xFF4A8BD6)

/** La plus grande taille, de 15,5 à 10 unités, à laquelle [text] tient dans [width] × [height]. */
@Composable
private fun fittingSize(text: String, width: Float, height: Float): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, width, height, density) {
        val px = with(density) { width.dp.roundToPx() }
        var size = 15.5f
        while (size > 10f) {
            val laid = measurer.measure(AnnotatedString(text), style(size, size * 1.32f), constraints = Constraints(maxWidth = px))
            if (with(density) { laid.size.height.toDp().value } <= height) break
            size -= 0.5f
        }
        size
    }
}

/**
 * Morceaux de texte de l'état d'un match : exposants (à [supRatio] em, posés par [PaperText]) ;
 * [nowrap] : espaces insécables. Android permet une coupure devant un exposant : un gluon (U+2060,
 * sans chasse) l'interdit.
 */
private fun segs(list: List<Seg>, supRatio: Float, nowrap: Boolean): AnnotatedString = buildAnnotatedString {
    for (seg in list) {
        withStyle(SpanStyle(fontStyle = if (seg.italic) FontStyle.Italic else FontStyle.Normal)) {
            append(if (nowrap) seg.text.replace(' ', '\u00A0') else seg.text)
        }
        if (seg.sup.isNotEmpty()) {
            append(WJ)
            withStyle(SpanStyle(fontSize = supRatio.em, baselineShift = BaselineShift.Superscript)) { append(seg.sup) }
            append(WJ)
        }
    }
}

/** Gluon (WORD JOINER) : invisible, sans chasse, interdit la coupure de part et d'autre. */
private const val WJ = '\u2060'

// ---------------------------------------------------------------- Outils

/** Texte calé à droite sur la marge (x = 390), son haut en [top]. */
@Composable
private fun RightText(
    text: String,
    style: TextStyle,
    top: Float,
    modifier: Modifier = Modifier,
    over: SketchText? = null,
    key: String = "",
    maxLines: Int = Int.MAX_VALUE,
) {
    Box(Modifier.at(22, top).width(368.dp)) {
        Text(
            text,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = over?.layout(key) ?: {},
            modifier = modifier.align(Alignment.TopEnd).then(over?.of(key) ?: Modifier),
        )
    }
}

/** Largeur de [text] sur une ligne, en unités. */
@Composable
private fun textWidth(text: AnnotatedString, style: TextStyle): Float {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, style, density) {
        with(density) { measurer.measure(text, style, softWrap = false).size.width.toDp().value }
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
