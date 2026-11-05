package dev.brandonvargas.conceptgames.engine

import dev.brandonvargas.conceptgames.engine.model.RawConcept
import dev.brandonvargas.conceptgames.engine.model.RawMcq
import dev.brandonvargas.conceptgames.engine.model.RawPack
import dev.brandonvargas.conceptgames.engine.validate.InvalidPackException
import dev.brandonvargas.conceptgames.engine.validate.PackValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackValidatorTest {
    private val concepts = listOf("Alpha", "Beta", "Gamma", "Delta").map {
        RawConcept(term = it, definition = "$it is a Greek letter.", clue = null)
    }

    @Test
    fun `sample packs validate with rich content`() {
        Fixtures.packs.take(2).forEach { pack ->
            assertTrue(pack.concepts.size >= 10, pack.topic)
            assertTrue(pack.questions.isNotEmpty() && pack.facts.isNotEmpty() && pack.clozes.isNotEmpty(), pack.topic)
            assertEquals(pack.concepts.size, pack.concepts.map { it.id }.toSet().size)
        }
    }

    @Test
    fun `malformed json is reported`() {
        assertFailsWith<InvalidPackException> { PackValidator.parse("{\"concepts\": [") }
    }

    @Test
    fun `tolerates markdown fences and unknown fields`() {
        val raw = PackValidator.parse("```json\n{\"summary\":\"s\",\"extra\":1,\"concepts\":[{\"term\":\"A\"}]}\n```")
        assertEquals("s", raw.summary)
    }

    @Test
    fun `too few concepts fails`() {
        assertFailsWith<InvalidPackException> {
            PackValidator.validate(RawPack(concepts = concepts.take(2)), "Greek", "en", emptyList())
        }
    }

    @Test
    fun `clues never contain the answer`() {
        val pack = PackValidator.validate(RawPack(concepts = concepts), "Greek", "en", emptyList())
        assertEquals("… is a Greek letter.", pack.concepts.first().clue)
    }

    @Test
    fun `duplicate concepts and bad questions are dropped`() {
        val raw = RawPack(
            concepts = concepts + RawConcept("alpha", "dup", null),
            questions = listOf(
                RawMcq("Q1", "Alpha", listOf("alpha", "Beta", "Gamma"), "", "Alpha"),
                RawMcq("Q2", "Alpha", listOf("ALPHA"), "", null),
            ),
        )
        val pack = PackValidator.validate(raw, "Greek", "en", emptyList())
        assertEquals(4, pack.concepts.size)
        assertEquals(1, pack.questions.size)
        assertEquals(listOf("Beta", "Gamma"), pack.questions.single().distractors)
        assertEquals("c0", pack.questions.single().conceptId)
    }

    @Test
    fun `cloze sentences get exactly one blank`() {
        assertEquals("The ___ is hot.", PackValidator.blankOut("The Sun is hot.", "sun"))
        assertEquals("The ___ is hot.", PackValidator.blankOut("The [blank] is hot.", "Sun"))
        assertNull(PackValidator.blankOut("No answer here.", "Sun"))
        assertNull(PackValidator.blankOut("___ and ___", "Sun"))
    }
}
