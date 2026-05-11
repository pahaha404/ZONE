package com.zone.android.feature.sensors

import com.zone.android.core.model.MotionSignal

/**
 * Parses simple CSV motion logs for replay-based regression tests.
 *
 * Expected header:
 * timestampMillis,postureDeviationDegrees,motionEnergy
 */
object CsvMotionSignalReplayParser {
    fun parse(csv: String): List<MotionSignal> {
        return csv.lineSequence()
            .filter { it.isNotBlank() }
            .drop(1)
            .map { line ->
                val columns = line.split(',')
                MotionSignal(
                    timestampMillis = columns[0].trim().toLong(),
                    postureDeviationDegrees = columns[1].trim().toDouble(),
                    motionEnergy = columns[2].trim().toDouble(),
                    quaternion = null,
                    isStable = columns[2].trim().toDouble() < 0.55,
                )
            }
            .toList()
    }
}

