package fr.perso.nimbus.ui.drawer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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

    /** Jaune de surligneur (\hl de soul), pour les lettres qui ont fait trouver un résultat. */
    val Highlighter = Color(0xFFFFEE99)

    /** Une lettre sans casse ni accent, ligatures développées ; sans [normalize] : ni espaces ni rognage. */
    private fun fold(c: Char): String =
        Normalizer.normalize(c.toString().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(marks, "")
            .replace("œ", "oe").replace("æ", "ae").replace("ß", "ss")

    /**
     * [text] dont les lettres qui correspondent à [query] sont surlignées : la saisie telle quelle (sans
     * casse ni accent : « eco » dans « École »), sinon les initiales des mots (« gm » dans « Google Maps »).
     * Rien n'est surligné si la correspondance vient d'ailleurs (ancien nom d'une app, mots séparés…).
     */
    fun highlight(text: String, query: String, color: Color = Highlighter): AnnotatedString {
        val q = normalize(query)
        if (q.isEmpty() || text.isEmpty()) return AnnotatedString(text)
        // Le texte replié, et pour chacune de ses lettres l'indice de la lettre d'origine.
        val folded = StringBuilder()
        val origin = ArrayList<Int>(text.length)
        text.forEachIndexed { i, c ->
            for (f in fold(c)) {
                folded.append(f)
                origin += i
            }
        }
        val ranges = ArrayList<IntRange>()
        val at = folded.indexOf(q)
        if (at >= 0) {
            ranges += origin[at]..origin[at + q.length - 1]
        } else if (q.length >= 2) {
            // Initiales : la première lettre de chacun des mots, dans l'ordre.
            val starts = ArrayList<Int>()
            var wordStart = true
            text.forEachIndexed { i, c ->
                val separator = separators.matches(c.toString())
                if (!separator && wordStart) starts += i
                wordStart = separator
            }
            val initials = starts.joinToString("") { fold(text[it]).take(1) }
            if (initials.startsWith(q)) {
                for (k in q.indices) ranges += starts[k]..starts[k]
            }
        }
        if (ranges.isEmpty()) return AnnotatedString(text)
        return buildAnnotatedString {
            append(text)
            for (r in ranges) addStyle(SpanStyle(background = color), r.first, r.last + 1)
        }
    }
}
