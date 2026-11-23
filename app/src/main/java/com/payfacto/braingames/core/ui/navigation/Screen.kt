package com.payfacto.braingames.core.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object DifficultyPicker : Screen("difficulty/{$ARG_GAME_TYPE}") {
        fun createRoute(gameType: String) = "difficulty/$gameType"
    }
    /** `mode` is optional so plain `sudoku/{difficulty}` links keep working. */
    object Sudoku : Screen("sudoku/{$ARG_DIFFICULTY}?$ARG_MODE={$ARG_MODE}") {
        fun createRoute(difficulty: String, mode: String) = "sudoku/$difficulty?$ARG_MODE=$mode"
    }
    object Kakuro : Screen("kakuro/{$ARG_DIFFICULTY}") {
        fun createRoute(difficulty: String) = "kakuro/$difficulty"
    }
    object WordSearch : Screen("wordsearch/{$ARG_DIFFICULTY}") {
        fun createRoute(difficulty: String) = "wordsearch/$difficulty"
    }
    /** Same pattern as Sudoku: `mode` is optional. */
    object Animals : Screen("animals/{$ARG_DIFFICULTY}?$ARG_MODE={$ARG_MODE}") {
        fun createRoute(difficulty: String, mode: String) = "animals/$difficulty?$ARG_MODE=$mode"
    }

    companion object {
        /** Route argument names. Navigation also copies them into each ViewModel's SavedStateHandle. */
        const val ARG_GAME_TYPE = "gameType"
        const val ARG_DIFFICULTY = "difficulty"
        const val ARG_MODE = "mode"
    }
}
