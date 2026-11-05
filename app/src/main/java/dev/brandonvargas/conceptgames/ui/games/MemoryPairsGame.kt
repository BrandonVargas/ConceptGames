package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.MemoryPairsRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.delay

@Composable
fun MemoryPairsGame(round: MemoryPairsRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    val pairs = round.cards.size / 2
    var faceUp by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var matched by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var seen by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var errors by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var moves by rememberSaveable { mutableIntStateOf(0) }

    // Two unmatched cards stay visible for a moment, then flip back.
    LaunchedEffect(faceUp) {
        if (faceUp.size == 2) {
            val (a, b) = faceUp.map { round.cards[it] }
            if (a.conceptId == b.conceptId) {
                matched = matched + a.conceptId
                faceUp = emptyList()
                feedback.right()
            } else {
                feedback.wrong()
                delay(1_000)
                faceUp = emptyList()
            }
        }
    }

    LaunchedEffect(matched.size) {
        if (matched.size == pairs) {
            delay(600)
            val missed = errors.distinct()
            onFinish(GameResult(GameId.MemoryPairs, pairs - missed.size, pairs, missed, durationMs = elapsedSince(start)))
        }
    }

    fun flip(index: Int) {
        if (faceUp.size >= 2 || index in faceUp) return
        val card = round.cards[index]
        if (card.conceptId in matched) return
        if (faceUp.size == 1) {
            moves++
            val first = round.cards[faceUp.first()]
            // A miss counts as a memory error only when the right partner had already been seen.
            val partnerSeen = round.cards.any { it.conceptId == first.conceptId && it.index != first.index && it.index in seen }
            if (first.conceptId != card.conceptId && partnerSeen) errors = errors + first.conceptId
        }
        faceUp = faceUp + index
        seen = (seen + index).distinct()
    }

    GameColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.memory_pairs, matched.size, pairs), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.memory_moves, moves), style = MaterialTheme.typography.labelLarge)
        }
        BoxWithConstraints {
            val columns = if (maxWidth > 520.dp) 4 else 3
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                round.cards.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { card ->
                            MemoryCard(
                                card = card,
                                revealed = card.index in faceUp || card.conceptId in matched,
                                matched = card.conceptId in matched,
                                onClick = { flip(card.index) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(card: MemoryPairsRound.Card, revealed: Boolean, matched: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val rotation by animateFloatAsState(if (revealed) 180f else 0f, tween(350), label = "memory_flip")
    val colors = GameTheme.colors
    val front = rotation > 90f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = when {
            !front -> MaterialTheme.colorScheme.primary
            matched -> colors.correctContainer
            card.isTerm -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (matched && front) BorderStroke(2.dp, colors.correct) else null,
        modifier = modifier
            .aspectRatio(0.78f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .testTag("memory_${card.index}"),
    ) {
        Box(
            Modifier.fillMaxSize().padding(6.dp).graphicsLayer { rotationY = if (front) 180f else 0f },
            contentAlignment = Alignment.Center,
        ) {
            if (front) {
                Text(
                    card.text,
                    style = if (card.isTerm) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    fontWeight = if (card.isTerm) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Icon(Icons.Rounded.QuestionMark, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
