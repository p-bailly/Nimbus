package fr.perso.nimbus.ui.paper.layouts

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.DiscStyle
import fr.perso.nimbus.ui.paper.MathBuilder
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperPage
import fr.perso.nimbus.ui.paper.SlotDisc
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.tex
import fr.perso.nimbus.ui.theme.PaperColors

// Ensemble de Julia rempli de z² + c, c racine de c³ + 2c² + c + 1 de partie imaginaire positive :
// le lapin de Douady. L'image est précalculée (920 × 950 px) ; les applis sont posées sur une ligne
// de niveau du temps de fuite.

/** Cadre de la figure, dans le repère de la page. */
private const val FIG_X = 22f
private const val FIG_Y = 210f
private const val FIG_W = 368f
private const val FIG_H = 380f

/** Diamètre des pastilles et de leur motif. */
private const val DISC = 29f
private const val GLYPH = 14f

/** Place d'une appli : coin haut gauche de sa pastille (valeurs de la maquette). */
private class Place(val slot: PaperSlot, val left: Float, val top: Float)

private val Places = listOf(
    Place(PaperSlot.WHATSAPP, 186.3f, 247.3f),
    Place(PaperSlot.MESSAGES, 200.4f, 284.0f),
    Place(PaperSlot.DISCORD, 208.0f, 321.2f),
    Place(PaperSlot.REDDIT, 245.6f, 331.6f),
    Place(PaperSlot.TELEPHONE, 256.0f, 366.4f),
    Place(PaperSlot.GMAIL, 259.3f, 403.6f),
    Place(PaperSlot.YOUTUBE, 293.9f, 414.3f),
    Place(PaperSlot.TWITCH, 316.2f, 443.7f),
    Place(PaperSlot.STREMIO, 357.5f, 447.6f),
    Place(PaperSlot.SPOTIFY, 338.3f, 478.1f),
    Place(PaperSlot.PHOTOS, 311.8f, 503.5f),
    Place(PaperSlot.CAMERA, 273.3f, 497.6f),
    Place(PaperSlot.WAZE, 234.7f, 494.1f),
    Place(PaperSlot.MAPS, 206.8f, 526.0f),
    Place(PaperSlot.METEO, 178.8f, 502.1f),
    Place(PaperSlot.CLAUDE, 181.0f, 462.1f),
    Place(PaperSlot.GITHUB, 153.5f, 436.2f),
    Place(PaperSlot.DRIVE, 125.5f, 410.7f),
    Place(PaperSlot.FICHIERS, 125.1f, 370.8f),
    Place(PaperSlot.AUTHENTICATOR, 89.8f, 357.3f),
    Place(PaperSlot.BANQUE, 67.2f, 327.1f),
    Place(PaperSlot.PLAY, 25.2f, 323.1f),
    Place(PaperSlot.AMAZON, 42.9f, 293.9f),
    Place(PaperSlot.PRONOTE, 62.4f, 267.2f),
    Place(PaperSlot.LICHESS, 94.4f, 279.7f),
    Place(PaperSlot.BASKET, 129.7f, 282.2f),
    Place(PaperSlot.PIR, 159.8f, 268.2f),
)

/**
 * Texte courant de la maquette (14,5 sur 20) : une ligne qui mêle romain et italique y mesure en fait
 * 21 unités (Chrome arrondit différemment les ascendantes des deux fontes), ligne de base à 15 du haut.
 */
private val Body = Paper.body.copy(lineHeight = 21.sp)

/** Ramène la ligne de base de Compose (interligne centré : 15,5) sur celle de la maquette (15). */
private const val BODY_SHIFT = -0.5f

/** Ascendante et descendante des polices du papier, en em. */
private const val CM_ASCENT = 0.842f
private const val CM_DESCENT = 0.157f

/** Indices et exposants de la maquette : 10 unités (`font-size: 10px`), quelle que soit la taille du texte. */
private const val SCRIPT = 10f

/**
 * L'indice de la première ligne de la légende allonge la descente de cette ligne ; Compose centre
 * l'interligne sur ces métriques (prises sur la première ligne) et remonte ainsi toute la légende de la
 * moitié de l'allongement. Chrome ne compte pas l'indice (`line-height: 0` sur sub) : on redescend d'autant.
 */
private const val CAPTION_SHIFT = (SCRIPT * CM_DESCENT + (12.5f / 5f + 1f) - 12.5f * CM_DESCENT) / 2f

/**
 * Indice italique, abaissé comme `vertical-align: sub` dans Chrome : de [around] / 5 + 1, [around] étant
 * la taille du texte porteur. Avec une taille en sp, [BaselineShift] compte en ascendantes du texte
 * porteur (mesuré sur les captures) : les `sub` / `sup` des fondations, qui comptent en ascendantes
 * de l'indice, décalent ici environ 1,45 fois trop.
 */
private fun MathBuilder.subIt(s: String, around: Float = 14.5f) =
    b.withStyle(
        SpanStyle(fontSize = SCRIPT.sp, fontStyle = FontStyle.Italic, baselineShift = BaselineShift(-(around / 5f + 1f) / (CM_ASCENT * around))),
    ) { append(s) }

/** Exposant italique, relevé comme `vertical-align: super` : de [around] / 3 + 1. */
private fun MathBuilder.supIt(s: String, around: Float = 14.5f) =
    b.withStyle(
        SpanStyle(fontSize = SCRIPT.sp, fontStyle = FontStyle.Italic, baselineShift = BaselineShift((around / 3f + 1f) / (CM_ASCENT * around))),
    ) { append(s) }

/** Valeur absolue : pas de coupure après la barre ouvrante (la maquette n'en fait pas). */
private fun MathBuilder.abs(block: MathBuilder.() -> Unit) {
    +"|${Char(0x2060)}"
    block()
    +"|"
}

/** Espaces insécables (`white-space: nowrap` de la maquette). */
private fun String.nowrap() = replace(' ', Typography.nbsp)

@Composable
fun LapinPage() {
    PaperPage {
        Text(
            tex {
                bf("Définition.")
                +" L'ensemble de Julia rempli de "
                it("f"); +"("; it("z"); +") = "; it("z"); +"² + "; it("c")
                +" est "
                it("K"); subIt("c")
                +" = { "; it("z"); +" ∈ ℂ : ("; it("f"); supIt("n"); +"("; it("z"); +")) bornée } ; ici "
                it("c"); +" ≈ −0,1226 + 0,7449 "; it("i"); +"."
            },
            style = Body,
            modifier = Modifier.at(22, 142 + BODY_SHIFT).width(368.dp),
        )

        Image(
            painterResource(R.drawable.papier_lapin),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.at(FIG_X, FIG_Y).size(FIG_W.dp, FIG_H.dp),
        )
        Box(
            Modifier
                .at(FIG_X, FIG_Y)
                .size(FIG_W.dp, FIG_H.dp)
                .border(1.dp, PaperColors.Ink),
        )

        for (p in Places) {
            SlotDisc(
                p.slot,
                cx = p.left + DISC / 2f,
                cy = p.top + DISC / 2f,
                size = DISC,
                style = DiscStyle.RING,
                ring = 2f,
                glyphScale = GLYPH / DISC,
            )
        }

        Column(
            Modifier.at(22, 600).width(368.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                tex {
                    b.withStyle(SpanStyle(fontFeatureSettings = "smcp", letterSpacing = 0.04.em)) { append("Figure 1") }
                    +" – Le lapin de Douady "
                    it("K"); subIt("c", around = 12.5f)
                    +". Les applis sont sur une ligne de niveau du temps de fuite."
                },
                style = Paper.caption,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).offset(y = CAPTION_SHIFT.dp),
            )
            Text(
                tex {
                    bf("Proposition.")
                    +" "; it("Si"); +" "; abs { it("c") }; +" ≤ 2 "; it("et"); +" "; abs { it("z") }; +" > 2, "
                    it("alors"); +" "; abs { it("f"); +"("; it("z"); +")" }; +" ≥ "
                    abs { it("z") }; +"("; abs { it("z") }; +" − 1) > "; abs { it("z") }; +" "
                    it("; donc"); +" "; it("f"); supIt("n"); +"("; it("z"); +") → ∞ "; it("et"); +" "
                    it("K"); subIt("c"); +" "; it("est inclus dans le disque"); +" "; abs { it("z") }; +" ≤ 2."
                },
                style = Body,
                modifier = Modifier.offset(y = BODY_SHIFT.dp),
            )
            Text(
                tex {
                    bf("Exercice.")
                    +" Montrer que "; it("f"); +"³(0) = 0 si et seulement si "; it("c"); +" = 0 ou "
                    it("c"); +"³ + 2".nowrap(); it("c"); +"² + ".nowrap(); it("c"); +" + 1 = 0".nowrap(); +"."
                },
                style = Body,
                modifier = Modifier.offset(y = BODY_SHIFT.dp),
            )
        }
    }
}
