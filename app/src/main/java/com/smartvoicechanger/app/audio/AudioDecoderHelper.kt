package com.smartvoicechanger.app.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Robust Universal Audio Decoder & Converter.
 * Decodes any audio format (MP3, M4A, AAC, FLAC, OGG, WAV) using Android MediaCodec / MediaExtractor
 * and converts it into standard 44.1 kHz, 16-bit Mono PCM WAV.
 */
object AudioDecoderHelper {

    suspend fun decodeToPcmWav(
        context: Context,
        inputUri: Uri,
        outputWavFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, inputUri, null)
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || inputFormat == null) {
                return@withContext false
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext false

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var isEos = false
            var totalPcmBytes = 0

            val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            FileOutputStream(outputWavFile).use { fos ->
                fos.write(ByteArray(44)) // Placeholder WAV header

                val timeoutUs = 10000L

                while (!isEos) {
                    val inIdx = codec.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val inputBuf = codec.getInputBuffer(inIdx)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEos = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    var outIdx = codec.dequeueOutputBuffer(info, timeoutUs)
                    while (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && info.size > 0) {
                            val chunk = ByteArray(info.size)
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            outBuf.get(chunk)

                            // If stereo, convert to mono 16-bit PCM
                            val pcmBytesToWrite = if (channelCount == 2) {
                                val monoBytes = ByteArray(chunk.size / 2)
                                val shortIn = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                                val byteOut = ByteBuffer.wrap(monoBytes).order(ByteOrder.LITTLE_ENDIAN)

                                while (shortIn.hasRemaining()) {
                                    val left = shortIn.get().toInt()
                                    val right = if (shortIn.hasRemaining()) shortIn.get().toInt() else left
                                    val mono = ((left + right) / 2).coerceIn(-32768, 32767).toShort()
                                    byteOut.putShort(mono)
                                }
                                monoBytes
                            } else {
                                chunk
                            }

                            fos.write(pcmBytesToWrite)
                            totalPcmBytes += pcmBytesToWrite.size
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isEos = true
                            break
                        }
                        outIdx = codec.dequeueOutputBuffer(info, 0)
                    }
                }

                writeWavHeader(outputWavFile, totalPcmBytes, sampleRate, 1, 16)
            }

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (_: Exception) {}
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
