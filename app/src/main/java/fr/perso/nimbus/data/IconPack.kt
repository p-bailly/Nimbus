package fr.perso.nimbus.data

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.abs

data class IconPackInfo(val packageName: String, val label: String)

/**
 * Pack d'icônes au format ADW / Nova (le format utilisé par la quasi-totalité
 * des packs du Play Store) : res/xml/appfilter.xml ou assets/appfilter.xml.
 */
class IconPack private constructor(
    private val packageName: String,
    private val res: Resources,
    private val map: Map<String, String>,
    private val backNames: List<String>,
    private val maskName: String?,
    private val uponName: String?,
    private val scale: Float,
) {

    /** Icône thématisée pour cette activité, ou null si le pack ne la couvre pas. */
    fun getIcon(cn: ComponentName): Drawable? {
        val name = map["${cn.packageName}/${cn.className}"] ?: return null
        return drawable(name)
    }

    val hasFallback: Boolean get() = backNames.isNotEmpty()

    /**
     * Pour les apps non couvertes : fond du pack + icône d'origine réduite,
     * masque et calque supérieur éventuels (comportement ADW standard).
     */
    fun applyFallback(source: Drawable, seed: String, size: Int): Bitmap? {
        if (backNames.isEmpty()) return null
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val back = drawable(backNames[abs(seed.hashCode() % backNames.size)])
        back?.setBounds(0, 0, size, size)
        back?.draw(canvas)

        val inner = (size * scale).toInt().coerceIn(1, size)
        val offset = (size - inner) / 2
        val layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layer)
        source.setBounds(offset, offset, offset + inner, offset + inner)
        source.draw(layerCanvas)

        maskName?.let { drawable(it) }?.let { mask ->
            val maskBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskBitmap)
            mask.setBounds(0, 0, size, size)
            mask.draw(maskCanvas)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }
            layerCanvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        }
        canvas.drawBitmap(layer, 0f, 0f, null)

        val upon = uponName?.let { drawable(it) }
        upon?.setBounds(0, 0, size, size)
        upon?.draw(canvas)
        return out
    }

    @SuppressLint("DiscouragedApi")
    private fun drawable(name: String): Drawable? {
        val id = res.getIdentifier(name, "drawable", packageName)
        if (id == 0) return null
        return try {
            res.getDrawableForDensity(id, DisplayMetrics.DENSITY_XXXHIGH, null)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private val THEME_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.teslacoilsw.launcher.THEME",
        )

        /** Liste des packs d'icônes installés. */
        @Suppress("DEPRECATION")
        fun installed(context: Context): List<IconPackInfo> {
            val pm = context.packageManager
            val found = LinkedHashMap<String, IconPackInfo>()
            for (action in THEME_ACTIONS) {
                val list = try {
                    pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
                } catch (e: Exception) {
                    emptyList()
                }
                for (ri in list) {
                    val pkg = ri.activityInfo?.packageName ?: continue
                    if (!found.containsKey(pkg)) {
                        found[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString())
                    }
                }
            }
            return found.values.sortedBy { it.label.lowercase() }
        }

        /** Charge et analyse le fichier appfilter d'un pack. Retourne null en cas d'échec. */
        @SuppressLint("DiscouragedApi")
        fun load(context: Context, packageName: String): IconPack? {
            return try {
                val res = context.packageManager.getResourcesForApplication(packageName)
                val parser = openAppFilter(res, packageName) ?: return null
                val map = HashMap<String, String>()
                val backs = ArrayList<String>()
                var mask: String? = null
                var upon: String? = null
                var scale = 0.8f

                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "item" -> {
                                val comp = parser.getAttributeValue(null, "component")
                                val draw = parser.getAttributeValue(null, "drawable")
                                if (comp != null && draw != null) {
                                    parseComponent(comp)?.let { key ->
                                        if (!map.containsKey(key)) map[key] = draw
                                    }
                                }
                            }
                            "iconback" -> {
                                for (i in 0 until parser.attributeCount) {
                                    if (parser.getAttributeName(i).startsWith("img")) {
                                        backs.add(parser.getAttributeValue(i))
                                    }
                                }
                            }
                            "iconmask" -> mask = parser.getAttributeValue(null, "img1")
                            "iconupon" -> upon = parser.getAttributeValue(null, "img1")
                            "scale" -> scale = parser.getAttributeValue(null, "factor")
                                ?.toFloatOrNull() ?: scale
                        }
                    }
                    event = parser.next()
                }
                IconPack(packageName, res, map, backs, mask, upon, scale)
            } catch (e: Exception) {
                null
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun openAppFilter(res: Resources, packageName: String): XmlPullParser? {
            val id = res.getIdentifier("appfilter", "xml", packageName)
            if (id != 0) return res.getXml(id)
            return try {
                val text = res.assets.open("appfilter.xml").bufferedReader().use { it.readText() }
                Xml.newPullParser().apply { setInput(StringReader(text)) }
            } catch (e: Exception) {
                null
            }
        }

        /** "ComponentInfo{com.pkg/com.pkg.Main}" -> "com.pkg/com.pkg.Main" */
        private fun parseComponent(raw: String): String? {
            val inner = raw.trim().removePrefix("ComponentInfo{").removeSuffix("}")
            val slash = inner.indexOf('/')
            if (slash <= 0 || slash == inner.length - 1) return null
            val pkg = inner.substring(0, slash)
            var cls = inner.substring(slash + 1)
            if (cls.startsWith(".")) cls = pkg + cls
            return "$pkg/$cls"
        }
    }
}
