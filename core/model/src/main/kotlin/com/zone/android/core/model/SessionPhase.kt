package com.zone.android.core.model

/**
 * Lifecycle states for a playback session.
 */
enum class SessionPhase {
    CALIBRATING,
    READY,
    READY_COUNTDOWN,
    PLAYING,
    WARNING,
    PAUSED_MOVEMENT,
    PAUSED_ATTENTION,
    RECOVERING,
    LOCKED_OUT,
    REQUIRES_RECALIBRATION,
    SESSION_DONE,
}
