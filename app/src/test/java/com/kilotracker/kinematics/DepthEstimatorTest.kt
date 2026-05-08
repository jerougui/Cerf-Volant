package com.kilotracker.kinematics

import com.kilotracker.calibration.CalibrationParams
import org.junit.Assert.*
import org.junit.Test

class DepthEstimatorTest {

    @Test
    fun `distance computation matches formula`() {
        // Given: H=1.2m, f=1200px, h=60px, pitch=20° (0.349 rad), cos(20°)=0.9397
        val params = CalibrationParams(
            referenceDistance = 20f,
            apparentHeight = 60f,
            pitch = Math.toRadians(20.0).toFloat(),
            focalPx = 1200f,
            realKiteHeight = 1.2f
        )
        val estimator = DepthEstimator(params)
        val dist = estimator.computeDistance(60f, Math.toRadians(20.0).toFloat())
        // Expected: 1.2 * 1200 / (60 * 0.9397) ≈ 25.5 m
        assertEquals(25.5f, dist, 0.5f)
    }

    @Test
    fun `distance halves when apparent height halves`() {
        val params = CalibrationParams(20f, 60f, 0f, 1200f, 1.2f)
        val estimator = DepthEstimator(params)
        val d1 = estimator.computeDistance(60f, 0f)
        val d2 = estimator.computeDistance(30f, 0f)
        // Approximately double
        assertTrue(d2 > d1 * 1.9 && d2 < d1 * 2.1)
    }

    @Test
    fun `position coordinates reasonable`() {
        val params = CalibrationParams(50f, 100f, Math.toRadians(30.0).toFloat(), 1200f, 1.2f)
        val estimator = DepthEstimator(params)
        val (x, y, z) = estimator.estimatePosition(
            apparentHeightPx = 100f,
            centerOffsetPx = 120, // offset ~120px
            imageWidthPx = 1920,
            currentPitch = Math.toRadians(30.0).toFloat()
        )
        // At 30° pitch, distance approx (1.2*1200)/(100*cos30°) ≈ 16.6m
        assertTrue(z > 15f && z < 20f)
        // X should be Z * tan(offset/f) with offset=120, f=1200 → tan(0.1) ≈ 0.1 * Z = ~1.66m
        assertTrue(x > 1.0f && x < 3.0f)
        // Y ~ Z * sin(pitch) = ~8.3m
        assertTrue(y > 5f && y < 12f)
    }
}
