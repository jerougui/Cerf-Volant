package com.kilotracker.kinematics

import org.junit.Assert.*
import org.junit.Test

class KinematicsTest {

    @Test
    fun `speed computation returns correct value`() {
        val kin = Kinematics(historySize = 5, alpha = 0.3f)
        val t0 = 0.0
        val t1 = 0.1
        kin.addSample(t0, 0f, 0f, 0f)
        kin.addSample(t1, 3f, 0f, 4f) // displacement (3,0,4) => speed sqrt(9+16)/0.1 = 50 m/s
        // Should clamp to max 50? Our max is 50, so maybe 50 exactly
        val speed = kin.getSpeed()
        assertTrue(speed in 45f..50f) // within range because capped
    }

    @Test
    fun `velocity outlier is capped`() {
        val kin = Kinematics(historySize = 5, alpha = 0.3f)
        val t0 = 0.0
        val t1 = 0.05 // short dt yields high velocity
        kin.addSample(t0, 0f, 0f, 0f)
        kin.addSample(t1, 100f, 0f, 0f) // displacement 100m in 0.05s => 2000 m/s (massive)
        val speed = kin.getSpeed()
        assertTrue(speed <= 50f) // capped at maxSpeed
    }

    @Test
    fun `azimuth points in correct quadrant`() {
        val kin = Kinematics(historySize = 5, alpha = 0.0f) // no smoothing for test
        val t0 = 0.0
        val t1 = 1.0
        kin.addSample(t0, 0f, 0f, 10f) // facing forward, Z=10
        kin.addSample(t1, 5f, 0f, 15f)  // moved right 5m, forward 5m
        val azimuth = kin.getAzimuth()
        // Should be positive (right of forward) and small ~0.3 rad
        assertTrue(azimuth > 0.2 && azimuth < 0.5)
    }
}
