package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.config.SiteLockConfig
import com.example.ui.components.AboutDialog
import com.example.ui.components.BrowserErrorView
import com.example.ui.components.BrowserTabBar
import com.example.ui.components.BrowserToolbar
import com.example.ui.components.DownloadsDialog
import com.example.ui.components.KioskOverlay
import com.example.ui.components.SecureWebView
import com.example.ui.components.SecurityAuditDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SslCertificateDialog
import com.example.ui.components.UrlEditorDialog
import com.example.ui.theme.CrimsonBlocked
import com.example.ui.theme.SiteLockTheme
import com.example.viewmodel.BrowserViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiteLockTheme {
                val viewModel: BrowserViewModel = viewModel()
                BrowserAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BrowserAppScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navAction by viewModel.navAction.collectAsStateWithLifecycle()

    val activeTab = uiState.tabs.find { it.id == uiState.activeTabId } ?: uiState.tabs.firstOrNull()

    // Handle physical back button / navigation gesture
    BackHandler(enabled = activeTab?.canGoBack == true) {
        viewModel.triggerGoBack()
    }

    // Auto-dismiss blocked notification after 4.5 seconds
    LaunchedEffect(uiState.blockedAlert) {
        if (uiState.blockedAlert != null) {
            delay(4500)
            viewModel.dismissBlockedAlert()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Browser Chrome (Tabs + Toolbar) - hidden in Kiosk Mode
                if (!uiState.isKioskMode) {
                    if (SiteLockConfig.ENABLE_TABS && uiState.tabs.size > 0) {
                        BrowserTabBar(
                            tabs = uiState.tabs,
                            activeTabId = uiState.activeTabId,
                            onSelectTab = { viewModel.selectTab(it) },
                            onCloseTab = { viewModel.closeTab(it) },
                            onNewTab = { viewModel.openNewTab() }
                        )
                    }

                    BrowserToolbar(
                        activeTab = activeTab,
                        tabCount = uiState.tabs.size,
                        blockedCount = uiState.auditLogs.size,
                        downloadCount = uiState.downloads.size,
                        isKioskMode = uiState.isKioskMode,
                        onBack = { viewModel.triggerGoBack() },
                        onForward = { viewModel.triggerGoForward() },
                        onReload = { viewModel.triggerReload() },
                        onStop = { viewModel.triggerStopLoading() },
                        onAddressClick = { viewModel.setUrlEditorOpen(true) },
                        onSslClick = { viewModel.setSslInfoOpen(true, uiState.activeSslInfo) },
                        onToggleKiosk = { viewModel.toggleKioskMode() },
                        onTabsClick = { viewModel.openNewTab() },
                        onDownloadsClick = { viewModel.setDownloadsOpen(true) },
                        onAuditClick = { viewModel.setAuditLogOpen(true) },
                        onSettingsClick = { viewModel.setSettingsOpen(true) }
                    )
                }

                // Web Content Area / Error View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (activeTab != null) {
                        if (activeTab.errorState != null) {
                            BrowserErrorView(
                                errorState = activeTab.errorState,
                                onRetry = { viewModel.triggerReload() },
                                onGoHome = {
                                    viewModel.navigateTab(activeTab.id, SiteLockConfig.DEFAULT_ALLOWED_URL)
                                },
                                onViewSecurityLog = {
                                    viewModel.setAuditLogOpen(true)
                                }
                            )
                        } else {
                            SecureWebView(
                                tab = activeTab,
                                viewModel = viewModel,
                                navigationGuard = viewModel.navigationGuard,
                                navAction = navAction,
                                onConsumeNavAction = { viewModel.consumeNavAction() }
                            )
                        }
                    }
                }
            }

            // Floating Kiosk Overlay
            KioskOverlay(
                isKioskMode = uiState.isKioskMode,
                onExitKiosk = { viewModel.toggleKioskMode() },
                onReload = { viewModel.triggerReload() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Blocked Notification Banner
            AnimatedVisibility(
                visible = uiState.blockedAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CrimsonBlocked
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blocked_notification_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.blockedAlert ?: "",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { viewModel.dismissBlockedAlert() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                }
            }

            // Dialogs
            if (uiState.isSettingsOpen) {
                SettingsDialog(
                    allowedDomains = uiState.allowedDomains,
                    allowSubdomains = uiState.allowSubdomains,
                    allowHttp = uiState.allowHttp,
                    devModeEnabled = uiState.devModeEnabled,
                    startupTimestamp = uiState.startupTimestamp,
                    pageLoadDurationMs = uiState.pageLoadDurationMs,
                    onUpdatePolicy = { domains, sub, http ->
                        viewModel.updateAllowlistPolicy(domains, sub, http)
                    },
                    onToggleDevMode = { viewModel.setDevMode(it) },
                    onClearData = { cookies, cache, storage ->
                        // Clear browsing data
                    },
                    onDismiss = { viewModel.setSettingsOpen(false) }
                )
            }

            if (uiState.isAuditLogOpen) {
                SecurityAuditDialog(
                    auditLogs = uiState.auditLogs,
                    onDismiss = { viewModel.setAuditLogOpen(false) }
                )
            }

            if (uiState.isDownloadsOpen) {
                DownloadsDialog(
                    downloads = uiState.downloads,
                    onDismiss = { viewModel.setDownloadsOpen(false) }
                )
            }

            if (uiState.isSslInfoOpen) {
                SslCertificateDialog(
                    sslInfo = uiState.activeSslInfo,
                    onDismiss = { viewModel.setSslInfoOpen(false) }
                )
            }

            if (uiState.isUrlEditorOpen && activeTab != null) {
                UrlEditorDialog(
                    currentUrl = activeTab.url,
                    navigationGuard = viewModel.navigationGuard,
                    onNavigate = { target: String ->
                        viewModel.navigateTab(activeTab.id, target)
                    },
                    onDismiss = { viewModel.setUrlEditorOpen(false) }
                )
            }

            if (uiState.isAboutOpen) {
                AboutDialog(
                    onDismiss = { viewModel.setAboutOpen(false) }
                )
            }
        }
    }
}
