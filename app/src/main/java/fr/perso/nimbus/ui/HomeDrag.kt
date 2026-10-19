package fr.perso.nimbus.ui

import android.graphics.Picture
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.HomeCell
import fr.perso.nimbus.data.HomeWidget
import kotlin.math.floor
import kotlin.math.roundToInt

/** Ce qui est glissé sur l'accueil. */
sealed interface DragItem {
    /** App du tiroir ([from] null) ou icône de l'accueil (sa case de départ). */
    data class App(val app: AppEntry, val from: HomeCell?) : DragItem

    /** Widget de l'accueil, avec son image (null si la capture a échoué) et la taille de sa case (px). */
    class Widget(val widget: HomeWidget, val picture: Picture?, val size: IntSize) : DragItem
}

/** Zone de la grille visée par un glissé, en cases. */
data class DropArea(val x: Int, val y: Int, val spanX: Int, val spanY: Int)

/**
 * Glisser-déposer sur l'accueil : icône de l'accueil, widget, ou app du tiroir (le tiroir
 * devient transparent pendant le glissé). L'élément suit le doigt dans un calque au-dessus de
 * tout ([HomeDragLayer]) : il reste visible quand les pages défilent. L'accueil signale la zone
 * visée, change de page quand le doigt est tenu contre un bord, et pose l'élément au lâcher
 * ([onDrop]). Positions en coordonnées de la fenêtre (racine).
 */
@Stable
class HomeDragState {
    var item by mutableStateOf<DragItem?>(null)
        private set

    /** Position du doigt (racine). */
    var position by mutableStateOf(Offset.Zero)

    /** Position du doigt dans l'élément au départ : le widget ne « saute » pas sous le doigt. */
    var grab by mutableStateOf(Offset.Zero)
        private set

    /** Pages de l'accueil (racine) et grille d'une page, déclarées par l'accueil. */
    var pagerBounds by mutableStateOf<Rect?>(null)
    var gridBounds by mutableStateOf<Rect?>(null)
    var gridColumns by mutableIntStateOf(1)
    var gridRows by mutableIntStateOf(1)

    /** Appelé au lâcher : zone visée de la page affichée, ou null (lâché hors de la grille). */
    var onDrop: (DragItem, DropArea?) -> Unit = { _, _ -> }

    /** Le tiroir reste invisible après un lâcher, le temps qu'il se ferme (pas de réapparition). */
    var drawerHidden by mutableStateOf(false)

    fun start(item: DragItem, at: Offset, grab: Offset = Offset.Zero) {
        this.item = item
        this.grab = grab
        position = at
        if (item is DragItem.App && item.from == null) drawerHidden = true
    }

    /** Bord des pages sous le doigt : -1 à gauche, 1 à droite, 0 ailleurs (ou hors glissé). */
    fun edge(zonePx: Float): Int {
        val bounds = pagerBounds ?: return 0
        if (item == null || position.y !in bounds.top..bounds.bottom) return 0
        return when {
            position.x < bounds.left + zonePx -> -1
            position.x > bounds.right - zonePx -> 1
            else -> 0
        }
    }

    /**
     * Zone visée : la case sous le doigt pour une icône (null hors de la grille) ; pour un widget,
     * la case la plus proche de son coin haut gauche, ramenée dans la grille.
     */
    fun target(): DropArea? {
        val bounds = gridBounds ?: return null
        val cellW = bounds.width / gridColumns
        val cellH = bounds.height / gridRows
        return when (val dragged = item) {
            null -> null
            is DragItem.App -> {
                if (!bounds.contains(position)) return null
                val x = floor((position.x - bounds.left) / cellW).toInt().coerceIn(0, gridColumns - 1)
                val y = floor((position.y - bounds.top) / cellH).toInt().coerceIn(0, gridRows - 1)
                DropArea(x, y, 1, 1)
            }
            is DragItem.Widget -> {
                val w = dragged.widget
                val spanX = w.spanX.coerceIn(1, gridColumns)
                val spanY = w.spanY.coerceIn(1, gridRows)
                val x = ((position.x - grab.x - bounds.left) / cellW).roundToInt().coerceIn(0, gridColumns - spanX)
                val y = ((position.y - grab.y - bounds.top) / cellH).roundToInt().coerceIn(0, gridRows - spanY)
                DropArea(x, y, spanX, spanY)
            }
        }
    }

    fun drop() {
        val dragged = item ?: return
        val area = target()
        item = null
        onDrop(dragged, area)
    }

    fun cancel() {
        item = null
        drawerHidden = false
    }
}

val LocalHomeDrag = staticCompositionLocalOf { HomeDragState() }

/**
 * Image d'une vue (widget) pour le calque de glissé. Enregistrée dans un [Picture], rejoué sur le
 * canevas matériel de Compose : les images matérielles des widgets passent aussi.
 */
fun View.capturePicture(): Picture? = try {
    if (width <= 0 || height <= 0) {
        null
    } else {
        Picture().also { picture ->
            draw(picture.beginRecording(width, height))
            picture.endRecording()
        }
    }
} catch (e: Exception) {
    null
}

/** L'élément glissé, sous le doigt, au-dessus de tout. */
@Composable
fun HomeDragLayer(state: HomeDragState, iconSizeDp: Int) {
    when (val item = state.item) {
        null -> Unit
        is DragItem.App -> {
            val size = (iconSizeDp * 1.15f).dp
            val half = with(LocalDensity.current) { size.toPx() / 2f }
            Box(
                Modifier
                    .offset { IntOffset((state.position.x - half).roundToInt(), (state.position.y - half).roundToInt()) }
                    .size(size)
                    .graphicsLayer { alpha = 0.95f },
            ) {
                Image(item.app.icon, contentDescription = null, modifier = Modifier.size(size))
            }
        }
        is DragItem.Widget -> {
            val density = LocalDensity.current
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .offset {
                        IntOffset((state.position.x - state.grab.x).roundToInt(), (state.position.y - state.grab.y).roundToInt())
                    }
                    .size(with(density) { item.size.width.toDp() }, with(density) { item.size.height.toDp() })
                    // Même marge que dans la grille : l'image se superpose exactement au widget.
                    .padding(4.dp)
                    .graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.04f
                        alpha = 0.92f
                        this.shape = shape
                        clip = true
                        shadowElevation = 12.dp.toPx()
                    },
            ) {
                val picture = item.picture
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (picture == null) Modifier.background(Color.White.copy(alpha = 0.3f))
                            else Modifier.drawBehind { drawIntoCanvas { it.nativeCanvas.drawPicture(picture) } },
                        ),
                )
            }
        }
    }
}
