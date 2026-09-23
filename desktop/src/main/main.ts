import { app, BrowserWindow, ipcMain, session, Menu, dialog } from 'electron';
import * as path from 'path';
import { CONFIG } from '../config';
import { navigationGuard } from './navigationGuard';

let mainWindow: BrowserWindow | null = null;
let isKiosk = false;

interface TabInfo {
  id: string;
  title: string;
  url: string;
  canGoBack: boolean;
  canGoForward: boolean;
  isLoading: boolean;
}

let tabs: TabInfo[] = [
  {
    id: 'tab-1',
    title: 'Locked Portal',
    url: CONFIG.ALLOWED_SITE_URL,
    canGoBack: false,
    canGoForward: false,
    isLoading: false
  }
];
let activeTabId = 'tab-1';

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    title: CONFIG.APP_NAME,
    frame: true,
    backgroundColor: '#0F172A',
    webPreferences: {
      preload: path.join(__dirname, '../preload/preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      webviewTag: true,
      spellcheck: false
    }
  });

  // Remove default menu to prevent DevTools / navigation bypass
  if (!CONFIG.DEV_MODE) {
    Menu.setApplicationMenu(null);
  }

  // Enforce session security policies
  const defaultSession = session.defaultSession;

  // Intercept all main and sub-frame navigations
  defaultSession.webRequest.onBeforeRequest({ urls: ['*://*/*'] }, (details, callback) => {
    // Check main frame or window navigations
    if (details.resourceType === 'main_frame') {
      const decision = navigationGuard.evaluate(details.url);
      if (!decision.allowed) {
        notifyBlocked(details.url, decision.reason);
        callback({ cancel: true });
        return;
      }
    }
    callback({ cancel: false });
  });

  // Set permission request handler
  defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
    const origin = webContents.getURL();
    const decision = navigationGuard.evaluate(origin);
    if (!decision.allowed) {
      callback(false);
      return;
    }
    // Only allow specific web permissions for the authorized site
    const allowedPermissions = ['media', 'geolocation', 'notifications'];
    callback(allowedPermissions.includes(permission));
  });

  // Handle file downloads safely
  defaultSession.on('will-download', (event, item) => {
    const downloadUrl = item.getURL();
    const decision = navigationGuard.evaluate(downloadUrl);
    if (!decision.allowed || !CONFIG.ENABLE_DOWNLOADS) {
      event.preventDefault();
      notifyBlocked(downloadUrl, 'Download from external source prevented.');
      return;
    }
    // Do not automatically launch downloaded executables
  });

  // Load renderer UI
  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    const decision = navigationGuard.evaluate(url);
    if (decision.allowed) {
      // Open in tab if tabs enabled
      addNewTab(url);
    } else {
      notifyBlocked(url, 'Popup to unauthorized external domain prevented.');
    }
    return { action: 'deny' };
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

function notifyBlocked(url: string, reason: string) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('browser:blockedAlert', {
      url,
      reason,
      timestamp: Date.now()
    });
  }
}

function broadcastState() {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('browser:stateUpdate', {
      tabs,
      activeTabId,
      isKiosk,
      allowedDomain: CONFIG.ALLOWED_DOMAINS[0]
    });
  }
}

function addNewTab(url: string = CONFIG.ALLOWED_SITE_URL) {
  if (tabs.length >= CONFIG.MAX_TABS) {
    notifyBlocked(url, `Maximum tab limit (${CONFIG.MAX_TABS}) reached.`);
    return;
  }
  const newId = `tab-${Date.now()}`;
  tabs.push({
    id: newId,
    title: 'Locked Portal',
    url,
    canGoBack: false,
    canGoForward: false,
    isLoading: false
  });
  activeTabId = newId;
  broadcastState();
}

// IPC Handlers
ipcMain.on('browser:newTab', () => addNewTab());
ipcMain.on('browser:switchTab', (_, tabId) => {
  activeTabId = tabId;
  broadcastState();
});
ipcMain.on('browser:closeTab', (_, tabId) => {
  if (tabs.length > 1) {
    tabs = tabs.filter((t) => t.id !== tabId);
    if (activeTabId === tabId) {
      activeTabId = tabs[0].id;
    }
    broadcastState();
  }
});
ipcMain.on('browser:toggleKiosk', () => {
  isKiosk = !isKiosk;
  mainWindow?.setFullScreen(isKiosk);
  broadcastState();
});

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
