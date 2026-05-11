package com.zone.android.core.model

import kotlin.math.roundToLong

/**
 * Tunable thresholds that drive calibration, warning, pause, and recovery behavior.
 */
data class FocusThresholds(
    val warningPostureDegrees: Double,
    val warningPostureHoldMs: Long,
    val pausePostureDegrees: Double,
    val pausePostureHoldMs: Long,
    val motionPauseEnergy: Double,
    val motionPauseHoldMs: Long,
    val faceMissingHoldMs: Long,
    val yawPauseDegrees: Double,
    val pitchPauseDegrees: Double,
    val rollPauseDegrees: Double,
    val headPoseHoldMs: Long,
    val recoverStableMs: Long,
    val calibrationStableMs: Long,
    val calibrationMotionEnergy: Double,
    val calibrationPostureDegrees: Double,
    val minimumFaceSizeRatio: Float,
    val faceSizeRetentionRatio: Float,
    val centerToleranceRatio: Float,
    val lockoutDurationMs: Long,
    val pauseBurstWindowMs: Long,
    val lockoutPauseCount: Int,
    val recalibrationPauseCount: Int,
    val maxMovementViolations: Int,
    val maxAttentionViolations: Int,
) {
    companion object {
        val Standard = FocusThresholds(
            warningPostureDegrees = 14.0,
            warningPostureHoldMs = 850,
            pausePostureDegrees = 24.0,
            pausePostureHoldMs = 700,
            motionPauseEnergy = 1.45,
            motionPauseHoldMs = 850,
            faceMissingHoldMs = 1_000,
            yawPauseDegrees = 24.0,
            pitchPauseDegrees = 24.0,
            rollPauseDegrees = 22.0,
            headPoseHoldMs = 1_100,
            recoverStableMs = 3_000,
            calibrationStableMs = 3_000,
            calibrationMotionEnergy = 0.62,
            calibrationPostureDegrees = 8.0,
            minimumFaceSizeRatio = 0.08f,
            faceSizeRetentionRatio = 0.65f,
            centerToleranceRatio = 0.22f,
            lockoutDurationMs = 10_000,
            pauseBurstWindowMs = 60_000,
            lockoutPauseCount = 3,
            recalibrationPauseCount = 5,
            maxMovementViolations = 3,
            maxAttentionViolations = 3,
        )
    }
}

/**
 * Resolves a preset to thresholds.
 */
fun StrictnessLevel.thresholds(): FocusThresholds {
    val base = FocusThresholds.Standard
    return when (this) {
        StrictnessLevel.STANDARD -> base
        StrictnessLevel.LIGHT -> base.copy(
            warningPostureDegrees = base.warningPostureDegrees * 1.15,
            warningPostureHoldMs = (base.warningPostureHoldMs * 1.15).roundToLong(),
            pausePostureDegrees = base.pausePostureDegrees * 1.15,
            pausePostureHoldMs = (base.pausePostureHoldMs * 1.15).roundToLong(),
            motionPauseEnergy = base.motionPauseEnergy * 1.15,
            motionPauseHoldMs = (base.motionPauseHoldMs * 1.15).roundToLong(),
            faceMissingHoldMs = (base.faceMissingHoldMs * 1.15).roundToLong(),
            yawPauseDegrees = base.yawPauseDegrees * 1.15,
            pitchPauseDegrees = base.pitchPauseDegrees * 1.15,
            rollPauseDegrees = base.rollPauseDegrees * 1.15,
            headPoseHoldMs = (base.headPoseHoldMs * 1.15).roundToLong(),
            calibrationMotionEnergy = base.calibrationMotionEnergy * 1.15,
            calibrationPostureDegrees = base.calibrationPostureDegrees * 1.15,
            minimumFaceSizeRatio = base.minimumFaceSizeRatio * 0.9f,
            faceSizeRetentionRatio = base.faceSizeRetentionRatio * 0.9f,
            centerToleranceRatio = base.centerToleranceRatio * 1.15f,
        )
        StrictnessLevel.HARD -> base.copy(
            warningPostureDegrees = base.warningPostureDegrees * 0.85,
            warningPostureHoldMs = (base.warningPostureHoldMs * 0.85).roundToLong(),
            pausePostureDegrees = base.pausePostureDegrees * 0.85,
            pausePostureHoldMs = (base.pausePostureHoldMs * 0.85).roundToLong(),
            motionPauseEnergy = base.motionPauseEnergy * 0.85,
            motionPauseHoldMs = (base.motionPauseHoldMs * 0.85).roundToLong(),
            faceMissingHoldMs = (base.faceMissingHoldMs * 0.85).roundToLong(),
            yawPauseDegrees = base.yawPauseDegrees * 0.85,
            pitchPauseDegrees = base.pitchPauseDegrees * 0.85,
            rollPauseDegrees = base.rollPauseDegrees * 0.85,
            headPoseHoldMs = (base.headPoseHoldMs * 0.85).roundToLong(),
            calibrationMotionEnergy = base.calibrationMotionEnergy * 0.85,
            calibrationPostureDegrees = base.calibrationPostureDegrees * 0.85,
            minimumFaceSizeRatio = base.minimumFaceSizeRatio * 1.1f,
            faceSizeRetentionRatio = (base.faceSizeRetentionRatio * 1.08f).coerceAtMost(0.95f),
            centerToleranceRatio = base.centerToleranceRatio * 0.85f,
        )
    }
}
