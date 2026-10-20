package fr.perso.nimbus.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import fr.perso.nimbus.data.HomeCell
import fr.perso.nimbus.ui.DragItem
import fr.perso.nimbus.ui.LocalHomeDrag
import fr.perso.nimbus.ui.PageRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fr.perso.nimbus.R
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

/** Zone contre le bord des pages où un élément glissé fait passer à la page voisine. */
private val EDGE_ZONE = 32.dp

/** Temps de maintien contre le bord avant de changer de page. */
private const val EDGE_HOLD_MS = 450L

/** Marge latérale de la grille dans chaque page (et de l'en-tête, du dock). */
private val SIDE_PADDING = 12.dp

/**
 * Accueil : horloge, date et batterie en haut, pages d'icônes et de widgets (balayer à gauche
 * ou à droite), dock (favoris) et barre de recherche en bas. Un élément glissé et tenu contre le
 * bord des pages fait passer à la page voisine ; au bord gauche de la première ou au bord droit
 * de la dernière, une page vide s'ajoute. Les pages vidées disparaissent (sauf la principale).
 */
@Composable
fun HomePage(
    vm: LauncherViewModel,
    settings: LauncherSettings,
    apps: AppsState,
    resizingWidgetId: Int?,
    pageRequest: PageRequest,
    onLongPress: (Offset) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val badges by vm.badges.collectAsStateWithLifecycle()
    val swipeActions by vm.iconSwipeActions.collectAsStateWithLifecycle()
    val doubleTapEnabled = settings.doubleTapAction != GestureAction.NONE
    val multiTouchEnabled = listOf(Gesture.TWO_FINGER_UP, Gesture.TWO_FINGER_DOWN, Gesture.PINCH_IN, Gesture.PINCH_OUT)
        .any { settings.actionFor(it) != GestureAction.NONE }
    val badgeDot = settings.badgeStyle == BadgeStyle.DOT

    val context = LocalContext.current
    val density = LocalDensity.current
    val widgetHost = remember { WidgetHostManager.get(context) }
    val drag = LocalHomeDrag.current

    // Pages : celles qui ont du contenu, plus pendant un glissé une page vide à droite
    // ([minPages]) ou à gauche ([leftPage] : les autres pages se décalent d'un cran à l'écran).
    var minPages by remember { mutableIntStateOf(0) }
    var leftPage by remember { mutableStateOf(false) }
    // Lâcher : la page finale de l'élément (renumérotée si une page s'est ajoutée ou vidée) est
    // affichée une fois l'accueil recalculé ; les pages vides du glissé disparaissent alors.
    var landing by remember { mutableStateOf<Landing?>(null) }
    // La page de gauche fait partie des données dès que l'accueil recalculé arrive.
    val offset = if (leftPage && landing.let { it == null || it.before === apps }) 1 else 0
    val pageOffset by rememberUpdatedState(offset)
    val dataPages by rememberUpdatedState(apps.pageCount)
    val pagerState = rememberPagerState(initialPage = apps.homePage) { maxOf(dataPages, minPages) + pageOffset }
    val iconsByPage = remember(apps.home) { apps.home.groupBy { it.page } }
    val widgetsByPage = remember(apps.homeWidgets) { apps.homeWidgets.groupBy { it.page } }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage - pageOffset }.collect { page ->
            vm.onHomePageShown(page)
            vm.setResizingWidget(null)
        }
    }
    LaunchedEffect(pageRequest) {
        if (pageRequest.id == 0L) return@LaunchedEffect
        // Page qui vient d'être créée : on attend qu'elle existe.
        withTimeoutOrNull(1000) { snapshotFlow { pagerState.pageCount }.first { it > pageRequest.page } }
        pagerState.animateScrollToPage(pageRequest.page + pageOffset)
    }

    SideEffect {
        drag.gridColumns = settings.homeColumns
        drag.gridRows = settings.homeRows
        drag.onDrop = { item, area ->
            // -1 : la page vide ajoutée à gauche.
            val page = pagerState.currentPage - offset
            val cell = area?.let { HomeCell(page, it.x, it.y) }
            val landingPage = when (item) {
                is DragItem.App -> when {
                    item.from == null -> {
                        vm.closeDrawer()
                        if (cell != null) vm.dropOnHome(item.app, cell) else vm.addToHome(item.app, page)
                    }
                    cell != null -> vm.moveHomeIcon(item.app, cell)
                    else -> null
                }
                is DragItem.Widget -> cell?.let { vm.moveHomeWidget(item.widget.id, it) }
            }
            landing = landingPage?.let { Landing(it, apps) }
        }
    }
    val dragging = drag.item != null
    LaunchedEffect(dragging, apps) {
        if (dragging) return@LaunchedEffect
        val target = landing
        if (target == null) {
            minPages = 0
            leftPage = false
            return@LaunchedEffect
        }
        // Accueil pas encore recalculé : on l'attend (l'effet repart dès qu'il arrive), sans
        // attendre indéfiniment si rien n'a changé à l'écran.
        if (apps === target.before) delay(500)
        landing = null
        minPages = 0
        leftPage = false
        snapshotFlow { pageOffset }.first { it == 0 }
        if (pagerState.currentPage != target.page) pagerState.scrollToPage(target.page)
    }

    // Doigt tenu contre un bord pendant un glissé : page voisine, ou nouvelle page vide à
    // gauche de la première ou à droite de la dernière (une de chaque côté au plus).
    val edgePx = with(density) { EDGE_ZONE.toPx() }
    val edge by remember { derivedStateOf { drag.edge(edgePx) } }
    LaunchedEffect(edge) {
        if (edge == 0) return@LaunchedEffect
        while (true) {
            delay(EDGE_HOLD_MS)
            val current = pagerState.currentPage
            var target = current + edge
            if (target < 0) {
                if (leftPage) break
                // Les pages gardent leur clé : celle affichée reste à l'écran, un cran plus à
                // droite, et la page vide apparaît à sa gauche.
                leftPage = true
                withTimeoutOrNull(500) { snapshotFlow { pagerState.currentPage }.first { it == current + 1 } }
                    ?: pagerState.scrollToPage(current + 1)
                target = 0
            } else if (target >= pagerState.pageCount) {
                val dataTarget = target - pageOffset
                if (dataTarget > dataPages) break
                minPages = dataTarget + 1
                snapshotFlow { pagerState.pageCount }.first { it > target }
            }
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pagerState.animateScrollToPage(target)
        }
    }

    val backgroundGestures = Modifier.homeBackground(
        doubleTapEnabled = doubleTapEnabled,
        onTap = { if (resizingWidgetId != null) vm.setResizingWidget(null) },
        onDoubleTap = { vm.onGesture(Gesture.DOUBLE_TAP) },
        onLongPress = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPress(it)
        },
    )

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
        // Les pages ont le leur : le défilement des pages recouvre celui-ci.
        Box(Modifier.fillMaxSize().then(backgroundGestures))

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 16.dp),
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

            HorizontalPager(
                state = pagerState,
                // Toutes les pages restent composées : un élément glissé garde son geste quand
                // sa page sort de l'écran, et les widgets ne se recréent pas à chaque défilement.
                beyondViewportPageCount = maxOf(1, pagerState.pageCount - 1),
                userScrollEnabled = !dragging,
                // Clé = numéro de page des données : une page ajoutée à gauche ne recrée pas les autres.
                key = { it - pageOffset },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val bounds = coords.boundsInRoot()
                        val side = with(density) { SIDE_PADDING.toPx() }
                        drag.pagerBounds = bounds
                        drag.gridBounds = Rect(bounds.left + side, bounds.top, bounds.right - side, bounds.bottom)
                    },
            ) { index ->
                val page = index - offset
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().then(backgroundGestures))
                    HomeGrid(
                        page = page,
                        isTarget = index == pagerState.currentPage,
                        icons = iconsByPage[page].orEmpty(),
                        widgets = widgetsByPage[page].orEmpty(),
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
                        onSwipeUp = { app -> vm.swipeTarget(app)?.let(vm::launch) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = SIDE_PADDING),
                    )
                }
            }

            if (pagerState.pageCount > 1) {
                PageIndicator(
                    count = pagerState.pageCount,
                    current = pagerState.currentPage,
                    main = apps.homePage + offset,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 6.dp),
                )
            }

            Column(Modifier.padding(horizontal = SIDE_PADDING)) {
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
}

/** Page où afficher l'élément qui vient d'être lâché, et l'accueil tel qu'il était avant. */
private class Landing(val page: Int, val before: AppsState)

/**
 * Gestes du fond (zones vides) : tap, double tap, appui long. Sans action de double tap, le
 * double tap n'est pas déclaré : il retarderait l'appui long.
 */
private fun Modifier.homeBackground(
    doubleTapEnabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: (Offset) -> Unit,
): Modifier = composed {
    val tap by rememberUpdatedState(onTap)
    val doubleTap by rememberUpdatedState(onDoubleTap)
    val longPress by rememberUpdatedState(onLongPress)
    pointerInput(doubleTapEnabled) {
        detectTapGestures(
            onTap = { tap() },
            onDoubleTap = if (doubleTapEnabled) { _ -> doubleTap() } else null,
            onLongPress = { longPress(it) },
        )
    }
}

/** Points sous les pages : la page affichée est la plus visible, la page principale est une maison. */
@Composable
private fun PageIndicator(count: Int, current: Int, main: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        repeat(count) { i ->
            val selected = i == current
            val color = Color.White.copy(alpha = if (selected) 0.95f else 0.5f)
            if (i == main) {
                Icon(painterResource(R.drawable.ms_home), contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            } else {
                Box(
                    Modifier
                        .size(if (selected) 8.dp else 6.dp)
                        .background(color, CircleShape),
                )
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
                "Rechercher",
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
