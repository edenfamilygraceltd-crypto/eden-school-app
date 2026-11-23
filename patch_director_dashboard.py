from pathlib import Path
import shutil, datetime
p=Path(r"C:\Users\graph\Documents\eden_app\app\src\main\java\com\grace\eden_smart\DashboardActivity.java")
s=p.read_text(encoding="utf-8")
stamp=datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
shutil.copy2(p, Path(str(p)+f".before-director-dashboard-{stamp}.bak"))
s=s.replace("    private Runnable clockRunnable;", """    private Runnable clockRunnable;
    private TextView dashUsers, dashTeachers, dashStudents, dashRevenue, dashBranches, dashAttendance;""")
s=s.replace("        buildInterface();\n        startClock();", "        buildInterface();\n        loadDirectorDashboard();\n        startClock();")
s=s.replace("        hero.addView(heroInner);\n        page.addView(hero);\n\n        LinearLayout metrics", "        hero.addView(heroInner);\n        page.addView(hero);\n        buildDirectorOverview(page);\n\n        LinearLayout metrics")
marker="    private void loadTeachers() {"
methods=r'''
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

'''
if marker not in s: raise SystemExit("Insertion marker not found")
s=s.replace(marker,methods+marker,1)
old="TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(TEXT_PRIMARY);"
new="TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);"
if old not in s: raise SystemExit("Text helper marker not found")
s=s.replace(old,new,1)
p.write_text(s,encoding="utf-8")
print("Patched director dashboard; backup suffix:",stamp)
