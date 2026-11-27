package com.payfacto.braingames.core.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.payfacto.braingames.R
import com.payfacto.braingames.core.config.FeatureFlags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTopBar(
    title: String,
    hintsRemaining: Int,
    isMuted: Boolean,
    onBack: () -> Unit,
    /** null hides the hint button entirely (e.g. Sudoku Hardcore). */
    onHint: (() -> Unit)?,
    onNewPuzzle: () -> Unit,
    onToggleMute: () -> Unit,
    /** null hides the "How to play" button (games without one). */
    onHelp: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            // One line, shrinking to fit: with four actions on a 360dp phone a long title
            // ("Couronnes") would otherwise wrap in the middle of the word.
            val style = LocalTextStyle.current
            BasicText(
                text = title,
                style = style.copy(color = LocalContentColor.current),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = MIN_TITLE_SIZE, maxFontSize = style.fontSize)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        actions = {
            if (onHint != null) {
                IconButton(onClick = onHint, enabled = hintsRemaining > 0) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.action_hint)
                    )
                }
            }
            if (onHelp != null) {
                IconButton(onClick = onHelp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.how_to_play)
                    )
                }
            }
            IconButton(onClick = onNewPuzzle) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.action_new_puzzle)
                )
            }
            if (FeatureFlags.isSoundEnabled) {
                IconButton(onClick = onToggleMute) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted)
                            stringResource(R.string.action_unmute)
                        else
                            stringResource(R.string.action_mute)
                    )
                }
            }
        }
    )
}

private val MIN_TITLE_SIZE = 14.sp
