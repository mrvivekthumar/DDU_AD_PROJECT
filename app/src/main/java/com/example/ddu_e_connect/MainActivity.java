package com.example.ddu_e_connect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        // Initialize Firebase App Check with SafetyNet provider
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());

        // Apply animation to the splash image
        ImageView splashImage = findViewById(R.id.splashImage);
        Animation splashAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
        splashImage.startAnimation(splashAnimation);

        // Delay the splash screen for 2 seconds
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
