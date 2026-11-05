package dev.brandonvargas.conceptgames.engine

import dev.brandonvargas.conceptgames.engine.text.Alphabets
import dev.brandonvargas.conceptgames.engine.text.Graphemes
import dev.brandonvargas.conceptgames.engine.text.WordNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordNormalizerTest {
    @Test
    fun `removes separators and upper-cases`() {
        assertEquals("CARBONDIOXIDE", WordNormalizer.gridWord("Carbon dioxide", "en"))
        assertEquals("RUBISCO", WordNormalizer.gridWord("RuBisCO", "en"))
    }

    @Test
    fun `folds Spanish accents but keeps eñe`() {
        assertEquals("JUPITER", WordNormalizer.gridWord("Júpiter", "es"))
        assertEquals("PLUTON", WordNormalizer.gridWord("Plutón", "es"))
        assertEquals("NIÑO", WordNormalizer.gridWord("niño", "es"))
    }

    @Test
    fun `keeps letters that are distinct in other alphabets`() {
        assertEquals("ÄPFEL", WordNormalizer.gridWord("Äpfel", "de"))
        assertEquals("СОЛНЦЕ", WordNormalizer.gridWord("Солнце", "ru"))
        assertEquals("İSTANBUL", WordNormalizer.gridWord("istanbul", "tr"))
    }

    @Test
    fun `accepts short logographic words`() {
        assertEquals("气孔", WordNormalizer.gridWord("气孔", "zh"))
    }

    @Test
    fun `rejects unusable terms`() {
        assertNull(WordNormalizer.gridWord("Light-dependent reactions", "en"), "too long")
        assertNull(WordNormalizer.gridWord("H2O", "en"), "digits")
        assertNull(WordNormalizer.gridWord("Ox", "en"), "too short")
    }

    @Test
    fun `splits graphemes`() {
        assertEquals(listOf("N", "I", "Ñ", "O"), Graphemes.split("NIÑO"))
        assertEquals(3, Graphemes.split("éab").size, "combining accent stays with its letter")
    }

    @Test
    fun `alphabet covers pack letters`() {
        val spanish = Alphabets.forPack("es", listOf("NIÑO"))
        assertTrue("Ñ" in spanish && "A" in spanish && spanish.size == 27)
        assertEquals(listOf("Л", "О", "С"), Alphabets.forPack("ru", listOf("СОЛ")))
    }
}
