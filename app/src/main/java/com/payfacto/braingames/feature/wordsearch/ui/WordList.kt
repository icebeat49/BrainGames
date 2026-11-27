package com.payfacto.braingames.feature.wordsearch.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.payfacto.braingames.feature.wordsearch.model.WordEntry

/** Words still to find first, then the found ones, each group alphabetical. */
internal fun wordListOrder(words: List<WordEntry>): List<WordEntry> =
    words.sortedWith(compareBy<WordEntry> { it.found }.thenBy { it.word })

/**
 * Compact labels rather than chips: chips reserve a 48dp touch target each, which with up to
 * ~100 words on 18×18 pushes most of the list below the screen.
 *
 * [onWordTapped] (Hardcore: tick a word off or back on) makes the labels tappable; null
 * leaves them as plain text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordList(
    words: List<WordEntry>,
    onWordTapped: ((String) -> Unit)? = null
) {
    val shape = RoundedCornerShape(6.dp)
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        wordListOrder(words).forEach { entry ->
            val outline = if (entry.found) colors.outlineVariant else colors.outline
            val tap = onWordTapped?.let { Modifier.clickable { it(entry.word) } } ?: Modifier
            Text(
                text = entry.word,
                modifier = Modifier
                    .clip(shape)
                    .border(1.dp, outline, shape)
                    .then(tap)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (entry.found) TextDecoration.LineThrough else null,
                color = if (entry.found) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface
            )
        }
    }
}
