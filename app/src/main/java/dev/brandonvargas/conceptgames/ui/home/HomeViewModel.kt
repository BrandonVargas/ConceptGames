package dev.brandonvargas.conceptgames.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.PackRepository
import dev.brandonvargas.conceptgames.data.db.PackSummary
import dev.brandonvargas.conceptgames.data.settings.Settings
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import dev.brandonvargas.conceptgames.data.wikipedia.LanguageRepository
import dev.brandonvargas.conceptgames.data.wikipedia.WikiLanguage
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val settings: Settings? = null,
    val language: WikiLanguage? = null,
    val library: List<PackSummary> = emptyList(),
    val languages: List<WikiLanguage> = emptyList(),
)

class HomeViewModel(
    private val settings: SettingsRepository,
    private val packs: PackRepository,
    private val languages: LanguageRepository,
) : ViewModel() {
    val state: StateFlow<HomeState> = combine(settings.settings, packs.library, languages.languages) { settings, library, all ->
        HomeState(settings, languages.find(settings.contentLanguage), library, all)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun loadAllLanguages() = viewModelScope.launch { languages.refresh() }

    fun setLanguage(code: String) = viewModelScope.launch { settings.setContentLanguage(code) }

    fun setDifficulty(difficulty: Difficulty) = viewModelScope.launch { settings.setDifficulty(difficulty) }

    fun delete(packId: Long) = viewModelScope.launch { packs.delete(packId) }
}
