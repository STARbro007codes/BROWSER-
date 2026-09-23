package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserTab
import com.example.ui.theme.CrimsonBlocked
import com.example.ui.theme.EmeraldSecure

@Composable
fun BrowserToolbar(
    activeTab: BrowserTab?,
    tabCount: Int,
    blockedCount: Int,
    downloadCount: Int,
    isKioskMode: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onAddressClick: () -> Unit,
    onSslClick: () -> Unit,
    onToggleKiosk: () -> Unit,
    onTabsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onAuditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Navigation: Back
            IconButton(
                onClick = onBack,
                enabled = activeTab?.canGoBack == true,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (activeTab?.canGoBack == true)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Navigation: Forward
            IconButton(
                onClick = onForward,
                enabled = activeTab?.canGoForward == true,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_forward")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (activeTab?.canGoForward == true)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Navigation: Reload or Stop
            IconButton(
                onClick = {
                    if (activeTab?.isLoading == true) onStop() else onReload()
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_reload")
            ) {
                Icon(
                    imageVector = if (activeTab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (activeTab?.isLoading == true) "Stop Loading" else "Reload",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Address Bar (Locked Single-Site)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onAddressClick() }
                    .padding(horizontal = 12.dp)
                    .testTag("toolbar_address_bar"),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // SSL Security Icon Pill
                    Surface(
                        shape = CircleShape,
                        color = EmeraldSecure.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSslClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure HTTPS Lock",
                                tint = EmeraldSecure,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // URL / Host Display
                    val displayUrl = activeTab?.url ?: "Locked Portal"
                    Text(
                        text = displayUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Locked Site Chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "LOCKED",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Kiosk / Fullscreen toggle
            IconButton(
                onClick = onToggleKiosk,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_kiosk")
            ) {
                Icon(
                    imageVector = if (isKioskMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isKioskMode) "Exit Kiosk" else "Enter Kiosk",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Tabs toggle with badge
            IconButton(
                onClick = onTabsClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_tabs")
            ) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(text = tabCount.toString(), fontSize = 10.sp)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Tab,
                        contentDescription = "Tabs",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Security Audit Shield with blocked count badge
            IconButton(
                onClick = onAuditClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_audit")
            ) {
                BadgedBox(
                    badge = {
                        if (blockedCount > 0) {
                            Badge(
                                containerColor = CrimsonBlocked,
                                contentColor = Color.White
                            ) {
                                Text(text = blockedCount.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Audit",
                        tint = if (blockedCount > 0) CrimsonBlocked else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Downloads Icon
            if (downloadCount > 0) {
                IconButton(
                    onClick = onDownloadsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("toolbar_btn_downloads")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Text(text = downloadCount.toString(), fontSize = 10.sp)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Downloads",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("toolbar_btn_settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Progress indicator
        AnimatedVisibility(visible = activeTab?.isLoading == true) {
            val currentProgress = (activeTab?.progress ?: 0) / 100f
            if (currentProgress > 0f) {
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
