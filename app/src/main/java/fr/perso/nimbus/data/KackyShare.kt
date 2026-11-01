package fr.perso.nimbus.data

import org.json.JSONObject
import java.time.Duration
import java.time.Instant

// Classement en direct du Kacky Reloaded (Trackmania, Kacky Krew) : l'API publique de kacky.gg, la même que celle
// du site. Un joueur y est classé au nombre de cartes terminées. Aucune appli perso ici : rien à partager, la
// page lit le réseau.

/** Une édition du Kacky Reloaded. */
data class KackyEvent(val id: Int, val edition: Int, val name: String, val start: Instant, val end: Instant)

/** Une ligne du classement. */
data class KackyEntry(val rank: Int, val name: String, val finishes: Int)

/** L'édition en cours (ou finie depuis peu), son top et le nombre de joueurs classés. */
data class KackyStanding(val event: KackyEvent, val top: List<KackyEntry>, val players: Int) {
    fun live(now: Instant): Boolean = now < event.end
}

object KackyShare {
    private const val API = "https://api.kacky.gg/v1"

    /** Le Kacky Reloaded (« KR ») ; les éditions Xtreme et les « hunting » ne sont pas concernées. */
    private const val TYPE = "KR"

    /** Un classement final reste affiché ce temps après la fin de l'événement. */
    private val AFTER: Duration = Duration.ofHours(12)

    const val TOP = 3

    /** La page de l'événement sur kacky.gg. */
    fun eventUrl(event: KackyEvent): String = "https://kacky.gg/event/${event.id}"

    /** L'édition en cours, ou null (réseau : hors du fil principal). */
    fun fetchEvent(now: Instant): KackyEvent? = parseEvent(BasketShare.fetchText("$API/events"), now)

    /** Le haut du classement de [event] (réseau : hors du fil principal). */
    fun fetchStanding(event: KackyEvent): KackyStanding {
        val (top, players) = parseTop(BasketShare.fetchText("$API/leaderboard?eventId=${event.id}&limit=$TOP"))
        return KackyStanding(event, top, players)
    }

    /** Le Kacky Reloaded commencé et pas fini depuis plus de [AFTER] (le plus récent s'il y en a plusieurs). */
    fun parseEvent(json: String, now: Instant): KackyEvent? {
        val data = JSONObject(json).optJSONArray("data") ?: return null
        return (0 until data.length()).mapNotNull { data.optJSONObject(it) }
            .filter { it.optString("eventType") == TYPE }
            .mapNotNull { e ->
                val start = runCatching { Instant.parse(e.optString("startTime")) }.getOrNull() ?: return@mapNotNull null
                val end = runCatching { Instant.parse(e.optString("endTime")) }.getOrNull() ?: return@mapNotNull null
                if (now < start || now > end.plus(AFTER)) return@mapNotNull null
                val edition = e.optInt("edition")
                val name = plain(e.optString("name")).ifEmpty { "Kacky Reloaded $edition" }
                KackyEvent(e.optInt("id"), edition, name, start, end)
            }
            .maxByOrNull { it.start }
    }

    /** Les premières lignes d'une page du classement, et le nombre de joueurs classés. */
    fun parseTop(json: String): Pair<List<KackyEntry>, Int> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data")
        val top = (0 until (data?.length() ?: 0)).mapNotNull { data?.optJSONObject(it) }.mapNotNull { p ->
            val rank = p.optInt("rank", 0).takeIf { it > 0 } ?: return@mapNotNull null
            // Le pseudo coloré du joueur, sinon son pseudo Trackmania.
            val name = plain(p.optString("customNickname")).ifEmpty { plain(p.optString("nickname")) }
            if (name.isEmpty()) null else KackyEntry(rank, name, p.optInt("finishCount"))
        }.sortedBy { it.rank }.take(TOP)
        return top to (root.optJSONObject("meta")?.optInt("total") ?: 0)
    }

    /**
     * Un nom de Trackmania sans sa mise en forme : couleurs (« $f00 », jusqu'à trois chiffres hexadécimaux),
     * styles (« $o », « $i »…), liens (« $l[adresse] ») ; « $$ » vaut un dollar.
     */
    fun plain(raw: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '$') {
                out.append(c)
                i++
                continue
            }
            val next = raw.getOrNull(i + 1) ?: break
            i += 2
            when {
                next == '$' -> out.append('$')
                next.isHex() -> {
                    var digits = 1
                    while (digits < 3 && raw.getOrNull(i)?.isHex() == true) {
                        i++
                        digits++
                    }
                }
                next.lowercaseChar() in "lhp" && raw.getOrNull(i) == '[' -> {
                    val close = raw.indexOf(']', i)
                    i = if (close < 0) raw.length else close + 1
                }
            }
        }
        return out.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun Char.isHex(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
