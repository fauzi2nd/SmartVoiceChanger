package com.smartvoicechanger.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.R
import com.smartvoicechanger.app.ui.theme.SvcColors

@Composable
fun HomeScreen(
    activeBackendName: String,
    onPickAudio: () -> Unit,
    onRecordAudio: () -> Unit,
    onFromLink: (() -> Unit)? = null // Conditionally displayed only when available
) {
    // 100% Single Viewport (Sticky, no scrolling, perfectly spaced)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
    ) {
        // 1. Full-screen seamless background artwork with anime character on the upper-right
        Image(
            painter = painterResource(id = R.drawable.home_hero_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient overlay for lower UI elements contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x10040614),
                            Color(0x70040614),
                            Color(0xFA040614)
                        ),
                        startY = 420f
                    )
                )
        )

        // 2. All-in-one unified foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // UPPER BLOCK: HEADER & TAGLINE
            Column(modifier = Modifier.fillMaxWidth()) {
                // SECTION A: UNIFIED HEADER (Logo + Stylized Sci-Fi Title + Pro Badge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.2.dp, SvcColors.CyanNeon.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            // Stylized Dual-Tone Title: "Smart" (Bold White) + "Voice Changer" (Neon Cyan-Magenta Gradient)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Smart ",
                                    color = Color.White,
                                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Voice Changer",
                                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                    style = TextStyle(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                SvcColors.CyanNeon,
                                                Color(0xFF818CF8),
                                                SvcColors.MagentaNeon
                                            )
                                        ),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                            // Sleek uppercase audio studio tag with wide letter tracking
                            Text(
                                text = "AI-POWERED AUDIO STUDIO",
                                color = SvcColors.CyanSoft.copy(alpha = 0.9f),
                                fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Rajdhani,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // Glowing Pro Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF9E00), Color(0xFFFF5400), Color(0xFFE040FB))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(id = R.string.badge_pro),
                                color = Color.White,
                                fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // SECTION B: TAGLINE (Raised directly below the logo, filling the left negative space cleanly)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .padding(top = 18.dp)
                ) {
                    Text(
                        text = "Neural Synthesis,\nZero Latency",
                        fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFE0F7FF),
                                    Color(0xFF7DD3FC),
                                    Color(0xFFC084FC)
                                )
                            ),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            lineHeight = 23.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(id = R.string.hero_desc),
                        color = Color(0xFF94A3B8),
                        fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Rajdhani,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // LOWER BLOCK: THE ACTION TILES & STATUS BAR (No duplicate Tap to Input Audio!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // PRIMARY ACTION TILES ROW (English labels: Pick Audio, Record Voice, and From Link if available)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        title = stringResource(id = R.string.action_pick_audio),
                        subtitle = stringResource(id = R.string.action_pick_audio_sub),
                        iconContent = {
                            FuturisticAudioDiscIcon(size = 24.dp, tint = SvcColors.PurpleNeon)
                        },
                        accentColor = SvcColors.PurpleNeon,
                        modifier = Modifier.weight(1f),
                        onClick = onPickAudio
                    )
                    ActionTile(
                        title = stringResource(id = R.string.action_record_voice),
                        subtitle = stringResource(id = R.string.action_record_voice_sub),
                        iconContent = {
                            FuturisticVocalMicIcon(size = 24.dp, tint = SvcColors.PinkNeon)
                        },
                        accentColor = SvcColors.PinkNeon,
                        modifier = Modifier.weight(1f),
                        onClick = onRecordAudio
                    )
                    // Conditionally show "From Link" only if the feature handler is supplied
                    if (onFromLink != null) {
                        ActionTile(
                            title = stringResource(id = R.string.action_from_link),
                            subtitle = stringResource(id = R.string.action_from_link_sub),
                            iconContent = {
                                FuturisticCloudStreamIcon(size = 24.dp, tint = SvcColors.CyanNeon)
                            },
                            accentColor = SvcColors.CyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = onFromLink
                        )
                    }
                }

                // Futuristic Cyber Telemetry Status Bar (Neural Engine & Realtime Latency)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x400A1226))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    SvcColors.GreenNeon.copy(alpha = 0.5f),
                                    SvcColors.CyanNeon.copy(alpha = 0.3f),
                                    SvcColors.PurpleNeon.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Active Neural Engine Radar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SvcColors.GreenNeon.copy(alpha = 0.12f))
                                    .border(1.2.dp, SvcColors.GreenNeon.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                FuturisticRadarNodeIcon(size = 24.dp, tint = SvcColors.GreenNeon)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "NEURAL INFERENCE ENGINE",
                                    color = SvcColors.TextSecondary,
                                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Rajdhani,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x4D052E16))
                                        .border(1.dp, SvcColors.GreenNeon.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "[ $activeBackendName ]",
                                        color = SvcColors.GreenNeon,
                                        fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        // Right: Cyber Oscilloscope Latency
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FuturisticOscilloscopeIcon(size = 22.dp, tint = SvcColors.CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "LATENCY",
                                    color = SvcColors.TextSecondary,
                                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Rajdhani,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "0.49 ms",
                                    color = SvcColors.CyanNeon,
                                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    iconContent: @Composable () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(132.dp) // Uniform fixed height ensures perfectly symmetric cards
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x380A1226)) // Translucent cyberpunk glass effect
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = SvcColors.TextPrimary,
                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = SvcColors.TextMuted,
                    fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Rajdhani,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
