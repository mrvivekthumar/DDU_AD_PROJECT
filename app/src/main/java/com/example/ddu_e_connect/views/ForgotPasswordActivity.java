package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityForogtPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForogtPasswordBinding binding;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForogtPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();

        binding.resetPasswordButton.setOnClickListener(v -> resetPassword());
        binding.backToLoginLink.setOnClickListener(v -> navigateToSignIn());
    }

    private void resetPassword() {
        String email = binding.emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        authController.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class));
                finish();
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "Failed to send password reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSignIn() {
        startActivity(new Intent(ForgotPasswordActivity.this, SignInActivity.class));
    }
}
