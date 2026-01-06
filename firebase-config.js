// Secure Firebase Configuration
// This file should be loaded securely and not exposed to clients

const firebaseConfig = {
  apiKey: process.env.FIREBASE_API_KEY || "AIzaSyCx6kmJ59x0tLt4vh_3czvEEQrtw4aWFHs",
  authDomain: process.env.FIREBASE_AUTH_DOMAIN || "edendatabase-7e1ed.firebaseapp.com",
  databaseURL: process.env.FIREBASE_DATABASE_URL || "https://edendatabase-7e1ed-default-rtdb.firebaseio.com",
  projectId: process.env.FIREBASE_PROJECT_ID || "edendatabase-7e1ed",
  storageBucket: process.env.FIREBASE_STORAGE_BUCKET || "edendatabase-7e1ed.firebasestorage.app",
  messagingSenderId: process.env.FIREBASE_MESSAGING_SENDER_ID || "147248399046",
  appId: process.env.FIREBASE_APP_ID || "1:147248399046:web:d0b433e755772bbe718dc7",
  measurementId: process.env.FIREBASE_MEASUREMENT_ID || "G-XB192PCMV7"
};

// Export for Node.js environments
if (typeof module !== 'undefined' && module.exports) {
  module.exports = firebaseConfig;
}

// Export for browser environments
if (typeof window !== 'undefined') {
  window.FIREBASE_CONFIG = firebaseConfig;
}
