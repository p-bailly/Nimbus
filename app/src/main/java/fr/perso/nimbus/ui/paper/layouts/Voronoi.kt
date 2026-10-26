package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
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
import fr.perso.nimbus.ui.paper.tex
import kotlin.math.hypot

// Page « Voronoï » : chaque appli possède sa cellule, les points de Ω plus proches de son site que
// de tout autre. Un appui n'importe où dans la cellule ouvre l'appli ; un appui long puis un glissé
// déplace son site dans Ω, et les cellules suivent le doigt. L'exercice qui finissait la page est
// dans design/exercices-accueils.md : la figure a pris sa place, pour des logos plus grands.

/** Site d'une appli, dans le repère de la figure (avant mise à l'échelle). */
private class Site(val slot: PaperSlot, val x: Double, val y: Double)

// Sites d'origine, obtenus par relaxation de Lloyd dans Ω, écartés de moins d'un point de l'optimum
// symétrique pour que quatre sites ne soient jamais cocycliques.
private val SITES = listOf(
    Site(PaperSlot.WHATSAPP, 63.4, 205.5), Site(PaperSlot.MESSAGES, 168.5, 199.5), Site(PaperSlot.DISCORD, 137.9, 275.0),
    Site(PaperSlot.REDDIT, 57.0, 304.3), Site(PaperSlot.TELEPHONE, 159.2, 361.0), Site(PaperSlot.GMAIL, 56.9, 397.2),
    Site(PaperSlot.YOUTUBE, 267.5, 204.9), Site(PaperSlot.TWITCH, 356.1, 206.3), Site(PaperSlot.STREMIO, 234.4, 283.6),
    Site(PaperSlot.SPOTIFY, 344.6, 296.7), Site(PaperSlot.PHOTOS, 263.5, 362.5), Site(PaperSlot.CAMERA, 359.0, 392.8),
    Site(PaperSlot.WAZE, 127.3, 448.3), Site(PaperSlot.MAPS, 57.3, 499.4), Site(PaperSlot.METEO, 56.6, 591.6),
    Site(PaperSlot.CLAUDE, 157.9, 535.2), Site(PaperSlot.GITHUB, 264.3, 532.5), Site(PaperSlot.DRIVE, 233.9, 612.0),
    Site(PaperSlot.FICHIERS, 344.4, 599.8), Site(PaperSlot.AUTHENTICATOR, 136.6, 620.8), Site(PaperSlot.BANQUE, 63.3, 690.8),
    Site(PaperSlot.PLAY, 167.8, 697.8), Site(PaperSlot.MES_APPLIS, 266.8, 691.8), Site(PaperSlot.PRONOTE, 356.1, 689.9),
    Site(PaperSlot.LICHESS, 210.7, 447.4), Site(PaperSlot.BASKET, 294.4, 447.6), Site(PaperSlot.PIR, 358.8, 504.3),
    // LoL Live, ajoutée après coup : au plus grand vide laissé près des autres jeux (à 56 de tout autre site).
    Site(PaperSlot.LOL, 212.0, 504.0),
    // Scrabble, de même : au plus grand vide à côté de πr (à 57 de tout autre site, du « Ω » à 400).
    Site(PaperSlot.SCRABBLE, 320.4, 547.3),
)

/** Ω = [14, 398] × [160, 736], dans le repère de la figure. */
private const val X0 = 14.0
private const val Y0 = 160.0
private const val X1 = 398.0
private const val Y1 = 736.0

/**
 * Un site déplacé reste à [MARGIN] du cadre et à [GAP] des autres sites (repère de la figure) : sa
 * pastille (rayon 28,5) tient entière dans sa cellule, avec 2,5 de blanc jusqu'au bord.
 */
private const val MARGIN = 31.0
private const val GAP = 62.0

/** Le « Ω » du coin (centre de son dessin) : une pastille reste à [OMEGA_GAP] de son centre. */
private const val OMEGA_X = 27.5
private const val OMEGA_Y = 174.3
private const val OMEGA_GAP = 40.0

/** La figure est réduite à 0,95 et décalée de (10,3 ; 46) sur la page : Ω y va d'une marge à l'autre. */
private const val SCALE = 0.95f
private const val SHIFT_X = 10.3f
private const val SHIFT_Y = 46f

/**
 * Interlignage des paragraphes : 20 dans la maquette, mais Chrome y arrondit les métriques de la
 * police et toute ligne qui contient de l'italique y fait 21 de haut ; on reprend ce rendu.
 */
private const val LINE = 21f

/** Pastille : 54 unités de diamètre plus un cerne de 1,5 à l'extérieur, motif de 18 (avant réduction). */
private const val DISC = (54f + 2 * 1.5f) * SCALE
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
private fun cell(sites: List<Site>, i: Int): List<Vertex> {
    var poly = listOf(Vertex(X0, Y0, -1), Vertex(X1, Y0, -1), Vertex(X1, Y1, -1), Vertex(X0, Y1, -1))
    sites.forEachIndexed { j, b -> if (j != i) poly = clip(poly, sites[i], b, j) }
    return poly
}

/** Sites de la page : ceux d'origine, sauf ceux que l'utilisateur a déplacés. */
private fun sites(moved: Map<PaperSlot, Offset>): List<Site> = SITES.map { site ->
    moved[site.slot]?.let { Site(site.slot, it.x.toDouble(), it.y.toDouble()) } ?: site
}

/**
 * Position permise la plus proche de [p] pour le site [i] : dans Ω à [MARGIN] du cadre, à [GAP] au
 * moins des autres sites, hors du « Ω ». null si le site est coincé (il garde alors sa place).
 */
private fun allowed(p: Offset, i: Int, sites: List<Site>): Offset? {
    var x = p.x.toDouble()
    var y = p.y.toDouble()
    repeat(8) {
        x = x.coerceIn(X0 + MARGIN, X1 - MARGIN)
        y = y.coerceIn(Y0 + MARGIN, Y1 - MARGIN)
        var free = true
        // Trop près de (cx, cy) : repoussé sur le cercle de rayon [gap] autour de ce point.
        fun keepAway(cx: Double, cy: Double, gap: Double) {
            val dx = x - cx
            val dy = y - cy
            val d = hypot(dx, dy)
            if (d >= gap) return
            free = false
            if (d < 1e-6) {
                x = cx + gap + 1e-3
            } else {
                x = cx + dx * (gap + 1e-3) / d
                y = cy + dy * (gap + 1e-3) / d
            }
        }
        sites.forEachIndexed { j, other -> if (j != i) keepAway(other.x, other.y, GAP) }
        keepAway(OMEGA_X, OMEGA_Y, OMEGA_GAP)
        if (free) return Offset(x.toFloat(), y.toFloat())
    }
    return null
}

/** Indice du site le plus proche de ([x], [y]) : la cellule qui contient ce point. */
private fun nearest(sites: List<Site>, x: Double, y: Double): Int =
    sites.indices.minBy { hypot(sites[it].x - x, sites[it].y - y) }

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

private fun diagram(sites: List<Site>): Diagram {
    val fills = PaperFamily.entries.associateWith { Path() }
    val inner = Path()
    val outer = Path()
    val cells = sites.mapIndexed { i, site ->
        val poly = cell(sites, i)
        fills.getValue(site.slot.family).apply {
            poly.forEachIndexed { k, p -> if (k == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
            close()
        }
        // Chaque frontière une seule fois : depuis la cellule de plus petit indice.
        poly.forEachIndexed { k, p ->
            if (p.edge > i) {
                val q = poly[(k + 1) % poly.size]
                val path = if (sites[p.edge].slot.family == site.slot.family) inner else outer
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
    val env = LocalPaper.current
    // Sites posés (à jour dès le lâcher, sans attendre l'enregistrement) et site qui suit le doigt.
    var placed by remember(env.sites) { mutableStateOf(env.sites) }
    var dragged by remember { mutableStateOf<Pair<PaperSlot, Offset>?>(null) }
    val sites = remember(placed, dragged) { sites(dragged?.let { placed + it } ?: placed) }
    val diagram = remember(sites) { diagram(sites) }
    PaperPage {
        Text(
            tex { bf("Définition."); +" Pour "; it("a"); +" ∈ "; it("A"); +" = {applis}, la cellule de "; it("a"); +" est" },
            style = Paper.text(15f, LINE),
            modifier = Modifier.at(22, 142).width(368.dp),
        )
        // (1) : centrée sur la largeur du texte moins la colonne du numéro, comme dans la maquette,
        // 6 sous la définition (une ligne de [LINE]).
        Box(Modifier.at(22, 169).width(340.dp).height(LINE.dp), contentAlignment = Alignment.Center) {
            Text(
                tex {
                    it("V"); +"("; it("a"); +") = { "; it("x"); +" ∈ Ω : ‖"; it("x"); +" − "; it("a"); +"‖ ≤ ‖"
                    it("x"); +" − "; it("b"); +"‖ pour tout "; it("b"); +" ∈ "; it("A"); +" }"
                },
                style = Paper.text(13.75f, LINE),
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(Modifier.at(22, 169).width(368.dp).height(LINE.dp), contentAlignment = Alignment.CenterEnd) {
            Text("(1)", style = Paper.text(15f, LINE))
        }

        Figure(
            diagram,
            sites,
            onMove = { slot, at -> dragged = slot to at },
            onDrop = {
                dragged?.let {
                    placed = placed + it
                    env.onSites(placed)
                }
                dragged = null
            },
            onCancel = { dragged = null },
        )

        Text(
            tex {
                b.withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                +" – Un appui en "; it("x"); +" ∈ "; it("V"); +"("; it("a"); +") ouvre l'appli "; it("a"); +"."
            },
            style = Paper.text(13.5f, 17f).copy(textAlign = TextAlign.Center),
            modifier = Modifier.at(22, 755).width(368.dp),
        )
        // Clé des familles.
        Row(
            Modifier.at(22, 778).width(368.dp).height(17.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaperFamily.entries.forEach { family ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(9.dp).background(family.color, CircleShape))
                    Text(family.label, style = Paper.text(12f, 17f, family.color).copy(letterSpacing = 0.04.em))
                }
            }
        }

        Text(
            tex {
                bf("Proposition."); +" "; italic { +"Pour a ≠ b," }; +" "
                it("V"); +"("; it("a"); +") ∩ "; it("V"); +"("; it("b"); +") ⊂ "; it("médiatrice de"); +" ["; it("a"); +", "; it("b"); +"]."
            },
            style = Paper.text(15.5f, LINE),
            modifier = Modifier.at(22, 804).width(368.dp),
        )
    }
}

/** Le diagramme, « Ω », puis les applis : chacune occupe toute sa cellule, pastille au site. */
@Composable
private fun Figure(
    diagram: Diagram,
    sites: List<Site>,
    onMove: (PaperSlot, Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
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
    sites.forEachIndexed { i, site -> key(site.slot) { Cell(site, diagram.cells[i]) } }
    Touch(sites, onMove, onDrop, onCancel)
}

/**
 * Touchers de toute la figure, au-dessus des cellules : le point touché est dans la cellule du site
 * le plus proche. Appui : ouvrir l'appli ; appui long : ses actions ; appui long puis glissé :
 * déplacer son site ([onMove] à chaque pas, [onDrop] au lâcher). Une seule zone qui ne bouge pas :
 * les cellules changent de forme sous le doigt sans interrompre le geste.
 */
@Composable
private fun Touch(sites: List<Site>, onMove: (PaperSlot, Offset) -> Unit, onDrop: () -> Unit, onCancel: () -> Unit) {
    val env by rememberUpdatedState(LocalPaper.current)
    val current by rememberUpdatedState(sites)
    val haptics = LocalHapticFeedback.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val origin = page(X0, Y0)
    Box(
        Modifier
            .at(origin.x, origin.y)
            .size(((X1 - X0) * SCALE).dp, ((Y1 - Y0) * SCALE).dp)
            .onGloballyPositioned { coords = it }
            // Ordre important : le glissé (dernier) voit les événements avant l'appui simple.
            .pointerInput(Unit) {
                val unit = 1.dp.toPx() * SCALE
                detectTapGestures(onTap = { env.onOpen(current[nearest(current, X0 + it.x / unit, Y0 + it.y / unit)].slot) })
            }
            .pointerInput(Unit) {
                // Pixels par unité de la figure.
                val unit = 1.dp.toPx() * SCALE
                val slop = viewConfiguration.touchSlop
                var index = 0
                var start = Offset.Zero
                var total = Offset.Zero
                var moving = false
                // Pastille du site à l'écran : ancre du pop-up des actions.
                fun disc(site: Site): Rect {
                    val c = coords ?: return Rect.Zero
                    val r = DISC / SCALE / 2
                    val x = ((site.x - X0) * unit).toFloat()
                    val y = ((site.y - Y0) * unit).toFloat()
                    return Rect(c.localToWindow(Offset(x - r * unit, y - r * unit)), c.localToWindow(Offset(x + r * unit, y + r * unit)))
                }
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        index = nearest(current, X0 + at.x / unit, Y0 + at.y / unit)
                        start = Offset(current[index].x.toFloat(), current[index].y.toFloat())
                        total = Offset.Zero
                        moving = false
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        if (!moving && total.getDistance() > slop) moving = true
                        if (moving) allowed(start + total / unit, index, current)?.let { onMove(current[index].slot, it) }
                    },
                    onDragEnd = {
                        if (moving) onDrop() else current[index].let { env.onActions(it.slot, disc(it)) }
                        moving = false
                    },
                    onDragCancel = {
                        if (moving) onCancel()
                        moving = false
                    },
                )
            },
    )
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
    val env = LocalPaper.current
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Touchers : [Touch], au-dessus ; ici, la cellule ne porte que l'accessibilité.
    Box(
        Modifier
            .at(left, top)
            .size(width.dp, height.dp)
            .onGloballyPositioned { coords = it }
            .semantics {
                contentDescription = "$name, ${site.slot.family.label}"
                onClick(label = "Ouvrir $name") { env.onOpen(site.slot); true }
                onLongClick(label = "Actions pour $name") { env.onActions(site.slot, coords?.boundsInWindow() ?: Rect.Zero); true }
            }
            .clip(shape),
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
