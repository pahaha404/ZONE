package com.zone.android.core.model

/**
 * Playback state emitted by the concrete player coordinator.
 */
data class PlaybackSnapshot(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false,
    val isEnded: Boolean = false,
)

