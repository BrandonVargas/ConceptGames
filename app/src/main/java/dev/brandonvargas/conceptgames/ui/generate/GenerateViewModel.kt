package dev.brandonvargas.conceptgames.ui.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.wikipedia.LanguageRepository
import dev.brandonvargas.conceptgames.data.wikipedia.WikiArticle
import dev.brandonvargas.conceptgames.domain.GeneratePackUseCase
import dev.brandonvargas.conceptgames.domain.GenerationEvent
import dev.brandonvargas.conceptgames.domain.GenerationRequest
import dev.brandonvargas.conceptgames.domain.GenerationStep
import dev.brandonvargas.conceptgames.engine.validate.PackProgress
import dev.brandonvargas.conceptgames.ui.navigation.GenerateRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerateState(
    val current: GenerationStep? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val stepStartedAt: Long = System.currentTimeMillis(),
    val articles: Int? = null,
    val characters: Int = 0,
    val webSources: Int? = null,
    val webSkipped: Boolean = false,
    /** Why web search was skipped; null when it is turned off in Settings. */
    val webSkipReason: Throwable? = null,
    val thought: String? = null,
    val progress: PackProgress? = null,
    /** Set while backing off before a retry, so the UI can count down instead of looking frozen. */
    val waitingUntil: Long? = null,
    val packId: Long? = null,
    val error: Throwable? = null,
    val cancelled: Boolean = false,
) {
    val running: Boolean get() = packId == null && error == null && !cancelled
}

/** Runs generation in the ViewModel so it survives rotation; leaving the screen cancels the request. */
class GenerateViewModel(
    private val generatePack: GeneratePackUseCase,
    private val languages: LanguageRepository,
    private val route: GenerateRoute,
) : ViewModel() {
    private val _state = MutableStateFlow(GenerateState())
    val state: StateFlow<GenerateState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        start()
    }

    fun start() {
        job?.cancel()
        _state.value = GenerateState()
        val request = GenerationRequest(
            topic = route.topic,
            language = route.language,
            languageName = languages.find(route.language).englishName,
            articles = route.articles.map { WikiArticle(it.pageId, it.title, null, null, it.url) },
            difficulty = route.difficulty,
        )
        job = viewModelScope.launch {
            generatePack(request).collect { event -> _state.update { it.reduce(event) } }
        }
    }

    /** Stops the in-flight Gemini/Wikipedia requests (the HTTP call is cancelled with the coroutine). */
    fun cancel() {
        job?.cancel()
        _state.update { it.copy(cancelled = true, waitingUntil = null) }
    }

    private fun GenerateState.reduce(event: GenerationEvent): GenerateState {
        val now = System.currentTimeMillis()
        return when (event) {
            is GenerationEvent.Step -> copy(current = event.step, stepStartedAt = now, waitingUntil = null)
            is GenerationEvent.ArticlesRead -> copy(articles = event.count, characters = event.characters)
            is GenerationEvent.WebSearched -> copy(webSources = event.sources, waitingUntil = null)
            is GenerationEvent.WebSkipped -> copy(webSkipped = true, webSkipReason = event.reason, waitingUntil = null)
            is GenerationEvent.Thinking -> copy(thought = event.summary.ifBlank { thought }, waitingUntil = null)
            is GenerationEvent.Writing -> copy(progress = event.progress, waitingUntil = null)
            is GenerationEvent.Waiting -> copy(waitingUntil = now + event.millis)
            is GenerationEvent.Done -> copy(packId = event.packId)
            is GenerationEvent.Failed -> copy(error = event.error, current = current ?: GenerationStep.ReadingWikipedia, waitingUntil = null)
        }
    }
}
