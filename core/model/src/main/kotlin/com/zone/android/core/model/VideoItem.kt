package com.zone.android.core.model

/**
 * Local video imported into the ZONE library.
 */
data class VideoItem(
    val id: Long = 0,
    val contentUri: String,
    val title: String,
    val durationMs: Long,
    val thumbnailBytes: ByteArray?,
    val addedAtEpochMs: Long,
)

