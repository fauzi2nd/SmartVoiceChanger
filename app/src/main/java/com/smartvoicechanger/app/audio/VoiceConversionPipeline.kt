package com.smartvoicechanger.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.smartvoicechanger.app.NativeBridge
import com.smartvoicechanger.app.inference.RvcOnnxEngine
import com.smartvoicechanger.app.model.RvcModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class VoiceConversionPipeline(private val context: Context) {

    private var activeTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)

    suspend fun convertAudio(
        inputFile: File,
        outputFile: File,
        pitchSemitones: Float,
        modelId: String = "aoi_cyber_girl",
        instrumentalFile: File? = null,
        onProgress: (Float) -> Unit
    ): AudioData = withContext(Dispatchers.Default) {
        onProgress(0.05f)
        val inputAudio = inputFile.inputStream().use { WavIoHelper.readWav(it) }

        // RVC requires three compatible ONNX models; never feed raw waveform
        // directly into the synthesizer or treat demo placeholder assets as models.
        val engine = RvcOnnxEngine(
            synthFile = RvcModelStore.file(context, RvcModelStore.SYNTH),
            hubertFile = RvcModelStore.file(context, RvcModelStore.HUBERT),
            rmvpeFile = RvcModelStore.file(context, RvcModelStore.RMVPE),
        )
        val converted = engine.use { runtime ->
            runtime.convert(
                inputSamples = inputAudio.samples,
                inputSampleRate = inputAudio.sampleRate,
                pitchSemitones = pitchSemitones.toInt(),
            ) { progress ->
                onProgress(0.10f + progress * 0.70f)
            }
        }
        val neuralSamples = converted.samples
        val targetRate = converted.sampleRate
        onProgress(0.82f)

        // 5. Audio Remixer Stage: align the instrumental stem to the synthesizer output rate.
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
