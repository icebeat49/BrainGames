package com.payfacto.braingames.feature.kakuro.data

import com.payfacto.braingames.core.data.RecentPuzzles
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.feature.kakuro.engine.KakuroDifficulty
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Supplies new Kakuro puzzles. An interface so ViewModel tests can use a fixed board. */
interface KakuroPuzzleSource {
    suspend fun newPuzzle(difficulty: Difficulty): KakuroBoard
}

/** Serves puzzles from the bundled bank, at random but never one of the recent ones for that size. */
class BundledKakuroPuzzleSource @Inject constructor(
    private val bank: KakuroPuzzleBank,
    private val recent: RecentPuzzles
) : KakuroPuzzleSource {

    override suspend fun newPuzzle(difficulty: Difficulty): KakuroBoard = withContext(Dispatchers.IO) {
        val size = KakuroDifficulty.sizeFor(difficulty)
        val puzzles = bank.puzzles(size)
        puzzles[recent.pick(GameType.KAKURO, size, puzzles.size)].toBoard()
    }
}
