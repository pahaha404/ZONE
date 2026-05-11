package com.zone.android.core.model

/**
 * Persisted top-level session record.
 */
data class Session(
    val id: Long = 0,
    val videoId: Long,
    val strictnessLevel: StrictnessLevel,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val totalDurationMs: Long = 0,
    val actualPlaybackMs: Long = 0,
    val movementViolationCount: Int = 0,
    val attentionViolationCount: Int = 0,
    val averageStableSegmentMs: Long = 0,
    val completionRate: Float = 0f,
    val focusScore: Int = 0,
    val endedReason: String? = null,
)

