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
import android.text.InputType
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
import kotlin.math.roundToInt

/**
 * Panneau de dictée flottant, affiché par le service d'accessibilité : il reste par-dessus les
 * applis et laisse passer les touches autour de lui, on peut donc naviguer en dictant.
 */
class DictationPanel(private val service: DictationService) {

    private val wm = service.getSystemService(WindowManager::class.java)
    private val density = service.resources.displayMetrics.density
    private var root: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var field: EditText
    private lateinit var status: TextView
    private lateinit var mic: FrameLayout
    private lateinit var send: TextView
    private lateinit var target: TextView
    private lateinit var choices: LinearLayout

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    /** Texte validé ; chaque nouvelle écoute s'y ajoute. */
    private var base = ""

    val isShowing: Boolean get() = root != null

    fun show() {
        if (root != null) return
        val view = build()
        params = WindowManager.LayoutParams(
            service.resources.displayMetrics.widthPixels - dp(24),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(96)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
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
        base = ""
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
            status.text = "Reconnaissance vocale indisponible (installez ou activez l'appli Google)"
            return
        }
        setEditing(false)
        base = field.text.toString().trim()
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(service).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        setListening(true, "Je vous écoute…")
        r.startListening(intent)
    }

    private fun join(a: String, b: String) = listOf(a.trim(), b.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val heard = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            field.setText(join(base, heard))
        }

        override fun onResults(results: Bundle?) {
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            base = join(base, heard)
            field.setText(base)
            setListening(false, if (base.isEmpty()) "Je n'ai rien entendu" else IDLE)
            if (heard.isNotBlank() && !Prefs.get(service).settings.value.dictationConfirm) sendNow()
        }

        override fun onError(error: Int) {
            field.setText(base)
            setListening(
                false,
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        if (base.isEmpty()) "Je n'ai rien entendu" else IDLE
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Autorisez le micro pour dicter"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Pas de connexion pour la reconnaissance vocale"
                    // Micro refusé hors premier plan : rouvrir le panneau depuis la bulle ou la tuile.
                    SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_AUDIO -> "Micro indisponible : fermez et rouvrez la dictée"
                    else -> "La reconnaissance vocale a échoué, réessayez"
                },
            )
        }
    }

    private fun setListening(on: Boolean, message: String) {
        listening = on
        status.text = message
        (mic.background as GradientDrawable).setColor(if (on) LISTENING_COLOR else ACCENT)
        mic.contentDescription = if (on) "Arrêter l'écoute" else "Parler"
        send.isEnabled = !on
        send.alpha = if (on) 0.5f else 1f
    }

    private fun sendNow() {
        val text = field.text.toString().trim()
        if (text.isEmpty()) return
        recognizer?.cancel()
        // L'appli où revenir : celle qui est ouverte au moment d'envoyer.
        service.captureOrigin()
        close()
        ClaudeDictation.send(service, text)
    }

    /** Le champ ne prend le clavier que si on le touche : sinon les touches vont à l'appli dessous. */
    private fun setEditing(editing: Boolean) {
        val view = root ?: return
        val flags = if (editing) FOCUSABLE else NOT_FOCUSABLE
        if (params.flags != flags) {
            params.flags = flags
            wm.updateViewLayout(view, params)
        }
        val imm = service.getSystemService(InputMethodManager::class.java)
        if (editing) {
            field.requestFocus()
            field.post { imm?.showSoftInput(field, 0) }
        } else {
            imm?.hideSoftInputFromWindow(field.windowToken, 0)
            field.clearFocus()
        }
    }

    // ------------------------------------------------------------ Vue

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun build(): View {
        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(CARD_COLOR)
            }
            elevation = dp(8).toFloat()
        }

        // En-tête : glisser pour déplacer le panneau, croix pour fermer.
        val header = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            TextView(service).apply {
                text = "Dictée pour Claude"
                setTextColor(TEXT_COLOR)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        header.addView(
            ImageView(service).apply {
                setImageResource(R.drawable.ms_close)
                setColorFilter(MUTED_COLOR)
                contentDescription = "Fermer la dictée"
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener { close() }
            },
            LinearLayout.LayoutParams(dp(36), dp(36)),
        )
        var downY = 0f
        var startY = 0
        header.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; startY = params.y }
                // Ancré en bas : monter le doigt augmente le décalage.
                MotionEvent.ACTION_MOVE -> root?.let {
                    params.y = (startY - (e.rawY - downY)).roundToInt().coerceAtLeast(0)
                    wm.updateViewLayout(it, params)
                }
            }
            true
        }
        card.addView(header)

        // Destination : touchez pour choisir parmi les conversations enregistrées.
        target = TextView(service).apply {
            setTextColor(ACCENT_LIGHT)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(2), 0, dp(6))
            setOnClickListener { toggleChoices() }
        }
        card.addView(target)
        choices = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(FIELD_COLOR)
            }
            setPadding(0, dp(4), 0, dp(4))
        }
        card.addView(
            choices,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            },
        )
        refreshTarget()

        status = TextView(service).apply {
            setTextColor(MUTED_COLOR)
            textSize = 13f
        }
        card.addView(status)

        field = EditText(service).apply {
            setTextColor(TEXT_COLOR)
            setHintTextColor(MUTED_COLOR)
            hint = "Votre message"
            textSize = 16f
            minLines = 2
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(FIELD_COLOR)
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnTouchListener { _, e ->
                if (e.actionMasked == MotionEvent.ACTION_UP && !listening) setEditing(true)
                false
            }
        }
        card.addView(
            field,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            },
        )

        val actions = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        actions.addView(
            TextView(service).apply {
                text = "Effacer"
                setTextColor(MUTED_COLOR)
                textSize = 15f
                setPadding(dp(8), dp(10), dp(8), dp(10))
                setOnClickListener {
                    recognizer?.cancel()
                    base = ""
                    field.setText("")
                    setListening(false, IDLE)
                }
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
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
                FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER),
            )
            setOnClickListener { if (listening) recognizer?.stopListening() else listen() }
        }
        actions.addView(mic, LinearLayout.LayoutParams(dp(56), dp(56)))
        val sendBox = FrameLayout(service)
        send = TextView(service).apply {
            text = "Envoyer"
            setTextColor(TEXT_COLOR)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(18), dp(10), dp(18), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(ACCENT)
            }
            setOnClickListener { sendNow() }
        }
        sendBox.addView(send, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL))
        actions.addView(sendBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(
            actions,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            },
        )
        return card
    }

    private fun refreshTarget() {
        val settings = Prefs.get(service).settings.value
        target.text = "Vers : ${settings.claudeTargetName}  ▾"
        target.contentDescription = "Destination : ${settings.claudeTargetName}. Toucher pour changer"
    }

    private fun toggleChoices() {
        if (choices.visibility == View.VISIBLE) {
            choices.visibility = View.GONE
            return
        }
        val prefs = Prefs.get(service)
        val settings = prefs.settings.value
        choices.removeAllViews()
        val options = listOf<Pair<String, String?>>("Dernière conversation ouverte" to null) +
            settings.claudeSessions.map { it.name to it.url }
        for ((name, url) in options) {
            val selected = url == settings.claudeSessionUrl
            choices.addView(
                TextView(service).apply {
                    text = if (selected) "✓  $name" else "     $name"
                    setTextColor(if (selected) ACCENT_LIGHT else TEXT_COLOR)
                    textSize = 15f
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    setOnClickListener {
                        prefs.updateSettings { it.copy(claudeSessionUrl = url) }
                        choices.visibility = View.GONE
                        refreshTarget()
                    }
                },
            )
        }
        if (settings.claudeSessions.isEmpty()) {
            choices.addView(
                TextView(service).apply {
                    text = "Ajoutez vos conversations dans Paramètres Nimbus › Dictée pour Claude"
                    setTextColor(MUTED_COLOR)
                    textSize = 13f
                    setPadding(dp(14), dp(6), dp(14), dp(10))
                },
            )
        }
        choices.visibility = View.VISIBLE
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private companion object {
        const val IDLE = "Touchez le micro pour continuer, ou changez d'appli"
        const val NOT_FOCUSABLE = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        const val FOCUSABLE = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        const val CARD_COLOR = 0xF21E2433.toInt()
        const val FIELD_COLOR = 0xFF2B3346.toInt()
        const val TEXT_COLOR = 0xFFFFFFFF.toInt()
        const val MUTED_COLOR = 0xFFB4BCCD.toInt()
        /** Bleu nuit de l'icône Nimbus. */
        const val ACCENT = 0xFF5B7FD6.toInt()
        const val ACCENT_LIGHT = 0xFF9DB4F0.toInt()
        const val LISTENING_COLOR = 0xFFE5534B.toInt()
    }
}
