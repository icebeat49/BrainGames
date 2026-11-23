package com.payfacto.braingames.feature.wordsearch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.payfacto.braingames.feature.wordsearch.model.WordEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordList(words: List<WordEntry>) {
    FlowRow(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        words.forEach { entry ->
            AssistChip(
                onClick = {},
                enabled = !entry.found,
                label = {
                    Text(
                        text = entry.word,
                        textDecoration = if (entry.found) TextDecoration.LineThrough else null,
                        color = if (entry.found) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (entry.found)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
