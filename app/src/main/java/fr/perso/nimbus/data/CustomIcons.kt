package fr.perso.nimbus.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.util.Base64
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Icônes personnalisées choisies par l'utilisateur (galerie ou import Smart Launcher),
 * stockées en PNG dans files/custom_icons. Le nom du fichier encode la clé de l'app,
 * ce qui permet de retrouver la clé à l'export sans index séparé.
 */
object CustomIcons {

    private const val DIR = "custom_icons"
    private const val MAX_SIZE = 256
    private const val MAX_BYTES = 10 * 1024 * 1024

    private fun dir(context: Context) = File(context.applicationContext.filesDir, DIR)

    private fun fileName(key: String) =
        Base64.encodeToString(key.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) + ".png"

    fun file(context: Context, key: String) = File(dir(context), fileName(key))

    fun has(context: Context, key: String) = file(context, key).isFile

    fun load(context: Context, key: String): Bitmap? {
        val f = file(context, key)
        return if (f.isFile) BitmapFactory.decodeFile(f.path) else null
    }

    /**
     * Enregistre une image (PNG, JPEG, WebP…) comme icône de l'app : recadrée au carré
     * (centre) et réduite à 256 px. Retourne false si l'image est illisible.
     */
    fun save(context: Context, key: String, bytes: ByteArray): Boolean {
        if (bytes.isEmpty() || bytes.size > MAX_BYTES) return false
        return try {
            val src = decodeSmall(bytes) ?: return false
            val side = min(src.width, src.height)
            val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
            val out = if (side > MAX_SIZE) Bitmap.createScaledBitmap(square, MAX_SIZE, MAX_SIZE, true) else square
            val target = file(context, key)
            target.parentFile?.mkdirs()
            target.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }
    }

    /**
     * Décode directement à taille réduite (petit côté ~2 × 256 px) : une photo de 50 Mpx
     * pèserait sinon 200 Mo en mémoire.
     */
    private fun decodeSmall(bytes: ByteArray): Bitmap? = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            val shortSide = min(info.size.width, info.size.height)
            val scale = min(1f, MAX_SIZE * 2f / max(1, shortSide))
            decoder.setTargetSize(
                max(1, (info.size.width * scale).roundToInt()),
                max(1, (info.size.height * scale).roundToInt()),
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: Exception) {
        // Format inconnu d'ImageDecoder : repli avec sous-échantillonnage.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIZE * 2) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun remove(context: Context, key: String) {
        file(context, key).delete()
    }

    /** Toutes les icônes personnalisées : clé d'app -> contenu PNG. */
    fun all(context: Context): Map<String, ByteArray> {
        val files = dir(context).listFiles() ?: return emptyMap()
        return files.mapNotNull { f ->
            val key = try {
                String(Base64.decode(f.name.removeSuffix(".png"), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            } catch (e: Exception) {
                return@mapNotNull null
            }
            key to f.readBytes()
        }.toMap()
    }

    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }
}
