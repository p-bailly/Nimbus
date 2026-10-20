package fr.perso.nimbus.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fr.perso.nimbus.R
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.data.claudeTargetName
import fr.perso.nimbus.dictation.ClaudeDictation
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pastille de dictée flottante, affichée par le service d'accessibilité : micro, état, crayon,
 * Copier ou Envoyer, fermer. Elle reste par-dessus les applis et laisse passer les touches autour
 * d'elle, on peut donc naviguer en dictant. Le texte n'apparaît que si on touche le crayon.
 * Le micro reste actif tant qu'on ne l'arrête pas : après chaque silence, l'écoute reprend.
 */
class DictationPanel(private val service: DictationService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private var root: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var status: TextView
    private lateinit var mic: FrameLayout
    private lateinit var action: TextView
    private lateinit var choices: LinearLayout
    private lateinit var editor: LinearLayout
    private lateinit var field: EditText
    private lateinit var restore: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    /** Le micro doit rester actif : l'écoute reprend après chaque phrase ou silence. */
    private var active = false
    private var listening = false
    private var listenStartedAt = 0L
    /** Échecs immédiats d'affilée (micro occupé, refusé…) : au-delà d'un seuil, on arrête. */
    private var quickFailures = 0
    /** Texte dicté ; chaque nouvelle phrase s'y ajoute. */
    private var text = ""
    /** Phrase en cours de reconnaissance (pas encore validée). */
    private var partial = ""
    /** Dernière dictée copiée ou envoyée, pour pouvoir la reprendre et la corriger. */
    private var last = ""
    private var savedPosition: Pair<Int, Int>? = null

    val isShowing: Boolean get() = root != null

    private val copyOnly: Boolean get() = Prefs.get(service).settings.value.dictationCopyOnly

    fun show() {
        if (root != null) return
        val view = build()
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(96)
        }
        try {
            wm.addView(view, params)
            root = view
        } catch (e: Exception) {
            return
        }
        start()
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        active = false
        recognizer?.destroy()
        recognizer = null
        listening = false
        text = ""
        partial = ""
        root?.let { view ->
            try {
                wm.removeView(view)
            } catch (e: Exception) {
                // déjà retirée
            }
        }
        root = null
        savedPosition = null
        DictationMicService.stop(service)
        service.setBubbleHidden(false)
    }

    // ------------------------------------------------------------ Écoute

    /** Active le micro jusqu'à ce qu'on l'arrête. */
    private fun start() {
        if (editor.visibility == View.VISIBLE) closeEditor()
        active = true
        quickFailures = 0
        listen()
    }

    /** Arrête le micro ; la phrase en cours est gardée. */
    private fun stop() {
        active = false
        handler.removeCallbacksAndMessages(null)
        if (listening) recognizer?.stopListening() else setListening(false, idle())
    }

    private fun listen() {
        if (!active) return
        if (!SpeechRecognizer.isRecognitionAvailable(service)) {
            active = false
            status.text = "Reconnaissance indisponible"
            SystemActions.toast(service, "Reconnaissance vocale indisponible : installez ou activez l'appli Google")
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(service).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Silence toléré avant de clore une phrase (souvent ignoré : l'écoute reprend de toute façon).
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10_000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 10_000L)
        partial = ""
        listenStartedAt = SystemClock.uptimeMillis()
        setListening(true, listeningLabel())
        r.startListening(intent)
    }

    /** Relance l'écoute (après une phrase ou un silence) tant que le micro doit rester actif. */
    private fun relisten(delayMs: Long = 150) {
        if (!active) return setListening(false, idle())
        listening = false
        handler.postDelayed({ listen() }, delayMs)
    }

    private fun listeningLabel(): String = if (text.isEmpty()) "Écoute…" else "Écoute… · ${idle()}"

    private fun join(a: String, b: String) = listOf(a.trim(), b.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    private fun words(t: String): Int = t.split(Regex("\\s+")).count { it.isNotEmpty() }

    private fun idle(): String = when (val n = words(text)) {
        0 -> "Touchez le micro"
        1 -> "1 mot"
        else -> "$n mots"
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onPartialResults(partialResults: Bundle?) {
            partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        }

        override fun onResults(results: Bundle?) {
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            text = join(text, heard)
            partial = ""
            quickFailures = 0
            if (heard.isNotBlank() && !Prefs.get(service).settings.value.dictationConfirm) return primaryAction()
            relisten()
        }

        override fun onError(error: Int) {
            partial = ""
            // Arrêt demandé (micro, Copier, crayon) : l'annulation remonte comme une erreur.
            if (!active) return setListening(false, idle())
            val quick = SystemClock.uptimeMillis() - listenStartedAt < 1_000
            quickFailures = if (quick) quickFailures + 1 else 0
            when (error) {
                // Silence : on réécoute, indéfiniment.
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    if (quickFailures < MAX_QUICK_FAILURES) return relisten()
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    if (quickFailures < MAX_QUICK_FAILURES) return relisten(500)
            }
            active = false
            setListening(
                false,
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Micro indisponible"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Micro non autorisé"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Pas de connexion"
                    // Micro refusé hors premier plan : rouvrir la dictée depuis la bulle ou la tuile.
                    SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_AUDIO -> "Micro indisponible"
                    else -> "Échec, réessayez"
                },
            )
        }
    }

    private fun setListening(on: Boolean, message: String) {
        listening = on
        status.text = message
        // Rouge tant que le micro est actif, même entre deux phrases.
        (mic.background as GradientDrawable).setColor(if (on || active) LISTENING_COLOR else ACCENT)
        mic.contentDescription = if (on || active) "Arrêter le micro" else "Parler"
    }

    /** Texte complet, phrase en cours comprise. */
    private fun currentText(): String =
        if (editor.visibility == View.VISIBLE) field.text.toString().trim() else join(text, partial)

    /** Arrête l'écoute en gardant tout ce qui a été dit. */
    private fun halt() {
        text = currentText()
        partial = ""
        active = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        listening = false
    }

    /** Bouton principal : envoyer à Claude, ou copier en mode « Copier seulement ». */
    private fun primaryAction() {
        halt()
        val t = text.trim()
        if (t.isEmpty()) return setListening(false, idle())
        last = t
        if (copyOnly) {
            close()
            ClaudeDictation.copyText(service, t)
        } else {
            // L'appli où revenir : celle qui est ouverte au moment d'envoyer.
            service.captureOrigin()
            close()
            ClaudeDictation.send(service, t)
        }
    }

    // ------------------------------------------------------------ Vue

    @SuppressLint("ClickableViewAccessibility")
    private fun build(): View {
        val column = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Destination Claude, déroulée en touchant l'état (masquée en « Copier seulement »).
        choices = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = rounded(CARD_COLOR, 18)
            setPadding(0, dp(4), 0, dp(4))
        }
        column.addView(
            choices,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            },
        )

        val pill = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(CARD_COLOR, 26)
            elevation = dp(6).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        mic = FrameLayout(service).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ACCENT)
            }
            addView(
                ImageView(service).apply {
                    setImageResource(R.drawable.ms_mic)
                    setColorFilter(TEXT_COLOR)
                },
                FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER),
            )
            setOnClickListener { if (active) stop() else start() }
        }
        pill.addView(mic, LinearLayout.LayoutParams(dp(40), dp(40)))

        status = TextView(service).apply {
            setTextColor(MUTED_COLOR)
            textSize = 13f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(110)
            setPadding(dp(10), 0, dp(10), 0)
            if (!copyOnly) {
                contentDescription = "Changer de conversation"
                setOnClickListener { toggleChoices() }
            }
        }
        pill.addView(status)

        pill.addView(
            ImageView(service).apply {
                setImageResource(R.drawable.ms_edit)
                setColorFilter(MUTED_COLOR)
                contentDescription = "Voir et corriger le texte"
                setPadding(dp(7), dp(7), dp(7), dp(7))
                setOnClickListener { if (editor.visibility == View.VISIBLE) closeEditor() else openEditor() }
            },
            LinearLayout.LayoutParams(dp(34), dp(34)),
        )
        if (ScreenCapture.supported) {
            pill.addView(
                ImageView(service).apply {
                    setImageResource(R.drawable.ms_photo_camera)
                    setColorFilter(MUTED_COLOR)
                    contentDescription = "Capture d'écran, copiée pour la coller"
                    setPadding(dp(7), dp(7), dp(7), dp(7))
                    setOnClickListener { screenshot() }
                },
                LinearLayout.LayoutParams(dp(34), dp(34)),
            )
        }
        pill.addView(View(service), LinearLayout.LayoutParams(dp(4), 1))

        action = TextView(service).apply {
            text = if (copyOnly) "Copier" else "Envoyer"
            setTextColor(TEXT_COLOR)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = rounded(ACCENT, 18)
            setOnClickListener { primaryAction() }
        }
        pill.addView(action)

        pill.addView(
            ImageView(service).apply {
                setImageResource(R.drawable.ms_close)
                setColorFilter(MUTED_COLOR)
                contentDescription = "Fermer la dictée"
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener { close() }
            },
            LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(4) },
        )

        // Glisser la pastille la déplace (ancrée en bas : monter le doigt augmente le décalage).
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        pill.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                }
                MotionEvent.ACTION_MOVE -> root?.takeIf { savedPosition == null }?.let {
                    if (abs(e.rawX - downX) > dp(4) || abs(e.rawY - downY) > dp(4)) {
                        params.x = startX + (e.rawX - downX).roundToInt()
                        params.y = (startY - (e.rawY - downY)).roundToInt().coerceAtLeast(0)
                        wm.updateViewLayout(it, params)
                    }
                }
            }
            true
        }
        // Texte à corriger, affiché seulement avec le crayon.
        editor = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = rounded(CARD_COLOR, 18)
            setPadding(dp(10), dp(10), dp(10), dp(8))
        }
        field = EditText(service).apply {
            setTextColor(TEXT_COLOR)
            setHintTextColor(MUTED_COLOR)
            hint = "Rien dicté pour l'instant"
            textSize = 15f
            minLines = 2
            maxLines = 6
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            background = rounded(FIELD_COLOR, 12)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        editor.addView(
            field,
            LinearLayout.LayoutParams(service.resources.displayMetrics.widthPixels - dp(48), ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        val editorActions = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        restore = TextView(service).apply {
            text = "↺ Reprendre la dernière dictée"
            setTextColor(ACCENT_LIGHT)
            textSize = 13f
            setPadding(dp(4), dp(10), dp(4), dp(6))
            setOnClickListener {
                field.setText(last)
                field.setSelection(field.text.length)
            }
        }
        editorActions.addView(restore, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        editorActions.addView(
            TextView(service).apply {
                text = "OK"
                setTextColor(TEXT_COLOR)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(16), dp(8), dp(16), dp(8))
                background = rounded(ACCENT, 16)
                setOnClickListener { closeEditor() }
            },
        )
        editor.addView(editorActions)
        column.addView(
            editor,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            },
        )
        column.addView(pill)
        return column
    }

    /** Capture l'écran sans la pastille, l'enregistre et la copie ; la dictée continue. */
    private fun screenshot() {
        if (!ScreenCapture.supported) return
        val view = root ?: return
        view.visibility = View.INVISIBLE
        // Laisse le temps à la pastille de disparaître de l'écran avant la capture.
        handler.postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ScreenCapture.capture(service) { ok ->
                    root?.visibility = View.VISIBLE
                    SystemActions.toast(
                        service,
                        if (ok) "Capture copiée : collez-la dans la conversation (aussi dans Images › Nimbus)"
                        else "Capture impossible : désactivez puis réactivez « Dictée Claude Nimbus »",
                    )
                }
            }
        }, 250)
    }

    /** Affiche le texte pour le corriger : le micro s'arrête et la pastille prend le clavier. */
    private fun openEditor() {
        halt()
        choices.visibility = View.GONE
        field.setText(text)
        field.setSelection(field.text.length)
        restore.visibility = if (last.isNotEmpty()) View.VISIBLE else View.INVISIBLE
        editor.visibility = View.VISIBLE
        setFocusable(true)
        field.requestFocus()
        field.post { service.getSystemService(InputMethodManager::class.java)?.showSoftInput(field, 0) }
        setListening(false, idle())
    }

    private fun closeEditor() {
        text = field.text.toString().trim()
        service.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(field.windowToken, 0)
        field.clearFocus()
        editor.visibility = View.GONE
        setFocusable(false)
        setListening(false, idle())
    }

    /** Sans le crayon, la pastille ne prend pas le clavier : il reste à l'appli dessous. */
    private fun setFocusable(focusable: Boolean) {
        val view = root ?: return
        params.flags = if (focusable) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        // Pendant la correction, la pastille monte en haut de l'écran : le clavier ne la cache pas.
        if (focusable) {
            savedPosition = params.x to params.y
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = 0
            params.y = dp(40)
        } else {
            savedPosition?.let { (x, y) -> params.x = x; params.y = y }
            savedPosition = null
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        wm.updateViewLayout(view, params)
    }

    private fun toggleChoices() {
        if (choices.visibility == View.VISIBLE) {
            choices.visibility = View.GONE
            return
        }
        val prefs = Prefs.get(service)
        val settings = prefs.settings.value
        choices.removeAllViews()
        choices.addView(
            TextView(service).apply {
                text = "Envoyer vers"
                setTextColor(MUTED_COLOR)
                textSize = 12f
                setPadding(dp(14), dp(6), dp(14), dp(2))
            },
        )
        val options = listOf<Pair<String, String?>>("Dernière conversation ouverte" to null) +
            settings.claudeSessions.map { it.name to it.url }
        for ((name, url) in options) {
            val selected = url == settings.claudeSessionUrl
            choices.addView(
                TextView(service).apply {
                    text = if (selected) "✓  $name" else "     $name"
                    setTextColor(if (selected) ACCENT_LIGHT else TEXT_COLOR)
                    textSize = 15f
                    setPadding(dp(14), dp(9), dp(14), dp(9))
                    setOnClickListener {
                        prefs.updateSettings { it.copy(claudeSessionUrl = url) }
                        choices.visibility = View.GONE
                        SystemActions.toast(service, "Vers : ${prefs.settings.value.claudeTargetName}")
                    }
                },
            )
        }
        if (settings.claudeSessions.isEmpty()) {
            choices.addView(
                TextView(service).apply {
                    text = "Ajoutez des conversations dans\nParamètres Nimbus › Dictée pour Claude"
                    setTextColor(MUTED_COLOR)
                    textSize = 12f
                    setPadding(dp(14), dp(4), dp(14), dp(8))
                },
            )
        }
        choices.visibility = View.VISIBLE
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private companion object {
        const val CARD_COLOR = 0xF21E2433.toInt()
        const val TEXT_COLOR = 0xFFFFFFFF.toInt()
        const val MUTED_COLOR = 0xFFB4BCCD.toInt()
        /** Bleu nuit de l'icône Nimbus. */
        const val ACCENT = 0xFF5B7FD6.toInt()
        const val ACCENT_LIGHT = 0xFF9DB4F0.toInt()
        const val LISTENING_COLOR = 0xFFE5534B.toInt()
        const val FIELD_COLOR = 0xFF2B3346.toInt()
        const val MAX_QUICK_FAILURES = 5
    }
}
