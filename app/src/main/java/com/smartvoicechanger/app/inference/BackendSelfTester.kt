package com.smartvoicechanger.app.inference

import java.io.File

data class SelfTestResult(
    val backendType: BackendType,
    val passed: Boolean,
    val initLatencyMs: Double,
    val inferenceLatencyMs: Double,
    val maxNumericalError: Float,
    val diagnosticDetails: String
)

class BackendSelfTester {

    suspend fun testBackend(
        backend: InferenceBackend,
        modelFile: File,
        testInput: FloatArray,
        testShape: LongArray,
        referenceOutput: FloatArray?
    ): SelfTestResult {
        // Step 1: Session Initialization Test
        val initRes = backend.initialize(modelFile)
        if (initRes !is BackendInitResult.Success) {
            val reason = (initRes as BackendInitResult.Failure).reason
            return SelfTestResult(
                backendType = backend.type,
                passed = false,
                initLatencyMs = 0.0,
                inferenceLatencyMs = 0.0,
                maxNumericalError = Float.MAX_VALUE,
                diagnosticDetails = "Init failed: $reason"
            )
        }

        // Step 2 & 3: Inference Test using test tensor
        return try {
            val execRes = backend.infer(testInput, testShape)

            // Step 4: Numerical Validation against CPU reference
            var maxErr = 0.0f
            var hasInvalidValues = false

            for (i in execRes.output.indices) {
                val v = execRes.output[i]
                if (v.isNaN() || v.isInfinite()) {
                    hasInvalidValues = true
                    break
                }
                if (referenceOutput != null && i < referenceOutput.size) {
                    val diff = kotlin.math.abs(v - referenceOutput[i])
                    if (diff > maxErr) maxErr = diff
                }
            }

            if (hasInvalidValues) {
                SelfTestResult(
                    backendType = backend.type,
                    passed = false,
                    initLatencyMs = initRes.initTimeMs,
                    inferenceLatencyMs = execRes.latencyMs,
                    maxNumericalError = Float.NaN,
                    diagnosticDetails = "Output contained NaN or Infinity values"
                )
            } else if (referenceOutput != null && maxErr > 1e-3f) {
                SelfTestResult(
                    backendType = backend.type,
                    passed = false,
                    initLatencyMs = initRes.initTimeMs,
                    inferenceLatencyMs = execRes.latencyMs,
                    maxNumericalError = maxErr,
                    diagnosticDetails = "Numerical discrepancy exceeded threshold ($maxErr > 1e-3)"
                )
            } else {
                SelfTestResult(
                    backendType = backend.type,
                    passed = true,
                    initLatencyMs = initRes.initTimeMs,
                    inferenceLatencyMs = execRes.latencyMs,
                    maxNumericalError = maxErr,
                    diagnosticDetails = "Self-test passed (Latency: ${"%.2f".format(execRes.latencyMs)} ms, Err: ${"%.6f".format(maxErr)})"
                )
            }
        } catch (t: Throwable) {
            SelfTestResult(
                backendType = backend.type,
                passed = false,
                initLatencyMs = initRes.initTimeMs,
                inferenceLatencyMs = 0.0,
                maxNumericalError = Float.MAX_VALUE,
                diagnosticDetails = "Inference execution threw exception: ${t.message}"
            )
        }
    }
}
