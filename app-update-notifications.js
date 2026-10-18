(() => {
  'use strict';
  const MANIFEST = '/downloads/version.json';
  const APK = '/downloads/eden-smart.apk';
  const KEY = 'edenSmartSeenAppRelease';
  let checking = false;
  let versionLine, statusLine, checkButton, notice;

  const button = (label, primary) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = label;
    b.style.cssText = 'min-height:44px;padding:11px 15px;border-radius:10px;border:1px solid '+(primary?'#2449a5':'#cbd5e1')+';background:'+(primary?'#2449a5':'#fff')+';color:'+(primary?'#fff':'#17317b')+';font:700 14px system-ui,sans-serif;cursor:pointer';
    return b;
  };

  function showNotice(release) {
    if (!notice) {
      notice = document.createElement('aside');
      notice.id = 'edenAppUpdateNotice';
      notice.setAttribute('role','status');
      notice.setAttribute('aria-live','polite');
      notice.style.cssText = 'box-sizing:border-box;display:none;position:fixed;z-index:99999;left:16px;right:16px;bottom:16px;max-width:720px;margin:auto;padding:16px;background:#10245b;color:#fff;border:1px solid #9bb8ff;border-radius:14px;box-shadow:0 12px 36px #0005;font:500 15px/1.5 system-ui,sans-serif';
      document.body.appendChild(notice);
    }
    notice.replaceChildren();
    const msg = document.createElement('p');
    msg.textContent = release.appMessage || 'Une nouvelle version d\u2019Eden Smart est disponible.';
    msg.style.margin = '0 0 12px';
    const link = document.createElement('a');
    link.href = release.downloadUrl || APK;
    link.textContent = 'T\u00e9l\u00e9charger la mise \u00e0 jour';
    link.style.cssText = 'display:inline-block;background:#fff;color:#17317b;font-weight:800;text-decoration:none;padding:10px 14px;border-radius:9px;margin-right:10px';
    const close = button('Plus tard', false);
    close.style.background = 'transparent';
    close.style.color = '#fff';
    close.style.borderColor = '#c5d3ff';
    close.onclick = () => { notice.style.display = 'none'; };
    notice.append(msg, link, close);
    notice.style.display = 'block';

    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        const n = new Notification('Mise \u00e0 jour Eden Smart', {body: msg.textContent, icon:'/asset/img/logo_eden.png', tag:'eden-smart-update'});
        n.onclick = () => { window.focus(); window.location.href = location.origin + '/#eden-smart-download'; n.close(); };
      } catch (e) { console.info('Notification système indisponible', e); }
    }
  }

  async function check(manual) {
    if (checking) return;
    checking = true;
    if (checkButton) { checkButton.disabled = true; checkButton.textContent = 'Vérification…'; }
    if (statusLine) statusLine.textContent = 'Connexion au service de mise à jour…';
    try {
      const response = await fetch(MANIFEST + '?_=' + Date.now(), {cache:'no-store', headers:{Accept:'application/json'}});
      if (!response.ok) throw new Error('HTTP ' + response.status);
      const release = await response.json();
      if (!release || !release.appVersion || !Number.isFinite(Number(release.appVersionCode))) throw new Error('Manifeste incomplet');
      if (versionLine) versionLine.textContent = 'Dernière version publiée : ' + release.appVersion + ' (code ' + release.appVersionCode + ')';
      if (statusLine) statusLine.textContent = 'Le service fonctionne. Vous pouvez télécharger la dernière version de l’application Android.';
      const key = String(release.appVersionCode) + ':' + String(release.appVersion);
      let previous = null;
      try { previous = localStorage.getItem(KEY); } catch (_) {}
      if (!previous) {
        try { localStorage.setItem(KEY, key); } catch (_) {}
      } else if (previous !== key) {
        try { localStorage.setItem(KEY, key); } catch (_) {}
        showNotice(release);
        if (statusLine) statusLine.textContent = 'Nouvelle version détectée. Téléchargez l’APK pour mettre à jour Eden Smart.';
      } else if (manual && statusLine) {
        statusLine.textContent = 'Vérification terminée : la dernière version publiée est ' + release.appVersion + '.';
      }
    } catch (e) {
      if (statusLine) statusLine.textContent = 'Impossible de vérifier la version. Vérifiez votre connexion puis réessayez.';
      console.warn('Vérification des mises à jour impossible', e);
    } finally {
      checking = false;
      if (checkButton) { checkButton.disabled = false; checkButton.textContent = 'Vérifier les mises à jour'; }
    }
  }

  function init() {
    const section = document.getElementById('eden-smart-download');
    if (!section) return;
    const title = document.getElementById('edenSmartDownloadTitle');
    if (title) title.textContent = 'Télécharger Eden Smart';
    const desc = section.querySelector('div[style*="max-width:1080px"] p');
    if (desc) desc.textContent = 'Application officielle de présence des enseignants : choisissez votre branche, enregistrez les arrivées et départs, puis synchronisez les présences avec l’école.';
    const oldDownload = section.querySelector('a[download]');
    if (oldDownload) oldDownload.textContent = 'Télécharger l’application Android';

    const panel = document.createElement('div');
    panel.id = 'edenAppUpdatePanel';
    panel.style.cssText = 'box-sizing:border-box;width:100%;max-width:1080px;margin:24px auto 0;padding:18px;background:#fff;color:#172554;border:1px solid #dbe5ff;border-radius:16px;box-shadow:0 8px 24px #07133d22';
    const h = document.createElement('h3');
    h.textContent = 'Mises à jour d’Eden Smart';
    h.style.cssText = 'margin:0 0 8px;font:800 1.1rem/1.35 system-ui,sans-serif;color:#17317b';
    const p = document.createElement('p');
    p.textContent = 'Vérifiez la dernière version publiée et téléchargez l’application Android officielle.';
    p.style.cssText = 'margin:0 0 10px;line-height:1.55;color:#334155';
    versionLine = document.createElement('p');
    versionLine.style.cssText = 'margin:0 0 6px;font-weight:800;color:#17317b';
    versionLine.textContent = 'Dernière version publiée : vérification en cours…';
    statusLine = document.createElement('p');
    statusLine.setAttribute('role','status');
    statusLine.setAttribute('aria-live','polite');
    statusLine.style.cssText = 'margin:0 0 14px;line-height:1.5;color:#475569';
    statusLine.textContent = 'Connexion au service de mise à jour…';
    const actions = document.createElement('div');
    actions.style.cssText = 'display:flex;flex-wrap:wrap;align-items:center;gap:10px';
    checkButton = button('Vérifier les mises à jour', true);
    checkButton.onclick = () => check(true);
    const download = document.createElement('a');
    download.href = APK;
    download.textContent = 'Télécharger Eden Smart (APK)';
    download.setAttribute('download','Eden-Smart.apk');
    download.style.cssText = 'display:inline-flex;align-items:center;justify-content:center;min-height:44px;padding:11px 15px;background:#eef2ff;color:#17317b;border:1px solid #c7d2fe;border-radius:10px;font:700 14px system-ui,sans-serif;text-decoration:none';
    const notifications = button('Activer les notifications', false);
    notifications.onclick = async () => {
      if (!('Notification' in window)) { statusLine.textContent = 'Ce navigateur ne prend pas en charge les notifications système. La vérification manuelle reste disponible.'; return; }
      if (!window.isSecureContext) { statusLine.textContent = 'Les notifications nécessitent une connexion HTTPS sécurisée.'; return; }
      try {
        const permission = await Notification.requestPermission();
        if (permission === 'granted') {
          notifications.textContent = 'Notifications activées';
          statusLine.textContent = 'Notifications autorisées lorsque le site est ouvert. Les alertes en arrière-plan lorsque le site est fermé ne sont pas encore activées.';
        } else {
          statusLine.textContent = 'Autorisation refusée. Vous pouvez toujours vérifier et télécharger les mises à jour ici.';
        }
      } catch (_) { statusLine.textContent = 'Impossible d’activer les notifications dans ce navigateur.'; }
    };
    actions.append(checkButton, download, notifications);
    panel.append(h,p,versionLine,statusLine,actions);
    const container = section.querySelector('div[style*="max-width:1080px"]') || section;
    container.appendChild(panel);
    check(false);
    window.setInterval(() => check(false), 60000);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, {once:true});
  else init();
})();
