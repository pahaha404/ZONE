package com.zone.android.feature.session

import com.google.common.truth.Truth.assertThat
import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.FocusThresholds
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.PlaybackSnapshot
import com.zone.android.core.model.SessionEventType
import org.junit.Test

class SessionStateMachineTest {
    @Test
    fun `movement pause transitions to paused movement`() {
        val machine = SessionStateMachine(FocusThresholds.Standard)
        val stableAttention = stableAttention()
        val unstableMotion = motionSignal(
            postureDeviationDegrees = 26.0,
            motionEnergy = 1.7,
            isStable = false,
        )

        machine.start(0)
        machine.update(0, unstableMotion, stableAttention, PlaybackSnapshot(isPlaying = true))
        val result = machine.update(
            900,
            unstableMotion.copy(timestampMillis = 900),
            stableAttention.copy(timestampMillis = 900),
            PlaybackSnapshot(isPlaying = true),
        )

        assertThat(result.uiState.phase.name).isEqualTo("PAUSED_MOVEMENT")
        assertThat(result.events.single().type.name).contains("PAUSE")
    }

    @Test
    fun `stable recovery resumes playback`() {
        val machine = SessionStateMachine(FocusThresholds.Standard)
        val attention = stableAttention()
        val badMotion = motionSignal(
            postureDeviationDegrees = 28.0,
            motionEnergy = 1.8,
            isStable = false,
        )
        val goodMotion = motionSignal(
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.1,
            isStable = true,
        )

        machine.start(0)
        machine.update(0, badMotion, attention, PlaybackSnapshot(isPlaying = true))
        machine.update(900, badMotion.copy(timestampMillis = 900), attention.copy(timestampMillis = 900), PlaybackSnapshot(isPlaying = true))
        machine.update(3_500, goodMotion, attention.copy(timestampMillis = 3_500), PlaybackSnapshot(isPlaying = false))
        machine.requestResume()
        val result = machine.update(
            6_500,
            goodMotion.copy(timestampMillis = 6_500),
            attention.copy(timestampMillis = 6_500),
            PlaybackSnapshot(isPlaying = false),
        )

        assertThat(result.playerCommand).isEqualTo(PlayerCommand.PLAY)
        assertThat(result.uiState.phase.name).isEqualTo("PLAYING")
    }

    @Test
    fun `stable posture alone does not resume without touch request`() {
        val machine = SessionStateMachine(FocusThresholds.Standard)
        val attention = stableAttention()
        val badMotion = motionSignal(
            postureDeviationDegrees = 28.0,
            motionEnergy = 1.8,
            isStable = false,
        )
        val goodMotion = motionSignal(
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.1,
            isStable = true,
        )

        machine.start(0)
        machine.update(0, badMotion, attention, PlaybackSnapshot(isPlaying = true))
        machine.update(900, badMotion.copy(timestampMillis = 900), attention.copy(timestampMillis = 900), PlaybackSnapshot(isPlaying = true))
        val result = machine.update(
            4_500,
            goodMotion.copy(timestampMillis = 4_500),
            attention.copy(timestampMillis = 4_500),
            PlaybackSnapshot(isPlaying = false),
        )

        assertThat(result.playerCommand).isNull()
        assertThat(result.uiState.phase.name).isEqualTo("PAUSED_MOVEMENT")
        assertThat(result.uiState.primaryActionEnabled).isTrue()
        assertThat(result.uiState.primaryActionLabel).isEqualTo("터치해서 다시 시작")
    }

    @Test
    fun `warning event is emitted before movement pause`() {
        val machine = SessionStateMachine(FocusThresholds.Standard)
        val attention = stableAttention()
        val warningMotion = motionSignal(
            postureDeviationDegrees = 15.0,
            motionEnergy = 0.3,
            isStable = false,
        )

        machine.start(0)
        machine.update(0, warningMotion, attention, PlaybackSnapshot(isPlaying = true))
        val result = machine.update(
            900,
            warningMotion.copy(timestampMillis = 900),
            attention.copy(timestampMillis = 900),
            PlaybackSnapshot(isPlaying = true),
        )

        assertThat(result.uiState.phase.name).isEqualTo("WARNING")
        assertThat(result.events.single().type).isEqualTo(SessionEventType.WARNING)
    }

    @Test
    fun `repeated pauses trigger lockout`() {
        val thresholds = FocusThresholds.Standard.copy(
            pauseBurstWindowMs = 10_000,
            lockoutPauseCount = 2,
            recalibrationPauseCount = 4,
        )
        val machine = SessionStateMachine(thresholds)
        val attention = stableAttention()
        val badMotion = motionSignal(
            postureDeviationDegrees = 26.0,
            motionEnergy = 1.7,
            isStable = false,
        )
        val goodMotion = motionSignal(
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.1,
            isStable = true,
        )

        machine.start(0)
        machine.update(0, badMotion, attention, PlaybackSnapshot(isPlaying = true))
        val firstPause = machine.update(
            900,
            badMotion.copy(timestampMillis = 900),
            attention.copy(timestampMillis = 900),
            PlaybackSnapshot(isPlaying = true),
        )
        machine.update(
            3_500,
            goodMotion.copy(timestampMillis = 3_500),
            attention.copy(timestampMillis = 3_500),
            PlaybackSnapshot(isPlaying = false),
        )
        machine.update(
            6_100,
            goodMotion.copy(timestampMillis = 6_100),
            attention.copy(timestampMillis = 6_100),
            PlaybackSnapshot(isPlaying = false),
        )
        machine.update(
            6_200,
            badMotion.copy(timestampMillis = 6_200),
            attention.copy(timestampMillis = 6_200),
            PlaybackSnapshot(isPlaying = true),
        )
        val secondPause = machine.update(
            7_100,
            badMotion.copy(timestampMillis = 7_100),
            attention.copy(timestampMillis = 7_100),
            PlaybackSnapshot(isPlaying = true),
        )

        assertThat(firstPause.events.single().type).isEqualTo(SessionEventType.PAUSE)
        assertThat(secondPause.events.single().type).isEqualTo(SessionEventType.LOCKOUT)
    }

    @Test
    fun `pause can immediately require recalibration when threshold is reached`() {
        val machine = SessionStateMachine(
            FocusThresholds.Standard.copy(
                lockoutPauseCount = 99,
                recalibrationPauseCount = 1,
            ),
        )
        val attention = stableAttention()
        val badMotion = motionSignal(
            postureDeviationDegrees = 26.0,
            motionEnergy = 1.7,
            isStable = false,
        )

        machine.start(0)
        machine.update(0, badMotion, attention, PlaybackSnapshot(isPlaying = true))
        val recalibration = machine.update(
            900,
            badMotion.copy(timestampMillis = 900),
            attention.copy(timestampMillis = 900),
            PlaybackSnapshot(isPlaying = true),
        )

        assertThat(recalibration.events.single().type).isEqualTo(SessionEventType.REQUIRES_RECALIBRATION)
    }

    @Test
    fun `motion energy uses dedicated hold duration`() {
        val machine = SessionStateMachine(
            FocusThresholds.Standard.copy(
                pausePostureDegrees = 99.0,
                pausePostureHoldMs = 1_000,
                motionPauseEnergy = 1.1,
                motionPauseHoldMs = 600,
            ),
        )
        val attention = stableAttention()
        val motionOnlyViolation = motionSignal(
            postureDeviationDegrees = 0.0,
            motionEnergy = 1.3,
            isStable = false,
        )

        machine.start(0)
        machine.update(0, motionOnlyViolation, attention, PlaybackSnapshot(isPlaying = true))

        val beforeHold = machine.update(
            500,
            motionOnlyViolation.copy(timestampMillis = 500),
            attention.copy(timestampMillis = 500),
            PlaybackSnapshot(isPlaying = true),
        )
        val pause = machine.update(
            600,
            motionOnlyViolation.copy(timestampMillis = 600),
            attention.copy(timestampMillis = 600),
            PlaybackSnapshot(isPlaying = true),
        )

        assertThat(beforeHold.uiState.phase.name).isEqualTo("PLAYING")
        assertThat(beforeHold.events).isEmpty()
        assertThat(pause.uiState.phase.name).isEqualTo("PAUSED_MOVEMENT")
        assertThat(pause.events.single().type).isEqualTo(SessionEventType.PAUSE)
    }

    @Test
    fun `stale attention signal pauses playback as attention violation`() {
        val machine = SessionStateMachine(FocusThresholds.Standard)
        val stableMotion = motionSignal(
            timestampMillis = 1_100,
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.1,
            isStable = true,
        )

        machine.start(0)
        val result = machine.update(
            timestampMillis = 1_100,
            motionSignal = stableMotion,
            attentionSignal = stableAttention(timestampMillis = 0),
            playbackSnapshot = PlaybackSnapshot(isPlaying = true),
        )

        assertThat(result.uiState.phase.name).isEqualTo("PAUSED_ATTENTION")
        assertThat(result.events.single().type).isEqualTo(SessionEventType.PAUSE)
    }

    private fun stableAttention(timestampMillis: Long = 0) = AttentionSignal(
        timestampMillis = timestampMillis,
        faceDetected = true,
        centered = true,
        faceCenterX = 0.5f,
        faceCenterY = 0.5f,
        faceSizeRatio = 0.12f,
        yawDegrees = 0f,
        pitchDegrees = 0f,
        rollDegrees = 0f,
        isAttentive = true,
        trackingStatus = AttentionTrackingStatus.RUNNING,
    )

    private fun motionSignal(
        timestampMillis: Long = 0,
        postureDeviationDegrees: Double,
        motionEnergy: Double,
        isStable: Boolean,
    ) = MotionSignal(
        timestampMillis = timestampMillis,
        postureDeviationDegrees = postureDeviationDegrees,
        motionEnergy = motionEnergy,
        quaternion = null,
        isStable = isStable,
    )
}
