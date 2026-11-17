package com.smartvoicechanger.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartvoicechanger.app.ui.theme.SvcColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Cyberpunk / Futuristic Vector Icon Set for Smart Voice Changer.
 * Designed with glowing cyber-circuits, audio frequencies, neural nodes, and quantum rays.
 */

@Composable
fun FuturisticAudioDiscIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = SvcColors.PurpleNeon
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val outerRadius = w * 0.44f

        // Outer neon holographic ring
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(tint, Color(0xFFE879F9), Color(0xFF6366F1), tint),
                center = center
            ),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Concentric track grooves
        drawCircle(
            color = tint.copy(alpha = 0.45f),
            radius = outerRadius * 0.72f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Center holographic core
        drawCircle(
            color = Color.White,
            radius = outerRadius * 0.22f,
            center = center
        )
        drawCircle(
            color = tint,
            radius = outerRadius * 0.35f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Futuristic audio soundwave bars crossing out of disk
        val barWidth = 1.5.dp.toPx()
        val bars = listOf(
            Offset(w * 0.16f, h * 0.5f) to Offset(w * 0.28f, h * 0.5f),
            Offset(w * 0.72f, h * 0.5f) to Offset(w * 0.84f, h * 0.5f),
            Offset(w * 0.5f, h * 0.16f) to Offset(w * 0.5f, h * 0.28f),
            Offset(w * 0.5f, h * 0.72f) to Offset(w * 0.5f, h * 0.84f),
            Offset(w * 0.76f, h * 0.24f) to Offset(w * 0.86f, h * 0.14f)
        )
        for ((start, end) in bars) {
            drawLine(
                color = tint.copy(alpha = 0.85f),
                start = start,
                end = end,
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun FuturisticVocalMicIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = SvcColors.PinkNeon
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer Pulsing Acoustic Sonar Waves
        val waveColor = tint.copy(alpha = 0.5f)
        drawArc(
            color = waveColor,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.84f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = waveColor,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.84f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Futuristic Studio Capsule (rounded cylinder)
        val capsuleLeft = w * 0.36f
        val capsuleRight = w * 0.64f
        val capsuleTop = h * 0.15f
        val capsuleBottom = h * 0.58f

        val capsulePath = Path().apply {
            moveTo(capsuleLeft, capsuleTop + (capsuleRight - capsuleLeft) / 2f)
            arcTo(
                rect = Rect(capsuleLeft, capsuleTop, capsuleRight, capsuleTop + (capsuleRight - capsuleLeft)),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(capsuleRight, capsuleBottom)
            arcTo(
                rect = Rect(capsuleLeft, capsuleBottom - (capsuleRight - capsuleLeft), capsuleRight, capsuleBottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }

        drawPath(
            path = capsulePath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, tint, Color(0xFFBE185D))
            ),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // Cyber Grid Lines inside mic capsule
        drawLine(
            color = tint.copy(alpha = 0.7f),
            start = Offset(capsuleLeft + 2f, h * 0.35f),
            end = Offset(capsuleRight - 2f, h * 0.35f),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = tint.copy(alpha = 0.7f),
            start = Offset(capsuleLeft + 2f, h * 0.45f),
            end = Offset(capsuleRight - 2f, h * 0.45f),
            strokeWidth = 1.2.dp.toPx()
        )

        // Mic Cradle & Stand Base
        val cradlePath = Path().apply {
            moveTo(w * 0.26f, h * 0.42f)
            cubicTo(
                w * 0.26f, h * 0.74f,
                w * 0.74f, h * 0.74f,
                w * 0.74f, h * 0.42f
            )
        }
        drawPath(
            path = cradlePath,
            color = tint,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Stem & Laser Stand Base
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.69f),
            end = Offset(w * 0.5f, h * 0.88f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.32f, h * 0.88f),
            end = Offset(w * 0.68f, h * 0.88f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FuturisticCloudStreamIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = SvcColors.CyanNeon
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Quantum Data Link Orbit Ring
        drawArc(
            color = tint.copy(alpha = 0.4f),
            startAngle = -30f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.16f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.68f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Dual Interlocking Cyber Rings / Infinity Data Loop
        val ring1Center = Offset(w * 0.38f, h * 0.5f)
        val ring2Center = Offset(w * 0.62f, h * 0.5f)
        val ringRadius = w * 0.19f

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(tint, Color.White),
                start = Offset(w * 0.2f, h * 0.3f),
                end = Offset(w * 0.5f, h * 0.7f)
            ),
            radius = ringRadius,
            center = ring1Center,
            style = Stroke(width = 1.8.dp.toPx())
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, tint),
                start = Offset(w * 0.5f, h * 0.3f),
                end = Offset(w * 0.8f, h * 0.7f)
            ),
            radius = ringRadius,
            center = ring2Center,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Center Laser Pulse Bridge
        drawLine(
            color = Color.White,
            start = Offset(w * 0.38f, h * 0.5f),
            end = Offset(w * 0.62f, h * 0.5f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Data Transmission Beams (Top right & bottom left satellite pulses)
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(w * 0.84f, h * 0.22f))
        drawCircle(color = tint, radius = 1.6.dp.toPx(), center = Offset(w * 0.16f, h * 0.78f))
    }
}

// ==========================================
// NAVBAR FUTURISTIC ICONS
// ==========================================

@Composable
fun FuturisticHomeIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val tint = if (selected) SvcColors.CyanNeon else SvcColors.TextMuted
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Sleek Cyber Portal / Home Hexagon
        val portalPath = Path().apply {
            moveTo(w * 0.5f, h * 0.1f) // Apex roof
            lineTo(w * 0.88f, h * 0.42f)
            lineTo(w * 0.88f, h * 0.88f)
            lineTo(w * 0.58f, h * 0.88f)
            lineTo(w * 0.58f, h * 0.62f)
            lineTo(w * 0.42f, h * 0.62f)
            lineTo(w * 0.42f, h * 0.88f)
            lineTo(w * 0.12f, h * 0.88f)
            lineTo(w * 0.12f, h * 0.42f)
            close()
        }

        drawPath(
            path = portalPath,
            brush = if (selected) {
                Brush.verticalGradient(listOf(Color.White, SvcColors.CyanNeon))
            } else {
                Brush.linearGradient(listOf(tint, tint))
            },
            style = Stroke(width = if (selected) 2.dp.toPx() else 1.5.dp.toPx(), join = StrokeJoin.Round)
        )

        if (selected) {
            // Neon core dot inside portal
            drawCircle(
                color = SvcColors.CyanNeon,
                radius = 2.2.dp.toPx(),
                center = Offset(w * 0.5f, h * 0.44f)
            )
        }
    }
}

@Composable
fun FuturisticModelsIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val tint = if (selected) SvcColors.PurpleNeon else SvcColors.TextMuted
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Holographic Neural Core (Isometric 3D Cube / Neural Crystal Nodes)
        val r = w * 0.38f
        val p1 = Offset(cx, cy - r)
        val p2 = Offset(cx + r * 0.86f, cy - r * 0.5f)
        val p3 = Offset(cx + r * 0.86f, cy + r * 0.5f)
        val p4 = Offset(cx, cy + r)
        val p5 = Offset(cx - r * 0.86f, cy + r * 0.5f)
        val p6 = Offset(cx - r * 0.86f, cy - r * 0.5f)

        val hexPath = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            lineTo(p5.x, p5.y)
            lineTo(p6.x, p6.y)
            close()
        }

        drawPath(
            path = hexPath,
            color = tint,
            style = Stroke(width = if (selected) 1.8.dp.toPx() else 1.4.dp.toPx(), join = StrokeJoin.Round)
        )

        // Internal Y-axis isometric cube facet lines
        drawLine(color = tint, start = Offset(cx, cy), end = p1, strokeWidth = 1.2.dp.toPx())
        drawLine(color = tint, start = Offset(cx, cy), end = p3, strokeWidth = 1.2.dp.toPx())
        drawLine(color = tint, start = Offset(cx, cy), end = p5, strokeWidth = 1.2.dp.toPx())

        if (selected) {
            // Glowing AI Neural Core Dot
            drawCircle(color = Color.White, radius = 2.4.dp.toPx(), center = Offset(cx, cy))
        }
    }
}

@Composable
fun FuturisticActivityIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val tint = if (selected) SvcColors.CyanNeon else SvcColors.TextMuted
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Neon Audio Spectrum Pulse / Waveform
        val barWidth = 2.dp.toPx()
        val bars = listOf(
            Triple(w * 0.15f, h * 0.40f, h * 0.60f),
            Triple(w * 0.32f, h * 0.20f, h * 0.80f),
            Triple(w * 0.50f, h * 0.08f, h * 0.92f), // Peak frequency
            Triple(w * 0.68f, h * 0.25f, h * 0.75f),
            Triple(w * 0.85f, h * 0.45f, h * 0.55f)
        )

        for ((x, top, bottom) in bars) {
            drawLine(
                brush = if (selected) {
                    Brush.verticalGradient(
                        colors = listOf(SvcColors.CyanNeon, SvcColors.PurpleNeon),
                        startY = top,
                        endY = bottom
                    )
                } else {
                    Brush.linearGradient(listOf(tint, tint))
                },
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }

        if (selected) {
            // Peak audio indicator beam
            drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = Offset(w * 0.50f, h * 0.08f))
        }
    }
}

@Composable
fun FuturisticHardwareIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val tint = if (selected) SvcColors.CyanNeon else SvcColors.TextMuted
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Cyber Silicon Microchip (CPU/NPU)
        val chipLeft = w * 0.26f
        val chipTop = h * 0.26f
        val chipSize = w * 0.48f

        // Outer Chip Body
        drawRect(
            color = tint,
            topLeft = Offset(chipLeft, chipTop),
            size = androidx.compose.ui.geometry.Size(chipSize, chipSize),
            style = Stroke(width = if (selected) 1.8.dp.toPx() else 1.4.dp.toPx())
        )

        // Center Neural Core Square
        drawRect(
            brush = if (selected) {
                Brush.linearGradient(listOf(Color.White, SvcColors.CyanNeon))
            } else {
                Brush.linearGradient(listOf(tint.copy(alpha = 0.5f), tint.copy(alpha = 0.5f)))
            },
            topLeft = Offset(w * 0.40f, h * 0.40f),
            size = androidx.compose.ui.geometry.Size(w * 0.20f, h * 0.20f)
        )

        // Microchip PIN Traces (3 Pins on each of 4 sides)
        val pinLength = w * 0.14f
        val stroke = 1.3.dp.toPx()

        // Top pins
        drawLine(tint, Offset(w * 0.36f, chipTop), Offset(w * 0.36f, chipTop - pinLength), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, chipTop), Offset(w * 0.50f, chipTop - pinLength), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, chipTop), Offset(w * 0.64f, chipTop - pinLength), stroke, StrokeCap.Round)

        // Bottom pins
        val chipBottom = chipTop + chipSize
        drawLine(tint, Offset(w * 0.36f, chipBottom), Offset(w * 0.36f, chipBottom + pinLength), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, chipBottom), Offset(w * 0.50f, chipBottom + pinLength), stroke, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, chipBottom), Offset(w * 0.64f, chipBottom + pinLength), stroke, StrokeCap.Round)

        // Left pins
        drawLine(tint, Offset(chipLeft, h * 0.36f), Offset(chipLeft - pinLength, h * 0.36f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(chipLeft, h * 0.50f), Offset(chipLeft - pinLength, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(chipLeft, h * 0.64f), Offset(chipLeft - pinLength, h * 0.64f), stroke, StrokeCap.Round)

        // Right pins
        val chipRight = chipLeft + chipSize
        drawLine(tint, Offset(chipRight, h * 0.36f), Offset(chipRight + pinLength, h * 0.36f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(chipRight, h * 0.50f), Offset(chipRight + pinLength, h * 0.50f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(chipRight, h * 0.64f), Offset(chipRight + pinLength, h * 0.64f), stroke, StrokeCap.Round)
    }
}

/**
 * Pulsing Quantum Core / AI Radar Scanner Icon for the telemetry status bar
 */
@Composable
fun FuturisticRadarNodeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = SvcColors.GreenNeon
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)

        // Outer Hexagonal Radar Shell
        val r = w * 0.44f
        val hexPath = Path().apply {
            for (i in 0 until 6) {
                val angle = Math.toRadians((i * 60 - 30).toDouble())
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = hexPath,
            color = tint.copy(alpha = 0.5f),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // Concentric Inner Radar Wave
        drawCircle(
            color = tint.copy(alpha = 0.35f),
            radius = r * 0.62f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Blinking Active AI Core Dot
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = center
        )
        drawCircle(
            color = tint,
            radius = 4.2.dp.toPx(),
            center = center,
            style = Stroke(width = 1.4.dp.toPx())
        )

        // Crosshair Radar Blips
        drawLine(tint.copy(alpha = 0.7f), Offset(w * 0.15f, center.y), Offset(w * 0.3f, center.y), 1.2.dp.toPx())
        drawLine(tint.copy(alpha = 0.7f), Offset(w * 0.7f, center.y), Offset(w * 0.85f, center.y), 1.2.dp.toPx())
    }
}

/**
 * Telemetry Pulse / Oscilloscope Signal Icon for Latency Display
 */
@Composable
fun FuturisticOscilloscopeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = SvcColors.CyanNeon
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Digital Electrocardiogram / Audio Waveform Pulse Line
        val pulsePath = Path().apply {
            moveTo(0f, h * 0.55f)
            lineTo(w * 0.22f, h * 0.55f)
            lineTo(w * 0.35f, h * 0.15f) // Peak Spike
            lineTo(w * 0.52f, h * 0.90f) // Low Spike
            lineTo(w * 0.68f, h * 0.35f) // Secondary Peak
            lineTo(w * 0.80f, h * 0.55f)
            lineTo(w, h * 0.55f)
        }

        drawPath(
            path = pulsePath,
            brush = Brush.horizontalGradient(
                colors = listOf(tint.copy(alpha = 0.3f), Color.White, tint)
            ),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Peak Blip Node
        drawCircle(
            color = Color.White,
            radius = 1.8.dp.toPx(),
            center = Offset(w * 0.35f, h * 0.15f)
        )
    }
}

