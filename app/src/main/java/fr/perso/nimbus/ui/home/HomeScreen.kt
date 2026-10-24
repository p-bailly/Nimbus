package fr.perso.nimbus.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.R
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.HomeMenu
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.UiState
import fr.perso.nimbus.ui.components.AppPickerDialog
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.paper.PaperHome
import fr.perso.nimbus.ui.settings.SettingsColors
import fr.perso.nimbus.ui.settings.SettingsEntry
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.NimbusBottomSheet
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperRule
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.inkClickable

/**
 * Accueil : pages d'icônes et de widgets, horloge, dock, recherche, et le menu de l'appui long.
 * [active] : rien n'est affiché par-dessus.
 */
@Composable
fun HomeScreen(
    vm: LauncherViewModel,
    ui: UiState,
    settings: LauncherSettings,
    apps: AppsState,
    active: Boolean,
) {
    BackHandler(enabled = active && ui.resizingWidget != null) { vm.setResizingWidget(null) }

    Box(Modifier.fillMaxSize()) {
        if (settings.homeStyle.paper) {
            PaperHome(vm = vm, settings = settings, onLongPress = vm::openHomeMenu)
        } else {
            HomePage(
                vm = vm,
                settings = settings,
                apps = apps,
                resizingWidgetId = ui.resizingWidget,
                pageRequest = ui.pageRequest,
                onLongPress = vm::openHomeMenu,
            )
        }
        ui.homeMenu?.let { HomeMenuSheet(menu = it, vm = vm, apps = apps, settings = settings) }
    }
}

/**
 * Menu de l'appui long sur le fond, en panneau du bas (comme Smart Launcher) : raccourcis
 * en tuiles, accès directs aux réglages de la page, puis « Voir tous les paramètres ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMenuSheet(menu: HomeMenu, vm: LauncherViewModel, apps: AppsState, settings: LauncherSettings) {
    val context = LocalContext.current
    var pickingIcon by remember { mutableStateOf(false) }
    var pickingStyle by remember { mutableStateOf(false) }
    val paper = settings.homeStyle.paper
    val voronoiSites by vm.voronoiSites.collectAsStateWithLifecycle()
    val ensemblesSites by vm.ensemblesSites.collectAsStateWithLifecycle()

    NimbusBottomSheet(
        onDismissRequest = vm::closeHomeMenu,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        if (LocalPaperUi.current) {
            // Mêmes raccourcis que les tuiles, dans le même ordre.
            val shortcuts = buildList<Pair<String, () -> Unit>> {
                add("Style de l'accueil" to { pickingStyle = true })
                if (settings.homeStyle == HomeStyle.VORONOI && voronoiSites.isNotEmpty()) {
                    add(
                        "Sites d'origine" to {
                            vm.closeHomeMenu()
                            vm.setVoronoiSites(emptyMap())
                        },
                    )
                }
                if (settings.homeStyle == HomeStyle.ENSEMBLES && ensemblesSites.isNotEmpty()) {
                    add(
                        "Places d'origine" to {
                            vm.closeHomeMenu()
                            vm.setEnsemblesSites(emptyMap())
                        },
                    )
                }
                if (!paper) {
                    add("Ajouter une icône" to { pickingIcon = true })
                    add("Ajouter un widget" to { vm.openWidgetPicker() })
                }
                add(
                    "Fond d'écran" to {
                        vm.closeHomeMenu()
                        SystemActions.openWallpaperPicker(context)
                    },
                )
                add("Toutes les apps" to { vm.openDrawer() })
                if (!paper && menu.page != apps.homePage && menu.page < apps.pageCount) {
                    add(
                        "Page principale" to {
                            vm.closeHomeMenu()
                            vm.setMainHomePage(menu.page)
                        },
                    )
                }
            }
            PaperHomeMenu(
                shortcuts = shortcuts,
                onAppearance = { vm.openSettings(SettingsSection.APPEARANCE) },
                onHomeSettings = { vm.openSettings(SettingsSection.HOME) },
                onGestures = { vm.openSettings(SettingsSection.GESTURES) },
                onAllSettings = { vm.openSettings() },
            )
            return@NimbusBottomSheet
        }
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                "Page d'accueil",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                MenuTile(R.drawable.ms_style, "Style de l'accueil") { pickingStyle = true }
                if (settings.homeStyle == HomeStyle.VORONOI && voronoiSites.isNotEmpty()) {
                    MenuTile(R.drawable.ms_history, "Sites d'origine") {
                        vm.closeHomeMenu()
                        vm.setVoronoiSites(emptyMap())
                    }
                }
                if (settings.homeStyle == HomeStyle.ENSEMBLES && ensemblesSites.isNotEmpty()) {
                    MenuTile(R.drawable.ms_history, "Places d'origine") {
                        vm.closeHomeMenu()
                        vm.setEnsemblesSites(emptyMap())
                    }
                }
                if (!paper) {
                    MenuTile(R.drawable.ms_add_box, "Ajouter une icône") { pickingIcon = true }
                    MenuTile(R.drawable.ms_dashboard_customize, "Ajouter un widget") { vm.openWidgetPicker() }
                }
                MenuTile(R.drawable.ms_wallpaper, "Fond d'écran") {
                    vm.closeHomeMenu()
                    SystemActions.openWallpaperPicker(context)
                }
                MenuTile(R.drawable.ms_apps, "Toutes les apps") { vm.openDrawer() }
                if (!paper && menu.page != apps.homePage && menu.page < apps.pageCount) {
                    MenuTile(R.drawable.ms_home, "Page principale") {
                        vm.closeHomeMenu()
                        vm.setMainHomePage(menu.page)
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            SettingsEntry(R.drawable.ms_palette, SettingsColors.Orange, "Apparence globale") {
                vm.openSettings(SettingsSection.APPEARANCE)
            }
            SettingsEntry(R.drawable.ms_home, SettingsColors.Blue, "Page d'accueil", chevron = true) {
                vm.openSettings(SettingsSection.HOME)
            }
            SettingsEntry(R.drawable.ms_swipe, SettingsColors.Red, "Gestes et raccourcis") {
                vm.openSettings(SettingsSection.GESTURES)
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = { vm.openSettings() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Icon(painterResource(R.drawable.ms_settings), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Voir tous les paramètres", fontSize = 16.sp)
                }
            }
        }
    }

    if (pickingStyle) {
        ChoiceDialog(
            title = "Style de l'accueil",
            options = HomeStyle.entries,
            selected = settings.homeStyle,
            label = { it.label },
            onSelect = { style ->
                pickingStyle = false
                vm.closeHomeMenu()
                vm.setHomeStyle(style)
            },
            onDismiss = { pickingStyle = false },
            footer = "Les styles « papier » placent 28 applis (27 pour Sierpiński) sur une page d'article ; appui long sur une appli pour en changer. " +
                "Voronoï : appui long puis glisser pour déplacer une appli dans le carré.",
        )
    }

    if (pickingIcon) {
        AppPickerDialog(
            title = "Ajouter une icône",
            apps = apps.visible.filter { it.key !in apps.homeKeys },
            onPick = { app ->
                pickingIcon = false
                vm.closeHomeMenu()
                app?.let(vm::addToHome)
            },
            onDismiss = { pickingIcon = false },
        )
    }
}

/**
 * Menu de l'accueil sous le thème papier : titre en gras, raccourcis en tableau à filets fins
 * (trois colonnes), réglages de la page, puis « voir tous les paramètres » en lien.
 */
@Composable
internal fun PaperHomeMenu(
    shortcuts: List<Pair<String, () -> Unit>>,
    onAppearance: () -> Unit,
    onHomeSettings: () -> Unit,
    onGestures: () -> Unit,
    onAllSettings: () -> Unit,
) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(
            "Page d'accueil",
            style = PaperStyle.serif(24f, 30f, bold = true),
            modifier = Modifier.padding(horizontal = PaperMargin),
        )
        Column(
            Modifier
                .padding(horizontal = PaperMargin)
                .padding(top = 16.dp, bottom = 10.dp),
        ) {
            PaperRule(thickness = 0.8.dp)
            for ((r, row) in shortcuts.chunked(3).withIndex()) {
                if (r > 0) PaperRule()
                Row(Modifier.height(IntrinsicSize.Min)) {
                    for (i in 0 until 3) {
                        if (i > 0) {
                            Box(
                                Modifier
                                    .width(0.6.dp)
                                    .fillMaxHeight()
                                    .background(PaperColors.Rule),
                            )
                        }
                        val item = row.getOrNull(i)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 60.dp)
                                .then(if (item != null) Modifier.inkClickable(onClick = item.second) else Modifier)
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                        ) {
                            if (item != null) {
                                Text(
                                    item.first,
                                    style = PaperStyle.serif(14.5f, 18f).copy(textAlign = TextAlign.Center),
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
            PaperRule(thickness = 0.8.dp)
        }
        SettingsEntry(R.drawable.ms_palette, SettingsColors.Orange, "Apparence globale", onClick = onAppearance)
        SettingsEntry(R.drawable.ms_home, SettingsColors.Blue, "Page d'accueil", chevron = true, onClick = onHomeSettings)
        SettingsEntry(R.drawable.ms_swipe, SettingsColors.Red, "Gestes et raccourcis", onClick = onGestures)
        PaperTextButton(
            "Voir tous les paramètres ›",
            onClick = onAllSettings,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp),
        )
    }
}

/** Tuile du menu de l'accueil : icône et libellé sur deux lignes. */
@Composable
private fun MenuTile(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}
