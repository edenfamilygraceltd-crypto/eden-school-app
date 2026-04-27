/**
 * MIGRATION COMPLÈTE - Combine tous les scripts de migration
 * 
 * 1️⃣ Migrer les bulletins vers la nouvelle structure hiérarchique
 * 2️⃣ Mettre à jour les teacherId des étudiants
 * 
 * Usage: node migrate-all.js
 */

const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require(path.join(__dirname, 'serviceAccountKey.json'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://edensmart-app-default-rtdb.firebaseio.com'
});

const db = admin.database();

// ============================================================================
// ÉTAPE 1: Migrer les bulletins vers la nouvelle structure hiérarchique
// ============================================================================

const BULLETIN_MAPPINGS = [
  {
    oldRef: 'bulletins_maternelle_N1A',
    newRef: 'bulletins/maternelle/kacyiru/N1A'
  },
  {
    oldRef: 'bulletins_maternelle_N1B',
    newRef: 'bulletins/maternelle/kacyiru/N1B'
  },
  {
    oldRef: 'bulletins_maternelle_N2',
    newRef: 'bulletins/maternelle/kacyiru/N2'
  },
  {
    oldRef: 'bulletins_maternelle_N3',
    newRef: 'bulletins/maternelle/kacyiru/N3'
  },
  {
    oldRef: 'bulletins_maternelle_N2_KA',
    newRef: 'bulletins/maternelle/kacyiru/N2_KA'
  },
  {
    oldRef: 'bulletins_maternelle_N3_GI',
    newRef: 'bulletins/maternelle/gisozi/N3_GI'
  },
  {
    oldRef: 'bulletins_gisozi_N1A',
    newRef: 'bulletins/maternelle/gisozi/N1A'
  },
  {
    oldRef: 'bulletins_gisozi_N1B',
    newRef: 'bulletins/maternelle/gisozi/N1B'
  },
  {
    oldRef: 'bulletins_gisozi_N2',
    newRef: 'bulletins/maternelle/gisozi/N2'
  },
  {
    oldRef: 'bulletins_kimisagara_N1',
    newRef: 'bulletins/maternelle/kimisagara/N1'
  },
  {
    oldRef: 'bulletins_kimisagara_N3',
    newRef: 'bulletins/maternelle/kimisagara/N3'
  }
];

async function migrateBulletins() {
  console.log('\n🚀 ÉTAPE 1: MIGRATION DES BULLETINS');
  console.log('=' .repeat(70));

  let bulletinsMigrated = 0;
  let bulletinsSkipped = 0;
  let bulletinsErrors = 0;

  for (const map of BULLETIN_MAPPINGS) {
    const snapshot = await db.ref(map.oldRef).once('value');

    if (!snapshot.exists()) {
      console.log(`⚠️  ${map.oldRef} - Aucune donnée`);
      bulletinsSkipped++;
      continue;
    }

    const data = snapshot.val();
    const count = Object.keys(data).length;

    try {
      await db.ref(map.newRef).set(data);
      console.log(`✅ ${map.oldRef} → ${map.newRef} (${count} bulletins)`);
      bulletinsMigrated += count;
    } catch (err) {
      console.error(`❌ Erreur pour ${map.oldRef}: ${err.message}`);
      bulletinsErrors++;
    }
  }

  console.log('=' .repeat(70));
  console.log(`📊 Bulletins migrés: ${bulletinsMigrated} | Ignorés: ${bulletinsSkipped} | Erreurs: ${bulletinsErrors}`);

  return { bulletinsMigrated, bulletinsSkipped, bulletinsErrors };
}

// ============================================================================
// ÉTAPE 2: Mettre à jour les teacherId des étudiants
// ============================================================================

async function migrateStudentTeacherIds() {
  console.log('\n🚀 ÉTAPE 2: MISE À JOUR DES TEACHERID');
  console.log('=' .repeat(70));

  try {
    // Fetch all teachers from /users where role === "teacher"
    console.log('📖 Récupération des professeurs...');
    const usersSnapshot = await db.ref('users').once('value');
    const usersData = usersSnapshot.val() || {};

    const teachers = [];
    Object.entries(usersData).forEach(([uid, userData]) => {
      if (userData.role === 'teacher') {
        teachers.push({
          uid: uid,
          name: userData.name || '',
          classes: userData.classes || [],
          branch: userData.branch || '',
          email: userData.email || ''
        });
      }
    });

    console.log(`✅ ${teachers.length} professeurs trouvés`);

    // Build mapping between teacher attributes and UID
    const teacherMapping = new Map();
    teachers.forEach(teacher => {
      const classesStr = Array.isArray(teacher.classes) 
        ? teacher.classes.join(',') 
        : (teacher.classes || '');
      const key = `${teacher.name}_${classesStr}_${teacher.branch}`.toLowerCase();
      teacherMapping.set(key, teacher.uid);
    });

    // Fetch all students from /students
    console.log('📖 Récupération des étudiants...');
    const studentsSnapshot = await db.ref('students').once('value');
    const studentsData = studentsSnapshot.val() || {};

    console.log(`✅ ${Object.keys(studentsData).length} étudiants trouvés`);

    // For each student, update teacherId if needed
    const updates = {};
    let updateCount = 0;
    let skipCount = 0;

    Object.entries(studentsData).forEach(([studentId, student]) => {
      const oldTeacherId = student.teacherId;
      const studentClass = student.class || '';
      const studentBranch = student.branch || '';

      // Skip if teacherId is already a valid Firebase UID (28 characters, alphanumeric)
      if (oldTeacherId && /^[a-zA-Z0-9]{28}$/.test(oldTeacherId)) {
        skipCount++;
        return;
      }

      // Skip if no old teacherId or missing required fields
      if (!oldTeacherId || !studentClass || !studentBranch) {
        skipCount++;
        return;
      }

      // Find matching teacher using same classes and branch
      let matchingUid = null;

      // First try: exact match with teacher name
      const exactKey = `${oldTeacherId}_${studentClass}_${studentBranch}`.toLowerCase();
      if (teacherMapping.has(exactKey)) {
        matchingUid = teacherMapping.get(exactKey);
      }

      // If not found, try with classes as array
      if (!matchingUid) {
        teachers.forEach(teacher => {
          const classes = Array.isArray(teacher.classes) ? teacher.classes : [];
          if (classes.includes(studentClass) && teacher.branch === studentBranch) {
            if (teacher.name.toLowerCase() === oldTeacherId.toLowerCase() ||
                teacher.email.toLowerCase() === oldTeacherId.toLowerCase()) {
              matchingUid = teacher.uid;
            }
          }
        });
      }

      // If still not found, try partial matches
      if (!matchingUid) {
        teachers.forEach(teacher => {
          const classes = Array.isArray(teacher.classes) ? teacher.classes : [];
          if (classes.includes(studentClass) && teacher.branch === studentBranch) {
            matchingUid = teacher.uid;
          }
        });
      }

      if (matchingUid) {
        updates[`students/${studentId}/teacherId`] = matchingUid;
        updateCount++;
      } else {
        skipCount++;
      }
    });

    // Apply all updates in a single batch
    if (updateCount > 0) {
      console.log(`🔄 Application de ${updateCount} mises à jour...`);
      await db.ref().update(updates);
      console.log(`✅ ${updateCount} étudiants mis à jour`);
    } else {
      console.log('✅ Aucune mise à jour nécessaire');
    }

    console.log(`⏭️  ${skipCount} étudiants ignorés`);
    console.log('=' .repeat(70));

    return { updateCount, skipCount };

  } catch (error) {
    console.error('❌ Erreur lors de la migration:', error);
    return { updateCount: 0, skipCount: 0 };
  }
}

// ============================================================================
// EXÉCUTION PRINCIPALE
// ============================================================================

async function runAllMigrations() {
  console.log('\n');
  console.log('╔═══════════════════════════════════════════════════════════════════╗');
  console.log('║        🚀 MIGRATION COMPLÈTE - Eden School App              🚀    ║');
  console.log('╚═══════════════════════════════════════════════════════════════════╝');

  try {
    // Exécute la migration des bulletins
    const bulletinsResult = await migrateBulletins();

    // Exécute la migration des teacherId
    const teacherResult = await migrateStudentTeacherIds();

    // Affiche le résumé final
    console.log('\n╔═══════════════════════════════════════════════════════════════════╗');
    console.log('║                     📊 RÉSUMÉ FINAL                               ║');
    console.log('╠═══════════════════════════════════════════════════════════════════╣');
    console.log(`║  ✅ Bulletins migrés: ${bulletinsResult.bulletinsMigrated}`.padEnd(68) + '║');
    console.log(`║  ✅ Étudiants mis à jour: ${teacherResult.updateCount}`.padEnd(68) + '║');
    console.log('║                                                                   ║');
    console.log('║  🎉 Migration terminée avec succès!                              ║');
    console.log('╚═══════════════════════════════════════════════════════════════════╝\n');

  } catch (error) {
    console.error('❌ Erreur fatale:', error);
  } finally {
    await db.goOffline();
    process.exit(0);
  }
}

// Lancer les migrations
runAllMigrations();
