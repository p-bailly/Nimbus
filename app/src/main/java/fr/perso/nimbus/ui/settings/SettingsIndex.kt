package fr.perso.nimbus.ui.settings

import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.drawer.SearchMatcher

/** Un réglage trouvé par la recherche du tiroir : [section] est la page de réglages qui le contient. */
data class SettingsHit(val title: String, val section: SettingsSection) {
    /** « Paramètres › Page de recherche » : où le réglage se trouve. */
    val path: String get() = if (section == SettingsSection.MAIN) "Paramètres" else "Paramètres › ${SettingsIndex.pageTitle(section)}"
}

/**
 * Les réglages de Nimbus, pour les retrouver depuis la barre de recherche : leur intitulé, la page qui
 * les contient et quelques mots qui les décrivent. Un appui sur un résultat ouvre cette page.
 */
object SettingsIndex {

    private class Entry(val title: String, val section: SettingsSection, keywords: String = "") {
        val folded = SearchMatcher.normalize(title)
        val words = folded.split(WORD_SEPARATORS).filter { it.isNotEmpty() }
        val keywordsFolded = SearchMatcher.normalize(keywords)
    }

    private val WORD_SEPARATORS = Regex("[\\s\\-_.:,;'’&+/()!?«»]+")

    private val pageTitles = mapOf(
        SettingsSection.MAIN to "Paramètres",
        SettingsSection.APPEARANCE to "Apparence globale",
        SettingsSection.GESTURES to "Gestes et raccourcis",
        SettingsSection.BACKUP to "Sauvegarde",
        SettingsSection.HOME to "Page d'accueil",
        SettingsSection.DRAWER to "Page des applications",
        SettingsSection.SEARCH to "Page de recherche",
        SettingsSection.DICTATION to "Dictée pour Claude",
        SettingsSection.NOTIFICATIONS to "Notifications",
        SettingsSection.PRIVACY to "Sécurité et confidentialité",
        SettingsSection.ABOUT to "À propos",
    )

    fun pageTitle(section: SettingsSection): String = pageTitles.getValue(section)

    private val entries: List<Entry> = buildList {
        fun e(section: SettingsSection, title: String, keywords: String = "") {
            add(Entry(title, section, keywords))
        }

        // Les pages elles-mêmes.
        for ((section, title) in pageTitles) {
            if (section != SettingsSection.MAIN) e(section, title)
        }

        val appearance = SettingsSection.APPEARANCE
        e(appearance, "Thème", "mode sombre clair nuit papier latex ambiant couleurs")
        e(appearance, "Fond d'écran", "image animé wallpaper")
        e(appearance, "Forme des icônes", "rond carré squircle cercle")
        e(appearance, "Pack d'icônes", "icon pack")
        e(appearance, "Icônes à thème", "monochromes material you couleurs du thème")
        e(appearance, "Uniformiser les anciennes icônes", "adaptatives fond forme")
        e(appearance, "Taille des icônes", "petite moyenne grande")

        val gestures = SettingsSection.GESTURES
        for (gesture in Gesture.entries) e(gestures, gesture.label, "geste action")
        e(gestures, "Service « Gestes Nimbus »", "accessibilité verrouillage capture d'écran applis récentes")

        val backup = SettingsSection.BACKUP
        e(backup, "Exporter la configuration", "sauvegarde json réglages")
        e(backup, "Importer une configuration", "restaurer smart launcher slbk json")
        e(backup, "Sauvegarde automatique", "dossier backup")

        val home = SettingsSection.HOME
        e(home, "Style de l'accueil", "grille groupes voronoi ensembles fourier papier")
        e(home, "Cacher les boutons de navigation", "barre de navigation geste")
        e(home, "Couleurs des applis", "papier famille")
        e(home, "Applis de la page", "places papier")
        e(home, "Colonnes de la grille", "icônes accueil")
        e(home, "Lignes de la grille", "icônes accueil")
        e(home, "Noms sous les icônes", "libellés accueil")
        e(home, "Icônes par rangée du dock", "favoris dock")
        e(home, "Horloge et date", "heure")
        e(home, "Niveau de batterie", "pile")
        e(home, "Barre de recherche", "en bas de l'accueil")
        e(home, "Widgets", "ajouter supprimer")

        val drawer = SettingsSection.DRAWER
        e(drawer, "Table des matières", "sommaire tiroir catégories")
        e(drawer, "Mode nuit", "tiroir sombre clair")
        e(drawer, "Colonnes", "grille du tiroir")
        e(drawer, "Nom des apps", "libellés tiroir")
        e(drawer, "Tri", "alphabétique usage")
        e(drawer, "Apps fréquentes en haut", "fréquentes figure")
        e(drawer, "Catégories automatiques", "onglets")
        e(drawer, "Ajouter une catégorie", "onglet section")
        e(drawer, "Catégories d'origine", "réinitialiser")

        val search = SettingsSection.SEARCH
        e(search, "Moteur de recherche web", "google duckduckgo qwant ecosia brave startpage")
        e(search, "Rechercher dans les contacts")
        e(search, "Rechercher dans les fichiers")
        e(search, "Rechercher sur le Play Store")
        e(search, "Rechercher dans les réglages", "paramètres")
        e(search, "Entrée ouvre la première app", "touche entrée clavier")

        val dictation = SettingsSection.DICTATION
        e(dictation, "Essayer maintenant", "dictée micro")
        e(dictation, "Bulle flottante", "micro bouton")
        e(dictation, "Tuile des réglages rapides", "dictée")
        e(dictation, "Copier seulement", "presse-papiers")
        e(dictation, "Confirmer avant d'envoyer", "relire transcription")
        e(dictation, "Revenir à l'appli d'origine", "après l'envoi")
        e(dictation, "Ajouter une conversation", "session claude lien")

        val notifications = SettingsSection.NOTIFICATIONS
        e(notifications, "Bandeau papier", "réglages rapides panneau")
        e(notifications, "Pastilles de notification", "badges icônes compteur")
        e(notifications, "Style des pastilles", "nombre point badge")

        val privacy = SettingsSection.PRIVACY
        e(privacy, "Page de déverrouillage", "météo matchs écran verrouillé")
        e(privacy, "Écran verrouillé Nimbus", "fond lockscreen empreinte")
        e(privacy, "Apps masquées", "cacher code pin")
        e(privacy, "Modifier le code PIN", "mot de passe sécurité")
        e(privacy, "Apps professionnelles", "profil travail pause")
        e(privacy, "Espace privé", "private space verrouiller")
        e(privacy, "Masquer l'espace privé verrouillé")
        e(privacy, "Réglages de l'espace privé")

        val about = SettingsSection.ABOUT
        e(about, "Application d'accueil", "launcher par défaut")
        e(about, "Icônes", "licence material symbols")
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
        }.sortedBy { it.first }.take(limit).map { (_, e) -> SettingsHit(e.title, e.section) }
    }
}
