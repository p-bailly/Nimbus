package fr.perso.nimbus.ui.components

import android.content.pm.ShortcutInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.service.SystemActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Raccourci d'app (appui long), avec son icône déjà dessinée. */
class ShortcutItem(val info: ShortcutInfo, val label: String, val icon: ImageBitmap?)

/** Raccourcis de l'app, chargés hors du thread principal (accessibles au seul launcher par défaut). */
@Composable
fun rememberAppShortcuts(app: AppEntry): List<ShortcutItem> {
    val context = LocalContext.current
    val iconPx = with(LocalDensity.current) { 24.dp.roundToPx() }
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
    return shortcuts
}
