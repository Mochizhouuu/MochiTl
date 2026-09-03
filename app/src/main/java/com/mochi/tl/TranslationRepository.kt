package com.mochi.tl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
@Serializable private data class GeminiCandidate(val content: GeminiContent? = null)
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
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }

    private fun cleanBaseUrl(baseUrl: String): String =
        baseUrl.trimEnd('/').removeSuffix("/v1")

    private fun cleanBaseUrlWithoutV1(baseUrl: String): String =
        baseUrl.trimEnd('/')

    private suspend fun checkResponseStatus(response: HttpResponse, providerId: String) {
        if (response.status.value !in 200..299) {
            val errBody = runCatching { response.bodyAsText() }.getOrDefault("")
            throw IllegalStateException(formatHttpError(response.status.value, response.status.description, errBody, providerId))
        }
    }

    private fun formatHttpError(statusCode: Int, description: String, body: String, providerId: String): String {
        return when (statusCode) {
            401 -> "API Key salah atau tidak valid (HTTP 401)."
            429 -> "Batas penggunaan (Rate Limit) terlampaui (HTTP 429). Silakan tunggu beberapa saat."
            else -> {
                val detail = body.take(MAX_ERROR_DETAIL_LENGTH).ifBlank { description }
                "API Error (${statusCode}): $detail"
            }
        }
    }

    private fun formatNetworkException(e: Exception, providerId: String, baseUrl: String): Exception {
        val isLocal = providerId == PROVIDER_OLLAMA || providerId == PROVIDER_LMSTUDIO ||
                baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1") || baseUrl.contains("10.0.2.2")
        return if (isLocal) {
            IllegalStateException("Gagal terhubung ke server lokal. Pastikan server ($providerId) sudah dijalankan dan URL server sudah benar.", e)
        } else {
            IllegalStateException("Gagal terhubung ke server API (${e.localizedMessage ?: "Koneksi terputus"}). Periksa koneksi internet atau URL server.", e)
        }
    }

    suspend fun translate(
        config: ProviderConfig,
        apiKey: String?,
        systemPrompt: String,
        text: String,
        temperature: Double = DEFAULT_TEMPERATURE,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): String {
        return if (config.id == PROVIDER_GEMINI) {
            translateGemini(config, apiKey.orEmpty(), systemPrompt, text, temperature, maxTokens)
        } else {
            translateOpenAiCompatible(config, apiKey, systemPrompt, text, temperature, maxTokens)
        }
    }

    private suspend fun translateOpenAiCompatible(
        config: ProviderConfig,
        apiKey: String?,
        system: String,
        text: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        val root = cleanBaseUrl(config.baseUrl)
        try {
            val response = client.post("$root/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
                setBody(ChatRequest(config.model, listOf(ChatMessage("system", system), ChatMessage("user", text)), temperature, maxTokens))
            }
            checkResponseStatus(response, config.id)
            val chatResponse = response.body<ChatResponse>()
            val result = chatResponse.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            if (result.isBlank()) {
                throw IllegalStateException("Respon AI kosong atau tidak valid.")
            }
            return result
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            throw formatNetworkException(e, config.id, config.baseUrl)
        }
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
        val root = cleanBaseUrlWithoutV1(config.baseUrl)
        try {
            val response = client.post("$root/v1beta/models/${config.model}:generateContent") {
                contentType(ContentType.Application.Json)
                header("x-goog-api-key", apiKey)
                setBody(GeminiRequest(
                    contents = listOf(GeminiContent(listOf(GeminiPart(text)))),
                    systemInstruction = GeminiContent(listOf(GeminiPart(system)), "system"),
                    generationConfig = GeminiGenerationConfig(temperature = temperature, maxOutputTokens = maxTokens)
                ))
            }
            checkResponseStatus(response, config.id)
            val geminiResponse = response.body<GeminiResponse>()
            val result = geminiResponse.candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text }?.trim().orEmpty()
            if (result.isBlank()) {
                throw IllegalStateException("Respon Gemini kosong atau terblokir filter keamanan.")
            }
            return result
        } catch (e: Exception) {
            if (e is IllegalStateException || e is IllegalArgumentException) throw e
            throw formatNetworkException(e, config.id, config.baseUrl)
        }
    }

    suspend fun testConnection(config: ProviderConfig, apiKey: String?): Result<Unit> = runCatching {
        val modelsResult = fetchModels(config, apiKey)
        if (modelsResult.isFailure) {
            throw modelsResult.exceptionOrNull() ?: IllegalStateException("Gagal terhubung ke API ${config.name}")
        }
    }

    suspend fun fetchModels(config: ProviderConfig, apiKey: String?): Result<List<String>> = runCatching {
        val root = cleanBaseUrl(config.baseUrl)
        try {
            val modelList = when (config.id) {
                PROVIDER_GEMINI -> {
                    require(!apiKey.isNullOrBlank()) { "API key Gemini belum diatur." }
                    val response = client.get("${cleanBaseUrlWithoutV1(config.baseUrl)}/v1beta/models") {
                        header("x-goog-api-key", apiKey)
                    }
                    checkResponseStatus(response, config.id)
                    val resp = response.body<GeminiModelsResponse>()
                    resp.models.map { it.name.removePrefix("models/") }.filter { it.contains("gemini", ignoreCase = true) }
                }
                PROVIDER_OLLAMA -> {
                    try {
                        val response = client.get("$root/v1/models")
                        checkResponseStatus(response, config.id)
                        val resp = response.body<OpenAiModelsResponse>()
                        resp.data.map { it.id }
                    } catch (e: Exception) {
                        if (e is IllegalStateException) throw e
                        val response = client.get("${cleanBaseUrlWithoutV1(config.baseUrl)}/api/tags")
                        checkResponseStatus(response, config.id)
                        val resp = response.body<OllamaModelsResponse>()
                        resp.models.map { it.name }
                    }
                }
                else -> {
                    val response = client.get("$root/v1/models") {
                        apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
                    }
                    checkResponseStatus(response, config.id)
                    val resp = response.body<OpenAiModelsResponse>()
                    resp.data.map { it.id }
                }
            }
            val sortedList = modelList.distinct().sorted()
            if (sortedList.isEmpty()) listOf(config.model) else sortedList
        } catch (e: Exception) {
            if (e is IllegalStateException || e is IllegalArgumentException) throw e
            throw formatNetworkException(e, config.id, config.baseUrl)
        }
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 60_000L
        const val CONNECT_TIMEOUT_MS = 30_000L
        const val SOCKET_TIMEOUT_MS = 60_000L
        const val MAX_ERROR_DETAIL_LENGTH = 200
        const val DEFAULT_TEMPERATURE = 0.3
        const val DEFAULT_MAX_TOKENS = 8192
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OLLAMA = "ollama"
        const val PROVIDER_LMSTUDIO = "lmstudio"
    }
}
