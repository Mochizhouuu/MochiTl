package com.mochi.tl

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Penyimpanan aplikasi.
 *
 * - Kredensial & pengaturan sederhana → EncryptedSharedPreferences / SharedPreferences.
 * - Koleksi (projects, prompts, glossary, history) → Room database.
 *
 * Semua pembacaan koleksi dilakukan secara asinkron agar tidak memblokir
 * thread utama saat aplikasi dibuka.
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
            val legacyPrompts = decodeLegacy<PromptTemplate>("prompts").orEmpty()
            db.promptDao().upsertAll(
                (BuiltIns.prompts + legacyPrompts).associateBy { it.id }.values.toList()
            )
        }
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

    // ===== Data koleksi asinkron (Flow) — TIDAK memblokir main thread =====

    fun projectsFlow(): Flow<List<TranslationProject>> = flow { emit(db.projectDao().getAll()) }.flowOn(Dispatchers.IO)
    fun promptsFlow(): Flow<List<PromptTemplate>> = flow { emit(db.promptDao().getAll().ifEmpty { BuiltIns.prompts }) }.flowOn(Dispatchers.IO)
    fun historyFlow(): Flow<List<TranslationRecord>> = flow { emit(db.historyDao().getAll()) }.flowOn(Dispatchers.IO)
    fun glossaryFlow(): Flow<List<GlossaryEntry>> = flow { emit(db.glossaryDao().getAll()) }.flowOn(Dispatchers.IO)

    // ===== Write operations (async) =====

    fun saveProjectsAsync(items: List<TranslationProject>) {
        storageScope.launch {
            db.projectDao().clear()
            db.projectDao().upsertAll(items)
        }
    }

    fun savePromptsAsync(items: List<PromptTemplate>) {
        storageScope.launch {
            db.promptDao().clear()
            db.promptDao().upsertAll(items)
        }
    }

    fun saveHistoryAsync(items: List<TranslationRecord>) {
        storageScope.launch {
            db.historyDao().clear()
            db.historyDao().upsertAll(items.take(100))
        }
    }

    fun saveGlossaryAsync(items: List<GlossaryEntry>) {
        storageScope.launch {
            db.glossaryDao().clear()
            db.glossaryDao().upsertAll(items)
        }
    }

    // ===== Pengaturan umum =====

    var autoSaveHistory: Boolean
        get() = plain.getBoolean("auto_save_history", false)
        set(value) { plain.edit().putBoolean("auto_save_history", value).apply() }

    var temperature: Float
        get() = plain.getFloat("temperature", 0.3f)
        set(value) { plain.edit().putFloat("temperature", value).apply() }

    var maxTokens: Int
        get() = plain.getInt("max_tokens", 8192)
        set(value) { plain.edit().putInt("max_tokens", value).apply() }
}
