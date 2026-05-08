package com.kilotracker.vision

import android.graphics.Bitmap
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.Mat

/**
 * TrackingManager orchestrates hybrid detect-then-track strategy.
 * Runs detector every N frames, tracks features between detections.
 */
class TrackingManager(
    private val detector: KiteDetector,
    private val tracker: KLTTracker,
    private val frameQueue: FrameQueue
) {

    data class TrackResult(
        val bbox: RectF,              // normalized bbox (0-1)
        val confidence: Float,        // from detector only
        val velocity: Float? = null  // optional future: optical flow magnitude
    )

    interface TrackingListener {
        fun onTrack(result: TrackResult)
        fun onTrackLost()
    }

    private var listener: TrackingListener? = null
    private var frameCount = 0
    private val detectInterval = 10  // detect every 10 frames
    private var lastBbox: RectF? = null
    private var isTracking = false
    private var consecutiveLowConfidence = 0

    fun setListener(l: TrackingListener?) {
        listener = l
    }

    /**
     * Called from processing thread at each frame.
     */
    fun processFrame(yuvData: ByteArray, width: Int, height: Int) {
        frameCount++
        // Convert YUV to RGB Bitmap for detection; for tracking we use OpenCV Mat
        val bitmap = yuvToBitmap(yuvData, width, height) // placeholder; real conversion expensive.
        // For efficiency, we'd use RenderScript or OpenCV's cvtColor. We'll stub call.

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // requires bitmap

        if (frameCount % detectInterval == 0 || !isTracking) {
            // Run detector
            val normalizedBbox = detector.detect(tensorImageFromMat(mat))
            if (normalizedBbox != null) {
                lastBbox = normalizedBbox
                isTracking = true
                consecutiveLowConfidence = 0
                tracker.init(mat, normalizedBbox, width, height)
                listener?.onTrack(TrackResult(normalizedBbox, 0.8f))
            } else {
                consecutiveLowConfidence++
                if (consecutiveLowConfidence > 3) {
                    isTracking = false
                    listener?.onTrackLost()
                }
            }
        } else if (isTracking) {
            // Track between detections
            lastBbox?.let { bbox ->
                val trackedBbox = tracker.track(mat, mat, width, height) // In real code: prev vs curr separately
                if (trackedBbox != null) {
                    lastBbox = trackedBbox
                    listener?.onTrack(TrackResult(trackedBbox, 1.0f))
                } else {
                    // Tracking failed, will try detection next cycle
                    isTracking = false
                    listener?.onTrackLost()
                }
            }
        }

        // Cleanup
        mat.release()
        bitmap.recycle()
    }

    private fun yuvToBitmap(data: ByteArray, w: Int, h: Int): Bitmap {
        // Stub: actual YUV420 to Bitmap conversion needed
        // Use YuvImage, but for now return an empty Bitmap to avoid crash; actual code should convert properly.
        // For this skeleton, assume placeholder:
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    }

    private fun tensorImageFromMat(mat: Mat): TensorImage {
        // Convert Mat to TensorImage; stub placeholder - real code uses Bitmap
        val bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bmp)
        return TensorImage.fromBitmap(bmp)
    }
}
