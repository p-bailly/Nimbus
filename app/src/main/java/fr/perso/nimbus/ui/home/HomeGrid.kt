package fr.perso.nimbus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.HomeWidget
import fr.perso.nimbus.ui.LocalAppDrag
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
 * Widgets — appui long : menu (redimensionner, supprimer) ; appui long puis glisser : déplacer.
 * Une app glissée depuis le tiroir se lâche sur la case signalée.
 */
@Composable
fun HomeGrid(
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
    onActions: (AppEntry) -> Unit,
    onMove: (AppEntry, Int, Int) -> Unit,
    onSwipeUp: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val appDrag = LocalAppDrag.current
    // La grille se déclare au glisser-déposer depuis le tiroir (position et nombre de cases).
    SideEffect {
        appDrag.gridColumns = columns
        appDrag.gridRows = rows
    }
    BoxWithConstraints(modifier.onGloballyPositioned { appDrag.gridBounds = it.boundsInRoot() }) {
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

        // Case visée par une app glissée depuis le tiroir.
        DrawerDropTarget(cellW, cellH)

        target?.let { (x, y) -> DropTarget(x, y, 1, 1, cellW, cellH) }

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

/** Lue dans son propre composable : suivre le doigt ne recompose pas toute la grille. */
@Composable
private fun DrawerDropTarget(cellW: Dp, cellH: Dp) {
    LocalAppDrag.current.hoveredCell()?.let { (x, y) -> DropTarget(x, y, 1, 1, cellW, cellH) }
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
 * Un widget de la grille. Appui long : menu ; appui long puis glisser : déplacement (le
 * widget suit le doigt, la zone d'arrivée est signalée). En mode redimensionnement, seule
 * une poignée apparaît dans le coin.
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
    // Position / taille visée pendant un glissé (null hors glissé).
    var preview by remember { mutableStateOf<HomeWidget?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    val shape = RoundedCornerShape(16.dp)

    fun movedBy(total: Offset): HomeWidget {
        val w = current
        return w.copy(
            x = (w.x + (total.x / cellWPx).roundToInt()).coerceIn(0, columns - w.spanX),
            y = (w.y + (total.y / cellHPx).roundToInt()).coerceIn(0, rows - w.spanY),
        )
    }

    Box(
        Modifier
            .offset(cellW * widget.x, cellH * widget.y)
            .size(cellW * widget.spanX, cellH * widget.spanY)
            .padding(4.dp)
            .zIndex(if (dragOffset != Offset.Zero || resizing) 2f else 0f)
            // Les gestes sont sur la boîte fixe ; seul le contenu se déplace avec le doigt.
            .widgetGestures(
                onMenu = { menuAt = it },
                onDrag = { total ->
                    dragOffset = total
                    preview = movedBy(total)
                },
                onDragEnd = { total ->
                    val target = movedBy(total)
                    dragOffset = Offset.Zero
                    preview = null
                    if (target != current) update(target)
                },
                onDragCancel = {
                    dragOffset = Offset.Zero
                    preview = null
                },
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                    if (dragOffset != Offset.Zero) {
                        scaleX = 1.03f
                        scaleY = 1.03f
                        alpha = 0.9f
                    }
                }
                .clip(shape),
        ) {
            HostedWidget(id = widget.id, host = host, modifier = Modifier.fillMaxSize())
        }

        if (resizing) {
            // Poignée de redimensionnement, coin inférieur droit.
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
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

/** Menu d'un widget (appui long). [onToHome] : seulement pour les widgets de la page widgets. */
@Composable
fun WidgetMenu(
    onDismiss: () -> Unit,
    onResize: () -> Unit,
    onRemove: () -> Unit,
    onToHome: (() -> Unit)? = null,
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Redimensionner") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = onResize,
        )
        if (onToHome != null) {
            DropdownMenuItem(
                text = { Text("Mettre sur l'accueil") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                onClick = onToHome,
            )
        }
        DropdownMenuItem(
            text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = onRemove,
        )
    }
}
