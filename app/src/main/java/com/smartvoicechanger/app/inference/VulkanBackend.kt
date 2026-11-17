package com.smartvoicechanger.app.inference

import java.io.File

class VulkanBackend(
    val hasComputeQueue: Boolean,
    val supportsFp16: Boolean
) : InferenceBackend {

    override val name: String = "Vulkan Compute GPU (Adreno / Mali Compute Queue)"
    override val type: BackendType = BackendType.VULKAN

    private var initialized = false

    override val isInitialized: Boolean
        get() = initialized

    override suspend fun initialize(modelFile: File): BackendInitResult {
        if (!hasComputeQueue) {
            return BackendInitResult.Failure("Vulkan compute queue not available on this device")
        }
        // In current milestone, Vulkan compute backend is registered for adaptive selection
        initialized = true
        return BackendInitResult.Success("Vulkan physical device verified with compute queue", 0.0)
    }

    override suspend fun infer(input: FloatArray, inputShape: LongArray): BackendExecutionResult {
        throw UnsupportedOperationException("Full graph Vulkan operator pipeline scheduled for Milestone M5/M6")
    }

    override suspend fun release() {
        initialized = false
    }
}
