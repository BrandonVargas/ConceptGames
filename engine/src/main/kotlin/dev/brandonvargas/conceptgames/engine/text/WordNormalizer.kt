package dev.brandonvargas.conceptgames.engine.text

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

/** Splits text into user-perceived characters, so "ñ" or Devanagari clusters stay a single tile. */
object Graphemes {
    fun split(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        val result = ArrayList<String>(text.length)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            result += text.substring(start, end)
            start = end
            end = iterator.next()
        }
        return result
    }
}

/**
 * Turns a term into a word usable by grid games: separators removed, upper-cased for the content language and,
 * for languages whose crosswords ignore accents, folded to base letters (Spanish keeps Ñ).
 */
object WordNormalizer {
    const val MAX_LENGTH = 14

    /** Latin-script languages whose word games conventionally ignore accents. */
    private val accentFoldingLanguages = setOf(
        "en", "es", "fr", "pt", "it", "ca", "gl", "nl", "ro", "eu", "id", "ms", "tl", "sw", "af", "oc", "la", "ga",
    )
    private val keepsEnye = setOf("es", "gl")
    private const val SEPARATORS = "-‐‑–—'’`´·.‧_/"

    fun baseLanguage(language: String): String = language.substringBefore('-').lowercase(Locale.ROOT)

    fun gridWord(term: String, language: String): String? {
        val lang = baseLanguage(language)
        var text = Normalizer.normalize(term.trim(), Normalizer.Form.NFC)
            .filterNot { it.isWhitespace() || it in SEPARATORS }
        if (lang in accentFoldingLanguages) text = foldAccents(text, keepEnye = lang in keepsEnye)
        text = text.uppercase(Locale.forLanguageTag(language))
        if (text.isEmpty() || !text.codePoints().allMatch(::isLetterOrMark)) return null
        val length = Graphemes.split(text).size
        val minLength = if (isLogographic(text)) 2 else 3
        return text.takeIf { length in minLength..MAX_LENGTH }
    }

    fun foldAccents(text: String, keepEnye: Boolean): String {
        val builder = StringBuilder(text.length)
        for (char in text) {
            if (keepEnye && (char == 'ñ' || char == 'Ñ')) {
                builder.append(char)
                continue
            }
            val decomposed = Normalizer.normalize(char.toString(), Normalizer.Form.NFD)
            decomposed.filterTo(builder) { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        }
        return builder.toString().replace("Æ", "AE").replace("æ", "ae").replace("Œ", "OE").replace("œ", "oe")
    }

    private fun isLetterOrMark(codePoint: Int): Boolean {
        if (Character.isLetter(codePoint)) return true
        val type = Character.getType(codePoint)
        return type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()
    }

    private fun isLogographic(text: String): Boolean {
        val script = Character.UnicodeScript.of(text.codePointAt(0))
        return script == Character.UnicodeScript.HAN ||
            script == Character.UnicodeScript.HIRAGANA ||
            script == Character.UnicodeScript.KATAKANA ||
            script == Character.UnicodeScript.HANGUL
    }
}

/** Letters offered by keyboards (hangman) and used to fill word-search grids. */
object Alphabets {
    private val latin = ('A'..'Z').map(Char::toString)

    fun forPack(language: String, words: Collection<String>): List<String> {
        val fromWords = words.flatMap(Graphemes::split).distinct()
        val lang = WordNormalizer.baseLanguage(language)
        val usesLatin = fromWords.isEmpty() ||
            fromWords.all { Character.UnicodeScript.of(it.codePointAt(0)) == Character.UnicodeScript.LATIN }
        if (!usesLatin) return fromWords.sorted()
        val extras = if (lang == "es" || lang == "gl") listOf("Ñ") else emptyList()
        return (latin + extras + fromWords).distinct().sortedWith(compareBy({ it !in latin }, { it }))
    }
}
