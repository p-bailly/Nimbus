package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.PaperFamily
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.DiscStyle
import fr.perso.nimbus.ui.paper.PAPER_HEIGHT
import fr.perso.nimbus.ui.paper.PAPER_WIDTH
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.SlotDisc
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.color
import fr.perso.nimbus.ui.paper.tex
import kotlin.math.hypot

// Page « Groupes » : les Échanges (ℤ/6ℤ) se projettent sur les Trajets (ℤ/3ℤ) par φ, en diagramme
// sagittal ; puis l'Écran forme D₃, les Jeux ℤ/3ℤ et les Outils ℤ/3ℤ × ℤ/3ℤ. Les exercices qui
// finissaient la page sont dans design/exercices-accueils.md : la place a servi à agrandir les logos.

private val Blue = PaperFamily.ECHANGES.color
private val Green = PaperFamily.TRAJETS.color
private val Pink = PaperFamily.ECRAN.color
private val Amber = PaperFamily.JEUX.color
private val Violet = PaperFamily.OUTILS.color

/** Abscisses des trois fibres de φ, ordonnées de ℤ/6ℤ (haut) et de ℤ/3ℤ (bas). */
private val FibreX = floatArrayOf(86f, 206f, 326f)
private const val TOP_Y = 270f
private const val BOTTOM_Y = 392f

/** Pastilles de la section 1, et demi-écart des deux pastilles d'une fibre. */
private const val MORPH_DISC = 48f
private const val PAIR = 27f

/** Échanges dans l'ordre des classes 0 à 5 de ℤ/6ℤ. */
private val Echanges = listOf(
    PaperSlot.WHATSAPP, PaperSlot.MESSAGES, PaperSlot.DISCORD,
    PaperSlot.REDDIT, PaperSlot.TELEPHONE, PaperSlot.GMAIL,
)

/** Rangée du haut, par fibre : φ⁻¹(0) = {0, 3}, φ⁻¹(1) = {1, 4}, φ⁻¹(2) = {2, 5}. */
private val FibreOrder = intArrayOf(0, 3, 1, 4, 2, 5)

@Composable
fun GroupesPage() {
    PaperPage {
        Morphism()
        Proposition()
        Dihedral()
        Cyclic()
        Product()
    }
}

// ---------------------------------------------------------------- 1. φ : ℤ/6ℤ → ℤ/3ℤ

/** Abscisse de la pastille de rang [i] de la rangée du haut (classe k = FibreOrder[i]). */
private fun topX(i: Int): Float = FibreX[FibreOrder[i] % 3] + if (i % 2 == 1) PAIR else -PAIR

@Composable
private fun Morphism() {
    Row(Modifier.at(22, 142)) {
        Text("1", style = heading(Paper.Ink))
        Text(
            // Premier théorème d'isomorphisme : φ est surjective de noyau {0, 3}, donc ℤ/6ℤ / ker φ ≅ ℤ/3ℤ.
            tex {
                color(Blue) { +"Échanges" }
                b.withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(" / ker ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("φ") }
                    append(" ≅ ")
                }
                color(Green) { +"Trajets" }
            },
            style = heading(Paper.Ink),
            modifier = Modifier.padding(start = 12.dp),
        )
    }
    Definition()

    // Flèches de chaque classe vers son image, et le noyau {0, 3} entouré de pointillés.
    Canvas(Modifier.size(PAPER_WIDTH.dp, PAPER_HEIGHT.dp)) {
        val u = 1.dp.toPx()
        val ink = Paper.Ink
        for (i in FibreOrder.indices) {
            val t = FibreOrder[i] % 3
            val x1 = topX(i)
            val y1 = TOP_Y + MORPH_DISC / 2f + 4f
            val x2 = FibreX[t] + if (i % 2 == 1) 7f else -7f
            val y2 = BOTTOM_Y - MORPH_DISC / 2f - 4f
            val length = hypot(x2 - x1, y2 - y1)
            val ux = (x2 - x1) / length
            val uy = (y2 - y1) / length
            val bx = x2 - ux * 7f
            val by = y2 - uy * 7f
            drawLine(ink, Offset(x1 * u, y1 * u), Offset(bx * u, by * u), strokeWidth = 1.1f * u, cap = StrokeCap.Round)
            val head = Path().apply {
                moveTo(x2 * u, y2 * u)
                lineTo((bx - uy * 3.2f) * u, (by + ux * 3.2f) * u)
                lineTo((bx + uy * 3.2f) * u, (by - ux * 3.2f) * u)
                close()
            }
            drawPath(head, ink)
        }
        val left = FibreX[0] - PAIR
        val right = FibreX[0] + PAIR
        val r = MORPH_DISC / 2f + 5f
        val kernel = Path().apply {
            moveTo(left * u, (TOP_Y - r) * u)
            lineTo(right * u, (TOP_Y - r) * u)
            arcTo(Rect(Offset(right * u, TOP_Y * u), r * u), -90f, 180f, false)
            lineTo(left * u, (TOP_Y + r) * u)
            arcTo(Rect(Offset(left * u, TOP_Y * u), r * u), 90f, 180f, false)
            close()
        }
        drawPath(
            kernel,
            ink.copy(alpha = 0.55f),
            style = Stroke(width = u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 3f * u))),
        )
    }
    Text("φ", style = Paper.text(17f, 18f).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center), modifier = Modifier.at(370f, (TOP_Y + BOTTOM_Y) / 2f - 9f).width(20.dp))

    val number = Paper.text(13f, 16f).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
    for (i in FibreOrder.indices) {
        val k = FibreOrder[i]
        val x = topX(i)
        Text("$k", style = number.copy(color = Blue), modifier = Modifier.at(x - 8f, TOP_Y - MORPH_DISC / 2f - 22f).width(16.dp))
        SlotDisc(Echanges[k], x, TOP_Y, MORPH_DISC, glyphScale = 17f / 36f)
    }
    listOf(PaperSlot.WAZE, PaperSlot.MAPS, PaperSlot.METEO).forEachIndexed { t, slot ->
        Text("$t", style = number.copy(color = Green), modifier = Modifier.at(FibreX[t] + MORPH_DISC / 2f + 6f, BOTTOM_Y - 8f).width(16.dp))
        SlotDisc(slot, FibreX[t], BOTTOM_Y, MORPH_DISC, glyphScale = 17f / 36f)
    }
}

/** φ : { ℤ/6ℤ ⟶ ℤ/3ℤ ; k + 6ℤ ⟼ k + 3ℤ }, l'accolade étirée sur les deux lignes. */
@Composable
private fun Definition() {
    val line = Paper.text(14f, 18f)
    Row(
        Modifier.at(22, 166).height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Définition.", style = line.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 10.dp))
        Text(tex { it("φ"); +" :" }, style = line)
        // Accolade de 38 sur une ligne de 36 : Compose ne resserre pas la ligne sous la taille, on la
        // compose donc sur 38 et on la centre sur les 36 de la rangée, comme CSS.
        Text(
            "{",
            style = Paper.text(38f, 38f),
            modifier = Modifier
                .height(36.dp)
                .wrapContentHeight(unbounded = true)
                .graphicsLayer(scaleX = 0.7f),
        )
        // Trois colonnes : départ calé à droite, flèche centrée, arrivée calée à gauche.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text("ℤ/6ℤ", style = line.copy(color = Blue))
                Text(tex { it("k"); +" + 6ℤ" }, style = line)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⟶", style = line)
                Text("⟼", style = line)
            }
            Column {
                Text("ℤ/3ℤ", style = line.copy(color = Green))
                Text(tex { it("k"); +" + 3ℤ" }, style = line)
            }
        }
    }
}

/** ker φ = {0, 3}, les deux logos du noyau, et le passage au quotient. */
@Composable
private fun Proposition() {
    val line = Paper.text(14f, 20f)
    Column(
        Modifier.at(22, 440).width(368.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.height(26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Proposition.", style = line.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 6.dp))
            Text("ker", style = line)
            Text("φ", style = line.copy(fontStyle = FontStyle.Italic))
            Text("= {0, 3} = {", style = line)
            SlotDisc(PaperSlot.WHATSAPP, 13f, 13f, 26f, glyphScale = 11f / 20f)
            Text(",", style = line)
            SlotDisc(PaperSlot.REDDIT, 13f, 13f, 26f, glyphScale = 11f / 20f)
            Text("}", style = line)
        }
        Text(
            tex {
                color(Blue) { +"ℤ/6ℤ" }
                +" ⁄ ker "
                it("φ")
                +" ≅ "
                color(Green) { +"ℤ/3ℤ" }
            },
            style = Paper.text(17f, 22f),
        )
    }
}

// ---------------------------------------------------------------- 2 à 4. Les autres familles

/** Titre de section : numéro et famille en gras, puis le groupe (et son indice), sur la même ligne de base. */
@Composable
private fun Heading(title: String, group: String, color: Color, x: Float, y: Float, index: String? = null) {
    Row(Modifier.at(x, y)) {
        Text(title, style = heading(color), modifier = Modifier.padding(end = 8.dp).alignByBaseline())
        Text(group, style = Paper.text(15f, 18f, color), modifier = Modifier.alignByBaseline())
        if (index != null) Subscript(index, 10f, 15f, color)
    }
}

/**
 * Indice posé comme `<sub>` dans Chrome : corps [size], ligne de base abaissée de [parent] / 5 + 1.
 * C'est un Text à part : en span, l'indice agrandirait la ligne, qui remonterait d'autant.
 */
@Composable
private fun RowScope.Subscript(text: String, size: Float, parent: Float, color: Color) {
    Text(
        text,
        style = Paper.text(size, size, color),
        modifier = Modifier
            .alignByBaseline()
            .graphicsLayer { translationY = (parent / 5f + 1f).dp.toPx() },
    )
}

/** Titres de section : gras 15 sur 18. */
private fun heading(color: Color): TextStyle = Paper.text(15f, 18f, color).copy(fontWeight = FontWeight.Bold)

/** Pastille de 48 unités des sections 2 à 4 (cerclée : motif de la couleur sur fond blanc). */
@Composable
private fun Element(slot: PaperSlot, cx: Float, cy: Float, color: Color, ring: Boolean = false) {
    SlotDisc(slot, cx, cy, ELEMENT, style = if (ring) DiscStyle.RING else DiscStyle.FILL, color = color, glyphScale = 16f / 34f)
}

private const val ELEMENT = 48f

/** Petit numéro en italique centré en ([cx], [cy]). */
@Composable
private fun Tag(value: Int, cx: Float, cy: Float, color: Color, width: Float = 40f) {
    Text(
        "$value",
        style = Paper.text(11f, 13f, color).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
        modifier = Modifier.at(cx - width / 2f, cy - 6.5f).width(width.dp),
    )
}

/** Écran ≅ D₃ = {e, r, r², s, sr, sr²} : rotations en pastilles pleines, symétries cerclées. */
@Composable
private fun Dihedral() {
    Heading("2  Écran", "≅ D", Pink, 22f, 516f, index = "3")
    val line = Paper.text(14.5f, 20f, Pink)
    Row(Modifier.at(22, 542)) {
        Text("D", style = line, modifier = Modifier.alignByBaseline())
        Subscript("3", 9.5f, 14.5f, Pink)
        Text(
            tex {
                +" = {"
                it("e"); +", "
                it("r"); +", "
                it("r"); +"², "
                it("s"); +", "
                it("sr"); +", "
                it("sr"); +"²}"
            },
            style = line,
            modifier = Modifier.alignByBaseline(),
        )
    }
    // Six pastilles de 48, espacées de 52 : la virgule tient dans l'écart, l'accolade avant la marge.
    val cy = 600f
    val sign = Paper.text(16f, 24f, Pink)
    Text("= {", style = sign, modifier = Modifier.at(22f, cy - 12f))
    val d3 = listOf(PaperSlot.YOUTUBE, PaperSlot.TWITCH, PaperSlot.STREMIO, PaperSlot.SPOTIFY, PaperSlot.CAMERA, PaperSlot.PHOTOS)
    d3.forEachIndexed { m, slot ->
        Element(slot, 76f + 52f * m, cy, Pink, ring = m >= 3)
        if (m < d3.lastIndex) Text(",", style = Paper.text(16f, 16f, Pink), modifier = Modifier.at(100f + 52f * m, cy + 8f))
    }
    Text("}", style = sign, modifier = Modifier.at(364f, cy - 12f))
}

/** Titres des sections 3 et 4, et rangées des pastilles (les Jeux sur la première). */
private const val SECTION_Y = 646f
private val GridY = floatArrayOf(SECTION_Y + 66f, SECTION_Y + 122f, SECTION_Y + 178f)

/** Jeux & perso ≅ ℤ/3ℤ. */
@Composable
private fun Cyclic() {
    Heading("3  Jeux & perso", "≅ ℤ/3ℤ", Amber, 22f, SECTION_Y)
    listOf(PaperSlot.LICHESS, PaperSlot.BASKET, PaperSlot.PIR).forEachIndexed { i, slot ->
        val cx = 46f + 56f * i
        Element(slot, cx, GridY[0], Amber)
        Tag(i, cx, GridY[0] + ELEMENT / 2f + 10f, Amber)
    }
}

/** Outils ≅ ℤ/3ℤ × ℤ/3ℤ : (i, j) en colonne i, ligne j. */
@Composable
private fun Product() {
    // 208 et non 204 comme la maquette : à 204, le « 4 » touche le « ℤ/3ℤ » du titre des Jeux ; 208
    // laisse une espace et finit encore dans la marge (390).
    Heading("4  Outils", "≅ ℤ/3ℤ × ℤ/3ℤ", Violet, 208f, SECTION_Y)
    // Colonnes calées sur la marge de droite (366 + 24 = 390), espacées de 56.
    val gx = floatArrayOf(254f, 310f, 366f)
    val gy = GridY
    val tools = listOf(
        PaperSlot.CLAUDE, PaperSlot.GITHUB, PaperSlot.DRIVE,
        PaperSlot.FICHIERS, PaperSlot.AUTHENTICATOR, PaperSlot.BANQUE,
        PaperSlot.PLAY, PaperSlot.AMAZON, PaperSlot.PRONOTE,
    )
    tools.forEachIndexed { n, slot -> Element(slot, gx[n % 3], gy[n / 3], Violet) }
    for (v in 0..2) {
        Tag(v, gx[v], SECTION_Y + 30f, Violet, 16f)
        Tag(v, 220f, gy[v], Violet, 16f)
    }
}
