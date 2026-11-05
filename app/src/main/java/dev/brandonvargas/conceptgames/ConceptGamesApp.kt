package dev.brandonvargas.conceptgames

import android.app.Application
import android.util.Log
import dev.brandonvargas.conceptgames.data.PackRepository
import kotlinx.coroutines.launch

class ConceptGamesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            val settings = container.settings.current()
            if (settings.samplesSeeded < PackRepository.SAMPLES_VERSION) {
                runCatching { container.packs.seedSamples() }
                    .onSuccess { container.settings.setSamplesSeeded(PackRepository.SAMPLES_VERSION) }
                    .onFailure { Log.e("ConceptGames", "Could not load the sample packs", it) }
            }
        }
    }
}
