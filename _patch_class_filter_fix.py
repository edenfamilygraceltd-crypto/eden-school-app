from pathlib import Path
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8')
old="""          // Only apply class filter if class filter is selected
          if (classFilter) {
            if (!s.classes || s.classes.length === 0) return false;
            if (!s.classes.includes(classFilter)) return false;
          }"""
new="""          // Comparer les classes sans tenir compte des majuscules, espaces ou du format
          // enregistré (texte, tableau, nom de classe ou identifiant Firebase).
          if (classFilter) {
            const wantedClass = normalizeKey(classFilter);
            const rawValues = [s.classes, s.class, s.classe, s.className, s.class_name, s.classId, s.classeId, s.assignedClass, s.classAssigned, s.classeAffectee];
            const studentClassKeys = new Set();
            const addStudentClassKey = value => {
              if (Array.isArray(value)) { value.forEach(addStudentClassKey); return; }
              if (value && typeof value === 'object') {
                addStudentClassKey(value.name || value.className || value.class || value.classe || value.id || '');
                return;
              }
              String(value == null ? '' : value).split(',').map(v => v.trim()).filter(Boolean).forEach(valuePart => {
                studentClassKeys.add(normalizeKey(valuePart));
                const classRecord = (Array.isArray(classesData) ? classesData : []).find(c =>
                  normalizeKey(c.id || '') === normalizeKey(valuePart) ||
                  normalizeKey(c.name || c.className || c.class || c.classe || '') === normalizeKey(valuePart)
                );
                if (classRecord) studentClassKeys.add(normalizeKey(classRecord.name || classRecord.className || classRecord.class || classRecord.classe || classRecord.id || ''));
              });
            };
            rawValues.forEach(addStudentClassKey);
            if (!studentClassKeys.has(wantedClass)) return false;
          }"""
assert old in s
s=s.replace(old,new,1)
# Re-populate classes whenever branch filter changes and immediately apply the selection.
old2="""        if (!restored) select.value = '';
        window.__secretaryPendingClassFilter = null;
      }"""
new2="""        if (!restored) select.value = '';
        window.__secretaryPendingClassFilter = null;
        // Le filtre doit fonctionner aussi lorsque les classes sont rechargées depuis Firebase.
        select.disabled = classes.length === 0;
        select.title = classes.length ? 'Choisir une classe pour filtrer les élèves' : 'Aucune classe disponible pour cette branche et cette année';
      }"""
assert old2 in s
s=s.replace(old2,new2,1)
p.write_text(s,encoding='utf-8')
print('Class filtering now compares normalized class names and resolves class IDs against classesData.')
