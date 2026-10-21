package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.data.PaperSlot.AMAZON
import fr.perso.nimbus.data.PaperSlot.AUTHENTICATOR
import fr.perso.nimbus.data.PaperSlot.BANQUE
import fr.perso.nimbus.data.PaperSlot.BASKET
import fr.perso.nimbus.data.PaperSlot.CAMERA
import fr.perso.nimbus.data.PaperSlot.CLAUDE
import fr.perso.nimbus.data.PaperSlot.DISCORD
import fr.perso.nimbus.data.PaperSlot.DRIVE
import fr.perso.nimbus.data.PaperSlot.FICHIERS
import fr.perso.nimbus.data.PaperSlot.GITHUB
import fr.perso.nimbus.data.PaperSlot.GMAIL
import fr.perso.nimbus.data.PaperSlot.LICHESS
import fr.perso.nimbus.data.PaperSlot.MAPS
import fr.perso.nimbus.data.PaperSlot.MESSAGES
import fr.perso.nimbus.data.PaperSlot.METEO
import fr.perso.nimbus.data.PaperSlot.PHOTOS
import fr.perso.nimbus.data.PaperSlot.PIR
import fr.perso.nimbus.data.PaperSlot.PLAY
import fr.perso.nimbus.data.PaperSlot.PRONOTE
import fr.perso.nimbus.data.PaperSlot.REDDIT
import fr.perso.nimbus.data.PaperSlot.SPOTIFY
import fr.perso.nimbus.data.PaperSlot.STREMIO
import fr.perso.nimbus.data.PaperSlot.TELEPHONE
import fr.perso.nimbus.data.PaperSlot.TWITCH
import fr.perso.nimbus.data.PaperSlot.WAZE
import fr.perso.nimbus.data.PaperSlot.WHATSAPP
import fr.perso.nimbus.data.PaperSlot.YOUTUBE
import fr.perso.nimbus.ui.components.rememberCrayonLogo
import fr.perso.nimbus.ui.paper.LOGO_FILL
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.MathBuilder
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.SlotGlyph
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.slot
import fr.perso.nimbus.ui.paper.tex
import fr.perso.nimbus.ui.theme.PaperColors
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Accueil « Sierpiński » : les 27 applis sont les 27 triangles de T₃, le troisième pas de la
// construction du triangle de Sierpiński. Chaque bloc de T₁ (haut, bas gauche, bas droite) est
// partagé en trois sous-blocs de trois triangles ; un sous-bloc a sa couleur. L'exercice qui
// finissait la page est dans design/exercices-accueils.md : la place a servi à agrandir les logos.

/** Sommet du haut et côté de T₀. */
private const val APEX_X = 206f
private const val APEX_Y = 266f
private const val SIDE = 380f
private val S3 = sqrt(3f) / 2f

/** Couleur de chaque sous-bloc : PAL[a][b], a = bloc de T₁, b = sous-bloc de T₂. */
private val PAL = listOf(
    listOf(0xFF2F5BD3, 0xFF4A72E0, 0xFFC77700),
    listOf(0xFFC23A6B, 0xFFD4567F, 0xFF0F8A6C),
    listOf(0xFF6B46C1, 0xFF7D5BD0, 0xFF8F70DC),
).map { row -> row.map(::Color) }

/** Place de chaque triangle : APPS[a][3b + c]. */
private val APPS = listOf(
    listOf(WHATSAPP, MESSAGES, DISCORD, REDDIT, TELEPHONE, GMAIL, LICHESS, BASKET, PIR),
    listOf(YOUTUBE, TWITCH, STREMIO, SPOTIFY, PHOTOS, CAMERA, WAZE, MAPS, METEO),
    listOf(CLAUDE, GITHUB, DRIVE, FICHIERS, AUTHENTICATOR, BANQUE, PLAY, AMAZON, PRONOTE),
)

/** Un triangle de T₃ : boîte englobante (x, y, côté, hauteur), pointe en haut au milieu. */
private class Cell(val slot: PaperSlot, val color: Color, val x: Float, val y: Float, val side: Float) {
    val height = side * S3
}

/** Les 27 triangles, construits comme T_{n+1} = f₁(T_n) ∪ f₂(T_n) ∪ f₃(T_n). */
private val CELLS: List<Cell> = buildList {
    fun rec(ax: Float, ay: Float, s: Float, path: List<Int>) {
        if (path.size == 3) {
            val (a, b, c) = path
            add(Cell(APPS[a][b * 3 + c], PAL[a][b], ax - s / 2f, ay, s))
            return
        }
        val half = s / 2f
        rec(ax, ay, half, path + 0)
        rec(ax - half / 2f, ay + half * S3, half, path + 1)
        rec(ax + half / 2f, ay + half * S3, half, path + 2)
    }
    rec(APEX_X, APEX_Y, SIDE, emptyList())
}

/** Triangle pointe en haut inscrit dans sa boîte. */
private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** Clé des couleurs : un sous-bloc par famille. */
private val KEY = listOf(
    "Échanges" to PAL[0][0],
    "Jeux & perso" to PAL[0][2],
    "Écran" to PAL[1][0],
    "Trajets" to PAL[1][2],
    "Outils" to PAL[2][0],
)

@Composable
fun SierpinskiPage() {
    PaperPage {
        Definition()
        Figure()
        Caption()
        Theorem()
    }
}

@Composable
private fun Definition() {
    Text(
        tex {
            bf("Définition.")
            +" Soient "; it("s"); lo("1"); +", "; it("s"); lo("2"); +", "; it("s"); lo("3")
            +" les sommets du triangle "; it("T"); lo("0"); +",\n"
            it("f"); lo("i", italic = true); +"("; it("x"); +") = ("; it("x"); +" + "; it("s"); lo("i", italic = true)
            +")/2 pour tout point "; it("x"); +" ∈ ℝ², et "; it("T"); script(raise = false) { it("n"); +"+1" }; +" =\n"
            it("f"); lo("1"); +"("; it("T"); lo("n", italic = true); +") ∪ "
            it("f"); lo("2"); +"("; it("T"); lo("n", italic = true); +") ∪ "
            it("f"); lo("3"); +"("; it("T"); lo("n", italic = true); +")."
        },
        style = Paper.text(15f, 26.5f),
        modifier = Modifier.baseline(22f, 157.1f).width(368.dp),
    )
}

/** T₀ en filet léger, les 27 triangles pleins de T₃, et une place touchable par triangle. */
@Composable
private fun Figure() {
    Canvas(Modifier.size(412.dp, 916.dp)) {
        val u = 1.dp.toPx()
        val outline = Path().apply {
            moveTo(APEX_X * u, APEX_Y * u)
            lineTo((APEX_X - SIDE / 2f) * u, (APEX_Y + SIDE * S3) * u)
            lineTo((APEX_X + SIDE / 2f) * u, (APEX_Y + SIDE * S3) * u)
            close()
        }
        drawPath(outline, PaperColors.Ink.copy(alpha = 0.18f), style = Stroke(width = 0.8f * u))
        for (c in CELLS) {
            val p = Path().apply {
                moveTo((c.x + c.side / 2f) * u, c.y * u)
                lineTo((c.x + c.side) * u, (c.y + c.height) * u)
                lineTo(c.x * u, (c.y + c.height) * u)
                close()
            }
            drawPath(p, c.color)
        }
    }
    for (c in CELLS) TriangleSlot(c)
}

/**
 * Disque d'un logo, au centre de gravité de son triangle : plus large que le cercle inscrit (27,4), il
 * déborde sur le blanc des trous de T₃ et laisse voir les trois pointes ; 9,5 entre deux disques voisins.
 */
private const val DISC = 38f

/** Place d'un triangle : logo sur un disque blanc au centre de gravité, ou motif blanc de 22 unités. */
@Composable
private fun TriangleSlot(c: Cell) {
    val env = LocalPaper.current
    val app = env.app(c.slot)
    val logo = rememberCrayonLogo(app)
    val badge = app?.let { env.badges[it.packageName] } ?: 0
    Box(
        Modifier
            .at(c.x, c.y)
            .size(c.side.dp, c.height.dp)
            .slot(c.slot, TriangleShape),
    ) {
        val cy = 2f * c.height / 3f
        val glyph = Modifier.offset((c.side / 2f - 11f).dp, (cy - 11f).dp)
        if (app != null && (env.ownColors || logo != null)) {
            // Aux couleurs des applis, ou avec un logo au crayon : le motif prend la couleur de l'appli
            // sur un disque blanc cerclé de celle du triangle (débordant du triangle, voir [DISC]).
            Box(
                Modifier
                    .offset((c.side / 2f - DISC / 2f).dp, (cy - DISC / 2f).dp)
                    .size(DISC.dp)
                    .background(Color.White, CircleShape)
                    .border(1.5.dp, c.color, CircleShape),
                contentAlignment = Alignment.Center,
            ) { SlotGlyph(c.slot, if (logo != null) DISC * LOGO_FILL else 19f, Color.White, own = true) }
        } else if (app != null) {
            SlotGlyph(c.slot, 22f, Color.White, glyph)
        } else {
            // Place sans appli : son initiale ; un appui permet de la choisir.
            Box(glyph.size(22.dp), contentAlignment = Alignment.Center) {
                Text(
                    c.slot.title.take(1),
                    style = Paper.text(17f, 22f, Color.White).copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
                )
            }
        }
        if (badge > 0) {
            // Pastille de notification : petit point d'encre cerclé de papier, en haut à droite du disque.
            Box(
                Modifier
                    .offset((c.side / 2f + DISC * 0.35f - 3.5f).dp, (cy - DISC * 0.35f - 3.5f).dp)
                    .size(7.dp)
                    .background(PaperColors.Paper, CircleShape)
                    .padding(1.5.dp)
                    .background(PaperColors.Ink, CircleShape),
            )
        }
    }
}

@Composable
private fun Caption() {
    Text(
        tex {
            b.withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
            +" – Le triangle "; it("T"); lo("3", parent = 13.5f, size = 10f); +" ; couleur : famille de l'appli."
        },
        style = Paper.text(13.5f, 17f).copy(textAlign = TextAlign.Center),
        modifier = Modifier.baseline(22f, 634f).width(368.dp),
    )
    Row(
        Modifier.baseline(22f, 661f).width(368.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((label, color) in KEY) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(10.dp, 9.dp)) {
                    drawPath(
                        Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        },
                        color,
                    )
                }
                Spacer(Modifier.width(5.dp))
                Text(label, style = Paper.text(12.5f, 17f, color), maxLines = 1)
            }
        }
    }
}

@Composable
private fun Theorem() {
    val body = Paper.text(16f, 22.4f)
    Text(
        tex {
            bf("Théorème."); +" "; it("T"); lo("n", italic = true, parent = 16f, size = 11f); +" "; it("est formé de"); +" 3"
            hi("n", italic = true, parent = 16f, size = 11f); +" "; it("triangles et")
        },
        style = body,
        modifier = Modifier.baseline(22f, 716f),
    )
    // aire(T_n) = (3/4)ⁿ aire(T₀) ⟶ 0, centrée sur la colonne de gauche ; numéro à droite.
    Box(Modifier.baseline(22f, 758f).width(338.dp), contentAlignment = Alignment.TopCenter) { AreaFormula() }
    Text(
        "(1)",
        style = Paper.text(18.5f, 25.9f).copy(textAlign = TextAlign.End),
        modifier = Modifier.baseline(22f, 759f).width(368.dp),
    )
    Text(
        tex {
            it("Les "); +"27"; it(" applis sont les "); +"27"; it(" triangles de "); it("T"); lo("3", parent = 16f, size = 11f); it(".")
        },
        style = body,
        modifier = Modifier.baseline(22f, 798f),
    )
}

@Composable
private fun AreaFormula() {
    // Corps 18,5 : la formule de la maquette (17) agrandie de 18,5 / 17, décalages compris.
    val eq = Paper.text(18.5f, 25.9f)
    val big = Paper.text(32.6f, 32.6f)
    val digit = Paper.text(18.5f, 19.4f)
    Formula {
        Text(tex { +"aire("; it("T"); lo("n", italic = true, parent = 18.5f, size = 10.9f); +") =" }, style = eq)
        Gap(6.17f)
        Text("(", style = big, modifier = Modifier.layoutId(6.86f))
        Column(
            Modifier.layoutId(-8.7f).padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Filet et dénominateur aux hauteurs relevées sur la maquette.
            Text("3", style = digit, modifier = Modifier.padding(horizontal = 2.dp))
            Box(Modifier.padding(top = 0.6.dp).width(13.6.dp).height(0.85.dp).background(PaperColors.Ink))
            Text("4", style = digit, modifier = Modifier.offset(y = (-0.3).dp).padding(horizontal = 2.dp))
        }
        Text(")", style = big, modifier = Modifier.layoutId(6.86f))
        Text("n", style = Paper.text(12f, 15.2f).copy(fontStyle = FontStyle.Italic), modifier = Modifier.layoutId(-7.26f))
        Gap(6.17f)
        Text(tex { +"aire("; it("T"); lo("0", parent = 18.5f, size = 10.9f); +")" }, style = eq)
        // Espace + 6 unités de chaque côté dans la maquette ; la flèche longue de Latin Modern est
        // 3 unités plus large que celle de la maquette : 4,5 unités (4,9 en corps 18,5) suffisent.
        Gap(6.17f + 4.9f)
        Text("⟶", style = eq)
        Gap(4.9f + 6.17f)
        Text("0.", style = eq)
    }
}

@Composable
private fun Gap(width: Float) = Spacer(Modifier.width(width.dp))

/**
 * Formule sur une ligne : les éléments se suivent sans espace, leurs lignes de base sur celle de la
 * formule, sauf ceux marqués `Modifier.layoutId(dy)`, abaissés de dy unités (parenthèses hautes,
 * fraction, exposant). La ligne de base de la formule est sa [FirstBaseline].
 */
@Composable
private fun Formula(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        // Position de la ligne de base de la formule depuis le haut de chaque élément.
        val bases = placeables.mapIndexed { i, p ->
            val first = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: 0
            first - ((measurables[i].layoutId as? Float) ?: 0f).dp.toPx()
        }
        val ascent = bases.max()
        val descent = placeables.indices.maxOf { placeables[it].height - bases[it] }
        layout(placeables.sumOf { it.width }, (ascent + descent).roundToInt(), mapOf(FirstBaseline to ascent.roundToInt())) {
            var x = 0
            placeables.forEachIndexed { i, p ->
                p.place(x, (ascent - bases[i]).roundToInt())
                x += p.width
            }
        }
    }
}

/**
 * Ascendante des polices papier, en em. Mesuré sur les captures : [BaselineShift] se compte en
 * ascendantes du texte qui porte l'indice, pas de l'indice lui-même.
 */
private const val ASCENT = 0.842f

/**
 * Indice ou exposant de [size] unités placé comme par le navigateur de la maquette : ligne de base
 * abaissée de parent / 5 + 1 unités (indice) ou remontée de parent / 3 + 1 (exposant), [parent]
 * étant la taille du texte qui le porte.
 */
private fun MathBuilder.script(raise: Boolean, parent: Float = 14.5f, size: Float = 10f, block: MathBuilder.() -> Unit) {
    val shift = if (raise) parent / 3f + 1f else -(parent / 5f + 1f)
    b.withStyle(SpanStyle(fontSize = size.sp, fontStyle = FontStyle.Normal, baselineShift = BaselineShift(shift / (ASCENT * parent)))) {
        MathBuilder(this).block()
    }
}

/** Indice (romain, ou italique pour une variable). */
private fun MathBuilder.lo(s: String, italic: Boolean = false, parent: Float = 14.5f, size: Float = 10f) =
    script(raise = false, parent, size) { if (italic) it(s) else +s }

/** Exposant. */
private fun MathBuilder.hi(s: String, italic: Boolean = false, parent: Float = 14.5f, size: Float = 10f) =
    script(raise = true, parent, size) { if (italic) it(s) else +s }

/** Place l'élément en x = [x] et sa première ligne de base en y = [y] (unités du repère). */
private fun Modifier.baseline(x: Float, y: Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    val first = p[FirstBaseline].takeIf { it != AlignmentLine.Unspecified } ?: 0
    layout(p.width, p.height) { p.place(x.dp.roundToPx(), (y.dp.toPx() - first).roundToInt()) }
}
