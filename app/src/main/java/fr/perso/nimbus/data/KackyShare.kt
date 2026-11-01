package fr.perso.nimbus.data

import org.json.JSONObject
import java.time.Duration
import java.time.Instant

// Classement en direct du Kacky Reloaded (Trackmania, Kacky Krew) : l'API publique de kacky.gg, la même que celle
// du site. Un joueur y est classé au nombre de cartes terminées. Aucune appli perso ici : rien à partager, la
// page lit le réseau.

/** Une édition du Kacky Reloaded. */
data class KackyEvent(val id: Int, val edition: Int, val name: String, val start: Instant, val end: Instant)

/** Un bout de pseudo d'une seule couleur ([rgb] : 0xRRGGBB, null : la couleur par défaut). */
data class KackySpan(val text: String, val rgb: Int?)

/**
 * Une ligne du classement, comme sur kacky.gg : rang, évolution depuis le dernier calcul ([delta], positif =
 * gagne des places), drapeau, pseudo coloré, cartes terminées, records du monde et 2e/3e places, rang moyen.
 */
data class KackyEntry(
    val rank: Int,
    val name: List<KackySpan>,
    val finishes: Int,
    val delta: Int? = null,
    /** Drapeau du pays en emoji ; null si le pays est inconnu. */
    val flag: String? = null,
    val firsts: Int = 0,
    val seconds: Int = 0,
    val thirds: Int = 0,
    val avgRank: Double? = null,
) {
    val plainName: String get() = name.joinToString("") { it.text }
}

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
            val name = styled(p.optString("customNickname")).takeIf { it.isNotEmpty() } ?: styled(p.optString("nickname"))
            if (name.isEmpty()) return@mapNotNull null
            KackyEntry(
                rank = rank,
                name = name,
                finishes = p.optInt("finishCount"),
                delta = if (p.isNull("delta")) null else p.optInt("delta").takeIf { it != 0 },
                flag = flagOf(p.optString("zone")),
                firsts = p.optInt("worldRecords"),
                seconds = p.optInt("secondPlaces"),
                thirds = p.optInt("thirdPlaces"),
                avgRank = if (p.isNull("averageRankFinishedMaps")) null else p.optDouble("averageRankFinishedMaps"),
            )
        }.sortedBy { it.rank }.take(TOP)
        return top to (root.optJSONObject("meta")?.optInt("total") ?: 0)
    }

    /** Un nom de Trackmania sans sa mise en forme ; voir [styled]. */
    fun plain(raw: String): String = styled(raw).joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()

    /**
     * Un nom de Trackmania en bouts colorés. Couleur : « $f00 » (trois chiffres hexadécimaux, complétés par des
     * zéros s'il en manque) ; « $z » et « $g » reviennent à la couleur par défaut ; les styles (« $o », « $i »…)
     * et les liens (« $l[adresse] ») sont ôtés ; « $$ » vaut un dollar.
     */
    fun styled(raw: String): List<KackySpan> {
        val spans = mutableListOf<KackySpan>()
        val text = StringBuilder()
        var color: Int? = null
        fun flush() {
            if (text.isNotEmpty()) spans += KackySpan(text.toString(), color)
            text.clear()
        }
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '$') {
                text.append(c)
                i++
                continue
            }
            val next = raw.getOrNull(i + 1) ?: break
            i += 2
            when {
                next == '$' -> text.append('$')
                next.isHex() -> {
                    val digits = StringBuilder().append(next)
                    while (digits.length < 3 && raw.getOrNull(i)?.isHex() == true) digits.append(raw[i++])
                    while (digits.length < 3) digits.append('0')
                    flush()
                    // Chaque chiffre vaut 0x11 fois lui-même : « f » = 0xff.
                    color = digits.toString().fold(0) { acc, d -> (acc shl 8) or (d.digitToInt(16) * 17) }
                }
                next.lowercaseChar() == 'z' || next.lowercaseChar() == 'g' -> {
                    flush()
                    color = null
                }
                next.lowercaseChar() in "lhp" && raw.getOrNull(i) == '[' -> {
                    val close = raw.indexOf(']', i)
                    i = if (close < 0) raw.length else close + 1
                }
            }
        }
        flush()
        // Ni blanc de tête ni blanc de queue.
        val first = spans.firstOrNull() ?: return emptyList()
        spans[0] = first.copy(text = first.text.trimStart())
        val last = spans.last()
        spans[spans.lastIndex] = last.copy(text = last.text.trimEnd())
        return spans.filter { it.text.isNotEmpty() }
    }

    private val countries: Map<String, String> by lazy {
        val byName = java.util.Locale.getISOCountries().associateBy { java.util.Locale("", it).getDisplayCountry(java.util.Locale.ENGLISH) }
        byName + mapOf("Czech Republic" to "CZ", "Turkey" to "TR", "Türkiye" to "TR", "Russia" to "RU", "South Korea" to "KR", "Macedonia" to "MK")
    }

    /** Le drapeau en emoji du pays d'une zone Trackmania (« World|Europe|Portugal|Lisboa »), ou null. */
    fun flagOf(zone: String): String? {
        val code = countries[zone.split('|').getOrNull(2)?.trim()] ?: return null
        return String(code.flatMap { listOf(0x1F1E6 + (it - 'A')) }.toIntArray(), 0, 2)
    }

    private fun Char.isHex(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
