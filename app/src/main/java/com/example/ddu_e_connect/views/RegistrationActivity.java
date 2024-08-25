package com.example.ddu_e_connect.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.controller.AuthController;
import com.example.ddu_e_connect.databinding.ActivityRegistrationBinding;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationActivity extends AppCompatActivity {
    private ActivityRegistrationBinding binding;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authController = new AuthController();

        binding.registerButton.setOnClickListener(v -> register());
        binding.loginLink.setOnClickListener(v -> navigateToSignIn());
    }

    private void register() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        binding.registerButton.setEnabled(false);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        authController.register(email, password, new AuthController.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(RegistrationActivity.this, "Registration successful! Please verify your email.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegistrationActivity.this, com.example.ddu_e_connect.views.SignInActivity.class));
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(RegistrationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSignIn() {
        startActivity(new Intent(RegistrationActivity.this, com.example.ddu_e_connect.views.SignInActivity.class));
    }
}
