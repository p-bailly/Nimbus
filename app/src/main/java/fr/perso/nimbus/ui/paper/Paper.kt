package fr.perso.nimbus.ui.paper

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.Glyph
import fr.perso.nimbus.data.GlyphFactory
import fr.perso.nimbus.data.PaperFamily
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperTextBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

// Les pages « papier » sont dessinées dans un repère de 412 × 916 unités (celui des maquettes) :
// une unité vaut un dp du Pixel visé ; [PaperCanvas] met le repère à l'échelle de l'écran.

/** Largeur et hauteur du repère des pages, en unités. */
const val PAPER_WIDTH = 412
const val PAPER_HEIGHT = 916

/** Bande du repère qui doit rester entre la barre d'état et la barre de navigation. */
private const val SAFE_TOP = 30
private const val SAFE_BOTTOM = 890

/** Encre d'une famille. */
val PaperFamily.color: Color get() = Color(argb)

/** Environnement d'une page : app de chaque place, pastilles de notification, actions. */
@Immutable
class PaperEnv(
    val apps: Map<PaperSlot, AppEntry?>,
    /** Notifications par paquet. */
    val badges: Map<String, Int> = emptyMap(),
    val onOpen: (PaperSlot) -> Unit = {},
    /** Appui long sur une place, avec sa position à l'écran (pour le pop-up). */
    val onActions: (PaperSlot, Rect) -> Unit = { _, _ -> },
    /** Heure figée (aperçus, captures) ; null = heure du téléphone. */
    val fixedTime: LocalDateTime? = null,
    /** Barres système (dp de l'écran) imposées, pour les captures ; null = celles de la fenêtre. */
    val fixedInsets: Pair<Float, Float>? = null,
    /** Animations (Fourier) : false pour les captures. */
    val animate: Boolean = true,
) {
    fun app(slot: PaperSlot): AppEntry? = apps[slot]

    /** Nom affiché d'une place : celui de l'app (renommage compris), sinon celui de la maquette. */
    fun name(slot: PaperSlot): String = apps[slot]?.label ?: slot.title

    companion object {
        val Empty = PaperEnv(apps = emptyMap())
    }
}

val LocalPaper = staticCompositionLocalOf { PaperEnv.Empty }

// ---------------------------------------------------------------- Repère et en-tête

/**
 * Fond papier sur tout l'écran et repère de 412 × 916 unités, mis à l'échelle pour que la bande
 * utile (30 à 890) tienne entre les barres système, et centré horizontalement. Dans [content],
 * 1.dp = 1 unité et 1.sp = 1 unité (la taille de police du système ne déforme pas la page).
 */
@Composable
fun PaperCanvas(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val env = LocalPaper.current
    BoxWithConstraints(modifier.fillMaxSize().background(PaperColors.Paper)) {
        val density = LocalDensity.current
        val bars = WindowInsets.systemBars
        val topPx = env.fixedInsets?.first?.let { with(density) { it.dp.toPx() } } ?: bars.getTop(density).toFloat()
        val bottomPx = env.fixedInsets?.second?.let { with(density) { it.dp.toPx() } } ?: bars.getBottom(density).toFloat()
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val scale = min(w / PAPER_WIDTH, (h - topPx - bottomPx) / (SAFE_BOTTOM - SAFE_TOP))
        val x = ((w - PAPER_WIDTH * scale) / 2f).roundToInt()
        val y = (topPx - SAFE_TOP * scale).roundToInt()
        CompositionLocalProvider(LocalDensity provides Density(scale, fontScale = 1f)) {
            Box(
                Modifier
                    .offset { IntOffset(x, y) }
                    .size(PAPER_WIDTH.dp, PAPER_HEIGHT.dp),
                content = content,
            )
        }
    }
}

/** Position absolue dans le repère de la page. */
fun Modifier.at(x: Number, y: Number): Modifier = offset(x.toFloat().dp, y.toFloat().dp)

/**
 * Page complète : titre courant (petites capitales) et numéro de page sur un filet, puis l'heure
 * et la date centrées. Le contenu commence vers y = 142.
 */
@Composable
fun PaperPage(runningHead: String, pageNumber: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    PaperCanvas(modifier) {
        PaperHeader(runningHead, pageNumber)
        content()
    }
}

@Composable
fun PaperHeader(runningHead: String, pageNumber: String) {
    val context = LocalContext.current
    val now by rememberPaperTime()
    val locale = Locale.FRANCE
    val fixed = LocalPaper.current.fixedTime != null
    val is24h = remember(fixed) { fixed || DateFormat.is24HourFormat(context) }
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm", locale))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale))

    val head = Paper.text(12f, 14f).copy(fontFeatureSettings = "smcp", letterSpacing = 0.08.em)
    Box(Modifier.at(22, 36).width(368.dp)) {
        Text(runningHead, style = head, modifier = Modifier.align(Alignment.CenterStart))
        Text(pageNumber, style = head, modifier = Modifier.align(Alignment.CenterEnd))
    }
    HRule(22, 55, 368, 0.6f)
    Text(
        time,
        style = Paper.text(44f, 44f).copy(textAlign = TextAlign.Center),
        modifier = Modifier
            .at(0, 66)
            .width(PAPER_WIDTH.dp)
            .semantics { contentDescription = "Heure : $time" }
            .clickable(onClickLabel = "Ouvrir l'horloge") { SystemActions.openAlarms(context) },
    )
    Text(
        date,
        style = Paper.text(13f, 16f).copy(fontFeatureSettings = "smcp", letterSpacing = 0.06.em, textAlign = TextAlign.Center),
        modifier = Modifier
            .at(0, 114)
            .width(PAPER_WIDTH.dp)
            .clickable(onClickLabel = "Ouvrir l'agenda") { SystemActions.openCalendar(context) },
    )
}

/** Heure de la page, rafraîchie à chaque minute (ou figée par [PaperEnv.fixedTime]). */
@Composable
fun rememberPaperTime(): State<LocalDateTime> {
    val fixed = LocalPaper.current.fixedTime
    return produceState(fixed ?: LocalDateTime.now(), fixed) {
        if (fixed != null) return@produceState
        while (true) {
            value = LocalDateTime.now()
            delay(60_000 - System.currentTimeMillis() % 60_000 + 20)
        }
    }
}

/** Filet horizontal d'épaisseur [thickness] unités. */
@Composable
fun HRule(x: Number, y: Number, width: Number, thickness: Float = 0.8f, color: Color = PaperColors.Rule) {
    Box(
        Modifier
            .at(x, y)
            .size(width.toFloat().dp, thickness.dp)
            .background(color),
    )
}

// ---------------------------------------------------------------- Texte

/** Styles de texte des pages. */
object Paper {
    val Ink = PaperColors.Ink
    val Soft = PaperColors.Soft

    /** Texte de [size] unités sur un interlignage de [line] unités. */
    fun text(size: Float, line: Float = size * 1.38f, color: Color = PaperColors.Ink): TextStyle =
        PaperTextBase.copy(fontSize = size.sp, lineHeight = line.sp, color = color)

    /** Corps du texte : 14,5 sur 20. */
    val body: TextStyle get() = text(14.5f, 20f)

    /** Légendes : 12,5 sur 16, centrées. */
    val caption: TextStyle get() = text(12.5f, 16f).copy(textAlign = TextAlign.Center)
}

/** Construction de formules : variables en italique, indices et exposants, petites capitales. */
class MathBuilder(val b: AnnotatedString.Builder) {
    operator fun String.unaryPlus() {
        b.append(this)
    }

    fun it(s: String) = b.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s) }
    fun bf(s: String) = b.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s) }
    fun sc(s: String) = b.withStyle(SpanStyle(fontFeatureSettings = "smcp")) { append(s) }
    fun color(c: Color, block: MathBuilder.() -> Unit) = b.withStyle(SpanStyle(color = c)) { MathBuilder(this).block() }
    fun italic(block: MathBuilder.() -> Unit) = b.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { MathBuilder(this).block() }
    fun bold(block: MathBuilder.() -> Unit) = b.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { MathBuilder(this).block() }

    /** Indice (romain par défaut, [italic] pour une variable : T_n). */
    fun sub(s: String, italic: Boolean = false) =
        b.withStyle(SpanStyle(fontSize = 0.69.em, baselineShift = SUB_SHIFT, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)) { append(s) }

    fun sup(s: String, italic: Boolean = false) =
        b.withStyle(SpanStyle(fontSize = 0.69.em, baselineShift = SUP_SHIFT, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)) { append(s) }

    /** Exposant composé (e^{it}) : [block] en petit, surélevé. */
    fun sup(block: MathBuilder.() -> Unit) =
        b.withStyle(SpanStyle(fontSize = 0.69.em, baselineShift = SUP_SHIFT)) { MathBuilder(this).block() }

    fun sub(block: MathBuilder.() -> Unit) =
        b.withStyle(SpanStyle(fontSize = 0.69.em, baselineShift = SUB_SHIFT)) { MathBuilder(this).block() }

    private companion object {
        // Comme Chrome (vertical-align super / sub) : la ligne de base monte de taille/3 + 1 et descend de
        // taille/5 + 1 ; BaselineShift compte en ascendantes de l'indice (0,69 em × 0,842), soit pour un texte
        // de 14,5 : 5,8 / 8,4 et 3,9 / 8,4.
        val SUP_SHIFT = BaselineShift(0.69f)
        val SUB_SHIFT = BaselineShift(-0.46f)
    }
}

/** Texte composé avec [MathBuilder] : `tex { it("V"); +"(a)" }`. */
fun tex(block: MathBuilder.() -> Unit): AnnotatedString = buildAnnotatedString { MathBuilder(this).block() }

// ---------------------------------------------------------------- Places des apps

/**
 * Zone d'une place : appui = ouvrir l'app, appui long = actions (ou choix de l'app si la place est
 * vide). [label] : description pour l'accessibilité (nom de l'app par défaut).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.slot(slot: PaperSlot, shape: Shape = CircleShape, label: String? = null): Modifier = composed {
    val env = LocalPaper.current
    val haptics = LocalHapticFeedback.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val name = label ?: env.name(slot)
    this
        .onGloballyPositioned { coords = it }
        .clip(shape)
        .combinedClickable(
            onClickLabel = "Ouvrir $name",
            onLongClickLabel = "Actions pour $name",
            onClick = { env.onOpen(slot) },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                env.onActions(slot, coords?.boundsInWindow() ?: Rect.Zero)
            },
        )
}

/** Motif monochrome de l'app d'une place, teint de [tint], dans un carré de [size] unités. */
@Composable
fun SlotGlyph(slot: PaperSlot, size: Float, tint: Color, modifier: Modifier = Modifier) {
    val env = LocalPaper.current
    val app = env.app(slot)
    val glyph = rememberGlyph(app, size)
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        when (glyph) {
            is Glyph.Mask -> Image(
                bitmap = glyph.bitmap,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(size.dp),
            )
            is Glyph.Letters -> Text(
                glyph.text,
                style = Paper.text(size * if (glyph.text.length > 1) 0.78f else 1.02f, size, tint)
                    .copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
                maxLines = 1,
            )
            null -> Unit
        }
    }
}

@Composable
private fun rememberGlyph(app: AppEntry?, size: Float): Glyph? {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.dp.roundToPx() }.coerceAtLeast(8)
    if (app == null) return null
    val state = produceState<Glyph?>(null, app.key, app.label, px) {
        value = withContext(Dispatchers.Default) {
            runCatching { GlyphFactory.get(context).glyph(app, px) }.getOrElse { Glyph.Letters(app.label.take(1)) }
        }
    }
    return state.value
}

enum class DiscStyle {
    /** Disque plein de la couleur, motif blanc. */
    FILL,
    /** Disque blanc cerclé de la couleur, motif de la couleur. */
    RING,
}

/**
 * Pastille ronde d'une place, centrée en ([cx], [cy]), de [size] unités de diamètre. Sans app, la
 * pastille est en pointillés avec l'initiale de la place : un appui permet de choisir l'app.
 */
@Composable
fun SlotDisc(
    slot: PaperSlot,
    cx: Float,
    cy: Float,
    size: Float = 36f,
    style: DiscStyle = DiscStyle.FILL,
    color: Color = slot.family.color,
    ring: Float = 1.5f,
    glyphScale: Float = 0.48f,
    glyphColor: Color? = null,
) {
    val env = LocalPaper.current
    val app = env.app(slot)
    val badge = app?.let { env.badges[it.packageName] } ?: 0
    Box(
        Modifier
            .at(cx - size / 2f, cy - size / 2f)
            .size(size.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .slot(slot)
                .then(
                    when {
                        app == null -> Modifier
                            .background(Color.White, CircleShape)
                            .dashedCircle(color, ring)
                        style == DiscStyle.FILL -> Modifier.background(color, CircleShape)
                        else -> Modifier
                            .background(Color.White, CircleShape)
                            .border(ring.dp, color, CircleShape)
                    },
                ),
        ) {
            if (app == null) {
                Text(
                    slot.title.take(if (slot.title.length <= 2) 2 else 1),
                    style = Paper.text(size * 0.42f, size * 0.5f, color).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
                )
            } else {
                SlotGlyph(slot, size * glyphScale, glyphColor ?: if (style == DiscStyle.FILL) Color.White else color)
            }
        }
        if (badge > 0) {
            // Pastille de notification : petit point d'encre, comme un appel de note.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(1.dp, (-1).dp)
                    .size((size * 0.24f).dp)
                    .background(PaperColors.Paper, CircleShape)
                    .padding(1.5.dp)
                    .background(PaperColors.Ink, CircleShape),
            )
        }
    }
}

/** Cercle en pointillés (place sans app). */
private fun Modifier.dashedCircle(color: Color, width: Float): Modifier = drawBehind {
    val stroke = width.dp.toPx()
    drawCircle(
        color = color,
        radius = size.minDimension / 2f - stroke / 2f,
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))),
    )
}

/** Ligne centrée avec son numéro d'équation à droite (« (1) »), sur la largeur du texte. */
@Composable
fun EquationLine(number: Int?, top: Float, style: TextStyle = Paper.body, x: Float = 22f, width: Float = 368f, content: @Composable () -> Unit) {
    Box(Modifier.at(x, top).width(width.dp)) {
        Box(Modifier.align(Alignment.Center).padding(horizontal = 28.dp)) { content() }
        if (number != null) Text("($number)", style = style, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

/** Rangée sans espace, alignée sur la ligne de base (fractions, parenthèses hautes…). */
@Composable
fun MathRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) { content() }
}
