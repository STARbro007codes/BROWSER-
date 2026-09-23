package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.SiteLockConfig
import com.example.model.BrowserErrorState
import com.example.model.BrowserTab
import com.example.model.DownloadRecord
import com.example.model.DownloadStatus
import com.example.model.SecurityAuditEntry
import com.example.model.SslInfo
import com.example.security.NavigationDecision
import com.example.security.NavigationGuard
import com.example.security.ViolationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTabId: String = "",
    val isKioskMode: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isDownloadsOpen: Boolean = false,
    val isAuditLogOpen: Boolean = false,
    val isSslInfoOpen: Boolean = false,
    val isAboutOpen: Boolean = false,
    val isUrlEditorOpen: Boolean = false,
    val auditLogs: List<SecurityAuditEntry> = emptyList(),
    val downloads: List<DownloadRecord> = emptyList(),
    val activeSslInfo: SslInfo? = null,
    val blockedAlert: String? = null,
    val devModeEnabled: Boolean = SiteLockConfig.DEV_MODE_DEFAULT,
    val allowedDomains: Set<String> = SiteLockConfig.DEFAULT_ALLOWED_DOMAINS,
    val allowSubdomains: Boolean = SiteLockConfig.DEFAULT_ALLOW_SUBDOMAINS,
    val allowHttp: Boolean = false,
    val startupTimestamp: Long = System.currentTimeMillis(),
    val pageLoadDurationMs: Long = 0
)

class BrowserViewModel : ViewModel() {

    val navigationGuard = NavigationGuard()

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    // Navigation command events for WebView consumption
    private val _navAction = MutableStateFlow<TabNavAction?>(null)
    val navAction: StateFlow<TabNavAction?> = _navAction.asStateFlow()

    private var pageStartMark: Long = 0

    init {
        // Initialize with default locked tab
        val initialTab = BrowserTab(
            id = UUID.randomUUID().toString(),
            title = "Locked Portal",
            url = SiteLockConfig.DEFAULT_ALLOWED_URL
        )
        _uiState.update {
            it.copy(
                tabs = listOf(initialTab),
                activeTabId = initialTab.id
            )
        }
    }

    fun getActiveTab(): BrowserTab? {
        val state = _uiState.value
        return state.tabs.find { it.id == state.activeTabId }
    }

    fun openNewTab(url: String = SiteLockConfig.DEFAULT_ALLOWED_URL) {
        val currentTabs = _uiState.value.tabs
        if (currentTabs.size >= SiteLockConfig.MAX_TABS) {
            showBlockedAlert("Maximum tab limit (${SiteLockConfig.MAX_TABS}) reached.")
            return
        }

        // Validate URL through NavigationGuard
        when (val decision = navigationGuard.evaluate(url)) {
            is NavigationDecision.Allow -> {
                val newTab = BrowserTab(
                    id = UUID.randomUUID().toString(),
                    title = "New Tab",
                    url = url
                )
                _uiState.update {
                    it.copy(
                        tabs = it.tabs + newTab,
                        activeTabId = newTab.id
                    )
                }
            }
            is NavigationDecision.Block -> {
                recordBlockedAttempt(url, decision.reason, decision.violationType)
                showBlockedAlert("Cannot open tab: ${decision.reason}")
            }
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _uiState.value.tabs
        if (currentTabs.size <= 1) {
            // Re-navigate single tab to home rather than closing entirely
            navigateTab(tabId, SiteLockConfig.DEFAULT_ALLOWED_URL)
            return
        }

        val remainingTabs = currentTabs.filter { it.id != tabId }
        val newActiveId = if (_uiState.value.activeTabId == tabId) {
            val closedIndex = currentTabs.indexOfFirst { it.id == tabId }
            val nextIndex = (closedIndex - 1).coerceAtLeast(0)
            remainingTabs[nextIndex].id
        } else {
            _uiState.value.activeTabId
        }

        _uiState.update {
            it.copy(
                tabs = remainingTabs,
                activeTabId = newActiveId
            )
        }
    }

    fun selectTab(tabId: String) {
        _uiState.update { it.copy(activeTabId = tabId) }
    }

    fun navigateTab(tabId: String, targetUrl: String) {
        when (val decision = navigationGuard.evaluate(targetUrl)) {
            is NavigationDecision.Allow -> {
                updateTabState(tabId) {
                    it.copy(
                        url = targetUrl,
                        errorState = null,
                        isLoading = true
                    )
                }
                _navAction.value = TabNavAction.LoadUrl(tabId, targetUrl)
            }
            is NavigationDecision.Block -> {
                recordBlockedAttempt(targetUrl, decision.reason, decision.violationType)
                updateTabState(tabId) {
                    it.copy(
                        errorState = BrowserErrorState.NavigationBlocked(
                            attemptedUrl = targetUrl,
                            reason = decision.reason,
                            violationType = decision.violationType
                        )
                    )
                }
                showBlockedAlert("Navigation blocked: ${decision.reason}")
            }
        }
    }

    fun triggerGoBack() {
        val active = getActiveTab() ?: return
        if (active.canGoBack) {
            _navAction.value = TabNavAction.GoBack(active.id)
        }
    }

    fun triggerGoForward() {
        val active = getActiveTab() ?: return
        if (active.canGoForward) {
            _navAction.value = TabNavAction.GoForward(active.id)
        }
    }

    fun triggerReload() {
        val active = getActiveTab() ?: return
        updateTabState(active.id) { it.copy(errorState = null) }
        _navAction.value = TabNavAction.Reload(active.id)
    }

    fun triggerStopLoading() {
        val active = getActiveTab() ?: return
        _navAction.value = TabNavAction.Stop(active.id)
    }

    fun consumeNavAction() {
        _navAction.value = null
    }

    fun onPageStarted(tabId: String, url: String) {
        pageStartMark = System.currentTimeMillis()
        updateTabState(tabId) {
            it.copy(
                url = url,
                isLoading = true,
                errorState = null
            )
        }
    }

    fun onPageFinished(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean) {
        val duration = if (pageStartMark > 0) System.currentTimeMillis() - pageStartMark else 0
        updateTabState(tabId) {
            it.copy(
                url = url,
                isLoading = false,
                progress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isSecureSsl = url.startsWith("https://")
            )
        }
        _uiState.update { it.copy(pageLoadDurationMs = duration) }
    }

    fun onProgressChanged(tabId: String, newProgress: Int) {
        updateTabState(tabId) {
            it.copy(
                progress = newProgress,
                isLoading = newProgress < 100
            )
        }
    }

    fun onReceivedTitle(tabId: String, title: String) {
        updateTabState(tabId) {
            it.copy(title = if (title.isBlank()) "Locked Site" else title)
        }
    }

    fun onReceivedFavicon(tabId: String, favicon: Bitmap) {
        updateTabState(tabId) { it.copy(favicon = favicon) }
    }

    fun onReceivedError(tabId: String, failingUrl: String, errorCode: Int, description: String) {
        updateTabState(tabId) {
            it.copy(
                isLoading = false,
                errorState = BrowserErrorState.NetworkError(
                    failingUrl = failingUrl,
                    errorCode = errorCode,
                    description = description
                )
            )
        }
    }

    fun onReceivedSslError(tabId: String, url: String, primaryError: String) {
        recordBlockedAttempt(url, "SSL Certificate Error: $primaryError", ViolationType.INSECURE_PROTOCOL)
        updateTabState(tabId) {
            it.copy(
                isLoading = false,
                errorState = BrowserErrorState.SslSecurityError(
                    url = url,
                    sslMessage = primaryError
                )
            )
        }
    }

    fun onRenderProcessGone(tabId: String) {
        updateTabState(tabId) {
            it.copy(
                isLoading = false,
                errorState = BrowserErrorState.RendererCrashed(url = it.url)
            )
        }
    }

    fun recordBlockedAttempt(url: String, reason: String, violationType: ViolationType) {
        val entry = SecurityAuditEntry(
            id = UUID.randomUUID().toString(),
            attemptedUrl = url,
            reason = reason,
            violationType = violationType
        )
        _uiState.update {
            it.copy(auditLogs = listOf(entry) + it.auditLogs.take(99))
        }
    }

    fun addDownloadRecord(url: String, mimeType: String, contentLength: Long, fileName: String) {
        val record = DownloadRecord(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            url = url,
            mimeType = mimeType,
            contentLength = contentLength,
            status = DownloadStatus.COMPLETED
        )
        _uiState.update {
            it.copy(downloads = listOf(record) + it.downloads)
        }
        showBlockedAlert("Download saved: $fileName")
    }

    fun toggleKioskMode() {
        _uiState.update { it.copy(isKioskMode = !it.isKioskMode) }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun setDownloadsOpen(open: Boolean) {
        _uiState.update { it.copy(isDownloadsOpen = open) }
    }

    fun setAuditLogOpen(open: Boolean) {
        _uiState.update { it.copy(isAuditLogOpen = open) }
    }

    fun setSslInfoOpen(open: Boolean, info: SslInfo? = null) {
        _uiState.update { it.copy(isSslInfoOpen = open, activeSslInfo = info) }
    }

    fun setAboutOpen(open: Boolean) {
        _uiState.update { it.copy(isAboutOpen = open) }
    }

    fun setUrlEditorOpen(open: Boolean) {
        _uiState.update { it.copy(isUrlEditorOpen = open) }
    }

    fun setDevMode(enabled: Boolean) {
        _uiState.update { it.copy(devModeEnabled = enabled) }
    }

    fun showBlockedAlert(msg: String) {
        _uiState.update { it.copy(blockedAlert = msg) }
    }

    fun dismissBlockedAlert() {
        _uiState.update { it.copy(blockedAlert = null) }
    }

    fun updateAllowlistPolicy(domains: Set<String>, allowSubdomains: Boolean, allowHttp: Boolean) {
        navigationGuard.updatePolicy(domains, allowSubdomains, allowHttp)
        _uiState.update {
            it.copy(
                allowedDomains = domains,
                allowSubdomains = allowSubdomains,
                allowHttp = allowHttp
            )
        }
    }

    fun clearBrowsingData(context: Context, clearCookies: Boolean, clearCache: Boolean, clearStorage: Boolean) {
        viewModelScope.launch {
            if (clearCookies) {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            if (clearStorage) {
                WebStorage.getInstance().deleteAllData()
            }
            if (clearCache) {
                // reload active tab without cache
                triggerReload()
            }
            showBlockedAlert("Selected browsing data cleared.")
        }
    }

    private fun updateTabState(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == tabId) transform(tab) else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }
}

sealed class TabNavAction {
    data class LoadUrl(val tabId: String, val url: String) : TabNavAction()
    data class GoBack(val tabId: String) : TabNavAction()
    data class GoForward(val tabId: String) : TabNavAction()
    data class Reload(val tabId: String) : TabNavAction()
    data class Stop(val tabId: String) : TabNavAction()
}
