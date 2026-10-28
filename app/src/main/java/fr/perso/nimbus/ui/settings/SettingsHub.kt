package fr.perso.nimbus.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.SystemStatus
import fr.perso.nimbus.ui.drawer.SearchMatcher
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextField
import fr.perso.nimbus.ui.theme.inkClickable

/**
 * Page principale : le champ de recherche des réglages, puis les dix pages en trois groupes (Aspect,
 * Interactions, Système), chacune avec la valeur qui la résume. Une saisie de deux lettres remplace la
 * liste par les réglages trouvés.
 */
@Composable
internal fun SettingsCtx.HubPage() {
    val c = this
    var query by rememberSaveable { mutableStateOf("") }
    val paper = LocalPaperUi.current
    val hits = remember(query) { SettingsIndex.search(query, limit = 40) }
    val searching = query.trim().length >= 2
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        } ?: "?"
    }

    Page(SettingsSection.MAIN) {
        item(key = "search") { SettingsSearchField(query) { query = it } }
        if (searching) {
            if (hits.isEmpty()) {
                item(key = "search:none") { SettingsNote("Aucun réglage ne correspond.") }
            }
            items(hits, key = { "hit:${it.id?.name ?: it.section.name}" }) { hit ->
                SettingsHitRow(hit, query) { open(hit.section, hit.id) }
            }
            return@Page
        }
        if (!paper) mainBanners(settings.badgesEnabled, settings.unlockPage, settings, status, context, host::requestDefaultLauncher)

        settingsSection("Aspect", SettingsColors.Orange)
        hubEntry(c, SettingsSection.APPEARANCE, R.drawable.ms_palette, SettingsColors.Orange, appearanceSummary())
        hubEntry(c, SettingsSection.HOME, R.drawable.ms_home, SettingsColors.Blue, homeSummary())
        hubEntry(c, SettingsSection.DRAWER, R.drawable.ms_apps, SettingsColors.Green, "${settings.drawerColumns} colonnes · ${settings.webSearchEngine.label}")

        settingsSection("Interactions", SettingsColors.Purple)
        hubEntry(c, SettingsSection.GESTURES, R.drawable.ms_swipe, SettingsColors.Red, gestureSummary())
        hubEntry(c, SettingsSection.DICTATION, R.drawable.ms_mic, SettingsColors.Purple, dictationSummary())
        hubEntry(c, SettingsSection.NOTIFICATIONS, R.drawable.ms_notifications, SettingsColors.Cyan, notificationsSummary())

        settingsSection("Système", SettingsColors.Teal)
        hubEntry(c, SettingsSection.LOCKSCREEN, R.drawable.ms_lock, SettingsColors.Teal, if (settings.unlockPage) "Page de déverrouillage activée" else "Page de déverrouillage désactivée")
        hubEntry(c, SettingsSection.PRIVACY, R.drawable.ms_shield_lock, SettingsColors.Blue, privacySummary())
        hubEntry(c, SettingsSection.BACKUP, R.drawable.ms_save, SettingsColors.Orange, if (backupFolder != null) "Sauvegarde automatique chaque jour" else "Sauvegarde automatique désactivée")
        hubEntry(c, SettingsSection.ABOUT, R.drawable.ms_info, SettingsColors.Gray, "Nimbus $version")

        if (paper) {
            item(key = "banners:gap") { Spacer(Modifier.height(20.dp)) }
            mainBanners(settings.badgesEnabled, settings.unlockPage, settings, status, context, host::requestDefaultLauncher)
        }
    }
}

/** Ligne d'une page : son titre, la valeur qui la résume et un chevron. */
private fun LazyListScope.hubEntry(c: SettingsCtx, section: SettingsSection, icon: Int, tint: Color, summary: String) {
    item(key = "page:${section.name}") {
        SettingsEntry(icon, tint, SettingsIndex.pageTitle(section), summary, chevron = true) { c.open(section) }
    }
}

private fun SettingsCtx.appearanceSummary(): String =
    "Thème ${settings.themeMode.label.substringBefore(" (")} · icônes ${settings.iconShape.label}"

private fun SettingsCtx.homeSummary(): String =
    if (settings.homeStyle.paper) settings.homeStyle.label else "${settings.homeStyle.label} · ${settings.homeColumns} × ${settings.homeRows}"

private fun SettingsCtx.gestureSummary(): String {
    val action = settings.actionFor(Gesture.DOUBLE_TAP)
    return "Double tap : " + action.label.replaceFirstChar { it.lowercase() }
}

private fun SettingsCtx.dictationSummary(): String {
    val n = settings.claudeSessions.size
    val service = if (status.dictationService) "Service activé" else "Service à activer"
    return service + if (n > 0) " · $n conversation${if (n > 1) "s" else ""}" else ""
}

private fun SettingsCtx.notificationsSummary(): String =
    if (settings.badgesEnabled) "Pastilles : " + settings.badgeStyle.label.replaceFirstChar { it.lowercase() } else "Pastilles désactivées"

private fun SettingsCtx.privacySummary(): String {
    val n = apps.hidden.size
    val hidden = if (n == 0) "Aucune app masquée" else "$n app${if (n > 1) "s" else ""} masquée${if (n > 1) "s" else ""}"
    return hidden + if (apps.privateSpace != null) " · espace privé" else ""
}

/**
 * Bandeaux de la page principale (notes de bas de page numérotées sous le thème papier) : l'accueil par défaut
 * et les autorisations dont un réglage actif a besoin.
 */
private fun LazyListScope.mainBanners(
    badgesEnabled: Boolean,
    unlockPage: Boolean,
    settings: fr.perso.nimbus.data.LauncherSettings,
    status: SystemStatus,
    context: android.content.Context,
    requestDefaultLauncher: () -> Unit,
) {
    var mark = 0
    if (!status.isDefaultLauncher) {
        val n = ++mark
        item(key = "banner:home") { SettingsBanner(R.drawable.ms_home, "Définir Nimbus comme application d'accueil", mark = n, rule = n == 1, onClick = requestDefaultLauncher) }
    }
    if (badgesEnabled && !status.notificationAccess) {
        val n = ++mark
        item(key = "banner:notifications") {
            SettingsBanner(R.drawable.ms_app_badging, "Voir le nombre de notifications sur les icônes", mark = n, rule = n == 1) {
                SystemActions.openNotificationAccessSettings(context)
            }
        }
    }
    val needsService = unlockPage || Gesture.entries.any { settings.actionFor(it) in SERVICE_ACTIONS }
    if (needsService && !status.gestureService) {
        val n = ++mark
        item(key = "banner:gestures") {
            SettingsBanner(R.drawable.ms_accessibility_new, "Activer les gestes système (verrouillage…)", mark = n, rule = n == 1) {
                SystemActions.openAccessibilitySettings(context)
            }
        }
    }
    if (unlockPage && !status.lockScreenAllowed) {
        val n = ++mark
        item(key = "banner:xiaomi") {
            SettingsBanner(R.drawable.ms_lock, "Laisser la page de déverrouillage passer devant l'écran verrouillé (Xiaomi)", mark = n, rule = n == 1) {
                SystemActions.openXiaomiPermissions(context)
            }
        }
    }
}

/** Champ de recherche des réglages : pastille arrondie (Material) ou ligne « Rechercher : ___ » (papier). */
@Composable
private fun SettingsSearchField(query: String, onChange: (String) -> Unit) {
    val clear: @Composable () -> Unit = {
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button, onClickLabel = "Effacer") { onChange("") },
            ) {
                Icon(painterResource(R.drawable.ms_close), contentDescription = "Effacer", modifier = Modifier.size(20.dp))
            }
        }
    }
    if (LocalPaperUi.current) {
        PaperTextField(
            value = query,
            onValueChange = onChange,
            label = "Rechercher",
            placeholder = "un réglage",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailing = { clear() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PaperMargin, end = PaperMargin, top = 12.dp, bottom = 6.dp),
        )
        return
    }
    val accent = LocalSettingsAccent.current
    val field = lerp(MaterialTheme.colorScheme.surfaceVariant, accent, 0.12f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(field)
            .padding(start = 18.dp, end = 6.dp),
    ) {
        Icon(painterResource(R.drawable.ms_search), contentDescription = null, tint = accentText(), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Rechercher un réglage", fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(accentText()),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        clear()
    }
}

/** Un réglage trouvé : son intitulé avec les lettres cherchées surlignées, et son chemin dessous. */
@Composable
private fun SettingsHitRow(hit: SettingsHit, query: String, onClick: () -> Unit) {
    val title = remember(hit.title, query) { SearchMatcher.highlight(hit.title, query) }
    val tint = hit.section.accent()
    val paper = LocalPaperUi.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (paper) Modifier.inkClickable(role = Role.Button, onClick = onClick) else Modifier.clickable(role = Role.Button, onClick = onClick))
            .heightIn(min = if (paper) 52.dp else 64.dp)
            .padding(horizontal = if (paper) PaperMargin else 24.dp, vertical = 8.dp),
    ) {
        SettingsIconTile(sectionIcon(hit.section), tint)
        Spacer(Modifier.width(if (paper) 14.dp else 20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                title,
                style = if (paper) PaperStyle.item else MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                hit.path,
                style = if (paper) PaperStyle.note else MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Icône de la page, pour les résultats de recherche. */
internal fun sectionIcon(section: SettingsSection): Int = when (section) {
    SettingsSection.MAIN, SettingsSection.ABOUT -> R.drawable.ms_info
    SettingsSection.APPEARANCE -> R.drawable.ms_palette
    SettingsSection.HOME, SettingsSection.HOME_APPS -> R.drawable.ms_home
    SettingsSection.DRAWER -> R.drawable.ms_apps
    SettingsSection.CATEGORIES -> R.drawable.ms_category
    SettingsSection.GESTURES -> R.drawable.ms_swipe
    SettingsSection.LOCKSCREEN -> R.drawable.ms_lock
    SettingsSection.PRIVACY -> R.drawable.ms_shield_lock
    SettingsSection.DICTATION -> R.drawable.ms_mic
    SettingsSection.NOTIFICATIONS -> R.drawable.ms_notifications
    SettingsSection.BACKUP -> R.drawable.ms_save
}
