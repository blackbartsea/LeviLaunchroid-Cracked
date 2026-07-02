package org.levimc.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import org.levimc.launcher.R;
import org.levimc.launcher.ui.dialogs.CustomAlertDialog;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GithubReleaseUpdater {
    private static final String TAG = "GithubReleaseUpdater";
    private static final String VERSION_URL = "https://kempa.alwaysdata.net/version";
    private static final String DOWNLOAD_URL = "https://kempa.alwaysdata.net/download";
    private final Activity activity;
    private final OkHttpClient client = new OkHttpClient();
    private ActivityResultLauncher<Intent> permissionResultLauncher;

    public GithubReleaseUpdater(Activity activity, String owner, String repo,
                                ActivityResultLauncher<Intent> permissionResultLauncher) {
        this.activity = activity;
        this.permissionResultLauncher = permissionResultLauncher;
    }

    public static int compareVersion(String a, String b) {
        a = a.startsWith("v") ? a.substring(1) : a;
        b = b.startsWith("v") ? b.substring(1) : b;
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        int len = Math.max(x.length, y.length);
        for (int i = 0; i < len; i++) {
            int vi = i < x.length ? Integer.parseInt(x[i]) : 0;
            int vj = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (vi > vj) return 1;
            if (vi < vj) return -1;
        }
        return 0;
    }

    public void checkUpdate() {
        Request request = new Request.Builder().url(VERSION_URL).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String latestVersion = response.body().string().trim();
                    String localVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                    if (compareVersion(latestVersion, localVersion) > 0) {
                        showUpdateDialog(latestVersion);
                    } else {
                        activity.runOnUiThread(() ->
                                Toast.makeText(activity, activity.getString(R.string.already_latest_version, localVersion), Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                }
            }
        });
    }

    public void checkUpdateOnLaunch() {
        Request request = new Request.Builder().url(VERSION_URL).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String latestVersion = response.body().string().trim();
                    String localVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;

                    if (compareVersion(latestVersion, localVersion) > 0) {
                        showUpdateDialog(latestVersion);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                }
            }
        });
    }

    private void showUpdateDialog(String version) {
        activity.runOnUiThread(() -> {
            CustomAlertDialog dialog = new CustomAlertDialog(activity);
            dialog.setTitleText(activity.getString(R.string.new_version_found, version));
            dialog.setMessage(activity.getString(R.string.update_question));
            dialog.setPositiveButton(activity.getString(R.string.download_update), (d) -> openDownloadUrl());
            dialog.setNegativeButton(activity.getString(R.string.cancel), null);
            dialog.show();
        });
    }

    private void openDownloadUrl() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(DOWNLOAD_URL));
        activity.startActivity(intent);
    }
}
