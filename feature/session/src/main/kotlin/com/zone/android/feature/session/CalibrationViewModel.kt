package com.zone.android.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.FaceAttentionAnalyzer
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.SensorFocusEngine
import com.zone.android.core.model.SessionRuntimeConfig
import com.zone.android.core.model.SessionRepository
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.isTerminalFailure
import com.zone.android.core.model.userMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the calibration gate.
 */
class CalibrationViewModel(
    private val videoId: Long,
    private val strictnessLevel: StrictnessLevel,
    private val runtimeConfig: SessionRuntimeConfig,
    private val sessionRepository: SessionRepository,
    private val sensorFocusEngine: SensorFocusEngine,
    private val faceAttentionAnalyzer: FaceAttentionAnalyzer,
) : ViewModel() {
    private val thresholds = runtimeConfig.thresholds
    private val gate = CalibrationGate(thresholds)
    private val _uiState = MutableStateFlow(CalibrationUiState(targetProgressMs = thresholds.calibrationStableMs))
    private var collectorJob: Job? = null
    private var started = false
    private var calibrationFinishing = false

    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    fun startMonitoring() {
        if (started) {
            return
        }
        started = true
        calibrationFinishing = false
        gate.reset()
        _uiState.value = CalibrationUiState(targetProgressMs = thresholds.calibrationStableMs)
        sensorFocusEngine.setBaseline(null)
        sensorFocusEngine.start()
        faceAttentionAnalyzer.start()
        collectorJob = viewModelScope.launch {
            combine(
                sensorFocusEngine.signals(),
                faceAttentionAnalyzer.signals(),
            ) { motion, attention -> motion to attention }
                .collect { (motion, attention) ->
                    if (attention.trackingStatus.isTerminalFailure()) {
                        _uiState.update { state ->
                            state.copy(
                                stable = false,
                                faceDetected = false,
                                cameraStatus = attention.trackingStatus,
                                errorMessage = attention.trackingStatus.userMessage(),
                            )
                        }
                        return@collect
                    }
                    val snapshot = gate.update(
                        timestampMillis = maxOf(motion.timestampMillis, attention.timestampMillis),
                        motionSignal = motion,
                        attentionSignal = attention,
                    )
                    _uiState.update { state ->
                        state.copy(
                            progressMs = snapshot.progressMillis.coerceAtMost(snapshot.targetMillis),
                            targetProgressMs = snapshot.targetMillis,
                            motionEnergy = motion.motionEnergy,
                            faceDetected = attention.faceDetected,
                            centered = attention.centered,
                            faceSizeRatio = attention.faceSizeRatio,
                            stable = snapshot.stable,
                            cameraStatus = attention.trackingStatus,
                            errorMessage = null,
                        )
                    }
                    if (snapshot.ready && !calibrationFinishing && _uiState.value.savedProfile == null) {
                        calibrationFinishing = true
                        saveProfile(motion, attention)
                    }
                }
        }
    }

    fun stopMonitoring() {
        collectorJob?.cancel()
        sensorFocusEngine.stop()
        faceAttentionAnalyzer.stop()
        started = false
    }

    fun retryMonitoring() {
        stopMonitoring()
        startMonitoring()
    }

    override fun onCleared() {
        stopMonitoring()
        super.onCleared()
    }

    private fun saveProfile(
        motion: MotionSignal,
        attention: AttentionSignal,
    ) {
        val quaternion = motion.quaternion ?: return
        viewModelScope.launch {
            val profile = CalibrationProfile(
                videoId = videoId,
                strictnessLevel = strictnessLevel,
                baselineQuaternion = quaternion,
                baselineFaceCenterX = attention.faceCenterX,
                baselineFaceCenterY = attention.faceCenterY,
                baselineFaceSizeRatio = attention.faceSizeRatio,
                baselineYawDegrees = attention.yawDegrees,
                baselinePitchDegrees = attention.pitchDegrees,
                baselineRollDegrees = attention.rollDegrees,
                capturedAtEpochMs = System.currentTimeMillis(),
            )
            val profileId = sessionRepository.saveCalibrationProfile(profile)
            val savedProfile = profile.copy(id = profileId)
            // Release the calibration camera before the session screen creates its own analyzer.
            stopMonitoring()
            _uiState.update { it.copy(savedProfile = savedProfile) }
        }
    }
}

/**
 * UI state for calibration.
 */
data class CalibrationUiState(
    val progressMs: Long = 0,
    val targetProgressMs: Long,
    val motionEnergy: Double = 0.0,
    val faceDetected: Boolean = false,
    val centered: Boolean = false,
    val faceSizeRatio: Float = 0f,
    val stable: Boolean = false,
    val cameraStatus: AttentionTrackingStatus = AttentionTrackingStatus.IDLE,
    val errorMessage: String? = null,
    val savedProfile: CalibrationProfile? = null,
)
