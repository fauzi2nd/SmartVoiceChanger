package com.smartvoicechanger.app.audio

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AudioData(
    val samples: FloatArray, // Normalized float [-1.0f, 1.0f]
    val sampleRate: Int,
    val channels: Int
)

object WavIoHelper {

    fun readWav(inputStream: InputStream): AudioData {
        val bytes = inputStream.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read RIFF header
        val riff = ByteArray(4)
        buffer.get(riff)
        val riffStr = String(riff)
        if (riffStr != "RIFF") throw IllegalArgumentException("Not a valid RIFF file: $riffStr")

        val fileSize = buffer.int
        val wave = ByteArray(4)
        buffer.get(wave)
        val waveStr = String(wave)
        if (waveStr != "WAVE") throw IllegalArgumentException("Not a valid WAVE file: $waveStr")

        var sampleRate = 44100
        var channels = 1
        var bitsPerSample = 16
        var audioFormat = 1 // 1 = PCM

        var dataOffset = -1
        var dataSize = 0

        while (buffer.hasRemaining()) {
            val chunkId = ByteArray(4)
            buffer.get(chunkId)
            val chunkStr = String(chunkId)
            val chunkSize = buffer.int

            when (chunkStr) {
                "fmt " -> {
                    audioFormat = buffer.short.toInt()
                    channels = buffer.short.toInt()
                    sampleRate = buffer.int
                    val byteRate = buffer.int
                    val blockAlign = buffer.short
                    bitsPerSample = buffer.short.toInt()
                    // Skip remaining fmt bytes if any
                    if (chunkSize > 16) {
                        buffer.position(buffer.position() + (chunkSize - 16))
                    }
                }
                "data" -> {
                    dataOffset = buffer.position()
                    dataSize = chunkSize
                    break
                }
                else -> {
                    buffer.position(buffer.position() + chunkSize)
                }
            }
        }

        if (dataOffset == -1) throw IllegalArgumentException("No data chunk found in WAV")

        buffer.position(dataOffset)
        val sampleCount = dataSize / (bitsPerSample / 8) / channels
        val samples = FloatArray(sampleCount)

        when (bitsPerSample) {
            16 -> {
                for (i in 0 until sampleCount) {
                    var sum = 0f
                    for (ch in 0 until channels) {
                        val s = buffer.short
                        sum += s / 32768.0f
                    }
                    samples[i] = sum / channels // Downmix to mono
                }
            }
            32 -> {
                if (audioFormat == 3) { // IEEE Float
                    for (i in 0 until sampleCount) {
                        var sum = 0f
                        for (ch in 0 until channels) {
                            sum += buffer.float
                        }
                        samples[i] = sum / channels
                    }
                } else {
                    for (i in 0 until sampleCount) {
                        var sum = 0f
                        for (ch in 0 until channels) {
                            val s = buffer.int
                            sum += s / 2147483648.0f
                        }
                        samples[i] = sum / channels
                    }
                }
            }
            else -> {
                throw UnsupportedOperationException("Bits per sample $bitsPerSample not supported yet")
            }
        }

        return AudioData(samples, sampleRate, 1)
    }

    fun writeWav(file: File, audio: AudioData) {
        val totalAudioLen = audio.samples.size * 2 // 16-bit PCM = 2 bytes per sample
        val totalDataLen = totalAudioLen + 36
        val sampleRate = audio.sampleRate
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16) // Subchunk1Size for PCM
        buf.putShort(1.toShort()) // AudioFormat 1 = PCM
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort((channels * 2).toShort()) // BlockAlign
        buf.putShort(16.toShort()) // BitsPerSample
        buf.put("data".toByteArray())
        buf.putInt(totalAudioLen)

        FileOutputStream(file).use { out ->
            out.write(header)
            val pcmBuf = ByteArray(audio.samples.size * 2)
            val pcmWrap = ByteBuffer.wrap(pcmBuf).order(ByteOrder.LITTLE_ENDIAN)
            for (s in audio.samples) {
                val clamped = s.coerceIn(-1.0f, 1.0f)
                val pcmShort = (clamped * 32767.0f).toInt().toShort()
                pcmWrap.putShort(pcmShort)
            }
            out.write(pcmBuf)
        }
    }
}
