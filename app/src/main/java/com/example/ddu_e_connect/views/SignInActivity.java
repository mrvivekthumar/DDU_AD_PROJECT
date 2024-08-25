package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivitySignInBinding;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {
    private ActivitySignInBinding binding;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();

        binding.signInButton.setOnClickListener(v -> signIn());
        binding.forgotPasswordLink.setOnClickListener(v -> navigateToForgotPassword());
        binding.registerLink.setOnClickListener(v -> navigateToRegister());
    }

    private void signIn() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

<<<<<<< HEAD
=======
        binding.signInButton.setEnabled(false);

>>>>>>> new-repo/master
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        authController.signIn(email, password, new AuthController.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (user.isEmailVerified()) {
                    startActivity(new Intent(SignInActivity.this, com.example.ddu_e_connect.views.HomeActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignInActivity.this, "Please verify your email address", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToForgotPassword() {
        startActivity(new Intent(SignInActivity.this, com.example.ddu_e_connect.views.ForgotPasswordActivity.class));
    }

    private void navigateToRegister() {
        startActivity(new Intent(SignInActivity.this, RegistrationActivity.class));
    }
}
