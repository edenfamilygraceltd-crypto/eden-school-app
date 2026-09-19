from pathlib import Path
p=Path(r'C:\Users\graph\Documents\directeur portaille\secretary.html')
s=p.read_text(encoding='utf-8',errors='replace')
s=s.replace('Les données dâ€™une ancienne annÃ©e restent conservées dans Firebase, mais elles ne sont plus affichées dans la gestion courante.','Les données d’une ancienne année restent conservées dans Firebase, mais elles ne sont plus affichées dans la gestion courante.')
s=s.replace("          populateNewSchoolYearSelect();\n          if(currentUser?.uid&&typeof loadStudents==='function')loadStudents(currentUser.uid);", "          populateNewSchoolYearSelect();\n          if(currentUser?.uid&&typeof loadStudents==='function')loadStudents(currentUser.uid);\n          const enrolledYearEl=document.getElementById('enrolledAcademicYearFilter'); if(enrolledYearEl) enrolledYearEl.value='';", 1)
p.write_text(s,encoding='utf-8')
print('cleanup done', 'Les données d’une ancienne année' in s)
