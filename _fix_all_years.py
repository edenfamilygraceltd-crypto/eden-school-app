from pathlib import Path
p=Path('comptable.html')
s=p.read_text(encoding='utf-8')
s=s.replace("if (!selectedAcademicYear) return;\n                    const selectedYearKey", "if (!selectedAcademicYear) return;\n                    const exportAllYears = selectedAcademicYear === 'ALL';\n                    const selectedYearKey")
s=s.replace("if (recordYear && normalizeYear(recordYear) !== selectedYearKey) return;", "if (!exportAllYears && recordYear && normalizeYear(recordYear) !== selectedYearKey) return;")
s=s.replace("Année scolaire : ${selectedAcademicYear}", "Année scolaire : ${exportAllYears ? 'Toutes les années' : selectedAcademicYear}")
s=s.replace("const safeYear = selectedAcademicYear.replace(/[^0-9-]/g, '_');", "const safeYear = (exportAllYears ? 'toutes_les_annees' : selectedAcademicYear).replace(/[^a-z0-9_-]/gi, '_');")
p.write_text(s,encoding='utf-8')
print('fixed all years')
