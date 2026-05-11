package com.zone.android.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zone.android.core.model.StrictnessLevel

/**
 * Room entity for session aggregates.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = VideoItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["video_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("video_id")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "video_id")
    val videoId: Long,
    @ColumnInfo(name = "strictness_level")
    val strictnessLevel: StrictnessLevel,
    @ColumnInfo(name = "started_at_epoch_ms")
    val startedAtEpochMs: Long,
    @ColumnInfo(name = "ended_at_epoch_ms")
    val endedAtEpochMs: Long?,
    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long,
    @ColumnInfo(name = "actual_playback_ms")
    val actualPlaybackMs: Long,
    @ColumnInfo(name = "movement_violation_count")
    val movementViolationCount: Int,
    @ColumnInfo(name = "attention_violation_count")
    val attentionViolationCount: Int,
    @ColumnInfo(name = "average_stable_segment_ms")
    val averageStableSegmentMs: Long,
    @ColumnInfo(name = "completion_rate")
    val completionRate: Float,
    @ColumnInfo(name = "focus_score")
    val focusScore: Int,
    @ColumnInfo(name = "ended_reason")
    val endedReason: String?,
)

