package com.smartvoicechanger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import com.smartvoicechanger.app.inference.DeviceCapabilities
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts

@Composable
fun HardwareMonitorScreen(
    capabilities: DeviceCapabilities,
    onRunDiagnosticsClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main SoC Card: Snapdragon 695 / Adreno 619 + Neural Acceleration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x400A1226))
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(SvcColors.CyanNeon.copy(alpha = 0.6f), SvcColors.PurpleNeon.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SvcColors.CyanNeon.copy(alpha = 0.15f))
                            .border(1.2.dp, SvcColors.CyanNeon, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        FuturisticHardwareIcon(selected = true, size = 26.dp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = capabilities.socModel,
                            color = SvcColors.TextPrimary,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = capabilities.vulkanCapabilities.deviceName,
                            color = SvcColors.CyanSoft,
                            fontFamily = SvcFonts.Rajdhani,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Green "OPTIMAL ACCEL" Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3310B981))
                        .border(1.dp, SvcColors.GreenNeon, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SvcColors.GreenNeon, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OPTIMAL",
                            color = SvcColors.GreenNeon,
                            fontFamily = SvcFonts.Orbitron,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section Title: Telemetry
        Text(
            text = "HARDWARE TELEMETRY",
            color = SvcColors.CyanNeon,
            fontFamily = SvcFonts.Orbitron,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        // 2x2 Metric Grid (CPU, RAM, GPU, Temperature)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Card
            FuturisticHardwareMetricCard(
                title = "CPU ENGINE",
                value = "32%",
                subtitle = "2.21 GHz (${capabilities.cpuCores} Cores)",
                accentColor = SvcColors.CyanNeon,
                modifier = Modifier.weight(1f)
            )

            // RAM Card
            val totalGb = "%.1f".format(capabilities.totalRamBytes / 1e9)
            val usedGb = "%.1f".format((capabilities.totalRamBytes - capabilities.availableRamBytes) / 1e9)
            FuturisticHardwareMetricCard(
                title = "RAM BUFFER",
                value = "62%",
                subtitle = "$usedGb / $totalGb GB",
                accentColor = SvcColors.PurpleNeon,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // GPU Card
            FuturisticHardwareMetricCard(
                title = "GPU COMPUTE",
                value = "ACTIVE",
                subtitle = "Vulkan 1.2 (Subgroup 64)",
                accentColor = SvcColors.GreenNeon,
                modifier = Modifier.weight(1f)
            )

            // Temperature Card
            FuturisticHardwareMetricCard(
                title = "THERMAL STATUS",
                value = "36°C",
                subtitle = if (capabilities.isThermalThrottling) "Throttling" else "Normal • Cool",
                accentColor = SvcColors.PinkNeon,
                modifier = Modifier.weight(1f)
            )
        }

        // Active Inference Pipeline Architecture
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x330A1226))
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SvcColors.GreenNeon.copy(alpha = 0.15f))
                                .border(1.dp, SvcColors.GreenNeon.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            FuturisticRadarNodeIcon(size = 20.dp, tint = SvcColors.GreenNeon)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ACTIVE BACKEND ROUTE",
                                color = SvcColors.TextSecondary,
                                fontFamily = SvcFonts.Rajdhani,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "ARM NEON CPU + Vulkan GPU",
                                color = SvcColors.TextPrimary,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3300E5FF))
                            .border(0.8.dp, SvcColors.CyanNeon, RoundedCornerShape(8.dp))
                            .clickable { onRunDiagnosticsClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BENCHMARK",
                                color = SvcColors.CyanNeon,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = SvcColors.CyanNeon, modifier = Modifier.size(10.dp))
                        }
                    }
                }

                // Supported text banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2210B981))
                        .border(0.8.dp, SvcColors.GreenNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SvcColors.GreenNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Snapdragon 695 5G fully supports real-time RVC v2 inference",
                            color = SvcColors.GreenNeon,
                            fontFamily = SvcFonts.Rajdhani,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuturisticHardwareMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x330A1226))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(accentColor.copy(alpha = 0.5f), accentColor.copy(alpha = 0.15f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = SvcColors.TextSecondary,
                    fontFamily = SvcFonts.Rajdhani,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = value,
                    color = accentColor,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = subtitle,
                color = SvcColors.TextMuted,
                fontFamily = SvcFonts.Rajdhani,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            // Digital Audio/Telemetry Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val bars = listOf(4, 7, 11, 8, 13, 9, 12, 14, 10, 13)
                    bars.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h.dp)
                                .background(accentColor.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}
