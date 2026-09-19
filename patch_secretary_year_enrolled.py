from pathlib import Path
P=Path(r"C:\\Users\\graph\\Documents\\directeur portaille\\secretary.html")
s=P.read_text(encoding="utf-8",errors="replace")
backup=P.with_name("secretary_backup_before_enrolled_year_fix_20260918.html")
backup.write_text(s,encoding="utf-8")
print("backup created")
old="""      function isInscriptionApprovedForSecretary(i){
        const a=(i?.statut||'').toString().trim().toLowerCase(),b=(i?.status||'').toString().trim().toLowerCase();
        return a==='confirmé'||a==='confirme'||a==='approuvé'||a==='approuve'||b==='approuve'||b==='approved'||b==='confirmed';
      }"""
new="""      function isInscriptionApprovedForSecretary(i){
        const a=(i?.statut||'').toString().trim().toLowerCase(),b=(i?.status||'').toString().trim().toLowerCase();
        if(['confirmé','confirme','approuvé','approuve','approved','confirmed'].includes(a)||['approuve','approved','confirmed'].includes(b)) return true;
        const st=getApprovedStudentProfile(i);
        const linkedFromAccountant=!!(st && i && st.fromInscriptionRequest===i.id);
        const activeStudent=!!(st && ['active','actif'].includes(String(st.status||'').trim().toLowerCase()));
        const assignedClass=!!(st && (st.assignedClass||st.classAssigned||st.classeAffectee||st.classeAffectée||st.class));
        return linkedFromAccountant || (activeStudent && assignedClass && !!i?.studentId);
      }"""
s=s.replace(old,new,1)

insert_at=s.index("      function renderEnrolledStudents(){")
helpers="""      function secretaryAcademicYearFromDate(value){
        const d=value instanceof Date?new Date(value.getTime()):new Date(value||'');
        if(Number.isNaN(d.getTime())) return '';
        const y=d.getFullYear(), start=d.getMonth()>=7?y:y-1;
        return String(start)+'-'+String(start+1);
      }
      function secretaryRecordAcademicYear(record){
        const raw=record?.academicYear||record?.anneeAcademique||record?.anneeScolaire||record?.schoolYear||record?.year||'';
        const normalized=normalizeSecretarySchoolYear(raw);
        return normalized||secretaryAcademicYearFromDate(record?.date||record?.dueDate||record?.paymentDate||record?.dateCreated||record?.createdAt||record?.timestamp);
      }
      function secretaryRecordMatchesActiveYear(record){
        return secretaryRecordAcademicYear(record)===getActiveSecretarySchoolYear();
      }

"""
if "function secretaryRecordMatchesActiveYear(record)" not in s:
    s=s[:insert_at]+helpers+s[insert_at:]

s=s.replace("        const yearFilter=document.getElementById('enrolledAcademicYearFilter')?.value||'';","        const yearFilter=document.getElementById('enrolledAcademicYearFilter')?.value||getActiveSecretarySchoolYear();",1)
old="""                <button class="btn btn-secondary btn-sm" onclick="renderEnrolledStudents()">
                  <i class="bi bi-arrow-clockwise me-1"></i>Rafraîchir
                </button>"""
new="""                <button class="btn btn-secondary btn-sm me-2" onclick="renderEnrolledStudents()">
                  <i class="bi bi-arrow-clockwise me-1"></i>Rafraîchir
                </button>
                <button class="btn btn-success btn-sm" onclick="exportSecretaryEnrolledStudents()">
                  <i class="bi bi-file-earmark-spreadsheet me-1"></i>Exporter la liste
                </button>"""
s=s.replace(old,new,1)

export_fn="""      function exportSecretaryEnrolledStudents(){
        const table=document.getElementById('enrolledStudentsTable');
        if(!table){showNotification('Liste des élèves inscrits introuvable.','warning');return;}
        const rows=[...table.querySelectorAll('tr')].map(tr=>[...tr.querySelectorAll('td')].map(c=>c.innerText.replace(/\s+/g,' ').trim())).filter(r=>r.length&&r[0]&&!r[0].includes('Aucune inscription'));
        if(!rows.length){showNotification('Aucun élève inscrit à exporter.','warning');return;}
        const clean=rows.map(r=>r.slice(0,7));
        const header=['Nom de l’élève','Branche','Classe','Parent','Année académique','Statut','Autorisé par'];
        const csv='\ufeff'+[header,...clean].map(row=>row.map(v=>'"'+String(v||'').replace(/"/g,'""')+'"').join(';')).join('\r\n');
        const blob=new Blob([csv],{type:'text/csv;charset=utf-8;'});
        const url=URL.createObjectURL(blob),a=document.createElement('a');
        const year=getActiveSecretarySchoolYear();
        a.href=url;a.download='Eleves_Inscrits_'+year+'.csv';document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(url);
        showNotification(String(clean.length)+' élève(s) exporté(s) pour '+year+'.','success');
      }

"""
s=s.replace("      function onEnrollmentBranchChange",export_fn+"      function onEnrollmentBranchChange",1)
s=s.replace("        assignmentsData.forEach(a => {","        assignmentsData.filter(secretaryRecordMatchesActiveYear).forEach(a => {",1)
s=s.replace("        testsData.forEach(t => {","        testsData.filter(secretaryRecordMatchesActiveYear).forEach(t => {",1)
s=s.replace("        examsData.forEach(e => {","        examsData.filter(secretaryRecordMatchesActiveYear).forEach(e => {",1)
s=s.replace("        vacationWorksData.forEach(v => {","        vacationWorksData.filter(secretaryRecordMatchesActiveYear).forEach(v => {",1)

old="      function normalizeSecretarySchoolYear(v){const m=String(v||'').trim().replace(/[â€“â€”\\/]/g,'-').match(/(\\d{4})\\s*-\\s*(\\d{2,4})/);if(!m)return '';const a=Number(m[1]);const b=m[2].length===2?Number(String(a).slice(0,2)+m[2]):Number(m[2]);return a+'-'+b;}"
new="      function normalizeSecretarySchoolYear(v){const raw=String(v||'').trim();const m=raw.match(/(\\d{4})\\D+(\\d{2,4})/);if(!m)return '';const a=Number(m[1]);const b=m[2].length===2?Number(String(a).slice(0,2)+m[2]):Number(m[2]);return a+'-'+b;}"
s=s.replace(old,new,1)

repls={
"AnnÃ©e scolaire":"Année scolaire",
"Gestion de lâ€™annÃ©e scolaire":"Gestion de l’année scolaire",
"Les données dâ€™une ancienne année restent":"Les données d’une ancienne année restent",
"Nouvelle annÃ©e scolaire":"Nouvelle année scolaire",
"Commencer une nouvelle annÃ©e":"Commencer une nouvelle année",
"SÃ©lectionnez une annÃ©e scolaire valide.":"Sélectionnez une année scolaire valide.",
"Cette annÃ©e est dÃ©jÃ  active.":"Cette année est déjà active.",
"Commencer lâ€™annÃ©e scolaire":"Commencer l’année scolaire",
"activÃ©e":"activée",
"lâ€™annÃ©e scolaire":"l’année scolaire"
}
for a,b in repls.items():
    s=s.replace(a,b)
s=s.replace("            loadAllData();\n            loadMessages();","            loadActiveSecretarySchoolYear().then(() => loadAllData());\n            loadMessages();",2)

old="          if(currentUser?.uid&&typeof loadStudents==='function')loadStudents(currentUser.uid);"
new="          if(currentUser?.uid&&typeof loadStudents==='function')loadStudents(currentUser.uid);\n          renderAssignments();renderTests();renderExams();renderVacationWorks();\n          populateEnrolledBranchFilter();renderEnrolledStudents();"
s=s.replace(old,new,1)

P.write_text(s,encoding="utf-8")
print("PATCH SCRIPT READY",len(s))
# Stamp the active school year on newly created academic records.
old="          fileUrl: fileUrl,\n          updatedAt: new Date().toISOString()"
new="          fileUrl: fileUrl,\n          academicYear: getActiveSecretarySchoolYear(),\n          updatedAt: new Date().toISOString()"
s=s.replace(old,new,1)

old="          fileUrl: fileUrl,\n          createdAt: new Date().toISOString(),\n          createdBy: currentUser.email"
new="          fileUrl: fileUrl,\n          academicYear: getActiveSecretarySchoolYear(),\n          createdAt: new Date().toISOString(),\n          createdBy: currentUser.email"
s=s.replace(old,new,1)
s=s.replace(old,new,1)

old="          fileUrl: fileUrl,\n          createdAt: new Date().toISOString(),\n          createdBy: currentUser.email"
s=s.replace(old,new,1)

P.write_text(s,encoding="utf-8")
print("academic creation year fields patched")
