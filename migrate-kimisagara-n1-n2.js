const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.join(__dirname, 'serviceAccountKey.json'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://edensmart-app-default-rtdb.firebaseio.com'
});

const db = admin.database();

async function migrateKimisagaraBulletins(className, oldPath) {
  console.log(`🔍 Migration des bulletins Kimisagara ${className}...`);

  try {
    // Vérifier les données existantes dans l'ancien chemin
    const oldSnapshot = await db.ref(oldPath).once('value');
    const oldData = oldSnapshot.val();

    if (!oldData) {
      console.log(`❌ Aucune donnée dans ${oldPath}`);
      return { migrated: 0, duplicates: 0 };
    }

    const oldCount = Object.keys(oldData).length;
    console.log(`📊 Trouvé ${oldCount} bulletins dans ${oldPath}`);

    // Nouvelle structure
    const newPath = `bulletins/maternelle/kimisagara/${className}`;
    const newSnapshot = await db.ref(newPath).once('value');
    const newData = newSnapshot.val() || {};
    const newCount = Object.keys(newData).length;
    console.log(`📊 ${newCount} bulletins déjà dans ${newPath}`);

    // Identifier les doublons potentiels
    let duplicates = 0;
    let toMigrate = 0;
    const updates = {};

    Object.entries(oldData).forEach(([key, bulletin]) => {
      const studentId = bulletin.studentId;
      const trimester = bulletin.trimester;

      // Vérifier si ce bulletin existe déjà dans la nouvelle structure
      const existingKeys = Object.keys(newData);
      let isDuplicate = false;

      for (const newKey of existingKeys) {
        const newBulletin = newData[newKey];
        if (newBulletin.studentId === studentId && newBulletin.trimester === trimester) {
          isDuplicate = true;
          break;
        }
      }

      if (isDuplicate) {
        duplicates++;
        console.log(`⚠️  Doublon ignoré: ${bulletin.studentName} (trimestre ${trimester})`);
      } else {
        toMigrate++;
        updates[`${newPath}/${key}`] = bulletin;
        console.log(`➡️ A migrer: ${bulletin.studentName} (trimestre ${trimester})`);
      }
    });

    if (toMigrate > 0) {
      console.log(`\n🔄 Migration de ${toMigrate} bulletins...`);
      await db.ref().update(updates);
      console.log('✅ Migration terminée !');
    } else {
      console.log('\n✅ Aucun bulletin à migrer (tous des doublons)');
    }

    console.log(`\n📋 Résumé ${className}:`);
    console.log(`   Ancien: ${oldCount} bulletins`);
    console.log(`   Nouveaux: ${newCount} bulletins`);
    console.log(`   Doublons: ${duplicates}`);
    console.log(`   Migrés: ${toMigrate}`);

    return { migrated: toMigrate, duplicates };

  } catch (error) {
    console.error('❌ Erreur:', error.message);
    return { migrated: 0, duplicates: 0 };
  }
}

async function migrateAllKimisagara() {
  console.log('🚀 Début de la migration des bulletins Kimisagara N1 et N2\n');

  const results = {};

  // Migrer N1
  results.N1 = await migrateKimisagaraBulletins('N1', 'bulletins_kimisagara_N1');
  console.log('');

  // Migrer N2
  results.N2 = await migrateKimisagaraBulletins('N2', 'bulletins_kimisagara_N2');
  console.log('');

  // Résumé global
  console.log('📊 RÉSUMÉ GLOBAL:');
  console.log(`   N1 - Migrés: ${results.N1.migrated}, Doublons: ${results.N1.duplicates}`);
  console.log(`   N2 - Migrés: ${results.N2.migrated}, Doublons: ${results.N2.duplicates}`);
  console.log(`   Total migrés: ${results.N1.migrated + results.N2.migrated}`);

  await db.goOffline();
}

migrateAllKimisagara();