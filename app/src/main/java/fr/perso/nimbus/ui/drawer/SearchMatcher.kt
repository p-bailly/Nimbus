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
    )

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("ß", "ss")
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(marks, "").trim()
    }

    fun index(apps: List<AppEntry>): List<Entry> = apps.map { app ->
        val label = normalize(app.label)
        val words = label.split(separators).filter { it.isNotEmpty() }
        Entry(
            app = app,
            label = label,
            compact = words.joinToString(""),
            words = words,
            initials = words.joinToString("") { it.take(1) },
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
                e.label.startsWith(q) -> 0
                e.words.any { it.startsWith(q) } -> 1
                e.label.contains(q) || (qCompact.isNotEmpty() && e.compact.contains(qCompact)) -> 2
                q.length >= 2 && e.initials.startsWith(q) -> 3
                else -> return@mapNotNull null
            }
            score to e.app
        }.sortedBy { it.first }.map { it.second }
    }
}
