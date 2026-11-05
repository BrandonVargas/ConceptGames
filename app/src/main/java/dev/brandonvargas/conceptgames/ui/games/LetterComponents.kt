package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

enum class KeyState { Unused, Hit, Miss }

/** A square letter cell: answer slots, scramble tiles and keyboard keys share this look. */
@Composable
fun LetterTile(
    letter: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurface,
    border: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    val inner: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Text(letter, style = if (size < 36.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge)
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = container,
            contentColor = content,
            border = border?.let { BorderStroke(2.dp, it) },
            modifier = modifier.size(size),
            content = inner,
        )
    } else {
        Surface(
            shape = shape,
            color = container,
            contentColor = content,
            border = border?.let { BorderStroke(2.dp, it) },
            modifier = modifier.size(size),
            content = inner,
        )
    }
}

/** On-screen keyboard built from the pack's own alphabet, so every Wikipedia language works. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterKeyboard(
    letters: List<String>,
    stateOf: (String) -> KeyState,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keySize: Dp = 40.dp,
) {
    val colors = GameTheme.colors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        letters.forEach { letter ->
            val state = stateOf(letter)
            LetterTile(
                letter = letter,
                size = keySize,
                container = when (state) {
                    KeyState.Unused -> MaterialTheme.colorScheme.surfaceContainerHighest
                    KeyState.Hit -> colors.correctContainer
                    KeyState.Miss -> MaterialTheme.colorScheme.surfaceVariant
                },
                content = when (state) {
                    KeyState.Unused -> MaterialTheme.colorScheme.onSurface
                    KeyState.Hit -> colors.onCorrectContainer
                    KeyState.Miss -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                onClick = if (enabled && state == KeyState.Unused) ({ onKey(letter) }) else null,
                modifier = Modifier.testTag("key_$letter"),
            )
        }
    }
}
