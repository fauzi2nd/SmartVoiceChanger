package com.smartvoicechanger.app.audio

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.smartvoicechanger.app.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer

data class StemSeparationResult(
    val vocalFile: File,
    val instrumentalFile: File,
    val durationSeconds: Int,
    val sampleRate: Int = 44100
)

/**
 * Robust Neural Source Separator using UVR-MDX-Net ONNX Runtime + C++ KissFFT STFT/iSTFT.
 * Performs real-time bounded-memory chunk processing on Snapdragon 695 / Android ARM64.
 */
class StemSeparator(private val context: Context) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    suspend fun separate(
        inputFile: File,
        outputVocalFile: File,
        outputInstrumentalFile: File,
        onProgress: (Float) -> Unit
    ): StemSeparationResult = withContext(Dispatchers.Default) {
        val modelFile = ModelDownloadService.getModelFile(context)
        if (!modelFile.exists() || !ModelDownloadService.isModelReady(context)) {
            throw IllegalStateException("MDX-Net neural model not loaded. Please download model first.")
        }

        onProgress(0.05f)

        // 1. Read input WAV
        val inputAudio = inputFile.inputStream().use {
            WavIoHelper.readWav(it)
        }

        val totalSamples = inputAudio.samples.size
        val sampleRate = inputAudio.sampleRate
        val samples = inputAudio.samples

        onProgress(0.10f)

        // 2. Initialize ONNX Runtime Session
        val env = ortEnv ?: OrtEnvironment.getEnvironment().also { ortEnv = it }
        val session = ortSession ?: run {
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            val s = env.createSession(modelFile.absolutePath, opts)
            ortSession = s
            s
        }

        val inputName = session.inputNames.iterator().next()

        // 3. MDX-Net parameters
        val nFft = 7680
        val hopLength = 1024
        val dimF = 3072
        val dimT = 256
        val chunkSamples = (dimT - 1) * hopLength + nFft // 268,800 samples (~6.09 seconds)
        val stepSamples = dimT * hopLength // 262,144 samples

        val vocalSamples = FloatArray(totalSamples)
        val instrumentalSamples = FloatArray(totalSamples)

        var sampleOffset = 0
        val totalChunks = kotlin.math.max(1, (totalSamples + stepSamples - 1) / stepSamples)
        var chunkIndex = 0

        while (sampleOffset < totalSamples && coroutineContext.isActive) {
            val currentChunkLen = minOf(chunkSamples, totalSamples - sampleOffset)
            val chunkInput = FloatArray(chunkSamples)
            System.arraycopy(samples, sampleOffset, chunkInput, 0, currentChunkLen)

            // Compute STFT via C++ Native Bridge
            val stftPlanes = NativeBridge.computeMdxStftNative(
                samples = chunkInput,
                nFft = nFft,
                hopLength = hopLength,
                dimF = dimF,
                dimT = dimT
            )

            // Run ONNX Session Inference
            // Shape: [1, 4, 3072, 256]
            val buffer = FloatBuffer.wrap(stftPlanes)
            val shape = longArrayOf(1, 4, dimF.toLong(), dimT.toLong())
            val tensor = OnnxTensor.createTensor(env, buffer, shape)

            val results = session.run(mapOf(inputName to tensor))
            val outTensor = results.get(0) as OnnxTensor
            val outBuffer = outTensor.floatBuffer

            val outPlanes = FloatArray(outBuffer.remaining())
            outBuffer.get(outPlanes)

            results.close()
            tensor.close()

            // Inverse STFT via C++ Native Bridge
            val chunkVocal = NativeBridge.computeMdxIstftNative(
                stftPlanes = outPlanes,
                totalOutputSamples = currentChunkLen,
                nFft = nFft,
                hopLength = hopLength,
                dimF = dimF,
                dimT = dimT
            )

            // Vocals & Instrumental (Phase cancellation: Inst = Mix - Vocal)
            val copyLen = minOf(chunkVocal.size, totalSamples - sampleOffset)
            for (i in 0 until copyLen) {
                val mixVal = samples[sampleOffset + i]
                val vocVal = chunkVocal[i].coerceIn(-1.0f, 1.0f)
                val instVal = (mixVal - vocVal).coerceIn(-1.0f, 1.0f)

                vocalSamples[sampleOffset + i] = vocVal
                instrumentalSamples[sampleOffset + i] = instVal
            }

            sampleOffset += stepSamples
            chunkIndex++
            val progress = 0.10f + (chunkIndex.toFloat() / totalChunks.toFloat()) * 0.80f
            onProgress(progress.coerceIn(0.10f, 0.90f))
        }

        if (!coroutineContext.isActive) {
            outputVocalFile.delete()
            outputInstrumentalFile.delete()
            throw kotlinx.coroutines.CancellationException("Stem separation cancelled by user")
        }

        onProgress(0.92f)

        // 4. Save both stems as validated WAV
        WavIoHelper.writeWav(outputVocalFile, AudioData(vocalSamples, sampleRate, 1))
        onProgress(0.96f)

        WavIoHelper.writeWav(outputInstrumentalFile, AudioData(instrumentalSamples, sampleRate, 1))
        onProgress(1.0f)

        val durationSec = (totalSamples / sampleRate).coerceAtLeast(1)
        StemSeparationResult(
            vocalFile = outputVocalFile,
            instrumentalFile = outputInstrumentalFile,
            durationSeconds = durationSec,
            sampleRate = sampleRate
        )
    }

    fun release() {
        try {
            ortSession?.close()
        } catch (_: Exception) {}
        ortSession = null
        ortEnv = null
    }
}
