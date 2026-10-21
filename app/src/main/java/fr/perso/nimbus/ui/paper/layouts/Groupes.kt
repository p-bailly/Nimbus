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
import androidx.compose.ui.text.TextStyle
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
// sagittal ; puis l'Écran forme D₃, les Jeux ℤ/3ℤ et les Outils ℤ/3ℤ × ℤ/3ℤ, et trois exercices
// se calculent avec les logos.

private val Blue = PaperFamily.ECHANGES.color
private val Green = PaperFamily.TRAJETS.color
private val Pink = PaperFamily.ECRAN.color
private val Amber = PaperFamily.JEUX.color
private val Violet = PaperFamily.OUTILS.color

/** Abscisses des trois fibres de φ, ordonnées de ℤ/6ℤ (haut) et de ℤ/3ℤ (bas). */
private val FibreX = floatArrayOf(86f, 206f, 326f)
private const val TOP_Y = 258f
private const val BOTTOM_Y = 364f

/** Échanges dans l'ordre des classes 0 à 5 de ℤ/6ℤ. */
private val Echanges = listOf(
    PaperSlot.WHATSAPP, PaperSlot.MESSAGES, PaperSlot.DISCORD,
    PaperSlot.REDDIT, PaperSlot.TELEPHONE, PaperSlot.GMAIL,
)

/** Rangée du haut, par fibre : φ⁻¹(0) = {0, 3}, φ⁻¹(1) = {1, 4}, φ⁻¹(2) = {2, 5}. */
private val FibreOrder = intArrayOf(0, 3, 1, 4, 2, 5)

@Composable
fun GroupesPage() {
    PaperPage("Groupes", "1") {
        Morphism()
        Proposition()
        Dihedral()
        Cyclic()
        Product()
        Exercises()
    }
}

// ---------------------------------------------------------------- 1. φ : ℤ/6ℤ → ℤ/3ℤ

/** Abscisse de la pastille de rang [i] de la rangée du haut (classe k = FibreOrder[i]). */
private fun topX(i: Int): Float = FibreX[FibreOrder[i] % 3] + if (i % 2 == 1) 22f else -22f

@Composable
private fun Morphism() {
    Row(Modifier.at(22, 142)) {
        Text("1", style = heading(Paper.Ink))
        Text(
            tex {
                color(Blue) { +"Échanges" }
                +" → "
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
            val y1 = TOP_Y + 22f
            val x2 = FibreX[t] + if (i % 2 == 1) 6f else -6f
            val y2 = BOTTOM_Y - 22f
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
        val left = FibreX[0] - 22f
        val right = FibreX[0] + 22f
        val r = 23f
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
    Text("φ", style = Paper.text(17f, 18f).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center), modifier = Modifier.at(366, 302).width(20.dp))

    val number = Paper.text(13f, 16f).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
    for (i in FibreOrder.indices) {
        val k = FibreOrder[i]
        val x = topX(i)
        Text("$k", style = number.copy(color = Blue), modifier = Modifier.at(x - 8f, TOP_Y - 44f).width(16.dp))
        SlotDisc(Echanges[k], x, TOP_Y, 36f, glyphScale = 17f / 36f)
    }
    listOf(PaperSlot.WAZE, PaperSlot.MAPS, PaperSlot.METEO).forEachIndexed { t, slot ->
        Text("$t", style = number.copy(color = Green), modifier = Modifier.at(FibreX[t] + 24f, BOTTOM_Y - 8f).width(16.dp))
        SlotDisc(slot, FibreX[t], BOTTOM_Y, 36f, glyphScale = 17f / 36f)
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
        Modifier.at(22, 418).width(368.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.height(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Proposition.", style = line.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 6.dp))
            Text("ker", style = line)
            Text("φ", style = line.copy(fontStyle = FontStyle.Italic))
            Text("= {0, 3} = {", style = line)
            SlotDisc(PaperSlot.WHATSAPP, 10f, 10f, 20f, glyphScale = 11f / 20f)
            Text(",", style = line)
            SlotDisc(PaperSlot.REDDIT, 10f, 10f, 20f, glyphScale = 11f / 20f)
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

/** Pastille de 34 unités des sections 2 à 4 (cerclée : motif de la couleur sur fond blanc). */
@Composable
private fun Element(slot: PaperSlot, cx: Float, cy: Float, color: Color, ring: Boolean = false) {
    SlotDisc(slot, cx, cy, 34f, style = if (ring) DiscStyle.RING else DiscStyle.FILL, color = color, glyphScale = 16f / 34f)
}

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
    Heading("2  Écran", "≅ D", Pink, 22f, 488f, index = "3")
    val line = Paper.text(14.5f, 20f, Pink)
    Row(Modifier.at(22, 514)) {
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
    val sign = Paper.text(16f, 24f, Pink)
    Text("= {", style = sign, modifier = Modifier.at(44, 548))
    val d3 = listOf(PaperSlot.YOUTUBE, PaperSlot.TWITCH, PaperSlot.STREMIO, PaperSlot.SPOTIFY, PaperSlot.CAMERA, PaperSlot.PHOTOS)
    d3.forEachIndexed { m, slot ->
        Element(slot, 92f + 42f * m, 560f, Pink, ring = m >= 3)
        if (m < d3.lastIndex) Text(",", style = Paper.text(16f, 16f, Pink), modifier = Modifier.at(110f + 42f * m, 564f))
    }
    Text("}", style = sign, modifier = Modifier.at(322, 548))
}

/** Jeux & perso ≅ ℤ/3ℤ. */
@Composable
private fun Cyclic() {
    Heading("3  Jeux & perso", "≅ ℤ/3ℤ", Amber, 22f, 596f)
    listOf(PaperSlot.LICHESS, PaperSlot.BASKET, PaperSlot.PIR).forEachIndexed { i, slot ->
        val cx = 39f + 44f * i
        Element(slot, cx, 652f, Amber)
        Tag(i, cx, 678f, Amber)
    }
}

/** Outils ≅ ℤ/3ℤ × ℤ/3ℤ : (i, j) en colonne i, ligne j. */
@Composable
private fun Product() {
    // 208 et non 204 comme la maquette : à 204, le « 4 » touche le « ℤ/3ℤ » du titre des Jeux ; 208
    // laisse une espace et finit encore dans la marge (390).
    Heading("4  Outils", "≅ ℤ/3ℤ × ℤ/3ℤ", Violet, 208f, 596f)
    val gx = floatArrayOf(262f, 302f, 342f)
    val gy = floatArrayOf(652f, 692f, 732f)
    val tools = listOf(
        PaperSlot.CLAUDE, PaperSlot.GITHUB, PaperSlot.DRIVE,
        PaperSlot.FICHIERS, PaperSlot.AUTHENTICATOR, PaperSlot.BANQUE,
        PaperSlot.PLAY, PaperSlot.AMAZON, PaperSlot.PRONOTE,
    )
    tools.forEachIndexed { n, slot -> Element(slot, gx[n % 3], gy[n / 3], Violet) }
    for (v in 0..2) {
        Tag(v, gx[v], 623f, Violet, 16f)
        Tag(v, 230f, gy[v], Violet, 16f)
    }
}

// ---------------------------------------------------------------- 5. Exercices

@Composable
private fun Exercises() {
    Text("5  Exercices", style = heading(Paper.Ink), modifier = Modifier.at(22, 762))
    Column(Modifier.at(22, 784), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // s · r = sr. L'indice de « D₃ » rehausse cette ligne : la fin s'y cale en haut.
        Exercise("a)", Modifier.height(30.18.dp)) {
            SlotDisc(PaperSlot.SPOTIFY, 12f, 12f, 24f, style = DiscStyle.RING, glyphScale = 13f / 24f)
            Text("·", style = Paper.text(16f, 24f, Pink))
            SlotDisc(PaperSlot.TWITCH, 12f, 12f, 24f, glyphScale = 13f / 24f)
            Row(Modifier.align(Alignment.Top)) {
                Text("dans D", style = Paper.text(16f, 24f), modifier = Modifier.alignByBaseline())
                Subscript("3", 10f, 16f, Paper.Ink)
                Text(".", style = Paper.text(16f, 24f), modifier = Modifier.alignByBaseline())
            }
        }
        // 1 + 2 = 0.
        Exercise("b)") {
            SlotDisc(PaperSlot.BASKET, 12f, 12f, 24f, glyphScale = 13f / 24f)
            Text("+", style = Paper.text(16f, 24f, Amber))
            SlotDisc(PaperSlot.PIR, 12f, 12f, 24f, glyphScale = 13f / 24f)
            Text("dans ℤ/3ℤ.", style = Paper.text(16f, 24f))
        }
        // (2, 1) + (0, 2) = (2, 0).
        Exercise("c)") {
            SlotDisc(PaperSlot.BANQUE, 12f, 12f, 24f, glyphScale = 13f / 24f)
            Text("+", style = Paper.text(16f, 24f, Violet))
            SlotDisc(PaperSlot.PLAY, 12f, 12f, 24f, glyphScale = 13f / 24f)
            Text("dans ℤ/3ℤ × ℤ/3ℤ.", style = Paper.text(16f, 24f))
        }
    }
}

/** Ligne d'exercice : « a) Calculer », puis l'opération écrite avec deux pastilles de 24 unités. */
@Composable
private fun Exercise(label: String, modifier: Modifier = Modifier.height(24.dp), content: @Composable RowScope.() -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = Paper.text(14f, 24f).copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(end = 2.dp))
        Text("Calculer", style = Paper.text(16f, 24f))
        content()
    }
}
