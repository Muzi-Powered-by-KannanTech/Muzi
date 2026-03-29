package com.kannantech.muzi.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NavigationTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        thumbnail?.invoke()

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            label?.let { label ->
                Text(
                    text = label.uppercase(), // All caps for editorial feel
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.1f, androidx.compose.ui.unit.TextUnitType.Em),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    modifier = androidx.compose.ui.Modifier.padding(bottom = 2.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall, // Larger for magazine impact
                fontWeight = FontWeight.SemiBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.04f, androidx.compose.ui.unit.TextUnitType.Em),
                color = MaterialTheme.colorScheme.primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
