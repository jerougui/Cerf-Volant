package com.kilotracker.vision

import android.graphics.RectF
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * Simple color-based fallback detector: threshold in HSV color space to find kite when ML detector fails.
 * Assumes kite is a solid, high-contrast color (default: red).
 */
object ColorFallbackDetector {

    // Default HSV range for red (two ranges due to hue wrap-around)
    private val lowerRed1 = Scalar(0.0, 100.0, 100.0)
    private val upperRed1 = Scalar(10.0, 255.0, 255.0)
    private val lowerRed2 = Scalar(170.0, 100.0, 100.0)
    private val upperRed2 = Scalar(180.0, 255.0, 255.0)

    /**
     * Detect largest colored blob in image. Returns bounding box in normalized coordinates or null.
     */
    fun detect(matBGR: Mat): RectF? {
        val hsv = Mat()
        Imgproc.cvtColor(matBGR, hsv, Imgproc.COLOR_BGR2HSV)

        // Threshold red range
        val mask1 = Mat()
        val mask2 = Mat()
        Core.inRange(hsv, lowerRed1, upperRed1, mask1)
        Core.inRange(hsv, lowerRed2, upperRed2, mask2)
        val mask = Mat()
        Core.add(mask1, mask2, mask)

        // Morphology to remove noise
        val morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, org.opencv.core.Size(5.0, 5.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, morphKernel)
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, morphKernel)

        // Find contours
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        if (contours.isEmpty()) {
            hsv.release(); mask.release(); mask1.release(); mask2.release(); morphKernel.release()
            return null
        }

        // Pick largest contour by area
        var maxArea = 0.0
        var largest: MatOfPoint? = null
        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area > maxArea) {
                maxArea = area
                largest = c
            }
        }

        largest?.let {
            val rect = Imgproc.boundingRect(it)
            val imgH = matBGR.height().toFloat()
            val imgW = matBGR.width().toFloat()
            val normalized = RectF(
                rect.x / imgW,
                rect.y / imgH,
                (rect.x + rect.width) / imgW,
                (rect.y + rect.height) / imgH
            )
            hsv.release(); mask.release(); mask1.release(); mask2.release(); morphKernel.release()
            return normalized
        }

        hsv.release(); mask.release(); mask1.release(); mask2.release(); morphKernel.release()
        return null
    }
}
