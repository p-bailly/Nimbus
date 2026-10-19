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
    OTHER("Autres"),
}

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
)

enum class ThemeMode(val label: String) {
    AMBIENT("Ambiant (couleurs du fond d'écran)"),
    MATERIAL_YOU("Material You (Android 12+)"),
    DARK("Sombre"),
    LIGHT("Clair"),
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
    val doubleTapAction: GestureAction = GestureAction.LOCK,
    val swipeUpAction: GestureAction = GestureAction.DRAWER,
    val swipeDownAction: GestureAction = GestureAction.NOTIFICATIONS,
)

/** Un widget posé sur la page widgets. */
data class WidgetSlot(val id: Int, val heightDp: Int)
