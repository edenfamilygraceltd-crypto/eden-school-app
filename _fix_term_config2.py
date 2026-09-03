from pathlib import Path
import re
p=Path('comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
pat=r'(currentTermConfig\s*=\s*snapshot\.val\(\);\s*// Populate the form\s*)document\.getElementById\(\x27termAcademicYear\x27\)\.value\s*=\s*currentTermConfig\.academicYear\s*\|\|\s*\x27\x27;'
rep=r"\1const calculatedAcademicYear = getAcademicYearForEnrollmentDate(new Date());\n                        currentTermConfig.academicYear = calculatedAcademicYear;\n                        document.getElementById('termAcademicYear').value = calculatedAcademicYear;"
t,n=re.subn(pat,rep,t,count=1,flags=re.S)
if n!=1: raise SystemExit('regex not found')
t=t.replace("academicYear: '2025-2026',","academicYear: getAcademicYearForEnrollmentDate(new Date()),",1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('updated term config',n)