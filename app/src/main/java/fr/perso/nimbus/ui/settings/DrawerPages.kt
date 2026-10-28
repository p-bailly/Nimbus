package fr.perso.nimbus.ui.settings

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.perso.nimbus.R
import fr.perso.nimbus.data.DrawerTheme
import fr.perso.nimbus.data.SortMode
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.theme.LocalPaperUi

/** Libellé court d'un mode nuit du tiroir, pour tenir sur trois boutons. */
private fun DrawerTheme.short(): String = when (this) {
    DrawerTheme.FOLLOW -> "Comme le lanceur"
    DrawerTheme.NIGHT -> "Nuit"
    DrawerTheme.BLACK -> "Noir pur (OLED)"
}

/**
 * Tiroir et recherche : l'affichage des apps (mode nuit, colonnes, tri, noms), les catégories, puis la
 * recherche du tiroir (moteur web, contacts, fichiers, Play Store, réglages).
 */
@Composable
internal fun SettingsCtx.DrawerPage() {
    val paper = LocalPaperUi.current
    Page(SettingsSection.DRAWER) {
        settingsSection("Tiroir")
        setting(SettingId.DRAWER_NIGHT) {
            SettingsControl(SettingId.DRAWER_NIGHT.title, id = SettingId.DRAWER_NIGHT) {
                SettingsSegmented(DrawerTheme.entries, settings.drawerTheme, { it.short() }, { theme -> update { it.copy(drawerTheme = theme) } })
            }
        }
        setting(SettingId.DRAWER_COLUMNS) {
            SettingsStepper(R.drawable.ms_view_column, SettingsColors.Teal, SettingId.DRAWER_COLUMNS.title, settings.drawerColumns, 3..7, SettingId.DRAWER_COLUMNS) { n ->
                update { it.copy(drawerColumns = n) }
            }
        }
        setting(SettingId.DRAWER_SORT) {
            SettingsControl(SettingId.DRAWER_SORT.title, id = SettingId.DRAWER_SORT) {
                SettingsSegmented(SortMode.entries, settings.sortMode, { it.label }, { mode -> update { it.copy(sortMode = mode) } })
            }
        }
        setting(SettingId.DRAWER_LABELS) {
            SettingsEntry(R.drawable.ms_title, SettingsColors.Blue, SettingId.DRAWER_LABELS.title, checked = settings.showLabels, id = SettingId.DRAWER_LABELS) {
                update { it.copy(showLabels = !it.showLabels) }
            }
        }
        setting(SettingId.DRAWER_FREQUENT) {
            SettingsEntry(
                R.drawable.ms_history, SettingsColors.Orange, SettingId.DRAWER_FREQUENT.title,
                if (paper && settings.drawerToc) "Sommaire : une figure des plus utilisées, hors de l'accueil" else "Une ligne des plus utilisées, en tri alphabétique",
                checked = settings.showFrequentApps,
                id = SettingId.DRAWER_FREQUENT,
            ) { update { it.copy(showFrequentApps = !it.showFrequentApps) } }
        }
        if (paper) {
            setting(SettingId.DRAWER_TOC) {
                SettingsEntry(
                    R.drawable.ms_sort, SettingsColors.Purple, SettingId.DRAWER_TOC.title,
                    "Les apps en sommaire, rangées par catégorie",
                    checked = settings.drawerToc,
                    id = SettingId.DRAWER_TOC,
                ) { update { it.copy(drawerToc = !it.drawerToc) } }
            }
        }

        settingsSection("Catégories", SettingsColors.Orange)
        setting(SettingId.AUTO_CATEGORIES) {
            SettingsEntry(
                R.drawable.ms_category, SettingsColors.Orange, SettingId.AUTO_CATEGORIES.title,
                "Onglets ; appui long sur une app › Plus pour la déplacer",
                checked = settings.autoCategories,
                id = SettingId.AUTO_CATEGORIES,
            ) { update { it.copy(autoCategories = !it.autoCategories) } }
        }
        setting(SettingId.MANAGE_CATEGORIES) {
            SettingsEntry(
                R.drawable.ms_interests, SettingsColors.Orange, SettingId.MANAGE_CATEGORIES.title,
                "${apps.allCategories.size} catégories · renommer, ordonner, ajouter",
                chevron = true,
                id = SettingId.MANAGE_CATEGORIES,
            ) { open(SettingsSection.CATEGORIES) }
        }

        settingsSection("Recherche", SettingsColors.Cyan)
        setting(SettingId.SEARCH_ENGINE) {
            SettingsEntry(
                R.drawable.ms_public, SettingsColors.Cyan, SettingId.SEARCH_ENGINE.title, settings.webSearchEngine.label,
                chevron = true, id = SettingId.SEARCH_ENGINE,
            ) { showDialog(SettingsDialog.Engine) }
        }
        setting(SettingId.SEARCH_CONTACTS) {
            SettingsEntry(
                R.drawable.ms_contacts, SettingsColors.Blue, SettingId.SEARCH_CONTACTS.title,
                if (settings.searchContacts && !status.contactsPermission) "Autorisation requise" else "Contacts dans les résultats",
                checked = settings.searchContacts,
                id = SettingId.SEARCH_CONTACTS,
            ) {
                val enable = !settings.searchContacts
                update { it.copy(searchContacts = enable) }
                if (enable && !status.contactsPermission) host.requestContactsPermission()
            }
        }
        if (settings.searchContacts && !status.contactsPermission) {
            item(key = "banner:contacts") { SettingsBanner(R.drawable.ms_contacts, "Autoriser l'accès aux contacts", onClick = host::requestContactsPermission) }
        }
        setting(SettingId.SEARCH_FILES) {
            SettingsEntry(
                R.drawable.ms_file_open, SettingsColors.Orange, SettingId.SEARCH_FILES.title,
                if (settings.searchFiles && !status.filesPermission) "Autorisation requise" else "Fichiers du téléphone, par nom",
                checked = settings.searchFiles,
                id = SettingId.SEARCH_FILES,
            ) {
                val enable = !settings.searchFiles
                update { it.copy(searchFiles = enable) }
                if (enable && !status.filesPermission) host.requestFilesPermission()
            }
        }
        if (settings.searchFiles && !status.filesPermission) {
            val mark = if (settings.searchContacts && !status.contactsPermission) 2 else 1
            item(key = "banner:files") { SettingsBanner(R.drawable.ms_file_open, "Autoriser l'accès aux fichiers", mark = mark, onClick = host::requestFilesPermission) }
        }
        setting(SettingId.SEARCH_PLAY) {
            SettingsEntry(
                R.drawable.ms_shop, SettingsColors.Green, SettingId.SEARCH_PLAY.title, "Lien vers le Play Store dans les résultats",
                checked = settings.searchPlayStore, id = SettingId.SEARCH_PLAY,
            ) { update { it.copy(searchPlayStore = !it.searchPlayStore) } }
        }
        setting(SettingId.SEARCH_SETTINGS) {
            SettingsEntry(
                R.drawable.ms_settings, SettingsColors.Gray, SettingId.SEARCH_SETTINGS.title, "Les réglages de Nimbus, dans les résultats",
                checked = settings.searchSettings, id = SettingId.SEARCH_SETTINGS,
            ) { update { it.copy(searchSettings = !it.searchSettings) } }
        }
        setting(SettingId.SEARCH_ENTER) {
            SettingsEntry(
                R.drawable.ms_apps, SettingsColors.Purple, SettingId.SEARCH_ENTER.title,
                if (settings.enterOpensFirstApp) "La touche Entrée ouvre la première app trouvée" else "Sinon, la touche Entrée lance la recherche web",
                checked = settings.enterOpensFirstApp, id = SettingId.SEARCH_ENTER,
            ) { update { it.copy(enterOpensFirstApp = !it.enterOpensFirstApp) } }
        }
    }
}

/** Une teinte par onglet (les catégories défilent sur ces couleurs). */
private val categoryTones = listOf(
    SettingsColors.Blue, SettingsColors.Green, SettingsColors.Orange, SettingsColors.Purple,
    SettingsColors.Teal, SettingsColors.Red, SettingsColors.Cyan,
)

/** Catégories : les onglets du tiroir (toucher pour renommer, déplacer ou supprimer), puis ajouter et rétablir. */
@Composable
internal fun SettingsCtx.CategoriesPage() {
    Page(SettingsSection.CATEGORIES) {
        item(key = "categories:note") {
            SettingsNote("Les onglets du tiroir, les sections du sommaire. Toucher une catégorie pour la renommer, la déplacer ou la supprimer.")
        }
        settingsSection("Onglets du tiroir")
        itemsIndexed(apps.allCategories, key = { _, category -> "category:${category.id}" }) { index, category ->
            val count = apps.visible.count { it.category == category.id }
            SettingsEntry(
                R.drawable.ms_category, categoryTones[index % categoryTones.size], category.label,
                if (count == 0) "Vide (onglet masqué)" else "$count app${if (count > 1) "s" else ""}",
                chevron = true,
            ) { showDialog(SettingsDialog.EditCategory(category.id)) }
        }
        settingsSection("Actions", SettingsColors.Green)
        setting(SettingId.ADD_CATEGORY) {
            SettingsEntry(R.drawable.ms_add_box, SettingsColors.Green, SettingId.ADD_CATEGORY.title, "Un onglet (une section du sommaire) de plus", id = SettingId.ADD_CATEGORY) {
                showDialog(SettingsDialog.EditCategory(null))
            }
        }
        setting(SettingId.RESET_CATEGORIES) {
            SettingsEntry(R.drawable.ms_autorenew, SettingsColors.Orange, SettingId.RESET_CATEGORIES.title, "Noms et ordre d'origine, catégories créées supprimées", id = SettingId.RESET_CATEGORIES) {
                showDialog(SettingsDialog.ResetCategories)
            }
        }
    }
}
