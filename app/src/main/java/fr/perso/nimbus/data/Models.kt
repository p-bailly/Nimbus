package fr.perso.nimbus.data

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap

/** Catégories du tiroir d'applications (l'ordre ici = l'ordre des onglets). */
enum class AppCategory(val label: String) {
    COMMUNICATION("Communication"),
    SOCIAL("Social"),
    INTERNET("Internet"),
    MEDIA("Multimédia"),
    PHOTO("Photo"),
    GAMES("Jeux"),
    PRODUCTIVITY("Productivité"),
    SHOPPING("Achats & Finance"),
    TRAVEL("Voyage & Cartes"),
    NEWS("Actualités & Lecture"),
    HEALTH("Santé & Sport"),
    TOOLS("Outils"),
    /** Jamais attribuée automatiquement : choix manuel ou import Smart Launcher (« Paramètres »). */
    SYSTEM("Système"),
    OTHER("Autres"),
}

/** Profil Android auquel appartient une app. */
enum class ProfileType {
    MAIN,
    /** Profil professionnel (ou autre profil secondaire). */
    WORK,
    /** Espace privé (Android 15+). */
    PRIVATE,
}

/** État d'un profil secondaire : profil pro ou espace privé. */
data class ProfileState(
    val user: UserHandle,
    val type: ProfileType,
    /** Profil pro en pause, ou espace privé verrouillé. */
    val quiet: Boolean,
    /** Espace privé : réglage système « masquer quand il est verrouillé » (Android 16+). */
    val hiddenWhenLocked: Boolean = false,
)

/** Une entrée lançable du tiroir (une activité d'une app, pour un profil utilisateur). */
data class AppEntry(
    /** Clé unique et stable : "package/classe#numéroDeProfil". */
    val key: String,
    val packageName: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val label: String,
    val icon: ImageBitmap,
    val autoCategory: AppCategory,
    val category: AppCategory = autoCategory,
    val isWorkProfile: Boolean = false,
    val profile: ProfileType = if (isWorkProfile) ProfileType.WORK else ProfileType.MAIN,
    /** App d'un profil pro en pause : affichée grisée, la lancer propose de réactiver le profil. */
    val isPaused: Boolean = false,
)

enum class ThemeMode(val label: String) {
    AMBIENT("Ambiant (couleurs du fond d'écran)"),
    MATERIAL_YOU("Material You (Android 12+)"),
    DARK("Sombre"),
    LIGHT("Clair"),
}

/** Couleurs du tiroir d'apps : celles du lanceur, ou sombres quel que soit le thème. */
enum class DrawerTheme(val label: String) {
    FOLLOW("Comme le lanceur"),
    NIGHT("Mode nuit"),
    BLACK("Mode nuit, noir pur (OLED)"),
}

enum class IconShape(val label: String) {
    SQUIRCLE("Squircle"),
    CIRCLE("Cercle"),
    ROUNDED("Carré arrondi"),
    TEARDROP("Goutte"),
    SQUARE("Carré"),
    SYSTEM("Forme du système"),
}

enum class IconSize(val label: String, val dp: Int) {
    SMALL("Petite", 44),
    MEDIUM("Moyenne", 52),
    LARGE("Grande", 60),
}

enum class SortMode(val label: String) {
    ALPHA("Alphabétique"),
    USAGE("Les plus utilisées d'abord"),
}

enum class GestureAction(val label: String) {
    NONE("Aucune action"),
    LOCK("Verrouiller l'écran"),
    NOTIFICATIONS("Ouvrir les notifications"),
    SEARCH("Ouvrir la recherche"),
    DRAWER("Ouvrir le tiroir d'apps"),
    SETTINGS("Paramètres Nimbus"),
    QUICK_SETTINGS("Réglages rapides"),
    RECENTS("Applis récentes"),
    SCREENSHOT("Capture d'écran"),
    POWER_MENU("Menu Marche/Arrêt"),
    ASSISTANT("Assistant"),
    WIDGETS("Ajouter un widget"),
    DICTATION("Dictée pour Claude"),
    APP("Ouvrir une app…"),
}

/** Gestes reconnus sur l'accueil. */
enum class Gesture(val label: String) {
    DOUBLE_TAP("Double tap"),
    SWIPE_UP("Glisser vers le haut"),
    SWIPE_DOWN("Glisser vers le bas"),
    TWO_FINGER_UP("Glisser à deux doigts vers le haut"),
    TWO_FINGER_DOWN("Glisser à deux doigts vers le bas"),
    PINCH_IN("Pincer"),
    PINCH_OUT("Écarter deux doigts"),
}

enum class BadgeStyle(val label: String) {
    COUNT("Nombre de notifications"),
    DOT("Simple point"),
}

/** Moteur de la recherche web ; [urlTemplate] null = appli de recherche du téléphone. */
enum class SearchEngine(val label: String, val urlTemplate: String?) {
    SYSTEM("Appli de recherche du téléphone", null),
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    QWANT("Qwant", "https://www.qwant.com/?q=%s"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s"),
    STARTPAGE("Startpage", "https://www.startpage.com/do/search?query=%s"),
}

data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.AMBIENT,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val iconPack: String? = null,
    val wrapLegacyIcons: Boolean = true,
    val iconSize: IconSize = IconSize.MEDIUM,
    val drawerColumns: Int = 4,
    val showLabels: Boolean = true,
    val sortMode: SortMode = SortMode.ALPHA,
    val autoCategories: Boolean = true,
    val showClock: Boolean = true,
    val showBattery: Boolean = true,
    val badgesEnabled: Boolean = true,
    val searchContacts: Boolean = true,
    /** Fichiers du téléphone dans les résultats (accès à tous les fichiers requis). */
    val searchFiles: Boolean = true,
    /** Ligne « Rechercher sur le Play Store » dans les résultats. */
    val searchPlayStore: Boolean = true,
    val drawerTheme: DrawerTheme = DrawerTheme.FOLLOW,
    /** Conversations Claude (Claude Code…) où l'on peut envoyer la dictée. */
    val claudeSessions: List<ClaudeSession> = emptyList(),
    /** Lien de la conversation choisie pour la dictée ; null = dernière conversation ouverte dans Claude. */
    val claudeSessionUrl: String? = null,
    /** Bulle micro flottante par-dessus les applis (service d'accessibilité requis). */
    val dictationBubble: Boolean = false,
    val dictationConfirm: Boolean = true,
    /** Après l'envoi, revenir à l'appli d'où l'on a dicté. */
    val dictationReturn: Boolean = true,
    val doubleTapAction: GestureAction = GestureAction.LOCK,
    val swipeUpAction: GestureAction = GestureAction.DRAWER,
    val swipeDownAction: GestureAction = GestureAction.NOTIFICATIONS,
    val twoFingerSwipeUpAction: GestureAction = GestureAction.NONE,
    val twoFingerSwipeDownAction: GestureAction = GestureAction.NONE,
    val pinchInAction: GestureAction = GestureAction.NONE,
    val pinchOutAction: GestureAction = GestureAction.NONE,
    /** Icônes monochromes aux couleurs du thème (Android 13+). */
    val themedIcons: Boolean = false,
    val badgeStyle: BadgeStyle = BadgeStyle.COUNT,
    val dockColumns: Int = 5,
    /** Grille d'icônes de l'accueil (placement libre, comme Nova / Smart Launcher). */
    val homeColumns: Int = 5,
    val homeRows: Int = 6,
    val homeShowLabels: Boolean = true,
    val showSearchBar: Boolean = true,
    /** Ligne des apps les plus utilisées en haut du tiroir. */
    val showFrequentApps: Boolean = true,
    val webSearchEngine: SearchEngine = SearchEngine.SYSTEM,
    /** Ne pas afficher l'espace privé verrouillé dans le tiroir (accès par la recherche). */
    val hidePrivateSpaceWhenLocked: Boolean = false,
) {
    fun actionFor(gesture: Gesture): GestureAction = when (gesture) {
        Gesture.DOUBLE_TAP -> doubleTapAction
        Gesture.SWIPE_UP -> swipeUpAction
        Gesture.SWIPE_DOWN -> swipeDownAction
        Gesture.TWO_FINGER_UP -> twoFingerSwipeUpAction
        Gesture.TWO_FINGER_DOWN -> twoFingerSwipeDownAction
        Gesture.PINCH_IN -> pinchInAction
        Gesture.PINCH_OUT -> pinchOutAction
    }

    fun withAction(gesture: Gesture, action: GestureAction): LauncherSettings = when (gesture) {
        Gesture.DOUBLE_TAP -> copy(doubleTapAction = action)
        Gesture.SWIPE_UP -> copy(swipeUpAction = action)
        Gesture.SWIPE_DOWN -> copy(swipeDownAction = action)
        Gesture.TWO_FINGER_UP -> copy(twoFingerSwipeUpAction = action)
        Gesture.TWO_FINGER_DOWN -> copy(twoFingerSwipeDownAction = action)
        Gesture.PINCH_IN -> copy(pinchInAction = action)
        Gesture.PINCH_OUT -> copy(pinchOutAction = action)
    }
}

/** Conversation Claude enregistrée pour la dictée : nom affiché et lien (claude.ai/code/session_…). */
data class ClaudeSession(val name: String, val url: String)

/** Nom de la destination de la dictée. */
val LauncherSettings.claudeTargetName: String
    get() = claudeSessionUrl?.let { url -> claudeSessions.firstOrNull { it.url == url }?.name ?: "Conversation enregistrée" }
        ?: "Dernière conversation ouverte"

/** Une case de l'accueil : page (0 = première), colonne, ligne. */
data class HomeCell(val page: Int, val x: Int, val y: Int)

/** Une icône posée sur la grille de l'accueil, en colonne [x] et ligne [y] de la page [page]. */
data class HomeIcon(val key: String, val x: Int, val y: Int, val page: Int = 0)

/** Un widget posé sur la grille de l'accueil : case en haut à gauche ([x], [y]), taille en cases, page. */
data class HomeWidget(val id: Int, val x: Int, val y: Int, val spanX: Int, val spanY: Int, val page: Int = 0) {
    /** Cases couvertes, sur sa page. */
    fun cells(): List<Pair<Int, Int>> = (y until y + spanY).flatMap { row -> (x until x + spanX).map { it to row } }

    fun overlaps(other: HomeWidget): Boolean = page == other.page &&
        x < other.x + other.spanX && other.x < x + spanX && y < other.y + other.spanY && other.y < y + spanY
}

/** Widget de l'ancienne page des widgets (déplacé sur la grille de l'accueil au démarrage). */
data class WidgetSlot(val id: Int, val heightDp: Int)
