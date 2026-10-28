package fr.perso.nimbus.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.Category
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.IconPack
import fr.perso.nimbus.data.IconPackInfo
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.data.SearchEngine
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.SystemStatus
import fr.perso.nimbus.ui.components.AppPickerDialog
import fr.perso.nimbus.ui.components.CategoryDialog
import fr.perso.nimbus.ui.components.ChoiceDialog
import fr.perso.nimbus.ui.components.ClaudeSessionDialog
import fr.perso.nimbus.ui.components.PinDialog
import fr.perso.nimbus.ui.components.PinMode
import fr.perso.nimbus.data.claudeTargetName
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperDialog
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dialogues ouverts depuis les réglages (les choix de quelques options se font sur place, sans dialogue). */
internal sealed interface SettingsDialog {
    data object IconPackChoice : SettingsDialog
    data object Engine : SettingsDialog

    /** [index] null : nouvelle conversation. */
    data class EditClaudeSession(val index: Int?) : SettingsDialog

    /** [id] null : nouvelle catégorie. */
    data class EditCategory(val id: String?) : SettingsDialog
    data object ResetCategories : SettingsDialog
    data class GestureChoice(val gesture: Gesture) : SettingsDialog
    data class GestureApp(val gesture: Gesture) : SettingsDialog
    data object ConfirmImport : SettingsDialog
    data object PinUnlock : SettingsDialog
    data object PinCreate : SettingsDialog
    data object PinChangeOld : SettingsDialog
    data object PinChangeNew : SettingsDialog
}

/** Actions de geste qui passent par le service d'accessibilité. */
internal val SERVICE_ACTIONS = setOf(GestureAction.LOCK, GestureAction.RECENTS, GestureAction.SCREENSHOT, GestureAction.POWER_MENU)

/**
 * Ce que savent toutes les pages : le modèle de vue, l'activité hôte, les réglages et l'état du système, relus
 * à chaque changement. Une page est une extension de ce contexte qui remplit une [SettingsPage].
 */
internal class SettingsCtx(
    val vm: LauncherViewModel,
    val host: LauncherHost,
    val context: Context,
    val settings: LauncherSettings,
    val status: SystemStatus,
    val apps: AppsState,
    val paperApps: Map<PaperSlot, fr.perso.nimbus.data.AppEntry?>,
    val gestureApps: Map<Gesture, String>,
    val backupFolder: String?,
    val wallpaperColor: Int?,
    val iconPacks: List<IconPackInfo>?,
    /** Réglage à montrer (venu d'une recherche), s'il est sur la page affichée. */
    val focus: SettingId?,
    val showDialog: (SettingsDialog?) -> Unit,
) {
    fun update(transform: (LauncherSettings) -> LauncherSettings) = vm.updateSettings(transform)

    /** Ouvre une autre page des réglages. */
    fun open(section: SettingsSection, focus: SettingId? = null) = vm.openSettingsSection(section, focus)

    /** Une page de réglages : titre, folio, couleur et retour de [section], puis les lignes de [content]. */
    @Composable
    fun Page(section: SettingsSection, content: LazyListScope.() -> Unit) {
        SettingsPage(
            title = SettingsIndex.pageTitle(section),
            onBack = if (section == SettingsSection.MAIN) null else vm::closeScreen,
            folio = folio(section),
            accent = section.accent(),
            parentTitle = section.parent?.let { SettingsIndex.pageTitle(it) } ?: "Paramètres",
            focus = focus?.takeIf { it.section == section },
            onFocusShown = vm::consumeSettingsFocus,
            content = content,
        )
    }

}

/** Paramètres : page principale (recherche et pages) et ses pages. */
@Composable
fun SettingsScreen(vm: LauncherViewModel, host: LauncherHost, section: SettingsSection) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val paperApps by vm.paperApps.collectAsStateWithLifecycle()
    val gestureApps by vm.gestureApps.collectAsStateWithLifecycle()
    val backupFolder by vm.autoBackupFolder.collectAsStateWithLifecycle()
    val wallpaperColor by vm.wallpaperColor.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val iconPacks by produceState<List<IconPackInfo>?>(null) {
        value = withContext(Dispatchers.IO) { IconPack.installed(context) }
    }

    BackHandler { vm.closeScreen() }

    val c = SettingsCtx(
        vm, host, context, settings, status, apps, paperApps, gestureApps, backupFolder, wallpaperColor, iconPacks,
        ui.settingsFocus, { dialog = it },
    )

    AnimatedContent(
        targetState = section,
        transitionSpec = {
            if (depth(targetState) > depth(initialState)) {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 8 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 8 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
            }
        },
        label = "settings",
    ) { page ->
        when (page) {
            SettingsSection.MAIN -> c.HubPage()
            SettingsSection.APPEARANCE -> c.AppearancePage()
            SettingsSection.HOME -> c.HomePage()
            SettingsSection.HOME_APPS -> c.HomeAppsPage()
            SettingsSection.DRAWER -> c.DrawerPage()
            SettingsSection.CATEGORIES -> c.CategoriesPage()
            SettingsSection.GESTURES -> c.GesturesPage()
            SettingsSection.LOCKSCREEN -> c.LockscreenPage()
            SettingsSection.PRIVACY -> c.PrivacyPage()
            SettingsSection.DICTATION -> c.DictationPage()
            SettingsSection.NOTIFICATIONS -> c.NotificationsPage()
            SettingsSection.BACKUP -> c.BackupPage()
            SettingsSection.ABOUT -> c.AboutPage()
        }
    }

    SettingsDialogs(dialog, { dialog = it }, settings, status, apps, iconPacks.orEmpty(), vm, host, context)
}

/** Profondeur d'une page : 0 la principale, 1 une page, 2 une sous-page (sens du glissement). */
private fun depth(section: SettingsSection): Int = when {
    section == SettingsSection.MAIN -> 0
    section.parent == null -> 1
    else -> 2
}

/** Folio d'une page des réglages, en chiffres romains comme les pages liminaires d'un livre. */
internal fun folio(section: SettingsSection): String = roman(section.ordinal + 1)

/** Folio de la page des apps masquées, après celles des réglages. */
internal val HIDDEN_APPS_FOLIO: String = roman(SettingsSection.entries.size + 1)

private fun roman(n: Int): String {
    val units = listOf("", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix")
    val tens = listOf("", "x", "xx", "xxx")
    return tens[(n / 10).coerceAtMost(3)] + units[n % 10]
}

/** Tous les dialogues des réglages. */
@Composable
private fun SettingsDialogs(
    dialog: SettingsDialog?,
    setDialog: (SettingsDialog?) -> Unit,
    settings: LauncherSettings,
    status: SystemStatus,
    apps: AppsState,
    iconPacks: List<IconPackInfo>,
    vm: LauncherViewModel,
    host: LauncherHost,
    context: Context,
) {
    fun update(transform: (LauncherSettings) -> LauncherSettings) = vm.updateSettings(transform)
    val close = { setDialog(null) }
    when (dialog) {
        SettingsDialog.IconPackChoice -> ChoiceDialog(
            title = "Pack d'icônes",
            options = listOf<String?>(null) + iconPacks.map { it.packageName },
            selected = settings.iconPack,
            label = { pkg -> if (pkg == null) "Aucun (icônes d'origine)" else iconPacks.firstOrNull { it.packageName == pkg }?.label ?: pkg },
            onSelect = { pkg -> update { it.copy(iconPack = pkg) }; close() },
            onDismiss = close,
            footer = if (iconPacks.isEmpty()) "Aucun pack installé. Nimbus accepte les packs au format ADW / Nova du Play Store." else null,
        )
        SettingsDialog.Engine -> ChoiceDialog(
            title = "Moteur de recherche web",
            options = SearchEngine.entries,
            selected = settings.webSearchEngine,
            label = { it.label },
            onSelect = { engine -> update { it.copy(webSearchEngine = engine) }; close() },
            onDismiss = close,
        )
        is SettingsDialog.EditClaudeSession -> {
            val index = dialog.index
            val current = index?.let { settings.claudeSessions.getOrNull(it) }
            ClaudeSessionDialog(
                initial = current,
                onSave = { session ->
                    update { s ->
                        val list = s.claudeSessions.toMutableList()
                        if (current != null && index != null) list[index] = session else list.add(session)
                        // La conversation modifiée reste la destination si elle l'était ; une première devient la destination.
                        val target = when {
                            current != null && s.claudeSessionUrl == current.url -> session.url
                            s.claudeSessions.isEmpty() -> session.url
                            else -> s.claudeSessionUrl
                        }
                        s.copy(claudeSessions = list, claudeSessionUrl = target)
                    }
                    close()
                },
                onDismiss = close,
                onDelete = if (current != null && index != null) {
                    {
                        update { s ->
                            s.copy(
                                claudeSessions = s.claudeSessions.filterIndexed { i, _ -> i != index },
                                claudeSessionUrl = s.claudeSessionUrl.takeIf { it != current.url },
                            )
                        }
                        close()
                    }
                } else {
                    null
                },
            )
        }
        is SettingsDialog.EditCategory -> {
            val list = apps.allCategories
            val index = list.indexOfFirst { it.id == dialog.id }
            val current = list.getOrNull(index)
            CategoryDialog(
                initial = current,
                count = current?.let { c -> apps.visible.count { it.category == c.id } } ?: 0,
                onSave = { name ->
                    if (current == null) vm.addCategory(name) else vm.renameCategory(current.id, name)
                    close()
                },
                onDismiss = close,
                onMoveUp = if (current != null && index > 0) ({ vm.moveCategory(current.id, -1) }) else null,
                onMoveDown = if (current != null && index < list.lastIndex) ({ vm.moveCategory(current.id, 1) }) else null,
                onDelete = if (current != null && current.id != Category.OTHER_ID) {
                    {
                        vm.deleteCategory(current.id)
                        close()
                    }
                } else {
                    null
                },
            )
        }
        SettingsDialog.ResetCategories -> {
            val text = "Les catégories reprennent leur nom et leur ordre d'origine ; celles que tu as créées sont " +
                "supprimées et leurs apps reviennent au classement automatique."
            val reset = {
                vm.resetCategories()
                close()
            }
            if (LocalPaperUi.current) {
                PaperDialog(
                    onDismissRequest = close,
                    title = "Catégories d'origine",
                    actions = {
                        PaperTextButton("Annuler", onClick = close)
                        PaperTextButton("Rétablir", bold = true, onClick = reset)
                    },
                ) { Text(text, style = PaperStyle.body.copy(textAlign = TextAlign.Justify)) }
            } else {
                AlertDialog(
                    onDismissRequest = close,
                    title = { Text("Catégories d'origine") },
                    text = { Text(text) },
                    confirmButton = { TextButton(onClick = reset) { Text("Rétablir") } },
                    dismissButton = { TextButton(onClick = close) { Text("Annuler") } },
                )
            }
        }
        is SettingsDialog.GestureChoice -> ChoiceDialog(
            title = dialog.gesture.label,
            options = GestureAction.entries,
            selected = settings.actionFor(dialog.gesture),
            label = { it.label },
            footer = if (!status.gestureService) {
                "Verrouillage, capture d'écran, applis récentes et menu Marche/Arrêt nécessitent le service d'accessibilité « Gestes Nimbus »."
            } else {
                null
            },
            onSelect = { action ->
                update { it.withAction(dialog.gesture, action) }
                setDialog(if (action == GestureAction.APP) SettingsDialog.GestureApp(dialog.gesture) else null)
                if (action in SERVICE_ACTIONS && !status.gestureService) {
                    SystemActions.toast(context, "Active « Gestes Nimbus » pour cette action")
                    SystemActions.openAccessibilitySettings(context)
                }
            },
            onDismiss = close,
        )
        is SettingsDialog.GestureApp -> AppPickerDialog(
            title = "${dialog.gesture.label} : ouvrir…",
            apps = apps.visible,
            onPick = { app ->
                vm.setGestureApp(dialog.gesture, app)
                close()
            },
            onDismiss = close,
        )
        SettingsDialog.ConfirmImport -> {
            val text = "L'import remplace l'accueil, le dock, les catégories, les apps masquées, les noms et les réglages " +
                "par ceux du fichier. Exporte d'abord ta configuration actuelle si tu veux pouvoir y revenir."
            val choose = {
                close()
                host.importBackup()
            }
            if (LocalPaperUi.current) {
                PaperDialog(
                    onDismissRequest = close,
                    title = "Importer une configuration",
                    actions = {
                        PaperTextButton("Annuler", onClick = close)
                        PaperTextButton("Choisir le fichier", bold = true, onClick = choose)
                    },
                ) { Text(text, style = PaperStyle.body.copy(textAlign = TextAlign.Justify)) }
            } else {
                AlertDialog(
                    onDismissRequest = close,
                    title = { Text("Importer une configuration") },
                    text = { Text(text) },
                    confirmButton = { TextButton(onClick = choose) { Text("Choisir le fichier") } },
                    dismissButton = { TextButton(onClick = close) { Text("Annuler") } },
                )
            }
        }
        SettingsDialog.PinUnlock -> PinDialog(
            title = "Apps masquées",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = {
                close()
                vm.openHiddenApps()
            },
            onDismiss = close,
        )
        SettingsDialog.PinCreate -> PinDialog(
            title = "Protéger les apps masquées",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                close()
                vm.openHiddenApps()
            },
            onDismiss = close,
        )
        SettingsDialog.PinChangeOld -> PinDialog(
            title = "Code actuel",
            mode = PinMode.VERIFY,
            verify = vm::checkPin,
            onSuccess = { setDialog(SettingsDialog.PinChangeNew) },
            onDismiss = close,
        )
        SettingsDialog.PinChangeNew -> PinDialog(
            title = "Nouveau code",
            mode = PinMode.CREATE,
            onSuccess = { pin ->
                vm.setPin(pin)
                close()
                SystemActions.toast(context, "Code PIN modifié")
            },
            onDismiss = close,
        )
        null -> Unit
    }
}

/** Nom lisible d'un dossier SAF (« primary:Download/Nimbus » -> « Download/Nimbus »). */
internal fun folderName(treeUri: String): String = try {
    DocumentsContract.getTreeDocumentId(Uri.parse(treeUri)).substringAfter(':').ifEmpty { "stockage" }
} catch (e: Exception) {
    "dossier choisi"
}
