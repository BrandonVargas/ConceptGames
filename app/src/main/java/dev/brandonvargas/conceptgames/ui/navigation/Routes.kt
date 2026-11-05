package dev.brandonvargas.conceptgames.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import dev.brandonvargas.conceptgames.engine.games.GameId
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute : NavKey

/** [fromOnboarding] decides where "Done" leads: home for first-time setup, otherwise back. */
@Serializable
data class KeyGuideRoute(val fromOnboarding: Boolean = false) : NavKey

@Serializable
data object HomeRoute : NavKey

@Serializable
data class PickerRoute(val topic: String, val language: String, val difficulty: Difficulty) : NavKey

@Serializable
data class SelectedArticle(val pageId: Long, val title: String, val url: String)

@Serializable
data class GenerateRoute(
    val topic: String,
    val language: String,
    val difficulty: Difficulty,
    val articles: List<SelectedArticle>,
) : NavKey

@Serializable
data class HubRoute(val packId: Long) : NavKey

/** Plays [games] back to back; more than one game is a "Mixed challenge". */
@Serializable
data class PlayRoute(val packId: Long, val games: List<GameId>, val difficulty: Difficulty) : NavKey

@Serializable
data object SettingsRoute : NavKey
