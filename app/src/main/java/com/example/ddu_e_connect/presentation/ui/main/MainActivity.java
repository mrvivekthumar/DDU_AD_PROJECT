package com.example.ddu_e_connect.presentation.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.data.source.remote.GoogleAuthRepository;
import com.example.ddu_e_connect.presentation.view.auth.SignInActivity;
import com.example.ddu_e_connect.presentation.view.home.HomeActivity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SPLASH_DISPLAY_LENGTH = 2000; // 2 seconds

    private GoogleAuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        Log.d(TAG, "MainActivity started");

        // Initialize Firebase App Check with Play Integrity provider
        initializeFirebaseAppCheck();

        // Initialize authentication repository
        authRepository = new GoogleAuthRepository(this);

        // Apply animation to the splash image
        setupSplashAnimation();

        // Delay the splash screen and check authentication
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthenticationAndNavigate();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }

    /**
     * Initialize Firebase App Check for security
     */
    private void initializeFirebaseAppCheck() {
        try {
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance());
            Log.d(TAG, "Firebase App Check initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e);
            // Continue without App Check if it fails
        }
    }

    /**
     * Setup splash screen animation
     */
    private void setupSplashAnimation() {
        try {
            ImageView splashImage = findViewById(R.id.splashImage);
            if (splashImage != null) {
                Animation splashAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_animation);
                splashImage.startAnimation(splashAnimation);
                Log.d(TAG, "Splash animation started");
            } else {
                Log.w(TAG, "Splash image not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup splash animation", e);
            // Continue without animation if it fails
        }
    }

    /**
     * Check user authentication status and navigate accordingly
     */
    private void checkAuthenticationAndNavigate() {
        Log.d(TAG, "Checking authentication status");

        try {
            GoogleSignInAccount currentUser = authRepository.getCurrentUser();

            if (currentUser != null) {
                Log.d(TAG, "User already signed in: " + currentUser.getEmail());

                // Verify the user is still valid by checking with server
                verifyUserAndNavigate(currentUser);
            } else {
                Log.d(TAG, "No user signed in, navigating to sign-in");
                navigateToSignIn();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking authentication", e);
            // If there's an error, default to sign-in screen
            navigateToSignIn();
        }
    }

    /**
     * Verify user with server and navigate to appropriate screen
     */
    private void verifyUserAndNavigate(GoogleSignInAccount user) {
        Log.d(TAG, "Verifying user with server");

        // Check if user exists in Firestore and fetch their role
        authRepository.fetchUserRole(user.getId(), new GoogleAuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                Log.d(TAG, "User verification successful, role: " + role);
                navigateToHome();
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "User verification failed: " + errorMessage);
                // User doesn't exist in Firestore, redirect to sign-in to create account
                Log.d(TAG, "Redirecting to sign-in to create user account");
                navigateToSignIn();
            }
        });
    }

    /**
     * Navigate to home activity
     */
    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");

        try {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to HomeActivity", e);
            // Fallback to sign-in if navigation fails
            navigateToSignIn();
        }
    }

    /**
     * Navigate to sign-in activity
     */
    private void navigateToSignIn() {
        Log.d(TAG, "Navigating to SignInActivity");

        try {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to SignInActivity", e);
            // If we can't navigate anywhere, finish the activity
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resumed");
    }
}