package fr.perso.nimbus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.WidgetSlot
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.WidgetTarget
import fr.perso.nimbus.ui.components.WallpaperTextShadow
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val MIN_HEIGHT_DP = 60f
private const val MAX_HEIGHT_DP = 600f

/**
 * Page des widgets (à droite de l'accueil), pour les widgets en plus de ceux de l'accueil.
 * Appui long sur un widget : menu (redimensionner, mettre sur l'accueil, supprimer) ;
 * appui long puis glisser : changer sa place dans la liste. Appui long sur une zone vide :
 * menu de l'accueil.
 */
@Composable
fun WidgetsPage(
    vm: LauncherViewModel,
    resizingWidget: Int?,
    onLongPress: (Offset) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val host = remember { WidgetHostManager.get(context) }
    val slots by vm.widgets.collectAsStateWithLifecycle()
    val longPress by rememberUpdatedState(onLongPress)

    // Position (haut, hauteur) de chaque widget, pour savoir où il atterrit quand on le glisse.
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    var dragging by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // Widget tenu contre le bord gauche : il va passer sur l'accueil.
    var edgeHover by remember { mutableStateOf(false) }

    fun reorder(id: Int, dy: Float) {
        val (top, height) = bounds[id] ?: return
        val center = top + height / 2f + dy
        val index = slots.filter { it.id != id }.count { other ->
            bounds[other.id]?.let { (t, h) -> t + h / 2f < center } ?: false
        }
        vm.moveWidgetToIndex(id, index)
    }

    Box(Modifier.fillMaxSize()) {
        // Calque de fond : seuls les touchers des zones vides l'atteignent.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        longPress(it)
                    })
                },
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            ) {
                Text(
                    "Widgets",
                    style = TextStyle(color = Color.White, fontSize = 26.sp, shadow = WallpaperTextShadow),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = { vm.openWidgetPicker(WidgetTarget.PAGE) }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un widget")
                }
            }

            if (slots.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                ) {
                    Text(
                        "Aucun widget sur cette page",
                        style = TextStyle(color = Color.White, fontSize = 18.sp, shadow = WallpaperTextShadow),
                    )
                    Text(
                        "Les widgets peuvent aussi aller sur l'accueil : appui long sur l'accueil › Ajouter un widget ici",
                        style = TextStyle(color = Color.White, fontSize = 13.sp, shadow = WallpaperTextShadow),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.openWidgetPicker(WidgetTarget.PAGE) }) { Text("Ajouter un widget") }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        // Un toucher n'importe où termine le redimensionnement, sans rien bloquer.
                        .then(if (resizingWidget != null) Modifier.observeTap { vm.setResizingWidget(null) } else Modifier)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 40.dp),
                ) {
                    for (slot in slots) {
                        key(slot.id) {
                            WidgetItem(
                                slot = slot,
                                host = host,
                                resizing = resizingWidget == slot.id,
                                dragOffset = if (dragging == slot.id) dragOffset else Offset.Zero,
                                onPositioned = { top, height -> bounds[slot.id] = top to height },
                                onDrag = { total ->
                                    dragging = slot.id
                                    dragOffset = total
                                },
                                onDragEnd = { total ->
                                    reorder(slot.id, total.y)
                                    dragging = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    dragging = null
                                    dragOffset = Offset.Zero
                                },
                                onEdge = { edgeHover = it == -1 },
                                onStartResize = { vm.setResizingWidget(slot.id) },
                                onResize = { vm.resizeWidget(slot.id, it) },
                                onToHome = { vm.moveWidgetToHome(slot.id) },
                                onRemove = { vm.removeWidget(slot.id) },
                            )
                        }
                    }
                }
            }
        }

        if (edgeHover) {
            EdgeIndicator(
                toRight = false,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
            )
        }
    }
}

/** Observe un toucher bref sans le consommer (les widgets dessous le reçoivent normalement). */
private fun Modifier.observeTap(onTap: () -> Unit): Modifier = composed {
    val callback by rememberUpdatedState(onTap)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (waitForUpOrCancellation() != null) callback()
        }
    }
}

@Composable
private fun WidgetItem(
    slot: WidgetSlot,
    host: WidgetHostManager,
    resizing: Boolean,
    dragOffset: Offset,
    onPositioned: (Float, Float) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onDragCancel: () -> Unit,
    onEdge: (Int) -> Unit,
    onStartResize: () -> Unit,
    onResize: (Int) -> Unit,
    onToHome: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    // Décalage pendant le glissé de la poignée ; enregistré une seule fois au relâchement.
    var resizeDp by remember { mutableFloatStateOf(0f) }
    val heightDp = (slot.heightDp + resizeDp).coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    val resize by rememberUpdatedState(onResize)
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    val haptics = LocalHapticFeedback.current
    // Tenu contre le bord gauche : passage sur l'accueil après un court maintien.
    var atLeftEdge by remember { mutableStateOf(false) }
    LaunchedEffect(atLeftEdge) {
        if (atLeftEdge) {
            delay(EDGE_HOLD_MS)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onEdge(0)
            onToHome()
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .zIndex(if (dragOffset != Offset.Zero) 1f else 0f)
            .onGloballyPositioned { coords ->
                val b = coords.boundsInRoot()
                onPositioned(b.top, b.height)
            }
            // Gestes sur la boîte fixe ; seul le contenu suit le doigt.
            .widgetGestures(
                onMenu = { menuAt = it },
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onEdge = { edge ->
                    atLeftEdge = edge == -1
                    onEdge(edge)
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
                        scaleX = 1.02f
                        scaleY = 1.02f
                        alpha = 0.9f
                    }
                }
                .clip(shape),
        ) {
            HostedWidget(id = slot.id, host = host, modifier = Modifier.fillMaxSize())
        }

        if (resizing) {
            // Poignée de redimensionnement en bas du widget.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(slot.id) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val delta = resizeDp.roundToInt()
                                resizeDp = 0f
                                resize(delta)
                            },
                            onDragCancel = { resizeDp = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                resizeDp += dragAmount.toDp().value
                            },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .size(width = 56.dp, height = 6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
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
                        onStartResize()
                    },
                    onToHome = {
                        menuAt = null
                        onToHome()
                    },
                    onRemove = {
                        menuAt = null
                        onRemove()
                    },
                )
            }
        }
    }
}
