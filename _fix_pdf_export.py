import re
from pathlib import Path
p = Path(r'C:\Users\graph\Documents\directeur portaille\comptable.html')
s = p.read_text(encoding='utf-8')
start = s.index('            async function exportRegistrationReportPDF() {')
end = s.index('\n            function mapInscriptionBranchToInternal', start)
new = r'''            async function exportRegistrationReportPDF() {
                const JsPDF = resolveJsPDF();
                if (!JsPDF) { showToast('PDF library non chargée', 'error'); return; }

                try {
                    // Le PDF doit contenir les MÊMES élèves que le tableau "Paiements d'Inscription":
                    // inscriptions non rejetées (payées ou non) + anciens paiements sans inscription.
                    const [inscSnap, studentsSnap, paymentsSnap] = await Promise.all([
                        pendingInscriptionsRef.once('value'),
                        studentsRef.once('value'),
                        registrationPaymentsRef.once('value')
                    ]);

                    const studentsById = {};
                    studentsSnap.forEach(ch => { studentsById[ch.key] = { id: ch.key, ...ch.val() }; });

                    const paymentsByStudent = {};
                    const allPayments = [];
                    paymentsSnap.forEach(ch => {
                        const payment = { id: ch.key, ...ch.val() };
                        allPayments.push(payment);
                        if (payment.studentId) paymentsByStudent[String(payment.studentId)] = payment;
                    });

                    const normalizeReportValue = value => String(value ?? '')
                        .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
                        .trim().toLowerCase();
                    const isRejectedInscription = insc => {
                        const status = normalizeReportValue(insc.statut || insc.status);
                        return ['rejete', 'rejected'].includes(status);
                    };
                    const normalizeYear = value => normalizeReportValue(value).replace(/\s+/g, '');

                    // Construire les années disponibles à partir des inscriptions ET paiements.
                    const academicYears = new Set();
                    inscSnap.forEach(ch => {
                        const insc = ch.val() || {};
                        if (isRejectedInscription(insc)) return;
                        if (!inscriptionMatchesSelectedBranch(insc)) return;
                        const year = insc.anneeAcademique || insc.anneeScolaire || insc.academicYear;
                        if (year) academicYears.add(String(year).trim());
                    });
                    allPayments.forEach(payment => {
                        if (!registrationRecordMatchesBranch(payment.branch, payment.section, payment.studentClass, selectedBranch)) return;
                        if (payment.academicYear) academicYears.add(String(payment.academicYear).trim());
                    });
                    Object.values(studentsById).forEach(student => {
                        if (student.academicYear) academicYears.add(String(student.academicYear).trim());
                    });

                    const sortedYears = [...academicYears].filter(Boolean).sort((a, b) => b.localeCompare(a));
                    const currentYear = getCurrentAcademicYearLabel();
                    if (!sortedYears.includes(currentYear)) sortedYears.unshift(currentYear);
                    const yearPrompt = sortedYears.map((year, index) => `${index + 1}. ${year}`).join('\n');
                    const yearAnswer = window.prompt(
                        `Sélectionnez l'année académique à afficher dans le rapport :\n\n${yearPrompt}\n\nEntrez le numéro :`,
                        '1'
                    );
                    if (yearAnswer === null) return;
                    const yearIndex = Number.parseInt(yearAnswer, 10) - 1;
                    if (!Number.isInteger(yearIndex) || !sortedYears[yearIndex]) {
                        showToast('Année académique invalide.', 'warning');
                        return;
                    }
                    const selectedAcademicYear = sortedYears[yearIndex];
                    const selectedYearKey = normalizeYear(selectedAcademicYear);

                    const rows = [];
                    const seenStudentIds = new Set();

                    // 1) Toutes les inscriptions non rejetées, même si elles sont encore en attente
                    // et même si aucun paiement n'existe.
                    inscSnap.forEach(ch => {
                        const insc = { id: ch.key, ...ch.val() };
                        if (isRejectedInscription(insc) || !insc.studentId) return;
                        if (!inscriptionMatchesSelectedBranch(insc)) return;

                        const payment = paymentsByStudent[String(insc.studentId)] || null;
                        const student = studentsById[insc.studentId] || {};
                        const inscriptionYear = insc.anneeAcademique || insc.anneeScolaire || insc.academicYear;
                        const recordYear = inscriptionYear || student.academicYear || payment?.academicYear;
                        if (recordYear && normalizeYear(recordYear) !== selectedYearKey) return;

                        const branch = student.branch || payment?.branch || mapInscriptionBranchToInternal(insc.branche, insc.section, insc.niveau);
                        const address = student.address || [insc.secteur, insc.umurenge, insc.akagari, insc.umudugudu].filter(Boolean).join(', ');
                        const secondParent = student.spouseName || student.secondParent || student.parent2 || student.parent2Name ||
                            insc.conjointNomPrenom || insc.secondParent || insc.parent2 || insc.parent2Name || '';
                        const secondParentPhone = student.spousePhone || student.secondParentPhone || student.parent2Phone ||
                            insc.conjointTelephone || insc.secondParentPhone || insc.parent2Phone || '';

                        rows.push({
                            studentId: insc.studentId,
                            reference: student.reference || payment?.reference || insc.codeUnique || insc.studentId,
                            name: student.name || student.fullName || payment?.studentName || insc.nomPrenom || 'N/A',
                            className: student.classes || student.class || student.classe || payment?.studentClass || insc.classe || insc.niveau || 'N/A',
                            branch: getBranchName(branch),
                            academicYear: recordYear || selectedAcademicYear,
                            bus: student.transportEnrolled === true || normalizeReportValue(student.transportEnrolled) === 'oui' || normalizeReportValue(insc.transport) === 'oui' ? 'OUI' : 'NON',
                            address: address || 'N/A',
                            parent: student.parent || insc.chefNomPrenom || 'N/A',
                            parentPhone: student.parentPhone || insc.chefTelephone || 'N/A',
                            spouse: secondParent || 'N/A',
                            spousePhone: secondParentPhone || 'N/A',
                            registrationStatus: insc.statut || insc.status || 'Enregistré',
                            unpaid: !payment
                        });
                        seenStudentIds.add(String(insc.studentId));
                    });

                    // 2) Paiements historiques qui n'ont pas d'inscription correspondante.
                    allPayments.forEach(payment => {
                        const studentId = payment.studentId ? String(payment.studentId) : '';
                        if (studentId && seenStudentIds.has(studentId)) return;
                        if (!registrationRecordMatchesBranch(payment.branch, payment.section, payment.studentClass, selectedBranch)) return;
                        const student = studentId ? (studentsById[studentId] || {}) : {};
                        const recordYear = payment.academicYear || student.academicYear;
                        if (recordYear && normalizeYear(recordYear) !== selectedYearKey) return;

                        const branch = student.branch || payment.branch || '';
                        rows.push({
                            studentId: payment.studentId || '',
                            reference: student.reference || payment.reference || payment.studentId || payment.id,
                            name: student.name || student.fullName || payment.studentName || 'N/A',
                            className: student.classes || student.class || student.classe || payment.studentClass || 'N/A',
                            branch: getBranchName(branch),
                            academicYear: recordYear || selectedAcademicYear,
                            bus: student.transportEnrolled === true || normalizeReportValue(student.transportEnrolled) === 'oui' ? 'OUI' : 'NON',
                            address: student.address || 'N/A',
                            parent: student.parent || 'N/A',
                            parentPhone: student.parentPhone || 'N/A',
                            spouse: student.spouseName || student.secondParent || student.parent2 || student.parent2Name || 'N/A',
                            spousePhone: student.spousePhone || student.secondParentPhone || student.parent2Phone || 'N/A',
                            registrationStatus: 'Paiement enregistré',
                            unpaid: false
                        });
                        if (studentId) seenStudentIds.add(studentId);
                    });

                    rows.sort((a, b) => String(a.branch).localeCompare(String(b.branch), 'fr') ||
                        String(a.className).localeCompare(String(b.className), 'fr') ||
                        String(a.name).localeCompare(String(b.name), 'fr'));

                    const doc = new JsPDF({ unit: 'mm', format: 'a4', orientation: 'landscape' });
                    const titleBranch = selectedBranch ? getBranchName(selectedBranch) : 'Toutes les branches';
                    const today = new Date().toLocaleDateString('fr-FR');

                    doc.setFont('helvetica', 'bold');
                    doc.setFontSize(16);
                    doc.text('RAPPORT DES ÉLÈVES INSCRITS', 14, 13);
                    doc.setFont('helvetica', 'normal');
                    doc.setFontSize(10);
                    doc.text(`Branche : ${titleBranch}   |   Année scolaire : ${selectedAcademicYear}   |   Date : ${today}`, 14, 20);
                    doc.text(`Total des élèves inscrits : ${rows.length}`, 14, 26);

                    const body = rows.map((r, i) => [
                        i + 1, r.reference, r.name, r.className, r.branch,
                        r.bus, r.address, r.parent, r.parentPhone, r.spouse, r.spousePhone
                    ]);

                    if (body.length) {
                        doc.autoTable({
                            startY: 31,
                            head: [['N°', 'RÉFÉRENCE', 'ÉLÈVE', 'CLASSE', 'BRANCHE', 'BUS',
                                'ADRESSE / LIEU DE RÉSIDENCE', 'PARENT / TUTEUR', 'TÉL. PARENT',
                                '2e PARENT', 'TÉL. 2e PARENT']],
                            body,
                            styles: { fontSize: 7.2, cellPadding: 2, overflow: 'linebreak', valign: 'middle' },
                            headStyles: { fontSize: 7.2, fontStyle: 'bold' },
                            columnStyles: {
                                0: { cellWidth: 9 }, 1: { cellWidth: 24 }, 2: { cellWidth: 31 },
                                3: { cellWidth: 18 }, 4: { cellWidth: 20 }, 5: { cellWidth: 11 },
                                6: { cellWidth: 42 }, 7: { cellWidth: 28 }, 8: { cellWidth: 25 },
                                9: { cellWidth: 25 }, 10: { cellWidth: 25 }
                            },
                            margin: { left: 10, right: 10, top: 31, bottom: 14 },
                            didDrawPage: data => {
                                doc.setFontSize(7);
                                doc.text(`Rapport des élèves inscrits - page ${data.pageNumber}`, 10, 203);
                            }
                        });
                    } else {
                        doc.setFontSize(11);
                        doc.text('Aucun élève inscrit trouvé pour cette année académique.', 14, 38);
                    }

                    const busCount = rows.filter(r => r.bus === 'OUI').length;
                    const finalY = doc.lastAutoTable ? doc.lastAutoTable.finalY : 42;
                    const summaryY = finalY + 8;
                    if (summaryY > 190) doc.addPage();
                    const y = summaryY > 190 ? 20 : summaryY;
                    doc.setFont('helvetica', 'bold');
                    doc.setFontSize(9);
                    doc.text(`Total : ${rows.length} élève(s)   |   Bus : ${busCount}   |   Sans bus : ${rows.length - busCount}`, 10, y);

                    const safeBranch = String(selectedBranch || 'all').replace(/[^a-z0-9_-]/gi, '_');
                    const safeYear = selectedAcademicYear.replace(/[^0-9-]/g, '_');
                    doc.save(`Rapport_Eleves_Inscrits_${safeBranch}_${safeYear}.pdf`);
                    showToast(`Rapport PDF téléchargé : ${rows.length} élève(s) - ${selectedAcademicYear}`, 'success');
                } catch (error) {
                    console.error('Erreur export rapport inscription:', error);
                    showToast('Erreur lors de l\'export du rapport : ' + (error.message || error), 'error');
                }
            }
'''
s = s[:start] + new + s[end:]
p.write_text(s, encoding='utf-8', newline='')
print('PDF export function replaced')
