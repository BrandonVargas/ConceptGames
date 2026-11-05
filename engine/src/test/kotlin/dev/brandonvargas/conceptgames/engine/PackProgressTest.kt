package dev.brandonvargas.conceptgames.engine

import dev.brandonvargas.conceptgames.engine.validate.PackProgress
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackProgressTest {
    private val sample = File(System.getProperty("samplesDir") ?: "../app/src/main/assets/sample_packs", "photosynthesis_en.json")
        .readText()
        .substringAfter("\"content\":")

    @Test
    fun `counts every section of a complete pack`() {
        val progress = PackProgress.of(sample)
        assertEquals(16, progress.concepts)
        assertEquals(12, progress.facts)
        assertEquals(10, progress.questions)
        assertEquals(8, progress.clozes)
        assertEquals(2, progress.timelines)
        assertEquals(3, progress.categories)
        assertEquals(6, progress.oddSets)
    }

    @Test
    fun `grows monotonically while streaming`() {
        var previous = PackProgress()
        for (end in sample.indices step 97) {
            val current = PackProgress.of(sample.substring(0, end))
            assertTrue(current.items >= previous.items)
            previous = current
        }
        assertEquals(0, PackProgress.of("{\"summary\": \"Photosynthesis is").items)
    }
}
