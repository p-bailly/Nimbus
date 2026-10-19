package fr.perso.nimbus.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Nombre de notifications actives par paquet, partagé avec l'interface. */
object BadgeStore {
    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts.asStateFlow()

    fun update(counts: Map<String, Int>) {
        _counts.value = counts
    }
}

/**
 * Écoute les notifications pour afficher des pastilles sur les icônes.
 * À activer dans Paramètres > Notifications > Accès aux notifications.
 */
class BadgeService : NotificationListenerService() {

    override fun onListenerConnected() {
        refresh()
    }

    override fun onListenerDisconnected() {
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
        for (sbn in active) {
            if (sbn.isOngoing) continue
            val n = sbn.notification ?: continue
            if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) continue
            counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
        }
        BadgeStore.update(counts)
    }
}
