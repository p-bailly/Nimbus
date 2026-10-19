package fr.perso.nimbus.ui.home

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.perso.nimbus.R
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.HomeWidget
import fr.perso.nimbus.ui.DragItem
import fr.perso.nimbus.ui.LocalHomeDrag
import fr.perso.nimbus.ui.PlacedIcon
import fr.perso.nimbus.ui.capturePicture
import fr.perso.nimbus.ui.components.AppIconContent
import fr.perso.nimbus.ui.components.swipeUp
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Grille d'une page de l'accueil, à placement libre (comme Nova Launcher ou Smart Launcher).
 * Icônes — appui : ouvrir ; appui long sans bouger : actions ; appui long puis glisser :
 * déplacer, y compris vers une autre page (l'icône de la case d'arrivée prend la place
 * libérée) ; glisser vers le haut : action de glissé.
 * Widgets — appui long : menu (redimensionner, supprimer) ; appui long puis glisser : déplacer.
 * Pendant un glissé, l'élément suit le doigt dans le calque de [LocalHomeDrag] ; [isTarget] :
 * page affichée, qui signale la zone visée.
 */
@Composable
fun HomeGrid(
    page: Int,
    isTarget: Boolean,
    icons: List<PlacedIcon>,
    widgets: List<HomeWidget>,
    widgetHost: WidgetHostManager,
    resizingWidget: Int?,
    onResizeWidget: (Int?) -> Unit,
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
    onActions: (AppEntry, Rect) -> Unit,
    onSwipeUp: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drag = LocalHomeDrag.current
    BoxWithConstraints(modifier) {
        val cellW = maxWidth / columns
        val cellH = maxHeight / rows
        val cellWPx = with(LocalDensity.current) { cellW.toPx() }
        val cellHPx = with(LocalDensity.current) { cellH.toPx() }
        // L'icône rétrécit si les cases sont plus petites qu'elle (grille dense).
        val labelSpace = if (showLabels) 24 else 8
        val effectiveIcon = min(iconSizeDp.toFloat(), min(cellW.value - 8f, cellH.value - labelSpace)).toInt().coerceAtLeast(24)

        for (widget in widgets) {
            key("widget:${widget.id}") {
                HomeWidgetCell(
                    widget = widget,
                    host = widgetHost,
                    resizing = resizingWidget == widget.id,
                    cellW = cellW,
                    cellH = cellH,
                    cellWPx = cellWPx,
                    cellHPx = cellHPx,
                    columns = columns,
                    rows = rows,
                    onUpdate = onUpdateWidget,
                    onResize = { onResizeWidget(widget.id) },
                    onRemove = { onRemoveWidget(widget.id) },
                )
            }
        }

        // Zone visée par un glissé (icône, widget ou app du tiroir), sur la page affichée.
        if (isTarget) DragTarget(cellW, cellH)

        for (icon in icons) {
            key(icon.app.key) {
                val current by rememberUpdatedState(icon)
                val dragged = (drag.item as? DragItem.App)?.let { it.from != null && it.app.key == icon.app.key } == true
                var iconCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(cellW * icon.x, cellH * icon.y)
                        .size(cellW, cellH)
                        .onGloballyPositioned { iconCoords = it }
                        // Ordre important : le déplacement (dernier) voit les événements en premier.
                        .pointerInput(Unit) { detectTapGestures(onTap = { onOpen(current.app) }) }
                        .then(if (hasSwipeAction(icon.app)) Modifier.swipeUp { onSwipeUp(current.app) } else Modifier)
                        .longPressDrag(
                            onLongPress = { onActions(current.app, iconCoords?.boundsInWindow() ?: Rect.Zero) },
                            onDragStart = { _, at -> drag.start(DragItem.App(current.app, current.cell), at) },
                            onDrag = { drag.position = it },
                            onDragEnd = drag::drop,
                            onDragCancel = drag::cancel,
                        )
                        // L'icône glissée est dessinée par le calque : sa case reste vide.
                        .graphicsLayer { alpha = if (dragged) 0f else 1f },
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

/** Lue dans son propre composable : suivre le doigt ne recompose pas toute la grille. */
@Composable
private fun DragTarget(cellW: Dp, cellH: Dp) {
    LocalHomeDrag.current.target()?.let { DropTarget(it.x, it.y, it.spanX, it.spanY, cellW, cellH) }
}

/** Zone visée par un déplacement : fond translucide, sans contour. */
@Composable
private fun DropTarget(x: Int, y: Int, spanX: Int, spanY: Int, cellW: Dp, cellH: Dp) {
    Box(
        Modifier
            .offset(cellW * x, cellH * y)
            .size(cellW * spanX, cellH * spanY)
            .zIndex(2.5f)
            .padding(4.dp)
            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
    )
}

/**
 * Un widget de la grille. Appui long : menu ; appui long puis glisser : déplacement (une image
 * du widget suit le doigt, éventuellement vers une autre page ; le widget lui-même reste en
 * place, invisible, jusqu'au lâcher). En mode redimensionnement, une poignée apparaît dans le coin.
 */
@Composable
private fun HomeWidgetCell(
    widget: HomeWidget,
    host: WidgetHostManager,
    resizing: Boolean,
    cellW: Dp,
    cellH: Dp,
    cellWPx: Float,
    cellHPx: Float,
    columns: Int,
    rows: Int,
    onUpdate: (HomeWidget) -> Unit,
    onResize: () -> Unit,
    onRemove: () -> Unit,
) {
    val current by rememberUpdatedState(widget)
    val update by rememberUpdatedState(onUpdate)
    val drag = LocalHomeDrag.current
    val dragged = (drag.item as? DragItem.Widget)?.widget?.id == widget.id
    // Taille visée pendant un redimensionnement (null sinon).
    var preview by remember { mutableStateOf<HomeWidget?>(null) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    val hostView = remember { arrayOfNulls<View>(1) }
    var cellSize by remember { mutableStateOf(IntSize.Zero) }
    val shape = RoundedCornerShape(16.dp)

    Box(
        Modifier
            .offset(cellW * widget.x, cellH * widget.y)
            .size(cellW * widget.spanX, cellH * widget.spanY)
            .zIndex(if (resizing) 2f else 0f)
            .onGloballyPositioned { cellSize = it.size }
            .longPressDrag(
                onLongPress = { menuAt = it },
                onDragStart = { local, at ->
                    drag.start(DragItem.Widget(current, hostView[0]?.capturePicture(), cellSize), at, grab = local)
                },
                onDrag = { drag.position = it },
                onDragEnd = drag::drop,
                onDragCancel = drag::cancel,
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .graphicsLayer { alpha = if (dragged) 0f else 1f }
                .clip(shape),
        ) {
            HostedWidget(id = widget.id, host = host, modifier = Modifier.fillMaxSize(), onView = { hostView[0] = it })
        }

        if (resizing) {
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

        menuAt?.let { pos ->
            Box(
                Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .size(1.dp),
            ) {
                WidgetMenu(
                    onDismiss = { menuAt = null },
                    onResize = {
                        menuAt = null
                        onResize()
                    },
                    onRemove = {
                        menuAt = null
                        onRemove()
                    },
                )
            }
        }
    }

    preview?.let { p -> DropTarget(p.x, p.y, p.spanX, p.spanY, cellW, cellH) }
}

/** Menu d'un widget (appui long). */
@Composable
private fun WidgetMenu(
    onDismiss: () -> Unit,
    onResize: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        DropdownMenuItem(
            text = { Text("Redimensionner") },
            leadingIcon = { Icon(painterResource(R.drawable.ms_open_in_full), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onResize,
        )
        DropdownMenuItem(
            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(painterResource(R.drawable.ms_delete), contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = onRemove,
        )
    }
}
