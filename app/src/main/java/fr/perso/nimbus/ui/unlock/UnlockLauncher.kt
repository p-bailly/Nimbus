package fr.perso.nimbus.ui.unlock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import fr.perso.nimbus.data.Prefs

/**
 * Pose la page de déverrouillage à l'extinction de l'écran, depuis le service « Gestes » : un service
 * d'accessibilité lié par le système peut démarrer une activité depuis l'arrière-plan. La page n'est
 * posée que si le réglage est actif, si le téléphone a un verrouillage (code, schéma, mot de passe),
 * s'il est bien verrouillé, et si rien ne doit rester devant : appel, sonnerie ou alarme.
 */
object UnlockLauncher {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Essais après l'extinction (ms) : le verrouillage peut suivre l'écran de quelques secondes (« verrouiller
     * après la mise en veille »). Les attentes ne comptent que pendant que le téléphone est éveillé.
     */
    private val TRIES = longArrayOf(0, 300, 1_000, 3_000, 6_000, 16_000, 31_000, 61_000)

    private val BUSY_USAGES = setOf(
        AudioAttributes.USAGE_ALARM,
        AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
        AudioAttributes.USAGE_VOICE_COMMUNICATION,
    )

    fun onScreenOff(context: Context) {
        handler.removeCallbacksAndMessages(null)
        if (!Prefs.get(context).settings.value.unlockPage) return
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return
        if (!keyguard.isDeviceSecure) return
        UnlockFeed.get(context).prefetch()
        attempt(context, 0)
    }

    /** Écran rallumé : trop tard pour poser la page sans qu'on la voie arriver. */
    fun onScreenOn() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun attempt(context: Context, step: Int) {
        val power = context.getSystemService(PowerManager::class.java) ?: return
        if (power.isInteractive || busy(context)) return
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return
        if (keyguard.isKeyguardLocked) {
            launch(context)
            return
        }
        if (step + 1 < TRIES.size) handler.postDelayed({ attempt(context, step + 1) }, TRIES[step + 1] - TRIES[step])
    }

    private fun launch(context: Context) {
        val intent = Intent(context, UnlockActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
        )
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Refusé par le système : l'écran verrouillé habituel reste.
        }
    }

    /**
     * Appel en cours ou qui sonne (téléphone ou appli de visio), alarme ou minuteur qui sonne : la page
     * ne doit pas passer devant leur écran.
     */
    private fun busy(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        if (audio.mode != AudioManager.MODE_NORMAL) return true
        return audio.activePlaybackConfigurations.any { it.audioAttributes.usage in BUSY_USAGES }
    }
}
