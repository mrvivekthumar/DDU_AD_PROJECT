package com.example.ddu_e_connect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.views.RegistrationActivity;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authController = new AuthController();

        // Check if the user is logged in
        FirebaseUser currentUser = authController.getCurrentUser();
        if (currentUser != null) {
            // User is logged in, redirect to HomePageActivity
            startActivity(new Intent(MainActivity.this, MainActivity.class));
        } else {
            // User is not logged in, redirect to SignInActivity
            startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
        }

        if (authController == null) {
            Log.e("Error", "firebaseAuth is null");
            // Handle initialization error
        }
        // Close MainActivity so the user can't return to it by pressing back
    }
}
