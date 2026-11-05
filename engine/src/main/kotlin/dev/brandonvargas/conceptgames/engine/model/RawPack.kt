package dev.brandonvargas.conceptgames.engine.model

import kotlinx.serialization.Serializable

/**
 * The shape the AI is asked to produce. Every field is optional so a partially malformed answer still parses;
 * [dev.brandonvargas.conceptgames.engine.validate.PackValidator] turns it into a clean [StudyPack].
 */
@Serializable
data class RawPack(
    val summary: String? = null,
    val concepts: List<RawConcept>? = null,
    val facts: List<RawFact>? = null,
    val questions: List<RawMcq>? = null,
    val clozes: List<RawCloze>? = null,
    val timelines: List<RawTimeline>? = null,
    val categories: List<RawCategory>? = null,
    val oddOneOut: List<RawOddSet>? = null,
)

@Serializable
data class RawConcept(
    val term: String? = null,
    val definition: String? = null,
    val clue: String? = null,
    val gridWord: String? = null,
)

@Serializable
data class RawFact(
    val statement: String? = null,
    val isTrue: Boolean? = null,
    val explanation: String? = null,
    val term: String? = null,
)

@Serializable
data class RawMcq(
    val question: String? = null,
    val answer: String? = null,
    val distractors: List<String>? = null,
    val explanation: String? = null,
    val term: String? = null,
)

@Serializable
data class RawCloze(
    val sentence: String? = null,
    val answer: String? = null,
    val distractors: List<String>? = null,
    val term: String? = null,
)

@Serializable
data class RawTimeline(
    val title: String? = null,
    val steps: List<RawTimelineStep>? = null,
)

@Serializable
data class RawTimelineStep(
    val label: String? = null,
    val detail: String? = null,
)

@Serializable
data class RawCategory(
    val name: String? = null,
    val members: List<String>? = null,
)

@Serializable
data class RawOddSet(
    val items: List<String>? = null,
    val odd: String? = null,
    val reason: String? = null,
)

/** Portable file format for a pack before validation: bundled samples, and later import/export. */
@Serializable
data class PackDocument(
    val topic: String,
    val language: String,
    val sources: List<Source> = emptyList(),
    val content: RawPack,
)
