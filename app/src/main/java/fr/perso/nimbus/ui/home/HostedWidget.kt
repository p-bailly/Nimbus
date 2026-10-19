package fr.perso.nimbus.ui.home

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Affiche le widget [id]. Sa taille est transmise au widget une fois stabilisée
 * (pas à chaque image d'un redimensionnement).
 */
@Composable
fun HostedWidget(id: Int, host: WidgetHostManager, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val info = remember(id) { host.info(id) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(viewSize) {
        if (viewSize == IntSize.Zero) return@LaunchedEffect
        delay(250)
        with(density) {
            host.updateSize(id, viewSize.width.toDp().value.roundToInt(), viewSize.height.toDp().value.roundToInt())
        }
    }
    if (info == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)),
        ) {
            Text(
                "Widget indisponible (app désinstallée ?)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        AndroidView(
            factory = { ctx -> host.createView(ctx, id) ?: FrameLayout(ctx) },
            modifier = modifier.onSizeChanged { viewSize = it },
        )
    }
}

/**
 * Appui long sur un widget sans lui voler ses touchers : un widget (vue Android) ne reçoit
 * les événements qu'en dernier, et seulement s'ils n'ont pas été consommés avant. Rien n'est
 * donc consommé tant que l'appui long n'a pas eu lieu ; ensuite tout l'est, et le widget reçoit
 * une annulation (pas de clic parasite).
 */
fun Modifier.widgetLongPress(onLongPress: () -> Unit): Modifier = composed {
    val callback by rememberUpdatedState(onLongPress)
    val haptics = LocalHapticFeedback.current
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            callback()
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) break
            }
        }
    }
}
