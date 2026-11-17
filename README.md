# Smart Voice Changer

**Smart Voice Changer** is an offline, AI-powered voice conversion application for Android based on Retrieval-based Voice Conversion (RVC).

The primary engineering goal of this project is to maximize actual on-device hardware acceleration (specifically targeting Qualcomm Snapdragon 695 / Adreno 619 GPU) while strictly validating actual GPU execution versus CPU fallback.

---

## Current Milestone: Milestone 1 — GPU Diagnostic & Benchmark

The project currently implements **Smart Voice Changer — GPU Diagnostic**, which performs hardware discovery, native Vulkan / OpenCL compute probing, and ONNX Runtime CPU & NNAPI inference benchmarking on Android devices.

### Key Capabilities
- **Device & SoC Diagnostics**: Real-time inspection of device manufacturer, model, Snapdragon SoC (`SM6375`), board, Android version, and build fingerprint.
- **GPU Hardware Probing**: Real-time detection of Adreno 619 GPU, driver build string, and OpenGL ES 3.2 support.
- **Native Vulkan Probing (JNI/C++)**: Dynamic runtime inspection of Vulkan 1.2 support, compute queue availability, FP16 compute shaders, and subgroup execution (64 threads).
- **Native OpenCL Probing & Compute Verification (JNI/C++)**: Dynamic linking with `/vendor/lib64/libOpenCL.so` (declared in vendor public libraries), measuring compute latency on Adreno 619, and strictly verifying mathematical correctness against CPU reference implementations.
- **ONNX Runtime Android**: Multi-threaded ARM NEON CPU inference alongside NNAPI provider evaluation with session initialization and latency benchmarking.
- **Hardware Honesty Rule**: Strictly reports *"Execution Not Verified"* whenever GPU acceleration cannot be affirmatively proven.

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
