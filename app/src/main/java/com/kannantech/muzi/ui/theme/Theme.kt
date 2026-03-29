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
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kannantech.muzi.playback.PlayerConnection

// ╔══════════════════════════════════════════════════════╗
// ║  MUZI — "Obsidian Noir" Dark-Only Color System       ║
// ║  Rich deep blacks, Electric Indigo accent            ║
// ╚══════════════════════════════════════════════════════╝
private val ObsidianNoirColors = darkColorScheme(
    // ─── Primary — Vivid Electric Indigo ─────────────────
    primary = Color(0xFF7C3AED),        // Deep rich Violet-Indigo
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3730A3), // Dense Indigo for containers
    onPrimaryContainer = Color(0xFFEDE9FE), // Lavender text on container
    // ─── Secondary — Emerald accent ──────────────────────
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    // ─── Tertiary — Rose accent ───────────────────────────
    tertiary = Color(0xFFF43F5E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9F1239),
    onTertiaryContainer = Color(0xFFFFE4E6),
    // ─── Backgrounds — True AMOLED Black ─────────────────
    background = Color(0xFF000000),     // Pure AMOLED Black
    onBackground = Color(0xFFF5F5F5),   // Near-white text — max contrast
    // ─── Surfaces — "Obsidian" Dark Family ───────────────
    surface = Color(0xFF0D0D0D),        // Obsidian: 1 step up from black
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF1A1A1A), // Onyx card / pill background
    onSurfaceVariant = Color(0xFFA1A1AA), // Zinc-400: readable secondary text
    // ─── Borders & Misc ──────────────────────────────────
    outline = Color(0xFF3F3F46),        // Zinc-700: subtle dark border
    outlineVariant = Color(0xFF27272A), // Zinc-800: ultra-subtle dividers
    inverseOnSurface = Color(0xFF000000),
    inverseSurface = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFF4338CA),
    // ─── Error State ─────────────────────────────────────
    error = Color(0xFFEF4444),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    surfaceTint = Color(0xFF7C3AED),
)

private val PremiumShapes = Shapes(
    extraSmall = RoundedCornerShape(percent = 50),
    small = RoundedCornerShape(percent = 50), // Perfect circular pills for buttons/chips
    medium = RoundedCornerShape(20.dp),       // Smooth squirkles for cards
    large = RoundedCornerShape(32.dp),        // Massive curves for bottom sheets
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MUZITheme(
    highContrastCompat: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(highContrastCompat) {
        // Always use the deep rich dark Obsidian Noir theme
        val baseScheme = ObsidianNoirColors

        val highContrastAdjusted = if (highContrastCompat) {
            baseScheme.copy(
                secondaryContainer = baseScheme.surface,
                onSecondaryContainer = baseScheme.secondary,
            )
        } else {
            baseScheme
        }

        highContrastAdjusted
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = PremiumShapes,
        content = content
    )
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this
