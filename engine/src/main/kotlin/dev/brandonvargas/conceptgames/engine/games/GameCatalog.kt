package dev.brandonvargas.conceptgames.engine.games

import dev.brandonvargas.conceptgames.engine.games.builders.CategorizeBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.CrosswordBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.CrosswordRound
import dev.brandonvargas.conceptgames.engine.games.builders.FillBlankBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.FillBlankRound
import dev.brandonvargas.conceptgames.engine.games.builders.FlashcardsBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.FlashcardsRound
import dev.brandonvargas.conceptgames.engine.games.builders.HangmanBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.HangmanRound
import dev.brandonvargas.conceptgames.engine.games.builders.MemoryPairsBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.MemoryPairsRound
import dev.brandonvargas.conceptgames.engine.games.builders.MultipleChoiceBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.MultipleChoiceRound
import dev.brandonvargas.conceptgames.engine.games.builders.OddOneOutBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.RopeMatchBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.RopeMatchRound
import dev.brandonvargas.conceptgames.engine.games.builders.TimelineBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.TrueFalseBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.TrueFalseRound
import dev.brandonvargas.conceptgames.engine.games.builders.WordScrambleBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.WordScrambleRound
import dev.brandonvargas.conceptgames.engine.games.builders.WordSearchBuilder
import dev.brandonvargas.conceptgames.engine.games.builders.WordSearchRound
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import kotlin.random.Random

/** Every minigame, in the order shown in the Study Hub. */
object GameCatalog {
    val builders: List<GameBuilder<GameRound>> = listOf(
        MultipleChoiceBuilder,
        RopeMatchBuilder,
        FlashcardsBuilder,
        TrueFalseBuilder,
        FillBlankBuilder,
        WordScrambleBuilder,
        CrosswordBuilder,
        WordSearchBuilder,
        HangmanBuilder,
        MemoryPairsBuilder,
        TimelineBuilder,
        CategorizeBuilder,
        OddOneOutBuilder,
    )

    init {
        check(builders.map { it.id }.toSet() == GameId.entries.toSet()) { "Every GameId needs exactly one builder" }
    }

    fun builder(id: GameId): GameBuilder<GameRound> = builders.first { it.id == id }

    fun available(pack: StudyPack): List<GameId> = builders.filter { it.isAvailable(pack) }.map { it.id }

    fun build(id: GameId, pack: StudyPack, settings: BuildSettings = BuildSettings()): GameRound =
        builder(id).build(pack, settings)

    /** A varied playlist for "Mixed challenge": different game styles back to back. */
    fun mixedPlaylist(pack: StudyPack, random: Random, size: Int = 5): List<GameId> =
        available(pack).filter { it != GameId.Flashcards }.shuffled(random).take(size)
}

/** Concepts practised by a round, used to track how often each one is seen versus missed. */
fun GameRound.conceptIds(): Set<String> = when (this) {
    is MultipleChoiceRound -> questions.mapNotNull { it.conceptId }
    is RopeMatchRound -> boards.flatMap { board -> board.terms.map { it.conceptId } }
    is WordScrambleRound -> words.map { it.conceptId }
    is HangmanRound -> words.map { it.conceptId }
    is CrosswordRound -> layout.entries.map { it.conceptId }
    is WordSearchRound -> layout.words.map { it.word.conceptId }
    is FlashcardsRound -> cards.map { it.conceptId }
    is TrueFalseRound -> cards.mapNotNull { it.conceptId }
    is FillBlankRound -> items.mapNotNull { it.conceptId }
    is MemoryPairsRound -> cards.map { it.conceptId }
    else -> emptyList()
}.toSet()
