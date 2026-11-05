package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.WordSearchRound
import dev.brandonvargas.conceptgames.engine.grid.GridPos
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/** Swipe across letters (or tap the first and last letter) to circle a hidden term. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchGame(round: WordSearchRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    val layout = round.layout
    val size = layout.size
    var found by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var gaveUp by rememberSaveable { mutableStateOf(false) }
    var anchor by remember { mutableStateOf<GridPos?>(null) }
    var selection by remember { mutableStateOf<List<GridPos>>(emptyList()) }
    var misses by remember { mutableIntStateOf(0) }
    val highlightColors = GameTheme.colors.accents.map { it.first }
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)

    fun evaluate(cells: List<GridPos>) {
        val match = layout.words.indices.firstOrNull { index ->
            index !in found && (layout.words[index].cells == cells || layout.words[index].cells == cells.reversed())
        }
        if (match != null) {
            found = found + match
            feedback.right()
        } else if (cells.size > 1) {
            misses++
            feedback.wrong()
        }
        selection = emptyList()
        anchor = null
    }

    val done = found.size == layout.words.size || gaveUp
    LaunchedEffect(done) {
        if (done) {
            delay(if (gaveUp) 2_000 else 800)
            val missed = layout.words.indices.filter { it !in found }.map { layout.words[it].word.conceptId }
            onFinish(GameResult(GameId.WordSearch, found.size, layout.words.size, missed, durationMs = elapsedSince(start)))
        }
    }

    GameColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wordsearch_found, found.size, layout.words.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cell = min(maxWidth / size, 40.dp)
            val cellPx = with(LocalDensity.current) { cell.toPx() }
            fun cellAt(offset: Offset) = GridPos(
                (offset.y / cellPx).toInt().coerceIn(0, size - 1),
                (offset.x / cellPx).toInt().coerceIn(0, size - 1),
            )
            fun center(pos: GridPos) = Offset(pos.col * cellPx + cellPx / 2, pos.row * cellPx + cellPx / 2)

            Box(
                Modifier
                    .size(cell * size)
                    .pointerInput(done) {
                        if (done) return@pointerInput
                        detectTapGestures { offset ->
                            val pos = cellAt(offset)
                            val first = anchor
                            if (first == null) {
                                anchor = pos
                                selection = listOf(pos)
                            } else {
                                evaluate(straightLine(first, pos, size))
                            }
                        }
                    }
                    .pointerInput(done) {
                        if (done) return@pointerInput
                        var from = GridPos(0, 0)
                        detectDragGestures(
                            onDragStart = { offset ->
                                from = cellAt(offset)
                                anchor = null
                                selection = listOf(from)
                            },
                            onDragEnd = { evaluate(selection) },
                            onDragCancel = { selection = emptyList() },
                        ) { change, _ ->
                            change.consume()
                            selection = straightLine(from, cellAt(change.position), size)
                        }
                    }
                    .testTag("wordsearch_grid"),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val shown = if (gaveUp) layout.words.indices.toList() else found
                    shown.forEach { index ->
                        val cells = layout.words[index].cells
                        val color = if (index in found) highlightColors[index % highlightColors.size] else Color.Red.copy(alpha = 0.25f)
                        drawLine(color, center(cells.first()), center(cells.last()), strokeWidth = cellPx * 0.8f, cap = StrokeCap.Round)
                    }
                    if (selection.isNotEmpty()) {
                        drawLine(selectionColor, center(selection.first()), center(selection.last()), strokeWidth = cellPx * 0.8f, cap = StrokeCap.Round)
                    }
                }
                Column {
                    layout.grid.forEach { row ->
                        Row {
                            row.forEach { letter ->
                                Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                                    Text(letter, fontSize = (cell.value * 0.5f).sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            layout.words.forEachIndexed { index, placed ->
                val isFound = index in found
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isFound) highlightColors[index % highlightColors.size] else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        placed.word.label,
                        style = MaterialTheme.typography.labelLarge,
                        textDecoration = if (isFound) TextDecoration.LineThrough else null,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        if (!done) {
            OutlinedButton(onClick = { gaveUp = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wordsearch_give_up)) }
        }
    }
}

/** Cells from [from] towards [to], snapped to the nearest of the 8 straight directions. */
internal fun straightLine(from: GridPos, to: GridPos, size: Int): List<GridPos> {
    var dr = to.row - from.row
    var dc = to.col - from.col
    when {
        dr == 0 && dc == 0 -> return listOf(from)
        abs(dr) > 2 * abs(dc) -> dc = 0
        abs(dc) > 2 * abs(dr) -> dr = 0
        else -> {
            val length = max(abs(dr), abs(dc))
            dr = dr.sign * length
            dc = dc.sign * length
        }
    }
    val steps = max(abs(dr), abs(dc))
    val step = GridPos(dr.sign, dc.sign)
    return (0..steps).map { GridPos(from.row + step.row * it, from.col + step.col * it) }
        .takeWhile { it.row in 0 until size && it.col in 0 until size }
}
