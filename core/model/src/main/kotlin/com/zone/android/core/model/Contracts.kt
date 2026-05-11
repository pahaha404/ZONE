package com.zone.android.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for imported local videos.
 */
interface VideoRepository {
    fun observeVideos(): Flow<List<VideoItem>>
    suspend fun importVideo(contentUri: String): Result<VideoItem>
    suspend fun getVideo(videoId: Long): VideoItem?
}

/**
 * Repository for calibration profiles, sessions, events, and reports.
 */
interface SessionRepository {
    suspend fun createSession(
        videoId: Long,
        strictnessLevel: StrictnessLevel,
        startedAtEpochMs: Long,
    ): Long

    suspend fun updateSessionSummary(sessionId: Long, summary: SessionSummary)
    suspend fun appendEvent(event: SessionEvent)
    suspend fun saveCalibrationProfile(profile: CalibrationProfile): Long
    suspend fun getLatestCalibrationProfile(
        videoId: Long,
        strictnessLevel: StrictnessLevel,
    ): CalibrationProfile?

    suspend fun getSessionReport(sessionId: Long): SessionReport?
    suspend fun getSessionStanding(sessionId: Long): SessionStanding?
}

/**
 * Persistent app settings store.
 */
interface SettingsRepository {
    val strictnessLevel: Flow<StrictnessLevel>
    val onboardingCompleted: Flow<Boolean>
    val cameraPermissionAsked: Flow<Boolean>
    val lastVideoId: Flow<Long?>
    val sessionIntent: Flow<String>

    suspend fun setStrictnessLevel(level: StrictnessLevel)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setCameraPermissionAsked(requested: Boolean)
    suspend fun setLastVideoId(videoId: Long?)
    suspend fun setSessionIntent(intent: String)
}

/**
 * Sensor-backed source of posture and motion signals.
 */
interface SensorFocusEngine {
    fun signals(): Flow<MotionSignal>
    fun setBaseline(quaternion: Quaternion?)
    fun start()
    fun stop()
}

/**
 * Camera-backed source of face presence and head pose signals.
 */
interface FaceAttentionAnalyzer {
    fun signals(): Flow<AttentionSignal>
    fun start()
    fun stop()
}

/**
 * Controller that drives pause/resume behavior for an active session.
 */
interface SessionController {
    val uiState: StateFlow<SessionUiState>
    fun start()
    fun finish(reason: String = "completed")
}

/**
 * Abstraction over the concrete Media3 player integration.
 */
interface PlayerCoordinator {
    val playback: StateFlow<PlaybackSnapshot>
    fun prepare(contentUri: String)
    fun play()
    fun pause()
    fun stop()
    fun release()
}
