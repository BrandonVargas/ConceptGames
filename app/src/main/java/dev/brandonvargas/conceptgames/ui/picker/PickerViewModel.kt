package dev.brandonvargas.conceptgames.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import dev.brandonvargas.conceptgames.data.wikipedia.WikiArticle
import dev.brandonvargas.conceptgames.data.wikipedia.WikipediaClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PickerState(
    val loading: Boolean = true,
    val articles: List<WikiArticle> = emptyList(),
    val selected: Set<Long> = emptySet(),
    val webSearch: Boolean = false,
    val error: Throwable? = null,
) {
    fun selectedArticles() = articles.filter { it.pageId in selected }
}

class PickerViewModel(
    private val wikipedia: WikipediaClient,
    private val settings: SettingsRepository,
    private val topic: String,
    private val language: String,
) : ViewModel() {
    private val _state = MutableStateFlow(PickerState())
    val state: StateFlow<PickerState> = _state.asStateFlow()

    init {
        search()
    }

    fun search() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val webSearch = settings.current().webSearch
            _state.value = try {
                val articles = wikipedia.search(topic, language)
                PickerState(
                    loading = false,
                    articles = articles,
                    selected = articles.take(DEFAULT_SELECTED).map { it.pageId }.toSet(),
                    webSearch = webSearch,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PickerState(loading = false, webSearch = webSearch, error = e)
            }
        }
    }

    fun toggle(pageId: Long) = _state.update { state ->
        val selected = when {
            pageId in state.selected -> state.selected - pageId
            state.selected.size < MAX_SELECTED -> state.selected + pageId
            else -> state.selected
        }
        state.copy(selected = selected)
    }

    companion object {
        const val DEFAULT_SELECTED = 3
        const val MAX_SELECTED = 5
    }
}
