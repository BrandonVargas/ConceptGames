package dev.brandonvargas.conceptgames.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.gemini.GeminiClient
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface KeyStatus {
    data object Idle : KeyStatus

    data object Testing : KeyStatus

    data class Valid(val modelCount: Int) : KeyStatus

    data class Invalid(val error: Throwable) : KeyStatus
}

data class KeyGuideState(
    val key: String = "",
    val status: KeyStatus = KeyStatus.Idle,
    val hasSavedKey: Boolean = false,
)

class ApiKeyGuideViewModel(
    private val settings: SettingsRepository,
    private val gemini: GeminiClient,
) : ViewModel() {
    private val _state = MutableStateFlow(KeyGuideState())
    val state: StateFlow<KeyGuideState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(hasSavedKey = settings.current().hasApiKey) } }
    }

    fun onKeyChange(key: String) = _state.update { it.copy(key = key.trim(), status = KeyStatus.Idle) }

    /** Validates the key with a free models-list call, and stores it only if Google accepts it. */
    fun testAndSave() {
        val key = _state.value.key.ifBlank { return }
        _state.update { it.copy(status = KeyStatus.Testing) }
        viewModelScope.launch {
            val status = try {
                val models = gemini.listModels(key)
                settings.setApiKey(key)
                settings.setOnboardingDone()
                KeyStatus.Valid(models.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                KeyStatus.Invalid(e)
            }
            _state.update { it.copy(status = status, hasSavedKey = it.hasSavedKey || status is KeyStatus.Valid) }
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            settings.setApiKey(null)
            _state.update { KeyGuideState() }
        }
    }
}
