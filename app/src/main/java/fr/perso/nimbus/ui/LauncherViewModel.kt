package fr.perso.nimbus.ui

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.AppRepository
import fr.perso.nimbus.data.BackupManager
import fr.perso.nimbus.data.ContactResult
import fr.perso.nimbus.data.ContactsSearch
import fr.perso.nimbus.data.CustomIcons
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.HomeIcon
import fr.perso.nimbus.data.IconConfig
import fr.perso.nimbus.data.IconFactory
import fr.perso.nimbus.data.ImportResult
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.data.ProfileState
import fr.perso.nimbus.data.ProfileType
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
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

/** Icône de la grille d'accueil, à sa case d'affichage. */
data class PlacedIcon(val app: AppEntry, val x: Int, val y: Int)

data class AppsState(
    val loaded: Boolean = false,
    /** Apps visibles (hors espace privé), triées selon le réglage (alphabétique ou usage). */
    val visible: List<AppEntry> = emptyList(),
    /** Apps masquées, par ordre alphabétique. */
    val hidden: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val hiddenKeys: Set<String> = emptySet(),
    /** Catégories qui contiennent au moins une app visible, dans l'ordre des onglets. */
    val categories: List<AppCategory> = emptyList(),
    /** Apps de l'espace privé (vide s'il est verrouillé). */
    val privateApps: List<AppEntry> = emptyList(),
    val privateSpace: ProfileState? = null,
    val workProfile: ProfileState? = null,
    /** Apps les plus utilisées, pour la ligne « Fréquentes » du tiroir. */
    val frequent: List<AppEntry> = emptyList(),
    val home: List<PlacedIcon> = emptyList(),
    val homeKeys: Set<String> = emptySet(),
    /** Toutes les apps connues (masquées comprises), par clé. */
    val byKey: Map<String, AppEntry> = emptyMap(),
)

data class SearchResults(
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val contacts: List<ContactResult> = emptyList(),
)

/** Réglages qui influencent la construction de [AppsState]. */
private data class AppsConfig(
    val sortMode: SortMode,
    val frequentCount: Int,
    val homeColumns: Int,
    val homeRows: Int,
)

private data class UserLists(
    val hidden: Set<String>,
    val usage: Map<String, Int>,
    val favorites: List<String>,
    val home: List<HomeIcon>,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Application get() = getApplication()
    private val prefs = Prefs.get(application)
    private val repository = AppRepository(application, viewModelScope, IconFactory(application))
    private val backup = BackupManager(application, prefs)
    private val wallpaperManager = WallpaperManager.getInstance(application)

    val settings: StateFlow<LauncherSettings> = prefs.settings
    val widgets: StateFlow<List<WidgetSlot>> = prefs.widgets
    val categoryOverrides: StateFlow<Map<String, AppCategory>> = prefs.categoryOverrides
    val customLabels: StateFlow<Map<String, String>> = prefs.customLabels
    val iconSwipeActions: StateFlow<Map<String, String>> = prefs.iconSwipeActions
    val gestureApps: StateFlow<Map<Gesture, String>> = prefs.gestureApps
    val autoBackupFolder: StateFlow<String?> = prefs.autoBackupFolder

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _status = MutableStateFlow(SystemStatus())
    val status: StateFlow<SystemStatus> = _status.asStateFlow()

    private val _wallpaperColor = MutableStateFlow<Int?>(null)
    /** Couleur dominante du fond d'écran (ARGB), ou null si inconnue (fond animé…). */
    val wallpaperColor: StateFlow<Int?> = _wallpaperColor.asStateFlow()

    /** Couleurs (fond, motif) des icônes à thème, fournies par le thème Compose. */
    private val themedColors = MutableStateFlow<Pair<Int, Int>?>(null)

    private val _customIconKeys = MutableStateFlow<Set<String>>(emptySet())
    /** Apps qui ont une icône personnalisée. */
    val customIconKeys: StateFlow<Set<String>> = _customIconKeys.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    /** Compte rendu du dernier import, affiché dans un dialogue. */
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val colorsListener = WallpaperManager.OnColorsChangedListener { colors, which ->
        if (which and WallpaperManager.FLAG_SYSTEM != 0) _wallpaperColor.value = colors.toArgb()
    }

    /** Une app a été lancée : le tiroir se referme quand le launcher passe en arrière-plan. */
    private var resetOnStop = false

    private val collator = Collator.getInstance(Locale.getDefault())

    val badges: StateFlow<Map<String, Int>> =
        combine(BadgeStore.counts, settings.map { it.badgesEnabled }.distinctUntilChanged()) { counts, enabled ->
            if (enabled) counts else emptyMap()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Noms personnalisés appliqués (et ordre alphabétique refait si besoin). */
    private val labeled = combine(repository.apps, prefs.customLabels) { apps, labels ->
        if (labels.isEmpty()) apps
        else apps.map { app -> labels[app.key]?.let { app.copy(label = it) } ?: app }
            .sortedWith(compareBy(collator) { it.label })
    }

    /** Apps avec leur catégorie finale : choix manuel, sinon classement automatique. */
    private val categorized = combine(
        labeled,
        prefs.categoryOverrides,
        settings.map { it.autoCategories }.distinctUntilChanged(),
    ) { apps, overrides, auto ->
        apps.map { app ->
            val category = overrides[app.key] ?: if (auto) app.autoCategory else AppCategory.OTHER
            if (category == app.category) app else app.copy(category = category)
        }
    }

    private val userLists = combine(prefs.hidden, prefs.usage, prefs.favorites, prefs.homeIcons) { hidden, usage, favorites, home ->
        UserLists(hidden, usage, favorites, home)
    }

    private val appsConfig = settings
        .map {
            AppsConfig(
                sortMode = it.sortMode,
                frequentCount = if (it.showFrequentApps) it.drawerColumns else 0,
                homeColumns = it.homeColumns,
                homeRows = it.homeRows,
            )
        }
        .distinctUntilChanged()

    val apps: StateFlow<AppsState> = combine(categorized, userLists, appsConfig, repository.profiles) { all, lists, config, profiles ->
        buildAppsState(all, lists, config, profiles)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppsState())

    private val searchIndex = combine(apps, repository.apps, prefs.customLabels) { state, raw, labels ->
        // Une app renommée se trouve aussi par son nom d'origine.
        val originals = if (labels.isEmpty()) emptyMap()
        else raw.filter { it.key in labels }.associate { it.key to it.label }
        SearchMatcher.index(state.visible + state.privateApps, originals)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        // Forme, pack, taille, icônes à thème : tout changement recharge les icônes du dépôt.
        // Avec les icônes à thème, on attend les couleurs du thème pour ne charger qu'une fois.
        combine(settings, themedColors) { s, colors -> s to colors }
            .mapNotNull { (s, colors) ->
                val themed = s.themedIcons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                if (themed && colors == null) return@mapNotNull null
                IconConfig(
                    shape = s.iconShape,
                    iconPack = s.iconPack,
                    wrapLegacy = s.wrapLegacyIcons,
                    sizePx = (s.iconSize.dp * context.resources.displayMetrics.density).roundToInt(),
                    themed = themed,
                    themeBackground = if (themed) colors!!.first else 0,
                    themeForeground = if (themed) colors!!.second else 0,
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

        refreshCustomIconKeys()
        refreshSystemStatus()
    }

    private fun WallpaperColors?.toArgb(): Int? = this?.primaryColor?.toArgb()

    private fun buildAppsState(
        all: List<AppEntry>,
        lists: UserLists,
        config: AppsConfig,
        profiles: List<ProfileState>,
    ): AppsState {
        val hidden = lists.hidden
        // La liste du dépôt est déjà alphabétique ; le tri par usage est stable,
        // donc les apps de même usage restent dans l'ordre alphabétique.
        val sorted = if (config.sortMode == SortMode.USAGE) all.sortedByDescending { lists.usage[it.key] ?: 0 } else all
        val visible = sorted.filter { it.profile != ProfileType.PRIVATE && it.key !in hidden }
        val byKey = all.associateBy { it.key }
        // Les apps de l'espace privé ne vont ni dans les favoris, ni sur l'accueil, ni dans « Fréquentes ».
        fun showable(app: AppEntry) = app.profile != ProfileType.PRIVATE && app.key !in hidden
        val frequent = if (config.frequentCount > 0 && config.sortMode == SortMode.ALPHA) {
            visible.filter { (lists.usage[it.key] ?: 0) > 0 }
                .sortedByDescending { lists.usage[it.key] ?: 0 }
                .take(config.frequentCount)
        } else {
            emptyList()
        }
        val home = placeHome(lists.home, byKey, ::showable, config.homeColumns, config.homeRows)
        return AppsState(
            loaded = all.isNotEmpty(),
            visible = visible,
            hidden = all.filter { it.key in hidden },
            favorites = lists.favorites.mapNotNull { byKey[it] }.filter(::showable),
            favoriteKeys = lists.favorites.toSet(),
            hiddenKeys = hidden,
            categories = AppCategory.entries.filter { cat -> visible.any { it.category == cat } },
            privateApps = sorted.filter { it.profile == ProfileType.PRIVATE && it.key !in hidden },
            privateSpace = profiles.firstOrNull { it.type == ProfileType.PRIVATE },
            workProfile = profiles.firstOrNull { it.type == ProfileType.WORK },
            frequent = frequent,
            home = home,
            homeKeys = lists.home.map { it.key }.toSet(),
            byKey = byKey,
        )
    }

    /**
     * Cases d'affichage de la grille : chaque icône garde sa case si elle est dans la grille
     * et libre ; les autres (grille réduite, collision) prennent les premières cases libres.
     * Rien n'est réécrit : agrandir de nouveau la grille rend leur place aux icônes.
     */
    private fun placeHome(
        icons: List<HomeIcon>,
        byKey: Map<String, AppEntry>,
        showable: (AppEntry) -> Boolean,
        columns: Int,
        rows: Int,
    ): List<PlacedIcon> {
        val taken = HashSet<Pair<Int, Int>>()
        val placed = ArrayList<PlacedIcon>()
        val overflow = ArrayList<AppEntry>()
        for (icon in icons) {
            val app = byKey[icon.key]?.takeIf(showable) ?: continue
            val cell = icon.x to icon.y
            if (icon.x in 0 until columns && icon.y in 0 until rows && taken.add(cell)) {
                placed.add(PlacedIcon(app, icon.x, icon.y))
            } else {
                overflow.add(app)
            }
        }
        if (overflow.isNotEmpty()) {
            val free = (0 until rows).flatMap { y -> (0 until columns).map { x -> x to y } }.filter { it !in taken }
            overflow.zip(free).forEach { (app, cell) -> placed.add(PlacedIcon(app, cell.first, cell.second)) }
        }
        return placed
    }

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
        autoBackupIfDue()
        if (!resetOnStop) return
        resetOnStop = false
        _ui.update {
            it.copy(drawerOpen = false, focusSearch = false, query = "", sheetApp = null, homeMenu = null)
        }
    }

    // ---------------------------------------------------------------- Gestes

    fun onGesture(gesture: Gesture) = perform(settings.value.actionFor(gesture), gesture)

    private fun perform(action: GestureAction, gesture: Gesture) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.LOCK -> if (!SystemActions.lockScreen()) askForGestureService()
            GestureAction.NOTIFICATIONS -> if (!SystemActions.expandNotifications(context)) askForGestureService()
            GestureAction.QUICK_SETTINGS -> if (!SystemActions.expandQuickSettings(context)) askForGestureService()
            GestureAction.RECENTS -> globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            GestureAction.SCREENSHOT -> globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            GestureAction.POWER_MENU -> globalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            GestureAction.ASSISTANT -> SystemActions.openAssistant(context)
            GestureAction.SEARCH -> openDrawer(focusSearch = true)
            GestureAction.DRAWER -> openDrawer()
            GestureAction.SETTINGS -> openSettings()
            GestureAction.WIDGETS -> showPage(1)
            GestureAction.APP -> {
                val app = prefs.gestureApps.value[gesture]?.let { apps.value.byKey[it] }
                if (app != null) launch(app)
                else SystemActions.toast(context, "Choisis l'app de ce geste dans Paramètres › Gestes")
            }
        }
    }

    private fun globalAction(action: Int) {
        if (!SystemActions.globalAction(action)) askForGestureService()
    }

    private fun askForGestureService() {
        SystemActions.toast(context, "Active « Gestes Nimbus » dans les réglages d'accessibilité")
        SystemActions.openAccessibilitySettings(context)
    }

    // ---------------------------------------------------------------- Lancements

    fun launch(app: AppEntry) {
        if (app.isPaused) {
            // Profil pro en pause : le système le réactive (et demande le code si besoin).
            SystemActions.toast(context, "Réactivation des apps professionnelles…")
            SystemActions.setQuietMode(context, app.user, false)
            return
        }
        prefs.incrementUsage(app.key)
        SystemActions.launch(context, app)
        resetOnStop = true
    }

    /** Glissé vers le haut sur une icône : ouvre l'app associée, s'il y en a une. */
    fun swipeTarget(app: AppEntry): AppEntry? =
        prefs.iconSwipeActions.value[app.key]?.let { apps.value.byKey[it] }

    fun openContact(contact: ContactResult) {
        ContactsSearch.open(context, contact)
        resetOnStop = true
    }

    fun webSearch(query: String) {
        if (query.isBlank()) return
        SystemActions.webSearch(context, query.trim(), settings.value.webSearchEngine)
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

    // ---------------------------------------------------------------- Personnalisation des apps

    fun toggleFavorite(app: AppEntry) = prefs.toggleFavorite(app.key)

    fun setCategory(app: AppEntry, category: AppCategory?) = prefs.setCategoryOverride(app.key, category)

    fun setHidden(app: AppEntry, hidden: Boolean) {
        prefs.setHidden(app.key, hidden)
        if (hidden) SystemActions.toast(context, "${app.label} est masquée (Paramètres › Apps masquées)")
    }

    /** Nom d'origine de l'app (avant renommage). */
    fun originalLabel(app: AppEntry): String =
        repository.apps.value.firstOrNull { it.key == app.key }?.label ?: app.label

    /** [label] vide ou identique au nom d'origine : retour au nom d'origine. */
    fun rename(app: AppEntry, label: String?) {
        val clean = label?.trim()?.takeIf { it.isNotEmpty() && it != originalLabel(app) }
        prefs.setCustomLabel(app.key, clean)
    }

    fun setIconSwipeAction(app: AppEntry, target: AppEntry?) = prefs.setIconSwipeAction(app.key, target?.key)

    fun setGestureApp(gesture: Gesture, app: AppEntry?) = prefs.setGestureApp(gesture, app?.key)

    fun setCustomIcon(app: AppEntry, image: ByteArray) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { CustomIcons.save(context, app.key, image) }
            if (ok) {
                repository.refreshIcons(listOf(app.key))
                refreshCustomIconKeys()
            } else {
                SystemActions.toast(context, "Image illisible")
            }
        }
    }

    fun resetCustomIcon(app: AppEntry) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { CustomIcons.remove(context, app.key) }
            repository.refreshIcons(listOf(app.key))
            refreshCustomIconKeys()
        }
    }

    private fun refreshCustomIconKeys() {
        viewModelScope.launch {
            _customIconKeys.value = withContext(Dispatchers.IO) { CustomIcons.all(context).keys }
        }
    }

    /** Couleurs des icônes à thème, déduites du thème Compose courant. */
    fun setThemedIconColors(background: Int, foreground: Int) {
        themedColors.value = background to foreground
    }

    // ---------------------------------------------------------------- Grille d'accueil

    fun addToHome(app: AppEntry) {
        val s = settings.value
        when {
            app.key in apps.value.homeKeys -> SystemActions.toast(context, "${app.label} est déjà sur l'accueil")
            prefs.addHomeIcon(app.key, s.homeColumns, s.homeRows) -> {
                SystemActions.toast(context, "${app.label} ajoutée à l'accueil")
                showPage(0)
            }
            else -> SystemActions.toast(context, "Plus de place sur l'accueil (agrandis la grille dans les paramètres)")
        }
    }

    fun removeFromHome(app: AppEntry) = prefs.removeHomeIcon(app.key)

    fun moveHomeIcon(app: AppEntry, x: Int, y: Int) = prefs.moveHomeIcon(app.key, x, y)

    // ---------------------------------------------------------------- Profil pro et espace privé

    fun setProfileQuiet(profile: ProfileState, quiet: Boolean) {
        SystemActions.setQuietMode(context, profile.user, quiet)
    }

    fun installInProfile(profile: ProfileState) = SystemActions.openAppMarket(context, profile.user)

    fun openPrivateSpaceSettings() = SystemActions.openPrivateSpaceSettings(context)

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

    // ---------------------------------------------------------------- Sauvegardes

    /** Contenu d'une sauvegarde Nimbus (à appeler hors du thread principal). */
    fun exportBackup(): ByteArray = backup.export()

    /** Importe une sauvegarde Nimbus ou Smart Launcher ; le compte rendu arrive dans [importResult]. */
    fun importBackup(bytes: ByteArray) {
        viewModelScope.launch {
            val keys = repository.apps.value.map { it.key }
            val result = withContext(Dispatchers.IO) { backup.import(bytes, keys) }
            if (result.iconKeys.isNotEmpty()) repository.refreshIcons(result.iconKeys)
            refreshCustomIconKeys()
            refreshSystemStatus()
            _importResult.value = result
        }
    }

    fun dismissImportResult() {
        _importResult.value = null
    }

    fun setAutoBackupFolder(uri: String?) {
        prefs.setAutoBackupFolder(uri)
        if (uri != null) {
            prefs.lastAutoBackup = 0L
            autoBackupIfDue()
        }
    }

    val lastAutoBackup: Long get() = prefs.lastAutoBackup

    private fun autoBackupIfDue() {
        if (prefs.autoBackupFolder.value == null) return
        viewModelScope.launch(Dispatchers.IO) { backup.autoBackupIfDue() }
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
