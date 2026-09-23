# SiteLock Desktop Browser — Production Single-Site Browser Container

SiteLock Browser is a locked, dedicated single-site desktop browser engineered to run on Windows, macOS, and Linux. It operates exclusively within an explicitly authorized domain allowlist, preventing arbitrary web navigation, unauthorized redirects, protocol hijacking, popup leaks, or malicious external downloads.

---

## 1. Project Structure

```text
desktop/
├── package.json               # Node.js project manifest & scripts
├── tsconfig.json              # TypeScript compilation settings
├── electron-builder.yml       # Windows NSIS and Portable .exe packaging rules
├── README.md                  # Complete deployment and security documentation
└── src/
    ├── config.ts              # Authoritative domain & feature allowlist
    ├── main/
    │   ├── main.ts            # Electron main process & IPC lifecycle
    │   └── navigationGuard.ts # Central authoritative navigation security guard
    ├── preload/
    │   └── preload.ts         # Isolated contextBridge exposing safe browser controls
    └── renderer/
        ├── index.html         # Modern browser chrome shell (Tabs, Toolbar, Webview)
        ├── styles.css         # Dark & light cyber-secure desktop theme
        └── renderer.ts        # Client-side UI binding & event handling
```

---

## 2. Technology Chosen and Why

**Choice: Electron with TypeScript & Chromium WebView**

1. **Enterprise Web Parity:** Full fidelity execution of modern web applications, WebSockets, WebRTC, Service Workers, Session Storage, and complex enterprise single-page applications.
2. **Deep IPC & Session Security:** Electron provides `session.defaultSession.webRequest`, `setWindowOpenHandler`, and `setPermissionRequestHandler`, enabling packet-level and navigation-level blocking before network sockets open.
3. **Context Isolation & Sandboxing:** Website JavaScript executes within a sandboxed renderer process with no Node.js access.
4. **Frictionless Windows Distribution:** Builds standalone one-click installers (`.exe`), portable binaries, and enterprise MSI packages without requiring native compiler toolchains on end-user machines.

---

## 3. Installation Commands

```bash
cd desktop
npm install
```

---

## 4. Development Command

Run the application in development mode with hot reload:

```bash
npm run dev
```

---

## 5. Production Build Command

Compile TypeScript source code into the `dist/` directory:

```bash
npm run build
```

---

## 6. Windows Installer Build Command

Generate the production Windows Installer (`.exe` NSIS installer) and portable executable:

```bash
# Builds NSIS x64 Installer into desktop/release/
npm run dist:win

# Builds standalone portable .exe into desktop/release/
npm run dist:portable
```

The resulting binaries will be placed in `desktop/release/SiteLock Browser Setup 1.0.0.exe`.

---

## 7. Configuration Instructions

All site configuration is maintained in a single authoritative file:
`desktop/src/config.ts`

```typescript
export const CONFIG: AppConfig = {
  APP_NAME: 'SiteLock Browser',
  APP_VERSION: '1.0.0',

  // 1. Set the primary website target
  ALLOWED_SITE_URL: 'https://your-enterprise-portal.com',

  // 2. Specify allowed domain names
  ALLOWED_DOMAINS: [
    'your-enterprise-portal.com',
    'auth.your-enterprise-portal.com'
  ],

  // 3. Subdomain policy (set true to permit all *.your-enterprise-portal.com)
  ALLOW_SUBDOMAINS: false,

  // 4. Protocol security (Strict HTTPS by default)
  ALLOW_HTTP: false,

  // 5. Feature controls
  ENABLE_TABS: true,
  MAX_TABS: 6,
  ENABLE_DOWNLOADS: true,
  ENABLE_KIOSK_MODE: true,
  DEV_MODE: false
};
```

---

## 8. Allowed-Domain Configuration Instructions

1. Open `desktop/src/config.ts`.
2. Update `ALLOWED_SITE_URL` to your full target URL (e.g., `https://portal.mycompany.com`).
3. Add any necessary hostnames to `ALLOWED_DOMAINS` (e.g., `['portal.mycompany.com', 'api.mycompany.com']`).
4. Keep `ALLOW_SUBDOMAINS: false` unless wildcard subdomain navigation is explicitly required.
5. Save the file and run `npm run build`.

---

## 9. Security Architecture Summary

- **Central Navigation Guard (`navigationGuard.ts`):** Evaluates every URL string before navigation is initiated. Rejects `file://`, `javascript:`, external protocols, and any unlisted host.
- **WebRequest Filtering:** Intercepts main frame requests at the Chromium network stack.
- **Strict Popup Handling:** `setWindowOpenHandler` evaluates target URLs; allowed URLs open within in-app tabs; unauthorized targets are denied.
- **Context Isolation:** Web pages cannot access Node.js or Electron internals.
- **Session Persistence:** Cookies and LocalStorage persist across launches via partition `persist:sitelock`.

---

## 10. Testing Checklist

- [x] **Target Navigation:** Allowed URL (`https://example.com`) loads cleanly.
- [x] **Blocked External Site:** Attempting to navigate to `https://google.com` or `https://youtube.com` immediately blocks with "Navigation blocked".
- [x] **Protocol Protection:** `file:///C:/` and `javascript:alert(1)` are intercepted and denied.
- [x] **Popup Guard:** External `window.open()` popups are intercepted and blocked.
- [x] **Safe Downloads:** Downloads from permitted host are tracked; downloads from external domains are rejected.
- [x] **Kiosk Mode:** Fullscreen kiosk mode hides toolbar controls while retaining navigation guards.
