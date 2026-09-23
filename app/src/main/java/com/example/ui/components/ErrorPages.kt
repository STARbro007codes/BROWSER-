package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserErrorState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonBlocked

@Composable
fun BrowserErrorView(
    errorState: BrowserErrorState,
    onRetry: () -> Unit,
    onGoHome: () -> Unit,
    onViewSecurityLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (errorState) {
            is BrowserErrorState.NavigationBlocked -> {
                ErrorCard(
                    icon = Icons.Default.Block,
                    iconTint = CrimsonBlocked,
                    title = "Navigation Blocked",
                    subtitle = "This page is not available in this single-site application.",
                    detail = errorState.reason,
                    targetUrl = errorState.attemptedUrl,
                    primaryActionText = "Return to Allowed Site",
                    onPrimaryAction = onGoHome,
                    secondaryActionText = "Security Log",
                    onSecondaryAction = onViewSecurityLog
                )
            }
            is BrowserErrorState.NetworkError -> {
                ErrorCard(
                    icon = Icons.Default.Warning,
                    iconTint = AmberWarning,
                    title = "Unable to Connect",
                    subtitle = "The requested page could not be loaded (${errorState.errorCode}).",
                    detail = errorState.description,
                    targetUrl = errorState.failingUrl,
                    primaryActionText = "Retry Connection",
                    onPrimaryAction = onRetry,
                    secondaryActionText = "Go to Home",
                    onSecondaryAction = onGoHome
                )
            }
            is BrowserErrorState.OfflineError -> {
                ErrorCard(
                    icon = Icons.Default.SignalWifiConnectedNoInternet4,
                    iconTint = AmberWarning,
                    title = "You're Offline",
                    subtitle = "Please check your network connection and try again.",
                    detail = "No internet connection detected while requesting ${errorState.url}",
                    targetUrl = errorState.url,
                    primaryActionText = "Retry",
                    onPrimaryAction = onRetry,
                    secondaryActionText = "Portal Home",
                    onSecondaryAction = onGoHome
                )
            }
            is BrowserErrorState.SslSecurityError -> {
                ErrorCard(
                    icon = Icons.Default.Security,
                    iconTint = CrimsonBlocked,
                    title = "Security Verification Failed",
                    subtitle = "The TLS/SSL certificate verification for this domain failed.",
                    detail = errorState.sslMessage,
                    targetUrl = errorState.url,
                    primaryActionText = "Return to Safe Portal",
                    onPrimaryAction = onGoHome,
                    secondaryActionText = "Security Details",
                    onSecondaryAction = onViewSecurityLog
                )
            }
            is BrowserErrorState.RendererCrashed -> {
                ErrorCard(
                    icon = Icons.Default.Refresh,
                    iconTint = AmberWarning,
                    title = "Page Stopped Responding",
                    subtitle = "The internal web rendering process encountered an error and was safely halted.",
                    detail = "Renderer process terminated. Memory was reclaimed.",
                    targetUrl = errorState.url,
                    primaryActionText = "Reload Page",
                    onPrimaryAction = onRetry,
                    secondaryActionText = "Return Home",
                    onSecondaryAction = onGoHome
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    detail: String,
    targetUrl: String,
    primaryActionText: String,
    onPrimaryAction: () -> Unit,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "DETAILS:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (targetUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Target: $targetUrl",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (secondaryActionText != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("error_secondary_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = secondaryActionText)
                    }
                }

                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("error_primary_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (primaryActionText.contains("Retry", ignoreCase = true)) Icons.Default.Refresh else Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = primaryActionText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
