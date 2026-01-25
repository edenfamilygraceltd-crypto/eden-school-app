# 📦 GUIDE DE DÉPLOIEMENT EN PRODUCTION - SÉCURITÉ

## ✅ PRÉ-DÉPLOIEMENT (À FAIRE AVANT LA MISE EN LIGNE)

### 1. Audit de code final
```bash
# Vérifier qu'il n'y a pas de console.log sensibles
grep -r "password\|token\|secret" *.js | grep -v node_modules

# Vérifier les fichiers sensibles ne sont pas exposés
ls -la | grep -E "\.env|\.config|backup|\.sql"
```

### 2. Tests de sécurité
```bash
# Exécuter les tests de sécurité
# Dans la console du navigateur (F12):
SECURITY_TESTS.runAllTests();

# Utiliser un scanner de sécurité
# https://www.ssllabs.com/ssltest/analyze.html?d=votre-domaine.com
```

### 3. Vérifier la configuration
- [ ] HTTPS configuré et activé
- [ ] Certificat SSL valide (Let's Encrypt recommandé)
- [ ] Certificat SSL sur les sous-domaines
- [ ] Redirection HTTP → HTTPS en place
- [ ] Cookies HttpOnly activés
- [ ] Cookies Secure activés
- [ ] SameSite=Strict configuré

### 4. Configuration côté serveur
- [ ] En-têtes de sécurité HTTP configurés (voir CONFIG_SECURITE_SERVEUR.md)
- [ ] CSP implémenté
- [ ] CORS correctement limité
- [ ] Rate limiting en place
- [ ] WAF (Web Application Firewall) en place
- [ ] Backups en place

### 5. Préparer Firebase
```bash
# Vérifier les règles de sécurité Firestore
firebase deploy --only firestore:rules

# Vérifier les règles de base de données
firebase deploy --only database

# Vérifier les règles d'authentification
firebase deploy --only auth
```

### 6. Variables d'environnement
```bash
# Créer un fichier .env.production (JAMAIS en versioning)
FIREBASE_API_KEY=xxx
FIREBASE_AUTH_DOMAIN=xxx
FIREBASE_DATABASE_URL=xxx
# ... autres variables
```

### 7. Nettoyage du code
```bash
# Supprimer les fichiers de test/développement
rm -f *.test.js *.dev.js

# Minifier le JavaScript (production)
npx uglify-js security-auth.js -c -m -o security-auth.min.js

# Minifier le CSS (production)
npx cleancss -o style.min.css style.css
```

---

## 🚀 DÉPLOIEMENT

### Option 1: Déployer avec Firebase Hosting
```bash
# Initialiser Firebase
firebase init hosting

# Vérifier la configuration
cat firebase.json

# Déployer
firebase deploy

# Déployer avec preview
firebase hosting:channel:deploy preview
```

### Option 2: Déployer sur un serveur Node.js
```bash
# 1. Préparer le serveur
ssh user@serveur.com
mkdir -p ~/eden-school

# 2. Copier les fichiers
scp -r ./* user@serveur.com:~/eden-school/

# 3. Installer les dépendances
cd ~/eden-school
npm install --production

# 4. Démarrer avec PM2 (process manager)
npm install -g pm2
pm2 start server.js --name "eden-school"
pm2 save
pm2 startup
```

### Option 3: Déployer sur Vercel
```bash
# Installer Vercel CLI
npm i -g vercel

# Déployer
vercel --prod
```

### Option 4: Déployer sur Netlify
```bash
# Installer Netlify CLI
npm i -g netlify-cli

# Déployer
netlify deploy --prod
```

---

## 🔐 POST-DÉPLOIEMENT

### 1. Vérifier le déploiement
```bash
# Tester HTTPS
curl -I https://votre-domaine.com

# Vérifier les en-têtes de sécurité
curl -I https://votre-domaine.com | grep -i "x-\|strict\|csp"

# Scanner de sécurité
https://www.ssllabs.com/ssltest/analyze.html

# Vérifier CSP
https://csp-evaluator.withgoogle.com/
```

### 2. Activer le monitoring
- [ ] Configurer Sentry pour les erreurs JavaScript
- [ ] Configurer les logs de sécurité
- [ ] Configurer les alertes
- [ ] Configurer la surveillance de disponibilité
- [ ] Configurer les sauvegardes automatiques

### 3. Vérifier les logs
```bash
# Affichage des erreurs en direct
pm2 logs eden-school

# Afficher les logs de sécurité
tail -f /var/log/eden-school/security.log
```

### 4. Tester les scénarios critiques
- [ ] Se connecter normalement ✅
- [ ] Accéder aux pages protégées ✅
- [ ] Essayer d'accéder sans authentification ❌
- [ ] Essayer d'accéder avec mauvais rôle ❌
- [ ] Timeout de session (30 min) ✅
- [ ] Déconnexion ✅

---

## 📊 MONITORING EN PRODUCTION

### 1. Configurer Sentry
```javascript
// Dans index.html ou Auth.html
<script src="https://browser.sentry-cdn.com/7.0.0/bundle.min.js" integrity="sha384-xxx"></script>
<script>
  Sentry.init({
    dsn: "https://xxx@xxx.ingest.sentry.io/xxx",
    environment: "production",
    tracesSampleRate: 1.0,
  });
</script>
```

### 2. Configurer les logs de sécurité
```javascript
// Dans security-auth.js
logSecurityEvent(eventType, data = {}) {
    // Envoyer au serveur en production
    if (window.location.hostname !== 'localhost') {
      fetch('/api/security-logs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          timestamp: new Date().toISOString(),
          eventType,
          userAgent: navigator.userAgent,
          url: window.location.href,
          data
        })
      }).catch(err => console.error('Erreur log:', err));
    }
}
```

### 3. Alertes de sécurité
```javascript
// Configurer des seuils d'alerte
- Tentatives de connexion échouées > 5 par heure
- Accès refusé > 10 par heure
- Sessions expirées > 20 par heure
- Erreurs > 50 par heure
```

---

## 🔄 MISES À JOUR ET MAINTENANCES

### Mise à jour sécuritaire
```bash
# 1. Test en environnement de staging d'abord
git checkout develop
git pull origin develop
npm install
npm test

# 2. Déployer en staging
firebase hosting:channel:deploy staging

# 3. Tester en staging
SECURITY_TESTS.runAllTests();

# 4. Approuver et déployer en production
firebase deploy --only hosting:default
```

### Rotation des secrets
- [ ] Changer les clés Firebase mensuellement
- [ ] Changer les certificats SSL avant expiration
- [ ] Auditer les permissions Firebase mensuellement
- [ ] Nettoyer les anciennes sessions mensuellement

---

## 📋 CHECKLIST DE LANCEMENT

```
PRÉ-LANCEMENT
[ ] Tous les tests passent (SECURITY_TESTS.runAllTests())
[ ] Audit de sécurité final complété
[ ] Scanner SSL Labs: A+ ou A
[ ] Pas de données sensibles en logs/code
[ ] Certificat SSL valide (>30 jours avant expiration)
[ ] HTTPS forcé via 301 redirect
[ ] CSP configuré et testé
[ ] En-têtes de sécurité HTTP configurés
[ ] CORS limité aux origines connues
[ ] Rate limiting en place
[ ] Backups en place
[ ] Plan de réponse aux incidents

LANCEMENT
[ ] Déployer en production
[ ] Vérifier tous les tests
[ ] Vérifier les logs
[ ] Vérifier la performance
[ ] Monitorer les erreurs (Sentry)

POST-LANCEMENT
[ ] Monitoring actif pendant 24h
[ ] Alertes configurées et testées
[ ] Logs de sécurité actifs
[ ] Support disponible
[ ] Communication utilisateurs si nécessaire
```

---

## 🆘 EN CAS DE PROBLÈME

### Problème: "ERR_CONTENT_SECURITY_POLICY_VIOLATION"
```
Solution: Vérifier la CSP dans CONFIG_SECURITE_SERVEUR.md
         Ajouter les domaines manquants à la whitelist
```

### Problème: "Mixed Content Blocked"
```
Solution: Convertir tous les http:// en https://
         Vérifier firebase-config.js
         Redéployer
```

### Problème: Certificat SSL invalide
```
Solution: Renouveler le certificat via Let's Encrypt
         Configurer auto-renewal
         Tester avec: https://www.ssllabs.com/
```

### Problème: Performances lentes après déploiement
```
Solution: Analyser avec: https://developers.google.com/speed
         Minifier CSS/JS
         Configurer un CDN
         Ajouter du caching
```

---

## 📞 CONTACTS D'URGENCE

- **Équipe Sécurité:** security@edenfamily.com
- **Support Technique:** support@edenfamily.com
- **Firebase Support:** https://firebase.google.com/support
- **SSL Labs:** https://www.ssllabs.com/

---

**Dernière mise à jour: 25 Janvier 2026**
**Statut: Production Ready** ✅
