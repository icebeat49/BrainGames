package com.payfacto.braingames.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.GameType

/** How a game is presented in menus. */
data class GameInfo(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
)

object GameCatalog {

    /** Exhaustive on purpose: adding a [GameType] does not compile until it has an entry here. */
    fun infoFor(game: GameType): GameInfo = when (game) {
        GameType.SUDOKU -> GameInfo(
            titleRes = R.string.home_play_sudoku,
            descriptionRes = R.string.home_desc_sudoku,
            icon = Icons.Default.GridOn
        )
        GameType.KAKURO -> GameInfo(
            titleRes = R.string.home_play_kakuro,
            descriptionRes = R.string.home_desc_kakuro,
            icon = Icons.Default.Extension
        )
        GameType.WORD_SEARCH -> GameInfo(
            titleRes = R.string.home_play_wordsearch,
            descriptionRes = R.string.home_desc_wordsearch,
            icon = Icons.Default.Search
        )
    }
}
