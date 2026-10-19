package fr.perso.nimbus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Service d'accessibilité minimal : il ne lit rien à l'écran, il sert uniquement
 * aux actions globales des gestes (verrouillage, notifications, réglages rapides,
 * applis récentes, capture d'écran, menu Marche/Arrêt).
 */
class GestureService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: GestureService? = null

        val isEnabled: Boolean get() = instance != null

        fun lockScreen(): Boolean =
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) ?: false

        fun openNotifications(): Boolean =
            instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) ?: false

        /** Une constante AccessibilityService.GLOBAL_ACTION_* ; false si le service est désactivé. */
        fun performGlobal(action: Int): Boolean =
            instance?.performGlobalAction(action) ?: false
    }
}
