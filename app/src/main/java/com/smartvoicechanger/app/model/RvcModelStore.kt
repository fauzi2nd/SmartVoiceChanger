package com.smartvoicechanger.app.model

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Stores the three ONNX assets required by a standard F0-enabled RVC model.
 * Files are copied from the Android document picker into app-private storage
 * so ONNX Runtime can open them by filesystem path (and memory-map large files).
 */
object RvcModelStore {
    const val SYNTH = "synth.onnx"
    const val HUBERT = "hubert.onnx"
    const val RMVPE = "rmvpe.onnx"

    fun directory(context: Context): File =
        File(context.filesDir, "models/rvc").apply { if (!exists()) mkdirs() }

    fun file(context: Context, modelName: String): File =
        File(directory(context), modelName)

    fun isReady(context: Context): Boolean =
        listOf(SYNTH, HUBERT, RMVPE).all { file(context, it).let { model -> model.isFile && model.length() > 0L } }

    fun status(context: Context): Map<String, Boolean> =
        listOf(SYNTH, HUBERT, RMVPE).associateWith { name ->
            file(context, name).let { it.isFile && it.length() > 0L }
        }

    fun importModel(context: Context, uri: Uri, modelName: String): File {
        require(modelName in listOf(SYNTH, HUBERT, RMVPE)) { "Unknown RVC model slot: $modelName" }
        val target = file(context, modelName)
        val temporary = File(target.parentFile, "$modelName.importing")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output -> input.copyTo(output) }
            } ?: error("Cannot open selected model file")
            require(temporary.length() > 0L) { "Selected model file is empty" }

            // Validate that imported file is not audio (e.g. WAV/MP3) and has valid ONNX header
            val headerBytes = ByteArray(16)
            temporary.inputStream().use { it.read(headerBytes) }
            val headerString = String(headerBytes, Charsets.US_ASCII)
            if (headerString.startsWith("RIFF") || headerString.startsWith("ID3") || headerString.startsWith("OggS")) {
                error("File is an audio file, not an ONNX model file (.onnx).")
            }
            if (headerBytes.isNotEmpty() && headerBytes[0] != 0x08.toByte()) {
                error("Invalid ONNX model file. Protobuf header mismatch.")
            }

            if (target.exists() && !target.delete()) error("Cannot replace existing model file")
            if (!temporary.renameTo(target)) error("Cannot finalize imported model file")
            return target
        } catch (t: Throwable) {
            temporary.delete()
            throw t
        }
    }
}
