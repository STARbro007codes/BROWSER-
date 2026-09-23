package com.example.security

import android.net.Uri
import com.example.config.SiteLockConfig

enum class ViolationType {
    UNAUTHORIZED_DOMAIN,
    INSECURE_PROTOCOL,
    DANGEROUS_SCHEME,
    MALFORMED_URL,
    FILE_ACCESS_ATTEMPT,
    JAVASCRIPT_EXECUTION,
    EXTERNAL_APP_LAUNCH
}

sealed class NavigationDecision {
    data object Allow : NavigationDecision()
    data class Block(
        val reason: String,
        val attemptedUrl: String,
        val violationType: ViolationType
    ) : NavigationDecision()
}

/**
 * NavigationGuard
 * Central authoritative security layer enforcing that all navigations,
 * popups, iframe links, redirects, and resource targets adhere strictly
 * to the allowed single-site boundary.
 */
class NavigationGuard(
    private var allowedDomains: Set<String> = SiteLockConfig.DEFAULT_ALLOWED_DOMAINS,
    private var allowedProtocols: Set<String> = SiteLockConfig.ALLOWED_PROTOCOLS,
    private var allowSubdomains: Boolean = SiteLockConfig.DEFAULT_ALLOW_SUBDOMAINS,
    private var allowHttp: Boolean = false
) {

    fun updatePolicy(
        domains: Set<String>,
        subdomainsAllowed: Boolean,
        httpAllowed: Boolean
    ) {
        allowedDomains = domains.map { it.lowercase().trim() }.toSet()
        allowSubdomains = subdomainsAllowed
        allowHttp = httpAllowed
    }

    fun getAllowedDomains(): Set<String> = allowedDomains
    fun isSubdomainsAllowed(): Boolean = allowSubdomains
    fun isHttpAllowed(): Boolean = allowHttp

    /**
     * Strictly evaluates an attempted URL navigation.
     */
    fun evaluate(url: String?): NavigationDecision {
        if (url.isNullOrBlank()) {
            return NavigationDecision.Block(
                reason = "Navigation target is empty or null.",
                attemptedUrl = "",
                violationType = ViolationType.MALFORMED_URL
            )
        }

        val trimmedUrl = url.trim()

        // 1. Strict Scheme & Dangerous Protocol Check
        val lowerUrl = trimmedUrl.lowercase()
        if (lowerUrl.startsWith("file://") || lowerUrl.startsWith("file:")) {
            return NavigationDecision.Block(
                reason = "Local filesystem access (file://) is strictly prohibited.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.FILE_ACCESS_ATTEMPT
            )
        }

        if (lowerUrl.startsWith("javascript:")) {
            return NavigationDecision.Block(
                reason = "Inline JavaScript protocol navigation (javascript:) is blocked.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.JAVASCRIPT_EXECUTION
            )
        }

        if (lowerUrl.startsWith("intent:") || lowerUrl.startsWith("market:") || lowerUrl.startsWith("app:")) {
            return NavigationDecision.Block(
                reason = "External application launching is strictly prohibited.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.EXTERNAL_APP_LAUNCH
            )
        }

        // Allow harmless internal data/blank for WebView internals if not navigating away
        if (trimmedUrl == "about:blank") {
            return NavigationDecision.Allow
        }

        val uri: Uri = try {
            Uri.parse(trimmedUrl)
        } catch (e: Exception) {
            return NavigationDecision.Block(
                reason = "Malformed URL structure: ${e.message}",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.MALFORMED_URL
            )
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme == null) {
            return NavigationDecision.Block(
                reason = "URL lacks a valid protocol scheme.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.INSECURE_PROTOCOL
            )
        }

        // Check protocol permissions
        if (scheme == "http") {
            if (!allowHttp) {
                return NavigationDecision.Block(
                    reason = "Insecure HTTP protocol is disabled. Only HTTPS is allowed.",
                    attemptedUrl = trimmedUrl,
                    violationType = ViolationType.INSECURE_PROTOCOL
                )
            }
        } else if (scheme != "https") {
            return NavigationDecision.Block(
                reason = "Protocol '$scheme' is not permitted. Only secure HTTPS is authorized.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.DANGEROUS_SCHEME
            )
        }

        // 2. Host and Domain Verification
        val host = uri.host?.lowercase()
        if (host.isNullOrBlank()) {
            return NavigationDecision.Block(
                reason = "URL does not specify a valid host.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.MALFORMED_URL
            )
        }

        val isAuthorized = isHostAuthorized(host)
        if (!isAuthorized) {
            return NavigationDecision.Block(
                reason = "External domain '$host' is not in the single-site allowlist.",
                attemptedUrl = trimmedUrl,
                violationType = ViolationType.UNAUTHORIZED_DOMAIN
            )
        }

        return NavigationDecision.Allow
    }

    private fun isHostAuthorized(host: String): Boolean {
        for (allowed in allowedDomains) {
            val normalizedAllowed = allowed.lowercase().trim()
            if (host == normalizedAllowed) {
                return true
            }
            if (allowSubdomains) {
                if (host.endsWith(".$normalizedAllowed")) {
                    return true
                }
            }
        }
        return false
    }
}
