package dev.brandonvargas.conceptgames

import dev.brandonvargas.conceptgames.data.gemini.ErrorBody
import dev.brandonvargas.conceptgames.data.gemini.GeminiClient
import dev.brandonvargas.conceptgames.data.gemini.GeminiEvent
import dev.brandonvargas.conceptgames.data.gemini.GeminiException
import dev.brandonvargas.conceptgames.data.gemini.PackPrompt
import dev.brandonvargas.conceptgames.data.network.HttpClients
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeminiClientTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val sse = headersOf(HttpHeaders.ContentType, "text/event-stream")
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(handler: suspend MockRequestHandleScope.(call: Int) -> HttpResponseData): GeminiClient {
        val engine = MockEngine { request ->
            assertEquals("test-key", request.headers[GeminiClient.KEY_HEADER])
            requests += request
            handler(requests.size - 1)
        }
        return GeminiClient(HttpClients.create(engine), HttpClients.json, retryDelayMs = 1)
    }

    private fun MockRequestHandleScope.error(status: HttpStatusCode, body: String) = respond(body, status, json)

    /** Server-sent events as Gemini streams them: a thought summary, then the answer in two chunks. */
    private fun MockRequestHandleScope.streamed() = respond(
        listOf(
            """{"candidates":[{"content":{"parts":[{"text":"**Planning the questions**\n\nI will...","thought":true}]}}]}""",
            """{"candidates":[{"content":{"parts":[{"text":"{\"summary\":"}]}}]}""",
            """{"candidates":[{"content":{"parts":[{"text":"\"ok\"}"}]},"finishReason":"STOP"}]}""",
        ).joinToString("") { "data: $it\r\n\r\n" },
        HttpStatusCode.OK,
        sse,
    )

    private val grounded = """
        {"candidates":[{"content":{"parts":[{"text":"Brief."}]},
        "groundingMetadata":{"groundingChunks":[{"web":{"uri":"https://example.org/a","title":"example.org"}}]}}]}
    """.trimIndent()

    private suspend fun GeminiClient.generate(onEvent: (GeminiEvent) -> Unit = {}) =
        generateJson("test-key", "gemini-flash-latest", "system", "prompt", PackPrompt.schema, 0.7, onEvent)

    @Test
    fun `streams the answer and reports thoughts and partial text`() = runTest {
        val gemini = client { streamed() }
        val events = mutableListOf<GeminiEvent>()
        assertEquals("{\"summary\":\"ok\"}", gemini.generate { events += it })
        assertEquals(GeminiEvent.Thought("Planning the questions"), events.first())
        assertEquals("{\"summary\":\"ok\"}", (events.last() as GeminiEvent.Partial).text.toString())
        assertTrue(requests.single().url.toString().contains(":streamGenerateContent?alt=sse"))
    }

    @Test
    fun `requests use the documented wire format`() = runTest {
        val gemini = client { call -> if (call == 0) streamed() else respond(grounded, HttpStatusCode.OK, json) }
        gemini.generate()
        val brief = gemini.groundedText("test-key", "gemini-flash-latest", "brief")
        val structured = (requests[0].body as TextContent).text
        assertTrue("\"responseMimeType\":\"application/json\"" in structured)
        assertTrue("\"responseSchema\":{\"type\":\"OBJECT\"" in structured)
        assertTrue("\"thinkingConfig\":{\"includeThoughts\":true}" in structured)
        assertTrue("\"tools\"" !in structured)
        assertTrue("\"tools\":[{\"google_search\":{}}]" in (requests[1].body as TextContent).text)
        assertEquals(listOf("example.org"), brief.sources.map { it.title })
    }

    @Test
    fun `models that can't think are asked again without thought summaries`() = runTest {
        val gemini = client { call ->
            if (call == 0) {
                error(HttpStatusCode.BadRequest, """{"error":{"code":400,"status":"INVALID_ARGUMENT","message":"Thinking is not supported by this model."}}""")
            } else {
                streamed()
            }
        }
        assertEquals("{\"summary\":\"ok\"}", gemini.generate())
        assertTrue("thinkingConfig" !in (requests[1].body as TextContent).text)
    }

    @Test
    fun `invalid key is reported as such`() = runTest {
        val gemini = client {
            error(
                HttpStatusCode.BadRequest,
                """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.",
                "status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""",
            )
        }
        assertFailsWith<GeminiException.InvalidKey> { gemini.listModels("test-key") }
    }

    @Test
    fun `short quota waits are retried once, reporting the wait`() = runTest {
        val gemini = client {
            error(
                HttpStatusCode.TooManyRequests,
                """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded",
                "details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"0.002s"}]}}""",
            )
        }
        val events = mutableListOf<GeminiEvent>()
        assertFailsWith<GeminiException.Quota> { gemini.generate { events += it } }
        assertEquals(2, requests.size)
        assertEquals(listOf<GeminiEvent>(GeminiEvent.Waiting(2)), events)
    }

    @Test
    fun `long quota waits are not retried`() = runTest {
        val gemini = client {
            error(
                HttpStatusCode.TooManyRequests,
                """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded",
                "details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"45s"}]}}""",
            )
        }
        val error = assertFailsWith<GeminiException.Quota> { gemini.generate() }
        assertEquals(45_000L, error.retryAfterMs)
        assertEquals(1, requests.size)
    }

    @Test
    fun `features without free allowance are never retried`() = runTest {
        val gemini = client {
            error(
                HttpStatusCode.TooManyRequests,
                """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded for metric: x, limit: 0",
                "details":[{"@type":"type.googleapis.com/google.rpc.QuotaFailure",
                "violations":[{"quotaMetric":"generativelanguage.googleapis.com/search_grounding_request","quotaValue":"0"}]}]}}""",
            )
        }
        assertFailsWith<GeminiException.NotOnFreeTier> { gemini.groundedText("test-key", "gemini-flash-latest", "brief") }
        assertEquals(1, requests.size)
    }

    @Test
    fun `overload is retried until it succeeds`() = runTest {
        val gemini = client { call ->
            if (call < 2) error(HttpStatusCode.ServiceUnavailable, """{"error":{"code":503,"status":"UNAVAILABLE","message":"overloaded"}}""") else streamed()
        }
        assertEquals("{\"summary\":\"ok\"}", gemini.generate())
        assertEquals(3, requests.size)
    }

    @Test
    fun `model list keeps only text generation models`() = runTest {
        val gemini = client {
            respond(
                """{"models":[
                {"name":"models/gemini-flash-latest","supportedGenerationMethods":["generateContent"]},
                {"name":"models/gemini-embedding-001","supportedGenerationMethods":["embedContent"]},
                {"name":"models/gemini-2.5-flash-image","supportedGenerationMethods":["generateContent"]}]}""",
                HttpStatusCode.OK,
                json,
            )
        }
        assertEquals(listOf("models/gemini-flash-latest"), gemini.listModels("test-key").map { it.name })
    }

    @Test
    fun `error mapping covers region, disabled API and missing model`() {
        assertIs<GeminiException.UnsupportedRegion>(GeminiClient.mapError(400, ErrorBody(message = "User location is not supported for the API use.")))
        assertIs<GeminiException.ApiDisabled>(GeminiClient.mapError(403, ErrorBody(message = "Generative Language API has not been used in project 1")))
        assertIs<GeminiException.ModelNotFound>(GeminiClient.mapError(404, ErrorBody(message = "models/x is not found")))
        assertIs<GeminiException.Overloaded>(GeminiClient.mapError(500, null))
    }

    @Test
    fun `thought titles come from the bold headline`() {
        assertEquals("Mapping the key ideas", GeminiClient.thoughtTitle("**Mapping the key ideas**\n\nI'm reading…"))
        assertEquals("Plain first line", GeminiClient.thoughtTitle("\nPlain first line\nmore"))
    }
}
