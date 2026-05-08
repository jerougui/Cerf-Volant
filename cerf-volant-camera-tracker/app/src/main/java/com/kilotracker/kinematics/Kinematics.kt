package com.kilotracker.kinematics

import java.util.*
import kotlin.math.sqrt

/**
 * Tracks position history and computes velocity, speed, and azimuth with smoothing.
 * Thread-safe via synchronized methods.
 */
class Kinematics(
    private val historySize: Int = 10,
    private val alpha: Float = 0.3f
) {
    private val history = ArrayDeque<Sample>(historySize)

    // Smoothed values (EMA)
    @Volatile private var smoothedSpeed = 0f
    @Volatile private var smoothedAzimuth = 0f
    @Volatile private var lastPosition = FloatArray(3) // X,Y,Z
    @Volatile private var lastVelocity = FloatArray(3)

    data class Sample(val timeSec: Double, val pos: FloatArray)

    /**
     * Add new position sample (in meters, phone-relative coordinates)
     */
    fun addSample(timeSec: Double, x: Float, y: Float, z: Float) {
        synchronized(history) {
            history.addLast(Sample(timeSec, floatArrayOf(x, y, z)))
            if (history.size > historySize) history.removeFirst()
        }
        computeDerivatives()
    }

    private fun computeDerivatives() {
        val samples: List<Sample> = synchronized(history) { history.toList() }
        if (samples.size < 2) return

        val now = samples.last()
        val prev = samples[samples.size - 2]
        val dt = (now.timeSec - prev.timeSec).toFloat().coerceIn(0.033f, 0.5f)

        val dx = now.pos[0] - prev.pos[0]
        val dy = now.pos[1] - prev.pos[1]
        val dz = now.pos[2] - prev.pos[2]

        // Outlier rejection: cap velocity magnitude
        val rawVx = dx / dt
        val rawVy = dy / dt
        val rawVz = dz / dt
        val speedSq = rawVx*rawVx + rawVy*rawVy + rawVz*rawVz
        val maxSpeed = 50f // m/s, exceeds typical kite speeds

        val (vx, vy, vz) = if (sqrt(speedSq) > maxSpeed) {
            // Scale down to max
            val scale = maxSpeed / sqrt(speedSq)
            Triple(rawVx * scale, rawVy * scale, rawVz * scale)
        } else {
            Triple(rawVx, rawVy, rawVz)
        }

        // EMA smoothing
        smoothedSpeed = alpha * sqrt(vx*vx + vy*vy + vz*vz) + (1 - alpha) * smoothedSpeed
        val rawAzimuth = atan2(vx, vz) // azimuth in horizontal plane right/forward
        smoothedAzimuth = alpha * rawAzimuth + (1 - alpha) * smoothedAzimuth

        // Update last values
        lastPosition = now.pos.clone()
        lastVelocity = floatArrayOf(vx, vy, vz)
    }

    fun getPosition(): FloatArray = lastPosition.clone()
    fun getVelocity(): FloatArray = lastVelocity.clone()
    fun getSpeed(): Float = smoothedSpeed
    fun getAzimuth(): Float = smoothedAzimuth
}
