package com.smartvoicechanger.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.smartvoicechanger.app.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean

class VoiceConversionPipeline(private val context: Context) {

    private var activeTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)
    private var ortEnv: OrtEnvironment? = null
    private var cachedSession: OrtSession? = null
    private var cachedModelId: String? = null

    suspend fun convertAudio(
        inputFile: File,
        outputFile: File,
        pitchSemitones: Float,
        modelId: String = "aoi_cyber_girl",
        instrumentalFile: File? = null,
        onProgress: (Float) -> Unit
    ): AudioData = withContext(Dispatchers.Default) {
        onProgress(0.05f)

        // 1. Read input WAV
        val inputAudio = inputFile.inputStream().use {
            WavIoHelper.readWav(it)
        }
        onProgress(0.15f)

        // 2. Pitch shifting via high-performance native C++ DSP
        val shiftedSamples = if (kotlin.math.abs(pitchSemitones) > 0.01f) {
            NativeBridge.pitchShiftNative(inputAudio.samples, inputAudio.sampleRate, pitchSemitones)
        } else {
            inputAudio.samples
        }
        onProgress(0.35f)

        // 3. Resample to standard target (44.1 kHz)
        val targetRate = 44100
        val resampledSamples = if (inputAudio.sampleRate != targetRate) {
            NativeBridge.resampleNative(shiftedSamples, inputAudio.sampleRate, targetRate)
        } else {
            shiftedSamples
        }
        onProgress(0.50f)

        // 4. Neural Model Inference via ONNX Runtime Mobile (ARM NEON)
        val neuralSamples = runNeuralInference(resampledSamples, modelId) { p ->
            onProgress(0.50f + p * 0.30f)
        }
        onProgress(0.80f)

        // 5. Audio Remixer Stage: If instrumental stem is provided, combine converted vocals with instrumental
        val finalSamples = if (instrumentalFile != null && instrumentalFile.exists()) {
            val instAudio = instrumentalFile.inputStream().use { WavIoHelper.readWav(it) }
            val instResampled = if (instAudio.sampleRate != targetRate) {
                NativeBridge.resampleNative(instAudio.samples, instAudio.sampleRate, targetRate)
            } else {
                instAudio.samples
            }
            onProgress(0.90f)
            NativeBridge.mixStemsNative(neuralSamples, instResampled, vocalGain = 1.0f, instGain = 0.92f)
        } else {
            neuralSamples
        }

        onProgress(0.95f)
        val outputAudio = AudioData(finalSamples, targetRate, 1)

        // 6. Save to output WAV
        WavIoHelper.writeWav(outputFile, outputAudio)
        onProgress(1.0f)

        outputAudio
    }

    private suspend fun runNeuralInference(
        samples: FloatArray,
        modelId: String,
        onProgress: (Float) -> Unit
    ): FloatArray = withContext(Dispatchers.Default) {
        // Load model from assets if needed
        val modelFile = getOrExtractModel(modelId)
            ?: throw IllegalStateException("Target neural model '$modelId' could not be loaded from assets.")

        val env = ortEnv ?: OrtEnvironment.getEnvironment().also { ortEnv = it }
        val session = if (cachedModelId == modelId && cachedSession != null) {
            cachedSession!!
        } else {
            cachedSession?.close()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            val newSession = env.createSession(modelFile.absolutePath, opts)
            cachedSession = newSession
            cachedModelId = modelId
            newSession
        }

        // Run neural model inference in dynamic chunks to bound memory and provide smooth progress
        val chunkSize = 44100 // 1-second chunks
        val totalSamples = samples.size
        val outputSamples = FloatArray(totalSamples)
        var offset = 0

        val inputName = session.inputNames.iterator().next()

        while (offset < totalSamples) {
            val currentChunkSize = minOf(chunkSize, totalSamples - offset)
            val chunkBuffer = FloatBuffer.wrap(samples, offset, currentChunkSize)
            val inputShape = longArrayOf(1, 1, currentChunkSize.toLong())

            val tensor = OnnxTensor.createTensor(env, chunkBuffer, inputShape)
            val results = session.run(mapOf(inputName to tensor))

            val outTensor = results.get(0) as OnnxTensor
            val outBuffer = outTensor.floatBuffer
            outBuffer.get(outputSamples, offset, currentChunkSize)

            results.close()
            tensor.close()

            offset += currentChunkSize
            onProgress(offset.toFloat() / totalSamples.toFloat())
        }

        outputSamples
    }

    private fun getOrExtractModel(modelId: String): File? {
        // 1. Check if model exists in models/voice (e.g. saisho_utane.onnx)
        val voiceDir = File(context.filesDir, "models/voice")
        val voiceModelFile = File(voiceDir, "$modelId.onnx")
        if (voiceModelFile.exists() && voiceModelFile.length() > 0) {
            return voiceModelFile
        }

        // 2. Check if model exists in general models/ directory
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val targetFile = File(modelsDir, "$modelId.onnx")

        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile
        }

        // 3. Fallback to extracting from bundled assets if present
        return try {
            context.assets.open("models/$modelId.onnx").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (_: Exception) {
            null
        }
    }

    fun playAudio(audio: AudioData) {
        stopPlayback()

        try {
            val sampleRate = audio.sampleRate
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(bufferSize, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeTrack = track
            isPlaying.set(true)
            track.play()

            Thread {
                try {
                    val shortBuf = ShortArray(1024)
                    var offset = 0
                    while (offset < audio.samples.size && isPlaying.get() && activeTrack == track) {
                        val chunkSize = minOf(shortBuf.size, audio.samples.size - offset)
                        for (i in 0 until chunkSize) {
                            val s = audio.samples[offset + i].coerceIn(-1.0f, 1.0f)
                            shortBuf[i] = (s * 32767.0f).toInt().toShort()
                        }
                        track.write(shortBuf, 0, chunkSize)
                        offset += chunkSize
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        track.stop()
                        track.release()
                    } catch (_: Exception) {}
                    if (activeTrack == track) {
                        activeTrack = null
                        isPlaying.set(false)
                    }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        isPlaying.set(false)
        activeTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        activeTrack = null
    }
}
