package com.smartvoicechanger.app.inference

import java.io.File

/**
 * Hardware probing is not an ONNX execution provider. Keep this backend
 * unavailable until a real Vulkan graph implementation can execute and
 * validate the model operators.
 */
class VulkanBackend(
    val hasComputeQueue: Boolean,
    val supportsFp16: Boolean
) : InferenceBackend {

    override val name: String = "Vulkan Compute GPU (experimental)"
    override val type: BackendType = BackendType.VULKAN

    override val isInitialized: Boolean = false

    override suspend fun initialize(modelFile: File): BackendInitResult =
        BackendInitResult.Failure(
            "Vulkan compute hardware may be present, but ONNX graph execution is not implemented. Use CPU or NNAPI."
        )

    override suspend fun infer(input: FloatArray, inputShape: LongArray): BackendExecutionResult {
        throw UnsupportedOperationException("Vulkan ONNX graph execution is not implemented")
    }

    override suspend fun release() = Unit
}
