package dev.brandonvargas.conceptgames.data.wikipedia

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** All Wikipedia editions: the bundled big ones right away, the full list once fetched. */
class LanguageRepository(private val wikipedia: WikipediaClient) {
    private val _languages = MutableStateFlow(BundledLanguages.all)
    val languages: StateFlow<List<WikiLanguage>> = _languages.asStateFlow()
    private val mutex = Mutex()
    private var loaded = false

    suspend fun refresh() = mutex.withLock {
        if (loaded) return@withLock
        val remote = runCatching { wikipedia.languages() }.getOrNull().orEmpty()
        if (remote.isNotEmpty()) {
            val bundledCodes = BundledLanguages.all.map { it.code }.toSet()
            _languages.value = BundledLanguages.all + remote.filter { it.code !in bundledCodes }.sortedBy { it.englishName }
            loaded = true
        }
    }

    fun find(code: String): WikiLanguage =
        _languages.value.firstOrNull { it.code == code } ?: WikiLanguage(code, code, code)
}
