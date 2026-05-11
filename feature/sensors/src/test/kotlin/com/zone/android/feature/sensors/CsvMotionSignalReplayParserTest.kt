package com.zone.android.feature.sensors

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvMotionSignalReplayParserTest {
    @Test
    fun `parser creates motion signals from csv`() {
        val csv = """
            timestampMillis,postureDeviationDegrees,motionEnergy
            0,0.0,0.1
            100,12.5,1.6
        """.trimIndent()

        val signals = CsvMotionSignalReplayParser.parse(csv)

        assertThat(signals).hasSize(2)
        assertThat(signals[1].postureDeviationDegrees).isEqualTo(12.5)
        assertThat(signals[1].motionEnergy).isEqualTo(1.6)
    }
}

