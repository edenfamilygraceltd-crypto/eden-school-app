(() => {
  'use strict';
  const VERSION_URL = '/downloads/version.json';
  const DOWNLOAD_URL = '/downloads/eden-smart.apk';
  const SITE_URL = `${window.location.origin}/#eden-smart-download`;
  const STORAGE_KEY = 'edenSmartSeenAppVersion';
  let banner;

  function ensureBanner() {
    if (banner) return banner;
    banner = document.createElement('aside');
    banner.id = 'edenAppUpdateNotice';
    banner.setAttribute('role', 'status');
    banner.setAttribute('aria-live', 'polite');
    banner.style.cssText = 'display:none;position:fixed;z-index:99999;left:16px;right:16px;bottom:16px;max-width:720px;margin:auto;padding:16px 18px;background:#10245b;color:#fff;border:1px solid #9bb8ff;border-radius:14px;box-shadow:0 12px 36px #0005;font:500 15px/1.5 system-ui,sans-serif';
    document.body.appendChild(banner);
    return banner;
  }

  function showUpdate(release) {
    const box = ensureBanner();
    box.replaceChildren();
    const text = document.createElement('div');
    text.textContent = release.message || `Une nouvelle mise à jour (${release.version}) est disponible sur Eden Family School.`;
    text.style.marginBottom = '12px';
    const download = document.createElement('a');
    download.href = release.downloadUrl || DOWNLOAD_URL;
    download.download = 'Eden-Smart.apk';
    download.textContent = 'Télécharger la mise à jour';
    download.style.cssText = 'display:inline-block;background:#fff;color:#17317b;font-weight:800;text-decoration:none;padding:10px 14px;border-radius:9px;margin-right:10px';
    const close = document.createElement('button');
    close.type = 'button';
    close.textContent = 'Plus tard';
    close.style.cssText = 'background:transparent;color:#fff;border:1px solid #c5d3ff;border-radius:9px;padding:9px 12px';
    close.addEventListener('click', () => { box.style.display = 'none'; });
    box.append(text, download, close);
    box.style.display = 'block';

    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        const notification = new Notification('Mise à jour Eden Family School', {
          body: text.textContent,
          icon: '/asset/img/logo_eden.png',
          tag: 'eden-smart-update'
        });
        notification.onclick = () => {
          window.focus();
          window.location.href = SITE_URL;
          notification.close();
        };
      } catch (error) { console.info('Notification système indisponible :', error); }
    }
  }

  async function checkVersion() {
    try {
      const response = await fetch(`${VERSION_URL}?_=${Date.now()}`, { cache: 'no-store' });
      if (!response.ok) return;
      const release = await response.json();
      if (!release || !release.version) return;
      const version = String(release.version);
      const previousVersion = localStorage.getItem(STORAGE_KEY);
      if (!previousVersion) {
        localStorage.setItem(STORAGE_KEY, version);
        return;
      }
      if (previousVersion !== version) {
        localStorage.setItem(STORAGE_KEY, version);
        showUpdate(release);
      }
    } catch (error) {
      console.info('Vérification des mises à jour indisponible :', error);
    }
  }

  function addNotificationControl() {
    const section = document.getElementById('eden-smart-download');
    if (!section || document.getElementById('edenEnableUpdateNotifications')) return;
    const button = document.createElement('button');
    button.id = 'edenEnableUpdateNotifications';
    button.type = 'button';
    button.textContent = 'Activer les notifications de mise à jour';
    button.style.cssText = 'display:inline-flex;align-items:center;justify-content:center;padding:12px 16px;background:#dbe7ff;color:#17317b;border:1px solid #fff;border-radius:12px;font-weight:800;cursor:pointer;margin-top:12px';
    if (!('Notification' in window)) {
      button.textContent = 'Les notifications ne sont pas prises en charge par ce navigateur';
      button.disabled = true;
    } else {
      button.addEventListener('click', async () => {
        try {
          const permission = await Notification.requestPermission();
          if (permission === 'granted') {
            button.textContent = 'Notifications autorisées sur cet appareil';
            await checkVersion();
          } else {
            button.textContent = 'Autorisation refusée : vérifiez les paramètres du navigateur';
          }
        } catch (_) {
          button.textContent = 'Notifications indisponibles dans ce navigateur';
        }
      });
    }
    const container = section.querySelector('div[style*="max-width:1080px"]') || section;
    container.appendChild(button);
  }

  window.addEventListener('DOMContentLoaded', () => {
    addNotificationControl();
    checkVersion();
    window.setInterval(checkVersion, 60000);
  });
})();
