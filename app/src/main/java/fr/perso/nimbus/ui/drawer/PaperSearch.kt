package fr.perso.nimbus.ui.drawer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.BadgeStyle
import fr.perso.nimbus.data.ContactResult
import fr.perso.nimbus.data.FileResult
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.SearchResults
import fr.perso.nimbus.ui.components.AppGlyph
import fr.perso.nimbus.ui.components.drawerAppGestures
import fr.perso.nimbus.ui.components.rememberAppColor
import fr.perso.nimbus.ui.settings.SettingsColors
import fr.perso.nimbus.ui.settings.SettingsHit
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperSelectionColors
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.footnoteMark
import fr.perso.nimbus.ui.theme.inkClickable

// Recherche du tiroir sous le thème papier : une démonstration. Le champ se lit « soit q = … » ; une
// Proposition, fixe sous le champ, dit ce que fait la touche Entrée (la recherche web, ou la première
// app) ; puis chaque famille de résultats est un ensemble { … } dans une bande teintée, avec son
// cardinal |A| = n : Apps (bleu), Contacts (orange), Fichiers (turquoise), Réglages (gris). Une
// famille sans résultat n'a pas de bande ; s'il n'y a de résultat nulle part, un Corollaire dit que
// l'ensemble des résultats de q est vide. Une famille dont l'accès manque s'écrit « ? ».

/** Couleurs d'accent des familles de résultats. */
private object Accent {
    val Apps = PaperColors.Link
    val Contacts = SettingsColors.Orange
    val Files = SettingsColors.Teal
    val Settings = PaperColors.Soft
    val PlayStore = SettingsColors.Green

    /** Teinte d'un accent assez foncée pour du texte ou un trait sur le papier crème et sur sa bande. */
    fun ink(accent: Color): Color = lerp(accent, PaperColors.Ink, 0.35f)
}

/** Au-delà de ce nombre de lignes, une bande n'en montre que [COLLAPSED] et propose « … et N autres ». */
private const val COLLAPSED = 3
private const val COLLAPSE_FROM = 5

private val NameWidth = 92.dp
private val BraceWidth = 12.dp
private val RowHeight = 40.dp

// ---------------------------------------------------------------- Champ

/** « soit q = », le champ souligné de bleu, et à droite « effacer » (ou « paramètres » quand il est vide). */
@Composable
internal fun PaperSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    focusRequester: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    val label = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.05.em)) { append("soit ") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontSize = 17.sp)) { append("q") }
            append(" =")
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PaperMargin),
    ) {
        Text(label, style = PaperStyle.serif(15f), modifier = Modifier.alignByBaseline().padding(end = 10.dp))
        Box(
            Modifier
                .weight(1f)
                .alignByBaseline()
                .drawBehind {
                    // Le filet passe juste sous les jambages, comme une ligne de formulaire.
                    val line = (if (focused) 1.4.dp else 1.dp).toPx()
                    drawRect(PaperColors.Link, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                }
                .padding(top = 8.dp, bottom = 2.dp),
        ) {
            CompositionLocalProvider(LocalTextSelectionColors provides PaperSelectionColors) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = PaperStyle.item,
                    cursorBrush = SolidColor(PaperColors.Link),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = "Rechercher" },
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    "app, contact…",
                                    style = PaperStyle.item.copy(color = PaperColors.Soft.copy(alpha = 0.8f), fontStyle = FontStyle.Italic),
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }
        if (query.isNotEmpty()) PaperTextButton("Effacer", onClick = { onQueryChange("") }, horizontalPadding = 0.dp, modifier = Modifier.padding(start = 14.dp))
        else PaperTextButton("Paramètres", onClick = onOpenSettings, horizontalPadding = 0.dp, modifier = Modifier.padding(start = 14.dp))
    }
}

// ---------------------------------------------------------------- Page

/** Résultats : la Proposition, fixe, puis les bandes, qui défilent. */
@Composable
internal fun PaperSearchPage(
    query: String,
    results: SearchResults,
    apps: AppsState,
    settings: LauncherSettings,
    badges: Map<String, Int>,
    contactsBlocked: Boolean,
    filesBlocked: Boolean,
    vm: LauncherViewModel,
    onRequestContacts: () -> Unit,
    onRequestFiles: () -> Unit,
) {
    val normalized = SearchMatcher.normalize(query)
    val privateSpace = apps.privateSpace
    val showPrivateEntry = privateSpace != null && normalized.length >= 3 &&
        PRIVATE_SPACE_WORDS.any { it.startsWith(normalized) }
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Les bandes montrées, dans l'ordre : la première porte le filet du haut. Une famille sans résultat n'en
    // a pas (sauf l'accès qui manque) ; s'il n'y a de résultat dans aucune famille, le Corollaire les remplace.
    val empty = noResultAnywhere(results, showPrivateEntry)
    val bands = buildList {
        if (results.apps.isNotEmpty()) add(Band.APPS)
        if (results.contacts.isNotEmpty() || contactsBlocked) add(Band.CONTACTS)
        if (results.files.isNotEmpty() || filesBlocked) add(Band.FILES)
        if (results.settings.isNotEmpty()) add(Band.SETTINGS)
    }

    Column(Modifier.fillMaxSize()) {
        Proposition(query, results, settings, vm)
        LazyColumn(
            contentPadding = PaddingValues(bottom = navBar + 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showPrivateEntry) {
                item(key = "private-search") {
                    PrivateSpaceHeader(
                        state = privateSpace!!,
                        onToggleLock = { vm.setProfileQuiet(privateSpace, !privateSpace.quiet) },
                        onInstall = { vm.installInProfile(privateSpace) },
                        onSettings = vm::openPrivateSpaceSettings,
                    )
                }
            }

            if (empty) {
                item(key = "no-result") { NoResult(query) }
            }

            if (Band.APPS in bands) {
                item(key = "band-apps") {
                    ResultBand(
                        name = "Apps", letter = "A", accent = Accent.Apps, items = results.apps,
                        query = query, first = bands.first() == Band.APPS,
                    ) { app, comma ->
                        AppRow(app, query, badges[app.packageName] ?: 0, settings, comma, vm)
                    }
                }
            }

            if (Band.CONTACTS in bands) {
                item(key = "band-contacts") {
                    if (results.contacts.isNotEmpty()) {
                        ResultBand(
                            name = "Contacts", letter = "C", accent = Accent.Contacts, items = results.contacts,
                            query = query, first = bands.first() == Band.CONTACTS, tint = 0.10f,
                        ) { contact, comma ->
                            ContactRow(contact, query, comma) { vm.openContact(contact) }
                        }
                    } else {
                        BlockedBand("Contacts", "autoriser l'accès", onRequestContacts, first = bands.first() == Band.CONTACTS)
                    }
                }
            }

            if (Band.FILES in bands) {
                item(key = "band-files") {
                    if (results.files.isNotEmpty()) {
                        ResultBand(
                            name = "Fichiers", letter = "F", accent = Accent.Files, items = results.files,
                            query = query, first = bands.first() == Band.FILES,
                        ) { file, comma ->
                            FileRow(file, query, comma) { vm.openFile(file) }
                        }
                    } else {
                        BlockedBand("Fichiers", "autoriser l'accès", onRequestFiles, first = bands.first() == Band.FILES)
                    }
                }
            }

            if (Band.SETTINGS in bands) {
                item(key = "band-settings") {
                    ResultBand(
                        name = "Réglages", letter = "R", accent = Accent.Settings, items = results.settings,
                        query = query, first = bands.first() == Band.SETTINGS, tint = 0.07f,
                    ) { hit, comma ->
                        SettingsRow(hit, query, comma) { vm.openSettings(hit.section) }
                    }
                }
            }
        }
    }
}

/** Les familles de résultats, dans l'ordre des bandes. */
private enum class Band { APPS, CONTACTS, FILES, SETTINGS }

/**
 * Vrai s'il n'y a de résultat nulle part (ni app, ni contact, ni fichier, ni réglage, ni espace privé
 * proposé en [privateEntry]) : le seul cas où le Corollaire « l'ensemble est vide » s'écrit. Tant que les contacts et les
 * fichiers sont encore cherchés ([SearchResults.pending]), on ne sait pas : rien ne s'écrit.
 */
internal fun noResultAnywhere(results: SearchResults, privateEntry: Boolean = false): Boolean =
    !privateEntry && !results.pending &&
        results.apps.isEmpty() && results.contacts.isEmpty() && results.files.isEmpty() && results.settings.isEmpty()

// ---------------------------------------------------------------- Proposition

/**
 * « Proposition 1. La touche ↵ envoie q à [Google]. » : ce que fait la touche Entrée, et en
 * « Démonstration » les autres moteurs. Fixe sous le champ : Google est toujours à portée.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Proposition(query: String, results: SearchResults, settings: LauncherSettings, vm: LauncherViewModel) {
    val engineName = if (settings.webSearchEngine.urlTemplate == null) "Web" else settings.webSearchEngine.label
    // Entrée ouvre la première app (si le réglage le veut et que les résultats sont ceux de la saisie).
    val first = results.apps.firstOrNull()?.takeIf { settings.enterOpensFirstApp && results.query == query }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PaperMargin, end = PaperMargin, top = 10.dp, bottom = 12.dp)
            .drawBehind { drawRect(PaperColors.Link, size = Size(2.5.dp.toPx(), size.height)) }
            .padding(start = 14.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Prose("Proposition 1.", bold = true)
            Prose("La touche", italic = true)
            Box(Modifier.height(ProseHeight), contentAlignment = Alignment.Center) { ReturnArrow() }
            Prose(if (first != null) "ouvre" else "envoie", italic = true)
            if (first == null) {
                Prose("q", italic = true)
                Prose("à", italic = true)
                FramedLink(engineName, PaperColors.Link, bold = true, description = "Rechercher « ${query.trim()} » sur $engineName") {
                    vm.webSearch(query)
                }
            } else {
                FramedLink(first.label, PaperColors.Link, bold = true, description = "Ouvrir ${first.label}") { vm.launch(first) }
            }
            Prose(".", italic = true)
        }
        // Le carré de fin de démonstration ferme la dernière ligne, à droite, sans point avant lui : un
        // point de plus passerait à la ligne et la Démonstration en prendrait une de plus.
        Row(verticalAlignment = Alignment.Bottom) {
            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Prose("Démonstration.", bold = true)
                Prose("Voir aussi", italic = true)
                if (first != null) {
                    FramedLink(engineName, PaperColors.Link, description = "Rechercher « ${query.trim()} » sur $engineName") { vm.webSearch(query) }
                }
                if (settings.searchPlayStore) {
                    FramedLink("Play Store", Accent.PlayStore, description = "Rechercher « ${query.trim()} » sur le Play Store") {
                        vm.playStoreSearch(query)
                    }
                }
            }
            EndOfProof(Modifier.padding(start = 5.dp))
        }
    }
}

/** Fin de la démonstration : un carré noir vide (□), centré sur la hauteur d'une ligne de la Proposition. */
@Composable
private fun EndOfProof(modifier: Modifier = Modifier, description: String = "Fin de la démonstration") {
    Box(modifier.height(ProseHeight), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(12.dp)
                .border(1.3.dp, PaperColors.Ink)
                .semantics { contentDescription = description },
        )
    }
}

private val ProseHeight = 36.dp

/** Un morceau de phrase de la Proposition, centré sur la hauteur des liens. */
@Composable
private fun Prose(text: String, bold: Boolean = false, italic: Boolean = false) {
    Text(
        text,
        style = PaperStyle.serif(16f, 21f, bold = bold, italic = italic),
        modifier = Modifier
            .heightIn(min = ProseHeight)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/** Lien à la manière d'hyperref : le texte dans un filet de sa couleur. La zone d'appui est plus haute que le filet. */
@Composable
private fun FramedLink(text: String, color: Color, bold: Boolean = false, description: String? = null, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = ProseHeight)
            .inkClickable(onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Text(
            text,
            style = PaperStyle.serif(16f, 21f, Accent.ink(color), bold = bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .border(1.dp, color)
                .padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}

/** La touche Entrée : une flèche qui revient à la ligne, dessinée au trait. */
@Composable
private fun ReturnArrow(size: Dp = 15.dp, color: Color = PaperColors.Ink) {
    Canvas(
        Modifier
            .size(size)
            .semantics { contentDescription = "Entrée" },
    ) {
        val s = this.size.width / 24f
        val path = Path().apply {
            moveTo(20 * s, 5 * s); lineTo(20 * s, 12 * s); lineTo(5 * s, 12 * s)
            moveTo(5 * s, 12 * s); lineTo(9 * s, 8 * s)
            moveTo(5 * s, 12 * s); lineTo(9 * s, 16 * s)
        }
        drawPath(path, color, style = Stroke(width = 2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ---------------------------------------------------------------- Bandes

/** Fond teinté de la bande, et son filet du bas (du haut aussi pour la première) ; [dashed] : filet en pointillés. */
private fun Modifier.bandBackground(accent: Color, tint: Float, first: Boolean, dashed: Boolean = false): Modifier = drawBehind {
    val thickness = 1.dp.toPx()
    val rule = accent.copy(alpha = 0.4f)
    if (tint > 0f) drawRect(accent.copy(alpha = tint))
    if (first) drawRect(rule, size = Size(size.width, thickness))
    if (dashed) {
        drawLine(
            accent.copy(alpha = 0.6f),
            Offset(0f, size.height - thickness / 2),
            Offset(size.width, size.height - thickness / 2),
            strokeWidth = thickness,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    } else {
        drawRect(rule, topLeft = Offset(0f, size.height - thickness), size = Size(size.width, thickness))
    }
}

/** L'accolade ouvrante { qui s'étire sur toute la hauteur de la bande : mêmes proportions que la maquette. */
private fun DrawScope.drawBrace(color: Color) {
    val w = BraceWidth.toPx()
    val sx = w / 12f
    val sy = size.height / 100f
    val path = Path().apply {
        moveTo(12 * sx, 1 * sy)
        cubicTo(6 * sx, 1 * sy, 6 * sx, 4 * sy, 6 * sx, 12 * sy)
        lineTo(6 * sx, 38 * sy)
        cubicTo(6 * sx, 46 * sy, 3 * sx, 50 * sy, 0f, 50 * sy)
        cubicTo(3 * sx, 50 * sy, 6 * sx, 54 * sy, 6 * sx, 62 * sy)
        lineTo(6 * sx, 88 * sy)
        cubicTo(6 * sx, 96 * sy, 6 * sx, 99 * sy, 12 * sx, 99 * sy)
    }
    drawPath(path, color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * Une famille de résultats : « Apps = { a, b, c, … } |A| = 14 », sur une bande teintée. Au-delà de
 * [COLLAPSE_FROM] lignes, trois seulement sont montrées, et « … et N autres » déplie la bande ; l'accolade
 * suit la hauteur en douceur.
 */
@Composable
private fun <T> ResultBand(
    name: String,
    letter: String,
    accent: Color,
    items: List<T>,
    query: String,
    modifier: Modifier = Modifier,
    first: Boolean = false,
    tint: Float = 0.09f,
    row: @Composable (item: T, comma: Boolean) -> Unit,
) {
    var expanded by remember(query) { mutableStateOf(false) }
    val shown = if (expanded || items.size < COLLAPSE_FROM) items else items.take(COLLAPSED)
    val more = items.size - shown.size
    val ink = Accent.ink(accent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .bandBackground(accent, tint, first)
            .padding(horizontal = PaperMargin, vertical = 10.dp),
    ) {
        Text(
            "$name =",
            style = PaperStyle.serif(16.5f, 21f, ink, italic = true),
            softWrap = false,
            modifier = Modifier
                .width(NameWidth)
                .semantics {
                    heading()
                    contentDescription = if (more > 0) "$name, ${shown.size} résultats sur ${items.size}" else "$name, ${items.size} résultats"
                },
        )
        Column(
            Modifier
                .weight(1f)
                .animateContentSize()
                .drawBehind { drawBrace(accent) }
                .padding(start = BraceWidth + 6.dp),
        ) {
            shown.forEachIndexed { i, item -> row(item, i < shown.lastIndex || more > 0) }
            if (items.size >= COLLAPSE_FROM) {
                Text(
                    if (expanded) "réduire" else "… et $more autres",
                    style = PaperStyle.serif(15f, 20f, ink, italic = true).copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier
                        .inkClickable(onClickLabel = if (expanded) "Réduire la liste" else "Voir les $more autres") { expanded = !expanded }
                        .heightIn(min = 36.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
        Text(
            "|$letter| = ${items.size}",
            style = PaperStyle.serif(14f, 18f, ink, italic = true),
            modifier = Modifier
                .padding(start = 8.dp)
                .clearAndSetSemantics { },
        )
    }
}

/**
 * Rien ne correspond nulle part : « Corollaire. L'ensemble des résultats de q est vide. □ », sous le
 * même filet (gris) que la Proposition (bleu). Aucune bande : aucune famille n'est nommée à tort.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoResult(query: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PaperMargin, end = PaperMargin, top = 6.dp, bottom = 12.dp)
            .drawBehind { drawRect(Accent.Settings, size = Size(2.5.dp.toPx(), size.height)) }
            .padding(start = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Aucun résultat pour « ${query.trim()} »" },
    ) {
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Prose("Corollaire.", bold = true)
            Prose("L'ensemble des résultats de", italic = true)
            Prose("q", italic = true)
            Prose("est vide.", italic = true)
        }
        EndOfProof(Modifier.padding(start = 5.dp), description = "Fin du corollaire")
    }
}

/** Famille dont l'accès manque : « Contacts = ? autorisation requise [autoriser l'accès] », filet en pointillés. */
@Composable
private fun BlockedBand(name: String, action: String, onClick: () -> Unit, first: Boolean = false) {
    val ink = Accent.ink(Accent.Settings)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .bandBackground(Accent.Settings, 0f, first = first, dashed = true)
            .padding(horizontal = PaperMargin, vertical = 10.dp),
    ) {
        Text("$name =", style = PaperStyle.serif(16.5f, 21f, ink, italic = true), softWrap = false, modifier = Modifier.width(NameWidth))
        Text("?", style = PaperStyle.serif(26f, 28f, ink, italic = true))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("autorisation requise", style = PaperStyle.note)
            FramedLink(action, PaperColors.Link, onClick = onClick)
        }
    }
}

// ---------------------------------------------------------------- Lignes

private val RowText: TextStyle get() = PaperStyle.serif(17f, 22f)

/** Un nom, ses lettres trouvées surlignées, suivi d'une virgule (hors de la lecture d'écran). */
@Composable
private fun ResultLine(
    comma: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().heightIn(min = RowHeight)) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Box(Modifier.weight(1f, fill = false)) { content() }
        if (comma) Text(",", style = RowText, modifier = Modifier.clearAndSetSemantics { })
    }
}

/**
 * Une app : son logo au crayon (sinon son motif), son nom surligné et, s'il y en a, l'appel de note des
 * notifications. Appui : ouvrir ; appui long : actions, ou glisser vers l'accueil.
 */
@Composable
private fun AppRow(app: AppEntry, query: String, badge: Int, settings: LauncherSettings, comma: Boolean, vm: LauncherViewModel) {
    val title = remember(app.label, query, badge, settings.badgeStyle) {
        buildAnnotatedString {
            append(SearchMatcher.highlight(app.label, query))
            if (badge > 0) footnoteMark(if (settings.badgeStyle == BadgeStyle.DOT) "∗" else "$badge")
        }
    }
    ResultLine(
        comma = comma,
        modifier = Modifier
            .drawerAppGestures(
                app,
                // Les apps de l'espace privé ne vont pas sur l'accueil.
                canDrag = app.profile != ProfileType.PRIVATE,
                onClick = { vm.launch(app) },
                onLongClick = { anchor -> vm.showAppPopup(app, anchor) },
            )
            .alpha(if (app.isPaused) 0.45f else 1f),
        leading = { AppGlyph(app, 26.dp, tint = rememberAppColor(app), colored = true) },
    ) {
        Text(title, style = RowText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Un contact : son initiale en italique dans un cercle au filet orange. */
@Composable
private fun ContactRow(contact: ContactResult, query: String, comma: Boolean, onClick: () -> Unit) {
    val name = remember(contact.name, query) { SearchMatcher.highlight(contact.name, query) }
    ResultLine(
        comma = comma,
        modifier = Modifier.inkClickable(onClick = onClick),
        leading = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Accent.Contacts, CircleShape),
            ) {
                Text(
                    contact.name.firstOrNull()?.uppercase() ?: "?",
                    style = PaperStyle.serif(14f, 16f, Accent.ink(Accent.Contacts), italic = true),
                )
            }
        },
    ) {
        Text(name, style = RowText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Un fichier : son nom surligné, et son dossier en petit italique dessous. */
@Composable
private fun FileRow(file: FileResult, query: String, comma: Boolean, onClick: () -> Unit) {
    val name = remember(file.name, query) { SearchMatcher.highlight(file.name, query) }
    ResultLine(comma = comma, modifier = Modifier.inkClickable(onClick = onClick).padding(vertical = 4.dp)) {
        Column {
            Text(name, style = RowText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!file.folder.isNullOrEmpty()) {
                Text(file.folder, style = PaperStyle.note.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Un réglage de Nimbus, en gris : son intitulé surligné, et sa page en petit italique dessous. */
@Composable
private fun SettingsRow(hit: SettingsHit, query: String, comma: Boolean, onClick: () -> Unit) {
    val title = remember(hit.title, query) { SearchMatcher.highlight(hit.title, query) }
    ResultLine(comma = comma, modifier = Modifier.inkClickable(onClick = onClick).padding(vertical = 4.dp)) {
        Column {
            Text(title, style = RowText.copy(color = PaperColors.Soft), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(hit.path, style = PaperStyle.note.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
