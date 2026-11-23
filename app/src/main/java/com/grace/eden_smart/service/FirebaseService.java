package com.grace.eden_smart.service;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/** Central Firebase access point for Eden Smart Attendance. */
public final class FirebaseService {
    private static final String PROJECT_ID = "edensmart-app";
    private static final String DATABASE_URL = "https://edensmart-app-default-rtdb.firebaseio.com";
    private static FirebaseService instance;

    private final FirebaseAuth auth;
    private final FirebaseDatabase database;

    private FirebaseService(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            // Temporary fallback to the website Firebase project settings.
            // Install app/google-services.json for package com.grace.eden_smart
            // after registering the Android app in Firebase Console.
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyApUFNELOfgIe7rWEek9GLS9EIphNW09-A")
                    .setApplicationId("1:1093120876724:web:bc37448cadd18d651c77e1")
                    .setProjectId(PROJECT_ID)
                    .setDatabaseUrl(DATABASE_URL)
                    .setStorageBucket("edensmart-app.firebasestorage.app")
                    .build();
            FirebaseApp.initializeApp(context.getApplicationContext(), options);
        }
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    public static synchronized FirebaseService getInstance(Context context) {
        if (instance == null) instance = new FirebaseService(context.getApplicationContext());
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public DatabaseReference getUsersReference() {
        return database.getReference("users");
    }

    public DatabaseReference getTeacherAttendanceReference() {
        return database.getReference("teacher_attendance");
    }
}
