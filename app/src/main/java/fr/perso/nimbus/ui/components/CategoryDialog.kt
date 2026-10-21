package fr.perso.nimbus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.data.Category
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperDialog
import fr.perso.nimbus.ui.theme.PaperFootnote
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.PaperTextField

/**
 * Création ou réglage d'une catégorie du tiroir : nom, place dans les onglets ([onMove] : -1 vers le
 * début, +1 vers la fin ; null quand elle ne peut pas bouger dans ce sens) et suppression ([onDelete],
 * absent pour « Autres » et pour une nouvelle catégorie). [count] : apps qu'elle contient.
 */
@Composable
fun CategoryDialog(
    initial: Category?,
    count: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.label.orEmpty()) }
    val valid = name.isNotBlank()
    val title = if (initial == null) "Nouvelle catégorie" else "Catégorie"
    val note = when {
        initial == null -> "Les apps s'y rangent par appui long › Plus › Catégorie."
        initial.id == Category.OTHER_ID -> "Les apps sans catégorie vont ici : elle ne peut pas être supprimée."
        else -> "$count app${if (count > 1) "s" else ""}. Supprimée, ses apps reviennent au classement automatique."
    }
    val keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
    if (LocalPaperUi.current) {
        PaperDialog(
            onDismissRequest = onDismiss,
            title = title,
            actions = {
                if (onDelete != null) PaperTextButton("Supprimer", onClick = onDelete, color = PaperColors.Error)
                else PaperTextButton("Annuler", onClick = onDismiss)
                PaperTextButton("Enregistrer", bold = true, enabled = valid, onClick = { onSave(name.trim()) })
            },
        ) {
            PaperTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nom",
                placeholder = "Travail, Lycée…",
                keyboardOptions = keyboard,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onMoveUp != null || onMoveDown != null) {
                Row(Modifier.padding(top = 10.dp)) {
                    PaperTextButton("↑ Plus tôt", onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null)
                    PaperTextButton("↓ Plus tard", onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null)
                }
            }
            PaperFootnote(mark = "*", text = note, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nom") },
                    placeholder = { Text("Travail, Lycée…") },
                    keyboardOptions = keyboard,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onMoveUp != null || onMoveDown != null) {
                    Row(Modifier.padding(top = 12.dp)) {
                        OutlinedButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null) { Text("↑ Plus tôt") }
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        OutlinedButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null) { Text("↓ Plus tard") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = valid) { Text("Enregistrer") }
        },
        dismissButton = {
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            else TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
