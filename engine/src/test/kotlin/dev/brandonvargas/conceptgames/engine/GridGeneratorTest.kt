package dev.brandonvargas.conceptgames.engine

import dev.brandonvargas.conceptgames.engine.grid.CrosswordGenerator
import dev.brandonvargas.conceptgames.engine.grid.CrosswordLayout
import dev.brandonvargas.conceptgames.engine.grid.CrosswordWord
import dev.brandonvargas.conceptgames.engine.grid.Direction
import dev.brandonvargas.conceptgames.engine.grid.GridPos
import dev.brandonvargas.conceptgames.engine.grid.WordSearchGenerator
import dev.brandonvargas.conceptgames.engine.grid.WordSearchWord
import dev.brandonvargas.conceptgames.engine.text.Alphabets
import dev.brandonvargas.conceptgames.engine.text.Graphemes
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GridGeneratorTest {
    @Test
    fun `crosswords are consistent, connected and have no accidental words`() {
        // The Chinese fixture shares too few characters to cross 3 words; the builder then hides the game.
        for (pack in Fixtures.packs.filter { it.language != "zh" }) {
            val words = pack.concepts.mapNotNull { concept ->
                concept.gridWord?.let { CrosswordWord(Graphemes.split(it), concept.clue, concept.id) }
            }
            repeat(10) { seed ->
                val layout = assertNotNull(CrosswordGenerator.generate(words, Random(seed)), "${pack.topic} seed $seed")
                assertValid(layout)
            }
        }
    }

    @Test
    fun `crossword generation is fast enough for the UI`() {
        val pack = Fixtures.pack("en")
        val words = pack.concepts.mapNotNull { concept ->
            concept.gridWord?.let { CrosswordWord(Graphemes.split(it), concept.clue, concept.id) }
        }
        repeat(5) { CrosswordGenerator.generate(words, Random(it)) } // warm up the JIT
        val started = System.nanoTime()
        repeat(20) { CrosswordGenerator.generate(words, Random(it)) }
        val perLayoutMs = (System.nanoTime() - started) / 20 / 1_000_000
        assertTrue(perLayoutMs < 50, "took ${perLayoutMs}ms per layout")
    }

    private fun assertValid(layout: CrosswordLayout) {
        val solution = layout.solution
        // Every letter of every entry agrees with the shared grid and stays inside it.
        for (entry in layout.entries) {
            entry.cells.forEachIndexed { i, cell ->
                assertEquals(entry.answer[i], solution[cell])
                assertTrue(cell.row in 0 until layout.rows && cell.col in 0 until layout.cols)
            }
        }
        // Every maximal run of 2+ letters is exactly one entry, so no unintended words appear.
        for (direction in Direction.entries) {
            val runs = mutableSetOf<Pair<GridPos, Int>>()
            for (cell in solution.keys) {
                if (solution.containsKey(cell - direction.step)) continue
                var length = 1
                while (solution.containsKey(cell + direction.step * length)) length++
                if (length >= 2) runs += cell to length
            }
            val entries = layout.entries.filter { it.direction == direction }.map { it.start to it.answer.size }.toSet()
            assertEquals(runs, entries, "runs in $direction")
        }
        // All entries are connected through shared cells.
        val visited = mutableSetOf(layout.entries.first())
        var frontier = listOf(layout.entries.first())
        while (frontier.isNotEmpty()) {
            frontier = frontier.flatMap { entry ->
                layout.entries.filter { other -> other !in visited && other.cells.any { it in entry.cells } }
            }.distinct()
            visited += frontier
        }
        assertEquals(layout.entries.size, visited.size, "connected")
        // Numbering: one number per start cell, in reading order.
        val starts = layout.entries.associate { it.start to it.number }
        assertEquals((1..starts.size).toList(), starts.entries.sortedWith(compareBy({ it.key.row }, { it.key.col })).map { it.value })
    }

    @Test
    fun `word search hides every word along its path`() {
        for (pack in Fixtures.packs) {
            val gridConcepts = pack.concepts.filter { it.gridWord != null }
            val words = gridConcepts.map { WordSearchWord(Graphemes.split(it.gridWord!!), it.term, it.id) }
            val alphabet = Alphabets.forPack(pack.language, gridConcepts.map { it.gridWord!! })
            repeat(10) { seed ->
                val layout = assertNotNull(
                    WordSearchGenerator.generate(words, alphabet, WordSearchGenerator.ALL_STEPS, Random(seed)),
                )
                assertTrue(layout.words.size >= 3)
                for (placed in layout.words) {
                    val read = placed.cells.map { layout.grid[it.row][it.col] }
                    assertEquals(placed.word.letters, read)
                }
                assertTrue(layout.grid.all { row -> row.size == layout.size })
            }
        }
    }
}
