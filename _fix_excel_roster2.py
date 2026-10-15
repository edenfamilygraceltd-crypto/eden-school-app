from pathlib import Path
p = Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s = p.read_text(encoding='utf-8')
old = """        const source = Array.isArray(studentsData) ? studentsData : [];
        let rows = source.filter(s => matchesClass(s) && matchesBranch(s));
        if (Array.isArray(filteredStudents)) {
          // Ne jamais ajouter les résultats d'autres classes : la branche seule ne suffit pas.
          filteredStudents.forEach(s => {
            if (matchesClass(s) && matchesBranch(s) &&
                !rows.some(r => (s.id && String(r.id || '') === String(s.id)) ||
                  (norm(r.reference || r.codeUnique || r.registrationReference || '') &&
                   norm(r.reference || r.codeUnique || r.registrationReference || '') ===
                   norm(s.reference || s.codeUnique || s.registrationReference || '')))) rows.push(s);
          });
        }"""
new = """        // Recalculer le filtre avant l'export : filteredStudents est la même liste que
        // celle affichée dans le tableau pour la classe choisie (profils + inscriptions).
        if (typeof filterStudents === 'function') {
          try { filterStudents(); } catch (e) { console.warn('Actualisation du filtre avant export:', e); }
        }
        const source = Array.isArray(studentsData) ? studentsData : [];
        const visibleRoster = Array.isArray(filteredStudents) ? filteredStudents : [];
        let rows = visibleRoster.filter(s => matchesBranch(s));
        // Secours si la liste affichée n'est pas encore prête : utiliser les profils
        // correspondant à la classe, puis compléter depuis les résultats affichés.
        if (!rows.length) rows = source.filter(s => matchesClass(s) && matchesBranch(s));
        if (Array.isArray(visibleRoster)) {
          visibleRoster.forEach(s => {
            if (matchesBranch(s) && !rows.some(r => (s.id && String(r.id || '') === String(s.id)) ||
                (norm(r.reference || r.codeUnique || r.registrationReference || '') &&
                 norm(r.reference || r.codeUnique || r.registrationReference || '') ===
                 norm(s.reference || s.codeUnique || s.registrationReference || '')))) rows.push(s);
          });
        }"""
if old not in s:
    raise SystemExit('Expected roster source block not found')
s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')
print('Export now refreshes and uses the filtered class roster as its primary source.')
