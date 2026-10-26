package fr.perso.nimbus.ui.drawer

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.Category
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.BadgeStyle
import fr.perso.nimbus.data.CrayonLogos
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.ProfileState
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.AppGlyph
import fr.perso.nimbus.ui.components.AppGlyphDisc
import fr.perso.nimbus.ui.components.drawerAppGestures
import fr.perso.nimbus.ui.components.rememberAppColor
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.footnoteMark

// Tiroir en sommaire (thème papier) : la table des matières d'un article LaTeX. Sous le titre, une
// figure des apps les plus utilisées qui ne sont pas sur l'accueil ; puis les catégories en sections
// numérotées en gras, les apps en sous-sections avec points de conduite et numéro de page, et l'espace
// privé en annexe A. Les notifications d'une app sont un appel de note après son nom.

/** Une ligne du sommaire. */
internal sealed interface TocLine {
    val key: String

    /** Titre de section ; [number] null pour une section non numérotée. */
    data class Section(
        val number: String?,
        val title: String,
        val page: String,
        override val key: String,
        /** Annexe de l'espace privé : ses actions (verrouiller, réglages…) suivent le titre. */
        val privateSpace: Boolean = false,
    ) : TocLine

    /** Une app, en sous-section. */
    data class Entry(val number: String?, val app: AppEntry, val page: String, override val key: String) : TocLine
}

/** Apps par page : la pagination est inventée, mais vraisemblable. */
private const val APPS_PER_PAGE = 4

/**
 * Lignes du sommaire : une section par catégorie de [categories] (ou les apps numérotées à la suite
 * s'il n'y en a qu'une), puis [privateApps] en annexe A s'il est donné (vide : espace privé verrouillé).
 * Chaque section commence sur une nouvelle page.
 */
internal fun tableOfContents(
    apps: List<AppEntry>,
    categories: List<Category>,
    privateApps: List<AppEntry>? = null,
): List<TocLine> {
    val lines = ArrayList<TocLine>()
    var page = 1
    fun section(number: String, members: List<AppEntry>) {
        members.forEachIndexed { i, app ->
            lines += TocLine.Entry("$number.${i + 1}", app, "${page + i / APPS_PER_PAGE}", key = app.key)
        }
        page += (members.size + APPS_PER_PAGE - 1) / APPS_PER_PAGE
    }
    if (categories.size > 1) {
        val byCategory = apps.groupBy { it.category }
        var n = 0
        for (category in categories) {
            val members = byCategory[category.id].orEmpty()
            if (members.isEmpty()) continue
            n++
            lines += TocLine.Section("$n", category.label, "$page", key = "section:${category.id}")
            section("$n", members)
        }
    } else {
        apps.forEachIndexed { i, app ->
            lines += TocLine.Entry("${i + 1}", app, "${page + i / APPS_PER_PAGE}", key = app.key)
        }
        page += (apps.size + APPS_PER_PAGE - 1) / APPS_PER_PAGE
    }
    if (privateApps != null) {
        lines += TocLine.Section("A", "Espace privé", "$page", key = "private", privateSpace = true)
        section("A", privateApps)
    }
    return lines
}

/** Apps de la figure du sommaire : au plus [FIGURE_APPS] des plus utilisées, hors de l'accueil ([onHome]). */
internal fun usageFigure(mostUsed: List<AppEntry>, onHome: Set<String>): List<AppEntry> =
    mostUsed.asSequence().filter { it.key !in onHome }.take(FIGURE_APPS).toList()

/** Une rangée de cinq pastilles : la figure reste petite. */
private const val FIGURE_APPS = 5

/** Corps du sommaire : un peu plus grand que le texte courant, pour viser sans peine. */
private val TocText: TextStyle get() = PaperStyle.serif(18f, 23f)
private val TocBold: TextStyle get() = PaperStyle.serif(18f, 23f, bold = true)

/** Retrait d'une sous-section et largeur de son numéro (1,5 em et 2,3 em, comme article.cls). */
private val EntryIndent = 28.dp
private val EntryNumberWidth = 44.dp
private val SectionNumberWidth = 28.dp
private val PageWidth = 34.dp

/**
 * Logo au crayon de l'app devant son nom (voir [CrayonLogos] : son dessin occupe 89 % du carré), sinon
 * son motif, à la même taille de dessin ; et l'espace qui les suit.
 */
private val LogoSize = 26.dp
private val GlyphSize = 23.dp
private val GlyphGap = 10.dp

/** Pas des points de conduite, en em, et marge avant le numéro de page. */
private const val DOT_PITCH = 0.55f
private val LeaderEnd = 6.dp

/** Le sommaire du tiroir (onglet unique), avec l'espace privé en annexe. */
@Composable
internal fun TocPage(apps: AppsState, settings: LauncherSettings, badges: Map<String, Int>, vm: LauncherViewModel) {
    val privateSpace = apps.privateSpace
    val showPrivate = privateSpace != null &&
        !(privateSpace.quiet && (settings.hidePrivateSpaceWhenLocked || privateSpace.hiddenWhenLocked))
    val privateApps = if (!showPrivate) null else if (privateSpace!!.quiet) emptyList() else apps.privateApps
    val lines = remember(apps.visible, apps.categories, privateApps) {
        tableOfContents(apps.visible, apps.categories, privateApps)
    }
    // Ce qui est déjà sur l'accueil : les places de la page papier, ou la grille et le dock.
    val paperApps by vm.paperApps.collectAsStateWithLifecycle()
    val onHome = remember(settings.homeStyle, paperApps, apps.homeKeys, apps.favoriteKeys) {
        if (settings.homeStyle.paper) paperApps.values.mapNotNullTo(HashSet()) { it?.key } else apps.homeKeys + apps.favoriteKeys
    }
    val figure = remember(apps.mostUsed, onHome) { usageFigure(apps.mostUsed, onHome) }
    val listState = rememberLazyListState()

    // Déverrouillage demandé depuis le sommaire : on descend jusqu'à l'annexe.
    var scrollToPrivate by remember { mutableStateOf(false) }
    LaunchedEffect(apps.privateApps.isNotEmpty(), scrollToPrivate) {
        if (scrollToPrivate && apps.privateApps.isNotEmpty()) {
            val index = lines.indexOfFirst { it is TocLine.Section && it.privateSpace }
            if (index >= 0) listState.animateScrollToItem(index + 1)
            scrollToPrivate = false
        }
    }

    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = PaperMargin, end = PaperMargin, top = 4.dp, bottom = navBar + 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "toc-title") {
            Text("Table des matières", style = PaperStyle.title, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
        }
        if (settings.showFrequentApps && figure.isNotEmpty()) {
            item(key = "toc-figure") { UsageFigure(figure, badges, vm) }
        }
        items(lines, key = { it.key }) { line ->
            when (line) {
                is TocLine.Section -> {
                    TocSection(line)
                    if (line.privateSpace) {
                        PrivateSpaceActions(
                            state = privateSpace!!,
                            onToggleLock = {
                                if (privateSpace.quiet) scrollToPrivate = true
                                vm.setProfileQuiet(privateSpace, !privateSpace.quiet)
                            },
                            onInstall = { vm.installInProfile(privateSpace) },
                            onSettings = vm::openPrivateSpaceSettings,
                        )
                    }
                }
                is TocLine.Entry -> TocEntry(line, settings, badges, vm, indent = true)
            }
        }
    }
}

/**
 * Figure des apps les plus utilisées hors de l'accueil : une rangée de pastilles cerclées à la couleur
 * de chaque app, sans leur nom, dans un cadre de filet, et sa légende. Appui : ouvrir ; appui long :
 * actions, ou glisser vers l'accueil.
 */
@Composable
private fun UsageFigure(apps: List<AppEntry>, badges: Map<String, Int>, vm: LauncherViewModel) {
    Column(Modifier.padding(top = 12.dp, bottom = 2.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.6.dp, PaperColors.Rule)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
            for (app in apps) {
                val color = rememberAppColor(app)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(FigureCell)
                        .drawerAppGestures(
                            app,
                            canDrag = true,
                            onClick = { vm.launch(app) },
                            onLongClick = { anchor -> vm.showAppPopup(app, anchor) },
                        )
                        .alpha(if (app.isPaused) 0.45f else 1f)
                        .padding(vertical = 2.dp),
                ) {
                    // Pas de nom dessous : la pastille le porte pour la lecture d'écran (contentDescription).
                    AppGlyphDisc(app, FigureDisc, color = color, ring = 1.3.dp, colored = true, dot = (badges[app.packageName] ?: 0) > 0)
                }
            }
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                append(" – Les applis les plus utilisées, hors de l'accueil.")
            },
            style = PaperStyle.caption.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
        )
    }
}

/** Case d'une app de la figure, et sa pastille. */
private val FigureCell = 66.dp
private val FigureDisc = 46.dp

/** Titre de section : numéro, titre et page en gras, sans points de conduite. */
@Composable
private fun TocSection(line: TocLine.Section) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 2.dp),
    ) {
        if (line.number != null) Text(line.number, style = TocBold, modifier = Modifier.width(SectionNumberWidth))
        Text(line.title, style = TocBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(line.page, style = TocBold.copy(textAlign = TextAlign.End), modifier = Modifier.width(PageWidth))
    }
}

/** Actions de l'annexe « Espace privé », en liens sous son titre. */
@Composable
private fun PrivateSpaceActions(state: ProfileState, onToggleLock: () -> Unit, onInstall: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.padding(start = EntryIndent - 10.dp)) {
        PaperTextButton(if (state.quiet) "Déverrouiller" else "Verrouiller", onClick = onToggleLock, size = 14f, bold = true)
        if (!state.quiet) {
            PaperTextButton("Réglages", onClick = onSettings, size = 14f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                PaperTextButton("Installer", onClick = onInstall, size = 14f)
            }
        }
    }
}

/**
 * Une app : numéro, motif en couleur, nom (et appel de note s'il y a des notifications), points de
 * conduite, page.
 * Appui : ouvrir ; appui long : actions, ou glisser vers l'accueil. [indent] : retrait d'une sous-section.
 */
@Composable
internal fun TocEntry(
    line: TocLine.Entry,
    settings: LauncherSettings,
    badges: Map<String, Int>,
    vm: LauncherViewModel,
    indent: Boolean,
    modifier: Modifier = Modifier,
) {
    val app = line.app
    val badge = badges[app.packageName] ?: 0
    val title = remember(app.label, badge, settings.badgeStyle) {
        buildAnnotatedString {
            append(app.label)
            if (badge > 0) footnoteMark(if (settings.badgeStyle == BadgeStyle.DOT) "∗" else "$badge")
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .drawerAppGestures(
                app,
                // Les apps de l'espace privé ne vont pas sur l'accueil.
                canDrag = app.profile != ProfileType.PRIVATE,
                onClick = { vm.launch(app) },
                onLongClick = { anchor -> vm.showAppPopup(app, anchor) },
            )
            .alpha(if (app.isPaused) 0.45f else 1f),
    ) {
        // Sur la ligne de base : l'appel de note hausse la ligne du nom.
        Row(Modifier.padding(start = if (indent) EntryIndent else 0.dp)) {
            if (line.number != null) {
                Text(line.number, style = TocText, modifier = Modifier.alignByBaseline().width(EntryNumberWidth))
            }
            // Son logo au crayon, sinon le motif de l'app (celui des pages papier) à sa propre couleur.
            val logo = remember(app.packageName, app.label) { CrayonLogos.of(app) }
            val iconModifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = GlyphGap)
            if (logo != null) {
                Image(painterResource(logo), contentDescription = null, modifier = iconModifier.size(LogoSize))
            } else {
                AppGlyph(app, GlyphSize, tint = rememberAppColor(app), colored = true, modifier = iconModifier.padding(horizontal = (LogoSize - GlyphSize) / 2))
            }
            Box(
                Modifier
                    .alignByBaseline()
                    .weight(1f)
                    .drawBehind {
                        val text = layout ?: return@drawBehind
                        // Points alignés d'une ligne à l'autre : comptés depuis la colonne des pages.
                        val pitch = DOT_PITCH * TocText.fontSize.toPx()
                        val radius = 0.055f * TocText.fontSize.toPx()
                        val y = text.firstBaseline - radius
                        val start = text.getLineRight(0) + pitch * 0.6f
                        var x = size.width - LeaderEnd.toPx()
                        while (x >= start) {
                            drawCircle(PaperColors.Ink, radius, Offset(x, y))
                            x -= pitch
                        }
                    },
            ) {
                Text(title, style = TocText, maxLines = 1, overflow = TextOverflow.Ellipsis, onTextLayout = { layout = it })
            }
            Text(line.page, style = TocText.copy(textAlign = TextAlign.End), modifier = Modifier.alignByBaseline().width(PageWidth))
        }
    }
}
