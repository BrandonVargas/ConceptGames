package dev.brandonvargas.conceptgames.data.gemini

import dev.brandonvargas.conceptgames.engine.model.Source
import dev.brandonvargas.conceptgames.engine.model.SourceKind
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.IOException
import kotlin.random.Random

/** Why a Gemini call failed, in terms the UI can explain and act on. */
sealed class GeminiException(message: String) : Exception(message) {
    class MissingKey : GeminiException("No API key")
    class InvalidKey(message: String) : GeminiException(message)
    class ApiDisabled(message: String) : GeminiException(message)
    class UnsupportedRegion(message: String) : GeminiException(message)

    /** Rate or daily limit hit. [retryAfterMs] is Google's own hint for when to try again. */
    class Quota(message: String, val retryAfterMs: Long? = null) : GeminiException(message)

    /** The free tier has no allowance at all for this model or feature (e.g. Google Search grounding). */
    class NotOnFreeTier(message: String) : GeminiException(message)
    class ModelNotFound(message: String) : GeminiException(message)
    class Overloaded(message: String) : GeminiException(message)
    class Blocked(message: String) : GeminiException(message)
    class Network(cause: Throwable) : GeminiException(cause.message ?: "Network error")
    class Other(message: String) : GeminiException(message)

    /** Errors that make every further call fail too, so the pipeline should stop right away. */
    val isFatalForKey: Boolean get() = this is MissingKey || this is InvalidKey || this is ApiDisabled || this is UnsupportedRegion
}

/** Live signals while Gemini works, so the UI never looks frozen. */
sealed interface GeminiEvent {
    /** A short summary of the model's reasoning (thinking models only). */
    data class Thought(val summary: String) : GeminiEvent

    /** The answer so far; grows with every streamed chunk. */
    data class Partial(val text: CharSequence) : GeminiEvent

    /** Backing off before a retry (busy servers or a short free-tier limit). */
    data class Waiting(val millis: Long) : GeminiEvent
}

data class GroundedText(val text: String, val sources: List<Source>)

/** Minimal client for the Gemini API using the user's own key (no SDK, no Google Play Services). */
class GeminiClient(
    private val http: HttpClient,
    private val json: Json,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val retryDelayMs: Long = 1_500,
) {
    /** Text models usable for generation. Doubles as the "Test key" check. */
    suspend fun listModels(apiKey: String): List<ModelInfo> = call {
        val response = http.get("$baseUrl/models") {
            header(KEY_HEADER, apiKey)
            parameter("pageSize", 1000)
        }
        response.ensureSuccess()
        response.body<ModelList>().models.orEmpty().filter { model ->
            model.supportedGenerationMethods.orEmpty().contains("generateContent") &&
                model.name.startsWith("models/gemini") &&
                excludedModelWords.none { model.name.contains(it) }
        }
    }

    /**
     * Streams a prompt constrained to [schema] and returns the full JSON text. Partial text and thought summaries
     * are reported through [onEvent] as they arrive.
     */
    suspend fun generateJson(
        apiKey: String,
        model: String,
        system: String,
        prompt: String,
        schema: JsonObject,
        temperature: Double,
        onEvent: (GeminiEvent) -> Unit = {},
    ): String {
        fun request(thoughts: Boolean) = GenerateContentRequest(
            contents = listOf(Content(listOf(Part(prompt)), role = "user")),
            systemInstruction = Content(listOf(Part(system))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = temperature,
                thinkingConfig = if (thoughts) ThinkingConfig(includeThoughts = true) else null,
            ),
        )
        return try {
            withRetries(onEvent) { stream(apiKey, model, request(thoughts = true), onEvent) }
        } catch (e: GeminiException.Other) {
            // Models without thinking reject thinkingConfig; they simply don't get thought summaries.
            if (!e.message.orEmpty().contains("think", ignoreCase = true)) throw e
            withRetries(onEvent) { stream(apiKey, model, request(thoughts = false), onEvent) }
        }
    }

    /**
     * Asks Gemini to research with Google Search and returns its answer plus the pages it cited.
     * Never retries on quota errors: grounding is often not part of the free tier, and retrying would only burn
     * the per-minute limit the main generation needs.
     */
    suspend fun groundedText(apiKey: String, model: String, prompt: String, onEvent: (GeminiEvent) -> Unit = {}): GroundedText {
        val request = GenerateContentRequest(
            contents = listOf(Content(listOf(Part(prompt)), role = "user")),
            tools = listOf(Tool(googleSearch = JsonObject(emptyMap()))),
        )
        val response = withRetries(onEvent, waitForQuota = false) {
            val response = http.post("$baseUrl/models/${model.removePrefix("models/")}:generateContent") {
                header(KEY_HEADER, apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.ensureSuccess()
            response.body<GenerateContentResponse>()
        }
        val candidate = response.candidates?.firstOrNull()
        val text = candidate?.content?.parts.orEmpty().filter { it.thought != true }.mapNotNull { it.text }.joinToString("")
        if (text.isBlank()) throw GeminiException.Other("Empty answer (${candidate?.finishReason})")
        val sources = candidate?.groundingMetadata?.groundingChunks.orEmpty()
            .mapNotNull { chunk ->
                val web = chunk.web ?: return@mapNotNull null
                Source(web.title ?: web.uri ?: return@mapNotNull null, web.uri ?: return@mapNotNull null, SourceKind.Web)
            }
            .distinctBy { it.title }
        return GroundedText(text, sources)
    }

    /** One streamed generateContent call over server-sent events. */
    private suspend fun stream(
        apiKey: String,
        model: String,
        request: GenerateContentRequest,
        onEvent: (GeminiEvent) -> Unit,
    ): String = http.preparePost("$baseUrl/models/${model.removePrefix("models/")}:streamGenerateContent") {
        parameter("alt", "sse")
        header(KEY_HEADER, apiKey)
        contentType(ContentType.Application.Json)
        setBody(request)
    }.execute { response ->
        response.ensureSuccess()
        val channel = response.bodyAsChannel()
        val answer = StringBuilder()
        var finishReason: String? = null
        while (true) {
            val line = channel.readUTF8Line() ?: break
            if (!line.startsWith("data:")) continue
            val chunk = json.decodeFromString(GenerateContentResponse.serializer(), line.substring(5).trim())
            chunk.promptFeedback?.blockReason?.let { throw GeminiException.Blocked(it) }
            val candidate = chunk.candidates?.firstOrNull() ?: continue
            candidate.finishReason?.let { finishReason = it }
            for (part in candidate.content?.parts.orEmpty()) {
                val text = part.text ?: continue
                if (part.thought == true) {
                    onEvent(GeminiEvent.Thought(thoughtTitle(text)))
                } else {
                    answer.append(text)
                    onEvent(GeminiEvent.Partial(answer))
                }
            }
        }
        if (answer.isBlank()) {
            if (finishReason == "SAFETY" || finishReason == "PROHIBITED_CONTENT") throw GeminiException.Blocked(finishReason!!)
            throw GeminiException.Other("Empty answer ($finishReason)")
        }
        answer.toString()
    }

    /**
     * Retries what is likely to succeed soon: busy servers, and per-minute limits when Google says the wait is short.
     * Every wait is reported, so the UI can show a countdown instead of a frozen spinner.
     */
    private suspend fun <T> withRetries(
        onEvent: (GeminiEvent) -> Unit,
        waitForQuota: Boolean = true,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        while (true) {
            try {
                return call(block)
            } catch (e: GeminiException) {
                val wait = when {
                    attempt >= MAX_RETRIES -> null
                    e is GeminiException.Overloaded -> retryDelayMs * (attempt + 1) + Random.nextLong(retryDelayMs / 2 + 1)
                    e is GeminiException.Quota && waitForQuota && attempt == 0 ->
                        (e.retryAfterMs ?: retryDelayMs).takeIf { it <= MAX_QUOTA_WAIT_MS }
                    else -> null
                } ?: throw e
                onEvent(GeminiEvent.Waiting(wait))
                delay(wait)
                attempt++
            }
        }
    }

    private suspend fun HttpResponse.ensureSuccess() {
        if (status.isSuccess()) return
        val body = runCatching { json.decodeFromString(ErrorEnvelope.serializer(), bodyAsText()).error }.getOrNull()
        throw mapError(status.value, body)
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: GeminiException) {
        throw e
    } catch (e: IOException) {
        throw GeminiException.Network(e)
    }

    companion object {
        const val KEY_HEADER = "x-goog-api-key"
        const val DEFAULT_MODEL = "gemini-flash-latest"
        const val FALLBACK_MODEL = "gemini-flash-lite-latest"

        /** Older Flash model that historically included free Google Search grounding. */
        const val GROUNDING_FALLBACK_MODEL = "gemini-2.5-flash"
        private const val MAX_RETRIES = 2
        private const val MAX_QUOTA_WAIT_MS = 30_000L
        private val excludedModelWords = listOf("image", "tts", "embedding", "live", "audio", "robotics", "computer-use")
        private val boldTitle = Regex("\\*\\*(.+?)\\*\\*")

        /** Thought summaries start with a bold headline such as "**Planning the questions**". */
        fun thoughtTitle(summary: String): String =
            boldTitle.find(summary)?.groupValues?.get(1)?.trim()
                ?: summary.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim().take(80)

        fun mapError(httpStatus: Int, body: ErrorBody?): GeminiException {
            val message = body?.message ?: "HTTP $httpStatus"
            val details = body?.details.orEmpty()
            val reasons = details.mapNotNull { it.reason }
            val noFreeAllowance = details.flatMap { it.violations.orEmpty() }.any { it.quotaValue?.content == "0" } ||
                message.contains("limit: 0")
            return when {
                "API_KEY_INVALID" in reasons || message.contains("API key not valid", ignoreCase = true) ->
                    GeminiException.InvalidKey(message)
                "SERVICE_DISABLED" in reasons || message.contains("has not been used in project", ignoreCase = true) ->
                    GeminiException.ApiDisabled(message)
                message.contains("location is not supported", ignoreCase = true) -> GeminiException.UnsupportedRegion(message)
                (httpStatus == 429 || body?.status == "RESOURCE_EXHAUSTED") && noFreeAllowance ->
                    GeminiException.NotOnFreeTier(message)
                httpStatus == 429 || body?.status == "RESOURCE_EXHAUSTED" ->
                    GeminiException.Quota(message, details.firstNotNullOfOrNull { it.retryDelay }?.let(::parseDelay))
                httpStatus == 404 -> GeminiException.ModelNotFound(message)
                httpStatus == 401 || httpStatus == 403 -> GeminiException.InvalidKey(message)
                httpStatus >= 500 -> GeminiException.Overloaded(message)
                else -> GeminiException.Other(message)
            }
        }

        /** Parses protobuf durations like "37s" or "1.5s". */
        internal fun parseDelay(value: String): Long? =
            value.removeSuffix("s").toDoubleOrNull()?.let { (it * 1000).toLong() }
    }
}
