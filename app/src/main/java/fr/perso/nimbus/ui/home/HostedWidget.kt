package fr.perso.nimbus.ui.home

import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Affiche le widget [id]. Sa taille est transmise au widget une fois stabilisée
 * (pas à chaque image d'un redimensionnement). [onView] reçoit la vue créée (capture d'image).
 */
@Composable
fun HostedWidget(id: Int, host: WidgetHostManager, modifier: Modifier = Modifier, onView: (View) -> Unit = {}) {
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
            factory = { ctx -> (host.createView(ctx, id) ?: FrameLayout(ctx)).also(onView) },
            modifier = modifier.onSizeChanged { viewSize = it },
        )
    }
}

/**
 * Appui long puis glisser, sans voler les touchers : rien n'est consommé avant l'appui long
 * (un widget, vue Android, ne reçoit les événements qu'en dernier et seulement s'ils n'ont pas
 * été consommés avant ; le tap d'une icône reste à son propre détecteur). Ensuite :
 * - lâché sans bouger -> [onLongPress] (position locale du doigt) ;
 * - glissé -> [onDragStart] (position locale du doigt au départ, et dans la fenêtre), [onDrag]
 *   (position du doigt dans la fenêtre, juste même quand la page défile sous le doigt), puis
 *   [onDragEnd] au lâcher ou [onDragCancel].
 * Après l'appui long tout est consommé : un widget reçoit une annulation, pas un clic.
 */
fun Modifier.longPressDrag(
    onLongPress: (Offset) -> Unit,
    onDragStart: (local: Offset, root: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
): Modifier = composed {
    val longPress by rememberUpdatedState(onLongPress)
    val start by rememberUpdatedState(onDragStart)
    val move by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onDragEnd)
    val cancel by rememberUpdatedState(onDragCancel)
    val haptics = LocalHapticFeedback.current
    // Coordonnées de l'élément, pour situer le doigt dans la fenêtre.
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    onGloballyPositioned { coords[0] = it }.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            // Attente de l'appui long. Le détecteur standard ne s'annule pas quand le doigt
            // bouge (seulement s'il est consommé, ce que le widget fait après nous) : on le fait
            // ici, sinon faire défiler lentement un widget liste ouvrirait le menu.
            val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed || change.isConsumed ||
                        (change.position - down.position).getDistance() > slop
                    ) break
                }
                true
            }
            if (cancelled != null) return@awaitEachGesture
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            var dragging = false
            fun root(local: Offset): Offset? = coords[0]?.takeIf { it.isAttached }?.localToRoot(local)
            val released = try {
                drag(down.id) { change ->
                    change.consume()
                    if (!dragging && (change.position - down.position).getDistance() > slop) {
                        val at = root(down.position) ?: return@drag
                        dragging = true
                        start(down.position, at)
                    }
                    if (dragging) root(change.position)?.let(move)
                }
            } catch (e: CancellationException) {
                // Geste interrompu en plein glissé : l'élément revient à sa place.
                if (dragging) cancel()
                throw e
            }
            currentEvent.changes.forEach { it.consume() }
            when {
                !released -> if (dragging) cancel()
                dragging -> end()
                else -> longPress(down.position)
            }
        }
    }
}
