package fr.perso.nimbus.ui.components

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import fr.perso.nimbus.ui.drawer.SearchMatcher
import fr.perso.nimbus.widgets.WidgetHostManager
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.NimbusBottomSheet
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextField
import fr.perso.nimbus.ui.theme.inkClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.math.roundToInt

private class WidgetOption(val info: AppWidgetProviderInfo, val label: String, val size: String)

private class WidgetGroup(val packageName: String, val appLabel: String, val widgets: List<WidgetOption>) {
    val searchKey = SearchMatcher.normalize(appLabel + " " + widgets.joinToString(" ") { it.label })
}

/** Liste des widgets installés, regroupés par app ; [onPick] reçoit le widget choisi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPicker(onDismiss: () -> Unit, onPick: (AppWidgetProviderInfo) -> Unit) {
    val context = LocalContext.current
    val groups by produceState<List<WidgetGroup>?>(null) {
        value = withContext(Dispatchers.IO) { loadGroups(context) }
    }
    var filter by remember { mutableStateOf("") }
    val shown = remember(groups, filter) {
        val q = SearchMatcher.normalize(filter)
        groups.orEmpty().filter { q.isEmpty() || it.searchKey.contains(q) }
    }

    val paper = LocalPaperUi.current
    NimbusBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            if (paper) {
                Text(
                    "Ajouter un widget",
                    style = PaperStyle.serif(22f, 28f, bold = true),
                    modifier = Modifier.padding(horizontal = PaperMargin),
                )
                PaperTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = "Rechercher",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PaperMargin, vertical = 12.dp),
                )
            } else {
                Text(
                    "Ajouter un widget",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    placeholder = { Text("Rechercher un widget") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            when {
                groups == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    if (paper) CircularProgressIndicator(color = PaperColors.Ink, strokeWidth = 1.5.dp)
                    else CircularProgressIndicator()
                }
                shown.isEmpty() -> Text(
                    "Aucun widget trouvé",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (paper) PaperStyle.note.copy(fontSize = PaperStyle.body.fontSize) else LocalTextStyle.current,
                    modifier = Modifier.padding(if (paper) PaperMargin else 24.dp),
                )
                else -> LazyColumn(Modifier.fillMaxWidth()) {
                    for (group in shown) {
                        item(key = "app:" + group.packageName) {
                            if (paper) {
                                Text(
                                    group.appLabel,
                                    style = PaperStyle.smallCaps(14f, spacing = 0.06f),
                                    modifier = Modifier.padding(start = PaperMargin, end = PaperMargin, top = 18.dp, bottom = 2.dp),
                                )
                            } else {
                                Text(
                                    group.appLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
                                )
                            }
                        }
                        items(group.widgets, key = { it.info.provider.flattenToString() + it.info.profile.hashCode() }) { option ->
                            WidgetRow(option, onClick = { onPick(option.info) })
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(option: WidgetOption, onClick: () -> Unit) {
    val context = LocalContext.current
    var preview by remember(option.info) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(option.info) {
        preview = withContext(Dispatchers.IO) { loadPreview(context, option.info) }
    }
    if (LocalPaperUi.current) {
        // Papier : l'aperçu encadré d'un filet, comme une figure ; nom en romain, taille en italique.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .inkClickable(onClick = onClick)
                .padding(horizontal = PaperMargin, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .border(0.6.dp, PaperColors.Rule),
            ) {
                preview?.let {
                    Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(6.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(option.label, style = PaperStyle.item, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(option.size + " cases", style = PaperStyle.note)
            }
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 96.dp, height = 72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            preview?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(6.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(option.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(option.size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun loadGroups(context: Context): List<WidgetGroup> {
    val pm = context.packageManager
    val density = context.resources.displayMetrics.density
    val collator = Collator.getInstance()
    return WidgetHostManager.get(context).providers()
        .groupBy { it.provider.packageName }
        .map { (pkg, infos) ->
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                pkg
            }
            val options = infos.map { info ->
                WidgetOption(info, info.loadLabel(pm) ?: appLabel, sizeText(info, density))
            }.sortedWith(compareBy(collator) { it.label })
            WidgetGroup(pkg, appLabel, options)
        }
        .sortedWith(compareBy(collator) { it.appLabel })
}

/** Taille en cases de la grille : Android 12+ la déclare, sinon formule historique (70 dp × n − 30). */
private fun sizeText(info: AppWidgetProviderInfo, density: Float): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
        return "${info.targetCellWidth} × ${info.targetCellHeight}"
    }
    fun cells(px: Int) = (((px / density) + 30f) / 70f).roundToInt().coerceAtLeast(1)
    return "${cells(info.minWidth)} × ${cells(info.minHeight)}"
}

private fun loadPreview(context: Context, info: AppWidgetProviderInfo): ImageBitmap? = try {
    val dpi = context.resources.displayMetrics.densityDpi
    val drawable = info.loadPreviewImage(context, dpi) ?: info.loadIcon(context, dpi)
    if (drawable == null) null else {
        // Aperçu réduit à 96 × 72 dp au plus, proportions conservées.
        val maxW = (96 * context.resources.displayMetrics.density).roundToInt()
        val maxH = (72 * context.resources.displayMetrics.density).roundToInt()
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxW
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxH
        val scale = minOf(maxW.toFloat() / w, maxH.toFloat() / h, 1f)
        drawable.toBitmap((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1)).asImageBitmap()
    }
} catch (e: Exception) {
    null
}
