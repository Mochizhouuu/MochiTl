package com.mochi.tl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.UUID

/**
 * Memecah teks menjadi chunk <= [maxChars] dengan TETAP menghormati batas
 * paragraf/baris — bukan pemotongan paksa di tengah kata/kalimat yang
 * merusak konteks terjemahan. Paragraf tunggal yang lebih panjang dari
 * [maxChars] dipotong paksa hanya sebagai jalan terakhir.
 */
internal fun chunkByParagraphs(text: String, maxChars: Int = 4000): List<String> {
    if (text.isBlank()) return emptyList()
    if (text.length <= maxChars) return listOf(text)

    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun flushCurrent() {
        if (current.isNotEmpty()) {
            chunks.add(current.toString())
            current.clear()
        }
    }

    for (paragraph in text.split("\n")) {
        when {
            // Jalan terakhir: paragraf tunggal melebihi batas → potong paksa.
            paragraph.length + 1 > maxChars -> {
                flushCurrent()
                var start = 0
                while (start < paragraph.length) {
                    val end = minOf(start + maxChars, paragraph.length)
                    chunks.add(paragraph.substring(start, end))
                    start = end
                }
            }
            // Paragraf berikutnya tidak muat → simpan buffer dulu.
            current.length + paragraph.length + 1 > maxChars -> {
                flushCurrent()
                current.append(paragraph)
            }
            else -> {
                if (current.isNotEmpty()) current.append('\n')
                current.append(paragraph)
            }
        }
    }
    flushCurrent()

    return chunks.filter { it.isNotBlank() }.ifEmpty { listOf(text) }
}

class MochiViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = AppStorage(app)
    private val repository = TranslationRepository()

    /**
     * Seluruh I/O penyimpanan (baca awal + tulis) berjalan di sini — TIDAK
     * pernah memblokir thread utama. StateFlow tetap dimutasi sinkron sebagai
     * sumber kebenaran UI; Room hanya lapisan durabilitas.
     */
    private val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Koleksi yang sudah diedit sejak proses start — hidrasi awal tidak boleh menimpanya. */
    private val editedCollections: MutableSet<String> =
        java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private fun persistAsync(vararg collections: String, block: suspend () -> Unit) {
        collections.forEach { editedCollections.add(it) }
        storageScope.launch { block() }
    }

    init {
        // Hidrasi awal dari Room. Jika user sempat mengubah sesuatu sebelum
        // baca selesai (nyaris mustahil, tapi aman), data lokal yang menang.
        storageScope.launch {
            if ("projects" !in editedCollections) projects.value = storage.projects()
            if ("prompts" !in editedCollections) prompts.value = storage.prompts()
            if ("history" !in editedCollections) history.value = storage.history()
            if ("glossary" !in editedCollections) glossary.value = storage.glossary()
        }
    }
    private val _state = MutableStateFlow(TranslationState())
    val state: StateFlow<TranslationState> = _state.asStateFlow()
    private var job: Job? = null
    val projects = MutableStateFlow(storage.projects())
    val prompts = MutableStateFlow(storage.prompts())
    val history = MutableStateFlow(storage.history())
    val glossary = MutableStateFlow(storage.glossary())
    val providers = MutableStateFlow(BuiltIns.providers)
    val activeProvider = MutableStateFlow(BuiltIns.providers.first())
    val activeProject = MutableStateFlow<TranslationProject?>(null)
    val activePrompt = MutableStateFlow(BuiltIns.defaultPrompt)

    val availableModels = MutableStateFlow<List<String>>(emptyList())

    var apiKey: String?
        get() = storage.apiKey(activeProvider.value.id)
        set(value) { if (value.isNullOrBlank()) storage.deleteApiKey(activeProvider.value.id) else storage.saveApiKey(activeProvider.value.id, value) }

    var customBaseUrl: String?
        get() = storage.baseUrl(activeProvider.value.id)
        set(value) { if (value.isNullOrBlank()) storage.deleteBaseUrl(activeProvider.value.id) else storage.saveBaseUrl(activeProvider.value.id, value) }

    var customModel: String?
        get() = storage.model(activeProvider.value.id)
        set(value) { if (value.isNullOrBlank()) storage.deleteModel(activeProvider.value.id) else storage.saveModel(activeProvider.value.id, value) }

    /** Kreativitas model 0.0..1.5 — berlaku untuk semua provider. */
    var generationTemperature: Float
        get() = storage.temperature
        set(value) { storage.temperature = value.coerceIn(0f, 1.5f) }

    /** Batas token output per chunk. */
    var generationMaxTokens: Int
        get() = storage.maxTokens
        set(value) { storage.maxTokens = value.coerceIn(256, 32768) }

    fun storageModelFor(providerId: String): String? = storage.model(providerId)

    fun setInput(value: String) { _state.value = _state.value.copy(input = value, error = null) }
    fun setOutput(value: String) { _state.value = _state.value.copy(output = value) }
    fun selectProvider(provider: ProviderConfig) { activeProvider.value = provider }
    fun setModelForActiveProvider(model: String) {
        customModel = model
    }

    suspend fun fetchModelsForActiveProvider(): Result<List<String>> {
        val currentProvider = activeProvider.value.let {
            val customUrl = customBaseUrl
            if (!customUrl.isNullOrBlank()) it.copy(baseUrl = customUrl) else it
        }
        val result = repository.fetchModels(currentProvider, apiKey)
        if (result.isSuccess) {
            availableModels.value = result.getOrDefault(emptyList())
        }
        return result
    }
    fun selectProject(project: TranslationProject?) {
        activeProject.value = project
        project?.let { proj ->
            prompts.value.find { it.id == proj.promptTemplateId }?.let { activePrompt.value = it }
            providers.value.find { it.id == proj.providerId }?.let { activeProvider.value = it }
        }
    }
    fun selectPrompt(prompt: PromptTemplate) { activePrompt.value = prompt }

    fun translate(
        prompt: PromptTemplate = activePrompt.value,
        source: String = _state.value.input,
        sourceLanguage: String = LanguageOptions.AUTO_DETECT,
        target: String = "Indonesia",
        project: TranslationProject? = activeProject.value
    ) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(input = source, isTranslating = true, isPaused = false, error = null, progress = 0f)
            try {
                val systemPrompt = PromptBuilder.buildSystemPrompt(
                    prompt = prompt,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = target,
                    glossaryList = glossary.value,
                    project = project
                )
                val currentProvider = activeProvider.value.let { prov ->
                    val customUrl = customBaseUrl
                    val modelToUse = customModel?.takeIf { it.isNotBlank() } ?: prov.model
                    var res = prov.copy(model = modelToUse)
                    if (!customUrl.isNullOrBlank()) res = res.copy(baseUrl = customUrl)
                    res
                }

                val chunks = chunkByParagraphs(source)
                val results = MutableList(chunks.size) { "" }

                chunks.forEachIndexed { index, chunk ->
                    while (_state.value.isPaused) delay(200)

                    try {
                        results[index] = translateChunkWithRetry(
                            provider = currentProvider,
                            systemPrompt = systemPrompt,
                            chunk = PromptBuilder.formatChunkText(chunk)
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Gagal setelah semua percobaan ulang: simpan hasil
                        // parsial agar kerja user tidak hilang, lalu laporkan.
                        val partial = results.filter { it.isNotBlank() }.joinToString("\n")
                        _state.value = _state.value.copy(
                            output = partial,
                            isTranslating = false,
                            error = "Gagal di bagian ${index + 1}/${chunks.size}: ${e.message ?: "terjemahan gagal"}. Hasil sebagian sudah disimpan."
                        )
                        return@launch
                    }

                    _state.value = _state.value.copy(progress = (index + 1).toFloat() / chunks.size)
                }

                val result = results.joinToString("\n")
                _state.value = _state.value.copy(output = result, isTranslating = false, progress = 1f)
                if (storage.autoSaveHistory && result.isNotBlank()) {
                    val updated = listOf(TranslationRecord(UUID.randomUUID().toString(), source.take(120), result, sourceLanguage, target, currentProvider.id)) + history.value
                    history.value = updated.take(100)
                    persistAsync("history") { storage.saveHistory(history.value) }
                }
            } catch (e: CancellationException) {
                // Pembatalan oleh user (cancel()) bukan error — jangan timpa
                // pesan state dengan teks exception.
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(isTranslating = false, error = e.message ?: "Terjemahan gagal")
            }
        }
    }

    suspend fun testConnection(): Result<Unit> {
        val currentProvider = activeProvider.value.let { prov ->
            val customUrl = customBaseUrl
            val modelToUse = customModel?.takeIf { it.isNotBlank() } ?: prov.model
            var res = prov.copy(model = modelToUse)
            if (!customUrl.isNullOrBlank()) res = res.copy(baseUrl = customUrl)
            res
        }
        return repository.testConnection(currentProvider, apiKey)
    }

    fun pause() { _state.value = _state.value.copy(isPaused = true) }
    fun resume() { _state.value = _state.value.copy(isPaused = false) }
    fun cancel() { job?.cancel(); _state.value = _state.value.copy(isTranslating = false, isPaused = false, progress = 0f) }

    /**
     * Menerjemahkan satu chunk dengan percobaan ulang otomatis (maks
     * [MAX_ATTEMPTS] kali) plus backoff progresif — menangani error sementara
     * seperti rate limit 429 atau jaringan terputus tanpa langsung gagal.
     */
    private suspend fun translateChunkWithRetry(
        provider: ProviderConfig,
        systemPrompt: String,
        chunk: String,
    ): String {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return repository.translate(
                    provider,
                    apiKey,
                    systemPrompt,
                    chunk,
                    temperature = generationTemperature.toDouble(),
                    maxTokens = generationMaxTokens
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_BACKOFF_MS * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Terjemahan gagal")
    }

    fun savePrompt(prompt: PromptTemplate) {
        val updated = prompts.value.filterNot { it.id == prompt.id } + prompt
        prompts.value = updated
        persistAsync("prompts") { storage.savePrompts(updated) }
        if (activePrompt.value.id == prompt.id) {
            activePrompt.value = prompt
        }
    }
    fun deletePrompt(id: String) {
        val updated = prompts.value.filterNot { it.id == id && !it.isBuiltIn }
        prompts.value = updated
        persistAsync("prompts") { storage.savePrompts(updated) }
        if (activePrompt.value.id == id) {
            activePrompt.value = updated.firstOrNull() ?: BuiltIns.defaultPrompt
        }
    }
    fun resetPromptsToDefault() {
        prompts.value = BuiltIns.prompts
        persistAsync("prompts") { storage.savePrompts(prompts.value) }
        if (prompts.value.none { it.id == activePrompt.value.id }) {
            activePrompt.value = BuiltIns.defaultPrompt
        }
    }

    /**
     * Duplikat prompt (termasuk built-in) menjadi salinan kustom yang bisa
     * diedit. @return prompt baru, atau null jika id tidak ditemukan.
     */
    fun duplicatePrompt(id: String): PromptTemplate? {
        val source = prompts.value.firstOrNull { it.id == id } ?: return null
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = "${source.name} (salinan)",
            isBuiltIn = false
        )
        savePrompt(copy)
        activePrompt.value = copy
        return copy
    }

    /** Ekspor seluruh prompt (built-in + kustom) sebagai JSON. */
    fun exportPromptsJson(): String = storage.json.encodeToString(prompts.value)

    /**
     * Impor prompt dari JSON hasil ekspor. Entri dengan id yang sama akan
     * ditimpa; built-in bawaan selalu dipertahankan. @return jumlah entri diimpor.
     */
    fun importPromptsJson(jsonContent: String): Result<Int> = runCatching {
        val importedList = storage.json.decodeFromString<List<PromptTemplate>>(jsonContent)
        val current = prompts.value.associateBy { it.id }.toMutableMap()
        var importedCount = 0
        for (item in importedList) {
            if (item.content.isBlank()) continue
            val valid = item.copy(
                id = item.id.ifBlank { UUID.randomUUID().toString() },
                name = item.name.trim().ifBlank { "Prompt tanpa nama" },
                content = item.content.trim(),
                category = item.category.trim().ifBlank { "custom" }
            )
            current[valid.id] = valid
            importedCount++
        }
        BuiltIns.prompts.forEach { builtin -> current.putIfAbsent(builtin.id, builtin) }
        val newList = current.values.toList()
        prompts.value = newList
        persistAsync("prompts") { storage.savePrompts(newList) }
        importedCount
    }

    fun saveGlossaryItem(entry: GlossaryEntry) {
        glossary.value = (glossary.value.filterNot { it.id == entry.id } + entry)
        persistAsync("glossary") { storage.saveGlossary(glossary.value) }
    }
    fun deleteGlossaryItem(id: String) {
        glossary.value = glossary.value.filterNot { it.id == id }
        persistAsync("glossary") { storage.saveGlossary(glossary.value) }
    }

    fun exportGlossaryJson(): String {
        return storage.json.encodeToString(glossary.value)
    }

    fun importGlossaryJson(jsonContent: String): Result<Int> = runCatching {
        val importedList = storage.json.decodeFromString<List<GlossaryEntry>>(jsonContent)
        val current = glossary.value.associateBy { it.id }.toMutableMap()
        var addedCount = 0
        for (item in importedList) {
            val validItem = item.copy(
                id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
                source = item.source.trim(),
                target = item.target.trim(),
                note = item.note.trim()
            )
            if (validItem.source.isNotBlank() && validItem.target.isNotBlank()) {
                current[validItem.id] = validItem
                addedCount++
            }
        }
        val newList = current.values.toList()
        glossary.value = newList
        persistAsync("glossary") { storage.saveGlossary(newList) }
        addedCount
    }

    fun saveProject(project: TranslationProject) {
        projects.value = (projects.value.filterNot { it.id == project.id } + project)
        persistAsync("projects") { storage.saveProjects(projects.value) }
    }
    fun deleteProject(id: String) {
        projects.value = projects.value.filterNot { it.id == id }
        persistAsync("projects") { storage.saveProjects(projects.value) }
        if (activeProject.value?.id == id) activeProject.value = null
    }

    fun deleteHistoryItem(id: String) {
        history.value = history.value.filterNot { it.id == id }
        persistAsync("history") { storage.saveHistory(history.value) }
    }
    fun clearHistory() {
        history.value = emptyList()
        persistAsync("history") { storage.saveHistory(emptyList()) }
    }

    fun setAutoSave(value: Boolean) { storage.autoSaveHistory = value }
    fun autoSave(): Boolean = storage.autoSaveHistory

    override fun onCleared() {
        storageScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val RETRY_BACKOFF_MS = 1500L
    }
}
