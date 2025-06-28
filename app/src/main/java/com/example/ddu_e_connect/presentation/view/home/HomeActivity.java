package com.example.ddu_e_connect.presentation.view.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.data.source.remote.RoleManager;
import com.example.ddu_e_connect.databinding.ActivityHomeBinding;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.example.ddu_e_connect.presentation.view.clubs.ClubsActivity;
import com.example.ddu_e_connect.presentation.view.contact.ContactUsActivity;
import com.example.ddu_e_connect.presentation.view.papers.PapersActivity;
import com.example.ddu_e_connect.presentation.view.papers.UploadActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HomeActivity";
    private ActivityHomeBinding binding;
    private GoogleAuthRepository authRepository;
    private GoogleSignInAccount currentUser;

    // For announcements
    private ArrayList<String> announcements;
    private ArrayList<String> announcementUrls;
    private LinearLayout announcementsContainer;
    private Animation currentAnimation;
    private static final int ANIMATION_DURATION = 25000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        checkUserAuthentication();
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        authRepository = new GoogleAuthRepository(this);
        currentUser = authRepository.getCurrentUser();

        // Initialize announcements
        initializeAnnouncements();
    }

    /**
     * Setup UI components
     */
    private void setupUI() {
        setupNavigationDrawer();
        setupClickListeners();
        displayAnnouncements();
        updateUserInfo();
    }

    /**
     * Setup navigation drawer
     */
    private void setupNavigationDrawer() {
        // Open drawer on button click
        binding.imgbtntoggle.setOnClickListener(view ->
                binding.drawlayout.openDrawer(binding.navigationview)
        );

        // Set navigation item listener
        binding.navigationview.setNavigationItemSelectedListener(this);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Upload PDF button click listener
        if (binding.uploadPdfButton != null) {
            binding.uploadPdfButton.setOnClickListener(v -> navigateToUploadActivity());
        }
    }

    /**
     * Enhanced method to check user authentication and role
     */
    private void checkUserAuthentication() {
        if (currentUser == null) {
            Log.w(TAG, "No user signed in, redirecting to sign in");
            navigateToSignIn();
            return;
        }

        Log.d(TAG, "User signed in: " + currentUser.getEmail());

        // Initially hide upload button until role is confirmed
        hideUploadButton();

        // Fetch user role to determine upload button visibility
        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "User role fetched: " + role);
                updateUploadButtonVisibility(role);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching user role: " + errorMessage);

                // If role fetch fails, assign default role and hide upload button
                Log.w(TAG, "Role fetch failed, assigning default student role");
                updateUploadButtonVisibility("student");

                // Try to re-assign role based on email
                attemptRoleReassignment();
            }
        });
    }


    /**
     * Attempt to reassign role if fetching fails
     */
    private void attemptRoleReassignment() {
        if (currentUser != null) {
            Log.d(TAG, "Attempting role reassignment for: " + currentUser.getEmail());

            com.example.ddu_e_connect.data.source.remote.RoleManager roleManager =
                    new com.example.ddu_e_connect.data.source.remote.RoleManager(this);

            roleManager.assignRoleBasedOnEmail(
                    currentUser.getEmail(),
                    currentUser.getId(),
                    new com.example.ddu_e_connect.data.source.remote.RoleManager.RoleCallback() {
                        @Override
                        public void onRoleAssigned(String role) {
                            Log.d(TAG, "Role reassigned successfully: " + role);
                            updateUploadButtonVisibility(role);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Role reassignment failed: " + errorMessage);
                            // Keep upload button hidden for safety
                            hideUploadButton();
                        }
                    }
            );
        }
    }

    /**
     * Update upload button visibility based on user role
     */
    private void updateUploadButtonVisibility(String role) {
        if (binding.uploadPdfButton != null) {
            boolean isAuthorized = "helper".equalsIgnoreCase(role) ||
                    "admin".equalsIgnoreCase(role);

            binding.uploadPdfButton.setVisibility(isAuthorized ? View.VISIBLE : View.GONE);

            // Update button text based on role
            if (isAuthorized) {
                if ("admin".equalsIgnoreCase(role)) {
                    binding.uploadPdfButton.setText("📤 Upload PDF (Admin)");
                } else if ("helper".equalsIgnoreCase(role)) {
                    binding.uploadPdfButton.setText("📤 Upload PDF (Helper)");
                }
            }

            Log.d(TAG, "Upload button visibility: " + (isAuthorized ? "VISIBLE" : "GONE") +
                    " for role: " + role);

            // Show a welcome message based on role
            showRoleWelcomeMessage(role, isAuthorized);
        }
    }

    /**
     * Show welcome message based on user role
     */
    private void showRoleWelcomeMessage(String role, boolean canUpload) {
        String message;

        if ("admin".equalsIgnoreCase(role)) {
            message = "👑 Welcome Admin! You have full access to upload and manage PDFs.";
        } else if ("helper".equalsIgnoreCase(role)) {
            message = "🤝 Welcome Helper! You can upload PDFs to help students.";
        } else {
            message = "👨‍🎓 Welcome Student! You can access all study materials and papers.";
        }

        // Show toast message (optional - remove if too annoying)
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();

        Log.d(TAG, "Role welcome message: " + message);
    }

    /**
     * Hide upload button
     */
    private void hideUploadButton() {
        if (binding.uploadPdfButton != null) {
            binding.uploadPdfButton.setVisibility(View.GONE);
        }
    }

    /**
     * Update user information in navigation drawer
     */
    private void updateUserInfo() {
        if (currentUser != null) {
            View headerView = binding.navigationview.getHeaderView(0);
            if (headerView != null) {
                // Update user name if TextView exists
                TextView userNameTextView = headerView.findViewById(R.id.user_name);
                if (userNameTextView != null) {
                    userNameTextView.setText(currentUser.getDisplayName());
                }

                // Update user email if TextView exists
                TextView userEmailTextView = headerView.findViewById(R.id.user_email);
                if (userEmailTextView != null) {
                    userEmailTextView.setText(currentUser.getEmail());
                }
            }
        }
    }

    /**
     * Handle navigation item clicks
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Close drawer first to prevent UI lag
        binding.drawlayout.closeDrawer(binding.navigationview);

        // Add slight delay to allow drawer to close smoothly
        new android.os.Handler().postDelayed(() -> {
            if (id == R.id.papers) {
                navigateToPapersActivity();
            } else if (id == R.id.clubs) {
                navigateToClubsActivity();
            } else if (id == R.id.logout) {
                handleLogout();
            } else if (id == R.id.contact) {
                navigateToContactActivity();
            } else if (id == R.id.share) {
                shareApp();
            }
        }, 250);

        return true;
    }

    /**
     * Handle logout process
     */
    private void handleLogout() {
        Log.d(TAG, "Logging out user");

        authRepository.signOut(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "User logged out, navigating to sign in");
                navigateToSignIn();
            }
        });
    }

    /**
     * Initialize announcements data
     */
    private void initializeAnnouncements() {
        announcements = new ArrayList<>();
        announcementUrls = new ArrayList<>();

        // Add announcements
        addAnnouncement(
                "CSI DDU Recruitment Drive: Join the tech revolution! Apply by Oct 10, 2024.",
                "https://www.linkedin.com/in/csi-ddu-76b988285/"
        );

        addAnnouncement(
                "GDSC DDU Coding Challenge: Solve real-world problems on Nov 1, 2024.",
                "https://gdg.community.dev/gdg-on-campus-dharmsinh-desai-university-nadiad-india/"
        );

        addAnnouncement(
                "IETE Tech Talk: Harness the power of innovation on Oct 20, 2024.",
                "https://isf-website.vercel.app/"
        );

        addAnnouncement(
                "Shutterbugs DDU Photo Walk: Capture moments of magic on Nov 5, 2024.",
                "https://www.instagram.com/shutterbugs_ddu/"
        );

        addAnnouncement(
                "Samvaad DDU Debate Session: Speak your mind, change the world on Oct 25, 2024.",
                "https://www.instagram.com/samvaad_ddu/"
        );

        addAnnouncement(
                "Malgadi-DDU Discount Week: Lowest prices guaranteed from Oct 15-20, 2024.",
                "https://www.linkedin.com/company/malgadi-ddu/"
        );

        addAnnouncement(
                "Decrypters Coding Club Hackathon: Prove your coding mettle on Nov 10, 2024.",
                "https://www.linkedin.com/company/decrypters-ddu/"
        );

        addAnnouncement(
                "Sports Club FOT DDU Annual Sports Meet: Let the games begin on Nov 1, 2024.",
                "https://www.linkedin.com/company/sports-club-fot-ddu/"
        );
    }

    /**
     * Add single announcement
     */
    private void addAnnouncement(String announcement, String url) {
        if (announcement != null && url != null) {
            announcements.add(announcement);
            announcementUrls.add(url);
        }
    }

    /**
     * Display announcements in UI
     */
    private void displayAnnouncements() {
        if (binding.announcementsContainer != null) {
            announcementsContainer = binding.announcementsContainer;

            for (int i = 0; i < announcements.size(); i++) {
                createAnnouncementView(announcements.get(i), announcementUrls.get(i), i);
            }

            // Start scrolling animation
            startScrollingAnimation();
        }
    }

    /**
     * Create announcement view
     */
    private void createAnnouncementView(String announcementText, String url, int index) {
        TextView announcementTextView = new TextView(this);
        announcementTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        announcementTextView.setText(announcementText);
        announcementTextView.setTextColor(getResources().getColor(R.color.text_primary));
        announcementTextView.setTextSize(20);
        announcementTextView.setPadding(16, 16, 16, 16);
        announcementTextView.setClickable(true);

        // Add click listener to open URL
        announcementTextView.setOnClickListener(v -> openUrl(url));

        // Add the TextView to the announcements container
        announcementsContainer.addView(announcementTextView);

        // Add separator between announcements
        if (index < announcements.size() - 1) {
            addSeparator();
        }
    }

    /**
     * Add separator between announcements
     */
    private void addSeparator() {
        View separator = new View(this);
        separator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        separator.setBackgroundColor(getResources().getColor(R.color.divider_color));
        announcementsContainer.addView(separator);
    }

    /**
     * Start scrolling animation for announcements
     */
    private void startScrollingAnimation() {
        if (announcementsContainer != null) {
            announcementsContainer.post(() -> {
                announcementsContainer.clearAnimation();
                int height = announcementsContainer.getHeight();
                if (height > 0) {
                    TranslateAnimation animation = new TranslateAnimation(0, 0, height, -height);
                    animation.setDuration(ANIMATION_DURATION);
                    animation.setRepeatCount(Animation.INFINITE);
                    animation.setRepeatMode(Animation.RESTART);
                    currentAnimation = animation;
                    announcementsContainer.startAnimation(animation);
                }
            });
        }
    }

    /**
     * Open URL in browser
     */
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open URL: " + url, e);
        }
    }

    // Navigation methods

    /**
     * Navigate to sign in activity
     */
    private void navigateToSignIn() {
        Intent intent = new Intent(HomeActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to contact activity
     */
    private void navigateToContactActivity() {
        Intent intent = new Intent(HomeActivity.this, ContactUsActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to papers activity
     */
    private void navigateToPapersActivity() {
        Intent intent = new Intent(HomeActivity.this, PapersActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to upload activity
     */
    private void navigateToUploadActivity() {
        Intent intent = new Intent(HomeActivity.this,UploadActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to clubs activity
     */
    private void navigateToClubsActivity() {
        Intent intent = new Intent(HomeActivity.this, ClubsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up animation to prevent memory leaks
        if (currentAnimation != null) {
            currentAnimation.cancel();
            currentAnimation = null;
        }

        if (announcementsContainer != null) {
            announcementsContainer.clearAnimation();
        }

        binding = null;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop animation when activity is paused
        if (announcementsContainer != null) {
            announcementsContainer.clearAnimation();
        }
    }

    /**
     * Share app functionality
     */
    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DDU E-Connect App");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out DDU E-Connect app for accessing university papers, clubs, and announcements!\n\n" +
                            "Features:\n" +
                            "• Access exam papers and study materials\n" +
                            "• Join university clubs and activities\n" +
                            "• Stay updated with announcements\n" +
                            "• Secure Google Sign-In");

            startActivity(Intent.createChooser(shareIntent, "Share DDU E-Connect"));
            Log.d(TAG, "Share intent created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to share app", e);
            android.widget.Toast.makeText(this, "Unable to share app", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart animation when activity is resumed
        if (announcementsContainer != null) {
            startScrollingAnimation();
        }
    }


    // Add these methods to HomeActivity.java for testing roles

    /**
     * Test current user role
     */
    private void testCurrentUserRole() {
        GoogleSignInAccount currentUser = authRepository.getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "No user signed in");
            return;
        }

        Log.d(TAG, "Testing role for user: " + currentUser.getEmail());

        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "Current user role: " + role);

                String message = "👤 User: " + currentUser.getDisplayName() + "\n" +
                        "📧 Email: " + currentUser.getEmail() + "\n" +
                        "🔐 Role: " + role.toUpperCase() + "\n" +
                        "📤 Can Upload: " + (RoleManager.canUpload(role) ? "YES ✅" : "NO ❌");

                showRoleDialog("Current User Role", message);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to fetch role: " + errorMessage);
            }
        });
    }

    /**
     * Show role information dialog
     */
    private void showRoleDialog(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("Re-assign Role", (dialog, which) -> reassignCurrentUserRole())
                .show();
    }

    /**
     * Reassign current user role (forces role check)
     */
    private void reassignCurrentUserRole() {
        GoogleSignInAccount currentUser = authRepository.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "No user signed in");
            return;
        }

        RoleManager roleManager = new RoleManager(this);

        roleManager.assignRoleBasedOnEmail(
                currentUser.getEmail(),
                currentUser.getId(),
                new RoleManager.RoleCallback() {
                    @Override
                    public void onRoleAssigned(String role) {
                        Log.d(TAG, "Role reassigned: " + role);

                        // Test the role again
                        testCurrentUserRole();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "Failed to reassign role: " + errorMessage);
                    }
                }
        );
    }

    /**
     * Show role setup instructions
     */
    private void showRoleSetupInstructions() {
        String instructions = RoleManager.getRoleSetupInstructions();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Role Assignment Setup")
                .setMessage(instructions)
                .setPositiveButton("OK", null)
                .setNeutralButton("Test My Role", (dialog, which) -> testCurrentUserRole())
                .show();
    }

    // Add this button click listener to test roles
    private void addRoleTestButton() {
        // You can add this to your existing layout or create a test button
        // For testing, you can call testCurrentUserRole() from your upload button

        if (binding.uploadPdfButton != null) {
            binding.uploadPdfButton.setOnLongClickListener(v -> {
                // Long press upload button to test role
                testCurrentUserRole();
                return true;
            });
        }
    }


    /**
     * Debug method to check current user role - Add this to HomeActivity.java
     */
    private void debugCurrentUserRole() {
        if (currentUser == null) {
            Log.e(TAG, "DEBUG: No user signed in");
            android.widget.Toast.makeText(this, "❌ No user signed in", android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "DEBUG: Checking role for: " + currentUser.getEmail());

        authRepository.fetchUserRole(currentUser.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                String debugInfo = "🔍 DEBUG INFO:\n\n" +
                        "👤 User: " + currentUser.getDisplayName() + "\n" +
                        "📧 Email: " + currentUser.getEmail() + "\n" +
                        "🆔 ID: " + currentUser.getId().substring(0, 8) + "...\n" +
                        "🔐 Role: " + role.toUpperCase() + "\n\n" +
                        "📤 Can Upload: " + (com.example.ddu_e_connect.data.source.remote.RoleManager.canUpload(role) ? "YES ✅" : "NO ❌") + "\n" +
                        "👑 Is Admin: " + (com.example.ddu_e_connect.data.source.remote.RoleManager.isAdmin(role) ? "YES ✅" : "NO ❌") + "\n" +
                        "🤝 Is Helper: " + (com.example.ddu_e_connect.data.source.remote.RoleManager.isHelper(role) ? "YES ✅" : "NO ❌");

                Log.d(TAG, "DEBUG: " + debugInfo);

                new androidx.appcompat.app.AlertDialog.Builder(HomeActivity.this)
                        .setTitle("🔍 Debug: Current Role")
                        .setMessage(debugInfo)
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Test Upload", (dialog, which) -> {
                            // Test upload access
                            Intent uploadIntent = new Intent(HomeActivity.this,
                                    com.example.ddu_e_connect.presentation.view.papers.UploadActivity.class);
                            startActivity(uploadIntent);
                        })
                        .show();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "DEBUG: Role fetch failed: " + errorMessage);
                android.widget.Toast.makeText(HomeActivity.this,
                        "❌ Role fetch failed: " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Add this click listener to your existing setupClickListeners() method in HomeActivity
     */
    private void addDebugFunctionality() {
        // Long press on the app title to show debug info
        TextView titleView = findViewById(R.id.toolbar).findViewById(android.R.id.text1);
        if (titleView != null) {
            titleView.setOnLongClickListener(v -> {
                debugCurrentUserRole();
                return true;
            });
        }

        // Or add to your existing upload button long press
        if (binding.uploadPdfButton != null) {
            binding.uploadPdfButton.setOnLongClickListener(v -> {
                debugCurrentUserRole();
                return true;
            });
        }
    }
}