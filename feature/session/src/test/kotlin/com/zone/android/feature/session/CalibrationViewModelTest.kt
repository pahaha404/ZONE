package com.zone.android.feature.session

import com.google.common.truth.Truth.assertThat
import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.FaceAttentionAnalyzer
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.Quaternion
import com.zone.android.core.model.SensorFocusEngine
import com.zone.android.core.model.SessionEvent
import com.zone.android.core.model.SessionReport
import com.zone.android.core.model.SessionRepository
import com.zone.android.core.model.SessionRuntimeConfig
import com.zone.android.core.model.SessionStanding
import com.zone.android.core.model.SessionSummary
import com.zone.android.core.model.StrictnessLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalibrationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calibration completion stops monitoring before handoff`() = runTest(dispatcher) {
        val sessionRepository = FakeSessionRepository()
        val sensorEngine = FakeSensorFocusEngine()
        val faceAnalyzer = FakeFaceAttentionAnalyzer()
        val viewModel = CalibrationViewModel(
            videoId = 7L,
            strictnessLevel = StrictnessLevel.STANDARD,
            runtimeConfig = SessionRuntimeConfig(thresholds = com.zone.android.core.model.FocusThresholds.Standard),
            sessionRepository = sessionRepository,
            sensorFocusEngine = sensorEngine,
            faceAttentionAnalyzer = faceAnalyzer,
        )

        viewModel.startMonitoring()
        advanceUntilIdle()

        sensorEngine.emit(stableMotion(0L))
        faceAnalyzer.emit(stableAttention(0L))
        advanceUntilIdle()
        sensorEngine.emit(stableMotion(2_000L))
        faceAnalyzer.emit(stableAttention(2_000L))
        advanceUntilIdle()
        sensorEngine.emit(stableMotion(3_100L))
        faceAnalyzer.emit(stableAttention(3_100L))
        advanceUntilIdle()

        val savedProfile = viewModel.uiState.value.savedProfile
        assertThat(savedProfile).isNotNull()
        assertThat(sensorEngine.startCount).isEqualTo(1)
        assertThat(faceAnalyzer.startCount).isEqualTo(1)
        assertThat(sensorEngine.stopCount).isEqualTo(1)
        assertThat(faceAnalyzer.stopCount).isEqualTo(1)
        assertThat(savedProfile?.id).isEqualTo(101L)
    }

    private fun stableMotion(timestampMillis: Long) = MotionSignal(
        timestampMillis = timestampMillis,
        postureDeviationDegrees = 0.0,
        motionEnergy = 0.05,
        quaternion = Quaternion(1.0, 0.0, 0.0, 0.0),
        isStable = true,
    )

    private fun stableAttention(timestampMillis: Long) = AttentionSignal(
        timestampMillis = timestampMillis,
        faceDetected = true,
        centered = true,
        faceCenterX = 0.5f,
        faceCenterY = 0.5f,
        faceSizeRatio = 0.15f,
        yawDegrees = 0f,
        pitchDegrees = 0f,
        rollDegrees = 0f,
        isAttentive = true,
        trackingStatus = AttentionTrackingStatus.RUNNING,
    )
}

private class FakeSensorFocusEngine : SensorFocusEngine {
    private val signals = MutableStateFlow(
        MotionSignal(
            timestampMillis = 0L,
            postureDeviationDegrees = 0.0,
            motionEnergy = 1.0,
            quaternion = null,
            isStable = false,
        ),
    )

    var startCount = 0
        private set
    var stopCount = 0
        private set

    override fun signals(): Flow<MotionSignal> = signals.asStateFlow()

    override fun setBaseline(quaternion: Quaternion?) = Unit

    override fun start() {
        startCount += 1
    }

    override fun stop() {
        stopCount += 1
    }

    fun emit(signal: MotionSignal) {
        signals.value = signal
    }
}

private class FakeFaceAttentionAnalyzer : FaceAttentionAnalyzer {
    private val signals = MutableStateFlow(
        AttentionSignal(
            timestampMillis = 0L,
            faceDetected = false,
            centered = false,
            faceCenterX = 0f,
            faceCenterY = 0f,
            faceSizeRatio = 0f,
            yawDegrees = 0f,
            pitchDegrees = 0f,
            rollDegrees = 0f,
            isAttentive = false,
            trackingStatus = AttentionTrackingStatus.IDLE,
        ),
    )

    var startCount = 0
        private set
    var stopCount = 0
        private set

    override fun signals(): Flow<AttentionSignal> = signals.asStateFlow()

    override fun start() {
        startCount += 1
    }

    override fun stop() {
        stopCount += 1
    }

    fun emit(signal: AttentionSignal) {
        signals.value = signal
    }
}

private class FakeSessionRepository : SessionRepository {
    override suspend fun createSession(
        videoId: Long,
        strictnessLevel: StrictnessLevel,
        startedAtEpochMs: Long,
    ): Long = error("Not used in calibration test")

    override suspend fun updateSessionSummary(sessionId: Long, summary: SessionSummary) = Unit

    override suspend fun appendEvent(event: SessionEvent) = Unit

    override suspend fun saveCalibrationProfile(profile: CalibrationProfile): Long = 101L

    override suspend fun getLatestCalibrationProfile(
        videoId: Long,
        strictnessLevel: StrictnessLevel,
    ): CalibrationProfile? = null

    override suspend fun getSessionReport(sessionId: Long): SessionReport? = null

    override suspend fun getSessionStanding(sessionId: Long): SessionStanding? = null
}
