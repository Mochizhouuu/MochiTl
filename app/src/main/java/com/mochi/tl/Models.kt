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
        """
| Aturan Gaya Terjemahan Umum
|=============================
| 1. Gunakan gaya bahasa yang alami, akurat, dan mengalir sesuai konteks.
| 2. Pertahankan nada dan register teks asli (formal/santai).
| 3. Jangan terjemahkan nama diri — romanisasi sesuai bahasa aslinya.
| 4. Adaptasi idiom dan peribahasa ke padanan {target} yang natural.
| 5. Jika ada istilah yang tidak memiliki padanan langsung, pertahankan istilah asli dengan penjelasan singkat pada kemunculan pertama.
        """.trimIndent(),
        "custom",
        "Penerjemahan umum yang akurat dan terstruktur.",
        true
    )
    val prompts = listOf(
        defaultPrompt,
        PromptTemplate(
            "builtin_novel",
            "Novel & Fiction",
            """
| Aturan Gaya Novel & Fiksi
|===========================
| 1. Jaga voice naratif — setiap tokoh harus memiliki "suara" yang konsisten.
| 2. Dialog: buat natural seperti percakapan asli, hindari terjemahan kaku.
| 3. Pertahankan honorifik Jepang/Korea (-san, -kun, -sama, oppa, unnie, dll).
| 4. Onomatopoeia: gunakan padanan {target} jika ada, kalau tidak deskripsikan dalam teks.
| 5. Monolog internal: sesuaikan konvensi narasi {target} (e.g., miring untuk pikiran).
| 6. Judul bab, bagian, dan header tetap dalam bahasa aslinya kecuali sudah ada versi {target} yang mapan.
| 7. Untuk light novel: pertahankan nuansa "terjemahan" yang umum di komunitas fiksi terjemah {target}.
| 8. Konsistensi: istilah yang sama harus diterjemahkan sama di seluruh teks.
        """.trimIndent(),
        "novel",
        "Khusus novel, light novel, dan fiksi naratif.",
        true
        ),
        PromptTemplate(
            "builtin_comic",
            "Komik / Manga / Webtoon",
            """
| Aturan Gaya Komik / Manga / Webtoon
|=====================================
| 1. Kalimat harus RINGKAS dan KOMUNIKATIF — sesuai ruang balon kata.
| 2. Prioritaskan dampak visual: kalimat pendek untuk momen dramatis.
| 3. Honorifik dan sapaan TETAP (san, kun, sama, oppa, unnie, etc).
| 4. Narasi kotak: gunakan voice yang konsisten berbeda dari dialog tokoh.
| 5. Sound effect (SFX): lokalisi ke {target} jika natural, otherwise pertahankan asli.
| 6. Webtoon vertikal: perhatikan flow scroll — hindari kalimat yang terlalu panjang per panel.
| 7. Slang & bahasa gaul: adaptasi ke budaya {target} tanpa menghilangkan karakter tokoh.
| 8. Jangan-translate nama jurus/skill ke {target} — gunakan romanisasi/terjemahan Inggris sesuai konvensi.
        """.trimIndent(),
        "comic",
        "Khusus percakapan komik, manga, manhwa, dan webtoon.",
        true
        ),
        PromptTemplate(
            "builtin_academic",
            "Dokumen & Akademik",
            """
| Aturan Gaya Dokumen & Akademik
|================================
| 1. Gunakan bahasa {target} formal, lugas, dan akademis.
| 2. Istilah teknis: gunakan terminologi baku {target} yang telah mapan.
| 3. Jika tidak ada padanan baku, pertahankan istilah Inggris dalam *miring*.
| 4. Pertahankan format sitasi, footnote, dan referensi persis seperti aslinya.
| 5. Angka dan satuan: ikuti konvensi {target} (koma desimal, satuan metrik).
| 6. Janganparafrase isi akademik — terjemahkan seakurat mungkin tanpa menambah/mengurangi makna.
| 7. Judul jurnal, nama konferensi, dan nama institusi tetap dalam bahasa aslinya.
        """.trimIndent(),
        "academic",
        "Khusus jurnal, artikel, dan dokumen teknis.",
        true
        ),
        PromptTemplate(
            "builtin_ocr",
            "Pembersih Teks OCR",
            """
| Aturan Pembersihan OCR
|========================
| 1. PERBAIKI kesalahan OCR: kata terputus, huruf salah baca, spasi berlebih.
| 2. JANGAN ubah isi cerita, dialog, atau makna — hanya rapikan bentuk teks.
| 3. Pulihkan paragraph break yang hilang akibat scan.
| 4. Jika karakter tidak terbaca sama sekali, ganti dengan [?].
| 5. Pertahankan semua nama tokoh, tempat, dan istilah khusus — jangan "diperbaiki" ke ejaan lain.
| 6. Rapikan punctuation yang berantakan (titik koma, tanda kutip, dll).
| 7. Output tetap dalam bahasa sumber — ini adalah tahap pembersihan sebelum terjemahan.
        """.trimIndent(),
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
