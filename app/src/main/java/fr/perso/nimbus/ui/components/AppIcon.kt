package fr.perso.nimbus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.ui.DragItem
import fr.perso.nimbus.ui.LocalHomeDrag

/** Ombre portée des textes posés directement sur le fond d'écran. */
val WallpaperTextShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), blurRadius = 8f)

private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Distance de glissé vers le haut qui déclenche l'action de glissé d'une icône. */
private val SWIPE_DISTANCE = 32.dp

/**
 * Icône d'app avec son nom et une pastille de notifications.
 * [onWallpaper] : texte blanc ombré, pour l'accueil (sinon couleurs du thème).
 * [onSwipeUp] : action de glissé vers le haut sur l'icône (comme Nova), null si aucune.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppEntry,
    iconSizeDp: Int,
    showLabel: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    onLongClick: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    onWallpaper: Boolean = false,
    badgeDot: Boolean = false,
    onSwipeUp: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .onGloballyPositioned { coords = it }
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClickLabel = "Ouvrir ${app.label}",
                onLongClickLabel = "Actions pour ${app.label}",
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(coords?.boundsInWindow() ?: Rect.Zero)
                },
            )
            .then(if (onSwipeUp != null) Modifier.swipeUp(onSwipeUp) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        AppIconContent(app, iconSizeDp, showLabel, badgeCount, badgeDot, onWallpaper)
    }
}

/**
 * Icône du tiroir. Appui : ouvrir ; appui long sans bouger : actions (au relâcher) ;
 * appui long puis glisser : le tiroir s'efface et l'app se pose sur l'accueil, à la case
 * où on la lâche. [canDrag] false (espace privé) : pas de glisser-déposer.
 */
@Composable
fun DrawerAppIcon(
    app: AppEntry,
    iconSizeDp: Int,
    showLabel: Boolean,
    badgeCount: Int,
    badgeDot: Boolean,
    canDrag: Boolean,
    onClick: () -> Unit,
    onLongClick: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val homeDrag = LocalHomeDrag.current
    val current by rememberUpdatedState(app)
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .onGloballyPositioned { coords = it }
            .semantics {
                onClick(label = "Ouvrir ${app.label}") { click(); true }
                onLongClick(label = "Actions pour ${app.label}") { longClick(coords?.boundsInWindow() ?: Rect.Zero); true }
            }
            // Ordre important : le glisser (dernier) voit les événements avant l'appui simple.
            .pointerInput(Unit) { detectTapGestures(onTap = { click() }) }
            .pointerInput(canDrag) {
                val slop = viewConfiguration.touchSlop
                var total = Offset.Zero
                var dragging = false
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        total = Offset.Zero
                        dragging = false
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        val c = coords
                        if (!dragging && canDrag && c != null && total.getDistance() > slop) {
                            dragging = true
                            homeDrag.start(DragItem.App(current, from = null), c.localToRoot(change.position))
                        }
                        if (dragging && c != null) homeDrag.position = c.localToRoot(change.position)
                    },
                    onDragEnd = {
                        if (dragging) homeDrag.drop() else longClick(coords?.boundsInWindow() ?: Rect.Zero)
                        dragging = false
                    },
                    onDragCancel = {
                        if (dragging) homeDrag.cancel()
                        dragging = false
                    },
                )
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        AppIconContent(app, iconSizeDp, showLabel, badgeCount, badgeDot, onWallpaper = false)
    }
}

/**
 * Détecte un glissé vers le haut. Un glissé vers le bas n'est jamais réclamé (ni consommé) :
 * il reste disponible pour le geste de page (ouvrir les notifications…).
 */
fun Modifier.swipeUp(onSwipeUp: () -> Unit): Modifier = composed {
    val action by rememberUpdatedState(onSwipeUp)
    pointerInput(Unit) {
        val threshold = SWIPE_DISTANCE.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var total = 0f
            // Sans consommation dans le rappel, la détection continue (puis s'annule si un autre la prend).
            val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                if (overSlop < 0f) {
                    change.consume()
                    total = overSlop
                }
            } ?: return@awaitEachGesture
            val completed = verticalDrag(start.id) { change ->
                total += change.positionChange().y
                change.consume()
            }
            if (completed && total < -threshold) action()
        }
    }
}

/** Visuel de l'icône (image, pastille, nom), sans gestion des touchers. */
@Composable
fun AppIconContent(
    app: AppEntry,
    iconSizeDp: Int,
    showLabel: Boolean,
    badgeCount: Int,
    badgeDot: Boolean,
    onWallpaper: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                // App d'un profil pro en pause : grisée.
                colorFilter = if (app.isPaused) Grayscale else null,
                modifier = Modifier
                    .size(iconSizeDp.dp)
                    .alpha(if (app.isPaused) 0.6f else 1f),
            )
            if (badgeCount > 0) {
                if (badgeDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                } else {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                    ) {
                        Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                    }
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = if (onWallpaper) {
                    TextStyle(color = Color.White, fontSize = 12.sp, shadow = WallpaperTextShadow)
                } else {
                    TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                },
            )
        }
    }
}
