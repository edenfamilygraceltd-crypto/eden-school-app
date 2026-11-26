package com.grace.eden_smart;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import com.grace.eden_smart.service.FirebaseService;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class DashboardActivity extends AppCompatActivity {
    private int NAVY, HERO_BG, BLUE, BG, GREEN, AMBER, RED, CARD_BG, TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, CARD_STROKE, SECONDARY_BUTTON_BG;

    private FirebaseService service;
    private DatabaseReference usersRef, attendanceRef;
    private FirebaseAuth auth;
    private TextView clockText, countdownText, sessionCountdownText, statusText, selectedTeacherText, lateSummaryText;
    private String selectedTeacherId, selectedTeacherName, selectedTeacherBranch, selectedTeacherCode;
    private String requestedAttendanceEvent = "arrival";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SharedPreferences prefs;
    private long sessionEndsAt;
    private boolean sessionExpired = false;
    private Runnable clockRunnable;
    private TextView dashUsers, dashTeachers, dashStudents, dashRevenue, dashBranches, dashAttendance;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean darkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        NAVY = darkMode ? Color.rgb(134, 165, 255) : Color.rgb(27, 44, 126);
        HERO_BG = darkMode ? Color.rgb(20, 31, 54) : Color.rgb(27, 44, 126);
        BLUE = darkMode ? Color.rgb(94, 126, 224) : Color.rgb(54, 86, 184);
        BG = darkMode ? Color.rgb(16, 24, 39) : Color.rgb(243, 246, 252);
        GREEN = Color.rgb(20, 135, 88);
        AMBER = Color.rgb(178, 104, 0);
        RED = Color.rgb(190, 40, 48);
        CARD_BG = darkMode ? Color.rgb(27, 38, 56) : Color.WHITE;
        // Une seule couleur de texte par thème : noir le jour, blanc la nuit.
        TEXT_PRIMARY = getColor(R.color.text_primary);
        TEXT_SECONDARY = TEXT_PRIMARY;
        TEXT_MUTED = TEXT_PRIMARY;
        CARD_STROKE = darkMode ? Color.rgb(57, 70, 92) : Color.rgb(226, 232, 240);
        SECONDARY_BUTTON_BG = darkMode ? Color.rgb(45, 58, 82) : Color.rgb(233, 238, 255);
        getWindow().setStatusBarColor(HERO_BG);
        getWindow().setNavigationBarColor(HERO_BG);
        prefs = getSharedPreferences("attendance_schedule", MODE_PRIVATE);
        sessionEndsAt = System.currentTimeMillis() + 60L * 60L * 1000L;
        try {
            service = FirebaseService.getInstance(getApplicationContext());
            auth = service.getAuth();
            usersRef = service.getUsersReference();
            attendanceRef = service.getTeacherAttendanceReference();
        } catch (Exception e) {
            Toast.makeText(this, "Firebase configuration needs to be completed.", Toast.LENGTH_LONG).show();
        }
        buildInterface();
        loadDirectorDashboard();
        startClock();
        AppUpdateChecker.check(this);
        if (auth == null || auth.getCurrentUser() == null) {
            openLogin();
        } else {
            verifyDirectorAccess();
        }
    }

    private void verifyDirectorAccess() {
        if (usersRef == null || auth == null || auth.getCurrentUser() == null) {
            openLogin();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot account) {
                String role = first(account.child("role").getValue(String.class),
                        account.child("userRole").getValue(String.class));
                String normalized = role == null ? "" : java.text.Normalizer
                        .normalize(role.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").replaceAll("[\\s_-]+", "");
                if (normalized.equals("director") || normalized.equals("directeur")
                        || normalized.equals("admin") || normalized.equals("administrateur")) {
                    loadLateSummary();
                    return;
                }
                if (normalized.equals("secretary") || normalized.equals("secretaire")) {
                    startActivity(new Intent(DashboardActivity.this, SecretaryPortalActivity.class));
                    finish();
                    return;
                }
                if (auth != null) auth.signOut();
                Toast.makeText(DashboardActivity.this, "Accès réservé à la direction.", Toast.LENGTH_LONG).show();
                openLogin();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (auth != null) auth.signOut();
                Toast.makeText(DashboardActivity.this, "Vérification des droits impossible.", Toast.LENGTH_LONG).show();
                openLogin();
            }
        });
    }

    private void buildInterface() {
        LinearLayout appRoot = new LinearLayout(this);
        appRoot.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(34), dp(18), dp(26));
        scroll.addView(page);
        appRoot.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(8), dp(6), dp(8), dp(6));
        bottomNav.setBackgroundColor(CARD_BG);
        bottomNav.setElevation(dp(8));
        Button navAttendance = smallButton("Attendance");
        navAttendance.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_my_calendar, 0, 0, 0);
        navAttendance.setOnClickListener(v -> { scroll.smoothScrollTo(0, 0); bounce(v); });
        Button navMessages = smallButton("Messages");
        navMessages.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_email, 0, 0, 0);
        navMessages.setOnClickListener(v -> { showFeed("messages", "Messages"); bounce(v); });
        Button navNotifications = smallButton("Notifications");
        navNotifications.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
        navNotifications.setOnClickListener(v -> { showFeed("notifications", "Notifications"); bounce(v); });
        Button navUpdates = smallButton("Updates");
        navUpdates.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_popup_sync, 0, 0, 0);
        navUpdates.setOnClickListener(v -> { showFeed("updates", "Updates"); bounce(v); });
        bottomNav.addView(navAttendance, new LinearLayout.LayoutParams(0, -2, 1f));
        bottomNav.addView(navMessages, new LinearLayout.LayoutParams(0, -2, 1f));
        bottomNav.addView(navNotifications, new LinearLayout.LayoutParams(0, -2, 1f));
        bottomNav.addView(navUpdates, new LinearLayout.LayoutParams(0, -2, 1f));
        appRoot.addView(bottomNav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(appRoot);
        KeyboardInsets.apply(this);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = new TextView(this);
        brand.setText("EDEN FAMILY SCHOOL");
        brand.setTextColor(TEXT_PRIMARY);
        brand.setTextSize(20);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button bell = smallButton("Notifications");
        bell.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_info, 0, 0, 0);
        bell.setContentDescription("Notifications");
        bell.setOnClickListener(v -> showFeed("notifications", "Notifications"));
        top.addView(bell);
        Button messages = smallButton("Messages");
        messages.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_email, 0, 0, 0);
        messages.setContentDescription("Messages");
        messages.setOnClickListener(v -> showFeed("messages", "Messages"));
        top.addView(messages);
        Button topSettings = smallButton("Settings");
        topSettings.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_preferences, 0, 0, 0);
        topSettings.setContentDescription("Settings");
        topSettings.setOnClickListener(v -> showSettings());
        top.addView(topSettings);
        page.addView(top);

        TextView subtitle = text("Teacher Attendance - Smart school portal", 13, TEXT_SECONDARY, false);
        subtitle.setPadding(0, dp(5), 0, dp(16));
        page.addView(subtitle);

        MaterialCardView hero = card(HERO_BG);
        LinearLayout heroInner = column();
        heroInner.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView greeting = text("Welcome back", 24, ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) ? Color.rgb(201, 162, 39) : Color.WHITE, true);
        heroInner.addView(greeting);
        FirebaseUser current = auth == null ? null : auth.getCurrentUser();
        TextView who = text(current == null ? "School administration" :
                (current.getDisplayName() == null ? current.getEmail() : current.getDisplayName()), 14, 0xFFDCE6FF, false);
        who.setPadding(0, dp(6), 0, dp(15));
        heroInner.addView(who);
        clockText = text("--:--:--", 32, ((getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) ? Color.rgb(201, 162, 39) : Color.WHITE, true);
        heroInner.addView(clockText);
        countdownText = text("Loading attendance time…", 14, 0xFFDCE6FF, false);
        countdownText.setPadding(0, dp(4), 0, 0);
        heroInner.addView(countdownText);
        sessionCountdownText = text("Session expires in 01:00:00", 13, 0xFFFFE6A8, true);
        sessionCountdownText.setPadding(0, dp(8), 0, 0);
        heroInner.addView(sessionCountdownText);
        hero.addView(heroInner);
        page.addView(hero);
        buildDirectorOverview(page);

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.setPadding(0, dp(14), 0, dp(12));
        MaterialCardView arrival = card(CARD_BG);
        LinearLayout arrivalBox = column();
        arrivalBox.setPadding(dp(14), dp(15), dp(14), dp(15));
        arrivalBox.addView(text("ARRIVAL", 13, GREEN, true));
        arrivalBox.addView(text("Scan to check in", 14, NAVY, true));
        arrival.addView(arrivalBox);
        metrics.addView(arrival, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialCardView departure = card(CARD_BG);
        LinearLayout departureBox = column();
        departureBox.setPadding(dp(14), dp(15), dp(14), dp(15));
        departureBox.addView(text("DEPARTURE", 13, BLUE, true));
        departureBox.addView(text("Scan to check out", 14, NAVY, true));
        departure.addView(departureBox);
        LinearLayout.LayoutParams depLp = new LinearLayout.LayoutParams(0, -2, 1);
        depLp.setMargins(dp(10), 0, 0, 0);
        metrics.addView(departure, depLp);
        arrival.setClickable(true);
        arrival.setOnClickListener(v -> { requestedAttendanceEvent = "arrival"; beginBiometricAttendance(); });
        arrival.setContentDescription("Record teacher arrival");
        departure.setClickable(true);
        departure.setOnClickListener(v -> { requestedAttendanceEvent = "departure"; beginBiometricAttendance(); });
        departure.setContentDescription("Record teacher departure");
        page.addView(metrics);

        MaterialCardView selectCard = card(CARD_BG);
        LinearLayout selectBox = column();
        selectBox.setPadding(dp(18), dp(18), dp(18), dp(18));
        selectBox.addView(text("Teacher attendance", 19, NAVY, true));
        selectBox.addView(text("Choose an assigned teacher. The school branch is taken from the teacher record.", 13, TEXT_SECONDARY, false));
        selectedTeacherText = text("No teacher selected", 15, NAVY, true);
        selectedTeacherText.setPadding(0, dp(12), 0, dp(10));
        selectBox.addView(selectedTeacherText);
        Button choose = primaryButton("Scan / register teacher");
        choose.setOnClickListener(v -> loadTeachers());
        selectBox.addView(choose, matchWrap());
        Button scan = primaryButton("Record selected arrival / departure");
        LinearLayout.LayoutParams scanLp = matchWrap();
        scanLp.topMargin = dp(10);
        scan.setOnClickListener(v -> beginBiometricAttendance());
        selectBox.addView(scan, scanLp);
        statusText = text("Ready when you are.", 14, GREEN, true);
        statusText.setPadding(0, dp(12), 0, 0);
        selectBox.addView(statusText);
        selectCard.addView(selectBox);
        LinearLayout.LayoutParams selectLp = matchWrap();
        selectLp.topMargin = dp(4);
        page.addView(selectCard, selectLp);

        MaterialCardView schedule = card(CARD_BG);
        LinearLayout scheduleBox = column();
        scheduleBox.setPadding(dp(18), dp(18), dp(18), dp(18));
        scheduleBox.addView(text("Attendance schedule", 18, NAVY, true));
        scheduleBox.addView(text("Set arrival and departure times for late-status calculation.", 13, TEXT_SECONDARY, false));
        Button setArrival = secondaryButton("Arrival time: " + prefs.getString("arrival", "08:00"));
        setArrival.setOnClickListener(v -> chooseTime("arrival", "Arrival time", setArrival));
        LinearLayout.LayoutParams timeLp = matchWrap(); timeLp.topMargin = dp(12);
        scheduleBox.addView(setArrival, timeLp);
        Button setDeparture = secondaryButton("Departure time: " + prefs.getString("departure", "16:00"));
        setDeparture.setOnClickListener(v -> chooseTime("departure", "Departure time", setDeparture));
        LinearLayout.LayoutParams timeLp2 = matchWrap(); timeLp2.topMargin = dp(8);
        scheduleBox.addView(setDeparture, timeLp2);
        lateSummaryText = text("Weekly lateness: loading…", 14, NAVY, true);
        lateSummaryText.setPadding(0, dp(12), 0, 0);
        scheduleBox.addView(lateSummaryText);
        schedule.addView(scheduleBox);
        LinearLayout.LayoutParams scheduleLp = matchWrap(); scheduleLp.topMargin = dp(14);
        page.addView(schedule, scheduleLp);

        Button settings = secondaryButton("Settings - biometrics and teacher code");
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams settingsLp = matchWrap(); settingsLp.topMargin = dp(10);
        page.addView(settings, settingsLp);

        Button signOut = secondaryButton("Sign out");
        signOut.setOnClickListener(v -> {
            if (auth != null) auth.signOut();
            openLogin();
        });
        LinearLayout.LayoutParams signLp = matchWrap(); signLp.topMargin = dp(16);
        page.addView(signOut, signLp);
        TextView note = text("Eden Smart - attendance records sync to the school database.", 11, TEXT_MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(16), 0, 0);
        page.addView(note);
    }


    private void buildDirectorOverview(LinearLayout page) {
        MaterialCardView overview = card(CARD_BG);
        LinearLayout box = column();
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("School overview", 19, NAVY, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Button refresh = secondaryButton("Refresh");
        refresh.setTextSize(12);
        refresh.setOnClickListener(v -> loadDirectorDashboard());
        heading.addView(refresh);
        box.addView(heading);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        dashUsers = dashboardMetric("USERS", "—", BLUE);
        dashTeachers = dashboardMetric("TEACHERS", "—", GREEN);
        row1.addView(dashboardMetricCard("Users", dashUsers, CARD_BG), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(0, -2, 1f);
        gap.leftMargin = dp(8);
        row1.addView(dashboardMetricCard("Teachers", dashTeachers, CARD_BG), gap);
        LinearLayout.LayoutParams rowLp = matchWrap(); rowLp.topMargin = dp(12);
        box.addView(row1, rowLp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        dashStudents = dashboardMetric("STUDENTS", "—", NAVY);
        dashRevenue = dashboardMetric("PAID", "—", GREEN);
        row2.addView(dashboardMetricCard("Students", dashStudents, CARD_BG), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams gap2 = new LinearLayout.LayoutParams(0, -2, 1f);
        gap2.leftMargin = dp(8);
        row2.addView(dashboardMetricCard("Revenue collected", dashRevenue, CARD_BG), gap2);
        LinearLayout.LayoutParams rowLp2 = matchWrap(); rowLp2.topMargin = dp(8);
        box.addView(row2, rowLp2);

        dashBranches = text("Students by branch: loading…", 13, TEXT_SECONDARY, false);
        dashBranches.setPadding(0, dp(14), 0, dp(8));
        box.addView(dashBranches);
        dashAttendance = text("Today's teacher attendance: loading…", 13, TEXT_SECONDARY, true);
        dashAttendance.setPadding(0, dp(6), 0, 0);
        box.addView(dashAttendance);
        overview.addView(box);
        LinearLayout.LayoutParams lp = matchWrap(); lp.topMargin = dp(14); lp.bottomMargin = dp(8);
        page.addView(overview, lp);
    }

    private TextView dashboardMetric(String label, String initial, int color) {
        TextView value = text(initial, 23, color, true);
        value.setContentDescription(label);
        return value;
    }

    private MaterialCardView dashboardMetricCard(String label, TextView value, int background) {
        MaterialCardView c = card(background);
        LinearLayout content = column();
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(text(label, 11, TEXT_SECONDARY, true));
        content.addView(value);
        c.addView(content);
        return c;
    }

    private void loadDirectorDashboard() {
        if (service == null || service.getDatabase() == null) return;
        service.getDatabase().getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                int users = 0, teachers = 0;
                if (snap.exists()) for (DataSnapshot user : snap.getChildren()) {
                    users++;
                    String role = first(user.child("role").getValue(String.class),
                            user.child("userRole").getValue(String.class));
                    if (role != null && role.trim().equalsIgnoreCase("teacher")) teachers++;
                }
                if (dashUsers != null) dashUsers.setText(String.valueOf(users));
                if (dashTeachers != null) dashTeachers.setText(String.valueOf(teachers));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (dashUsers != null) dashUsers.setText("Unavailable");
            }
        });

        service.getDatabase().getReference("students").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                int total = 0;
                Map<String, Integer> branches = new java.util.LinkedHashMap<>();
                branches.put("Kacyiru", 0); branches.put("Kimisagara", 0);
                branches.put("Gisozi Maternelle", 0); branches.put("Gisozi Primaire", 0);
                for (DataSnapshot student : snap.getChildren()) {
                    total++;
                    String branch = first(student.child("branch").getValue(String.class),
                            student.child("schoolBranch").getValue(String.class));
                    if (branch != null) {
                        String key = branch.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
                        if (key.contains("kacyiru")) branches.put("Kacyiru", branches.get("Kacyiru") + 1);
                        else if (key.contains("kimisagara")) branches.put("Kimisagara", branches.get("Kimisagara") + 1);
                        else if (key.contains("gisozi") && (key.contains("mater") || key.contains("nursery"))) branches.put("Gisozi Maternelle", branches.get("Gisozi Maternelle") + 1);
                        else if (key.contains("gisozi")) branches.put("Gisozi Primaire", branches.get("Gisozi Primaire") + 1);
                    }
                }
                if (dashStudents != null) dashStudents.setText(String.valueOf(total));
                if (dashBranches != null) dashBranches.setText("Students by branch  •  Kacyiru: " + branches.get("Kacyiru")
                        + "  •  Kimisagara: " + branches.get("Kimisagara") + "\nGisozi Maternelle: "
                        + branches.get("Gisozi Maternelle") + "  •  Gisozi Primaire: " + branches.get("Gisozi Primaire"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (dashStudents != null) dashStudents.setText("Unavailable");
            }
        });

        service.getDatabase().getReference("payments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                long revenue = 0;
                for (DataSnapshot payment : snap.getChildren()) {
                    String status = payment.child("status").getValue(String.class);
                    Object raw = payment.child("amount").getValue();
                    if (status != null && status.equalsIgnoreCase("paid") && raw != null) {
                        try { revenue += raw instanceof Number ? ((Number) raw).longValue()
                                : Long.parseLong(String.valueOf(raw).replace(",", "").trim()); }
                        catch (NumberFormatException ignored) { }
                    }
                }
                if (dashRevenue != null) dashRevenue.setText(String.format(Locale.US, "%,d RWF", revenue));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (dashRevenue != null) dashRevenue.setText("Unavailable");
            }
        });

        if (attendanceRef != null) {
            String today = dayFormat.format(new Date());
            attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                    int arrived = 0, departed = 0;
                    for (DataSnapshot teacher : snap.getChildren()) {
                        DataSnapshot day = teacher.child(today);
                        if (day.child("arrival").exists()) arrived++;
                        if (day.child("departure").exists()) departed++;
                    }
                    if (dashAttendance != null) dashAttendance.setText("Today's teacher attendance  •  Arrivals: "
                            + arrived + "  •  Departures: " + departed);
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {
                    if (dashAttendance != null) dashAttendance.setText("Today's teacher attendance: unavailable");
                }
            });
        }
    }

    private void loadTeachers() {
        if (usersRef == null || auth == null || auth.getCurrentUser() == null) {
            message("Firebase is not configured or you are not signed in.");
            return;
        }
        statusText.setText("Loading teachers for your school account...");
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot account) {
                String accountRole = first(account.child("role").getValue(String.class), account.child("userRole").getValue(String.class));
                String normalizedRole = accountRole == null ? "" : accountRole.trim().toLowerCase(Locale.ROOT);
                boolean secretary = normalizedRole.contains("secretary") || normalizedRole.contains("secretaire") || normalizedRole.contains("secrétaire");
                String accountBranch = first(account.child("branch").getValue(String.class), account.child("branche").getValue(String.class),
                        account.child("branchName").getValue(String.class), account.child("schoolBranch").getValue(String.class));
                if (secretary && accountBranch == null) {
                    statusText.setText("Your secretary account has no assigned branch. Ask the school administrator to assign one.");
                    message("The branch is taken from your secretary account. No branch will be selected manually.");
                    return;
                }
                usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> labels = new ArrayList<>();
                        ArrayList<String> ids = new ArrayList<>();
                        ArrayList<String> names = new ArrayList<>();
                        ArrayList<String> branches = new ArrayList<>();
                        ArrayList<String> codes = new ArrayList<>();
                        for (DataSnapshot item : snapshot.getChildren()) {
                            String role = first(item.child("role").getValue(String.class), item.child("userRole").getValue(String.class));
                            if (role == null) continue;
                            String normalized = role.trim().toLowerCase(Locale.ROOT);
                            if (!normalized.equals("teacher") && !normalized.equals("enseignant") && !normalized.equals("enseignante")) continue;
                            String branch = first(item.child("branch").getValue(String.class), item.child("branche").getValue(String.class),
                                    item.child("branchName").getValue(String.class), item.child("schoolBranch").getValue(String.class));
                            String status = first(item.child("status").getValue(String.class), "active");
                            if (branch == null || !status.equalsIgnoreCase("active")) continue;
                            if (secretary && !branch.trim().equalsIgnoreCase(accountBranch.trim())) continue;
                            String name = first(item.child("nom").getValue(String.class), item.child("name").getValue(String.class),
                                    item.child("displayName").getValue(String.class), item.child("email").getValue(String.class));
                            if (name == null) name = item.getKey();
                            // Create the attendance code once when a teacher record is first used.
                            // The stable Firebase account UID is the canonical code, so it cannot drift from the account.
                            String code = first(item.child("teacherCode").getValue(String.class), item.getKey());
                            if (!item.child("teacherCode").exists() || item.child("teacherCode").getValue(String.class) == null
                                    || item.child("teacherCode").getValue(String.class).trim().isEmpty()) {
                                Map<String, Object> codeFields = new HashMap<>();
                                codeFields.put("teacherCode", code);
                                codeFields.put("teacherCodeCreatedAt", System.currentTimeMillis());
                                codeFields.put("teacherCodeMethod", "firebase_account_uid");
                                usersRef.child(item.getKey()).updateChildren(codeFields);
                            }
                            ids.add(item.getKey());
                            names.add(name);
                            branches.add(branch.trim());
                            codes.add(code);
                            labels.add(name + "  |  " + branch.trim());
                        }
                        if (ids.isEmpty()) {
                            statusText.setText(secretary ? "No active teachers are assigned to " + accountBranch + "." : "No active teachers with an assigned branch were found.");
                            return;
                        }
                        String title = secretary ? "Teachers - " + accountBranch : "Select assigned teacher";
                        new androidx.appcompat.app.AlertDialog.Builder(DashboardActivity.this)
                                .setTitle(title)
                                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                                    selectedTeacherId = ids.get(which);
                                    selectedTeacherName = names.get(which);
                                    selectedTeacherBranch = branches.get(which);
                                    selectedTeacherCode = codes.get(which);
                                    selectedTeacherText.setText(selectedTeacherName + "  |  " + selectedTeacherBranch + "\nTeacher code: " + selectedTeacherCode);
                                    statusText.setText("Selected: " + selectedTeacherName + ". The teacher account ID is used as the attendance code. Choose ARRIVAL or DEPARTURE.");
                                    loadLateSummary();
                                }).setNegativeButton("Cancel", null).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        statusText.setText("Could not load teachers: " + error.getMessage());
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("Could not read your school account: " + error.getMessage());
            }
        });
    }

    private void beginBiometricAttendance() {
        if (selectedTeacherId == null) {
            message("First select or register the assigned teacher.");
            return;
        }
        if (selectedTeacherCode == null || selectedTeacherCode.trim().isEmpty()) {
            message("The selected teacher account ID is missing. Check the teacher account in the school website.");
            return;
        }
        android.widget.EditText codeInput = new android.widget.EditText(this);
        codeInput.setSingleLine(true);
        codeInput.setHint("Enter the teacher attendance code");
        codeInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = dp(18);
        codeInput.setPadding(pad, pad, pad, pad);
        androidx.appcompat.app.AlertDialog codeDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Verify teacher account ID")
                .setMessage("Enter the unique attendance code registered for " + selectedTeacherName + ".")
                .setView(codeInput)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Continue to biometric", null)
                .create();
        codeDialog.setOnShowListener(ignored -> codeDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = codeInput.getText() == null ? "" : codeInput.getText().toString().trim();
            if (!entered.equalsIgnoreCase(selectedTeacherCode.trim())) {
                codeInput.setError("Incorrect teacher code");
                statusText.setText("Teacher code does not match. Attendance was not recorded.");
                statusText.setTextColor(RED);
                return;
            }
            codeDialog.dismiss();
            if (prefs.getBoolean("require_biometric", true)) showBiometricPrompt();
            else saveAttendanceEvent(requestedAttendanceEvent);
        }));
        codeDialog.show();
    }

    private void showBiometricPrompt() {
        BiometricManager manager = BiometricManager.from(this);
        int result = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            message("Biometric authentication is unavailable or not enrolled. Android's standard biometric prompt cannot identify which teacher's finger was used.");
            return;
        }
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult authResult) {
                super.onAuthenticationSucceeded(authResult);
                saveAttendanceEvent(requestedAttendanceEvent);
            }
            @Override public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                statusText.setText("Biometric cancelled or failed: " + errString);
            }
            @Override public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                statusText.setText("Biometric not recognised. Try again.");
            }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Eden teacher attendance")
                .setSubtitle(("arrival".equals(requestedAttendanceEvent) ? "Arrival" : "Departure") + " for " + selectedTeacherName)
                .setDescription("Teacher code plus device biometric check. Android cannot identify a teacher-specific fingerprint.")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();
        prompt.authenticate(info);
    }
    private void saveAttendanceEvent(String eventKey) {
        if (attendanceRef == null || selectedTeacherId == null) { message("Firebase is not ready."); return; }
        String date = dayFormat.format(new Date());
        String now = timeFormat.format(new Date());
        DatabaseReference dayRef = attendanceRef.child(selectedTeacherId).child(date);
        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                if ("departure".equals(eventKey) && !snapshot.child("arrival").exists()) {
                    statusText.setText("Record arrival before recording departure.");
                    statusText.setTextColor(RED);
                    return;
                }
                if ("arrival".equals(eventKey) && snapshot.child("arrival").exists()) {
                    statusText.setText("Arrival is already recorded for today.");
                    statusText.setTextColor(AMBER);
                    return;
                }
                if ("departure".equals(eventKey) && snapshot.child("departure").exists()) {
                    statusText.setText("Both arrival and departure are already recorded for today.");
                    return;
                }
                Map<String, Object> event = new HashMap<>();
                event.put("time", now);
                event.put("timestamp", System.currentTimeMillis());
                event.put("verifiedByDeviceBiometric", prefs.getBoolean("require_biometric", true));
                event.put("recordedByUid", auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "");
                event.put("recordedByRole", "director_or_secretary");
                String arrivalTime = prefs.getString("arrival", "08:00");
                boolean late = "arrival".equals(eventKey) && now.compareTo(arrivalTime + ":00") > 0;
                event.put("lateStatus", late ? "late" : "on_time");
                event.put("teacherName", selectedTeacherName);
                event.put("teacherCode", selectedTeacherCode);
                event.put("branch", selectedTeacherBranch == null ? "" : selectedTeacherBranch);
                event.put("source", "efs_app");
                event.put("signatureText", "Scann\u00e9 par EFS APP");
                event.put("signatureStatus", prefs.getBoolean("require_biometric", true) ? "verified_by_device_biometric" : "code_only");
                dayRef.child(eventKey).setValue(event).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        statusText.setText("Could not save attendance. Check Firebase rules.");
                        return;
                    }
                    String greeting = "arrival".equals(eventKey) ? "Welcome, " : "Goodbye, ";
                    statusText.setText((late ? "LATE • " : "ON TIME • ") + greeting + selectedTeacherName + " • " + now);
                    statusText.setTextColor(late ? AMBER : GREEN);
                    statusText.setScaleX(0.75f); statusText.setScaleY(0.75f);
                    statusText.animate().scaleX(1f).scaleY(1f).setDuration(420).setInterpolator(new OvershootInterpolator(1.7f)).start();
                    loadLateSummary();
                    Toast.makeText(DashboardActivity.this, greeting + selectedTeacherName, Toast.LENGTH_LONG).show();
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("Could not read today's attendance: " + error.getMessage());
            }
        });
    }

    private void showSettings() {
        LinearLayout box = column();
        box.setPadding(dp(12), dp(8), dp(12), dp(4));
        android.widget.CheckBox biometric = new android.widget.CheckBox(this);
        biometric.setText("Require this phone's biometric confirmation");
        biometric.setTextColor(TEXT_PRIMARY);
        biometric.setChecked(prefs.getBoolean("require_biometric", true));
        box.addView(biometric);

        TextView note = text("Each teacher receives one stable attendance code when the account is first registered in Eden Smart. If the website has not stored a code yet, the app saves the teacher account ID as teacherCode in Firebase. Fingerprints are managed by Android; the app never reads or stores fingerprint images and cannot identify which person's enrolled finger was used.", 13, TEXT_SECONDARY, false);
        note.setPadding(0, dp(8), 0, dp(12));
        box.addView(note);

        Button copyId = secondaryButton("Copy selected teacher account ID");
        copyId.setOnClickListener(v -> {
            if (selectedTeacherId == null || selectedTeacherId.trim().isEmpty()) {
                message("Select a teacher first.");
                return;
            }
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Teacher account ID", selectedTeacherId));
                message("Teacher account ID copied. This ID is the attendance code.");
            }
        });
        box.addView(copyId, matchWrap());

        Button biometricSettings = secondaryButton("Open Android security / fingerprint settings");
        LinearLayout.LayoutParams systemLp = matchWrap();
        systemLp.topMargin = dp(8);
        biometricSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
            } catch (Exception e) {
                message("Open your phone Settings and choose Security or Fingerprint.");
            }
        });
        box.addView(biometricSettings, systemLp);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(box)
                .setNegativeButton("Close", null)
                .setPositiveButton("Save settings", (dialog, which) -> {
                    prefs.edit().putBoolean("require_biometric", biometric.isChecked()).apply();
                    message("Settings saved. Biometric confirmation " + (biometric.isChecked() ? "is required." : "is disabled."));
                }).show();
    }

    private void loadLateSummary() {
        if (attendanceRef == null || lateSummaryText == null) return;
        attendanceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot root) {
                if (selectedTeacherId == null) {
                    lateSummaryText.setText("Select a teacher to review weekly lateness.");
                    return;
                }
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                String start = dayFormat.format(cal.getTime());
                String today = dayFormat.format(new Date());
                int count = 0;
                DataSnapshot teacher = root.child(selectedTeacherId);
                for (DataSnapshot day : teacher.getChildren()) {
                    String key = day.getKey();
                    if (key != null && key.compareTo(start) >= 0 && key.compareTo(today) <= 0
                            && "late".equalsIgnoreCase(day.child("arrival").child("lateStatus").getValue(String.class))) count++;
                }
                lateSummaryText.setText("This week: " + count + " late arrival(s). " + (count > 2 ? "Repeated lateness: RED alert." : count > 0 ? "Late status: YELLOW." : "No lateness recorded."));
                lateSummaryText.setTextColor(count > 2 ? RED : count > 0 ? AMBER : GREEN);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                lateSummaryText.setText("Weekly lateness could not be loaded.");
            }
        });
    }

    private void showFeed(String path, String title) {
        if (service == null) { message("Firebase is not configured."); return; }
        service.getDatabase().getReference(path).limitToLast(50).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder content = new StringBuilder();
                String currentUid = selectedTeacherId != null ? selectedTeacherId
                        : (auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "");
                for (DataSnapshot item : snapshot.getChildren()) {
                    String recipient = first(item.child("to").getValue(String.class), item.child("teacherId").getValue(String.class),
                            item.child("recipientId").getValue(String.class), item.child("userId").getValue(String.class));
                    if ("notifications".equals(path) && recipient != null && !recipient.isEmpty()
                            && !recipient.equals(currentUid)) continue;
                    String titleText = first(item.child("title").getValue(String.class), item.child("message").getValue(String.class), item.getValue(String.class));
                    String body = first(item.child("body").getValue(String.class), item.child("description").getValue(String.class), item.child("content").getValue(String.class));
                    if (titleText == null && body == null) continue;
                    content.append("• ").append(titleText == null ? title : titleText);
                    if (body != null && !body.equals(titleText)) content.append("\n").append(body);
                    String branch = item.child("branch").getValue(String.class);
                    if (branch != null && !branch.trim().isEmpty()) content.append("\nBranch: ").append(branch);
                    content.append("\n\n");
                }
                String empty = "updates".equals(path)
                        ? "No app updates have been published yet. This section will show new versions and change notes."
                        : "There are no " + title.toLowerCase(Locale.ROOT) + " to show yet.";
                message(content.length() == 0 ? empty : content.toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { message("Could not load " + title + ": " + error.getMessage()); }
        });
    }

    private void chooseTime(String key, String title, Button button) {
        String value = prefs.getString(key, "arrival".equals(key) ? "08:00" : "16:00");
        String[] parts = value.split(":");
        new TimePickerDialog(this, (view, hour, minute) -> {
            String formatted = String.format(Locale.US, "%02d:%02d", hour, minute);
            prefs.edit().putString(key, formatted).apply();
            button.setText(("arrival".equals(key) ? "⏰  Arrival time: " : "🏁  Departure time: ") + formatted);
            updateCountdown();
        }, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), true).show();
    }

    private void startClock() {
        clockRunnable = new Runnable() {
            @Override public void run() {
                if (clockText != null) clockText.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                updateCountdown();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockRunnable);
    }

    private void updateCountdown() {
        if (countdownText == null) return;
        Calendar now = Calendar.getInstance();
        String arrival = prefs.getString("arrival", "08:00");
        String[] parts = arrival.split(":");
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        target.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        target.set(Calendar.SECOND, 0);
        long diff = target.getTimeInMillis() - now.getTimeInMillis();
        if (diff > 0) {
            long h = diff / 3600000, m = (diff / 60000) % 60, s = (diff / 1000) % 60;
            countdownText.setText(String.format(Locale.getDefault(), "Arrival starts in %02d:%02d:%02d", h, m, s));
        } else {
            countdownText.setText("Arrival time reached • scheduled arrival " + arrival);
        }
    }

    private void updateSessionCountdown() {
        if (sessionCountdownText == null || sessionExpired) return;
        long remaining = sessionEndsAt - System.currentTimeMillis();
        if (remaining <= 0) {
            sessionExpired = true;
            sessionCountdownText.setText("Session expired • sign in again");
            if (auth != null) auth.signOut();
            Toast.makeText(this, "One hour has passed. Please sign in again.", Toast.LENGTH_LONG).show();
            openLogin();
            return;
        }
        long hours = remaining / 3600000L;
        long minutes = (remaining / 60000L) % 60L;
        long seconds = (remaining / 1000L) % 60L;
        sessionCountdownText.setText(String.format(Locale.getDefault(),
                "🔐 Session timeout in %02d:%02d:%02d", hours, minutes, seconds));
    }
    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void message(String msg) {
        new androidx.appcompat.app.AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK", null).show();
    }
    private String first(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return null;
    }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private MaterialCardView card(int color) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(color); c.setRadius(dp(22)); c.setCardElevation(dp(3));
        c.setStrokeWidth(dp(1)); c.setStrokeColor(CARD_STROKE); return c;
    }
    private Button primaryButton(String value) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextColor(TEXT_PRIMARY);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE)); addBounce(b); return b;
    }
    private Button secondaryButton(String value) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextColor(TEXT_PRIMARY);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(SECONDARY_BUTTON_BG)); addBounce(b); return b;
    }
    private Button smallButton(String value) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextSize(16);
        b.setMinWidth(dp(42)); b.setPadding(dp(4), 0, dp(4), 0);
        b.setTextColor(TEXT_PRIMARY); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(CARD_BG)); addBounce(b); return b;
    }
    private void addBounce(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(90).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new OvershootInterpolator(2f)).start();
                    break;
            }
            return false;
        });
    }

    private void bounce(View view) {
        view.setScaleX(0.92f); view.setScaleY(0.92f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new OvershootInterpolator(2f)).start();
    }
    @Override protected void onDestroy() {
        if (clockRunnable != null) handler.removeCallbacks(clockRunnable);
        super.onDestroy();
    }
}
