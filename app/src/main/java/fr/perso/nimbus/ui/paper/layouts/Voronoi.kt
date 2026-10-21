package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.perso.nimbus.data.PaperFamily
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.DiscStyle
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PAPER_HEIGHT
import fr.perso.nimbus.ui.paper.PAPER_WIDTH
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.SlotDisc
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.color
import fr.perso.nimbus.ui.paper.slot
import fr.perso.nimbus.ui.paper.tex

// Page « Voronoï » : chaque appli possède sa cellule, les points de Ω plus proches de son site que
// de tout autre. Un appui n'importe où dans la cellule ouvre l'appli.

/** Site d'une appli, dans le repère de la figure (avant mise à l'échelle). */
private class Site(val slot: PaperSlot, val x: Double, val y: Double)

// Sites obtenus par relaxation de Lloyd dans Ω, écartés de moins d'un point de l'optimum symétrique
// pour que quatre sites ne soient jamais cocycliques.
private val SITES = listOf(
    Site(PaperSlot.WHATSAPP, 63.4, 205.5), Site(PaperSlot.MESSAGES, 168.5, 199.5), Site(PaperSlot.DISCORD, 137.9, 275.0),
    Site(PaperSlot.REDDIT, 57.0, 304.3), Site(PaperSlot.TELEPHONE, 159.2, 361.0), Site(PaperSlot.GMAIL, 56.9, 397.2),
    Site(PaperSlot.YOUTUBE, 267.5, 204.9), Site(PaperSlot.TWITCH, 356.1, 206.3), Site(PaperSlot.STREMIO, 234.4, 283.6),
    Site(PaperSlot.SPOTIFY, 344.6, 296.7), Site(PaperSlot.PHOTOS, 263.5, 362.5), Site(PaperSlot.CAMERA, 359.0, 392.8),
    Site(PaperSlot.WAZE, 127.3, 448.3), Site(PaperSlot.MAPS, 57.3, 499.4), Site(PaperSlot.METEO, 56.6, 591.6),
    Site(PaperSlot.CLAUDE, 157.9, 535.2), Site(PaperSlot.GITHUB, 264.3, 532.5), Site(PaperSlot.DRIVE, 233.9, 612.0),
    Site(PaperSlot.FICHIERS, 344.4, 599.8), Site(PaperSlot.AUTHENTICATOR, 136.6, 620.8), Site(PaperSlot.BANQUE, 63.3, 690.8),
    Site(PaperSlot.PLAY, 167.8, 697.8), Site(PaperSlot.AMAZON, 266.8, 691.8), Site(PaperSlot.PRONOTE, 356.1, 689.9),
    Site(PaperSlot.LICHESS, 210.7, 447.4), Site(PaperSlot.BASKET, 294.4, 447.6), Site(PaperSlot.PIR, 358.8, 504.3),
)

/** Ω = [14, 398] × [160, 736], dans le repère de la figure. */
private const val X0 = 14.0
private const val Y0 = 160.0
private const val X1 = 398.0
private const val Y1 = 736.0

/** La figure est réduite à 0,9 et décalée de (20,6 ; 54) sur la page. */
private const val SCALE = 0.9f
private const val SHIFT_X = 20.6f
private const val SHIFT_Y = 54f

/**
 * Interlignage des paragraphes : 20 dans la maquette, mais Chrome y arrondit les métriques de la
 * police et toute ligne qui contient de l'italique y fait 21 de haut ; on reprend ce rendu.
 */
private const val LINE = 21f

/** Pastille : 38 unités de diamètre plus un cerne de 1,5 à l'extérieur, motif de 18 (avant réduction). */
private const val DISC = (38f + 2 * 1.5f) * SCALE
private const val RING = 1.5f * SCALE
private const val GLYPH = 18f * SCALE

/**
 * Sommet d'une cellule. [edge] : site dont la médiatrice porte le côté qui part de ce sommet
 * (−1 pour le cadre de Ω).
 */
private class Vertex(val x: Double, val y: Double, val edge: Int)

/** Garde la partie de [poly] du côté de a de la médiatrice de [a, b] ; les côtés créés portent [j]. */
private fun clip(poly: List<Vertex>, a: Site, b: Site, j: Int): List<Vertex> {
    val nx = b.x - a.x
    val ny = b.y - a.y
    val c = (b.x * b.x + b.y * b.y - a.x * a.x - a.y * a.y) / 2
    fun f(p: Vertex) = p.x * nx + p.y * ny - c
    val out = ArrayList<Vertex>(poly.size + 1)
    poly.forEachIndexed { k, p ->
        val q = poly[(k + 1) % poly.size]
        val fp = f(p)
        val fq = f(q)
        fun cut(e: Int) {
            val t = fp / (fp - fq)
            out += Vertex(p.x + t * (q.x - p.x), p.y + t * (q.y - p.y), e)
        }
        if (fp <= 0) {
            out += p
            if (fq > 0) cut(j)
        } else if (fq <= 0) {
            cut(p.edge)
        }
    }
    return out
}

/** V(a) = Ω ∩ ⋂_b {x : ‖x − a‖ ≤ ‖x − b‖} : Ω découpé par le demi-plan de chaque médiatrice. */
private fun cell(i: Int): List<Vertex> {
    var poly = listOf(Vertex(X0, Y0, -1), Vertex(X1, Y0, -1), Vertex(X1, Y1, -1), Vertex(X0, Y1, -1))
    SITES.forEachIndexed { j, b -> if (j != i) poly = clip(poly, SITES[i], b, j) }
    return poly
}

/**
 * Tracés de la figure (repère de la figure) : un aplat par famille, les frontières intérieures à
 * une famille et celles entre familles ; [cells] : chaque cellule dans le repère de la page.
 */
private class Diagram(
    val fills: Map<PaperFamily, Path>,
    val inner: Path,
    val outer: Path,
    val cells: List<List<Offset>>,
)

private fun diagram(): Diagram {
    val fills = PaperFamily.entries.associateWith { Path() }
    val inner = Path()
    val outer = Path()
    val cells = SITES.mapIndexed { i, site ->
        val poly = cell(i)
        fills.getValue(site.slot.family).apply {
            poly.forEachIndexed { k, p -> if (k == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
            close()
        }
        // Chaque frontière une seule fois : depuis la cellule de plus petit indice.
        poly.forEachIndexed { k, p ->
            if (p.edge > i) {
                val q = poly[(k + 1) % poly.size]
                val path = if (SITES[p.edge].slot.family == site.slot.family) inner else outer
                path.moveTo(p.x.toFloat(), p.y.toFloat())
                path.lineTo(q.x.toFloat(), q.y.toFloat())
            }
        }
        poly.map { page(it.x, it.y) }
    }
    return Diagram(fills, inner, outer, cells)
}

/** Point de la figure dans le repère de la page. */
private fun page(x: Double, y: Double) = Offset(SHIFT_X + SCALE * x.toFloat(), SHIFT_Y + SCALE * y.toFloat())

@Composable
fun VoronoiPage() {
    val diagram = remember { diagram() }
    PaperPage("Voronoï", "2") {
        Text(
            tex { bf("Définition."); +" Pour "; it("a"); +" ∈ "; it("A"); +" = {applis}, la cellule de "; it("a"); +" est" },
            style = Paper.body,
            modifier = Modifier.at(22, 142).width(368.dp),
        )
        // (1) : centrée sur la largeur du texte moins la colonne du numéro, comme dans la maquette,
        // 6 sous la définition (une ligne de [LINE]).
        Box(Modifier.at(22, 169).width(340.dp).height(20.dp), contentAlignment = Alignment.Center) {
            Text(
                tex {
                    it("V"); +"("; it("a"); +") = { "; it("x"); +" ∈ Ω : ‖"; it("x"); +" − "; it("a"); +"‖ ≤ ‖"
                    it("x"); +" − "; it("b"); +"‖ pour tout "; it("b"); +" ∈ "; it("A"); +" }"
                },
                style = Paper.text(13.5f, 20f),
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(Modifier.at(22, 169).width(368.dp).height(20.dp), contentAlignment = Alignment.CenterEnd) {
            Text("(1)", style = Paper.body)
        }

        Figure(diagram)

        Text(
            tex {
                b.withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                +" – Un appui en "; it("x"); +" ∈ "; it("V"); +"("; it("a"); +") ouvre l'appli "; it("a"); +"."
            },
            style = Paper.caption,
            modifier = Modifier.at(22, 726).width(368.dp),
        )
        // Clé des familles.
        Row(
            Modifier.at(22, 747).width(368.dp).height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaperFamily.entries.forEach { family ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(9.dp).background(family.color, CircleShape))
                    Text(family.label, style = Paper.text(11f, 16f, family.color).copy(letterSpacing = 0.04.em))
                }
            }
        }

        Column(Modifier.at(22, 772).width(368.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                tex {
                    bf("Proposition."); +" "; italic { +"Pour a ≠ b," }; +" "
                    it("V"); +"("; it("a"); +") ∩ "; it("V"); +"("; it("b"); +") ⊂ "; it("médiatrice de"); +" ["; it("a"); +", "; it("b"); +"]."
                },
                style = Paper.text(14.5f, LINE),
            )
            Text(
                tex {
                    bf("Exercice."); +" On prend Ω = ℝ², "; it("A"); +" = {"; it("a"); +", "; it("b"); +"}, "
                    it("a"); +" = (0, 0) et "; it("b"); +" = (2, 0). Déterminer "; it("V"); +"("; it("a"); +")."
                },
                style = Paper.text(14.5f, LINE),
            )
        }
    }
}

/** Le diagramme, « Ω », puis les applis : chacune occupe toute sa cellule, pastille au site. */
@Composable
private fun Figure(diagram: Diagram) {
    Canvas(Modifier.size(PAPER_WIDTH.dp, PAPER_HEIGHT.dp)) {
        val u = 1.dp.toPx()
        withTransform({
            translate(SHIFT_X * u, SHIFT_Y * u)
            scale(SCALE * u, SCALE * u, Offset.Zero)
        }) {
            diagram.fills.forEach { (family, path) -> drawPath(path, family.color.copy(alpha = 0.2f)) }
            drawPath(diagram.inner, Paper.Ink.copy(alpha = 0.45f), style = Stroke(0.8f))
            drawPath(diagram.outer, Paper.Ink, style = Stroke(1.5f, cap = StrokeCap.Round))
            drawRect(
                Paper.Ink,
                topLeft = Offset(X0.toFloat(), Y0.toFloat()),
                size = Size((X1 - X0).toFloat(), (Y1 - Y0).toFloat()),
                style = Stroke(1.2f),
            )
        }
    }
    // Interlignage « normal » de la maquette : 1,385 em.
    Text(
        "Ω",
        style = Paper.text(15f * SCALE, 15f * 1.385f * SCALE).copy(fontStyle = FontStyle.Italic),
        modifier = Modifier.at(SHIFT_X + 21 * SCALE, SHIFT_Y + 164 * SCALE),
    )
    SITES.forEachIndexed { i, site -> Cell(site, diagram.cells[i]) }
}

/** Zone d'une appli : sa cellule entière (polygone), avec la pastille au site, découpée par la cellule. */
@Composable
private fun Cell(site: Site, poly: List<Offset>) {
    val name = LocalPaper.current.name(site.slot)
    val left = poly.minOf { it.x }
    val top = poly.minOf { it.y }
    val width = poly.maxOf { it.x } - left
    val height = poly.maxOf { it.y } - top
    val shape = remember(poly) {
        GenericShape { size, _ ->
            val k = size.width / width
            poly.forEachIndexed { n, p ->
                val x = (p.x - left) * k
                val y = (p.y - top) * k
                if (n == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
    val center = page(site.x, site.y)
    Box(
        Modifier
            .at(left, top)
            .size(width.dp, height.dp)
            .semantics { contentDescription = "$name, ${site.slot.family.label}" }
            .slot(site.slot, shape),
    ) {
        // La cellule porte seule la description : la pastille n'est qu'un dessin.
        Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            SlotDisc(
                site.slot,
                cx = center.x - left,
                cy = center.y - top,
                size = DISC,
                style = DiscStyle.RING,
                ring = RING,
                glyphScale = GLYPH / DISC,
            )
        }
    }
}
