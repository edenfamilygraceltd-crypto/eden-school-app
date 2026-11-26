package com.grace.eden_smart;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.content.ContentResolver;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import android.view.Gravity;
import android.widget.ArrayAdapter;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DirectorPortalActivity extends AppCompatActivity {
    private static final int NAVY = Color.rgb(27, 44, 126);
    private static final int GOLD = Color.rgb(201, 162, 39);
    private static final int BG = Color.rgb(247, 245, 239);
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);

    private FirebaseAuth auth;
    private DatabaseReference usersRef;
    private LinearLayout page;
    private TextView directorName, assignmentText, welcomeText, statusText, cotationStatus, cotationResults;
    private Spinner classSpinner, subjectSpinner, trimesterSpinner, typeSpinner, assessmentSpinner;
    private DatabaseReference database;
    private String assignedLevel = "";
    private String assignedBranch = "";
    private final List<String> assessmentIds = new ArrayList<>();
    private final java.util.Map<String, String> studentNames = new java.util.HashMap<>();
    private TextView migrationStatus;
    private TextView attendanceStatus;
    private LinearLayout attendanceList;
    private MaterialButton migrationButton;
    private String pendingMigrationJson = "";
    private final List<MigrationJob> pendingMigrationJobs = new ArrayList<>();
    private int migrationDone = 0, migrationFailed = 0, migrationPreserved = 0;

    private static final int CREATE_COTATION_BACKUP_REQUEST = 7401;
    private static final class MigrationJob {
        final String sourceKey, targetKey;
        final java.util.Map<String, Object> record;
        MigrationJob(String sourceKey, String targetKey, java.util.Map<String, Object> record) {
            this.sourceKey = sourceKey; this.targetKey = targetKey; this.record = record;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        try {
            FirebaseService service = FirebaseService.getInstance(getApplicationContext());
            auth = service.getAuth();
            usersRef = service.getUsersReference();
            database = service.getDatabase().getReference();
        } catch (Exception e) {
            Toast.makeText(this, "Configuration Firebase indisponible.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (auth.getCurrentUser() == null) {
            goLogin();
            return;
        }
        buildUi();
        verifyDirector();
        AppUpdateChecker.check(this);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("EDEN FAMILY SCHOOL", 17, NAVY, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialButton logout = button("Déconnexion", false);
        logout.setOnClickListener(v -> {
            auth.signOut();
            goLogin();
        });
        top.addView(logout);
        page.addView(top);

        MaterialCardView hero = card(NAVY);
        LinearLayout heroContent = column();
        heroContent.setPadding(dp(20), dp(22), dp(20), dp(22));
        TextView eyebrow = label("ESPACE DE DIRECTION", 12, GOLD, true);
        heroContent.addView(eyebrow);
        directorName = label("Chargement du profil…", 24, Color.WHITE, true);
        directorName.setPadding(0, dp(8), 0, dp(4));
        heroContent.addView(directorName);
        welcomeText = label("Pilotage pédagogique et suivi de votre établissement.", 14, Color.rgb(225, 232, 249), false);
        heroContent.addView(welcomeText);
        assignmentText = label("Affectation : vérification…", 13, Color.WHITE, true);
        assignmentText.setPadding(0, dp(16), 0, 0);
        heroContent.addView(assignmentText);
        hero.addView(heroContent);
        LinearLayout.LayoutParams heroLp = matchWrap();
        heroLp.topMargin = dp(18);
        page.addView(hero, heroLp);

        TextView heading = label("Tableau de bord", 20, INK, true);
        heading.setPadding(0, dp(22), 0, dp(10));
        page.addView(heading);

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);
        cards.addView(infoCard("👩🏽‍🏫", "Enseignants", "Consultation des enseignants affectés à votre périmètre."));
        cards.addView(infoCard("📊", "Cotations", "Consultation des cotations de votre niveau et de votre branche."));
        cards.addView(infoCard("🎓", "Suivi pédagogique", "Vue d’ensemble des activités pédagogiques."));
        page.addView(cards);

        MaterialCardView cotations = card(Color.WHITE);
        LinearLayout cotationBox = column();
        cotationBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        cotationBox.addView(label("Consultation des cotations", 18, NAVY, true));
        cotationBox.addView(label("Lecture seule — données de votre niveau et de votre branche.", 13, MUTED, false));
        classSpinner = addSelector(cotationBox, "Classe");
        subjectSpinner = addSelector(cotationBox, "Matière", "Étude du milieu", "Pre-mathématique", "Français", "Français langage", "Art& Culture", "Activité Physique et Motrice");
        trimesterSpinner = addSelector(cotationBox, "Trimestre", "Trimestre 1", "Trimestre 2", "Trimestre 3");
        typeSpinner = addSelector(cotationBox, "Type", "Travail journalier (TJ)", "Examen (EX)");
        assessmentSpinner = addSelector(cotationBox, "Évaluation");
        MaterialButton refresh = button("Afficher les cotations", true);
        refresh.setOnClickListener(v -> loadDirectorAssessments());
        cotationBox.addView(refresh);
        cotationStatus = label("Chargement des classes…", 13, MUTED, false);
        cotationStatus.setPadding(0, dp(10), 0, dp(6));
        cotationBox.addView(cotationStatus);
        cotationResults = label("", 14, INK, false);
        cotationResults.setPadding(0, dp(6), 0, 0);
        cotationBox.addView(cotationResults);
        cotations.addView(cotationBox);
        LinearLayout.LayoutParams cotLp = matchWrap(); cotLp.topMargin = dp(14); page.addView(cotations, cotLp);

        MaterialCardView attendance = card(Color.WHITE);
        LinearLayout attendanceBox = column();
        attendanceBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        attendanceBox.addView(label("Présence du personnel • Observations de la Direction", 17, NAVY, true));
        attendanceBox.addView(label("Les heures d’arrivée et de départ viennent du pointage partagé. La Direction peut ajouter une observation pour confirmer ou expliquer une présence.", 13, MUTED, false));
        MaterialButton attendanceRefresh = button("Actualiser les présences du jour", true);
        attendanceRefresh.setOnClickListener(v -> loadDirectorAttendance());
        attendanceBox.addView(attendanceRefresh);
        attendanceStatus = label("Chargement des présences…", 13, MUTED, false);
        attendanceStatus.setPadding(0, dp(8), 0, dp(6)); attendanceBox.addView(attendanceStatus);
        attendanceList = column(); attendanceBox.addView(attendanceList);
        attendance.addView(attendanceBox);
        LinearLayout.LayoutParams attendanceLp = matchWrap(); attendanceLp.topMargin = dp(14); page.addView(attendance, attendanceLp);

        MaterialCardView migration = card(Color.WHITE);
        LinearLayout migrationBox = column();
        migrationBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        migrationBox.addView(label("Migration temporaire des cotations", 17, NAVY, true));
        migrationBox.addView(label("Sauvegarde JSON obligatoire, puis fusion des anciennes fiches de votre branche. Les notes déjà présentes sont conservées et les sources ne sont jamais supprimées.", 13, MUTED, false));
        migrationButton = button("Sauvegarder et migrer les cotations", true);
        migrationButton.setOnClickListener(v -> prepareCotationMigration());
        migrationButton.setEnabled(false);
        LinearLayout.LayoutParams migrationBtnLp = matchWrap(); migrationBtnLp.topMargin = dp(10);
        migrationBox.addView(migrationButton, migrationBtnLp);
        migrationStatus = label("Aucune migration lancée.", 12, MUTED, false);
        migrationStatus.setPadding(0, dp(8), 0, 0);
        migrationBox.addView(migrationStatus);
        migration.addView(migrationBox);
        LinearLayout.LayoutParams migrationLp = matchWrap(); migrationLp.topMargin = dp(14); page.addView(migration, migrationLp);

        MaterialCardView scope = card(Color.WHITE);
        LinearLayout scopeContent = column();
        scopeContent.setPadding(dp(16), dp(16), dp(16), dp(16));
        scopeContent.addView(label("Votre périmètre de gestion", 17, NAVY, true));
        TextView scopeNote = label("Les informations affichées dans ce portail seront limitées au niveau scolaire et à la branche associés à votre compte.", 14, MUTED, false);
        scopeNote.setPadding(0, dp(8), 0, 0);
        scopeContent.addView(scopeNote);
        scope.addView(scopeContent);
        LinearLayout.LayoutParams scopeLp = matchWrap();
        scopeLp.topMargin = dp(12);
        page.addView(scope, scopeLp);

        statusText = label("Chargement des informations…", 13, MUTED, false);
        statusText.setPadding(0, dp(14), 0, dp(4));
        page.addView(statusText);

        setContentView(root);
        KeyboardInsets.apply(this);
    }


    private void prepareCotationMigration() {
        if (database == null || auth == null || auth.getCurrentUser() == null || assignedBranch.isEmpty()) {
            migrationStatus.setText("Profil Directeur ou branche non vérifié. Migration indisponible.");
            return;
        }
        migrationButton.setEnabled(false);
        migrationStatus.setText("Analyse des anciennes cotations de " + displayBranch(assignedBranch) + "…");
        pendingMigrationJobs.clear();
        database.child("cotations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot root) {
                if (!root.exists()) {
                    migrationStatus.setText("Aucune donnée trouvée dans cotations/.");
                    migrationButton.setEnabled(true);
                    return;
                }
                Map<String, Object> branchBackup = new java.util.LinkedHashMap<>();
                int skipped = 0;
                List<String> skippedKeys = new ArrayList<>();
                Set<String> seenSources = new HashSet<>();
                for (DataSnapshot child : root.getChildren()) {
                    Object raw = child.getValue();
                    if (!(raw instanceof Map)) { skipped++; continue; }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = (Map<String, Object>) raw;
                    String branch = stringValue(record.get("branch"));
                    if (branch.isEmpty() || !normalize(branch).equals(normalize(assignedBranch))) continue;
                    branchBackup.put(child.getKey(), record);

                    Object entriesRaw = record.get("entries");
                    if (!(entriesRaw instanceof Map) || ((Map<?, ?>) entriesRaw).isEmpty()) {
                        skipped++; addSkippedKey(skippedKeys, child.getKey()); continue;
                    }
                    String cls = first(stringValue(record.get("class")), stringValue(record.get("classe")));
                    String subject = first(stringValue(record.get("subject")), stringValue(record.get("matiere")));
                    if (cls.isEmpty() || subject.isEmpty()) {
                        skipped++; addSkippedKey(skippedKeys, child.getKey()); continue;
                    }

                    String sourceKey = child.getKey() == null ? "" : child.getKey();
                    java.util.regex.Matcher canonical = java.util.regex.Pattern
                            .compile("_(20\\d{2}_20\\d{2})_T([1-3])_(TJ|EX)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(sourceKey);
                    java.util.regex.Matcher monthly = java.util.regex.Pattern
                            .compile("_(20\\d{2}-\\d{2})_(TJ|EX)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(sourceKey);
                    String year = first(stringValue(record.get("academicYear")), stringValue(record.get("schoolYear")));
                    String tri = parseTrimester(stringValue(record.get("trimester")));
                    String type = stringValue(record.get("type")).toUpperCase(Locale.ROOT);
                    if (canonical.find()) {
                        if (year.isEmpty()) year = canonical.group(1).replace('_', '-');
                        if (tri.isEmpty()) tri = canonical.group(2);
                        if (type.isEmpty()) type = canonical.group(3).toUpperCase(Locale.ROOT);
                    } else if (monthly.find()) {
                        String month = first(stringValue(record.get("month")), monthly.group(1));
                        if (year.isEmpty()) year = academicYearFromMonth(month);
                        if (tri.isEmpty()) {
                            skipped++; addSkippedKey(skippedKeys, sourceKey); continue;
                        }
                        if (type.isEmpty()) type = monthly.group(2).toUpperCase(Locale.ROOT);
                    } else {
                        skipped++; addSkippedKey(skippedKeys, sourceKey); continue;
                    }
                    if (year.isEmpty() || tri.isEmpty() || (!"TJ".equals(type) && !"EX".equals(type))) {
                        skipped++; addSkippedKey(skippedKeys, sourceKey); continue;
                    }
                    String targetBaseKey = cotationKey(branch) + "_" + cotationKey(cls) + "_"
                            + cotationKey(subject) + "_" + cotationKey(year) + "_T" + tri + "_" + type;
                    if (!seenSources.add(sourceKey)) continue;
                    String month = stringValue(record.get("month"));
                    String assessmentId = "EX".equals(type) ? "exam"
                            : (!month.isEmpty() ? "legacy_" + cotationKey(month)
                            : first(stringValue(record.get("assessmentId")), "test_1"));
                    Map<String, Object> assessmentRecord = new java.util.LinkedHashMap<>(record);
                    if (stringValue(assessmentRecord.get("label")).isEmpty()) {
                        assessmentRecord.put("label", month.isEmpty() ? "Ancienne fiche migrée" : "Ancienne fiche - " + month);
                    }
                    pendingMigrationJobs.add(new MigrationJob(sourceKey, targetBaseKey + "/" + assessmentId, assessmentRecord));
                }

                Map<String, Object> backup = new java.util.LinkedHashMap<>();
                backup.put("exportedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date()));
                backup.put("project", "edensmart-app");
                backup.put("root", "cotations");
                backup.put("branch", assignedBranch);
                backup.put("data", branchBackup);
                try {
                    pendingMigrationJson = new JSONObject(backup).toString(2);
                } catch (Exception e) {
                    migrationStatus.setText("Impossible de préparer la sauvegarde JSON : " + e.getMessage());
                    migrationButton.setEnabled(true);
                    return;
                }
                migrationStatus.setText("Analyse terminée : " + branchBackup.size() + " fiche(s) sauvegardée(s), "
                        + pendingMigrationJobs.size() + " à fusionner, " + skipped + " ignorée(s). Choisissez où enregistrer la sauvegarde.");
                Intent create = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                create.addCategory(Intent.CATEGORY_OPENABLE);
                create.setType("application/json");
                create.putExtra(Intent.EXTRA_TITLE, "eden-cotations-" + cotationKey(assignedBranch) + "-"
                        + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".json");
                startActivityForResult(create, CREATE_COTATION_BACKUP_REQUEST);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                migrationStatus.setText("Lecture des cotations refusée : " + error.getMessage());
                migrationButton.setEnabled(true);
            }
        });
    }

    private void addSkippedKey(List<String> keys, String key) {
        if (key != null && keys.size() < 8) keys.add(key);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String parseTrimester(String raw) {
        if (raw == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([1-3])").matcher(raw);
        return m.find() ? m.group(1) : "";
    }

    private String academicYearFromMonth(String month) {
        if (month == null || !month.matches("20\\d{2}-\\d{2}")) return "";
        int year = Integer.parseInt(month.substring(0, 4));
        int monthNumber = Integer.parseInt(month.substring(5, 7));
        int start = monthNumber >= 9 ? year : year - 1;
        return start + "-" + (start + 1);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_COTATION_BACKUP_REQUEST) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            migrationStatus.setText("Sauvegarde annulée. Aucune donnée Firebase n'a été modifiée.");
            migrationButton.setEnabled(true);
            return;
        }
        Uri uri = data.getData();
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new IllegalStateException("Fichier de sauvegarde inaccessible.");
            output.write(pendingMigrationJson.getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (Exception e) {
            migrationStatus.setText("Échec de la sauvegarde. Migration annulée : " + e.getMessage());
            migrationButton.setEnabled(true);
            return;
        }
        if (pendingMigrationJobs.isEmpty()) {
            migrationStatus.setText("Sauvegarde enregistrée. Aucune fiche avec métadonnées suffisantes à migrer. Les sources restent intactes.");
            migrationButton.setEnabled(true);
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmer la migration")
                .setMessage("La sauvegarde JSON a été enregistrée. " + pendingMigrationJobs.size()
                        + " fiche(s) de " + displayBranch(assignedBranch)
                        + " vont être fusionnées. Les données de destination déjà présentes seront conservées. "
                        + "Aucune source ne sera supprimée. Continuer ?")
                .setNegativeButton("Annuler", (d, w) -> {
                    migrationStatus.setText("Sauvegarde enregistrée. Migration annulée ; aucune donnée Firebase modifiée.");
                    migrationButton.setEnabled(true);
                })
                .setPositiveButton("Migrer sans écraser", (d, w) -> {
                    migrationDone = 0; migrationFailed = 0; migrationPreserved = 0;
                    migrationStatus.setText("Migration en cours…");
                    runNextCotationMigration(0);
                }).show();
    }

    private void runNextCotationMigration(int index) {
        if (index >= pendingMigrationJobs.size()) {
            migrationStatus.setText("Migration terminée : " + migrationDone + " fiche(s) fusionnée(s), "
                    + migrationPreserved + " destination(s) déjà existante(s) préservée(s), "
                    + migrationFailed + " échec(s). Les anciennes sources sont conservées.");
            migrationButton.setEnabled(true);
            return;
        }
        MigrationJob job = pendingMigrationJobs.get(index);
        DatabaseReference target = database.child("cotations").child(job.targetKey);
        final boolean[] hadExisting = {false};
        target.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Object existing = currentData.getValue();
                hadExisting[0] = existing != null;
                currentData.setValue(mergeMissingMigrationData(existing, job.record));
                return Transaction.success(currentData);
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null || !committed) migrationFailed++;
                else {
                    migrationDone++;
                    if (hadExisting[0]) migrationPreserved++;
                }
                migrationStatus.setText("Migration : " + (index + 1) + "/" + pendingMigrationJobs.size()
                        + " • fusionnées " + migrationDone + " • échecs " + migrationFailed);
                runNextCotationMigration(index + 1);
            }
        });
    }

    private Object mergeMissingMigrationData(Object existing, Object incoming) {
        if (existing == null) return incoming;
        if (!(existing instanceof Map) || !(incoming instanceof Map)) return existing;
        Map<String, Object> merged = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) existing).entrySet()) {
            merged.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) incoming).entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object oldValue = merged.get(key);
            Object newValue = entry.getValue();
            if (oldValue == null) merged.put(key, newValue);
            else merged.put(key, mergeMissingMigrationData(oldValue, newValue));
        }
        return merged;
    }


    private void loadDirectorAttendance() {
        if (database == null || assignedBranch.isEmpty() || attendanceList == null) return;
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        attendanceStatus.setText("Chargement des pointages du " + dateKey + "…");
        attendanceList.removeAllViews();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot userRoot) {
                database.child("teacher_attendance").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot attendanceRoot) {
                        attendanceList.removeAllViews();
                        int count = 0;
                        for (DataSnapshot user : userRoot.getChildren()) {
                            String role = normalize(first(textValue(user, "role"), textValue(user, "userRole"), textValue(user, "type")));
                            if (!role.equals("teacher") && !role.equals("enseignant") && !role.equals("enseignante")) continue;
                            String branch = first(textValue(user, "branchConfig"), textValue(user, "branch"), textValue(user, "branche"),
                                    textValue(user, "branchName"), textValue(user, "schoolBranch"), textValue(user, "school_branch"));
                            if (!sameAttendanceBranch(branch, assignedBranch)) continue;
                            String uid = user.getKey();
                            DataSnapshot day = attendanceRoot.child(uid).child(dateKey);
                            String name = first(textValue(user, "name"), textValue(user, "nom"), textValue(user, "displayName"),
                                    textValue(user, "fullName"), textValue(user, "email"), uid);
                            String arrival = textValue(day.child("arrival"), "time");
                            String departure = textValue(day.child("departure"), "time");
                            String observation = textValue(day, "observation");
                            addDirectorAttendanceCard(uid, name, branch, dateKey, arrival, departure, observation);
                            count++;
                        }
                        attendanceStatus.setText(count + " enseignant(s) de " + displayBranch(assignedBranch)
                                + " • " + dateKey + " • observations enregistrées dans teacher_attendance");
                        if (count == 0) attendanceList.addView(label("Aucun enseignant correspondant à votre branche.", 13, MUTED, false));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        attendanceStatus.setText("Lecture des pointages impossible : " + error.getMessage());
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                attendanceStatus.setText("Lecture des enseignants impossible : " + error.getMessage());
            }
        });
    }

    private boolean sameAttendanceBranch(String a, String b) {
        String x = normalize(a), y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        return x.equals(y) || x.startsWith(y) || y.startsWith(x);
    }

    private String branchRootOf(String value) {
        if (value == null) return "";
        int i = value.indexOf('_');
        return i > 0 ? value.substring(0, i) : value;
    }

    private void addDirectorAttendanceCard(String uid, String name, String branch, String date,
                                           String arrival, String departure, String observation) {
        MaterialCardView c = card(Color.WHITE);
        LinearLayout body = column();
        body.setPadding(dp(12), dp(12), dp(12), dp(12));
        body.addView(label(name, 15, NAVY, true));
        body.addView(label("Branche : " + branch + "   •   Date : " + date, 11, MUTED, false));
        body.addView(label("Arrivée : " + (arrival == null ? "—" : arrival)
                + "   •   Départ : " + (departure == null ? "—" : departure), 13, INK, false));
        TextView obs = label("Observation : " + (observation == null || observation.trim().isEmpty()
                ? "Aucune observation" : observation), 13, observation == null || observation.trim().isEmpty() ? MUTED : Color.rgb(156, 78, 24), false);
        obs.setPadding(0, dp(5), 0, dp(6));
        body.addView(obs);
        MaterialButton edit = button(observation == null || observation.trim().isEmpty()
                ? "Ajouter une observation" : "Modifier l’observation", true);
        edit.setOnClickListener(v -> editDirectorObservation(uid, name, date, observation));
        body.addView(edit);
        c.addView(body);
        LinearLayout.LayoutParams lp = matchWrap(); lp.bottomMargin = dp(8); attendanceList.addView(c, lp);
    }

    private void editDirectorObservation(String teacherUid, String teacherName, String date, String current) {
        EditText input = new EditText(this);
        input.setHint("Ex. : Présence confirmée à 07:18 ; retard justifié…");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);
        input.setText(current == null ? "" : current);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Observation de la Direction")
                .setMessage("Cette observation sera visible dans Attendance du portail web et dans le pointage du secrétariat.")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("observation", value.isEmpty() ? null : value);
                    updates.put("observationByUid", auth.getCurrentUser().getUid());
                    updates.put("observationByName", directorName.getText().toString());
                    updates.put("observationAt", System.currentTimeMillis());
                    updates.put("observationRole", "director");
                    database.child("teacher_attendance").child(teacherUid).child(date).updateChildren(updates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Observation enregistrée pour " + teacherName, Toast.LENGTH_SHORT).show();
                                    loadDirectorAttendance();
                                } else {
                                    Toast.makeText(this, "Enregistrement impossible : " +
                                            (task.getException() == null ? "erreur Firebase" : task.getException().getMessage()),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }).show();
    }

    private void verifyDirector() {
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String role = first(snap.child("role").getValue(String.class),
                        snap.child("userRole").getValue(String.class));
                String normalizedRole = normalize(role);
                if (!"directeur".equals(normalizedRole) && !"director".equals(normalizedRole)) {
                    reject("Accès réservé aux directeurs.");
                    return;
                }
                assignedLevel = first(snap.child("niveau").getValue(String.class),
                        snap.child("educationLevel").getValue(String.class),
                        snap.child("level").getValue(String.class));
                assignedBranch = first(snap.child("branche").getValue(String.class),
                        snap.child("branch").getValue(String.class),
                        snap.child("branchConfig").getValue(String.class),
                        snap.child("branchName").getValue(String.class),
                        snap.child("schoolBranch").getValue(String.class),
                        snap.child("school_branch").getValue(String.class));
                if (assignedLevel == null || assignedBranch == null) {
                    reject("Votre compte Directeur n'a pas encore de niveau ou de branche attribué. Contactez l'IT Admin.");
                    return;
                }
                String name = first(snap.child("nom").getValue(String.class),
                        snap.child("name").getValue(String.class));
                directorName.setText(name == null ? "Directeur" : name);
                assignmentText.setText("Niveau : " + assignedLevel + "   •   Branche : " + displayBranch(assignedBranch));
                if (migrationButton != null) migrationButton.setEnabled(true);
                statusText.setText("Portail prêt. Les données seront filtrées selon votre affectation.");
                loadDirectorClasses();
                loadDirectorAttendance();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                reject("Impossible de vérifier votre profil : " + error.getMessage());
            }
        });
    }


    private Spinner addSelector(LinearLayout parent, String hint, String... values) {
        Spinner spinner = new Spinner(this);
        List<String> items = new ArrayList<>();
        if (values.length == 0) items.add("Chargement…");
        else java.util.Collections.addAll(items, values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        parent.addView(spinner, matchWrap());
        return spinner;
    }

    private void setSpinnerItems(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private String cotationKey(String value) {
        String s = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }

    private String academicYear() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH) + 1;
        int start = month >= 9 ? year : year - 1;
        return start + "-" + (start + 1);
    }

    private boolean classMatchesLevel(String cls) {
        String c = normalize(cls);
        String level = normalize(assignedLevel);
        if (level.contains("maternelle") || level.contains("nursery")) {
            return c.matches("^(n[123].*|ps.*|ms.*|gs.*|petitesection.*|moyennesection.*|grandesection.*)$");
        }
        if (level.contains("primaire") || level.contains("primary")) {
            return c.matches("^p[1-6].*$");
        }
        return false;
    }

    private void loadDirectorClasses() {
        cotationStatus.setText("Recherche des classes de votre branche…");
        database.child("students").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot root) {
                Set<String> classes = new HashSet<>();
                studentNames.clear();
                for (DataSnapshot student : root.getChildren()) {
                    String branch = first(textValue(student, "branch"), textValue(student, "branche"),
                            textValue(student, "schoolBranch"), textValue(student, "branchName"),
                            textValue(student, "location"), textValue(student, "campus"));
                    if (!normalize(branch).equals(normalize(assignedBranch))) continue;
                    String cls = first(textValue(student, "class"), textValue(student, "classes"),
                            textValue(student, "classe"), textValue(student, "className"),
                            textValue(student, "classroom"), textValue(student, "niveau"));
                    if (cls != null && classMatchesLevel(cls)) {
                        classes.add(cls);
                        String name = first(textValue(student, "name"), textValue(student, "fullName"), textValue(student, "studentName"));
                        if (name == null) name = first(textValue(student, "firstName"), "") + " " + first(textValue(student, "lastName"), "");
                        studentNames.put(student.getKey(), name.trim());
                    }
                }
                List<String> sorted = new ArrayList<>(classes);
                java.util.Collections.sort(sorted);
                setSpinnerItems(classSpinner, sorted.isEmpty()
                        ? java.util.Collections.singletonList("Aucune classe trouvée") : sorted);
                if (sorted.isEmpty()) cotationStatus.setText("Aucune classe trouvée pour ce niveau et cette branche.");
                else {
                    cotationStatus.setText(sorted.size() + " classe(s) trouvée(s). Choisissez les critères puis affichez.");
                    loadDirectorAssessments();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                cotationStatus.setText("Lecture des classes impossible : " + error.getMessage());
            }
        });
    }

    private String textValue(DataSnapshot snap, String key) {
        Object value = snap.child(key).getValue();
        return value == null ? null : String.valueOf(value);
    }

    private String cotationPath(String type) {
        String branchKey = cotationKey(assignedBranch);
        String cls = String.valueOf(classSpinner.getSelectedItem());
        String subject = String.valueOf(subjectSpinner.getSelectedItem());
        int tri = trimesterSpinner.getSelectedItemPosition() + 1;
        return "cotations/" + branchKey + "_" + cotationKey(cls) + "_" + cotationKey(subject)
                + "_" + cotationKey(academicYear()) + "_T" + tri + "_" + type;
    }

    private void loadDirectorAssessments() {
        if (classSpinner == null || classSpinner.getSelectedItem() == null
                || subjectSpinner.getSelectedItem() == null) return;
        boolean exam = typeSpinner.getSelectedItemPosition() == 1;
        String base = cotationPath(exam ? "EX" : "TJ");
        cotationStatus.setText("Chargement des évaluations…");
        database.child(base).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                assessmentIds.clear();
                List<String> labels = new ArrayList<>();
                if (exam) {
                    assessmentIds.add("exam");
                    labels.add("Examen du trimestre");
                } else {
                    for (DataSnapshot child : snap.getChildren()) {
                        assessmentIds.add(child.getKey());
                        String label = textValue(child, "label");
                        labels.add(label == null ? "Test " + labels.size() : label);
                    }
                }
                if (assessmentIds.isEmpty()) {
                    assessmentIds.add("test_1");
                    labels.add("Test 1");
                }
                setSpinnerItems(assessmentSpinner, labels);
                cotationStatus.setText(labels.size() + " évaluation(s) disponible(s).");
                renderDirectorCotation();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                cotationStatus.setText("Lecture des évaluations refusée : " + error.getMessage());
            }
        });
    }

    private void renderDirectorCotation() {
        if (assessmentSpinner.getSelectedItemPosition() < 0 || assessmentIds.isEmpty()) return;
        String type = typeSpinner.getSelectedItemPosition() == 1 ? "EX" : "TJ";
        String assessment = type.equals("EX") ? "exam" : assessmentIds.get(assessmentSpinner.getSelectedItemPosition());
        String path = cotationPath(type) + "/" + assessment;
        String cls = String.valueOf(classSpinner.getSelectedItem());
        cotationStatus.setText("Lecture des notes…");
        database.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                StringBuilder result = new StringBuilder();
                int count = 0;
                DataSnapshot entries = snap.child("entries");
                for (DataSnapshot student : entries.getChildren()) {
                    DataSnapshot record = student;
                    StringBuilder scores = new StringBuilder();
                    for (DataSnapshot component : record.getChildren()) {
                        String componentName = component.getKey();
                        if ("savedAt".equals(componentName) || "savedBy".equals(componentName)) continue;
                        Object score = component.child("score").getValue();
                        if (score != null) {
                            if (scores.length() > 0) scores.append(" | ");
                            scores.append(componentName).append(" : ").append(score);
                        }
                    }
                    if (scores.length() == 0) continue;
                    String studentName = studentNames.get(student.getKey());
                    if (studentName == null || studentName.trim().isEmpty()) studentName = "Élève " + student.getKey();
                    result.append("• ").append(studentName).append("\n  ").append(scores).append("\n\n");
                    count++;
                }
                cotationResults.setText(count == 0 ? "Aucune note enregistrée pour cette sélection."
                        : result.toString().trim());
                cotationStatus.setText(count + " élève(s) avec des notes • " + cls + " • "
                        + subjectSpinner.getSelectedItem() + " • Trimestre "
                        + (trimesterSpinner.getSelectedItemPosition() + 1));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                cotationStatus.setText("Lecture des notes impossible : " + error.getMessage());
            }
        });
    }

    private void reject(String message) {
        if (auth != null) auth.signOut();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        goLogin();
    }

    private MaterialCardView infoCard(String icon, String title, String description) {
        MaterialCardView c = card(Color.WHITE);
        LinearLayout inner = column();
        inner.setPadding(dp(16), dp(15), dp(16), dp(15));
        TextView titleView = label(icon + "   " + title, 17, NAVY, true);
        inner.addView(titleView);
        TextView detail = label(description, 13, MUTED, false);
        detail.setPadding(0, dp(7), 0, 0);
        inner.addView(detail);
        c.addView(inner);
        LinearLayout.LayoutParams lp = matchWrap();
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    private MaterialCardView card(int color) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(color);
        c.setRadius(dp(20));
        c.setCardElevation(dp(3));
        c.setStrokeWidth(dp(1));
        c.setStrokeColor(Color.rgb(229, 231, 235));
        return c;
    }
    private MaterialButton button(String text, boolean primary) {
        MaterialButton b = new MaterialButton(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setCornerRadius(dp(12));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? NAVY : Color.WHITE));
        b.setTextColor(primary ? Color.WHITE : NAVY);
        return b;
    }
    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private String first(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return null;
    }
    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[\\s_-]+", "");
    }
    private String displayBranch(String branch) {
        String key = normalize(branch);
        if (key.equals("kacyiru")) return "Kacyiru";
        if (key.equals("gisozi")) return "Gisozi";
        if (key.equals("kimisagara")) return "Kimisagara";
        return branch;
    }
    private void goLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
