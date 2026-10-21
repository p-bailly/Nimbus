package fr.perso.nimbus.screens.paper

import android.graphics.BitmapFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import com.github.takahirom.roborazzi.captureRoboImage
import fr.perso.nimbus.data.Game
import fr.perso.nimbus.data.GameTeam
import fr.perso.nimbus.data.LiveLine
import fr.perso.nimbus.data.MeteoNow
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PaperEnv
import fr.perso.nimbus.ui.unlock.UnlockPage
import fr.perso.nimbus.ui.unlock.UnlockState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime

/**
 * Page de déverrouillage (H8a) avec les données de la maquette : Morteau, 11 °C (ressenti 9), « Éclaircies »,
 * 16 et 7 aujourd'hui, le coloriage du soir de Météo (1027 × 717, celui dont la maquette montre le bas),
 * Spurs 83 – 81 Mavericks au 3ᵉ quart-temps ; 20 h 35 le mardi 20 octobre 2026.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class UnlockScreenshot {

    private val meteo = MeteoNow(
        place = "Morteau",
        region = "Doubs",
        temperature = 11.0,
        feelsLike = 9.0,
        sky = "Éclaircies",
        todayMax = 16.0,
        todayMin = 7.0,
        weatherCode = 2,
        isDay = false,
        fetchedAt = 0L,
        stale = false,
    )

    private val sketch by lazy {
        val stream = checkNotNull(javaClass.getResourceAsStream("/unlock/coloriage_morteau.png"))
        stream.use { BitmapFactory.decodeStream(it) }.asImageBitmap()
    }

    private val spurs = LiveLine(
        Game(
            id = "401",
            state = "in",
            statusName = "STATUS_IN_PROGRESS",
            period = 3,
            displayClock = "4:12",
            home = GameTeam("6", "DAL", "Mavericks", 81),
            away = GameTeam("24", "SA", "Spurs", 83),
        ),
        awayFavorite = true,
        homeFavorite = false,
    )

    private fun shoot(name: String, state: UnlockState, time: LocalDateTime = LocalDateTime.of(2026, 10, 20, 20, 35)) {
        val base = PaperFixture.env()
        val env = PaperEnv(
            apps = base.apps,
            fixedTime = time,
            fixedInsets = base.fixedInsets,
            animate = false,
            syncGlyphs = true,
        )
        captureRoboImage("build/outputs/roborazzi/paper/$name.png") {
            CompositionLocalProvider(LocalPaper provides env) {
                UnlockPage(state, onMeteo = {}, onBasket = {}, onUnlock = {})
            }
        }
    }

    @Test
    fun page() = shoot("h8_deverrouillage", UnlockState(meteo, sketch, listOf(spurs)))

    /** Aucun match en direct : la section « Basket » disparaît, l'exercice remonte. */
    @Test
    fun noLiveGame() = shoot("h8_sans_direct", UnlockState(meteo, sketch, emptyList()))

    /** Ni Météo ni match : l'exercice suit la date. */
    @Test
    fun noSections() = shoot("h8_sans_sections", UnlockState())

    /** Jour 297 (samedi 24 octobre) : énoncé de trois lignes, solution d'hier sur quatre. */
    @Test
    fun longExercise() = shoot("h8_exercice_long", UnlockState(meteo, sketch, listOf(spurs)), LocalDateTime.of(2026, 10, 24, 20, 35))

    /**
     * Cas limites : lieu et ciel longs, températures négatives, trois matchs (deux favorites l'une contre
     * l'autre à la mi-temps, une fin de quart-temps, une double prolongation) ; le 1er janvier 2027 au
     * matin, la solution d'hier est celle du 31 décembre (n° 365, avec 2026).
     */
    @Test
    fun longLabels() {
        fun game(id: String, status: String, period: Int, clock: String, home: GameTeam, away: GameTeam) =
            Game(id, "in", status, period, clock, home, away)
        val lines = listOf(
            LiveLine(
                game("1", "STATUS_HALFTIME", 2, "0.0", GameTeam("22", "POR", "Trail Blazers", 58), GameTeam("16", "MIN", "Timberwolves", 61)),
                awayFavorite = true,
                homeFavorite = true,
            ),
            LiveLine(
                game("2", "STATUS_END_PERIOD", 3, "0.0", GameTeam("9", "GS", "Warriors", 88), GameTeam("13", "LAL", "Lakers", 90)),
                awayFavorite = true,
                homeFavorite = false,
            ),
            LiveLine(
                game("3", "STATUS_IN_PROGRESS", 6, "0:45", GameTeam("2", "BOS", "Celtics", 121), GameTeam("20", "PHI", "76ers", 119)),
                awayFavorite = false,
                homeFavorite = true,
            ),
        )
        val cold = meteo.copy(
            place = "Saint-Rémy-de-Provence",
            temperature = -3.4,
            feelsLike = -8.2,
            sky = "Fortes averses de neige",
            todayMax = 1.0,
            todayMin = -6.0,
        )
        shoot("h8_libelles_longs", UnlockState(cold, sketch, lines), LocalDateTime.of(2027, 1, 1, 7, 5))
    }
}
