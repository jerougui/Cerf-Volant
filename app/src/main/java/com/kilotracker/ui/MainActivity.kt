package com.kilotracker.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kilotracker.R
import com.kilotracker.audio.*
import com.kilotracker.calibration.CalibrationData
import com.kilotracker.calibration.CalibrationParams
import com.kilotracker.calibration.SensorFusion
import com.kilotracker.kinematics.*
import com.kilotracker.vision.*

class MainActivity : AppCompatActivity(),
    CameraManager.FrameListener,
    TrackingManager.TrackingListener,
    BluetoothRouter.Listener {

    private lateinit var cameraManager: CameraManager
    private lateinit var trackingManager: TrackingManager
    private lateinit var detector: KiteDetector
    private lateinit var tracker: KLTTracker
    private lateinit var frameQueue: FrameQueue
    private lateinit var sensorFusion: SensorFusion
    private lateinit var depthEstimator: DepthEstimator
    private lateinit var kinematics: Kinematics
    private lateinit var audioEngine: AudioEngine
    private lateinit var telemetryMapper: TelemetryMapper
    private lateinit var bluetoothRouter: BluetoothRouter

    private lateinit var surfaceView: SurfaceView
    private lateinit var tvStats: TextView
    private lateinit var btnStart: Button
    private lateinit var btnCalibrate: Button
    private lateinit var btnSettings: Button
    private lateinit var spinnerBluetooth: Spinner

    private var calibration: CalibrationParams? = null
    private var trackingActive = false
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var fps = 0

    // Paint for overlay
    private val boxPaint = Paint().apply { color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 4f }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 48f }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surfaceView)
        tvStats = findViewById(R.id.tvStats)
        btnStart = findViewById(R.id.btnStart)
        btnCalibrate = findViewById(R.id.btnCalibrate)
        btnSettings = findViewById(R.id.btnSettings)
        spinnerBluetooth = findViewById(R.id.spinnerBluetooth)

        // Load calibration
        calibration = CalibrationData.load(this)
        if (calibration == null) {
            // Go to calibration first
            startActivity(Intent(this, CalibrationActivity::class.java))
            finish()
            return
        }

        depthEstimator = DepthEstimator(calibration!!)
        sensorFusion = SensorFusion(this)
        sensorFusion.start()

        detector = KiteDetector(this)
        tracker = KLTTracker()
        frameQueue = FrameQueue()

        kinematics = Kinematics()
        audioEngine = AudioEngine()
        telemetryMapper = TelemetryMapper(maxDistance = 200f)
        bluetoothRouter = BluetoothRouter(this)

        trackingManager = TrackingManager(detector, tracker, frameQueue)
        trackingManager.setListener(this)

        // SurfaceView callback for drawing
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                drawOverlay(holder)
            }
        })

        // Buttons
        btnStart.setOnClickListener {
            if (trackingActive) {
                stopTracking()
            } else {
                startTracking()
            }
        }
        btnCalibrate.setOnClickListener {
            // Recalibrate from current view: show overlay, ask distance, update CalibrationData
            showRecalibrationDialog()
        }
        btnSettings.setOnClickListener {
            // Open settings activity (stub)
            Toast.makeText(this, "Settings not implemented", Toast.LENGTH_SHORT).show()
        }

        // Bluetooth spinner populated via adapter
        bluetoothRouter.start(object : BluetoothRouter.Listener {
            override fun onDeviceConnected(device: BluetoothDevice) {
                runOnUiThread { populateBluetoothSpinner() }
            }
            override fun onDeviceDisconnected() {
                runOnUiThread { populateBluetoothSpinner() }
            }
            override fun onLatencyUpdated(latencyMs: Int, codec: String?) {
                // Optionally display
            }
        })
        populateBluetoothSpinner()
        spinnerBluetooth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val device = parent.getItemAtPosition(pos) as? BluetoothDevice
                bluetoothRouter.selectDevice(device)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun startTracking() {
        cameraManager = CameraManager(this, this)
        cameraManager.start()
        audioEngine.start()
        trackingActive = true
        btnStart.text = "Arrêt"
        lastFpsTime = System.currentTimeMillis()
    }

    private fun stopTracking() {
        cameraManager.stop()
        audioEngine.stop()
        trackingActive = false
        btnStart.text = "Démarrer suivi"
    }

    override fun onFrame(yuvData: ByteArray, width: Int, height: Int, timestamp: Long) {
        frameQueue.offer(FrameQueue.Frame(yuvData, width, height, timestamp))
        // Process in a separate handler thread (not shown)
        processFrameAsync()
    }

    private fun processFrameAsync() {
        // Simple: process on this thread? Not ideal. For skeleton, dispatch to coroutine or thread pool.
        // For now, assume we process directly in callback via handler.
        // Implementation would pull from frameQueue and pass to trackingManager.processFrame
    }

    private fun processFrame(frame: FrameQueue.Frame) {
        trackingManager.processFrame(frame.yuvData, frame.width, frame.height)
    }

    override fun onTrack(result: TrackingManager.TrackResult) {
        val bbox = result.bbox
        // Convert to pixel coordinates for drawing; compute center offset and apparent height
        val imageWidth = 1080 // placeholder, should get from camera
        val imageHeight = 1920
        val pixelBox = RectF(
            bbox.left * imageWidth,
            bbox.top * imageHeight,
            bbox.right * imageWidth,
            bbox.bottom * imageHeight
        )
        val centerX = (bbox.left + bbox.right) / 2
        val centerOffsetPx = ((centerX - 0.5) * imageWidth).toInt()
        val apparentHeightPx = (bbox.bottom - bbox.top) * imageHeight

        val pitch = sensorFusion.getOrientation().pitch
        val (x, y, z) = depthEstimator.estimatePosition(
            apparentHeightPx = apparentHeightPx,
            centerOffsetPx = centerOffsetPx,
            imageWidthPx = imageWidth,
            currentPitch = pitch
        )
        kinematics.addSample(
            timeSec = System.currentTimeMillis() / 1000.0,
            x = x, y = y, z = z
        )
        val speed = kinematics.getSpeed()
        val azimuth = kinematics.getAzimuth()

        // Map to audio
        val audioParams = telemetryMapper.map(x, y, z, speed)
        audioEngine.setParameters(audioParams.frequency, audioParams.amplitude, audioParams.pan, audioParams.modulationRate)

        // Draw
        runOnUiThread {
            drawOverlay(surfaceView.holder, pixelBox)
            updateStats(x, y, z, speed, fps)
        }
    }

    override fun onTrackLost() {
        runOnUiThread { Toast.makeText(this, "Tracking lost", Toast.LENGTH_SHORT).show() }
    }

    private fun drawOverlay(holder: SurfaceHolder, box: RectF? = null) {
        val canvas = holder.lockCanvas() ?: return
        canvas.drawColor(Color.BLACK)

        // Draw bounding box
        box?.let {
            canvas.drawRect(it, boxPaint)
        }

        // Draw simple audio meters: left/right volume bars at bottom
        val vol = audioEngine.amplitude
        val pan = audioEngine.pan
        val meterWidth = 200f
        val meterHeight = 20f
        val cx = canvas.width / 2f
        val cy = canvas.height - 50f

        // Left channel bar (scaled by (1-pan))
        val leftVol = vol * (1.0f - pan) / 2f
        canvas.drawRect(cx - meterWidth, cy, cx - meterWidth + meterWidth * leftVol, cy + meterHeight,
            Paint().apply { color = Color.GREEN })
        // Right channel bar (scaled by (1+pan))
        val rightVol = vol * (1.0f + pan) / 2f
        canvas.drawRect(cx, cy, cx + meterWidth * rightVol, cy + meterHeight,
            Paint().apply { color = Color.GREEN })

        holder.unlockCanvasAndPost(canvas)
    }

    private fun updateStats(x: Float, y: Float, z: Float, speed: Float, fps: Int) {
        tvStats.text = String.format("Dist: %.1fm\nH: %.1fm\nSpd: %.1fm/s\nFPS: %d", z, y, speed, fps)
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = now
        }
    }

    private fun populateBluetoothSpinner() {
        val devices = bluetoothRouter.getPairedDevices()
        val names = devices.map { it.name ?: it.address }.toMutableList()
        names.add(0, "Phone speaker")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBluetooth.adapter = adapter
    }

    private fun showRecalibrationDialog() {
        // TODO: implement recalibration dialog similar to CalibrationActivity but quicker
        Toast.makeText(this, "Recalibration dialog to be implemented", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (trackingActive) stopTracking()
        sensorFusion.stop()
        bluetoothRouter.stop()
        audioEngine.release()
    }
}
