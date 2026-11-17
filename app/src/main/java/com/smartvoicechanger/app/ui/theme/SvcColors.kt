package com.smartvoicechanger.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object SvcColors {
    // Ultra-deep dark immersive canvas (Image 1 style)
    val Background = Color(0xFF03050C)
    val BackgroundElevated = Color(0xFF070B18)

    // Glowing Neon Accents
    val CyanNeon = Color(0xFF00E5FF)
    val CyanSoft = Color(0xFF38BDF8)
    val PurpleNeon = Color(0xFFA855F7)
    val MagentaNeon = Color(0xFFD946EF)
    val PinkNeon = Color(0xFFEC4899)
    val BlueNeon = Color(0xFF3B82F6)
    val GreenNeon = Color(0xFF10B981)

    // Premium Translucent Glass Surfaces
    val GlassCardBg = Color(0xE60A0F1E)
    val GlassCardBgHover = Color(0xF010182E)
    val GlassCardBorder = Color(0x3338BDF8)
    val GlassCardBorderHighlight = Color(0x6600E5FF)

    // Fallbacks
    val CardBg = Color(0xCC080D1A)
    val CardBorder = Color(0x2438BDF8)
    val CardBorderHighlight = Color(0x4D00E5FF)

    // Typography
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    // Gradients
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(CyanNeon, PurpleNeon)
    )

    val ButtonGradient = Brush.horizontalGradient(
        colors = listOf(CyanNeon, BlueNeon, PurpleNeon)
    )

    val ProBadgeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFA000), Color(0xFFFF5722))
    )

    val CyanGlowBorderGradient = Brush.horizontalGradient(
        colors = listOf(CyanNeon, BlueNeon, PurpleNeon)
    )

    val TopHeroScrimGradient = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x3303050C),
            Color(0xCC03050C),
            Color(0xFF03050C)
        )
    )
}
