package com.kilotracker.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import java.util.*

/**
 * Manages Bluetooth A2DP device discovery, selection, and routing.
 * Reports estimated latency based on codec.
 */
class BluetoothRouter(private val context: Context) {

    interface Listener {
        fun onDeviceConnected(device: BluetoothDevice)
        fun onDeviceDisconnected()
        fun onLatencyUpdated(latencyMs: Int, codec: String?)
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val handler = Handler(Looper.getMainLooper())

    private var listener: Listener? = null
    private val connectedDevices = mutableListOf<BluetoothDevice>()
    private var currentDevice: BluetoothDevice? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        connectedDevices.add(it)
                        listener?.onDeviceConnected(it)
                        updateLatencyEstimate(it)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        connectedDevices.remove(it)
                        if (it == currentDevice) {
                            currentDevice = null
                            listener?.onDeviceDisconnected()
                        }
                    }
                }
            }
        }
    }

    fun start(listener: Listener) {
        this.listener = listener
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
        // Refresh currently connected A2DP devices
        bluetoothAdapter?.getProfileProxy(context, { _, profile ->
            if (profile != null) {
                val devices = bluetoothAdapter.bondedDevices.filter { d ->
                    d.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                    d.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO
                }
                devices.forEach { listener.onDeviceConnected(it) }
            }
        }, BluetoothProfile.A2DP)
    }

    fun stop() {
        try { context.unregisterReceiver(receiver) } catch (e: IllegalArgumentException) {}
        listener = null
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.filter {
            it.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
            it.bluetoothClass?.deviceClass == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO
        }?.toList() ?: emptyList()
    }

    fun selectDevice(device: BluetoothDevice?) {
        if (device == null) {
            audioManager.isBluetoothA2dpOn = false
            currentDevice = null
            return
        }
        // Route through Bluetooth A2DP
        audioManager.isBluetoothA2dpOn = true
        // Some manufacturers require additional steps; this is standard
        currentDevice = device
        // Could also use Oboe device selection by Bluetooth MAC via AudioDeviceInfo
        updateLatencyEstimate(device)
    }

    private fun updateLatencyEstimate(device: BluetoothDevice) {
        // Heuristic: infer codec from device name or capabilities, else use generic SBC (~180ms)
        // More accurate: query Bluetooth A2DP codec via AudioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) etc., not exposed
        val codec = detectCodec(device)
        val latency = when (codec) {
            "aptX Low Latency" -> 40
            "aptX" -> 80
            "AAC" -> 100
            "SBC" -> 180
            else -> 150
        }
        listener?.onLatencyUpdated(latency, codec)
    }

    private fun detectCodec(device: BluetoothDevice): String {
        // Placeholder: in a real app, query AudioManager codec info via AudioDeviceInfo.getCodecPrecedence()
        // For demo: return SBC generic
        return "SBC"
    }
}
