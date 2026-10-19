package fr.perso.nimbus.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import fr.perso.nimbus.data.ThemeMode

/** Teinte de repli (bleu nuit de l'icône) quand la couleur du fond d'écran est inconnue. */
private const val DEFAULT_SEED = 0xFF5B7FD6.toInt()

/** Les thèmes qui suivent le système passent en sombre avec lui ; les autres sont fixes. */
fun isDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.AMBIENT, ThemeMode.MATERIAL_YOU -> systemDark
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun NimbusTheme(
    mode: ThemeMode,
    wallpaperColor: Int?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = isDarkTheme(mode, isSystemInDarkTheme())
    val seed = wallpaperColor ?: DEFAULT_SEED
    val scheme = when {
        mode == ThemeMode.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            // La palette système change avec le fond d'écran : on la relit quand il change.
            remember(seed, dark) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
        // Material You indisponible avant Android 12 : on retombe sur la palette ambiante.
        mode == ThemeMode.AMBIENT || mode == ThemeMode.MATERIAL_YOU ->
            remember(seed, dark) { seededColorScheme(seed, dark) }
        else -> remember(dark) { seededColorScheme(DEFAULT_SEED, dark) }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/**
 * Palette complète dérivée d'une seule couleur : on garde sa teinte et on
 * fait varier saturation et luminosité pour chaque rôle Material 3.
 * Un fond d'écran presque gris donne une palette neutre.
 */
fun seededColorScheme(seed: Int, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    val hue = hsl[0]
    val sat = if (hsl[1] < 0.08f) 0.06f else hsl[1].coerceIn(0.35f, 0.75f)
    val tertiaryHue = (hue + 60f) % 360f

    fun tone(lightness: Float, saturation: Float = sat, h: Float = hue): Color =
        Color(ColorUtils.HSLToColor(floatArrayOf(h, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))))

    val neutral = sat * 0.25f
    val neutralVariant = sat * 0.35f

    return if (dark) {
        darkColorScheme(
            primary = tone(0.76f),
            onPrimary = tone(0.16f),
            primaryContainer = tone(0.30f, sat * 0.85f),
            onPrimaryContainer = tone(0.90f),
            inversePrimary = tone(0.40f),
            secondary = tone(0.74f, sat * 0.45f),
            onSecondary = tone(0.16f, sat * 0.45f),
            secondaryContainer = tone(0.27f, sat * 0.40f),
            onSecondaryContainer = tone(0.90f, sat * 0.40f),
            tertiary = tone(0.76f, sat * 0.70f, tertiaryHue),
            onTertiary = tone(0.16f, sat * 0.70f, tertiaryHue),
            tertiaryContainer = tone(0.30f, sat * 0.60f, tertiaryHue),
            onTertiaryContainer = tone(0.90f, sat * 0.60f, tertiaryHue),
            background = tone(0.07f, neutral),
            onBackground = tone(0.92f, neutral),
            surface = tone(0.08f, neutral),
            onSurface = tone(0.92f, neutral),
            surfaceVariant = tone(0.24f, neutralVariant),
            onSurfaceVariant = tone(0.80f, neutralVariant),
            surfaceTint = tone(0.76f),
            inverseSurface = tone(0.90f, neutral),
            inverseOnSurface = tone(0.16f, neutral),
            outline = tone(0.56f, neutralVariant),
            outlineVariant = tone(0.30f, neutralVariant),
            surfaceBright = tone(0.24f, neutral),
            surfaceDim = tone(0.07f, neutral),
            surfaceContainerLowest = tone(0.05f, neutral),
            surfaceContainerLow = tone(0.10f, neutral),
            surfaceContainer = tone(0.12f, neutral),
            surfaceContainerHigh = tone(0.16f, neutral),
            surfaceContainerHighest = tone(0.20f, neutral),
        )
    } else {
        lightColorScheme(
            primary = tone(0.40f),
            onPrimary = tone(0.99f),
            primaryContainer = tone(0.88f, sat * 0.85f),
            onPrimaryContainer = tone(0.12f),
            inversePrimary = tone(0.80f),
            secondary = tone(0.40f, sat * 0.45f),
            onSecondary = tone(0.99f, sat * 0.45f),
            secondaryContainer = tone(0.88f, sat * 0.40f),
            onSecondaryContainer = tone(0.12f, sat * 0.40f),
            tertiary = tone(0.40f, sat * 0.70f, tertiaryHue),
            onTertiary = tone(0.99f, sat * 0.70f, tertiaryHue),
            tertiaryContainer = tone(0.88f, sat * 0.60f, tertiaryHue),
            onTertiaryContainer = tone(0.12f, sat * 0.60f, tertiaryHue),
            background = tone(0.98f, neutral),
            onBackground = tone(0.10f, neutral),
            surface = tone(0.98f, neutral),
            onSurface = tone(0.10f, neutral),
            surfaceVariant = tone(0.90f, neutralVariant),
            onSurfaceVariant = tone(0.30f, neutralVariant),
            surfaceTint = tone(0.40f),
            inverseSurface = tone(0.20f, neutral),
            inverseOnSurface = tone(0.95f, neutral),
            outline = tone(0.50f, neutralVariant),
            outlineVariant = tone(0.80f, neutralVariant),
            surfaceBright = tone(0.98f, neutral),
            surfaceDim = tone(0.87f, neutral),
            surfaceContainerLowest = tone(1.00f, neutral),
            surfaceContainerLow = tone(0.96f, neutral),
            surfaceContainer = tone(0.94f, neutral),
            surfaceContainerHigh = tone(0.92f, neutral),
            surfaceContainerHighest = tone(0.90f, neutral),
        )
    }
}
