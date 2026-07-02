package org.levimc.launcher.ui.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import org.levimc.launcher.R;
import org.levimc.launcher.util.PremiumManager;

public class PremiumCodeDialog {
    
    private final Context context;
    private final PremiumManager premiumManager;
    private final Callback callback;
    private AlertDialog dialog;
    private boolean isValidating = false;
    
    public interface Callback {
        void onCodeVerified();
        void onCancelled();
    }
    
    public PremiumCodeDialog(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
        this.premiumManager = new PremiumManager(context);
    }
    
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_premium_code, null);
        
        EditText editCode = view.findViewById(R.id.edit_code);
        Button btnEnter = view.findViewById(R.id.btn_enter);
        Button btnGetCode = view.findViewById(R.id.btn_get_code);
        ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        TextView tvStatus = view.findViewById(R.id.tv_status);
        
        builder.setView(view)
            .setCancelable(false)
            .setOnDismissListener(dialogInterface -> callback.onCancelled());
        
        dialog = builder.create();
        
        // Handle Enter button
        btnEnter.setOnClickListener(v -> {
            String code = editCode.getText().toString().trim();
            if (code.isEmpty()) {
                tvStatus.setText("Please enter a code");
                tvStatus.setVisibility(View.VISIBLE);
                return;
            }
            validateCode(code, progressBar, tvStatus, editCode, btnEnter);
        });
        
        // Handle Get Code button
        btnGetCode.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://kempa.alwaysdata.net/get-code"));
            context.startActivity(intent);
        });
        
        // Update button state based on input
        editCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                tvStatus.setVisibility(View.GONE);
                btnEnter.setEnabled(!s.toString().trim().isEmpty());
            }
        });
        
        dialog.show();
    }
    
    private void validateCode(String code, ProgressBar progressBar, 
                            TextView tvStatus, EditText editCode, Button btnEnter) {
        if (isValidating) return;
        
        isValidating = true;
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.GONE);
        btnEnter.setEnabled(false);
        editCode.setEnabled(false);
        
        // Validate on background thread
        new Thread(() -> {
            String result = premiumManager.validateCodeWithServer(code);
            
            new Handler(Looper.getMainLooper()).post(() -> {
                progressBar.setVisibility(View.GONE);
                btnEnter.setEnabled(true);
                editCode.setEnabled(true);
                isValidating = false;
                
                if ("ok".equals(result)) {
                    premiumManager.savePremiumCode(code);
                    tvStatus.setText("✓ You got 6 hours of time! Thanks to BlackBart.");
                    tvStatus.setTextColor(0xFF4CAF50);
                    tvStatus.setVisibility(View.VISIBLE);
                    
                    dialog.dismiss();
                    callback.onCodeVerified();
                } else if ("network_error".equals(result)) {
                    tvStatus.setText("Network error. Please try again.");
                    tvStatus.setTextColor(0xFFFF9800);
                    tvStatus.setVisibility(View.VISIBLE);
                } else {
                    tvStatus.setText("✗ Invalid Code");
                    tvStatus.setTextColor(0xFFF44336);
                    tvStatus.setVisibility(View.VISIBLE);
                    editCode.setText("");
                }
            });
        }).start();
    }
}
