package com.zone.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zone.android.core.database.entity.CalibrationProfileEntity
import com.zone.android.core.model.StrictnessLevel

/**
 * DAO for calibration baselines.
 */
@Dao
interface CalibrationProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CalibrationProfileEntity): Long

    @Query(
        """
        SELECT * FROM calibration_profiles
        WHERE video_id = :videoId AND strictness_level = :strictnessLevel
        ORDER BY captured_at_epoch_ms DESC
        LIMIT 1
        """,
    )
    suspend fun getLatest(videoId: Long, strictnessLevel: StrictnessLevel): CalibrationProfileEntity?
}

