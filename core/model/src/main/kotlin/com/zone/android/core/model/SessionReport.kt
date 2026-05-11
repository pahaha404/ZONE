package com.zone.android.core.model

/**
 * Report payload rendered after a session ends.
 */
data class SessionReport(
    val session: Session,
    val video: VideoItem,
    val events: List<SessionEvent>,
)

