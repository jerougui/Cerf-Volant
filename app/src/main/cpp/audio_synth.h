#ifndef AUDIO_SYNTH_H
#define AUDIO_SYNTH_H

#include <oboe/Oboe.h>

class SynthEngine : public oboe::AudioStreamCallback {
public:
    SynthEngine();
    ~SynthEngine();

    void start();
    void stop();
    void setParams(float frequency, float amplitude, float pan);

    // oboe::AudioStreamCallback overrides
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) override;
    void onErrorBeforeClose(oboe::AudioStream *oboeStream, oboe::Result error) override;
    void onErrorAfterClose(oboe::AudioStream *oboeStream, oboe::Result error) override;
    void onClose(oboe::AudioStream *oboeStream) override;

private:
    oboe::AudioStream *stream = nullptr;
    oboe::AudioStreamBuilder builder;
    float phase = 0.0f;
    float frequency = 220.0f;
    float amplitude = 0.1f;
    float pan = 0.0f;
    oboe::AudioFormat format = oboe::AudioFormat::Float;
};

#endif //AUDIO_SYNTH_H
