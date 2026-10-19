package fr.perso.nimbus.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Stockage de tous les réglages du launcher (SharedPreferences),
 * exposés sous forme de StateFlow pour l'interface.
 */
class Prefs private constructor(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("nimbus", Context.MODE_PRIVATE)
    private val usageSp: SharedPreferences =
        context.getSharedPreferences("nimbus_usage", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _favorites = MutableStateFlow(readList(KEY_FAVORITES))
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _hidden = MutableStateFlow(readList(KEY_HIDDEN).toSet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _categoryOverrides = MutableStateFlow(readOverrides())
    val categoryOverrides: StateFlow<Map<String, AppCategory>> = _categoryOverrides.asStateFlow()

    private val _usage = MutableStateFlow(readUsage())
    val usage: StateFlow<Map<String, Int>> = _usage.asStateFlow()

    private val _widgets = MutableStateFlow(readWidgets())
    val widgets: StateFlow<List<WidgetSlot>> = _widgets.asStateFlow()

    private val _customLabels = MutableStateFlow(readMap(KEY_LABELS))
    /** Noms personnalisés : clé d'app -> nom affiché. */
    val customLabels: StateFlow<Map<String, String>> = _customLabels.asStateFlow()

    private val _iconSwipeActions = MutableStateFlow(readMap(KEY_ICON_SWIPES))
    /** Glissé vers le haut sur une icône du dock : clé d'app -> clé de l'app à ouvrir. */
    val iconSwipeActions: StateFlow<Map<String, String>> = _iconSwipeActions.asStateFlow()

    private val _homeIcons = MutableStateFlow(readHomeIcons())
    /** Icônes de la grille d'accueil. */
    val homeIcons: StateFlow<List<HomeIcon>> = _homeIcons.asStateFlow()

    private val _autoBackupFolder = MutableStateFlow(sp.getString(KEY_AUTO_BACKUP_DIR, null))
    /** Dossier (URI d'arborescence SAF) des sauvegardes automatiques, null si désactivées. */
    val autoBackupFolder: StateFlow<String?> = _autoBackupFolder.asStateFlow()

    private val _gestureApps = MutableStateFlow(readGestureApps())
    /** App ouverte par un geste réglé sur « Ouvrir une app ». */
    val gestureApps: StateFlow<Map<Gesture, String>> = _gestureApps.asStateFlow()

    // ---------------------------------------------------------------- Réglages

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val s = transform(_settings.value)
        _settings.value = s
        val e = sp.edit()
        e.putString("themeMode", s.themeMode.name)
        e.putString("iconShape", s.iconShape.name)
        e.putString("iconPack", s.iconPack)
        e.putBoolean("wrapLegacyIcons", s.wrapLegacyIcons)
        e.putString("iconSize", s.iconSize.name)
        e.putInt("drawerColumns", s.drawerColumns)
        e.putBoolean("showLabels", s.showLabels)
        e.putString("sortMode", s.sortMode.name)
        e.putBoolean("autoCategories", s.autoCategories)
        e.putBoolean("showClock", s.showClock)
        e.putBoolean("showBattery", s.showBattery)
        e.putBoolean("badgesEnabled", s.badgesEnabled)
        e.putBoolean("searchContacts", s.searchContacts)
        e.putString("doubleTapAction", s.doubleTapAction.name)
        e.putString("swipeUpAction", s.swipeUpAction.name)
        e.putString("swipeDownAction", s.swipeDownAction.name)
        e.putString("twoFingerSwipeUpAction", s.twoFingerSwipeUpAction.name)
        e.putString("twoFingerSwipeDownAction", s.twoFingerSwipeDownAction.name)
        e.putString("pinchInAction", s.pinchInAction.name)
        e.putString("pinchOutAction", s.pinchOutAction.name)
        e.putBoolean("themedIcons", s.themedIcons)
        e.putString("badgeStyle", s.badgeStyle.name)
        e.putInt("dockColumns", s.dockColumns)
        e.putBoolean("showFrequentApps", s.showFrequentApps)
        e.putString("webSearchEngine", s.webSearchEngine.name)
        e.putBoolean("hidePrivateSpaceWhenLocked", s.hidePrivateSpaceWhenLocked)
        e.putInt("homeColumns", s.homeColumns)
        e.putInt("homeRows", s.homeRows)
        e.putBoolean("homeShowLabels", s.homeShowLabels)
        e.putBoolean("showSearchBar", s.showSearchBar)
        e.apply()
    }

    private fun readSettings(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            themeMode = enumPref("themeMode", d.themeMode),
            iconShape = enumPref("iconShape", d.iconShape),
            iconPack = sp.getString("iconPack", d.iconPack),
            wrapLegacyIcons = sp.getBoolean("wrapLegacyIcons", d.wrapLegacyIcons),
            iconSize = enumPref("iconSize", d.iconSize),
            drawerColumns = sp.getInt("drawerColumns", d.drawerColumns),
            showLabels = sp.getBoolean("showLabels", d.showLabels),
            sortMode = enumPref("sortMode", d.sortMode),
            autoCategories = sp.getBoolean("autoCategories", d.autoCategories),
            showClock = sp.getBoolean("showClock", d.showClock),
            showBattery = sp.getBoolean("showBattery", d.showBattery),
            badgesEnabled = sp.getBoolean("badgesEnabled", d.badgesEnabled),
            searchContacts = sp.getBoolean("searchContacts", d.searchContacts),
            doubleTapAction = enumPref("doubleTapAction", d.doubleTapAction),
            swipeUpAction = enumPref("swipeUpAction", d.swipeUpAction),
            swipeDownAction = enumPref("swipeDownAction", d.swipeDownAction),
            twoFingerSwipeUpAction = enumPref("twoFingerSwipeUpAction", d.twoFingerSwipeUpAction),
            twoFingerSwipeDownAction = enumPref("twoFingerSwipeDownAction", d.twoFingerSwipeDownAction),
            pinchInAction = enumPref("pinchInAction", d.pinchInAction),
            pinchOutAction = enumPref("pinchOutAction", d.pinchOutAction),
            themedIcons = sp.getBoolean("themedIcons", d.themedIcons),
            badgeStyle = enumPref("badgeStyle", d.badgeStyle),
            dockColumns = sp.getInt("dockColumns", d.dockColumns),
            showFrequentApps = sp.getBoolean("showFrequentApps", d.showFrequentApps),
            webSearchEngine = enumPref("webSearchEngine", d.webSearchEngine),
            hidePrivateSpaceWhenLocked = sp.getBoolean("hidePrivateSpaceWhenLocked", d.hidePrivateSpaceWhenLocked),
            homeColumns = sp.getInt("homeColumns", d.homeColumns),
            homeRows = sp.getInt("homeRows", d.homeRows),
            homeShowLabels = sp.getBoolean("homeShowLabels", d.homeShowLabels),
            showSearchBar = sp.getBoolean("showSearchBar", d.showSearchBar),
        )
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, default: T): T {
        val name = sp.getString(key, null) ?: return default
        return enumValues<T>().firstOrNull { it.name == name } ?: default
    }

    // ---------------------------------------------------------------- Favoris

    fun isFavorite(key: String): Boolean = key in _favorites.value

    fun toggleFavorite(key: String) {
        val list = _favorites.value.toMutableList()
        if (!list.remove(key)) list.add(key)
        _favorites.value = list
        writeList(KEY_FAVORITES, list)
    }

    // ---------------------------------------------------------------- Apps masquées

    fun isHidden(key: String): Boolean = key in _hidden.value

    fun setHidden(key: String, hidden: Boolean) {
        val set = _hidden.value.toMutableSet()
        if (hidden) set.add(key) else set.remove(key)
        _hidden.value = set
        writeList(KEY_HIDDEN, set.toList())
    }

    // ---------------------------------------------------------------- Catégories

    fun setCategoryOverride(key: String, category: AppCategory?) {
        val map = _categoryOverrides.value.toMutableMap()
        if (category == null) map.remove(key) else map[key] = category
        _categoryOverrides.value = map
        writeList(KEY_CATEGORIES, map.map { (k, v) -> "$k=${v.name}" })
    }

    private fun readOverrides(): Map<String, AppCategory> {
        val result = HashMap<String, AppCategory>()
        for (line in readList(KEY_CATEGORIES)) {
            val idx = line.lastIndexOf('=')
            if (idx <= 0) continue
            val cat = AppCategory.entries.firstOrNull { it.name == line.substring(idx + 1) } ?: continue
            result[line.substring(0, idx)] = cat
        }
        return result
    }

    // ---------------------------------------------------------------- Usage (tri par utilisation)

    fun incrementUsage(key: String) {
        val count = usageSp.getInt(key, 0) + 1
        usageSp.edit().putInt(key, count).apply()
        _usage.value = _usage.value + (key to count)
    }

    private fun readUsage(): Map<String, Int> =
        usageSp.all.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.toMap()

    // ---------------------------------------------------------------- Widgets

    fun addWidget(id: Int, heightDp: Int) {
        val list = _widgets.value + WidgetSlot(id, heightDp)
        _widgets.value = list
        writeWidgets(list)
    }

    fun removeWidget(id: Int) {
        val list = _widgets.value.filterNot { it.id == id }
        _widgets.value = list
        writeWidgets(list)
    }

    fun resizeWidget(id: Int, deltaDp: Int) {
        val list = _widgets.value.map {
            if (it.id == id) it.copy(heightDp = (it.heightDp + deltaDp).coerceIn(60, 600)) else it
        }
        _widgets.value = list
        writeWidgets(list)
    }

    private fun readWidgets(): List<WidgetSlot> = readList(KEY_WIDGETS).mapNotNull { line ->
        val parts = line.split(':')
        val id = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val h = parts.getOrNull(1)?.toIntOrNull() ?: 180
        WidgetSlot(id, h)
    }

    private fun writeWidgets(list: List<WidgetSlot>) =
        writeList(KEY_WIDGETS, list.map { "${it.id}:${it.heightDp}" })

    // ---------------------------------------------------------------- Grille d'accueil

    /** Ajoute l'icône dans la première case libre ; false si elle y est déjà ou si la grille est pleine. */
    fun addHomeIcon(key: String, columns: Int, rows: Int): Boolean {
        val list = _homeIcons.value
        if (list.any { it.key == key }) return false
        val taken = list.map { it.x to it.y }.toSet()
        for (y in 0 until rows) for (x in 0 until columns) {
            if ((x to y) !in taken) {
                setHomeIcons(list + HomeIcon(key, x, y))
                return true
            }
        }
        return false
    }

    /** Déplace une icône ; si la case est occupée, les deux icônes s'échangent. */
    fun moveHomeIcon(key: String, x: Int, y: Int) {
        val list = _homeIcons.value
        val moving = list.firstOrNull { it.key == key } ?: return
        setHomeIcons(list.map {
            when {
                it.key == key -> it.copy(x = x, y = y)
                it.x == x && it.y == y -> it.copy(x = moving.x, y = moving.y)
                else -> it
            }
        })
    }

    fun removeHomeIcon(key: String) = setHomeIcons(_homeIcons.value.filterNot { it.key == key })

    fun setHomeIcons(list: List<HomeIcon>) {
        _homeIcons.value = list
        writeList(KEY_HOME_ICONS, list.map { "${it.key}@${it.x},${it.y}" })
    }

    private fun readHomeIcons(): List<HomeIcon> = readList(KEY_HOME_ICONS).mapNotNull { line ->
        val at = line.lastIndexOf('@')
        if (at <= 0) return@mapNotNull null
        val pos = line.substring(at + 1).split(',')
        val x = pos.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val y = pos.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        HomeIcon(line.substring(0, at), x, y)
    }

    // ---------------------------------------------------------------- Remplacement en bloc (imports)

    /** Remplace les listes fournies (null = inchangée), par exemple lors d'un import Smart Launcher. */
    fun replaceAll(
        favorites: List<String>? = null,
        hidden: Set<String>? = null,
        categoryOverrides: Map<String, AppCategory>? = null,
        usage: Map<String, Int>? = null,
        customLabels: Map<String, String>? = null,
        homeIcons: List<HomeIcon>? = null,
    ) {
        favorites?.let {
            _favorites.value = it
            writeList(KEY_FAVORITES, it)
        }
        hidden?.let {
            _hidden.value = it
            writeList(KEY_HIDDEN, it.toList())
        }
        categoryOverrides?.let {
            _categoryOverrides.value = it
            writeList(KEY_CATEGORIES, it.map { (k, v) -> "$k=${v.name}" })
        }
        usage?.let {
            val e = usageSp.edit().clear()
            for ((k, v) in it) e.putInt(k, v)
            e.apply()
            _usage.value = it
        }
        customLabels?.let {
            _customLabels.value = it
            writeMap(KEY_LABELS, it)
        }
        homeIcons?.let { setHomeIcons(it) }
    }

    // ---------------------------------------------------------------- Noms, glissés sur icônes, apps des gestes

    /** [label] null ou vide : retour au nom d'origine. */
    fun setCustomLabel(key: String, label: String?) {
        val clean = label?.replace('\n', ' ')?.trim()
        val map = _customLabels.value.toMutableMap()
        if (clean.isNullOrEmpty()) map.remove(key) else map[key] = clean
        _customLabels.value = map
        writeMap(KEY_LABELS, map)
    }

    fun setIconSwipeAction(key: String, targetKey: String?) {
        val map = _iconSwipeActions.value.toMutableMap()
        if (targetKey == null) map.remove(key) else map[key] = targetKey
        _iconSwipeActions.value = map
        writeMap(KEY_ICON_SWIPES, map)
    }

    fun setGestureApp(gesture: Gesture, appKey: String?) {
        val map = _gestureApps.value.toMutableMap()
        if (appKey == null) map.remove(gesture) else map[gesture] = appKey
        _gestureApps.value = map
        writeMap(KEY_GESTURE_APPS, map.mapKeys { it.key.name })
    }

    private fun readGestureApps(): Map<Gesture, String> =
        readMap(KEY_GESTURE_APPS).mapNotNull { (name, app) ->
            Gesture.entries.firstOrNull { it.name == name }?.let { it to app }
        }.toMap()

    // ---------------------------------------------------------------- Sauvegarde

    fun setAutoBackupFolder(uri: String?) {
        _autoBackupFolder.value = uri
        sp.edit().putString(KEY_AUTO_BACKUP_DIR, uri).apply()
    }

    /** Date (ms) de la dernière sauvegarde automatique réussie. */
    var lastAutoBackup: Long
        get() = sp.getLong(KEY_LAST_AUTO_BACKUP, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_AUTO_BACKUP, value).apply()

    /**
     * Exporte réglages, favoris, apps masquées, catégories, noms, gestes et usage en JSON.
     * Le code PIN et les widgets (identifiants propres à l'appareil) ne sont pas exportés.
     */
    fun exportJson(): JSONObject {
        val strings = JSONObject()
        val booleans = JSONObject()
        val ints = JSONObject()
        for ((key, value) in sp.all) {
            if (key in NOT_EXPORTED) continue
            when (value) {
                is String -> strings.put(key, value)
                is Boolean -> booleans.put(key, value)
                is Int -> ints.put(key, value)
            }
        }
        val usage = JSONObject()
        for ((key, count) in _usage.value) usage.put(key, count)
        return JSONObject()
            .put("app", BACKUP_APP)
            .put("version", BACKUP_VERSION)
            .put("strings", strings)
            .put("booleans", booleans)
            .put("ints", ints)
            .put("usage", usage)
    }

    /** Remplace les données exportables par celles de la sauvegarde. Retourne false si elle n'est pas reconnue. */
    fun importJson(root: JSONObject): Boolean {
        if (root.optString("app") != BACKUP_APP) return false

        val e = sp.edit()
        for (key in sp.all.keys) if (key !in NOT_EXPORTED) e.remove(key)
        root.optJSONObject("strings")?.let { o -> o.keys().forEach { k -> if (k !in NOT_EXPORTED) e.putString(k, o.getString(k)) } }
        root.optJSONObject("booleans")?.let { o -> o.keys().forEach { k -> if (k !in NOT_EXPORTED) e.putBoolean(k, o.getBoolean(k)) } }
        root.optJSONObject("ints")?.let { o -> o.keys().forEach { k -> if (k !in NOT_EXPORTED) e.putInt(k, o.getInt(k)) } }
        // apply() met la mémoire à jour tout de suite : les relectures ci-dessous voient déjà l'import.
        e.apply()

        val u = usageSp.edit().clear()
        root.optJSONObject("usage")?.let { o -> o.keys().forEach { k -> u.putInt(k, o.optInt(k)) } }
        u.apply()

        _settings.value = readSettings()
        _favorites.value = readList(KEY_FAVORITES)
        _hidden.value = readList(KEY_HIDDEN).toSet()
        _categoryOverrides.value = readOverrides()
        _usage.value = readUsage()
        _customLabels.value = readMap(KEY_LABELS)
        _iconSwipeActions.value = readMap(KEY_ICON_SWIPES)
        _gestureApps.value = readGestureApps()
        _homeIcons.value = readHomeIcons()
        return true
    }

    // ---------------------------------------------------------------- Code PIN

    fun hasPin(): Boolean = sp.getString(KEY_PIN, null) != null

    fun checkPin(pin: String): Boolean = sp.getString(KEY_PIN, null) == hash(pin)

    fun setPin(pin: String?) {
        sp.edit().putString(KEY_PIN, pin?.let { hash(it) }).apply()
    }

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("nimbus:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ---------------------------------------------------------------- Outils

    private fun readList(key: String): List<String> =
        sp.getString(key, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    private fun writeList(key: String, list: List<String>) {
        sp.edit().putString(key, list.joinToString("\n")).apply()
    }

    /** Lignes « clé=valeur » ; la clé ne contient jamais de « = » (paquet/classe#profil). */
    private fun readMap(key: String): Map<String, String> =
        readList(key).mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
        }.toMap()

    private fun writeMap(key: String, map: Map<String, String>) =
        writeList(key, map.map { (k, v) -> "$k=$v" })

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HIDDEN = "hidden"
        private const val KEY_CATEGORIES = "categoryOverrides"
        private const val KEY_WIDGETS = "widgets"
        private const val KEY_PIN = "pinHash"
        private const val KEY_LABELS = "customLabels"
        private const val KEY_ICON_SWIPES = "iconSwipeActions"
        private const val KEY_GESTURE_APPS = "gestureApps"
        private const val KEY_HOME_ICONS = "homeIcons"

        private const val BACKUP_APP = "nimbus"
        private const val BACKUP_VERSION = 1
        private const val KEY_AUTO_BACKUP_DIR = "autoBackupFolder"
        private const val KEY_LAST_AUTO_BACKUP = "lastAutoBackup"

        /** Propres à l'appareil ou secrets : jamais exportés ni écrasés par un import. */
        private val NOT_EXPORTED = setOf(KEY_PIN, KEY_WIDGETS, KEY_AUTO_BACKUP_DIR, KEY_LAST_AUTO_BACKUP)

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }
}
