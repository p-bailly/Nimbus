package fr.perso.nimbus.ui.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.data.IconSize
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.components.AppGlyphDisc
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperStyle
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Apparence : l'aperçu de l'accueil reste en haut pendant qu'on règle, puis le thème en vignettes, la forme, la
 * taille et le pack des icônes, et le fond d'écran.
 */
@Composable
internal fun SettingsCtx.AppearancePage() {
    val packLabel = settings.iconPack?.let { pkg -> iconPacks?.firstOrNull { it.packageName == pkg }?.label ?: pkg }
        ?: "Aucun (icônes d'origine)"
    val themedSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    Page(SettingsSection.APPEARANCE) {
        pinnedPreview { AppearancePreview(apps, settings, wallpaperColor) }

        settingsSection("Thème")
        setting(SettingId.THEME) {
            Column(Modifier.settingFocus(SettingId.THEME)) { ThemePicker(settings.themeMode) { mode -> update { it.copy(themeMode = mode) } } }
        }
        item(key = "theme:note") { SettingsNote(settings.themeMode.label) }

        settingsSection("Icônes")
        setting(SettingId.ICON_SHAPE) {
            SettingsControl(SettingId.ICON_SHAPE.title, settings.iconShape.label, SettingId.ICON_SHAPE) {
                IconShapePicker(settings.iconShape) { shape -> update { it.copy(iconShape = shape) } }
            }
        }
        setting(SettingId.ICON_SIZE) {
            SettingsControl(SettingId.ICON_SIZE.title, id = SettingId.ICON_SIZE) {
                SettingsSegmented(IconSize.entries, settings.iconSize, { it.label }, { size -> update { it.copy(iconSize = size) } })
            }
        }
        setting(SettingId.ICON_PACK) {
            SettingsEntry(R.drawable.ms_style, SettingsColors.Orange, SettingId.ICON_PACK.title, packLabel, chevron = true, id = SettingId.ICON_PACK) {
                showDialog(SettingsDialog.IconPackChoice)
            }
        }
        setting(SettingId.THEMED_ICONS) {
            SettingsEntry(
                R.drawable.ms_format_paint, SettingsColors.Purple, SettingId.THEMED_ICONS.title,
                if (themedSupported) "Monochromes, aux couleurs du thème (comme sur Pixel)" else "Nécessite Android 13",
                enabled = themedSupported,
                checked = settings.themedIcons && themedSupported,
                id = SettingId.THEMED_ICONS,
            ) { update { it.copy(themedIcons = !it.themedIcons) } }
        }
        setting(SettingId.WRAP_LEGACY) {
            SettingsEntry(
                R.drawable.ms_dashboard_customize, SettingsColors.Teal, SettingId.WRAP_LEGACY.title,
                "Fond de la forme choisie pour les icônes non adaptatives",
                checked = settings.wrapLegacyIcons,
                id = SettingId.WRAP_LEGACY,
            ) { update { it.copy(wrapLegacyIcons = !it.wrapLegacyIcons) } }
        }

        settingsSection("Fond d'écran")
        setting(SettingId.WALLPAPER) {
            SettingsEntry(R.drawable.ms_wallpaper, SettingsColors.Green, SettingId.WALLPAPER.title, "Image ou fond animé", chevron = true, id = SettingId.WALLPAPER) {
                SystemActions.openWallpaperPicker(context)
            }
        }
    }
}

/**
 * Aperçu de l'accueil (comme « Apparence » de Smart Launcher) : couleur du fond d'écran, heure, vraies
 * icônes avec la forme, le pack et la taille choisis, barre de recherche. Les icônes se redessinent dès
 * qu'un réglage change.
 */
@Composable
internal fun AppearancePreview(apps: AppsState, settings: LauncherSettings, wallpaperColor: Int?) {
    if (LocalPaperUi.current) {
        PaperAppearancePreview(apps, settings)
        return
    }
    val seed = wallpaperColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val samples = remember(apps.favorites, apps.visible) { (apps.favorites + apps.visible).distinctBy { it.key }.take(4) }
    val time = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    SettingsCard {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(seed, lerp(seed, Color(0xFF1B2333), 0.75f))))
                .padding(24.dp),
        ) {
            Text(time, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                for (app in samples) {
                    Image(app.icon, contentDescription = app.label, modifier = Modifier.size(settings.iconSize.dp.coerceAtMost(56).dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp),
            ) {
                Icon(painterResource(R.drawable.ms_search), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Rechercher", color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

/**
 * Aperçu sous le thème papier : la page d'accueil en réduction, encadrée d'un filet comme une
 * figure (heure, date, pastilles des apps ; pas d'en-tête, comme les pages), puis sa légende.
 */
@Composable
internal fun PaperAppearancePreview(apps: AppsState, settings: LauncherSettings) {
    val samples = remember(apps.favorites, apps.visible) { (apps.favorites + apps.visible).distinctBy { it.key }.take(5) }
    val now = remember { LocalDateTime.now() }
    val accent = accentDark()
    SettingsCard {
        Column(Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.copy(alpha = 0.06f))
                    .border(0.8.dp, accent, RoundedCornerShape(3.dp))
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
            ) {
                Text(
                    now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE)),
                    style = PaperStyle.serif(30f, 34f),
                )
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)),
                    style = PaperStyle.smallCaps(10f, accent),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 16.dp)) {
                    for (app in samples) AppGlyphDisc(app, 34.dp, ring = 1.dp)
                }
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                    append(" – Aperçu de l'accueil.")
                },
                style = PaperStyle.caption.copy(textAlign = TextAlign.Center, color = PaperColors.Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
            )
        }
    }
}
