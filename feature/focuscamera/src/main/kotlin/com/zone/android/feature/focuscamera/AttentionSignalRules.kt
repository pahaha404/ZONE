package com.zone.android.feature.focuscamera

import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.SessionRuntimeConfig
import kotlin.math.abs
import kotlin.math.max

/**
 * Raw face metrics extracted from the latest analyzed frame.
 */
data class FaceFrameMetrics(
    val centerX: Float,
    val centerY: Float,
    val faceSizeRatio: Float,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
)

/**
 * Pure rules for converting face pose metrics into an attention decision.
 */
object AttentionSignalRules {
    fun evaluate(
        timestampMillis: Long,
        metrics: FaceFrameMetrics?,
        runtimeConfig: SessionRuntimeConfig,
        trackingStatus: AttentionTrackingStatus,
    ): AttentionSignal {
        if (metrics == null) {
            return AttentionSignal(
                timestampMillis = timestampMillis,
                faceDetected = false,
                centered = false,
                faceCenterX = 0.5f,
                faceCenterY = 0.5f,
                faceSizeRatio = 0f,
                yawDegrees = 0f,
                pitchDegrees = 0f,
                rollDegrees = 0f,
                isAttentive = false,
                trackingStatus = trackingStatus,
            )
        }

        val thresholds = runtimeConfig.thresholds
        val baseline = runtimeConfig.calibrationProfile
        val targetCenterX = baseline?.baselineFaceCenterX ?: 0.5f
        val targetCenterY = baseline?.baselineFaceCenterY ?: 0.5f
        val centered = abs(metrics.centerX - targetCenterX) <= thresholds.centerToleranceRatio &&
            abs(metrics.centerY - targetCenterY) <= thresholds.centerToleranceRatio
        val minimumFaceSize = max(
            thresholds.minimumFaceSizeRatio,
            (baseline?.baselineFaceSizeRatio ?: 0f) * thresholds.faceSizeRetentionRatio,
        )
        val yawDelta = abs(metrics.yawDegrees - (baseline?.baselineYawDegrees ?: 0f))
        val pitchDelta = abs(metrics.pitchDegrees - (baseline?.baselinePitchDegrees ?: 0f))
        val rollDelta = abs(metrics.rollDegrees - (baseline?.baselineRollDegrees ?: 0f))
        val attentive = trackingStatus == AttentionTrackingStatus.RUNNING &&
            centered &&
            metrics.faceSizeRatio >= minimumFaceSize &&
            yawDelta <= thresholds.yawPauseDegrees &&
            pitchDelta <= thresholds.pitchPauseDegrees &&
            rollDelta <= thresholds.rollPauseDegrees

        return AttentionSignal(
            timestampMillis = timestampMillis,
            faceDetected = true,
            centered = centered,
            faceCenterX = metrics.centerX,
            faceCenterY = metrics.centerY,
            faceSizeRatio = metrics.faceSizeRatio,
            yawDegrees = metrics.yawDegrees,
            pitchDegrees = metrics.pitchDegrees,
            rollDegrees = metrics.rollDegrees,
            isAttentive = attentive,
            trackingStatus = trackingStatus,
        )
    }
}
