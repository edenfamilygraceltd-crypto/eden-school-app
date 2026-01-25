# Diagnostic Firebase - Auth.html

## 🔧 Vérifications Automatiques à Faire

### 1. Ouvrir la Console JavaScript
```
F12 → Console Tab
```

### 2. Copier-Coller dans la Console

Exécutez les commandes suivantes pour vérifier l'état de Firebase:

```javascript
// Vérifier si Firebase est chargé
console.log('Firebase version:', firebase.SDK_VERSION);
console.log('Auth loaded:', typeof firebase.auth !== 'undefined');
console.log('Database loaded:', typeof firebase.database !== 'undefined');

// Vérifier la configuration
console.log('Config exists:', typeof firebaseConfig !== 'undefined');
console.log('Config:', firebaseConfig);

// Vérifier les instances Firebase
console.log('auth instance:', auth);
console.log('realtimeDB instance:', realtimeDB);

// Tester la connexion à la base de données
realtimeDB.ref('test').once('value')
  .then(snapshot => {
    console.log('✅ Connexion DB réussie');
    console.log('Data:', snapshot.val());
  })
  .catch(err => {
    console.error('❌ Erreur connexion DB:', err.message);
  });
```

### 3. Vérifier l'Utilisateur Actuel
```javascript
// Voir l'utilisateur actuellement authentifié
console.log('Current user:', auth.currentUser);

// Voir les utilisateurs enregistrés
auth.listUsers(100)
  .then(result => {
    console.log('Utilisateurs:', result.users.map(u => ({ email: u.email, uid: u.uid })));
  })
  .catch(err => {
    console.error('Erreur listing users:', err.message);
  });
```

### 4. Vérifier les Données dans Realtime Database
```javascript
// Vérifier les directeurs
realtimeDB.ref('directors').once('value')
  .then(snapshot => {
    console.log('Directors:', snapshot.val());
  });

// Vérifier les secrétaires
realtimeDB.ref('secretaries').once('value')
  .then(snapshot => {
    console.log('Secretaries:', snapshot.val());
  });

// Vérifier les enseignants
realtimeDB.ref('teachers').once('value')
  .then(snapshot => {
    console.log('Teachers:', snapshot.val());
  });
```

## 📋 Checklist de Diagnostic

- [ ] Firebase SDK est chargé (Firebase version s'affiche)
- [ ] `firebaseConfig` est défini et contient 8 propriétés
- [ ] `auth` instance existe et n'est pas undefined
- [ ] `realtimeDB` instance existe et n'est pas undefined
- [ ] Connexion DB réussie (message ✅)
- [ ] Au moins un utilisateur existe dans les données
- [ ] Les utilisateurs ont un champ `role` approprié

## 🐛 Erreurs Courantes et Solutions

### "firebase is not defined"
- **Cause:** Firebase SDK n'est pas chargé avant le script Auth.html
- **Solution:** Vérifier les `<script>` tags dans Auth.html (lignes 11-14)

### "auth/invalid-api-key"
- **Cause:** Clé API invalide ou incorrecte
- **Solution:** Vérifier la clé API dans Firebase Console → Project Settings

### "auth/permission-denied"
- **Cause:** Les règles Firebase Realtime Database refusent l'accès
- **Solution:** Vérifier et modifier les règles dans Firebase Console → Database → Rules

### "User not found"
- **Cause:** L'utilisateur n'existe pas dans Firebase Auth
- **Solution:** Créer l'utilisateur via Firebase Console ou le bouton "Initialiser utilisateurs de test"

### "❌ Aucun rôle staff trouvé"
- **Cause:** L'utilisateur existe dans Auth mais pas dans les collections directors/secretaries/teachers
- **Solution:** Vérifier la structure de la Realtime Database

## 🔍 Logs Détaillés à Observer

### Lors du chargement de Auth.html:
```
✅ Firebase initialisé avec succès
📋 État Firebase au chargement:
  - auth: ✅ Défini
  - realtimeDB: ✅ Défini
  - firebaseConfig: ✅ Défini
```

### Lors d'une tentative de connexion réussie:
```
🔐 Tentative de connexion pour: email@example.com
✅ Authentification réussie, vérification du rôle...
🔍 Vérification du rôle pour: [UID] email@example.com
👤 Directeur: true
📊 Données directeur: {uid, email, name, role, createdAt}
✅ Directeur authentifié, redirection...
```

### Lors d'une tentative de connexion échouée:
```
🔐 Tentative de connexion pour: email@example.com
❌ Erreur de connexion: [CODE] [MESSAGE]
```

## 📊 Structure de Données Attendue

Chaque utilisateur dans la Realtime Database doit avoir:

```javascript
{
  "uid": "unique-identifier",
  "email": "user@edenschool.edu",
  "name": "User Full Name",
  "role": "director|secretary|accountant|teacher",
  "createdAt": "2024-01-01T00:00:00.000Z"
}
```

## 🔐 Vérification des Règles Firebase

Les règles doivent permettre la lecture des chemins publics:

```json
{
  "rules": {
    "directors": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('admins').child(auth.uid).exists()",
        ".write": "auth.uid == $uid || root.child('admins').child(auth.uid).exists()"
      }
    },
    "secretaries": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('admins').child(auth.uid).exists()",
        ".write": "auth.uid == $uid || root.child('admins').child(auth.uid).exists()"
      }
    },
    "teachers": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('admins').child(auth.uid).exists()",
        ".write": "auth.uid == $uid || root.child('admins').child(auth.uid).exists()"
      }
    }
  }
}
```

## 🔧 Outils Firebase Console

### Pour vérifier les utilisateurs:
1. Firebase Console → Authentication → Users
2. Vérifiez que les utilisateurs de test existent

### Pour vérifier la Realtime Database:
1. Firebase Console → Realtime Database → Data
2. Vérifiez la structure: `directors/`, `secretaries/`, `teachers/`

### Pour vérifier les règles:
1. Firebase Console → Realtime Database → Rules
2. Vérifiez que `.read` et `.write` sont configurés correctement

## 📱 Test Mobile/Responsive

Si vous testez sur mobile, assurez-vous que:
- [ ] Les touches "F12" ou version équivalente fonctionnent pour la console
- [ ] Vous pouvez voir les logs via le navigateur (Chrome Mobile → DevTools)
- [ ] La connexion internet est stable

## ⚡ Performance

Si la connexion est lente:
- Vérifiez la latence réseau (Console → Network tab)
- Vérifiez que Firebase Realtime Database est dans la bonne région
- Vérifiez la bande passante disponible
