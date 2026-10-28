package fr.perso.nimbus.ui.settings

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.IconShape
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.inkClickable

/** Marge latérale des contrôles posés directement dans la page. */
@Composable
private fun pickerPadding() = if (LocalPaperUi.current) PaperMargin else 24.dp

/** Légende sous une vignette : intitulé court, en gras et de la couleur de la page quand elle est choisie. */
@Composable
private fun PickerCaption(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    val paper = LocalPaperUi.current
    Text(
        text,
        style = if (paper) PaperStyle.serif(14f, 16f, if (selected) accentDark() else PaperColors.Ink, bold = selected)
        else MaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) accentText() else MaterialTheme.colorScheme.onSurface,
            lineHeight = 15.sp,
        ),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

/**
 * Cadre de sélection d'une vignette : liseré de la couleur de la page quand elle est choisie, transparent sinon
 * (la vignette ne bouge pas d'un état à l'autre). À faire suivre d'un `padding(3.dp)`.
 */
@Composable
private fun Modifier.pickerOutline(selected: Boolean, shape: Shape): Modifier =
    border(2.5.dp, if (selected) accentDark() else Color.Transparent, shape)

// ------------------------------------------------------------------------------------------ thèmes

private class ThemeSwatch(val mode: ThemeMode, val name: String, val bg: Color, val ink: Color, val accent: Color)

private val themeSwatches = listOf(
    ThemeSwatch(ThemeMode.AMBIENT, "Ambiant", Color(0xFFF6F9F8), Color(0xFF151A19), Color(0xFF1F6F66)),
    ThemeSwatch(ThemeMode.MATERIAL_YOU, "Material You", Color(0xFFFEF7FF), Color(0xFF1D1B20), Color(0xFF6750A4)),
    ThemeSwatch(ThemeMode.DARK, "Sombre", Color(0xFF0F1014), Color(0xFFE8EAEE), Color(0xFF9DB3E7)),
    ThemeSwatch(ThemeMode.LIGHT, "Clair", Color(0xFFF8F9FB), Color(0xFF161720), Color(0xFF2951A3)),
    ThemeSwatch(ThemeMode.PAPER, "Papier", PaperColors.Paper, PaperColors.Ink, PaperColors.Ink),
)

/** Les cinq thèmes en vignettes (trois puis deux) : choisir un thème l'applique tout de suite à la page. */
@Composable
fun ThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = pickerPadding(), vertical = 8.dp)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (row in themeSwatches.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                for (swatch in row) {
                    val supported = swatch.mode != ThemeMode.MATERIAL_YOU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    Column(
                        Modifier
                            .weight(1f)
                            .alpha(if (supported) 1f else 0.4f)
                            .inkClickable(enabled = supported, role = Role.RadioButton) { onSelect(swatch.mode) },
                    ) {
                        ThemeThumbnail(swatch, swatch.mode == selected)
                        PickerCaption(swatch.name, swatch.mode == selected)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ThemeThumbnail(swatch: ThemeSwatch, selected: Boolean) {
    val outline = if (swatch.mode == ThemeMode.PAPER) swatch.ink else MaterialTheme.colorScheme.outlineVariant
    val shape = if (swatch.mode == ThemeMode.PAPER) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
    Box(Modifier.pickerOutline(selected, shape).padding(3.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(shape)
                .background(swatch.bg)
                .border(1.dp, outline, shape),
        ) {
            val pad = 10.dp.toPx()
            val bars = listOf(0.50f to 7.dp.toPx() to 0.8f, 0.78f to 5.dp.toPx() to 0.35f, 0.62f to 5.dp.toPx() to 0.35f)
            var y = pad
            for ((spec, alpha) in bars) {
                val (frac, h) = spec
                drawRoundRect(
                    swatch.ink.copy(alpha = alpha),
                    topLeft = Offset(pad, y),
                    size = Size((size.width - 2 * pad) * frac, h),
                    cornerRadius = CornerRadius(h / 2),
                )
                y += h + 5.dp.toPx()
            }
            val r = 7.dp.toPx()
            val center = Offset(size.width - pad - r, size.height - pad - r)
            if (swatch.mode == ThemeMode.PAPER) {
                drawRect(swatch.ink, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(1.dp.toPx()))
            } else {
                drawCircle(swatch.accent, r, center)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ formes

private fun iconShapeOf(shape: IconShape): Shape = when (shape) {
    IconShape.SQUIRCLE -> RoundedCornerShape(36)
    IconShape.CIRCLE -> CircleShape
    IconShape.ROUNDED -> RoundedCornerShape(22)
    IconShape.TEARDROP -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 12)
    IconShape.SQUARE -> RoundedCornerShape(6)
    IconShape.SYSTEM -> RoundedCornerShape(30)
}

/** Les formes d'icônes en pastilles (la forme choisie est entourée) ; l'aperçu de la page montre les vraies icônes. */
@Composable
fun IconShapePicker(selected: IconShape, onSelect: (IconShape) -> Unit) {
    val paper = LocalPaperUi.current
    val dark = accentDark()
    val text = accentText()
    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (shape in IconShape.entries) {
            val on = shape == selected
            val frame = if (paper) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(16.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp, 56.dp)
                    .clip(frame)
                    .then(if (on) Modifier.border(1.5.dp, dark, frame).background(dark.copy(alpha = 0.12f)) else Modifier)
                    .semantics { contentDescription = shape.label }
                    .inkClickable(role = Role.RadioButton) { onSelect(shape) },
            ) {
                val swatch = iconShapeOf(shape)
                Box(
                    Modifier
                        .size(32.dp)
                        .then(
                            when {
                                shape == IconShape.SYSTEM -> Modifier.dashedOutline(text, swatch)
                                paper -> Modifier.border(1.2.dp, dark, swatch)
                                else -> Modifier.background(text.copy(alpha = 0.85f), swatch)
                            },
                        ),
                )
            }
        }
    }
}

/** Contour en tirets (la « forme du système », qui dépend du téléphone). */
private fun Modifier.dashedOutline(color: Color, shape: Shape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    drawPath(
        path,
        color,
        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
    )
}

// ------------------------------------------------------------------------------------------ styles d'accueil

/** Intitulé court d'un style, pour sa vignette. */
private fun HomeStyle.short(): String = when (this) {
    HomeStyle.GRID -> "Grille"
    HomeStyle.GROUPES -> "Groupes"
    HomeStyle.VORONOI -> "Voronoï"
    HomeStyle.ENSEMBLES -> "Ensembles"
    HomeStyle.FOURIER -> "Fourier"
}

/** Les cinq styles d'accueil en vignettes dessinées (grille de points, groupes, cellules, ensembles, courbe). */
@Composable
fun HomeStylePicker(selected: HomeStyle, onSelect: (HomeStyle) -> Unit) {
    val paper = LocalPaperUi.current
    val ink = if (paper) accentDark() else accentText()
    val line = if (paper) PaperColors.Ink else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (paper) PaperMargin else 16.dp, vertical = 8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (style in HomeStyle.entries) {
            val on = style == selected
            val shape = if (paper) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(10.dp)
            Column(
                Modifier
                    .weight(1f)
                    .inkClickable(role = Role.RadioButton) { onSelect(style) },
            ) {
                Box(Modifier.pickerOutline(on, shape).padding(3.dp)) {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .clip(shape)
                            .background(if (on) accentDark().copy(alpha = 0.1f) else Color.Transparent)
                            .border(if (paper) 0.6.dp else 1.dp, line, shape)
                            .padding(4.dp),
                    ) {
                        drawHomeStyle(style, ink, 1.3.dp.toPx())
                    }
                }
                PickerCaption(style.short(), on)
            }
        }
    }
}

/**
 * Dessin d'un style d'accueil dans un repère de 60 × 92, mis à l'échelle et centré : une grille de points,
 * des groupes cerclés, des cellules de Voronoï, des ensembles qui se recoupent ou une courbe de Fourier.
 */
fun DrawScope.drawHomeStyle(style: HomeStyle, color: Color, stroke: Float) {
    val k = minOf(size.width / 60f, size.height / 92f)
    val dx = (size.width - 60f * k) / 2f
    val dy = (size.height - 92f * k) / 2f
    fun p(x: Float, y: Float) = Offset(dx + x * k, dy + y * k)
    val line = Stroke(stroke)
    fun dot(x: Float, y: Float) = drawCircle(color, radius = stroke * 1.9f, center = p(x, y))
    fun ring(x: Float, y: Float, r: Float) = drawCircle(color, radius = r * k, center = p(x, y), style = line)
    when (style) {
        HomeStyle.GRID -> {
            for (y in listOf(14f, 28f, 42f, 56f, 70f)) for (x in listOf(12f, 24f, 36f, 48f)) dot(x, y)
            drawLine(color, p(8f, 82f), p(52f, 82f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        HomeStyle.GROUPES -> {
            ring(22f, 26f, 15f); ring(40f, 52f, 14f); ring(20f, 76f, 11f)
            for ((x, y) in listOf(17f to 22f, 27f to 22f, 22f to 31f, 36f to 48f, 45f to 48f, 40f to 57f, 16f to 76f, 24f to 76f)) dot(x, y)
        }
        HomeStyle.VORONOI -> {
            drawRect(color, p(4f, 4f), Size(52f * k, 84f * k), style = line)
            val lines = listOf(
                listOf(34f to 4f, 26f to 34f, 4f to 28f),
                listOf(26f to 34f, 36f to 52f, 56f to 44f),
                listOf(36f to 52f, 30f to 70f, 4f to 62f),
                listOf(30f to 70f, 38f to 88f),
                listOf(30f to 70f, 56f to 76f),
            )
            for (poly in lines) for (i in 0 until poly.size - 1) {
                drawLine(color, p(poly[i].first, poly[i].second), p(poly[i + 1].first, poly[i + 1].second), stroke)
            }
            for ((x, y) in listOf(16f to 14f, 44f to 20f, 14f to 46f, 46f to 62f, 18f to 78f, 48f to 80f)) dot(x, y)
        }
        HomeStyle.ENSEMBLES -> {
            ring(24f, 34f, 20f); ring(38f, 34f, 20f); ring(31f, 58f, 20f)
            for ((x, y) in listOf(14f to 30f, 48f to 30f, 31f to 22f, 31f to 38f, 31f to 66f)) dot(x, y)
        }
        HomeStyle.FOURIER -> {
            val curve = Path().apply {
                moveTo(p(4f, 46f).x, p(4f, 46f).y)
                cubicTo(p(10f, 12f).x, p(10f, 12f).y, p(20f, 12f).x, p(20f, 12f).y, p(28f, 46f).x, p(28f, 46f).y)
                cubicTo(p(36f, 80f).x, p(36f, 80f).y, p(46f, 80f).x, p(46f, 80f).y, p(56f, 46f).x, p(56f, 46f).y)
            }
            drawPath(curve, color, style = line)
            ring(14f, 80f, 6f)
            for ((x, y) in listOf(16f to 20f, 28f to 46f, 40f to 72f, 52f to 52f)) dot(x, y)
        }
    }
}

// ------------------------------------------------------------------------------------------ aperçu de la grille

/** Une couleur de pastille par case remplie de la mini-grille (sans icônes réelles). */
private val cellTones = listOf(
    SettingsColors.Green, SettingsColors.Blue, SettingsColors.Orange, SettingsColors.Red, SettingsColors.Purple, SettingsColors.Teal,
)

/**
 * Mini accueil en grille : [columns] × [rows] cases dont une sur deux environ porte une icône (les vraies
 * icônes de [icons] quand on en a), puis le dock de [dock] icônes. Se redessine à chaque réglage.
 */
@Composable
fun HomeGridPreview(columns: Int, rows: Int, dock: Int, icons: List<ImageBitmap>, modifier: Modifier = Modifier) {
    val paper = LocalPaperUi.current
    val accent = LocalSettingsAccent.current
    val dark = accentDark()
    val ink = if (paper) PaperColors.Ink else MaterialTheme.colorScheme.onSurface
    val frame = if (paper) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(26.dp)
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(160.dp, 244.dp)
                .clip(frame)
                .then(
                    if (paper) Modifier.border(0.8.dp, dark, frame)
                    else Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    lerp(MaterialTheme.colorScheme.surface, accent, 0.32f),
                                    lerp(MaterialTheme.colorScheme.surfaceVariant, accent, 0.08f),
                                ),
                            ),
                        )
                        .border(1.dp, lerp(MaterialTheme.colorScheme.outlineVariant, accent, 0.35f), frame),
                ),
        ) {
            val pad = 10.dp.toPx()
            val gap = 4.dp.toPx()
            val dockH = 24.dp.toPx()
            val gridTop = 12.dp.toPx()
            // Entre la grille et le dock, un petit espace.
            val gridH = size.height - gridTop - dockH - pad - 14.dp.toPx()
            val cellW = (size.width - 2 * pad - gap * (columns - 1)) / columns
            val cellH = (gridH - gap * (rows - 1)) / rows
            fun cell(i: Int, x: Float, y: Float, w: Float, h: Float, tone: Color, image: ImageBitmap?) {
                val r = CornerRadius(minOf(w, h) * 0.32f)
                when {
                    // Papier : des pastilles cerclées, comme sur les accueils papier (pas les vraies icônes).
                    paper -> drawCircle(tone, minOf(w, h) / 2f, Offset(x + w / 2f, y + h / 2f), style = Stroke(1.dp.toPx()))
                    image != null -> drawImage(
                        image,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(image.width, image.height),
                        dstOffset = IntOffset(x.toInt(), y.toInt()),
                        dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
                        filterQuality = FilterQuality.Medium,
                    )
                    else -> drawRoundRect(tone, Offset(x, y), Size(w, h), r)
                }
            }
            for (i in 0 until columns * rows) {
                val cx = pad + (i % columns) * (cellW + gap)
                val cy = gridTop + (i / columns) * (cellH + gap)
                val filled = (i * 37 + 11) % 7 < 3
                if (filled) {
                    val tone = cellTones[(i * 5) % cellTones.size]
                    cell(i, cx, cy, cellW, cellH, tone, icons.takeIf { it.isNotEmpty() }?.get(i % icons.size))
                } else {
                    if (paper) {
                        drawCircle(ink.copy(alpha = 0.3f), minOf(cellW, cellH) / 2f, Offset(cx + cellW / 2f, cy + cellH / 2f), style = Stroke(0.7.dp.toPx()))
                    } else {
                        drawRoundRect(ink.copy(alpha = 0.09f), Offset(cx, cy), Size(cellW, cellH), CornerRadius(minOf(cellW, cellH) * 0.32f))
                    }
                }
            }
            val dockTop = size.height - pad - dockH
            val dockW = (size.width - 2 * pad - gap * (dock - 1)) / dock
            for (j in 0 until dock) {
                cell(j, pad + j * (dockW + gap), dockTop, dockW, dockH, cellTones[(j + 1) % cellTones.size], icons.takeIf { it.isNotEmpty() }?.get((j + 3) % icons.size))
            }
        }
    }
}

/** Aperçu d'un accueil papier : le dessin du style, encadré comme une figure. */
@Composable
fun HomeStyleFigure(style: HomeStyle, modifier: Modifier = Modifier) {
    val paper = LocalPaperUi.current
    val dark = accentDark()
    val frame = if (paper) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(26.dp)
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(160.dp, 244.dp)
                .clip(frame)
                .then(
                    if (paper) Modifier.border(0.8.dp, dark, frame)
                    else Modifier.background(LocalSettingsAccent.current.copy(alpha = 0.1f)).border(1.dp, dark.copy(alpha = 0.4f), frame),
                )
                .padding(18.dp),
        ) {
            drawHomeStyle(style, dark, 1.1.dp.toPx())
        }
    }
}
