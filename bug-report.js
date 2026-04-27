/**
 * Système de Signalement de Bug/Suggestion
 * Module réutilisable pour toute l'application
 */

class BugReportSystem {
  constructor(firebaseConfig = null) {
    this.firebaseConfig = firebaseConfig;
    this.db = firebase.firestore ? firebase.firestore() : null;
    this.auth = firebase.auth ? firebase.auth() : null;
    this.initModal();
  }

  /**
   * Initialise le modal de rapport
   */
  initModal() {
    // Créer le HTML du modal s'il n'existe pas
    if (!document.getElementById('bugReportModal')) {
      document.body.insertAdjacentHTML('beforeend', this.getModalHTML());
    }

    // Ajouter les styles s'ils n'existent pas
    if (!document.getElementById('bugReportStyles')) {
      const styleElement = document.createElement('style');
      styleElement.id = 'bugReportStyles';
      styleElement.textContent = this.getModalStyles();
      document.head.appendChild(styleElement);
    }

    this.attachEventListeners();
  }

  /**
   * Retourne le HTML du modal
   */
  getModalHTML() {
    return `
      <div id="bugReportModal" class="bug-report-modal">
        <div class="bug-report-modal-content">
          <div class="bug-report-header">
            <h2>📋 Signaler un Bug ou une Suggestion</h2>
            <button class="bug-report-close" onclick="bugReportSystem.closeModal()">&times;</button>
          </div>

          <form id="bugReportForm" class="bug-report-form">
            <!-- Type de Rapport -->
            <div class="form-group">
              <label for="reportType">Type de Rapport</label>
              <select id="reportType" name="type" required>
                <option value="">-- Sélectionner --</option>
                <option value="bug">🐛 Bug</option>
                <option value="suggestion">💡 Suggestion</option>
                <option value="improvement">⚡ Amélioration</option>
                <option value="other">📌 Autre</option>
              </select>
            </div>

            <!-- Titre -->
            <div class="form-group">
              <label for="reportTitle">Titre</label>
              <input 
                type="text" 
                id="reportTitle" 
                name="title" 
                placeholder="Ex: Erreur lors de la connexion"
                required
                maxlength="100"
              >
              <small class="char-count">0/100</small>
            </div>

            <!-- Description -->
            <div class="form-group">
              <label for="reportDescription">Description</label>
              <textarea 
                id="reportDescription" 
                name="description" 
                placeholder="Décrivez le problème ou votre suggestion en détail..."
                required
                maxlength="1000"
                rows="5"
              ></textarea>
              <small class="char-count">0/1000</small>
            </div>

            <!-- Priorité -->
            <div class="form-group">
              <label for="reportPriority">Priorité</label>
              <select id="reportPriority" name="priority" required>
                <option value="low">🟢 Basse</option>
                <option value="medium" selected>🟡 Moyenne</option>
                <option value="high">🔴 Haute</option>
                <option value="critical">⚫ Critique</option>
              </select>
            </div>

            <!-- Navigateur/Appareil (optionnel) -->
            <div class="form-group">
              <label for="reportBrowser">Navigateur/Appareil (optionnel)</label>
              <input 
                type="text" 
                id="reportBrowser" 
                name="browser" 
                placeholder="Ex: Chrome, Firefox, Mobile..."
                maxlength="100"
              >
            </div>

            <!-- Fichier attaché (optionnel) -->
            <div class="form-group">
              <label for="reportAttachment">Ajouter une capture d'écran (optionnel)</label>
              <input 
                type="file" 
                id="reportAttachment" 
                name="attachment"
                accept="image/*"
              >
              <small>Taille max: 5MB</small>
            </div>

            <!-- Boutons -->
            <div class="form-actions">
              <button type="button" class="btn-cancel" onclick="bugReportSystem.closeModal()">
                ❌ Annuler
              </button>
              <button type="submit" class="btn-submit" id="submitBugBtn">
                ✉️ Envoyer le Rapport
              </button>
            </div>

            <!-- Message de statut -->
            <div id="statusMessage" class="status-message" style="display: none;"></div>
          </form>
        </div>
      </div>

      <!-- Bouton flottant pour ouvrir le formulaire -->
      <button id="bugReportBtn" class="bug-report-btn" onclick="bugReportSystem.openModal()" title="Signaler un problème">
        🐛
      </button>
    `;
  }

  /**
   * Retourne les styles CSS du modal
   */
  getModalStyles() {
    return `
      /* Bouton flottant */
      .bug-report-btn {
        position: fixed;
        bottom: 30px;
        right: 30px;
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        font-size: 28px;
        cursor: pointer;
        box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
        transition: all 0.3s ease;
        z-index: 999;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .bug-report-btn:hover {
        transform: scale(1.1);
        box-shadow: 0 8px 25px rgba(102, 126, 234, 0.6);
      }

      .bug-report-btn:active {
        transform: scale(0.95);
      }

      /* Modal */
      .bug-report-modal {
        display: none;
        position: fixed;
        z-index: 10000;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        animation: fadeIn 0.3s ease;
      }

      .bug-report-modal.show {
        display: flex;
        align-items: center;
        justify-content: center;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      .bug-report-modal-content {
        background-color: white;
        padding: 30px;
        border-radius: 15px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
        max-width: 600px;
        width: 90%;
        animation: slideIn 0.3s ease;
        max-height: 90vh;
        overflow-y: auto;
      }

      @keyframes slideIn {
        from {
          transform: translateY(-50px);
          opacity: 0;
        }
        to {
          transform: translateY(0);
          opacity: 1;
        }
      }

      /* Header du modal */
      .bug-report-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 25px;
        border-bottom: 2px solid #667eea;
        padding-bottom: 15px;
      }

      .bug-report-header h2 {
        color: #333;
        font-size: 22px;
        margin: 0;
      }

      .bug-report-close {
        background: none;
        border: none;
        font-size: 28px;
        cursor: pointer;
        color: #666;
        transition: color 0.3s ease;
      }

      .bug-report-close:hover {
        color: #333;
      }

      /* Formulaire */
      .bug-report-form {
        display: flex;
        flex-direction: column;
        gap: 20px;
      }

      .form-group {
        display: flex;
        flex-direction: column;
      }

      .form-group label {
        font-weight: 600;
        color: #333;
        margin-bottom: 8px;
        font-size: 14px;
      }

      .form-group input[type="text"],
      .form-group input[type="file"],
      .form-group select,
      .form-group textarea {
        padding: 12px 15px;
        border: 2px solid #e0e0e0;
        border-radius: 8px;
        font-family: 'Poppins', Arial, sans-serif;
        font-size: 14px;
        transition: all 0.3s ease;
        background-color: #f9f9f9;
      }

      .form-group input[type="text"]:focus,
      .form-group select:focus,
      .form-group textarea:focus {
        outline: none;
        border-color: #667eea;
        background-color: white;
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
      }

      .form-group textarea {
        resize: vertical;
        min-height: 120px;
      }

      .form-group small {
        font-size: 12px;
        color: #999;
        margin-top: 5px;
      }

      /* Compteur de caractères */
      .char-count {
        align-self: flex-end;
      }

      /* Boutons */
      .form-actions {
        display: flex;
        gap: 15px;
        margin-top: 20px;
      }

      .btn-cancel,
      .btn-submit {
        flex: 1;
        padding: 12px 20px;
        border: none;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
        font-family: 'Poppins', Arial, sans-serif;
      }

      .btn-cancel {
        background-color: #f0f0f0;
        color: #333;
      }

      .btn-cancel:hover {
        background-color: #e0e0e0;
      }

      .btn-submit {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }

      .btn-submit:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
      }

      .btn-submit:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      /* Message de statut */
      .status-message {
        padding: 15px;
        border-radius: 8px;
        text-align: center;
        font-weight: 500;
        margin-top: 15px;
      }

      .status-message.success {
        background-color: #c8e6c9;
        color: #2e7d32;
        border-left: 4px solid #4caf50;
      }

      .status-message.error {
        background-color: #ffcdd2;
        color: #c62828;
        border-left: 4px solid #f44336;
      }

      .status-message.loading {
        background-color: #e3f2fd;
        color: #1565c0;
        border-left: 4px solid #2196f3;
      }

      /* Responsive */
      @media (max-width: 600px) {
        .bug-report-btn {
          bottom: 20px;
          right: 20px;
          width: 50px;
          height: 50px;
          font-size: 24px;
        }

        .bug-report-modal-content {
          width: 95%;
          padding: 20px;
        }

        .bug-report-header h2 {
          font-size: 18px;
        }

        .form-actions {
          flex-direction: column;
        }
      }
    `;
  }

  /**
   * Attache les événements aux éléments du formulaire
   */
  attachEventListeners() {
    const form = document.getElementById('bugReportForm');
    const titleInput = document.getElementById('reportTitle');
    const descInput = document.getElementById('reportDescription');

    if (form) {
      form.addEventListener('submit', (e) => this.submitReport(e));
    }

    if (titleInput) {
      titleInput.addEventListener('input', (e) => {
        const count = e.target.value.length;
        const parent = e.target.parentElement;
        const charCount = parent.querySelector('.char-count');
        if (charCount) charCount.textContent = `${count}/100`;
      });
    }

    if (descInput) {
      descInput.addEventListener('input', (e) => {
        const count = e.target.value.length;
        const parent = e.target.parentElement;
        const charCount = parent.querySelector('.char-count');
        if (charCount) charCount.textContent = `${count}/1000`;
      });
    }
  }

  /**
   * Ouvre le modal
   */
  openModal() {
    const modal = document.getElementById('bugReportModal');
    if (modal) {
      modal.classList.add('show');
      document.body.style.overflow = 'hidden';
    }
  }

  /**
   * Ferme le modal
   */
  closeModal() {
    const modal = document.getElementById('bugReportModal');
    if (modal) {
      modal.classList.remove('show');
      document.body.style.overflow = 'auto';
      this.resetForm();
    }
  }

  /**
   * Réinitialise le formulaire
   */
  resetForm() {
    const form = document.getElementById('bugReportForm');
    if (form) {
      form.reset();
      // Réinitialiser les compteurs
      const charCounts = document.querySelectorAll('.char-count');
      charCounts.forEach(count => {
        count.textContent = '0/' + (count.textContent.split('/')[1] || '100');
      });
    }
  }

  /**
   * Soumet le rapport
   */
  async submitReport(e) {
    e.preventDefault();

    const submitBtn = document.getElementById('submitBugBtn');
    const statusMessage = document.getElementById('statusMessage');

    // Récupérer les données du formulaire
    const formData = new FormData(document.getElementById('bugReportForm'));
    const data = {
      type: formData.get('type'),
      title: formData.get('title'),
      description: formData.get('description'),
      priority: formData.get('priority'),
      browser: formData.get('browser') || 'Non spécifié',
      timestamp: new Date().toISOString(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      userId: this.auth?.currentUser?.uid || 'Anonymous',
      userEmail: this.auth?.currentUser?.email || 'Anonymous',
      status: 'open'
    };

    // Afficher le message de chargement
    this.showStatus('Envoi du rapport en cours...', 'loading', statusMessage);
    submitBtn.disabled = true;

    try {
      // Gérer l'attachement s'il existe
      const attachmentFile = formData.get('attachment');
      if (attachmentFile && attachmentFile.size > 0) {
        // Vérifier la taille
        if (attachmentFile.size > 5 * 1024 * 1024) {
          throw new Error('Fichier trop volumineux (max 5MB)');
        }

        // Uploader le fichier
        const fileName = `${Date.now()}_${attachmentFile.name}`;
        const storageRef = firebase.storage().ref(`bug-reports/${fileName}`);
        await storageRef.put(attachmentFile);
        data.attachmentUrl = await storageRef.getDownloadURL();
      }

      // Sauvegarder dans Firebase
      if (this.db) {
        await this.db.collection('bug_reports').add(data);
      } else {
        // Fallback: sauvegarder en base de données réaltime
        const database = firebase.database();
        const newReportRef = database.ref('bug_reports').push();
        await newReportRef.set(data);
      }

      // Succès
      this.showStatus('✅ Rapport envoyé avec succès! Merci de votre aide.', 'success', statusMessage);
      
      setTimeout(() => {
        this.closeModal();
      }, 2000);

    } catch (error) {
      console.error('Erreur lors de l\'envoi:', error);
      this.showStatus(`❌ Erreur: ${error.message}`, 'error', statusMessage);
    } finally {
      submitBtn.disabled = false;
    }
  }

  /**
   * Affiche un message de statut
   */
  showStatus(message, type, element) {
    if (!element) return;

    element.textContent = message;
    element.className = `status-message ${type}`;
    element.style.display = 'block';
  }

  /**
   * Ferme le modal au clic en dehors
   */
  attachOutsideClickListener() {
    const modal = document.getElementById('bugReportModal');
    if (modal) {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          this.closeModal();
        }
      });
    }
  }
}

// Initialiser le système automatiquement
document.addEventListener('DOMContentLoaded', () => {
  if (typeof firebase !== 'undefined') {
    window.bugReportSystem = new BugReportSystem();
    window.bugReportSystem.attachOutsideClickListener();
  }
});
