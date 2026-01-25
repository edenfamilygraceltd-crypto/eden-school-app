/**
 * SYSTÈME DE SÉCURITÉ D'AUTHENTIFICATION - Eden Family School
 * Protège l'accès aux pages sensibles
 */

// Configuration de sécurité
const SECURITY_CONFIG = {
  SESSION_TIMEOUT: 30 * 60 * 1000, // 30 minutes
  MAX_LOGIN_ATTEMPTS: 5,
  LOCKOUT_DURATION: 15 * 60 * 1000, // 15 minutes
  REQUIRE_HTTPS: true,
  ALLOWED_PAGES: {
    'director.html': ['director', 'admin'],
    'comptable.html': ['accountant', 'admin'],
    'secretary.html': ['secretary', 'admin'],
    'teacher_clean.html': ['teacher', 'admin', 'parent']
  }
};

// Classe de gestion de session sécurisée
class SecureSessionManager {
  constructor() {
    this.sessionKey = 'eden_secure_session';
    this.userKey = 'eden_user_data';
    this.tokenKey = 'eden_auth_token';
    this.initSession();
  }

  /**
   * Initialiser et valider la session
   */
  initSession() {
    // Vérifier HTTPS en production
    if (SECURITY_CONFIG.REQUIRE_HTTPS && window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
      console.warn('⚠️ AVERTISSEMENT SÉCURITÉ: HTTPS non détecté. Utilisez HTTPS en production!');
    }

    // Pour index.html, ne pas forcer redirection si non connecté
    // Les pages publiques peuvent être visitées sans session
    if (window.location.pathname.includes('index.html') || window.location.pathname.endsWith('/')) {
      // Page d'accueil publique - juste valider si session existe
      if (this.isSessionValid()) {
        this.resetSessionTimeout();
      }
      return; // Ne pas rediriger sur index.html
    }

    // Pour les autres pages, valider la session
    if (!this.isSessionValid()) {
      this.clearSession();
      this.redirectToAuth();
    } else {
      this.resetSessionTimeout();
    }

    // Écouter l'inactivité
    this.setupInactivityListener();
  }

  /**
   * Vérifier si la session est valide
   */
  isSessionValid() {
    const session = this.getSession();
    if (!session) return false;

    // Vérifier l'expiration
    if (Date.now() > session.expiresAt) {
      return false;
    }

    // Vérifier que les données utilisateur existent
    const userData = this.getUserData();
    return userData && userData.uid && userData.role;
  }

  /**
   * Obtenir les données de session
   */
  getSession() {
    try {
      const session = sessionStorage.getItem(this.sessionKey);
      return session ? JSON.parse(session) : null;
    } catch (e) {
      console.error('Erreur de décodage session:', e);
      return null;
    }
  }

  /**
   * Obtenir les données utilisateur
   */
  getUserData() {
    try {
      const userData = sessionStorage.getItem(this.userKey);
      return userData ? JSON.parse(userData) : null;
    } catch (e) {
      console.error('Erreur de décodage utilisateur:', e);
      return null;
    }
  }

  /**
   * Créer une nouvelle session sécurisée
   */
  createSession(user) {
    // Validation des données utilisateur
    if (!user || !user.uid || !user.role || !user.email) {
      throw new Error('Données utilisateur invalides');
    }

    // Créer un token sécurisé
    const token = this.generateSecureToken();

    // Créer la session
    const session = {
      createdAt: Date.now(),
      expiresAt: Date.now() + SECURITY_CONFIG.SESSION_TIMEOUT,
      token: token,
      userAgent: this.getSecureUserAgent()
    };

    // Sauvegarder la session (sessionStorage = supprimée à la fermeture du navigateur)
    sessionStorage.setItem(this.sessionKey, JSON.stringify(session));
    
    // Sauvegarder les données utilisateur
    const cleanUserData = {
      uid: user.uid,
      email: user.email,
      role: user.role,
      name: user.name || '',
      loginTime: Date.now()
    };
    sessionStorage.setItem(this.userKey, JSON.stringify(cleanUserData));
    sessionStorage.setItem(this.tokenKey, token);

    // Enregistrer dans les logs
    this.logSecurityEvent('SESSION_CREATED', cleanUserData);
  }

  /**
   * Vérifier l'accès à une page spécifique
   */
  checkPageAccess(pageName) {
    const userData = this.getUserData();
    
    if (!userData) {
      return {
        allowed: false,
        reason: 'Pas de session active'
      };
    }

    const allowedRoles = SECURITY_CONFIG.ALLOWED_PAGES[pageName];
    if (!allowedRoles) {
      return {
        allowed: true, // Page non protégée
        reason: 'Page non protégée'
      };
    }

    const hasAccess = allowedRoles.includes(userData.role);
    return {
      allowed: hasAccess,
      reason: hasAccess ? 'Accès autorisé' : 'Rôle insuffisant',
      requiredRoles: allowedRoles,
      userRole: userData.role
    };
  }

  /**
   * Réinitialiser le timeout de session
   */
  resetSessionTimeout() {
    const session = this.getSession();
    if (session) {
      session.expiresAt = Date.now() + SECURITY_CONFIG.SESSION_TIMEOUT;
      sessionStorage.setItem(this.sessionKey, JSON.stringify(session));
    }
  }

  /**
   * Configurer l'écoute de l'inactivité
   */
  setupInactivityListener() {
    let inactivityTimer;

    const resetTimer = () => {
      clearTimeout(inactivityTimer);
      this.resetSessionTimeout();

      inactivityTimer = setTimeout(() => {
        if (this.isSessionValid()) {
          this.logSecurityEvent('SESSION_EXPIRED_INACTIVITY');
          this.clearSession();
          alert('Votre session a expiré due à l\'inactivité. Reconnexion requise.');
          this.redirectToAuth();
        }
      }, SECURITY_CONFIG.SESSION_TIMEOUT);
    };

    // Événements d'activité utilisateur
    ['mousedown', 'keydown', 'scroll', 'touchstart', 'click'].forEach(event => {
      document.addEventListener(event, resetTimer, true);
    });

    resetTimer();
  }

  /**
   * Effacer la session
   */
  clearSession() {
    sessionStorage.removeItem(this.sessionKey);
    sessionStorage.removeItem(this.userKey);
    sessionStorage.removeItem(this.tokenKey);
    this.logSecurityEvent('SESSION_CLEARED');
  }

  /**
   * Générer un token sécurisé
   */
  generateSecureToken() {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Obtenir user agent hashé pour sécurité
   */
  getSecureUserAgent() {
    // Utiliser un hash simple du user agent pour validation
    return btoa(navigator.userAgent).substring(0, 50);
  }

  /**
   * Rediriger vers Auth.html
   */
  redirectToAuth() {
    // Éviter boucle de redirection si déjà sur Auth.html
    if (window.location.pathname.includes('Auth.html')) {
      return; // Déjà sur page de login
    }
    // Rediriger simplement vers Auth.html SANS paramètre redirect
    // La redirection sera basée sur le rôle de l'utilisateur, pas sur l'URL
    window.location.href = './Auth.html';
  }

  /**
   * Enregistrer les événements de sécurité
   */
  logSecurityEvent(eventType, data = {}) {
    const logEntry = {
      timestamp: new Date().toISOString(),
      eventType: eventType,
      userAgent: navigator.userAgent,
      url: window.location.href,
      data: data
    };
    
    // Enregistrer dans console en développement
    console.log(`🔒 SECURITY: ${eventType}`, logEntry);
    
    // En production, envoyer à un serveur de logs
    // await fetch('/api/security-logs', { method: 'POST', body: JSON.stringify(logEntry) });
  }

  /**
   * Vérifier les tentatives de connexion échouées
   */
  recordFailedAttempt(email) {
    const key = `failed_attempts_${email}`;
    const attempts = parseInt(localStorage.getItem(key) || '0') + 1;
    localStorage.setItem(key, attempts);

    if (attempts >= SECURITY_CONFIG.MAX_LOGIN_ATTEMPTS) {
      const lockoutKey = `lockout_${email}`;
      localStorage.setItem(lockoutKey, Date.now().toString());
      return { locked: true, attempts: attempts };
    }

    return { locked: false, attempts: attempts };
  }

  /**
   * Vérifier si un compte est verrouillé
   */
  isAccountLocked(email) {
    const lockoutKey = `lockout_${email}`;
    const lockoutTime = localStorage.getItem(lockoutKey);

    if (!lockoutTime) return false;

    const now = Date.now();
    const lockedUntil = parseInt(lockoutTime) + SECURITY_CONFIG.LOCKOUT_DURATION;

    if (now > lockedUntil) {
      localStorage.removeItem(lockoutKey);
      localStorage.removeItem(`failed_attempts_${email}`);
      return false;
    }

    return true;
  }

  /**
   * Réinitialiser les tentatives après connexion réussie
   */
  clearFailedAttempts(email) {
    localStorage.removeItem(`failed_attempts_${email}`);
    localStorage.removeItem(`lockout_${email}`);
  }
}

// Instance globale
const sessionManager = new SecureSessionManager();

/**
 * PROTECTION DES PAGES
 * À ajouter en haut de chaque page protégée
 */
function protectPage(requiredRole = null) {
  const pageName = window.location.pathname.split('/').pop() || 'index.html';
  
  const access = sessionManager.checkPageAccess(pageName);

  if (!access.allowed) {
    console.error('❌ ACCÈS REFUSÉ:', access);
    alert(`Accès refusé. ${access.reason}`);
    sessionManager.redirectToAuth();
    return false;
  }

  if (requiredRole && !sessionManager.getUserData().role.includes(requiredRole)) {
    console.error('❌ RÔLE INSUFFISANT:', requiredRole);
    alert('Vous n\'avez pas les permissions requises pour accéder à cette ressource.');
    sessionManager.redirectToAuth();
    return false;
  }

  return true;
}

/**
 * DÉCONNEXION SÉCURISÉE
 */
function secureLogout() {
  sessionManager.logSecurityEvent('USER_LOGOUT');
  sessionManager.clearSession();
  window.location.href = 'Auth.html';
}

/**
 * OBTENIR UTILISATEUR ACTUEL
 */
function getCurrentUser() {
  return sessionManager.getUserData();
}

/**
 * VÉRIFIER SI AUTHENTIFIÉ
 */
function isAuthenticated() {
  return sessionManager.isSessionValid();
}

// Export pour utilisation
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { SecureSessionManager, sessionManager, protectPage, secureLogout };
}
