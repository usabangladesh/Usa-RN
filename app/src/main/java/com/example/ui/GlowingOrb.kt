package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantState
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.PinkCyber
import com.example.ui.theme.VioletElectric
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlowingOrb(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbAnimations")

    // Slow organic breathing
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Fast rotation for rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Pulse wave for listening/speaking
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleRadius"
    )

    val (primaryColor, secondaryColor, accentColor) = when (state) {
        AssistantState.Disconnected -> Triple(
            Color(0xFF1E293B),
            Color(0xFF0EA5E9),
            Color(0xFF38BDF8)
        )
        AssistantState.Connecting -> Triple(
            CyanNeon,
            VioletElectric,
            Color(0xFF00F0FF)
        )
        AssistantState.Listening -> Triple(
            CyanNeon,
            EmeraldGlow,
            Color(0xFF22D3EE)
        )
        AssistantState.Processing -> Triple(
            VioletElectric,
            PinkCyber,
            CyanNeon
        )
        AssistantState.Speaking -> Triple(
            VioletElectric,
            PinkCyber,
            CyanNeon
        )
        AssistantState.Error -> Triple(
            CrimsonStop,
            Color(0xFFF97316),
            Color(0xFFDC2626)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.62f
            val audioBoost = (amplitude * 24f).coerceIn(0f, 30f)
            val dynamicRadius = (baseRadius * breathScale) + audioBoost

            // 1. Outer diffuse glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f),
                        secondaryColor.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.5f
                ),
                radius = dynamicRadius * 1.5f,
                center = center
            )

            // 2. Animated ripple wave when listening or speaking
            if (state == AssistantState.Listening || state == AssistantState.Speaking) {
                val waveAlpha = (1f - (rippleRadius - 0.7f) / 0.65f).coerceIn(0f, 0.6f)
                drawCircle(
                    color = primaryColor.copy(alpha = waveAlpha),
                    radius = dynamicRadius * rippleRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // 3. Orbital dotted ring
            val orbitalRadius = dynamicRadius * 1.18f
            val dotCount = 18
            val radAngle = Math.toRadians(rotationAngle.toDouble())
            for (i in 0 until dotCount) {
                val angle = radAngle + (i * (2 * Math.PI / dotCount))
                val dotX = center.x + (orbitalRadius * cos(angle)).toFloat()
                val dotY = center.y + (orbitalRadius * sin(angle)).toFloat()
                val dotAlpha = if (i % 2 == 0) 0.8f else 0.35f
                drawCircle(
                    color = accentColor.copy(alpha = dotAlpha),
                    radius = if (i % 3 == 0) 3.5f else 2.0f,
                    center = Offset(dotX, dotY)
                )
            }

            // 4. Secondary inner rotating dashed ring
            val innerRingRadius = dynamicRadius * 0.92f
            drawCircle(
                color = secondaryColor.copy(alpha = 0.5f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 5. Solid vibrant core sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        accentColor.copy(alpha = 0.90f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.70f)
                    ),
                    center = Offset(center.x - dynamicRadius * 0.2f, center.y - dynamicRadius * 0.2f),
                    radius = dynamicRadius
                ),
                radius = dynamicRadius * 0.78f,
                center = center
            )

            // 6. High-tech center specular highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = dynamicRadius * 0.16f,
                center = Offset(center.x - dynamicRadius * 0.25f, center.y - dynamicRadius * 0.25f)
            )
        }
    }
}
