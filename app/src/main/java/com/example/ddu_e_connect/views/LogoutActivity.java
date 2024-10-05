package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityLogoutBinding;

public class LogoutActivity extends AppCompatActivity {

    private ActivityLogoutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Initialize view binding
        binding = ActivityLogoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up logout button click listener
        binding.logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {

        binding.logoutButton.setEnabled(false);
        AuthController authController = new AuthController();
        authController.logout().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Redirect to SignInActivity after successful logout
                Intent intent = new Intent(LogoutActivity.this, com.example.ddu_e_connect.views.SignInActivity.class);

                // Clear the activity stack so that the user cannot go back to the home activity
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // Handle logout failure
                // You can show an error message to the user here
            }
        });
    }
}
