package fr.perso.nimbus.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat

data class FileResult(val id: Long, val name: String, val mimeType: String?, val folder: String?)

/** Recherche de fichiers (par nom) dans le stockage partagé, pour la barre de recherche du tiroir. */
object FilesSearch {

    /** Android 11+ : « Accès à tous les fichiers » ; avant : lecture du stockage. */
    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    /** Écran système qui accorde l'accès à tous les fichiers (Android 11+). */
    fun allFilesAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + context.packageName))

    fun search(context: Context, query: String, limit: Int = 6): List<FileResult> {
        val q = query.trim()
        if (q.isEmpty() || !hasPermission(context)) return emptyList()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            // RELATIVE_PATH n'existe qu'à partir d'Android 10 ; avant, on déduit le dossier du chemin.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Files.FileColumns.RELATIVE_PATH
            else MediaStore.Files.FileColumns.DATA,
        )
        // Les dossiers n'ont pas de type MIME : on ne garde que les fichiers.
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? ESCAPE '\\' AND " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL"
        val escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val sort = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val out = ArrayList<FileResult>()
        try {
            context.contentResolver.query(uri, projection, selection, arrayOf("%$escaped%"), sort)?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    val name = c.getString(1) ?: continue
                    // Fichiers cachés (.nomedia, .thumbnails…) : sans intérêt ici.
                    if (name.startsWith(".")) continue
                    val path = c.getString(3)
                    val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) path?.trimEnd('/')
                    else path?.substringBeforeLast('/')?.substringAfter("/0/")
                    out.add(FileResult(c.getLong(0), name, c.getString(2), folder))
                }
            }
        } catch (e: Exception) {
            // permission retirée ou fournisseur indisponible
        }
        return out
    }

    /** Ouvre le fichier dans l'app associée à son type ; false si aucune ne convient. */
    fun open(context: Context, file: FileResult): Boolean {
        val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), file.id)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, file.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
