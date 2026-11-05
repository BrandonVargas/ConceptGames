package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.OddOneOutRound

@Composable
fun OddOneOutGame(round: OddOneOutRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    val set = round.sets[index]

    GameColumn {
        GameProgress(index, round.sets.size)
        Text(stringResource(R.string.odd_instructions), style = MaterialTheme.typography.titleLarge)
        set.items.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { column, item ->
                    val itemIndex = rowIndex * 2 + column
                    val state = when {
                        selected == null -> OptionState.Idle
                        itemIndex == set.oddIndex -> OptionState.Correct
                        itemIndex == selected -> OptionState.Wrong
                        else -> OptionState.Dimmed
                    }
                    OptionButton(item, state, modifier = Modifier.weight(1f).testTag("option_$itemIndex"), onClick = {
                        selected = itemIndex
                        if (itemIndex == set.oddIndex) {
                            correct++
                            feedback.right()
                        } else {
                            feedback.wrong()
                        }
                    })
                }
                if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
        FeedbackPanel(
            visible = selected != null,
            correct = selected == set.oddIndex,
            title = stringResource(R.string.odd_answer, set.items[set.oddIndex]),
            explanation = set.reason,
            onContinue = {
                if (index + 1 < round.sets.size) {
                    index++
                    selected = null
                } else {
                    onFinish(GameResult(GameId.OddOneOut, correct, round.sets.size, durationMs = elapsedSince(start)))
                }
            },
        )
    }
}
