package com.grace.eden_smart;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Checks the public release manifest and notifies every signed-in app role. */
public final class AppUpdateChecker {
    private AppUpdateChecker() {}

    public static void check(Activity activity) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://edensmart-c7019.web.app/downloads/version.json?check=" + System.currentTimeMillis());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-cache");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject info = new JSONObject(body.toString());
                int latestCode = info.optInt("appVersionCode", 0);
                String latestName = info.optString("appVersion", "nouvelle version");
                String message = info.optString("appMessage", "Une nouvelle version d?Eden Smart est disponible.");
                String download = info.optString("downloadUrl", "/downloads/eden-smart.apk");
                int currentCode = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                if (latestCode > currentCode) {
                    String downloadUrl = download.startsWith("http") ? download : "https://edensmart-c7019.web.app" + (download.startsWith("/") ? "" : "/") + download;
                    activity.runOnUiThread(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        new AlertDialog.Builder(activity)
                                .setTitle("Mise ? jour Eden Smart disponible")
                                .setMessage(message + "\n\nVersion " + latestName + "\n\nInstallez la mise ? jour pour profiter des am?liorations.")
                                .setCancelable(true)
                                .setNegativeButton("Plus tard", (dialog, which) -> dialog.dismiss())
                                .setPositiveButton("T?l?charger", (dialog, which) -> {
                                    try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))); }
                                    catch (Exception e) { Toast.makeText(activity, "Impossible d?ouvrir le t?l?chargement.", Toast.LENGTH_LONG).show(); }
                                }).show();
                    });
                }
            } catch (Exception ignored) {
                android.util.Log.w("EdenSmartUpdate", "Version check unavailable", ignored);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "eden-app-update-check").start();
    }
}
