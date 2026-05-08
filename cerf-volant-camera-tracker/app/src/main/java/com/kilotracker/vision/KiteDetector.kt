package com.kilotracker.vision

import android.content.Context
import android.graphics.RectF
import org.tensorflow.lite.task.vision.detector.Detector
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class KiteDetector(private val context: Context) {

    interface DetectionListener {
        fun onDetection(box: RectF, confidence: Float)
        fun onNoDetection()
    }

    private var detector: Detector? = null
    private var listener: DetectionListener? = null

    init {
        try {
            // Load TFLite model from assets (kite_detect.tflite)
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(1)
                .setScoreThreshold(0.5f)
                .build()
            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "kite_detect.tflite",
                options
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: color-based detection or just null; will use fallback tracker
            detector = null
        }
    }

    fun setListener(l: DetectionListener?) {
        listener = l
    }

    /**
     * Run detection on RGB bitmap (converted externally).
     * Returns bounding box normalized [0,1] relative to image size.
     */
    fun detect(image: TensorImage): RectF? {
        return try {
            val results = detector?.detect(image)
            results?.firstOrNull()?.boundingBox?.let { box ->
                // Convert to RectF normalized coordinates
                listener?.onDetection(
                    RectF(box.left, box.top, box.right, box.bottom),
                    results.first().categories.first().score
                )
                RectF(box.left, box.top, box.right, box.bottom)
            } ?: run {
                listener?.onNoDetection()
                null
            }
        } catch (e: Exception) {
            listener?.onNoDetection()
            null
        }
    }
}
