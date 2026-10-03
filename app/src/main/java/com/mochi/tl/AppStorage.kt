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
import kotlinx.coroutines.withContext
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

    // Single-thread dispatcher untuk write — mencegah lost update ( B-5 ).
    private val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    /** Encrypted prefs dibuat lazy — hanya saat dibutuhkan (first access). */
    private var secureRef: SharedPreferences? = null
    private fun getSecure(): SharedPreferences = secureRef ?: runCatching {
        createEncryptedPrefs(appContext)
    }.getOrElse {
        Log.w("AppStorage", "EncryptedSharedPreferences gagal dibuka; mencoba recreate", it)
        runCatching {
            // Master key kemungkinan rusak — hapus prefs lama agar bisa recreate.
            appContext.deleteSharedPreferences("mochitl_secure")
            createEncryptedPrefs(appContext)
        }.getOrElse {
            Log.e("AppStorage", "Storage aman gagal total — fallback plaintext", it)
            // Fallback sepenuhnya menyadari kondisi rusak: tandai degraded
            // agar UI bisa memperingatkan pengguna, JANGAN diam-diam.
            plain.edit().putBoolean("secure_storage_degraded", true).apply()
            appContext.getSharedPreferences("mochitl_secure_fallback", Context.MODE_PRIVATE)
        }
    }.also { secureRef = it }

    /** True jika penyimpanan kredensial jatuh ke fallback plaintext. */
    fun isSecureStorageDegraded(): Boolean =
        plain.getBoolean("secure_storage_degraded", false)

    /** Database dibuat lazy — baru diakses saat collection flow dipanggil. */
    @Volatile private var dbRef: MochiTlDatabase? = null
    private val dbLock = Any()
    private val db: MochiTlDatabase
        get() = dbRef ?: synchronized(dbLock) {
            dbRef ?:             Room.databaseBuilder(appContext, MochiTlDatabase::class.java, "mochitl.db")
                .addMigrations(MochiTlDatabase.MIGRATION_1_2)
                .build().also { dbRef = it }
        }

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

    /** Cache memori untuk prefs terenkripsi agar UI tidak dekripsi setiap rekomposisi. */
    private val secureCache = mutableMapOf<String, String?>()
    private fun readSecure(key: String): String? {
        return if (secureCache.containsKey(key)) {
            secureCache[key]
        } else {
            getSecure().getString(key, null).also { secureCache[key] = it }
        }
    }

    fun saveApiKey(providerId: String, value: String) {
        secureCache["api_key_$providerId"] = value
        getSecure().edit().putString("api_key_$providerId", value).apply()
    }
    fun apiKey(providerId: String): String? = readSecure("api_key_$providerId")
    fun deleteApiKey(providerId: String) {
        secureCache.remove("api_key_$providerId")
        getSecure().edit().remove("api_key_$providerId").apply()
    }
    fun saveBaseUrl(providerId: String, value: String) {
        secureCache["base_url_$providerId"] = value
        getSecure().edit().putString("base_url_$providerId", value).apply()
    }
    fun baseUrl(providerId: String): String? = readSecure("base_url_$providerId")
    fun deleteBaseUrl(providerId: String) {
        secureCache.remove("base_url_$providerId")
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

    // ===== Cache terjemahan per chunk =====
    suspend fun getCachedTranslation(key: String): TranslationCache? =
        db.translationCacheDao().get(key)

    suspend fun putCachedTranslation(key: String, text: String) {
        db.translationCacheDao().upsert(TranslationCache(key, text))
    }

    suspend fun clearTranslationCache() {
        db.translationCacheDao().clear()
    }

    // ===== Pengaturan umum =====

    var autoSaveHistory: Boolean
        get() = plain.getBoolean("auto_save_history", false)
        set(value) { plain.edit().putBoolean("auto_save_history", value).apply() }

    var darkTheme: Boolean
        get() = plain.getBoolean("pref_dark_theme", true)
        set(value) { plain.edit().putBoolean("pref_dark_theme", value).apply() }

    var oledTheme: Boolean
        get() = plain.getBoolean("pref_oled_theme", false)
        set(value) { plain.edit().putBoolean("pref_oled_theme", value).apply() }

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
