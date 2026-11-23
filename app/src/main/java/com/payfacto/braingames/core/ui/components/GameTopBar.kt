package com.payfacto.braingames.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.payfacto.braingames.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    title: String,
    hintsRemaining: Int,
    isMuted: Boolean,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onNewPuzzle: () -> Unit,
    onToggleMute: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = onHint, enabled = hintsRemaining > 0) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.action_hint)
                )
            }
            IconButton(onClick = onNewPuzzle) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.action_new_puzzle)
                )
            }
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted)
                        stringResource(R.string.action_unmute)
                    else
                        stringResource(R.string.action_mute)
                )
            }
        }
    )
}
