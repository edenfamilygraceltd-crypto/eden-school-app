from pathlib import Path
p=Path('comptable.html')
with open(p,encoding='utf-8',newline='') as f: t=f.read()
bad="""            function getCurrentAcademicYearLabel() {
                return getAcademicYearForEnrollmentDate(new Date());
            }
                const now = new Date();
                const year = now.getFullYear();
                const month = now.getMonth() + 1;
                return month >= 9 ? `${year}-${year + 1}` : `${year - 1}-${year}`;
            }"""
good="""            function getCurrentAcademicYearLabel() {
                return getAcademicYearForEnrollmentDate(new Date());
            }"""
if bad not in t: raise SystemExit('malformed block not found')
t=t.replace(bad,good,1)
# Add 2027-2028 to Term Configuration and confirmation selects if absent.
term='<option value="2026-2027">2026-2027</option>\n                                    </select>'
if term in t: t=t.replace(term,'<option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>',1)
confirm='<option value="2026-2027">2026-2027</option>\n                                    </select>'
if confirm in t: t=t.replace(confirm,'<option value="2026-2027">2026-2027</option>\n                                        <option value="2027-2028">2027-2028</option>\n                                    </select>',1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(t)
print('fixed')