package fr.perso.nimbus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import fr.perso.nimbus.R
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel
import kotlin.math.roundToInt

/**
 * Petit pop-up de l'appui long sur une app, posé au-dessus de son icône (en dessous s'il
 * manque de place) : raccourcis de l'app, puis Infos, Accueil, Désinstaller et « Plus »
 * (la feuille complète : dock, glissé, nom, icône, catégorie, masquer…).
 */
@Composable
fun AppPopup(app: AppEntry, anchor: Rect, apps: AppsState, vm: LauncherViewModel) {
    val shortcuts = rememberAppShortcuts(app)
    val density = LocalDensity.current
    val positionProvider = remember(anchor) {
        AnchoredPopupPosition(
            anchor = IntRect(anchor.left.roundToInt(), anchor.top.roundToInt(), anchor.right.roundToInt(), anchor.bottom.roundToInt()),
            gap = with(density) { 6.dp.roundToPx() },
            margin = with(density) { 12.dp.roundToPx() },
        )
    }
    val isPrivate = app.profile == ProfileType.PRIVATE
    val onHome = app.key in apps.homeKeys

    fun then(action: () -> Unit) {
        vm.dismissAppPopup()
        action()
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = vm::dismissAppPopup,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(min = 232.dp, max = 300.dp),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                for (item in shortcuts.take(4)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { then { vm.startShortcut(item.info) } }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                            if (item.icon != null) Image(item.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                            else Icon(painterResource(R.drawable.ms_app_shortcut), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(item.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    PopupAction(R.drawable.ms_info, "Infos") { then { vm.appInfo(app) } }
                    if (!isPrivate) {
                        if (onHome) PopupAction(R.drawable.ms_close, "Retirer") { then { vm.removeFromHome(app) } }
                        else PopupAction(R.drawable.ms_add_to_home_screen, "Accueil") { then { vm.addToHome(app) } }
                    }
                    PopupAction(R.drawable.ms_delete, "Désinstaller") { then { vm.uninstall(app) } }
                    PopupAction(R.drawable.ms_more_horiz, "Plus") { then { vm.showAppSheet(app) } }
                }
            }
        }
    }
}

@Composable
private fun PopupAction(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** Au-dessus de l'icône, centré sur elle, sans sortir de l'écran ; en dessous s'il manque de place. */
private class AnchoredPopupPosition(
    private val anchor: IntRect,
    private val gap: Int,
    private val margin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = (anchor.center.x - popupContentSize.width / 2).coerceIn(margin, maxX)
        val above = anchor.top - gap - popupContentSize.height
        val y = if (above >= margin) above
        else (anchor.bottom + gap).coerceAtMost((windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        return IntOffset(x, y)
    }
}
