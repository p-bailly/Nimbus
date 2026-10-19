package fr.perso.nimbus.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Paramètres qui influencent le rendu des icônes. */
data class IconConfig(
    val shape: IconShape,
    val iconPack: String?,
    val wrapLegacy: Boolean,
    val sizePx: Int,
)

/** Fabrique les icônes : pack d'icônes, formes personnalisées, badge profil pro. */
class IconFactory(context: Context) {

    private val appContext = context.applicationContext
    private val pm = appContext.packageManager
    private val density = appContext.resources.displayMetrics.densityDpi
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun clear() = cache.clear()

    fun invalidate(packageName: String) {
        cache.keys.removeAll { it.startsWith("$packageName/") }
    }

    fun create(
        key: String,
        info: LauncherActivityInfo,
        pack: IconPack?,
        config: IconConfig,
        user: UserHandle,
        isWork: Boolean,
    ): ImageBitmap = cache.getOrPut(key) {
        render(key, info, pack, config, user, isWork).asImageBitmap()
    }

    private fun render(
        key: String,
        info: LauncherActivityInfo,
        pack: IconPack?,
        config: IconConfig,
        user: UserHandle,
        isWork: Boolean,
    ): Bitmap {
        val size = config.sizePx
        val loaded: Drawable? = try {
            info.getIcon(density)
        } catch (e: Exception) {
            null
        }
        val raw: Drawable = loaded ?: pm.defaultActivityIcon

        val packIcon = pack?.getIcon(info.componentName)
        var bitmap: Bitmap = when {
            packIcon != null -> drawableToBitmap(packIcon, size)
            pack != null && pack.hasFallback ->
                pack.applyFallback(raw, key, size) ?: shaped(raw, config, size)
            else -> shaped(raw, config, size)
        }

        if (isWork) {
            val badged = pm.getUserBadgedIcon(BitmapDrawable(appContext.resources, bitmap), user)
            bitmap = drawableToBitmap(badged, size)
        }
        return bitmap
    }

    private fun shaped(drawable: Drawable, config: IconConfig, size: Int): Bitmap {
        if (config.shape == IconShape.SYSTEM) return drawableToBitmap(drawable, size)

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = IconShapes.path(config.shape, size.toFloat())

        if (drawable is AdaptiveIconDrawable) {
            // Les calques adaptatifs font 108dp dont 72dp visibles : on agrandit de 1,5x.
            val inset = size / 4
            val bounds = Rect(-inset, -inset, size + inset, size + inset)
            val layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val layerCanvas = Canvas(layer)
            drawable.background?.let { it.bounds = bounds; it.draw(layerCanvas) }
            drawable.foreground?.let { it.bounds = bounds; it.draw(layerCanvas) }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(layer, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.drawPath(path, paint)
        } else if (config.wrapLegacy) {
            // Ancienne icône : posée sur une plaque blanche de la forme choisie.
            val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawPath(path, plate)
            val pad = (size * 0.16f).toInt()
            drawable.setBounds(pad, pad, size - pad, size - pad)
            drawable.draw(canvas)
        } else {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
        return out
    }

    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
        }
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return out
    }
}

/** Chemins des formes d'icônes. */
object IconShapes {
    fun path(shape: IconShape, s: Float): Path = Path().apply {
        when (shape) {
            IconShape.CIRCLE -> addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW)
            IconShape.SQUARE, IconShape.SYSTEM -> addRect(0f, 0f, s, s, Path.Direction.CW)
            IconShape.ROUNDED -> addRoundRect(0f, 0f, s, s, s * 0.22f, s * 0.22f, Path.Direction.CW)
            IconShape.TEARDROP -> {
                val r = s / 2f
                val small = s * 0.12f
                // coins : haut-gauche, haut-droit, bas-droit, bas-gauche
                val radii = floatArrayOf(r, r, r, r, small, small, r, r)
                addRoundRect(0f, 0f, s, s, radii, Path.Direction.CW)
            }
            IconShape.SQUIRCLE -> {
                // Superellipse |x|^4 + |y|^4 = 1
                val c = s / 2.0
                val steps = 96
                for (i in 0..steps) {
                    val t = 2.0 * PI * i / steps
                    val ct = cos(t)
                    val st = sin(t)
                    val x = c + c * sign(ct) * abs(ct).pow(0.5)
                    val y = c + c * sign(st) * abs(st).pow(0.5)
                    if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
                }
                close()
            }
        }
    }
}
