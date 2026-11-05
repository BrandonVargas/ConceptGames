package dev.brandonvargas.conceptgames.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.PackRepository
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameCatalog
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class HubState(
    val loading: Boolean = true,
    val pack: StudyPack? = null,
    val available: Set<GameId> = emptySet(),
    val stars: Map<GameId, Int> = emptyMap(),
    val difficulty: Difficulty = Difficulty.Medium,
)

class HubViewModel(
    private val packs: PackRepository,
    private val settings: SettingsRepository,
    packId: Long,
) : ViewModel() {
    private val loaded = MutableStateFlow<Pair<StudyPack?, Set<GameId>>?>(null)

    val state: StateFlow<HubState> = combine(
        loaded,
        packs.bestScores(packId).map { scores ->
            scores.mapNotNull { score ->
                val id = runCatching { GameId.valueOf(score.gameId) }.getOrNull() ?: return@mapNotNull null
                id to starsFor(score.best)
            }.toMap()
        },
        settings.settings.map { it.difficulty },
    ) { loaded, stars, difficulty ->
        HubState(loading = loaded == null, pack = loaded?.first, available = loaded?.second.orEmpty(), stars = stars, difficulty = difficulty)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HubState())

    init {
        viewModelScope.launch {
            val pack = packs.pack(packId)
            // Availability checks run the crossword generator, so keep them off the main thread.
            val available = withContext(Dispatchers.Default) { pack?.let(GameCatalog::available).orEmpty().toSet() }
            loaded.value = pack to available
        }
    }

    fun setDifficulty(difficulty: Difficulty) = viewModelScope.launch { settings.setDifficulty(difficulty) }

    fun mixedPlaylist(): List<GameId> = state.value.pack?.let { GameCatalog.mixedPlaylist(it, Random.Default) }.orEmpty()

    private fun starsFor(best: Double) = when {
        best >= 0.9 -> 3
        best >= 0.7 -> 2
        best >= 0.4 -> 1
        else -> 0
    }
}
