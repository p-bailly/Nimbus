package fr.perso.nimbus.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
 * Pastille de dictée flottante, affichée par le service d'accessibilité : micro, état, Copier ou
 * Envoyer, fermer. Elle reste par-dessus les applis et laisse passer les touches autour d'elle,
 * on peut donc naviguer en dictant. Le texte n'est pas affiché, seulement son nombre de mots.
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

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    /** Texte dicté ; chaque nouvelle écoute s'y ajoute. */
    private var text = ""

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
        listen()
    }

    fun close() {
        recognizer?.destroy()
        recognizer = null
        listening = false
        text = ""
        root?.let { view ->
            try {
                wm.removeView(view)
            } catch (e: Exception) {
                // déjà retirée
            }
        }
        root = null
        DictationMicService.stop(service)
        service.setBubbleHidden(false)
    }

    // ------------------------------------------------------------ Écoute

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(service)) {
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
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        setListening(true, "Écoute…")
        r.startListening(intent)
    }

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
        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onResults(results: Bundle?) {
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            text = join(text, heard)
            setListening(false, if (text.isEmpty()) "Rien entendu" else idle())
            if (heard.isNotBlank() && !Prefs.get(service).settings.value.dictationConfirm) primaryAction()
        }

        override fun onError(error: Int) {
            setListening(
                false,
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        if (text.isEmpty()) "Rien entendu" else idle()
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
        (mic.background as GradientDrawable).setColor(if (on) LISTENING_COLOR else ACCENT)
        mic.contentDescription = if (on) "Arrêter l'écoute" else "Parler"
        val ready = !on && text.isNotEmpty()
        action.isEnabled = ready
        action.alpha = if (ready) 1f else 0.45f
    }

    /** Bouton principal : envoyer à Claude, ou copier en mode « Copier seulement ». */
    private fun primaryAction() {
        val t = text.trim()
        if (t.isEmpty()) return
        recognizer?.cancel()
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
        val column = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }

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
            setOnClickListener { if (listening) recognizer?.stopListening() else listen() }
        }
        pill.addView(mic, LinearLayout.LayoutParams(dp(40), dp(40)))

        status = TextView(service).apply {
            setTextColor(MUTED_COLOR)
            textSize = 13f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(120)
            setPadding(dp(10), 0, dp(10), 0)
            if (!copyOnly) {
                contentDescription = "Changer de conversation"
                setOnClickListener { toggleChoices() }
            }
        }
        pill.addView(status)

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
                MotionEvent.ACTION_MOVE -> root?.let {
                    if (abs(e.rawX - downX) > dp(4) || abs(e.rawY - downY) > dp(4)) {
                        params.x = startX + (e.rawX - downX).roundToInt()
                        params.y = (startY - (e.rawY - downY)).roundToInt().coerceAtLeast(0)
                        wm.updateViewLayout(it, params)
                    }
                }
            }
            true
        }
        column.addView(pill)
        return column
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
    }
}
