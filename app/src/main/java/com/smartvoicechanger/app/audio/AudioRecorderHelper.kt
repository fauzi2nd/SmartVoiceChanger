package com.smartvoicechanger.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance, clean PCM WAV recorder for Voice Input.
 * Records at 44.1 kHz, 16-bit Mono PCM directly to disk.
 */
class AudioRecorderHelper {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    suspend fun startRecording(outputWavFile: File) = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true

        val buffer = ShortArray(1024)
        FileOutputStream(outputWavFile).use { fos ->
            // Write placeholder 44-byte WAV header
            fos.write(ByteArray(44))

            var totalAudioLen = 0

            while (isRecording && audioRecord != null) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val byteBuffer = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until read) {
                        byteBuffer.putShort(buffer[i])
                    }
                    val bytes = byteBuffer.array()
                    fos.write(bytes)
                    totalAudioLen += bytes.size
                }
            }

            // Write final WAV header with actual data length
            writeWavHeader(outputWavFile, totalAudioLen, sampleRate, 1, 16)
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
        }
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size
            header.putShort(1.toShort()) // AudioFormat (1 = PCM)
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort((channels * bitsPerSample / 8).toShort()) // BlockAlign
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(totalAudioLen)

            raf.write(header.array())
        }
    }
}
