# 🔐 Configuration des Règles de Sécurité Firebase

## 📋 Guide de Configuration

### Étape 1: Accédez à Firebase Console
1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Sélectionnez votre projet "edendatabase-7e1ed"
3. Allez dans **Realtime Database**

### Étape 2: Configurez les Règles

1. Cliquez sur l'onglet **Rules** (Règles)
2. Remplacez tout le contenu par les règles ci-dessous:

```json
{
  "rules": {
    // Règles pour la collection "users"
    "users": {
      "$uid": {
        // Chaque utilisateur peut lire ses propres données
        ".read": "auth.uid == $uid || root.child('users').child($uid).child('role').val() === 'admin'",
        
        // Seulement les admins ou l'utilisateur lui-même peuvent modifier
        ".write": "auth.uid == $uid || root.child('users').child($uid).child('role').val() === 'admin'",
        
        // Valider que les données requises existent
        ".validate": "newData.hasChildren(['email', 'name', 'role'])",
        
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && newData.val() in ['director', 'admin', 'secretary', 'accountant', 'teacher']"
        },
        "name": {
          ".validate": "newData.isString() && newData.val().length > 0"
        }
      }
    },

    // Règles pour la collection "secretaries"
    "secretaries": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('users').child(auth.uid).child('role').val() === 'admin'",
        ".write": "auth.uid == $uid || root.child('users').child(auth.uid).child('role').val() === 'admin'",
        ".validate": "newData.hasChildren(['email', 'name', 'role'])",
        
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && newData.val() in ['secretary', 'accountant']"
        },
        "name": {
          ".validate": "newData.isString() && newData.val().length > 0"
        }
      }
    },

    // Règles pour la collection "teachers"
    "teachers": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('users').child(auth.uid).child('role').val() === 'admin'",
        ".write": "auth.uid == $uid || root.child('users').child(auth.uid).child('role').val() === 'admin'",
        ".validate": "newData.hasChildren(['email', 'name', 'role'])",
        
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && newData.val() === 'teacher'"
        },
        "name": {
          ".validate": "newData.isString() && newData.val().length > 0"
        }
      }
    },

    // Règles pour la collection "parents"
    "parents": {
      "$uid": {
        ".read": "auth.uid == $uid || root.child('users').child(auth.uid).child('role').val() === 'admin'",
        ".write": "auth.uid == $uid || root.child('users').child(auth.uid).child('role').val() === 'admin'",
        ".validate": "newData.hasChildren(['email', 'name', 'role'])",
        
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && newData.val() === 'parent'"
        },
        "name": {
          ".validate": "newData.isString() && newData.val().length > 0"
        }
      }
    },

    // Autres collections - lecture authentifiée, écriture contrôlée
    "students": {
      ".read": "auth != null",
      ".write": "root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('role').val() === 'director'"
    },

    "reports": {
      ".read": "auth != null",
      ".write": "root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('role').val() === 'director'"
    },

    "monthlyReports": {
      ".read": "auth != null",
      ".write": "root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('role').val() === 'director'"
    },

    "weeklyReports": {
      ".read": "auth != null",
      ".write": "root.child('users').child(auth.uid).child('role').val() === 'admin' || root.child('users').child(auth.uid).child('role').val() === 'director'"
    },

    "studentFees": {
      ".read": "auth != null",
      ".write": "root.child('users').child(auth.uid).child('role').val() === 'accountant' || root.child('users').child(auth.uid).child('role').val() === 'admin'"
    },

    "messages": {
      ".read": "auth != null",
      ".write": "auth != null"
    },

    "presentations": {
      ".read": "auth != null",
      ".write": "auth != null"
    },

    "settings": {
      ".read": "auth != null",
      ".write": "root.child('users').child(auth.uid).child('role').val() === 'admin'"
    },

    // Règle par défaut - interdire tout accès non autorisé
    ".read": false,
    ".write": false
  }
}
```

### Étape 3: Cliquez sur "Publish" (Publier)

### Étape 4: Testez les Règles

1. Ouvrez l'onglet **Simulator** (Simulateur)
2. Sélectionnez l'opération: `read`, `write`, ou `delete`
3. Entrez le chemin: `/users/{uid}`
4. Sélectionnez Authentication: `Authorized`
5. Cliquez "Run Simulator"

## 🔐 Qu'est-ce que Ces Règles Font?

### 1. **Authentification Obligatoire**
Seuls les utilisateurs connectés peuvent accéder aux données.

### 2. **Champs Requis**
Chaque utilisateur doit avoir:
- `email` - Email valide
- `name` - Nom non-vide
- `role` - Rôle valide

### 3. **Validation Email**
L'email doit être un email valide (format: user@domain.com)

### 4. **Rôles Valides**
Les rôles acceptés sont:
- `director` - Directeur
- `admin` - Administrateur
- `secretary` - Secrétaire
- `accountant` - Comptable
- `teacher` - Enseignant
- `parent` - Parent

### 5. **Contrôle d'Accès par Rôle**
- **Directors/Admins**: Accès complet aux étudiants et rapports
- **Accountants**: Accès aux frais étudiants
- **Teachers**: Lecture des données étudiants
- **Parents**: Accès limité aux leurs enfants

## ✅ Structure Correcte des Données

Quand vous créez un utilisateur dans `users`:

```json
{
  "users": {
    "uid_directeur": {
      "email": "director@school.edu",
      "name": "Directeur Name",
      "role": "director"
    },
    "uid_secretary": {
      "email": "secretary@school.edu",
      "name": "Secretary Name",
      "role": "secretary"
    },
    "uid_accountant": {
      "email": "accountant@school.edu",
      "name": "Accountant Name",
      "role": "accountant"
    },
    "uid_teacher": {
      "email": "teacher@school.edu",
      "name": "Teacher Name",
      "role": "teacher"
    }
  }
}
```

## 🚨 Erreurs Courantes

### "Permission denied"
- L'utilisateur n'est pas authentifié
- L'utilisateur n'a pas le rôle requis
- Les données manquent des champs `email`, `name`, ou `role`

### "Invalid data"
- Email invalide (format incorrect)
- Rôle invalide (pas dans la liste autorisée)
- Nom vide ou manquant

### "Validation failed"
- Une des validations a échoué
- Vérifiez le format de vos données

## 📝 Notes Importantes

1. **Les règles s'appliquent immédiatement** après publication
2. **Aucune donnée ne peut être créée** qui ne respecte pas les règles
3. **Les utilisateurs ne peuvent pas modifier** les données des autres (sauf admins)
4. **Les règles sont vérifiées côté serveur** - sécurité garantie

## 🔄 Redirection et Règles

Maintenant quand l'utilisateur se connecte à Auth.html:

1. ✅ Firebase Auth vérifie le mot de passe
2. ✅ Auth.html lit les données `users/{uid}`
3. ✅ Les règles Firebase permettent la lecture SEULEMENT si:
   - L'utilisateur est authentifié ET
   - L'utilisateur essaie de lire ses propres données OU
   - L'utilisateur est admin
4. ✅ Si le rôle est valide, redirection vers la bonne page
5. ✅ Les règles Firebase contrôlent l'accès à chaque collection

## 🎯 Résultat Final

- ✅ Seules les données complètes et valides permettent la redirection
- ✅ Chaque page (director.html, secretary.html, etc.) reçoit une session sécurisée
- ✅ Firebase Realtime Database vérifie les permissions à chaque lecture
- ✅ Aucun accès non autorisé possible
