package com.payfacto.braingames.core.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object DifficultyPicker : Screen("difficulty/{$ARG_GAME_TYPE}") {
        fun createRoute(gameType: String) = "difficulty/$gameType"
    }
    /** `mode` and `resume` are optional so plain `sudoku/{difficulty}` links keep working. */
    object Sudoku : Screen("sudoku/{$ARG_DIFFICULTY}?$ARG_MODE={$ARG_MODE}&$ARG_RESUME={$ARG_RESUME}") {
        fun createRoute(difficulty: String, mode: String, resume: Boolean = false) =
            "sudoku/$difficulty?$ARG_MODE=$mode&$ARG_RESUME=$resume"
    }
    object Kakuro : Screen("kakuro/{$ARG_DIFFICULTY}?$ARG_RESUME={$ARG_RESUME}") {
        fun createRoute(difficulty: String, resume: Boolean = false) = "kakuro/$difficulty?$ARG_RESUME=$resume"
    }
    object WordSearch : Screen("wordsearch/{$ARG_DIFFICULTY}") {
        fun createRoute(difficulty: String) = "wordsearch/$difficulty"
    }
    /** Same pattern as Sudoku: `mode` and `resume` are optional. */
    object Crowns : Screen("crowns/{$ARG_DIFFICULTY}?$ARG_MODE={$ARG_MODE}&$ARG_RESUME={$ARG_RESUME}") {
        fun createRoute(difficulty: String, mode: String, resume: Boolean = false) =
            "crowns/$difficulty?$ARG_MODE=$mode&$ARG_RESUME=$resume"
    }

    companion object {
        /** Route argument names. Navigation also copies them into each ViewModel's SavedStateHandle. */
        const val ARG_GAME_TYPE = "gameType"
        const val ARG_DIFFICULTY = "difficulty"
        const val ARG_MODE = "mode"
        /** True when the player chose Continue: the game loads its saved state instead of a new puzzle. */
        const val ARG_RESUME = "resume"
    }
}
