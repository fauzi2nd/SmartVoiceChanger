package com.smartvoicechanger.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.smartvoicechanger.app.NativeBridge
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
        onProgress: (Float) -> Unit
    ): AudioData = withContext(Dispatchers.Default) {
        onProgress(0.05f)

        // 1. Read input WAV
        val inputAudio = inputFile.inputStream().use {
            WavIoHelper.readWav(it)
        }
        onProgress(0.25f)

        // 2. Pitch shifting via high-performance native C++ DSP
        val shiftedSamples = if (kotlin.math.abs(pitchSemitones) > 0.01f) {
            NativeBridge.pitchShiftNative(inputAudio.samples, inputAudio.sampleRate, pitchSemitones)
        } else {
            inputAudio.samples
        }
        onProgress(0.60f)

        // 3. Resample to standard target (44.1 kHz or 40 kHz RVC)
        val targetRate = 44100
        val processedSamples = if (inputAudio.sampleRate != targetRate) {
            NativeBridge.resampleNative(shiftedSamples, inputAudio.sampleRate, targetRate)
        } else {
            shiftedSamples
        }
        onProgress(0.85f)

        val outputAudio = AudioData(processedSamples, targetRate, 1)

        // 4. Save to output WAV
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
