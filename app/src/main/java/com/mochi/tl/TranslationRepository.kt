package com.mochi.tl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable private data class ChatMessage(val role: String, val content: String)
@Serializable private data class ChatRequest(val model: String, val messages: List<ChatMessage>, val temperature: Double = 0.3, val max_tokens: Int = 8192)
@Serializable private data class ChatChoice(val message: ChatMessage)
@Serializable private data class ChatResponse(val choices: List<ChatChoice> = emptyList())
@Serializable private data class GeminiPart(val text: String)
@Serializable private data class GeminiContent(val parts: List<GeminiPart>, val role: String = "user")
@Serializable private data class GeminiRequest(val contents: List<GeminiContent>, val systemInstruction: GeminiContent? = null)
@Serializable private data class GeminiCandidate(val content: GeminiContent)
@Serializable private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

class TranslationRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun translate(config: ProviderConfig, apiKey: String?, systemPrompt: String, text: String): String {
        return if (config.id == "gemini") translateGemini(config, apiKey.orEmpty(), systemPrompt, text)
        else translateOpenAiCompatible(config, apiKey, systemPrompt, text)
    }

    private suspend fun translateOpenAiCompatible(config: ProviderConfig, apiKey: String?, system: String, text: String): String {
        val root = config.baseUrl.trimEnd('/').removeSuffix("/v1")
        val response = client.post("$root/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            setBody(ChatRequest(config.model, listOf(ChatMessage("system", system), ChatMessage("user", text))))
        }
        return response.body<ChatResponse>().choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    private suspend fun translateGemini(config: ProviderConfig, apiKey: String, system: String, text: String): String {
        require(apiKey.isNotBlank()) { "API key Gemini belum diatur." }
        val response = client.post("${config.baseUrl.trimEnd('/')}/v1beta/models/${config.model}:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(text)))), GeminiContent(listOf(GeminiPart(system)), "system")))
        }
        return response.body<GeminiResponse>().candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text }?.trim().orEmpty()
    }

    suspend fun testConnection(config: ProviderConfig, apiKey: String?): Result<Unit> = runCatching {
        if (config.id == "gemini") require(!apiKey.isNullOrBlank()) { "API key wajib untuk Gemini." }
        else {
            val root = config.baseUrl.trimEnd('/').removeSuffix("/v1")
            client.get("$root/v1/models") { apiKey?.let { header("Authorization", "Bearer $it") } }
        }
    }
}
