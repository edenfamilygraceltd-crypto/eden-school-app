from pathlib import Path
p=Path(r"C:\Users\graph\Documents\directeur portaille\secretary.html")
s=p.read_text(encoding="utf-8",errors="replace")
backup=p.with_name("secretary_backup_before_final_year_cleanup_20260918.html")
backup.write_text(s,encoding="utf-8")
repls={
"Les donnÃ©es d’une ancienne annÃ©e":"Les données d’une ancienne année",
"seront conservÃ©es":"seront conservées",
"Erreur changement annÃ©e scolaire":"Erreur changement année scolaire",
"Les donnÃ©es de ":"Les données de ",
"conservÃ©es":"conservées",
}
for a,b in repls.items():
    s=s.replace(a,b)
old="""          renderAssignments();renderTests();renderExams();renderVacationWorks();
          populateEnrolledBranchFilter();renderEnrolledStudents();
          renderAssignments();renderTests();renderExams();renderVacationWorks();
          populateEnrolledBranchFilter();renderEnrolledStudents();"""
new="""          renderAssignments();renderTests();renderExams();renderVacationWorks();
          populateEnrolledBranchFilter();renderEnrolledStudents();"""
s=s.replace(old,new,1)

old="""        if (assignmentsData.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-inbox"></i>
            <p>Aucun devoir assigné</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        assignmentsData.filter(secretaryRecordMatchesActiveYear).forEach(a => {"""
new="""        const visibleAssignments = assignmentsData.filter(secretaryRecordMatchesActiveYear);
        if (visibleAssignments.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-inbox"></i>
            <p>Aucun devoir assigné pour l'année scolaire active</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        visibleAssignments.forEach(a => {"""
s=s.replace(old,new,1)
old="""        if (testsData.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-file-text"></i>
            <p>Aucun test</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        testsData.filter(secretaryRecordMatchesActiveYear).forEach(t => {"""
new="""        const visibleTests = testsData.filter(secretaryRecordMatchesActiveYear);
        if (visibleTests.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-file-text"></i>
            <p>Aucun test pour l'année scolaire active</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        visibleTests.forEach(t => {"""
s=s.replace(old,new,1)

old="""        if (examsData.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-pen"></i>
            <p>Aucun examen</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        examsData.filter(secretaryRecordMatchesActiveYear).forEach(e => {"""
new="""        const visibleExams = examsData.filter(secretaryRecordMatchesActiveYear);
        if (visibleExams.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-pen"></i>
            <p>Aucun examen pour l'année scolaire active</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        visibleExams.forEach(e => {"""
s=s.replace(old,new,1)
old="""        if (vacationWorksData.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-sun"></i>
            <p>Aucun travail de vacances</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        vacationWorksData.filter(secretaryRecordMatchesActiveYear).forEach(v => {"""
new="""        const visibleVacationWorks = vacationWorksData.filter(secretaryRecordMatchesActiveYear);
        if (visibleVacationWorks.length === 0) {
          container.innerHTML = `<div class="academic-empty-state">
            <i class="bi bi-sun"></i>
            <p>Aucun travail de vacances pour l'année scolaire active</p>
          </div>`;
          return;
        }
        let html = '<div class="academic-cards-grid">';
        visibleVacationWorks.forEach(v => {"""
s=s.replace(old,new,1)

p.write_text(s,encoding="utf-8")
print("FINAL CLEANUP APPLIED",len(s), "exportCount",s.count("function exportSecretaryEnrolledStudents"))
