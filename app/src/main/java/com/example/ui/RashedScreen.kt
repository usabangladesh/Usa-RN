package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState
import com.example.assistant.RashedAssistantEngine
import com.example.device.SamsungOptimizer
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.PinkCyber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RashedScreen(
    assistantEngine: RashedAssistantEngine,
    memoryManager: MemoryManager,
    permissionManager: PermissionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state by assistantEngine.state.collectAsState()
    val transcript by assistantEngine.currentTranscript.collectAsState()
    val assistantResponse by assistantEngine.lastAssistantResponse.collectAsState()
    val lastActionStatus by assistantEngine.lastActionStatus.collectAsState()
    val pendingConfirmation by assistantEngine.pendingConfirmation.collectAsState()
    val amplitude by assistantEngine.audioAmplitude.collectAsState()

    val memories by memoryManager.getAllMemoriesFlow().collectAsState(initial = emptyList())
    var isMemorySheetOpen by remember { mutableStateOf(false) }
    var isApiKeyDialogOpen by remember { mutableStateOf(false) }
    var isCharacterMode by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isSamsung = remember { SamsungOptimizer.isSamsungDevice() }
    val oneUiVersion = remember { SamsungOptimizer.getOneUiVersion() }
    val isAccessibilityEnabled = remember { permissionManager.isAccessibilityServiceEnabled() }

    val quickCommands = remember {
        listOf(
            "আমার battery কত?",
            "WhatsApp খুলে দাও",
            "YouTube চালু করো",
            "Volume কমাও",
            "ফোনটা lock করো",
            "Wi-Fi status কী?",
            "নোটিফিকেশন কী এসেছে?",
            "Camera খুলে দাও"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkCanvas
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & Tagline
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (state == AssistantState.Disconnected) TextMuted else CyanNeon,
                                        CircleShape
                                    )
                                    .shadow(
                                        8.dp,
                                        CircleShape,
                                        ambientColor = CyanNeon,
                                        spotColor = CyanNeon
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RASHED AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Your Voice. Your Intelligence. Your Control.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Action Icons: API Key Settings, Memory Drawer & Emergency Stop
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // API Key Configuration Button
                        IconButton(
                            onClick = { isApiKeyDialogOpen = true },
                            modifier = Modifier
                                .size(42.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                                .testTag("api_key_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Gemini API Key Settings",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Memory Button
                        IconButton(
                            onClick = { isMemorySheetOpen = true },
                            modifier = Modifier
                                .size(42.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                                .testTag("memory_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Memory and Settings",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Emergency Stop Button
                        IconButton(
                            onClick = { assistantEngine.emergencyStop() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(CrimsonStop.copy(alpha = 0.2f), CircleShape)
                                .shadow(6.dp, CircleShape, ambientColor = CrimsonStop, spotColor = CrimsonStop)
                                .testTag("emergency_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Emergency Stop",
                                tint = CrimsonStop,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Samsung & System Info Badge Pill
                Row(
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSamsung) "Galaxy • ${oneUiVersion ?: "One UI"}" else "Android Automation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyanNeon
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isAccessibilityEnabled) "✓ Service Active" else "⚠ Accessibility Setup",
                        fontSize = 12.sp,
                        color = if (isAccessibilityEnabled) EmeraldGlow else AmberAlert,
                        modifier = Modifier.clickable {
                            if (!isAccessibilityEnabled) {
                                permissionManager.openAccessibilitySettings()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Mode Selector: Real-Time 3D Character vs Holographic Orb
                Row(
                    modifier = Modifier
                        .background(DarkSurface, RoundedCornerShape(20.dp))
                        .padding(4.dp)
                        .testTag("avatar_mode_selector"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCharacterMode) CyanNeon.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { isCharacterMode = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("select_3d_character_mode")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "3D AI Character",
                                tint = if (isCharacterMode) CyanNeon else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3D AI Character",
                                fontSize = 12.sp,
                                fontWeight = if (isCharacterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCharacterMode) CyanNeon else TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!isCharacterMode) CyanNeon.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { isCharacterMode = false }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("select_orb_mode")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BlurOn,
                                contentDescription = "Holographic Orb",
                                tint = if (!isCharacterMode) CyanNeon else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Holographic Orb",
                                fontSize = 12.sp,
                                fontWeight = if (!isCharacterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isCharacterMode) CyanNeon else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Centerpiece: Real-Time 3D Character or Glowing AI Orb
                if (isCharacterMode) {
                    Character3D(
                        state = state,
                        amplitude = amplitude,
                        size = 250.dp,
                        modifier = Modifier.testTag("ai_3d_character")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "👆 3D মডেল ড্র্যাগ করে ঘুরিয়ে দেখুন • কণ্ঠস্বরের সাথে লাইভ মুখ ও অঙ্গভঙ্গি",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                } else {
                    GlowingOrb(
                        state = state,
                        amplitude = amplitude,
                        size = 210.dp,
                        modifier = Modifier.testTag("ai_glowing_orb")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Waveform Canvas
                WaveformCanvas(
                    amplitude = amplitude,
                    state = state,
                    height = 46.dp,
                    modifier = Modifier.testTag("waveform_canvas")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Assistant State Indicator Badge
                StateIndicatorBadge(state = state)

                Spacer(modifier = Modifier.height(18.dp))

                // Live Assistant Response & Transcript Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("assistant_response_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // User transcript header
                        if (transcript.isNotBlank()) {
                            Text(
                                text = "আপনি বলেছেন: \"$transcript\"",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Assistant voice response
                        Text(
                            text = assistantResponse,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 23.sp
                        )

                        // Action verification status tag
                        if (!lastActionStatus.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lastActionStatus ?: "",
                                color = EmeraldGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirmation Card (when WhatsApp or sensitive action is pending)
                AnimatedVisibility(
                    visible = pendingConfirmation != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    pendingConfirmation?.let { conf ->
                        ConfirmationCard(
                            confirmation = conf,
                            onConfirm = { assistantEngine.confirmPending() },
                            onCancel = { assistantEngine.cancelPending() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Command Suggestions
                Text(
                    text = "দ্রুত কমান্ড বলুন বা ট্যাপ করুন",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (cmd in quickCommands) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.clickable {
                                assistantEngine.handleUserVoiceInput(cmd)
                            }
                        ) {
                            Text(
                                text = cmd,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Mic FAB with Tactile Glowing Halo
                VoiceControlMicButton(
                    state = state,
                    onToggle = { assistantEngine.toggleVoiceSession() }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Memory Management Drawer Sheet
    if (isMemorySheetOpen) {
        MemorySheet(
            sheetState = sheetState,
            memories = memories,
            isMemoryEnabled = memoryManager.isMemoryEnabled,
            onToggleMemoryEnabled = { enabled -> memoryManager.isMemoryEnabled = enabled },
            onDeleteMemory = { id -> scope.launch { memoryManager.deleteMemory(id) } },
            onClearAll = { scope.launch { memoryManager.clearAll() } },
            onOpenApiKeyConfig = {
                isMemorySheetOpen = false
                isApiKeyDialogOpen = true
            },
            onDismiss = { isMemorySheetOpen = false }
        )
    }

    // Gemini API Key Config Dialog
    if (isApiKeyDialogOpen) {
        ApiKeyConfigDialog(
            onDismiss = { isApiKeyDialogOpen = false }
        )
    }
}

@Composable
private fun StateIndicatorBadge(state: AssistantState) {
    val (label, tint) = when (state) {
        AssistantState.Disconnected -> Pair("মাইক্রোফোনে ট্যাপ করুন (Ready)", TextSecondary)
        AssistantState.Connecting -> Pair("Gemini Live এর সাথে যুক্ত হচ্ছে...", CyanNeon)
        AssistantState.Listening -> Pair("শুনছি... আপনার নির্দেশ বলুন", CyanNeon)
        AssistantState.Processing -> Pair("ভাবছি ও কাজ প্রস্তুত করছি...", VioletElectric)
        AssistantState.Speaking -> Pair("Rashed উত্তর দিচ্ছে...", PinkCyber)
        AssistantState.Error -> Pair("সংযোগ ত্রুটি বা পারমিশন প্রয়োজন", CrimsonStop)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(tint, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
private fun VoiceControlMicButton(
    state: AssistantState,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicHalo")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val isListening = state == AssistantState.Listening || state == AssistantState.Speaking

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(96.dp)
    ) {
        // Outer glowing halo
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(haloPulse)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyanNeon.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }

        // Primary tactile button
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (isListening) CyanNeon else DarkSurfaceElevated
                )
                .shadow(
                    elevation = if (isListening) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = CyanNeon,
                    spotColor = CyanNeon
                )
                .testTag("main_mic_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
                tint = if (isListening) Color(0xFF030712) else CyanNeon,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}
