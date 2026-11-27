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
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.sound.SoundManager
import com.payfacto.braingames.feature.crowns.ui.CrownsScreen
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
            IfGameEnabled(gameType, backStack, navController) {
                DifficultyPickerScreen(
                    gameType = gameType,
                    onStart = { difficulty, mode, resume ->
                        navController.navigate(gameRoute(gameType, difficulty, mode, resume))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Game screens: the difficulty argument reaches each ViewModel through its
        // SavedStateHandle, so the screens themselves don't need it.
        composable(
            route = Screen.Sudoku.route,
            arguments = listOf(difficultyArgument(), modeArgument(), resumeArgument())
        ) {
            SudokuScreen(
                soundManager = soundManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Kakuro.route,
            arguments = listOf(difficultyArgument(), resumeArgument())
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

        composable(
            route = Screen.Crowns.route,
            arguments = listOf(difficultyArgument(), modeArgument(), resumeArgument())
        ) { backStack ->
            IfGameEnabled(GameType.CROWNS, backStack, navController) {
                CrownsScreen(
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

/** The game screen for a picker choice; [resume] continues the saved game. */
private fun gameRoute(game: GameType, difficulty: Difficulty, mode: GameMode, resume: Boolean): String =
    when (game) {
        GameType.SUDOKU      -> Screen.Sudoku.createRoute(difficulty.name, mode.name, resume)
        GameType.KAKURO      -> Screen.Kakuro.createRoute(difficulty.name, resume)
        GameType.WORD_SEARCH -> Screen.WordSearch.createRoute(difficulty.name)
        GameType.CROWNS      -> Screen.Crowns.createRoute(difficulty.name, mode.name, resume)
    }

/** Optional `?resume=`; false starts a new puzzle. */
private fun resumeArgument() =
    navArgument(Screen.ARG_RESUME) {
        type = NavType.BoolType
        defaultValue = false
    }

private fun difficultyArgument() =
    navArgument(Screen.ARG_DIFFICULTY) { type = NavType.StringType }

/** Optional `?mode=`; links without it start in Classic. */
private fun modeArgument() =
    navArgument(Screen.ARG_MODE) {
        type = NavType.StringType
        defaultValue = GameMode.CLASSIC.name
    }
