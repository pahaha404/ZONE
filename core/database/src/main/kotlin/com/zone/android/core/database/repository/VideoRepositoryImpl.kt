package com.zone.android.core.database.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.zone.android.core.database.dao.VideoItemDao
import com.zone.android.core.database.entity.VideoItemEntity
import com.zone.android.core.database.mapper.toModel
import com.zone.android.core.model.VideoItem
import com.zone.android.core.model.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Android-backed implementation of [VideoRepository].
 */
class VideoRepositoryImpl(
    private val context: Context,
    private val videoItemDao: VideoItemDao,
) : VideoRepository {
    override fun observeVideos(): Flow<List<VideoItem>> =
        videoItemDao.observeAll().map { items -> items.map(VideoItemEntity::toModel) }

    override suspend fun importVideo(contentUri: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceUri = Uri.parse(contentUri)
            val sourceTitle = queryTitle(context.contentResolver, sourceUri)
            val internalContentUri = ensureInternalCopy(sourceUri, sourceTitle)
            videoItemDao.getByContentUri(internalContentUri)?.toModel()
                ?: buildImportedVideo(
                    contentUri = internalContentUri,
                    fallbackTitle = sourceTitle,
                ).let { imported ->
                    val insertedId = videoItemDao.insert(imported)
                    imported.copy(id = insertedId).toModel()
                }
        }
    }

    override suspend fun getVideo(videoId: Long): VideoItem? = withContext(Dispatchers.IO) {
        val entity = videoItemDao.getById(videoId) ?: return@withContext null
        val migrated = runCatching { migrateLegacyContentUri(entity) }.getOrElse { entity }
        migrated.toModel()
    }

    private fun buildImportedVideo(
        contentUri: String,
        fallbackTitle: String? = null,
    ): VideoItemEntity {
        val uri = Uri.parse(contentUri)
        val resolver = context.contentResolver
        val title = queryTitle(resolver, uri) ?: fallbackTitle
        val metadataRetriever = MediaMetadataRetriever()
        return try {
            metadataRetriever.setDataSource(context, uri)
            val durationMs = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val thumbnailBytes = metadataRetriever.getFrameAtTime()?.toCompressedJpeg()
            VideoItemEntity(
                contentUri = contentUri,
                title = title ?: "가져온 영상",
                durationMs = durationMs,
                thumbnailBytes = thumbnailBytes,
                addedAtEpochMs = System.currentTimeMillis(),
            )
        } finally {
            metadataRetriever.release()
        }
    }

    private suspend fun migrateLegacyContentUri(entity: VideoItemEntity): VideoItemEntity {
        if (!entity.contentUri.startsWith("content://")) {
            return entity
        }
        val sourceUri = Uri.parse(entity.contentUri)
        val internalContentUri = ensureInternalCopy(sourceUri, entity.title)
        if (internalContentUri == entity.contentUri) {
            return entity
        }
        val updated = entity.copy(contentUri = internalContentUri)
        videoItemDao.update(updated)
        return updated
    }

    private fun ensureInternalCopy(
        sourceUri: Uri,
        titleHint: String?,
    ): String {
        val directory = File(context.filesDir, "zone/videos").apply { mkdirs() }
        val extension = resolveExtension(sourceUri, titleHint)
        val targetFile = File(
            directory,
            "${sourceUri.toString().hashCode().toUInt().toString(16)}.$extension",
        )
        if (!targetFile.exists() || targetFile.length() == 0L) {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: error("가져온 영상을 열 수 없습니다")
        }
        return Uri.fromFile(targetFile).toString()
    }

    private fun queryTitle(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return null
    }

    private fun resolveExtension(
        sourceUri: Uri,
        titleHint: String?,
    ): String {
        val titleExtension = titleHint
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
        if (titleExtension != null) {
            return titleExtension
        }
        val mimeExtension = context.contentResolver.getType(sourceUri)
            ?.substringAfter('/')
            ?.substringAfterLast('.')
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
        return mimeExtension ?: "mp4"
    }

    private fun Bitmap.toCompressedJpeg(): ByteArray {
        return ByteArrayOutputStream().use { output ->
            compress(Bitmap.CompressFormat.JPEG, 80, output)
            output.toByteArray()
        }
    }
}
