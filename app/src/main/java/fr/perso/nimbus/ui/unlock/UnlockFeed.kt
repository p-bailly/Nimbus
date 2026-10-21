package fr.perso.nimbus.ui.unlock

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.SystemClock
import fr.perso.nimbus.data.BasketShare
import fr.perso.nimbus.data.FavoriteTeam
import fr.perso.nimbus.data.Game
import fr.perso.nimbus.data.MeteoShare
import fr.perso.nimbus.ui.paper.PAPER_WIDTH
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Données de la page de déverrouillage, gardées d'un affichage à l'autre : une lecture qui échoue
 * laisse les dernières valeurs. Tout se lit hors du fil principal.
 */
class UnlockFeed private constructor(context: Context) {
    private val app = context.applicationContext
    private val resolver: ContentResolver = app.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(UnlockState())
    val state: StateFlow<UnlockState> = _state.asStateFlow()

    private val meteoLock = Mutex()
    private val basketLock = Mutex()

    /** Ce qui décide du coloriage (lieu, temps, prévision, taille) et le moment où il a été lu. */
    @Volatile private var sketchKey: List<Any>? = null
    @Volatile private var sketchAt = 0L

    @Volatile private var favorites: List<FavoriteTeam> = emptyList()
    @Volatile private var games: List<Game> = emptyList()
    @Volatile private var scoresAt = 0L

    /**
     * Tant que la page est visible : tout relire tout de suite, suivre Météo et Basket Live (leurs
     * fournisseurs préviennent à chaque changement), puis les scores (direct et résultats de la nuit)
     * toutes les 15 s si un match de favori est en cours, toutes les 5 min sinon.
     */
    suspend fun run(): Unit = coroutineScope {
        launch {
            refreshMeteo()
            changes(MeteoShare.URI).collect { refreshMeteo(forceSketch = true) }
        }
        launch {
            changes(BasketShare.URI).collect {
                refreshFavorites()
                refreshScores()
            }
        }
        refreshFavorites()
        while (true) {
            refreshScores()
            delay(if (_state.value.lines.isNotEmpty()) LIVE_POLL else IDLE_POLL)
        }
    }

    /** À l'extinction de l'écran : la page sera à jour au réveil. */
    fun prefetch() {
        scope.launch { refreshMeteo() }
        scope.launch {
            refreshFavorites()
            refreshScores()
        }
    }

    suspend fun refreshMeteo(forceSketch: Boolean = false) = meteoLock.withLock {
        withContext(Dispatchers.IO) {
            val read = try {
                MeteoShare.readNow(resolver)
            } catch (e: Exception) {
                return@withContext // Météo répond mal : on garde ce qu'on a.
            }
            val now = read?.takeIf { it.fetchedAt <= 0 || System.currentTimeMillis() - it.fetchedAt < MAX_METEO_AGE }
            if (now == null) {
                // Pas de Météo, ou rien à dire : la section disparaît.
                sketchKey = null
                _state.update { it.copy(meteo = null, sketch = null) }
                return@withContext
            }
            _state.update { it.copy(meteo = now) }

            val (w, h) = sketchSize()
            val key = listOf(now.place, now.weatherCode, now.isDay, now.fetchedAt, w, h)
            val fresh = SystemClock.elapsedRealtime() - sketchAt < SKETCH_AGE
            if (!forceSketch && key == sketchKey && fresh && _state.value.sketch != null) return@withContext
            try {
                val sketch = MeteoShare.readSketch(resolver, w, h)
                sketchKey = key
                sketchAt = SystemClock.elapsedRealtime()
                _state.update { it.copy(sketch = sketch) }
            } catch (e: Exception) {
                // Pas de dessin cette fois : le précédent reste.
            }
        }
    }

    suspend fun refreshFavorites() = withContext(Dispatchers.IO) {
        try {
            favorites = BasketShare.readFavorites(resolver).orEmpty()
        } catch (e: Exception) {
            // On garde les favoris connus.
        }
        publishLines()
    }

    suspend fun refreshScores() = basketLock.withLock {
        withContext(Dispatchers.IO) {
            if (favorites.isEmpty()) {
                games = emptyList()
            } else {
                try {
                    games = BasketShare.scoreDays(ZonedDateTime.now()).flatMap { BasketShare.fetchScoreboard(it) }
                    scoresAt = SystemClock.elapsedRealtime()
                } catch (e: Exception) {
                    // Hors réseau : les derniers scores restent un moment, puis ne sont plus « en direct » ;
                    // les résultats, eux, ne changent plus.
                    if (SystemClock.elapsedRealtime() - scoresAt > MAX_SCORES_AGE) games = games.filter { it.finished }
                }
            }
            publishLines()
        }
    }

    private fun publishLines() {
        val lines = BasketShare.liveLines(favorites, games)
        val results = BasketShare.resultLines(favorites, games, lines, Instant.now())
        _state.update { it.copy(lines = lines, results = results) }
    }

    /**
     * Taille du coloriage en pixels : la largeur du texte de la page (368 unités sur 412) et le rapport
     * 287 / 411 du dessin de Météo.
     */
    private fun sketchSize(): Pair<Int, Int> {
        val m = app.resources.displayMetrics
        val w = (min(m.widthPixels, m.heightPixels) * 368f / PAPER_WIDTH).roundToInt().coerceIn(64, 2048)
        return w to (w * MeteoShare.SKETCH_RATIO).roundToInt().coerceIn(64, 2048)
    }

    /**
     * Changements signalés par un fournisseur (et ses descendants). Sans l'appli, l'inscription est
     * refusée : le flux reste muet, la page relira à son prochain affichage.
     */
    private fun changes(uri: Uri): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val registered = try {
            resolver.registerContentObserver(uri, true, observer)
            true
        } catch (e: Exception) {
            false
        }
        awaitClose { if (registered) resolver.unregisterContentObserver(observer) }
    }.conflate()

    companion object {
        private const val LIVE_POLL = 15_000L
        private const val IDLE_POLL = 5 * 60_000L

        /** Coloriage relu au plus tard après 15 min (la lumière change avec l'heure). */
        private const val SKETCH_AGE = 15 * 60_000L

        /** Une prévision de plus de 12 h ne dit plus le temps qu'il fait. */
        private const val MAX_METEO_AGE = 12 * 3_600_000L

        /** Des scores de plus de 20 min ne sont plus « en direct ». */
        private const val MAX_SCORES_AGE = 20 * 60_000L

        @Volatile
        private var instance: UnlockFeed? = null

        fun get(context: Context): UnlockFeed =
            instance ?: synchronized(this) { instance ?: UnlockFeed(context).also { instance = it } }
    }
}
