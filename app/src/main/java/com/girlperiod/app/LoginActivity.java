package com.girlperiod.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.User;

import java.util.concurrent.Executor;

/**
 * Login screen with username/password authentication and optional fingerprint login.
 * Features a soft Ghibli-inspired pastel background.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnFingerprint;
    private TextView tvRegister;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        // If already logged in, skip straight to MainActivity
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnFingerprint = findViewById(R.id.btnFingerprintLogin);
        tvRegister = findViewById(R.id.tvRegisterLink);

        // Soft Ghibli-inspired pastel background (warm cream)
        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#FDF6E3"));

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnFingerprint.setOnClickListener(v -> showBiometricPrompt());

        // Show fingerprint button only if the entered username has it enabled
        setupFingerprintVisibility();

        // Show registration success message if returning from RegisterActivity
        if (getIntent().getBooleanExtra("registration_success", false)) {
            Toast.makeText(this, "Registration successful! Please log in.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Listens to username input and shows the fingerprint button
     * only when the entered username has fingerprint login enabled.
     */
    private void setupFingerprintVisibility() {
        btnFingerprint.setVisibility(View.GONE);

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                String username = s.toString().trim();
                if (!username.isEmpty() && dbHelper.isFingerprintEnabled(username)
                        && isBiometricSupported()) {
                    btnFingerprint.setVisibility(View.VISIBLE);
                } else {
                    btnFingerprint.setVisibility(View.GONE);
                }
            }
        });
    }

    private boolean isBiometricSupported() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = dbHelper.validateUserAndReturn(username, password);
        if (user != null) {
            sessionManager.saveUser(user);
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        String username = etUsername.getText().toString().trim();
                        User user = dbHelper.getUserByUsername(username);
                        if (user != null) {
                            sessionManager.saveUser(user);
                            Toast.makeText(LoginActivity.this, "Fingerprint login successful!",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(LoginActivity.this, "User not found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(LoginActivity.this, "Authentication error: " + errString,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this, "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Fingerprint Login")
                .setSubtitle("Use your fingerprint to log in")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
