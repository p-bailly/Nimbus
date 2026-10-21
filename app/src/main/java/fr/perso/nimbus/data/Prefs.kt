package fr.perso.nimbus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
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
    /** Catégorie choisie par app : clé d'app -> identifiant de [Category]. */
    val categoryOverrides: StateFlow<Map<String, String>> = _categoryOverrides.asStateFlow()

    private val _categories = MutableStateFlow(readCategories())
    /** Catégories du tiroir, dans l'ordre des onglets (prédéfinies tant qu'elles n'ont pas été modifiées). */
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

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

    private val _homeWidgets = MutableStateFlow(readHomeWidgets())
    /** Widgets posés sur la grille de l'accueil (identifiants propres à l'appareil : jamais exportés tels quels). */
    val homeWidgets: StateFlow<List<HomeWidget>> = _homeWidgets.asStateFlow()

    private val _homePage = MutableStateFlow(sp.getInt(KEY_HOME_PAGE, 0))
    /** Page principale de l'accueil (celle du bouton Accueil) ; décalée quand une page s'ajoute avant elle. */
    val homePage: StateFlow<Int> = _homePage.asStateFlow()

    private val _homeIcons = MutableStateFlow(readHomeIcons())
    /** Icônes de la grille d'accueil. */
    val homeIcons: StateFlow<List<HomeIcon>> = _homeIcons.asStateFlow()

    private val _autoBackupFolder = MutableStateFlow(sp.getString(KEY_AUTO_BACKUP_DIR, null))
    /** Dossier (URI d'arborescence SAF) des sauvegardes automatiques, null si désactivées. */
    val autoBackupFolder: StateFlow<String?> = _autoBackupFolder.asStateFlow()

    private val _paperSlots = MutableStateFlow(readPaperSlots())
    /** Accueils « papier » : app choisie pour chaque place (sinon, choix automatique). */
    val paperSlots: StateFlow<Map<PaperSlot, String>> = _paperSlots.asStateFlow()

    private val _voronoiSites = MutableStateFlow(readVoronoiSites())
    /** Accueil « Voronoï » : sites déplacés par l'utilisateur (repère de la figure) ; les autres restent à leur place. */
    val voronoiSites: StateFlow<Map<PaperSlot, Offset>> = _voronoiSites.asStateFlow()

    private val _glyphChoices = MutableStateFlow(readMap(KEY_GLYPHS))
    /** Motifs choisis (accueils papier, sommaire) : clé d'app -> [GlyphChoice.spec]. */
    val glyphChoices: StateFlow<Map<String, String>> = _glyphChoices.asStateFlow()

    private val _gestureApps = MutableStateFlow(readGestureApps())
    /** App ouverte par un geste réglé sur « Ouvrir une app ». */
    val gestureApps: StateFlow<Map<Gesture, String>> = _gestureApps.asStateFlow()

    // ---------------------------------------------------------------- Réglages

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val s = transform(_settings.value)
        _settings.value = s
        val e = sp.edit()
        e.putString("themeMode", s.themeMode.name)
        e.putString("homeStyle", s.homeStyle.name)
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
        e.putBoolean("searchFiles", s.searchFiles)
        e.putBoolean("searchPlayStore", s.searchPlayStore)
        e.putString("drawerTheme", s.drawerTheme.name)
        e.putString("claudeSessionUrl", s.claudeSessionUrl)
        e.putString(
            "claudeSessions",
            JSONArray(s.claudeSessions.map { JSONObject().put("name", it.name).put("url", it.url) }).toString(),
        )
        e.putBoolean("dictationBubble", s.dictationBubble)
        e.putBoolean("dictationConfirm", s.dictationConfirm)
        e.putBoolean("dictationCopyOnly", s.dictationCopyOnly)
        e.putBoolean("dictationReturn", s.dictationReturn)
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
        e.putBoolean("drawerToc", s.drawerToc)
        e.putBoolean("paperAppColors", s.paperAppColors)
        e.putBoolean("paperPanel", s.paperPanel)
        e.putString("webSearchEngine", s.webSearchEngine.name)
        e.putBoolean("hidePrivateSpaceWhenLocked", s.hidePrivateSpaceWhenLocked)
        e.putInt("homeColumns", s.homeColumns)
        e.putInt("homeRows", s.homeRows)
        e.putBoolean("homeShowLabels", s.homeShowLabels)
        e.putBoolean("showSearchBar", s.showSearchBar)
        e.putBoolean("hideNavigationOnHome", s.hideNavigationOnHome)
        e.putBoolean("unlockPage", s.unlockPage)
        e.apply()
    }

    private fun readSettings(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            themeMode = enumPref("themeMode", d.themeMode),
            homeStyle = enumPref("homeStyle", d.homeStyle),
            iconShape = enumPref("iconShape", d.iconShape),
            iconPack = sp.getString("iconPack", d.iconPack),
            wrapLegacyIcons = sp.getBoolean("wrapLegacyIcons", d.wrapLegacyIcons),
            iconSize = enumPref("iconSize", d.iconSize),
            drawerColumns = sp.getInt("drawerColumns", d.drawerColumns).coerceIn(3, 7),
            showLabels = sp.getBoolean("showLabels", d.showLabels),
            sortMode = enumPref("sortMode", d.sortMode),
            autoCategories = sp.getBoolean("autoCategories", d.autoCategories),
            showClock = sp.getBoolean("showClock", d.showClock),
            showBattery = sp.getBoolean("showBattery", d.showBattery),
            badgesEnabled = sp.getBoolean("badgesEnabled", d.badgesEnabled),
            searchContacts = sp.getBoolean("searchContacts", d.searchContacts),
            searchFiles = sp.getBoolean("searchFiles", d.searchFiles),
            searchPlayStore = sp.getBoolean("searchPlayStore", d.searchPlayStore),
            drawerTheme = enumPref("drawerTheme", d.drawerTheme),
            claudeSessionUrl = sp.getString("claudeSessionUrl", d.claudeSessionUrl),
            claudeSessions = readClaudeSessions(),
            dictationBubble = sp.getBoolean("dictationBubble", d.dictationBubble),
            dictationConfirm = sp.getBoolean("dictationConfirm", d.dictationConfirm),
            dictationCopyOnly = sp.getBoolean("dictationCopyOnly", d.dictationCopyOnly),
            dictationReturn = sp.getBoolean("dictationReturn", d.dictationReturn),
            doubleTapAction = enumPref("doubleTapAction", d.doubleTapAction),
            swipeUpAction = enumPref("swipeUpAction", d.swipeUpAction),
            swipeDownAction = enumPref("swipeDownAction", d.swipeDownAction),
            twoFingerSwipeUpAction = enumPref("twoFingerSwipeUpAction", d.twoFingerSwipeUpAction),
            twoFingerSwipeDownAction = enumPref("twoFingerSwipeDownAction", d.twoFingerSwipeDownAction),
            pinchInAction = enumPref("pinchInAction", d.pinchInAction),
            pinchOutAction = enumPref("pinchOutAction", d.pinchOutAction),
            themedIcons = sp.getBoolean("themedIcons", d.themedIcons),
            badgeStyle = enumPref("badgeStyle", d.badgeStyle),
            dockColumns = sp.getInt("dockColumns", d.dockColumns).coerceIn(3, 7),
            showFrequentApps = sp.getBoolean("showFrequentApps", d.showFrequentApps),
            drawerToc = sp.getBoolean("drawerToc", d.drawerToc),
            paperAppColors = sp.getBoolean("paperAppColors", d.paperAppColors),
            paperPanel = sp.getBoolean("paperPanel", d.paperPanel),
            webSearchEngine = enumPref("webSearchEngine", d.webSearchEngine),
            hidePrivateSpaceWhenLocked = sp.getBoolean("hidePrivateSpaceWhenLocked", d.hidePrivateSpaceWhenLocked),
            // Bornées : un fichier importé modifié à la main ne doit pas casser l'accueil.
            homeColumns = sp.getInt("homeColumns", d.homeColumns).coerceIn(3, 8),
            homeRows = sp.getInt("homeRows", d.homeRows).coerceIn(3, 14),
            homeShowLabels = sp.getBoolean("homeShowLabels", d.homeShowLabels),
            showSearchBar = sp.getBoolean("showSearchBar", d.showSearchBar),
            hideNavigationOnHome = sp.getBoolean("hideNavigationOnHome", d.hideNavigationOnHome),
            unlockPage = sp.getBoolean("unlockPage", d.unlockPage),
        )
    }

    private fun readClaudeSessions(): List<ClaudeSession> {
        val raw = sp.getString("claudeSessions", null)
        if (raw == null) {
            // Avant la liste, un seul lien : il devient la première conversation.
            val url = sp.getString("claudeSessionUrl", null) ?: return emptyList()
            return listOf(ClaudeSession("Ma conversation", url))
        }
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val url = o.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ClaudeSession(o.optString("name").ifBlank { "Conversation" }, url)
            }
        } catch (e: Exception) {
            emptyList()
        }
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

    fun setCategoryOverride(key: String, category: String?) {
        val map = _categoryOverrides.value.toMutableMap()
        if (category == null) map.remove(key) else map[key] = category
        writeOverrides(map)
    }

    private fun writeOverrides(map: Map<String, String>) {
        _categoryOverrides.value = map
        writeList(KEY_CATEGORIES, map.map { (k, v) -> "$k=$v" })
    }

    private fun readOverrides(): Map<String, String> {
        val result = HashMap<String, String>()
        for (line in readList(KEY_CATEGORIES)) {
            val idx = line.lastIndexOf('=')
            if (idx <= 0 || idx == line.lastIndex) continue
            result[line.substring(0, idx)] = line.substring(idx + 1)
        }
        return result
    }

    /**
     * Remplace la liste des catégories (ordre des onglets). « Autres » y reste toujours ; les apps
     * rangées dans une catégorie disparue reviennent au classement automatique.
     */
    fun setCategories(list: List<Category>) {
        val clean = list.filter { it.id.isNotBlank() }
            .map { it.copy(label = it.label.replace('\n', ' ').trim().ifEmpty { it.id }) }
            .distinctBy { it.id }
            .let { if (it.none { c -> c.id == Category.OTHER_ID }) it + Category.defaults.first { c -> c.id == Category.OTHER_ID } else it }
        _categories.value = clean
        if (clean == Category.defaults) {
            sp.edit().remove(KEY_CATEGORY_LIST).apply()
        } else {
            val array = JSONArray(clean.map { JSONObject().put("id", it.id).put("label", it.label) })
            sp.edit().putString(KEY_CATEGORY_LIST, array.toString()).apply()
        }
        val ids = clean.mapTo(HashSet()) { it.id }
        val overrides = _categoryOverrides.value
        if (overrides.values.any { it !in ids }) writeOverrides(overrides.filterValues { it in ids })
    }

    private fun readCategories(): List<Category> {
        val raw = sp.getString(KEY_CATEGORY_LIST, null) ?: return Category.defaults
        return try {
            val array = JSONArray(raw)
            val list = (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Category(id, o.optString("label").ifBlank { id })
            }.distinctBy { it.id }
            if (list.none { it.id == Category.OTHER_ID }) list + Category.defaults.first { it.id == Category.OTHER_ID } else list
        } catch (e: Exception) {
            Category.defaults
        }
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

    /** Déplace un widget de la page widgets à la position [toIndex] (réordonnancement). */
    fun moveWidget(id: Int, toIndex: Int) {
        val list = _widgets.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return
        val slot = list.removeAt(from)
        list.add(toIndex.coerceIn(0, list.size), slot)
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

    /**
     * Place ces icônes aux cases indiquées (ajout si absentes). Les cases sont calculées par
     * l'appelant à partir de la grille affichée (icônes visibles, grille éventuellement réduite).
     */
    fun setHomeIconPositions(positions: Map<String, HomeCell>) {
        val list = _homeIcons.value
        val updated = list.map { icon -> positions[icon.key]?.let { icon.copy(page = it.page, x = it.x, y = it.y) } ?: icon }
        val added = positions.filterKeys { key -> list.none { it.key == key } }
            .map { (key, cell) -> HomeIcon(key, cell.x, cell.y, cell.page) }
        setHomeIcons(updated + added)
    }

    fun removeHomeIcon(key: String) = setHomeIcons(_homeIcons.value.filterNot { it.key == key })

    fun setHomeIcons(list: List<HomeIcon>) {
        _homeIcons.value = list
        // « clé@x,y » sur la première page (format d'origine), « clé@x,y,page » sur les suivantes.
        writeList(KEY_HOME_ICONS, list.map { "${it.key}@${it.x},${it.y}" + if (it.page > 0) ",${it.page}" else "" })
    }

    private fun readHomeIcons(): List<HomeIcon> = readList(KEY_HOME_ICONS).mapNotNull { line ->
        val at = line.lastIndexOf('@')
        if (at <= 0) return@mapNotNull null
        val pos = line.substring(at + 1).split(',')
        val x = pos.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val y = pos.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        HomeIcon(line.substring(0, at), x, y, pos.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0)
    }

    // ---------------------------------------------------------------- Widgets de l'accueil

    /** Ajoute ou remplace (même identifiant) un widget de l'accueil. */
    fun putHomeWidget(widget: HomeWidget) {
        val list = _homeWidgets.value.filterNot { it.id == widget.id } + widget
        _homeWidgets.value = list
        writeHomeWidgets(list)
    }

    fun removeHomeWidget(id: Int) = setHomeWidgets(_homeWidgets.value.filterNot { it.id == id })

    fun setHomeWidgets(list: List<HomeWidget>) {
        _homeWidgets.value = list
        writeHomeWidgets(list)
    }

    private fun writeHomeWidgets(list: List<HomeWidget>) =
        writeList(KEY_HOME_WIDGETS, list.map { "${it.id}@${it.x},${it.y},${it.spanX},${it.spanY}" + if (it.page > 0) ",${it.page}" else "" })

    private fun readHomeWidgets(): List<HomeWidget> = readList(KEY_HOME_WIDGETS).mapNotNull { line ->
        val id = line.substringBefore('@').toIntOrNull() ?: return@mapNotNull null
        val v = line.substringAfter('@').split(',').mapNotNull { it.toIntOrNull() }
        if (v.size != 4 && v.size != 5) return@mapNotNull null
        HomeWidget(id, v[0], v[1], v[2].coerceAtLeast(1), v[3].coerceAtLeast(1), v.getOrElse(4) { 0 }.coerceAtLeast(0))
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
        categoryOverrides?.let { overrides ->
            // Une catégorie prédéfinie supprimée revient si l'import y range des apps.
            val missing = overrides.values.toSet().filter { cat -> _categories.value.none { it.id == cat.name } }
            if (missing.isNotEmpty()) setCategories(_categories.value + missing.map { Category(it.name, it.label) })
            writeOverrides(overrides.mapValues { it.value.name })
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

    /** [choice] null : motif automatique (celui de la place, sinon la silhouette de l'app). */
    fun setGlyphChoice(key: String, choice: GlyphChoice?) {
        val map = _glyphChoices.value.toMutableMap()
        if (choice == null) map.remove(key) else map[key] = choice.spec.replace('\n', ' ')
        _glyphChoices.value = map
        writeMap(KEY_GLYPHS, map)
    }

    fun setGestureApp(gesture: Gesture, appKey: String?) {
        val map = _gestureApps.value.toMutableMap()
        if (appKey == null) map.remove(gesture) else map[gesture] = appKey
        _gestureApps.value = map
        writeMap(KEY_GESTURE_APPS, map.mapKeys { it.key.name })
    }

    /** [appKey] null : la place revient au choix automatique. */
    fun setPaperSlot(slot: PaperSlot, appKey: String?) {
        val map = _paperSlots.value.toMutableMap()
        if (appKey == null) map.remove(slot) else map[slot] = appKey
        _paperSlots.value = map
        writeMap(KEY_PAPER_SLOTS, map.mapKeys { it.key.name })
    }

    /** Vide : tous les sites reviennent à leur place d'origine. */
    fun setVoronoiSites(sites: Map<PaperSlot, Offset>) {
        _voronoiSites.value = sites
        writeMap(KEY_VORONOI_SITES, sites.entries.associate { (slot, p) -> slot.name to "${p.x},${p.y}" })
    }

    private fun readVoronoiSites(): Map<PaperSlot, Offset> =
        readMap(KEY_VORONOI_SITES).mapNotNull { (name, value) ->
            val slot = PaperSlot.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            val (x, y) = value.split(',').mapNotNull { it.toFloatOrNull() }.takeIf { it.size == 2 } ?: return@mapNotNull null
            slot to Offset(x, y)
        }.toMap()

    private fun readPaperSlots(): Map<PaperSlot, String> =
        readMap(KEY_PAPER_SLOTS).mapNotNull { (name, app) ->
            PaperSlot.entries.firstOrNull { it.name == name }?.let { it to app }
        }.toMap()

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
    fun setHomePage(page: Int) {
        val value = page.coerceAtLeast(0)
        _homePage.value = value
        sp.edit().putInt(KEY_HOME_PAGE, value).apply()
    }

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
        _categories.value = readCategories()
        _usage.value = readUsage()
        _customLabels.value = readMap(KEY_LABELS)
        _iconSwipeActions.value = readMap(KEY_ICON_SWIPES)
        _glyphChoices.value = readMap(KEY_GLYPHS)
        _gestureApps.value = readGestureApps()
        _paperSlots.value = readPaperSlots()
        _voronoiSites.value = readVoronoiSites()
        _homeIcons.value = readHomeIcons()
        _homePage.value = sp.getInt(KEY_HOME_PAGE, 0)
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
        private const val KEY_CATEGORY_LIST = "categoryList"
        private const val KEY_WIDGETS = "widgets"
        private const val KEY_PIN = "pinHash"
        private const val KEY_LABELS = "customLabels"
        private const val KEY_ICON_SWIPES = "iconSwipeActions"
        private const val KEY_GESTURE_APPS = "gestureApps"
        private const val KEY_GLYPHS = "glyphChoices"
        private const val KEY_PAPER_SLOTS = "paperSlots"
        private const val KEY_VORONOI_SITES = "voronoiSites"
        private const val KEY_HOME_ICONS = "homeIcons"
        private const val KEY_HOME_WIDGETS = "homeWidgets"

        private const val BACKUP_APP = "nimbus"
        private const val BACKUP_VERSION = 1
        private const val KEY_AUTO_BACKUP_DIR = "autoBackupFolder"
        private const val KEY_LAST_AUTO_BACKUP = "lastAutoBackup"
        private const val KEY_HOME_PAGE = "homePage"

        /** Propres à l'appareil ou secrets : jamais exportés ni écrasés par un import. */
        private val NOT_EXPORTED = setOf(KEY_PIN, KEY_WIDGETS, KEY_HOME_WIDGETS, KEY_AUTO_BACKUP_DIR, KEY_LAST_AUTO_BACKUP)

        @Volatile
        private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }
}
