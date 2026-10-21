package fr.perso.nimbus.ui.unlock

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.random.Random

// Coloriage au crayon des bandeaux de la page de déverrouillage (Basket, notifications) : un lavis pâle,
// des hachures obliques un peu irrégulières, comme coloriées à la main, et un contour repassé deux fois.
// Le hasard est tiré d'une graine : le même bandeau garde le même coloriage d'un affichage à l'autre.

/**
 * Bandeau colorié de [color] (coins de [radius] unités) ; [seed] fixe ses irrégularités. Les hachures,
 * plus denses à gauche, s'éclaircissent vers la droite : le texte, à droite, reste lisible.
 */
internal fun Modifier.crayonBanner(color: Color, seed: Int, radius: Float = 12f): Modifier = drawBehind {
    val r = radius.dp.toPx()
    val u = 1.dp.toPx()
    val random = Random(seed)
    val shape = RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))
    drawRoundRect(color.copy(alpha = 0.09f), cornerRadius = CornerRadius(r))
    clipPath(Path().apply { addRoundRect(shape) }) {
        // Hachures à 60° environ, un trait tous les 6 unités, chacun un peu de travers.
        val slant = size.height * 0.55f
        var x = -slant
        while (x < size.width + slant) {
            val fade = 1f - (x / size.width).coerceIn(0f, 1f) * 0.55f
            val alpha = (0.15f + random.nextFloat() * 0.13f) * fade
            val wobble = (random.nextFloat() - 0.5f) * 3f * u
            drawLine(
                color.copy(alpha = alpha),
                Offset(x + wobble, size.height + 2 * u),
                Offset(x + slant + (random.nextFloat() - 0.5f) * 4f * u, -2 * u),
                strokeWidth = (1.1f + random.nextFloat() * 0.9f) * u,
                cap = StrokeCap.Round,
            )
            x += (5.2f + random.nextFloat() * 1.6f) * u
        }
        // Un second passage plus léger, en travers, sur le bord gauche : le coloriage y est plus appuyé.
        var y = -size.height
        while (y < size.height * 2) {
            drawLine(
                color.copy(alpha = 0.10f + random.nextFloat() * 0.06f),
                Offset(-2 * u, y),
                Offset(size.height * 0.9f, y - size.height * 0.5f + (random.nextFloat() - 0.5f) * 3 * u),
                strokeWidth = (1f + random.nextFloat() * 0.6f) * u,
                cap = StrokeCap.Round,
            )
            y += (6f + random.nextFloat() * 2f) * u
        }
    }
    // Contour repassé : deux traits un peu décalés.
    val outline = lerp(color, PaperColors.Ink, 0.15f)
    for (pass in 0..1) {
        val dx = (random.nextFloat() - 0.5f) * 1.2f * u
        val dy = (random.nextFloat() - 0.5f) * 1.2f * u
        drawRoundRect(
            outline.copy(alpha = if (pass == 0) 0.55f else 0.3f),
            topLeft = Offset(dx + 0.6f * u, dy + 0.6f * u),
            size = Size(size.width - 1.2f * u, size.height - 1.2f * u),
            cornerRadius = CornerRadius(r + pass * u),
            style = Stroke(width = (1.3f - pass * 0.3f) * u, cap = StrokeCap.Round),
        )
    }
}

/** Coup de surligneur au crayon sous la moitié basse d'un mot (« victoire », « défaite »). */
internal fun Modifier.crayonMark(color: Color, seed: Int): Modifier = drawBehind {
    val u = 1.dp.toPx()
    val random = Random(seed)
    for (pass in 0..1) {
        val top = size.height * (0.42f + random.nextFloat() * 0.08f)
        drawRoundRect(
            color.copy(alpha = if (pass == 0) 0.2f else 0.13f),
            topLeft = Offset(-3 * u + random.nextFloat() * u, top),
            size = Size(size.width + 5 * u, size.height * 0.5f),
            cornerRadius = CornerRadius(3 * u),
        )
    }
}

/** Couleur d'un texte sur un bandeau de [color] : assez sombre pour se lire (le jaune de Snapchat…). */
internal fun inkOf(color: Color): Color = lerp(color, PaperColors.Ink, 0.4f)
