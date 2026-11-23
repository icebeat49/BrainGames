package com.payfacto.braingames.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.feature.home.HomeScreen
import com.payfacto.braingames.feature.kakuro.ui.KakuroScreen
import com.payfacto.braingames.feature.settings.SettingsScreen
import com.payfacto.braingames.feature.sudoku.ui.SudokuScreen
import com.payfacto.braingames.feature.wordsearch.ui.WordSearchScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    soundManager: SoundManager
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onGameSelected = { gameType ->
                    navController.navigate(Screen.DifficultyPicker.createRoute(gameType.name))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DifficultyPicker.route,
            arguments = listOf(navArgument(Screen.ARG_GAME_TYPE) { type = NavType.StringType })
        ) { backStack ->
            val gameTypeStr = backStack.arguments?.getString(Screen.ARG_GAME_TYPE) ?: GameType.SUDOKU.name
            val gameType = runCatching { GameType.valueOf(gameTypeStr) }.getOrDefault(GameType.SUDOKU)
            DifficultyPickerScreen(
                gameType = gameType,
                onDifficultySelected = { difficulty ->
                    val route = when (gameType) {
                        GameType.SUDOKU      -> Screen.Sudoku.createRoute(difficulty.name)
                        GameType.KAKURO      -> Screen.Kakuro.createRoute(difficulty.name)
                        GameType.WORD_SEARCH -> Screen.WordSearch.createRoute(difficulty.name)
                    }
                    navController.navigate(route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Game screens: the difficulty argument reaches each ViewModel through its
        // SavedStateHandle, so the screens themselves don't need it.
        composable(
            route = Screen.Sudoku.route,
            arguments = listOf(difficultyArgument())
        ) {
            SudokuScreen(
                soundManager = soundManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Kakuro.route,
            arguments = listOf(difficultyArgument())
        ) {
            KakuroScreen(
                soundManager = soundManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WordSearch.route,
            arguments = listOf(difficultyArgument())
        ) {
            WordSearchScreen(
                soundManager = soundManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun difficultyArgument() =
    navArgument(Screen.ARG_DIFFICULTY) { type = NavType.StringType }
