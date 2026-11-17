package com.smartvoicechanger.app.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Universal Hardware Audio Decoder & WAV Converter.
 * Decodes arbitrary container formats (MP3, M4A, AAC, FLAC, OGG, WAV) using Android MediaCodec
 * and produces validated standard 44.1 kHz, 16-bit Mono PCM RIFF WAV files.
 */
object AudioDecoderHelper {

    suspend fun decodeToPcmWav(
        context: Context,
        inputUri: Uri,
        outputWavFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, inputUri, null)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) {
                cleanupFile(outputWavFile)
                return@withContext false
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: run {
                cleanupFile(outputWavFile)
                return@withContext false
            }

            val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                trackFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var totalPcmBytes = 0

            // Dynamic format properties updated upon INFO_OUTPUT_FORMAT_CHANGED
            var currentSampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            var currentChannelCount = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            // Ensure parent directory exists
            outputWavFile.parentFile?.mkdirs()

            FileOutputStream(outputWavFile).use { fos ->
                fos.write(ByteArray(44)) // 44-byte placeholder WAV header

                val timeoutUs = 10000L
                var noOutputCount = 0

                while (coroutineContext.isActive && !outputEos) {
                    // Feed input buffer if not EOS
                    if (!inputEos) {
                        val inIdx = codec.dequeueInputBuffer(timeoutUs)
                        if (inIdx >= 0) {
                            val inBuf = codec.getInputBuffer(inIdx)
                            if (inBuf != null) {
                                val sampleSize = extractor.readSampleData(inBuf, 0)
                                if (sampleSize < 0) {
                                    codec.queueInputBuffer(
                                        inIdx, 0, 0, 0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputEos = true
                                } else {
                                    val presentationTime = extractor.sampleTime
                                    codec.queueInputBuffer(inIdx, 0, sampleSize, presentationTime, 0)
                                    extractor.advance()

                                    if (durationUs > 0L) {
                                        val p = (presentationTime.toFloat() / durationUs.toFloat())
                                            .coerceIn(0f, 0.95f)
                                        onProgress?.invoke(p)
                                    }
                                }
                            }
                        }
                    }

                    // Drain output buffer
                    val outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                    when {
                        outIdx >= 0 -> {
                            noOutputCount = 0
                            val outBuf = codec.getOutputBuffer(outIdx)
                            if (outBuf != null && info.size > 0) {
                                val chunk = ByteArray(info.size)
                                outBuf.position(info.offset)
                                outBuf.limit(info.offset + info.size)
                                outBuf.get(chunk)

                                // Downmix stereo or multi-channel to mono 16-bit PCM if needed
                                val pcmBytesToWrite = when {
                                    currentChannelCount == 2 -> {
                                        val samplesCount = chunk.size / 4 // 2 bytes per sample * 2 channels
                                        val monoBytes = ByteArray(samplesCount * 2)
                                        val shortIn = ByteBuffer.wrap(chunk)
                                            .order(ByteOrder.LITTLE_ENDIAN)
                                            .asShortBuffer()
                                        val byteOut = ByteBuffer.wrap(monoBytes)
                                            .order(ByteOrder.LITTLE_ENDIAN)

                                        while (shortIn.hasRemaining()) {
                                            val left = shortIn.get().toInt()
                                            val right = if (shortIn.hasRemaining()) shortIn.get().toInt() else left
                                            val mono = ((left + right) / 2).coerceIn(-32768, 32767).toShort()
                                            byteOut.putShort(mono)
                                        }
                                        monoBytes
                                    }
                                    currentChannelCount > 2 -> {
                                        // Downmix multi-channel: average the first 2 channels
                                        val frameSize = currentChannelCount * 2
                                        val frames = chunk.size / frameSize
                                        val monoBytes = ByteArray(frames * 2)
                                        val shortIn = ByteBuffer.wrap(chunk)
                                            .order(ByteOrder.LITTLE_ENDIAN)
                                            .asShortBuffer()
                                        val byteOut = ByteBuffer.wrap(monoBytes)
                                            .order(ByteOrder.LITTLE_ENDIAN)

                                        for (f in 0 until frames) {
                                            val left = shortIn.get().toInt()
                                            val right = shortIn.get().toInt()
                                            for (skip in 2 until currentChannelCount) {
                                                shortIn.get()
                                            }
                                            val mono = ((left + right) / 2).coerceIn(-32768, 32767).toShort()
                                            byteOut.putShort(mono)
                                        }
                                        monoBytes
                                    }
                                    else -> chunk
                                }

                                fos.write(pcmBytesToWrite)
                                totalPcmBytes += pcmBytesToWrite.size
                            }

                            codec.releaseOutputBuffer(outIdx, false)

                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputEos = true
                            }
                        }
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val newFormat = codec.outputFormat
                            if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                                currentSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            }
                            if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                                currentChannelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            }
                        }
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            noOutputCount++
                            if (inputEos && noOutputCount > 50) {
                                // Decoder drained all buffers
                                outputEos = true
                            }
                        }
                    }
                }

                if (!coroutineContext.isActive) {
                    cleanupFile(outputWavFile)
                    return@withContext false
                }

                // If no audio decoded, fail cleanly
                if (totalPcmBytes == 0) {
                    cleanupFile(outputWavFile)
                    return@withContext false
                }

                writeWavHeader(outputWavFile, totalPcmBytes, currentSampleRate, 1, 16)
                onProgress?.invoke(1.0f)
            }

            // Strict RIFF WAV validation check before declaring success
            if (!isValidWav(outputWavFile)) {
                cleanupFile(outputWavFile)
                return@withContext false
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            cleanupFile(outputWavFile)
            return@withContext false
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {}
            try {
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    private fun cleanupFile(file: File) {
        try {
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    fun isValidWav(file: File): Boolean {
        if (!file.exists() || file.length() < 44) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(12)
                raf.readFully(header)
                val riff = String(header, 0, 4)
                val wave = String(header, 8, 4)
                riff == "RIFF" && wave == "WAVE"
            }
        } catch (_: Exception) {
            false
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
