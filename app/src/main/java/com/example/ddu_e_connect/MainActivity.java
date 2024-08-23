package com.example.ddu_e_connect;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.view.HomePageActivity;
import com.example.ddu_e_connect.view.RegistrationActivity;
import com.example.ddu_e_connect.view.SignInActivity;
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
            startActivity(new Intent(MainActivity.this, HomePageActivity.class));
        } else {
            // User is not logged in, redirect to SignInActivity
            startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
        }
        // Close MainActivity so the user can't return to it by pressing back
        finish();
    }
}
