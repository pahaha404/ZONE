package com.zone.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zone.android.core.database.entity.SessionEntity

/**
 * DAO for session aggregates.
 */
@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): SessionEntity?

    @Query(
        """
        SELECT * FROM sessions
        WHERE ended_at_epoch_ms IS NOT NULL
        ORDER BY focus_score DESC, completion_rate DESC, actual_playback_ms DESC, started_at_epoch_ms ASC
        """,
    )
    suspend fun listFinishedByRanking(): List<SessionEntity>
}
