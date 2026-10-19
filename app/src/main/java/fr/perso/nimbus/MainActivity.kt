package fr.perso.nimbus

import android.Manifest
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.HomeWidget
import fr.perso.nimbus.data.PendingWidget
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.NimbusApp
import fr.perso.nimbus.ui.home.PAGE_WIDGETS
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), LauncherHost {

    private val viewModel: LauncherViewModel by viewModels()
    private val widgetHost by lazy { WidgetHostManager.get(this) }

    /** Widget en cours d'ajout (entre l'autorisation, la configuration et l'enregistrement). */
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /** Hauteur imposée au widget en cours d'ajout (widgets restaurés après un import). */
    private var pendingWidgetHeightDp: Int? = null

    /** Le widget en cours d'ajout va sur la grille de l'accueil (sinon sur la page widgets). */
    private var pendingWidgetToHome = false

    /** Emplacement imposé sur l'accueil (widget restauré après un import). */
    private var pendingWidgetPlacement: HomeWidget? = null

    /** Widgets restaurés après un import, ajoutés l'un après l'autre. */
    private val widgetQueue = ArrayDeque<PendingWidget>()

    /** Clé de l'app dont on choisit l'icône dans la galerie. */
    private var iconTargetKey: String? = null

    private val exportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(viewModel.exportBackup()) } != null
                } catch (e: Exception) {
                    false
                }
            }
            SystemActions.toast(this@MainActivity, if (ok) "Configuration exportée" else "Échec de l'export")
        }
    }

    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { readUri(uri, MAX_IMPORT_BYTES) }
            if (bytes == null) SystemActions.toast(this@MainActivity, "Fichier illisible ou trop gros")
            else viewModel.importBackup(bytes)
        }
    }

    private val backupFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setAutoBackupFolder(uri.toString())
            SystemActions.toast(this, "Sauvegarde automatique quotidienne activée")
        } catch (e: SecurityException) {
            SystemActions.toast(this, "Dossier inutilisable")
        }
    }

    private val iconImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val app = iconTargetKey?.let { viewModel.apps.value.byKey[it] }
        iconTargetKey = null
        if (uri == null || app == null) return@registerForActivityResult
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { readUri(uri, MAX_ICON_BYTES) }
            if (bytes == null) SystemActions.toast(this@MainActivity, "Image illisible")
            else viewModel.setCustomIcon(app, bytes)
        }
    }

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return@registerForActivityResult
        if (result.resultCode == RESULT_OK) configureWidget(id) else cancelWidget(id)
    }

    private val configureWidgetDirect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onConfigureResult(result.resultCode)
    }

    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.refreshSystemStatus()
        if (!granted) SystemActions.toast(this, "Sans accès aux contacts, la recherche se limite aux apps et au web")
    }

    private val homeRole = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.refreshSystemStatus()
        // Demande refusée (ou déjà refusée deux fois : le système ne la montre plus).
        if (result.resultCode != RESULT_OK && !SystemActions.isDefaultLauncher(this)) openHomeSettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightSystemBars(false)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        savedInstanceState?.let { restoreTransientState(it) }
        setContent { NimbusApp(vm = viewModel, host = this) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_WIDGET, pendingWidgetId)
        // Choix d'icône et widgets en attente survivent à une recréation (passage en mode sombre…).
        outState.putString(KEY_ICON_TARGET, iconTargetKey)
        outState.putInt(KEY_PENDING_HEIGHT, pendingWidgetHeightDp ?: -1)
        outState.putBoolean(KEY_PENDING_TO_HOME, pendingWidgetToHome)
        outState.putIntArray(KEY_PENDING_PLACEMENT, pendingWidgetPlacement?.let { intArrayOf(it.x, it.y, it.spanX, it.spanY) })
        outState.putIntArray(
            KEY_QUEUE_HOME,
            widgetQueue.flatMap { w -> w.home?.let { listOf(it.x, it.y, it.spanX, it.spanY) } ?: listOf(-1, -1, -1, -1) }.toIntArray(),
        )
        outState.putStringArrayList(KEY_QUEUE_PROVIDERS, ArrayList(widgetQueue.map { it.provider }))
        outState.putIntArray(KEY_QUEUE_HEIGHTS, widgetQueue.map { it.heightDp ?: -1 }.toIntArray())
        outState.putFloatArray(KEY_QUEUE_FRACTIONS, widgetQueue.map { it.heightFraction ?: -1f }.toFloatArray())
    }

    private fun restoreTransientState(state: Bundle) {
        iconTargetKey = state.getString(KEY_ICON_TARGET)
        pendingWidgetHeightDp = state.getInt(KEY_PENDING_HEIGHT, -1).takeIf { it > 0 }
        pendingWidgetToHome = state.getBoolean(KEY_PENDING_TO_HOME, false)
        pendingWidgetPlacement = state.getIntArray(KEY_PENDING_PLACEMENT)?.takeIf { it.size == 4 }
            ?.let { HomeWidget(0, it[0], it[1], it[2], it[3]) }
        val homes = state.getIntArray(KEY_QUEUE_HOME) ?: IntArray(0)
        val providers = state.getStringArrayList(KEY_QUEUE_PROVIDERS).orEmpty()
        val heights = state.getIntArray(KEY_QUEUE_HEIGHTS) ?: IntArray(0)
        val fractions = state.getFloatArray(KEY_QUEUE_FRACTIONS) ?: FloatArray(0)
        providers.forEachIndexed { i, provider ->
            widgetQueue.add(
                PendingWidget(
                    provider,
                    heightDp = heights.getOrNull(i)?.takeIf { it > 0 },
                    heightFraction = fractions.getOrNull(i)?.takeIf { it > 0f },
                    home = homes.getOrNull(i * 4 + 2)?.takeIf { it > 0 }?.let {
                        HomeWidget(0, homes[i * 4], homes[i * 4 + 1], homes[i * 4 + 2], homes[i * 4 + 3])
                    },
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatus()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
        viewModel.onLauncherStopped()
    }

    /** Appui sur le bouton Accueil alors que Nimbus est déjà lancé. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.goHome()
        }
    }

    // ---------------------------------------------------------------- LauncherHost

    override fun setLightSystemBars(light: Boolean) {
        val style = if (light) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun exportBackup() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        try {
            exportFile.launch("Nimbus-$date.json")
        } catch (e: ActivityNotFoundException) {
            SystemActions.toast(this, "Aucun gestionnaire de fichiers")
        }
    }

    override fun importBackup() {
        try {
            // Les .slbk n'ont pas de type MIME connu : on accepte tout.
            importFile.launch(arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
            SystemActions.toast(this, "Aucun gestionnaire de fichiers")
        }
    }

    override fun chooseAutoBackupFolder() {
        try {
            backupFolder.launch(null)
        } catch (e: ActivityNotFoundException) {
            SystemActions.toast(this, "Aucun gestionnaire de fichiers")
        }
    }

    override fun pickCustomIcon(app: AppEntry) {
        iconTargetKey = app.key
        iconImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun readUri(uri: Uri, maxBytes: Int): ByteArray? = try {
        contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                if (total > maxBytes) return null
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }

    override fun addWidgets(widgets: List<PendingWidget>) {
        widgetQueue.addAll(widgets)
        if (pendingWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) addNextQueuedWidget()
    }

    /** Ajoute le prochain widget restauré dont le fournisseur est installé. */
    private fun addNextQueuedWidget() {
        while (widgetQueue.isNotEmpty()) {
            val next = widgetQueue.removeFirst()
            val component = ComponentName.unflattenFromString(next.provider) ?: continue
            val info = widgetHost.providers().firstOrNull { it.provider == component }
            if (info == null) {
                SystemActions.toast(this, "Widget introuvable : ${component.packageName}")
                continue
            }
            val metrics = resources.displayMetrics
            pendingWidgetHeightDp = next.heightDp
                ?: next.heightFraction?.let { (it * metrics.heightPixels / metrics.density).roundToInt() }
            startAddWidget(info, toHome = next.home != null, placement = next.home)
            return
        }
    }

    override fun requestContactsPermission() {
        contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    override fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                try {
                    homeRole.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    return
                } catch (e: ActivityNotFoundException) {
                    // pas de dialogue de rôle sur cet appareil : réglages Accueil
                }
            }
        }
        openHomeSettings()
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** Étape 1 : lier le widget à Nimbus (autorisation demandée à l'utilisateur la première fois). */
    override fun addWidget(info: AppWidgetProviderInfo, toHome: Boolean) = startAddWidget(info, toHome, placement = null)

    /** L'état « en attente » est posé avant la liaison : elle peut aboutir immédiatement (sans dialogue). */
    private fun startAddWidget(info: AppWidgetProviderInfo, toHome: Boolean, placement: HomeWidget?) {
        val id = widgetHost.allocateId()
        pendingWidgetId = id
        pendingWidgetToHome = toHome
        pendingWidgetPlacement = placement
        if (widgetHost.bindIfAllowed(id, info)) {
            configureWidget(id)
            return
        }
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
        try {
            bindWidget.launch(intent)
        } catch (e: ActivityNotFoundException) {
            cancelWidget(id)
            SystemActions.toast(this, "Impossible d'ajouter ce widget")
        }
    }

    /** Étape 2 : écran de configuration du widget, s'il en a un. */
    private fun configureWidget(id: Int) {
        val info = widgetHost.info(id) ?: return cancelWidget(id)
        if (!needsConfiguration(info)) {
            commitWidget(id, info)
            return
        }
        try {
            // Passe par l'hôte : fonctionne aussi pour les activités non exportées et le profil pro.
            widgetHost.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQUEST_CONFIGURE_WIDGET, null)
        } catch (e: Exception) {
            try {
                configureWidgetDirect.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .setComponent(info.configure)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                )
            } catch (e2: Exception) {
                commitWidget(id, info)
            }
        }
    }

    private fun needsConfiguration(info: AppWidgetProviderInfo): Boolean {
        if (info.configure == null) return false
        // Android 12+ : configuration facultative et modifiable plus tard -> on l'ajoute directement.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val features = info.widgetFeatures
            if (features and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0 &&
                features and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0
            ) return false
        }
        return true
    }

    @Deprecated("Résultat de startAppWidgetConfigureActivityForResult, qui n'a pas d'équivalent ActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) onConfigureResult(resultCode)
    }

    private fun onConfigureResult(resultCode: Int) {
        val id = pendingWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val info = widgetHost.info(id)
        if (resultCode == RESULT_OK && info != null) commitWidget(id, info) else cancelWidget(id)
    }

    /** Étape 3 : enregistrer le widget sur la page widgets, à sa hauteur minimale déclarée. */
    private fun commitWidget(id: Int, info: AppWidgetProviderInfo) {
        val onHome = pendingWidgetToHome && run {
            val placement = pendingWidgetPlacement
            val (spanX, spanY) = placement?.let { it.spanX to it.spanY } ?: viewModel.defaultWidgetSpan(info)
            viewModel.addHomeWidget(id, spanX, spanY, placement?.let { it.x to it.y })
        }
        if (pendingWidgetToHome && !onHome) {
            SystemActions.toast(this, "Plus de place sur l'accueil : widget ajouté à la page widgets")
        }
        if (!onHome) {
            val density = resources.displayMetrics.density
            val heightDp = pendingWidgetHeightDp?.coerceIn(60, 600)
                ?: ((info.minHeight / density).roundToInt() + 16).coerceIn(100, 400)
            viewModel.addWidget(id, heightDp)
        }
        clearPendingWidget()
        viewModel.showPage(if (onHome) 0 else PAGE_WIDGETS)
        addNextQueuedWidget()
    }

    private fun cancelWidget(id: Int) {
        widgetHost.deleteId(id)
        clearPendingWidget()
        addNextQueuedWidget()
    }

    private fun clearPendingWidget() {
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingWidgetHeightDp = null
        pendingWidgetToHome = false
        pendingWidgetPlacement = null
    }

    private companion object {
        const val KEY_PENDING_WIDGET = "pendingWidgetId"
        const val KEY_ICON_TARGET = "iconTargetKey"
        const val KEY_PENDING_HEIGHT = "pendingWidgetHeightDp"
        const val KEY_QUEUE_PROVIDERS = "widgetQueueProviders"
        const val KEY_QUEUE_HEIGHTS = "widgetQueueHeights"
        const val KEY_QUEUE_FRACTIONS = "widgetQueueFractions"
        const val KEY_QUEUE_HOME = "widgetQueueHome"
        const val KEY_PENDING_TO_HOME = "pendingWidgetToHome"
        const val KEY_PENDING_PLACEMENT = "pendingWidgetPlacement"
        const val REQUEST_CONFIGURE_WIDGET = 42
        const val MAX_IMPORT_BYTES = 64 * 1024 * 1024
        const val MAX_ICON_BYTES = 10 * 1024 * 1024
    }
}
