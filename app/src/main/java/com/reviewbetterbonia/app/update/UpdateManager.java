package com.reviewbetterbonia.app.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.reviewbetterbonia.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateManager {

    private static final String PREFS = "app_updates";
    private static final String KEY_LAST_CHECK = "last_check_ms";
    private static final String KEY_PENDING_APK = "pending_apk";

    // Check GitHub at most once every 12 hours.
    private static final long CHECK_INTERVAL_MS =
            12L * 60L * 60L * 1000L;

    private final Activity activity;

    private final Handler main =
            new Handler(Looper.getMainLooper());

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public UpdateManager(Activity activity) {
        this.activity = activity;
    }

    public void checkAutomatically() {

        SharedPreferences prefs =
                activity.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        long last =
                prefs.getLong(KEY_LAST_CHECK, 0L);

        if (System.currentTimeMillis() - last
                < CHECK_INTERVAL_MS) {
            return;
        }

        check(false);
    }

    public void checkNow() {
        check(true);
    }

    public void resumePendingInstall() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager()
                .canRequestPackageInstalls()) {
            return;
        }

        String path =
                activity.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                ).getString(KEY_PENDING_APK, "");

        if (path == null || path.isEmpty()) {
            return;
        }

        File apk = new File(path);

        if (apk.isFile()) {
            installApk(apk);
        } else {
            clearPendingApk();
        }
    }

    private void check(boolean userInitiated) {

        String owner = getStringResource("github_repo_owner");
        String repo = getStringResource("github_repo_name");

        if (owner.isEmpty()
                || repo.isEmpty()
                || owner.contains("YOUR_")
                || repo.contains("YOUR_")) {

            if (userInitiated) {
                toast("GitHub update settings are not configured.");
            }

            return;
        }

        SharedPreferences prefs =
                activity.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putLong(
                        KEY_LAST_CHECK,
                        System.currentTimeMillis()
                )
                .apply();

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                String endpoint =
                        "https://api.github.com/repos/"
                                + owner
                                + "/"
                                + repo
                                + "/releases/latest";

                connection =
                        (HttpURLConnection)
                                new URL(endpoint).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                );

                connection.setRequestProperty(
                        "X-GitHub-Api-Version",
                        "2026-03-10"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {

                    if (userInitiated) {
                        main.post(() ->
                                toast(
                                        "Could not check for updates. HTTP "
                                                + responseCode
                                )
                        );
                    }

                    return;
                }

                String json =
                        readAll(
                                connection.getInputStream()
                        );

                JSONObject release =
                        new JSONObject(json);

                if (release.optBoolean("draft", false)
                        || release.optBoolean("prerelease", false)) {

                    if (userInitiated) {
                        main.post(() ->
                                toast(
                                        "No published update is available."
                                )
                        );
                    }

                    return;
                }

                String remoteVersion =
                        release
                                .optString("tag_name", "")
                                .trim()
                                .replaceFirst(
                                        "^[vV]",
                                        ""
                                );

                String localVersion =
                        BuildConfig.VERSION_NAME
                                .trim()
                                .replaceFirst(
                                        "^[vV]",
                                        ""
                                );

                if (compareVersions(
                        remoteVersion,
                        localVersion
                ) <= 0) {

                    if (userInitiated) {
                        main.post(() ->
                                toast(
                                        "You are using the latest version."
                                )
                        );
                    }

                    return;
                }

                JSONArray assets =
                        release.optJSONArray("assets");

                if (assets == null) {

                    if (userInitiated) {
                        main.post(() ->
                                toast(
                                        "The latest release has no APK."
                                )
                        );
                    }

                    return;
                }

                String apkUrl = "";
                String apkName = "";

                for (int i = 0;
                     i < assets.length();
                     i++) {

                    JSONObject asset =
                            assets.optJSONObject(i);

                    if (asset == null) {
                        continue;
                    }

                    String name =
                            asset.optString(
                                    "name",
                                    ""
                            );

                    if (name
                            .toLowerCase()
                            .endsWith(".apk")) {

                        apkUrl =
                                asset.optString(
                                        "browser_download_url",
                                        ""
                                );

                        apkName = name;

                        break;
                    }
                }

                if (apkUrl.isEmpty()) {

                    if (userInitiated) {
                        main.post(() ->
                                toast(
                                        "The latest release has no APK asset."
                                )
                        );
                    }

                    return;
                }

                String notes =
                        release
                                .optString("body", "")
                                .trim();

                final String finalApkUrl = apkUrl;
                final String finalApkName =
                        apkName.isEmpty()
                                ? "Review-Betterbonia.apk"
                                : apkName;

                main.post(() ->
                        showUpdateDialog(
                                remoteVersion,
                                notes,
                                finalApkUrl,
                                finalApkName
                        )
                );

            } catch (Exception e) {

                if (userInitiated) {
                    main.post(() ->
                            toast(
                                    "Could not check for updates."
                            )
                    );
                }

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void showUpdateDialog(
            String version,
            String notes,
            String apkUrl,
            String apkName
    ) {

        if (activity.isFinishing()
                || activity.isDestroyed()) {
            return;
        }

        String message =
                "A new version ("
                        + version
                        + ") is available.";

        if (!notes.isEmpty()) {

            String trimmedNotes =
                    notes.length() > 1500
                            ? notes.substring(0, 1500) + "..."
                            : notes;

            message += "\n\n" + trimmedNotes;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Update available")
                .setMessage(message)
                .setNegativeButton(
                        "Later",
                        null
                )
                .setPositiveButton(
                        "Update",
                        (dialog, which) ->
                                downloadApk(
                                        apkUrl,
                                        apkName
                                )
                )
                .show();
    }

    private void downloadApk(
            String apkUrl,
            String apkName
    ) {

        AlertDialog progress =
                new AlertDialog.Builder(activity)
                        .setTitle("Downloading update")
                        .setMessage("Please wait...")
                        .setCancelable(false)
                        .create();

        progress.show();

        executor.execute(() -> {

            File updatesDirectory =
                    new File(
                            activity.getFilesDir(),
                            "updates"
                    );

            if (!updatesDirectory.exists()
                    && !updatesDirectory.mkdirs()) {

                main.post(() -> {

                    progress.dismiss();

                    toast(
                            "Could not create the update folder."
                    );
                });

                return;
            }

            File apk =
                    new File(
                            updatesDirectory,
                            "latest-update.apk"
                    );

            HttpURLConnection connection = null;

            try {

                connection =
                        (HttpURLConnection)
                                new URL(apkUrl)
                                        .openConnection();

                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);

                connection.setRequestProperty(
                        "Accept",
                        "application/octet-stream"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException(
                            "HTTP " + responseCode
                    );
                }

                try (
                        InputStream input =
                                connection.getInputStream();

                        FileOutputStream output =
                                new FileOutputStream(
                                        apk,
                                        false
                                )
                ) {

                    byte[] buffer =
                            new byte[8192];

                    int count;

                    while ((count =
                            input.read(buffer)) != -1) {

                        output.write(
                                buffer,
                                0,
                                count
                        );
                    }

                    output.flush();
                }

                activity
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        )
                        .edit()
                        .putString(
                                KEY_PENDING_APK,
                                apk.getAbsolutePath()
                        )
                        .apply();

                main.post(() -> {

                    progress.dismiss();

                    installApk(apk);
                });

            } catch (Exception e) {

                if (apk.exists()) {
                    apk.delete();
                }

                main.post(() -> {

                    progress.dismiss();

                    toast(
                            "The update could not be downloaded."
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void installApk(File apk) {

        if (!apk.isFile()) {

            clearPendingApk();

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager()
                .canRequestPackageInstalls()) {

            new AlertDialog.Builder(activity)
                    .setTitle("Allow app installation")
                    .setMessage(
                            "Android needs permission to install "
                                    + "updates downloaded from GitHub. "
                                    + "Enable this for Review Betterbonia, "
                                    + "then return to the app."
                    )
                    .setNegativeButton(
                            "Cancel",
                            null
                    )
                    .setPositiveButton(
                            "Settings",
                            (dialog, which) -> {

                                Intent settings =
                                        new Intent(
                                                Settings
                                                        .ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                Uri.parse(
                                                        "package:"
                                                                + activity
                                                                .getPackageName()
                                                )
                                        );

                                activity.startActivity(
                                        settings
                                );
                            }
                    )
                    .show();

            return;
        }

        try {

            Uri uri =
                    FileProvider.getUriForFile(
                            activity,
                            activity.getPackageName()
                                    + ".fileprovider",
                            apk
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_INSTALL_PACKAGE
                    );

            intent.setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            activity.startActivity(intent);

        } catch (Exception e) {

            toast(
                    "Android could not open the installer."
            );
        }
    }

    private void clearPendingApk() {

        String path =
                activity
                        .getSharedPreferences(
                                PREFS,
                                Context.MODE_PRIVATE
                        )
                        .getString(
                                KEY_PENDING_APK,
                                ""
                        );

        if (path != null && !path.isEmpty()) {

            try {
                new File(path).delete();
            } catch (Exception ignored) {
            }
        }

        activity
                .getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE
                )
                .edit()
                .remove(KEY_PENDING_APK)
                .apply();
    }

    private String getStringResource(
            String resourceName
    ) {

        int resourceId =
                activity
                        .getResources()
                        .getIdentifier(
                                resourceName,
                                "string",
                                activity.getPackageName()
                        );

        if (resourceId == 0) {
            return "";
        }

        return activity
                .getString(resourceId)
                .trim();
    }

    private static int compareVersions(
            String remote,
            String local
    ) {

        String[] remoteParts =
                remote.split("\\.");

        String[] localParts =
                local.split("\\.");

        int count =
                Math.max(
                        remoteParts.length,
                        localParts.length
                );

        for (int i = 0; i < count; i++) {

            int remoteNumber =
                    i < remoteParts.length
                            ? numericPart(remoteParts[i])
                            : 0;

            int localNumber =
                    i < localParts.length
                            ? numericPart(localParts[i])
                            : 0;

            if (remoteNumber != localNumber) {

                return Integer.compare(
                        remoteNumber,
                        localNumber
                );
            }
        }

        return 0;
    }

    private static int numericPart(
            String value
    ) {

        StringBuilder digits =
                new StringBuilder();

        for (int i = 0;
             i < value.length();
             i++) {

            char c =
                    value.charAt(i);

            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }

        if (digits.length() == 0) {
            return 0;
        }

        try {
            return Integer.parseInt(
                    digits.toString()
            );
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String readAll(
            InputStream input
    ) throws Exception {

        StringBuilder result =
                new StringBuilder();

        byte[] buffer =
                new byte[8192];

        int count;

        while ((count =
                input.read(buffer)) != -1) {

            result.append(
                    new String(
                            buffer,
                            0,
                            count,
                            "UTF-8"
                    )
            );
        }

        return result.toString();
    }

    private void toast(
            String message
    ) {

        Toast.makeText(
                activity,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}