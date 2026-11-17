package com.smartvoicechanger.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Service to manage downloading and local validation of Neural Audio Models.
 * Specifically handles the UVR-MDX-NET-Voc_FT (Vocals specialist) model.
 */
object ModelDownloadService {

    const val MDX_MODEL_FILENAME = "UVR-MDX-NET-Voc_FT.onnx"
    const val MDX_MODEL_URL = "https://huggingface.co/Anjok/model-repo/resolve/048ddcf9a0db28453d71943c1e18a1e25291a642/UVR-MDX-NET-Voc_FT.onnx"
    const val MDX_MODEL_SHA256 = "534b2070fcc7df514b13ef660dc8cbb328679c2374d04354a5c42bb14ecce111"
    const val MDX_MODEL_SIZE_BYTES = 66762490L // ~63.7 MB
    const val MDX_MODEL_SIZE_LABEL = "63.7 MB"
    const val MDX_MODEL_LICENSE = "MIT License (Ultimate Vocal Remover / Anjok)"

    fun getModelFile(context: Context): File {
        val dir = File(context.filesDir, "models/stem")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, MDX_MODEL_FILENAME)
    }

    fun isModelReady(context: Context): Boolean {
        val file = getModelFile(context)
        if (!file.exists() || file.length() != MDX_MODEL_SIZE_BYTES) {
            return false
        }
        return true
    }

    suspend fun downloadModel(
        context: Context,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getModelFile(context)
        val tempFile = File(targetFile.parentFile, "${MDX_MODEL_FILENAME}.tmp")

        try {
            if (tempFile.exists()) tempFile.delete()

            val url = URL(MDX_MODEL_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                tempFile.delete()
                return@withContext false
            }

            val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else MDX_MODEL_SIZE_BYTES
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            tempFile.delete()
                            return@withContext false
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                }
            }

            // Verify SHA-256 Hash
            val digest = MessageDigest.getInstance("SHA-256")
            tempFile.inputStream().use { input ->
                val buffer = ByteArray(32768)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }

            if (!calculatedHash.equals(MDX_MODEL_SHA256, ignoreCase = true)) {
                tempFile.delete()
                return@withContext false
            }

            // Move temp file to final location atomically
            if (targetFile.exists()) targetFile.delete()
            val renamed = tempFile.renameTo(targetFile)
            renamed
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (_: Exception) {}
            false
        }
    }
}
