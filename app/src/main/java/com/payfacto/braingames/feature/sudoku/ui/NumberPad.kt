package com.payfacto.braingames.feature.sudoku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumberPad(
    onNumber: (Int) -> Unit,
    onClear: () -> Unit,
    isNotesMode: Boolean = false,
    onToggleNotesMode: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (n in 1..9) {
            FilledTonalButton(
                onClick = { onNumber(n) },
                modifier = Modifier.size(width = 36.dp, height = 48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = n.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = onClear) {
            Text("✕")
        }
        Spacer(modifier = Modifier.width(16.dp))
        FilledTonalButton(
            onClick = onToggleNotesMode,
            colors = if (isNotesMode) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            }
        ) {
            Text("✏")
        }
    }
}
