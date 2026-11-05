package dev.brandonvargas.conceptgames.data

import android.content.res.AssetManager
import dev.brandonvargas.conceptgames.data.db.AppDatabase
import dev.brandonvargas.conceptgames.data.db.GameBest
import dev.brandonvargas.conceptgames.data.db.PackEntity
import dev.brandonvargas.conceptgames.data.db.PackSummary
import dev.brandonvargas.conceptgames.data.db.ResultEntity
import dev.brandonvargas.conceptgames.engine.games.ConceptWeights
import dev.brandonvargas.conceptgames.engine.games.GameResult
import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.validate.PackValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PackRepository(private val db: AppDatabase, private val assets: AssetManager) {
    private val cache = ConcurrentHashMap<Long, StudyPack>()

    val library: Flow<List<PackSummary>> = db.packs().observeSummaries()

    suspend fun pack(id: Long): StudyPack? = cache[id] ?: withContext(Dispatchers.IO) {
        db.packs().get(id)?.let { PackValidator.json.decodeFromString(StudyPack.serializer(), it.json) }
            ?.also { cache[id] = it }
    }

    suspend fun save(pack: StudyPack, isSample: Boolean = false): Long = withContext(Dispatchers.IO) {
        val json = PackValidator.json.encodeToString(StudyPack.serializer(), pack)
        db.packs().insert(PackEntity(topic = pack.topic, language = pack.language, createdAt = System.currentTimeMillis(), isSample = isSample, json = json))
            .also { cache[it] = pack }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        cache.remove(id)
        db.packs().delete(id)
    }

    fun bestScores(packId: Long): Flow<List<GameBest>> = db.progress().observeBest(packId)

    suspend fun record(packId: Long, result: GameResult) = withContext(Dispatchers.IO) {
        db.progress().record(
            ResultEntity(
                packId = packId,
                gameId = result.gameId.name,
                correct = result.correct,
                total = result.total,
                durationMs = result.durationMs,
                playedAt = System.currentTimeMillis(),
            ),
            seen = result.seenConceptIds,
            missed = result.missedConceptIds,
        )
    }

    /** Per-concept miss rates, so the next rounds focus on what the player struggles with. */
    suspend fun weights(packId: Long): ConceptWeights = withContext(Dispatchers.IO) {
        val stats = db.progress().conceptStats(packId)
        ConceptWeights(stats.filter { it.seen > 0 }.associate { it.conceptId to it.missed.toDouble() / it.seen })
    }

    /** Copies the bundled sample packs into the library so the app is playable without an API key. */
    suspend fun seedSamples() = withContext(Dispatchers.IO) {
        val files = assets.list(SAMPLES_DIR).orEmpty().filter { it.endsWith(".json") }.sorted()
        for (file in files) {
            val text = assets.open("$SAMPLES_DIR/$file").bufferedReader().use { it.readText() }
            save(PackValidator.validate(PackValidator.parseDocument(text)), isSample = true)
        }
    }

    companion object {
        const val SAMPLES_DIR = "sample_packs"

        /** Bump when new samples ship, so existing installs receive them. */
        const val SAMPLES_VERSION = 1
    }
}
