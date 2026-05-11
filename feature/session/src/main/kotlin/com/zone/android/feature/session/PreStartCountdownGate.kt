package com.zone.android.feature.session

/**
 * Pure manual countdown gate that only starts after an explicit arm request.
 */
class ManualCountdownGate(
    private val countdownDurationMs: Long,
) {
    private var armed = false
    private var readySinceMillis: Long? = null

    fun arm() {
        armed = true
        readySinceMillis = null
    }

    fun reset() {
        armed = false
        readySinceMillis = null
    }

    fun update(
        timestampMillis: Long,
        ready: Boolean,
    ): ManualCountdownSnapshot {
        if (!ready) {
            reset()
            return ManualCountdownSnapshot(
                actionEnabled = false,
                countdownRunning = false,
                countdownRemainingMs = countdownDurationMs,
                countdownCompleted = false,
            )
        }

        if (!armed) {
            readySinceMillis = null
            return ManualCountdownSnapshot(
                actionEnabled = true,
                countdownRunning = false,
                countdownRemainingMs = countdownDurationMs,
                countdownCompleted = false,
            )
        }

        val startedAt = readySinceMillis ?: timestampMillis.also { readySinceMillis = it }
        val elapsedMillis = (timestampMillis - startedAt).coerceAtLeast(0L)
        val remainingMillis = (countdownDurationMs - elapsedMillis).coerceAtLeast(0L)
        val countdownCompleted = remainingMillis == 0L
        if (countdownCompleted) {
            reset()
        }
        return ManualCountdownSnapshot(
            actionEnabled = false,
            countdownRunning = !countdownCompleted,
            countdownRemainingMs = remainingMillis,
            countdownCompleted = countdownCompleted,
        )
    }
}

/**
 * Snapshot rendered while the user is waiting to start or resume playback.
 */
data class ManualCountdownSnapshot(
    val actionEnabled: Boolean,
    val countdownRunning: Boolean,
    val countdownRemainingMs: Long,
    val countdownCompleted: Boolean,
)
