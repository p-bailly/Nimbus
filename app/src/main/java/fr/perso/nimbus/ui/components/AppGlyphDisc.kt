package fr.perso.nimbus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.CrayonLogos
import fr.perso.nimbus.data.Glyph
import fr.perso.nimbus.data.GlyphChoice
import fr.perso.nimbus.data.GlyphChoices
import fr.perso.nimbus.data.GlyphFactory
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperTextBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pastille « papier » d'une app, comme sur les pages d'accueil papier : disque blanc cerclé d'encre
 * et silhouette monochrome de l'app (voir [GlyphFactory]), ou son initiale en italique quand l'icône
 * n'a pas de silhouette. [dot] : notifications, en petit point d'encre. Sans app (place vide) :
 * cercle en pointillés et [placeholder] en italique.
 */
@Composable
fun AppGlyphDisc(
    app: AppEntry?,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = PaperColors.Ink,
    ring: Dp = 1.2.dp,
    glyphScale: Float = 0.5f,
    dot: Boolean = false,
    placeholder: String? = null,
    /** Motif dans les couleurs de l'icône quand il les a gardées (voir [AppGlyph]). */
    colored: Boolean = false,
) {
    Box(modifier.size(size).then(if (app != null) Modifier.semantics { contentDescription = app.label } else Modifier)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (app == null) {
                        Modifier
                            .background(Color.White, CircleShape)
                            .dashedRing(color, ring)
                    } else {
                        Modifier
                            .background(Color.White, CircleShape)
                            .border(ring, color, CircleShape)
                    },
                ),
        ) {
            if (app != null) {
                AppGlyph(app, size * glyphScale, color, colored = colored)
            } else if (placeholder != null) {
                val fontSize = with(LocalDensity.current) { (size * 0.42f).toSp() }
                Text(
                    placeholder,
                    style = PaperTextBase.copy(fontSize = fontSize, lineHeight = fontSize, color = color, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
                    maxLines = 1,
                )
            }
        }
        if (dot) {
            // Notification : petit point d'encre posé contre le cercle, en haut à droite, comme un
            // appel de note (sans mordre sur le cercle).
            val dotSize = size * 0.2f
            val reach = (size / 2 + dotSize / 2 + 1.5.dp) * 0.7071f
            Box(
                Modifier
                    .offset(x = size / 2 + reach - dotSize / 2, y = size / 2 - reach - dotSize / 2)
                    .size(dotSize)
                    .background(PaperColors.Ink, CircleShape),
            )
        }
    }
}

/** Motifs choisis pour les apps (voir [GlyphChoices]), fournis par la racine de l'interface. */
val LocalGlyphChoices = staticCompositionLocalOf { GlyphChoices() }

/**
 * Motif de [app] dans un carré de [px] pixels : celui choisi pour l'app, sinon [fallback] (motif de
 * sa place), sinon sa silhouette. Calculé en arrière-plan (null en attendant), ou tout de suite si [sync].
 */
@Composable
fun rememberAppGlyph(app: AppEntry?, px: Int, fallback: GlyphChoice? = null, sync: Boolean = false): Glyph? {
    if (app == null) return null
    val context = LocalContext.current
    val (chosen, other) = LocalGlyphChoices.current.resolve(app)
    val choice = chosen ?: fallback
    fun make(): Glyph = runCatching { GlyphFactory.get(context).glyph(app, px, choice, other) }
        .getOrElse { Glyph.Letters(app.label.take(1)) }
    if (sync) return remember(app.key, app.label, px, choice, other?.key) { make() }
    return produceState<Glyph?>(null, app.key, app.label, px, choice, other?.key) {
        value = withContext(Dispatchers.Default) { make() }
    }.value
}

/**
 * Couleur propre de [app] (voir [GlyphFactory.color]), avec le motif choisi, sinon [fallback] (motif
 * de sa place) ; l'encre en attendant.
 */
@Composable
fun rememberAppColor(app: AppEntry, fallback: GlyphChoice? = null): Color {
    val context = LocalContext.current
    val (chosen, other) = LocalGlyphChoices.current.resolve(app)
    val choice = chosen ?: fallback
    return produceState(PaperColors.Ink, app.key, choice, other?.key) {
        value = withContext(Dispatchers.Default) {
            runCatching { Color(GlyphFactory.get(context).color(app, choice, other)) }.getOrDefault(PaperColors.Ink)
        }
    }.value
}

/** Logo au crayon de [app] (voir [CrayonLogos]), ou null : l'app garde son motif. */
@Composable
fun rememberCrayonLogo(app: AppEntry?): Int? =
    if (app == null) null else remember(app.packageName, app.label) { CrayonLogos.of(app) }

/**
 * Logo au crayon dans le carré d'un motif de [size] : un peu plus grand que le carré (son dessin n'en
 * occupe que 89 %, et ses couleurs le rendent plus fin qu'une silhouette pleine), centré.
 */
@Composable
fun CrayonLogo(@DrawableRes logo: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(painterResource(logo), contentDescription = null, modifier = Modifier.requiredSize(size * CRAYON_LOGO_SCALE))
    }
}

/** Taille d'un logo au crayon, rapportée au carré du motif qu'il remplace. */
const val CRAYON_LOGO_SCALE = 1.2f

/**
 * Logo au crayon de l'app (voir [CrayonLogos]) ; sinon sa silhouette teinte de [tint] dans un carré de
 * [size], ou ses lettres en italique ; avec [colored], dans les couleurs de l'icône quand le motif les a
 * gardées (applis Google).
 */
@Composable
fun AppGlyph(app: AppEntry, size: Dp, tint: Color, modifier: Modifier = Modifier, colored: Boolean = false) {
    rememberCrayonLogo(app)?.let { logo ->
        CrayonLogo(logo, size, modifier)
        return
    }
    val density = LocalDensity.current
    val px = with(density) { size.roundToPx() }.coerceAtLeast(8)
    val glyph = rememberAppGlyph(app, px)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (glyph) {
            is Glyph.Mask -> {
                val colors = glyph.colors
                if (colored && colors != null) {
                    Image(bitmap = colors, contentDescription = null, modifier = Modifier.size(size))
                } else {
                    Image(
                        bitmap = glyph.bitmap,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                        modifier = Modifier.size(size),
                    )
                }
            }
            is Glyph.Letters -> {
                val fontSize = with(density) { (size * if (glyph.text.length > 1) 0.78f else 1.02f).toSp() }
                Text(
                    glyph.text,
                    style = PaperTextBase.copy(fontSize = fontSize, lineHeight = with(density) { size.toSp() }, color = tint, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                )
            }
            null -> Unit
        }
    }
}

/** Cercle en pointillés (place sans app). */
private fun Modifier.dashedRing(color: Color, width: Dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawCircle(
        color = color,
        radius = size.minDimension / 2f - stroke / 2f,
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx()))),
    )
}
