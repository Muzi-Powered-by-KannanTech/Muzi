package com.kannantech.muzi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.navigation.NavController
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.TopBarInsets
import com.kannantech.muzi.ui.component.ColumnWithContentPadding
import com.kannantech.muzi.ui.component.PreferenceEntry
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val haptic = LocalHapticFeedback.current
 
    ColumnWithContentPadding(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background), // Absolute Black
        columnModifier = Modifier
            .padding(horizontal = 20.dp) // Premium breathing room
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))
 
        // Editorial Magazine Header
        Text(
            text = stringResource(R.string.more_title).uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = TextUnit(0.06f, TextUnitType.Em),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 24.dp)
        )
 
        // ─── Main Actions Section (Onyx Glass) ───────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.history), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { 
                        navController.navigate("history")
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
                PreferenceEntry(
                    title = { Text(stringResource(R.string.stats), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { 
                        navController.navigate("stats")
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
                PreferenceEntry(
                    title = { Text(stringResource(R.string.scanner_local_title), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.SdCard, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { 
                        navController.navigate("settings/local")
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
            }
        }
 
        Spacer(modifier = Modifier.height(20.dp))
 
        // ─── System Section (Onyx Glass) ─────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.account), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.AccountCircle, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { 
                        navController.navigate("account")
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
                PreferenceEntry(
                    title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Rounded.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { 
                        navController.navigate("settings")
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp)) // Reserve space for Nav Bar
    }
 
    TopAppBar(
        title = { /* Invisible but handles scroll logic */ },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
        ),
        windowInsets = TopBarInsets,
        scrollBehavior = scrollBehavior
    )
}
