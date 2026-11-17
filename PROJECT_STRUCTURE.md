# Smart Voice Changer — Project Structure

```
SmartVoiceChanger/
├── app/
│   ├── build.gradle.kts                # App dependencies, NDK CMake config, ONNX Runtime
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # Permissions (RECORD_AUDIO, INTERNET, FOREGROUND_SERVICE)
│           ├── assets/
│           │   ├── diagnostic_benchmark.onnx  # Benchmark tensor model
│           │   └── models/             # Target neural voice models (.onnx)
│           ├── cpp/
│           │   ├── CMakeLists.txt      # Native build script
│           │   ├── native-lib.cpp      # JNI bridge
│           │   ├── gpu_diagnostic_probe.cpp # Vulkan & OpenCL hardware prober
│           │   └── dsp/
│           │       └── audio_dsp_engine.cpp # Native C++20 pitch shifting & resampling
│           └── java/com/smartvoicechanger/app/
│               ├── MainActivity.kt     # App entry point, navigation coordinator & state
│               ├── NativeBridge.kt     # JNI declarations
│               ├── audio/
│               │   ├── AudioData.kt              # PCM float sample container
│               │   ├── AudioDecoderHelper.kt     # Universal MediaCodec/MediaExtractor decoder
│               │   ├── AudioRecorderHelper.kt    # 44.1 kHz 16-bit PCM mic recorder
│               │   ├── VoiceConversionPipeline.kt# Audio pipeline (DSP + Inference + AudioTrack)
│               │   └── WavIoHelper.kt            # RIFF WAV reader and writer
│               ├── inference/
│               │   ├── AdaptiveInferenceManager.kt# Multi-backend evaluator & fallback
│               │   ├── BackendRegistry.kt        # Registry for CPU, NNAPI, Vulkan
│               │   ├── BackendSelfTester.kt      # Mathematical & latency validation
│               │   ├── CpuBackend.kt             # Multi-threaded ARM NEON ONNX Runtime
│               │   ├── InferenceBackend.kt       # Interface contracts
│               │   ├── NnapiBackend.kt           # Android NNAPI acceleration evaluator
│               │   └── VulkanBackend.kt          # Vulkan GPU compute probe & runner
│               ├── model/
│               │   └── VoiceModel.kt             # Voice model definitions & metadata
│               └── ui/
│                   ├── HomeScreen.kt             # Neural studio home & hardware status
│                   ├── RecordBoothScreen.kt      # Vocal recording booth & visualizer
│                   ├── LinkStreamScreen.kt       # Remote media ingest & stream download
│                   ├── AudioTuningScreen.kt      # Pitch tuning & voice model preset selector
│                   ├── ActivityScreen.kt         # Conversion progress, history & player
│                   ├── ModelsScreen.kt           # Voice catalog management
│                   ├── SettingsScreen.kt         # Engine settings & hardware diagnostics
│                   ├── DiagnosticScreen.kt       # Detailed SoC/Vulkan/NNAPI report
│                   └── theme/                    # SvcColors, SvcFonts, SvcTheme
├── ARCHITECTURE.md                     # High-level architecture documentation
├── DEVELOPMENT_GUIDE.md                # Development standard, rules & build instructions
├── DEVELOPMENT_STATUS.md               # Honest milestone tracker & verification status
├── DEVICE_BENCHMARK.md                 # Snapdragon 695 hardware benchmark report
├── DEVICE_DIAGNOSTICS.md               # Hardware diagnostics on physical Realme 9 Pro
├── GPU_COMPATIBILITY.md                # Vulkan compute queue analysis vs OpenCL namespaces
└── README.md                           # Main repository README
```
