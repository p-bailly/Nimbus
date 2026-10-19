package fr.perso.nimbus.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.WidgetSlot
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.WidgetTarget
import fr.perso.nimbus.ui.components.WallpaperTextShadow
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlin.math.roundToInt

private const val MIN_HEIGHT_DP = 60f
private const val MAX_HEIGHT_DP = 600f
private const val RESIZE_STEP_DP = 40

/**
 * Page des widgets : ajout, redimensionnement et suppression (mode édition).
 * Appui long sur un widget : mode édition ; sur une zone vide : menu de l'accueil.
 */
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

    Box(Modifier.fillMaxSize()) {
        // Calque de fond : seuls les touchers des zones vides l'atteignent (les widgets et
        // boutons, posés au-dessus, les gardent pour eux).
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
                if (slots.isNotEmpty()) {
                    FilledTonalButton(onClick = { vm.setEditingWidgets(!editing) }) {
                        Text(if (editing) "Terminer" else "Modifier")
                    }
                    Spacer(Modifier.width(8.dp))
                }
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
                        "Les widgets peuvent aussi aller sur l'accueil : appui long sur l'accueil › Ajouter un widget",
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
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 40.dp),
                ) {
                    for (slot in slots) {
                        key(slot.id) {
                            WidgetItem(
                                slot = slot,
                                host = host,
                                editing = editing,
                                onEdit = { vm.setEditingWidgets(true) },
                                onResize = { vm.resizeWidget(slot.id, it) },
                                onRemove = { vm.removeWidget(slot.id) },
                            )
                        }
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
    onEdit: () -> Unit,
    onResize: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    // Décalage pendant le glissé de la poignée ; enregistré une seule fois au relâchement.
    var dragDp by remember { mutableFloatStateOf(0f) }
    val heightDp = (slot.heightDp + dragDp).coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    val resize by rememberUpdatedState(onResize)

    Box(
        Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(shape)
            .then(if (editing) Modifier else Modifier.widgetLongPress(onEdit)),
    ) {
        HostedWidget(id = slot.id, host = host, modifier = Modifier.fillMaxSize())

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
