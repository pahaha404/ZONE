package com.zone.android.core.model

/**
 * Thrown when a persisted session is expected but cannot be found.
 */
class SessionEntityNotFoundException(
    sessionId: Long,
) : IllegalStateException("Session $sessionId was not found.")
