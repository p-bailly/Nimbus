package fr.perso.nimbus.ui.home

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.WidgetSlot
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.WallpaperTextShadow
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val MIN_HEIGHT_DP = 60f
private const val MAX_HEIGHT_DP = 600f
private const val RESIZE_STEP_DP = 40

/** Page des widgets : ajout, redimensionnement et suppression (mode édition). */
@Composable
fun WidgetsPage(
    vm: LauncherViewModel,
    editing: Boolean,
    onLongPress: (Offset) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val host = remember { WidgetHostManager.get(context) }
    val slots by vm.widgets.collectAsStateWithLifecycle()
    val longPress by rememberUpdatedState(onLongPress)

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    longPress(it)
                })
            }
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
            if (slots.isNotEmpty()) {
                FilledTonalButton(onClick = { vm.setEditingWidgets(!editing) }) {
                    Text(if (editing) "Terminer" else "Modifier")
                }
                Spacer(Modifier.width(8.dp))
            }
            FilledTonalIconButton(onClick = vm::openWidgetPicker) {
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
                    "Aucun widget pour l'instant",
                    style = TextStyle(color = Color.White, fontSize = 18.sp, shadow = WallpaperTextShadow),
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = vm::openWidgetPicker) { Text("Ajouter un widget") }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 40.dp),
            ) {
                for (slot in slots) {
                    key(slot.id) {
                        WidgetItem(
                            slot = slot,
                            host = host,
                            editing = editing,
                            onResize = { vm.resizeWidget(slot.id, it) },
                            onRemove = { vm.removeWidget(slot.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetItem(
    slot: WidgetSlot,
    host: WidgetHostManager,
    editing: Boolean,
    onResize: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(20.dp)
    // Décalage pendant le glissé de la poignée ; enregistré une seule fois au relâchement.
    var dragDp by remember { mutableFloatStateOf(0f) }
    val heightDp = (slot.heightDp + dragDp).coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    val info = remember(slot.id) { host.info(slot.id) }
    val resize by rememberUpdatedState(onResize)

    // Taille transmise au widget une fois le redimensionnement stabilisé (pas à chaque image du glissé).
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(viewSize) {
        if (viewSize == IntSize.Zero) return@LaunchedEffect
        delay(250)
        with(density) {
            host.updateSize(slot.id, viewSize.width.toDp().value.roundToInt(), viewSize.height.toDp().value.roundToInt())
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(shape),
    ) {
        if (info == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)),
            ) {
                Text("Widget indisponible (app désinstallée ?)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            AndroidView(
                factory = { ctx -> host.createView(ctx, slot.id) ?: FrameLayout(ctx) },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewSize = it },
            )
        }

        if (editing) {
            // Calque qui intercepte les touchers : le widget n'est plus utilisable pendant l'édition.
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    .pointerInput(Unit) { detectTapGestures { } },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                FilledTonalIconButton(onClick = { onResize(-RESIZE_STEP_DP) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Réduire")
                }
                FilledTonalIconButton(onClick = { onResize(RESIZE_STEP_DP) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Agrandir")
                }
                FilledIconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer le widget")
                }
            }
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
                                val delta = dragDp.roundToInt()
                                dragDp = 0f
                                resize(delta)
                            },
                            onDragCancel = { dragDp = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                dragDp += dragAmount.toDp().value
                            },
                        )
                    },
            ) {
                Box(
                    Modifier
                        .size(width = 48.dp, height = 5.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape),
                )
            }
        }
    }
}
