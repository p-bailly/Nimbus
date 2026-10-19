package fr.perso.nimbus.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import kotlin.math.min

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
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
        val side = min(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val out = if (side > MAX_SIZE) Bitmap.createScaledBitmap(square, MAX_SIZE, MAX_SIZE, true) else square
        val target = file(context, key)
        target.parentFile?.mkdirs()
        return try {
            target.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            false
        }
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
