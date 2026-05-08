package com.kilotracker.config

object AppConfig {
    var performanceMode = false
        set(value) {
            field = value
            // Notify relevant components
        }

    // Target FPS for tracking
    val targetFps: Int
        get() = if (performanceMode) 15 else 30

    // Audio buffer size factor (Oboe will choose)
    val audioBufferSizeMultiplier: Int
        get() = if (performanceMode) 2 else 1  // smaller buffer in normal mode
}
