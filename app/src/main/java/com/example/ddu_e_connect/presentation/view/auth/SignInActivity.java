package com.example.ddu_e_connect.presentation.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
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
        setupFeaturesList();

        Log.d(TAG, "SignInActivity initialized with enhanced UI");
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
        // Google Sign-In button click
        binding.signInButton.setOnClickListener(v -> startGoogleSignIn());

        // Add subtle animation to sign-in card
        binding.signInCard.setOnClickListener(v -> {
            // Optional: Add click feedback to the card
            v.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        });

        // Logo click for fun interaction
        binding.logoCard.setOnClickListener(v -> {
            v.animate()
                    .rotationY(360f)
                    .setDuration(800)
                    .withEndAction(() -> v.setRotationY(0f))
                    .start();

            showInfo("Welcome to DDU E-Connect! 🎓");
        });
    }

    /**
     * Setup features list in the preview card
     */
    private void setupFeaturesList() {
        try {
            // Feature 1: Papers
            setupFeatureItem(R.id.feature1, R.drawable.paper, "Access exam papers and study materials");

            // Feature 2: Clubs
            setupFeatureItem(R.id.feature2, R.drawable.club, "Join university clubs and activities");

            // Feature 3: Contact
            setupFeatureItem(R.id.feature3, R.drawable.phone, "Stay updated with announcements");

            Log.d(TAG, "Features list setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up features list", e);
        }
    }

    /**
     * Setup individual feature item
     */
    private void setupFeatureItem(int featureId, int iconRes, String featureText) {
        try {
            View featureView = binding.getRoot().findViewById(featureId);
            if (featureView != null) {
                ImageView featureIcon = featureView.findViewById(R.id.featureIcon);
                TextView featureTextView = featureView.findViewById(R.id.featureText);

                if (featureIcon != null) {
                    featureIcon.setImageResource(iconRes);
                }

                if (featureTextView != null) {
                    featureTextView.setText(featureText);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up feature item: " + featureId, e);
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
                showSuccess("Welcome, " + getFirstName(userProfile.getName()) + "! 🎉");

                // Add slight delay for better UX
                new android.os.Handler().postDelayed(() -> navigateToHome(), 1000);
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
     * Get first name from full name
     */
    private String getFirstName(String fullName) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] nameParts = fullName.trim().split("\\s+");
            return nameParts[0];
        }
        return "User";
    }

    /**
     * Set loading state for UI
     */
    private void setLoadingState(boolean isLoading) {
        // Disable/enable sign-in button
        if (binding.signInButton != null) {
            binding.signInButton.setEnabled(!isLoading);
        }

        // Add loading feedback
        if (isLoading) {
            showInfo("Signing in... Please wait");

            // Add subtle loading animation to the sign-in card
            binding.signInCard.animate()
                    .alpha(0.7f)
                    .setDuration(300)
                    .start();
        } else {
            // Restore normal appearance
            binding.signInCard.animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .start();
        }
    }

    /**
     * Navigate to home activity
     */
    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");

        try {
            Intent intent = new Intent(SignInActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Add transition animation
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to HomeActivity", e);
            showError("Navigation failed. Please try again.");
        }
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

    /**
     * Show info message to user
     */
    private void showInfo(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Info message: " + message);
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
    protected void onResume() {
        super.onResume();

        // Reset any loading states when returning to activity
        setLoadingState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "SignInActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        // Add confirmation dialog for exit if needed
        super.onBackPressed();
    }
}