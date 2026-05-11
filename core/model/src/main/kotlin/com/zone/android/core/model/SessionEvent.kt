package com.zone.android.core.model

/**
 * Fine-grained event emitted during a session for debugging and reporting.
 */
data class SessionEvent(
    val id: Long = 0,
    val sessionId: Long,
    val eventType: SessionEventType,
    val violation: FocusViolation = FocusViolation.NONE,
    val timestampMillis: Long,
    val message: String? = null,
)

/**
 * Supported event kinds persisted for a session.
 */
enum class SessionEventType {
    STARTED,
    WARNING,
    PAUSE,
    RESUME,
    LOCKOUT,
    REQUIRES_RECALIBRATION,
    TERMINATED,
    COMPLETED,
}
