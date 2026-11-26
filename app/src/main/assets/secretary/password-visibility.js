/**
 * Fonction globale pour basculer la visibilité des mots de passe
 * À utiliser avec le bouton toggle dans les formulaires de mot de passe
 */
function togglePasswordVisibility(inputId, evt) {
    const input = document.getElementById(inputId);
    const e = evt || window.event;

    // Try to find the toggle button from the event, otherwise fall back to the DOM
    let button = null;
    if (e && e.target && typeof e.target.closest === 'function') {
        button = e.target.closest('.password-toggle-btn');
    }
    if (!button && input) {
        const container = input.parentElement || input.closest('.password-input-container');
        if (container) button = container.querySelector('.password-toggle-btn');
    }

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
