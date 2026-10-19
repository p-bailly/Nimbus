package fr.perso.nimbus.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.ContactResult
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.SearchResults
import fr.perso.nimbus.ui.UiState
import fr.perso.nimbus.ui.components.AppIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/** Distance de glissé vers le bas au-delà de laquelle le tiroir se ferme. */
private val CLOSE_DISTANCE = 120.dp

/** Vitesse de lancer vers le bas (px/s) qui ferme le tiroir même sur une courte distance. */
private const val CLOSE_VELOCITY = 2500f

/** Tiroir d'apps : recherche, onglets de catégories et grille. Se ferme par glissé vers le bas. */
@Composable
fun AppDrawer(
    vm: LauncherViewModel,
    ui: UiState,
    settings: LauncherSettings,
    apps: AppsState,
    host: LauncherHost,
) {
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val closeDistance = with(LocalDensity.current) { CLOSE_DISTANCE.toPx() }

    // Décalage vertical du tiroir pendant le glissé vers le bas.
    var offsetY by remember { mutableFloatStateOf(0f) }

    suspend fun settle(velocity: Float) {
        if (offsetY > closeDistance || (offsetY > 0f && velocity > CLOSE_VELOCITY)) {
            vm.closeDrawer()
        } else if (offsetY > 0f) {
            animate(offsetY, 0f) { value, _ -> offsetY = value }
        }
    }

    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) keyboard?.hide()
                // Le tiroir est déjà descendu : on le remonte avant de faire défiler la grille.
                if (offsetY > 0f && available.y < 0f) {
                    val consumed = max(available.y, -offsetY)
                    offsetY += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // La grille est en haut et le doigt continue de descendre : le tiroir suit.
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    offsetY += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY > 0f) {
                    settle(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler { vm.closeDrawer() }

    LaunchedEffect(ui.drawerOpen) {
        if (!ui.drawerOpen) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    val headerDrag = rememberDraggableState { delta -> offsetY = (offsetY + delta).coerceAtLeast(0f) }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = offsetY
                alpha = 1f - (offsetY / (closeDistance * 4f)).coerceIn(0f, 0.5f)
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScroll)
                .statusBarsPadding()
                .imePadding(),
        ) {
            Column(
                Modifier.draggable(
                    state = headerDrag,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> settle(velocity) },
                ),
            ) {
                DragHandle()
                SearchField(
                    query = ui.query,
                    focusRequested = ui.focusSearch,
                    onFocusConsumed = vm::consumeSearchFocus,
                    onQueryChange = vm::setQuery,
                    onSearch = vm::submitSearch,
                    onOpenSettings = vm::openSettings,
                )
            }

            if (!apps.loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (ui.query.isBlank()) {
                CategoryPager(apps = apps, settings = settings, badges = badges, vm = vm)
            } else {
                SearchResultsGrid(
                    query = ui.query,
                    results = results,
                    settings = settings,
                    badges = badges,
                    contactsBlocked = settings.searchContacts && !status.contactsPermission,
                    vm = vm,
                    onRequestContacts = host::requestContactsPermission,
                )
            }
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    focusRequested: Boolean,
    onFocusConsumed: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusRequested) {
        if (focusRequested) {
            // Laisse l'animation d'ouverture démarrer avant de demander le clavier.
            delay(80)
            focusRequester.requestFocus()
            keyboard?.show()
            onFocusConsumed()
        }
    }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Rechercher des apps, contacts, sur le web") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                }
            } else {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Paramètres Nimbus")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .focusRequester(focusRequester),
    )
}

/** Onglets « Toutes » + catégories, avec balayage horizontal entre les onglets. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryPager(
    apps: AppsState,
    settings: LauncherSettings,
    badges: Map<String, Int>,
    vm: LauncherViewModel,
) {
    // null = « Toutes ». Un seul onglet de catégorie n'apporterait rien : on les masque alors.
    val tabs: List<AppCategory?> = remember(apps.categories) {
        if (apps.categories.size > 1) listOf<AppCategory?>(null) + apps.categories else listOf(null)
    }
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column {
        if (tabs.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage.coerceIn(0, tabs.lastIndex),
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                tabs.forEachIndexed { index, category ->
                    Tab(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(category?.label ?: "Toutes", maxLines = 1) },
                    )
                }
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val category = tabs.getOrNull(page)
            val list = remember(apps.visible, category) {
                if (category == null) apps.visible else apps.visible.filter { it.category == category }
            }
            AppGrid(columns = settings.drawerColumns) {
                appItems(list, settings, badges, vm)
            }
        }
    }
}

@Composable
private fun AppGrid(columns: Int, content: LazyGridScope.() -> Unit) {
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.coerceIn(3, 7)),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = navBar + 16.dp),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

private fun LazyGridScope.appItems(
    list: List<AppEntry>,
    settings: LauncherSettings,
    badges: Map<String, Int>,
    vm: LauncherViewModel,
) {
    items(list, key = { it.key }) { app ->
        AppIcon(
            app = app,
            iconSizeDp = settings.iconSize.dp,
            showLabel = settings.showLabels,
            badgeCount = badges[app.packageName] ?: 0,
            onClick = { vm.launch(app) },
            onLongClick = { vm.showAppSheet(app) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Résultats : apps (insensible aux accents), contacts, puis recherche web. */
@Composable
private fun SearchResultsGrid(
    query: String,
    results: SearchResults,
    settings: LauncherSettings,
    badges: Map<String, Int>,
    contactsBlocked: Boolean,
    vm: LauncherViewModel,
    onRequestContacts: () -> Unit,
) {
    val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    AppGrid(columns = settings.drawerColumns) {
        if (results.apps.isEmpty()) {
            item(span = fullSpan) {
                Text(
                    "Aucune app ne correspond",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            appItems(results.apps, settings, badges, vm)
        }

        if (results.contacts.isNotEmpty()) {
            item(span = fullSpan) { SectionHeader("Contacts") }
            items(results.contacts, key = { "contact:" + it.id }, span = { fullSpan() }) { contact ->
                ContactRow(contact, onClick = { vm.openContact(contact) })
            }
        } else if (contactsBlocked) {
            item(span = fullSpan) {
                ResultRow(
                    text = "Autoriser l'accès aux contacts pour les rechercher ici",
                    onClick = onRequestContacts,
                    leading = { Icon(Icons.Default.Person, contentDescription = null) },
                )
            }
        }

        item(span = fullSpan) {
            Column {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                ResultRow(
                    text = "Rechercher « ${query.trim()} » sur le web",
                    onClick = { vm.webSearch(query) },
                    leading = { Icon(Icons.Default.Search, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ContactRow(contact: ContactResult, onClick: () -> Unit) {
    ResultRow(
        text = contact.name,
        onClick = onClick,
        leading = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            ) {
                Text(
                    contact.name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
    )
}

@Composable
private fun ResultRow(text: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { leading() }
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
