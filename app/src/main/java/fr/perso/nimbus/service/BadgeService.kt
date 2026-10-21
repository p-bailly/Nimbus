package fr.perso.nimbus.service

import android.app.Notification
import android.app.PendingIntent
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Une notification, pour le bandeau papier. */
data class NotificationItem(
    val key: String,
    val packageName: String,
    val user: UserHandle,
    val title: String,
    val text: String,
    /** Heure de la notification (ms). */
    val time: Long,
    /** Effaçable (ni lecture en cours, ni appel, ni service). */
    val clearable: Boolean,
    val intent: PendingIntent?,
    /** À effacer une fois ouverte. */
    val autoCancel: Boolean,
)

/** Nombre de notifications actives par paquet, et les notifications elles-mêmes, partagés avec l'interface. */
object BadgeStore {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    /** Les plus récentes d'abord, sans les résumés de groupe. */
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun update(counts: Map<String, Int>, notifications: List<NotificationItem> = emptyList()) {
        _counts.value = counts
        _notifications.value = notifications
    }
}

/**
 * Écoute les notifications pour afficher des pastilles sur les icônes et le bandeau papier.
 * À activer dans Paramètres > Notifications > Accès aux notifications.
 */
class BadgeService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        instance = null
        BadgeStore.update(emptyMap())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val active: Array<StatusBarNotification>? = try {
            activeNotifications
        } catch (e: Exception) {
            null
        }
        if (active == null) return
        val counts = HashMap<String, Int>()
        val items = ArrayList<NotificationItem>()
        for (sbn in active) {
            val n = sbn.notification ?: continue
            if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) continue
            if (!sbn.isOngoing) counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
            val extras = n.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = (extras?.getCharSequence(Notification.EXTRA_TEXT) ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT))
                ?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) continue
            items += NotificationItem(
                key = sbn.key,
                packageName = sbn.packageName,
                user = sbn.user,
                title = title.trim(),
                text = text.trim(),
                time = if (n.`when` > 0) n.`when` else sbn.postTime,
                clearable = sbn.isClearable,
                intent = n.contentIntent,
                autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
            )
        }
        BadgeStore.update(counts, items.sortedByDescending { it.time })
    }

    companion object {
        @Volatile
        private var instance: BadgeService? = null

        /** Efface une notification ; false sans accès aux notifications. */
        fun dismiss(key: String): Boolean = instance?.let {
            runCatching { it.cancelNotification(key) }.isSuccess
        } ?: false

        /** Efface toutes les notifications effaçables. */
        fun dismissAll(): Boolean = instance?.let {
            runCatching { it.cancelAllNotifications() }.isSuccess
        } ?: false
    }
}
