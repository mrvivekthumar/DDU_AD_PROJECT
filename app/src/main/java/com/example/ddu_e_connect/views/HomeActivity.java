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
    private LinearLayout innerLayout; // To reference the inner layout for animation

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
            // Fetch user role from Firestore
            authController.fetchUserRole(currentUser.getUid(), new AuthController.RoleCallback() {
                @Override
                public void onRoleFetched(String role) {
                    // Show or hide upload PDF button based on user role
                    if ("helper".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
                        binding.uploadPdfButton.setVisibility(View.VISIBLE);
                    } else {
                        binding.uploadPdfButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Handle role fetch error
                    Log.e("HomeActivity", "Error fetching user role: " + errorMessage);
                }
            });
        } else {
            Log.e("HomeActivity", "Current user is null.");
        }

        // Set click listener for upload PDF button
        binding.uploadPdfButton.setOnClickListener(v -> navigateToUploadActivity());
    }

    // Initialize announcements
    private void initializeAnnouncements() {
        announcements = new ArrayList<>();
        announcementUrls = new ArrayList<>();

        // Sample announcements
        announcements.add("CSI DDU Recruitment Drive: Join the tech revolution! Apply by Oct 10, 2024.");
        announcementUrls.add("https://www.instagram.com/csi_ddu/");

        announcements.add("GDSC DDU Coding Challenge: Solve real-world problems on Nov 1, 2024.");
        announcementUrls.add("https://www.instagram.com/gdscddu/");

        announcements.add("IETE Tech Talk: Harness the power of innovation on Oct 20, 2024.");
        announcementUrls.add("https://www.instagram.com/csi_ddu/");

        announcements.add("Shutterbugs DDU Photo Walk: Capture moments of magic on Nov 5, 2024.");
        announcementUrls.add("https://www.instagram.com/shutterbugs_ddu/");

        announcements.add("Samvaad DDU Debate Session: Speak your mind, change the world on Oct 25, 2024.");
        announcementUrls.add("https://www.instagram.com/samvaad_ddu/");

        announcements.add("Malgadi-DDU Discount Week: Lowest prices guaranteed from Oct 15-20, 2024.");
        announcementUrls.add("https://www.instagram.com/malgadi_ddu/");

        announcements.add("Decrypters Coding Club Hackathon: Prove your coding mettle on Nov 10, 2024.");
        announcementUrls.add("https://www.instagram.com/decrypters_ddu/");

        announcements.add("Sports Club FOT DDU Annual Sports Meet: Let the games begin on Nov 1, 2024.");
        announcementUrls.add("https://www.instagram.com/sportsclubddu/");


    }

    // Display announcements in HomeActivity
    private void displayAnnouncements() {
        LinearLayout announcementsContainer = binding.announcementsContainer;

        // Create a new CardView for all announcements
        CardView announcementCard = new CardView(this);
        announcementCard.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        announcementCard.setCardBackgroundColor(getResources().getColor(R.color.card_background));
        announcementCard.setCardElevation(6);

        // Create a LinearLayout to hold the announcements
        innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Loop through announcements
        for (int i = 0; i < announcements.size(); i++) {
            String announcementText = announcements.get(i);
            String url = announcementUrls.get(i);

            // Create a TextView for the announcement
            TextView announcementTextView = new TextView(this);
            announcementTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            announcementTextView.setText(announcementText);
            announcementTextView.setTextColor(getResources().getColor(R.color.text_primary));
            announcementTextView.setTextSize(20); // Increased font size
            announcementTextView.setPadding(16, 16, 16, 16);

            // Add click listener to open URL
            announcementTextView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            });

            // Add TextView to inner layout
            innerLayout.addView(announcementTextView);

            // Add a divider after each announcement except the last one
            if (i < announcements.size() - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1)); // Height of the divider
                divider.setBackgroundColor(getResources().getColor(R.color.divider_color)); // Set divider color
                innerLayout.addView(divider);
            }
        }

        // Add inner layout to CardView
        announcementCard.addView(innerLayout);

        // Add CardView to announcements container
        announcementsContainer.addView(announcementCard);

        // Start the bottom-to-top scrolling animation
        startScrollingAnimation();
    }

    // Animate announcements
    private void startScrollingAnimation() {
        innerLayout.post(() -> {
            int height = innerLayout.getHeight(); // The height of the announcements
            // Create a TranslateAnimation that moves from bottom to top
            TranslateAnimation animation = new TranslateAnimation(
                    0, 0, height, -height);
            animation.setDuration(15000); // Set the duration for the scroll (7 seconds)
            animation.setRepeatCount(Animation.INFINITE); // Repeat indefinitely
            animation.setRepeatMode(Animation.RESTART); // Restart from the beginning

            // Start the animation
            innerLayout.startAnimation(animation);
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
