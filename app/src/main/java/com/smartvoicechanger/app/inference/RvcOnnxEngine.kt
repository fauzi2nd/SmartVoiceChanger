package com.smartvoicechanger.app.inference

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.smartvoicechanger.app.NativeBridge
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Standard offline RVC inference path:
 * 44.1 kHz PCM -> 16 kHz -> ContentVec/HuBERT -> optional RMVPE ->
 * 50 fps to 100 fps feature alignment -> RVC synthesizer.
 *
 * Model files are expected in the app-private RvcModelStore directory.
 * The synthesizer must be exported with voice-changer's embedded "metadata" JSON.
 */
class RvcOnnxEngine(
    private val synthFile: File,
    private val hubertFile: File,
    private val rmvpeFile: File,
) : Closeable {

    data class ConversionResult(val samples: FloatArray, val sampleRate: Int)

    private val env = OrtEnvironment.getEnvironment()
    private var synthSession: OrtSession? = null
    private var hubertSession: OrtSession? = null
    private var rmvpeSession: OrtSession? = null

    data class SynthMetadata(
        val sampleRate: Int,
        val f0: Boolean,
        val embeddingChannels: Int,
        val embedder: String,
        val outputLayer: Int,
        val useFinalProjection: Boolean,
    )

    fun convert(
        inputSamples: FloatArray,
        inputSampleRate: Int,
        pitchSemitones: Int,
        onProgress: (Float) -> Unit,
    ): ConversionResult {
        require(inputSamples.isNotEmpty()) { "Input audio is empty" }
        require(inputSampleRate > 0) { "Invalid input sample rate: $inputSampleRate" }
        require(inputSamples.size.toDouble() / inputSampleRate <= 60.0) {
            "This mobile inference build currently supports up to 60 seconds per conversion. Split longer audio into shorter clips to avoid HuBERT memory exhaustion."
        }
        require(synthFile.isFile && synthFile.length() > 0L) { "RVC synthesizer model is missing" }
        require(hubertFile.isFile && hubertFile.length() > 0L) { "HuBERT/ContentVec ONNX model is missing" }

        onProgress(0.02f)
        val synth = openSession(synthFile).also { synthSession = it }
        val metadata = readMetadata(synth)
        validateSynthInputs(synth, metadata)
        require(metadata.f0 || pitchSemitones == 0) {
            "Pitch shifting requires an F0-enabled RVC synthesizer. Set pitch to 0 or import an F0 model."
        }
        onProgress(0.08f)

        val hubert = openSession(hubertFile).also { hubertSession = it }
        val hubertOutput = selectHubertOutput(metadata)
        require("audio" in hubert.inputInfo) {
            "Unsupported HuBERT ONNX input signature. Expected input named 'audio'; found ${hubert.inputInfo.keys}."
        }
        require(hubertOutput in hubert.outputInfo) {
            "HuBERT model does not expose '$hubertOutput'. Available outputs: ${hubert.outputInfo.keys}."
        }
        val hubertInputType = (hubert.inputInfo["audio"]?.info as? TensorInfo)?.type
        val hubertOutputType = (hubert.outputInfo[hubertOutput]?.info as? TensorInfo)?.type
        require(hubertInputType == OnnxJavaType.FLOAT && hubertOutputType == OnnxJavaType.FLOAT) {
            "HuBERT ONNX must use FP32 audio input and feature output; found input=$hubertInputType, output=$hubertOutputType."
        }
        onProgress(0.12f)

        val audio16k = if (inputSampleRate == 16000) inputSamples
            else NativeBridge.resampleNative(inputSamples, inputSampleRate, 16000)
        require(audio16k.isNotEmpty()) { "Resampling to 16 kHz produced no samples" }

        val features = extractHubert(hubert, hubertOutput, audio16k)
        require(features.channels == metadata.embeddingChannels) {
            "HuBERT produced ${features.channels} channels, but synthesizer expects ${metadata.embeddingChannels}. " +
                "Check whether the voice model is RVC v1 or v2 and select the matching ContentVec output."
        }
        onProgress(0.48f)

        var pitchCoarse: LongArray? = null
        var pitchFine: FloatArray? = null
        if (metadata.f0) {
            require(rmvpeFile.isFile && rmvpeFile.length() > 0L) {
                "This synthesizer requires F0. Import a compatible RMVPE ONNX model."
            }
            val rmvpe = openSession(rmvpeFile).also { rmvpeSession = it }
            val pitch = extractPitch(rmvpe, audio16k, pitchSemitones)
            pitchCoarse = pitch.first
            pitchFine = pitch.second
        }
        onProgress(0.66f)

        val upsampledFrames = features.frames * 2
        val targetFrames = if (metadata.f0) minOf(upsampledFrames, pitchFine!!.size) else upsampledFrames
        require(targetFrames > 0) { "Feature extraction returned no usable frames" }
        val alignedFeatures = FloatArray(targetFrames * features.channels)
        for (frame in 0 until targetFrames) {
            val sourceFrame = frame / 2
            System.arraycopy(
                features.values,
                sourceFrame * features.channels,
                alignedFeatures,
                frame * features.channels,
                features.channels,
            )
        }
        val alignedPitch = pitchCoarse?.copyOf(targetFrames)
        val alignedPitchFine = pitchFine?.copyOf(targetFrames)
        onProgress(0.76f)

        val converted = runSynthesizer(
            synth = synth,
            metadata = metadata,
            features = alignedFeatures,
            frames = targetFrames,
            pitchCoarse = alignedPitch,
            pitchFine = alignedPitchFine,
        )
        require(converted.isNotEmpty()) { "RVC synthesizer returned empty audio" }
        onProgress(1f)
        return ConversionResult(converted, metadata.sampleRate)
    }

    private data class FeatureData(val values: FloatArray, val frames: Int, val channels: Int)

    private fun extractHubert(session: OrtSession, outputName: String, audio: FloatArray): FeatureData {
        val tensor = floatTensor(audio, longArrayOf(1L, audio.size.toLong()))
        try {
            session.run(mapOf("audio" to tensor), setOf(outputName)).use { result ->
                val output = result.iterator().next().value as OnnxTensor
                val shape = (output.info as TensorInfo).shape
                require(shape.size == 3 && shape[0] == 1L && shape[1] > 0L && shape[2] > 0L) {
                    "Unexpected HuBERT output shape: ${shape.contentToString()}"
                }
                val values = output.floatBuffer.let { buffer ->
                    FloatArray(buffer.remaining()).also { buffer.get(it) }
                }
                return FeatureData(values, shape[1].toInt(), shape[2].toInt())
            }
        } finally {
            tensor.close()
        }
    }

    private fun extractPitch(
        session: OrtSession,
        audio: FloatArray,
        semitones: Int,
    ): Pair<LongArray, FloatArray> {
        require("waveform" in session.inputInfo && "threshold" in session.inputInfo) {
            "Unsupported RMVPE ONNX inputs. Expected 'waveform' and 'threshold'; found ${session.inputInfo.keys}."
        }
        val waveform = floatTensor(audio, longArrayOf(1L, audio.size.toLong()))
        val threshold = floatTensor(floatArrayOf(0.3f), longArrayOf(1L))
        try {
            session.run(mapOf("waveform" to waveform, "threshold" to threshold)).use { result ->
                val output = result.iterator().next().value as OnnxTensor
                val buffer = output.floatBuffer
                val raw = FloatArray(buffer.remaining()).also { buffer.get(it) }
                val factor = 2.0.pow(semitones / 12.0).toFloat()
                val shifted = FloatArray(raw.size) { index ->
                    val value = raw[index] * factor
                    if (value.isFinite()) value else 0f
                }
                return Pair(quantizePitch(shifted), shifted)
            }
        } finally {
            waveform.close()
            threshold.close()
        }
    }

    private fun runSynthesizer(
        synth: OrtSession,
        metadata: SynthMetadata,
        features: FloatArray,
        frames: Int,
        pitchCoarse: LongArray?,
        pitchFine: FloatArray?,
    ): FloatArray {
        val inputs = LinkedHashMap<String, OnnxTensor>()
        try {
            inputs["feats"] = floatTensor(features, longArrayOf(1L, frames.toLong(), metadata.embeddingChannels.toLong()))
            inputs["p_len"] = longTensor(longArrayOf(frames.toLong()), longArrayOf(1L))
            inputs["sid"] = longTensor(longArrayOf(0L), longArrayOf(1L))
            if (metadata.f0) {
                inputs["pitch"] = longTensor(requireNotNull(pitchCoarse), longArrayOf(1L, frames.toLong()))
                inputs["pitchf"] = floatTensor(requireNotNull(pitchFine), longArrayOf(1L, frames.toLong()))
            }
            synth.run(inputs, setOf("audio")).use { result ->
                val output = result.iterator().next().value as OnnxTensor
                val buffer = output.floatBuffer
                return FloatArray(buffer.remaining()) { index ->
                    buffer.get().coerceIn(-1f, 1f)
                }
            }
        } finally {
            inputs.values.forEach { runCatching { it.close() } }
        }
    }

    private fun readMetadata(session: OrtSession): SynthMetadata {
        val raw = session.metadata.customMetadata["metadata"]
            ?: throw IllegalArgumentException(
                "Synthesizer ONNX has no embedded 'metadata'. Export it with an RVC/voice-changer ONNX exporter that embeds samplingRate, f0, embChannels, embedder, embOutputLayer and useFinalProj."
            )
        val json = JSONObject(raw)
        val metadata = SynthMetadata(
            sampleRate = json.getInt("samplingRate"),
            f0 = json.getBoolean("f0"),
            embeddingChannels = json.getInt("embChannels"),
            embedder = json.getString("embedder"),
            outputLayer = json.getInt("embOutputLayer"),
            useFinalProjection = json.optBoolean("useFinalProj", false),
        )
        require(metadata.sampleRate in 16000..96000) { "Unsupported synthesizer sample rate: ${metadata.sampleRate}" }
        require(metadata.embeddingChannels in setOf(256, 768)) { "Unsupported embedding dimension: ${metadata.embeddingChannels}" }
        return metadata
    }

    private fun selectHubertOutput(metadata: SynthMetadata): String = when {
        metadata.outputLayer == 12 && !metadata.useFinalProjection -> "unit12"
        metadata.outputLayer == 9 && metadata.useFinalProjection -> "units9"
        metadata.outputLayer == 12 && metadata.useFinalProjection -> "unit12s"
        else -> throw IllegalArgumentException(
            "Unsupported HuBERT configuration: layer=${metadata.outputLayer}, finalProj=${metadata.useFinalProjection}."
        )
    }

    private fun validateSynthInputs(session: OrtSession, metadata: SynthMetadata) {
        val required = if (metadata.f0) setOf("feats", "p_len", "pitch", "pitchf", "sid")
            else setOf("feats", "p_len", "sid")
        val missing = required - session.inputInfo.keys
        require(missing.isEmpty()) {
            "Synthesizer input signature is incompatible. Missing: $missing; available: ${session.inputInfo.keys}."
        }
        require("audio" in session.outputInfo) {
            "Synthesizer ONNX must expose an 'audio' output; found ${session.outputInfo.keys}."
        }
        val featureType = (session.inputInfo["feats"]?.info as? TensorInfo)?.type
        require(featureType == OnnxJavaType.FLOAT) {
            "Synthesizer feats input must be FP32 in this build; found $featureType. FP16 synthesizers are not supported yet."
        }
        val audioType = (session.outputInfo["audio"]?.info as? TensorInfo)?.type
        require(audioType == OnnxJavaType.FLOAT) {
            "Synthesizer audio output must be FP32 in this build; found $audioType. FP16 outputs are not supported yet."
        }
    }

    private fun quantizePitch(pitch: FloatArray): LongArray {
        val minMel = 1127f * ln(1f + 50f / 700f)
        val maxMel = 1127f * ln(1f + 1100f / 700f)
        val span = maxMel - minMel
        return LongArray(pitch.size) { index ->
            val frequency = pitch[index]
            val value = if (frequency > 0f) {
                val mel = 1127f * ln(1f + frequency / 700f)
                ((mel - minMel) * 254f / span + 1f).coerceIn(1f, 255f)
            } else 1f
            value.roundToInt().toLong()
        }
    }

    private fun openSession(file: File): OrtSession {
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        return try {
            env.createSession(file.absolutePath, options)
        } finally {
            options.close()
        }
    }

    private fun floatTensor(values: FloatArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(env, FloatBuffer.wrap(values), shape)

    private fun longTensor(values: LongArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(env, LongBuffer.wrap(values), shape)

    override fun close() {
        runCatching { rmvpeSession?.close() }
        runCatching { hubertSession?.close() }
        runCatching { synthSession?.close() }
        rmvpeSession = null
        hubertSession = null
        synthSession = null
    }
}
