package com.zone.android.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovingRmsWindowTest {
    @Test
    fun `window trims old samples`() {
        val window = MovingRmsWindow(windowMs = 1_000)
        window.add(0, 1.0)
        window.add(500, 1.0)

        val rms = window.add(1_500, 2.0)

        assertThat(rms).isWithin(0.001).of(2.0)
    }
}

