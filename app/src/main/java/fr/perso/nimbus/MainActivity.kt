package fr.perso.nimbus

import android.Manifest
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.NimbusApp
import fr.perso.nimbus.ui.home.PAGE_WIDGETS
import fr.perso.nimbus.widgets.WidgetHostManager
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), LauncherHost {

    private val viewModel: LauncherViewModel by viewModels()
    private val widgetHost by lazy { WidgetHostManager.get(this) }

    /** Widget en cours d'ajout (entre l'autorisation, la configuration et l'enregistrement). */
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

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
        setContent { NimbusApp(vm = viewModel, host = this) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_WIDGET, pendingWidgetId)
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
    override fun addWidget(info: AppWidgetProviderInfo) {
        val id = widgetHost.allocateId()
        pendingWidgetId = id
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
        val density = resources.displayMetrics.density
        val heightDp = (info.minHeight / density).roundToInt() + 16
        viewModel.addWidget(id, heightDp.coerceIn(100, 400))
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        viewModel.showPage(PAGE_WIDGETS)
    }

    private fun cancelWidget(id: Int) {
        widgetHost.deleteId(id)
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private companion object {
        const val KEY_PENDING_WIDGET = "pendingWidgetId"
        const val REQUEST_CONFIGURE_WIDGET = 42
    }
}
