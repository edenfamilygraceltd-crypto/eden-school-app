package com.grace.eden_smart;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ViewFlipper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.grace.eden_smart.service.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Native teacher space. Access is role-checked and student lists are filtered
 * by assigned class and branch. Attendance is saved separately by date.
 */
public class TeacherPortalActivity extends AppCompatActivity {
    private static final int NAVY = Color.rgb(16, 36, 77);
    private static final int BLUE = Color.rgb(28, 60, 116);
    private static final int GOLD = Color.rgb(201, 162, 39);
    private static final int BG = Color.rgb(247, 245, 239);
    private Typeface interTypeface, frauncesTypeface;
    private final Handler heroHandler = new Handler(Looper.getMainLooper());
    private Runnable heroCarouselTask;
    private int heroSlideIndex = 0;
    private int TEXT;
    private int MUTED;

    private FirebaseAuth auth;
    private DatabaseReference usersRef, teachersRef, studentsRef, activitiesRef;
    private LinearLayout studentList, attendanceList, activityList;
    private TextView activityStatus, performanceSummary;
    private LinearLayout performanceSubjects, performanceRanking;
    private LinearLayout teacherMessageList;
    private TextView messageStatus, notificationStatus;
    private LinearLayout teacherNotificationList;
    private LinearLayout teacherGalleryList;
    private TextView galleryStatus;
    private TextView lessonPreparationStatus;
    private LinearLayout lessonPreparationList;
    private final java.util.List<Map<String, Object>> lessonPreparationRecords = new java.util.ArrayList<>();
    private EditText profileFirstName, profileLastName, profilePhone;
    private TextView profileEmail, profileBranch, profileClasses, profileSettingsStatus;
    private final java.util.concurrent.ExecutorService galleryExecutor = java.util.concurrent.Executors.newFixedThreadPool(3);
    private final java.util.List<Map<String, String>> messageRecipients = new java.util.ArrayList<>();
    private Spinner performanceTrimester;
    private Spinner cotationClass, cotationSubject, cotationType, cotationTrimester, cotationAssessment;
    private EditText cotationMonth;
    private String cotationAssessmentId = "test_1";
    private final java.util.List<String> cotationAssessmentIds = new java.util.ArrayList<>();
    private final java.util.List<String> cotationAssessmentLabels = new java.util.ArrayList<>();
    private TextView cotationStatus, cotationSavedStatus;
    private LinearLayout cotationEntries, cotationSavedEntries;
    private final Map<String, EditText> cotationInputs = new java.util.LinkedHashMap<>();
    private final Map<String, String> cotationStudentNames = new java.util.LinkedHashMap<>();
    private final Map<String, Double> cotationMaxPoints = new HashMap<>();
    private final java.util.List<String> cotationSavedRows = new java.util.ArrayList<>();
    private Spinner bulletinStudent, bulletinTrimester;
    private EditText bulletinYear, bulletinTeacher, bulletinComment;
    private TextView bulletinStatus, bulletinHistoryStatus;
    private LinearLayout bulletinGrades, bulletinConduct, bulletinHistoryList;
    private final Map<String, EditText> bulletinScoreInputs = new java.util.LinkedHashMap<>();
    private final Map<String, Spinner> bulletinConductInputs = new java.util.LinkedHashMap<>();
    private final String[] bulletinSubjects = {"Pré-mathématique", "Français", "Étude du milieu",
            "Français langage", "Art& Culture", "Activité Physique et Motrice"};
    private final String[] conductLabels = {"Respect des enseignants et des camarades",
            "Politesse et bonnes manières", "Obéissance aux consignes", "Discipline en classe",
            "Ponctualité et assiduité", "Propreté et hygiène personnelle",
            "Participation aux activités", "Esprit de partage et de collaboration"};
    private final String[][] cotationSubjects = {
            {"Étude du milieu", "Observation", "D.E. du Milieu"},
            {"Pre-mathématique", "Pré-calcul", "Formes géométriques"},
            {"Français", "Pré-lecture", "Pré-écriture"},
            {"Français langage", "Expression orale", "Histoire"},
            {"Art& Culture", "Dessin", "Coloriage", "Peinture", "Découpage", "Déchiquetage", "Modelage", "Pliage", "Récitation", "Bricolage et construction", "Chant"},
            {"Activité Physique et Motrice", "Exercices physiques", "Jeux collectifs"}
    };
    private final java.util.List<Map<String, Object>> activityRecords = new java.util.ArrayList<>();
    private EditText studentSearch;
    private TextView teacherName, branchText, classText, studentCount, status, attendanceDate, attendanceStats;
    private String uid, teacherBranch = "", selectedAttendanceDate, cotationSchoolYear = "";
    private final Set<String> assignedClasses = new HashSet<>();
    private final Map<String, RadioGroup> attendanceGroups = new HashMap<>();
    private final Map<String, String> savedAttendance = new HashMap<>();
    private final Map<String, String> accessibleStudents = new java.util.LinkedHashMap<>();
    private final Map<String, Map<String, Object>> studentRecords = new HashMap<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppUpdateChecker.check(this);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        TEXT = getColor(R.color.text_primary);
        MUTED = getColor(R.color.text_primary);
        buildScreen();
        try {
            FirebaseService service = FirebaseService.getInstance(getApplicationContext());
            auth = service.getAuth();
            usersRef = service.getUsersReference();
            teachersRef = service.getDatabase().getReference("teachers");
            studentsRef = service.getDatabase().getReference("students");
            activitiesRef = service.getDatabase().getReference("classes_activities");
        } catch (Exception e) {
            showFatal("Impossible d'initialiser Firebase. Vérifiez la configuration de l'application.");
            return;
        }
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) { returnToLogin(); return; }
        uid = current.getUid();
        verifyTeacherProfile(current);
    }

    private void setupTeacherQuickNavigation() {
        LinearLayout bar = findViewById(R.id.teacherQuickNav);
        ScrollView pageScroll = findViewById(R.id.teacherMainScroll);
        if (bar == null || pageScroll == null) return;
        String[][] items = {
                {"👥", "Élèves", "navStudentsSection"},
                {"📅", "Présences", "navAttendanceSection"},
                {"🎨", "Activités", "navActivitiesSection"},
                {"🗒️", "Préparation", "navLessonPreparationSection"},
                {"📊", "Résultats", "navPerformanceSection"},
                {"📝", "Cotation", "navCotationSection"},
                {"📘", "Bulletins", "navBulletinsSection"},
                {"✉️", "Messages", "navMessagesSection"},
                {"🔔", "Alertes", "navNotificationsSection"},
                {"🗂️", "Historique", "navHistorySection"},
                {"🖼️", "Galerie", "navGallerySection"},
                {"⚙️", "Paramètres", "navSettingsSection"}
        };
        int[] targets = {
                R.id.navStudentsSection, R.id.navAttendanceSection, R.id.navActivitiesSection,
                R.id.navLessonPreparationSection, R.id.navPerformanceSection, R.id.navCotationSection,
                R.id.navBulletinsSection, R.id.navMessagesSection, R.id.navNotificationsSection,
                R.id.navHistorySection, R.id.navGallerySection, R.id.navSettingsSection
        };
        for (int i = 0; i < items.length; i++) {
            final int targetId = targets[i];
            MaterialButton button = new MaterialButton(this);
            button.setText(items[i][0] + "\n" + items[i][1]);
            button.setTag("quick_nav");
            button.setAllCaps(false);
            button.setTextSize(11);
            button.setTextColor(Color.rgb(27, 44, 126));
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BG));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.rgb(227, 221, 201)));
            button.setStrokeWidth(dp(1));
            button.setCornerRadius(dp(14));
            button.setInsetTop(0);
            button.setInsetBottom(0);
            button.setMinHeight(dp(62));
            button.setMinimumHeight(dp(62));
            button.setPadding(dp(10), dp(5), dp(10), dp(5));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(88), dp(66));
            params.setMarginEnd(dp(8));
            bar.addView(button, params);
            button.setOnClickListener(v -> {
                showTeacherSection(targetId);
                pageScroll.smoothScrollTo(0, 0);
            });
        }
    }

    private void showTeacherSection(int selectedHeaderId) {
        LinearLayout page = findViewById(R.id.teacherPage);
        LinearLayout bar = findViewById(R.id.teacherQuickNav);
        if (page == null) return;
        int[] sectionIds = {
                R.id.navStudentsSection, R.id.navAttendanceSection, R.id.navActivitiesSection,
                R.id.navLessonPreparationSection, R.id.navPerformanceSection, R.id.navCotationSection,
                R.id.navBulletinsSection, R.id.navMessagesSection, R.id.navNotificationsSection,
                R.id.navHistorySection, R.id.navGallerySection, R.id.navSettingsSection
        };
        int selectedIndex = 0;
        for (int i = 0; i < sectionIds.length; i++) if (sectionIds[i] == selectedHeaderId) selectedIndex = i;
        int firstIndex = page.indexOfChild(findViewById(sectionIds[0]));
        int selectedStart = page.indexOfChild(findViewById(sectionIds[selectedIndex]));
        int selectedEnd = page.getChildCount();
        if (selectedStart < 0 || firstIndex < 0) return;
        for (int i = selectedIndex + 1; i < sectionIds.length; i++) {
            int next = page.indexOfChild(findViewById(sectionIds[i]));
            if (next >= 0) { selectedEnd = next; break; }
        }
        for (int i = firstIndex; i < page.getChildCount(); i++) {
            View child = page.getChildAt(i);
            child.setVisibility(i >= selectedStart && i < selectedEnd ? View.VISIBLE : View.GONE);
        }
        if (bar != null) {
            for (int i = 0; i < bar.getChildCount(); i++) {
                View child = bar.getChildAt(i);
                if (child instanceof MaterialButton) {
                    MaterialButton nav = (MaterialButton) child;
                    boolean active = i == selectedIndex;
                    nav.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            active ? Color.rgb(201, 162, 39) : BG));
                    nav.setTextColor(NAVY);
                    nav.setStrokeColor(android.content.res.ColorStateList.valueOf(
                            active ? GOLD : Color.rgb(227, 221, 201)));
                    nav.setElevation(active ? dp(5) : dp(1));
                }
            }
        }
    }

    private void setupTeacherHeroCarousel() {
        ViewFlipper flipper = findViewById(R.id.teacherHeroCarousel);
        TextView title = findViewById(R.id.teacherHeroTitle);
        TextView subtitle = findViewById(R.id.teacherHeroSubtitle);
        TextView dots = findViewById(R.id.teacherHeroDots);
        if (flipper == null) return;
        String[] titles = {"EDEN FAMILY SCHOOL", "NOS MOMENTS DE RÉUSSITE", "APPRENDRE EN BOUGEANT"};
        String[] subtitles = {"Apprendre, grandir et réussir ensemble",
                "Célébrons le parcours et les réussites de nos élèves",
                "Le sport, le jeu et le bien-être au cœur de nos apprentissages"};
        heroCarouselTask = new Runnable() {
            @Override public void run() {
                if (isFinishing() || isDestroyed()) return;
                flipper.showNext();
                heroSlideIndex = (heroSlideIndex + 1) % 3;
                if (title != null) title.setText(titles[heroSlideIndex]);
                if (subtitle != null) subtitle.setText(subtitles[heroSlideIndex]);
                if (dots != null) dots.setText(heroSlideIndex == 0 ? "●  ○  ○"
                        : heroSlideIndex == 1 ? "○  ●  ○" : "○  ○  ●");
                heroHandler.postDelayed(this, 4200);
            }
        };
        heroHandler.removeCallbacks(heroCarouselTask);
        heroHandler.postDelayed(heroCarouselTask, 4200);
    }

    private void applyModernTeacherTheme() {
        ScrollView root = findViewById(R.id.teacherMainScroll);
        if (root != null) root.setBackgroundColor(BG);
        styleTeacherViews(findViewById(R.id.teacherPage));
        applyReadableDashboardText();
    }

    private void applyReadableDashboardText() {
        int dark = Color.rgb(27, 44, 126);
        int navy = NAVY;
        int[] ids = {R.id.teacherName, R.id.teacherBranch, R.id.teacherClasses,
                R.id.teacherStudentCount, R.id.teacherStatus, R.id.performanceSummary,
                R.id.performanceSubjects, R.id.performanceRanking};
        for (int id : ids) {
            View target = findViewById(id);
            if (target instanceof TextView) ((TextView) target).setTextColor(dark);
            else if (target instanceof android.view.ViewGroup) setTextColorRecursively((android.view.ViewGroup) target, dark);
        }
        int[] headings = {R.id.navStudentsSection, R.id.navAttendanceSection, R.id.navActivitiesSection,
                R.id.navLessonPreparationSection, R.id.navPerformanceSection, R.id.navCotationSection,
                R.id.navBulletinsSection, R.id.navMessagesSection, R.id.navNotificationsSection,
                R.id.navHistorySection, R.id.navGallerySection, R.id.navSettingsSection};
        for (int id : headings) {
            TextView heading = findViewById(id);
            if (heading != null) {
                heading.setTextColor(navy);
                if (frauncesTypeface != null) heading.setTypeface(frauncesTypeface, Typeface.BOLD);
            }
        }
        TextView name = findViewById(R.id.teacherName);
        if (name != null) {
            name.setTextColor(navy);
            if (frauncesTypeface != null) name.setTypeface(frauncesTypeface, Typeface.BOLD);
        }
        TextView branch = findViewById(R.id.teacherBranch);
        if (branch != null) branch.setTextColor(dark);
        TextView carouselTitle = findViewById(R.id.teacherHeroTitle);
        TextView carouselSubtitle = findViewById(R.id.teacherHeroSubtitle);
        TextView carouselDots = findViewById(R.id.teacherHeroDots);
        if (carouselTitle != null) carouselTitle.setTextColor(Color.WHITE);
        if (carouselSubtitle != null) carouselSubtitle.setTextColor(Color.WHITE);
        if (carouselDots != null) carouselDots.setTextColor(Color.WHITE);
        TextView summary = findViewById(R.id.performanceSummary);
        if (summary != null) summary.setTextColor(dark);
    }

    private void setTextColorRecursively(android.view.ViewGroup group, int color) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) ((TextView) child).setTextColor(color);
            else if (child instanceof android.view.ViewGroup) setTextColorRecursively((android.view.ViewGroup) child, color);
        }
    }

    private void styleTeacherViews(View view) {
        if (view == null) return;
        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setCardBackgroundColor(BG);
            card.setRadius(dp(20));
            card.setCardElevation(dp(3));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(Color.rgb(227, 221, 201));
        }
        if (view instanceof TextView) {
            TextView label = (TextView) view;
            int id = label.getId();
            boolean section = id == R.id.navStudentsSection || id == R.id.navAttendanceSection
                    || id == R.id.navActivitiesSection || id == R.id.navLessonPreparationSection
                    || id == R.id.navPerformanceSection
                    || id == R.id.navCotationSection || id == R.id.navBulletinsSection
                    || id == R.id.navMessagesSection || id == R.id.navNotificationsSection
                    || id == R.id.navHistorySection;
            label.setTextColor(getColor(R.color.text_primary));
            if (label.getTypeface() != null && label.getTypeface().isBold() && frauncesTypeface != null) {
                label.setTypeface(frauncesTypeface, Typeface.BOLD);
            } else if (interTypeface != null) {
                label.setTypeface(interTypeface);
            }
            label.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        }
        if (view instanceof MaterialButton) {
            MaterialButton button = (MaterialButton) view;
            button.setAllCaps(false);
            button.setCornerRadius(dp(15));
            if ("quick_nav".equals(button.getTag())) {
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(247, 245, 239)));
                button.setTextColor(NAVY);
                button.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.rgb(227, 221, 201)));
            } else {
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BG));
                button.setTextColor(NAVY);
                button.setStrokeColor(android.content.res.ColorStateList.valueOf(GOLD));
                button.setStrokeWidth(dp(1));
            }
        }
        if (view instanceof EditText) {
            ((EditText) view).setTextColor(getColor(R.color.text_primary));
            ((EditText) view).setHintTextColor(getColor(R.color.text_primary));
            ((EditText) view).setBackgroundResource(R.drawable.bg_input_glass);
        }
        if (view instanceof LinearLayout) {
            LinearLayout group = (LinearLayout) view;
            for (int i = 0; i < group.getChildCount(); i++) styleTeacherViews(group.getChildAt(i));
        } else if (view instanceof ScrollView) {
            styleTeacherViews(((ScrollView) view).getChildAt(0));
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleTeacherViews(group.getChildAt(i));
        }
    }

    private void buildScreen() {
        setContentView(R.layout.activity_teacher_portal);
        KeyboardInsets.apply(this);
        interTypeface = ResourcesCompat.getFont(this, R.font.inter);
        frauncesTypeface = ResourcesCompat.getFont(this, R.font.fraunces);
        setupTeacherQuickNavigation();
        applyModernTeacherTheme();
        setupTeacherHeroCarousel();
        showTeacherSection(R.id.navStudentsSection);
        teacherName = findViewById(R.id.teacherName);
        branchText = findViewById(R.id.teacherBranch);
        classText = findViewById(R.id.teacherClasses);
        studentCount = findViewById(R.id.teacherStudentCount);
        status = findViewById(R.id.teacherStatus);
        studentList = findViewById(R.id.teacherStudentList);
        findViewById(R.id.addStudentButton).setOnClickListener(v -> showStudentDialog(null));
        studentSearch = findViewById(R.id.studentSearch);
        studentSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterStudentRows(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });
        attendanceDate = findViewById(R.id.attendanceDate);
        attendanceStats = findViewById(R.id.attendanceStats);
        attendanceList = findViewById(R.id.attendanceList);
        selectedAttendanceDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        attendanceDate.setText(selectedAttendanceDate);
        attendanceDate.setOnClickListener(v -> openAttendanceDatePicker());
        findViewById(R.id.loadAttendanceButton).setOnClickListener(v -> loadAttendanceForDate());
        findViewById(R.id.saveAttendanceButton).setOnClickListener(v -> saveAttendanceForDate());
        activityList = findViewById(R.id.teacherActivityList);
        activityStatus = findViewById(R.id.activityStatus);
        findViewById(R.id.addActivityButton).setOnClickListener(v -> showActivityDialog());
        lessonPreparationStatus = findViewById(R.id.lessonPreparationStatus);
        lessonPreparationList = findViewById(R.id.lessonPreparationList);
        findViewById(R.id.addLessonPreparationButton).setOnClickListener(v -> showLessonPreparationDialog(null, null));
        findViewById(R.id.refreshLessonPreparationsButton).setOnClickListener(v -> loadLessonPreparations());
        performanceSummary = findViewById(R.id.performanceSummary);
        performanceSubjects = findViewById(R.id.performanceSubjects);
        performanceRanking = findViewById(R.id.performanceRanking);
        performanceTrimester = findViewById(R.id.performanceTrimester);
        cotationClass = findViewById(R.id.cotationClass);
        cotationSubject = findViewById(R.id.cotationSubject);
        cotationType = findViewById(R.id.cotationType);
        cotationTrimester = findViewById(R.id.cotationTrimester);
        cotationAssessment = findViewById(R.id.cotationAssessment);
        cotationMonth = findViewById(R.id.cotationMonth);
        cotationStatus = findViewById(R.id.cotationStatus);
        cotationEntries = findViewById(R.id.cotationEntries);
        cotationSavedStatus = findViewById(R.id.cotationSavedStatus);
        cotationSavedEntries = findViewById(R.id.cotationSavedEntries);
        findViewById(R.id.exportCotationPdfButton).setOnClickListener(v -> exportCotationPdf());
        // Les sélecteurs de cotation sont initialisés dans setupCotationModule().
        findViewById(R.id.saveCotationButton).setOnClickListener(v -> saveCotationSheet());
        setupBulletinViews();
        teacherMessageList = findViewById(R.id.teacherMessageList);
        messageStatus = findViewById(R.id.messageStatus);
        findViewById(R.id.composeMessageButton).setOnClickListener(v -> showNativeComposeMessage());
        findViewById(R.id.refreshMessagesButton).setOnClickListener(v -> loadNativeMessages());
        notificationStatus = findViewById(R.id.notificationStatus);
        teacherNotificationList = findViewById(R.id.teacherNotificationList);
        findViewById(R.id.refreshNotificationsButton).setOnClickListener(v -> loadNativeNotifications());
        galleryStatus = findViewById(R.id.galleryStatus);
        teacherGalleryList = findViewById(R.id.teacherGalleryList);
        findViewById(R.id.addGalleryPhotoButton).setOnClickListener(v -> showAddGalleryPhotoDialog());
        findViewById(R.id.refreshGalleryButton).setOnClickListener(v -> loadTeacherGallery());
        profileFirstName = findViewById(R.id.profileFirstName);
        profileLastName = findViewById(R.id.profileLastName);
        profilePhone = findViewById(R.id.profilePhone);
        profileEmail = findViewById(R.id.profileEmail);
        profileBranch = findViewById(R.id.profileBranch);
        profileClasses = findViewById(R.id.profileClasses);
        profileSettingsStatus = findViewById(R.id.profileSettingsStatus);
        findViewById(R.id.saveTeacherProfileButton).setOnClickListener(v -> saveTeacherProfile());
        findViewById(R.id.changeTeacherEmailButton).setOnClickListener(v -> showChangeTeacherEmailDialog());
        findViewById(R.id.resetTeacherPasswordButton).setOnClickListener(v -> sendTeacherPasswordReset());
        cotationMonth.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 7) loadCotationSheet();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        ArrayAdapter<String> trimesterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                java.util.Arrays.asList("Tous les trimestres", "Trimestre 1", "Trimestre 2", "Trimestre 3"));
        trimesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        performanceTrimester.setAdapter(trimesterAdapter);
        performanceTrimester.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadTeacherPerformance();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        findViewById(R.id.teacherLogout).setOnClickListener(v -> {
            if (auth != null) auth.signOut();
            returnToLogin();
        });
    }

    private void verifyTeacherProfile(FirebaseUser user) {
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot userSnap) {
                if (userSnap.exists()) {
                    String role = userSnap.child("role").getValue(String.class);
                    if (isTeacherRole(role)) { loadTeacherData(userSnap, null, user); return; }
                    rejectWrongRole();
                    return;
                }
                teachersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot teacherSnap) {
                        if (!teacherSnap.exists() || !isTeacherRole(teacherSnap.child("role").getValue(String.class))) {
                            rejectWrongRole();
                            return;
                        }
                        loadTeacherData(null, teacherSnap, user);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        showFatal("Vérification du profil impossible : " + error.getMessage());
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                showFatal("Vérification du rôle impossible : " + error.getMessage());
            }
        });
    }

    private void loadTeacherData(DataSnapshot userSnap, DataSnapshot teacherSnap, FirebaseUser user) {
        DataSnapshot primary = teacherSnap != null ? teacherSnap : userSnap;
        String first = first(value(primary, "firstName"), value(primary, "prenom"));
        String last = first(value(primary, "lastName"), value(primary, "nom"));
        String name = first(value(primary, "name"), (first + " " + last).trim(), value(primary, "nom"),
                user.getDisplayName(), user.getEmail());
        teacherName.setText(name);
        teacherName.setTextColor(NAVY);
        branchText.setTextColor(Color.rgb(28, 35, 51));
        profileFirstName.setText(first);
        profileLastName.setText(last);
        profilePhone.setText(first(value(primary, "phone"), value(primary, "telephone"), value(primary, "phoneNumber")));
        profileEmail.setText("E-mail : " + (user.getEmail() == null ? "Non renseigné" : user.getEmail()));
        teacherBranch = first(value(primary, "branch"), value(primary, "branche"),
                value(primary, "branchName"), value(primary, "schoolBranch"));
        branchText.setText("Branche : " + (teacherBranch.isEmpty() ? "Non renseignée" : teacherBranch));
        profileBranch.setText("Branche : " + (teacherBranch.isEmpty() ? "Non renseignée" : teacherBranch));

        readClasses(primary);
        if (teacherSnap == null) {
            teachersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot extra) {
                    if (extra.exists()) {
                        String extraBranch = first(value(extra, "branch"), value(extra, "branche"),
                                value(extra, "branchName"), value(extra, "schoolBranch"));
                        if (!extraBranch.isEmpty()) teacherBranch = extraBranch;
                        readClasses(extra);
                    }
                    finishProfile();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { finishProfile(); }
            });
        } else finishProfile();
    }

    private void readClasses(DataSnapshot snap) {
        // Fusionner les classes de users/{uid} et teachers/{uid}.
        // Le second profil peut ne contenir que les informations RH et ne doit
        // jamais effacer les classes déjà lues dans le premier profil.
        DataSnapshot classes = snap.child("classes");
        if (classes.exists()) {
            if (classes.getValue() instanceof String) addClasses(classes.getValue(String.class));
            else for (DataSnapshot item : classes.getChildren()) {
                String value = item.getValue(String.class);
                if (value != null && !value.trim().isEmpty()) assignedClasses.add(value.trim());
            }
        }
        String oneClass = first(value(snap, "class"), value(snap, "classe"));
        if (!oneClass.isEmpty()) addClasses(oneClass);
    }

    private void addClasses(String raw) {
        if (raw == null) return;
        for (String item : raw.split(",")) if (!item.trim().isEmpty()) assignedClasses.add(item.trim());
    }

    private void finishProfile() {
        classText.setText(assignedClasses.isEmpty() ? "Aucune classe assignée" : join(assignedClasses));
        profileClasses.setText("Classes : " + (assignedClasses.isEmpty() ? "Aucune" : join(assignedClasses)));
        if (teacherBranch.isEmpty()) {
            teacherBranch = branchFromClassSuffix(firstAssignedClass());
            branchText.setText("Branche : " + (teacherBranch.isEmpty() ? "Non renseignée" : teacherBranch));
            profileBranch.setText("Branche : " + (teacherBranch.isEmpty() ? "Non renseignée" : teacherBranch));
        }
        if (assignedClasses.isEmpty()) {
            status.setText("Votre profil doit contenir au moins une classe assignée. Contactez la direction.");
            studentCount.setText("Accès aux élèves suspendu");
            return;
        }
        loadAssignedStudents();
        loadAssignedActivities();
        loadLessonPreparations();
        loadTeacherPerformance();
        setupCotationModule();
        setupBulletinModule();
        loadNativeMessages();
        loadNativeNotifications();
        loadTeacherGallery();
    }

    private void saveTeacherProfile() {
        String firstName = profileFirstName.getText().toString().trim();
        String lastName = profileLastName.getText().toString().trim();
        String phone = profilePhone.getText().toString().trim();
        if (firstName.isEmpty() || lastName.isEmpty()) {
            profileSettingsStatus.setText("Le prénom et le nom sont obligatoires.");
            return;
        }
        String fullName = (firstName + " " + lastName).trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("prenom", firstName);
        updates.put("nom", lastName);
        updates.put("name", fullName);
        updates.put("phone", phone);
        updates.put("telephone", phone);
        updates.put("updatedAt", System.currentTimeMillis());

        profileSettingsStatus.setText("Enregistrement du profil...");
        final int[] completed = {0};
        final boolean[] failed = {false};
        final String[] error = {""};
        Runnable finishOne = () -> {
            completed[0]++;
            if (completed[0] == 2) {
                if (!failed[0]) {
                    teacherName.setText(fullName);
                    profileSettingsStatus.setText("Profil mis à jour avec succès.");
                    FirebaseUser current = auth == null ? null : auth.getCurrentUser();
                    if (current != null) {
                        com.google.firebase.auth.UserProfileChangeRequest request =
                                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(fullName).build();
                        current.updateProfile(request);
                    }
                } else {
                    profileSettingsStatus.setText("Mise à jour partielle ou refusée : " + error[0]);
                }
            }
        };
        usersRef.child(uid).updateChildren(updates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                failed[0] = true;
                error[0] = task.getException() == null ? "users/" + uid : task.getException().getMessage();
            }
            finishOne.run();
        });
        teachersRef.child(uid).updateChildren(updates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                failed[0] = true;
                if (error[0].isEmpty()) error[0] = task.getException() == null
                        ? "teachers/" + uid : task.getException().getMessage();
            }
            finishOne.run();
        });
    }

    private void showChangeTeacherEmailDialog() {
        FirebaseUser current = auth == null ? null : auth.getCurrentUser();
        if (current == null) return;
        EditText input = new EditText(this);
        input.setHint("Nouvelle adresse e-mail");
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        new AlertDialog.Builder(this)
                .setTitle("Modifier l'adresse e-mail")
                .setMessage("Un lien de vérification sera envoyé à la nouvelle adresse.")
                .setView(input)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Envoyer le lien", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        profileSettingsStatus.setText("Saisissez une adresse e-mail valide.");
                        return;
                    }
                    current.verifyBeforeUpdateEmail(email).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            profileSettingsStatus.setText("Lien de vérification envoyé à " + email +
                                    ". L'adresse sera modifiée après confirmation.");
                        } else {
                            profileSettingsStatus.setText("Modification impossible : " +
                                    (task.getException() == null ? "réessayez." : task.getException().getMessage()));
                        }
                    });
                }).show();
    }

    private void sendTeacherPasswordReset() {
        FirebaseUser current = auth == null ? null : auth.getCurrentUser();
        if (current == null || current.getEmail() == null) {
            profileSettingsStatus.setText("Aucune adresse e-mail associée au compte.");
            return;
        }
        auth.sendPasswordResetEmail(current.getEmail()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                profileSettingsStatus.setText("Lien de réinitialisation envoyé à " + current.getEmail() + ".");
            } else {
                profileSettingsStatus.setText("Envoi impossible : " +
                        (task.getException() == null ? "réessayez." : task.getException().getMessage()));
            }
        });
    }

    private String galleryBranchKey() {
        String key = teacherBranch == null ? "" : teacherBranch.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        return key.isEmpty() ? "unknown_branch" : key;
    }

    private String normalizeGalleryUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:drive\\.google\\.com/file/d/|drive\\.google\\.com/open\\?id=)([A-Za-z0-9_-]+)")
                .matcher(url);
        if (matcher.find()) return "https://drive.google.com/uc?export=view&id=" + matcher.group(1);
        return url;
    }

    private void showAddGalleryPhotoDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        EditText title = new EditText(this);
        title.setHint("Titre de la photo");
        title.setSingleLine(true);
        EditText url = new EditText(this);
        url.setHint("Lien HTTPS de l'image (Google Drive ou autre)");
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        EditText caption = new EditText(this);
        caption.setHint("Description (facultatif)");
        caption.setMinLines(2);
        form.addView(title);
        form.addView(url);
        form.addView(caption);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Ajouter une photo à la galerie")
                .setView(form)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String photoTitle = title.getText().toString().trim();
            String photoUrl = normalizeGalleryUrl(url.getText().toString());
            String description = caption.getText().toString().trim();
            if (photoTitle.isEmpty() || photoUrl.isEmpty()) {
                Toast.makeText(this, "Saisissez un titre et un lien d'image.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!photoUrl.startsWith("https://")) {
                Toast.makeText(this, "Le lien de l'image doit commencer par https://", Toast.LENGTH_LONG).show();
                return;
            }
            // Le portail web lit la galerie dans la racine "gallery", avec branch sur chaque photo.
            DatabaseReference galleryRef = FirebaseService.getInstance(getApplicationContext())
                    .getDatabase().getReference("gallery");
            String key = galleryRef.push().getKey();
            if (key == null) {
                Toast.makeText(this, "Impossible de créer la photo.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> photo = new HashMap<>();
            photo.put("title", photoTitle);
            photo.put("url", photoUrl);
            photo.put("desc", description); // champ utilisé par le portail web
            photo.put("caption", description); // compatibilité avec les anciennes versions Android
            photo.put("uploadedAt", System.currentTimeMillis());
            photo.put("createdAt", System.currentTimeMillis());
            photo.put("createdBy", uid);
            photo.put("teacherName", teacherName == null ? "" : teacherName.getText().toString());
            photo.put("branch", normalizeBranchForMatch(teacherBranch));
            galleryRef.child(key).setValue(photo).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(this, "Photo ajoutée à la galerie.", Toast.LENGTH_SHORT).show();
                    loadTeacherGallery();
                } else {
                    Toast.makeText(this, "Enregistrement impossible : " +
                            (task.getException() == null ? "vérifiez les droits Firebase." : task.getException().getMessage()),
                            Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private void loadTeacherGallery() {
        if (galleryStatus == null || teacherGalleryList == null) return;
        if (teacherBranch == null || teacherBranch.trim().isEmpty()) {
            galleryStatus.setText("La branche de l'enseignant n'est pas renseignée.");
            return;
        }
        galleryStatus.setText("Chargement de la galerie...");
        teacherGalleryList.removeAllViews();
        // Même emplacement et même filtrage que le site : gallery/{photoId}, champ branch.
        DatabaseReference galleryRef = FirebaseService.getInstance(getApplicationContext())
                .getDatabase().getReference("gallery");
        galleryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                teacherGalleryList.removeAllViews();
                java.util.List<DataSnapshot> photos = new java.util.ArrayList<>();
                String expectedBranch = normalizeBranchForMatch(teacherBranch);
                for (DataSnapshot item : snapshot.getChildren()) {
                    String itemBranch = normalizeBranchForMatch(value(item, "branch"));
                    if (expectedBranch.equals(itemBranch)) photos.add(item);
                }
                if (photos.isEmpty()) {
                    galleryStatus.setText("Aucune photo pour le moment. Ajoutez la première photo de votre branche.");
                    return;
                }
                photos.sort((a, b) -> Long.compare(galleryTimestamp(b), galleryTimestamp(a)));
                for (DataSnapshot photo : photos) addTeacherGalleryCard(galleryRef, photo);
                galleryStatus.setText(photos.size() + (photos.size() > 1 ? " photos partagées dans cette branche." : " photo partagée dans cette branche."));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                galleryStatus.setText("Chargement impossible : " + error.getMessage());
            }
        });
    }

    private long galleryTimestamp(DataSnapshot photo) {
        Object raw = photo.child("uploadedAt").getValue();
        if (!(raw instanceof Number)) raw = photo.child("createdAt").getValue();
        return raw instanceof Number ? ((Number) raw).longValue() : 0L;
    }

    private void addTeacherGalleryCard(DatabaseReference galleryRef, DataSnapshot photo) {
        String key = photo.getKey();
        String title = first(value(photo, "title"), "Photo scolaire");
        String url = normalizeGalleryUrl(value(photo, "url"));
        String caption = first(value(photo, "desc"), value(photo, "caption"));
        String author = value(photo, "teacherName");
        String owner = value(photo, "createdBy");
        long createdAt = galleryTimestamp(photo);
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(BG);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(TEXT);
        heading.setTextSize(16);
        heading.setTypeface(null, Typeface.BOLD);
        content.addView(heading);
        ImageView image = new ImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190)));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundColor(Color.rgb(235, 240, 248));
        image.setTag(url);
        content.addView(image);
        TextView details = new TextView(this);
        String date = createdAt > 0 ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(createdAt)) : "";
        details.setText((caption.isEmpty() ? "" : caption + "\\n") +
                (author.isEmpty() ? "" : "Publié par " + author) + (date.isEmpty() ? "" : "  •  " + date));
        details.setTextColor(MUTED);
        details.setTextSize(12);
        details.setPadding(0, dp(8), 0, dp(4));
        content.addView(details);
        if (uid != null && uid.equals(owner)) {
            MaterialButton delete = new MaterialButton(this);
            delete.setText("Supprimer ma photo");
            delete.setAllCaps(false);
            delete.setTextSize(12);
            delete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(255, 241, 242)));
            delete.setTextColor(Color.rgb(170, 35, 55));
            content.addView(delete);
            delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Supprimer cette photo ?")
                    .setMessage("Cette photo sera retirée de la galerie de la branche.")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Supprimer", (d, w) -> galleryRef.child(key).removeValue()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) loadTeacherGallery();
                                else Toast.makeText(this, "Suppression impossible.", Toast.LENGTH_SHORT).show();
                            })).show());
        }
        card.addView(content);
        teacherGalleryList.addView(card);
        loadGalleryImage(image, url);
    }

    private void loadGalleryImage(ImageView target, String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("https://")) return;
        galleryExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setConnectTimeout(16000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "EdenSmart-Android");
                connection.connect();
                if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return;
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                if (bitmap != null) runOnUiThread(() -> {
                    if (imageUrl.equals(target.getTag())) target.setImageBitmap(bitmap);
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    if (imageUrl.equals(target.getTag())) target.setContentDescription("Aperçu indisponible. Vérifiez que le lien est public.");
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void setupCotationModule() {
        java.util.List<String> classes = new java.util.ArrayList<>(assignedClasses);
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classes);
        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cotationClass.setAdapter(classAdapter);
        java.util.List<String> subjects = new java.util.ArrayList<>();
        for (String[] item : cotationSubjects) subjects.add(item[0]);
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjects);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cotationSubject.setAdapter(subjectAdapter);
        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                java.util.Arrays.asList("Trimestre 1", "Trimestre 2", "Trimestre 3"));
        termAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cotationTrimester.setAdapter(termAdapter);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                java.util.Arrays.asList("Travail journalier (TJ /100)", "Examen (EX)"));
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cotationType.setAdapter(typeAdapter);
        cotationMonth.setText(new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date()));
        cotationTrimester.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                cotationAssessmentId = pos == 0 ? "test_1" : "test_1";
                loadCotationAssessments();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        cotationAssessment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < cotationAssessmentIds.size()) {
                    cotationAssessmentId = cotationAssessmentIds.get(pos);
                    loadCotationSheet();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        cotationType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                cotationAssessmentId = pos == 1 ? "exam" : "test_1";
                loadCotationAssessments();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        cotationClass.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { loadCotationAssessments(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        cotationSubject.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { loadCotationAssessments(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        findViewById(R.id.addCotationTestButton).setOnClickListener(v -> createCotationTest());
        findViewById(R.id.removeCotationTestButton).setOnClickListener(v -> removeCotationTest());
        cotationStatus.setText("Cotation par trimestre — données partagées avec le portail web.");
        loadCotationSchoolYear();
    }

    private String cotationKey(String value) {
        String s = value == null ? "" : value.toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
    }
    private String selectedCotationSubject() {
        Object item = cotationSubject.getSelectedItem(); return item == null ? "" : item.toString();
    }
    private String selectedCotationClass() {
        Object item = cotationClass.getSelectedItem(); return item == null ? "" : item.toString();
    }
    private int selectedCotationTerm() { return cotationTrimester == null ? 1 : cotationTrimester.getSelectedItemPosition() + 1; }
    private String cotationTermKey() { return "T" + selectedCotationTerm(); }
    private String cotationAcademicYear() {
        if (cotationSchoolYear != null && cotationSchoolYear.matches("\\d{4}-\\d{4}")) return cotationSchoolYear;
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR), month = c.get(Calendar.MONTH) + 1;
        int start = month >= 9 ? year : year - 1;
        return start + "-" + (start + 1);
    }
    private void loadCotationSchoolYear() {
        FirebaseService.getInstance(getApplicationContext()).getDatabase()
                .getReference("schoolSettings/activeAcademicYear")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String remote = snapshot.getValue(String.class);
                        if (remote != null && remote.trim().matches("\\d{4}-\\d{4}")) {
                            cotationSchoolYear = remote.trim();
                            if (bulletinYear != null) {
                                String current = bulletinYear.getText().toString().trim();
                                if (current.isEmpty() || "2026-2027".equals(current)) bulletinYear.setText(cotationSchoolYear);
                            }
                            if (cotationClass != null && cotationClass.getSelectedItem() != null) loadCotationAssessments();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.w("EdenSmart", "Active academic year unavailable; using calendar year.", error.toException());
                    }
                });
    }
    private String cotationBranchKey() {
        String branch = teacherBranch == null || teacherBranch.trim().isEmpty() ? "default" : teacherBranch;
        return cotationKey(branch);
    }
    private int cotationSubjectMax(String subject) {
        // Toutes les matières et branches utilisent désormais le même barème /100.
        return 100;
    }
    private int selectedCotationMax() { return cotationType.getSelectedItemPosition() == 1 ? cotationSubjectMax(selectedCotationSubject()) : 100; }
    private String cotationBasePath(String cls, String subject, int tri, String type) {
        return "cotations/" + cotationBranchKey() + "_" + cotationKey(cls) + "_" + cotationKey(subject) + "_" +
                cotationKey(cotationAcademicYear()) + "_T" + tri + "_" + type;
    }
    private String cotationAssessmentPath(String id) {
        String type = cotationType.getSelectedItemPosition() == 1 ? "EX" : "TJ";
        return cotationBasePath(selectedCotationClass(), selectedCotationSubject(), selectedCotationTerm(), type) +
                "/" + (type.equals("EX") ? "exam" : id);
    }
    private String selectedCotationMonth() {
        String value = cotationMonth.getText().toString().trim();
        return value.matches("\\d{4}-\\d{2}") ? value : new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }
    private void loadCotationAssessments() {
        if (cotationClass.getSelectedItem() == null || cotationSubject.getSelectedItem() == null) return;
        boolean exam = cotationType.getSelectedItemPosition() == 1;
        String base = cotationBasePath(selectedCotationClass(), selectedCotationSubject(), selectedCotationTerm(), exam ? "EX" : "TJ");
        DatabaseReference ref = FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(base);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                cotationAssessmentIds.clear(); cotationAssessmentLabels.clear();
                if (exam) {
                    cotationAssessmentIds.add("exam"); cotationAssessmentLabels.add("Examen du trimestre");
                } else if (snap.exists()) {
                    for (DataSnapshot child : snap.getChildren()) {
                        String id = child.getKey();
                        if (id == null) continue;
                        cotationAssessmentIds.add(id);
                        String label = child.child("label").getValue(String.class);
                        cotationAssessmentLabels.add(label == null || label.isEmpty() ? "Test " + (cotationAssessmentIds.size()) : label);
                    }
                }
                if (cotationAssessmentIds.isEmpty()) {
                    cotationAssessmentIds.add("test_1"); cotationAssessmentLabels.add("Test 1");
                }
                if (!cotationAssessmentIds.contains(cotationAssessmentId)) cotationAssessmentId = cotationAssessmentIds.get(0);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(TeacherPortalActivity.this,
                        android.R.layout.simple_spinner_item, cotationAssessmentLabels);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                cotationAssessment.setAdapter(adapter);
                int selected = cotationAssessmentIds.indexOf(cotationAssessmentId);
                cotationAssessment.setSelection(Math.max(0, selected));
                cotationAssessment.setEnabled(!exam);
                findViewById(R.id.addCotationTestButton).setVisibility(exam ? View.GONE : View.VISIBLE);
                findViewById(R.id.removeCotationTestButton).setVisibility(exam || cotationAssessmentIds.size() <= 1 ? View.GONE : View.VISIBLE);
                loadCotationSheet();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                cotationStatus.setText("Impossible de charger les tests : " + error.getMessage());
            }
        });
    }
    private void createCotationTest() {
        if (cotationType.getSelectedItemPosition() == 1) return;
        EditText label = new EditText(this); label.setHint("Nom du test (ex. Test 1)");
        EditText start = new EditText(this); start.setHint("Date de début (AAAA-MM-JJ)");
        EditText end = new EditText(this); end.setHint("Date de fin (AAAA-MM-JJ)");
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), dp(4)); form.addView(label); form.addView(start); form.addView(end);
        label.setText("Test " + (cotationAssessmentIds.size() + 1));
        new AlertDialog.Builder(this).setTitle("Créer un test TJ").setView(form)
                .setNegativeButton("Annuler", null).setPositiveButton("Créer", (d, w) -> {
                    String name = label.getText().toString().trim();
                    if (name.isEmpty()) { cotationStatus.setText("Donnez un nom au test."); return; }
                    String id = "test_" + System.currentTimeMillis();
                    String path = cotationBasePath(selectedCotationClass(), selectedCotationSubject(), selectedCotationTerm(), "TJ") + "/" + id;
                    Map<String,Object> data = new HashMap<>();
                    data.put("label", name); data.put("entries", new HashMap<>());
                    data.put("exerciseConfig", new HashMap<>()); data.put("class", selectedCotationClass());
                    data.put("subject", selectedCotationSubject()); data.put("trimester", String.valueOf(selectedCotationTerm()));
                    data.put("type", "TJ"); data.put("month", selectedCotationMonth());
                    data.put("teacherId", uid); data.put("teacherName", teacherName.getText().toString());
                    data.put("branch", teacherBranch); data.put("createdAt", System.currentTimeMillis());
                    data.put("startDate", start.getText().toString().trim()); data.put("endDate", end.getText().toString().trim());
                    FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path).setValue(data)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) { cotationAssessmentId = id; loadCotationAssessments(); }
                                else cotationStatus.setText("Création impossible : " + (task.getException()==null ? "" : task.getException().getMessage()));
                            });
                }).show();
    }
    private void removeCotationTest() {
        if (cotationType.getSelectedItemPosition() == 1 || cotationAssessmentIds.size() <= 1) return;
        new AlertDialog.Builder(this).setTitle("Supprimer le test")
                .setMessage("Supprimer ce test et toutes ses notes ?")
                .setNegativeButton("Annuler", null).setPositiveButton("Supprimer", (d,w) -> {
                    String path = cotationAssessmentPath(cotationAssessmentId);
                    FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path).removeValue()
                            .addOnCompleteListener(task -> { if (task.isSuccessful()) { cotationAssessmentId = "test_1"; loadCotationAssessments(); } });
                }).show();
    }
    private void loadCotationSheet() {
        if (cotationEntries == null || cotationClass.getSelectedItem() == null || cotationSubject.getSelectedItem() == null || accessibleStudents.isEmpty()) return;
        String path = cotationAssessmentPath(cotationAssessmentId);
        cotationStatus.setText("Chargement de la fiche " + cotationTermKey() + "…");
        FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                cotationEntries.removeAllViews(); cotationInputs.clear(); cotationStudentNames.clear(); cotationMaxPoints.clear();
                String[] subject = findCotationSubject(selectedCotationSubject());
                if (subject == null) return;
                int totalMax = selectedCotationMax();
                DataSnapshot subjectConfig = snapshot.child("exerciseConfig").child(selectedCotationSubject());
                for (Map.Entry<String, String> student : accessibleStudents.entrySet()) {
                    String sid = student.getKey(); String[] details = student.getValue().split("\\|", 2);
                    String name = details[0], cls = details.length > 1 ? details[1] : "";
                    if (!normalize(cls).equals(normalize(selectedCotationClass()))) continue;
                    TextView heading = text(name, 14, TEXT, true); heading.setPadding(0, dp(10), 0, dp(3)); cotationEntries.addView(heading);
                    cotationStudentNames.put(sid, name);
                    LinearLayout row = new LinearLayout(TeacherPortalActivity.this); row.setOrientation(LinearLayout.HORIZONTAL);
                    for (int i=1;i<subject.length;i++) {
                        String sub=subject[i], key=cotationKey(sub);
                        DataSnapshot configured=subjectConfig.child(key);
                        double max = key.equals("bricolage_et_construction") ? 10d : (configured.exists()?numberValue(configured):(double)totalMax/(subject.length-1));
                        cotationMaxPoints.put(sid+"|"+key,max);
                        EditText score=new EditText(TeacherPortalActivity.this); score.setSingleLine(true); score.setTextSize(13);
                        score.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        score.setHint(sub+" /"+max); score.setPadding(dp(6),dp(5),dp(6),dp(5));
                        DataSnapshot existing=snapshot.child("entries").child(sid).child(key).child("score");
                        if(existing.exists()) score.setText(String.valueOf(existing.getValue()));
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f); lp.setMarginEnd(dp(4)); row.addView(score,lp);
                        cotationInputs.put(sid+"|"+key,score);
                    }
                    cotationEntries.addView(row);
                    View divider=new View(TeacherPortalActivity.this); divider.setBackgroundColor(Color.rgb(226,232,240));
                    cotationEntries.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
                }
                renderSavedCotationEntries(snapshot, subject);
                cotationStatus.setText(cotationStudentNames.size()+" élève(s) • "+selectedCotationSubject()+" • "+cotationTermKey()+
                        " • "+(cotationType.getSelectedItemPosition()==0?"TJ /100":"EX /"+totalMax)+" • "+selectedCotationMonth());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { cotationStatus.setText("Lecture refusée : "+error.getMessage()); }
        });
    }
    private void renderSavedCotationEntries(DataSnapshot snapshot, String[] subject) {
        cotationSavedRows.clear();
        cotationSavedEntries.removeAllViews();
        DataSnapshot entries = snapshot.child("entries");
        for (DataSnapshot studentNode : entries.getChildren()) {
            String sid = studentNode.getKey() == null ? "" : studentNode.getKey();
            String studentName = cotationStudentNames.getOrDefault(sid, sid);
            for (int i = 1; i < subject.length; i++) {
                String sub = subject[i];
                String key = cotationKey(sub);
                DataSnapshot scoreNode = studentNode.child(key);
                if (!scoreNode.child("score").exists()) continue;
                String score = String.valueOf(scoreNode.child("score").getValue());
                long savedAt = scoreNode.child("savedAt").getValue(Long.class) == null
                        ? 0L : scoreNode.child("savedAt").getValue(Long.class);
                String updated = savedAt > 0
                        ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date(savedAt))
                        : "Date non disponible";
                String line = studentName + " — " + sub + " : " + score + "/" +
                        cotationMaxPoints.getOrDefault(sid + "|" + key, 0.0) + " — " + updated;
                cotationSavedRows.add(line);
                TextView row = text("• " + line, 12, TEXT, false);
                row.setPadding(dp(8), dp(7), dp(4), dp(7));
                cotationSavedEntries.addView(row);
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(226, 232, 240));
                cotationSavedEntries.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        cotationSavedStatus.setText(cotationSavedRows.size() + " note(s) enregistrée(s) • " +
                selectedCotationClass() + " • " + selectedCotationSubject() + " • " +
                cotationTermKey() + " • " + (cotationType.getSelectedItemPosition() == 1 ? "Examen" : "TJ"));
        if (cotationSavedRows.isEmpty()) {
            cotationSavedEntries.addView(text("Aucune note enregistrée pour cette sélection.", 12, MUTED, false));
        }
    }

    private void exportCotationPdf() {
        if (cotationSavedRows.isEmpty()) {
            Toast.makeText(this, "Aucune note enregistrée à exporter.", Toast.LENGTH_LONG).show();
            return;
        }
        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNo = 1, y = 55;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo).create());
        Canvas canvas = page.getCanvas();
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("EDEN FAMILY SCHOOL");
        lines.add("RELEVÉ DES COTATIONS");
        lines.add("Classe : " + selectedCotationClass());
        lines.add("Matière : " + selectedCotationSubject());
        lines.add("Période : " + cotationTermKey() + " • " + selectedCotationMonth());
        lines.add("Évaluation : " + (cotationAssessmentLabels.isEmpty() ? cotationAssessmentId :
                cotationAssessmentLabels.get(Math.max(0, cotationAssessmentIds.indexOf(cotationAssessmentId)))));
        lines.add("Type : " + (cotationType.getSelectedItemPosition() == 1 ? "Examen" : "Travail journalier"));
        lines.add(" ");
        lines.addAll(cotationSavedRows);
        lines.add(" ");
        lines.add("Généré le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date()));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean heading = i < 2;
            paint.setColor(heading ? NAVY : TEXT);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, heading ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextSize(heading ? 16f : 10.5f);
            if (y > 790) {
                pdf.finishPage(page);
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNo).create());
                canvas = page.getCanvas();
                y = 55;
            }
            String remaining = line;
            while (paint.measureText(remaining) > 515f && remaining.length() > 8) {
                int cut = remaining.length();
                while (cut > 8 && paint.measureText(remaining.substring(0, cut)) > 515f) cut--;
                canvas.drawText(remaining.substring(0, cut), 40, y, paint);
                remaining = remaining.substring(cut).trim();
                y += 16;
            }
            canvas.drawText(remaining, 40, y, paint);
            y += heading ? 25 : 18;
        }
        pdf.finishPage(page);
        try {
            String filename = "Cotation_" + cotationKey(selectedCotationClass()) + "_" +
                    cotationKey(selectedCotationSubject()) + "_" + cotationTermKey() + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".pdf";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Eden Family School");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("Impossible de créer le PDF.");
                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new java.io.IOException("Flux de sauvegarde indisponible.");
                    pdf.writeTo(output);
                }
            } else {
                File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Eden Family School");
                if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException("Dossier inaccessible.");
                try (OutputStream output = new FileOutputStream(new File(folder, filename))) {
                    pdf.writeTo(output);
                }
            }
            Toast.makeText(this, "PDF enregistré dans Téléchargements / Eden Family School.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export PDF impossible : " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdf.close();
        }
    }

    private String[] findCotationSubject(String name) {
        for (String[] item : cotationSubjects) if (item[0].equals(name)) return item;
        return null;
    }
    private void saveCotationSheet() {
        if (cotationInputs.isEmpty()) { Toast.makeText(this,"Aucune fiche chargée.",Toast.LENGTH_LONG).show(); return; }
        String subject=selectedCotationSubject(), cls=selectedCotationClass();
        String type=cotationType.getSelectedItemPosition()==1?"EX":"TJ";
        String path=cotationAssessmentPath(cotationAssessmentId);
        Map<String,Object> updates=new HashMap<>(); long now=System.currentTimeMillis();
        for (Map.Entry<String,EditText> item:cotationInputs.entrySet()) {
            String[] parts=item.getKey().split("\\|",2); String raw=item.getValue().getText().toString().trim();
            if(raw.isEmpty()) {
                updates.put("entries/"+parts[0]+"/"+parts[1], null);
                continue;
            }
            double score; try { score=Double.parseDouble(raw); } catch(NumberFormatException ex) { item.getValue().setError("Note invalide"); return; }
            double max=cotationMaxPoints.getOrDefault(item.getKey(),10.0);
            if(score<0 || score>max+0.0001) { item.getValue().setError("Maximum : "+max); return; }
            Map<String,Object> entry=new HashMap<>(); entry.put("score",score); entry.put("savedAt",now); entry.put("savedBy",uid);
            updates.put("entries/"+parts[0]+"/"+parts[1],entry);
        }
        updates.put("label", cotationAssessmentLabels.isEmpty() ? (type.equals("EX")?"Examen du trimestre":"Test") : cotationAssessmentLabels.get(Math.max(0,cotationAssessmentIds.indexOf(cotationAssessmentId))));
        updates.put("teacherId",uid); updates.put("teacherName",teacherName.getText().toString());
        updates.put("branch",teacherBranch); updates.put("class",cls); updates.put("subject",subject);
        updates.put("month",selectedCotationMonth()); updates.put("type",type);
        updates.put("trimester",String.valueOf(selectedCotationTerm())); updates.put("maxScore",type.equals("TJ")?100:cotationSubjectMax(subject));
        updates.put("subjectMaxScore",cotationSubjectMax(subject)); updates.put("updatedAt",now);
        FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        cotationStatus.setText("Fiche sauvegardée • "+subject+" • "+cls+" • "+cotationTermKey());
                        Toast.makeText(this,"Cotations enregistrées.",Toast.LENGTH_SHORT).show();
                        loadCotationSheet();
                    }
                    else cotationStatus.setText("Échec : "+(task.getException()==null?"accès refusé":task.getException().getMessage()));
                });
    }

    private void setupBulletinViews() {
        bulletinStudent = findViewById(R.id.bulletinStudent);
        bulletinTrimester = findViewById(R.id.bulletinTrimester);
        bulletinYear = findViewById(R.id.bulletinYear);
        bulletinTeacher = findViewById(R.id.bulletinTeacher);
        bulletinComment = findViewById(R.id.bulletinComment);
        bulletinStatus = findViewById(R.id.bulletinStatus);
        bulletinGrades = findViewById(R.id.bulletinGrades);
        bulletinConduct = findViewById(R.id.bulletinConduct);
        ArrayAdapter<String> triAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                java.util.Arrays.asList("1", "2", "3", "BN"));
        triAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bulletinTrimester.setAdapter(triAdapter);
        findViewById(R.id.loadBulletinButton).setOnClickListener(v -> loadExistingNativeBulletin());
        findViewById(R.id.saveBulletinButton).setOnClickListener(v -> saveNativeBulletin());
        findViewById(R.id.publishBulletinButton).setOnClickListener(v -> publishSelectedBulletin());
        findViewById(R.id.exportBulletinPdfButton).setOnClickListener(v -> exportNativeBulletinPdf());
        findViewById(R.id.exportBulletinsZipButton).setOnClickListener(v -> exportAccessibleBulletinsZip());
        findViewById(R.id.openPrimaryBulletinButton).setOnClickListener(v -> showPrimaryBulletinDialog());
        bulletinHistoryStatus = findViewById(R.id.bulletinHistoryStatus);
        bulletinHistoryList = findViewById(R.id.bulletinHistoryList);
        findViewById(R.id.refreshBulletinHistoryButton).setOnClickListener(v -> loadNativeBulletinHistory());
        findViewById(R.id.checkMissingBulletinsButton).setOnClickListener(v -> checkMissingNativeBulletins());
        findViewById(R.id.checkDuplicateBulletinsButton).setOnClickListener(v -> detectDuplicateNativeBulletins());
        findViewById(R.id.generatePrimaryAnnualButton).setOnClickListener(v -> generatePrimaryAnnualAndRanking());
        bulletinStudent.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                loadExistingNativeBulletin();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        bulletinTrimester.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                loadExistingNativeBulletin();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        for (String subject : bulletinSubjects) {
            TextView title = text(subject, 13, TEXT, true);
            title.setPadding(0, dp(8), 0, dp(2));
            bulletinGrades.addView(title);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            EditText tj = formField("TJ /100", "");
            EditText ex = formField("EX /100", "");
            tj.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            ex.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            row.addView(tj, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(ex, new LinearLayout.LayoutParams(0, -2, 1f));
            bulletinGrades.addView(row);
            bulletinScoreInputs.put(subject + "|tj", tj);
            bulletinScoreInputs.put(subject + "|ex", ex);
        }
        for (int i = 0; i < conductLabels.length; i++) {
            TextView label = text(conductLabels[i], 12, TEXT, false);
            label.setPadding(0, dp(7), 0, 0);
            bulletinConduct.addView(label);
            Spinner grade = new Spinner(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    java.util.Arrays.asList("—", "E", "TB", "B"));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            grade.setAdapter(adapter);
            bulletinConduct.addView(grade);
            bulletinConductInputs.put("c" + i, grade);
        }
    }

    private void setupBulletinModule() {
        bulletinYear.setText("2026-2027");
        bulletinTeacher.setText(teacherName.getText().toString());
        refreshBulletinStudentChoices();
        loadNativeBulletinHistory();
        loadCotationSchoolYear();
    }

    private void refreshBulletinStudentChoices() {
        if (bulletinStudent == null) return;
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Map.Entry<String, String> item : accessibleStudents.entrySet()) {
            String[] parts = item.getValue().split("\\|", 2);
            names.add(item.getKey() + " • " + parts[0] + (parts.length > 1 ? " (" + parts[1] + ")" : ""));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bulletinStudent.setAdapter(adapter);
        bulletinStatus.setText(names.size() + " élève(s) disponibles pour les bulletins.");
    }

    private String selectedBulletinStudentId() {
        Object item = bulletinStudent == null ? null : bulletinStudent.getSelectedItem();
        if (item == null) return "";
        String value = item.toString();
        int split = value.indexOf(" • ");
        return split < 0 ? "" : value.substring(0, split);
    }

    private String bulletinClassForStudent(String studentId) {
        String record = accessibleStudents.get(studentId);
        if (record == null) return "";
        String[] parts = record.split("\\|", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    private String bulletinPath(String studentId, String trimester) {
        String cls = bulletinClassForStudent(studentId).toLowerCase(Locale.ROOT);
        cls = cls.replaceFirst("_(ka|gi|ki|kacyiru|gisozi|kimisagara)$", "");
        String branch = normalize(teacherBranch);
        String root;
        if (branch.contains("gisozi")) root = "bulletins_gisozi_" + cls;
        else if (branch.contains("kimisagara")) root = "bulletins_kimisagara_" + cls;
        else if (branch.contains("primaire") || branch.contains("pra")) root = "bulletins_primaire_" + cls;
        else if (branch.contains("kacyiru") || branch.contains("maternelle")) root = "bulletins_maternelle_" + cls;
        else root = "bulletins/maternelle/kacyiru/" + cls;
        return root + "/" + studentId + "_" + trimester;
    }

    private void loadExistingNativeBulletin() {
        String studentId = selectedBulletinStudentId();
        if (studentId.isEmpty() || bulletinTrimester.getSelectedItem() == null) return;
        String tri = bulletinTrimester.getSelectedItem().toString();
        DatabaseReference ref = FirebaseService.getInstance(getApplicationContext()).getDatabase()
                .getReference(bulletinPath(studentId, tri));
        bulletinStatus.setText("Recherche du bulletin existant…");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                for (Map.Entry<String, EditText> input : bulletinScoreInputs.entrySet()) input.getValue().setText("");
                for (Spinner input : bulletinConductInputs.values()) input.setSelection(0);
                bulletinComment.setText("");
                if (!snap.exists()) {
                    bulletinStatus.setText("Aucun bulletin enregistré pour cet élève et cette période. Vous pouvez en créer un.");
                    return;
                }
                DataSnapshot skills = snap.child("skills");
                for (String subject : bulletinSubjects) {
                    String key = subject;
                    for (String type : new String[]{"tj", "ex"}) {
                        DataSnapshot score = skills.child(key).child(type);
                        if (!score.exists()) score = skills.child(key).child(type.equals("ex") ? "exam" : type);
                        if (score.exists()) bulletinScoreInputs.get(key + "|" + type).setText(String.valueOf(score.getValue()));
                    }
                }
                DataSnapshot conduct = snap.child("conduct").exists() ? snap.child("conduct") : snap.child("conduite");
                for (Map.Entry<String, Spinner> entry : bulletinConductInputs.entrySet()) {
                    String value = conduct.child(entry.getKey()).getValue(String.class);
                    if (value != null) {
                        String[] grades = {"—", "E", "TB", "B"};
                        for (int i = 0; i < grades.length; i++) if (grades[i].equalsIgnoreCase(value)) entry.getValue().setSelection(i);
                    }
                }
                bulletinComment.setText(first(value(snap, "appreciation"), value(snap, "commentaire")));
                bulletinTeacher.setText(first(value(snap, "teacherName"), value(snap, "teacher"), teacherName.getText().toString()));
                bulletinYear.setText(first(value(snap, "schoolYear"), value(snap, "annee"), "2026-2027"));
                bulletinStatus.setText("Bulletin existant chargé. Vous pouvez le modifier puis l'enregistrer.");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                bulletinStatus.setText("Lecture impossible : " + error.getMessage());
            }
        });
    }



    private static final String[][] PRIMARY_SUBJECTS = {
            {"MATHEMATIQUES", "100", "100"},
            {"ENGLISH", "100", "100"},
            {"KINYARWANDA", "100", "100"},
            {"FRANÇAIS", "100", "100"},
            {"SCIENCE ET I.C.T", "100", "100"},
            {"ET. SOCI ET RELIG", "100", "100"},
            {"ART ET CULTURE", "100", "100"},
            {"EDUCATION PHYS", "100", "100"}
    };

    private void showPrimaryBulletinDialog() {
        String studentId = selectedBulletinStudentId();
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Sélectionnez un élève dans le formulaire des bulletins.", Toast.LENGTH_LONG).show();
            return;
        }
        String cls = bulletinClassForStudent(studentId);
        if (!normalize(cls).matches(".*p[1-6].*")) {
            Toast.makeText(this, "Le formulaire primaire est réservé aux classes P1 à P6.", Toast.LENGTH_LONG).show();
            return;
        }
        String trimester = bulletinTrimester.getSelectedItem() == null ? "1" : bulletinTrimester.getSelectedItem().toString();
        if (!trimester.matches("[123]")) {
            Toast.makeText(this, "Choisissez T1, T2 ou T3 pour le bulletin primaire.", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout form = column();
        form.setPadding(dp(12), dp(4), dp(12), dp(4));
        Map<String, EditText> inputs = new LinkedHashMap<>();
        for (String[] subject : PRIMARY_SUBJECTS) {
            form.addView(text(subject[0] + " (TJ /" + subject[1] + " • EX /" + subject[2] + ")", 13, TEXT, true));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            EditText tj = formField("TJ", "");
            EditText ex = formField("EX", "");
            tj.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            ex.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            row.addView(tj, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(ex, new LinearLayout.LayoutParams(0, -2, 1f));
            form.addView(row);
            inputs.put(subject[0] + "|test", tj);
            inputs.put(subject[0] + "|ex", ex);
        }
        EditText conduct = formField("Conduite", "");
        EditText remark = formField("Remarque / délibération", "");
        EditText date = formField("Date du bulletin (AAAA-MM-JJ)", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        form.addView(conduct); form.addView(remark); form.addView(date);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Bulletin primaire • " + accessibleStudents.get(studentId).split("\\|", 2)[0] + " • T" + trimester)
                .setView(scroll)
                .setNegativeButton("Fermer", null)
                .setPositiveButton("Enregistrer", null)
                .create();
        DatabaseReference ref = FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(
                "bulletins_primaire_" + normalizePrimaryClass(cls) + "/" + studentId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                DataSnapshot scores = snap.child("scores");
                for (int i = 0; i < PRIMARY_SUBJECTS.length; i++) {
                    DataSnapshot row = scores.child(String.valueOf(i)).child("t" + trimester);
                    if (row.child("test").exists()) inputs.get(PRIMARY_SUBJECTS[i][0] + "|test").setText(String.valueOf(row.child("test").getValue()));
                    if (row.child("ex").exists()) inputs.get(PRIMARY_SUBJECTS[i][0] + "|ex").setText(String.valueOf(row.child("ex").getValue()));
                }
                conduct.setText(value(snap, "conduite"));
                remark.setText(value(snap, "remarqueDelib"));
                date.setText(first(value(snap, "bulletinDate"), new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())));
                dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                    savePrimaryBulletin(ref, snap, studentId, cls, trimester, inputs, conduct, remark, date, dialog);
                }));
                dialog.show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherPortalActivity.this, "Lecture du bulletin primaire impossible : " + error.getMessage(), Toast.LENGTH_LONG).show();
                dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false));
                dialog.show();
            }
        });
    }

    private String normalizePrimaryClass(String cls) {
        return cls == null ? "" : cls.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private void savePrimaryBulletin(DatabaseReference ref, DataSnapshot existing, String studentId, String cls,
                                     String trimester, Map<String, EditText> inputs, EditText conduct,
                                     EditText remark, EditText date, AlertDialog dialog) {
        java.util.List<Map<String, Object>> rows = new java.util.ArrayList<>();
        DataSnapshot oldScores = existing.child("scores");
        double[] trimTotals = new double[3];
        for (int i = 0; i < PRIMARY_SUBJECTS.length; i++) {
            String[] subject = PRIMARY_SUBJECTS[i];
            double testMax = Double.parseDouble(subject[1]);
            double exMax = Double.parseDouble(subject[2]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("subject", subject[0]); row.put("maxTest", testMax); row.put("maxEx", exMax);
            row.put("maxTotal", testMax + exMax);
            for (int t = 1; t <= 3; t++) {
                DataSnapshot old = oldScores.child(String.valueOf(i)).child("t" + t);
                double test = old.child("test").getValue(Double.class) == null ? 0 : old.child("test").getValue(Double.class);
                double ex = old.child("ex").getValue(Double.class) == null ? 0 : old.child("ex").getValue(Double.class);
                if (String.valueOf(t).equals(trimester)) {
                    try {
                        String tr = inputs.get(subject[0] + "|test").getText().toString().trim();
                        String er = inputs.get(subject[0] + "|ex").getText().toString().trim();
                        test = tr.isEmpty() ? 0 : Double.parseDouble(tr);
                        ex = er.isEmpty() ? 0 : Double.parseDouble(er);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Une note est invalide dans " + subject[0], Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (test < 0 || test > testMax || ex < 0 || ex > exMax) {
                        Toast.makeText(this, "Valeurs hors limites pour " + subject[0] + " (TJ ≤ " + testMax + ", EX ≤ " + exMax + ").", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                double total = Math.round((test + ex) * 10.0) / 10.0;
                Map<String, Object> term = new HashMap<>();
                term.put("test", test); term.put("ex", ex); term.put("total", total);
                row.put("t" + t, term);
                trimTotals[t - 1] += total;
            }
            double annualTotalRow = 0;
            for (int t = 1; t <= 3; t++) annualTotalRow += ((Map<?, ?>) row.get("t" + t)).get("total") instanceof Number
                    ? ((Number) ((Map<?, ?>) row.get("t" + t)).get("total")).doubleValue() : 0;
            row.put("annualMax", (testMax + exMax) * 3);
            row.put("annualTotal", Math.round(annualTotalRow * 10.0) / 10.0);
            row.put("annualPct", formatPrimaryPct(annualTotalRow / ((testMax + exMax) * 3) * 100));
            rows.add(row);
        }
        double annualTotal = trimTotals[0] + trimTotals[1] + trimTotals[2];
        double currentTotal = trimTotals[Integer.parseInt(trimester) - 1];
        String currentPct = formatPrimaryPct(currentTotal / 1600.0 * 100);
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", studentId);
        payload.put("studentName", accessibleStudents.get(studentId).split("\\|", 2)[0]);
        payload.put("name", accessibleStudents.get(studentId).split("\\|", 2)[0]);
        payload.put("branch", teacherBranch);
        payload.put("class", cls);
        String activeSchoolYear = bulletinYear == null ? cotationAcademicYear() : bulletinYear.getText().toString().trim();
        if (activeSchoolYear.isEmpty()) activeSchoolYear = cotationAcademicYear();
        payload.put("schoolYear", activeSchoolYear);
        payload.put("academicYear", activeSchoolYear);
        payload.put("anneeScolaire", activeSchoolYear);
        payload.put("annee", activeSchoolYear);
        payload.put("trimester", trimester);
        payload.put("section", "primaire");
        payload.put("teacherId", uid);
        payload.put("teacherName", teacherName.getText().toString());
        payload.put("trimestre", Integer.parseInt(trimester));
        payload.put("total", currentTotal);
        payload.put("average", currentPct);
        payload.put("createdAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
        payload.put("updatedAt", System.currentTimeMillis());
        payload.put("mode", "modification");
        payload.put("status", "draft");
        payload.put("remarqueDelib", remark.getText().toString().trim());
        payload.put("conduite", conduct.getText().toString().trim());
        payload.put("bulletinDate", date.getText().toString().trim());
        payload.put("annualTotal", annualTotal);
        payload.put("annualPct", formatPrimaryPct((annualTotal / 3.0) / 1600.0 * 100));
        payload.put("maxGrand", 4800);
        payload.put("trim1Total", trimTotals[0]); payload.put("trim2Total", trimTotals[1]); payload.put("trim3Total", trimTotals[2]);
        payload.put("trim1Pct", formatPrimaryPct(trimTotals[0] / 1600.0 * 100));
        payload.put("trim2Pct", formatPrimaryPct(trimTotals[1] / 1600.0 * 100));
        payload.put("trim3Pct", formatPrimaryPct(trimTotals[2] / 1600.0 * 100));
        payload.put("scores", rows);
        ref.updateChildren(payload).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dialog.dismiss();
                Toast.makeText(this, "Bulletin primaire enregistré • T" + trimester, Toast.LENGTH_LONG).show();
                loadNativeBulletinHistory();
                loadTeacherPerformance();
            } else {
                Toast.makeText(this, "Échec de sauvegarde : " + (task.getException() == null ? "accès refusé" : task.getException().getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatPrimaryPct(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0.0";
        return String.format(Locale.US, "%.1f", Math.floor(value * 10.0) / 10.0);
    }

    private void generatePrimaryAnnualAndRanking() {
        String selectedId = selectedBulletinStudentId();
        if (selectedId.isEmpty()) {
            Toast.makeText(this, "Sélectionnez un élève de la classe concernée.", Toast.LENGTH_SHORT).show();
            return;
        }
        String className = bulletinClassForStudent(selectedId);
        String normalizedClass = normalizePrimaryClass(className);
        if (normalizedClass.isEmpty()) {
            Toast.makeText(this, "Classe primaire non reconnue.", Toast.LENGTH_LONG).show();
            return;
        }
        String sourcePath = "bulletins_primaire_" + normalizedClass;
        String annualPath = "bulletins_annuel_" + normalizedClass;
        bulletinHistoryStatus.setText("Génération des bulletins annuels et du classement…");
        FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(sourcePath)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Map<String, Object>> ranked = new HashMap<>();
                        for (Map.Entry<String, String> entry : accessibleStudents.entrySet()) {
                            String[] parts = entry.getValue().split("\\|", 2);
                            String studentClass = parts.length > 1 ? parts[1] : "";
                            if (!normalizePrimaryClass(studentClass).equals(normalizedClass)) continue;
                            DataSnapshot bulletin = snapshot.child(entry.getKey());
                            double t1 = snapshotNumber(bulletin, "trim1Total");
                            double t2 = snapshotNumber(bulletin, "trim2Total");
                            double t3 = snapshotNumber(bulletin, "trim3Total");
                            double total = t1 + t2 + t3;
                            double average = total / 3.0;
                            double pct = average > 0 ? Math.floor((average / 1600.0 * 100.0) * 10.0) / 10.0 : 0.0;
                            Map<String, Object> item = new HashMap<>();
                            item.put("studentId", entry.getKey());
                            item.put("studentName", parts[0]);
                            item.put("class", className);
                            item.put("branch", teacherBranch);
                            item.put("trim1Total", t1);
                            item.put("trim2Total", t2);
                            item.put("trim3Total", t3);
                            item.put("totalAnnual", total);
                            item.put("averageAnnual", average);
                            item.put("annualPct", formatPrimaryPct(pct));
                            item.put("percentage", pct);
                            item.put("max", 4800);
                            item.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
                            item.put("sourceBulletin", sourcePath + "/" + entry.getKey());
                            ranked.put(entry.getKey(), item);
                        }
                        if (ranked.isEmpty()) {
                            bulletinHistoryStatus.setText("Aucun élève de cette classe n'est attribué à votre compte.");
                            return;
                        }
                        java.util.List<Map<String, Object>> ordered = new java.util.ArrayList<>(ranked.values());
                        ordered.sort((left, right) -> Double.compare(
                                ((Number) right.get("percentage")).doubleValue(),
                                ((Number) left.get("percentage")).doubleValue()));
                        int rank = 0;
                        Double previous = null;
                        for (int i = 0; i < ordered.size(); i++) {
                            Map<String, Object> item = ordered.get(i);
                            double pct = ((Number) item.get("percentage")).doubleValue();
                            if (previous == null || Double.compare(pct, previous) != 0) {
                                rank = i + 1;
                                previous = pct;
                            }
                            item.put("rank", rank);
                            item.put("place", rank + "/" + ordered.size());
                            item.put("grade", primaryGrade(pct));
                            item.remove("percentage");
                        }
                        DatabaseReference annualRef = FirebaseService.getInstance(getApplicationContext())
                                .getDatabase().getReference(annualPath);
                        Map<String, Object> updates = new HashMap<>();
                        for (Map<String, Object> item : ordered) {
                            String sid = String.valueOf(item.get("studentId"));
                            updates.put(sid, item);
                        }
                        annualRef.updateChildren(updates).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                bulletinHistoryStatus.setText("Bulletins annuels générés : " + ordered.size()
                                        + " élève(s) • Classement calculé avec ex æquo.");
                                showPrimaryAnnualRanking(ordered, className);
                                loadNativeBulletinHistory();
                            } else {
                                bulletinHistoryStatus.setText("Échec de génération : "
                                        + (task.getException() == null ? "accès refusé" : task.getException().getMessage()));
                            }
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        bulletinHistoryStatus.setText("Lecture impossible : " + error.getMessage());
                    }
                });
    }

    private double snapshotNumber(DataSnapshot snapshot, String child) {
        Object value = snapshot.child(child).getValue();
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble(((String) value).replace(",", ".")); }
            catch (Exception ignored) { }
        }
        return 0.0;
    }

    private long nativeBulletinTimestamp(DataSnapshot snapshot) {
        for (String field : new String[]{"publishedAt", "updatedAt", "createdAt"}) {
            Object raw = snapshot.child(field).getValue();
            if (raw instanceof Number) return ((Number) raw).longValue();
            if (raw != null) {
                try { return Long.parseLong(raw.toString()); } catch (Exception ignored) { }
            }
        }
        return 0L;
    }

    private String primaryGrade(double percentage) {
        if (percentage >= 80) return "D1";
        if (percentage >= 75) return "C2";
        if (percentage >= 70) return "C3";
        if (percentage >= 60) return "C4";
        if (percentage >= 55) return "C5";
        if (percentage >= 50) return "P6";
        if (percentage >= 45) return "P7";
        if (percentage >= 40) return "P8";
        return "P9";
    }

    private void showPrimaryAnnualRanking(java.util.List<Map<String, Object>> ordered, String className) {
        LinearLayout content = column();
        content.setPadding(dp(16), dp(8), dp(16), dp(8));
        content.addView(text("Classe " + className + " • " + ordered.size() + " élève(s)", 13, MUTED, true));
        for (Map<String, Object> item : ordered) {
            String line = item.get("place") + "  •  " + item.get("studentName")
                    + "  •  " + item.get("annualPct") + "%  •  " + item.get("grade");
            content.addView(text(line, 13, TEXT, true));
            TextView totals = text("T1 : " + formatPrimaryPct(((Number) item.get("trim1Total")).doubleValue())
                    + "/1600   |   T2 : " + formatPrimaryPct(((Number) item.get("trim2Total")).doubleValue())
                    + "/1600   |   T3 : " + formatPrimaryPct(((Number) item.get("trim3Total")).doubleValue())
                    + "/1600", 11, MUTED, false);
            content.addView(totals);
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(230, 234, 242));
            content.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        new AlertDialog.Builder(this).setTitle("Palmarès annuel • " + className)
                .setView(scroll).setPositiveButton("Fermer", null).show();
    }

    private void detectDuplicateNativeBulletins() {
        if (accessibleStudents.isEmpty()) {
            Toast.makeText(this, "Aucun élève dans vos classes attribuées.", Toast.LENGTH_LONG).show();
            return;
        }
        bulletinHistoryStatus.setText("Recherche des doublons dans les bulletins de vos classes…");
        final java.util.Map<String, java.util.Map<String, Object>> records = new java.util.LinkedHashMap<>();
        final java.util.List<String> errors = new java.util.ArrayList<>();
        final int[] pending = {0};
        final boolean[] started = {false};
        for (Map.Entry<String, String> student : accessibleStudents.entrySet()) {
            String sid = student.getKey();
            String[] details = student.getValue().split("\\|", 2);
            String cls = details.length > 1 ? details[1] : "";
            boolean primary = normalize(cls).matches(".*p[1-6].*");
            for (String tri : new String[]{"1", "2", "3"}) {
                java.util.List<String> paths = new java.util.ArrayList<>();
                if (primary) {
                    paths.add("bulletins_primaire_" + normalizePrimaryClass(cls) + "/" + sid);
                } else {
                    paths.add(bulletinPath(sid, tri));
                    String cleanClass = cls.toLowerCase(Locale.ROOT)
                            .replaceFirst("_(ka|gi|ki|kacyiru|gisozi|kimisagara)$", "");
                    String branch = normalize(teacherBranch);
                    String section = branch.contains("primaire") || branch.contains("pra") ? "primaire" : "maternelle";
                    String location = branch.contains("gisozi") ? "gisozi"
                            : branch.contains("kimisagara") ? "kimisagara" : "kacyiru";
                    paths.add("bulletins_" + location + "_" + cleanClass + "/" + sid + "_" + tri);
                    paths.add("bulletins_" + section + "_" + cleanClass + "/" + sid + "_" + tri);
                    paths.add("bulletins/" + section + "/" + location + "/" + cleanClass + "/" + sid + "_" + tri);
                }
                for (String path : new java.util.LinkedHashSet<>(paths)) {
                    pending[0]++;
                    FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                                    if (snap.exists()) {
                                        String actualTri = primary ? value(snap, "trimestre") : tri;
                                        if (actualTri == null || actualTri.isEmpty()) actualTri = tri;
                                        String key = sid + "__" + actualTri;
                                        Map<String, Object> row = new java.util.LinkedHashMap<>();
                                        row.put("studentId", sid);
                                        row.put("studentName", details.length > 0 ? details[0] : sid);
                                        row.put("class", cls);
                                        row.put("trimester", actualTri);
                                        row.put("path", path);
                                        row.put("timestamp", nativeBulletinTimestamp(snap));
                                        records.put(path, row);
                                    }
                                    pending[0]--;
                                    if (started[0] && pending[0] == 0) showNativeDuplicateResults(records, errors);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {
                                    errors.add(path + " : " + error.getMessage());
                                    pending[0]--;
                                    if (started[0] && pending[0] == 0) showNativeDuplicateResults(records, errors);
                                }
                            });
                }
            }
        }
        started[0] = true;
        if (pending[0] == 0) showNativeDuplicateResults(records, errors);
    }

    private void showNativeDuplicateResults(java.util.Map<String, java.util.Map<String, Object>> records,
                                            java.util.List<String> errors) {
        java.util.Map<String, java.util.List<java.util.Map<String, Object>>> groups = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : records.values()) {
            String key = row.get("studentId") + "__" + row.get("trimester");
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(row);
        }
        java.util.List<java.util.Map<String, Object>> duplicates = new java.util.ArrayList<>();
        for (java.util.List<java.util.Map<String, Object>> group : groups.values()) {
            if (group.size() < 2) continue;
            group.sort((a, b) -> Long.compare(
                    ((Number) b.get("timestamp")).longValue(), ((Number) a.get("timestamp")).longValue()));
            duplicates.addAll(group.subList(1, group.size()));
        }
        java.util.Collections.sort(duplicates, (a, b) ->
                String.valueOf(a.get("studentName")).compareToIgnoreCase(String.valueOf(b.get("studentName"))));
        bulletinHistoryStatus.setText(duplicates.size() + " doublon(s) détecté(s)."
                + (errors.isEmpty() ? "" : " " + errors.size() + " chemin(s) non vérifié(s)."));
        StringBuilder message = new StringBuilder();
        if (duplicates.isEmpty()) {
            message.append("Aucun doublon détecté parmi les bulletins accessibles.");
        } else {
            message.append("Anciennes versions détectées (la version la plus récente est conservée) :\n");
            for (Map<String, Object> row : duplicates) {
                message.append("• ").append(row.get("studentName")).append(" — ")
                        .append(row.get("class")).append(" — T").append(row.get("trimester"))
                        .append("\nChemin : ").append(row.get("path")).append("\n\n");
            }
        }
        if (!errors.isEmpty()) {
            message.append("Chemins non vérifiés (non comptés comme doublons) :\n");
            for (String error : errors) message.append("• ").append(error).append("\n");
        }
        new AlertDialog.Builder(this).setTitle("Détection des doublons (" + duplicates.size() + ")")
                .setMessage(message.toString()).setPositiveButton("Fermer", null).show();
    }

    private void checkMissingNativeBulletins() {
        if (accessibleStudents.isEmpty()) {
            Toast.makeText(this, "Aucun élève dans vos classes attribuées.", Toast.LENGTH_LONG).show();
            return;
        }
        String tri = bulletinTrimester.getSelectedItem() == null ? "1" : bulletinTrimester.getSelectedItem().toString();
        final java.util.List<String> missing = new java.util.ArrayList<>();
        final java.util.List<String> errors = new java.util.ArrayList<>();
        final int total = accessibleStudents.size();
        final int[] completed = {0};
        bulletinHistoryStatus.setText("Vérification des bulletins de la période " + tri + "…");
        for (Map.Entry<String, String> student : accessibleStudents.entrySet()) {
            String sid = student.getKey();
            String[] details = student.getValue().split("\\|", 2);
            String name = details.length > 0 ? details[0] : sid;
            String cls = details.length > 1 ? details[1] : "";
            boolean primary = normalize(cls).matches(".*p[1-6].*");
            String path;
            if (primary) {
                path = "BN".equalsIgnoreCase(tri)
                        ? "bulletins_annuel_" + normalizePrimaryClass(cls) + "/" + sid
                        : "bulletins_primaire_" + normalizePrimaryClass(cls) + "/" + sid;
            } else {
                path = bulletinPath(sid, tri);
            }
            FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap) {
                            boolean exists = snap.exists();
                            if (primary && !"BN".equalsIgnoreCase(tri) && exists) {
                                exists = tri.equals(value(snap, "trimestre"));
                            }
                            if (!exists) missing.add(name + " — " + cls);
                            completed[0]++;
                            if (completed[0] == total) showMissingBulletinResults(missing, errors, tri);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            errors.add(name + " — " + cls + " : " + error.getMessage());
                            completed[0]++;
                            if (completed[0] == total) showMissingBulletinResults(missing, errors, tri);
                        }
                    });
        }
    }

    private void showMissingBulletinResults(java.util.List<String> missing, java.util.List<String> errors, String tri) {
        java.util.Collections.sort(missing, String.CASE_INSENSITIVE_ORDER);
        java.util.Collections.sort(errors, String.CASE_INSENSITIVE_ORDER);
        bulletinHistoryStatus.setText(missing.size() + " bulletin(s) manquant(s) pour " + tri
                + (errors.isEmpty() ? "." : " • " + errors.size() + " vérification(s) impossible(s)."));
        StringBuilder message = new StringBuilder();
        message.append(missing.isEmpty() ? "Aucun bulletin manquant détecté." : "Élèves sans bulletin :\n"
                + android.text.TextUtils.join("\n", missing));
        if (!errors.isEmpty()) {
            message.append("\n\nVérifications impossibles (non comptées comme bulletins manquants) :\n")
                    .append(android.text.TextUtils.join("\n", errors));
        }
        new AlertDialog.Builder(this)
                .setTitle("Bulletins manquants — " + tri + " (" + missing.size() + ")")
                .setMessage(message.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }

    private void loadNativeBulletinHistory() {
        if (bulletinHistoryList == null || accessibleStudents.isEmpty()) return;
        bulletinHistoryStatus.setText("Recherche des bulletins de vos classes…");
        bulletinHistoryList.removeAllViews();
        final java.util.List<Map<String, Object>> found = new java.util.ArrayList<>();
        final int total = accessibleStudents.size() * 4;
        final int[] completed = {0};
        for (Map.Entry<String, String> student : accessibleStudents.entrySet()) {
            String sid = student.getKey();
            String[] details = student.getValue().split("\\|", 2);
            String name = details[0];
            String cls = details.length > 1 ? details[1] : "";
            for (String tri : new String[]{"1", "2", "3", "BN"}) {
                FirebaseService.getInstance(getApplicationContext()).getDatabase()
                        .getReference(bulletinPath(sid, tri))
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                                if (snap.exists()) {
                                    Map<String, Object> item = new HashMap<>();
                                    item.put("studentId", sid); item.put("studentName", name);
                                    item.put("class", cls); item.put("trimester", tri);
                                    item.put("appreciation", value(snap, "appreciation"));
                                    item.put("schoolYear", first(value(snap, "schoolYear"), value(snap, "annee")));
                                    item.put("updatedAt", snap.child("updatedAt").getValue());
                                    found.add(item);
                                }
                                completed[0]++;
                                if (completed[0] == total) renderNativeBulletinHistory(found);
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {
                                completed[0]++;
                                if (completed[0] == total) renderNativeBulletinHistory(found);
                            }
                        });
            }
        }
    }

    private void renderNativeBulletinHistory(java.util.List<Map<String, Object>> records) {
        bulletinHistoryList.removeAllViews();
        records.sort((a, b) -> String.valueOf(b.get("updatedAt")).compareTo(String.valueOf(a.get("updatedAt"))));
        if (records.isEmpty()) {
            bulletinHistoryStatus.setText("Aucun bulletin trouvé pour les élèves de vos classes.");
            return;
        }
        bulletinHistoryStatus.setText(records.size() + " bulletin(s) retrouvé(s). Appuyez sur Modifier pour charger le bulletin dans le formulaire.");
        for (Map<String, Object> record : records) {
            LinearLayout card = column();
            card.setPadding(dp(10), dp(10), dp(10), dp(10));
            card.setBackgroundColor(Color.rgb(248, 250, 255));
            card.addView(text(String.valueOf(record.get("studentName")), 15, TEXT, true));
            card.addView(text("Classe : " + record.get("class") + " • Période : " + record.get("trimester") +
                    " • Année : " + first(String.valueOf(record.get("schoolYear")), "—"), 12, MUTED, false));
            String appreciation = String.valueOf(record.get("appreciation"));
            if (!appreciation.equals("null") && !appreciation.trim().isEmpty())
                card.addView(text("Appréciation : " + appreciation, 12, TEXT, false));
            MaterialButton edit = new MaterialButton(this);
            edit.setText("Ouvrir / modifier"); edit.setAllCaps(false);
            String sid = String.valueOf(record.get("studentId"));
            String tri = String.valueOf(record.get("trimester"));
            edit.setOnClickListener(v -> {
                for (int i = 0; i < bulletinStudent.getCount(); i++) {
                    if (bulletinStudent.getItemAtPosition(i).toString().startsWith(sid + " • ")) {
                        bulletinStudent.setSelection(i); break;
                    }
                }
                int triIndex = tri.equals("BN") ? 3 : Math.max(0, Math.min(2, Integer.parseInt(tri) - 1));
                bulletinTrimester.setSelection(triIndex);
                Toast.makeText(this, "Bulletin chargé : modifiez les champs puis enregistrez.", Toast.LENGTH_LONG).show();
            });
            card.addView(edit);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            bulletinHistoryList.addView(card, lp);
        }
    }

    private String gradeFromScore(double score) {
        if (score >= 70) return "E";
        if (score >= 60) return "TB";
        if (score >= 50) return "B";
        return "Effort";
    }

    private void publishSelectedBulletin() {
        String studentId = selectedBulletinStudentId();
        if (studentId.isEmpty() || bulletinTrimester.getSelectedItem() == null) {
            Toast.makeText(this, "Sélectionnez un élève et une période.", Toast.LENGTH_SHORT).show();
            return;
        }
        String tri = bulletinTrimester.getSelectedItem().toString();
        String cls = bulletinClassForStudent(studentId);
        boolean primary = normalize(cls).matches(".*p[1-6].*");
        DatabaseReference ref = FirebaseService.getInstance(getApplicationContext()).getDatabase()
                .getReference(primary
                        ? "bulletins_primaire_" + normalizePrimaryClass(cls) + "/" + studentId
                        : bulletinPath(studentId, tri));
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    Toast.makeText(TeacherPortalActivity.this, "Enregistrez d'abord le bulletin avant de le publier.", Toast.LENGTH_LONG).show();
                    return;
                }
                String savedTri = primary ? value(snap, "trimestre") : first(value(snap, "trimester"), tri);
                if (primary && !tri.equals(savedTri)) {
                    Toast.makeText(TeacherPortalActivity.this, "Le bulletin primaire enregistré correspond à T" + savedTri + ". Sélectionnez cette période.", Toast.LENGTH_LONG).show();
                    return;
                }
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "published");
                updates.put("publishedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
                updates.put("publishedBy", uid);
                updates.put("updatedAt", System.currentTimeMillis());
                ref.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        bulletinStatus.setText("Bulletin publié • " + tri + " • " + cls);
                        Toast.makeText(TeacherPortalActivity.this, "Bulletin publié avec succès.", Toast.LENGTH_LONG).show();
                        loadNativeBulletinHistory();
                    } else {
                        Toast.makeText(TeacherPortalActivity.this, "Publication impossible : "
                                + (task.getException() == null ? "accès refusé" : task.getException().getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherPortalActivity.this, "Lecture impossible : " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveNativeBulletin() {
        String studentId = selectedBulletinStudentId();
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Sélectionnez un élève.", Toast.LENGTH_SHORT).show();
            return;
        }
        String tri = bulletinTrimester.getSelectedItem().toString();
        Map<String, Object> skills = new java.util.LinkedHashMap<>();
        double subjectAveragesTotal = 0; int subjectCount = 0;
        for (String subject : bulletinSubjects) {
            Map<String, Object> grades = new HashMap<>();
            double subjectTotal = 0; int subjectScoreCount = 0;
            for (String type : new String[]{"tj", "ex"}) {
                String raw = bulletinScoreInputs.get(subject + "|" + type).getText().toString().trim();
                if (raw.isEmpty()) continue;
                double score;
                try { score = Double.parseDouble(raw); }
                catch (NumberFormatException ex) {
                    bulletinScoreInputs.get(subject + "|" + type).setError("Note invalide"); return;
                }
                if (score < 0 || score > 100) {
                    bulletinScoreInputs.get(subject + "|" + type).setError("Note entre 0 et 100"); return;
                }
                grades.put(type, score);
                subjectTotal += score; subjectScoreCount++;
            }
            if (!grades.isEmpty()) {
                skills.put(subject, grades);
                subjectAveragesTotal += subjectTotal / subjectScoreCount;
                subjectCount++;
            }
        }
        if (skills.isEmpty()) {
            Toast.makeText(this, "Saisissez au moins une note avant d'enregistrer.", Toast.LENGTH_LONG).show();
            return;
        }
        Map<String, Object> conduct = new HashMap<>();
        for (Map.Entry<String, Spinner> entry : bulletinConductInputs.entrySet()) {
            String grade = entry.getValue().getSelectedItem().toString();
            if (!"—".equals(grade)) conduct.put(entry.getKey(), grade);
        }
        String cls = bulletinClassForStudent(studentId);
        Map<String, Object> data = new HashMap<>();
        data.put("studentId", studentId);
        data.put("studentName", accessibleStudents.get(studentId).split("\\|", 2)[0]);
        data.put("name", accessibleStudents.get(studentId).split("\\|", 2)[0]);
        data.put("branch", teacherBranch);
        data.put("class", cls.replaceFirst("_(KA|GI|KI|KACYIRU|GISOZI|KIMISAGARA)$", "").toUpperCase(Locale.ROOT));
        data.put("section", "maternelle");
        data.put("degree", "Maternelle");
        data.put("teacherId", uid);
        data.put("teacherName", bulletinTeacher.getText().toString().trim());
        data.put("trimester", tri);
        data.put("schoolYear", bulletinYear.getText().toString().trim());
        data.put("annee", bulletinYear.getText().toString().trim());
        data.put("skills", skills);
        data.put("conduct", conduct);
        data.put("conduite", conduct);
        data.put("appreciation", bulletinComment.getText().toString().trim());
        data.put("applicationGrade", subjectCount == 0 ? "B" : gradeFromScore(subjectAveragesTotal / subjectCount));
        data.put("createdAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
        data.put("updatedAt", System.currentTimeMillis());
        data.put("status", "draft");
        DatabaseReference ref = FirebaseService.getInstance(getApplicationContext()).getDatabase()
                .getReference(bulletinPath(studentId, tri));
        ref.setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                bulletinStatus.setText("Bulletin enregistré • " + tri + " • " + cls);
                Toast.makeText(this, "Bulletin enregistré dans Firebase.", Toast.LENGTH_SHORT).show();
            } else {
                bulletinStatus.setText("Échec de sauvegarde : " +
                        (task.getException() == null ? "accès refusé" : task.getException().getMessage()));
            }
        });
    }

    private void loadNativeNotifications() {
        if (teacherNotificationList == null || uid == null) return;
        notificationStatus.setText("Chargement des notifications…");
        FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference("notifications")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot root) {
                        teacherNotificationList.removeAllViews();
                        java.util.List<DataSnapshot> notifications = new java.util.ArrayList<>();
                        for (DataSnapshot item : root.getChildren()) {
                            String target = first(value(item, "teacherId"), value(item, "userId"),
                                    value(item, "uid"), value(item, "recipientId"));
                            String roleTarget = normalize(value(item, "targetRole"));
                            if (uid.equals(target) || roleTarget.equals("teacher")) notifications.add(item);
                        }
                        notifications.sort((a, b) -> Long.compare(
                                b.child("createdAt").getValue(Long.class) == null ? 0L : b.child("createdAt").getValue(Long.class),
                                a.child("createdAt").getValue(Long.class) == null ? 0L : a.child("createdAt").getValue(Long.class)));
                        int unread = 0;
                        for (DataSnapshot item : notifications) {
                            boolean read = Boolean.TRUE.equals(item.child("read").getValue(Boolean.class));
                            if (!read) unread++;
                            String message = first(value(item, "message"), value(item, "title"), "Notification");
                            String title = first(value(item, "title"), value(item, "type"), "Notification");
                            LinearLayout row = column();
                            row.setPadding(dp(12), dp(10), dp(12), dp(10));
                            row.setBackgroundColor(read ? BG : Color.rgb(255, 248, 230));
                            row.addView(text(title + (read ? "" : "  •  NOUVEAU"), 14, read ? TEXT : NAVY, true));
                            row.addView(text(message, 13, MUTED, false));
                            Long timestamp = item.child("createdAt").getValue(Long.class);
                            if (timestamp != null) row.addView(text(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                                    .format(new Date(timestamp)), 11, MUTED, false));
                            row.setOnClickListener(v -> {
                                if (item.getKey() != null && !read) {
                                    FirebaseService.getInstance(getApplicationContext()).getDatabase()
                                            .getReference("notifications").child(item.getKey()).child("read").setValue(true);
                                    row.setBackgroundColor(BG);
                                    loadNativeNotifications();
                                }
                                if ("message_request".equalsIgnoreCase(value(item, "type"))) {
                                    String from = value(item, "fromName");
                                    new AlertDialog.Builder(TeacherPortalActivity.this)
                                            .setTitle("Demande de message")
                                            .setMessage(message + (from.isEmpty() ? "" : "\nDe : " + from))
                                            .setPositiveButton("Fermer", null).show();
                                }
                            });
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                            lp.bottomMargin = dp(6);
                            teacherNotificationList.addView(row, lp);
                        }
                        notificationStatus.setText(notifications.size() + " notification(s) • " + unread + " non lue(s)");
                        if (notifications.isEmpty()) teacherNotificationList.addView(
                                text("Aucune notification pour le moment.", 13, MUTED, false));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        notificationStatus.setText("Chargement impossible : " + error.getMessage());
                    }
                });
    }

    private void loadNativeMessages() {
        if (teacherMessageList == null || uid == null) return;
        messageStatus.setText("Chargement de la boîte de réception…");
        FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference("messages")
                .limitToLast(60).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot root) {
                        teacherMessageList.removeAllViews();
                        java.util.List<DataSnapshot> items = new java.util.ArrayList<>();
                        for (DataSnapshot child : root.getChildren()) {
                            String fromUid = value(child, "fromUid");
                            String toUid = value(child, "toUid");
                            String fromName = normalize(value(child, "from"));
                            String toName = normalize(value(child, "to"));
                            String myName = normalize(teacherName.getText().toString());
                            if (uid.equals(fromUid) || uid.equals(toUid) ||
                                    (!myName.isEmpty() && (myName.equals(fromName) || myName.equals(toName)))) {
                                items.add(child);
                            }
                        }
                        java.util.Collections.reverse(items);
                        int unread = 0;
                        for (DataSnapshot item : items) {
                            String sender = first(value(item, "from"), "Utilisateur");
                            String subject = first(value(item, "subject"), "(Sans sujet)");
                            String body = value(item, "body");
                            boolean read = Boolean.TRUE.equals(item.child("read").getValue(Boolean.class));
                            String messageType = value(item, "type");
                            if (!read && uid.equals(value(item, "toUid"))) unread++;
                            LinearLayout row = column();
                            row.setPadding(dp(12), dp(10), dp(12), dp(10));
                            row.setBackgroundColor(read ? BG : BG);
                            row.addView(text(sender + (read ? "" : "  •  NON LU"), 14, read ? TEXT : NAVY, true));
                            row.addView(text(subject, 14, TEXT, true));
                            row.addView(text(body == null ? "" : body, 13, MUTED, false));
                            String date = item.child("createdAt").getValue() == null ? "" :
                                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                                            .format(new Date(item.child("createdAt").getValue(Long.class) == null ? 0L :
                                                    item.child("createdAt").getValue(Long.class)));
                            row.addView(text(date + ("pending".equalsIgnoreCase(value(item, "status")) ?
                                    " • Demande en attente" : ""), 11, MUTED, false));
                            row.setOnClickListener(v -> {
                                if (item.getKey() != null) {
                                    FirebaseService.getInstance(getApplicationContext()).getDatabase()
                                            .getReference("messages").child(item.getKey()).child("read").setValue(true);
                                    row.setBackgroundColor(BG);
                                    loadNativeMessages();
                                }
                                new AlertDialog.Builder(thisContext())
                                        .setTitle(subject).setMessage(sender + "\n\n" + (body == null ? "" : body))
                                        .setPositiveButton("Fermer", null).show();
                            });
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                            lp.bottomMargin = dp(6);
                            teacherMessageList.addView(row, lp);
                        }
                        messageStatus.setText(items.size() + " message(s) • " + unread + " non lu(s)");
                        if (items.isEmpty()) teacherMessageList.addView(text("Votre boîte de réception est vide.", 13, MUTED, false));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        messageStatus.setText("Chargement impossible : " + error.getMessage());
                    }
                });
    }

    private android.content.Context thisContext() { return TeacherPortalActivity.this; }

    private void showNativeComposeMessage() {
        messageStatus.setText("Recherche des enseignants destinataires…");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot root) {
                messageRecipients.clear();
                java.util.List<String> labels = new java.util.ArrayList<>();
                for (DataSnapshot child : root.getChildren()) {
                    if (uid.equals(child.getKey())) continue;
                    String role = normalize(value(child, "role"));
                    if (!role.equals("teacher") && !role.equals("enseignant") &&
                            !role.equals("enseignante")) continue;
                    String name = first(value(child, "name"),
                            (value(child, "firstName") + " " + value(child, "lastName")).trim(),
                            value(child, "displayName"), value(child, "email"), "Enseignant");
                    Map<String, String> recipient = new HashMap<>();
                    recipient.put("uid", child.getKey());
                    recipient.put("name", name);
                    recipient.put("email", value(child, "email"));
                    messageRecipients.add(recipient);
                    labels.add(name + (value(child, "email").isEmpty() ? "" : " (" + value(child, "email") + ")"));
                }
                if (messageRecipients.isEmpty()) {
                    new AlertDialog.Builder(TeacherPortalActivity.this)
                            .setTitle("Nouveau message")
                            .setMessage("Aucun autre enseignant n'est disponible dans la liste des utilisateurs.")
                            .setPositiveButton("Fermer", null).show();
                    return;
                }
                LinearLayout form = column();
                form.setPadding(dp(18), dp(8), dp(18), dp(4));
                Spinner recipientSpinner = activitySpinner(labels);
                EditText subject = formField("Sujet du message *", "");
                EditText body = formField("Votre message", "");
                body.setMinLines(4);
                body.setGravity(android.view.Gravity.TOP);
                body.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                addFormLabel(form, "Destinataire");
                form.addView(recipientSpinner);
                form.addView(subject);
                form.addView(body);
                android.widget.ScrollView scroll = new android.widget.ScrollView(TeacherPortalActivity.this);
                scroll.addView(form);
                AlertDialog dialog = new AlertDialog.Builder(TeacherPortalActivity.this)
                        .setTitle("Demande de message")
                        .setView(scroll)
                        .setNegativeButton("Annuler", null)
                        .setPositiveButton("Envoyer la demande", null).create();
                dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
                    String sub = subject.getText().toString().trim();
                    if (sub.isEmpty()) { subject.setError("Le sujet est obligatoire"); return; }
                    Map<String, String> recipient = messageRecipients.get(recipientSpinner.getSelectedItemPosition());
                    sendNativeMessageRequest(recipient, sub, body.getText().toString().trim(), dialog);
                }));
                dialog.show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                messageStatus.setText("Impossible de charger les destinataires : " + error.getMessage());
            }
        });
    }

    private void sendNativeMessageRequest(Map<String, String> recipient, String subject,
                                          String body, AlertDialog dialog) {
        String toUid = recipient.get("uid");
        String fromName = teacherName.getText().toString().trim();
        String toName = recipient.get("name");
        long now = System.currentTimeMillis();
        String requestKey = uid + "_" + toUid;
        DatabaseReference root = FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference();
        DatabaseReference requestRef = root.child("message_requests").child(requestKey);
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot existing) {
                if (existing.exists() && "pending".equalsIgnoreCase(value(existing, "status"))) {
                    Toast.makeText(TeacherPortalActivity.this,
                            "Une demande est déjà en attente pour cet enseignant.", Toast.LENGTH_LONG).show();
                    return;
                }
                Map<String, Object> request = new HashMap<>();
                request.put("requestKey", requestKey);
                request.put("type", "message_request");
                request.put("fromUid", uid);
                request.put("fromName", fromName);
                request.put("toUid", toUid);
                request.put("toName", toName);
                request.put("toEmail", recipient.get("email"));
                request.put("subject", subject);
                request.put("body", body);
                request.put("status", "pending");
                request.put("createdAt", now);
                request.put("updatedAt", now);
                Map<String, Object> message = new HashMap<>();
                message.put("type", "message_request");
                message.put("requestKey", requestKey);
                message.put("status", "pending");
                message.put("fromUid", uid);
                message.put("from", fromName);
                message.put("toUid", toUid);
                message.put("to", toName);
                message.put("subject", subject);
                message.put("body", body);
                message.put("createdAt", now);
                message.put("read", false);
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "message_request");
                notification.put("title", "Nouvelle demande de message");
                notification.put("message", fromName + " souhaite vous envoyer un message : " + subject);
                notification.put("teacherId", toUid);
                notification.put("targetRole", "teacher");
                notification.put("fromUid", uid);
                notification.put("fromName", fromName);
                notification.put("createdAt", now);
                notification.put("read", false);
                Map<String, Object> updates = new HashMap<>();
                updates.put("message_requests/" + requestKey, request);
                updates.put("messages/" + root.child("messages").push().getKey(), message);
                updates.put("notifications/" + root.child("notifications").push().getKey(), notification);
                root.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        Toast.makeText(TeacherPortalActivity.this, "Demande envoyée avec succès.", Toast.LENGTH_LONG).show();
                        loadNativeMessages();
                    } else {
                        Toast.makeText(TeacherPortalActivity.this, "Échec d'envoi : vérifiez les règles Firebase.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherPortalActivity.this, "Vérification impossible : " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void exportAccessibleBulletinsZip() {
        if (accessibleStudents.isEmpty()) {
            Toast.makeText(this, "Aucun élève dans vos classes attribuées.", Toast.LENGTH_LONG).show();
            return;
        }
        String tri = bulletinTrimester.getSelectedItem() == null ? "1" : bulletinTrimester.getSelectedItem().toString();
        bulletinStatus.setText("Préparation des bulletins PDF pour le ZIP…");
        final java.util.Map<String, byte[]> pdfFiles = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.List<String> errors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final int total = accessibleStudents.size();
        final int[] completed = {0};
        for (Map.Entry<String, String> student : accessibleStudents.entrySet()) {
            final String sid = student.getKey();
            final String[] details = student.getValue().split("\\|", 2);
            final String studentName = details.length > 0 ? details[0] : sid;
            final String cls = details.length > 1 ? details[1] : "";
            final boolean primary = normalize(cls).matches(".*p[1-6].*");
            final String path;
            if (primary) {
                path = "BN".equalsIgnoreCase(tri)
                        ? "bulletins_annuel_" + normalizePrimaryClass(cls) + "/" + sid
                        : "bulletins_primaire_" + normalizePrimaryClass(cls) + "/" + sid;
            } else {
                path = bulletinPath(sid, tri);
            }
            FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap) {
                            try {
                                boolean valid = snap.exists();
                                if (primary && !"BN".equalsIgnoreCase(tri) && valid) {
                                    valid = tri.equals(value(snap, "trimestre"));
                                }
                                String state = value(snap, "status");
                                if (!state.isEmpty() && !"published".equalsIgnoreCase(state)) valid = false;
                                if (valid) {
                                    byte[] bytes = createBulletinPdfBytes(studentName, cls, tri, snap);
                                    String safe = studentName.replaceAll("[^A-Za-z0-9_-]", "_");
                                    pdfFiles.put("Bulletin_" + safe + "_" + sid.replaceAll("[^A-Za-z0-9_-]", "_")
                                            + "_" + cls.replaceAll("[^A-Za-z0-9_-]", "_") + "_T" + tri + ".pdf", bytes);
                                }
                            } catch (Exception e) {
                                errors.add(studentName + " : " + e.getMessage());
                            }
                            completed[0]++;
                            if (completed[0] == total) finishAccessibleBulletinsZip(pdfFiles, errors, tri);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            errors.add(studentName + " : " + error.getMessage());
                            completed[0]++;
                            if (completed[0] == total) finishAccessibleBulletinsZip(pdfFiles, errors, tri);
                        }
                    });
        }
    }

    private byte[] createBulletinPdfBytes(String studentName, String cls, String tri, DataSnapshot snap)
            throws java.io.IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("EDEN FAMILY SCHOOL");
        lines.add("BULLETIN SCOLAIRE");
        lines.add("Élève : " + studentName);
        lines.add("Classe : " + cls + "    Période : " + tri);
        lines.add("Branche : " + teacherBranch);
        lines.add("Enseignant(e) : " + first(value(snap, "teacherName"), bulletinTeacher.getText().toString()));
        lines.add("");
        lines.add("RÉSULTATS ET INFORMATIONS");
        for (DataSnapshot field : snap.getChildren()) {
            String key = field.getKey() == null ? "" : field.getKey();
            if ("scores".equals(key) || "conduct".equals(key) || "conduite".equals(key)) {
                lines.add(key.toUpperCase(Locale.ROOT) + " :");
                for (DataSnapshot row : field.getChildren()) {
                    StringBuilder rowText = new StringBuilder(row.getKey() == null ? "" : row.getKey());
                    if (row.hasChildren()) {
                        for (DataSnapshot cell : row.getChildren()) {
                            Object v = cell.getValue();
                            if (v != null && !(v instanceof Map) && !(v instanceof java.util.List)) {
                                rowText.append(" | ").append(cell.getKey()).append(": ").append(v);
                            }
                        }
                    } else if (row.getValue() != null) rowText.append(" : ").append(row.getValue());
                    lines.add(rowText.toString());
                }
            } else if (field.getValue() != null && !field.hasChildren()
                    && !"studentId".equals(key) && !"teacherId".equals(key)) {
                lines.add(key + " : " + field.getValue());
            }
        }
        lines.add("");
        lines.add("Document généré le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date()));
        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(TEXT);
        paint.setTextSize(10.5f);
        int pageNo = 1;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo).create());
        Canvas canvas = page.getCanvas();
        int y = 55;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (y > 790) {
                pdf.finishPage(page);
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNo).create());
                canvas = page.getCanvas();
                y = 55;
            }
            boolean heading = i < 2 || "RÉSULTATS ET INFORMATIONS".equals(line);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, heading ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextSize(heading ? 15f : 10.5f);
            paint.setColor(heading ? NAVY : TEXT);
            String remaining = line;
            while (paint.measureText(remaining) > 515f && remaining.length() > 8) {
                int cut = remaining.length();
                while (cut > 8 && paint.measureText(remaining.substring(0, cut)) > 515f) cut--;
                canvas.drawText(remaining.substring(0, cut), 40, y, paint);
                remaining = remaining.substring(cut).trim();
                y += 16;
            }
            canvas.drawText(remaining, 40, y, paint);
            y += heading ? 25 : 18;
        }
        pdf.finishPage(page);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        pdf.writeTo(out);
        pdf.close();
        return out.toByteArray();
    }

    private void finishAccessibleBulletinsZip(java.util.Map<String, byte[]> pdfFiles,
                                              java.util.List<String> errors, String tri) {
        if (pdfFiles.isEmpty()) {
            bulletinStatus.setText("Aucun bulletin publié à exporter pour la période " + tri + ".");
            Toast.makeText(this, "Aucun bulletin publié à exporter.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes);
            java.util.List<String> names = new java.util.ArrayList<>(pdfFiles.keySet());
            java.util.Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            for (String name : names) {
                zip.putNextEntry(new java.util.zip.ZipEntry(name));
                zip.write(pdfFiles.get(name));
                zip.closeEntry();
            }
            zip.close();
            String fileName = "Bulletins_Eden_T" + tri + "_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".zip";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Eden Family School");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new java.io.IOException("Impossible de créer le fichier ZIP.");
                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new java.io.IOException("Flux de sauvegarde indisponible.");
                    output.write(bytes.toByteArray());
                    output.flush();
                }
            } else {
                File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Eden Family School");
                if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException("Dossier de documents inaccessible.");
                try (OutputStream output = new FileOutputStream(new File(folder, fileName))) {
                    output.write(bytes.toByteArray());
                    output.flush();
                }
            }
            String status = pdfFiles.size() + " bulletin(s) PDF regroupé(s) dans " + fileName
                    + (errors.isEmpty() ? "" : " • " + errors.size() + " erreur(s) de lecture.");
            bulletinStatus.setText(status);
            Toast.makeText(this, status, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            bulletinStatus.setText("Création du ZIP impossible : " + e.getMessage());
            Toast.makeText(this, "Erreur ZIP : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportNativeBulletinPdf() {
        String studentId = selectedBulletinStudentId();
        if (studentId.isEmpty()) {
            Toast.makeText(this, "Sélectionnez un élève avant l'exportation.", Toast.LENGTH_SHORT).show();
            return;
        }
        String studentInfo = accessibleStudents.get(studentId);
        String studentName = studentInfo == null ? "Élève" : studentInfo.split("\\|", 2)[0];
        String cls = bulletinClassForStudent(studentId);
        String tri = bulletinTrimester.getSelectedItem() == null ? "1" : bulletinTrimester.getSelectedItem().toString();
        String year = bulletinYear.getText().toString().trim();
        if (year.isEmpty()) year = "Année scolaire non renseignée";

        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("EDEN FAMILY SCHOOL");
        lines.add("BULLETIN SCOLAIRE - MATERNELLE");
        lines.add("Branche : " + teacherBranch);
        lines.add("Élève : " + studentName);
        lines.add("Classe : " + cls);
        lines.add("Trimestre : " + tri + "     Année scolaire : " + year);
        lines.add("Enseignant(e) : " + bulletinTeacher.getText().toString().trim());
        lines.add("");
        lines.add("RÉSULTATS PAR MATIÈRE");
        lines.add("Matière                                      TJ (/100)     EX (/100)");
        double avgTotal = 0; int avgCount = 0;
        for (String subject : bulletinSubjects) {
            String tjRaw = bulletinScoreInputs.get(subject + "|tj").getText().toString().trim();
            String exRaw = bulletinScoreInputs.get(subject + "|ex").getText().toString().trim();
            String tj = tjRaw.isEmpty() ? "—" : tjRaw;
            String ex = exRaw.isEmpty() ? "—" : exRaw;
            lines.add(subject + "     " + tj + "     " + ex);
            double sum = 0; int n = 0;
            try { if (!tjRaw.isEmpty()) { sum += Double.parseDouble(tjRaw); n++; } } catch (Exception ignored) { }
            try { if (!exRaw.isEmpty()) { sum += Double.parseDouble(exRaw); n++; } } catch (Exception ignored) { }
            if (n > 0) { avgTotal += sum / n; avgCount++; }
        }
        lines.add("");
        lines.add("CONDUITE");
        for (Map.Entry<String, Spinner> entry : bulletinConductInputs.entrySet()) {
            Object selected = entry.getValue().getSelectedItem();
            String grade = selected == null ? "—" : selected.toString();
            lines.add(entry.getKey() + " : " + grade);
        }
        lines.add("");
        lines.add("Appréciation générale :");
        String comment = bulletinComment.getText().toString().trim();
        lines.add(comment.isEmpty() ? "—" : comment);
        lines.add("");
        lines.add("Appréciation des résultats : " + (avgCount == 0 ? "Non calculée" :
                gradeFromScore(avgTotal / avgCount)));
        lines.add("Document généré le : " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date()));

        PdfDocument pdf = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(23, 35, 59));
        paint.setTextSize(11f);
        int pageNumber = 1;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create());
        Canvas canvas = page.getCanvas();
        int y = 55;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (y > 790) {
                pdf.finishPage(page);
                page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create());
                canvas = page.getCanvas();
                y = 55;
            }
            if (i == 0 || i == 1 || "RÉSULTATS PAR MATIÈRE".equals(line) || "CONDUITE".equals(line)) {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(i < 2 ? 16f : 12f);
                paint.setColor(NAVY);
            } else {
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                paint.setTextSize(10.5f);
                paint.setColor(TEXT);
            }
            String drawable = line;
            float maxWidth = 515f;
            while (paint.measureText(drawable) > maxWidth && drawable.length() > 8) {
                int cut = drawable.length() - 1;
                while (cut > 8 && paint.measureText(drawable.substring(0, cut)) > maxWidth) cut--;
                canvas.drawText(drawable.substring(0, cut), 40, y, paint);
                y += 16;
                drawable = drawable.substring(cut).trim();
                if (y > 790) {
                    pdf.finishPage(page);
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create());
                    canvas = page.getCanvas();
                    y = 55;
                }
            }
            canvas.drawText(drawable, 40, y, paint);
            y += (i < 2 ? 25 : 19);
        }
        pdf.finishPage(page);

        OutputStream output = null;
        Uri savedUri = null;
        try {
            String safeName = studentName.replaceAll("[^A-Za-z0-9_-]", "_");
            String fileName = "Bulletin_" + safeName + "_T" + tri + "_" +
                    new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".pdf";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Eden Family School");
                savedUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (savedUri == null) throw new java.io.IOException("Impossible de créer le fichier PDF.");
                output = getContentResolver().openOutputStream(savedUri);
            } else {
                File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Eden Family School");
                if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException("Dossier de documents inaccessible.");
                File file = new File(folder, fileName);
                output = new FileOutputStream(file);
            }
            if (output == null) throw new java.io.IOException("Flux de sauvegarde indisponible.");
            pdf.writeTo(output);
            output.flush();
            Toast.makeText(this, "Bulletin PDF exporté dans Téléchargements / Eden Family School.", Toast.LENGTH_LONG).show();
            bulletinStatus.setText("PDF exporté : " + fileName);
        } catch (Exception e) {
            if (savedUri != null) getContentResolver().delete(savedUri, null, null);
            bulletinStatus.setText("Export PDF impossible : " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'export PDF : " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) { }
            pdf.close();
        }
    }

    private void loadAssignedStudents() {
        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot root) {
                studentList.removeAllViews();
                accessibleStudents.clear();
                studentRecords.clear();
                int count = 0;
                for (DataSnapshot child : root.getChildren()) {
                    DataSnapshot s = child;
                    String sBranch = first(value(s, "branch"), value(s, "branche"),
                            value(s, "schoolBranch"), value(s, "branchName"), value(s, "location"),
                            value(s, "schoolLocation"), value(s, "campus"));
                    String sClass = first(snapshotText(s.child("class")), snapshotText(s.child("classes")),
                            snapshotText(s.child("classe")), snapshotText(s.child("className")),
                            snapshotText(s.child("classroom")), snapshotText(s.child("niveau")));
                    // Même logique que le portail web : branche normalisée, suffixe de classe
                    // en secours, puis classe compatible. Un champ branche absent reste accepté.
                    String studentBranchMatch = normalizeBranchForMatch(
                            first(sBranch, branchFromClassSuffix(sClass)));
                    String teacherBranchMatch = normalizeBranchForMatch(
                            first(teacherBranch, branchFromClassSuffix(firstAssignedClass())));
                    if (!teacherBranchMatch.isEmpty() && !studentBranchMatch.isEmpty()
                            && !teacherBranchMatch.equals(studentBranchMatch)) continue;
                    if (!containsClass(sClass)) continue;
                    if (isAssignedToAnotherTeacher(s)) continue;
                    String firstName = first(value(s, "firstName"), value(s, "prenom"));
                    String lastName = first(value(s, "lastName"), value(s, "nom"));
                    String fullName = first((firstName + " " + lastName).trim(),
                            value(s, "name"), value(s, "fullName"), value(s, "studentName"));
                    if (fullName.isEmpty()) fullName = "Élève";
                    accessibleStudents.put(child.getKey(), fullName + "|" + sClass);
                    Map<String, Object> record = new HashMap<>();
                    for (DataSnapshot field : s.getChildren()) record.put(field.getKey(), field.getValue());
                    studentRecords.put(child.getKey(), record);
                    addStudentRow(child.getKey(), fullName, sClass, value(s, "gender"), value(s, "parentName"));
                    count++;
                }
                studentCount.setText(count + " élève(s) dans les classes assignées");
                status.setText(count == 0 ? "Aucun élève trouvé pour cette branche et ces classes." :
                        "Liste filtrée par branche et classes attribuées à votre compte.");
                loadAttendanceForDate();
                loadCotationSheet();
                refreshBulletinStudentChoices();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                status.setText("Impossible de charger les élèves : " + error.getMessage());
            }
        });
    }


    private void loadLessonPreparations() {
        if (lessonPreparationStatus == null || lessonPreparationList == null || uid == null) return;
        lessonPreparationStatus.setText("Chargement des fiches de préparation…");
        lessonPreparationList.removeAllViews();
        FirebaseService.getInstance(getApplicationContext()).getDatabase()
                .getReference("lesson_preparations").orderByChild("teacherUid").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                lessonPreparationRecords.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Map<String, Object> record = (Map<String, Object>) child.getValue();
                        if (record == null) continue;
                        String owner = recordValue(record, "teacherUid", "teacherId", "createdBy");
                        String branch = recordValue(record, "branch", "schoolBranch", "branchName");
                        String cls = recordValue(record, "class", "className", "classe");
                        if (!uid.equals(owner)) continue;
                        if (branch.isEmpty() || !normalizeBranchForMatch(branch)
                                .equals(normalizeBranchForMatch(teacherBranch))) continue;
                        if (cls.isEmpty() || !containsClass(cls)) continue;
                        record.put("_id", child.getKey());
                        lessonPreparationRecords.add(record);
                    }
                }
                lessonPreparationRecords.sort((a, b) ->
                        recordValue(b, "lessonDate", "date").compareTo(recordValue(a, "lessonDate", "date")));
                renderLessonPreparations();
                lessonPreparationStatus.setText(lessonPreparationRecords.size()
                        + " fiche(s) de préparation pour vos classes assignées.");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                lessonPreparationStatus.setText("Chargement refusé par Firebase : " + error.getMessage());
            }
        });
    }

    private void renderLessonPreparations() {
        if (lessonPreparationList == null) return;
        lessonPreparationList.removeAllViews();
        if (lessonPreparationRecords.isEmpty()) {
            TextView empty = text("Aucune fiche enregistrée. Créez votre première préparation de leçon.",
                    13, MUTED, false);
            empty.setPadding(0, dp(12), 0, dp(12));
            lessonPreparationList.addView(empty);
            return;
        }
        for (Map<String, Object> record : lessonPreparationRecords) {
            MaterialCardView card = card();
            LinearLayout content = column();
            content.setPadding(dp(14), dp(12), dp(14), dp(12));
            String title = recordValue(record, "title");
            String cls = recordValue(record, "class", "className", "classe");
            String subject = recordValue(record, "subject");
            String date = recordValue(record, "lessonDate", "date");
            content.addView(text(title.isEmpty() ? "Préparation de leçon" : title, 16, NAVY, true));
            content.addView(text(subject + " • " + cls + " • " + date, 12, MUTED, false));
            String objectives = recordValue(record, "objectives");
            if (!objectives.isEmpty()) content.addView(text("Objectifs : " + objectives, 13, TEXT, false));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            MaterialButton edit = new MaterialButton(this);
            edit.setText("Modifier");
            edit.setAllCaps(false);
            MaterialButton delete = new MaterialButton(this);
            delete.setText("Supprimer");
            delete.setAllCaps(false);
            actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));
            actions.addView(delete, new LinearLayout.LayoutParams(0, dp(44), 1));
            content.addView(actions);
            card.addView(content);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(10);
            lessonPreparationList.addView(card, params);
            String id = recordValue(record, "_id");
            edit.setOnClickListener(v -> showLessonPreparationDialog(id, record));
            delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Supprimer cette fiche ?")
                    .setMessage("Cette action supprimera uniquement votre fiche de préparation.")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Supprimer", (dialog, which) ->
                            FirebaseService.getInstance(getApplicationContext()).getDatabase()
                                    .getReference("lesson_preparations").child(id).removeValue()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) loadLessonPreparations();
                                        else Toast.makeText(this, "Suppression refusée : "
                                                + (task.getException() == null ? "" : task.getException().getMessage()),
                                                Toast.LENGTH_LONG).show();
                                    })).show());
        }
    }

    private void showLessonPreparationDialog(String recordId, Map<String, Object> existing) {
        if (assignedClasses.isEmpty()) {
            Toast.makeText(this, "Aucune classe n'est assignée à votre compte.", Toast.LENGTH_LONG).show();
            return;
        }
        LinearLayout form = column();
        form.setPadding(dp(18), dp(8), dp(18), dp(4));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        Spinner classSpinner = new Spinner(this);
        java.util.List<String> classes = new java.util.ArrayList<>(assignedClasses);
        java.util.Collections.sort(classes);
        classSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classes));
        Spinner subjectSpinner = new Spinner(this);
        String[] subjects = {"Français", "Pré-mathématique", "Étude du milieu",
                "Français langage", "Art & Culture", "Activité Physique et Motrice", "Autre"};
        subjectSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects));
        EditText title = lessonField("Titre / thème de la leçon", existing, "title", false);
        EditText date = lessonField("Date (AAAA-MM-JJ)", existing, "lessonDate", false);
        EditText duration = lessonField("Durée en minutes", existing, "durationMinutes", false);
        EditText objectives = lessonField("Objectifs pédagogiques", existing, "objectives", true);
        EditText materials = lessonField("Matériels et ressources", existing, "materials", true);
        EditText introduction = lessonField("Introduction / mise en situation", existing, "introduction", true);
        EditText development = lessonField("Développement / déroulement", existing, "development", true);
        EditText conclusion = lessonField("Conclusion / synthèse", existing, "conclusion", true);
        EditText assessment = lessonField("Évaluation formative", existing, "assessment", true);
        if (existing != null) {
            String oldClass = recordValue(existing, "class", "className", "classe");
            int classIndex = classes.indexOf(oldClass);
            if (classIndex >= 0) classSpinner.setSelection(classIndex);
            String oldSubject = recordValue(existing, "subject");
            for (int i = 0; i < subjects.length; i++) if (subjects[i].equals(oldSubject)) subjectSpinner.setSelection(i);
        } else {
            date.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
            duration.setText("30");
        }
        addFormLabel(form, "Classe assignée"); form.addView(classSpinner);
        addFormLabel(form, "Matière"); form.addView(subjectSpinner);
        addFormLabel(form, "Titre / thème"); form.addView(title);
        addFormLabel(form, "Date"); form.addView(date);
        date.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try { cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date.getText().toString())); }
            catch (Exception ignored) { }
            new DatePickerDialog(this, (picker, year, month, day) ->
                    date.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        addFormLabel(form, "Durée (minutes)"); form.addView(duration);
        addFormLabel(form, "Objectifs pédagogiques"); form.addView(objectives);
        addFormLabel(form, "Matériels et ressources"); form.addView(materials);
        addFormLabel(form, "Introduction"); form.addView(introduction);
        addFormLabel(form, "Développement"); form.addView(development);
        addFormLabel(form, "Conclusion"); form.addView(conclusion);
        addFormLabel(form, "Évaluation formative"); form.addView(assessment);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(recordId == null ? "Nouvelle fiche de préparation" : "Modifier la fiche")
                .setView(scroll).setNegativeButton("Annuler", null).setPositiveButton("Enregistrer", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String selectedClass = String.valueOf(classSpinner.getSelectedItem());
            String selectedSubject = String.valueOf(subjectSpinner.getSelectedItem());
            String lessonTitle = title.getText().toString().trim();
            String lessonDate = date.getText().toString().trim();
            String minutes = duration.getText().toString().trim();
            if (lessonTitle.isEmpty() || !validIsoDate(lessonDate)) {
                Toast.makeText(this, "Le titre et une date valide sont obligatoires.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!assignedClasses.contains(selectedClass)) {
                Toast.makeText(this, "Cette classe ne fait pas partie de vos affectations.", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (Integer.parseInt(minutes) < 1 || Integer.parseInt(minutes) > 600) throw new NumberFormatException();
            } catch (Exception ex) {
                Toast.makeText(this, "La durée doit être comprise entre 1 et 600 minutes.", Toast.LENGTH_LONG).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("teacherUid", uid);
            data.put("teacherName", teacherName == null ? "" : teacherName.getText().toString());
            data.put("branch", teacherBranch);
            data.put("class", selectedClass);
            data.put("subject", selectedSubject);
            data.put("title", lessonTitle);
            data.put("lessonDate", lessonDate);
            data.put("durationMinutes", Integer.parseInt(minutes));
            data.put("objectives", objectives.getText().toString().trim());
            data.put("materials", materials.getText().toString().trim());
            data.put("introduction", introduction.getText().toString().trim());
            data.put("development", development.getText().toString().trim());
            data.put("conclusion", conclusion.getText().toString().trim());
            data.put("assessment", assessment.getText().toString().trim());
            data.put("updatedAt", System.currentTimeMillis());
            DatabaseReference root = FirebaseService.getInstance(getApplicationContext()).getDatabase()
                    .getReference("lesson_preparations");
            DatabaseReference target = recordId == null ? root.push() : root.child(recordId);
            if (recordId == null) data.put("createdAt", System.currentTimeMillis());
            target.updateChildren(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    loadLessonPreparations();
                    Toast.makeText(this, "Fiche enregistrée dans Firebase.", Toast.LENGTH_SHORT).show();
                } else Toast.makeText(this, "Enregistrement refusé : "
                        + (task.getException() == null ? "" : task.getException().getMessage()),
                        Toast.LENGTH_LONG).show();
            });
        }));
        dialog.show();
    }

    private EditText lessonField(String hint, Map<String, Object> existing, String key, boolean multiline) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(existing == null ? "" : recordValue(existing, key));
        if (multiline) {
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            input.setMinLines(2);
            input.setGravity(android.view.Gravity.TOP);
        } else input.setSingleLine(true);
        return input;
    }

    private void loadAssignedActivities() {
        if (activitiesRef == null || activityList == null) return;
        activityStatus.setText("Chargement des activités…");
        activitiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot root) {
                activityRecords.clear();
                for (DataSnapshot child : root.getChildren()) {
                    String branch = first(value(child, "branch"), value(child, "teacherBranch"));
                    String cls = first(value(child, "class"), value(child, "classe"));
                    // Même normalisation de branche que le filtrage des élèves et le portail web.
                    if (!branch.isEmpty() && !normalizeBranchForMatch(branch).equals(normalizeBranchForMatch(teacherBranch))) continue;
                    if (!containsClass(cls)) continue;
                    Map<String, Object> item = new HashMap<>();
                    item.put("_id", child.getKey());
                    for (DataSnapshot field : child.getChildren()) item.put(field.getKey(), field.getValue());
                    activityRecords.add(item);
                }
                activityRecords.sort((a, b) -> recordValue(a, "date").compareTo(recordValue(b, "date")));
                renderAssignedActivities();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                activityStatus.setText("Chargement impossible : " + error.getMessage());
            }
        });
    }

    private void renderAssignedActivities() {
        activityList.removeAllViews();
        if (activityRecords.isEmpty()) {
            activityStatus.setText("Aucune activité enregistrée pour vos classes.");
            return;
        }
        int upcoming = 0, ongoing = 0, done = 0;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        for (Map<String, Object> item : activityRecords) {
            String start = recordValue(item, "date");
            String end = first(recordValue(item, "dueDate"), start);
            if (!start.isEmpty() && today.compareTo(start) < 0) upcoming++;
            else if (!end.isEmpty() && today.compareTo(end) > 0) done++;
            else ongoing++;

            LinearLayout card = column();
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackgroundColor(Color.rgb(248, 250, 255));
            String type = recordValue(item, "type");
            String title = first(recordValue(item, "title"), "Activité");
            card.addView(text(title, 16, TEXT, true));
            card.addView(text(first(type.toUpperCase(Locale.ROOT), "AUTRE") + " • " +
                    first(recordValue(item, "class"), "Classe") , 12, BLUE, true));
            String dates = "Date : " + (start.isEmpty() ? "Non définie" : start);
            if (!end.isEmpty() && !end.equals(start)) dates += " • Échéance : " + end;
            card.addView(text(dates, 12, MUTED, false));
            String description = recordValue(item, "description");
            if (!description.isEmpty()) card.addView(text(description, 13, TEXT, false));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(8);
            activityList.addView(card, params);
        }
        activityStatus.setText(activityRecords.size() + " activité(s) • À venir : " + upcoming +
                " • En cours : " + ongoing + " • Terminées : " + done);
    }

    private void showActivityDialog() {
        if (assignedClasses.isEmpty()) {
            Toast.makeText(this, "Aucune classe n'est attribuée à votre compte.", Toast.LENGTH_LONG).show();
            return;
        }
        LinearLayout form = column();
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        EditText title = formField("Titre de l'activité *", "");
        EditText description = formField("Description / consignes", "");
        EditText date = formField("Date de début (AAAA-MM-JJ)", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        EditText dueDate = formField("Date limite (AAAA-MM-JJ)", "");
        date.setInputType(android.text.InputType.TYPE_CLASS_DATETIME | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
        dueDate.setInputType(android.text.InputType.TYPE_CLASS_DATETIME | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
        Spinner classSpinner = activitySpinner(new java.util.ArrayList<>(assignedClasses));
        Spinner typeSpinner = activitySpinner(java.util.Arrays.asList("devoir", "cours", "examen", "sortie", "autre"));
        Spinner prioritySpinner = activitySpinner(java.util.Arrays.asList("medium", "high", "low"));
        addFormLabel(form, "Classe"); form.addView(classSpinner);
        addFormLabel(form, "Type"); form.addView(typeSpinner);
        addFormLabel(form, "Priorité"); form.addView(prioritySpinner);
        form.addView(title, 0); form.addView(description, 1); form.addView(date, 2); form.addView(dueDate, 3);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nouvelle activité")
                .setView(scroll)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Enregistrer", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String activityTitle = title.getText().toString().trim();
            String startDate = date.getText().toString().trim();
            String endDate = dueDate.getText().toString().trim();
            if (activityTitle.isEmpty()) {
                title.setError("Le titre est obligatoire");
                return;
            }
            if (!validIsoDate(startDate) || (!endDate.isEmpty() && !validIsoDate(endDate))) {
                Toast.makeText(this, "Utilisez le format de date AAAA-MM-JJ.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!endDate.isEmpty() && endDate.compareTo(startDate) < 0) {
                Toast.makeText(this, "La date limite doit être après la date de début.", Toast.LENGTH_LONG).show();
                return;
            }
            String cls = String.valueOf(classSpinner.getSelectedItem());
            if (!assignedClasses.contains(cls)) return;
            Map<String, Object> data = new HashMap<>();
            data.put("title", activityTitle);
            data.put("description", description.getText().toString().trim());
            data.put("date", startDate);
            data.put("dueDate", endDate);
            data.put("type", String.valueOf(typeSpinner.getSelectedItem()));
            data.put("class", cls);
            data.put("branch", teacherBranch);
            data.put("teacherId", uid);
            data.put("teacherName", teacherName.getText().toString());
            data.put("teacherBranch", teacherBranch);
            data.put("priority", String.valueOf(prioritySpinner.getSelectedItem()));
            data.put("attachments", new java.util.ArrayList<>());
            data.put("createdAt", System.currentTimeMillis());
            data.put("updatedAt", System.currentTimeMillis());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            activitiesRef.push().setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(this, "Activité enregistrée.", Toast.LENGTH_SHORT).show();
                    loadAssignedActivities();
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this, "Enregistrement refusé : vérifiez les règles Firebase.", Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private Spinner activitySpinner(java.util.List<String> values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void addFormLabel(LinearLayout form, String label) {
        TextView view = text(label, 13, MUTED, true);
        view.setPadding(0, dp(8), 0, dp(3));
        form.addView(view);
    }

    private boolean validIsoDate(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            return format.parse(value) != null;
        } catch (Exception e) { return false; }
    }

    private java.util.List<String> bulletinPathsForClass(String className) {
        String cls = normalize(className).toLowerCase(Locale.ROOT);
        cls = cls.replaceAll("_(ka|gi|ki|kacyiru|gisozi|kimisagara)$", "");
        String branch = normalize(teacherBranch);
        String section = branch.contains("primaire") || branch.contains("pra") ? "primaire" : "maternelle";
        String location = branch.contains("gisozi") ? "gisozi" :
                branch.contains("kimisagara") ? "kimisagara" : "kacyiru";
        Set<String> paths = new java.util.LinkedHashSet<>();
        if (branch.contains("kacyiru") || branch.contains("maternelle")) paths.add("bulletins_maternelle_" + cls);
        if (branch.contains("kimisagara")) paths.add("bulletins_kimisagara_" + cls);
        if (branch.contains("gisozi")) paths.add("bulletins_gisozi_" + cls);
        if (section.equals("primaire")) paths.add("bulletins_primaire_" + cls);
        paths.add("bulletins_" + location + "_" + cls);
        paths.add("bulletins_" + section + "_" + cls);
        paths.add("bulletins/" + section + "/" + location + "/" + cls);
        if (location.equals("kimisagara")) paths.add("bulletins/maternelle/kimisagara/" + cls);
        return new java.util.ArrayList<>(paths);
    }

    private void loadTeacherPerformance() {
        if (performanceSummary == null || assignedClasses.isEmpty()) return;
        performanceSummary.setTextColor(Color.rgb(28, 35, 51));
        performanceSummary.setText("Chargement des bulletins publiés…");
        performanceSubjects.removeAllViews();
        performanceRanking.removeAllViews();
        final String selectedTri = performanceTrimester == null || performanceTrimester.getSelectedItemPosition() == 0
                ? "ALL" : String.valueOf(performanceTrimester.getSelectedItemPosition());
        final String expectedYear = bulletinYear == null ? "2026-2027" : bulletinYear.getText().toString().trim();
        final Set<String> allowedClasses = new java.util.HashSet<>();
        for (String cls : assignedClasses) allowedClasses.add(normalizePerformanceClass(cls));
        final String expectedBranch = normalizePerformanceBranch(teacherBranch);
        Set<String> paths = new java.util.LinkedHashSet<>();
        for (String cls : assignedClasses) paths.addAll(bulletinPathsForClass(cls));
        if (paths.isEmpty()) {
            performanceSummary.setText("Aucun bulletin trouvé pour vos classes.");
            return;
        }
        final Map<String, Map<String, Object>> bulletins = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(paths.size());
        for (String path : paths) {
            FirebaseService.getInstance(getApplicationContext()).getDatabase().getReference(path)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot root) {
                            for (DataSnapshot child : root.getChildren()) {
                                if (!child.exists() || !child.child("skills").exists()) continue;
                                String statusValue = normalize(value(child, "status"));
                                if (!statusValue.isEmpty() && !statusValue.equals("published")) continue;
                                String year = first(value(child, "schoolYear"), value(child, "annee"));
                                if (!expectedYear.isEmpty() && !year.isEmpty() && !expectedYear.equals(year)) continue;
                                String recordBranch = first(value(child, "branch"), value(child, "schoolBranch"));
                                if (!expectedBranch.isEmpty() && !recordBranch.isEmpty()
                                        && !expectedBranch.equals(normalizePerformanceBranch(recordBranch))) continue;
                                String childKey = child.getKey() == null ? "" : child.getKey();
                                String studentId = first(value(child, "studentId"), value(child, "studentID"),
                                        value(child, "id"), childKey.replaceFirst("_(T)?[123]$", ""));
                                if (studentId.isEmpty() || !accessibleStudents.containsKey(studentId)) continue;
                                String studentRecord = accessibleStudents.get(studentId);
                                String[] studentParts = studentRecord == null ? new String[0] : studentRecord.split("\\|", 2);
                                String authorizedClass = studentParts.length > 1 ? studentParts[1] : "";
                                String recordClass = first(value(child, "class"), value(child, "classes"), value(child, "classe"), authorizedClass);
                                if (!allowedClasses.contains(normalizePerformanceClass(authorizedClass))) continue;
                                if (!normalizePerformanceClass(recordClass).equals(normalizePerformanceClass(authorizedClass))) continue;
                                String tri = first(value(child, "trimester"), value(child, "trimestre"));
                                if (tri.isEmpty() && childKey.toUpperCase(Locale.ROOT).matches(".*_(T)?[123]$"))
                                    tri = childKey.substring(childKey.length() - 1);
                                String normalizedTri = normalizeTrimester(tri);
                                if (!"ALL".equals(selectedTri) && !selectedTri.equals(normalizedTri)) continue;
                                if (!normalizedTri.matches("[123]")) continue;
                                Map<String, Object> item = new HashMap<>();
                                item.put("studentId", studentId);
                                item.put("studentName", first(value(child, "studentName"), value(child, "name"), studentParts.length > 0 ? studentParts[0] : "Élève"));
                                item.put("class", authorizedClass);
                                item.put("skills", child.child("skills"));
                                item.put("publishedAt", first(value(child, "publishedAt"), value(child, "updatedAt"), value(child, "createdAt")));
                                String signature = studentId + "|" + normalizedTri;
                                Map<String, Object> previous = bulletins.get(signature);
                                if (previous == null || String.valueOf(item.get("publishedAt")).compareTo(String.valueOf(previous.get("publishedAt"))) > 0)
                                    bulletins.put(signature, item);
                            }
                            if (pending.decrementAndGet() == 0) renderTeacherPerformance(bulletins);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            if (pending.decrementAndGet() == 0) renderTeacherPerformance(bulletins);
                        }
                    });
        }
    }

    private String normalizePerformanceClass(String raw) {
        String value = normalize(raw).toLowerCase(Locale.ROOT).replaceAll("_(ka|gi|ki|kacyiru|gisozi|kimisagara)$", "");
        return value.replaceAll("[^a-z0-9]", "");
    }

    private String normalizePerformanceBranch(String raw) {
        return normalize(raw).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private double performanceSubjectMax(String subject) {
        // Harmonisation du calcul des performances : chaque matière est sur 100.
        return 100d;
    }

    private String normalizeTrimester(String raw) {
        String v = normalize(raw).toUpperCase(Locale.ROOT);
        if (v.contains("3")) return "3";
        if (v.contains("2")) return "2";
        if (v.contains("1")) return "1";
        if (v.contains("ANNEE") || v.equals("BN")) return "BN";
        return "";
    }

    private double numberValue(DataSnapshot snap) {
        Object value = snap.getValue();
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble(((String) value).replace(",", ".")); }
            catch (Exception ignored) { }
        }
        return 0;
    }

    private void renderTeacherPerformance(Map<String, Map<String, Object>> bulletins) {
        Map<String, double[]> subjects = new java.util.TreeMap<>();
        Map<String, Map<String, Object>> students = new HashMap<>();
        int validScores = 0;
        for (Map<String, Object> bulletin : bulletins.values()) {
            Object rawSkills = bulletin.get("skills");
            if (!(rawSkills instanceof DataSnapshot)) continue;
            DataSnapshot skills = (DataSnapshot) rawSkills;
            String sid = String.valueOf(bulletin.get("studentId"));
            String name = String.valueOf(bulletin.get("studentName"));
            String cls = String.valueOf(bulletin.get("class"));
            Map<String, Object> student = students.get(sid);
            if (student == null) {
                student = new HashMap<>();
                student.put("name", name); student.put("class", cls);
                student.put("sum", 0d); student.put("count", 0);
                students.put(sid, student);
            }
            for (DataSnapshot skill : skills.getChildren()) {
                DataSnapshot tjSnap = skill.child("tj");
                DataSnapshot exSnap = skill.child("ex").exists() ? skill.child("ex") : skill.child("exam");
                if (!tjSnap.exists() || !exSnap.exists()) continue;
                double max = performanceSubjectMax(skill.getKey());
                double tj = numberValue(tjSnap);
                double ex = numberValue(exSnap);
                if (max <= 0 || tj < 0 || ex < 0 || tj > max || ex > max) continue;
                // Même calcul que le bulletin web : moyenne des pourcentages TJ et EX.
                double tjPercent = (tj / max) * 100d;
                double exPercent = (ex / max) * 100d;
                double avg = (tjPercent + exPercent) / 2d;
                double[] aggregate = subjects.get(skill.getKey());
                if (aggregate == null) { aggregate = new double[]{0d, 0d}; subjects.put(skill.getKey(), aggregate); }
                aggregate[0] += avg; aggregate[1] += 1d;
                student.put("sum", ((Double) student.get("sum")) + avg);
                student.put("count", ((Integer) student.get("count")) + 1);
                validScores++;
            }
        }
        performanceSubjects.removeAllViews();
        performanceRanking.removeAllViews();
        if (bulletins.isEmpty() || validScores == 0) {
            performanceSummary.setText("Aucun résultat trouvé pour ce trimestre dans les bulletins de vos classes.");
            performanceSubjects.addView(text("Les résultats apparaîtront ici lorsqu'ils seront enregistrés dans les bulletins.", 13, MUTED, false));
            return;
        }
        double overallSum = 0d;
        int overallCount = 0;
        for (Map.Entry<String, double[]> entry : subjects.entrySet()) {
            double avg = entry.getValue()[0] / entry.getValue()[1];
            overallSum += entry.getValue()[0];
            overallCount += (int) entry.getValue()[1];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            TextView subject = text(entry.getKey() == null ? "Matière" : entry.getKey(), 14, TEXT, true);
            row.addView(subject, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text(String.format(Locale.getDefault(), "%.1f / 100", avg), 14, BLUE, true));
            performanceSubjects.addView(row);
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(230, 234, 242));
            performanceSubjects.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        java.util.List<Map.Entry<String, Map<String, Object>>> ranked = new java.util.ArrayList<>(students.entrySet());
        ranked.sort((a, b) -> Double.compare(
                ((Double) b.getValue().get("sum")) / Math.max(1, (Integer) b.getValue().get("count")),
                ((Double) a.getValue().get("sum")) / Math.max(1, (Integer) a.getValue().get("count"))));
        int rank = 1;
        for (Map.Entry<String, Map<String, Object>> entry : ranked) {
            Map<String, Object> student = entry.getValue();
            int count = (Integer) student.get("count");
            if (count == 0) continue;
            double average = ((Double) student.get("sum")) / count;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            TextView identity = text(rank + ". " + student.get("name") + " • " + student.get("class"), 13, TEXT, true);
            row.addView(identity, new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(text(String.format(Locale.getDefault(), "%.1f", average), 14, BLUE, true));
            performanceRanking.addView(row);
            rank++;
        }
        performanceSummary.setTextColor(Color.rgb(28, 35, 51));
        performanceSummary.setText(students.size() + " élève(s) évalué(s) • " + overallCount +
                " notes prises en compte • Moyenne générale : " +
                String.format(Locale.getDefault(), "%.1f / 100", overallSum / Math.max(1, overallCount)));
    }

    private void openAttendanceDatePicker() {
        Calendar c = Calendar.getInstance();
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedAttendanceDate);
            if (parsed != null) c.setTime(parsed);
        } catch (Exception ignored) { }
        new DatePickerDialog(this, (picker, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            selectedAttendanceDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selected.getTime());
            attendanceDate.setText(selectedAttendanceDate);
            loadAttendanceForDate();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadAttendanceForDate() {
        if (attendanceList == null) return;
        attendanceList.removeAllViews();
        attendanceGroups.clear();
        savedAttendance.clear();
        if (accessibleStudents.isEmpty()) {
            attendanceStats.setText("Aucun élève disponible pour les classes assignées.");
            return;
        }
        DatabaseReference ref = FirebaseService.getInstance(getApplicationContext())
                .getDatabase().getReference("attendance").child(selectedAttendanceDate);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                savedAttendance.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String val = child.getValue(String.class);
                    if (val != null) savedAttendance.put(child.getKey(), val);
                }
                attendanceList.removeAllViews();
                attendanceGroups.clear();
                for (Map.Entry<String, String> entry : accessibleStudents.entrySet()) {
                    String[] parts = entry.getValue().split("\\|", 2);
                    String name = parts.length > 0 ? parts[0] : "Élève";
                    String cls = parts.length > 1 ? parts[1] : "";
                    addAttendanceRow(entry.getKey(), name, cls, savedAttendance.get(entry.getKey()));
                }
                updateAttendanceStats();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                attendanceStats.setText("Lecture des présences impossible : " + error.getMessage());
            }
        });
    }

    private void addAttendanceRow(String studentId, String name, String cls, String savedValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(4), dp(8), dp(4), dp(8));
        TextView student = text(name + (cls.isEmpty() ? "" : " • " + cls), 14, TEXT, true);
        row.addView(student);
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        String[] labels = {"Présent", "Absent", "Retard"};
        String[] values = {"P", "A", "R"};
        for (int i = 0; i < values.length; i++) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setText(labels[i]);
            option.setTextSize(12);
            option.setTag(values[i]);
            group.addView(option, new RadioGroup.LayoutParams(0, -2, 1f));
            if (values[i].equalsIgnoreCase(savedValue == null ? "" : savedValue)) group.check(option.getId());
        }
        group.setOnCheckedChangeListener((g, checkedId) -> updateAttendanceStats());
        attendanceGroups.put(studentId, group);
        row.addView(group);
        attendanceList.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(230, 234, 242));
        attendanceList.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void saveAttendanceForDate() {
        if (attendanceGroups.isEmpty()) {
            Toast.makeText(this, "Aucun élève à enregistrer.", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, RadioGroup> entry : attendanceGroups.entrySet()) {
            int selectedId = entry.getValue().getCheckedRadioButtonId();
            if (selectedId == -1) continue;
            RadioButton selected = entry.getValue().findViewById(selectedId);
            if (selected != null) updates.put(entry.getKey(), selected.getTag().toString());
        }
        if (updates.isEmpty()) {
            Toast.makeText(this, "Sélectionnez au moins un statut de présence.", Toast.LENGTH_SHORT).show();
            return;
        }
        findViewById(R.id.saveAttendanceButton).setEnabled(false);
        FirebaseService.getInstance(getApplicationContext()).getDatabase()
                .getReference("attendance").child(selectedAttendanceDate)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    findViewById(R.id.saveAttendanceButton).setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Présences enregistrées.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Enregistrement refusé : vérifiez les autorisations Firebase.", Toast.LENGTH_LONG).show();
                    }
                    updateAttendanceStats();
                });
    }

    private void updateAttendanceStats() {
        int present = 0, absent = 0, late = 0;
        for (RadioGroup group : attendanceGroups.values()) {
            int id = group.getCheckedRadioButtonId();
            if (id == -1) continue;
            RadioButton selected = group.findViewById(id);
            if (selected == null) continue;
            String value = String.valueOf(selected.getTag());
            if ("P".equals(value)) present++;
            else if ("A".equals(value)) absent++;
            else if ("R".equals(value)) late++;
        }
        attendanceStats.setText("Présent : " + present + "  •  Absent : " + absent + "  •  Retard : " + late);
    }

    private boolean isAssignedToAnotherTeacher(DataSnapshot student) {
        String[] keys = {"teacherId", "teacherUid", "teacherUID", "assignedTeacherId",
                "assignedTeacher", "teacher"};
        Set<String> assignedValues = new HashSet<>();
        for (String key : keys) {
            DataSnapshot field = student.child(key);
            if (!field.exists()) continue;
            String raw = snapshotText(field);
            if (raw.isEmpty()) continue;
            for (String part : raw.split("[,;]")) {
                String candidate = normalize(part);
                if (!candidate.isEmpty()) assignedValues.add(candidate);
            }
        }
        if (assignedValues.isEmpty()) return false;
        String currentName = normalize(teacherName == null ? "" : teacherName.getText().toString());
        String currentEmail = normalize(userEmail());
        String currentUid = normalize(uid);
        return !assignedValues.contains(currentUid)
                && !assignedValues.contains(currentEmail)
                && (currentName.isEmpty() || !assignedValues.contains(currentName));
    }

    private String firstAssignedClass() {
        return assignedClasses.isEmpty() ? "" : assignedClasses.iterator().next();
    }

    private String branchFromClassSuffix(String className) {
        if (className == null) return "";
        String clean = className.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        String[] parts = clean.split("_");
        if (parts.length == 0) return "";
        String suffix = parts[parts.length - 1];
        if ("KA".equals(suffix) || suffix.contains("KACYIRU")) return "kacyiru";
        if ("GI".equals(suffix) || suffix.contains("GISOZI")) return "gisozi";
        if ("KI".equals(suffix) || suffix.contains("KIMISAGARA")) return "kimisagara";
        return "";
    }

    private String normalizeBranchForMatch(String branch) {
        if (branch == null || branch.trim().isEmpty()) return "";
        String clean = normalize(branch).replace('-', '_').replace(' ', '_');
        if (clean.contains("gisozi")) return "gisozi";
        if (clean.contains("kimisagara")) return "kimisagara";
        if (clean.contains("kacyiru")) return "kacyiru";
        return clean;
    }

    private String normalizeClassWithoutBranchSuffix(String className) {
        if (className == null) return "";
        String clean = className.trim().replaceAll("(?i)_(ka|gi|ki|kacyiru|gisozi|kimisagara)$", "");
        return normalize(clean);
    }

    private boolean containsClass(String studentClass) {
        String studentValue = normalizeClassWithoutBranchSuffix(studentClass);
        if (studentValue.isEmpty()) return false;
        for (String assigned : assignedClasses) {
            String teacherValue = normalizeClassWithoutBranchSuffix(assigned);
            if (teacherValue.isEmpty()) continue;
            // Reproduit le portail web : N3 accepte N3A, mais N3A n'accepte pas N3B.
            if (teacherValue.equals(studentValue)
                    || teacherValue.startsWith(studentValue)
                    || studentValue.startsWith(teacherValue)) return true;
        }
        return false;
    }

    private String snapshotText(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return "";
        // Les champs Firebase peuvent être une String, une liste ou un objet.
        // getValue(String.class) provoque une DatabaseException pour ArrayList/Map.
        Object raw = snapshot.getValue();
        return flattenSnapshotValue(raw);
    }

    private String flattenSnapshotValue(Object raw) {
        if (raw == null) return "";
        if (raw instanceof String || raw instanceof Number || raw instanceof Boolean) {
            return String.valueOf(raw).trim();
        }
        StringBuilder result = new StringBuilder();
        if (raw instanceof Iterable<?>) {
            for (Object item : (Iterable<?>) raw) {
                appendSnapshotPart(result, flattenSnapshotValue(item));
            }
        } else if (raw instanceof Map<?, ?>) {
            for (Object item : ((Map<?, ?>) raw).values()) {
                appendSnapshotPart(result, flattenSnapshotValue(item));
            }
        } else {
            return String.valueOf(raw).trim();
        }
        return result.toString();
    }

    private void appendSnapshotPart(StringBuilder result, String part) {
        if (part == null || part.trim().isEmpty()) return;
        if (result.length() > 0) result.append(",");
        result.append(part.trim());
    }

    private String userEmail() {
        FirebaseUser current = auth == null ? null : auth.getCurrentUser();
        return current == null ? "" : current.getEmail();
    }

    private void filterStudentRows(String query) {
        if (studentList == null) return;
        String needle = normalize(query);
        int visible = 0;
        for (int i = 0; i < studentList.getChildCount(); i++) {
            View child = studentList.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof String) {
                boolean match = needle.isEmpty() || normalize((String) tag).contains(needle);
                child.setVisibility(match ? View.VISIBLE : View.GONE);
                if (match) visible++;
            }
        }
        if (studentSearch != null && !needle.isEmpty()) {
            status.setText(visible + " résultat(s) pour « " + query + " »");
        } else if (accessibleStudents.size() > 0) {
            status.setText("Liste filtrée par branche et classes attribuées à votre compte.");
        }
    }


    private void showStudentDialog(String studentId) {
        final boolean editing = studentId != null;
        Map<String, Object> existing = editing ? studentRecords.get(studentId) : null;
        if (editing && existing == null) {
            Toast.makeText(this, "Fiche élève introuvable. Actualisez la liste.", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout form = column();
        form.setPadding(dp(20), dp(8), dp(20), dp(4));
        EditText firstName = formField("Prénom", existing == null ? "" : recordValue(existing, "firstName", "prenom"));
        EditText lastName = formField("Nom", existing == null ? "" : recordValue(existing, "lastName", "nom"));
        EditText parent = formField("Nom du parent", existing == null ? "" : recordValue(existing, "parentName", "parent"));
        EditText phone = formField("Téléphone du parent", existing == null ? "" : recordValue(existing, "phone", "phoneParent"));
        EditText dob = formField("Date de naissance (JJ/MM/AAAA)", existing == null ? "" : recordValue(existing, "dob"));
        EditText gender = formField("Sexe (M/F)", existing == null ? "" : recordValue(existing, "gender"));
        Spinner classSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new java.util.ArrayList<>(assignedClasses));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classSpinner.setAdapter(adapter);
        String oldClass = existing == null ? "" : recordValue(existing, "class", "classes", "classe");
        int selected = new java.util.ArrayList<>(assignedClasses).indexOf(oldClass);
        if (selected >= 0) classSpinner.setSelection(selected);
        TextView classLabel = text("Classe", 13, MUTED, true);
        classLabel.setPadding(0, dp(8), 0, dp(4));
        form.addView(classLabel);
        form.addView(classSpinner);

        form.addView(firstName, 0);
        form.addView(lastName, 1);
        form.addView(parent, 2);
        form.addView(phone, 3);
        form.addView(dob, 4);
        form.addView(gender, 5);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Modifier l'élève" : "Ajouter un élève")
                .setView(form)
                .setNegativeButton("Annuler", null)
                .setPositiveButton(editing ? "Enregistrer" : "Ajouter", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String fn = firstName.getText().toString().trim();
            String ln = lastName.getText().toString().trim();
            if (fn.isEmpty() || ln.isEmpty()) {
                Toast.makeText(this, "Le prénom et le nom sont obligatoires.", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedClass = classSpinner.getSelectedItem() == null ? "" : classSpinner.getSelectedItem().toString();
            if (selectedClass.isEmpty() || !assignedClasses.contains(selectedClass)) {
                Toast.makeText(this, "Choisissez une de vos classes assignées.", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("firstName", fn);
            data.put("lastName", ln);
            data.put("prenom", fn);
            data.put("nom", ln);
            data.put("name", (fn + " " + ln).trim());
            data.put("class", selectedClass);
            data.put("branch", teacherBranch);
            data.put("gender", gender.getText().toString().trim());
            data.put("dob", dob.getText().toString().trim());
            data.put("parentName", parent.getText().toString().trim());
            data.put("phone", phone.getText().toString().trim());
            data.put("status", "Actif");
            if (!editing) {
                data.put("createdAt", System.currentTimeMillis());
                data.put("teacherId", uid);
            } else {
                data.remove("status");
                data.remove("createdAt");
                data.remove("teacherId");
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            DatabaseReference target = editing ? studentsRef.child(studentId) : studentsRef.push();
            if (editing) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("firstName", fn); updates.put("lastName", ln);
                updates.put("prenom", fn); updates.put("nom", ln);
                updates.put("name", (fn + " " + ln).trim());
                updates.put("class", selectedClass); updates.put("branch", teacherBranch);
                updates.put("gender", gender.getText().toString().trim());
                updates.put("dob", dob.getText().toString().trim());
                updates.put("parentName", parent.getText().toString().trim());
                updates.put("phone", phone.getText().toString().trim());
                target.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        Toast.makeText(this, "Fiche élève mise à jour.", Toast.LENGTH_SHORT).show();
                        loadAssignedStudents();
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(this, "Modification refusée par Firebase.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                target.setValue(data).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        Toast.makeText(this, "Élève ajouté avec succès.", Toast.LENGTH_SHORT).show();
                        loadAssignedStudents();
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(this, "Ajout refusé par Firebase.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }));
        dialog.show();
    }

    private EditText formField(String hint, String value) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setText(value);
        field.setHint(hint);
        field.setTextSize(14);
        field.setPadding(dp(10), dp(8), dp(10), dp(8));
        return field;
    }

    private String recordValue(Map<String, Object> record, String... keys) {
        for (String key : keys) {
            Object value = record.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) return String.valueOf(value);
        }
        return "";
    }

    private void confirmDeleteStudent(String studentId, String studentName) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer l'élève")
                .setMessage("Voulez-vous vraiment supprimer " + studentName + " ? Cette action est irréversible.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer", (dialog, which) -> studentsRef.child(studentId).removeValue()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Élève supprimé.", Toast.LENGTH_SHORT).show();
                                loadAssignedStudents();
                            } else Toast.makeText(this, "Suppression refusée par Firebase.", Toast.LENGTH_LONG).show();
                        }))
                .show();
    }

    private void addStudentRow(String studentId, String name, String studentClass, String gender, String parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(8), dp(10), dp(8), dp(10));
        row.setTag(name + " " + studentClass + " " + (gender == null ? "" : gender) + " " + (parent == null ? "" : parent));
        row.addView(text(name, 15, TEXT, true));
        String detail = first(studentClass, "Classe non renseignée");
        if (gender != null && !gender.trim().isEmpty()) detail += " • " + gender;
        if (parent != null && !parent.trim().isEmpty()) detail += " • Parent : " + parent;
        row.addView(text(detail, 12, MUTED, false));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton edit = new MaterialButton(this);
        edit.setText("Modifier");
        edit.setAllCaps(false);
        edit.setOnClickListener(v -> showStudentDialog(studentId));
        MaterialButton delete = new MaterialButton(this);
        delete.setText("Supprimer");
        delete.setAllCaps(false);
        delete.setOnClickListener(v -> confirmDeleteStudent(studentId, name));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(42), 1f));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(42), 1f);
        deleteParams.setMarginStart(dp(8));
        actions.addView(delete, deleteParams);
        row.addView(actions);
        studentList.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(230, 234, 242));
        studentList.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private boolean isTeacherRole(String role) {
        String r = normalize(role);
        return "teacher".equals(r) || "enseignant".equals(r) || "enseignante".equals(r);
    }

    private String normalize(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return s.replaceAll("\\p{M}", "").replaceAll("[\\s_-]+", "");
    }

    private String value(DataSnapshot snap, String key) {
        String v = snap.child(key).getValue(String.class);
        return v == null ? "" : v.trim();
    }

    private String first(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private String join(Set<String> values) {
        StringBuilder b = new StringBuilder();
        for (String v : values) { if (b.length() > 0) b.append(", "); b.append(v); }
        return b.toString();
    }

    private void rejectWrongRole() {
        if (auth != null) auth.signOut();
        showFatal("Accès refusé : ce compte n'est pas enregistré comme enseignant.");
    }

    private void showFatal(String message) {
        if (status != null) status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (auth != null && auth.getCurrentUser() == null) {
            new android.os.Handler(getMainLooper()).postDelayed(this::returnToLogin, 1200);
        }
    }

    @Override protected void onDestroy() {
        if (heroCarouselTask != null) heroHandler.removeCallbacks(heroCarouselTask);
        super.onDestroy();
    }

    private void returnToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private MaterialCardView card() {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(BG);
        c.setRadius(dp(18));
        c.setCardElevation(dp(2));
        c.setStrokeWidth(dp(1));
        c.setStrokeColor(Color.rgb(227, 221, 201));
        return c;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color == BG || Color.alpha(color) == 0 ? Color.rgb(28, 35, 51) : color);
        if (interTypeface != null) t.setTypeface(interTypeface);
        if (bold) t.setTypeface(frauncesTypeface != null ? frauncesTypeface : Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
