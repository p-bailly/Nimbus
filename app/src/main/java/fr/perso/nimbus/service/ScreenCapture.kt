package fr.perso.nimbus.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Display
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Capture d'écran par le service d'accessibilité (Android 11+) : l'image est enregistrée dans
 * Images/Nimbus et copiée dans le presse-papiers, pour la coller dans une conversation.
 */
object ScreenCapture {

    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** [done] reçoit true si l'image est enregistrée et copiée ; appelé sur le thread principal. */
    @RequiresApi(Build.VERSION_CODES.R)
    fun capture(service: AccessibilityService, done: (Boolean) -> Unit) {
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    val bitmap = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        ?.copy(Bitmap.Config.ARGB_8888, false)
                    buffer.close()
                    if (bitmap == null) return done(false)
                    // Compression et écriture hors du thread principal.
                    Thread {
                        val uri = save(service, bitmap)
                        bitmap.recycle()
                        service.mainExecutor.execute {
                            if (uri != null) copy(service, uri)
                            done(uri != null)
                        }
                    }.start()
                }

                override fun onFailure(errorCode: Int) = done(false)
            },
        )
    }

    private fun save(service: AccessibilityService, bitmap: Bitmap): Uri? {
        val resolver = service.contentResolver
        val name = "Nimbus_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date()) + ".png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Nimbus")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("flux indisponible")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun copy(service: AccessibilityService, uri: Uri) {
        val clipboard = service.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newUri(service.contentResolver, "Capture d'écran", uri))
    }
}
