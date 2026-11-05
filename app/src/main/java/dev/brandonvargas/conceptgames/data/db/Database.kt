package dev.brandonvargas.conceptgames.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "packs")
data class PackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val language: String,
    val createdAt: Long,
    val isSample: Boolean,
    /** Serialized engine StudyPack. */
    val json: String,
)

@Entity(
    tableName = "results",
    indices = [Index("packId")],
    foreignKeys = [ForeignKey(PackEntity::class, ["id"], ["packId"], onDelete = ForeignKey.CASCADE)],
)
data class ResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: Long,
    val gameId: String,
    val correct: Int,
    val total: Int,
    val durationMs: Long,
    val playedAt: Long,
)

@Entity(
    tableName = "concept_stats",
    primaryKeys = ["packId", "conceptId"],
    foreignKeys = [ForeignKey(PackEntity::class, ["id"], ["packId"], onDelete = ForeignKey.CASCADE)],
)
data class ConceptStatEntity(
    val packId: Long,
    val conceptId: String,
    val seen: Int,
    val missed: Int,
)

data class PackSummary(
    val id: Long,
    val topic: String,
    val language: String,
    val createdAt: Long,
    val isSample: Boolean,
    val plays: Int,
)

data class GameBest(val gameId: String, val best: Double, val plays: Int)

@Dao
interface PackDao {
    @Query(
        """
        SELECT p.id, p.topic, p.language, p.createdAt, p.isSample,
            (SELECT COUNT(*) FROM results r WHERE r.packId = p.id) AS plays
        FROM packs p ORDER BY p.isSample ASC, p.createdAt DESC
        """,
    )
    fun observeSummaries(): Flow<List<PackSummary>>

    @Query("SELECT * FROM packs WHERE id = :id")
    suspend fun get(id: Long): PackEntity?

    @Insert
    suspend fun insert(pack: PackEntity): Long

    @Query("DELETE FROM packs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ProgressDao {
    @Insert
    suspend fun insertResult(result: ResultEntity)

    @Query(
        "SELECT gameId, MAX(CAST(correct AS REAL) / total) AS best, COUNT(*) AS plays " +
            "FROM results WHERE packId = :packId AND total > 0 GROUP BY gameId",
    )
    fun observeBest(packId: Long): Flow<List<GameBest>>

    @Query("SELECT * FROM concept_stats WHERE packId = :packId")
    suspend fun conceptStats(packId: Long): List<ConceptStatEntity>

    @Upsert
    suspend fun upsertStats(stats: List<ConceptStatEntity>)

    @Transaction
    suspend fun record(result: ResultEntity, seen: Collection<String>, missed: Collection<String>) {
        insertResult(result)
        val current = conceptStats(result.packId).associateBy { it.conceptId }
        upsertStats(
            (seen + missed).toSet().map { id ->
                val stat = current[id] ?: ConceptStatEntity(result.packId, id, 0, 0)
                stat.copy(seen = stat.seen + 1, missed = stat.missed + if (id in missed) 1 else 0)
            },
        )
    }
}

@Database(entities = [PackEntity::class, ResultEntity::class, ConceptStatEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packs(): PackDao

    abstract fun progress(): ProgressDao
}
