#pragma once

#include <vector>
#include <cstdint>
#include <cstddef>
#include <cmath>

namespace svc::dsp {

struct AudioPcm {
    std::vector<float> samples; // Normalized [-1.0, 1.0]
    uint32_t sample_rate{44100};
    uint16_t channels{1};
};

class AudioDspEngine {
public:
    // Simple resampler (Linear interpolation for fast low-overhead mobile DSP)
    static AudioPcm resample(const AudioPcm& input, uint32_t target_sample_rate);

    // Semitone pitch shifting (Time-domain Pitch Synchronous Overlap-Add / WSOLA approximation)
    // semitones: e.g. -12 to +12
    static AudioPcm pitchShift(const AudioPcm& input, float semitones);

    // Apply chunking with cross-fade overlap to avoid audio clicks
    static void applyCrossfade(float* chunk_out, const float* chunk_in, size_t chunk_size, size_t overlap_size);

    // Fast root-mean-square calculation for audio visualization
    static float calculateRms(const float* samples, size_t count);
};

} // namespace svc::dsp
