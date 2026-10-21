package fr.perso.nimbus.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import fr.perso.nimbus.ui.unlock.UnlockLauncher

/**
 * Service d'accessibilité minimal : il ne lit rien à l'écran, il sert uniquement
 * aux actions globales des gestes (verrouillage, notifications, réglages rapides,
 * applis récentes, capture d'écran, menu Marche/Arrêt), et à poser la page de
 * déverrouillage à l'extinction de l'écran (un service lié par le système peut
 * démarrer une activité depuis l'arrière-plan).
 */
class GestureService : AccessibilityService() {

    private var listening = false

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> UnlockLauncher.onScreenOff(this@GestureService)
                Intent.ACTION_SCREEN_ON -> UnlockLauncher.onScreenOn(this@GestureService)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (!listening) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            ContextCompat.registerReceiver(this, screen, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            listening = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        stopListening()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        stopListening()
        super.onDestroy()
    }

    private fun stopListening() {
        if (!listening) return
        listening = false
        UnlockLauncher.cancel()
        try {
            unregisterReceiver(screen)
        } catch (e: IllegalArgumentException) {
            // Déjà désinscrit.
        }
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
