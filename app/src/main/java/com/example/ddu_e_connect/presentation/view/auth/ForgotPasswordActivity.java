package com.example.ddu_e_connect.presentation.view.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.data.repository.GoogleAuthRepository;
import com.example.ddu_e_connect.databinding.ActivityForogtPasswordBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * Help/Support Activity - Provides assistance and support information
 * Converted from ForgotPasswordActivity since we use Google Authentication
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";

    // Support contact information
    private static final String SUPPORT_EMAIL = "support@ddu.ac.in";
    private static final String SUPPORT_PHONE = "+91-2687-252244";
    private static final String DDU_WEBSITE = "https://www.ddu.ac.in";
    private static final String GOOGLE_ACCOUNT_HELP = "https://support.google.com/accounts";

    private ActivityForogtPasswordBinding binding;
    private GoogleAuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForogtPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        checkUserStatus();

        Log.d(TAG, "ForgotPasswordActivity (Help Screen) initialized");
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
        // Hide password reset elements
        hidePasswordResetForm();

        // Setup help content
        setupHelpContent();

        // Setup click listeners
        setupClickListeners();
    }

    /**
     * Hide password reset form elements
     */
    private void hidePasswordResetForm() {
        // Hide email input since we don't need password reset
        if (binding.emailEditText != null) {
            binding.emailEditText.setVisibility(View.GONE);
        }

        // Convert reset button to help button
        if (binding.resetPasswordButton != null) {
            binding.resetPasswordButton.setText("Contact Support");
            binding.resetPasswordButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Setup help content
     */
    private void setupHelpContent() {
        // You can update this based on your actual layout elements
        Log.d(TAG, "Setting up help content");

        // If you have TextViews for help content, update them here
        // This would require adding TextViews to your layout
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Contact Support button
        if (binding.resetPasswordButton != null) {
            binding.resetPasswordButton.setOnClickListener(v -> showSupportOptions());
        }

        // Back to login link
        if (binding.backToLoginLink != null) {
            binding.backToLoginLink.setOnClickListener(v -> navigateToSignIn());
        }
    }

    /**
     * Check if user is already signed in
     */
    private void checkUserStatus() {
        GoogleSignInAccount currentUser = authRepository.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            // User is signed in, show different help options
            updateUIForSignedInUser();
        }
    }

    /**
     * Update UI for signed-in users
     */
    private void updateUIForSignedInUser() {
        if (binding.resetPasswordButton != null) {
            binding.resetPasswordButton.setText("Get Help");
        }

        if (binding.backToLoginLink != null) {
            binding.backToLoginLink.setText("Back to Home");
            binding.backToLoginLink.setOnClickListener(v -> navigateToHome());
        }
    }

    /**
     * Show support options dialog or menu
     */
    private void showSupportOptions() {
        Log.d(TAG, "Showing support options");

        // Create an array of support options
        String[] options = {
                "Email Support",
                "Call Support",
                "Visit DDU Website",
                "Google Account Help",
                "App Information"
        };

        // Create and show alert dialog with options
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("How can we help you?")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            sendSupportEmail();
                            break;
                        case 1:
                            callSupport();
                            break;
                        case 2:
                            openDDUWebsite();
                            break;
                        case 3:
                            openGoogleAccountHelp();
                            break;
                        case 4:
                            showAppInformation();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Send support email
     */
    private void sendSupportEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DDU E-Connect Support Request");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello,\n\nI need help with DDU E-Connect app.\n\nDetails:\n");

            startActivity(Intent.createChooser(emailIntent, "Send email"));
            Log.d(TAG, "Support email intent started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send support email", e);
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Call support
     */
    private void callSupport() {
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + SUPPORT_PHONE));
            startActivity(callIntent);
            Log.d(TAG, "Support call intent started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initiate support call", e);
            Toast.makeText(this, "Cannot make calls", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open DDU website
     */
    private void openDDUWebsite() {
        openWebUrl(DDU_WEBSITE, "DDU website");
    }

    /**
     * Open Google Account Help
     */
    private void openGoogleAccountHelp() {
        openWebUrl(GOOGLE_ACCOUNT_HELP, "Google Account Help");
    }

    /**
     * Open web URL
     */
    private void openWebUrl(String url, String description) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
            Log.d(TAG, description + " opened");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open " + description, e);
            Toast.makeText(this, "Cannot open " + description, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show app information
     */
    private void showAppInformation() {
        String appInfo = "DDU E-Connect v1.0\n\n" +
                "Features:\n" +
                "• Access exam papers and study materials\n" +
                "• Join university clubs and activities\n" +
                "• Stay updated with announcements\n" +
                "• Upload and share documents (for authorized users)\n\n" +
                "Authentication:\n" +
                "• Secure Google Sign-In\n" +
                "• No password required\n\n" +
                "Support:\n" +
                "• Email: " + SUPPORT_EMAIL + "\n" +
                "• Phone: " + SUPPORT_PHONE + "\n\n" +
                "Developed for Dharmsinh Desai University";

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("About DDU E-Connect")
                .setMessage(appInfo)
                .setPositiveButton("OK", null)
                .show();

        Log.d(TAG, "App information displayed");
    }

    /**
     * Navigate to sign in activity
     */
    private void navigateToSignIn() {
        Log.d(TAG, "Navigating to SignInActivity");

        try {
            Intent intent = new Intent(ForgotPasswordActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
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
            Intent intent = new Intent(ForgotPasswordActivity.this,
                    com.example.ddu_e_connect.presentation.view.home.HomeActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to HomeActivity", e);
            navigateToSignIn(); // Fallback
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Log.d(TAG, "ForgotPasswordActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        // Navigate back to sign in
        super.onBackPressed();
        navigateToSignIn();
    }
}