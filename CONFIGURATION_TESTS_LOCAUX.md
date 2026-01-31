# 🚀 Configuration Locale - Tests de Création de Compte

## 📋 Configuration Requise

### 1. Vérifier que vous avez accès à:
- ✅ Navigateur moderne (Chrome, Firefox, Edge)
- ✅ Accès à Firebase Console
- ✅ Accès à EmailJS Dashboard
- ✅ Un serveur local ou accès direct aux fichiers HTML

## 🔧 Configuration EmailJS

### Vérifier les Credentials

1. **Ouvrez:** [EmailJS Dashboard](https://dashboard.emailjs.com)
2. **Vérifiez:**
   - Service ID: `service_yvl11d5` ✅
   - Template ID: `template_hjw7vbj` ✅
   - Public Key: `Un7snKzeE4AGeorc-` ✅

### Ajouter une Adresse Email de Test

1. **Allez à:** "Email Templates"
2. **Créez un nouveau template** ou utilisez `template_hjw7vbj`
3. **Paramètres:**
   ```
   To Email: {{to_email}} ou {{email}} ou {{user_email}}
   Subject: Code de vérification OTP
   Body:
   Votre code OTP: {{otp}}
   Veuillez entrer ce code pour vérifier votre email.
   ```

## 🔐 Configuration Firebase

### Vérifier les Credentials

**Fichier:** IT.html (lignes 10-30)
```javascript
const firebaseConfig = {
    apiKey: "AIzaSyApUFNELOfgIe7rWEek9GLS9EIphNW09-A",
    authDomain: "edensmart-app.firebaseapp.com",
    projectId: "edensmart-app",
    storageBucket: "edensmart-app.firebasestorage.app",
    messagingSenderId: "1093120876724",
    appId: "1:1093120876724:web:bc37448cadd18d651c77e1",
    measurementId: "G-1FL70PZZSW"
};
```

### Vérifier les Collections Firestore

1. **Ouvrez:** [Firebase Console](https://console.firebase.google.com)
2. **Sélectionnez:** Projet `edensmart-app`
3. **Onglet:** Firestore Database
4. **Vérifiez les collections:**
   - [ ] `account_creation_settings`
   - [ ] `account_requests`
   - [ ] `users`

## 🧪 Tests Locaux

### Test 1: Vérifier EmailJS

**Fichier:** `email-test.html`

```bash
# Ouvrez dans le navigateur
http://localhost:PORT/email-test.html
# ou simplement cliquez sur email-test.html
```

**Étapes:**
1. Entrez votre email
2. Générez un OTP
3. Cliquez "Envoyer Email de Test"
4. Vérifiez votre boîte mail (+ spam)

**Logs attendus:**
```
[10:00:00] ✅ EmailJS détecté
[10:00:01] EmailJS initialisé avec la clé publique
[10:00:02] Paramètres: {...}
[10:00:03] ✅ Email envoyé avec succès!
```

### Test 2: Créer le Compte Primary Admin

**Fichier:** `IT.html`

```bash
1. Ouvrez http://localhost:PORT/IT.html
2. Onglet "Créer Comptes"
3. Remplissez:
   - Nom: Test Admin
   - Email: test@example.com
   - Mot de passe: 123456
   - Rôle: Directeur
4. Cliquez "Créer Comptes"
```

**Console logs (F12) attendus:**
```
Vérification du primary admin...
Primary admin trouvé: null
Premier compte - création du Primary Admin
Utilisateur Firebase créé: uid_xxx...
Document utilisateur créé dans Firestore
Primary admin settings créés
✅ Message: Compte créé avec succès! Vous êtes maintenant l'administrateur principal.
```

**Vérifications Firestore:**
```
account_creation_settings/primary:
  adminId: uid_xxx...
  adminEmail: test@example.com

users/uid_xxx:
  nom: Test Admin
  email: test@example.com
  role: Directeur
  isPrimaryAdmin: true
```

### Test 3: Créer un Compte Secondaire

**Étapes:**
1. Ré-ouvrez IT.html (le compte est créé)
2. Onglet "Créer Comptes"
3. Remplissez avec un nouvel email:
   ```
   Nom: Nouveau Staff
   Email: staff@example.com
   Mot de passe: 654321
   Rôle: Teacher
   ```
4. Cliquez "Créer Comptes"

**Console logs attendus:**
```
Vérification du primary admin...
Primary admin trouvé: {adminId: "uid_xxx...", ...}
Compte secondaire - création d'une demande d'approbation
Création de la demande: {...}
Demande créée avec ID: request_id_xxx
✅ Message: Votre demande a été envoyée pour approbation
```

**Vérifications Firestore:**
```
account_requests/request_id_xxx:
  name: Nouveau Staff
  email: staff@example.com
  status: pending
  role: Teacher
  requestedBy: uid_xxx (Primary Admin)
```

### Test 4: Approuver une Demande

**Étapes:**
1. Restez sur IT.html (en tant que Primary Admin)
2. Section "Demandes en Attente"
3. Cherchez la demande pour "staff@example.com"
4. Cliquez "Approuver"

**Résultats attendus:**
```
account_requests/request_id_xxx:
  status: approved ← Changé de "pending"
  approvedAt: timestamp
  approvedBy: uid_xxx

users/new_uid:
  nom: Nouveau Staff
  email: staff@example.com
  status: active
  isPrimaryAdmin: false
```

## 📊 Vérification Complète

### Checklist de Déploiement

- [ ] EmailJS fonctionne (email-test.html)
- [ ] Credentials Firebase correctes
- [ ] Collections Firestore existent
- [ ] Premier compte (Primary Admin) créé
- [ ] Email OTP reçu
- [ ] Deuxième compte crée une demande
- [ ] Demande approuvée par Primary Admin
- [ ] Nouveau compte activé après approbation
- [ ] Redirection vers index.html#contact fonctionne

## 🔍 Debugging

### Problème: EmailJS ne répond pas

**Solution:**
```javascript
// Ouvrez la console (F12)
// Testez manuellement:
emailjs.init('Un7snKzeE4AGeorc-');
emailjs.send('service_yvl11d5', 'template_hjw7vbj', {
    to_email: 'test@example.com',
    otp: '123456'
}).then(res => console.log('Success:', res))
  .catch(err => console.log('Error:', err));
```

### Problème: Firestore permissions

**Solution:**
1. Ouvrez Firebase Console
2. Onglet "Firestore Database"
3. Onglet "Rules"
4. Assurez-vous que les permissions sont:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Problème: "Failed to fetch"

**Causes possibles:**
1. Serveur non accessible
2. CORS bloqué
3. URL incorrecte

**Solution:**
- Utilisez un serveur HTTP (pas file://)
- Ouvrez fichier directement si sur le même domaine

## 📱 Tests sur Différents Navigateurs

### Chrome/Chromium
✅ Support complet EmailJS  
✅ Support complet Firebase  
✅ Console fiable

### Firefox
✅ Support complet  
✅ Console fiable

### Safari
⚠️ Peut avoir des problèmes CORS  
✅ Sinon compatible

## 🎯 Prochaines Étapes

1. **Testez localement:**
   - Lancez email-test.html
   - Créez un compte Primary Admin
   - Vérifiez que tout fonctionne

2. **Déployez en production:**
   - Poussez le code à GitHub
   - Déployez sur Firebase Hosting ou autre

3. **Configurez les utilisateurs:**
   - Créez les premiers staffs
   - Approuvez les demandes
   - Activez les comptes

---

**Version:** 1.0  
**Date:** 31 Janvier 2026
