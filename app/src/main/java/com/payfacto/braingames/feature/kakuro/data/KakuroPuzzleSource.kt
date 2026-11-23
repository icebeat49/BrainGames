package com.payfacto.braingames.feature.kakuro.data

import android.content.Context
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.feature.kakuro.model.KakuroBoard
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Supplies new Kakuro puzzles. An interface so ViewModel tests can use a fixed board. */
interface KakuroPuzzleSource {
    fun newPuzzle(difficulty: Difficulty): KakuroBoard
}

class BundledKakuroPuzzleSource @Inject constructor(
    @ApplicationContext private val context: Context
) : KakuroPuzzleSource {
    override fun newPuzzle(difficulty: Difficulty): KakuroBoard =
        KakuroPuzzleBank.load(context, difficulty)
}
