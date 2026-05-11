package com.zone.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zone.android.core.database.entity.SessionEventEntity

/**
 * DAO for session event history.
 */
@Dao
interface SessionEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SessionEventEntity): Long

    @Query("SELECT * FROM session_events WHERE session_id = :sessionId ORDER BY timestamp_ms ASC")
    suspend fun listBySessionId(sessionId: Long): List<SessionEventEntity>
}

