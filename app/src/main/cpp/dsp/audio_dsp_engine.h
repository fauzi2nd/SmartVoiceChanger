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

    // Audio Remixing Engine: Mixes converted vocals and original instrumental stems
    // Handles alignment, zero-latency phase match, per-track gain, and soft-knee peak limiting
    static std::vector<float> mixStems(
        const float* vocal_samples,
        size_t vocal_len,
        const float* inst_samples,
        size_t inst_len,
        float vocal_gain = 1.0f,
        float inst_gain = 0.95f
    );

    // STFT Spectrogram calculation for UVR MDX-Net
    // Slices input mono/stereo audio into [4, dim_f, dim_t] STFT planes (Real, Imag left, Real, Imag right)
    static std::vector<float> computeMdxStft(
        const float* audio_samples,
        size_t total_samples,
        int n_fft = 7680,
        int hop_length = 1024,
        int dim_f = 3072,
        int dim_t = 256
    );

    // Inverse STFT to reconstruct audio from [4, dim_f, dim_t] MDX-Net output tensor
    static std::vector<float> computeMdxIstft(
        const float* stft_planes,
        size_t total_output_samples,
        int n_fft = 7680,
        int hop_length = 1024,
        int dim_f = 3072,
        int dim_t = 256
    );
};

} // namespace svc::dsp
