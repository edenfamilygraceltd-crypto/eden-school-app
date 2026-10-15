from pathlib import Path
import shutil
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8')
start=s.index('      // Export de la liste des élèves de la classe sélectionnée (PDF + Excel)')
end=s.index('      // Fonction générique pour remplir les sélecteurs de classes', start)
backup=p.with_name('secretary.html.before-full-student-class-pdf-20261013.bak')
shutil.copy2(p, backup)
new=r'''      // Export complet des élèves d'une classe : informations de l'élève + dossier d'inscription lié.
      function getSecretarySelectedClassExportData() {
        const className = String(document.getElementById('classFilter')?.value || '').trim();
        const branchName = String(document.getElementById('branchFilter')?.value || '').trim();
        if (!className) {
          showNotification('Sélectionnez d’abord une classe dans le filtre « Classe ».', 'warning');
          return null;
        }
        const norm = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/\s+/g, ' ');
        const valueOf = value => value && typeof value === 'object' ? (value.name || value.className || value.class || value.classe || value.id || value.value || '') : value;
        const matchesClass = student => {
          const vals = [student.class, student.classe, student.classes, student.className, student.class_name, student.classId, student.classeId];
          const names = vals.flatMap(v => Array.isArray(v) ? v : [v]).map(valueOf).map(norm).filter(Boolean);
          return names.includes(norm(className));
        };
        const matchesBranch = student => {
          if (!branchName) return true;
          const branch = norm(student.branch || student.branche || student.branchName || student.schoolBranch || student.campus || '');
          return !branch || branch === norm(branchName);
        };
        const source = Array.isArray(studentsData) ? studentsData : [];
        let rows = source.filter(s => matchesClass(s) && matchesBranch(s));
        if (!rows.length && Array.isArray(filteredStudents)) rows = filteredStudents.filter(s => matchesClass(s) && matchesBranch(s));
        if (!rows.length) {
          showNotification('Aucun élève trouvé dans la classe sélectionnée. Vérifiez la classe enregistrée dans les dossiers.', 'warning');
          return null;
        }
        rows.sort((a,b) => String(a.name || a.fullName || a.nomPrenom || '').localeCompare(String(b.name || b.fullName || b.nomPrenom || '')));
        return { className, branchName: branchName || (rows[0].branch || rows[0].branche || rows[0].branchName || rows[0].schoolBranch || ''), academicYear: typeof getActiveSecretarySchoolYear === 'function' ? getActiveSecretarySchoolYear() : '', rows };
      }

      function secretaryFlattenStudentRecord(obj, prefix = '', out = {}) {
        const excluded = /payment|paiement|paid|unpaid|fee|fees|tuition|schoolfee|amount|montant|balance|solde|debt|dette|receipt|recu|reçu|invoice|facture|transaction|echeance|échéance|financial|financier|cash|money|frais/i;
        if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return out;
        Object.entries(obj).forEach(([key, value]) => {
          if (excluded.test(key) || /password|token|secret|credential/i.test(key) || /^\\$/.test(key)) return;
          const label = prefix ? prefix + ' / ' + key : key;
          if (value && typeof value === 'object' && !Array.isArray(value)) secretaryFlattenStudentRecord(value, label, out);
          else if (Array.isArray(value)) out[label] = value.map(v => v && typeof v === 'object' ? JSON.stringify(v) : String(v ?? '')).join(' ; ');
          else out[label] = value == null ? '' : (typeof value === 'boolean' ? (value ? 'Oui' : 'Non') : String(value));
        });
        return out;
      }

      function exportSecretaryStudentsByClassExcel() {
        const data = getSecretarySelectedClassExportData();
        if (!data) return;
        if (typeof XLSX === 'undefined') {
          showNotification('La bibliothèque Excel n’est pas disponible.', 'danger');
          return;
        }
        const records = data.rows.map((student, i) => Object.assign({ 'N°': i + 1 }, secretaryFlattenStudentRecord(student)));
        const ws = XLSX.utils.json_to_sheet(records);
        ws['!cols'] = Object.keys(records[0] || {}).map(k => ({ wch: Math.min(32, Math.max(12, k.length + 2)) }));
        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, 'Eleves');
        XLSX.writeFile(wb, 'Liste_complete_eleves_' + data.className.replace(/[^a-z0-9_-]+/gi, '_') + '.xlsx');
        showNotification('Liste Excel de ' + data.className + ' exportée.', 'success');
      }

      async function exportSecretaryStudentsByClassPDF() {
        const data = getSecretarySelectedClassExportData();
        if (!data) return;
        if (typeof html2pdf === 'undefined') {
          showNotification('La bibliothèque PDF n’est pas disponible.', 'danger');
          return;
        }
        const safe = value => String(value ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
        const excluded = /payment|paiement|paid|unpaid|fee|fees|tuition|schoolfee|amount|montant|balance|solde|debt|dette|receipt|recu|reçu|invoice|facture|transaction|echeance|échéance|financial|financier|cash|money|frais/i;
        try {
          // Compléter les champs familiaux depuis le dossier d'inscription public lié à l'élève.
          let registrations = [];
          try {
            const snap = await database.ref('inscriptions-scolaires').once('value');
            registrations = Object.entries(snap.val() || {}).map(([key, value]) => Object.assign({ _registrationKey: key }, value || {}));
          } catch (e) { console.warn('Lecture des dossiers d’inscription impossible:', e); }
          const norm = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/\s+/g, ' ');
          const enriched = data.rows.map(student => {
            const s = Object.assign({}, student);
            const ref = norm(s.reference || s.codeUnique || s.registrationReference || s.referenceInscription || s.registrationId || s.inscriptionId || '');
            const name = norm(s.name || s.fullName || s.nomPrenom || s.nom || s.studentName || '');
            const birth = norm(s.dateNaissance || s.birthDate || s.dateOfBirth || s.dob || '');
            const reg = registrations.find(r => {
              const rr = norm(r.reference || r.codeUnique || r.registrationReference || r.referenceInscription || '');
              if (ref && rr && ref === rr) return true;
              const rn = norm(r.nomPrenom || r.name || r.fullName || r.nom || '');
              const rb = norm(r.dateNaissance || r.birthDate || r.dateOfBirth || '');
              return !!name && name === rn && (!birth || !rb || birth === rb);
            });
            if (reg) Object.entries(reg).forEach(([key, value]) => {
              if (key === '_registrationKey' || excluded.test(key)) return;
              if (s[key] === undefined || s[key] === null || s[key] === '') s[key] = value;
              else if (s[key] && value && typeof s[key] === 'object' && typeof value === 'object' && !Array.isArray(s[key]) && !Array.isArray(value)) s[key] = Object.assign({}, value, s[key]);
            });
            return s;
          });
          const flattened = enriched.map((s, i) => Object.assign({ 'N°': String(i + 1) }, secretaryFlattenStudentRecord(s)));
          const columns = [...new Set(flattened.flatMap(row => Object.keys(row)))].filter(k => !excluded.test(k));
          const head = columns.map(col => '<th>' + safe(col) + '</th>').join('');
          const body = flattened.map(row => '<tr>' + columns.map(col => '<td>' + safe(row[col] ?? '—') + '</td>').join('') + '</tr>').join('');
          const container = document.createElement('div');
          container.style.cssText = 'background:#fff;color:#111;padding:8mm;font-family:Arial,sans-serif;width:100%;box-sizing:border-box;';
          container.innerHTML = '<div style="text-align:center;border-bottom:2px solid #1e6b4e;padding-bottom:8px;margin-bottom:10px;">' +
            '<h2 style="margin:0;font-size:18px;">EDEN FAMILY SCHOOL</h2><div style="font-size:13px;font-weight:700;margin-top:4px;">LISTE COMPLÈTE DES ÉLÈVES</div>' +
            '<div style="font-size:11px;margin-top:4px;">Classe : <strong>' + safe(data.className) + '</strong> | Branche : <strong>' + safe(data.branchName || 'Toutes') + '</strong></div>' +
            '<div style="font-size:10px;margin-top:3px;">Année scolaire : <strong>' + safe(data.academicYear || '') + '</strong> | Date : <strong>' + safe(new Date().toLocaleDateString('fr-FR')) + '</strong> | Total : <strong>' + enriched.length + '</strong></div></div>' +
            '<table style="width:100%;border-collapse:collapse;font-size:7px;table-layout:auto;"><thead style="display:table-header-group;"><tr style="background:#e8f1ec;">' + head + '</tr></thead><tbody>' + body + '</tbody></table>' +
            '<div style="margin-top:8px;font-size:9px;text-align:right;">Total : <strong>' + enriched.length + '</strong> élève(s)</div>';
          container.querySelectorAll('th,td').forEach(cell => {
            cell.style.border = '1px solid #777'; cell.style.padding = '3px'; cell.style.verticalAlign = 'top';
            cell.style.overflowWrap = 'anywhere'; cell.style.wordBreak = 'break-word';
          });
          container.querySelectorAll('th').forEach(cell => { cell.style.fontWeight = '700'; cell.style.textAlign = 'left'; });
          container.querySelectorAll('tbody tr').forEach((row, i) => { if (i % 2) row.style.background = '#f7f9f8'; row.style.pageBreakInside = 'avoid'; });
          document.body.appendChild(container);
          try {
            await html2pdf().set({
              margin: 5,
              filename: 'Liste_complete_eleves_' + data.className.replace(/[^a-z0-9_-]+/gi, '_') + '.pdf',
              image: { type: 'jpeg', quality: 0.96 },
              html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff', logging: false },
              jsPDF: { unit: 'mm', format: 'a4', orientation: 'landscape', compress: true },
              pagebreak: { mode: ['css', 'legacy'], avoid: ['tr'] }
            }).from(container).save();
          } finally { container.remove(); }
          showNotification('Liste PDF complète de ' + data.className + ' exportée (' + enriched.length + ' élèves).', 'success');
        } catch (error) {
          console.error('Erreur export PDF classe:', error);
          showNotification('Erreur lors de la génération du PDF : ' + (error.message || error), 'danger');
        }
      }

'''
s=s[:start]+new+s[end:]
p.write_text(s, encoding='utf-8')
print('Updated:', p)
print('Backup:', backup)
print('PDF function count:', s.count('async function exportSecretaryStudentsByClassPDF()'))
