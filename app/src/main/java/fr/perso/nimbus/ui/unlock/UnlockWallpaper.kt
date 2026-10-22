package fr.perso.nimbus.ui.unlock

import android.annotation.SuppressLint
import android.app.Presentation
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import fr.perso.nimbus.ui.paper.LocalPaper
import fr.perso.nimbus.ui.paper.PaperEnv
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperShapes
import fr.perso.nimbus.ui.theme.PaperTypography
import fr.perso.nimbus.ui.theme.paperColorScheme
import kotlinx.coroutines.launch

/**
 * L'écran verrouillé en fond d'écran animé : la page de déverrouillage dessinée sous l'heure du système
 * (petite horloge du Pixel, en haut à gauche), sans heure à elle ni lieu, la météo à côté de l'heure.
 * Rien ne passe devant l'écran verrouillé : l'empreinte et le visage déverrouillent directement, mais la
 * page ne reçoit pas les appuis.
 *
 * La page est composée dans une [Presentation] posée sur un écran virtuel privé dont la surface est celle
 * du fond d'écran. À chaque réveil (le fond redevient visible), une nouvelle figure, et le coloriage de
 * Météo monte et se colore de nouveau ; les données ne sont suivies que pendant qu'il est visible.
 */
class UnlockWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = PageEngine()

    private inner class PageEngine : Engine() {
        private val owner = PageOwner()
        private var display: VirtualDisplay? = null
        private var presentation: Presentation? = null

        /** Réveils : la page est recomposée à chacun (animation du coloriage). */
        private var wake by mutableIntStateOf(0)
        private var figure by mutableStateOf<DailyFigure?>(null)

        private val context: Context get() = this@UnlockWallpaper

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            setOffsetNotificationsEnabled(false)
            figure = FigureDeck.upcoming(context)
            owner.create()
            val feed = UnlockFeed.get(context)
            // Visible : tout relire, puis suivre les matchs ; caché (écran éteint, appli ouverte) : rien.
            owner.lifecycleScope.launch { owner.repeatOnLifecycle(Lifecycle.State.RESUMED) { feed.run() } }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            val dpi = resources.displayMetrics.densityDpi
            display?.let {
                it.surface = holder.surface
                it.resize(width, height, dpi)
                return
            }
            val virtual = getSystemService(DisplayManager::class.java)
                .createVirtualDisplay("nimbus-ecran-verrouille", width, height, dpi, holder.surface, 0)
            display = virtual
            presentation = Presentation(context, virtual.display, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen).apply {
                window?.setBackgroundDrawable(ColorDrawable(PaperColors.Paper.toArgb()))
                setContentView(page(this.context))
                window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                window?.decorView?.let {
                    it.setViewTreeLifecycleOwner(owner)
                    it.setViewTreeSavedStateRegistryOwner(owner)
                }
                show()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                // Une nouvelle figure à chaque réveil (pas dans l'aperçu du choix du fond d'écran).
                if (!isPreview) figure = FigureDeck.advance(context)
                wake++
                owner.resume()
            } else {
                owner.pause()
                // Écran éteint : la page sera à jour au prochain réveil.
                if (getSystemService(PowerManager::class.java)?.isInteractive == false) UnlockFeed.get(context).prefetch()
            }
        }

        /** Couleurs du papier, texte foncé : le système écrit l'heure et la barre d'état en encre. */
        override fun onComputeColors(): WallpaperColors {
            val accent = android.graphics.Color.valueOf(Accent.toArgb())
            val paper = android.graphics.Color.valueOf(PaperColors.Paper.toArgb())
            val ink = android.graphics.Color.valueOf(PaperColors.Ink.toArgb())
            return if (Build.VERSION.SDK_INT >= 31) {
                WallpaperColors(accent, paper, ink, WallpaperColors.HINT_SUPPORTS_DARK_TEXT)
            } else {
                WallpaperColors(accent, paper, ink)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            release()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            release()
            owner.destroy()
            super.onDestroy()
        }

        private fun release() {
            presentation?.dismiss()
            presentation = null
            display?.release()
            display = null
        }

        /** La page, comme celle qui passe devant l'écran verrouillé, mais sous l'heure du système. */
        private fun page(context: Context) = ComposeView(context).apply {
            val feed = UnlockFeed.get(context)
            val env = PaperEnv(apps = emptyMap(), fixedInsets = bars(context))
            setContent {
                val state by feed.state.collectAsStateWithLifecycle()
                val notes = rememberMessageNotes()
                MaterialTheme(colorScheme = paperColorScheme(), typography = PaperTypography, shapes = PaperShapes) {
                    CompositionLocalProvider(LocalPaper provides env) {
                        key(wake) {
                            UnlockPage(
                                state.copy(notes = notes),
                                onMeteo = {},
                                onBasket = {},
                                onUnlock = {},
                                onSketchText = feed::sketchText,
                                figure = figure,
                                wallpaper = true,
                            )
                        }
                    }
                }
            }
        }
    }

    /** Cycle de vie de la page : créée avec le fond d'écran, au premier plan tant qu'il est visible. */
    private class PageOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

        fun create() {
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.CREATED
        }

        fun resume() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun pause() {
            if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) registry.currentState = Lifecycle.State.STARTED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    companion object {
        /** Couleur que le système tire du fond (l'heure du Pixel) : un brun d'encre, sur le crème du papier. */
        private val Accent = Color(0xFF6E4E24)

        /**
         * Barres du système (dp) : l'écran virtuel n'en a pas, la page laisse la place de celles de l'écran
         * verrouillé.
         */
        @SuppressLint("DiscouragedApi", "InternalInsetResource")
        private fun bars(context: Context): Pair<Float, Float> {
            val res = context.resources
            fun dp(name: String): Float {
                val id = res.getIdentifier(name, "dimen", "android")
                return if (id == 0) 0f else res.getDimension(id) / res.displayMetrics.density
            }
            return dp("status_bar_height") to dp("navigation_bar_height")
        }

        fun component(context: Context) = ComponentName(context, UnlockWallpaper::class.java)

        /**
         * Le fond de l'écran verrouillé est cette page (choisi pour l'écran verrouillé seul, ou pour les deux
         * écrans) : la page n'a plus à passer devant.
         */
        fun onLockScreen(context: Context): Boolean = try {
            val manager = WallpaperManager.getInstance(context)
            // Sans fond propre à l'écran verrouillé (identifiant < 0), il reprend celui de l'accueil.
            val shared = manager.getWallpaperId(WallpaperManager.FLAG_LOCK) < 0
            val info = if (Build.VERSION.SDK_INT >= 34 && !shared) {
                manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)
            } else {
                manager.wallpaperInfo.takeIf { shared }
            }
            info?.component == component(context)
        } catch (e: Exception) {
            false
        }
    }
}
