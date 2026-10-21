package fr.perso.nimbus.screens.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import fr.perso.nimbus.data.DailyFigure
import fr.perso.nimbus.data.FigureKind
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.unlock.FigureArtPreview
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Les figures du jour, chacune dans deux variantes, sur le papier de la page (368 × 250 unités). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class FiguresScreenshot {

    @Test
    fun allFigures() {
        for (kind in FigureKind.entries) for (variant in 0..1) {
            val name = "figure_%02d_%s_%d".format(kind.ordinal + 1, kind.name.lowercase(), variant)
            captureRoboImage("build/outputs/roborazzi/figures/$name.png") {
                Box(Modifier.size(368.dp, 250.dp).background(PaperColors.Paper)) {
                    FigureArtPreview(DailyFigure(kind, variant, ""), Modifier.size(368.dp, 250.dp))
                }
            }
        }
    }
}
