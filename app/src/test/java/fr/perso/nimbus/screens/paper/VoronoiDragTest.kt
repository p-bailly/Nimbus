package fr.perso.nimbus.screens.paper

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import fr.perso.nimbus.data.PaperSlot
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PaperEnv
import fr.perso.nimbus.ui.paper.layouts.VoronoiPage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot

/**
 * Page « Voronoï » : appui, appui long et glissé d'un site. Repère : 1 unité = 1 dp (3 px), la
 * figure est réduite à 0,95 et décalée de (10,3 ; 46) ; Ω = [14, 398] × [160, 736].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class VoronoiDragTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private var opened: PaperSlot? = null
    private var actions: PaperSlot? = null
    private var saved: Map<PaperSlot, Offset>? = null

    @Before
    fun start() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(context.packageManager).addActivityIfNotPresent(ComponentName(context, ComponentActivity::class.java))
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                var sites by mutableStateOf(emptyMap<PaperSlot, Offset>())
                val base = PaperFixture.env()
                val env = PaperEnv(
                    apps = base.apps,
                    onOpen = { opened = it },
                    onActions = { slot, _ -> actions = slot },
                    fixedTime = base.fixedTime,
                    fixedInsets = base.fixedInsets,
                    animate = false,
                    syncGlyphs = true,
                    sites = sites,
                    onSites = {
                        saved = it
                        sites = it
                    },
                )
                CompositionLocalProvider(LocalPaper provides env) { VoronoiPage() }
            }
        }
    }

    @After
    fun stop() {
        scenario?.close()
    }

    /** Point de la figure, en pixels de l'écran. */
    private fun px(x: Double, y: Double) = Offset((10.3f + 0.95f * x.toFloat()) * 3f, (46f + 0.95f * y.toFloat()) * 3f)

    @Test
    fun tapOpensTheNearestSite() {
        // Entre Lichess (210,7 ; 447,4) et Basket (294,4 ; 447,6), plus près de Basket.
        compose.onRoot().performTouchInput { click(px(262.0, 452.0)) }
        compose.waitForIdle()
        assertEquals(PaperSlot.BASKET, opened)
        assertNull(saved)
    }

    @Test
    fun longPressShowsActions() {
        compose.onRoot().performTouchInput { longClick(px(70.0, 210.0)) }
        compose.waitForIdle()
        assertEquals(PaperSlot.WHATSAPP, actions)
        assertNull(saved)
    }

    @Test
    fun dragMovesTheSiteInsideOmega() {
        // WhatsApp (63,4 ; 205,5) tiré loin vers la gauche et le bas : il s'arrête au bord de Ω.
        compose.onRoot().performTouchInput {
            down(px(63.4, 205.5))
            advanceEventTime(1000)
            repeat(20) { moveBy(Offset(-12f, 1f)) }
            up()
        }
        compose.waitForIdle()
        val edge = saved?.get(PaperSlot.WHATSAPP)!!
        assertEquals(45f, edge.x)
        assertNull(actions)

        // Vers le coin : le « Ω » l'arrête à 40 de son centre (27,5 ; 174,3).
        compose.onRoot().performTouchInput {
            down(px(edge.x.toDouble(), edge.y.toDouble()))
            advanceEventTime(1000)
            repeat(20) { moveBy(Offset(-6f, -12f)) }
            up()
        }
        compose.waitForIdle()
        val corner = saved?.get(PaperSlot.WHATSAPP)!!
        assertTrue(hypot(corner.x - 27.5, corner.y - 174.3) >= 40.0)

        // Puis contre Discord (137,9 ; 275) : il est repoussé à 62 au moins de son site.
        compose.onRoot().performTouchInput {
            down(px(corner.x.toDouble(), corner.y.toDouble()))
            advanceEventTime(1000)
            val target = px(137.9, 275.0) - px(corner.x.toDouble(), corner.y.toDouble())
            repeat(10) { moveBy(target / 10f) }
            up()
        }
        compose.waitForIdle()
        val pushed = saved?.get(PaperSlot.WHATSAPP)!!
        assertTrue(hypot(pushed.x - 137.9, pushed.y - 275.0) >= 62.0)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/paper/h2_voronoi_glisse.png")
    }
}
