package com.payfacto.braingames.core.session

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** What every game screen shows besides its board. */
data class SessionState(
    val elapsedSeconds: Long = 0L,
    val hintsRemaining: Int = GameSession.DEFAULT_HINTS,
    val isMuted: Boolean = false,
    val showWin: Boolean = false,
)

/**
 * State shared by all games: the elapsed-time clock, the hint counter, the mute
 * preference and the win flag. Owned by a game's ViewModel and driven by it.
 *
 * The clock runs only while a puzzle is being played ([start] … [complete]) **and**
 * the screen is visible ([onForeground] … [onBackground]). It measures real elapsed time
 * from [timeSource], so pausing keeps partial seconds.
 *
 * Elapsed time, hints, the playing flag and the win flag are written to [savedState],
 * so a ViewModel recreated after process death continues where it left off.
 */
class GameSession(
    private val scope: CoroutineScope,
    private val savedState: SavedStateHandle,
    private val preferences: PreferencesRepository,
    private val maxHints: Int = DEFAULT_HINTS,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var accumulatedMs: Long = savedState[KEY_ELAPSED_MS] ?: 0L
    private var isPlaying: Boolean = savedState[KEY_PLAYING] ?: false
    private var isForeground = false
    private var runningSince: TimeMark? = null
    private var tickerJob: Job? = null
    private var pendingWinJob: Job? = null

    private val _state = MutableStateFlow(
        SessionState(
            elapsedSeconds = accumulatedMs / MS_PER_SECOND,
            hintsRemaining = savedState[KEY_HINTS] ?: maxHints,
            showWin = savedState[KEY_SHOW_WIN] ?: false,
        )
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val hasHints: Boolean get() = _state.value.hintsRemaining > 0

    init {
        scope.launch {
            preferences.preferences.map { it.isMuted }.distinctUntilChanged().collect { muted ->
                _state.update { it.copy(isMuted = muted) }
            }
        }
    }

    /** Prepares for a new puzzle: clock at zero and stopped, hints refilled, win hidden. */
    fun reset() {
        pendingWinJob?.cancel()
        isPlaying = false
        stopClock()
        accumulatedMs = 0L
        publishElapsed()
        savedState[KEY_PLAYING] = false
        setHints(maxHints)
        setShowWin(false)
    }

    /** The puzzle is ready: the clock runs whenever the screen is visible. */
    fun start() {
        isPlaying = true
        savedState[KEY_PLAYING] = true
        updateClock()
    }

    /** Uses one hint. Returns false, and changes nothing, when none are left. */
    fun consumeHint(): Boolean {
        if (!hasHints) return false
        setHints(_state.value.hintsRemaining - 1)
        return true
    }

    /** The puzzle is solved: stops the clock now and shows the win screen after [winDelay]. */
    fun complete(winDelay: Duration = Duration.ZERO) {
        isPlaying = false
        savedState[KEY_PLAYING] = false
        updateClock()
        if (winDelay <= Duration.ZERO) {
            setShowWin(true)
        } else {
            pendingWinJob = scope.launch {
                delay(winDelay)
                setShowWin(true)
            }
        }
    }

    fun dismissWin() = setShowWin(false)

    /**
     * Reads time, hints and the playing and win flags again from the SavedStateHandle,
     * after a saved game was copied into it.
     */
    fun reloadSaved() {
        stopClock()
        accumulatedMs = savedState[KEY_ELAPSED_MS] ?: 0L
        isPlaying = savedState[KEY_PLAYING] ?: false
        publishElapsed()
        setHints(savedState[KEY_HINTS] ?: maxHints)
        setShowWin(savedState[KEY_SHOW_WIN] ?: false)
        updateClock()
    }

    fun onForeground() {
        isForeground = true
        updateClock()
    }

    fun onBackground() {
        isForeground = false
        updateClock()
    }

    fun toggleMute() {
        scope.launch { preferences.setMuted(!_state.value.isMuted) }
    }

    private fun updateClock() {
        val shouldRun = isPlaying && isForeground
        when {
            shouldRun && runningSince == null -> startClock()
            !shouldRun && runningSince != null -> stopClock()
        }
    }

    private fun startClock() {
        runningSince = timeSource.markNow()
        tickerJob = scope.launch {
            while (isActive) {
                val elapsedMs = currentElapsedMs()
                publishElapsed(elapsedMs)
                delay(MS_PER_SECOND - elapsedMs % MS_PER_SECOND)
            }
        }
    }

    private fun stopClock() {
        val since = runningSince ?: return
        accumulatedMs += since.elapsedNow().inWholeMilliseconds
        runningSince = null
        tickerJob?.cancel()
        tickerJob = null
        publishElapsed()
    }

    private fun currentElapsedMs(): Long =
        accumulatedMs + (runningSince?.elapsedNow()?.inWholeMilliseconds ?: 0L)

    private fun publishElapsed(elapsedMs: Long = currentElapsedMs()) {
        savedState[KEY_ELAPSED_MS] = elapsedMs
        _state.update { it.copy(elapsedSeconds = elapsedMs / MS_PER_SECOND) }
    }

    private fun setHints(value: Int) {
        savedState[KEY_HINTS] = value
        _state.update { it.copy(hintsRemaining = value) }
    }

    private fun setShowWin(value: Boolean) {
        savedState[KEY_SHOW_WIN] = value
        _state.update { it.copy(showWin = value) }
    }

    companion object {
        const val DEFAULT_HINTS = 5
        private const val MS_PER_SECOND = 1_000L

        private const val KEY_ELAPSED_MS = "session_elapsed_ms"
        private const val KEY_HINTS = "session_hints"
        private const val KEY_PLAYING = "session_playing"
        private const val KEY_SHOW_WIN = "session_show_win"

        /** The SavedStateHandle entries a disk save must keep. */
        val SAVED_KEYS = listOf(KEY_ELAPSED_MS, KEY_HINTS, KEY_PLAYING, KEY_SHOW_WIN)

        /** Whether the game in [handle] is being played: loaded and not yet won. */
        fun isPlayingIn(handle: SavedStateHandle): Boolean = handle[KEY_PLAYING] ?: false

        /** Whether the game in [handle] was won and its win screen is still due. */
        fun isWonIn(handle: SavedStateHandle): Boolean = handle[KEY_SHOW_WIN] ?: false

        fun elapsedSecondsIn(values: Map<String, Any>): Long =
            (values[KEY_ELAPSED_MS] as? Long ?: 0L) / MS_PER_SECOND
    }
}
