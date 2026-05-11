package com.zone.android.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FocusThresholdsTest {
    @Test
    fun `light preset relaxes standard thresholds`() {
        val light = StrictnessLevel.LIGHT.thresholds()
        val standard = StrictnessLevel.STANDARD.thresholds()

        assertThat(light.pausePostureDegrees).isGreaterThan(standard.pausePostureDegrees)
        assertThat(light.faceMissingHoldMs).isGreaterThan(standard.faceMissingHoldMs)
        assertThat(light.faceSizeRetentionRatio).isLessThan(standard.faceSizeRetentionRatio)
    }

    @Test
    fun `hard preset tightens standard thresholds`() {
        val hard = StrictnessLevel.HARD.thresholds()
        val standard = StrictnessLevel.STANDARD.thresholds()

        assertThat(hard.pausePostureDegrees).isLessThan(standard.pausePostureDegrees)
        assertThat(hard.faceMissingHoldMs).isLessThan(standard.faceMissingHoldMs)
        assertThat(hard.faceSizeRetentionRatio).isGreaterThan(standard.faceSizeRetentionRatio)
    }
}
