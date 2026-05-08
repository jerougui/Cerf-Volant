package com.kilotracker.calibration

import android.content.Context
import android.content.SharedPreferences

data class CalibrationParams(
    val referenceDistance: Float,    // D0 in meters
    val apparentHeight: Float,        // h0 in pixels
    val pitch: Float,                // θ0 in radians
    val focalPx: Float,              // f in pixels
    val realKiteHeight: Float        // H in meters
) {
    // Base scale factor: S = (H × f) / (h0 × cos θ)
    val scaleFactor: Float
        get() = (realKiteHeight * focalPx) / (apparentHeight * kotlin.math.cos(pitch))
}

object CalibrationData {
    private const val PREFS_NAME = "kite_calibration"
    private const val KEY_DISTANCE = "ref_distance"
    private const val KEY_HEIGHT = "apparent_height"
    private const val KEY_PITCH = "pitch"
    private const val KEY_FOCAL = "focal_px"
    private const val KEY_REAL_HEIGHT = "real_height"

    fun store(context: Context,
              referenceDistance: Float,
              apparentHeight: Float,
              pitch: Float,
              focalPx: Float,
              realKiteHeight: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_DISTANCE, referenceDistance)
            .putFloat(KEY_HEIGHT, apparentHeight)
            .putFloat(KEY_PITCH, pitch)
            .putFloat(KEY_FOCAL, focalPx)
            .putFloat(KEY_REAL_HEIGHT, realKiteHeight)
            .apply()
    }

    /**
     * Recalibrate mid-flight: update the apparent size and/or distance while keeping real height same.
     * Updates stored params so new scaleFactor = (H × f) / (h_new × cos pitch_current)
     */
    fun updateCurrentFrame(context: Context, newApparentHeight: Float, currentPitch: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val focal = prefs.getFloat(KEY_FOCAL, 1200f)
        val realH = prefs.getFloat(KEY_REAL_HEIGHT, 1.2f)
        // Keep referenceDistance for history? Not needed; scaleFactor derived from current params.
        // We update apparentHeight and pitch. Distance itself not saved; only needed to compute scale.
        prefs.edit()
            .putFloat(KEY_HEIGHT, newApparentHeight)
            .putFloat(KEY_PITCH, currentPitch)
            .apply()
    }

    fun load(context: Context): CalibrationParams? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_DISTANCE)) return null
        return CalibrationParams(
            referenceDistance = prefs.getFloat(KEY_DISTANCE, 20f),
            apparentHeight = prefs.getFloat(KEY_HEIGHT, 100f),
            pitch = prefs.getFloat(KEY_PITCH, 0f),
            focalPx = prefs.getFloat(KEY_FOCAL, 1200f),
            realKiteHeight = prefs.getFloat(KEY_REAL_HEIGHT, 1.2f)
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
