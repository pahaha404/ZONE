package com.zone.android.core.common

import com.zone.android.core.model.Quaternion
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Helpers for quaternion normalization and angular distance calculations.
 */
object QuaternionMath {
    fun normalize(quaternion: Quaternion): Quaternion {
        val magnitude = sqrt(
            quaternion.w * quaternion.w +
                quaternion.x * quaternion.x +
                quaternion.y * quaternion.y +
                quaternion.z * quaternion.z,
        )
        if (magnitude == 0.0) {
            return Quaternion(1.0, 0.0, 0.0, 0.0)
        }
        return Quaternion(
            w = quaternion.w / magnitude,
            x = quaternion.x / magnitude,
            y = quaternion.y / magnitude,
            z = quaternion.z / magnitude,
        )
    }

    fun angularDistanceDegrees(reference: Quaternion, candidate: Quaternion): Double {
        val a = normalize(reference)
        val b = normalize(candidate)
        val dot = max(-1.0, min(1.0, a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z))
        return Math.toDegrees(2.0 * acos(kotlin.math.abs(dot)))
    }
}

