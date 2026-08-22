package com.mochi.tl

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppStorage(context: Context) {
    private val plain = context.getSharedPreferences("mochitl_preferences", Context.MODE_PRIVATE)
    private val secure = runCatching {
        EncryptedSharedPreferences.create(
            context,
            "mochitl_secure",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { context.getSharedPreferences("mochitl_secure_fallback", Context.MODE_PRIVATE) }
    private val json = Json { ignoreUnknownKeys = true }

    fun saveApiKey(providerId: String, value: String) = secure.edit().putString("api_key_$providerId", value).apply()
    fun apiKey(providerId: String): String? = secure.getString("api_key_$providerId", null)
    fun deleteApiKey(providerId: String) = secure.edit().remove("api_key_$providerId").apply()
    fun saveBaseUrl(providerId: String, value: String) = secure.edit().putString("base_url_$providerId", value).apply()
    fun baseUrl(providerId: String): String? = secure.getString("base_url_$providerId", null)
    fun deleteBaseUrl(providerId: String) = secure.edit().remove("base_url_$providerId").apply()

    fun saveProjects(items: List<TranslationProject>) = plain.edit().putString("projects", json.encodeToString(items)).apply()
    fun projects(): List<TranslationProject> = plain.getString("projects", null)?.let { runCatching { json.decodeFromString<List<TranslationProject>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
    fun savePrompts(items: List<PromptTemplate>) = plain.edit().putString("prompts", json.encodeToString(items)).apply()
    fun prompts(): List<PromptTemplate> = plain.getString("prompts", null)?.let { runCatching { json.decodeFromString<List<PromptTemplate>>(it) }.getOrDefault(BuiltIns.prompts) } ?: BuiltIns.prompts
    fun saveHistory(items: List<TranslationRecord>) = plain.edit().putString("history", json.encodeToString(items.take(100))).apply()
    fun history(): List<TranslationRecord> = plain.getString("history", null)?.let { runCatching { json.decodeFromString<List<TranslationRecord>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
    fun saveGlossary(items: List<GlossaryEntry>) = plain.edit().putString("glossary", json.encodeToString(items)).apply()
    fun glossary(): List<GlossaryEntry> = plain.getString("glossary", null)?.let { runCatching { json.decodeFromString<List<GlossaryEntry>>(it) }.getOrDefault(emptyList()) } ?: emptyList()
    var autoSaveHistory: Boolean
        get() = plain.getBoolean("auto_save_history", false)
        set(value) { plain.edit().putBoolean("auto_save_history", value).apply() }
}
