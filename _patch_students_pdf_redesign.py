from pathlib import Path
import shutil
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8')
start=s.index('      // Export complet des élèves d’une classe') if '      // Export complet des élèves d’une classe' in s else s.index('      // Export complet des élèves d\'une classe')
end=s.index('      // Fonction générique pour remplir les sélecteurs de classes', start)
backup=p.with_name('secretary.html.before-pdf-table-redesign-20261013.bak')
shutil.copy2(p, backup)
new=r'''      // Export de classe : tableau lisible avec les champs importants et complets du dossier élève.
      function getSecretarySelectedClassExportData() {
        const className = String(document.getElementById('classFilter')?.value || '').trim();
        const branchName = String(document.getElementById('branchFilter')?.value || '').trim();
        if (!className) {
          showNotification('Sélectionnez une classe avant de lancer l’export.', 'warning');
          return null;
        }
        const norm = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/\s+/g, ' ');
        const val = v => v && typeof v === 'object' ? (v.name || v.className || v.class || v.classe || v.id || v.value || '') : v;
        const classKeys = ['class','classe','classes','className','class_name','classId','classeId','niveau','level'];
        const matchesClass = student => classKeys.some(k => {
          const value = student[k];
          return (Array.isArray(value) ? value : [value]).some(v => norm(val(v)) === norm(className));
        });
        const matchesBranch = student => {
          if (!branchName) return true;
          const b = norm(student.branch || student.branche || student.branchName || student.schoolBranch || student.campus || '');
          return !b || b === norm(branchName);
        };
        const source = Array.isArray(studentsData) ? studentsData : [];
        let rows = source.filter(s => matchesClass(s) && matchesBranch(s));
        if (!rows.length && Array.isArray(filteredStudents)) rows = filteredStudents.filter(s => matchesClass(s) && matchesBranch(s));
        if (!rows.length) {
          showNotification('Aucun élève trouvé dans la classe sélectionnée. Vérifiez la classe et la branche.', 'warning');
          return null;
        }
        return { className, branchName: branchName || (rows[0].branch || rows[0].branche || ''), academicYear: typeof getActiveSecretarySchoolYear === 'function' ? getActiveSecretarySchoolYear() : '', rows };
      }

      // Retire toutes les clés techniques et financières; ne transforme pas chaque clé technique en colonne.
      function secretaryStudentCleanRecord(obj, prefix = '', out = {}) {
        const excluded = /payment|paiement|paid|unpaid|fee|fees|tuition|schoolfee|amount|montant|balance|solde|debt|dette|receipt|recu|reçu|invoice|facture|transaction|echeance|échéance|financial|financier|cash|money|frais|password|token|secret|credential|importedAt|createdAt|updatedAt|timestamp|firebase|uid|parentUID|approvedBy|approvedAt|source|importedBy|lastModified/i;
        if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return out;
        Object.entries(obj).forEach(([key, value]) => {
          if (excluded.test(key) || key === 'id' || key.startsWith('_') || /^\$/.test(key)) return;
          const label = prefix ? prefix + ' / ' + key : key;
          if (value && typeof value === 'object' && !Array.isArray(value)) secretaryStudentCleanRecord(value, label, out);
          else if (Array.isArray(value)) out[label] = value.map(v => v && typeof v === 'object' ? Object.values(v).join(' ') : String(v ?? '')).join(' ; ');
          else out[label] = value == null ? '' : (typeof value === 'boolean' ? (value ? 'Oui' : 'Non') : String(value));
        });
        return out;
      }

      async function secretaryGetEnrichedClassStudents(data) {
        let registrations = Array.isArray(inscriptionsData) ? inscriptionsData.slice() : [];
        // Une lecture ponctuelle garantit de récupérer les dossiers d’inscription les plus récents.
        try {
          const snap = await database.ref('inscriptions-scolaires').once('value');
          registrations = Object.entries(snap.val() || {}).map(([id, value]) => Object.assign({ id }, value || {}));
        } catch (e) { console.warn('Lecture des inscriptions pour le PDF:', e); }
        const norm = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/\s+/g, ' ');
        const getRef = s => norm(s.reference || s.codeUnique || s.registrationReference || s.referenceInscription || s.registrationId || s.inscriptionId || s.idInscription || '');
        const getName = s => norm(s.name || s.fullName || s.nomPrenom || s.nomComplet || s.studentName || [s.firstName || s.prenom, s.lastName || s.nom].filter(Boolean).join(' '));
        const getBirth = s => norm(s.dateNaissance || s.birthDate || s.dateOfBirth || s.dob || '');
        const excluded = /payment|paiement|paid|unpaid|fee|fees|tuition|schoolfee|amount|montant|balance|solde|debt|dette|receipt|recu|reçu|invoice|facture|transaction|echeance|échéance|financial|financier|cash|money|frais/i;
        return data.rows.map(student => {
          const s = Object.assign({}, student);
          const ref = getRef(s), name = getName(s), birth = getBirth(s);
          // Match prioritaire par référence d'inscription; sinon nom + date de naissance.
          const reg = registrations.find(r => {
            const rr = getRef(r);
            if (ref && rr && ref === rr) return true;
            const rn = getName(r), rb = getBirth(r);
            return !!name && name === rn && (!birth || !rb || birth === rb);
          });
          if (reg) Object.entries(reg).forEach(([key, value]) => {
            if (key === 'id' || key.startsWith('_') || excluded.test(key) || /password|token|secret|credential/i.test(key)) return;
            if (s[key] === undefined || s[key] === null || s[key] === '') s[key] = value;
            else if (s[key] && value && typeof s[key] === 'object' && typeof value === 'object' && !Array.isArray(s[key]) && !Array.isArray(value)) s[key] = Object.assign({}, value, s[key]);
          });
          return s;
        });
      }

      function secretaryStudentDisplayValue(value) {
        if (value === null || value === undefined || value === '') return '—';
        if (typeof value === 'boolean') return value ? 'Oui' : 'Non';
        if (Array.isArray(value)) return value.map(v => typeof v === 'object' ? Object.values(v).join(' ') : String(v ?? '')).join(', ');
        if (typeof value === 'object') return Object.entries(value).filter(([k]) => !/payment|paiement|fee|frais|montant|balance|solde|debt|dette/i.test(k)).map(([k,v]) => k + ': ' + (v && typeof v === 'object' ? Object.values(v).join(' ') : String(v ?? ''))).join(' | ');
        return String(value);
      }

      function exportSecretaryStudentsByClassExcel() {
        const data = getSecretarySelectedClassExportData();
        if (!data) return;
        if (typeof XLSX === 'undefined') {
          showNotification('La bibliothèque Excel n’est pas disponible.', 'danger');
          return;
        }
        secretaryGetEnrichedClassStudents(data).then(enriched => {
          const records = enriched.map((student, i) => Object.assign({ 'N°': i + 1 }, secretaryStudentCleanRecord(student)));
          const ws = XLSX.utils.json_to_sheet(records);
          ws['!cols'] = Object.keys(records[0] || {}).map(k => ({ wch: Math.min(34, Math.max(14, k.length + 2)) }));
          const wb = XLSX.utils.book_new();
          XLSX.utils.book_append_sheet(wb, ws, 'Eleves');
          XLSX.writeFile(wb, 'Liste_complete_eleves_' + data.className.replace(/[^a-z0-9_-]+/gi, '_') + '.xlsx');
          showNotification('Liste Excel complète exportée : ' + enriched.length + ' élèves.', 'success');
        }).catch(error => {
          console.error('Export Excel élèves:', error);
          showNotification('Erreur pendant la préparation de la liste Excel.', 'danger');
        });
      }

      async function exportSecretaryStudentsByClassPDF() {
        const data = getSecretarySelectedClassExportData();
        if (!data) return;
        if (typeof html2pdf !== 'function') {
          showNotification('La bibliothèque PDF n’est pas disponible.', 'danger');
          return;
        }
        const esc = value => String(value ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
        try {
          const enriched = await secretaryGetEnrichedClassStudents(data);
          // Tableau volontairement structuré en champs scolaires/famille plutôt qu’en centaines de clés Firebase.
          const fields = [
            ['N°', s => ''], ['Nom complet de l’élève', s => s.name || s.fullName || s.nomPrenom || s.nomComplet || s.studentName || [s.firstName || s.prenom, s.lastName || s.nom].filter(Boolean).join(' ')],
            ['Sexe', s => s.gender || s.sexe || s.sex || ''], ['Date de naissance', s => s.dateNaissance || s.birthDate || s.dateOfBirth || s.dob || ''],
            ['Classe', s => s.class || s.classe || s.className || s.niveau || s.level || data.className], ['Branche', s => s.branch || s.branche || data.branchName],
            ['Année scolaire', s => s.academicYear || s.anneeAcademique || s.schoolYear || data.academicYear],
            ['Référence inscription', s => s.reference || s.codeUnique || s.registrationReference || s.referenceInscription || ''],
            ['École précédente', s => s.ecoleProvenance || s.previousSchool || s.previous_school || ''],
            ['Transport scolaire', s => s.transport || s.usesTransport || ''],
            ['Nom du parent / chef de famille', s => s.chefNomPrenom || s.chefDeFamille || s.parentName || s.parentFullName || s.parent || ''],
            ['Téléphone du parent', s => s.chefTelephone || s.parentPhone || s.parentTelephone || s.phoneParent || s.telephoneParent || ''],
            ['Pièce d’identité du parent', s => s.chefPieceIdentite || s.parentID || s.parentIdNumber || ''],
            ['Nom du conjoint', s => s.conjointNomPrenom || s.spouseName || s.conjoint || ''],
            ['Téléphone du conjoint', s => s.conjointTelephone || s.spousePhone || s.telephoneConjoint || ''],
            ['Pièce d’identité du conjoint', s => s.conjointPieceIdentite || s.spouseID || s.conjointID || ''],
            ['Ville', s => s.ville || s.city || ''], ['Province', s => s.province || ''], ['District', s => s.district || ''],
            ['Secteur', s => s.secteur || s.sector || ''], ['Cellule', s => s.akagari || s.cellule || ''],
            ['Village', s => s.umudugudu || s.village || ''], ['Umurenge', s => s.umurenge || s.sector || ''],
            ['Email du parent', s => s.parentEmail || s.emailParent || s.parent_email || ''],
            ['Statut de l’élève', s => s.status || s.statut || s.studentStatus || ''],
            ['Décision / promotion', s => s.decision || s.promotionStatus || s.deliberationStatus || s.promotionDecision || '']
          ];
          const dynamicKeys = [...new Set(enriched.flatMap(s => Object.keys(secretaryStudentCleanRecord(s))))];
          const importantKey = /name|nom|prenom|firstName|lastName|birth|naissance|gender|sexe|class|classe|branch|branche|parent|chef|conjoint|spouse|telephone|phone|email|ville|city|province|district|secteur|sector|akagari|cellule|umudugudu|village|umurenge|transport|school|ecole|reference|codeUnique|statut|status|decision|promotion|address|adresse|family|famille|idNumber|pieceIdentite/i;
          const extraFields = dynamicKeys.filter(k => importantKey.test(k) && !fields.some(f => f[0] === k)).map(k => [k, s => secretaryStudentCleanRecord(s)[k] || '']);
          const allFields = fields.concat(extraFields);
          const widths = [5, 19, 7, 11, 9, 9, 10, 13, 13, 9, 17, 12, 13, 14, 12, 13, 9, 9, 9, 9, 9, 10, 9, 15, 10, 13];
          const header = allFields.map(([label], i) => '<th style="width:' + (widths[i] || 12) + 'mm">' + esc(label) + '</th>').join('');
          const rows = enriched.map((student, idx) => '<tr>' + allFields.map(([label, getter], col) => {
            const raw = label === 'N°' ? idx + 1 : getter(student);
            return '<td>' + esc(secretaryStudentDisplayValue(raw)) + '</td>';
          }).join('') + '</tr>').join('');
          const container = document.createElement('div');
          container.style.cssText = 'position:fixed;left:-20000px;top:0;width:277mm;background:#fff;color:#172033;padding:0;font-family:Arial,sans-serif;box-sizing:border-box;';
          container.innerHTML =
            '<div style="text-align:center;border-bottom:3px solid #174a36;padding:0 0 5mm;margin-bottom:4mm;">' +
              '<div style="font-size:20px;font-weight:800;letter-spacing:1px;color:#174a36;">EDEN FAMILY SCHOOL</div>' +
              '<div style="font-size:14px;font-weight:700;margin-top:2mm;">LISTE DES ÉLÈVES — DOSSIERS COMPLETS</div>' +
              '<div style="font-size:10px;margin-top:2mm;">Classe : <b>' + esc(data.className) + '</b> &nbsp; | &nbsp; Branche : <b>' + esc(data.branchName || 'Toutes') + '</b> &nbsp; | &nbsp; Année : <b>' + esc(data.academicYear || '—') + '</b></div>' +
              '<div style="font-size:9px;margin-top:1mm;">Date d’édition : ' + esc(new Date().toLocaleDateString('fr-FR')) + ' &nbsp; | &nbsp; Effectif : ' + enriched.length + ' élèves</div>' +
            '</div>' +
            '<table style="width:100%;border-collapse:collapse;table-layout:fixed;font-size:6.5px;line-height:1.25;">' +
              '<thead style="display:table-header-group;"><tr>' + header + '</tr></thead><tbody>' + rows + '</tbody></table>' +
            '<div style="font-size:9px;text-align:right;margin-top:3mm;color:#475569;">Total : <b>' + enriched.length + ' élève(s)</b></div>';
          container.querySelectorAll('th').forEach(th => {
            th.style.background = '#174a36'; th.style.color = '#fff'; th.style.fontWeight = '700';
            th.style.border = '1px solid #d0d9d4'; th.style.padding = '5px 3px'; th.style.textAlign = 'center';
            th.style.verticalAlign = 'middle'; th.style.overflowWrap = 'anywhere'; th.style.wordBreak = 'break-word';
          });
          container.querySelectorAll('td').forEach(td => {
            td.style.border = '1px solid #d6ddd9'; td.style.padding = '4px 3px'; td.style.verticalAlign = 'top';
            td.style.overflowWrap = 'anywhere'; td.style.wordBreak = 'break-word';
          });
          container.querySelectorAll('tbody tr').forEach((tr, i) => {
            if (i % 2) tr.style.background = '#f1f6f3';
            tr.style.pageBreakInside = 'avoid';
          });
          document.body.appendChild(container);
          try {
            await html2pdf().set({
              margin: [7, 7, 8, 7],
              filename: 'Liste_Eleves_' + data.className.replace(/[^a-z0-9_-]+/gi, '_') + '.pdf',
              image: { type: 'jpeg', quality: 0.98 },
              html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff', logging: false, windowWidth: 1800 },
              jsPDF: { unit: 'mm', format: 'a3', orientation: 'landscape', compress: true },
              pagebreak: { mode: ['css', 'legacy'], avoid: ['tr'] }
            }).from(container).save();
          } finally { container.remove(); }
          showNotification('PDF bien mis en page : ' + enriched.length + ' élèves.', 'success');
        } catch (error) {
          console.error('Erreur export PDF classe:', error);
          showNotification('Erreur lors de la génération du PDF : ' + (error.message || error), 'danger');
        }
      }

'''
s=s[:start]+new+s[end:]
p.write_text(s,encoding='utf-8')
print('Updated',p)
print('Backup',backup)
print('PDF function count',s.count('async function exportSecretaryStudentsByClassPDF()'))
