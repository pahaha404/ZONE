package com.zone.android.feature.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.zone.android.core.common.MovingRmsWindow
import com.zone.android.core.common.QuaternionMath
import com.zone.android.core.model.MotionSignal
import com.zone.android.core.model.Quaternion
import com.zone.android.core.model.SensorFocusEngine
import com.zone.android.core.model.SessionRuntimeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Android [SensorManager]-based implementation of [SensorFocusEngine].
 */
class AndroidSensorFocusEngine(
    context: Context,
    private val runtimeConfig: SessionRuntimeConfig,
) : SensorFocusEngine, SensorEventListener {
    companion object {
        private const val SENSOR_EMIT_INTERVAL_MS = 75L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val motionWindow = MovingRmsWindow(windowMs = 1_000)
    private val gyroWindow = MovingRmsWindow(windowMs = 1_000)
    private val _signals = MutableStateFlow(
        MotionSignal(
            timestampMillis = SystemClock.elapsedRealtime(),
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.0,
            quaternion = null,
            isStable = false,
        ),
    )

    private var baselineQuaternion: Quaternion? = runtimeConfig.calibrationProfile?.baselineQuaternion
    private var latestQuaternion: Quaternion? = null
    private var latestMotionEnergy = 0.0
    private var latestGyroRms = 0.0
    private var gravity = FloatArray(3)
    private var usingAccelerometerFallback = false
    private var started = false
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private var lastEmissionMillis = 0L

    override fun signals(): Flow<MotionSignal> = _signals.asStateFlow()

    override fun setBaseline(quaternion: Quaternion?) {
        baselineQuaternion = quaternion
        emitSignal(SystemClock.elapsedRealtime(), force = true)
    }

    override fun start() {
        if (started) {
            return
        }
        started = true
        resetTransientState()
        sensorThread = HandlerThread("zone-sensors").also { it.start() }
        sensorHandler = Handler(sensorThread!!.looper)
        register(Sensor.TYPE_ROTATION_VECTOR)
        register(Sensor.TYPE_GYROSCOPE)
        val linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        usingAccelerometerFallback = linearAccelerationSensor == null
        register(
            if (usingAccelerometerFallback) Sensor.TYPE_ACCELEROMETER else Sensor.TYPE_LINEAR_ACCELERATION,
        )
    }

    override fun stop() {
        if (!started) {
            return
        }
        started = false
        sensorManager.unregisterListener(this)
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val timestampMillis = SystemClock.elapsedRealtime()
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                latestQuaternion = event.values.toQuaternion()
            }
            Sensor.TYPE_GYROSCOPE -> {
                latestGyroRms = gyroWindow.add(timestampMillis, event.values.magnitude())
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                latestMotionEnergy = motionWindow.add(timestampMillis, event.values.magnitude())
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val filtered = removeGravity(event.values)
                latestMotionEnergy = motionWindow.add(timestampMillis, filtered.magnitude())
            }
        }
        emitSignal(timestampMillis)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun register(sensorType: Int) {
        sensorManager.getDefaultSensor(sensorType)?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI,
                sensorHandler,
            )
        }
    }

    private fun emitSignal(timestampMillis: Long, force: Boolean = false) {
        if (!force && timestampMillis - lastEmissionMillis < SENSOR_EMIT_INTERVAL_MS) {
            return
        }
        lastEmissionMillis = timestampMillis
        val postureDeviation = if (baselineQuaternion != null && latestQuaternion != null) {
            QuaternionMath.angularDistanceDegrees(baselineQuaternion!!, latestQuaternion!!)
        } else {
            0.0
        }
        val motionEnergy = latestMotionEnergy + latestGyroRms
        _signals.value = MotionSignal(
            timestampMillis = timestampMillis,
            postureDeviationDegrees = postureDeviation,
            motionEnergy = motionEnergy,
            quaternion = latestQuaternion,
            isStable = MotionSignalRules.isStable(
                postureDeviationDegrees = postureDeviation,
                motionEnergy = motionEnergy,
                hasQuaternion = latestQuaternion != null,
                runtimeConfig = runtimeConfig,
            ),
        )
    }

    private fun resetTransientState() {
        motionWindow.clear()
        gyroWindow.clear()
        latestQuaternion = null
        latestMotionEnergy = 0.0
        latestGyroRms = 0.0
        gravity = FloatArray(3)
        lastEmissionMillis = 0L
        _signals.value = MotionSignal(
            timestampMillis = SystemClock.elapsedRealtime(),
            postureDeviationDegrees = 0.0,
            motionEnergy = 0.0,
            quaternion = null,
            isStable = false,
        )
    }

    private fun removeGravity(values: FloatArray): FloatArray {
        val alpha = 0.8f
        gravity = FloatArray(3) { index ->
            alpha * gravity[index] + (1 - alpha) * values[index]
        }
        return FloatArray(3) { index -> values[index] - gravity[index] }
    }

    private fun FloatArray.toQuaternion(): Quaternion {
        val quaternionValues = FloatArray(4)
        SensorManager.getQuaternionFromVector(quaternionValues, this)
        return Quaternion(
            w = quaternionValues[0].toDouble(),
            x = quaternionValues[1].toDouble(),
            y = quaternionValues[2].toDouble(),
            z = quaternionValues[3].toDouble(),
        )
    }

    private fun FloatArray.magnitude(): Double =
        sqrt(sumOf { axis -> (axis * axis).toDouble() })
}
