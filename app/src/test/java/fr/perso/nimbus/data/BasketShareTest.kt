package fr.perso.nimbus.data

import android.database.MatrixCursor
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Tableau d'ESPN, choix des lignes de la page de déverrouillage, lecture du fournisseur de Météo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BasketShareTest {

    private fun event(
        id: String,
        state: String,
        statusName: String,
        period: Int,
        clock: String,
        home: Triple<String, String, String>,
        away: Triple<String, String, String>,
    ): JSONObject {
        fun competitor(side: String, t: Triple<String, String, String>) = JSONObject()
            .put("homeAway", side)
            .put("score", t.third)
            .put("team", JSONObject().put("id", t.first).put("shortDisplayName", t.second).put("abbreviation", t.second.take(3).uppercase()))
        val status = JSONObject().put("period", period).put("displayClock", clock)
            .put("type", JSONObject().put("state", state).put("name", statusName))
        val comp = JSONObject()
            .put("competitors", JSONArray().put(competitor("home", home)).put(competitor("away", away)))
            .put("status", status)
        return JSONObject().put("id", id).put("competitions", JSONArray().put(comp))
    }

    private val spurs = FavoriteTeam("24", "SA", "Spurs")
    private val celtics = FavoriteTeam("2", "BOS", "Celtics")
    private val knicks = FavoriteTeam("18", "NY", "Knicks")
    private val lakers = FavoriteTeam("13", "LAL", "Lakers")

    private val board = JSONObject().put(
        "events",
        JSONArray()
            .put(event("1", "in", "STATUS_IN_PROGRESS", 3, "4:12", Triple("6", "Mavericks", "81"), Triple("24", "Spurs", "83")))
            .put(event("2", "pre", "STATUS_SCHEDULED", 0, "0.0", Triple("18", "Knicks", ""), Triple("2", "Celtics", "")))
            .put(event("3", "in", "STATUS_HALFTIME", 2, "0.0", Triple("13", "Lakers", "55"), Triple("9", "Warriors", "60")))
            .put(event("4", "post", "STATUS_FINAL", 4, "0.0", Triple("20", "76ers", "101"), Triple("1", "Hawks", "99")))
            .put(JSONObject().put("id", "5")),
    ).toString()

    @Test
    fun parsesScoreboard() {
        val games = BasketShare.parseScoreboard(board)
        assertEquals(listOf("1", "2", "3", "4"), games.map { it.id })
        val g = games[0]
        assertEquals("Spurs", g.away.name)
        assertEquals(83, g.away.score)
        assertEquals("Mavericks", g.home.name)
        assertEquals(81, g.home.score)
        assertEquals(3, g.period)
        assertTrue(g.live)
        assertNull(games[1].home.score)
    }

    @Test
    fun onlyLiveGamesOfFavorites() {
        val games = BasketShare.parseScoreboard(board)
        // Celtics : match à venir, pas de ligne ; Lakers : mi-temps, en cours.
        val lines = BasketShare.liveLines(listOf(spurs, celtics, lakers), games)
        assertEquals(listOf("1", "3"), lines.map { it.game.id })
        assertTrue(lines[0].awayFavorite)
        assertEquals(false, lines[0].homeFavorite)
        assertEquals(listOf(Seg("mi-temps")), BasketShare.status(lines[1].game))
        assertTrue(BasketShare.liveLines(listOf(celtics, knicks), games).isEmpty())
        assertTrue(BasketShare.liveLines(emptyList(), games).isEmpty())
    }

    @Test
    fun twoFavoritesInOneGameMakeOneLine() {
        val games = BasketShare.parseScoreboard(board)
        val mavs = FavoriteTeam("6", "DAL", "Mavericks")
        val lines = BasketShare.liveLines(listOf(spurs, mavs), games)
        assertEquals(1, lines.size)
        assertTrue(lines[0].awayFavorite && lines[0].homeFavorite)
    }

    @Test
    fun atMostThreeLines() {
        val events = JSONArray()
        val favorites = (1..5).map { FavoriteTeam("$it", null, null) }
        for (i in 1..5) events.put(event("g$i", "in", "STATUS_IN_PROGRESS", 1, "11:00", Triple("$i", "Home$i", "2"), Triple("x$i", "Away$i", "0")))
        val games = BasketShare.parseScoreboard(JSONObject().put("events", events).toString())
        assertEquals(listOf("g1", "g2", "g3"), BasketShare.liveLines(favorites, games).map { it.game.id })
    }

    @Test
    fun statusLabels() {
        fun game(name: String, period: Int, clock: String) =
            Game("1", "in", name, period, clock, GameTeam("1", "A", "A", 0), GameTeam("2", "B", "B", 0))
        assertEquals(listOf(Seg("3", "e"), Seg(" qt · 4:12")), BasketShare.status(game("STATUS_IN_PROGRESS", 3, "4:12")))
        assertEquals(listOf(Seg("1", "er"), Seg(" qt · 11:58")), BasketShare.status(game("STATUS_IN_PROGRESS", 1, "11:58")))
        assertEquals(listOf(Seg("fin du "), Seg("3", "e"), Seg(" qt")), BasketShare.status(game("STATUS_END_PERIOD", 3, "0.0")))
        assertEquals(listOf(Seg("mi-temps")), BasketShare.status(game("STATUS_HALFTIME", 2, "0.0")))
        assertEquals(listOf(Seg("prol. · 2:31")), BasketShare.status(game("STATUS_IN_PROGRESS", 5, "2:31")))
        assertEquals(listOf(Seg("2", "e"), Seg(" prol. · 0:45")), BasketShare.status(game("STATUS_IN_PROGRESS", 6, "0:45")))
        assertEquals(listOf(Seg("fin de la prol.")), BasketShare.status(game("STATUS_END_PERIOD", 5, "0.0")))
    }

    @Test
    fun lastNightResultsOfFavorites() {
        fun final(id: String, start: String, period: Int, home: Triple<String, String, String>, away: Triple<String, String, String>) =
            event(id, "post", "STATUS_FINAL", period, "0.0", home, away).put("date", start)
        val events = JSONArray()
            .put(event("1", "in", "STATUS_IN_PROGRESS", 3, "4:12", Triple("6", "Mavericks", "81"), Triple("24", "Spurs", "83")).put("date", "2026-10-21T00:00Z"))
            // Celtics : vainqueurs cette nuit après prolongation ; Knicks : match d'avant-hier, trop vieux.
            .put(final("2", "2026-10-20T23:30Z", 5, Triple("2", "Celtics", "112"), Triple("18", "Knicks", "108")))
            .put(final("3", "2026-10-19T00:00Z", 4, Triple("18", "Knicks", "99"), Triple("13", "Lakers", "101")))
            // Spurs : un match terminé plus tôt, mais ils jouent en ce moment : seul le direct compte.
            .put(final("4", "2026-10-19T23:00Z", 4, Triple("24", "Spurs", "120"), Triple("1", "Hawks", "100")))
        val games = BasketShare.parseScoreboard(JSONObject().put("events", events).toString())
        val now = java.time.Instant.parse("2026-10-21T06:30:00Z")
        val favorites = listOf(spurs, celtics, knicks, lakers)
        val live = BasketShare.liveLines(favorites, games)
        val results = BasketShare.resultLines(favorites, games, live, now)
        assertEquals(listOf("1"), live.map { it.game.id })
        // Celtics et Knicks dans le même match : une ligne ; Lakers : leur match a 54 h.
        assertEquals(listOf("2"), results.map { it.game.id })
        assertTrue(results[0].homeFavorite && results[0].awayFavorite)
        assertEquals(listOf(Seg("prol.")), BasketShare.status(results[0].game))
        // Deux favorites : ni victoire ni défaite, mais les Celtics (à domicile) ont gagné.
        assertEquals(null, BasketShare.favoriteWon(results[0]))
        assertEquals("2", BasketShare.winner(results[0].game)?.id)
        assertEquals(true, BasketShare.favoriteWon(results[0].copy(awayFavorite = false)))
        assertEquals(false, BasketShare.favoriteWon(results[0].copy(homeFavorite = false)))
        assertEquals("terminé après prolongation", BasketShare.spokenStatus(results[0].game))
    }

    @Test
    fun scoreDaysAreYesterdayAndTodayInNewYork() {
        val paris = ZoneId.of("Europe/Paris")
        assertEquals(
            listOf(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 20)),
            BasketShare.scoreDays(ZonedDateTime.of(2026, 10, 20, 20, 35, 0, 0, paris)),
        )
    }

    @Test
    fun newYorkDays() {
        val paris = ZoneId.of("Europe/Paris")
        // 20 h 35 à Paris = 14 h 35 à New York : la journée du 20.
        assertEquals(listOf(LocalDate.of(2026, 10, 20)), BasketShare.liveDays(ZonedDateTime.of(2026, 10, 20, 20, 35, 0, 0, paris)))
        // 7 h à Paris = 1 h à New York : les matchs de la veille peuvent encore se jouer.
        assertEquals(
            listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)),
            BasketShare.liveDays(ZonedDateTime.of(2026, 10, 21, 7, 0, 0, 0, paris)),
        )
    }

    @Test
    fun meteoRow() {
        val columns = arrayOf("place", "region", "temperature", "feels_like", "sky", "today_max", "today_min", "weather_code", "is_day", "fetched_at", "stale")
        val cursor = MatrixCursor(columns)
        assertNull(MeteoShare.fromCursor(cursor))
        cursor.addRow(arrayOf<Any?>("Morteau", "Doubs", 11.2, 8.6, "Partiellement nuageux", 16.4, 6.9, 2, 0, 1_792_000_000_000L, 0))
        val now = MeteoShare.fromCursor(cursor)!!
        assertEquals("Morteau", now.place)
        assertEquals("Doubs", now.region)
        assertEquals(11.2, now.temperature, 0.0)
        assertEquals("Partiellement nuageux", now.sky)
        assertEquals(false, now.isDay)
        assertEquals(false, now.stale)
    }
}
