package fr.perso.nimbus.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import fr.perso.nimbus.R
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.dictation.ClaudeDictation
import fr.perso.nimbus.dictation.DictationActivity
import fr.perso.nimbus.dictation.DictationStarterActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Service d'accessibilité de la dictée Claude :
 * - bulle micro flottante par-dessus les applis ;
 * - écrit la dictée dans le champ de l'appli Claude et appuie sur Envoyer.
 * Il ne lit le contenu de l'écran que dans l'appli Claude, pendant un envoi ; ailleurs il
 * note seulement le nom de l'appli au premier plan, pour pouvoir y revenir.
 */
class DictationService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bubble: View? = null
    private var bubbleHidden = false
    private val panel by lazy { DictationPanel(this) }

    /** Dernière appli au premier plan (hors écran de dictée, Claude, barre système, clavier). */
    private var foreground: String? = null
    private var claudeOnTop = false
    private var origin: String? = null
    private var pending: Pending? = null

    private class Pending(val text: String, val deadline: Long, val returnTo: String?) {
        var textSet = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        scope.launch {
            Prefs.get(this@DictationService).settings
                .map { it.dictationBubble }
                .distinctUntilChanged()
                .collect { show -> if (show) showBubble() else removeBubble() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName && event.className in DICTATION_SCREENS) return
        if (pkg == "com.android.systemui" || pkg in keyboards()) return
        claudeOnTop = pkg == ClaudeDictation.CLAUDE_PACKAGE
        if (!claudeOnTop) foreground = pkg
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        clear()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    private fun clear() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        if (panel.isShowing) panel.close()
        removeBubble()
        scope.cancel()
    }

    private fun keyboards(): Set<String> =
        getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.map { it.packageName }?.toSet().orEmpty()

    // ------------------------------------------------------------ Dictée

    /** Appelé au début d'une dictée : l'appli où revenir après l'envoi. */
    fun captureOrigin() {
        // Dictée lancée depuis Claude lui-même : on y reste.
        origin = if (claudeOnTop) null else foreground
    }

    /** Ouvre le panneau flottant (le service « micro » doit déjà tourner). */
    fun showPanel() {
        setBubbleHidden(true)
        panel.show()
    }

    fun closePanel() {
        if (panel.isShowing) panel.close()
    }

    /** Pendant la dictée, la bulle s'efface. */
    fun setBubbleHidden(hidden: Boolean) {
        bubbleHidden = hidden
        bubble?.visibility = if (hidden) View.GONE else View.VISIBLE
    }

    /** L'appli Claude vient d'être ouverte : on y écrit [text] puis on l'envoie. */
    fun deliver(text: String, returnToOrigin: Boolean) {
        handler.removeCallbacks(inject)
        pending = Pending(text, SystemClock.uptimeMillis() + DELIVERY_TIMEOUT_MS, if (returnToOrigin) origin else null)
        handler.postDelayed(inject, 500)
    }

    private val inject = object : Runnable {
        override fun run() {
            val p = pending ?: return
            if (SystemClock.uptimeMillis() > p.deadline) return fail(p)
            val root = rootInActiveWindow
            if (root == null || root.packageName != ClaudeDictation.CLAUDE_PACKAGE) return retry()
            val field = findInputField(root) ?: return retry()
            if (!p.textSet) {
                field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, p.text)
                }
                if (field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) p.textSet = true
                // Le bouton Envoyer n'apparaît (ou ne s'active) qu'une fois le champ rempli.
                return retry()
            }
            val send = findSendButton(root, field) ?: return retry()
            if (!send.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return retry()
            pending = null
            SystemActions.toast(this@DictationService, "Envoyé à Claude")
            p.returnTo?.let { pkg -> handler.postDelayed({ returnTo(pkg) }, 700) }
        }
    }

    private fun retry() {
        handler.postDelayed(inject, 300)
    }

    private fun fail(p: Pending) {
        pending = null
        ClaudeDictation.copy(this, p.text)
        SystemActions.toast(
            this,
            if (p.textSet) "Texte écrit dans Claude : appuyez sur Envoyer" else "Champ de Claude introuvable : texte copié, collez-le",
        )
    }

    private fun returnTo(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg) ?: return
        try {
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // appli désinstallée entre-temps
        }
    }

    /** Champ de saisie visible le plus bas de l'écran : celui du message. */
    private fun findInputField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        nodes(root).filter { it.isEditable && it.isVisibleToUser && it.isEnabled }.maxByOrNull { bounds(it).bottom }

    /**
     * Bouton Envoyer : décrit « Envoyer » / « Send », à hauteur du champ ou en dessous
     * (pour ne pas cliquer sur un texte de la conversation).
     */
    private fun findSendButton(root: AccessibilityNodeInfo, field: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val top = bounds(field).top - 200
        return nodes(root).filter { node ->
            val label = listOfNotNull(node.contentDescription, node.viewIdResourceName).joinToString(" ")
            SEND_LABEL.containsMatchIn(label) && node.isVisibleToUser && node.isEnabled && bounds(node).top >= top
        }.mapNotNull { clickable(it) }.firstOrNull()
    }

    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = node
        repeat(4) {
            if (n?.isClickable == true) return n
            n = n?.parent
        }
        return null
    }

    private fun nodes(root: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> = sequence {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            yield(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
    }

    private fun bounds(node: AccessibilityNodeInfo): Rect = Rect().also { node.getBoundsInScreen(it) }

    // ------------------------------------------------------------ Bulle

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        if (bubble != null) return
        val wm = getSystemService(WindowManager::class.java) ?: return
        val density = resources.displayMetrics.density
        val size = (BUBBLE_DP * density).roundToInt()
        val positions = getSharedPreferences(BUBBLE_PREFS, Context.MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = positions.getInt("x", resources.displayMetrics.widthPixels - size - (8 * density).roundToInt())
            y = positions.getInt("y", resources.displayMetrics.heightPixels / 3)
        }
        val view = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(BUBBLE_COLOR)
            }
            elevation = 4 * density
            alpha = 0.92f
            contentDescription = "Dictée pour Claude"
            addView(
                ImageView(this@DictationService).apply {
                    setImageResource(R.drawable.ms_mic)
                    setColorFilter(0xFFFFFFFF.toInt())
                },
                FrameLayout.LayoutParams((BUBBLE_ICON_DP * density).roundToInt(), (BUBBLE_ICON_DP * density).roundToInt(), Gravity.CENTER),
            )
        }
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        view.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (dragging || abs(dx) > slop || abs(dy) > slop) {
                        dragging = true
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        wm.updateViewLayout(v, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) positions.edit().putInt("x", params.x).putInt("y", params.y).apply()
                    else ClaudeDictation.start(this)
                }
            }
            true
        }
        view.visibility = if (bubbleHidden) View.GONE else View.VISIBLE
        try {
            wm.addView(view, params)
            bubble = view
        } catch (e: Exception) {
            // fenêtre refusée (service en cours d'arrêt)
        }
    }

    private fun removeBubble() {
        val view = bubble ?: return
        bubble = null
        try {
            getSystemService(WindowManager::class.java)?.removeView(view)
        } catch (e: Exception) {
            // déjà retirée
        }
    }

    companion object {
        @Volatile
        var instance: DictationService? = null

        val isEnabled: Boolean get() = instance != null

        private const val DELIVERY_TIMEOUT_MS = 10_000L
        private const val BUBBLE_DP = 32
        private const val BUBBLE_ICON_DP = 16
        private const val BUBBLE_PREFS = "nimbus_dictation_bubble"
        /** Bleu nuit de l'icône Nimbus. */
        private const val BUBBLE_COLOR = 0xFF5B7FD6.toInt()
        private val DICTATION_SCREENS = setOf(DictationActivity::class.java.name, DictationStarterActivity::class.java.name)
        private val SEND_LABEL = Regex("(?i)\\b(send|envoyer|envoi)")
    }
}
