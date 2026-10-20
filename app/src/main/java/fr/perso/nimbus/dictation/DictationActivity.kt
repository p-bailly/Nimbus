package fr.perso.nimbus.dictation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.perso.nimbus.R
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.data.claudeTargetName
import fr.perso.nimbus.service.DictationService
import fr.perso.nimbus.ui.theme.NimbusTheme
import java.util.Locale

/**
 * Écran de dictée, transparent, par-dessus l'appli en cours : on écoute, on montre la
 * transcription (modifiable), puis on l'envoie dans la conversation Claude.
 */
class DictationActivity : ComponentActivity() {

    private var recognizer: SpeechRecognizer? = null

    /** Texte déjà validé ; une nouvelle écoute s'y ajoute. */
    private var text by mutableStateOf("")
    private var partial by mutableStateOf("")
    private var listening by mutableStateOf(false)
    private var message by mutableStateOf<String?>(null)

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else message = "Autorisez le micro pour dicter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Prefs.get(this).settings.value
        setContent {
            NimbusTheme(mode = settings.themeMode, wallpaperColor = null) {
                DictationScreen(
                    text = text,
                    partial = partial,
                    listening = listening,
                    message = message,
                    target = if (settings.dictationCopyOnly) "Le texte sera copié, à coller où vous voulez" else "Vers : " + settings.claudeTargetName,
                    sendLabel = if (settings.dictationCopyOnly) "Copier" else "Envoyer",
                    onTextChange = { text = it },
                    onMic = { if (listening) recognizer?.stopListening() else startListening() },
                    onSend = ::send,
                    onCopy = if (settings.dictationCopyOnly) null else ::copy,
                    onCancel = ::finish,
                )
            }
        }
        if (savedInstanceState == null) startListening()
    }

    override fun onStart() {
        super.onStart()
        DictationService.instance?.setBubbleHidden(true)
    }

    override fun onStop() {
        super.onStop()
        DictationService.instance?.setBubbleHidden(false)
        recognizer?.cancel()
        listening = false
        // Ne reste pas en arrière-plan : on reviendra à une dictée neuve.
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listen()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            message = "Reconnaissance vocale indisponible (installez ou activez l'appli Google)"
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        message = null
        partial = ""
        listening = true
        r.startListening(intent)
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
            listening = false
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            partial = ""
            if (heard.isBlank()) {
                if (text.isBlank()) message = "Je n'ai rien entendu"
                return
            }
            text = listOf(text.trim(), heard.trim()).filter { it.isNotEmpty() }.joinToString(" ")
            if (!Prefs.get(this@DictationActivity).settings.value.dictationConfirm) send()
        }

        override fun onError(error: Int) {
            listening = false
            partial = ""
            message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    if (text.isBlank()) "Je n'ai rien entendu" else null
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Autorisez le micro pour dicter"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Pas de connexion pour la reconnaissance vocale"
                else -> "La reconnaissance vocale a échoué, réessayez"
            }
        }
    }

    /** Bouton principal : envoyer à Claude, ou copier en mode « Copier seulement ». */
    private fun send() {
        val t = text.trim()
        if (t.isEmpty()) return
        recognizer?.cancel()
        if (Prefs.get(this).settings.value.dictationCopyOnly) ClaudeDictation.copyText(applicationContext, t)
        else ClaudeDictation.send(applicationContext, t)
        finish()
    }

    private fun copy() {
        val t = text.trim()
        if (t.isEmpty()) return
        recognizer?.cancel()
        ClaudeDictation.copyText(applicationContext, t)
        finish()
    }
}

@Composable
private fun DictationScreen(
    text: String,
    partial: String,
    listening: Boolean,
    message: String?,
    target: String,
    sendLabel: String,
    onTextChange: (String) -> Unit,
    onMic: () -> Unit,
    onSend: () -> Unit,
    onCopy: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                // Les touches sur la carte ne ferment pas l'écran.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(20.dp),
            ) {
                Text("Dictée pour Claude", style = MaterialTheme.typography.titleLarge)
                Text(target, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(16.dp))
                if (listening) {
                    val shown = listOf(text, partial).filter { it.isNotBlank() }.joinToString(" ")
                    Text(
                        shown.ifBlank { "Je vous écoute…" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (shown.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(vertical = 8.dp),
                    )
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Touchez le micro pour parler") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    )
                }
                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.size(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onCancel) { Text("Annuler") }
                        if (onCopy != null) {
                            IconButton(onClick = onCopy, enabled = text.isNotBlank() && !listening) {
                                Icon(painterResource(R.drawable.ms_content_copy), contentDescription = "Copier le texte")
                            }
                        }
                    }
                    MicButton(listening, onMic)
                    Button(onClick = onSend, enabled = text.isNotBlank() && !listening) { Text(sendLabel) }
                }
            }
        }
    }
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse",
    )
    FilledIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = if (listening) IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
        else IconButtonDefaults.filledIconButtonColors(),
        modifier = Modifier
            .size(64.dp)
            .scale(if (listening) pulse else 1f),
    ) {
        Icon(painterResource(R.drawable.ms_mic), contentDescription = if (listening) "Arrêter l'écoute" else "Parler", modifier = Modifier.size(30.dp))
    }
}
