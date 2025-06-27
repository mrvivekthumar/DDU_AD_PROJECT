package com.example.ddu_e_connect.presentation.view.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.repository.GoogleAuthRepository;
import com.example.ddu_e_connect.databinding.ActivityLogoutBinding;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class LogoutActivity extends AppCompatActivity {
    private static final String TAG = "LogoutActivity";

    private ActivityLogoutBinding binding;
    private GoogleAuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize view binding
        binding = ActivityLogoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize authentication repository
        authRepository = new GoogleAuthRepository(this);

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup UI
        setupUI();

        Log.d(TAG, "LogoutActivity initialized");
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        // Set up logout button click listener
        binding.logoutButton.setOnClickListener(v -> showLogoutConfirmation());

        binding.cancelButton.setOnClickListener(v -> finish());


        // Display current user info
        displayUserInfo();

        // Update button text
        updateButtonText();
    }

    /**
     * Display current user information
     */
    /**
     * Display current user information in UI
     */
    private void displayUserInfo() {
        GoogleSignInAccount currentUser = authRepository.getCurrentUser();

        if (currentUser != null) {
            // Update user name
            if (binding.userNameText != null) {
                binding.userNameText.setText(currentUser.getDisplayName());
            }

            // Update user email
            if (binding.userEmailText != null) {
                binding.userEmailText.setText(currentUser.getEmail());
            }

            // Fetch and display user role
            authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
                @Override
                public void onRoleFetched(String role) {
                    runOnUiThread(() -> {
                        if (binding.userRoleText != null) {
                            binding.userRoleText.setText(role.toUpperCase());
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    // Keep default "Student" text if role fetch fails
                }
            });
        }
    }

    /**
     * Update logout button text based on user state
     */
    private void updateButtonText() {
        GoogleSignInAccount currentUser = authRepository.getCurrentUser();

        if (currentUser != null) {
            String userName = currentUser.getDisplayName();
            if (userName != null && !userName.trim().isEmpty()) {
                binding.logoutButton.setText("Sign out " + userName.split(" ")[0]);
            } else {
                binding.logoutButton.setText("Sign out");
            }
        } else {
            binding.logoutButton.setText("Sign out");
        }
    }

    /**
     * Show logout confirmation (optional)
     */
    private void showLogoutConfirmation() {
        // For now, directly logout. You can add confirmation dialog later if needed
        performLogout();

        // Uncomment below to add confirmation dialog:
        /*
        new AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", null)
            .show();
        */
    }

    /**
     * Perform the actual logout process
     */
    private void performLogout() {
        Log.d(TAG, "Starting logout process");

        // Disable logout button to prevent multiple clicks
        setLogoutButtonState(false, "Signing out...");

        // Show loading message
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();

        // Perform Google Sign-Out
        authRepository.signOut(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Google sign-out completed");

                // Run on UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onLogoutSuccess();
                    }
                });
            }
        });
    }

    /**
     * Handle successful logout
     */
    private void onLogoutSuccess() {
        Log.d(TAG, "Logout successful");

        // Show success message
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to sign-in activity
        navigateToSignIn();
    }

    /**
     * Handle logout failure
     */
    private void onLogoutFailure(String errorMessage) {
        Log.e(TAG, "Logout failed: " + errorMessage);

        // Re-enable logout button
        setLogoutButtonState(true, "Sign out");

        // Show error message
        Toast.makeText(this, "Sign out failed: " + errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Set logout button state
     */
    private void setLogoutButtonState(boolean enabled, String text) {
        if (binding.logoutButton != null) {
            binding.logoutButton.setEnabled(enabled);
            binding.logoutButton.setText(text);
        }
    }

    /**
     * Navigate to sign-in activity
     */
    private void navigateToSignIn() {
        Log.d(TAG, "Navigating to SignInActivity");

        try {
            Intent intent = new Intent(LogoutActivity.this, SignInActivity.class);

            // Clear the activity stack so user cannot go back to logged-in screens
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to SignInActivity", e);

            // If navigation fails, at least finish this activity
            finish();
        }
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        // Go back to previous activity instead of staying on logout screen
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is still signed in
        if (!authRepository.isUserSignedIn()) {
            Log.w(TAG, "User not signed in on activity start");
            navigateToSignIn();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "LogoutActivity destroyed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "LogoutActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "LogoutActivity resumed");

        // Refresh user info when activity resumes
        displayUserInfo();
        updateButtonText();
    }
}