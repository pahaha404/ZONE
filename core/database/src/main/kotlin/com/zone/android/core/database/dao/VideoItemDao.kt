package com.zone.android.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zone.android.core.database.entity.VideoItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for local video items.
 */
@Dao
interface VideoItemDao {
    @Query("SELECT * FROM video_items ORDER BY added_at_epoch_ms DESC")
    fun observeAll(): Flow<List<VideoItemEntity>>

    @Query("SELECT * FROM video_items WHERE id = :videoId LIMIT 1")
    suspend fun getById(videoId: Long): VideoItemEntity?

    @Query("SELECT * FROM video_items WHERE content_uri = :contentUri LIMIT 1")
    suspend fun getByContentUri(contentUri: String): VideoItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(videoItem: VideoItemEntity): Long

    @Update
    suspend fun update(videoItem: VideoItemEntity)
}
