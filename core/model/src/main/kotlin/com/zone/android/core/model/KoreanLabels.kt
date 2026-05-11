package com.zone.android.core.model

/**
 * Korean display label for a strictness preset.
 */
fun StrictnessLevel.displayName(): String = when (this) {
    StrictnessLevel.LIGHT -> "라이트"
    StrictnessLevel.STANDARD -> "스탠다드"
    StrictnessLevel.HARD -> "하드"
}

/**
 * Korean display label for a session phase.
 */
fun SessionPhase.displayName(): String = when (this) {
    SessionPhase.CALIBRATING -> "보정 중"
    SessionPhase.READY -> "준비됨"
    SessionPhase.READY_COUNTDOWN -> "시작 카운트다운"
    SessionPhase.PLAYING -> "재생 중"
    SessionPhase.WARNING -> "경고"
    SessionPhase.PAUSED_MOVEMENT -> "움직임 감지"
    SessionPhase.PAUSED_ATTENTION -> "집중 이탈"
    SessionPhase.RECOVERING -> "복귀 확인 중"
    SessionPhase.LOCKED_OUT -> "일시 잠금"
    SessionPhase.REQUIRES_RECALIBRATION -> "재보정 필요"
    SessionPhase.SESSION_DONE -> "완료"
}

/**
 * Korean display label for the camera attention pipeline.
 */
fun AttentionTrackingStatus.displayName(): String = when (this) {
    AttentionTrackingStatus.IDLE -> "대기 중"
    AttentionTrackingStatus.RUNNING -> "실행 중"
    AttentionTrackingStatus.NO_FRONT_CAMERA -> "전면 카메라 없음"
    AttentionTrackingStatus.CAMERA_BIND_FAILED -> "카메라 시작 실패"
    AttentionTrackingStatus.DETECTOR_FAILED -> "얼굴 감지 실패"
}

/**
 * Korean display label for a persisted session event.
 */
fun SessionEventType.displayName(): String = when (this) {
    SessionEventType.STARTED -> "시작"
    SessionEventType.WARNING -> "경고"
    SessionEventType.PAUSE -> "일시정지"
    SessionEventType.RESUME -> "재개"
    SessionEventType.LOCKOUT -> "잠금"
    SessionEventType.REQUIRES_RECALIBRATION -> "재보정 필요"
    SessionEventType.TERMINATED -> "강제 종료"
    SessionEventType.COMPLETED -> "완료"
}

/**
 * Korean display label for a violation category.
 */
fun FocusViolation.displayName(): String = when (this) {
    FocusViolation.NONE -> "정상"
    FocusViolation.MOVEMENT -> "움직임"
    FocusViolation.ATTENTION -> "집중 이탈"
}

/**
 * Korean display label for an internal session ending reason.
 */
fun sessionEndReasonLabel(reason: String): String = when (reason) {
    "completed" -> "영상 시청 완료"
    "recalibration_requested" -> "재보정 요청"
    "user_left_session" -> "세션 이탈"
    "movement_limit_reached" -> "움직임 누적 종료"
    "attention_limit_reached" -> "집중 이탈 누적 종료"
    else -> reason
}
