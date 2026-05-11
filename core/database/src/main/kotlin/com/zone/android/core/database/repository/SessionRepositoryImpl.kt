package com.zone.android.core.database.repository

import com.zone.android.core.database.dao.CalibrationProfileDao
import com.zone.android.core.database.dao.SessionDao
import com.zone.android.core.database.dao.SessionEventDao
import com.zone.android.core.database.dao.VideoItemDao
import com.zone.android.core.database.entity.SessionEntity
import com.zone.android.core.database.mapper.toEntity
import com.zone.android.core.database.mapper.toModel
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.SessionEntityNotFoundException
import com.zone.android.core.model.SessionEvent
import com.zone.android.core.model.SessionReport
import com.zone.android.core.model.SessionRepository
import com.zone.android.core.model.SessionStanding
import com.zone.android.core.model.SessionSummary
import com.zone.android.core.model.StrictnessLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room-backed implementation of [SessionRepository].
 */
class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val sessionEventDao: SessionEventDao,
    private val calibrationProfileDao: CalibrationProfileDao,
    private val videoItemDao: VideoItemDao,
) : SessionRepository {
    override suspend fun createSession(
        videoId: Long,
        strictnessLevel: StrictnessLevel,
        startedAtEpochMs: Long,
    ): Long = withContext(Dispatchers.IO) {
        sessionDao.insert(
            SessionEntity(
                videoId = videoId,
                strictnessLevel = strictnessLevel,
                startedAtEpochMs = startedAtEpochMs,
                endedAtEpochMs = null,
                totalDurationMs = 0,
                actualPlaybackMs = 0,
                movementViolationCount = 0,
                attentionViolationCount = 0,
                averageStableSegmentMs = 0,
                completionRate = 0f,
                focusScore = 0,
                endedReason = null,
            ),
        )
    }

    override suspend fun updateSessionSummary(sessionId: Long, summary: SessionSummary) {
        withContext(Dispatchers.IO) {
            val existing = sessionDao.getById(sessionId)
                ?: throw SessionEntityNotFoundException(sessionId)
            sessionDao.update(
                existing.copy(
                    endedAtEpochMs = summary.endedAtEpochMs,
                    totalDurationMs = summary.totalDurationMs,
                    actualPlaybackMs = summary.actualPlaybackMs,
                    movementViolationCount = summary.movementViolationCount,
                    attentionViolationCount = summary.attentionViolationCount,
                    averageStableSegmentMs = summary.averageStableSegmentMs,
                    completionRate = summary.completionRate,
                    focusScore = summary.focusScore,
                    endedReason = summary.endedReason,
                ),
            )
        }
    }

    override suspend fun appendEvent(event: SessionEvent) {
        withContext(Dispatchers.IO) {
            sessionEventDao.insert(event.toEntity())
        }
    }

    override suspend fun saveCalibrationProfile(profile: CalibrationProfile): Long =
        withContext(Dispatchers.IO) {
            calibrationProfileDao.insert(profile.toEntity())
        }

    override suspend fun getLatestCalibrationProfile(
        videoId: Long,
        strictnessLevel: StrictnessLevel,
    ): CalibrationProfile? = withContext(Dispatchers.IO) {
        calibrationProfileDao.getLatest(videoId, strictnessLevel)?.toModel()
    }

    override suspend fun getSessionReport(sessionId: Long): SessionReport? =
        withContext(Dispatchers.IO) {
            val session = sessionDao.getById(sessionId)?.toModel() ?: return@withContext null
            val video = videoItemDao.getById(session.videoId)?.toModel() ?: return@withContext null
            val events = sessionEventDao.listBySessionId(sessionId).map { it.toModel() }
            SessionReport(session = session, video = video, events = events)
        }

    override suspend fun getSessionStanding(sessionId: Long): SessionStanding? =
        withContext(Dispatchers.IO) {
            val rankedSessions = sessionDao.listFinishedByRanking()
            val rankIndex = rankedSessions.indexOfFirst { it.id == sessionId }
            if (rankIndex == -1) {
                null
            } else {
                SessionStanding(
                    rank = rankIndex + 1,
                    totalSessions = rankedSessions.size,
                )
            }
        }
}
