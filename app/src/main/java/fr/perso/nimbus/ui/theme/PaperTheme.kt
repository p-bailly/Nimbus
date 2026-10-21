package fr.perso.nimbus.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R

/**
 * Computer Modern (CMU Serif), complétée des symboles de Latin Modern Math (ℤ, ∈, ⟶, ∫…) :
 * voir design/polices. Romain, italique, gras, gras italique.
 */
val PaperSerif = FontFamily(
    Font(R.font.papier_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.papier_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.papier_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.papier_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

/** Couleurs du papier : fond crème, encre presque noire, liens bleus. */
object PaperColors {
    val Paper = Color(0xFFFBFAF6)
    val Ink = Color(0xFF14141A)
    /** Texte secondaire : encre à 70 %. */
    val Soft = Color(0xFF4A4C53)
    /** Filets fins. */
    val Rule = Color(0xFF14141A)
    val Link = Color(0xFF2F5BD3)
    val Error = Color(0xFFB3261E)
    /** Fonds à peine teintés (menus, champs), sans « encadré de couleur ». */
    val Shade = Color(0xFFF3F1EA)
    val ShadeStrong = Color(0xFFEAE6DC)
    val Line = Color(0xFFD9D4C7)
}

/**
 * Style de base, réglé pour composer comme un navigateur (donc comme les maquettes) : interlignage à la
 * manière de CSS (demi-interligne au-dessus et au-dessous) et chasse sans arrondi au pixel
 * ([TextMotion.Animated] : positions sous-pixel, sans hinting), sinon les lignes sortent 2 % trop serrées
 * et ne se coupent plus au même endroit.
 */
val PaperTextBase = TextStyle(
    fontFamily = PaperSerif,
    color = PaperColors.Ink,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    textMotion = TextMotion.Animated,
)

fun paperColorScheme(): ColorScheme = lightColorScheme(
    primary = PaperColors.Ink,
    onPrimary = PaperColors.Paper,
    primaryContainer = PaperColors.ShadeStrong,
    onPrimaryContainer = PaperColors.Ink,
    inversePrimary = Color(0xFFC9CCE0),
    secondary = PaperColors.Link,
    onSecondary = PaperColors.Paper,
    secondaryContainer = PaperColors.ShadeStrong,
    onSecondaryContainer = PaperColors.Ink,
    tertiary = PaperColors.Link,
    onTertiary = PaperColors.Paper,
    tertiaryContainer = PaperColors.Shade,
    onTertiaryContainer = PaperColors.Ink,
    background = PaperColors.Paper,
    onBackground = PaperColors.Ink,
    surface = PaperColors.Paper,
    onSurface = PaperColors.Ink,
    surfaceVariant = PaperColors.Shade,
    onSurfaceVariant = PaperColors.Soft,
    surfaceTint = Color.Transparent,
    inverseSurface = PaperColors.Ink,
    inverseOnSurface = PaperColors.Paper,
    error = PaperColors.Error,
    onError = PaperColors.Paper,
    outline = Color(0xFF6B6B70),
    outlineVariant = PaperColors.Line,
    scrim = Color.Black,
    surfaceBright = PaperColors.Paper,
    surfaceDim = PaperColors.ShadeStrong,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = PaperColors.Paper,
    surfaceContainer = PaperColors.Paper,
    surfaceContainerHigh = PaperColors.Paper,
    surfaceContainerHighest = PaperColors.Shade,
)

private fun serif(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) =
    PaperTextBase.copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = spacing.sp, color = Color.Unspecified)

/** Échelle typographique d'un article : titres gras, texte courant 15 sp, légendes 12,5 sp. */
val PaperTypography = Typography(
    displayLarge = serif(54, 60),
    displayMedium = serif(44, 50),
    displaySmall = serif(36, 42),
    headlineLarge = serif(30, 36, FontWeight.Bold),
    headlineMedium = serif(26, 32, FontWeight.Bold),
    headlineSmall = serif(22, 28, FontWeight.Bold),
    titleLarge = serif(21, 27, FontWeight.Bold),
    titleMedium = serif(17, 23, FontWeight.Bold),
    titleSmall = serif(15, 20, FontWeight.Bold),
    bodyLarge = serif(17, 24),
    bodyMedium = serif(15, 21),
    bodySmall = serif(13, 18),
    labelLarge = serif(15, 20),
    labelMedium = serif(13, 17),
    labelSmall = serif(12, 16),
)

/** Angles presque droits : des filets et des cadres plutôt que des cartes arrondies. */
val PaperShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(3.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)
