package dev.brandonvargas.conceptgames.engine.validate

import dev.brandonvargas.conceptgames.engine.model.CategoryGroup
import dev.brandonvargas.conceptgames.engine.model.Cloze
import dev.brandonvargas.conceptgames.engine.model.Concept
import dev.brandonvargas.conceptgames.engine.model.Fact
import dev.brandonvargas.conceptgames.engine.model.Mcq
import dev.brandonvargas.conceptgames.engine.model.OddSet
import dev.brandonvargas.conceptgames.engine.model.PackDocument
import dev.brandonvargas.conceptgames.engine.model.RawPack
import dev.brandonvargas.conceptgames.engine.model.Source
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.model.Timeline
import dev.brandonvargas.conceptgames.engine.model.TimelineStep
import dev.brandonvargas.conceptgames.engine.text.WordNormalizer
import kotlinx.serialization.json.Json
import java.util.Locale

class InvalidPackException(message: String) : Exception(message)

/**
 * Cleans whatever the AI returned: trims text, drops malformed or duplicate items, links items to concepts
 * and derives grid words. Never trusts the model to follow the schema perfectly.
 */
object PackValidator {
    const val MIN_CONCEPTS = 4
    private const val MAX_CONCEPTS = 30
    private const val MAX_TEXT = 400

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun parse(rawJson: String): RawPack {
        val cleaned = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            json.decodeFromString(RawPack.serializer(), cleaned)
        } catch (e: IllegalArgumentException) {
            throw InvalidPackException("The AI answer was not valid JSON: ${e.message?.take(200)}")
        }
    }

    fun parseDocument(documentJson: String): PackDocument = json.decodeFromString(PackDocument.serializer(), documentJson)

    fun validate(document: PackDocument): StudyPack =
        validate(document.content, document.topic, document.language, document.sources)

    fun validate(raw: RawPack, topic: String, language: String, sources: List<Source>): StudyPack {
        val concepts = cleanConcepts(raw, language)
        if (concepts.size < MIN_CONCEPTS) {
            throw InvalidPackException("Only ${concepts.size} usable concepts were generated (need $MIN_CONCEPTS).")
        }
        val idByTerm = concepts.associate { it.term.key() to it.id }
        fun link(term: String?): String? = term?.key()?.let(idByTerm::get)

        val facts = raw.facts.orEmpty().mapNotNull { fact ->
            val statement = fact.statement.clean() ?: return@mapNotNull null
            val isTrue = fact.isTrue ?: return@mapNotNull null
            Fact(statement, isTrue, fact.explanation.clean().orEmpty(), link(fact.term))
        }.distinctBy { it.statement.key() }

        val questions = raw.questions.orEmpty().mapNotNull { mcq ->
            val question = mcq.question.clean() ?: return@mapNotNull null
            val answer = mcq.answer.clean() ?: return@mapNotNull null
            val distractors = cleanOptions(mcq.distractors, answer)
            if (distractors.size < 2) return@mapNotNull null
            Mcq(question, answer, distractors.take(3), mcq.explanation.clean().orEmpty(), link(mcq.term))
        }.distinctBy { it.question.key() }

        val clozes = raw.clozes.orEmpty().mapNotNull { cloze ->
            val answer = cloze.answer.clean() ?: return@mapNotNull null
            val sentence = blankOut(cloze.sentence.clean() ?: return@mapNotNull null, answer) ?: return@mapNotNull null
            val distractors = cleanOptions(cloze.distractors, answer)
            if (distractors.size < 2) return@mapNotNull null
            Cloze(sentence, answer, distractors.take(3), link(cloze.term) ?: link(answer))
        }.distinctBy { it.sentence.key() }

        val timelines = raw.timelines.orEmpty().mapNotNull { timeline ->
            val steps = timeline.steps.orEmpty()
                .mapNotNull { step -> step.label.clean()?.let { TimelineStep(it, step.detail.clean()) } }
                .distinctBy { it.label.key() }
                .take(8)
            if (steps.size < 3) return@mapNotNull null
            Timeline(timeline.title.clean() ?: topic, steps)
        }

        return StudyPack(
            topic = topic.trim(),
            language = language,
            summary = raw.summary.clean(limit = 1200).orEmpty(),
            concepts = concepts,
            facts = facts,
            questions = questions,
            clozes = clozes,
            timelines = timelines,
            categories = cleanCategories(raw),
            oddSets = cleanOddSets(raw),
            sources = sources,
        )
    }

    private fun cleanConcepts(raw: RawPack, language: String): List<Concept> {
        val seen = HashSet<String>()
        val seenGridWords = HashSet<String>()
        return raw.concepts.orEmpty().mapNotNull { concept ->
            val term = concept.term.clean(limit = 80) ?: return@mapNotNull null
            val definition = concept.definition.clean() ?: return@mapNotNull null
            if (!seen.add(term.key())) return@mapNotNull null
            val clue = maskTerm(concept.clue.clean() ?: definition, term)
            val gridWord = (concept.gridWord?.let { WordNormalizer.gridWord(it, language) })
                ?: WordNormalizer.gridWord(term, language)
            Concept(
                id = "",
                term = term,
                definition = definition,
                clue = clue,
                gridWord = gridWord?.takeIf(seenGridWords::add),
            )
        }.take(MAX_CONCEPTS).mapIndexed { index, concept -> concept.copy(id = "c$index") }
    }

    private fun cleanCategories(raw: RawPack): List<CategoryGroup> {
        val groups = raw.categories.orEmpty().mapNotNull { group ->
            val name = group.name.clean(limit = 60) ?: return@mapNotNull null
            CategoryGroup(name, group.members.orEmpty().mapNotNull { it.clean(limit = 80) }.distinctBy { it.key() })
        }.distinctBy { it.name.key() }
        // An item listed under two categories would be impossible to sort, so drop it everywhere.
        val counts = groups.flatMap { it.members }.groupingBy { it.key() }.eachCount()
        val cleaned = groups
            .map { group -> group.copy(members = group.members.filter { counts[it.key()] == 1 }.take(6)) }
            .filter { it.members.size >= 2 }
            .take(4)
        return if (cleaned.size >= 2) cleaned else emptyList()
    }

    private fun cleanOddSets(raw: RawPack): List<OddSet> = raw.oddOneOut.orEmpty().mapNotNull { set ->
        val items = set.items.orEmpty().mapNotNull { it.clean(limit = 80) }.distinctBy { it.key() }
        val odd = set.odd.clean(limit = 80) ?: return@mapNotNull null
        val oddItem = items.firstOrNull { it.key() == odd.key() } ?: return@mapNotNull null
        if (items.size !in 3..6) return@mapNotNull null
        OddSet(items, oddItem, set.reason.clean().orEmpty())
    }

    private fun cleanOptions(options: List<String>?, answer: String): List<String> = options.orEmpty()
        .mapNotNull { it.clean(limit = 120) }
        .filter { it.key() != answer.key() }
        .distinctBy { it.key() }

    /** Ensures the sentence has exactly one blank: keeps a provided blank or blanks out the answer itself. */
    internal fun blankOut(sentence: String, answer: String): String? {
        val normalizedBlank = sentence.replace(Regex("_{2,}|\\[blank\\]|\\{blank\\}", RegexOption.IGNORE_CASE), Cloze.BLANK)
        val blanks = Regex(Regex.escape(Cloze.BLANK)).findAll(normalizedBlank).count()
        if (blanks == 1) return normalizedBlank
        if (blanks > 1) return null
        val index = normalizedBlank.indexOf(answer, ignoreCase = true)
        if (index < 0) return null
        return normalizedBlank.replaceRange(index, index + answer.length, Cloze.BLANK)
    }

    /** Hides the answer inside its own clue, e.g. "Photosynthesis is how plants…" → "… is how plants…". */
    internal fun maskTerm(text: String, term: String): String =
        text.replace(Regex(Regex.escape(term), RegexOption.IGNORE_CASE), "…")

    private fun String?.clean(limit: Int = MAX_TEXT): String? = this
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.take(limit)
        ?.takeIf { it.isNotEmpty() }

    private fun String.key(): String = lowercase(Locale.ROOT).replace(Regex("[\\s\\p{Punct}]+"), " ").trim()
}
