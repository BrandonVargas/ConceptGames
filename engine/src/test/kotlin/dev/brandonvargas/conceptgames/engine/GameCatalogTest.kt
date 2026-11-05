package dev.brandonvargas.conceptgames.engine

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.ConceptWeights
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameCatalog
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.builders.CategorizeRound
import dev.brandonvargas.conceptgames.engine.games.builders.CrosswordRound
import dev.brandonvargas.conceptgames.engine.games.builders.FillBlankRound
import dev.brandonvargas.conceptgames.engine.games.builders.FlashcardsRound
import dev.brandonvargas.conceptgames.engine.games.builders.HangmanRound
import dev.brandonvargas.conceptgames.engine.games.builders.MemoryPairsRound
import dev.brandonvargas.conceptgames.engine.games.builders.MultipleChoiceRound
import dev.brandonvargas.conceptgames.engine.games.builders.OddOneOutRound
import dev.brandonvargas.conceptgames.engine.games.builders.RopeMatchRound
import dev.brandonvargas.conceptgames.engine.games.builders.TimelineRound
import dev.brandonvargas.conceptgames.engine.games.builders.TrueFalseRound
import dev.brandonvargas.conceptgames.engine.games.builders.WordScrambleRound
import dev.brandonvargas.conceptgames.engine.games.builders.WordSearchRound
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class GameCatalogTest {
    @Test
    fun `bundled samples unlock every game`() {
        listOf("en", "es").forEach { language ->
            assertEquals(GameId.entries.toSet(), GameCatalog.available(Fixtures.pack(language)).toSet(), language)
        }
    }

    @Test
    fun `sparse packs only offer games they can fill`() {
        val chinese = GameCatalog.available(Fixtures.pack("zh"))
        assertTrue(GameId.WordSearch in chinese && GameId.Hangman in chinese && GameId.RopeMatch in chinese)
        assertTrue(GameId.TrueFalse !in chinese && GameId.Timeline !in chinese && GameId.Crossword !in chinese)
        assertTrue(GameId.Crossword in GameCatalog.available(Fixtures.pack("ru")))
    }

    @Test
    fun `every available game builds valid rounds`() {
        for (pack in Fixtures.packs) {
            for (difficulty in Difficulty.entries) {
                repeat(8) { seed ->
                    val settings = BuildSettings(difficulty, Random(seed))
                    for (id in GameCatalog.available(pack)) {
                        val round = GameCatalog.build(id, pack, settings)
                        assertEquals(id, round.gameId)
                        check(round)
                    }
                }
            }
        }
    }

    @Test
    fun `missed concepts are picked more often`() {
        val pack = Fixtures.pack("en")
        val target = pack.concepts.last().id
        val weights = ConceptWeights(mapOf(target to 1.0))
        val withWeights = (0 until 200).count { seed ->
            val round = GameCatalog.build(GameId.Flashcards, pack, BuildSettings(Difficulty.Easy, Random(seed), weights))
            (round as FlashcardsRound).cards.any { it.conceptId == target }
        }
        val without = (0 until 200).count { seed ->
            val round = GameCatalog.build(GameId.Flashcards, pack, BuildSettings(Difficulty.Easy, Random(seed)))
            (round as FlashcardsRound).cards.any { it.conceptId == target }
        }
        assertTrue(withWeights > without, "weighted $withWeights vs plain $without")
    }

    private fun check(round: GameRound) {
        when (round) {
            is MultipleChoiceRound -> round.questions.forEach {
                assertTrue(it.correctIndex in it.options.indices)
                assertEquals(it.options.size, it.options.toSet().size)
            }
            is RopeMatchRound -> round.boards.forEach { board ->
                assertEquals(board.terms.map { it.conceptId }.toSet(), board.definitions.map { it.conceptId }.toSet())
                assertTrue(board.terms.indices.none { board.terms[it].conceptId == board.definitions[it].conceptId })
            }
            is WordScrambleRound -> round.words.forEach {
                assertNotEquals(it.answer, it.tiles)
                assertEquals(it.answer.sorted(), it.tiles.sorted())
            }
            is HangmanRound -> round.words.forEach { word -> assertTrue(word.keyboard.containsAll(word.answer)) }
            is CrosswordRound -> assertTrue(round.layout.entries.size >= 3)
            is WordSearchRound -> assertTrue(round.layout.words.size >= 3)
            is FlashcardsRound -> assertTrue(round.cards.isNotEmpty())
            is TrueFalseRound -> assertTrue(round.cards.any { it.isTrue } && round.cards.any { !it.isTrue })
            is FillBlankRound -> round.items.forEach { assertTrue(it.correctIndex in it.options.indices) }
            is TimelineRound -> {
                assertEquals(round.steps.indices.toSet(), round.startOrder.toSet())
                assertNotEquals(round.steps.indices.toList(), round.startOrder)
            }
            is MemoryPairsRound -> round.cards.groupBy { it.conceptId }.values.forEach { pair ->
                assertEquals(2, pair.size)
                assertEquals(1, pair.count { it.isTerm })
            }
            is CategorizeRound -> {
                assertTrue(round.items.all { it.categoryIndex in round.categories.indices })
                assertEquals(round.categories.indices.toSet(), round.items.map { it.categoryIndex }.toSet())
            }
            is OddOneOutRound -> round.sets.forEach { assertTrue(it.oddIndex in it.items.indices) }
            else -> fail("Unchecked round type ${round::class.simpleName}")
        }
    }
}
