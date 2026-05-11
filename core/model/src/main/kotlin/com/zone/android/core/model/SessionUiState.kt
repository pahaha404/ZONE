package com.zone.android.core.model

/**
 * Observable state rendered by session-related screens.
 */
data class SessionUiState(
    val phase: SessionPhase = SessionPhase.CALIBRATING,
    val activeViolation: FocusViolation = FocusViolation.NONE,
    val warningMessage: String? = null,
    val fatalErrorMessage: String? = null,
    val primaryActionLabel: String? = null,
    val primaryActionEnabled: Boolean = false,
    val calibrationProgressMs: Long = 0,
    val countdownRemainingMs: Long = 0,
    val recoverProgressMs: Long = 0,
    val recoverRemainingMs: Long = 0,
    val lockoutRemainingMs: Long = 0,
    val movementViolationCount: Int = 0,
    val attentionViolationCount: Int = 0,
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    val requiresRecalibration: Boolean = false,
)
