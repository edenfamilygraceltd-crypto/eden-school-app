from pathlib import Path
p=Path('comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
start=t.index('            function getAcademicYearForEnrollmentDate(dateValue)')
marker='FUZZY DUPLICATE DETECTION HELPERS'
mi=t.index(marker,start)
end=t.rfind('// ============================================',start,mi)
block="""            function getAcademicYearForEnrollmentDate(dateValue) {
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
            }

            """
t=t[:start]+block+t[end:]
# Add 2027-2028 only if absent in each select.
for select_id in ['termAcademicYear','confirmAcademicYear']:
    idx=t.find('id="'+select_id+'"')
    if idx>=0:
        endsel=t.find('</select>',idx)
        seg=t[idx:endsel]
        if '2027-2028' not in seg:
            pos=seg.find('<option value="2026-2027">')
            if pos>=0:
                close=seg.find('</option>',pos)+len('</option>')
                seg=seg[:close]+'\n                                        <option value="2027-2028">2027-2028</option>'+seg[close:]
                t=t[:idx]+seg+t[endsel:]
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('fixed',len(t))