const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.join(__dirname, 'serviceAccountKey.json'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://edensmart-app-default-rtdb.firebaseio.com'
});

const db = admin.database();

async function migrateKimisagaraN3Bulletins() {
  console.log('🔍 Migration des bulletins Kimisagara N3...');

  try {
    // Vérifier les données existantes dans bulletins_kimisagara_N3
    const oldSnapshot = await db.ref('bulletins_kimisagara_N3').once('value');
    const oldData = oldSnapshot.val();

    if (!oldData) {
      console.log('❌ Aucune donnée dans bulletins_kimisagara_N3');
      return;
    }

    const oldCount = Object.keys(oldData).length;
    console.log(`📊 Trouvé ${oldCount} bulletins dans bulletins_kimisagara_N3`);

    // Vérifier les données existantes dans la nouvelle structure
    const newSnapshot = await db.ref('bulletins/maternelle/kimisagara/N3').once('value');
    const newData = newSnapshot.val() || {};
    const newCount = Object.keys(newData).length;
    console.log(`📊 ${newCount} bulletins déjà dans bulletins/maternelle/kimisagara/N3`);

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
        updates[`bulletins/maternelle/kimisagara/N3/${key}`] = bulletin;
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

    console.log(`\n📋 Résumé:`);
    console.log(`   Ancien: ${oldCount} bulletins`);
    console.log(`   Nouveaux: ${newCount} bulletins`);
    console.log(`   Doublons: ${duplicates}`);
    console.log(`   Migrés: ${toMigrate}`);

  } catch (error) {
    console.error('❌ Erreur:', error.message);
  } finally {
    await db.goOffline();
  }
}

migrateKimisagaraN3Bulletins();