# Smart Voice Changer — Architecture

## 1. Universal Adaptive Architecture Overview

Smart Voice Changer is built using a vendor-agnostic **Universal Adaptive** architecture. It guarantees execution across arbitrary Android devices without requiring user intervention or assuming specific SoC / GPU models.

```text
+-----------------------------------------------------------------------------+
|                          Jetpack Compose UI Layer                           |
|        (VoiceConverterScreen, DiagnosticScreen, DashboardCards, Controls)   |
+-----------------------------------------------------------------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                        Adaptive Inference Manager                           |
|      (HardwareInspector -> BackendRegistry -> SelfTester -> Fallback)       |
+-------------------------------------+---------------------------------------+
                                      |
                                      v
               +----------------------+----------------------+
               |                      |                      |
               v                      v                      v
     +--------------------+ +--------------------+ +--------------------+
     |   VulkanBackend    | |    NnapiBackend    | |     CpuBackend     |
     |   (GPU Compute)    | |   (Android HAL)    | | (ARM NEON Fallback)|
     +--------------------+ +--------------------+ +--------------------+
                                      |
                                      v
+-----------------------------------------------------------------------------+
|                        Native C++20 Core & DSP Engine                       |
|   (AudioDspEngine: Pitch Shift, Resample, Cross-fade, WAV I/O, Vulkan Probe)|
+-----------------------------------------------------------------------------+
```

---

## 2. Core Modules & Responsibilities

### 2.1 Hardware Abstraction (`com.smartvoicechanger.app.inference.HardwareInspector`)
- Discovers RAM size (`ActivityManager.MemoryInfo`), CPU cores, thermal state (`PowerManager.currentThermalStatus`), and Vulkan device capabilities via JNI.
- Categorizes devices into three runtime profiles:
  - `ENTRY`: Low RAM (< 4GB) or <= 4 CPU cores (conservative thread allocation and chunk size).
  - `STANDARD`: 4GB to 8GB RAM (multi-threaded CPU baseline and NNAPI eligibility).
  - `PERFORMANCE`: > 8GB RAM with verified GPU compute queues.

### 2.2 Backend Registry (`BackendRegistry`)
- Provides a unified contract (`InferenceBackend`):
  - `initialize(modelFile)`
  - `infer(input, inputShape)`
  - `release()`
- Implemented backends:
  - `CpuBackend`: Multi-threaded ARM NEON ONNX Runtime with auto-thread tuning.
  - `NnapiBackend`: Android Neural Networks HAL integration.
  - `VulkanBackend`: Native Vulkan compute pipeline.

### 2.3 Self-Test Engine (`BackendSelfTester`)
- Every backend undergoes four strict self-test phases before being approved:
  1. Session initialization.
  2. Test tensor inference.
  3. Numerical validation against the CPU reference baseline (`NaN`/`Infinity` checks and max error tolerance < 1e-3).
  4. Latency qualification (accelerated backends are only selected if they demonstrably beat CPU latency).

### 2.4 Automatic Chunk-Boundary Fallback (`AdaptiveInferenceManager`)
- If an accelerated backend fails or produces invalid values, execution automatically and seamlessly falls back to `CpuBackend` at the safe chunk boundary without application crashes.

### 2.5 Audio Processing Pipeline (`com.smartvoicechanger.app.audio`)
- High-performance native C++ DSP (`AudioDspEngine`):
  - Time-domain pitch shifting (-12 to +12 semitones).
  - Linear audio resampling.
  - Overlap cross-fading for artifact-free chunk stitching.
- Pure Kotlin WAV reader and writer (`WavIoHelper`).
- Low-latency PCM streaming playback via `AudioTrack`.
