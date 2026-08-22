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
data class GlossaryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val source: String,
    val target: String,
    val note: String = ""
)

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
        "builtin_default",
        "Umum (General)",
        "You are a professional literary translator. Translate the given text into natural, accurate {target}.\n" +
                "Strict Rules:\n" +
                "1. Maintain high fidelity to original meaning and nuance while producing fluent {target}.\n" +
                "2. Preserve original line breaks, paragraph structure, punctuation, and markdown formatting.\n" +
                "3. Do NOT output commentary, notes, or intros. Output ONLY the translated text.\n" +
                "4. Strictly adhere to provided glossary terms if present.",
        "custom",
        "Penerjemahan umum yang akurat dan terstruktur.",
        true
    )
    val prompts = listOf(
        defaultPrompt,
        PromptTemplate(
            "builtin_novel",
            "Novel & Fiction",
            "You are a master translator specializing in Light Novels, Web Novels, and Fiction. Translate the source text into expressive, narrative {target}.\n" +
                    "Strict Rules:\n" +
                    "1. Preserve narrative voice, emotion, speech nuances, and scene atmosphere.\n" +
                    "2. Keep character names, honorifics, and localized titles consistent throughout.\n" +
                    "3. Maintain original paragraph breaks, quotation styles, and dialogue structure exactly.\n" +
                    "4. Output ONLY the translated story narrative without extra chat or notes.",
            "novel",
            "Khusus novel, light novel, dan fiksi naratif.",
            true
        ),
        PromptTemplate(
            "builtin_comic",
            "Komik / Manga / Webtoon",
            "You are a professional comic localizer (Manga, Manhwa, Webtoon). Translate dialogue and sound effects into punchy, natural {target}.\n" +
                    "Strict Rules:\n" +
                    "1. Keep dialogue concise, natural, and expressive for speech bubbles.\n" +
                    "2. Maintain line breaks per speaker block. Do not merge separate dialogue bubbles.\n" +
                    "3. Translate SFX (sound effects) naturally where applicable.\n" +
                    "4. Output ONLY the localized dialogue text without meta comments.",
            "comic",
            "Khusus percakapan komik, manga, manhwa, dan webtoon.",
            true
        ),
        PromptTemplate(
            "builtin_academic",
            "Dokumen & Akademik",
            "You are an expert technical translator. Translate the source text into formal, clear, and precise {target}.\n" +
                    "Strict Rules:\n" +
                    "1. Use professional terminology and formal register suitable for technical or academic literature.\n" +
                    "2. Preserve citation keys, code blocks, technical terms, and formulas intact.\n" +
                    "3. Output ONLY the formal translated content.",
            "academic",
            "Khusus jurnal, artikel, dan dokumen teknis.",
            true
        ),
        PromptTemplate(
            "builtin_ocr",
            "Pembersih Teks OCR",
            "You are an expert OCR text restoration engine.\n" +
                    "Strict Rules:\n" +
                    "1. Repair obvious character recognition errors, broken words, merged spaces, and accidental line splits.\n" +
                    "2. Do NOT translate or summarize the text, and do NOT alter valid original words.\n" +
                    "3. Output ONLY the restored, cleaned text without chat or explanations.",
            "ocr_cleanup",
            "Merapikan hasil scan/OCR yang berantakan.",
            true
        )
    )
    val providers = listOf(
        ProviderConfig("gemini", "Google Gemini", "https://generativelanguage.googleapis.com", "gemini-2.0-flash", true, true),
        ProviderConfig("openai", "OpenAI", "https://api.openai.com", "gpt-4o-mini", true, true),
        ProviderConfig("openaicompatible", "OpenAI Compatible", "https://api.openai.com/v1", "gpt-4o-mini", true, true),
        ProviderConfig("openrouter", "OpenRouter", "https://openrouter.ai/api", "openai/gpt-4o-mini", true, true),
        ProviderConfig("ollama", "Ollama", "http://127.0.0.1:11434", "llama3.2", false, true),
        ProviderConfig("lmstudio", "LM Studio", "http://127.0.0.1:1234", "local-model", false, true)
    )
}
