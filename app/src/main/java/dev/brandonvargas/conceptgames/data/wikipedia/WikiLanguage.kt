package dev.brandonvargas.conceptgames.data.wikipedia

import java.util.Locale

data class WikiLanguage(
    /** Subdomain of the edition, e.g. "es" for es.wikipedia.org. */
    val code: String,
    val nativeName: String,
    /** English name, used when asking the AI to write in this language. */
    val englishName: String,
)

/** Largest editions, available offline; the full list is fetched from the Wikimedia site matrix. */
object BundledLanguages {
    val all = listOf(
        WikiLanguage("en", "English", "English"),
        WikiLanguage("es", "Español", "Spanish"),
        WikiLanguage("fr", "Français", "French"),
        WikiLanguage("de", "Deutsch", "German"),
        WikiLanguage("pt", "Português", "Portuguese"),
        WikiLanguage("it", "Italiano", "Italian"),
        WikiLanguage("nl", "Nederlands", "Dutch"),
        WikiLanguage("ca", "Català", "Catalan"),
        WikiLanguage("gl", "Galego", "Galician"),
        WikiLanguage("eu", "Euskara", "Basque"),
        WikiLanguage("pl", "Polski", "Polish"),
        WikiLanguage("cs", "Čeština", "Czech"),
        WikiLanguage("sv", "Svenska", "Swedish"),
        WikiLanguage("no", "Norsk bokmål", "Norwegian"),
        WikiLanguage("da", "Dansk", "Danish"),
        WikiLanguage("fi", "Suomi", "Finnish"),
        WikiLanguage("ro", "Română", "Romanian"),
        WikiLanguage("hu", "Magyar", "Hungarian"),
        WikiLanguage("tr", "Türkçe", "Turkish"),
        WikiLanguage("el", "Ελληνικά", "Greek"),
        WikiLanguage("ru", "Русский", "Russian"),
        WikiLanguage("uk", "Українська", "Ukrainian"),
        WikiLanguage("ar", "العربية", "Arabic"),
        WikiLanguage("he", "עברית", "Hebrew"),
        WikiLanguage("fa", "فارسی", "Persian"),
        WikiLanguage("hi", "हिन्दी", "Hindi"),
        WikiLanguage("bn", "বাংলা", "Bengali"),
        WikiLanguage("ta", "தமிழ்", "Tamil"),
        WikiLanguage("th", "ไทย", "Thai"),
        WikiLanguage("vi", "Tiếng Việt", "Vietnamese"),
        WikiLanguage("id", "Bahasa Indonesia", "Indonesian"),
        WikiLanguage("ms", "Bahasa Melayu", "Malay"),
        WikiLanguage("tl", "Tagalog", "Tagalog"),
        WikiLanguage("sw", "Kiswahili", "Swahili"),
        WikiLanguage("zh", "中文", "Chinese"),
        WikiLanguage("ja", "日本語", "Japanese"),
        WikiLanguage("ko", "한국어", "Korean"),
        WikiLanguage("eo", "Esperanto", "Esperanto"),
        WikiLanguage("la", "Latina", "Latin"),
    )

    fun find(code: String): WikiLanguage? = all.firstOrNull { it.code == code }

    /** The device language if Wikipedia has it, else English. */
    fun deviceDefault(): String {
        val device = Locale.getDefault().language
        return find(device)?.code ?: "en"
    }
}
