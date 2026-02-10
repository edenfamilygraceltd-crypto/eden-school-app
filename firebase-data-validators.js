/**
 * 📋 Schémas de validation des données Firebase Realtime Database
 * Utilisez ces validateurs pour s'assurer que vos données respectent la structure
 */

// ============ VALIDATEURS UTILITAIRES ============

const validators = {
  // Valider un email
  isValidEmail: (email) => {
    const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return typeof email === 'string' && regex.test(email);
  },

  // Valider un rôle
  isValidRole: (role) => {
    const validRoles = ['director', 'IT Admin', 'secretary', 'accountant', 'teacher', 'parent'];
    return validRoles.includes(role);
  },

  // Valider un statut utilisateur
  isValidUserStatus: (status) => {
    const validStatuses = ['active', 'inactive', 'suspended'];
    return validStatuses.includes(status);
  },

  // Valider un statut de frais
  isValidFeeStatus: (status) => {
    const validStatuses = ['paid', 'pending', 'overdue'];
    return validStatuses.includes(status);
  },

  // Valider une branche
  isValidBranch: (branch) => {
    const validBranches = ['kacyiru', 'kimisagara', 'gisozi_maternelle', 'gisozi_primaire'];
    return validBranches.includes(branch);
  },

  // Valider un numéro de mois
  isValidMonth: (month) => {
    return typeof month === 'number' && month >= 1 && month <= 12;
  },

  // Valider une année
  isValidYear: (year) => {
    return typeof year === 'number' && year >= 2020 && year <= 2100;
  },

  // Valider un numéro de semaine
  isValidWeek: (week) => {
    return typeof week === 'number' && week >= 1 && week <= 53;
  },

  // Valider une priorité
  isValidPriority: (priority) => {
    const validPriorities = ['Basse', 'Moyenne', 'Haute', 'Critique'];
    return validPriorities.includes(priority);
  },

  // Valider un statut de rapport
  isValidReportStatus: (status) => {
    const validStatuses = ['Nouveau', 'En cours', 'Résolu', 'Fermé'];
    return validStatuses.includes(status);
  },

  // Valider un type de rapport
  isValidReportType: (type) => {
    const validTypes = ['Bug', 'Suggestion', 'Amélioration', 'Autre'];
    return validTypes.includes(type);
  },

  // Valider un statut de paie
  isValidPayrollStatus: (status) => {
    const validStatuses = ['pending', 'processed', 'paid'];
    return validStatuses.includes(status);
  },

  // Valider un montant (positif)
  isValidAmount: (amount) => {
    return typeof amount === 'number' && amount > 0;
  },

  // Valider une chaîne non-vide
  isNonEmptyString: (str) => {
    return typeof str === 'string' && str.trim().length > 0;
  },

  // Valider une date ISO 8601
  isValidISODate: (date) => {
    return typeof date === 'string' && !isNaN(Date.parse(date));
  },

  // Valider un timestamp
  isValidTimestamp: (ts) => {
    return typeof ts === 'string' || typeof ts === 'number';
  }
};

// ============ SCHÉMAS DE VALIDATION ============

const schemas = {
  // Schéma utilisateur
  user: {
    required: ['nom', 'email', 'role'],
    validate: (data) => {
      const errors = [];

      // Vérifier les champs requis
      if (!data.nom || !validators.isNonEmptyString(data.nom)) {
        errors.push('nom: Requis et non-vide');
      }
      if (!data.email || !validators.isValidEmail(data.email)) {
        errors.push('email: Format email invalide');
      }
      if (!data.role || !validators.isValidRole(data.role)) {
        errors.push('role: Rôle invalide');
      }

      // Vérifier les champs optionnels
      if (data.telephone && !validators.isNonEmptyString(data.telephone)) {
        errors.push('telephone: Format invalide');
      }
      if (data.status && !validators.isValidUserStatus(data.status)) {
        errors.push('status: Statut invalide');
      }
      if (data.createdAt && !validators.isValidISODate(data.createdAt)) {
        errors.push('createdAt: Format date invalide');
      }
      if (data.lastLogin && data.lastLogin !== null && !validators.isValidISODate(data.lastLogin)) {
        errors.push('lastLogin: Format date invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma activité
  activity: {
    required: ['utilisateur', 'action', 'timestamp'],
    validate: (data) => {
      const errors = [];

      if (!data.utilisateur || !validators.isNonEmptyString(data.utilisateur)) {
        errors.push('utilisateur: Requis et non-vide');
      }
      if (!data.action || !validators.isNonEmptyString(data.action)) {
        errors.push('action: Requis et non-vide');
      }
      if (!data.timestamp || !validators.isValidTimestamp(data.timestamp)) {
        errors.push('timestamp: Requis et valide');
      }

      if (data.details && !validators.isNonEmptyString(data.details)) {
        errors.push('details: Format invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma rapport (bug/suggestion)
  report: {
    required: ['type', 'titre', 'description', 'priorite', 'statut', 'rapportePar', 'rapporteParId', 'date'],
    validate: (data) => {
      const errors = [];

      if (!data.type || !validators.isValidReportType(data.type)) {
        errors.push('type: Type de rapport invalide');
      }
      if (!data.titre || !validators.isNonEmptyString(data.titre) || data.titre.length > 200) {
        errors.push('titre: Doit faire 1-200 caractères');
      }
      if (!data.description || !validators.isNonEmptyString(data.description)) {
        errors.push('description: Requis et non-vide');
      }
      if (!data.priorite || !validators.isValidPriority(data.priorite)) {
        errors.push('priorite: Priorité invalide');
      }
      if (!data.statut || !validators.isValidReportStatus(data.statut)) {
        errors.push('statut: Statut invalide');
      }
      if (!data.rapportePar || !validators.isNonEmptyString(data.rapportePar)) {
        errors.push('rapportePar: Requis et non-vide');
      }
      if (!data.rapporteParId || !validators.isNonEmptyString(data.rapporteParId)) {
        errors.push('rapporteParId: Requis et non-vide');
      }
      if (!data.date || !validators.isValidISODate(data.date)) {
        errors.push('date: Format date invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma étudiant
  student: {
    required: ['name', 'email'],
    validate: (data) => {
      const errors = [];

      if (!data.name || !validators.isNonEmptyString(data.name)) {
        errors.push('name: Requis et non-vide');
      }
      if (!data.email || !validators.isValidEmail(data.email)) {
        errors.push('email: Format email invalide');
      }
      if (data.branch && !validators.isValidBranch(data.branch)) {
        errors.push('branch: Branche invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma frais scolaires
  studentFee: {
    required: ['studentId', 'amount', 'date'],
    validate: (data) => {
      const errors = [];

      if (!data.studentId || !validators.isNonEmptyString(data.studentId)) {
        errors.push('studentId: Requis et non-vide');
      }
      if (!data.amount || !validators.isValidAmount(data.amount)) {
        errors.push('amount: Doit être > 0');
      }
      if (!data.date || !validators.isValidISODate(data.date)) {
        errors.push('date: Format date invalide');
      }
      if (data.status && !validators.isValidFeeStatus(data.status)) {
        errors.push('status: Statut invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma paie
  payroll: {
    required: ['workerId', 'month', 'year'],
    validate: (data) => {
      const errors = [];

      if (!data.workerId || !validators.isNonEmptyString(data.workerId)) {
        errors.push('workerId: Requis et non-vide');
      }
      if (!validators.isValidMonth(data.month)) {
        errors.push('month: Doit être 1-12');
      }
      if (!validators.isValidYear(data.year)) {
        errors.push('year: Doit être 2020-2100');
      }
      if (data.basicSalary && (typeof data.basicSalary !== 'number' || data.basicSalary < 0)) {
        errors.push('basicSalary: Doit être >= 0');
      }
      if (data.totalSalary && !validators.isValidAmount(data.totalSalary)) {
        errors.push('totalSalary: Doit être >= 0');
      }
      if (data.status && !validators.isValidPayrollStatus(data.status)) {
        errors.push('status: Statut invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma dette
  debt: {
    required: ['workerId', 'amount', 'date'],
    validate: (data) => {
      const errors = [];

      if (!data.workerId || !validators.isNonEmptyString(data.workerId)) {
        errors.push('workerId: Requis et non-vide');
      }
      if (!data.amount || !validators.isValidAmount(data.amount)) {
        errors.push('amount: Doit être > 0');
      }
      if (!data.date || !validators.isValidISODate(data.date)) {
        errors.push('date: Format date invalide');
      }
      if (data.status && !['pending', 'paid', 'partial'].includes(data.status)) {
        errors.push('status: Statut invalide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma annonce
  announcement: {
    required: ['message', 'timestamp'],
    validate: (data) => {
      const errors = [];

      if (!data.message || !validators.isNonEmptyString(data.message)) {
        errors.push('message: Requis et non-vide');
      }
      if (!data.timestamp || !validators.isValidTimestamp(data.timestamp)) {
        errors.push('timestamp: Requis et valide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma message
  message: {
    required: ['senderId', 'message', 'timestamp'],
    validate: (data) => {
      const errors = [];

      if (!data.senderId || !validators.isNonEmptyString(data.senderId)) {
        errors.push('senderId: Requis et non-vide');
      }
      if (!data.message || !validators.isNonEmptyString(data.message)) {
        errors.push('message: Requis et non-vide');
      }
      if (!data.timestamp || !validators.isValidTimestamp(data.timestamp)) {
        errors.push('timestamp: Requis et valide');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma rapport financier
  financialReport: {
    required: ['date', 'month'],
    validate: (data) => {
      const errors = [];

      if (!data.date || !validators.isValidISODate(data.date)) {
        errors.push('date: Format date invalide');
      }
      if (!data.month) {
        errors.push('month: Requis');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma rapport mensuel
  monthlyReport: {
    required: ['month', 'year'],
    validate: (data) => {
      const errors = [];

      if (!validators.isValidMonth(data.month)) {
        errors.push('month: Doit être 1-12');
      }
      if (!validators.isValidYear(data.year)) {
        errors.push('year: Doit être 2020-2100');
      }

      return { valid: errors.length === 0, errors };
    }
  },

  // Schéma rapport hebdomadaire
  weeklyReport: {
    required: ['week', 'year'],
    validate: (data) => {
      const errors = [];

      if (!validators.isValidWeek(data.week)) {
        errors.push('week: Doit être 1-53');
      }
      if (!validators.isValidYear(data.year)) {
        errors.push('year: Doit être 2020-2100');
      }

      return { valid: errors.length === 0, errors };
    }
  }
};

// ============ FONCTION DE VALIDATION GÉNÉRALE ============

function validateData(schemaName, data) {
  const schema = schemas[schemaName];

  if (!schema) {
    return {
      valid: false,
      errors: [`Schéma "${schemaName}" non trouvé`]
    };
  }

  return schema.validate(data);
}

// ============ EXEMPLE D'UTILISATION ============

/*
// Valider un utilisateur
const newUser = {
  nom: 'Jean Dupont',
  email: 'jean@example.com',
  role: 'teacher',
  telephone: '+250 78 123 4567',
  status: 'active'
};

const result = validateData('user', newUser);
console.log(result);
// Output: { valid: true, errors: [] }

// Valider un utilisateur avec email invalide
const invalidUser = {
  nom: 'Marie',
  email: 'invalid-email',
  role: 'teacher'
};

const result2 = validateData('user', invalidUser);
console.log(result2);
// Output: { valid: false, errors: ['email: Format email invalide'] }
*/

// ============ INTÉGRATION AVEC VOS FONCTIONS ============

async function createUserWithValidation(userData) {
  try {
    // Valider les données
    const validation = validateData('user', userData);
    
    if (!validation.valid) {
      console.error('❌ Erreurs de validation:');
      validation.errors.forEach(err => console.error('  -', err));
      throw new Error('Validation échouée: ' + validation.errors.join(', '));
    }

    console.log('✅ Données valides');

    // Créer l'utilisateur
    const userId = userData.uid;
    await window.rtdbSet(
      window.rtdbRef(window.rtdb, `users/${userId}`),
      userData
    );

    console.log('✅ Utilisateur créé');
    return true;
  } catch (error) {
    console.error('❌ Erreur:', error);
    throw error;
  }
}

// Export des validateurs et schémas
window.validators = validators;
window.schemas = schemas;
window.validateData = validateData;
window.createUserWithValidation = createUserWithValidation;
