package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

/** "3 / 10" with a progress bar, shown at the top of most games. */
@Composable
fun GameProgress(current: Int, total: Int, modifier: Modifier = Modifier, trailing: String? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.game_progress, (current + 1).coerceAtMost(total), total),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else current.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

enum class OptionState { Idle, Correct, Wrong, Dimmed }

/** A big tappable answer used by quizzes. */
@Composable
fun OptionButton(text: String, state: OptionState, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = GameTheme.colors
    val (container, content, border) = when (state) {
        OptionState.Idle -> Triple(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.outlineVariant)
        OptionState.Correct -> Triple(colors.correctContainer, colors.onCorrectContainer, colors.correct)
        OptionState.Wrong -> Triple(colors.wrongContainer, colors.onWrongContainer, colors.wrong)
        OptionState.Dimmed -> Triple(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        onClick = onClick,
        enabled = enabled && state == OptionState.Idle,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        border = BorderStroke(if (state == OptionState.Idle) 1.dp else 2.dp, border),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when (state) {
                OptionState.Correct -> Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.game_correct), tint = colors.correct)
                OptionState.Wrong -> Icon(Icons.Rounded.Cancel, contentDescription = stringResource(R.string.game_wrong), tint = colors.wrong)
                else -> Unit
            }
        }
    }
}

/** Right/wrong verdict with an explanation and a Continue button. */
@Composable
fun FeedbackPanel(
    visible: Boolean,
    correct: Boolean,
    explanation: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = GameTheme.colors
    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }, modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (correct) colors.correctContainer else colors.wrongContainer,
            contentColor = if (correct) colors.onCorrectContainer else colors.onWrongContainer,
            modifier = Modifier.fillMaxWidth().testTag("feedback"),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = if (correct) colors.correct else colors.wrong,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title ?: stringResource(if (correct) R.string.game_correct else R.string.game_wrong),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (!explanation.isNullOrBlank()) Text(explanation, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("continue")) {
                    Text(stringResource(R.string.action_continue))
                }
            }
        }
    }
}

/** Haptic + optional shake feedback helpers shared by games. */
class GameFeedback(private val haptics: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    fun right() = haptics.performHapticFeedback(HapticFeedbackType.Confirm)

    fun wrong() = haptics.performHapticFeedback(HapticFeedbackType.Reject)
}

@Composable
fun rememberGameFeedback(): GameFeedback {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { GameFeedback(haptics) }
}

/** Shakes its content horizontally whenever [trigger] changes to a new non-zero value. */
@Composable
fun Modifier.shakeOn(trigger: Int): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (target in listOf(-18f, 16f, -12f, 8f, -4f, 0f)) {
            offset.animateTo(target, spring(stiffness = Spring.StiffnessHigh))
        }
    }
    return graphicsLayer { translationX = offset.value }
}

/** Centered prompt card used for questions and statements. */
@Composable
fun PromptCard(text: String, modifier: Modifier = Modifier, label: String? = null, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    Surface(shape = RoundedCornerShape(24.dp), color = color, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (label != null) Text(label, style = MaterialTheme.typography.labelLarge)
            Text(text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start)
        }
    }
}

@Composable
fun ResultIcon(correct: Boolean, modifier: Modifier = Modifier) {
    val colors = GameTheme.colors
    Icon(
        if (correct) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
        contentDescription = null,
        tint = if (correct) colors.correct else colors.wrong,
        modifier = modifier.size(20.dp),
    )
}

/** Milliseconds since the composable first appeared, for result durations. */
@Composable
fun rememberStartTime(): Long = remember { System.currentTimeMillis() }

fun elapsedSince(start: Long) = System.currentTimeMillis() - start
