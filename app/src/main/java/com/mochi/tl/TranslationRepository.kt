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
@Serializable private data class GeminiGenerationConfig(val temperature: Double, val maxOutputTokens: Int)
@Serializable private data class GeminiPart(val text: String)
@Serializable private data class GeminiContent(val parts: List<GeminiPart>, val role: String = "user")
@Serializable private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)
@Serializable private data class GeminiCandidate(val content: GeminiContent)
@Serializable private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable private data class OpenAiModelItem(val id: String)
@Serializable private data class OpenAiModelsResponse(val data: List<OpenAiModelItem> = emptyList())

@Serializable private data class OllamaModelItem(val name: String)
@Serializable private data class OllamaModelsResponse(val models: List<OllamaModelItem> = emptyList())

@Serializable private data class GeminiModelItem(val name: String)
@Serializable private data class GeminiModelsResponse(val models: List<GeminiModelItem> = emptyList())

class TranslationRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun translate(
        config: ProviderConfig,
        apiKey: String?,
        systemPrompt: String,
        text: String,
        temperature: Double = 0.3,
        maxTokens: Int = 8192
    ): String {
        return if (config.id == "gemini") translateGemini(config, apiKey.orEmpty(), systemPrompt, text, temperature, maxTokens)
        else translateOpenAiCompatible(config, apiKey, systemPrompt, text, temperature, maxTokens)
    }

    private suspend fun translateOpenAiCompatible(
        config: ProviderConfig,
        apiKey: String?,
        system: String,
        text: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        val root = config.baseUrl.trimEnd('/').removeSuffix("/v1")
        val response = client.post("$root/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            setBody(ChatRequest(config.model, listOf(ChatMessage("system", system), ChatMessage("user", text)), temperature, maxTokens))
        }
        return response.body<ChatResponse>().choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    private suspend fun translateGemini(
        config: ProviderConfig,
        apiKey: String,
        system: String,
        text: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        require(apiKey.isNotBlank()) { "API key Gemini belum diatur." }
        // API key dikirim via header x-goog-api-key, BUKAN query param ?key=
        // agar tidak ikut tercatat di log HTTP/proxy/error reporting.
        val response = client.post("${config.baseUrl.trimEnd('/')}/v1beta/models/${config.model}:generateContent") {
            contentType(ContentType.Application.Json)
            header("x-goog-api-key", apiKey)
            setBody(GeminiRequest(
                contents = listOf(GeminiContent(listOf(GeminiPart(text)))),
                systemInstruction = GeminiContent(listOf(GeminiPart(system)), "system"),
                generationConfig = GeminiGenerationConfig(temperature = temperature, maxOutputTokens = maxTokens)
            ))
        }
        return response.body<GeminiResponse>().candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text }?.trim().orEmpty()
    }

    suspend fun testConnection(config: ProviderConfig, apiKey: String?): Result<Unit> = runCatching {
        if (config.id == "gemini") require(!apiKey.isNullOrBlank()) { "API key wajib untuk Gemini." }
        else {
            val root = config.baseUrl.trimEnd('/').removeSuffix("/v1")
            client.get("$root/v1/models") { apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") } }
        }
    }

    suspend fun fetchModels(config: ProviderConfig, apiKey: String?): Result<List<String>> = runCatching {
        val root = config.baseUrl.trimEnd('/').removeSuffix("/v1")
        val modelList = when (config.id) {
            "gemini" -> {
                require(!apiKey.isNullOrBlank()) { "API key Gemini belum diatur." }
                val resp = client.get("${config.baseUrl.trimEnd('/')}/v1beta/models") {
                    header("x-goog-api-key", apiKey)
                }.body<GeminiModelsResponse>()
                resp.models.map { it.name.removePrefix("models/") }.filter { it.contains("gemini", ignoreCase = true) }
            }
            "ollama" -> {
                try {
                    val resp = client.get("$root/v1/models").body<OpenAiModelsResponse>()
                    resp.data.map { it.id }
                } catch (e: Exception) {
                    val resp = client.get("${config.baseUrl.trimEnd('/')}/api/tags").body<OllamaModelsResponse>()
                    resp.models.map { it.name }
                }
            }
            else -> {
                val resp = client.get("$root/v1/models") {
                    apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
                }.body<OpenAiModelsResponse>()
                resp.data.map { it.id }
            }
        }
        val sortedList = modelList.distinct().sorted()
        if (sortedList.isEmpty()) listOf(config.model) else sortedList
    }
}
