(() => {
  'use strict';
  const MANIFEST_URL = '/downloads/version.json';
  const APK_URL = '/downloads/eden-smart.apk';
  const SITE_KEY = 'edenSmartSeenSiteVersion';
  const APP_KEY = 'edenSmartSeenAppRelease';
  const LEGACY_APP_KEY = 'edenSmartSeenAppVersion';
  const CHECK_INTERVAL_MS = 60000;
  let checking = false, intervalId = null, versionLine, statusLine, checkButton, notificationButton, notice;

  function makeButton(label, primary) {
    const b = document.createElement('button');
    b.type = 'button'; b.textContent = label;
    b.style.cssText = 'min-height:44px;padding:11px 15px;border-radius:10px;border:1px solid '+(primary?'#2449a5':'#cbd5e1')+';background:'+(primary?'#2449a5':'#fff')+';color:'+(primary?'#fff':'#17317b')+';font:700 14px system-ui,sans-serif;cursor:pointer';
    return b;
  }
  function get(key) { try { return localStorage.getItem(key); } catch (_) { return null; } }
  function set(key, value) { try { localStorage.setItem(key, value); } catch (_) {} }

  function showNotice(kind, release) {
    const isApp = kind === 'app';
    const title = isApp ? 'Mise à jour d’Eden Smart' : 'Mise à jour du site';
    const message = isApp ? (release.appMessage || 'Une nouvelle version d’Eden Smart est disponible.') : (release.message || 'Une mise à jour du site Eden Family School est disponible.');
    if (!notice) {
      notice = document.createElement('aside');
      notice.id = 'edenAppUpdateNotice'; notice.setAttribute('role','status'); notice.setAttribute('aria-live','polite');
      notice.style.cssText = 'box-sizing:border-box;display:none;position:fixed;z-index:99999;left:16px;right:16px;bottom:16px;max-width:720px;margin:auto;padding:16px;background:#10245b;color:#fff;border:1px solid #9bb8ff;border-radius:14px;box-shadow:0 12px 36px #0005;font:500 15px/1.5 system-ui,sans-serif';
      document.body.appendChild(notice);
    }
    notice.replaceChildren();
    const h = document.createElement('strong'); h.textContent = title; h.style.cssText = 'display:block;margin-bottom:6px;font-size:16px';
    const p = document.createElement('p'); p.textContent = message; p.style.margin = '0 0 12px';
    const a = document.createElement('a'); a.href = isApp ? (release.downloadUrl || APK_URL) : '/'; a.textContent = isApp ? 'Télécharger la mise à jour' : 'Ouvrir le site actualisé';
    a.style.cssText = 'display:inline-block;background:#fff;color:#17317b;font-weight:800;text-decoration:none;padding:10px 14px;border-radius:9px;margin-right:10px';
    const close = makeButton('Plus tard', false); close.style.background='transparent'; close.style.color='#fff'; close.style.borderColor='#c5d3ff'; close.onclick=()=>{notice.style.display='none';};
    notice.append(h,p,a,close); notice.style.display='block';
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        const n = new Notification(title,{body:message,icon:'/asset/img/logo_eden.png',tag:'eden-'+kind+'-update'});
        n.onclick=()=>{window.focus();window.location.href=isApp?(release.downloadUrl||APK_URL):'/';n.close();};
      } catch (e) { console.info('Notification système indisponible.',e); }
    }
  }

  async function checkUpdates(manual) {
    if (checking) return;
    checking=true;
    if(checkButton){checkButton.disabled=true;checkButton.textContent='Vérification en cours…';}
    if(statusLine)statusLine.textContent='Connexion au service de mise à jour…';
    try {
      const response=await fetch(MANIFEST_URL+'?_='+Date.now(),{cache:'no-store',headers:{Accept:'application/json'}});
      if(!response.ok)throw new Error('HTTP '+response.status);
      const release=await response.json();
      if(!release||!release.siteVersion||!release.appVersion||!Number.isFinite(Number(release.appVersionCode))||!release.downloadUrl)throw new Error('Manifeste de version incomplet.');
      const siteVersion=String(release.siteVersion);
      const appVersion=String(release.appVersionCode)+':'+String(release.appVersion);
      const oldSite=get(SITE_KEY);
      const oldApp=get(APP_KEY)||get(LEGACY_APP_KEY);
      if(versionLine)versionLine.textContent='Site : '+siteVersion+' · Eden Smart : v'+release.appVersion+' (code '+release.appVersionCode+')';
      let updateKind=null;
      if(oldApp&&oldApp!==appVersion)updateKind='app';
      else if(oldSite&&oldSite!==siteVersion)updateKind='site';
      set(SITE_KEY,siteVersion); set(APP_KEY,appVersion);
      if(updateKind){
        showNotice(updateKind,release);
        if(statusLine)statusLine.textContent=updateKind==='app'?'Nouvelle version d’Eden Smart détectée. Téléchargez l’APK puis installez-la sur votre téléphone.':'Une nouvelle version du site est disponible. Actualisez la page pour charger les dernières modifications.';
      } else if(statusLine) {
        statusLine.textContent=manual?'Vérification terminée : le service fonctionne et les versions sont à jour.':'Service opérationnel. Vérification automatique activée pendant votre visite.';
      }
    } catch(error) {
      if(statusLine)statusLine.textContent='Vérification impossible. Vérifiez votre connexion Internet puis réessayez.';
      console.warn('Vérification des mises à jour impossible.',error);
    } finally {
      checking=false;
      if(checkButton){checkButton.disabled=false;checkButton.textContent='Vérifier les mises à jour';}
    }
  }

  async function enableNotifications() {
    if(!('Notification' in window)){statusLine.textContent='Ce navigateur ne prend pas en charge les notifications système. La vérification manuelle reste disponible.';return;}
    if(!window.isSecureContext){statusLine.textContent='Les notifications nécessitent une connexion HTTPS sécurisée.';return;}
    try {
      const permission=Notification.permission==='default'?await Notification.requestPermission():Notification.permission;
      if(permission==='granted'){notificationButton.textContent='Notifications autorisées';statusLine.textContent='Notifications autorisées pendant que le site est ouvert. Les alertes lorsque le site est fermé nécessitent encore un service push.';}
      else statusLine.textContent='Autorisation refusée. Vous pouvez toujours vérifier et télécharger les mises à jour ici.';
    } catch(_){statusLine.textContent='Impossible d’activer les notifications dans ce navigateur.';}
  }

  function init() {
    const section=document.getElementById('eden-smart-download');
    if(!section||document.getElementById('edenAppUpdatePanel'))return;
    const oldDownload=section.querySelector('a[download]');
    if(oldDownload)oldDownload.textContent='Télécharger l’application Android';
    const panel=document.createElement('div');panel.id='edenAppUpdatePanel';
    panel.style.cssText='box-sizing:border-box;width:100%;max-width:1080px;margin:24px auto 0;padding:18px;background:#fff;color:#172554;border:1px solid #dbe5ff;border-radius:16px;box-shadow:0 8px 24px #07133d22';
    const h=document.createElement('h3');h.textContent='Mises à jour du site et d’Eden Smart';h.style.cssText='margin:0 0 8px;font:800 1.1rem/1.35 system-ui,sans-serif;color:#17317b';
    const p=document.createElement('p');p.textContent='Vérifiez les versions publiées, recevez une alerte pendant votre visite et téléchargez la dernière application Android officielle.';p.style.cssText='margin:0 0 10px;line-height:1.55;color:#334155';
    versionLine=document.createElement('p');versionLine.style.cssText='margin:0 0 6px;font-weight:800;color:#17317b';versionLine.textContent='Versions publiées : vérification en cours…';
    statusLine=document.createElement('p');statusLine.setAttribute('role','status');statusLine.setAttribute('aria-live','polite');statusLine.style.cssText='margin:0 0 14px;line-height:1.5;color:#475569';statusLine.textContent='Connexion au service de mise à jour…';
    const actions=document.createElement('div');actions.style.cssText='display:flex;flex-wrap:wrap;align-items:center;gap:10px';
    checkButton=makeButton('Vérifier les mises à jour',true);checkButton.onclick=()=>checkUpdates(true);
    const download=document.createElement('a');download.href=APK_URL;download.textContent='Télécharger Eden Smart (APK)';download.setAttribute('download','Eden-Smart.apk');download.style.cssText='display:inline-flex;align-items:center;justify-content:center;min-height:44px;padding:11px 15px;background:#eef2ff;color:#17317b;border:1px solid #c7d2fe;border-radius:10px;font:700 14px system-ui,sans-serif;text-decoration:none';
    notificationButton=makeButton('Activer les notifications',false);notificationButton.onclick=enableNotifications;
    if('Notification'in window&&Notification.permission==='granted')notificationButton.textContent='Notifications autorisées';
    actions.append(checkButton,download,notificationButton);panel.append(h,p,versionLine,statusLine,actions);
    const container=section.querySelector('div[style*="max-width:1080px"]')||section;container.appendChild(panel);
    checkUpdates(false);
    if(intervalId===null)intervalId=window.setInterval(()=>checkUpdates(false),CHECK_INTERVAL_MS);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init,{once:true});else init();
})();
