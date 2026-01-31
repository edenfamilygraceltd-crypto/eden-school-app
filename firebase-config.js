// Secure Firebase Configuration
// This file should be loaded securely and not exposed to clients

const firebaseConfig = {
  apiKey: process.env.FIREBASE_API_KEY || "AIzaSyApUFNELOfgIe7rWEek9GLS9EIphNW09-A",
  authDomain: process.env.FIREBASE_AUTH_DOMAIN || "edensmart-app.firebaseapp.com",
  projectId: process.env.FIREBASE_PROJECT_ID || "edensmart-app",
  storageBucket: process.env.FIREBASE_STORAGE_BUCKET || "edensmart-app.firebasestorage.app",
  messagingSenderId: process.env.FIREBASE_MESSAGING_SENDER_ID || "1093120876724",
  appId: process.env.FIREBASE_APP_ID || "1:1093120876724:web:bc37448cadd18d651c77e1",
  measurementId: process.env.FIREBASE_MEASUREMENT_ID || "G-1FL70PZZSW"
};

// Export for Node.js environments
if (typeof module !== 'undefined' && module.exports) {
  module.exports = firebaseConfig;
}

// Export for browser environments
if (typeof window !== 'undefined') {
  window.FIREBASE_CONFIG = firebaseConfig;
}
