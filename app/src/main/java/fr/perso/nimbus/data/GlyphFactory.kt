package fr.perso.nimbus.data

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.PathParser
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/** Motif d'une app sur les accueils « papier » : silhouette blanche à teinter, ou lettres à composer. */
sealed interface Glyph {
    /** [colors] : le même motif dans les couleurs de l'icône (applis Google…), pour le sommaire. */
    class Mask(val bitmap: ImageBitmap, val colors: ImageBitmap? = null) : Glyph
    data class Letters(val text: String) : Glyph
}

/**
 * Logos dessinés pour Nimbus (ceux des maquettes, tracés SVG sur 24 × 24), pour remplacer une
 * silhouette qui ne convient pas ; [argb] : leur couleur dans le sommaire (null : celle de l'app).
 */
enum class PaperLogo(val label: String, val argb: Long?, val path: String) {
    YOUTUBE(
        "Logo YouTube", 0xFFE0201B,
        "M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 " +
            ".502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 " +
            "9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z",
    ),
    /** Banque Populaire : « + » et « x ». */
    PLUS_X(
        "Logo +x", null,
        "M3 10.5h3v-3h2.5v3h3V13h-3v3H6v-3H3zM13.2 7.5h2.9l1.9 2.9 1.9-2.9h2.9l-3.3 4.75 3.3 4.75h-2.9L18 14.1l-1.9 2.9h-2.9l3.3-4.75z",
    ),
    /** Prof de maths (πr). */
    PI_R(
        "Logo πr", null,
        "M3.5 7h9.5v1.8H3.5zM5.6 8.8h1.8v8.4H5.6zM9.6 8.8h1.8v6.2c0 .5.3.7.8.7h.6v1.6h-1c-1.4 0-2.2-.8-2.2-2.2zM14.5 10.5h1.7v1.3" +
            "c.6-1 1.6-1.5 2.9-1.5h.6v1.8h-.8c-1.6 0-2.7 1-2.7 2.9v2.2h-1.7z",
    ),
}

/**
 * Motif choisi pour une app à la place de sa silhouette : un logo de Nimbus, des lettres (« +x »,
 * « πr »), ou la silhouette d'une autre app. Enregistré sous la forme de [spec].
 */
sealed interface GlyphChoice {
    val spec: String

    data class Logo(val logo: PaperLogo) : GlyphChoice {
        override val spec: String get() = "logo:${logo.name}"
    }

    data class Text(val text: String) : GlyphChoice {
        override val spec: String get() = "text:$text"
    }

    /** Silhouette (et couleur) de l'app de clé [key]. */
    data class OfApp(val key: String) : GlyphChoice {
        override val spec: String get() = "app:$key"
    }

    /** La silhouette de l'app elle-même, même sur une place qui propose un autre motif ([PaperSlot.glyph]). */
    data object Own : GlyphChoice {
        override val spec: String get() = "own"
    }

    /** Silhouette dans les couleurs de l'icône (le sommaire la montre en couleur : applis Google…). */
    data object Colored : GlyphChoice {
        override val spec: String get() = "color"
    }

    /**
     * Logo détouré de l'icône en couleur : ce qui se distingue de son fond (le dessin blanc d'une icône
     * pleine, comme BlaBlaCar ou SNCF Connect), quand le calque à thème ne convient pas.
     */
    data object Detoured : GlyphChoice {
        override val spec: String get() = "cut"
    }

    companion object {
        fun parse(spec: String): GlyphChoice? {
            when (spec) {
                Own.spec -> return Own
                Colored.spec -> return Colored
                Detoured.spec -> return Detoured
            }
            val value = spec.substringAfter(':', "")
            if (value.isEmpty()) return null
            return when (spec.substringBefore(':')) {
                "logo" -> PaperLogo.entries.firstOrNull { it.name == value }?.let(::Logo)
                "text" -> Text(value)
                "app" -> OfApp(value)
                else -> null
            }
        }
    }
}

/**
 * Motifs choisis pour les apps (choix de l'utilisateur et motifs des places), avec les apps connues
 * pour retrouver celle dont la silhouette est empruntée.
 */
class GlyphChoices(
    private val choices: Map<String, GlyphChoice> = emptyMap(),
    private val byKey: Map<String, AppEntry> = emptyMap(),
) {
    /**
     * Motif de [app] (null : aucun choix) et, pour la silhouette d'une autre app, cette app. Si
     * celle-ci a elle-même un logo ou des lettres, ce sont eux qui sont empruntés.
     */
    fun resolve(app: AppEntry): Pair<GlyphChoice?, AppEntry?> {
        val choice = choices[app.key] ?: return null to null
        if (choice !is GlyphChoice.OfApp) return choice to null
        val other = byKey[choice.key] ?: return null to null
        val borrowed = choices[other.key]
        if (borrowed is GlyphChoice.Logo || borrowed is GlyphChoice.Text) return borrowed to null
        // Couleurs ou détourage de l'autre app : ils se calculent sur son icône, on prend sa silhouette.
        return choice to other
    }

    operator fun get(key: String): GlyphChoice? = choices[key]

    override fun equals(other: Any?): Boolean = other is GlyphChoices && other.choices == choices && other.byKey === byKey
    override fun hashCode(): Int = choices.hashCode()
}

/**
 * Silhouettes monochromes des apps, pour les pastilles des accueils « papier ». Ordre de priorité :
 * calque monochrome de l'icône adaptative (Android 13+, celui des icônes à thème du Pixel), puis
 * premier plan de l'icône adaptative, puis l'icône elle-même quand elle a un fond transparent ;
 * une icône pleine (carré coloré) donne l'initiale du nom. La silhouette est recadrée sur son
 * contenu : toutes les apps ont ainsi la même taille de motif. Un [GlyphChoice] la remplace.
 */
class GlyphFactory private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val density = appContext.resources.displayMetrics.densityDpi
    private val cache = ConcurrentHashMap<String, Glyph>()
    private val colors = ConcurrentHashMap<String, Int>()

    /**
     * Motif de [app] dans un carré de [sizePx], selon [choice] (null : sa propre silhouette) ;
     * [other] : l'app dont la silhouette est choisie ([GlyphChoice.OfApp]).
     * À appeler depuis un fil d'arrière-plan : le dessin des icônes peut prendre quelques millisecondes.
     */
    fun glyph(app: AppEntry, sizePx: Int, choice: GlyphChoice? = null, other: AppEntry? = null): Glyph = when (choice) {
        is GlyphChoice.Logo -> cache.getOrPut("logo:${choice.logo.name}@$sizePx") { logo(choice.logo, sizePx) }
        is GlyphChoice.Text -> Glyph.Letters(choice.text)
        is GlyphChoice.OfApp -> glyph(other ?: app, sizePx)
        GlyphChoice.Colored -> cache.getOrPut("${app.key}@$sizePx:color") { colored(app, sizePx) ?: glyph(app, sizePx) }
        GlyphChoice.Detoured -> cache.getOrPut("${app.key}@$sizePx:cut") {
            icon(app)?.let { detoured(it, sizePx) } ?: glyph(app, sizePx)
        }
        GlyphChoice.Own, null -> cache.getOrPut("${app.key}@$sizePx") { render(app, sizePx) }
    }

    /**
     * Couleur propre de l'app (ARGB) : teinte dominante de son icône d'origine, assez foncée pour se
     * lire sur le papier ; l'encre si l'icône est grise. Un logo a sa couleur, une autre app la sienne.
     */
    fun color(app: AppEntry, choice: GlyphChoice? = null, other: AppEntry? = null): Int {
        AppBrands.color(app)?.let { return it.toInt() }
        if (choice is GlyphChoice.Logo) choice.logo.argb?.let { return it.toInt() }
        if (choice is GlyphChoice.OfApp && other != null) return color(other)
        return colors.getOrPut(app.key) { dominantColor(app) }
    }

    fun invalidate(packageName: String) {
        cache.keys.removeAll { it.startsWith("$packageName/") }
        colors.keys.removeAll { it.startsWith("$packageName/") }
    }

    private fun letters(app: AppEntry): Glyph.Letters {
        val label = app.label.trim()
        return Glyph.Letters(if (label.length <= 2) label else label.take(1).uppercase())
    }

    private fun icon(app: AppEntry): Drawable? = try {
        launcherApps.getActivityList(app.packageName, app.user)
            .firstOrNull { it.componentName == app.componentName }
            ?.getIcon(density)
    } catch (e: Exception) {
        null
    }

    private fun render(app: AppEntry, sizePx: Int): Glyph {
        val icon = icon(app) ?: return letters(app)
        // Contour mesuré sur un rendu d'au moins 96 px, avant le dessin à la taille finale.
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
     * Silhouette de [drawable] : blanche, opacité d'origine (renforcée, voir [ALPHA_GAIN]), recadrée
     * puis centrée dans un carré de [sizePx]. null si le motif est vide ou couvre plus de [maxCover] de
     * la zone visible (fond plein). Le contour est mesuré sur un premier rendu de [work] px, puis le
     * dessin est refait à la taille finale (deux fois plus grand, puis réduit) : un motif petit dans son
     * icône n'est pas agrandi après coup, il reste net.
     */
    private fun mask(drawable: Drawable, work: Int, sizePx: Int, adaptive: Boolean, maxCover: Float, keepColors: Boolean = false): Glyph.Mask? {
        val d = drawable.constantState?.newDrawable(appContext.resources)?.mutate() ?: drawable.mutate()
        if (adaptive) {
            // Calque adaptatif : 108 dp dont 72 dp visibles au centre.
            val inset = work / 4
            d.setBounds(-inset, -inset, work + inset, work + inset)
        } else {
            d.setBounds(0, 0, work, work)
        }
        val probe = Bitmap.createBitmap(work, work, Bitmap.Config.ARGB_8888)
        d.draw(Canvas(probe))
        val pixels = IntArray(work * work)
        probe.getPixels(pixels, 0, work, 0, 0, work, work)
        probe.recycle()
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

        // Second rendu : le contour remplit le carré final, sur-échantillonné deux fois.
        val big = sizePx * 2
        val scale = big / max(right - left + 1, bottom - top + 1).toFloat()
        val layer = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(layer)
        canvas.translate(big / 2f, big / 2f)
        canvas.scale(scale, scale)
        canvas.translate(-(left + right + 1) / 2f, -(top + bottom + 1) / 2f)
        d.draw(canvas)
        val out = IntArray(big * big)
        layer.getPixels(out, 0, big, 0, 0, big, big)
        // Les couleurs d'origine, sauf si elles sont presque toutes blanches (illisibles sur le papier).
        val colored = if (keepColors && !mostlyLight(out)) {
            Bitmap.createScaledBitmap(layer, sizePx, sizePx, true).asImageBitmap()
        } else {
            null
        }
        if (keepColors && colored == null) {
            layer.recycle()
            return null
        }
        // Seule l'opacité compte : le motif devient blanc, la teinte vient de la page. Les parties
        // translucides (calques à thème en plusieurs opacités) sont renforcées, sinon elles paraissent floues.
        for (i in out.indices) {
            val alpha = ((out[i] ushr 24) * ALPHA_GAIN).roundToInt().coerceAtMost(255)
            out[i] = (alpha shl 24) or 0x00FFFFFF
        }
        layer.setPixels(out, 0, big, 0, 0, big, big)
        return Glyph.Mask(downscale(layer, sizePx).asImageBitmap(), colored)
    }

    /** Plus de 60 % des pixels visibles presque blancs. */
    private fun mostlyLight(pixels: IntArray): Boolean {
        var visible = 0
        var light = 0
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            visible++
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (r > 215 && g > 215 && b > 215) light++
        }
        return visible == 0 || light > visible * 0.6f
    }

    /** Silhouette en couleur : premier plan de l'icône adaptative (ou l'icône), couleurs gardées. */
    private fun colored(app: AppEntry, sizePx: Int): Glyph.Mask? {
        val icon = icon(app) ?: return null
        val work = max(sizePx * 2, 96)
        if (icon is AdaptiveIconDrawable) {
            return icon.foreground?.let { mask(it, work, sizePx, adaptive = true, maxCover = 0.55f, keepColors = true) }
        }
        return mask(icon, work, sizePx, adaptive = false, maxCover = 0.55f, keepColors = true)
    }

    /**
     * Logo détouré : l'icône entière est dessinée en couleur, son fond est la couleur médiane d'un cercle
     * de rayon 0,4 (dans la forme, quelle qu'elle soit) ; le motif est ce qui s'en écarte, plus ou moins
     * opaque selon l'écart. null si l'icône n'a pas de fond uni, ou si rien ne s'en distingue.
     */
    private fun detoured(icon: Drawable, sizePx: Int): Glyph.Mask? {
        val n = max(sizePx * 4, 192)
        val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val d = icon.constantState?.newDrawable(appContext.resources)?.mutate() ?: icon.mutate()
        d.setBounds(0, 0, n, n)
        d.draw(Canvas(bitmap))
        val pixels = IntArray(n * n)
        bitmap.getPixels(pixels, 0, n, 0, 0, n, n)
        bitmap.recycle()
        val alpha = detourAlpha(pixels, n) ?: return null
        var left = n
        var top = n
        var right = -1
        var bottom = -1
        var covered = 0
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (alpha[y * n + x] > 40) {
                    covered++
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < 0 || covered > n * n * 0.5f) return null
        val w = right - left + 1
        val h = bottom - top + 1
        val side = max(w, h)
        val square = IntArray(side * side)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val a = alpha[(top + y) * n + left + x]
                square[(y + (side - h) / 2) * side + x + (side - w) / 2] = (a shl 24) or 0x00FFFFFF
            }
        }
        val crop = Bitmap.createBitmap(square, side, side, Bitmap.Config.ARGB_8888)
        return Glyph.Mask(downscale(crop, sizePx).asImageBitmap())
    }

    /** Logo dessiné par Nimbus (son tracé SVG), blanc sur transparent, recadré et centré dans un carré de [sizePx]. */
    private fun logo(logo: PaperLogo, sizePx: Int): Glyph.Mask {
        val big = sizePx * 2
        val path = PathParser.createPathFromPathData(logo.path)
        val bounds = RectF()
        @Suppress("DEPRECATION")
        path.computeBounds(bounds, true)
        val scale = big / max(bounds.width(), bounds.height())
        val bitmap = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.translate(big / 2f, big / 2f)
        canvas.scale(scale, scale)
        canvas.translate(-bounds.centerX(), -bounds.centerY())
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
        return Glyph.Mask(downscale(bitmap, sizePx).asImageBitmap())
    }

    private fun downscale(bitmap: Bitmap, sizePx: Int): Bitmap {
        val result = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
        if (result != bitmap) bitmap.recycle()
        return result
    }

    /** Teinte dominante (pondérée par la saturation) de l'icône d'origine, foncée si besoin pour le papier. */
    private fun dominantColor(app: AppEntry): Int {
        val icon = icon(app) ?: return INK
        val n = 48
        val bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val d = icon.constantState?.newDrawable(appContext.resources)?.mutate() ?: icon.mutate()
        d.setBounds(0, 0, n, n)
        d.draw(Canvas(bitmap))
        val pixels = IntArray(n * n)
        bitmap.getPixels(pixels, 0, n, 0, 0, n, n)
        bitmap.recycle()
        return dominantColor(pixels)
    }

    companion object {
        /** Gain d'opacité des silhouettes : une partie à 55 % d'opacité devient pleine. */
        private const val ALPHA_GAIN = 1.8f

        /** Encre des pages (PaperColors.Ink). */
        private const val INK = 0xFF14141A.toInt()

        /**
         * Opacité du motif détouré de [pixels] (icône de [n] × [n] px) : écart de couleur au fond (médiane d'un
         * cercle de rayon 0,4 n), de 0 sous 40 à pleine au-delà de 100 ; null sans fond uni et opaque.
         */
        internal fun detourAlpha(pixels: IntArray, n: Int): IntArray? {
            val ring = (0 until 96).mapNotNull { i ->
                val t = i * 2 * Math.PI / 96
                val x = (n / 2f + kotlin.math.cos(t) * 0.4f * n).toInt().coerceIn(0, n - 1)
                val y = (n / 2f + kotlin.math.sin(t) * 0.4f * n).toInt().coerceIn(0, n - 1)
                pixels[y * n + x].takeIf { (it ushr 24) > 200 }
            }
            if (ring.size < 48) return null
            fun median(shift: Int) = ring.map { (it shr shift) and 0xFF }.sorted()[ring.size / 2]
            val br = median(16)
            val bg = median(8)
            val bb = median(0)
            // Fond uni : les trois quarts du cercle près de la médiane (sinon c'est un dessin, pas un fond).
            val near = ring.count { p ->
                kotlin.math.abs(((p shr 16) and 0xFF) - br) + kotlin.math.abs(((p shr 8) and 0xFF) - bg) + kotlin.math.abs((p and 0xFF) - bb) < 60
            }
            if (near < ring.size * 0.75f) return null
            return IntArray(pixels.size) { i ->
                val p = pixels[i]
                if ((p ushr 24) < 128) {
                    0
                } else {
                    val dr = ((p shr 16) and 0xFF) - br
                    val dg = ((p shr 8) and 0xFF) - bg
                    val db = (p and 0xFF) - bb
                    val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
                    (((dist - 40f) / 60f).coerceIn(0f, 1f) * 255).roundToInt()
                }
            }
        }

        /**
         * Teinte dominante de [pixels] (ARGB) : les pixels opaques et colorés sont rangés en 24 teintes,
         * pondérés par saturation × luminosité ; la teinte la plus lourde l'emporte, ramenée à 45 % de
         * clarté au plus pour se lire sur le papier crème. L'encre si moins d'un pixel sur 40 est coloré.
         */
        internal fun dominantColor(pixels: IntArray): Int {
            val bins = 24
            val weight = FloatArray(bins)
            val sums = Array(bins) { FloatArray(3) }
            val hsv = FloatArray(3)
            for (p in pixels) {
                if ((p ushr 24) < 160) continue
                android.graphics.Color.colorToHSV(p, hsv)
                if (hsv[1] < 0.28f || hsv[2] < 0.22f) continue
                val bin = ((hsv[0] / 360f) * bins).toInt().coerceIn(0, bins - 1)
                val w = hsv[1] * hsv[2]
                weight[bin] += w
                sums[bin][0] += w * ((p shr 16) and 0xFF)
                sums[bin][1] += w * ((p shr 8) and 0xFF)
                sums[bin][2] += w * (p and 0xFF)
            }
            val best = weight.indices.maxBy { weight[it] }
            if (weight[best] < pixels.size / 40f * 0.5f) return INK
            val hsl = FloatArray(3)
            ColorUtils.RGBToHSL(
                (sums[best][0] / weight[best]).roundToInt(),
                (sums[best][1] / weight[best]).roundToInt(),
                (sums[best][2] / weight[best]).roundToInt(),
                hsl,
            )
            hsl[2] = hsl[2].coerceAtMost(0.45f)
            return ColorUtils.HSLToColor(hsl)
        }

        @Volatile
        private var instance: GlyphFactory? = null

        fun get(context: Context): GlyphFactory =
            instance ?: synchronized(this) {
                instance ?: GlyphFactory(context).also { instance = it }
            }
    }
}
