package com.smartvoicechanger.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.model.VoiceModel
import com.smartvoicechanger.app.ui.theme.SvcColors
import com.smartvoicechanger.app.ui.theme.SvcFonts
import java.io.File

data class PresetVoiceCard(
    val id: String,
    val name: String,
    val category: String,
    val tag: String,
    val sizeMb: Int,
    val isInstalled: Boolean,
    val accentColor: Color,
    val source: String = "Bundled" // Bundled, HuggingFace, or Local
)

@Composable
fun VoiceLibraryScreen(
    modelsDir: File,
    onModelSelected: (VoiceModel) -> Unit
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("ALL", "ANIME", "CYBER", "CHARACTER", "IMPORTED")

    // File picker launcher for importing custom RVC models (.zip or .onnx)
    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Future model import handler
        }
    }

    // Pre-configured models list with our Bundled Starter Model + Hugging Face Free Models
    var modelsList by remember {
        mutableStateOf(
            listOf(
                PresetVoiceCard(
                    id = "aoi_rvc_v2",
                    name = "Aoi Cyber Girl",
                    category = "ANIME",
                    tag = "Studio RVC v2",
                    sizeMb = 28,
                    isInstalled = true,
                    accentColor = SvcColors.PurpleNeon,
                    source = "Bundled Offline"
                ),
                PresetVoiceCard(
                    id = "cyber_mecha_v2",
                    name = "Nexus Mech AI",
                    category = "CYBER",
                    tag = "Vocal Synth",
                    sizeMb = 18,
                    isInstalled = false,
                    accentColor = SvcColors.CyanNeon,
                    source = "HuggingFace Hub"
                ),
                PresetVoiceCard(
                    id = "rei_soft_v2",
                    name = "Rei Ethereal",
                    category = "ANIME",
                    tag = "Sweet Acoustic",
                    sizeMb = 24,
                    isInstalled = false,
                    accentColor = SvcColors.PinkNeon,
                    source = "HuggingFace Hub"
                ),
                PresetVoiceCard(
                    id = "glitch_droid_v2",
                    name = "Droid 808",
                    category = "CYBER",
                    tag = "Deep Robotic",
                    sizeMb = 16,
                    isInstalled = false,
                    accentColor = Color(0xFF38BDF8),
                    source = "HuggingFace Hub"
                ),
                PresetVoiceCard(
                    id = "shoujo_idol_v2",
                    name = "Idol Sparkle",
                    category = "CHARACTER",
                    tag = "Bright Vocal",
                    sizeMb = 26,
                    isInstalled = false,
                    accentColor = SvcColors.MagentaNeon,
                    source = "HuggingFace Hub"
                ),
                PresetVoiceCard(
                    id = "deep_narrator_v2",
                    name = "Ronin Narrator",
                    category = "CHARACTER",
                    tag = "Cinematic Bass",
                    sizeMb = 22,
                    isInstalled = false,
                    accentColor = Color(0xFFF59E0B),
                    source = "HuggingFace Hub"
                )
            )
        )
    }

    val filteredModels = remember(selectedCategoryIndex, modelsList) {
        if (selectedCategoryIndex == 0) modelsList
        else {
            val targetCategory = categories[selectedCategoryIndex]
            modelsList.filter { it.category.equals(targetCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040614))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Info & Quick Import Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NEURAL PRESETS",
                    color = SvcColors.CyanNeon,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "High-fidelity RVC v2 voice cartridges",
                    color = SvcColors.TextMuted,
                    fontFamily = SvcFonts.Rajdhani,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Import Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x3300E5FF))
                    .border(1.dp, SvcColors.CyanNeon, RoundedCornerShape(10.dp))
                    .clickable { modelPickerLauncher.launch("*/*") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SvcColors.CyanNeon,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "IMPORT",
                        color = Color.White,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Category Filter Pills (Smooth Horizontal Scrollable)
        val categoryScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(categoryScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEachIndexed { index, catName ->
                val isSelected = selectedCategoryIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon))
                            } else {
                                Brush.linearGradient(listOf(Color(0x2B0D1933), Color(0x2B0D1933)))
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) SvcColors.CyanNeon else Color(0x3338BDF8),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedCategoryIndex = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = catName,
                        color = if (isSelected) Color.Black else SvcColors.TextSecondary,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // 2-Column Neural Cartridge Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredModels) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x400A1226))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    item.accentColor.copy(alpha = 0.5f),
                                    item.accentColor.copy(alpha = 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            // Select model callback
                            val targetFile = File(modelsDir, "${item.id}.onnx")
                            val model = VoiceModel(
                                id = item.id,
                                name = item.name,
                                modelFile = targetFile
                            )
                            onModelSelected(model)
                        }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Badge Row (Installed vs Downloadable Free)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.isInstalled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3310B981))
                                        .border(0.8.dp, SvcColors.GreenNeon, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "READY",
                                        color = SvcColors.GreenNeon,
                                        fontFamily = SvcFonts.Orbitron,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x2638BDF8))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${item.sizeMb} MB",
                                        color = SvcColors.CyanSoft,
                                        fontFamily = SvcFonts.Rajdhani,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Favorite / Pin Icon
                            Icon(
                                imageVector = if (item.isInstalled) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (item.isInstalled) SvcColors.GreenNeon else SvcColors.CyanSoft,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Sci-Fi Hologram Avatar / Icon Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.accentColor.copy(alpha = 0.12f))
                                .border(1.dp, item.accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            FuturisticModelsIcon(selected = true, size = 30.dp)
                        }

                        // Model Title & Details
                        Column {
                            Text(
                                text = item.name,
                                color = SvcColors.TextPrimary,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.tag} • ${item.source}",
                                color = SvcColors.TextMuted,
                                fontFamily = SvcFonts.Rajdhani,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        // Action Button (Use Voice or Download)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (item.isInstalled) {
                                        Brush.horizontalGradient(listOf(SvcColors.PurpleNeon, Color(0xFF6366F1)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (item.isInstalled) SvcColors.PurpleNeon else SvcColors.CyanNeon.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (item.isInstalled) "SELECT" else "DOWNLOAD",
                                color = Color.White,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
