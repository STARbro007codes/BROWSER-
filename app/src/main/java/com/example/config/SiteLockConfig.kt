package com.example.config

/**
 * SiteLock Browser Configuration
 * Central authoritative configuration for single-site locked operation.
 */
object SiteLockConfig {
    const val APP_NAME = "SiteLock Browser"
    const val APP_VERSION = "1.0.0"

    /**
     * The primary allowed target website URL.
     * Change this to your dedicated enterprise, portal, or web application URL.
     */
    const val DEFAULT_ALLOWED_URL = "https://example.com"

    /**
     * Primary domain allowed for navigation.
     */
    const val DEFAULT_ALLOWED_DOMAIN = "example.com"

    /**
     * Explicit list of allowed domains.
     * By default, only the exact target domain is allowed.
     */
    val DEFAULT_ALLOWED_DOMAINS = setOf(
        "example.com",
        "www.example.com"
    )

    /**
     * Explicit list of allowed origins.
     */
    val DEFAULT_ALLOWED_ORIGINS = setOf(
        "https://example.com",
        "https://www.example.com"
    )

    /**
     * Whether to permit subdomains of allowed domains (*.example.com).
     * Strictly false by default as required by the security specification.
     */
    const val DEFAULT_ALLOW_SUBDOMAINS = false

    /**
     * Allowed network protocols.
     * HTTP is strictly blocked by default; only HTTPS is permitted.
     */
    val ALLOWED_PROTOCOLS = setOf("https")

    /**
     * Enable in-app tabs
     */
    const val ENABLE_TABS = true
    const val MAX_TABS = 6

    /**
     * Enable in-app downloads from the allowed site
     */
    const val ENABLE_DOWNLOADS = true

    /**
     * Enable kiosk / fullscreen mode
     */
    const val ENABLE_KIOSK_MODE = true

    /**
     * Developer diagnostics mode
     */
    const val DEV_MODE_DEFAULT = false

    /**
     * User Agent suffix for desktop emulation or identification
     */
    const val USER_AGENT_SUFFIX = "SiteLockSecureDesktop/1.0"
}
