package fr.perso.nimbus.ui.paper

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GlyphFactory
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.HomeStyle
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.LocalGlyphChoices
import fr.perso.nimbus.ui.home.homeBackground
import fr.perso.nimbus.ui.home.multiTouchGestures
import fr.perso.nimbus.ui.paper.layouts.EnsemblesPage
import fr.perso.nimbus.ui.paper.layouts.FourierPage
import fr.perso.nimbus.ui.paper.layouts.GroupesPage
import fr.perso.nimbus.ui.paper.layouts.LapinPage
import fr.perso.nimbus.ui.paper.layouts.RemerciementsPage
import fr.perso.nimbus.ui.paper.layouts.SierpinskiPage
import fr.perso.nimbus.ui.paper.layouts.VoronoiPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Accueil « papier » : une page d'article où les 27 applis des places sont posées sur une figure.
 * Appui sur une appli : l'ouvrir (place vide : choisir l'appli) ; appui long : ses actions, dont
 * « Changer ». Les gestes de l'accueil (glisser, double tap, deux doigts, appui long sur le fond)
 * restent ceux de la grille.
 */
@Composable
fun PaperHome(vm: LauncherViewModel, settings: LauncherSettings, onLongPress: (Offset) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val paperApps by vm.paperApps.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val sites by vm.voronoiSites.collectAsStateWithLifecycle()
    val colors = rememberSlotColors(paperApps, settings.paperAppColors)
    val env = remember(paperApps, badges, sites, colors) {
        PaperEnv(
            apps = paperApps,
            badges = badges,
            onOpen = { slot -> paperApps[slot]?.let(vm::launch) ?: vm.showPaperSlotPicker(slot) },
            onActions = { slot, anchor ->
                paperApps[slot]?.let { vm.showAppPopup(it, anchor, slot) } ?: vm.showPaperSlotPicker(slot)
            },
            // L'heure et la date sont déjà dans la barre d'état : la place va aux applis.
            clock = false,
            sites = sites,
            onSites = vm::setVoronoiSites,
            colors = colors,
        )
    }
    val doubleTapEnabled = settings.doubleTapAction != GestureAction.NONE
    val multiTouchEnabled = listOf(Gesture.TWO_FINGER_UP, Gesture.TWO_FINGER_DOWN, Gesture.PINCH_IN, Gesture.PINCH_OUT)
        .any { settings.actionFor(it) != GestureAction.NONE }

    Box(
        Modifier
            .fillMaxSize()
            .then(if (multiTouchEnabled) Modifier.multiTouchGestures { vm.onGesture(it) } else Modifier)
            .pointerInput(Unit) {
                val threshold = 56.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> vm.onGesture(Gesture.SWIPE_UP)
                            total > threshold -> vm.onGesture(Gesture.SWIPE_DOWN)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                )
            },
    ) {
        // Sous la page : double tap et appui long sur le papier nu.
        Box(
            Modifier
                .fillMaxSize()
                .homeBackground(
                    doubleTapEnabled = doubleTapEnabled,
                    onTap = {},
                    onDoubleTap = { vm.onGesture(Gesture.DOUBLE_TAP) },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(it)
                    },
                ),
        )
        CompositionLocalProvider(LocalPaper provides env) { PaperLayout(settings.homeStyle) }
    }
}

/**
 * Couleur propre de l'app de chaque place (voir [GlyphFactory.color], celle du sommaire), calculée en
 * arrière-plan ; vide si [enabled] est faux (couleurs des familles) ou en attendant.
 */
@Composable
private fun rememberSlotColors(apps: Map<PaperSlot, AppEntry?>, enabled: Boolean): Map<PaperSlot, Color> {
    val context = LocalContext.current
    val choices = LocalGlyphChoices.current
    return produceState(emptyMap(), apps, enabled, choices) {
        value = if (!enabled) {
            emptyMap()
        } else {
            withContext(Dispatchers.Default) {
                val factory = GlyphFactory.get(context)
                apps.mapNotNull { (slot, app) ->
                    app ?: return@mapNotNull null
                    val (choice, other) = choices.resolve(app)
                    runCatching { slot to Color(factory.color(app, choice, other)) }.getOrNull()
                }.toMap()
            }
        }
    }.value
}

/** La page d'un style « papier ». */
@Composable
fun PaperLayout(style: HomeStyle) {
    when (style) {
        HomeStyle.GRID -> Unit
        HomeStyle.GROUPES -> GroupesPage()
        HomeStyle.VORONOI -> VoronoiPage()
        HomeStyle.ENSEMBLES -> EnsemblesPage()
        HomeStyle.SIERPINSKI -> SierpinskiPage()
        HomeStyle.FOURIER -> FourierPage()
        HomeStyle.LAPIN -> LapinPage()
        HomeStyle.REMERCIEMENTS -> RemerciementsPage()
    }
}
