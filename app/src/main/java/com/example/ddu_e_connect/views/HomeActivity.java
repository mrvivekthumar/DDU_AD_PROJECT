package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityHomeBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private AuthController authController;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();
        currentUser = authController.getCurrentUser();

        // Open Drawer on Button Click
        binding.imgbtntoggle.setOnClickListener(view -> binding.drawlayout.openDrawer(binding.navigationview));

        // Set up Navigation Item Selected Listener
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
