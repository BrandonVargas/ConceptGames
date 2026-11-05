package dev.brandonvargas.conceptgames.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.brandonvargas.conceptgames.data.PackRepository
import dev.brandonvargas.conceptgames.engine.games.BuildSettings
import dev.brandonvargas.conceptgames.engine.games.GameCatalog
import dev.brandonvargas.conceptgames.engine.games.GameId
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.conceptIds
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.ui.navigation.PlayRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

sealed interface PlayPhase {
    data object Loading : PlayPhase

    data object Missing : PlayPhase

    /** [roundNumber] changes for every new round so game screens start with fresh state. */
    data class Playing(val round: GameRound, val index: Int, val roundNumber: Int) : PlayPhase

    data class BetweenGames(val last: GameResult, val next: GameId, val index: Int) : PlayPhase

    data class Finished(val results: List<GameResult>) : PlayPhase
}

class PlayViewModel(private val packs: PackRepository, private val route: PlayRoute) : ViewModel() {
    private val _phase = MutableStateFlow<PlayPhase>(PlayPhase.Loading)
    val phase: StateFlow<PlayPhase> = _phase.asStateFlow()
    var pack: StudyPack? = null
        private set
    private val results = mutableListOf<GameResult>()
    private var roundNumber = 0

    val playlist: List<GameId> get() = route.games
    val isMixed: Boolean get() = route.games.size > 1

    // Practice stats load alongside the pack instead of delaying every round.
    private val weights = viewModelScope.async { packs.weights(route.packId) }

    init {
        viewModelScope.launch {
            pack = packs.pack(route.packId)
            if (pack == null) _phase.value = PlayPhase.Missing else startGame(0)
        }
    }

    fun finish(result: GameResult) {
        val current = _phase.value as? PlayPhase.Playing ?: return
        val recorded = result.copy(seenConceptIds = current.round.conceptIds().toList())
        results += recorded
        viewModelScope.launch { packs.record(route.packId, recorded) }
        val nextIndex = current.index + 1
        _phase.value = if (nextIndex < route.games.size) {
            PlayPhase.BetweenGames(recorded, route.games[nextIndex], nextIndex)
        } else {
            PlayPhase.Finished(results.toList())
        }
    }

    fun next() {
        val between = _phase.value as? PlayPhase.BetweenGames ?: return
        viewModelScope.launch { startGame(between.index) }
    }

    fun playAgain() {
        results.clear()
        viewModelScope.launch { startGame(0) }
    }

    private suspend fun startGame(index: Int) {
        val pack = pack ?: return
        _phase.value = PlayPhase.Loading
        val weights = weights.await()
        // Crossword layout search can take a moment, so build rounds off the main thread.
        val round = withContext(Dispatchers.Default) {
            GameCatalog.build(route.games[index], pack, BuildSettings(route.difficulty, Random.Default, weights))
        }
        _phase.value = PlayPhase.Playing(round, index, ++roundNumber)
    }
}
