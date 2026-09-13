package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantState
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.PinkCyber
import com.example.ui.theme.VioletElectric
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Point with projection and rotation math
 */
data class Point3D(val x: Float, val y: Float, val z: Float) {
    fun rotateX(radians: Float): Point3D {
        val cosA = cos(radians)
        val sinA = sin(radians)
        return Point3D(
            x = x,
            y = y * cosA - z * sinA,
            z = y * sinA + z * cosA
        )
    }

    fun rotateY(radians: Float): Point3D {
        val cosA = cos(radians)
        val sinA = sin(radians)
        return Point3D(
            x = x * cosA + z * sinA,
            y = y,
            z = -x * sinA + z * cosA
        )
    }

    fun rotateZ(radians: Float): Point3D {
        val cosA = cos(radians)
        val sinA = sin(radians)
        return Point3D(
            x = x * cosA - y * sinA,
            y = x * sinA + y * cosA,
            z = z
        )
    }

    fun project(centerX: Float, centerY: Float, fov: Float = 340f, cameraDistance: Float = 420f): Offset {
        val depth = z + cameraDistance
        val scale = if (depth > 1f) fov / depth else 1f
        return Offset(
            x = centerX + x * scale,
            y = centerY + y * scale
        )
    }
}

/**
 * Real-Time 3D AI Character for Rashed AI.
 * Renders a futuristic robotic humanoid AI avatar with 3D perspective projection,
 * real-time facial expressions, gaze tracking, audio-synced lip sync, breathing dynamics,
 * floating cyber halo rings, and interactive 3D drag rotation!
 */
@Composable
fun Character3D(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val coroutineScope = rememberCoroutineScope()

    // Interactive Drag Rotations
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }

    // Natural Animations (Breathing, Floating Bob, Ring Rotation)
    val infiniteTransition = rememberInfiniteTransition(label = "3DCharacterAnimations")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingBob"
    )

    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    // Eye Blink Controller (periodic organic blinking)
    var eyeOpenness by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200L + (Math.random() * 2000).toLong())
            // Blink down
            eyeOpenness = 0.05f
            delay(120)
            // Blink up
            eyeOpenness = 1f
        }
    }

    // Dynamic Color Coding based on Assistant State
    val (primaryGlow, secondaryGlow, accentColor) = when (state) {
        AssistantState.Disconnected -> Triple(
            Color(0xFF38BDF8),
            Color(0xFF0284C7),
            Color(0xFF64748B)
        )
        AssistantState.Connecting -> Triple(
            CyanNeon,
            VioletElectric,
            Color(0xFF00E5FF)
        )
        AssistantState.Listening -> Triple(
            CyanNeon,
            EmeraldGlow,
            Color(0xFF34D399)
        )
        AssistantState.Processing -> Triple(
            VioletElectric,
            PinkCyber,
            CyanNeon
        )
        AssistantState.Speaking -> Triple(
            PinkCyber,
            CyanNeon,
            VioletElectric
        )
        AssistantState.Error -> Triple(
            CrimsonStop,
            Color(0xFFF97316),
            Color(0xFFDC2626)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("character_3d_view")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Spring back to centered view
                        coroutineScope.launch {
                            rotX.animateTo(0f, spring(stiffness = 300f))
                        }
                        coroutineScope.launch {
                            rotY.animateTo(0f, spring(stiffness = 300f))
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        rotY.snapTo((rotY.value + dragAmount.x * 0.008f).coerceIn(-0.6f, 0.6f))
                        rotX.snapTo((rotX.value - dragAmount.y * 0.008f).coerceIn(-0.4f, 0.4f))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = this.size.width / 2f
            val centerY = (this.size.height / 2f) + floatOffset

            // Base rotation angles combining touch input and state attitude
            val listeningTiltY = if (state == AssistantState.Listening) 0.08f else 0f
            val listeningTiltX = if (state == AssistantState.Listening) 0.05f else 0f
            val speakingNod = if (state == AssistantState.Speaking) (amplitude * 0.12f) else 0f

            val totalRotY = rotY.value + listeningTiltY
            val totalRotX = rotX.value + listeningTiltX + speakingNod
            val totalRotZ = if (state == AssistantState.Listening) 0.04f else 0f

            fun transform(point: Point3D): Point3D {
                return point
                    .rotateX(totalRotX)
                    .rotateY(totalRotY)
                    .rotateZ(totalRotZ)
            }

            // 1. Ambient Background Glow Behind Character
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.28f),
                        secondaryGlow.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = this.size.minDimension * 0.65f
                ),
                radius = this.size.minDimension * 0.65f,
                center = Offset(centerX, centerY)
            )

            // 2. 3D Orbiting Halo Rings
            draw3DHaloRings(
                centerX = centerX,
                centerY = centerY - 75f,
                rotationAngle = haloRotation,
                transform = ::transform,
                color = primaryGlow,
                accentColor = secondaryGlow
            )

            // 3. 3D Floating Cyber Shoulders & Holographic Chest Arc Core
            draw3DShouldersAndChest(
                centerX = centerX,
                centerY = centerY + 80f,
                transform = ::transform,
                primaryColor = primaryGlow,
                secondaryColor = secondaryGlow,
                corePulse = corePulse,
                amplitude = amplitude,
                state = state
            )

            // 4. 3D Cyber Head Chassis & Helmet Architecture
            draw3DHeadChassis(
                centerX = centerX,
                centerY = centerY - 15f,
                transform = ::transform,
                primaryColor = primaryGlow,
                secondaryColor = secondaryGlow
            )

            // 5. 3D Visor, Expressive Glowing Optical Eyes, and Gaze
            draw3DFaceAndEyes(
                centerX = centerX,
                centerY = centerY - 15f,
                transform = ::transform,
                eyeOpenness = eyeOpenness,
                primaryColor = primaryGlow,
                accentColor = accentColor,
                state = state
            )

            // 6. 3D Real-Time Lip-Sync Mouth & Voice Audio Visualizer
            draw3DMouth(
                centerX = centerX,
                centerY = centerY + 28f,
                transform = ::transform,
                amplitude = amplitude,
                state = state,
                mouthColor = primaryGlow
            )

            // 7. 3D Swarm of Floating Ambient Cyber Particles
            draw3DParticleSwarm(
                centerX = centerX,
                centerY = centerY,
                time = haloRotation,
                transform = ::transform,
                particleColor = accentColor
            )
        }
    }
}

/**
 * 3D Orbiting Cyber Halo Rings with depth sorting
 */
private fun DrawScope.draw3DHaloRings(
    centerX: Float,
    centerY: Float,
    rotationAngle: Float,
    transform: (Point3D) -> Point3D,
    color: Color,
    accentColor: Color
) {
    val ringRadius = 90f
    val segmentCount = 36
    val ringRad = Math.toRadians(rotationAngle.toDouble()).toFloat()

    val ringPoints = mutableListOf<Point3D>()
    for (i in 0 until segmentCount) {
        val angle = (i * (2 * Math.PI / segmentCount)).toFloat()
        val localX = ringRadius * cos(angle)
        val localZ = ringRadius * sin(angle)
        val localY = 12f * sin(angle * 2f)

        // Pre-rotate by halo spin
        val cosSpin = cos(ringRad)
        val sinSpin = sin(ringRad)
        val spunX = localX * cosSpin - localZ * sinSpin
        val spunZ = localX * sinSpin + localZ * cosSpin

        ringPoints.add(transform(Point3D(spunX, localY, spunZ)))
    }

    for (i in 0 until segmentCount) {
        val p1 = ringPoints[i]
        val p2 = ringPoints[(i + 1) % segmentCount]
        val proj1 = p1.project(centerX, centerY)
        val proj2 = p2.project(centerX, centerY)

        // Depth fogging
        val alpha = ((p1.z + 120f) / 240f).coerceIn(0.18f, 0.9f)
        val strokeW = if (p1.z > 0) 2.2f else 1.2f

        drawLine(
            color = if (i % 4 == 0) accentColor.copy(alpha = alpha) else color.copy(alpha = alpha * 0.7f),
            start = proj1,
            end = proj2,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Occasional floating node beads
        if (i % 6 == 0) {
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = if (p1.z > 0) 3.5f else 2.0f,
                center = proj1
            )
        }
    }
}

/**
 * 3D Floating Shoulders, Cyber Neck and Holographic Chest Core
 */
private fun DrawScope.draw3DShouldersAndChest(
    centerX: Float,
    centerY: Float,
    transform: (Point3D) -> Point3D,
    primaryColor: Color,
    secondaryColor: Color,
    corePulse: Float,
    amplitude: Float,
    state: AssistantState
) {
    // Neck base
    val neckLeft = transform(Point3D(-24f, -48f, 0f)).project(centerX, centerY)
    val neckRight = transform(Point3D(24f, -48f, 0f)).project(centerX, centerY)
    val neckBottomL = transform(Point3D(-28f, -22f, 0f)).project(centerX, centerY)
    val neckBottomR = transform(Point3D(28f, -22f, 0f)).project(centerX, centerY)

    val neckPath = Path().apply {
        moveTo(neckLeft.x, neckLeft.y)
        lineTo(neckRight.x, neckRight.y)
        lineTo(neckBottomR.x, neckBottomR.y)
        lineTo(neckBottomL.x, neckBottomL.y)
        close()
    }
    drawPath(neckPath, color = Color(0xFF0F172A))
    drawPath(neckPath, color = secondaryColor.copy(alpha = 0.5f), style = Stroke(1.5f))

    // Cyber Shoulders Arc
    val shoulderFarL = transform(Point3D(-86f, -12f, -15f)).project(centerX, centerY)
    val shoulderL = transform(Point3D(-55f, -18f, 8f)).project(centerX, centerY)
    val chestCenter = transform(Point3D(0f, -5f, 22f)).project(centerX, centerY)
    val shoulderR = transform(Point3D(55f, -18f, 8f)).project(centerX, centerY)
    val shoulderFarR = transform(Point3D(86f, -12f, -15f)).project(centerX, centerY)

    val torsoBottom = transform(Point3D(0f, 40f, 15f)).project(centerX, centerY)
    val torsoL = transform(Point3D(-42f, 32f, 5f)).project(centerX, centerY)
    val torsoR = transform(Point3D(42f, 32f, 5f)).project(centerX, centerY)

    val armorPath = Path().apply {
        moveTo(shoulderFarL.x, shoulderFarL.y)
        lineTo(shoulderL.x, shoulderL.y)
        lineTo(chestCenter.x, chestCenter.y)
        lineTo(shoulderR.x, shoulderR.y)
        lineTo(shoulderFarR.x, shoulderFarR.y)
        lineTo(torsoR.x, torsoR.y)
        lineTo(torsoBottom.x, torsoBottom.y)
        lineTo(torsoL.x, torsoL.y)
        close()
    }

    // Armor Plate fill
    drawPath(
        path = armorPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A),
                Color(0xFF080D1A)
            ),
            startY = shoulderL.y - 10f,
            endY = torsoBottom.y + 10f
        )
    )
    drawPath(armorPath, color = primaryColor.copy(alpha = 0.6f), style = Stroke(width = 1.8f))

    // Shoulder Circuit Lines
    drawLine(
        color = secondaryColor.copy(alpha = 0.75f),
        start = shoulderL,
        end = chestCenter,
        strokeWidth = 2f
    )
    drawLine(
        color = secondaryColor.copy(alpha = 0.75f),
        start = shoulderR,
        end = chestCenter,
        strokeWidth = 2f
    )

    // Holographic Arc Reactor / Chest Core
    val coreRadius = (16f * corePulse) + (amplitude * 18f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                primaryColor,
                secondaryColor.copy(alpha = 0.6f),
                Color.Transparent
            ),
            center = chestCenter,
            radius = coreRadius * 2.2f
        ),
        radius = coreRadius * 2.2f,
        center = chestCenter
    )
    drawCircle(
        color = Color.White,
        radius = coreRadius * 0.45f,
        center = chestCenter
    )
    drawCircle(
        color = primaryColor,
        radius = coreRadius,
        center = chestCenter,
        style = Stroke(width = 2.5f)
    )
}

/**
 * 3D Cyber Head Chassis & Helmet
 */
private fun DrawScope.draw3DHeadChassis(
    centerX: Float,
    centerY: Float,
    transform: (Point3D) -> Point3D,
    primaryColor: Color,
    secondaryColor: Color
) {
    // 3D Helmet Geometry
    val topCrown = transform(Point3D(0f, -88f, 0f)).project(centerX, centerY)
    val templeL = transform(Point3D(-62f, -50f, 10f)).project(centerX, centerY)
    val templeR = transform(Point3D(62f, -50f, 10f)).project(centerX, centerY)
    val earL = transform(Point3D(-68f, -18f, -5f)).project(centerX, centerY)
    val earR = transform(Point3D(68f, -18f, -5f)).project(centerX, centerY)
    val jawL = transform(Point3D(-46f, 32f, 15f)).project(centerX, centerY)
    val jawR = transform(Point3D(46f, 32f, 15f)).project(centerX, centerY)
    val chin = transform(Point3D(0f, 48f, 25f)).project(centerX, centerY)

    val helmetPath = Path().apply {
        moveTo(topCrown.x, topCrown.y)
        lineTo(templeR.x, templeR.y)
        lineTo(earR.x, earR.y)
        lineTo(jawR.x, jawR.y)
        lineTo(chin.x, chin.y)
        lineTo(jawL.x, jawL.y)
        lineTo(earL.x, earL.y)
        lineTo(templeL.x, templeL.y)
        close()
    }

    // Faceted shading
    drawPath(
        path = helmetPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF26334D),
                Color(0xFF141D30),
                Color(0xFF090E1A)
            ),
            center = Offset(centerX, centerY - 20f),
            radius = 95f
        )
    )
    drawPath(
        path = helmetPath,
        color = primaryColor.copy(alpha = 0.5f),
        style = Stroke(width = 2.2f)
    )

    // Cyber Antennae / Ear Nodes
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyanNeon, Color(0xFF0B132B)),
            center = earL,
            radius = 12f
        ),
        radius = 9f,
        center = earL
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyanNeon, Color(0xFF0B132B)),
            center = earR,
            radius = 12f
        ),
        radius = 9f,
        center = earR
    )
    drawCircle(color = CyanNeon, radius = 9f, center = earL, style = Stroke(1.5f))
    drawCircle(color = CyanNeon, radius = 9f, center = earR, style = Stroke(1.5f))

    // Forehead cyber crest
    val crestTop = transform(Point3D(0f, -76f, 22f)).project(centerX, centerY)
    val crestBottom = transform(Point3D(0f, -44f, 32f)).project(centerX, centerY)
    val crestL = transform(Point3D(-14f, -55f, 28f)).project(centerX, centerY)
    val crestR = transform(Point3D(14f, -55f, 28f)).project(centerX, centerY)

    val crestPath = Path().apply {
        moveTo(crestTop.x, crestTop.y)
        lineTo(crestR.x, crestR.y)
        lineTo(crestBottom.x, crestBottom.y)
        lineTo(crestL.x, crestL.y)
        close()
    }
    drawPath(crestPath, color = secondaryColor.copy(alpha = 0.7f))
    drawPath(crestPath, color = primaryColor, style = Stroke(1.5f))
}

/**
 * 3D Visor, Expressive Glowing Optical Eyes, and Gaze
 */
private fun DrawScope.draw3DFaceAndEyes(
    centerX: Float,
    centerY: Float,
    transform: (Point3D) -> Point3D,
    eyeOpenness: Float,
    primaryColor: Color,
    accentColor: Color,
    state: AssistantState
) {
    // Holographic Visor Frame
    val visorTopL = transform(Point3D(-50f, -34f, 22f)).project(centerX, centerY)
    val visorTopR = transform(Point3D(50f, -34f, 22f)).project(centerX, centerY)
    val visorBottomR = transform(Point3D(42f, 2f, 26f)).project(centerX, centerY)
    val visorBottomL = transform(Point3D(-42f, 2f, 26f)).project(centerX, centerY)

    val visorPath = Path().apply {
        moveTo(visorTopL.x, visorTopL.y)
        lineTo(visorTopR.x, visorTopR.y)
        lineTo(visorBottomR.x, visorBottomR.y)
        lineTo(visorBottomL.x, visorBottomL.y)
        close()
    }

    // Visor Glass (Deep Tint with Holographic Sheen)
    drawPath(
        path = visorPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xEE030712),
                Color(0xDD0C1322),
                Color(0xEE030712)
            ),
            startY = visorTopL.y,
            endY = visorBottomL.y
        )
    )
    drawPath(visorPath, color = primaryColor.copy(alpha = 0.45f), style = Stroke(1.5f))

    // 3D Optical Eyes
    val eyeZ = 34f
    val eyeY = -16f
    val leftEyeCenter = transform(Point3D(-22f, eyeY, eyeZ)).project(centerX, centerY)
    val rightEyeCenter = transform(Point3D(22f, eyeY, eyeZ)).project(centerX, centerY)

    val eyeBaseWidth = 18f
    val eyeHeight = 12f * eyeOpenness.coerceAtLeast(0.08f)

    fun drawCyberEye(center: Offset) {
        // Eye Glow Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.85f),
                    accentColor.copy(alpha = 0.40f),
                    Color.Transparent
                ),
                center = center,
                radius = 20f
            ),
            radius = 20f,
            center = center
        )

        // Eye Outline / Lens
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(center.x - eyeBaseWidth / 2f, center.y - eyeHeight / 2f),
            size = Size(eyeBaseWidth, eyeHeight),
            cornerRadius = CornerRadius(4f, 4f),
            style = Fill
        )

        // Core Pupil
        if (eyeOpenness > 0.3f) {
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = center
            )
            // Cyber Iris detail lines
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(center.x - 7f, center.y),
                end = Offset(center.x + 7f, center.y),
                strokeWidth = 1.2f
            )
        }
    }

    drawCyberEye(leftEyeCenter)
    drawCyberEye(rightEyeCenter)

    // Expressive Brow Lines (Tilted in Listening/Thinking)
    val browTilt = when (state) {
        AssistantState.Listening -> -3f
        AssistantState.Processing -> 2.5f
        else -> 0f
    }
    val browL1 = transform(Point3D(-32f, -28f + browTilt, eyeZ)).project(centerX, centerY)
    val browL2 = transform(Point3D(-14f, -28f - browTilt, eyeZ)).project(centerX, centerY)
    val browR1 = transform(Point3D(14f, -28f - browTilt, eyeZ)).project(centerX, centerY)
    val browR2 = transform(Point3D(32f, -28f + browTilt, eyeZ)).project(centerX, centerY)

    drawLine(color = primaryColor.copy(alpha = 0.8f), start = browL1, end = browL2, strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(color = primaryColor.copy(alpha = 0.8f), start = browR1, end = browR2, strokeWidth = 2.5f, cap = StrokeCap.Round)
}

/**
 * 3D Real-Time Lip-Sync Mouth & Dynamic Speech Waves
 */
private fun DrawScope.draw3DMouth(
    centerX: Float,
    centerY: Float,
    transform: (Point3D) -> Point3D,
    amplitude: Float,
    state: AssistantState,
    mouthColor: Color
) {
    val mouthZ = 30f
    val mouthY = 0f
    val mouthCenter = transform(Point3D(0f, mouthY, mouthZ)).project(centerX, centerY)

    val isSpeaking = state == AssistantState.Speaking
    val mouthAperture = if (isSpeaking) (amplitude * 24f).coerceIn(4f, 22f) else 3f
    val mouthWidth = if (isSpeaking) (26f + amplitude * 12f) else 24f

    val mL = transform(Point3D(-mouthWidth / 2f, mouthY, mouthZ)).project(centerX, centerY)
    val mR = transform(Point3D(mouthWidth / 2f, mouthY, mouthZ)).project(centerX, centerY)

    if (isSpeaking && mouthAperture > 6f) {
        // Dynamic opening mouth oval with cyber glow
        val mouthPath = Path().apply {
            moveTo(mL.x, mL.y)
            quadraticTo(mouthCenter.x, mouthCenter.y - mouthAperture / 2f, mR.x, mR.y)
            quadraticTo(mouthCenter.x, mouthCenter.y + mouthAperture / 2f, mL.x, mL.y)
            close()
        }

        drawPath(
            path = mouthPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    mouthColor,
                    Color(0xFF030712)
                ),
                center = mouthCenter,
                radius = mouthAperture * 1.6f
            )
        )
        drawPath(mouthPath, color = mouthColor, style = Stroke(width = 1.8f))
    } else {
        // Sleek cyber smile / neutral mouth line
        val mouthPath = Path().apply {
            moveTo(mL.x, mL.y)
            quadraticTo(mouthCenter.x, mouthCenter.y + 3.5f, mR.x, mR.y)
        }
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    mouthColor.copy(alpha = 0.2f),
                    mouthColor,
                    Color.White,
                    mouthColor,
                    mouthColor.copy(alpha = 0.2f)
                ),
                startX = mL.x,
                endX = mR.x
            ),
            start = mL,
            end = mR,
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 3D Ambient Holographic Particle Swarm
 */
private fun DrawScope.draw3DParticleSwarm(
    centerX: Float,
    centerY: Float,
    time: Float,
    transform: (Point3D) -> Point3D,
    particleColor: Color
) {
    val particleCount = 18
    val radTime = Math.toRadians(time.toDouble()).toFloat()

    for (i in 0 until particleCount) {
        val angle = (i * (2 * Math.PI / particleCount)).toFloat() + (radTime * 0.7f)
        val radius = 100f + 25f * sin(i.toFloat())
        val pY = -90f + (i * 12f) + (15f * cos(angle * 2f))
        val pX = radius * cos(angle)
        val pZ = radius * sin(angle)

        val projected = transform(Point3D(pX, pY, pZ)).project(centerX, centerY)
        val alpha = ((pZ + 120f) / 240f).coerceIn(0.15f, 0.85f)
        val pSize = if (pZ > 0) 2.6f else 1.4f

        drawCircle(
            color = particleColor.copy(alpha = alpha),
            radius = pSize,
            center = projected
        )
    }
}
