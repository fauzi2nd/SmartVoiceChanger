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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.audio.VoiceConversionPipeline
import com.smartvoicechanger.app.audio.WavIoHelper
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts
import java.io.File

data class PresetOption(
    val id: String,
    val name: String,
    val role: String,
    val defaultPitch: Float,
    val color: Color
)

@Composable
fun AudioTuningScreen(
    inputFile: File,
    audioPipeline: VoiceConversionPipeline,
    onBack: () -> Unit,
    onStartConversion: (File, String, Float) -> Unit
) {
    val presets = remember {
        listOf(
            PresetOption("aoi", "Aoi Cyber Girl", "Anime Heroine", 12.0f, SvcColors.PurpleNeon),
            PresetOption("nexus", "Nexus Mech AI", "Sci-Fi Robot", -6.0f, SvcColors.CyanNeon),
            PresetOption("rei", "Rei Ethereal", "Sweet Vocal", 10.0f, SvcColors.PinkNeon),
            PresetOption("ronin", "Ronin Bass", "Cinematic Male", -12.0f, Color(0xFFF59E0B))
        )
    }

    var selectedPreset by remember { mutableStateOf(presets[0]) }
    var pitchSemitones by remember { mutableFloatStateOf(12.0f) }
    var isPlayingSource by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33A855F7))
                        .border(1.dp, SvcColors.PurpleNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            audioPipeline.stopPlayback()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SvcColors.PurpleNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "STUDIO PRESET TUNING",
                        color = SvcColors.TextPrimary,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "RVC Neural Model & Pitch Shift",
                        color = SvcColors.CyanSoft,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2210B981))
                    .border(0.8.dp, SvcColors.GreenNeon, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "READY",
                    color = SvcColors.GreenNeon,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 9.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        // Center Content Container
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sleek Audio Waveform Source Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x400A1226))
                    .border(1.2.dp, SvcColors.CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SvcColors.CyanNeon.copy(alpha = 0.15f))
                                .border(1.2.dp, SvcColors.CyanNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            FuturisticAudioDiscIcon(size = 24.dp, tint = SvcColors.CyanNeon)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = inputFile.name,
                                color = Color.White,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 12.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "44.1 kHz PCM • Input Audio",
                                color = SvcColors.CyanSoft,
                                fontFamily = SvcFonts.Rajdhani,
                                fontSize = 11.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                    }

                    // Cyber Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlayingSource) Brush.horizontalGradient(listOf(SvcColors.GreenNeon, SvcColors.CyanNeon))
                                else Brush.horizontalGradient(listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon))
                            )
                            .clickable {
                                if (isPlayingSource) {
                                    audioPipeline.stopPlayback()
                                    isPlayingSource = false
                                } else {
                                    isPlayingSource = true
                                    try {
                                        val audio = inputFile.inputStream().use { WavIoHelper.readWav(it) }
                                        audioPipeline.playAudio(audio)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlayingSource) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Target Voice Models Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TARGET VOICE MODEL",
                    color = SvcColors.PurpleNeon,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // 4-Preset Grid with Rich Visual Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { opt ->
                        val isSelected = selectedPreset.id == opt.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) opt.color.copy(alpha = 0.22f)
                                    else Color(0x330A1226)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) opt.color else Color(0x2238BDF8),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedPreset = opt
                                    pitchSemitones = opt.defaultPitch
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FuturisticModelsIcon(selected = isSelected, size = 26.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = opt.name.split(" ").first(),
                                    color = if (isSelected) Color.White else SvcColors.TextMuted,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 10.5.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = opt.role,
                                    color = if (isSelected) opt.color else SvcColors.TextMuted.copy(alpha = 0.6f),
                                    fontFamily = SvcFonts.Rajdhani,
                                    fontSize = 9.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. Pitch Tuning & Digital Oscilloscope Slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x400A1226))
                    .border(1.2.dp, Color(0x3338BDF8), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FuturisticOscilloscopeIcon(size = 18.dp, tint = SvcColors.CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PITCH FREQUENCY",
                                color = SvcColors.TextSecondary,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "${if (pitchSemitones > 0) "+" else ""}${pitchSemitones.toInt()} SEMITONES",
                            color = SvcColors.CyanNeon,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    // Futuristic Neon Slider
                    Slider(
                        value = pitchSemitones,
                        onValueChange = { pitchSemitones = it },
                        valueRange = -12f..12f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = SvcColors.CyanNeon,
                            activeTrackColor = SvcColors.CyanNeon,
                            inactiveTrackColor = Color(0x3338BDF8)
                        )
                    )

                    // Quick Pitch Presets Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Male -> Female (+12)" to 12.0f,
                            "Natural (0)" to 0.0f,
                            "Female -> Male (-12)" to -12.0f
                        ).forEach { (label, semitones) ->
                            val isQuickActive = pitchSemitones == semitones
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isQuickActive) Color(0x3300E5FF) else Color(0x220A1226))
                                    .border(
                                        width = 1.dp,
                                        color = if (isQuickActive) SvcColors.CyanNeon else Color(0x2238BDF8),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { pitchSemitones = semitones }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isQuickActive) Color.White else SvcColors.TextMuted,
                                    fontFamily = SvcFonts.Rajdhani,
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom CTA: Start Neural Conversion
        Button(
            onClick = {
                audioPipeline.stopPlayback()
                onStartConversion(inputFile, selectedPreset.name, pitchSemitones)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon, SvcColors.MagentaNeon)
                    )
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FuturisticActivityIcon(selected = true, size = 20.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "START NEURAL CONVERSION",
                    color = Color.Black,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
