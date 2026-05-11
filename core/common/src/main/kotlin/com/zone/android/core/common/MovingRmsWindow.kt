package com.zone.android.core.common

import kotlin.math.sqrt

/**
 * Sliding RMS window for motion energy calculations.
 */
class MovingRmsWindow(
    private val windowMs: Long,
) {
    private val samples = ArrayDeque<Pair<Long, Double>>()
    private var squaredSum = 0.0

    fun add(timestampMillis: Long, magnitude: Double): Double {
        val squared = magnitude * magnitude
        samples.addLast(timestampMillis to squared)
        squaredSum += squared
        trim(timestampMillis)
        return current()
    }

    fun current(): Double {
        if (samples.isEmpty()) {
            return 0.0
        }
        return sqrt(squaredSum / samples.size)
    }

    fun clear() {
        samples.clear()
        squaredSum = 0.0
    }

    private fun trim(nowMillis: Long) {
        while (samples.isNotEmpty() && nowMillis - samples.first().first > windowMs) {
            squaredSum -= samples.removeFirst().second
        }
    }
}
