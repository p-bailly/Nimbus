package fr.perso.nimbus.ui.panel

import android.hardware.camera2.CameraManager
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.service.NotificationItem
import fr.perso.nimbus.service.QuickToggles
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.service.ToggleState
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.AppGlyph
import fr.perso.nimbus.ui.components.rememberAppColor
import fr.perso.nimbus.ui.theme.PaperCheckbox
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperMargin
import fr.perso.nimbus.ui.theme.PaperRule
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.PaperTextButton
import fr.perso.nimbus.ui.theme.inkClickable
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Bandeau papier : ce que le bandeau du système montre (heure, réglages rapides, notifications), composé
// comme une page d'article. Il s'ouvre en glissant vers le bas sur l'accueil (thème papier) ; le bandeau
// du système, lui, ne peut pas être redessiné par une appli : deux liens y mènent, en bas.

/** Une case des réglages : intitulé, état (coché ou non ; null : simple lien) et précision en italique. */
private class Toggle(val label: String, val on: Boolean?, val note: String?, val onClick: () -> Unit)

@Composable
fun PaperPanel(vm: LauncherViewModel, apps: AppsState, hasNotificationAccess: Boolean) {
    val context = LocalContext.current
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    BackHandler { vm.closePanel() }

    // Réglages relus à l'ouverture, puis toutes les deux secondes (retour d'un panneau du système).
    var refresh by remember { mutableStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by produceState(ToggleState(), refresh) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = QuickToggles.read(context)
                delay(2_000)
            }
        }
    }
    val torch = rememberTorch()

    val time = rememberNow()
    val is24h = remember { DateFormat.is24HourFormat(context) }
    val alarm = state.nextAlarm?.let { alarmNote(it, is24h) }
    val toggles = listOf(
        Toggle("Wi-Fi", state.wifi, null) { vm.closePanel(); QuickToggles.wifi(context) },
        Toggle("Données mobiles", null, null) { vm.closePanel(); QuickToggles.mobileData(context) },
        Toggle("Bluetooth", state.bluetooth, null) { vm.closePanel(); QuickToggles.bluetooth(context) },
        Toggle("Mode avion", state.airplane, null) { vm.closePanel(); QuickToggles.airplane(context) },
        Toggle("Portefeuille", null, null) { vm.closePanel(); QuickToggles.wallet(context) },
        Toggle("Lampe torche", torch.value, null) { QuickToggles.setTorch(context, !torch.value) },
        Toggle("Réveil", null, alarm ?: "aucun") { vm.closePanel(); SystemActions.openAlarms(context) },
        Toggle("Rotation auto.", state.rotation, null) {
            if (QuickToggles.toggleRotation(context, state.rotation) != null) refresh++ else vm.closePanel()
        },
    )

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperColors.Paper)
            // Glisser vers le haut : refermer, comme le bandeau du système.
            .pointerInput(Unit) {
                val threshold = 72.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -threshold) vm.closePanel() },
                    onVerticalDrag = { _, amount -> total += amount },
                )
            },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = PaperMargin, end = PaperMargin, top = top + 10.dp, bottom = bottom + 24.dp,
        ),
    ) {
        item(key = "clock") {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    time.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm", Locale.FRANCE)),
                    style = PaperStyle.serif(40f, 44f),
                    modifier = Modifier.inkClickable(onClickLabel = "Ouvrir l'horloge") { vm.closePanel(); SystemActions.openAlarms(context) },
                )
                Spacer(Modifier.width(12.dp))
                val day = if (time.dayOfMonth == 1) "d'er'" else "d"
                Text(
                    time.format(DateTimeFormatter.ofPattern("EEEE $day MMMM", Locale.FRANCE)),
                    style = PaperStyle.smallCaps(14f, spacing = 0.06f),
                    modifier = Modifier
                        .padding(bottom = 7.dp)
                        .inkClickable(onClickLabel = "Ouvrir l'agenda") { vm.closePanel(); SystemActions.openCalendar(context) },
                )
            }
            PaperRule()
        }

        item(key = "toggles") {
            Text("Réglages", style = PaperStyle.section, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Column {
                toggles.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth()) {
                        for (t in pair) ToggleCell(t, Modifier.weight(1f))
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            PaperRule(Modifier.padding(top = 12.dp))
        }

        item(key = "notifications-title") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                Text(
                    buildAnnotatedString {
                        append("Notifications")
                        if (notifications.isNotEmpty()) {
                            withStyle(SpanStyle(fontSize = 0.69.em, baselineShift = androidx.compose.ui.text.style.BaselineShift(0.69f))) {
                                append(" ${notifications.size}")
                            }
                        }
                    },
                    style = PaperStyle.section,
                    modifier = Modifier.weight(1f),
                )
                if (notifications.any { it.clearable }) {
                    PaperTextButton("Tout effacer", onClick = vm::dismissAllNotifications, size = 13.5f)
                }
            }
        }
        when {
            !hasNotificationAccess -> item(key = "no-access") {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("Nimbus n'a pas accès aux notifications.", style = PaperStyle.note)
                    PaperTextButton(
                        "Autoriser l'accès",
                        onClick = { vm.closePanel(); SystemActions.openNotificationAccessSettings(context) },
                        bold = true,
                        horizontalPadding = 0.dp,
                    )
                }
            }
            notifications.isEmpty() -> item(key = "empty") {
                Text("Aucune notification : tout a été lu.", style = PaperStyle.note, modifier = Modifier.padding(vertical = 10.dp))
            }
            else -> items(notifications, key = { it.key }) { item ->
                NotificationRow(item, apps.byKey.values.firstOrNull { it.packageName == item.packageName && it.user == item.user }, is24h, vm)
            }
        }

        item(key = "system") {
            PaperRule(Modifier.padding(top = 14.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                PaperTextButton("Bandeau du système", onClick = { vm.openSystemPanel(quickSettings = false) }, size = 13f)
                Text("·", style = PaperStyle.serif(14f, 14f, PaperColors.Soft), modifier = Modifier.align(Alignment.CenterVertically))
                PaperTextButton("Réglages rapides", onClick = { vm.openSystemPanel(quickSettings = true) }, size = 13f)
            }
        }
    }
}

/** Une case des réglages : case à cocher (ou flèche pour un simple lien), intitulé, précision. */
@Composable
private fun ToggleCell(t: Toggle, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 48.dp)
            .inkClickable(onClickLabel = t.label, onClick = t.onClick)
            .padding(end = 8.dp),
    ) {
        Box(Modifier.width(26.dp), contentAlignment = Alignment.CenterStart) {
            if (t.on != null) PaperCheckbox(t.on) else Text("→", style = PaperStyle.serif(16f, 18f, PaperColors.Link))
        }
        Column {
            Text(t.label, style = PaperStyle.serif(16f, 20f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.note != null) Text(t.note, style = PaperStyle.note, maxLines = 1)
        }
    }
}

/**
 * Une notification : motif de l'app à sa couleur, nom de l'app en petites capitales et heure, titre en
 * gras, texte sur deux lignes. Appui : l'ouvrir ; appui long : l'effacer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(item: NotificationItem, app: AppEntry?, is24h: Boolean, vm: LauncherViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = "Ouvrir",
                onLongClickLabel = if (item.clearable) "Effacer" else null,
                onClick = { vm.openNotification(item) },
                onLongClick = { vm.dismissNotification(item) },
            )
            .padding(vertical = 8.dp),
    ) {
        Box(Modifier.width(32.dp).padding(top = 2.dp)) {
            if (app != null) AppGlyph(app, 20.dp, tint = rememberAppColor(app), colored = true)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    app?.label ?: item.packageName,
                    style = PaperStyle.smallCaps(12.5f, PaperColors.Soft),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(timeNote(item.time, is24h), style = PaperStyle.serif(12.5f, 16f, PaperColors.Soft).copy(textAlign = TextAlign.End))
                if (item.clearable) {
                    // Effacer sans appui long : un petit lien après l'heure.
                    PaperTextButton("effacer", onClick = { vm.dismissNotification(item) }, size = 12f, horizontalPadding = 8.dp, color = PaperColors.Link)
                }
            }
            if (item.title.isNotEmpty()) {
                Text(item.title, style = PaperStyle.serif(15.5f, 20f, bold = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.text.isNotEmpty()) {
                Text(item.text, style = PaperStyle.serif(15f, 20f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    PaperRule(color = PaperColors.Line)
}

/** Heure d'une notification : « 16:32 » aujourd'hui, « hier », sinon la date. */
private fun timeNote(ms: Long, is24h: Boolean): String {
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
    val today = LocalDate.now()
    return when (at.toLocalDate()) {
        today -> at.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.FRANCE))
        today.minusDays(1) -> "hier"
        else -> at.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE))
    }
}

/** Prochaine alarme : « 7:00 », ou « demain 7:00 », ou le jour. */
private fun alarmNote(ms: Long, is24h: Boolean): String {
    val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
    val hour = at.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.FRANCE))
    val today = LocalDate.now()
    return when (at.toLocalDate()) {
        today -> hour
        today.plusDays(1) -> "demain $hour"
        else -> at.format(DateTimeFormatter.ofPattern("EEE", Locale.FRANCE)) + " $hour"
    }
}

/** Heure courante, à la minute. */
@Composable
private fun rememberNow(): LocalDateTime =
    produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000 - System.currentTimeMillis() % 60_000 + 20)
        }
    }.value

/** État de la lampe torche, suivi par le système (elle peut être allumée ailleurs). */
@Composable
private fun rememberTorch(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    val on = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val camera = context.getSystemService(CameraManager::class.java)
        val id = QuickToggles.torchCamera(context)
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == id) on.value = enabled
            }
        }
        runCatching { camera?.registerTorchCallback(callback, null) }
        onDispose { runCatching { camera?.unregisterTorchCallback(callback) } }
    }
    return on
}
