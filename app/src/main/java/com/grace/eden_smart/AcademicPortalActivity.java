package com.grace.eden_smart;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.grace.eden_smart.service.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AcademicPortalActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(27,44,126), BG=Color.rgb(247,248,252), MUTED=Color.rgb(100,116,139);
    private static final int REQUEST_MEDIA=8101, REQUEST_DOC=8102, REQUEST_PDF=8103;
    private static final String[] TAB_KEYS={"assignments","tests","exams","vacation","reports","classes","photos","presentations","deliberation"};
    private static final String[] TAB_LABELS={"Devoirs","Tests","Examens","Travaux vacances","Bulletins","Classes","Photos","Présentations","Délibération"};
    private FirebaseAuth auth;
    private DatabaseReference db, users;
    private String branch="", userName="", academicYear="", activeTab="assignments";
    private LinearLayout page, tabs, content, list;
    private TextView status;
    private final List<String> classes=new ArrayList<>();
    private final List<Map<String,Object>> rows=new ArrayList<>();
    private final List<Map<String,Object>> bulletinRows=new ArrayList<>();
    private final List<Map<String,Object>> mediaRows=new ArrayList<>();
    private final Map<String,Map<String,Object>> classRows=new LinkedHashMap<>();
    private final Map<String,Map<String,Object>> studentRows=new LinkedHashMap<>();
    private final Map<String,Map<String,Object>> teacherRows=new LinkedHashMap<>();
    private String editingId="", editingCollection="", pendingUploadKind="";
    private EditText dialogFileUrl;
    private byte[] pendingPdfBytes;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY); getWindow().setNavigationBarColor(NAVY);
        FirebaseService service=FirebaseService.getInstance(getApplicationContext());
        auth=service.getAuth(); db=service.getDatabase().getReference(); users=service.getUsersReference();
        if(auth.getCurrentUser()==null){finish();return;}
        academicYear=activeYear();
        buildShell();
        verifyProfile();
    }

    private void buildShell(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        page=column();page.setPadding(dp(14),dp(14),dp(14),dp(26));scroll.addView(page);setContentView(scroll);
        KeyboardInsets.apply(this);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(label("GESTION ACADÉMIQUE",19,NAVY,true),new LinearLayout.LayoutParams(0,-2,1));
        MaterialButton back=button("Retour",false);back.setOnClickListener(v->finish());top.addView(back);page.addView(top);
        status=label("Chargement du profil…",12,MUTED,false);status.setPadding(0,dp(5),0,dp(8));page.addView(status);
        ScrollView tabScroll=new ScrollView(this);tabScroll.setFillViewport(false);tabScroll.setHorizontalScrollBarEnabled(false);
        tabScroll.setLayoutParams(matchWrap());tabScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabScroll.addView(tabs);page.addView(tabScroll);
        content=column();LinearLayout.LayoutParams cp=matchWrap();cp.topMargin=dp(12);page.addView(content,cp);
        buildTabs();
    }

    private void verifyProfile(){
        users.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot s){
                String role=norm(first(val(s,"role"),val(s,"userRole"),val(s,"type")));
                if(!role.equals("secretary")&&!role.equals("secretaire")&&!role.equals("secretariat")){Toast.makeText(AcademicPortalActivity.this,"Accès réservé au secrétariat.",Toast.LENGTH_LONG).show();finish();return;}
                branch=first(val(s,"branchConfig"),val(s,"branch"),val(s,"branche"),val(s,"branchName"),val(s,"schoolBranch"));
                userName=first(val(s,"name"),val(s,"nom"),val(s,"email"),"Secrétariat");
                if(branch.isEmpty()){status.setText("Aucune branche attribuée.");return;}
                loadBaseData();
            }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Profil inaccessible : "+e.getMessage());}
        });
    }

    private void loadBaseData(){
        loadClasses();
        loadPeople();
        loadTab(activeTab);
    }

    private void loadClasses(){
        db.child("classes").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){
                classes.clear();classRows.clear();
                for(DataSnapshot s:root.getChildren()){
                    Map<String,Object> m=map(s);if(m==null)continue;
                    String b=first(str(m.get("branch")),str(m.get("branche")),str(m.get("branchName")),str(m.get("schoolBranch")));
                    String n=first(str(m.get("name")),str(m.get("className")),str(m.get("class")),str(m.get("classe")));
                    if(!n.isEmpty()&&scopeMatches(m)){classes.add(n);m.put("_id",s.getKey());classRows.put(s.getKey(),m);}
                }
                Collections.sort(classes,String.CASE_INSENSITIVE_ORDER);buildTabs();if(activeTab.equals("classes")||activeTab.equals("deliberation"))renderTab();
            }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Classes indisponibles : "+e.getMessage());}
        });
    }

    private void loadPeople(){
        db.child("students").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){
                studentRows.clear();
                for(DataSnapshot s:root.getChildren()){Map<String,Object> m=map(s);if(m!=null&&scopeMatches(m)){m.put("_id",s.getKey());studentRows.put(s.getKey(),m);}}
                if(activeTab.equals("classes")||activeTab.equals("deliberation"))renderTab();
            }
            @Override public void onCancelled(@NonNull DatabaseError e){}
        });
        users.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){
                teacherRows.clear();
                for(DataSnapshot s:root.getChildren()){Map<String,Object> m=map(s);if(m==null)continue;String role=norm(first(str(m.get("role")),str(m.get("userRole"))));if((role.equals("teacher")||role.equals("enseignant")||role.equals("enseignante"))&&scopeMatches(m)){m.put("_id",s.getKey());teacherRows.put(s.getKey(),m);}}
            }
            @Override public void onCancelled(@NonNull DatabaseError e){}
        });
    }

    private void buildTabs(){
        if(tabs==null)return;tabs.removeAllViews();
        for(int i=0;i<TAB_KEYS.length;i++){
            final String key=TAB_KEYS[i];MaterialButton b=button(TAB_LABELS[i],key.equals(activeTab));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(42));lp.setMargins(0,0,dp(6),0);tabs.addView(b,lp);
            b.setOnClickListener(v->{activeTab=key;editingId="";buildTabs();loadTab(key);});
        }
    }

    private void loadTab(String tab){
        status.setText("Branche : "+branch+" • Année scolaire : "+academicYear);
        if(tab.equals("assignments"))loadAcademicRecords("assignments");
        else if(tab.equals("tests"))loadAcademicRecords("tests");
        else if(tab.equals("exams"))loadAcademicRecords("exams");
        else if(tab.equals("vacation"))loadAcademicRecords("vacationWorks");
        else if(tab.equals("reports"))loadBulletins();
        else if(tab.equals("classes"))renderTab();
        else if(tab.equals("photos"))loadMedia("photo");
        else if(tab.equals("presentations"))loadMedia("presentation");
        else if(tab.equals("deliberation"))loadDeliberationHome();
    }

    private void loadAcademicRecords(String collection){
        editingCollection=collection;status.setText("Lecture de /"+collection+"…");
        db.child(collection).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){
                rows.clear();
                for(DataSnapshot s:root.getChildren()){
                    Map<String,Object> m=map(s);if(m==null)continue;
                    String b=first(str(m.get("branch")),str(m.get("branche")));
                    String cls=first(str(m.get("classes")),str(m.get("class")),str(m.get("classe")));
                    if(!classes.contains(cls))continue;
                    if(!b.isEmpty()&&!sameBranch(b,branch))continue;
                    String year=first(str(m.get("academicYear")),str(m.get("schoolYear")),academicYear);
                    if(!year.equals(academicYear))continue;
                    m.put("_id",s.getKey());rows.add(m);
                }
                renderTab();
            }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Lecture refusée : "+e.getMessage());}
        });
    }

    private void renderTab(){
        if(content==null)return;content.removeAllViews();
        switch(activeTab){
            case "assignments":case "tests":case "exams":case "vacation":renderAcademicList();break;
            case "reports":renderBulletins();break;
            case "classes":renderClasses();break;
            case "photos":case "presentations":renderMedia();break;
            case "deliberation":renderDeliberationHome();break;
        }
    }

    private void renderAcademicList(){
        String title=activeTab.equals("assignments")?"Gestion des devoirs":activeTab.equals("tests")?"Gestion des tests":activeTab.equals("exams")?"Gestion des examens":"Travaux de vacances";
        cardTitle(title);
        MaterialButton add=button(activeTab.equals("vacation")?"+ Ajouter un travail":"+ Ajouter",true);
        add.setOnClickListener(v->showAcademicEditor(null));content.addView(add);
        Spinner filter=spinnerWithClasses(true);content.addView(filter,matchWrap());
        list=column();content.addView(list);
        int count=0;String selected=String.valueOf(filter.getSelectedItem());
        for(Map<String,Object> m:rows){
            String cls=first(str(m.get("classes")),str(m.get("class")),str(m.get("classe")));
            if(!selected.equals("Toutes les classes")&&!selected.equals(cls))continue;
            count++;MaterialCardView c=card();LinearLayout b=column();b.setPadding(dp(12),dp(12),dp(12),dp(12));
            b.addView(label(first(str(m.get("title")),"Sans titre"),15,NAVY,true));
            b.addView(label(cls+" • "+str(m.get("subject")),12,MUTED,false));
            b.addView(label(str(m.get("description")),13,Color.DKGRAY,false));
            String date=activeTab.equals("assignments")?str(m.get("dueDate")):activeTab.equals("vacation")?first(str(m.get("deadline")),str(m.get("period"))):str(m.get("date"));
            b.addView(label("Date : "+first(date,"Non précisée"),12,MUTED,false));
            if(activeTab.equals("exams"))b.addView(label("Salle : "+first(str(m.get("room")),"—")+" • Durée : "+first(str(m.get("duration")),"—")+" min",12,MUTED,false));
            if(activeTab.equals("vacation"))b.addView(label("Période : "+str(m.get("period"))+" • Type : "+str(m.get("type")),12,MUTED,false));
            if(!str(m.get("fileUrl")).isEmpty()){MaterialButton open=button("Ouvrir le document",false);open.setOnClickListener(v->openUrl(str(m.get("fileUrl"))));b.addView(open);}
            LinearLayout actions=new LinearLayout(this);MaterialButton edit=button("Modifier",false),del=button("Supprimer",false);
            edit.setOnClickListener(v->showAcademicEditor(m));del.setOnClickListener(v->confirmDeleteAcademic(m));
            actions.addView(edit);actions.addView(del);b.addView(actions);c.addView(b);LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(8);list.addView(c,lp);
        }
        status.setText(count+" enregistrement(s) • "+branch+" • "+academicYear);
        filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){renderTab();}});
    }

    private void showAcademicEditor(Map<String,Object> existing){
        editingId=existing==null?"":str(existing.get("_id"));
        final String collection=collectionForTab(activeTab);
        LinearLayout form=column();form.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText title=input("Titre *");title.setText(existing==null?"":str(existing.get("title")));form.addView(title);
        Spinner cls=spinnerWithClasses(false);select(cls,existing==null?"":first(str(existing.get("classes")),str(existing.get("class"))));form.addView(cls,matchWrap());
        EditText subject=input("Matière");subject.setText(existing==null?"":str(existing.get("subject")));form.addView(subject);
        EditText date=input(activeTab.equals("vacation")?"Date limite (AAAA-MM-JJ)":"Date (AAAA-MM-JJ HH:MM)");
        date.setText(existing==null?"":activeTab.equals("vacation")?str(existing.get("deadline")):activeTab.equals("assignments")?str(existing.get("dueDate")):str(existing.get("date")));
        date.setInputType(InputType.TYPE_CLASS_DATETIME);form.addView(date);
        EditText description=input("Description / instructions");description.setMinLines(3);description.setGravity(Gravity.TOP);description.setText(existing==null?"":str(existing.get("description")));form.addView(description);
        EditText room=null,duration=null,period=null,type=null;
        if(activeTab.equals("exams")){room=input("Salle");room.setText(existing==null?"":str(existing.get("room")));form.addView(room);duration=input("Durée en minutes");duration.setInputType(InputType.TYPE_CLASS_NUMBER);duration.setText(existing==null?"":str(existing.get("duration")));form.addView(duration);}
        if(activeTab.equals("vacation")){period=input("Période de vacances");period.setText(existing==null?"":str(existing.get("period")));form.addView(period);type=input("Type : Devoir / Projet / Révision / Lecture");type.setText(existing==null?"":str(existing.get("type")));form.addView(type);}
        final EditText roomField=room, durationField=duration, periodField=period, typeField=type;
        dialogFileUrl=input("URL du document (facultatif)");dialogFileUrl.setText(existing==null?"":str(existing.get("fileUrl")));form.addView(dialogFileUrl);
        MaterialButton attach=button("Joindre PDF / image",false);attach.setOnClickListener(v->{pendingUploadKind=collection;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQUEST_DOC);});form.addView(attach);
        ScrollView sc=new ScrollView(this);sc.addView(form);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(existing==null?"Nouvel enregistrement":"Modifier l’enregistrement").setView(sc).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",null).create();
        dlg.setOnShowListener(v->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn->{
            String t=title.getText().toString().trim(),className=String.valueOf(cls.getSelectedItem());
            if(t.isEmpty()||classes.isEmpty()||className.equals("Aucune classe")){title.setError("Titre obligatoire");return;}
            Map<String,Object> data=new LinkedHashMap<>();data.put("title",t);data.put("classes",className);data.put("subject",subject.getText().toString().trim());
            data.put("description",description.getText().toString().trim());data.put("fileUrl",dialogFileUrl.getText().toString().trim());
            if(activeTab.equals("assignments"))data.put("dueDate",date.getText().toString().trim());else if(activeTab.equals("vacation")){data.put("deadline",date.getText().toString().trim());data.put("period",periodField.getText().toString().trim());data.put("type",typeField.getText().toString().trim());}else data.put("date",date.getText().toString().trim());
            if(roomField!=null)data.put("room",roomField.getText().toString().trim());if(durationField!=null)data.put("duration",durationField.getText().toString().trim());
            data.put("branch",branch);data.put("academicYear",academicYear);data.put("updatedAt",isoNow());data.put("updatedBy",userName);
            if(existing==null){data.put("createdAt",isoNow());data.put("createdBy",auth.getCurrentUser().getEmail()==null?userName:auth.getCurrentUser().getEmail());}
            DatabaseReference ref=editingId.isEmpty()?db.child(collection).push():db.child(collection).child(editingId);
            String id=ref.getKey();ref.updateChildren(data).addOnCompleteListener(task->{
                if(!task.isSuccessful()){Toast.makeText(this,"Enregistrement impossible : "+task.getException(),Toast.LENGTH_LONG).show();return;}
                if(existing==null)mirrorAcademic(collection,id,data);else updateAcademicMirror(collection,existing,data);
                dlg.dismiss();Toast.makeText(this,"Enregistrement effectué.",Toast.LENGTH_SHORT).show();loadAcademicRecords(collection);
            });
        }));
        dlg.show();
    }

    private void mirrorAcademic(String collection,String id,Map<String,Object> data){
        String cls=str(data.get("classes"));String classBranch=branchForClass(cls);
        String sub=collection.equals("assignments")?"devoirs":collection.equals("tests")?"exercices":collection.equals("exams")?"examens":"vacances";
        Map<String,Object> mirror=new LinkedHashMap<>();mirror.put("titre",data.get("title"));mirror.put("branche",classBranch.isEmpty()?branch:classBranch);
        mirror.put("classe",cls);mirror.put("date",first(str(data.get("dueDate")),str(data.get("deadline")),str(data.get("date")),str(data.get("period"))));
        mirror.put("description",data.get("description"));mirror.put("fichierUrl",data.get("fileUrl"));
        DatabaseReference ref=db.child("activites").child(sub).push();ref.setValue(mirror).addOnSuccessListener(v->db.child(collection).child(id).child("mirrorId").setValue(ref.getKey()));
    }

    private void updateAcademicMirror(String collection,Map<String,Object> old,Map<String,Object> data){
        String mirrorId=str(old.get("mirrorId"));if(mirrorId.isEmpty())return;
        String sub=collection.equals("assignments")?"devoirs":collection.equals("tests")?"exercices":collection.equals("exams")?"examens":"vacances";
        Map<String,Object> m=new LinkedHashMap<>();m.put("titre",data.get("title"));m.put("branche",branchForClass(str(data.get("classes"))));m.put("classe",data.get("classes"));
        m.put("date",first(str(data.get("dueDate")),str(data.get("deadline")),str(data.get("date")),str(data.get("period"))));m.put("description",data.get("description"));m.put("fichierUrl",data.get("fileUrl"));
        db.child("activites").child(sub).child(mirrorId).updateChildren(m);
    }

    private void confirmDeleteAcademic(Map<String,Object> m){
        String collection=collectionForTab(activeTab),id=str(m.get("_id")),mirror=str(m.get("mirrorId"));
        new AlertDialog.Builder(this).setTitle("Supprimer").setMessage("Supprimer « "+str(m.get("title"))+" » ?")
                .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->db.child(collection).child(id).removeValue().addOnSuccessListener(v->{
                    String sub=collection.equals("assignments")?"devoirs":collection.equals("tests")?"exercices":collection.equals("exams")?"examens":"vacances";
                    if(!mirror.isEmpty())db.child("activites").child(sub).child(mirror).removeValue();loadAcademicRecords(collection);
                }).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show())).show();
    }

    private void loadBulletins(){
        bulletinRows.clear();status.setText("Recherche des bulletins publiés…");
        List<String> paths=new ArrayList<>();List<String> classUniverse=new ArrayList<>(classes);
        if(classUniverse.isEmpty())classUniverse.addAll(Arrays.asList("P1A","P1B","P1C","P2A","P2B","P3","P4","P5","N1","N1A","N1B","N2","N3"));
        for(String cls:classUniverse)for(String p:bulletinCandidates(branch,cls))if(!paths.contains(p))paths.add(p);
        readBulletinPath(paths,0);
    }

    private void readBulletinPath(List<String> paths,int index){
        if(index>=paths.size()){deduplicateBulletins();renderTab();return;}
        db.child(paths.get(index)).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){
                for(DataSnapshot s:root.getChildren()){Map<String,Object> m=map(s);if(m==null)continue;m.put("_id",s.getKey());m.put("_path",paths.get(index));if(bulletinMatches(m))bulletinRows.add(m);}
                readBulletinPath(paths,index+1);
            }
            @Override public void onCancelled(@NonNull DatabaseError e){readBulletinPath(paths,index+1);}
        });
    }

    private List<String> bulletinCandidates(String branch,String cls){
        String b=norm(branch),c=norm(cls);List<String> p=new ArrayList<>();
        if(Arrays.asList("p1a","p1b","p1c","p2a","p2b","p3","p4","p5").contains(c))p.add("bulletins_primaire_"+c);
        if(Arrays.asList("n1","n1a","n1b","n2","n3").contains(c))p.add("bulletins_maternelle_"+c);
        if(b.startsWith("kacyiru")){p.add("bulletins_maternelle_"+c);p.add("bulletins_kacyiru_"+c);}
        if(b.startsWith("kimisagara")){p.add("bulletins_kimisagara_"+c);p.add("bulletins/maternelle/kimisagara/"+c);}
        if(b.startsWith("gisozi")){p.add("bulletins_gisozi_"+c);p.add("bulletins_primaire_"+c);}
        p.add("bulletins_primaire_"+c);p.add("bulletins_maternelle_"+c);if(!b.isEmpty())p.add("bulletins_"+b+"_"+c);
        return p;
    }

    private boolean bulletinMatches(Map<String,Object> m){
        String b=first(str(m.get("branch")),str(m.get("branche")),str(m.get("schoolBranch")));
        String cls=first(str(m.get("class")),str(m.get("classe")));
        if(!b.isEmpty()&&!sameBranch(b,branch))return false;
        if(norm(branch).endsWith("maternelle")&&!cls.isEmpty()&&!norm(cls).startsWith("n"))return false;
        if(norm(branch).endsWith("primaire")&&!cls.isEmpty()&&!norm(cls).startsWith("p"))return false;
        String y=first(str(m.get("academicYear")),str(m.get("anneeAcademique")),str(m.get("schoolYear")),str(m.get("anneeScolaire")));
        if(!y.isEmpty()&&!normalizeYear(y).equals(normalizeYear(academicYear)))return false;
        return true;
    }

    private void deduplicateBulletins(){
        Map<String,Map<String,Object>> unique=new LinkedHashMap<>();
        for(Map<String,Object> m:bulletinRows){
            String key=first(str(m.get("studentId")),str(m.get("studentUid")),str(m.get("studentName")),str(m.get("name")))+"|"+
                    first(str(m.get("class")),str(m.get("classe")))+"|"+first(str(m.get("trimester")),str(m.get("trimestre")))+"|"+first(str(m.get("annee")),str(m.get("academicYear")),academicYear);
            Map<String,Object> old=unique.get(key);if(old==null||timestamp(m)>=timestamp(old))unique.put(key,m);
        }
        bulletinRows.clear();bulletinRows.addAll(unique.values());
    }

    private void renderBulletins(){
        cardTitle("Historique des bulletins scolaires");
        LinearLayout tools=new LinearLayout(this);
        MaterialButton refresh=button("Actualiser",false);refresh.setOnClickListener(v->loadBulletins());tools.addView(refresh);
        MaterialButton missing=button("Notes manquantes",false);missing.setOnClickListener(v->showMissingNotes());tools.addView(missing);
        MaterialButton dup=button("Analyser doublons",false);dup.setOnClickListener(v->showBulletinDuplicates());tools.addView(dup);
        MaterialButton all=button("Print All / PDF",true);all.setOnClickListener(v->exportBulletinsPdf(bulletinRows));tools.addView(all);content.addView(tools);
        Spinner classFilter=spinnerWithClasses(true);content.addView(classFilter,matchWrap());
        Spinner term=spinner(new String[]{"Tous les trimestres","1","2","3","BN"});content.addView(term,matchWrap());
        TextView summary=label("Élèves : "+studentRows.size()+" • Bulletins : "+bulletinRows.size(),12,MUTED,false);content.addView(summary);
        list=column();content.addView(list);
        renderBulletinCards(classFilter,term);
        android.widget.AdapterView.OnItemSelectedListener listener=new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){renderBulletinCards(classFilter,term);}};
        classFilter.setOnItemSelectedListener(listener);term.setOnItemSelectedListener(listener);
    }

    private void renderBulletinCards(Spinner classFilter,Spinner term){
        list.removeAllViews();String cf=String.valueOf(classFilter.getSelectedItem()),tf=String.valueOf(term.getSelectedItem());int count=0;
        for(Map<String,Object> m:bulletinRows){
            String cls=first(str(m.get("class")),str(m.get("classe")));String tr=first(str(m.get("trimester")),str(m.get("trimestre")));
            if(!cf.equals("Toutes les classes")&&!norm(cf).equals(norm(cls)))continue;
            if(!tf.equals("Tous les trimestres")&&!normalizeTerm(tf).equals(normalizeTerm(tr)))continue;
            count++;String name=first(str(m.get("studentName")),str(m.get("name")),str(m.get("studentId")),"Élève inconnu");
            MaterialCardView c=card();LinearLayout b=column();b.setPadding(dp(12),dp(12),dp(12),dp(12));
            b.addView(label(name,15,NAVY,true));b.addView(label("Classe : "+cls+" • Trimestre : "+tr+" • Année : "+first(str(m.get("annee")),str(m.get("academicYear")),academicYear),12,MUTED,false));
            b.addView(label("Moyenne : "+first(str(m.get("average")),str(m.get("moyenne")),calculateAverage(m)),13,NAVY,true));
            LinearLayout actions=new LinearLayout(this);MaterialButton preview=button("Présentation",false),pdf=button("PDF",true);
            preview.setOnClickListener(v->showBulletinPreview(m));pdf.setOnClickListener(v->exportBulletinsPdf(Arrays.asList(m)));
            actions.addView(preview);actions.addView(pdf);b.addView(actions);c.addView(b);LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(8);list.addView(c,lp);
        }
        summaryCount(count);
    }

    private void summaryCount(int count){status.setText(count+" bulletin(s) correspondant aux filtres • "+branch+" • "+academicYear);}

    private void showBulletinPreview(Map<String,Object> m){
        String student=first(str(m.get("studentName")),str(m.get("name")),str(m.get("studentId")),"Élève");
        String cls=first(str(m.get("class")),str(m.get("classe")),"—");
        String term=first(str(m.get("trimester")),str(m.get("trimestre")),"—");
        String year=first(str(m.get("annee")),str(m.get("academicYear")),str(m.get("schoolYear")),academicYear);
        ScrollView scroll=new ScrollView(this);
        LinearLayout sheet=column();sheet.setPadding(dp(16),dp(12),dp(16),dp(18));sheet.setBackgroundColor(Color.WHITE);scroll.addView(sheet);
        TextView school=label("EDEN FAMILY SCHOOL",19,NAVY,true);school.setGravity(Gravity.CENTER);sheet.addView(school);
        TextView heading=label("BULLETIN SCOLAIRE",16,Color.DKGRAY,true);heading.setGravity(Gravity.CENTER);sheet.addView(heading);
        TextView branchLine=label(branch+"  •  ANNÉE SCOLAIRE "+year,11,MUTED,true);branchLine.setGravity(Gravity.CENTER);sheet.addView(branchLine);
        MaterialCardView identity=card();LinearLayout identityBox=column();identityBox.setPadding(dp(12),dp(10),dp(12),dp(10));
        identityBox.addView(label("ÉLÈVE : "+student,15,NAVY,true));
        identityBox.addView(label("CLASSE : "+cls+"     •     PÉRIODE : "+term,12,MUTED,true));
        identityBox.addView(label("ENSEIGNANT : "+first(str(m.get("teacherName")),str(m.get("teacher")), "—"),12,MUTED,false));
        String studentId=first(str(m.get("studentId")),str(m.get("studentUid")),str(m.get("uid")));
        Map<String,Object> studentRecord=studentRows.get(studentId);
        if(studentRecord!=null){
            identityBox.addView(label("DATE DE NAISSANCE : "+first(str(studentRecord.get("birthDate")),str(studentRecord.get("dateOfBirth")),"—")+"   •   SEXE : "+first(str(studentRecord.get("gender")),"—"),11,MUTED,false));
            identityBox.addView(label("PARENT / TUTEUR : "+first(str(studentRecord.get("parentName")),str(studentRecord.get("guardianName")),"—")+"   •   TÉL. : "+first(str(studentRecord.get("parentPhone")),str(studentRecord.get("guardianPhone")),"—"),11,MUTED,false));
            identityBox.addView(label("IDENTIFIANT : "+first(studentId,"—"),10,MUTED,false));
        }
        identity.addView(identityBox);sheet.addView(identity,matchWrap());

        Object rawScores=m.get("scores");List<?> scoreRows=rawScores instanceof List?(List<?>)rawScores:rawScores instanceof Map?new ArrayList<>(((Map<?,?>)rawScores).values()):new ArrayList<>();
        MaterialCardView tableCard=card();LinearLayout table=column();table.setPadding(dp(10),dp(10),dp(10),dp(10));
        table.addView(label("RÉSULTATS PAR MATIÈRE",13,NAVY,true));
        if(scoreRows.isEmpty()){
            table.addView(label("Les détails des notes ne sont pas présents dans cet enregistrement.",12,MUTED,false));
        }else{
            for(Object value:scoreRows){
                if(!(value instanceof Map))continue;
                Map<?,?> row=(Map<?,?>)value;
                String subject=first(str(row.get("subject")),str(row.get("name")),str(row.get("matiere")),"Matière");
                table.addView(label(subject,13,Color.DKGRAY,true));
                for(String key:new String[]{"t1","t2","t3"}){
                    Object termValue=row.get(key);if(!(termValue instanceof Map))continue;
                    Map<?,?> tm=(Map<?,?>)termValue;
                    String termLabel=key.equals("t1")?"1er trimestre":key.equals("t2")?"2e trimestre":"3e trimestre";
                    String test=str(tm.get("test")),exam=str(tm.get("ex")),total=str(tm.get("total"));
                    if(test.isEmpty()&&exam.isEmpty()&&total.isEmpty())continue;
                    table.addView(label(termLabel+"  •  Test : "+first(test,"—")+"  •  Examen : "+first(exam,"—")+"  •  Total : "+first(total,"—"),11,MUTED,false));
                }
                table.addView(label("Maximum : "+first(str(row.get("maxTotal")),str(row.get("annualMax")),"—")+"    |    Total obtenu : "+first(str(row.get("annualTotal")), "—"),11,NAVY,false));
                View divider=new View(this);divider.setBackgroundColor(Color.rgb(225,230,238));table.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
            }
        }
        tableCard.addView(table);sheet.addView(tableCard,matchWrap());

        MaterialCardView result=card();LinearLayout resultBox=column();resultBox.setPadding(dp(12),dp(10),dp(12),dp(10));
        resultBox.addView(label("SYNTHÈSE ET DÉCISION",13,NAVY,true));
        resultBox.addView(label("MOYENNE / POURCENTAGE : "+first(str(m.get("average")),str(m.get("moyenne")),str(m.get("generalAverage")),str(m.get("annualPct")),str(m.get("percentage")),calculateAverage(m)),14,Color.DKGRAY,true));
        resultBox.addView(label("TOTAL ANNUEL : "+first(str(m.get("annualTotal")),str(m.get("totalAnnual")),str(m.get("total")),"—")+" / "+first(str(m.get("annualMax")),str(m.get("maxGrand")),"—"),12,MUTED,false));
        resultBox.addView(label("MOYENNES TRIMESTRIELLES : T1 "+first(str(m.get("trim1Average")),str(m.get("t1Average")),"—")+"  •  T2 "+first(str(m.get("trim2Average")),str(m.get("t2Average")),"—")+"  •  T3 "+first(str(m.get("trim3Average")),str(m.get("t3Average")),"—"),11,MUTED,false));
        resultBox.addView(label("RANGS : T1 "+first(str(m.get("trim1Place")),"—")+"  •  T2 "+first(str(m.get("trim2Place")),"—")+"  •  T3 "+first(str(m.get("trim3Place")),"—")+"  •  Annuel "+first(str(m.get("place")),str(m.get("rank")),"—"),11,MUTED,false));
        resultBox.addView(label("Grade : "+first(str(m.get("grade")),str(m.get("mention")),"—"),12,MUTED,false));
        resultBox.addView(label("Rang : "+first(str(m.get("rank")),str(m.get("place")),"—"),12,MUTED,false));
        resultBox.addView(label("Conduite : "+first(str(m.get("conduite")),str(m.get("conduct")),"—"),12,MUTED,false));
        resultBox.addView(label("Décision : "+first(str(m.get("remarqueDelib")),str(m.get("decision")),str(m.get("deliberation")),"—"),12,MUTED,false));
        resultBox.addView(label("Date du bulletin : "+first(str(m.get("bulletinDate")),str(m.get("date")), "—"),12,MUTED,false));
        result.addView(resultBox);sheet.addView(result,matchWrap());

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Présentation du bulletin").setView(scroll)
                .setNegativeButton("Fermer",null).setNeutralButton("Exporter PDF",(x,w)->exportBulletinsPdf(Arrays.asList(m))).create();
        dialog.show();
    }

    private void showMissingNotes(){
        List<String> missing=new ArrayList<>();
        for(Map<String,Object> m:bulletinRows){String n=first(str(m.get("studentName")),str(m.get("name")),str(m.get("studentId")));
            Object notes=m.get("notes"),grades=m.get("grades"),subjects=m.get("subjects");
            boolean has=(notes instanceof Map&&!((Map<?,?>)notes).isEmpty())||(grades instanceof Map&&!((Map<?,?>)grades).isEmpty())||(subjects instanceof Map&&!((Map<?,?>)subjects).isEmpty());
            if(!has)missing.add(n+" • "+first(str(m.get("class")),str(m.get("classe")))+" • "+first(str(m.get("trimester")),str(m.get("trimestre"))));
        }
        new AlertDialog.Builder(this).setTitle("Analyse des notes manquantes").setMessage(missing.isEmpty()?"Aucun bulletin sans notes détecté.":missing.size()+" bulletin(s) à vérifier :\n\n"+android.text.TextUtils.join("\n",missing))
                .setPositiveButton("Fermer",null).setNeutralButton("Exporter PDF",(d,w)->exportTextPdf("Notes manquantes",missing)).show();
    }

    private void showBulletinDuplicates(){
        Map<String,Integer> counts=new LinkedHashMap<>();for(Map<String,Object> m:bulletinRows){String key=first(str(m.get("studentId")),str(m.get("studentUid")),str(m.get("studentName")))+"|"+first(str(m.get("class")),str(m.get("classe")))+"|"+first(str(m.get("trimester")),str(m.get("trimestre")));counts.put(key,counts.getOrDefault(key,0)+1);}
        int dup=0;for(int n:counts.values())if(n>1)dup+=n-1;
        new AlertDialog.Builder(this).setTitle("Analyse des doublons").setMessage(dup==0?"Aucun doublon détecté.":dup+" doublon(s) potentiel(s) détecté(s). Vérifiez les chemins source avant toute suppression.")
                .setPositiveButton("Fermer",null).show();
    }

    private void exportBulletinsPdf(List<Map<String,Object>> source){
        List<String> lines=new ArrayList<>();for(Map<String,Object> m:source){lines.add("BULLETIN SCOLAIRE");lines.add("Élève : "+first(str(m.get("studentName")),str(m.get("name"))));lines.add("Classe : "+first(str(m.get("class")),str(m.get("classe"))));lines.add("Trimestre : "+first(str(m.get("trimester")),str(m.get("trimestre"))));lines.add("Année : "+first(str(m.get("annee")),str(m.get("academicYear")),academicYear));lines.add("Moyenne : "+calculateAverage(m));for(Map.Entry<String,Object> e:m.entrySet())if(!e.getKey().startsWith("_"))lines.add(e.getKey()+" : "+String.valueOf(e.getValue()));lines.add("----------------------------------------");}
        exportTextPdf("Bulletins_"+branch+"_"+academicYear,lines);
    }

    private void exportTextPdf(String title,List<String> lines){
        try{
            PdfDocument pdf=new PdfDocument();int pageNo=1,y=50;PdfDocument.Page pagePdf=null;android.graphics.Canvas canvas=null;android.graphics.Paint paint=new android.graphics.Paint();
            for(String line:lines){if(pagePdf==null||y>790){if(pagePdf!=null)pdf.finishPage(pagePdf);PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,pageNo++).create();pagePdf=pdf.startPage(info);canvas=pagePdf.getCanvas();y=50;paint.setTextSize(12);paint.setColor(Color.BLACK);}
                String txt=line==null?"":line;canvas.drawText(txt.length()>85?txt.substring(0,85):txt,40,y,paint);y+=18;}
            if(pagePdf!=null)pdf.finishPage(pagePdf);ByteArrayOutputStream out=new ByteArrayOutputStream();pdf.writeTo(out);pdf.close();pendingPdfBytes=out.toByteArray();
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,title.replaceAll("[^A-Za-z0-9_-]","_")+".pdf");startActivityForResult(i,REQUEST_PDF);
        }catch(Exception e){Toast.makeText(this,"Création PDF impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void renderClasses(){
        cardTitle("Gestion des classes");
        MaterialButton add=button("Créer une classe",true);add.setOnClickListener(v->showClassEditor(null));content.addView(add);
        MaterialButton refresh=button("Actualiser",false);refresh.setOnClickListener(v->loadClasses());content.addView(refresh);
        for(Map.Entry<String,Map<String,Object>> entry:classRows.entrySet()){
            Map<String,Object> m=entry.getValue();String id=entry.getKey(),name=first(str(m.get("name")),str(m.get("className")),str(m.get("class")));
            int students=0,teachers=0;for(Map<String,Object>s:studentRows.values())if(studentInClass(s,name))students++;for(Map<String,Object>t:teacherRows.values())if(teacherHasClass(t,name))teachers++;
            MaterialCardView c=card();LinearLayout b=column();b.setPadding(dp(12),dp(12),dp(12),dp(12));
            b.addView(label(name,16,NAVY,true));b.addView(label("Niveau : "+str(m.get("level"))+" • Capacité : "+str(m.get("capacity")),12,MUTED,false));
            b.addView(label("Élèves : "+students+" • Enseignants : "+teachers+" • Année : "+first(str(m.get("academicYear")),str(m.get("schoolYear")),academicYear),12,MUTED,false));
            LinearLayout actions=new LinearLayout(this);MaterialButton details=button("Détails",false),edit=button("Modifier",false),del=button("Supprimer",false);
            details.setOnClickListener(v->showClassDetails(id,m));edit.setOnClickListener(v->showClassEditor(m));del.setOnClickListener(v->deleteClass(id,name));
            actions.addView(details);actions.addView(edit);actions.addView(del);b.addView(actions);c.addView(b);LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(8);content.addView(c,lp);
        }
    }

    private void showClassEditor(Map<String,Object> old){
        String id=old==null?"":str(old.get("_id"));LinearLayout form=column();form.setPadding(dp(14),dp(8),dp(14),dp(8));
        EditText name=input("Nom de la classe");name.setText(old==null?"":str(old.get("name")));form.addView(name);
        EditText level=input("Niveau (Maternelle / Primaire)");level.setText(old==null?"":str(old.get("level")));form.addView(level);
        EditText capacity=input("Capacité");capacity.setInputType(InputType.TYPE_CLASS_NUMBER);capacity.setText(old==null?"":str(old.get("capacity")));form.addView(capacity);
        EditText year=input("Année scolaire");year.setText(old==null?academicYear:first(str(old.get("academicYear")),str(old.get("schoolYear")),academicYear));form.addView(year);
        new AlertDialog.Builder(this).setTitle(old==null?"Créer une classe":"Modifier la classe").setView(form).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
            String n=name.getText().toString().trim(),lv=level.getText().toString().trim();if(n.isEmpty()||lv.isEmpty()){Toast.makeText(this,"Nom et niveau obligatoires.",Toast.LENGTH_SHORT).show();return;}
            Map<String,Object> data=new LinkedHashMap<>();data.put("name",n);data.put("className",n);data.put("level",lv);data.put("branch",baseBranch(branch));data.put("branchConfig",branch);data.put("capacity",(int)num(capacity.getText().toString()));data.put("academicYear",year.getText().toString().trim());data.put("schoolYear",year.getText().toString().trim());data.put("updatedAt",isoNow());
            DatabaseReference ref=id.isEmpty()?db.child("classes").push():db.child("classes").child(id);String key=ref.getKey();if(id.isEmpty()){data.put("classId",key);data.put("dateCreated",isoNow());}
            ref.updateChildren(data).addOnSuccessListener(v->{if(old!=null&&!norm(str(old.get("name"))).equals(norm(n)))cascadeClassRename(id,str(old.get("name")),n);Toast.makeText(this,"Classe enregistrée.",Toast.LENGTH_SHORT).show();loadClasses();})
                    .addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
        }).show();
    }

    private void cascadeClassRename(String id,String oldName,String newName){
        Map<String,Object> updates=new HashMap<>();
        for(Map.Entry<String,Map<String,Object>> e:studentRows.entrySet()){Map<String,Object>s=e.getValue();if(!studentInClass(s,oldName))continue;String sid=e.getKey();updates.put("students/"+sid+"/class",newName);updates.put("students/"+sid+"/classe",newName);updates.put("students/"+sid+"/classes",Arrays.asList(newName));updates.put("students/"+sid+"/classId",id);}
        for(Map.Entry<String,Map<String,Object>> e:teacherRows.entrySet()){Map<String,Object>t=e.getValue();Object cs=t.get("classes");if(cs instanceof List){List<Object> next=new ArrayList<>((List<?>)cs);boolean changed=false;for(int i=0;i<next.size();i++)if(norm(str(next.get(i))).equals(norm(oldName))){next.set(i,newName);changed=true;}if(changed)updates.put("users/"+e.getKey()+"/classes",next);}else if(norm(str(cs)).equals(norm(oldName)))updates.put("users/"+e.getKey()+"/classes",newName);}
        db.child("inscriptions-scolaires").addListenerForSingleValueEvent(new ValueEventListener(){@Override public void onDataChange(@NonNull DataSnapshot root){for(DataSnapshot s:root.getChildren()){Map<String,Object>m=map(s);if(m!=null&&norm(first(str(m.get("class")),str(m.get("classe")))).equals(norm(oldName)))updates.put("inscriptions-scolaires/"+s.getKey()+"/class",newName);}if(!updates.isEmpty())db.updateChildren(updates);}@Override public void onCancelled(@NonNull DatabaseError e){}});
    }

    private void deleteClass(String id,String name){
        new AlertDialog.Builder(this).setTitle("Supprimer la classe").setMessage("Supprimer "+name+" de /classes ? Les dossiers élèves ne seront pas effacés.")
                .setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->db.child("classes").child(id).removeValue().addOnSuccessListener(v->{Toast.makeText(this,"Classe supprimée.",Toast.LENGTH_SHORT).show();loadClasses();})).show();
    }

    private void showClassDetails(String id,Map<String,Object> cls){
        String name=first(str(cls.get("name")),str(cls.get("className")));
        LinearLayout box=column();box.setPadding(dp(14),dp(8),dp(14),dp(8));
        box.addView(label("Élèves inscrits dans "+name,15,NAVY,true));
        List<Map<String,Object>> students=new ArrayList<>();for(Map<String,Object>s:studentRows.values())if(studentInClass(s,name))students.add(s);
        for(Map<String,Object>s:students)box.addView(label("• "+first(str(s.get("name")),str(s.get("nom")),str(s.get("fullName")))+" — "+first(str(s.get("teacherName")),"Sans enseignant"),12,MUTED,false));
        MaterialButton assign=button("Affecter des élèves à cette classe",true);assign.setOnClickListener(v->assignStudentsToClass(id,name));box.addView(assign);
        box.addView(label("Enseignants affectés",15,NAVY,true));
        for(Map<String,Object>t:teacherRows.values())if(teacherHasClass(t,name))box.addView(label("• "+first(str(t.get("name")),str(t.get("fullName"))),12,MUTED,false));
        MaterialButton assignTeacher=button("Affecter un enseignant",false);assignTeacher.setOnClickListener(v->assignTeacherToClass(name));box.addView(assignTeacher);
        ScrollView sc=new ScrollView(this);sc.addView(box);new AlertDialog.Builder(this).setTitle("Détails de classe").setView(sc).setPositiveButton("Fermer",null).show();
    }

    private void assignStudentsToClass(String classId,String className){
        List<Map<String,Object>> all=new ArrayList<>(studentRows.values());if(all.isEmpty()){Toast.makeText(this,"Aucun élève disponible.",Toast.LENGTH_SHORT).show();return;}
        String[] labels=new String[all.size()];boolean[] checked=new boolean[all.size()];
        for(int i=0;i<all.size();i++){Map<String,Object>s=all.get(i);labels[i]=first(str(s.get("name")),str(s.get("nom")),str(s.get("fullName")))+" • "+first(str(s.get("class")),str(s.get("classe")),"sans classe");checked[i]=studentInClass(s,className);}
        new AlertDialog.Builder(this).setTitle("Affecter les élèves").setMultiChoiceItems(labels,checked,(d,which,isChecked)->checked[which]=isChecked)
                .setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{Map<String,Object> updates=new HashMap<>();for(int i=0;i<all.size();i++){String sid=str(all.get(i).get("_id"));if(checked[i]){updates.put("students/"+sid+"/class",className);updates.put("students/"+sid+"/classe",className);updates.put("students/"+sid+"/classes",Arrays.asList(className));updates.put("students/"+sid+"/classId",classId);updates.put("students/"+sid+"/branch",baseBranch(branch));updates.put("students/"+sid+"/branchConfig",branch);updates.put("students/"+sid+"/academicYear",academicYear);}}if(updates.isEmpty())return;db.updateChildren(updates).addOnSuccessListener(v->{Toast.makeText(this,"Affectations enregistrées.",Toast.LENGTH_SHORT).show();loadPeople();}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}).show();
    }

    private void assignTeacherToClass(String className){
        List<String> ids=new ArrayList<>(),names=new ArrayList<>();for(Map.Entry<String,Map<String,Object>> e:teacherRows.entrySet()){ids.add(e.getKey());names.add(first(str(e.getValue().get("name")),str(e.getValue().get("fullName")),e.getKey()));}
        if(ids.isEmpty()){Toast.makeText(this,"Aucun enseignant dans cette branche.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("Affecter un enseignant à "+className).setItems(names.toArray(new String[0]),(d,w)->{
            String id=ids.get(w);Map<String,Object> t=teacherRows.get(id);List<String> current=new ArrayList<>();Object old=t.get("classes");if(old instanceof List)for(Object v:(List<?>)old)current.add(str(v));else if(old!=null&&!str(old).isEmpty())current.addAll(Arrays.asList(str(old).split(",")));if(!current.contains(className))current.add(className);
            db.child("users").child(id).child("classes").setValue(current).addOnSuccessListener(v->Toast.makeText(this,"Enseignant affecté.",Toast.LENGTH_SHORT).show());
        }).show();
    }

    private void loadMedia(String type){
        status.setText("Chargement de /academic_documents…");db.child("academic_documents").orderByChild("type").equalTo(type).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){mediaRows.clear();for(DataSnapshot s:root.getChildren()){Map<String,Object>m=map(s);if(m==null)continue;String b=first(str(m.get("branch")),str(m.get("branche")));if(!b.isEmpty()&&!sameBranch(b,branch))continue;m.put("_id",s.getKey());mediaRows.add(m);}renderTab();}
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Médias indisponibles : "+e.getMessage());}
        });
    }

    private void renderMedia(){
        String type=activeTab.equals("photos")?"photo":"presentation";cardTitle(type.equals("photo")?"Photos de l’école":"Présentations vidéo");
        MaterialButton link=button("Publier un lien",true);link.setOnClickListener(v->showMediaLinkEditor(type));content.addView(link);
        MaterialButton upload=button(type.equals("photo")?"Téléverser une photo":"Téléverser une vidéo",false);upload.setOnClickListener(v->{pendingUploadKind=type;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQUEST_MEDIA);});content.addView(upload);
        for(Map<String,Object>m:mediaRows){MaterialCardView c=card();LinearLayout b=column();b.setPadding(dp(12),dp(12),dp(12),dp(12));b.addView(label(first(str(m.get("title")),type),14,NAVY,true));b.addView(label(first(str(m.get("classe")),"Toutes les classes")+" • "+first(str(m.get("branch")),str(m.get("branche")),branch),12,MUTED,false));String url=first(str(m.get("downloadUrl")),str(m.get("directUrl")),str(m.get("originalUrl")),str(m.get("driveLink")),str(m.get("previewUrl")));if(!url.isEmpty()){MaterialButton open=button("Ouvrir",false);open.setOnClickListener(v->openUrl(url));b.addView(open);}MaterialButton del=button("Supprimer",false);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Supprimer cette publication ?").setNegativeButton("Annuler",null).setPositiveButton("Supprimer",(d,w)->db.child("academic_documents").child(str(m.get("_id"))).removeValue().addOnSuccessListener(x->loadMedia(type))).show());b.addView(del);c.addView(b);LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(8);content.addView(c,lp);}
        status.setText(mediaRows.size()+" publication(s) • "+branch);
    }

    private void showMediaLinkEditor(String type){
        LinearLayout form=column();form.setPadding(dp(14),dp(8),dp(14),dp(8));EditText title=input("Titre");form.addView(title);EditText url=input(type.equals("photo")?"URL directe de l’image":"Lien YouTube / TikTok");url.setInputType(InputType.TYPE_TEXT_VARIATION_URI);form.addView(url);EditText cls=input("Classe (facultatif)");form.addView(cls);
        new AlertDialog.Builder(this).setTitle("Publier un lien").setView(form).setNegativeButton("Annuler",null).setPositiveButton("Publier",(d,w)->saveMedia(type,title.getText().toString().trim(),cls.getText().toString().trim(),url.getText().toString().trim(),"link","external")).show();
    }

    private void saveMedia(String type,String title,String cls,String url,String sourceType,String provider){
        if(url.isEmpty()){Toast.makeText(this,"Lien obligatoire.",Toast.LENGTH_SHORT).show();return;}
        if(type.equals("presentation")&&!(url.contains("youtube.com")||url.contains("youtu.be")||url.contains("tiktok.com"))&&sourceType.equals("link")){Toast.makeText(this,"Utilisez un lien YouTube ou TikTok.",Toast.LENGTH_LONG).show();return;}
        String preview=url;if(type.equals("presentation")&&url.contains("youtu")){String id=youtubeId(url);if(!id.isEmpty())preview="https://www.youtube.com/embed/"+id;}
        Map<String,Object> doc=new LinkedHashMap<>();doc.put("title",first(title,type.equals("photo")?"Photo":"Présentation"));doc.put("type",type);doc.put("classe",cls);doc.put("branch",branch);doc.put("branche",branch);doc.put("sourceType",sourceType);doc.put("provider",provider);doc.put("originalUrl",url);doc.put("directUrl",url);doc.put("previewUrl",preview);doc.put("downloadUrl",url);doc.put("driveLink",url);doc.put("uploadedBy",auth.getCurrentUser().getEmail()==null?userName:auth.getCurrentUser().getEmail());doc.put("uploadedAt",System.currentTimeMillis());doc.put("academicYear",academicYear);
        db.child("academic_documents").push().setValue(doc).addOnSuccessListener(v->{Toast.makeText(this,"Publication enregistrée.",Toast.LENGTH_SHORT).show();loadMedia(type);}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
    }

    private void uploadMedia(Uri uri,String type,String name){
        String safe=name.replaceAll("[^A-Za-z0-9._-]","_");String path=(type.equals("photo")?"photos/":"presentations/")+System.currentTimeMillis()+"_"+safe;
        status.setText("Téléversement vers Firebase Storage…");
        FirebaseStorage.getInstance().getReference().child(path).putFile(uri).continueWithTask(t->{if(!t.isSuccessful())throw t.getException()==null?new Exception("Upload échoué"):t.getException();return t.getResult().getStorage().getDownloadUrl();})
                .addOnSuccessListener(download->{saveMedia(type,name,"",download.toString(),"upload","firebase-storage");})
                .addOnFailureListener(e->{status.setText("Échec du téléversement : "+e.getMessage());Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();});
    }

    private void loadDeliberationHome(){
        status.setText("Délibération • "+branch+" • "+academicYear);renderTab();
    }

    private void renderDeliberationHome(){
        cardTitle("Délibération de fin de période");
        content.addView(label("Décidez, classe par classe, qui est admis, en repêchage ou redouble.",13,MUTED,false));
        Spinner year=spinner(new String[]{academicYear,previousYear(),nextYear()});content.addView(year,matchWrap());
        for(String cls:classes){MaterialCardView c=card();LinearLayout b=column();b.setPadding(dp(12),dp(12),dp(12),dp(12));int count=0;for(Map<String,Object>s:studentRows.values())if(studentInClass(s,cls))count++;b.addView(label(cls,15,NAVY,true));b.addView(label(count+" élève(s)",12,MUTED,false));MaterialButton open=button("Ouvrir la délibération",true);open.setOnClickListener(v->openDeliberationClass(cls,String.valueOf(year.getSelectedItem())));b.addView(open);c.addView(b);LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(7);content.addView(c,lp);}
    }

    private void openDeliberationClass(String className,String year){
        db.child("deliberations").child(year).child(norm(branch)).child(className).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot saved){
                Map<String,Object> decisions=map(saved);if(decisions==null)decisions=new LinkedHashMap<>();showDeliberationWorkspace(className,year,decisions);
            }
            @Override public void onCancelled(@NonNull DatabaseError e){Toast.makeText(AcademicPortalActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();}
        });
    }

    private void showDeliberationWorkspace(String className,String year,Map<String,Object> decisions){
        List<Map<String,Object>> students=new ArrayList<>();for(Map<String,Object>s:studentRows.values())if(studentInClass(s,className))students.add(s);
        LinearLayout body=column();body.setPadding(dp(12),dp(8),dp(12),dp(8));body.addView(label(className+" • "+year,16,NAVY,true));
        Map<String,Map<String,Object>> newResults=new LinkedHashMap<>();
        for(Map<String,Object>s:students){
            String sid=str(s.get("_id"));Object old=decisions.get(sid);Map<String,Object> res=old instanceof Map?new LinkedHashMap<>((Map<String,Object>)old):new LinkedHashMap<>();
            String avg=first(str(res.get("average")),str(res.get("moyenne")),str(s.get("average")));
            MaterialCardView card=card();LinearLayout row=column();row.setPadding(dp(10),dp(10),dp(10),dp(10));
            row.addView(label(first(str(s.get("name")),str(s.get("nom")),str(s.get("fullName"))),14,NAVY,true));row.addView(label("Moyenne : "+first(avg,"À calculer")+" • Décision : "+first(str(res.get("decision")),"À vérifier"),12,MUTED,false));
            MaterialButton decide=button("Décision / commentaire",false);decide.setOnClickListener(v->showDecisionEditor(className,year,s,res));row.addView(decide);card.addView(row);body.addView(card);newResults.put(sid,res);
        }
        MaterialButton save=button("Enregistrer la délibération de la classe",true);save.setOnClickListener(v->{
            Map<String,Object> payload=new LinkedHashMap<>();for(Map.Entry<String,Map<String,Object>>e:newResults.entrySet())payload.put(e.getKey(),e.getValue());
            Map<String,Object> meta=new LinkedHashMap<>();meta.put("status","done");meta.put("decidedBy",userName);meta.put("decidedAt",System.currentTimeMillis());payload.put("__meta",meta);
            db.child("deliberations").child(year).child(norm(branch)).child(className).setValue(payload).addOnSuccessListener(x->Toast.makeText(this,"Délibération enregistrée.",Toast.LENGTH_SHORT).show()).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
        });body.addView(save);
        MaterialButton apply=button("Appliquer les promotions des élèves admis",false);apply.setOnClickListener(v->applyDeliberationPromotions(className,year,newResults));body.addView(apply);
        ScrollView sc=new ScrollView(this);sc.addView(body);new AlertDialog.Builder(this).setTitle("Délibération • "+className).setView(sc).setPositiveButton("Fermer",null).show();
    }

    private void showDecisionEditor(String className,String year,Map<String,Object> student,Map<String,Object> existing){
        String sid=str(student.get("_id"));String[] options={"A_VERIFIER","ADMIS","REPECHAGE","REDOUBLE"};Spinner decision=spinner(options);select(decision,first(str(existing.get("decision")),"A_VERIFIER"));
        Spinner target=spinnerWithClasses(false);select(target,str(existing.get("nextClass")));
        EditText average=input("Moyenne générale");average.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);average.setText(first(str(existing.get("average")),str(existing.get("moyenne")),str(student.get("average"))));
        EditText comment=input("Observation / commentaire");comment.setMinLines(2);comment.setText(str(existing.get("comment")));
        LinearLayout f=column();f.addView(label(first(str(student.get("name")),str(student.get("nom")),str(student.get("fullName"))),15,NAVY,true));f.addView(label("Décision",12,MUTED,false));f.addView(decision,matchWrap());f.addView(label("Classe suivante",12,MUTED,false));f.addView(target,matchWrap());f.addView(average);f.addView(comment);
        new AlertDialog.Builder(this).setTitle("Décision de délibération").setView(f).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer",(d,w)->{
            Map<String,Object> res=new LinkedHashMap<>(existing);res.put("studentId",sid);res.put("studentName",first(str(student.get("name")),str(student.get("nom")),str(student.get("fullName"))));res.put("average",average.getText().toString().trim());res.put("decision",String.valueOf(decision.getSelectedItem()));res.put("nextClass",String.valueOf(target.getSelectedItem()));res.put("comment",comment.getText().toString().trim());res.put("updatedAt",System.currentTimeMillis());
            existing.clear();existing.putAll(res);Toast.makeText(this,"Décision préparée. Enregistrez ensuite la classe.",Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void applyDeliberationPromotions(String sourceClass,String year,Map<String,Map<String,Object>> results){
        Map<String,Object> updates=new HashMap<>();int count=0;
        for(Map.Entry<String,Map<String,Object>> e:results.entrySet()){
            Map<String,Object> r=e.getValue();if(!"ADMIS".equals(str(r.get("decision"))))continue;String target=str(r.get("nextClass"));if(target.isEmpty()||target.equals("Aucune classe"))continue;
            String sid=e.getKey();updates.put("students/"+sid+"/class",target);updates.put("students/"+sid+"/classe",target);updates.put("students/"+sid+"/classes",Arrays.asList(target));updates.put("students/"+sid+"/academicYear",activeYear());updates.put("students/"+sid+"/anneeAcademique",activeYear());updates.put("students/"+sid+"/promotionStatus","PROMOTED");updates.put("students/"+sid+"/promotedFromClass",sourceClass);updates.put("students/"+sid+"/promotedAt",System.currentTimeMillis());count++;
        }
        if(count==0){Toast.makeText(this,"Aucun élève admis avec une classe suivante sélectionnée.",Toast.LENGTH_LONG).show();return;}
        int finalCount=count;new AlertDialog.Builder(this).setTitle("Confirmer les promotions").setMessage("Transférer "+count+" élève(s) admis vers leur classe suivante ?")
                .setNegativeButton("Annuler",null).setPositiveButton("Confirmer",(d,w)->db.updateChildren(updates).addOnSuccessListener(x->{Toast.makeText(this,finalCount+" promotion(s) appliquée(s).",Toast.LENGTH_LONG).show();loadPeople();}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show())).show();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(requestCode==REQUEST_PDF){try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out!=null&&pendingPdfBytes!=null)out.write(pendingPdfBytes);Toast.makeText(this,"PDF enregistré.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Écriture PDF impossible : "+e.getMessage(),Toast.LENGTH_LONG).show();}pendingPdfBytes=null;return;}
        String name="document_"+System.currentTimeMillis();try(android.database.Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception ignored){}
        if(requestCode==REQUEST_MEDIA){uploadMedia(uri,pendingUploadKind,name);return;}
        if(requestCode==REQUEST_DOC){String safe=name.replaceAll("[^A-Za-z0-9._-]","_");String path=pendingUploadKind+"/"+System.currentTimeMillis()+"_"+safe;status.setText("Téléversement du document…");FirebaseStorage.getInstance().getReference().child(path).putFile(uri).continueWithTask(t->{if(!t.isSuccessful())throw t.getException()==null?new Exception("Upload échoué"):t.getException();return t.getResult().getStorage().getDownloadUrl();}).addOnSuccessListener(url->{if(dialogFileUrl!=null)dialogFileUrl.setText(url.toString());Toast.makeText(this,"Document téléversé.",Toast.LENGTH_SHORT).show();}).addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}
    }

    private void cardTitle(String title){MaterialCardView c=card();LinearLayout b=column();b.setPadding(dp(13),dp(12),dp(13),dp(12));b.addView(label(title,17,NAVY,true));c.addView(b);LinearLayout.LayoutParams lp=matchWrap();lp.bottomMargin=dp(10);content.addView(c,lp);}
    private Spinner spinnerWithClasses(boolean all){List<String> opts=new ArrayList<>();if(all)opts.add("Toutes les classes");opts.addAll(classes);if(opts.isEmpty())opts.add("Aucune classe");return spinner(opts.toArray(new String[0]));}
    private Spinner spinner(String[] values){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);return s;}
    private void select(Spinner s,String v){if(s.getAdapter()==null)return;for(int i=0;i<s.getAdapter().getCount();i++)if(norm(v).equals(norm(String.valueOf(s.getItemAtPosition(i))))){s.setSelection(i);return;}}
    private String collectionForTab(String tab){return tab.equals("assignments")?"assignments":tab.equals("tests")?"tests":tab.equals("exams")?"exams":"vacationWorks";}
    private String branchForClass(String cls){for(Map<String,Object>m:classRows.values())if(norm(first(str(m.get("name")),str(m.get("className")))).equals(norm(cls)))return first(str(m.get("branch")),str(m.get("branche")),branch);return branch;}
    private boolean studentInClass(Map<String,Object>s,String cls){String c=first(str(s.get("class")),str(s.get("classe")),str(s.get("className")));Object cs=s.get("classes");if(cs instanceof List){for(Object x:(List<?>)cs)if(norm(str(x)).equals(norm(cls)))return true;}return norm(c).equals(norm(cls));}
    private boolean teacherHasClass(Map<String,Object>t,String cls){Object c=t.get("classes");if(c instanceof List){for(Object x:(List<?>)c)if(norm(str(x)).equals(norm(cls)))return true;}return norm(str(c)).contains(norm(cls));}
    private String rowBranch(Map<String,Object>m){return first(str(m.get("branchConfig")),str(m.get("branch")),str(m.get("branche")),str(m.get("branchName")),str(m.get("schoolBranch")));}
    private boolean sameBranch(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return false;return x.equals(y)||baseBranch(x).equals(baseBranch(y));}
    private boolean scopeMatches(Map<String,Object> m){String rb=rowBranch(m);if(!sameBranch(rb,branch))return false;String scope=norm(branch);if(scope.endsWith("maternelle")||scope.endsWith("primaire")){String cls=first(str(m.get("name")),str(m.get("class")),str(m.get("classe")),str(m.get("className")),str(m.get("classes")));String level=norm(first(str(m.get("level")),str(m.get("section")),str(m.get("sectionName"))));String rowScope=norm(rb);if(rowScope.endsWith("maternelle")&&!scope.endsWith("maternelle"))return false;if(rowScope.endsWith("primaire")&&!scope.endsWith("primaire"))return false;if(norm(cls).startsWith("n")||level.contains("maternelle")||level.contains("nursery"))return scope.endsWith("maternelle");if(norm(cls).startsWith("p")||level.contains("primaire")||level.contains("primary"))return scope.endsWith("primaire");return false;}return true;}
    private String baseBranch(String b){String n=norm(b);if(n.startsWith("gisozi"))return "gisozi";if(n.startsWith("kacyiru"))return "kacyiru";if(n.startsWith("kimisagara"))return "kimisagara";return n;}
    private String normalizeYear(String y){return y==null?"":y.replaceAll("[^0-9]","");}
    private String normalizeTerm(String t){String n=norm(t);if(n.contains("bn")||n.contains("annuel"))return "bn";return n.replace("trimestre","").replace("t","");}
    private String calculateAverage(Map<String,Object>m){return first(str(m.get("average")),str(m.get("moyenne")),str(m.get("generalAverage")),"—");}
    private long timestamp(Map<String,Object>m){for(String k:new String[]{"updatedAt","publishedAt","createdAt","date"}){Object v=m.get(k);if(v instanceof Number)return ((Number)v).longValue();try{return java.time.Instant.parse(str(v)).toEpochMilli();}catch(Exception ignored){}}return 0;}
    private String youtubeId(String url){try{if(url.contains("youtu.be/"))return url.substring(url.lastIndexOf('/')+1).split("[?&]")[0];int i=url.indexOf("v=");return i<0?"":url.substring(i+2).split("[&]")[0];}catch(Exception e){return "";}}
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"Lien impossible à ouvrir.",Toast.LENGTH_SHORT).show();}}
    private Map<String,Object> map(DataSnapshot s){Object v=s.getValue();if(v instanceof Map){Map<String,Object>m=new LinkedHashMap<>((Map<String,Object>)v);return m;}return null;}
    private String val(DataSnapshot s,String k){Object v=s.child(k).getValue();return v==null?"":String.valueOf(v);}
    private String str(Object o){return o==null?"":String.valueOf(o);}
    private String first(String...v){for(String x:v)if(x!=null&&!x.trim().isEmpty())return x.trim();return "";}
    private double num(String s){try{return Double.parseDouble(s);}catch(Exception e){return 0;}}
    private String norm(String v){return Normalizer.normalize(v==null?"":v.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[\\s_-]+","");}
    private String activeYear(){Calendar c=Calendar.getInstance();int y=c.get(Calendar.YEAR),m=c.get(Calendar.MONTH)+1;int s=m>=9?y:y-1;return s+"-"+(s+1);}
    private String previousYear(){Calendar c=Calendar.getInstance();int y=c.get(Calendar.YEAR)-1;return y+"-"+(y+1);}
    private String nextYear(){Calendar c=Calendar.getInstance();int y=c.get(Calendar.YEAR)+1;return y+"-"+(y+1);}
    private String isoNow(){return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(new Date());}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(14);return e;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private MaterialCardView card(){MaterialCardView c=new MaterialCardView(this);c.setRadius(dp(14));c.setCardElevation(dp(2));c.setCardBackgroundColor(Color.WHITE);return c;}
    private MaterialButton button(String t,boolean primary){MaterialButton b=new MaterialButton(this);b.setText(t);b.setAllCaps(false);b.setTextSize(11);b.setCornerRadius(dp(9));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?NAVY:Color.rgb(235,239,248)));b.setTextColor(primary?Color.WHITE:NAVY);return b;}
    private TextView label(String t,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(null,1);return v;}
    private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(-1,-2);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}
