package com.zone.android.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for imported local videos.
 */
@Entity(
    tableName = "video_items",
    indices = [Index(value = ["content_uri"], unique = true)],
)
data class VideoItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "content_uri")
    val contentUri: String,
    val title: String,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "thumbnail_bytes")
    val thumbnailBytes: ByteArray?,
    @ColumnInfo(name = "added_at_epoch_ms")
    val addedAtEpochMs: Long,
)

