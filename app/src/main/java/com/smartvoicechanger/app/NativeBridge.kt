package com.smartvoicechanger.app

object NativeBridge {
    init {
        System.loadLibrary("smartvoicechanger_native")
    }

    external fun getNativeBuildInfo(): String
    external fun probeVulkanNative(): String
    external fun probeOpenCLNative(): String
    external fun runCpuReferenceBenchmarkNative(elementCount: Int): String
    external fun runOpenCLComputeBenchmarkNative(elementCount: Int): String
    external fun runVulkanComputeBenchmarkNative(elementCount: Int): String

    // Native DSP operations
    external fun pitchShiftNative(samples: FloatArray, sampleRate: Int, semitones: Float): FloatArray
    external fun resampleNative(samples: FloatArray, srcRate: Int, targetRate: Int): FloatArray

    // Native STFT / iSTFT for MDX-Net
    external fun computeMdxStftNative(
        samples: FloatArray,
        nFft: Int = 7680,
        hopLength: Int = 1024,
        dimF: Int = 3072,
        dimT: Int = 256
    ): FloatArray

    external fun computeMdxIstftNative(
        stftPlanes: FloatArray,
        totalOutputSamples: Int,
        nFft: Int = 7680,
        hopLength: Int = 1024,
        dimF: Int = 3072,
        dimT: Int = 256
    ): FloatArray
}
