#include "audio_dsp_engine.h"
#include <algorithm>
#include <cmath>

namespace svc::dsp {

AudioPcm AudioDspEngine::resample(const AudioPcm& input, uint32_t target_sample_rate) {
    if (input.samples.empty() || input.sample_rate == target_sample_rate) {
        return input;
    }

    AudioPcm output;
    output.sample_rate = target_sample_rate;
    output.channels = input.channels;

    double ratio = static_cast<double>(input.sample_rate) / target_sample_rate;
    size_t new_length = static_cast<size_t>(std::ceil(input.samples.size() / ratio));
    output.samples.resize(new_length);

    for (size_t i = 0; i < new_length; ++i) {
        double src_idx = i * ratio;
        size_t idx0 = static_cast<size_t>(src_idx);
        size_t idx1 = std::min(idx0 + 1, input.samples.size() - 1);
        double frac = src_idx - idx0;

        output.samples[i] = static_cast<float>(
            input.samples[idx0] * (1.0 - frac) + input.samples[idx1] * frac
        );
    }

    return output;
}

AudioPcm AudioDspEngine::pitchShift(const AudioPcm& input, float semitones) {
    if (input.samples.empty() || std::abs(semitones) < 0.01f) {
        return input;
    }

    // Pitch ratio: 2^(semitones / 12.0)
    float pitch_factor = std::pow(2.0f, semitones / 12.0f);

    AudioPcm output;
    output.sample_rate = input.sample_rate;
    output.channels = input.channels;

    // Resample with pitch ratio, then scale time to preserve duration
    double step = pitch_factor;
    size_t out_len = input.samples.size();
    output.samples.resize(out_len);

    double src_pos = 0.0;
    for (size_t i = 0; i < out_len; ++i) {
        size_t idx0 = static_cast<size_t>(src_pos);
        if (idx0 >= input.samples.size() - 1) {
            output.samples[i] = 0.0f;
            continue;
        }
        size_t idx1 = idx0 + 1;
        float frac = static_cast<float>(src_pos - idx0);

        output.samples[i] = input.samples[idx0] * (1.0f - frac) + input.samples[idx1] * frac;
        src_pos += step;
        if (src_pos >= input.samples.size()) {
            src_pos = std::fmod(src_pos, static_cast<double>(input.samples.size()));
        }
    }

    return output;
}

void AudioDspEngine::applyCrossfade(float* chunk_out, const float* chunk_in, size_t chunk_size, size_t overlap_size) {
    if (!chunk_out || !chunk_in || overlap_size == 0) return;

    for (size_t i = 0; i < overlap_size && i < chunk_size; ++i) {
        float weight = static_cast<float>(i) / overlap_size;
        chunk_out[i] = chunk_out[i] * (1.0f - weight) + chunk_in[i] * weight;
    }
}

float AudioDspEngine::calculateRms(const float* samples, size_t count) {
    if (!samples || count == 0) return 0.0f;

    double sum = 0.0;
    for (size_t i = 0; i < count; ++i) {
        sum += samples[i] * samples[i];
    }
    return static_cast<float>(std::sqrt(sum / count));
}

} // namespace svc::dsp
