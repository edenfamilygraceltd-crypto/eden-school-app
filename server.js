// Backend Node.js simple pour Eden Family School
// Utilise Firebase Admin SDK pour accéder à la même base de données que le front

const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const nodemailer = require('nodemailer');
const { google } = require('googleapis');

// Initialiser Firebase Admin uniquement quand les credentials serveur sont disponibles.
// Le fichier serviceAccountKey.json est gitignored et ne doit jamais être requis au démarrage.
function getFirebaseAdmin() {
  if (admin.apps.length) return admin;

  const serviceAccountPath = path.join(__dirname, 'serviceAccountKey.json');
  if (fs.existsSync(serviceAccountPath)) {
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
    return admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: process.env.FIREBASE_DATABASE_URL,
      storageBucket: process.env.FIREBASE_STORAGE_BUCKET
    });
  }

  if (process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PRIVATE_KEY) {
    return admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n')
      }),
      databaseURL: process.env.FIREBASE_DATABASE_URL,
      storageBucket: process.env.FIREBASE_STORAGE_BUCKET
    });
  }

  throw new Error('FIREBASE_ADMIN_NOT_CONFIGURED');
}

function getDatabase() {
  return admin.database(getFirebaseAdmin());
}

function getBucket() {
  return admin.storage(getFirebaseAdmin()).bucket();
}

const db = { ref: (...args) => getDatabase().ref(...args) };
const bucket = {
  file: (...args) => getBucket().file(...args),
  get name() { return getBucket().name; }
};

const app = express();
const PORT = process.env.PORT || 3000;
const UPLOADS_DIR = process.env.VERCEL ? path.join('/tmp', 'eden-uploads') : path.join(__dirname, 'uploads');
const DRIVE_KEY_PATH = path.join(__dirname, 'drive-key.json');
const ACCOUNT_ALERT_EMAIL = process.env.ACCOUNT_ALERT_EMAIL || 'sergetumbwe@gmail.com';

function buildMailTransport() {
  const host = process.env.SMTP_HOST;
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASS;
  const port = Number(process.env.SMTP_PORT || 587);

  if (!host || !user || !pass) {
    return null;
  }

  return nodemailer.createTransport({
    host,
    port,
    secure: port === 465,
    auth: { user, pass }
  });
}

async function sendAccountCreationAlertEmail(payload) {
  const transporter = buildMailTransport();
  if (!transporter) {
    throw new Error('SMTP_NOT_CONFIGURED');
  }

  const roleLabel = payload.role || 'utilisateur';
  const sourceLabel = payload.source || 'application';
  const verificationStatus = payload.verificationSent ? 'Lien de vérification envoyé' : 'Lien de vérification non envoyé';
  const createdAt = new Date().toLocaleString('fr-FR');

  const text = [
    'Un nouveau compte a été créé dans Eden Family School.',
    '',
    `Nom: ${payload.name || 'N/A'}`,
    `Email: ${payload.email || 'N/A'}`,
    `Rôle: ${roleLabel}`,
    `Créé par: ${payload.createdBy || 'Système'}`,
    `Source: ${sourceLabel}`,
    `Vérification email: ${verificationStatus}`,
    `Date: ${createdAt}`
  ].join('\n');

  const html = `
    <div style="font-family:Arial,sans-serif;line-height:1.5;color:#1f2937;">
      <h2 style="margin:0 0 12px;">Nouveau compte créé</h2>
      <p>Un nouveau compte a été créé dans Eden Family School.</p>
      <table style="border-collapse:collapse;min-width:320px;">
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Nom</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${payload.name || 'N/A'}</td></tr>
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Email</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${payload.email || 'N/A'}</td></tr>
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Rôle</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${roleLabel}</td></tr>
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Créé par</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${payload.createdBy || 'Système'}</td></tr>
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Source</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${sourceLabel}</td></tr>
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Vérification email</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${verificationStatus}</td></tr>
        <tr><td style="padding:6px 10px;border:1px solid #e5e7eb;"><strong>Date</strong></td><td style="padding:6px 10px;border:1px solid #e5e7eb;">${createdAt}</td></tr>
      </table>
    </div>
  `;

  await transporter.sendMail({
    from: process.env.SMTP_FROM || process.env.SMTP_USER,
    to: ACCOUNT_ALERT_EMAIL,
    subject: `[Eden] Nouveau compte créé: ${payload.email || roleLabel}`,
    text,
    html
  });
}

// ── Auth Google Drive (initialisée à la demande) ──
function getDrive() {
  const credentials = process.env.GOOGLE_SERVICE_ACCOUNT_JSON;
  const authOptions = {
    scopes: ['https://www.googleapis.com/auth/drive']
  };

  if (credentials) {
    authOptions.credentials = JSON.parse(credentials);
  } else if (fs.existsSync(DRIVE_KEY_PATH)) {
    authOptions.keyFile = DRIVE_KEY_PATH;
  } else {
    throw new Error('GOOGLE_DRIVE_NOT_CONFIGURED');
  }

  return google.drive({ version: 'v3', auth: new google.auth.GoogleAuth(authOptions) });
}

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

app.post('/api/account-created-alert', async (req, res) => {
  try {
    const { name, email, role, createdBy, source, verificationSent } = req.body || {};

    if (!email) {
      return res.status(400).json({ success: false, error: 'Email du nouveau compte manquant.' });
    }

    try {
      await sendAccountCreationAlertEmail({ name, email, role, createdBy, source, verificationSent });
      return res.json({ success: true, message: 'Alerte email envoyée.' });
    } catch (mailError) {
      if (mailError.message === 'SMTP_NOT_CONFIGURED') {
        console.warn('⚠️ SMTP non configuré: alerte email non envoyée pour', email);
        return res.status(202).json({ success: false, warning: 'SMTP non configuré. Définissez SMTP_HOST, SMTP_PORT, SMTP_USER et SMTP_PASS.' });
      }
      throw mailError;
    }
  } catch (error) {
    console.error('Erreur lors de l\'envoi de l\'alerte de création de compte:', error);
    res.status(500).json({ success: false, error: error.message || 'Erreur interne serveur' });
  }
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

// ── Upload + Permissions + URL ──
async function uploadToDrive(filePath, fileName, mimeType, folderId) {
  // 1. Créer le fichier dans Drive
  const file = await getDrive().files.create({
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
  await getDrive().permissions.create({
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

    let clientError = backendMessage;
    if (/File not found/i.test(backendMessage)) {
      clientError = `Dossier Drive inaccessible (type: '${uploadType}', id: ${folderId}). Verifiez que le dossier existe et est partage avec le compte de service.`;
    } else if (/Service Accounts do not have storage quota/i.test(backendMessage)) {
      clientError = `Quota Drive: le dossier '${uploadType}' (${folderId}) doit etre place dans un Shared Drive. ` +
        `1) Creez un Shared Drive dans drive.google.com. ` +
        `2) Ajoutez le compte de service comme membre. ` +
        `3) Creez les dossiers dans ce Shared Drive. ` +
        `4) Mettez a jour les IDs dans DRIVE_FOLDERS (server.js).`;
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

    const response = await getDrive().files.list({
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

    await getDrive().files.delete({ fileId, supportsAllDrives: true });

    res.json({ success: true, message: "Fichier supprimé" });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

module.exports = app;

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Backend Node.js démarré sur le port ${PORT}`);
    console.log(`Dossiers Drive configurés pour: examen, devoir, photo, video, test, travaux, vacances, presentation`);
    console.log(`Clé Drive: ${fs.existsSync(DRIVE_KEY_PATH) || process.env.GOOGLE_SERVICE_ACCOUNT_JSON ? 'trouvée' : 'MANQUANTE'}`);
    console.log(`Alerte création de compte vers: ${ACCOUNT_ALERT_EMAIL}`);
  });
}


