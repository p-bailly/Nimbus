package fr.perso.nimbus.screens.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import fr.perso.nimbus.R
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.data.ThemeMode
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.SystemStatus
import fr.perso.nimbus.ui.settings.AboutPage
import fr.perso.nimbus.ui.settings.AppearancePage
import fr.perso.nimbus.ui.settings.BackupPage
import fr.perso.nimbus.ui.settings.CategoriesPage
import fr.perso.nimbus.ui.settings.DictationPage
import fr.perso.nimbus.ui.settings.DrawerPage
import fr.perso.nimbus.ui.settings.GesturesPage
import fr.perso.nimbus.ui.settings.HomeAppsPage
import fr.perso.nimbus.ui.settings.HomePage
import fr.perso.nimbus.ui.settings.HubPage
import fr.perso.nimbus.ui.settings.LockscreenPage
import fr.perso.nimbus.ui.settings.NotificationsPage
import fr.perso.nimbus.ui.settings.PrivacyPage
import fr.perso.nimbus.ui.settings.SettingId
import fr.perso.nimbus.ui.settings.SettingsBanner
import fr.perso.nimbus.ui.settings.SettingsColors
import fr.perso.nimbus.ui.settings.SettingsCtx
import fr.perso.nimbus.ui.settings.SettingsEntry
import fr.perso.nimbus.ui.settings.SettingsPage
import fr.perso.nimbus.ui.settings.SettingsScreen
import fr.perso.nimbus.ui.settings.settingsSection
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Les pages de réglages, une capture chacune (grandes : une page entière tient sur l'image), sous le thème
 * papier puis sous le thème ambiant (Material). Apps fictives de [UiFixture].
 *
 *   ./gradlew recordRoborazziDebug --tests 'fr.perso.nimbus.screens.ui.SettingsScreenshot'
 * → app/build/outputs/roborazzi/ui/settings_*.png
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h2100dp-xxhdpi")
class SettingsScreenshot {

    private fun ctx(settings: LauncherSettings, focus: SettingId? = null): SettingsCtx {
        val apps = UiFixture.appsState().let { state ->
            state.copy(
                allCategories = fr.perso.nimbus.data.Category.defaults,
                hidden = listOf(UiFixture.app("Banque Populaire"), UiFixture.app("Pronote")),
            )
        }
        return SettingsCtx(
            vm = UiFixture.viewModel(),
            host = FakeHost,
            context = org.robolectric.RuntimeEnvironment.getApplication(),
            settings = settings,
            status = SystemStatus(gestureService = false, dictationService = false, hasPin = true),
            apps = apps,
            paperApps = PaperSlot.resolve(UiFixture.apps, emptyMap(), UiFixture.apps.associateBy { it.key }),
            gestureApps = emptyMap(),
            backupFolder = "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FNimbus",
            wallpaperColor = 0xFF5B7FD6.toInt(),
            iconPacks = emptyList(),
            focus = focus,
            showDialog = {},
        )
    }

    private fun page(name: String, mode: ThemeMode, settings: LauncherSettings, focus: SettingId? = null, content: @Composable SettingsCtx.() -> Unit) =
        UiFixture.shoot("settings_${name}_${if (mode == ThemeMode.PAPER) "paper" else "ambient"}", mode) { ctx(settings, focus).content() }

    private val paper = LauncherSettings(themeMode = ThemeMode.PAPER)
    private val ambient = LauncherSettings(themeMode = ThemeMode.AMBIENT)

    // ---- Papier
    @Test fun hubPaper() = page("hub", ThemeMode.PAPER, paper) { HubPage() }
    @Test fun appearancePaper() = page("appearance", ThemeMode.PAPER, paper) { AppearancePage() }
    @Test fun homePaper() = page("home", ThemeMode.PAPER, paper.copy(homeStyle = HomeStyle.GROUPES)) { HomePage() }
    @Test fun homeGridPaper() = page("homegrid", ThemeMode.PAPER, paper) { HomePage() }
    @Test fun homeAppsPaper() = page("homeapps", ThemeMode.PAPER, paper.copy(homeStyle = HomeStyle.GROUPES)) { HomeAppsPage() }
    @Test fun drawerPaper() = page("drawer", ThemeMode.PAPER, paper) { DrawerPage() }
    @Test fun categoriesPaper() = page("categories", ThemeMode.PAPER, paper) { CategoriesPage() }
    @Test fun gesturesPaper() = page("gestures", ThemeMode.PAPER, paper) { GesturesPage() }
    @Test fun lockscreenPaper() = page("lockscreen", ThemeMode.PAPER, paper.copy(unlockPage = true)) { LockscreenPage() }
    @Test fun privacyPaper() = page("privacy", ThemeMode.PAPER, paper) { PrivacyPage() }
    @Test fun dictationPaper() = page("dictation", ThemeMode.PAPER, paper) { DictationPage() }
    @Test fun notificationsPaper() = page("notifications", ThemeMode.PAPER, paper) { NotificationsPage() }
    @Test fun backupPaper() = page("backup", ThemeMode.PAPER, paper) { BackupPage() }
    @Test fun aboutPaper() = page("about", ThemeMode.PAPER, paper) { AboutPage() }

    /** Arrivée d'une recherche sur « Taille des icônes » : vérifie surtout que la page se construit avec un réglage à montrer. */
    @Test fun focusPaper() = page("focus", ThemeMode.PAPER, paper, focus = SettingId.ICON_SIZE) { AppearancePage() }

    // ---- Ambiant (Material)
    @Test fun hubAmbient() = page("hub", ThemeMode.AMBIENT, ambient) { HubPage() }
    @Test fun appearanceAmbient() = page("appearance", ThemeMode.AMBIENT, ambient) { AppearancePage() }
    @Test fun homeAmbient() = page("home", ThemeMode.AMBIENT, ambient) { HomePage() }
    @Test fun drawerAmbient() = page("drawer", ThemeMode.AMBIENT, ambient) { DrawerPage() }
    @Test fun gesturesAmbient() = page("gestures", ThemeMode.AMBIENT, ambient) { GesturesPage() }
    @Test fun lockscreenAmbient() = page("lockscreen", ThemeMode.AMBIENT, ambient.copy(unlockPage = true)) { LockscreenPage() }
    @Test fun dictationAmbient() = page("dictation", ThemeMode.AMBIENT, ambient) { DictationPage() }
    @Test fun notificationsAmbient() = page("notifications", ThemeMode.AMBIENT, ambient) { NotificationsPage() }

    /** Page principale réelle (modèle de vue sur Robolectric : aucune app, Nimbus n'est pas l'accueil). */
    @Test
    fun mainScreen() {
        val vm = UiFixture.viewModel()
        UiFixture.shoot("settings_screen_main") { SettingsScreen(vm, FakeHost, SettingsSection.MAIN) }
    }

    /** Page de réglages remplie à la main : entrées, sections, cases à cocher, entrée désactivée, note de bas de page. */
    @Test
    fun components() = UiFixture.shoot("settings_components") {
        SettingsPage("Accueil", onBack = {}, folio = "iii") {
            item { SettingsEntry(R.drawable.ms_style, SettingsColors.Blue, "Style de l'accueil", "1 · Groupes", chevron = true) {} }
            settingsSection("Grille d'icônes")
            item { SettingsEntry(R.drawable.ms_title, SettingsColors.Blue, "Noms sous les icônes", checked = true) {} }
            item { SettingsEntry(R.drawable.ms_schedule, SettingsColors.Blue, "Horloge et date", checked = false) {} }
            item {
                SettingsEntry(R.drawable.ms_format_paint, SettingsColors.Orange, "Icônes à thème", "Nécessite Android 13", enabled = false, checked = false) {}
            }
            item { SettingsBanner(R.drawable.ms_app_badging, "Autoriser « Pastilles de notification Nimbus »") {} }
        }
    }
}
