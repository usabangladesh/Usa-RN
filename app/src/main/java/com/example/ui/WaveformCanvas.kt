package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PinkCyber
import com.example.ui.theme.VioletElectric
import kotlin.math.sin

@Composable
fun WaveformCanvas(
    amplitude: Float,
    state: AssistantState,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val barCount = 36
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalSpacing = canvasWidth * 0.25f
        val barWidth = (canvasWidth - totalSpacing) / barCount
        val barSpacing = totalSpacing / (barCount - 1)

        val isActive = state == AssistantState.Listening || state == AssistantState.Speaking || state == AssistantState.Processing

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            // Bell curve envelope in center
            val envelope = sin(normalizedX * Math.PI).toFloat()

            val waveMod = if (isActive) {
                sin((i * 0.45f) + phase).coerceAtLeast(0.1f)
            } else {
                0.15f
            }

            val dynamicFactor = if (isActive) {
                (amplitude * 1.8f).coerceIn(0.15f, 1.0f)
            } else {
                0.08f
            }

            val calculatedHeight = (canvasHeight * envelope * waveMod * dynamicFactor).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val y = (canvasHeight - calculatedHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CyanNeon,
                        VioletElectric,
                        PinkCyber
                    ),
                    startY = y,
                    endY = y + calculatedHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, calculatedHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
