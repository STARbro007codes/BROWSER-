/**
 * SiteLock Browser Configuration
 * Authoritative single-source configuration file for locked operation.
 */

export interface AppConfig {
  APP_NAME: string;
  APP_VERSION: string;
  ALLOWED_SITE_URL: string;
  ALLOWED_DOMAINS: string[];
  ALLOWED_ORIGINS: string[];
  ALLOWED_PROTOCOLS: string[];
  ALLOW_SUBDOMAINS: boolean;
  ALLOW_HTTP: boolean;
  ENABLE_TABS: boolean;
  MAX_TABS: number;
  ENABLE_DOWNLOADS: boolean;
  ENABLE_FULLSCREEN: boolean;
  ENABLE_KIOSK_MODE: boolean;
  DEV_MODE: boolean;
  DEFAULT_THEME: 'dark' | 'light' | 'system';
}

export const CONFIG: AppConfig = {
  APP_NAME: 'SiteLock Browser',
  APP_VERSION: '1.0.0',

  /**
   * Primary target website URL.
   * Modify this to lock the application to your specific enterprise or service portal.
   */
  ALLOWED_SITE_URL: 'https://example.com',

  /**
   * Explicitly permitted domains.
   */
  ALLOWED_DOMAINS: [
    'example.com',
    'www.example.com'
  ],

  /**
   * Explicitly permitted origin prefixes.
   */
  ALLOWED_ORIGINS: [
    'https://example.com',
    'https://www.example.com'
  ],

  /**
   * Allowed protocols. Only HTTPS is allowed by default.
   */
  ALLOWED_PROTOCOLS: ['https:'],

  /**
   * Whether to allow subdomains of configured domains (*.example.com).
   * Default: false for strict isolation.
   */
  ALLOW_SUBDOMAINS: false,

  /**
   * Allow insecure unencrypted HTTP protocol.
   * Default: false.
   */
  ALLOW_HTTP: false,

  /**
   * Browser Tab capabilities
   */
  ENABLE_TABS: true,
  MAX_TABS: 6,

  /**
   * Safe downloads from permitted domain
   */
  ENABLE_DOWNLOADS: true,

  /**
   * Window features
   */
  ENABLE_FULLSCREEN: true,
  ENABLE_KIOSK_MODE: true,

  /**
   * Developer Mode toggle (disables DevTools shortcuts in production)
   */
  DEV_MODE: false,

  DEFAULT_THEME: 'dark'
};
