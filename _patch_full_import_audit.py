from pathlib import Path
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8')
# Add a detailed audit area below the comparison summary.
old='''              <div id="importAnalysisSummary" class="alert alert-info py-2 small" role="status">L’analyse compare les élèves du fichier aux fiches déjà enregistrées.</div>
              <div class="table-responsive"'''
new='''              <div id="importAnalysisSummary" class="alert alert-info py-2 small" role="status">Cliquez sur « Analyser » pour comparer le fichier et contrôler les élèves, les classes et les enseignants.</div>
              <div id="importFullAuditReport" class="mt-3" aria-live="polite"></div>
              <div class="table-responsive"'''
assert old in s
s=s.replace(old,new,1)
# Branch changes must not silently run analysis; require the user to click Analyse.
old='''          if (importedStudentsData.length && document.getElementById('importPreviewContainer')?.style.display !== 'none') {
            analyzeImportedStudents();
          }'''
new='''          if (importedStudentsData.length && document.getElementById('importPreviewContainer')?.style.display !== 'none') {
            const confirmButton = document.getElementById('confirmImportBtn');
            if (confirmButton) confirmButton.disabled = true;
            const summary = document.getElementById('importAnalysisSummary');
            if (summary) { summary.className = 'alert alert-info py-2 small'; summary.textContent = 'Branche modifiée : cliquez sur « Analyser » pour actualiser le contrôle complet.'; }
            const report = document.getElementById('importFullAuditReport');
            if (report) report.innerHTML = '';
          }'''
assert old in s
s=s.replace(old,new,1)
old='''          // L’analyse démarre automatiquement après lecture du fichier, sans modifier Firebase.
          await analyzeImportedStudents();'''
new='''          // Aucun rapprochement automatique : l'utilisateur lance l'analyse avec le bouton « Analyser ».
          const analyzeButton = document.getElementById('analyzeImportBtn');
          if (analyzeButton) analyzeButton.disabled = false;
          const summary = document.getElementById('importAnalysisSummary');
          if (summary) { summary.className = 'alert alert-info py-2 small'; summary.textContent = 'Fichier lu. Cliquez sur « Analyser » pour contrôler les correspondances, les classes et les enseignants.'; }
          const report = document.getElementById('importFullAuditReport');
          if (report) report.innerHTML = '';
          document.getElementById('confirmImportBtn').disabled = true;'''
assert old in s
s=s.replace(old,new,1)
# Replace analyze function with comparison plus full-school audit.
start=s.index('      async function analyzeImportedStudents() {')
end=s.index('\n      function displayImportPreview(data) {', start)
newfunc=r'''      function secretaryImportAuditNormalize(value) {
        return normalizeImportedStudentValue(value || '');
      }

      function secretaryImportStudentName(student) {
        return student && (student.name || student.fullName || student.nomPrenom || student.nom || student.studentName || '');
      }

      function secretaryImportStudentClasses(student) {
        const raw = student && (student.classes || student.class || student.classe || student.niveauPrimaire || student.niveauMaternelle || student.className || '');
        return (Array.isArray(raw) ? raw : String(raw || '').split(/[,;|]/)).map(v => String(v || '').trim()).filter(Boolean);
      }

      function secretaryImportBranch(student) {
        return student && (student.branch || student.branche || student.branchName || student.schoolBranch || '');
      }

      function secretaryImportYear(student) {
        return normalizeSecretarySchoolYear(student && (student.academicYear || student.anneeAcademique || student.schoolYear || student.year || ''));
      }

      function secretaryImportRenderAudit(items, counts, classIssues, missingTeachers, absentFromFile, branch, year) {
        const box = document.getElementById('importFullAuditReport');
        if (!box) return;
        const esc = value => String(value == null ? '' : value).replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
        const cards = [
          ['Élèves contrôlés', counts.total, 'primary'],
          ['Absents du fichier', absentFromFile.length, 'warning'],
          ['Classes inexistantes / non définies', classIssues.length, 'danger'],
          ['Sans enseignant assigné', missingTeachers.length, 'warning']
        ];
        const allRows = [];
        absentFromFile.forEach(s => allRows.push({name: secretaryImportStudentName(s), cls: secretaryImportStudentClasses(s).join(', ') || 'Aucune classe', issue: 'Élève existant absent du fichier importé', branch: secretaryImportBranch(s)}));
        classIssues.forEach(x => allRows.push({name: x.name, cls: x.cls, issue: x.issue, branch: x.branch}));
        missingTeachers.forEach(x => allRows.push({name: x.name, cls: x.cls, issue: 'Aucun enseignant assigné à la classe / à l’élève', branch: x.branch}));
        const unique = new Map();
        allRows.forEach(r => unique.set([secretaryImportAuditNormalize(r.name), secretaryImportAuditNormalize(r.cls), r.issue].join('|'), r));
        const rows = [...unique.values()];
        const cardHtml = cards.map(c => '<div class="col-6 col-lg-3"><div class="border rounded p-2 h-100"><div class="small text-muted">'+esc(c[0])+'</div><div class="fs-4 fw-bold text-'+c[2]+'">'+c[1]+'</div></div></div>').join('');
        const table = rows.length ? '<div class="table-responsive mt-3" style="max-height:340px;overflow:auto"><table class="table table-sm table-bordered table-striped align-middle"><thead class="table-light"><tr><th>Élève</th><th>Classe(s)</th><th>Branche</th><th>Problème détecté</th></tr></thead><tbody>'+rows.map(r=>'<tr><td>'+esc(r.name || 'Nom manquant')+'</td><td>'+esc(r.cls)+'</td><td>'+esc(r.branch || '—')+'</td><td><span class="badge bg-warning text-dark">'+esc(r.issue)+'</span></td></tr>').join('')+'</tbody></table></div>' : '<div class="alert alert-success mt-3 mb-0">Aucune anomalie parmi les contrôles demandés pour cette branche et cette année.</div>';
        box.innerHTML = '<div class="border rounded p-3"><h6 class="fw-bold mb-2"><i class="bi bi-clipboard2-check me-1"></i>Analyse complète des listes enregistrées</h6><div class="small text-muted mb-3">Branche : '+esc(branch)+' · Année : '+esc(year)+' · Les lignes signalées sont à vérifier/corriger dans Gestion académique et les affectations.</div><div class="row g-2">'+cardHtml+'</div>'+table+'</div>';
        window.__lastSecretaryStudentImportAudit = {branch, year, counts, absentFromFile: absentFromFile.map(secretaryImportStudentName), classIssues, missingTeachers, issueCount: rows.length};
      }

      async function analyzeImportedStudents() {
        if (!importedStudentsData.length) { showNotification('Sélectionnez d’abord un fichier contenant des élèves.', 'warning'); return; }
        const branch = document.getElementById('importBranchSelect').value;
        const year = document.getElementById('importAcademicYearSelect')?.value || getActiveSecretarySchoolYear();
        if (!branch) { showNotification('Choisissez une branche avant de lancer l’analyse.', 'warning'); return; }
        const button = document.getElementById('analyzeImportBtn');
        const summary = document.getElementById('importAnalysisSummary');
        const confirmButton = document.getElementById('confirmImportBtn');
        if (button) { button.disabled = true; button.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Analyse complète...'; }
        if (confirmButton) confirmButton.disabled = true;
        try {
          const [studentsSnap, classesSnap, usersSnap, enrollmentsSnap] = await Promise.all([
            database.ref('students').once('value'),
            database.ref('classes').once('value'),
            database.ref('users').once('value'),
            database.ref('inscriptions-scolaires').once('value')
          ]);
          const existingStudents = [];
          studentsSnap.forEach(child => existingStudents.push({ id: child.key, ...(child.val() || {}) }));
          const allClasses = [];
          classesSnap.forEach(child => allClasses.push({ id: child.key, ...(child.val() || {}) }));
          const allUsers = [];
          usersSnap.forEach(child => { const u = child.val() || {}; if (u.role === 'teacher') allUsers.push({id:child.key, uid:child.key, ...u}); });
          const allEnrollments = [];
          enrollmentsSnap.forEach(child => allEnrollments.push({id:child.key, ...(child.val() || {})}));

          let exact = 0, possible = 0, newCount = 0;
          importedStudentsData.forEach(student => {
            const match = getImportedStudentMatch(student, existingStudents, branch);
            student.matchId = match ? String(match.id) : '';
            student.matchName = match ? (match.name || match.fullName || match.nom || 'Élève existant') : '';
            const sameName = !!match && normalizeImportedStudentValue(match.name || match.fullName || match.nom) === normalizeImportedStudentValue(student.name);
            student.matchType = match ? (sameName ? 'Nom concordant' : 'Correspondance possible') : '';
            student.importAction = match ? (student._actionChosen ? student.importAction : 'update') : 'add';
            if (match && sameName) exact++; else if (match) possible++; else newCount++;
          });
          displayImportPreview(importedStudentsData.slice(0, 50));

          const branchKey = secretaryImportAuditNormalize(branch);
          const yearKey = normalizeSecretarySchoolYear(year || '');
          const inScope = s => {
            const b = secretaryImportAuditNormalize(secretaryImportBranch(s));
            const y = secretaryImportYear(s);
            return (!b || b === branchKey) && (!y || !yearKey || y === yearKey);
          };
          const scopeStudents = existingStudents.filter(inScope);
          const scopeEnrollments = allEnrollments.filter(inScope);
          // Merge student profiles and enrollment-only pupils so the audit doesn't hide records.
          const allRecords = [];
          const seen = new Set();
          const addRecord = (record, source) => {
            const name = secretaryImportStudentName(record);
            const id = String(record.id || record.studentId || record.code || '');
            const key = id ? 'id:'+id : 'name:'+secretaryImportAuditNormalize(name);
            if (!name && !id) return;
            if (seen.has(key)) {
              const old = allRecords.find(x => x._auditKey === key);
              if (old) { old._sources.push(source); old._raws.push(record); }
              return;
            }
            seen.add(key); allRecords.push({...record, _auditKey:key, _sources:[source], _raws:[record]});
          };
          scopeStudents.forEach(s => addRecord(s, 'Fiche élève'));
          scopeEnrollments.forEach(s => addRecord(s, 'Inscription'));
          const importedNames = new Set(importedStudentsData.map(s => secretaryImportAuditNormalize(s.name)).filter(Boolean));
          const absentFromFile = allRecords.filter(s => {
            const name = secretaryImportAuditNormalize(secretaryImportStudentName(s));
            return name && !importedNames.has(name);
          });
          const validClasses = allClasses.filter(c => {
            const cy = normalizeSecretarySchoolYear(c.academicYear || c.anneeAcademique || c.schoolYear || c.year || '');
            const cb = secretaryImportAuditNormalize(c.branch || c.branche || c.branchName || c.schoolBranch || '');
            return (!cy || !yearKey || cy === yearKey) && (!cb || cb === branchKey);
          });
          const classMap = new Map(validClasses.map(c => [secretaryImportAuditNormalize(c.name || c.className || c.class || c.classe || c.id), c]));
          const teacherIds = new Set(allUsers.flatMap(t => [String(t.id || ''), String(t.uid || '')]).filter(Boolean));
          const teacherClasses = new Set();
          allUsers.forEach(t => {
            const tb = secretaryImportAuditNormalize(t.branch || t.branche || '');
            if (tb && tb !== branchKey) return;
            const raw = Array.isArray(t.classes) ? t.classes : String(t.classes || t.class || '').split(/[,;|]/);
            raw.forEach(c => { const k = secretaryImportAuditNormalize(c); if (k) teacherClasses.add(k); });
          });
          const classIssues = [];
          const missingTeachers = [];
          const audited = new Set();
          allRecords.forEach(s => {
            const name = secretaryImportStudentName(s) || 'Nom manquant';
            const classes = secretaryImportStudentClasses(s);
            const studentBranch = secretaryImportBranch(s);
            const key = secretaryImportAuditNormalize(name)+'|'+classes.map(secretaryImportAuditNormalize).join(',');
            if (audited.has(key)) return; audited.add(key);
            if (!classes.length) classIssues.push({name, cls:'—', branch:studentBranch, issue:'Aucune classe assignée'});
            classes.forEach(cls => {
              const classKey = secretaryImportAuditNormalize(cls);
              const classRecord = classMap.get(classKey);
              if (!classRecord) classIssues.push({name, cls, branch:studentBranch, issue:'Classe inexistante ou non définie pour la branche/année'});
              const directTeacher = String(s.teacherId || s.teacherUid || s.assignedTeacherId || '');
              const directValid = directTeacher && teacherIds.has(directTeacher);
              const recordTeacher = classRecord && (classRecord.teacherId || classRecord.teacherUid || classRecord.assignedTeacherId || classRecord.teacher);
              const classTeacherValid = recordTeacher && (teacherIds.has(String(recordTeacher)) || allUsers.some(t => secretaryImportAuditNormalize(t.name || t.displayName) === secretaryImportAuditNormalize(recordTeacher)));
              if (!directValid && !classTeacherValid && !teacherClasses.has(classKey)) missingTeachers.push({name, cls, branch:studentBranch});
            });
          });
          const counts = {total: allRecords.length};
          secretaryImportRenderAudit([], counts, classIssues, missingTeachers, absentFromFile, branch, yearKey || year);
          const issues = absentFromFile.length + classIssues.length + missingTeachers.length;
          if (summary) {
            summary.className = 'alert ' + (possible || issues ? 'alert-warning' : 'alert-success') + ' py-2 small';
            summary.textContent = importedStudentsData.length + ' élève(s) importé(s) analysé(s) — ' + exact + ' correspondance(s) exacte(s), ' + possible + ' correspondance(s) possible(s), ' + newCount + ' nouvel/nouveaux élève(s). Contrôle système : ' + allRecords.length + ' enregistrement(s), ' + absentFromFile.length + ' absent(s) du fichier, ' + classIssues.length + ' problème(s) de classe, ' + missingTeachers.length + ' sans enseignant.';
          }
          if (confirmButton) confirmButton.disabled = false;
        } catch (error) {
          console.error('Erreur d’analyse complète des élèves importés :', error);
          if (summary) { summary.className = 'alert alert-danger py-2 small'; summary.textContent = 'Échec de l’analyse complète : ' + (error.message || error); }
          if (confirmButton) confirmButton.disabled = true;
        } finally {
          if (button) { button.disabled = false; button.innerHTML = '<i class="bi bi-search me-1"></i>Analyser'; }
        }
      }
'''
s=s[:start]+newfunc+s[end:]
# Ensure writes are read back from Firebase before counting them as successful.
s=s.replace("""                  await database.ref('students/' + student.matchId).update(updates);
                  updatedCount++;""","""                  await database.ref('students/' + student.matchId).update(updates);
                  const verified = await database.ref('students/' + student.matchId).once('value');
                  if (!verified.exists() || Object.entries(updates).some(([key, value]) => String((verified.val() || {})[key] ?? '') !== String(value ?? ''))) {
                    throw new Error('La vérification après modification a échoué pour ' + student.name);
                  }
                  updatedCount++;""",1)
s=s.replace("""                  await database.ref('students/' + student.matchId).update(importedFields);
                  overwrittenCount++;""","""                  await database.ref('students/' + student.matchId).update(importedFields);
                  const verified = await database.ref('students/' + student.matchId).once('value');
                  if (!verified.exists() || Object.entries(importedFields).some(([key, value]) => String((verified.val() || {})[key] ?? '') !== String(value ?? ''))) {
                    throw new Error('La vérification après écrasement a échoué pour ' + student.name);
                  }
                  overwrittenCount++;""",1)
s=s.replace("""              await database.ref('students').push().set({ ...importedFields, dateAdded: new Date().toISOString() });
              addedCount++; successCount++;""","""              const newRef = database.ref('students').push();
              const newRecord = { ...importedFields, dateAdded: new Date().toISOString() };
              await newRef.set(newRecord);
              const verifiedNew = await newRef.once('value');
              if (!verifiedNew.exists() || secretaryImportAuditNormalize((verifiedNew.val() || {}).name) !== secretaryImportAuditNormalize(student.name)) {
                throw new Error('La vérification après ajout a échoué pour ' + student.name);
              }
              addedCount++; successCount++;""",1)
# After import, reload the saved profiles from Firebase and report audit findings.
p.write_text(s,encoding='utf-8')
print('Patched secretary.html; chars=',len(s))

