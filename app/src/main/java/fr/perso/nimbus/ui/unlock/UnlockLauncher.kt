package fr.perso.nimbus.ui.unlock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import fr.perso.nimbus.data.Prefs

/**
 * Pose la page de déverrouillage à l'extinction de l'écran, depuis le service « Gestes » : un service
 * d'accessibilité lié par le système peut démarrer une activité depuis l'arrière-plan. La page n'est
 * posée que si le réglage est actif, si le téléphone a un verrouillage (code, schéma, mot de passe),
 * s'il est bien verrouillé, et si rien ne doit rester devant : appel, sonnerie ou alarme. Si l'écran se
 * rallume avant (verrouillage un peu en retard), la page est posée au réveil, par-dessus l'écran verrouillé ;
 * de même à chaque réveil où le téléphone est verrouillé sans page (elle a été fermée par le système, ou
 * n'a pas pu être posée), pour que l'écran verrouillé du système ne se montre pas.
 */
object UnlockLauncher {
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Essais après l'extinction (ms) : le verrouillage peut suivre l'écran de quelques secondes (« verrouiller
     * après la mise en veille »). Les attentes ne comptent que pendant que le téléphone est éveillé.
     */
    private val TRIES = longArrayOf(0, 300, 1_000, 3_000, 6_000, 16_000, 31_000, 61_000)

    /**
     * Écran rallumé moins de [CAMERA_GESTURE] ms après l'extinction : double appui sur Marche/Arrêt,
     * l'appareil photo arrive (300 ms au plus entre les appuis sur Android) ; la page ne passe pas devant.
     */
    private const val CAMERA_GESTURE = 500L

    /** Essais au réveil (ms) : l'état du verrouillage peut suivre l'allumage de quelques instants. */
    private val LATE_TRIES = longArrayOf(0, 250)

    /** Extinction vue, page pas encore posée. */
    private var waiting = false
    private var offAt = 0L

    private val BUSY_USAGES = setOf(
        AudioAttributes.USAGE_ALARM,
        AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
        AudioAttributes.USAGE_VOICE_COMMUNICATION,
    )

    fun onScreenOff(context: Context) {
        cancel()
        if (!Prefs.get(context).settings.value.unlockPage) return
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return
        if (!keyguard.isDeviceSecure) return
        UnlockFeed.get(context).prefetch()
        waiting = true
        offAt = SystemClock.elapsedRealtime()
        attempt(context, 0, TRIES, interactive = false)
    }

    /**
     * Écran rallumé avant que la page soit posée (rallumé vite, le verrouillage n'était pas encore là) :
     * si le téléphone est verrouillé, la page arrive par-dessus l'écran verrouillé.
     */
    fun onScreenOn(context: Context) {
        val camera = SystemClock.elapsedRealtime() - offAt < CAMERA_GESTURE
        val late = waiting && !camera
        cancel()
        // Pas de page alors que le téléphone est verrouillé : posée au réveil (sauf appareil photo).
        val missing = !camera && !UnlockActivity.shown && Prefs.get(context).settings.value.unlockPage &&
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        if (late || missing) attempt(context, 0, LATE_TRIES, interactive = true)
    }

    /** Plus rien à poser (service arrêté, écran rallumé). */
    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        waiting = false
    }

    /** Essai [step] de [tries], écran éteint ou, au réveil, allumé ([interactive]). */
    private fun attempt(context: Context, step: Int, tries: LongArray, interactive: Boolean) {
        val power = context.getSystemService(PowerManager::class.java) ?: return
        if (power.isInteractive != interactive || busy(context)) return
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return
        if (keyguard.isKeyguardLocked) {
            waiting = false
            launch(context)
            return
        }
        if (step + 1 < tries.size) {
            handler.postDelayed({ attempt(context, step + 1, tries, interactive) }, tries[step + 1] - tries[step])
        }
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
