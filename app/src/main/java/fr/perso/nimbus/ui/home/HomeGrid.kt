package fr.perso.nimbus.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.ui.PlacedIcon
import fr.perso.nimbus.ui.components.AppIconContent
import fr.perso.nimbus.ui.components.swipeUp
import kotlin.math.floor
import kotlin.math.min

/**
 * Grille d'icônes de l'accueil, à placement libre (comme Nova Launcher ou Smart Launcher).
 * Appui : ouvrir. Appui long sans bouger : actions. Appui long puis glisser : déplacer
 * (l'icône de la case d'arrivée prend la place libérée). Glisser vers le haut : action de glissé.
 */
@Composable
fun HomeGrid(
    icons: List<PlacedIcon>,
    columns: Int,
    rows: Int,
    iconSizeDp: Int,
    showLabels: Boolean,
    badges: Map<String, Int>,
    badgeDot: Boolean,
    hasSwipeAction: (AppEntry) -> Boolean,
    onOpen: (AppEntry) -> Unit,
    onActions: (AppEntry) -> Unit,
    onMove: (AppEntry, Int, Int) -> Unit,
    onSwipeUp: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(modifier) {
        val cellW = maxWidth / columns
        val cellH = maxHeight / rows
        val cellWPx = with(LocalDensity.current) { cellW.toPx() }
        val cellHPx = with(LocalDensity.current) { cellH.toPx() }
        // L'icône rétrécit si les cases sont plus petites qu'elle (grille dense).
        val labelSpace = if (showLabels) 24 else 8
        val effectiveIcon = min(iconSizeDp.toFloat(), min(cellW.value - 8f, cellH.value - labelSpace)).toInt().coerceAtLeast(24)

        var dragging by remember { mutableStateOf<String?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var target by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        target?.let { (x, y) ->
            Box(
                Modifier
                    .offset(cellW * x, cellH * y)
                    .size(cellW, cellH)
                    .padding(4.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
            )
        }

        for (icon in icons) {
            key(icon.app.key) {
                val current by rememberUpdatedState(icon)
                val isDragging = dragging == icon.app.key

                fun cellUnderFinger(): Pair<Int, Int> {
                    val cx = (current.x + 0.5f) * cellWPx + dragOffset.x
                    val cy = (current.y + 0.5f) * cellHPx + dragOffset.y
                    return floor(cx / cellWPx).toInt().coerceIn(0, columns - 1) to
                        floor(cy / cellHPx).toInt().coerceIn(0, rows - 1)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(cellW * icon.x, cellH * icon.y)
                        .size(cellW, cellH)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.12f
                                scaleY = 1.12f
                            }
                        }
                        // Ordre important : le déplacement (dernier) voit les événements en premier.
                        .pointerInput(Unit) { detectTapGestures(onTap = { onOpen(current.app) }) }
                        .then(if (hasSwipeAction(icon.app)) Modifier.swipeUp { onSwipeUp(current.app) } else Modifier)
                        .pointerInput(cellWPx, cellHPx, columns, rows) {
                            val slop = viewConfiguration.touchSlop
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragging = current.app.key
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                    target = if (dragOffset.getDistance() > slop) cellUnderFinger() else null
                                },
                                onDragEnd = {
                                    if (dragOffset.getDistance() <= slop) {
                                        onActions(current.app)
                                    } else {
                                        val (x, y) = cellUnderFinger()
                                        if (x != current.x || y != current.y) onMove(current.app, x, y)
                                    }
                                    dragging = null
                                    target = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    dragging = null
                                    target = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        },
                ) {
                    AppIconContent(
                        app = icon.app,
                        iconSizeDp = effectiveIcon,
                        showLabel = showLabels,
                        badgeCount = badges[icon.app.packageName] ?: 0,
                        badgeDot = badgeDot,
                        onWallpaper = true,
                    )
                }
            }
        }
    }
}
