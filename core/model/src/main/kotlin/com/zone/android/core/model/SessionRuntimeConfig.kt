package com.zone.android.core.model

/**
 * Runtime focus configuration shared by calibration, camera, sensors, and sessions.
 */
data class SessionRuntimeConfig(
    val thresholds: FocusThresholds,
    val calibrationProfile: CalibrationProfile? = null,
)

/**
 * Factory for a calibration-time config.
 */
fun StrictnessLevel.calibrationRuntimeConfig(): SessionRuntimeConfig =
    SessionRuntimeConfig(thresholds = thresholds())

/**
 * Factory for an active-session config using a saved calibration profile.
 */
fun CalibrationProfile.sessionRuntimeConfig(): SessionRuntimeConfig =
    SessionRuntimeConfig(
        thresholds = strictnessLevel.thresholds(),
        calibrationProfile = this,
    )
