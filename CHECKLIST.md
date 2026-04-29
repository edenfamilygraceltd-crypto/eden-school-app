# 📋 CHECKLIST — Configuration avant lancement

## 🔴 OBLIGATOIRE pour que ça fonctionne

### ✅ Étape 1 : Google Cloud Setup (10 min)

- [ ] Créer un projet sur [Google Cloud Console](https://console.cloud.google.com/)
- [ ] Activer Google Drive API
- [ ] Créer une Service Account
- [ ] Télécharger la clé JSON
- [ ] Renommer en `drive-key.json`
- [ ] Placer dans le dossier racine (à côté de `server.js`)

**Vérification :**
```bash
ls -la drive-key.json  # Doit exister
```

---

### ✅ Étape 2 : Google Drive Folders (5 min)

Crée ces 7 dossiers dans Google Drive :
- [ ] 📝 **Devoirs**
- [ ] 📋 **Tests**
- [ ] 📄 **Examens**
- [ ] 🔨 **Travaux**
- [ ] 🏖️ **Vacances**
- [ ] 📷 **Photos**
- [ ] 🎥 **Vidéos**

Pour **chaque dossier** :
- [ ] Clique droit → "Share"
- [ ] Ajoute l'email Service Account
- [ ] Sélectionne "Editor"
- [ ] Copie l'ID du dossier (depuis l'URL)

**Exemple d'URL :**
```
https://drive.google.com/drive/folders/1HLflt6qTu_UDtbTLjQU6UNHNS0MEHbA0
                                       ↑ C'est cet ID qu'il faut copier
```

---

### ✅ Étape 3 : Mettre à jour server.js (2 min)

Ouvre `server.js` et remplace les IDs dans `DRIVE_FOLDERS` :

```javascript
const DRIVE_FOLDERS = {
  examen:   "ID_DE_TON_DOSSIER_EXAMENS",
  devoir:   "ID_DE_TON_DOSSIER_DEVOIRS",
  photo:    "ID_DE_TON_DOSSIER_PHOTOS",
  test:     "ID_DE_TON_DOSSIER_TESTS",
  travaux:  "ID_DE_TON_DOSSIER_TRAVAUX",
  video:    "ID_DE_TON_DOSSIER_VIDEOS",
  vacances: "ID_DE_TON_DOSSIER_VACANCES"
};
```

**Exemple complet :**
```javascript
const DRIVE_FOLDERS = {
  examen:   "1HLflt6qTu_UDtbTLjQU6UNHNS0MEHbA0",
  devoir:   "19Z-HL0ttxFCoIJeSwp1MxYuzzGe5Ipd-",
  photo:    "1T17ES75mQyAWrzCB8KZuZQDAPU3hKSCw",
  test:     "1k_jBwSr0nASiQ27ow6Tib7Ukug24xIO0",
  travaux:  "1pybPOwSy7-Gp4Kx-sWfm5x5dBKR_ekIV",
  video:    "1upIgjidP2I9yA2N4BqxB26JkUqIu60O2",
  vacances: "1HLflt6qTu_UDtbTLjQU6UNHNS0MEHbA0"
};
```

---

### ✅ Étape 4 : Créer le dossier uploads/ (30 sec)

```bash
mkdir uploads
```

---

### ✅ Étape 5 : Installer les dépendances (1 min)

```bash
npm install
```

---

## 🟢 PRÊT À LANCER

### Lancer le serveur

**Mode développement :**
```bash
npm run dev
```

Ou directement :
```bash
nodemon server.js
```

**Mode production :**
```bash
npm start
```

---

### Attendre ce message ✅

```
✅ Backend Node.js démarré sur le port 3000
```

Puis ouvre le navigateur : **http://localhost:3000**

---

## 🧪 Test rapide

### 1. Teste la page
```
Portail Secrétaire → Gestion Académique → Documents Drive
```

### 2. Upload un fichier
- Sélectionne le type
- Glisse un fichier
- Clique "Uploader"

### 3. Vérifie les résultats
- ✅ Message "Document uploadé"
- ✅ Fichier visible dans Google Drive
- ✅ Fichier dans la liste "Documents publiés"
- ✅ Métadonnées dans Firebase

---

## ⚠️ Si ça ne marche pas

| Erreur | Solution |
|--------|----------|
| `ENOENT: uploads/` | `mkdir uploads` |
| `drive-key.json not found` | Place le fichier à la racine |
| `403 Forbidden` | Partage les dossiers avec Service Account |
| `Cannot connect localhost:3000` | Lance le serveur d'abord |
| `FileId is undefined` | Vérifiez les IDs dans DRIVE_FOLDERS |

Consulte **GOOGLE_DRIVE_SETUP.md** pour le dépannage complet.

---

## 📞 Resumé des fichiers importants

| Fichier | Rôle |
|---------|------|
| `secretary.html` | UI + JavaScript frontend |
| `server.js` | API backend + routes Google Drive |
| `drive-key.json` | Credentials Service Account |
| `GOOGLE_DRIVE_SETUP.md` | Guide détaillé |
| `LAUNCH_SERVER.sh` | Script de lancement |
| `CHECKLIST.md` | Ce fichier |

---

**Une fois configuré, tu peux :**
- ✅ Uploader depuis secretary.html
- ✅ Partager les docs avec les élèves
- ✅ Télécharger et supprimer les docs
- ✅ Filtrer par type, classe, etc.

**Bonne chance ! 🚀**
