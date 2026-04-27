// Script pour insérer des données de test dans Firebase
// À exécuter une seule fois pour initialiser les données

const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccount.json');

// Initialiser Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function seedDatabase() {
  try {
    console.log('Ajout des données de test à Firebase...');

    // Ajouter des directeurs
    const directorsData = [
      {
        name: 'Dr. Innocent Nduwimana',
        email: 'innocent.nduwimana@edenfamily.rw',
        phone: '+250 78 123 4567',
        role: 'director',
        createdAt: new Date().toISOString()
      },
      {
        name: 'Prof. Marie Uwimana',
        email: 'marie.uwimana@edenfamily.rw',
        phone: '+250 78 234 5678',
        role: 'director',
        createdAt: new Date().toISOString()
      }
    ];

    console.log('Ajout des directeurs...');
    for (const director of directorsData) {
      await db.collection('directors').add(director);
    }
    console.log(`✓ ${directorsData.length} directeurs ajoutés`);

    // Ajouter des secrétaires
    const secretariesData = [
      {
        name: 'Claudette Habimana',
        email: 'claudette.habimana@edenfamily.rw',
        phone: '+250 78 345 6789',
        role: 'secretary',
        department: 'administration',
        createdAt: new Date().toISOString(),
        status: 'active'
      },
      {
        name: 'Jean Kwizera',
        email: 'jean.kwizera@edenfamily.rw',
        phone: '+250 78 456 7890',
        role: 'accountant',
        department: 'finance',
        createdAt: new Date().toISOString(),
        status: 'active'
      },
      {
        name: 'Francine Mukamusoni',
        email: 'francine.mukamusoni@edenfamily.rw',
        phone: '+250 78 567 8901',
        role: 'secretary',
        department: 'administration',
        createdAt: new Date().toISOString(),
        status: 'active'
      }
    ];

    console.log('Ajout des secrétaires...');
    for (const secretary of secretariesData) {
      await db.collection('secretaries').add(secretary);
    }
    console.log(`✓ ${secretariesData.length} secrétaires ajoutés`);

    // Ajouter des enseignants
    const teachersData = [
      {
        name: 'Joseph Niyibizi',
        email: 'joseph.niyibizi@edenfamily.rw',
        phone: '+250 78 678 9012',
        role: 'teacher',
        classe: '1ère A Maternelle',
        section: 'maternelle',
        createdAt: new Date().toISOString(),
        status: 'active'
      },
      {
        name: 'Sandrine Nyirubugara',
        email: 'sandrine.nyirubugara@edenfamily.rw',
        phone: '+250 78 789 0123',
        role: 'teacher',
        classe: '2ème A Primaire',
        section: 'primaire',
        createdAt: new Date().toISOString(),
        status: 'active'
      },
      {
        name: 'Patrick Mwebaze',
        email: 'patrick.mwebaze@edenfamily.rw',
        phone: '+250 78 890 1234',
        role: 'teacher',
        classe: '3ème A Primaire',
        section: 'primaire',
        createdAt: new Date().toISOString(),
        status: 'active'
      },
      {
        name: 'Cécile Mukamurangwa',
        email: 'cecile.mukamurangwa@edenfamily.rw',
        phone: '+250 78 901 2345',
        role: 'teacher',
        classe: '4ème A Primaire',
        section: 'primaire',
        createdAt: new Date().toISOString(),
        status: 'active'
      },
      {
        name: 'David Mutoni',
        email: 'david.mutoni@edenfamily.rw',
        phone: '+250 78 012 3456',
        role: 'teacher',
        classe: 'Sciences - Tous niveaux',
        section: 'primaire',
        createdAt: new Date().toISOString(),
        status: 'active'
      }
    ];

    console.log('Ajout des enseignants...');
    for (const teacher of teachersData) {
      await db.collection('teachers').add(teacher);
    }
    console.log(`✓ ${teachersData.length} enseignants ajoutés`);

    // Ajouter des parents (pour l'onglet Parents)
    const parentsData = [
      {
        nom: 'Francois Habimana',
        email: 'francois.habimana@gmail.com',
        telephone: '+250 78 123 0000',
        enfants: 2,
        statut: 'Actif',
        dernierAcces: new Date().toISOString(),
        createdAt: new Date().toISOString()
      },
      {
        nom: 'Miriam Rwigema',
        email: 'miriam.rwigema@gmail.com',
        telephone: '+250 78 234 0000',
        enfants: 1,
        statut: 'Actif',
        dernierAcces: new Date(Date.now() - 86400000).toISOString(),
        createdAt: new Date().toISOString()
      },
      {
        nom: 'Pascal Mutabazi',
        email: 'pascal.mutabazi@gmail.com',
        telephone: '+250 78 345 0000',
        enfants: 3,
        statut: 'Actif',
        dernierAcces: new Date(Date.now() - 172800000).toISOString(),
        createdAt: new Date().toISOString()
      }
    ];

    console.log('Ajout des parents...');
    for (const parent of parentsData) {
      await db.collection('parents').add(parent);
    }
    console.log(`✓ ${parentsData.length} parents ajoutés`);

    // Ajouter des rapports d'activités
    const activitiesData = [
      {
        utilisateur: 'Système',
        action: 'Initialisation',
        details: 'Données de test insérées dans la base de données',
        timestamp: new Date().toISOString(),
        userId: 'system'
      },
      {
        utilisateur: 'Innocent Nduwimana',
        action: 'Connexion',
        details: 'Connexion au système IT Admin',
        timestamp: new Date(Date.now() - 3600000).toISOString(),
        userId: 'director-001'
      },
      {
        utilisateur: 'Claudette Habimana',
        action: 'Création',
        details: 'Création d\'un nouveau compte utilisateur',
        timestamp: new Date(Date.now() - 7200000).toISOString(),
        userId: 'secretary-001'
      }
    ];

    console.log('Ajout des activités...');
    for (const activity of activitiesData) {
      await db.collection('activities').add(activity);
    }
    console.log(`✓ ${activitiesData.length} activités ajoutées`);

    // Ajouter des rapports (bugs/suggestions)
    const reportsData = [
      {
        type: 'Bug',
        titre: 'Erreur affichage des bulletins',
        description: 'Les bulletins ne s\'affichent pas correctement pour la classe 1ère A',
        priorite: 'Haute',
        statut: 'Nouveau',
        rapportePar: 'Joseph Niyibizi',
        rapporteParId: 'teacher-001',
        date: new Date().toISOString(),
        assigneA: '',
        resolution: ''
      },
      {
        type: 'Suggestion',
        titre: 'Ajouter export en PDF',
        description: 'Permettre l\'export des rapports en format PDF pour une meilleure conservation',
        priorite: 'Moyenne',
        statut: 'En cours',
        rapportePar: 'Claudette Habimana',
        rapporteParId: 'secretary-001',
        date: new Date(Date.now() - 86400000).toISOString(),
        assigneA: 'Jean Kwizera',
        resolution: ''
      },
      {
        type: 'Amélioration',
        titre: 'Améliorer la recherche d\'élèves',
        description: 'Ajouter des filtres supplémentaires pour la recherche d\'élèves',
        priorite: 'Basse',
        statut: 'Résolu',
        rapportePar: 'Marie Uwimana',
        rapporteParId: 'director-002',
        date: new Date(Date.now() - 172800000).toISOString(),
        assigneA: 'Innocent Nduwimana',
        resolution: 'Filtres ajoutés: classe, section, nom'
      }
    ];

    console.log('Ajout des rapports...');
    for (const report of reportsData) {
      await db.collection('reports').add(report);
    }
    console.log(`✓ ${reportsData.length} rapports ajoutés`);

    console.log('\n✓✓✓ Toutes les données de test ont été ajoutées avec succès! ✓✓✓\n');

  } catch (error) {
    console.error('Erreur lors de l\'ajout des données:', error);
  } finally {
    // Fermer la connexion
    process.exit(0);
  }
}

// Exécuter le script
seedDatabase();
