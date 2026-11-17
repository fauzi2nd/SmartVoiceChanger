package com.smartvoicechanger.app.storage

import android.content.Context
import com.smartvoicechanger.app.ui.ConversionHistoryItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HistoryStorageHelper {

    private const val FILE_NAME = "conversion_history.json"

    fun loadHistory(context: Context): List<ConversionHistoryItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ConversionHistoryItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val audioPath = obj.optString("audioFilePath", "")
                val audioFile = if (audioPath.isNotEmpty()) File(audioPath) else null

                list.add(
                    ConversionHistoryItem(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        fileName = obj.optString("fileName", "Unknown"),
                        modelName = obj.optString("modelName", "Neural Model"),
                        duration = obj.optString("duration", "00:00"),
                        timestamp = obj.optString("timestamp", "Earlier"),
                        latencyMs = obj.optDouble("latencyMs", 0.0),
                        audioFile = if (audioFile != null && audioFile.exists()) audioFile else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveHistory(context: Context, items: List<ConversionHistoryItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("fileName", item.fileName)
                    put("modelName", item.modelName)
                    put("duration", item.duration)
                    put("timestamp", item.timestamp)
                    put("latencyMs", item.latencyMs)
                    put("audioFilePath", item.audioFile?.absolutePath ?: "")
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteItem(context: Context, id: String) {
        val current = loadHistory(context).toMutableList()
        val itemToDelete = current.find { it.id == id }
        itemToDelete?.audioFile?.let { file ->
            if (file.exists()) file.delete()
        }
        current.removeAll { it.id == id }
        saveHistory(context, current)
    }
}
