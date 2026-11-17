package com.smartvoicechanger.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch
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
    stemSeparator: com.smartvoicechanger.app.audio.StemSeparator,
    onBack: () -> Unit,
    onStartConversion: (File, String, Float, String, File?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val presets = remember {
        listOf(
            PresetOption("saisho_utane", "Saisho Utane", "RVC v2 HD", 12.0f, SvcColors.CyanNeon),
            PresetOption("aoi_cyber_girl", "Aoi Cyber Girl", "Anime Heroine", 12.0f, SvcColors.PurpleNeon),
            PresetOption("nexus_mech_ai", "Nexus Mech AI", "Sci-Fi Robot", -6.0f, Color(0xFF38BDF8)),
            PresetOption("rei_ethereal", "Rei Ethereal", "Sweet Vocal", 10.0f, SvcColors.PinkNeon),
            PresetOption("ronin_heavy_blade", "Ronin Bass", "Cinematic Male", -12.0f, Color(0xFFF59E0B))
        )
    }

    var selectedPresetId by rememberSaveable { mutableStateOf("saisho_utane") }
    val selectedPreset = presets.find { it.id == selectedPresetId } ?: presets[0]
    var pitchSemitones by rememberSaveable { mutableFloatStateOf(12.0f) }
    var isPlayingSource by remember { mutableStateOf(false) }

    // Audio Stem Choice: "MIX", "VOCAL", "INSTRUMENTAL"
    var selectedStemMode by rememberSaveable { mutableStateOf("MIX") }

    // Separated stem file paths persisted safely
    var vocalStemPath by rememberSaveable { mutableStateOf<String?>(null) }
    var instStemPath by rememberSaveable { mutableStateOf<String?>(null) }

    val vocalStemFile = vocalStemPath?.let { File(it) }?.takeIf { it.exists() }
    val instStemFile = instStemPath?.let { File(it) }?.takeIf { it.exists() }

    val activeAudioFile = when (selectedStemMode) {
        "VOCAL" -> vocalStemFile ?: inputFile
        "INSTRUMENTAL" -> instStemFile ?: inputFile
        else -> inputFile
    }

    // Download modal state
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloadingModel by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // Separation running state
    var isSeparating by remember { mutableStateOf(false) }
    var separationProgress by remember { mutableFloatStateOf(0f) }
    var separationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var isModelInstalled by remember {
        mutableStateOf(com.smartvoicechanger.app.audio.ModelDownloadService.isModelReady(context))
    }

    val scrollState = rememberScrollState()

    fun triggerStemSeparation() {
        if (!isModelInstalled) {
            showDownloadDialog = true
            return
        }
        if (isSeparating) return

        isSeparating = true
        separationProgress = 0.05f
        val outVoc = File(context.cacheDir, "stem_vocals_${System.currentTimeMillis()}.wav")
        val outInst = File(context.cacheDir, "stem_inst_${System.currentTimeMillis()}.wav")

        separationJob = coroutineScope.launch {
            try {
                val res = stemSeparator.separate(
                    inputFile = inputFile,
                    outputVocalFile = outVoc,
                    outputInstrumentalFile = outInst
                ) { p ->
                    separationProgress = p
                }
                vocalStemPath = res.vocalFile.absolutePath
                instStemPath = res.instrumentalFile.absolutePath
                selectedStemMode = "VOCAL"
                android.widget.Toast.makeText(
                    context,
                    "Vocals & Instrumental separated successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(
                    context,
                    "Separation failed: ${e.localizedMessage ?: "Unknown error"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } finally {
                isSeparating = false
                separationProgress = 0f
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Neural Vocal Conversion & Remixer",
                        color = SvcColors.CyanSoft,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 11.5.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2210B981))
                    .border(0.8.dp, SvcColors.GreenNeon, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "READY",
                    color = SvcColors.GreenNeon,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 9.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    softWrap = false,
                    maxLines = 1
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
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = activeAudioFile.name,
                                color = Color.White,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 11.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (selectedStemMode) {
                                    "VOCAL" -> "44.1 kHz PCM • Isolated Vocals Stem"
                                    "INSTRUMENTAL" -> "44.1 kHz PCM • Isolated Instrumental Stem"
                                    else -> "44.1 kHz PCM • Full Song Mix"
                                },
                                color = if (selectedStemMode == "VOCAL") SvcColors.PurpleNeon else if (selectedStemMode == "INSTRUMENTAL") SvcColors.GreenNeon else SvcColors.CyanSoft,
                                fontFamily = SvcFonts.Rajdhani,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                        val audio = activeAudioFile.inputStream().use { WavIoHelper.readWav(it) }
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

                // 5-Preset Horizontal Scrollable Carousel / Grid with Rich Visual Cards
                val presetScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(presetScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { opt ->
                        val isSelected = selectedPreset.id == opt.id
                        Box(
                            modifier = Modifier
                                .width(76.dp)
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
                                    selectedPresetId = opt.id
                                    pitchSemitones = opt.defaultPitch
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FuturisticModelsIcon(
                                    selected = isSelected,
                                    size = 22.dp
                                )
                                Text(
                                    text = opt.name.split(" ").first(),
                                    color = if (isSelected) Color.White else SvcColors.TextMuted,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 10.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2.5. STUDIO MULTI-TRACK PROCESSING CONSOLE (Opsi A: Studio Track Splitter)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x350A1226))
                    .border(1.2.dp, Color(0x3338BDF8), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header Bar with Clean Track Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FuturisticRadarNodeIcon(size = 17.dp, tint = SvcColors.CyanNeon)
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "STUDIO TRACK ROUTING",
                                color = SvcColors.CyanNeon,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Compact Segmented Switcher: [ FULL SONG ] vs [ AI ISOLATED ]
                        val isSeparatedActive = selectedStemMode != "MIX"
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2B0D1933))
                                .border(0.8.dp, Color(0x3338BDF8), RoundedCornerShape(12.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Full Mix Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (!isSeparatedActive) SvcColors.CyanNeon.copy(alpha = 0.25f) else Color.Transparent)
                                    .clickable {
                                        audioPipeline.stopPlayback()
                                        isPlayingSource = false
                                        selectedStemMode = "MIX"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "FULL SONG",
                                    color = if (!isSeparatedActive) Color.White else SvcColors.TextMuted,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 9.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }

                            // AI Stems Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSeparatedActive) SvcColors.PurpleNeon.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        audioPipeline.stopPlayback()
                                        isPlayingSource = false
                                        if (vocalStemFile != null && vocalStemFile.exists()) {
                                            selectedStemMode = "VOCAL"
                                        } else {
                                            triggerStemSeparation()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "AI STEMS",
                                    color = if (isSeparatedActive) Color.White else SvcColors.TextMuted,
                                    fontFamily = SvcFonts.Orbitron,
                                    fontSize = 9.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Dual Track Cards (Vocal Channel & Music Channel)
                    if (vocalStemFile != null && instStemFile != null) {
                        // STATE 1: STEMS ARE READY (Dual Visual Channels)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Lane 1: Vocal Track (Target AI Voice)
                            val isVocalActive = selectedStemMode == "VOCAL"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isVocalActive) Color(0x28A855F7) else Color(0x180A1226))
                                    .border(
                                        width = if (isVocalActive) 1.2.dp else 0.8.dp,
                                        color = if (isVocalActive) SvcColors.PurpleNeon else Color(0x2238BDF8),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        audioPipeline.stopPlayback()
                                        isPlayingSource = false
                                        selectedStemMode = "VOCAL"
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(SvcColors.PurpleNeon.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = SvcColors.PurpleNeon,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "VOCAL STEM",
                                                color = Color.White,
                                                fontFamily = SvcFonts.Orbitron,
                                                fontSize = 10.5.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            Text(
                                                text = "Isolated • Target for RVC Neural Conversion",
                                                color = SvcColors.PurpleNeon,
                                                fontFamily = SvcFonts.Rajdhani,
                                                fontSize = 10.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Active Status Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isVocalActive) SvcColors.PurpleNeon.copy(alpha = 0.3f) else Color.Transparent)
                                            .border(0.8.dp, if (isVocalActive) SvcColors.PurpleNeon else Color(0x3338BDF8), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isVocalActive) "CONVERT" else "SELECT",
                                            color = if (isVocalActive) Color.White else SvcColors.TextMuted,
                                            fontFamily = SvcFonts.Orbitron,
                                            fontSize = 9.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Lane 2: Backing Instrumental Track (Auto-Remix Companion)
                            val isInstActive = selectedStemMode == "INSTRUMENTAL"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isInstActive) Color(0x2510B981) else Color(0x180A1226))
                                    .border(
                                        width = if (isInstActive) 1.2.dp else 0.8.dp,
                                        color = if (isInstActive) SvcColors.GreenNeon else Color(0x2238BDF8),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        audioPipeline.stopPlayback()
                                        isPlayingSource = false
                                        selectedStemMode = "INSTRUMENTAL"
                                    }
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(SvcColors.GreenNeon.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = SvcColors.GreenNeon,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "BACKING MUSIC STEM",
                                                color = Color.White,
                                                fontFamily = SvcFonts.Orbitron,
                                                fontSize = 10.5.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                            Text(
                                                text = "Preserved Original • Auto-remixed in final output",
                                                color = SvcColors.GreenNeon,
                                                fontFamily = SvcFonts.Rajdhani,
                                                fontSize = 10.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isInstActive) SvcColors.GreenNeon.copy(alpha = 0.3f) else Color.Transparent)
                                            .border(0.8.dp, if (isInstActive) SvcColors.GreenNeon else Color(0x3338BDF8), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isInstActive) "ACTIVE" else "REMIX ON",
                                            color = if (isInstActive) Color.White else SvcColors.TextMuted,
                                            fontFamily = SvcFonts.Orbitron,
                                            fontSize = 9.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // STATE 2: STEMS NOT YET EXTRACTED (Interactive Studio Action Card)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0x30A855F7),
                                            Color(0x2000E5FF)
                                        )
                                    )
                                )
                                .border(1.dp, SvcColors.PurpleNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI VOCAL ISOLATION (UVR-MDX)",
                                        color = Color.White,
                                        fontFamily = SvcFonts.Orbitron,
                                        fontSize = 10.5.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Separate vocals from backing instruments to create studio covers & clean remix.",
                                        color = SvcColors.TextSecondary,
                                        fontFamily = SvcFonts.Rajdhani,
                                        fontSize = 10.5.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                val splitBtnBrush = if (isSeparating) {
                                    Brush.linearGradient(listOf(Color(0x3338BDF8), Color(0x3338BDF8)))
                                } else {
                                    Brush.horizontalGradient(listOf(SvcColors.PurpleNeon, SvcColors.CyanNeon))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(splitBtnBrush)
                                        .clickable {
                                            if (!isSeparating) triggerStemSeparation()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSeparating) "SPLITTING..." else if (!isModelInstalled) "GET MODEL" else "⚡ SPLIT STEMS",
                                        color = Color.Black,
                                        fontFamily = SvcFonts.Orbitron,
                                        fontSize = 9.5.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
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
                            "M -> F (+12)" to 12.0f,
                            "Natural (0)" to 0.0f,
                            "F -> M (-12)" to -12.0f
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
                                    fontSize = 11.5.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom CTA: Dynamic State-Aware Action
        val isSeparateNeeded = (selectedStemMode == "VOCAL" && vocalStemFile == null) ||
                                (selectedStemMode == "INSTRUMENTAL" && instStemFile == null)

        val ctaText = when {
            isSeparating -> "SEPARATING STEMS IN PROGRESS..."
            isSeparateNeeded -> if (!isModelInstalled) "GET STEM SEPARATOR MODEL" else "SEPARATE VOCAL & INSTRUMENTAL"
            selectedStemMode == "VOCAL" && instStemFile != null -> "CONVERT VOCALS & REMIX WITH MUSIC"
            selectedStemMode == "VOCAL" -> "CONVERT VOCALS STEM"
            selectedStemMode == "INSTRUMENTAL" -> "PROCESS INSTRUMENTAL STEM"
            else -> "CONVERT FULL SONG MIX"
        }

        val ctaGradient = if (isSeparateNeeded) {
            listOf(SvcColors.PurpleNeon, SvcColors.CyanNeon)
        } else {
            listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon, SvcColors.MagentaNeon)
        }

        Button(
            onClick = {
                audioPipeline.stopPlayback()
                if (isSeparating) return@Button

                if (isSeparateNeeded) {
                    triggerStemSeparation()
                } else {
                    val instFileForMixing = if (selectedStemMode == "VOCAL") instStemFile else null
                    onStartConversion(activeAudioFile, selectedPreset.name, pitchSemitones, selectedPreset.id, instFileForMixing)
                }
            },
            enabled = !isSeparating,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(ctaGradient)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color(0x3338BDF8)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSeparateNeeded) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    FuturisticActivityIcon(selected = true, size = 20.dp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = ctaText,
                    color = Color.Black,
                    fontFamily = SvcFonts.Orbitron,
                    fontSize = 11.5.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }

    // Modal 1: On-Demand Model Download Dialog
    if (showDownloadDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {
            if (!isDownloadingModel) showDownloadDialog = false
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF00A1226))
                    .border(1.2.dp, SvcColors.PurpleNeon, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STEM SEPARATION MODEL",
                        color = Color.White,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Download UVR-MDX-Net Vocals Model for high-fidelity vocal & instrumental extraction.",
                        color = SvcColors.CyanSoft,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Details Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x2238BDF8))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Download Size:", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                                Text(com.smartvoicechanger.app.audio.ModelDownloadService.MDX_MODEL_SIZE_LABEL, color = Color.White, fontSize = 11.sp, fontFamily = SvcFonts.Orbitron)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("License:", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                                Text("MIT License", color = SvcColors.GreenNeon, fontSize = 11.sp, fontFamily = SvcFonts.Orbitron)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Verification:", color = SvcColors.TextMuted, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                                Text("SHA-256 Checksum", color = SvcColors.CyanNeon, fontSize = 11.sp, fontFamily = SvcFonts.Rajdhani)
                            }
                        }
                    }

                    if (isDownloadingModel) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = SvcColors.PurpleNeon,
                                trackColor = Color(0x3338BDF8)
                            )
                            Text(
                                text = "Downloading & verifying... ${(downloadProgress * 100).toInt()}%",
                                color = Color.White,
                                fontFamily = SvcFonts.Orbitron,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDownloadDialog = false },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8))
                            ) {
                                Text("CANCEL", color = SvcColors.TextMuted, fontFamily = SvcFonts.Orbitron, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    isDownloadingModel = true
                                    downloadProgress = 0f
                                    coroutineScope.launch {
                                        val success = com.smartvoicechanger.app.audio.ModelDownloadService.downloadModel(context) { p ->
                                            downloadProgress = p
                                        }
                                        isDownloadingModel = false
                                        if (success) {
                                            isModelInstalled = true
                                            showDownloadDialog = false
                                            android.widget.Toast.makeText(context, "Model verified & ready!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Download failed or checksum mismatch.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SvcColors.PurpleNeon)
                            ) {
                                Text("DOWNLOAD", color = Color.White, fontFamily = SvcFonts.Orbitron, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 2: Source Separation Progress Modal
    if (isSeparating) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF00A1226))
                    .border(1.2.dp, SvcColors.CyanNeon, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = SvcColors.CyanNeon,
                        trackColor = Color(0x3338BDF8),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "NEURAL STEM SEPARATION",
                        color = Color.White,
                        fontFamily = SvcFonts.Orbitron,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Executing UVR-MDX-Net STFT inference chunks... ${(separationProgress * 100).toInt()}%",
                        color = SvcColors.CyanSoft,
                        fontFamily = SvcFonts.Rajdhani,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = {
                            separationJob?.cancel()
                            isSeparating = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SvcColors.PinkNeon.copy(alpha = 0.6f))
                    ) {
                        Text("CANCEL SEPARATION", color = SvcColors.PinkNeon, fontFamily = SvcFonts.Orbitron, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
