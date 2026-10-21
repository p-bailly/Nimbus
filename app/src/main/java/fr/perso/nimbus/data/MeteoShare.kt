package fr.perso.nimbus.data

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale

// Lecture de ce que l'appli Météo partage avec Nimbus (fournisseur content://io.github.pbailly.meteo.nimbus) :
// le temps qu'il fait au lieu ouvert dans Météo, et le coloriage de son écran Aujourd'hui.

/** Le temps qu'il fait maintenant, comme l'écrit l'écran Aujourd'hui de Météo. */
data class MeteoNow(
    val place: String,
    val region: String?,
    val temperature: Double,
    val feelsLike: Double,
    /** Le ciel en toutes lettres (« Partiellement nuageux »). */
    val sky: String,
    val todayMax: Double,
    val todayMin: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    /** Date de la prévision (ms depuis l'époque). */
    val fetchedAt: Long,
    /** Prévision tirée du cache de Météo, faute de réseau. */
    val stale: Boolean,
)

object MeteoShare {
    const val PACKAGE = "io.github.pbailly.meteo"
    val URI: Uri = Uri.parse("content://io.github.pbailly.meteo.nimbus")
    private val NOW: Uri = URI.buildUpon().appendPath("now").build()

    /** Le dessin est composé par Météo pour 411 × 287 dp, quelle que soit la taille demandée. */
    const val SKETCH_RATIO = 287f / 411f

    /**
     * Le temps qu'il fait, ou null quand Météo n'est pas installée (ou trop ancienne pour partager) ou
     * n'a rien à dire (aucun lieu, ou une température manquante : zéro ligne). Lève une exception si
     * Météo répond mal : l'appelant garde alors ce qu'il avait.
     */
    fun readNow(resolver: ContentResolver): MeteoNow? =
        resolver.query(NOW, null, null, null, null)?.use(::fromCursor)

    /** Première ligne de `now` (colonnes lues par leur nom), null sans ligne. */
    fun fromCursor(cursor: Cursor): MeteoNow? {
        if (!cursor.moveToFirst()) return null
        fun col(name: String) = cursor.getColumnIndexOrThrow(name)
        fun double(name: String): Double? {
            val i = col(name)
            return if (cursor.isNull(i)) null else cursor.getDouble(i).takeUnless { it.isNaN() }
        }
        val place = cursor.getString(col("place"))?.takeIf { it.isNotBlank() } ?: return null
        return MeteoNow(
            place = place,
            region = cursor.getString(col("region"))?.takeIf { it.isNotBlank() },
            temperature = double("temperature") ?: return null,
            feelsLike = double("feels_like") ?: return null,
            sky = cursor.getString(col("sky")).orEmpty(),
            todayMax = double("today_max") ?: return null,
            todayMin = double("today_min") ?: return null,
            weatherCode = cursor.getInt(col("weather_code")),
            isDay = cursor.getInt(col("is_day")) == 1,
            fetchedAt = cursor.getLong(col("fetched_at")),
            stale = cursor.getInt(col("stale")) == 1,
        )
    }

    /**
     * Le coloriage (calque transparent) de [width] × [height] pixels, dessiné autour de [text] (le texte
     * qui sera posé dessus, en fractions de l'image). Lève une exception sans dessin
     * (FileNotFoundException) ou si l'image est illisible.
     */
    fun readSketch(resolver: ContentResolver, width: Int, height: Int, text: List<Rect> = emptyList()): ImageBitmap {
        val uri = URI.buildUpon()
            .appendPath("sketch")
            .appendQueryParameter("width", "$width")
            .appendQueryParameter("height", "$height")
            .apply { if (text.isNotEmpty()) appendQueryParameter("text", textParameter(text)) }
            .build()
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        return checkNotNull(bitmap) { "Dessin illisible" }.asImageBitmap()
    }

    /** Les zones de texte comme les lit Météo : « gauche,haut,droite,bas » séparées par des points-virgules. */
    internal fun textParameter(text: List<Rect>): String =
        text.joinToString(";") { r -> listOf(r.left, r.top, r.right, r.bottom).joinToString(",") { String.format(Locale.ROOT, "%.4f", it) } }
}
