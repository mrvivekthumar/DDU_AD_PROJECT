package com.example.ddu_e_connect.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.controller.AuthController;
import com.google.firebase.auth.FirebaseUser;

public class RegistrationActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button registerButton;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        authController = new AuthController();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (isValidEmail(email)) {
                    if(TextUtils.isEmpty(password)){
                        Toast.makeText(RegistrationActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                        return ;
                    }
                    authController.register(email, password, new AuthController.OnAuthCompleteListener() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            // Registration successful, navigate to SignInActivity
                            Toast.makeText(RegistrationActivity.this, "Registration successful! Please sign in.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegistrationActivity.this, SignInActivity.class));
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            // Registration failed, show error message
                            Toast.makeText(RegistrationActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(RegistrationActivity.this, "Invalid email format. Please use your institutional email.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[0-9]{2}[a-zA-Z0-9._%+-]*@ddu\\.ac\\.in$";
        return email.matches(emailPattern);
    }
}