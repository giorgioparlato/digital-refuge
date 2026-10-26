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
 * The apps a contact's row offers to message them in, beside calling. [takesText]: whether a message
 * typed in the search arrives already written; for the others it's copied, ready to paste.
 */
enum class ChatApp(val key: String, val label: String, val pkg: String?, val takesText: Boolean) {
    SMS("sms", "message", null, true),
    WHATSAPP("whatsapp", "whatsapp", "com.whatsapp", true),
    SIGNAL("signal", "signal", "org.thoughtcrime.securesms", false),
    TELEGRAM("telegram", "telegram", "org.telegram.messenger", false),
    ;

    companion object {
        const val MAX = 3
        val DEFAULT = listOf(SMS.key, WHATSAPP.key)

        /** Those of [keys] that are on the phone, in that order, at most [MAX]. */
        fun chosen(keys: List<String>, available: List<ChatApp>): List<ChatApp> =
            keys.mapNotNull { k -> available.firstOrNull { it.key == k } }.take(MAX)
    }
}

/**
 * The phone's contacts, read only to match what you type in the search, and only once you've
 * switched contacts on there. Nothing is kept or sent.
 */
object ContactSearch {
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

    /** Everyone in your contacts, by name, A–Z. */
    fun names(context: Context): List<String> {
        if (!canRead(context)) return emptyList()
        return runCatching {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
            )?.use { c -> buildList { while (c.moveToNext()) c.getString(0)?.takeIf { it.isNotBlank() }?.let(::add) } }.orEmpty()
        }.getOrDefault(emptyList()).distinct()
    }

    /** The messaging apps on the phone; texting is always there. */
    fun chatApps(context: Context): List<ChatApp> = ChatApp.entries.filter { app ->
        app.pkg == null || runCatching { context.packageManager.getApplicationInfo(app.pkg, 0) }.isSuccess
    }

    /**
     * Words that start with someone's name, split into the people they could mean and the message after
     * it: "anna maria running late" is Anna Maria and "running late". A whole name beats a first name
     * alone; when only a first name fits, everyone with it is offered. Nobody fits: no one, no message.
     */
    fun split(words: String, names: List<String>): Pair<List<String>, String> {
        val w = words.trim()
        fun fits(prefix: String) = prefix.isNotBlank() && (w.equals(prefix, ignoreCase = true) || w.startsWith("$prefix ", ignoreCase = true))
        val fitting = names.flatMap { name -> listOf(name, name.substringBefore(' ')).distinct().filter(::fits).map { name to it.length } }
        val longest = fitting.maxOfOrNull { it.second } ?: return emptyList<String>() to ""
        return fitting.filter { it.second == longest }.map { it.first }.distinct() to w.substring(longest).trim()
    }

    /** The phone app with their number ready: one more tap calls, and no calling permission is needed. */
    fun call(hit: ContactHit): Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(hit.number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * A chat with them in [app], with [text] already typed where the app allows it. Apps other than
     * texting need their number with its country code; null without it.
     */
    fun chat(app: ChatApp, hit: ContactHit, text: String = ""): Intent? {
        if (app == ChatApp.SMS) {
            return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(hit.number)}"))
                .apply { if (text.isNotBlank()) putExtra("sms_body", text) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val digits = (hit.international ?: hit.number.takeIf { it.startsWith("+") })?.filter { it.isDigit() } ?: return null
        val link = when (app) {
            ChatApp.WHATSAPP -> "https://wa.me/$digits" + if (text.isNotBlank()) "?text=${Uri.encode(text)}" else ""
            ChatApp.SIGNAL -> "https://signal.me/#p/+$digits"
            else -> "https://t.me/+$digits"
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(app.pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
