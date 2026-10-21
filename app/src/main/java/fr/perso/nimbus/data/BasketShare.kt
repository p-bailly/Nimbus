package fr.perso.nimbus.data

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// Matchs en direct, et résultats de la nuit, des équipes favorites de l'appli Basket Live : les favoris viennent de son
// fournisseur (content://com.pbailly.superfanbasket.nimbus), les scores de l'API publique d'ESPN,
// la même que l'appli.

/** Une équipe favorite de Basket Live (identifiant ESPN). */
data class FavoriteTeam(val id: String, val abbr: String?, val name: String?)

/** Une équipe dans un match : nom court (« Spurs »), score (null avant le match). */
data class GameTeam(val id: String, val abbr: String, val name: String, val score: Int?)

/** Un match du tableau d'ESPN. */
data class Game(
    val id: String,
    /** « pre », « in » ou « post ». */
    val state: String,
    /** STATUS_IN_PROGRESS, STATUS_HALFTIME, STATUS_END_PERIOD… */
    val statusName: String,
    val period: Int,
    val displayClock: String,
    val home: GameTeam,
    val away: GameTeam,
    /** Coup d'envoi (null s'il manque au tableau). */
    val start: Instant? = null,
) {
    val live: Boolean get() = state == "in"
    val finished: Boolean get() = state == "post"
}

/** Une ligne de la page : le match, et les équipes favorites (en gras pendant le direct). */
data class LiveLine(val game: Game, val awayFavorite: Boolean, val homeFavorite: Boolean)

object BasketShare {
    const val PACKAGE = "com.pbailly.superfanbasket"
    val URI: Uri = Uri.parse("content://com.pbailly.superfanbasket.nimbus")
    private val FAVORITES: Uri = URI.buildUpon().appendPath("favorites").build()

    private const val SCOREBOARD = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates="
    private val NEW_YORK: ZoneId = ZoneId.of("America/New_York")
    private val YMD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /** Au plus trois matchs en direct, et quatre lignes en tout avec les résultats : la page reste courte. */
    const val MAX_LINES = 3
    const val MAX_TOTAL = 4

    /** Un résultat de plus de 36 h n'est plus « de la nuit ». */
    private val RESULT_AGE: Duration = Duration.ofHours(36)

    /**
     * Les équipes favorites, dans l'ordre de l'appli ; null si Basket Live n'est pas installée (ou trop
     * ancienne). Lève une exception si le fournisseur répond mal.
     */
    fun readFavorites(resolver: ContentResolver): List<FavoriteTeam>? =
        resolver.query(FAVORITES, arrayOf("team_id", "abbr", "name"), null, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow("team_id")
            val abbr = c.getColumnIndexOrThrow("abbr")
            val name = c.getColumnIndexOrThrow("name")
            buildList {
                while (c.moveToNext()) {
                    val team = c.getString(id)?.takeIf { it.isNotBlank() } ?: continue
                    add(FavoriteTeam(team, c.getString(abbr), c.getString(name)))
                }
            }
        }

    /**
     * Journées d'ESPN (heure de New York) qui peuvent avoir un match en cours : aujourd'hui, et hier
     * avant 6 h (un match du soir finit après minuit).
     */
    fun liveDays(now: ZonedDateTime): List<LocalDate> {
        val ny = now.withZoneSameInstant(NEW_YORK)
        val today = ny.toLocalDate()
        return if (ny.hour < 6) listOf(today.minusDays(1), today) else listOf(today)
    }

    /**
     * Journées à lire pour le direct et les résultats de la nuit : hier et aujourd'hui à New York (les
     * matchs du soir américain se jouent la nuit en France).
     */
    fun scoreDays(now: ZonedDateTime): List<LocalDate> {
        val today = now.withZoneSameInstant(NEW_YORK).toLocalDate()
        return listOf(today.minusDays(1), today)
    }

    /** Le tableau des matchs d'une journée d'ESPN (réseau : hors du fil principal). */
    fun fetchScoreboard(day: LocalDate): List<Game> {
        val connection = URL(SCOREBOARD + day.format(YMD)).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "ESPN $code" }
            val body = connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    check(out.size() <= MAX_BYTES) { "Réponse trop grosse" }
                }
                out.toString("UTF-8")
            }
            return parseScoreboard(body)
        } finally {
            connection.disconnect()
        }
    }

    private const val MAX_BYTES = 8 * 1024 * 1024

    /** Les matchs d'un tableau d'ESPN (comme `parseEvent` de Basket Live) ; un match mal formé est ignoré. */
    fun parseScoreboard(json: String): List<Game> {
        val events = JSONObject(json).optJSONArray("events") ?: return emptyList()
        return (0 until events.length()).mapNotNull { i -> events.optJSONObject(i)?.let(::parseEvent) }
    }

    private fun parseEvent(e: JSONObject): Game? {
        val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: return null
        val competitors = comp.optJSONArray("competitors") ?: return null
        val teams = (0 until competitors.length()).mapNotNull { competitors.optJSONObject(it) }
        val home = teams.firstOrNull { it.optString("homeAway") == "home" } ?: teams.getOrNull(0) ?: return null
        val away = teams.firstOrNull { it.optString("homeAway") == "away" } ?: teams.getOrNull(1) ?: return null
        val status = comp.optJSONObject("status") ?: e.optJSONObject("status") ?: return null
        val type = status.optJSONObject("type") ?: JSONObject()
        return Game(
            id = e.optString("id"),
            state = type.optString("state", "pre"),
            statusName = type.optString("name"),
            period = status.optInt("period", 0),
            displayClock = status.optString("displayClock"),
            home = parseTeam(home) ?: return null,
            away = parseTeam(away) ?: return null,
            // « 2026-10-21T23:30Z » : les secondes manquent souvent.
            start = runCatching { OffsetDateTime.parse(e.optString("date")).toInstant() }.getOrNull(),
        )
    }

    private fun parseTeam(c: JSONObject): GameTeam? {
        val t = c.optJSONObject("team") ?: return null
        val id = t.optString("id").takeIf { it.isNotBlank() } ?: return null
        val abbr = t.optString("abbreviation")
        val name = t.optString("shortDisplayName").ifBlank { t.optString("name") }.ifBlank { abbr }
        // Le score arrive en texte (« 83 ») ; vide avant le match.
        val score = when (val s = c.opt("score")) {
            is Number -> s.toInt()
            is String -> s.trim().toDoubleOrNull()?.toInt()
            is JSONObject -> s.optString("value").toDoubleOrNull()?.toInt()
            else -> null
        }
        return GameTeam(id, abbr, name, score)
    }

    /**
     * Les lignes de la page : pour chaque favorite, dans l'ordre, son match en cours (mi-temps et fin
     * de période comprises) ; un match entre deux favorites n'apparaît qu'une fois ; [MAX_LINES] au plus.
     */
    fun liveLines(favorites: List<FavoriteTeam>, games: List<Game>): List<LiveLine> {
        val ids = favorites.map { it.id }.toSet()
        val lines = mutableListOf<LiveLine>()
        for (fav in favorites) {
            if (lines.size == MAX_LINES) break
            val game = games.firstOrNull { it.live && (it.home.id == fav.id || it.away.id == fav.id) } ?: continue
            if (lines.any { it.game.id == game.id }) continue
            lines += LiveLine(game, awayFavorite = game.away.id in ids, homeFavorite = game.home.id in ids)
        }
        return lines
    }

    /**
     * Les résultats de la nuit : pour chaque favorite, dans l'ordre, son dernier match terminé depuis
     * moins de 36 h (coup d'envoi), s'il ne joue pas en ce moment ; un match entre deux favorites n'apparaît
     * qu'une fois ; de quoi compléter [live] jusqu'à [MAX_TOTAL] lignes.
     */
    fun resultLines(favorites: List<FavoriteTeam>, games: List<Game>, live: List<LiveLine>, now: Instant): List<LiveLine> {
        val ids = favorites.map { it.id }.toSet()
        val playing = live.flatMap { listOf(it.game.home.id, it.game.away.id) }.toSet()
        val done = games.filter { g -> g.finished && g.start?.let { Duration.between(it, now) < RESULT_AGE } != false }
            .sortedByDescending { it.start }
        val lines = mutableListOf<LiveLine>()
        for (fav in favorites) {
            if (live.size + lines.size >= MAX_TOTAL) break
            if (fav.id in playing) continue
            val game = done.firstOrNull { it.home.id == fav.id || it.away.id == fav.id } ?: continue
            if (lines.any { it.game.id == game.id }) continue
            lines += LiveLine(game, awayFavorite = game.away.id in ids, homeFavorite = game.home.id in ids)
        }
        return lines
    }

    /** Équipe gagnante d'un match terminé (null avant la fin, ou à égalité). */
    fun winner(game: Game): GameTeam? {
        if (!game.finished) return null
        val home = game.home.score ?: return null
        val away = game.away.score ?: return null
        return when {
            home > away -> game.home
            away > home -> game.away
            else -> null
        }
    }

    /**
     * Pour un match terminé avec une seule favorite : true si elle a gagné, false si elle a perdu ; null
     * sinon (match en cours, deux favorites l'une contre l'autre, score inconnu).
     */
    fun favoriteWon(line: LiveLine): Boolean? {
        if (line.awayFavorite == line.homeFavorite) return null
        val winner = winner(line.game) ?: return null
        return (winner.id == line.game.home.id) == line.homeFavorite
    }

    /**
     * État d'un match, en morceaux (l'ordinal en exposant) : en cours, « 3ᵉ qt · 4:12 », « mi-temps »,
     * « fin du 3ᵉ qt », « prol. · 2:31 », « 2ᵉ prol. · 0:45 » ; terminé, seulement la prolongation
     * (« prol. », « 2 prol. »), rien sinon : la page dit plutôt qui a gagné.
     */
    fun status(game: Game): List<Seg> {
        val p = game.period
        val clock = game.displayClock.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()
        fun ordinal(n: Int) = Seg("$n", if (n == 1) "er" else "e")
        return when {
            game.finished -> when {
                p > 5 -> listOf(Seg("${p - 4} prol."))
                p == 5 -> listOf(Seg("prol."))
                else -> emptyList()
            }
            game.statusName == "STATUS_HALFTIME" -> listOf(Seg("mi-temps"))
            p <= 0 -> listOf(Seg("en cours"))
            game.statusName == "STATUS_END_PERIOD" -> when {
                p <= 4 -> listOf(Seg("fin du "), ordinal(p), Seg(" qt"))
                p == 5 -> listOf(Seg("fin de la prol."))
                else -> listOf(Seg("fin de la "), ordinal(p - 4), Seg(" prol."))
            }
            p <= 4 -> listOf(ordinal(p), Seg(" qt$clock"))
            p == 5 -> listOf(Seg("prol.$clock"))
            else -> listOf(ordinal(p - 4), Seg(" prol.$clock"))
        }
    }

    /** Pour l'accessibilité : « 3e quart-temps, 4:12 à jouer ». */
    fun spokenStatus(game: Game): String {
        val p = game.period
        val clock = game.displayClock.trim().takeIf { it.isNotEmpty() }?.let { ", $it à jouer" }.orEmpty()
        fun nth(n: Int) = if (n == 1) "1er" else "${n}e"
        return when {
            game.finished -> if (p > 4) "terminé après prolongation" else "terminé"
            game.statusName == "STATUS_HALFTIME" -> "mi-temps"
            p <= 0 -> "en cours"
            game.statusName == "STATUS_END_PERIOD" -> if (p <= 4) "fin du ${nth(p)} quart-temps" else "fin de la prolongation"
            p <= 4 -> "${nth(p)} quart-temps$clock"
            else -> "prolongation$clock"
        }
    }
}

