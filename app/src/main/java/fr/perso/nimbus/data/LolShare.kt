package fr.perso.nimbus.data

import android.content.ContentResolver
import android.net.Uri
import org.json.JSONObject
import java.text.Normalizer
import java.time.Duration
import java.time.Instant

// Séries en direct, et résultats récents, des équipes favorites de l'appli LoL Live : les favoris viennent de son
// fournisseur (content://com.pbailly.riftlive.nimbus), les séries du calendrier de lolesports.com, la même API que
// l'appli. Les types sont ceux de Basket Live (Game, LiveLine) : une série est un « match » dont le score est le
// nombre de games gagnées.

object LolShare {
    const val PACKAGE = "com.pbailly.riftlive"
    val URI: Uri = Uri.parse("content://com.pbailly.riftlive.nimbus")
    private val FAVORITES: Uri = URI.buildUpon().appendPath("favorites").build()

    private const val SCHEDULE = "https://esports-api.lolesports.com/persisted/gw/getSchedule?hl=fr-FR"

    /** Clé publique du site lolesports.com (celle que son code envoie lui-même) : l'API la réclame. */
    private const val API_KEY = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z"

    /** Pages du calendrier lues au plus : la première contient d'ordinaire « maintenant », la suivante comble un trou. */
    private const val MAX_PAGES = 2

    /** Les équipes favorites, dans l'ordre de l'appli ; null si LoL Live n'est pas installée (ou trop ancienne). */
    fun readFavorites(resolver: ContentResolver): List<FavoriteTeam>? = BasketShare.readFavorites(resolver, FAVORITES)

    /**
     * Les séries autour de maintenant : la page du calendrier qui les contient, et la précédente s'il faut remonter
     * jusqu'aux résultats encore montrés (réseau : hors du fil principal).
     */
    fun fetchGames(now: Instant): List<Game> {
        val games = mutableListOf<Game>()
        var token: String? = null
        repeat(MAX_PAGES) {
            val url = if (token == null) SCHEDULE else "$SCHEDULE&pageToken=" + Uri.encode(token)
            val body = BasketShare.fetchText(url, mapOf("x-api-key" to API_KEY))
            val page = parseSchedule(body)
            games += page
            token = olderToken(body)
            // Rien de plus ancien à chercher : la page remonte déjà plus loin que la durée d'un résultat.
            val first = page.mapNotNull { it.start }.minOrNull()
            if (token == null || (first != null && first < now.minus(WINDOW))) return games
        }
        return games
    }

    /** Un résultat plus vieux que cela ne se montre plus : inutile de remonter plus loin dans le calendrier. */
    private val WINDOW: Duration = Duration.ofHours(36)

    /** Jeton de la page précédente du calendrier, ou null. */
    fun olderToken(json: String): String? {
        val pages = JSONObject(json).optJSONObject("data")?.optJSONObject("schedule")?.optJSONObject("pages") ?: return null
        return if (pages.isNull("older")) null else pages.optString("older").takeIf { it.isNotBlank() }
    }

    /** Les séries d'une page du calendrier ; une série sans équipes connues (« TBD ») ou mal formée est ignorée. */
    fun parseSchedule(json: String): List<Game> {
        val events = JSONObject(json).optJSONObject("data")?.optJSONObject("schedule")?.optJSONArray("events") ?: return emptyList()
        return (0 until events.length()).mapNotNull { events.optJSONObject(it)?.let(::parseEvent) }
    }

    private fun parseEvent(e: JSONObject): Game? {
        val match = e.optJSONObject("match") ?: return null
        val raw = match.optJSONArray("teams") ?: return null
        if (raw.length() != 2) return null
        val teams = (0 until 2).map { parseTeam(raw.optJSONObject(it) ?: return null) ?: return null }
        val state = when (e.optString("state")) {
            "unstarted" -> "pre"
            "inProgress" -> "in"
            "completed" -> "post"
            else -> return null
        }
        val bestOf = (match.optJSONObject("strategy")?.optInt("count", 1) ?: 1).coerceAtLeast(1)
        val played = teams.sumOf { it.score ?: 0 }
        // Série en cours : « game 2 · BO3 » (la game suivante entre deux games) ; une BO1 est seulement « en cours ».
        val game = (played + 1).coerceAtMost(bestOf)
        val label = when {
            state != "in" -> emptyList()
            bestOf == 1 -> listOf(Seg("en cours"))
            else -> listOf(Seg("game $game · BO$bestOf"))
        }
        val spoken = when {
            state == "post" -> "terminée"
            state == "pre" -> "à venir"
            bestOf == 1 -> "en cours"
            else -> "game $game sur $bestOf"
        }
        return Game(
            id = match.optString("id"),
            state = state,
            statusName = e.optString("state"),
            period = 0,
            displayClock = "",
            // La première équipe de la série à gauche (« à l'extérieur » du tableau), la seconde à droite.
            away = teams[0],
            home = teams[1],
            start = runCatching { Instant.parse(e.optString("startTime")) }.getOrNull(),
            label = label,
            spoken = spoken,
            // Une game dure environ une heure, pauses comprises : la série, au plus bestOf heures.
            length = Duration.ofHours(bestOf.toLong()),
        )
    }

    private fun parseTeam(t: JSONObject): GameTeam? {
        val name = t.optString("name").trim()
        val code = t.optString("code").trim().ifEmpty { name }
        if (name.isEmpty() || code.equals("TBD", ignoreCase = true)) return null
        val score = t.optJSONObject("result")?.takeIf { !it.isNull("gameWins") }?.optInt("gameWins")
        return GameTeam(id = key(name), abbr = code, name = name, score = score)
    }

    /**
     * Les favorites présentes dans [games], avec pour identifiant celui de leur équipe dans les séries (de quoi
     * réutiliser les choix de lignes de Basket Live) ; une favorite qui ne joue pas est écartée. Une équipe se
     * reconnaît à son nom (« G2 Esports »), sinon à son sigle (« G2 »), sans tenir compte des accents ni de la casse.
     */
    fun resolve(favorites: List<FavoriteTeam>, games: List<Game>): List<FavoriteTeam> {
        val teams = games.flatMap { listOf(it.away, it.home) }.distinctBy { it.id }
        return favorites.mapNotNull { fav ->
            val names = listOfNotNull(fav.id, fav.name).map(::key).filter { it.isNotEmpty() }
            val abbr = fav.abbr?.let(::key)?.takeIf { it.isNotEmpty() }
            val team = teams.firstOrNull { it.id in names } ?: abbr?.let { a -> teams.firstOrNull { key(it.abbr) == a } }
            team?.let { FavoriteTeam(it.id, fav.abbr, fav.name) }
        }
    }

    /** Les séries en cours des favorites, comme [BasketShare.liveLines]. */
    fun liveLines(favorites: List<FavoriteTeam>, games: List<Game>): List<LiveLine> =
        BasketShare.liveLines(resolve(favorites, games), games)

    /** Les résultats récents des favorites, comme [BasketShare.resultLines]. */
    fun resultLines(favorites: List<FavoriteTeam>, games: List<Game>, live: List<LiveLine>, now: Instant): List<LiveLine> =
        BasketShare.resultLines(resolve(favorites, games), games, live, now)

    private val marks = Regex("\\p{Mn}+")
    private val others = Regex("[^a-z0-9]+")

    /** Nom ramené à ses lettres et chiffres, en minuscules et sans accents : « Gen.G » et « GEN G » se valent. */
    private fun key(text: String): String =
        others.replace(marks.replace(Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD), ""), "")
}
