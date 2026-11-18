# Smart Voice Changer

**Smart Voice Changer** is an offline, AI-powered voice conversion application for Android based on Retrieval-based Voice Conversion (RVC).

The primary engineering goal of this project is to maximize actual on-device hardware acceleration (specifically targeting Qualcomm Snapdragon 695 / Adreno 619 GPU) while strictly validating actual GPU execution versus CPU fallback.

---

## Current Milestone: Milestone 7 — RVC ONNX Integration (model import and runtime validation)

The project implements a modern mobile neural voice conversion studio with:
- **Audio Decoding Engine**: Hardware-accelerated `MediaExtractor` + `MediaCodec` decoding MP3, AAC, M4A, FLAC, OGG, and WAV into standard 44.1 kHz 16-bit Mono PCM WAV.
- **Studio Preset Tuning**: Interactive pitch tuning (-12 to +12 semitones) and target voice model preset selection with live preview.
- **Microphone Vocal Booth**: Real-time microphone capture with live visualizer, recording review, and tuning handoff.
- **Native C++20 Audio DSP**: SIMD-accelerated pitch shifting and sinc/linear resampling on ARM NEON.
- **RVC Inference**: HuBERT/ContentVec feature extraction, optional RMVPE F0 extraction, feature alignment, and RVC synthesizer ONNX execution on CPU.
- **Hardware Honesty**: Active execution backend is reported as **ARM NEON CPU**. Vulkan hardware probing is not treated as model execution; Vulkan remains unavailable until a real ONNX graph provider is implemented. Zero fake benchmark numbers or simulated history.

---

## RVC model setup

The app does not ship trained voice models. Import three compatible ONNX files from the Audio Tuning screen:

1. **Synthesizer**: RVC ONNX exported with embedded `metadata` JSON (`samplingRate`, `f0`, `embChannels`, `embedder`, `embOutputLayer`, `useFinalProj`) and `audio` output.
2. **HuBERT / ContentVec**: ONNX with `audio` input and the `unit12`, `units9`, and/or `unit12s` outputs expected by the synthesizer metadata.
3. **RMVPE**: Required for F0-enabled synthesizers; ONNX inputs must be `waveform` and `threshold`.

The imported files are copied to app-private storage under `files/models/rvc/`. The app validates the synthesizer metadata, tensor signatures, and feature dimensions before saving converted audio. The four legacy preset assets are placeholders and are no longer used for neural inference.

## Hardware Discovery & Inference Backends
- **Device & SoC Diagnostics**: Real-time inspection of device manufacturer, model, Snapdragon SoC (`SM6375`), board, Android version, and build fingerprint.
- **GPU Hardware Probing**: Real-time detection of Adreno 619 GPU, driver build string, and OpenGL ES 3.2 support.
- **Native Vulkan Probing (JNI/C++)**: Dynamic runtime inspection of Vulkan 1.2 support, compute queue availability, FP16 compute shaders, and subgroup execution (64 threads).
- **Inference Runtime**: Multi-threaded ARM NEON CPU inference via ONNX Runtime Android alongside NNAPI evaluation.

---

## Architecture & Technology Stack

- **UI**: Kotlin + Jetpack Compose with Material 3 dark-themed audio tool aesthetics.
- **Native Engine**: C++20 with Android NDK 28 (r28b).
- **Build System**: CMake 3.22.1 + Gradle 9.8.2 + AGP 9.5.0.
- **Inference Runtime**: Microsoft ONNX Runtime Android (`com.microsoft.onnxruntime:onnxruntime-android:1.20.0`).
- **Target ABI**: `arm64-v8a` (`minSdk = 26`, `targetSdk = 34`, `compileSdk = 35`).

---

## Building and Running

### Build Debug APK:
```powershell
.\gradlew.bat :app:assembleDebug
```
The resulting APK is produced at:
`app/build/outputs/apk/debug/app-debug.apk` and copied to root as `SmartVoiceChanger.apk`.

### Install via ADB:
```powershell
adb install -r SmartVoiceChanger.apk
adb shell am start -n com.smartvoicechanger.app/.MainActivity
```
