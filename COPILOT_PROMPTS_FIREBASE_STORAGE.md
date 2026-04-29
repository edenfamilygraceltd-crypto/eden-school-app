# 🤖 Prompts Copilot — Firebase Storage Upload System

## 📋 PROMPT 1 — Basique (Recommandé pour débuter)

```
I am building a school management web app using Firebase Storage and vanilla JavaScript.

Fix the following issues and implement a clean upload system:

1. Fix JavaScript error
The function uploadDocument is not defined
Ensure the function exists and is correctly bound to a button click event
Do NOT use inline onclick, use addEventListener

2. Implement Firebase Storage upload
Use Firebase v10 modular SDK
Upload selected file from <input type="file" id="fileInput">
Store files under: assignments/

3. After upload
Log success message
Return the download URL of the uploaded file using getDownloadURL
Display the file URL in console

4. Firebase rules assumption
Assume storage rules allow read/write for testing

5. HTML structure

Use:

file input
upload button with id uploadBtn

6. Requirements
Clean, beginner-friendly code
No inline scripts
Fully working example
🚀 🔥
```

---

## 🔥 PROMPT 2 — Avancé (Recommandé pour production)

```
Extend the system:

Detect file type (image, PDF, video)
Upload to different folders:
images → assignments/images/
pdf → assignments/docs/
video → assignments/videos/
Return download URL
Display preview:
image → <img>
pdf → <iframe>
video → <video>
Store metadata in Firebase Realtime Database:
fileName
fileURL
fileType
timestamp
```

---

## 🎯 Comment utiliser ces prompts

### Avec GitHub Copilot Chat
1. Ouvre VS Code
2. Copilot Chat (Ctrl + Shift + I)
3. Copie l'un des prompts ci-dessus
4. Colle-le dans la conversation
5. Copilot génère le code

### Avec ChatGPT/Claude
1. Va sur https://chat.openai.com ou ton service IA
2. Copie l'un des prompts
3. Colle et demande le code
4. Récupère la solution

### Avec Copilot Web
1. https://copilot.microsoft.com
2. Copie le prompt
3. Suis les étapes

---

## 📝 Notes importantes

**Prompt 1 (Basique)** :
- ✅ Pour débuter
- ✅ Fonction simple `uploadDocument()`
- ✅ Upload vers un seul dossier
- ✅ Retourne URL de téléchargement

**Prompt 2 (Avancé)** :
- ✅ Pour cas d'usage complexe
- ✅ Détection automatique du type
- ✅ Dossiers organisés par type
- ✅ Aperçu multimédia
- ✅ Métadonnées Firebase Database

---

## 🔐 Firebase Configuration attendue

Avant d'utiliser ces prompts, assure-toi d'avoir :

```javascript
// firebase-config.js
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.0.0/firebase-app.js';
import { getStorage } from 'https://www.gstatic.com/firebasejs/10.0.0/firebase-storage.js';
import { getDatabase } from 'https://www.gstatic.com/firebasejs/10.0.0/firebase-database.js';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_AUTH_DOMAIN",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_STORAGE_BUCKET",
  databaseURL: "YOUR_DATABASE_URL",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

export const app = initializeApp(firebaseConfig);
export const storage = getStorage(app);
export const db = getDatabase(app);
```

---

## ✨ Résultat attendu

Après avoir utilisé Prompt 1, tu auras :
- ✅ Fonction `uploadDocument()` workante
- ✅ Upload vers Firebase Storage
- ✅ URL de téléchargement retournée
- ✅ Logs de succès

Après avoir utilisé Prompt 2, tu auras en plus :
- ✅ Détection type fichier
- ✅ Organisation dossiers intelligente
- ✅ Prévisualisations multimédia
- ✅ Métadonnées dans Database

---

**📌 Copie exactement le contenu du prompt (pas de modifications) pour les meilleurs résultats !**
