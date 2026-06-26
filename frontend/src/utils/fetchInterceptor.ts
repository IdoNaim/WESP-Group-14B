let originalFetch = window.fetch;

export function setupFetchInterceptor() {
  originalFetch = window.fetch;
  window.fetch = async function (...args) {
    if (navigator.onLine === false) {
      showGlobalOfflineModal();
      const offlineError = new Error('No internet connection. Please check your network settings.');
      offlineError.name = 'OfflineError';
      throw offlineError;
    }

    try {
      const response = await originalFetch(...args);
      return response;
    } catch (error: any) {
      const msg = error.message || '';
      if (
        msg.toLowerCase().includes('failed to fetch') ||
        msg.toLowerCase().includes('load failed') ||
        msg.toLowerCase().includes('network error')
      ) {
        showGlobalOfflineModal();
        const offlineError = new Error('No internet connection. Please check your network settings.');
        offlineError.name = 'OfflineError';
        throw offlineError;
      }
      throw error;
    }
  };
}

function showGlobalOfflineModal() {
  if (document.getElementById('global-offline-modal')) {
    return;
  }

  const modal = document.createElement('div');
  modal.id = 'global-offline-modal';
  modal.style.position = 'fixed';
  modal.style.inset = '0';
  modal.style.backgroundColor = 'rgba(11, 19, 38, 0.8)';
  modal.style.backdropFilter = 'blur(4px)';
  modal.style.display = 'flex';
  modal.style.alignItems = 'center';
  modal.style.justifyContent = 'center';
  modal.style.zIndex = '99999';
  modal.style.color = '#dae2fd';
  modal.style.fontFamily = 'sans-serif';

  const card = document.createElement('div');
  card.style.backgroundColor = '#eeefff';
  card.style.color = '#171f33';
  card.style.padding = '32px';
  card.style.borderRadius = '16px';
  card.style.boxShadow = '0 25px 50px -12px rgba(0, 0, 0, 0.25)';
  card.style.maxWidth = '400px';
  card.style.width = '90%';
  card.style.textAlign = 'center';
  card.style.display = 'flex';
  card.style.flexDirection = 'column';
  card.style.alignItems = 'center';
  card.style.gap = '16px';

  const icon = document.createElement('span');
  icon.className = 'material-symbols-outlined';
  icon.textContent = 'cloud_off';
  icon.style.fontSize = '48px';
  icon.style.color = '#dc2626';

  const title = document.createElement('h2');
  title.textContent = 'Connection Failed';
  title.style.fontSize = '20px';
  title.style.fontWeight = 'bold';
  title.style.margin = '0';

  const message = document.createElement('p');
  message.textContent = 'No internet connection. Please check your network settings.';
  message.style.fontSize = '14px';
  message.style.color = '#4b5563';
  message.style.margin = '0';

  const button = document.createElement('button');
  button.textContent = 'Retry';
  button.style.backgroundColor = '#2563eb';
  button.style.color = 'white';
  button.style.padding = '12px 24px';
  button.style.borderRadius = '8px';
  button.style.border = 'none';
  button.style.fontWeight = 'bold';
  button.style.cursor = 'pointer';
  button.style.transition = 'background-color 0.2s';
  button.onmouseover = () => { button.style.backgroundColor = '#1d4ed8'; };
  button.onmouseout = () => { button.style.backgroundColor = '#2563eb'; };
  button.onclick = async () => {
    button.textContent = 'Checking connection...';
    button.disabled = true;
    try {
      // silent, lightweight background request to backend guest entry (a basic endpoint)
      await originalFetch('/api/identity/guest', { method: 'POST' });
      modal.remove();
      window.location.reload();
    } catch (err) {
      // Still offline
      message.textContent = 'Still offline. Please check your connection and try again.';
      message.style.color = '#dc2626';
      button.textContent = 'Retry';
      button.disabled = false;
    }
  };

  card.appendChild(icon);
  card.appendChild(title);
  card.appendChild(message);
  card.appendChild(button);
  modal.appendChild(card);
  document.body.appendChild(modal);
}
