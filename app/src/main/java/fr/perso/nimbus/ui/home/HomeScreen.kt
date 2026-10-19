package fr.perso.nimbus.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.HomeMenu
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.UiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val PAGE_HOME = 0
const val PAGE_WIDGETS = 1

/**
 * Accueil sur deux pages : page principale (horloge, recherche, favoris)
 * et page de widgets. [active] : rien n'est affiché par-dessus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    ui: UiState,
    settings: LauncherSettings,
    apps: AppsState,
    active: Boolean,
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.pageRequest) {
        if (pagerState.currentPage != ui.pageRequest.page) pagerState.animateScrollToPage(ui.pageRequest.page)
    }
    // En quittant la page widgets, on sort du mode édition.
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != PAGE_WIDGETS && ui.editingWidgets) vm.setEditingWidgets(false)
    }

    BackHandler(enabled = active && (ui.editingWidgets || pagerState.currentPage != PAGE_HOME)) {
        if (ui.editingWidgets) vm.setEditingWidgets(false)
        else scope.launch { pagerState.animateScrollToPage(PAGE_HOME) }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                PAGE_HOME -> HomePage(
                    vm = vm,
                    settings = settings,
                    favorites = apps.favorites,
                    onLongPress = { vm.openHomeMenu(PAGE_HOME, it) },
                )
                else -> WidgetsPage(
                    vm = vm,
                    editing = ui.editingWidgets,
                    onLongPress = { vm.openHomeMenu(PAGE_WIDGETS, it) },
                )
            }
        }

        PageIndicator(
            count = 2,
            current = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
        )

        ui.homeMenu?.let { HomeMenuPopup(menu = it, vm = vm) }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        repeat(count) { i ->
            val selected = i == current
            Box(
                Modifier
                    .size(if (selected) 8.dp else 6.dp)
                    .background(Color.White.copy(alpha = if (selected) 0.95f else 0.5f), CircleShape),
            )
        }
    }
}

/** Menu de l'appui long, affiché à l'endroit du doigt. */
@Composable
private fun HomeMenuPopup(menu: HomeMenu, vm: LauncherViewModel) {
    val context = LocalContext.current
    Box(
        Modifier
            .offset { IntOffset(menu.position.x.roundToInt(), menu.position.y.roundToInt()) }
            .size(1.dp),
    ) {
        DropdownMenu(expanded = true, onDismissRequest = vm::closeHomeMenu) {
            MenuItem("Ajouter un widget", Icons.Default.Add) {
                vm.showPage(PAGE_WIDGETS)
                vm.openWidgetPicker()
            }
            if (menu.page == PAGE_WIDGETS) {
                MenuItem("Modifier les widgets", Icons.Default.Edit) { vm.setEditingWidgets(true) }
            }
            MenuItem("Fond d'écran", Icons.Default.Star) {
                vm.closeHomeMenu()
                SystemActions.openWallpaperPicker(context)
            }
            MenuItem("Toutes les apps", Icons.AutoMirrored.Filled.List) { vm.openDrawer() }
            MenuItem("Paramètres Nimbus", Icons.Default.Settings) { vm.openSettings() }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}
