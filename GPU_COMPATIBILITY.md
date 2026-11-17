# GPU Compatibility Analysis — Snapdragon 695 / Adreno 619

This document records the empirical hardware compatibility analysis for offline ML inference and audio compute on the **Realme 9 Pro (Snapdragon 695 5G / Adreno 619)**.

---

## 1. Hardware Architecture Overview

- **SoC:** Qualcomm Snapdragon 695 5G (`SM6375`, board `holi`).
- **CPU:** 8-core Kryo 660 (2x Cortex-A78 @ 2.2 GHz + 6x Cortex-A55 @ 1.8 GHz) with ARM NEON FP16/FP32 SIMD.
- **GPU:** Qualcomm Adreno (TM) 619.
- **GPU Architecture:** Adreno 600 family with unified compute units, 64-thread wavefront/subgroup size, FP16 arithmetic ALUs.
- **DSP/NPU:** Hexagon DSP with Hexagon Vector Extensions (HVX), accessible via CDSP RPC (`libcdsprpc.so`).

---

## 2. Evaluation of Acceleration Backends

| Backend | Platform Support on Realme 9 Pro | Viability for RVC Inference | Empirical Assessment |
| :--- | :--- | :--- | :--- |
| **ONNX Runtime CPU** | Verified & Built-in | **High (Guaranteed Baseline)** | Uses multi-threaded ARM NEON execution. Completely deterministic, supports all ONNX opsets. |
| **ONNX Runtime NNAPI** | System library `libneuralnetworks.so` present | **Medium (Partitioned)** | Deprecated by Google in modern Android. On Snapdragon 695, NNAPI frequently delegates layers to CPU or Hexagon DSP HAL. Cannot confirm GPU execution without driver-level tracing. |
| **Native OpenCL GPU** | Vendor public library `/vendor/lib64/libOpenCL.so` present | **High (Direct GPU Compute)** | Adreno 619 exposes full OpenCL 2.0/3.0. Non-system apps can link to it directly via `dlopen`. Ideal for custom vocoder/FFT/activation kernels. |
| **Native Vulkan Compute** | Vulkan 1.2 (`Qualcomm Adreno Vulkan Driver`) present | **High (Cross-Platform GPU)** | Exposes compute queue with subgroup size 64 and `VK_KHR_shader_float16_int8`. Excellent for cross-platform compute pipelines (e.g. NCNN Vulkan or custom compute shaders). |
| **Qualcomm QNN (GPU)** | Not present in stock `/vendor/lib64/` | **Conditional (Requires APK Bundling)** | Requires bundling `libQnnGpu.so` and QNN runtime inside the APK. Experimental until tested. |

---

## 3. Acceleration Classification Policy

Under the Smart Voice Changer engineering discipline:
1. **Never assume GPU execution**: The presence of a library or execution provider does not confirm execution on the GPU.
2. **Explicit Verification**:
   - For OpenCL/Vulkan: Native code compiles and dispatches a compute workload directly to the Adreno GPU device ID and measures real execution time and numerical outputs.
   - For ONNX NNAPI: Until an explicit GPU hardware delegate is confirmed by vendor telemetry, status is strictly reported as *"Execution Not Verified (GPU unconfirmed / NNAPI partition)"*.
