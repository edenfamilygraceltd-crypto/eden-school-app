package com.grace.eden_smart;

import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.grace.eden_smart.service.FirebaseService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AccountantPortalActivity extends AppCompatActivity {
    private static final int GREEN = Color.rgb(46,139,87);
    private static final int DEEP = Color.rgb(30,107,78);
    private static final int INK = Color.rgb(47,79,79);
    private static final int BG = Color.rgb(244,248,245);

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference usersRef;
    private LinearLayout content, navigation, branchBar;
    private View navigationScroll, accountantLogo;
    private TextView pageTitle, userLabel, languageView;
    private boolean navigationExpanded = true;
    private String filterQuery = "";
    private String language = "fr", userName = "Comptable", userRole = "", selectedBranch = "kacyiru";
    private final List<String> allowedBranches = new ArrayList<>();
    private final Map<String,List<Map<String,Object>>> data = new LinkedHashMap<>();
    private final String[][] modules = {
        {"dashboard","⌂","Tableau de bord","Dashboard"},
        {"announcements","▣","Annonces","Announcements"},
        {"teachers","♙","Personnel","Workers"},
        {"payroll","₣","Salaires","Payroll"},
        {"student-fees","▣","Paiements","Payments"},
        {"fees","＋","Frais scolaires","School fees"},
        {"coaching","▤","Coaching","Coaching"},
        {"transport","▰","Transport","Transport"},
        {"registration","✚","Inscriptions","Registration"},
        {"students","♧","Élèves","Students"},
        {"reports","▤","Rapports financiers","Financial reports"},
        {"expenses","−","Dépenses","Expenses"},
        {"settings","⚙","Paramètres","Settings"},
        {"trash","⌫","Corbeille","Trash"}
    };
    private String active = "dashboard";
    private static final int REQUEST_ACCOUNTANT_EXPORT = 8201;
    private static final int REQUEST_ACCOUNTANT_PDF = 8202;
    private byte[] pendingExportBytes;
    private String pendingExportMime = "text/csv";
    private String reportMode = "daily";
    private String reportDate = "";
    private String activePayrollKey = "";
    private String paymentClassFilter = "", paymentMonthFilter = "", paymentDateFrom = "", paymentDateTo = "", paymentTypeFilter = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(DEEP);
        getWindow().setNavigationBarColor(DEEP);
        FirebaseService service = FirebaseService.getInstance(getApplicationContext());
        auth = service.getAuth();
        database = service.getDatabase();
        usersRef = service.getUsersReference();
        if (auth.getCurrentUser() == null) { finish(); return; }
        setContentView(R.layout.activity_accountant_portal);
        KeyboardInsets.apply(this);
        content = findViewById(R.id.accountantContent);
        navigation = findViewById(R.id.accountantNavigation);
        navigationScroll = findViewById(R.id.accountantNavigationContainer);
        accountantLogo = findViewById(R.id.accountantLogo);
        accountantLogo.setOnClickListener(v -> toggleAccountantNavigation());
        branchBar = findViewById(R.id.accountantBranchSelector);
        pageTitle = findViewById(R.id.accountantPageTitle);
        userLabel = findViewById(R.id.accountantUserLabel);
        languageView = findViewById(R.id.accountantLanguage);
        languageView.setOnClickListener(v -> {
            language = language.equals("fr") ? "en" : "fr";
            languageView.setText(language.toUpperCase(Locale.ROOT));
            buildNavigation();
            buildBranches();
            render(active);
        });
        loadProfile();
    }

    private void loadProfile() {
        String uid = auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Map<String,Object> profile = (Map<String,Object>) s.getValue();
                if (profile == null) { Toast.makeText(AccountantPortalActivity.this,"Profil utilisateur introuvable",Toast.LENGTH_LONG).show(); finish(); return; }
                userRole = str(profile.get("role")).toLowerCase(Locale.ROOT);
                String normalized = norm(userRole);
                if (!(normalized.contains("comptable") || normalized.contains("accountant"))) {
                    Toast.makeText(AccountantPortalActivity.this,"Accès réservé au portail Comptable",Toast.LENGTH_LONG).show();
                    finish(); return;
                }
                userName = first(str(profile.get("name")),str(profile.get("fullName")),str(profile.get("displayName")),str(profile.get("email")));
                String rawBranch = first(str(profile.get("branchConfig")),str(profile.get("branch")),str(profile.get("branche")),str(profile.get("branchName")),str(profile.get("schoolBranch")));
                configureBranches(rawBranch, userRole);
                if (allowedBranches.isEmpty()) {
                    Toast.makeText(AccountantPortalActivity.this,"Aucune branche autorisée n’est associée à ce compte. Contactez la direction.",Toast.LENGTH_LONG).show();
                    finish(); return;
                }
                selectedBranch = defaultBranch(rawBranch);
                if (!allowedBranches.contains(selectedBranch)) selectedBranch = allowedBranches.get(0);
                userLabel.setText(userName + " • " + roleLabel(userRole));
                buildNavigation();
                buildBranches();
                loadExistingCollections();
                render("dashboard");
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(AccountantPortalActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    // Reprise des règles exactes getAllowedBranchesForCurrentUser() du portail web.
    private void configureBranches(String raw, String role) {
        String b = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        allowedBranches.clear();
        String normalizedRole = norm(role);
        if (b.isEmpty() && (normalizedRole.contains("principal") || normalizedRole.contains("directeur"))) allowedBranches.addAll(Arrays.asList("kacyiru","kimisagara","gisozi_maternelle","gisozi_primaire"));
        else if (b.isEmpty()) { /* No branch means no access: fail closed. */ }
        else if (b.equals("kacyiru") && role.trim().equalsIgnoreCase("comptable"))
            allowedBranches.addAll(Arrays.asList("kacyiru","gisozi_maternelle","gisozi_primaire"));
        else if (Arrays.asList("gisozi","gisozi_maternelle","gisozi_primaire").contains(b))
            allowedBranches.addAll(Arrays.asList("gisozi_maternelle","gisozi_primaire"));
        else allowedBranches.add(b);
    }
    private String defaultBranch(String raw) {
        String b = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (b.equals("gisozi")) return "gisozi_maternelle";
        return b.isEmpty() ? "kacyiru" : b;
    }
    private String firebaseBranch() {
        if (selectedBranch.equals("gisozi_maternelle") || selectedBranch.equals("gisozi_primaire")) return "gisozi";
        if (selectedBranch.equals("kacyiru_maternelle") || selectedBranch.equals("kacyiru_creche")) return "kacyiru";
        return selectedBranch;
    }

    private void buildNavigation() {
        navigation.removeAllViews();
        for (String[] item : modules) {
            TextView b = new TextView(this);
            b.setText(item[1] + "\n" + (language.equals("fr") ? item[2] : item[3]));
            b.setTextColor(active.equals(item[0]) ? DEEP : Color.WHITE);
            b.setTextSize(10);
            b.setGravity(Gravity.CENTER);
            b.setTypeface(Typeface.DEFAULT, active.equals(item[0]) ? Typeface.BOLD : Typeface.NORMAL);
            b.setPadding(dp(3),dp(12),dp(3),dp(12));
            if (active.equals(item[0])) b.setBackgroundColor(Color.rgb(232,244,236));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,dp(66));
            p.bottomMargin = dp(3);
            navigation.addView(b,p);
            b.setOnClickListener(v -> render(item[0]));
        }
    }

    private void buildBranches() {
        branchBar.removeAllViews();
        String[] labels = {"kacyiru","kimisagara","gisozi_maternelle","gisozi_primaire"};
        for (String branch : labels) {
            if (!allowedBranches.contains(branch)) continue;
            TextView b = new TextView(this);
            b.setText(branchLabel(branch));
            b.setTextSize(11);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(12),dp(7),dp(12),dp(7));
            b.setTextColor(branch.equals(selectedBranch) ? Color.WHITE : DEEP);
            b.setBackgroundColor(branch.equals(selectedBranch) ? GREEN : Color.rgb(232,244,236));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2,dp(34));
            p.setMargins(dp(4),0,dp(4),0);
            branchBar.addView(b,p);
            b.setOnClickListener(v -> {
                selectedBranch = branch;
                buildBranches();
                render(active);
            });
        }
    }

    private void loadExistingCollections() {
        // Ces références correspondent aux collections déclarées dans comptable.html.
        String[] collections = {"works","students","studentFees","feesPayments","transportPayments",
                "coachingPayments","registrationPayments","payroll","debts","reports","trash",
                "inscriptions-scolaires","payrollHistory","reportHistory","dailyReports","weeklyReports",
                "monthlyReports","feesConfiguration","openingBalances","closingBalances","termConfig","dailyExpenses","announcements"};
        for (String collection : collections) {
            database.getReference(collection).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                    List<Map<String,Object>> rows = new ArrayList<>();
                    for (DataSnapshot child : snap.getChildren()) {
                        Object value = child.getValue();
                        if (value instanceof Map) {
                            Map<String,Object> row = new LinkedHashMap<>((Map<String,Object>) value);
                            row.put("_id",child.getKey());
                            rows.add(row);
                        }
                    }
                    data.put(collection,rows);
                    if (active.equals("dashboard") || sectionCollection(active).equals(collection) || (active.equals("payroll") && collection.equals("payrollHistory")) || (active.equals("reports") && (collection.equals("dailyExpenses") || collection.equals("dailyReports") || collection.equals("weeklyReports") || collection.equals("monthlyReports")))) render(active);
                }
                @Override public void onCancelled(@NonNull DatabaseError e) {
                    Toast.makeText(AccountantPortalActivity.this,"Lecture " + collection + " : " + e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void render(String section) {
        active = section;
        buildNavigation();
        String heading = moduleLabel(section);
        pageTitle.setText(heading);
        content.removeAllViews();
        if (section.equals("payroll")) { renderPayrollWorkspace(); return; }
        if (section.equals("reports")) { renderFinancialReports(); return; }
        if (section.equals("expenses")) { renderExpenses(); return; }
        if (section.equals("announcements")) { renderAnnouncements(); return; }
        if (section.equals("dashboard")) {
            addText(language.equals("fr") ? "Tableau de bord financier" : "Financial dashboard",22,INK,true);
            addText(language.equals("fr") ? "État des données de la branche sélectionnée" : "Data overview for selected branch",13,Color.GRAY,false);
            List<Map<String,Object>> workers = filtered("works");
            List<Map<String,Object>> students = filtered("students");
            List<Map<String,Object>> payments = filtered("studentFees");
            List<Map<String,Object>> payroll = filtered("payroll");
            addStat(language.equals("fr") ? "Personnel" : "Workers",String.valueOf(workers.size()),"♙");
            addStat(language.equals("fr") ? "Élèves" : "Students",String.valueOf(countStudentsForBranch(students)), "♧");
            addStat(language.equals("fr") ? "Paiements élèves" : "Student payments",String.valueOf(payments.size()),"▣");
            double salary = 0;
            for (Map<String,Object> w : workers) salary += number(w.get("salary"));
            addStat(language.equals("fr") ? "Masse salariale mensuelle" : "Monthly payroll",money(salary),"₣");
            addText(language.equals("fr") ? "Accès rapide" : "Quick access",17,DEEP,true);
            for (String[] m : modules) if (!m[0].equals("dashboard")) addQuickLink(m);
        } else {
            addText(heading,21,INK,true);
            addText(branchLabel(selectedBranch),12,GREEN,false);
            addSectionToolbar(section);
            if (section.equals("student-fees")) addStudentPaymentFilters();
            if (section.equals("settings")) {
                renderSettings();
                return;
            }
            String collection = sectionCollection(section);
            if (collection.isEmpty()) {
                addText(language.equals("fr") ? "Aucun module disponible." : "No module available.",14,Color.GRAY,false);
                return;
            }
            List<Map<String,Object>> rows = filtered(collection);
            if (section.equals("student-fees")) { List<Map<String,Object>> selectedRows=new ArrayList<>(); for(Map<String,Object> row:rows) if(matchesStudentPaymentFilter(row)) selectedRows.add(row); rows=selectedRows; }
            addText((language.equals("fr") ? "Enregistrements : " : "Records: ") + rows.size(),13,Color.GRAY,false);
            if (rows.isEmpty()) {
                addText(language.equals("fr") ? "Aucune donnée trouvée pour cette branche." : "No records found for this branch.",14,Color.GRAY,false);
            }
            int shown = 0;
            for (Map<String,Object> row : rows) {
                if (shown++ >= 100) break;
                addRecord(row,section);
            }
        }
    }


    private void renderAnnouncements() {
        addText("Annonces de la Direction et du Secrétariat",19,DEEP,true);
        addText("Les 10 dernières communications publiées dans Eden Smart.",12,Color.GRAY,false);
        List<Map<String,Object>> items = new ArrayList<>();
        List<Map<String,Object>> source = data.get("announcements");
        if (source != null) {
            for (Map<String,Object> item : source) {
                String author = norm(first(str(item.get("author")),str(item.get("createdByRole")),str(item.get("role"))));
                if (author.equals("directeur") || author.equals("secretaire") ||
                    author.equals("director") || author.equals("secretary")) items.add(item);
            }
        }
        java.util.Collections.sort(items,(a,b) -> Long.compare(
                (long)number(b.get("timestamp")),(long)number(a.get("timestamp"))));
        if (items.isEmpty()) {
            addText("Aucune annonce du directeur ou du secrétariat pour le moment.",14,Color.GRAY,false);
            return;
        }
        int shown=0;
        for (Map<String,Object> item : items) {
            if (shown++ >= 10) break;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14),dp(12),dp(14),dp(12));
            card.setBackgroundColor(Color.WHITE);
            String author = norm(first(str(item.get("author")),str(item.get("createdByRole")),str(item.get("role"))));
            addTo(card,author.equals("directeur")||author.equals("director")?"DIRECTEUR":"SECRÉTARIAT",11,GREEN,true);
            addTo(card,first(str(item.get("title")),str(item.get("subject")),"Communication officielle"),15,DEEP,true);
            addTo(card,first(str(item.get("message")),str(item.get("content")),str(item.get("description")),""),13,INK,false);
            long stamp=(long)number(item.get("timestamp"));
            if(stamp>0) addTo(card,new java.text.SimpleDateFormat("dd/MM/yyyy • HH:mm",Locale.getDefault()).format(new java.util.Date(stamp)),11,Color.GRAY,false);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.bottomMargin=dp(9);content.addView(card,lp);
        }
        TextView refresh=text("Actualiser les annonces",13,GREEN,true);
        refresh.setPadding(dp(12),dp(10),dp(12),dp(10));
        refresh.setOnClickListener(v->refreshCollection("announcements"));
        content.addView(refresh);
    }

    private void addStudentPaymentFilters(){
        addText("Filtres des paiements",14,DEEP,true);
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.VERTICAL);filters.setPadding(dp(10),dp(8),dp(10),dp(8));filters.setBackgroundColor(Color.WHITE);
        EditText cls=new EditText(this);cls.setHint("Classe (ex. P1A / N2)");cls.setText(paymentClassFilter);filters.addView(cls);
        EditText month=new EditText(this);month.setHint("Mois YYYY-MM");month.setText(paymentMonthFilter);filters.addView(month);
        EditText from=new EditText(this);from.setHint("Date début YYYY-MM-DD");from.setText(paymentDateFrom);filters.addView(from);
        EditText to=new EditText(this);to.setHint("Date fin YYYY-MM-DD");to.setText(paymentDateTo);filters.addView(to);
        Spinner type=new Spinner(this);String[] types={"Tous les types","fees","transport","coaching","uniforme","registration","other"};type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,types));if(!paymentTypeFilter.isEmpty())for(int i=0;i<types.length;i++)if(types[i].equals(paymentTypeFilter))type.setSelection(i);filters.addView(type);
        LinearLayout actions=new LinearLayout(this);addAction(actions,"Appliquer les filtres",DEEP,()->{paymentClassFilter=cls.getText().toString().trim();paymentMonthFilter=month.getText().toString().trim();paymentDateFrom=from.getText().toString().trim();paymentDateTo=to.getText().toString().trim();paymentTypeFilter=String.valueOf(type.getSelectedItem());if(paymentTypeFilter.equals("Tous les types"))paymentTypeFilter="";render(active);});
        addAction(actions,"Effacer",Color.GRAY,()->{paymentClassFilter="";paymentMonthFilter="";paymentDateFrom="";paymentDateTo="";paymentTypeFilter="";render(active);});
        filters.addView(actions);content.addView(filters);
    }

    private boolean matchesStudentPaymentFilter(Map<String,Object> row){
        String date=paymentDate(row);String type=norm(str(row.get("paymentType")));String month=first(str(row.get("paymentMonth")),date.length()>=7?date.substring(0,7):"");
        String cls=first(str(row.get("class")),str(row.get("classe")),str(row.get("studentClass")));
        String sid=str(row.get("studentId"));
        if(cls.isEmpty()&&!sid.isEmpty())for(Map<String,Object>s:filtered("students"))if(sid.equals(str(s.get("_id")))){cls=first(str(s.get("classes")),str(s.get("class")),str(s.get("classe")));break;}
        if(!paymentClassFilter.isEmpty()&&!norm(cls).contains(norm(paymentClassFilter)))return false;
        if(!paymentMonthFilter.isEmpty()&&!month.startsWith(paymentMonthFilter))return false;
        if(!paymentDateFrom.isEmpty()&&date.compareTo(paymentDateFrom)<0)return false;
        if(!paymentDateTo.isEmpty()&&date.compareTo(paymentDateTo)>0)return false;
        if(!paymentTypeFilter.isEmpty()&&!type.equals(norm(paymentTypeFilter)))return false;
        return true;
    }

    private List<Map<String,Object>> filtered(String collection) {
        List<Map<String,Object>> source = data.get(collection);
        List<Map<String,Object>> out = new ArrayList<>();
        if (source == null) return out;
        String fb = firebaseBranch();
        for (Map<String,Object> row : source) {
            Object original = row.get("originalData");
            Map<?,?> originalMap = original instanceof Map ? (Map<?,?>) original : new LinkedHashMap<>();
            String rowBranch = first(str(row.get("branchConfig")),str(row.get("branch")),str(row.get("branche")),
                    str(row.get("branchName")),str(row.get("schoolBranch")),str(row.get("school_branch")),
                    str(originalMap.get("branch")),str(originalMap.get("branchConfig")),str(originalMap.get("schoolBranch")));
            if (!rowBranch.isEmpty() && (norm(rowBranch).equals(norm(fb)) || norm(rowBranch).equals(norm(selectedBranch)))) {
                if (!collection.equals("students") || studentLevelMatches(row)) out.add(row);
            }
        }
        return out;
    }
    private int countStudentsForBranch(List<Map<String,Object>> rows) { return rows.size(); }
    private boolean studentLevelMatches(Map<String,Object> row) {
        if (selectedBranch.equals("gisozi_maternelle") || selectedBranch.equals("gisozi_primaire")) {
            String cls = first(str(row.get("classes")),str(row.get("class")),str(row.get("classe"))).toUpperCase(Locale.ROOT);
            String level = first(str(row.get("level")),str(row.get("degre"))).toLowerCase(Locale.ROOT);
            boolean nursery = level.equals("maternelle") || level.equals("nursery") || cls.startsWith("N");
            boolean primary = level.equals("primaire") || level.equals("primary") || cls.startsWith("P");
            return selectedBranch.equals("gisozi_maternelle") ? nursery : primary;
        }
        return true;
    }

    private String sectionCollection(String s) {
        switch(s) {
            case "teachers": return "works";
            case "payroll": return "payroll";
            case "student-fees": return "studentFees";
            case "fees": return "feesPayments";
            case "coaching": return "coachingPayments";
            case "transport": return "transportPayments";
            case "registration": return "registrationPayments";
            case "students": return "students";
            case "reports": return "reports";
            case "expenses": return "dailyExpenses";
            case "trash": return "trash";
            default: return "";
        }
    }
    private void addStat(String label,String value,String symbol) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(12),dp(14),dp(12));
        card.setBackgroundColor(Color.WHITE);
        TextView icon = text(symbol,21,GREEN,true);
        TextView val = text(value,21,INK,true);
        TextView name = text(label,12,Color.GRAY,false);
        card.addView(icon); card.addView(val); card.addView(name);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin = dp(10);
        content.addView(card,p);
    }
    private void addQuickLink(String[] m) {
        TextView item = text(m[1] + "   " + (language.equals("fr") ? m[2] : m[3]) + "     ›",14,DEEP,true);
        item.setPadding(dp(12),dp(13),dp(12),dp(13));
        item.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin = dp(6);
        content.addView(item,p);
        item.setOnClickListener(v -> render(m[0]));
    }
    private void addRecord(Map<String,Object> row,String section) {
        String searchable = row.toString().toLowerCase(Locale.ROOT);
        if (!filterQuery.isEmpty() && !searchable.contains(filterQuery.toLowerCase(Locale.ROOT))) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12),dp(10),dp(12),dp(10));
        card.setBackgroundColor(Color.WHITE);
        String name = first(str(row.get("name")),str(row.get("studentName")),str(row.get("workerName")),
                str(row.get("fullName")),str(row.get("reference")),str(row.get("title")),str(row.get("_id")));
        addTo(card,name,14,DEEP,true);
        String details = first(str(row.get("type")),str(row.get("studentClass")),str(row.get("classe")),
                str(row.get("paymentType")),str(row.get("paymentMode")),str(row.get("status")));
        if (!details.isEmpty()) addTo(card,details,12,Color.GRAY,false);
        if (row.containsKey("amount") || row.containsKey("salary") || row.containsKey("totalAmount"))
            addTo(card,money(number(row.get("amount")) + number(row.get("salary")) + number(row.get("totalAmount"))),14,INK,true);
        String date = first(str(row.get("paymentDate")),str(row.get("date")),str(row.get("createdAt")),str(row.get("timestamp")));
        if (!date.isEmpty()) addTo(card,date,11,Color.GRAY,false);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (section.equals("trash")) {
            addAction(actions,language.equals("fr") ? "Restaurer" : "Restore",GREEN,() -> restoreTrashRecord(str(row.get("_id"))));
            addAction(actions,language.equals("fr") ? "Supprimer définitivement" : "Delete permanently",Color.RED,() -> permanentlyDeleteTrashRecord(str(row.get("_id"))));
        } else {
            addAction(actions,language.equals("fr") ? "Modifier" : "Edit",DEEP,() -> showRecordEditor(section,row));
            addAction(actions,language.equals("fr") ? "Supprimer" : "Delete",Color.RED,() -> deleteRecord(section,row));
            if (section.equals("teachers")) addAction(actions,language.equals("fr") ? "Dette" : "Debt",Color.rgb(190,115,20),() -> showDebtEditor(row));
        }
        card.addView(actions);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin = dp(8);
        content.addView(card,p);
    }
    private void addText(String value,int size,int color,boolean bold) { addTo(content,value,size,color,bold); }
    private void addTo(LinearLayout parent,String value,int size,int color,boolean bold) {
        TextView t = text(value,size,color,bold);
        t.setPadding(0,dp(4),0,dp(5));
        parent.addView(t);
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private String moduleLabel(String id) {
        for(String[] m:modules) if(m[0].equals(id)) return language.equals("fr")?m[2]:m[3];
        return id;
    }
    private String branchLabel(String b) {
        switch(b) {
            case "kacyiru": return "Kacyiru";
            case "kimisagara": return "Kimisagara";
            case "gisozi_maternelle": return "Gisozi • Maternelle";
            case "gisozi_primaire": return "Gisozi • Primaire";
            default: return b;
        }
    }
    private String roleLabel(String role) {
        String n=norm(role);
        if(n.contains("principal") || n.equals("comptable")) return "Chief Accountant";
        return "Accountant";
    }
    private String str(Object v) { return v == null ? "" : String.valueOf(v); }
    private String first(String... a) { for(String s:a) if(s!=null&&!s.trim().isEmpty()) return s.trim(); return ""; }
    private String norm(String s) { return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[\\s_-]+",""); }
    private double number(Object v) { try { return Double.parseDouble(str(v)); } catch(Exception e) { return 0; } }
    private String money(double v) { return NumberFormat.getNumberInstance(Locale.US).format(v) + " RWF"; }
    private void toggleAccountantNavigation() {
        navigationExpanded = !navigationExpanded;
        navigation.setVisibility(navigationExpanded ? View.VISIBLE : View.GONE);
        android.view.ViewGroup.LayoutParams params = navigationScroll.getLayoutParams();
        params.width = dp(navigationExpanded ? 116 : 70);
        navigationScroll.setLayoutParams(params);
        accountantLogo.setAlpha(navigationExpanded ? 1f : 0.82f);
        Toast.makeText(this, navigationExpanded ? "Menu affiché" : "Menu masqué", Toast.LENGTH_SHORT).show();
    }

    private void addSectionToolbar(String section) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0,dp(7),0,dp(7));
        HorizontalScrollViewWrap(bar);
        if (section.equals("teachers")) {
            addAction(bar,"+ Personnel",DEEP,() -> showRecordEditor("teachers",null));
            addAction(bar,"+ Dette",Color.rgb(190,115,20),() -> showDebtEditor(null));
            addAction(bar,"Voir dettes",INK,() -> showCollectionHistory("debts"));
        } else if (section.equals("payroll")) {
            addAction(bar,"Générer la paie",DEEP,this::generateMonthlyPayroll);
            addAction(bar,"Aperçu banque / MoMo",GREEN,this::showPayrollBankPreview);
            addAction(bar,"Historique",INK,() -> showCollectionHistory("payrollHistory"));
        } else if (section.equals("student-fees")) {
            addAction(bar,"+ Paiement",DEEP,() -> showRecordEditor(section,null));
            addAction(bar,"Historique élève",GREEN,this::showStudentPaymentHistory);
            addAction(bar,"Doublons",Color.rgb(190,115,20),this::checkPaymentDuplicates);
        } else if (section.equals("fees")) {
            addAction(bar,"Configurer un élève",DEEP,() -> showStudentServiceConfig("fees"));
            addAction(bar,"Configurer en masse",Color.rgb(120,80,150),() -> showBulkServiceConfig("fees"));
            addAction(bar,"Paiement frais",GREEN,() -> showRecordEditor(section,null));
            addAction(bar,"Paiements en masse",INK,() -> showBulkPaymentDialog(section));
        } else if (section.equals("coaching") || section.equals("transport")) {
            addAction(bar,"Configurer",DEEP,() -> showStudentServiceConfig(section));
            addAction(bar,"Configurer en masse",Color.rgb(120,80,150),() -> showBulkServiceConfig(section));
            addAction(bar,"+ Paiement",GREEN,() -> showRecordEditor(section,null));
            addAction(bar,"Paiements en masse",INK,() -> showBulkPaymentDialog(section));
        } else if (section.equals("registration")) {
            addAction(bar,"Nouvelle inscription",DEEP,this::showNewRegistrationDialog);
            addAction(bar,"Demandes en attente",GREEN,this::showPendingRegistrations);
            addAction(bar,"Paiement",INK,() -> showRecordEditor(section,null));
        } else if (section.equals("students")) {
            addAction(bar,"+ Élève",DEEP,() -> showRecordEditor(section,null));
            addAction(bar,"Actualiser",GREEN,() -> refreshCollection("students"));
        } else if (section.equals("expenses")) {
            addAction(bar,"Ajouter une dépense",DEEP,() -> showStandaloneExpenseDialog());
            addAction(bar,"Actualiser",GREEN,() -> refreshCollection("dailyExpenses"));
        } else if (section.equals("reports")) {
            addAction(bar,"Rapport journalier",DEEP,() -> showFinancialReportDialog("daily"));
            addAction(bar,"Hebdomadaire",GREEN,() -> showFinancialReportDialog("weekly"));
            addAction(bar,"Mensuel",INK,() -> showFinancialReportDialog("monthly"));
            addAction(bar,"Historique",Color.rgb(120,80,150),() -> showCollectionHistory("reportHistory"));
        } else if (section.equals("trash")) {
            addAction(bar,"Actualiser",DEEP,() -> refreshCollection("trash"));
            addAction(bar,"Vider",Color.RED,this::emptyTrash);
        }
        if (!section.equals("settings") && !section.equals("trash")) addAction(bar,"Exporter CSV",INK,this::exportCurrentSectionCsv);
        if (bar.getChildCount() > 0) {
            HorizontalScrollView strip = new HorizontalScrollView(this);
            strip.setHorizontalScrollBarEnabled(false);
            strip.addView(bar);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1,-2);
            bp.bottomMargin = dp(4);
            content.addView(strip,bp);
        }
        if (!section.equals("trash") && !section.equals("settings")) {
            LinearLayout search = new LinearLayout(this);
            search.setOrientation(LinearLayout.HORIZONTAL);
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setTextSize(13);
            input.setHint(language.equals("fr") ? "Rechercher dans cette liste..." : "Search this list...");
            input.setText(filterQuery);
            search.addView(input,new LinearLayout.LayoutParams(0,dp(44),1));
            Button go = new Button(this);
            go.setText(language.equals("fr") ? "Filtrer" : "Filter");
            go.setOnClickListener(v -> { filterQuery = input.getText().toString().trim(); render(active); });
            search.addView(go,new LinearLayout.LayoutParams(-2,dp(44)));
            content.addView(search);
        }
    }

    private void HorizontalScrollViewWrap(LinearLayout bar) {
        // Keep the action strip compact on narrow phone screens.
        bar.setBackgroundColor(Color.rgb(232,244,236));
    }

    private void addAction(LinearLayout parent,String label,int color,Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        button.setPadding(dp(7),0,dp(7),0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2,dp(42));
        lp.setMargins(dp(3),dp(2),dp(3),dp(2));
        parent.addView(button,lp);
        button.setOnClickListener(v -> action.run());
    }

    private String collectionFor(String section) {
        if (section.equals("teachers")) return "works";
        return sectionCollection(section);
    }

    private void refreshCollection(String collection) {
        database.getReference(collection).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                List<Map<String,Object>> rows = new ArrayList<>();
                for (DataSnapshot child : snap.getChildren()) {
                    Object value = child.getValue();
                    if (value instanceof Map) {
                        Map<String,Object> row = new LinkedHashMap<>((Map<String,Object>) value);
                        row.put("_id",child.getKey());
                        rows.add(row);
                    }
                }
                data.put(collection,rows);
                render(active);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(AccountantPortalActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    private String[] editorFields(String section) {
        switch(section) {
            case "teachers": return new String[]{"no","name","type","phone","salary","paymentMode","accountNo","bankName","accountHolderName","momoOperator","momoNumber","momoRegisteredName","hireDate","status"};
            case "students": return new String[]{"fullName","classes","gender","birthDate","level","academicYear","parentName","parentPhone","parentID","parentEmail","guardianName","guardianPhone","address","city","sector","cell","village","transportEnrolled","transportMonthlyFee","coachingEnrolled","coachingMonthlyFee","status"};
            case "student-fees": return new String[]{"studentId","amount","paymentType","paymentMonth","trimester","academicYear","paymentMode","transactionRef","paymentDate"};
            case "fees": return new String[]{"studentId","amount","paymentType","paymentDate","paymentMode","transactionRef","receiptNumber","paymentMonth","trimester","academicYear","description"};
            case "coaching": return new String[]{"studentId","amount","paymentDate","paymentMonth","trimester","academicYear","paymentMode","transactionRef","receiptNumber","teacherName","description"};
            case "transport": return new String[]{"studentId","amount","paymentDate","paymentMonth","trimester","academicYear","paymentMode","transactionRef","receiptNumber","description"};
            case "registration": return new String[]{"studentId","amount","paymentType","paymentDate","paymentMode","transactionRef","receiptNumber","academicYear","description"};
            case "payroll": return new String[]{"month","year","totalAmount","status","notes"};
            default: return new String[]{"title","date","amount","description","status"};
        }
    }

    private String fieldLabel(String key) {
        switch(key) {
            case "no": return "Numéro employé";
            case "name": case "fullName": return "Nom complet";
            case "type": return "Catégorie / type";
            case "phone": case "parentPhone": return "Téléphone";
            case "salary": return "Salaire mensuel (RWF)";
            case "paymentMode": return "Mode de paiement";
            case "accountNo": return "Numéro de compte";
            case "bankName": return "Banque";
            case "accountHolderName": return "Titulaire du compte";
            case "momoOperator": return "Opérateur Mobile Money";
            case "momoNumber": return "Numéro Mobile Money";
            case "momoRegisteredName": return "Nom Mobile Money";
            case "hireDate": return "Date d'embauche";
            case "classes": return "Classe";
            case "gender": return "Sexe";
            case "birthDate": return "Date de naissance";
            case "parentName": return "Nom du parent";
            case "parentID": return "ID du parent";
            case "parentEmail": return "E-mail du parent";
            case "guardianName": return "Nom du tuteur";
            case "guardianPhone": return "Téléphone du tuteur";
            case "level": return "Section / niveau";
            case "academicYear": return "Année scolaire";
            case "city": return "Ville";
            case "sector": return "Secteur";
            case "cell": return "Cellule";
            case "village": return "Village";
            case "transportEnrolled": return "Inscrit au transport (true/false)";
            case "transportMonthlyFee": return "Frais mensuels transport";
            case "coachingEnrolled": return "Inscrit au coaching (true/false)";
            case "coachingMonthlyFee": return "Frais mensuels coaching";
            case "receiptNumber": return "Numéro de reçu";
            case "address": return "Adresse";
            case "studentId": return "ID de l'élève";
            case "amount": case "totalAmount": return "Montant (RWF)";
            case "paymentType": return "Type de paiement";
            case "paymentMonth": return "Mois payé (YYYY-MM)";
            case "trimester": return "Trimestre";
            case "transactionRef": return "Référence de transaction";
            case "paymentDate": case "date": return "Date (YYYY-MM-DD)";
            case "teacherName": return "Enseignant responsable";
            case "month": return "Mois";
            case "year": return "Année";
            case "notes": case "description": return "Notes / description";
            case "title": return "Titre";
            default: return key;
        }
    }

    private void showRecordEditor(String section,Map<String,Object> existing) {
        String collection = collectionFor(section);
        if (collection.isEmpty()) return;
        Map<String,Object> record = existing == null ? new LinkedHashMap<>() : existing;
        String[] fields = editorFields(section);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18),dp(8),dp(18),dp(8));
        Map<String,EditText> inputs = new LinkedHashMap<>();
        Map<String,Spinner> studentPickers = new LinkedHashMap<>();
        List<Map<String,Object>> branchStudents = filtered("students");
        for (String key : fields) {
            TextView label = text(fieldLabel(key),12,INK,true);
            form.addView(label);
            if (key.equals("studentId")) {
                if (branchStudents.isEmpty()) { Toast.makeText(this,"Aucun élève disponible dans cette branche.",Toast.LENGTH_LONG).show(); return; }
                List<String> studentLabels = new ArrayList<>();
                int selectedIndex = 0;
                for (int i=0;i<branchStudents.size();i++) {
                    Map<String,Object> student = branchStudents.get(i);
                    String id = str(student.get("_id"));
                    studentLabels.add(first(str(student.get("fullName")),str(student.get("name")))+" • "+id);
                    if (id.equals(str(record.get("studentId")))) selectedIndex = i;
                }
                Spinner picker = new Spinner(this);
                picker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,studentLabels));
                picker.setSelection(selectedIndex);
                form.addView(picker);
                studentPickers.put(key,picker);
                continue;
            }
            EditText edit = new EditText(this);
            edit.setSingleLine(key.equals("notes") || key.equals("address") ? false : true);
            edit.setTextSize(14);
            edit.setHint(fieldLabel(key));
            Object value = record.get(key);
            if (value != null) edit.setText(String.valueOf(value));
            if (key.equals("amount") || key.equals("salary") || key.equals("totalAmount"))
                edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            form.addView(edit);
            inputs.put(key,edit);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nouvel enregistrement" : "Modifier l'enregistrement")
                .setView(scroll).setNegativeButton("Annuler",null)
                .setPositiveButton("Enregistrer",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            Map<String,Object> payload = new LinkedHashMap<>();
            for (String key : fields) {
                if (key.equals("studentId")) {
                    Spinner picker = studentPickers.get(key);
                    Map<String,Object> chosen = branchStudents.get(picker.getSelectedItemPosition());
                    payload.put("studentId",str(chosen.get("_id")));
                    continue;
                }
                String value = inputs.get(key).getText().toString().trim();
                if ((key.equals("name") || key.equals("fullName") || key.equals("amount")) && value.isEmpty()) {
                    inputs.get(key).setError("Champ obligatoire");
                    return;
                }
                if (key.equals("amount") || key.equals("salary") || key.equals("totalAmount")) {
                    if (!value.isEmpty()) {
                        try { payload.put(key,Double.parseDouble(value)); }
                        catch(Exception ex) { inputs.get(key).setError("Montant invalide"); return; }
                    }
                } else payload.put(key,value);
            }
            String branch = section.equals("teachers") ? firebaseBranch() : selectedBranch;
            payload.put("branch",branch);
            payload.put("branchConfig",selectedBranch);
            payload.put("schoolBranch",selectedBranch);
            payload.put("timestamp",System.currentTimeMillis());
            payload.put("createdBy",userName);
            if (section.equals("teachers")) payload.put("status",first(str(payload.get("status")),"active"));
            if (section.equals("students")) {
                payload.put("name",first(str(payload.get("fullName")),str(payload.get("name"))));
                payload.put("fullName",payload.get("name"));
            }
            if (section.equals("student-fees") || section.equals("fees") || section.equals("coaching") ||
                    section.equals("transport") || section.equals("registration")) {
                Map<String,Object> student = findStudent(str(payload.get("studentId")));
                if (student == null) { Toast.makeText(this,"ID élève introuvable dans cette branche.",Toast.LENGTH_LONG).show(); return; }
                payload.put("studentName",first(str(student.get("fullName")),str(student.get("name"))));
                payload.put("studentClass",first(str(student.get("classes")),str(student.get("class")),str(student.get("classe"))));
                payload.put("receivedBy",userName);
                payload.put("status","paid");
                payload.put("paymentType",first(str(payload.get("paymentType")),section.equals("fees") || section.equals("student-fees") ? "fees" : section));
                payload.put("academicYear",first(str(payload.get("academicYear")),currentAcademicYear()));
                if (str(payload.get("paymentDate")).isEmpty()) payload.put("paymentDate",today());
            }
            if (section.equals("student-fees")) {
                String type = str(payload.get("paymentType"));
                String month = str(payload.get("paymentMonth"));
                if ((type.equals("coaching") || type.equals("transport")) && month.isEmpty()) {
                    Toast.makeText(this,"Indiquez le mois payé (YYYY-MM).",Toast.LENGTH_LONG).show();
                    return;
                }
                for (Map<String,Object> saved : filtered("studentFees")) {
                    boolean same = str(saved.get("studentId")).equals(str(payload.get("studentId"))) &&
                            str(saved.get("paymentType")).equals(type) &&
                            first(str(saved.get("paymentMonth")),str(saved.get("paymentDate"))).equals(first(month,str(payload.get("paymentDate")))) &&
                            str(saved.get("trimester")).equals(str(payload.get("trimester"))) &&
                            !str(saved.get("_id")).equals(str(record.get("_id")));
                    if (same) {
                        Toast.makeText(this,"Paiement potentiellement déjà enregistré pour cet élève et cette période.",Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
            DatabaseReference ref = database.getReference(collection);
            String id = str(record.get("_id"));
            if (existing == null) {
                DatabaseReference newRef = ref.push();
                newRef.setValue(payload).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { Toast.makeText(this,"Échec : "+task.getException(),Toast.LENGTH_LONG).show(); return; }
                    dialog.dismiss(); Toast.makeText(this,"Enregistrement ajouté.",Toast.LENGTH_SHORT).show(); refreshCollection(collection);
                });
            } else {
                ref.child(id).updateChildren(payload).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { Toast.makeText(this,"Échec : "+task.getException(),Toast.LENGTH_LONG).show(); return; }
                    dialog.dismiss(); Toast.makeText(this,"Modifications enregistrées.",Toast.LENGTH_SHORT).show(); refreshCollection(collection);
                });
            }
        }));
        dialog.show();
    }

    private Map<String,Object> findStudent(String id) {
        List<Map<String,Object>> rows = filtered("students");
        for (Map<String,Object> row : rows) if (str(row.get("_id")).equals(id)) return row;
        return null;
    }

    private void deleteRecord(String section,Map<String,Object> row) {
        String collection = collectionFor(section);
        String id = str(row.get("_id"));
        if (collection.isEmpty() || id.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("Déplacer vers la corbeille")
                .setMessage("Cet élément sera archivé et pourra être restauré depuis la corbeille.")
                .setNegativeButton("Annuler",null).setPositiveButton("Continuer",(d,w) -> {
                    DatabaseReference ref = database.getReference(collection).child(id);
                    ref.get().addOnSuccessListener(snap -> {
                        if (!snap.exists()) return;
                        Map<String,Object> item = new LinkedHashMap<>();
                        item.put("source",collection);
                        item.put("originalPath",collection+"/"+id);
                        item.put("originalData",snap.getValue());
                        item.put("label",first(str(row.get("name")),str(row.get("studentName")),str(row.get("fullName")),id));
                        item.put("reason","Suppression depuis l'application Android");
                        item.put("deletedBy",userName);
                        item.put("deletedAt",System.currentTimeMillis());
                        item.put("branch",first(str(row.get("branch")),firebaseBranch()));
                        database.getReference("trash").push().setValue(item).addOnSuccessListener(x ->
                                ref.removeValue().addOnSuccessListener(y -> refreshCollection(collection)));
                    });
                }).show();
    }

    private void restoreTrashRecord(String trashId) {
        if (trashId.isEmpty()) return;
        DatabaseReference trashRef = database.getReference("trash").child(trashId);
        trashRef.get().addOnSuccessListener(snap -> {
            String path = str(snap.child("originalPath").getValue());
            Object original = snap.child("originalData").getValue();
            if (path.isEmpty() || original == null) { Toast.makeText(this,"Sauvegarde de corbeille invalide.",Toast.LENGTH_LONG).show(); return; }
            database.getReference(path).setValue(original).addOnSuccessListener(v ->
                    trashRef.removeValue().addOnSuccessListener(x -> { refreshCollection("trash"); refreshCollection(sectionCollection(active)); }));
        });
    }

    private void permanentlyDeleteTrashRecord(String id) {
        new AlertDialog.Builder(this).setTitle("Suppression définitive")
                .setMessage("Cette action est irréversible. Continuer ?")
                .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w) ->
                        database.getReference("trash").child(id).removeValue().addOnSuccessListener(v -> refreshCollection("trash"))).show();
    }

    private void emptyTrash() {
        new AlertDialog.Builder(this).setTitle("Vider la corbeille")
                .setMessage("Tous les éléments archivés seront supprimés définitivement.")
                .setNegativeButton("Annuler",null).setPositiveButton("Vider",(d,w) ->
                        database.getReference("trash").removeValue().addOnSuccessListener(v -> refreshCollection("trash"))).show();
    }

    private void showDebtEditor(Map<String,Object> worker) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        EditText amount = new EditText(this); amount.setHint("Montant de la dette (RWF)");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText reason = new EditText(this); reason.setHint("Motif / description");
        form.addView(amount); form.addView(reason);
        new AlertDialog.Builder(this).setTitle("Enregistrer une dette").setView(form)
                .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w) -> {
                    double value = number(amount.getText().toString());
                    if (value <= 0) { Toast.makeText(this,"Montant invalide.",Toast.LENGTH_SHORT).show(); return; }
                    Map<String,Object> debt = new LinkedHashMap<>();
                    debt.put("workerId",worker == null ? "" : str(worker.get("_id")));
                    debt.put("workerName",worker == null ? "" : str(worker.get("name")));
                    debt.put("amount",value); debt.put("reason",reason.getText().toString().trim());
                    debt.put("branch",firebaseBranch()); debt.put("status","unpaid");
                    debt.put("addedBy",userName); debt.put("timestamp",System.currentTimeMillis());
                    database.getReference("debts").push().setValue(debt).addOnSuccessListener(x -> {
                        Toast.makeText(this,"Dette enregistrée.",Toast.LENGTH_SHORT).show(); refreshCollection("debts");
                    });
                }).show();
    }

    private void showStudentServiceConfig(String service) {
        List<Map<String,Object>> students = filtered("students");
        if (students.isEmpty()) { Toast.makeText(this,"Aucun élève pour cette branche.",Toast.LENGTH_SHORT).show(); return; }
        List<String> labels = new ArrayList<>();
        for (Map<String,Object> student : students) labels.add(first(str(student.get("fullName")),str(student.get("name")))+" • "+str(student.get("_id")));
        Spinner picker = new Spinner(this);
        picker.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        EditText amount = new EditText(this); amount.setHint("Montant (RWF)"); amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
        form.addView(text("Élève",13,INK,true)); form.addView(picker); form.addView(amount);
        new AlertDialog.Builder(this).setTitle("Configurer "+service).setView(form)
                .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w) -> {
                    Map<String,Object> student = students.get(picker.getSelectedItemPosition());
                    double value = number(amount.getText().toString());
                    if (value <= 0) { Toast.makeText(this,"Montant invalide.",Toast.LENGTH_SHORT).show(); return; }
                    String id = str(student.get("_id"));
                    Map<String,Object> update = new LinkedHashMap<>();
                    if (service.equals("fees")) {
                        update.put("feesMonthlyAmount",value); update.put("feesDateConfigured",today()); update.put("feesConfiguredBy",userName); update.put("feesUpdatedAt",System.currentTimeMillis());
                    } else if (service.equals("transport")) {
                        update.put("transportEnrolled",true); update.put("transportMonthlyFee",value); update.put("transportDateConfigured",today()); update.put("transportConfiguredBy",userName); update.put("transportUpdatedAt",System.currentTimeMillis());
                    } else {
                        update.put("coachingEnrolled",true); update.put("coachingMonthlyFee",value); update.put("coachingDateConfigured",today()); update.put("coachingConfiguredBy",userName); update.put("coachingUpdatedAt",System.currentTimeMillis());
                    }
                    database.getReference("students").child(id).updateChildren(update).addOnSuccessListener(x -> {
                        Toast.makeText(this,"Configuration enregistrée.",Toast.LENGTH_SHORT).show(); refreshCollection("students");
                    });
                }).show();
    }

    private void showBulkPaymentDialog(String section) {
        List<Map<String,Object>> students=filtered("students");
        if(students.isEmpty()) { Toast.makeText(this,"Aucun élève dans cette branche.",Toast.LENGTH_SHORT).show(); return; }
        String[] labels=new String[students.size()];
        boolean[] checked=new boolean[students.size()];
        for(int i=0;i<students.size();i++) labels[i]=first(str(students.get(i).get("fullName")),str(students.get(i).get("name")));
        new AlertDialog.Builder(this).setTitle("Sélectionner les élèves à payer")
                .setMultiChoiceItems(labels,checked,(dialog,which,isChecked)->checked[which]=isChecked)
                .setNegativeButton("Annuler",null).setPositiveButton("Continuer",(dialog,which)->{
                    List<Map<String,Object>> selected=new ArrayList<>();
                    for(int i=0;i<checked.length;i++) if(checked[i]) selected.add(students.get(i));
                    if(selected.isEmpty()) { Toast.makeText(this,"Sélectionnez au moins un élève.",Toast.LENGTH_SHORT).show(); return; }
                    LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
                    EditText amount=new EditText(this); amount.setHint("Montant par élève (RWF)");
                    amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    EditText date=new EditText(this); date.setText(today()); date.setHint("Date du paiement");
                    EditText month=new EditText(this); month.setHint("Mois payé (YYYY-MM) — obligatoire pour coaching/transport");
                    form.addView(amount); form.addView(date);
                    if(section.equals("coaching")||section.equals("transport")) form.addView(month);
                    new AlertDialog.Builder(this).setTitle("Paiement groupé • "+selected.size()+" élève(s)").setView(form)
                            .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
                                double value=number(amount.getText().toString());
                                String paidDate=date.getText().toString().trim(), paidMonth=month.getText().toString().trim();
                                if(value<=0 || paidDate.isEmpty()) { Toast.makeText(this,"Montant et date obligatoires.",Toast.LENGTH_SHORT).show(); return; }
                                if((section.equals("coaching")||section.equals("transport"))&&paidMonth.isEmpty()) { Toast.makeText(this,"Le mois payé est obligatoire.",Toast.LENGTH_SHORT).show(); return; }
                                String collection=collectionFor(section);
                                List<Map<String,Object>> existing=filtered(collection);
                                Map<String,Object> updates=new LinkedHashMap<>();
                                int added=0,duplicates=0;
                                for(Map<String,Object> student:selected) {
                                    String sid=str(student.get("_id"));
                                    boolean duplicate=false;
                                    for(Map<String,Object> old:existing) {
                                        if(str(old.get("studentId")).equals(sid) &&
                                                str(old.get("paymentType")).equals(section) &&
                                                first(str(old.get("paymentMonth")),str(old.get("paymentDate"))).equals(first(paidMonth,paidDate))) { duplicate=true; break; }
                                    }
                                    if(duplicate) { duplicates++; continue; }
                                    Map<String,Object> payment=new LinkedHashMap<>();
                                    payment.put("studentId",sid); payment.put("studentName",first(str(student.get("fullName")),str(student.get("name"))));
                                    payment.put("studentClass",first(str(student.get("classes")),str(student.get("class")),str(student.get("classe"))));
                                    payment.put("amount",value); payment.put("paymentType",section.equals("fees")?"fees":section);
                                    payment.put("paymentDate",paidDate); payment.put("paymentMonth",paidMonth);
                                    payment.put("trimester","T1"); payment.put("academicYear",currentAcademicYear());
                                    payment.put("branch",selectedBranch); payment.put("branchConfig",selectedBranch);
                                    payment.put("receivedBy",userName); payment.put("status","paid"); payment.put("timestamp",System.currentTimeMillis());
                                    String id=database.getReference(collection).push().getKey();
                                    updates.put(collection+"/"+id,payment); added++;
                                }
                                if(updates.isEmpty()) { Toast.makeText(this,"Aucun paiement ajouté : doublons détectés.",Toast.LENGTH_LONG).show(); return; }
                                int saved=added, skipped=duplicates;
                                database.getReference().updateChildren(updates).addOnSuccessListener(x -> {
                                    Toast.makeText(this,saved+" paiement(s) enregistré(s), "+skipped+" doublon(s) ignoré(s).",Toast.LENGTH_LONG).show();
                                    refreshCollection(collection);
                                }).addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
                            }).show();
                }).show();
    }

    private void showBulkServiceConfig(String service) {
        List<Map<String,Object>> students = filtered("students");
        if(students.isEmpty()) { Toast.makeText(this,"Aucun élève dans cette branche.",Toast.LENGTH_SHORT).show(); return; }
        String[] labels = new String[students.size()];
        boolean[] checked = new boolean[students.size()];
        for(int i=0;i<students.size();i++) labels[i]=first(str(students.get(i).get("fullName")),str(students.get(i).get("name")));
        new AlertDialog.Builder(this).setTitle("Sélectionner les élèves")
                .setMultiChoiceItems(labels,checked,(dialog,which,isChecked) -> checked[which]=isChecked)
                .setNegativeButton("Annuler",null).setPositiveButton("Continuer",(dialog,which) -> {
                    List<String> ids = new ArrayList<>();
                    for(int i=0;i<checked.length;i++) if(checked[i]) ids.add(str(students.get(i).get("_id")));
                    if(ids.isEmpty()) { Toast.makeText(this,"Sélectionnez au moins un élève.",Toast.LENGTH_SHORT).show(); return; }
                    EditText amount = new EditText(this); amount.setHint("Montant mensuel (RWF)");
                    amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    new AlertDialog.Builder(this).setTitle("Montant pour "+ids.size()+" élève(s)").setView(amount)
                            .setNegativeButton("Annuler",null).setPositiveButton("Appliquer",(d,w) -> {
                                double value=number(amount.getText().toString());
                                if(value<=0) { Toast.makeText(this,"Montant invalide.",Toast.LENGTH_SHORT).show(); return; }
                                Map<String,Object> updates=new LinkedHashMap<>();
                                for(String id:ids) {
                                    String base="students/"+id+"/";
                                    if(service.equals("fees")) {
                                        updates.put(base+"feesMonthlyAmount",value); updates.put(base+"feesDateConfigured",today());
                                        updates.put(base+"feesConfiguredBy",userName); updates.put(base+"feesUpdatedAt",System.currentTimeMillis());
                                    } else if(service.equals("transport")) {
                                        updates.put(base+"transportEnrolled",true); updates.put(base+"transportMonthlyFee",value);
                                        updates.put(base+"transportDateConfigured",today()); updates.put(base+"transportConfiguredBy",userName);
                                        updates.put(base+"transportUpdatedAt",System.currentTimeMillis());
                                    } else {
                                        updates.put(base+"coachingEnrolled",true); updates.put(base+"coachingMonthlyFee",value);
                                        updates.put(base+"coachingDateConfigured",today()); updates.put(base+"coachingConfiguredBy",userName);
                                        updates.put(base+"coachingUpdatedAt",System.currentTimeMillis());
                                    }
                                }
                                database.getReference().updateChildren(updates).addOnSuccessListener(x -> {
                                    Toast.makeText(this,"Configuration appliquée à "+ids.size()+" élève(s).",Toast.LENGTH_LONG).show();
                                    refreshCollection("students");
                                }).addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
                            }).show();
                }).show();
    }

    private void showNewRegistrationDialog() {
        String[] keys = {"fullName","birthDate","gender","classes","parentName","parentPhone","address","amount"};
        String[] labels = {"Nom complet de l'élève","Date de naissance","Sexe","Classe demandée","Nom du parent","Téléphone du parent","Adresse","Frais d'inscription (RWF)"};
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
        Map<String,EditText> fields = new LinkedHashMap<>();
        for (int i=0;i<keys.length;i++) { form.addView(text(labels[i],12,INK,true)); EditText e = new EditText(this); e.setSingleLine(true); if(keys[i].equals("amount")) e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); form.addView(e); fields.put(keys[i],e); }
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        new AlertDialog.Builder(this).setTitle("Nouvelle inscription").setView(scroll).setNegativeButton("Annuler",null)
                .setPositiveButton("Créer", (dlg,which) -> {
                    String name = fields.get("fullName").getText().toString().trim();
                    if(name.isEmpty()) { Toast.makeText(this,"Nom de l'élève obligatoire.",Toast.LENGTH_SHORT).show(); return; }
                    Map<String,Object> student = new LinkedHashMap<>();
                    for(String key:keys) if(!key.equals("amount")) student.put(key,fields.get(key).getText().toString().trim());
                    student.put("name",name); student.put("fullName",name); student.put("branch",selectedBranch);
                    student.put("branchConfig",selectedBranch); student.put("schoolBranch",selectedBranch);
                    student.put("status","active"); student.put("createdAt",System.currentTimeMillis()); student.put("createdBy",userName);
                    DatabaseReference newStudentRef = database.getReference("students").push();
                    newStudentRef.setValue(student).addOnSuccessListener(x -> {
                        double fee = number(fields.get("amount").getText().toString());
                        if(fee > 0) {
                            Map<String,Object> payment = new LinkedHashMap<>();
                            payment.put("studentName",name); payment.put("studentId",newStudentRef.getKey()); payment.put("amount",fee);
                            payment.put("paymentType","registration"); payment.put("paymentDate",today());
                            payment.put("branch",firebaseBranch()); payment.put("branchConfig",selectedBranch);
                            payment.put("receivedBy",userName); payment.put("status","paid"); payment.put("timestamp",System.currentTimeMillis());
                            database.getReference("registrationPayments").push().setValue(payment);
                        }
                        Toast.makeText(this,"Inscription créée.",Toast.LENGTH_SHORT).show();
                        refreshCollection("students"); refreshCollection("registrationPayments");
                    });
                }).show();
    }

    private void showPendingRegistrations() {
        List<Map<String,Object>> pending = filtered("inscriptions-scolaires");
        if(pending.isEmpty()) { Toast.makeText(this,"Aucune demande en attente.",Toast.LENGTH_SHORT).show(); return; }
        String[] names = new String[pending.size()];
        for(int i=0;i<pending.size();i++) names[i]=first(str(pending.get(i).get("fullName")),str(pending.get(i).get("name")),str(pending.get(i).get("_id")));
        new AlertDialog.Builder(this).setTitle("Demandes d'inscription")
                .setItems(names,(dlg,which) -> {
                    Map<String,Object> item = pending.get(which);
                    new AlertDialog.Builder(this).setTitle(names[which]).setMessage(item.toString())
                            .setNegativeButton("Fermer",null).setPositiveButton("Accepter",(d,w) -> {
                                Map<String,Object> accepted = new LinkedHashMap<>(item); accepted.remove("_id");
                                accepted.put("branch",selectedBranch); accepted.put("branchConfig",selectedBranch); accepted.put("status","active");
                                database.getReference("students").push().setValue(accepted).addOnSuccessListener(x -> {
                                    database.getReference("inscriptions-scolaires").child(str(item.get("_id"))).child("status").setValue("approved");
                                    refreshCollection("students"); refreshCollection("inscriptions-scolaires");
                                });
                            }).show();
                }).show();
    }

    private void generateMonthlyPayroll() {
        List<Map<String,Object>> workers = filtered("works");
        if(workers.isEmpty()) { Toast.makeText(this,"Aucun personnel dans cette branche.",Toast.LENGTH_SHORT).show(); return; }
        java.util.Calendar c = java.util.Calendar.getInstance();
        int month = c.get(java.util.Calendar.MONTH)+1, year = c.get(java.util.Calendar.YEAR);
        String key = "payroll_"+firebaseBranch()+"_"+year+"_"+String.format(Locale.US,"%02d",month);
        double total = 0; List<Map<String,Object>> entries = new ArrayList<>();
        for(Map<String,Object> worker:workers) {
            double salary = number(worker.get("salary")); total += salary;
            Map<String,Object> line = new LinkedHashMap<>();
            line.put("workerId",worker.get("_id")); line.put("workerName",worker.get("name"));
            line.put("salary",salary); line.put("paymentMode",worker.get("paymentMode"));
            line.put("accountNo",worker.get("accountNo")); line.put("bankName",worker.get("bankName"));
            line.put("momoNumber",worker.get("momoNumber")); line.put("status","pending");
            entries.add(line);
        }
        Map<String,Object> payrollData = new LinkedHashMap<>();
        payrollData.put("branch",firebaseBranch()); payrollData.put("branchConfig",selectedBranch);
        payrollData.put("month",month); payrollData.put("year",year); payrollData.put("period",year+"-"+month);
        payrollData.put("totalAmount",total); payrollData.put("workersCount",entries.size());
        payrollData.put("entries",entries); payrollData.put("status","generated");
        payrollData.put("generatedBy",userName); payrollData.put("timestamp",System.currentTimeMillis());
        new AlertDialog.Builder(this).setTitle("Générer la paie")
                .setMessage("Période : "+year+"-"+month+"\nPersonnel : "+entries.size()+"\nTotal : "+money(total)+"\n\nCréer la paie ?")
                .setNegativeButton("Annuler",null).setPositiveButton("Générer",(dlg,w) ->
                        database.getReference("payroll").child(key).get().addOnSuccessListener(existing -> {
                            if(existing.exists()) { Toast.makeText(this,"La paie de cette période existe déjà.",Toast.LENGTH_LONG).show(); return; }
                            database.getReference("payroll").child(key).setValue(payrollData).addOnSuccessListener(x -> {
                                database.getReference("payrollHistory").push().setValue(payrollData);
                                Toast.makeText(this,"Paie générée. Vérifiez les montants avant paiement.",Toast.LENGTH_LONG).show();
                                refreshCollection("payroll"); refreshCollection("payrollHistory");
                            });
                        })).show();
    }

    private void showFinancialReportDialog(String type) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText date = new EditText(this); date.setText(today()); date.setHint("Date / période");
        EditText income = new EditText(this); income.setHint("Recettes (RWF)"); income.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText expenses = new EditText(this); expenses.setHint("Dépenses (RWF)"); expenses.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText observations = new EditText(this); observations.setHint("Observations");
        form.addView(date); form.addView(income); form.addView(expenses); form.addView(observations);
        new AlertDialog.Builder(this).setTitle("Rapport "+type).setView(form).setNegativeButton("Annuler",null)
                .setPositiveButton("Enregistrer",(dlg,w) -> {
                    String period = date.getText().toString().trim();
                    if(period.isEmpty()) { Toast.makeText(this,"Date obligatoire.",Toast.LENGTH_SHORT).show(); return; }
                    Map<String,Object> report = new LinkedHashMap<>();
                    report.put("type",type); report.put("date",period); report.put("income",number(income.getText().toString()));
                    report.put("expenses",number(expenses.getText().toString())); report.put("balance",number(income.getText().toString())-number(expenses.getText().toString()));
                    report.put("observations",observations.getText().toString().trim()); report.put("branch",firebaseBranch());
                    report.put("branchConfig",selectedBranch); report.put("createdBy",userName); report.put("timestamp",System.currentTimeMillis());
                    String collection = type.equals("daily") ? "dailyReports" : type.equals("weekly") ? "weeklyReports" : "monthlyReports";
                    String key = type+"_"+firebaseBranch()+"_"+period.replaceAll("[^A-Za-z0-9_-]","_");
                    database.getReference(collection).child(key).setValue(report).addOnSuccessListener(x -> {
                        database.getReference("reportHistory").child(key).setValue(report);
                        database.getReference("reports").child(key).setValue(report);
                        Toast.makeText(this,"Rapport enregistré.",Toast.LENGTH_SHORT).show();
                        refreshCollection("reports"); refreshCollection("reportHistory");
                    });
                }).show();
    }

    private void checkPaymentDuplicates() {
        List<Map<String,Object>> rows = filtered("studentFees");
        Map<String,Integer> counts = new LinkedHashMap<>();
        for(Map<String,Object> row:rows) {
            String key = str(row.get("studentId"))+"|"+str(row.get("paymentType"))+"|"+
                    first(str(row.get("paymentMonth")),str(row.get("paymentDate")))+"|"+str(row.get("trimester"));
            counts.put(key,counts.containsKey(key)?counts.get(key)+1:1);
        }
        int duplicates=0; for(Integer count:counts.values()) if(count>1) duplicates+=count-1;
        new AlertDialog.Builder(this).setTitle("Contrôle des doublons")
                .setMessage(duplicates==0 ? "Aucun doublon détecté pour cette branche." : duplicates+" paiement(s) potentiellement en double. Vérifiez les dossiers avant toute suppression.")
                .setPositiveButton("OK",null).show();
    }

    private void showStudentPaymentHistory() {
        List<Map<String,Object>> payments = filtered("studentFees");
        if(payments.isEmpty()) { Toast.makeText(this,"Aucun paiement élève.",Toast.LENGTH_SHORT).show(); return; }
        String[] labels = new String[payments.size()];
        for(int i=0;i<payments.size();i++) {
            Map<String,Object> p=payments.get(i);
            labels[i]=first(str(p.get("studentName")),str(p.get("studentId")))+" • "+
                    first(str(p.get("paymentType")),"Paiement")+" • "+money(number(p.get("amount")))+" • "+
                    first(str(p.get("paymentDate")),str(p.get("paymentMonth")));
        }
        new AlertDialog.Builder(this).setTitle("Historique des paiements").setItems(labels,(dialog,which) -> {
            Map<String,Object> item=payments.get(which);
            new AlertDialog.Builder(this).setTitle(labels[which]).setMessage(item.toString())
                    .setNegativeButton("Fermer",null)
                    .setPositiveButton("Modifier",(d,w) -> showRecordEditor("student-fees",item)).show();
        }).show();
    }

    private void showPayrollBankPreview() {
        List<Map<String,Object>> workers=filtered("works");
        if(workers.isEmpty()) { Toast.makeText(this,"Aucun personnel.",Toast.LENGTH_SHORT).show(); return; }
        StringBuilder report=new StringBuilder();
        for(Map<String,Object> worker:workers) {
            report.append(first(str(worker.get("name")),"Sans nom")).append("\n")
                    .append("Salaire : ").append(money(number(worker.get("salary")))).append("\n")
                    .append("Mode : ").append(first(str(worker.get("paymentMode")),"Non défini")).append("\n")
                    .append("Banque : ").append(first(str(worker.get("bankName")),"—"))
                    .append(" / Compte : ").append(first(str(worker.get("accountNo")),"—")).append("\n")
                    .append("MoMo : ").append(first(str(worker.get("momoOperator")),"—"))
                    .append(" ").append(first(str(worker.get("momoNumber")),"—")).append("\n\n");
        }
        ScrollView scroll=new ScrollView(this);
        TextView details=text(report.toString(),13,INK,false);
        details.setPadding(dp(16),dp(12),dp(16),dp(12));
        scroll.addView(details);
        new AlertDialog.Builder(this).setTitle("Contrôle des coordonnées de paie")
                .setView(scroll).setPositiveButton("Fermer",null).show();
    }

    private void showCollectionHistory(String collection) {
        List<Map<String,Object>> rows = filtered(collection);
        if(rows.isEmpty()) { Toast.makeText(this,"Aucun historique pour cette branche.",Toast.LENGTH_SHORT).show(); return; }
        String[] labels = new String[rows.size()];
        for(int i=0;i<rows.size();i++) labels[i]=first(str(rows.get(i).get("period")),str(rows.get(i).get("date")),str(rows.get(i).get("label")),str(rows.get(i).get("_id")));
        new AlertDialog.Builder(this).setTitle("Historique").setItems(labels,(dlg,which) ->
                new AlertDialog.Builder(this).setMessage(rows.get(which).toString()).setPositiveButton("Fermer",null).show()).show();
    }

    private void renderSettings() {
        addText("Paramètres du compte et de la comptabilité",16,DEEP,true);
        addText("Branche active : "+branchLabel(selectedBranch),13,INK,false);
        addAction(content,"Configurer les frais par classe",DEEP,this::showFeeConfigurationDialog);
        addAction(content,"Année scolaire et trimestre",GREEN,this::showTermConfigDialog);
        addAction(content,"Changer le mot de passe",DEEP,() -> {
            String email = auth.getCurrentUser()==null ? "" : auth.getCurrentUser().getEmail();
            if(email.isEmpty()) return;
            auth.sendPasswordResetEmail(email).addOnSuccessListener(v -> Toast.makeText(this,"Lien envoyé à "+email,Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
        });
        addAction(content,"Enregistrer le profil",GREEN,() -> {
            EditText name = new EditText(this); name.setText(userName); name.setHint("Nom affiché");
            new AlertDialog.Builder(this).setTitle("Profil comptable").setView(name).setNegativeButton("Annuler",null)
                    .setPositiveButton("Enregistrer",(dlg,w) -> {
                        String value=name.getText().toString().trim(); if(value.isEmpty()) return;
                        usersRef.child(auth.getCurrentUser().getUid()).child("name").setValue(value).addOnSuccessListener(x -> {
                            userName=value; userLabel.setText(userName+" • "+roleLabel(userRole)); Toast.makeText(this,"Profil mis à jour.",Toast.LENGTH_SHORT).show();
                        });
                    }).show();
        });
        addAction(content,"Actualiser les données",INK,this::loadExistingCollections);
        addAction(content,"Déconnexion",Color.RED,() -> new AlertDialog.Builder(this).setMessage("Se déconnecter ?")
                .setNegativeButton("Annuler",null).setPositiveButton("Déconnexion",(dlg,w) -> { auth.signOut(); finish(); }).show());
    }

    private void showFeeConfigurationDialog() {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText cls=new EditText(this); cls.setHint("Classe (ex. N1A, P2A)");
        EditText amount=new EditText(this); amount.setHint("Frais par trimestre (RWF)");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText due=new EditText(this); due.setHint("Date d'échéance / remarque");
        form.addView(cls); form.addView(amount); form.addView(due);
        new AlertDialog.Builder(this).setTitle("Configuration des frais scolaires").setView(form)
                .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(dialog,which) -> {
                    String className=cls.getText().toString().trim(); double value=number(amount.getText().toString());
                    if(className.isEmpty() || value<=0) { Toast.makeText(this,"Classe et montant obligatoires.",Toast.LENGTH_SHORT).show(); return; }
                    String key=className.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");
                    Map<String,Object> config=new LinkedHashMap<>();
                    config.put("class",className); config.put("amount",value); config.put("dueDate",due.getText().toString().trim());
                    config.put("branch",selectedBranch); config.put("academicYear",currentAcademicYear());
                    config.put("updatedBy",userName); config.put("updatedAt",System.currentTimeMillis());
                    database.getReference("feesConfiguration").child(selectedBranch).child(key).setValue(config)
                            .addOnSuccessListener(x -> { Toast.makeText(this,"Configuration enregistrée.",Toast.LENGTH_SHORT).show(); refreshCollection("feesConfiguration"); })
                            .addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
                }).show();
    }

    private void showTermConfigDialog() {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText year=new EditText(this); year.setText(currentAcademicYear()); year.setHint("Année scolaire (2026-2027)");
        EditText trimester=new EditText(this); trimester.setText("1"); trimester.setHint("Trimestre (1, 2 ou 3)");
        EditText start=new EditText(this); start.setHint("Date de début (YYYY-MM-DD)");
        EditText end=new EditText(this); end.setHint("Date de fin (YYYY-MM-DD)");
        form.addView(year); form.addView(trimester); form.addView(start); form.addView(end);
        new AlertDialog.Builder(this).setTitle("Année scolaire et trimestre").setView(form)
                .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(dialog,which) -> {
                    String academic=year.getText().toString().trim();
                    int term=(int)number(trimester.getText().toString());
                    if(academic.isEmpty() || term<1 || term>3) { Toast.makeText(this,"Année ou trimestre invalide.",Toast.LENGTH_SHORT).show(); return; }
                    Map<String,Object> config=new LinkedHashMap<>();
                    config.put("academicYear",academic); config.put("trimestre",term);
                    config.put("startDate",start.getText().toString().trim()); config.put("endDate",end.getText().toString().trim());
                    config.put("branch",selectedBranch); config.put("updatedBy",userName); config.put("updatedAt",System.currentTimeMillis());
                    database.getReference("termConfig").child(selectedBranch).setValue(config)
                            .addOnSuccessListener(x -> { Toast.makeText(this,"Période scolaire enregistrée.",Toast.LENGTH_SHORT).show(); refreshCollection("termConfig"); })
                            .addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
                }).show();
    }


    private void renderPayrollWorkspace() {
        addText("Paie du personnel • " + branchLabel(selectedBranch),18,DEEP,true);
        java.util.Calendar cal=java.util.Calendar.getInstance();
        int currentMonth=cal.get(java.util.Calendar.MONTH)+1, currentYear=cal.get(java.util.Calendar.YEAR);
        String[] monthNames={"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        Spinner month=new Spinner(this);month.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,monthNames));month.setSelection(currentMonth-1);
        Spinner year=new Spinner(this);year.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"2025","2026","2027","2028","2029","2030"}));year.setSelection(Math.max(0,Math.min(5,currentYear-2025)));
        addText("Mois",12,INK,true);content.addView(month);addText("Année",12,INK,true);content.addView(year);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);
        addAction(actions,"Générer la paie",DEEP,()->generatePayrollForPeriod(month.getSelectedItemPosition()+1,Integer.parseInt(String.valueOf(year.getSelectedItem())),false));
        addAction(actions,"Générer banque seulement",Color.rgb(23,162,184),()->generatePayrollForPeriod(month.getSelectedItemPosition()+1,Integer.parseInt(String.valueOf(year.getSelectedItem())),true));
        addAction(actions,"Enregistrer la paie",GREEN,()->saveCurrentPayroll(month.getSelectedItemPosition()+1,Integer.parseInt(String.valueOf(year.getSelectedItem()))));
        addAction(actions,"Aperçu Banque / Mobile Money",INK,this::showPayrollBankPreview);
        addAction(actions,"Historique des paies",Color.rgb(120,80,150),()->showCollectionHistory("payrollHistory"));
        addAction(actions,"Exporter la paie en PDF",Color.rgb(150,60,60),()->exportPayrollPdf(month.getSelectedItemPosition()+1,Integer.parseInt(String.valueOf(year.getSelectedItem()))));
        content.addView(actions);
        String key=payrollHistoryKey(month.getSelectedItemPosition()+1,Integer.parseInt(String.valueOf(year.getSelectedItem())));
        Map<String,Object> record=findById("payrollHistory",key);
        if(record==null){List<Map<String,Object>> history=filtered("payrollHistory");for(Map<String,Object> h:history)if(String.valueOf(h.get("month")).equals(String.valueOf(month.getSelectedItemPosition()+1))&&String.valueOf(h.get("year")).equals(String.valueOf(year.getSelectedItem()))){record=h;break;}}
        if(record==null){addText("Aucune paie générée pour cette période.",14,Color.GRAY,false);addText("Choisissez une période puis appuyez sur « Générer la paie ».",12,INK,false);return;}
        activePayrollKey=str(record.get("_id"));
        addText("Période : "+first(str(record.get("period")),monthNames[month.getSelectedItemPosition()]+" "+year.getSelectedItem()),15,DEEP,true);
        addText("Effectif : "+first(str(record.get("workersCount")),String.valueOf(payrollEntries(record).size()))+" • Total net : "+money(number(record.get("totalPayroll"))),14,INK,true);
        List<Map<String,Object>> entries=payrollEntries(record);int index=0;
        for(Map<String,Object> entry:entries){final int rowIndex=index++;String name=first(str(entry.get("name")),str(entry.get("workerName")),"Personnel");
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackgroundColor(Color.WHITE);
            addTo(card,name,14,DEEP,true);
            addTo(card,first(str(entry.get("type")),"Personnel")+" • "+first(str(entry.get("paymentMode")),"Mode non défini")+" • "+first(str(entry.get("phone")),"Sans téléphone"),11,Color.GRAY,false);
            addTo(card,"Salaire de base : "+money(number(entry.get("baseSalary")))+" • Dette : "+money(number(entry.get("debts"))),12,INK,false);
            addTo(card,"Net à payer : "+money(number(entry.get("netSalary")))+" • Statut : "+first(str(entry.get("status")),"pending"),13,DEEP,true);
            addAction(card,"Marquer comme payé",GREEN,()->updatePayrollEntryStatus(activePayrollKey,rowIndex,"paid"));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(7);content.addView(card,lp);
        }
    }

    private List<Map<String,Object>> payrollEntries(Map<String,Object> record){
        List<Map<String,Object>> out=new ArrayList<>();Object raw=record.get("entries");
        if(raw instanceof List){for(Object item:(List<?>)raw)if(item instanceof Map)out.add(new LinkedHashMap<>((Map<String,Object>)item));}
        else if(raw instanceof Map){for(Object item:((Map<?,?>)raw).values())if(item instanceof Map)out.add(new LinkedHashMap<>((Map<String,Object>)item));}
        return out;
    }

    private Map<String,Object> findById(String collection,String id){for(Map<String,Object> m:filtered(collection))if(id.equals(str(m.get("_id"))))return m;return null;}
    private String payrollHistoryKey(int month,int year){return "payroll_"+firebaseBranch()+"_"+year+"_"+month;}
    private void generatePayrollForPeriod(int month,int year,boolean bankOnly){
        List<Map<String,Object>> workers=filtered("works");List<Map<String,Object>> debts=filtered("debts");
        Map<String,Double> debtTotals=new LinkedHashMap<>();
        for(Map<String,Object>d:debts)if("pending".equalsIgnoreCase(str(d.get("status")))){String wid=str(d.get("workerId"));debtTotals.put(wid,debtTotals.getOrDefault(wid,0d)+number(d.get("amount")));}
        String[] monthNames={"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        List<Map<String,Object>> entries=new ArrayList<>();double total=0;int no=1;
        for(Map<String,Object>w:workers){
            String mode=norm(str(w.get("paymentMode")));if(bankOnly&&!mode.equals("bank"))continue;
            String wid=str(w.get("_id"));double base=number(w.get("salary")),debt=debtTotals.getOrDefault(wid,0d),net=Math.max(0,base-debt);
            String destination=mode.equals("bank")?first(str(w.get("bankName")),"Banque")+" | "+str(w.get("name")):mode.equals("momo")?first(str(w.get("momoOperator")),"MoMo")+" "+first(str(w.get("momoNumber")),str(w.get("phone")))+" | "+str(w.get("name")):first(str(w.get("phone")),"");
            Map<String,Object> e=new LinkedHashMap<>();e.put("no",first(str(w.get("no")),String.valueOf(no++)));e.put("name",str(w.get("name")));e.put("type",str(w.get("type")));e.put("phone",str(w.get("phone")));
            e.put("cardName",mode.equals("bank")?str(w.get("accountHolderName")):mode.equals("momo")?str(w.get("momoRegisteredName")):"");
            e.put("paymentMode",first(str(w.get("paymentMode")),"cash"));e.put("paymentDestination",destination);e.put("baseSalary",base);e.put("debts",debt);e.put("netSalary",net);e.put("status","pending");e.put("workerId",wid);entries.add(e);total+=net;
        }
        if(entries.isEmpty()){Toast.makeText(this,"Aucun personnel correspondant à cette branche et ce mode de paiement.",Toast.LENGTH_LONG).show();return;}
        String period=monthNames[month-1]+" "+year+(bankOnly?" (Bank Only)":"");String key=bankOnly?makeCompositeKey("payroll_bank",firebaseBranch(),year+"_"+month):payrollHistoryKey(month,year);
        Map<String,Object> payroll=new LinkedHashMap<>();payroll.put("period",period);payroll.put("month",String.valueOf(month));payroll.put("year",String.valueOf(year));payroll.put("branch",firebaseBranch());payroll.put("branchConfig",selectedBranch);
        payroll.put("entries",entries);payroll.put("totalPayroll",total);payroll.put("workersCount",entries.size());payroll.put("generatedBy",userName);payroll.put("timestamp",System.currentTimeMillis());payroll.put("status","generated");
        DatabaseReference ref=database.getReference("payrollHistory").child(key);
        ref.get().addOnSuccessListener(existing->{if(existing.exists()){Toast.makeText(this,"La paie de cette période existe déjà dans l’historique.",Toast.LENGTH_LONG).show();return;}
            ref.setValue(payroll).addOnSuccessListener(v->{Toast.makeText(this,"Paie générée et enregistrée dans l’historique.",Toast.LENGTH_LONG).show();refreshCollection("payrollHistory");}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());});
    }

    private void saveCurrentPayroll(int month,int year){
        Map<String,Object> record=findById("payrollHistory",payrollHistoryKey(month,year));
        if(record==null){Toast.makeText(this,"Générez d’abord la paie de cette période.",Toast.LENGTH_SHORT).show();return;}
        String key="saved_payroll_"+firebaseBranch()+"_"+year+"_"+month;
        Map<String,Object> saved=new LinkedHashMap<>(record);saved.remove("_id");saved.put("status","saved");saved.put("savedBy",userName);saved.put("savedAt",System.currentTimeMillis());
        DatabaseReference ref=database.getReference("payroll").child(key);
        ref.get().addOnSuccessListener(s->{if(s.exists()){Toast.makeText(this,"Cette paie est déjà enregistrée.",Toast.LENGTH_SHORT).show();return;}ref.setValue(saved).addOnSuccessListener(v->{Toast.makeText(this,"Paie sauvegardée.",Toast.LENGTH_SHORT).show();refreshCollection("payroll");});});
    }

    private void updatePayrollEntryStatus(String key,int index,String status){
        if(key==null||key.isEmpty())return;
        database.getReference("payrollHistory").child(key).child("entries").child(String.valueOf(index)).child("status").setValue(status)
                .addOnSuccessListener(v->{Toast.makeText(this,"Statut du paiement mis à jour.",Toast.LENGTH_SHORT).show();refreshCollection("payrollHistory");})
                .addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
    }

    private void exportPayrollPdf(int month,int year){
        Map<String,Object> record=findById("payrollHistory",payrollHistoryKey(month,year));if(record==null){Toast.makeText(this,"Aucune paie à exporter.",Toast.LENGTH_SHORT).show();return;}
        List<String> lines=new ArrayList<>();lines.add("EDEN FAMILY SCHOOL • PAIE");lines.add("Branche : "+branchLabel(selectedBranch));lines.add("Période : "+str(record.get("period")));lines.add("Total net : "+money(number(record.get("totalPayroll"))));lines.add("");
        for(Map<String,Object>e:payrollEntries(record)){lines.add(first(str(e.get("no")),"")+" • "+str(e.get("name")));lines.add("Base : "+money(number(e.get("baseSalary")))+" | Dette : "+money(number(e.get("debts")))+" | Net : "+money(number(e.get("netSalary"))));lines.add("Mode : "+str(e.get("paymentMode"))+" | Statut : "+str(e.get("status")));lines.add("");}
        exportPdf("Paie_"+year+"_"+month,lines);
    }

    private void renderExpenses(){
        addText("Journal des dépenses",20,DEEP,true);
        addText("Enregistrement, contrôle et suivi des sorties de caisse de la branche active.",12,Color.GRAY,false);
        List<Map<String,Object>> expenses=filtered("dailyExpenses");
        double total=0;for(Map<String,Object> e:expenses)total+=number(e.get("amount"));
        addStat("Nombre de dépenses",String.valueOf(expenses.size()),"−");
        addStat("Total des dépenses",money(total),"₣");
        addAction(content,"Nouvelle dépense",DEEP,this::showStandaloneExpenseDialog);
        addAction(content,"Actualiser",GREEN,()->refreshCollection("dailyExpenses"));
        if(expenses.isEmpty()){addText("Aucune dépense enregistrée pour cette branche.",13,Color.GRAY,false);return;}
        java.util.Collections.sort(expenses,(a,b)->Long.compare((long)number(b.get("timestamp")),(long)number(a.get("timestamp"))));
        for(Map<String,Object> e:expenses){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackgroundColor(Color.WHITE);
            addTo(card,first(str(e.get("category")),str(e.get("description")),"Dépense sans catégorie"),14,DEEP,true);
            addTo(card,money(number(e.get("amount")))+"  •  "+first(str(e.get("date")),"Date non précisée"),13,INK,true);
            addTo(card,"Ajouté par : "+first(str(e.get("addedBy")),str(e.get("createdBy")),"—"),11,Color.GRAY,false);
            LinearLayout actions=new LinearLayout(this);
            addAction(actions,"Modifier",DEEP,()->editDailyExpense(e,()->render("expenses")));
            addAction(actions,"Archiver",Color.RED,()->new AlertDialog.Builder(this).setMessage("Archiver cette dépense ?").setNegativeButton("Annuler",null).setPositiveButton("Archiver",(dlg,w)->archiveDailyExpense(e)).show());
            card.addView(actions);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);content.addView(card,lp);
        }
    }

    private void showStandaloneExpenseDialog(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText category=new EditText(this);category.setHint("Catégorie / justification");form.addView(category);
        EditText amount=new EditText(this);amount.setHint("Montant (RWF)");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);form.addView(amount);
        EditText date=new EditText(this);date.setHint("Date (AAAA-MM-JJ)");date.setText(today());form.addView(date);
        EditText note=new EditText(this);note.setHint("Observation / référence");form.addView(note);
        new AlertDialog.Builder(this).setTitle("Enregistrer une dépense").setView(form).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(dlg,w)->{
            String c=category.getText().toString().trim(),day=date.getText().toString().trim();double value=number(amount.getText().toString());
            if(c.isEmpty()||day.isEmpty()||value<=0){Toast.makeText(this,"Catégorie, date et montant positif obligatoires.",Toast.LENGTH_LONG).show();return;}
            Map<String,Object> expense=new LinkedHashMap<>();expense.put("category",c);expense.put("description",note.getText().toString().trim());expense.put("amount",value);expense.put("date",day);expense.put("addedBy",userName);expense.put("branch",firebaseBranch());expense.put("branchConfig",selectedBranch);expense.put("timestamp",System.currentTimeMillis());expense.put("status","recorded");
            database.getReference("dailyExpenses").push().setValue(expense).addOnSuccessListener(x->{Toast.makeText(this,"Dépense enregistrée.",Toast.LENGTH_SHORT).show();refreshCollection("dailyExpenses");}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
        }).show();
    }

    private void renderFinancialReports(){
        addText("Rapports financiers détaillés • "+branchLabel(selectedBranch),18,DEEP,true);
        LinearLayout modes=new LinearLayout(this);addAction(modes,"Journalier",DEEP,()->{reportMode="daily";render(active);});addAction(modes,"Hebdomadaire",GREEN,()->{reportMode="weekly";render(active);});addAction(modes,"Mensuel",INK,()->{reportMode="monthly";render(active);});content.addView(modes);
        String defaultValue=reportMode.equals("daily")?today():reportMode.equals("weekly")?weekKeyToday():today().substring(0,7);
        EditText period=new EditText(this);period.setText(reportDate.isEmpty()?defaultValue:reportDate);period.setHint(reportMode.equals("daily")?"Date YYYY-MM-DD":reportMode.equals("weekly")?"Semaine YYYY-W##":"Mois YYYY-MM");content.addView(period);
        EditText opening=new EditText(this);opening.setHint("Solde d’ouverture (RWF)");opening.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);content.addView(opening);
        EditText observations=new EditText(this);observations.setHint("Observations générales");observations.setMinLines(2);content.addView(observations);
        LinearLayout summary=new LinearLayout(this);summary.setOrientation(LinearLayout.VERTICAL);content.addView(summary);
        Runnable calculate=()->{reportDate=period.getText().toString().trim();summary.removeAllViews();Map<String,Object> totals=calculateReportPeriod(reportMode,reportDate,number(opening.getText().toString()));for(String k:new String[]{"fees","transport","coaching","uniform","other","totalIncome","totalExpenses","closingBalance","difference"})addTo(summary,reportLabel(k)+" : "+money(number(totals.get(k))),13,INK,true);};
        Button calc=new Button(this);calc.setText("Calculer / Actualiser");calc.setAllCaps(false);calc.setOnClickListener(v->calculate.run());content.addView(calc);
        if(reportMode.equals("daily"))renderDailyExpenseEditor(period,calculate);
        Button save=new Button(this);save.setText(reportMode.equals("daily")?"Enregistrer le rapport journalier":"Enregistrer le rapport");save.setAllCaps(false);save.setOnClickListener(v->saveFinancialReportDetailed(reportMode,period.getText().toString().trim(),number(opening.getText().toString()),observations.getText().toString().trim(),calculate));content.addView(save);
        Button pdf=new Button(this);pdf.setText("Exporter le rapport PDF");pdf.setAllCaps(false);pdf.setOnClickListener(v->{Map<String,Object> totals=calculateReportPeriod(reportMode,period.getText().toString().trim(),number(opening.getText().toString()));List<String> lines=new ArrayList<>();lines.add("RAPPORT FINANCIER • "+reportMode.toUpperCase(Locale.ROOT));lines.add("Branche : "+branchLabel(selectedBranch));lines.add("Période : "+period.getText());for(Map.Entry<String,Object>e:totals.entrySet())lines.add(reportLabel(e.getKey())+" : "+money(number(e.getValue())));lines.add("Observations : "+observations.getText());exportPdf("Rapport_"+reportMode+"_"+period.getText(),lines);});content.addView(pdf);
        calculate.run();
    }

    private void renderDailyExpenseEditor(EditText period,Runnable calculate){
        addText("Dépenses de la journée",15,DEEP,true);
        EditText category=new EditText(this);category.setHint("Justification / catégorie (Salaires, Loyer, Eau…)");content.addView(category);
        EditText amount=new EditText(this);amount.setHint("Montant (RWF)");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);content.addView(amount);
        Button add=new Button(this);add.setText("Ajouter une dépense");add.setAllCaps(false);add.setOnClickListener(v->{String c=category.getText().toString().trim(),date=period.getText().toString().trim();double a=number(amount.getText().toString());if(c.isEmpty()||a<=0||date.isEmpty()){Toast.makeText(this,"Catégorie, montant et date obligatoires.",Toast.LENGTH_SHORT).show();return;}Map<String,Object> expense=new LinkedHashMap<>();expense.put("category",c);expense.put("amount",a);expense.put("date",date);expense.put("addedBy",userName);expense.put("branch",firebaseBranch());expense.put("timestamp",System.currentTimeMillis());database.getReference("dailyExpenses").push().setValue(expense).addOnSuccessListener(x->{Toast.makeText(this,"Dépense ajoutée.",Toast.LENGTH_SHORT).show();refreshCollection("dailyExpenses");category.setText("");amount.setText("");calculate.run();});});content.addView(add);
        for(Map<String,Object> e:filtered("dailyExpenses"))if(period.getText().toString().trim().equals(str(e.get("date")))){addText(str(e.get("category"))+" • "+money(number(e.get("amount"))),12,INK,false);addAction(content,"Modifier / supprimer : "+str(e.get("_id")),Color.rgb(150,60,60),()->editDailyExpense(e,calculate));}
    }

    private void editDailyExpense(Map<String,Object> old,Runnable calculate){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);EditText c=new EditText(this);c.setText(str(old.get("category")));EditText a=new EditText(this);a.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);a.setText(str(old.get("amount")));form.addView(c);form.addView(a);
        new AlertDialog.Builder(this).setTitle("Modifier la dépense").setView(form).setNegativeButton("Annuler",null).setNeutralButton("Supprimer",(d,w)->archiveDailyExpense(old)).setPositiveButton("Enregistrer",(d,w)->{Map<String,Object> up=new LinkedHashMap<>();up.put("category",c.getText().toString().trim());up.put("amount",number(a.getText().toString()));up.put("updatedAt",System.currentTimeMillis());database.getReference("dailyExpenses").child(str(old.get("_id"))).updateChildren(up).addOnSuccessListener(v->{refreshCollection("dailyExpenses");calculate.run();});}).show();
    }

    private void archiveDailyExpense(Map<String,Object> old){
        String id=str(old.get("_id"));Map<String,Object> trash=new LinkedHashMap<>();trash.put("source","dailyExpenses");trash.put("originalPath","dailyExpenses/"+id);Map<String,Object> dataCopy=new LinkedHashMap<>(old);dataCopy.remove("_id");trash.put("originalData",dataCopy);trash.put("label",str(old.get("category")));trash.put("reason","Suppression d’une dépense journalière");trash.put("deletedAt",System.currentTimeMillis());trash.put("deletedBy",userName);trash.put("branch",firebaseBranch());
        database.getReference("trash").push().setValue(trash).addOnSuccessListener(v->database.getReference("dailyExpenses").child(id).removeValue().addOnSuccessListener(x->refreshCollection("dailyExpenses")));
    }

    private Map<String,Object> calculateReportPeriod(String mode,String period,double opening){
        List<String> dates=new ArrayList<>();
        try{
            if(mode.equals("daily"))dates.add(period);
            else if(mode.equals("weekly")){String[] p=period.split("-W");int year=Integer.parseInt(p[0]),week=Integer.parseInt(p[1]);java.util.Calendar c=java.util.Calendar.getInstance();c.clear();c.set(java.util.Calendar.YEAR,year);c.set(java.util.Calendar.WEEK_OF_YEAR,week);c.set(java.util.Calendar.DAY_OF_WEEK,java.util.Calendar.MONDAY);for(int i=0;i<7;i++){dates.add(new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime()));c.add(java.util.Calendar.DATE,1);}}
            else {String[] p=period.split("-");int y=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]);java.util.Calendar c=java.util.Calendar.getInstance();c.clear();c.set(y,m-1,1);int days=c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);for(int i=0;i<days;i++){dates.add(new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime()));c.add(java.util.Calendar.DATE,1);}}
        }catch(Exception e){dates.clear();if(!period.isEmpty()&&mode.equals("daily"))dates.add(period);}
        Map<String,Object> total=new LinkedHashMap<>();for(String k:new String[]{"fees","transport","coaching","uniform","other","totalIncome","totalExpenses","closingBalance","difference"})total.put(k,0d);
        double running=opening;List<Map<String,Object>> payments=filtered("studentFees"),trans=filtered("transportPayments"),expenses=filtered("dailyExpenses");
        for(String date:dates){double fees=0,transport=0,coaching=0,uniform=0,other=0,expense=0;
            for(Map<String,Object> p:payments){if(!date.equals(paymentDate(p)))continue;double a=number(p.get("amount"));String type=norm(str(p.get("paymentType")));if(type.equals("fees")||type.equals("minervale"))fees+=a;else if(type.equals("transport"))transport+=a;else if(type.equals("coaching"))coaching+=a;else if(type.equals("uniforme"))uniform+=a;else other+=a;}
            for(Map<String,Object> p:trans)if(date.equals(paymentDate(p)))transport+=number(p.get("amount"));
            for(Map<String,Object> e:expenses)if(date.equals(str(e.get("date"))))expense+=number(e.get("amount"));
            double income=fees+transport+coaching+uniform+other;running+=income-expense;
            total.put("fees",number(total.get("fees"))+fees);total.put("transport",number(total.get("transport"))+transport);total.put("coaching",number(total.get("coaching"))+coaching);total.put("uniform",number(total.get("uniform"))+uniform);total.put("other",number(total.get("other"))+other);total.put("totalIncome",number(total.get("totalIncome"))+income);total.put("totalExpenses",number(total.get("totalExpenses"))+expense);total.put("difference",number(total.get("difference"))+income-expense);
        }
        total.put("closingBalance",running);return total;
    }

    private void saveFinancialReportDetailed(String mode,String period,double opening,String observations,Runnable recalc){
        if(period.isEmpty()){Toast.makeText(this,"Période obligatoire.",Toast.LENGTH_SHORT).show();return;}
        Map<String,Object> summary=calculateReportPeriod(mode,period,opening);Map<String,Object> report=new LinkedHashMap<>(summary);report.put("type",mode);report.put("date",period);report.put("period",period);report.put("branch",selectedBranch);report.put("branchConfig",selectedBranch);report.put("openingBalance",opening);report.put("generalObservations",observations);report.put("observations",observations);report.put("preparedBy",userName);report.put("generatedBy",userName);report.put("timestamp",System.currentTimeMillis());report.put("status","draft");
        String collection=mode.equals("daily")?"dailyReports":mode.equals("weekly")?"weeklyReports":"monthlyReports";String key=makeCompositeKey(mode,selectedBranch,period);
        database.getReference(collection).child(key).setValue(report).addOnSuccessListener(v->{database.getReference("reportHistory").child(key).setValue(report);database.getReference("reports").child(key).setValue(report);if(mode.equals("daily"))database.getReference("openingBalances").child(firebaseBranch()).child(period).setValue(opening);database.getReference("closingBalances").child(firebaseBranch()).child(period).setValue(number(summary.get("closingBalance")));Toast.makeText(this,"Rapport enregistré dans Firebase.",Toast.LENGTH_SHORT).show();refreshCollection(collection);refreshCollection("reportHistory");}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
    }

    private String makeCompositeKey(String prefix,String branch,String period){String b=(branch==null||branch.trim().isEmpty()?"unknown":branch).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");String q=(period==null||period.trim().isEmpty()?String.valueOf(System.currentTimeMillis()):period).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");return prefix+"_"+b+"_"+q;}
    private String paymentDate(Map<String,Object> p){return first(str(p.get("paymentDate")),str(p.get("date")),str(p.get("timestamp"))).length()>=10?first(str(p.get("paymentDate")),str(p.get("date")),str(p.get("timestamp"))).substring(0,10):"";}
    private String reportLabel(String key){switch(key){case "fees":return "Frais scolaires";case "transport":return "Transport";case "coaching":return "Coaching";case "uniform":return "Uniformes";case "other":return "Autres recettes";case "totalIncome":return "Total recettes";case "totalExpenses":return "Total dépenses";case "closingBalance":return "Solde de clôture";case "difference":return "Différence";default:return key;}}
    private String weekKeyToday(){java.util.Calendar c=java.util.Calendar.getInstance();return c.get(java.util.Calendar.YEAR)+"-W"+String.format(Locale.US,"%02d",c.get(java.util.Calendar.WEEK_OF_YEAR));}

    private void exportCurrentSectionCsv(){
        String collection=active.equals("payroll")?"payrollHistory":active.equals("reports")?"reports":collectionFor(active);List<Map<String,Object>> rows=filtered(collection);
        if(rows.isEmpty()){Toast.makeText(this,"Aucune donnée à exporter pour cette branche.",Toast.LENGTH_SHORT).show();return;}
        java.util.LinkedHashSet<String> keys=new java.util.LinkedHashSet<>();for(Map<String,Object>r:rows)for(String k:r.keySet())if(!k.startsWith("_"))keys.add(k);
        StringBuilder csv=new StringBuilder();List<String> header=new ArrayList<>(keys);for(int i=0;i<header.size();i++){if(i>0)csv.append(';');csv.append(csvCell(header.get(i)));}csv.append('\n');
        for(Map<String,Object>r:rows){for(int i=0;i<header.size();i++){if(i>0)csv.append(';');csv.append(csvCell(str(r.get(header.get(i)))));}csv.append('\n');}
        pendingExportBytes=csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);pendingExportMime="text/csv";Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/csv");intent.putExtra(Intent.EXTRA_TITLE,active+"_"+selectedBranch+"_"+today()+".csv");startActivityForResult(intent,REQUEST_ACCOUNTANT_EXPORT);
    }
    private String csvCell(String value){String v=value==null?"":value.replace("\"","\"\"");return "\""+v+"\"";}
    private void exportPdf(String title,List<String> lines){
        try{PdfDocument pdf=new PdfDocument();android.graphics.Paint paint=new android.graphics.Paint();paint.setTextSize(11);paint.setColor(Color.BLACK);PdfDocument.Page page=null;android.graphics.Canvas canvas=null;int pageNo=1,y=48;
            for(String line:lines){if(page==null||y>790){if(page!=null)pdf.finishPage(page);page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo++).create());canvas=page.getCanvas();y=48;}String s=line==null?"":line;canvas.drawText(s.length()>90?s.substring(0,90):s,36,y,paint);y+=18;}
            if(page!=null)pdf.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();pdf.writeTo(out);pdf.close();pendingExportBytes=out.toByteArray();pendingExportMime="application/pdf";Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,title.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");startActivityForResult(i,REQUEST_ACCOUNTANT_PDF);
        }catch(Exception e){Toast.makeText(this,"Export PDF impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        if(requestCode==REQUEST_ACCOUNTANT_EXPORT||requestCode==REQUEST_ACCOUNTANT_PDF){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out!=null&&pendingExportBytes!=null)out.write(pendingExportBytes);Toast.makeText(this,"Fichier enregistré.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}pendingExportBytes=null;}
    }

    private String currentAcademicYear() {
        java.util.Calendar c=java.util.Calendar.getInstance(); int y=c.get(java.util.Calendar.YEAR);
        return c.get(java.util.Calendar.MONTH)>=8 ? y+"-"+(y+1) : (y-1)+"-"+y;
    }
    private String today() { return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date()); }

    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
}