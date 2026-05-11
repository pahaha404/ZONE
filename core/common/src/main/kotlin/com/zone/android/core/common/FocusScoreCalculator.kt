package com.zone.android.core.common

import kotlin.math.roundToInt

/**
 * Computes the coarse focus score shown on the report screen.
 */
object FocusScoreCalculator {
    fun calculate(
        movementCount: Int,
        attentionCount: Int,
        pausedRatio: Float,
    ): Int {
        val raw = 100f - movementCount * 4f - attentionCount * 6f - pausedRatio * 40f
        return raw.coerceIn(0f, 100f).roundToInt()
    }
}

