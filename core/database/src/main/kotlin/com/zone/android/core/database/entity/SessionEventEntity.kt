package com.zone.android.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zone.android.core.model.FocusViolation
import com.zone.android.core.model.SessionEventType

/**
 * Room entity for per-session events.
 */
@Entity(
    tableName = "session_events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("session_id")],
)
data class SessionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "event_type")
    val eventType: SessionEventType,
    val violation: FocusViolation,
    @ColumnInfo(name = "timestamp_ms")
    val timestampMillis: Long,
    val message: String?,
)

