package dev.brandonvargas.conceptgames.engine.games.builders

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameBuilder
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.gridConcepts
import dev.brandonvargas.conceptgames.engine.games.pickWeighted
import dev.brandonvargas.conceptgames.engine.grid.CrosswordGenerator
import dev.brandonvargas.conceptgames.engine.grid.CrosswordLayout
import dev.brandonvargas.conceptgames.engine.grid.CrosswordWord
import dev.brandonvargas.conceptgames.engine.grid.WordSearchGenerator
import dev.brandonvargas.conceptgames.engine.grid.WordSearchLayout
import dev.brandonvargas.conceptgames.engine.grid.WordSearchWord
import dev.brandonvargas.conceptgames.engine.model.Concept
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.text.Alphabets
import dev.brandonvargas.conceptgames.engine.text.Graphemes
import kotlin.random.Random

data class CrosswordRound(
    val layout: CrosswordLayout,
    /** Concept id → readable term, revealed after solving. */
    val terms: Map<String, String>,
    /** Right-to-left scripts fill cells from the right. */
    val rightToLeft: Boolean,
) : GameRound {
    override val gameId = GameId.Crossword
}

object CrosswordBuilder : GameBuilder<CrosswordRound> {
    override val id = GameId.Crossword
    private val rtlLanguages = setOf("ar", "arz", "fa", "he", "ur", "ps", "yi", "ckb", "sd", "ug", "dv")

    override fun isAvailable(pack: StudyPack): Boolean {
        val concepts = pack.gridConcepts()
        return concepts.size >= 4 && CrosswordGenerator.generate(concepts.toWords(), Random(7), attempts = 8) != null
    }

    override fun build(pack: StudyPack, settings: BuildSettings): CrosswordRound {
        val all = pack.gridConcepts()
        val picked = all.pickWeighted(settings.byDifficulty(6, 9, 12), settings) { it.id }
        val layout = CrosswordGenerator.generate(picked.toWords(), settings.random)
            ?: CrosswordGenerator.generate(all.toWords(), settings.random)
            ?: error("Crossword requested for a pack where isAvailable() is false")
        return CrosswordRound(
            layout = layout,
            terms = all.associate { it.id to it.term },
            rightToLeft = pack.language.substringBefore('-') in rtlLanguages,
        )
    }

    private fun List<Concept>.toWords() = map { CrosswordWord(Graphemes.split(it.gridWord!!), it.clue, it.id) }
}

data class WordSearchRound(val layout: WordSearchLayout) : GameRound {
    override val gameId = GameId.WordSearch
}

object WordSearchBuilder : GameBuilder<WordSearchRound> {
    override val id = GameId.WordSearch

    override fun isAvailable(pack: StudyPack) = pack.gridConcepts().size >= 4

    override fun build(pack: StudyPack, settings: BuildSettings): WordSearchRound {
        val all = pack.gridConcepts()
        val alphabet = Alphabets.forPack(pack.language, all.map { it.gridWord!! })
        val steps = settings.byDifficulty(
            WordSearchGenerator.EASY_STEPS,
            WordSearchGenerator.MEDIUM_STEPS,
            WordSearchGenerator.ALL_STEPS,
        )
        val picked = all.pickWeighted(settings.byDifficulty(6, 8, 10), settings) { it.id }
        val words = picked.map { WordSearchWord(Graphemes.split(it.gridWord!!), it.term, it.id) }
        val layout = WordSearchGenerator.generate(words, alphabet, steps, settings.random)
            ?: error("Word search could not place enough words")
        return WordSearchRound(layout)
    }
}
