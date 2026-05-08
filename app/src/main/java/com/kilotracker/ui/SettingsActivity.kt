package com.kilotracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.kilotracker.R
import com.kilotracker.config.AppConfig

class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerWaveform: Spinner
    private lateinit var seekbarSensitivity: SeekBar
    private lateinit var switchPerformance: Switch
    private lateinit var switchBluetooth: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        spinnerWaveform = findViewById(R.id.spinnerWaveform)
        seekbarSensitivity = findViewById(R.id.seekbarSensitivity)
        switchPerformance = findViewById(R.id.switchPerformance)
        switchBluetooth = findViewById(R.id.switchBluetooth)

        // Populate waveform options
        val waveforms = arrayOf("Sine", "Triangle", "Square", "Sawtooth")
        spinnerWaveform.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, waveforms)

        // Load saved preferences (stub)
        AppConfig.performanceMode = switchPerformance.isChecked

        seekbarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update sensitivity scaling factor
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchPerformance.setOnCheckedChangeListener { _, isChecked ->
            AppConfig.performanceMode = isChecked
        }

        // Bluetooth toggle shows/hides spinner in MainActivity via shared prefs or singleton
    }
}
