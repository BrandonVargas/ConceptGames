package dev.brandonvargas.conceptgames.engine.grid

import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

data class WordSearchWord(
    val letters: List<String>,
    val label: String,
    val conceptId: String,
)

data class PlacedWord(
    val word: WordSearchWord,
    val start: GridPos,
    val step: GridPos,
) {
    val cells: List<GridPos> get() = word.letters.indices.map { start + step * it }
}

data class WordSearchLayout(
    val size: Int,
    /** Row-major letters. */
    val grid: List<List<String>>,
    val words: List<PlacedWord>,
)

object WordSearchGenerator {
    val EASY_STEPS = listOf(GridPos(0, 1), GridPos(1, 0))
    val MEDIUM_STEPS = EASY_STEPS + listOf(GridPos(1, 1), GridPos(-1, 1))
    val ALL_STEPS = MEDIUM_STEPS + listOf(GridPos(0, -1), GridPos(-1, 0), GridPos(-1, -1), GridPos(1, -1))

    fun generate(
        words: List<WordSearchWord>,
        alphabet: List<String>,
        steps: List<GridPos>,
        random: Random,
        minWords: Int = 3,
    ): WordSearchLayout? {
        val candidates = words.distinctBy { it.letters }.sortedByDescending { it.letters.size }
        if (candidates.size < minWords || alphabet.isEmpty()) return null
        val longest = candidates.first().letters.size
        val letterCount = candidates.sumOf { it.letters.size }
        val size = maxOf(longest, ceil(sqrt(letterCount * 2.0)).toInt(), 8).coerceAtMost(14)
        val cells = HashMap<GridPos, String>()
        val placed = mutableListOf<PlacedWord>()
        for (word in candidates) {
            if (word.letters.size > size) continue
            val placement = place(word, size, steps, cells, random) ?: continue
            placed += placement
            placement.cells.forEachIndexed { i, pos -> cells[pos] = word.letters[i] }
        }
        if (placed.size < minWords) return null
        val grid = List(size) { row -> List(size) { col -> cells[GridPos(row, col)] ?: alphabet.random(random) } }
        return WordSearchLayout(size, grid, placed)
    }

    private fun place(
        word: WordSearchWord,
        size: Int,
        steps: List<GridPos>,
        cells: Map<GridPos, String>,
        random: Random,
    ): PlacedWord? {
        repeat(400) {
            val step = steps.random(random)
            val start = GridPos(random.nextInt(size), random.nextInt(size))
            val candidate = PlacedWord(word, start, step)
            val fits = candidate.cells.withIndex().all { (i, pos) ->
                pos.row in 0 until size && pos.col in 0 until size && (cells[pos] == null || cells[pos] == word.letters[i])
            }
            if (fits) return candidate
        }
        return null
    }
}
