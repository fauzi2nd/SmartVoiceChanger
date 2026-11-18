package com.smartvoicechanger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.audio.AudioData
import com.smartvoicechanger.app.audio.VoiceConversionPipeline
import com.smartvoicechanger.app.audio.WavIoHelper
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts
import java.io.File

data class ConversionHistoryItem(
    val id: String,
    val fileName: String,
    val modelName: String,
    val duration: String,
    val timestamp: String,
    val latencyMs: Double,
    val audioFile: File? = null
)

@Composable
fun ActivityScreen(
    pipeline: VoiceConversionPipeline,
    isConverting: Boolean,
    progress: Float,
    currentFileName: String?,
    currentModelName: String?,
    stageLabel: String? = null,
    onCancelJob: (() -> Unit)? = null,
    historyItems: List<ConversionHistoryItem>,
    isRecording: Boolean = false,
    recordingDurationSeconds: Int = 0,
    onStopRecording: (() -> Unit)? = null
) {
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: ONGOING CONVERSION / REALTIME PROCESSING PIPELINE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRecording) "LIVE MICROPHONE RECORDING" else "NEURAL INFERENCE QUEUE",
                color = if (isRecording) SvcColors.PinkNeon else SvcColors.CyanNeon,
                fontFamily = SvcFonts.Orbitron,
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            if (isRecording) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33EC4899))
                        .border(0.8.dp, SvcColors.PinkNeon, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "REC",
                        color = SvcColors.PinkNeon,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            } else if (isConverting) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x3300E5FF))
                        .border(0.8.dp, SvcColors.CyanNeon, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PROCESSING",
                        color = SvcColors.CyanNeon,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x2210B981))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STANDBY",
                        color = SvcColors.GreenNeon,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 9.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x400A1226))
                .border(
                    width = 1.2.dp,
                    brush = if (isRecording) {
                        Brush.horizontalGradient(listOf(SvcColors.PinkNeon, Color(0xFFE11D48)))
                    } else if (isConverting) {
                        Brush.horizontalGradient(listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0x2238BDF8), Color(0x1138BDF8)))
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            if (isRecording) {
                // Recording Active UI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SvcColors.PinkNeon.copy(alpha = 0.15f))
                                .border(1.dp, SvcColors.PinkNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            FuturisticVocalMicIcon(size = 22.dp, tint = SvcColors.PinkNeon)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Recording Vocal Take...",
                                color = SvcColors.TextPrimary,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Duration: ${recordingDurationSeconds}s • 44.1kHz PCM",
                                color = SvcColors.PinkNeon,
                                fontFamily = SvcFonts.Rajdhani,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                    }

                    // Stop Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE11D48))
                            .clickable { onStopRecording?.invoke() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "STOP",
                            color = Color.White,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            } else if (isConverting) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SvcColors.CyanNeon.copy(alpha = 0.15f))
                                    .border(1.dp, SvcColors.CyanNeon, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                FuturisticActivityIcon(selected = true, size = 22.dp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentFileName ?: "audio_input.wav",
                                    color = SvcColors.TextPrimary,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 11.5.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stageLabel ?: "Target Voice: ${currentModelName ?: "Saisho Utane"}",
                                    color = SvcColors.CyanSoft,
                                    fontFamily = SvcFonts.Rajdhani,
                                    fontSize = 11.5.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Percentage text with dedicated fixed min width to avoid wrapping
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = SvcColors.CyanNeon,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    // Futuristic Neon Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x2638BDF8))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SvcColors.CyanNeon, SvcColors.MagentaNeon)
                                    )
                                )
                        )
                    }

                    // Bottom info & cancel button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model: ${currentModelName ?: "Saisho Utane"}",
                            color = SvcColors.TextMuted,
                            fontFamily = SvcFonts.Rajdhani,
                            fontSize = 11.sp
                        )

                        if (onCancelJob != null) {
                            Text(
                                text = "CANCEL",
                                color = SvcColors.PinkNeon,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 10.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onCancelJob() }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2238BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        FuturisticRadarNodeIcon(size = 22.dp, tint = SvcColors.GreenNeon)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Engine Ready & Listening",
                            color = SvcColors.TextPrimary,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pick audio from Home or Record voice to start",
                            color = SvcColors.TextMuted,
                            fontFamily = SvcFonts.Rajdhani,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }

        // SECTION 2: CONVERSION HISTORY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONVERSION ARCHIVE",
                color = SvcColors.PurpleNeon,
                fontFamily = SvcFonts.Orbitron,
                fontSize = 11.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "${historyItems.size} RECORDINGS",
                color = SvcColors.TextMuted,
                fontFamily = SvcFonts.Rajdhani,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1A0A1226))
                    .border(1.dp, Color(0x1A38BDF8), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuturisticAudioDiscIcon(size = 36.dp, tint = SvcColors.TextMuted)
                    Text(
                        text = "NO CONVERSION ARCHIVE",
                        color = SvcColors.TextMuted,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Processed voice outputs will be cataloged here.",
                        color = SvcColors.TextMuted.copy(alpha = 0.7f),
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyItems) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x330A1226))
                        .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SvcColors.PurpleNeon.copy(alpha = 0.15f))
                                    .border(1.dp, SvcColors.PurpleNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                FuturisticAudioDiscIcon(size = 22.dp, tint = SvcColors.PurpleNeon)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.fileName,
                                    color = SvcColors.TextPrimary,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 11.5.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.modelName} • ${item.duration}",
                                    color = SvcColors.CyanSoft,
                                    fontFamily = SvcFonts.Rajdhani,
                                    fontSize = 11.5.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        }

                        // Play Holographic Button
                        val isPlaying = currentlyPlayingId == item.id
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying) Brush.horizontalGradient(listOf(SvcColors.GreenNeon, SvcColors.CyanNeon))
                                    else Brush.horizontalGradient(listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon))
                                )
                                .clickable {
                                    if (isPlaying) {
                                        pipeline.stopPlayback()
                                        currentlyPlayingId = null
                                    } else {
                                        currentlyPlayingId = item.id
                                        item.audioFile?.let { file ->
                                            if (file.exists()) {
                                                try {
                                                    val audioData = file.inputStream().use { WavIoHelper.readWav(it) }
                                                    pipeline.playAudio(audioData)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
