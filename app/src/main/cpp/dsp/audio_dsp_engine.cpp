#include "audio_dsp_engine.h"
#include "kissfft/kiss_fftr.h"
#include <algorithm>
#include <cmath>
#include <cstdlib>

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

std::vector<float> AudioDspEngine::computeMdxStft(
    const float* audio_samples,
    size_t total_samples,
    int n_fft,
    int hop_length,
    int dim_f,
    int dim_t
) {
    // Tensor layout: [4, dim_f, dim_t]
    // Plane 0: Left Real, Plane 1: Left Imag, Plane 2: Right Real, Plane 3: Right Imag
    size_t total_plane_elements = static_cast<size_t>(dim_f) * dim_t;
    std::vector<float> planes(4 * total_plane_elements, 0.0f);

    if (!audio_samples || total_samples == 0) return planes;

    // Allocate KissFFTR config
    // kiss_fftr handles real forward FFT of length n_fft
    kiss_fftr_cfg cfg = kiss_fftr_alloc(n_fft, 0, nullptr, nullptr);
    if (!cfg) return planes;

    std::vector<float> window(n_fft);
    const double pi = 3.14159265358979323846;
    for (int i = 0; i < n_fft; ++i) {
        window[i] = static_cast<float>(0.5 * (1.0 - std::cos(2.0 * pi * i / n_fft)));
    }

    std::vector<float> timedata(n_fft, 0.0f);
    std::vector<kiss_fft_cpx> freqdata(n_fft / 2 + 1);

    for (int t = 0; t < dim_t; ++t) {
        size_t start_idx = static_cast<size_t>(t * hop_length);

        // Windowed input slice
        for (int i = 0; i < n_fft; ++i) {
            size_t sample_idx = start_idx + i;
            float val = (sample_idx < total_samples) ? audio_samples[sample_idx] : 0.0f;
            timedata[i] = val * window[i];
        }

        kiss_fftr(cfg, timedata.data(), freqdata.data());

        // Fill real and imaginary planes for left & right channels
        for (int f = 0; f < dim_f && f <= n_fft / 2; ++f) {
            size_t out_idx = static_cast<size_t>(f) * dim_t + t;
            float r = freqdata[f].r;
            float im = freqdata[f].i;

            planes[0 * total_plane_elements + out_idx] = r;  // Left Real
            planes[1 * total_plane_elements + out_idx] = im; // Left Imag
            planes[2 * total_plane_elements + out_idx] = r;  // Right Real (duplicated mono)
            planes[3 * total_plane_elements + out_idx] = im; // Right Imag (duplicated mono)
        }
    }

    free(cfg);
    return planes;
}

std::vector<float> AudioDspEngine::computeMdxIstft(
    const float* stft_planes,
    size_t total_output_samples,
    int n_fft,
    int hop_length,
    int dim_f,
    int dim_t
) {
    size_t chunk_samples = static_cast<size_t>((dim_t - 1) * hop_length + n_fft);
    size_t out_len = (total_output_samples > 0) ? total_output_samples : chunk_samples;
    std::vector<float> audio_out(out_len, 0.0f);
    std::vector<float> window_sum(out_len, 0.0f);

    if (!stft_planes) return audio_out;

    kiss_fftr_cfg icfg = kiss_fftr_alloc(n_fft, 1, nullptr, nullptr);
    if (!icfg) return audio_out;

    std::vector<float> window(n_fft);
    const double pi = 3.14159265358979323846;
    for (int i = 0; i < n_fft; ++i) {
        window[i] = static_cast<float>(0.5 * (1.0 - std::cos(2.0 * pi * i / n_fft)));
    }

    size_t total_plane_elements = static_cast<size_t>(dim_f) * dim_t;
    std::vector<kiss_fft_cpx> freqdata(n_fft / 2 + 1, {0.0f, 0.0f});
    std::vector<float> timedata(n_fft, 0.0f);

    for (int t = 0; t < dim_t; ++t) {
        // Construct complex frequency bins
        for (int f = 0; f <= n_fft / 2; ++f) {
            if (f < dim_f) {
                size_t in_idx = static_cast<size_t>(f) * dim_t + t;
                // Average left (0, 1) and right (2, 3) channels
                float r = (stft_planes[0 * total_plane_elements + in_idx] + stft_planes[2 * total_plane_elements + in_idx]) * 0.5f;
                float im = (stft_planes[1 * total_plane_elements + in_idx] + stft_planes[3 * total_plane_elements + in_idx]) * 0.5f;
                freqdata[f].r = r;
                freqdata[f].i = im;
            } else {
                freqdata[f].r = 0.0f;
                freqdata[f].i = 0.0f;
            }
        }

        kiss_fftri(icfg, freqdata.data(), timedata.data());

        size_t start_idx = static_cast<size_t>(t * hop_length);
        float scale = 1.0f / static_cast<float>(n_fft);

        for (int i = 0; i < n_fft; ++i) {
            size_t out_idx = start_idx + i;
            if (out_idx < out_len) {
                audio_out[out_idx] += (timedata[i] * scale) * window[i];
                window_sum[out_idx] += window[i] * window[i];
            }
        }
    }

    // Normalize by overlap-add window sum
    for (size_t i = 0; i < out_len; ++i) {
        if (window_sum[i] > 1e-4f) {
            audio_out[i] /= window_sum[i];
        }
    }

    free(icfg);
    return audio_out;
}

} // namespace svc::dsp
