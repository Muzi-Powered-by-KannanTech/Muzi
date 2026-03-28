/*
 * Copyright (C) 2024 z-huang/InnerTune
 * Copyright (C) 2025 O&zwnj;u&zwnj;t&zwnj;e&zwnj;r&zwnj;T&zwnj;u&zwnj;n&zwnj;e Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */
package com.kannantech.muzi.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.kannantech.muzi.playback.PlayerConnection

private val LightPremiumColors = lightColorScheme(
    primary = Color(0xFF4F46E5),        // Deep rich Indigo
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF), // Soft Indigo card
    onPrimaryContainer = Color(0xFF312E81), // Dark Indigo text
    secondary = Color(0xFF059669),      // Emerald Emerald secondary
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFE11D48),       // Rose Red
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF881337),
    background = Color(0xFFF9FAFB),     // Off-White background
    onBackground = Color(0xFF111827),   // Deep Slate Black text
    surface = Color(0xFFFFFFFF),        // White card background
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6), // Frost Gray elevated background
    onSurfaceVariant = Color(0xFF4B5563), // Medium gray for secondary text
    outline = Color(0xFFD1D5DB),        // Light gray border
    inverseOnSurface = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF1F2937),
    inversePrimary = Color(0xFFA5B4FC),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    surfaceTint = Color(0xFF4F46E5),
)

private val DarkPremiumColors = darkColorScheme(
    primary = Color(0xFF6366F1),        // Electric Indigo 
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3730A3), // Deep Indigo
    onPrimaryContainer = Color(0xFFE0E7FF), // Light Indigo Text
    secondary = Color(0xFF10B981),      // Emerald Green
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFF43F5E),       // Rose Red
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9F1239),
    onTertiaryContainer = Color(0xFFFFE4E6),
    background = Color(0xFF000000),     // Deep AMOLED Black base
    onBackground = Color(0xFFF9FAFB),   // Crisp off-white text
    surface = Color(0xFF0A0A0A),        // Obsidian slightly raised from black
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF171717), // Charcoal elevated card background
    onSurfaceVariant = Color(0xFFD1D5DB), // Soft gray for secondary text/icons
    outline = Color(0xFF3F3F46),        // Subtle dark gray border
    inverseOnSurface = Color(0xFF000000),
    inverseSurface = Color(0xFFF3F4F6),
    inversePrimary = Color(0xFF4338CA),
    error = Color(0xFFEF4444),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    surfaceTint = Color(0xFF6366F1),
)

@Composable
fun MUZITheme(
    context: Context,
    playerConnection: PlayerConnection?, // Kept for interface compatibility
    enableDynamicTheme: Boolean,         // Kept for interface compatibility
    isSystemInDarkTheme: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    highContrastCompat: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme, pureBlack, highContrastCompat) {
        val baseScheme = if (darkTheme) DarkPremiumColors else LightPremiumColors

        val highContrastAdjusted = if (highContrastCompat) {
            baseScheme.copy(
                secondaryContainer = baseScheme.surface,
                onSecondaryContainer = baseScheme.secondary,
            )
        } else {
            baseScheme
        }

        if (darkTheme && pureBlack) {
            highContrastAdjusted.copy(
                surface = Color.Black,
                background = Color.Black
            )
        } else {
            highContrastAdjusted
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this
