package com.payfacto.braingames.core.session

import androidx.lifecycle.SavedStateHandle
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.testutil.fakePreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionTest {

    private val savedState = SavedStateHandle()
    private val preferences: PreferencesRepository = fakePreferencesRepository()

    /** Session on the test scheduler's virtual clock; [backgroundScope] cancels its ticker at the end. */
    private fun TestScope.newSession(handle: SavedStateHandle = savedState) =
        GameSession(
            scope = backgroundScope,
            savedState = handle,
            preferences = preferences,
            timeSource = testScheduler.timeSource
        )

    private fun TestScope.advanceBy(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun TestScope.playingInForeground(): GameSession =
        newSession().apply {
            start()
            onForeground()
        }

    // ── Timer ───────────────────────────────────────────────────────────────

    @Test
    fun `clock stays at zero before the puzzle starts`() = runTest {
        val session = newSession()
        session.onForeground()

        advanceBy(5_000)

        assertEquals(0L, session.state.value.elapsedSeconds)
    }

    @Test
    fun `clock stays at zero until the screen is visible`() = runTest {
        val session = newSession()
        session.start()

        advanceBy(5_000)

        assertEquals(0L, session.state.value.elapsedSeconds)
    }

    @Test
    fun `clock counts whole seconds while playing in the foreground`() = runTest {
        val session = playingInForeground()

        advanceBy(3_000)

        assertEquals(3L, session.state.value.elapsedSeconds)
    }

    @Test
    fun `clock is frozen while the app is in the background`() = runTest {
        val session = playingInForeground()
        advanceBy(2_000)

        session.onBackground()
        advanceBy(60_000)

        assertEquals(2L, session.state.value.elapsedSeconds)
    }

    @Test
    fun `clock resumes on return without losing partial seconds`() = runTest {
        val session = playingInForeground()
        advanceBy(1_500)
        session.onBackground()
        advanceBy(60_000)

        session.onForeground()
        advanceBy(500)

        assertEquals(2L, session.state.value.elapsedSeconds)
    }

    @Test
    fun `complete stops the clock and shows the win screen`() = runTest {
        val session = playingInForeground()
        advanceBy(4_000)

        session.complete()
        advanceBy(10_000)

        assertEquals(4L, session.state.value.elapsedSeconds)
        assertTrue(session.state.value.showWin)
    }

    @Test
    fun `complete with a delay shows the win screen only after the delay`() = runTest {
        val session = playingInForeground()

        session.complete(winDelay = 2.seconds)
        advanceBy(1_999)
        val shownEarly = session.state.value.showWin
        advanceBy(1)

        assertFalse(shownEarly)
        assertTrue(session.state.value.showWin)
    }

    @Test
    fun `reset before a delayed win cancels it`() = runTest {
        val session = playingInForeground()

        session.complete(winDelay = 2.seconds)
        session.reset()
        advanceBy(5_000)

        assertFalse(session.state.value.showWin)
    }

    @Test
    fun `reset zeroes the clock, refills hints and hides the win screen`() = runTest {
        val session = playingInForeground()
        advanceBy(7_000)
        session.consumeHint()
        session.complete()

        session.reset()

        assertEquals(
            SessionState(elapsedSeconds = 0L, hintsRemaining = GameSession.DEFAULT_HINTS, showWin = false),
            session.state.value
        )
    }

    @Test
    fun `clock does not run after reset until the next puzzle starts`() = runTest {
        val session = playingInForeground()

        session.reset()
        advanceBy(3_000)

        assertEquals(0L, session.state.value.elapsedSeconds)
    }

    // ── Hints ───────────────────────────────────────────────────────────────

    @Test
    fun `consumeHint decrements the counter`() = runTest {
        val session = newSession()

        val used = session.consumeHint()

        assertTrue(used)
        assertEquals(GameSession.DEFAULT_HINTS - 1, session.state.value.hintsRemaining)
    }

    @Test
    fun `consumeHint refuses once no hints are left`() = runTest {
        val session = newSession()
        repeat(GameSession.DEFAULT_HINTS) { session.consumeHint() }

        val used = session.consumeHint()

        assertFalse(used)
        assertFalse(session.hasHints)
        assertEquals(0, session.state.value.hintsRemaining)
    }

    // ── Mute ────────────────────────────────────────────────────────────────

    @Test
    fun `toggleMute writes the preference and the state follows it`() = runTest {
        val session = newSession()
        runCurrent()

        session.toggleMute()
        runCurrent()

        assertTrue(preferences.preferences.first().isMuted)
        assertTrue(session.state.value.isMuted)
    }

    // ── Saved state ─────────────────────────────────────────────────────────

    @Test
    fun `recreated session restores elapsed time, hints and win flag`() = runTest {
        val first = playingInForeground()
        advanceBy(2_500)
        first.consumeHint()
        first.complete()

        val recreated = newSession(savedState)

        assertEquals(
            SessionState(elapsedSeconds = 2L, hintsRemaining = GameSession.DEFAULT_HINTS - 1, showWin = true),
            recreated.state.value
        )
    }

    @Test
    fun `recreated session keeps counting from the saved elapsed time`() = runTest {
        val first = playingInForeground()
        advanceBy(2_500)
        first.onBackground()

        val recreated = newSession(savedState)
        recreated.onForeground()
        advanceBy(500)

        assertEquals(3L, recreated.state.value.elapsedSeconds)
    }
}
