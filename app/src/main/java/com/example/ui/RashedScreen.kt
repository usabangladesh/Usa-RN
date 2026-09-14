package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState
import com.example.assistant.RashedAssistantEngine
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

@Composable
fun RashedScreen(
    assistantEngine: RashedAssistantEngine,
    memoryManager: MemoryManager,
    permissionManager: PermissionManager,
    modifier: Modifier = Modifier
) {
    val state by assistantEngine.state.collectAsState()
    val transcript by assistantEngine.currentTranscript.collectAsState()
    val assistantResponse by assistantEngine.lastAssistantResponse.collectAsState()
    val lastActionStatus by assistantEngine.lastActionStatus.collectAsState()
    val pendingConfirmation by assistantEngine.pendingConfirmation.collectAsState()
    val amplitude by assistantEngine.audioAmplitude.collectAsState()

    var isApiKeyDialogOpen by remember { mutableStateOf(false) }
    var isCharacterMode by remember { mutableStateOf(false) }

    val quickVoiceSparks = remember {
        listOf(
            "Hey Mahi, tease me!",
            "Open website YouTube",
            "How's my battery?",
            "Search latest tech news",
            "Tell me something witty",
            "Open Camera",
            "Who is the smartest AI?",
            "Browse to reddit.com"
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
            // Ambient Futuristic Background Glows
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VioletElectric.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

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
                    // Brand Identity
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        when (state) {
                                            AssistantState.Listening -> EmeraldGlow
                                            AssistantState.Speaking -> PinkCyber
                                            AssistantState.Connecting, AssistantState.Processing -> CyanNeon
                                            else -> TextMuted
                                        },
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
                                text = "MAHI AI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Confident, witty, and delightfully yours.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Top Action Icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Visual Mode Switch: Holographic Orb vs 3D Avatar
                        IconButton(
                            onClick = { isCharacterMode = !isCharacterMode },
                            modifier = Modifier
                                .size(40.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                                .testTag("toggle_avatar_mode")
                        ) {
                            Icon(
                                imageVector = if (isCharacterMode) Icons.Default.BlurOn else Icons.Default.Face,
                                contentDescription = "Switch Visual Mode",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Gemini API Key Settings
                        IconButton(
                            onClick = { isApiKeyDialogOpen = true },
                            modifier = Modifier
                                .size(40.dp)
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

                        // Stop / Disconnect Button (active when session running)
                        if (state != AssistantState.Disconnected) {
                            IconButton(
                                onClick = { assistantEngine.emergencyStop() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CrimsonStop.copy(alpha = 0.2f), CircleShape)
                                    .testTag("emergency_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = CrimsonStop,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Real-Time Visual States: Centerpiece Orb or 3D Character
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (isCharacterMode) {
                        Character3D(
                            state = state,
                            amplitude = amplitude,
                            size = 230.dp,
                            modifier = Modifier.testTag("ai_3d_character")
                        )
                    } else {
                        GlowingOrb(
                            state = state,
                            amplitude = amplitude,
                            size = 220.dp,
                            modifier = Modifier
                                .clickable { assistantEngine.toggleVoiceSession() }
                                .testTag("ai_glowing_orb")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Audio Waveform Canvas
                WaveformCanvas(
                    amplitude = amplitude,
                    state = state,
                    height = 42.dp,
                    modifier = Modifier.testTag("waveform_canvas")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Visual State Indicator Badge
                StateIndicatorBadge(state = state)

                Spacer(modifier = Modifier.height(16.dp))

                // Floating Spoken Voice Banner (No persistent text chat log!)
                AnimatedVisibility(
                    visible = assistantResponse.isNotBlank() || transcript.isNotBlank(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_interaction_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(
                            1.dp,
                            if (state == AssistantState.Speaking) PinkCyber.copy(alpha = 0.5f)
                            else DarkSurfaceBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // User speech echo
                            if (transcript.isNotBlank()) {
                                Text(
                                    text = "You: \"$transcript\"",
                                    color = CyanNeon,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Mahi's live witty voice response
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (state == AssistantState.Speaking) PinkCyber else TextMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 3.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = assistantResponse,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 22.sp
                                )
                            }

                            if (!lastActionStatus.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = lastActionStatus ?: "",
                                    color = EmeraldGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Security Confirmation Card (if pending action)
                AnimatedVisibility(
                    visible = pendingConfirmation != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    pendingConfirmation?.let { conf ->
                        Spacer(modifier = Modifier.height(14.dp))
                        ConfirmationCard(
                            confirmation = conf,
                            onConfirm = { assistantEngine.confirmPending() },
                            onCancel = { assistantEngine.cancelPending() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick Voice Spark Chips
                Text(
                    text = "Try saying or tap to ask:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (spark in quickVoiceSparks) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkSurfaceBorder),
                            modifier = Modifier.clickable {
                                assistantEngine.handleUserVoiceInput(spark)
                            }
                        ) {
                            Text(
                                text = spark,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Central Mic / Power Button with Smooth Pulsing Animations
                CentralMicPowerButton(
                    state = state,
                    amplitude = amplitude,
                    onToggle = { assistantEngine.toggleVoiceSession() }
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
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
        AssistantState.Disconnected -> Pair("IDLE • Tap Mic or Core to Connect", TextSecondary)
        AssistantState.Connecting -> Pair("Connecting to Gemini Live...", CyanNeon)
        AssistantState.Listening -> Pair("Listening to you...", CyanNeon)
        AssistantState.Processing -> Pair("Mahi is thinking...", VioletElectric)
        AssistantState.Speaking -> Pair("Mahi is speaking...", PinkCyber)
        AssistantState.Error -> Pair("Connection notice or mic permission needed", CrimsonStop)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(tint, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

/**
 * Central Mic/Power button with glowing multi-layer pulsing rings and smooth animations
 */
@Composable
private fun CentralMicPowerButton(
    state: AssistantState,
    amplitude: Float,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CentralMicPulse")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val isLive = state == AssistantState.Listening || state == AssistantState.Speaking || state == AssistantState.Processing
    val isSpeaking = state == AssistantState.Speaking
    val isDisconnected = state == AssistantState.Disconnected

    val activeGlowColor = if (isSpeaking) PinkCyber else CyanNeon

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(112.dp)
    ) {
        // Outer ripple 2
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulse2 + (amplitude * 0.4f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeGlowColor.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }

        // Inner ripple 1
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .scale(pulse1 + (amplitude * 0.25f))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeGlowColor.copy(alpha = 0.38f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }

        // Primary Central Power / Mic Action Button
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSpeaking -> Brush.linearGradient(listOf(PinkCyber, VioletElectric))
                        isLive -> Brush.linearGradient(listOf(CyanNeon, Color(0xFF0099FF)))
                        else -> Brush.linearGradient(listOf(DarkSurfaceElevated, DarkSurface))
                    }
                )
                .shadow(
                    elevation = if (isLive) 16.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = if (isLive) activeGlowColor else Color.Transparent,
                    spotColor = if (isLive) activeGlowColor else Color.Transparent
                )
                .testTag("main_mic_button")
        ) {
            Icon(
                imageVector = if (isDisconnected) Icons.Default.PowerSettingsNew else Icons.Default.Mic,
                contentDescription = if (isLive) "Disconnect voice session" else "Connect voice session",
                tint = if (isLive) Color(0xFF030712) else CyanNeon,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
