package fr.perso.nimbus.ui.unlock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.perso.nimbus.data.BasketShare
import fr.perso.nimbus.data.MeteoShare
import fr.perso.nimbus.ui.theme.PaperShapes
import fr.perso.nimbus.ui.theme.PaperTypography
import fr.perso.nimbus.ui.theme.paperColorScheme
import kotlinx.coroutines.launch

/**
 * Page de déverrouillage, par-dessus l'écran verrouillé (showWhenLocked) : lancée par le service
 * « Gestes » à l'extinction de l'écran, elle est déjà là au réveil. Glisser vers le haut, toucher le bas
 * de la page ou Retour demandent le déverrouillage au système (empreinte, visage ou code) ; elle se
 * ferme une fois le téléphone déverrouillé, ou d'elle-même s'il ne l'était pas au réveil.
 */
class UnlockActivity : ComponentActivity() {

    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }
    private val power by lazy { getSystemService(PowerManager::class.java) }

    /** À faire une fois déverrouillé (ouvrir Météo, Basket Live…). */
    private var pending: (() -> Unit)? = null
    private var dismissing = false

    private val events = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                // Déverrouillé autrement (visage sans glisser, notification…) : on s'efface.
                Intent.ACTION_USER_PRESENT -> unlocked()
                Intent.ACTION_SCREEN_ON -> closeIfUnlocked()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shown = true
        setShowWhenLocked(true)
        // Jamais setTurnScreenOn : l'activité arrive écran éteint et doit le rester.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        onBackPressedDispatcher.addCallback(this) { unlock() }
        ContextCompat.registerReceiver(
            this,
            events,
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val feed = UnlockFeed.get(this)
        setContent {
            val state by feed.state.collectAsStateWithLifecycle()
            // Ce que la page lance (horloge, agenda du haut de la page) attend le déverrouillage.
            val context = remember { LockedContext(this) }
            CompositionLocalProvider(LocalContext provides context) {
                MaterialTheme(colorScheme = paperColorScheme(), typography = PaperTypography, shapes = PaperShapes) {
                    UnlockPage(
                        state,
                        onMeteo = { openApp(MeteoShare.PACKAGE) },
                        onBasket = { openApp(BasketShare.PACKAGE) },
                        onUnlock = { unlock() },
                    )
                }
            }
        }
        // Visible : tout relire, puis suivre les matchs ; caché (écran éteint, autre appli) : rien.
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.RESUMED) { feed.run() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pending = null
    }

    override fun onResume() {
        super.onResume()
        closeIfUnlocked()
    }

    override fun onDestroy() {
        shown = false
        unregisterReceiver(events)
        super.onDestroy()
    }

    /** Écran allumé et téléphone déverrouillé : la page n'a plus rien à faire là. */
    private fun closeIfUnlocked() {
        if (power.isInteractive && !keyguard.isKeyguardLocked && !dismissing) unlocked()
    }

    /** Demande le déverrouillage au système, puis fait [then] et ferme la page. */
    fun unlock(then: (() -> Unit)? = null) {
        pending = then
        if (!keyguard.isKeyguardLocked) {
            unlocked()
            return
        }
        if (dismissing) return
        dismissing = true
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    dismissing = false
                    unlocked()
                }

                override fun onDismissCancelled() {
                    dismissing = false
                    pending = null
                }

                override fun onDismissError() {
                    dismissing = false
                    pending = null
                }
            },
        )
    }

    /** Lance [intent] une fois déverrouillé. */
    fun startAfterUnlock(intent: Intent) = unlock {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // Appli introuvable : la page se ferme simplement.
        }
    }

    private fun openApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) unlock() else startAfterUnlock(intent)
    }

    private fun unlocked() {
        val action = pending
        pending = null
        action?.invoke()
        if (isFinishing) return
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        /** Une page existe (au premier plan ou derrière une alarme, un appel…). */
        @Volatile
        var shown = false
            private set
    }

    /** Contexte de la page : une activité lancée depuis la page (horloge, agenda…) attend le déverrouillage. */
    private class LockedContext(private val activity: UnlockActivity) : ContextWrapper(activity) {
        override fun startActivity(intent: Intent) {
            activity.startAfterUnlock(Intent(intent))
        }

        override fun startActivity(intent: Intent, options: Bundle?) {
            activity.startAfterUnlock(Intent(intent))
        }
    }
}
