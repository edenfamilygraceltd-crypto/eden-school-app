from pathlib import Path
import re
p=Path('comptable.html')
s=p.read_text(encoding='utf-8')
old=re.compile(r"\s*const yearPrompt = sortedYears\.map\(\(year, index\) => `\$\{index \+ 1\}\. \$\{year\}`\)\.join\('\\n'\);.*?\s*const selectedAcademicYear = sortedYears\[yearIndex\];\s*const selectedYearKey = normalizeYear\(selectedAcademicYear\);",re.S)
new="""
                    const selectedAcademicYear = await showExportAcademicYearModal(sortedYears);
                    if (!selectedAcademicYear) return;
                    const selectedYearKey = normalizeYear(selectedAcademicYear);"""
if not old.search(s): raise SystemExit('selection block not found')
s=old.sub(new,s,count=1)
marker='            async function exportRegistrationReportPDF() {'
helper=r'''            function showExportAcademicYearModal(years) {
                return new Promise(resolve => {
                    const existing = document.getElementById('exportAcademicYearModal');
                    if (existing) existing.remove();
                    const options = ['ALL', ...years];
                    const cards = options.map((year, i) => {
                        const label = year === 'ALL' ? 'Toutes les années scolaires' : year;
                        const active = i === 1 || (i === 0 && years.length === 0) ? ' active' : '';
                        return `<button type="button" class="export-year-option${active}" data-year="${year}"><span>${year === 'ALL' ? '📚' : '🎓'}</span><strong>${label}</strong><small>${year === 'ALL' ? 'Tous les élèves disponibles' : 'Élèves inscrits pour cette année'}</small></button>`;
                    }).join('');
                    const modalHtml = `<div class="modal fade" id="exportAcademicYearModal" tabindex="-1"><div class="modal-dialog modal-dialog-centered"><div class="modal-content export-year-modal-content"><div class="modal-header"><div><div class="export-year-kicker">EXPORT REPORT (PDF)</div><h5 class="modal-title">Sélection de l’année scolaire</h5><p class="export-year-subtitle">Choisissez les élèves à inclure dans le rapport PDF.</p></div><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body"><div class="export-year-grid">${cards}</div></div><div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Annuler</button><button type="button" class="btn btn-primary" id="confirmExportAcademicYear" disabled>Exporter le rapport</button></div></div></div></div>`;
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    const el=document.getElementById('exportAcademicYearModal'), confirm=document.getElementById('confirmExportAcademicYear'); let selected=null;
                    el.querySelectorAll('.export-year-option').forEach(btn=>btn.addEventListener('click',()=>{el.querySelectorAll('.export-year-option').forEach(b=>b.classList.remove('active'));btn.classList.add('active');selected=btn.dataset.year==='ALL'?(years[0]||'ALL'):btn.dataset.year;confirm.disabled=false;}));
                    el.addEventListener('hidden.bs.modal',()=>{el.remove();resolve(null)},{once:true});
                    confirm.addEventListener('click',()=>{bootstrap.Modal.getInstance(el).hide();resolve(selected)},{once:true});
                    new bootstrap.Modal(el).show();
                });
            }

'''
s=s.replace(marker,helper+marker,1)
style='''\n<style id="export-year-modal-style">.export-year-modal-content{border:0;border-radius:20px;box-shadow:0 18px 60px rgba(0,0,0,.22);overflow:hidden}.export-year-modal-content .modal-header{padding:24px 26px 12px;border:0}.export-year-kicker{font-size:11px;font-weight:800;letter-spacing:1.5px;color:#2563eb;margin-bottom:5px}.export-year-modal-content .modal-title{font-weight:800;font-size:21px}.export-year-subtitle{margin:4px 0 0;color:#64748b;font-size:13px}.export-year-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.export-year-option{border:1px solid #e2e8f0;background:#fff;border-radius:14px;padding:16px;text-align:left;display:flex;flex-direction:column;gap:5px;transition:.18s;cursor:pointer}.export-year-option:hover,.export-year-option.active{border-color:#2563eb;box-shadow:0 5px 18px rgba(37,99,235,.13);transform:translateY(-1px)}.export-year-option span{font-size:22px}.export-year-option strong{font-size:14px;color:#0f172a}.export-year-option small{font-size:11px;color:#64748b}.export-year-modal-content .modal-footer{border:0;padding:14px 26px 22px}@media(max-width:576px){.export-year-grid{grid-template-columns:1fr}}</style>\n'''
if 'id="export-year-modal-style"' not in s: s=s.replace('</head>',style+'</head>',1)
p.write_text(s,encoding='utf-8')
print('redesigned export year modal')
