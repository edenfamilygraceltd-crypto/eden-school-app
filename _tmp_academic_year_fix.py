from pathlib import Path
p = Path(r'C:\Users\graph\Documents\directeur portaille')

def edit(fn, repls):
    f = p / fn
    with open(f, encoding='utf-8', newline='') as h:
        t = h.read()
    orig = t
    for old, new in repls:
        if old not in t:
            raise SystemExit(f'MISSING in {fn}: {old[:100]}')
        t = t.replace(old, new, 1)
    with open(f, 'w', encoding='utf-8', newline='') as h:
        h.write(t)
    print(fn, 'updated', len(orig), '->', len(t))

helper = """
      // Academic year is determined by the real enrollment date.
      // 09/09/2025-01/06/2026 => 2025-2026; 02/06/2026-25/07/2027 => 2026-2027; after 25/07/2027 => 2027-2028.
      function getAcademicYearForEnrollmentDate(dateValue) {
        const date = dateValue instanceof Date ? new Date(dateValue.getTime()) : new Date(dateValue || Date.now());
        if (Number.isNaN(date.getTime())) return '2026-2027';
        const ymd = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
        if (ymd < '2025-09-09') return '2024-2025';
        if (ymd <= '2026-06-01') return '2025-2026';
        if (ymd <= '2027-07-25') return '2026-2027';
        return '2027-2028';
      }

"""
edit('secretary.html', [
    ("      async function submitSecretaryInscription() {", helper + "      async function submitSecretaryInscription() {"),
    ("          anneeAcademique: document.getElementById('secretaryEnrollmentAcademicYear')?.value || `${new Date().getFullYear()}-${new Date().getFullYear()+1}`",
     "          anneeAcademique: getAcademicYearForEnrollmentDate(new Date())")
])

chelp = """
            // Academic year is determined by the real enrollment date.
            // 09/09/2025-01/06/2026 => 2025-2026; 02/06/2026-25/07/2027 => 2026-2027; after 25/07/2027 => 2027-2028.
            function getAcademicYearForEnrollmentDate(dateValue) {
                const date = dateValue instanceof Date ? new Date(dateValue.getTime()) : new Date(dateValue || Date.now());
                if (Number.isNaN(date.getTime())) return '2026-2027';
                const ymd = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
                if (ymd < '2025-09-09') return '2024-2025';
                if (ymd <= '2026-06-01') return '2025-2026';
                if (ymd <= '2027-07-25') return '2026-2027';
                return '2027-2028';
            }

"""
oldfunc = """            function getCurrentAcademicYearLabel() {
                if (currentTermConfig && currentTermConfig.academicYear) {
                    return currentTermConfig.academicYear;
                }
                const now = new Date();
                const year = now.getFullYear();
                const month = now.getMonth() + 1;
                return month >= 9 ? `${year}-${year + 1}` : `${year - 1}-${year}`;
            }"""
newfunc = chelp + """            function getCurrentAcademicYearLabel() {
                return getAcademicYearForEnrollmentDate(new Date());
            }"""
edit('comptable.html', [
    (oldfunc, newfunc),
    ('                                        <option value="2026-2027">2026-2027</option>\n                                    </select>',
     '                                        <option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>'),
    ('                                        <option value="2026-2027">2026-2027</option>\n                                    </select>\n                                </div>\n                                <div class="col-md-6">\n                                    <label class="form-label">Classe affect',
     '                                        <option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>\n                                </div>\n                                <div class="col-md-6">\n                                    <label class="form-label">Classe affect'),
    ("                const academicYear = document.getElementById('confirmAcademicYear').value;",
     "                const academicYear = getAcademicYearForEnrollmentDate(insc.dateInscription || insc.timestamp || new Date());\n                const academicYearSelect = document.getElementById('confirmAcademicYear');\n                if (academicYearSelect) academicYearSelect.value = academicYear;"),
])
(p / '_tmp_academic_year_fix.py').unlink()
