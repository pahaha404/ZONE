package com.zone.android.feature.session

import com.google.common.truth.Truth.assertThat
import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.FocusThresholds
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.Quaternion
import org.junit.Test

class CalibrationGateTest {
    @Test
    fun `gate becomes ready after three stable seconds`() {
        val gate = CalibrationGate(FocusThresholds.Standard)
        val motion = MotionSignal(
            timestampMillis = 0,
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.2,
            quaternion = Quaternion(1.0, 0.0, 0.0, 0.0),
            isStable = true,
        )
        val attention = AttentionSignal(
            timestampMillis = 0,
            faceDetected = true,
            centered = true,
            faceCenterX = 0.5f,
            faceCenterY = 0.5f,
            faceSizeRatio = 0.12f,
            yawDegrees = 0f,
            pitchDegrees = 0f,
            rollDegrees = 0f,
            isAttentive = true,
            trackingStatus = AttentionTrackingStatus.RUNNING,
        )

        gate.update(0, motion, attention)
        gate.update(2_000, motion.copy(timestampMillis = 2_000), attention.copy(timestampMillis = 2_000))
        val result = gate.update(3_100, motion.copy(timestampMillis = 3_100), attention.copy(timestampMillis = 3_100))

        assertThat(result.ready).isTrue()
    }
}
