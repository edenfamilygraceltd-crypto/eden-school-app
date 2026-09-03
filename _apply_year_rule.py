from pathlib import Path
p=Path('comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
old="""            function getCurrentAcademicYearLabel() {
                if (currentTermConfig && currentTermConfig.academicYear) {
                    return currentTermConfig.academicYear;
                }
                const now = new Date();
                const year = now.getFullYear();
                const month = now.getMonth() + 1;
                return month >= 9 ? `${year}-${year + 1}` : `${year - 1}-${year}`;
            }"""
new="""            // Academic year is determined by the real enrollment date.
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
if old not in t: raise SystemExit('current year function not found')
t=t.replace(old,new,1)
old="""                                        <option value="2026-2027">2026-2027</option>
                                    </select>"""
new="""                                        <option value="2026-2027">2026-2027</option>
                                        <option value="2027-2028">2027-2028</option>
                                    </select>"""
if old not in t: raise SystemExit('term year select not found')
t=t.replace(old,new,1)
old="""                                        <option value="2026-2027">2026-2027</option>
                                    </select>
                                </div>
                                <div class="col-md-6">"""
new="""                                        <option value="2026-2027">2026-2027</option>
                                        <option value="2027-2028">2027-2028</option>
                                    </select>
                                </div>
                                <div class="col-md-6">"""
if old not in t: raise SystemExit('confirm year select not found')
t=t.replace(old,new,1)
old="""                const academicYear = document.getElementById('confirmAcademicYear').value;
                const assignedClass = document.getElementById('confirmAssignedClass').value;"""
new="""                // Never let the confirmation modal override the year calculated from the enrollment date.
                const academicYear = getAcademicYearForEnrollmentDate(insc.dateInscription || insc.timestamp || new Date());
                const academicYearSelect = document.getElementById('confirmAcademicYear');
                if (academicYearSelect) academicYearSelect.value = academicYear;
                const assignedClass = document.getElementById('confirmAssignedClass').value;"""
if old not in t: raise SystemExit('confirm save line not found')
t=t.replace(old,new,1)
# Also calculate the modal value from the request's enrollment date.
old="""                const currentAcademicYear = getCurrentAcademicYearLabel();
                const academicYearSelect = document.getElementById('confirmAcademicYear');"""
new="""                const currentAcademicYear = getAcademicYearForEnrollmentDate(insc.dateInscription || insc.timestamp || new Date());
                const academicYearSelect = document.getElementById('confirmAcademicYear');"""
if old not in t: raise SystemExit('modal year line not found')
t=t.replace(old,new,1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('updated comptable',len(t))