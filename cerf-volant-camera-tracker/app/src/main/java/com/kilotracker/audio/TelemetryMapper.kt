package com.kilotracker.audio

/**
 * Maps kite position and velocity to audio synthesis parameters.
 * - X (horizontal) → stereo pan [-1..1]
 * - Z (distance)   → amplitude [0..1] (closer = louder)
 * - speed          → modulation rate / timbre brightness
 */
class TelemetryMapper(
    private val maxDistance: Float = 100f,
    private val maxSpeed: Float = 30f
) {
    /**
     * Convert 3D position (X_right, Y_up, Z_forward) and speed to AudioParams.
     */
    fun map(x: Float, y: Float, z: Float, speed: Float): AudioParams {
        // Pan: X normalized; assume X range -maxDistance..maxDistance maps to -1..1
        val pan = (x / maxDistance).coerceIn(-1f, 1f)

        // Volume: inverse relationship with distance. Use simple linear falloff: 1 at 0m, 0 at maxDistance
        val volume = (1.0f - (z / maxDistance).coerceIn(0f, 1f)).coerceAtLeast(0.1f)

        // Modulation rate: proportional to speed, capped
        val modRate = (speed / maxSpeed) * 10.0f // up to 10 Hz

        // Frequency base: could vary slightly with altitude? Not required.
        val freq = 220.0f // fixed A3

        return AudioParams(freq, volume, pan, modRate)
    }
}

data class AudioParams(
    val frequency: Float,
    val amplitude: Float,
    val pan: Float,
    val modulationRate: Float
)
