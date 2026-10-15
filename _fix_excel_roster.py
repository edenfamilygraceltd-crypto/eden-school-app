from pathlib import Path
p = Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s = p.read_text(encoding='utf-8')
old = """        if (Array.isArray(filteredStudents)) {
          filteredStudents.forEach(s => {
            if (matchesBranch(s) && !rows.some(r => String(r.id || '') === String(s.id || '') && s.id)) rows.push(s);
          });
        }"""
new = """        if (Array.isArray(filteredStudents)) {
          // Ne jamais ajouter les résultats d'autres classes : la branche seule ne suffit pas.
          filteredStudents.forEach(s => {
            if (matchesClass(s) && matchesBranch(s) &&
                !rows.some(r => (s.id && String(r.id || '') === String(s.id)) ||
                  (norm(r.reference || r.codeUnique || r.registrationReference || '') &&
                   norm(r.reference || r.codeUnique || r.registrationReference || '') ===
                   norm(s.reference || s.codeUnique || s.registrationReference || '')))) rows.push(s);
          });
        }"""
if old not in s:
    raise SystemExit('filteredStudents block not found')
s = s.replace(old, new, 1)
needle = """          (Array.isArray(studentsData) ? studentsData : []).forEach(s => {
            if (classMatches(s) && branchMatches(s)) addCandidate(s, 'students');
          });"""
replacement = """          // La liste rows provient du filtre de la classe sélectionnée. La prendre comme
          // source prioritaire évite le faux message demandant de vérifier les inscriptions.
          (Array.isArray(data.rows) ? data.rows : []).forEach(s => addCandidate(s, 'data'));

          (Array.isArray(studentsData) ? studentsData : []).forEach(s => {
            if (classMatches(s) && branchMatches(s)) addCandidate(s, 'students');
          });"""
if needle not in s:
    raise SystemExit('studentsData candidates block not found')
s = s.replace(needle, replacement, 1)
old2 = """          (Array.isArray(data.rows) ? data.rows : []).forEach(s => {
            if (classMatches(s) && branchMatches(s)) addCandidate(s, 'data');
          });

"""
if old2 not in s:
    raise SystemExit('later data.rows block not found')
s = s.replace(old2, '', 1)
# Fix over-escaped regex in the Excel exporter only.
s = s.replace("replace(/[\\\\\\\\u0300-\\\\\\\\u036f]/g, '').trim().toLowerCase().replace(/\\\\\\\\s+/g, ' ');", "replace(/[\\\\u0300-\\\\u036f]/g, '').trim().toLowerCase().replace(/\\\\s+/g, ' ');", 1)
p.write_text(s, encoding='utf-8')
print('Patched class roster export: selected rows prioritized; filteredStudents now must match class.')
