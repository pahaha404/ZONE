package com.zone.android.core.database.mapper

import com.zone.android.core.database.entity.CalibrationProfileEntity
import com.zone.android.core.database.entity.SessionEntity
import com.zone.android.core.database.entity.SessionEventEntity
import com.zone.android.core.database.entity.VideoItemEntity
import com.zone.android.core.model.CalibrationProfile
import com.zone.android.core.model.Quaternion
import com.zone.android.core.model.Session
import com.zone.android.core.model.SessionEvent
import com.zone.android.core.model.VideoItem

internal fun VideoItemEntity.toModel(): VideoItem = VideoItem(
    id = id,
    contentUri = contentUri,
    title = title,
    durationMs = durationMs,
    thumbnailBytes = thumbnailBytes,
    addedAtEpochMs = addedAtEpochMs,
)

internal fun VideoItem.toEntity(): VideoItemEntity = VideoItemEntity(
    id = id,
    contentUri = contentUri,
    title = title,
    durationMs = durationMs,
    thumbnailBytes = thumbnailBytes,
    addedAtEpochMs = addedAtEpochMs,
)

internal fun SessionEntity.toModel(): Session = Session(
    id = id,
    videoId = videoId,
    strictnessLevel = strictnessLevel,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    totalDurationMs = totalDurationMs,
    actualPlaybackMs = actualPlaybackMs,
    movementViolationCount = movementViolationCount,
    attentionViolationCount = attentionViolationCount,
    averageStableSegmentMs = averageStableSegmentMs,
    completionRate = completionRate,
    focusScore = focusScore,
    endedReason = endedReason,
)

internal fun SessionEventEntity.toModel(): SessionEvent = SessionEvent(
    id = id,
    sessionId = sessionId,
    eventType = eventType,
    violation = violation,
    timestampMillis = timestampMillis,
    message = message,
)

internal fun SessionEvent.toEntity(): SessionEventEntity = SessionEventEntity(
    id = id,
    sessionId = sessionId,
    eventType = eventType,
    violation = violation,
    timestampMillis = timestampMillis,
    message = message,
)

internal fun CalibrationProfileEntity.toModel(): CalibrationProfile = CalibrationProfile(
    id = id,
    videoId = videoId,
    strictnessLevel = strictnessLevel,
    baselineQuaternion = Quaternion(
        w = baselineQuaternionW,
        x = baselineQuaternionX,
        y = baselineQuaternionY,
        z = baselineQuaternionZ,
    ),
    baselineFaceCenterX = baselineFaceCenterX,
    baselineFaceCenterY = baselineFaceCenterY,
    baselineFaceSizeRatio = baselineFaceSizeRatio,
    baselineYawDegrees = baselineYawDegrees,
    baselinePitchDegrees = baselinePitchDegrees,
    baselineRollDegrees = baselineRollDegrees,
    capturedAtEpochMs = capturedAtEpochMs,
)

internal fun CalibrationProfile.toEntity(): CalibrationProfileEntity = CalibrationProfileEntity(
    id = id,
    videoId = videoId,
    strictnessLevel = strictnessLevel,
    baselineQuaternionW = baselineQuaternion.w,
    baselineQuaternionX = baselineQuaternion.x,
    baselineQuaternionY = baselineQuaternion.y,
    baselineQuaternionZ = baselineQuaternion.z,
    baselineFaceCenterX = baselineFaceCenterX,
    baselineFaceCenterY = baselineFaceCenterY,
    baselineFaceSizeRatio = baselineFaceSizeRatio,
    baselineYawDegrees = baselineYawDegrees,
    baselinePitchDegrees = baselinePitchDegrees,
    baselineRollDegrees = baselineRollDegrees,
    capturedAtEpochMs = capturedAtEpochMs,
)

