package com.payfacto.braingames.core.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object DifficultyPicker : Screen("difficulty/{$ARG_GAME_TYPE}") {
        fun createRoute(gameType: String) = "difficulty/$gameType"
    }
    object Sudoku : Screen("sudoku/{$ARG_DIFFICULTY}") {
        fun createRoute(difficulty: String) = "sudoku/$difficulty"
    }
    object Kakuro : Screen("kakuro/{$ARG_DIFFICULTY}") {
        fun createRoute(difficulty: String) = "kakuro/$difficulty"
    }
    object WordSearch : Screen("wordsearch/{$ARG_DIFFICULTY}") {
        fun createRoute(difficulty: String) = "wordsearch/$difficulty"
    }

    companion object {
        /** Route argument names. Navigation also copies them into each ViewModel's SavedStateHandle. */
        const val ARG_GAME_TYPE = "gameType"
        const val ARG_DIFFICULTY = "difficulty"
    }
}
