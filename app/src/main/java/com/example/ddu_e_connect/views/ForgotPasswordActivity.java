package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityForogtPasswordBinding;

import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";
    private ActivityForogtPasswordBinding binding;
    private AuthController authController;

    // Regex pattern for the specific email format
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\d{2}[a-zA-Z]{5}@ddu\\.ac\\.in$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForogtPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();

        Log.d(TAG, "onCreate: ForgotPasswordActivity started.");

        binding.resetPasswordButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Reset Password button clicked.");
            resetPassword();
        });

        binding.backToLoginLink.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Back to login link clicked.");
            navigateToSignIn();
        });
    }

    private void resetPassword() {
        String email = binding.emailEditText.getText().toString().trim();

        Log.d(TAG, "resetPassword: Attempting to reset password for email: " + email);

        if (email.isEmpty()) {
            Log.e(TAG, "resetPassword: Email field is empty.");
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            Log.e(TAG, "resetPassword: Invalid email format.");
            Toast.makeText(this, "Invalid email format. Email must be like '12abcde@ddu.ac.in'", Toast.LENGTH_SHORT).show();
            return;
        }

        authController.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "resetPassword: Password reset email sent successfully.");
                Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class));
                finish();
            } else {
                Log.e(TAG, "resetPassword: Failed to send password reset email.", task.getException());
                Toast.makeText(ForgotPasswordActivity.this, "Failed to send password reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSignIn() {
        Log.d(TAG, "navigateToSignIn: Navigating to SignInActivity.");
        startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class));
    }

    // Method to validate email using the regex pattern
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
