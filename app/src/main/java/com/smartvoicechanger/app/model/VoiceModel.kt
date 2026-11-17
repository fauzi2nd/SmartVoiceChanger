package com.smartvoicechanger.app.model

import java.io.File

data class VoiceModel(
    val id: String,
    val name: String,
    val modelFile: File,
    val indexFile: File? = null,
    val sampleRate: Int = 40000,
    val isV2: Boolean = true
)

object VoiceModelManager {
    fun listModels(modelsDir: File): List<VoiceModel> {
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val files = modelsDir.listFiles { f -> f.extension.equals("onnx", ignoreCase = true) } ?: emptyArray()
        return files.map { file ->
            val index = File(file.parentFile, "${file.nameWithoutExtension}.index")
            VoiceModel(
                id = file.nameWithoutExtension,
                name = file.nameWithoutExtension.replace('_', ' ').replaceFirstChar { it.uppercase() },
                modelFile = file,
                indexFile = if (index.exists()) index else null
            )
        }
    }
}
