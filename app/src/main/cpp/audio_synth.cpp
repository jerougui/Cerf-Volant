#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>

#define LOG_TAG "AudioSynth"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

class SynthEngine : public oboe::AudioStreamCallback {
public:
    oboe::AudioStreamBuilder builder;
    oboe::AudioStream *stream = nullptr;

    float phase = 0.0f;
    float frequency = 220.0f;
    float amplitude = 0.1f;
    float targetAmplitude = 0.1f;
    float pan = 0.0f;
    float targetPan = 0.0f;
    float modRate = 0.0f;     // Hz of vibrato LFO
    float modDepth = 0.0f;    // ± frequency deviation in Hz

    oboe::AudioFormat format = oboe::AudioFormat::Float;
    const float envelopeRate = 0.02f;
    float modPhase = 0.0f;

    virtual oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override {
        // Envelope smoothing
        if (amplitude < targetAmplitude) { amplitude += envelopeRate; if (amplitude > targetAmplitude) amplitude = targetAmplitude; }
        else if (amplitude > targetAmplitude) { amplitude -= envelopeRate; if (amplitude < targetAmplitude) amplitude = targetAmplitude; }
        if (pan < targetPan) { pan += envelopeRate; if (pan > targetPan) pan = targetPan; }
        else if (pan > targetPan) { pan -= envelopeRate; if (pan < targetPan) pan = targetPan; }

        float sr = oboeStream->getSampleRate();
        float modInc = 2.0f * M_PI * modRate / sr;

        for (int i = 0; i < numFrames; ++i) {
            // Frequency modulation via sine LFO
            float mod = modRate > 0.0f ? sinf(modPhase) * modDepth : 0.0f;
            modPhase += modInc; if (modPhase > 2.0f * M_PI) modPhase -= 2.0f * M_PI;

            float freq = frequency + mod;
            float sample = sinf(phase) * amplitude;
            phase += 2.0f * M_PI * freq / sr;
            if (phase > 2.0f * M_PI) phase -= 2.0f * M_PI;

            float leftGain = 1.0f - pan;
            float rightGain = 1.0f + pan;
            floatData[2*i] = sample * leftGain * 0.5f;
            floatData[2*i+1] = sample * rightGain * 0.5f;
        }
        return oboe::DataCallbackResult::Continue;
    }
        if (pan < targetPan) {
            pan += envelopeRate;
            if (pan > targetPan) pan = targetPan;
        } else if (pan > targetPan) {
            pan -= envelopeRate;
            if (pan < targetPan) pan = targetPan;
        }

        for (int i = 0; i < numFrames; ++i) {
            float sample = sinf(phase) * amplitude;
            phase += 2.0f * M_PI * frequency / oboeStream->getSampleRate();
            if (phase > 2.0f * M_PI) phase -= 2.0f * M_PI;

            // Stereo pan: left = sample*(1-pan), right = sample*(1+pan)
            float leftGain = 1.0f - pan;
            float rightGain = 1.0f + pan;
            floatData[2*i] = sample * leftGain * 0.5f;
            floatData[2*i+1] = sample * rightGain * 0.5f;
        }
        return oboe::DataCallbackResult::Continue;
    }

    virtual void onErrorBeforeClose(oboe::AudioStream *oboeStream, oboe::Result error) override {
        LOGI("AudioStream error: %d", error);
    }

    virtual void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override {}
    virtual void onClose(oboe::AudioStream *oboeStream) override {}

    void start() {
        builder.setPerformanceMode(oboe::PerformanceMode::LowLatency)
               .setSharingMode(oboe::SharingMode::Exclusive)
               .setFormat(format)
               .setChannelCount(2)
               .setSampleRate(44100)
               .setCallback(this);
        oboe::Result result = builder.openStream(&stream);
        if (result != oboe::Result::OK) {
            LOGI("Failed to open stream: %d", result);
            return;
        }
        stream->requestStart();
    }

    void stop() {
        if (stream) {
            stream->stop();
            stream->close();
            stream = nullptr;
        }
    }

    void setParams(float freq, float amp, float p, float modRateHz) {
        frequency = freq;
        targetAmplitude = amp;
        targetPan = p;
        modRate = modRateHz;
        modDepth = (modRateHz > 0.0f) ? freq * 0.1f : 0.0f; // 10% of base frequency for vibrato
    }
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_kilotracker_audio_AudioEngine_nativeCreate(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(new SynthEngine());
}

extern "C" JNIEXPORT void JNICALL
Java_com_kilotracker_audio_AudioEngine_nativeStart(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *engine = reinterpret_cast<SynthEngine*>(ptr);
    if (engine) engine->start();
}

extern "C" JNIEXPORT void JNICALL
Java_com_kilotracker_audio_AudioEngine_nativeStop(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *engine = reinterpret_cast<SynthEngine*>(ptr);
    if (engine) engine->stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_kilotracker_audio_AudioEngine_nativeSetParams(JNIEnv *env, jobject thiz, jlong ptr,
                                                        jfloat freq, jfloat amp, jfloat pan, jfloat modRate) {
    auto *engine = reinterpret_cast<SynthEngine*>(ptr);
    if (engine) engine->setParams(freq, amp, pan, modRate);
}

extern "C" JNIEXPORT void JNICALL
Java_com_kilotracker_audio_AudioEngine_nativeDelete(JNIEnv *env, jobject thiz, jlong ptr) {
    auto *engine = reinterpret_cast<SynthEngine*>(ptr);
    delete engine;
}
