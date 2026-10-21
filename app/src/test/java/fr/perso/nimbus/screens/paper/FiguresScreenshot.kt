package fr.perso.nimbus.screens.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.unlock.ALL_FIGURES
import fr.perso.nimbus.ui.unlock.FigureArt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Les figures du jour, chacune dans deux variantes, sur le papier de la page (368 × 250 unités), avec
 * un index (clé, thème, nom, explication). FIGURES=Fractales (un thème) ou FIGURES=koch,dragon (des
 * clés) ne capture qu'une partie.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class FiguresScreenshot {

    @Test
    fun allFigures() {
        val only = System.getenv("FIGURES")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val specs = ALL_FIGURES.filter { only.isEmpty() || it.group in only || it.key in only }
        val dir = File("build/outputs/roborazzi/figures").apply { mkdirs() }
        File(dir, "index.tsv").writeText(
            ALL_FIGURES.joinToString("\n") { listOf(it.key, it.group, it.title, it.info(0)).joinToString("\t") } + "\n",
        )
        for (spec in specs) for (variant in 0..1) {
            captureRoboImage("build/outputs/roborazzi/figures/figure_${spec.key}_$variant.png") {
                Box(Modifier.size(368.dp, 250.dp).background(PaperColors.Paper)) {
                    FigureArt(spec, variant, Modifier.size(368.dp, 250.dp))
                }
            }
        }
    }
}
