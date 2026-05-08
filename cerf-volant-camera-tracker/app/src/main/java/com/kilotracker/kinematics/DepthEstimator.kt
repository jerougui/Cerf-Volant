package com.kilotracker.kinematics

import com.kilotracker.calibration.CalibrationParams
import kotlin.math.*

/**
 * Estimates 3D position from apparent size, focal length, and pitch.
 * Outputs phone-relative coordinates: X right, Y up, Z forward.
 */
class DepthEstimator(private val calibration: CalibrationParams) {

    /**
     * Compute 3D position from bounding box height and horizontal offset.
     * @param apparentHeightPx current kite bounding box height in pixels
     * @param centerOffsetPx horizontal offset from image center (positive = right) in pixels
     * @param imageWidthPx width of camera image
     * @param currentPitch current device pitch from IMU (radians)
     * @return Triple(X_right_m, Y_up_m, Z_forward_m)
     */
    fun estimatePosition(
        apparentHeightPx: Float,
        centerOffsetPx: Int,
        imageWidthPx: Int,
        currentPitch: Float
    ): Triple<Float, Float, Float> {
        // Distance formula: Z = (H_real * f_px) / (h_apparent * cos(pitch_relative))
        // pitch_relative = currentPitch - calibration.pitch
        val pitchRel = currentPitch - calibration.pitch
        val cosPitch = cos(pitchRel).coerceAtLeast(0.1f) // avoid div by zero
        val distanceZ = (calibration.realKiteHeight * calibration.focalPx) /
                (apparentHeightPx * cosPitch)

        // Horizontal angle φ = arctan2(centerOffset * (sensor_width_px / focal_px)? simpler: atan2(offset, focal_px)
        val phi = atan2(centerOffsetPx.toFloat(), calibration.focalPx)

        // X = Z * tan(φ) ≈ Z * (offset / f) for small angles
        val X = distanceZ * tan(phi)

        // Y from distance and pitch: if user holds phone at hand height, Y = distanceZ * sin(pitchRel) approx
        val Y = distanceZ * sin(pitchRel)

        return Triple(X, Y, distanceZ)
    }

    /**
     * Recalibrate mid-flight: update apparent height and current pitch.
     */
    fun updateCalibration(newApparentHeight: Float, currentPitch: Float) {
        // CalibrationData.updateCurrentFrame handles persistence; here we update in-memory
        // Not used directly; caller should reload CalibrationParams after update or pass new values
    }

    /**
     * Test helper: compute pure distance given height (no X/Y)
     */
    fun computeDistance(apparentHeightPx: Float, currentPitch: Float): Float {
        val pitchRel = currentPitch - calibration.pitch
        val cosPitch = cos(pitchRel).coerceAtLeast(0.1f)
        return (calibration.realKiteHeight * calibration.focalPx) /
                (apparentHeightPx * cosPitch)
    }
}
