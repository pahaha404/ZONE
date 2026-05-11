package com.zone.android.feature.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PreStartCountdownGateTest {
    @Test
    fun `ready posture enables manual start action before arming`() {
        val gate = ManualCountdownGate(countdownDurationMs = 10_000)

        val result = gate.update(timestampMillis = 500, ready = true)

        assertThat(result.actionEnabled).isTrue()
        assertThat(result.countdownRunning).isFalse()
        assertThat(result.countdownRemainingMs).isEqualTo(10_000)
        assertThat(result.countdownCompleted).isFalse()
    }

    @Test
    fun `countdown resets when posture is broken after arming`() {
        val gate = ManualCountdownGate(countdownDurationMs = 10_000)

        gate.arm()
        gate.update(timestampMillis = 0, ready = true)
        val result = gate.update(timestampMillis = 4_000, ready = false)

        assertThat(result.actionEnabled).isFalse()
        assertThat(result.countdownRunning).isFalse()
        assertThat(result.countdownRemainingMs).isEqualTo(10_000)
        assertThat(result.countdownCompleted).isFalse()
    }

    @Test
    fun `countdown completes after full hold duration once armed`() {
        val gate = ManualCountdownGate(countdownDurationMs = 10_000)

        gate.arm()
        gate.update(timestampMillis = 0, ready = true)
        val result = gate.update(timestampMillis = 10_000, ready = true)

        assertThat(result.actionEnabled).isFalse()
        assertThat(result.countdownRunning).isFalse()
        assertThat(result.countdownRemainingMs).isEqualTo(0)
        assertThat(result.countdownCompleted).isTrue()
    }
}
