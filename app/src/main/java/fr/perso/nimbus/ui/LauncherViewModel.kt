package fr.perso.nimbus.ui

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppBrands
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.Category
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.data.AppRepository
import fr.perso.nimbus.data.BackupManager
import fr.perso.nimbus.data.ContactResult
import fr.perso.nimbus.data.ContactsSearch
import fr.perso.nimbus.data.FileResult
import fr.perso.nimbus.data.FilesSearch
import fr.perso.nimbus.data.CustomIcons
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.GlyphChoice
import fr.perso.nimbus.data.GlyphChoices
import fr.perso.nimbus.data.HomeCell
import fr.perso.nimbus.data.HomeIcon
import fr.perso.nimbus.data.HomeWidget
import fr.perso.nimbus.data.IconConfig
import fr.perso.nimbus.data.IconFactory
import fr.perso.nimbus.data.ImportResult
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.data.ProfileState
import fr.perso.nimbus.data.ProfileType
import fr.perso.nimbus.data.SortMode
import fr.perso.nimbus.dictation.ClaudeDictation
import fr.perso.nimbus.service.BadgeService
import fr.perso.nimbus.service.BadgeStore
import fr.perso.nimbus.service.NotificationItem
import fr.perso.nimbus.service.QuickToggles
import fr.perso.nimbus.service.DictationService
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.Collator
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Écran affiché au-dessus de l'accueil. */
enum class Screen { HOME, SETTINGS, HIDDEN_APPS }

/** Pages des paramètres (une page principale et ses sous-pages). */
enum class SettingsSection { MAIN, APPEARANCE, GESTURES, BACKUP, HOME, DRAWER, SEARCH, DICTATION, NOTIFICATIONS, PRIVACY, ABOUT }

/** Menu ouvert par appui long sur l'accueil, à la position du doigt, sur la page [page]. */
data class HomeMenu(val position: Offset, val page: Int)

/** Demande d'affichage d'une page de l'accueil ; [id] change à chaque demande. */
data class PageRequest(val page: Int, val id: Long)

/** Pop-up d'appui long sur une app, ancré sur son icône (coordonnées de la fenêtre). */
/** [paperSlot] : appui long sur une place d'un accueil « papier » (le pop-up propose d'en changer l'app). */
data class AppPopupRequest(val app: AppEntry, val anchor: Rect, val paperSlot: PaperSlot? = null)

data class UiState(
    val drawerOpen: Boolean = false,
    /** Le champ de recherche du tiroir doit prendre le focus à l'ouverture. */
    val focusSearch: Boolean = false,
    val query: String = "",
    val screen: Screen = Screen.HOME,
    val settingsSection: SettingsSection = SettingsSection.MAIN,
    /** Page par laquelle les paramètres ont été ouverts : Retour depuis celle-ci referme les paramètres. */
    val settingsRoot: SettingsSection = SettingsSection.MAIN,
    /** App dont la feuille d'actions complète est ouverte (« Plus »). */
    val sheetApp: AppEntry? = null,
    /** Pop-up compact de l'appui long sur une app. */
    val appPopup: AppPopupRequest? = null,
    val homeMenu: HomeMenu? = null,
    val widgetPickerOpen: Boolean = false,
    /** Widget de l'accueil en cours de redimensionnement, null sinon. */
    val resizingWidget: Int? = null,
    val pageRequest: PageRequest = PageRequest(0, 0),
    /** Place d'un accueil « papier » dont on choisit l'app. */
    val paperSlotPicker: PaperSlot? = null,
    /** Place d'un accueil « papier » dont on échange l'app avec une autre place de la même famille. */
    val paperSwap: PaperSlot? = null,
    /** Bandeau papier (réglages rapides et notifications) ouvert. */
    val panelOpen: Boolean = false,
)

/** État des autorisations et services système, relu à chaque retour au premier plan. */
data class SystemStatus(
    val isDefaultLauncher: Boolean = false,
    val notificationAccess: Boolean = false,
    val gestureService: Boolean = false,
    val contactsPermission: Boolean = false,
    val filesPermission: Boolean = false,
    val dictationService: Boolean = false,
    val hasPin: Boolean = false,
    /** Xiaomi : la page de déverrouillage a le droit de passer devant l'écran verrouillé (toujours vrai ailleurs). */
    val lockScreenAllowed: Boolean = true,
)

/** Icône de la grille d'accueil, à sa case d'affichage. */
data class PlacedIcon(val app: AppEntry, val page: Int, val x: Int, val y: Int) {
    val cell: HomeCell get() = HomeCell(page, x, y)
}

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
    val categories: List<Category> = emptyList(),
    /** Toutes les catégories réglées (vides comprises), dans l'ordre des onglets. */
    val allCategories: List<Category> = Category.defaults,
    /** Apps de l'espace privé (vide s'il est verrouillé). */
    val privateApps: List<AppEntry> = emptyList(),
    val privateSpace: ProfileState? = null,
    val workProfile: ProfileState? = null,
    /** Apps les plus utilisées, pour la ligne « Fréquentes » du tiroir. */
    val frequent: List<AppEntry> = emptyList(),
    /** Apps visibles déjà ouvertes, de la plus utilisée à la moins utilisée (figure du sommaire). */
    val mostUsed: List<AppEntry> = emptyList(),
    val home: List<PlacedIcon> = emptyList(),
    val homeKeys: Set<String> = emptySet(),
    /** Widgets de la grille d'accueil, ramenés dans les limites de la grille. */
    val homeWidgets: List<HomeWidget> = emptyList(),
    /** Nombre de pages de l'accueil (au moins 1) : jusqu'à la dernière qui contient quelque chose. */
    val pageCount: Int = 1,
    /** Page principale (bouton Accueil, ouverture). */
    val homePage: Int = 0,
    /** Toutes les apps connues (masquées comprises), par clé. */
    val byKey: Map<String, AppEntry> = emptyMap(),
) {
    private val labels: Map<String, String> by lazy { allCategories.associate { it.id to it.label } }

    /** Nom affiché de la catégorie [id]. */
    fun categoryLabel(id: String): String =
        labels[id] ?: AppCategory.entries.firstOrNull { it.name == id }?.label ?: id
}

data class SearchResults(
    val query: String = "",
    val apps: List<AppEntry> = emptyList(),
    val contacts: List<ContactResult> = emptyList(),
    val files: List<FileResult> = emptyList(),
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
    val homeWidgets: List<HomeWidget>,
    val homePage: Int,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Application get() = getApplication()
    private val prefs = Prefs.get(application)
    private val repository = AppRepository(application, viewModelScope, IconFactory(application))
    private val backup = BackupManager(application, prefs)
    private val wallpaperManager = WallpaperManager.getInstance(application)

    val settings: StateFlow<LauncherSettings> = prefs.settings
    val categoryOverrides: StateFlow<Map<String, String>> = prefs.categoryOverrides
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

    /**
     * Apps avec leur catégorie finale : choix manuel, sinon classement automatique ; « Autres » si la
     * catégorie automatique a été supprimée. Avec la liste des catégories réglées.
     */
    private val categorized = combine(
        labeled,
        prefs.categoryOverrides,
        settings.map { it.autoCategories }.distinctUntilChanged(),
        prefs.categories,
    ) { apps, overrides, auto, categories ->
        val ids = categories.mapTo(HashSet()) { it.id }
        val list = apps.map { app ->
            val automatic = if (auto && app.autoCategory.name in ids) app.autoCategory.name else Category.OTHER_ID
            val category = overrides[app.key]?.takeIf { it in ids } ?: automatic
            if (category == app.category) app else app.copy(category = category)
        }
        list to categories
    }

    private val homeLayout = combine(prefs.homeIcons, prefs.homeWidgets, prefs.homePage) { icons, widgets, page ->
        Triple(icons, widgets, page)
    }

    private val userLists = combine(
        prefs.hidden,
        prefs.usage,
        prefs.favorites,
        homeLayout,
    ) { hidden, usage, favorites, (home, homeWidgets, homePage) ->
        UserLists(hidden, usage, favorites, home, homeWidgets, homePage)
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

    val apps: StateFlow<AppsState> = combine(categorized, userLists, appsConfig, repository.profiles) { (all, categories), lists, config, profiles ->
        buildAppsState(all, categories, lists, config, profiles)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppsState())

    /** App de chaque place des accueils « papier » (choix de l'utilisateur, sinon automatique). */
    val paperApps: StateFlow<Map<PaperSlot, AppEntry?>> = combine(apps, prefs.paperSlots) { state, chosen ->
        PaperSlot.resolve(state.visible, chosen, state.byKey)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * Motifs des apps : choix de l'utilisateur, sinon motif de la place qu'occupe l'app (logo YouTube…),
     * sinon celui de [AppBrands] (applis Google en couleur, BlaBlaCar détouré…).
     */
    val glyphChoices: StateFlow<GlyphChoices> = combine(prefs.glyphChoices, paperApps, apps) { chosen, slots, state ->
        val map = HashMap<String, GlyphChoice>()
        for (app in state.byKey.values) AppBrands.glyph(app)?.let { map[app.key] = it }
        for ((slot, app) in slots) {
            val glyph = slot.glyph
            if (app != null && glyph != null) map[app.key] = glyph
        }
        for ((key, spec) in chosen) GlyphChoice.parse(spec)?.let { map[key] = it }
        GlyphChoices(map, state.byKey)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GlyphChoices())

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

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val files: StateFlow<List<FileResult>> = combine(
        ui.map { it.query.trim() }.distinctUntilChanged(),
        settings.map { it.searchFiles }.distinctUntilChanged(),
        status.map { it.filesPermission }.distinctUntilChanged(),
    ) { query, enabled, granted -> Triple(query, enabled, granted) }
        .debounce(200)
        .mapLatest { (query, enabled, granted) ->
            // Une ou deux lettres donneraient des centaines de fichiers sans rapport.
            if (query.length < 3 || !enabled || !granted) emptyList()
            else withContext(Dispatchers.IO) { FilesSearch.search(context, query) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchResults: StateFlow<SearchResults> = combine(
        searchIndex,
        ui.map { it.query }.distinctUntilChanged(),
        contacts,
        files,
    ) { index, query, contacts, files ->
        if (query.isBlank()) SearchResults()
        else SearchResults(query, SearchMatcher.search(index, query), contacts, files)
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

        // Une app qui disparaît (désinstallée, espace privé verrouillé) ferme sa feuille d'actions.
        apps.onEach { state ->
            _ui.update { ui ->
                ui.copy(
                    sheetApp = ui.sheetApp?.takeIf { it.key in state.byKey },
                    appPopup = ui.appPopup?.takeIf { it.app.key in state.byKey },
                )
            }
        }.launchIn(viewModelScope)

        // Au démarrage, l'accueil s'ouvre sur sa page principale.
        viewModelScope.launch {
            val first = apps.first { it.loaded }
            if (first.homePage > 0) showHomePage(first.homePage)
        }

        migrateWidgetsPage()
        refreshCustomIconKeys()
        refreshSystemStatus()
    }

    private fun WallpaperColors?.toArgb(): Int? = this?.primaryColor?.toArgb()

    private fun buildAppsState(
        all: List<AppEntry>,
        categories: List<Category>,
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
        val homeWidgets = fitWidgets(lists.homeWidgets, config.homeColumns, config.homeRows)
        val home = placeHome(lists.home, byKey, ::showable, config.homeColumns, config.homeRows, homeWidgets)
        return AppsState(
            loaded = all.isNotEmpty(),
            visible = visible,
            hidden = all.filter { it.key in hidden },
            favorites = lists.favorites.mapNotNull { byKey[it] }.filter(::showable),
            favoriteKeys = lists.favorites.toSet(),
            hiddenKeys = hidden,
            categories = categories.filter { cat -> visible.any { it.category == cat.id } },
            allCategories = categories,
            privateApps = sorted.filter { it.profile == ProfileType.PRIVATE && it.key !in hidden },
            privateSpace = profiles.firstOrNull { it.type == ProfileType.PRIVATE },
            workProfile = profiles.firstOrNull { it.type == ProfileType.WORK },
            frequent = frequent,
            mostUsed = visible.filter { (lists.usage[it.key] ?: 0) > 0 }.sortedByDescending { lists.usage[it.key] ?: 0 },
            home = home,
            homeKeys = lists.home.map { it.key }.toSet(),
            homeWidgets = homeWidgets,
            pageCount = (home.map { it.page } + homeWidgets.map { it.page } + lists.homePage).max() + 1,
            homePage = lists.homePage,
            byKey = byKey,
        )
    }

    /**
     * Cases d'affichage de la grille : chaque icône garde sa case si elle est dans la grille
     * et libre ; les autres (grille réduite, collision) prennent la première case libre de leur
     * page, sinon des pages suivantes. Rien n'est réécrit : agrandir de nouveau la grille rend
     * leur place aux icônes.
     */
    private fun placeHome(
        icons: List<HomeIcon>,
        byKey: Map<String, AppEntry>,
        showable: (AppEntry) -> Boolean,
        columns: Int,
        rows: Int,
        widgets: List<HomeWidget>,
    ): List<PlacedIcon> {
        // Les widgets passent en premier : une icône sous un widget prend la première case libre.
        val taken = HashSet<HomeCell>()
        for (w in widgets) w.cells().forEach { (x, y) -> taken.add(HomeCell(w.page, x, y)) }
        val placed = ArrayList<PlacedIcon>()
        val overflow = ArrayList<Pair<AppEntry, Int>>()
        for (icon in icons) {
            val app = byKey[icon.key]?.takeIf(showable) ?: continue
            if (icon.x in 0 until columns && icon.y in 0 until rows && taken.add(HomeCell(icon.page, icon.x, icon.y))) {
                placed.add(PlacedIcon(app, icon.page, icon.x, icon.y))
            } else {
                overflow.add(app to icon.page)
            }
        }
        for ((app, page) in overflow) {
            val cell = firstFree(taken, page, columns, rows)
            taken.add(cell)
            placed.add(PlacedIcon(app, cell.page, cell.x, cell.y))
        }
        return placed
    }

    /** Première case libre à partir de la page [fromPage] (les pages suivantes au besoin, jusqu'à une page vide). */
    private fun firstFree(taken: Set<HomeCell>, fromPage: Int, columns: Int, rows: Int): HomeCell {
        var page = fromPage
        while (true) {
            for (y in 0 until rows) for (x in 0 until columns) {
                val cell = HomeCell(page, x, y)
                if (cell !in taken) return cell
            }
            page++
        }
    }

    /** Widgets ramenés dans la grille (après réduction de la grille, par exemple). */
    private fun fitWidgets(widgets: List<HomeWidget>, columns: Int, rows: Int): List<HomeWidget> = widgets.map {
        val spanX = it.spanX.coerceIn(1, columns)
        val spanY = it.spanY.coerceIn(1, rows)
        it.copy(x = it.x.coerceIn(0, columns - spanX), y = it.y.coerceIn(0, rows - spanY), spanX = spanX, spanY = spanY)
    }

    // ---------------------------------------------------------------- Navigation

    fun openDrawer(focusSearch: Boolean = false) = _ui.update {
        it.copy(drawerOpen = true, focusSearch = focusSearch, homeMenu = null)
    }

    fun closeDrawer() = _ui.update { it.copy(drawerOpen = false, focusSearch = false, query = "") }

    // ---------------------------------------------------------------- Bandeau papier

    /** Notifications du bandeau papier, les plus récentes d'abord. */
    val notifications: StateFlow<List<NotificationItem>> = BadgeStore.notifications

    fun openPanel() = _ui.update { it.copy(panelOpen = true, homeMenu = null, drawerOpen = false) }

    fun closePanel() = _ui.update { it.copy(panelOpen = false) }

    /** Ouvre une notification (puis l'efface si elle le demande) et referme le bandeau. */
    fun openNotification(item: NotificationItem) {
        closePanel()
        val intent = item.intent
        if (intent == null || !QuickToggles.send(context, intent)) {
            context.packageManager.getLaunchIntentForPackage(item.packageName)?.let {
                runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        if (item.autoCancel && item.clearable) BadgeService.dismiss(item.key)
    }

    fun dismissNotification(item: NotificationItem) {
        if (item.clearable) BadgeService.dismiss(item.key)
    }

    fun dismissAllNotifications() {
        BadgeService.dismissAll()
    }

    /** Le bandeau du système, pour ce que le bandeau papier ne montre pas. */
    fun openSystemPanel(quickSettings: Boolean) {
        closePanel()
        val ok = if (quickSettings) SystemActions.expandQuickSettings(context) else SystemActions.expandNotifications(context)
        if (!ok) askForGestureService()
    }

    fun setQuery(query: String) = _ui.update { it.copy(query = query) }

    fun consumeSearchFocus() = _ui.update { it.copy(focusSearch = false) }

    fun openSettings(section: SettingsSection = SettingsSection.MAIN) = _ui.update {
        it.copy(
            screen = Screen.SETTINGS,
            settingsSection = section,
            settingsRoot = section,
            drawerOpen = false,
            query = "",
            homeMenu = null,
            resizingWidget = null,
        )
    }

    fun openSettingsSection(section: SettingsSection) = _ui.update { it.copy(settingsSection = section) }

    /** À n'appeler qu'après vérification du code PIN. */
    fun openHiddenApps() = _ui.update { it.copy(screen = Screen.HIDDEN_APPS) }

    /**
     * Retour d'un cran : apps masquées -> sous-page -> page principale des réglages -> accueil.
     * Une sous-page ouverte directement (menu de l'accueil) ramène à l'accueil.
     */
    fun closeScreen() = _ui.update {
        when {
            it.screen == Screen.HIDDEN_APPS -> it.copy(screen = Screen.SETTINGS)
            it.screen == Screen.SETTINGS && it.settingsSection != it.settingsRoot ->
                it.copy(settingsSection = it.settingsRoot)
            else -> it.copy(screen = Screen.HOME)
        }
    }

    fun showAppSheet(app: AppEntry) = _ui.update { it.copy(sheetApp = app, appPopup = null) }

    fun showAppPopup(app: AppEntry, anchor: Rect, paperSlot: PaperSlot? = null) =
        _ui.update { it.copy(appPopup = AppPopupRequest(app, anchor, paperSlot)) }

    fun showPaperSlotPicker(slot: PaperSlot?) = _ui.update { it.copy(paperSlotPicker = slot, appPopup = null) }

    /** [app] null : la place revient au choix automatique. */
    fun assignPaperSlot(slot: PaperSlot, app: AppEntry?) = prefs.setPaperSlot(slot, app?.key)

    fun showPaperSwap(slot: PaperSlot?) = _ui.update { it.copy(paperSwap = slot, appPopup = null) }

    /**
     * Échange les applis de deux places (de la même famille) : chacune prend l'appli de l'autre, choisie
     * à la main pour que le choix automatique ne la remette pas. Une place vide le reste… à l'autre bout.
     */
    fun swapPaperSlots(a: PaperSlot, b: PaperSlot) {
        val apps = paperApps.value
        val appA = apps[a]
        val appB = apps[b]
        prefs.setPaperSlot(a, appB?.key)
        prefs.setPaperSlot(b, appA?.key)
    }

    /** Accueil « Voronoï » : sites déplacés par l'utilisateur. */
    val voronoiSites: StateFlow<Map<PaperSlot, Offset>> = prefs.voronoiSites

    fun setVoronoiSites(sites: Map<PaperSlot, Offset>) = prefs.setVoronoiSites(sites)

    /** Accueil « Ensembles » : pastilles déplacées (repère de la figure). */
    val ensemblesSites: StateFlow<Map<PaperSlot, Offset>> = prefs.ensemblesSites

    fun setEnsemblesSites(sites: Map<PaperSlot, Offset>) = prefs.setEnsemblesSites(sites)

    /** Un accueil « papier » passe aussi l'interface au thème papier (on peut en changer ensuite). */
    fun setHomeStyle(style: HomeStyle) = updateSettings {
        it.copy(homeStyle = style, themeMode = if (style.paper) ThemeMode.PAPER else it.themeMode)
    }

    fun dismissAppPopup() = _ui.update { it.copy(appPopup = null) }

    fun dismissAppSheet() = _ui.update { it.copy(sheetApp = null) }

    fun openHomeMenu(position: Offset) = _ui.update { it.copy(homeMenu = HomeMenu(position, visiblePage)) }

    fun closeHomeMenu() = _ui.update { it.copy(homeMenu = null) }

    fun openWidgetPicker() = _ui.update {
        it.copy(widgetPickerOpen = true, homeMenu = null, resizingWidget = null)
    }

    fun closeWidgetPicker() = _ui.update { it.copy(widgetPickerOpen = false) }

    /** Page de l'accueil affichée (signalée par l'interface) : les ajouts y vont en priorité. */
    private var visiblePage = 0

    fun onHomePageShown(page: Int) {
        visiblePage = page.coerceAtLeast(0)
    }

    /** La page [page] devient la page principale (bouton Accueil). */
    fun setMainHomePage(page: Int) {
        prefs.setHomePage(page)
        SystemActions.toast(context, "Page ${page + 1} définie comme page principale")
    }

    fun showHomePage(page: Int) = _ui.update { it.copy(pageRequest = PageRequest(page, it.pageRequest.id + 1)) }

    /** Mode redimensionnement d'un seul widget (poignée), null pour en sortir. */
    fun setResizingWidget(id: Int?) = _ui.update { it.copy(resizingWidget = id, homeMenu = null) }

    /** Bouton Accueil : tout refermer et revenir à la page principale. */
    fun goHome() {
        resetOnStop = false
        _ui.update { UiState(pageRequest = PageRequest(prefs.homePage.value, it.pageRequest.id + 1)) }
    }

    fun onLauncherStopped() {
        autoBackupIfDue()
        if (!resetOnStop) return
        resetOnStop = false
        _ui.update {
            it.copy(drawerOpen = false, focusSearch = false, query = "", sheetApp = null, appPopup = null, homeMenu = null)
        }
    }

    // ---------------------------------------------------------------- Gestes

    fun onGesture(gesture: Gesture) = perform(settings.value.actionFor(gesture), gesture)

    private fun perform(action: GestureAction, gesture: Gesture) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.LOCK -> if (!SystemActions.lockScreen()) askForGestureService()
            GestureAction.NOTIFICATIONS -> when {
                settings.value.themeMode == ThemeMode.PAPER && settings.value.paperPanel -> openPanel()
                !SystemActions.expandNotifications(context) -> askForGestureService()
            }
            GestureAction.QUICK_SETTINGS -> if (!SystemActions.expandQuickSettings(context)) askForGestureService()
            GestureAction.RECENTS -> globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            GestureAction.SCREENSHOT -> globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            GestureAction.POWER_MENU -> globalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            GestureAction.ASSISTANT -> SystemActions.openAssistant(context)
            GestureAction.SEARCH -> openDrawer(focusSearch = true)
            GestureAction.DRAWER -> openDrawer()
            GestureAction.SETTINGS -> openSettings()
            GestureAction.WIDGETS -> openWidgetPicker()
            GestureAction.DICTATION -> ClaudeDictation.start(context)
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
        // L'usage des apps de l'espace privé n'est pas enregistré : il finirait dans les sauvegardes.
        if (app.profile != ProfileType.PRIVATE) prefs.incrementUsage(app.key)
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

    fun openFile(file: FileResult) {
        if (FilesSearch.open(context, file)) resetOnStop = true
        else SystemActions.toast(context, "Aucune app pour ouvrir ce fichier")
    }

    fun playStoreSearch(query: String) {
        if (query.isBlank()) return
        SystemActions.playStoreSearch(context, query.trim())
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

    /** [category] : identifiant d'une [Category] ; null : classement automatique. */
    fun setCategory(app: AppEntry, category: String?) = prefs.setCategoryOverride(app.key, category)

    // ---------------------------------------------------------------- Catégories (réglages)

    val categories: StateFlow<List<Category>> = prefs.categories

    /** Nouvelle catégorie, à la fin des onglets (avant « Autres »). */
    fun addCategory(label: String): Category? {
        val clean = label.trim().ifEmpty { return null }
        val list = prefs.categories.value
        list.firstOrNull { it.label.equals(clean, ignoreCase = true) }?.let { return it }
        val category = Category("c" + System.currentTimeMillis().toString(36), clean)
        val other = list.indexOfFirst { it.id == Category.OTHER_ID }
        prefs.setCategories(if (other == list.lastIndex) list.dropLast(1) + category + list.last() else list + category)
        return category
    }

    fun renameCategory(id: String, label: String) {
        val clean = label.trim().ifEmpty { return }
        prefs.setCategories(prefs.categories.value.map { if (it.id == id) it.copy(label = clean) else it })
    }

    /** Supprime une catégorie (sauf « Autres ») : ses apps reviennent au classement automatique. */
    fun deleteCategory(id: String) {
        if (id == Category.OTHER_ID) return
        prefs.setCategories(prefs.categories.value.filterNot { it.id == id })
    }

    /** Déplace une catégorie d'un cran ([delta] = -1 : vers le début des onglets). */
    fun moveCategory(id: String, delta: Int) {
        val list = prefs.categories.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        prefs.setCategories(list)
    }

    /** Catégories prédéfinies, avec leur nom et leur ordre d'origine (les apps rangées à la main le restent si possible). */
    fun resetCategories() = prefs.setCategories(Category.defaults)

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

    /** L'icône de [source] devient celle de [app] (icône personnalisée, comme une image de la galerie). */
    fun setIconFromApp(app: AppEntry, source: AppEntry) {
        viewModelScope.launch {
            val png = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().also { source.icon.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            }
            setCustomIcon(app, png)
        }
    }

    /** Motif de [app] sur les pages papier et dans le sommaire ; null : automatique. */
    fun setGlyphChoice(app: AppEntry, choice: GlyphChoice?) = prefs.setGlyphChoice(app.key, choice)

    /** Motif choisi par l'utilisateur pour [app] (null : automatique). */
    fun userGlyphChoice(app: AppEntry): GlyphChoice? = prefs.glyphChoices.value[app.key]?.let(GlyphChoice::parse)

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

    /** Cases occupées de la grille affichée (icônes et widgets, toutes pages). */
    private fun takenCells(state: AppsState): MutableSet<HomeCell> {
        val taken = state.home.mapTo(HashSet()) { it.cell }
        for (w in state.homeWidgets) w.cells().forEach { (x, y) -> taken.add(HomeCell(w.page, x, y)) }
        return taken
    }

    /**
     * Première case libre, page [first] d'abord, puis les suivantes, les précédentes, et enfin
     * une nouvelle page. [first] au-delà des pages existantes : la nouvelle page directement.
     */
    private fun firstFreeCell(taken: Set<HomeCell>, first: Int, pageCount: Int): HomeCell {
        if (first >= pageCount) return HomeCell(pageCount, 0, 0)
        val s = settings.value
        for (page in (first until pageCount) + (0 until first)) {
            for (y in 0 until s.homeRows) for (x in 0 until s.homeColumns) {
                val cell = HomeCell(page, x, y)
                if (cell !in taken) return cell
            }
        }
        return HomeCell(pageCount, 0, 0)
    }

    /**
     * Supprime les pages vides (la page principale reste toujours) en renumérotant les autres.
     * Retourne la nouvelle numérotation, pour suivre un élément qui vient d'être posé.
     */
    private fun compactPages(): (Int) -> Int {
        val state = apps.value
        // Icône d'une app masquée ou absente : invisible, elle ne retient pas sa page.
        fun shown(icon: HomeIcon) = !state.loaded ||
            state.byKey[icon.key]?.let { it.profile != ProfileType.PRIVATE && it.key !in state.hiddenKeys } == true
        val icons = prefs.homeIcons.value
        val widgets = prefs.homeWidgets.value
        val mainPage = prefs.homePage.value
        val used = (icons.filter(::shown).map { it.page } + widgets.map { it.page } + mainPage).toSortedSet()
        val remap = used.withIndex().associate { (index, page) -> page to index }
        val newPage = { page: Int -> remap[page] ?: remap.getValue(mainPage) }
        if (icons.any { newPage(it.page) != it.page }) prefs.setHomeIcons(icons.map { it.copy(page = newPage(it.page)) })
        if (widgets.any { newPage(it.page) != it.page }) prefs.setHomeWidgets(widgets.map { it.copy(page = newPage(it.page)) })
        if (newPage(mainPage) != mainPage) prefs.setHomePage(newPage(mainPage))
        return newPage
    }

    /**
     * Page demandée avant la première ([HomeCell.page] négative : page vide créée à gauche
     * pendant un glissé) : toutes les pages, la principale comprise, sont décalées d'un cran
     * pour lui faire place. Retourne la case, ramenée sur la nouvelle première page.
     */
    private fun makeRoomBefore(cell: HomeCell): HomeCell {
        if (cell.page >= 0) return cell
        prefs.setHomeIcons(prefs.homeIcons.value.map { it.copy(page = it.page + 1) })
        prefs.setHomeWidgets(prefs.homeWidgets.value.map { it.copy(page = it.page + 1) })
        prefs.setHomePage(prefs.homePage.value + 1)
        return cell.copy(page = 0)
    }

    /**
     * Ajoute l'app à la première case libre, en commençant par la page [page] (celle affichée par
     * défaut) ; une nouvelle page est créée si tout est plein. Retourne la page de l'icône.
     */
    fun addToHome(app: AppEntry, page: Int = visiblePage): Int? {
        val state = apps.value
        if (state.home.any { it.app.key == app.key }) {
            SystemActions.toast(context, "${app.label} est déjà sur l'accueil")
            return null
        }
        val cell = if (page < 0) makeRoomBefore(HomeCell(page, 0, 0)) else firstFreeCell(takenCells(state), page, state.pageCount)
        prefs.setHomeIconPositions(mapOf(app.key to cell))
        SystemActions.toast(context, if (cell.page == page || page < 0) "${app.label} ajoutée à l'accueil" else "${app.label} ajoutée à l'accueil, page ${cell.page + 1}")
        if (cell.page != visiblePage) showHomePage(cell.page)
        return cell.page
    }

    fun removeFromHome(app: AppEntry) {
        prefs.removeHomeIcon(app.key)
        compactPages()
    }

    /**
     * App lâchée depuis le tiroir sur la case [cell]. Déjà sur l'accueil : simple déplacement.
     * Case occupée par une icône : celle-ci prend la première case libre ; par un widget :
     * l'app va à la première case libre. Retourne la page de l'app (null si rien n'a changé).
     */
    fun dropOnHome(app: AppEntry, cell: HomeCell): Int? {
        if (app.profile == ProfileType.PRIVATE) return null
        val state = apps.value
        if (state.home.any { it.app.key == app.key }) return moveHomeIcon(app, cell)
        if (cell.page < 0) {
            prefs.setHomeIconPositions(mapOf(app.key to makeRoomBefore(cell)))
            return 0
        }
        val taken = takenCells(state)
        if (state.homeWidgets.any { it.page == cell.page && (cell.x to cell.y) in it.cells() }) {
            val free = firstFreeCell(taken, cell.page, state.pageCount)
            prefs.setHomeIconPositions(mapOf(app.key to free))
            return free.page
        }
        val moves = mutableMapOf(app.key to cell)
        state.home.firstOrNull { it.cell == cell }?.let { occupant ->
            moves[occupant.app.key] = firstFreeCell(taken + cell, cell.page, maxOf(state.pageCount, cell.page + 1))
        }
        prefs.setHomeIconPositions(moves)
        return cell.page
    }

    /**
     * Déplace une icône vers la case [to], éventuellement sur une autre page (ou une page
     * nouvelle). L'icône affichée dans cette case prend la case qu'occupait l'icône déplacée.
     * Tout est calculé sur la grille affichée, pas sur les positions enregistrées (qui peuvent
     * être hors d'une grille réduite). Retourne la page finale de l'icône (null si rien n'a changé).
     */
    fun moveHomeIcon(app: AppEntry, to: HomeCell): Int? {
        val state = apps.value
        val from = state.home.firstOrNull { it.app.key == app.key } ?: return null
        if (to.page < 0) {
            prefs.setHomeIconPositions(mapOf(app.key to makeRoomBefore(to)))
            return compactPages()(0)
        }
        if (from.cell == to) return null
        // Pas d'icône sous un widget.
        if (state.homeWidgets.any { it.page == to.page && (to.x to to.y) in it.cells() }) return null
        val occupant = state.home.firstOrNull { it.cell == to }
        val moves = mutableMapOf(app.key to to)
        if (occupant != null) moves[occupant.app.key] = from.cell
        prefs.setHomeIconPositions(moves)
        return compactPages()(to.page)
    }

    // ---------------------------------------------------------------- Widgets de l'accueil

    /**
     * Pose un widget sur la grille, à la taille demandée. Ordre de recherche : emplacement
     * [preferred] (import, icônes décalées au besoin), puis une zone libre de la page affichée,
     * des autres pages, et sinon une nouvelle page. Retourne la page du widget.
     * Les widgets déjà posés sont lus dans les préférences, à jour immédiatement : plusieurs
     * ajouts d'affilée (import, migration) ne se superposent pas.
     */
    fun addHomeWidget(id: Int, spanX: Int, spanY: Int, preferred: HomeCell? = null): Int {
        val s = settings.value
        val columns = s.homeColumns
        val rows = s.homeRows
        val widgets = fitWidgets(prefs.homeWidgets.value, columns, rows).filter { it.id != id }
        val icons = if (apps.value.loaded) apps.value.home.map { it.cell }.toSet()
        else prefs.homeIcons.value.map { HomeCell(it.page, it.x, it.y) }.toSet()
        val pageCount = maxOf(apps.value.pageCount, (widgets.map { it.page } + icons.map { it.page }).maxOrNull()?.plus(1) ?: 1)
        fun fits(w: HomeWidget, avoidIcons: Boolean) =
            w.x >= 0 && w.y >= 0 && w.x + w.spanX <= columns && w.y + w.spanY <= rows &&
                widgets.none { it.overlaps(w) } && (!avoidIcons || w.cells().none { (x, y) -> HomeCell(w.page, x, y) in icons })
        fun place(w: HomeWidget): Int {
            prefs.putHomeWidget(w)
            return w.page
        }

        val sx = spanX.coerceIn(1, columns)
        val sy = spanY.coerceIn(1, rows)
        preferred?.let { cell ->
            val w = HomeWidget(id, cell.x, cell.y, sx, sy, cell.page.coerceAtLeast(0))
            if (fits(w, avoidIcons = false)) return place(w)
        }
        val first = (preferred?.page ?: visiblePage).coerceIn(0, pageCount)
        for (page in (first until pageCount) + (0 until first)) {
            for (y in 0..rows - sy) for (x in 0..columns - sx) {
                val w = HomeWidget(id, x, y, sx, sy, page)
                if (fits(w, avoidIcons = true)) return place(w)
            }
        }
        return place(HomeWidget(id, 0, 0, sx, sy, pageCount))
    }

    /**
     * Déplace (éventuellement vers une autre page) ou redimensionne un widget de l'accueil ;
     * refusé s'il sort de la grille ou chevauche un autre widget. Retourne la page finale du
     * widget (null si refusé ou inchangé).
     */
    fun updateHomeWidget(widget: HomeWidget): Int? {
        val s = settings.value
        val ok = widget.page >= 0 && widget.x >= 0 && widget.y >= 0 && widget.spanX >= 1 && widget.spanY >= 1 &&
            widget.x + widget.spanX <= s.homeColumns && widget.y + widget.spanY <= s.homeRows &&
            apps.value.homeWidgets.none { it.id != widget.id && it.overlaps(widget) }
        if (!ok) {
            SystemActions.toast(context, "Pas de place : un autre widget occupe cette zone")
            return null
        }
        if (apps.value.homeWidgets.any { it == widget }) return null
        prefs.putHomeWidget(widget)
        return compactPages()(widget.page)
    }

    /** Déplace un widget de l'accueil vers [to] (coin haut gauche), même page ou non. */
    fun moveHomeWidget(id: Int, to: HomeCell): Int? {
        val widget = apps.value.homeWidgets.firstOrNull { it.id == id } ?: return null
        if (to.page < 0) {
            // Nouvelle page vide : aucun widget à chevaucher.
            val cell = makeRoomBefore(to)
            prefs.putHomeWidget(widget.copy(page = cell.page, x = cell.x, y = cell.y))
            return compactPages()(cell.page)
        }
        return updateHomeWidget(widget.copy(page = to.page, x = to.x, y = to.y))
    }

    fun removeHomeWidget(id: Int) {
        prefs.removeHomeWidget(id)
        WidgetHostManager.get(context).deleteId(id)
        _ui.update { it.copy(resizingWidget = null) }
        compactPages()
    }

    /**
     * Taille par défaut d'un widget en cases de la grille : celle qu'il déclare (Android 12+),
     * sinon déduite de sa taille minimale et de la taille approximative d'une case.
     */
    fun defaultWidgetSpan(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val s = settings.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
            return info.targetCellWidth.coerceIn(1, s.homeColumns) to info.targetCellHeight.coerceIn(1, s.homeRows)
        }
        val metrics = context.resources.displayMetrics
        val cellW = (metrics.widthPixels / metrics.density - 24f) / s.homeColumns
        val cellH = (metrics.heightPixels / metrics.density * 0.7f) / s.homeRows
        val spanX = ceil((info.minWidth / metrics.density) / cellW).toInt()
        val spanY = ceil((info.minHeight / metrics.density) / cellH).toInt()
        return spanX.coerceIn(1, s.homeColumns) to spanY.coerceIn(1, s.homeRows)
    }

    /**
     * L'ancienne page des widgets n'existe plus : ses widgets passent sur la grille de
     * l'accueil (même identifiant, rien à reconfigurer), sur une nouvelle page s'il le faut.
     */
    private fun migrateWidgetsPage() {
        val slots = prefs.widgets.value
        if (slots.isEmpty()) return
        val host = WidgetHostManager.get(context)
        for (slot in slots) {
            if (prefs.homeWidgets.value.none { it.id == slot.id }) {
                val (spanX, spanY) = host.info(slot.id)?.let(::defaultWidgetSpan) ?: (2 to 2)
                addHomeWidget(slot.id, spanX, spanY)
            }
            prefs.removeWidget(slot.id)
        }
    }

    /**
     * Libère les widgets alloués par Nimbus mais affichés nulle part (ajout interrompu,
     * ancienne page des widgets…). À n'appeler que sans ajout de widget en cours.
     */
    fun deleteOrphanWidgets() {
        val host = WidgetHostManager.get(context)
        val used = prefs.homeWidgets.value.map { it.id }.toSet()
        val allocated = try {
            host.host.appWidgetIds
        } catch (e: Exception) {
            return
        }
        allocated.filter { it !in used }.forEach(host::deleteId)
    }

    /** Nom du widget [id] (celui de son fournisseur), pour la liste des réglages. */
    fun widgetLabel(id: Int): String {
        val info = WidgetHostManager.get(context).info(id) ?: return "Widget indisponible"
        return try {
            info.loadLabel(context.packageManager)
        } catch (e: Exception) {
            null
        } ?: info.provider.packageName
    }

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
            filesPermission = FilesSearch.hasPermission(context),
            dictationService = DictationService.isEnabled,
            hasPin = prefs.hasPin(),
            lockScreenAllowed = SystemActions.xiaomiLockScreenAllowed(context),
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

    override fun onCleared() {
        wallpaperManager.removeOnColorsChangedListener(colorsListener)
        repository.close()
    }
}
