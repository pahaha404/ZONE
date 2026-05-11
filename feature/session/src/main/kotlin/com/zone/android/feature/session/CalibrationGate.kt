package com.zone.android.feature.session

import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.FocusThresholds
import com.zone.android.core.model.MotionSignal

/**
 * Pure gate that tracks whether calibration has remained stable long enough.
 */
class CalibrationGate(
    private val thresholds: FocusThresholds,
) {
    private var stableSinceMillis: Long? = null

    fun update(
        timestampMillis: Long,
        motionSignal: MotionSignal,
        attentionSignal: AttentionSignal,
    ): CalibrationSnapshot {
        val stable = motionSignal.quaternion != null &&
            motionSignal.motionEnergy <= thresholds.calibrationMotionEnergy &&
            attentionSignal.faceDetected &&
            attentionSignal.isAttentive

        if (!stable) {
            stableSinceMillis = null
            return CalibrationSnapshot(
                progressMillis = 0,
                targetMillis = thresholds.calibrationStableMs,
                stable = false,
                ready = false,
            )
        }

        val start = stableSinceMillis ?: timestampMillis.also { stableSinceMillis = it }
        val progress = timestampMillis - start
        return CalibrationSnapshot(
            progressMillis = progress,
            targetMillis = thresholds.calibrationStableMs,
            stable = true,
            ready = progress >= thresholds.calibrationStableMs,
        )
    }

    fun reset() {
        stableSinceMillis = null
    }
}

/**
 * Immutable calibration progress snapshot.
 */
data class CalibrationSnapshot(
    val progressMillis: Long,
    val targetMillis: Long,
    val stable: Boolean,
    val ready: Boolean,
)
