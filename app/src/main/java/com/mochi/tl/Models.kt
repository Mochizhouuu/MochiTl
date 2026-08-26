package com.mochi.tl

import androidx.room.Entity
import androidx.room.PrimaryKey
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

/** Prompt template tersimpan di Room (tabel "prompts"). */
@Serializable
@Entity(tableName = "prompts")
data class PromptTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val category: String,
    val description: String = "",
    val isBuiltIn: Boolean = false
)

/** Istilah glosarium tersimpan di Room (tabel "glossary"). */
@Serializable
@Entity(tableName = "glossary")
data class GlossaryEntry(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val source: String,
    val target: String,
    val note: String = ""
)

/** Project terjemahan tersimpan di Room (tabel "projects"). */
@Serializable
@Entity(tableName = "projects")
data class TranslationProject(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val promptTemplateId: String = "",
    val glossaryIds: List<String> = emptyList(),
    val providerId: String = "gemini",
    val modelId: String = "gemini-2.0-flash",
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "id"
)

/** Riwayat terjemahan tersimpan di Room (tabel "history", dibatasi 100 entri). */
@Serializable
@Entity(tableName = "history")
data class TranslationRecord(
    @PrimaryKey val id: String,
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
        "Gunakan gaya bahasa yang alami, akurat, dan mengalir sesuai konteks.",
        "custom",
        "Penerjemahan umum yang akurat dan terstruktur.",
        true
    )
    val prompts = listOf(
        defaultPrompt,
        PromptTemplate(
            "builtin_novel",
            "Novel & Fiction",
            "Gaya penulisan novel fiksi. Pertahankan nada emosional, narasi ekspresif, nuansa percakapan tokoh, dan konsistensi panggilan/honorifik.",
            "novel",
            "Khusus novel, light novel, dan fiksi naratif.",
            true
        ),
        PromptTemplate(
            "builtin_comic",
            "Komik / Manga / Webtoon",
            "Gaya penerjemahan komik/manga/webtoon. Gunakan kalimat ringkas, komunikatif, dan santai yang cocok untuk balon kata percakapan.",
            "comic",
            "Khusus percakapan komik, manga, manhwa, dan webtoon.",
            true
        ),
        PromptTemplate(
            "builtin_academic",
            "Dokumen & Akademik",
            "Gaya bahasa formal, lugas, dan akademis. Gunakan istilah teknis yang baku dan tepat.",
            "academic",
            "Khusus jurnal, artikel, dan dokumen teknis.",
            true
        ),
        PromptTemplate(
            "builtin_ocr",
            "Pembersih Teks OCR",
            "Perbaiki hasil scan/OCR yang berantakan (kata terputus atau salah baca) dan rapikan tata letaknya tanpa mengubah isi cerita.",
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
