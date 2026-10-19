package fr.perso.nimbus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.ui.drawer.SearchMatcher

/**
 * Choix d'une app dans une liste filtrable. [noneLabel] non null : première ligne
 * « aucune app », qui renvoie null à [onPick].
 */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppEntry>,
    onPick: (AppEntry?) -> Unit,
    onDismiss: () -> Unit,
    noneLabel: String? = null,
) {
    var filter by remember { mutableStateOf("") }
    val sorted = remember(apps) { apps.sortedBy { SearchMatcher.normalize(it.label) } }
    val shown = remember(sorted, filter) {
        if (filter.isBlank()) sorted
        else SearchMatcher.search(SearchMatcher.index(sorted), filter)
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    placeholder = { Text("Rechercher") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    if (noneLabel != null && filter.isBlank()) {
                        item(key = "none") {
                            ListItem(
                                headlineContent = { Text(noneLabel) },
                                leadingContent = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(40.dp)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onPick(null) },
                            )
                        }
                    }
                    items(shown, key = { it.key }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = { Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onPick(app) },
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                }
            }
        }
    }
}
