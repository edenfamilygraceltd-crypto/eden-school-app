// Secure Firebase Configuration
// Load configuration from secure source
if (typeof window !== 'undefined' && window.FIREBASE_CONFIG) {
    firebase.initializeApp(window.FIREBASE_CONFIG);
} else {
    // Fallback for development - in production, config should be loaded securely
    console.warn('Firebase config not loaded securely. Using fallback configuration.');
    const firebaseConfig = {
        apiKey: "AIzaSyCx6kmJ59x0tLt4vh_3czvEEQrtw4aWFHs",
        authDomain: "edendatabase-7e1ed.firebaseapp.com",
        databaseURL: "https://edendatabase-7e1ed-default-rtdb.firebaseio.com",
        projectId: "edendatabase-7e1ed",
        storageBucket: "edendatabase-7e1ed.firebasestorage.app",
        messagingSenderId: "147248399046",
        appId: "1:147248399046:web:d0b433e755772bbe718dc7",
        measurementId: "G-XB192PCMV7"
    };
    firebase.initializeApp(firebaseConfig);
}
const db = firebase.database();
const storage = firebase.storage();

// Variables globales
let bulletins = [];
let students = [];
let classes = new Set();
let selectedBulletinId = null;

// Initialisation de l'application
document.addEventListener('DOMContentLoaded', function() {
    loadStudents();
    loadBulletins();
    setupEventListeners();
});

// Charger la liste des élèves
async function loadStudents() {
    try {
        const snapshot = await db.ref('students').once('value');
        students = [];
        const studentSelect = document.getElementById('filterStudent');
        
        // Sauvegarder la sélection actuelle
        const selectedValue = studentSelect.value;
        studentSelect.innerHTML = '<option value="">Tous les élèves</option>';
        
        snapshot.forEach(child => {
            const student = { id: child.key, ...child.val() };
            students.push(student);
            classes.add(student.classe);
            
            // Ajouter l'option au select des élèves
            const option = document.createElement('option');
            option.value = student.id;
            option.textContent = `${student.nom} ${student.prenom} (${student.classe})`;
            studentSelect.appendChild(option);
        });
        
        // Restaurer la sélection si elle existe toujours
        if (selectedValue && studentSelect.querySelector(`option[value="${selectedValue}"]`)) {
            studentSelect.value = selectedValue;
        }

        // Mettre à jour le select des classes
        const classSelect = document.getElementById('filterClass');
        const selectedClass = classSelect.value;
        classSelect.innerHTML = '<option value="">Toutes les classes</option>';
        
        Array.from(classes).sort().forEach(className => {
            const option = document.createElement('option');
            option.value = className;
            option.textContent = className;
            classSelect.appendChild(option);
        });
        
        if (selectedClass && classSelect.querySelector(`option[value="${selectedClass}"]`)) {
            classSelect.value = selectedClass;
        }
        
    } catch (error) {
        console.error('Erreur lors du chargement des élèves:', error);
        showAlert('Erreur lors du chargement des élèves', 'error');
    }
}

// Charger la liste des bulletins
async function loadBulletins() {
    try {
        showLoading(true);
        const snapshot = await db.ref('bulletins').once('value');
        bulletins = [];
        
        snapshot.forEach(child => {
            bulletins.push({
                id: child.key,
                ...child.val()
            });
        });
        
        applyFilters();
    } catch (error) {
        console.error('Erreur lors du chargement des bulletins:', error);
        showAlert('Erreur lors du chargement des bulletins', 'error');
    } finally {
        showLoading(false);
    }
}

// Appliquer les filtres
function applyFilters() {
    const classFilter = document.getElementById('filterClass').value;
    const studentFilter = document.getElementById('filterStudent').value;
    const trimesterFilter = document.getElementById('filterTrimester').value;
    
    let filtered = [...bulletins];
    
    if (classFilter) {
        filtered = filtered.filter(b => b.student?.classe === classFilter);
    }
    
    if (studentFilter) {
        filtered = filtered.filter(b => b.studentId === studentFilter);
    }
    
    if (trimesterFilter) {
        filtered = filtered.filter(b => b.trimestre?.toString() === trimesterFilter);
    }
    
    displayBulletins(filtered);
}

// Afficher la liste des bulletins
function displayBulletins(bulletinsToShow) {
    const bulletinsList = document.getElementById('bulletinsList');
    bulletinsList.innerHTML = '';
    
    if (bulletinsToShow.length === 0) {
        bulletinsList.innerHTML = `
            <div class="text-center py-12">
                <i class="fas fa-inbox text-gray-400 text-4xl mb-2"></i>
                <p class="text-gray-500">Aucun bulletin trouvé</p>
            </div>
        `;
        return;
    }
    
    bulletinsToShow.sort((a, b) => {
        // Trier par date décroissante
        return new Date(b.date) - new Date(a.date);
    });
    
    bulletinsToShow.forEach(bulletin => {
        const student = students.find(s => s.code === bulletin.studentId) || {};
        const date = new Date(bulletin.date);
        const formattedDate = date.toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
        
        const card = document.createElement('li');
        card.className = 'p-4 hover:bg-gray-50 bulletin-card';
        card.innerHTML = `
            <div class="flex flex-col md:flex-row md:items-center md:justify-between">
                <div class="flex-1">
                    <div class="flex items-center">
                        <div class="flex-shrink-0 h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                            <i class="fas ${bulletin.type === 'conduite' ? 'fa-clipboard-check text-blue-600' : 'fa-book text-green-600'}"></i>
                        </div>
                        <div class="ml-4">
                            <div class="text-sm font-medium text-gray-900">
                                ${student.nom || 'Inconnu'} ${student.prenom || ''}
                                <span class="text-sm text-gray-500 ml-2">${student.classe || ''}</span>
                            </div>
                            <div class="text-sm text-gray-500">
                                ${bulletin.type === 'conduite' ? 'Bulletin de conduite' : 'Bulletin scolaire'} - 
                                ${bulletin.trimestre ? `${bulletin.trimestre}ème trimestre` : 'Sans trimestre'}
                            </div>
                        </div>
                    </div>
                </div>
                <div class="mt-4 flex-shrink-0 flex md:mt-0 md:ml-4">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        bulletin.statut === 'finalisé' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                    } mr-2">
                        ${bulletin.statut === 'finalisé' ? 'Finalisé' : 'Brouillon'}
                    </span>
                    <span class="text-sm text-gray-500">${formattedDate}</span>
                </div>
                <div class="mt-4 flex-shrink-0 flex space-x-3 md:mt-0 md:ml-4">
                    <button onclick="downloadPDF('${bulletin.id}')" class="inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                        <i class="fas fa-download mr-1"></i> Télécharger
                    </button>
                    <button onclick="confirmDelete('${bulletin.id}')" class="inline-flex items-center px-3 py-1.5 border border-gray-300 shadow-sm text-xs font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500">
                        <i class="fas fa-trash-alt mr-1"></i> Supprimer
                    </button>
                </div>
            </div>
        `;
        
        bulletinsList.appendChild(card);
    });
}

// Télécharger un bulletin PDF
async function downloadPDF(bulletinId) {
    try {
        showLoading(true);
        console.log('Début du téléchargement du bulletin:', bulletinId);
        
        // Trouver le bulletin dans la liste locale
        const bulletin = bulletins.find(b => b.id === bulletinId);
        if (!bulletin) {
            console.error('Bulletin non trouvé dans la liste locale');
            // Essayer de récupérer depuis Firebase directement
            const snapshot = await db.ref(`bulletins/${bulletinId}`).once('value');
            if (!snapshot.exists()) {
                throw new Error('Bulletin non trouvé dans la base de données');
            }
            Object.assign(bulletin, snapshot.val());
        }
        
        // Récupérer les informations de l'élève
        let student = students.find(s => s.code === bulletin.studentId);
        if (!student) {
            console.log('Élève non trouvé dans la liste locale, récupération depuis Firebase...');
            const studentSnapshot = await db.ref(`students/${bulletin.studentId}`).once('value');
            if (studentSnapshot.exists()) {
                student = { id: studentSnapshot.key, ...studentSnapshot.val() };
                students.push(student); // Mettre en cache pour les prochains appels
            } else {
                student = { nom: 'Inconnu', prenom: '', classe: 'NC' };
            }
        }
        
        // Créer un nouveau document PDF
        const { jsPDF } = window.jspdf;
        const pdf = new jsPDF('p', 'mm', 'a4');
        const pageWidth = pdf.internal.pageSize.getWidth();
        const pageHeight = pdf.internal.pageSize.getHeight();
        
        // Charger les logos avec gestion d'erreur
        let logo1, logo2;
        try {
            logo1 = await loadImage('asset/img/logo_eden.png');
            console.log('Logo 1 chargé avec succès');
        } catch (e) {
            console.warn('Erreur lors du chargement du logo 1:', e);
            // Essayer avec un chemin alternatif
            try {
                logo1 = await loadImage('/asset/img/logo_eden.png');
                console.log('Logo 1 chargé avec le chemin alternatif');
            } catch (e2) {
                console.error('Impossible de charger le logo 1');
            }
        }

        try {
            logo2 = await loadImage('asset/img/logo_rwanda.png');
            console.log('Logo 2 chargé avec succès');
        } catch (e) {
            console.warn('Erreur lors du chargement du logo 2:', e);
            // Essayer avec un chemin alternatif
            try {
                logo2 = await loadImage('/asset/img/logo_rwanda.png');
                console.log('Logo 2 chargé avec le chemin alternatif');
            } catch (e2) {
                console.error('Impossible de charger le logo 2');
            }
        }
        
        // En-tête avec les logos
        pdf.setFontSize(20);
        pdf.setFont('helvetica', 'bold');
        
        // Logo de gauche
        if (logo1) {
            const logo1Width = 40;
            const logo1Height = (logo1.height * logo1Width) / logo1.width;
            pdf.addImage(logo1, 'PNG', 20, 15, logo1Width, logo1Height);
        }
        
        // Titre
        pdf.setFontSize(18);
        pdf.text('EDEN FAMILY SCHOOL', pageWidth / 2, 25, { align: 'center' });
        pdf.setFontSize(12);
        pdf.setFont('helvetica', 'normal');
        pdf.text('Établissement d\'Enseignement Primaire et Secondaire', pageWidth / 2, 32, { align: 'center' });
        
        // Logo de droite
        if (logo2) {
            const logo2Width = 40;
            const logo2Height = (logo2.height * logo2Width) / logo2.width;
            pdf.addImage(logo2, 'PNG', pageWidth - 60, 15, logo2Width, logo2Height);
        }
        
        // Ligne de séparation
        pdf.setDrawColor(200, 200, 200);
        pdf.setLineWidth(0.5);
        pdf.line(20, 50, pageWidth - 20, 50);
        
        // Informations de l'élève
        pdf.setFont('helvetica', 'bold');
        pdf.setFontSize(14);
        pdf.text('BULLETIN DE NOTES', 20, 65);
        
        pdf.setFont('helvetica', 'normal');
        pdf.setFontSize(12);
        pdf.text(`Élève: ${student.nom || ''} ${student.prenom || ''}`, 20, 80);
        pdf.text(`Classe: ${student.classe || ''}`, 20, 90);
        pdf.text(`Trimestre: ${bulletin.trimestre || 'N/A'}`, 20, 100);
        pdf.text(`Date: ${new Date(bulletin.date).toLocaleDateString('fr-FR')}`, 20, 110);
        
        // Tableau des notes
        if (bulletin.type !== 'conduite' && bulletin.notes) {
            let y = 130;
            const startY = y;
            
            // En-tête du tableau
            pdf.setFillColor(59, 130, 246);
            pdf.setTextColor(255, 255, 255);
            pdf.rect(20, y, pageWidth - 40, 10, 'F');
            pdf.text('Matière', 25, y + 7);
            pdf.text('T1', 120, y + 7, { align: 'center' });
            pdf.text('T2', 150, y + 7, { align: 'center' });
            pdf.text('T3', 180, y + 7, { align: 'center' });
            
            y += 10;
            
            // Lignes des matières
            pdf.setTextColor(0, 0, 0);
            let line = 0;
            
            for (const [matiere, notes] of Object.entries(bulletin.notes)) {
                if (line % 2 === 0 && line > 0) {
                    pdf.setFillColor(240, 240, 240);
                    pdf.rect(20, y - 1, pageWidth - 40, 10, 'F');
                } else {
                    pdf.setFillColor(255, 255, 255);
                }
                
                pdf.text(matiere, 25, y + 7);
                
                // Notes par trimestre
                for (let t = 1; t <= 3; t++) {
                    const trim = notes[`trim${t}`];
                    if (trim) {
                        const x = 120 + (t - 1) * 30;
                        const noteTest = trim.test || 0;
                        const noteExam = trim.examen || 0;
                        const moyenne = ((noteTest + noteExam) / 2).toFixed(2);
                        
                        pdf.text(`${moyenne}`, x, y + 7, { align: 'center' });
                    }
                }
                
                y += 10;
                line++;
                
                // Nouvelle page si nécessaire
                if (y > pageHeight - 50) {
                    pdf.addPage();
                    y = 30;
                }
            }
            
            // Cadre du tableau
            pdf.setDrawColor(0, 0, 0);
            pdf.rect(20, startY, pageWidth - 40, y - startY);
            
            // Lignes verticales
            for (let x = 20; x <= pageWidth - 20; x += 30) {
                pdf.line(x, startY, x, y);
            }
        } else if (bulletin.type === 'conduite') {
            // Affichage des notes de conduite
            let y = 130;
            pdf.setFont('helvetica', 'bold');
            pdf.text('ÉVALUATION DE LA CONDUITE', 20, y);
            y += 15;
            
            pdf.setFont('helvetica', 'normal');
            for (const [critere, note] of Object.entries(bulletin.notes || {})) {
                pdf.text(`• ${critere}: ${note}`, 25, y);
                y += 8;
                
                if (y > pageHeight - 50) {
                    pdf.addPage();
                    y = 30;
                }
            }
        }
        
        // Commentaire et décision
        if (bulletin.commentaire) {
            let y = pageHeight - 50;
            
            pdf.setFont('helvetica', 'bold');
            pdf.text('COMMENTAIRES ET DÉCISION', 20, y);
            y += 10;
            
            pdf.setFont('helvetica', 'normal');
            const splitText = pdf.splitTextToSize(bulletin.commentaire, pageWidth - 40);
            pdf.text(splitText, 20, y);
            y += splitText.length * 7;
            
            // Décision
            if (bulletin.decision) {
                y += 10;
                pdf.setFont('helvetica', 'bold');
                pdf.text('DÉCISION:', 20, y);
                
                if (bulletin.decision.promu) {
                    y += 7;
                    pdf.setFont('helvetica', 'normal');
                    pdf.text('✓ Admis(e) en classe supérieure', 30, y);
                }
                
                if (bulletin.decision.repeter) {
                    y += 7;
                    pdf.setFont('helvetica', 'normal');
                    pdf.text(`✓ Doit refaire la ${bulletin.decision.repeter}`, 30, y);
                }
                
                if (bulletin.decision.exclu) {
                    y += 7;
                    pdf.setFont('helvetica', 'normal');
                    pdf.text('✓ Exclu(e) de l\'établissement', 30, y);
                }
            }
        }
        
        // Pied de page
        pdf.setFontSize(10);
        pdf.setTextColor(100, 100, 100);
        pdf.text('Eden Family School - Tous droits réservés', pageWidth / 2, pageHeight - 10, { align: 'center' });
        
        // Générer le nom du fichier
        const studentName = `${student.nom || 'Inconnu'}_${student.prenom || ''}`.trim().replace(/\s+/g, '_');
        const fileName = `Bulletin_${studentName}_T${bulletin.trimestre || 'X'}_${new Date().toISOString().split('T')[0]}.pdf`;
        
        // Sauvegarder le PDF
        console.log('Téléchargement du PDF:', fileName);
        try {
            pdf.save(fileName);
            console.log('PDF généré avec succès');
            showAlert('Bulletin téléchargé avec succès', 'success');
            
            // Enregistrer le téléchargement dans les logs
            try {
                await db.ref(`downloadLogs/${bulletinId}`).set({
                    timestamp: firebase.database.ServerValue.TIMESTAMP,
                    bulletinId: bulletinId,
                    studentId: student.id,
                    fileName: fileName
                });
            } catch (logError) {
                console.warn('Erreur lors de l\'enregistrement du log de téléchargement:', logError);
            }
        } catch (saveError) {
            console.error('Erreur lors de la sauvegarde du PDF:', saveError);
            // Essayer une méthode alternative de sauvegarde
            try {
                const blob = pdf.output('blob');
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(url);
                showAlert('Bulletin téléchargé avec succès (méthode alternative)', 'success');
            } catch (altError) {
                console.error('Échec de la méthode alternative de téléchargement:', altError);
                throw new Error('Impossible de télécharger le PDF. Veuillez réessayer.');
            }
        }
    } catch (error) {
        console.error('Erreur lors de la génération du PDF:', error);
        showAlert('Erreur lors de la génération du PDF: ' + error.message, 'error');
    } finally {
        showLoading(false);
    }
}

// Charger une image depuis une URL
function loadImage(url) {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.crossOrigin = 'Anonymous';
        img.onload = () => resolve(img);
        img.onerror = (error) => {
            console.warn('Erreur de chargement de l\'image:', url, error);
            resolve(null);
        };
        img.src = url;
    });
}

// Confirmer la suppression d'un bulletin
function confirmDelete(bulletinId) {
    selectedBulletinId = bulletinId;
    const modal = document.getElementById('deleteModal');
    modal.classList.remove('hidden');
    modal.setAttribute('aria-hidden', 'false');
}

// Supprimer un bulletin
async function deleteBulletin() {
    if (!selectedBulletinId) return;
    
    try {
        showLoading(true);
        await db.ref(`bulletins/${selectedBulletinId}`).remove();
        
        // Supprimer le bulletin de la liste locale
        const index = bulletins.findIndex(b => b.id === selectedBulletinId);
        if (index !== -1) {
            bulletins.splice(index, 1);
        }
        
        // Mettre à jour l'affichage
        applyFilters();
        showAlert('Bulletin supprimé avec succès', 'success');
    } catch (error) {
        console.error('Erreur lors de la suppression du bulletin:', error);
        showAlert('Erreur lors de la suppression du bulletin', 'error');
    } finally {
        closeDeleteModal();
        showLoading(false);
    }
}

// Fermer la modale de suppression
function closeDeleteModal() {
    const modal = document.getElementById('deleteModal');
    modal.classList.add('hidden');
    modal.setAttribute('aria-hidden', 'true');
    selectedBulletinId = null;
}

// Afficher/masquer l'indicateur de chargement
function showLoading(show) {
    const loadingElement = document.getElementById('loading');
    if (loadingElement) {
        loadingElement.style.display = show ? 'flex' : 'none';
    }
}

// Afficher une alerte
function showAlert(message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `fixed top-4 right-4 p-4 rounded-md ${
        type === 'error' ? 'bg-red-100 text-red-700' : 
        type === 'success' ? 'bg-green-100 text-green-700' : 
        'bg-blue-100 text-blue-700'
    } shadow-lg z-50 max-w-md`;
    
    alert.innerHTML = `
        <div class="flex">
            <div class="flex-shrink-0">
                ${type === 'error' ? '❌' : type === 'success' ? '✅' : 'ℹ️'}
            </div>
            <div class="ml-3">
                <p class="text-sm font-medium">${message}</p>
            </div>
            <div class="ml-4 flex-shrink-0 flex">
                <button onclick="this.parentElement.parentElement.parentElement.remove()" class="inline-flex text-gray-400 hover:text-gray-500 focus:outline-none">
                    <span class="sr-only">Fermer</span>
                    <i class="fas fa-times"></i>
                </button>
            </div>
        </div>
    `;
    
    document.body.appendChild(alert);
    
    // Supprimer l'alerte après 5 secondes
    setTimeout(() => {
        if (alert.parentNode === document.body) {
            document.body.removeChild(alert);
        }
    }, 5000);
}

// Configurer les écouteurs d'événements
function setupEventListeners() {
    // Filtres
    document.getElementById('applyFilters').addEventListener('click', applyFilters);
    
    // Modale de suppression
    document.getElementById('confirmDelete').addEventListener('click', deleteBulletin);
    document.getElementById('cancelDelete').addEventListener('click', closeDeleteModal);
    
    // Fermer la modale en cliquant en dehors
    document.getElementById('deleteModal').addEventListener('click', (e) => {
        if (e.target === document.getElementById('deleteModal')) {
            closeDeleteModal();
        }
    });
    
    // Rafraîchir la liste des élèves quand la classe change
    document.getElementById('filterClass').addEventListener('change', function() {
        const selectedClass = this.value;
        const studentSelect = document.getElementById('filterStudent');
        const selectedValue = studentSelect.value;
        
        studentSelect.innerHTML = '<option value="">Tous les élèves</option>';
        
        students
            .filter(student => !selectedClass || student.classe === selectedClass)
            .forEach(student => {
                const option = document.createElement('option');
                option.value = student.id;
                option.textContent = `${student.nom} ${student.prenom} (${student.classe})`;
                studentSelect.appendChild(option);      
                
                // Restaurer la sélection si elle existe toujours
                if (selectedValue === student.id) {
                    studentSelect.value = selectedValue;
                }
            });
    });
}

// Rendre les fonctions disponibles globalement
window.downloadPDF = downloadPDF;
window.confirmDelete = confirmDelete;
