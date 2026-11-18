# Development Status — Smart Voice Changer

**Current Milestone:** Milestone 7 — RVC ONNX Integration (first implementation pass)  
**Device Tested:** Realme 9 Pro (Snapdragon 695 / Adreno 619, Android 14 API 34)

---

## Progress Overview

| Phase / Milestone | Status | Details |
| :--- | :--- | :--- |
| **M1: Hardware Abstraction & Diagnostics** | Completed | `HardwareInspector` implemented; probes Qualcomm Snapdragon 695, Adreno 619, Vulkan 1.2 compute queues, and thermals. |
| **M2: Backend Registry** | Completed | Clean `InferenceBackend` contract for `CpuBackend`, `NnapiBackend`, and `VulkanBackend`. |
| **M3: Self-Test Engine & Benchmarking** | Completed | `BackendSelfTester` checks initialization, numerical validation, and latency with vocoder test block (`diagnostic_benchmark.onnx`). |
| **M4: Audio Studio Workflow** | Completed | Full interactive multi-stage UX: Audio picker, Vocal recording booth, and Link ingest leading to `AudioTuningScreen` (pitch shift & model preset). |
| **M5: Universal Audio Decoding & Native DSP** | Completed | `AudioDecoderHelper` converts any input format (MP3, AAC, FLAC, M4A, WAV) to PCM WAV; C++20 `AudioDspEngine` performs pitch shifting and resampling. |
| **M6: Neural Stem Separation (Vocals / Inst)** | Completed | Integrated **UVR-MDX-Net** ONNX model with C++ KissFFT STFT/iSTFT engine. Supports on-demand verified SHA-256 downloading, independent vocal/instrumental extraction, preview playback, and direct vocal handoff to neural voice conversion. |
| **M7: Active Backend Reporting** | Active | Multi-threaded ARM NEON CPU set as active production backend. Vulkan GPU compute graph integration in research/evaluation. |
| **M7: RVC ONNX Inference** | First implementation pass | Added HuBERT/ContentVec extraction, optional RMVPE pitch extraction, v1/v2 feature selection from synthesizer metadata, 2x frame alignment, and synthesizer inference. Requires compatible user-imported ONNX assets and device validation. |

---

## Deliverables

- `SmartVoiceChanger.apk` (Root directory, 32 MB, debug signed)
- Source code in `app/src/main/java/com/smartvoicechanger/app/` (`inference/`, `audio/`, `model/`, `ui/`)
- Native C++20 source code in `app/src/main/cpp/` (`dsp/`, `gpu_diagnostic_probe.cpp`, `native-lib.cpp`)
- Documentation: `README.md`, `ARCHITECTURE.md`, `DEVELOPMENT_STATUS.md`, `DEVICE_DIAGNOSTICS.md`, `DEVICE_BENCHMARK.md`, `GPU_COMPATIBILITY.md`.


## RVC Integration Notes

- RVC model files are imported from the Audio Tuning screen and stored in `files/models/rvc/` as `synth.onnx`, `hubert.onnx`, and `rmvpe.onnx`.
- Conversion no longer sends raw waveform tensors directly to a synthesizer. It runs the HuBERT -> optional RMVPE -> synthesizer sequence.
- F0-enabled models require RMVPE. Synthesizer metadata and ONNX input/output names are validated before inference.
- The runtime currently targets the ContentVec ONNX signature (`audio` input; `unit12`, `units9`, `unit12s` outputs) and the voice-changer RVC synthesizer signature. Other exports may need an adapter.
- Vulkan is not registered as an inference backend until real ONNX graph execution is implemented. Vulkan hardware capability detection alone is not sufficient.
- Build and physical-device validation are still required; this branch is not yet verified as a release APK.
