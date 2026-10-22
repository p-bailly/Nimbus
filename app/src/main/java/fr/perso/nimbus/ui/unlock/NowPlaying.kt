package fr.perso.nimbus.ui.unlock

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import fr.perso.nimbus.service.BadgeService

// La musique en cours, pour le bandeau « Musique » de la page de déverrouillage : la session de lecture
// active (Spotify, YouTube Music, Deezer…), lue comme le fait l'écran verrouillé du système. Il faut l'accès
// aux notifications de Nimbus (les sessions ne sont données qu'à un service d'écoute des notifications).

/** Ce que joue un lecteur : titre, artiste, pochette, et s'il joue ou s'il est en pause. */
@Immutable
data class Music(
    val packageName: String,
    /** Nom de l'appli (Spotify…). */
    val app: String,
    val title: String,
    val artist: String,
    val art: ImageBitmap? = null,
    val playing: Boolean,
)

/** Les boutons du bandeau, et l'appui sur le reste du bandeau (ouvrir le lecteur). */
enum class MusicAction { PREVIOUS, TOGGLE, NEXT, OPEN }

/** La musique en cours et le lecteur qui la joue (pour ses boutons). */
class NowPlaying(val music: Music?, val controller: MediaController?)

/** Une lecture en pause depuis plus longtemps ne s'affiche plus. */
private const val PAUSE_SHOWN_MS = 30 * 60_000L

/**
 * La musique en cours, suivie tant que la page est composée : le lecteur qui joue (sinon le dernier mis en
 * pause depuis moins de 30 min), ses changements de morceau et de lecture. Sans accès aux notifications ou
 * sans lecteur : rien.
 */
@Composable
internal fun rememberNowPlaying(): NowPlaying {
    val context = LocalContext.current.applicationContext
    var now by remember { mutableStateOf(NowPlaying(null, null)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, BadgeService::class.java)
        val handler = Handler(Looper.getMainLooper())
        var current: MediaController? = null
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                now = NowPlaying(current?.let { music(context, it) }, current)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                now = NowPlaying(current?.let { music(context, it) }, current)
            }
        }

        fun pick(sessions: List<MediaController>?) {
            val best = sessions.orEmpty().filter(::shown).maxByOrNull { if (it.playbackState?.state == PlaybackState.STATE_PLAYING) 1 else 0 }
            if (best?.sessionToken != current?.sessionToken) {
                current?.unregisterCallback(callback)
                current = best
                best?.registerCallback(callback, handler)
            }
            now = NowPlaying(current?.let { music(context, it) }, current)
        }

        val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }
        val registered = try {
            manager?.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
            pick(manager?.getActiveSessions(listener))
            true
        } catch (e: SecurityException) {
            false // Pas d'accès aux notifications : pas de bandeau.
        }
        onDispose {
            if (registered) manager?.removeOnActiveSessionsChangedListener(sessionsChanged)
            current?.unregisterCallback(callback)
        }
    }
    return now
}

/** Joue (ou charge), ou est en pause depuis peu. */
private fun shown(controller: MediaController): Boolean {
    val state = controller.playbackState ?: return false
    return when (state.state) {
        PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING -> true
        PlaybackState.STATE_PAUSED -> SystemClock.elapsedRealtime() - state.lastPositionUpdateTime < PAUSE_SHOWN_MS
        else -> false
    }
}

private fun music(context: Context, controller: MediaController): Music? {
    if (!shown(controller)) return null
    val meta = controller.metadata ?: return null
    val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
    if (title.isNullOrBlank()) return null
    val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
    val art: Bitmap? = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val pkg = controller.packageName
    val app = try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }
    return Music(
        packageName = pkg,
        app = app,
        title = title,
        artist = artist.orEmpty(),
        art = art?.asImageBitmap(),
        playing = controller.playbackState?.state != PlaybackState.STATE_PAUSED,
    )
}

/** Les boutons du bandeau sur [controller] ; [MusicAction.OPEN] est laissé à la page (il faut déverrouiller). */
internal fun MediaController.perform(action: MusicAction, playing: Boolean) {
    val controls = transportControls
    when (action) {
        MusicAction.PREVIOUS -> controls.skipToPrevious()
        MusicAction.TOGGLE -> if (playing) controls.pause() else controls.play()
        MusicAction.NEXT -> controls.skipToNext()
        MusicAction.OPEN -> Unit
    }
}

/** Couleur du bandeau : celle du lecteur quand on la connaît, sinon un violet de musique. */
internal fun musicColor(packageName: String): Long = when (packageName) {
    "com.spotify.music" -> 0xFF1DB954
    "com.google.android.apps.youtube.music", "app.revanced.android.apps.youtube.music" -> 0xFFE62117
    "deezer.android.app" -> 0xFFA238FF
    "com.apple.android.music" -> 0xFFFA243C
    "com.soundcloud.android" -> 0xFFFF5500
    "com.amazon.mp3" -> 0xFF1A98FF
    "com.qobuz.music" -> 0xFF3A3A3A
    "au.com.shiftyjelly.pocketcasts" -> 0xFFF43E37
    "com.radiofrance.radio.radiofrance.android" -> 0xFFE20134
    else -> 0xFF7E57C2
}
