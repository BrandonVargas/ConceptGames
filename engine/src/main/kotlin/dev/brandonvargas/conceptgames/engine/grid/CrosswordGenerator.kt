package dev.brandonvargas.conceptgames.engine.grid

import kotlin.random.Random

data class GridPos(val row: Int, val col: Int) {
    operator fun plus(step: GridPos) = GridPos(row + step.row, col + step.col)
    operator fun minus(step: GridPos) = GridPos(row - step.row, col - step.col)
    operator fun times(factor: Int) = GridPos(row * factor, col * factor)
}

enum class Direction(val step: GridPos) {
    Across(GridPos(0, 1)),
    Down(GridPos(1, 0)),
    ;

    val perpendicular: Direction get() = if (this == Across) Down else Across
}

data class CrosswordWord(
    /** One grapheme per cell. */
    val answer: List<String>,
    val clue: String,
    val conceptId: String,
)

data class CrosswordEntry(
    val number: Int,
    val start: GridPos,
    val direction: Direction,
    val answer: List<String>,
    val clue: String,
    val conceptId: String,
) {
    val cells: List<GridPos> get() = answer.indices.map { start + direction.step * it }
}

data class CrosswordLayout(
    val rows: Int,
    val cols: Int,
    val entries: List<CrosswordEntry>,
) {
    /** Solution letter per cell, or null for a black square. */
    val solution: Map<GridPos, String> by lazy {
        buildMap { this@CrosswordLayout.entries.forEach { entry -> entry.cells.forEachIndexed { i, cell -> put(cell, entry.answer[i]) } } }
    }

    val intersections: Int get() = entries.sumOf { it.answer.size } - solution.size
}

/**
 * Builds a compact, fully connected crossword from a word list. Each attempt places the words greedily (most
 * crossings wins); the best of several shuffled attempts is kept. Words that cannot cross anything are left out.
 */
object CrosswordGenerator {
    const val MIN_WORDS = 3
    private const val MIN_ATTEMPTS = 6

    fun generate(
        words: List<CrosswordWord>,
        random: Random,
        attempts: Int = 24,
        maxSize: Int = 15,
    ): CrosswordLayout? {
        val usable = words.filter { it.answer.size in 2..maxSize }.distinctBy { it.answer }
        if (usable.size < MIN_WORDS) return null
        var best: Attempt? = null
        for (index in 0 until attempts) {
            val order = if (index == 0) usable.sortedByDescending { it.answer.size } else shuffledLongFirst(usable, random)
            val attempt = Attempt(maxSize).apply { run(order, random) }
            if (best == null || attempt.score > best.score) best = attempt
            // Once a few attempts placed every word, further tries only shave a little area: stop early.
            if (index >= MIN_ATTEMPTS && best.placed.size == usable.size) break
        }
        return best?.takeIf { it.placed.size >= MIN_WORDS }?.toLayout()
    }

    private fun shuffledLongFirst(words: List<CrosswordWord>, random: Random): List<CrosswordWord> {
        val longest = words.maxBy { it.answer.size }
        return listOf(longest) + (words - longest).shuffled(random)
    }

    private data class Placement(val word: CrosswordWord, val start: GridPos, val direction: Direction)

    private class Attempt(private val maxSize: Int) {
        val placed = mutableListOf<Placement>()
        private val letters = HashMap<GridPos, String>()

        /** Cells holding each letter, so candidate crossings are found without scanning the whole grid. */
        private val cellsByLetter = HashMap<String, MutableList<GridPos>>()
        private val usedDirections = HashMap<GridPos, MutableSet<Direction>>()
        private var minRow = 0
        private var maxRow = 0
        private var minCol = 0
        private var maxCol = 0
        private var crossings = 0

        val score: Int
            get() {
                val area = (maxRow - minRow + 1) * (maxCol - minCol + 1)
                return placed.size * 10_000 + crossings * 100 - area
            }

        fun run(order: List<CrosswordWord>, random: Random) {
            val first = order.first()
            put(Placement(first, GridPos(0, 0), if (random.nextBoolean()) Direction.Across else Direction.Down), 0)
            val pending = order.drop(1).toMutableList()
            // Words skipped early may fit once more words are on the board, so keep sweeping while we make progress.
            var progress = true
            while (progress && pending.isNotEmpty()) {
                progress = false
                val iterator = pending.iterator()
                while (iterator.hasNext()) {
                    val word = iterator.next()
                    val candidate = bestPlacement(word, random) ?: continue
                    put(candidate.first, candidate.second)
                    iterator.remove()
                    progress = true
                }
            }
        }

        private fun bestPlacement(word: CrosswordWord, random: Random): Pair<Placement, Int>? {
            var best: Pair<Placement, Int>? = null
            var bestTieBreak = 0.0
            for ((index, letter) in word.answer.withIndex()) {
                for (pos in cellsByLetter[letter].orEmpty()) {
                    for (direction in Direction.entries) {
                        if (usedDirections[pos]?.contains(direction) == true) continue
                        val placement = Placement(word, pos - direction.step * index, direction)
                        val crossings = crossingsIfValid(placement) ?: continue
                        val tieBreak = random.nextDouble()
                        if (best == null || crossings > best.second || (crossings == best.second && tieBreak > bestTieBreak)) {
                            best = placement to crossings
                            bestTieBreak = tieBreak
                        }
                    }
                }
            }
            return best
        }

        /** Number of crossings if the word can go there, or null when it would clash or touch other words. */
        private fun crossingsIfValid(placement: Placement): Int? {
            val (word, start, direction) = placement
            val step = direction.step
            val side = direction.perpendicular.step
            if (letters.containsKey(start - step) || letters.containsKey(start + step * word.answer.size)) return null
            val end = start + step * (word.answer.size - 1)
            val rows = minOf(minRow, start.row) to maxOf(maxRow, end.row)
            val cols = minOf(minCol, start.col) to maxOf(maxCol, end.col)
            if (rows.second - rows.first + 1 > maxSize || cols.second - cols.first + 1 > maxSize) return null
            var crossings = 0
            for (i in word.answer.indices) {
                val pos = start + step * i
                val existing = letters[pos]
                if (existing != null) {
                    if (existing != word.answer[i] || usedDirections[pos]?.contains(direction) == true) return null
                    crossings++
                } else if (letters.containsKey(pos + side) || letters.containsKey(pos - side)) {
                    return null
                }
            }
            return crossings.takeIf { it > 0 }
        }

        private fun put(placement: Placement, newCrossings: Int) {
            placed += placement
            crossings += newCrossings
            placement.word.answer.forEachIndexed { i, letter ->
                val pos = placement.start + placement.direction.step * i
                if (letters.put(pos, letter) == null) cellsByLetter.getOrPut(letter) { mutableListOf() } += pos
                usedDirections.getOrPut(pos) { mutableSetOf() } += placement.direction
                minRow = minOf(minRow, pos.row)
                maxRow = maxOf(maxRow, pos.row)
                minCol = minOf(minCol, pos.col)
                maxCol = maxOf(maxCol, pos.col)
            }
        }

        fun toLayout(): CrosswordLayout {
            val offset = GridPos(minRow, minCol)
            val shifted = placed.map { it.copy(start = it.start - offset) }
            // Standard numbering: cells are numbered in reading order, one number per starting cell.
            val numbers = shifted.map { it.start }.distinct()
                .sortedWith(compareBy({ it.row }, { it.col }))
                .withIndex()
                .associate { (index, pos) -> pos to index + 1 }
            val entries = shifted
                .map { CrosswordEntry(numbers.getValue(it.start), it.start, it.direction, it.word.answer, it.word.clue, it.word.conceptId) }
                .sortedWith(compareBy({ it.direction }, { it.number }))
            return CrosswordLayout(rows = maxRow - minRow + 1, cols = maxCol - minCol + 1, entries = entries)
        }
    }
}
