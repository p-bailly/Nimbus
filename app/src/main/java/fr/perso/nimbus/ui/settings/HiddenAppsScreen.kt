package fr.perso.nimbus.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel

/**
 * Apps masquées : absentes du tiroir et de la recherche, lançables d'ici.
 * On n'arrive sur cet écran qu'après le code PIN (voir les réglages).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsScreen(vm: LauncherViewModel, apps: AppsState, settings: LauncherSettings) {
    var picking by remember { mutableStateOf(false) }

    BackHandler { vm.closeScreen() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apps masquées") },
                navigationIcon = {
                    IconButton(onClick = vm::closeScreen) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picking = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Masquer une app") },
            )
        },
    ) { padding ->
        if (apps.hidden.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    "Aucune app masquée.\nLes apps masquées n'apparaissent ni dans le tiroir ni dans la recherche.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 88.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(apps.hidden, key = { it.key }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = { Text(app.category.label) },
                        leadingContent = {
                            Image(app.icon, contentDescription = null, modifier = Modifier.size(settings.iconSize.dp.coerceAtMost(48).dp))
                        },
                        trailingContent = {
                            TextButton(onClick = { vm.setHidden(app, false) }) { Text("Afficher") }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { vm.launch(app) },
                    )
                }
            }
        }
    }

    if (picking) {
        AppPickerDialog(
            apps = apps.visible,
            onPick = { app ->
                picking = false
                vm.setHidden(app, true)
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun AppPickerDialog(apps: List<AppEntry>, onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val sorted = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(
                    "Masquer une app",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .padding(top = 12.dp),
                ) {
                    items(sorted, key = { it.key }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = { Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onPick(app) },
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                }
            }
        }
    }
}
