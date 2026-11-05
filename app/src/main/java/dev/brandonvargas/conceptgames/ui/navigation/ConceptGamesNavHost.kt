package dev.brandonvargas.conceptgames.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.brandonvargas.conceptgames.LocalAppContainer
import dev.brandonvargas.conceptgames.data.settings.Settings
import dev.brandonvargas.conceptgames.ui.generate.GenerateScreen
import dev.brandonvargas.conceptgames.ui.home.HomeScreen
import dev.brandonvargas.conceptgames.ui.hub.HubScreen
import dev.brandonvargas.conceptgames.ui.onboarding.ApiKeyGuideScreen
import dev.brandonvargas.conceptgames.ui.onboarding.OnboardingScreen
import dev.brandonvargas.conceptgames.ui.picker.PickerScreen
import dev.brandonvargas.conceptgames.ui.play.PlayScreen
import dev.brandonvargas.conceptgames.ui.settings.SettingsScreen

@Composable
fun ConceptGamesNavHost() {
    val container = LocalAppContainer.current
    val settings: Settings? by container.settings.settings.collectAsState(initial = null)
    val loaded = settings
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val backStack = rememberNavBackStack(if (loaded.onboardingDone) HomeRoute else OnboardingRoute)
    val back: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        // Exposes test tags as resource ids so UI Automator and adb scripts can find elements.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(
                    onSetUpKey = { backStack.add(KeyGuideRoute(fromOnboarding = true)) },
                    onSkip = { backStack.replaceAll(HomeRoute) },
                )
            }
            entry<KeyGuideRoute> { route ->
                ApiKeyGuideScreen(
                    onBack = back,
                    onDone = { if (route.fromOnboarding) backStack.replaceAll(HomeRoute) else back() },
                )
            }
            entry<HomeRoute> {
                HomeScreen(
                    onFindSources = { topic, language, difficulty -> backStack.add(PickerRoute(topic, language, difficulty)) },
                    onOpenPack = { backStack.add(HubRoute(it)) },
                    onOpenSettings = { backStack.add(SettingsRoute) },
                    onSetUpKey = { backStack.add(KeyGuideRoute()) },
                )
            }
            entry<PickerRoute> { route ->
                PickerScreen(
                    route = route,
                    onBack = back,
                    onGenerate = { articles -> backStack.add(GenerateRoute(route.topic, route.language, route.difficulty, articles)) },
                )
            }
            entry<GenerateRoute> { route ->
                GenerateScreen(
                    route = route,
                    onBack = back,
                    onDone = { packId ->
                        // The picker and progress screens are done; back from the hub returns home.
                        backStack.removeAll { it is PickerRoute || it is GenerateRoute }
                        backStack.add(HubRoute(packId))
                    },
                    onFixKey = { backStack.add(KeyGuideRoute()) },
                )
            }
            entry<HubRoute> { route ->
                HubScreen(
                    packId = route.packId,
                    onBack = back,
                    onPlay = { games, difficulty -> backStack.add(PlayRoute(route.packId, games, difficulty)) },
                )
            }
            entry<PlayRoute> { route ->
                PlayScreen(route = route, onExit = back)
            }
            entry<SettingsRoute> {
                SettingsScreen(onBack = back, onOpenKeyGuide = { backStack.add(KeyGuideRoute()) })
            }
        },
    )
}

private fun NavBackStack<NavKey>.replaceAll(route: NavKey) {
    clear()
    add(route)
}
