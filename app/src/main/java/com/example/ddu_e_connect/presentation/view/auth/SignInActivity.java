package com.example.ddu_e_connect.presentation.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.databinding.ActivitySignInBinding;
import com.example.ddu_e_connect.data.repository.GoogleAuthRepository;
import com.example.ddu_e_connect.presentation.view.home.HomeActivity;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    private ActivitySignInBinding binding;
    private GoogleAuthRepository authRepository;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupGoogleSignInLauncher();
        setupClickListeners();

        Log.d(TAG, "SignInActivity initialized");
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        authRepository = new GoogleAuthRepository(this);

        // Check if user is already signed in
        if (authRepository.isUserSignedIn()) {
            Log.d(TAG, "User already signed in, navigating to home");
            navigateToHome();
        }
    }

    /**
     * Setup Google Sign-In result launcher
     */
    private void setupGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleGoogleSignInResult(result);
                    }
                }
        );
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Your signInButton is already a Google SignInButton, perfect!
        binding.signInButton.setOnClickListener(v -> startGoogleSignIn());

        // Hide other buttons since we're only using Google Sign-In
        hideOtherAuthOptions();
    }

    /**
     * Hide other authentication options
     */
    private void hideOtherAuthOptions() {
        // Hide email/password fields since we're using Google Sign-In
        if (binding.emailEditText != null) {
            binding.emailEditText.setVisibility(View.GONE);
        }
        if (binding.passwordEditText != null) {
            binding.passwordEditText.setVisibility(View.GONE);
        }

        // Google SignInButton doesn't need text changes - it has default Google styling
        // Just keep it visible for Google Sign-In

        // Hide other links
        if (binding.forgotPasswordLink != null) {
            binding.forgotPasswordLink.setVisibility(View.GONE);
        }
        if (binding.registerLink != null) {
            binding.registerLink.setVisibility(View.GONE);
        }
    }

    /**
     * Start Google Sign-In process
     */
    private void startGoogleSignIn() {
        Log.d(TAG, "Starting Google Sign-In");

        // Show loading state
        setLoadingState(true);

        try {
            Intent signInIntent = authRepository.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Google Sign-In", e);
            setLoadingState(false);
            showError("Failed to start sign-in. Please try again.");
        }
    }

    /**
     * Handle Google Sign-In result
     */
    private void handleGoogleSignInResult(ActivityResult result) {
        Log.d(TAG, "Handling Google Sign-In result");

        authRepository.handleSignInResult(result.getData(), new GoogleAuthRepository.AuthCallback() {
            @Override
            public void onSuccess(GoogleAuthRepository.UserProfile userProfile) {
                Log.d(TAG, "Google Sign-In successful for: " + userProfile.getEmail());
                setLoadingState(false);

                // Show success message
                showSuccess("Welcome, " + userProfile.getName() + "!");

                // Navigate to home
                navigateToHome();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Google Sign-In failed: " + errorMessage);
                setLoadingState(false);
                showError(errorMessage);
            }
        });
    }

    /**
     * Set loading state for UI
     */
    private void setLoadingState(boolean isLoading) {
        // Google SignInButton - just disable/enable it
        if (binding.signInButton != null) {
            binding.signInButton.setEnabled(!isLoading);
            // Google SignInButton automatically shows proper text and styling
        }

        // Show loading message via Toast instead
        if (isLoading) {
            Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigate to home activity
     */
    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");
        Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Show error message to user
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error shown to user: " + message);
    }

    /**
     * Show success message to user
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Success message: " + message);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in when activity starts
        if (authRepository != null && authRepository.isUserSignedIn()) {
            Log.d(TAG, "User already signed in on activity start");
            navigateToHome();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}