import { CONFIG } from '../config';

export type NavigationDecision =
  | { allowed: true }
  | { allowed: false; reason: string; attemptedUrl: string; violationType: string };

export class NavigationGuard {
  private allowedDomains: string[];
  private allowedProtocols: string[];
  private allowSubdomains: boolean;
  private allowHttp: boolean;

  constructor() {
    this.allowedDomains = CONFIG.ALLOWED_DOMAINS.map((d) => d.toLowerCase().trim());
    this.allowedProtocols = [...CONFIG.ALLOWED_PROTOCOLS];
    this.allowSubdomains = CONFIG.ALLOW_SUBDOMAINS;
    this.allowHttp = CONFIG.ALLOW_HTTP;
  }

  public evaluate(rawUrl: string | undefined | null): NavigationDecision {
    if (!rawUrl || typeof rawUrl !== 'string') {
      return {
        allowed: false,
        reason: 'Empty or invalid URL supplied.',
        attemptedUrl: rawUrl || '',
        violationType: 'MALFORMED_URL'
      };
    }

    const trimmed = rawUrl.trim();
    const lower = trimmed.toLowerCase();

    // 1. Dangerous scheme checks
    if (lower.startsWith('file:') || lower.startsWith('file://')) {
      return {
        allowed: false,
        reason: 'Local filesystem access (file://) is strictly prohibited.',
        attemptedUrl: trimmed,
        violationType: 'FILE_ACCESS'
      };
    }

    if (lower.startsWith('javascript:')) {
      return {
        allowed: false,
        reason: 'Inline JavaScript pseudo-protocol execution is prohibited.',
        attemptedUrl: trimmed,
        violationType: 'JAVASCRIPT_INLINE'
      };
    }

    if (lower.startsWith('data:') && !lower.startsWith('data:image/')) {
      return {
        allowed: false,
        reason: 'Arbitrary data URLs are blocked.',
        attemptedUrl: trimmed,
        violationType: 'DATA_URL'
      };
    }

    if (
      lower.startsWith('intent:') ||
      lower.startsWith('shell:') ||
      lower.startsWith('powershell:') ||
      lower.startsWith('cmd:') ||
      lower.startsWith('ms-settings:')
    ) {
      return {
        allowed: false,
        reason: 'External system protocol invocation is blocked.',
        attemptedUrl: trimmed,
        violationType: 'SYSTEM_PROTOCOL'
      };
    }

    if (trimmed === 'about:blank') {
      return { allowed: true };
    }

    let parsed: URL;
    try {
      parsed = new URL(trimmed);
    } catch {
      return {
        allowed: false,
        reason: 'Malformed URL structure.',
        attemptedUrl: trimmed,
        violationType: 'MALFORMED_URL'
      };
    }

    // 2. Protocol check
    if (parsed.protocol === 'http:') {
      if (!this.allowHttp) {
        return {
          allowed: false,
          reason: 'Insecure HTTP protocol is disabled. Only HTTPS is allowed.',
          attemptedUrl: trimmed,
          violationType: 'INSECURE_PROTOCOL'
        };
      }
    } else if (parsed.protocol !== 'https:') {
      return {
        allowed: false,
        reason: `Protocol '${parsed.protocol}' is not permitted.`,
        attemptedUrl: trimmed,
        violationType: 'UNAUTHORIZED_PROTOCOL'
      };
    }

    // 3. Host authorization
    const hostname = parsed.hostname.toLowerCase();
    const isDomainAllowed = this.checkHost(hostname);

    if (!isDomainAllowed) {
      return {
        allowed: false,
        reason: `Destination domain '${hostname}' is outside the authorized single-site policy.`,
        attemptedUrl: trimmed,
        violationType: 'UNAUTHORIZED_DOMAIN'
      };
    }

    return { allowed: true };
  }

  private checkHost(host: string): boolean {
    for (const domain of this.allowedDomains) {
      if (host === domain) {
        return true;
      }
      if (this.allowSubdomains && host.endsWith('.' + domain)) {
        return true;
      }
    }
    return false;
  }
}

export const navigationGuard = new NavigationGuard();
