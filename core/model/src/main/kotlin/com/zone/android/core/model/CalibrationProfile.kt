package com.zone.android.core.model

/**
 * Baseline posture and face pose captured during calibration.
 */
data class CalibrationProfile(
    val id: Long = 0,
    val videoId: Long,
    val strictnessLevel: StrictnessLevel,
    val baselineQuaternion: Quaternion,
    val baselineFaceCenterX: Float,
    val baselineFaceCenterY: Float,
    val baselineFaceSizeRatio: Float,
    val baselineYawDegrees: Float,
    val baselinePitchDegrees: Float,
    val baselineRollDegrees: Float,
    val capturedAtEpochMs: Long,
)

