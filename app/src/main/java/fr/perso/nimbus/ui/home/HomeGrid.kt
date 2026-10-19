package fr.perso.nimbus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.HomeWidget
import fr.perso.nimbus.ui.PlacedIcon
import fr.perso.nimbus.ui.components.AppIconContent
import fr.perso.nimbus.ui.components.swipeUp
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Grille de l'accueil, à placement libre (comme Nova Launcher ou Smart Launcher).
 * Icônes — appui : ouvrir ; appui long sans bouger : actions ; appui long puis glisser :
 * déplacer (l'icône de la case d'arrivée prend la place libérée) ; glisser vers le haut :
 * action de glissé.
 * Widgets — appui long : mode édition (glisser pour déplacer, poignée pour redimensionner).
 */
@Composable
fun HomeGrid(
    icons: List<PlacedIcon>,
    widgets: List<HomeWidget>,
    widgetHost: WidgetHostManager,
    editingWidget: Int?,
    onEditWidget: (Int?) -> Unit,
    onUpdateWidget: (HomeWidget) -> Unit,
    onRemoveWidget: (Int) -> Unit,
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

        for (widget in widgets) {
            key("widget:${widget.id}") {
                HomeWidgetCell(
                    widget = widget,
                    host = widgetHost,
                    editing = editingWidget == widget.id,
                    cellW = cellW,
                    cellH = cellH,
                    cellWPx = cellWPx,
                    cellHPx = cellHPx,
                    columns = columns,
                    rows = rows,
                    onLongPress = { onEditWidget(widget.id) },
                    onUpdate = onUpdateWidget,
                )
            }
        }

        // Barre d'édition du widget sélectionné, toujours visible quelle que soit sa taille :
        // en haut si le widget est dans la moitié basse (pour ne pas cacher sa poignée).
        val edited = widgets.firstOrNull { it.id == editingWidget }
        if (edited != null) {
            val atTop = edited.y + edited.spanY / 2f >= rows / 2f
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .zIndex(3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    "Glisser pour déplacer · coin pour redimensionner",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                FilledIconButton(
                    onClick = { onRemoveWidget(edited.id) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Icon(Icons.Default.Delete, contentDescription = "Supprimer le widget") }
                FilledTonalIconButton(onClick = { onEditWidget(null) }) {
                    Icon(Icons.Default.Check, contentDescription = "Terminé")
                }
            }
        }

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
                        }
                        // Après les gestes : les déplacements mesurés ne doivent pas subir l'agrandissement
                        // ni la translation de l'icône (sinon elle prendrait du retard sur le doigt).
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.12f
                                scaleY = 1.12f
                            }
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

/** Un widget de la grille ; en édition, un calque permet de le déplacer et de le redimensionner. */
@Composable
private fun HomeWidgetCell(
    widget: HomeWidget,
    host: WidgetHostManager,
    editing: Boolean,
    cellW: Dp,
    cellH: Dp,
    cellWPx: Float,
    cellHPx: Float,
    columns: Int,
    rows: Int,
    onLongPress: () -> Unit,
    onUpdate: (HomeWidget) -> Unit,
) {
    val current by rememberUpdatedState(widget)
    val update by rememberUpdatedState(onUpdate)
    // Aperçu de la position / taille visée pendant un glissé (null hors glissé).
    var preview by remember { mutableStateOf<HomeWidget?>(null) }
    val shape = RoundedCornerShape(16.dp)

    Box(
        Modifier
            .offset(cellW * widget.x, cellH * widget.y)
            .size(cellW * widget.spanX, cellH * widget.spanY)
            .padding(4.dp)
            .zIndex(if (editing) 2f else 0f)
            .clip(shape)
            .then(if (editing) Modifier else Modifier.widgetLongPress(onLongPress)),
    ) {
        HostedWidget(id = widget.id, host = host, modifier = Modifier.fillMaxSize())
        if (editing) {
            // Le calque capte les touchers : glisser n'importe où déplace le widget.
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    .pointerInput(cellWPx, cellHPx, columns, rows) {
                        var offset = Offset.Zero
                        detectDragGestures(
                            onDragStart = { offset = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                offset += amount
                                val w = current
                                preview = w.copy(
                                    x = (w.x + (offset.x / cellWPx).roundToInt()).coerceIn(0, columns - w.spanX),
                                    y = (w.y + (offset.y / cellHPx).roundToInt()).coerceIn(0, rows - w.spanY),
                                )
                            },
                            onDragEnd = {
                                preview?.let { if (it != current) update(it) }
                                preview = null
                            },
                            onDragCancel = { preview = null },
                        )
                    },
            )
            // Poignée de redimensionnement, coin inférieur droit.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(cellWPx, cellHPx, columns, rows) {
                        var offset = Offset.Zero
                        detectDragGestures(
                            onDragStart = { offset = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                offset += amount
                                val w = current
                                preview = w.copy(
                                    spanX = (w.spanX + (offset.x / cellWPx).roundToInt()).coerceIn(1, columns - w.x),
                                    spanY = (w.spanY + (offset.y / cellHPx).roundToInt()).coerceIn(1, rows - w.y),
                                )
                            },
                            onDragEnd = {
                                preview?.let { if (it != current) update(it) }
                                preview = null
                            },
                            onDragCancel = { preview = null },
                        )
                    },
            )
        }
    }

    preview?.let { p ->
        Box(
            Modifier
                .offset(cellW * p.x, cellH * p.y)
                .size(cellW * p.spanX, cellH * p.spanY)
                .zIndex(2.5f)
                .padding(4.dp)
                .border(2.dp, Color.White.copy(alpha = 0.9f), shape),
        )
    }
}
