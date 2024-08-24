package com.example.ddu_e_connect.views;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityHomeBinding;
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
        binding.uploadPdfButton.setOnClickListener(v -> uploadPdf());
    }

    private void uploadPdf() {
        // Implement PDF upload logic here
        // For example, show a message indicating that this feature is not yet implemented
        Log.i("HomeActivity", "Upload PDF functionality not implemented yet.");
    }
}
