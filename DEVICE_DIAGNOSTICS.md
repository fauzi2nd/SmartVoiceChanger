# Device Diagnostics — Realme 9 Pro (SM6375)

**Date of Diagnostic:** 2026-11-17  
**Device State:** Physically connected via USB, authorized via ADB (`d0476669`), non-rooted, stock firmware.

---

## 1. Device Hardware & Identity

| Attribute | Value / Output |
| :--- | :--- |
| **Manufacturer** | `realme` |
| **Model** | `RMX3472` (Realme 9 Pro 5G) |
| **Device Internal Name** | `RE54CBL1` (Product: `RMX3472T2`) |
| **SoC Model** | Qualcomm Snapdragon 695 5G (`SM6375`) |
| **Board / Platform** | `holi` |
| **Hardware Name** | `qcom` |
| **Android Version** | Android 14 |
| **API Level** | 34 (Target SDK 34 / 35 compatible) |
| **Build Fingerprint** | `realme/RMX3472T2/RE54CBL1:14/UKQ1.230924.001/S.1ca575d_5b9-11cfbb:user/release-keys` |

---

## 2. GPU & Graphics Subsystem

### 2.1 OpenGL ES
- **GPU Renderer:** `Qualcomm`, `Adreno (TM) 619`
- **OpenGL ES Version:** `OpenGL ES 3.2 V@0530.48 (GIT@e7626cbb06, If33672715f, 1674571865) (Date:01/24/23)`
- **Android Extension Pack:** Supported (`android.hardware.opengles.aep`)
- **Key GLES Extensions:** `GL_EXT_gpu_shader5`, `GL_EXT_color_buffer_half_float`, `GL_EXT_buffer_storage`, `GL_EXT_memory_object_fd`, `GL_QCOM_tiled_rendering`

### 2.2 Vulkan
- **Vulkan API Version:** `1.2.192` (`4206592`)
- **Vulkan Level:** `1` (`android.hardware.vulkan.level=1`)
- **Vulkan Compute:** Supported (`android.hardware.vulkan.compute`)
- **Driver Name:** `Qualcomm Technologies Inc. Adreno Vulkan Driver`
- **Driver Info:** `Driver Build: e7626cbb06, If33672715f, 1674571865 (Date: 01/24/23)`
- **Driver Conformance:** `1.2.1.0`
- **Subgroup Properties:**
  - `subgroupSize`: 64
  - `supportedOperations`: 63 (Arithmetic, Ballot, Shuffle, etc.)
  - `supportedStages`: Compute (32)
- **Shader Float16 / Int8:** Supported (`shaderFloat16: 1.0`, `shaderInt8: 1.0`)
- **Buffer Device Address:** Supported (`VK_KHR_buffer_device_address`)

---

## 3. Native Acceleration Libraries & Runtime Accessibility

### 3.1 Public Vendor Libraries (`/vendor/etc/public.libraries.txt` & `/system/etc/public.libraries.txt`)
The following relevant compute and hardware libraries are explicitly declared as accessible to non-system NDK applications:
- `libOpenCL.so` (Present in `/vendor/etc/public.libraries.txt` and verified in `/vendor/lib64/libOpenCL.so` - 115 KB)
- `libvulkan.so` (System public library via `/vendor/lib64/hw/vulkan.adreno.so`)
- `libneuralnetworks.so` (System public library for NNAPI)
- `libcdsprpc.so`, `libadsprpc.so` (Qualcomm Hexagon DSP RPC runtime)
- `libsnpe_loader.so` (Declared in vendor public libraries)

### 3.2 Acceleration Availability Analysis for Adreno 619

| Backend Option | System Verification | Assessment for RVC Inference |
| :--- | :--- | :--- |
| **Vulkan Compute** | Confirmed (Vulkan 1.2, float16 support) | **High feasibility**: Standard cross-platform GPU compute via Vulkan compute shaders or ONNX Runtime / NCNN / ML backends that use Vulkan. |
| **OpenCL** | Confirmed accessible via `/vendor/lib64/libOpenCL.so` in public libraries | **Available**: OpenCL 2.0 / 3.0 runtime present on Adreno 619; accessible from native C++ dynamically via `dlopen("libOpenCL.so")`. |
| **NNAPI** | `libneuralnetworks.so` present | **Limited/Deprecated**: NNAPI is available on Android 14, but Google has frozen feature development for NNAPI in favour of vendor-specific runtimes. Must be benchmarked with caution (often falls back to CPU). |
| **Qualcomm QNN (GPU)** | `libQnnGpu.so` is NOT part of vendor stock OS libraries | **Requires bundling**: To use Qualcomm QNN Execution Provider with Adreno GPU, the QNN SDK native libraries (`libQnnGpu.so`, `libQnnSystem.so`, etc.) must be bundled inside the application APK. |
| **CPU (ARM NEON)** | Fully verified (8-core Kryo 660, ARMv8-A) | **Baseline reference**: Multi-threaded ONNX Runtime with FP32/FP16 NEON SIMD. |

---

## 4. Build Toolchain Verification

| Component | Tested Version | Compatibility Verification |
| :--- | :--- | :--- |
| **JDK** | Java 25 (`Oracle JDK 25.0.5` / `Android Studio JBR 25.0.3`) | Verified running Gradle 9.8.2 without launcher errors. |
| **Gradle** | 9.8.2 | Compatible with Android Gradle Plugin 9.4.x / 9.5.x and modern Java runtimes. |
| **Android Gradle Plugin (AGP)** | 9.4.1 / 9.5.0 available in offline cache | Officially supports Gradle 9.x and Android 14/15 (`compileSdk 35`). |
| **Kotlin Gradle Plugin** | 2.2.10 / 2.3.20 / 2.4.21 available in cache | Modern Compose compiler integrated. |
| **Android NDK** | `28.2.13676358` (NDK r28b) | C++20 / C++23, Clang 19 toolchain, CMake 3.22.1 integration. |
