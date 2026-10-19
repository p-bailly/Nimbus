package fr.perso.nimbus.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.AppIcon
import fr.perso.nimbus.ui.components.WallpaperTextShadow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DOCK_COLUMNS = 5

/** Page principale : horloge, date, batterie, favoris et barre de recherche. */
@Composable
fun HomePage(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    favorites: List<AppEntry>,
    onLongPress: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val badges by vm.badges.collectAsStateWithLifecycle()
    val current by rememberUpdatedState(settings)
    val longPress by rememberUpdatedState(onLongPress)
    val doubleTapEnabled = settings.doubleTapAction != GestureAction.NONE

    Box(
        Modifier
            .fillMaxSize()
            // Sans action de double tap, on ne déclare pas le détecteur : il retarderait l'appui long.
            .pointerInput(doubleTapEnabled) {
                detectTapGestures(
                    onDoubleTap = if (doubleTapEnabled) { _ -> vm.onGesture(current.doubleTapAction) } else null,
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        longPress(it)
                    },
                )
            }
            .pointerInput(Unit) {
                val threshold = 56.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> vm.onGesture(current.swipeUpAction)
                            total > threshold -> vm.onGesture(current.swipeDownAction)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                )
            }
            .systemBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        ) {
            if (settings.showClock) ClockAndDate()
            if (settings.showBattery) {
                Spacer(Modifier.height(10.dp))
                BatteryIndicator()
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            if (favorites.isEmpty()) {
                WallpaperText(
                    "Appui long sur une app du tiroir › Ajouter aux favoris",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            } else {
                for (row in favorites.chunked(DOCK_COLUMNS)) {
                    Row(Modifier.fillMaxWidth()) {
                        for (i in 0 until DOCK_COLUMNS) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                row.getOrNull(i)?.let { app ->
                                    AppIcon(
                                        app = app,
                                        iconSizeDp = settings.iconSize.dp,
                                        showLabel = settings.showLabels,
                                        badgeCount = badges[app.packageName] ?: 0,
                                        onClick = { vm.launch(app) },
                                        onLongClick = { vm.showAppSheet(app) },
                                        onWallpaper = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            IconButton(onClick = { vm.openDrawer() }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Toutes les apps", tint = Color.White)
            }
            SearchBar(onClick = { vm.openDrawer(focusSearch = true) })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SearchBar(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp)) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                "Rechercher des apps, contacts, sur le web",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WallpaperText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight? = null,
) {
    Text(
        text,
        modifier = modifier,
        style = TextStyle(color = Color.White, fontSize = fontSize, fontWeight = fontWeight, shadow = WallpaperTextShadow),
    )
}

// ---------------------------------------------------------------- Horloge

/** Heure courante, rafraîchie à chaque changement de minute. */
@Composable
private fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        val now = System.currentTimeMillis()
        delay(60_000 - now % 60_000 + 20)
    }
}

@Composable
private fun ClockAndDate() {
    val context = LocalContext.current
    val now by rememberNow()
    val is24h = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val time = remember(now, is24h) {
        now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", locale))
    }
    val date = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    WallpaperText(
        time,
        fontSize = 72.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Ouvrir l'horloge") { SystemActions.openAlarms(context) }
            .padding(horizontal = 8.dp),
    )
    WallpaperText(
        date,
        fontSize = 18.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Ouvrir l'agenda") { SystemActions.openCalendar(context) }
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

// ---------------------------------------------------------------- Batterie

private data class BatteryInfo(val percent: Int, val charging: Boolean)

@Composable
private fun rememberBattery(): State<BatteryInfo?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<BatteryInfo?>(null) }
    DisposableEffect(context) {
        fun parse(intent: Intent): BatteryInfo? {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return null
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            return BatteryInfo(level * 100 / scale, charging)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                state.value = parse(intent)
            }
        }
        // Diffusion « collante » : l'enregistrement renvoie immédiatement le dernier état.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { state.value = parse(it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
private fun BatteryIndicator() {
    val battery by rememberBattery()
    val info = battery ?: return
    val low = info.percent <= 15 && !info.charging
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
            val stroke = 1.5.dp.toPx()
            val tip = 2.5.dp.toPx()
            val body = Size(size.width - tip, size.height)
            drawRoundRect(
                color = Color.White,
                size = body,
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(stroke),
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(body.width, size.height * 0.3f),
                size = Size(tip, size.height * 0.4f),
            )
            val inset = stroke * 1.6f
            drawRoundRect(
                color = if (low) Color(0xFFFF6B6B) else Color.White,
                topLeft = Offset(inset, inset),
                size = Size((body.width - inset * 2) * info.percent / 100f, body.height - inset * 2),
                cornerRadius = CornerRadius(1.5.dp.toPx()),
            )
        }
        Spacer(Modifier.width(8.dp))
        WallpaperText(
            if (info.charging) "${info.percent} % · En charge" else "${info.percent} %",
            fontSize = 14.sp,
        )
    }
}
