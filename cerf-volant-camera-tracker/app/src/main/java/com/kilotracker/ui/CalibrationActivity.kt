package com.kilotracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kilotracker.R
import com.kilotracker.calibration.CalibrationData
import com.kilotracker.calibration.SensorFusion
import com.kilotracker.vision.CameraManager

class CalibrationActivity : AppCompatActivity(), CameraManager.FrameListener {

    private lateinit var textureView: TextureView
    private lateinit var distanceInput: EditText
    private lateinit var btnCalibrate: Button
    private lateinit var warningText: TextView
    private lateinit var overlayBox: View

    private lateinit var cameraManager: CameraManager
    private lateinit var sensorFusion: SensorFusion

    private var overlayX = 0
    private var overlayY = 0
    private var overlayWidth = 0
    private var overlayHeight = 0

    companion object {
        private const val TAG = "Calibration"
        private const val REQUEST_CAMERA = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        textureView = findViewById(R.id.textureView)
        distanceInput = findViewById(R.id.distanceInput)
        btnCalibrate = findViewById(R.id.btnCalibrate)
        warningText = findViewById(R.id.warningText)
        overlayBox = findViewById(R.id.overlayBox)

        sensorFusion = SensorFusion(this)
        sensorFusion.start()

        cameraManager = CameraManager(this, this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            startCamera()
        }

        // Position overlay at center
        textureView.post {
            overlayWidth = textureView.width / 3
            overlayHeight = textureView.height / 3
            overlayX = (textureView.width - overlayWidth) / 2
            overlayY = (textureView.height - overlayHeight) / 2
            overlayBox.layout(overlayX, overlayY, overlayX + overlayWidth, overlayY + overlayHeight)
            overlayBox.visibility = View.VISIBLE
        }

        btnCalibrate.setOnClickListener {
            performCalibration()
        }
    }

    private fun startCamera() {
        cameraManager.start()
    }

    private fun performCalibration() {
        val distStr = distanceInput.text.toString()
        val distance = distStr.toFloatOrNull()
        if (distance == null || distance < 5f) {
            warningText.text = getString(R.string.warning_too_close)
            warningText.visibility = View.VISIBLE
            return
        }

        // Validate apparent height >= 1% of frame height
        val frameHeight = textureView.height
        if (overlayHeight < frameHeight * 0.01f) {
            warningText.text = "Le cerf-volant doit occuper au moins 1% de l'image"
            warningText.visibility = View.VISIBLE
            return
        }

        // Validate roughly centered
        val centerX = textureView.width / 2
        val boxCenterX = overlayX + overlayWidth / 2
        if (kotlin.math.abs(boxCenterX - centerX) > textureView.width * 0.3f) {
            warningText.text = "Le cerf-volant doit être centré horizontalement"
            warningText.visibility = View.VISIBLE
            return
        }

        // Capture calibration parameters
        val pitch = sensorFusion.getOrientation().pitch  // radians
        val focalPx = estimateFocalLength()  // placeholder: should read from camera characteristics
        val realHeight = 1.2f  // default kite height in meters (user should ideally set this)
        val apparentHeight = overlayHeight.toFloat()

        CalibrationData.store(
            context = this,
            referenceDistance = distance,
            apparentHeight = apparentHeight,
            pitch = pitch,
            focalPx = focalPx,
            realKiteHeight = realHeight
        )

        Toast.makeText(this, "Calibration enregistrée", Toast.LENGTH_SHORT).show()
        // Return to main activity (would start tracking)
        val intent = Intent(this, com.kilotracker.ui.MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun estimateFocalLength(): Float {
        // For now assume 1080p camera with focal length ~1200px
        // In full implementation, read from CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
        return 1200.0f
    }

    override fun onFrame(yuvData: ByteArray, width: Int, height: Int, timestamp: Long) {
        // Frame data not used directly during calibration UI; overlay shown
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stop()
        sensorFusion.stop()
    }
}
