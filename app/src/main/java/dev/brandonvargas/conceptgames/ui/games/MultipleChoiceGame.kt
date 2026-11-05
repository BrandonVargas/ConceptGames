package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.MultipleChoiceRound

@Composable
fun MultipleChoiceGame(round: MultipleChoiceRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val question = round.questions[index]

    GameColumn {
        GameProgress(index, round.questions.size)
        PromptCard(
            question.prompt,
            label = if (question.kind == MultipleChoiceRound.Kind.DefinitionToTerm) stringResource(R.string.mcq_which_term) else null,
        )
        question.options.forEachIndexed { optionIndex, option ->
            val state = when {
                selected == null -> OptionState.Idle
                optionIndex == question.correctIndex -> OptionState.Correct
                optionIndex == selected -> OptionState.Wrong
                else -> OptionState.Dimmed
            }
            OptionButton(
                text = option,
                state = state,
                modifier = Modifier.testTag("option_$optionIndex"),
                onClick = {
                    selected = optionIndex
                    if (optionIndex == question.correctIndex) {
                        correct++
                        feedback.right()
                    } else {
                        question.conceptId?.let { missed = missed + it }
                        feedback.wrong()
                    }
                },
            )
        }
        FeedbackPanel(
            visible = selected != null,
            correct = selected == question.correctIndex,
            explanation = question.explanation,
            onContinue = {
                if (index + 1 < round.questions.size) {
                    index++
                    selected = null
                } else {
                    onFinish(GameResult(GameId.MultipleChoice, correct, round.questions.size, missed, durationMs = elapsedSince(start)))
                }
            },
        )
    }
}
