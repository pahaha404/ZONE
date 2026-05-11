package com.zone.android.core.model

/**
 * Immutable quaternion used to store device orientation baselines.
 */
data class Quaternion(
    val w: Double,
    val x: Double,
    val y: Double,
    val z: Double,
)

