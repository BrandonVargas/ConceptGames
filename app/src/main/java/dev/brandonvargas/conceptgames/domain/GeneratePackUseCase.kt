package dev.brandonvargas.conceptgames.domain

import android.util.Log
import dev.brandonvargas.conceptgames.data.PackRepository
import dev.brandonvargas.conceptgames.data.gemini.GeminiClient
import dev.brandonvargas.conceptgames.data.gemini.GeminiEvent
import dev.brandonvargas.conceptgames.data.gemini.GeminiException
import dev.brandonvargas.conceptgames.data.gemini.GroundedText
import dev.brandonvargas.conceptgames.data.gemini.PackPrompt
import dev.brandonvargas.conceptgames.data.gemini.SourceText
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import dev.brandonvargas.conceptgames.data.wikipedia.WikiArticle
import dev.brandonvargas.conceptgames.data.wikipedia.WikipediaClient
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.model.Source
import dev.brandonvargas.conceptgames.engine.model.SourceKind
import dev.brandonvargas.conceptgames.engine.validate.InvalidPackException
import dev.brandonvargas.conceptgames.engine.validate.PackProgress
import dev.brandonvargas.conceptgames.engine.validate.PackValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

data class GenerationRequest(
    val topic: String,
    val language: String,
    val languageName: String,
    val articles: List<WikiArticle>,
    val difficulty: Difficulty,
)

enum class GenerationStep { ReadingWikipedia, SearchingWeb, WritingGames, Saving }

sealed interface GenerationEvent {
    data class Step(val step: GenerationStep) : GenerationEvent

    data class ArticlesRead(val count: Int, val characters: Int) : GenerationEvent

    data class WebSearched(val sources: Int) : GenerationEvent

    /** [reason] is null when web search is turned off in Settings. */
    data class WebSkipped(val reason: Throwable?) : GenerationEvent

    data class Thinking(val summary: String) : GenerationEvent

    data class Writing(val progress: PackProgress) : GenerationEvent

    /** Waiting [millis] before retrying (busy servers or a short free-tier limit). */
    data class Waiting(val millis: Long) : GenerationEvent

    data class Done(val packId: Long) : GenerationEvent

    data class Failed(val error: Throwable) : GenerationEvent
}

class NoSourcesException : Exception("No source text could be read")

/**
 * The whole research → AI → validation → storage pipeline, reporting live progress so the UI never looks stuck.
 * Costs one streamed Gemini call for the pack, plus one or two cheap attempts at a Google Search grounded brief.
 * Cancelling the collector cancels the in-flight HTTP request.
 */
class GeneratePackUseCase(
    private val wikipedia: WikipediaClient,
    private val gemini: GeminiClient,
    private val settings: SettingsRepository,
    private val packs: PackRepository,
) {
    operator fun invoke(request: GenerationRequest): Flow<GenerationEvent> = channelFlow {
        try {
            val apiKey = settings.apiKey() ?: throw GeminiException.MissingKey()
            val config = settings.current()
            val forward: (GeminiEvent) -> Unit = { event -> trySend(event.toGenerationEvent()) }

            send(GenerationEvent.Step(GenerationStep.ReadingWikipedia))
            val perArticle = MAX_SOURCE_CHARS / request.articles.size.coerceAtLeast(1)
            val articles = readArticles(request, perArticle)
            send(GenerationEvent.ArticlesRead(articles.size, articles.sumOf { it.text.length }))

            val texts = articles.toMutableList()
            val sources = articles.map { Source(it.title, it.url, SourceKind.Wikipedia) }.toMutableList()
            if (config.webSearch) {
                send(GenerationEvent.Step(GenerationStep.SearchingWeb))
                val brief = webBrief(apiKey, config.model, request, forward)
                if (brief != null) {
                    texts += SourceText("Web search brief", "google-search", brief.text.take(MAX_WEB_CHARS))
                    sources += brief.sources.take(MAX_WEB_SOURCES)
                    send(GenerationEvent.WebSearched(brief.sources.size))
                }
            } else {
                send(GenerationEvent.WebSkipped(null))
            }
            if (texts.isEmpty()) throw NoSourcesException()

            send(GenerationEvent.Step(GenerationStep.WritingGames))
            val prompt = PackPrompt.pack(request.topic, request.languageName, request.language, request.difficulty, texts)
            val pack = generateValidPack(apiKey, config.model, prompt, request, sources, forward)

            send(GenerationEvent.Step(GenerationStep.Saving))
            send(GenerationEvent.Done(packs.save(pack)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(GenerationEvent.Failed(e))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun readArticles(request: GenerationRequest, perArticle: Int): List<SourceText> = coroutineScope {
        request.articles.map { article ->
            async {
                runCatching { wikipedia.extract(request.language, article.pageId, perArticle) }
                    .getOrNull()
                    ?.takeIf { it.length > MIN_ARTICLE_CHARS }
                    ?.let { SourceText(article.title, article.url, it) }
            }
        }.awaitAll().filterNotNull()
    }

    /**
     * Google Search grounding is a bonus. Newer models often don't include it in the free tier, so try the chosen
     * model once, then an older Flash model, and otherwise carry on with Wikipedia alone and say why.
     */
    private suspend fun ProducerScope<GenerationEvent>.webBrief(
        apiKey: String,
        model: String,
        request: GenerationRequest,
        forward: (GeminiEvent) -> Unit,
    ): GroundedText? {
        val prompt = PackPrompt.webBrief(request.topic, request.languageName, request.difficulty)
        var lastError: GeminiException? = null
        for (candidate in listOf(model, GeminiClient.GROUNDING_FALLBACK_MODEL).distinct()) {
            try {
                return gemini.groundedText(apiKey, candidate, prompt, forward)
            } catch (e: GeminiException) {
                if (e.isFatalForKey) throw e
                Log.w(TAG, "Web search with $candidate failed: ${e::class.simpleName}: ${e.message}")
                lastError = e
                val worthAnotherModel = e is GeminiException.NotOnFreeTier || e is GeminiException.Quota ||
                    e is GeminiException.ModelNotFound || e is GeminiException.Other
                if (!worthAnotherModel) break
            }
        }
        send(GenerationEvent.WebSkipped(lastError))
        return null
    }

    private suspend fun generateValidPack(
        apiKey: String,
        model: String,
        prompt: String,
        request: GenerationRequest,
        sources: List<Source>,
        forward: (GeminiEvent) -> Unit,
    ) = withModelFallback(model) { chosen ->
        var lastError: InvalidPackException? = null
        // A malformed answer is usually a one-off, so try once more a bit less creatively.
        for (temperature in listOf(0.7, 0.4)) {
            val json = gemini.generateJson(apiKey, chosen, PackPrompt.system, prompt, PackPrompt.schema, temperature, forward)
            try {
                return@withModelFallback PackValidator.validate(PackValidator.parse(json), request.topic, request.language, sources)
            } catch (e: InvalidPackException) {
                lastError = e
            }
        }
        throw lastError!!
    }

    /** Falls back to the lighter model when the chosen one is out of free quota or unavailable. */
    private suspend fun <T> withModelFallback(model: String, block: suspend (String) -> T): T = try {
        block(model)
    } catch (e: GeminiException) {
        val canFallBack = (e is GeminiException.Quota || e is GeminiException.NotOnFreeTier || e is GeminiException.ModelNotFound) &&
            model != GeminiClient.FALLBACK_MODEL
        if (!canFallBack) throw e
        Log.w(TAG, "Generation with $model failed (${e::class.simpleName}), trying ${GeminiClient.FALLBACK_MODEL}")
        block(GeminiClient.FALLBACK_MODEL)
    }

    private fun GeminiEvent.toGenerationEvent(): GenerationEvent = when (this) {
        is GeminiEvent.Thought -> GenerationEvent.Thinking(summary)
        is GeminiEvent.Partial -> GenerationEvent.Writing(PackProgress.of(text))
        is GeminiEvent.Waiting -> GenerationEvent.Waiting(millis)
    }

    private companion object {
        const val TAG = "ConceptGames"
        const val MAX_SOURCE_CHARS = 40_000
        const val MAX_WEB_CHARS = 6_000
        const val MAX_WEB_SOURCES = 6
        const val MIN_ARTICLE_CHARS = 200
    }
}
