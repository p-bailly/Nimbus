package fr.perso.nimbus.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

// L'interface « papier » hors des pages d'accueil : réglages, tiroir, menus et dialogues prennent
// l'allure d'un article LaTeX. Titres courants en petites capitales sur un filet, sections numérotées
// en gras, sous-titres en italique, filets fins à l'encre, liens bleus façon hyperref, notes de bas
// de page. Pas de cartes ni de fonds colorés : du texte, des filets, de l'encre.

/** Vrai sous le thème « Papier (LaTeX) » (palette claire) : les écrans prennent alors l'allure papier. */
val LocalPaperUi = staticCompositionLocalOf { false }

/** Marge latérale des écrans papier, celle des pages d'accueil. */
val PaperMargin = 22.dp

/** Styles de texte de l'interface papier (tailles en sp). */
object PaperStyle {
    fun serif(size: Float, line: Float = size * 1.36f, color: Color = PaperColors.Ink, bold: Boolean = false, italic: Boolean = false): TextStyle =
        PaperTextBase.copy(
            fontSize = size.sp,
            lineHeight = line.sp,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        )

    /** Petites capitales espacées (la police les a en romain et en gras). */
    fun smallCaps(size: Float, color: Color = PaperColors.Ink, spacing: Float = 0.06f, bold: Boolean = false): TextStyle =
        serif(size, size * 1.25f, color, bold).copy(fontFeatureSettings = "smcp", letterSpacing = spacing.em)

    /** Titre courant (en haut de page, sur le filet). */
    val runningHead: TextStyle get() = smallCaps(12f, spacing = 0.08f).copy(lineHeight = 14.sp)

    /** Titre d'une page. */
    val title: TextStyle get() = serif(27f, 33f, bold = true)

    /** Titre de section (« 1 Pages »). */
    val section: TextStyle get() = serif(17f, 22f, bold = true)

    /** Texte courant. */
    val body: TextStyle get() = serif(15f, 21f)

    /** Intitulé d'une entrée de liste. */
    val item: TextStyle get() = serif(16.5f, 22f)

    /** Précision sous une entrée : italique, gris. */
    val note: TextStyle get() = serif(13.5f, 18f, PaperColors.Soft, italic = true)

    /** Légendes et notes de bas de page. */
    val caption: TextStyle get() = serif(12.5f, 16f)
}

/** Appel de note : chiffre en exposant, à la hauteur des exposants des pages (voir MathBuilder.sup). */
fun AnnotatedString.Builder.footnoteMark(mark: String, color: Color = PaperColors.Ink) {
    withStyle(SpanStyle(fontSize = 0.69.em, baselineShift = BaselineShift(0.69f), color = color)) { append(mark) }
}

/**
 * Appui « à l'encre » : le contenu pâlit sous le doigt, sans onde grise (comme les touches des
 * maquettes papier).
 */
fun Modifier.inkClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    this
        .clickable(interaction, indication = null, enabled = enabled, onClickLabel = onClickLabel, role = role, onClick = onClick)
        .alpha(if (pressed) 0.45f else 1f)
}

/** Filet horizontal sur toute la largeur disponible. */
@Composable
fun PaperRule(modifier: Modifier = Modifier, thickness: Dp = 0.6.dp, color: Color = PaperColors.Rule) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

/**
 * Titre courant d'une page : à gauche le titre (ou « ← paramètres » en lien bleu quand [onBack] est
 * donné), à droite le folio, sur un filet de 0,6.
 */
@Composable
fun PaperRunningHead(head: String, folio: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) {
                Text(
                    "← " + head.lowercase(Locale.FRENCH),
                    style = PaperStyle.runningHead.copy(color = PaperColors.Link),
                    modifier = Modifier
                        .inkClickable(onClickLabel = "Retour", onClick = onBack)
                        .padding(top = 14.dp, bottom = 5.dp, end = 16.dp),
                )
            } else {
                Text(head, style = PaperStyle.runningHead, modifier = Modifier.padding(top = 14.dp, bottom = 5.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(folio, style = PaperStyle.runningHead.copy(fontFeatureSettings = null), modifier = Modifier.padding(bottom = 5.dp))
        }
        PaperRule()
    }
}

/** Titre de section numéroté, à la manière de \section : « 1   Pages ». */
@Composable
fun PaperSectionTitle(number: String?, title: String, modifier: Modifier = Modifier, style: TextStyle = PaperStyle.section) {
    Row(modifier) {
        if (number != null) Text(number, style = style, modifier = Modifier.width(30.dp))
        Text(title, style = style)
    }
}

/** Bouton-texte : petites capitales bleues (ou [color]), qui pâlissent à l'appui. */
@Composable
fun PaperTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = PaperColors.Link,
    bold: Boolean = false,
    size: Float = 14f,
    horizontalPadding: Dp = 10.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .inkClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding),
    ) {
        Text(
            text.lowercase(Locale.FRENCH),
            style = PaperStyle.smallCaps(size, if (enabled) color else PaperColors.Soft.copy(alpha = 0.55f), bold = bold),
            maxLines = 1,
        )
    }
}

/** Case à cocher dessinée à l'encre : ☐ ou ☒. */
@Composable
fun PaperCheckbox(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 16.dp, color: Color = PaperColors.Ink) {
    Canvas(modifier.size(size)) {
        val line = 0.9.dp.toPx()
        drawRect(color, topLeft = Offset(line / 2f, line / 2f), size = Size(this.size.width - line, this.size.height - line), style = Stroke(line))
        if (checked) {
            val inset = 3.4.dp.toPx()
            val cross = 1.1.dp.toPx()
            val end = this.size.width - inset
            drawLine(color, Offset(inset, inset), Offset(end, end), cross, StrokeCap.Round)
            drawLine(color, Offset(inset, end), Offset(end, inset), cross, StrokeCap.Round)
        }
    }
}

/** Bouton radio dessiné à l'encre : ○ ou ●. */
@Composable
fun PaperRadio(selected: Boolean, modifier: Modifier = Modifier, size: Dp = 15.dp, color: Color = PaperColors.Ink) {
    Canvas(modifier.size(size)) {
        val line = 0.9.dp.toPx()
        drawCircle(color, radius = this.size.minDimension / 2f - line / 2f, style = Stroke(line))
        if (selected) drawCircle(color, radius = this.size.minDimension * 0.24f)
    }
}

private val PaperSelectionColors = TextSelectionColors(handleColor = PaperColors.Link, backgroundColor = PaperColors.Link.copy(alpha = 0.22f))

/**
 * Champ de saisie souligné, précédé de son intitulé en petites capitales (« Rechercher : ___ »).
 * Le filet s'épaissit quand le champ a le focus.
 */
@Composable
fun PaperTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = PaperStyle.item,
    fieldModifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (label != null) {
            Text(
                label.lowercase(Locale.FRENCH) + " :",
                style = PaperStyle.smallCaps(14.5f, spacing = 0.05f),
                modifier = Modifier
                    .alignByBaseline()
                    .padding(end = 10.dp),
            )
        }
        Box(
            Modifier
                .weight(1f)
                .alignByBaseline()
                .drawBehind {
                    // Le filet passe juste sous les jambages, comme une ligne de formulaire.
                    val line = (if (focused) 1.2.dp else 0.8.dp).toPx()
                    drawRect(PaperColors.Ink, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                }
                .padding(top = 8.dp, bottom = 2.dp),
        ) {
            // Sélection en bleu pâle : la seule touche de couleur du champ.
            CompositionLocalProvider(LocalTextSelectionColors provides PaperSelectionColors) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(PaperColors.Ink),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = fieldModifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        Box {
                            if (value.text.isEmpty() && placeholder != null) {
                                Text(placeholder, style = textStyle.copy(color = PaperColors.Soft.copy(alpha = 0.8f), fontStyle = FontStyle.Italic), maxLines = 1)
                            }
                            inner()
                        }
                    },
                )
            }
        }
        trailing?.invoke()
    }
}

/** [PaperTextField] pour un simple texte. */
@Composable
fun PaperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textStyle: TextStyle = PaperStyle.item,
    fieldModifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    // Même logique que BasicTextField(String) : la sélection est gardée tant que le texte ne change pas.
    var state by remember { mutableStateOf(TextFieldValue(value)) }
    val current = if (state.text == value) state else TextFieldValue(value, state.selection)
    PaperTextField(
        value = current,
        onValueChange = {
            state = it
            if (it.text != value) onValueChange(it.text)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        fieldModifier = fieldModifier,
        trailing = trailing,
    )
}

/**
 * Note de bas de page : petit filet, appel de note en exposant, texte en corps 12,5. [link] : la
 * note entière est un lien (bleu) qui déclenche [onClick].
 */
@Composable
fun PaperFootnote(
    mark: String,
    text: String,
    modifier: Modifier = Modifier,
    rule: Boolean = true,
    markColor: Color = PaperColors.Ink,
    color: Color = PaperColors.Ink,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier) {
        if (rule) {
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .size(148.dp, 0.6.dp)
                    .background(PaperColors.Rule),
            )
        }
        val annotated = buildAnnotatedString {
            footnoteMark(mark, markColor)
            append(" ")
            withStyle(SpanStyle(color = if (onClick != null) PaperColors.Link else color)) { append(text) }
            if (onClick != null && !text.endsWith(".")) append(".")
        }
        Text(
            annotated,
            // L'appel déborde à gauche : les lignes suivantes s'alignent sur le texte de la première.
            style = PaperStyle.caption.copy(textIndent = TextIndent(firstLine = 0.6.em, restLine = 1.12.em)),
            modifier = if (onClick != null) Modifier.inkClickable(onClick = onClick).padding(vertical = 6.dp) else Modifier,
        )
    }
}

/**
 * Cadre d'un dialogue papier : fond papier, filet d'encre, angles droits, sans ombre. Titre en gras,
 * contenu (qui défile s'il est long), puis actions alignées à droite.
 */
@Composable
fun PaperDialogFrame(
    title: String?,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .widthIn(min = 280.dp, max = 560.dp)
            .background(PaperColors.Paper)
            .border(0.8.dp, PaperColors.Rule)
            .padding(top = 22.dp, bottom = if (actions != null) 6.dp else 18.dp),
    ) {
        if (title != null) {
            Text(
                title,
                style = PaperStyle.serif(19f, 24f, bold = true),
                modifier = Modifier.padding(start = PaperMargin, end = PaperMargin, bottom = 12.dp),
            )
        }
        Column(
            Modifier
                .weight(1f, fill = false)
                .padding(horizontal = PaperMargin),
        ) { content() }
        if (actions != null) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                content = actions,
            )
        }
    }
}

/**
 * Fenêtre d'un dialogue papier : sa largeur vient de marges fixes (28 dp de chaque côté), pas de la
 * plateforme, pour un cadre identique d'un téléphone à l'autre.
 */
@Composable
fun PaperDialogWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(horizontal = 28.dp, vertical = 24.dp), contentAlignment = Alignment.Center) { content() }
    }
}

/** Dialogue papier : [PaperDialogFrame] dans une [PaperDialogWindow]. */
@Composable
fun PaperDialog(
    onDismissRequest: () -> Unit,
    title: String?,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    PaperDialogWindow(onDismissRequest) {
        PaperDialogFrame(title = title, actions = actions, content = content)
    }
}

/** Poignée d'une feuille papier : un petit filet. [edge] : filet d'encre sur le bord haut de la feuille. */
@Composable
fun PaperSheetHandle(edge: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .then(
                if (edge) Modifier.drawBehind { drawRect(PaperColors.Rule, size = Size(size.width, 0.8.dp.toPx())) }
                else Modifier,
            ),
    ) {
        Box(
            Modifier
                .size(32.dp, 0.8.dp)
                .background(PaperColors.Ink.copy(alpha = 0.55f)),
        )
    }
}

/**
 * Feuille du bas. Sous le thème papier : fond papier à angles droits, bordée en haut d'un filet
 * d'encre, poignée en filet ; sinon la feuille Material habituelle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimbusBottomSheet(onDismissRequest: () -> Unit, sheetState: SheetState, content: @Composable ColumnScope.() -> Unit) {
    if (LocalPaperUi.current) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            shape = RectangleShape,
            containerColor = PaperColors.Paper,
            contentColor = PaperColors.Ink,
            tonalElevation = 0.dp,
            dragHandle = { PaperSheetHandle(edge = true) },
            content = content,
        )
    } else {
        ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState, content = content)
    }
}

/** Ligne de liste papier : intitulé en romain, précision en italique gris, élément à droite. */
@Composable
fun PaperListRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleStyle: TextStyle = PaperStyle.item,
    maxTitleLines: Int = 2,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.inkClickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = PaperMargin, vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, style = titleStyle, maxLines = maxTitleLines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, style = PaperStyle.note)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(14.dp))
            trailing()
        }
    }
}

/** Paragraphe d'article : justifié, alinéa de 1,5 em, dans les marges. */
@Composable
fun PaperParagraph(text: AnnotatedString, modifier: Modifier = Modifier, style: TextStyle = PaperStyle.body) {
    Text(
        text,
        style = style.copy(textAlign = TextAlign.Justify, textIndent = TextIndent(firstLine = 1.5.em)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PaperMargin, vertical = 6.dp),
    )
}

@Composable
fun PaperParagraph(text: String, modifier: Modifier = Modifier, style: TextStyle = PaperStyle.body) =
    PaperParagraph(AnnotatedString(text), modifier, style)

/** Paragraphe à intitulé en gras (« Remarque. … »), comme \paragraph. */
fun runIn(head: String, text: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(head) }
    append(" ")
    append(text)
}

/** Chevron « › » des entrées qui ouvrent une page ou un choix. */
@Composable
fun PaperChevron() {
    Text("›", style = PaperStyle.serif(22f, 22f, PaperColors.Soft))
}
