// Configuration Firebase réelle (client) pour Eden Family School
const firebaseConfig = {
    apiKey: "AIzaSyApUFNELOfgIe7rWEek9GLS9EIphNW09-A",
    authDomain: "edensmart-app.firebaseapp.com",
    projectId: "edensmart-app",
    storageBucket: "edensmart-app.firebasestorage.app",
    messagingSenderId: "1093120876724",
    appId: "1:1093120876724:web:bc37448cadd18d651c77e1",
    measurementId: "G-1FL70PZZSW"
};

// Initialisation Firebase côté navigateur (SDK compat chargé via index.html)
firebase.initializeApp(firebaseConfig);
const database = firebase.database();
const auth = firebase.auth();
const storage = firebase.storage();
const firestore = firebase.firestore();

// Code de l'école (Kacyiru)
const SCHOOL_CODE = 'EDF-KA';

        let currentDirector = null;
        let carouselItems = [];

        // Récupère un nom de directeur sûr même si les données ne sont pas encore chargées
        function getDirectorName() {
            if (currentDirector && currentDirector.name) {
                return currentDirector.name;
            }
            const user = auth.currentUser;
            if (user) {
                return user.displayName || user.email || 'Directeur';
            }
            return 'Directeur';
        }

        // Class structure - Chargée depuis Firebase
        let classesData = {
            maternelle: [],
            primaire: []
        };

        // Charger les classes depuis Firebase
        async function loadClassesFromFirebase() {
            try {
                const classesRef = database.ref('config/classes');
                const classesSnapshot = await classesRef.once('value');
                
                if (classesSnapshot.exists()) {
                    const classes = classesSnapshot.val();
                    classesData.maternelle = classes.maternelle || [];
                    classesData.primaire = classes.primaire || [];
                    console.log('Classes chargées depuis Firebase:', classesData);
                } else {
                    // Valeurs par défaut si pas encore configuré dans Firebase
                    classesData = {
                        maternelle: [
                            { id: '1A', name: '1ère A Maternelle', niveau: '1A' },
                            { id: '1B', name: '1ère B Maternelle', niveau: '1B' },
                            { id: '2', name: '2ème Maternelle', niveau: '2' },
                            { id: '3', name: '3ème Maternelle', niveau: '3' }
                        ],
                        primaire: [
                            { id: 'P1', name: 'Primaire - P1', niveau: 'P1' },
                            { id: 'P2', name: 'Primaire - P2', niveau: 'P2' },
                            { id: 'P3', name: 'Primaire - P3', niveau: 'P3' },
                            { id: 'P4', name: 'Primaire - P4', niveau: 'P4' },
                            { id: 'P5', name: 'Primaire - P5', niveau: 'P5' },
                            { id: 'P6', name: 'Primaire - P6', niveau: 'P6' }
                        ]
                    };
                    // Sauvegarder les valeurs par défaut dans Firebase
                    await classesRef.set(classesData);
                    console.log('Classes par défaut sauvegardées dans Firebase');
                }
            } catch (error) {
                console.error('Erreur lors du chargement des classes:', error);
                // Utiliser les valeurs par défaut en cas d'erreur
                classesData = {
                    maternelle: [
                        { id: '1A', name: '1ère A Maternelle', niveau: '1A' },
                        { id: '1B', name: '1ère B Maternelle', niveau: '1B' },
                        { id: '2', name: '2ème Maternelle', niveau: '2' },
                        { id: '3', name: '3ème Maternelle', niveau: '3' }
                    ],
                    primaire: [
                        { id: 'P1', name: 'Primaire - P1', niveau: 'P1' },
                        { id: 'P2', name: 'Primaire - P2', niveau: 'P2' },
                        { id: 'P3', name: 'Primaire - P3', niveau: 'P3' },
                        { id: 'P4', name: 'Primaire - P4', niveau: 'P4' },
                        { id: 'P5', name: 'Primaire - P5', niveau: 'P5' },
                        { id: 'P6', name: 'Primaire - P6', niveau: 'P6' }
                    ]
                };
            }
        }

        // Initialize
        document.addEventListener('DOMContentLoaded', async function() {
            // Charger les classes depuis Firebase
            await loadClassesFromFirebase();
            checkAuth();
            loadCarousel();
        });

        // Check authentication
        function checkAuth() {
            auth.onAuthStateChanged(function(user) {
                if (user) {
                    loadDirectorData(user.email);
                } else {
                    window.location.href = 'index.html';
                }
            });
        }

        // Load director data
        async function loadDirectorData(userEmail) {
            try {
                const snapshot = await database.ref('directors').orderByChild('email').equalTo(userEmail.toLowerCase()).once('value');
                if (snapshot.exists()) {
                    const directorKey = Object.keys(snapshot.val())[0];
                    currentDirector = snapshot.val()[directorKey];
                    currentDirector.id = directorKey; // Pour compatibilité
                    console.log('Données directeur chargées depuis Firebase:', currentDirector);
                    document.getElementById('directorName').textContent = currentDirector.name || 'Directeur';
                    loadDashboardData();
                    loadSchoolLogo();
                    loadActivities();
                    loadSchoolSettings();
                } else {
                    console.error('Aucun directeur trouvé avec cet email:', userEmail);
                }
            } catch (error) {
                console.error('Erreur lors du chargement des données directeur:', error);
            }
        }

        // Load school logo
        function loadSchoolLogo() {
            storage.ref('logos/school-logo.png').getDownloadURL().then(function(url) {
                document.getElementById('schoolLogo').src = url;
            }).catch(function(error) {
                console.log('Logo not found');
            });
        }

        // Load dashboard statistics
        function loadDashboardData() {
            // Count students
            database.ref('students').once('value').then(function(snapshot) {
                const count = snapshot.numChildren();
                document.getElementById('totalStudents').textContent = count;

                // Count completed bulletins
                let completed = 0;
                snapshot.forEach(function(child) {
                    if (child.val().bulletinCompleted) completed++;
                });
                document.getElementById('completedBulletins').textContent = completed;
            });

            // Count teachers
            database.ref('teachers').once('value').then(function(snapshot) {
                document.getElementById('totalTeachers').textContent = snapshot.numChildren();
            });

            // Load financial data
            loadFinancialData();
            
            // Load classes overview
            loadClassesOverview();
            
            // Load reports
            loadSecretaryReports();
        }

        // Load financial data
        function loadFinancialData() {
            database.ref('financial/reports').once('value').then(function(snapshot) {
                let total = 0;
                let reported = 0;
                let verifiedCount = 0;
                let totalCount = 0;

                snapshot.forEach(function(child) {
                    const report = child.val();
                    total += report.amount || 0;
                    totalCount++;
                    
                    if (report.verified) {
                        reported += report.amount || 0;
                        verifiedCount++;
                    }
                });

                const difference = total - reported;
                const matchRate = total > 0 ? Math.round((reported / total) * 100) : 100;

                document.getElementById('totalRevenue').textContent = total + '€';
                document.getElementById('reportedRevenue').textContent = reported + '€';
                document.getElementById('difference').textContent = difference + '€';
                document.getElementById('verificationStatus').textContent = matchRate + '%';
            });
        }

        // Load classes overview
        function loadClassesOverview() {
            const grid = document.getElementById('classesGrid');
            let html = '';
            const allClasses = [...classesData.maternelle, ...classesData.primaire];

            if (allClasses.length === 0) {
                grid.innerHTML = '<p style="color: #6b7280;">Aucune classe trouvée</p>';
                return;
            }

            let processed = 0;
            allClasses.forEach(function(classe) {
                database.ref('students').orderByChild('classe').equalTo(classe.name).once('value').then(function(snapshot) {
                    const studentCount = snapshot.numChildren();
                    let completedCount = 0;

                    snapshot.forEach(function(child) {
                        if (child.val().bulletinCompleted) completedCount++;
                    });

                    database.ref('teachers').orderByChild('classe').equalTo(classe.name).once('value').then(function(teacherSnap) {
                        let teacherName = 'Non assigné';
                        teacherSnap.forEach(function(child) {
                            teacherName = child.val().name;
                        });

                        html += `
                            <div class="class-card">
                                <div class="class-name">${classe.name}</div>
                                <div class="class-info">👥 ${studentCount} élèves</div>
                                <div class="class-info">✅ ${completedCount}/${studentCount} bulletins</div>
                                <div class="class-info">👨‍🏫 ${teacherName}</div>
                            </div>
                        `;

                        processed++;
                        if (processed === allClasses.length) {
                            grid.innerHTML = html;
                        }
                    });
                });
            });
        }

        // Load secretary reports
        function loadSecretaryReports() {
            const reportsDiv = document.getElementById('reportsList');
            database.ref('financial/reports').orderByChild('date').limitToLast(5).once('value').then(function(snapshot) {
                if (!snapshot.exists()) {
                    reportsDiv.innerHTML = '<p style="color: #6b7280; padding: 20px; text-align: center;">Aucun rapport disponible</p>';
                    return;
                }

                let html = '';
                snapshot.forEach(function(child) {
                    const report = child.val();
                    const date = new Date(report.date).toLocaleDateString('fr-FR');
                    
                    html += `
                        <div class="report-item">
                            <div class="report-header">
                                <div>
                                    <strong>${report.secretaryName || 'Secrétaire'}</strong>
                                    <div class="report-date">${date}</div>
                                </div>
                                <div class="report-amount">${report.amount || 0}€</div>
                            </div>
                            <p style="margin: 10px 0;">${report.description || 'Aucune description'}</p>
                            <div class="action-buttons">
                                <button class="btn btn-small btn-success" onclick="verifyReport('${child.key}')">✅ Vérifier</button>
                                <button class="btn btn-small btn-warning" onclick="editReport('${child.key}')">✏️ Modifier</button>
                            </div>
                        </div>
                    `;
                });

                reportsDiv.innerHTML = html;
            });
        }

        // Load activities
        function loadActivities() {
            const activitiesDiv = document.getElementById('activitiesList');
            database.ref('activities').orderByChild('timestamp').limitToLast(10).once('value').then(function(snapshot) {
                if (!snapshot.exists()) {
                    activitiesDiv.innerHTML = `
                        <div class="activity-item">
                            <div style="color: #6b7280; font-style: italic;">Aucune activité récente</div>
                            <div class="activity-time">-</div>
                        </div>
                    `;
                    return;
                }

                let html = '';
                snapshot.forEach(function(child) {
                    const activity = child.val();
                    const time = new Date(activity.timestamp).toLocaleString('fr-FR');
                    
                    html += `
                        <div class="activity-item">
                            <div>${activity.message}</div>
                            <div class="activity-time">${time}</div>
                        </div>
                    `;
                });

                activitiesDiv.innerHTML = html;
            });
        }

        // Show section
        function showSection(section) {
            // Clear active state on all nav buttons
            document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

            // Try to find the clicked nav button by inspecting onclick attribute or data-section
            const selector = `.nav-btn[onclick="showSection('${section}')"]`;
            const clicked = document.querySelector(selector) || document.querySelector(`.nav-btn[data-section="${section}"]`);
            if (clicked) clicked.classList.add('active');

            // Hide all sections by convention
            const sections = ['dashboard','reports','accounts','carousel','settings','teachers','payroll','student-fees','students'];
            sections.forEach(s => {
                const el = document.getElementById(s + 'Section');
                if (el) el.classList.add('hidden');
            });

            const sectionEl = document.getElementById(section + 'Section');
            if (sectionEl) sectionEl.classList.remove('hidden');

            // Trigger section-specific loads
            if (section === 'dashboard') {
                loadDashboardData();
            } else if (section === 'reports') {
                if (typeof loadBulletinStats === 'function') loadBulletinStats();
                if (typeof loadFinancialData === 'function') loadFinancialData();
            } else if (section === 'accounts') {
                if (typeof loadTeachersList === 'function') loadTeachersList();
                if (typeof loadSecretariesList === 'function') loadSecretariesList();
            } else if (section === 'carousel') {
                if (typeof loadCarousel === 'function') loadCarousel();
            }
        }

        // Update niveaux for reports
        function updateReportNiveaux() {
            const section = document.getElementById('reportSection').value;
            const niveauSelect = document.getElementById('reportNiveau');

            niveauSelect.innerHTML = '<option value="">-- Choisir une classe --</option>';
            niveauSelect.disabled = !section;

            if (section && classesData[section]) {
                classesData[section].forEach(function(classe) {
                    niveauSelect.innerHTML += `<option value="${classe.id}">${classe.name}</option>`;
                });
            }

            // Hide class reports panel when section changes
            document.getElementById('classReportsPanel').style.display = 'none';
            document.getElementById('individualReportPanel').style.display = 'none';
        }

        // Load class reports
        function loadClassReports() {
            const section = document.getElementById('reportSection').value;
            const niveau = document.getElementById('reportNiveau').value;

            if (!section || !niveau) {
                document.getElementById('classReportsPanel').style.display = 'none';
                return;
            }

            const panel = document.getElementById('classReportsPanel');
            const loading = document.getElementById('classReportsLoading');
            const content = document.getElementById('classReportsContent');
            const title = document.getElementById('classReportsTitle');

            panel.style.display = 'block';
            loading.style.display = 'block';
            content.style.display = 'none';

            // Set title
            const className = classesData[section].find(c => c.id === niveau)?.name || niveau;
            title.textContent = `👥 Élèves de ${className}`;

            // Load students and their reports
            loadStudentsReports(section, niveau);
        }

        // Load students reports for a class
        async function loadStudentsReports(section, niveau) {
            try {
                // Get students in the class
                const studentsRef = database.ref('students');
                const studentsSnapshot = await studentsRef.orderByChild('classe').equalTo(niveau).once('value');

                if (!studentsSnapshot.exists()) {
                    showEmptyState('Aucun élève trouvé dans cette classe');
                    return;
                }

                const students = [];
                studentsSnapshot.forEach(child => {
                    const student = child.val();
                    student.id = child.key;
                    students.push(student);
                });

                // Sort students alphabetically by name
                students.sort((a, b) => (a.nom || '').localeCompare(b.nom || ''));

                // Get bulletins for this class
                const [conduiteSnapshot, academiqueSnapshot] = await Promise.all([
                    database.ref('bulletins-conduite').orderByChild('student/classe').equalTo(niveau).once('value'),
                    database.ref('bulletins-academique').orderByChild('student/classe').equalTo(niveau).once('value')
                ]);

                const conduiteReports = {};
                const academiqueReports = {};

                // Process conduite bulletins
                if (conduiteSnapshot.exists()) {
                    conduiteSnapshot.forEach(child => {
                        const bulletin = child.val();
                        const studentId = bulletin.student?.id;
                        if (studentId) {
                            if (!conduiteReports[studentId]) conduiteReports[studentId] = [];
                            conduiteReports[studentId].push(bulletin);
                        }
                    });
                }

                // Process academique bulletins
                if (academiqueSnapshot.exists()) {
                    academiqueSnapshot.forEach(child => {
                        const bulletin = child.val();
                        const studentId = bulletin.student?.id;
                        if (studentId) {
                            if (!academiqueReports[studentId]) academiqueReports[studentId] = [];
                            academiqueReports[studentId].push(bulletin);
                        }
                    });
                }

                // Update summary counts
                updateReportsSummary(students.length, conduiteReports, academiqueReports);

                // Display students with their reports
                displayStudentsReports(students, conduiteReports, academiqueReports);

            } catch (error) {
                console.error('Error loading class reports:', error);
                showAlert('Erreur lors du chargement des rapports', 'error');
            }
        }

        // Update reports summary
        function updateReportsSummary(totalStudents, conduiteReports, academiqueReports) {
            document.getElementById('totalStudentsCount').textContent = totalStudents;

            let totalConduite = 0;
            let totalAcademique = 0;
            let totalComplete = 0;

            // Count reports
            Object.values(conduiteReports).forEach(reports => totalConduite += reports.length);
            Object.values(academiqueReports).forEach(reports => totalAcademique += reports.length);

            // Count students with both types of reports
            const allStudentIds = new Set([
                ...Object.keys(conduiteReports),
                ...Object.keys(academiqueReports)
            ]);

            allStudentIds.forEach(studentId => {
                const hasConduite = conduiteReports[studentId] && conduiteReports[studentId].length > 0;
                const hasAcademique = academiqueReports[studentId] && academiqueReports[studentId].length > 0;
                if (hasConduite && hasAcademique) totalComplete++;
            });

            document.getElementById('conduiteReportsCount').textContent = totalConduite;
            document.getElementById('academiqueReportsCount').textContent = totalAcademique;
            document.getElementById('completeReportsCount').textContent = totalComplete;
        }

        // Display students reports
        function displayStudentsReports(students, conduiteReports, academiqueReports) {
            const grid = document.getElementById('studentsReportsGrid');

            if (students.length === 0) {
                grid.innerHTML = '<div class="empty-state"><div>📚</div><p>Aucun élève trouvé</p></div>';
                return;
            }

            let html = '';
            students.forEach(student => {
                const studentId = student.id;
                const conduiteCount = conduiteReports[studentId]?.length || 0;
                const academiqueCount = academiqueReports[studentId]?.length || 0;
                const hasBoth = conduiteCount > 0 && academiqueCount > 0;

                html += `
                    <div class="student-report-card" onclick="viewStudentReports('${studentId}', '${student.nom}', '${student.code}')">
                        <div class="student-report-header">
                            <div>
                                <div class="student-report-name">${student.nom || 'Nom inconnu'}</div>
                                <div class="student-report-code">Code: ${student.code || 'N/A'}</div>
                            </div>
                            <div class="student-report-badges">
                                ${conduiteCount > 0 ? `<div class="report-badge conduite">${conduiteCount} Cond.</div>` : `<div class="report-badge missing">0 Cond.</div>`}
                                ${academiqueCount > 0 ? `<div class="report-badge academique">${academiqueCount} Acad.</div>` : `<div class="report-badge missing">0 Acad.</div>`}
                                ${hasBoth ? `<div class="report-badge complete">Complet</div>` : `<div class="report-badge missing">Incomplet</div>`}
                            </div>
                        </div>
                        <div class="student-report-actions">
                            <button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); downloadStudentReports('${studentId}', '${student.nom}')">
                                📥 PDF
                            </button>
                            <button class="btn btn-sm btn-outline" onclick="event.stopPropagation(); viewStudentReports('${studentId}', '${student.nom}', '${student.code}')">
                                👁️ Voir
                            </button>
                        </div>
                    </div>
                `;
            });

            grid.innerHTML = html;

            // Show content
            document.getElementById('classReportsLoading').style.display = 'none';
            document.getElementById('classReportsContent').style.display = 'block';
        }

        // Sort students
        function sortStudents(sortBy) {
            const grid = document.getElementById('studentsReportsGrid');
            const cards = Array.from(grid.children);

            cards.sort((a, b) => {
                if (sortBy === 'name') {
                    const nameA = a.querySelector('.student-report-name').textContent.toLowerCase();
                    const nameB = b.querySelector('.student-report-name').textContent.toLowerCase();
                    return nameA.localeCompare(nameB);
                } else if (sortBy === 'code') {
                    const codeA = a.querySelector('.student-report-code').textContent.replace('Code: ', '').toLowerCase();
                    const codeB = b.querySelector('.student-report-code').textContent.replace('Code: ', '').toLowerCase();
                    return codeA.localeCompare(codeB);
                }
                return 0;
            });

            cards.forEach(card => grid.appendChild(card));
        }

        // Filter reports
        function filterReports() {
            const type = document.getElementById('reportType').value;
            const trimestre = document.getElementById('reportTrimestre').value;
            const cards = document.querySelectorAll('.student-report-card');

            cards.forEach(card => {
                let show = true;

                if (type !== 'all') {
                    const hasType = card.querySelector(`.report-badge.${type}`);
                    const count = hasType ? parseInt(hasType.textContent.split(' ')[0]) : 0;
                    show = show && count > 0;
                }

                // Note: Filtering by trimestre would require more complex logic
                // For now, we show all cards when trimestre filter is applied

                card.style.display = show ? 'block' : 'none';
            });
        }

        // View student reports
        async function viewStudentReports(studentId, studentName, studentCode) {
            try {
                const panel = document.getElementById('individualReportPanel');
                const title = document.getElementById('individualReportTitle');
                const content = document.getElementById('individualReportContent');

                title.textContent = `👁️ Bulletins de ${studentName}`;
                panel.style.display = 'block';
                content.innerHTML = '<div class="loading-state"><div>📄</div><p>Chargement des bulletins...</p></div>';

                // Scroll to panel
                panel.scrollIntoView({ behavior: 'smooth' });

                // Load student reports
                const [conduiteSnapshot, academiqueSnapshot] = await Promise.all([
                    database.ref('bulletins-conduite').orderByChild('student/id').equalTo(studentId).once('value'),
                    database.ref('bulletins-academique').orderByChild('student/id').equalTo(studentId).once('value')
                ]);

                let html = '<div class="individual-report-container">';

                // Student info header
                html += `
                    <div class="report-header-info">
                        <div class="report-info-item">
                            <div class="report-info-label">Nom et Prénoms</div>
                            <div class="report-info-value">${studentName}</div>
                        </div>
                        <div class="report-info-item">
                            <div class="report-info-label">Code Élève</div>
                            <div class="report-info-value">${studentCode}</div>
                        </div>
                    </div>
                `;

                let hasReports = false;

                // Display conduite bulletins
                if (conduiteSnapshot.exists()) {
                    html += '<div class="report-section">';
                    html += '<div class="report-section-title">📝 Bulletins de Conduite</div>';

                    conduiteSnapshot.forEach(child => {
                        const bulletin = child.val();
                        hasReports = true;

                        html += `
                            <div class="report-content">
                                <h4>Trimestre ${bulletin.trimestre || 'N/A'} - ${new Date(bulletin.dateCreation || Date.now()).toLocaleDateString('fr-FR')}</h4>
                                <div class="conduite-grid">
                        `;

                        if (bulletin.conduite) {
                            Object.entries(bulletin.conduite).forEach(([key, value]) => {
                                const label = key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1');
                                html += `
                                    <div class="conduite-item">
                                        <span class="conduite-label">${label}</span>
                                        <span class="conduite-value">${value}</span>
                                    </div>
                                `;
                            });
                        }

                        if (bulletin.commentaire) {
                            html += `
                                <div class="report-comment">
                                    <div class="report-comment-title">Commentaire de l'enseignant</div>
                                    <div class="report-comment-text">${bulletin.commentaire}</div>
                                </div>
                            `;
                        }

                        html += '</div></div>';
                    });

                    html += '</div>';
                }

                // Display academique bulletins
                if (academiqueSnapshot.exists()) {
                    html += '<div class="report-section">';
                    html += '<div class="report-section-title">📚 Bulletins Académiques</div>';

                    academiqueSnapshot.forEach(child => {
                        const bulletin = child.val();
                        hasReports = true;

                        html += `
                            <div class="report-content">
                                <h4>Trimestre ${bulletin.trimestre || 'N/A'} - ${new Date(bulletin.dateCreation || Date.now()).toLocaleDateString('fr-FR')}</h4>
                                <div class="academique-table-container">
                                    <table class="academique-table">
                                        <thead>
                                            <tr>
                                                <th>Matière</th>
                                                <th>Test</th>
                                                <th>Examen</th>
                                                <th>Total</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                        `;

                        if (bulletin.academique) {
                            Object.entries(bulletin.academique).forEach(([key, value]) => {
                                if (typeof value === 'object' && value.test !== undefined) {
                                    html += `
                                        <tr>
                                            <td>${key.replace(/_/g, ' ')}</td>
                                            <td>${value.test || '-'}</td>
                                            <td>${value.examen || '-'}</td>
                                            <td>${(value.test || 0) + (value.examen || 0)}</td>
                                        </tr>
                                    `;
                                }
                            });
                        }

                        html += '</tbody></table>';

                        if (bulletin.commentaire) {
                            html += `
                                <div class="report-comment">
                                    <div class="report-comment-title">Commentaire de l'enseignant</div>
                                    <div class="report-comment-text">${bulletin.commentaire}</div>
                                </div>
                            `;
                        }

                        html += '</div></div>';
                    });

                    html += '</div>';
                }

                if (!hasReports) {
                    html += '<div class="empty-state"><div>📄</div><p>Aucun bulletin trouvé pour cet élève</p></div>';
                }

                html += '</div>';
                content.innerHTML = html;

            } catch (error) {
                console.error('Error viewing student reports:', error);
                document.getElementById('individualReportContent').innerHTML =
                    '<div class="empty-state"><div>❌</div><p>Erreur lors du chargement des bulletins</p></div>';
            }
        }

        // Close individual report
        function closeIndividualReport() {
            document.getElementById('individualReportPanel').style.display = 'none';
        }

        // Download individual student reports
        async function downloadStudentReports(studentId, studentName) {
            try {
                showAlert('📄 Génération du PDF en cours...', 'success');

                // Get student reports
                const [conduiteSnapshot, academiqueSnapshot] = await Promise.all([
                    database.ref('bulletins-conduite').orderByChild('student/id').equalTo(studentId).once('value'),
                    database.ref('bulletins-academique').orderByChild('student/id').equalTo(studentId).once('value')
                ]);

                // Create PDF content
                let pdfContent = `
                    <div style="font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px;">
                        <h1 style="text-align: center; color: #1e40af; margin-bottom: 30px;">
                            📊 Rapports de ${studentName}
                        </h1>
                `;

                let hasReports = false;

                // Add conduite reports
                if (conduiteSnapshot.exists()) {
                    pdfContent += '<h2 style="color: #059669; margin-top: 30px;">📝 Bulletins de Conduite</h2>';
                    conduiteSnapshot.forEach(child => {
                        const bulletin = child.val();
                        hasReports = true;
                        pdfContent += `
                            <div style="margin-bottom: 20px; padding: 15px; border: 1px solid #e5e7eb; border-radius: 8px;">
                                <h3>Trimestre ${bulletin.trimestre} - ${new Date(bulletin.dateCreation).toLocaleDateString('fr-FR')}</h3>
                                <table style="width: 100%; border-collapse: collapse; margin-top: 10px;">
                                    <thead>
                                        <tr style="background: #f8fafc;">
                                            <th style="border: 1px solid #e5e7eb; padding: 8px; text-align: left;">Critère</th>
                                            <th style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">Note</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                        `;

                        if (bulletin.conduite) {
                            Object.entries(bulletin.conduite).forEach(([key, value]) => {
                                const label = key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1');
                                pdfContent += `
                                    <tr>
                                        <td style="border: 1px solid #e5e7eb; padding: 8px;">${label}</td>
                                        <td style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">${value}</td>
                                    </tr>
                                `;
                            });
                        }

                        pdfContent += '</tbody></table>';

                        if (bulletin.commentaire) {
                            pdfContent += `<p style="margin-top: 10px;"><strong>Commentaire:</strong> ${bulletin.commentaire}</p>`;
                        }

                        pdfContent += '</div>';
                    });
                }

                // Add academique reports
                if (academiqueSnapshot.exists()) {
                    pdfContent += '<h2 style="color: #dc2626; margin-top: 30px;">📚 Bulletins Académiques</h2>';
                    academiqueSnapshot.forEach(child => {
                        const bulletin = child.val();
                        hasReports = true;
                        pdfContent += `
                            <div style="margin-bottom: 20px; padding: 15px; border: 1px solid #e5e7eb; border-radius: 8px;">
                                <h3>Trimestre ${bulletin.trimestre} - ${new Date(bulletin.dateCreation).toLocaleDateString('fr-FR')}</h3>
                                <table style="width: 100%; border-collapse: collapse; margin-top: 10px;">
                                    <thead>
                                        <tr style="background: #f8fafc;">
                                            <th style="border: 1px solid #e5e7eb; padding: 8px; text-align: left;">Matière</th>
                                            <th style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">Test</th>
                                            <th style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">Examen</th>
                                            <th style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">Total</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                        `;

                        if (bulletin.academique) {
                            Object.entries(bulletin.academique).forEach(([key, value]) => {
                                if (typeof value === 'object' && value.test !== undefined) {
                                    pdfContent += `
                                        <tr>
                                            <td style="border: 1px solid #e5e7eb; padding: 8px;">${key.replace(/_/g, ' ')}</td>
                                            <td style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">${value.test || '-'}</td>
                                            <td style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">${value.examen || '-'}</td>
                                            <td style="border: 1px solid #e5e7eb; padding: 8px; text-align: center;">${(value.test || 0) + (value.examen || 0)}</td>
                                        </tr>
                                    `;
                                }
                            });
                        }

                        pdfContent += '</tbody></table>';

                        if (bulletin.commentaire) {
                            pdfContent += `<p style="margin-top: 10px;"><strong>Commentaire:</strong> ${bulletin.commentaire}</p>`;
                        }

                        pdfContent += '</div>';
                    });
                }

                if (!hasReports) {
                    pdfContent += '<p style="text-align: center; color: #6b7280; margin-top: 50px;">Aucun bulletin trouvé pour cet élève</p>';
                }

                pdfContent += '</div>';

                // Generate PDF
                const element = document.createElement('div');
                element.innerHTML = pdfContent;
                element.style.position = 'absolute';
                element.style.left = '-9999px';
                document.body.appendChild(element);

                const canvas = await html2canvas(element, {
                    scale: 2,
                    useCORS: true,
                    allowTaint: true,
                    backgroundColor: '#ffffff'
                });

                document.body.removeChild(element);

                const imgData = canvas.toDataURL('image/png');
                const { jsPDF } = window.jspdf;
                const pdf = new jsPDF('p', 'mm', 'a4');

                const imgWidth = 210;
                const pageHeight = 295;
                const imgHeight = (canvas.height * imgWidth) / canvas.width;
                let heightLeft = imgHeight;

                let position = 0;

                pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
                heightLeft -= pageHeight;

                while (heightLeft >= 0) {
                    position = heightLeft - imgHeight;
                    pdf.addPage();
                    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
                    heightLeft -= pageHeight;
                }

                const fileName = `Rapports_${studentName.replace(/\s+/g, '_')}.pdf`;
                pdf.save(fileName);

                showAlert('✅ PDF généré avec succès!', 'success');

            } catch (error) {
                console.error('Error downloading student reports:', error);
                showAlert('❌ Erreur lors de la génération du PDF', 'error');
            }
        }

        // Download individual report
        function downloadIndividualReport() {
            const title = document.getElementById('individualReportTitle').textContent;
            const studentName = title.replace('👁️ Bulletins de ', '');

            // Use the same function as downloadStudentReports but for current student
            const studentCard = document.querySelector('.student-report-card[onclick*="viewStudentReports"]');
            if (studentCard) {
                const onclickAttr = studentCard.getAttribute('onclick');
                const matches = onclickAttr.match(/viewStudentReports\('([^']+)', '([^']+)', '([^']+)'\)/);
                if (matches) {
                    downloadStudentReports(matches[1], matches[2]);
                }
            }
        }

        // Download all reports PDF
        async function downloadAllReportsPDF() {
            const section = document.getElementById('reportSection').value;
            const niveau = document.getElementById('reportNiveau').value;

            if (!section || !niveau) {
                showAlert('⚠️ Veuillez sélectionner une section et une classe', 'error');
                return;
            }

            try {
                showAlert('📄 Génération du PDF complet en cours...', 'success');

                // Get all students and their reports
                const studentsRef = database.ref('students');
                const studentsSnapshot = await studentsRef.orderByChild('classe').equalTo(niveau).once('value');

                if (!studentsSnapshot.exists()) {
                    showAlert('❌ Aucun élève trouvé dans cette classe', 'error');
                    return;
                }

                const students = [];
                studentsSnapshot.forEach(child => {
                    const student = child.val();
                    student.id = child.key;
                    students.push(student);
                });

                // Sort alphabetically
                students.sort((a, b) => (a.nom || '').localeCompare(b.nom || ''));

                // Create comprehensive PDF
                let pdfContent = `
                    <div style="font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px;">
                        <h1 style="text-align: center; color: #1e40af; margin-bottom: 30px;">
                            📊 Rapport Complet de Classe
                        </h1>
                        <h2 style="text-align: center; color: #6b7280; margin-bottom: 40px;">
                            ${classesData[section].find(c => c.id === niveau)?.name || niveau}
                        </h2>
                        <p style="text-align: center; margin-bottom: 30px;">
                            Généré le ${new Date().toLocaleDateString('fr-FR')} à ${new Date().toLocaleTimeString('fr-FR')}
                        </p>
                `;

                for (const student of students) {
                    pdfContent += `
                        <div style="page-break-after: always; margin-bottom: 50px; border-bottom: 2px solid #e5e7eb; padding-bottom: 30px;">
                            <h3 style="color: #1e40af; margin-bottom: 20px;">👤 ${student.nom} (${student.code})</h3>
                    `;

                    // Get student reports
                    const [conduiteSnapshot, academiqueSnapshot] = await Promise.all([
                        database.ref('bulletins-conduite').orderByChild('student/id').equalTo(student.id).once('value'),
                        database.ref('bulletins-academique').orderByChild('student/id').equalTo(student.id).once('value')
                    ]);

                    // Add conduite reports
                    if (conduiteSnapshot.exists()) {
                        pdfContent += '<h4 style="color: #059669; margin-top: 20px;">📝 Bulletins de Conduite</h4>';
                        conduiteSnapshot.forEach(child => {
                            const bulletin = child.val();
                            pdfContent += `
                                <div style="margin-bottom: 15px; padding: 10px; background: #f8fafc; border-radius: 5px;">
                                    <strong>Trimestre ${bulletin.trimestre}</strong>
                                    <table style="width: 100%; border-collapse: collapse; margin-top: 5px; font-size: 12px;">
                                        <tbody>
                            `;

                            if (bulletin.conduite) {
                                Object.entries(bulletin.conduite).forEach(([key, value]) => {
                                    const label = key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1');
                                    pdfContent += `<tr><td style="border: 1px solid #e5e7eb; padding: 4px;">${label}</td><td style="border: 1px solid #e5e7eb; padding: 4px; text-align: center;">${value}</td></tr>`;
                                });
                            }

                            pdfContent += '</tbody></table>';
                            if (bulletin.commentaire) {
                                pdfContent += `<p style="margin-top: 5px; font-size: 12px;"><em>${bulletin.commentaire}</em></p>`;
                            }
                            pdfContent += '</div>';
                        });
                    }

                    // Add academique reports
                    if (academiqueSnapshot.exists()) {
                        pdfContent += '<h4 style="color: #dc2626; margin-top: 20px;">📚 Bulletins Académiques</h4>';
                        academiqueSnapshot.forEach(child => {
                            const bulletin = child.val();
                            pdfContent += `
                                <div style="margin-bottom: 15px; padding: 10px; background: #fefce8; border-radius: 5px;">
                                    <strong>Trimestre ${bulletin.trimestre}</strong>
                                    <table style="width: 100%; border-collapse: collapse; margin-top: 5px; font-size: 12px;">
                                        <thead>
                                            <tr style="background: #f8fafc;">
                                                <th style="border: 1px solid #e5e7eb; padding: 4px;">Matière</th>
                                                <th style="border: 1px solid #e5e7eb; padding: 4px;">Test</th>
                                                <th style="border: 1px solid #e5e7eb; padding: 4px;">Examen</th>
                                                <th style="border: 1px solid #e5e7eb; padding: 4px;">Total</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                            `;

                            if (bulletin.academique) {
                                Object.entries(bulletin.academique).forEach(([key, value]) => {
                                    if (typeof value === 'object' && value.test !== undefined) {
                                        pdfContent += `
                                            <tr>
                                                <td style="border: 1px solid #e5e7eb; padding: 4px;">${key.replace(/_/g, ' ')}</td>
                                                <td style="border: 1px solid #e5e7eb; padding: 4px; text-align: center;">${value.test || '-'}</td>
                                                <td style="border: 1px solid #e5e7eb; padding: 4px; text-align: center;">${value.examen || '-'}</td>
                                                <td style="border: 1px solid #e5e7eb; padding: 4px; text-align: center;">${(value.test || 0) + (value.examen || 0)}</td>
                                            </tr>
                                        `;
                                    }
                                });
                            }

                            pdfContent += '</tbody></table>';
                            if (bulletin.commentaire) {
                                pdfContent += `<p style="margin-top: 5px; font-size: 12px;"><em>${bulletin.commentaire}</em></p>`;
                            }
                            pdfContent += '</div>';
                        });
                    }

                    if (!conduiteSnapshot.exists() && !academiqueSnapshot.exists()) {
                        pdfContent += '<p style="color: #6b7280; font-style: italic;">Aucun bulletin trouvé</p>';
                    }

                    pdfContent += '</div>';
                }

                pdfContent += '</div>';

                // Generate PDF
                const element = document.createElement('div');
                element.innerHTML = pdfContent;
                element.style.position = 'absolute';
                element.style.left = '-9999px';
                document.body.appendChild(element);

                const canvas = await html2canvas(element, {
                    scale: 1.5,
                    useCORS: true,
                    allowTaint: true,
                    backgroundColor: '#ffffff'
                });

                document.body.removeChild(element);

                const imgData = canvas.toDataURL('image/png');
                const { jsPDF } = window.jspdf;
                const pdf = new jsPDF('p', 'mm', 'a4');

                const imgWidth = 210;
                const pageHeight = 295;
                const imgHeight = (canvas.height * imgWidth) / canvas.width;
                let heightLeft = imgHeight;

                let position = 0;

                pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
                heightLeft -= pageHeight;

                while (heightLeft >= 0) {
                    position = heightLeft - imgHeight;
                    pdf.addPage();
                    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
                    heightLeft -= pageHeight;
                }

                const className = classesData[section].find(c => c.id === niveau)?.name || niveau;
                const fileName = `Rapport_Complet_${className.replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.pdf`;
                pdf.save(fileName);

                showAlert('✅ Rapport PDF complet généré avec succès!', 'success');

            } catch (error) {
                console.error('Error downloading all reports PDF:', error);
                showAlert('❌ Erreur lors de la génération du PDF', 'error');
            }
        }

        // Download all reports ZIP
        async function downloadAllReportsZIP() {
            showAlert('📦 Fonctionnalité ZIP en développement...', 'success');
            // TODO: Implement ZIP download functionality
        }

        // Export reports to Excel
        async function exportReportsExcel() {
            showAlert('📊 Fonctionnalité Excel en développement...', 'success');
            // TODO: Implement Excel export functionality
        }

        // Show empty state
        function showEmptyState(message) {
            const loading = document.getElementById('classReportsLoading');
            const content = document.getElementById('classReportsContent');

            loading.style.display = 'none';
            content.style.display = 'block';
            content.innerHTML = `<div class="empty-state"><div>📚</div><p>${message}</p></div>`;
        }

        // Load bulletin statistics
        function loadBulletinStats() {
            const statsDiv = document.getElementById('bulletinStats');
            database.ref('bulletins-conduite').once('value').then(function(snapshot) {
                const total = snapshot.numChildren();
                statsDiv.innerHTML = `
                    <div class="teacher-section">
                        <h3>📊 Statistiques Globales</h3>
                        <p><strong>Total bulletins de conduite créés:</strong> ${total}</p>
                        <p><strong>Total bulletins académiques:</strong> En attente...</p>
                    </div>
                `;
            });
        }

        // Update teacher niveaux
        function updateTeacherNiveaux() {
            const section = document.getElementById('teacherSection').value;
            const classeSelect = document.getElementById('teacherClasse');

            classeSelect.innerHTML = '<option value="">-- Choisir --</option>';
            classeSelect.disabled = !section;

            if (section && classesData[section]) {
                classesData[section].forEach(function(classe) {
                    classeSelect.innerHTML += `<option value="${classe.name}">${classe.name}</option>`;
                });
            }
        }

        // Create teacher account
        function createTeacherAccount() {
            const name = document.getElementById('teacherFullName').value.trim();
            const email = document.getElementById('teacherEmail').value.trim();
            const password = document.getElementById('teacherPassword').value;
            const phone = document.getElementById('teacherPhone').value.trim();
            const section = document.getElementById('teacherSection').value;
            const classe = document.getElementById('teacherClasse').value;

            if (!name || !email || !password || !section || !classe) {
                alert('❌ Veuillez remplir tous les champs obligatoires');
                return;
            }

            if (password.length < 6) {
                alert('❌ Le mot de passe doit contenir au moins 6 caractères');
                return;
            }

            // Code enseignant : EDF-KA-T-<CLASSE>-****
            const prefix = `${SCHOOL_CODE}-T-${classe}`;

            generateUniqueCode('teachers', prefix).then(function(code) {
                // Create Firebase auth account
                return auth.createUserWithEmailAndPassword(email, password).then(function(userCredential) {
                    const userId = userCredential.user.uid;

                    // Save teacher data
                    const teacherData = {
                        id: userId,
                        name: name,
                        email: email,
                        phone: phone,
                        section: section,
                        classe: classe,
                        code: code,
                        schoolCode: SCHOOL_CODE,
                        role: 'teacher',
                        createdAt: new Date().toISOString(),
                        createdBy: getDirectorName()
                    };

                    const rtdbRef = database.ref('teachers/' + userId);
                    return rtdbRef.set(teacherData).then(function() {
                        return firestore.collection('teachers').doc(userId).set(teacherData);
                    });
                });
            }).then(function() {
                document.getElementById('accountSuccessAlert').classList.remove('hidden');
                setTimeout(function() {
                    document.getElementById('accountSuccessAlert').classList.add('hidden');
                }, 3000);

                // Clear form
                document.getElementById('teacherFullName').value = '';
                document.getElementById('teacherEmail').value = '';
                document.getElementById('teacherPassword').value = '';
                document.getElementById('teacherPhone').value = '';
                document.getElementById('teacherSection').value = '';
                document.getElementById('teacherClasse').value = '';
                document.getElementById('teacherClasse').disabled = true;

                // Add activity
                addActivity(`Compte enseignant créé: ${name}`);
                
                loadTeachersList();
            }).catch(function(error) {
                alert('❌ Erreur: ' + error.message);
            });
        }

        // Load teachers list
        function loadTeachersList() {
            const tbody = document.getElementById('teachersTableBody');
            database.ref('teachers').once('value').then(function(snapshot) {
                if (!snapshot.exists()) {
                    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; color: #6b7280;">Aucun enseignant trouvé</td></tr>';
                    return;
                }

                let html = '';
                snapshot.forEach(function(child) {
                    const teacher = child.val();
                    html += `
                        <tr>
                            <td>${teacher.name}</td>
                            <td>${teacher.email}</td>
                            <td>${teacher.classe || 'Non assigné'}</td>
                            <td>${teacher.phone || 'Non renseigné'}</td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn btn-small btn-warning" onclick="editTeacher('${child.key}')">✏️</button>
                                    <button class="btn btn-small btn-primary" onclick="toggleTeacherStatus('${child.key}')">
                                        ${teacher.status === 'blocked' ? '🔓' : '🔒'}
                                    </button>
                                    <button class="btn btn-small btn-danger" onclick="deleteTeacher('${child.key}')">🗑️</button>
                                </div>
                            </td>
                        </tr>
                    `;
                });

                tbody.innerHTML = html;
            });
        }

        // Create secretary account
        function createSecretaryAccount() {
            const name = document.getElementById('secretaryFullName').value.trim();
            const email = document.getElementById('secretaryEmail').value.trim();
            const password = document.getElementById('secretaryPassword').value;
            const phone = document.getElementById('secretaryPhone').value.trim();
            const role = document.getElementById('secretaryRole').value;
            const department = document.getElementById('secretaryDepartment').value;

            if (!name || !email || !password || !role || !department) {
                alert('❌ Veuillez remplir tous les champs obligatoires');
                return;
            }

            if (password.length < 6) {
                alert('❌ Le mot de passe doit contenir au moins 6 caractères');
                return;
            }

            // Code secrétaire : EDF-KA-S-<DEPARTMENT>-****
            const prefix = `${SCHOOL_CODE}-S-${department.substring(0, 3).toUpperCase()}`;

            generateUniqueCode('secretaries', prefix).then(function(code) {
                // Create Firebase auth account
                return auth.createUserWithEmailAndPassword(email, password).then(function(userCredential) {
                    const userId = userCredential.user.uid;

                    // Save secretary data
                    const secretaryData = {
                        id: userId,
                        name: name,
                        email: email,
                        phone: phone,
                        role: role,
                        department: department,
                        code: code,
                        schoolCode: SCHOOL_CODE,
                        userRole: 'secretary',
                        createdAt: new Date().toISOString(),
                        createdBy: getDirectorName(),
                        status: 'active'
                    };

                    const rtdbRef = database.ref('secretaries/' + userId);
                    return rtdbRef.set(secretaryData).then(function() {
                        return firestore.collection('secretaries').doc(userId).set(secretaryData);
                    });
                });
            }).then(function() {
                document.getElementById('secretarySuccessAlert').classList.remove('hidden');
                setTimeout(function() {
                    document.getElementById('secretarySuccessAlert').classList.add('hidden');
                }, 3000);

                // Clear form
                document.getElementById('secretaryFullName').value = '';
                document.getElementById('secretaryEmail').value = '';
                document.getElementById('secretaryPassword').value = '';
                document.getElementById('secretaryPhone').value = '';
                document.getElementById('secretaryRole').value = 'secretary';
                document.getElementById('secretaryDepartment').value = 'administration';

                // Add activity
                addActivity(`Compte secrétaire créé: ${name} (${role})`);

                loadSecretariesList();
            }).catch(function(error) {
                alert('❌ Erreur: ' + error.message);
            });
        }

        // Load secretaries list
        function loadSecretariesList() {
            const tbody = document.getElementById('secretariesTableBody');
            database.ref('secretaries').once('value').then(function(snapshot) {
                if (!snapshot.exists()) {
                    tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #6b7280;">Aucun secrétaire trouvé</td></tr>';
                    return;
                }

                let html = '';
                snapshot.forEach(function(child) {
                    const secretary = child.val();
                    const roleDisplay = secretary.role === 'secretary' ? 'Secrétaire' :
                                      secretary.role === 'accountant' ? 'Comptable' :
                                      secretary.role === 'admin-assistant' ? 'Assistant Admin' : secretary.role;

                    const departmentDisplay = secretary.department === 'administration' ? 'Administration' :
                                            secretary.department === 'finance' ? 'Finance' :
                                            secretary.department === 'secretariat' ? 'Secrétariat' :
                                            secretary.department === 'direction' ? 'Direction' : secretary.department;

                    html += `
                        <tr>
                            <td>${secretary.name}</td>
                            <td>${secretary.email}</td>
                            <td>${roleDisplay}</td>
                            <td>${departmentDisplay}</td>
                            <td>${secretary.phone || 'Non renseigné'}</td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn btn-small btn-warning" onclick="editSecretary('${child.key}')">✏️</button>
                                    <button class="btn btn-small btn-primary" onclick="toggleSecretaryStatus('${child.key}')">
                                        ${secretary.status === 'blocked' ? '🔓' : '🔒'}
                                    </button>
                                    <button class="btn btn-small btn-danger" onclick="deleteSecretary('${child.key}')">🗑️</button>
                                </div>
                            </td>
                        </tr>
                    `;
                });

                tbody.innerHTML = html;
            });
        }

        // Edit secretary
        function editSecretary(secretaryId) {
            alert('✏️ Fonctionnalité de modification en développement pour le secrétaire: ' + secretaryId);
        }

        // Toggle secretary status
        function toggleSecretaryStatus(secretaryId) {
            database.ref('secretaries/' + secretaryId).once('value').then(function(snapshot) {
                const secretary = snapshot.val();
                const newStatus = secretary.status === 'blocked' ? 'active' : 'blocked';

                database.ref('secretaries/' + secretaryId + '/status').set(newStatus).then(function() {
                    addActivity(`Statut secrétaire modifié: ${secretary.name} (${newStatus})`);
                    loadSecretariesList();
                });
            });
        }

        // Delete secretary
        function deleteSecretary(secretaryId) {
            if (!confirm('Êtes-vous sûr de vouloir supprimer ce compte secrétaire ? Cette action est irréversible.')) {
                return;
            }

            database.ref('secretaries/' + secretaryId).once('value').then(function(snapshot) {
                const secretary = snapshot.val();

                // Delete from RTDB
                database.ref('secretaries/' + secretaryId).remove().then(function() {
                    // Delete from Firestore
                    firestore.collection('secretaries').doc(secretaryId).delete().then(function() {
                        // Delete auth account
                        // Note: This would require admin SDK, for now we'll just mark as deleted
                        addActivity(`Compte secrétaire supprimé: ${secretary.name}`);
                        loadSecretariesList();
                    });
                });
            });
        }

        // Update student niveaux
        function updateStudentNiveaux() {
            const section = document.getElementById('studentSection').value;
            const classeSelect = document.getElementById('studentClasse');

            classeSelect.innerHTML = '<option value="">-- Choisir --</option>';
            classeSelect.disabled = !section;

            if (section && classesData[section]) {
                classesData[section].forEach(function(classe) {
                    classeSelect.innerHTML += `<option value="${classe.name}">${classe.name}</option>`;
                });
            }
        }

        // Génère un suffixe numérique unique (4 chiffres) pour un chemin donné et un préfixe
        function generateUniqueCode(nodePath, prefix) {
            function randomPart() {
                return Math.floor(1000 + Math.random() * 9000); // 4 chiffres
            }

            function tryOnce() {
                const candidate = `${prefix}-${randomPart()}`;
                return database.ref(nodePath).orderByChild('code').equalTo(candidate).once('value')
                    .then(function(snapshot) {
                        if (snapshot.exists()) {
                            return tryOnce();
                        }
                        return candidate;
                    });
            }

            return tryOnce();
        }

        // Generate student code (EDF-KA-N1A-****)
        function generateStudentCode() {
            const section = document.getElementById('studentSection').value;
            const classe = document.getElementById('studentClasse').value;

            if (!section || !classe) {
                alert('⚠️ Veuillez d\'abord choisir la section et la classe.');
                return;
            }

            const prefix = `${SCHOOL_CODE}-${classe}`;
            generateUniqueCode('students', prefix).then(function(code) {
                document.getElementById('studentCode').value = code;
            }).catch(function(error) {
                alert('❌ Erreur lors de la génération du code élève : ' + error.message);
            });
        }

        // Create student account
        function createStudentAccount() {
            const name = document.getElementById('studentFullName').value.trim();
            const section = document.getElementById('studentSection').value;
            const classe = document.getElementById('studentClasse').value;

            if (!name || !section || !classe) {
                alert('❌ Veuillez remplir tous les champs obligatoires');
                return;
            }

            // Code élève : EDF-KA-<CLASSE>-****
            const prefix = `${SCHOOL_CODE}-${classe}`;

            generateUniqueCode('students', prefix).then(function(code) {
                const studentData = {
                    name: name,
                    code: code,
                    section: section,
                    classe: classe,
                    schoolCode: SCHOOL_CODE,
                    createdAt: new Date().toISOString(),
                    createdBy: getDirectorName(),
                    bulletinCompleted: false
                };

                const pushRef = database.ref('students').push();
                return pushRef.set(studentData).then(function() {
                    return firestore.collection('students').doc(pushRef.key).set(studentData);
                });
            }).then(function() {
                alert('✅ Élève ajouté avec succès !');
                
                // Clear form
                document.getElementById('studentFullName').value = '';
                document.getElementById('studentCode').value = '';
                document.getElementById('studentSection').value = '';
                document.getElementById('studentClasse').value = '';
                document.getElementById('studentClasse').disabled = true;

                // Add activity
                addActivity(`Élève ajouté: ${name}`);
            }).catch(function(error) {
                alert('❌ Erreur: ' + error.message);
            });
        }

        // Verify report
        function verifyReport(reportId) {
            database.ref('financial/reports/' + reportId).update({
                verified: true,
                verifiedBy: getDirectorName(),
                verifiedAt: new Date().toISOString()
            }).then(function() {
                alert('✅ Rapport vérifié avec succès !');
                loadSecretaryReports();
                loadFinancialData();
                addActivity('Rapport financier vérifié');
            });
        }

        // Verify all reports
        function verifyAllReports() {
            database.ref('financial/reports').once('value').then(function(snapshot) {
                const updates = {};
                snapshot.forEach(function(child) {
                    if (!child.val().verified) {
                        updates[child.key + '/verified'] = true;
                        updates[child.key + '/verifiedBy'] = getDirectorName();
                        updates[child.key + '/verifiedAt'] = new Date().toISOString();
                    }
                });

                database.ref('financial/reports').update(updates).then(function() {
                    alert('✅ Tous les rapports ont été vérifiés !');
                    loadSecretaryReports();
                    loadFinancialData();
                    addActivity('Tous les rapports vérifiés');
                });
            });
        }

        // Handle file upload for carousel
        function handleFileUpload(event) {
            const files = event.target.files;
            const uploadPromises = [];

            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                if (file.size > 10 * 1024 * 1024) {
                    alert(`❌ Le fichier ${file.name} dépasse 10MB`);
                    continue;
                }

                const uploadPromise = new Promise((resolve, reject) => {
                    const storageRef = storage.ref('carousel/' + Date.now() + '_' + file.name);
                    const uploadTask = storageRef.put(file);

                    uploadTask.on('state_changed',
                        function(snapshot) {
                            // Progress
                        },
                        function(error) {
                            reject(error);
                        },
                        function() {
                            uploadTask.snapshot.ref.getDownloadURL().then(function(downloadURL) {
                                carouselItems.push({
                                    url: downloadURL,
                                    name: file.name,
                                    type: file.type.startsWith('video/') ? 'video' : 'image',
                                    timestamp: new Date().toISOString()
                                });
                                resolve();
                            });
                        }
                    );
                });

                uploadPromises.push(uploadPromise);
            }

            Promise.all(uploadPromises).then(function() {
                saveCarousel();
                showCarouselMessage('✅ Contenu uploadé avec succès !');
                addActivity(`${files.length} fichier(s) ajouté(s) au carrousel`);
            }).catch(function(error) {
                alert('❌ Erreur lors de l\'upload: ' + error.message);
            });
        }

        // Load carousel
        function loadCarousel() {
            database.ref('carousel').once('value').then(function(snapshot) {
                const previewDiv = document.getElementById('carouselPreview');
                
                if (!snapshot.exists() || !snapshot.val().items) {
                    previewDiv.innerHTML = '<p style="color: #6b7280; padding: 20px; text-align: center;">Aucun contenu dans le carrousel</p>';
                    return;
                }

                carouselItems = snapshot.val().items || [];
                displayCarousel();
            });
        }

        // Save carousel
        function saveCarousel() {
            database.ref('carousel').set({
                items: carouselItems,
                lastUpdated: new Date().toISOString(),
                updatedBy: getDirectorName()
            }).then(function() {
                displayCarousel();
            });
        }

        // Display carousel
        function displayCarousel() {
            const previewDiv = document.getElementById('carouselPreview');
            
            if (carouselItems.length === 0) {
                previewDiv.innerHTML = '<p style="color: #6b7280; padding: 20px; text-align: center;">Aucun contenu dans le carrousel</p>';
                return;
            }

            let html = '';
            carouselItems.forEach(function(item, index) {
                if (item.type === 'video') {
                    html += `
                        <div class="carousel-item">
                            <video controls>
                                <source src="${item.url}" type="video/mp4">
                            </video>
                            <button class="remove-btn" onclick="removeCarouselItem(${index})">×</button>
                        </div>
                    `;
                } else {
                    html += `
                        <div class="carousel-item">
                            <img src="${item.url}" alt="${item.name}">
                            <button class="remove-btn" onclick="removeCarouselItem(${index})">×</button>
                        </div>
                    `;
                }
            });

            previewDiv.innerHTML = html;
        }

        // Remove carousel item
        function removeCarouselItem(index) {
            if (confirm('Supprimer cet élément ?')) {
                carouselItems.splice(index, 1);
                saveCarousel();
                addActivity('Élément supprimé du carrousel');
            }
        }

        // Clear carousel
        function clearCarousel() {
            if (confirm('Supprimer tout le contenu du carrousel ?')) {
                carouselItems = [];
                saveCarousel();
                addActivity('Carrousel vidé');
            }
        }

        // Show carousel message
        function showCarouselMessage(message) {
            const msgDiv = document.getElementById('carouselMessage');
            msgDiv.textContent = message;
            msgDiv.classList.remove('hidden');
            setTimeout(function() {
                msgDiv.classList.add('hidden');
            }, 3000);
        }

        // Toggle maintenance mode
        function toggleMaintenance() {
            const toggle = document.getElementById('maintenanceToggle');
            toggle.classList.toggle('active');
            const isActive = toggle.classList.contains('active');
            
            database.ref('settings/maintenance').set(isActive);
            addActivity(`Mode maintenance ${isActive ? 'activé' : 'désactivé'}`);
            alert(`Mode maintenance ${isActive ? 'activé' : 'désactivé'}`);
        }

        // Toggle registration
        function toggleRegistration() {
            const toggle = document.getElementById('registrationToggle');
            toggle.classList.toggle('active');
            const isActive = toggle.classList.contains('active');
            
            database.ref('settings/registration').set(isActive);
            addActivity(`Inscriptions ${isActive ? 'activées' : 'désactivées'}`);
        }

        // Toggle notifications
        function toggleNotifications() {
            const toggle = document.getElementById('notificationToggle');
            toggle.classList.toggle('active');
            const isActive = toggle.classList.contains('active');
            
            database.ref('settings/notifications').set(isActive);
            addActivity(`Notifications ${isActive ? 'activées' : 'désactivées'}`);
        }

        // Toggle dark mode
        function toggleDarkMode() {
            const toggle = document.getElementById('darkModeToggle');
            toggle.classList.toggle('active');
            const isActive = toggle.classList.contains('active');
            
            if (isActive) {
                document.body.style.filter = 'invert(1) hue-rotate(180deg)';
            } else {
                document.body.style.filter = 'none';
            }
            
            addActivity(`Mode sombre ${isActive ? 'activé' : 'désactivé'}`);
        }

        // Publish announcement
        function publishAnnouncement() {
            const title = document.getElementById('announcementTitle').value.trim();
            const text = document.getElementById('announcementText').value.trim();

            if (!title || !text) {
                alert('❌ Veuillez remplir le titre et le message');
                return;
            }

            const announcement = {
                title: title,
                text: text,
                date: new Date().toISOString(),
                author: getDirectorName()
            };

            database.ref('announcements').push(announcement).then(function() {
                const msgDiv = document.getElementById('announcementMessage');
                msgDiv.textContent = `📢 ${title}: ${text}`;
                msgDiv.style.display = 'block';
                
                document.getElementById('announcementTitle').value = '';
                document.getElementById('announcementText').value = '';
                
                addActivity(`Annonce publiée: ${title}`);
                alert('✅ Annonce publiée avec succès !');
            });
        }

        // Save school settings
        function saveSchoolSettings() {
            const logoFile = document.getElementById('schoolLogoUpload').files[0];
            
            if (logoFile) {
                const storageRef = storage.ref('logos/school-logo.png');
                storageRef.put(logoFile).then(function() {
                    storageRef.getDownloadURL().then(function(url) {
                        document.getElementById('schoolLogo').src = url;
                        updateSchoolInfo();
                    });
                });
            } else {
                updateSchoolInfo();
            }
        }

        // Load school settings
        function loadSchoolSettings() {
            database.ref('school/info').once('value').then(function(snapshot) {
                if (snapshot.exists()) {
                    const schoolInfo = snapshot.val();
                    document.getElementById('schoolName').value = schoolInfo.name || 'Eden Family School';
                    document.getElementById('schoolEmail').value = schoolInfo.email || 'contact@edenfamily.edu';
                    document.getElementById('schoolPhone').value = schoolInfo.phone || '+250 788 123 456';
                }
            }).catch(function(error) {
                console.error('Erreur lors du chargement des paramètres école:', error);
            });
        }

        // Add activity
        function addActivity(message) {
            const activity = {
                message: message,
                timestamp: new Date().toISOString(),
                user: getDirectorName()
            };

            database.ref('activities').push(activity);
            loadActivities();
        }

        // Logout
        function logout() {
            auth.signOut().then(function() {
                window.location.href = 'index.html';
            });
        }

        // Functions to implement (stubs)
        function editReport(reportId) {
            alert('Modification de rapport - Fonctionnalité en développement');
        }

        function editTeacher(teacherId) {
            database.ref('teachers/' + teacherId).once('value').then(function(snapshot) {
                if (!snapshot.exists()) {
                    alert('Enseignant introuvable.');
                    return;
                }

                const teacher = snapshot.val();
                const newName = prompt('Nom complet de l\'enseignant :', teacher.name || '');
                if (newName === null) return;
                const newEmail = prompt('Email de l\'enseignant :', teacher.email || '');
                if (newEmail === null) return;
                const newPhone = prompt('Téléphone :', teacher.phone || '');
                if (newPhone === null) return;
                const newClasse = prompt('Classe (ex: 1ère A Maternelle) :', teacher.classe || '');
                if (newClasse === null) return;

                const updates = {
                    name: newName.trim(),
                    email: newEmail.trim(),
                    phone: newPhone.trim(),
                    classe: newClasse.trim(),
                    updatedAt: new Date().toISOString(),
                    updatedBy: getDirectorName()
                };

                return database.ref('teachers/' + teacherId).update(updates).then(function() {
                    return firestore.collection('teachers').doc(teacherId).update(updates);
                });
            }).then(function() {
                loadTeachersList();
                addActivity('Profil enseignant modifié');
            }).catch(function(error) {
                if (error) {
                    alert('❌ Erreur lors de la modification : ' + error.message);
                }
            });
        }

        function toggleTeacherStatus(teacherId) {
            database.ref('teachers/' + teacherId).once('value').then(function(snapshot) {
                if (!snapshot.exists()) {
                    alert('Enseignant introuvable.');
                    return;
                }
                const teacher = snapshot.val();
                const currentStatus = teacher.status || 'active';
                const newStatus = currentStatus === 'blocked' ? 'active' : 'blocked';

                const confirmMsg = newStatus === 'blocked'
                    ? 'Bloquer cet enseignant ? Il ne devra plus utiliser son compte.'
                    : 'Débloquer cet enseignant et réactiver son compte ?';

                if (!confirm(confirmMsg)) return;

                const statusUpdate = {
                    status: newStatus,
                    statusUpdatedAt: new Date().toISOString(),
                    statusUpdatedBy: getDirectorName()
                };

                return database.ref('teachers/' + teacherId).update(statusUpdate).then(function() {
                    return firestore.collection('teachers').doc(teacherId).update(statusUpdate);
                });
            }).then(function() {
                loadTeachersList();
            }).catch(function(error) {
                if (error) {
                    alert('❌ Erreur lors du changement de statut : ' + error.message);
                }
            });
        }

        function deleteTeacher(teacherId) {
            if (confirm('Supprimer cet enseignant ?')) {
                database.ref('teachers/' + teacherId).remove().then(function() {
                    return firestore.collection('teachers').doc(teacherId).delete();
                }).then(function() {
                    alert('✅ Enseignant supprimé !');
                    loadTeachersList();
                    addActivity('Enseignant supprimé');
                }).catch(function(error) {
                    alert('❌ Erreur lors de la suppression : ' + error.message);
                });
            }
        }