package fr.perso.nimbus.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperCheckbox
import fr.perso.nimbus.ui.theme.PaperChevron
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperFootnote
import fr.perso.nimbus.ui.theme.PaperListRow
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperParagraph
import fr.perso.nimbus.ui.theme.PaperRule
import fr.perso.nimbus.ui.theme.PaperRunningHead
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.inkClickable

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

/** Couleur d'accent d'une page : titres de section, interrupteurs, choix, bandeaux et fond du haut de page. */
fun SettingsSection.accent(): Color = when (this) {
    SettingsSection.MAIN -> SettingsColors.Blue
    SettingsSection.APPEARANCE -> SettingsColors.Orange
    SettingsSection.HOME, SettingsSection.HOME_APPS -> SettingsColors.Blue
    SettingsSection.DRAWER, SettingsSection.CATEGORIES -> SettingsColors.Green
    SettingsSection.GESTURES -> SettingsColors.Red
    SettingsSection.LOCKSCREEN -> SettingsColors.Teal
    SettingsSection.PRIVACY -> SettingsColors.Blue
    SettingsSection.DICTATION -> SettingsColors.Purple
    SettingsSection.NOTIFICATIONS -> SettingsColors.Cyan
    SettingsSection.BACKUP -> SettingsColors.Orange
    SettingsSection.ABOUT -> SettingsColors.Blue
}

/** Accent de la page affichée (voir [SettingsSection.accent]). */
val LocalSettingsAccent = staticCompositionLocalOf { SettingsColors.Blue }

/** Accent assez sombre pour porter du texte blanc ou un aplat (interrupteur, choix sélectionné). */
@Composable
internal fun accentDark(accent: Color = LocalSettingsAccent.current): Color = lerp(accent, Color.Black, 0.22f)

/** Accent lisible en texte sur le fond de la page (plus clair en thème sombre, plus foncé en thème clair). */
@Composable
internal fun accentText(accent: Color = LocalSettingsAccent.current): Color =
    lerp(accent, if (LocalPaperUi.current) PaperColors.Ink else MaterialTheme.colorScheme.onSurface, 0.4f)

/**
 * Ligne à montrer (résultat de recherche) : [key] est le nom d'un [SettingId], [alpha] l'intensité de la
 * surbrillance (lue au dessin, sans recomposer).
 */
internal class SettingsFocus(val key: String?, val alpha: State<Float>)

internal val LocalSettingsFocus = compositionLocalOf<SettingsFocus?> { null }

/** Surligne la ligne [id] quand une recherche vient d'y mener. */
@Composable
internal fun Modifier.settingFocus(id: SettingId?): Modifier {
    val focus = LocalSettingsFocus.current
    val color = LocalSettingsAccent.current
    if (id == null || focus == null || focus.key != id.name) return this
    return drawBehind { drawRect(color.copy(alpha = 0.26f * focus.alpha.value)) }
}

/**
 * Page de réglages : grand titre (avec le logo sur la page principale, une flèche retour
 * sur les sous-pages), puis le contenu. Les marges suivent les barres système. Le haut de page prend
 * la couleur de la page ([accent]).
 * Thème papier : page d'article, titre courant « Nimbus » (ou « ← [parentTitle] ») et [folio],
 * titre en gras souligné de l'accent, sections numérotées.
 * [focus] : ligne à faire défiler dans la vue puis à surligner ([onFocusShown] quand c'est fait).
 */
@Composable
fun SettingsPage(
    title: String,
    onBack: (() -> Unit)?,
    folio: String = "",
    accent: Color = LocalSettingsAccent.current,
    parentTitle: String = "Paramètres",
    focus: SettingId? = null,
    onFocusShown: () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val bars = WindowInsets.systemBars.asPaddingValues()
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val keys = remember { mutableMapOf<String, Int>() }
    val pulse = remember { Animatable(0f) }
    var focusKey by remember { mutableStateOf<String?>(null) }

    // Arrivée d'une recherche : la ligne défile vers le haut de la page, puis clignote deux fois.
    LaunchedEffect(focus) {
        val wanted = focus ?: return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        val index = keys[wanted.name]
        if (index != null) {
            listState.animateScrollToItem(index, scrollOffset = -with(density) { 140.dp.roundToPx() })
            focusKey = wanted.name
            repeat(2) {
                pulse.animateTo(1f, tween(260))
                pulse.animateTo(0.2f, tween(380))
            }
            pulse.animateTo(0f, tween(500))
            focusKey = null
        }
        onFocusShown()
    }
    val focusState = remember(focusKey) { SettingsFocus(focusKey, pulse.asState()) }

    CompositionLocalProvider(LocalSettingsAccent provides accent, LocalSettingsFocus provides focusState) {
        if (LocalPaperUi.current) {
            Surface(color = PaperColors.Paper, contentColor = PaperColors.Ink, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = bars.calculateStartPadding(direction),
                        end = bars.calculateEndPadding(direction),
                        top = bars.calculateTopPadding() + 4.dp,
                        bottom = bars.calculateBottomPadding() + 40.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    keys.clear()
                    item(key = "title") {
                        Column(Modifier.padding(horizontal = PaperMargin)) {
                            PaperRunningHead(
                                if (onBack == null) "Nimbus" else parentTitle,
                                folio,
                                onBack = onBack,
                                folioColor = accentDark(accent),
                            )
                            Text(title, style = PaperStyle.title, modifier = Modifier.padding(top = 26.dp))
                            Box(
                                Modifier
                                    .padding(top = 9.dp, bottom = 10.dp)
                                    .size(46.dp, 3.dp)
                                    .background(accent),
                            )
                        }
                    }
                    SettingsListScope(this, keys, 1).content()
                }
            }
            return@CompositionLocalProvider
        }
        val background = MaterialTheme.colorScheme.background
        val tint = remember(background, accent) { lerp(background, accent, 0.17f) }
        Surface(color = background, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(tint, background), endY = with(density) { 340.dp.toPx() })),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = bars.calculateStartPadding(direction),
                        end = bars.calculateEndPadding(direction),
                        top = bars.calculateTopPadding() + 8.dp,
                        bottom = bars.calculateBottomPadding() + 32.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    keys.clear()
                    item(key = "title") {
                        if (onBack == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_launcher_monochrome),
                                    contentDescription = null,
                                    tint = accentText(accent),
                                    modifier = Modifier.size(52.dp),
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(title, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                Surface(
                                    onClick = onBack,
                                    shape = CircleShape,
                                    color = accent.copy(alpha = 0.18f),
                                    contentColor = accentText(accent),
                                    modifier = Modifier
                                        .padding(start = 12.dp, top = 8.dp)
                                        .size(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Retour")
                                    }
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
                    SettingsListScope(this, keys, 1).content()
                }
            }
        }
    }
}

/**
 * Bandeau d'information cliquable (fond teinté de l'accent, texte de l'accent, chevron).
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
            markColor = accentDark(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PaperMargin, end = PaperMargin, top = if (rule) 14.dp else 0.dp),
        )
        return
    }
    val textColor = accentText()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = LocalSettingsAccent.current.copy(alpha = 0.16f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = textColor, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(20.dp))
            Text(text, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ms_chevron_right), contentDescription = null, tint = textColor)
        }
    }
}

/**
 * Portée de la liste d'une page de réglages : numérote les sections (thème papier) et note la position de
 * chaque ligne à clé ([keys]), pour faire défiler jusqu'à un réglage trouvé par la recherche.
 */
class SettingsListScope internal constructor(
    private val base: LazyListScope,
    private val keys: MutableMap<String, Int>,
    private var index: Int,
) : LazyListScope by base {
    internal var sections = 0

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        if (key != null) keys[key.toString()] = index
        index++
        base.item(key, contentType, content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        if (key != null) for (i in 0 until count) keys[key(i).toString()] = index + i
        index += count
        base.items(count, key, contentType, itemContent)
    }

    /** Bandeau qui reste en haut de la page pendant le défilement (aperçu en direct). */
    @OptIn(ExperimentalFoundationApi::class)
    internal fun pinned(key: String, content: @Composable () -> Unit) {
        keys[key] = index
        index++
        base.stickyHeader(key = key) { content() }
    }
}

/** Fond d'un bandeau collant : opaque, à la teinte du haut de la page. */
@Composable
internal fun pinnedBackground(): Color {
    if (LocalPaperUi.current) return PaperColors.Paper
    val background = MaterialTheme.colorScheme.background
    return lerp(background, LocalSettingsAccent.current, 0.08f)
}

/**
 * Titre de section en petites capitales espacées (« ICÔNES ») de la couleur de la page, [tone] la
 * remplaçant au besoin. Thème papier : filet de la couleur, puis titre numéroté en gras (« 1  Icônes »).
 */
fun LazyListScope.settingsSection(title: String, tone: Color? = null) {
    val number = (this as? SettingsListScope)?.let { ++it.sections }
    item(key = "section:$title") { SectionTitle(title, number, tone ?: LocalSettingsAccent.current) }
}

/**
 * Section repliable : le titre porte le nombre de lignes ([count]) et une flèche ; toucher le titre
 * déplie ou replie les lignes que la page ajoute ensuite (seulement si [expanded]).
 */
fun LazyListScope.collapsibleSection(title: String, count: String, expanded: Boolean, tone: Color? = null, onToggle: () -> Unit) {
    val number = (this as? SettingsListScope)?.let { ++it.sections }
    item(key = "section:$title") {
        SectionTitle(title, number, tone ?: LocalSettingsAccent.current, count, expanded, onToggle)
    }
}

/**
 * Aperçu qui reste en haut de la page pendant qu'on la fait défiler (fond opaque, à la teinte du haut de la
 * page) : on règle et on voit le résultat.
 */
fun LazyListScope.pinnedPreview(key: String = "preview", content: @Composable () -> Unit) {
    val scope = this as? SettingsListScope
    if (scope != null) {
        scope.pinned(key) { Box(Modifier.fillMaxWidth().background(pinnedBackground())) { content() } }
    } else {
        item(key = key) { content() }
    }
}

/** Ligne à clé du nom de [id] : c'est elle que la recherche des réglages fait défiler dans la vue. */
fun LazyListScope.setting(id: SettingId, content: @Composable () -> Unit) {
    item(key = id.name) { content() }
}

@Composable
private fun SectionTitle(
    title: String,
    number: Int?,
    accent: Color,
    count: String? = null,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val paper = LocalPaperUi.current
    val tap = if (onClick == null) Modifier else if (paper) Modifier.inkClickable(role = Role.Button, onClick = onClick) else Modifier.clickable(role = Role.Button, onClick = onClick)
    val arrow: @Composable () -> Unit = {
        if (expanded != null) {
            Icon(
                painterResource(R.drawable.ms_expand_more),
                contentDescription = if (expanded) "Replier" else "Déplier",
                tint = accentText(accent),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(20.dp)
                    .rotate(if (expanded) 0f else -90f),
            )
        }
    }
    if (paper) {
        Column(Modifier.padding(start = PaperMargin, end = PaperMargin, top = 14.dp, bottom = 2.dp)) {
            PaperRule(color = accentDark(accent), thickness = 0.8.dp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().then(tap).padding(top = 16.dp, bottom = 2.dp)) {
                if (number != null) Text(number.toString(), style = PaperStyle.section.copy(color = accentDark(accent)), modifier = Modifier.width(30.dp))
                Text(title, style = PaperStyle.section, modifier = Modifier.weight(1f))
                if (count != null) Text(count, style = PaperStyle.note)
                arrow()
            }
        }
        return
    }
    Column {
        Box(
            Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(0.6.dp)
                .background(lerp(MaterialTheme.colorScheme.outlineVariant, accent, 0.35f)),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().then(tap).padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = accentText(accent),
                modifier = Modifier.weight(1f),
            )
            if (count != null) Text(count, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            arrow()
        }
    }
}

/** Pastille d'icône de la ligne : aplat de la teinte, glyphe blanc (Material) ; anneau fin de la teinte (papier). */
@Composable
internal fun SettingsIconTile(@DrawableRes icon: Int, tint: Color) {
    if (LocalPaperUi.current) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(tint.copy(alpha = 0.09f), CircleShape)
                .border(1.3.dp, tint, CircleShape),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        return
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(lerp(tint, Color.Black, 0.12f), RoundedCornerShape(14.dp)),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/**
 * Cadre d'une ligne de réglage : pastille d'icône, titre, sous-titre éventuel, puis [trailing] à droite.
 * [onClick] null : la ligne ne se touche pas (c'est son contrôle qui réagit). [id] : surbrillance de la recherche.
 */
@Composable
internal fun SettingsRow(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    id: SettingId?,
    paperLeading: (@Composable () -> Unit)? = null,
    toggle: Boolean? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (LocalPaperUi.current) {
        PaperListRow(
            title = title,
            subtitle = subtitle,
            enabled = enabled,
            onClick = onClick,
            leading = paperLeading ?: { SettingsIconTile(icon, iconTint) },
            trailing = trailing,
            modifier = Modifier
                .settingFocus(id)
                .then(if (toggle != null) Modifier.semantics { toggleableState = ToggleableState(toggle) } else Modifier),
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .settingFocus(id)
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        SettingsIconTile(icon, iconTint)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/** Interrupteur (Material) ou case ☐ / ☒ (papier), à la couleur de la page. */
@Composable
internal fun SettingsSwitch(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val dark = accentDark()
    if (LocalPaperUi.current) {
        PaperCheckbox(checked, color = dark)
        return
    }
    Switch(
        checked = checked,
        onCheckedChange = { onToggle() },
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedTrackColor = dark,
            checkedBorderColor = dark,
            checkedThumbColor = Color.White,
        ),
    )
}

/** Valeur courante (de la couleur de la page) et chevron à droite d'une ligne. */
@Composable
private fun ValueAndChevron(value: String?, chevron: Boolean) {
    val paper = LocalPaperUi.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (value != null) {
            Text(
                value,
                style = if (paper) PaperStyle.note.copy(color = accentText()) else MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentText(),
                ),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp),
            )
            if (chevron) Spacer(Modifier.width(8.dp))
        }
        if (chevron) {
            if (paper) {
                PaperChevron()
            } else {
                Icon(painterResource(R.drawable.ms_chevron_right), contentDescription = null, tint = accentText())
            }
        }
    }
}

/**
 * Ligne de réglage : pastille d'icône colorée, titre, sous-titre éventuel, et à droite une [value], un chevron ou un
 * interrupteur. Thème papier : anneau fin de la teinte, titre en romain, sous-titre en italique gris, chevron « › » ou case ☐ / ☒.
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
    value: String? = null,
    id: SettingId? = null,
    onClick: () -> Unit,
) {
    val trailing: (@Composable () -> Unit)? = when {
        checked != null -> {
            { SettingsSwitch(checked, enabled, onClick) }
        }
        value != null || chevron -> {
            { ValueAndChevron(value, chevron) }
        }
        else -> null
    }
    SettingsRow(icon, iconTint, title, subtitle, enabled, onClick, id, paperLeading, checked, trailing)
}

/** Réglage à choisir sur place : titre (et valeur) au-dessus, puis le contrôle [content] sur toute la largeur. */
@Composable
fun SettingsControl(
    title: String,
    value: String? = null,
    id: SettingId? = null,
    content: @Composable () -> Unit,
) {
    val paper = LocalPaperUi.current
    Column(
        Modifier
            .fillMaxWidth()
            .settingFocus(id)
            .padding(horizontal = if (paper) PaperMargin else 24.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                style = if (paper) PaperStyle.item else MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    value,
                    style = if (paper) PaperStyle.note.copy(color = accentText()) else MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentText(),
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/**
 * Choix unique entre quelques options, sur une seule rangée. Material : boutons segmentés teintés de la couleur de la page ;
 * papier : cadre à filet fin, l'option choisie en aplat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = accentDark()
    if (LocalPaperUi.current) {
        Row(
            modifier
                .fillMaxWidth()
                .border(0.8.dp, dark),
        ) {
            options.forEachIndexed { i, option ->
                val on = option == selected
                if (i > 0) Box(Modifier.width(0.8.dp).height(44.dp).background(dark))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .background(if (on) dark else Color.Transparent)
                        .inkClickable(role = Role.RadioButton) { onSelect(option) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    Text(
                        label(option),
                        style = PaperStyle.item.copy(fontSize = 16.sp, color = if (on) PaperColors.Paper else PaperColors.Ink),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        return
    }
    val accent = LocalSettingsAccent.current
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = accent.copy(alpha = 0.28f),
                    activeContentColor = accentText(),
                    activeBorderColor = dark.copy(alpha = 0.6f),
                    inactiveBorderColor = lerp(MaterialTheme.colorScheme.outlineVariant, accent, 0.4f),
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(label(option), textAlign = TextAlign.Center, maxLines = 2, fontSize = 14.sp, lineHeight = 17.sp)
            }
        }
    }
}

/** Bouton rond « − » ou « + » du réglage d'un nombre. */
@Composable
private fun StepButton(symbol: String, enabled: Boolean, description: String, onClick: () -> Unit) {
    val paper = LocalPaperUi.current
    val dark = accentDark()
    val shape = if (paper) androidx.compose.ui.graphics.RectangleShape else CircleShape
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .then(
                if (paper) Modifier.border(0.8.dp, dark, shape)
                else Modifier.background(LocalSettingsAccent.current.copy(alpha = 0.2f), shape),
            )
            .clip(shape)
            .then(
                if (!enabled) Modifier
                else if (paper) Modifier.inkClickable(onClickLabel = description, onClick = onClick)
                else Modifier.clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
            ),
    ) {
        Text(symbol, fontSize = 24.sp, color = if (paper) dark else accentText(), lineHeight = 24.sp)
    }
}

/** Réglage d'un nombre entre deux bornes : « − 5 + » à droite d'une ligne. */
@Composable
fun SettingsStepper(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    value: Int,
    range: IntRange,
    id: SettingId? = null,
    onChange: (Int) -> Unit,
) {
    SettingsRow(icon, iconTint, title, null, true, null, id) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", value > range.first, "Diminuer") { onChange(value - 1) }
            Text(
                value.toString(),
                style = if (LocalPaperUi.current) PaperStyle.serif(21f, 24f, accentDark()) else MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentText(),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp),
            )
            StepButton("+", value < range.last, "Augmenter") { onChange(value + 1) }
        }
    }
}

/** Paragraphe d'explication sous une ligne ou une section. Thème papier : paragraphe d'article justifié. */
@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier) {
    if (LocalPaperUi.current) {
        PaperParagraph(text, modifier)
        return
    }
    Text(
        text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 6.dp),
    )
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
