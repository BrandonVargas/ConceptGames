package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.RopeMatchRound
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Drag a rope from a term to its definition. A right match ties the rope tight; a wrong one makes it snap back.
 * Tapping a term and then a definition does the same, for players who prefer not to drag.
 */
@Composable
fun RopeMatchGame(round: RopeMatchRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    var boardIndex by rememberSaveable { mutableIntStateOf(0) }
    var missed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val total = round.boards.sumOf { it.terms.size }

    GameColumn {
        GameProgress(boardIndex, round.boards.size)
        Text(stringResource(R.string.rope_instructions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.runtime.key(boardIndex) {
            RopeBoard(
                board = round.boards[boardIndex],
                onMistake = { conceptId -> if (conceptId !in missed) missed = missed + conceptId },
                onComplete = {
                    if (boardIndex + 1 < round.boards.size) {
                        boardIndex++
                    } else {
                        val distinctMissed = missed.distinct()
                        onFinish(GameResult(GameId.RopeMatch, total - distinctMissed.size, total, distinctMissed, durationMs = elapsedSince(start)))
                    }
                },
            )
        }
    }
}

@Composable
private fun RopeBoard(board: RopeMatchRound.Board, onMistake: (String) -> Unit, onComplete: () -> Unit) {
    val feedback = rememberGameFeedback()
    val scope = rememberCoroutineScope()
    val colors = GameTheme.colors
    val ropeColor = MaterialTheme.colorScheme.primary
    val tiedColor = colors.correct

    var origin by remember { mutableStateOf(Offset.Zero) }
    val termAnchors = remember { mutableStateMapOf<String, Offset>() }
    val definitionAnchors = remember { mutableStateMapOf<String, Offset>() }
    val definitionBounds = remember { mutableStateMapOf<String, Rect>() }
    var connected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedTerm by rememberSaveable { mutableStateOf<String?>(null) }
    var wrongDefinition by remember { mutableStateOf<String?>(null) }
    var shakeTerm by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // The loose end of the rope being dragged (root coordinates) and the term it belongs to.
    var draggingTerm by remember { mutableStateOf<String?>(null) }
    val looseEnd = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    fun attempt(termId: String, definitionId: String?): Boolean {
        if (definitionId == null) return false
        return if (termId == definitionId) {
            connected = connected + termId
            feedback.right()
            true
        } else {
            onMistake(termId)
            wrongDefinition = definitionId
            shakeTerm = termId to ((shakeTerm?.second ?: 0) + 1)
            feedback.wrong()
            false
        }
    }

    LaunchedEffect(connected.size) {
        if (connected.size == board.terms.size) {
            delay(700)
            onComplete()
        }
    }
    LaunchedEffect(wrongDefinition) {
        if (wrongDefinition != null) {
            delay(600)
            wrongDefinition = null
        }
    }

    Box(Modifier.fillMaxWidth().onGloballyPositioned { origin = it.positionInRoot() }) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                board.terms.forEach { item ->
                    val isConnected = item.conceptId in connected
                    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }
                    Surface(
                        onClick = { if (!isConnected) selectedTerm = if (selectedTerm == item.conceptId) null else item.conceptId },
                        enabled = !isConnected,
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isConnected -> colors.correctContainer
                            selectedTerm == item.conceptId || draggingTerm == item.conceptId -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        },
                        border = BorderStroke(2.dp, if (isConnected) tiedColor else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .shakeOn(shakeTerm?.takeIf { it.first == item.conceptId }?.second ?: 0)
                            .onGloballyPositioned { coordinates ->
                                cardTopLeft = coordinates.positionInRoot()
                                val bounds = coordinates.boundsInRoot()
                                termAnchors[item.conceptId] = Offset(bounds.right, bounds.center.y)
                            }
                            .pointerInput(item.conceptId, isConnected) {
                                if (isConnected) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { local ->
                                        draggingTerm = item.conceptId
                                        selectedTerm = null
                                        scope.launch { looseEnd.snapTo(cardTopLeft + local) }
                                    },
                                    onDragEnd = {
                                        val drop = looseEnd.value
                                        val target = definitionBounds.entries.firstOrNull { (id, rect) ->
                                            id !in connected && rect.inflate(12f).contains(drop)
                                        }?.key
                                        val tied = attempt(item.conceptId, target)
                                        scope.launch {
                                            if (!tied) {
                                                // Snap the rope back to its knot like a stretched elastic.
                                                looseEnd.animateTo(
                                                    termAnchors[item.conceptId] ?: drop,
                                                    spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                                                )
                                            }
                                            draggingTerm = null
                                        }
                                    },
                                    onDragCancel = { draggingTerm = null },
                                ) { change, amount ->
                                    change.consume()
                                    scope.launch { looseEnd.snapTo(looseEnd.value + amount) }
                                }
                            }
                            .testTag("rope_term_${item.conceptId}"),
                    ) {
                        Box(Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp), contentAlignment = Alignment.CenterStart) {
                            Text(item.text, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            Box(Modifier.width(36.dp))
            Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                board.definitions.forEach { item ->
                    val isConnected = item.conceptId in connected
                    Surface(
                        onClick = {
                            val term = selectedTerm ?: return@Surface
                            if (attempt(term, item.conceptId)) selectedTerm = null
                        },
                        enabled = !isConnected,
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isConnected -> colors.correctContainer
                            wrongDefinition == item.conceptId -> colors.wrongContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            2.dp,
                            when {
                                isConnected -> tiedColor
                                wrongDefinition == item.conceptId -> colors.wrong
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInRoot()
                                definitionBounds[item.conceptId] = bounds
                                definitionAnchors[item.conceptId] = Offset(bounds.left, bounds.center.y)
                            }
                            .testTag("rope_definition_${item.conceptId}"),
                    ) {
                        Box(Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp), contentAlignment = Alignment.CenterStart) {
                            Text(item.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Ropes are drawn above the cards but never intercept touches.
        Canvas(Modifier.matchParentSize()) {
            connected.forEach { id ->
                val from = termAnchors[id] ?: return@forEach
                val to = definitionAnchors[id] ?: return@forEach
                drawRope(from - origin, to - origin, tiedColor, sagFactor = 0.08f)
            }
            board.terms.forEach { item ->
                val anchor = termAnchors[item.conceptId] ?: return@forEach
                drawKnot(anchor - origin, if (item.conceptId in connected) tiedColor else ropeColor)
            }
            board.definitions.forEach { item ->
                val anchor = definitionAnchors[item.conceptId] ?: return@forEach
                drawKnot(anchor - origin, if (item.conceptId in connected) tiedColor else Color.Gray.copy(alpha = 0.6f))
            }
            val dragging = draggingTerm
            if (dragging != null) {
                val from = termAnchors[dragging]
                if (from != null) drawRope(from - origin, looseEnd.value - origin, ropeColor, sagFactor = 0.3f)
            }
        }
    }
}

/** A rope hanging between two points: sag grows with length, with a twisted-fibre dash on top. */
private fun DrawScope.drawRope(from: Offset, to: Offset, color: Color, sagFactor: Float) {
    val length = hypot(to.x - from.x, to.y - from.y)
    val sag = (length * sagFactor).coerceAtMost(48.dp.toPx())
    val control = Offset((from.x + to.x) / 2, (from.y + to.y) / 2 + sag)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(control.x, control.y, to.x, to.y)
    }
    val width = 5.dp.toPx()
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
    // Diagonal dashes on top read as twisted rope fibres.
    val fibre = 2.dp.toPx()
    drawPath(
        path,
        Color.White.copy(alpha = 0.4f),
        style = Stroke(width = fibre, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(fibre * 3, fibre * 3))),
    )
    drawCircle(color, radius = 6.dp.toPx(), center = to)
}

private fun DrawScope.drawKnot(center: Offset, color: Color) {
    drawCircle(color, radius = 7.dp.toPx(), center = center)
    drawCircle(Color.White, radius = 3.dp.toPx(), center = center)
}
