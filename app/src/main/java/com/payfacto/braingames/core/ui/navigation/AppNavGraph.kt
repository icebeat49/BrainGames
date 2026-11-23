package com.payfacto.braingames.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.payfacto.braingames.core.config.FeatureFlags
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.feature.home.HomeScreen
import com.payfacto.braingames.feature.kakuro.ui.KakuroScreen
import com.payfacto.braingames.feature.settings.SettingsScreen
import com.payfacto.braingames.feature.sudoku.model.SudokuMode
import com.payfacto.braingames.feature.sudoku.ui.SudokuDifficultyPicker
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
            val onBack: () -> Unit = { navController.popBackStack() }
            IfGameEnabled(gameType, backStack, navController) {
                when (gameType) {
                    GameType.SUDOKU -> SudokuDifficultyPicker(
                        onStart = { difficulty, mode ->
                            navController.navigate(Screen.Sudoku.createRoute(difficulty.name, mode.name))
                        },
                        onBack = onBack
                    )
                    GameType.KAKURO -> DifficultyPickerScreen(
                        gameType = gameType,
                        onDifficultySelected = { navController.navigate(Screen.Kakuro.createRoute(it.name)) },
                        onBack = onBack
                    )
                    GameType.WORD_SEARCH -> DifficultyPickerScreen(
                        gameType = gameType,
                        onDifficultySelected = { navController.navigate(Screen.WordSearch.createRoute(it.name)) },
                        onBack = onBack
                    )
                }
            }
        }

        // Game screens: the difficulty argument reaches each ViewModel through its
        // SavedStateHandle, so the screens themselves don't need it.
        composable(
            route = Screen.Sudoku.route,
            arguments = listOf(
                difficultyArgument(),
                navArgument(Screen.ARG_MODE) {
                    type = NavType.StringType
                    defaultValue = SudokuMode.CLASSIC.name
                }
            )
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

        // Registered even when Word Search is hidden, so the code stays compiled and can be re-enabled.
        composable(
            route = Screen.WordSearch.route,
            arguments = listOf(difficultyArgument())
        ) { backStack ->
            IfGameEnabled(GameType.WORD_SEARCH, backStack, navController) {
                WordSearchScreen(
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Shows [content] only when [game] is enabled. Otherwise renders nothing and pops
 * [entry] off the back stack, provided it is still the current destination.
 */
@Composable
private fun IfGameEnabled(
    game: GameType,
    entry: NavBackStackEntry,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    if (FeatureFlags.isEnabled(game)) {
        content()
    } else {
        LaunchedEffect(entry) {
            if (navController.currentBackStackEntry?.id == entry.id) navController.popBackStack()
        }
    }
}

private fun difficultyArgument() =
    navArgument(Screen.ARG_DIFFICULTY) { type = NavType.StringType }
