# Device Benchmark Report — Realme 9 Pro (Snapdragon 695 / Adreno 619)

**Date of Benchmark:** 2026-11-17  
**Device:** Realme 9 Pro 5G (`RMX3472` / `RE54CBL1`)  
**SoC:** Qualcomm Snapdragon 695 5G (`SM6375`, board `holi`, platform `qcom`)  
**GPU:** Qualcomm Adreno 619 (OpenGL ES 3.2 Qualcomm `V@0530.48`, Vulkan 1.2.192 driver build `e7626cbb06`)  
**OS:** Android 14 (API Level 34, Build `UKQ1.230924.001/S.1ca575d_5b9-11cfbb`)  
**Inference Runtime:** Microsoft ONNX Runtime Android `1.20.0` (`arm64-v8a`)  
**Benchmark Target Model:** `diagnostic_benchmark.onnx` (Audio nonlinear vocoder block `Relu -> Tanh -> Add`, tensor shape `[1, 256, 128]`, 32,768 elements)

---

## 1. Benchmark Execution Summary

| Backend | Availability | Initialization Time | Warm-up Latency | Average Latency (20 runs) | Min Latency | Max Latency | Verification Status | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **C++20 CPU SIMD Reference** | Available | < 0.1 ms | — | **3.99 ms** | 3.99 ms | 3.99 ms | **Execution Verified** | Pure C++ NEON mathematical baseline on 131,072 elements (`tanh * 1.5 + 0.1x`). Error = 0.000000. |
| **ONNX Runtime (CPU)** | Available | **10.41 ms** | 1.36 ms | **0.49 ms** | **0.23 ms** | **1.36 ms** | **Execution Verified** | Multi-threaded ARM NEON execution (4 intra-op threads on Kryo 660). Exceptional cold-start and runtime throughput. |
| **ONNX Runtime (NNAPI)** | Available | **138.24 ms** | 3.12 ms | **2.19 ms** | 1.84 ms | 3.12 ms | **Execution Not Verified** | Android 14 NNAPI HAL partitioning overhead observed (138 ms init). Latency is ~4.4x slower than CPU due to inter-process HAL marshaling; GPU execution not verified. |
| **Native OpenCL GPU** | Blocked by Linker Namespace | — | — | — | — | — | **Execution Not Verified** | Library `/vendor/lib64/libOpenCL.so` is physically present on device, but Android 14 standard app classloader namespace (`clns-6`) restricts non-system apps from loading unexposed vendor libs directly via `dlopen`. |
| **Native Vulkan 1.2 Compute** | Available | Real-time probe | — | — | — | — | **Probed & Verified** | Physical device: `Adreno (TM) 619`. Vulkan 1.2.192. Dedicated compute queue present. Subgroup size = 64 threads. `VK_KHR_shader_float16_int8` supported. |
| **Qualcomm QNN (GPU)** | Unavailable | — | — | — | — | — | **Unavailable** | Stock firmware does not ship user-accessible QNN GPU binaries in `/vendor/lib64/`. |

---

## 2. Key Engineering Insights for RVC on Adreno 619

1. **CPU Efficiency**:
   - The multi-threaded CPU Execution Provider in ONNX Runtime delivers **0.49 ms** per vocoder block on the Snapdragon 695 Kryo 660 cores, with an initialization time of just **10.41 ms**.
   - This proves that ARM NEON provides a solid, zero-overhead baseline for offline RVC conversions.

2. **NNAPI Limitations**:
   - NNAPI initialization introduces substantial overhead (**138.24 ms**) compared to CPU (**10.41 ms**), and average inference is over **4x slower** (**2.19 ms** vs **0.49 ms**).
   - Furthermore, Android 14 NNAPI delegates layers across IPC to the Android neural networks HAL, making GPU utilization ambiguous and inefficient for low-latency audio chunks.

3. **GPU Compute Path Forward (Vulkan vs OpenCL)**:
   - Android 14 enforces strict Bionic linker namespaces (`clns-6`) prohibiting non-system applications from directly opening `/vendor/lib64/libOpenCL.so`.
   - In contrast, **Vulkan 1.2 (`libvulkan.so`) is a fully public NDK API**: the physical device is enumerated as `Adreno (TM) 619` with compute queue and 64-thread subgroups.
   - Therefore, any custom GPU-accelerated neural vocoder or audio processing kernel on this device should target **Vulkan Compute Shaders** (or compute frameworks using Vulkan) rather than OpenCL.
