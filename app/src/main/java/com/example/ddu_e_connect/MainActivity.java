package com.example.ddu_e_connect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = new AuthController().getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                startActivity(new Intent(MainActivity.this, com.example.ddu_e_connect.views.HomeActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, com.example.ddu_e_connect.views.SignInActivity.class));
            }
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}
