from pathlib import Path
p = Path(r"C:\Users\graph\Documents\directeur portaille\secretary.html")
s = p.read_text(encoding="utf-8")
start = s.index("      async function exportSecretaryStudentsByClassPDF() {")
end = s.index("\n      // Fonction générique pour remplir les sélecteurs de classes", start)
new = r'''      async function exportSecretaryStudentsByClassPDF() {
        const data = getSecretarySelectedClassExportData();
        if (!data) return;
        if (typeof html2pdf !== 'function') {
          showNotification('La bibliothèque PDF n’est pas disponible.', 'danger');
          return;
        }
        const esc = value => String(value ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
        const norm = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/\s+/g, ' ');
        const nameOf = s => String(s.name || s.fullName || s.nomPrenom || s.nomComplet || s.studentName || s.studentFullName || [s.firstName || s.prenom, s.lastName || s.nom].filter(Boolean).join(' ') || '').trim();
        const refOf = s => norm(s.reference || s.codeUnique || s.registrationReference || s.referenceInscription || s.registrationId || s.inscriptionId || s.idInscription || s.studentId || s.studentID || '');
        const birthOf = s => norm(s.dateNaissance || s.birthDate || s.dateOfBirth || s.dob || '');
        const branchOf = s => s.branch || s.branche || s.branchName || s.schoolBranch || s.campus || s.site || '';
        const classKeys = ['class','classe','classes','className','class_name','classId','classID','classeId','niveau','level','studentClass','currentClass','classAssigned','classroom','grade','gradeName','class_id','classeActuelle'];
        const classTokens = [data.className, data.classLabel].map(norm).filter(Boolean);
        // Resolve class ID to its displayed name when the selector stores an ID.
        try {
          (Array.isArray(classesData) ? classesData : []).forEach(c => {
            const id = norm(c.id || c.classId || c.classID || c.key || '');
            if (id && classTokens.includes(id)) classTokens.push(norm(c.name || c.className || c.label || ''));
          });
        } catch (_) {}
        const classMatches = s => {
          for (const k of classKeys) {
            const v = s && s[k];
            const list = Array.isArray(v) ? v : [v];
            for (const item of list) {
              if (item && typeof item === 'object') {
                const vals = [item.id,item.classId,item.classID,item.name,item.className,item.label,item.value,item.class,item.classe];
                if (vals.some(x => classTokens.includes(norm(x)))) return true;
              } else if (item !== undefined && item !== null && classTokens.includes(norm(item))) return true;
            }
          }
          return false;
        };
        const branchMatches = s => {
          if (!data.branchName) return true;
          const b = norm(branchOf(s));
          return !b || b === norm(data.branchName);
        };
        const excluded = /payment|paiement|paid|unpaid|fee|fees|tuition|schoolfee|amount|montant|balance|solde|debt|dette|receipt|recu|reçu|invoice|facture|transaction|echeance|échéance|financial|financier|cash|money|frais|password|token|secret|credential|importedAt|createdAt|updatedAt|timestamp|firebase|uid|parentUID|approvedBy|approvedAt|importedBy|lastModified/i;
        const clean = (obj, prefix = '', out = {}) => {
          if (!obj || typeof obj !== 'object') return out;
          Object.entries(obj).forEach(([k,v]) => {
            if (excluded.test(k) || k === 'id' || k.startsWith('_') || /^\$/.test(k)) return;
            const label = prefix ? prefix + ' / ' + k : k;
            if (v && typeof v === 'object' && !Array.isArray(v)) clean(v, label, out);
            else if (Array.isArray(v)) out[label] = v.map(x => x && typeof x === 'object' ? Object.values(x).filter(y => y != null && typeof y !== 'object').join(' ') : String(x ?? '')).join(' ; ');
            else out[label] = v == null ? '' : (typeof v === 'boolean' ? (v ? 'Oui' : 'Non') : String(v));
          });
          return out;
        };
        try {
          let registrations = Array.isArray(inscriptionsData) ? inscriptionsData.slice() : [];
          try {
            const snap = await database.ref('inscriptions-scolaires').once('value');
            registrations = Object.entries(snap.val() || {}).map(([id, value]) => Object.assign({ id }, value || {}));
          } catch (e) { console.warn('Lecture Firebase des inscriptions:', e); }
          let candidates = [];
          (Array.isArray(studentsData) ? studentsData : []).forEach(s => { if (classMatches(s) && branchMatches(s)) candidates.push(Object.assign({}, s)); });
          (Array.isArray(filteredStudents) ? filteredStudents : []).forEach(s => { if (classMatches(s) && branchMatches(s)) candidates.push(Object.assign({}, s)); });
          // Add every enrollment record assigned to the selected class, not just records already in the student list.
          registrations.forEach(r => { if (classMatches(r) && branchMatches(r)) candidates.push(Object.assign({}, r)); });
          // Preserve the selected class's already-filtered rows even when records use a different class alias.
          (Array.isArray(data.rows) ? data.rows : []).forEach(s => candidates.push(Object.assign({}, s)));
          // Merge enrollment data into matching student records; keep unmatched class records too.
          const merged = [];
          candidates.forEach(candidate => {
            const ref = refOf(candidate), nm = norm(nameOf(candidate)), dob = birthOf(candidate);
            let existing = merged.find(x => (ref && refOf(x) === ref) || (nm && norm(nameOf(x)) === nm && (!dob || !birthOf(x) || dob === birthOf(x))));
            if (existing) {
              Object.entries(candidate).forEach(([k,v]) => {
                if ((existing[k] === undefined || existing[k] === null || existing[k] === '') && v !== undefined && v !== null && v !== '') existing[k] = v;
                else if (existing[k] && v && typeof existing[k] === 'object' && typeof v === 'object' && !Array.isArray(existing[k]) && !Array.isArray(v)) existing[k] = Object.assign({}, v, existing[k]);
              });
            } else merged.push(candidate);
          });
          const enriched = merged.filter(s => nameOf(s) || refOf(s));
          if (!enriched.length) {
            showNotification('Aucun élève trouvé pour cette classe. Vérifiez le champ classe dans les inscriptions.', 'warning');
            return;
          }
          const cards = enriched.map((student, index) => {
            const record = clean(student);
            record['Classe sélectionnée'] = record['Classe sélectionnée'] || student.class || student.classe || student.className || student.studentClass || data.classLabel || data.className;
            record['Branche sélectionnée'] = record['Branche sélectionnée'] || branchOf(student) || data.branchName || '—';
            record['Année scolaire'] = record['Année scolaire'] || student.academicYear || student.anneeAcademique || student.schoolYear || data.academicYear || '—';
            const entries = Object.entries(record).filter(([k,v]) => String(k).trim() && String(v ?? '').trim() !== '');
            if (!entries.length) entries.push(['Nom complet', nameOf(student) || '—']);
            let cells = '';
            for (let i = 0; i < entries.length; i += 2) {
              const a = entries[i], b = entries[i + 1];
              cells += '<tr><th>' + esc(a[0]) + '</th><td>' + esc(a[1]) + '</td>' +
                (b ? '<th>' + esc(b[0]) + '</th><td>' + esc(b[1]) + '</td>' : '<th></th><td></td>') + '</tr>';
            }
            return '<section class="student-card"><div class="student-title"><span>' + (index + 1) + '. ' + esc(nameOf(student) || 'Élève sans nom enregistré') + '</span><span class="student-count">Dossier ' + (index + 1) + ' / ' + enriched.length + '</span></div><table class="student-details"><tbody>' + cells + '</tbody></table></section>';
          }).join('');
          const container = document.createElement('div');
          container.id = 'secretary-student-pdf-export';
          container.style.cssText = 'position:absolute;left:0;top:0;width:190mm;background:#ffffff;color:#172033;padding:0;font-family:Arial,Helvetica,sans-serif;box-sizing:border-box;z-index:2147483647;';
          container.innerHTML =
            '<style>' +
            '#secretary-student-pdf-export *{box-sizing:border-box}'+
            '#secretary-student-pdf-export .school-head{text-align:center;border-bottom:3px solid #174a36;padding:0 0 4mm;margin-bottom:4mm}'+
            '#secretary-student-pdf-export .school-name{font-size:19px;font-weight:800;letter-spacing:1px;color:#174a36}'+
            '#secretary-student-pdf-export .school-sub{font-size:13px;font-weight:700;margin-top:2mm;color:#172033}'+
            '#secretary-student-pdf-export .meta{font-size:10px;margin-top:2mm;color:#334155}'+
            '#secretary-student-pdf-export .student-card{margin:0 0 4mm;border:1px solid #b9c9c0;border-radius:2mm;overflow:hidden;break-inside:avoid;page-break-inside:avoid;background:#fff}'+
            '#secretary-student-pdf-export .student-title{display:flex;justify-content:space-between;gap:5mm;background:#174a36;color:#fff;font-size:11px;font-weight:700;padding:2.5mm 3mm}'+
            '#secretary-student-pdf-export .student-count{font-size:9px;font-weight:400;white-space:nowrap}'+
            '#secretary-student-pdf-export .student-details{width:100%;border-collapse:collapse;table-layout:fixed;font-size:8px;line-height:1.25}'+
            '#secretary-student-pdf-export .student-details th{width:18%;background:#e8f1ec;color:#174a36;font-weight:700;text-align:left}'+
            '#secretary-student-pdf-export .student-details td{width:32%;color:#172033;background:#fff}'+
            '#secretary-student-pdf-export .student-details th,#secretary-student-pdf-export .student-details td{border:1px solid #d6dfda;padding:1.6mm;vertical-align:top;overflow-wrap:anywhere;word-break:break-word}'+
            '#secretary-student-pdf-export .footer{font-size:9px;text-align:right;color:#475569;margin-top:3mm;padding-top:2mm;border-top:1px solid #d6ddd9}'+
            '</style><div class="school-head"><div class="school-name">EDEN FAMILY SCHOOL</div><div class="school-sub">LISTE DES ÉLÈVES — DOSSIERS COMPLETS</div><div class="meta">Classe : <b>' + esc(data.classLabel || data.className) + '</b> &nbsp; | &nbsp; Branche : <b>' + esc(data.branchName || 'Toutes') + '</b> &nbsp; | &nbsp; Année scolaire : <b>' + esc(data.academicYear || '—') + '</b></div><div class="meta">Date : ' + esc(new Date().toLocaleDateString('fr-FR')) + ' &nbsp; | &nbsp; Effectif exporté : <b>' + enriched.length + ' élèves</b></div></div>' +
            cards + '<div class="footer">Total des dossiers exportés : <b>' + enriched.length + '</b></div>';
          document.body.appendChild(container);
          try {
            await html2pdf().set({
              margin: [8, 8, 8, 8],
              filename: 'Liste_complete_Eleves_' + String(data.classLabel || data.className).replace(/[^a-z0-9_-]+/gi, '_') + '.pdf',
              image: { type: 'jpeg', quality: 0.98 },
              html2canvas: { scale: 1.5, useCORS: true, backgroundColor: '#ffffff', logging: false, windowWidth: 900, scrollX: 0, scrollY: 0 },
              jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait', compress: true },
              pagebreak: { mode: ['css', 'legacy'], avoid: ['.student-card', 'tr'] }
            }).from(container).save();
          } finally { container.remove(); }
          showNotification('PDF complet exporté : ' + enriched.length + ' élèves, avec les informations enregistrées.', 'success');
        } catch (error) {
          console.error('Erreur export PDF classe:', error);
          showNotification('Erreur lors de la génération du PDF : ' + (error.message || error), 'danger');
        }
      }
'''
s = s[:start] + new + s[end:]
p.write_text(s, encoding="utf-8")
print("PDF export function replaced", len(new), "characters")
