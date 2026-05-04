// Backend Node.js simple pour Eden Family School
// Utilise Firebase Admin SDK pour accéder à la même base de données que le front

const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const { google } = require('googleapis');

// Importer les credentials Firebase
const serviceAccount = require('./serviceAccountKey.json');


// Initialiser Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://edensmart-app-default-rtdb.firebaseio.com',
  storageBucket: 'edensmart-app.appspot.com' // Bucket de stockage
});

const db = admin.database(); // Realtime Database
const bucket = admin.storage().bucket(); // Firebase Storage

const app = express();
const PORT = process.env.PORT || 3000;
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const DRIVE_KEY_PATH = path.join(__dirname, 'drive-key.json');

// ── Auth Google Drive (initialisée une seule fois) ──
const driveAuth = new google.auth.GoogleAuth({
  keyFile: DRIVE_KEY_PATH,
  scopes: ['https://www.googleapis.com/auth/drive']
});
const drive = google.drive({ version: 'v3', auth: driveAuth });

if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

if (!fs.existsSync(DRIVE_KEY_PATH)) {
  console.warn(`⚠️ Fichier Google Drive introuvable: ${DRIVE_KEY_PATH}`);
}

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.static(path.join(__dirname)));

// Middleware pour gérer les erreurs
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).send('Quelque chose a mal tourné!');
});

// Endpoint pour enregistrer un bulletin
app.post('/api/bulletins', async (req, res) => {
  try {
    const { student, trimestre, notes, commentaire } = req.body;
    const bulletinRef = db.ref('bulletins').push();
    
    const bulletinData = {
      id: bulletinRef.key,
      studentId: student.code,
      studentName: student.nom,
      classe: student.classe,
      section: student.section,
      trimestre: parseInt(trimestre),
      notes,
      commentaire,
      date: admin.database.ServerValue.TIMESTAMP,
      statut: 'brouillon'
    };

    await bulletinRef.set(bulletinData);
    res.status(201).json({ success: true, id: bulletinRef.key, ...bulletinData });
  } catch (error) {
    console.error('Erreur lors de l\'enregistrement du bulletin:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// Endpoint pour télécharger un bulletin PDF
app.post('/api/bulletins/:id/pdf', async (req, res) => {
  try {
    const { id } = req.params;
    const { pdfData } = req.body;
    
    // Convertir le base64 en buffer
    const buffer = Buffer.from(pdfData.split(',')[1], 'base64');
    const fileName = `bulletins/${id}.pdf`;
    
    // Téléverser sur Firebase Storage
    const file = bucket.file(fileName);
    await file.save(buffer, {
      metadata: {
        contentType: 'application/pdf',
      },
      public: true
    });

    // Mettre à jour le statut du bulletin
    await db.ref(`bulletins/${id}`).update({
      pdfUrl: `https://storage.googleapis.com/${bucket.name}/${fileName}`,
      statut: 'finalisé',
      dateFinalisation: admin.database.ServerValue.TIMESTAMP
    });

    res.status(200).json({ 
      success: true, 
      pdfUrl: `https://storage.googleapis.com/${bucket.name}/${fileName}`
    });
  } catch (error) {
    console.error('Erreur lors du téléchargement du PDF:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// Endpoint pour récupérer les bulletins d'un élève
app.get('/api/eleves/:eleveId/bulletins', async (req, res) => {
  try {
    const { eleveId } = req.params;
    const snapshot = await db.ref('bulletins')
      .orderByChild('studentId')
      .equalTo(eleveId)
      .once('value');
    
    const bulletins = [];
    snapshot.forEach(child => {
      bulletins.push({
        id: child.key,
        ...child.val()
      });
    });
    
    res.status(200).json(bulletins);
  } catch (error) {
    console.error('Erreur lors de la récupération des bulletins:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// Exemple: route de santé
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Backend Node.js fonctionne.' });
});

// Exemple: stats simples en lisant la base (students / travailleurs)
app.get('/api/stats', async (req, res) => {
  try {
    const studentsSnap = await db.ref('students').once('value');
    const travailleursSnap = await db.ref('Travailleurs').once('value');

    res.json({
      totalStudents: studentsSnap.numChildren(),
      totalTravailleurs: travailleursSnap.numChildren()
    });
  } catch (err) {
    console.error('Erreur /api/stats:', err);
    res.status(500).json({ error: 'Erreur interne serveur' });
  }
});

// (Optionnel) servir les fichiers statiques du front si tu veux tout lancer via Node
app.use(express.static(path.join(__dirname)));

// Routes pour les pages principales
app.get('/teacher.html', (req, res) => {
  res.sendFile(path.join(__dirname, 'teacher_clean.html'));
});

app.get('/teacher', (req, res) => {
  res.sendFile(path.join(__dirname, 'teacher_clean.html'));
});

/* ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
   GOOGLE DRIVE — UPLOAD DOCUMENTS ACADÉMIQUES
   ════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════ */

// ── Multer : stockage temporaire local ──
const upload = multer({
  dest: UPLOADS_DIR,
  limits: { fileSize: 500 * 1024 * 1024 } // 500 MB max
});

// ── Dossiers Drive par type ──
const DRIVE_FOLDERS = {
  examen:   "1HLflt6qTu_UDtbTLjQU6UNHNS0MEHbA0",
  devoir:   "19Z-HL0ttxFCoIJeSwp1MxYuzzGe5Ipd-",
  photo:    "1PFf3ZQgssGbvtcAKxdHtO97xBQi6m399",
  test:     "1k_jBwSr0nASiQ27ow6Tib7Ukug24xIO0",
  travaux:  "1pybPOwSy7-Gp4Kx-sWfm5x5dBKR_ekIV",
  video:    "1upIgjidP2I9yA2N4BqxB26JkUqIu60O2",
  vacances: "1pybPOwSy7-Gp4Kx-sWfm5x5dBKR_ekIV",
  presentation: "1upIgjidP2I9yA2N4BqxB26JkUqIu60O2"
};

function getFolderByType(type) {
  const key = (type || "").toLowerCase().trim();
  return DRIVE_FOLDERS[key] || DRIVE_FOLDERS["test"];
}

// Les comptes de service n'ont pas de quota My Drive :
// les uploads doivent cibler un dossier situe dans un Shared Drive.
async function assertSharedDriveFolder(folderId) {
  const folder = await drive.files.get({
    fileId: folderId,
    fields: 'id, name, driveId, mimeType',
    supportsAllDrives: true
  });

  const driveId = folder?.data?.driveId;
  if (!driveId) {
    throw new Error(
      `Le dossier Drive (${folderId}) n'est pas dans un Shared Drive. ` +
      `Avec un compte de service, creez/utilisez un Shared Drive puis placez ce dossier dedans.`
    );
  }
}

// ── Upload + Permissions + URL ──
async function uploadToDrive(filePath, fileName, mimeType, folderId) {
  await assertSharedDriveFolder(folderId);

  // 1. Créer le fichier dans Drive
  const file = await drive.files.create({
    resource: {
      name:    fileName,
      parents: [folderId]
    },
    media: {
      mimeType: mimeType,
      body:     fs.createReadStream(filePath)
    },
    fields: "id, name, webViewLink, webContentLink",
    supportsAllDrives: true
  });

  const fileId = file.data.id;

  // 2. Rendre le fichier public
  await drive.permissions.create({
    fileId:      fileId,
    requestBody: {
      role: "reader",
      type: "anyone"
    },
    supportsAllDrives: true
  });

  // 3. Supprimer le fichier temporaire local
  fs.unlinkSync(filePath);

  // 4. Retourner les URLs
  return {
    fileId:       fileId,
    fileName:     file.data.name,
    directUrl:    `https://drive.google.com/uc?export=view&id=${fileId}`,
    previewUrl:   `https://drive.google.com/file/d/${fileId}/preview`,
    downloadUrl:  `https://drive.google.com/uc?export=download&id=${fileId}`,
    driveLink:    file.data.webViewLink
  };
}

// ── ROUTE POST /upload ──
app.post("/upload", upload.single("file"), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        error:   "Aucun fichier reçu"
      });
    }

    const type      = req.body.type || "test";
    const title     = (req.body.title || '').toString().trim();
    const classe    = req.body.classe    || "";
    const matiere   = req.body.matiere   || "";
    const trimestre = req.body.trimestre || "";

    const folderId = getFolderByType(type);
    const ext = path.extname(req.file.originalname);
    const providedName = title || req.file.originalname;
    const baseName = providedName.replace(/\.[^/.]+$/, '');
    const safeName = `${baseName}${ext || ''}`.replace(/\s+/g, "_");

    console.log(`📤 Upload: ${safeName} → dossier ${type} (${folderId})`);

    const result = await uploadToDrive(req.file.path, safeName, req.file.mimetype, folderId);

    console.log(`✅ Fichier uploadé: ${result.fileId}`);

    res.json({
      success:     true,
      fileId:      result.fileId,
      fileName:    result.fileName,
      directUrl:   result.directUrl,
      previewUrl:  result.previewUrl,
      downloadUrl: result.downloadUrl,
      driveLink:   result.driveLink,
      url:         result.directUrl,
      type,
      classe,
      matiere,
      trimestre
    });

  } catch (error) {
    const backendMessage = error?.response?.data?.error?.message || error?.message || 'Erreur inconnue';
    console.error("❌ Erreur upload:", backendMessage);

    if (req.file && fs.existsSync(req.file.path)) {
      fs.unlinkSync(req.file.path);
    }

    const uploadType = req?.body?.type || "test";
    const folderId = getFolderByType(uploadType);
    const isFolderError = /File not found/i.test(backendMessage);
    const isQuotaError = /Service Accounts do not have storage quota/i.test(backendMessage);
    const isSharedDriveError = /n'est pas dans un Shared Drive/i.test(backendMessage);

    let clientError = backendMessage;
    if (isFolderError) {
      clientError = `Dossier Drive inaccessible pour le type '${uploadType}' (folderId: ${folderId}). Partagez ce dossier avec le compte de service et reessayez.`;
    } else if (isQuotaError || isSharedDriveError) {
      clientError = `Le dossier Drive du type '${uploadType}' doit etre dans un Shared Drive (pas My Drive) pour un compte de service. ` +
        `Action: creez/choisissez un Shared Drive, deplacez le dossier cible dedans, partagez-le avec l'email du compte de service, puis mettez a jour l'ID dans server.js.`;
    }

    res.status(500).json({
      success: false,
      error: clientError
    });
  }
});

app.get('/health', (req, res) => {
  res.json({ status: 'ok', message: 'Serveur Eden Family School operationnel' });
});

// ── ROUTE GET /files/:type ──
app.get("/files/:type", async (req, res) => {
  try {
    const folderId  = getFolderByType(req.params.type);

    const response = await drive.files.list({
      q:      `'${folderId}' in parents and trashed = false`,
      fields: "files(id, name, mimeType, createdTime, size)",
      orderBy: "createdTime desc",
      supportsAllDrives: true,
      includeItemsFromAllDrives: true
    });

    res.json({
      success: true,
      files:   response.data.files
    });

  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ── ROUTE DELETE /delete/:fileId ──
app.delete("/delete/:fileId", async (req, res) => {
  try {
    const fileId = req.params.fileId;

    await drive.files.delete({ fileId, supportsAllDrives: true });

    res.json({ success: true, message: "Fichier supprimé" });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`✅ Backend Node.js démarré sur le port ${PORT}`);
  console.log(`📁 Dossiers Drive configurés pour: examen, devoir, photo, video, test, travaux, vacances, presentation`);
  console.log(`🔑 Clé Drive: ${fs.existsSync(DRIVE_KEY_PATH) ? 'trouvée' : 'MANQUANTE'}`);
});


