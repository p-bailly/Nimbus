package fr.perso.nimbus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperDialog
import fr.perso.nimbus.ui.theme.PaperFootnote
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.PaperTextField

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
    if (LocalPaperUi.current) {
        PaperDialog(
            onDismissRequest = onDismiss,
            title = if (initial == null) "Nouvelle conversation" else "Conversation",
            actions = {
                if (onDelete != null) PaperTextButton("Supprimer", onClick = onDelete, color = PaperColors.Error)
                else PaperTextButton("Annuler", onClick = onDismiss)
                PaperTextButton("Enregistrer", bold = true, enabled = valid, onClick = { onSave(ClaudeSession(name.trim(), url.trim())) })
            },
        ) {
            PaperTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nom",
                placeholder = "Nimbus, travail…",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            PaperTextField(
                value = url,
                onValueChange = { url = it },
                label = "Lien",
                placeholder = "https://claude.ai/code/session_…",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                textStyle = PaperStyle.serif(15f, 21f),
                modifier = Modifier.fillMaxWidth(),
            )
            PaperFootnote(
                mark = "*",
                text = "Le lien de la session, copié depuis l'adresse claude.ai/code/… ou le menu de partage de Claude.",
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
            )
        }
        return
    }
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
