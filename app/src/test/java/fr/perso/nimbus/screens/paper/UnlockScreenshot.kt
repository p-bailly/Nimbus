package fr.perso.nimbus.screens.paper

import android.graphics.BitmapFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import com.github.takahirom.roborazzi.captureRoboImage
import fr.perso.nimbus.data.Game
import fr.perso.nimbus.data.GameTeam
import fr.perso.nimbus.data.LiveLine
import fr.perso.nimbus.data.MessageApps
import fr.perso.nimbus.data.MeteoNow
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PaperEnv
import fr.perso.nimbus.ui.unlock.ALL_FIGURES
import fr.perso.nimbus.ui.unlock.DailyFigure
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

    private fun shoot(
        name: String,
        state: UnlockState,
        time: LocalDateTime = LocalDateTime.of(2026, 10, 20, 20, 35),
        infoShown: Boolean = false,
        figure: DailyFigure? = null,
        wallpaper: Boolean = false,
    ) {
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
                UnlockPage(state, onMeteo = {}, onBasket = {}, onUnlock = {}, infoShown = infoShown, figure = figure, wallpaper = wallpaper)
            }
        }
    }

    @Test
    fun page() = shoot("h8_deverrouillage", UnlockState(meteo, sketch, listOf(spurs)))

    /** Au réveil : plus de direct, les résultats de la nuit des favorites (dont une prolongation). */
    @Test
    fun lastNight() {
        fun final(id: String, period: Int, home: GameTeam, away: GameTeam) = Game(id, "post", "STATUS_FINAL", period, "0.0", home, away)
        val results = listOf(
            LiveLine(final("5", 4, GameTeam("6", "DAL", "Mavericks", 104), GameTeam("24", "SA", "Spurs", 112)), awayFavorite = true, homeFavorite = false),
            LiveLine(final("6", 5, GameTeam("2", "BOS", "Celtics", 118), GameTeam("18", "NY", "Knicks", 121)), awayFavorite = false, homeFavorite = true),
        )
        shoot("h8_cette_nuit", UnlockState(meteo, sketch, results = results), LocalDateTime.of(2026, 10, 21, 7, 40))
    }

    /**
     * Messages reçus : WhatsApp, Discord, Claude et Messages (le quatrième ne tient plus : « et 1 autre
     * notification »), au-dessus du match en direct.
     */
    @Test
    fun messages() {
        val at = java.time.ZonedDateTime.of(2026, 10, 20, 20, 30, 0, 0, java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        fun note(pkg: String, title: String, text: String, minutesAgo: Int) =
            MessageApps.Candidate("0|$pkg|$minutesAgo", pkg, title, text, at - minutesAgo * 60_000L, clearable = true)
        val notes = MessageApps.notes(
            listOf(
                note("com.whatsapp", "Maman", "Tu viens dîner dimanche ? On fait une raclette avec les cousins", 2),
                note("com.discord", "#basket · Julien", "Wemby vient de mettre son 5e contre 😱", 9),
                note("com.anthropic.claude", "Claude", "Ta session « Nimbus » est terminée : les modifications sont poussées.", 25),
                note("com.google.android.apps.messaging", "06 12 34 56 78", "Votre colis arrive demain entre 9 h et 13 h.", 70),
                // Pas une appli de messages : jamais sur la page.
                note("com.android.vending", "Mises à jour", "3 applis mises à jour", 1),
            ),
        )
        shoot("h8_messages", UnlockState(meteo, sketch, listOf(spurs), notes = notes))
    }

    /**
     * En fond de l'écran verrouillé : sans heure, date ni lieu (l'heure et la date sont celles du Pixel, en haut
     * à gauche), la météo à droite en Google Sans, le maximum et le minimum coloriés sur le dessin.
     */
    @Test
    fun wallpaper() = shoot("h8_fond_ecran", UnlockState(meteo, sketch, listOf(spurs)), wallpaper = true)

    /** En fond d'écran, ciel et ressenti longs : le ciel laisse la gauche à la date du système. */
    @Test
    fun wallpaperLongSky() = shoot(
        "h8_fond_ecran_libelles_longs",
        UnlockState(meteo.copy(sky = "Fortes averses de neige", temperature = -3.4, feelsLike = -8.2), sketch, emptyList()),
        LocalDateTime.of(2026, 9, 30, 7, 5),
        wallpaper = true,
    )

    /** Aucun match en direct : la section « Basket » disparaît, la figure remonte et grandit. */
    @Test
    fun noLiveGame() = shoot("h8_sans_direct", UnlockState(meteo, sketch, emptyList()))

    /** Ni Météo ni match : la figure suit la date (Figure 1). */
    @Test
    fun noSections() = shoot("h8_sans_sections", UnlockState())

    /** L'explication de la figure, ouverte par le (i). */
    @Test
    fun figureInfo() = shoot("h8_figure_info", UnlockState(meteo, sketch, emptyList()), infoShown = true)

    /** L'explication la plus longue (chaîne de Steiner), sur la figure réduite par les résultats de la nuit. */
    @Test
    fun longestInfo() {
        fun final(id: String, period: Int, home: GameTeam, away: GameTeam) = Game(id, "post", "STATUS_FINAL", period, "0.0", home, away)
        val results = listOf(
            LiveLine(final("5", 4, GameTeam("6", "DAL", "Mavericks", 104), GameTeam("24", "SA", "Spurs", 112)), awayFavorite = true, homeFavorite = false),
            LiveLine(final("6", 5, GameTeam("2", "BOS", "Celtics", 118), GameTeam("18", "NY", "Knicks", 121)), awayFavorite = false, homeFavorite = true),
        )
        val steiner = DailyFigure(ALL_FIGURES.single { it.key == "steiner" }, 0)
        shoot("h8_explication_longue", UnlockState(meteo, sketch, results = results), infoShown = true, figure = steiner)
    }

    /** Un autre jour, une autre figure : samedi 24 octobre. */
    @Test
    fun otherDay() = shoot("h8_autre_figure", UnlockState(meteo, sketch, listOf(spurs)), LocalDateTime.of(2026, 10, 24, 20, 35))

    /**
     * Cas limites : lieu et ciel longs, températures négatives, trois matchs (deux favorites l'une contre
     * l'autre à la mi-temps, une fin de quart-temps, une double prolongation) ; le 1er janvier 2027 au
     * matin.
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
