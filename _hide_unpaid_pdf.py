from pathlib import Path
p=Path(r'C:\Users\graph\Documents\directeur portaille\comptable.html')
with open(p,'r',encoding='utf-8',newline='') as f: s=f.read()
old="""                        const payment = paymentsByStudent[String(insc.studentId)] || null;\n                        const student = studentsById[insc.studentId] || {};"""
new="""                        const payment = paymentsByStudent[String(insc.studentId)] || null;\n                        // Le PDF doit afficher uniquement les élèves ayant payé.\n                        if (!payment) return;\n                        const student = studentsById[insc.studentId] || {};"""
if old not in s: raise SystemExit('target not found')
s=s.replace(old,new,1)
old2="""                            registrationStatus: insc.statut || insc.status || 'Enregistré',\n                            unpaid: !payment"""
new2="""                            registrationStatus: insc.statut || insc.status || 'Enregistré',\n                            unpaid: false"""
if old2 not in s: raise SystemExit('second target not found')
s=s.replace(old2,new2,1)
with open(p,'w',encoding='utf-8',newline='') as f: f.write(s)
print('PDF unpaid students hidden')
