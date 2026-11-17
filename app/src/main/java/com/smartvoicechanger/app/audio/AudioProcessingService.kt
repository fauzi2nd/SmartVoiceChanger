package com.smartvoicechanger.app.audio

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smartvoicechanger.app.MainActivity
import com.smartvoicechanger.app.R
import com.smartvoicechanger.app.storage.HistoryStorageHelper
import com.smartvoicechanger.app.ui.ConversionHistoryItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class AudioJobState {
    object Idle : AudioJobState()
    data class Running(
        val jobId: String,
        val stage: String,
        val progress: Float,
        val fileName: String,
        val modelName: String
    ) : AudioJobState()
    data class Completed(
        val jobId: String,
        val outputFile: File,
        val latencyMs: Double
    ) : AudioJobState()
    data class Error(
        val jobId: String,
        val message: String
    ) : AudioJobState()
    data class Cancelled(
        val jobId: String
    ) : AudioJobState()
}

class AudioProcessingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var activeJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "svc_audio_processing_channel"
        private const val NOTIFICATION_ID = 4001

        private const val ACTION_START_CONVERSION = "com.smartvoicechanger.app.action.START_CONVERSION"
        private const val ACTION_CANCEL = "com.smartvoicechanger.app.action.CANCEL"

        private const val EXTRA_INPUT_PATH = "extra_input_path"
        private const val EXTRA_OUTPUT_PATH = "extra_output_path"
        private const val EXTRA_TARGET_NAME = "extra_target_name"
        private const val EXTRA_MODEL_NAME = "extra_model_name"
        private const val EXTRA_MODEL_ID = "extra_model_id"
        private const val EXTRA_PITCH = "extra_pitch"
        private const val EXTRA_INST_PATH = "extra_inst_path"

        private val _jobState = MutableStateFlow<AudioJobState>(AudioJobState.Idle)
        val jobState: StateFlow<AudioJobState> = _jobState.asStateFlow()

        fun startConversion(
            context: Context,
            inputFile: File,
            outputFile: File,
            targetName: String,
            modelName: String,
            modelId: String,
            pitchSemitones: Float,
            instrumentalFile: File?
        ) {
            val intent = Intent(context, AudioProcessingService::class.java).apply {
                action = ACTION_START_CONVERSION
                putExtra(EXTRA_INPUT_PATH, inputFile.absolutePath)
                putExtra(EXTRA_OUTPUT_PATH, outputFile.absolutePath)
                putExtra(EXTRA_TARGET_NAME, targetName)
                putExtra(EXTRA_MODEL_NAME, modelName)
                putExtra(EXTRA_MODEL_ID, modelId)
                putExtra(EXTRA_PITCH, pitchSemitones)
                instrumentalFile?.let { putExtra(EXTRA_INST_PATH, it.absolutePath) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelCurrentJob(context: Context) {
            val intent = Intent(context, AudioProcessingService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CONVERSION -> {
                val inputPath = intent.getStringExtra(EXTRA_INPUT_PATH)
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: "Audio Take"
                val modelName = intent.getStringExtra(EXTRA_MODEL_NAME) ?: "Neural Voice"
                val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: "aoi_cyber_girl"
                val pitch = intent.getFloatExtra(EXTRA_PITCH, 0.0f)
                val instPath = intent.getStringExtra(EXTRA_INST_PATH)

                if (inputPath != null && outputPath != null) {
                    val inputFile = File(inputPath)
                    val outputFile = File(outputPath)
                    val instFile = instPath?.let { File(it) }

                    val notification = buildProgressNotification(targetName, 0f, "Starting conversion...")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    startProcessing(inputFile, outputFile, targetName, modelName, modelId, pitch, instFile)
                }
            }
            ACTION_CANCEL -> {
                cancelJob("Conversion cancelled by user.")
            }
        }
        return START_NOT_STICKY
    }

    private fun startProcessing(
        inputFile: File,
        outputFile: File,
        targetName: String,
        modelName: String,
        modelId: String,
        pitchSemitones: Float,
        instrumentalFile: File?
    ) {
        activeJob?.cancel()
        val jobId = System.currentTimeMillis().toString()

        activeJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            val pipeline = VoiceConversionPipeline(applicationContext)

            try {
                _jobState.value = AudioJobState.Running(
                    jobId = jobId,
                    stage = "Converting vocals & pitch...",
                    progress = 0.05f,
                    fileName = targetName,
                    modelName = modelName
                )

                val result = pipeline.convertAudio(
                    inputFile = inputFile,
                    outputFile = outputFile,
                    pitchSemitones = pitchSemitones,
                    modelId = modelId,
                    instrumentalFile = instrumentalFile
                ) { p ->
                    _jobState.value = AudioJobState.Running(
                        jobId = jobId,
                        stage = if (p >= 0.85f && instrumentalFile != null) "Remixing with instrumental..." else "Neural inference...",
                        progress = p,
                        fileName = targetName,
                        modelName = modelName
                    )
                    updateNotification(targetName, p)
                }

                val elapsedMs = (System.currentTimeMillis() - startTime).toDouble()
                val audioDurationSec = (result.samples.size / result.sampleRate).coerceAtLeast(1)
                val durationStr = String.format("%02d:%02d", audioDurationSec / 60, audioDurationSec % 60)

                // Persist to history atomically
                val newHistory = ConversionHistoryItem(
                    id = jobId,
                    fileName = targetName,
                    modelName = modelName,
                    duration = durationStr,
                    timestamp = "Just now",
                    latencyMs = elapsedMs,
                    audioFile = outputFile
                )
                val currentHistory = HistoryStorageHelper.loadHistory(applicationContext).toMutableList()
                currentHistory.add(0, newHistory)
                HistoryStorageHelper.saveHistory(applicationContext, currentHistory)

                _jobState.value = AudioJobState.Completed(
                    jobId = jobId,
                    outputFile = outputFile,
                    latencyMs = elapsedMs
                )

                showCompletedNotification(targetName, outputFile)
            } catch (e: CancellationException) {
                // Clean up partial output file
                try {
                    if (outputFile.exists()) outputFile.delete()
                } catch (_: Exception) {}
                _jobState.value = AudioJobState.Cancelled(jobId)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    if (outputFile.exists()) outputFile.delete()
                } catch (_: Exception) {}
                _jobState.value = AudioJobState.Error(jobId, e.localizedMessage ?: "Conversion failed.")
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    private fun cancelJob(reason: String) {
        activeJob?.cancel(CancellationException(reason))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartVoiceChanger Audio Studio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background neural conversion & stem separation progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(fileName: String, progress: Float, status: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = (progress * 100).toInt().coerceIn(0, 100)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Neural Conversion: $fileName")
            .setContentText("$status ($progressPercent%)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(fileName: String, progress: Float) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildProgressNotification(fileName, progress, "Processing audio...")
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletedNotification(fileName: String, outputFile: File) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Conversion Complete!")
            .setContentText("$fileName has been converted and remixed successfully.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
