package dev.brandonvargas.conceptgames.ui.games

import androidx.compose.runtime.Composable
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.games.GameRound
import dev.brandonvargas.conceptgames.engine.games.builders.CategorizeRound
import dev.brandonvargas.conceptgames.engine.games.builders.CrosswordRound
import dev.brandonvargas.conceptgames.engine.games.builders.FillBlankRound
import dev.brandonvargas.conceptgames.engine.games.builders.FlashcardsRound
import dev.brandonvargas.conceptgames.engine.games.builders.HangmanRound
import dev.brandonvargas.conceptgames.engine.games.builders.MemoryPairsRound
import dev.brandonvargas.conceptgames.engine.games.builders.MultipleChoiceRound
import dev.brandonvargas.conceptgames.engine.games.builders.OddOneOutRound
import dev.brandonvargas.conceptgames.engine.games.builders.RopeMatchRound
import dev.brandonvargas.conceptgames.engine.games.builders.TimelineRound
import dev.brandonvargas.conceptgames.engine.games.builders.TrueFalseRound
import dev.brandonvargas.conceptgames.engine.games.builders.WordScrambleRound
import dev.brandonvargas.conceptgames.engine.games.builders.WordSearchRound

/**
 * Maps each engine round type to its screen. Adding a minigame? Register its composable here
 * (see CONTRIBUTING.md, "How to add a minigame").
 */
@Composable
fun GameScreen(round: GameRound, onFinish: (GameResult) -> Unit) {
    when (round) {
        is MultipleChoiceRound -> MultipleChoiceGame(round, onFinish)
        is RopeMatchRound -> RopeMatchGame(round, onFinish)
        is WordScrambleRound -> WordScrambleGame(round, onFinish)
        is CrosswordRound -> CrosswordGame(round, onFinish)
        is FlashcardsRound -> FlashcardsGame(round, onFinish)
        is TrueFalseRound -> TrueFalseGame(round, onFinish)
        is FillBlankRound -> FillBlankGame(round, onFinish)
        is TimelineRound -> TimelineGame(round, onFinish)
        is WordSearchRound -> WordSearchGame(round, onFinish)
        is HangmanRound -> HangmanGame(round, onFinish)
        is MemoryPairsRound -> MemoryPairsGame(round, onFinish)
        is CategorizeRound -> CategorizeGame(round, onFinish)
        is OddOneOutRound -> OddOneOutGame(round, onFinish)
        else -> error("No screen registered for ${round::class.simpleName}")
    }
}
