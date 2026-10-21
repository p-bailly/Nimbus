package fr.perso.nimbus.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperDialog
import fr.perso.nimbus.ui.theme.PaperFootnote
import fr.perso.nimbus.ui.theme.PaperRadio
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton

/** Dialogue de choix unique (boutons radio). Choisir une option ferme le dialogue via [onSelect]. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    enabled: (T) -> Boolean = { true },
    footer: String? = null,
) {
    if (LocalPaperUi.current) {
        PaperDialog(
            onDismissRequest = onDismiss,
            title = title,
            actions = { PaperTextButton("Annuler", onClick = onDismiss) },
        ) {
            PaperChoices(options, selected, label, onSelect, enabled, footer)
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
            ) {
                for (option in options) {
                    val isEnabled = enabled(option)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = option == selected,
                                enabled = isEnabled,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .alpha(if (isEnabled) 1f else 0.4f),
                    ) {
                        RadioButton(selected = option == selected, onClick = null, enabled = isEnabled)
                        Spacer(Modifier.width(12.dp))
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (footer != null) {
                    Text(
                        footer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

/** Options papier : ○ / ● à l'encre, intitulés en romain ; [footer] en note de bas de page. */
@Composable
internal fun <T> PaperChoices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: (T) -> Boolean = { true },
    footer: String? = null,
) {
    Column(
        Modifier
            .selectableGroup()
            .verticalScroll(rememberScrollState()),
    ) {
        for (option in options) {
            val isEnabled = enabled(option)
            val interaction = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .selectable(
                        selected = option == selected,
                        enabled = isEnabled,
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = 6.dp)
                    .alpha(if (isEnabled) 1f else 0.4f),
            ) {
                PaperRadio(selected = option == selected)
                Spacer(Modifier.width(14.dp))
                Text(label(option), style = PaperStyle.item)
            }
        }
        if (footer != null) {
            PaperFootnote(mark = "*", text = footer, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        }
    }
}
