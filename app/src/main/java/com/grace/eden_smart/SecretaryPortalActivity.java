package com.grace.eden_smart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;

/**
 * Native secretary portal. This Activity intentionally contains no WebView and
 * does not load the website HTML. XML owns the screen structure; Java owns
 * navigation, authorization and Firebase reads.
 */
public class SecretaryPortalActivity extends AppCompatActivity {
    private static final int NAVY = Color.rgb(44, 62, 80);
    private static final int NAVY_LIGHT = Color.rgb(52, 73, 94);
    private static final int BLUE = Color.rgb(52, 152, 219);
    private static final int GREEN = Color.rgb(22, 160, 133);
    private static final int BG = Color.rgb(236, 240, 241);
    private static final int MUTED = Color.rgb(127, 140, 141);

    private FirebaseAuth auth;
    private DatabaseReference usersRef, databaseRoot;
    private LinearLayout navigation, cards, records;
    private TextView userLabel, branchLabel, pageTitle, description, status;
    private String userName = "Secrétariat", branch = "", activeSection = "home";
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, List<Map<String, Object>>> cache = new LinkedHashMap<>();
    private final List<Map<String, Object>> teacherRows = new ArrayList<>();
    private final List<Map<String, Object>> studentRows = new ArrayList<>();
    private final List<Map<String, Object>> classRows = new ArrayList<>();
    private final List<Map<String, Object>> enrollmentRows = new ArrayList<>();
    private String teacherQuery = "", studentQuery = "", enrollmentQuery = "", enrollmentStatusFilter = "Tous", enrollmentYearFilter = "", enrollmentOriginFilter = "Toutes les origines", studentClassFilter = "", studentSectionFilter = "", studentViewFilter = "Tous les élèves";
    private String currentTeacherDetailId = "";
    private final List<Map<String, Object>> importRows = new ArrayList<>();
    private List<Map<String, Object>> exportRows = new ArrayList<>();
    private String pendingExportKind = "students";
    private static final int REQUEST_IMPORT_STUDENTS = 4201, REQUEST_EXPORT_XLSX = 4202, REQUEST_EXPORT_PDF = 4203;

    private static class Module {
        final String key, label, subtitle, collection;
        Module(String key, String label, String subtitle, String collection) {
            this.key = key; this.label = label; this.subtitle = subtitle; this.collection = collection;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        setContentView(R.layout.activity_secretary_portal);
        KeyboardInsets.apply(this);

        navigation = findViewById(R.id.secretaryNavigation);
        cards = findViewById(R.id.secretaryCards);
        records = findViewById(R.id.secretaryRecords);
        userLabel = findViewById(R.id.secretaryUserLabel);
        branchLabel = findViewById(R.id.secretaryBranchLabel);
        pageTitle = findViewById(R.id.secretaryPageTitle);
        description = findViewById(R.id.secretaryPageDescription);
        status = findViewById(R.id.secretaryStatus);

        FirebaseService service = FirebaseService.getInstance(getApplicationContext());
        auth = service.getAuth();
        databaseRoot = service.getDatabase().getReference();
        usersRef = service.getUsersReference();

        configureModules();
        findViewById(R.id.secretaryRefresh).setOnClickListener(v -> refreshActive());
        findViewById(R.id.secretaryLogout).setOnClickListener(v -> {
            auth.signOut();
            finish();
        });

        if (auth.getCurrentUser() == null) { finish(); return; }
        verifySecretaryProfile();
    }

    private void configureModules() {
        modules.clear();
        modules.add(new Module("home", "Accueil", "Vue générale du secrétariat", ""));
        modules.add(new Module("students", "Élèves", "Dossiers des élèves de votre branche", "students"));
        modules.add(new Module("inscription", "Nouvelle inscription", "Formulaire complet d’inscription scolaire", ""));
        modules.add(new Module("enrolled", "Inscrits", "Inscriptions et élèves inscrits", "inscriptions-scolaires"));
        modules.add(new Module("duplicates", "Doublons", "Contrôle des dossiers en double", "students"));
        modules.add(new Module("teachers", "Enseignants", "Personnel enseignant de votre branche", "users"));
        modules.add(new Module("attendance", "Pointage", "Présence du personnel", ""));
        modules.add(new Module("year", "Année scolaire", "Préparation et suivi de l'année scolaire", ""));
        modules.add(new Module("academic", "Académique", "Devoirs, tests, examens et bulletins", ""));
        modules.add(new Module("library", "Bibliothèque", "Livres et ressources pédagogiques", "books"));
        modules.add(new Module("messages", "Messages", "Communication du secrétariat", "messages"));
        modules.add(new Module("activities", "Activités", "Activités et annonces", "activities"));
        modules.add(new Module("users", "Utilisateurs", "Comptes autorisés de votre branche", "users"));
        modules.add(new Module("settings", "Paramètres", "Profil et préférences", ""));
        modules.add(new Module("trash", "Corbeille", "Éléments archivés ou supprimés", "trash"));
    }

    private void verifySecretaryProfile() {
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = first(value(snapshot, "role"), value(snapshot, "userRole"), value(snapshot, "type"));
                String normalizedRole = norm(role);
                if (!(normalizedRole.equals("secretary") || normalizedRole.equals("secretaire")
                        || normalizedRole.equals("secretariat"))) {
                    Toast.makeText(SecretaryPortalActivity.this,
                            "Accès réservé au portail Secrétaire.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                userName = first(value(snapshot, "name"), value(snapshot, "nom"),
                        value(snapshot, "displayName"), value(snapshot, "fullName"),
                        value(snapshot, "email"), "Secrétariat");
                branch = first(value(snapshot, "branchConfig"), value(snapshot, "branch"),
                        value(snapshot, "branche"), value(snapshot, "branchName"),
                        value(snapshot, "schoolBranch"), value(snapshot, "school_branch"));
                if (branch.isEmpty()) {
                    Toast.makeText(SecretaryPortalActivity.this,
                            "Aucune branche n'est attribuée à ce compte. Contactez la direction.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                userLabel.setText(userName + " • Espace Secrétariat");
                branchLabel.setText("Branche : " + branch);
                buildNavigation();
                render("home");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SecretaryPortalActivity.this,
                        "Vérification du compte impossible : " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void buildNavigation() {
        navigation.removeAllViews();
        for (Module module : modules) {
            MaterialButton item = new MaterialButton(this);
            item.setText(module.label);
            item.setAllCaps(false);
            item.setTextSize(12);
            item.setCornerRadius(dp(9));
            boolean selected = activeSection.equals(module.key);
            item.setTextColor(selected ? Color.WHITE : Color.rgb(236, 240, 241));
            item.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected ? GREEN : NAVY_LIGHT));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(42));
            params.setMargins(dp(4), 0, dp(4), 0);
            navigation.addView(item, params);
            item.setOnClickListener(v -> {
                if (module.key.equals("attendance")) {
                    startActivity(new Intent(this, NativeSecretaryAttendanceActivity.class));
                } else if (module.key.equals("academic")) {
                    startActivity(new Intent(this, AcademicPortalActivity.class));
                } else {
                    render(module.key);
                }
            });
        }
    }

    private void render(String key) {
        activeSection = key;
        buildNavigation();
        cards.removeAllViews();
        records.removeAllViews();
        Module module = findModule(key);
        pageTitle.setText(module == null ? "Portail Secrétaire" : module.label);
        description.setText(module == null ? "" : module.subtitle);
        status.setText("Données limitées à la branche " + branch + ".");
        if ("home".equals(key)) {
            addStatCard("Élèves", "Ouvrir les dossiers des élèves", "students");
            addStatCard("Nouvelle inscription", "Enregistrer un nouveau dossier scolaire", "inscription");
            addStatCard("Élèves inscrits", "Consulter et autoriser les inscriptions", "enrolled");
            addStatCard("Enseignants", "Consulter le personnel de la branche", "teachers");
            addStatCard("Pointage du personnel", "Valider les présences avec biométrie", "attendance");
            addStatCard("Section académique", "Devoirs, tests, examens et bulletins", "academic");
            addStatCard("Messages", "Ouvrir la communication", "messages");
            status.setText("Connecté en tant que " + userName + " • " + branch);
            return;
        }
        if ("attendance".equals(key)) {
            startActivity(new Intent(this, NativeSecretaryAttendanceActivity.class));
            return;
        }
        if ("academic".equals(key)) {
            startActivity(new Intent(this, AcademicPortalActivity.class));
            return;
        }
        if ("teachers".equals(key)) { loadTeachers(); return; }
        if ("students".equals(key)) { loadStudents(); return; }
        if ("enrolled".equals(key)) { loadEnrollments(); return; }
        if ("inscription".equals(key)) { showInscriptionForm(); return; }
        if ("duplicates".equals(key)) { loadStudents(); return; }
        if ("year".equals(key) || "academic".equals(key) || "settings".equals(key)) {
            String detail = "year".equals(key)
                    ? "La gestion de l'année scolaire sera reconstruite ici avec sauvegarde, vérification des inscriptions et promotion des classes."
                    : "academic".equals(key)
                    ? "Les modules Devoirs, Tests, Examens, Travaux de vacances, Bulletins, Classes et Délibération seront intégrés nativement."
                    : "Les paramètres du compte et les préférences seront gérés dans l'application, sans ouvrir le site web.";
            addInfo(module.label, detail);
            return;
        }
        if (module != null && !module.collection.isEmpty()) {
            loadCollection(module.collection, "teachers".equals(key) || "users".equals(key));
        } else {
            addInfo(module == null ? "Module" : module.label, "Ce module est identifié et sera connecté à ses données après vérification de sa structure Firebase.");
        }
    }

    private void addStatCard(String title, String detail, String target) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView heading = label(title, 16, NAVY, true);
        TextView sub = label(detail, 12, MUTED, false);
        body.addView(heading);
        body.addView(sub);
        card.addView(body);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(10);
        cards.addView(card, p);
        card.setOnClickListener(v -> {
            if ("attendance".equals(target)) startActivity(new Intent(this, NativeSecretaryAttendanceActivity.class));
            else render(target);
        });
    }

    private void addInfo(String title, String detail) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(16));
        body.addView(label(title, 16, NAVY, true));
        TextView text = label(detail, 13, MUTED, false);
        text.setPadding(0, dp(8), 0, 0);
        body.addView(text);
        card.addView(body);
        cards.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void loadCollection(String collection, boolean usersCollection) {
        status.setText("Chargement des données...");
        databaseRoot.child(collection).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                records.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> row = asMap(child);
                    if (row == null) continue;
                    if (!branchMatches(row)) continue;
                    if (usersCollection && !isRelevantUser(row)) continue;
                    count++;
                    if (count <= 100) addRecordCard(row, child.getKey(), usersCollection);
                }
                status.setText(count + " enregistrement(s) trouvé(s) pour " + branch
                        + (count > 100 ? " • 100 affichés" : ""));
                if (count == 0) addInfo("Aucun résultat", "Aucune donnée correspondant à cette branche n'a été trouvée.");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                status.setText("Lecture impossible : " + error.getMessage());
            }
        });
    }

    private boolean branchMatches(Map<String, Object> row) {
        String rowBranch = first(str(row.get("branch")), str(row.get("branche")),
                str(row.get("branchConfig")), str(row.get("branchName")),
                str(row.get("schoolBranch")), str(row.get("school_branch")));
        // Fail closed: a record without a branch is not exposed in the secretary portal.
        return !rowBranch.isEmpty() && norm(rowBranch).equals(norm(branch));
    }

    private boolean isRelevantUser(Map<String, Object> row) {
        String role = norm(first(str(row.get("role")), str(row.get("userRole")), str(row.get("type"))));
        return role.contains("teacher") || role.contains("enseign") || role.contains("secretary")
                || role.contains("secretaire");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (!(value instanceof Map)) return null;
        return new LinkedHashMap<>((Map<String, Object>) value);
    }

    private void addRecordCard(Map<String, Object> row, String id, boolean userRecord) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(12));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(12), dp(14), dp(12));
        String name = first(str(row.get("name")), str(row.get("nom")), str(row.get("fullName")),
                str(row.get("displayName")), str(row.get("studentName")), str(row.get("firstName")),
                str(row.get("title")), str(row.get("email")), id);
        body.addView(label(name, 15, NAVY, true));
        String detail = first(str(row.get("class")), str(row.get("classe")), str(row.get("classes")),
                str(row.get("role")), str(row.get("status")), str(row.get("email")));
        if (!detail.isEmpty() && !detail.equals(name)) {
            TextView sub = label(detail, 12, MUTED, false);
            sub.setPadding(0, dp(4), 0, 0);
            body.addView(sub);
        }
        card.addView(body);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        records.addView(card, p);
    }

    private void refreshActive() {
        cache.clear();
        render(activeSection);
        Toast.makeText(this, "Actualisation demandée", Toast.LENGTH_SHORT).show();
    }


    private String branchRootOf(String value) {
        String v=value==null?"":value.trim();
        int index=v.indexOf('_');return index>0?v.substring(0,index):v;
    }
    private String branchRoot() { return branchRootOf(branch); }
    private String sectionForClass(String classValue,String branchValue) {
        String c=norm(classValue), b=norm(branchValue);
        if(c.matches("p[1-5].*")||b.contains("primaire"))return "PRIMAIRE";
        if(c.matches("n[123].*")||b.contains("maternelle")||b.contains("creche"))return "MATERNELLE";
        return "MATERNELLE";
    }
    private String branchSectionForClass(String classValue) {
        String root=norm(branchRoot()), account=norm(branch), cls=norm(classValue);
        if(account.contains("_"))return branch;
        if(root.equals("gisozi"))return cls.matches("p[1-5].*")?"gisozi_primaire":"gisozi_maternelle";
        if(root.equals("kacyiru"))return cls.contains("creche")?"kacyiru_creche":"kacyiru_maternelle";
        return branchRoot();
    }
    private String rowBranch(Map<String, Object> row) {
        return first(str(row.get("branch")), str(row.get("branche")), str(row.get("branchConfig")),
                str(row.get("branchName")), str(row.get("schoolBranch")), str(row.get("school_branch")));
    }
    private boolean inSecretaryBranch(Map<String, Object> row) {
        String item = norm(rowBranch(row)), account = norm(branch);
        if (item.isEmpty() || account.isEmpty()) return false;
        if (item.equals(account)) return true;
        if (!account.contains("_") && item.startsWith(account + "_")) return true;
        if (account.contains("_") && item.equals(account.substring(0, account.indexOf('_')))) {
            String suffix = account.substring(account.indexOf('_') + 1);
            String section = norm(first(str(row.get("section")), str(row.get("sectionName")), str(row.get("schoolSection")), str(row.get("branchSection"))));
            if (!section.isEmpty()) return section.equals(account) || section.equals(suffix) || account.endsWith(section);
            String level = norm(first(str(row.get("level")), str(row.get("niveau")), str(row.get("classLevel"))));
            if (suffix.contains("maternelle") && (level.matches("n[123]") || className(row).toLowerCase(Locale.ROOT).matches("n[123].*"))) return true;
            if (suffix.contains("primaire") && (level.matches("p[1-5]") || className(row).toLowerCase(Locale.ROOT).matches("p[1-5].*"))) return true;
            if (suffix.contains("creche") && (level.contains("creche") || className(row).toLowerCase(Locale.ROOT).contains("creche"))) return true;
            Object assigned = row.get("classes");
            if (assigned instanceof List) for (Object c : (List<?>) assigned) {
                String value = norm(String.valueOf(c));
                if (suffix.contains("maternelle") && value.matches("n[123].*")) return true;
                if (suffix.contains("primaire") && value.matches("p[1-5].*")) return true;
                if (suffix.contains("creche") && value.contains("creche")) return true;
            }
        }
        return false;
    }
    private String displayName(Map<String, Object> row) {
        return first(str(row.get("name")), str(row.get("fullName")), str(row.get("displayName")),
                str(row.get("nomPrenom")), str(row.get("firstName")) + " " + str(row.get("lastName")));
    }
    private String className(Map<String, Object> row) {
        return first(str(row.get("name")), str(row.get("className")), str(row.get("class")),
                str(row.get("classe")), str(row.get("id")));
    }
    private String activeYear() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        return year + "-" + (year + 1);
    }
    private EditText input(String hint, String value, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(14);
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setMinLines(2);
            e.setGravity(Gravity.TOP);
        }
        return e;
    }
    private void formLabel(LinearLayout parent, String value) {
        TextView t = label(value, 12, NAVY, true);
        t.setPadding(0, dp(10), 0, dp(2));
        parent.addView(t);
    }
    private Spinner spinner(List<String> values, String selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(adapter);
        int index = values.indexOf(selected);
        if (index >= 0) s.setSelection(index);
        return s;
    }
    private MaterialButton actionButton(String text) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        return b;
    }
    private void loadClasses(Runnable done) {
        databaseRoot.child("classes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                classRows.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String,Object> row = asMap(child);
                    if (row == null) continue;
                    row.put("id", child.getKey());
                    if (inSecretaryBranch(row)) classRows.add(row);
                }
                if (done != null) done.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                status.setText("Lecture des classes impossible : " + error.getMessage());
            }
        });
    }
    private void loadTeachers() {
        status.setText("Chargement des enseignants...");
        loadClasses(() -> databaseRoot.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                teacherRows.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String,Object> row = asMap(child);
                    if (row == null) continue;
                    String role = norm(first(str(row.get("role")), str(row.get("userRole")), str(row.get("type"))));
                    if (!role.equals("teacher") && !role.equals("enseignant")) continue;
                    row.put("id", child.getKey());
                    row.put("uid", child.getKey());
                    if (inSecretaryBranch(row)) teacherRows.add(row);
                }
                if ("teachers".equals(activeSection)) renderTeachers();
                loadStudentsForTeacherRefresh();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                status.setText("Lecture des enseignants impossible : " + error.getMessage());
            }
        }));
    }
    private void renderTeachers() {
        cards.removeAllViews(); records.removeAllViews();
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        EditText search = input("Rechercher un enseignant...", teacherQuery, false);
        search.addTextChangedListener(new SimpleWatcher(() -> { teacherQuery = search.getText().toString(); renderTeacherCards(); }));
        bar.addView(search);
        LinearLayout actions = new LinearLayout(this);
        MaterialButton add = actionButton("+ Nouvel enseignant");
        add.setOnClickListener(v -> showTeacherForm(null));
        actions.addView(add);
        MaterialButton refresh = actionButton("Actualiser");
        refresh.setOnClickListener(v -> loadTeachers());
        actions.addView(refresh);
        bar.addView(actions);
        cards.addView(bar);
        status.setText(teacherRows.size() + " enseignant(s) dans " + branch);
        renderTeacherCards();
    }
    private void renderTeacherCards() {
        records.removeAllViews();
        int shown = 0;
        for (Map<String,Object> teacher : teacherRows) {
            String name = displayName(teacher);
            String searchable = norm(name + " " + str(teacher.get("email")) + " " + str(teacher.get("subject")));
            if (!teacherQuery.trim().isEmpty() && !searchable.contains(norm(teacherQuery))) continue;
            shown++;
            String id = str(teacher.get("id"));
            int assigned = 0;
            for (Map<String,Object> student : studentRows) if (id.equals(str(student.get("teacherId"))) && inSecretaryBranch(student)) assigned++;
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(12)); card.setCardElevation(dp(2)); card.setCardBackgroundColor(Color.WHITE);
            LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(14), dp(12), dp(14), dp(12));
            body.addView(label(name.isEmpty() ? "Enseignant sans nom" : name, 16, NAVY, true));
            body.addView(label(first(str(teacher.get("email")), "E-mail non renseigné") + " • " +
                    first(str(teacher.get("phone")), "Téléphone non renseigné"), 12, MUTED, false));
            body.addView(label("Matière : " + first(str(teacher.get("subject")), "Non renseignée"), 12, NAVY, false));
            Object raw = teacher.get("classes");
            String classes = raw instanceof List ? android.text.TextUtils.join(", ", (List<?>) raw) : str(raw);
            body.addView(label("Classes : " + (classes.isEmpty() ? "Aucune" : classes), 12, NAVY, false));
            body.addView(label(assigned + " élève(s) attribué(s) • " +
                    ("inactive".equalsIgnoreCase(str(teacher.get("status"))) ? "Inactif" : "Actif"), 12, GREEN, true));
            LinearLayout buttons = new LinearLayout(this);
            MaterialButton details = actionButton("Détails / Affectations");
            details.setOnClickListener(v -> showTeacherDetail(id));
            MaterialButton edit = actionButton("Modifier");
            edit.setOnClickListener(v -> showTeacherForm(teacher));
            MaterialButton remove = actionButton("Supprimer");
            remove.setOnClickListener(v -> confirmDeleteTeacher(id, name));
            buttons.addView(details, new LinearLayout.LayoutParams(0, dp(44), 1));
            buttons.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));
            buttons.addView(remove, new LinearLayout.LayoutParams(0, dp(44), 1));
            body.addView(buttons); card.addView(body);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(8);
            records.addView(card, p);
        }
        if (shown == 0) addInfo("Aucun enseignant", "Aucun enseignant ne correspond à la recherche dans votre branche.");
    }
    private List<String> classNamesForBranch() {
        List<String> names = new ArrayList<>();
        for (Map<String,Object> c : classRows) {
            String n = className(c);
            if (!n.isEmpty() && !names.contains(n)) names.add(n);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }
    private List<String> selectedClasses(Map<String,Object> row) {
        List<String> values = new ArrayList<>();
        Object raw = row == null ? null : row.get("classes");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) if (o != null) values.add(String.valueOf(o));
        } else if (raw != null) {
            for (String s : String.valueOf(raw).split(",")) if (!s.trim().isEmpty()) values.add(s.trim());
        }
        if (values.isEmpty() && row != null && !str(row.get("class")).isEmpty()) values.add(str(row.get("class")));
        return values;
    }
    private void showTeacherForm(Map<String,Object> existing) {
        if (classRows.isEmpty()) { loadClasses(() -> { if (classRows.isEmpty()) Toast.makeText(this, "Créez d’abord les classes dans la gestion académique.", Toast.LENGTH_LONG).show(); else showTeacherForm(existing); }); return; }
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(4));
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        EditText name = input("Nom complet", displayName(existing == null ? new HashMap<>() : existing), false);
        EditText email = input("Adresse e-mail", existing == null ? "" : str(existing.get("email")), false);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText phone = input("Téléphone", existing == null ? "" : str(existing.get("phone")), false);
        EditText code = input("Code personnel de pointage (EFS)", existing == null ? "" :
                first(str(existing.get("teacherCode")), str(existing.get("codeEnseignant"))), false);
        EditText subject = input("Matière enseignée", existing == null ? "" : str(existing.get("subject")), false);
        String branchValue = branchRootOf(existing == null ? branch : first(str(existing.get("branch")), branch));
        TextView branchText = label("Branche attribuée : " + branchValue, 13, GREEN, true);
        List<String> available = classNamesForBranch();
        List<String> chosen = selectedClasses(existing);
        TextView classPick = label("Classes : " + (chosen.isEmpty() ? "Aucune" : android.text.TextUtils.join(", ", chosen)) +
                "  •  Toucher pour modifier", 13, BLUE, true);
        classPick.setPadding(0, dp(12), 0, dp(12));
        classPick.setOnClickListener(v -> {
            boolean[] checked = new boolean[available.size()];
            for (int i=0;i<available.size();i++) checked[i] = chosen.contains(available.get(i));
            new AlertDialog.Builder(this).setTitle("Classes assignées")
                    .setMultiChoiceItems(available.toArray(new String[0]), checked, (dialog, which, isChecked) -> {
                        if (isChecked) { if (!chosen.contains(available.get(which))) chosen.add(available.get(which)); }
                        else chosen.remove(available.get(which));
                    }).setPositiveButton("Valider", (dialog, which) ->
                            classPick.setText("Classes : " + (chosen.isEmpty() ? "Aucune" : android.text.TextUtils.join(", ", chosen)) + "  •  Toucher pour modifier"))
                    .setNegativeButton("Annuler", null).show();
        });
        Spinner state = spinner(Arrays.asList("active", "inactive"),
                existing == null ? "active" : first(str(existing.get("status")), "active"));
        formLabel(form, "Nom complet"); form.addView(name);
        formLabel(form, "E-mail"); form.addView(email);
        formLabel(form, "Téléphone"); form.addView(phone);
        formLabel(form, "Code de pointage"); form.addView(code);
        formLabel(form, "Matière"); form.addView(subject);
        formLabel(form, "Branche"); form.addView(branchText);
        formLabel(form, "Classes assignées (sélection multiple)"); form.addView(classPick);
        formLabel(form, "Statut"); form.addView(state);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "Nouvel enseignant" : "Modifier l'enseignant")
                .setView(scroll).setNegativeButton("Annuler", null).setPositiveButton("Enregistrer", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String n = name.getText().toString().trim(), em = email.getText().toString().trim();
            String ph = phone.getText().toString().trim(), tc = code.getText().toString().trim().toUpperCase(Locale.ROOT);
            if (n.isEmpty() || em.isEmpty() || ph.isEmpty() || chosen.isEmpty()) {
                Toast.makeText(this, "Nom, e-mail, téléphone et au moins une classe sont obligatoires.", Toast.LENGTH_LONG).show(); return;
            }
            if (tc.isEmpty()) tc = generateTeacherCode();
            for (Map<String,Object> t : teacherRows) {
                String tid = str(t.get("id"));
                String other = first(str(t.get("teacherCode")), str(t.get("codeEnseignant")));
                if (!tid.equals(existing == null ? "" : str(existing.get("id"))) && other.equalsIgnoreCase(tc)) {
                    Toast.makeText(this, "Ce code de pointage est déjà utilisé.", Toast.LENGTH_LONG).show(); return;
                }
            }
            Map<String,Object> data = new HashMap<>();
            data.put("name", n); data.put("email", em); data.put("phone", ph); data.put("teacherCode", tc);
            data.put("subject", subject.getText().toString().trim()); data.put("branch", branchValue.toLowerCase(Locale.ROOT));
            data.put("branche", branchValue.toLowerCase(Locale.ROOT)); data.put("branchName", branchValue.toLowerCase(Locale.ROOT));
            data.put("branchConfig", branch);
            data.put("classes", new ArrayList<>(chosen)); data.put("role", "teacher");
            data.put("status", String.valueOf(state.getSelectedItem())); data.put("updatedAt", System.currentTimeMillis());
            for (Map<String,Object> c : classRows) if (chosen.contains(className(c))) {
                String level = first(str(c.get("level")), str(c.get("niveau")));
                if (!level.isEmpty()) data.put("level", level);
            }
            String id = existing == null ? databaseRoot.child("users").push().getKey() : str(existing.get("id"));
            if (id == null || id.isEmpty()) return;
            data.put("uid", id);
            if (existing == null) data.put("dateAdded", System.currentTimeMillis());
            Map<String,Object> updates = new HashMap<>();
            for (String key : data.keySet()) {
                updates.put("users/" + id + "/" + key, data.get(key));
                updates.put("teachers/" + id + "/" + key, data.get(key));
            }
            databaseRoot.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) { dialog.dismiss(); loadTeachers(); Toast.makeText(this, "Enseignant enregistré.", Toast.LENGTH_SHORT).show(); }
                else Toast.makeText(this, "Échec : " + (task.getException()==null?"":task.getException().getMessage()), Toast.LENGTH_LONG).show();
            });
        }));
        dialog.show();
    }
    private String generateTeacherCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom(); StringBuilder b = new StringBuilder("EFS-");
        for (int i=0;i<8;i++) b.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return b.toString();
    }
    private void confirmDeleteTeacher(String id, String name) {
        new AlertDialog.Builder(this).setTitle("Supprimer l'enseignant ?")
                .setMessage("Le profil « " + name + " » sera retiré des listes /users et /teachers. Les dossiers élèves resteront conservés.")
                .setNegativeButton("Annuler", null).setPositiveButton("Supprimer", (dialog, which) -> {
                    Map<String,Object> updates = new HashMap<>(); updates.put("users/"+id, null); updates.put("teachers/"+id, null);
                    databaseRoot.updateChildren(updates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) loadTeachers();
                        else Toast.makeText(this, "Suppression refusée : " + task.getException(), Toast.LENGTH_LONG).show();
                    });
                }).show();
    }
    private void showTeacherDetail(String teacherId) {
        Map<String,Object> teacher = null;
        for (Map<String,Object> t : teacherRows) if (teacherId.equals(str(t.get("id")))) teacher = t;
        if (teacher == null) return;
        currentTeacherDetailId = teacherId;
        final Map<String,Object> selectedTeacher = teacher;
        List<String> classes = selectedClasses(teacher);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(8), dp(16), dp(4));
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        TextView info = label("Classes assignées : " + android.text.TextUtils.join(", ", classes), 13, NAVY, true);
        body.addView(info);
        MaterialButton changeClasses = actionButton("Modifier les classes assignées");
        body.addView(changeClasses);
        Spinner classSelect = spinner(classes.isEmpty() ? Arrays.asList("Aucune classe") : classes, classes.isEmpty() ? "Aucune classe" : classes.get(0));
        formLabel(body, "Afficher les élèves de la classe"); body.addView(classSelect);
        TextView progress = label("", 12, GREEN, true); body.addView(progress);
        LinearLayout studentList = new LinearLayout(this); studentList.setOrientation(LinearLayout.VERTICAL); body.addView(studentList);
        Runnable renderClass = () -> {
            studentList.removeAllViews();
            String cls = String.valueOf(classSelect.getSelectedItem());
            List<Map<String,Object>> inClass = new ArrayList<>();
            for (Map<String,Object> s : studentRows) if (inSecretaryBranch(s) && studentHasClass(s, cls)) inClass.add(s);
            int assigned=0;
            for (Map<String,Object> s : inClass) if (teacherId.equals(str(s.get("teacherId")))) assigned++;
            progress.setText(assigned + " / " + inClass.size() + " élèves assignés (" +
                    (inClass.isEmpty()?0:Math.round(assigned*100f/inClass.size())) + "%)");
            for (Map<String,Object> s : inClass) {
                CheckBox check = new CheckBox(this); check.setText(displayName(s));
                check.setTextSize(13); check.setChecked(teacherId.equals(str(s.get("teacherId"))));
                check.setOnCheckedChangeListener((button, checked) -> setStudentTeacher(str(s.get("id")), checked ? teacherId : "", displayName(selectedTeacher)));
                studentList.addView(check);
            }
            if (inClass.isEmpty()) studentList.addView(label("Aucun élève dans cette classe.", 12, MUTED, false));
        };
        classSelect.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long rowId) { renderClass.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        changeClasses.setOnClickListener(v -> {
            List<String> available = classNamesForBranch(); boolean[] checked = new boolean[available.size()];
            for (int i=0;i<available.size();i++) checked[i] = classes.contains(available.get(i));
            new AlertDialog.Builder(this).setTitle("Classes assignées")
                    .setMultiChoiceItems(available.toArray(new String[0]), checked, (dlg, which, isChecked) -> {
                        if (isChecked) { if (!classes.contains(available.get(which))) classes.add(available.get(which)); }
                        else classes.remove(available.get(which));
                    }).setPositiveButton("Enregistrer", (dlg, which) -> {
                        Map<String,Object> up = new HashMap<>();
                        up.put("users/"+teacherId+"/classes", new ArrayList<>(classes));
                        up.put("teachers/"+teacherId+"/classes", new ArrayList<>(classes));
                        databaseRoot.updateChildren(up).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                info.setText("Classes assignées : " + android.text.TextUtils.join(", ", classes));
                                classSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classes));
                                if (!classes.isEmpty()) classSelect.setSelection(0);
                                else studentList.removeAllViews();
                                Toast.makeText(this, "Classes mises à jour.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).setNegativeButton("Annuler", null).show();
        });
        MaterialButton assignAll = actionButton("Assigner toute la classe à cet enseignant");
        assignAll.setOnClickListener(v -> {
            String cls = String.valueOf(classSelect.getSelectedItem());
            Map<String,Object> up = new HashMap<>();
            for (Map<String,Object> s : studentRows) if (inSecretaryBranch(s) && studentHasClass(s, cls)) {
                String sid = str(s.get("id")); up.put("students/"+sid+"/teacherId", teacherId);
                up.put("students/"+sid+"/teacherName", displayName(selectedTeacher));
            }
            if (up.isEmpty()) { Toast.makeText(this, "Aucun élève dans cette classe.", Toast.LENGTH_SHORT).show(); return; }
            databaseRoot.updateChildren(up).addOnCompleteListener(task -> {
                if (task.isSuccessful()) { loadStudentsForTeacherRefresh(); renderClass.run(); }
                else Toast.makeText(this, "Erreur : " + task.getException(), Toast.LENGTH_LONG).show();
            });
        });
        body.addView(assignAll);
        MaterialButton removeAll = actionButton("Retirer tous les élèves de cet enseignant");
        removeAll.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Retirer tous les élèves de cet enseignant dans votre branche ?")
                .setNegativeButton("Annuler", null).setPositiveButton("Confirmer", (dlg, which) -> {
                    Map<String,Object> up = new HashMap<>();
                    for (Map<String,Object> s : studentRows) if (inSecretaryBranch(s)
                            && teacherId.equals(str(s.get("teacherId")))) {
                        String sid = str(s.get("id")); up.put("students/"+sid+"/teacherId", null); up.put("students/"+sid+"/teacherName", null);
                    }
                    databaseRoot.updateChildren(up).addOnCompleteListener(task -> { if(task.isSuccessful()){loadStudentsForTeacherRefresh();renderClass.run();} });
                }).show());
        body.addView(removeAll);
        renderClass.run();
        new AlertDialog.Builder(this).setTitle("Détails de l'enseignant : " + displayName(teacher))
                .setView(scroll).setPositiveButton("Fermer", null).show();
    }
    private void setStudentTeacher(String studentId, String teacherId, String teacherName) {
        if (studentId.isEmpty()) return;
        Map<String,Object> updates = new HashMap<>();
        updates.put("students/"+studentId+"/teacherId", teacherId.isEmpty() ? null : teacherId);
        updates.put("students/"+studentId+"/teacherName", teacherId.isEmpty() ? null : teacherName);
        databaseRoot.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) loadStudentsForTeacherRefresh();
            else Toast.makeText(this, "Modification d’affectation refusée : " + task.getException(), Toast.LENGTH_LONG).show();
        });
    }
    private boolean studentHasClass(Map<String,Object> s, String cls) {
        if (cls == null || cls.isEmpty()) return false;
        for (String key : Arrays.asList("class","classe","className","class_name","classroom","classes","assignedClass","assignedClasses")) {
            Object v=s.get(key);
            if (v instanceof List) { for(Object x:(List<?>)v) if(norm(String.valueOf(x)).equals(norm(cls))) return true; }
            else if (v != null && norm(String.valueOf(v)).equals(norm(cls))) return true;
        }
        return false;
    }
    private void loadStudentsForTeacherRefresh() {
        databaseRoot.child("students").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentRows.clear();
                for (DataSnapshot child:snapshot.getChildren()) { Map<String,Object> row=asMap(child); if(row!=null){row.put("id",child.getKey());if(inSecretaryBranch(row))studentRows.add(row);} }
                if ("teachers".equals(activeSection)) renderTeacherCards();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable action;
        SimpleWatcher(Runnable action) { this.action=action; }
        @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
        @Override public void onTextChanged(CharSequence s,int start,int before,int count) { if(action!=null) action.run(); }
        @Override public void afterTextChanged(Editable e) { }
    }


    private void loadStudents() {
        status.setText("Chargement des élèves...");
        loadClasses(() -> databaseRoot.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot users) {
                teacherRows.clear();
                for (DataSnapshot child : users.getChildren()) {
                    Map<String,Object> row = asMap(child);
                    if (row == null) continue;
                    if (!"teacher".equalsIgnoreCase(first(str(row.get("role")), str(row.get("userRole"))))) continue;
                    row.put("id", child.getKey());
                    if (inSecretaryBranch(row) && !"inactive".equalsIgnoreCase(str(row.get("status"))) ) teacherRows.add(row);
                }
                databaseRoot.child("students").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        studentRows.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Map<String,Object> row = asMap(child);
                            if (row == null) continue;
                            row.put("id", child.getKey());
                            if (inSecretaryBranch(row)) studentRows.add(row);
                        }
                        if ("students".equals(activeSection)) renderStudents();
                        else if ("duplicates".equals(activeSection)) showStudentDuplicateAudit();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { if ("students".equals(activeSection)) status.setText("Lecture des élèves impossible : " + error.getMessage()); }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { status.setText("Lecture des enseignants impossible : " + error.getMessage()); }
        }));
    }
    private void renderStudents() {
        cards.removeAllViews(); records.removeAllViews();
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL);
        EditText search = input("Nom, classe, parent, téléphone...", studentQuery, false);
        search.addTextChangedListener(new SimpleWatcher(() -> { studentQuery = search.getText().toString(); renderStudentCards(); }));
        header.addView(search);
        if (!branch.contains("_") && (norm(branch).equals("gisozi") || norm(branch).equals("kacyiru"))) {
            List<String> sectionFilters = new ArrayList<>(Arrays.asList("Toutes les sections","Maternelle","Primaire"));
            if (norm(branch).equals("kacyiru")) sectionFilters.add("Crèche");
            Spinner sectionFilter = spinner(sectionFilters, studentSectionFilter.isEmpty() ? "Toutes les sections" : studentSectionFilter);
            sectionFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    studentSectionFilter = pos == 0 ? "" : norm(String.valueOf(sectionFilter.getSelectedItem())); renderStudentCards();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
            });
            header.addView(sectionFilter);
        }
        List<String> classFilters = new ArrayList<>(); classFilters.add("Toutes les classes"); classFilters.addAll(classNamesForBranch());
        Spinner classFilter = spinner(classFilters, studentClassFilter.isEmpty() ? "Toutes les classes" : studentClassFilter);
        classFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                studentClassFilter = pos == 0 ? "" : String.valueOf(classFilter.getSelectedItem()); renderStudentCards();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        header.addView(classFilter);
        Spinner viewFilter = spinner(Arrays.asList("Tous les élèves", "Actifs", "Inactifs", "Promus N3 → Primaire"), studentViewFilter);
        viewFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                studentViewFilter = String.valueOf(viewFilter.getSelectedItem()); renderStudentCards();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        header.addView(viewFilter);
        LinearLayout actions = new LinearLayout(this);
        MaterialButton add = actionButton("+ Nouvel élève"); add.setOnClickListener(v -> showStudentForm(null)); actions.addView(add);
        MaterialButton importBtn = actionButton("Importer Excel / Word / CSV"); importBtn.setOnClickListener(v -> pickStudentImport()); actions.addView(importBtn);
        MaterialButton csv = actionButton("Excel tous"); csv.setOnClickListener(v -> beginExport("students", false)); actions.addView(csv);
        MaterialButton pdf = actionButton("PDF tous"); pdf.setOnClickListener(v -> beginExport("students", true)); actions.addView(pdf);
        MaterialButton byClass = actionButton("PDF par classe"); byClass.setOnClickListener(v -> beginExport("students_by_class", true)); actions.addView(byClass);
        MaterialButton excelByClass = actionButton("Excel par classe"); excelByClass.setOnClickListener(v -> beginExport("students_by_class", false)); actions.addView(excelByClass);
        MaterialButton audit = actionButton("Audit classes"); audit.setOnClickListener(v -> showStudentClassAudit()); actions.addView(audit);
        MaterialButton duplicates = actionButton("Doublons"); duplicates.setOnClickListener(v -> showStudentDuplicateAudit()); actions.addView(duplicates);
        MaterialButton refresh = actionButton("Actualiser"); refresh.setOnClickListener(v -> loadStudents()); actions.addView(refresh);
        header.addView(actions); cards.addView(header);
        status.setText(studentRows.size() + " dossier(s) élève(s) dans " + branch);
        renderStudentCards();
    }
    private void renderStudentCards() {
        records.removeAllViews(); int count=0;
        for (Map<String,Object> student : studentRows) {
            String name = displayName(student);
            String cls = first(str(student.get("class")), str(student.get("classe")), str(student.get("className")),
                    student.get("classes") instanceof List ? android.text.TextUtils.join(", ", (List<?>)student.get("classes")) : str(student.get("classes")));
            String searchable = norm(name + " " + cls + " " + rowBranch(student) + " " + str(student.get("section")) + " "
                    + str(student.get("parent")) + " " + str(student.get("phone")) + " " + str(student.get("address"))
                    + " " + str(student.get("ville")) + " " + str(student.get("secteur"))
                    + " " + str(student.get("umurenge")) + " " + str(student.get("akagari")) + " " + str(student.get("umudugudu")));
            if (!studentQuery.isEmpty() && !searchable.contains(norm(studentQuery))) continue;
            if (!studentClassFilter.isEmpty() && !studentHasClass(student, studentClassFilter)) continue;
            if (!studentSectionFilter.isEmpty()) {
                String sectionValue = norm(first(str(student.get("section")),str(student.get("sectionName")),str(student.get("branchSection")),rowBranch(student)));
                String levelValue = norm(first(str(student.get("level")),str(student.get("niveau")),cls));
                boolean matches = sectionValue.contains(studentSectionFilter);
                if (!matches && "maternelle".equals(studentSectionFilter)) matches = levelValue.matches("n[123].*");
                if (!matches && "primaire".equals(studentSectionFilter)) matches = levelValue.matches("p[1-5].*");
                if (!matches && "creche".equals(studentSectionFilter)) matches = levelValue.contains("creche");
                if (!matches) continue;
            }
            if ("Actifs".equals(studentViewFilter) && !Arrays.asList("actif","active").contains(norm(str(student.get("status"))))) continue;
            if ("Inactifs".equals(studentViewFilter) && !Arrays.asList("inactif","inactive").contains(norm(str(student.get("status"))))) continue;
            if ("Promus N3 → Primaire".equals(studentViewFilter)) {
                String previous = norm(first(str(student.get("promotedFrom")),str(student.get("previousClass")),
                        str(student.get("promotionFrom")),str(student.get("previousLevel"))));
                boolean promoted = previous.contains("n3") || Boolean.TRUE.equals(student.get("promotedFromN3"))
                        || "promoted_n3".equals(norm(str(student.get("promotionStatus"))));
                if (!promoted) continue;
            }
            count++;
            MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(12)); card.setCardElevation(dp(1));
            LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(14),dp(10),dp(14),dp(10));
            body.addView(label(name.isEmpty()?"Élève sans nom":name,15,NAVY,true));
            body.addView(label("Classe : " + (cls.isEmpty()?"Non attribuée":cls) + " • " + first(str(student.get("gender")), "Sexe non renseigné"),12,MUTED,false));
            body.addView(label("Parent : " + first(str(student.get("parent")), "Non renseigné") + " • " + first(str(student.get("phone")), "Sans téléphone"),12,NAVY,false));
            String teacherId=str(student.get("teacherId")); Map<String,Object> teacher=null;
            for(Map<String,Object> t:teacherRows) if(teacherId.equals(str(t.get("id")))){teacher=t;break;}
            body.addView(label("Enseignant : " + (teacher==null?first(str(student.get("teacherName")),"Non assigné"):displayName(teacher)),12,MUTED,false));
            body.addView(label("Adresse : " + first(str(student.get("address")),str(student.get("adresse")),"Non renseignée"),11,MUTED,false));
            body.addView(label("Statut : " + first(str(student.get("status")),"Actif") + " • Ajouté le : " + first(str(student.get("dateAdded")),str(student.get("createdAt")),"Date non renseignée"),11,MUTED,false));
            LinearLayout buttons=new LinearLayout(this);
            MaterialButton edit=actionButton("Modifier"); edit.setOnClickListener(v->showStudentForm(student));
            MaterialButton profile=actionButton("Dossier complet"); profile.setOnClickListener(v->showStudentDetails(student));
            MaterialButton remove=actionButton("Supprimer"); remove.setOnClickListener(v->archiveStudent(student));
            buttons.addView(edit,new LinearLayout.LayoutParams(0,dp(42),1));
            buttons.addView(profile,new LinearLayout.LayoutParams(0,dp(42),1));
            buttons.addView(remove,new LinearLayout.LayoutParams(0,dp(42),1));
            body.addView(buttons); card.addView(body);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);records.addView(card,p);
        }
        if(count==0)addInfo("Aucun élève","Aucun dossier ne correspond à votre recherche.");
    }

    private void showStudentClassAudit() {
        List<String> problems=new ArrayList<>();List<String> available=classNamesForBranch();
        for(Map<String,Object> s:studentRows){String cls=first(str(s.get("class")),str(s.get("classe")),str(s.get("className")));
            boolean found=false;for(String c:available)if(norm(c).equals(norm(cls))){found=true;break;}
            if(cls.isEmpty()||!found)problems.add(displayName(s)+" — classe : "+(cls.isEmpty()?"non attribuée":cls));}
        String report=problems.isEmpty()?"Aucune anomalie détectée. Toutes les fiches correspondent à une classe connue de la branche."
                :problems.size()+" dossier(s) à vérifier :\\n\\n"+android.text.TextUtils.join("\\n",problems);
        new AlertDialog.Builder(this).setTitle("Audit classes / effectifs").setMessage(report).setPositiveButton("Fermer",null).show();
    }
    private void showStudentDuplicateAudit() {
        Map<String,List<Map<String,Object>>> groups=new LinkedHashMap<>();
        for(Map<String,Object> s:studentRows){String key=norm(displayName(s));if(key.isEmpty())continue;
            groups.computeIfAbsent(key,k->new ArrayList<>()).add(s);}
        List<List<Map<String,Object>>> duplicates=new ArrayList<>();
        for(List<Map<String,Object>> group:groups.values())if(group.size()>1)duplicates.add(group);
        if(duplicates.isEmpty()){
            new AlertDialog.Builder(this).setTitle("Analyse des doublons")
                    .setMessage("Aucun doublon de nom détecté dans les dossiers de cette branche.")
                    .setPositiveButton("Fermer",null).show();return;
        }
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(8),dp(16),dp(8));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        List<Map<String,Object>> selected=new ArrayList<>();
        for(List<Map<String,Object>> group:duplicates){
            body.addView(label("Groupe : "+displayName(group.get(0)),14,NAVY,true));
            for(int i=0;i<group.size();i++){
                Map<String,Object> student=group.get(i);
                CheckBox check=new CheckBox(this);
                check.setText((i==0?"Fiche de référence : ":"Doublon potentiel : ")+displayName(student)
                        +" • "+first(str(student.get("dob")),"date inconnue")+" • "+first(str(student.get("class")),str(student.get("classe"))));
                check.setTextSize(12);check.setChecked(false);check.setEnabled(i>0);
                check.setOnCheckedChangeListener((button,checked)->{if(checked){if(!selected.contains(student))selected.add(student);}else selected.remove(student);});
                body.addView(check);
            }
        }
        TextView note=label("Sélectionnez uniquement les fiches réellement en double. Les fiches choisies seront conservées dans la Corbeille et pourront être restaurées.",11,MUTED,false);
        note.setPadding(0,dp(12),0,dp(4));body.addView(note);
        new AlertDialog.Builder(this).setTitle("Analyse des doublons • "+duplicates.size()+" groupe(s)")
                .setView(scroll).setNegativeButton("Fermer",null)
                .setPositiveButton("Placer les sélectionnés dans la Corbeille",(dialog,which)->{
                    if(selected.isEmpty()){Toast.makeText(this,"Aucun doublon sélectionné.",Toast.LENGTH_SHORT).show();return;}
                    Map<String,Object> updates=new HashMap<>();
                    for(Map<String,Object> student:selected){
                        String id=str(student.get("id"));Map<String,Object> archived=new HashMap<>(student);archived.remove("id");
                        Map<String,Object> trash=new HashMap<>();trash.put("source","students");trash.put("originalPath","students/"+id);
                        trash.put("originalData",archived);trash.put("label",displayName(student));trash.put("reason","Doublon identifié par l'analyse du secrétariat");
                        trash.put("deletedAt",System.currentTimeMillis());trash.put("deletedBy",auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid());trash.put("branch",branch);
                        String trashId=databaseRoot.child("trash").push().getKey();updates.put("trash/"+trashId,trash);updates.put("students/"+id,null);
                    }
                    databaseRoot.updateChildren(updates).addOnCompleteListener(task->{if(task.isSuccessful()){loadStudents();Toast.makeText(this,selected.size()+" dossier(s) déplacé(s) dans la Corbeille.",Toast.LENGTH_LONG).show();}
                        else Toast.makeText(this,"Déplacement refusé : "+task.getException(),Toast.LENGTH_LONG).show();});
                }).show();
    }

    private void showStudentForm(Map<String,Object> existing) {
        if(classRows.isEmpty()){loadClasses(()->{if(classRows.isEmpty())Toast.makeText(this,"Aucune classe créée pour cette branche.",Toast.LENGTH_LONG).show();else showStudentForm(existing);});return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),dp(8),dp(18),dp(4));
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        EditText name=input("Nom complet",existing==null?"":displayName(existing),false);
        EditText dob=input("Date de naissance (AAAA-MM-JJ)",existing==null?"":first(str(existing.get("dob")),str(existing.get("dateNaissance"))),false);attachDatePicker(dob);
        EditText parent=input("Nom du parent",existing==null?"":first(str(existing.get("parent")),str(existing.get("parentName"))),false);
        EditText phone=input("Téléphone du parent",existing==null?"":first(str(existing.get("phone")),str(existing.get("parentPhone"))),false);
        EditText email=input("E-mail du parent",existing==null?"":str(existing.get("parentEmail")),false);
        EditText address=input("Adresse complète",existing==null?"":first(str(existing.get("address")),str(existing.get("adresse"))),true);
        EditText city=input("Ville",existing==null?"":str(existing.get("ville")),false);
        EditText sector=input("Secteur",existing==null?"":str(existing.get("secteur")),false);
        EditText cell=input("Umurenge",existing==null?"":str(existing.get("umurenge")),false);
        EditText village=input("Akagari / Umudugudu",existing==null?"":first(str(existing.get("akagari")),str(existing.get("umudugudu"))),false);
        Spinner studentStatus=spinner(Arrays.asList("Actif","Inactif"),existing==null?"Actif":first(str(existing.get("status")),"Actif"));
        List<String> classes=classNamesForBranch();
        String currentClass=existing==null?"":first(str(existing.get("class")),str(existing.get("classe")),str(existing.get("className")),
                existing.get("classes") instanceof List?(((List<?>)existing.get("classes")).isEmpty()?"":String.valueOf(((List<?>)existing.get("classes")).get(0))):str(existing.get("classes")));
        if(!currentClass.isEmpty()&&!classes.contains(currentClass))classes.add(0,currentClass);
        Spinner classSpinner=spinner(classes,currentClass);
        Spinner gender=spinner(Arrays.asList("Masculin","Féminin"),existing==null?"Masculin":first(str(existing.get("gender")),"Masculin"));
        List<String> teacherNames=new ArrayList<>();teacherNames.add("Aucun");
        for(Map<String,Object> t:teacherRows)teacherNames.add(displayName(t)+" ["+str(t.get("id"))+"]");
        String selectedTeacherId=existing==null?"":str(existing.get("teacherId"));
        int teacherIndex=0;
        for(int i=0;i<teacherRows.size();i++)if(selectedTeacherId.equals(str(teacherRows.get(i).get("id"))))teacherIndex=i+1;
        Spinner teacherSpinner=spinner(teacherNames,teacherNames.get(Math.min(teacherIndex,teacherNames.size()-1)));
        formLabel(form,"Nom complet");form.addView(name);
        formLabel(form,"Branche");form.addView(label(branchRoot()+" • "+branch,13,GREEN,true));
        formLabel(form,"Classe");form.addView(classSpinner);
        formLabel(form,"Sexe");form.addView(gender);
        formLabel(form,"Date de naissance");form.addView(dob);
        formLabel(form,"Parent / responsable");form.addView(parent);
        formLabel(form,"Téléphone parent");form.addView(phone);
        formLabel(form,"E-mail parent");form.addView(email);
        formLabel(form,"Adresse complète");form.addView(address);
        formLabel(form,"Ville");form.addView(city);
        formLabel(form,"Secteur");form.addView(sector);
        formLabel(form,"Umurenge");form.addView(cell);
        formLabel(form,"Akagari / Umudugudu");form.addView(village);
        formLabel(form,"Statut du dossier");form.addView(studentStatus);
        formLabel(form,"Enseignant responsable");form.addView(teacherSpinner);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Ajouter un élève":"Modifier le dossier de l'élève")
                .setView(scroll).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            String n=name.getText().toString().trim(), cls=String.valueOf(classSpinner.getSelectedItem());
            if(n.isEmpty()||cls.isEmpty()){Toast.makeText(this,"Le nom et la classe sont obligatoires.",Toast.LENGTH_LONG).show();return;}
            Map<String,Object> classRecord=null;for(Map<String,Object> c:classRows)if(norm(className(c)).equals(norm(cls)))classRecord=c;
            if(classRecord==null){Toast.makeText(this,"La classe sélectionnée n'existe pas dans votre branche.",Toast.LENGTH_LONG).show();return;}
            String tid="";if(teacherSpinner.getSelectedItemPosition()>0)tid=str(teacherRows.get(teacherSpinner.getSelectedItemPosition()-1).get("id"));
            String tname="";if(!tid.isEmpty())tname=displayName(teacherRows.get(teacherSpinner.getSelectedItemPosition()-1));
            Map<String,Object> data=new HashMap<>();
            String[] parts=n.split("\\s+",2);String firstName=parts[0],lastName=parts.length>1?parts[1]:"";
            data.put("name",n);data.put("firstName",firstName);data.put("lastName",lastName);data.put("prenom",firstName);data.put("nom",lastName);
            String baseBranch=branchRoot();
            data.put("branch",baseBranch);data.put("branche",baseBranch);data.put("branchName",baseBranch);data.put("schoolBranch",baseBranch);
            String branchSectionValue=branchSectionForClass(cls);
            data.put("branchSection",branchSectionValue);data.put("branchConfig",branchSectionValue);
            data.put("section",sectionForClass(cls,branchSectionValue));
            data.put("class",cls);data.put("classe",cls);data.put("classes",new ArrayList<>(Collections.singletonList(cls)));
            data.put("className",cls);data.put("class_name",cls);data.put("classroom",cls);data.put("assignedClass",cls);data.put("assignedClasses",new ArrayList<>(Collections.singletonList(cls)));
            data.put("classId",first(str(classRecord.get("id")),cls));data.put("level",first(str(classRecord.get("level")),str(classRecord.get("niveau"))));
            data.put("gender",String.valueOf(gender.getSelectedItem()));data.put("dob",dob.getText().toString().trim());
            data.put("parent",parent.getText().toString().trim());data.put("phone",phone.getText().toString().trim());
            data.put("parentPhone",phone.getText().toString().trim());data.put("parentEmail",email.getText().toString().trim());
            data.put("address",address.getText().toString().trim());data.put("adresse",address.getText().toString().trim());
            data.put("ville",city.getText().toString().trim());data.put("secteur",sector.getText().toString().trim());
            data.put("umurenge",cell.getText().toString().trim());data.put("akagari",village.getText().toString().trim());
            data.put("status",String.valueOf(studentStatus.getSelectedItem()));
            data.put("teacherId",tid.isEmpty()?null:tid);data.put("teacherName",tid.isEmpty()?null:tname);data.put("updatedAt",System.currentTimeMillis());
            data.put("academicYear",existing==null?activeYear():first(str(existing.get("academicYear")),activeYear()));
            String id=existing==null?databaseRoot.child("students").push().getKey():str(existing.get("id"));
            if(id==null||id.isEmpty())return;
            if(existing==null)data.put("dateAdded",System.currentTimeMillis());
            Map<String,Object> updates=new HashMap<>();for(String key:data.keySet())updates.put("students/"+id+"/"+key,data.get(key));
            databaseRoot.updateChildren(updates).addOnCompleteListener(task->{
                if(!task.isSuccessful()){Toast.makeText(this,"Échec : "+task.getException(),Toast.LENGTH_LONG).show();return;}
                Runnable finishSave=()->{dialog.dismiss();loadStudents();Toast.makeText(this,"Dossier élève enregistré et inscriptions liées synchronisées.",Toast.LENGTH_SHORT).show();};
                if(existing==null) finishSave.run(); else syncLinkedEnrollmentData(id,data,finishSave);
            });
        }));
        dialog.show();
    }
    private String childValue(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value == null ? "" : String.valueOf(value);
    }
    private void syncLinkedEnrollmentData(String studentId, Map<String,Object> student, Runnable done) {
        databaseRoot.child("inscriptions-scolaires").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> linked=new ArrayList<>();
                for(DataSnapshot child:snapshot.getChildren()){
                    String sid=first(childValue(child,"studentId"),childValue(child,"studentUid"),childValue(child,"student_id"));
                    if(studentId.equals(sid))linked.add(child);
                }
                String year=first(str(student.get("academicYear")),activeYear());Map<String,Object> up=new HashMap<>();
                for(DataSnapshot child:linked){
                    String oldYear=first(childValue(child,"academicYear"),childValue(child,"anneeAcademique"),childValue(child,"year"));
                    if(!oldYear.isEmpty()&&!norm(oldYear).equals(norm(year)))continue;
                    String base="inscriptions-scolaires/"+child.getKey()+"/";
                    up.put(base+"nomPrenom",student.get("name"));up.put(base+"classe",student.get("class"));
                    up.put(base+"class",student.get("class"));up.put(base+"branche",student.get("branchSection"));
                    up.put(base+"branchSection",student.get("branchSection"));up.put(base+"branchConfig",student.get("branchConfig"));
                    up.put(base+"section",student.get("section"));
                    up.put(base+"branch",student.get("branch"));up.put(base+"chefNomPrenom",student.get("parent"));
                    up.put(base+"chefTelephone",student.get("phone"));up.put(base+"chefEmail",student.get("parentEmail"));
                    up.put(base+"dateNaissance",student.get("dob"));up.put(base+"adresse",student.get("address"));
                    up.put(base+"sexe",student.get("gender"));up.put(base+"ville",student.get("ville"));
                    up.put(base+"secteur",student.get("secteur"));up.put(base+"umurenge",student.get("umurenge"));
                    up.put(base+"akagari",student.get("akagari"));up.put(base+"updatedAt",System.currentTimeMillis());
                }
                if(up.isEmpty()){done.run();return;}
                databaseRoot.updateChildren(up).addOnCompleteListener(t->{if(t.isSuccessful())done.run();else Toast.makeText(SecretaryPortalActivity.this,"Élève enregistré, mais certaines inscriptions liées n'ont pas pu être synchronisées.",Toast.LENGTH_LONG).show();});
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { done.run(); }
        });
    }
    private void showStudentDetails(Map<String,Object> s) {
        StringBuilder b=new StringBuilder();
        for(Map.Entry<String,Object> e:s.entrySet()) if(!"id".equals(e.getKey())) b.append(e.getKey()).append(" : ").append(String.valueOf(e.getValue())).append("\\n");
        new AlertDialog.Builder(this).setTitle(displayName(s)).setMessage(b.toString()).setPositiveButton("Fermer",null).show();
    }
    private void archiveStudent(Map<String,Object> student) {
        String id=str(student.get("id"));
        new AlertDialog.Builder(this).setTitle("Supprimer le dossier ?")
                .setMessage("Le dossier sera déplacé dans la Corbeille avant son retrait de la liste.")
                .setNegativeButton("Annuler",null).setPositiveButton("Continuer",(dlg,which)->{
                    Map<String,Object> archive=new HashMap<>(student);archive.remove("id");
                    Map<String,Object> trash=new HashMap<>();trash.put("source","students");trash.put("originalPath","students/"+id);
                    trash.put("originalData",archive);trash.put("label",displayName(student));trash.put("reason","Suppression depuis le portail secrétaire");
                    trash.put("deletedAt",System.currentTimeMillis());trash.put("deletedBy",auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid());trash.put("branch",branch);
                    String trashId=databaseRoot.child("trash").push().getKey();
                    Map<String,Object> updates=new HashMap<>();updates.put("trash/"+trashId,trash);updates.put("students/"+id,null);
                    databaseRoot.updateChildren(updates).addOnCompleteListener(task->{if(task.isSuccessful())loadStudents();else Toast.makeText(this,"Suppression refusée : "+task.getException(),Toast.LENGTH_LONG).show();});
                }).show();
    }
    private void pickStudentImport() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
        startActivityForResult(intent,REQUEST_IMPORT_STUDENTS);
    }
    private List<String> parseCsvLine(String line,char delimiter) {
        List<String> cells=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){cell.append('"');i++;}else quoted=!quoted;}
            else if(c==delimiter&&!quoted){cells.add(cell.toString().trim());cell.setLength(0);}else cell.append(c);}
        cells.add(cell.toString().trim());return cells;
    }
    private List<Map<String,Object>> parseStudentImport(Uri uri) throws Exception {
        String filename=String.valueOf(getContentResolver().getType(uri));
        String display="";try(android.database.Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(i>=0)display=c.getString(i);}}
        String lower=(display+" "+filename).toLowerCase(Locale.ROOT);
        if(lower.endsWith(".docx")||lower.contains("wordprocessingml"))return parseDocxStudents(uri);
        if(lower.endsWith(".xlsx")||lower.contains("spreadsheetml"))return parseXlsxStudents(uri);
        if(lower.endsWith(".xls"))throw new IllegalArgumentException("Le format .xls ancien n'est pas pris en charge. Enregistrez le fichier en .xlsx ou .csv.");
        try(InputStream in=getContentResolver().openInputStream(uri);BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
            List<String> lines=new ArrayList<>();String line;while((line=reader.readLine())!=null)if(!line.trim().isEmpty())lines.add(line);
            if(lines.isEmpty())return new ArrayList<>();
            char delim=lines.get(0).contains(";")?';':(lines.get(0).contains("\t")?'\t':',');
            List<String> headers=parseCsvLine(lines.get(0),delim);List<Map<String,Object>> result=new ArrayList<>();
            for(int i=1;i<lines.size();i++){List<String> cells=parseCsvLine(lines.get(i),delim);Map<String,Object> row=mapImportCells(headers,cells);if(!displayName(row).isEmpty())result.add(row);}
            return result;
        }
    }
    private Map<String,Object> mapImportCells(List<String> headers,List<String> cells) {
        Map<String,Object> row=new HashMap<>();
        for(int i=0;i<headers.size()&&i<cells.size();i++){String h=norm(headers.get(i)),v=cells.get(i).trim();if(v.isEmpty())continue;
            if(h.contains("parent")||h.contains("pere")||h.contains("mere")||h.contains("responsable"))row.put("parent",v);
            else if(h.contains("classe")||h.equals("class"))row.put("class",v);
            else if(h.contains("nom")||h.contains("name"))row.put("name",v);
            else if(h.contains("telephone")||h.contains("phone")||h.contains("tel"))row.put("phone",v);
            else if(h.contains("email")||h.contains("mail"))row.put("parentEmail",v);
            else if(h.contains("enseignant")||h.contains("teacher"))row.put("teacherName",v);
            else if(h.contains("adresse")||h.contains("address"))row.put("address",v);
            else if(h.contains("ville")||h.contains("city"))row.put("ville",v);
            else if(h.contains("secteur")||h.contains("sector"))row.put("secteur",v);
            else if(h.contains("umurenge")||h.contains("district"))row.put("umurenge",v);
            else if(h.contains("akagari")||h.contains("cell"))row.put("akagari",v);
            else if(h.contains("umudugudu")||h.contains("village"))row.put("umudugudu",v);
            else if(h.contains("sexe")||h.contains("gender"))row.put("gender",v);
            else if(h.contains("naissance")||h.contains("dob")||h.contains("birth"))row.put("dob",v);
        }return row;
    }
    private List<Map<String,Object>> parseDocxStudents(Uri uri) throws Exception {
        byte[] xml=null;try(ZipInputStream zip=new ZipInputStream(getContentResolver().openInputStream(uri))){ZipEntry e;while((e=zip.getNextEntry())!=null)if("word/document.xml".equals(e.getName())){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=zip.read(buf))>0)out.write(buf,0,n);xml=out.toByteArray();break;}}
        if(xml==null)throw new IllegalArgumentException("Le document Word ne contient pas de texte lisible.");
        XmlPullParser p=Xml.newPullParser();p.setInput(new java.io.ByteArrayInputStream(xml),"UTF-8");List<Map<String,Object>> rows=new ArrayList<>();StringBuilder para=new StringBuilder();int event;
        while((event=p.next())!=XmlPullParser.END_DOCUMENT){if(event==XmlPullParser.START_TAG&&"p".equals(p.getName()))para.setLength(0);else if(event==XmlPullParser.START_TAG&&"t".equals(p.getName())){String text=p.nextText();if(text!=null)para.append(text);}else if(event==XmlPullParser.END_TAG&&"p".equals(p.getName())){String name=para.toString().trim().replaceAll("^[0-9]+[.)-]\\s*","").replaceAll("^[•*-]\\s*","");if(!name.isEmpty()){Map<String,Object> row=new HashMap<>();row.put("name",name);rows.add(row);}}}
        return rows;
    }
    private List<Map<String,Object>> parseXlsxStudents(Uri uri) throws Exception {
        byte[] sharedXml=null;List<byte[]> worksheets=new ArrayList<>();
        try(ZipInputStream zip=new ZipInputStream(getContentResolver().openInputStream(uri))){
            ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                String name=entry.getName();
                if("xl/sharedStrings.xml".equals(name)||name.startsWith("xl/worksheets/sheet")&&name.endsWith(".xml")){
                    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;
                    while((count=zip.read(buffer))>0)out.write(buffer,0,count);
                    if("xl/sharedStrings.xml".equals(name))sharedXml=out.toByteArray();else worksheets.add(out.toByteArray());
                }
            }
        }
        if(worksheets.isEmpty())throw new IllegalArgumentException("Aucune feuille Excel lisible.");
        List<String> shared=new ArrayList<>();
        if(sharedXml!=null){
            XmlPullParser strings=Xml.newPullParser();strings.setInput(new java.io.ByteArrayInputStream(sharedXml),"UTF-8");
            StringBuilder value=new StringBuilder();int event;
            while((event=strings.next())!=XmlPullParser.END_DOCUMENT){
                if(event==XmlPullParser.START_TAG&&"si".equals(strings.getName()))value.setLength(0);
                else if(event==XmlPullParser.START_TAG&&"t".equals(strings.getName()))value.append(strings.nextText());
                else if(event==XmlPullParser.END_TAG&&"si".equals(strings.getName()))shared.add(value.toString());
            }
        }
        List<Map<String,Object>> result=new ArrayList<>();
        for(byte[] sheet:worksheets){
            List<List<String>> rows=new ArrayList<>();XmlPullParser parser=Xml.newPullParser();
            parser.setInput(new java.io.ByteArrayInputStream(sheet),"UTF-8");
            List<String> current=null;String cellType="",cellRef="";int event;
            while((event=parser.next())!=XmlPullParser.END_DOCUMENT){
                if(event==XmlPullParser.START_TAG&&"row".equals(parser.getName()))current=new ArrayList<>();
                else if(event==XmlPullParser.START_TAG&&"c".equals(parser.getName())){
                    cellType=parser.getAttributeValue(null,"t");cellRef=parser.getAttributeValue(null,"r");
                }else if(event==XmlPullParser.START_TAG&&"v".equals(parser.getName())&&current!=null){
                    String cell=parser.nextText();
                    if("s".equals(cellType)){int index=Integer.parseInt(cell);cell=index>=0&&index<shared.size()?shared.get(index):"";}
                    int col=columnIndex(cellRef);while(current.size()<=col)current.add("");current.set(col,cell);
                }else if(event==XmlPullParser.START_TAG&&"t".equals(parser.getName())&&current!=null&&"inlineStr".equals(cellType)){
                    String cell=parser.nextText();int col=columnIndex(cellRef);while(current.size()<=col)current.add("");current.set(col,cell);
                }else if(event==XmlPullParser.END_TAG&&"row".equals(parser.getName())&&current!=null){rows.add(current);current=null;}
            }
            if(rows.size()<2)continue;
            for(int i=1;i<rows.size();i++){Map<String,Object> row=mapImportCells(rows.get(0),rows.get(i));if(!displayName(row).isEmpty())result.add(row);}
        }
        return result;
    }
    private int columnIndex(String ref) { if(ref==null)return 0;int n=0;for(int i=0;i<ref.length();i++){char c=ref.charAt(i);if(c>='A'&&c<='Z')n=n*26+(c-'A'+1);else break;}return Math.max(0,n-1); }
    private Map<String,Object> findImportDuplicate(Map<String,Object> imported) {
        String name=norm(displayName(imported)), dob=norm(str(imported.get("dob")));
        String phone=norm(str(imported.get("phone"))), parent=norm(str(imported.get("parent")));
        for(Map<String,Object> existing:studentRows){
            String oldName=norm(displayName(existing)), oldDob=norm(first(str(existing.get("dob")),str(existing.get("dateNaissance"))));
            String oldPhone=norm(first(str(existing.get("phone")),str(existing.get("parentPhone"))));
            String oldParent=norm(first(str(existing.get("parent")),str(existing.get("parentName"))));
            if(!name.isEmpty()&&name.equals(oldName))return existing;
            if(!name.isEmpty()&&name.length()>5&&oldName.length()>5&&similarity(name,oldName)>=0.90
                    && ((!dob.isEmpty()&&dob.equals(oldDob))||(!phone.isEmpty()&&phone.equals(oldPhone))))return existing;
            if(!dob.isEmpty()&&dob.equals(oldDob)&&!parent.isEmpty()&&parent.equals(oldParent))return existing;
        }
        return null;
    }
    private double similarity(String a,String b) {
        int[][] d=new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++)d[i][0]=i;
        for(int j=0;j<=b.length();j++)d[0][j]=j;
        for(int i=1;i<=a.length();i++)for(int j=1;j<=b.length();j++)
            d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
        int max=Math.max(a.length(),b.length());return max==0?1d:1d-(double)d[a.length()][b.length()]/max;
    }
    private void showImportPreview() {
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(14),dp(8),dp(14),dp(8));
        Spinner year=spinner(Arrays.asList(activeYear(),(Calendar.getInstance().get(Calendar.YEAR)-1)+"-"+Calendar.getInstance().get(Calendar.YEAR)),activeYear());
        formLabel(body,"Année académique");body.addView(year);
        Spinner defaultClass=spinner(new ArrayList<String>(){{add("Aucune classe");addAll(classNamesForBranch());}},"Aucune classe");
        formLabel(body,"Classe par défaut (facultative)");body.addView(defaultClass);
        TextView summary=label(importRows.size()+" ligne(s) lue(s). Les doublons sont signalés avant l'import.",12,BLUE,true);body.addView(summary);
        LinearLayout rowsBox=new LinearLayout(this);rowsBox.setOrientation(LinearLayout.VERTICAL);
        ScrollView listScroll=new ScrollView(this);listScroll.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(360)));listScroll.addView(rowsBox);body.addView(listScroll);
        for(Map<String,Object> row:importRows){
            String n=displayName(row);Map<String,Object> match=findImportDuplicate(row);
            if(match!=null){row.put("_matchId",str(match.get("id")));row.put("_matchName",displayName(match));}
            List<String> options=Arrays.asList("Ajouter","Mettre à jour","Écraser","Ignorer","Supprimer");
            Spinner action=spinner(options,match==null?"Ajouter":"Ignorer");
            LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(0,dp(6),0,dp(6));
            item.addView(label(n+(match==null?"":" • Correspondance : "+displayName(match)),13,NAVY,true));
            item.addView(label(first(str(row.get("class")),"Classe non fournie")+" • "+first(str(row.get("parent")),"Parent non fourni"),11,MUTED,false));
            item.addView(action);row.put("_actionView",action);rowsBox.addView(item);
        }
        ScrollView wrapper=new ScrollView(this);wrapper.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Analyse et import des élèves").setView(wrapper)
                .setNegativeButton("Annuler",null).setPositiveButton("Importer",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            int added=0,updated=0,deleted=0,errors=0;Map<String,Object> updates=new HashMap<>();
            String yearValue=String.valueOf(year.getSelectedItem());String classDefault=String.valueOf(defaultClass.getSelectedItem());
            for(Map<String,Object> row:importRows){
                Spinner action=(Spinner)row.get("_actionView");String act=action==null?"Ignorer":String.valueOf(action.getSelectedItem());
                if("Ignorer".equals(act))continue;
                String n=displayName(row);if(n.isEmpty()){errors++;continue;}
                String matchId=str(row.get("_matchId"));
                if("Supprimer".equals(act)){
                    Map<String,Object> matched=null;for(Map<String,Object> s:studentRows)if(matchId.equals(str(s.get("id"))))matched=s;
                    if(matched==null){errors++;continue;}
                    Map<String,Object> archived=new HashMap<>(matched);archived.remove("id");
                    Map<String,Object> trash=new HashMap<>();trash.put("source","students");trash.put("originalPath","students/"+matchId);
                    trash.put("originalData",archived);trash.put("label",displayName(matched));trash.put("reason","Suppression depuis l'import du portail secrétaire");
                    trash.put("deletedAt",System.currentTimeMillis());trash.put("deletedBy",auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid());trash.put("branch",branch);
                    String trashId=databaseRoot.child("trash").push().getKey();updates.put("trash/"+trashId,trash);updates.put("students/"+matchId,null);deleted++;continue;
                }
                String cls=first(str(row.get("class")),"Aucune classe".equals(classDefault)?"":classDefault);
                if(!cls.isEmpty()&&!classNamesForBranch().contains(cls)){errors++;continue;}
                Map<String,Object> data=new HashMap<>();data.put("name",n);data.put("branch",branchRoot());data.put("branche",branchRoot());
                data.put("branchName",branchRoot());data.put("schoolBranch",branchRoot());
                String importedBranchSection=branchSectionForClass(cls);
                data.put("branchSection",importedBranchSection);data.put("branchConfig",importedBranchSection);
                data.put("section",sectionForClass(cls,importedBranchSection));
                data.put("status","Actif");
                data.put("parent",str(row.get("parent")));data.put("phone",str(row.get("phone")));data.put("parentPhone",str(row.get("phone")));
                data.put("parentEmail",str(row.get("parentEmail")));data.put("gender",str(row.get("gender")));data.put("dob",str(row.get("dob")));
                data.put("address",str(row.get("address")));data.put("ville",str(row.get("ville")));data.put("secteur",str(row.get("secteur")));
                data.put("umurenge",str(row.get("umurenge")));data.put("akagari",str(row.get("akagari")));data.put("umudugudu",str(row.get("umudugudu")));
                data.put("academicYear",yearValue);data.put("importedFrom","FILE_IMPORT");data.put("importedAt",System.currentTimeMillis());
                String importedTeacher=str(row.get("teacherName"));
                if(!importedTeacher.isEmpty()){for(Map<String,Object> t:teacherRows)if(norm(displayName(t)).equals(norm(importedTeacher))){data.put("teacherId",str(t.get("id")));data.put("teacherName",displayName(t));break;}
                    if(!data.containsKey("teacherId"))data.put("teacherName",importedTeacher);}
                if(!cls.isEmpty()){
                    data.put("section",sectionForClass(cls,branch));data.put("class",cls);data.put("classe",cls);data.put("className",cls);data.put("class_name",cls);
                    data.put("classroom",cls);data.put("assignedClass",cls);data.put("classes",new ArrayList<>(Collections.singletonList(cls)));
                    data.put("assignedClasses",new ArrayList<>(Collections.singletonList(cls)));
                    for(Map<String,Object> c:classRows)if(norm(className(c)).equals(norm(cls)))data.put("classId",str(c.get("id")));
                }
                if("Mettre à jour".equals(act)||"Écraser".equals(act)){
                    if(matchId.isEmpty()){errors++;continue;}
                    if("Mettre à jour".equals(act)){data.entrySet().removeIf(e->e.getValue()==null||String.valueOf(e.getValue()).trim().isEmpty());}
                    for(String key:data.keySet())updates.put("students/"+matchId+"/"+key,data.get(key));updated++;
                }else{
                    String id=databaseRoot.child("students").push().getKey();if(id==null){errors++;continue;}
                    data.put("dateAdded",System.currentTimeMillis());for(String key:data.keySet())updates.put("students/"+id+"/"+key,data.get(key));added++;
                }
            }
            if(updates.isEmpty()){Toast.makeText(this,"Aucun dossier à importer.",Toast.LENGTH_LONG).show();return;}
            final int addedCount=added, updatedCount=updated, deletedCount=deleted, errorCount=errors;
            databaseRoot.updateChildren(updates).addOnCompleteListener(task->{
                if(task.isSuccessful()){dialog.dismiss();loadStudents();Toast.makeText(this,addedCount+" ajouté(s), "+updatedCount+" modifié(s), "+deletedCount+" supprimé(s), "+errorCount+" erreur(s).",Toast.LENGTH_LONG).show();}
                else Toast.makeText(this,"Import refusé : "+task.getException(),Toast.LENGTH_LONG).show();
            });
        }));
        dialog.show();
    }
    private void beginExport(String kind, boolean pdf) {
        exportRows=new ArrayList<>();pendingExportKind=kind;
        List<Map<String,Object>> source="enrolled".equals(kind)?enrollmentRows:studentRows;
        for(Map<String,Object> row:source){
            String search=norm(displayName(row)+" "+str(row.get("class"))+" "+str(row.get("classe"))+" "+str(row.get("parent"))+" "+str(row.get("chefNomPrenom"))+" "+str(row.get("phone"))+" "+str(row.get("chefTelephone"))+" "+str(row.get("codeUnique")));
            if(("students".equals(kind)||"students_by_class".equals(kind))&&!studentQuery.isEmpty()&&!search.contains(norm(studentQuery)))continue;
            if("enrolled".equals(kind)){
                boolean approved=isEnrollmentApproved(row);
                if("En attente".equals(enrollmentStatusFilter)&&approved)continue;
                if("Autorisé".equals(enrollmentStatusFilter)&&!approved)continue;
                String year=first(str(row.get("anneeAcademique")),str(row.get("academicYear")),str(row.get("year")));
                if(!enrollmentYearFilter.isEmpty()&&!"Toutes les années".equals(enrollmentYearFilter)&&!norm(year).equals(norm(enrollmentYearFilter)))continue;
                String sourceValue=norm(first(str(row.get("source")),str(row.get("origin")),str(row.get("createdByRole"))));
                if("Index.html".equals(enrollmentOriginFilter)&&!sourceValue.contains("index"))continue;
                if("Comptable".equals(enrollmentOriginFilter)&&!sourceValue.contains("comptable")&&!sourceValue.contains("account"))continue;
                if("Secrétariat".equals(enrollmentOriginFilter)&&!sourceValue.contains("secretary")&&!sourceValue.contains("secretaire"))continue;
                if(!enrollmentQuery.isEmpty()&&!search.contains(norm(enrollmentQuery)))continue;
            }
            exportRows.add(new HashMap<>(row));
        }
        if("students_by_class".equals(kind)) Collections.sort(exportRows,(a,b)->first(str(a.get("class")),str(a.get("classe"))).compareToIgnoreCase(first(str(b.get("class")),str(b.get("classe")))));
        if(exportRows.isEmpty()){Toast.makeText(this,"Aucune donnée à exporter.",Toast.LENGTH_SHORT).show();return;}
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(pdf?"application/pdf":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String suffix=pdf?".pdf":".xlsx";intent.putExtra(Intent.EXTRA_TITLE,"Eden_"+kind+"_"+activeYear()+suffix);
        startActivityForResult(intent,pdf?REQUEST_EXPORT_PDF:REQUEST_EXPORT_XLSX);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        if(requestCode==REQUEST_IMPORT_STUDENTS){
            try{importRows.clear();importRows.addAll(parseStudentImport(uri));if(importRows.isEmpty())Toast.makeText(this,"Aucun élève lisible dans le fichier.",Toast.LENGTH_LONG).show();else showImportPreview();}
            catch(Exception e){Toast.makeText(this,"Lecture du fichier impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }else if(requestCode==REQUEST_EXPORT_XLSX||requestCode==REQUEST_EXPORT_PDF){
            try(OutputStream out=getContentResolver().openOutputStream(uri)){
                if(out==null)throw new IllegalStateException("Impossible d'ouvrir le fichier de destination.");
                if(requestCode==REQUEST_EXPORT_XLSX)writeXlsxExport(out,"students_by_class".equals(pendingExportKind));else writePdfExport(out);
                Toast.makeText(this,"Export terminé : "+exportRows.size()+" dossier(s).",Toast.LENGTH_LONG).show();
            }catch(Exception e){Toast.makeText(this,"Échec de l'export : "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }


    private String[] exportHeaders() {
        return new String[]{"Nom complet","Référence","Branche","Section","Classe","Niveau","Sexe","Date de naissance",
                "Parent / chef de famille","Téléphone parent","E-mail parent","Pièce identité parent","Conjoint(e)",
                "Téléphone conjoint","Pièce identité conjoint","Adresse","Ville","Secteur","Umurenge","Akagari","Umudugudu",
                "Transport","Enseignant responsable","Année académique","Statut","Code SDMS","Code inscription","Origine"};
    }
    private List<String> exportValues(Map<String,Object> s) {
        String tid=str(s.get("teacherId")),tn=first(str(s.get("teacherName")),"");
        for(Map<String,Object> t:teacherRows)if(tid.equals(str(t.get("id"))))tn=displayName(t);
        return Arrays.asList(displayName(s),first(str(s.get("reference")),str(s.get("codeUnique"))),
                rowBranch(s),first(str(s.get("section")),str(s.get("sectionName"))),
                first(str(s.get("class")),str(s.get("classe")),str(s.get("niveau"))),first(str(s.get("level")),str(s.get("niveau"))),
                first(str(s.get("gender")),str(s.get("sexe"))),first(str(s.get("dob")),str(s.get("dateNaissance"))),
                first(str(s.get("parent")),str(s.get("parentName")),str(s.get("chefNomPrenom"))),
                first(str(s.get("parentPhone")),str(s.get("phone")),str(s.get("chefTelephone"))),
                first(str(s.get("parentEmail")),str(s.get("chefEmail"))),first(str(s.get("chefPieceIdentite")),str(s.get("parentId"))),
                first(str(s.get("conjointNomPrenom")),str(s.get("spouseName"))),str(s.get("conjointTelephone")),
                str(s.get("conjointPieceIdentite")),first(str(s.get("address")),str(s.get("adresse"))),
                str(s.get("ville")),str(s.get("secteur")),str(s.get("umurenge")),str(s.get("akagari")),str(s.get("umudugudu")),
                str(s.get("transport")),tn,first(str(s.get("academicYear")),str(s.get("anneeAcademique")),str(s.get("year"))),
                first(str(s.get("status")),str(s.get("statut"))),str(s.get("sdmsCode")),
                first(str(s.get("codeUnique")),str(s.get("reference"))),first(str(s.get("source")),str(s.get("origin"))));
    }
    private String xmlEscape(String value) {
        return str(value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&apos;");
    }
    private String excelColumn(int value) {
        StringBuilder b=new StringBuilder();int n=value+1;
        while(n>0){int rem=(n-1)%26;b.insert(0,(char)('A'+rem));n=(n-1)/26;}
        return b.toString();
    }
    private void zipText(ZipOutputStream zip,String name,String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }
    private String makeSheetXml(List<Map<String,Object>> rows) {
        StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        int rowNumber=1;xml.append("<row r=\"1\">");String[] headers=exportHeaders();
        for(int c=0;c<headers.length;c++)xml.append(excelCell(c,1,headers[c]));
        xml.append("</row>");
        for(Map<String,Object> row:rows){
            rowNumber++;xml.append("<row r=\"").append(rowNumber).append("\">");
            List<String> values=exportValues(row);
            for(int c=0;c<values.size();c++)xml.append(excelCell(c,rowNumber,values.get(c)));
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");return xml.toString();
    }
    private String excelCell(int column,int row,String value) {
        return "<c r=\""+excelColumn(column)+row+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"
                +xmlEscape(value)+"</t></is></c>";
    }
    private void writeXlsxExport(OutputStream out,boolean oneSheetPerClass) throws Exception {
        Map<String,List<Map<String,Object>>> sheets=new LinkedHashMap<>();
        if(oneSheetPerClass){
            for(Map<String,Object> row:exportRows){
                String cls=first(str(row.get("class")),str(row.get("classe")),str(row.get("niveau")),"Non attribuée");
                sheets.computeIfAbsent(cls,k->new ArrayList<>()).add(row);
            }
        }else sheets.put("Dossiers",exportRows);
        if(sheets.isEmpty())sheets.put("Dossiers",new ArrayList<>());
        List<String> sheetNames=new ArrayList<>();Set<String> used=new HashSet<>();
        for(String original:sheets.keySet()){
            String safe=original.replace('/','_').replace((char)92,'_').replace('?','_').replace('*','_').replace('[','_').replace(']','_').replace(':','_').trim();
            if(safe.isEmpty())safe="Classe";if(safe.length()>31)safe=safe.substring(0,31);
            String base=safe;int suffix=2;while(used.contains(safe.toLowerCase(Locale.ROOT))){String tail="_"+suffix++;safe=base.substring(0,Math.min(base.length(),31-tail.length()))+tail;}
            used.add(safe.toLowerCase(Locale.ROOT));sheetNames.add(safe);
        }
        ZipOutputStream zip=new ZipOutputStream(out);
        StringBuilder types=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        StringBuilder workbook=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        StringBuilder rels=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        int index=1;
        for(Map.Entry<String,List<Map<String,Object>>> entry:sheets.entrySet()){
            String sheetName=sheetNames.get(index-1);
            workbook.append("<sheet name=\"").append(xmlEscape(sheetName)).append("\" sheetId=\"").append(index)
                    .append("\" r:id=\"rId").append(index).append("\"/>");
            rels.append("<Relationship Id=\"rId").append(index).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(index).append(".xml\"/>");
            types.append("<Override PartName=\"/xl/worksheets/sheet").append(index)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            index++;
        }
        workbook.append("</sheets></workbook>");rels.append("</Relationships>");types.append("</Types>");
        zipText(zip,"[Content_Types].xml",types.toString());
        zipText(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
        zipText(zip,"xl/workbook.xml",workbook.toString());
        zipText(zip,"xl/_rels/workbook.xml.rels",rels.toString());
        index=1;for(Map.Entry<String,List<Map<String,Object>>> entry:sheets.entrySet()){
            zipText(zip,"xl/worksheets/sheet"+index+".xml",makeSheetXml(entry.getValue()));index++;
        }
        zip.finish();zip.flush();
    }

    private void writeCsvExport(OutputStream out) throws Exception {
        String[] headers={"Nom complet","Référence","Branche","Section","Classe","Niveau","Sexe","Date de naissance",
                "Parent / chef de famille","Téléphone parent","E-mail parent","Pièce identité parent",
                "Conjoint(e)","Téléphone conjoint","Pièce identité conjoint","Adresse","Ville","Secteur","Umurenge","Akagari","Umudugudu",
                "Transport","Enseignant responsable","Année académique","Statut","Code SDMS","Code inscription","Origine"};
        StringBuilder csv=new StringBuilder();
        for(int i=0;i<headers.length;i++){if(i>0)csv.append(',');csv.append(csvQuote(headers[i]));}
        csv.append((char)13).append((char)10);
        for(Map<String,Object> s:exportRows){
            String tid=str(s.get("teacherId")),tn=first(str(s.get("teacherName")),"");
            for(Map<String,Object> t:teacherRows)if(tid.equals(str(t.get("id"))))tn=displayName(t);
            List<String> values=Arrays.asList(displayName(s),first(str(s.get("reference")),str(s.get("codeUnique"))),
                    rowBranch(s),first(str(s.get("section")),str(s.get("sectionName"))),
                    first(str(s.get("class")),str(s.get("classe")),str(s.get("niveau"))),first(str(s.get("level")),str(s.get("niveau"))),
                    first(str(s.get("gender")),str(s.get("sexe"))),first(str(s.get("dob")),str(s.get("dateNaissance"))),
                    first(str(s.get("parent")),str(s.get("parentName")),str(s.get("chefNomPrenom"))),
                    first(str(s.get("parentPhone")),str(s.get("phone")),str(s.get("chefTelephone"))),
                    first(str(s.get("parentEmail")),str(s.get("chefEmail"))),first(str(s.get("chefPieceIdentite")),str(s.get("parentId"))),
                    first(str(s.get("conjointNomPrenom")),str(s.get("spouseName"))),str(s.get("conjointTelephone")),
                    str(s.get("conjointPieceIdentite")),first(str(s.get("address")),str(s.get("adresse"))),
                    str(s.get("ville")),str(s.get("secteur")),str(s.get("umurenge")),str(s.get("akagari")),str(s.get("umudugudu")),
                    str(s.get("transport")),tn,first(str(s.get("academicYear")),str(s.get("anneeAcademique")),
                    str(s.get("year"))),first(str(s.get("status")),str(s.get("statut"))),str(s.get("sdmsCode")),
                    first(str(s.get("codeUnique")),str(s.get("reference"))),first(str(s.get("source")),str(s.get("origin"))));
            for(int i=0;i<values.size();i++){if(i>0)csv.append(',');csv.append(csvQuote(values.get(i)));}
            csv.append((char)13).append((char)10);
        }
        out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private String csvQuote(String value) {
        String q=String.valueOf((char)34);
        return q+str(value).replace(q,q+q)+q;
    }
    private void writePdfExport(OutputStream out) throws Exception {
        PdfDocument pdf=new PdfDocument();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);int page=1,y=48;
        PdfDocument.Page p=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,page).create());
        android.graphics.Canvas canvas=p.getCanvas();
        paint.setTextSize(15);paint.setFakeBoldText(true);
        canvas.drawText("EDEN FAMILY SCHOOL - DOSSIERS DES "+pendingExportKind.toUpperCase(Locale.ROOT),32,y,paint);y+=28;
        paint.setTextSize(9);paint.setFakeBoldText(false);
        canvas.drawText("Branche : "+branch+" | Année : "+activeYear()+" | Total : "+exportRows.size(),32,y,paint);y+=24;
        String previousClass="";
        for(int i=0;i<exportRows.size();i++){
            Map<String,Object>s=exportRows.get(i);String cls=first(str(s.get("class")),str(s.get("classe")),str(s.get("niveau")));
            if("students_by_class".equals(pendingExportKind)&&!cls.equals(previousClass)){
                if(y>780){pdf.finishPage(p);page++;p=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,page).create());canvas=p.getCanvas();y=48;}
                paint.setFakeBoldText(true);canvas.drawText("CLASSE : "+(cls.isEmpty()?"NON ATTRIBUÉE":cls),32,y,paint);paint.setFakeBoldText(false);y+=20;previousClass=cls;
            }
            String[] lines={
                    (i+1)+". "+displayName(s),
                    "Classe : "+cls+" | Branche : "+rowBranch(s)+" | Année : "+first(str(s.get("academicYear")),str(s.get("anneeAcademique"))),
                    "Sexe : "+first(str(s.get("gender")),str(s.get("sexe")))+" | Naissance : "+first(str(s.get("dob")),str(s.get("dateNaissance"))),
                    "Parent : "+first(str(s.get("parent")),str(s.get("chefNomPrenom")))+" | Tél : "+first(str(s.get("phone")),str(s.get("chefTelephone"))),
                    "E-mail : "+first(str(s.get("parentEmail")),str(s.get("chefEmail")))+" | Adresse : "+first(str(s.get("address")),str(s.get("adresse"))),
                    "Transport : "+str(s.get("transport"))+" | SDMS : "+str(s.get("sdmsCode"))+" | Statut : "+first(str(s.get("status")),str(s.get("statut")))
            };
            for(String line:lines){
                if(y>800){pdf.finishPage(p);page++;p=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,page).create());canvas=p.getCanvas();y=48;}
                canvas.drawText(line.length()>112?line.substring(0,112):line,32,y,paint);y+=14;
            }
            y+=8;
        }
        pdf.finishPage(p);pdf.writeTo(out);pdf.close();
    }



    private List<String> enrollmentBranches() {
        String b=norm(branch);List<String> values=new ArrayList<>();
        if(b.equals("gisozi")){values.add("gisozi_maternelle");values.add("gisozi_primaire");}
        else if(b.equals("kacyiru")){values.add("kacyiru_maternelle");values.add("kacyiru_creche");}
        else values.add(branch);
        return values;
    }
    private List<String> classNamesForEnrollmentBranch(String branchValue) {
        List<String> names=new ArrayList<>();String target=norm(branchValue);
        for(Map<String,Object> c:classRows){
            String row=norm(rowBranch(c));boolean allowed=row.equals(target);
            if(!allowed&&!target.contains("_")&&row.startsWith(target+"_"))allowed=true;
            if(!allowed&&target.contains("_")&&row.equals(target.substring(0,target.indexOf('_')))){
                String suffix=target.substring(target.indexOf('_')+1);
                String section=norm(first(str(c.get("section")),str(c.get("sectionName"))));
                String level=norm(first(str(c.get("level")),str(c.get("niveau"))));
                String classLabel=norm(className(c));
                if(!section.isEmpty())allowed=section.equals(suffix)||section.equals(target);
                else if(suffix.contains("maternelle"))allowed=level.matches("n[123]")||classLabel.matches("n[123].*");
                else if(suffix.contains("primaire"))allowed=level.matches("p[1-5]")||classLabel.matches("p[1-5].*");
                else if(suffix.contains("creche"))allowed=level.contains("creche")||classLabel.contains("creche");
            }
            if(allowed){String n=className(c);if(!n.isEmpty()&&!names.contains(n))names.add(n);}
        }
        Collections.sort(names,String.CASE_INSENSITIVE_ORDER);return names;
    }
    private void attachDatePicker(EditText field) {
        field.setFocusable(false);
        field.setOnClickListener(v -> {
            Calendar cal=Calendar.getInstance();
            try { Date date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(field.getText().toString()); if(date!=null)cal.setTime(date); }
            catch(Exception ignored) { }
            new android.app.DatePickerDialog(this,(picker,year,month,day)->
                    field.setText(String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day)),
                    cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }
    private boolean validIsoDate(String value) {
        try {
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US);format.setLenient(false);
            Date date=format.parse(value);return date!=null&&!date.after(new Date());
        } catch(Exception e) { return false; }
    }
    private void showInscriptionForm() {
        loadClasses(() -> buildInscriptionForm());
    }
    private void buildInscriptionForm() {
        cards.removeAllViews();records.removeAllViews();
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),dp(8),dp(16),dp(8));
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        EditText child=input("Nom et prénom de l'enfant *","",false);
        EditText dob=input("Date de naissance (AAAA-MM-JJ) *","",false);attachDatePicker(dob);
        EditText origin=input("École de provenance *","",false);
        Spinner gender=spinner(Arrays.asList("GARÇON","FILLE"),"GARÇON");
        Spinner section=spinner(Arrays.asList("MATERNELLE","PRIMAIRE"),"MATERNELLE");
        Spinner level=spinner(Arrays.asList("N1","N2","N3"),"N1");
        EditText sdms=input("Code SDMS (obligatoire si requis)","",false);
        List<String> branches=enrollmentBranches();
        Spinner branchSection=spinner(branches,branches.get(0));
        List<String> classOptions=new ArrayList<>();classOptions.add("Aucune classe");classOptions.addAll(classNamesForEnrollmentBranch(String.valueOf(branchSection.getSelectedItem())));
        Spinner cls=spinner(classOptions,"Aucune classe");
        branchSection.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                List<String> next=new ArrayList<>();next.add("Aucune classe");next.addAll(classNamesForEnrollmentBranch(String.valueOf(branchSection.getSelectedItem())));
                cls.setAdapter(new ArrayAdapter<>(SecretaryPortalActivity.this,android.R.layout.simple_spinner_dropdown_item,next));
                if(!next.contains(String.valueOf(cls.getSelectedItem())))cls.setSelection(0);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        Spinner year=spinner(Arrays.asList(activeYear(),"2025-2026","2027-2028"),activeYear());
        Spinner transport=spinner(Arrays.asList("OUI","NON"),"NON");
        EditText parent=input("Nom et prénom du chef de famille *","",false);
        EditText parentPhone=input("Téléphone du chef de famille (10 chiffres) *","",false);
        EditText parentId=input("Pièce d'identité du chef de famille *","",false);
        EditText spouse=input("Nom et prénom du conjoint (facultatif)","",false);
        EditText spousePhone=input("Téléphone du conjoint","",false);
        EditText spouseId=input("Pièce d'identité du conjoint","",false);
        EditText city=input("Ville *","Kigali",false),sector=input("Secteur *","",false),umurenge=input("Umurenge *","",false);
        EditText akagari=input("Akagari *","",false),umudugudu=input("Umudugudu *","",false);
        addEnrollmentField(form,"INFORMATION DE L'ENFANT",child);
        addEnrollmentField(form,"Date de naissance",dob);addEnrollmentField(form,"École de provenance",origin);
        formLabel(form,"Sexe");form.addView(gender);
        addEnrollmentField(form,"NIVEAU SCOLAIRE",section);
        formLabel(form,"Niveau");form.addView(level);
        addEnrollmentField(form,"SDMS Code",sdms);
        addEnrollmentField(form,"BRANCHE / SECTION",branchSection);
        addEnrollmentField(form,"Classe (facultative)",cls);
        addEnrollmentField(form,"Année académique",year);
        addEnrollmentField(form,"TRANSPORT",transport);
        addEnrollmentField(form,"CHEF DE FAMILLE",parent);
        addEnrollmentField(form,"Téléphone",parentPhone);addEnrollmentField(form,"Pièce d'identité",parentId);
        addEnrollmentField(form,"CONJOINT(E) - facultatif",spouse);addEnrollmentField(form,"Téléphone conjoint",spousePhone);
        addEnrollmentField(form,"Pièce d'identité conjoint",spouseId);
        addEnrollmentField(form,"ADRESSE - Ville",city);addEnrollmentField(form,"Secteur",sector);
        addEnrollmentField(form,"Umurenge",umurenge);addEnrollmentField(form,"Akagari",akagari);addEnrollmentField(form,"Umudugudu",umudugudu);
        section.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                List<String> levels="PRIMAIRE".equals(String.valueOf(section.getSelectedItem()))
                        ?Arrays.asList("P1","P2","P3","P4","P5"):Arrays.asList("N1","N2","N3");
                level.setAdapter(new ArrayAdapter<>(SecretaryPortalActivity.this,android.R.layout.simple_spinner_dropdown_item,levels));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        MaterialButton submit=actionButton("Soumettre l'inscription");
        submit.setOnClickListener(v->{
            String n=child.getText().toString().trim(),birth=dob.getText().toString().trim();
            String phone=parentPhone.getText().toString().trim();
            if(n.isEmpty()||!validIsoDate(birth)||origin.getText().toString().trim().isEmpty()||parent.getText().toString().trim().isEmpty()
                    ||phone.isEmpty()||parentId.getText().toString().trim().isEmpty()||city.getText().toString().trim().isEmpty()
                    ||sector.getText().toString().trim().isEmpty()||umurenge.getText().toString().trim().isEmpty()
                    ||akagari.getText().toString().trim().isEmpty()||umudugudu.getText().toString().trim().isEmpty()){
                Toast.makeText(this,"Veuillez remplir tous les champs obligatoires.",Toast.LENGTH_LONG).show();return;
            }
            if(!phone.matches("[0-9]{10}")){Toast.makeText(this,"Le téléphone du chef de famille doit contenir 10 chiffres.",Toast.LENGTH_LONG).show();return;}
            String spousePhoneValue=spousePhone.getText().toString().trim();
            if(!spousePhoneValue.isEmpty()&&!spousePhoneValue.matches("[0-9]{10}")){Toast.makeText(this,"Le téléphone du conjoint doit contenir 10 chiffres.",Toast.LENGTH_LONG).show();return;}
            String sectionValue=String.valueOf(section.getSelectedItem()),branchValue=String.valueOf(branchSection.getSelectedItem());
            if(branchValue.startsWith("gisozi_")&&!branchValue.equals("gisozi_"+sectionValue.toLowerCase(Locale.ROOT))){Toast.makeText(this,"La section choisie ne correspond pas à la branche sélectionnée.",Toast.LENGTH_LONG).show();return;}
            String levelValue=String.valueOf(level.getSelectedItem()),classValue=String.valueOf(cls.getSelectedItem());
            if("Aucune classe".equals(classValue))classValue="";
            long now=System.currentTimeMillis();String code="INS-"+now;
            Map<String,Object> inscription=new HashMap<>();
            inscription.put("nomPrenom",n);inscription.put("dateNaissance",birth);inscription.put("ecoleProvenance",origin.getText().toString().trim());
            inscription.put("sexe",String.valueOf(gender.getSelectedItem()));inscription.put("section",sectionValue);
            inscription.put("niveau",levelValue);inscription.put("sdmsCode",sdms.getText().toString().trim());
            inscription.put("branche",branchValue);inscription.put("branch",branchRootOf(branchValue));inscription.put("branchSection",branchValue);
            inscription.put("transport",String.valueOf(transport.getSelectedItem()));
            inscription.put("chefNomPrenom",parent.getText().toString().trim());inscription.put("chefTelephone",phone);
            inscription.put("chefPieceIdentite",parentId.getText().toString().trim());
            inscription.put("conjointNomPrenom",spouse.getText().toString().trim());inscription.put("conjointTelephone",spousePhone.getText().toString().trim());
            inscription.put("conjointPieceIdentite",spouseId.getText().toString().trim());
            inscription.put("ville",city.getText().toString().trim());inscription.put("secteur",sector.getText().toString().trim());
            inscription.put("umurenge",umurenge.getText().toString().trim());inscription.put("akagari",akagari.getText().toString().trim());
            inscription.put("umudugudu",umudugudu.getText().toString().trim());inscription.put("codeUnique",code);
            inscription.put("dateInscription",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US).format(new Date(now)));
            inscription.put("timestamp",now);inscription.put("statut","En attente de validation");
            inscription.put("classe",classValue);inscription.put("anneeAcademique",String.valueOf(year.getSelectedItem()));
            inscription.put("academicYear",String.valueOf(year.getSelectedItem()));inscription.put("source","secretary");
            Map<String,Object> student=new HashMap<>();student.put("name",n);student.put("class",classValue);
            student.put("classes",classValue.isEmpty()?new ArrayList<String>():new ArrayList<>(Collections.singletonList(classValue)));
            student.put("level",levelValue);student.put("academicYear",String.valueOf(year.getSelectedItem()));
            student.put("gender","GARÇON".equals(String.valueOf(gender.getSelectedItem()))?"Masculin":"Féminin");
            student.put("dob",birth);student.put("parent",parent.getText().toString().trim());student.put("phone",phone);
            student.put("parentEmail","");student.put("address",city.getText()+", "+sector.getText()+", "+umurenge.getText()+", "+akagari.getText()+", "+umudugudu.getText());
            student.put("dateAdded",now);student.put("status","Actif");student.put("teacherId",null);student.put("source","secretary");
            student.put("branch",branchRootOf(branchValue));student.put("branche",branchRootOf(branchValue));
            student.put("branchName",branchRootOf(branchValue));student.put("schoolBranch",branchRootOf(branchValue));
            student.put("branchSection",branchValue);student.put("branchConfig",branchValue);student.put("section",sectionValue);
            String studentId=databaseRoot.child("students").push().getKey(),inscriptionId=databaseRoot.child("inscriptions-scolaires").push().getKey();
            if(studentId==null||inscriptionId==null)return;
            inscription.put("studentId",studentId);
            Map<String,Object> updates=new HashMap<>();
            for(String key:student.keySet())updates.put("students/"+studentId+"/"+key,student.get(key));
            for(String key:inscription.keySet())updates.put("inscriptions-scolaires/"+inscriptionId+"/"+key,inscription.get(key));
            databaseRoot.updateChildren(updates).addOnCompleteListener(task->{
                if(task.isSuccessful()){Toast.makeText(this,"Inscription enregistrée. Code : "+code,Toast.LENGTH_LONG).show();render("enrolled");}
                else Toast.makeText(this,"Échec de l'inscription : "+task.getException(),Toast.LENGTH_LONG).show();
            });
        });
        cards.addView(label("Formulaire d'inscription scolaire • Branche : "+branch,14,NAVY,true));
        cards.addView(scroll);cards.addView(submit);
        status.setText("Les dossiers créés ici sont enregistrés ensemble dans /students et /inscriptions-scolaires.");
    }
    private void addEnrollmentField(LinearLayout parent,String title,View field){formLabel(parent,title);parent.addView(field);}
    private void loadEnrollments() {
        status.setText("Chargement des inscriptions...");
        databaseRoot.child("students").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot students){
                studentRows.clear();for(DataSnapshot c:students.getChildren()){Map<String,Object> r=asMap(c);if(r!=null){r.put("id",c.getKey());if(inSecretaryBranch(r))studentRows.add(r);}}
                databaseRoot.child("inscriptions-scolaires").addListenerForSingleValueEvent(new ValueEventListener(){
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot){
                        enrollmentRows.clear();for(DataSnapshot c:snapshot.getChildren()){Map<String,Object> r=asMap(c);if(r!=null){r.put("id",c.getKey());if(inSecretaryBranch(r))enrollmentRows.add(r);}}
                        if ("enrolled".equals(activeSection)) renderEnrollments();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e){if ("enrolled".equals(activeSection)) status.setText("Lecture des inscriptions impossible : "+e.getMessage());}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Lecture des élèves impossible : "+e.getMessage());}
        });
    }
    private boolean isEnrollmentApproved(Map<String,Object> i) {
        String a=norm(first(str(i.get("statut")),str(i.get("status"))));
        if (Arrays.asList("confirme","confirmee","approuve","approved","confirmed","autorise","authorized","valide").contains(a)
                || Boolean.TRUE.equals(i.get("approved")) || Boolean.TRUE.equals(i.get("authorized"))
                || !str(i.get("authorizedBy")).isEmpty() || !str(i.get("confirmedBy")).isEmpty()) return true;
        Map<String,Object> linked = findLinkedStudent(i);
        if (linked == null) return false;
        String id = str(i.get("id"));
        boolean fromAccounting = id.equals(str(linked.get("fromInscriptionRequest")));
        boolean active = Arrays.asList("active","actif").contains(norm(str(linked.get("status"))));
        boolean hasClass = !first(str(linked.get("assignedClass")),str(linked.get("classAssigned")),
                str(linked.get("classeAffectee")),str(linked.get("class"))).isEmpty();
        return fromAccounting || (active && hasClass && !str(i.get("studentId")).isEmpty());
    }
    private Map<String,Object> findLinkedStudent(Map<String,Object> i) {
        String sid=first(str(i.get("studentId")),str(i.get("studentUid")),str(i.get("student_id")));
        for(Map<String,Object>s:studentRows)if(!sid.isEmpty()&&sid.equals(str(s.get("id"))))return s;
        String name=norm(first(str(i.get("nomPrenom")),str(i.get("name"))));
        for(Map<String,Object>s:studentRows)if(!name.isEmpty()&&name.equals(norm(displayName(s))))return s;
        return null;
    }
    private void renderEnrollments() {
        cards.removeAllViews();records.removeAllViews();
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.VERTICAL);
        EditText search=input("Rechercher nom, parent, téléphone, code...",enrollmentQuery,false);
        search.addTextChangedListener(new SimpleWatcher(()->{enrollmentQuery=search.getText().toString();renderEnrollmentCards();}));
        header.addView(search);
        Spinner filter=spinner(Arrays.asList("Tous","En attente","Autorisé"),enrollmentStatusFilter);
        filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){enrollmentStatusFilter=String.valueOf(filter.getSelectedItem());renderEnrollmentCards();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        formLabel(header,"Statut");header.addView(filter);
        List<String> years=new ArrayList<>();years.add("Toutes les années");
        Set<String> knownYears=new HashSet<>();for(Map<String,Object> i:enrollmentRows){String y=first(str(i.get("anneeAcademique")),str(i.get("academicYear")));if(!y.isEmpty())knownYears.add(y);}
        List<String> sortedYears=new ArrayList<>(knownYears);Collections.sort(sortedYears,Collections.reverseOrder());years.addAll(sortedYears);
        if(enrollmentYearFilter.isEmpty())enrollmentYearFilter=years.contains(activeYear())?activeYear():"Toutes les années";
        Spinner yearFilter=spinner(years,enrollmentYearFilter);yearFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){enrollmentYearFilter=String.valueOf(yearFilter.getSelectedItem());renderEnrollmentCards();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        formLabel(header,"Année académique");header.addView(yearFilter);
        Spinner originFilter=spinner(Arrays.asList("Toutes les origines","Index.html","Comptable","Secrétariat"),enrollmentOriginFilter);
        originFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){enrollmentOriginFilter=String.valueOf(originFilter.getSelectedItem());renderEnrollmentCards();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        formLabel(header,"Origine de l'inscription");header.addView(originFilter);
        LinearLayout actions=new LinearLayout(this);
        MaterialButton newIns=actionButton("Nouvelle inscription");newIns.setOnClickListener(v->render("inscription"));actions.addView(newIns);
        MaterialButton csv=actionButton("Excel");csv.setOnClickListener(v->beginExport("enrolled",false));actions.addView(csv);
        MaterialButton pdf=actionButton("PDF");pdf.setOnClickListener(v->beginExport("enrolled",true));actions.addView(pdf);
        MaterialButton refresh=actionButton("Actualiser");refresh.setOnClickListener(v->loadEnrollments());actions.addView(refresh);
        header.addView(actions);cards.addView(header);
        status.setText(uniqueEnrollmentCount()+" élève(s) inscrit(s) dédoublonné(s) • "+enrollmentRows.size()+" dossier(s) d'inscription dans "+branch);renderEnrollmentCards();
    }
    private int uniqueEnrollmentCount() {
        Set<String> unique=new HashSet<>();
        for(Map<String,Object> i:enrollmentRows){
            if(!isEnrollmentApproved(i))continue;
            String sid=first(str(i.get("studentId")),str(i.get("studentUid")),str(i.get("student_id")));
            String key=!sid.isEmpty()?sid:norm(first(str(i.get("nomPrenom")),str(i.get("name"))))+"|"
                    +norm(first(str(i.get("dateNaissance")),str(i.get("dob"))))+"|"+norm(rowBranch(i))+"|"
                    +norm(first(str(i.get("anneeAcademique")),str(i.get("academicYear"))));
            if(!key.replace("|","").isEmpty())unique.add(key);
        }
        return unique.size();
    }
    private void renderEnrollmentCards() {
        records.removeAllViews();int count=0;
        for(Map<String,Object> ins:enrollmentRows){
            String name=first(str(ins.get("nomPrenom")),str(ins.get("name"))),cls=first(str(ins.get("classe")),str(ins.get("class")),str(ins.get("niveau")));
            String phone=str(ins.get("chefTelephone"));boolean approved=isEnrollmentApproved(ins);
            if("En attente".equals(enrollmentStatusFilter)&&approved)continue;
            if("Autorisé".equals(enrollmentStatusFilter)&&!approved)continue;
            String year=first(str(ins.get("anneeAcademique")),str(ins.get("academicYear")),str(ins.get("year")));
            if(!enrollmentYearFilter.isEmpty()&&!"Toutes les années".equals(enrollmentYearFilter)&&!norm(year).equals(norm(enrollmentYearFilter)))continue;
            String source=norm(first(str(ins.get("source")),str(ins.get("origin")),str(ins.get("createdByRole"))));
            if("Index.html".equals(enrollmentOriginFilter)&&!source.contains("index"))continue;
            if("Comptable".equals(enrollmentOriginFilter)&&!source.contains("comptable")&&!source.contains("account"))continue;
            if("Secrétariat".equals(enrollmentOriginFilter)&&!source.contains("secretary")&&!source.contains("secretaire"))continue;
            String searchable=norm(name+" "+str(ins.get("chefNomPrenom"))+" "+phone+" "+str(ins.get("codeUnique"))+" "+cls);
            if(!enrollmentQuery.isEmpty()&&!searchable.contains(norm(enrollmentQuery)))continue;
            count++;MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(12));card.setCardElevation(dp(1));
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(14),dp(10),dp(14),dp(10));
            body.addView(label(name,15,NAVY,true));
            body.addView(label("Référence : "+first(str(ins.get("codeUnique")),str(ins.get("reference"))),11,MUTED,false));
            body.addView(label("Classe : "+(cls.isEmpty()?"Non attribuée":cls)+" • Année : "+first(str(ins.get("anneeAcademique")),str(ins.get("academicYear"))),12,NAVY,false));
            body.addView(label("Parent : "+str(ins.get("chefNomPrenom"))+" • "+phone,12,MUTED,false));
            body.addView(label(approved?"Statut : AUTORISÉ • "+first(str(ins.get("authorizedBy")),str(ins.get("confirmedBy")),"Secrétariat"):"Statut : EN ATTENTE DE VALIDATION",12,approved?GREEN:Color.rgb(192,120,20),true));
            LinearLayout actions=new LinearLayout(this);
            MaterialButton details=actionButton("Détails");details.setOnClickListener(v->showEnrollmentDetails(ins));
            MaterialButton assign=actionButton("Classe / Année");assign.setOnClickListener(v->showEnrollmentAssignment(ins));
            actions.addView(details,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(assign,new LinearLayout.LayoutParams(0,dp(42),1));
            if(!approved){MaterialButton authorize=actionButton("Autoriser");authorize.setOnClickListener(v->authorizeEnrollment(ins));actions.addView(authorize,new LinearLayout.LayoutParams(0,dp(42),1));}
            MaterialButton delete=actionButton("Corbeille");delete.setOnClickListener(v->archiveEnrollment(ins));actions.addView(delete,new LinearLayout.LayoutParams(0,dp(42),1));
            body.addView(actions);card.addView(body);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);records.addView(card,p);
        }
        if(count==0)addInfo("Aucune inscription","Aucun dossier ne correspond aux filtres sélectionnés.");
    }
    private void showEnrollmentDetails(Map<String,Object> i) {
        StringBuilder b=new StringBuilder();
        String[] keys={"nomPrenom","dateNaissance","ecoleProvenance","sexe","section","niveau","sdmsCode","branche","transport",
                "chefNomPrenom","chefTelephone","chefPieceIdentite","conjointNomPrenom","conjointTelephone","conjointPieceIdentite",
                "ville","secteur","umurenge","akagari","umudugudu","classe","anneeAcademique","codeUnique","statut","authorizedBy","authorizedDate"};
        for(String key:keys)if(i.containsKey(key))b.append(key).append(" : ").append(str(i.get(key))).append("\\n");
        new AlertDialog.Builder(this).setTitle(first(str(i.get("nomPrenom")),"Détails de l'inscription"))
                .setMessage(b.toString()).setPositiveButton("Fermer",null).show();
    }
    private void authorizeEnrollment(Map<String,Object> i) {
        EditText who=input("Votre nom pour confirmer l'autorisation",userName,false);
        new AlertDialog.Builder(this).setTitle("Autoriser l'inscription").setView(who)
                .setNegativeButton("Annuler",null).setPositiveButton("Confirmer",(dlg,which)->{
                    String signer=who.getText().toString().trim();if(signer.isEmpty()){Toast.makeText(this,"Le nom du responsable est obligatoire.",Toast.LENGTH_SHORT).show();return;}
                    String id=str(i.get("id"));long now=System.currentTimeMillis();Map<String,Object> up=new HashMap<>();
                    up.put("inscriptions-scolaires/"+id+"/authorizedBy",signer);up.put("inscriptions-scolaires/"+id+"/authorizedDate",now);
                    up.put("inscriptions-scolaires/"+id+"/statut","Confirmé");up.put("inscriptions-scolaires/"+id+"/status","approved");up.put("inscriptions-scolaires/"+id+"/authorized",true);
                    Map<String,Object> st=findLinkedStudent(i);if(st!=null){String sid=str(st.get("id"));up.put("students/"+sid+"/status","Actif");up.put("students/"+sid+"/fromInscriptionRequest",id);}
                    databaseRoot.updateChildren(up).addOnCompleteListener(task->{if(task.isSuccessful())loadEnrollments();else Toast.makeText(this,"Autorisation refusée : "+task.getException(),Toast.LENGTH_LONG).show();});
                }).show();
    }
    private void showEnrollmentAssignment(Map<String,Object> i) {
        loadClasses(()->{
            LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),dp(8),dp(18),dp(4));
            List<String> branchChoices=enrollmentBranches();Spinner branchSpinner=spinner(branchChoices,first(str(i.get("branche")),str(i.get("branch")),branch));
            List<String> classes=new ArrayList<>();classes.add("Aucune classe");classes.addAll(classNamesForEnrollmentBranch(String.valueOf(branchSpinner.getSelectedItem())));
            Spinner classSpinner=spinner(classes,first(str(i.get("classe")),str(i.get("class")),"Aucune classe"));
            branchSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long rowId){
                    String selected=String.valueOf(classSpinner.getSelectedItem());
                    List<String> next=new ArrayList<>();next.add("Aucune classe");next.addAll(classNamesForEnrollmentBranch(String.valueOf(branchSpinner.getSelectedItem())));
                    classSpinner.setAdapter(new ArrayAdapter<>(SecretaryPortalActivity.this,android.R.layout.simple_spinner_dropdown_item,next));
                    if(next.contains(selected))classSpinner.setSelection(next.indexOf(selected));else classSpinner.setSelection(0);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
            });
            List<String> years=Arrays.asList(activeYear(),"2025-2026","2027-2028");Spinner yearSpinner=spinner(years,first(str(i.get("anneeAcademique")),str(i.get("academicYear")),activeYear()));
            formLabel(form,"Branche");form.addView(branchSpinner);formLabel(form,"Classe");form.addView(classSpinner);formLabel(form,"Année académique");form.addView(yearSpinner);
            new AlertDialog.Builder(this).setTitle("Affectation scolaire").setView(form).setNegativeButton("Annuler",null)
                    .setPositiveButton("Enregistrer",(dlg,which)->{
                        String b=String.valueOf(branchSpinner.getSelectedItem()),c=String.valueOf(classSpinner.getSelectedItem());
                        if("Aucune classe".equals(c))c="";
                        String y=String.valueOf(yearSpinner.getSelectedItem()),id=str(i.get("id"));
                        Map<String,Object> up=new HashMap<>();up.put("inscriptions-scolaires/"+id+"/branche",b);up.put("inscriptions-scolaires/"+id+"/branch",branchRootOf(b));
                        up.put("inscriptions-scolaires/"+id+"/branchSection",b);
                        up.put("inscriptions-scolaires/"+id+"/classe",c);up.put("inscriptions-scolaires/"+id+"/class",c);
                        up.put("inscriptions-scolaires/"+id+"/anneeAcademique",y);up.put("inscriptions-scolaires/"+id+"/academicYear",y);
                        up.put("inscriptions-scolaires/"+id+"/assignedAt",System.currentTimeMillis());up.put("inscriptions-scolaires/"+id+"/assignedBy",userName);
                        Map<String,Object> st=findLinkedStudent(i);if(st!=null){
                            String sid=str(st.get("id"));String base=branchRootOf(b);
                            up.put("students/"+sid+"/branch",base);up.put("students/"+sid+"/branche",base);
                            up.put("students/"+sid+"/branchName",base);up.put("students/"+sid+"/schoolBranch",base);
                            up.put("students/"+sid+"/branchSection",b);up.put("students/"+sid+"/branchConfig",b);
                            up.put("students/"+sid+"/class",c);up.put("students/"+sid+"/classe",c);
                            up.put("students/"+sid+"/classes",c.isEmpty()?new ArrayList<String>():new ArrayList<>(Collections.singletonList(c)));
                            up.put("students/"+sid+"/className",c);up.put("students/"+sid+"/academicYear",y);
                            String sectionValue=b.contains("primaire")?"PRIMAIRE":"MATERNELLE";up.put("students/"+sid+"/section",sectionValue);
                            up.put("inscriptions-scolaires/"+id+"/section",sectionValue);
                            for(Map<String,Object> classRow:classRows)if(norm(className(classRow)).equals(norm(c)))up.put("students/"+sid+"/classId",str(classRow.get("id")));
                            up.put("students/"+sid+"/updatedAt",System.currentTimeMillis());
                        }
                        databaseRoot.updateChildren(up).addOnCompleteListener(task->{if(task.isSuccessful())loadEnrollments();else Toast.makeText(this,"Affectation refusée : "+task.getException(),Toast.LENGTH_LONG).show();});
                    }).show();
        });
    }
    private void archiveEnrollment(Map<String,Object> i) {
        String id=str(i.get("id"));new AlertDialog.Builder(this).setTitle("Placer l'inscription dans la Corbeille ?")
                .setMessage("Le dossier d'inscription sera conservé dans /trash avant son retrait de la liste.")
                .setNegativeButton("Annuler",null).setPositiveButton("Continuer",(dlg,which)->{
                    Map<String,Object> data=new HashMap<>(i);data.remove("id");Map<String,Object> trash=new HashMap<>();
                    trash.put("source","inscriptions-scolaires");trash.put("originalPath","inscriptions-scolaires/"+id);trash.put("originalData",data);
                    trash.put("label",str(i.get("nomPrenom")));trash.put("reason","Suppression depuis le portail secrétaire");
                    trash.put("deletedAt",System.currentTimeMillis());trash.put("deletedBy",auth.getCurrentUser()==null?"":auth.getCurrentUser().getUid());trash.put("branch",branch);
                    String tid=databaseRoot.child("trash").push().getKey();Map<String,Object> up=new HashMap<>();up.put("trash/"+tid,trash);up.put("inscriptions-scolaires/"+id,null);
                    databaseRoot.updateChildren(up).addOnCompleteListener(task->{if(task.isSuccessful())loadEnrollments();else Toast.makeText(this,"Déplacement vers la corbeille refusé : "+task.getException(),Toast.LENGTH_LONG).show();});
                }).show();
    }

    private Module findModule(String key) {
        for (Module module : modules) if (module.key.equals(key)) return module;
        return null;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private String value(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value == null ? "" : String.valueOf(value);
    }

    private String str(Object value) { return value == null ? "" : String.valueOf(value); }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return "";
    }

    private String norm(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[\\s_-]+", "");
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
