package dev.brandonvargas.conceptgames.engine.games.builders

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameBuilder
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.maskedDefinition
import dev.brandonvargas.conceptgames.engine.games.pickWeighted
import dev.brandonvargas.conceptgames.engine.model.StudyPack

/** Tie each term to its definition with a rope. Items match when their concept ids are equal. */
data class RopeMatchRound(val boards: List<Board>) : GameRound {
    override val gameId = GameId.RopeMatch

    data class Board(val terms: List<Item>, val definitions: List<Item>)

    data class Item(val conceptId: String, val text: String)
}

object RopeMatchBuilder : GameBuilder<RopeMatchRound> {
    override val id = GameId.RopeMatch
    private const val MAX_TEXT = 110

    override fun isAvailable(pack: StudyPack) = pack.concepts.size >= 4

    override fun build(pack: StudyPack, settings: BuildSettings): RopeMatchRound {
        val random = settings.random
        val perBoard = settings.byDifficulty(4, 5, 6).coerceAtMost(pack.concepts.size)
        val boards = settings.byDifficulty(2, 2, 3)
        val picked = pack.concepts.pickWeighted(perBoard * boards, settings) { it.id }
        val chunks = picked.chunked(perBoard).filter { it.size >= 3 }
        return RopeMatchRound(
            chunks.map { chunk ->
                val terms = chunk.map { RopeMatchRound.Item(it.id, it.term) }.shuffled(random)
                val definitions = chunk.map {
                    val text = it.maskedDefinition.takeIf { definition -> definition.length <= MAX_TEXT } ?: it.clue
                    RopeMatchRound.Item(it.id, text.take(MAX_TEXT))
                }
                RopeMatchRound.Board(terms, crossShuffle(terms, definitions, random))
            },
        )
    }

    /** Shuffles definitions so (almost) none sits straight across from its own term. */
    private fun crossShuffle(
        terms: List<RopeMatchRound.Item>,
        definitions: List<RopeMatchRound.Item>,
        random: kotlin.random.Random,
    ): List<RopeMatchRound.Item> {
        fun aligned(order: List<RopeMatchRound.Item>) = order.indices.count { order[it].conceptId == terms[it].conceptId }
        var best = definitions.shuffled(random)
        repeat(30) {
            if (aligned(best) == 0) return best
            val candidate = definitions.shuffled(random)
            if (aligned(candidate) < aligned(best)) best = candidate
        }
        return best
    }
}
