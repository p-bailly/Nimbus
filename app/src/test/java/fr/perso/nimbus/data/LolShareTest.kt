package fr.perso.nimbus.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Calendrier de LoL Esports (getSchedule), choix des séries de la page de déverrouillage. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LolShareTest {

    private fun team(name: String, code: String, wins: Int? = 0): JSONObject {
        val t = JSONObject().put("name", name).put("code", code)
        return t.put("result", if (wins == null) JSONObject.NULL else JSONObject().put("gameWins", wins))
    }

    private fun event(id: String, state: String, start: String, bestOf: Int, a: JSONObject, b: JSONObject): JSONObject =
        JSONObject()
            .put("startTime", start)
            .put("state", state)
            .put("type", "match")
            .put("league", JSONObject().put("name", "Mondial").put("slug", "worlds"))
            .put(
                "match",
                JSONObject().put("id", id).put("teams", JSONArray().put(a).put(b))
                    .put("strategy", JSONObject().put("type", "bestOf").put("count", bestOf)),
            )

    private fun schedule(vararg events: JSONObject, older: String? = "abc"): String =
        JSONObject().put(
            "data",
            JSONObject().put(
                "schedule",
                JSONObject()
                    .put("pages", JSONObject().put("older", older ?: JSONObject.NULL).put("newer", JSONObject.NULL))
                    .put("events", JSONArray().apply { events.forEach { put(it) } }),
            ),
        ).toString()

    private val g2 = FavoriteTeam("G2 Esports", "G2", "G2 Esports")
    private val t1 = FavoriteTeam("T1", "T1", "T1")
    private val gen = FavoriteTeam("Gen.G", "GEN", "Gen.G")
    private val fnc = FavoriteTeam("Fnatic", "FNC", "Fnatic")

    private val board = schedule(
        event("1", "inProgress", "2026-10-24T19:00:00Z", 3, team("G2 Esports", "G2", 1), team("Los Ratones", "LOS", 0)),
        event("2", "unstarted", "2026-10-24T21:00:00Z", 1, team("T1", "T1"), team("Dplus KIA", "DK")),
        event("3", "completed", "2026-10-23T23:00:00Z", 1, team("Los Ratones", "LOS", 0), team("T1", "T1", 1)),
        event("4", "unstarted", "2026-10-25T19:00:00Z", 3, team("TBD", "TBD", null), team("TBD", "TBD", null)),
        event("5", "inProgress", "2026-10-24T20:00:00Z", 1, team("Gen.G Esports", "GEN", 0), team("Hanwha Life Esports", "HLE", 0)),
        JSONObject().put("startTime", "2026-10-24T22:00:00Z").put("state", "unstarted"),
    )

    @Test
    fun parsesSchedule() {
        val games = LolShare.parseSchedule(board)
        // La série entre deux « TBD » et l'événement sans match sont ignorés.
        assertEquals(listOf("1", "2", "3", "5"), games.map { it.id })
        val g = games[0]
        assertEquals("in", g.state)
        assertTrue(g.live)
        assertEquals("G2 Esports", g.away.name)
        assertEquals("G2", g.away.abbr)
        assertEquals(1, g.away.score)
        assertEquals(0, g.home.score)
        assertEquals(Instant.parse("2026-10-24T19:00:00Z"), g.start)
        assertEquals("pre", games[1].state)
        assertEquals("post", games[2].state)
    }

    @Test
    fun olderPageToken() {
        assertEquals("abc", LolShare.olderToken(board))
        assertNull(LolShare.olderToken(schedule(older = null)))
    }

    @Test
    fun seriesStatus() {
        val games = LolShare.parseSchedule(board)
        // BO3 à 1-0 : on joue la game 2 ; une BO1 est seulement en cours ; terminée : rien (la page dit « victoire »).
        assertEquals(listOf(Seg("game 2 · BO3")), BasketShare.status(games[0]))
        assertEquals("game 2 sur 3", BasketShare.spokenStatus(games[0]))
        assertEquals(listOf(Seg("en cours")), BasketShare.status(games[3]))
        assertTrue(BasketShare.status(games[2]).isEmpty())
    }

    @Test
    fun favoritesAreFoundByNameThenCode() {
        val games = LolShare.parseSchedule(board)
        // « G2 Esports » par son nom, « Gen.G » par son sigle (la série dit « Gen.G Esports »), Fnatic ne joue pas.
        val resolved = LolShare.resolve(listOf(g2, gen, fnc), games)
        assertEquals(listOf("g2esports", "gengesports"), resolved.map { it.id })
        assertEquals(listOf("G2", "GEN"), resolved.map { it.abbr })
    }

    @Test
    fun liveLinesOfFavorites() {
        val games = LolShare.parseSchedule(board)
        val lines = LolShare.liveLines(listOf(t1, g2, gen, fnc), games)
        // T1 : série à venir et résultat, pas de direct ; G2 et Gen.G : en direct, dans l'ordre des favoris.
        assertEquals(listOf("1", "5"), lines.map { it.game.id })
        assertTrue(lines[0].awayFavorite)
        assertEquals(false, lines[0].homeFavorite)
    }

    @Test
    fun recentResultsOfFavorites() {
        val games = LolShare.parseSchedule(board)
        val favorites = listOf(t1, g2)
        val live = LolShare.liveLines(favorites, games)
        // Le 24 à 5 h : la victoire de T1 (début le 23 à 23 h, BO1) date de moins de 12 h.
        val results = LolShare.resultLines(favorites, games, live, Instant.parse("2026-10-24T05:00:00Z"))
        assertEquals(listOf("3"), results.map { it.game.id })
        assertEquals(true, BasketShare.favoriteWon(results[0]))
        assertEquals("t1", BasketShare.winner(results[0].game)?.id)
        // Le 24 à 14 h : la série a plus de 12 h après sa fin, elle disparaît.
        assertTrue(LolShare.resultLines(favorites, games, live, Instant.parse("2026-10-24T14:00:00Z")).isEmpty())
    }

    @Test
    fun aLongSeriesStaysLongerThanABo1() {
        val bo5 = LolShare.parseSchedule(
            schedule(event("9", "completed", "2026-10-24T12:00:00Z", 5, team("G2 Esports", "G2", 3), team("Fnatic", "FNC", 1))),
        )[0]
        // Début à 12 h, au plus 5 h de jeu, puis 12 h : visible jusqu'à 05 h le lendemain.
        assertEquals(Instant.parse("2026-10-25T05:00:00Z"), BasketShare.resultUntil(bo5))
    }
}
