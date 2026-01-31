/**
 * Fonction globale pour basculer la visibilité des mots de passe
 * À utiliser avec le bouton toggle dans les formulaires de mot de passe
 */
function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    const button = event.target.closest('.password-toggle-btn');
    
    if (!input || !button) return;
    
    const icon = button.querySelector('i');
    const isPassword = input.type === 'password';
    
    if (isPassword) {
        input.type = 'text';
        if (icon) {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }
    } else {
        input.type = 'password';
        if (icon) {
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        }
    }
}
