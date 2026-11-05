package dev.brandonvargas.conceptgames.engine.games

import dev.brandonvargas.conceptgames.engine.model.Concept
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Stable ids: they are persisted with results, so never rename an entry. */
@Serializable
enum class GameId {
    MultipleChoice,
    RopeMatch,
    WordScramble,
    Crossword,
    Flashcards,
    TrueFalse,
    FillBlank,
    Timeline,
    WordSearch,
    Hangman,
    MemoryPairs,
    Categorize,
    OddOneOut,
}

@Serializable
enum class Difficulty { Easy, Medium, Hard }

/** Concepts the player keeps missing are picked more often (light spaced repetition). */
class ConceptWeights(private val missRates: Map<String, Double> = emptyMap()) {
    fun weight(conceptId: String?): Double = 1.0 + 3.0 * (conceptId?.let(missRates::get) ?: 0.0)

    companion object {
        val None = ConceptWeights()
    }
}

data class BuildSettings(
    val difficulty: Difficulty = Difficulty.Medium,
    val random: Random = Random.Default,
    val weights: ConceptWeights = ConceptWeights.None,
) {
    fun <T> byDifficulty(easy: T, medium: T, hard: T): T = when (difficulty) {
        Difficulty.Easy -> easy
        Difficulty.Medium -> medium
        Difficulty.Hard -> hard
    }
}

/** A ready-to-play game. Each game screen renders exactly one implementation. */
interface GameRound {
    val gameId: GameId
}

/**
 * Turns a [StudyPack] into a playable [GameRound]. To add a new minigame: implement this, register it in
 * [GameCatalog], and add a screen for its round type in the app's GameScreenRegistry.
 */
interface GameBuilder<out R : GameRound> {
    val id: GameId

    /** Whether the pack has enough material for this game. Must be cheap and deterministic. */
    fun isAvailable(pack: StudyPack): Boolean

    fun build(pack: StudyPack, settings: BuildSettings = BuildSettings()): R
}

@Serializable
data class GameResult(
    val gameId: GameId,
    val correct: Int,
    val total: Int,
    val missedConceptIds: List<String> = emptyList(),
    val seenConceptIds: List<String> = emptyList(),
    val durationMs: Long = 0,
) {
    val fraction: Float get() = if (total == 0) 0f else correct.toFloat() / total

    /** 0–3 stars. */
    val stars: Int get() = when {
        fraction >= 0.9f -> 3
        fraction >= 0.7f -> 2
        fraction >= 0.4f -> 1
        else -> 0
    }
}

/** Weighted sampling without replacement, so frequently missed concepts show up more. */
internal fun <T> List<T>.pickWeighted(count: Int, settings: BuildSettings, conceptId: (T) -> String?): List<T> {
    val pool = toMutableList()
    val picked = ArrayList<T>(minOf(count, size))
    while (picked.size < count && pool.isNotEmpty()) {
        val weights = pool.map { settings.weights.weight(conceptId(it)) }
        var roll = settings.random.nextDouble() * weights.sum()
        var index = 0
        while (index < pool.lastIndex && roll >= weights[index]) {
            roll -= weights[index]
            index++
        }
        picked += pool.removeAt(index)
    }
    return picked
}

/** Definition with the term hidden, so matching games don't give the answer away. */
val Concept.maskedDefinition: String
    get() = definition.replace(Regex(Regex.escape(term), RegexOption.IGNORE_CASE), "…")

internal fun StudyPack.gridConcepts(): List<Concept> = concepts.filter { it.gridWord != null }
