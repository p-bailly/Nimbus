package fr.perso.nimbus.ui.settings

import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.drawer.SearchMatcher

/**
 * Un réglage (ou une page) trouvé par la recherche : [section] est la page qui le contient, [id] la ligne à
 * montrer (null pour une page), [group] la section de la page où elle se trouve.
 */
data class SettingsHit(
    val title: String,
    val section: SettingsSection,
    val id: SettingId? = null,
    val group: String? = null,
) {
    /** « Paramètres › Accueil › Grille d'icônes » : où le réglage se trouve. */
    val path: String
        get() = buildString {
            append("Paramètres")
            if (id != null) {
                // Pour une page, le titre du résultat est déjà celui de la page.
                section.parent?.let { append(" › ").append(SettingsIndex.pageTitle(it)) }
                if (section != SettingsSection.MAIN) append(" › ").append(SettingsIndex.pageTitle(section))
                if (group != null) append(" › ").append(group)
            } else if (section.parent != null) {
                append(" › ").append(SettingsIndex.pageTitle(section.parent))
            }
        }
}

/**
 * La recherche des réglages, à partir de [SettingId] et des titres de pages : le champ de recherche des
 * paramètres et celui du tiroir lisent la même liste. Un appui sur un résultat ouvre la page et fait
 * défiler jusqu'à la ligne.
 */
object SettingsIndex {

    private class Entry(val hit: SettingsHit, keywords: String = "") {
        val folded = SearchMatcher.normalize(hit.title)
        val words = folded.split(WORD_SEPARATORS).filter { it.isNotEmpty() }
        val keywordsFolded = SearchMatcher.normalize(keywords)
    }

    private val WORD_SEPARATORS = Regex("[\\s\\-_.:,;'’&+/()!?«»]+")

    private val pageTitles = mapOf(
        SettingsSection.MAIN to "Paramètres",
        SettingsSection.APPEARANCE to "Apparence",
        SettingsSection.HOME to "Accueil",
        SettingsSection.DRAWER to "Tiroir et recherche",
        SettingsSection.GESTURES to "Gestes et raccourcis",
        SettingsSection.LOCKSCREEN to "Écran verrouillé",
        SettingsSection.PRIVACY to "Confidentialité",
        SettingsSection.DICTATION to "Dictée pour Claude",
        SettingsSection.NOTIFICATIONS to "Notifications",
        SettingsSection.BACKUP to "Sauvegarde",
        SettingsSection.ABOUT to "À propos",
        SettingsSection.HOME_APPS to "Applis de la page",
        SettingsSection.CATEGORIES to "Catégories",
    )

    /** Mots qui décrivent une page, pour la retrouver. */
    private val pageKeywords = mapOf(
        SettingsSection.HOME_APPS to "places papier 29 applis",
        SettingsSection.CATEGORIES to "onglets tiroir renommer",
        SettingsSection.DRAWER to "recherche moteur tiroir apps",
    )

    fun pageTitle(section: SettingsSection): String = pageTitles.getValue(section)

    private val entries: List<Entry> = buildList {
        for ((section, title) in pageTitles) {
            if (section != SettingsSection.MAIN) add(Entry(SettingsHit(title, section), pageKeywords[section].orEmpty()))
        }
        for (id in SettingId.entries) add(Entry(SettingsHit(id.title, id.section, id, id.group), id.keywords))
    }

    /** Réglages dont l'intitulé ou la description contient [query] ; deux lettres au moins. */
    fun search(query: String, limit: Int = 24): List<SettingsHit> {
        val q = SearchMatcher.normalize(query)
        if (q.length < 2) return emptyList()
        return entries.mapNotNull { e ->
            val score = when {
                e.folded.startsWith(q) -> 0
                e.words.any { it.startsWith(q) } -> 1
                e.folded.contains(q) -> 2
                e.keywordsFolded.contains(q) -> 3
                else -> return@mapNotNull null
            }
            score to e
        }.sortedBy { it.first }.take(limit).map { (_, e) -> e.hit }
    }
}
