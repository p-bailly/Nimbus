package fr.perso.nimbus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.data.AppEntry

/** Ombre portée des textes posés directement sur le fond d'écran. */
val WallpaperTextShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), blurRadius = 8f)

private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Distance de glissé vers le haut qui déclenche l'action de glissé d'une icône. */
private val SWIPE_DISTANCE = 32.dp

/**
 * Icône d'app avec son nom et une pastille de notifications.
 * [onWallpaper] : texte blanc ombré, pour l'accueil (sinon couleurs du thème).
 * [onSwipeUp] : action de glissé vers le haut sur l'icône (comme Nova), null si aucune.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppEntry,
    iconSizeDp: Int,
    showLabel: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWallpaper: Boolean = false,
    badgeDot: Boolean = false,
    onSwipeUp: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClickLabel = "Ouvrir ${app.label}",
                onLongClickLabel = "Actions pour ${app.label}",
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .then(if (onSwipeUp != null) Modifier.swipeUp(onSwipeUp) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        AppIconContent(app, iconSizeDp, showLabel, badgeCount, badgeDot, onWallpaper)
    }
}

/** Détecte un glissé vers le haut ; les glissés vers le bas restent pour l'écran (geste de page). */
fun Modifier.swipeUp(onSwipeUp: () -> Unit): Modifier = composed {
    val action by rememberUpdatedState(onSwipeUp)
    pointerInput(Unit) {
        val threshold = SWIPE_DISTANCE.toPx()
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = { if (total < -threshold) action() },
            onVerticalDrag = { change, dragAmount ->
                total += dragAmount
                if (total < 0f) change.consume()
            },
        )
    }
}

/** Visuel de l'icône (image, pastille, nom), sans gestion des touchers. */
@Composable
fun AppIconContent(
    app: AppEntry,
    iconSizeDp: Int,
    showLabel: Boolean,
    badgeCount: Int,
    badgeDot: Boolean,
    onWallpaper: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                // App d'un profil pro en pause : grisée.
                colorFilter = if (app.isPaused) Grayscale else null,
                modifier = Modifier
                    .size(iconSizeDp.dp)
                    .alpha(if (app.isPaused) 0.6f else 1f),
            )
            if (badgeCount > 0) {
                if (badgeDot) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                } else {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                    ) {
                        Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                    }
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = if (onWallpaper) {
                    TextStyle(color = Color.White, fontSize = 12.sp, shadow = WallpaperTextShadow)
                } else {
                    TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                },
            )
        }
    }
}
