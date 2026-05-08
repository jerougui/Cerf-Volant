package com.kilotracker.vision

import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*

/**
 * Lucas-Kanade optical flow tracker using OpenCV.
 * Tracks feature points from a reference bounding box across frames.
 */
class KLTTracker {

    private var prevGray: Mat? = null
    private var pointsToTrack: MatOfPoint2f? = null
    private var trackedBBox: RectF? = null

    // Parameters
    private val maxCorners = 100
    private val qualityLevel = 0.01
    private val minDistance = 10.0
    private val blockSize = 3

    /**
     * Initialize tracker with first frame and bounding box (normalized 0-1 coordinates to image width/height).
     */
    fun init(firstFrameBGR: Mat, bbox: RectF, imageWidth: Int, imageHeight: Int) {
        val gray = Mat()
        Imgproc.cvtColor(firstFrameBGR, gray, Imgproc.COLOR_BGR2GRAY)
        prevGray = gray

        // Convert normalized bbox to pixel coordinates
        val left = (bbox.left * imageWidth).toInt()
        val top = (bbox.top * imageHeight).toInt()
        val right = (bbox.right * imageWidth).toInt()
        val bottom = (bbox.bottom * imageHeight).toInt()

        // Region of interest
        val roi = Rect(left, top, right - left, bottom - left)
        val roiGray = Mat(gray, roi)

        // Detect good features within the ROI
        val points = Mat()
        Imgproc.goodFeaturesToTrack(
            roiGray, points, maxCorners.toDouble(),
            qualityLevel, minDistance, Mat(), blockSize, false, 0.04
        )

        // Convert points to global coordinates
        if (!points.empty()) {
            val pointsList = points.toList()
            val offset = Point2f(left.toDouble(), top.toDouble())
            val globalPoints = pointsList.map { Point2f(it.x + offset.x, it.y + offset.y) }
            pointsToTrack = MatOfPoint2f(*globalPoints.toTypedArray())
        } else {
            pointsToTrack = null
        }

        trackedBBox = bbox
    }

    /**
     * Track from previous frame to current frame. Returns true if tracking successful and provides updated bbox.
     */
    fun track(prevFrameBGR: Mat, currFrameBGR: Mat, imageWidth: Int, imageHeight: Int): RectF? {
        val prevGray = this.prevGray ?: return null
        val currGray = Mat()
        Imgproc.cvtColor(currFrameBGR, currGray, Imgproc.COLOR_BGR2GRAY)

        val prevPts = pointsToTrack ?: return null

        val currPts = Mat()
        val status = Mat()
        val err = Mat()

        // Optical flow
        Video.calcOpticalFlowPyrLK(prevGray, currGray, prevPts, currPts, status, err)

        // Filter good points
        val goodPrev = ArrayList<Point>()
        val goodCurr = ArrayList<Point>()
        val prevList = prevPts.toList()
        val currList = currPts.toList()
        val statusArr = status.toIntArray()

        for (i in prevList.indices) {
            if (statusArr[i] == 1) {
                goodPrev.add(prevList[i])
                goodCurr.add(currList[i])
            }
        }

        if (goodCurr.size < 8) {
            // Tracking lost, need re-detect
            return null
        }

        // Estimate new bounding box as bounding box of moved points
        val xs = goodCurr.map { it.x }
        val ys = goodCurr.map { it.y }
        val minX = xs.minOrNull() ?: 0.0
        val maxX = xs.maxOrNull() ?: 0.0
        val minY = ys.minOrNull() ?: 0.0
        val maxY = ys.maxOrNull() ?: 0.0

        // Add small margin (10%)
        val marginX = (maxX - minX) * 0.1
        val marginY = (maxY - minY) * 0.1
        val newLeft = ((minX - marginX) / imageWidth).coerceIn(0.0, 1.0).toFloat()
        val newTop = ((minY - marginY) / imageHeight).coerceIn(0.0, 1.0).toFloat()
        val newRight = ((maxX + marginX) / imageWidth).coerceIn(0.0, 1.0).toFloat()
        val newBottom = ((maxY + marginY) / imageHeight).coerceIn(0.0, 1.0).toFloat()

        // Update for next frame
        pointsToTrack = MatOfPoint2f(*goodCurr.map { Point(it.x, it.y) }.toTypedArray())
        this.prevGray = currGray

        return RectF(newLeft, newTop, newRight, newBottom)
    }

    fun reset() {
        prevGray?.release()
        prevGray = null
        pointsToTrack = null
        trackedBBox = null
    }
}
