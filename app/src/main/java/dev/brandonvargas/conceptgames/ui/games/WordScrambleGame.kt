package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.WordScrambleRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

/** Tap tiles to move them into the answer slots; tap a filled slot to send its tile back. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordScrambleGame(round: WordScrambleRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    var index by rememberSaveable { mutableIntStateOf(0) }
    val word = round.words[index]
    // slots[i] = index of the tile placed in answer slot i, or -1.
    var slots by rememberSaveable(index) { mutableStateOf(List(word.answer.size) { -1 }) }
    var order by rememberSaveable(index) { mutableStateOf(word.tiles.indices.toList()) }
    var flawed by rememberSaveable(index) { mutableStateOf(false) }
    var done by rememberSaveable(index) { mutableStateOf(false) }
    var shake by rememberSaveable { mutableIntStateOf(0) }
    var solvedCount by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val colors = GameTheme.colors

    fun checkIfFull(newSlots: List<Int>) {
        if (newSlots.any { it < 0 }) return
        if (newSlots.map { word.tiles[it] } == word.answer) {
            done = true
            feedback.right()
        } else {
            flawed = true
            shake++
            feedback.wrong()
        }
    }

    fun reveal() {
        // Place the correct tile for every slot, reusing the tiles that carry each letter.
        val free = word.tiles.indices.toMutableList()
        slots = word.answer.map { letter -> free.first { word.tiles[it] == letter }.also { free.remove(it) } }
        flawed = true
        done = true
    }

    GameColumn {
        GameProgress(index, round.words.size)
        PromptCard(word.clue, label = stringResource(R.string.scramble_clue))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().shakeOn(shake).testTag("scramble_slots"),
        ) {
            slots.forEachIndexed { slotIndex, tile ->
                LetterTile(
                    letter = if (tile >= 0) word.tiles[tile] else "",
                    container = when {
                        done && !flawed -> colors.correctContainer
                        done -> MaterialTheme.colorScheme.secondaryContainer
                        tile >= 0 -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = MaterialTheme.colorScheme.outlineVariant,
                    onClick = if (tile >= 0 && !done) ({ slots = slots.toMutableList().also { it[slotIndex] = -1 } }) else null,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            order.forEach { tileIndex ->
                val used = tileIndex in slots
                LetterTile(
                    letter = word.tiles[tileIndex],
                    container = if (used) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer,
                    content = if (used) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = if (!used && !done) {
                        {
                            val target = slots.indexOf(-1)
                            if (target >= 0) {
                                val updated = slots.toMutableList().also { it[target] = tileIndex }
                                slots = updated
                                checkIfFull(updated)
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag("tile_$tileIndex"),
                )
            }
        }
        if (!done) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { order = order.shuffled() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.scramble_shuffle))
                }
                OutlinedButton(onClick = { slots = List(word.answer.size) { -1 } }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.scramble_clear))
                }
            }
            TextButton(onClick = ::reveal, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_reveal)) }
        }
        FeedbackPanel(
            visible = done,
            correct = !flawed,
            title = word.term,
            explanation = null,
            onContinue = {
                val solved = solvedCount + if (flawed) 0 else 1
                val missedNow = if (flawed) missed + word.conceptId else missed
                if (index + 1 < round.words.size) {
                    solvedCount = solved
                    missed = missedNow
                    index++
                } else {
                    onFinish(GameResult(GameId.WordScramble, solved, round.words.size, missedNow, durationMs = elapsedSince(start)))
                }
            },
        )
    }
}
