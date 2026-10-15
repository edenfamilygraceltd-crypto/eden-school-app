from pathlib import Path
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8')
old="""        const modal = bootstrap.Modal.getInstance(document.getElementById('importFileModal'));
        if (modal) modal.hide();

        try {"""
new="""        // Garder la fenêtre ouverte pour afficher le contrôle réel après écriture.
        try {"""
assert old in s
s=s.replace(old,new,1)
old2="""          // Réinitialiser le formulaire
          document.getElementById('importFileInput').value = '';
          document.getElementById('importBranchSelect').value = '';
          document.getElementById('importClassSelect').value = '';
          document.getElementById('importAcademicYearSelect').value = '';
          importedStudentsData = [];
          filteredImportedStudents = [];"""
new2="""          // Refaire l'analyse sur les données relues depuis Firebase après l'import.
          await analyzeImportedStudents();
          const confirmAfterImport = document.getElementById('confirmImportBtn');
          if (confirmAfterImport) confirmAfterImport.disabled = true;
          const summaryAfterImport = document.getElementById('importAnalysisSummary');
          if (summaryAfterImport) {
            summaryAfterImport.className = 'alert alert-info py-2 small';
            summaryAfterImport.textContent = 'Import terminé et vérifié dans Firebase. Le rapport ci-dessous est l’analyse actualisée après importation. Fermez cette fenêtre lorsque vous avez terminé.';
          }"""
assert old2 in s
s=s.replace(old2,new2,1)
p.write_text(s,encoding='utf-8')
print('Post-import audit now runs after re-reading Firebase; modal remains open to show the refreshed report.')
