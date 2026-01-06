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
        projectId: "edendatabase-7e1ed",
        storageBucket: "edendatabase-7e1ed.firebasestorage.app",
        messagingSenderId: "147248399046",
        appId: "1:147248399046:web:d0b433e755772bbe718dc7"
    };
    firebase.initializeApp(firebaseConfig);
}
const auth = firebase.auth();
const db = firebase.firestore();

// Initialize Materialize Components
document.addEventListener('DOMContentLoaded', function() {
    // Sidenav
    var elems = document.querySelectorAll('.sidenav');
    M.Sidenav.init(elems);

    // Parallax
    var parallaxElems = document.querySelectorAll('.parallax');
    M.Parallax.init(parallaxElems);

    // Tabs
    var tabElems = document.querySelectorAll('.tabs');
    M.Tabs.init(tabElems);

    // Smooth Scroll
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Check if user is already logged in
    checkAuthState();
});

// Modal Functions
function openLoginModal() {
    document.getElementById('authModal').style.display = 'block';
    document.body.style.overflow = 'hidden';
}

function closeLoginModal() {
    document.getElementById('authModal').style.display = 'none';
    document.body.style.overflow = 'auto';
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('authModal');
    if (event.target == modal) {
        closeLoginModal();
    }
}

// Switch between Login and Signup
function switchToSignup() {
    document.getElementById('loginForm').classList.add('hidden');
    document.getElementById('signupForm').classList.remove('hidden');
}

function switchToLogin() {
    document.getElementById('signupForm').classList.add('hidden');
    document.getElementById('loginForm').classList.remove('hidden');
}

function showForgotPassword() {
    const email = document.getElementById('login-email').value;
    
    if (!email) {
        M.toast({html: '❌ Veuillez entrer votre email', classes: 'red'});
        return;
    }

    auth.sendPasswordResetEmail(email)
        .then(() => {
            M.toast({html: '✅ Email de réinitialisation envoyé!', classes: 'green'});
        })
        .catch((error) => {
            M.toast({html: '❌ Erreur: ' + error.message, classes: 'red'});
        });
}

// Handle Login
async function handleLogin(event) {
    event.preventDefault();
    
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const rememberMe = document.getElementById('remember-me').checked;

    try {
        // Set persistence based on "Remember me"
        const persistence = rememberMe ? 
            firebase.auth.Auth.Persistence.LOCAL : 
            firebase.auth.Auth.Persistence.SESSION;
        
        await auth.setPersistence(persistence);
        
        const userCredential = await auth.signInWithEmailAndPassword(email, password);
        
        M.toast({html: '✅ Connexion réussie!', classes: 'green'});
        
        // Close modal
        closeLoginModal();
        
        // Redirect to dashboard
        setTimeout(() => {
            window.location.href = 'parent-dashboard.html';
        }, 1500);
        
    } catch (error) {
        console.error('Error:', error);
        let errorMessage = 'Erreur de connexion';
        
        switch(error.code) {
            case 'auth/user-not-found':
                errorMessage = 'Aucun compte trouvé avec cet email';
                break;
            case 'auth/wrong-password':
                errorMessage = 'Mot de passe incorrect';
                break;
            case 'auth/invalid-email':
                errorMessage = 'Email invalide';
                break;
            case 'auth/user-disabled':
                errorMessage = 'Ce compte a été désactivé';
                break;
            default:
                errorMessage = error.message;
        }
        
        M.toast({html: '❌ ' + errorMessage, classes: 'red'});
    }
}

// Handle Signup
async function handleSignup(event) {
    event.preventDefault();
    
    const name = document.getElementById('signup-name').value;
    const email = document.getElementById('signup-email').value;
    const phone = document.getElementById('signup-phone').value;
    const childName = document.getElementById('signup-child').value;
    const password = document.getElementById('signup-password').value;
    const confirmPassword = document.getElementById('signup-confirm-password').value;

    // Validate passwords match
    if (password !== confirmPassword) {
        M.toast({html: '❌ Les mots de passe ne correspondent pas', classes: 'red'});
        return;
    }

    // Validate password strength
    if (password.length < 6) {
        M.toast({html: '❌ Le mot de passe doit contenir au moins 6 caractères', classes: 'red'});
        return;
    }

    try {
        // Create user account
        const userCredential = await auth.createUserWithEmailAndPassword(email, password);
        const user = userCredential.user;

        // Update user profile
        await user.updateProfile({
            displayName: name
        });

        // Save additional user data to Firestore
        await db.collection('parents').doc(user.uid).set({
            name: name,
            email: email,
            phone: phone,
            childName: childName,
            createdAt: firebase.firestore.FieldValue.serverTimestamp(),
            role: 'parent'
        });

        M.toast({html: '✅ Inscription réussie!', classes: 'green'});
        
        // Close modal
        closeLoginModal();
        
        // Redirect to dashboard
        setTimeout(() => {
            window.location.href = 'parent-dashboard.html';
        }, 1500);
        
    } catch (error) {
        console.error('Error:', error);
        let errorMessage = 'Erreur lors de l\'inscription';
        
        switch(error.code) {
            case 'auth/email-already-in-use':
                errorMessage = 'Cet email est déjà utilisé';
                break;
            case 'auth/invalid-email':
                errorMessage = 'Email invalide';
                break;
            case 'auth/weak-password':
                errorMessage = 'Mot de passe trop faible';
                break;
            default:
                errorMessage = error.message;
        }
        
        M.toast({html: '❌ ' + errorMessage, classes: 'red'});
    }
}

// Handle Contact Form
function handleContactForm(event) {
    event.preventDefault();
    
    const name = document.getElementById('contact-name').value;
    const email = document.getElementById('contact-email').value;
    const message = document.getElementById('contact-message').value;

    // Save to Firestore
    db.collection('contacts').add({
        name: name,
        email: email,
        message: message,
        timestamp: firebase.firestore.FieldValue.serverTimestamp(),
        status: 'new'
    })
    .then(() => {
        M.toast({html: '✅ Message envoyé avec succès!', classes: 'green'});
        
        // Reset form
        event.target.reset();
    })
    .catch((error) => {
        console.error('Error:', error);
        M.toast({html: '❌ Erreur lors de l\'envoi', classes: 'red'});
    });
}

// Check Authentication State
function checkAuthState() {
    auth.onAuthStateChanged((user) => {
        const loginBtn = document.getElementById('login-btn');
        
        if (user) {
            // User is signed in
            loginBtn.innerHTML = '<i class="fas fa-user"></i> MON COMPTE';
            loginBtn.onclick = () => {
                window.location.href = 'parent-dashboard.html';
            };
        } else {
            // User is signed out
            loginBtn.innerHTML = '<i class="fas fa-sign-in-alt"></i> CONNEXION';
            loginBtn.onclick = openLoginModal;
        }
    });
}

// Logout Function
function logout() {
    auth.signOut()
        .then(() => {
            M.toast({html: '✅ Déconnexion réussie', classes: 'green'});
            window.location.href = 'index.html';
        })
        .catch((error) => {
            console.error('Error:', error);
            M.toast({html: '❌ Erreur lors de la déconnexion', classes: 'red'});
        });
}

// Add scroll effect to navbar
window.addEventListener('scroll', function() {
    const navbar = document.getElementById('nb');
    if (window.scrollY > 100) {
        navbar.classList.remove('transparent');
        navbar.classList.add('grey', 'darken-4');
    } else {
        navbar.classList.add('transparent');
        navbar.classList.remove('grey', 'darken-4');
    }
});