package app.bedtime.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** A contact matching the search, with the number to reach them on. */
data class ContactHit(val name: String, val number: String, val international: String?)

/**
 * The phone's contacts, read only to match what you type in the search, and only once you've
 * switched contacts on there. Nothing is kept or sent.
 */
object ContactSearch {
    private const val WHATSAPP = "com.whatsapp"

    fun canRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Up to [limit] contacts whose name matches [query], one number each. */
    fun find(context: Context, query: String, limit: Int = 3): List<ContactHit> {
        if (!canRead(context) || query.isBlank()) return emptyList()
        val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query.trim()))
        val columns = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
        )
        return runCatching {
            context.contentResolver.query(uri, columns, null, null, null)?.use { c ->
                buildList {
                    while (c.moveToNext()) add(ContactHit(c.getString(0).orEmpty(), c.getString(1).orEmpty(), c.getString(2)))
                }
            }.orEmpty()
        }.getOrDefault(emptyList()).filter { it.name.isNotBlank() && it.number.isNotBlank() }.distinctBy { it.name }.take(limit)
    }

    fun hasWhatsApp(context: Context): Boolean =
        runCatching { context.packageManager.getApplicationInfo(WHATSAPP, 0) }.isSuccess

    /** The phone app with their number ready: one more tap calls, and no calling permission is needed. */
    fun call(hit: ContactHit): Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(hit.number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun message(hit: ContactHit): Intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(hit.number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** A WhatsApp chat with them; it needs their number with its country code. */
    fun whatsapp(hit: ContactHit): Intent? {
        val digits = (hit.international ?: hit.number.takeIf { it.startsWith("+") })?.filter { it.isDigit() } ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")).setPackage(WHATSAPP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
