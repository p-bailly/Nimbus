package fr.perso.nimbus.screens.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.ui.paper.Paper
import fr.perso.nimbus.ui.paper.PaperCanvas
import fr.perso.nimbus.ui.paper.PaperText
import fr.perso.nimbus.ui.paper.at
import fr.perso.nimbus.ui.paper.tex
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Banc des indices et exposants (comparé à une page HTML équivalente). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class ScriptsProbe {
    @Test
    fun page() = PaperFixture.shoot("scripts_probe") {
        PaperCanvas {
            Box(Modifier.at(0, 0).size(412.dp, 916.dp).background(Color.White))
            PaperText(tex { +"Xx D"; sub("3"); +" = {e, r, r"; sup("2"); +", s} Xx" }, Paper.text(14.5f, 20f), Modifier.at(22, 100).width(368.dp))
            PaperText(tex { +"Xx dans D"; sub("3"); +". Xx" }, Paper.text(16f, 24f), Modifier.at(22, 200).width(368.dp))
            PaperText(tex { +"Xx 7"; sup("2026"); +" Xx" }, Paper.text(24f, 48f), Modifier.at(22, 300).width(368.dp))
            PaperText(tex { +"Xx T"; sub("n", italic = true); +" a "; it("x"); sup("n+1"); +" Xx et une ligne très longue qui se coupe pour voir la deuxième ligne Xx" }, Paper.text(14.5f, 20f), Modifier.at(22, 400).width(368.dp))
        }
    }
}
