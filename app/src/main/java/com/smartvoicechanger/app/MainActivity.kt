package com.smartvoicechanger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvoicechanger.app.audio.VoiceConversionPipeline
import com.smartvoicechanger.app.inference.AndroidHardwareInspector
import com.smartvoicechanger.app.inference.HardwareInspector
import com.smartvoicechanger.app.model.VoiceModel
import com.smartvoicechanger.app.ui.*
import com.smartvoicechanger.app.ui.theme.SvcColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var diagnosticEngine: DiagnosticEngine
    private lateinit var audioPipeline: VoiceConversionPipeline
    private lateinit var hardwareInspector: HardwareInspector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        diagnosticEngine = DiagnosticEngine(this)
        audioPipeline = VoiceConversionPipeline(this)
        hardwareInspector = AndroidHardwareInspector(this)

        val modelsDir = File(filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Transparent,
                    surface = SvcColors.BackgroundElevated,
                    primary = SvcColors.CyanNeon,
                    secondary = SvcColors.PurpleNeon,
                    onBackground = SvcColors.TextPrimary,
                    onSurface = SvcColors.TextPrimary
                )
            ) {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    MainAppHost(
                        diagnosticEngine = diagnosticEngine,
                        audioPipeline = audioPipeline,
                        inspector = hardwareInspector,
                        modelsDir = modelsDir,
                        cacheDir = cacheDir
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPipeline.stopPlayback()
    }
}

@Composable
fun MainAppHost(
    diagnosticEngine: DiagnosticEngine,
    audioPipeline: VoiceConversionPipeline,
    inspector: HardwareInspector,
    modelsDir: File,
    cacheDir: File
) {
    // 4 Distinct Navigation Tabs in exact requested order:
    // 0: Home, 1: Models, 2: Activity, 3: Hardware
    var currentTab by remember { mutableStateOf(0) }
    // Sub-screens for studio workflow: "NONE", "RECORD_BOOTH", "LINK_INGEST", "AUDIO_TUNING"
    var activeSubScreen by remember { mutableStateOf("NONE") }
    var tuningFile by remember { mutableStateOf<File?>(null) }
    val capabilities = remember { inspector.inspect() }

    var selectedAudioFile by remember { mutableStateOf<File?>(null) }
    var selectedVoiceModel by remember { mutableStateOf<VoiceModel?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableStateOf(0f) }

    // Real audio recorder helper and reactive history items (starts empty, populated only by real runs)
    val recorderHelper = remember { com.smartvoicechanger.app.audio.AudioRecorderHelper() }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var lastMeasuredLatencyMs by remember { mutableStateOf<Double?>(null) }
    val historyItems = remember {
        mutableStateListOf<com.smartvoicechanger.app.ui.ConversionHistoryItem>()
    }

    val coroutineScope = rememberCoroutineScope()

    // Function to trigger audio conversion pipeline
    fun processConversion(inputFile: File, targetName: String, pitchSemitones: Float = 12.0f) {
        selectedAudioFile = inputFile
        activeSubScreen = "NONE"
        currentTab = 2 // Switch directly to Activity tab
        isConverting = true
        conversionProgress = 0.05f

        val outputDir = File(cacheDir, "converted_audio")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "output_${System.currentTimeMillis()}.wav")

        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val result = audioPipeline.convertAudio(
                    inputFile = inputFile,
                    outputFile = outputFile,
                    pitchSemitones = pitchSemitones
                ) { p ->
                    conversionProgress = p
                }

                val elapsedMs = (System.currentTimeMillis() - startTime).toDouble()
                lastMeasuredLatencyMs = elapsedMs

                // Add to history list with real measured execution latency
                val audioDurationSec = (result.samples.size / result.sampleRate).coerceAtLeast(1)
                val durationStr = String.format("%02d:%02d", audioDurationSec / 60, audioDurationSec % 60)

                val newHistory = com.smartvoicechanger.app.ui.ConversionHistoryItem(
                    id = System.currentTimeMillis().toString(),
                    fileName = targetName,
                    modelName = selectedVoiceModel?.name ?: "Aoi Cyber Girl",
                    duration = durationStr,
                    timestamp = "Just now",
                    latencyMs = elapsedMs,
                    audioFile = outputFile
                )
                historyItems.add(0, newHistory)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isConverting = false
                conversionProgress = 1.0f
            }
        }
    }

    // Permission launcher for microphone recording
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            activeSubScreen = "RECORD_BOOTH"
        }
    }

    // Context for ContentResolver
    val context = androidx.compose.ui.platform.LocalContext.current

    // SAF Audio Picker Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val dest = File(cacheDir, "imported_${System.currentTimeMillis()}.wav")
            coroutineScope.launch {
                // Universal decoder: Handles MP3, M4A, AAC, FLAC, WAV into PCM WAV
                val success = com.smartvoicechanger.app.audio.AudioDecoderHelper.decodeToPcmWav(context, uri, dest)
                if (success && dest.exists()) {
                    tuningFile = dest
                    activeSubScreen = "AUDIO_TUNING"
                } else {
                    // Fallback to raw copy if decoder had issue
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (dest.exists()) {
                            tuningFile = dest
                            activeSubScreen = "AUDIO_TUNING"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    if (activeSubScreen != "NONE") {
        when (activeSubScreen) {
            "RECORD_BOOTH" -> RecordBoothScreen(
                recorderHelper = recorderHelper,
                audioPipeline = audioPipeline,
                cacheDir = cacheDir,
                onBack = { activeSubScreen = "NONE" },
                onProceedToTuning = { file ->
                    tuningFile = file
                    activeSubScreen = "AUDIO_TUNING"
                }
            )
            "LINK_INGEST" -> LinkStreamScreen(
                cacheDir = cacheDir,
                onBack = { activeSubScreen = "NONE" },
                onProceedToTuning = { file ->
                    tuningFile = file
                    activeSubScreen = "AUDIO_TUNING"
                }
            )
            "AUDIO_TUNING" -> tuningFile?.let { file ->
                AudioTuningScreen(
                    inputFile = file,
                    audioPipeline = audioPipeline,
                    onBack = { activeSubScreen = "NONE" },
                    onStartConversion = { sourceFile, targetModelName, pitch ->
                        processConversion(sourceFile, sourceFile.name, pitch)
                    }
                )
            }
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // Only render TopBar on tabs 1 (Models), 2 (Activity), 3 (Hardware).
            // On Tab 0 (Home), the header is unified in-line!
            if (currentTab != 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040614))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (currentTab) {
                            1 -> {
                                // Models Top Bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SvcColors.PurpleNeon.copy(alpha = 0.2f))
                                            .border(1.dp, SvcColors.PurpleNeon.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FuturisticModelsIcon(selected = true, size = 20.dp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "VOICE MODELS",
                                        color = SvcColors.TextPrimary,
                                        fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProBadge()
                                }
                            }
                            2 -> {
                                // Activity Top Bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SvcColors.CyanNeon.copy(alpha = 0.2f))
                                            .border(1.dp, SvcColors.CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FuturisticActivityIcon(selected = true, size = 20.dp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "ACTIVITY & QUEUE",
                                        color = SvcColors.TextPrimary,
                                        fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                ProBadge()
                            }
                            3 -> {
                                // Hardware Top Bar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SvcColors.GreenNeon.copy(alpha = 0.2f))
                                            .border(1.dp, SvcColors.GreenNeon.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FuturisticHardwareIcon(selected = true, size = 20.dp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "HARDWARE ENGINE",
                                        color = SvcColors.TextPrimary,
                                        fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                ProBadge()
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant, floating translucent Navigation Bar with exactly: 1. Home, 2. Models, 3. Activity, 4. Hardware
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xE6070B16))
                        .border(1.dp, SvcColors.CyanNeon.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTabItem(
                        selected = currentTab == 0,
                        title = stringResource(id = R.string.nav_home),
                        iconContent = { FuturisticHomeIcon(selected = currentTab == 0, size = 22.dp) },
                        onClick = { currentTab = 0 }
                    )
                    NavTabItem(
                        selected = currentTab == 1,
                        title = stringResource(id = R.string.nav_models),
                        iconContent = { FuturisticModelsIcon(selected = currentTab == 1, size = 22.dp) },
                        onClick = { currentTab = 1 }
                    )
                    NavTabItem(
                        selected = currentTab == 2,
                        title = stringResource(id = R.string.nav_activity),
                        iconContent = { FuturisticActivityIcon(selected = currentTab == 2, size = 22.dp) },
                        onClick = { currentTab = 2 }
                    )
                    NavTabItem(
                        selected = currentTab == 3,
                        title = stringResource(id = R.string.nav_hardware),
                        iconContent = { FuturisticHardwareIcon(selected = currentTab == 3, size = 22.dp) },
                        onClick = { currentTab = 3 }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (currentTab == 0) 0.dp else innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .background(Color.Transparent)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    activeBackendName = "ARM NEON CPU",
                    lastMeasuredLatencyMs = lastMeasuredLatencyMs,
                    onPickAudio = { audioPickerLauncher.launch("audio/*") },
                    onRecordAudio = {
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    },
                    onFromLink = {
                        activeSubScreen = "LINK_INGEST"
                    }
                )
                1 -> VoiceLibraryScreen(
                    modelsDir = modelsDir,
                    onModelSelected = { model ->
                        selectedVoiceModel = model
                        currentTab = 2 // Go to Activity with selected model
                    }
                )
                2 -> ActivityScreen(
                    pipeline = audioPipeline,
                    isConverting = isConverting,
                    progress = conversionProgress,
                    currentFileName = selectedAudioFile?.name,
                    currentModelName = selectedVoiceModel?.name,
                    historyItems = historyItems,
                    isRecording = isRecording,
                    recordingDurationSeconds = recordingDurationSeconds,
                    onStopRecording = {
                        isRecording = false
                        recorderHelper.stopRecording()
                    }
                )
                3 -> HardwareMonitorScreen(
                    capabilities = capabilities,
                    onRunDiagnosticsClick = {
                        diagnosticEngine.runVulkanComputeBenchmark()
                    }
                )
            }
        }
    }
}

@Composable
fun NavTabItem(
    selected: Boolean,
    title: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        // Glowing cyan pill indicator above active tab
        if (selected) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SvcColors.CyanNeon)
            )
            Spacer(modifier = Modifier.height(3.dp))
        } else {
            Spacer(modifier = Modifier.height(5.5.dp))
        }

        iconContent()
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title.uppercase(),
            color = if (selected) SvcColors.CyanNeon else SvcColors.TextMuted,
            fontFamily = com.smartvoicechanger.app.ui.theme.SvcFonts.Orbitron,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun ProBadge() {
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
            Text(stringResource(id = R.string.badge_pro), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
