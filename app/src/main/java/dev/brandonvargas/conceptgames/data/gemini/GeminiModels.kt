package dev.brandonvargas.conceptgames.data.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Wire format of the Gemini REST API (generativelanguage.googleapis.com/v1beta). Only the fields we use.

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null,
)

@Serializable
data class Content(val parts: List<Part>, val role: String? = null)

@Serializable
data class Part(val text: String? = null, val thought: Boolean? = null)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Double? = null,
    val thinkingConfig: ThinkingConfig? = null,
)

/** Asks thinking models to stream short summaries of their reasoning, shown while the user waits. */
@Serializable
data class ThinkingConfig(val includeThoughts: Boolean)

@Serializable
data class Tool(@SerialName("google_search") val googleSearch: JsonObject? = null)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
    val groundingMetadata: GroundingMetadata? = null,
)

@Serializable
data class PromptFeedback(val blockReason: String? = null)

@Serializable
data class GroundingMetadata(val groundingChunks: List<GroundingChunk>? = null)

@Serializable
data class GroundingChunk(val web: WebChunk? = null)

@Serializable
data class WebChunk(val uri: String? = null, val title: String? = null)

@Serializable
data class ModelList(val models: List<ModelInfo>? = null, val nextPageToken: String? = null)

@Serializable
data class ModelInfo(
    val name: String,
    val displayName: String? = null,
    val supportedGenerationMethods: List<String>? = null,
)

@Serializable
data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
data class ErrorBody(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null,
    val details: List<ErrorDetail>? = null,
)

@Serializable
data class ErrorDetail(
    @SerialName("@type") val type: String? = null,
    val reason: String? = null,
    /** From google.rpc.RetryInfo, e.g. "37s". */
    val retryDelay: String? = null,
    /** From google.rpc.QuotaFailure. */
    val violations: List<QuotaViolation>? = null,
)

@Serializable
data class QuotaViolation(
    val quotaMetric: String? = null,
    val quotaId: String? = null,
    /** int64 in the proto, so Google may send it quoted or as a number. */
    val quotaValue: JsonPrimitive? = null,
)
