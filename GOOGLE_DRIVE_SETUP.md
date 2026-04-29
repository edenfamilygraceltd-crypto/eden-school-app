# 📚 Système Upload Google Drive — Configuration

## 🎯 Fonctionnalités

✅ **Upload de documents** sur Google Drive depuis `secretary.html`  
✅ **Drag & drop** avec aperçu en temps réel  
✅ **Barre de progression XHR** réelle  
✅ **Métadonnées stockées** dans Firebase Realtime Database  
✅ **Liste colorée** des documents avec filtres  
✅ **Téléchargement et aperçu** directs depuis Drive  
✅ **Suppression** depuis Drive et Firebase  

---

## 📋 Prérequis

1. **Node.js** (v18+) installé sur ta machine
2. **npm** ou **yarn**
3. **Compte Google Cloud** avec accès à Google Drive API
4. **Fichier `drive-key.json`** (Service Account credentials)
5. **Dossiers Google Drive** créés pour chaque type de document

---

## 🔧 Étape 1 : Obtenir les credentials Google Drive

### 1.1 Créer un projet Google Cloud

1. Va sur [Google Cloud Console](https://console.cloud.google.com/)
2. Crée un nouveau projet : `Eden Family School`
3. Active l'API Google Drive :
   - Vas à "APIs & Services" → "Library"
   - Cherche "Google Drive API"
   - Clique sur "Enable"

### 1.2 Créer une Service Account

1. Va à "APIs & Services" → "Credentials"
2. Clique sur "Create Credentials" → "Service Account"
3. Remplis les infos :
   - Service Account name: `eden-drive-service`
   - Description: "Pour upload académique Eden School"
4. Clique "Create and Continue"

### 1.3 Créer une clé JSON

1. Dans la page de la Service Account, va à l'onglet "Keys"
2. Clique "Add Key" → "Create new key"
3. Choisis **JSON** comme format
4. Clique "Create"
5. Le fichier `eden-drive-service-xxxxx.json` se télécharge

### 1.4 Renommer et placer le fichier

1. Renomme le fichier en `drive-key.json`
2. Place-le à la **racine du projet** (même dossier que `server.js`)

### 1.5 Donne les permissions à la Service Account

1. Note l'email de la Service Account (format : `xxx@xxx.iam.gserviceaccount.com`)
2. Partage les dossiers Google Drive avec cet email

---

## 📁 Étape 2 : Créer les dossiers Google Drive

Dans Google Drive, crée ces dossiers (ou utilise les tiens) :

- 📝 **Devoirs**
- 📋 **Tests**
- 📄 **Examens**
- 🔨 **Travaux**
- 🏖️ **Vacances**
- 📷 **Photos**
- 🎥 **Vidéos**

Pour chaque dossier :

1. Clique droit → "Share"
2. Ajoute l'email de la Service Account (avec permissions "Editor")
3. Copie l'ID du dossier depuis l'URL : `https://drive.google.com/drive/folders/[FOLDER_ID]`

---

## 🔑 Étape 3 : Configurer les IDs des dossiers

Ouvre `server.js` et remplace les IDs dans l'objet `DRIVE_FOLDERS` :

```javascript
const DRIVE_FOLDERS = {
  examen:   "TON_ID_EXAMEN",       // 📄
  devoir:   "TON_ID_DEVOIR",       // 📝
  photo:    "TON_ID_PHOTO",        // 📷
  test:     "TON_ID_TEST",         // 📋
  travaux:  "TON_ID_TRAVAUX",      // 🔨
  video:    "TON_ID_VIDEO",        // 🎥
  vacances: "TON_ID_VACANCES"      // 🏖️
};
```

---

## 📦 Étape 4 : Installer les dépendances

```bash
# Dans le dossier du projet
npm install

# Ou si tu utilises yarn
yarn install
```

Les dépendances nécessaires sont déjà dans `package.json` :
- `express` : serveur web
- `multer` : gestion des uploads
- `googleapis` : API Google Drive
- `cors` : partage cross-origin
- `firebase-admin` : Firebase Backend
- `nodemon` (dev) : rechargement automatique

---

## ▶️ Étape 5 : Lancer le serveur

### En développement (avec rechargement automatique)

```bash
npm run dev
```

Ou directement :

```bash
nodemon server.js
```

### En production

```bash
npm start
```

Le serveur démarre sur : **http://localhost:3000**

Console attendue :
```
✅ Backend Node.js démarré sur le port 3000
```

---

## 🎨 Étape 6 : Accéder au portail

1. Ouvre le portail secrétaire : `secretary.html` (via ton serveur)
2. Va à la section **"Gestion Académique"** → onglet **"📚 Documents Drive"**
3. Sélectionne un document et clique "Uploader sur Google Drive"

---

## 🧪 Tests

### Test 1 : Upload simple

```javascript
// Dans la console du navigateur
fetch('http://localhost:3000/upload', {
  method: 'POST',
  body: new FormData(document.querySelector('form'))
})
.then(r => r.json())
.then(console.log)
```

### Test 2 : Lister les fichiers

```javascript
fetch('http://localhost:3000/files/devoir')
  .then(r => r.json())
  .then(console.log)
```

---

## 🔧 Dépannage

### "❌ Erreur upload: ENOENT: no such file or directory"

**Problème** : Le dossier `uploads/` n'existe pas  
**Solution** : 

```bash
mkdir uploads
```

### "❌ Erreur upload: Cannot read property 'fileSize' of undefined"

**Problème** : `multer` n'est pas configuré correctement  
**Solution** : Vérifiez que `multer` est importé correctement dans `server.js`

### "❌ 403 Forbidden: The user does not have sufficient permissions"

**Problème** : La Service Account n'a pas accès aux dossiers Drive  
**Solution** : 

1. Partage chaque dossier Drive avec l'email de la Service Account
2. Accorde les permissions "Editor"
3. Attends quelques minutes pour la propagation

### "❌ Error: Service account key not found"

**Problème** : `drive-key.json` est absent ou mal placé  
**Solution** : Vérifie que `drive-key.json` est à la racine du projet

---

## 📊 Structure des données Firebase

Les documents uploadés sont sauvegardés dans Firebase :

```json
{
  "academic_documents": {
    "auto_id_1": {
      "title": "Devoir Maths N3A T1",
      "type": "devoir",
      "classe": "N3A",
      "matiere": "mathematiques",
      "trimestre": "T1",
      "fileId": "drive_file_id",
      "fileName": "devoir_N3A_mathematiques_T1_1234567890.pdf",
      "directUrl": "https://drive.google.com/uc?export=view&id=...",
      "previewUrl": "https://drive.google.com/file/d/.../preview",
      "downloadUrl": "https://drive.google.com/uc?export=download&id=...",
      "driveLink": "https://drive.google.com/file/d/.../view",
      "mimeType": "application/pdf",
      "size": 2048576,
      "uploadedBy": "secretaire@school.com",
      "uploadedAt": 1704067200000
    }
  }
}
```

---

## 📝 Routes API disponibles

| Méthode | Route | Description |
|---------|-------|-------------|
| POST | `/upload` | Uploader un fichier vers Google Drive |
| GET | `/files/:type` | Lister les fichiers d'un type |
| DELETE | `/delete/:fileId` | Supprimer un fichier de Drive |

---

## 🚀 Prochaines étapes

1. ✅ Configure les dossiers Google Drive
2. ✅ Obtiens les credentials
3. ✅ Lance le serveur
4. ✅ Teste les uploads
5. ✅ Optimise les filtres et la recherche

---

## 📞 Support

Si tu rencontres des erreurs :

1. **Vérifie les logs du serveur** : Cherche les messages `📤` et `✅`
2. **Vérifie la console du navigateur** : F12 → Console
3. **Teste la connexion au serveur** : `curl http://localhost:3000/api/health`

---

**Happy uploading! 🎉**
