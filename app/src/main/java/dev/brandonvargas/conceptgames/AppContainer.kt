package dev.brandonvargas.conceptgames

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import dev.brandonvargas.conceptgames.data.PackRepository
import dev.brandonvargas.conceptgames.data.db.AppDatabase
import dev.brandonvargas.conceptgames.data.gemini.GeminiClient
import dev.brandonvargas.conceptgames.data.network.HttpClients
import dev.brandonvargas.conceptgames.data.settings.ApiKeyCipher
import dev.brandonvargas.conceptgames.data.settings.SettingsRepository
import dev.brandonvargas.conceptgames.data.wikipedia.LanguageRepository
import dev.brandonvargas.conceptgames.data.wikipedia.WikipediaClient
import dev.brandonvargas.conceptgames.domain.GeneratePackUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Manual dependency injection: one instance of each service for the whole app. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob())
    val http = HttpClients.create()
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "conceptgames.db").build()
    val settings = SettingsRepository(context, ApiKeyCipher())
    val wikipedia = WikipediaClient(http)
    val languages = LanguageRepository(wikipedia)
    val gemini = GeminiClient(http, HttpClients.json, baseUrl = BuildConfig.GEMINI_BASE_URL)
    val packs = PackRepository(database, context.assets)
    val generatePack = GeneratePackUseCase(wikipedia, gemini, settings, packs)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
