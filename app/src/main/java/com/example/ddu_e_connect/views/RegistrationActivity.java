package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityRegistrationBinding;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\d{2}[a-zA-Z]{5}\\d{3}@ddu\\.ac\\.in$");

    private ActivityRegistrationBinding binding;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();

        Log.d(TAG, "onCreate: RegistrationActivity started.");

        binding.registerButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Register button clicked.");
            register();
        });
        binding.loginLink.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Login link clicked.");
            navigateToSignIn();
        });
    }

    private void register() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        Log.d(TAG, "register: Attempting to register with email: " + email);

        binding.registerButton.setEnabled(false);

        // Validate email format
        if (!isValidEmail(email)) {
            Log.e(TAG, "register: Invalid email format.");
            Toast.makeText(this, "Invalid email format. Please use your institutional Email to login", Toast.LENGTH_SHORT).show();
            binding.registerButton.setEnabled(true); // Re-enable the button
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "register: Email or password is empty.");
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            binding.registerButton.setEnabled(true); // Re-enable the button
            return;
        }

        authController.register(email, password, new AuthController.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "onSuccess: Registration successful for email: " + user.getEmail());
                Toast.makeText(RegistrationActivity.this, "Registration successful! Please verify your email.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegistrationActivity.this, com.example.ddu_e_connect.views.SignInActivity.class));
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "onFailure: Registration failed with error: " + errorMessage);
                Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                binding.registerButton.setEnabled(true); // Re-enable the button
            }
        });
    }

    private void navigateToSignIn() {
        Log.d(TAG, "navigateToSignIn: Navigating to SignInActivity.");
        startActivity(new Intent(RegistrationActivity.this, com.example.ddu_e_connect.views.SignInActivity.class));
    }

    // Method to validate email using the regex pattern
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
