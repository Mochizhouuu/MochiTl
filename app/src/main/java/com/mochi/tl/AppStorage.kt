package com.mochi.tl

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Penyimpanan aplikasi.
 *
 * - Pengaturan sederhana (API key terenkripsi, base URL, model, parameter
 *   generasi, flag auto-save) tetap memakai SharedPreferences.
 * - Koleksi (projects, prompts, glossary, history) kini tersimpan di
 *   database Room ([MochiTlDatabase]) — lebih aman & scalable dibanding
 *   JSON string di SharedPreferences. Data lama dimigrasikan otomatis
 *   sekali saat pertama kali dibuka.
 *
 * API tetap sinkron agar tidak mengubah ViewModel/UI/test; ukuran data
 * koleksi kecil (≤100 riwayat) sehingga biayanya dapat diabaikan.
 */
class AppStorage(context: Context) {
    private val appContext = context.applicationContext
    private val plain: SharedPreferences = context.getSharedPreferences("mochitl_preferences", Context.MODE_PRIVATE)
    private val secure: SharedPreferences = runCatching {
        createEncryptedPrefs(context)
    }.getOrElse {
        runCatching {
            context.deleteSharedPreferences("mochitl_secure")
            createEncryptedPrefs(context)
        }.getOrElse {
            context.getSharedPreferences("mochitl_secure_fallback", Context.MODE_PRIVATE)
        }
    }

    val json = Json { ignoreUnknownKeys = true }

    private val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val db: MochiTlDatabase by lazy {
        Room.databaseBuilder(appContext, MochiTlDatabase::class.java, "mochitl.db")
            .allowMainThreadQueries()
            .build()
    }

    init {
        if (!plain.getBoolean("room_migrated", false)) {
            runBlocking {
                migrateLegacyPrefsToRoom()
            }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "mochitl_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Sekali jalan: pindahkan koleksi JSON lama di prefs ke Room. */
    private suspend fun migrateLegacyPrefsToRoom() {
        if (plain.getBoolean("room_migrated", false)) return
        if (db.projectDao().getAll().isEmpty()) {
            decodeLegacy<TranslationProject>("projects")?.let { db.projectDao().upsertAll(it) }
        }
        if (db.glossaryDao().getAll().isEmpty()) {
            decodeLegacy<GlossaryEntry>("glossary")?.let { db.glossaryDao().upsertAll(it) }
        }
        if (db.historyDao().getAll().isEmpty()) {
            decodeLegacy<TranslationRecord>("history")?.let { db.historyDao().upsertAll(it.take(100)) }
        }
        if (db.promptDao().getAll().isEmpty()) {
            // Prompt lama sudah menyertakan built-ins; gabungkan dengan
            // bawaan agar built-ins selalu ada meski data lama rusak.
            val legacyPrompts = decodeLegacy<PromptTemplate>("prompts").orEmpty()
            db.promptDao().upsertAll(
                (BuiltIns.prompts + legacyPrompts).associateBy { it.id }.values.toList()
            )
        }
        // Key lama dibiarkan sebagai cadangan; penanda mencegah migrasi ulang.
        plain.edit().putBoolean("room_migrated", true).apply()
    }

    private inline fun <reified T> decodeLegacy(key: String): List<T>? =
        plain.getString(key, null)?.let { raw ->
            runCatching { json.decodeFromString<List<T>>(raw) }.getOrNull()
        }

    // ===== Kredensial & pengaturan provider =====

    fun saveApiKey(providerId: String, value: String) = secure.edit().putString("api_key_$providerId", value).apply()
    fun apiKey(providerId: String): String? = secure.getString("api_key_$providerId", null)
    fun deleteApiKey(providerId: String) = secure.edit().remove("api_key_$providerId").apply()
    fun saveBaseUrl(providerId: String, value: String) = secure.edit().putString("base_url_$providerId", value).apply()
    fun baseUrl(providerId: String): String? = secure.getString("base_url_$providerId", null)
    fun deleteBaseUrl(providerId: String) = secure.edit().remove("base_url_$providerId").apply()

    fun saveModel(providerId: String, value: String) = plain.edit().putString("model_$providerId", value).apply()
    fun model(providerId: String): String? = plain.getString("model_$providerId", null)
    fun deleteModel(providerId: String) = plain.edit().remove("model_$providerId").apply()

    // ===== Koleksi di Room =====

    fun saveProjects(items: List<TranslationProject>) = runBlocking {
        db.projectDao().clear(); db.projectDao().upsertAll(items)
    }
    fun projects(): List<TranslationProject> = runBlocking { db.projectDao().getAll() }

    fun savePrompts(items: List<PromptTemplate>) = runBlocking {
        db.promptDao().clear(); db.promptDao().upsertAll(items)
    }
    fun prompts(): List<PromptTemplate> = runBlocking {
        db.promptDao().getAll().ifEmpty { BuiltIns.prompts }
    }

    fun saveHistory(items: List<TranslationRecord>) = runBlocking {
        db.historyDao().clear(); db.historyDao().upsertAll(items.take(100))
    }
    fun history(): List<TranslationRecord> = runBlocking { db.historyDao().getAll() }

    fun saveGlossary(items: List<GlossaryEntry>) = runBlocking {
        db.glossaryDao().clear(); db.glossaryDao().upsertAll(items)
    }
    fun glossary(): List<GlossaryEntry> = runBlocking { db.glossaryDao().getAll() }

    // ===== Pengaturan umum =====

    var autoSaveHistory: Boolean
        get() = plain.getBoolean("auto_save_history", false)
        set(value) { plain.edit().putBoolean("auto_save_history", value).apply() }

    /** Kreativitas model (0.0 konsisten .. 1.5 kreatif), dipakai semua provider. */
    var temperature: Float
        get() = plain.getFloat("temperature", 0.3f)
        set(value) { plain.edit().putFloat("temperature", value).apply() }

    /** Batas token output per permintaan/chunk. */
    var maxTokens: Int
        get() = plain.getInt("max_tokens", 8192)
        set(value) { plain.edit().putInt("max_tokens", value).apply() }
}
