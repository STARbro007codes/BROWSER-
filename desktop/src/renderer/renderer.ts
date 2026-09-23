// Secure desktop renderer logic communicating through window.siteLockAPI

declare global {
  interface Window {
    siteLockAPI: any;
  }
}

const activeWebview = document.getElementById('active-webview') as any;
const addressDisplay = document.getElementById('address-display') as HTMLElement;
const btnBack = document.getElementById('btn-back') as HTMLButtonElement;
const btnForward = document.getElementById('btn-forward') as HTMLButtonElement;
const btnReload = document.getElementById('btn-reload') as HTMLButtonElement;
const btnNewTab = document.getElementById('btn-new-tab') as HTMLButtonElement;
const btnKiosk = document.getElementById('btn-kiosk') as HTMLButtonElement;
const tabsContainer = document.getElementById('tabs-container') as HTMLElement;
const blockedToast = document.getElementById('blocked-toast') as HTMLElement;
const blockedToastMsg = document.getElementById('blocked-toast-msg') as HTMLElement;

let toastTimer: any = null;

// Controls
btnBack.addEventListener('click', () => {
  if (activeWebview && activeWebview.canGoBack()) activeWebview.goBack();
});

btnForward.addEventListener('click', () => {
  if (activeWebview && activeWebview.canGoForward()) activeWebview.goForward();
});

btnReload.addEventListener('click', () => {
  if (activeWebview) activeWebview.reload();
});

btnNewTab.addEventListener('click', () => {
  window.siteLockAPI?.newTab();
});

btnKiosk.addEventListener('click', () => {
  window.siteLockAPI?.toggleKiosk();
});

// Update webview events
if (activeWebview) {
  activeWebview.addEventListener('did-start-loading', () => {
    btnReload.textContent = '✕';
  });

  activeWebview.addEventListener('did-stop-loading', () => {
    btnReload.textContent = '⟳';
    if (addressDisplay) addressDisplay.textContent = activeWebview.getURL();
    btnBack.disabled = !activeWebview.canGoBack();
    btnForward.disabled = !activeWebview.canGoForward();
  });

  activeWebview.addEventListener('did-navigate', (e: any) => {
    if (addressDisplay) addressDisplay.textContent = e.url;
  });

  activeWebview.addEventListener('did-navigate-in-page', (e: any) => {
    if (addressDisplay) addressDisplay.textContent = e.url;
  });
}

// Listen to main process state
window.siteLockAPI?.onStateUpdate((state: any) => {
  renderTabs(state.tabs, state.activeTabId);
});

window.siteLockAPI?.onBlockedAlert((data: any) => {
  showBlockedToast(`Navigation Blocked: ${data.reason || 'External domain prohibited'}`);
});

function renderTabs(tabs: any[], activeId: string) {
  if (!tabsContainer) return;
  tabsContainer.innerHTML = '';
  tabs.forEach((tab) => {
    const tabEl = document.createElement('div');
    tabEl.className = `tab ${tab.id === activeId ? 'active' : ''}`;
    tabEl.innerHTML = `
      <span>${tab.title || 'Locked Portal'}</span>
      ${tabs.length > 1 ? `<span class="tab-close" data-id="${tab.id}">×</span>` : ''}
    `;

    tabEl.addEventListener('click', (e) => {
      const target = e.target as HTMLElement;
      if (target.classList.contains('tab-close')) {
        window.siteLockAPI?.closeTab(tab.id);
      } else {
        window.siteLockAPI?.switchTab(tab.id);
      }
    });

    tabsContainer.appendChild(tabEl);
  });
}

function showBlockedToast(message: string) {
  if (blockedToast && blockedToastMsg) {
    blockedToastMsg.textContent = message;
    blockedToast.classList.remove('hidden');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      blockedToast.classList.add('hidden');
    }, 4000);
  }
}
