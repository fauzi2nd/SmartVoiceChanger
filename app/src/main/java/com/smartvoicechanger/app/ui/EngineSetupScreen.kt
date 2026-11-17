package com.smartvoicechanger.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.audio.ModelDownloadService
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts
import kotlinx.coroutines.launch

@Composable
fun EngineSetupScreen(
    onCompleteSetup: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isStemReady by remember { mutableStateOf(ModelDownloadService.isModelReady(context)) }
    var isStemDownloading by remember { mutableStateOf(false) }
    var stemProgress by remember { mutableFloatStateOf(0f) }

    var isSaishoReady by remember { mutableStateOf(ModelDownloadService.isSaishoModelReady(context)) }
    var isSaishoDownloading by remember { mutableStateOf(false) }
    var saishoProgress by remember { mutableFloatStateOf(0f) }

    fun triggerStemDownload() {
        if (isStemDownloading) return
        isStemDownloading = true
        stemProgress = 0f
        coroutineScope.launch {
            val success = ModelDownloadService.downloadModel(context) { p ->
                stemProgress = p
            }
            isStemDownloading = false
            if (success) {
                isStemReady = true
                android.widget.Toast.makeText(context, "UVR-MDX-Net Vocals Model Verified & Ready!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Download failed or checksum mismatch.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STUDIO ENGINE SETUP",
                    color = Color.White,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Neural Audio Assets Onboarding",
                    color = SvcColors.CyanSoft,
                    fontFamily = SvcFonts.Rajdhani,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2238BDF8))
                    .border(0.8.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
                    .clickable { onSkip() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SKIP FOR NOW",
                    color = SvcColors.CyanSoft,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Info Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x250A1226))
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Download required on-device neural models for high-fidelity vocal extraction & conversion. All processing runs 100% locally on your Snapdragon chip — no server uploads.",
                color = SvcColors.TextMuted,
                fontFamily = SvcFonts.Rajdhani,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Card 1: Stem Separation Engine
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x400A1226))
                .border(
                    width = if (isStemReady) 1.2.dp else 1.dp,
                    color = if (isStemReady) SvcColors.GreenNeon.copy(alpha = 0.6f) else Color(0x3338BDF8),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎧 UVR-MDX-Net Vocals",
                            color = Color.White,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Isolates singing vocal & backing track for clean remixing.",
                            color = SvcColors.CyanSoft,
                            fontFamily = SvcFonts.Rajdhani,
                            fontSize = 11.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22EF4444))
                            .border(0.8.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REQUIRED FOR SONGS",
                            color = Color(0xFFF87171),
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size: ${ModelDownloadService.MDX_MODEL_SIZE_LABEL}", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                    Text("License: MIT", color = SvcColors.CyanNeon, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                    Text("SHA-256: Verified", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                }

                if (isStemDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { stemProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SvcColors.CyanNeon,
                            trackColor = Color(0x3338BDF8)
                        )
                        Text(
                            text = "Downloading & verifying... ${(stemProgress * 100).toInt()}%",
                            color = Color.White,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 10.sp
                        )
                    }
                } else if (isStemReady) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SvcColors.GreenNeon))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INSTALLED & READY", color = SvcColors.GreenNeon, fontFamily = SvcFonts.Orbitron, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { triggerStemDownload() },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SvcColors.CyanNeon)
                    ) {
                        Text("DOWNLOAD UVR-MDX-NET (63.7 MB)", color = Color.Black, fontFamily = SvcFonts.Orbitron, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card 2: Default Voice Model (Saisho Utane RVC v2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x400A1226))
                .border(
                    width = if (isSaishoReady) 1.2.dp else 1.dp,
                    color = if (isSaishoReady) SvcColors.GreenNeon.copy(alpha = 0.6f) else SvcColors.PurpleNeon.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎙️ Saisho Utane (RVC v2)",
                            color = Color.White,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "48 kHz High-Definition melodic anime singing model.",
                            color = SvcColors.CyanSoft,
                            fontFamily = SvcFonts.Rajdhani,
                            fontSize = 11.5.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22A855F7))
                            .border(0.8.dp, SvcColors.PurpleNeon, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "RECOMMENDED",
                            color = SvcColors.PurpleNeon,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size: ${ModelDownloadService.SAISHO_MODEL_SIZE_LABEL}", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                    Text("License: CC BY 4.0", color = SvcColors.PurpleNeon, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                    Text("Author: Utane Project", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                }

                if (isSaishoReady) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SvcColors.GreenNeon))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INSTALLED & READY", color = SvcColors.GreenNeon, fontFamily = SvcFonts.Orbitron, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Model asset available for installation in local storage.",
                        color = SvcColors.CyanSoft,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Card 3: Pre-Installed Offline Filters
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x200A1226))
                .border(0.8.dp, Color(0x2238BDF8), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Built-in Studio Filters",
                        color = Color.White,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x2210B981))
                            .border(0.8.dp, SvcColors.GreenNeon, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRE-INSTALLED",
                            color = SvcColors.GreenNeon,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "Aoi, Nexus Mech, Rei, Ronin spectral timbre filters ready offline (0 MB).",
                    color = SvcColors.TextMuted,
                    fontFamily = SvcFonts.Rajdhani,
                    fontSize = 11.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Enter Studio Button
        Button(
            onClick = { onCompleteSetup() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon, SvcColors.MagentaNeon)
                    )
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Text(
                text = "ENTER STUDIO & START CREATING",
                color = Color.Black,
                fontFamily = SvcFonts.Orbitron,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
