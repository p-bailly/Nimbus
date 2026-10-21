package fr.perso.nimbus.screens.paper

import android.content.ComponentName
import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import com.github.takahirom.roborazzi.captureRoboImage
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PaperEnv
import java.time.LocalDateTime

/**
 * Banc des captures des pages « papier » : 27 apps fictives (leur motif est l'initiale), heure figée
 * à 20 h 35 le mardi 20 octobre 2026, barres système de 30 et 24 dp (le repère tombe alors pile sur
 * celui des maquettes, 1 unité = 1 dp), animations arrêtées.
 *
 *   ./gradlew recordRoborazziDebug --tests 'fr.perso.nimbus.screens.paper.*'
 * → app/build/outputs/roborazzi/paper/<page>.png
 */
object PaperFixture {
    fun env(): PaperEnv {
        val user = Process.myUserHandle()
        val apps = PaperSlot.entries.associateWith { slot ->
            val pkg = "test.${slot.name.lowercase()}"
            AppEntry(
                key = "$pkg/Main#0",
                packageName = pkg,
                componentName = ComponentName(pkg, "$pkg.Main"),
                user = user,
                label = slot.title,
                icon = ImageBitmap(1, 1),
                autoCategory = AppCategory.OTHER,
            )
        }
        return PaperEnv(
            apps = apps,
            fixedTime = LocalDateTime.of(2026, 10, 20, 20, 35),
            fixedInsets = 30f to 24f,
            animate = false,
            syncGlyphs = true,
        )
    }

    fun shoot(name: String, content: @Composable () -> Unit) {
        captureRoboImage("build/outputs/roborazzi/paper/$name.png") {
            CompositionLocalProvider(LocalPaper provides env()) { content() }
        }
    }
}
