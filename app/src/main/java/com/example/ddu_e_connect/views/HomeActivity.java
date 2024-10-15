package com.example.ddu_e_connect.views;

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
import androidx.cardview.widget.CardView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityHomeBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private AuthController authController;
    private FirebaseUser currentUser;

    // For announcements
    private ArrayList<String> announcements;
    private ArrayList<String> announcementUrls;
    private LinearLayout announcementsContainer; // To reference the layout for scrolling animation
    private boolean isScrolling = false; // Flag to control scrolling
    private int animationDuration = 25000; // Duration for the scrolling animation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();
        currentUser = authController.getCurrentUser();

        // Initialize and display announcements
        initializeAnnouncements();
        displayAnnouncements();

        // Open Drawer on Button Click
        binding.imgbtntoggle.setOnClickListener(view -> binding.drawlayout.openDrawer(binding.navigationview));

        binding.navigationview.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.papers) {
                    navigateToPapersActivity();
                } else if (id == R.id.clubs) {
                    navigateToClubsActivity();
                } else if (id == R.id.logout) {
                    navigateToLogoutActivity();
                } else if (id == R.id.contact) {
                    navigateToContactActivity();
                }
                // Close the drawer after the item is clicked
                binding.drawlayout.closeDrawer(binding.navigationview);
                return true;
            }
        });

        if (currentUser != null) {
            authController.fetchUserRole(currentUser.getUid(), new AuthController.RoleCallback() {
                @Override
                public void onRoleFetched(String role) {
                    if ("helper".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
                        binding.uploadPdfButton.setVisibility(View.VISIBLE);
                    } else {
                        binding.uploadPdfButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("HomeActivity", "Error fetching user role: " + errorMessage);
                }
            });
        } else {
            Log.e("HomeActivity", "Current user is null.");
        }

        binding.uploadPdfButton.setOnClickListener(v -> navigateToUploadActivity());
    }

    // Initialize announcements
    private void initializeAnnouncements() {
        announcements = new ArrayList<>();
        announcementUrls = new ArrayList<>();

        // Sample announcements
        announcements.add("CSI DDU Recruitment Drive: Join the tech revolution! Apply by Oct 10, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("GDSC DDU Coding Challenge: Solve real-world problems on Nov 1, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("IETE Tech Talk: Harness the power of innovation on Oct 20, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("Shutterbugs DDU Photo Walk: Capture moments of magic on Nov 5, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("Samvaad DDU Debate Session: Speak your mind, change the world on Oct 25, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("Malgadi-DDU Discount Week: Lowest prices guaranteed from Oct 15-20, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("Decrypters Coding Club Hackathon: Prove your coding mettle on Nov 10, 2024.");
        announcementUrls.add("http://example.com/exam");

        announcements.add("Sports Club FOT DDU Annual Sports Meet: Let the games begin on Nov 1, 2024.");
        announcementUrls.add("http://example.com/exam");
    }

    // Display announcements
    private void displayAnnouncements() {
        announcementsContainer = binding.announcementsContainer;

        for (int i = 0; i < announcements.size(); i++) {
            String announcementText = announcements.get(i);
            String url = announcementUrls.get(i);

            // Create a TextView for each announcement
            TextView announcementTextView = new TextView(this);
            announcementTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            announcementTextView.setText(announcementText);
            announcementTextView.setTextColor(getResources().getColor(R.color.text_primary));
            announcementTextView.setTextSize(20);
            announcementTextView.setPadding(16, 16, 16, 16);
            announcementTextView.setClickable(true);

            // Add click listener to open the URL
            announcementTextView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            });

            // Add the TextView to the announcements container
            announcementsContainer.addView(announcementTextView);

            // Add a separator between announcements
            if (i < announcements.size() - 1) {
                View separator = new View(this);
                separator.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2)); // Height of the separator
                separator.setBackgroundColor(getResources().getColor(R.color.divider_color));
                announcementsContainer.addView(separator);
            }
        }

        // Start the scrolling animation
        startScrollingAnimation();
    }

    // Start scrolling animation for announcements
    private void startScrollingAnimation() {
        announcementsContainer.post(() -> {
            announcementsContainer.clearAnimation();
            int height = announcementsContainer.getHeight();
            TranslateAnimation animation = new TranslateAnimation(0, 0, height, -height);
            animation.setDuration(animationDuration);
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.RESTART);
            announcementsContainer.startAnimation(animation);
        });
    }

    private void navigateToContactActivity() {
        Intent intent = new Intent(HomeActivity.this, ContactUsActivity.class);
        startActivity(intent);
    }

    private void navigateToPapersActivity() {
        Intent intent = new Intent(HomeActivity.this, PapersActivity.class);
        startActivity(intent);
    }

    private void navigateToUploadActivity() {
        Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
        startActivity(intent);
    }

    private void navigateToClubsActivity() {
        Intent intent = new Intent(HomeActivity.this, ClubsActivity.class);
        startActivity(intent);
    }

    private void navigateToLogoutActivity() {
        Intent intent = new Intent(HomeActivity.this, LogoutActivity.class);
        startActivity(intent);
    }
}
