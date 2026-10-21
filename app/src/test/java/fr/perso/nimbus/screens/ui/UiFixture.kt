package fr.perso.nimbus.screens.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Looper
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.core.app.ActivityScenario
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import fr.perso.nimbus.data.AppCategory
import fr.perso.nimbus.data.Category
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.PendingWidget
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherHost
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.theme.NimbusTheme
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowEnvironment
import java.time.Duration

/**
 * Banc des captures de l'interface (réglages, tiroir, menus, dialogues) sous le thème papier, ou un
 * autre thème pour vérifier qu'il n'a pas changé. Apps fictives : leur motif est l'initiale.
 *
 *   ./gradlew recordRoborazziDebug --tests 'fr.perso.nimbus.screens.ui.*'
 * → app/build/outputs/roborazzi/ui/<nom>.png
 */
object UiFixture {
    private val names = listOf(
        "Agenda" to AppCategory.PRODUCTIVITY, "Amazon" to AppCategory.SHOPPING, "Appareil photo" to AppCategory.PHOTO,
        "Banque Populaire" to AppCategory.SHOPPING, "Calculatrice" to AppCategory.TOOLS, "Claude" to AppCategory.PRODUCTIVITY,
        "Discord" to AppCategory.SOCIAL, "Drive" to AppCategory.PRODUCTIVITY, "Fichiers" to AppCategory.TOOLS,
        "GitHub" to AppCategory.PRODUCTIVITY, "Gmail" to AppCategory.COMMUNICATION, "Horloge" to AppCategory.TOOLS,
        "Lichess" to AppCategory.GAMES, "Maps" to AppCategory.TRAVEL, "Messages" to AppCategory.COMMUNICATION,
        "Météo" to AppCategory.TRAVEL, "Photos" to AppCategory.PHOTO, "Play Store" to AppCategory.TOOLS,
        "Pronote" to AppCategory.PRODUCTIVITY, "Reddit" to AppCategory.SOCIAL, "Spotify" to AppCategory.MEDIA,
        "Stremio" to AppCategory.MEDIA, "Téléphone" to AppCategory.COMMUNICATION, "Twitch" to AppCategory.MEDIA,
        "Waze" to AppCategory.TRAVEL, "WhatsApp" to AppCategory.COMMUNICATION, "YouTube" to AppCategory.MEDIA,
    )

    val apps: List<AppEntry> by lazy {
        val user = Process.myUserHandle()
        names.mapIndexed { i, (label, category) ->
            val pkg = "test.app$i"
            AppEntry(
                key = "$pkg/Main#0",
                packageName = pkg,
                componentName = ComponentName(pkg, "$pkg.Main"),
                user = user,
                label = label,
                icon = ImageBitmap(1, 1),
                autoCategory = category,
            )
        }
    }

    fun app(label: String): AppEntry = apps.first { it.label == label }

    /** Vrai modèle de vue (sans apps sous Robolectric) ; il lui faut un stockage externe fictif. */
    fun viewModel(): LauncherViewModel {
        ShadowEnvironment.addExternalDir("sdcard")
        return LauncherViewModel(RuntimeEnvironment.getApplication())
    }

    fun appsState(): AppsState {
        val visible = apps
        return AppsState(
            loaded = true,
            visible = visible,
            favorites = listOf(app("Téléphone"), app("Messages"), app("WhatsApp"), app("Appareil photo")),
            categories = Category.defaults.filter { c -> visible.any { it.category == c.id } },
            frequent = listOf(app("WhatsApp"), app("Claude"), app("Spotify"), app("Maps"), app("Gmail")),
            // WhatsApp est dans le dock : la figure du sommaire commence à Claude.
            mostUsed = listOf("WhatsApp", "Claude", "Spotify", "Maps", "Gmail", "Waze", "Discord").map(::app),
            favoriteKeys = setOf(app("Téléphone"), app("Messages"), app("WhatsApp"), app("Appareil photo")).map { it.key }.toSet(),
            byKey = visible.associateBy { it.key },
        )
    }

    /** Capture d'un écran rendu dans l'activité (sans fenêtres de dialogue). */
    fun shoot(name: String, mode: ThemeMode = ThemeMode.PAPER, content: @Composable () -> Unit) {
        captureRoboImage("build/outputs/roborazzi/ui/$name.png") {
            NimbusTheme(mode = mode, wallpaperColor = null) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
    }

    /** Capture de tout l'écran, fenêtres de dialogue, pop-up et feuilles comprises. */
    fun shootScreen(name: String, mode: ThemeMode = ThemeMode.PAPER, content: @Composable () -> Unit) {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    NimbusTheme(mode = mode, wallpaperColor = null) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
                    }
                }
            }
            // Laisse finir les animations d'ouverture (feuilles) et le chargement des motifs.
            repeat(8) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250))
                Thread.sleep(60)
            }
            shadowOf(Looper.getMainLooper()).idle()
            captureScreenRoboImage("build/outputs/roborazzi/ui/$name.png")
        }
    }
}

/** Hôte sans effet : les captures n'appellent aucune action. */
object FakeHost : LauncherHost {
    override fun addWidget(info: AppWidgetProviderInfo) = Unit
    override fun addWidgets(widgets: List<PendingWidget>) = Unit
    override fun requestContactsPermission() = Unit
    override fun requestFilesPermission() = Unit
    override fun requestDefaultLauncher() = Unit
    override fun setSystemBars(statusLight: Boolean, navigationLight: Boolean) = Unit
    override fun exportBackup() = Unit
    override fun importBackup() = Unit
    override fun chooseAutoBackupFolder() = Unit
    override fun pickCustomIcon(app: AppEntry) = Unit
}
