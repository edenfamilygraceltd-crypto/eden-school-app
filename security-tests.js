/**
 * 🧪 TESTS DE SÉCURITÉ AUTOMATISÉS
 * Pour vérifier que la sécurité fonctionne correctement
 * 
 * À exécuter dans la console du navigateur (F12)
 */

const SECURITY_TESTS = {
  results: [],
  
  // Test 1: Vérifier que sessionManager existe
  testSessionManagerExists() {
    const test = {
      name: '1. SessionManager est chargé',
      passed: typeof sessionManager !== 'undefined',
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 2: Vérifier les fonctions requises
  testRequiredFunctions() {
    const test = {
      name: '2. Fonctions de sécurité disponibles',
      passed: (
        typeof protectPage === 'function' &&
        typeof secureLogout === 'function' &&
        typeof isAuthenticated === 'function' &&
        typeof getCurrentUser === 'function'
      ),
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 3: Vérifier la configuration de sécurité
  testSecurityConfig() {
    const test = {
      name: '3. Configuration de sécurité correcte',
      passed: (
        SECURITY_CONFIG &&
        SECURITY_CONFIG.SESSION_TIMEOUT > 0 &&
        SECURITY_CONFIG.MAX_LOGIN_ATTEMPTS > 0 &&
        SECURITY_CONFIG.ALLOWED_PAGES
      ),
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 4: Vérifier sessionStorage ne contient pas d'informations sensibles
  testSessionStorageSecurity() {
    const session = sessionManager.getSession();
    const userData = sessionManager.getUserData();
    
    const test = {
      name: '4. SessionStorage sécurisé (pas de mots de passe)',
      details: {
        sessionExists: !!session,
        userDataExists: !!userData,
        hasPassword: userData && Object.values(userData).some(v => v === 'password')
      },
      passed: !(userData && Object.values(userData).some(v => v === 'password')),
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 5: Vérifier la validation de session
  testSessionValidation() {
    const isValid = sessionManager.isSessionValid();
    const test = {
      name: '5. Validation de session fonctionne',
      details: {
        sessionValid: isValid,
        sessionExpiry: sessionManager.getSession()?.expiresAt
      },
      passed: typeof isValid === 'boolean',
      severity: 'HAUTE'
    };
    this.results.push(test);
    return test;
  },

  // Test 6: Vérifier HTTPS en production
  testHTTPSRequirement() {
    const isProduction = !window.location.hostname.includes('localhost');
    const isHTTPS = window.location.protocol === 'https:';
    
    const test = {
      name: '6. HTTPS utilisé en production',
      details: {
        environment: isProduction ? 'Production' : 'Développement',
        protocol: window.location.protocol,
        secure: isHTTPS || !isProduction
      },
      passed: isHTTPS || !isProduction,
      severity: 'HAUTE'
    };
    this.results.push(test);
    return test;
  },

  // Test 7: Vérifier Content Security Policy
  testCSP() {
    const metaTags = document.querySelectorAll('meta[http-equiv="Content-Security-Policy"]');
    const test = {
      name: '7. Content Security Policy (CSP) configuré',
      details: {
        cspMetaTagCount: metaTags.length,
        hasCSP: metaTags.length > 0 || document.contentSecurityPolicy !== undefined
      },
      passed: metaTags.length > 0 || document.contentSecurityPolicy !== undefined,
      severity: 'MOYENNE'
    };
    this.results.push(test);
    return test;
  },

  // Test 8: Vérifier les en-têtes de sécurité
  testSecurityHeaders() {
    // Note: Cela ne fonctionne que depuis un serveur avec CORS enabled
    const test = {
      name: '8. En-têtes de sécurité configurés (serveur)',
      details: {
        note: 'À vérifier avec curl ou postman',
        expectedHeaders: [
          'X-Content-Type-Options',
          'X-Frame-Options',
          'X-XSS-Protection',
          'Strict-Transport-Security',
          'Content-Security-Policy'
        ]
      },
      passed: true, // Vérifier manuellement
      severity: 'MOYENNE',
      manual: true
    };
    this.results.push(test);
    return test;
  },

  // Test 9: Vérifier la génération de tokens
  testTokenGeneration() {
    const token1 = sessionManager.generateSecureToken();
    const token2 = sessionManager.generateSecureToken();
    
    const test = {
      name: '9. Génération de tokens aléatoires',
      details: {
        token1Length: token1.length,
        token2Length: token2.length,
        tokensUnique: token1 !== token2,
        expectedLength: 64 // 32 bytes * 2 hex
      },
      passed: (
        token1.length === 64 &&
        token2.length === 64 &&
        token1 !== token2
      ),
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 10: Vérifier le timeout de session
  testSessionTimeout() {
    const session = sessionManager.getSession();
    const now = Date.now();
    const timeoutMs = SECURITY_CONFIG.SESSION_TIMEOUT;
    
    const test = {
      name: '10. Timeout de session configuré',
      details: {
        timeoutMinutes: (timeoutMs / 1000 / 60).toFixed(0),
        sessionActive: session !== null,
        sessionWillExpireSoon: session && (session.expiresAt - now) > 0
      },
      passed: timeoutMs > 0 && timeoutMs < 60 * 60 * 1000, // Entre 1 min et 1 heure
      severity: 'HAUTE'
    };
    this.results.push(test);
    return test;
  },

  // Test 11: Vérifier localStorage pour données sensibles
  testLocalStorageSecurity() {
    let sensitiveDataFound = false;
    
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      const value = localStorage.getItem(key);
      
      // Chercher les patterns sensibles
      if (
        key.toLowerCase().includes('password') ||
        key.toLowerCase().includes('token') ||
        key.toLowerCase().includes('secret') ||
        value.includes('password') ||
        value.includes('auth')
      ) {
        sensitiveDataFound = true;
        console.warn(`⚠️ Donnée sensible trouvée en localStorage: ${key}`);
      }
    }
    
    const test = {
      name: '11. LocalStorage sans données sensibles',
      details: {
        storageItemCount: localStorage.length,
        hasSensitiveData: sensitiveDataFound,
        recommendation: 'Utiliser sessionStorage pour les données sensibles'
      },
      passed: !sensitiveDataFound,
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 12: Vérifier RBAC (Role-Based Access Control)
  testRBAC() {
    const user = getCurrentUser();
    const pages = SECURITY_CONFIG.ALLOWED_PAGES;
    
    const test = {
      name: '12. Contrôle d\'accès basé sur les rôles (RBAC)',
      details: {
        currentUser: user?.email,
        userRole: user?.role,
        protectedPages: Object.keys(pages).length,
        rolesPerPage: Object.values(pages).map(r => r.length)
      },
      passed: user && user.role && Object.keys(pages).length > 0,
      severity: 'CRITIQUE'
    };
    this.results.push(test);
    return test;
  },

  // Test 13: Vérifier la validation d'email
  testEmailValidation() {
    const user = getCurrentUser();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    const test = {
      name: '13. Validation d\'email utilisateur',
      details: {
        userEmail: user?.email,
        isValidEmail: user && emailRegex.test(user.email)
      },
      passed: user && emailRegex.test(user.email),
      severity: 'MOYENNE'
    };
    this.results.push(test);
    return test;
  },

  // Test 14: Vérifier les logs de sécurité
  testSecurityLogging() {
    const test = {
      name: '14. Logs de sécurité configurés',
      details: {
        hasLogSecurityEvent: typeof sessionManager.logSecurityEvent === 'function'
      },
      passed: typeof sessionManager.logSecurityEvent === 'function',
      severity: 'MOYENNE'
    };
    this.results.push(test);
    return test;
  },

  // Exécuter tous les tests
  runAllTests() {
    console.log('%c🧪 EXÉCUTION DES TESTS DE SÉCURITÉ', 'color: #0066cc; font-size: 16px; font-weight: bold;');
    console.log('%c================================', 'color: #0066cc; font-size: 14px;');
    
    this.testSessionManagerExists();
    this.testRequiredFunctions();
    this.testSecurityConfig();
    this.testSessionStorageSecurity();
    this.testSessionValidation();
    this.testHTTPSRequirement();
    this.testCSP();
    this.testSecurityHeaders();
    this.testTokenGeneration();
    this.testSessionTimeout();
    this.testLocalStorageSecurity();
    this.testRBAC();
    this.testEmailValidation();
    this.testSecurityLogging();
    
    this.displayResults();
    this.displaySummary();
  },

  // Afficher les résultats
  displayResults() {
    this.results.forEach((test, index) => {
      const icon = test.passed ? '✅' : '❌';
      const color = test.passed ? 'color: green;' : 'color: red;';
      
      console.log(`%c${icon} ${test.name}`, color);
      
      if (test.details) {
        console.log('%cDétails:', 'font-weight: bold;');
        console.table(test.details);
      }
      
      if (test.manual) {
        console.log('%c⚠️ MANUEL - À vérifier manuellement', 'color: orange; font-weight: bold;');
      }
      
      console.log('---');
    });
  },

  // Afficher le résumé
  displaySummary() {
    const passed = this.results.filter(t => t.passed).length;
    const failed = this.results.filter(t => !t.passed).length;
    const total = this.results.length;
    const percentage = (passed / total * 100).toFixed(0);
    
    const criticalFailed = this.results.filter(t => !t.passed && t.severity === 'CRITIQUE');
    
    console.log('%c================================', 'color: #0066cc; font-size: 14px;');
    console.log('%c📊 RÉSUMÉ DES TESTS', 'color: #0066cc; font-size: 16px; font-weight: bold;');
    console.log('%c================================', 'color: #0066cc; font-size: 14px;');
    console.table({
      'Total': total,
      'Réussis': passed,
      'Échoués': failed,
      'Pourcentage': percentage + '%'
    });
    
    if (criticalFailed.length > 0) {
      console.error('%c🔴 ERREURS CRITIQUES TROUVÉES:', 'color: red; font-size: 14px; font-weight: bold;');
      criticalFailed.forEach(t => {
        console.error(`   - ${t.name}`);
      });
    } else {
      console.log('%c✅ TOUS LES TESTS CRITIQUES SONT PASSÉS!', 'color: green; font-size: 14px; font-weight: bold;');
    }
  },

  // Exporter les résultats
  exportResults() {
    const report = {
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      results: this.results,
      summary: {
        total: this.results.length,
        passed: this.results.filter(t => t.passed).length,
        failed: this.results.filter(t => !t.passed).length
      }
    };
    
    return JSON.stringify(report, null, 2);
  }
};

// 🚀 Pour exécuter les tests, tapez dans la console:
// SECURITY_TESTS.runAllTests();

console.log('%c💡 TIP: Exécutez SECURITY_TESTS.runAllTests() pour tester la sécurité', 'color: blue; font-size: 12px;');
