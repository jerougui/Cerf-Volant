package com.kilotracker.audio

/**
 * AudioEngine wraps Oboe-based native audio synthesis.
 * Parameters: frequency, amplitude, pan, modulation rate.
 */
class AudioEngine private constructor() {

    companion object {
        init {
            System.loadLibrary("audio_synth")
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeStart(ptr: Long)
    private external fun nativeStop(ptr: Long)
    private external fun nativeSetParams(ptr: Long, freq: Float, amp: Float, pan: Float, modRate: Float)

    fun setParameters(freq: Float, amp: Float, panVal: Float, mod: Float) {
        frequency = freq
        amplitude = amp
        pan = panVal
        modRate = mod
        if (nativePtr != 0L) {
            nativeSetParams(nativePtr, freq, amp, panVal, mod)
        }
    }
        nativeStart(nativePtr)
    }

    fun stop() {
        if (nativePtr != 0L) {
            nativeStop(nativePtr)
        }
    }

    fun setParameters(freq: Float, amp: Float, panVal: Float, mod: Float) {
        frequency = freq
        amplitude = amp
        pan = panVal
        modRate = mod
        // Push to native immediately for low-latency
        if (nativePtr != 0L) {
            nativeSetParams(nativePtr, freq, amp, panVal)
        }
    }

    fun release() {
        if (nativePtr != 0L) {
            nativeDelete(nativePtr)
            nativePtr = 0
        }
    }
}
