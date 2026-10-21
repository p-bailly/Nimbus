package fr.perso.nimbus.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperCheckbox
import fr.perso.nimbus.ui.theme.PaperChevron
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperFootnote
import fr.perso.nimbus.ui.theme.PaperListRow
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperRule
import fr.perso.nimbus.ui.theme.PaperRunningHead
import fr.perso.nimbus.ui.theme.PaperSectionTitle
import fr.perso.nimbus.ui.theme.PaperStyle

/** Couleurs des icônes de réglages, comme les pastilles colorées de Smart Launcher. */
object SettingsColors {
    val Green = Color(0xFF2E9E4F)
    val Orange = Color(0xFFF2792B)
    val Red = Color(0xFFE5484D)
    val Cyan = Color(0xFF0FA5C4)
    val Purple = Color(0xFF7C4DFF)
    val Blue = Color(0xFF2F6BEA)
    val Teal = Color(0xFF0F9D8A)
    val Gray = Color(0xFF7A7F87)
}

/**
 * Page de réglages : grand titre (avec le logo sur la page principale, une flèche retour
 * sur les sous-pages), puis le contenu. Les marges suivent les barres système.
 * Thème papier : page d'article, titre courant « Nimbus » (ou « ← paramètres ») et [folio],
 * titre en gras, sections numérotées.
 */
@Composable
fun SettingsPage(
    title: String,
    onBack: (() -> Unit)?,
    folio: String = "",
    content: LazyListScope.() -> Unit,
) {
    val bars = WindowInsets.systemBars.asPaddingValues()
    val direction = LocalLayoutDirection.current
    if (LocalPaperUi.current) {
        Surface(color = PaperColors.Paper, contentColor = PaperColors.Ink, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = bars.calculateStartPadding(direction),
                    end = bars.calculateEndPadding(direction),
                    top = bars.calculateTopPadding() + 4.dp,
                    bottom = bars.calculateBottomPadding() + 40.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "title") {
                    Column(Modifier.padding(horizontal = PaperMargin)) {
                        PaperRunningHead(if (onBack == null) "Nimbus" else "Paramètres", folio, onBack = onBack)
                        Text(title, style = PaperStyle.title, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
                    }
                }
                SettingsListScope(this).content()
            }
        }
        return
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = bars.calculateStartPadding(direction),
                end = bars.calculateEndPadding(direction),
                top = bars.calculateTopPadding() + 8.dp,
                bottom = bars.calculateBottomPadding() + 32.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "title") {
                if (onBack == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_launcher_monochrome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(title, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                            Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Retour")
                        }
                        Text(
                            title,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                        )
                    }
                }
            }
            content()
        }
    }
}

/**
 * Bandeau d'information cliquable (fond teinté, texte de la couleur principale, chevron).
 * Thème papier : note de bas de page numérotée [mark], le texte en lien bleu ; [rule] : petit
 * filet au-dessus (la première note d'une série).
 */
@Composable
fun SettingsBanner(@DrawableRes icon: Int, text: String, mark: Int = 1, rule: Boolean = true, onClick: () -> Unit) {
    if (LocalPaperUi.current) {
        PaperFootnote(
            mark = "$mark",
            text = text,
            rule = rule,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PaperMargin, end = PaperMargin, top = if (rule) 14.dp else 0.dp),
        )
        return
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(20.dp))
            Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ms_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Portée de la liste d'une page de réglages : numérote les sections (thème papier). */
private class SettingsListScope(base: LazyListScope) : LazyListScope by base {
    var sections = 0
}

/**
 * Titre de section en petites capitales espacées (« PAGES », « AUTRES »).
 * Thème papier : filet fin, puis titre numéroté en gras (« 1  Pages »).
 */
fun LazyListScope.settingsSection(title: String) {
    val number = (this as? SettingsListScope)?.let { ++it.sections }
    item(key = "section:$title") {
        if (LocalPaperUi.current) {
            Column(Modifier.padding(start = PaperMargin, end = PaperMargin, top = 14.dp, bottom = 2.dp)) {
                PaperRule()
                PaperSectionTitle(number?.toString(), title, Modifier.padding(top = 16.dp, bottom = 2.dp))
            }
            return@item
        }
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Text(
                title.uppercase(),
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
            )
        }
    }
}

/**
 * Ligne de réglage : icône colorée, titre, sous-titre éventuel, et à droite un chevron ou un interrupteur.
 * Thème papier : pas d'icône (sauf [paperLeading]), titre en romain, sous-titre en italique gris,
 * chevron « › » ou case à cocher ☐ / ☒.
 */
@Composable
fun SettingsEntry(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    chevron: Boolean = false,
    checked: Boolean? = null,
    paperLeading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    if (LocalPaperUi.current) {
        PaperListRow(
            title = title,
            subtitle = subtitle,
            enabled = enabled,
            onClick = onClick,
            leading = paperLeading,
            trailing = when {
                checked != null -> {
                    { PaperCheckbox(checked) }
                }
                chevron -> {
                    { PaperChevron() }
                }
                else -> null
            },
            modifier = if (checked != null) Modifier.semantics { toggleableState = ToggleableState(checked) } else Modifier,
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            checked != null -> {
                Spacer(Modifier.width(12.dp))
                Switch(checked = checked, onCheckedChange = { onClick() }, enabled = enabled)
            }
            chevron -> Icon(
                painterResource(R.drawable.ms_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Carte arrondie qui regroupe un aperçu (page Apparence). Thème papier : simple bloc dans les marges. */
@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    if (LocalPaperUi.current) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PaperMargin, vertical = 8.dp),
        ) { content() }
        return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        content()
    }
}
