package org.sedaiadesigns

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

internal data class Question(val number: Int, val title: String, val slug: String, val difficulty: String, val content: String) {
    val url: String get() = "https://leetcode.com/problems/$slug/"
}

internal object LeetCodeClient {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private const val LIST_QUERY =
        $$"""query($filters: QuestionListFilterInput) { questionList(categorySlug: "", skip: 0, limit: 50, filters: $filters) { data { questionFrontendId titleSlug } } }"""
    private const val DETAIL_QUERY =
        $$"""query($slug: String!) { question(titleSlug: $slug) { questionFrontendId title titleSlug difficulty content isPaidOnly } }"""

    fun fetch(number: Int): Question? {
        val list = query(LIST_QUERY, JsonObject().apply {
            add("filters", JsonObject().apply { addProperty("searchKeywords", number.toString()) })
        }).getAsJsonObject("questionList")?.getAsJsonArray("data") ?: return null
        val slug = list.firstNotNullOfOrNull { item ->
            val candidate = item.asJsonObject
            if (candidate.get("questionFrontendId")?.asString == number.toString())
                candidate.get("titleSlug")?.asString else null
        } ?: return null
        val detail = query(DETAIL_QUERY, JsonObject().apply { addProperty("slug", slug) })
            .getAsJsonObject("question") ?: return null
        if (detail.get("questionFrontendId")?.asString != number.toString()) return null
        val paid = detail.get("isPaidOnly")?.asBoolean == true
        return Question(
            number,
            detail.get("title")?.asString ?: "Question $number",
            slug,
            detail.get("difficulty")?.asString ?: "",
            if (paid) "<p>This question requires LeetCode Premium. Open it on LeetCode to view it.</p>"
            else detail.get("content")?.takeUnless { it.isJsonNull }?.asString ?: "<p>No statement available.</p>"
        )
    }

    private fun query(graphql: String, variables: JsonObject): JsonObject {
        val body = JsonObject().apply {
            addProperty("query", graphql)
            add("variables", variables)
        }.toString()
        val request = HttpRequest.newBuilder(URI.create("https://leetcode.com/graphql/"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "Leetcode Helper IntelliJ plugin")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) error("LeetCode returned HTTP ${response.statusCode()}")
        val json = JsonParser.parseString(response.body()).asJsonObject
        if (json.has("errors")) error(json.getAsJsonArray("errors").firstOrNull()?.asJsonObject?.get("message")?.asString ?: "LeetCode query failed")
        return json.getAsJsonObject("data") ?: error("LeetCode returned no data")
    }
}
