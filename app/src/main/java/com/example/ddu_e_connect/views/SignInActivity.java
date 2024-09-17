package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivitySignInBinding;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";
    private ActivitySignInBinding binding;
    private AuthController authController;

    // Regex pattern for the specific email format
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\d{2}[a-zA-Z]{5}\\d{3}@ddu\\.ac\\.in$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();

        Log.d(TAG, "onCreate: SignInActivity started.");

        binding.signInButton.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: SignIn button clicked.");
            signIn();
        });
        binding.forgotPasswordLink.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Forgot password link clicked.");
            navigateToForgotPassword();
        });
        binding.registerLink.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: Register link clicked.");
            navigateToRegister();
        });
    }

    private void signIn() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        Log.d(TAG, "signIn: Attempting to sign in with email: " + email);

        binding.signInButton.setEnabled(false);

        // Validate email format
        if (!isValidEmail(email)) {
            Log.e(TAG, "signIn: Invalid email format.");
            Toast.makeText(this, "Invalid email format. Email must be like '12abcde@ddu.ac.in'", Toast.LENGTH_SHORT).show();
            binding.signInButton.setEnabled(true); // Re-enable button
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "signIn: Email or password is empty.");
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            binding.signInButton.setEnabled(true); // Re-enable button
            return;
        }

        authController.signIn(email, password, new AuthController.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "onSuccess: Sign in successful for email: " + user.getEmail());
                if (user.isEmailVerified()) {
                    Log.d(TAG, "onSuccess: Email is verified.");
                    startActivity(new Intent(SignInActivity.this, com.example.ddu_e_connect.views.HomeActivity.class));
                    finish();
                } else {
                    Log.w(TAG, "onSuccess: Email not verified.");
                    Toast.makeText(SignInActivity.this, "Please verify your email address", Toast.LENGTH_SHORT).show();
                    binding.signInButton.setEnabled(true); // Re-enable button
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "onFailure: Sign in failed with error: " + errorMessage);
                Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                binding.signInButton.setEnabled(true); // Re-enable button
            }
        });
    }

    private void navigateToForgotPassword() {
        Log.d(TAG, "navigateToForgotPassword: Navigating to ForgotPasswordActivity.");
        startActivity(new Intent(SignInActivity.this, com.example.ddu_e_connect.views.ForgotPasswordActivity.class));
    }

    private void navigateToRegister() {
        Log.d(TAG, "navigateToRegister: Navigating to RegistrationActivity.");
        startActivity(new Intent(SignInActivity.this, RegistrationActivity.class));
    }

    // Method to validate email using the regex pattern
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
