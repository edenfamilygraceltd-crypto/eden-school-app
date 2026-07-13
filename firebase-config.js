// Secure Firebase Configuration
// This file should be loaded securely and not exposed to clients

const env = (typeof process !== 'undefined' && process.env) ? process.env : {};
const _firebaseConfig = {
  apiKey: env.FIREBASE_API_KEY || "AIzaSyApUFNELOfgIe7rWEek9GLS9EIphNW09-A",
  authDomain: env.FIREBASE_AUTH_DOMAIN || "edensmart-app.firebaseapp.com",
  databaseURL: env.FIREBASE_DATABASE_URL || "https://edensmart-app-default-rtdb.firebaseio.com",
  projectId: env.FIREBASE_PROJECT_ID || "edensmart-app",
  storageBucket: env.FIREBASE_STORAGE_BUCKET || "edensmart-app.firebasestorage.app",
  messagingSenderId: env.FIREBASE_MESSAGING_SENDER_ID || "1093120876724",
  appId: env.FIREBASE_APP_ID || "1:1093120876724:web:bc37448cadd18d651c77e1",
  measurementId: env.FIREBASE_MEASUREMENT_ID || "G-1FL70PZZSW"
};

// Export for Node.js environments
if (typeof module !== 'undefined' && module.exports) {
  module.exports = _firebaseConfig;
}

// Export for browser environments (do not create a global `firebaseConfig` identifier)
if (typeof window !== 'undefined') {
  window.FIREBASE_CONFIG = _firebaseConfig;
}
