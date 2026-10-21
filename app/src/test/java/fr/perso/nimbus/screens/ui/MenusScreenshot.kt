package fr.perso.nimbus.screens.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import fr.perso.nimbus.ui.components.AppActionSheet
import fr.perso.nimbus.ui.components.AppPopup
import fr.perso.nimbus.ui.components.PaperPopupCard
import fr.perso.nimbus.ui.components.WidgetPicker
import fr.perso.nimbus.ui.home.PaperHomeMenu
import fr.perso.nimbus.ui.theme.NimbusBottomSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class MenusScreenshot {

    /** Pop-up d'appui long avec des raccourcis (contenu seul, posé au milieu de la page). */
    @Test
    fun popupCard() = UiFixture.shoot("menu_popup_card") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PaperPopupCard(
                shortcuts = listOf("Nouvelle discussion", "Appareil photo", "Nouvel appel", "Rechercher"),
                onShortcut = {},
                actions = listOf("Infos" to {}, "Accueil" to {}, "Désinstaller" to {}, "Plus" to {}),
            )
        }
    }

    /** Le vrai pop-up, ancré sur une icône (sans raccourcis sous Robolectric). */
    @Test
    fun popup() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("menu_popup") {
            AppPopup(UiFixture.app("WhatsApp"), Rect(500f, 1500f, 660f, 1660f), UiFixture.appsState(), vm)
        }
    }

    /** Feuille d'actions d'une app. */
    @Test
    fun actionSheet() {
        val vm = UiFixture.viewModel()
        UiFixture.shootScreen("menu_action_sheet") {
            AppActionSheet(UiFixture.app("WhatsApp"), UiFixture.appsState(), vm, FakeHost)
        }
    }

    /** Menu de l'appui long sur l'accueil (grille d'icônes : six raccourcis). */
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun homeMenu() = UiFixture.shootScreen("menu_home") {
        NimbusBottomSheet(onDismissRequest = {}, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            PaperHomeMenu(
                shortcuts = listOf(
                    "Style de l'accueil" to {}, "Ajouter une icône" to {}, "Ajouter un widget" to {},
                    "Fond d'écran" to {}, "Toutes les apps" to {}, "Page principale" to {},
                ),
                onAppearance = {}, onHomeSettings = {}, onGestures = {}, onAllSettings = {},
            )
        }
    }

    /** Menu de l'accueil « papier » : trois raccourcis. */
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun homeMenuPaper() = UiFixture.shootScreen("menu_home_paper") {
        NimbusBottomSheet(onDismissRequest = {}, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            PaperHomeMenu(
                shortcuts = listOf("Style de l'accueil" to {}, "Fond d'écran" to {}, "Toutes les apps" to {}),
                onAppearance = {}, onHomeSettings = {}, onGestures = {}, onAllSettings = {},
            )
        }
    }

    @Test
    fun widgetPicker() = UiFixture.shootScreen("menu_widget_picker") {
        WidgetPicker(onDismiss = {}, onPick = {})
    }
}
