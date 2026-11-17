package com.smartvoicechanger.app.inference

import java.io.File

sealed class BackendInitResult {
    data class Success(val message: String, val initTimeMs: Double) : BackendInitResult()
    data class Failure(val reason: String, val throwable: Throwable? = null) : BackendInitResult()
}

data class BackendExecutionResult(
    val output: FloatArray,
    val latencyMs: Double,
    val isGpuAccelerated: Boolean
)

enum class BackendType {
    CPU,
    NNAPI,
    VULKAN
}

interface InferenceBackend {
    val name: String
    val type: BackendType
    val isInitialized: Boolean

    suspend fun initialize(modelFile: File): BackendInitResult
    suspend fun infer(input: FloatArray, inputShape: LongArray): BackendExecutionResult
    suspend fun release()
}
