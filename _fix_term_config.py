from pathlib import Path
p=Path('comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
old="""                        currentTermConfig = snapshot.val();
                        // Populate the form
                        document.getElementById('termAcademicYear').value = currentTermConfig.academicYear || '';"""
new="""                        currentTermConfig = snapshot.val();
                        // Current Academic Year always follows the enrollment-date rule.
                        const calculatedAcademicYear = getAcademicYearForEnrollmentDate(new Date());
                        currentTermConfig.academicYear = calculatedAcademicYear;
                        // Populate the form
                        document.getElementById('termAcademicYear').value = calculatedAcademicYear;"""
if old not in t: raise SystemExit('load config block not found')
t=t.replace(old,new,1)
t=t.replace("""                        currentTermConfig = {
                            academicYear: '2025-2026',""","""                        currentTermConfig = {
                            academicYear: getAcademicYearForEnrollmentDate(new Date()),""",1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('term config updated')