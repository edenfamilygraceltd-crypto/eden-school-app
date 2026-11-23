package com.grace.eden_smart;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.grace.eden_smart.service.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.Locale;

public class SecretaryPortalActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference usersRef, attendanceRef;
    private LinearLayout content;
    private TextView nameText, attendanceText;
    private static final int NAVY = Color.rgb(27,44,126);
    private static final int BG = Color.rgb(243,246,252);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppUpdateChecker.check(this);
        getWindow().setStatusBarColor(NAVY);
        try {
            FirebaseService service = FirebaseService.getInstance(getApplicationContext());
            auth = service.getAuth();
            usersRef = service.getUsersReference();
            attendanceRef = service.getTeacherAttendanceReference();
        } catch (Exception e) {
            Toast.makeText(this, "Configuration Firebase indisponible.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        if (auth.getCurrentUser() == null) { goLogin(); return; }
        buildUi();
        verifySecretary();
    }

    private void buildUi() {
        setContentView(R.layout.activity_secretary_portal);
        nameText = findViewById(R.id.secretaryName);
        attendanceText = findViewById(R.id.secretaryAttendanceSummary);
        findViewById(R.id.secretaryLogout).setOnClickListener(v -> {
            auth.signOut();
            goLogin();
        });
    }

    private void verifySecretary() {
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String role = snap.child("role").getValue(String.class);
                if (!"secretary".equals(normalize(role)) && !"secretaire".equals(normalize(role))) {
                    auth.signOut(); Toast.makeText(SecretaryPortalActivity.this,"Accès réservé au secrétariat.",Toast.LENGTH_LONG).show(); goLogin(); return;
                }
                String name = snap.child("nom").getValue(String.class);
                if (name == null || name.trim().isEmpty()) name = snap.child("name").getValue(String.class);
                if (name == null || name.trim().isEmpty()) name = auth.getCurrentUser().getEmail();
                nameText.setText(name == null ? "Secrétariat" : name);
                loadAttendanceOverview();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                auth.signOut(); Toast.makeText(SecretaryPortalActivity.this,"Vérification du rôle impossible.",Toast.LENGTH_LONG).show(); goLogin();
            }
        });
    }

    private void loadAttendanceOverview() {
        attendanceText.setText("Le module de consultation des présences par branche sera activé après validation de la structure des données et des autorisations Firebase. Aucune donnée d'une autre branche n'est chargée dans cet écran.");
    }

    private String normalize(String role) {
        if (role == null) return "";
        return Normalizer.normalize(role.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").replaceAll("[\\s_-]+","");
    }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private void goLogin(){Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);finish();}
}
