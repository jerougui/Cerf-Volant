package com.kilotracker.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.*

class SensorFusion(private val context: Context) : SensorEventListener {

    data class Orientation(val pitch: Float, val roll: Float, val azimuth: Float)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var latestOrientation = Orientation(0f, 0f, 0f)

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun getOrientation(): Orientation = latestOrientation

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, event.values.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Could use gyro for short-term integration if needed
                // For now, rely on accelerometer+magnetometer for absolute orientation
            }
        }

        if (gravity.isNotEmpty() && geomagnetic.isNotEmpty()) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            val success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                // azimuth (yaw), pitch, roll
                latestOrientation = Orientation(
                    pitch = orientation[1],  // -π/2 to π/2
                    roll = orientation[2],   // -π to π
                    azimuth = orientation[0] // 0 to 2π
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

// Pitch in radians from horizontal (phone pitched up → positive pitch)
// cos(pitch) used to correct foreshortening in depth formula
