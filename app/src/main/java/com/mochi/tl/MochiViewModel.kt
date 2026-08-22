package com.mochi.tl

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.UUID

class MochiViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = AppStorage(app)
    private val repository = TranslationRepository()
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
        target: String = "Indonesia",
        project: TranslationProject? = activeProject.value
    ) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(input = source, isTranslating = true, isPaused = false, error = null, progress = 0f)
            try {
                val glossaryList = glossary.value
                val activeGlossary = if (project != null && project.glossaryIds.isNotEmpty()) {
                    glossaryList.filter { it.id in project.glossaryIds }
                } else {
                    glossaryList
                }

                val glossaryContext = if (activeGlossary.isNotEmpty()) {
                    "\n\nGlossary Mapping (Strictly enforce these exact term translations):\n" +
                            activeGlossary.joinToString("\n") { "- ${it.source} -> ${it.target}" + if (it.note.isNotBlank()) " (${it.note})" else "" }
                } else ""

                val systemPrompt = prompt.content.replace("{target}", target) + glossaryContext
                val currentProvider = activeProvider.value.let { prov ->
                    val customUrl = customBaseUrl
                    val modelToUse = customModel?.takeIf { it.isNotBlank() } ?: prov.model
                    var res = prov.copy(model = modelToUse)
                    if (!customUrl.isNullOrBlank()) res = res.copy(baseUrl = customUrl)
                    res
                }

                val chunks = source.chunked(4000)
                val result = buildString {
                    chunks.forEachIndexed { index, chunk ->
                        while (_state.value.isPaused) delay(200)
                        val formattedChunk = "<source_text>\n$chunk\n</source_text>"
                        append(repository.translate(currentProvider, apiKey, systemPrompt, formattedChunk))
                        if (index != chunks.lastIndex) append("\n")
                        _state.value = _state.value.copy(progress = (index + 1).toFloat() / chunks.size)
                    }
                }
                _state.value = _state.value.copy(output = result, isTranslating = false, progress = 1f)
                if (storage.autoSaveHistory && result.isNotBlank()) {
                    val updated = listOf(TranslationRecord(UUID.randomUUID().toString(), source.take(120), result, "auto", target, currentProvider.id)) + history.value
                    history.value = updated.take(100)
                    storage.saveHistory(history.value)
                }
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

    fun savePrompt(prompt: PromptTemplate) {
        val updated = prompts.value.filterNot { it.id == prompt.id } + prompt
        prompts.value = updated
        storage.savePrompts(updated)
        if (activePrompt.value.id == prompt.id) {
            activePrompt.value = prompt
        }
    }
    fun deletePrompt(id: String) {
        val updated = prompts.value.filterNot { it.id == id && !it.isBuiltIn }
        prompts.value = updated
        storage.savePrompts(updated)
        if (activePrompt.value.id == id) {
            activePrompt.value = updated.firstOrNull() ?: BuiltIns.defaultPrompt
        }
    }
    fun resetPromptsToDefault() {
        prompts.value = BuiltIns.prompts
        storage.savePrompts(prompts.value)
        if (prompts.value.none { it.id == activePrompt.value.id }) {
            activePrompt.value = BuiltIns.defaultPrompt
        }
    }

    fun saveGlossaryItem(entry: GlossaryEntry) {
        glossary.value = (glossary.value.filterNot { it.id == entry.id } + entry)
        storage.saveGlossary(glossary.value)
    }
    fun deleteGlossaryItem(id: String) {
        glossary.value = glossary.value.filterNot { it.id == id }
        storage.saveGlossary(glossary.value)
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
        storage.saveGlossary(newList)
        addedCount
    }

    fun saveProject(project: TranslationProject) {
        projects.value = (projects.value.filterNot { it.id == project.id } + project)
        storage.saveProjects(projects.value)
    }
    fun deleteProject(id: String) {
        projects.value = projects.value.filterNot { it.id == id }
        storage.saveProjects(projects.value)
        if (activeProject.value?.id == id) activeProject.value = null
    }

    fun deleteHistoryItem(id: String) {
        history.value = history.value.filterNot { it.id == id }
        storage.saveHistory(history.value)
    }
    fun clearHistory() {
        history.value = emptyList()
        storage.saveHistory(emptyList())
    }

    fun setAutoSave(value: Boolean) { storage.autoSaveHistory = value }
    fun autoSave(): Boolean = storage.autoSaveHistory
}
