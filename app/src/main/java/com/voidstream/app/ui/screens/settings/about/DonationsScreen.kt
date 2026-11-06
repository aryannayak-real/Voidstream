package com.voidstream.app.ui.screens.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voidstream.app.ui.components.settings.SettingsPage

private val HeaderPadding = 24.dp
private val HeaderSpacing = 12.dp
private val HeroIconSize = 48.dp

@Composable
fun DonationsScreen(onNavigateBack: () -> Unit) {
    SettingsPage(
        title = "Support VoidStream",
        onBack = onNavigateBack,
    ) {
        item("donations.header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HeaderPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
            ) {
                Icon(
                    Icons.Outlined.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(HeroIconSize),
                )
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "VoidStream's donation options are being prepared. Please check back later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
