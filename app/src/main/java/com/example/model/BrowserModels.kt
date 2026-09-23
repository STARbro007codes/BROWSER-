package com.example.model

import android.graphics.Bitmap
import com.example.security.ViolationType

data class BrowserTab(
    val id: String,
    val title: String = "Locked Portal",
    val url: String,
    val favicon: Bitmap? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isSecureSsl: Boolean = true,
    val errorState: BrowserErrorState? = null
)

sealed class BrowserErrorState {
    data class NavigationBlocked(
        val attemptedUrl: String,
        val reason: String,
        val violationType: ViolationType
    ) : BrowserErrorState()

    data class NetworkError(
        val failingUrl: String,
        val errorCode: Int,
        val description: String
    ) : BrowserErrorState()

    data class OfflineError(
        val url: String
    ) : BrowserErrorState()

    data class SslSecurityError(
        val url: String,
        val sslMessage: String
    ) : BrowserErrorState()

    data class RendererCrashed(
        val url: String
    ) : BrowserErrorState()
}

data class SecurityAuditEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptedUrl: String,
    val reason: String,
    val violationType: ViolationType,
    val actionTaken: String = "Blocked & Prevented"
)

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class DownloadRecord(
    val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val contentLength: Long,
    val status: DownloadStatus = DownloadStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis()
)

data class SslInfo(
    val domain: String,
    val issuedTo: String,
    val issuedBy: String,
    val validUntil: String,
    val isSecure: Boolean
)
