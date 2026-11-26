package com.grace.eden_smart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.grace.eden_smart.service.FirebaseService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class NativeSecretaryAttendanceActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private DatabaseReference usersRef, attendanceRef;
    private LinearLayout list;
    private TextView heading, branchLabel, summary, status;
    private String branch="", dateKey="";
    private final List<Teacher> teachers=new ArrayList<>();

    private static class Teacher {
        String uid,name,branch,code,arrival="",departure="",observation="";
        Teacher(String u,String n,String b,String c){uid=u;name=n;branch=b;code=c;}
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(27,44,126));
        getWindow().setNavigationBarColor(Color.rgb(27,44,126));
        try {
            FirebaseService service=FirebaseService.getInstance(getApplicationContext());
            auth=service.getAuth(); usersRef=service.getUsersReference();
            attendanceRef=service.getTeacherAttendanceReference();
        } catch(Exception e){finish();return;}
        if(auth.getCurrentUser()==null){finish();return;}
        dateKey=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        buildScreen();
        verifySecretary();
    }

    private void buildScreen(){
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(243,246,252));
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(18),dp(18),dp(18),dp(28));
        scroll.addView(page); setContentView(scroll);
        KeyboardInsets.apply(this);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("EDEN FAMILY SCHOOL",19,true,Color.rgb(27,44,126));
        top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        MaterialButton back=button("Retour au portail",false); top.addView(back); back.setOnClickListener(v->finish()); page.addView(top);
        heading=label("POINTAGE DU PERSONNEL",14,true,Color.rgb(27,44,126)); add(page,heading,0,dp(20),0,0);
        branchLabel=label("Branche : vérification…",14,false,Color.DKGRAY); add(page,branchLabel,0,dp(5),0,0);
        TextView date=label("Date : "+dateKey,13,false,Color.DKGRAY); add(page,date,0,dp(4),0,dp(12));
        MaterialCardView box=new MaterialCardView(this); box.setRadius(dp(14)); box.setCardElevation(dp(2));
        summary=label("Chargement du résumé…",14,true,Color.rgb(31,41,55)); summary.setPadding(dp(15),dp(15),dp(15),dp(15)); box.addView(summary); page.addView(box);
        status=label("Chargement des enseignants…",13,false,Color.DKGRAY); add(page,status,0,dp(14),0,dp(8));
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); page.addView(list);
    }

    private void verifySecretary(){
        String uid=auth.getCurrentUser().getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot s){
                String role=norm(first(val(s,"role"),val(s,"userRole"),val(s,"type")));
                if(!role.equals("secretary")&&!role.equals("secretaire")&&!role.equals("secretariat")){Toast.makeText(NativeSecretaryAttendanceActivity.this,"Accès réservé au secrétariat.",Toast.LENGTH_LONG).show();finish();return;}
                branch=first(val(s,"branchConfig"),val(s,"branch"),val(s,"branche"),val(s,"branchName"),val(s,"schoolBranch"),val(s,"school_branch"));
                branchLabel.setText("Branche : "+branch);
                if(branch.isEmpty()){status.setText("Aucune branche n'est attribuée à ce compte.");return;}
                loadTeachers();
            }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Vérification impossible : "+e.getMessage());}
        });
    }

    private void loadTeachers(){
        status.setText("Chargement des enseignants de "+branch+"…");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot root){
                teachers.clear();
                for(DataSnapshot s:root.getChildren()){
                    String role=norm(first(val(s,"role"),val(s,"userRole"),val(s,"type")));
                    if(!role.equals("teacher")&&!role.equals("enseignant")&&!role.equals("enseignante"))continue;
                    if(!"active".equalsIgnoreCase(first(val(s,"status"),"active")))continue;
                    String b=first(val(s,"branchConfig"),val(s,"branch"),val(s,"branche"),val(s,"branchName"),val(s,"schoolBranch"),val(s,"school_branch"));
                    if(b.isEmpty()||!norm(b).equals(norm(branch)))continue;
                    String name=first(val(s,"nom"),val(s,"name"),val(s,"displayName"),val(s,"fullName"),val(s,"teacherName"),val(s,"email"),s.getKey());
                    String code=first(val(s,"teacherCode"),s.getKey());
                    teachers.add(new Teacher(s.getKey(),name,b,code));
                }
                teachers.sort((a,b)->a.name.compareToIgnoreCase(b.name));
                status.setText(teachers.size()+" enseignant(s) actif(s) dans "+branch+". Code personnel puis empreinte du secrétariat.");
                if(teachers.isEmpty()){summary.setText("Aucun enseignant actif trouvé.");return;}
                for(Teacher t:teachers)loadDay(t);
            }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Lecture des enseignants impossible : "+e.getMessage());}
        });
    }

    private void loadDay(Teacher t){
        attendanceRef.child(t.uid).child(dateKey).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override public void onDataChange(@NonNull DataSnapshot s){t.arrival=val(s.child("arrival"),"time");t.departure=val(s.child("departure"),"time");t.observation=val(s,"observation");render(); }
            @Override public void onCancelled(@NonNull DatabaseError e){status.setText("Lecture des présences impossible : "+e.getMessage());}
        });
    }

    private void render(){
        list.removeAllViews(); int on=0,late=0,absent=0,depart=0;
        for(Teacher t:teachers){
            if(t.arrival.isEmpty())absent++; else if(isLate(t.arrival))late++; else on++;
            if(!t.departure.isEmpty())depart++;
            MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(13));card.setCardElevation(dp(2));
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(14),dp(12),dp(14),dp(12));
            body.addView(label(t.name,16,true,Color.rgb(27,44,126)));
            body.addView(label("Arrivée : "+(t.arrival.isEmpty()?"—":t.arrival)+"   |   "+(t.arrival.isEmpty()?"Non pointé":isLate(t.arrival)?"En retard":"À l'heure"),13,false,Color.DKGRAY));
            body.addView(label("Départ : "+(t.departure.isEmpty()?"—":t.departure),13,false,Color.DKGRAY));
            if(!t.observation.isEmpty())body.addView(label("Observation de la Direction : "+t.observation,13,true,Color.rgb(156,78,24)));
            LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);
            MaterialButton a=button("Arrivée",true),d=button("Départ",false);
            a.setEnabled(t.arrival.isEmpty());d.setEnabled(!t.arrival.isEmpty()&&t.departure.isEmpty());
            a.setOnClickListener(v->verifyCode(t,"arrival"));d.setOnClickListener(v->verifyCode(t,"departure"));
            actions.addView(a);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-2,-2);dp.leftMargin=dp(8);actions.addView(d,dp);body.addView(actions);
            card.addView(body);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(10);list.addView(card,cp);
        }
        summary.setText("À l'heure : "+on+"     En retard : "+late+"\nNon pointés : "+absent+"     Départs : "+depart);
    }

    private void verifyCode(Teacher t,String event){
        if(event.equals("departure")&&t.arrival.isEmpty())return;
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Code personnel de l'enseignant");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("Identifier l'enseignant").setMessage("Saisissez le code personnel de "+t.name+". L'empreinte du secrétariat validera ensuite le pointage.").setView(input)
                .setNegativeButton("Annuler",null).setPositiveButton("Continuer",(dialog,w)->{
                    String entered=input.getText()==null?"":input.getText().toString().trim();
                    if(!entered.equalsIgnoreCase(t.code.trim())){Toast.makeText(this,"Code incorrect.",Toast.LENGTH_SHORT).show();return;}
                    biometric(t,event);
                }).show();
    }

    private void biometric(Teacher t,String event){
        int result=BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if(result==BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED){
            new AlertDialog.Builder(this).setTitle("Empreinte non configurée").setMessage("Enregistrez d'abord l'empreinte digitale du secrétariat dans les paramètres Android.")
                    .setNegativeButton("Annuler",null).setPositiveButton("Configurer",(d,w)->{
                        Intent i=new Intent(Settings.ACTION_BIOMETRIC_ENROLL);i.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,BiometricManager.Authenticators.BIOMETRIC_STRONG);startActivity(i);
                    }).show();return;
        }
        if(result!=BiometricManager.BIOMETRIC_SUCCESS){Toast.makeText(this,result==BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE?"Lecteur d'empreinte non disponible.":"Lecteur biométrique indisponible. Vérifiez les paramètres du téléphone.",Toast.LENGTH_LONG).show();return;}
        Executor executor=ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt=new BiometricPrompt(this,executor,new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r){writeOnce(t,event);}
            @Override public void onAuthenticationError(int code,@NonNull CharSequence message){Toast.makeText(NativeSecretaryAttendanceActivity.this,"Validation annulée : "+message,Toast.LENGTH_SHORT).show();}
            @Override public void onAuthenticationFailed(){Toast.makeText(NativeSecretaryAttendanceActivity.this,"Empreinte non reconnue. Réessayez.",Toast.LENGTH_SHORT).show();}
        });
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("Validation du pointage").setSubtitle("Empreinte du secrétariat — "+t.name)
                .setNegativeButtonText("Annuler").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build());
    }

    private void writeOnce(Teacher t,String event){
        String now=new SimpleDateFormat("HH:mm",Locale.US).format(new Date());
        Map<String,Object> data=new HashMap<>();data.put("time",now);data.put("timestamp",ServerValue.TIMESTAMP);data.put("clientTimestamp",System.currentTimeMillis());
        data.put("verifiedByDeviceBiometric",true);data.put("recordedByUid",auth.getCurrentUser().getUid());data.put("recordedByRole","secretary");
        data.put("lateStatus",event.equals("arrival")&&isLate(now)?"late":"on_time");data.put("teacherName",t.name);data.put("teacherCode",t.code);
        data.put("branch",t.branch);data.put("source","efs_app");data.put("signatureStatus","verified_by_secretary_device_biometric");
        DatabaseReference ref=attendanceRef.child(t.uid).child(dateKey).child(event);
        ref.runTransaction(new Transaction.Handler(){
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData current){if(current.getValue()!=null)return Transaction.abort();current.setValue(data);return Transaction.success(current);}
            @Override public void onComplete(DatabaseError error,boolean committed,DataSnapshot snap){
                if(error!=null)Toast.makeText(NativeSecretaryAttendanceActivity.this,"Enregistrement impossible : "+error.getMessage(),Toast.LENGTH_LONG).show();
                else if(!committed)Toast.makeText(NativeSecretaryAttendanceActivity.this,"Pointage déjà enregistré. Aucune donnée remplacée.",Toast.LENGTH_LONG).show();
                else {Toast.makeText(NativeSecretaryAttendanceActivity.this,"Pointage enregistré pour "+t.name,Toast.LENGTH_SHORT).show();loadDay(t);}
            }
        });
    }

    private boolean isLate(String v){try{String[] p=v.split(":");return Integer.parseInt(p[0])*60+Integer.parseInt(p[1])>=470;}catch(Exception e){return false;}}
    private String val(DataSnapshot s,String k){Object v=s.child(k).getValue();return v==null?"":String.valueOf(v);}
    private String val(DataSnapshot s){Object v=s.getValue();return v==null?"":String.valueOf(v);}
    private String first(String... a){for(String v:a)if(v!=null&&!v.trim().isEmpty())return v.trim();return "";}
    private String norm(String v){return Normalizer.normalize(v==null?"":v.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[\\s_-]+","");}
    private TextView label(String s,int size,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);return t;}
    private MaterialButton button(String s,boolean primary){MaterialButton b=new MaterialButton(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setCornerRadius(dp(9));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?Color.rgb(27,44,126):Color.rgb(235,239,248)));b.setTextColor(primary?Color.WHITE:Color.rgb(27,44,126));return b;}
    private void add(LinearLayout parent,android.view.View child,int l,int top,int r,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(l,top,r,bottom);parent.addView(child,p);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
