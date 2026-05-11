package com.zone.android.core.model

/**
 * Health of the front-camera attention pipeline.
 */
enum class AttentionTrackingStatus {
    IDLE,
    RUNNING,
    NO_FRONT_CAMERA,
    CAMERA_BIND_FAILED,
    DETECTOR_FAILED,
}

/**
 * Snapshot of face presence and head pose derived from the front camera.
 */
data class AttentionSignal(
    val timestampMillis: Long,
    val faceDetected: Boolean,
    val centered: Boolean,
    val faceCenterX: Float,
    val faceCenterY: Float,
    val faceSizeRatio: Float,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val isAttentive: Boolean,
    val trackingStatus: AttentionTrackingStatus = AttentionTrackingStatus.IDLE,
)

/**
 * Returns true when the attention pipeline is in a terminal failure state.
 */
fun AttentionTrackingStatus.isTerminalFailure(): Boolean =
    this == AttentionTrackingStatus.NO_FRONT_CAMERA ||
        this == AttentionTrackingStatus.CAMERA_BIND_FAILED ||
        this == AttentionTrackingStatus.DETECTOR_FAILED

/**
 * Human-readable copy for the current attention pipeline state.
 */
fun AttentionTrackingStatus.userMessage(): String? = when (this) {
    AttentionTrackingStatus.IDLE,
    AttentionTrackingStatus.RUNNING,
    -> null
    AttentionTrackingStatus.NO_FRONT_CAMERA -> "전면 카메라가 있는 기기에서만 ZONE 세션을 진행할 수 있습니다."
    AttentionTrackingStatus.CAMERA_BIND_FAILED -> "전면 카메라를 시작하지 못했습니다. 다시 보정해 주세요."
    AttentionTrackingStatus.DETECTOR_FAILED -> "얼굴 감지가 중단되었습니다. 다시 보정해 주세요."
}
