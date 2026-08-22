package com.mochi.tl

import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String,
    val requiresApiKey: Boolean = true,
    val isBuiltIn: Boolean = false
)

@Serializable
data class PromptTemplate(
    val id: String,
    val name: String,
    val content: String,
    val category: String,
    val description: String = "",
    val isBuiltIn: Boolean = false
)

@Serializable
data class GlossaryEntry(val source: String, val target: String, val note: String = "")

@Serializable
data class TranslationProject(
    val id: String,
    val name: String,
    val description: String = "",
    val promptTemplateId: String = "",
    val glossaryIds: List<String> = emptyList(),
    val providerId: String = "gemini",
    val modelId: String = "gemini-2.0-flash",
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "id"
)

@Serializable
data class TranslationRecord(
    val id: String,
    val sourcePreview: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val providerId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class TranslationState(
    val input: String = "",
    val output: String = "",
    val isTranslating: Boolean = false,
    val isPaused: Boolean = false,
    val error: String? = null,
    val progress: Float = 0f
)

object BuiltIns {
    val defaultPrompt = PromptTemplate(
        "builtin_default", "Default Prompt MochiTL",
        "Translate accurately into {target}. Preserve formatting and meaning. Use natural language. Apply glossary terms exactly when provided. Never invent missing information.",
        "custom", "Safe general-purpose translation", true
    )
    val prompts = listOf(
        defaultPrompt,
        PromptTemplate("builtin_novel", "Novel", "Translate narrative prose into natural {target}. Preserve tone, dialogue, names, and paragraph structure.", "novel", "For fiction and novels", true),
        PromptTemplate("builtin_comic", "Komik", "Translate dialogue naturally into {target}. Keep speaker intent, concise phrasing, and panel-friendly line breaks.", "comic", "For comic dialogue", true),
        PromptTemplate("builtin_ocr", "OCR Cleanup", "Clean OCR text only: repair spacing, obvious character recognition errors, and line breaks. Do not translate or infer missing content.", "ocr_cleanup", "Text cleanup only; does not read images", true)
    )
    val providers = listOf(
        ProviderConfig("gemini", "Google Gemini", "https://generativelanguage.googleapis.com", "gemini-2.0-flash", true, true),
        ProviderConfig("openai", "OpenAI", "https://api.openai.com", "gpt-4o-mini", true, true),
        ProviderConfig("openrouter", "OpenRouter", "https://openrouter.ai/api", "openai/gpt-4o-mini", true, true),
        ProviderConfig("ollama", "Ollama", "http://127.0.0.1:11434", "llama3.2", false, true),
        ProviderConfig("lmstudio", "LM Studio", "http://127.0.0.1:1234", "local-model", false, true)
    )
}
