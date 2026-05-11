package com.zone.android.feature.sensors

import com.zone.android.core.model.SessionRuntimeConfig

/**
 * Pure motion rules shared by live sensor processing and tests.
 */
object MotionSignalRules {
    fun isStable(
        postureDeviationDegrees: Double,
        motionEnergy: Double,
        hasQuaternion: Boolean,
        runtimeConfig: SessionRuntimeConfig,
    ): Boolean {
        if (!hasQuaternion) {
            return false
        }

        val thresholds = runtimeConfig.thresholds
        return if (runtimeConfig.calibrationProfile == null) {
            motionEnergy <= thresholds.calibrationMotionEnergy
        } else {
            postureDeviationDegrees <= thresholds.warningPostureDegrees &&
                motionEnergy <= thresholds.motionPauseEnergy * 0.7
        }
    }
}
