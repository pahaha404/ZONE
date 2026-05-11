package com.zone.android.feature.session

import android.os.SystemClock
import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.common.FocusScoreCalculator
import com.zone.android.core.model.FocusViolation
import com.zone.android.core.model.FaceAttentionAnalyzer
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.PlayerCoordinator
import com.zone.android.core.model.PlaybackSnapshot
import com.zone.android.core.model.SensorFocusEngine
import com.zone.android.core.model.SessionController
import com.zone.android.core.model.SessionEvent
import com.zone.android.core.model.SessionEventType
import com.zone.android.core.model.SessionPhase
import com.zone.android.core.model.SessionRepository
import com.zone.android.core.model.SessionRuntimeConfig
import com.zone.android.core.model.SessionSummary
import com.zone.android.core.model.SessionUiState
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.VideoItem
import com.zone.android.core.model.sessionEndReasonLabel
import com.zone.android.core.model.isTerminalFailure
import com.zone.android.core.model.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Default live session orchestrator that combines sensors, camera, player, and persistence.
 */
class DefaultSessionController(
    private val videoItem: VideoItem,
    private val strictnessLevel: StrictnessLevel,
    private val runtimeConfig: SessionRuntimeConfig,
    private val sessionIntent: String,
    private val sessionRepository: SessionRepository,
    private val sensorFocusEngine: SensorFocusEngine,
    private val faceAttentionAnalyzer: FaceAttentionAnalyzer,
    private val playerCoordinator: PlayerCoordinator,
) : SessionController {
    companion object {
        private const val PRE_START_COUNTDOWN_MS = 10_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stateMachine = SessionStateMachine(runtimeConfig.thresholds)
    private val preStartCountdownGate = ManualCountdownGate(PRE_START_COUNTDOWN_MS)
    private val _uiState = MutableStateFlow(SessionUiState())
    private val _completedSessionId = MutableStateFlow<Long?>(null)

    private var started = false
    private var finished = false
    private var playbackStarted = false
    private var sessionId: Long? = null
    private var sessionStartedElapsedMillis: Long = 0L
    private var controllerStartedElapsedMillis: Long = 0L
    private var collectorJob: Job? = null

    override val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()
    val completedSessionId: StateFlow<Long?> = _completedSessionId.asStateFlow()

    override fun start() {
        if (started) {
            return
        }
        started = true
        playbackStarted = false
        preStartCountdownGate.reset()
        scope.launch {
            sessionId = sessionRepository.createSession(
                videoId = videoItem.id,
                strictnessLevel = strictnessLevel,
                startedAtEpochMs = System.currentTimeMillis(),
            )
            sessionId?.let { activeSessionId ->
                sessionRepository.appendEvent(
                    SessionEvent(
                        sessionId = activeSessionId,
                        eventType = SessionEventType.STARTED,
                        timestampMillis = System.currentTimeMillis(),
                        message = "목표: ${sessionIntent.trim()}",
                    ),
                )
            }
            controllerStartedElapsedMillis = SystemClock.elapsedRealtime()
            sessionStartedElapsedMillis = 0L
            playerCoordinator.prepare(videoItem.contentUri)
            sensorFocusEngine.setBaseline(runtimeConfig.calibrationProfile?.baselineQuaternion)
            sensorFocusEngine.start()
            faceAttentionAnalyzer.start()
            _uiState.value = SessionUiState(
                phase = SessionPhase.READY,
                warningMessage = "화면을 바라보고 휴대폰을 고정해 주세요",
                primaryActionLabel = null,
                primaryActionEnabled = false,
                countdownRemainingMs = PRE_START_COUNTDOWN_MS,
                playback = playerCoordinator.playback.value,
            )

            collectorJob = scope.launch {
                combine(
                    sensorFocusEngine.signals(),
                    faceAttentionAnalyzer.signals(),
                    playerCoordinator.playback,
                ) { motion, attention, playback ->
                    Triple(motion, attention, playback)
                }.collect { (motion, attention, playback) ->
                    if (attention.trackingStatus.isTerminalFailure()) {
                        val errorMessage = attention.trackingStatus.userMessage() ?: "카메라를 사용할 수 없습니다"
                        playerCoordinator.pause()
                        _uiState.value = _uiState.value.copy(
                            phase = SessionPhase.PAUSED_ATTENTION,
                            activeViolation = FocusViolation.NONE,
                            warningMessage = errorMessage,
                            fatalErrorMessage = errorMessage,
                            playback = playback,
                        )
                        return@collect
                    }
                    val nowMillis = maxOf(SystemClock.elapsedRealtime(), motion.timestampMillis, attention.timestampMillis)
                    if (!playbackStarted) {
                        handlePreStart(
                            timestampMillis = nowMillis,
                            motionSignal = motion,
                            attentionSignal = attention,
                            playbackSnapshot = playback,
                        )
                        return@collect
                    }
                    val result = stateMachine.update(
                        timestampMillis = nowMillis,
                        motionSignal = motion,
                        attentionSignal = attention,
                        playbackSnapshot = playback,
                    )
                    _uiState.value = result.uiState.copy(playback = playback)
                    applyPlayerCommand(result.playerCommand)
                    persistEvents(result.events)
                    if (result.uiState.phase == SessionPhase.SESSION_DONE) {
                        finish("completed")
                    }
                }
            }
        }
    }

    override fun finish(reason: String) {
        if (finished) {
            return
        }
        finished = true
        scope.launch {
            collectorJob?.cancel()
            sensorFocusEngine.stop()
            faceAttentionAnalyzer.stop()
            playerCoordinator.pause()

            val endedAtElapsed = SystemClock.elapsedRealtime()
            val metrics = stateMachine.finish(endedAtElapsed)
            val effectiveSessionStart = sessionStartedElapsedMillis.takeIf { it > 0L } ?: controllerStartedElapsedMillis
            val totalDuration = (endedAtElapsed - effectiveSessionStart).coerceAtLeast(0L)
            val pausedRatio = if (totalDuration == 0L) {
                0f
            } else {
                ((totalDuration - metrics.actualPlaybackMillis).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            }
            val focusScore = FocusScoreCalculator.calculate(
                movementCount = metrics.movementViolationCount,
                attentionCount = metrics.attentionViolationCount,
                pausedRatio = pausedRatio,
            )
            val completionRate = if (videoItem.durationMs > 0L) {
                (metrics.actualPlaybackMillis.toFloat() / videoItem.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            sessionId?.let { activeSessionId ->
                sessionRepository.updateSessionSummary(
                    sessionId = activeSessionId,
                    summary = SessionSummary(
                        endedAtEpochMs = System.currentTimeMillis(),
                        totalDurationMs = totalDuration,
                        actualPlaybackMs = metrics.actualPlaybackMillis,
                        movementViolationCount = metrics.movementViolationCount,
                        attentionViolationCount = metrics.attentionViolationCount,
                        averageStableSegmentMs = metrics.averageStableSegmentMillis,
                        completionRate = completionRate,
                        focusScore = focusScore,
                        endedReason = reason,
                    ),
                )
                sessionRepository.appendEvent(
                    SessionEvent(
                        sessionId = activeSessionId,
                        eventType = SessionEventType.COMPLETED,
                        timestampMillis = System.currentTimeMillis(),
                        message = sessionEndReasonLabel(reason),
                    ),
                )
                _completedSessionId.value = activeSessionId
            }
            playerCoordinator.release()
        }
    }

    private suspend fun persistEvents(events: List<PendingSessionEvent>) {
        val activeSessionId = sessionId ?: return
        events.forEach { event ->
            sessionRepository.appendEvent(
                SessionEvent(
                    sessionId = activeSessionId,
                    eventType = event.type,
                    violation = event.violation,
                    timestampMillis = System.currentTimeMillis(),
                    message = event.message,
                ),
            )
        }
    }

    private fun applyPlayerCommand(command: PlayerCommand?) {
        when (command) {
            PlayerCommand.PLAY -> playerCoordinator.play()
            PlayerCommand.PAUSE -> playerCoordinator.pause()
            null -> Unit
        }
    }

    private fun handlePreStart(
        timestampMillis: Long,
        motionSignal: MotionSignal,
        attentionSignal: AttentionSignal,
        playbackSnapshot: PlaybackSnapshot,
    ) {
        val ready = isReadyForPlaybackStart(
            timestampMillis = timestampMillis,
            motionSignal = motionSignal,
            attentionSignal = attentionSignal,
        )
        val countdownSnapshot = preStartCountdownGate.update(
            timestampMillis = timestampMillis,
            ready = ready,
        )

        if (countdownSnapshot.countdownCompleted) {
            playbackStarted = true
            sessionStartedElapsedMillis = timestampMillis
            val startResult = stateMachine.start(timestampMillis)
            _uiState.value = startResult.uiState.copy(playback = playbackSnapshot)
            applyPlayerCommand(startResult.playerCommand)
            return
        }

        playerCoordinator.pause()
        _uiState.value = SessionUiState(
            phase = if (countdownSnapshot.countdownRunning) SessionPhase.READY_COUNTDOWN else SessionPhase.READY,
            activeViolation = FocusViolation.NONE,
            warningMessage = if (countdownSnapshot.countdownRunning) {
                "10초 후에 영상이 시작됩니다"
            } else {
                if (countdownSnapshot.actionEnabled) {
                    "조건이 확인되었습니다. 터치하면 10초 후 영상이 시작됩니다"
                } else {
                    "화면을 바라보고 휴대폰을 고정해 주세요"
                }
            },
            primaryActionLabel = if (countdownSnapshot.actionEnabled) "터치해서 시작" else null,
            primaryActionEnabled = countdownSnapshot.actionEnabled,
            countdownRemainingMs = countdownSnapshot.countdownRemainingMs,
            playback = playbackSnapshot,
        )
    }

    private fun isReadyForPlaybackStart(
        timestampMillis: Long,
        motionSignal: MotionSignal,
        attentionSignal: AttentionSignal,
    ): Boolean {
        val thresholds = runtimeConfig.thresholds
        val cameraSignalAgeMillis = (timestampMillis - attentionSignal.timestampMillis).coerceAtLeast(0L)
        val cameraSignalStale = cameraSignalAgeMillis >= thresholds.faceMissingHoldMs
        return motionSignal.postureDeviationDegrees < thresholds.warningPostureDegrees &&
            motionSignal.motionEnergy < thresholds.motionPauseEnergy * 0.7 &&
            !cameraSignalStale &&
            attentionSignal.faceDetected &&
            attentionSignal.isAttentive
    }

    fun onPrimaryAction() {
        when {
            !playbackStarted && _uiState.value.primaryActionEnabled -> {
                preStartCountdownGate.arm()
                _uiState.value = _uiState.value.copy(
                    phase = SessionPhase.READY_COUNTDOWN,
                    warningMessage = "10초 후에 영상이 시작됩니다",
                    primaryActionLabel = null,
                    primaryActionEnabled = false,
                    countdownRemainingMs = PRE_START_COUNTDOWN_MS,
                )
            }
            playbackStarted && _uiState.value.primaryActionEnabled -> {
                stateMachine.requestResume()
                _uiState.value = _uiState.value.copy(
                    phase = SessionPhase.RECOVERING,
                    warningMessage = "정면을 보고 3초 동안 유지해 주세요",
                    primaryActionLabel = null,
                    primaryActionEnabled = false,
                    recoverRemainingMs = runtimeConfig.thresholds.recoverStableMs,
                )
            }
        }
    }
}
