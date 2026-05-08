package com.kilotracker.vision

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.ImageReader
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.min

class CameraManager(
    private val context: Context,
    private val frameListener: FrameListener
) {
    interface FrameListener {
        fun onFrame(yuvData: ByteArray, width: Int, height: Int, timestamp: Long)
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private var isRunning = false

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startCaptureSession()
        }
        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }
        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    fun start() {
        if (isRunning) return
        startBackgroundThread()
        val cameraId = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
        } ?: throw IllegalStateException("No back camera found")

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                as StreamConfigurationMap
        val sizes = map.getOutputSizes(ImageFormat.YUV_420_888)
        val chosenSize = sizes.firstOrNull { it.width >= 1280 && it.height >= 720 } ?: sizes[0]

        imageReader = ImageReader.newInstance(
            chosenSize.width, chosenSize.height, ImageFormat.YUV_420_888, 2
        )
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val planes = image.planes
            val y = planes[0].buffer
            val u = planes[1].buffer
            val v = planes[2].buffer
            val ySize = y.remaining()
            val uSize = u.remaining()
            val vSize = v.remaining()
            val data = ByteArray(ySize + uSize + vSize)
            y.get(data, 0, ySize)
            u.get(data, ySize, uSize)
            v.get(data, ySize + uSize, vSize)
            frameListener.onFrame(data, image.width, image.height, image.timestamp)
            image.close()
        }, backgroundHandler)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Camera permission not granted")
        }

        cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        isRunning = true
    }

    private fun startCaptureSession() {
        val target = imageReader.surface
        val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            ?: return
        requestBuilder.addTarget(target)
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        cameraDevice?.createCaptureSession(
            listOf(target),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }, backgroundHandler
        )
    }

    fun stop() {
        isRunning = false
        captureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        backgroundThread.join()
    }
}
