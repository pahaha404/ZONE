package com.zone.android.core.model

/**
 * Final aggregate numbers written back to the session record when a run ends.
 */
data class SessionSummary(
    val endedAtEpochMs: Long,
    val totalDurationMs: Long,
    val actualPlaybackMs: Long,
    val movementViolationCount: Int,
    val attentionViolationCount: Int,
    val averageStableSegmentMs: Long,
    val completionRate: Float,
    val focusScore: Int,
    val endedReason: String,
)

