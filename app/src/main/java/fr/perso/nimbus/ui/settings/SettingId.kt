package fr.perso.nimbus.ui.settings

import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.ui.SettingsSection

/**
 * Les réglages de Nimbus, une seule fois : l'intitulé affiché sur la ligne, la page et le groupe qui la
 * contiennent, et quelques mots qui la décrivent. Les pages prennent leurs intitulés ici et la recherche
 * ([SettingsIndex]) lit la même liste : un réglage ajouté à une page sans entrée ici ne se retrouve pas
 * (le test `SettingIdTest` vérifie que chaque entrée est cohérente). [name] sert de clé à la ligne dans la
 * liste : c'est elle qu'on fait défiler dans la vue et qu'on surligne quand on arrive d'une recherche.
 */
enum class SettingId(
    val section: SettingsSection,
    val title: String,
    /** Titre de section de la page sous lequel se trouve la ligne (« Icônes »). */
    val group: String? = null,
    val keywords: String = "",
) {
    // ---- Apparence
    THEME(SettingsSection.APPEARANCE, "Thème", "Thème", "mode sombre clair nuit papier latex ambiant couleurs material you"),
    ICON_SHAPE(SettingsSection.APPEARANCE, "Forme des icônes", "Icônes", "rond carré squircle cercle goutte"),
    ICON_SIZE(SettingsSection.APPEARANCE, "Taille des icônes", "Icônes", "petite moyenne grande"),
    ICON_PACK(SettingsSection.APPEARANCE, "Pack d'icônes", "Icônes", "icon pack"),
    THEMED_ICONS(SettingsSection.APPEARANCE, "Icônes à thème", "Icônes", "monochromes material you couleurs du thème"),
    WRAP_LEGACY(SettingsSection.APPEARANCE, "Uniformiser les anciennes icônes", "Icônes", "adaptatives fond forme"),
    WALLPAPER(SettingsSection.APPEARANCE, "Fond d'écran", "Fond d'écran", "image animé wallpaper"),

    // ---- Accueil
    HOME_STYLE(SettingsSection.HOME, "Style de l'accueil", "Style", "grille groupes voronoi ensembles fourier papier"),
    PAPER_COLORS(SettingsSection.HOME, "Couleurs des applis", "Applis", "papier famille sommaire"),
    PAPER_APPS(SettingsSection.HOME, "Applis de la page", "Applis", "places papier 29"),
    GRID_COLUMNS(SettingsSection.HOME, "Colonnes de la grille", "Grille d'icônes", "icônes accueil"),
    GRID_ROWS(SettingsSection.HOME, "Lignes de la grille", "Grille d'icônes", "icônes accueil"),
    DOCK_COLUMNS(SettingsSection.HOME, "Icônes par rangée du dock", "Grille d'icônes", "favoris dock colonnes"),
    GRID_LABELS(SettingsSection.HOME, "Noms sous les icônes", "Grille d'icônes", "libellés accueil"),
    SHOW_CLOCK(SettingsSection.HOME, "Horloge et date", "Affichage", "heure"),
    SHOW_BATTERY(SettingsSection.HOME, "Niveau de batterie", "Affichage", "pile"),
    SHOW_SEARCH_BAR(SettingsSection.HOME, "Barre de recherche", "Affichage", "en bas de l'accueil"),
    WIDGETS(SettingsSection.HOME, "Widgets", "Widgets", "ajouter supprimer"),
    HIDE_NAV(SettingsSection.HOME, "Cacher les boutons de navigation", "Barre de navigation", "barre de navigation geste"),

    // ---- Tiroir et recherche
    DRAWER_NIGHT(SettingsSection.DRAWER, "Mode nuit", "Tiroir", "tiroir sombre clair noir oled"),
    DRAWER_COLUMNS(SettingsSection.DRAWER, "Colonnes du tiroir", "Tiroir", "grille du tiroir"),
    DRAWER_SORT(SettingsSection.DRAWER, "Tri", "Tiroir", "alphabétique usage utilisées"),
    DRAWER_LABELS(SettingsSection.DRAWER, "Nom des apps", "Tiroir", "libellés tiroir"),
    DRAWER_FREQUENT(SettingsSection.DRAWER, "Apps fréquentes en haut", "Tiroir", "fréquentes figure"),
    DRAWER_TOC(SettingsSection.DRAWER, "Table des matières", "Tiroir", "sommaire tiroir catégories papier"),
    AUTO_CATEGORIES(SettingsSection.DRAWER, "Catégories automatiques", "Catégories", "onglets"),
    MANAGE_CATEGORIES(SettingsSection.DRAWER, "Gérer les catégories", "Catégories", "onglets renommer ordre"),
    SEARCH_ENGINE(SettingsSection.DRAWER, "Moteur de recherche web", "Recherche", "google duckduckgo qwant ecosia brave startpage"),
    SEARCH_CONTACTS(SettingsSection.DRAWER, "Rechercher dans les contacts", "Recherche"),
    SEARCH_FILES(SettingsSection.DRAWER, "Rechercher dans les fichiers", "Recherche"),
    SEARCH_PLAY(SettingsSection.DRAWER, "Rechercher sur le Play Store", "Recherche"),
    SEARCH_SETTINGS(SettingsSection.DRAWER, "Rechercher dans les réglages", "Recherche", "paramètres"),
    SEARCH_ENTER(SettingsSection.DRAWER, "Entrée ouvre la première app", "Recherche", "touche entrée clavier"),

    // ---- Catégories
    ADD_CATEGORY(SettingsSection.CATEGORIES, "Ajouter une catégorie", "Actions", "onglet section"),
    RESET_CATEGORIES(SettingsSection.CATEGORIES, "Catégories d'origine", "Actions", "réinitialiser"),

    // ---- Gestes
    GESTURE_DOUBLE_TAP(SettingsSection.GESTURES, Gesture.DOUBLE_TAP.label, "Sur l'accueil", "geste action"),
    GESTURE_SWIPE_UP(SettingsSection.GESTURES, Gesture.SWIPE_UP.label, "Sur l'accueil", "geste action"),
    GESTURE_SWIPE_DOWN(SettingsSection.GESTURES, Gesture.SWIPE_DOWN.label, "Sur l'accueil", "geste action"),
    GESTURE_TWO_FINGER_UP(SettingsSection.GESTURES, Gesture.TWO_FINGER_UP.label, "Sur l'accueil", "geste action"),
    GESTURE_TWO_FINGER_DOWN(SettingsSection.GESTURES, Gesture.TWO_FINGER_DOWN.label, "Sur l'accueil", "geste action"),
    GESTURE_PINCH_IN(SettingsSection.GESTURES, Gesture.PINCH_IN.label, "Sur l'accueil", "geste action"),
    GESTURE_PINCH_OUT(SettingsSection.GESTURES, Gesture.PINCH_OUT.label, "Sur l'accueil", "geste action"),
    GESTURE_SERVICE(
        SettingsSection.GESTURES, "Service « Gestes Nimbus »", "Service d'accessibilité",
        "accessibilité verrouillage capture d'écran applis récentes",
    ),

    // ---- Écran verrouillé
    UNLOCK_PAGE(SettingsSection.LOCKSCREEN, "Page de déverrouillage", "Page de déverrouillage", "météo matchs écran verrouillé"),
    UNLOCK_WALLPAPER(SettingsSection.LOCKSCREEN, "Écran verrouillé Nimbus", "Fond de l'écran verrouillé", "fond lockscreen empreinte"),

    // ---- Confidentialité
    HIDDEN_APPS(SettingsSection.PRIVACY, "Apps masquées", "Apps masquées", "cacher code pin"),
    PIN(SettingsSection.PRIVACY, "Modifier le code PIN", "Apps masquées", "mot de passe sécurité"),
    WORK_PROFILE(SettingsSection.PRIVACY, "Apps professionnelles", "Profil professionnel", "travail pause"),
    PRIVATE_SPACE(SettingsSection.PRIVACY, "Espace privé", "Espace privé", "private space verrouiller"),
    HIDE_PRIVATE(SettingsSection.PRIVACY, "Masquer l'espace privé verrouillé", "Espace privé"),
    PRIVATE_SETTINGS(SettingsSection.PRIVACY, "Réglages de l'espace privé", "Espace privé"),

    // ---- Dictée pour Claude
    DICTATION_TRY(SettingsSection.DICTATION, "Essayer maintenant", null, "dictée micro"),
    DICTATION_ADD(SettingsSection.DICTATION, "Ajouter une conversation", "Conversations", "session claude lien"),
    DICTATION_COPY_ONLY(SettingsSection.DICTATION, "Copier seulement", "Envoi", "presse-papiers"),
    DICTATION_CONFIRM(SettingsSection.DICTATION, "Confirmer avant d'envoyer", "Envoi", "relire transcription"),
    DICTATION_RETURN(SettingsSection.DICTATION, "Revenir à l'appli d'origine", "Envoi", "après l'envoi"),
    DICTATION_BUBBLE(SettingsSection.DICTATION, "Bulle flottante", "Lancement", "micro bouton"),
    DICTATION_TILE(SettingsSection.DICTATION, "Tuile des réglages rapides", "Lancement", "dictée"),

    // ---- Notifications
    BADGES(SettingsSection.NOTIFICATIONS, "Pastilles de notification", "Pastilles", "badges icônes compteur"),
    BADGE_STYLE(SettingsSection.NOTIFICATIONS, "Style des pastilles", "Pastilles", "nombre point badge"),
    PAPER_PANEL(SettingsSection.NOTIFICATIONS, "Bandeau papier", "Bandeau papier", "réglages rapides panneau"),

    // ---- Sauvegarde
    BACKUP_EXPORT(SettingsSection.BACKUP, "Exporter la configuration", "Fichier", "sauvegarde json réglages"),
    BACKUP_IMPORT(SettingsSection.BACKUP, "Importer une configuration", "Fichier", "restaurer smart launcher slbk json"),
    BACKUP_AUTO(SettingsSection.BACKUP, "Sauvegarde automatique", "Automatique", "dossier backup"),
    BACKUP_AUTO_OFF(SettingsSection.BACKUP, "Désactiver la sauvegarde automatique", "Automatique", "dossier backup"),

    // ---- À propos
    DEFAULT_LAUNCHER(SettingsSection.ABOUT, "Application d'accueil", null, "launcher par défaut"),
    ABOUT_ICONS(SettingsSection.ABOUT, "Icônes", null, "licence material symbols"),
    ;

    companion object {
        /** Ligne du geste [gesture] dans la page des gestes. */
        fun of(gesture: Gesture): SettingId = when (gesture) {
            Gesture.DOUBLE_TAP -> GESTURE_DOUBLE_TAP
            Gesture.SWIPE_UP -> GESTURE_SWIPE_UP
            Gesture.SWIPE_DOWN -> GESTURE_SWIPE_DOWN
            Gesture.TWO_FINGER_UP -> GESTURE_TWO_FINGER_UP
            Gesture.TWO_FINGER_DOWN -> GESTURE_TWO_FINGER_DOWN
            Gesture.PINCH_IN -> GESTURE_PINCH_IN
            Gesture.PINCH_OUT -> GESTURE_PINCH_OUT
        }
    }
}
