package fr.perso.nimbus.ui

import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.pm.ShortcutInfo
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.AppRepository
import fr.perso.nimbus.data.ContactResult
import fr.perso.nimbus.data.ContactsSearch
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.IconConfig
import fr.perso.nimbus.data.IconFactory
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.data.SortMode
import fr.perso.nimbus.data.WidgetSlot
import fr.perso.nimbus.service.BadgeStore
import fr.perso.nimbus.service.GestureService
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.drawer.SearchMatcher
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Écran affiché au-dessus de l'accueil. */
enum class Screen { HOME, SETTINGS, HIDDEN_APPS }

/** Menu ouvert par appui long sur l'accueil : page du pager et position du doigt. */
data class HomeMenu(val page: Int, val position: Offset)

/** Demande de défilement du pager d'accueil ; [id] change à chaque nouvelle demande. */
data class PageRequest(val page: Int, val id: Long)

data class UiState(
    val drawerOpen: Boolean = false,
    /** Le champ de recherche du tiroir doit prendre le focus à l'ouverture. */
    val focusSearch: Boolean = false,
    val query: String = "",
    val screen: Screen = Screen.HOME,
    /** App dont la feuille d'actions est ouverte. */
    val sheetApp: AppEntry? = null,
    val homeMenu: HomeMenu? = null,
    val widgetPickerOpen: Boolean = false,
    val editingWidgets: Boolean = false,
    val pageRequest: PageRequest = PageRequest(0, 0),
)

/** État des autorisations et services système, relu à chaque retour au premier plan. */
data class SystemStatus(
    val isDefaultLauncher: Boolean = false,
    val notificationAccess: Boolean = false,
    val gestureService: Boolean = false,
    val contactsPermission: Boolean = false,
    val hasPin: Boolean = false,
)

data class AppsState(
    val loaded: Boolean = false,
    /** Apps visibles, triées selon le réglage (alphabétique ou usage). */
    val visible: List<AppEntry> = emptyList(),
    /** Apps masquées, par ordre alphabétique. */
    val hidden: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val hiddenKeys: Set<String> = emptySet(),
    /** Catégories qui contiennent au moins une app visible, dans l'ordre des onglets. */
    val categories: List<AppCategory> = emptyList(),
)

data class SearchResults(
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val contacts: List<ContactResult> = emptyList(),
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Application get() = getApplication()
    private val prefs = Prefs.get(application)
    private val repository = AppRepository(application, viewModelScope, IconFactory(application))
    private val wallpaperManager = WallpaperManager.getInstance(application)

    val settings: StateFlow<LauncherSettings> = prefs.settings
    val widgets: StateFlow<List<WidgetSlot>> = prefs.widgets
    val categoryOverrides: StateFlow<Map<String, AppCategory>> = prefs.categoryOverrides

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _status = MutableStateFlow(SystemStatus())
    val status: StateFlow<SystemStatus> = _status.asStateFlow()

    private val _wallpaperColor = MutableStateFlow<Int?>(null)
    /** Couleur dominante du fond d'écran (ARGB), ou null si inconnue (fond animé…). */
    val wallpaperColor: StateFlow<Int?> = _wallpaperColor.asStateFlow()

    private val colorsListener = WallpaperManager.OnColorsChangedListener { colors, which ->
        if (which and WallpaperManager.FLAG_SYSTEM != 0) _wallpaperColor.value = colors.toArgb()
    }

    /** Une app a été lancée : le tiroir se referme quand le launcher passe en arrière-plan. */
    private var resetOnStop = false

    val badges: StateFlow<Map<String, Int>> =
        combine(BadgeStore.counts, settings.map { it.badgesEnabled }.distinctUntilChanged()) { counts, enabled ->
            if (enabled) counts else emptyMap()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Apps avec leur catégorie finale : choix manuel, sinon classement automatique. */
    private val categorized = combine(
        repository.apps,
        prefs.categoryOverrides,
        settings.map { it.autoCategories }.distinctUntilChanged(),
    ) { apps, overrides, auto ->
        apps.map { app ->
            val category = overrides[app.key] ?: if (auto) app.autoCategory else AppCategory.OTHER
            if (category == app.category) app else app.copy(category = category)
        }
    }

    val apps: StateFlow<AppsState> = combine(
        categorized,
        prefs.hidden,
        prefs.usage,
        prefs.favorites,
        settings.map { it.sortMode }.distinctUntilChanged(),
    ) { all, hidden, usage, favorites, sortMode ->
        // La liste du dépôt est déjà alphabétique ; le tri par usage est stable,
        // donc les apps de même usage restent dans l'ordre alphabétique.
        val sorted = if (sortMode == SortMode.USAGE) all.sortedByDescending { usage[it.key] ?: 0 } else all
        val visible = sorted.filterNot { it.key in hidden }
        val byKey = all.associateBy { it.key }
        AppsState(
            loaded = all.isNotEmpty(),
            visible = visible,
            hidden = all.filter { it.key in hidden },
            favorites = favorites.mapNotNull { byKey[it] }.filterNot { it.key in hidden },
            favoriteKeys = favorites.toSet(),
            hiddenKeys = hidden,
            categories = AppCategory.entries.filter { cat -> visible.any { it.category == cat } },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppsState())

    private val searchIndex = apps
        .map { SearchMatcher.index(it.visible) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val contacts: StateFlow<List<ContactResult>> = combine(
        ui.map { it.query.trim() }.distinctUntilChanged(),
        settings.map { it.searchContacts }.distinctUntilChanged(),
        status.map { it.contactsPermission }.distinctUntilChanged(),
    ) { query, enabled, granted -> Triple(query, enabled, granted) }
        .debounce(150)
        .mapLatest { (query, enabled, granted) ->
            if (query.isEmpty() || !enabled || !granted) emptyList()
            else withContext(Dispatchers.IO) { ContactsSearch.search(context, query) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchResults: StateFlow<SearchResults> = combine(
        searchIndex,
        ui.map { it.query }.distinctUntilChanged(),
        contacts,
    ) { index, query, contacts ->
        if (query.isBlank()) SearchResults()
        else SearchResults(query, SearchMatcher.search(index, query), contacts)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchResults())

    init {
        // Tout changement de forme / pack / taille d'icône recharge les icônes du dépôt.
        settings
            .map {
                val density = context.resources.displayMetrics.density
                IconConfig(
                    shape = it.iconShape,
                    iconPack = it.iconPack,
                    wrapLegacy = it.wrapLegacyIcons,
                    sizePx = (it.iconSize.dp * density).roundToInt(),
                )
            }
            .distinctUntilChanged()
            .onEach { repository.setIconConfig(it) }
            .launchIn(viewModelScope)

        wallpaperManager.addOnColorsChangedListener(colorsListener, Handler(Looper.getMainLooper()))
        viewModelScope.launch {
            val color = withContext(Dispatchers.IO) {
                try {
                    wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM).toArgb()
                } catch (e: Exception) {
                    null
                }
            }
            // Le listener a pu répondre entre-temps avec une valeur plus récente.
            if (_wallpaperColor.value == null) _wallpaperColor.value = color
        }

        refreshSystemStatus()
    }

    private fun WallpaperColors?.toArgb(): Int? = this?.primaryColor?.toArgb()

    // ---------------------------------------------------------------- Navigation

    fun openDrawer(focusSearch: Boolean = false) = _ui.update {
        it.copy(drawerOpen = true, focusSearch = focusSearch, homeMenu = null)
    }

    fun closeDrawer() = _ui.update { it.copy(drawerOpen = false, focusSearch = false, query = "") }

    fun setQuery(query: String) = _ui.update { it.copy(query = query) }

    fun consumeSearchFocus() = _ui.update { it.copy(focusSearch = false) }

    fun openSettings() = _ui.update {
        it.copy(screen = Screen.SETTINGS, drawerOpen = false, query = "", homeMenu = null, editingWidgets = false)
    }

    /** À n'appeler qu'après vérification du code PIN. */
    fun openHiddenApps() = _ui.update { it.copy(screen = Screen.HIDDEN_APPS) }

    /** Retour d'un cran : apps masquées -> réglages -> accueil. */
    fun closeScreen() = _ui.update {
        it.copy(screen = if (it.screen == Screen.HIDDEN_APPS) Screen.SETTINGS else Screen.HOME)
    }

    fun showAppSheet(app: AppEntry) = _ui.update { it.copy(sheetApp = app) }

    fun dismissAppSheet() = _ui.update { it.copy(sheetApp = null) }

    fun openHomeMenu(page: Int, position: Offset) = _ui.update { it.copy(homeMenu = HomeMenu(page, position)) }

    fun closeHomeMenu() = _ui.update { it.copy(homeMenu = null) }

    fun openWidgetPicker() = _ui.update { it.copy(widgetPickerOpen = true, homeMenu = null) }

    fun closeWidgetPicker() = _ui.update { it.copy(widgetPickerOpen = false) }

    fun setEditingWidgets(editing: Boolean) = _ui.update { it.copy(editingWidgets = editing, homeMenu = null) }

    fun showPage(page: Int) = _ui.update { it.copy(pageRequest = PageRequest(page, it.pageRequest.id + 1)) }

    /** Bouton Accueil : tout refermer et revenir à la première page. */
    fun goHome() {
        resetOnStop = false
        _ui.update { UiState(pageRequest = PageRequest(0, it.pageRequest.id + 1)) }
    }

    fun onLauncherStopped() {
        if (!resetOnStop) return
        resetOnStop = false
        _ui.update {
            it.copy(drawerOpen = false, focusSearch = false, query = "", sheetApp = null, homeMenu = null)
        }
    }

    // ---------------------------------------------------------------- Gestes

    fun onGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.LOCK -> if (!SystemActions.lockScreen()) askForGestureService()
            GestureAction.NOTIFICATIONS -> if (!SystemActions.expandNotifications(context)) askForGestureService()
            GestureAction.SEARCH -> openDrawer(focusSearch = true)
            GestureAction.DRAWER -> openDrawer()
            GestureAction.SETTINGS -> openSettings()
        }
    }

    private fun askForGestureService() {
        SystemActions.toast(context, "Active « Gestes Nimbus » dans les réglages d'accessibilité")
        SystemActions.openAccessibilitySettings(context)
    }

    // ---------------------------------------------------------------- Lancements

    fun launch(app: AppEntry) {
        prefs.incrementUsage(app.key)
        SystemActions.launch(context, app)
        resetOnStop = true
    }

    fun openContact(contact: ContactResult) {
        ContactsSearch.open(context, contact)
        resetOnStop = true
    }

    fun webSearch(query: String) {
        if (query.isBlank()) return
        SystemActions.webSearch(context, query.trim())
        resetOnStop = true
    }

    /** Touche « Rechercher » du clavier : première app trouvée, sinon recherche web. */
    fun submitSearch() {
        val results = searchResults.value
        val first = results.apps.firstOrNull()
        if (first != null && results.query == ui.value.query) launch(first) else webSearch(ui.value.query)
    }

    fun startShortcut(shortcut: ShortcutInfo) {
        SystemActions.startShortcut(context, shortcut)
        resetOnStop = true
    }

    fun appInfo(app: AppEntry) = SystemActions.appInfo(context, app)

    fun uninstall(app: AppEntry) = SystemActions.uninstall(context, app)

    // ---------------------------------------------------------------- Favoris, catégories, masquage

    fun toggleFavorite(app: AppEntry) = prefs.toggleFavorite(app.key)

    fun setCategory(app: AppEntry, category: AppCategory?) = prefs.setCategoryOverride(app.key, category)

    fun setHidden(app: AppEntry, hidden: Boolean) {
        prefs.setHidden(app.key, hidden)
        if (hidden) SystemActions.toast(context, "${app.label} est masquée (Paramètres › Apps masquées)")
    }

    // ---------------------------------------------------------------- Réglages et PIN

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = prefs.updateSettings(transform)

    fun checkPin(pin: String): Boolean = prefs.checkPin(pin)

    fun setPin(pin: String) {
        prefs.setPin(pin)
        _status.update { it.copy(hasPin = prefs.hasPin()) }
    }

    fun refreshSystemStatus() {
        _status.value = SystemStatus(
            isDefaultLauncher = SystemActions.isDefaultLauncher(context),
            notificationAccess = SystemActions.hasNotificationAccess(context),
            gestureService = GestureService.isEnabled,
            contactsPermission = ContactsSearch.hasPermission(context),
            hasPin = prefs.hasPin(),
        )
    }

    // ---------------------------------------------------------------- Widgets

    fun addWidget(id: Int, heightDp: Int) = prefs.addWidget(id, heightDp)

    fun removeWidget(id: Int) {
        prefs.removeWidget(id)
        WidgetHostManager.get(context).deleteId(id)
    }

    fun resizeWidget(id: Int, deltaDp: Int) {
        if (deltaDp != 0) prefs.resizeWidget(id, deltaDp)
    }

    override fun onCleared() {
        wallpaperManager.removeOnColorsChangedListener(colorsListener)
        repository.close()
    }
}
