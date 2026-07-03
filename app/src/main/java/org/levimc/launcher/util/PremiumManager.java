package org.levimc.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PremiumManager {
    
    private static final String PREFS_NAME = "premium_prefs";
    private static final String KEY_CODE = "premium_code";
    private static final String KEY_EXPIRY = "premium_expiry";
    private static final String CHECK_URL = "https://kempa.alwaysdata.net/check/";
    
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    
    public PremiumManager(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Check if premium code is currently valid and not expired
     */
    public boolean isPremiumValid() {
        String code = getSavedCode();
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        long expiry = prefs.getLong(KEY_EXPIRY, 0);
        return System.currentTimeMillis() < expiry;
    }
    
    /**
     * Get saved premium code from SharedPreferences
     */
    public String getSavedCode() {
        return prefs.getString(KEY_CODE, null);
    }
    
    /**
     * Validate code with remote server
     * Returns: "ok" if valid, error message otherwise
     */
    public String validateCodeWithServer(@NonNull String code) {
        try {
            Request request = new Request.Builder()
                .url(CHECK_URL + code)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String result = response.body().string().trim().toLowerCase();
                return result;
            }
            return "network_error";
        } catch (IOException e) {
            return "network_error";
        }
    }
    
    /**
     * Save premium code and set 6-hour expiry
     */
    public void savePremiumCode(@NonNull String code) {
        long expiryTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(72);
        prefs.edit()
            .putString(KEY_CODE, code)
            .putLong(KEY_EXPIRY, expiryTime)
            .apply();
    }
    
    /**
     * Clear saved premium code
     */
    public void clearPremiumCode() {
        prefs.edit()
            .remove(KEY_CODE)
            .remove(KEY_EXPIRY)
            .apply();
    }
}
