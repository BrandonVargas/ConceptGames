package dev.brandonvargas.conceptgames.engine.games.builders

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameBuilder
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.gridConcepts
import dev.brandonvargas.conceptgames.engine.games.pickWeighted
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.text.Alphabets
import dev.brandonvargas.conceptgames.engine.text.Graphemes

/** Unscramble letter tiles into the term described by the clue. */
data class WordScrambleRound(val words: List<Word>) : GameRound {
    override val gameId = GameId.WordScramble

    data class Word(
        val answer: List<String>,
        val tiles: List<String>,
        val clue: String,
        val term: String,
        val conceptId: String,
    )
}

object WordScrambleBuilder : GameBuilder<WordScrambleRound> {
    override val id = GameId.WordScramble

    override fun isAvailable(pack: StudyPack) = pack.gridConcepts().count { scrambles(Graphemes.split(it.gridWord!!)) } >= 3

    override fun build(pack: StudyPack, settings: BuildSettings): WordScrambleRound {
        val count = settings.byDifficulty(4, 6, 8)
        val candidates = pack.gridConcepts().filter { scrambles(Graphemes.split(it.gridWord!!)) }
        val words = candidates.pickWeighted(count, settings) { it.id }.map { concept ->
            val answer = Graphemes.split(concept.gridWord!!)
            var tiles = answer.shuffled(settings.random)
            while (tiles == answer) tiles = answer.shuffled(settings.random)
            WordScrambleRound.Word(answer, tiles, concept.clue, concept.term, concept.id)
        }
        return WordScrambleRound(words)
    }

    /** A word made of a single repeated letter can't be scrambled. */
    private fun scrambles(letters: List<String>) = letters.distinct().size > 1
}

/** Guess the hidden term letter by letter before the balloon deflates. */
data class HangmanRound(val words: List<Word>, val maxMistakes: Int) : GameRound {
    override val gameId = GameId.Hangman

    data class Word(
        val answer: List<String>,
        val keyboard: List<String>,
        val clue: String,
        val term: String,
        val conceptId: String,
    )
}

object HangmanBuilder : GameBuilder<HangmanRound> {
    override val id = GameId.Hangman

    /** Scripts with huge alphabets (e.g. Chinese) get a keyboard of the word's letters plus decoys. */
    private const val MAX_KEYBOARD = 32

    override fun isAvailable(pack: StudyPack) = pack.gridConcepts().size >= 2

    override fun build(pack: StudyPack, settings: BuildSettings): HangmanRound {
        val random = settings.random
        val gridConcepts = pack.gridConcepts()
        val alphabet = Alphabets.forPack(pack.language, gridConcepts.map { it.gridWord!! })
        val words = gridConcepts.pickWeighted(settings.byDifficulty(3, 4, 5), settings) { it.id }.map { concept ->
            val answer = Graphemes.split(concept.gridWord!!)
            val keyboard = if (alphabet.size <= MAX_KEYBOARD) {
                alphabet
            } else {
                val decoys = (alphabet - answer.toSet()).shuffled(random).take(MAX_KEYBOARD - answer.distinct().size)
                (answer.distinct() + decoys).shuffled(random)
            }
            HangmanRound.Word(answer, keyboard, concept.clue, concept.term, concept.id)
        }
        return HangmanRound(words, maxMistakes = settings.byDifficulty(7, 6, 5))
    }
}
