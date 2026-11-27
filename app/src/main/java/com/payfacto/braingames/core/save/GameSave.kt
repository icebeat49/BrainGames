package com.payfacto.braingames.core.save

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.session.GameSession

/**
 * A game in progress, as kept on disk between app launches.
 *
 * [values] holds the same entries the game keeps in its SavedStateHandle (its saver's keys
 * and [GameSession]'s), so resuming only has to copy them back. Allowed value types:
 * Int, Long, Boolean, IntArray and BooleanArray.
 */
class GameSave(
    val difficulty: Difficulty,
    val mode: GameMode,
    val values: Map<String, Any>
) {
    /** Time played so far, for the Continue button. */
    val elapsedSeconds: Long get() = GameSession.elapsedSecondsIn(values)

    override fun equals(other: Any?): Boolean =
        other is GameSave && difficulty == other.difficulty && mode == other.mode &&
            values.keys == other.values.keys &&
            values.all { (key, value) -> contentEquals(value, other.values.getValue(key)) }

    override fun hashCode(): Int = 31 * difficulty.hashCode() + mode.hashCode()

    private fun contentEquals(a: Any, b: Any): Boolean = when (a) {
        is IntArray -> b is IntArray && a.contentEquals(b)
        is BooleanArray -> b is BooleanArray && a.contentEquals(b)
        else -> a == b
    }
}
