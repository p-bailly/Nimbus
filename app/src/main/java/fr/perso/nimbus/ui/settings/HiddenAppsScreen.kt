package fr.perso.nimbus.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.AppPickerDialog

/**
 * Apps masquées : absentes du tiroir et de la recherche, lançables d'ici.
 * On n'arrive sur cet écran qu'après le code PIN (voir les réglages).
 */
@Composable
fun HiddenAppsScreen(vm: LauncherViewModel, apps: AppsState, settings: LauncherSettings) {
    var picking by remember { mutableStateOf(false) }

    BackHandler { vm.closeScreen() }

    SettingsPage("Apps masquées", onBack = vm::closeScreen) {
        item {
            SettingsEntry(R.drawable.ms_visibility_off, SettingsColors.Teal, "Masquer une app", "Elle n'apparaîtra plus dans le tiroir ni dans la recherche") {
                picking = true
            }
        }
        if (apps.hidden.isEmpty()) {
            item {
                Text(
                    "Aucune app masquée.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        } else {
            settingsSection("Masquées")
            items(apps.hidden, key = { it.key }) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.launch(app) }
                        .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Image(app.icon, contentDescription = null, modifier = Modifier.size(settings.iconSize.dp.coerceAtMost(44).dp))
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.label, fontSize = 18.sp)
                        Text(app.category.label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { vm.setHidden(app, false) }) { Text("Afficher") }
                }
            }
        }
    }

    if (picking) {
        AppPickerDialog(
            title = "Masquer une app",
            apps = apps.visible,
            onPick = { app ->
                picking = false
                app?.let { vm.setHidden(it, true) }
            },
            onDismiss = { picking = false },
        )
    }
}
