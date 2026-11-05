package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.FillBlankRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

@Composable
fun FillBlankGame(round: FillBlankRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val item = round.items[index]
    val colors = GameTheme.colors
    val isRight = selected == item.correctIndex

    GameColumn {
        GameProgress(index, round.items.size)
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            val sentence = buildAnnotatedString {
                append(item.before)
                if (selected == null) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) { append(" _______ ") }
                } else {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = if (isRight) colors.correct else colors.wrong,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) { append(item.answer) }
                }
                append(item.after)
            }
            Text(sentence, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
        }
        item.options.forEachIndexed { optionIndex, option ->
            val state = when {
                selected == null -> OptionState.Idle
                optionIndex == item.correctIndex -> OptionState.Correct
                optionIndex == selected -> OptionState.Wrong
                else -> OptionState.Dimmed
            }
            OptionButton(option, state, modifier = Modifier.testTag("option_$optionIndex"), onClick = {
                selected = optionIndex
                if (optionIndex == item.correctIndex) {
                    correct++
                    feedback.right()
                } else {
                    item.conceptId?.let { missed = missed + it }
                    feedback.wrong()
                }
            })
        }
        FeedbackPanel(
            visible = selected != null,
            correct = isRight,
            explanation = null,
            onContinue = {
                if (index + 1 < round.items.size) {
                    index++
                    selected = null
                } else {
                    onFinish(GameResult(GameId.FillBlank, correct, round.items.size, missed, durationMs = elapsedSince(start)))
                }
            },
        )
    }
}
