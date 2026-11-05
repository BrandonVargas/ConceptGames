package dev.brandonvargas.conceptgames.engine.model

import kotlinx.serialization.Serializable

/**
 * Everything the app knows about a topic after research. One pack is generated per topic with a single AI call;
 * every minigame is then built locally from it, so replays cost no API quota.
 */
@Serializable
data class StudyPack(
    val topic: String,
    /** Wikipedia language code, e.g. "en", "es", "zh". Games and grid words follow this language. */
    val language: String,
    val summary: String,
    val concepts: List<Concept>,
    val facts: List<Fact> = emptyList(),
    val questions: List<Mcq> = emptyList(),
    val clozes: List<Cloze> = emptyList(),
    val timelines: List<Timeline> = emptyList(),
    val categories: List<CategoryGroup> = emptyList(),
    val oddSets: List<OddSet> = emptyList(),
    val sources: List<Source> = emptyList(),
) {
    fun concept(id: String?): Concept? = id?.let { wanted -> concepts.firstOrNull { it.id == wanted } }
}

/** A key term of the topic. */
@Serializable
data class Concept(
    val id: String,
    val term: String,
    val definition: String,
    /** Short crossword-style clue (definition rephrased, never containing the term). */
    val clue: String,
    /** Upper-cased letters usable in word games (crossword, word search...), or null when the term can't be one. */
    val gridWord: String? = null,
)

@Serializable
data class Fact(
    val statement: String,
    val isTrue: Boolean,
    val explanation: String,
    val conceptId: String? = null,
)

/** Multiple-choice question with exactly one right answer. */
@Serializable
data class Mcq(
    val question: String,
    val answer: String,
    val distractors: List<String>,
    val explanation: String,
    val conceptId: String? = null,
)

/** Sentence with a blank, written as [BLANK]. */
@Serializable
data class Cloze(
    val sentence: String,
    val answer: String,
    val distractors: List<String>,
    val conceptId: String? = null,
) {
    companion object {
        const val BLANK = "___"
    }
}

/** Steps or events listed in their correct order. */
@Serializable
data class Timeline(
    val title: String,
    val steps: List<TimelineStep>,
)

@Serializable
data class TimelineStep(
    val label: String,
    /** Optional date or short hint shown after answering. */
    val detail: String? = null,
)

@Serializable
data class CategoryGroup(
    val name: String,
    val members: List<String>,
)

/** A set of items where exactly one doesn't belong. */
@Serializable
data class OddSet(
    val items: List<String>,
    val odd: String,
    val reason: String,
)

@Serializable
data class Source(
    val title: String,
    val url: String,
    val kind: SourceKind = SourceKind.Wikipedia,
)

@Serializable
enum class SourceKind { Wikipedia, Web }
