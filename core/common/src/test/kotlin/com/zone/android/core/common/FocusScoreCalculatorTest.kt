package com.zone.android.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FocusScoreCalculatorTest {
    @Test
    fun `focus score follows configured weights`() {
        val score = FocusScoreCalculator.calculate(
            movementCount = 3,
            attentionCount = 2,
            pausedRatio = 0.25f,
        )

        assertThat(score).isEqualTo(66)
    }
}

