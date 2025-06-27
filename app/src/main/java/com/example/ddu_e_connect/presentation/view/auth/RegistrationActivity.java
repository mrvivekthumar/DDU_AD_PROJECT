package com.example.ddu_e_connect.presentation.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.databinding.ActivityRegistrationBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * Welcome/Info Activity - Shows app information and features
 * Since we use Google Authentication, traditional registration is not needed
 */
public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    private ActivityRegistrationBinding binding;
    private GoogleAuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        checkUserStatus();

        Log.d(TAG, "RegistrationActivity (Welcome Screen) initialized");
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        authRepository = new GoogleAuthRepository(this);
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Hide registration form elements since we use Google Auth
        hideRegistrationForm();

        // Setup navigation buttons
        setupClickListeners();

        // Show welcome content
        showWelcomeContent();
    }

    /**
     * Hide registration form elements
     */
    private void hideRegistrationForm() {
        // Hide email and password fields
        if (binding.emailEditText != null) {
            binding.emailEditText.setVisibility(View.GONE);
        }
        if (binding.passwordEditText != null) {
            binding.passwordEditText.setVisibility(View.GONE);
        }
        if (binding.registerButton != null) {
            binding.registerButton.setVisibility(View.GONE);
        }
    }

    /**
     * Show welcome content
     */
    private void showWelcomeContent() {
        // You can update this based on your actual layout elements
        // This is a placeholder for welcome content

        Log.d(TAG, "Displaying welcome content");

        // If you have welcome text views in your layout, update them here
        // Example:
        // binding.welcomeTitle.setText("Welcome to DDU E-Connect");
        // binding.welcomeMessage.setText("Connect with your university community");
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Login link - navigate to sign in
        if (binding.loginLink != null) {
            binding.loginLink.setOnClickListener(v -> navigateToSignIn());
        }

        // If you want to keep a "Get Started" button instead of register
        if (binding.registerButton != null) {
            binding.registerButton.setText("Get Started");
            binding.registerButton.setVisibility(View.VISIBLE);
            binding.registerButton.setOnClickListener(v -> navigateToSignIn());
        }
    }

    /**
     * Check if user is already signed in
     */
    private void checkUserStatus() {
        GoogleSignInAccount currentUser = authRepository.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            // User is already signed in, redirect to home
            navigateToHome();
        }
    }

    /**
     * Navigate to sign in activity
     */
    private void navigateToSignIn() {
        Log.d(TAG, "Navigating to SignInActivity");

        try {
            Intent intent = new Intent(RegistrationActivity.this, SignInActivity.class);
            startActivity(intent);
            finish(); // Optional: finish this activity
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to SignInActivity", e);
        }
    }

    /**
     * Navigate to home activity
     */
    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");

        try {
            Intent intent = new Intent(RegistrationActivity.this,
                    com.example.ddu_e_connect.presentation.view.home.HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to HomeActivity", e);
            // Fallback to sign-in
            navigateToSignIn();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check authentication status when activity starts
        checkUserStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "RegistrationActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        // Allow back navigation to previous screen
        super.onBackPressed();
    }
}