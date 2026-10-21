package fr.perso.nimbus.data

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/** Motif d'une app sur les accueils « papier » : silhouette blanche à teinter, ou lettres à composer. */
sealed interface Glyph {
    class Mask(val bitmap: ImageBitmap) : Glyph
    data class Letters(val text: String) : Glyph
}

/**
 * Silhouettes monochromes des apps, pour les pastilles des accueils « papier ». Ordre de priorité :
 * calque monochrome de l'icône adaptative (Android 13+, celui des icônes à thème du Pixel), puis
 * premier plan de l'icône adaptative, puis l'icône elle-même quand elle a un fond transparent ;
 * une icône pleine (carré coloré) donne l'initiale du nom. La silhouette est recadrée sur son
 * contenu : toutes les apps ont ainsi la même taille de motif.
 */
class GlyphFactory private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val density = appContext.resources.displayMetrics.densityDpi
    private val cache = ConcurrentHashMap<String, Glyph>()

    /** À appeler depuis un fil d'arrière-plan : le dessin des icônes peut prendre quelques millisecondes. */
    fun glyph(app: AppEntry, sizePx: Int): Glyph = cache.getOrPut("${app.key}@$sizePx") { render(app, sizePx) }

    fun invalidate(packageName: String) {
        cache.keys.removeAll { it.startsWith("$packageName/") }
    }

    private fun letters(app: AppEntry): Glyph.Letters {
        val label = app.label.trim()
        return Glyph.Letters(if (label.length <= 2) label else label.take(1).uppercase())
    }

    private fun render(app: AppEntry, sizePx: Int): Glyph {
        val icon: Drawable = try {
            launcherApps.getActivityList(app.packageName, app.user)
                .firstOrNull { it.componentName == app.componentName }
                ?.getIcon(density)
        } catch (e: Exception) {
            null
        } ?: return letters(app)

        // Rendu deux fois plus grand que le motif final, puis réduit : contours nets.
        val work = max(sizePx * 2, 96)
        if (icon is AdaptiveIconDrawable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                icon.monochrome?.let { mono -> mask(mono, work, sizePx, adaptive = true, maxCover = 1f)?.let { return it } }
            }
            icon.foreground?.let { fg -> mask(fg, work, sizePx, adaptive = true, maxCover = 0.55f)?.let { return it } }
            return letters(app)
        }
        return mask(icon, work, sizePx, adaptive = false, maxCover = 0.55f) ?: letters(app)
    }

    /**
     * Silhouette de [drawable] : blanche, opacité d'origine, recadrée puis centrée dans un carré de
     * [sizePx]. null si le motif est vide ou couvre plus de [maxCover] de la zone visible (fond plein).
     */
    private fun mask(drawable: Drawable, work: Int, sizePx: Int, adaptive: Boolean, maxCover: Float): Glyph.Mask? {
        val layer = Bitmap.createBitmap(work, work, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(layer)
        val d = drawable.constantState?.newDrawable(appContext.resources)?.mutate() ?: drawable.mutate()
        if (adaptive) {
            // Calque adaptatif : 108 dp dont 72 dp visibles au centre.
            val inset = work / 4
            d.setBounds(-inset, -inset, work + inset, work + inset)
        } else {
            d.setBounds(0, 0, work, work)
        }
        d.draw(canvas)

        val pixels = IntArray(work * work)
        layer.getPixels(pixels, 0, work, 0, 0, work, work)
        var left = work
        var top = work
        var right = -1
        var bottom = -1
        var covered = 0
        for (y in 0 until work) {
            for (x in 0 until work) {
                if ((pixels[y * work + x] ushr 24) > 40) {
                    covered++
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < 0 || covered / (work * work).toFloat() > maxCover) return null
        // Seule l'opacité compte : le motif devient blanc, la teinte vient de la page.
        for (i in pixels.indices) pixels[i] = (pixels[i] and 0xFF000000.toInt()) or 0x00FFFFFF
        layer.setPixels(pixels, 0, work, 0, 0, work, work)

        val w = right - left + 1
        val h = bottom - top + 1
        val scale = sizePx / max(w, h).toFloat()
        val outW = (w * scale).roundToInt().coerceAtLeast(1)
        val outH = (h * scale).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val dst = Rect((sizePx - outW) / 2, (sizePx - outH) / 2, (sizePx - outW) / 2 + outW, (sizePx - outH) / 2 + outH)
        Canvas(out).drawBitmap(layer, Rect(left, top, right + 1, bottom + 1), dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        layer.recycle()
        return Glyph.Mask(out.asImageBitmap())
    }

    companion object {
        @Volatile
        private var instance: GlyphFactory? = null

        fun get(context: Context): GlyphFactory =
            instance ?: synchronized(this) {
                instance ?: GlyphFactory(context).also { instance = it }
            }
    }
}
