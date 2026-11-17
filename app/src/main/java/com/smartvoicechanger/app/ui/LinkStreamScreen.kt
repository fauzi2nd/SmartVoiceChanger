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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LinkStreamScreen(
    cacheDir: File,
    onBack: () -> Unit,
    onProceedToTuning: (File) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val clipboardManager = LocalClipboardManager.current
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
                        .background(Color(0x3300E5FF))
                        .border(1.dp, SvcColors.CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SvcColors.CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "URL AUDIO INGEST",
                        color = SvcColors.TextPrimary,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Cloud Stream • Direct Media Link",
                        color = SvcColors.CyanSoft,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2638BDF8))
                    .border(0.8.dp, SvcColors.CyanNeon, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "CLOUD LINK",
                    color = SvcColors.CyanNeon,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center URL Input & Ingest Card
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x400A1226))
                    .border(1.2.dp, SvcColors.CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "AUDIO STREAM URL",
                        color = SvcColors.TextSecondary,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Input Box with Paste Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B18))
                            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            placeholder = {
                                Text(
                                    text = "https://audio-host.com/track.mp3",
                                    color = SvcColors.TextMuted,
                                    fontSize = 13.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Paste Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x3300E5FF))
                                .border(0.8.dp, SvcColors.CyanNeon, RoundedCornerShape(8.dp))
                                .clickable {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        urlText = clip
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "PASTE",
                                color = SvcColors.CyanNeon,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Direct audio files (.mp3, .wav, .ogg, .m4a) are supported.",
                        color = SvcColors.TextMuted,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 11.5.sp
                    )
                }
            }

            // Download Progress or Ready Card
            if (isDownloading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x330A1226))
                        .border(1.dp, SvcColors.CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FuturisticCloudStreamIcon(size = 20.dp, tint = SvcColors.CyanNeon)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Streaming Media Ingest...",
                                    color = Color.White,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                color = SvcColors.CyanNeon,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SvcColors.CyanNeon,
                            trackColor = Color(0x2638BDF8)
                        )
                    }
                }
            } else if (downloadedFile != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x330A1226))
                        .border(1.dp, SvcColors.GreenNeon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                                    .background(SvcColors.GreenNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = SvcColors.GreenNeon, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = downloadedFile?.name ?: "Stream_Audio.wav",
                                    color = Color.White,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Downloaded successfully",
                                    color = SvcColors.GreenNeon,
                                    fontFamily = SvcFonts.Rajdhani,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Button
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (downloadedFile == null) {
                Button(
                    onClick = {
                        if (urlText.isNotBlank()) {
                            isDownloading = true
                            downloadProgress = 0.1f
                            val dest = File(cacheDir, "stream_${System.currentTimeMillis()}.wav")

                            coroutineScope.launch {
                                // Simulate buffered stream ingest
                                for (i in 2..10) {
                                    delay(200)
                                    downloadProgress = i / 10f
                                }
                                // Dummy WAV write for testing
                                dest.writeBytes(ByteArray(1024))
                                downloadedFile = dest
                                isDownloading = false
                            }
                        }
                    },
                    enabled = urlText.isNotBlank() && !isDownloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (urlText.isNotBlank()) Brush.horizontalGradient(listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon))
                            else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FETCH AUDIO STREAM",
                            color = Color.Black,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        downloadedFile?.let { onProceedToTuning(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(listOf(SvcColors.GreenNeon, SvcColors.CyanNeon))
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
                        Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
