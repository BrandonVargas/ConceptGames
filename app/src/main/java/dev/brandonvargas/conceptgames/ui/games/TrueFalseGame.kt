package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.TrueFalseRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TrueFalseGame(round: TrueFalseRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var answer by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val card = round.cards[index]
    val scope = rememberCoroutineScope()
    val offsetX = remember(index) { Animatable(0f) }
    val threshold = with(LocalDensity.current) { 110.dp.toPx() }
    val colors = GameTheme.colors

    fun choose(value: Boolean) {
        if (answer != null) return
        answer = value
        scope.launch { offsetX.animateTo(if (value) threshold * 0.12f else -threshold * 0.12f) }
        if (value == card.isTrue) {
            correct++
            feedback.right()
        } else {
            card.conceptId?.let { missed = missed + it }
            feedback.wrong()
        }
    }

    GameColumn {
        GameProgress(index, round.cards.size)
        Text(
            stringResource(R.string.tf_instructions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val verdict = answer?.let { it == card.isTrue }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = verdict?.let { BorderStroke(3.dp, if (it) colors.correct else colors.wrong) },
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = offsetX.value / 30f
                }
                .pointerInput(index, answer) {
                    if (answer != null) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (abs(offsetX.value) > threshold) choose(offsetX.value > 0) else offsetX.animateTo(0f)
                            }
                        },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + drag) }
                    }
                }
                .testTag("tf_card"),
        ) {
            Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(card.statement, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = { choose(false) },
                enabled = answer == null,
                modifier = Modifier.weight(1f).testTag("tf_false"),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.tf_false))
            }
            FilledTonalButton(
                onClick = { choose(true) },
                enabled = answer == null,
                modifier = Modifier.weight(1f).testTag("tf_true"),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.tf_true))
            }
        }
        FeedbackPanel(
                visible = answer != null,
                correct = verdict == true,
                title = stringResource(if (card.isTrue) R.string.tf_it_is_true else R.string.tf_it_is_false),
                explanation = card.explanation,
                onContinue = {
                    if (index + 1 < round.cards.size) {
                        index++
                        answer = null
                    } else {
                        onFinish(GameResult(GameId.TrueFalse, correct, round.cards.size, missed, durationMs = elapsedSince(start)))
                    }
                },
            )
    }
}
