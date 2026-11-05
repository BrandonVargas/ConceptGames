package dev.brandonvargas.conceptgames.engine.games.builders

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameBuilder
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.pickWeighted
import dev.brandonvargas.conceptgames.engine.model.Cloze
import dev.brandonvargas.conceptgames.engine.model.StudyPack

/** Flip a card to reveal the definition, then grade yourself. */
data class FlashcardsRound(val cards: List<Card>) : GameRound {
    override val gameId = GameId.Flashcards

    data class Card(val term: String, val definition: String, val conceptId: String)
}

object FlashcardsBuilder : GameBuilder<FlashcardsRound> {
    override val id = GameId.Flashcards

    override fun isAvailable(pack: StudyPack) = pack.concepts.size >= 3

    override fun build(pack: StudyPack, settings: BuildSettings) = FlashcardsRound(
        pack.concepts
            .pickWeighted(settings.byDifficulty(8, 12, 16), settings) { it.id }
            .map { FlashcardsRound.Card(it.term, it.definition, it.id) },
    )
}

/** Swipe statements right (true) or left (false). */
data class TrueFalseRound(val cards: List<Card>) : GameRound {
    override val gameId = GameId.TrueFalse

    data class Card(val statement: String, val isTrue: Boolean, val explanation: String, val conceptId: String?)
}

object TrueFalseBuilder : GameBuilder<TrueFalseRound> {
    override val id = GameId.TrueFalse

    override fun isAvailable(pack: StudyPack) = pack.facts.size >= 4

    override fun build(pack: StudyPack, settings: BuildSettings): TrueFalseRound {
        val count = settings.byDifficulty(6, 10, 12)
        // Keep both answers represented so guessing "true" every time doesn't win.
        val (trueFacts, falseFacts) = pack.facts.partition { it.isTrue }
        val half = count / 2
        val falseCount = minOf(falseFacts.size, maxOf(half, count - trueFacts.size))
        val picked = falseFacts.pickWeighted(falseCount, settings) { it.conceptId } +
            trueFacts.pickWeighted(count - falseCount, settings) { it.conceptId }
        return TrueFalseRound(
            picked.shuffled(settings.random).map { TrueFalseRound.Card(it.statement, it.isTrue, it.explanation, it.conceptId) },
        )
    }
}

/** Complete the sentence by choosing the missing word. */
data class FillBlankRound(val items: List<Item>) : GameRound {
    override val gameId = GameId.FillBlank

    data class Item(
        val before: String,
        val after: String,
        val options: List<String>,
        val correctIndex: Int,
        val conceptId: String?,
    ) {
        val answer: String get() = options[correctIndex]
    }
}

object FillBlankBuilder : GameBuilder<FillBlankRound> {
    override val id = GameId.FillBlank

    override fun isAvailable(pack: StudyPack) = pack.clozes.size >= 3

    override fun build(pack: StudyPack, settings: BuildSettings): FillBlankRound {
        val optionCount = settings.byDifficulty(3, 4, 4)
        val items = pack.clozes.pickWeighted(settings.byDifficulty(5, 8, 10), settings) { it.conceptId }.map { cloze ->
            val before = cloze.sentence.substringBefore(Cloze.BLANK)
            val after = cloze.sentence.substringAfter(Cloze.BLANK)
            val options = (cloze.distractors.shuffled(settings.random).take(optionCount - 1) + cloze.answer)
                .shuffled(settings.random)
            FillBlankRound.Item(before, after, options, options.indexOf(cloze.answer), cloze.conceptId)
        }
        return FillBlankRound(items)
    }
}

/** Flip two cards at a time to find term ↔ definition pairs. */
data class MemoryPairsRound(val cards: List<Card>) : GameRound {
    override val gameId = GameId.MemoryPairs

    data class Card(val index: Int, val conceptId: String, val text: String, val isTerm: Boolean)
}

object MemoryPairsBuilder : GameBuilder<MemoryPairsRound> {
    override val id = GameId.MemoryPairs
    private const val MAX_TEXT = 70

    override fun isAvailable(pack: StudyPack) = pack.concepts.size >= 4

    override fun build(pack: StudyPack, settings: BuildSettings): MemoryPairsRound {
        val concepts = pack.concepts.pickWeighted(settings.byDifficulty(4, 6, 8), settings) { it.id }
        val cards = concepts
            .flatMap { listOf(it.id to it.term, it.id to it.clue.take(MAX_TEXT)) }
            .mapIndexed { i, (conceptId, text) -> Triple(conceptId, text, i % 2 == 0) }
            .shuffled(settings.random)
            .mapIndexed { index, (conceptId, text, isTerm) -> MemoryPairsRound.Card(index, conceptId, text, isTerm) }
        return MemoryPairsRound(cards)
    }
}
