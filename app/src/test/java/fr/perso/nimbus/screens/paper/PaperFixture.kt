package fr.perso.nimbus.screens.paper

import android.content.ComponentName
import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
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
 * à 20 h 35 le mardi 20 octobre 2026, barres système de 30 et 24 dp (1 unité = 1 dp), animations
 * arrêtées. Comme sur l'accueil, ni en-tête ni heure ni date.
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
            clock = false,
            fixedTime = LocalDateTime.of(2026, 10, 20, 20, 35),
            fixedInsets = 30f to 24f,
            animate = false,
            syncGlyphs = true,
        )
    }

    /**
     * Couleurs propres d'applis vraisemblables (celles du sommaire : WhatsApp vert, YouTube rouge,
     * Google bleu…), pour les pages aux couleurs des applis.
     */
    val ownColors: Map<PaperSlot, Color> = PaperSlot.entries.associateWith { slot ->
        Color(
            when (slot) {
                PaperSlot.WHATSAPP -> 0xFF1D9E52
                PaperSlot.YOUTUBE -> 0xFFE0201B
                PaperSlot.REDDIT -> 0xFFD63C08
                PaperSlot.DISCORD -> 0xFF4752C4
                PaperSlot.SPOTIFY -> 0xFF168D40
                PaperSlot.TWITCH -> 0xFF7A2FE6
                PaperSlot.CLAUDE -> 0xFFC2582F
                PaperSlot.AMAZON -> 0xFFC4620A
                PaperSlot.WAZE -> 0xFF0A8FC2
                PaperSlot.BANQUE -> 0xFF00579D
                PaperSlot.BASKET -> 0xFFE0560C
                PaperSlot.GITHUB, PaperSlot.LICHESS -> 0xFF14141A
                else -> 0xFF1A73E8
            },
        )
    }

    fun shoot(name: String, colors: Map<PaperSlot, Color> = emptyMap(), content: @Composable () -> Unit) {
        val env = env()
        captureRoboImage("build/outputs/roborazzi/paper/$name.png") {
            CompositionLocalProvider(LocalPaper provides PaperEnv(env.apps, clock = env.clock, fixedTime = env.fixedTime, fixedInsets = env.fixedInsets, animate = false, syncGlyphs = true, colors = colors)) {
                content()
            }
        }
    }
}
