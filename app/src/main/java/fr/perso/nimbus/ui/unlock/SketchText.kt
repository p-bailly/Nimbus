package fr.perso.nimbus.ui.unlock

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.toSize
import kotlin.math.round

// Le texte de la météo posé sur le coloriage : mesuré une fois placé, pour que Météo dessine autour de lui
// (le soleil, la lune et les nuages à côté ; le ciel passe sous les lettres, sans papier blanc autour).

/**
 * Le texte posé sur le coloriage : chaque écriture le dit ([of], et [layout] pour garder ses lettres
 * plutôt que toute sa boîte), le dessin aussi ([frame]). [zones] dit, en pixels du dessin, ce que le
 * soleil et les nuages ne doivent pas toucher ; elle ne change pas quand la page bouge tout entière.
 */
@Stable
class SketchText {
    private val layouts = HashMap<String, TextLayoutResult>()
    private val nodes = HashMap<String, LayoutCoordinates>()
    private var drawing: LayoutCoordinates? = null

    /** Ce que couvre le texte, en pixels du dessin. */
    var zones by mutableStateOf(emptyList<Rect>())
        private set

    /** À poser sur le dessin. */
    val frame: Modifier = Modifier.onGloballyPositioned {
        drawing = it
        update()
    }

    /** À poser sur l'écriture [key]. */
    fun of(key: String): Modifier = Modifier.onGloballyPositioned {
        nodes[key] = it
        update()
    }

    /** À donner au `onTextLayout` du texte [key] : on ne garde que ses lettres. */
    fun layout(key: String): (TextLayoutResult) -> Unit = { layouts[key] = it }

    private fun update() {
        val f = drawing?.takeIf { it.isAttached } ?: return
        // Une écriture qui a disparu (le bandeau d'erreur) ne compte plus.
        nodes.entries.removeAll { !it.value.isAttached }
        val next = nodes.flatMap { (key, node) ->
            val origin = f.localPositionOf(node, Offset.Zero)
            val parts = layouts[key]?.let(::letters) ?: listOf(Rect(Offset.Zero, node.size.toSize()))
            parts.map { r -> Rect(round(r.left + origin.x), round(r.top + origin.y), round(r.right + origin.x), round(r.bottom + origin.y)) }
        }
        if (next != zones) zones = next
    }
}

/**
 * Ce que couvrent les lettres de [layout], ligne par ligne : de la hauteur des majuscules (des accents
 * s'il y en a) au bas des jambages (à la ligne de base s'il n'y en a pas).
 */
internal fun letters(layout: TextLayoutResult): List<Rect> {
    val size = layout.layoutInput.style.fontSize
    // Sans taille connue, celle qu'on lit dans la hauteur de la première ligne.
    val em = if (size.isSp) with(layout.layoutInput.density) { size.toPx() } else (layout.getLineBottom(0) - layout.getLineTop(0)) / 1.25f
    val text = layout.layoutInput.text.text
    return (0 until layout.lineCount).mapNotNull { i ->
        val line = text.substring(layout.getLineStart(i), layout.getLineEnd(i, visibleEnd = true))
        if (line.isBlank()) return@mapNotNull null
        val baseline = layout.getLineBaseline(i)
        val high = line.any { it.isUpperCase() && it.code > 0x7F }
        val low = line.any { it in "gjpqyçQ,;()[]{}|/@" }
        Rect(layout.getLineLeft(i), baseline - em * if (high) 0.96f else 0.8f, layout.getLineRight(i), baseline + em * if (low) 0.25f else 0.06f)
    }
}

/** Marge que Météo laisse entre le texte et ce qu'il écarte (soleil, nuages), en unités de la page. */
internal const val TEXT_MARGIN = 5f
