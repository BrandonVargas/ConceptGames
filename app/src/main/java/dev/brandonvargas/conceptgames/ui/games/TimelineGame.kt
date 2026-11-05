package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.TimelineRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme

/** Long-press and drag (or use the arrows) to put the steps in order, then check. */
@Composable
fun TimelineGame(round: TimelineRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    val colors = GameTheme.colors
    var order by rememberSaveable { mutableStateOf(round.startOrder) }
    var checkedScore by rememberSaveable { mutableStateOf<Int?>(null) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    var draggingStep by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val spacingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val checked = checkedScore != null

    fun move(from: Int, to: Int) {
        if (to !in order.indices || from == to) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
    }

    GameColumn {
        Text(round.title, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.timeline_instructions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            order.forEachIndexed { position, stepIndex ->
                // Keyed by step so a card keeps its drag gesture while it moves between positions.
                key(stepIndex) {
                val step = round.steps[stepIndex]
                val inPlace = stepIndex == position
                val isDragging = draggingStep == stepIndex
                val elevation by animateDpAsState(if (isDragging) 8.dp else 1.dp, label = "lift")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = elevation,
                    color = when {
                        !checked -> MaterialTheme.colorScheme.surfaceContainerHigh
                        inPlace -> colors.correctContainer
                        else -> colors.wrongContainer
                    },
                    border = if (checked) BorderStroke(2.dp, if (inPlace) colors.correct else colors.wrong) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .onSizeChanged { rowHeight = it.height }
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .pointerInput(stepIndex, checked) {
                            if (checked) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingStep = stepIndex
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggingStep = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggingStep = null
                                    dragOffset = 0f
                                },
                            ) { change, amount ->
                                change.consume()
                                val current = order.indexOf(stepIndex)
                                dragOffset += amount.y
                                val slot = rowHeight + spacingPx
                                // Swap with a neighbour once the card is dragged past half of it.
                                if (dragOffset > slot / 2 && current < order.lastIndex) {
                                    move(current, current + 1)
                                    dragOffset -= slot
                                } else if (dragOffset < -slot / 2 && current > 0) {
                                    move(current, current - 1)
                                    dragOffset += slot
                                }
                            }
                        }
                        .testTag("timeline_$position"),
                ) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                            Text("${position + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(step.label, style = MaterialTheme.typography.bodyLarge)
                            if (revealed && step.detail != null) {
                                Text(step.detail!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (!checked) {
                            Column {
                                IconButton(onClick = { move(position, position - 1) }, enabled = position > 0, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.timeline_move_up))
                                }
                                IconButton(onClick = { move(position, position + 1) }, enabled = position < order.lastIndex, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.timeline_move_down))
                                }
                            }
                            Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        } else {
                            ResultIcon(inPlace, Modifier.padding(end = 8.dp))
                        }
                    }
                }
                }
            }
        }
        when {
            !checked -> Button(
                onClick = {
                    val score = order.withIndex().count { (position, step) -> position == step }
                    checkedScore = score
                    if (score == order.size) feedback.right() else feedback.wrong()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("timeline_check"),
            ) { Text(stringResource(R.string.timeline_check)) }
            !revealed -> FeedbackPanel(
                visible = true,
                correct = checkedScore == order.size,
                title = stringResource(R.string.timeline_score, checkedScore ?: 0, order.size),
                explanation = null,
                onContinue = {
                    order = round.steps.indices.toList()
                    revealed = true
                },
            )
            else -> Button(
                onClick = { onFinish(GameResult(GameId.Timeline, checkedScore ?: 0, order.size, durationMs = elapsedSince(start))) },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("continue"),
            ) { Text(stringResource(R.string.action_continue)) }
        }
    }
}
