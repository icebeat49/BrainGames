package com.payfacto.braingames.core.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.payfacto.braingames.R
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameType
import com.payfacto.braingames.core.ui.GameCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultyPickerScreen(
    gameType: GameType,
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit
) {
    val titleRes = GameCatalog.infoFor(gameType).titleRes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.difficulty_select),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(32.dp))
            Difficulty.entries.forEach { difficulty ->
                Button(
                    onClick = { onDifficultySelected(difficulty) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(difficulty.label())
                }
            }
        }
    }
}

@Composable
private fun Difficulty.label(): String = stringResource(
    when (this) {
        Difficulty.VERY_EASY -> R.string.difficulty_very_easy
        Difficulty.EASY      -> R.string.difficulty_easy
        Difficulty.MEDIUM    -> R.string.difficulty_medium
        Difficulty.HARD      -> R.string.difficulty_hard
        Difficulty.EXPERT    -> R.string.difficulty_expert
    }
)
