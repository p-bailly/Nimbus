package fr.perso.nimbus.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
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
import fr.perso.nimbus.data.BadgeStyle
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.LauncherSettings
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.AppsState
import fr.perso.nimbus.ui.LauncherViewModel
import fr.perso.nimbus.ui.components.AppIcon
import fr.perso.nimbus.ui.components.WallpaperTextShadow
import fr.perso.nimbus.widgets.WidgetHostManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Page principale : horloge, date, batterie, grille d'icônes, dock (favoris) et barre de recherche. */
@Composable
fun HomePage(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    apps: AppsState,
    resizingWidgetId: Int?,
    onLongPress: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val badges by vm.badges.collectAsStateWithLifecycle()
    val swipeActions by vm.iconSwipeActions.collectAsStateWithLifecycle()
    val current by rememberUpdatedState(settings)
    val longPress by rememberUpdatedState(onLongPress)
    val doubleTapEnabled = settings.doubleTapAction != GestureAction.NONE
    val multiTouchEnabled = listOf(Gesture.TWO_FINGER_UP, Gesture.TWO_FINGER_DOWN, Gesture.PINCH_IN, Gesture.PINCH_OUT)
        .any { settings.actionFor(it) != GestureAction.NONE }
    val badgeDot = settings.badgeStyle == BadgeStyle.DOT

    val context = LocalContext.current
    val widgetHost = remember { WidgetHostManager.get(context) }
    val resizingWidget by rememberUpdatedState(resizingWidgetId)

    Box(
        Modifier
            .fillMaxSize()
            // Gestes à deux doigts : observés avant tout le monde, puis consommés.
            .then(if (multiTouchEnabled) Modifier.multiTouchGestures { vm.onGesture(it) } else Modifier)
            // Le glissé vertical ne consomme le toucher qu'une fois le seuil franchi :
            // un glissé qui part d'une icône ou d'un widget ouvre quand même le tiroir.
            .pointerInput(Unit) {
                val threshold = 56.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> vm.onGesture(Gesture.SWIPE_UP)
                            total > threshold -> vm.onGesture(Gesture.SWIPE_DOWN)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                    },
                )
            }
    ) {
        // Calque de fond : double tap et appui long ne concernent que les zones vides. Posé
        // sous le contenu, il ne reçoit pas les touchers des icônes ni des widgets (un détecteur
        // de tap posé sur toute la page consommerait l'appui avant le widget, qui ne réagirait plus).
        Box(
            Modifier
                .fillMaxSize()
                // Sans action de double tap, on ne déclare pas le détecteur : il retarderait l'appui long.
                .pointerInput(doubleTapEnabled) {
                    detectTapGestures(
                        onTap = { if (resizingWidget != null) vm.setResizingWidget(null) },
                        onDoubleTap = if (doubleTapEnabled) { _ -> vm.onGesture(Gesture.DOUBLE_TAP) } else null,
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            longPress(it)
                        },
                    )
                },
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
        ) {
            if (settings.showClock || settings.showBattery) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 8.dp),
                ) {
                    if (settings.showClock) ClockAndDate()
                    if (settings.showBattery) {
                        Spacer(Modifier.height(10.dp))
                        BatteryIndicator()
                    }
                }
            }

            HomeGrid(
                icons = apps.home,
                widgets = apps.homeWidgets,
                widgetHost = widgetHost,
                resizingWidget = resizingWidgetId,
                onResizeWidget = vm::setResizingWidget,
                onUpdateWidget = vm::updateHomeWidget,
                onRemoveWidget = vm::removeHomeWidget,
                columns = settings.homeColumns,
                rows = settings.homeRows,
                iconSizeDp = settings.iconSize.dp,
                showLabels = settings.homeShowLabels,
                badges = badges,
                badgeDot = badgeDot,
                hasSwipeAction = { it.key in swipeActions },
                onOpen = vm::launch,
                onActions = vm::showAppPopup,
                onMove = vm::moveHomeIcon,
                onSwipeUp = { app -> vm.swipeTarget(app)?.let(vm::launch) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            if (apps.favorites.isNotEmpty()) {
                Dock(
                    favorites = apps.favorites,
                    columns = settings.dockColumns,
                    iconSizeDp = settings.iconSize.dp,
                    showLabels = settings.showLabels,
                    badges = badges,
                    badgeDot = badgeDot,
                    swipeActions = swipeActions,
                    vm = vm,
                )
            } else if (apps.home.isEmpty() && apps.homeWidgets.isEmpty() && apps.loaded) {
                WallpaperText(
                    "Appui long sur une app du tiroir › Ajouter à l'accueil",
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp),
                )
            }
            if (settings.showSearchBar) {
                IconButton(onClick = { vm.openDrawer() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Toutes les apps", tint = Color.White)
                }
                SearchBar(onClick = { vm.openDrawer(focusSearch = true) })
            }
        }
    }
}

/** Dock : favoris sur une ou plusieurs rangées. */
@Composable
private fun Dock(
    favorites: List<AppEntry>,
    columns: Int,
    iconSizeDp: Int,
    showLabels: Boolean,
    badges: Map<String, Int>,
    badgeDot: Boolean,
    swipeActions: Map<String, String>,
    vm: LauncherViewModel,
) {
    Column(Modifier.padding(top = 8.dp)) {
        for (row in favorites.chunked(columns)) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until columns) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        row.getOrNull(i)?.let { app ->
                            AppIcon(
                                app = app,
                                iconSizeDp = iconSizeDp,
                                showLabel = showLabels,
                                badgeCount = badges[app.packageName] ?: 0,
                                badgeDot = badgeDot,
                                onClick = { vm.launch(app) },
                                onLongClick = { anchor -> vm.showAppPopup(app, anchor) },
                                onWallpaper = true,
                                onSwipeUp = if (app.key in swipeActions) {
                                    { vm.swipeTarget(app)?.let(vm::launch) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gestes à deux doigts (Nova) : glisser vers le haut / le bas, pincer, écarter.
 * Observés dès la passe Initial ; dès que deux doigts sont posés, les événements sont
 * consommés pour que le glissé à un doigt ne réagisse pas.
 */
private fun Modifier.multiTouchGestures(onGesture: (Gesture) -> Unit): Modifier = composed {
    val callback by rememberUpdatedState(onGesture)
    pointerInput(Unit) {
        val distance = 64.dp.toPx()
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var multi = false
            var startCentroid = Offset.Zero
            var startSpan = 0f
            var lastCentroid = Offset.Zero
            var lastSpan = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                if (pressed.size >= 2) {
                    val centroid = pressed.fold(Offset.Zero) { acc, c -> acc + c.position } / pressed.size.toFloat()
                    val span = pressed.map { (it.position - centroid).getDistance() }.average().toFloat()
                    if (!multi) {
                        multi = true
                        startCentroid = centroid
                        startSpan = span
                    }
                    lastCentroid = centroid
                    lastSpan = span
                }
                if (multi) event.changes.forEach { it.consume() }
            }
            if (multi && startSpan > 0f) {
                val zoom = lastSpan / startSpan
                val dy = lastCentroid.y - startCentroid.y
                when {
                    zoom < 0.7f -> callback(Gesture.PINCH_IN)
                    zoom > 1.4f -> callback(Gesture.PINCH_OUT)
                    dy < -distance -> callback(Gesture.TWO_FINGER_UP)
                    dy > distance -> callback(Gesture.TWO_FINGER_DOWN)
                }
            }
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
