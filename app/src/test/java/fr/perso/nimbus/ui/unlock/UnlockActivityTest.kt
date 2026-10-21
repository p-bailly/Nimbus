package fr.perso.nimbus.ui.unlock

import android.app.Application
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Looper
import android.os.PowerManager
import fr.perso.nimbus.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Logique de la page de déverrouillage, sur Robolectric : quand le service la pose, comment elle demande
 * le déverrouillage et quand elle se ferme. (L'affichage par-dessus l'écran verrouillé, lui, dépend du
 * système : non vérifiable ici.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UnlockActivityTest {

    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val keyguard get() = shadowOf(app.getSystemService(KeyguardManager::class.java))
    private val power get() = shadowOf(app.getSystemService(PowerManager::class.java))
    private val audio get() = shadowOf(app.getSystemService(AudioManager::class.java))

    @Before
    fun lockedPhone() {
        Prefs.get(app).updateSettings { it.copy(unlockPage = true) }
        keyguard.setIsDeviceSecure(true)
        keyguard.setKeyguardLocked(true)
        power.setIsInteractive(false)
    }

    private fun started(): ComponentName? = shadowOf(app).nextStartedActivity?.component

    @Test
    fun screenOffPutsThePageOverTheLockedPhone() {
        UnlockLauncher.onScreenOff(app)
        assertEquals(ComponentName(app, UnlockActivity::class.java), started())
    }

    @Test
    fun nothingWithoutTheSettingOrALock() {
        Prefs.get(app).updateSettings { it.copy(unlockPage = false) }
        UnlockLauncher.onScreenOff(app)
        assertNull(started())

        Prefs.get(app).updateSettings { it.copy(unlockPage = true) }
        keyguard.setIsDeviceSecure(false)
        UnlockLauncher.onScreenOff(app)
        assertNull(started())
    }

    @Test
    fun callsAndAlarmsStayInFront() {
        app.getSystemService(AudioManager::class.java).mode = AudioManager.MODE_IN_CALL
        UnlockLauncher.onScreenOff(app)
        assertNull(started())

        app.getSystemService(AudioManager::class.java).mode = AudioManager.MODE_NORMAL
        audio.setActivePlaybackConfigurationsFor(listOf(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()))
        UnlockLauncher.onScreenOff(app)
        assertNull(started())
    }

    @Test
    fun waitsForTheLockAfterTheScreenGoesOff() {
        keyguard.setKeyguardLocked(false)
        UnlockLauncher.onScreenOff(app)
        assertNull(started())
        // Verrouillé une seconde plus tard (« verrouiller après la mise en veille ») : la page arrive.
        keyguard.setKeyguardLocked(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_100))
        assertEquals(ComponentName(app, UnlockActivity::class.java), started())
    }

    @Test
    fun screenOnBeforeTheLockPutsThePageOnWake() {
        // Rallumé une seconde après l'extinction, le verrouillage arrivé entre-temps : la page vient au réveil.
        keyguard.setKeyguardLocked(false)
        UnlockLauncher.onScreenOff(app)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900))
        assertNull(started())
        keyguard.setKeyguardLocked(true)
        power.setIsInteractive(true)
        UnlockLauncher.onScreenOn(app)
        assertEquals(ComponentName(app, UnlockActivity::class.java), started())
        // Et une seule fois.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(70))
        assertNull(started())
    }

    @Test
    fun nothingOnWakeIfStillUnlocked() {
        // Rallumé pendant le délai de verrouillage : le téléphone n'est pas verrouillé, rien à poser.
        keyguard.setKeyguardLocked(false)
        UnlockLauncher.onScreenOff(app)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900))
        power.setIsInteractive(true)
        UnlockLauncher.onScreenOn(app)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(70))
        assertNull(started())
    }

    @Test
    fun doublePressForTheCameraCancels() {
        // Double appui sur Marche/Arrêt : l'écran se rallume aussitôt pour l'appareil photo, la page ne vient pas.
        keyguard.setKeyguardLocked(false)
        UnlockLauncher.onScreenOff(app)
        keyguard.setKeyguardLocked(true)
        power.setIsInteractive(true)
        UnlockLauncher.onScreenOn(app)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(70))
        assertNull(started())
    }

    @Test
    fun alreadyPutPageIsNotPutTwice() {
        UnlockLauncher.onScreenOff(app)
        assertEquals(ComponentName(app, UnlockActivity::class.java), started())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        power.setIsInteractive(true)
        UnlockLauncher.onScreenOn(app)
        assertNull(started())
    }

    @Test
    fun swipeOrBackAsksTheSystemThenCloses() {
        val activity = Robolectric.buildActivity(UnlockActivity::class.java).setup().get()
        power.setIsInteractive(true)
        assertFalse(activity.isFinishing)
        var opened = false
        activity.unlock { opened = true }
        assertFalse(activity.isFinishing)
        // L'utilisateur renonce sur l'écran du code : la page reste.
        keyguard.setKeyguardLocked(true)
        assertFalse(activity.isFinishing)
        assertFalse(opened)

        activity.onBackPressedDispatcher.onBackPressed()
        keyguard.setKeyguardLocked(false) // empreinte, visage ou code acceptés
        assertTrue(activity.isFinishing)
        assertFalse("l'action abandonnée ne revient pas", opened)
    }

    @Test
    fun opensTheAppOnceUnlocked() {
        val activity = Robolectric.buildActivity(UnlockActivity::class.java).setup().get()
        var opened = false
        activity.unlock { opened = true }
        keyguard.setKeyguardLocked(false)
        assertTrue(opened)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun closesWhenUnlockedElsewhere() {
        val activity = Robolectric.buildActivity(UnlockActivity::class.java).setup().get()
        assertFalse(activity.isFinishing)
        app.sendBroadcast(Intent(Intent.ACTION_USER_PRESENT))
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun closesItselfWhenThePhoneWakesUnlocked() {
        keyguard.setKeyguardLocked(false)
        power.setIsInteractive(true)
        val activity = Robolectric.buildActivity(UnlockActivity::class.java).setup().get()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun staysWhileTheScreenIsOff() {
        // Posée écran éteint, pas encore verrouillée (délai de verrouillage) : elle attend le réveil.
        keyguard.setKeyguardLocked(false)
        val activity = Robolectric.buildActivity(UnlockActivity::class.java).setup().get()
        assertFalse(activity.isFinishing)
        keyguard.setKeyguardLocked(true)
        power.setIsInteractive(true)
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_ON))
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(activity.isFinishing)
    }
}
