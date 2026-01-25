# Guide de Test - Authentification Auth.html

## 🚀 Étapes de Test

### 1. Initialiser les utilisateurs de test
1. Ouvrez `Auth.html` dans votre navigateur
2. Cliquez sur le bouton **"Initialiser utilisateurs de test"** (bouton gris)
3. Attendez le message de succès

### 2. Vérifier la console du navigateur
Pour voir les logs de debug:
1. Appuyez sur **F12** pour ouvrir les outils de développement
2. Allez dans l'onglet **"Console"**
3. Vous devriez voir les messages:
   - `✅ Firebase initialisé avec succès`
   - `📋 État Firebase au chargement: auth: ✅ Défini, realtimeDB: ✅ Défini`

### 3. Tester chaque utilisateur

#### Directeur
- **Email:** `director@edenschool.edu`
- **Mot de passe:** `director123`
- **Redirection attendue:** `director.html`

#### Secrétaire
- **Email:** `secretary@edenschool.edu`
- **Mot de passe:** `secretary123`
- **Redirection attendue:** `secretary.html`

#### Comptable
- **Email:** `accountant@edenschool.edu`
- **Mot de passe:** `accountant123`
- **Redirection attendue:** `comptable.html`

#### Enseignant
- **Email:** `teacher@edenschool.edu`
- **Mot de passe:** `teacher123`
- **Redirection attendue:** `teacher_clean.html`

### 4. Observer les logs de connexion
Lors de chaque tentative de connexion, vous devriez voir dans la console:

```
🔐 Tentative de connexion pour: user@edenschool.edu
✅ Authentification réussie, vérification du rôle...
🔍 Vérification du rôle pour: [UID] user@edenschool.edu
👤 Directeur: true/false
📊 Données directeur: {...}
✅ Directeur authentifié, redirection...
```

## 🔍 Diagnostic en Cas de Problème

### Erreur: "Erreur technique: auth/invalid-api-key"
**Cause:** Firebase n'est pas initialisé correctement
**Solution:** 
1. Vérifiez que Firebase SDK est chargé (Console → onglet Network)
2. Vérifiez que la clé API Firebase est correcte dans Auth.html (lignes 652-668)

### Erreur: "Accès refusé. Vos identifiants ne sont pas reconnus"
**Cause:** L'utilisateur n'existe pas dans la Realtime Database ou n'a pas de rôle "staff"
**Solution:**
1. Vérifiez Firebase Console → Realtime Database
2. Les données doivent être sous `/directors/`, `/secretaries/`, ou `/teachers/`
3. Chaque utilisateur doit avoir un champ `role` avec la valeur correcte

### Erreur: "Email non trouvé" ou "Mot de passe incorrect"
**Cause:** Les identifiants saisis sont incorrects
**Solution:**
1. Copiez-collez exactement les identifiants du tableau ci-dessus
2. Assurez-vous que le clavier est en AZERTY (ou ajustez les accents)

### Pas de redirection après connexion réussie
**Cause:** Le fichier de redirection n'existe pas ou il y a une erreur JavaScript
**Solution:**
1. Vérifiez que `director.html`, `secretary.html`, `comptable.html`, et `teacher_clean.html` existent
2. Vérifiez la console pour les erreurs JavaScript

## 📊 Structure Firebase Realtime Database Requise

```
{
  "directors": {
    "director-001": {
      "uid": "director-001",
      "email": "director@edenschool.edu",
      "name": "Directeur Test",
      "role": "director",
      "createdAt": "2024-01-01T00:00:00.000Z"
    }
  },
  "secretaries": {
    "secretary-001": {
      "uid": "secretary-001",
      "email": "secretary@edenschool.edu",
      "name": "Secrétaire Test",
      "role": "secretary",
      "createdAt": "2024-01-01T00:00:00.000Z"
    },
    "accountant-001": {
      "uid": "accountant-001",
      "email": "accountant@edenschool.edu",
      "name": "Comptable Test",
      "role": "accountant",
      "createdAt": "2024-01-01T00:00:00.000Z"
    }
  },
  "teachers": {
    "teacher-001": {
      "uid": "teacher-001",
      "email": "teacher@edenschool.edu",
      "name": "Enseignant Test",
      "role": "teacher",
      "createdAt": "2024-01-01T00:00:00.000Z"
    }
  }
}
```

## ✅ Vérifications Pré-Test

- [ ] Auth.html existe et est accessible
- [ ] Firebase est configuré dans le projet
- [ ] Les fichiers de redirection existent (director.html, secretary.html, comptable.html, teacher_clean.html)
- [ ] Aucune erreur CORS dans la console
- [ ] Firebase Realtime Database est activée et accessible

## 🔐 Notes de Sécurité

⚠️ **ATTENTION:** Cette page de test utilise des identifiants de test en clair. Pour la production:
1. Supprimez le bouton "Initialiser utilisateurs de test"
2. Utilisez des variables d'environnement pour les identifiants
3. Authentifiez les utilisateurs via un système d'administration sécurisé
4. Activez les règles Firebase Realtime Database appropriées
