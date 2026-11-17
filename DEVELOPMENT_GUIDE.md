# Smart Voice Changer — Development Guide

## 1. Project Overview & Architecture

**Smart Voice Changer** (`com.smartvoicechanger.app`) is an offline, privacy-first audio studio app for Android that performs audio processing and voice conversion.

Target Hardware: **Qualcomm Snapdragon 695 5G / Adreno 619** (tested on Realme 9 Pro, Android 14 API 34).

### Architecture Principles:
- **Audio Decoding**: Uses Android's low-level hardware-accelerated `MediaExtractor` and `MediaCodec` pipeline (`AudioDecoderHelper`) to decode arbitrary audio containers (MP3, AAC, M4A, FLAC, OGG, WAV) into standard 44.1 kHz 16-bit Mono PCM WAV files.
- **Native DSP Engine**: High-performance C++20 routines compiled via Android NDK for linear interpolation resampling, semitone pitch shifting, and sample normalization.
- **Inference Runtime**: Microsoft ONNX Runtime Mobile (`ai.onnxruntime:onnxruntime-android`) targeting ARM NEON CPU.
- **UI & Experience**: 100% English, Jetpack Compose, cyber-audio aesthetic (Orbitron/Rajdhani fonts, dark futuristic neon glassmorphism).

---

## 2. Audio Processing Flow

```
Input Audio Source (File Picker / Mic Recording / Stream URL)
  ↓
[AudioDecoderHelper / AudioRecorderHelper]
  ↓ (Extracts/Decodes to 44.1 kHz 16-bit Mono PCM WAV)
[AudioTuningScreen] (User configures Model & Semitone Pitch: -12 to +12)
  ↓
[VoiceConversionPipeline]
  ├── 1. Read WAV PCM Float Samples
  ├── 2. Native C++20 DSP Pitch Shift (Semitones)
  ├── 3. Native C++20 Sample Resampling
  ├── 4. Neural Model Inference (ONNX Runtime Mobile)
  └── 5. Write Standard WAV Output
  ↓
[ActivityScreen]
  ├── Live Progress Tracking (0% - 100%)
  ├── Elapsed Latency Display (Real measured duration)
  └── Interactive Playback via AudioTrack
```

---

## 3. Hardware Honesty & Metric Rules

1. **No Fabricated Numbers**:
   - Never display placeholder latencies (e.g. `0.49 ms`) as if they were current measurements.
   - If conversion has not run, display `-- ms` or `Standby`.
   - The conversion history list must start empty and only display actual completed conversion sessions.

2. **Backend Transparency**:
   - The active backend is explicitly reported as **ARM NEON CPU**.
   - Vulkan GPU and NNAPI are documented as experimental / evaluation backends. Never claim active GPU inference until actual neural model execution is running on the GPU compute queue.

3. **Benchmark Boundaries**:
   - Clearly delineate synthetic / diagnostic vocoder block benchmarks (`diagnostic_benchmark.onnx`) from full end-to-end RVC model execution.

---

## 4. Building, Installing & Testing

### Prerequisites
- JDK 17 or 21
- Android SDK (API 34/35) & NDK 28 (28.0.12674087)
- CMake 3.22.1+

### Build Debug APK:
```powershell
.\gradlew.bat :app:assembleDebug
```

### Install and Launch:
```powershell
adb install -r SmartVoiceChanger.apk
adb shell am start -n com.smartvoicechanger.app/.MainActivity
```
