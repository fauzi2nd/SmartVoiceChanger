package com.smartvoicechanger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.audio.AudioRecorderHelper
import com.smartvoicechanger.app.audio.VoiceConversionPipeline
import com.smartvoicechanger.app.audio.WavIoHelper
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RecordBoothScreen(
    recorderHelper: AudioRecorderHelper,
    audioPipeline: VoiceConversionPipeline,
    cacheDir: File,
    onBack: () -> Unit,
    onProceedToTuning: (File) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordDurationSeconds by remember { mutableIntStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33EC4899))
                        .border(1.dp, SvcColors.PinkNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            if (isRecording) {
                                isRecording = false
                                recorderHelper.stopRecording()
                            }
                            audioPipeline.stopPlayback()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SvcColors.PinkNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "VOCAL RECORD BOOTH",
                        color = SvcColors.TextPrimary,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "44.1 kHz • 16-Bit Studio Acoustic",
                        color = SvcColors.PinkNeon,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRecording) Color(0x33EC4899) else Color(0x2210B981))
                    .border(
                        0.8.dp,
                        if (isRecording) SvcColors.PinkNeon else SvcColors.GreenNeon,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isRecording) "LIVE REC" else if (recordedFile != null) "SAVED" else "STANDBY",
                    color = if (isRecording) SvcColors.PinkNeon else if (recordedFile != null) SvcColors.CyanNeon else SvcColors.GreenNeon,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Vocal Visualizer & Timer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pulsing Sonar Ring
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) SvcColors.PinkNeon.copy(alpha = 0.15f)
                        else Color(0x180A1226)
                    )
                    .border(
                        width = if (isRecording) 2.dp else 1.dp,
                        brush = if (isRecording) Brush.sweepGradient(listOf(SvcColors.PinkNeon, Color(0xFFE11D48), SvcColors.PinkNeon))
                        else Brush.linearGradient(listOf(Color(0x3338BDF8), Color(0x2238BDF8))),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                FuturisticVocalMicIcon(size = 64.dp, tint = if (isRecording) SvcColors.PinkNeon else SvcColors.CyanSoft)
            }

            // Digital Duration Clock
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = recordDurationSeconds / 60
                val secs = recordDurationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    color = if (isRecording) SvcColors.PinkNeon else Color.White,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isRecording) "RECORDING IN PROGRESS..." else if (recordedFile != null) "RECORDING READY TO REVIEW" else "TAP START TO RECORD VOCAL",
                    color = SvcColors.TextMuted,
                    fontFamily = SvcFonts.Rajdhani,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Recorded File Preview Card
            if (recordedFile != null && !isRecording) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x330A1226))
                        .border(1.dp, SvcColors.PinkNeon.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SvcColors.PinkNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                FuturisticAudioDiscIcon(size = 20.dp, tint = SvcColors.PinkNeon)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = recordedFile?.name ?: "Vocal_Take.wav",
                                    color = Color.White,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ready for voice conversion",
                                    color = SvcColors.CyanSoft,
                                    fontFamily = SvcFonts.Rajdhani,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Play/Pause Preview
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SvcColors.PinkNeon)
                                .clickable {
                                    if (isPlayingPreview) {
                                        audioPipeline.stopPlayback()
                                        isPlayingPreview = false
                                    } else {
                                        recordedFile?.let { file ->
                                            isPlayingPreview = true
                                            try {
                                                val audio = file.inputStream().use { WavIoHelper.readWav(it) }
                                                audioPipeline.playAudio(audio)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Record / Stop Button
            Button(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        recorderHelper.stopRecording()
                    } else {
                        audioPipeline.stopPlayback()
                        isPlayingPreview = false
                        recordDurationSeconds = 0
                        val dest = File(cacheDir, "mic_take_${System.currentTimeMillis()}.wav")
                        recordedFile = dest
                        isRecording = true

                        coroutineScope.launch {
                            val timer = launch {
                                while (isRecording) {
                                    delay(1000)
                                    recordDurationSeconds++
                                }
                            }
                            recorderHelper.startRecording(dest)
                            timer.cancel()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isRecording) Brush.horizontalGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
                        else Brush.horizontalGradient(listOf(SvcColors.PinkNeon, Color(0xFF9333EA)))
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecording) "STOP RECORDING" else if (recordedFile != null) "RE-RECORD VOCAL" else "START RECORDING",
                        color = Color.White,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Proceed to Tuning Button (Shown when a take is recorded)
            if (recordedFile != null && !isRecording) {
                Button(
                    onClick = {
                        audioPipeline.stopPlayback()
                        recordedFile?.let { onProceedToTuning(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon))
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PROCEED TO TUNING",
                            color = Color.Black,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
