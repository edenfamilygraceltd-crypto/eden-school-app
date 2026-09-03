from pathlib import Path
import re
p=Path(r'C:\Users\graph\Documents\directeur portaille\comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
helper="""            // Academic year is determined by the real enrollment date.
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

            function getCurrentAcademicYearLabel() {
                return getAcademicYearForEnrollmentDate(new Date());
            }"""
pat=r'            function getCurrentAcademicYearLabel\(\) \{.*?            \}'
t,n=re.subn(pat,helper,t,count=1,flags=re.S)
if n!=1: raise SystemExit('current year function regex not found')
# Add 2027-2028 to first year select (Term Configuration).
old='                                        <option value="2026-2027">2026-2027</option>\n                                    </select>'
new='                                        <option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>'
if old not in t: raise SystemExit('term option not found')
t=t.replace(old,new,1)
# Add 2027-2028 to confirmation modal.
needle='                                        <option value="2026-2027">2026-2027</option>\n                                    </select>\n                                </div>\n                                <div class="col-md-6">\n                                    <label class="form-label">Classe affect'
if needle not in t: raise SystemExit('confirm option not found')
t=t.replace(needle,needle.replace('2026-2027</option>\n','2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n',1),1)
oldline="                const academicYear = document.getElementById('confirmAcademicYear').value;"
newline="""                const academicYear = getAcademicYearForEnrollmentDate(insc.dateInscription || insc.timestamp || new Date());
                const academicYearSelect = document.getElementById('confirmAcademicYear');
                if (academicYearSelect) academicYearSelect.value = academicYear;"""
if oldline not in t: raise SystemExit('confirm save line not found')
t=t.replace(oldline,newline,1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('comptable updated',len(t))
