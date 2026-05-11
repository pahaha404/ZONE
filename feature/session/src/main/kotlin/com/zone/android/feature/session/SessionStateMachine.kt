package com.zone.android.feature.session

import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.FocusThresholds
import com.zone.android.core.model.FocusViolation
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.PlaybackSnapshot
import com.zone.android.core.model.SessionEventType
import com.zone.android.core.model.SessionPhase
import com.zone.android.core.model.SessionUiState

/**
 * Pure session state machine for warning, pause, recovery, and lockout transitions.
 */
class SessionStateMachine(
    private val thresholds: FocusThresholds,
) {
    private var currentPhase: SessionPhase = SessionPhase.READY
    private var movementWarningSinceMillis: Long? = null
    private var posturePauseSinceMillis: Long? = null
    private var motionPauseSinceMillis: Long? = null
    private var faceMissingSinceMillis: Long? = null
    private var headPoseSinceMillis: Long? = null
    private var recoverySinceMillis: Long? = null
    private var recoveryRequested: Boolean = false
    private var lockoutUntilMillis: Long? = null
    private var lastPauseViolation: FocusViolation = FocusViolation.NONE

    private var movementViolationCount: Int = 0
    private var attentionViolationCount: Int = 0
    private val pauseTimestamps = ArrayDeque<Long>()
    private val stableSegments = mutableListOf<Long>()
    private var stableSegmentStartedAtMillis: Long? = null
    private var playingAccumulatedMillis: Long = 0
    private var playingSinceMillis: Long? = null

    fun start(timestampMillis: Long): SessionMachineResult {
        currentPhase = SessionPhase.PLAYING
        recoveryRequested = false
        playingSinceMillis = timestampMillis
        stableSegmentStartedAtMillis = timestampMillis
        return result(timestampMillis).copy(playerCommand = PlayerCommand.PLAY)
    }

    fun requestResume() {
        if (currentPhase == SessionPhase.PAUSED_MOVEMENT || currentPhase == SessionPhase.PAUSED_ATTENTION) {
            recoveryRequested = true
            recoverySinceMillis = null
        }
    }

    fun update(
        timestampMillis: Long,
        motionSignal: MotionSignal,
        attentionSignal: AttentionSignal,
        playbackSnapshot: PlaybackSnapshot,
    ): SessionMachineResult {
        val events = mutableListOf<PendingSessionEvent>()
        var playerCommand: PlayerCommand? = null

        if (playbackSnapshot.isEnded && currentPhase != SessionPhase.SESSION_DONE) {
            currentPhase = SessionPhase.SESSION_DONE
            closePlayingWindow(timestampMillis)
            events += PendingSessionEvent(SessionEventType.COMPLETED, FocusViolation.NONE, "영상 시청 완료")
            return result(timestampMillis, playbackSnapshot).copy(events = events)
        }

        val cameraSignalAgeMillis = (timestampMillis - attentionSignal.timestampMillis).coerceAtLeast(0L)
        val cameraSignalStale = cameraSignalAgeMillis >= thresholds.faceMissingHoldMs
        val motionWarningActive = motionSignal.postureDeviationDegrees >= thresholds.warningPostureDegrees ||
            motionSignal.motionEnergy >= thresholds.motionPauseEnergy * 0.8
        val posturePauseActive = motionSignal.postureDeviationDegrees >= thresholds.pausePostureDegrees
        val motionPauseActive = motionSignal.motionEnergy >= thresholds.motionPauseEnergy
        val faceMissing = cameraSignalStale || !attentionSignal.faceDetected
        val attentionPauseActive = !cameraSignalStale && attentionSignal.faceDetected && !attentionSignal.isAttentive
        val stableNow = motionSignal.postureDeviationDegrees < thresholds.warningPostureDegrees &&
            motionSignal.motionEnergy < thresholds.motionPauseEnergy * 0.7 &&
            !cameraSignalStale &&
            attentionSignal.faceDetected &&
            attentionSignal.isAttentive

        movementWarningSinceMillis = accumulateStart(motionWarningActive, movementWarningSinceMillis, timestampMillis)
        posturePauseSinceMillis = accumulateStart(posturePauseActive, posturePauseSinceMillis, timestampMillis)
        motionPauseSinceMillis = accumulateStart(motionPauseActive, motionPauseSinceMillis, timestampMillis)
        faceMissingSinceMillis = accumulateStart(faceMissing, faceMissingSinceMillis, timestampMillis)
        headPoseSinceMillis = accumulateStart(attentionPauseActive, headPoseSinceMillis, timestampMillis)

        val warningHeld = heldLongEnough(movementWarningSinceMillis, thresholds.warningPostureHoldMs, timestampMillis)
        val movementPauseHeld =
            heldLongEnough(posturePauseSinceMillis, thresholds.pausePostureHoldMs, timestampMillis) ||
                heldLongEnough(motionPauseSinceMillis, thresholds.motionPauseHoldMs, timestampMillis)
        val faceMissingHeld = cameraSignalStale || heldLongEnough(faceMissingSinceMillis, thresholds.faceMissingHoldMs, timestampMillis)
        val headPoseHeld = heldLongEnough(headPoseSinceMillis, thresholds.headPoseHoldMs, timestampMillis)

        when (currentPhase) {
            SessionPhase.READY -> {
                currentPhase = SessionPhase.PLAYING
                playerCommand = PlayerCommand.PLAY
                playingSinceMillis = timestampMillis
                stableSegmentStartedAtMillis = timestampMillis
            }
            SessionPhase.READY_COUNTDOWN -> Unit
            SessionPhase.PLAYING,
            SessionPhase.WARNING,
            -> {
                when {
                    movementPauseHeld -> {
                        closePlayingWindow(timestampMillis)
                        movementViolationCount += 1
                        lastPauseViolation = FocusViolation.MOVEMENT
                        events += registerPause(timestampMillis, FocusViolation.MOVEMENT)
                        playerCommand = PlayerCommand.PAUSE
                    }
                    faceMissingHeld || headPoseHeld -> {
                        closePlayingWindow(timestampMillis)
                        attentionViolationCount += 1
                        lastPauseViolation = FocusViolation.ATTENTION
                        events += registerPause(timestampMillis, FocusViolation.ATTENTION)
                        playerCommand = PlayerCommand.PAUSE
                    }
                    warningHeld -> {
                        if (currentPhase != SessionPhase.WARNING) {
                            events += PendingSessionEvent(
                                type = SessionEventType.WARNING,
                                violation = FocusViolation.MOVEMENT,
                                message = "휴대폰을 고정해 주세요",
                            )
                        }
                        currentPhase = SessionPhase.WARNING
                    }
                    else -> currentPhase = SessionPhase.PLAYING
                }
            }
            SessionPhase.PAUSED_MOVEMENT,
            SessionPhase.PAUSED_ATTENTION,
            SessionPhase.RECOVERING,
            -> {
                when {
                    !recoveryRequested -> {
                        recoverySinceMillis = null
                        currentPhase = pausedPhaseFor(lastPauseViolation)
                    }
                    stableNow -> {
                    val start = recoverySinceMillis ?: timestampMillis.also { recoverySinceMillis = it }
                    currentPhase = SessionPhase.RECOVERING
                    if (timestampMillis - start >= thresholds.recoverStableMs) {
                        recoverySinceMillis = null
                        recoveryRequested = false
                        currentPhase = SessionPhase.PLAYING
                        playerCommand = PlayerCommand.PLAY
                        events += PendingSessionEvent(
                            type = SessionEventType.RESUME,
                            violation = lastPauseViolation,
                            message = "안정 상태가 확인되어 재생을 다시 시작합니다",
                        )
                        playingSinceMillis = timestampMillis
                        stableSegmentStartedAtMillis = timestampMillis
                    }
                    }
                    else -> {
                        recoverySinceMillis = null
                        recoveryRequested = false
                        currentPhase = pausedPhaseFor(lastPauseViolation)
                    }
                }
            }
            SessionPhase.LOCKED_OUT -> {
                if (lockoutUntilMillis != null && timestampMillis >= lockoutUntilMillis!!) {
                    lockoutUntilMillis = null
                    recoverySinceMillis = null
                    recoveryRequested = false
                    currentPhase = pausedPhaseFor(lastPauseViolation)
                }
            }
            SessionPhase.REQUIRES_RECALIBRATION,
            SessionPhase.SESSION_DONE,
            SessionPhase.CALIBRATING,
            -> Unit
        }

        val resumeActionEnabled = when (currentPhase) {
            SessionPhase.PAUSED_MOVEMENT,
            SessionPhase.PAUSED_ATTENTION,
            -> stableNow && !recoveryRequested
            else -> false
        }
        return result(
            timestampMillis = timestampMillis,
            playbackSnapshot = playbackSnapshot,
            primaryActionEnabledOverride = resumeActionEnabled,
        ).copy(
            playerCommand = playerCommand,
            events = events,
        )
    }

    fun finish(timestampMillis: Long): SessionMachineMetrics {
        closePlayingWindow(timestampMillis)
        return SessionMachineMetrics(
            movementViolationCount = movementViolationCount,
            attentionViolationCount = attentionViolationCount,
            averageStableSegmentMillis = stableSegments.average().takeIf { !it.isNaN() }?.toLong() ?: 0L,
            actualPlaybackMillis = playingAccumulatedMillis,
        )
    }

    private fun registerPause(
        timestampMillis: Long,
        violation: FocusViolation,
    ): PendingSessionEvent {
        prunePauseWindow(timestampMillis)
        pauseTimestamps.addLast(timestampMillis)
        recoveryRequested = false
        recoverySinceMillis = null
        currentPhase = pausedPhaseFor(violation)

        if (pauseTimestamps.size >= thresholds.recalibrationPauseCount) {
            currentPhase = SessionPhase.REQUIRES_RECALIBRATION
            return PendingSessionEvent(
                type = SessionEventType.REQUIRES_RECALIBRATION,
                violation = violation,
                message = "반복된 이탈이 감지되어 재보정이 필요합니다",
            )
        }

        if (pauseTimestamps.size >= thresholds.lockoutPauseCount) {
            currentPhase = SessionPhase.LOCKED_OUT
            lockoutUntilMillis = timestampMillis + thresholds.lockoutDurationMs
            return PendingSessionEvent(
                type = SessionEventType.LOCKOUT,
                violation = violation,
                message = "짧은 시간 안에 이탈이 반복되어 잠시 대기합니다",
            )
        }

        return PendingSessionEvent(
            type = SessionEventType.PAUSE,
            violation = violation,
            message = if (violation == FocusViolation.MOVEMENT) {
                "움직임이 감지되었습니다"
            } else {
                "화면 응시가 끊겼습니다"
            },
        )
    }

    private fun prunePauseWindow(timestampMillis: Long) {
        while (pauseTimestamps.isNotEmpty() && timestampMillis - pauseTimestamps.first() > thresholds.pauseBurstWindowMs) {
            pauseTimestamps.removeFirst()
        }
    }

    private fun closePlayingWindow(timestampMillis: Long) {
        val playingStart = playingSinceMillis
        if (playingStart != null) {
            playingAccumulatedMillis += timestampMillis - playingStart
            playingSinceMillis = null
        }
        stableSegmentStartedAtMillis?.let { started ->
            stableSegments += timestampMillis - started
            stableSegmentStartedAtMillis = null
        }
    }

    private fun result(
        timestampMillis: Long,
        playbackSnapshot: PlaybackSnapshot = PlaybackSnapshot(),
        primaryActionEnabledOverride: Boolean? = null,
    ): SessionMachineResult {
        val recoveryProgress = recoverySinceMillis?.let { timestampMillis - it } ?: 0L
        val recoveryRemaining = (thresholds.recoverStableMs - recoveryProgress).coerceAtLeast(0L)
        val lockoutRemaining = lockoutUntilMillis?.let { (it - timestampMillis).coerceAtLeast(0L) } ?: 0L
        val primaryActionEnabled = primaryActionEnabledOverride ?: when (currentPhase) {
            SessionPhase.PAUSED_MOVEMENT,
            SessionPhase.PAUSED_ATTENTION,
            -> !recoveryRequested
            else -> false
        }
        val primaryActionLabel = if (primaryActionEnabled) {
            when (currentPhase) {
                SessionPhase.PAUSED_MOVEMENT,
                SessionPhase.PAUSED_ATTENTION,
                -> "터치해서 다시 시작"
                else -> null
            }
        } else {
            null
        }
        return SessionMachineResult(
            uiState = SessionUiState(
                phase = currentPhase,
                activeViolation = when (currentPhase) {
                    SessionPhase.PAUSED_MOVEMENT -> FocusViolation.MOVEMENT
                    SessionPhase.PAUSED_ATTENTION -> FocusViolation.ATTENTION
                    SessionPhase.READY_COUNTDOWN,
                    SessionPhase.LOCKED_OUT,
                    SessionPhase.RECOVERING,
                    SessionPhase.REQUIRES_RECALIBRATION,
                    -> lastPauseViolation
                    else -> FocusViolation.NONE
                },
                warningMessage = when (currentPhase) {
                    SessionPhase.WARNING -> "휴대폰을 고정해 주세요"
                    SessionPhase.PAUSED_MOVEMENT -> "움직임이 감지되었습니다. 화면을 다시 맞춘 뒤 터치해 주세요"
                    SessionPhase.PAUSED_ATTENTION -> "화면을 바라본 뒤 터치해 주세요"
                    SessionPhase.READY_COUNTDOWN -> "곧 영상이 시작됩니다"
                    SessionPhase.RECOVERING -> "정면을 보고 3초 동안 유지해 주세요"
                    SessionPhase.LOCKED_OUT -> "반복 이탈로 잠시 재생이 제한됩니다"
                    SessionPhase.REQUIRES_RECALIBRATION -> "재보정이 필요합니다"
                    else -> null
                },
                fatalErrorMessage = null,
                primaryActionLabel = primaryActionLabel,
                primaryActionEnabled = primaryActionEnabled,
                calibrationProgressMs = 0,
                countdownRemainingMs = 0,
                recoverProgressMs = recoveryProgress,
                recoverRemainingMs = recoveryRemaining,
                lockoutRemainingMs = lockoutRemaining,
                movementViolationCount = movementViolationCount,
                attentionViolationCount = attentionViolationCount,
                playback = playbackSnapshot,
                requiresRecalibration = currentPhase == SessionPhase.REQUIRES_RECALIBRATION,
            ),
        )
    }

    private fun pausedPhaseFor(violation: FocusViolation): SessionPhase =
        if (violation == FocusViolation.MOVEMENT) SessionPhase.PAUSED_MOVEMENT else SessionPhase.PAUSED_ATTENTION

    private fun accumulateStart(
        active: Boolean,
        existingStart: Long?,
        timestampMillis: Long,
    ): Long? = if (active) existingStart ?: timestampMillis else null

    private fun heldLongEnough(startMillis: Long?, requiredMillis: Long, nowMillis: Long): Boolean =
        startMillis != null && nowMillis - startMillis >= requiredMillis
}

/**
 * Pure metrics snapshot returned when a session ends.
 */
data class SessionMachineMetrics(
    val movementViolationCount: Int,
    val attentionViolationCount: Int,
    val averageStableSegmentMillis: Long,
    val actualPlaybackMillis: Long,
)

/**
 * Controller-side effect emitted by the state machine.
 */
enum class PlayerCommand {
    PLAY,
    PAUSE,
}

/**
 * Persistable event produced by a state transition.
 */
data class PendingSessionEvent(
    val type: SessionEventType,
    val violation: FocusViolation,
    val message: String,
)

/**
 * State machine output for a single update tick.
 */
data class SessionMachineResult(
    val uiState: SessionUiState,
    val playerCommand: PlayerCommand? = null,
    val events: List<PendingSessionEvent> = emptyList(),
)
