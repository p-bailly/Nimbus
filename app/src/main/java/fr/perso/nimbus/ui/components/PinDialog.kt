package fr.perso.nimbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperDialogFrame
import fr.perso.nimbus.ui.theme.PaperDialogWindow
import fr.perso.nimbus.ui.theme.PaperFootnote
import fr.perso.nimbus.ui.theme.PaperRule
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.footnoteMark
import fr.perso.nimbus.ui.theme.inkClickable
import androidx.compose.ui.text.buildAnnotatedString

enum class PinMode {
    /** Saisir le code existant. */
    VERIFY,

    /** Choisir un nouveau code, saisi deux fois. */
    CREATE,
}

private const val PIN_MIN = 4
private const val PIN_MAX = 8

/** Erreurs de saisie : message court (thème Material) et note de bas de page (thème papier). */
private enum class PinError(val text: String, val note: String) {
    TOO_SHORT("Le code doit contenir au moins $PIN_MIN chiffres", "Le code doit contenir au moins $PIN_MIN chiffres."),
    WRONG("Code incorrect", "Code incorrect. Vérifie-le et recommence."),
    MISMATCH("Les deux codes ne correspondent pas", "Les deux codes ne correspondent pas. Recommence."),
}

/**
 * Dialogue de code PIN avec pavé numérique.
 * En mode [PinMode.VERIFY], [verify] décide si le code est bon ;
 * [onSuccess] reçoit le code validé (ou le nouveau code en mode création).
 */
@Composable
fun PinDialog(
    title: String,
    mode: PinMode,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    verify: (String) -> Boolean = { true },
) {
    var entered by remember { mutableStateOf("") }
    var firstEntry by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<PinError?>(null) }

    val message = when {
        mode == PinMode.VERIFY -> "Saisis ton code PIN"
        firstEntry == null -> "Choisis un code de $PIN_MIN à $PIN_MAX chiffres"
        else -> "Confirme le code"
    }

    fun submit() {
        if (entered.length < PIN_MIN) {
            error = PinError.TOO_SHORT
            return
        }
        when (mode) {
            PinMode.VERIFY ->
                if (verify(entered)) onSuccess(entered) else {
                    error = PinError.WRONG
                    entered = ""
                }
            PinMode.CREATE -> {
                val first = firstEntry
                when {
                    first == null -> {
                        firstEntry = entered
                        entered = ""
                        error = null
                    }
                    first == entered -> onSuccess(entered)
                    else -> {
                        error = PinError.MISMATCH
                        firstEntry = null
                        entered = ""
                    }
                }
            }
        }
    }

    val onDigit: (Char) -> Unit = { digit ->
        if (entered.length < PIN_MAX) {
            entered += digit
            error = null
        }
    }
    val onDelete = { entered = entered.dropLast(1) }

    if (LocalPaperUi.current) {
        PaperDialogWindow(onDismiss) {
            PaperPinPanel(
                title = title,
                message = message,
                count = entered.length,
                error = error?.note,
                onDigit = onDigit,
                onDelete = onDelete,
                onSubmit = ::submit,
                onDismiss = onDismiss,
            )
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                PinDots(count = entered.length)
                Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                    error?.let {
                        Text(
                            it.text,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Keypad(onDigit = onDigit, onDelete = onDelete, onSubmit = ::submit)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        }
    }
}

@Composable
private fun PinDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(maxOf(PIN_MIN, count)) { i ->
            val filled = i < count
            Box(
                Modifier
                    .size(14.dp)
                    .then(
                        if (filled) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ),
            )
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (digit in row) KeyButton(onClick = { onDigit(digit) }) { Text(digit.toString(), style = MaterialTheme.typography.headlineSmall) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KeyButton(onClick = onDelete) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Effacer")
            }
            KeyButton(onClick = { onDigit('0') }) { Text("0", style = MaterialTheme.typography.headlineSmall) }
            KeyButton(onClick = onSubmit, accent = true) {
                Icon(Icons.Default.Check, contentDescription = "Valider")
            }
        }
    }
}

@Composable
private fun KeyButton(onClick: () -> Unit, accent: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(64.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

// ---------------------------------------------------------------- Thème papier

/**
 * Code PIN sous le thème papier : titre en gras, consigne en italique, code en tirets qui se
 * remplissent de points, pavé 3 × 4 en tableau à filets fins, « ← effacer » et « valider » en
 * petites capitales ; une erreur s'affiche en note de bas de page.
 */
@Composable
internal fun PaperPinPanel(
    title: String,
    message: String,
    count: Int,
    error: String?,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    PaperDialogFrame(title = title, actions = { PaperTextButton("Annuler", onClick = onDismiss) }) {
        Text(message, style = PaperStyle.note.copy(fontSize = PaperStyle.body.fontSize, lineHeight = PaperStyle.body.lineHeight))
        PaperPinSlots(count, error != null, Modifier.padding(top = 18.dp, bottom = 22.dp))
        PaperKeypad(onDigit, onDelete, onSubmit)
        // Place réservée à la note : le pavé ne bouge pas quand elle apparaît.
        Box(Modifier.fillMaxWidth().height(44.dp).padding(top = 14.dp)) {
            if (error != null) {
                PaperFootnote(
                    mark = "1",
                    text = error,
                    markColor = PaperColors.Error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

/** Un tiret par chiffre (au moins quatre), qui reçoit un point à chaque chiffre saisi. */
@Composable
private fun PaperPinSlots(count: Int, error: Boolean, modifier: Modifier = Modifier) {
    val description = "Code : $count " + if (count > 1) "chiffres saisis" else "chiffre saisi"
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description + if (error) ", erreur" else "" },
    ) {
        Spacer(Modifier.weight(1f))
        // « code : », avec l'appel de la note d'erreur en rouge.
        Text(
            buildAnnotatedString {
                append("code")
                if (error) footnoteMark("1", PaperColors.Error)
                append(" :")
            },
            style = PaperStyle.smallCaps(14.5f, spacing = 0.06f),
            modifier = Modifier.padding(end = 14.dp, bottom = 2.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(maxOf(PIN_MIN, count)) { i ->
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.size(22.dp, 30.dp),
                ) {
                    if (i < count) {
                        Box(
                            Modifier
                                .padding(bottom = 7.dp)
                                .size(8.dp)
                                .background(PaperColors.Ink, CircleShape),
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PaperColors.Ink),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Pavé en tableau : filets de 0,8 en haut et en bas, filets de 0,6 entre les cases. */
@Composable
private fun PaperKeypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("123", "456", "789", "←0✓")
    Column(Modifier.fillMaxWidth()) {
        PaperRule(thickness = 0.8.dp)
        rows.forEachIndexed { r, row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                row.forEachIndexed { c, key ->
                    if (c > 0) {
                        Box(
                            Modifier
                                .width(0.6.dp)
                                .fillMaxHeight()
                                .background(PaperColors.Rule),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .inkClickable(
                                onClickLabel = when (key) {
                                    '←' -> "Effacer le dernier chiffre"
                                    '✓' -> "Valider le code"
                                    else -> null
                                },
                                onClick = {
                                    when (key) {
                                        '←' -> onDelete()
                                        '✓' -> onSubmit()
                                        else -> onDigit(key)
                                    }
                                },
                            ),
                    ) {
                        when (key) {
                            '←' -> Text("← effacer", style = PaperStyle.smallCaps(14.5f))
                            '✓' -> Text("valider", style = PaperStyle.smallCaps(14.5f, PaperColors.Link, bold = true))
                            else -> Text(key.toString(), style = PaperStyle.serif(26f, 28f))
                        }
                    }
                }
            }
            PaperRule(thickness = if (r == rows.lastIndex) 0.8.dp else 0.6.dp)
        }
    }
}
