package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.brandonvargas.conceptgames.R
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.builders.CrosswordRound
import dev.brandonvargas.conceptgames.engine.grid.CrosswordEntry
import dev.brandonvargas.conceptgames.engine.grid.Direction
import dev.brandonvargas.conceptgames.engine.grid.GridPos
import dev.brandonvargas.conceptgames.engine.text.Alphabets
import dev.brandonvargas.conceptgames.ui.theme.GameTheme
import kotlinx.coroutines.delay

@Composable
fun CrosswordGame(round: CrosswordRound, onFinish: (GameResult) -> Unit) {
    val start = rememberStartTime()
    val feedback = rememberGameFeedback()
    val layout = round.layout
    val solution = layout.solution
    val entries = layout.entries
    val keyboard = remember(round) { Alphabets.forPack("", entries.map { it.answer.joinToString("") }) }
    val cellIndex = { pos: GridPos -> pos.row * layout.cols + pos.col }

    // Saveable flat state: typed letter per cell ("" = empty).
    var letters by rememberSaveable { mutableStateOf(List(layout.rows * layout.cols) { "" }) }
    var selectedEntry by rememberSaveable { mutableIntStateOf(0) }
    var selectedCell by rememberSaveable { mutableIntStateOf(cellIndex(entries.first().start)) }
    var revealed by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    val entry = entries[selectedEntry]

    fun letterAt(pos: GridPos) = letters[cellIndex(pos)]
    fun entryCorrect(e: CrosswordEntry) = e.cells.withIndex().all { (i, cell) -> letterAt(cell) == e.answer[i] }
    fun score(): GameResult {
        val good = entries.indices.filter { it !in revealed && entryCorrect(entries[it]) }
        val missed = entries.indices.filter { it !in good }.map { entries[it].conceptId }
        return GameResult(GameId.Crossword, good.size, entries.size, missed, durationMs = elapsedSince(start))
    }

    fun select(pos: GridPos) {
        val index = cellIndex(pos)
        val candidates = entries.indices.filter { pos in entries[it].cells }
        if (candidates.isEmpty()) return
        // Tapping the selected cell again switches between across and down.
        selectedEntry = if (index == selectedCell && candidates.size > 1) {
            candidates.first { it != selectedEntry }
        } else {
            candidates.firstOrNull { entries[it].direction == entry.direction } ?: candidates.first()
        }
        selectedCell = index
    }

    fun type(letter: String) {
        checking = false
        letters = letters.toMutableList().also { it[selectedCell] = letter }
        val cells = entry.cells
        val position = cells.indexOfFirst { cellIndex(it) == selectedCell }
        if (position in 0 until cells.lastIndex) selectedCell = cellIndex(cells[position + 1])
    }

    fun erase() {
        checking = false
        val cells = entry.cells
        val position = cells.indexOfFirst { cellIndex(it) == selectedCell }
        if (letters[selectedCell].isEmpty() && position > 0) selectedCell = cellIndex(cells[position - 1])
        letters = letters.toMutableList().also { it[selectedCell] = "" }
    }

    fun revealCurrent() {
        letters = letters.toMutableList().also { list -> entry.cells.forEachIndexed { i, cell -> list[cellIndex(cell)] = entry.answer[i] } }
        if (selectedEntry !in revealed) revealed = revealed + selectedEntry
    }

    val allCorrect = solution.all { (pos, letter) -> letterAt(pos) == letter }
    LaunchedEffect(allCorrect) {
        if (allCorrect && !finished) {
            finished = true
            feedback.right()
            delay(900)
            onFinish(score())
        }
    }

    // Clue and keyboard stay pinned; only the grid and clue list scroll, so typing never hides the board.
    GameColumn(scrollable = false) {
        ClueBar(
            entry = entry,
            onPrevious = {
                selectedEntry = (selectedEntry - 1 + entries.size) % entries.size
                selectedCell = cellIndex(entries[selectedEntry].start)
            },
            onNext = {
                selectedEntry = (selectedEntry + 1) % entries.size
                selectedCell = cellIndex(entries[selectedEntry].start)
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides if (round.rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                CrosswordGrid(
                    round = round,
                    letterAt = ::letterAt,
                    selectedCell = selectedCell,
                    activeCells = entry.cells.map(cellIndex).toSet(),
                    checking = checking,
                    cellIndex = cellIndex,
                    onSelect = ::select,
                )
            }
            ClueList(
                entries = entries,
                selected = selectedEntry,
                solved = { entryCorrect(it) },
                onSelect = { index ->
                    selectedEntry = index
                    selectedCell = cellIndex(entries[index].start)
                },
            )
        }
        LetterKeyboard(
            letters = keyboard,
            stateOf = { KeyState.Unused },
            onKey = ::type,
            enabled = !finished,
            keySize = 34.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = ::erase) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = stringResource(R.string.crossword_erase))
            }
            OutlinedButton(onClick = { checking = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.crossword_check)) }
            OutlinedButton(onClick = ::revealCurrent, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.crossword_reveal)) }
            Button(
                onClick = {
                    finished = true
                    onFinish(score())
                },
                enabled = !finished,
                modifier = Modifier.weight(1f).testTag("crossword_finish"),
            ) { Text(stringResource(R.string.crossword_finish)) }
        }
    }
}

@Composable
private fun ClueBar(entry: CrosswordEntry, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.crossword_previous))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${entry.number} ${stringResource(if (entry.direction == Direction.Across) R.string.crossword_across else R.string.crossword_down)} · ${entry.answer.size}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(entry.clue, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.crossword_next))
            }
        }
    }
}

@Composable
private fun CrosswordGrid(
    round: CrosswordRound,
    letterAt: (GridPos) -> String,
    selectedCell: Int,
    activeCells: Set<Int>,
    checking: Boolean,
    cellIndex: (GridPos) -> Int,
    onSelect: (GridPos) -> Unit,
) {
    val layout = round.layout
    val numbers = remember(layout) { layout.entries.associate { it.start to it.number } }
    val colors = GameTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val cell = min(maxWidth / layout.cols, 40.dp)
        Column(Modifier.testTag("crossword_grid")) {
            repeat(layout.rows) { row ->
                Row {
                    repeat(layout.cols) { col ->
                        val pos = GridPos(row, col)
                        val solution = layout.solution[pos]
                        if (solution == null) {
                            Spacer(Modifier.size(cell))
                        } else {
                            val index = cellIndex(pos)
                            val typed = letterAt(pos)
                            val wrong = checking && typed.isNotEmpty() && typed != solution
                            Box(
                                Modifier
                                    .size(cell)
                                    .background(
                                        when {
                                            index == selectedCell -> MaterialTheme.colorScheme.primary
                                            wrong -> colors.wrongContainer
                                            index in activeCells -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainerLowest
                                        },
                                    )
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                    .clickable { onSelect(pos) }
                                    .testTag("cell_${row}_$col"),
                                contentAlignment = Alignment.Center,
                            ) {
                                numbers[pos]?.let {
                                    Text(
                                        it.toString(),
                                        fontSize = 8.sp,
                                        modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp),
                                        color = if (index == selectedCell) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    typed,
                                    fontSize = (cell.value * 0.5f).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (index == selectedCell) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClueList(entries: List<CrosswordEntry>, selected: Int, solved: (CrosswordEntry) -> Boolean, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Direction.entries.forEach { direction ->
            Text(
                stringResource(if (direction == Direction.Across) R.string.crossword_across else R.string.crossword_down),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            entries.withIndex().filter { it.value.direction == direction }.forEach { (index, entry) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (index == selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .clickable { onSelect(index) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${entry.number}.", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(32.dp))
                    Text(entry.clue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (solved(entry)) ResultIcon(true)
                }
            }
        }
    }
}
