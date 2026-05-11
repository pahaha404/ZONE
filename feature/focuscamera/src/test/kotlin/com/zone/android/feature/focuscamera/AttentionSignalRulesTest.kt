package com.zone.android.feature.focuscamera

import com.google.common.truth.Truth.assertThat
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.FocusThresholds
import com.zone.android.core.model.Quaternion
import com.zone.android.core.model.SessionRuntimeConfig
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.thresholds
import org.junit.Test

class AttentionSignalRulesTest {
    @Test
    fun `attention respects baseline-relative center tolerance`() {
        val baseline = CalibrationProfile(
            videoId = 1L,
            strictnessLevel = StrictnessLevel.STANDARD,
            baselineQuaternion = Quaternion(1.0, 0.0, 0.0, 0.0),
            baselineFaceCenterX = 0.5f,
            baselineFaceCenterY = 0.5f,
            baselineFaceSizeRatio = 0.2f,
            baselineYawDegrees = 0f,
            baselinePitchDegrees = 0f,
            baselineRollDegrees = 0f,
            capturedAtEpochMs = 0L,
        )
        val metrics = FaceFrameMetrics(
            centerX = 0.71f,
            centerY = 0.5f,
            faceSizeRatio = 0.2f,
            yawDegrees = 0f,
            pitchDegrees = 0f,
            rollDegrees = 0f,
        )
        val light = AttentionSignalRules.evaluate(
            timestampMillis = 0L,
            metrics = metrics,
            runtimeConfig = SessionRuntimeConfig(
                thresholds = StrictnessLevel.LIGHT.thresholds(),
                calibrationProfile = baseline,
            ),
            trackingStatus = AttentionTrackingStatus.RUNNING,
        )
        val hard = AttentionSignalRules.evaluate(
            timestampMillis = 0L,
            metrics = metrics,
            runtimeConfig = SessionRuntimeConfig(
                thresholds = StrictnessLevel.HARD.thresholds(),
                calibrationProfile = baseline,
            ),
            trackingStatus = AttentionTrackingStatus.RUNNING,
        )

        assertThat(light.isAttentive).isTrue()
        assertThat(hard.isAttentive).isFalse()
    }

    @Test
    fun `attention requires enough retained face size from calibration baseline`() {
        val baseline = CalibrationProfile(
            videoId = 1L,
            strictnessLevel = StrictnessLevel.STANDARD,
            baselineQuaternion = Quaternion(1.0, 0.0, 0.0, 0.0),
            baselineFaceCenterX = 0.5f,
            baselineFaceCenterY = 0.5f,
            baselineFaceSizeRatio = 0.2f,
            baselineYawDegrees = 0f,
            baselinePitchDegrees = 0f,
            baselineRollDegrees = 0f,
            capturedAtEpochMs = 0L,
        )
        val signal = AttentionSignalRules.evaluate(
            timestampMillis = 0L,
            metrics = FaceFrameMetrics(
                centerX = 0.5f,
                centerY = 0.5f,
                faceSizeRatio = 0.12f,
                yawDegrees = 0f,
                pitchDegrees = 0f,
                rollDegrees = 0f,
            ),
            runtimeConfig = SessionRuntimeConfig(
                thresholds = FocusThresholds.Standard,
                calibrationProfile = baseline,
            ),
            trackingStatus = AttentionTrackingStatus.RUNNING,
        )

        assertThat(signal.isAttentive).isFalse()
    }

    @Test
    fun `standard preset tolerates moderate distance change after calibration`() {
        val baseline = CalibrationProfile(
            videoId = 1L,
            strictnessLevel = StrictnessLevel.STANDARD,
            baselineQuaternion = Quaternion(1.0, 0.0, 0.0, 0.0),
            baselineFaceCenterX = 0.5f,
            baselineFaceCenterY = 0.5f,
            baselineFaceSizeRatio = 0.2f,
            baselineYawDegrees = 0f,
            baselinePitchDegrees = 0f,
            baselineRollDegrees = 0f,
            capturedAtEpochMs = 0L,
        )
        val signal = AttentionSignalRules.evaluate(
            timestampMillis = 0L,
            metrics = FaceFrameMetrics(
                centerX = 0.56f,
                centerY = 0.49f,
                faceSizeRatio = 0.145f,
                yawDegrees = 6f,
                pitchDegrees = 5f,
                rollDegrees = 4f,
            ),
            runtimeConfig = SessionRuntimeConfig(
                thresholds = FocusThresholds.Standard,
                calibrationProfile = baseline,
            ),
            trackingStatus = AttentionTrackingStatus.RUNNING,
        )

        assertThat(signal.isAttentive).isTrue()
    }
}
