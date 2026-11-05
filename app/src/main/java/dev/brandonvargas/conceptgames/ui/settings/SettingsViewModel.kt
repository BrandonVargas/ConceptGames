package dev.brandonvargas.conceptgames.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.gemini.GeminiClient
import dev.brandonvargas.conceptgames.data.settings.Settings
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(val settings: Settings? = null, val models: List<String> = DEFAULT_MODELS)

private val DEFAULT_MODELS = listOf(GeminiClient.DEFAULT_MODEL, GeminiClient.FALLBACK_MODEL)

class SettingsViewModel(private val settings: SettingsRepository, private val gemini: GeminiClient) : ViewModel() {
    private val models = MutableStateFlow(DEFAULT_MODELS)

    val state: StateFlow<SettingsState> = combine(settings.settings, models) { current, models ->
        SettingsState(current, (models + current.model).distinct())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    /** Lists the models this key can use, so newer free models can be picked without an app update. */
    fun loadModels() = viewModelScope.launch {
        val key = settings.apiKey() ?: return@launch
        val remote = runCatching { gemini.listModels(key) }.getOrNull().orEmpty().map { it.name.removePrefix("models/") }
        if (remote.isNotEmpty()) models.value = DEFAULT_MODELS + remote.sorted().filter { it !in DEFAULT_MODELS }
    }

    fun setModel(model: String) = viewModelScope.launch { settings.setModel(model) }

    fun setWebSearch(enabled: Boolean) = viewModelScope.launch { settings.setWebSearch(enabled) }

    fun setDifficulty(difficulty: Difficulty) = viewModelScope.launch { settings.setDifficulty(difficulty) }
}
