package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.FlashcardsRound

@Composable
fun FlashcardsGame(round: FlashcardsRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    var known by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val card = round.cards[index]
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "flip")

    fun grade(knewIt: Boolean) {
        if (knewIt) known++ else missed = missed + card.conceptId
        if (index + 1 < round.cards.size) {
            index++
            flipped = false
        } else {
            onFinish(GameResult(GameId.Flashcards, known, round.cards.size, missed, durationMs = elapsedSince(start)))
        }
    }

    GameColumn {
        GameProgress(index, round.cards.size)
        Surface(
            onClick = { flipped = !flipped },
            shape = RoundedCornerShape(28.dp),
            color = if (rotation <= 90f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .testTag("flashcard"),
        ) {
            Box(
                Modifier
                    .padding(24.dp)
                    // Counter-rotate the back so its text isn't mirrored.
                    .graphicsLayer { rotationY = if (rotation > 90f) 180f else 0f },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (rotation <= 90f) {
                        Text(card.term, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Text(
                            stringResource(R.string.flashcards_tap),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(card.definition, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (flipped) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { grade(false) }, modifier = Modifier.weight(1f).height(52.dp).testTag("fc_learning")) {
                    Text(stringResource(R.string.flashcards_learning))
                }
                Button(onClick = { grade(true) }, modifier = Modifier.weight(1f).height(52.dp).testTag("fc_knew")) {
                    Text(stringResource(R.string.flashcards_knew))
                }
            }
        }
    }
}
