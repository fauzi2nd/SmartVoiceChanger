package com.smartvoicechanger.app.inference

import java.io.File

class AdaptiveInferenceManager(
    private val capabilities: DeviceCapabilities,
    private val registry: BackendRegistry,
    private val selfTester: BackendSelfTester = BackendSelfTester()
) {

    private var activeBackend: InferenceBackend? = null
    private val testResults = mutableMapOf<BackendType, SelfTestResult>()

    fun getActiveBackend(): InferenceBackend? = activeBackend
    fun getTestResults(): Map<BackendType, SelfTestResult> = testResults

    suspend fun resolveAndSelectBackend(
        modelFile: File,
        testInput: FloatArray,
        testShape: LongArray
    ): InferenceBackend {
        // Step 1: Run CPU reference self-test first to establish ground truth
        val cpuBackend = registry.getBackend(BackendType.CPU) as CpuBackend
        val cpuTestResult = selfTester.testBackend(
            backend = cpuBackend,
            modelFile = modelFile,
            testInput = testInput,
            testShape = testShape,
            referenceOutput = null
        )
        testResults[BackendType.CPU] = cpuTestResult

        var cpuReferenceOutput: FloatArray? = null
        if (cpuTestResult.passed) {
            val exec = cpuBackend.infer(testInput, testShape)
            cpuReferenceOutput = exec.output
        }

        // Step 2: Test other available accelerated backends
        for (backend in registry.getPrioritizedBackends()) {
            if (backend.type == BackendType.CPU) continue

            val result = selfTester.testBackend(
                backend = backend,
                modelFile = modelFile,
                testInput = testInput,
                testShape = testShape,
                referenceOutput = cpuReferenceOutput
            )
            testResults[backend.type] = result

            // Evaluate if this backend is viable and outperforms CPU
            if (result.passed && cpuTestResult.passed) {
                // Check if latency is actually better than CPU
                if (result.inferenceLatencyMs < cpuTestResult.inferenceLatencyMs) {
                    activeBackend = backend
                    return backend
                }
            }
        }

        // Fallback: CPU is always the safe, reliable default
        activeBackend = cpuBackend
        return cpuBackend
    }

    suspend fun inferSafe(
        input: FloatArray,
        inputShape: LongArray
    ): BackendExecutionResult {
        val current = activeBackend ?: throw IllegalStateException("No backend selected")

        return try {
            current.infer(input, inputShape)
        } catch (t: Throwable) {
            // Automatic Chunk-Boundary Fallback to CPU
            if (current.type != BackendType.CPU) {
                val fallbackCpu = registry.getBackend(BackendType.CPU) ?: throw t
                activeBackend = fallbackCpu
                fallbackCpu.infer(input, inputShape)
            } else {
                throw t
            }
        }
    }
}
