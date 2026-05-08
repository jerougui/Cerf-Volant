package com.kilotracker.vision

import java.util.concurrent.atomic.AtomicReference

/**
 * Simple single-producer, single-consumer lock-free queue for passing frame data between threads.
 * Holds only the most recent frame; drops previous if consumer lags.
 */
class FrameQueue {
    private val frameRef = AtomicReference<Frame?>()

    fun offer(frame: Frame) {
        frameRef.set(frame)
    }

    fun poll(): Frame? {
        return frameRef.getAndSet(null)
    }

    data class Frame(
        val yuvData: ByteArray,
        val width: Int,
        val height: Int,
        val timestamp: Long
    )
}
