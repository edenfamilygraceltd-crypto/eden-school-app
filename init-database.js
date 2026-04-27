// Script pour initialiser les données Firebase Realtime Database
// À exécuter pour créer les données manquantes

// Configuration Firebase
const firebaseConfig = {
  apiKey: "AIzaSyApUFNELOfgIe7rWEek9GLS9EIphNW09-A",
  authDomain: "edensmart-app.firebaseapp.com",
  databaseURL: "https://edensmart-app-default-rtdb.firebaseio.com",
  projectId: "edensmart-app",
  storageBucket: "edensmart-app.firebasestorage.app",
  messagingSenderId: "1093120876724",
  appId: "1:1093120876724:web:bc37448cadd18d651c77e1",
  measurementId: "G-1FL70PZZSW"
};

// Initialiser Firebase
firebase.initializeApp(firebaseConfig);
const db = firebase.database();

async function initializeDatabase() {
  try {
    console.log('Initialisation des données Firebase...');

    // Créer des étudiants de test
    const studentsRef = db.ref('students');
    const studentsData = [
      {
        name: 'Alice Uwimana',
        email: 'alice.uwimana@edenfamily.rw',
        branch: 'gisozi_maternelle',
        class: 'N3A',
        status: 'active',
        createdAt: new Date().toISOString()
      },
      {
        name: 'Bruno Hakizimana',
        email: 'bruno.hakizimana@edenfamily.rw',
        branch: 'gisozi_maternelle',
        class: 'N3A',
        status: 'active',
        createdAt: new Date().toISOString()
      },
      {
        name: 'Claire Ingabire',
        email: 'claire.ingabire@edenfamily.rw',
        branch: 'gisozi_maternelle',
        class: 'N3B',
        status: 'active',
        createdAt: new Date().toISOString()
      }
    ];

    console.log('Ajout des étudiants...');
    for (const student of studentsData) {
      await studentsRef.push(student);
    }

    // Créer des bulletins de test
    const bulletinsRef = db.ref('bulletins_maternelle');
    const bulletinsData = [
      {
        studentId: 'student1',
        studentName: 'Alice Uwimana',
        class: 'N3A',
        trimestre: 1,
        annee: 2024,
        notes: {
          math: { tj: 85, ex: 90 },
          french: { tj: 80, ex: 85 },
          science: { tj: 88, ex: 92 }
        },
        createdAt: new Date().toISOString(),
        teacherId: 'teacher1'
      },
      {
        studentId: 'student2',
        studentName: 'Bruno Hakizimana',
        class: 'N3A',
        trimestre: 1,
        annee: 2024,
        notes: {
          math: { tj: 75, ex: 80 },
          french: { tj: 82, ex: 78 },
          science: { tj: 85, ex: 88 }
        },
        createdAt: new Date().toISOString(),
        teacherId: 'teacher1'
      }
    ];

    console.log('Ajout des bulletins...');
    for (const bulletin of bulletinsData) {
      await bulletinsRef.push(bulletin);
    }

    // Créer des annonces/alertes
    const announcementsRef = db.ref('announcements');
    const announcementsData = [
      {
        message: 'Rappel: Réunion des enseignants le 15 mars à 14h',
        timestamp: Date.now(),
        author: 'Direction',
        type: 'information'
      },
      {
        message: 'Les bulletins du 1er trimestre doivent être soumis avant le 20 mars',
        timestamp: Date.now(),
        author: 'Secrétariat',
        type: 'urgent'
      }
    ];

    console.log('Ajout des annonces...');
    for (const announcement of announcementsData) {
      await announcementsRef.push(announcement);
    }

    // Créer des utilisateurs de test
    const usersRef = db.ref('users');
    const usersData = [
      {
        nom: 'Marie Uwase',
        email: 'marie.uwase@edenfamily.rw',
        role: 'teacher',
        status: 'active',
        createdAt: new Date().toISOString(),
        telephone: '+250 78 123 4567'
      },
      {
        nom: 'Jean Habimana',
        email: 'jean.habimana@edenfamily.rw',
        role: 'teacher',
        status: 'active',
        createdAt: new Date().toISOString(),
        telephone: '+250 78 234 5678'
      }
    ];

    console.log('Ajout des utilisateurs...');
    for (const user of usersData) {
      const userId = usersRef.push().key;
      await usersRef.child(userId).set(user);
    }

    console.log('✓ Base de données initialisée avec succès!');
    console.log('Vous pouvez maintenant utiliser teacher_clean.html avec des données réelles.');

  } catch (error) {
    console.error('Erreur lors de l\'initialisation:', error);
  }
}

// Exécuter l'initialisation
initializeDatabase();