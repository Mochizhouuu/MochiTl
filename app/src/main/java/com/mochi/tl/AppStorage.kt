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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Log
import androidx.room.withTransaction

/**
 * Penyimpanan aplikasi — semua inisialisasi berat (EncryptedPrefs, Room DB)
 * dilakukan secara lazy/asinkron agar tidak memblokir main thread saat startup.
 */
class AppStorage(context: Context) {
    private val appContext = context.applicationContext
    private val plain: SharedPreferences = context.getSharedPreferences("mochitl_preferences", Context.MODE_PRIVATE)

    val json = Json { ignoreUnknownKeys = true }

    private val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Encrypted prefs dibuat lazy — hanya saat dibutuhkan (first access). */
    private var secureRef: SharedPreferences? = null
    private fun getSecure(): SharedPreferences = secureRef ?: runCatching {
        createEncryptedPrefs(appContext)
    }.getOrElse {
        runCatching {
            appContext.deleteSharedPreferences("mochitl_secure")
            createEncryptedPrefs(appContext)
        }.getOrElse {
            appContext.getSharedPreferences("mochitl_secure_fallback", Context.MODE_PRIVATE)
        }
    }.also { secureRef = it }

    /** Database dibuat lazy — baru diakses saat collection flow dipanggil. */
    private var dbRef: MochiTlDatabase? = null
    private val db: MochiTlDatabase
        get() = dbRef ?: run {
            Room.databaseBuilder(appContext, MochiTlDatabase::class.java, "mochitl.db")
                .build()
        }.also { dbRef = it }

    init {
        // Migration dilakukan sekali di background, bukan di main thread.
        storageScope.launch {
            try {
                if (!plain.getBoolean("room_migrated", false)) {
                    migrateLegacyPrefsToRoom()
                }
            } catch (e: Exception) {
                Log.w("AppStorage", "Migrasi legacy prefs ke Room gagal", e)
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
    // Semua akses ke secure prefs tetap async — UI tidak menunggu.

    fun saveApiKey(providerId: String, value: String) {
        getSecure().edit().putString("api_key_$providerId", value).apply()
    }
    fun apiKey(providerId: String): String? = getSecure().getString("api_key_$providerId", null)
    fun deleteApiKey(providerId: String) {
        getSecure().edit().remove("api_key_$providerId").apply()
    }
    fun saveBaseUrl(providerId: String, value: String) {
        getSecure().edit().putString("base_url_$providerId", value).apply()
    }
    fun baseUrl(providerId: String): String? = getSecure().getString("base_url_$providerId", null)
    fun deleteBaseUrl(providerId: String) {
        getSecure().edit().remove("base_url_$providerId").apply()
    }

    fun saveModel(providerId: String, value: String) = plain.edit().putString("model_$providerId", value).apply()
    fun model(providerId: String): String? = plain.getString("model_$providerId", null)
    fun deleteModel(providerId: String) = plain.edit().remove("model_$providerId").apply()

    // ===== Data koleksi asinkron (Flow) — TIDAK memblokir main thread =====

    fun projectsFlow(): Flow<List<TranslationProject>> = flow { emit(db.projectDao().getAll()) }.flowOn(Dispatchers.IO)
    fun promptsFlow(): Flow<List<PromptTemplate>> = flow { emit(db.promptDao().getAll().ifEmpty { BuiltIns.prompts }) }.flowOn(Dispatchers.IO)
    fun historyFlow(): Flow<List<TranslationRecord>> = flow { emit(db.historyDao().getAll()) }.flowOn(Dispatchers.IO)
    fun glossaryFlow(): Flow<List<GlossaryEntry>> = flow { emit(db.glossaryDao().getAll()) }.flowOn(Dispatchers.IO)

    // ===== Write operations (async, transactional) =====

    private fun persistReplace(action: suspend MochiTlDatabase.() -> Unit) {
        storageScope.launch {
            try {
                db.withTransaction { action(db) }
            } catch (e: Exception) {
                Log.w("AppStorage", "Gagal menyimpan koleksi ke Room", e)
            }
        }
    }

    fun saveProjectsAsync(items: List<TranslationProject>) {
        persistReplace {
            projectDao().clear()
            projectDao().upsertAll(items)
        }
    }

    fun savePromptsAsync(items: List<PromptTemplate>) {
        persistReplace {
            promptDao().clear()
            promptDao().upsertAll(items)
        }
    }

    fun saveHistoryAsync(items: List<TranslationRecord>) {
        persistReplace {
            historyDao().clear()
            historyDao().upsertAll(items.take(MAX_HISTORY_ITEMS))
        }
    }

    fun saveGlossaryAsync(items: List<GlossaryEntry>) {
        persistReplace {
            glossaryDao().clear()
            glossaryDao().upsertAll(items)
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
        set(value) { plain.edit().putInt("max_tokens", value.coerceIn(MIN_MAX_TOKENS, MAX_MAX_TOKENS)).apply() }

    private companion object {
        const val MAX_HISTORY_ITEMS = 100
        const val MIN_MAX_TOKENS = 256
        const val MAX_MAX_TOKENS = 32768
    }
}
