package dev.brandonvargas.conceptgames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameCatalog
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
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
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.validate.PackValidator
import dev.brandonvargas.conceptgames.ui.games.GameScreen
import dev.brandonvargas.conceptgames.ui.theme.ConceptGamesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Plays every minigame to the end with the right answers, on a real device, using the bundled sample pack.
 * This also checks that the engine (e.g. its regexes) behaves the same on Android as on the JVM.
 */
@RunWith(AndroidJUnit4::class)
class GamesPlaythroughTest {
    @get:Rule
    val compose = createComposeRule()

    private val pack: StudyPack by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val text = assets.open("sample_packs/photosynthesis_en.json").bufferedReader().use { it.readText() }
        PackValidator.validate(PackValidator.parseDocument(text))
    }

    private var result: GameResult? = null

    private inline fun <reified R : GameRound> start(id: GameId): R {
        val round = GameCatalog.build(id, pack, BuildSettings(Difficulty.Easy, Random(42)))
        compose.setContent { ConceptGamesTheme { GameScreen(round) { result = it } } }
        return round as R
    }

    private fun click(tag: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTagExists(tag) }
        val node = compose.onNodeWithTag(tag)
        // Pinned controls (e.g. the crossword keyboard) have no scrollable parent.
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun finished(): GameResult {
        compose.waitUntil(10_000) { result != null }
        return result!!
    }

    private fun assertPerfect(expectedTotal: Int? = null) {
        val result = finished()
        assertEquals("all answers right", result.total, result.correct)
        expectedTotal?.let { assertEquals(it, result.total) }
    }

    @Test
    fun multipleChoice() {
        val round = start<MultipleChoiceRound>(GameId.MultipleChoice)
        round.questions.forEach {
            click("option_${it.correctIndex}")
            click("continue")
        }
        assertPerfect(round.questions.size)
    }

    @Test
    fun trueFalse() {
        val round = start<TrueFalseRound>(GameId.TrueFalse)
        round.cards.forEach {
            click(if (it.isTrue) "tf_true" else "tf_false")
            click("continue")
        }
        assertPerfect(round.cards.size)
    }

    @Test
    fun fillBlank() {
        val round = start<FillBlankRound>(GameId.FillBlank)
        round.items.forEach {
            click("option_${it.correctIndex}")
            click("continue")
        }
        assertPerfect(round.items.size)
    }

    @Test
    fun oddOneOut() {
        val round = start<OddOneOutRound>(GameId.OddOneOut)
        round.sets.forEach {
            click("option_${it.oddIndex}")
            click("continue")
        }
        assertPerfect(round.sets.size)
    }

    @Test
    fun flashcards() {
        val round = start<FlashcardsRound>(GameId.Flashcards)
        round.cards.forEach { _ ->
            click("flashcard")
            click("fc_knew")
        }
        assertPerfect(round.cards.size)
    }

    @Test
    fun ropeMatchByTapping() {
        val round = start<RopeMatchRound>(GameId.RopeMatch)
        round.boards.forEach { board ->
            board.terms.forEach {
                click("rope_term_${it.conceptId}")
                click("rope_definition_${it.conceptId}")
            }
        }
        assertPerfect(round.boards.sumOf { it.terms.size })
    }

    @Test
    fun wordScramble() {
        val round = start<WordScrambleRound>(GameId.WordScramble)
        round.words.forEach { word ->
            val used = mutableSetOf<Int>()
            word.answer.forEach { letter ->
                val tile = word.tiles.indices.first { it !in used && word.tiles[it] == letter }
                used += tile
                click("tile_$tile")
            }
            click("continue")
        }
        assertPerfect(round.words.size)
    }

    @Test
    fun hangman() {
        val round = start<HangmanRound>(GameId.Hangman)
        round.words.forEach { word ->
            word.answer.distinct().forEach { click("key_$it") }
            click("continue")
        }
        assertPerfect(round.words.size)
    }

    @Test
    fun memoryPairs() {
        val round = start<MemoryPairsRound>(GameId.MemoryPairs)
        round.cards.groupBy { it.conceptId }.values.forEach { pair ->
            pair.forEach { click("memory_${it.index}") }
            compose.mainClock.advanceTimeBy(100)
        }
        assertPerfect(round.cards.size / 2)
    }

    @Test
    fun categorize() {
        val round = start<CategorizeRound>(GameId.Categorize)
        round.items.forEach { click("bucket_${it.categoryIndex}") }
        assertPerfect(round.items.size)
    }

    @Test
    fun timeline() {
        val round = start<TimelineRound>(GameId.Timeline)
        click("timeline_check")
        click("continue")
        click("continue")
        val result = finished()
        assertEquals(round.steps.size, result.total)
        assertEquals(round.startOrder.withIndex().count { (position, step) -> position == step }, result.correct)
    }

    @Test
    fun crossword() {
        val round = start<CrosswordRound>(GameId.Crossword)
        round.layout.solution.forEach { (pos, letter) ->
            click("cell_${pos.row}_${pos.col}")
            click("key_$letter")
        }
        assertPerfect(round.layout.entries.size)
    }

    @Test
    fun wordSearchBySwiping() {
        val round = start<WordSearchRound>(GameId.WordSearch)
        val size = round.layout.size
        round.layout.words.forEach { placed ->
            compose.onNodeWithTag("wordsearch_grid").performScrollTo().performTouchInput {
                val cell = width.toFloat() / size
                fun center(row: Int, col: Int) = Offset((col + 0.5f) * cell, (row + 0.5f) * cell)
                val first = placed.cells.first()
                val last = placed.cells.last()
                swipe(center(first.row, first.col), center(last.row, last.col), durationMillis = 400)
            }
            compose.waitForIdle()
        }
        val result = finished()
        assertEquals(round.layout.words.size, result.correct)
        assertTrue(result.missedConceptIds.isEmpty())
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagExists(tag: String) =
    onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
