package fr.perso.nimbus.ui.components

import android.content.pm.ShortcutInfo
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class ShortcutItem(val info: ShortcutInfo, val label: String, val icon: ImageBitmap?)

private enum class SheetDialog { CATEGORY, RENAME, SWIPE }

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
    val context = LocalContext.current
    val iconPx = with(LocalDensity.current) { 24.dp.roundToPx() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val overrides by vm.categoryOverrides.collectAsStateWithLifecycle()
    val customLabels by vm.customLabels.collectAsStateWithLifecycle()
    val customIcons by vm.customIconKeys.collectAsStateWithLifecycle()
    val swipeActions by vm.iconSwipeActions.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SheetDialog?>(null) }

    val isPrivate = app.profile == ProfileType.PRIVATE
    val isFavorite = app.key in apps.favoriteKeys
    val onHome = app.key in apps.homeKeys
    val isHidden = app.key in apps.hiddenKeys

    // Les raccourcis ne sont accessibles qu'au launcher par défaut.
    var shortcuts by remember(app.key) { mutableStateOf(emptyList<ShortcutItem>()) }
    LaunchedEffect(app.key) {
        shortcuts = withContext(Dispatchers.IO) {
            SystemActions.shortcuts(context, app).map { info ->
                val icon = try {
                    SystemActions.shortcutIcon(context, info)?.toBitmap(iconPx, iconPx)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
                ShortcutItem(info, (info.shortLabel ?: info.longLabel ?: "").toString(), icon)
            }
        }
    }

    // Referme la feuille avec son animation, puis exécute l'action.
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            vm.dismissAppSheet()
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = vm::dismissAppSheet, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Image(app.icon, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        buildString {
                            append(if (isPrivate) "Espace privé" else app.category.label)
                            if (app.profile == ProfileType.WORK) append(" · Profil professionnel")
                            if (app.isPaused) append(" (en pause)")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 24.dp))
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
                if (app.key in customIcons) {
                    SheetRow(label = "Icône d'origine", icon = Icons.Filled.Refresh, onClick = { closeThen { vm.resetCustomIcon(app) } })
                }
                SheetRow(
                    label = "Catégorie : ${app.category.label}",
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
        SheetDialog.CATEGORY -> ChoiceDialog(
            title = "Catégorie de ${app.label}",
            options = listOf<AppCategory?>(null) + AppCategory.entries,
            selected = overrides[app.key],
            label = { it?.label ?: "Automatique (${app.autoCategory.label})" },
            onSelect = {
                dialog = null
                closeThen { vm.setCategory(app, it) }
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
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
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
