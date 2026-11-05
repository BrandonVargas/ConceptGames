package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.HangmanRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlin.math.cos
import kotlin.math.sin

/** Hangman without the gallows: every wrong letter lets air out of a balloon. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HangmanGame(round: HangmanRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var guesses by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var solvedCount by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val word = round.words[index]
    val mistakes = guesses.count { it !in word.answer }
    val won = word.answer.all { it in guesses }
    val lost = mistakes >= round.maxMistakes
    val colors = GameTheme.colors

    GameColumn {
        GameProgress(index, round.words.size, trailing = pluralStringResource(R.plurals.hangman_lives, round.maxMistakes - mistakes, round.maxMistakes - mistakes))
        Balloon(
            air = 1f - mistakes.toFloat() / round.maxMistakes,
            popped = lost,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .semantics { contentDescription = "${round.maxMistakes - mistakes}/${round.maxMistakes}" },
        )
        PromptCard(word.clue, label = stringResource(R.string.hangman_clue))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            word.answer.forEach { letter ->
                val shown = letter in guesses || lost
                LetterTile(
                    letter = if (shown) letter else "",
                    size = 36.dp,
                    container = when {
                        lost && letter !in guesses -> colors.wrongContainer
                        shown -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        LetterKeyboard(
            letters = word.keyboard,
            stateOf = { letter ->
                when {
                    letter !in guesses -> KeyState.Unused
                    letter in word.answer -> KeyState.Hit
                    else -> KeyState.Miss
                }
            },
            enabled = !won && !lost,
            onKey = { letter ->
                guesses = guesses + letter
                if (letter in word.answer) feedback.right() else feedback.wrong()
            },
        )
        FeedbackPanel(
            visible = won || lost,
            correct = won,
            title = if (won) stringResource(R.string.hangman_won, word.term) else stringResource(R.string.hangman_lost, word.term),
            explanation = null,
            onContinue = {
                val solved = solvedCount + if (won) 1 else 0
                val missedNow = if (won) missed else missed + word.conceptId
                if (index + 1 < round.words.size) {
                    solvedCount = solved
                    missed = missedNow
                    index++
                    guesses = emptyList()
                } else {
                    onFinish(GameResult(GameId.Hangman, solved, round.words.size, missedNow, durationMs = elapsedSince(start)))
                }
            },
        )
    }
}

@Composable
private fun Balloon(air: Float, popped: Boolean, modifier: Modifier) {
    val scale by animateFloatAsState(if (popped) 0f else 0.45f + 0.55f * air, spring(dampingRatio = 0.4f), label = "balloon")
    val balloonColor = MaterialTheme.colorScheme.tertiary
    val stringColor = MaterialTheme.colorScheme.outline
    val burstColor = GameTheme.colors.wrong
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height * 0.42f)
        val radius = size.height * 0.36f * scale
        val knotY = center.y + radius * 1.15f
        // String
        val string = Path().apply {
            moveTo(center.x, knotY)
            cubicTo(center.x - 12f, knotY + 20f, center.x + 12f, knotY + 40f, center.x, size.height)
        }
        drawPath(string, stringColor, style = Stroke(width = 3f))
        if (popped) {
            drawBurst(center, size.height * 0.3f, burstColor)
            return@Canvas
        }
        drawOval(balloonColor, topLeft = Offset(center.x - radius, center.y - radius * 1.15f), size = Size(radius * 2, radius * 2.3f))
        drawOval(Color.White.copy(alpha = 0.35f), topLeft = Offset(center.x - radius * 0.55f, center.y - radius * 0.85f), size = Size(radius * 0.4f, radius * 0.6f))
        val knot = Path().apply {
            moveTo(center.x - 8f, knotY + 8f)
            lineTo(center.x + 8f, knotY + 8f)
            lineTo(center.x, knotY - 2f)
            close()
        }
        drawPath(knot, balloonColor)
    }
}

private fun DrawScope.drawBurst(center: Offset, radius: Float, color: Color) {
    repeat(10) { i ->
        val angle = i * (2 * Math.PI / 10)
        val inner = Offset(center.x + cos(angle).toFloat() * radius * 0.3f, center.y + sin(angle).toFloat() * radius * 0.3f)
        val outer = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
        drawLine(color, inner, outer, strokeWidth = 6f)
    }
}
