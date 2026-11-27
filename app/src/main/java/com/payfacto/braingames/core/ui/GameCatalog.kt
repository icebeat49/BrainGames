package com.payfacto.braingames.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.feature.crowns.engine.CrownsDifficulty
import com.payfacto.braingames.feature.kakuro.engine.KakuroDifficulty
import com.payfacto.braingames.feature.crowns.ui.CrownIcon
import com.payfacto.braingames.feature.wordsearch.data.WordSearchDifficulty

/** How a game is presented in menus. */
data class GameInfo(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    /** One description per mode when the game offers Classic / Hardcore; null otherwise. */
    val modeDescriptions: Map<GameMode, Int>? = null,
    /** Grid side per difficulty, shown on the picker; null when it says nothing useful. */
    val gridSize: ((Difficulty) -> Int)? = null
) {
    val supportsModes: Boolean get() = modeDescriptions != null
}

object GameCatalog {

    /** Exhaustive on purpose: adding a [GameType] does not compile until it has an entry here. */
    fun infoFor(game: GameType): GameInfo = when (game) {
        GameType.SUDOKU -> GameInfo(
            titleRes = R.string.home_play_sudoku,
            descriptionRes = R.string.home_desc_sudoku,
            icon = Icons.Default.GridOn,
            modeDescriptions = mapOf(
                GameMode.CLASSIC to R.string.sudoku_mode_classic_description,
                GameMode.HARDCORE to R.string.sudoku_mode_hardcore_description
            )
            // Always 9×9: no size label.
        )
        GameType.KAKURO -> GameInfo(
            titleRes = R.string.home_play_kakuro,
            descriptionRes = R.string.home_desc_kakuro,
            icon = Icons.Default.Extension,
            gridSize = KakuroDifficulty::sizeFor
        )
        GameType.WORD_SEARCH -> GameInfo(
            titleRes = R.string.home_play_wordsearch,
            descriptionRes = R.string.home_desc_wordsearch,
            icon = Icons.Default.Search,
            gridSize = WordSearchDifficulty::sizeFor
        )
        GameType.CROWNS -> GameInfo(
            titleRes = R.string.home_play_crowns,
            descriptionRes = R.string.home_desc_crowns,
            icon = CrownIcon,
            modeDescriptions = mapOf(
                GameMode.CLASSIC to R.string.crowns_mode_classic_description,
                GameMode.HARDCORE to R.string.crowns_mode_hardcore_description
            ),
            gridSize = CrownsDifficulty::sizeFor
        )
    }
}
