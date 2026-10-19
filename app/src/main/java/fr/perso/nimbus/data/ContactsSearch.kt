package fr.perso.nimbus.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactResult(val id: Long, val lookupKey: String, val name: String)

/** Recherche de contacts pour la barre de recherche du tiroir. */
object ContactsSearch {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun search(context: Context, query: String, limit: Int = 5): List<ContactResult> {
        if (query.isBlank() || !hasPermission(context)) return emptyList()
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim()))
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        )
        val out = ArrayList<ContactResult>()
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    val name = c.getString(2) ?: continue
                    out.add(ContactResult(c.getLong(0), c.getString(1) ?: "", name))
                }
            }
        } catch (e: Exception) {
            // permission retirée ou fournisseur indisponible
        }
        return out
    }

    fun open(context: Context, contact: ContactResult) {
        val uri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // aucune app de contacts
        }
    }
}
