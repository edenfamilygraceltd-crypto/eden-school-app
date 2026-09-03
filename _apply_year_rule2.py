from pathlib import Path
import re
p=Path('comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
pat=r'function getCurrentAcademicYearLabel\(\)\s*\{.*?\n\s*\}'
new="""function getAcademicYearForEnrollmentDate(dateValue) {
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
t,n=re.subn(pat,new,t,count=1,flags=re.S)
if n!=1: raise SystemExit('year function regex failed')
t=t.replace('<option value="2026-2027">2026-2027</option>\n                                    </select>','<option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>',1)
t=t.replace('const currentAcademicYear = getCurrentAcademicYearLabel();','const currentAcademicYear = getAcademicYearForEnrollmentDate(insc.dateInscription || insc.timestamp || new Date());',1)
t=t.replace("const academicYear = document.getElementById('confirmAcademicYear').value;","const academicYear = getAcademicYearForEnrollmentDate(insc.dateInscription || insc.timestamp || new Date());\n                const academicYearSelect = document.getElementById('confirmAcademicYear');\n                if (academicYearSelect) academicYearSelect.value = academicYear;",1)
# add 2027 option to confirm select if not already added
needle='<option value="2026-2027">2026-2027</option>\n                                    </select>'
if needle in t: t=t.replace(needle,'<option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>',1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('updated',n,len(t))