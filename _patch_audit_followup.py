from pathlib import Path
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8')
needle="""          const counts = {total: allRecords.length};
          secretaryImportRenderAudit([], counts, classIssues, missingTeachers, absentFromFile, branch, yearKey || year);"""
replacement="""          // Contrôler aussi les lignes du fichier avant leur sauvegarde.
          const selectedImportClass = document.getElementById('importClassSelect')?.value || '';
          importedStudentsData.forEach(student => {
            const name = student.name || 'Nom manquant';
            const chosenClass = String(selectedImportClass || student.classes || '').trim();
            const studentClasses = chosenClass && chosenClass !== 'À assigner' ? [chosenClass] : [];
            if (!studentClasses.length) {
              classIssues.push({name, cls:'—', branch, issue:'Aucune classe assignée dans le fichier / formulaire'});
              missingTeachers.push({name, cls:'—', branch});
              return;
            }
            studentClasses.forEach(cls => {
              const key = secretaryImportAuditNormalize(cls);
              const classRecord = classMap.get(key);
              if (!classRecord) classIssues.push({name, cls, branch, issue:'Classe importée inexistante ou non définie pour la branche/année'});
              const teacherRef = classRecord && (classRecord.teacherId || classRecord.teacherUid || classRecord.assignedTeacherId || classRecord.teacher);
              const teacherAssigned = !!teacherRef && (teacherIds.has(String(teacherRef)) || allUsers.some(t => secretaryImportAuditNormalize(t.name || t.displayName) === secretaryImportAuditNormalize(teacherRef)));
              if (!teacherAssigned && !teacherClasses.has(key)) missingTeachers.push({name, cls, branch});
            });
          });
          const counts = {total: allRecords.length};
          secretaryImportRenderAudit([], counts, classIssues, missingTeachers, absentFromFile, branch, yearKey || year);"""
assert needle in s
s=s.replace(needle,replacement,1)
anchor="          const message = `"
pos=s.index('      async function confirmImportStudents()')
i=s.index(anchor,pos)
refresh="""          const freshStudentsSnap = await database.ref('students').once('value');
          studentsData = [];
          freshStudentsSnap.forEach(child => studentsData.push({id: child.key, ...(child.val() || {})}));
          renderEnrolledStudents();
          const audit = window.__lastSecretaryStudentImportAudit;
          const auditMessage = audit ? '\\nContrôle : ' + audit.absentFromFile.length + ' absent(s) du fichier, ' + audit.classIssues.length + ' problème(s) de classe, ' + audit.missingTeachers.length + ' sans enseignant.' : '';
          const message = `"""
s=s[:i]+s[i:].replace(anchor,refresh,1)
old="""        classSelect.innerHTML = options;
      }

      // Écouter les changements de branche"""
new="""        classSelect.innerHTML = options;
        classSelect.onchange = function() {
          const confirmButton = document.getElementById('confirmImportBtn');
          if (confirmButton) confirmButton.disabled = true;
          const summary = document.getElementById('importAnalysisSummary');
          if (summary && importedStudentsData.length) { summary.className = 'alert alert-info py-2 small'; summary.textContent = 'Classe modifiée : cliquez sur « Analyser » pour actualiser le contrôle complet.'; }
          const report = document.getElementById('importFullAuditReport');
          if (report) report.innerHTML = '';
        };
      }

      // Écouter les changements de branche"""
assert old in s
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')
print('Added imported-row class/teacher audit and post-import refresh.')
