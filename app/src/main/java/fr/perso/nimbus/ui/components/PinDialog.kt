package fr.perso.nimbus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

enum class PinMode {
    /** Saisir le code existant. */
    VERIFY,

    /** Choisir un nouveau code, saisi deux fois. */
    CREATE,
}

private const val PIN_MIN = 4
private const val PIN_MAX = 8

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
    var error by remember { mutableStateOf<String?>(null) }

    val message = when {
        mode == PinMode.VERIFY -> "Saisis ton code PIN"
        firstEntry == null -> "Choisis un code de $PIN_MIN à $PIN_MAX chiffres"
        else -> "Confirme le code"
    }

    fun submit() {
        if (entered.length < PIN_MIN) {
            error = "Le code doit contenir au moins $PIN_MIN chiffres"
            return
        }
        when (mode) {
            PinMode.VERIFY ->
                if (verify(entered)) onSuccess(entered) else {
                    error = "Code incorrect"
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
                        error = "Les deux codes ne correspondent pas"
                        firstEntry = null
                        entered = ""
                    }
                }
            }
        }
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
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Keypad(
                    onDigit = { digit ->
                        if (entered.length < PIN_MAX) {
                            entered += digit
                            error = null
                        }
                    },
                    onDelete = { entered = entered.dropLast(1) },
                    onSubmit = ::submit,
                )
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
