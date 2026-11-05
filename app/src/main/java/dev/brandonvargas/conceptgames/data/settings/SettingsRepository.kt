package dev.brandonvargas.conceptgames.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.brandonvargas.conceptgames.BuildConfig
import dev.brandonvargas.conceptgames.data.gemini.GeminiClient
import dev.brandonvargas.conceptgames.data.wikipedia.BundledLanguages
import dev.brandonvargas.conceptgames.engine.games.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class Settings(
    val onboardingDone: Boolean,
    val hasApiKey: Boolean,
    val model: String,
    val webSearch: Boolean,
    val difficulty: Difficulty,
    val contentLanguage: String,
    val samplesSeeded: Int,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context, private val cipher: ApiKeyCipher) {
    private val store = context.dataStore

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            onboardingDone = prefs[ONBOARDING_DONE] ?: false,
            hasApiKey = prefs[API_KEY] != null || BuildConfig.DEBUG_GEMINI_API_KEY.isNotBlank(),
            model = prefs[MODEL] ?: GeminiClient.DEFAULT_MODEL,
            webSearch = prefs[WEB_SEARCH] ?: true,
            difficulty = prefs[DIFFICULTY]?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() } ?: Difficulty.Medium,
            contentLanguage = prefs[CONTENT_LANGUAGE] ?: BundledLanguages.deviceDefault(),
            samplesSeeded = prefs[SAMPLES_SEEDED] ?: 0,
        )
    }

    suspend fun current(): Settings = settings.first()

    /** The decrypted key, or the developer key from local.properties in debug builds. */
    suspend fun apiKey(): String? = withContext(Dispatchers.IO) {
        store.data.first()[API_KEY]?.let(cipher::decrypt) ?: BuildConfig.DEBUG_GEMINI_API_KEY.ifBlank { null }
    }

    suspend fun setApiKey(key: String?) = withContext(Dispatchers.IO) {
        val encrypted = key?.trim()?.takeIf { it.isNotEmpty() }?.let(cipher::encrypt)
        store.edit { prefs -> if (encrypted == null) prefs.remove(API_KEY) else prefs[API_KEY] = encrypted }
    }

    suspend fun setOnboardingDone() = store.edit { it[ONBOARDING_DONE] = true }

    suspend fun setModel(model: String) = store.edit { it[MODEL] = model }

    suspend fun setWebSearch(enabled: Boolean) = store.edit { it[WEB_SEARCH] = enabled }

    suspend fun setDifficulty(difficulty: Difficulty) = store.edit { it[DIFFICULTY] = difficulty.name }

    suspend fun setContentLanguage(code: String) = store.edit { it[CONTENT_LANGUAGE] = code }

    suspend fun setSamplesSeeded(version: Int) = store.edit { it[SAMPLES_SEEDED] = version }

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val API_KEY = stringPreferencesKey("api_key_encrypted")
        val MODEL = stringPreferencesKey("model")
        val WEB_SEARCH = booleanPreferencesKey("web_search")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val CONTENT_LANGUAGE = stringPreferencesKey("content_language")
        val SAMPLES_SEEDED = intPreferencesKey("samples_seeded")
    }
}
