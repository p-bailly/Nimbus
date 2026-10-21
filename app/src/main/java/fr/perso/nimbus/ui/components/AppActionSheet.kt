package fr.perso.nimbus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.Category
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.GlyphChoice
import fr.perso.nimbus.data.PaperLogo
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.NimbusBottomSheet
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperRule
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.inkClickable
import kotlinx.coroutines.launch

private enum class SheetDialog { CATEGORY, NEW_CATEGORY, RENAME, SWIPE, ICON_APP, GLYPH, GLYPH_TEXT, GLYPH_APP }

/** Option « Nouvelle catégorie… » du choix de catégorie. */
private const val NEW_CATEGORY = "+new"

/** Choix du dialogue « Motif » ; les lettres et l'autre app ouvrent un second dialogue. */
private const val GLYPH_AUTO = "auto"
private const val GLYPH_TEXT = "text"
private const val GLYPH_APP = "app"

/** Description d'un motif : « logo YouTube », « lettres +x », « silhouette de Drive ». */
private fun describe(choice: GlyphChoice?, apps: AppsState): String = when (choice) {
    is GlyphChoice.Logo -> choice.logo.label.replaceFirstChar { it.lowercase() }
    is GlyphChoice.Text -> "lettres ${choice.text}"
    is GlyphChoice.OfApp -> "silhouette de ${apps.byKey[choice.key]?.label ?: "l'app choisie"}"
    GlyphChoice.Own, null -> "sa silhouette"
}

/**
 * Feuille d'actions d'une app (appui long) : raccourcis de l'app, accueil, dock,
 * glissé vers le haut, nom, icône, catégorie, masquage, infos et désinstallation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: AppEntry,
    apps: AppsState,
    vm: LauncherViewModel,
    host: LauncherHost,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val overrides by vm.categoryOverrides.collectAsStateWithLifecycle()
    val customLabels by vm.customLabels.collectAsStateWithLifecycle()
    val customIcons by vm.customIconKeys.collectAsStateWithLifecycle()
    val swipeActions by vm.iconSwipeActions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val glyphs by vm.glyphChoices.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SheetDialog?>(null) }

    val isPrivate = app.profile == ProfileType.PRIVATE
    val isFavorite = app.key in apps.favoriteKeys
    val onHome = app.key in apps.homeKeys
    val isHidden = app.key in apps.hiddenKeys

    val shortcuts = rememberAppShortcuts(app)

    // Referme la feuille avec son animation, puis exécute l'action.
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            vm.dismissAppSheet()
            action()
        }
    }

    val paper = LocalPaperUi.current
    NimbusBottomSheet(onDismissRequest = vm::dismissAppSheet, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            val detail = buildString {
                append(if (isPrivate) "Espace privé" else apps.categoryLabel(app.category))
                if (app.profile == ProfileType.WORK) append(" · Profil professionnel")
                if (app.isPaused) append(" (en pause)")
            }
            if (paper) {
                PaperSheetHeader(app, detail)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Image(app.icon, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (shortcuts.isNotEmpty()) {
                SectionTitle("Raccourcis")
                for (item in shortcuts) {
                    SheetRow(
                        label = item.label,
                        onClick = { closeThen { vm.startShortcut(item.info) } },
                        leading = {
                            if (item.icon != null) Image(item.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                            else Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        },
                    )
                }
                if (paper) PaperRule(Modifier.padding(vertical = 8.dp, horizontal = PaperMargin))
                else HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 24.dp))
            }

            // L'espace privé ne doit pas fuiter sur l'accueil ni dans le dock.
            if (!isPrivate) {
                SheetRow(
                    label = if (onHome) "Retirer de l'accueil" else "Ajouter à l'accueil",
                    icon = Icons.Filled.Home,
                    onClick = { closeThen { if (onHome) vm.removeFromHome(app) else vm.addToHome(app) } },
                )
                SheetRow(
                    label = if (isFavorite) "Retirer du dock" else "Ajouter au dock",
                    icon = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    onClick = { closeThen { vm.toggleFavorite(app) } },
                )
                if (onHome || isFavorite) {
                    val target = swipeActions[app.key]?.let { apps.byKey[it] }
                    SheetRow(
                        label = "Glisser vers le haut : " + (target?.label ?: "aucune action"),
                        icon = Icons.Filled.KeyboardArrowUp,
                        onClick = { dialog = SheetDialog.SWIPE },
                    )
                }
            }
            // Rien de ce qui concerne l'espace privé n'est enregistré (cela finirait dans les sauvegardes).
            if (!isPrivate) {
                SheetRow(label = "Renommer", icon = Icons.Filled.Edit, onClick = { dialog = SheetDialog.RENAME })
                SheetRow(
                    label = "Changer l'icône (galerie)",
                    icon = Icons.Filled.Face,
                    onClick = { closeThen { host.pickCustomIcon(app) } },
                )
                SheetRow(label = "Icône d'une autre app", icon = Icons.Filled.Face, onClick = { dialog = SheetDialog.ICON_APP })
                if (app.key in customIcons) {
                    SheetRow(label = "Icône d'origine", icon = Icons.Filled.Refresh, onClick = { closeThen { vm.resetCustomIcon(app) } })
                }
                // Motif des pages papier et du sommaire (silhouette monochrome, logo ou lettres).
                if (paper || settings.homeStyle.paper) {
                    SheetRow(
                        label = "Motif : ${describe(glyphs[app.key], apps)}",
                        icon = Icons.Filled.Face,
                        onClick = { dialog = SheetDialog.GLYPH },
                    )
                }
                SheetRow(
                    label = "Catégorie : ${apps.categoryLabel(app.category)}",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = { dialog = SheetDialog.CATEGORY },
                )
                SheetRow(
                    label = if (isHidden) "Ne plus masquer" else "Masquer l'app",
                    icon = Icons.Filled.Lock,
                    onClick = { closeThen { vm.setHidden(app, !isHidden) } },
                )
            }
            SheetRow(label = "Infos sur l'app", icon = Icons.Filled.Info, onClick = { closeThen { vm.appInfo(app) } })
            SheetRow(label = "Désinstaller", icon = Icons.Filled.Delete, onClick = { closeThen { vm.uninstall(app) } })
        }
    }

    when (dialog) {
        SheetDialog.CATEGORY -> {
            val automatic = apps.allCategories.firstOrNull { it.id == app.autoCategory.name }?.label ?: apps.categoryLabel(Category.OTHER_ID)
            ChoiceDialog(
                title = "Catégorie de ${app.label}",
                options = listOf<String?>(null) + apps.allCategories.map { it.id } + NEW_CATEGORY,
                selected = overrides[app.key]?.takeIf { id -> apps.allCategories.any { it.id == id } },
                label = {
                    when (it) {
                        null -> "Automatique ($automatic)"
                        NEW_CATEGORY -> "Nouvelle catégorie…"
                        else -> apps.categoryLabel(it)
                    }
                },
                onSelect = {
                    if (it == NEW_CATEGORY) {
                        dialog = SheetDialog.NEW_CATEGORY
                    } else {
                        dialog = null
                        closeThen { vm.setCategory(app, it) }
                    }
                },
                onDismiss = { dialog = null },
                footer = "Les catégories se créent, se renomment et se réordonnent aussi dans Paramètres › Page des applications.",
            )
        }
        SheetDialog.NEW_CATEGORY -> TextInputDialog(
            title = "Nouvelle catégorie",
            initial = "",
            placeholder = "Nom de la catégorie",
            onConfirm = { name ->
                dialog = null
                closeThen { vm.addCategory(name)?.let { vm.setCategory(app, it.id) } }
            },
            onDismiss = { dialog = null },
        )
        SheetDialog.RENAME -> TextInputDialog(
            title = "Renommer",
            initial = app.label,
            placeholder = vm.originalLabel(app),
            onConfirm = {
                dialog = null
                closeThen { vm.rename(app, it) }
            },
            onDismiss = { dialog = null },
            onReset = if (app.key in customLabels) {
                {
                    dialog = null
                    closeThen { vm.rename(app, null) }
                }
            } else {
                null
            },
        )
        SheetDialog.SWIPE -> AppPickerDialog(
            title = "Glisser vers le haut sur ${app.label}",
            apps = apps.visible.filter { it.key != app.key },
            noneLabel = "Aucune action",
            onPick = { target ->
                dialog = null
                closeThen { vm.setIconSwipeAction(app, target) }
            },
            onDismiss = { dialog = null },
        )
        SheetDialog.ICON_APP -> AppPickerDialog(
            title = "Icône de ${app.label}",
            apps = apps.visible.filter { it.key != app.key },
            onPick = { source ->
                dialog = null
                if (source != null) closeThen { vm.setIconFromApp(app, source) }
            },
            onDismiss = { dialog = null },
        )
        SheetDialog.GLYPH -> {
            val user = vm.userGlyphChoice(app)
            val options = listOf(GLYPH_AUTO, GlyphChoice.Own.spec) +
                PaperLogo.entries.map { GlyphChoice.Logo(it).spec } + listOf(GLYPH_TEXT, GLYPH_APP)
            ChoiceDialog(
                title = "Motif de ${app.label}",
                options = options,
                selected = when (user) {
                    null -> GLYPH_AUTO
                    is GlyphChoice.Text -> GLYPH_TEXT
                    is GlyphChoice.OfApp -> GLYPH_APP
                    else -> user.spec
                },
                label = { option ->
                    when (option) {
                        GLYPH_AUTO -> "Automatique"
                        GlyphChoice.Own.spec -> "Silhouette de l'app"
                        GLYPH_TEXT -> "Lettres…" + ((user as? GlyphChoice.Text)?.let { " (${it.text})" } ?: "")
                        GLYPH_APP -> "Silhouette d'une autre app…" +
                            ((user as? GlyphChoice.OfApp)?.let { " (${apps.byKey[it.key]?.label ?: "?"})" } ?: "")
                        else -> (GlyphChoice.parse(option) as? GlyphChoice.Logo)?.logo?.label ?: option
                    }
                },
                onSelect = { option ->
                    when (option) {
                        GLYPH_TEXT -> dialog = SheetDialog.GLYPH_TEXT
                        GLYPH_APP -> dialog = SheetDialog.GLYPH_APP
                        else -> {
                            dialog = null
                            closeThen { vm.setGlyphChoice(app, if (option == GLYPH_AUTO) null else GlyphChoice.parse(option)) }
                        }
                    }
                },
                onDismiss = { dialog = null },
                footer = "Automatique : le motif de la place de l'app sur l'accueil (logo YouTube, +x, πr), sinon sa silhouette.",
            )
        }
        SheetDialog.GLYPH_TEXT -> {
            val current = (vm.userGlyphChoice(app) as? GlyphChoice.Text)?.text ?: app.label.take(2)
            TextInputDialog(
                title = "Lettres du motif",
                initial = current,
                placeholder = "+x, πr…",
                onConfirm = { text ->
                    dialog = null
                    val clean = text.trim().take(4)
                    closeThen { vm.setGlyphChoice(app, if (clean.isEmpty()) null else GlyphChoice.Text(clean)) }
                },
                onDismiss = { dialog = null },
            )
        }
        SheetDialog.GLYPH_APP -> AppPickerDialog(
            title = "Silhouette pour ${app.label}",
            apps = apps.visible.filter { it.key != app.key },
            onPick = { source ->
                dialog = null
                if (source != null) closeThen { vm.setGlyphChoice(app, GlyphChoice.OfApp(source.key)) }
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

/** En-tête papier de la feuille : pastille de l'app, nom en gras, catégorie en italique, filet. */
@Composable
internal fun PaperSheetHeader(app: AppEntry, detail: String) {
    Column(Modifier.padding(horizontal = PaperMargin)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)) {
            AppGlyphDisc(app, 46.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(app.label, style = PaperStyle.serif(21f, 26f, bold = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = PaperStyle.note.copy(fontSize = PaperStyle.body.fontSize))
            }
        }
        PaperRule()
    }
}

@Composable
private fun SectionTitle(text: String) {
    if (LocalPaperUi.current) {
        Text(
            text,
            style = PaperStyle.smallCaps(13.5f, PaperColors.Soft, spacing = 0.08f),
            modifier = Modifier.padding(start = PaperMargin, end = PaperMargin, top = 14.dp, bottom = 2.dp),
        )
        return
    }
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SheetRow(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    if (LocalPaperUi.current) {
        // Papier : une ligne de texte, sans icône.
        Text(
            label,
            style = PaperStyle.item,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .inkClickable(onClick = onClick)
                .padding(horizontal = PaperMargin, vertical = 12.dp),
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                leading != null -> leading()
                icon != null -> Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
