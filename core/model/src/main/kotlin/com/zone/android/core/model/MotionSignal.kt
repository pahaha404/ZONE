package com.zone.android.core.model

/**
 * Snapshot of device motion derived from live sensor streams.
 */
data class MotionSignal(
    val timestampMillis: Long,
    val postureDeviationDegrees: Double,
    val motionEnergy: Double,
    val quaternion: Quaternion?,
    val isStable: Boolean,
)

