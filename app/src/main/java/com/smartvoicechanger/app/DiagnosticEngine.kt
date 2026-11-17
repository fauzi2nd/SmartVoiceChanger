package com.smartvoicechanger.app

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

data class OnnxBenchmarkResult(
    val backendName: String,
    val isAvailable: Boolean,
    val initTimeMs: Double,
    val warmUpLatencyMs: Double,
    val avgLatencyMs: Double,
    val minLatencyMs: Double,
    val maxLatencyMs: Double,
    val repeatedRuns: Int,
    val executionStatus: String,
    val isGpuVerified: Boolean,
    val details: String
)

class DiagnosticEngine(private val context: Context) {

    private val TAG = "DiagnosticEngine"
    private val modelAssetPath = "diagnostic_benchmark.onnx"

    private fun getModelFile(): File {
        val file = File(context.cacheDir, "diagnostic_benchmark.onnx")
        // Overwrite if existing file was an older version
        context.assets.open(modelAssetPath).use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun probeVulkan(): JSONObject {
        return try {
            JSONObject(NativeBridge.probeVulkanNative())
        } catch (e: Exception) {
            JSONObject().apply {
                put("is_available", false)
                put("diagnostic_message", "Error probing Vulkan: ${e.message}")
            }
        }
    }

    fun probeOpenCL(): JSONObject {
        return try {
            JSONObject(NativeBridge.probeOpenCLNative())
        } catch (e: Exception) {
            JSONObject().apply {
                put("is_available", false)
                put("diagnostic_message", "Error probing OpenCL: ${e.message}")
            }
        }
    }

    fun runCpuReferenceBenchmark(elementCount: Int = 131072): JSONObject {
        return JSONObject(NativeBridge.runCpuReferenceBenchmarkNative(elementCount))
    }

    fun runOpenCLComputeBenchmark(elementCount: Int = 131072): JSONObject {
        return JSONObject(NativeBridge.runOpenCLComputeBenchmarkNative(elementCount))
    }

    fun runVulkanComputeBenchmark(elementCount: Int = 131072): JSONObject {
        return JSONObject(NativeBridge.runVulkanComputeBenchmarkNative(elementCount))
    }

    fun runOnnxBenchmark(useNnapi: Boolean, runs: Int = 20): OnnxBenchmarkResult {
        val backendLabel = if (useNnapi) "ONNX Runtime (NNAPI)" else "ONNX Runtime (CPU)"
        val ortEnv = OrtEnvironment.getEnvironment()

        val modelFile = getModelFile()
        Log.i(TAG, "Testing $backendLabel with model: ${modelFile.absolutePath} (len: ${modelFile.length()})")

        val opts = OrtSession.SessionOptions()

        if (useNnapi) {
            try {
                opts.addNnapi()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding NNAPI", e)
                return OnnxBenchmarkResult(
                    backendName = backendLabel,
                    isAvailable = false,
                    initTimeMs = 0.0,
                    warmUpLatencyMs = 0.0,
                    avgLatencyMs = 0.0,
                    minLatencyMs = 0.0,
                    maxLatencyMs = 0.0,
                    repeatedRuns = 0,
                    executionStatus = "Initialization Failed",
                    isGpuVerified = false,
                    details = "Failed to add NNAPI provider: ${e.message}"
                )
            }
        } else {
            opts.setIntraOpNumThreads(4)
        }

        // Measure Session Initialization Time
        val initStart = System.nanoTime()
        val session: OrtSession = try {
            ortEnv.createSession(modelFile.absolutePath, opts)
        } catch (e: Throwable) {
            Log.e(TAG, "Error creating session for $backendLabel", e)
            return OnnxBenchmarkResult(
                backendName = backendLabel,
                isAvailable = false,
                initTimeMs = 0.0,
                warmUpLatencyMs = 0.0,
                avgLatencyMs = 0.0,
                minLatencyMs = 0.0,
                maxLatencyMs = 0.0,
                repeatedRuns = 0,
                executionStatus = "Initialization Failed",
                isGpuVerified = false,
                details = "Session creation error: ${e.message}"
            )
        }
        val initEnd = System.nanoTime()
        val initTimeMs = (initEnd - initStart) / 1_000_000.0
        Log.i(TAG, "$backendLabel session initialized in $initTimeMs ms")

        // Input tensor setup: shape [1, 256, 128] = 32768 floats
        val totalElements = 1 * 256 * 128
        val inputBuffer = FloatBuffer.allocate(totalElements)
        for (i in 0 until totalElements) {
            inputBuffer.put((i % 1000) * 0.001f - 0.5f)
        }
        inputBuffer.rewind()

        val inputShape = longArrayOf(1, 256, 128)

        // Warm-up run
        val warmUpStart = System.nanoTime()
        try {
            val inputTensorWarm = OnnxTensor.createTensor(ortEnv, inputBuffer, inputShape)
            val warmResult = session.run(mapOf("input" to inputTensorWarm))
            warmResult.close()
            inputTensorWarm.close()
        } catch (e: Throwable) {
            Log.e(TAG, "Warm-up inference failed for $backendLabel", e)
            session.close()
            opts.close()
            return OnnxBenchmarkResult(
                backendName = backendLabel,
                isAvailable = false,
                initTimeMs = initTimeMs,
                warmUpLatencyMs = 0.0,
                avgLatencyMs = 0.0,
                minLatencyMs = 0.0,
                maxLatencyMs = 0.0,
                repeatedRuns = 0,
                executionStatus = "Inference Failed",
                isGpuVerified = false,
                details = "Inference error: ${e.message}"
            )
        }
        val warmUpEnd = System.nanoTime()
        val warmUpMs = (warmUpEnd - warmUpStart) / 1_000_000.0

        // Repeated inference runs
        val latencies = mutableListOf<Double>()
        for (i in 0 until runs) {
            inputBuffer.rewind()
            val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, inputShape)
            val t0 = System.nanoTime()
            val result = session.run(mapOf("input" to inputTensor))
            val t1 = System.nanoTime()
            result.close()
            inputTensor.close()
            latencies.add((t1 - t0) / 1_000_000.0)
        }

        val avgLatency = latencies.average()
        val minLatency = latencies.minOrNull() ?: 0.0
        val maxLatency = latencies.maxOrNull() ?: 0.0

        session.close()
        opts.close()

        val executionStatus = if (useNnapi) {
            "Execution Not Verified (GPU unconfirmed / NNAPI partition)"
        } else {
            "Execution Verified (ARM NEON CPU)"
        }

        return OnnxBenchmarkResult(
            backendName = backendLabel,
            isAvailable = true,
            initTimeMs = initTimeMs,
            warmUpLatencyMs = warmUpMs,
            avgLatencyMs = avgLatency,
            minLatencyMs = minLatency,
            maxLatencyMs = maxLatency,
            repeatedRuns = runs,
            executionStatus = executionStatus,
            isGpuVerified = false,
            details = "Initialized in ${"%.2f".format(initTimeMs)} ms. Latency: ${"%.2f".format(avgLatency)} ms (min: ${"%.2f".format(minLatency)} ms, max: ${"%.2f".format(maxLatency)} ms)"
        )
    }
}
