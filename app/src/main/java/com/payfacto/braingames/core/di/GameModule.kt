package com.payfacto.braingames.core.di

import com.payfacto.braingames.feature.crowns.data.BundledCrownsPuzzleSource
import com.payfacto.braingames.feature.crowns.data.CrownsPuzzleSource
import com.payfacto.braingames.feature.kakuro.data.BundledKakuroPuzzleSource
import com.payfacto.braingames.feature.kakuro.data.KakuroPuzzleSource
import com.payfacto.braingames.feature.sudoku.data.GeneratedSudokuPuzzleSource
import com.payfacto.braingames.feature.sudoku.data.SudokuPuzzleSource
import com.payfacto.braingames.feature.wordsearch.data.GeneratedWordSearchPuzzleSource
import com.payfacto.braingames.feature.wordsearch.data.WordSearchPuzzleSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
abstract class GameModule {

    @Binds
    abstract fun bindSudokuPuzzleSource(impl: GeneratedSudokuPuzzleSource): SudokuPuzzleSource

    @Binds
    abstract fun bindKakuroPuzzleSource(impl: BundledKakuroPuzzleSource): KakuroPuzzleSource

    @Binds
    abstract fun bindWordSearchPuzzleSource(impl: GeneratedWordSearchPuzzleSource): WordSearchPuzzleSource

    @Binds
    abstract fun bindCrownsPuzzleSource(impl: BundledCrownsPuzzleSource): CrownsPuzzleSource

    companion object {
        /** Clock used by GameSession; tests substitute the coroutine scheduler's virtual time. */
        @Provides
        fun provideTimeSource(): TimeSource = TimeSource.Monotonic
    }
}
