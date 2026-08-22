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
    val providers = MutableStateFlow(BuiltIns.providers)
    val activeProvider = MutableStateFlow(BuiltIns.providers.first())
    var apiKey: String?
        get() = storage.apiKey(activeProvider.value.id)
        set(value) { if (value.isNullOrBlank()) storage.deleteApiKey(activeProvider.value.id) else storage.saveApiKey(activeProvider.value.id, value) }

    fun setInput(value: String) { _state.value = _state.value.copy(input = value, error = null) }
    fun selectProvider(provider: ProviderConfig) { activeProvider.value = provider }
    fun translate(prompt: PromptTemplate = BuiltIns.defaultPrompt, source: String = _state.value.input, target: String = "Indonesian") {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(input = source, isTranslating = true, isPaused = false, error = null, progress = 0f)
            try {
                val glossary = ""
                val system = prompt.content.replace("{target}", target) + if (glossary.isBlank()) "" else "\nGlossary:\n$glossary"
                val chunks = source.chunked(6000)
                val result = buildString {
                    chunks.forEachIndexed { index, chunk ->
                        while (_state.value.isPaused) delay(200)
                        append(repository.translate(activeProvider.value, apiKey, system, chunk))
                        if (index != chunks.lastIndex) append("\n")
                        _state.value = _state.value.copy(progress = (index + 1).toFloat() / chunks.size)
                    }
                }
                _state.value = _state.value.copy(output = result, isTranslating = false, progress = 1f)
                if (storage.autoSaveHistory) {
                    val updated = listOf(TranslationRecord(UUID.randomUUID().toString(), source.take(120), result, "auto", target, activeProvider.value.id)) + history.value
                    history.value = updated.take(100); storage.saveHistory(history.value)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isTranslating = false, error = e.message ?: "Translation failed")
            }
        }
    }
    fun pause() { _state.value = _state.value.copy(isPaused = true) }
    fun resume() { _state.value = _state.value.copy(isPaused = false) }
    fun cancel() { job?.cancel(); _state.value = _state.value.copy(isTranslating = false, isPaused = false, progress = 0f) }
    fun savePrompt(prompt: PromptTemplate) { prompts.value = (prompts.value.filterNot { it.id == prompt.id } + prompt); storage.savePrompts(prompts.value) }
    fun saveProject(project: TranslationProject) { projects.value = (projects.value.filterNot { it.id == project.id } + project); storage.saveProjects(projects.value) }
    fun setAutoSave(value: Boolean) { storage.autoSaveHistory = value }
    fun autoSave(): Boolean = storage.autoSaveHistory
}
