package dev.brandonvargas.conceptgames.data.wikipedia

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

data class WikiArticle(
    val pageId: Long,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val url: String,
)

class WikipediaException(message: String) : IOException(message)

/** Talks to the public MediaWiki Action API of any Wikipedia language edition. No key needed. */
class WikipediaClient(private val http: HttpClient) {

    suspend fun search(query: String, language: String, limit: Int = 8): List<WikiArticle> {
        val response = http.get(apiUrl(language)) {
            parameter("action", "query")
            parameter("format", "json")
            parameter("formatversion", 2)
            parameter("generator", "search")
            parameter("gsrsearch", query)
            parameter("gsrlimit", limit)
            parameter("gsrnamespace", 0)
            parameter("prop", "description|pageimages|info")
            parameter("piprop", "thumbnail")
            parameter("pithumbsize", 160)
            parameter("inprop", "url")
            parameter("redirects", 1)
        }
        val body = response.checked().body<QueryResponse<SearchPage>>()
        return body.query?.pages.orEmpty()
            .sortedBy { it.index ?: Int.MAX_VALUE }
            .map { WikiArticle(it.pageid, it.title, it.description, it.thumbnail?.source, it.fullurl ?: articleUrl(language, it.title)) }
    }

    /** Plain-text article body without reference sections, trimmed to [maxChars]. */
    suspend fun extract(language: String, pageId: Long, maxChars: Int): String {
        val response = http.get(apiUrl(language)) {
            parameter("action", "query")
            parameter("format", "json")
            parameter("formatversion", 2)
            parameter("prop", "extracts")
            parameter("explaintext", 1)
            parameter("exsectionformat", "wiki")
            parameter("pageids", pageId)
        }
        val page = response.checked().body<QueryResponse<ExtractPage>>().query?.pages?.firstOrNull()
        return ExtractCleaner.clean(page?.extract.orEmpty(), maxChars)
    }

    /** Every open Wikipedia edition, from the Wikimedia site matrix. */
    suspend fun languages(): List<WikiLanguage> {
        val response = http.get("https://meta.wikimedia.org/w/api.php") {
            parameter("action", "sitematrix")
            parameter("format", "json")
            parameter("formatversion", 2)
            parameter("smtype", "language")
            parameter("smsiteprop", "code|url")
            parameter("smstate", "all")
        }
        val matrix = response.checked().body<JsonObject>()["sitematrix"]?.jsonObject ?: return emptyList()
        return matrix.entries.filter { it.key != "count" }.mapNotNull { (_, value) ->
            val language = value.jsonObject
            val site = language["site"]?.jsonArray?.firstOrNull {
                it.jsonObject["code"]?.jsonPrimitive?.content == "wiki" && it.jsonObject["closed"] == null
            } ?: return@mapNotNull null
            val host = site.jsonObject["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            WikiLanguage(
                code = host.removePrefix("https://").substringBefore(".wikipedia.org"),
                nativeName = language["name"]?.jsonPrimitive?.content.orEmpty(),
                englishName = language["localname"]?.jsonPrimitive?.content.orEmpty(),
            )
        }.filter { it.code.isNotBlank() && it.nativeName.isNotBlank() }
    }

    private fun HttpResponse.checked(): HttpResponse {
        if (!status.isSuccess()) throw WikipediaException("Wikipedia answered ${status.value}")
        return this
    }

    companion object {
        fun apiUrl(language: String) = "https://$language.wikipedia.org/w/api.php"

        fun articleUrl(language: String, title: String) =
            "https://$language.wikipedia.org/wiki/${title.replace(' ', '_')}"
    }

    @Serializable
    private data class QueryResponse<T>(val query: Query<T>? = null)

    @Serializable
    private data class Query<T>(val pages: List<T>? = null)

    @Serializable
    private data class SearchPage(
        val pageid: Long,
        val title: String,
        val index: Int? = null,
        val description: String? = null,
        val thumbnail: Thumbnail? = null,
        val fullurl: String? = null,
    )

    @Serializable
    private data class Thumbnail(val source: String)

    @Serializable
    private data class ExtractPage(val pageid: Long? = null, val extract: String? = null)
}

/** Removes reference-like sections ("See also", "Referencias"...) and blank sections from plain-text extracts. */
object ExtractCleaner {
    private val stopHeadings = setOf(
        // en
        "references", "notes", "see also", "external links", "further reading", "bibliography", "sources", "citations",
        // es
        "referencias", "notas", "véase también", "enlaces externos", "bibliografía", "fuentes",
        // fr
        "références", "notes et références", "voir aussi", "liens externes", "bibliographie",
        // de
        "einzelnachweise", "literatur", "weblinks", "siehe auch", "anmerkungen",
        // pt
        "referências", "ver também", "ligações externas", "bibliografia",
        // it
        "note", "voci correlate", "collegamenti esterni",
        // ru
        "примечания", "литература", "ссылки", "см. также",
        // zh / ja
        "参考文献", "脚注", "関連項目", "外部リンク", "参见", "外部链接", "注释",
    )
    private val heading = Regex("^(=+)\\s*(.+?)\\s*=+\\s*$")

    fun clean(extract: String, maxChars: Int): String {
        val lines = extract.lines()
        val kept = ArrayList<String>(lines.size)
        for (line in lines) {
            val match = heading.matchEntire(line.trim())
            if (match != null && match.groupValues[2].lowercase() in stopHeadings) break
            kept += line
        }
        // Drop headings immediately followed by another heading or the end (empty sections).
        val compact = kept.filterIndexed { index, line ->
            val isHeading = heading.matches(line.trim())
            val next = kept.drop(index + 1).firstOrNull { it.isNotBlank() }
            !isHeading || (next != null && !heading.matches(next.trim()))
        }
        return compact.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
            .take(maxChars)
    }
}
