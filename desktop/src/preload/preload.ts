import { contextBridge, ipcRenderer } from 'electron';

// Expose safe, isolated browser controls to the renderer window
contextBridge.exposeInMainWorld('siteLockAPI', {
  // Navigation
  goBack: () => ipcRenderer.send('browser:goBack'),
  goForward: () => ipcRenderer.send('browser:goForward'),
  reload: () => ipcRenderer.send('browser:reload'),
  stop: () => ipcRenderer.send('browser:stop'),
  loadUrl: (url: string) => ipcRenderer.send('browser:loadUrl', url),

  // Tabs
  newTab: () => ipcRenderer.send('browser:newTab'),
  switchTab: (tabId: string) => ipcRenderer.send('browser:switchTab', tabId),
  closeTab: (tabId: string) => ipcRenderer.send('browser:closeTab', tabId),

  // Window & Kiosk
  toggleKiosk: () => ipcRenderer.send('browser:toggleKiosk'),
  minimize: () => ipcRenderer.send('window:minimize'),
  maximize: () => ipcRenderer.send('window:maximize'),
  close: () => ipcRenderer.send('window:close'),

  // Events from Main process
  onStateUpdate: (callback: (state: any) => void) => {
    ipcRenderer.on('browser:stateUpdate', (_, state) => callback(state));
  },
  onBlockedAlert: (callback: (data: any) => void) => {
    ipcRenderer.on('browser:blockedAlert', (_, data) => callback(data));
  }
});
