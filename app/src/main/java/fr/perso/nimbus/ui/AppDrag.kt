package fr.perso.nimbus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.AppEntry
import kotlin.math.roundToInt

/**
 * Glisser-déposer d'une app du tiroir vers la grille de l'accueil. Le tiroir devient
 * transparent pendant le glissé ; l'icône suit le doigt dans un calque au-dessus de tout,
 * et la grille signale la case visée. Positions en coordonnées de la fenêtre (racine).
 */
@Stable
class AppDragState {
    /** App en cours de glissé, null sinon. */
    var app by mutableStateOf<AppEntry?>(null)
        private set

    /** Position du doigt (racine). */
    var position by mutableStateOf(Offset.Zero)

    /** Grille de l'accueil, déclarée par elle-même (racine). */
    var gridBounds by mutableStateOf<Rect?>(null)
    var gridColumns by mutableIntStateOf(1)
    var gridRows by mutableIntStateOf(1)

    /** Appelé au lâcher : case de la grille sous le doigt, ou null (lâché ailleurs sur l'accueil). */
    var onDrop: (AppEntry, Pair<Int, Int>?) -> Unit = { _, _ -> }

    /** Le tiroir reste invisible après un lâcher, le temps qu'il se ferme (pas de réapparition). */
    var drawerHidden by mutableStateOf(false)

    fun start(app: AppEntry, at: Offset) {
        this.app = app
        position = at
        drawerHidden = true
    }

    /** Case de la grille sous le doigt, ou null hors de la grille. */
    fun hoveredCell(): Pair<Int, Int>? {
        val bounds = gridBounds ?: return null
        if (app == null || !bounds.contains(position)) return null
        val x = ((position.x - bounds.left) / (bounds.width / gridColumns)).toInt().coerceIn(0, gridColumns - 1)
        val y = ((position.y - bounds.top) / (bounds.height / gridRows)).toInt().coerceIn(0, gridRows - 1)
        return x to y
    }

    fun drop() {
        val dragged = app ?: return
        onDrop(dragged, hoveredCell())
        app = null
    }

    fun cancel() {
        app = null
        drawerHidden = false
    }
}

val LocalAppDrag = staticCompositionLocalOf { AppDragState() }

/** Icône qui suit le doigt pendant un glissé depuis le tiroir. */
@Composable
fun AppDragLayer(state: AppDragState, iconSizeDp: Int) {
    val app = state.app ?: return
    val half = with(LocalDensity.current) { (iconSizeDp.dp * 1.15f).toPx() / 2f }
    Box(
        Modifier
            .offset { IntOffset((state.position.x - half).roundToInt(), (state.position.y - half).roundToInt()) }
            .size((iconSizeDp * 1.15f).dp)
            .graphicsLayer { alpha = 0.95f },
    ) {
        Image(app.icon, contentDescription = null, modifier = Modifier.size((iconSizeDp * 1.15f).dp))
    }
}
