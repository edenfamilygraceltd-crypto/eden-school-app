// Backend Node.js simple pour Eden Family School
// Utilise Firebase Admin SDK pour accéder à la même base de données que le front

const express = require('express');
const cors = require('cors');
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Importer les credentials Firebase
const serviceAccount = require('./serviceAccount.json');


// Initialiser Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://edensmart-app-default-rtdb.firebaseio.com',
  storageBucket: 'edensmart-app.appspot.com' // Bucket de stockage
});

const db = admin.database(); // Realtime Database
const bucket = admin.storage().bucket(); // Firebase Storage

const app = express();
const PORT = process.env.PORT || 3001;

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

// Exemple: stats simples en lisant la base (students / teachers)
app.get('/api/stats', async (req, res) => {
  try {
    const studentsSnap = await db.ref('students').once('value');
    const teachersSnap = await db.ref('teachers').once('value');

    res.json({
      totalStudents: studentsSnap.numChildren(),
      totalTeachers: teachersSnap.numChildren()
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

app.listen(PORT, () => {
  console.log(`✅ Backend Node.js démarré sur le port ${PORT}`);
});


