package fr.perso.nimbus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.ClaudeSession

/** Ajout ou modification d'une conversation Claude : nom et lien. [onDelete] : bouton Supprimer. */
@Composable
fun ClaudeSessionDialog(
    initial: ClaudeSession?,
    onSave: (ClaudeSession) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var url by remember { mutableStateOf(initial?.url.orEmpty()) }
    val valid = name.isNotBlank() && url.trim().startsWith("http")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle conversation" else "Conversation") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nom") },
                    placeholder = { Text("Nimbus, travail…") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text("Lien") },
                    placeholder = { Text("https://claude.ai/code/session_…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Le lien de la session, copié depuis l'adresse claude.ai/code/… ou le menu de partage de Claude.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ClaudeSession(name.trim(), url.trim())) }, enabled = valid) { Text("Enregistrer") }
        },
        dismissButton = {
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            else TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
