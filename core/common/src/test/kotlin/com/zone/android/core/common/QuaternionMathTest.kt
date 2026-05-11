package com.zone.android.core.common

import com.google.common.truth.Truth.assertThat
import com.zone.android.core.model.Quaternion
import org.junit.Test

class QuaternionMathTest {
    @Test
    fun `angular distance is zero for same quaternion`() {
        val quaternion = Quaternion(1.0, 0.0, 0.0, 0.0)

        val distance = QuaternionMath.angularDistanceDegrees(quaternion, quaternion)

        assertThat(distance).isWithin(0.0001).of(0.0)
    }

    @Test
    fun `angular distance matches quarter turn`() {
        val identity = Quaternion(1.0, 0.0, 0.0, 0.0)
        val quarterTurn = Quaternion(0.70710678, 0.0, 0.70710678, 0.0)

        val distance = QuaternionMath.angularDistanceDegrees(identity, quarterTurn)

        assertThat(distance).isWithin(0.5).of(90.0)
    }
}

