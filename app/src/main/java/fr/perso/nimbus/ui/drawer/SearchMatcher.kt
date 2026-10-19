package fr.perso.nimbus.ui.drawer

import fr.perso.nimbus.data.AppEntry
import java.text.Normalizer
import java.util.Locale

/**
 * Recherche d'apps insensible à la casse et aux accents :
 * « eco » trouve « Écoles », « oeuvre » trouve « Œuvres ».
 */
object SearchMatcher {

    private val marks = Regex("\\p{Mn}+")
    private val separators = Regex("[\\s\\-_.:,;'’&+/()!?]+")

    class Entry(
        val app: AppEntry,
        val label: String,
        val compact: String,
        val words: List<String>,
        val initials: String,
        /** Nom d'origine normalisé d'une app renommée. */
        val alias: String? = null,
    )

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("ß", "ss")
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(marks, "").trim()
    }

    /** [aliases] : clé d'app -> nom d'origine, pour retrouver une app renommée par son ancien nom. */
    fun index(apps: List<AppEntry>, aliases: Map<String, String> = emptyMap()): List<Entry> = apps.map { app ->
        val label = normalize(app.label)
        val alias = aliases[app.key]?.let(::normalize)
        val words = (label.split(separators) + alias?.split(separators).orEmpty()).filter { it.isNotEmpty() }
        Entry(
            app = app,
            label = label,
            compact = label.split(separators).joinToString(""),
            words = words,
            initials = label.split(separators).filter { it.isNotEmpty() }.joinToString("") { it.take(1) },
            alias = alias,
        )
    }

    /**
     * Ordre des résultats : début du nom, puis début d'un mot, puis n'importe où,
     * puis initiales (« gm » -> « Google Maps »). À score égal, l'ordre du tiroir est conservé.
     */
    fun search(index: List<Entry>, query: String): List<AppEntry> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val qCompact = q.split(separators).joinToString("")
        return index.mapNotNull { e ->
            val score = when {
                e.label.startsWith(q) || e.alias?.startsWith(q) == true -> 0
                e.words.any { it.startsWith(q) } -> 1
                e.label.contains(q) || e.alias?.contains(q) == true ||
                    (qCompact.isNotEmpty() && e.compact.contains(qCompact)) -> 2
                q.length >= 2 && e.initials.startsWith(q) -> 3
                else -> return@mapNotNull null
            }
            score to e.app
        }.sortedBy { it.first }.map { it.second }
    }
}
