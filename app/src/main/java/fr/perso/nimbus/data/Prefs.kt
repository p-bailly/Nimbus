package fr.perso.nimbus.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HIDDEN = "hidden"
        private const val KEY_CATEGORIES = "categoryOverrides"
        private const val KEY_WIDGETS = "widgets"
        private const val KEY_PIN = "pinHash"

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }
}
