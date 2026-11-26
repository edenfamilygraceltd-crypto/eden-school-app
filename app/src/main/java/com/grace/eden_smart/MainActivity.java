package com.grace.eden_smart;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.grace.eden_smart.service.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference usersRef, teachersRef;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton signInButton, forgotPasswordButton;
    private TextView statusText;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        KeyboardInsets.apply(this);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signInButton = findViewById(R.id.signInButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        statusText = findViewById(R.id.statusText);
        try {
            FirebaseService service = FirebaseService.getInstance(getApplicationContext());
            auth = service.getAuth();
            usersRef = service.getUsersReference();
            teachersRef = service.getDatabase().getReference("teachers");
        } catch (Exception e) {
            showStatus("Initialisation Firebase impossible. Vérifiez la configuration Android.", true);
            signInButton.setEnabled(false);
            forgotPasswordButton.setEnabled(false);
            return;
        }
        signInButton.setOnClickListener(v -> signIn());
        forgotPasswordButton.setOnClickListener(v -> sendPasswordReset());
        View root = findViewById(R.id.main);
        if (root != null) { root.setAlpha(0f); root.animate().alpha(1f).setDuration(450).start(); }
    }

    private void signIn() {
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showStatus("Saisissez une adresse e-mail valide.", true); emailInput.requestFocus(); return;
        }
        if (TextUtils.isEmpty(password)) {
            showStatus("Saisissez votre mot de passe.", true); passwordInput.requestFocus(); return;
        }
        setBusy(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                setBusy(false); showStatus(authError(task.getException()), true); return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) { rejectAccess("Compte non vérifiable. Réessayez."); return; }
            verifySchoolRole(user);
        });
    }

    private void verifySchoolRole(@NonNull FirebaseUser user) {
        usersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String accountRole = snapshot.child("role").getValue(String.class);
                if (snapshot.exists() && accountRole != null && !accountRole.trim().isEmpty()) {
                    routeByRole(accountRole);
                    return;
                }
                // Older teacher accounts may have their profile stored under /teachers/{uid}.
                teachersRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot teacher) {
                        if (teacher.exists()) routeByRole(teacher.child("role").getValue(String.class));
                        else rejectAccess("Compte absent du répertoire du personnel de l'école.");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        rejectAccess("Impossible de vérifier le profil enseignant dans Firebase.");
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                rejectAccess("Impossible de vérifier votre rôle. Vérifiez les autorisations Firebase.");
            }
        });
    }

    private void routeByRole(String rawRole) {
        String role = normalizeRole(rawRole);
        if (isRole(role, "teacher", "enseignant", "enseignante")) {
            openPortal(TeacherPortalActivity.class);
        } else if (isRole(role, "director", "directeur")) {
            openPortal(DirectorPortalActivity.class);
        } else if (isRole(role, "admin", "administrateur")) {
            openPortal(DashboardActivity.class);
        } else if (isRole(role, "secretary", "secretaire")) {
            openPortal(SecretaryPortalActivity.class);
        } else if (isRole(role, "accountant", "comptable", "comptableprincipal", "comptablesecondaire", "chiefaccountant")) {
            openPortal(AccountantPortalActivity.class);
        } else {
            rejectAccess("Le rôle " + (rawRole == null ? "non défini" : rawRole)
                    + " n'est pas autorisé dans cette application.");
        }
    }

    private void openPortal(Class<?> destination) {
        setBusy(false); hideStatus();
        Intent intent = new Intent(MainActivity.this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void sendPasswordReset() {
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showStatus("Saisissez d'abord votre adresse e-mail scolaire.", true); emailInput.requestFocus(); return;
        }
        setBusy(true);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
            setBusy(false);
            if (task.isSuccessful()) showStatus("Si cette adresse correspond à un compte, un lien de réinitialisation a été envoyé.", false);
            else showStatus(authError(task.getException()), true);
        });
    }

    private boolean isRole(String role, String... accepted) {
        for (String candidate : accepted) if (candidate.equals(role)) return true;
        return false;
    }
    private String normalizeRole(String role) {
        if (role == null) return "";
        String normalized = Normalizer.normalize(role.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[\\s_-]+", "");
    }
    private String firstNonEmpty(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return "Personnel scolaire";
    }
    private void rejectAccess(String message) {
        setBusy(false);
        if (auth != null) auth.signOut();
        showStatus(message, true);
    }
    private void setBusy(boolean busy) {
        signInButton.setEnabled(!busy);
        forgotPasswordButton.setEnabled(!busy);
        signInButton.setText(busy ? "Vérification…" : "Se connecter");
    }
    private void showStatus(String message, boolean error) {
        statusText.setText(message);
        statusText.setTextColor(getColor(error ? android.R.color.holo_red_dark : android.R.color.holo_green_dark));
        statusText.setVisibility(View.VISIBLE);
    }
    private void hideStatus() { statusText.setText(""); statusText.setVisibility(View.GONE); }
    private String authError(Exception exception) {
        if (exception == null) return "Connexion impossible. Vérifiez vos identifiants.";
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("network")) return "Erreur réseau. Vérifiez votre connexion Internet.";
        if (message.contains("no user record") || message.contains("user not found")) return "Aucun compte trouvé pour cet e-mail.";
        if (message.contains("password is invalid") || message.contains("invalid credential")) return "E-mail ou mot de passe incorrect.";
        if (message.contains("too many requests")) return "Trop de tentatives. Veuillez patienter.";
        if (message.contains("permission_denied") || message.contains("permission denied")) return "Accès Firebase refusé pendant la vérification du rôle.";
        return "Connexion impossible. Vérifiez vos identifiants.";
    }
}
