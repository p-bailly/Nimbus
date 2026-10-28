package fr.perso.nimbus.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.R
import fr.perso.nimbus.data.PaperFamily
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.components.AppGlyphDisc
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperTextButton

/**
 * Accueil : le style en vignettes avec l'aperçu en haut de page. Seuls les réglages qui servent au style
 * choisi sont montrés : les applis de la page pour un accueil papier, la grille, l'affichage et les widgets
 * pour la grille d'icônes.
 */
@Composable
internal fun SettingsCtx.HomePage() {
    val paperHome = settings.homeStyle.paper
    val previewIcons = remember(apps.favorites, apps.visible) { (apps.favorites + apps.visible).distinctBy { it.key }.take(8).map { it.icon } }
    Page(SettingsSection.HOME) {
        pinnedPreview {
            if (paperHome) HomeStyleFigure(settings.homeStyle)
            else HomeGridPreview(settings.homeColumns, settings.homeRows, settings.dockColumns, previewIcons)
        }

        settingsSection("Style")
        setting(SettingId.HOME_STYLE) {
            androidx.compose.foundation.layout.Box(Modifier.settingFocus(SettingId.HOME_STYLE)) {
                HomeStylePicker(settings.homeStyle) { style -> vm.setHomeStyle(style) }
            }
        }
        item(key = "style:note") {
            SettingsNote("Les styles « papier » placent 29 applis sur une page d'article ; appui long sur une appli pour en changer.")
        }

        if (paperHome) {
            settingsSection("Applis")
            setting(SettingId.PAPER_COLORS) {
                SettingsEntry(
                    R.drawable.ms_palette, SettingsColors.Orange, SettingId.PAPER_COLORS.title,
                    if (settings.paperAppColors) "Chaque appli à sa couleur, comme dans le sommaire"
                    else "Couleurs des familles (Échanges, Écran, Trajets…)",
                    checked = settings.paperAppColors,
                    id = SettingId.PAPER_COLORS,
                ) { update { it.copy(paperAppColors = !it.paperAppColors) } }
            }
            setting(SettingId.PAPER_APPS) {
                SettingsEntry(
                    R.drawable.ms_apps, SettingsColors.Purple, SettingId.PAPER_APPS.title,
                    "${PaperSlot.entries.size} places · l'appli de chacune",
                    chevron = true,
                    id = SettingId.PAPER_APPS,
                ) { open(SettingsSection.HOME_APPS) }
            }
        } else {
            settingsSection("Grille d'icônes")
            setting(SettingId.GRID_COLUMNS) {
                SettingsStepper(R.drawable.ms_view_column, SettingsColors.Blue, SettingId.GRID_COLUMNS.title, settings.homeColumns, 3..8, SettingId.GRID_COLUMNS) { n ->
                    update { it.copy(homeColumns = n) }
                }
            }
            setting(SettingId.GRID_ROWS) {
                SettingsStepper(R.drawable.ms_grid_view, SettingsColors.Cyan, SettingId.GRID_ROWS.title, settings.homeRows, 3..14, SettingId.GRID_ROWS) { n ->
                    update { it.copy(homeRows = n) }
                }
            }
            setting(SettingId.DOCK_COLUMNS) {
                SettingsStepper(R.drawable.ms_dock_to_bottom, SettingsColors.Teal, SettingId.DOCK_COLUMNS.title, settings.dockColumns, 3..7, SettingId.DOCK_COLUMNS) { n ->
                    update { it.copy(dockColumns = n) }
                }
            }
            setting(SettingId.GRID_LABELS) {
                SettingsEntry(R.drawable.ms_title, SettingsColors.Orange, SettingId.GRID_LABELS.title, checked = settings.homeShowLabels, id = SettingId.GRID_LABELS) {
                    update { it.copy(homeShowLabels = !it.homeShowLabels) }
                }
            }

            settingsSection("Affichage")
            setting(SettingId.SHOW_CLOCK) {
                SettingsEntry(R.drawable.ms_schedule, SettingsColors.Orange, SettingId.SHOW_CLOCK.title, checked = settings.showClock, id = SettingId.SHOW_CLOCK) {
                    update { it.copy(showClock = !it.showClock) }
                }
            }
            setting(SettingId.SHOW_BATTERY) {
                SettingsEntry(R.drawable.ms_battery_full, SettingsColors.Green, SettingId.SHOW_BATTERY.title, checked = settings.showBattery, id = SettingId.SHOW_BATTERY) {
                    update { it.copy(showBattery = !it.showBattery) }
                }
            }
            setting(SettingId.SHOW_SEARCH_BAR) {
                SettingsEntry(
                    R.drawable.ms_search, SettingsColors.Blue, SettingId.SHOW_SEARCH_BAR.title, "En bas de l'accueil",
                    checked = settings.showSearchBar, id = SettingId.SHOW_SEARCH_BAR,
                ) { update { it.copy(showSearchBar = !it.showSearchBar) } }
            }

            settingsSection("Widgets")
            if (apps.homeWidgets.isEmpty()) {
                item(key = "widgets:none") { SettingsNote("Aucun widget. Appui long sur le fond de l'accueil › Ajouter un widget.") }
            }
            items(apps.homeWidgets, key = { "widget:${it.id}" }) { widget ->
                val label = remember(widget.id) { vm.widgetLabel(widget.id) }
                SettingsRow(
                    R.drawable.ms_dashboard_customize, SettingsColors.Purple, label,
                    "Page ${widget.page + 1} · ${widget.spanX} × ${widget.spanY} cases", true, null, null,
                ) {
                    if (LocalPaperUi.current) {
                        PaperTextButton("Supprimer", onClick = { vm.removeHomeWidget(widget.id) }, color = PaperColors.Error)
                    } else {
                        TextButton(onClick = { vm.removeHomeWidget(widget.id) }) {
                            Text("Supprimer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        settingsSection("Barre de navigation")
        setting(SettingId.HIDE_NAV) {
            SettingsEntry(
                R.drawable.ms_swipe_up, SettingsColors.Teal, SettingId.HIDE_NAV.title,
                "Sur l'accueil : de la couleur du fond, ils restent à leur place et utilisables",
                checked = settings.hideNavigationOnHome, id = SettingId.HIDE_NAV,
            ) { update { it.copy(hideNavigationOnHome = !it.hideNavigationOnHome) } }
        }
    }
}

/** Couleur d'une famille de places, pour les pastilles. */
private fun PaperFamily.color(): Color = Color(argb)

/**
 * Applis de la page : les 29 places d'un accueil papier, repliées par famille (la première est dépliée).
 * Toucher une place ouvre le choix de son appli.
 */
@Composable
internal fun SettingsCtx.HomeAppsPage() {
    var open by remember { mutableStateOf(setOf(PaperFamily.ECHANGES)) }
    Page(SettingsSection.HOME_APPS) {
        item(key = "apps:note") {
            SettingsNote(
                "${PaperSlot.entries.size} places : 6 échanges, 6 écran, 3 trajets, 9 outils, 5 jeux. " +
                    "Une place reçoit l'appli choisie, sinon la première appli installée qui lui correspond.",
            )
        }
        for (family in PaperFamily.entries) {
            val slots = PaperSlot.of(family)
            val expanded = family in open
            collapsibleSection(family.label, "${slots.size}", expanded, family.color()) {
                open = if (expanded) open - family else open + family
            }
            if (expanded) {
                items(slots, key = { "slot:${it.name}" }) { slot ->
                    val app = paperApps[slot]
                    SettingsEntry(
                        R.drawable.ms_apps,
                        family.color(),
                        slot.title,
                        app?.label ?: "Aucune appli : toucher pour choisir",
                        chevron = true,
                        // Papier : la pastille de la place, cerclée de la couleur de sa famille.
                        paperLeading = {
                            AppGlyphDisc(app, 30.dp, color = family.color(), ring = 1.1.dp, placeholder = slot.title.take(1))
                        },
                    ) { vm.showPaperSlotPicker(slot) }
                }
            }
        }
        item(key = "apps:end") { Spacer(Modifier.height(8.dp)) }
    }
}
