package fr.perso.nimbus.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capture d'écran JVM d'un @Composable, à la taille d'un téléphone (412 × 916 dp, xxhdpi).
 *
 * Sans option, `testDebugUnitTest` ne capture rien. Pour écrire le PNG :
 *   ./gradlew recordRoborazziDebug --tests 'fr.perso.nimbus.screens.ScreenshotSmokeTest'
 * → app/build/outputs/roborazzi/fr.perso.nimbus.screens.ScreenshotSmokeTest.textInPhoneSizedBox.png
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class ScreenshotSmokeTest {

    @Test
    fun textInPhoneSizedBox() {
        captureRoboImage {
            Box(
                modifier = Modifier.size(412.dp, 916.dp).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("Nimbus", color = Color.Black, fontSize = 48.sp)
            }
        }
    }
}
