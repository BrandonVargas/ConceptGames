package dev.brandonvargas.conceptgames.engine.games.builders

import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameBuilder
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.maskedDefinition
import dev.brandonvargas.conceptgames.engine.games.pickWeighted
import dev.brandonvargas.conceptgames.engine.model.StudyPack

data class MultipleChoiceRound(val questions: List<Question>) : GameRound {
    override val gameId = GameId.MultipleChoice

    enum class Kind {
        /** A question written by the AI. */
        Authored,

        /** "Which term matches this definition?" built from the concept bank to top up short packs. */
        DefinitionToTerm,
    }

    data class Question(
        val kind: Kind,
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String,
        val conceptId: String?,
    )
}

object MultipleChoiceBuilder : GameBuilder<MultipleChoiceRound> {
    override val id = GameId.MultipleChoice

    override fun isAvailable(pack: StudyPack) = pack.questions.size >= 3 || pack.concepts.size >= 4

    override fun build(pack: StudyPack, settings: BuildSettings): MultipleChoiceRound {
        val random = settings.random
        val count = settings.byDifficulty(5, 8, 10)
        val optionCount = settings.byDifficulty(3, 4, 4)
        val authored = pack.questions.pickWeighted(count, settings) { it.conceptId }.map { mcq ->
            val options = (listOf(mcq.answer) + mcq.distractors.shuffled(random).take(optionCount - 1)).shuffled(random)
            MultipleChoiceRound.Question(
                MultipleChoiceRound.Kind.Authored,
                mcq.question,
                options,
                options.indexOf(mcq.answer),
                mcq.explanation,
                mcq.conceptId,
            )
        }
        val usedConcepts = authored.mapNotNull { it.conceptId }.toSet()
        val toppedUp = pack.concepts
            .filter { it.id !in usedConcepts }
            .pickWeighted(count - authored.size, settings) { it.id }
            .map { concept ->
                val others = (pack.concepts - concept).shuffled(random).take(optionCount - 1).map { it.term }
                val options = (others + concept.term).shuffled(random)
                MultipleChoiceRound.Question(
                    MultipleChoiceRound.Kind.DefinitionToTerm,
                    concept.maskedDefinition,
                    options,
                    options.indexOf(concept.term),
                    concept.definition,
                    concept.id,
                )
            }
        return MultipleChoiceRound((authored + toppedUp).shuffled(random))
    }
}
