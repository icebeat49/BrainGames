package com.payfacto.braingames.core.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object DifficultyPicker : Screen("difficulty/{gameType}") {
        fun createRoute(gameType: String) = "difficulty/$gameType"
    }
    object Sudoku : Screen("sudoku/{difficulty}") {
        fun createRoute(difficulty: String) = "sudoku/$difficulty"
    }
    object Kakuro : Screen("kakuro/{difficulty}") {
        fun createRoute(difficulty: String) = "kakuro/$difficulty"
    }
    object WordSearch : Screen("wordsearch/{difficulty}") {
        fun createRoute(difficulty: String) = "wordsearch/$difficulty"
    }
}
