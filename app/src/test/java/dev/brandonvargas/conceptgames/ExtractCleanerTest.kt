package dev.brandonvargas.conceptgames

import dev.brandonvargas.conceptgames.data.wikipedia.ExtractCleaner
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtractCleanerTest {
    @Test
    fun `cuts reference sections and empty headings`() {
        val extract = """
            Intro text.

            == History ==
            Some history.

            == Empty ==

            == Uses ==
            Some uses.

            == Véase también ==
            Other article
            == Referencias ==
            [1] Source
        """.trimIndent()
        assertEquals("Intro text.\n\n== History ==\nSome history.\n\n== Uses ==\nSome uses.", ExtractCleaner.clean(extract, 1_000))
    }

    @Test
    fun `respects the character budget`() {
        assertEquals("abc", ExtractCleaner.clean("abcdef", 3))
    }
}
