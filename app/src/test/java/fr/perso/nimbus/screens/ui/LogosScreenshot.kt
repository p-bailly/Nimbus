package fr.perso.nimbus.screens.ui

import android.content.ComponentName
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.Glyph
import fr.perso.nimbus.data.GlyphChoice
import fr.perso.nimbus.data.GlyphFactory
import fr.perso.nimbus.data.PaperLogo
import fr.perso.nimbus.ui.theme.PaperStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Les logos dessinés pour Nimbus, à la taille du sommaire et en grand, dans leur couleur. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class LogosScreenshot {
    @Test
    fun logos() {
        val factory = GlyphFactory.get(RuntimeEnvironment.getApplication())
        val app = AppEntry("test/Main#0", "test", ComponentName("test", "test.Main"), Process.myUserHandle(), "Test", ImageBitmap(1, 1), AppCategory.OTHER)
        UiFixture.shoot("logos") {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                for (logo in PaperLogo.entries) {
                    val tint = Color(logo.argb ?: 0xFF1A73E8)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        for (px in listOf(57, 180)) {
                            val glyph = factory.glyph(app, px, GlyphChoice.Logo(logo)) as Glyph.Mask
                            Image(glyph.bitmap, null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size((px / 3).dp))
                        }
                        Text(logo.label, style = PaperStyle.item)
                    }
                }
            }
        }
    }
}
