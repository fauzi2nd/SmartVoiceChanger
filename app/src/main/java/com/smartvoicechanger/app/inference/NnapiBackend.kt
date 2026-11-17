package com.smartvoicechanger.app.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer

class NnapiBackend : InferenceBackend {

    override val name: String = "ONNX Runtime NNAPI (Android Neural Networks)"
    override val type: BackendType = BackendType.NNAPI

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var sessionOptions: OrtSession.SessionOptions? = null

    override val isInitialized: Boolean
        get() = session != null

    override suspend fun initialize(modelFile: File): BackendInitResult = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        try {
            val env = OrtEnvironment.getEnvironment()
            ortEnv = env

            val opts = OrtSession.SessionOptions().apply {
                addNnapi()
            }
            sessionOptions = opts

            session = env.createSession(modelFile.absolutePath, opts)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
            BackendInitResult.Success("NNAPI Session created", elapsedMs)
        } catch (t: Throwable) {
            BackendInitResult.Failure("Failed to initialize NNAPI backend: ${t.message}", t)
        }
    }

    override suspend fun infer(input: FloatArray, inputShape: LongArray): BackendExecutionResult = withContext(Dispatchers.Default) {
        val s = session ?: throw IllegalStateException("NnapiBackend is not initialized")
        val env = ortEnv ?: throw IllegalStateException("OrtEnvironment is null")

        val buffer = FloatBuffer.wrap(input)
        val tensor = OnnxTensor.createTensor(env, buffer, inputShape)

        val t0 = System.nanoTime()
        val results = s.run(mapOf(s.inputNames.iterator().next() to tensor))
        val t1 = System.nanoTime()

        val outputTensor = results.get(0) as OnnxTensor
        val outBuffer = outputTensor.floatBuffer
        val outArray = FloatArray(outBuffer.remaining())
        outBuffer.get(outArray)

        results.close()
        tensor.close()

        val latencyMs = (t1 - t0) / 1_000_000.0
        // Under our strict rules: NNAPI cannot claim GPU execution without explicit driver confirmation
        BackendExecutionResult(
            output = outArray,
            latencyMs = latencyMs,
            isGpuAccelerated = false
        )
    }

    override suspend fun release() = withContext(Dispatchers.IO) {
        try {
            session?.close()
            sessionOptions?.close()
        } catch (_: Exception) {}
        session = null
        sessionOptions = null
        ortEnv = null
    }
}
